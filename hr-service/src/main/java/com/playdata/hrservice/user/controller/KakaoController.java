package com.playdata.hrservice.user.controller;

import com.playdata.hrservice.user.service.KaKaoLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user-service/oauth/kakao")
public class KakaoController {

    @Autowired
    private KaKaoLoginService loginService;


    @GetMapping("")
   public ResponseEntity<Map<String, Object>> doLogin(@RequestParam("code") String code) {

        Map<String, Object> user = loginService.login(code);

        return new ResponseEntity<>(user, HttpStatus.OK);


//        try {
//            String encodeNickName = URLEncoder.encode(nickName, StandardCharsets.UTF_8.toString());
//            String encodeEmail = URLEncoder.encode(email, StandardCharsets.UTF_8.toString());
//            String encodeProfileImageUrl = URLEncoder.encode(profileImageUrl, StandardCharsets.UTF_8.toString());
//
//            String redirectUrl = "http://localhost:8000/user-service/users/signup?ninckname="+encodeNickName +
//                    "&email=" + encodeEmail + "&profileImageUrl=" + encodeProfileImageUrl;
//
//            return "redirect:" + redirectUrl;
//        } catch (UnsupportedEncodingException e) {
//            throw new RuntimeException(e);
//        }



    }




}
