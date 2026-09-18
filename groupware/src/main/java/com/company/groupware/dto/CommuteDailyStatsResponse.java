package com.company.groupware.dto;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class CommuteDailyStatsResponse {
    private final LocalDate attendanceDate;
    private final long normalCount;
    private final long lateCount;
    private final long absentCount;
    private final long notCheckedInCount; // 출근/지각/결근 처리 전 (아직 기록 없음)
    private final long presentCount; // 정상 + 지각 = 출근 인원수

    public CommuteDailyStatsResponse(LocalDate attendanceDate, long normalCount, long lateCount, long absentCount,
                                      long notCheckedInCount) {
        this.attendanceDate = attendanceDate;
        this.normalCount = normalCount;
        this.lateCount = lateCount;
        this.absentCount = absentCount;
        this.notCheckedInCount = notCheckedInCount;
        this.presentCount = normalCount + lateCount;
    }
}
