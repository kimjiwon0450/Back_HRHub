package com.playdata.hrservice.user.service;


import com.playdata.hrservice.user.dto.*;
import com.playdata.hrservice.common.config.AwsS3Config;
import com.playdata.hrservice.user.entity.User;
import com.playdata.hrservice.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final AwsS3Config awsS3Config;
    private final MailSenderService mailSenderService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CODE_CHARS =
            "0123456789" + "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom random = new SecureRandom();

    // Redis key 상수
    private static final String VERIFICATION_CODE_KEY = "email_verify:code:";
    private static final String VERIFICATION_ATTEMPT_KEY = "email_verify:attempt:";
    private static final String VERIFICATION_BLOCK_KEY = "email_verify:block:";
    private static final String RESET_KEY_PREFIX = "pw-reset:";
    private static final Duration RESET_CODE_TTL = Duration.ofMinutes(5);


    @Transactional
    public UserResDto createUser(UserSaveReqDto dto) {
        // 1. 이메일 중복 확인 (신규 가입에만 해당)
        // 이메일이 이미 존재하면 예외 발생 (카카오 연동은 linkKakaoAccount에서 처리하므로 여기서 걸리면 안 됨)
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 이메일 입니다!");
        }

        // 2. 닉네임 중복 확인
        if (userRepository.findByNickName(dto.getNickName()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 닉네임 입니다!");
        }

        // 3. 비밀번호 처리 (카카오 로그인 사용자 vs 일반 사용자)
        String finalEncodedPassword;
        if (dto.getKakaoId() != null) {
            // 카카오 로그인 사용자: 비밀번호를 따로 입력받지 않으므로, DB의 NOT NULL 제약조건을 만족시키기 위해 임의의 비밀번호 생성
            finalEncodedPassword = encoder.encode(UUID.randomUUID().toString());
        } else {
            // 일반 회원가입 사용자: 비밀번호가 필수로 존재해야 함
            if (!StringUtils.hasText(dto.getPassword())) { // 비밀번호가 null이거나 빈 문자열인지 확인
                throw new IllegalArgumentException("비밀번호는 필수입니다.");
            }
            // 비밀번호 길이 검사 (패턴 검사도 필요하다면 여기에 추가)
            if (dto.getPassword().length() < 8) {
                throw new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다.");
            }
            // UserSaveReqDto에서 @Pattern이 제거되었으므로 여기에 패턴 검사를 추가할 수 있습니다.
            // 예시: if (!dto.getPassword().matches("^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,20}$")) {
            //     throw new IllegalArgumentException("비밀번호는 영문, 숫자, 특수문자를 포함하여 8~20자리여야 합니다.");
            // }
            finalEncodedPassword = encoder.encode(dto.getPassword());
        }

        // 4. User Entity 생성 및 저장
        User newUser = User.builder()
                .nickName(dto.getNickName())
                .email(dto.getEmail())
                .password(finalEncodedPassword) // 인코딩된 최종 비밀번호
                .kakaoId(dto.getKakaoId()) // 카카오 ID (null 또는 Long 값)
                .role(dto.getRole()) // Role enum 그대로 사용
                .isBlack(false) // 기본값
                .point(0) // 기본값
                .build();

        User savedUser = userRepository.save(newUser);
        return savedUser.toDto();
    }




    public UserResDto login(UserLoginReqDto dto) {
        User user = userRepository.findByEmail(dto.getEmail()).orElseThrow(
                () -> new EntityNotFoundException("User not found!")
        );

        if(Boolean.TRUE.equals(user.getIsBlack())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"정지된 계정입니다.");
        }

        if (!encoder.matches(dto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        return user.toDto();
    }

    public User findById(String id) {
        return userRepository.findById(Long.parseLong(id)).orElseThrow(
                () -> new EntityNotFoundException("User not found!")
        );
    }


    // 유저 ID로 포인트 조회
    @Transactional(readOnly = true)
    public int getUserPoint(Long userId) {
        log.info("[UserService] getUserPoint() 호출됨 - userId: {}", userId);

        // 유저 ID로 DB에서 유저 찾기
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("해당 ID의 유저 없음: {} ", userId);
                    return new IllegalArgumentException("유저 없음");
                });

        log.info("찾은 유저 포인트: {}", user.getPoint());
        // 유저 엔티티에서 포인트 리턴
        return user.getPoint();
    }


    public String uploadProfile(UserRequestDto userRequestDto) throws Exception {
        User user = userRepository.findById(userRequestDto.getId()).orElseThrow(
                () -> new EntityNotFoundException("User not found!")
        );


        // 1) 이전 프로필이 기본 url이 아니고, null도 아니라면 삭제
        String oldUrl = user.getProfileImage();
        if (oldUrl != null && !oldUrl.isBlank()) {
            awsS3Config.deleteFromS3Bucket(oldUrl);

        }

        //2) 새 파일 업로드
        MultipartFile profileImage = userRequestDto.getProfileImage();
        String uniqueFileName = UUID.randomUUID() + "_" + profileImage.getOriginalFilename();
        String imageUrl = awsS3Config.uploadToS3Bucket(profileImage.getBytes(), uniqueFileName);


        user.setProfileImage(imageUrl);
        userRepository.save(user);
        return imageUrl;
    }

    public UserResDto findByEmail(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return null;
        }
        return user.toDto();
    }

    public UserResDto addPoint(Long userId, Integer point) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new EntityNotFoundException("User not found!")
        );
        if (user == null) {
            return null;
        }

        user.setPoint(user.getPoint()+point);
        userRepository.save(user);
        return user.toDto();
    }

    public UserUpdateDto updateInfoUser(UserUpdateDto dto) {
        User user = userRepository.findById(dto.getId()).orElseThrow(
                () -> new EntityNotFoundException("User not found!")
        );

        String newNick = dto.getNickName();
        if (dto.getNickName() != null && !dto.getNickName().isBlank()) {
            if(!newNick.equals(user.getNickName())&& userRepository.existsByNickName(newNick)) {
                throw new IllegalArgumentException("이미 사용중인 닉네임 입니다.");
            }
            user.setNickName(newNick);
        }

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            String encodedPassword = encoder.encode(dto.getPassword());
            user.setPassword(encodedPassword);
        }
        userRepository.save(user);

        return UserUpdateDto.builder()
                .id(user.getId())
                .nickName(user.getNickName())
                .build();
    }

    public UserResDto getUserProfile(String userId) {

        User user = userRepository.findById(Long.parseLong(userId)).orElseThrow(
                () -> new EntityNotFoundException("User not found!")
        );

        return UserResDto.builder()
                .profileImage(user.getProfileImage())
                .build();
    }

    public String mailCheck(String email) {
        // 차단 상태 확인
        if (isBlocked("email")) {
            throw new IllegalArgumentException("Blocking");
        }

        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 이메일 입니다.");
        }

        String authNum;

        try {
            // 이메일 전송만을 담당하는 객체를 이용해서 이메일 로직 작성.
            authNum = mailSenderService.joinMail(email);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 전송 과정 중 문제 발생!");
        }

        // 인증 코드를 Redis 저장
        String key = VERIFICATION_CODE_KEY + email;
        redisTemplate.opsForValue().set(key, authNum, Duration.ofMinutes(1));

        return authNum;

    }

    // 인증 코드 검증 로직
    public Map<String, String> verifyEmail(Map<String, String> map) {
        // 차단 상태 확인
        if (isBlocked(map.get("email"))) {
            throw new IllegalArgumentException("blocking");
        }

        // 레디스에 저장된 인증 코드 조회
        String key = VERIFICATION_CODE_KEY + map.get("email");
        Object foundCode = redisTemplate.opsForValue().get(key);
        if (foundCode == null) { // 조회결과가 null? -> 만료됨!
            throw new IllegalArgumentException("authCode expired!");
        }

        // 인증 시도 횟수 증가
        int attemptCount = incrementAttemptCount(map.get("email"));

        // 조회한 코드와 사용자가 입력한 인증번호 검증
        if (!foundCode.toString().equals(map.get("code"))) {
            // 최대 시도 횟수 초과시 차단
            if (attemptCount >= 3) {
                blockUser(map.get("email"));
                throw new IllegalArgumentException("email blocked!");
            }
            int remainingAttempts = 3 - attemptCount;
            throw new IllegalArgumentException(String.format("authCode wrong!, %d", remainingAttempts));
        }

        log.info("이메일 인증 성공!, email: {}", map.get("email"));
        redisTemplate.delete(key); // 레디스에서 인증번호 삭제
        return map;
    }

    private boolean isBlocked(String email) {
        String key = VERIFICATION_BLOCK_KEY + email;
        return redisTemplate.hasKey(key);
    }

    private void blockUser(String email) {

        String key = VERIFICATION_BLOCK_KEY + email;
        redisTemplate.opsForValue().set(key, "blocked", Duration.ofMinutes(30));
    }

    private int incrementAttemptCount(String email) {
        String key = VERIFICATION_ATTEMPT_KEY + email;
        Object obj = redisTemplate.opsForValue().get(key);

        int count = (obj != null) ? Integer.parseInt(obj.toString()) + 1 : 1;
        redisTemplate.opsForValue().set(key, count, Duration.ofMinutes(1));

        return count;
    }


    public String addBlackUser(String email, Boolean black) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if(userOptional.isEmpty()) {
            throw new EntityNotFoundException("해당 회원이 없습니다.");
        }

        User user = userOptional.get();

        if (user.getIsBlack()) {
            user.setIsBlack(false);
        }
        else{
            user.setIsBlack(true);
        }
        userRepository.save(user);
        return user.getNickName();
    }

    public List<UserResDto> findAll() {
        List<User> users = userRepository.findAll();

        return users.stream()
                .map(user->user.toDto())
                .collect(Collectors.toList());

    }
    public Boolean changeStatus(String userEmail) {
        Optional<User> byEmail = Optional.ofNullable(userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없음")));
        User user = byEmail.get();
        if(user.getIsBlack()) {
            user.setIsBlack(false);
        }else{
            user.setIsBlack(true);
        }
        userRepository.save(user);

        return user.getIsBlack();

    }

    public void sendPasswordResetCode(@NotBlank(message = "이메일을 입력해 주세요.") String email) {
        // 1) 회원 존재 확인
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("해당 이메일의 사용자를 찾을 수 없습니다"));

        // 2) 인증 코드 생성
        String code = makeAlphanumericCode(9);

        // 3) Rediss에 저장 (키: pw-reset:{email})
        String redisKey = RESET_KEY_PREFIX + email;
        redisTemplate.opsForValue()
                .set(redisKey, code, RESET_CODE_TTL);

        // 4) 비밀번호 재설정 메일 발송
        // MailSenderService에 별도 메서드를 만들어 두는 걸 추천합니다.
        try {
            mailSenderService.sendPasswordResetMail(email, user.getNickName(), code);
        } catch (MessagingException ex) {
            log.error("비밀번호 재설정 메일 전송 실패", ex);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "인증 메일 전송에 실패했습니다. 잠시 후 다시 시도해 주세요."
            );
        }


    }


    private String makeAlphanumericCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = random.nextInt(CODE_CHARS.length());
            sb.append(CODE_CHARS.charAt(idx));
        }
        String code = sb.toString();
        log.info("생성된 비밀번호 재설정 코드:{}", code);
        return code;
    }

    public void verifyResetCode( @NotBlank String email, @NotBlank String code) {

        // 1) 사용자 재확인 (옵션)
        userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 유저를 찾을 수 없습니다."));


        // 2) Redis에서 코드 조회
        String key = RESET_KEY_PREFIX + email;
        Object savedCode = redisTemplate.opsForValue().get(key);
        if (savedCode == null) {
            throw new IllegalArgumentException("인증 코드가 만료되었습니다. 다시 요청해 주세요.");
        }

        if (!savedCode.equals(code)) {
            throw new IllegalArgumentException("인증 코드가 일치하지 않습니다.");
        }
        // 3) 검증 성공 시에는 코드 삭제하거나 TTL을 짧게 조정해도 좋습니다.
        // redisTemplate.delete(key);
    }


    public void resetPassword( @NotBlank String email, @NotBlank String code, @NotBlank String newPassword) {
        verifyResetCode(email, code);

        // 2) 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 유저를 찾을 수 없습니다."));

        // 3) 비밀번호 해시 후 저장
        String encoded = encoder.encode(newPassword);
        user.setPassword(encoded);
        userRepository.save(user);

        // 4) 사용한 코드 삭제
        redisTemplate.delete(RESET_KEY_PREFIX + email);
    }
}





