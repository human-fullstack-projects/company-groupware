package com.company.groupware.repository;

import com.company.groupware.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    long countByEmplStatTrue();

    long countByDepartment_DeptId(Long deptId);

    long countByGrade_GradeId(Long gradeId);

    /**
     * 1. [직원조회 페이지용]
     * - 검색 조건이 없으면 전체 직원이 조회됨
     * - 부서별 오름차순(d.deptName ASC), 직급별 오름차순(g.gradeId ASC), 이름 오름차순(e.emplName ASC) 정렬
     */
    @Query("SELECT e FROM Employee e " +
            "LEFT JOIN FETCH e.department d " +
            "LEFT JOIN FETCH e.grade g " +
            "WHERE (:departmentId IS NULL OR d.deptId = :departmentId) " +
            "AND (:gradeId IS NULL OR g.gradeId = :gradeId) " +
            "AND (:emplName IS NULL OR e.emplName LIKE %:emplName%) " +
            "ORDER BY d.deptName ASC, g.gradeId ASC, e.emplName ASC")
    List<Employee> searchEmployees(
            @Param("departmentId") Long departmentId,
            @Param("gradeId") Long gradeId,
            @Param("emplName") String emplName
    );

    /**
     * 2. [부서별 조직도 페이지용]
     * - 특정 부서의 직원들을 조회
     * - 직급이 높은 순(DESC)으로 위에서 아래에 오도록 정렬 (g.gradeId DESC) 후 이름순 정렬
     */
    @Query("SELECT e FROM Employee e " +
            "LEFT JOIN FETCH e.grade g " +
            "LEFT JOIN FETCH e.department d " +
            "WHERE d.deptId = :deptId " +
            "ORDER BY g.gradeId ASC, e.emplName ASC")
    List<Employee> findOrgEmployeesByDepartmentId(@Param("deptId") Long deptId);

//    List<Employee> findByDepartmentDeptId(@Param("deptId") Long deptId);
}