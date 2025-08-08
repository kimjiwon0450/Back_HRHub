package com.playdata.hrservice.hr.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playdata.hrservice.common.auth.Role;
import com.playdata.hrservice.common.auth.TokenUserInfo;
import com.playdata.hrservice.common.config.AwsS3Config;
import com.playdata.hrservice.hr.dto.*;
import com.playdata.hrservice.hr.entity.*;
import com.playdata.hrservice.hr.repository.EmployeePasswordRepository;
import com.playdata.hrservice.hr.repository.EmployeeRepository;
import com.playdata.hrservice.hr.repository.HrTransferHistoryRepository;
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
import java.nio.file.AccessDeniedException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;

import org.springframework.http.HttpStatus;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeePasswordRepository employeePasswordRepository;
    private final HrTransferHistoryRepository hrTransferHistoryRepository;
    private final VerificationService verificationService;
    private final PasswordEncoder encoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final DepartmentService departmentService;


    public void sendVerificationEmail(String email) {
        verificationService.sendVerificationEmail(email);
    }

    @Transactional
    public void createUser(EmployeeReqDto dto) throws JsonProcessingException {
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
                        .hireDate(dto.getHireDate())
                        .isNewEmployee(dto.getIsNewEmployee())
                        .status(EmployeeStatus.valueOf(dto.getStatus()))
                        .role(Role.valueOf(dto.getRole()))
                        .position(Position.valueOf(dto.getPosition()))
                        .profileImageUri(dto.getProfileImageUri())
                        .memo(dto.getMemo())
                        .build()
        );
        initTransferHistory(save, save.getDepartment().getId(), save.getPosition().name(), "");
        sendVerificationEmail(save.getEmail());
    }

    public void modifyPassword(EmployeePasswordDto dto) {
        Employee employee = employeeRepository.findByEmail(dto.getEmail()).orElseThrow(
                () -> new EntityNotFoundException("There is no employee with email: " + dto.getEmail())
        );

        // 비밀번호 길이 검사 (패턴 검사도 필요하다면 여기에 추가)
        if (dto.getPassword().length() < 8) {
            throw new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다.");
        }

//        if (!verificationService.verifyCode(dto.getEmail(), dto.getVerificationCode())) {
//            throw new IllegalArgumentException("인증번호가 일치하지 않거나 만료되었습니다.");
//        }

        String finalEncodedPassword = encoder.encode(dto.getPassword()); // hashString
        byte[] hashBytes = finalEncodedPassword.getBytes(StandardCharsets.UTF_8); // hashBytes
        employee.setPasswordHash(hashBytes);
        employeeRepository.save(employee);
    }

    public Employee findByEmail(String email) {
        return null;
    }

    public Employee findById(Long id) {
        return employeeRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("계정을 찾을 수 없습니다.")
        );
    }

    public EmployeeResDto login(EmployeeReqDto dto) {
        if (dto.getPassword().length() < 8) {
            throw new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다.");
        }

        Employee employee = employeeRepository.findByEmail(dto.getEmail()).orElseThrow(
                () -> new EntityNotFoundException("계정을 찾을 수 없습니다.")
        );

        // F2-25
        if (employee.getStatus().equals(EmployeeStatus.INACTIVE)) {
            throw new IllegalArgumentException("퇴사자 입니다. 인사부에 문의하세요");
        }

        if (employee.getPasswordHash() == null) {
            throw new IllegalStateException("해당 계정은 아직 비밀번호가 설정되지 않았습니다.");
        }

        if (!encoder.matches(dto.getPassword(), new String(employee.getPasswordHash(), StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        return employee.toDto();
    }

    public Page<EmployeeListResDto> getEmployeeList(Pageable pageable, String field, String keyword, String department, TokenUserInfo tokenUserInfo, boolean isContact, boolean isActive) {
        Page<Employee> page = null;
        EmployeeStatus status = isActive ? EmployeeStatus.ACTIVE : EmployeeStatus.INACTIVE;

        if (tokenUserInfo.getRole().equals(Role.ADMIN)||tokenUserInfo.getRole().equals(Role.HR_MANAGER)||isContact) {
            log.info("getEmployeeList: field={}, keyword={}, department={}", field, keyword, department);

            if (field != null) {
                switch (field) {
                    case "name" -> {
                        if (department != null) {
                            page = employeeRepository.findByNameContainingAndDepartmentNameContainingAndStatus(keyword, department, status, pageable);
                        } else {
                            page = employeeRepository.findByNameContainingAndStatus(keyword, status, pageable);
                        }
                    }
                    case "position" -> {
                        List<String> positions = Arrays.stream(Position.values()).map(Enum::name).collect(Collectors.toList());
                        String matchedPositionName = positions.stream()
                                .filter(roleName -> roleName.contains(keyword))
                                .findFirst().orElse(null);
                        Position position = null;
                        if (matchedPositionName != null) {
                            position = Position.valueOf(matchedPositionName);
                        }
                        if (department != null) {
                            page = employeeRepository.findByPositionAndDepartmentNameContainingAndStatus(position, department, status, pageable);
                        } else {
                            page = employeeRepository.findByPositionAndStatus(position, status, pageable);
                        }
                    }
                    case "role" -> {
                        List<String> roles = Arrays.stream(Role.values()).map(Enum::name).collect(Collectors.toList());
                        String matchedRoleName = roles.stream()
                                .filter(roleName -> roleName.contains(keyword))
                                .findFirst().orElse(null);
                        Role role = null;
                        if (matchedRoleName != null) {
                            role = Role.valueOf(matchedRoleName);
                        }
                        if (department != null) {
                            page = employeeRepository.findByRoleAndDepartmentNameContainingAndStatus(role, department,status, pageable);
                        } else {
                            page = employeeRepository.findByRoleAndStatus(role,status, pageable);
                        }
                    }
                    case "department" -> page = employeeRepository.findByDepartmentNameContainingAndStatus(keyword,status, pageable);
                    case "phone" -> {
                        if (department != null) {
                            page = employeeRepository.findByPhoneContainingAndDepartmentNameContainingAndStatus(keyword, department,status, pageable);
                        } else {
                            page = employeeRepository.findByPhoneContainingAndStatus(keyword,status, pageable);
                        }
                    }
                }
            } else if (department != null) {
                page = employeeRepository.findByDepartmentNameContainingAndStatus(department,status, pageable);
            }
            if (page == null) {
                page = employeeRepository.findAllByStatus(status, pageable);
            }
        } else {
            page = employeeRepository.findByEmployeeIdAndStatus(tokenUserInfo.getEmployeeId(), status, pageable);

        }
        return page.map(employee -> EmployeeListResDto.builder()
                .id(employee.getEmployeeId())
                .name(employee.getName())
                .phone(employee.getPhone())
                .department(employee.getDepartment().getName())
                .position(employee.getPosition().name())
                .email(employee.getEmail())
                .profileImageUri(employee.getProfileImageUri())
                .role(employee.getRole().name())
                .status(employee.getStatus())
                .build());
    }

    public EmployeeResDto getEmployee(Long id) {
        return employeeRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("조회되지 않는 사용자 입니다.")
        ).toDto();
    }

    public EmployeeResDto getEmployeeByEmail(String email) {
        return employeeRepository.findByEmail(email).orElseThrow(
                () -> new EntityNotFoundException("조회되지 않는 사용자 입니다.")
        ).toDto();
    }

    // 직원 수정
    @Transactional
    public EmployeeResDto modifyEmployeeInfo(Long id, EmployeeReqDto dto, Role role) throws JsonProcessingException {
        Employee employee = employeeRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("조회되지 않는 사용자 입니다.")
        );
        if (employee.getStatus().equals(EmployeeStatus.INACTIVE)) {
            throw new RuntimeException("퇴사한 직원 정보는 수정할 수 없습니다.");
        }
        if (role.equals(Role.ADMIN) || role.equals(Role.HR_MANAGER)) {
            employee.updateRoleAndPosition(Role.valueOf(dto.getRole()), Position.valueOf(dto.getPosition()));
            employee.updateDepartment(departmentService.getDepartmentEntity(dto.getDepartmentId()));
            insertTransferHistory(employee, dto.getDepartmentId(), dto.getPosition(), "");
        }
        employee.updateFromDto(dto);
        return employee.toDto();

    }

    // 인사이동 이력 초기화
    private void initTransferHistory(Employee employee, Long departmentId, String positionName, String memo) throws JsonProcessingException {
        List<HrTransferHistoryDto> hrTransferHistoryDtos = new ArrayList<>();
        hrTransferHistoryDtos.add(HrTransferHistoryDto.builder()
                .sequenceId(0L)
                .departmentId(departmentId)
                .positionName(positionName)
                .memo(memo)
                .build());
        HrTransferHistory hrTransferHistory = HrTransferHistory.builder()
                .employee(employee)
                .transferHistory(new ObjectMapper().writeValueAsString(hrTransferHistoryDtos))
                .build();
        hrTransferHistoryRepository.save(hrTransferHistory);
    }

    // 인사이동
    private void insertTransferHistory(Employee employee, Long departmentId, String positionName, String memo) throws JsonProcessingException {
        HrTransferHistory hrTransferHistory = hrTransferHistoryRepository.findByEmployee(employee);
        if (hrTransferHistory == null) {
            initTransferHistory(employee, departmentId, positionName, memo);
            return;
        }
        String json = hrTransferHistory.getTransferHistory();
        List<HrTransferHistoryDto> hrTransferHistoryDtos = new ObjectMapper()
                .readValue(json, new TypeReference<List<HrTransferHistoryDto>>() {
                });
        if (!hrTransferHistoryDtos.get(hrTransferHistoryDtos.size() - 1).getDepartmentId().equals(departmentId)
            || !hrTransferHistoryDtos.get(hrTransferHistoryDtos.size() - 1).getPositionName().equals(positionName)) {
            hrTransferHistoryDtos.add(HrTransferHistoryDto.builder()
                    .sequenceId((long) hrTransferHistoryDtos.size())
                    .departmentId(departmentId)
                    .positionName(positionName)
                    .memo(memo)
                    .build());
            hrTransferHistory.updateTransferHistory(new ObjectMapper().writeValueAsString(hrTransferHistoryDtos));
            hrTransferHistoryRepository.save(hrTransferHistory);
        }

    }

    public String getEmployeeName(Long id) {
        return employeeRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("조회되지 않는 사용자 입니다.")).getName();
    }

    public String getDepartmentNameOfEmployee(Long id) {
        return employeeRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("조회되지 않는 사용자 입니다.")).getDepartment().getName();
    }

    // 직원삭제
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findByEmployeeId(id).orElseThrow(
                () -> new EntityNotFoundException("조회되지 않는 사용자 입니다.")
        );
        employee.updateStatus(EmployeeStatus.INACTIVE);
        employeeRepository.save(employee);
    }

    public Map<Long, String> getEmployeeNamesByEmployeeIds(List<Long> employeeIds) {
        List<Employee> employees = employeeRepository.findAllById(employeeIds);

        Map<Long, String> map = employees.stream().collect(
                Collectors.toMap(Employee::getEmployeeId, Employee::getName)
        );

        return map;
    }

    public HrTransferHistoryResDto getTransferHistory(Long employeeId, TokenUserInfo tokenUserInfo) throws JsonProcessingException {
        if (!tokenUserInfo.getEmployeeId().equals(employeeId)) {
            if (!tokenUserInfo.getRole().equals(Role.ADMIN) && !tokenUserInfo.getRole().equals(Role.HR_MANAGER)) {
                throw new RuntimeException("권한이 없습니다!");
            }
        }

        HrTransferHistory hrTransferHistory = hrTransferHistoryRepository.findByEmployee(findById(employeeId));
        if (hrTransferHistory == null) {
            throw new EntityNotFoundException("해당 직원의 인사이동 이력이 존재하지 않습니다.");
        }

        String json = hrTransferHistory.getTransferHistory();
        List<HrTransferHistoryDto> hrTransferHistoryDtos = new ObjectMapper().readValue(json, new TypeReference<List<HrTransferHistoryDto>>() {
        });

        return HrTransferHistoryResDto.builder()
                .tranferHistoryId(hrTransferHistory.getId())
                .employeeId(employeeId)
                .hrTransferHistories(hrTransferHistoryDtos)
                .build();
    }

    /**
     * 이메일로 직원을 찾아 직원의 ID를 반환합니다.
     */
    public Long findIdByEmail(String email) {
        // EmployeeRepository에 findByEmail 메소드가 정의되어 있어야 함
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "해당 이메일의 사용자를 찾을 수 없습니다: " + email
                ));
        return employee.getEmployeeId();
    }


    // ✅ ID Set으로 여러 명 조회
    public List<EmployeeResDto> findByIds(Set<Long> employeeIds) {
        List<Employee> employees = employeeRepository.findAllById(employeeIds);
        return employees.stream()
                .map(EmployeeResDto::fromEntity)
                .toList();
    }

    // 퇴사자 조회
    public List<EmployeeResDto> findActiveEmployees() {
        List<Employee> activeEmployees = employeeRepository.findByStatus(EmployeeStatus.ACTIVE);
        return activeEmployees.stream()
                .map(Employee::toDto) // 엔티티의 toDto() 메소드 활용
                .collect(Collectors.toList());
    }


}





