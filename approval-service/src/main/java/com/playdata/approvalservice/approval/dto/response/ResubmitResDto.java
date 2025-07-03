package com.playdata.approvalservice.approval.dto.response;


import lombok.*;

import java.time.LocalDateTime;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResubmitResDto {
    private Long reportId;
    private String status;
    private LocalDateTime resubmittedAt;
}