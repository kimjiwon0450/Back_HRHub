package com.playdata.noticeservice.common.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playdata.noticeservice.common.dto.CommonResDto;
import com.playdata.noticeservice.common.dto.DepResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class DepartmentClient {

    private final RestTemplate restTemplate;
    private final Environment env;
    private final ObjectMapper objectMapper;

    public DepResponse getDepInfo(Long departmentId) {
        // gateway 주소를 통해 요청 (Eureka 통해 포워딩됨)
        String gatewayUrl = env.getProperty("gateway.url", "http://localhost:8000"); // application.properties에서 관리 가능
        String url = gatewayUrl + "/hr/departments/" + departmentId;
        System.out.println("요청 url = " + url);
        System.out.println("부서 번호 : " + departmentId);

        // -----------------------------------------------------------
//        DepResponse response = restTemplate.getForObject(url, DepResponse.class);
//        System.out.println("부서 정보 응답: " + response); // 또는 log.info
//
//        return response;

        // -----------------------------------------------------------------------
        ResponseEntity<CommonResDto> exchange = restTemplate.exchange(url, HttpMethod.GET, null, CommonResDto.class);
        CommonResDto body = exchange.getBody();
        log.info("body: {}", body);
        LinkedHashMap<String, Object> resultMap = (LinkedHashMap<String, Object>) body.getResult();
        DepResponse response = objectMapper.convertValue(resultMap, DepResponse.class);


        System.out.println("부서 정보 응답: " + response); // 또는 log.info

        return response;
        // --------------------------------------------------------------------------------
    }
}
