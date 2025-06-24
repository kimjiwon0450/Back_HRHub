package com.playdata.hrservice.user.dto;

import com.playdata.hrservice.common.auth.Role;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResDto {
    private Long id;
    private String email;
    private String nickName;
    private String profileImage;
    private int point;
    private Boolean isBlack;
    private Long KakaoId;
    private Role role;
}
