package com.playdata.approvalservice.approval.dto.response;

import com.playdata.approvalservice.approval.entity.ApprovalStatus;
import com.playdata.approvalservice.approval.entity.ReportStatus;
import lombok.*;

import java.util.List;

@Getter @Setter @ToString
@NoArgsConstructor @AllArgsConstructor @Builder
public class ReportDetailResDto {
    private Long id;
    private String title;
    private String content;
    private List<AttachmentResDto> attachments;
    private List<ReferenceJsonResDto> references;
    private WriterInfoDto writer;
    private String createdAt;
    private ReportStatus reportStatus;
    private List<ApprovalLineResDto> approvalLine;
    private String currentApprover;
    private String dueDate;

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