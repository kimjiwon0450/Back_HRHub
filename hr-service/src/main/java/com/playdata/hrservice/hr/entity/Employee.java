package com.playdata.hrservice.hr.entity;

import com.playdata.hrservice.common.auth.Role;
import com.playdata.hrservice.common.entity.BaseTimeEntity;
import com.playdata.hrservice.hr.dto.EmployeeReqDto;
import com.playdata.hrservice.hr.dto.EmployeeResDto;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Employee extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String email;
    private String phone;
    private String address;
    private Date birthday;


    @ManyToOne
    @JoinColumn(name = "departmentId")
    private Department department;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime hireDate;
    private LocalDateTime retireDate;

    private Boolean isNewEmployee; // 경력 또는 신입 (입사구분) - yhj

    @Enumerated(EnumType.STRING)
    private EmployeeStatus status;
    @Enumerated(EnumType.STRING)
    private Role role;
    private String profileImageUri;
    private String memo;

    public EmployeeResDto toDto() {
        return EmployeeResDto.builder()
                .employeeId(employeeId)
                .name(name)
                .email(email)
                .phone(phone)
                .address(address)
                .departmentId(department.getId())
                .birthday(birthday)
                .hireDate(hireDate)
                .retireDate(retireDate)
                .isNewEmployee(isNewEmployee)
                .status(status.name())
                .role(role.name())
                .profileImageUri(profileImageUri)
                .memo(memo)
                .build();
    }

    public void updateDepartment(Department department) {
        this.department = department;
    }

    public void updateFromDto(EmployeeReqDto dto) {
        if (dto.getName() != null) this.name = dto.getName();
        if (dto.getPhone() != null) this.phone = dto.getPhone();
        if (dto.getAddress() != null) this.address = dto.getAddress();
        if (dto.getBirthday() != null) this.birthday = dto.getBirthday();
        if (dto.getMemo() != null) this.memo = dto.getMemo();
    }

    public void  updateRole(Role role){
        this.role = role;
    }

    public void updateStatus(EmployeeStatus status){
        this.status = status;
        this.retireDate = LocalDateTime.now();
    }

    public void updateProfileImageUri(String profileImageUri){
        this.profileImageUri = profileImageUri;
    }
}
