package com.playdata.approvalservice.approval.feign;

import jakarta.persistence.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.Date;

@FeignClient(name = "hr-service", path = "/employees")
public interface EmployeeFeignClient {
    @GetMapping("/{id}")
    EmployeeResDto getById(@PathVariable("id") Long id);
}
