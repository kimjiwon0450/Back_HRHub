package com.playdata.approvalservice.approval.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playdata.approvalservice.approval.dto.request.*;
import com.playdata.approvalservice.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
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
     * 재상신 이력
     */
    @Column(name = "previous_report_id")
    private Long previousReportId;

    /**
     * 예약이 갔는지 확인하는 변수
     */
    private boolean published = false; // 예약 발행 여부 (false: 예약 상태, true: 발행 완료)

    /**
     * 예약 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private ZonedDateTime scheduledAt;   // 예약 발행 시간 (null이면 즉시 발행)

    @Builder.Default
    @OneToMany(mappedBy = "reports", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("approvalContext ASC")
    private List<ApprovalLine> approvalLines = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "reports", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportReferences> reportReferences = new ArrayList<>();

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
     * 예약 상신 fromDto
     * @param dto
     * @param writerId
     * @return
     */
    public static Reports fromDtoForScheduled(ReportScheduleReqDto dto, Long writerId) {
        // new Reports() 대신 빌더를 사용하여 필수 필드를 명시적으로 초기화합니다.
        Reports report = Reports.builder()
                .writerId(writerId)
                .title(dto.getTitle())
                .content(dto.getContent())
                .reportTemplateId(dto.getTemplateId())
                .reportTemplateData(dto.getReportTemplateData())
                .reportCreatedAt(LocalDateTime.now())
                .reportStatus(ReportStatus.SCHEDULED) // 또는 다른 어떤 NOT NULL 값
                .build();

        // 결재 라인 설정
        if (dto.getApprovalLine() != null && !dto.getApprovalLine().isEmpty()) {
            report.replaceApprovalLines(dto.getApprovalLine());
        }

        // 참조자 설정 (1번 해결방안 적용 시)
        if (dto.getReferences() != null && !dto.getReferences().isEmpty()) {
            dto.getReferences().forEach(refDto -> report.addReference(refDto.getEmployeeId()));
        }

        return report;
    }


    /**
     * 보고서를 '예약(SCHEDULED)' 상태로 설정합니다.
     * 이 행위는 보고서 상태, 발행 여부, 예약 시간을 하나의 논리적 단위로 묶어 처리합니다.
     * @param scheduledTime 사용자가 지정한 예약 시간
     */
    public void schedule(ZonedDateTime scheduledTime) {
        this.reportStatus = ReportStatus.SCHEDULED;
        this.published = false;
        this.scheduledAt = scheduledTime;

        // 예약 시에는 currentApproverId를 null로 유지합니다.
        this.currentApproverId = null;
    }

    /**
     * 예약된 보고서를 '발행(IN_PROGRESS)' 상태로 변경합니다.
     * 스케줄러에 의해 호출되는 것을 의도합니다.
     */
    public void publish() {
        if (this.reportStatus != ReportStatus.SCHEDULED || this.published) {
            // 방어 로직: 이미 처리되었거나 잘못된 상태의 문서라면 로직을 중단.
            // 혹은 IllegalStateException을 던져서 문제를 명확히 알릴 수도 있습니다.
            return;
        }

        this.reportStatus = ReportStatus.IN_PROGRESS;
        this.published = true;
        this.submittedAt = LocalDateTime.now();

        // 첫 번째 결재자를 현재 결재자로 설정합니다.
        if (this.approvalLines != null && !this.approvalLines.isEmpty()) {
            // 순서가 보장되므로 get(0) 사용 가능
            this.currentApproverId = this.approvalLines.get(0).getEmployeeId();
        }
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

        // ★★★ 여기에 두 필드 업데이트 로직 추가 ★★★
        this.reportTemplateId = dto.getTemplateId();
        this.reportTemplateData = dto.getReportTemplateData();

        if (dto.getApprovalLine() != null) {
            replaceApprovalLines(dto.getApprovalLine());
            if (!approvalLines.isEmpty()) {
                this.currentApproverId = approvalLines.get(0).getEmployeeId();
            } else {
                this.currentApproverId = null; // 결재선이 비워졌을 경우
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
        Optional<ApprovalLine> next = this.approvalLines.stream()
                .filter(l -> l.getApprovalStatus() == ApprovalStatus.PENDING)
                .min(Comparator.comparing(ApprovalLine::getApprovalContext));

        // 3. PENDING 상태인 결재자가 남아있는지 확인
        if (next.isPresent()) {
            // 아직 결재할 사람이 남았으므로 IN_PROGRESS 유지
            this.reportStatus = ReportStatus.IN_PROGRESS;
            this.currentApproverId = next.get().getEmployeeId();
        } else {
            // PENDING 상태인 결재자가 더 이상 없으면 최종 승인
            this.reportStatus = ReportStatus.APPROVED;
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


    public ReportReferences addReference(@NotNull(message = "참조자 ID를 입력해주세요.") Long employeeId) {
        ReportReferences newRef = ReportReferences.builder()
                .reports(this)
                .employeeId(employeeId)
                .build();
        this.reportReferences.add(newRef);
        return newRef;
    }

    /**
     * 재상신 시 원본 문서의 템플릿 정보를 새로운 문서에 복사합니다.
     * 이 메소드는 오직 재상신 로직에서만 사용되어야 합니다.
     * @param originalTemplateId 원본 문서의 템플릿 ID
     * @param newTemplateData 사용자가 새로 입력한 템플릿 데이터
     */
    public void applyResubmitTemplateInfo(Long originalTemplateId, String newTemplateData) {
        this.reportTemplateId = originalTemplateId;
        this.reportTemplateData = newTemplateData;
    }
}

