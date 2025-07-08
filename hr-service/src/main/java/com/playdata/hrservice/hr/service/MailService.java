package com.playdata.hrservice.hr.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender mailSender;

    public String sendVerificationMail(String toEmail) {
        // 인증번호 생성
        String verificationCode = generateCode();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("HRHub 메일 인증 안내");
        message.setText("인증 링크는 [ http://localhost:5173?email=" + toEmail + " ] 입니다." );

        mailSender.send(message);

        return verificationCode;
    }

    private String generateCode() {
        Random random = new Random();
        int code = random.nextInt(888888) + 111111;
        return String.valueOf(code);
    }
}
