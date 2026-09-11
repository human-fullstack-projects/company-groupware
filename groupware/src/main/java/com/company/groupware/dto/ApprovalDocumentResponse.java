package com.company.groupware.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ApprovalDocumentResponse {

    private Long documentId;

    private Long writerEmplId;
    private String writerName;

    private String title;
    private String content;
    private String status;

    // 추가
    private String approvalLineName;

    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;

    private List<ApprovalStepResponse> approvers;
}
