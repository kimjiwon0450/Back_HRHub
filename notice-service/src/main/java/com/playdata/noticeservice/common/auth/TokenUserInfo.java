package com.playdata.noticeservice.common.auth;

import lombok.*;

@Getter @Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenUserInfo {
    private Long employeeId;
    private String email;
    private Role role;
}
