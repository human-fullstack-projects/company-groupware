package com.company.groupware.repository;

import com.company.groupware.entity.Commute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CommuteRepository extends JpaRepository<Commute, Long> {
    // 특정 직원의 특정 날짜 출퇴근 기록 (중복 출근 체크, 퇴근 처리 시 사용)
    Optional<Commute> findByEmployee_EmplIdAndAttendanceDate(Long emplId, LocalDate attendanceDate);

    // 특정 직원의 전체 출퇴근 이력 (최근 날짜순)
    List<Commute> findByEmployee_EmplIdOrderByAttendanceDateDesc(Long emplId);
}
