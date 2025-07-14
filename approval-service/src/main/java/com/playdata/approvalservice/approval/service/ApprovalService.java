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
import com.playdata.approvalservice.common.config.AwsS3Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    private final AwsS3Config awsS3Config;

    private final ObjectMapper objectMapper;

    private final DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 보고서 생성 (초안 저장)
     */
    @Transactional
    public ReportCreateResDto createReport(
            ReportCreateReqDto req,
            Long writerId,
            List<MultipartFile> files
    ) {

        Reports report = Reports.fromDto(req, writerId);

        List<AttachmentJsonReqDto> attachments = new ArrayList<>();

        if (files != null) {
            for (MultipartFile file : files) {
                try {
                    String key = UUID.randomUUID() + "_" + file.getOriginalFilename();
                    byte[] data = file.getBytes();
                    String url = awsS3Config.uploadToS3Bucket(data, key);
                    attachments.add(new AttachmentJsonReqDto(
                            file.getOriginalFilename(),
                            url
                    ));
                } catch (IOException e) {
                    log.error("S3 업로드 실패: {}", file.getOriginalFilename(), e);
                    throw new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "파일 업로드에 실패했습니다: " + file.getOriginalFilename(), e);

                }
            }
        }

            Map<String, Object> detailMap = new HashMap<>();
            if (!attachments.isEmpty()) {
                detailMap.put("attachments", attachments);
            }
            if (req.getReferences() != null && !req.getReferences().isEmpty()) {
                detailMap.put("references", req.getReferences());
            }
            if (!detailMap.isEmpty()) {
                try {
                    String detailJson = objectMapper.writeValueAsString(detailMap);
                    log.debug(" → 직렬화된 detail JSON: {}", detailJson);
                    report.setDetail(objectMapper.writeValueAsString(detailMap));
                } catch (JsonProcessingException e) {
                    log.error("detail JSON 생성 실패", e);
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "detail JSON 생성 실패", e);
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
                    .reportCreateAt(saved.getReportCreatedAt()) // 만든 시각
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
        public ReportUpdateResDto updateReport (Long reportId, ReportUpdateReqDto req, Long writerId){
            Reports report = reportsRepository.findByIdAndReportStatus(reportId, ReportStatus.DRAFT)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Draft 보고서를 찾을 수 없습니다. id=" + reportId));

            if (!report.getWriterId().equals(writerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");
            }
            // 제목
            report.updateFromDto(req);

            // detail 에 attachments, references 담기
            Map<String, Object> detailMap = new HashMap<>();
            if (req.getAttachments() != null) {
                detailMap.put("attachments", req.getAttachments());
            }
            if (req.getReferences() != null) {
                detailMap.put("references", req.getReferences());
            }
            if (!detailMap.isEmpty()) {
                try {
                    report.setDetail(objectMapper.writeValueAsString(detailMap));
                } catch (JsonProcessingException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "detail JSON 생성 실패", e);
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
     * 보고서 생성과 동시에 상신 (IN_PROGRESS)
     * @param req
     * @param writerId
     * @param files
     * @return
     */
    @Transactional
        public ReportCreateResDto progressReport(
                ReportCreateReqDto req,
                Long writerId,
                List<MultipartFile> files
        ){
        Reports report = Reports.fromDtoForInProgress(req, writerId);
        List<AttachmentJsonReqDto> attachments = new ArrayList<>();

        if (files != null) {
            for (MultipartFile file : files) {
                try {
                    String key = UUID.randomUUID() + "_" + file.getOriginalFilename();
                    byte[] data = file.getBytes();
                    String url = awsS3Config.uploadToS3Bucket(data, key);
                    attachments.add(new AttachmentJsonReqDto(
                            file.getOriginalFilename(),
                            url
                    ));
                } catch (IOException e) {
                    log.error("S3 업로드 실패: {}", file.getOriginalFilename(), e);
                    throw new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "파일 업로드에 실패했습니다: " + file.getOriginalFilename(), e);

                }
            }
        }

        Map<String, Object> detailMap = new HashMap<>();
        if (!attachments.isEmpty()) {
            detailMap.put("attachments", attachments);
        }
        if (req.getReferences() != null && !req.getReferences().isEmpty()) {
            detailMap.put("references", req.getReferences());
        }
        if (!detailMap.isEmpty()) {
            try {
                String detailJson = objectMapper.writeValueAsString(detailMap);
                log.debug(" → 직렬화된 detail JSON: {}", detailJson);
                report.setDetail(objectMapper.writeValueAsString(detailMap));
            } catch (JsonProcessingException e) {
                log.error("detail JSON 생성 실패", e);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "detail JSON 생성 실패", e);
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
                .reportCreateAt(saved.getReportCreatedAt()) // 만든 시각
                .submittedAt(saved.getSubmittedAt()) // 승인 시각, 날짜
                .returnAt(saved.getReturnAt()) // 반려된 날짜
                .completedAt(saved.getCompletedAt()) // 전자 결재 완료 날짜
                .approvalId(firstApprovalId) // 하나의 전자결재 고유 ID
                .reminderCount(saved.getReminderCount()) // 리마인드 카운터
                .remindedAt(saved.getRemindedAt()) // 리마인드 시각
                .build();
        }

        /**
         * 보고서 목록 조회
         */
        public ReportListResDto getReports (String role, ReportStatus status, String keyword,
                int page, int size, Long writerId){
            Pageable pageable = PageRequest.of(page, size);

            Page<Reports> pr;

            if ("writer".equalsIgnoreCase(role)) {
                pr = (status != null)
                        ? reportsRepository.findByWriterIdAndReportStatus(writerId, status, pageable)
                        : reportsRepository.findByWriterId(writerId, pageable);
            } else if ("approver".equalsIgnoreCase(role)) {
                if(status == ReportStatus.IN_PROGRESS) {
                    pr = reportsRepository.findByCurrentApproverIdAndReportStatus(writerId, ReportStatus.IN_PROGRESS, pageable);
                }
                else {
                    pr = reportsRepository.findByApproverIdAndExcludeDraftRecalled(writerId, pageable);
                }
            } else if ("reference".equalsIgnoreCase(role)) {
                // 이제 이 메서드는 정렬 정보가 담긴 Pageable 객체를 받아
                // JPA가 최종 SQL을 만들 때 ORDER BY 절을 자동으로 추가해줍니다.
                pr = reportsRepository.findByReferenceEmployeeIdInDetailJsonAndExcludeDraftRecalled(writerId, pageable);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "role은 writer, approver, 또는 reference만 가능합니다.");
            }

            Set<Long> employeeIdsToFetch = new HashSet<>();
            pr.getContent().forEach(report -> {
                employeeIdsToFetch.add(report.getWriterId());
                if(report.getCurrentApproverId() != null){
                    employeeIdsToFetch.add(report.getCurrentApproverId());
                }
                report.getApprovalLines().forEach(line -> employeeIdsToFetch.add(line.getEmployeeId()));
            });

            Map<Long, String> employeeNamesMap = Collections.emptyMap();

            if(!employeeIdsToFetch.isEmpty()){
                try {
                    ResponseEntity<Map<Long,String>> response = employeeFeignClient.getEmployeeNamesByEmployeeIds(new ArrayList<>(employeeIdsToFetch));
                    employeeNamesMap = Optional.ofNullable(response.getBody()).orElse(Collections.emptyMap());
                    log.info("Successfully fetched {} employee names.", employeeNamesMap.size());
                } catch (Exception e) {
                    log.error("Error fetching employee names from hr-service", e);
                    employeeNamesMap = Collections.emptyMap();
                }
            }

            final Map<Long, String> finalEmployeeNamesMap = employeeNamesMap;

            List<ReportListResDto.ReportSimpleDto> simples = pr.getContent().stream()
                    .filter(r -> keyword == null || r.getTitle().contains(keyword)
                            || r.getContent().contains(keyword))
                    .map(r -> {
                        String writerName = finalEmployeeNamesMap.getOrDefault(r.getWriterId(), "알 수 없는 사용자");
                        String approverName = r.getCurrentApproverId() != null
                                ? finalEmployeeNamesMap.getOrDefault(r.getCurrentApproverId(), "알 수 없는 사용자")
                                : null;
                        // 각 보고서의 결재선 정보를 DTO 리스트로 만듭니다.
                        List<ReportListResDto.ApprovalLineSimpleDto> approvalLineSimpleDtos = r.getApprovalLines().stream()
                                .map(line -> ReportListResDto.ApprovalLineSimpleDto.builder()
                                        .employeeId(line.getEmployeeId())
                                        .employeeName(finalEmployeeNamesMap.getOrDefault(line.getEmployeeId(), "알 수 없는 사용자"))
                                        .approvalStatus(line.getApprovalStatus())
                                        .build())
                                .collect(Collectors.toList());

                        return ReportListResDto.ReportSimpleDto.builder()
                                .id(r.getId())
                                .title(r.getTitle())
                                .name(writerName)
                                .reportCreatedAt(r.getReportCreatedAt().format(fmt))
                                .reportStatus(r.getReportStatus())
                                .currentApprover(approverName)
                                .approvalLine(approvalLineSimpleDtos)
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
     * 보고서 상세 조회 (N+1 문제 해결을 위해 리팩토링)
     * @param reportId 조회할 보고서의 ID
     * @param writerId 현재 사용자의 직원 ID (권한 확인용)
     * @return 보고서 상세 정보 DTO
     */
    public ReportDetailResDto getReportDetail(Long reportId, Long writerId) {

        // 1. 보고서 엔티티를 조회합니다.
        Reports report = reportsRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다. id=" + reportId));

        // 2. 조회 권한을 확인합니다. (작성자 또는 결재 라인에 포함된 사람만 조회 가능)
        boolean isWriter = report.getWriterId().equals(writerId);
        boolean isApprover = report.getApprovalLines().stream()
                .anyMatch(l -> l.getEmployeeId().equals(writerId));

        boolean isReference = false;
        if (report.getDetail() != null && !report.getDetail().isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(report.getDetail());
                JsonNode referencesNode = root.path("references");
                if (referencesNode.isArray()) {
                    for (JsonNode refNode : referencesNode) {
                        if (refNode.path("employeeId").asLong() == writerId) {
                            isReference = true;
                            break;
                        }
                    }
                }
            } catch (JsonProcessingException e) {
                log.error("getApprovalHistory 권한 체크 중 detail JSON 파싱 실패, reportId: {}", reportId, e);
            }
        }

        if (!isWriter && !isApprover && !isReference) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "조회 권한이 없습니다.");
        }

        // 3. API 호출을 위한 모든 관련 직원 ID를 수집합니다.
        Set<Long> employeeIdsToFetch = new HashSet<>();

        employeeIdsToFetch.add(report.getWriterId()); // 작성자 ID
        if (report.getCurrentApproverId() != null) {
            employeeIdsToFetch.add(report.getCurrentApproverId()); // 현재 결재자 ID
        }
        report.getApprovalLines().forEach(line -> employeeIdsToFetch.add(line.getEmployeeId())); // 결재 라인의 모든 직원 ID

        // 4. 단 한 번의 Feign API 호출로 모든 직원 이름을 가져옵니다.
        Map<Long, String> employeeNamesMap = Collections.emptyMap();
        if (!employeeIdsToFetch.isEmpty()) {
            ResponseEntity<Map<Long, String>> response = employeeFeignClient.getEmployeeNamesByEmployeeIds(new ArrayList<>(employeeIdsToFetch));
            employeeNamesMap = Optional.ofNullable(response.getBody()).orElse(Collections.emptyMap());
        }
        final Map<Long, String> finalEmployeeNamesMap = employeeNamesMap;

        // 5. 첨부파일 및 참조 정보 파싱 (JSON)
        List<ReportDetailResDto.AttachmentResDto> atts = Collections.emptyList();
        List<ReportDetailResDto.ReferenceJsonResDto> refs = Collections.emptyList();
        if (report.getDetail() != null && !report.getDetail().isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(report.getDetail());
                // 첨부파일 파싱
                atts = Optional.of(root.path("attachments"))
                        .filter(JsonNode::isArray)
                        .map(arr -> objectMapper.convertValue(arr, new TypeReference<List<AttachmentJsonReqDto>>() {}))
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(a -> new ReportDetailResDto.AttachmentResDto(a.getFileName(), a.getUrl()))
                        .collect(Collectors.toList());
                // 참조자 파싱
                refs = Optional.of(root.path("references"))
                        .filter(JsonNode::isArray)
                        .map(arr -> objectMapper.convertValue(arr, new TypeReference<List<ReferenceJsonReqDto>>() {}))
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(rj -> new ReportDetailResDto.ReferenceJsonResDto(rj.getEmployeeId()))
                        .collect(Collectors.toList());
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "detail JSON 파싱 실패", e);
            }
        }

        // 6. 미리 가져온 이름 맵을 사용하여 결재 라인 DTO를 생성합니다.
        List<ReportDetailResDto.ApprovalLineResDto> lines = report.getApprovalLines().stream()
                .map(l -> {
                    String name = finalEmployeeNamesMap.getOrDefault(l.getEmployeeId(), "알 수 없는 사용자");
                    return ReportDetailResDto.ApprovalLineResDto.builder()
                            .employeeId(l.getEmployeeId())
                            .name(name)
                            .approvalStatus(l.getApprovalStatus())
                            .context(l.getApprovalContext())
                            .approvedAt(l.getApprovalDateTime() != null ? l.getApprovalDateTime().format(fmt) : null)
                            .build();
                })
                .collect(Collectors.toList());

        // 7. 작성자 및 현재 결재자 이름을 Map에서 가져옵니다.
        String writerName = finalEmployeeNamesMap.getOrDefault(report.getWriterId(), "알 수 없는 사용자");
        String currentApprover = report.getCurrentApproverId() != null
                ? finalEmployeeNamesMap.getOrDefault(report.getCurrentApproverId(), "알 수 없는 사용자")
                : null;

        // 8. 최종 상세 정보 DTO를 빌드하여 반환합니다.
        ReportDetailResDto resultDto = ReportDetailResDto.builder()
                .id(report.getId())
                .title(report.getTitle())
                .content(report.getContent())
                .attachments(atts)
                .references(refs)
                .writer(ReportDetailResDto.WriterInfoDto.builder()
                        .id(report.getWriterId())
                        .name(writerName)
                        .build())
                .reportCreatedAt(report.getReportCreatedAt().format(fmt))
                .reportStatus(report.getReportStatus())
                .approvalLine(lines)
                .currentApprover(currentApprover)
                .dueDate(null) // 필요 시 구현
                .build();

        if (!resultDto.getApprovalLine().isEmpty()) {
            log.info("First approver in DTO: {}", resultDto.getApprovalLine().get(0).toString());
        }

        return resultDto;
    }

        /**
         * 결재 처리 (Approve/Rejected)
         */
        @Transactional
        public ApprovalProcessResDto processApproval (Long reportId, Long writerId, ApprovalProcessReqDto req){


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
        public List<ApprovalHistoryResDto> getApprovalHistory (Long reportId, Long writerId){

            Reports report = reportsRepository.findById(reportId).orElseThrow
                    (() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다. id=" + reportId));

            boolean isWriter = report.getWriterId().equals(writerId);
            boolean isApprover = report.getApprovalLines().stream()
                    .anyMatch(l -> l.getEmployeeId().equals(writerId));

            boolean isReference = false;
            if (report.getDetail() != null && !report.getDetail().isBlank()) {
                try {
                    JsonNode root = objectMapper.readTree(report.getDetail());
                    JsonNode referencesNode = root.path("references");
                    if (referencesNode.isArray()) {
                        for (JsonNode refNode : referencesNode) {
                            if (refNode.path("employeeId").asLong() == writerId) {
                                isReference = true;
                                break;
                            }
                        }
                    }
                } catch (JsonProcessingException e) {
                    log.error("getApprovalHistory 권한 체크 중 detail JSON 파싱 실패, reportId: {}", reportId, e);
                }
            }

            if (!isWriter && !isApprover && !isReference) {
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

            // Feign 클라이언트의 반환 타입이 ResponseEntity<Map>이므로 .getBody()를 호출해야 합니다.
            ResponseEntity<Map<Long, String>> response = employeeFeignClient.getEmployeeNamesByEmployeeIds(employeeIds);
            Map<Long, String> nameMap = Optional.ofNullable(response.getBody()).orElse(Collections.emptyMap());

            // 4) DTO로 변환
            return lines.stream()
                    .map(line -> ApprovalHistoryResDto.builder()
                            .context(line.getApprovalContext())
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
        public ReportRecallResDto recallReport (Long reportId, Long writerId){
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
        public ResubmitResDto resubmit (Long originalReportId, Long writerId, ResubmitReqDto req){

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

            // 2) attachments/references 덮어쓰기
            Map<String, Object> detailMap = new HashMap<>();
            if (req.getAttachments() != null && !req.getAttachments().isEmpty()) {
                detailMap.put("attachments", req.getAttachments());
            }
            if (req.getReferences() != null && !req.getReferences().isEmpty()) {
                detailMap.put("references", req.getReferences());
            }
            if (!detailMap.isEmpty()) {
                try {
                    newReport.setDetail(objectMapper.writeValueAsString(detailMap));
                } catch (JsonProcessingException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "detail JSON 직렬화 실패", e);
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
        public ReportRemindResDto remindReport (Long reportId, Long writerId){
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
        public ReferenceResDto addReference (Long reportId, Long writerId, ReferenceReqDto req){
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
        public ReportReferencesResDto deleteReferences (Long reportId, Long writerId, Long employeeId){
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
