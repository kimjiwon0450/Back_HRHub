package com.playdata.approvalservice.approval.dto.request;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportUpdateReqDto {
    private String title;
    private String content;
    private List<ApprovalLineReqDto> approvalLine;
    private List<AttachmentReqDto> attachments;
}
