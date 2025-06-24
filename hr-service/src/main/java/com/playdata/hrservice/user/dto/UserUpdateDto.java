package com.playdata.hrservice.user.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateDto {
    private Long id;
    @Size(min = 1, message = "닉네임을 입력해주세요.")
    private String nickName;

    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    private String password;


}
