package com.playdata.approvalservice.approval.feign;

import com.playdata.approvalservice.common.dto.EmployeeResDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "hr")
public interface EmployeeFeignClient {
    /**
     * 직원 단건 조회
     * @param employeeId 조회할 직원의 ID
     * @return 직원의 정보가 담긴 ResponseEntity
     */
    @GetMapping("/feign/employees/{id}")
    ResponseEntity<EmployeeResDto> getById(@PathVariable("id") Long employeeId);

    /**
     * email로 직원 정보 조회
     * @param email 조회할 직원의 이메일
     * @return 직원의 정보가 담긴 ResponseEntity
     */
    @GetMapping("/feign/employees/email/{email}")
    ResponseEntity<EmployeeResDto> getEmployeeByEmail(@PathVariable("email") String email);

    /**
     * 여러 직원 ID에 대한 이름을 한 번에 조회
     * GET /hr-service/feign/employees/names?ids=1,2,3
     * @param employeeIds 조회할 직원 ID 목록
     * @return 직원 ID와 이름이 매핑된 Map을 담은 ResponseEntity
     */
    @GetMapping("/feign/employees/names")
    ResponseEntity<Map<Long, String>> getEmployeeNamesByEmployeeIds(
            @RequestParam("ids") List<Long> employeeIds
    );

    ResponseEntity<Long> findIdByEmail(String writerEmail);
}