package com.company.groupware.service;

import com.company.groupware.Exception.InvalidCommuteStateException;
import com.company.groupware.Exception.ResourceNotFoundException;
import com.company.groupware.config.CommuteProperties;
import com.company.groupware.dto.CommuteAdminListResponse;
import com.company.groupware.dto.CommuteDailyStatsResponse;
import com.company.groupware.dto.CommuteResponse;
import com.company.groupware.dto.CommuteUpdateRequest;
import com.company.groupware.entity.Commute;
import com.company.groupware.entity.CommuteStatus;
import com.company.groupware.entity.Employee;
import com.company.groupware.repository.CommuteRepository;
import com.company.groupware.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommuteService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final LocalTime FIXED_FINISH_TIME = LocalTime.of(18, 0);

    private final CommuteRepository commuteRepository;
    private final EmployeeRepository employeeRepository;
    private final CommuteProperties commuteProperties;

    /**
     * 출근 처리: 오늘 날짜로 이미 기록이 있으면 중복 출근으로 간주해 예외 발생
     */
    @Transactional
    public CommuteResponse checkIn(Long emplId) {
        Employee employee = employeeRepository.findById(emplId)
                .orElseThrow(() -> new ResourceNotFoundException("직원을 찾을 수 없습니다. empl_id=" + emplId));

        LocalDate today = LocalDate.now();
        commuteRepository.findByEmployee_EmplIdAndAttendanceDate(emplId, today)
                .ifPresent(c -> {
                    throw new InvalidCommuteStateException("이미 오늘 출근 기록이 있습니다. empl_id=" + emplId);
                });

        LocalTime now = LocalTime.now();

        Commute commute = new Commute();
        commute.setEmployee(employee);
        commute.setAttendanceDate(today);
        commute.setStartTime(now.format(TIME_FORMATTER));
        commute.setStatus(now.isAfter(commuteProperties.getLateTime()) ? CommuteStatus.LATE : CommuteStatus.NORMAL);
        commuteRepository.save(commute);

        return new CommuteResponse(commute);
    }

    /**
     * 퇴근 처리: 오늘 출근 기록이 있어야 하고, 아직 퇴근 처리 전이어야 함
     */
    @Transactional
    public CommuteResponse checkOut(Long emplId) {
        LocalDate today = LocalDate.now();
        Commute commute = commuteRepository.findByEmployee_EmplIdAndAttendanceDate(emplId, today)
                .orElseThrow(() -> new InvalidCommuteStateException(
                        "오늘 출근 기록이 없습니다. empl_id=" + emplId));

        if (commute.getFinishTime() != null) {
            throw new InvalidCommuteStateException("이미 퇴근 처리되었습니다. empl_id=" + emplId);
        }

        commute.setFinishTime(LocalTime.now().format(TIME_FORMATTER));
        return new CommuteResponse(commute);
    }

    /**
     * 특정 직원의 출퇴근 이력 (최근 날짜순)
     */
    public List<CommuteResponse> getHistory(Long emplId) {
        return commuteRepository.findByEmployee_EmplIdOrderByAttendanceDateDesc(emplId).stream()
                .map(CommuteResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * 오늘 출퇴근 상태 조회 (출근 전이면 null)
     */
    public CommuteResponse getToday(Long emplId) {
        return commuteRepository.findByEmployee_EmplIdAndAttendanceDate(emplId, LocalDate.now())
                .map(CommuteResponse::new)
                .orElse(null);
    }

    /**
     * 관리자 대시보드: 특정 날짜의 전체 직원 근태상태(정상/지각/결근) 인원수 집계
     */
    public CommuteDailyStatsResponse getDailyStats(LocalDate date) {
        long normalCount = commuteRepository.countByAttendanceDateAndStatus(date, CommuteStatus.NORMAL);
        long lateCount = commuteRepository.countByAttendanceDateAndStatus(date, CommuteStatus.LATE);
        long absentCount = commuteRepository.countByAttendanceDateAndStatus(date, CommuteStatus.ABSENT);
        return new CommuteDailyStatsResponse(date, normalCount, lateCount, absentCount);
    }

    /**
     * 관리자 근태 목록: 특정 날짜의 직원별 근태 현황 (부서/이름 필터 적용, 기록 없는 직원은 "출근 전")
     */
    public List<CommuteAdminListResponse> getCommuteList(LocalDate date, Long departmentId, String emplName) {
        String keyword = StringUtils.hasText(emplName) ? emplName.trim() : null;

        // 이름의 글자 수 계산 (null인 경우 0으로 처리)
        int nameLength = (keyword != null) ? keyword.length() : 0;

        // gradeId 자리에 null을 넣고, 마지막에 nameLength 파라미터를 추가하여 4개 맞춤
        List<Employee> employees = employeeRepository.searchEmployeesWithLengthCheck(
                departmentId, null, keyword, nameLength
        );

        Map<Long, Commute> commuteByEmplId = commuteRepository.findByAttendanceDateWithEmployee(date).stream()
                .collect(Collectors.toMap(c -> c.getEmployee().getEmplId(), c -> c));

        return employees.stream()
                .map(employee -> {
                    Commute commute = commuteByEmplId.get(employee.getEmplId());
                    return commute != null
                            ? CommuteAdminListResponse.from(commute)
                            : CommuteAdminListResponse.notCheckedIn(employee, date);
                })
                .toList();
    }

    /**
     * 관리자 근태 수정: 지정한 직원의 특정 날짜 출근시간/상태를 수정한다.
     * 해당 날짜에 기록이 없던 직원(출근 전)이면 새로 생성한다.
     * 수정 사유와, 수정한 관리자 이름·시각을 함께 기록한다(마지막 수정 정보만 유지).
     */
    @Transactional
    public CommuteAdminListResponse updateCommute(Long emplId, LocalDate date, CommuteUpdateRequest request,
                                                   String modifiedByName) {
        if (request.getStatus() == null || !StringUtils.hasText(request.getStartTime())) {
            throw new IllegalArgumentException("상태와 시간을 모두 입력해주세요.");
        }
        if (!StringUtils.hasText(request.getReason())) {
            throw new IllegalArgumentException("수정 사유를 입력해주세요.");
        }

        Employee employee = employeeRepository.findById(emplId)
                .orElseThrow(() -> new ResourceNotFoundException("직원을 찾을 수 없습니다. empl_id=" + emplId));

        Commute commute = commuteRepository.findByEmployee_EmplIdAndAttendanceDate(emplId, date)
                .orElseGet(() -> {
                    Commute newCommute = new Commute();
                    newCommute.setEmployee(employee);
                    newCommute.setAttendanceDate(date);
                    return newCommute;
                });

        commute.setStartTime(normalizeTime(request.getStartTime()));
        commute.setStatus(request.getStatus());
        commute.setFinishTime(FIXED_FINISH_TIME.format(TIME_FORMATTER));
        commute.setModifyReason(request.getReason().trim());
        commute.setModifiedBy(modifiedByName);
        commute.setModifiedAt(LocalDateTime.now());
        commuteRepository.save(commute);

        return CommuteAdminListResponse.from(commute);
    }

    private String normalizeTime(String rawTime) {
        try {
            return LocalTime.parse(rawTime).format(TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("시간 형식이 올바르지 않습니다.");
        }
    }

}
