package com.company.groupware.dto;

import com.company.groupware.entity.ApprovalDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApprovalDocumentRequest {

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 200, message = "제목은 200자 이내로 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    // 결재라인
    private Long approvalLineId;

    /*
     * 문서 종류
     *
     * GENERAL  : 일반 문서
     * VACATION : 휴가 신청서
     * WORKLOG  : 업무일지
     * PROPOSAL : 품의서
     */
    private ApprovalDocumentType documentType;

    /*
     * 휴가 신청서일 때만 사용
     *
     * 일반 문서에서는 null
     */
    private AnnualLeaveRequest annualLeave;
}