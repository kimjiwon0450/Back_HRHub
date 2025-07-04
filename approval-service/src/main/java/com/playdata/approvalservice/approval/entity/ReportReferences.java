// ReportReferences.java
package com.playdata.approvalservice.approval.entity;

import com.playdata.approvalservice.approval.dto.request.ReferenceReqDto;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "report_references")
public class ReportReferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_approval_id", insertable = false, updatable = false)
    private Reports reports;

    /**
     * ReferenceReqDto → Entity 변환
     */
    public static ReportReferences fromReferenceReqDto(Reports report, ReferenceReqDto dto) {
        return ReportReferences.builder()
                .referenceId(report.getId())
                .employeeId(dto.getEmployeeId())
                .build();
    }
}
