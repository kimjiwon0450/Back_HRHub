package com.playdata.hrservice.hr.controller;
import com.playdata.hrservice.common.auth.JwtTokenProvider;
import com.playdata.hrservice.common.dto.CommonErrorDto;
import com.playdata.hrservice.common.dto.CommonResDto;
import com.playdata.hrservice.hr.dto.EmployeePasswordDto;
import com.playdata.hrservice.hr.dto.EmployeeReqDto;
import com.playdata.hrservice.hr.dto.EmployeeResDto;
import com.playdata.hrservice.hr.entity.Employee;
import com.playdata.hrservice.hr.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/hr-service")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    private final Environment env;

    @PostMapping("/employees")
    public ResponseEntity<?> createUser(@RequestBody EmployeeReqDto dto) {
        employeeService.createUser(dto);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/employees/password")
    public ResponseEntity<?> modifyPassword(@RequestBody EmployeePasswordDto dto) {
        employeeService.modifyPassword(dto);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "Success", null),HttpStatus.OK);
    }

    @PostMapping("/employees/login")
    public ResponseEntity<?> login(@RequestBody EmployeeReqDto dto) {
        EmployeeResDto employeeDto = employeeService.login(dto);

        String token
                = jwtTokenProvider.createToken(employeeDto.getEmail(), employeeDto.getRole().toString());
        String refreshToken
                = jwtTokenProvider.createRefreshToken(employeeDto.getEmail(), employeeDto.getRole().toString());

        redisTemplate.opsForValue().set("user:refresh:" + employeeDto.getEmployeeId(), refreshToken, 2, TimeUnit.MINUTES);




        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("token", token);
        loginInfo.put("id", employeeDto.getEmployeeId());
        loginInfo.put("name", employeeDto.getName());
        loginInfo.put("role", employeeDto.getRole().toString());

        CommonResDto resDto
                = new CommonResDto(HttpStatus.OK,
                "Login Success", loginInfo);
        return new ResponseEntity<>(resDto, HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> map) {
        String id = map.get("id");
        log.info("/user/refresh: POST, id: {}", id);
        // redis에 해당 id로 조회되는 내용이 있는지 확인
        Object obj = redisTemplate.opsForValue().get("user:refresh:" + id);
        log.info("obj: {}", obj);
        if (obj == null) { // refresh token이 수명이 다됨.
            return new ResponseEntity<>(new CommonErrorDto(
                    HttpStatus.UNAUTHORIZED, "EXPIRED_RT"),
                    HttpStatus.UNAUTHORIZED);
        }
        // 새로운 access token을 발급
        Employee employee = employeeService.findById(Long.parseLong(id));
        String newAccessToken
                = jwtTokenProvider.createToken(employee.getEmail(), employee.getRole().toString());

        Map<String, Object> info = new HashMap<>();
        info.put("token", newAccessToken);
        CommonResDto resDto
                = new CommonResDto(HttpStatus.OK, "새 토큰 발급됨", info);
        return ResponseEntity.ok().body(resDto);

    }

    @GetMapping("/health-check")
    public String healthCheck() {
        String msg = "";
        msg += "token.exp_time:" + env.getProperty("token.expiration_time") +"\n";
        return msg;
    }

}
