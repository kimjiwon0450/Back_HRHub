package com.playdata.approvalservice.approval.dto.response;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRecallResDto {
    private Long id;
    private String status;
}