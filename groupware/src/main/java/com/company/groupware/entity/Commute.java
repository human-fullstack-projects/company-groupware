package com.company.groupware.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 출퇴근
 * empl_id 가 PK 이면서 employee 테이블을 참조하는 FK 이므로
 * @MapsId 로 직원과 공유 기본키(shared PK) 1:1 관계로 매핑
 */
@Entity
@Table(name = "commute")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commute {

    @Id
    @Column(name = "empl_id")
    private Long emplId; // 사번 (PK / FK)

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "empl_id", foreignKey = @ForeignKey(name = "FK_employee_TO_commute"))
    private Employee employee;

    // 원본 DDL 상 VARCHAR(50) 로 정의되어 있어 String 으로 매핑함
    // (시간 계산이 필요하면 DB 컬럼 타입을 TIME/DATETIME 으로 변경 후 LocalTime 등으로 매핑 권장)
    @Column(name = "start_time", length = 50)
    private String startTime; // 출근시간

    @Column(name = "finish_time", length = 50)
    private String finishTime; // 퇴근시간

    @Column(name = "COL")
    private LocalDate attendanceDate; // 출근일자 (원본 컬럼명: COL)
}
