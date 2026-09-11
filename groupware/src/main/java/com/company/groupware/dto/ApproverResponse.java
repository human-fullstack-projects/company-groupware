package com.company.groupware.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApproverResponse {

    private Long emplId;
    private String emplName;
    private String position;
    private String department;
    private int approvalOrder;
}