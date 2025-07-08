package com.playdata.hrservice.hr.repository;

import com.playdata.hrservice.hr.entity.Employee;
import com.playdata.hrservice.hr.entity.HrTransferHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HrTransferHistoryRepository extends JpaRepository<HrTransferHistory, Long> {
    HrTransferHistory findByEmployee(Employee employee);
}
