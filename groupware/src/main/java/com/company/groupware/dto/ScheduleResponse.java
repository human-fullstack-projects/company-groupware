package com.company.groupware.dto;

import java.time.LocalDateTime;

public record ScheduleResponse(
        Long scheduleId,
        String title,
        String content,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String departmentName,
        String writerName,
        Long version,
        boolean editable,
        boolean cancelled
) {
}
