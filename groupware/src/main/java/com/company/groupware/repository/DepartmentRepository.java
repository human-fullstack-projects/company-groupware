package com.company.groupware.repository;

import com.company.groupware.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository
        extends JpaRepository<Department, Long> {

    boolean existsByDeptName(String deptName);
}