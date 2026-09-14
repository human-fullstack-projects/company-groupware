package com.company.groupware.dto;

import com.company.groupware.entity.Commute;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class CommuteResponse {
    private final Long commuteId;
    private final Long emplId;
    private final String emplName;
    private final String startTime;
    private final String finishTime;
    private final LocalDate attendanceDate;

    public CommuteResponse(Commute commute) {
        this.commuteId = commute.getCommuteId();
        this.emplId = commute.getEmployee().getEmplId();
        this.emplName = commute.getEmployee().getEmplName();
        this.startTime = commute.getStartTime();
        this.finishTime = commute.getFinishTime();
        this.attendanceDate = commute.getAttendanceDate();
    }
}
