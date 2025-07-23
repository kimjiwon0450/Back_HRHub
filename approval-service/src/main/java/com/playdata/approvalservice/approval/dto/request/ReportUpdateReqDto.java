package com.playdata.approvalservice.approval.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportUpdateReqDto {
    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private List<ApprovalLineReqDto> approvalLine;
    private List<AttachmentJsonReqDto> attachments;
    private List<ReferenceJsonReqDto> references;

    private Long templateId;
    private String reportTemplateData; // JSON 형식의 문자열
}
