package com.company.groupware.controller;

import com.company.groupware.dto.EmployeeSearchRequest;
import com.company.groupware.dto.EmployeeSearchResponse;
import com.company.groupware.entity.Department;
import com.company.groupware.entity.Grade;
import com.company.groupware.service.DepartmentService;
import com.company.groupware.service.EmployeeService;
import com.company.groupware.service.GradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final GradeService gradeService;

    /**
     * 관리자 대시보드
     *
     * SecurityConfig의 "/admin/**" 규칙에 의해 ROLE_ADMIN 계정만 접근 가능하다.
     */
    @GetMapping("/admin")
    public String dashboard() {
        return "admin/dashboard";
    }

    /**
     * 직원계정 관리 - 전 직원 목록 + 관리자 지정/해제
     */
    @GetMapping("/admin/employees")
    public String employees(
            @ModelAttribute("searchRequest") EmployeeSearchRequest searchRequest,
            Model model) {

        List<Department> departments = departmentService.getDepartments();
        List<Grade> grades = gradeService.getGrades();
        List<EmployeeSearchResponse> employees = employeeService.searchEmployees(searchRequest);
        long adminCount = employeeService.countAdmins();

        model.addAttribute("departments", departments);
        model.addAttribute("grades", grades);
        model.addAttribute("employees", employees);
        model.addAttribute("adminCount", adminCount);

        return "admin/employees";
    }

    @PostMapping("/admin/employees/{emplId}/promote")
    public String promote(@PathVariable Long emplId, RedirectAttributes redirectAttributes) {
        employeeService.promoteToAdmin(emplId);
        redirectAttributes.addFlashAttribute("message", "관리자로 지정했습니다.");
        return "redirect:/admin/employees";
    }

    @PostMapping("/admin/employees/{emplId}/demote")
    public String demote(@PathVariable Long emplId, RedirectAttributes redirectAttributes) {
        try {
            employeeService.demoteFromAdmin(emplId);
            redirectAttributes.addFlashAttribute("message", "관리자 권한을 해제했습니다.");
        } catch (ResponseStatusException e) {
            redirectAttributes.addFlashAttribute("error", e.getReason());
        }
        return "redirect:/admin/employees";
    }

    /**
     * 부서 관리 - 직원의 소속 부서 이동
     */
    @GetMapping("/admin/departments")
    public String departments(
            @ModelAttribute("searchRequest") EmployeeSearchRequest searchRequest,
            Model model) {

        List<Department> departments = departmentService.getDepartments();
        List<EmployeeSearchResponse> employees = employeeService.searchEmployees(searchRequest);

        model.addAttribute("departments", departments);
        model.addAttribute("grades", gradeService.getGrades());
        model.addAttribute("employees", employees);

        return "admin/departments";
    }

    @PostMapping("/admin/employees/{emplId}/department")
    public String changeDepartment(
            @PathVariable Long emplId,
            @RequestParam Long deptId,
            RedirectAttributes redirectAttributes) {

        employeeService.changeDepartment(emplId, deptId);
        redirectAttributes.addFlashAttribute("message", "부서를 변경했습니다.");
        return "redirect:/admin/departments";
    }

    @PostMapping("/admin/departments/add")
    public String addDepartment(@RequestParam String deptName, RedirectAttributes redirectAttributes) {
        try {
            departmentService.addDepartment(deptName);
            redirectAttributes.addFlashAttribute("message", "부서를 추가했습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/departments";
    }

    @PostMapping("/admin/departments/{deptId}/delete")
    public String deleteDepartment(@PathVariable Long deptId, RedirectAttributes redirectAttributes) {
        try {
            departmentService.deleteDepartment(deptId);
            redirectAttributes.addFlashAttribute("message", "부서를 삭제했습니다.");
        } catch (ResponseStatusException e) {
            redirectAttributes.addFlashAttribute("error", e.getReason());
        }
        return "redirect:/admin/departments";
    }

    /**
     * 직급 관리 - 직원의 직급 변경
     */
    @GetMapping("/admin/grades")
    public String grades(
            @ModelAttribute("searchRequest") EmployeeSearchRequest searchRequest,
            Model model) {

        model.addAttribute("departments", departmentService.getDepartments());
        List<Grade> grades = gradeService.getGrades();
        List<EmployeeSearchResponse> employees = employeeService.searchEmployees(searchRequest);

        Map<Long, Integer> gradePriorityById = grades.stream()
                .filter(g -> g.getGradePriority() != null)
                .collect(Collectors.toMap(Grade::getGradeId, Grade::getGradePriority));

        model.addAttribute("grades", grades);
        model.addAttribute("employees", employees);
        model.addAttribute("gradePriorityById", gradePriorityById);

        return "admin/grades";
    }

    @PostMapping("/admin/employees/{emplId}/grade")
    public String changeGrade(
            @PathVariable Long emplId,
            @RequestParam Long gradeId,
            RedirectAttributes redirectAttributes) {

        employeeService.changeGrade(emplId, gradeId);
        redirectAttributes.addFlashAttribute("message", "직급을 변경했습니다.");
        return "redirect:/admin/grades";
    }

    @PostMapping("/admin/grades/add")
    public String addGrades(@RequestParam String gradeNames, RedirectAttributes redirectAttributes) {
        List<String> names = Arrays.stream(gradeNames.split("\\r?\\n"))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .toList();
        try {
            gradeService.addGrades(names);
            redirectAttributes.addFlashAttribute("message", names.size() + "개 직급을 추가했습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/grades";
    }

    @PostMapping("/admin/grades/{gradeId}/delete")
    public String deleteGrade(@PathVariable Long gradeId, RedirectAttributes redirectAttributes) {
        try {
            gradeService.deleteGrade(gradeId);
            redirectAttributes.addFlashAttribute("message", "직급을 삭제했습니다.");
        } catch (ResponseStatusException e) {
            redirectAttributes.addFlashAttribute("error", e.getReason());
        }
        return "redirect:/admin/grades";
    }

    @PostMapping("/admin/grades/{gradeId}/priority")
    public String updateGradePriority(
            @PathVariable Long gradeId,
            @RequestParam int priority,
            RedirectAttributes redirectAttributes) {

        try {
            gradeService.updatePriority(gradeId, priority);
            redirectAttributes.addFlashAttribute("message", "직급 순위를 변경했습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (ResponseStatusException e) {
            redirectAttributes.addFlashAttribute("error", e.getReason());
        }
        return "redirect:/admin/grades";
    }
}
