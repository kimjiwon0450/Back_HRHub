package com.playdata.hrservice.hr.dto;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeePasswordDto {
    private String email;
    private String password;
    private String verificationCode;
}
