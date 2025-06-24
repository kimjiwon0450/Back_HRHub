package com.playdata.hrservice.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KakaoUserInfoResponseDto {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KakaoAccount {

        @JsonProperty("email")
        private String email;

        @JsonProperty("profile")
        private Profile profile;

        @Getter
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Profile {

            @JsonProperty("nickname")
            private String nickname;

            @JsonProperty("profile_image_url")
            private String profileImageUrl;

        }
    }

}
