package com.company.groupware.service;

import com.company.groupware.dto.ScheduleRequest;
import com.company.groupware.dto.ScheduleResponse;
import com.company.groupware.entity.Employee;
import com.company.groupware.entity.Schedule;
import com.company.groupware.repository.EmployeeRepository;
import com.company.groupware.repository.ScheduleBookingLockRepository;
import com.company.groupware.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final EmployeeRepository employeeRepository;
    private final ScheduleBookingLockRepository bookingLockRepository;

    // 일정 잠금 획득 후, 직전에 확정된 일정도 읽을 수 있게 READ_COMMITTED 사용.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ScheduleResponse create(String loginId, ScheduleRequest request) {
        Employee employee = getEmployee(loginId);
        if (employee.getDepartment() == null) {
            throw error(HttpStatus.FORBIDDEN, "부서가 배정된 직원만 등록할 수 있습니다.");
        }
        validateRequest(request);
        lockCalendar();
        checkAvailability(request, null);

        Schedule schedule = new Schedule();
        schedule.setEmployee(employee);
        schedule.setDepartment(employee.getDepartment());
        schedule.setCancelled(false);
        applyRequest(schedule, request);
        return save(schedule, employee);
    }

    public List<ScheduleResponse> getSchedules(
            String loginId, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        Employee employee = getEmployee(loginId);
        if (rangeStart == null || rangeEnd == null || !rangeEnd.isAfter(rangeStart)) {
            throw error(HttpStatus.BAD_REQUEST, "조회 기간을 올바르게 입력해주세요.");
        }
        return scheduleRepository.findSchedulesInRange(rangeStart, rangeEnd)
                .stream().map(s -> toResponse(s, employee)).toList();
    }

    public ScheduleResponse getSchedule(String loginId, Long scheduleId) {
        Employee employee = getEmployee(loginId);
        return toResponse(findSchedule(scheduleId), employee);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ScheduleResponse update(
            String loginId, Long scheduleId, ScheduleRequest request) {
        Employee employee = getEmployee(loginId);
        lockCalendar();
        Schedule schedule = findSchedule(scheduleId);
        requireSameDepartment(schedule, employee);
        if (isCancelled(schedule)) {
            throw error(HttpStatus.CONFLICT, "취소된 일정은 수정할 수 없습니다.");
        }
        validateRequest(request);
        checkVersion(schedule, request.getVersion());

        // 자기 자신을 제외하고 전체 부서 일정과 중복 검사
        checkAvailability(request, scheduleId);
        applyRequest(schedule, request);
        return save(schedule, employee);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ScheduleResponse cancel(String loginId, Long scheduleId, Long version) {
        Employee employee = getEmployee(loginId);
        lockCalendar();
        Schedule schedule = findSchedule(scheduleId);
        requireSameDepartment(schedule, employee);

        // 이미 취소되었으면 재요청해도 기록을 다시 바꾸지 않음
        if (isCancelled(schedule)) return toResponse(schedule, employee);
        checkVersion(schedule, version);
        schedule.setCancelled(true);
        return save(schedule, employee);
    }

    private void lockCalendar() {
        bookingLockRepository.lockCalendar()
                .orElseThrow(() -> error(HttpStatus.CONFLICT,
                        "일정 잠금 기본 데이터가 없습니다. 관리자에게 문의해주세요."));
    }

    private void checkAvailability(ScheduleRequest request, Long excludedId) {
        long count = scheduleRepository.countConflicts(
                request.getStartAt(), request.getEndAt(), excludedId);
        if (count > 0) {
            throw error(HttpStatus.CONFLICT,
                    "선택한 날짜·시간에 겹치는 일정이 있습니다. 다른 시간 또는 날짜를 선택해주세요.");
        }
    }

    private ScheduleResponse save(Schedule schedule, Employee employee) {
        try {
            return toResponse(scheduleRepository.saveAndFlush(schedule), employee);
        } catch (OptimisticLockingFailureException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "다른 직원이 변경한 일정입니다. 창을 닫고 다시 조회해주세요.", e);
        }
    }

    private void checkVersion(Schedule schedule, Long version) {
        if (version == null) {
            throw error(HttpStatus.BAD_REQUEST, "일정 버전 정보가 필요합니다.");
        }
        if (!Objects.equals(schedule.getVersion(), version)) {
            throw error(HttpStatus.CONFLICT,
                    "다른 직원이 변경한 일정입니다. 창을 닫고 다시 조회해주세요.");
        }
    }

    private Employee getEmployee(String loginId) {
        if (!StringUtils.hasText(loginId)) {
            throw error(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return employeeRepository.findByLoginId(loginId)
                .orElseThrow(() -> error(HttpStatus.UNAUTHORIZED,
                        "로그인한 직원 정보를 찾을 수 없습니다."));
    }

    private Schedule findSchedule(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."));
    }

    private boolean sameDepartment(Schedule schedule, Employee employee) {
        return employee.getDepartment() != null
                && schedule.getDepartment() != null
                && Objects.equals(employee.getDepartment().getDeptId(),
                                  schedule.getDepartment().getDeptId());
    }

    private void requireSameDepartment(Schedule schedule, Employee employee) {
        if (!sameDepartment(schedule, employee)) {
            throw error(HttpStatus.FORBIDDEN,
                    "일정 담당 부서 직원만 수정하거나 취소할 수 있습니다.");
        }
    }

    private boolean isCancelled(Schedule schedule) {
        return Boolean.TRUE.equals(schedule.getCancelled());
    }

    private void validateRequest(ScheduleRequest request) {
        if (request == null || !StringUtils.hasText(request.getTitle())) {
            throw error(HttpStatus.BAD_REQUEST, "일정 제목을 입력해주세요.");
        }
        if (request.getTitle().trim().length() > 100) {
            throw error(HttpStatus.BAD_REQUEST, "일정 제목은 100자 이내로 입력해주세요.");
        }
        if (request.getContent() != null && request.getContent().length() > 2000) {
            throw error(HttpStatus.BAD_REQUEST, "일정 내용은 2000자 이내로 입력해주세요.");
        }
        if (request.getStartAt() == null || request.getEndAt() == null) {
            throw error(HttpStatus.BAD_REQUEST, "시작 일시와 종료 일시를 입력해주세요.");
        }
        if (!request.getEndAt().isAfter(request.getStartAt())) {
            throw error(HttpStatus.BAD_REQUEST, "종료 일시는 시작 일시보다 늦어야 합니다.");
        }
    }

    private void applyRequest(Schedule schedule, ScheduleRequest request) {
        schedule.setTitle(request.getTitle().trim());
        schedule.setContent(request.getContent() == null ? "" : request.getContent().trim());
        schedule.setStartAt(request.getStartAt());
        schedule.setEndAt(request.getEndAt());
    }

    private ScheduleResponse toResponse(Schedule schedule, Employee employee) {
        boolean cancelled = isCancelled(schedule);
        return new ScheduleResponse(
                schedule.getScheduleId(), schedule.getTitle(), schedule.getContent(),
                schedule.getStartAt(), schedule.getEndAt(),
                schedule.getDepartment().getDeptName(), schedule.getEmployee().getEmplName(),
                schedule.getVersion(), sameDepartment(schedule, employee) && !cancelled, cancelled
        );
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
