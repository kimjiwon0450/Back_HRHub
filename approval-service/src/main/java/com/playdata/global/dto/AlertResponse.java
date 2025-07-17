package com.playdata.global.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AlertResponse {
    private String message; // alert에 표시될 메시지
    private String type;    // "success", "error", "info" 등
}
