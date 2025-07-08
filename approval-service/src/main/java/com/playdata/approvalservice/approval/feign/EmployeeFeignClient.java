package com.playdata.approvalservice.approval.feign;

import com.playdata.approvalservice.common.dto.EmployeeResDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "hr-service")
public interface EmployeeFeignClient {
    /**
     * 직원 단건 조회
     */
    @GetMapping("/feign/employees/{id}")
    ResponseEntity<EmployeeResDto> getById(@PathVariable("id") Long employeeId);


    /**
     * email로 직원 Id 조회
     * @param email
     * @return
     */
    @GetMapping("/employees/email/{email}")
    ResponseEntity<EmployeeResDto> getEmployeeByEmail(@PathVariable("email") String email);

    /**
     * 여러 사번에 대한 이름을 한 번에 조회
     * -> GET /hr-service/employees/names?ids=1,2,3
     */
    @GetMapping("/employees/names")
    Map<Long, String> getEmployeeNamesByEmployeeIds(
            @RequestParam("ids") List<Long> employeeIds
    );

}
