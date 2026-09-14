package com.company.groupware.dto;

import com.company.groupware.entity.Employee;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmployeeSearchResponse {

    private final Long emplId;
    private final Long departmentId;
    private final String departmentName;
    private final String teamName;
    private final Long gradeId;
    private final String gradeName;
    private final String emplName;
    private final String emplPhone;
    private final String emplEmail;
    private final String duties;
    private final String loginId;
    private final boolean admin;

    public EmployeeSearchResponse(Long emplId, Long departmentId, String departmentName, String teamName,
                                  Long gradeId, String gradeName, String emplName, String emplPhone,
                                  String emplEmail, String duties, String loginId, boolean admin) {
        this.emplId = emplId;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.teamName = teamName;
        this.gradeId = gradeId;
        this.gradeName = gradeName;
        this.emplName = emplName;
        this.emplPhone = emplPhone;
        this.emplEmail = emplEmail;
        this.duties = duties;
        this.loginId = loginId;
        this.admin = admin;
    }

    public static EmployeeSearchResponse from(Employee employee) {
        return EmployeeSearchResponse.builder()
                .emplId(employee.getEmplId())
                .departmentId(employee.getDepartment() != null ? employee.getDepartment().getDeptId() : null)
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getDeptName() : "-")
                .teamName("-")
                .gradeId(employee.getGrade() != null ? employee.getGrade().getGradeId() : null)
                .gradeName(employee.getGrade() != null ? employee.getGrade().getGradeName() : "-")
                .emplName(employee.getEmplName())
                .emplPhone(employee.getEmplPhone())
                .emplEmail(employee.getEmplEmail())
                .duties("-")
                .loginId(employee.getLoginId())
                .admin(Boolean.TRUE.equals(employee.getEmplStat()))
                .build();
    }
}
