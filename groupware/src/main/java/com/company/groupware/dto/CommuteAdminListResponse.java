package com.company.groupware.dto;

import com.company.groupware.entity.Commute;
import com.company.groupware.entity.CommuteStatus;
import com.company.groupware.entity.Employee;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class CommuteAdminListResponse {
    private final Long commuteId;       // 근태 기록이 없으면 null (출근 전)
    private final Long emplId;
    private final String emplName;
    private final String departmentName;
    private final LocalDate attendanceDate;
    private final String startTime;     // 기록 없으면 null
    private final CommuteStatus status; // 기록 없으면 null
    private final String statusLabel;   // 정상 / 지각 / 결근 / 출근 전
    private final String modifyReason;  // 마지막 수정 사유 (수정 이력 없으면 null)
    private final String modifiedBy;    // 마지막으로 수정한 관리자 이름
    private final LocalDateTime modifiedAt; // 마지막 수정 시각

    private CommuteAdminListResponse(Long commuteId, Long emplId, String emplName, String departmentName,
                                      LocalDate attendanceDate, String startTime, CommuteStatus status,
                                      String statusLabel, String modifyReason, String modifiedBy,
                                      LocalDateTime modifiedAt) {
        this.commuteId = commuteId;
        this.emplId = emplId;
        this.emplName = emplName;
        this.departmentName = departmentName;
        this.attendanceDate = attendanceDate;
        this.startTime = startTime;
        this.status = status;
        this.statusLabel = statusLabel;
        this.modifyReason = modifyReason;
        this.modifiedBy = modifiedBy;
        this.modifiedAt = modifiedAt;
    }

    public static CommuteAdminListResponse notCheckedIn(Employee employee, LocalDate attendanceDate) {
        return new CommuteAdminListResponse(
                null,
                employee.getEmplId(),
                employee.getEmplName(),
                departmentName(employee),
                attendanceDate,
                null,
                null,
                "출근 전",
                null,
                null,
                null);
    }

    public static CommuteAdminListResponse from(Commute commute) {
        Employee employee = commute.getEmployee();
        return new CommuteAdminListResponse(
                commute.getCommuteId(),
                employee.getEmplId(),
                employee.getEmplName(),
                departmentName(employee),
                commute.getAttendanceDate(),
                commute.getStartTime(),
                commute.getStatus(),
                statusLabel(commute.getStatus()),
                commute.getModifyReason(),
                commute.getModifiedBy(),
                commute.getModifiedAt());
    }

    private static String departmentName(Employee employee) {
        return employee.getDepartment() != null ? employee.getDepartment().getDeptName() : "-";
    }

    private static String statusLabel(CommuteStatus status) {
        return switch (status) {
            case NORMAL -> "정상";
            case LATE -> "지각";
            case ABSENT -> "결근";
        };
    }
}
