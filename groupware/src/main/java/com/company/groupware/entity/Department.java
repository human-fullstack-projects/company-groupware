package com.company.groupware.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 부서
 */
@Entity
@Table(name = "department")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dept_id")
    private Long deptId; // 부서번호

    @Column(name = "dept_name", length = 50, nullable = false)
    private String deptName; // 부서명
}
