package com.playdata.approvalservice.approval.dto.response;

import com.playdata.approvalservice.approval.entity.ApprovalStatus;
import com.playdata.approvalservice.approval.entity.ReportStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCreateResDto {
    private Long id;
    private Long writerId;
    private ReportStatus reportStatus;
    private String title;
    private String content;
    private ApprovalStatus approvalStatus;
    private LocalDateTime createAt;
    private LocalDateTime submittedAt;
    private LocalDateTime returnAt;
    private LocalDateTime completedAt;
    private Long approvalId;
    private int reminderCount;
    private LocalDateTime remindedAt;

}