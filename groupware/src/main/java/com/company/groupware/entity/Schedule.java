package com.company.groupware.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "calendar_schedule")
@Getter
@Setter
@NoArgsConstructor
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    // 최초 작성 직원
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empl_id", nullable = false)
    private Employee employee;

    // 등록 당시 담당 부서
    // 작성 직원이 부서를 옮겨도 일정의 담당 부서는 유지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dept_id", nullable = false)
    private Department department;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", length = 2000)
    private String content;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    // 취소 시 삭제하지 않고 기록을 유지
    @Column(name = "cancelled")
    private Boolean cancelled = false;

    // 같은 일정을 동시에 수정할 때 덮어쓰기 방지에 사용
    @Version
    private Long version;
}
