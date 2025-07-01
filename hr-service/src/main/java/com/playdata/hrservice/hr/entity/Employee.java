package com.playdata.hrservice.hr.entity;

import com.playdata.hrservice.common.auth.Role;
import com.playdata.hrservice.common.entity.BaseTimeEntity;
import com.playdata.hrservice.hr.dto.EmployeeReqDto;
import com.playdata.hrservice.hr.dto.EmployeeResDto;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

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
    @Setter
    private String password;
    private String phone;
    private String address;
    private String position;

    @ManyToOne
    @JoinColumn(name = "departmentId")
    private Department department;
    private int salary;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime hireDate;
    private LocalDateTime retireDate;

    private EmployeeStatus status;
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
                .position(position)
                .departmentId(department.getId())
                .salary(salary)
                .hireDate(hireDate)
                .retireDate(retireDate)
                .status(status.name())
                .role(role.name())
                .profileImageUri(profileImageUri)
                .memo(memo)
                .build();
    }
}
