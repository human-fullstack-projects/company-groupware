package com.company.groupware.service;

import com.company.groupware.dto.AnnualLeaveHistoryResponse;
import com.company.groupware.dto.AnnualLeaveSummaryResponse;
import com.company.groupware.entity.AnnualLeave;
import com.company.groupware.repository.AnnualLeaveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnualLeaveService {

    private static final BigDecimal TOTAL_ANNUAL_LEAVE =
            new BigDecimal("15.0");

    private final AnnualLeaveRepository annualLeaveRepository;


    /**
     * 직원의 연차 현황 조회
     */
    @Transactional(readOnly = true)
    public AnnualLeaveSummaryResponse getSummary(Long emplId) {

        List<AnnualLeave> annualLeaves =
                annualLeaveRepository.findAll();

        BigDecimal usedDays = BigDecimal.ZERO;
        BigDecimal pendingDays = BigDecimal.ZERO;

        for (AnnualLeave annualLeave : annualLeaves) {

            if (!annualLeave
                    .getDocument()
                    .getWriter()
                    .getEmplId()
                    .equals(emplId)) {

                continue;
            }

            String status =
                    annualLeave
                            .getDocument()
                            .getStatus();

            if ("APPROVED".equals(status)) {

                usedDays =
                        usedDays.add(
                                annualLeave.getLeaveDays()
                        );
            }

            if ("IN_PROGRESS".equals(status)) {

                pendingDays =
                        pendingDays.add(
                                annualLeave.getLeaveDays()
                        );
            }
        }

        BigDecimal remainingDays =
                TOTAL_ANNUAL_LEAVE.subtract(
                        usedDays
                );

        return new AnnualLeaveSummaryResponse(
                TOTAL_ANNUAL_LEAVE,
                usedDays,
                pendingDays,
                remainingDays
        );
    }


    /**
     * 직원의 연차 신청 내역 조회
     */
    @Transactional(readOnly = true)
    public List<AnnualLeaveHistoryResponse> getHistory(Long emplId) {

        return annualLeaveRepository
                .findAll()
                .stream()

                // 로그인한 직원이 작성한 연차만
                .filter(annualLeave ->
                        annualLeave
                                .getDocument()
                                .getWriter()
                                .getEmplId()
                                .equals(emplId)
                )

                // 최근 신청한 연차부터
                .sorted(
                        Comparator.comparing(
                                (AnnualLeave annualLeave) ->
                                        annualLeave
                                                .getDocument()
                                                .getCreatedAt(),
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
                )

                .map(annualLeave ->
                        new AnnualLeaveHistoryResponse(
                                annualLeave
                                        .getDocument()
                                        .getDocumentId(),

                                annualLeave.getLeaveType(),

                                annualLeave.getStartDate(),

                                annualLeave.getEndDate(),

                                annualLeave.getLeaveDays(),

                                annualLeave
                                        .getDocument()
                                        .getStatus()
                        )
                )

                .toList();
    }
}