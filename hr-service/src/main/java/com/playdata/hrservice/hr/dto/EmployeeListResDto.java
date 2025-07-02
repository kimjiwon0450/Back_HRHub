package com.playdata.hrservice.hr.dto;

import com.playdata.hrservice.common.auth.Role;
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
    private String role; // String?? or Role?
    private String phone;
}
