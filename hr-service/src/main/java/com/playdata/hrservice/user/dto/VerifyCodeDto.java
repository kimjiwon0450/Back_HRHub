package com.playdata.hrservice.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyCodeDto {
    @NotBlank private String email;
    @NotBlank private String code;
}
