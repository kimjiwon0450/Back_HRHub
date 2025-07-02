package com.playdata.approvalservice.approval.dto.response;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalProcessResDto {
    private Long reportId;
    private String action;
    private String status;
    private String nextApprover;
}
