package com.company.groupware.controller;

import com.company.groupware.dto.ScheduleRequest;
import com.company.groupware.dto.ScheduleCancelRequest;
import org.springframework.dao.PessimisticLockingFailureException;
import com.company.groupware.dto.ScheduleResponse;
import com.company.groupware.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    // 1. 기간별 전체 일정 조회
    @GetMapping
    public List<ScheduleResponse> getSchedules(
            Principal principal,
            @RequestParam("start")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime start,

            @RequestParam("end")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime end
    ) {
        return scheduleService.getSchedules(
                principal.getName(),
                start,
                end
        );
    }

    // 2. 일정 상세 조회
    @GetMapping("/{scheduleId}")
    public ScheduleResponse getSchedule(
            Principal principal,
            @PathVariable("scheduleId") Long scheduleId
    ) {
        return scheduleService.getSchedule(
                principal.getName(),
                scheduleId
        );
    }

    // 3. 일정 추가
    @PostMapping
    public ResponseEntity<ScheduleResponse> create(
            Principal principal,
            @RequestBody ScheduleRequest request
    ) {
        ScheduleResponse response = scheduleService.create(
                principal.getName(),
                request
        );

        return ResponseEntity
                .created(URI.create(
                        "/api/schedules/" + response.scheduleId()
                ))
                .body(response);
    }

    // 4. 일정 수정
    @PutMapping("/{scheduleId}")
    public ScheduleResponse update(
            Principal principal,
            @PathVariable("scheduleId") Long scheduleId,
            @RequestBody ScheduleRequest request
    ) {
        return scheduleService.update(
                principal.getName(),                    //현재 로그인한 사용자의  아이디
                scheduleId,
                request
        );
    }

    // 같은 부서 직원만 일정 취소. 실제 삭제는 하지 않음.
    @PostMapping("/{scheduleId}/cancel")
    public ScheduleResponse cancel(
            Principal principal,
            @PathVariable("scheduleId") Long scheduleId,
            @RequestBody ScheduleCancelRequest request
    ) {
        return scheduleService.cancel(
                principal.getName(), scheduleId, request.version()
        );
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> handleLockFailure() {
        return ResponseEntity.status(409).body(Map.of(
                "message", "다른 일정을 처리 중입니다. 잠시 후 다시 시도해주세요."
        ));
    }

    // Service에서 발생한 안내 메시지를 화면으로 전달
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleException(
            ResponseStatusException exception
    ) {
        String message = exception.getReason() == null
                ? "일정 요청을 처리할 수 없습니다."
                : exception.getReason();

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(Map.of("message", message));
    }
}
