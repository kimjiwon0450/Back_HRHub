package com.playdata.approvalservice.approval.dto.request;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitReportReqDto {
    private String comment;
}