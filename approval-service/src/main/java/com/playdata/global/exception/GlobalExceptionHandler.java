package com.playdata.global.exception;

import com.playdata.global.dto.AlertResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomAlertException.class)
    public ResponseEntity<AlertResponse> handleCustomAlertException(CustomAlertException e) {
        return ResponseEntity
                .badRequest()
                .body(new AlertResponse(e.getAlertMessage().getMessage(), e.getType()));
    }
}