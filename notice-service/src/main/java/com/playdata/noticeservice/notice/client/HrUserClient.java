package com.playdata.noticeservice.notice.client;

import com.playdata.noticeservice.notice.dto.HrUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class HrUserClient {

    private final RestTemplate restTemplate;
    private final Environment env;

    public HrUserResponse getUserInfo(Long userId) {
        // gateway 주소를 통해 요청 (Eureka 통해 포워딩됨)
        String gatewayUrl = env.getProperty("gateway.url", "http://localhost:8000"); // application.properties에서 관리 가능
        String url = gatewayUrl + "/hr-service/employees/" + userId;

        return restTemplate.getForObject(url, HrUserResponse.class);
    }
}
