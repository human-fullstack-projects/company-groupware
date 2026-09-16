package com.company.groupware.repository;

import com.company.groupware.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DepartmentRepository
        extends JpaRepository<Department, Long> {

    boolean existsByDeptName(String deptName);

    // DB에서 부서 목록을 ID 순서대로 조회 (필요시 정렬 조건 변경 가능)
    @Query
            ("SELECT d FROM Department d ORDER BY d.deptId ASC")
    List<Department> findAllDepartmentsOrderById();

}