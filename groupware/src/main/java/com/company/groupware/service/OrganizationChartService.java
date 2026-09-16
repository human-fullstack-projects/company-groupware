package com.company.groupware.service;

import com.company.groupware.dto.OrganizationChartResponse;
import com.company.groupware.entity.Department;
import com.company.groupware.entity.Employee;
import com.company.groupware.repository.DepartmentRepository;
import com.company.groupware.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationChartService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository; // [추가] 주입
    /**
     * 조직도 화면에 필요한 데이터를 조회하고 구조화하여 반환하는 비즈니스 로직
     */
    @Transactional(readOnly = true)
    public OrganizationChartResponse getOrganizationChartData() {
        // 1. 최상단 및 임원진 설정 (필요시 이 부분도 DB 연동 가능)
        String ceoTitle = "대표이사";

        List<OrganizationChartResponse.NodeItem> topLevelNodes = List.of(
                OrganizationChartResponse.NodeItem.builder().id(1L).name("고문").build(),
                OrganizationChartResponse.NodeItem.builder().id(2L).name("총괄임원").build()
        );

        // 2. [DB 연동] 실제 데이터베이스에서 부서 목록을 조회
        List<Department> departments = departmentRepository.findAllDepartmentsOrderById();

        // 3. [DB 연동] 조회한 부서 엔티티를 DTO로 변환 (하단 팀 정보는 제외)
        List<OrganizationChartResponse.DepartmentNode> departmentNodes = departments.stream()
                .map(dept -> OrganizationChartResponse.DepartmentNode.builder()
                        .deptId(dept.getDeptId())
                        .deptName(dept.getDeptName()) // 실제 DB의 부서명 매핑
                        .build())
                .toList();

        // 4. 최종 응답 객체 조립
        OrganizationChartResponse response = new OrganizationChartResponse();
        response.setTitle(ceoTitle);
        response.setTopLevelNodes(topLevelNodes);
        response.setDepartments(departmentNodes);

        return response;
    }
    // [추가] 특정 부서 클릭 시 해당 부서의 직급별 조직도 데이터 생성 메서드
    @Transactional(readOnly = true)
    public OrganizationChartResponse getDepartmentOrganizationChart(Long deptId) {
        // 1. 부서 정보 조회
        Department department = departmentRepository.findById(deptId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 부서입니다."));

        // 2. 해당 부서의 직원 목록 조회 (직급 정렬됨)
//        List<Employee> employees = employeeRepository.findByDepartmentDeptId(deptId);

        // 2-1. 다른 페이지에 영향을 안 주는 조직도 전용 메서드 호출 (내림차순 정렬 적용됨)
        List<Employee> employees = employeeRepository.findOrgEmployeesByDepartmentId(deptId);

        // 3. 직급별로 직원 그룹화 (직급 순서 유지 유지를 위해 LinkedHashMap 사용)
        Map<String, List<Employee0Wrapper>> groupedByGrade = new LinkedHashMap<>();

        for (Employee emp : employees) {
            String gradeName = (emp.getGrade() != null) ? emp.getGrade().getGradeName() : "직급없음";
            groupedByGrade.computeIfAbsent(gradeName, k -> new ArrayList<>()).add(new Employee0Wrapper(emp));
        }

        // 4. 각 직급별 인원을 5개씩 쪼개어 행(Row) 리스트로 변환
        List<OrganizationChartResponse.GradeTierGroup> tierGroups = new ArrayList<>();

        for (Map.Entry<String, List<Employee0Wrapper>> entry : groupedByGrade.entrySet()) {
            String gradeName = entry.getKey();
            List<Employee0Wrapper> empList = entry.getValue();

            List<List<OrganizationChartResponse.EmployeeItem>> rowLists = new ArrayList<>();
            List<OrganizationChartResponse.EmployeeItem> currentRow = new ArrayList<>();

            for (int i = 0; i < empList.size(); i++) {
                Employee emp = empList.get(i).employee;
                currentRow.add(OrganizationChartResponse.EmployeeItem.builder()
                        .emplId(emp.getEmplId())
                        .emplName(emp.getEmplName())
                        .gradeName(gradeName)
                        .build());

                // 5개 단위로 끊어서 행 추가
                if (currentRow.size() == 5 || i == empList.size() - 1) {
                    rowLists.add(new ArrayList<>(currentRow));
                    currentRow.clear();
                }
            }

            tierGroups.add(OrganizationChartResponse.GradeTierGroup.builder()
                    .gradeName(gradeName)
                    .rowEmployeeLists(rowLists)
                    .build());
        }

        // 5. 응답 DTO 구성
        OrganizationChartResponse response = new OrganizationChartResponse();
        response.setCurrentDeptName(department.getDeptName());
        response.setDepartments(departmentRepository.findAllDepartmentsOrderById().stream()
                .map(d -> OrganizationChartResponse.DepartmentNode.builder().deptId(d.getDeptId()).deptName(d.getDeptName()).build())
                .toList()); // 좌측 메뉴나 상단 부서 목록 유지를 위해 대입
        response.setGradeTierGroups(tierGroups);

        return response;
    }

    // 내부 래퍼 클래스
    private static class Employee0Wrapper {
        Employee employee;
        public Employee0Wrapper(Employee employee) { this.employee = employee; }
    }
}