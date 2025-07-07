package com.playdata.approvalservice.approval.dto.response;

import com.playdata.approvalservice.approval.dto.request.ApprovalHistoryReqDto;
import com.playdata.approvalservice.approval.entity.ApprovalStatus;
import com.playdata.approvalservice.approval.entity.Reports;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter @ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalHistoryResDto {
    private int order;
    private Long employeeId;
    private String employeeName;
    private ApprovalStatus approvalStatus;
    private String comment;
    private LocalDateTime approvalDateTime;
}
