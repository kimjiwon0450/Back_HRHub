package com.playdata.approvalservice.approval.dto.response;


import com.playdata.approvalservice.approval.entity.ReportStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResubmitResDto {
    private Long reportId;
    private ReportStatus reportStatus;
    private LocalDateTime resubmittedAt;
}