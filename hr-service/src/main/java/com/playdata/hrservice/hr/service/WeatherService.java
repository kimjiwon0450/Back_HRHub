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

        // 1) 서비스키를 한 번만 직접 URL-인코딩
        String encodedApiKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8.toString());

        // 2) 인코딩된 값을 queryParam으로 넣음
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/getVilageFcst")
                .queryParam("serviceKey", encodedApiKey);

        params.forEach(builder::queryParam);

        /* 3) **build(true)** → “이미 인코딩된 값이니 더 인코딩하지 마!” */
        String url = builder.build(true)        // ← 중요!
                .toUriString();

        System.out.println("apiKey(raw)  : " + apiKey);
        System.out.println("최종 호출 URL : " + url);
        //  → serviceKey=ZXtHl7IjyIssn%2BuPYkh...%3D%3D  ( %2B / %2F / %3D , 한 번만 인코딩 )

        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/json");

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        return response.getBody();
    }
}