package com.playdata.hrservice.hr.repository;

import com.playdata.hrservice.common.auth.Role;
import com.playdata.hrservice.hr.entity.Employee;
import com.playdata.hrservice.hr.entity.EmployeeStatus;
import com.playdata.hrservice.hr.entity.Position;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmail(String email);

    Optional<Employee> findByEmployeeId(Long employeeId);

    Page<Employee> findByEmployeeId(Long employeeId, Pageable pageable);

    Page<Employee> findByNameContaining(String keyword, Pageable pageable);

    Page<Employee> findByDepartmentNameContaining(String keyword, Pageable pageable);

    Page<Employee> findByNameContainingAndDepartmentNameContaining(String name, String departmentName, Pageable pageable);

    Page<Employee> findByRoleAndDepartmentNameContaining(Role role, String department, Pageable pageable);

    Page<Employee> findByRole(Role role, Pageable pageable);

    Page<Employee> findByPhoneContaining(String keyword, Pageable pageable);

    Page<Employee> findByPhoneContainingAndDepartmentNameContaining(String keyword, String department, Pageable pageable);

    Page<Employee> findByPosition(Position position, Pageable pageable);

    Page<Employee> findByPositionAndDepartmentNameContaining(Position position, String department, Pageable pageable);

    List<Employee> findByStatus(EmployeeStatus status);

    Page<Employee> findByNameContainingAndDepartmentNameContainingAndStatus(String keyword, String department, EmployeeStatus status, Pageable pageable);

    Page<Employee> findByNameContainingAndStatus(String keyword, EmployeeStatus status, Pageable pageable);

    Page<Employee> findByPositionAndDepartmentNameContainingAndStatus(Position position, String department, EmployeeStatus status, Pageable pageable);

    Page<Employee> findByPositionAndStatus(Position position, EmployeeStatus status, Pageable pageable);

    Page<Employee> findByRoleAndDepartmentNameContainingAndStatus(Role role, String department, EmployeeStatus status, Pageable pageable);

    Page<Employee> findByRoleAndStatus(Role role, EmployeeStatus status, Pageable pageable);

    Page<Employee> findByDepartmentNameContainingAndStatus(String keyword, EmployeeStatus status, Pageable pageable);

    Page<Employee> findByPhoneContainingAndDepartmentNameContainingAndStatus(String keyword, String department, EmployeeStatus status, Pageable pageable);

    Page<Employee> findByPhoneContainingAndStatus(String keyword, EmployeeStatus status, Pageable pageable);

    Page<Employee> findAllByStatus(EmployeeStatus status, Pageable pageable);

    Page<Employee> findByEmployeeIdAndStatus(Long employeeId, EmployeeStatus status, Pageable pageable);
}
