package com.playdata.hrservice.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserBadgeResDto {
    // 클라이언트에 응답할 배지 정보 DTO
    private String badgeName;
    private String description;
    private String iconUrl;
    private String level; // 배지 레벨 문자열
}
