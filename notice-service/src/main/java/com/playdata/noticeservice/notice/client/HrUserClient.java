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
        // 예: hr-service의 사용자 정보 API URL (application.properties나 env에서 관리)
        String hrServiceUrl = env.getProperty("hr.service.url", "http://localhost:8081/hr-service");
        String url = hrServiceUrl + "/users/" + userId;

        // 실제 호출하여 HrUserResponse 반환
        return restTemplate.getForObject(url, HrUserResponse.class);
    }

}
