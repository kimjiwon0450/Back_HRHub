package com.playdata.approvalservice.approval.dto.response;


import com.playdata.approvalservice.approval.entity.ReportAttachment;
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

    /**
     * Entity → Response DTO 변환
     */
    public static AttachmentResDto fromReportAttachment(ReportAttachment att) {
        return AttachmentResDto.builder()
                .attachmentId(att.getId())
                .reportId(att.getReports().getId())
                .fileName(att.getName())
                .url(att.getUrl())
                .uploadedAt(att.getUploadTime())
                .build();
    }
}