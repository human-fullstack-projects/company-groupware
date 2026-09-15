package com.company.groupware.service;

import com.company.groupware.Exception.InvalidCommuteStateException;
import com.company.groupware.Exception.ResourceNotFoundException;
import com.company.groupware.config.CommuteProperties;
import com.company.groupware.dto.CommuteResponse;
import com.company.groupware.entity.Commute;
import com.company.groupware.entity.CommuteStatus;
import com.company.groupware.entity.Employee;
import com.company.groupware.repository.CommuteRepository;
import com.company.groupware.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommuteService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

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

}
