package com.company.groupware.repository;

import com.company.groupware.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleRepository
        extends JpaRepository<Schedule, Long> {

    // 조회 기간과 겹치는 전체 부서의 일정 조회
    @Query("""
            select s
            from Schedule s
            join fetch s.employee
            join fetch s.department
            where s.startAt < :rangeEnd
              and s.endAt > :rangeStart
            order by s.startAt asc, s.scheduleId asc
            """)
    List<Schedule> findSchedulesInRange(
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd
    );
    // 종료 시각 = 다음 시작 시각이면 겹치지 않음.
    // 수정할 때는 자기 자신 제외, 취소된 일정은 제외.
    @Query("""
            select count(s)
            from Schedule s
            where (s.cancelled = false or s.cancelled is null)
              and s.startAt < :endAt
              and s.endAt > :startAt
              and (:excludedId is null or s.scheduleId <> :excludedId)
            """)
    long countConflicts(
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("excludedId") Long excludedId
    );
}
