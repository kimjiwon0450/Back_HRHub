package com.playdata.hrservice.user.entity;

import com.playdata.hrservice.common.auth.Role;
import com.playdata.hrservice.common.entity.BaseTimeEntity;
import com.playdata.hrservice.user.dto.UserResDto;
import jakarta.persistence.*;
import lombok.*;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity

@Table(name = "tbl_user")
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(unique = true, length = 50, nullable = false)
    @Setter
    private String nickName;

    @Column(length = 255, nullable = true)
    @Setter
    private String password;

    @Column(unique = true, length = 100, nullable = false)
    private String email;

    @Column(nullable = false)
    @Builder.Default
    @Setter
    private int point = 0;

    @Column(length = 255, nullable = true)
    @Setter
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    @Setter
    @Builder.Default
    private Boolean isBlack = false; // True이면 활동 불가임.

    @Column(nullable = true)
    @Setter
    private Long kakaoId; //카카오 회원번호 저장

    // DTO에 Entity 변환 메서드가 있는 거처럼
    // Entity에도 응답용 DTO 변환 메서드를 세팅해서 언제든 변환이 자유롭도록 작성.
    public UserResDto toDto() {
        return UserResDto.builder()
                .id(id)
                .nickName(nickName)
                .email(email)
                .point(point)
                .profileImage(profileImage)
                .role(role)
                .isBlack(isBlack)
                .KakaoId(kakaoId)
                .build();
    }
}
