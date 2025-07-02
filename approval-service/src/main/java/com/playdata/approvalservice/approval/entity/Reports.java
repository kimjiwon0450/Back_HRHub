package com.playdata.approvalservice.approval.entity;

import com.playdata.approvalservice.approval.dto.request.ApprovalLineReqDto;
import com.playdata.approvalservice.approval.dto.request.AttachmentReqDto;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


import com.playdata.approvalservice.common.entity.BaseTimeEntity;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "board_report")
public class Reports extends BaseTimeEntity {

    /**
     * 보고서 id
     * PK
     * 자동 생성
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    /** 기안 직원 */
    @Column(name = "report_writer_id", nullable = false)
    private Long writerId;


    /** 기안 양식 */
    @Column(name = "report_type", nullable = false)
    private String type;

    /** 기안 제목 */
    @Column(name = "report_title", nullable = false)
    private String title;

    /** 기안 본문 */
    @Column(name = "report_content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 결재 상태*/
    @Column(name = "report_status", columnDefinition = "ENUM", nullable = false)
    private ApprovalStatus status;

    /** 상신(제출) 일시 */
    @Column(name = "report_created_at")
    private LocalDateTime createdAt;

    /** 승인 일시 */
    @Column(name = "report_submitted_at")
    private LocalDateTime submittedAt;

    /** 회수(리콜) 일시 */
    @Column(name = "report_return_at")
    private LocalDateTime returnAt;

    /** 최종 완료(모두 승인) 시각 */
    @Column(name = "report_completed_at")
    private LocalDateTime completedAt;

    /** 현재 차례 결재자 ID */
    @Column(name = "report_current_approver_id")
    private Long currentApproverId;

    /** 상세 JSON */
    @Column(name = "report_detail", columnDefinition = "JSON")
    private String detail;

    /** 보낸 리마인더 횟수 */
    @Column(name = "reminder_count", nullable = false)
    private Integer reminderCount;

    /** 마지막 리마인더 시각 */
    @Column(name = "reminded_at")
    private LocalDateTime remindedAt;

    /** 첨부파일 (1:N) */
    @Builder.Default
    @OneToMany(mappedBy = "boardReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportAttachment> attachments = new ArrayList<>();

    /** 결재라인 (1:N) */
    @Builder.Default
    @OneToMany(mappedBy = "boardReport", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderSequence ASC")
    private List<ApprovalLine> approvalLine = new ArrayList<>();

    public void updateContent(String newTitle, String newContent) {
        this.title = newTitle;
        this.content = newContent;
    }

    public void replaceApprovalLine(List<ApprovalLineReqDto> dtoList){
        this.approvalLine.clear();
        dtoList.forEach(dto -> {
            ApprovalLine ap = ApprovalLine.builder()
                    .reports(this)
                    .employeeId(dto.getEmployeeId())
                    .approvalOrder(dto.getOrder())
                    .status(ApprovalStatus.PENDING)
                    .build();
            this.approvalLine.add(ap);
        });
    }

    public void replaceAttachments(List<AttachmentReqDto> dtoList){
        this.attachments.clear();
        if(dtoList != null){
            dtoList.forEach(dto -> {
                ReportAttachment att = ReportAttachment.builder()
                        .reports(this)
                        .name(dto.getFileName())
                        .url(dto.getUrl())
                        .build();
                this.attachments.add(att);
            });
        }
    }
}