package com.playdata.approvalservice.approval.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRemindResDto {
    private Long reportId;
    private LocalDateTime remindedAt;
    private int reminderCount;
    private String message;
}