package com.playdata.noticeservice.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HrUserResponse {
    private Long id;
    private String username;
    private String email;
    private Long departmentId;
    private String departmentName;
}
