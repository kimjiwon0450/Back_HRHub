package com.playdata.hrservice.hr.service;


import com.playdata.hrservice.common.auth.Role;
import com.playdata.hrservice.common.auth.TokenUserInfo;
import com.playdata.hrservice.common.config.AwsS3Config;
import com.playdata.hrservice.hr.dto.EmployeeListResDto;
import com.playdata.hrservice.hr.dto.EmployeePasswordDto;
import com.playdata.hrservice.hr.dto.EmployeeReqDto;
import com.playdata.hrservice.hr.dto.EmployeeResDto;
import com.playdata.hrservice.hr.entity.Employee;
import com.playdata.hrservice.hr.entity.EmployeePassword;
import com.playdata.hrservice.hr.entity.EmployeeStatus;
import com.playdata.hrservice.hr.repository.EmployeePasswordRepository;
import com.playdata.hrservice.hr.repository.EmployeeRepository;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeePasswordRepository employeePasswordRepository;
    private final PasswordEncoder encoder;
    private final AwsS3Config awsS3Config;
    private final RedisTemplate<String, Object> redisTemplate;
    private final DepartmentService departmentService;


    @Transactional
    public void createUser(EmployeeReqDto dto) {
        // 1. 이메일 중복 확인 (신규 가입에만 해당)
        if (employeeRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 이메일 입니다!");
        }



        Employee save = employeeRepository.save(
                Employee.builder()
                .email(dto.getEmail())
                .name(dto.getName())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .department(departmentService.getDepartmentEntity(dto.getDepartmentId()))
                .birthday(dto.getBirthday())
                .status(EmployeeStatus.valueOf(dto.getStatus()))
                .role(Role.valueOf(dto.getRole()))
                .profileImageUri(dto.getProfileImageUri())
                .memo(dto.getMemo())
                .build()
        );
        EmployeePassword employeePassword = EmployeePassword.builder()
                .userId(save.getEmployeeId()).build();
        employeePasswordRepository.save(employeePassword);
    }
    public void modifyPassword(EmployeePasswordDto dto) {
        Employee employee = employeeRepository.findByEmail(dto.getEmail()).orElseThrow(
                () -> new EntityNotFoundException("There is no employee with email: " + dto.getEmail())
        );

        // 비밀번호 길이 검사 (패턴 검사도 필요하다면 여기에 추가)
        if (dto.getPassword().length() < 8) {
            throw new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다.");
        }
        EmployeePassword employeePassword = employeePasswordRepository.findById(employee.getEmployeeId()).orElseThrow(
                () -> new EntityNotFoundException("There is no employee with id: " + employee.getEmployeeId())
        );

        String finalEncodedPassword = encoder.encode(dto.getPassword()); // hashString
        byte[] hashBytes = finalEncodedPassword.getBytes(StandardCharsets.UTF_8); // hashBytes
        employeePassword.setPasswordHash(hashBytes);
        employeePasswordRepository.save(employeePassword);
    }

    public Employee findByEmail(String email) {
        return null;
    }
    public Employee findById(Long id) {
        return employeeRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Employee not found by id")
        );
    }

    public EmployeeResDto login(EmployeeReqDto dto) {
        if (dto.getPassword().length() < 8) {
            throw new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다.");
        }

        Employee employee = employeeRepository.findByEmail(dto.getEmail()).orElseThrow(
                () -> new EntityNotFoundException("Employee not found!")
        );

        EmployeePassword employeePassword = employeePasswordRepository.findById(employee.getEmployeeId()).orElseThrow(
                () -> new EntityNotFoundException("There is no employee with id: " + employee.getEmployeeId())
        );

        if (!encoder.matches(dto.getPassword(), new String(employeePassword.getPasswordHash(), StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        return employee.toDto();
    }

    public Page<EmployeeListResDto> getEmployeeList(Pageable pageable) {
        Page<Employee> page = employeeRepository.findAll(pageable);
        return page.map(employee -> EmployeeListResDto.builder()
                .id(employee.getEmployeeId())
                .name(employee.getName())
                .phone(employee.getPhone())
                .department(employee.getDepartment().getName())
                .role(employee.getRole().name())
                .build());
    }

    public EmployeeResDto getEmployee(Long id) {
        return employeeRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("해당 직원은 존재하지 않습니다.")
        ).toDto();
    }

    @Transactional
    public void modifyEmployeeInfo(Long id, EmployeeReqDto dto, Role role) {
        Employee employee = employeeRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Employee not found!")
        );
        if(role.equals(Role.ADMIN) || role.equals(Role.HR_MANAGER)) {
            employee.updateRole(Role.valueOf(dto.getRole()));
        }
        employee.updateFromDto(dto);
        employee.updateDepartment(departmentService.getDepartmentEntity(dto.getDepartmentId()));
    }

    public void insertTransferHistory() {
        //여기서 인사이동

    }

    public String getEmployeeName(Long id) {
        return employeeRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("해당 직원이 존재하지 않습니다.")        ).getName();
    }

    public String getDepartmentNameOfEmployee(Long id) {
        return employeeRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("해당 직원이 존재하지 않습니다.")        ).getDepartment().getName();
    }


//
//
//    public String uploadProfile(UserRequestDto userRequestDto) throws Exception {
//        User user = userRepository.findById(userRequestDto.getId()).orElseThrow(
//                () -> new EntityNotFoundException("User not found!")
//        );
//
//        // 1) 이전 프로필이 기본 url이 아니고, null도 아니라면 삭제
//        String oldUrl = user.getProfileImage();
//        if (oldUrl != null && !oldUrl.isBlank()) {
//            awsS3Config.deleteFromS3Bucket(oldUrl);
//
//        }
//
//        //2) 새 파일 업로드
//        MultipartFile profileImage = userRequestDto.getProfileImage();
//        String uniqueFileName = UUID.randomUUID() + "_" + profileImage.getOriginalFilename();
//        String imageUrl = awsS3Config.uploadToS3Bucket(profileImage.getBytes(), uniqueFileName);
//
//
//        user.setProfileImage(imageUrl);
//        userRepository.save(user);
//        return imageUrl;
//    }
}





