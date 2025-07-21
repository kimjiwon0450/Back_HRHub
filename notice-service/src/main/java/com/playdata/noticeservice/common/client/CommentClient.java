package com.playdata.noticeservice.common.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playdata.noticeservice.common.dto.CommentCountResponse;
import com.playdata.noticeservice.common.dto.CommonResDto;
import com.playdata.noticeservice.common.dto.DepResponse;
import org.springframework.http.HttpHeaders;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommentClient {

    private final RestTemplate restTemplate;
    private final Environment env;
    private final ObjectMapper objectMapper;

    public int getCommentInfo(Long noticeId) {
        // gateway 주소를 통해 요청 (Eureka 통해 포워딩됨)
        String gatewayUrl = env.getProperty("gateway.url", "http://localhost:8000"); // application.properties에서 관리 가능
        String url = gatewayUrl + "/notice/noticeboard/" + noticeId + "/comments/count";
        System.out.println("요청 url = " + url);
        System.out.println("게시글 번호 : " + noticeId);

        // -----------------------------------------------------------
//        DepResponse response = restTemplate.getForObject(url, DepResponse.class);
//        System.out.println("부서 정보 응답: " + response); // 또는 log.info
//
//        return response;

        // -----------------------------------------------------------------------
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", getCurrentAuthorizationToken()); // 👈 헤더 설정

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<CommonResDto> exchange = restTemplate.exchange(url, HttpMethod.GET, entity, CommonResDto.class);
        CommonResDto body = exchange.getBody();
        log.info("body: {}", body);
        LinkedHashMap<String, Object> resultMap = (LinkedHashMap<String, Object>) body.getResult();

        return (Integer) resultMap.get("commentCount");

//        CommentResponse response = objectMapper.convertValue(resultMap, CommentResponse.class);
//        return response;
        // --------------------------------------------------------------------------------
    }

    private String getCurrentAuthorizationToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() instanceof String) {
            return "Bearer " + authentication.getCredentials().toString();
        }
        throw new RuntimeException("Authorization token not found in security context");
    }

}
