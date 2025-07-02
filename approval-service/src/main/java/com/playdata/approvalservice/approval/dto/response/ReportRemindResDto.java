package com.playdata.approvalservice.approval.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRemindResDto {
    /** 알림을 보낸 보고서 ID */
    private Long reportId;

    /** 마지막 리마인더 발송 시각 */
    private LocalDateTime remindedAt;

    /** 지금까지 보낸 리마인더 횟수 */
    private int reminderCount;
}