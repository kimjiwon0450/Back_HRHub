package com.playdata.approvalservice.approval.dto.response;

import com.playdata.approvalservice.approval.entity.ReportStatus;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRecallResDto {
    private Long id;
    private ReportStatus reportStatus;
}