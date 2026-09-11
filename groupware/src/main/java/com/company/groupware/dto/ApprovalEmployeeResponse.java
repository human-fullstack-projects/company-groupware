package com.company.groupware.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApprovalEmployeeResponse {

    private Long emplId;
    private String emplName;
}