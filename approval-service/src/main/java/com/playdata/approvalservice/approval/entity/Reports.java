package com.playdata.approvalservice.approval.entity;

import com.playdata.approvalservice.approval.dto.request.ApprovalLineReqDto;
import com.playdata.approvalservice.approval.dto.request.AttachmentJsonReqDto;
import com.playdata.approvalservice.approval.dto.request.ReportCreateReqDto;
import com.playdata.approvalservice.approval.dto.request.ReportUpdateReqDto;
import com.playdata.approvalservice.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "reports")
public class Reports extends BaseTimeEntity {

    /**
     * 보고서 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    /**
     * 기안직원 (FK)
     */
    @Column(name = "report_writer_id", nullable = false)
    private Long writerId;

    /**
     * 기안 양식 (FK)
     */
    @Column(name = "report_template", columnDefinition = "JSON")
    private Long reportTemplate;

    /**
     * 템플릿 데이터
     */
    @Column(name = "report_template_data", columnDefinition = "JSON")
    private Long reportTemplateData;

    /**
     * 기안 제목
     */
    @Column(name = "report_title", nullable = false)
    private String title;

    /**
     * 기안 내용
     */
    @Column(name = "report_content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 결재 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "report_status", nullable = false)
    private ReportStatus reportStatus;

    /**
     * 제출 일시
     */
    @Column(name = "report_created_at")
    private LocalDateTime createdAt;

    /**
     * 승인 일시
     */
    @Column(name = "report_submitted_at")
    private LocalDateTime submittedAt;

    /**
     * 회수 일시
     */
    @Column(name = "report_return_at")
    private LocalDateTime returnAt;

    /**
     * 완료 일시
     */
    @Column(name = "report_completed_at")
    private LocalDateTime completedAt;

    /**
     * 현재 결재 상태
     */
    @Setter
    @Column(name = "report_current_approver_id")
    private Long currentApproverId;

    /**
     * 보고서 첨부 파일
     */
    @Setter
    @Column(name = "report_detail", columnDefinition = "JSON")
    private String detail;

    /**
     * 리마인더 횟수
     */
    @Column(name = "reminder_count", nullable = false)
    private Integer reminderCount;

    /**
     * 리마인더 발송 시각
     */
    @Column(name = "reminded_at")
    private LocalDateTime remindedAt;

    /**
     * 재상신 이력
     */
    @Column(name = "previous_report_id")
    private Long previousReportId;

    @Builder.Default
    @OneToMany(mappedBy = "reports", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("approvalContext ASC")
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
                .title(dto.getTitle())
                .content(dto.getContent())
                .reportStatus(ReportStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .reminderCount(0)
                .build();

        // 첨부파일 설정

        // 결재 라인 설정
        report.replaceApprovalLines(dto.getApprovalLine());
        if (!report.approvalLines.isEmpty()) {
            report.currentApproverId = report.approvalLines.get(0).getEmployeeId();
        }

        // 첫 결재자
        if(!report.getApprovalLines().isEmpty()) {
            report.setCurrentApproverId(
                    report.getApprovalLines().get(0).getEmployeeId()
            );
        }

        return report;
    }

    /**
     * 수정 요청 DTO의 내용을 엔티티에 반영
     */
    public void updateFromDto(ReportUpdateReqDto dto) {
        this.title = dto.getTitle();
        this.content = dto.getContent();

        if (dto.getApprovalLine() != null) {
            replaceApprovalLines(dto.getApprovalLine());
            if (!approvalLines.isEmpty()) {
                this.currentApproverId = approvalLines.get(0).getEmployeeId();
            }
        }
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
                    .approvalContext(dto.getOrder())
                    .approvalStatus(ApprovalStatus.PENDING)
                    .approvalDateTime(LocalDateTime.now())
                    .build();
            approvalLines.add(line);
        });
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
                .filter(l -> l.getApprovalContext() > line.getApprovalContext())
                .min(Comparator.comparing(ApprovalLine::getApprovalContext));

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

    /**
     * 현재 보고서의 정보를 바탕으로 재상신할 새로운 보고서를 생성합니다.
     *
     * @param newTitle    새로운 제목
     * @param newContent  새로운 내용
     * @param newLinesDto 새로운 결재 라인 정보
     * @param attachments
     * @return 재상신된 새로운 Reports 객체
     */
    public Reports resubmit(String newTitle, String newContent, List<ApprovalLineReqDto> newLinesDto, List<AttachmentJsonReqDto> attachments) {
        // 1. 새로운 Reports 객체 생성
        Reports newReport = Reports.builder()
                .writerId(this.writerId)
                .reportTemplate(this.reportTemplate)
                .reportTemplateData(this.reportTemplateData)
                .title(newTitle)
                .content(newContent)
                .reportStatus(ReportStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .submittedAt(LocalDateTime.now())
                .previousReportId(this.id)
                .reminderCount(0)
                .build();

        // 2. 새로운 결재 라인 설정
        newReport.replaceApprovalLines(newLinesDto); // 기존 메소드 재활용
        if (!newReport.getApprovalLines().isEmpty()) {
            newReport.setCurrentApproverId(newReport.getApprovalLines().get(0).getEmployeeId());
        }

        newReport.setDetail(this.getDetail());

        return newReport;
    }

    // 기존 보고서의 상태를 변경하는 메소드
    public void markAsResubmitted() {
        this.reportStatus = ReportStatus.RECALLED; // 또는 'RESUBMITTED' 같은 새로운 상태
        this.returnAt = LocalDateTime.now();
        this.currentApproverId = null;
    }
}

