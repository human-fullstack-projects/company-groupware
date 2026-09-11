package com.company.groupware.service;

import com.company.groupware.dto.EmployeeSearchRequest;
import com.company.groupware.dto.EmployeeSearchResponse;
import com.company.groupware.entity.Department;
import com.company.groupware.entity.Employee;
import com.company.groupware.entity.Grade;
import com.company.groupware.repository.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * 직원 검색 비즈니스 로직(EmployeeService) 단위 테스트 및 검증
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    // EmployeeRepository 모의(Mock) 객체 주입
    @Mock
    private EmployeeRepository employeeRepository;

    // 모의 객체들이 주입된 EmployeeService 인스턴스
    @InjectMocks
    private EmployeeService employeeService;

    @Test
    @DisplayName("검색 조건이 주어지지 않았을 때 전체 직원 목록이 정상 조회되는지 검증")
    void searchEmployees_noCondition_returnsAll() {
        // 1. [Given] 모의 엔티티 데이터 생성
        Department department = Department.builder().deptId(1L).deptName("개발팀").build();
        Grade grade = Grade.builder().gradeId(1L).gradeName("주임").build();
        Employee employee = Employee.builder()
                .emplId(1L)
                .emplName("홍길동")
                .emplPhone("010-1234-5678")
                .emplEmail("hong@company.com")
                .department(department)
                .grade(grade)
                .build();

        // 조건이 null일 때의 리포지토리 반환값 설정
        given(employeeRepository.searchEmployees(null, null, null))
                .willReturn(List.of(employee));

        // 2. [When] 서비스의 검색 메서드 호출
        List<EmployeeSearchResponse> responses = employeeService.searchEmployees(new EmployeeSearchRequest());

        // 3. [Then] DTO 변환 및 반환 데이터 일치 검증
        assertThat(responses).hasSize(1);
        EmployeeSearchResponse response = responses.get(0);
        assertThat(response.getEmplId()).isEqualTo(1L);
        assertThat(response.getEmplName()).isEqualTo("홍길동");
        assertThat(response.getDepartmentName()).isEqualTo("개발팀");
        assertThat(response.getGradeName()).isEqualTo("주임");
        assertThat(response.getEmplPhone()).isEqualTo("010-1234-5678");

        // 리포지토리의 searchEmployees(null, null, null) 호출 검증
        verify(employeeRepository).searchEmployees(null, null, null);
    }

    @Test
    @DisplayName("부서, 직급, 직원명 조건이 주어졌을 때 파라미터가 정확하게 전달되어 필터링되는지 검증")
    void searchEmployees_withConditions_passesParametersCorrectly() {
        // 1. [Given] 검색 조건 파라미터 준비
        Long departmentId = 2L;
        Long gradeId = 3L;
        String keyword = "이순신";
        EmployeeSearchRequest request = new EmployeeSearchRequest(departmentId, gradeId, keyword);

        Department department = Department.builder().deptId(departmentId).deptName("인사팀").build();
        Grade grade = Grade.builder().gradeId(gradeId).gradeName("팀장").build();
        Employee employee = Employee.builder()
                .emplId(2L)
                .emplName(keyword)
                .emplPhone("010-9876-5432")
                .emplEmail("lee@company.com")
                .department(department)
                .grade(grade)
                .build();

        given(employeeRepository.searchEmployees(departmentId, gradeId, keyword))
                .willReturn(List.of(employee));

        // 2. [When] 조건부 검색 로직 실행
        List<EmployeeSearchResponse> responses = employeeService.searchEmployees(request);

        // 3. [Then] 필터링된 결과 데이터 일치 확인
        assertThat(responses).hasSize(1);
        EmployeeSearchResponse response = responses.get(0);
        assertThat(response.getEmplName()).isEqualTo("이순신");
        assertThat(response.getDepartmentName()).isEqualTo("인사팀");
        assertThat(response.getGradeName()).isEqualTo("팀장");

        // 리포지토리에 지정된 파라미터가 누락 없이 전달되었는지 호출 검증
        verify(employeeRepository).searchEmployees(departmentId, gradeId, keyword);
    }
}
