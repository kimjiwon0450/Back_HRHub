package com.playdata.approvalservice.approval.dto.response;


import lombok.*;

import java.time.LocalDateTime;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferenceResDto {
    private Long referenceId;
    private Long reportId;
    private Long employeeId;
    private LocalDateTime createdAt;
}
