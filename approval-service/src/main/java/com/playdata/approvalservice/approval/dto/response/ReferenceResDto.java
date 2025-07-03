package com.playdata.approvalservice.approval.dto.response;


import com.playdata.approvalservice.approval.entity.ReportReferences;
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
    /**
     * Entity → ReferenceResDto 변환
     */
    public static ReferenceResDto fromReportReferences(ReportReferences ref) {
        return ReferenceResDto.builder()
                .referenceId(ref.getReferenceId())
                .reportId(ref.getReportApprovalId())
                .employeeId(ref.getEmployeeId())
                .build();
    }
}
