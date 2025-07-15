package com.playdata.hrservice.hr.controller;

import com.playdata.hrservice.hr.dto.EmployeeResDto;
import com.playdata.hrservice.hr.service.EmployeeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/feign/employees")
public class EmployeeFeignController {

    private final EmployeeService employeeService;

    // 생성자 주입
    public EmployeeFeignController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * Feign 클라이언트에서 이메일로 직원 정보를 조회하기 위한 엔드포인트입니다.
     * /feign/employees/email/{email} 경로로 GET 요청을 처리합니다.
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<EmployeeResDto> getEmployeeByEmail(@PathVariable("email") String email) {
        EmployeeResDto employee = employeeService.getEmployeeByEmail(email);

        return ResponseEntity.ok(employee);
    }

    /**
     * Feign 클라이언트에서 직원 ID로 직원 정보를 조회하기 위한 엔드포인트입니다.
     * /feign/employees/{id} 경로로 GET 요청을 처리합니다.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResDto> getById(@PathVariable("id") Long employeeId) {
        EmployeeResDto employee = employeeService.getEmployee(employeeId);

        return ResponseEntity.ok(employee);
    }

    /**
     * Feign 클라이언트에서 여러 직원 ID로 직원 이름 목록을 조회하기 위한 엔드포인트입니다.
     * /feign/employees/names?ids=1,2,3 형태의 GET 요청을 처리합니다.
     */
    @GetMapping("/names")
    public ResponseEntity<Map<Long, String>> getEmployeeNamesByEmployeeIds(@RequestParam("ids") List<Long> employeeIds) {
        Map<Long, String> employeeNames = employeeService.getEmployeeNamesByEmployeeIds(employeeIds);
        return ResponseEntity.ok(employeeNames);
    }

    /**
     * Feign 클라이언트에서 이메일 주소로 직원의 ID(PK)를 조회하기 위한 API입니다.
     * GET /feign/employees/id?email=... 요청을 처리합니다.
     */
    @GetMapping("/id")
    public ResponseEntity<Long> findIdByEmail(@RequestParam("email") String email) {
        Long employeeId = employeeService.findIdByEmail(email);
        return ResponseEntity.ok(employeeId);
    }
}