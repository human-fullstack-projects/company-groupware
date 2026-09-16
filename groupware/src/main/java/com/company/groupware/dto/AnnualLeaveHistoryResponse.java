package com.company.groupware.dto;

import com.company.groupware.entity.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class AnnualLeaveHistoryResponse {

    private Long documentId;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal leaveDays;
    private String status;
}