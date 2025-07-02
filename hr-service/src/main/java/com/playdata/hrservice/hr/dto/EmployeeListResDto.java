package com.playdata.hrservice.hr.dto;

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
    private String position;
    private String phone;
}
