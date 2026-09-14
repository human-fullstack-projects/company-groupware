package com.company.groupware.service;

import com.company.groupware.entity.ApprovalSignature;
import com.company.groupware.repository.ApprovalSignatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApprovalSignatureService {

    private final ApprovalSignatureRepository signatureRepository;

    private final Path uploadPath =
            Paths.get("uploads", "signature")
                    .toAbsolutePath()
                    .normalize();


    /*
     * 내 사인 조회
     */
    @Transactional(readOnly = true)
    public Optional<ApprovalSignature> getSignature(Long emplId) {

        return signatureRepository.findByEmplId(emplId);
    }


    /*
     * 신규 등록 / 기존 사인 교체
     */
    @Transactional
    public ApprovalSignature saveSignature(
            Long emplId,
            MultipartFile file) {

        validateImage(file);

        try {

            Files.createDirectories(uploadPath);


            String originalName =
                    file.getOriginalFilename();

            String extension = "";

            if (originalName != null &&
                    originalName.lastIndexOf(".") >= 0) {

                extension =
                        originalName.substring(
                                originalName.lastIndexOf(".")
                        );
            }


            String savedName =
                    emplId
                            + "_"
                            + UUID.randomUUID()
                            + extension;


            Path targetPath =
                    uploadPath.resolve(savedName)
                            .normalize();


            if (!targetPath.startsWith(uploadPath)) {

                throw new IllegalArgumentException(
                        "잘못된 파일 경로입니다."
                );
            }


            /*
             * 기존 사인이 있다면
             * 이전 실제 파일 삭제
             */
            Optional<ApprovalSignature> existing =
                    signatureRepository.findByEmplId(emplId);

            if (existing.isPresent()) {

                deletePhysicalFile(
                        existing.get()
                );
            }


            Files.copy(
                    file.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );


            ApprovalSignature signature =
                    existing.orElseGet(
                            ApprovalSignature::new
                    );


            signature.setEmplId(emplId);

            signature.setFilePath(
                    "uploads/signature"
            );

            signature.setFileName(
                    savedName
            );


            return signatureRepository.save(
                    signature
            );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "사인 이미지 저장에 실패했습니다.",
                    e
            );
        }
    }


    /*
     * 삭제
     */
    @Transactional
    public void deleteSignature(Long emplId) {

        ApprovalSignature signature =
                signatureRepository
                        .findByEmplId(emplId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "등록된 사인이 없습니다."
                                )
                        );


        deletePhysicalFile(signature);

        signatureRepository.delete(signature);
    }


    /*
     * 이미지 Resource
     */
    @Transactional(readOnly = true)
    public Resource loadSignature(Long emplId) {

        ApprovalSignature signature =
                signatureRepository
                        .findByEmplId(emplId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "등록된 사인이 없습니다."
                                )
                        );

        try {

            Path file =
                    uploadPath.resolve(
                            signature.getFileName()
                    ).normalize();


            Resource resource =
                    new UrlResource(
                            file.toUri()
                    );


            if (!resource.exists()) {

                throw new IllegalStateException(
                        "사인 이미지 파일이 없습니다."
                );
            }


            return resource;

        } catch (Exception e) {

            throw new IllegalStateException(
                    "사인 이미지 조회에 실패했습니다.",
                    e
            );
        }
    }


    private void deletePhysicalFile(
            ApprovalSignature signature) {

        try {

            Path file =
                    uploadPath.resolve(
                            signature.getFileName()
                    ).normalize();

            Files.deleteIfExists(file);

        } catch (IOException e) {

            throw new IllegalStateException(
                    "기존 사인 이미지 삭제에 실패했습니다.",
                    e
            );
        }
    }


    private void validateImage(
            MultipartFile file) {

        if (file == null || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "이미지를 선택해주세요."
            );
        }


        String contentType =
                file.getContentType();

        if (contentType == null ||
                !contentType.startsWith("image/")) {

            throw new IllegalArgumentException(
                    "이미지 파일만 등록할 수 있습니다."
            );
        }


        // 사인인데 5MB 넘을 이유는 거의 없음
        if (file.getSize() > 5 * 1024 * 1024) {

            throw new IllegalArgumentException(
                    "이미지는 5MB 이하만 등록할 수 있습니다."
            );
        }
    }
}