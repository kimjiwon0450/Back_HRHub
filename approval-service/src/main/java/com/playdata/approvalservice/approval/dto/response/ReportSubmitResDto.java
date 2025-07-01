package com.playdata.approvalservice.approval.dto.response;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSubmitResDto {
    private Long id;
    private String status;
    private String submittedAt;
}