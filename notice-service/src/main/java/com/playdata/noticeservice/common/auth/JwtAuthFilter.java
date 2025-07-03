package com.playdata.noticeservice.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secretKey;


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = parseBearerToken(request); // Authorization 헤더에서 토큰 추출

            if (token != null) {
                Claims claims = Jwts.parser()
                        .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8)) // 키 설정
                        .parseClaimsJws(token)
                        .getBody();

                // 게이트웨이가 토큰 내에 클레임을 헤더에 담아서 보내준다.
                String userEmail = claims.get("email", String.class);
                String userRole = claims.get("role", String.class);
                Long employeeId = claims.get("id", Long.class);
                log.info("userEmail:{} userRole:{} employeeId: {}", userEmail, userRole, employeeId);

                if (userEmail != null && userRole != null && employeeId != null) {
                    List<SimpleGrantedAuthority> authorityList = new ArrayList<>();
                    authorityList.add(new SimpleGrantedAuthority("ROLE_" + userRole));

                    TokenUserInfo userInfo = new TokenUserInfo(employeeId, userEmail, Role.valueOf(userRole));

                    Authentication auth = new UsernamePasswordAuthenticationToken(
                            userInfo,
                            null,
                            authorityList  // 인가 정보 (권한)
                    );

                    SecurityContextHolder.getContext().setAuthentication(auth);

                }
            }
        } catch (Exception e) {
            log.error("JWT 처리 중 에러 발생: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }


    private String parseBearerToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}
