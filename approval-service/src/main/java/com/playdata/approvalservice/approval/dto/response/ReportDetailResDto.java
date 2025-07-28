package com.playdata.approvalservice.approval.dto.response;

import com.playdata.approvalservice.approval.entity.ApprovalStatus;
import com.playdata.approvalservice.approval.entity.ReportStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter @Setter @ToString
@NoArgsConstructor @AllArgsConstructor @Builder
public class ReportDetailResDto {
    private Long id;
    private String title;
    private String content;
    private List<AttachmentResDto> attachments;
    private List<ReferenceJsonResDto> references;
    private WriterInfoDto writer;
    private String reportCreatedAt;
    private ReportStatus reportStatus;
    private List<ApprovalLineResDto> approvalLine;
    private String currentApprover;
    private String dueDate;

    private Map<String, Object> template;
    private Map<String, Object> formData;

    private Long templateId;

    // 저장된 예약 정보를 클라이언트에 전달하기 위함
    private boolean published;
    private LocalDateTime scheduledAt;

    @Getter @Setter @ToString
    @NoArgsConstructor
    @AllArgsConstructor @Builder
    public static class AttachmentResDto {
        private String fileName;
        private String url;
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class WriterInfoDto {
        private Long id;
        private String name;
    }

    @Getter @Setter @ToString
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ApprovalLineResDto {
        private Long employeeId;
        private String name;
        private ApprovalStatus approvalStatus;
        private int context;
        private String approvedAt;
    }


    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ReferenceJsonResDto {
        private Long employeeId;
    }
}