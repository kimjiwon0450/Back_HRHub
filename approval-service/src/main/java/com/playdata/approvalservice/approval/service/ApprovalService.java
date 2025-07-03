package com.playdata.approvalservice.approval.service;

import com.playdata.approvalservice.approval.dto.request.*;
import com.playdata.approvalservice.approval.dto.response.*;
import com.playdata.approvalservice.approval.entity.*;
import com.playdata.approvalservice.approval.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 전자결재 비즈니스 로직 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ApprovalService {

    private final ReportsRepository reportsRepository;
    private final ApprovalRepository approvalRepository;
    private final ReportAttachmentRepository attachmentRepository;
    private final ReferenceRepository referenceRepository;
    private final CommentRepository commentRepository;

    private final DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 보고서 생성 (초안 저장)
     */
    @Transactional
    public ReportCreateResDto createReport(ReportCreateReqDto req, Long userId) {
        Reports report = Reports.fromDto(req, userId);
        Reports saved = reportsRepository.save(report);
        return ReportCreateResDto.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .
                .status(saved.getStatus().name())
                .build();
    }

    /**
     * 보고서 수정 (Draft 상태)
     */
    @Transactional
    public ReportUpdateResDto updateReport(Long reportId, ReportUpdateReqDto req, Long userId) {
        Reports report = reportsRepository.findByIdAndStatus(reportId, ReportStatus.DRAFT)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Draft 보고서를 찾을 수 없습니다. id=" + reportId));
        if (!report.getWriterId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");
        }
        report.updateFromDto(req);
        Reports updated = reportsRepository.save(report);
        return ReportUpdateResDto.builder()
                .id(updated.getId())
                .title(updated.getTitle())
                .status(updated.getStatus().name())
                .build();
    }

    /**
     * 보고서 목록 조회
     */
    public ReportListResDto getReports(String role, ReportStatus status, String keyword,
                                       int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Reports> pr;
        if ("writer".equalsIgnoreCase(role)) {
            pr = (status != null)
                    ? reportsRepository.findByWriterIdAndStatus(userId, status, pageable)
                    : reportsRepository.findByWriterId(userId, pageable);
        } else if ("approver".equalsIgnoreCase(role)) {
            pr = reportsRepository.findByApproverId(userId, pageable);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "role은 writer 또는 approver만 가능합니다.");
        }
        List<ReportListResDto.ReportSimpleDto> simples = pr.getContent().stream()
                .filter(r -> keyword == null || r.getTitle().contains(keyword)
                        || r.getContent().contains(keyword))
                .map(r -> ReportListResDto.ReportSimpleDto.builder()
                        .id(r.getId())
                        .title(r.getTitle())
                        .writerName(String.valueOf(r.getWriterId()))
                        .createdAt(r.getCreatedAt().format(fmt))
                        .status(r.getStatus().name())
                        .currentApprover(String.valueOf(r.getCurrentApproverId()))
                        .build())
                .collect(Collectors.toList());
        return ReportListResDto.builder()
                .reports(simples)
                .totalPages(pr.getTotalPages())
                .totalElements(pr.getTotalElements())
                .size(pr.getSize())
                .number(pr.getNumber())
                .build();
    }

    /**
     * 보고서 상세 조회
     */
    public ReportDetailResDto getReportDetail(Long reportId, Long userId) {
        Reports r = reportsRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다. id=" + reportId));
        boolean writer = r.getWriterId().equals(userId);
        boolean approver = r.getApprovalLines().stream()
                .anyMatch(l -> l.getEmployeeId().equals(userId));
        if (!writer && !approver) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "조회 권한이 없습니다.");
        }
        List<ReportDetailResDto.AttachmentResDto> atts = r.getAttachments().stream()
                .map(a -> ReportDetailResDto.AttachmentResDto.builder()
                        .fileName(a.getName())
                        .url(a.getUrl())
                        .build())
                .collect(Collectors.toList());
        List<ReportDetailResDto.ApprovalLineResDto> lines = r.getApprovalLines().stream()
                .map(l -> ReportDetailResDto.ApprovalLineResDto.builder()
                        .employeeId(l.getEmployeeId())
                        .name(String.valueOf(l.getEmployeeId()))
                        .status(l.getStatus().name())
                        .order(l.getApprovalOrder())
                        .approvedAt(l.getProcessedAt() != null ? l.getProcessedAt().format(fmt) : null)
                        .build())
                .collect(Collectors.toList());
        return ReportDetailResDto.builder()
                .id(r.getId())
                .title(r.getTitle())
                .content(r.getContent())
                .attachments(atts)
                .writer(ReportDetailResDto.WriterInfoDto.builder()
                        .id(r.getWriterId())
                        .name(String.valueOf(r.getWriterId()))
                        .build())
                .createdAt(r.getCreatedAt().format(fmt))
                .status(r.getStatus().name())
                .approvalLine(lines)
                .currentApprover(String.valueOf(r.getCurrentApproverId()))
                .dueDate(null)
                .build();
    }

    /**
     * 결재 처리 (Approve/Reject)
     */
    @Transactional
    public ApprovalProcessResDto processApproval(Long reportId, Long userId, ApprovalProcessReqDto req) {
        ApprovalLine line = approvalRepository
                .findByReports_IdAndEmployeeIdAndStatus(reportId, userId, ApprovalStatus.PENDING)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "결재 권한이 없습니다."));
        line.process(req.getAction(), req.getComment());
        line.setProcessedAt(LocalDateTime.now());
        approvalRepository.save(line);
        Reports report = line.getReports();
        report.moveToNextOrComplete(line);
        reportsRepository.save(report);
        return ApprovalProcessResDto.builder()
                .reportId(reportId)
                .action(req.getAction())
                .status(report.getStatus().name())
                .nextApprover(report.getCurrentApproverId() != null ?
                        String.valueOf(report.getCurrentApproverId()) : null)
                .build();
    }

    /**
     * 보고서 회수 처리
     */
    @Transactional
    public ReportRecallResDto recallReport(Long reportId, Long userId) {
        Reports report = reportsRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다. id=" + reportId));
        if (!report.getWriterId().equals(userId) || report.getStatus() != ReportStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "회수 권한이 없습니다.");
        }
        report.recall();
        Reports updated = reportsRepository.save(report);
        return ReportRecallResDto.builder()
                .id(updated.getId())
                .status(updated.getStatus().name())
                .build();
    }

    /**
     * 리마인드 전송 처리
     */
    @Transactional
    public ReportRemindResDto remindReport(Long reportId, Long userId) {
        Reports report = reportsRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다. id=" + reportId));
        if (!report.getCurrentApproverId().equals(userId) || report.getStatus() != ReportStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "리마인드 권한이 없습니다.");
        }
        report.remind();
        Reports updated = reportsRepository.save(report);
        return ReportRemindResDto.builder()
                .reportId(updated.getId())
                .remindedAt(updated.getRemindedAt())
                .reminderCount(updated.getReminderCount())
                .message("리마인더가 전송되었습니다.")
                .build();
    }

    /**
     * 재상신 처리
     */
    @Transactional
    public ResubmitResDto resubmitReport(Long reportId, Long userId, ResubmitReqDto req) {
        Reports report = reportsRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다. id=" + reportId));
        if (!report.getWriterId().equals(userId) || report.getStatus() != ReportStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "재상신 권한이 없습니다.");
        }
        report.resubmit(req.getComment());
        Reports updated = reportsRepository.save(report);
        return ResubmitResDto.builder()
                .reportId(updated.getId())
                .status(updated.getStatus().name())
                .resubmittedAt(updated.getSubmittedAt())
                .build();
    }

    /**
     * 참조자 추가 처리
     */
    @Transactional
    public ReferenceResDto addReference(Long reportId, Long userId, ReferenceReqDto req) {
        Reports report = reportsRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다. id=" + reportId));
        ReportReferences ref = ReportReferences.fromDto(report, req);
        ReportReferences saved = referenceRepository.save(ref);
        return ReferenceResDto.fromEntity(saved);
    }

    /**
     * 참조자 삭제 처리
     */
    @Transactional
    public void deleteReference(Long reportId, Long employeeId) {
        referenceRepository.deleteByReportIdAndEmployeeId(reportId, employeeId);
    }

    /**
     * 첨부파일 업로드 처리
     */
    @Transactional
    public AttachmentResDto uploadAttachment(Long reportId, AttachmentReqDto req) {
        Reports report = reportsRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다. id=" + reportId));
        ReportAttachment att = ReportAttachment.fromDto(report, req);
        ReportAttachment saved = attachmentRepository.save(att);
        return AttachmentResDto.fromEntity(saved);
    }

    /**
     * 댓글 등록 처리
     */
    @Transactional
    public CommentResDto createComment(Long reportId, Long userId, CommentReqDto req) {
        Reports report = reportsRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다. id=" + reportId));
        Comment comment = Comment.fromDto(report, userId, req);
        Comment saved = commentRepository.save(comment);
        return CommentResDto.fromEntity(saved);
    }
}
