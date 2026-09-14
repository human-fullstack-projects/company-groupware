package com.company.groupware.repository;

import com.company.groupware.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository
        extends JpaRepository<Employee, Long> {

    Optional<Employee> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    long countByEmplStatTrue();

    long countByDepartment_DeptId(Long deptId);

    long countByGrade_GradeId(Long gradeId);

    @Query("SELECT e FROM Employee e " +
            "LEFT JOIN FETCH e.department " +
            "LEFT JOIN FETCH e.grade " +
            "WHERE (:departmentId IS NULL OR e.department.deptId = :departmentId) " +
            "AND (:gradeId IS NULL OR e.grade.gradeId = :gradeId) " +
            "AND (:emplName IS NULL OR :emplName = '' OR e.emplName LIKE CONCAT('%', :emplName, '%')) " +
            "ORDER BY e.department.deptId ASC, e.grade.gradeId DESC, e.emplName ASC")
    List<Employee> searchEmployees(
            @Param("departmentId") Long departmentId,
            @Param("gradeId") Long gradeId,
            @Param("emplName") String emplName
    );
}