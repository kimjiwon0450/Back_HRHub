package com.playdata.hrservice.user.controller;
import com.playdata.hrservice.common.auth.JwtTokenProvider;
import com.playdata.hrservice.common.dto.CommonErrorDto;
import com.playdata.hrservice.common.dto.CommonResDto;
import com.playdata.hrservice.user.dto.UserBadgeResDto;
import com.playdata.hrservice.user.dto.UserLoginReqDto;
import com.playdata.hrservice.user.dto.UserResDto;
import com.playdata.hrservice.user.dto.UserSaveReqDto;
import com.playdata.hrservice.user.entity.User;
import com.playdata.hrservice.user.external.client.BadgeClient;
import com.playdata.hrservice.user.dto.*;
import com.playdata.hrservice.user.service.KaKaoLoginService;
import com.playdata.hrservice.user.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/user-service")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    private final BadgeClient badgeClient;
    private final KaKaoLoginService kakaoLoginService;

    private final Environment env;

    @PostMapping("/users/signup")
    public ResponseEntity<?> createUser(
            @Valid @RequestBody UserSaveReqDto dto) {
        UserResDto saved = userService.createUser(dto);
        CommonResDto resDto
                = new CommonResDto(HttpStatus.CREATED,
                "User Created", saved.getNickName());

        return new ResponseEntity<>(resDto, HttpStatus.CREATED);
    }

    @PostMapping("/user/profile")
    public ResponseEntity<?> uploadProfile(@ModelAttribute UserRequestDto dto) throws Exception {
        String newProfile = userService.uploadProfile(dto);
        CommonResDto resDto
                = new CommonResDto(HttpStatus.OK,
                "upload success", Map.of("newProfileName", newProfile));

        return new ResponseEntity<>(resDto, HttpStatus.OK);
    }

    @PutMapping("/user/update-info")
        public ResponseEntity<?> updateUser(@Valid @RequestBody UserUpdateDto dto){
        try {
            UserUpdateDto updated = userService.updateInfoUser(dto);
            CommonResDto resDto = new CommonResDto(
                    HttpStatus.OK,
                    "회원정보 수정 성공",
                    updated
            );
            return ResponseEntity.ok(resDto);

        } catch (IllegalArgumentException e) {
            // 닉네임 중복 또는 비밀번호 조건 위반
            CommonResDto resDto = new CommonResDto(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage(),
                    null
            );
            return new ResponseEntity<>(resDto, HttpStatus.BAD_REQUEST);
        } catch (EntityNotFoundException e) {
            CommonResDto resDto = new CommonResDto(
                    HttpStatus.NOT_FOUND,
                    e.getMessage(),
                    null
            );
            return new ResponseEntity<>(resDto, HttpStatus.NOT_FOUND);
        }
    }


    @PostMapping("/user/login")
    public ResponseEntity<?> login(@RequestBody UserLoginReqDto dto) {
        UserResDto user = userService.login(dto);

        String token
                = jwtTokenProvider.createToken(user.getEmail(), user.getRole().toString());
        String refreshToken
                = jwtTokenProvider.createRefreshToken(user.getEmail(), user.getRole().toString());

        redisTemplate.opsForValue().set("user:refresh:" + user.getId(), refreshToken, 2, TimeUnit.MINUTES);


        // 여기서 FeignClient 호출
        UserBadgeResDto badge = null;
        try {
            badge = badgeClient.getUserBadge(user.getId()); // point-service 에 요청
            log.info("badgeClient 결과: {}", badge);
        } catch (Exception e) {
            log.warn("배지 없음 또는 조회 실패: {}", e.getMessage());
        }




        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("token", token);
        loginInfo.put("id", user.getId());
        loginInfo.put("nickName", user.getNickName());
        loginInfo.put("role", user.getRole().toString());
        loginInfo.put("badge", badge);
        loginInfo.put("profileImage", user.getProfileImage());



        CommonResDto resDto
                = new CommonResDto(HttpStatus.OK,
                "Login Success", loginInfo);
        return new ResponseEntity<>(resDto, HttpStatus.OK);
    }

    // 유효한 이메일인지 검증 요청
    @PostMapping("/email-valid")
    public ResponseEntity<?> emailValid(@RequestBody Map<String, String> map) {
        String email = map.get("email");
        log.info("이메일 인증 요청! email: {}", email);
        try {
            String authNum = userService.mailCheck(email);
            // 성공: 200 + 인증번호
            return ResponseEntity.ok(
                    new CommonResDto(
                        HttpStatus.OK,
                        "인증 코드 발송 성공",
                        authNum
                    )
            );
        } catch (IllegalArgumentException ex) {
            // 중복 이메일 또는 차단 상태
            return ResponseEntity
                    .badRequest()
                    .body(new CommonResDto(
                        HttpStatus.BAD_REQUEST,
                        ex.getMessage(),
                        null
                    ));
        } catch (RuntimeException ex) {
            // 메일 전송 실패 등
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResDto(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "이메일 전송 과정 중 문제 발생!",
                        null
                    ));
        }

    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> map) {
        log.info("인증 코드 검증! map: {}", map);
        Map<String, String> result
                = userService.verifyEmail(map);

        return ResponseEntity.ok().body("Success");
    }

    @PostMapping("/find-password")
    public ResponseEntity<Void> sendVerificationCode(@Valid @RequestBody FindPwDto dto) {
        userService.sendPasswordResetCode(dto.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-code")
    public ResponseEntity<CommonResDto> verifyCode(@Valid @RequestBody VerifyCodeDto dto) {

        try {
            userService.verifyResetCode( dto.getEmail(), dto.getCode());
            return ResponseEntity.ok(
                    new CommonResDto(
                        HttpStatus.OK,
                        "인증 코드가 일치합니다.",
                        null
                    )
            );
        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                    .badRequest()
                    .body(new CommonResDto(
                            HttpStatus.BAD_REQUEST,
                            ex.getMessage(),
                            null
                    ));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<CommonResDto> resetPassword(@Valid @RequestBody ResetPasswordDto dto) {
        try {
            userService.resetPassword(dto.getEmail(), dto.getCode(), dto.getNewPassword());
            return ResponseEntity.ok(
                    new CommonResDto(
                            HttpStatus.OK,
                            "비밀번호 재설정이 완료되었습니다.",
                            null
                    )
            );
        } catch (IllegalArgumentException ex) {

            return ResponseEntity
                    .badRequest()
                    .body(new CommonResDto(
                            HttpStatus.BAD_REQUEST,
                            ex.getMessage(),
                            null
                    ));
        } catch (Exception ex) {

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResDto(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "서버 에러가 발생했습니다. 다시 시도해주세요.",
                            null
                    ));
        }
    }



    @GetMapping("user/profileImage/{userId}")
    public ResponseEntity<?> getUserProfileImage(@PathVariable("userId") String userId) {
        try {
            UserResDto user = userService.getUserProfile(userId);
            CommonResDto resDto = new CommonResDto(
                    HttpStatus.OK,
                    "프로필 전달 완료 ",
                    user);
            return new ResponseEntity<>(resDto, HttpStatus.OK);
        } catch (Exception e) {
            log.error("유저 못찾음", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "statusCode", 500,
                            "statusMessage", "server error",
                            "error", e.getMessage()
                    ));
        }
    }

    @PostMapping("user/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> map) {
        String id = map.get("id");
        log.info("/user/refresh: POST, id: {}", id);
        // redis에 해당 id로 조회되는 내용이 있는지 확인
        Object obj = redisTemplate.opsForValue().get("user:refresh:" + id);
        log.info("obj: {}", obj);
        if (obj == null) { // refresh token이 수명이 다됨.
            return new ResponseEntity<>(new CommonErrorDto(
                    HttpStatus.UNAUTHORIZED, "EXPIRED_RT"),
                    HttpStatus.UNAUTHORIZED);
        }
        // 새로운 access token을 발급
        User user = userService.findById(id);
        String newAccessToken
                = jwtTokenProvider.createToken(user.getEmail(), user.getRole().toString());

        Map<String, Object> info = new HashMap<>();
        info.put("token", newAccessToken);
        CommonResDto resDto
                = new CommonResDto(HttpStatus.OK, "새 토큰 발급됨", info);
        return ResponseEntity.ok().body(resDto);

    }

    @GetMapping("/user/{userId}/point")
    public ResponseEntity<?> getUserPoint(@PathVariable Long userId) {
        try {
            int point = userService.getUserPoint(userId);
            return ResponseEntity.ok(point);
        } catch (Exception e) {
            log.error("유저 포인트 조회 중 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "statusCode", 500,
                            "statusMessage", "server error",
                            "error", e.getMessage()
                    ));
        }
    }


    @PostMapping("/users")
    public ResponseEntity<?> getUserForReivew(@RequestBody List<Integer> userIds) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < userIds.size(); i++) {
            users.add(userService.findById(String.valueOf(userIds.get(i))));
        }

        users.stream()
                .map(user->UserResDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .nickName(user.getNickName())
                        .profileImage(user.getProfileImage())
                        .point(user.getPoint())
                        .build()).collect(Collectors.toList());

        return ResponseEntity.ok().body(users);

    }

    @GetMapping("/users")
    public ResponseEntity<UserResDto> getUserByEmail(@RequestParam String email) {
        UserResDto userResDto = userService.findByEmail(email);

        return ResponseEntity.ok().body(userResDto);
    }

    @PutMapping("/users/point")
    public ResponseEntity<UserResDto> updatePoint(@RequestParam Long userId, @RequestParam int point ){
        UserResDto userResDto = userService.addPoint(userId, point);
        return ResponseEntity.ok().body(userResDto);
    };

    @GetMapping("/health-check")
    public String healthCheck() {
        String msg = "";
        msg += "token.exp_time:" + env.getProperty("token.expiration_time") +"\n";
        return msg;
    }

    @PostMapping("/add-black")
    public ResponseEntity<String> addBlack(@RequestBody UserBlackReqDto blackReqDto) {
        String userNickName = userService.addBlackUser(blackReqDto.getUserEmail(), blackReqDto.getIsBlack());
        return ResponseEntity.ok().body(userNickName);
    }

    @GetMapping("/user-list")
    public ResponseEntity<List<UserResDto>> getUserList() {
        List<UserResDto> allUsers = userService.findAll();

        return ResponseEntity.ok().body(allUsers);
    }

    @PatchMapping("/change-status")
    public ResponseEntity<Boolean> changeStatus(@RequestBody UserBlackReqDto statusReqDto) {
        Boolean currentStatus = userService.changeStatus(statusReqDto.getUserEmail());
        return ResponseEntity.ok().body(currentStatus);
    }

    // ⭐⭐⭐ 새로운 컨트롤러 엔드포인트: 카카오 계정 연동 ⭐⭐⭐
    @PostMapping("/user/link-kakao") // 프론트엔드에서 호출할 새로운 API 엔드포인트
    public ResponseEntity<?> linkKakaoAccount(@RequestBody UserKakaoLinkReqDto dto) {
        try {
            // 카카오 ID 업데이트 및 사용자 정보 조회
            UserResDto linkedUser = kakaoLoginService.linkKakaoAccount(dto.getEmail(), dto.getKakaoId(), dto.getNickName(), dto.getProfileImage());

            // ⭐ 연동 완료 후 바로 로그인 처리 (JWT 토큰 발급) ⭐
            String token = jwtTokenProvider.createToken(linkedUser.getEmail(), linkedUser.getRole().toString());
            // 필요한 경우 리프레시 토큰도 발급 및 Redis에 저장

            Map<String, Object> loginInfo = new HashMap<>();
            loginInfo.put("token", token);
            loginInfo.put("id", linkedUser.getId());
            loginInfo.put("nickName", linkedUser.getNickName());
            loginInfo.put("role", linkedUser.getRole().toString());
            // loginInfo.put("badge", badgeClient.getUserBadge(linkedUser.getId())); // 배지 정보 필요하면 호출
            loginInfo.put("profileImage", linkedUser.getProfileImage());

            CommonResDto resDto = new CommonResDto(
                    HttpStatus.OK,
                    "카카오 계정 연동 및 로그인 성공",
                    loginInfo
            );
            return new ResponseEntity<>(resDto, HttpStatus.OK);

        } catch (EntityNotFoundException e) {
            log.error("카카오 연동 실패: 사용자 없음 - {}", dto.getEmail(), e);
            CommonResDto resDto = new CommonResDto(
                    HttpStatus.NOT_FOUND,
                    "연동할 사용자를 찾을 수 없습니다.",
                    null
            );
            return new ResponseEntity<>(resDto, HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException e) {
            log.error("카카오 연동 실패: 유효성 문제 - {}", e.getMessage(), e);
            CommonResDto resDto = new CommonResDto(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage(),
                    null
            );
            return new ResponseEntity<>(resDto, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("카카오 연동 중 알 수 없는 오류 발생", e);
            CommonResDto resDto = new CommonResDto(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "계정 연동 중 서버 오류가 발생했습니다.",
                    null
            );
            return new ResponseEntity<>(resDto, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
