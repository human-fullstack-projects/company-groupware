package com.company.groupware.repository;

import com.company.groupware.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository
        extends JpaRepository<Employee, Long> {

    Optional<Employee> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);
}