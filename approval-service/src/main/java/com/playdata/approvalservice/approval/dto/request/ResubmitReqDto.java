package com.playdata.approvalservice.approval.dto.request;

import com.playdata.approvalservice.approval.dto.response.ReportDetailResDto;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResubmitReqDto {
    @NotBlank
    private String newTitle;

    private String newContent;               // 보고서 본문도 수정 가능하다면

    private List<ApprovalLineReqDto> approvalLine;  // List 타입으로 변경

    private String comment;

    private List<AttachmentJsonReqDto> attachments;

    private List<ReportDetailResDto.ReferenceJsonResDto> references;

    private String reportTemplateData;


}
