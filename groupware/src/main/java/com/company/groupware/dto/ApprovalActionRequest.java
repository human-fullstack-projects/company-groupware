package com.company.groupware.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApprovalActionRequest {

    @Size(max = 500, message = "결재 의견은 500자 이내로 입력해주세요.")
    private String comment;
}
