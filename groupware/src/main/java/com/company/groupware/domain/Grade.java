package com.company.groupware.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 직급
 */
@Entity
@Table(name = "grade")
@Getter
@Setter
@NoArgsConstructor
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grade_id")
    private Integer gradeId; // 직급번호

    @Column(name = "grade_name", nullable = false, length = 50)
    private String gradeName; // 직급명
}
