package com.company.groupware.service;

import com.company.groupware.entity.Department;
import com.company.groupware.repository.DepartmentRepository;
import com.company.groupware.util.EmployeeSortHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    // DB에 등록된 전체 부서 목록을 지정된 순서(인사팀, 재무팀, 기획팀, 개발팀, 디자인팀)로 정렬하여 반환
    @Transactional(readOnly = true)
    public List<Department> getDepartments() {
        List<Department> list = departmentRepository.findAll();
        return list.stream()
                .sorted(Comparator.comparingInt(EmployeeSortHelper::getDepartmentOrder)
                        .thenComparing(Department::getDeptName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}