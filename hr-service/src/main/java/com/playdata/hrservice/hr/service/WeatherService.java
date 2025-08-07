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
    private String apiKey;

    public String getVilageFcst(Map<String, String> params) throws UnsupportedEncodingException {
        String encodedApiKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8.toString());
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/getVilageFcst")
                .queryParam("serviceKey", encodedApiKey);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.queryParam(entry.getKey(), entry.getValue());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class
        );
        System.out.println("apiKey: " + apiKey);
        // 2. 결과 URL 로그 꼭 찍어서 확인
        System.out.println("기상청 호출 URL: " + builder.toUriString());

        return response.getBody();
    }
}
