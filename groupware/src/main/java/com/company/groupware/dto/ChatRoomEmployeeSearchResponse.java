package com.company.groupware.dto;

import com.company.groupware.entity.Employee;
import lombok.Getter;

@Getter
public class ChatRoomEmployeeSearchResponse {

    private final Long emplId;
    private final String emplName;
    private final String deptName;

    public ChatRoomEmployeeSearchResponse(Employee employee) {
        this.emplId = employee.getEmplId();
        this.emplName = employee.getEmplName();
        this.deptName = employee.getDepartment() != null ? employee.getDepartment().getDeptName() : null;
    }
}
