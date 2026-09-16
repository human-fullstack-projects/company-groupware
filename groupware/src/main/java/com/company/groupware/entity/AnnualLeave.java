package com.company.groupware.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "annual_leave",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_annual_leave_document",
                        columnNames = "document_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnualLeave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_id")
    private Long leaveId;

    /*
     * 어떤 전자결재 문서의 연차 신청인지 연결
     *
     * AnnualLeave 1개 ↔ ApprovalDocument 1개
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "document_id",
            nullable = false,
            unique = true
    )
    private ApprovalDocument document;

    /*
     * 연차 종류
     *
     * ANNUAL  : 연차
     * AM_HALF : 오전 반차
     * PM_HALF : 오후 반차
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 20)
    private LeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /*
     * 사용 일수
     *
     * 연차      : 1.0 이상
     * 오전 반차 : 0.5
     * 오후 반차 : 0.5
     */
    @Column(
            name = "leave_days",
            nullable = false,
            precision = 4,
            scale = 1
    )
    private BigDecimal leaveDays;
}