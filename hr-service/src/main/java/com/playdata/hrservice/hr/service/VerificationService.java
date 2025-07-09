package com.playdata.hrservice.hr.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class VerificationService {
    private final RedisTemplate<String, String> redisTemplate;
    private final MailService mailService;

    private static final long EXPIRE_MINUTES = 3;

    public void sendVerificationEmail(String email) {
        String code = mailService.sendVerificationMail(email);

        redisTemplate.opsForValue().set(getKey(email), code, EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    public boolean verifyCode(String email, String code) {
        String key = getKey(email);
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode != null && storedCode.equals(code)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    private String getKey(String email) {
        return "verification:" + email;
    }
}
