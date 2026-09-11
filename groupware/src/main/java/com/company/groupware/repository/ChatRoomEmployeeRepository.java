package com.company.groupware.repository;

import com.company.groupware.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomEmployeeRepository
        extends JpaRepository<Employee, Long> {

    Optional<Employee> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    List<Employee> findByEmplNameContaining(String emplName);
}