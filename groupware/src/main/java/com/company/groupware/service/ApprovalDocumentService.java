package com.company.groupware.service;

import com.company.groupware.dto.ApprovalDocumentRequest;
import com.company.groupware.dto.ApprovalDocumentResponse;
import com.company.groupware.dto.ApprovalStepResponse;
import com.company.groupware.entity.*;
import com.company.groupware.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ApprovalDocumentService {

    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final ApprovalDocumentMemberRepository approvalDocumentMemberRepository;

    private final ApprovalLineRepository approvalLineRepository;
    private final ApprovalLineMemberRepository approvalLineMemberRepository;

    private final EmployeeRepository employeeRepository;


    /**
     * 문서 상신
     *
     * 1. 로그인 사용자 확인
     * 2. 본인이 만든 개인 결재라인인지 확인
     * 3. 문서 저장
     * 4. 개인 결재라인의 결재자들을 문서 결재자 테이블로 복사
     * 5. 첫 번째 결재자만 PENDING, 나머지는 WAITING
     */
    @Transactional
    public ApprovalDocumentResponse submitDocument(
            Long writerEmplId,
            ApprovalDocumentRequest request) {

        if (request.getApprovalLineId() == null) {
            throw new IllegalArgumentException(
                    "결재라인을 선택해주세요."
            );
        }

        Employee writer = employeeRepository.findById(writerEmplId)
                .orElseThrow(() ->
                        new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        ApprovalLine approvalLine = approvalLineRepository
                .findById(request.getApprovalLineId())
                .orElseThrow(() ->
                        new IllegalArgumentException("결재라인을 찾을 수 없습니다."));

        // 다른 사람의 개인 결재라인 사용 방지
        if (!Objects.equals(
                approvalLine.getOwner().getEmplId(),
                writerEmplId)) {

            throw new IllegalArgumentException(
                    "본인의 결재라인만 사용할 수 있습니다."
            );
        }

        List<ApprovalLineMember> lineMembers =
                approvalLineMemberRepository
                        .findByApprovalLine_LineIdOrderByApprovalOrderAsc(
                                approvalLine.getLineId()
                        );

        if (lineMembers.isEmpty()) {
            throw new IllegalArgumentException(
                    "결재자가 등록되지 않은 결재라인입니다."
            );
        }

        LocalDateTime now = LocalDateTime.now();

        ApprovalDocument document = ApprovalDocument.builder()
                .writer(writer)
                .title(request.getTitle())
                .content(request.getContent())
                .approvalLineName(approvalLine.getLineName())
                .status("IN_PROGRESS")
                .submittedAt(now)
                .build();

        approvalDocumentRepository.save(document);

        for (int i = 0; i < lineMembers.size(); i++) {

            ApprovalLineMember lineMember = lineMembers.get(i);

            ApprovalDocumentMember documentMember =
                    ApprovalDocumentMember.builder()
                            .document(document)
                            .approver(lineMember.getApprover())
                            .approvalOrder(lineMember.getApprovalOrder())
                            .status(i == 0 ? "PENDING" : "WAITING")
                            .build();

            approvalDocumentMemberRepository.save(documentMember);
        }

        return toResponse(document);
    }

    /**
     * 문서 임시저장
     *
     * 결재라인 없이 저장 가능
     * 결재자 테이블은 생성하지 않음
     */
    @Transactional
    public ApprovalDocumentResponse saveDraft(
            Long writerEmplId,
            ApprovalDocumentRequest request) {

        Employee writer = employeeRepository.findById(writerEmplId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다."
                        ));

        ApprovalDocument document =
                ApprovalDocument.builder()
                        .writer(writer)
                        .title(
                                request.getTitle() == null
                                        ? ""
                                        : request.getTitle()
                        )
                        .content(
                                request.getContent() == null
                                        ? ""
                                        : request.getContent()
                        )
                        .status("DRAFT")
                        .build();

        approvalDocumentRepository.save(document);

        return toResponse(document);
    }


    /**
     * 내가 상신한 문서
     */
    @Transactional(readOnly = true)
    public List<ApprovalDocumentResponse> getMyDocuments(Long emplId) {

        return approvalDocumentRepository
                .findByWriter_EmplIdOrderByCreatedAtDesc(emplId)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    /**
     * 현재 내가 결재해야 하는 문서
     */
    @Transactional(readOnly = true)
    public List<ApprovalDocumentResponse> getPendingDocuments(Long emplId) {
        return approvalDocumentMemberRepository
                .findByApprover_EmplIdAndStatusOrderByDocument_CreatedAtDesc(
                        emplId,
                        "PENDING"
                )
                .stream()
                .map(ApprovalDocumentMember::getDocument)
                .map(this::toResponse)
                .toList();
    }


    /**
     * 문서 상세 조회
     *
     * 작성자는 항상 조회 가능.
     * 결재자는 자신의 순서가 아직 오지 않은 WAITING 상태라면 조회를 막습니다.
     */
    @Transactional(readOnly = true)
    public ApprovalDocumentResponse getDocument(
            Long documentId,
            Long emplId) {

        ApprovalDocument document = getDocumentEntity(documentId);

        // 작성자라면 바로 조회 가능
        if (Objects.equals(
                document.getWriter().getEmplId(),
                emplId)) {

            return toResponse(document);
        }

        ApprovalDocumentMember myStep =
                approvalDocumentMemberRepository
                        .findByDocument_DocumentIdAndApprover_EmplId(
                                documentId,
                                emplId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "해당 문서를 조회할 권한이 없습니다."
                                ));

        if ("WAITING".equals(myStep.getStatus())) {
            throw new IllegalArgumentException(
                    "아직 결재 순서가 아닙니다."
            );
        }

        return toResponse(document);
    }


    /**
     * 승인
     */
    @Transactional
    public ApprovalDocumentResponse approveDocument(
            Long documentId,
            Long emplId,
            String comment) {

        ApprovalDocument document = getDocumentEntity(documentId);

        if (!"IN_PROGRESS".equals(document.getStatus())) {
            throw new IllegalArgumentException(
                    "이미 결재가 종료된 문서입니다."
            );
        }

        ApprovalDocumentMember currentMember =
                approvalDocumentMemberRepository
                        .findByDocument_DocumentIdAndApprover_EmplIdAndStatus(
                                documentId,
                                emplId,
                                "PENDING"
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "현재 결재 순서가 아닙니다."
                                ));

        currentMember.setStatus("APPROVED");
        currentMember.setApprovedAt(LocalDateTime.now());
        currentMember.setComment(comment);

        // 다음 WAITING 결재자를 찾음
        ApprovalDocumentMember nextMember =
                approvalDocumentMemberRepository
                        .findFirstByDocument_DocumentIdAndStatusOrderByApprovalOrderAsc(
                                documentId,
                                "WAITING"
                        )
                        .orElse(null);

        if (nextMember != null) {
            nextMember.setStatus("PENDING");
        } else {
            // 더 이상 결재자가 없으면 최종 승인
            document.setStatus("APPROVED");
            document.setCompletedAt(LocalDateTime.now());
        }

        return toResponse(document);
    }


    /**
     * 반려
     * 반려되는 순간 문서 전체 결재 종료
     */
    @Transactional
    public ApprovalDocumentResponse rejectDocument(
            Long documentId,
            Long emplId,
            String comment) {

        ApprovalDocument document = getDocumentEntity(documentId);

        if (!"IN_PROGRESS".equals(document.getStatus())) {
            throw new IllegalArgumentException(
                    "이미 결재가 종료된 문서입니다."
            );
        }

        ApprovalDocumentMember currentMember =
                approvalDocumentMemberRepository
                        .findByDocument_DocumentIdAndApprover_EmplIdAndStatus(
                                documentId,
                                emplId,
                                "PENDING"
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "현재 결재 순서가 아닙니다."
                                ));

        currentMember.setStatus("REJECTED");
        currentMember.setApprovedAt(LocalDateTime.now());
        currentMember.setComment(comment);

        document.setStatus("REJECTED");
        document.setCompletedAt(LocalDateTime.now());

        return toResponse(document);
    }

    @Transactional
    public ApprovalDocumentResponse updateDocument(
            Long documentId,
            Long emplId,
            ApprovalDocumentRequest request) {

        ApprovalDocument document =
                getDocumentEntity(documentId);


        // 작성자 확인
        if (!Objects.equals(
                document.getWriter().getEmplId(),
                emplId)) {

            throw new IllegalArgumentException(
                    "본인이 작성한 문서만 수정할 수 있습니다."
            );
        }


        // 이미 누군가 승인/반려함
        if (hasApprovalAction(documentId)) {
            throw new IllegalArgumentException(
                    "결재가 진행된 문서는 수정할 수 없습니다."
            );
        }

        // 종료된 문서 방어
        if ("APPROVED".equals(document.getStatus())||"REJECTED".equals(document.getStatus())) {
            throw new IllegalArgumentException(
                    "결재가 종료된 문서는 수정할 수 없습니다."
            );
        }

        document.setTitle(request.getTitle());
        document.setContent(request.getContent());


        return toResponse(document);
    }


    //문서 제거 (처리 전에만 가능)
    @Transactional
    public void deleteDocument(
            Long documentId,
            Long emplId) {

        ApprovalDocument document =
                getDocumentEntity(documentId);


        if (!Objects.equals(
                document.getWriter().getEmplId(),
                emplId)) {

            throw new IllegalArgumentException(
                    "본인이 작성한 문서만 삭제할 수 있습니다."
            );
        }


        if (hasApprovalAction(documentId)) {

            throw new IllegalArgumentException(
                    "결재가 진행된 문서는 삭제할 수 없습니다."
            );
        }


        if ("APPROVED".equals(document.getStatus())
                || "REJECTED".equals(document.getStatus())) {

            throw new IllegalArgumentException(
                    "결재가 종료된 문서는 삭제할 수 없습니다."
            );
        }


        /*
         * 상신한 문서는 approval_document_member가 있으므로
         * 자식부터 삭제
         */
        approvalDocumentMemberRepository
                .deleteByDocument_DocumentId(documentId);

        approvalDocumentRepository.delete(document);
    }


    private ApprovalDocument getDocumentEntity(Long documentId) {

        return approvalDocumentRepository
                .findById(documentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "전자결재 문서를 찾을 수 없습니다."
                        ));
    }


    /**
     * Entity -> Response DTO
     */
    private ApprovalDocumentResponse toResponse(
            ApprovalDocument document) {

        List<ApprovalStepResponse> approvers =
                approvalDocumentMemberRepository
                        .findByDocument_DocumentIdOrderByApprovalOrderAsc(
                                document.getDocumentId()
                        )
                        .stream()
                        .map(member ->
                                ApprovalStepResponse.builder()
                                        .documentMemberId(
                                                member.getDocumentMemberId()
                                        )
                                        .approverId(
                                                member.getApprover().getEmplId()
                                        )
                                        .approverName(
                                                member.getApprover().getEmplName()
                                        )
                                        .approvalOrder(
                                                member.getApprovalOrder()
                                        )
                                        .status(
                                                member.getStatus()
                                        )
                                        .approvedAt(
                                                member.getApprovedAt()
                                        )
                                        .comment(
                                                member.getComment()
                                        )
                                        .build()
                        )
                        .toList();

        return ApprovalDocumentResponse.builder()
                .documentId(document.getDocumentId())
                .writerEmplId(document.getWriter().getEmplId())
                .writerName(document.getWriter().getEmplName())
                .title(document.getTitle())
                .content(document.getContent())
                .status(document.getStatus())
                .approvalLineName(document.getApprovalLineName())
                .createdAt(document.getCreatedAt())
                .submittedAt(document.getSubmittedAt())
                .completedAt(document.getCompletedAt())
                .approvers(approvers)
                .build();
    }

    // 처리중인 문서인지 확인
    private boolean hasApprovalAction(Long documentId) {

        return approvalDocumentMemberRepository
                .findByDocument_DocumentIdOrderByApprovalOrderAsc(documentId)
                .stream()
                .anyMatch(member ->
                        "APPROVED".equals(member.getStatus())
                                || "REJECTED".equals(member.getStatus())
                );
    }
}
