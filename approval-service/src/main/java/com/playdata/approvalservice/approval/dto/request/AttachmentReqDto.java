package com.playdata.approvalservice.approval.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachmentReqDto {
    @NotBlank
    private String fileName;

    @NotBlank
    private String url;
}