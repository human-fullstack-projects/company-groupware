package com.company.groupware.service;

import com.company.groupware.entity.Department;
import com.company.groupware.repository.DepartmentRepository;
import com.company.groupware.repository.EmployeeRepository;
import com.company.groupware.util.EmployeeSortHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    // DB에 등록된 전체 부서 목록을 지정된 순서(인사팀, 재무팀, 기획팀, 개발팀, 디자인팀)로 정렬하여 반환
    @Transactional(readOnly = true)
    public List<Department> getDepartments() {
        List<Department> list = departmentRepository.findAll();
        return list.stream()
                .sorted(Comparator.comparingInt(EmployeeSortHelper::getDepartmentOrder)
                        .thenComparing(Department::getDeptName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /**
     * 부서 추가
     */
    @Transactional
    public void addDepartment(String deptName) {
        if (!StringUtils.hasText(deptName)) {
            throw new IllegalArgumentException("부서 이름을 입력해주세요.");
        }
        deptName = deptName.trim();

        if (departmentRepository.existsByDeptName(deptName)) {
            throw new IllegalArgumentException("이미 존재하는 부서 이름입니다.");
        }

        departmentRepository.save(Department.builder().deptName(deptName).build());
    }

    /**
     * 부서 삭제 (소속 직원이 있으면 삭제 불가)
     */
    @Transactional
    public void deleteDepartment(Long deptId) {
        Department department = departmentRepository.findById(deptId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 부서입니다."
                ));

        if (employeeRepository.countByDepartment_DeptId(deptId) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "이 부서에 소속된 직원이 있어 삭제할 수 없습니다."
            );
        }

        departmentRepository.delete(department);
    }
}