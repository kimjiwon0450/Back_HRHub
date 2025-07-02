package com.playdata.approvalservice.approval.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachmentReqDto {
    @NotBlank(message = "파일명을 입력해주세요.")
    private String fileName;

    @NotBlank(message = "파일 URL을 입력해주세요.")
    private String url;
}