package com.company.groupware.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ApprovalStepResponse {

    private Long documentMemberId;
    private Long approverId;
    private String approverName;
    private int approvalOrder;
    private String status;
    private LocalDateTime approvedAt;
    private String comment;
}
