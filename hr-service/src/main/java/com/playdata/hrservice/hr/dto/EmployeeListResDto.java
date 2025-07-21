package com.playdata.hrservice.hr.dto;

import com.playdata.hrservice.common.auth.Role;
import com.playdata.hrservice.hr.entity.EmployeeStatus;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeListResDto {
    private Long id;
    private String name;
    private String department;
    private String role;
    private String position;
    private String phone;
    private String email; // 조직도를 위해 추가
    private String profileImageUri; // 조직도를 위해 추가

    private EmployeeStatus status; //

}
