package com.playdata.noticeservice.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class HrUserResponse {
    private Long employeeId;
    private String name;
    private String email;
    private Long departmentId;
    private String departmentName;
}
