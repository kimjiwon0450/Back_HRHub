package com.playdata.global.exception;

import com.playdata.global.enums.AlertMessage;
import lombok.Getter;

@Getter
public class CustomAlertException extends RuntimeException {
    private final AlertMessage alertMessage;
    private final String type; // "error", "warning", "info" 등

    public CustomAlertException(AlertMessage alertMessage, String type) {
        super(alertMessage.getMessage());
        this.alertMessage = alertMessage;
        this.type = type;
    }
}