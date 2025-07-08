package com.playdata.approvalservice.approval.dto.response;

import lombok.*;

/**
 * 참조자 삭제 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportReferencesResDto {
    private Long reportId;
    private Long employeeId;


}