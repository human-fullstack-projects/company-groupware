package com.company.groupware.controller;

import com.company.groupware.dto.OrganizationChartResponse;
import com.company.groupware.service.OrganizationChartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 조직도 관련 웹 요청을 처리하는 컨트롤러
 */
@Controller
@RequestMapping("/organization")
@RequiredArgsConstructor
public class OrganizationChartController {

    private final OrganizationChartService organizationChartService;

    /**
     * 조직도 화면 제공 및 데이터 전달 매핑
     *
     * @param model 화면에 조직도 데이터를 전달하기 위한 Model 객체
     * @return 조직도 Thymeleaf 템플릿 뷰 경로
     */
    @GetMapping("/chart")
    public String getOrganizationChart(Model model) {
        // 1. 서비스 레이어로부터 조직도 구성 데이터 조회
        OrganizationChartResponse chartData = organizationChartService.getOrganizationChartData();

        // 2. 뷰(HTML)에 데이터 전달
        model.addAttribute("chartData", chartData);

        // 3. 조직도 Thymeleaf 템플릿 뷰 반환
        return "organization/chart";
    }
    // [추가] 특정 부서 클릭 시 해당 부서 직급 조직도 화면 매핑
    @GetMapping("/chart/{deptId}")
    public String getDepartmentChart(@PathVariable("deptId") Long deptId, Model model) {
        OrganizationChartResponse chartData = organizationChartService.getDepartmentOrganizationChart(deptId);
        model.addAttribute("chartData", chartData);
        return "organization/chart"; // 동일한 템플릿 재사용 (흰 바탕 영역만 조건부로 바뀜)
    }

    // [추가] 직원 상세 정보 조회 REST API
    @GetMapping("/api/employee/{emplId}")
    @ResponseBody
    public OrganizationChartResponse.EmployeeItem getEmployeeDetailApi(@PathVariable("emplId") Long emplId) {
        return organizationChartService.getEmployeeDetail(emplId);
    }

}