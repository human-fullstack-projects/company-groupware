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
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * 직원 관련 요청을 처리하는 웹 컨트롤러
 */
@Controller
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final GradeService gradeService;

    /**
     * 직원 검색 화면 제공 및 조건별 검색 결과 조회
     *
     * @param searchRequest 부서, 직급, 직원명 등 검색 조건 DTO (GET 파라미터와 자동 바인딩)
     * @param model         화면에 데이터를 전달하기 위한 Model 객체
     * @return 직원 검색 결과 템플릿 뷰 경로
     */
    @GetMapping({"", "/search"})
    public String searchEmployees(
            @ModelAttribute("searchRequest") EmployeeSearchRequest searchRequest,
            Model model) {

        // 1. 검색 필터 드롭다운 구성에 필요한 부서 및 직급 목록 조회
        List<Department> departments = departmentService.getDepartments();
        List<Grade> grades = gradeService.getGrades();

        // 2. 검색 조건(부서ID, 직급ID, 직원이름)을 바탕으로 직원 목록 조회
        List<EmployeeSearchResponse> employees = employeeService.searchEmployees(searchRequest);

        // 3. 뷰(HTML)에 데이터 전달 (검색 필터 옵션 및 검색 결과 목록)
        model.addAttribute("departments", departments);
        model.addAttribute("grades", grades);
        model.addAttribute("employees", employees);

        // 4. 직원 검색 Thymeleaf 템플릿 렌더링
        return "empl/search";
    }
}
