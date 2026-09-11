package com.company.groupware.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ApprovalLineRequest {

    private String lineName;

    private List<Long> approverIds;
}