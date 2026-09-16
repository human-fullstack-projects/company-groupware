package com.company.groupware.service;

import com.company.groupware.dto.AnnualLeaveRequest;
import com.company.groupware.dto.ApprovalDocumentRequest;
import com.company.groupware.dto.ApprovalDocumentResponse;
import com.company.groupware.dto.ApprovalStepResponse;
import com.company.groupware.entity.*;
import com.company.groupware.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
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

    private final ApprovalDocumentAttachmentService approvalDocumentAttachmentService;

    private final AnnualLeaveRepository annualLeaveRepository;


    /**
     * 문서 상신
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
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다."
                        ));

        ApprovalLine approvalLine = approvalLineRepository
                .findById(request.getApprovalLineId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "결재라인을 찾을 수 없습니다."
                        ));

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

        // 휴가 신청서라면 저장 전에 휴가 정보 검증
        if (request.getDocumentType() == ApprovalDocumentType.VACATION) {
            validateAnnualLeave(request.getAnnualLeave());
        }

        LocalDateTime now = LocalDateTime.now();

        ApprovalDocument document = ApprovalDocument.builder()
                .writer(writer)
                .title(request.getTitle())
                .content(request.getContent())
                .documentType(
                        request.getDocumentType() != null
                                ? request.getDocumentType()
                                : ApprovalDocumentType.GENERAL
                )
                .approvalLineName(approvalLine.getLineName())
                .status("IN_PROGRESS")
                .submittedAt(now)
                .build();

        approvalDocumentRepository.save(document);

        // 휴가 신청서 상세정보 저장
        saveAnnualLeave(document, request);

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

        // 휴가 신청서라면 휴가 정보 검증
        if (request.getDocumentType() == ApprovalDocumentType.VACATION) {
            validateAnnualLeave(request.getAnnualLeave());
        }

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
                        .documentType(
                                request.getDocumentType() != null
                                        ? request.getDocumentType()
                                        : ApprovalDocumentType.GENERAL
                        )
                        .status("DRAFT")
                        .build();

        approvalDocumentRepository.save(document);

        // 휴가 신청서 상세정보 저장
        saveAnnualLeave(document, request);

        return toResponse(document);
    }


    /**
     * 휴가 상세정보 저장
     */
    private void saveAnnualLeave(
            ApprovalDocument document,
            ApprovalDocumentRequest request) {

        if (document.getDocumentType() != ApprovalDocumentType.VACATION) {
            return;
        }

        AnnualLeaveRequest leaveRequest =
                request.getAnnualLeave();

        validateAnnualLeave(leaveRequest);

        LocalDate startDate =
                leaveRequest.getStartDate();

        LocalDate endDate =
                leaveRequest.getEndDate();

        LeaveType leaveType =
                leaveRequest.getLeaveType();

        BigDecimal leaveDays;


        /*
         * 오전/오후 반차
         */
        if (leaveType == LeaveType.AM_HALF
                || leaveType == LeaveType.PM_HALF) {

            if (isWeekend(startDate)) {
                throw new IllegalArgumentException(
                        "주말에는 반차를 신청할 수 없습니다."
                );
            }

            // 반차는 무조건 같은 날짜
            endDate = startDate;

            leaveDays = new BigDecimal("0.5");

        } else {

            /*
             * 연차
             * 토/일 제외
             */
            int weekdayCount =
                    countWeekdays(
                            startDate,
                            endDate
                    );

            if (weekdayCount <= 0) {
                throw new IllegalArgumentException(
                        "선택한 기간에 사용 가능한 평일이 없습니다."
                );
            }

            leaveDays =
                    BigDecimal.valueOf(
                            weekdayCount
                    );
        }


        AnnualLeave annualLeave =
                AnnualLeave.builder()
                        .document(document)
                        .leaveType(leaveType)
                        .startDate(startDate)
                        .endDate(endDate)
                        .leaveDays(leaveDays)
                        .build();

        annualLeaveRepository.save(annualLeave);
    }


    /**
     * 휴가 입력값 검증
     */
    private void validateAnnualLeave(
            AnnualLeaveRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "휴가 정보를 입력해주세요."
            );
        }

        if (request.getLeaveType() == null) {
            throw new IllegalArgumentException(
                    "휴가 종류를 선택해주세요."
            );
        }

        if (request.getStartDate() == null) {
            throw new IllegalArgumentException(
                    "휴가 시작일을 선택해주세요."
            );
        }


        /*
         * 반차
         */
        if (request.getLeaveType() == LeaveType.AM_HALF
                || request.getLeaveType() == LeaveType.PM_HALF) {

            return;
        }


        /*
         * 연차
         */
        if (request.getEndDate() == null) {
            throw new IllegalArgumentException(
                    "휴가 종료일을 선택해주세요."
            );
        }

        if (request.getEndDate()
                .isBefore(request.getStartDate())) {

            throw new IllegalArgumentException(
                    "휴가 종료일은 시작일보다 빠를 수 없습니다."
            );
        }
    }


    /**
     * 시작일 ~ 종료일 중 평일 수 계산
     */
    private int countWeekdays(
            LocalDate startDate,
            LocalDate endDate) {

        int count = 0;

        LocalDate date =
                startDate;

        while (!date.isAfter(endDate)) {

            if (!isWeekend(date)) {
                count++;
            }

            date =
                    date.plusDays(1);
        }

        return count;
    }


    /**
     * 토/일 확인
     */
    private boolean isWeekend(
            LocalDate date) {

        DayOfWeek dayOfWeek =
                date.getDayOfWeek();

        return dayOfWeek == DayOfWeek.SATURDAY
                || dayOfWeek == DayOfWeek.SUNDAY;
    }


    /**
     * 내가 상신한 문서
     */
    @Transactional(readOnly = true)
    public Page<ApprovalDocumentResponse> getMyDocuments(
            Long emplId,
            int page,
            int size) {

        Pageable pageable =
                PageRequest.of(page, size);

        return approvalDocumentRepository
                .findByWriter_EmplIdOrderByCreatedAtDesc(
                        emplId,
                        pageable
                )
                .map(this::toResponse);
    }


    /**
     * 현재 내가 결재해야 하는 문서
     */
    @Transactional(readOnly = true)
    public List<ApprovalDocumentResponse> getPendingDocuments(
            Long emplId) {

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
     */
    @Transactional(readOnly = true)
    public ApprovalDocumentResponse getDocument(
            Long documentId,
            Long emplId) {

        ApprovalDocument document =
                getDocumentEntity(documentId);

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

        ApprovalDocument document =
                getDocumentEntity(documentId);

        if (!"IN_PROGRESS".equals(
                document.getStatus())) {

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
        currentMember.setApprovedAt(
                LocalDateTime.now()
        );
        currentMember.setComment(comment);

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

            document.setStatus("APPROVED");
            document.setCompletedAt(
                    LocalDateTime.now()
            );
        }

        return toResponse(document);
    }


    /**
     * 반려
     */
    @Transactional
    public ApprovalDocumentResponse rejectDocument(
            Long documentId,
            Long emplId,
            String comment) {

        ApprovalDocument document =
                getDocumentEntity(documentId);

        if (!"IN_PROGRESS".equals(
                document.getStatus())) {

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
        currentMember.setApprovedAt(
                LocalDateTime.now()
        );
        currentMember.setComment(comment);

        document.setStatus("REJECTED");
        document.setCompletedAt(
                LocalDateTime.now()
        );

        return toResponse(document);
    }


    /**
     * 문서 수정
     */
    @Transactional
    public ApprovalDocumentResponse updateDocument(
            Long documentId,
            Long emplId,
            ApprovalDocumentRequest request) {

        ApprovalDocument document =
                getDocumentEntity(documentId);

        if (!Objects.equals(
                document.getWriter().getEmplId(),
                emplId)) {

            throw new IllegalArgumentException(
                    "본인이 작성한 문서만 수정할 수 있습니다."
            );
        }

        if (hasApprovalAction(documentId)) {
            throw new IllegalArgumentException(
                    "결재가 진행된 문서는 수정할 수 없습니다."
            );
        }

        if ("APPROVED".equals(document.getStatus())
                || "REJECTED".equals(document.getStatus())) {

            throw new IllegalArgumentException(
                    "결재가 종료된 문서는 수정할 수 없습니다."
            );
        }

        document.setTitle(
                request.getTitle()
        );

        document.setContent(
                request.getContent()
        );

        return toResponse(document);
    }


    /**
     * 문서 제거
     */
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

        approvalDocumentAttachmentService
                .deleteByDocumentId(documentId);

        approvalDocumentMemberRepository
                .deleteByDocument_DocumentId(
                        documentId
                );

        /*
         * 휴가 상세정보가 있다면 먼저 삭제
         */
        annualLeaveRepository
                .findByDocument_DocumentId(
                        documentId
                )
                .ifPresent(
                        annualLeaveRepository::delete
                );

        approvalDocumentRepository
                .delete(document);
    }


    private ApprovalDocument getDocumentEntity(
            Long documentId) {

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
                .documentId(
                        document.getDocumentId()
                )
                .writerEmplId(
                        document.getWriter().getEmplId()
                )
                .writerName(
                        document.getWriter().getEmplName()
                )
                .title(
                        document.getTitle()
                )
                .content(
                        document.getContent()
                )
                .status(
                        document.getStatus()
                )
                .approvalLineName(
                        document.getApprovalLineName()
                )
                .createdAt(
                        document.getCreatedAt()
                )
                .submittedAt(
                        document.getSubmittedAt()
                )
                .completedAt(
                        document.getCompletedAt()
                )
                .approvers(
                        approvers
                )
                .build();
    }


    /**
     * 처리 시작 여부 확인
     */
    private boolean hasApprovalAction(
            Long documentId) {

        return approvalDocumentMemberRepository
                .findByDocument_DocumentIdOrderByApprovalOrderAsc(
                        documentId
                )
                .stream()
                .anyMatch(member ->
                        "APPROVED".equals(
                                member.getStatus()
                        )
                                || "REJECTED".equals(
                                member.getStatus()
                        )
                );
    }
}