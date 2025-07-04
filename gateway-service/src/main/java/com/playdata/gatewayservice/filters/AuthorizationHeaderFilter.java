package com.playdata.gatewayservice.filters;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

// 토큰에 유효성 검사를 하여 회원 권한 요청 처리
@Component
@Slf4j
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory {

    @Value("${jwt.secretKey}")
    private String secretKey;

    private final List<String> allowedPaths = List.of(
            "/hr-service/employees",
            "/hr-service/employees/login",
            "/hr-service/employees/password", "/hr-service/employees/*",
            "/hr-service/departments", "/hr-service/departments/*",
            "/badges/**",
            "/icons/**",
            "/notice-service", "/notice-service/noticeboard", "/notice-service/noticeboard/*",
            "/notice-service/noticeboard/write", "/notice-service/noticeboard/department/**",
            "/notice-service/reviews/user/*", "/hr-service/user/*",
            "/restaurant-service/restaurant/list",
            "/restaurant-service/restaurants/*",
            "user-service/add-black",
            "user-service/user-list",
            "user-service/change-status",
            "/user-service/oauth/kakao/**",
            "/user-service/find-password",
            "/user-service/verify-code",
            "/user-service/reset-password",
            "/user-service/user/link-kakao",
            "/actuator/**"
    );

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            AntPathMatcher antPathMatcher = new AntPathMatcher();
            boolean isAllowed = allowedPaths.stream().anyMatch(url -> antPathMatcher.match(url, path));
            log.info("path: {}", path);
            log.info("isAllowed: {}", isAllowed);
            if (isAllowed) {
                return chain.filter(exchange);
            }
                    // 일단 토큰을 얻어오자
            String authorizationHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
                return onError(exchange, "Authorization header is missing or invalid", HttpStatus.UNAUTHORIZED);
            }

            String token = authorizationHeader.replace("Bearer ", "");

            Claims claims = validateJwt(token);
            if (claims == null) {
                return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
            }

            log.info("jwt 토큰값 검증");
            log.info("claims : {}", claims);
            log.info("userId: {}", claims.get("employeeId"));

            ServerHttpRequest request = exchange.getRequest()
                    .mutate()
                    .header("X-User-Email", claims.getSubject())
                    .header("X-User-Role", claims.get("role", String.class))
                    .header("X-User-Id", claims.get("employeeId", String.class))
                    .build();


            // 새로 만든 요청을 exchange에 갈아끼워 보내기
            return chain.filter(exchange.mutate().request(request).build());
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String msg, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        log.error(msg);

        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        // just는 준비된 데이터를 Mono로 감싸는 메서드이다
        return response.writeWith(Mono.just(buffer));
    }

    private Claims validateJwt(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("JWT Token Validation Failed", e.getMessage());
            return null;
        }
    }
}
