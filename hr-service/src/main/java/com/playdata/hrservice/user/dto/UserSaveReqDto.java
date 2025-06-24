package com.playdata.hrservice.user.dto;

import com.playdata.hrservice.user.entity.User;
import jakarta.validation.constraints.NotEmpty;
import com.playdata.hrservice.common.auth.Role;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSaveReqDto {

    @NotEmpty(message = "이메일은 필수입니다!")
    private String email;

    @NotEmpty(message = "닉네임은 필수입니다!")
    private String nickName;

    private String profileImageUrl;

    private String password;


    @NotNull(message = "가입자 권한을 설정해주세요.(리뷰작성자, 사업자)")
    private Role role;

    private Long kakaoId;

    // dto가 자기가 가지고 있는 필드 정보를 토대로 User Entity를 생성해서 리턴하는 메서드
    public User toEntity(PasswordEncoder encoder) {
        Role roleFilter = (this.role == Role.ADMIN) ? Role.USER : this.role;

        String encodedPassword = null;
        if(this.password != null && this.password.isEmpty()){
            encodedPassword = encoder.encode(this.password);
        }

        return User.builder()
                .nickName(this.nickName)
                .email(this.email)
                .profileImage(this.profileImageUrl)
                .password(encodedPassword)
                .kakaoId(this.kakaoId)
                .role(roleFilter)
                .build();
    }
}
