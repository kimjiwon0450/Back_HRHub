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
public class ReportSaveReqDto {
    @NotBlank
    private String title;

    private String content;

    private List<ApprovalLineReqDto> approvalLine;

//    private List<AttachmentJsonReqDto> attachments;

    private List<ReferenceJsonReqDto> references;

}