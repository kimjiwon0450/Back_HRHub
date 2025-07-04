package com.playdata.approvalservice.approval.service;

import com.playdata.approvalservice.approval.dto.request.*;
import com.playdata.approvalservice.approval.dto.response.*;
import com.playdata.approvalservice.approval.entity.*;
import com.playdata.approvalservice.approval.feign.EmployeeFeignClient;
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
    private final EmployeeFeignClient employeeFeignClient;

    private final DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 보고서 생성 (초안 저장)
     */
    @Transactional
    public ReportCreateResDto createReport(ReportCreateReqDto req, Long writerId) {
        Reports report = Reports.fromDto(req, writerId);
        Reports saved = reportsRepository.save(report);

        ApprovalLine firstLine = saved.getApprovalLines().stream().findFirst().orElse(null);
        Long firstApprovalId = firstLine != null ? firstLine.getId() : null;
        ApprovalStatus firstStatus = firstLine != null ? firstLine.getStatus() : null;

        return ReportCreateResDto.builder()
                .id(saved.getId())
                .writerId(saved.getWriterId())
                .status(saved.getStatus())
                .title(saved.getTitle())
                .content(saved.getContent())
                .approvalStatus(firstStatus)
                .createAt(saved.getCreatedAt()) // 만든 시각
                .submittedAt(saved.getSubmittedAt()) // 승인 시각, 날짜
                .returnAt(saved.getReturnAt()) // 반려된 날짜
                .completedAt(saved.getCompletedAt()) // 전자 결재 완료 날짜
                .approvalId(firstApprovalId) // 하나의 전자결재 고유 ID
                .reminderCount(saved.getReminderCount()) // 리마인드 카운터
                .remindedAt(saved.getRemindedAt()) // 리마인드 시각
                .build();
    }

    /**
     * 보고서 수정 (Draft 상태)
     */
    @Transactional
    public ReportUpdateResDto updateReport(Long reportId, ReportUpdateReqDto req, Long writerId) {
        Reports report = reportsRepository.findByIdAndStatus(reportId, ReportStatus.DRAFT)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Draft 보고서를 찾을 수 없습니다. id=" + reportId));
        if (!report.getWriterId().equals(writerId)) {
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
                                       int page, int size, Long writerId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Reports> pr;
        if ("writer".equalsIgnoreCase(role)) {
            pr = (status != null)
                    ? reportsRepository.findByWriterIdAndStatus(writerId, status, pageable)
                    : reportsRepository.findByWriterId(writerId, pageable);
        } else if ("approver".equalsIgnoreCase(role)) {
            pr = reportsRepository.findByApproverId(writerId, pageable);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "role은 writer 또는 approver만 가능합니다.");
        }
        List<ReportListResDto.ReportSimpleDto> simples = pr.getContent().stream()
                .filter(r -> keyword == null || r.getTitle().contains(keyword)
                        || r.getContent().contains(keyword))
                .map(r -> {
                    String writerName = employeeFeignClient.getById(r.getWriterId())
                            .getBody().getName();
                    String approverName = r.getCurrentApproverId() != null
                            ? employeeFeignClient.getById(r.getCurrentApproverId())
                            .getBody().getName()
                            : null;
                    return ReportListResDto.ReportSimpleDto.builder()
                            .id(r.getId())
                            .title(r.getTitle())
                            .name(writerName)
                            .createdAt(r.getCreatedAt().format(fmt))
                            .status(r.getStatus().name())
                            .currentApprover(approverName)
                            .build();
                })
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
    public ReportDetailResDto getReportDetail(Long reportId, Long writerId) {
        Reports r = reportsRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다. id=" + reportId));
        boolean writer = r.getWriterId().equals(writerId);
        boolean approver = r.getApprovalLines().stream()
                .anyMatch(l -> l.getEmployeeId().equals(writerId));
        if (!writer && !approver) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "조회 권한이 없습니다.");
        }
        String writerName = employeeFeignClient.getById(r.getWriterId())
                .getBody().getName();
        List<ReportDetailResDto.AttachmentResDto> atts = r.getAttachments().stream()
                .map(a -> ReportDetailResDto.AttachmentResDto.builder()
                        .fileName(a.getName())
                        .url(a.getUrl())
                        .build())
                .collect(Collectors.toList());
        List<ReportDetailResDto.ApprovalLineResDto> lines = r.getApprovalLines().stream()
                .map(l -> {
                    String name = employeeFeignClient.getById(l.getEmployeeId())
                            .getBody().getName();
                    return ReportDetailResDto.ApprovalLineResDto.builder()
                            .employeeId(l.getEmployeeId())
                            .name(name)
                            .status(l.getStatus().name())
                            .order(l.getApprovalOrder())
                            .approvedAt(l.getApprovalDateTime() != null
                                    ? l.getApprovalDateTime().format(fmt) : null)
                            .build();
                })
                .collect(Collectors.toList());
        String currentApprover = r.getCurrentApproverId() != null
                ? employeeFeignClient.getById(r.getCurrentApproverId())
                .getBody().getName()
                : null;
        return ReportDetailResDto.builder()
                .id(r.getId())
                .title(r.getTitle())
                .content(r.getContent())
                .attachments(atts)
                .writer(ReportDetailResDto.WriterInfoDto.builder()
                        .id(r.getWriterId())
                        .name(writerName)
                        .build())
                .createdAt(r.getCreatedAt().format(fmt))
                .status(r.getStatus().name())
                .approvalLine(lines)
                .currentApprover(currentApprover)
                .dueDate(null)
                .build();
    }

    /**
     * 결재 처리 (Approve/Reject)
     */
    @Transactional
    public ApprovalProcessResDto processApproval(Long reportId, Long writerId, ApprovalProcessReqDto req) {

        ApprovalLine line = approvalRepository
                .findByReportsIdAndEmployeeIdAndStatus(reportId, writerId, ApprovalStatus.PENDING)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "결재 권한이 없습니다."));
        // ② action에 따라 approve/reject 호출 (approvalDateTime, approvalComment가 세팅됨)
        if ("APPROVE".equalsIgnoreCase(req.getAction())) {
            line.approve(req.getComment());
        } else if ("REJECT".equalsIgnoreCase(req.getAction())) {
            line.reject(req.getComment());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "알 수 없는 action 입니다.");
        }

        // ③ 변경된 라인 저장
        approvalRepository.save(line);
        // ④ 보고서 상태 이동
        Reports report = line.getReports();
        report.moveToNextOrComplete(line);
        reportsRepository.save(report);

        // ⑤ 다음 결재자 이름 조회 (Fei​gn)
        String nextName = report.getCurrentApproverId() != null
                ? employeeFeignClient.getById(report.getCurrentApproverId())
                .getBody().getName()
                : null;

        // ⑥ DTO 반환
        return ApprovalProcessResDto.builder()
                .reportId(reportId)
                .action(req.getAction())
                .status(report.getStatus().name())
                .nextApprover(nextName)
                .build();
    }

    /**
     * 보고서 회수 처리
     */
    @Transactional
    public ReportRecallResDto recallReport(Long reportId, Long writerId) {
        Reports report = reportsRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다. id=" + reportId));
        if (!report.getWriterId().equals(writerId) || report.getStatus() != ReportStatus.IN_PROGRESS) {
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
    public ReportRemindResDto remindReport(Long reportId, Long writerId) {
        Reports report = reportsRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다. id=" + reportId));
        if (!report.getCurrentApproverId().equals(writerId) || report.getStatus() != ReportStatus.IN_PROGRESS) {
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
    public ResubmitResDto resubmitReport(Long reportId, Long writerId, ResubmitReqDto req) {
        Reports report = reportsRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다. id=" + reportId));
        if (!report.getWriterId().equals(writerId) || report.getStatus() != ReportStatus.REJECTED) {
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
    public ReferenceResDto addReference(Long reportId, Long writerId, ReferenceReqDto req) {
        Reports report = reportsRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다. id=" + reportId));
        ReportReferences ref = ReportReferences.fromReferenceReqDto(report, req);
        ReportReferences saved = referenceRepository.save(ref);
        return ReferenceResDto.fromReportReferences(saved);
    }

    /**
     * 참조자 제거 처리
     */
    @Transactional
    public ReportReferencesResDto deleteReferences(Long reportId, Long writerId, Long employeeId) {
        // 1) 보고서 존재 및 권한 확인 (작성자만 삭제 가능)
        Reports report = reportsRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다. id=" + reportId));
        if (!report.getWriterId().equals(writerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "참조자 삭제 권한이 없습니다.");
        }

        // 2) 참조 삭제
        referenceRepository.deleteByReports_IdAndEmployeeId(reportId, employeeId);

        // 3) 응답 DTO 반환
        return ReportReferencesResDto.builder()
                .reportId(reportId)
                .employeeId(employeeId)
                .build();
    }

    /**
     * 첨부파일 업로드 처리
     */
    @Transactional
    public AttachmentResDto uploadAttachment(Long reportId, AttachmentReqDto req) {
        Reports report = reportsRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다. id=" + reportId));
        ReportAttachment att = ReportAttachment.fromAttachmentReqDto(report, req);
        ReportAttachment saved = attachmentRepository.save(att);
        return AttachmentResDto.fromReportAttachment(saved);
    }
}
