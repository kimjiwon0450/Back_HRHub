package com.playdata.hrservice.hr.dto;

import com.playdata.hrservice.common.auth.Role;
import com.playdata.hrservice.hr.entity.Department;
import com.playdata.hrservice.hr.entity.EmployeeStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeReqDto {
    private String name;
    private String email;
    private String password;
    private String phone;
    private String address;
    private Date birthday;
    private Long departmentId;
    private Boolean isNewEmployee;
    private LocalDateTime hireDate;
    private LocalDateTime retireDate;

    private String status;
    private String role;
    private String profileImageUri;
    private String memo;
}
