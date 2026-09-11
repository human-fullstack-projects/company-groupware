package com.company.groupware.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EmployeeSearchRequest {

    private Long departmentId;
    private Long gradeId;
    private String emplName;

    public EmployeeSearchRequest(Long departmentId, Long gradeId, String emplName) {
        this.departmentId = departmentId;
        this.gradeId = gradeId;
        this.emplName = emplName;
    }
}
