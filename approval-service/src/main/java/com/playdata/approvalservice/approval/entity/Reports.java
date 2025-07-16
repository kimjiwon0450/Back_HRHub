package com.playdata.approvalservice.approval.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playdata.approvalservice.approval.dto.request.*;
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
    @Column(name = "report_template_id")
    private Long reportTemplateId;

    /**
     * 템플릿 데이터
     */
    @Column(name = "report_template_data", columnDefinition = "JSON")
    private String reportTemplateData;

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
    private LocalDateTime reportCreatedAt;

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
     * 탬플릿 여부
     */
    @Column(name = "report_is_custom")
    private Boolean isCustom;

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

    /**
     * 요청 DTO를 엔티티로 변환하는 팩토리 메서드 (최종 수정안)
     */
    public static Reports fromDto(ReportSaveReqDto dto, Long userId) {
        Reports report = Reports.builder()
                .writerId(userId)
                .title(dto.getTitle())
                .content(dto.getContent())
                .reportStatus(ReportStatus.DRAFT)
                .reportCreatedAt(LocalDateTime.now())
                .reportTemplateData(dto.getReportTemplateData())
                .reminderCount(0)
                .reportTemplateId(dto.getTemplateId())
                .build();

        // 결재 라인 설정
        if (dto.getApprovalLine() != null) {
            report.replaceApprovalLines(dto.getApprovalLine());
            if (!report.getApprovalLines().isEmpty()) {
                report.setCurrentApproverId(report.getApprovalLines().get(0).getEmployeeId());
            }
        }

        return report;
    }

    /**
     * DTO를 IN_PROGRESS 상태의 엔티리로 변환하는 메서드
     */
    public static Reports fromDtoForInProgress(ReportCreateReqDto dto, Long userId) {
        Reports report = Reports.builder()
                .writerId(userId)
                .title(dto.getTitle())
                .content(dto.getContent())
                .reportStatus(ReportStatus.IN_PROGRESS) // ★ 상태를 IN_PROGRESS로 설정
                .reportTemplateData(dto.getReportTemplateData())
                .reportCreatedAt(LocalDateTime.now())
                .submittedAt(LocalDateTime.now()) // ★ 제출일시도 바로 기록
                .reminderCount(0)
                .reportTemplateId(dto.getTemplateId())
                .build();

        // 결재 라인 설정 (기존 로직과 동일)
        report.replaceApprovalLines(dto.getApprovalLine());
        if (!report.getApprovalLines().isEmpty()) {
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
     * 연관관계 편의 메서드
     * Reports의 approvalLines에 자식을 추가하고,
     * ApprovalLine에는 부모(this)를 설정합니다.
     */
    public void addApprovalLine(ApprovalLine approvalLine) {
        this.approvalLines.add(approvalLine);
        approvalLine.setReports(this);
    }


    /**
     * 결재 라인을 DTO 목록으로 교체
     */
    public void replaceApprovalLines(List<ApprovalLineReqDto> dtoList) {

        approvalLines.clear();

        if(dtoList != null) {
            dtoList.forEach(dto -> {
                ApprovalLine line = ApprovalLine.builder()
                        .employeeId(dto.getEmployeeId())
                        .approvalContext(dto.getApprovalContext())
                        .approvalStatus(ApprovalStatus.PENDING)
                        .approvalDateTime(LocalDateTime.now())
                        .build();

                this.addApprovalLine(line);
            });
        }
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
                .reportTemplateId(this.reportTemplateId)
                .reportTemplateData(this.reportTemplateData)
                .title(newTitle)
                .content(newContent)
                .reportStatus(ReportStatus.DRAFT)
                .reportCreatedAt(LocalDateTime.now())
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

