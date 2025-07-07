package com.playdata.approvalservice.approval.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "approval_line")
public class ApprovalLine {

    /** PK: 결재 라인 코드 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_line_id")
    private Long id;

    /** FK → 보고서ID */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_approval_id")
    private Reports reports;

    /** 승인자 */
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    /** 승인 여부 (APPROVED, PENDING, REJECTED) */
    @Column(name = "approval_status", nullable = false)
    private ApprovalStatus approvalStatus;

    /** 결재 순서 */
    @Column(name = "approval_context", nullable = false)
    private Integer approvalContext;

    /** 승인 일시 (승인/반려/회수 등 최종 변경 시각) */
    @Column(name = "approval_date_time", nullable = false)
    private LocalDateTime approvalDateTime;

    /** 결재 코멘트 */
    @Column(name = "approval_comment", length = 500)
    private String approvalComment;

    /**
     * 편의 메서드: 상태 변경 + 타임스탬프
     * */
    public void approve(String comment) {
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.approvalComment = comment;
        this.approvalDateTime = LocalDateTime.now();
    }

    public void rejected(String comment) {
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.approvalComment = comment;
        this.approvalDateTime = LocalDateTime.now();
    }


}
