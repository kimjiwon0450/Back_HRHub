package com.playdata.hrservice.hr.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class WeatherService {

    @Value("${weather.api.base-url}")
    private String baseUrl;

    @Value("${weather.api.key}")
    private String apiKey;

    public String getVilageFcst(Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/getVilageFcst");
        // 항상 API 키를 파라미터로 붙임
        builder.queryParam("serviceKey", apiKey);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.queryParam(entry.getKey(), entry.getValue());
        }

        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(builder.toUriString(), String.class);
    }
}
