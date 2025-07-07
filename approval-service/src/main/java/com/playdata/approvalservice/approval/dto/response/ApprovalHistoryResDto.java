package com.playdata.approvalservice.approval.dto.response;

import com.playdata.approvalservice.approval.entity.ApprovalStatus;
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
        private Integer order;
        private String approverName;
        private ApprovalStatus approvalStatus;
        private String comment;
        private String processedAt;
    }
}
