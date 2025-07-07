package com.playdata.approvalservice.approval.entity;

import com.playdata.approvalservice.approval.dto.request.ApprovalLineReqDto;
import com.playdata.approvalservice.approval.dto.request.AttachmentReqDto;
import com.playdata.approvalservice.approval.dto.request.ReportCreateReqDto;
import com.playdata.approvalservice.approval.dto.request.ReportUpdateReqDto;
import com.playdata.approvalservice.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "reports")
public class Reports extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @Column(name = "report_writer_id", nullable = false)
    private Long writerId;

    @Column(name = "report_type", nullable = false)
    private String type;

    @Column(name = "report_title", nullable = false)
    private String title;

    @Column(name = "report_content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_status", nullable = false)
    private ReportStatus reportStatus;

    @Column(name = "report_created_at")
    private LocalDateTime createdAt;

    @Column(name = "report_submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "report_return_at")
    private LocalDateTime returnAt;

    @Column(name = "report_completed_at")
    private LocalDateTime completedAt;

    @Setter
    @Column(name = "report_current_approver_id")
    private Long currentApproverId;

    @Setter
    @Column(name = "report_detail", columnDefinition = "JSON")
    private String detail;

    @Column(name = "reminder_count", nullable = false)
    private Integer reminderCount;

    @Column(name = "reminded_at")
    private LocalDateTime remindedAt;


    @Builder.Default
    @OneToMany(mappedBy = "reports", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportAttachment> attachments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "reports", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("approvalOrder ASC")
    private List<ApprovalLine> approvalLines = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "reports", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportReferences> references = new ArrayList<>();

    /**
     * 요청 DTO를 엔티티로 변환하는 팩토리 메서드
     */
    public static Reports fromDto(ReportCreateReqDto dto, Long userId) {
        Reports report = Reports.builder()
                .writerId(userId)
                .type(dto.getType())
                .title(dto.getTitle())
                .content(dto.getContent())
                .reportStatus(ReportStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .reminderCount(0)
                .build();

        // 첨부파일 설정
        if (dto.getAttachments() != null) {
            report.replaceAttachments(dto.getAttachments());
        }
        // 결재 라인 설정
        report.replaceApprovalLines(dto.getApprovalLine());
        if (!report.approvalLines.isEmpty()) {
            report.currentApproverId = report.approvalLines.get(0).getEmployeeId();
        }
        return report;
    }

    /**
     * 수정 요청 DTO의 내용을 엔티티에 반영
     */
    public void updateFromDto(ReportUpdateReqDto dto) {
        this.title = dto.getTitle();
        this.content = dto.getContent();
        if (dto.getDetail() != null) this.detail = dto.getDetail();

        if (dto.getAttachments() != null) {
            replaceAttachments(dto.getAttachments());
        }
        if (dto.getApprovalLine() != null) {
            replaceApprovalLines(dto.getApprovalLine());
            if (!approvalLines.isEmpty()) {
                this.currentApproverId = approvalLines.get(0).getEmployeeId();
            }
        }
    }

    /**
     * 제목과 본문을 업데이트
     */
    public void updateContent(String newTitle, String newContent) {
        this.title = newTitle;
        this.content = newContent;
    }

    /**
     * 결재 라인을 DTO 목록으로 교체
     */
    public void replaceApprovalLines(List<ApprovalLineReqDto> dtoList) {
        approvalLines.clear();
        dtoList.forEach(dto -> {
            ApprovalLine line = ApprovalLine.builder()
                    .reports(this)
                    .employeeId(dto.getEmployeeId())
                    .approvalOrder(dto.getOrder())
                    .approvalStatus(ApprovalStatus.PENDING)
                    .approvalDateTime(LocalDateTime.now())
                    .build();
            approvalLines.add(line);
        });
    }

    /**
     * 첨부파일 목록을 DTO 목록으로 교체
     */
    public void replaceAttachments(List<AttachmentReqDto> dtoList) {
        attachments.clear();
        dtoList.forEach(dto -> {
            ReportAttachment att = ReportAttachment.builder()
                    .reports(this)
                    .name(dto.getFileName())
                    .url(dto.getUrl())
                    .build();
            attachments.add(att);
        });
    }

    /**
     * 보고서 상신 처리 (초안 → 결재 대기)
     */
    public void submit() {
        this.reportStatus = ReportStatus.IN_PROGRESS;
        this.submittedAt = LocalDateTime.now();

        ApprovalLine next = approvalLines.stream()
                .min(Comparator.comparing(ApprovalLine::getApprovalOrder))
                .orElseThrow();
        this.currentApproverId = next.getEmployeeId();
    }

    /**
     * 보고서 회수 처리 (결재 대기 → 회수)
     */
    public void recall() {
        this.reportStatus = ReportStatus.RECALLED;
        this.returnAt = LocalDateTime.now();
        this.currentApproverId = null;
    }

    /**
     * 리마인더 전송 처리
     */
    public void remind() {
        this.reminderCount = this.reminderCount + 1;
        this.remindedAt = LocalDateTime.now();
    }

    /**
     * 재상신 처리 (반려 → 결재 대기)
     */
    public void resubmit(String comment) {
        this.reportStatus = ReportStatus.DRAFT;
        // 재상신 시 추가 로직 필요 시 적용
    }

    // ② 결재 처리 후 호출
    public void moveToNextOrComplete(ApprovalLine line) {
        if (line.getApprovalStatus() == ApprovalStatus.REJECTED) {
            // 반려
            this.reportStatus = ReportStatus.REJECTED;
            this.returnAt = line.getApprovalDateTime();
            this.currentApproverId = null;
            return;
        }

        // 승인된 경우, 다음 결재자 찾기
        Optional<ApprovalLine> next = approvalLines.stream()
                .filter(l -> l.getApprovalOrder() > line.getApprovalOrder())
                .min(Comparator.comparing(ApprovalLine::getApprovalOrder));

        if (next.isPresent()) {
            // 아직 남은 결재자가 있으면 in progress
            this.reportStatus = ReportStatus.IN_PROGRESS;
            this.currentApproverId = next.get().getEmployeeId();
        } else {
            // 마지막 결재자였으면 최종 승인
            this.reportStatus      = ReportStatus.APPROVED;
            this.completedAt = line.getApprovalDateTime();
            this.currentApproverId = null;
        }
    }

}

