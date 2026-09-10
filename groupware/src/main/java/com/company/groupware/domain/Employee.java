package com.company.groupware.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 직원
 */
//@Entity
//@Table(name = "employee")
@Getter
@Setter
@NoArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "empl_id")
    private Integer emplId; // 사번

    @Column(name = "empl_name", nullable = false, length = 50)
    private String emplName; // 이름

    @Column(name = "empl_phone", length = 50)
    private String emplPhone; // 연락처

    @Column(name = "empl_email", length = 50)
    private String emplEmail; // 이메일

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_id")
    private Grade grade; // 직급

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_id")
    private Department department; // 부서

    @Column(name = "empl_stat")
    private Boolean emplStat = false; // 관리자여부
}