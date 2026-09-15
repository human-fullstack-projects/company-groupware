package com.company.groupware.service;

import com.company.groupware.entity.ApprovalDocument;
import com.company.groupware.entity.ApprovalDocumentAttachment;
import com.company.groupware.repository.ApprovalDocumentAttachmentRepository;
import com.company.groupware.repository.ApprovalDocumentMemberRepository;
import com.company.groupware.repository.ApprovalDocumentRepository;
import com.company.groupware.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApprovalDocumentAttachmentService {
    private final ApprovalDocumentAttachmentRepository attachmentRepository;
    private final ApprovalDocumentRepository documentRepository;
    private final ApprovalDocumentMemberRepository documentMemberRepository;
    private final EmployeeRepository employeeRepository;

    private final Path uploadPath =
            Paths.get(
                            "uploads",
                            "approval-document"
                    )
                    .toAbsolutePath()
                    .normalize();


    /*
     * 문서의 첨부파일 전체 조회
     */
    @Transactional(readOnly = true)
    public List<ApprovalDocumentAttachment> getAttachments(
            Long documentId) {

        return attachmentRepository.findByDocument_DocumentIdOrderByAttachmentIdAsc(
                documentId
        );
    }

    @Transactional
    public void deleteAttachment(
            Long documentId,
            Long emplId,
            Long attachmentId) {

        ApprovalDocument document = documentRepository.findById(documentId).orElseThrow(() ->
                new IllegalArgumentException(
                        "전자결재 문서를 찾을 수 없습니다."
                )
        );


        // 작성자 확인
        if (!Objects.equals(
                document.getWriter().getEmplId(),
                emplId)) {

            throw new IllegalArgumentException(
                    "본인이 작성한 문서의 첨부파일만 삭제할 수 있습니다."
            );
        }


        // 승인/반려 완료
        if ("APPROVED".equals(document.getStatus())
                || "REJECTED".equals(document.getStatus())) {

            throw new IllegalArgumentException(
                    "결재가 종료된 문서의 첨부파일은 삭제할 수 없습니다."
            );
        }


        // 이미 누군가 승인/반려했는지
        boolean hasAction =
                documentMemberRepository
                        .findByDocument_DocumentIdOrderByApprovalOrderAsc(
                                documentId
                        )
                        .stream()
                        .anyMatch(member ->
                                "APPROVED".equals(member.getStatus())
                                        || "REJECTED".equals(member.getStatus())
                        );


        if (hasAction) {
            throw new IllegalArgumentException(
                    "결재가 진행된 문서의 첨부파일은 삭제할 수 없습니다."
            );
        }


        ApprovalDocumentAttachment attachment =
                getAttachment(
                        documentId,
                        attachmentId
                );


        // 실제 파일 삭제
        deletePhysicalFile(attachment);

        // DB 삭제
        attachmentRepository.delete(attachment);
    }


    /*
     * 여러 파일 등록
     */
    @Transactional
    public List<ApprovalDocumentAttachment> saveAttachments(
            Long documentId,
            Long emplId,
            List<MultipartFile> files) {

        validateFiles(files);


        ApprovalDocument document =
                documentRepository
                        .findById(documentId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "전자결재 문서를 찾을 수 없습니다."
                                )
                        );


        /*
         * 작성자만 파일 첨부 가능
         */
        if (!Objects.equals(
                document.getWriter().getEmplId(),
                emplId)) {

            throw new IllegalArgumentException(
                    "본인이 작성한 문서에만 파일을 첨부할 수 있습니다."
            );
        }


        /*
         * 승인/반려 완료 문서
         */
        if ("APPROVED".equals(document.getStatus())
                || "REJECTED".equals(document.getStatus())) {

            throw new IllegalArgumentException(
                    "결재가 종료된 문서에는 파일을 첨부할 수 없습니다."
            );
        }


        /*
         * 이미 결재가 시작됐는지 확인
         */
        boolean hasAction =
                documentMemberRepository
                        .findByDocument_DocumentIdOrderByApprovalOrderAsc(
                                documentId
                        )
                        .stream()
                        .anyMatch(member ->
                                "APPROVED".equals(
                                        member.getStatus()
                                )
                                        ||
                                        "REJECTED".equals(
                                                member.getStatus()
                                        )
                        );


        if (hasAction) {

            throw new IllegalArgumentException(
                    "결재가 진행된 문서에는 파일을 첨부할 수 없습니다."
            );
        }


        try {
            long currentCount =
                    attachmentRepository
                            .countByDocument_DocumentId(
                                    documentId
                            );


            if (currentCount + files.size() > 5) {

                throw new IllegalArgumentException(
                        "첨부파일은 기존 파일을 포함하여 최대 5개까지 가능합니다."
                );
            }

            Files.createDirectories(uploadPath);

            List<ApprovalDocumentAttachment> result =
                    new ArrayList<>();


            for (MultipartFile file : files) {

                String originalName =
                        file.getOriginalFilename();


                if (originalName == null
                        || originalName.isBlank()) {

                    originalName =
                            "attachment";
                }


                /*
                 * 혹시 브라우저가 전체 경로를 보내더라도
                 * 순수 파일명만 사용
                 */
                originalName =
                        Paths.get(originalName)
                                .getFileName()
                                .toString();


                String extension = "";

                int dotIndex =
                        originalName.lastIndexOf(".");


                if (dotIndex >= 0) {

                    extension =
                            originalName.substring(
                                    dotIndex
                            );
                }


                String storedName =
                        documentId
                                + "_"
                                + UUID.randomUUID()
                                + extension;


                Path targetPath =
                        uploadPath
                                .resolve(storedName)
                                .normalize();


                if (!targetPath.startsWith(
                        uploadPath)) {

                    throw new IllegalArgumentException(
                            "잘못된 파일 경로입니다."
                    );
                }


                Files.copy(
                        file.getInputStream(),
                        targetPath,
                        StandardCopyOption.REPLACE_EXISTING
                );


                ApprovalDocumentAttachment attachment =
                        ApprovalDocumentAttachment
                                .builder()
                                .document(document)
                                .originalName(originalName)
                                .storedName(storedName)
                                .filePath(
                                        "uploads/approval-document"
                                )
                                .build();


                result.add(
                        attachmentRepository.save(
                                attachment
                        )
                );
            }


            return result;

        } catch (IOException e) {

            throw new IllegalStateException(
                    "첨부파일 저장에 실패했습니다.",
                    e
            );
        }
    }


    /*
     * 파일 1개 정보 조회
     *
     * attachmentId뿐 아니라 documentId도 함께 검사
     */
    @Transactional(readOnly = true)
    public ApprovalDocumentAttachment getAttachment(
            Long documentId,
            Long attachmentId) {

        return attachmentRepository
                .findByAttachmentIdAndDocument_DocumentId(
                        attachmentId,
                        documentId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "첨부파일을 찾을 수 없습니다."
                        )
                );
    }


    /*
     * 실제 파일 Resource
     */
    @Transactional(readOnly = true)
    public Resource loadAttachment(
            ApprovalDocumentAttachment attachment) {

        try {

            Path file =
                    uploadPath
                            .resolve(
                                    attachment.getStoredName()
                            )
                            .normalize();


            if (!file.startsWith(
                    uploadPath)) {

                throw new IllegalArgumentException(
                        "잘못된 파일 경로입니다."
                );
            }


            Resource resource =
                    new UrlResource(
                            file.toUri()
                    );


            if (!resource.exists()) {

                throw new IllegalStateException(
                        "첨부파일이 존재하지 않습니다."
                );
            }


            return resource;

        } catch (Exception e) {

            throw new IllegalStateException(
                    "첨부파일 조회에 실패했습니다.",
                    e
            );
        }
    }


    /*
     * 문서 삭제할 때 모든 첨부파일 제거
     */
    @Transactional
    public void deleteByDocumentId(
            Long documentId) {

        List<ApprovalDocumentAttachment> attachments =
                attachmentRepository
                        .findByDocument_DocumentIdOrderByAttachmentIdAsc(
                                documentId
                        );


        /*
         * 실제 파일부터 제거
         */
        for (ApprovalDocumentAttachment attachment
                : attachments) {

            deletePhysicalFile(
                    attachment
            );
        }


        /*
         * DB row 제거
         */
        attachmentRepository
                .deleteByDocument_DocumentId(
                        documentId
                );
    }


    private void deletePhysicalFile(
            ApprovalDocumentAttachment attachment) {

        try {

            Path file =
                    uploadPath
                            .resolve(
                                    attachment.getStoredName()
                            )
                            .normalize();


            if (!file.startsWith(
                    uploadPath)) {
                return;
            }


            Files.deleteIfExists(file);

        } catch (IOException e) {

            throw new IllegalStateException(
                    "첨부파일 삭제에 실패했습니다.",
                    e
            );
        }
    }


    /*
     * 첨부파일 검증
     */
    private void validateFiles(
            List<MultipartFile> files) {

        if (files == null
                || files.isEmpty()) {

            throw new IllegalArgumentException(
                    "첨부파일을 선택해주세요."
            );
        }


        /*
         * 최대 5개
         */
        if (files.size() > 5) {

            throw new IllegalArgumentException(
                    "첨부파일은 최대 5개까지 가능합니다."
            );
        }


        for (MultipartFile file : files) {

            if (file == null
                    || file.isEmpty()) {

                throw new IllegalArgumentException(
                        "빈 파일은 첨부할 수 없습니다."
                );
            }


            if (file.getSize()
                    > 10 * 1024 * 1024) {

                throw new IllegalArgumentException(
                        "파일 하나의 최대 크기는 10MB입니다."
                );
            }
        }
    }
}