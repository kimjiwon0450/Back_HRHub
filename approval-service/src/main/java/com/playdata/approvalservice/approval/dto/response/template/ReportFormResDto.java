package com.playdata.approvalservice.approval.dto.response.template;

import com.playdata.approvalservice.approval.dto.response.ReportDetailResDto;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class ReportFormResDto {

    private final Map<String, Object> template;
    private final Map<String, Object> formData;
    private final List<ReportDetailResDto.ApprovalLineResDto> approvalLine;
    private final List<ReportDetailResDto.AttachmentResDto> attachments;

    // 생성자에서 모든 final 필드를 초기화합니다.
    public ReportFormResDto(
            Map<String, Object> template,
            Map<String, Object> formData,
            List<ReportDetailResDto.ApprovalLineResDto> approvalLine,
            List<ReportDetailResDto.AttachmentResDto> attachments
    ) {
        this.template = template;
        this.formData = formData;
        this.approvalLine = approvalLine;
        this.attachments = attachments;
    }
}