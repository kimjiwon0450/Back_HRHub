package com.playdata.approvalservice.approval.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "approval_line")
public class ApprovalLine {

    /** PK: 결재 코드 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_line_id")
    private Long id;

    /** FK → 보고서ID */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_approval_id", nullable = false)
    private Reports reports;

    /** 결재자(사번) */
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    /** 승인 여부 (APPROVED, PENDING, REJECTED) */
    @Column(name = "approval_status", nullable = false)
    private ApprovalStatus status;

    /** 결재 순서 */
    @Column(name = "approval_order", nullable = false)
    private Integer approvalOrder;

    /** 결재 처리 일시 (승인/반려/회수 등 최종 변경 시각) */
    @Column(name = "approval_date_time", nullable = false)
    private LocalDateTime approvalDateTime;

    /** 결재 코멘트 */
    @Column(name = "approval_comment", length = 500)
    private String approvalComment;

    /**
     * 편의 메서드: 상태 변경 + 타임스탬프
     * */
    public void approve(String comment) {
        this.status = ApprovalStatus.APPROVED;
        this.approvalComment = comment;
        this.approvalDateTime = LocalDateTime.now();
    }

    public void reject(String comment) {
        this.status = ApprovalStatus.REJECTED;
        this.approvalComment = comment;
        this.approvalDateTime = LocalDateTime.now();
    }
}
