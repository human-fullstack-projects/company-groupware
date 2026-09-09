package com.company.groupware.service;

import com.company.groupware.entity.Department;
import com.company.groupware.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    // DB에 등록된 전체 부서 목록 조회
    @Transactional(readOnly = true)
    public List<Department> getDepartments() {
        return departmentRepository.findAll();
    }
}