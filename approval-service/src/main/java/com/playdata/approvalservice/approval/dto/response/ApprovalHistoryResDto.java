package com.playdata.approvalservice.approval.dto.response;

import com.playdata.approvalservice.approval.entity.ApprovalStatus;
import com.playdata.approvalservice.approval.entity.Reports;
import lombok.*;

import java.util.List;

@Getter
@Setter @ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalHistoryResDto {
    private List<ApprovalHistoryItemDto> history;

    @Getter @Setter
    @ToString
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ApprovalHistoryItemDto {
        private int order;
        private Reports reportName;
        private ApprovalStatus approvalStatus;
        private String comment;
        private String processedAt;
    }
}
