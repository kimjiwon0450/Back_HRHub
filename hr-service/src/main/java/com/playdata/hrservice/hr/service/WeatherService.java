package com.playdata.hrservice.hr.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class WeatherService {

    @Value("${weather.api.base-url}")
    private String baseUrl;

    @Value("${weather.api.key}")
    private String apiKey;   // ← yml에는 반드시 디코딩된(+ / =) 원본 값!

    public String getVilageFcst(Map<String, String> params) throws UnsupportedEncodingException {

        // 1) 키는 yml에 “디코딩된 원본” 그대로 두고
// 2) 여기서 한 번만 인코딩
        String encodedApiKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/getVilageFcst")
                .queryParam("serviceKey", encodedApiKey);
        params.forEach(builder::queryParam);

// ★ build(true) 로 “이미 인코딩됨” 선언하고 URI 객체로
        URI uri = builder.build(true).toUri();   // ← 중요!

        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/json");

        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response =
                rt.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        return response.getBody();
    }
}