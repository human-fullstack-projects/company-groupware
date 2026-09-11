package com.company.groupware.dto;

import com.company.groupware.entity.Employee;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmployeeSearchResponse {

    private final Long emplId;
    private final String departmentName;
    private final String teamName;
    private final String gradeName;
    private final String emplName;
    private final String emplPhone;
    private final String emplEmail;
    private final String duties;

    public EmployeeSearchResponse(Long emplId, String departmentName, String teamName,
                                  String gradeName, String emplName, String emplPhone,
                                  String emplEmail, String duties) {
        this.emplId = emplId;
        this.departmentName = departmentName;
        this.teamName = teamName;
        this.gradeName = gradeName;
        this.emplName = emplName;
        this.emplPhone = emplPhone;
        this.emplEmail = emplEmail;
        this.duties = duties;
    }

    public static EmployeeSearchResponse from(Employee employee) {
        return EmployeeSearchResponse.builder()
                .emplId(employee.getEmplId())
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getDeptName() : "-")
                .teamName("-")
                .gradeName(employee.getGrade() != null ? employee.getGrade().getGradeName() : "-")
                .emplName(employee.getEmplName())
                .emplPhone(employee.getEmplPhone())
                .emplEmail(employee.getEmplEmail())
                .duties("-")
                .build();
    }
}
