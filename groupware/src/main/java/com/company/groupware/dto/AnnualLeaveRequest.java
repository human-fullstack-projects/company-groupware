package com.company.groupware.dto;

import com.company.groupware.entity.LeaveType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AnnualLeaveRequest {

    // ANNUAL / AM_HALF / PM_HALF
    private LeaveType leaveType;

    private LocalDate startDate;

    private LocalDate endDate;
}