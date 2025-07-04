package com.playdata.approvalservice.common.dto;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Date;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeResDto {
    private Long employeeId;
    private String name;
    private String email;
    private String phone;
    private String address;
    private Long departmentId;
    private Date birthday;
    private LocalDateTime hireDate;
    private LocalDateTime retireDate;
    private String status;
    private String role;
    private String profileImageUri;
    private String memo;
}

