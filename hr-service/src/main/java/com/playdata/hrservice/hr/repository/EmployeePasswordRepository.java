package com.playdata.hrservice.hr.repository;

import com.playdata.hrservice.hr.entity.EmployeePassword;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeePasswordRepository extends JpaRepository<EmployeePassword, Long> {
}
