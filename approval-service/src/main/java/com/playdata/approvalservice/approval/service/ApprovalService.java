package com.playdata.approvalservice.approval.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final ReferenceRepository referenceRepository;
    private final EmployeeFeignClient employeeFeignClient;

    private final ObjectMapper objectMapper;

    private final DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 보고서 생성 (초안 저장)
     */
    @Transactional
    public ReportCreateResDto createReport(ReportCreateReqDto req, Long writerId) {
        Reports report = Reports.fromDto(req, writerId);


        if(req.getAttachments() != null && !req.getAttachments().isEmpty()) {
            try{
                Map<String, Object> detailMap = new HashMap<>();
                detailMap.put("attachments", req.getAttachments());
                String detailJson = objectMapper.writeValueAsString(detailMap);
                report.setDetail(detailJson);
            }catch (JsonProcessingException e){
                throw new ResponseStatusException
                        (HttpStatus.BAD_REQUEST, "첨부파일 JSON 실패", e);
            }
        }
        Reports saved = reportsRepository.save(report);

        ApprovalLine firstLine = saved.getApprovalLines().stream().findFirst().orElse(null);
        Long firstApprovalId = firstLine != null ? firstLine.getId() : null;
        ApprovalStatus firstStatus = firstLine != null ? firstLine.getApprovalStatus() : null;

        return ReportCreateResDto.builder()
                .id(saved.getId())
                .writerId(saved.getWriterId())
                .reportStatus(saved.getReportStatus())
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
        Reports report = reportsRepository.findByIdAndReportStatus(reportId, ReportStatus.DRAFT)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Draft 보고서를 찾을 수 없습니다. id=" + reportId));

        if (!report.getWriterId().equals(writerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");
        }
        // 제목
        report.updateFromDto(req);

        // JSON 첨부파일
        if(req.getAttachments() != null){
            try {
                Map<String, Object> detailMap = new HashMap<>();
                detailMap.put("attachments", req.getAttachments());
                String detailJson = objectMapper.writeValueAsString(detailMap);
                report.setDetail(detailJson);
            }catch (JsonProcessingException e){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "첨부파일 JSON 실패", e);
            }
        }


        Reports updated = reportsRepository.save(report);
        return ReportUpdateResDto.builder()
                .id(updated.getId())
                .title(updated.getTitle())
                .reportStatus(updated.getReportStatus())
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
                    ? reportsRepository.findByWriterIdAndReportStatus(writerId, status, pageable)
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
                            .reportStatus(r.getReportStatus())
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

        List<ReportDetailResDto.AttachmentResDto> atts;
        try {
            if(r.getDetail() != null){
                JsonNode root = objectMapper.readTree(r.getDetail());
                JsonNode arr = root.path("attachments");
                List<AttachmentJsonReqDto> dtoList = objectMapper.convertValue(
                        arr, new TypeReference<List<AttachmentJsonReqDto>>() {});
                atts = dtoList.stream()
                        .map(a -> new ReportDetailResDto.AttachmentResDto(
                                a.getFileName(), a.getUrl()))
                        .collect(Collectors.toList());
            }
            else{
                atts = Collections.emptyList();
            }
        }catch (JsonProcessingException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "첨부파일 JSON 실패", e);
        }

        List<ReportDetailResDto.ApprovalLineResDto> lines = r.getApprovalLines().stream()
                .map(l -> {
                    String name = employeeFeignClient.getById(l.getEmployeeId())
                            .getBody().getName();
                    return ReportDetailResDto.ApprovalLineResDto.builder()
                            .employeeId(l.getEmployeeId())
                            .name(name)
                            .approvalStatus(l.getApprovalStatus())
                            .order(l.getApprovalContext())
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
                .reportStatus(r.getReportStatus())
                .approvalLine(lines)
                .currentApprover(currentApprover)
                .dueDate(null)
                .build();
    }

    /**
     * 결재 처리 (Approve/Rejected)
     */
    @Transactional
    public ApprovalProcessResDto processApproval(Long reportId, Long writerId, ApprovalProcessReqDto req) {


        Reports submit = reportsRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다"));

        ApprovalLine currentline = approvalRepository
                .findByReportsIdAndEmployeeIdAndApprovalStatus(reportId, writerId, ApprovalStatus.PENDING)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "결재 권한이 없거나, 이미 처리된 결재입니다."));



        // ② action에 따라 approve/rejected 호출 (approvalDateTime, approvalComment가 세팅됨)
        if (req.getApprovalStatus() == ApprovalStatus.APPROVED) {
            currentline.approve(req.getComment());
        } else if (req.getApprovalStatus() == ApprovalStatus.REJECTED) {
            currentline.rejected(req.getComment());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "알 수 없는 action 입니다.");
        }

        approvalRepository.save(currentline);

        // 보고서 상태 이동
        Reports report = currentline.getReports();
        ApprovalStatus approvalLine = currentline.getApprovalStatus();

        report.moveToNextOrComplete(currentline);
        reportsRepository.save(report);

        // 다음 결재자 이름 조회
        String nextName = report.getCurrentApproverId() != null
                ? employeeFeignClient.getById(report.getCurrentApproverId())
                .getBody().getName()
                : null;

        // DTO 반환
        return ApprovalProcessResDto.builder()
                .reportId(reportId)
                .approvalStatus(approvalLine)
                .reportStatus(report.getReportStatus())
                .nextApprover(nextName)
                .build();
    }

    /**
     * 결재 이력/상세 확인
     */
    @Transactional(readOnly = true)
    public List<ApprovalHistoryResDto> getApprovalHistory(Long reportId, Long writerId) {

        Reports report = reportsRepository.findById(reportId).orElseThrow
                (() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다. id=" + reportId));

        boolean isWriter = report.getWriterId().equals(writerId);
        boolean isApprover = report.getApprovalLines().stream()
                .anyMatch(l -> l.getEmployeeId().equals(writerId));
        if (!isWriter && !isApprover) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "조회 권한이 없습니다.");
        }


        List<ApprovalLine> lines = approvalRepository
                .findApprovalLinesByReportId(reportId);

        if (lines.isEmpty()) {
            return Collections.emptyList();
        }

        // 3) 사번에 대한 이름을 한 번에 조회 (Feign 배치 API 필요)
        List<Long> employeeIds = lines.stream()
                .map(ApprovalLine::getEmployeeId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> nameMap =
                employeeFeignClient.getEmployeeNamesByEmployeeIds(employeeIds);

        // 4) DTO로 변환
        return lines.stream()
                .map(line -> ApprovalHistoryResDto.builder()
                        .order(line.getApprovalContext())
                        .employeeId(line.getEmployeeId())
                        .employeeName(nameMap.get(line.getEmployeeId()))
                        .approvalStatus(line.getApprovalStatus())
                        .comment(line.getApprovalComment())
                        .approvalDateTime(line.getApprovalDateTime())
                        .build()
                )
                .collect(Collectors.toList());
    }


    /**
     * 보고서 회수 처리
     */
    @Transactional
    public ReportRecallResDto recallReport(Long reportId, Long writerId) {
        Reports report = reportsRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다. id=" + reportId));

        System.out.println("요청된 보고서 ID: " + reportId);
        System.out.println("토큰에서 추출한 작성자 ID (userInfo): " + writerId);
        System.out.println("보고서 엔티티의 작성자 ID: " + report.getWriterId());
        System.out.println("보고서 현재 상태: " + report.getReportStatus());
        // --- 로그 추가 끝 ---

        if (!report.getWriterId().equals(writerId) || report.getReportStatus() != ReportStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "회수 권한이 없습니다.");
        }
        report.recall();
        Reports updated = reportsRepository.save(report);
        return ReportRecallResDto.builder()
                .id(updated.getId())
                .reportStatus(updated.getReportStatus())
                .build();
    }


    /**
     * 보고서 재상신
     */
    @Transactional
    public ResubmitResDto resubmit(Long originalReportId, Long writerId, ResubmitReqDto req) {

        // 1. 원본 보고서를 찾고, 권한을 확인합니다. (반려 상태의 보고서만 재상신 가능)
        Reports originalReport = reportsRepository.findById(originalReportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다."));

        if (!originalReport.getWriterId().equals(writerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "재상신 권한이 없습니다.");
        }
        if (originalReport.getReportStatus() != ReportStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "반려된 보고서만 재상신할 수 있습니다.");
        }



        // 2) 새로운 라인 리스트와 제목·본문을 넘기도록 변경
        Reports newReport = originalReport.resubmit(
                req.getNewTitle(),
                req.getNewContent(),
                req.getApprovalLine(),
                req.getAttachments()
        );

        // 새 첨부파일이 있으면 JSON으로 detail에 덮어쓰기
        if (req.getAttachments() != null && !req.getAttachments().isEmpty()) {
            try {
                Map<String,Object> detailMap = new HashMap<>();
                detailMap.put("attachments", req.getAttachments());
                String detailJson = objectMapper.writeValueAsString(detailMap);
                newReport.setDetail(detailJson);
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "첨부파일 JSON 직렬화 실패", e);
            }
        }


        // 3. 새로운 보고서를 저장합니다. (cascade 설정으로 결재라인도 함께 저장됨)
        Reports savedNewReport = reportsRepository.save(newReport);

        // 4. 원본 보고서의 상태를 변경하여 더 이상 유효하지 않음을 표시합니다.
        originalReport.markAsResubmitted();
        reportsRepository.save(originalReport);

        // 5. 응답 DTO를 반환합니다. (새로 생성된 reportId를 반환)
        return ResubmitResDto.builder()
                .reportId(savedNewReport.getId()) // 새로 생성된 ID
                .reportStatus(savedNewReport.getReportStatus())
                .resubmittedAt(savedNewReport.getSubmittedAt())
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
        if (!report.getCurrentApproverId().equals(writerId) || report.getReportStatus() != ReportStatus.IN_PROGRESS) {
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
        referenceRepository.deleteByReportsIdAndEmployeeId(reportId, employeeId);

        // 3) 응답 DTO 반환
        return ReportReferencesResDto.builder()
                .reportId(reportId)
                .employeeId(employeeId)
                .build();
    }
}
