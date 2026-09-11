package com.company.groupware.controller;

import com.company.groupware.dto.EmployeeSearchRequest;
import com.company.groupware.dto.EmployeeSearchResponse;
import com.company.groupware.entity.Department;
import com.company.groupware.entity.Grade;
import com.company.groupware.service.DepartmentService;
import com.company.groupware.service.EmployeeService;
import com.company.groupware.service.GradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 직원 검색 웹 컨트롤러(EmployeeController) 요청/응답 및 뷰 렌더링 검증 테스트
 */
@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {

    @Mock
    private EmployeeService employeeService;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private GradeService gradeService;

    @InjectMocks
    private EmployeeController employeeController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // 1. 스프링 무거운 컨텍스트 로딩 없이 컨트롤러 단독 검증을 위한 Standalone MockMvc 구성
        mockMvc = MockMvcBuilders.standaloneSetup(employeeController).build();
    }

    @Test
    @DisplayName("직원 검색 GET 요청 시 정상 상태코드(200), 타임리프 템플릿(empl/search) 및 필수 모델 속성 반환 검증")
    void searchEmployees_success() throws Exception {
        // 2. [Given] 서비스 계층에서 반환할 모의 데이터 정의
        List<Department> departments = List.of(Department.builder().deptId(1L).deptName("기획과").build());
        List<Grade> grades = List.of(Grade.builder().gradeId(1L).gradeName("주무관").build());
        List<EmployeeSearchResponse> employees = List.of(
                EmployeeSearchResponse.builder()
                        .emplId(1L)
                        .departmentName("기획과")
                        .teamName("-")
                        .gradeName("주무관")
                        .emplName("홍길동")
                        .emplPhone("010-1234-5678")
                        .duties("업무 총괄")
                        .build()
        );

        given(departmentService.getDepartments()).willReturn(departments);
        given(gradeService.getGrades()).willReturn(grades);
        given(employeeService.searchEmployees(any(EmployeeSearchRequest.class))).willReturn(employees);

        // 3. [When & Then] GET /employees 호출 및 뷰/모델 검증 수행
        mockMvc.perform(get("/employees")
                        .param("departmentId", "1")
                        .param("gradeId", "1")
                        .param("emplName", "홍길동"))
                // HTTP 상태코드 200 OK 검증
                .andExpect(status().isOk())
                // 지정된 뷰 이름(empl/search) 반환 검증
                .andExpect(view().name("empl/search"))
                // 화면 렌더링에 필요한 모델 속성 존재 검증
                .andExpect(model().attributeExists("departments"))
                .andExpect(model().attributeExists("grades"))
                .andExpect(model().attributeExists("employees"))
                .andExpect(model().attributeExists("searchRequest"));
    }
}
