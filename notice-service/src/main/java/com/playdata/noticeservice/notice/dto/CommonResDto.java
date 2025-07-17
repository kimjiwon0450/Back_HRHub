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

    // 성공 응답 생성
    public static CommonResDto success(String message, Object data) {
        CommonResDto response = new CommonResDto();
        response.setStatus(HttpStatus.OK);
        response.setMessage(message);
        response.setData(data);
        return response;
    }

    // 실패 응답 생성
    public static CommonResDto fail(String message, HttpStatus status) {
        CommonResDto response = new CommonResDto();
        response.setStatus(status);
        response.setMessage(message);
        response.setData(null);
        return response;
    }

}
