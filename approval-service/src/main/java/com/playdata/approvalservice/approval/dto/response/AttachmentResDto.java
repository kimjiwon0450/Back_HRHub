package com.playdata.approvalservice.approval.dto.response;


import lombok.*;

import java.time.LocalDateTime;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachmentResDto {
    private Long attachmentId;
    private Long reportId;
    private String fileName;
    private String url;
    private LocalDateTime uploadedAt;
}