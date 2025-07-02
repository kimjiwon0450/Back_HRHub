package com.playdata.approvalservice.approval.dto.response;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportUpdateResDto {
    private Long id;
    private String title;
    private String status;
}