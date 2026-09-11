package com.company.groupware.util;

import com.company.groupware.entity.Department;
import com.company.groupware.entity.Employee;
import com.company.groupware.entity.Grade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeSortHelperTest {

    private Department dept(String name) {
        return Department.builder().deptName(name).build();
    }

    private Grade grade(String name, Long id) {
        return Grade.builder().gradeName(name).gradeId(id).build();
    }

    private Employee emp(String name, Department dept, Grade grade) {
        return Employee.builder()
                .emplName(name)
                .department(dept)
                .grade(grade)
                .build();
    }

    @Test
    @DisplayName("기본 부서 순서 검증: 인사팀 -> 재무팀 -> 기획팀 -> 개발팀 -> 디자인팀")
    void testDepartmentOrder() {
        Department hr = dept("인사팀");
        Department finance = dept("재무팀");
        Department plan = dept("기획팀");
        Department dev = dept("개발팀");
        Department design = dept("디자인팀");

        Grade bujang = grade("부장", 6L);

        List<Employee> list = new ArrayList<>(List.of(
                emp("디자인직원", design, bujang),
                emp("개발직원", dev, bujang),
                emp("기획직원", plan, bujang),
                emp("재무직원", finance, bujang),
                emp("인사직원", hr, bujang)
        ));

        list.sort(EmployeeSortHelper.EMPLOYEE_COMPARATOR);

        assertThat(list).extracting(Employee::getEmplName)
                .containsExactly("인사직원", "재무직원", "기획직원", "개발직원", "디자인직원");
    }

    @Test
    @DisplayName("부서 내 직급 높은순 및 동급 시 이름 오름차순 검증")
    void testGradeOrderWithinDepartment() {
        Department dev = dept("개발팀");
        Grade bujang = grade("부장", 6L);
        Grade chajang = grade("차장", 5L);
        Grade gwajang = grade("과장", 4L);
        Grade sawon = grade("사원", 1L);

        List<Employee> list = new ArrayList<>(List.of(
                emp("김사원", dev, sawon),
                emp("홍과장", dev, gwajang),
                emp("강부장", dev, bujang),
                emp("박과장", dev, gwajang),
                emp("이차장", dev, chajang)
        ));

        list.sort(EmployeeSortHelper.EMPLOYEE_COMPARATOR);

        assertThat(list).extracting(Employee::getEmplName)
                .containsExactly("강부장", "이차장", "박과장", "홍과장", "김사원");
    }

    @Test
    @DisplayName("직급은 부장보다 높고 부서가 없는 직원은 최상단에 나열 검증")
    void testAboveBujangWithoutDept_placedAtTop() {
        Department hr = dept("인사팀");
        Grade bujang = grade("부장", 6L);
        Grade daepyo = grade("대표이사", 10L);
        Grade isa = grade("이사", 8L);

        List<Employee> list = new ArrayList<>(List.of(
                emp("인사부장", hr, bujang),
                emp("김대표", null, daepyo),
                emp("박이사", null, isa)
        ));

        list.sort(EmployeeSortHelper.EMPLOYEE_COMPARATOR);

        // 대표이사 -> 이사 -> 인사팀 순서
        assertThat(list).extracting(Employee::getEmplName)
                .containsExactly("김대표", "박이사", "인사부장");
    }

    @Test
    @DisplayName("부서 지정 + 직급 미지정 직원은 해당 부서의 마지막에 이름 오름차순으로 나열 검증")
    void testDeptWithNoGrade_placedAtEndOfDepartment() {
        Department dev = dept("개발팀");
        Department hr = dept("인사팀");
        Grade sawon = grade("사원", 1L);
        Grade bujang = grade("부장", 6L);

        List<Employee> list = new ArrayList<>(List.of(
                emp("개발직급없음B", dev, null),
                emp("개발사원", dev, sawon),
                emp("개발직급없음A", dev, null),
                emp("개발부장", dev, bujang),
                emp("인사부장", hr, bujang)
        ));

        list.sort(EmployeeSortHelper.EMPLOYEE_COMPARATOR);

        // 인사부장 -> 개발부장 -> 개발사원 -> 개발직급없음A -> 개발직급없음B
        assertThat(list).extracting(Employee::getEmplName)
                .containsExactly("인사부장", "개발부장", "개발사원", "개발직급없음A", "개발직급없음B");
    }

    @Test
    @DisplayName("부서 없음 + 직급 부장 이하 직원은 디자인팀 이후에 나열 검증")
    void testBelowOrEqualBujangWithoutDept_placedAfterDesign() {
        Department design = dept("디자인팀");
        Grade bujang = grade("부장", 6L);
        Grade gwajang = grade("과장", 4L);
        Grade sawon = grade("사원", 1L);

        List<Employee> list = new ArrayList<>(List.of(
                emp("부서없는과장", null, gwajang),
                emp("디자인사원", design, sawon),
                emp("부서없는부장", null, bujang),
                emp("부서없는사원B", null, sawon),
                emp("부서없는사원A", null, sawon)
        ));

        list.sort(EmployeeSortHelper.EMPLOYEE_COMPARATOR);

        // 디자인사원 -> 부서없는부장 -> 부서없는과장 -> 부서없는사원A -> 부서없는사원B
        assertThat(list).extracting(Employee::getEmplName)
                .containsExactly("디자인사원", "부서없는부장", "부서없는과장", "부서없는사원A", "부서없는사원B");
    }

    @Test
    @DisplayName("부서와 직급 모두 미지정인 직원은 디자인팀 이후에 이름 오름차순으로 나열 검증")
    void testNoDeptAndNoGrade_placedAfterDesign() {
        Department design = dept("디자인팀");
        Grade sawon = grade("사원", 1L);
        Grade bujang = grade("부장", 6L);

        List<Employee> list = new ArrayList<>(List.of(
                emp("미지정B", null, null),
                emp("디자인사원", design, sawon),
                emp("부서없는부장", null, bujang),
                emp("미지정A", null, null)
        ));

        list.sort(EmployeeSortHelper.EMPLOYEE_COMPARATOR);

        // 디자인사원 -> 부서없는부장 -> 미지정A -> 미지정B
        assertThat(list).extracting(Employee::getEmplName)
                .containsExactly("디자인사원", "부서없는부장", "미지정A", "미지정B");
    }
}
