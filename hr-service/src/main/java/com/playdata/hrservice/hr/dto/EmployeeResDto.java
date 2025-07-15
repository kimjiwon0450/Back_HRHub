package com.playdata.hrservice.hr.dto;

import com.playdata.hrservice.common.auth.Role;
import com.playdata.hrservice.hr.entity.Department;
import com.playdata.hrservice.hr.entity.Employee;
import com.playdata.hrservice.hr.entity.EmployeeStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeResDto {
    private Long employeeId;

    private String name;
    private String email;
    private String phone;
    private String address;
    private Date birthday;

    private Long departmentId;
    private LocalDate hireDate;
    private LocalDate retireDate;

    private String status;
    private String role;
    private String position;
    private String profileImageUri;
    private String memo;
    private Boolean isNewEmployee;

    // notice-service에서 글 작성자들의 정보를 한번에 가져오기 위해 추가함(2025-07-15)
    public static EmployeeResDto fromEntity(Employee e) {
        return EmployeeResDto.builder()
                .employeeId(e.getEmployeeId())
                .name(e.getName())
//                .departmentId(e.)
                .build();
    }
}
