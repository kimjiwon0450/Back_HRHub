package com.playdata.hrservice.user.service;

import com.playdata.hrservice.common.auth.JwtTokenProvider;
import com.playdata.hrservice.user.dto.KakaoUserInfoResponseDto;
import com.playdata.hrservice.user.dto.UserResDto;
import com.playdata.hrservice.user.entity.User;
import com.playdata.hrservice.user.external.client.KakaoOAuthClient;
import com.playdata.hrservice.user.external.client.KakaoUserInfoClient;
import com.playdata.hrservice.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class KaKaoLoginService {

    @Autowired
    private KakaoOAuthClient kakaoOAuthClient;
    @Autowired
    private KakaoUserInfoClient kakaoUserInfoClient;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;


    @Transactional
    public UserResDto linkKakaoAccount(String email, Long kakaoId, String nickName, String profileImageUrl) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("연동할 사용자를 찾을 수 없습니다: " + email));

        // 이미 다른 카카오 ID로 연동되어 있는 경우 방지 (선택 사항)
        // 비즈니스 로직에 따라, 이미 연동된 카카오 ID가 있다면 에러를 던지거나,
        // 아니면 요청된 kakaoId로 무조건 업데이트하도록 할 수 있습니다.
        if (user.getKakaoId() != null && !user.getKakaoId().equals(kakaoId)) {
            throw new IllegalArgumentException("이 계정은 이미 다른 카카오 계정과 연동되어 있습니다.");
        }

        user.setKakaoId(kakaoId); // 카카오 ID 업데이트

        // 닉네임과 프로필 이미지도 업데이트할지 결정 (선택 사항)
        if (nickName != null && !nickName.isBlank() && !nickName.equals(user.getNickName())) {
            user.setNickName(nickName);
        }
        if (profileImageUrl != null && !profileImageUrl.isBlank() && !profileImageUrl.equals(user.getProfileImage())) {
            user.setProfileImage(profileImageUrl);
        }
        // Save는 @Transactional 어노테이션 덕분에 메서드 종료 시 자동 flush 되거나,
        // 명시적으로 userRepository.save(user); 호출하여 변경사항을 즉시 반영할 수 있습니다.
        User updatedUser = userRepository.save(user);
        return updatedUser.toDto();
    }


    @Transactional
    public Map<String, Object> login(String code) {

        // 카카오 토큰 요청
        String accessToken = kakaoOAuthClient.requestAccessToken(code);

        // 카카오 사용자 정보 요청
        KakaoUserInfoResponseDto kakaoUser = kakaoUserInfoClient.getUserInfoClient(accessToken);

        Long kakaoId = kakaoUser.getId(); // 카카오 고유 Id
        String email = kakaoUser.getKakaoAccount().getEmail();
        String nickname = kakaoUser.getKakaoAccount().getProfile().getNickname();
        String profileImageUrl = kakaoUser.getKakaoAccount().getProfile().getProfileImageUrl();

        //DB에서 kakaoId로 사용자 중복확인
        Optional<User> foundUserBykakaoId = userRepository.findBykakaoId(kakaoId);

        Map<String, Object> result = new HashMap<>(); // 프론트엔드로 보낼 결과 매핑

        if (foundUserBykakaoId.isPresent()) {
            User foundUser = foundUserBykakaoId.get();

            String token = jwtTokenProvider.createToken((foundUser.getEmail()),foundUser.getRole().toString());
            String refreshToken = jwtTokenProvider.createToken(foundUser.getEmail(),(foundUser.getRole().toString()));
//            redisTemplae.opsFor;


            // Case 1: 카카오 ID로 찾은 사용자가 있음 -> 바로 로그인 처리
            // 현재 UserLoginReqDto 및 관련 서비스와 연동 필요
            result.put("status", "LOGIN_SUCCESS");
            result.put("message", "카카오 계정으로 로그인되었습니다.");
             result.put("token", token); // 로그인 성공 시 JWT 토큰 포함
             result.put("userInfo", foundUser.toDto()); // 로그인된 사용자 정보
        } else {
            // Case 2 or 3: 카카오 ID로 찾은 사용자가 없음 -> 이메일로 다시 조회 (기존 일반 계정 여부 확인)
            Optional<User> foundUserByEmail = userRepository.findByEmail(email);

            if (foundUserByEmail.isPresent()) {
                // Case 2: 카카오 ID는 없지만 이메일이 이미 존재 -> 연동 제안
                result.put("status", "EMAIL_EXISTS_SUGGEST_LINK");
                result.put("message", "해당 이메일로 가입된 계정이 있습니다. 카카오 계정을 연동하시겠습니까?");
                result.put("nickName", nickname);
                result.put("email", email);
                result.put("profileImageUrl", profileImageUrl);
                result.put("kakaoId", kakaoId); // 연동을 위해 kakaoId도 함께 넘겨줌
            } else {
                // Case 3: 카카오 ID도, 이메일도 없음 -> 신규 회원가입 유도
                result.put("status", "NEW_USER_SIGNUP");
                result.put("message", "새로운 회원입니다. 추가 정보를 입력해 주세요.");
                result.put("nickName", nickname);
                result.put("email", email);
                result.put("profileImageUrl", profileImageUrl);
                result.put("kakaoId", kakaoId); // 신규 가입 시에도 kakaoId 저장해야 함
            }

        }
        return result;
    }



}
