package com.company.groupware.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class AnnualLeaveSummaryResponse {

    // 총 연차
    private BigDecimal totalDays;

    // 사용 완료 연차 (APPROVED)
    private BigDecimal usedDays;

    // 현재 결재중 연차 (IN_PROGRESS)
    private BigDecimal pendingDays;

    // 잔여 연차
    private BigDecimal remainingDays;
}