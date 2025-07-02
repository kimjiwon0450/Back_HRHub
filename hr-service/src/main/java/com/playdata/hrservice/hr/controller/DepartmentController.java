package com.playdata.hrservice.hr.controller;

import com.playdata.hrservice.common.auth.JwtTokenProvider;
import com.playdata.hrservice.common.dto.CommonResDto;
import com.playdata.hrservice.hr.dto.DepartmentResDto;
import com.playdata.hrservice.hr.entity.Department;
import com.playdata.hrservice.hr.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hr-service")
@Slf4j
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentService departmentService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/departments/{id}")
    public ResponseEntity<?> getDepartmentById(@PathVariable Long id) {
        CommonResDto resDto = new CommonResDto(
                HttpStatus.OK, "Success", departmentService.getDepartment(id)
        );
        return new ResponseEntity<>(resDto, HttpStatus.OK);
    }

    @GetMapping("/departments")
    public ResponseEntity<?> getAllDepartment() {
        CommonResDto resDto = new CommonResDto(
                HttpStatus.OK, "Success", departmentService.getAllDepartments()
        );
        return new ResponseEntity<>(resDto, HttpStatus.OK);
    }
}
