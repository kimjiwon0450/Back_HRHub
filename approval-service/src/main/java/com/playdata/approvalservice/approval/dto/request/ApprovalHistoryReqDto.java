package com.playdata.approvalservice.approval.dto.request;

import com.playdata.approvalservice.approval.entity.ApprovalStatus;
import com.playdata.approvalservice.approval.entity.Reports;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalHistoryReqDto {
    private int order;
    private Reports reportName;
    private ApprovalStatus approvalStatus;
    private String comment;
    private String processedAt;
}
