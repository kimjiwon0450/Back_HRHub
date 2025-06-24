package com.playdata.hrservice.user.dto;

import lombok.Data; // Lombok의 @Data (getter, setter, equals, hashCode, toString 포함)
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// UserSaveReqDto나 UserUpdateDto의 필드 중 연동에 필요한 것들만 모아서 만듭니다.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserKakaoLinkReqDto {
    private String email; // 연동할 기존 사용자를 찾기 위한 이메일 (필수)
    private Long kakaoId; // 카카오에서 받은 고유 ID (필수)
    private String nickName; // 카카오에서 받은 닉네임 (업데이트용, 선택)
    private String profileImage; // 카카오에서 받은 프로필 이미지 URL (업데이트용, 선택)
}