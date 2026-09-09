package com.company.groupware.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 직급
 */
@Entity
@Table(name = "grade")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grade_id")
    private Long gradeId; // 직급번호

    @Column(name = "grade_name", length = 50, nullable = false)
    private String gradeName; // 직급명
}
