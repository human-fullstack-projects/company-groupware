package com.company.groupware.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 출퇴근 기록
 * - 기존 empl_id 단독 PK(직원당 1행) 구조에서, 일자별 이력을 쌓을 수 있도록
 *   surrogate key(commute_id)로 변경하고 empl_id는 일반 FK로 전환함
 * - 같은 직원이 같은 날 중복 기록되지 않도록 (empl_id, attendance_date) 유니크 제약
 */
@Entity
@Table(
        name = "commute",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_commute_empl_date",
                columnNames = {"empl_id", "attendance_date"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "commute_id")
    @Setter(AccessLevel.NONE)
    private Long commuteId; // 출퇴근기록번호

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empl_id", foreignKey = @ForeignKey(name = "FK_employee_TO_commute"))
    private Employee employee; // 직원

    @Column(name = "start_time", length = 50)
    private String startTime; // 출근시간

    @Column(name = "finish_time", length = 50)
    private String finishTime; // 퇴근시간

    @Column(name = "attendance_date")
    private LocalDate attendanceDate; // 출근일자
}