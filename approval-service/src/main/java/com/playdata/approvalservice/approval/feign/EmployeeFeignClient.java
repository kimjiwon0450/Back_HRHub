package com.playdata.approvalservice.approval.feign;

import com.playdata.approvalservice.common.dto.EmployeeResDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;

@FeignClient(name = "hr-service", path = "/hr-service/employees")
public interface EmployeeFeignClient {
    /**
     * 직원 단건 조회
     */
    @GetMapping("/{id}")
    ResponseEntity<EmployeeResDto> getById(@PathVariable("id") Long id);


    /**
     * email로 직원 Id 조회
     * @param email
     * @return
     */
    @GetMapping("/email/{email}")
    ResponseEntity<EmployeeResDto> getEmployeeByEmail(@PathVariable("email") String email);
}
