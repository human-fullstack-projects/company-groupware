package com.company.groupware.repository;

import com.company.groupware.entity.Commute;
import com.company.groupware.entity.CommuteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CommuteRepository extends JpaRepository<Commute, Long> {
    // 특정 직원의 특정 날짜 출퇴근 기록 (중복 출근 체크, 퇴근 처리 시 사용)
    Optional<Commute> findByEmployee_EmplIdAndAttendanceDate(Long emplId, LocalDate attendanceDate);

    // 특정 직원의 전체 출퇴근 이력 (최근 날짜순)
    List<Commute> findByEmployee_EmplIdOrderByAttendanceDateDesc(Long emplId);

    // 특정 날짜에 이미 출근 기록이 있는 직원 ID만 조회 : 결근용
    @Query("SELECT c.employee.emplId FROM Commute c WHERE c.attendanceDate = :date")
    List<Long> findEmplIdsByAttendanceDate(@Param("date") LocalDate date);

    // 관리자 대시보드 : 특정 날짜의 근태상태(정상/지각/결근)별 인원수 집계
    long countByAttendanceDateAndStatus(LocalDate attendanceDate, CommuteStatus status);

    // 관리자 근태 목록 : 특정 날짜의 전체 근태 기록 (직원 정보 함께 조회, 기록 없는 직원은 서비스단에서 "출근 전"으로 채움)
    @Query("SELECT c FROM Commute c JOIN FETCH c.employee WHERE c.attendanceDate = :date")
    List<Commute> findByAttendanceDateWithEmployee(@Param("date") LocalDate date);
}
