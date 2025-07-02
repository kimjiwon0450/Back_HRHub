package com.playdata.noticeservice.notice.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class CommonResDto {
    private HttpStatus status;
    private String message;
    private Object data; // 실제 직원 정보가 여기에 들어 있음
}
