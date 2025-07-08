package com.playdata.approvalservice.approval.dto.response;

import com.playdata.approvalservice.approval.entity.ApprovalStatus;
import com.playdata.approvalservice.approval.entity.ReportStatus;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalProcessResDto {
    private Long reportId;
    private ApprovalStatus approvalStatus;
    private ReportStatus reportStatus;
    private String nextApprover;
}
