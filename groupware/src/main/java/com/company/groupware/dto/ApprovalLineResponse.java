package com.company.groupware.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ApprovalLineResponse {

    private Long lineId;
    private String lineName;

    private List<ApproverResponse> approvers;
}