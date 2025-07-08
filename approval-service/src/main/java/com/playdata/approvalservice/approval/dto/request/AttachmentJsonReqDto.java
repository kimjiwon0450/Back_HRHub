package com.playdata.approvalservice.approval.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachmentJsonReqDto {
    private String fileName;
    private String url;
}
