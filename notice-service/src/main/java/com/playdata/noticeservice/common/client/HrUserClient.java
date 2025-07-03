package com.playdata.noticeservice.common.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playdata.noticeservice.common.dto.CommonErrorDto;
import com.playdata.noticeservice.common.dto.CommonResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.playdata.noticeservice.common.dto.HrUserResponse;

import java.util.LinkedHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class HrUserClient {

    private final RestTemplate restTemplate;
    private final Environment env;
    private final ObjectMapper objectMapper;

    public HrUserResponse getUserInfo(Long userId) {
        // gateway 주소를 통해 요청 (Eureka 통해 포워딩됨)
        String gatewayUrl = env.getProperty("gateway.url", "http://localhost:8000"); // application.properties에서 관리 가능
        String url = gatewayUrl + "/hr-service/employees/" + userId;

//        HrUserResponse response = restTemplate.getForObject(url, HrUserResponse.class);
        ResponseEntity<CommonResDto> exchange = restTemplate.exchange(url, HttpMethod.GET, null, CommonResDto.class);
        CommonResDto body = exchange.getBody();
        log.info("body: {}", body);
        LinkedHashMap<String, Object> resultMap = (LinkedHashMap<String, Object>) body.getResult();
        HrUserResponse response = objectMapper.convertValue(resultMap, HrUserResponse.class);


        System.out.println("사용자 정보 응답: " + response); // 또는 log.info

        return response;
    }
}
