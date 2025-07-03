package com.playdata.hrservice.hr.repository;

import com.playdata.hrservice.hr.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmail(String email);

   Optional<Employee> findByEmployeeId(Long employeeId);
}
