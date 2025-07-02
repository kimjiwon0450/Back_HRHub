package com.playdata.approvalservice.approval.dto.response;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCreateResDto {
    private Long id;
    private String title;
    private String status;
}