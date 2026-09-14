package com.company.groupware.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 직원
 */
@Entity
@Table(name = "employee", indexes = {
        @Index(name = "IX_employee", columnList = "empl_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "empl_id")
    private Long emplId; // 사번

    @Column(name = "empl_name", length = 50, nullable = false)
    private String emplName; // 이름

    @Column(name = "empl_phone", length = 50)
    private String emplPhone; // 연락처

    @Column(name = "empl_email", length = 50)
    private String emplEmail; // 이메일

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_id", foreignKey = @ForeignKey(name = "FK_grade_TO_employee"))
    private Grade grade; // 직급

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_id", foreignKey = @ForeignKey(name = "FK_department_TO_employee"))
    private Department department; // 부서

    @Column(name = "empl_stat")
    @Builder.Default
    private Boolean emplStat = false; // 관리자여부

    //직원 정보에 로그인용 아이디, 비번 저장할수 있는 항목 추가

    @Column(name = "login_id", nullable = false, unique = true, length = 50)
    private String loginId; // 로그인 아이디

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash; // 해싱된 비밀번호

}
