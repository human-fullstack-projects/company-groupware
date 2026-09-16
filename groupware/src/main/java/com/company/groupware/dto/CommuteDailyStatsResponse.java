package com.company.groupware.dto;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class CommuteDailyStatsResponse {
    private final LocalDate attendanceDate;
    private final long normalCount;
    private final long lateCount;
    private final long absentCount;
    private final long presentCount; // 정상 + 지각 = 출근 인원수

    public CommuteDailyStatsResponse(LocalDate attendanceDate, long normalCount, long lateCount, long absentCount) {
        this.attendanceDate = attendanceDate;
        this.normalCount = normalCount;
        this.lateCount = lateCount;
        this.absentCount = absentCount;
        this.presentCount = normalCount + lateCount;
    }
}
