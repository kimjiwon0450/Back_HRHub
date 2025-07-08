package com.playdata.noticeservice.common.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 게이트웨이가 토큰 내에 클레임을 헤더에 담아서 보내준다.
        String userEmail = request.getHeader("X-User-Email");
        String userRole = request.getHeader("X-User-Role");
        String userId = request.getHeader("X-User-Id");
        String departmentId = request.getHeader("X-Department-Id");
        log.info("userId: {}, userEmail:{}, userRole:{}, departmentId:{}", userId, userEmail, userRole, departmentId);

        if (userEmail != null && userRole != null) {

            List<SimpleGrantedAuthority> authorityList = new ArrayList<>();
            authorityList.add(new SimpleGrantedAuthority("ROLE_" + userRole));
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    new TokenUserInfo(Long.parseLong(userId), userEmail, Role.valueOf(userRole), Long.parseLong(departmentId)), // 컨트롤러 등에서 활용할 유저 정보
                    "", // 인증된 사용자의 비밀번호: 보통 null 혹은 빈 문자열로 선언.
                    authorityList // 인가 정보 (권한)
            );

            SecurityContextHolder.getContext().setAuthentication(auth);

        }
        filterChain.doFilter(request, response);
    }
}
