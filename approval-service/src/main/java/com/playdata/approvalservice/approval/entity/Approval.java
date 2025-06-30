package com.playdata.approvalservice.approval.entity;


import com.playdata.approvalservice.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "approval")
@IdClass(ApprovalId.class)
//결재자 단계
public class Approval extends BaseTimeEntity {


    /**
     * FK → BoardReport.id
     */
    @Id
    @Column(name = "report_approval_id2")
    private Long reportApprovalId;

    /**
     * 결재자(사원) ID
     */
    @Id
    @Column(name = "employee_id")
    private Long employeeId;

    /**
     * FK → BoardReport.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_approval_id2", insertable = false, updatable = false)
    private BoardReport boardReport;

    /**
     * 해당 결재 단계의 상태(기안, 승인, 반려, 회수)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    private ReportType status;

    /**
     * 이 단계 결재 처리 일시
     */
    @Column(name = "approval_date_time", nullable = false)
    private LocalDateTime approvalDateTime;

    /**
     * 결재 순서 (1,2,3…)
     */
    @Column(name = "approval_order_sequence", nullable = false)
    private Integer orderSequence;
}

