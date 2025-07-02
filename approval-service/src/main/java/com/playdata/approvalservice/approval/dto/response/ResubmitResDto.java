package com.playdata.approvalservice.approval.dto.response;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

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