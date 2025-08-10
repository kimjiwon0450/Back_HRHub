package com.playdata.approvalservice.approval.dto.response;

import com.playdata.approvalservice.approval.entity.ApprovalStatus;
import com.playdata.approvalservice.approval.entity.ReportStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private LocalDateTime reportCreateAt;
    private LocalDateTime submittedAt;
    private LocalDateTime returnAt;
    private LocalDateTime completedAt;
    private Long approvalId;
    private int reminderCount;
    private LocalDateTime remindedAt;

    private Map<String, Object> template;
    private Map<String, Object> formData;

}