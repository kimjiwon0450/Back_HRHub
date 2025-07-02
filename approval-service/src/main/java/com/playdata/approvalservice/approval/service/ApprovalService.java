package com.playdata.approvalservice.approval.service;

import com.playdata.approvalservice.approval.dto.request.*;
import com.playdata.approvalservice.approval.dto.response.*;
import com.playdata.approvalservice.approval.entity.*;
import com.playdata.approvalservice.approval.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 전자결재 비즈니스 로직 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ApprovalService {

    private final BoardReportRepository boardReportRepository;
    private final ApprovalRepository approvalRepository;
    private final ReportAttachmentRepository attachmentRepository;
    private final ReportReferenceRepository referenceRepository;
    private final CommentRepository commentRepository;

    /**
     * 보고서 생성 (임시저장 DRAFT)
     */
    public ReportCreateResDto createReport(ReportCreateReqDto req, Long userId) {
        // 1) BoardReport 엔티티 생성
        Reports report = Reports.builder()
                .writerId(userId)
                .type(req.getType())
                .title(req.getTitle())
                .content(req.getContent())
                .submittedAt(LocalDateTime.now())
                .status(ReportStatus.DRAFT)
                .approvedAt(LocalDateTime.now())
                .build();

        // 2) 결재라인(Approval) 설정
        List<ApprovalLine> approvalLines = req.getApprovalLine().stream()
                .map(lineDto -> ApprovalLine.builder()
                        .reports(report)                 // 양방향 연관관계
                        .employeeId(lineDto.getEmployeeId()) // 결재자 ID
                        .orderSequence(lineDto.getOrder())   // 순서
                        .status(ApprovalStatus.PENDING)      // 최초엔 모두 대기(PENDING)
                        .approvalDateTime(LocalDateTime.now()) // 생성 시각
                        .build()
                )
                .toList();
        report.getApprovalLine().addAll(approvalLines);

        // 3) 첨부파일 설정
        if (req.getAttachments() != null) {
            List<ReportAttachment> atts = req.getAttachments().stream()
                    .map(attDto -> ReportAttachment.builder()
                            .reports(report)
                            .name(attDto.getFileName())
                            .url(attDto.getUrl())
                            .uploadTime(LocalDateTime.now())
                            .build()
                    )
                    .toList();
            report.getAttachments().addAll(atts);
        }

        // 4) 저장 및 반환
        Reports saved = boardReportRepository.save(report);
        return ReportCreateResDto.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .status(saved.getStatus().name())
                .build();
    }

    /**
     * 보고서 수정 (DRAFT 상태)
     */
    @Transactional
    public ReportUpdateResDto updateReport(Long reportId,
                                           ReportUpdateReqDto req,
                                           Long userId) {
        // 1) DRAFT 상태·권한 확인
        Reports report = fetchDraftReport(reportId, userId);

        // 2) 제목/본문 교체
        report.updateContent(req.getTitle(), req.getContent());

        // 3) 결재라인 전체 교체
        report.replaceApprovalLine(req.getApprovalLine());

        // 4) 첨부파일 전체 교체
        report.replaceAttachments(req.getAttachments());

        // 5) 저장 (변경 감지로 인해 생략 가능하지만, 명시적으로 호출)
        Reports updated = boardReportRepository.save(report);

        return ReportUpdateResDto.builder()
                .id(updated.getId())
                .title(updated.getTitle())
                .status(updated.getStatus().name())
                .build();
    }

    /**
     * DRAFT 상태 & 작성자 권한 검증 헬퍼
     */
    private Reports fetchDraftReport(Long reportId, Long userId){
        Reports report = boardReportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("해당 보고서를 찾을 수 없습니다. id=" + reportId));

        if(report.getStatus() != ReportStatus.DRAFT){
            throw new IllegalStateException( "대기 상태가 아닌 보고서는 수정할 수 없습니다. 현재 상태="+ report.getStatus());
        }

        if(!report.getSubmitId().equals(userId)){
            throw new AccessDeniedException("작성자만 수정할 수 있습니다.");
        }

        return report;
    }


    /**
     * 보고서 목록 조회
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public ReportListResDto getReports(String role, String status,
                                       int page, int size,
                                       String keyword, String from, String to,
                                       Long userId) {
        // TODO: role(writer/approver)에 따라 repository 메서드 호출
        // TODO: 페이징, 필터링, DTO 매핑
        return new ReportListResDto();
    }

    /**
     * 보고서 상세 조회
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public ReportDetailResDto getReportDetail(Long reportId, Long userId) {
        Reports report = boardReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("보고서를 찾을 수 없습니다."));
        return ReportDetailResDto.fromEntity(report);
    }

    /**
     * 결재 처리 (승인/반려)
     */
    public ApprovalProcessResDto processApproval(Long reportId, Long userId, ApprovalProcessReqDto req) {
        // TODO: 권한, 현재 결재자 확인
        ApprovalLine approvalLine = approvalRepository.findByReportIdAndEmployeeIdAndStatus(reportId, userId, ApprovalStatus.PENDING)
                .orElseThrow(() -> new IllegalArgumentException("결재 권한이 없습니다."));
        approvalLine.process(req.getAction(), req.getComment());
        approvalLine.setProcessedAt(LocalDateTime.now());
        approvalRepository.save(approvalLine);

        // TODO: 다음 결재자 셋업 및 report status 업데이트
        return ApprovalProcessResDto.fromEntity(approvalLine);
    }

    /**
     * 결재 이력 조회
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public ApprovalHistoryResDto getApprovalHistory(Long reportId) {
        List<ApprovalLine> history = approvalRepository.findByReportIdOrderByOrder(reportId);
        return ApprovalHistoryResDto.fromEntities(history);
    }

    /**
     * 보고서 상신 (DRAFT → IN_PROGRESS)
     */
    public ReportSubmitResDto submitReport(Long reportId, Long userId, SubmitReportReqDto req) {
        Reports report = fetchDraftReport(reportId, userId);
        report.submit(req != null ? req.getComment() : null);
        Reports updated = boardReportRepository.save(report);
        return ReportSubmitResDto.fromEntity(updated);
    }

    /**
     * 보고서 삭제 (DRAFT)
     */
    public void deleteReport(Long reportId, Long userId) {
        Reports report = fetchDraftReport(reportId, userId);
        boardReportRepository.delete(report);
    }

    /**
     * 보고서 회수 (IN_PROGRESS → RECALLED)
     */
    public ReportRecallResDto recallReport(Long reportId, Long userId) {
        Reports report = boardReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("보고서를 찾을 수 없습니다."));
        report.recall(userId);
        Reports updated = boardReportRepository.save(report);
        return ReportRecallResDto.fromEntity(updated);
    }

    /**
     * 결재 리마인드
     */
    public ReportRemindResDto reportRemind(Long reportId, Long userId) {
        Reports report = boardReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("보고서를 찾을 수 없습니다."));
        // TODO: reminderCount++, remindedAt 업데이트 및 알림 발송
        report.remind();
        boardReportRepository.save(report);
        return ReportRemindResDto.fromEntity(report);
    }

    /**
     * 보고서 재상신 (REJECTED → IN_PROGRESS)
     */
    public ResubmitResDto resubmitReport(Long reportId, Long userId, ResubmitReqDto req) {
        Reports report = boardReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("보고서를 찾을 수 없습니다."));
        report.resubmit(req != null ? req.getComment() : null);
        Reports updated = boardReportRepository.save(report);
        return ResubmitResDto.fromEntity(updated);
    }

    /**
     * 참조자 추가
     */
    public ReferenceResDto addReference(Long reportId, Long userId, ReferenceReqDto req) {
        ReportReference ref = ReportReference.builder()
                .reportId(reportId)
                .employeeId(req.getEmployeeId())
                .createdAt(LocalDateTime.now())
                .build();
        ReportReference saved = referenceRepository.save(ref);
        return ReferenceResDto.fromEntity(saved);
    }

    /**
     * 참조자 삭제
     */
    public ReportReferencesResDto deleteReferences(Long reportId, Long userId, Long employeeId) {
        referenceRepository.deleteByReportIdAndEmployeeId(reportId, employeeId);
        return new ReportReferencesResDto(reportId, employeeId);
    }

    /**
     * 첨부파일 업로드
     */
    public AttachmentResDto uploadAttachment(Long reportId, Long userId, AttachmentReqDto req) {
        ReportAttachment att = ReportAttachment.builder()
                .reportId(reportId)
                .fileName(req.getFileName())
                .url(req.getUrl())
                .uploadedAt(LocalDateTime.now())
                .build();
        ReportAttachment saved = attachmentRepository.save(att);
        return AttachmentResDto.fromEntity(saved);
    }

    /**
     * 결재 코멘트 등록
     */
    public CommentResDto createComment(Long reportId, Long userId, CommentReqDto req) {
        Comment comment = Comment.builder()
                .reportId(reportId)
                .authorId(userId)
                .content(req.getComment())
                .createdAt(LocalDateTime.now())
                .build();
        Comment saved = commentRepository.save(comment);
        return CommentResDto.fromEntity(saved);
    }

}
