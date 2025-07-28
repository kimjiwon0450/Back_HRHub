package com.playdata.approvalservice.approval.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
public class ReportScheduleReqDto extends ReportCreateReqDto {
    private ZonedDateTime scheduledAt;
}