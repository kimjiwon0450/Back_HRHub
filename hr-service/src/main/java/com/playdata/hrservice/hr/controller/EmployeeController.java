package com.playdata.hrservice.hr.controller;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.playdata.hrservice.common.auth.JwtTokenProvider;
import com.playdata.hrservice.common.auth.Role;
import com.playdata.hrservice.common.auth.TokenUserInfo;
import com.playdata.hrservice.common.dto.CommonErrorDto;
import com.playdata.hrservice.common.dto.CommonResDto;
import com.playdata.hrservice.hr.dto.*;
import com.playdata.hrservice.hr.entity.Employee;
import com.playdata.hrservice.hr.service.EmployeeService;
import com.playdata.hrservice.hr.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/hr")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    private final S3Service s3Service;
    private final Environment env;

    // 직원 등록
    @PostMapping("/employees")
    public ResponseEntity<?> createUser(@RequestBody EmployeeReqDto dto) throws JsonProcessingException {
        log.info("Create employee : {}", dto);
        employeeService.createUser(dto);
        return ResponseEntity.ok().build();
    }

    // 비밀번호 설정
    @PatchMapping("/employees/password")
    public ResponseEntity<CommonResDto> modifyPassword(@RequestBody EmployeePasswordDto dto) {
        employeeService.modifyPassword(dto);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "Success", null),HttpStatus.OK);
    }

    // 이메일 인증 전송
    @GetMapping("/employees/email/verification/{email}")
    public ResponseEntity<CommonResDto> sendVerificationEmail(@PathVariable String email) {
        employeeService.sendVerificationEmail(email);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "Success", null),HttpStatus.OK);
    }

    // 로그인
    @PostMapping("/employees/login")
    public ResponseEntity<CommonResDto<Map<String, Object>>> login(@RequestBody EmployeeReqDto dto) {
        EmployeeResDto employeeDto = employeeService.login(dto);

        String token
                = jwtTokenProvider.createToken(employeeDto.getEmployeeId(), employeeDto.getEmail(), employeeDto.getRole().toString(), employeeDto.getDepartmentId());
        String refreshToken
                = jwtTokenProvider.createRefreshToken(employeeDto.getEmployeeId(), employeeDto.getEmail(), employeeDto.getRole().toString(), employeeDto.getDepartmentId());

        redisTemplate.opsForValue().set("user:refresh:" + employeeDto.getEmployeeId(), refreshToken, 180, TimeUnit.MINUTES);

        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("token", token);
        loginInfo.put("id", employeeDto.getEmployeeId());
        loginInfo.put("name", employeeDto.getName());
        loginInfo.put("role", employeeDto.getRole().toString());
        loginInfo.put("position", employeeDto.getPosition());
        loginInfo.put("depId", employeeDto.getDepartmentId());


        CommonResDto<Map<String,Object>> resDto
                = new CommonResDto<>(HttpStatus.OK,
                "Login Success", loginInfo);
        return new ResponseEntity<>(resDto, HttpStatus.OK);
    }

    // 간소화 된 직원 리스트
    @GetMapping("/employees")
    public ResponseEntity<CommonResDto<Page<EmployeeListResDto>>> getEmployeesList(@PageableDefault(size = 10, sort = "employeeId") Pageable pageable, @RequestParam(required = false) String field,
                                                                                   @RequestParam(required = false) String keyword,
                                                                                   @RequestParam(required = false) String department,
                                                                                   @RequestParam(required = false, defaultValue = "true") String isActive,
                                                                                   @AuthenticationPrincipal TokenUserInfo tokenUserInfo) {
        return new ResponseEntity<>(
                new CommonResDto<>(HttpStatus.OK,
                    "Success",
                    employeeService.getEmployeeList(pageable, field, keyword, department, tokenUserInfo, false, Boolean.parseBoolean(isActive)))
                , HttpStatus.OK);
    }// 간소화 된 직원 리스트
    @GetMapping("/employees/contact")
    public ResponseEntity<CommonResDto<Page<EmployeeListResDto>>> getEmployeesListForContact(@PageableDefault(size = 10, sort = "employeeId") Pageable pageable, @RequestParam(required = false) String field,
                                                                                   @RequestParam(required = false) String keyword,
                                                                                             @RequestParam(required = false) String department,
                                                                                             @RequestParam(required = false, defaultValue = "true") String isActive,
                                                                                   @AuthenticationPrincipal TokenUserInfo tokenUserInfo) {
        return new ResponseEntity<>(
                new CommonResDto<>(HttpStatus.OK,
                    "Success",
                    employeeService.getEmployeeList(pageable, field, keyword, department, tokenUserInfo, true, Boolean.parseBoolean(isActive)))
                , HttpStatus.OK);
    }

    // 직원 상세조회
    @GetMapping("/employees/{id}")
    public ResponseEntity<CommonResDto<EmployeeResDto>> getEmployee(@PathVariable("id") Long id) {
        return new ResponseEntity<>(new CommonResDto<>(HttpStatus.OK, "Success", employeeService.getEmployee(id)), HttpStatus.OK);
    }

    // 직원 이름 조회
    @GetMapping("/employees/{id}/name")
    public ResponseEntity<CommonResDto<String>> getEmployeeName(@PathVariable("id") Long id) {
        return new ResponseEntity<>(new CommonResDto<>(HttpStatus.OK, "Success", employeeService.getEmployeeName(id)), HttpStatus.OK);
    }
    // 직원 부서명 조회
    @GetMapping("/employees/{id}/name/department")
    public ResponseEntity<CommonResDto<String>> getDepartmentNameOfEmployee(@PathVariable("id") Long id) {
        return new ResponseEntity<>(new CommonResDto<>(HttpStatus.OK, "Success", employeeService.getDepartmentNameOfEmployee(id)), HttpStatus.OK);
    }


    // 직원 정보수정
    @PatchMapping("/employees/{id}")
    public ResponseEntity<CommonResDto<EmployeeResDto>> modifyEmployeeInfo(@PathVariable("id") Long id, @RequestBody EmployeeReqDto dto, @AuthenticationPrincipal TokenUserInfo tokenUserInfo) throws JsonProcessingException {
        log.info("Modify employee : {}", dto);
        Role role = tokenUserInfo.getRole();
        EmployeeResDto employeeResDto = employeeService.modifyEmployeeInfo(id, dto, role);
        return new ResponseEntity<>(new CommonResDto<>(HttpStatus.OK, "Success", employeeResDto), HttpStatus.OK);
    }

    // 직원 퇴사 처리
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR_MANAGER')")
    @PatchMapping("/employee/{id}/retire")
    public ResponseEntity<CommonResDto> retireEmployee(@PathVariable("id") Long id){

        employeeService.deleteEmployee(id);

        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "Success", null), HttpStatus.OK);
    }

    //프로필 이미지 업로드
    @PostMapping("/profileImage")
    public ResponseEntity<?> uploadFile(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            String targetEmail,
            @RequestParam("file") MultipartFile file) throws Exception {

        // 토큰으로 인증 유저 정보 확인
        String userEmail = userInfo.getEmail();
        Role userRole = userInfo.getRole();
        //타인 사진을 변경하는 요청이 들어오는 요청거부(employee 일때)
        if(userRole.equals(Role.EMPLOYEE)&& !userEmail.equals(targetEmail)) {
            return new ResponseEntity<>(new CommonResDto(HttpStatus.BAD_REQUEST, "본인 사진만 변경가능합니다", null), HttpStatus.BAD_REQUEST);
        }

        String resImageUri = s3Service.uploadProfile(targetEmail, file);
        return ResponseEntity.ok(resImageUri);
    }

    // 인사 이동 이력
//    @PreAuthorize("hasRole('ADMIN') or hasRole('HR_MANAGER')")
    @GetMapping("/transfer-history/{employeeId}")
    public ResponseEntity<CommonResDto<HrTransferHistoryResDto>> getTransferHistory(@PathVariable("employeeId") Long employeeId, @AuthenticationPrincipal TokenUserInfo tokenUserInfo) throws JsonProcessingException {
        HrTransferHistoryResDto resDto = employeeService.getTransferHistory(employeeId, tokenUserInfo);
        return new ResponseEntity<>(new CommonResDto<>(HttpStatus.OK, "success", resDto), HttpStatus.OK);
    }

    // 토큰 리프레시
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
                = jwtTokenProvider.createToken(employee.getEmployeeId(), employee.getEmail(), employee.getRole().toString(), employee.getDepartment().getId());

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

    // ✅ Bulk 조회 API 추가
    @PostMapping("/employees/bulk")
    public ResponseEntity<CommonResDto> getEmployeesByIds(@RequestBody Set<Long> employeeIds) {
        List<EmployeeResDto> users = employeeService.findByIds(employeeIds);
        log.info("getEmployeesByIds: {}", users);
        return ResponseEntity.ok().body(CommonResDto.success(users));
    }


}
