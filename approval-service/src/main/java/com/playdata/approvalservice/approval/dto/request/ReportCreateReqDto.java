package com.playdata.approvalservice.approval.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCreateReqDto {
    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotEmpty
    private List<ApprovalLineReqDto> approvalLine;

    private List<AttachmentJsonReqDto> attachments;

    private List<ReferenceJsonReqDto> references;

}