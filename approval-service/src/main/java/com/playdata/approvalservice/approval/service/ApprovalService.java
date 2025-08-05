package com.playdata.approvalservice.approval.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playdata.approvalservice.approval.dto.request.*;
import com.playdata.approvalservice.approval.dto.request.template.ReportFromTemplateReqDto;
import com.playdata.approvalservice.approval.dto.response.*;
import com.playdata.approvalservice.approval.dto.response.template.ReportFormResDto;
import com.playdata.approvalservice.approval.entity.*;
import com.playdata.approvalservice.approval.feign.EmployeeFeignClient;
import com.playdata.approvalservice.approval.repository.*;
import com.playdata.approvalservice.common.config.AwsS3Config;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;


import java.io.IOException;
import java.net.URL;
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
    private final ReportTemplateRepository templateRepository;
    private final AwsS3Config awsS3Config;
    private final S3Service s3Service;

    private final ObjectMapper objectMapper;

    private final DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 보고서 생성 (초안 저장)
     */
    @Transactional
    public ReportCreateResDto createReport(
            ReportSaveReqDto req,
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
                .build();
    }

    /**
     * 보고서 수정 (Draft 상태)
     */
    @Transactional
    public ReportDetailResDto updateReport(Long reportId, ReportUpdateReqDto req, Long writerId, List<MultipartFile> newFiles) {
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

        // 수정된 보고서 저장
        Reports updated = reportsRepository.save(report);

        return getReportDetail(updated.getId(), writerId);
    }

    /**
     * 보고서 생성과 동시에 상신 (IN_PROGRESS)
     *
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
    ) {
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
                .build();
    }

    /**
     * 보고서 생성과 동시에 예약 상신 (SCHEDULED)
     *
     * @param req      예약 정보가 포함된 요청 DTO
     * @param writerId 작성자 ID
     * @param files    첨부 파일
     * @return 생성된 보고서 정보
     */
    @Transactional
    public ReportCreateResDto scheduleReport(
            @Valid ReportCreateReqDto req, // ★ 새로운 DTO를 사용하는 것이 좋습니다. (아래 설명 참고)
            Long writerId,
            List<MultipartFile> files
    ) {
        // 1. DTO를 Reports 엔티티로 변환 (예약용 fromDto 메소드 필요)
        Reports report = Reports.fromDtoForScheduled(req, writerId);

        // 2. 첨부 파일 처리 로직 (progressReport 메소드와 동일)
        List<AttachmentJsonReqDto> attachments = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                try {
                    String key = UUID.randomUUID() + "_" + file.getOriginalFilename();
                    byte[] data = file.getBytes();
                    String url = awsS3Config.uploadToS3Bucket(data, key);
                    attachments.add(new AttachmentJsonReqDto(file.getOriginalFilename(), url));
                } catch (IOException e) {
                    log.error("S3 업로드 실패: {}", file.getOriginalFilename(), e);
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드 실패: " + file.getOriginalFilename());
                }
            }
        }

        // 3. detail JSON 처리 로직 (progressReport 메소드와 동일)
        Map<String, Object> detailMap = new HashMap<>();
        if (!attachments.isEmpty()) {
            detailMap.put("attachments", attachments);
        }
        if (req.getReferences() != null && !req.getReferences().isEmpty()) {
            detailMap.put("references", req.getReferences());
        }
        if (!detailMap.isEmpty()) {
            try {
                report.setDetail(objectMapper.writeValueAsString(detailMap));
            } catch (JsonProcessingException e) {
                log.error("detail JSON 생성 실패", e);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "detail JSON 생성 실패");
            }
        }

        // 4. 예약 정보 설정 (가장 중요한 부분)
        report.schedule(req.getScheduledAt());

        // 5. DB에 저장
        Reports saved = reportsRepository.save(report);

        // 6. 응답 DTO 생성 (progressReport 메소드와 동일)
        ApprovalLine firstLine = saved.getApprovalLines().stream().findFirst().orElse(null);
        Long firstApprovalId = firstLine != null ? firstLine.getId() : null;
        ApprovalStatus firstStatus = firstLine != null ? firstLine.getApprovalStatus() : null;

        return ReportCreateResDto.builder()
                .id(saved.getId())
                .writerId(saved.getWriterId())
                .reportStatus(saved.getReportStatus())
                .title(saved.getTitle())
                .content(saved.getContent())
                .approvalStatus(firstStatus) // PENDING 상태가 반환될 것
                .reportCreateAt(saved.getReportCreatedAt())
                .submittedAt(saved.getSubmittedAt()) // null 이거나 예약시간이 될 것
                .returnAt(saved.getReturnAt())
                .completedAt(saved.getCompletedAt())
                .approvalId(firstApprovalId)
                .build();
    }

    // ApprovalService.java 파일에 포함될 전체 메소드

    /**
     * 특정 사용자가 작성한 '예약된' 문서 목록을 조회합니다. (Feign Client 호출 제거 버전)
     *
     * @param writerId 조회할 사용자의 ID
     * @param page     페이지 번호
     * @param size     페이지 당 항목 수
     * @return 페이징 처리된 예약 문서 목록
     */
    public ReportListResDto getScheduledReports(Long writerId, int page, int size) {
        // 1. 정렬 기준 설정: 'scheduledAt' 기준 오름차순 (곧 실행될 예약이 위로)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "scheduledAt"));

        // 2. Repository를 호출하여 데이터 조회
        Page<Reports> scheduledReportsPage = reportsRepository.findByWriterIdAndReportStatus(
                writerId,
                ReportStatus.SCHEDULED,
                pageable
        );


        // 2. [추가] 조회된 문서들의 작성자, 결재자 ID를 모두 수집합니다.
        Set<Long> employeeIdsToFetch = new HashSet<>();
        scheduledReportsPage.getContent().forEach(report -> {
            employeeIdsToFetch.add(report.getWriterId());
            report.getApprovalLines().forEach(line -> employeeIdsToFetch.add(line.getEmployeeId()));
        });

        // 3. [추가] Feign Client를 한 번만 호출하여 모든 직원 이름을 가져옵니다.
        Map<Long, String> employeeNamesMap = Collections.emptyMap();
        if (!employeeIdsToFetch.isEmpty()) {
            try {
                ResponseEntity<Map<Long, String>> response = employeeFeignClient.getEmployeeNamesByEmployeeIds(new ArrayList<>(employeeIdsToFetch));
                employeeNamesMap = Optional.ofNullable(response.getBody()).orElse(Collections.emptyMap());
            } catch (Exception e) {
                log.error("Error fetching employee names from hr-service", e);
                // 오류 발생 시 비어있는 맵을 사용합니다.
            }
        }
        final Map<Long, String> finalEmployeeNamesMap = employeeNamesMap;

        // 4. 조회된 엔티티 목록을 DTO로 변환합니다.
        List<ReportListResDto.ReportSimpleDto> reportDtos = scheduledReportsPage.getContent().stream()
                .map(report -> {
                    // [수정] 위에서 가져온 이름 맵에서 기안자 이름을 찾습니다.
                    String writerName = finalEmployeeNamesMap.getOrDefault(report.getWriterId(), "알 수 없는 사용자");

                    // [수정] 결재선 정보를 DTO 리스트로 만듭니다.
                    List<ReportListResDto.ApprovalLineSimpleDto> approvalLineSimpleDtos = report.getApprovalLines().stream()
                            .map(line -> ReportListResDto.ApprovalLineSimpleDto.builder()
                                    .employeeId(line.getEmployeeId())
                                    .employeeName(finalEmployeeNamesMap.getOrDefault(line.getEmployeeId(), "알 수 없는 사용자"))
                                    .approvalStatus(line.getApprovalStatus())
                                    .build())
                            .collect(Collectors.toList());

                    // [수정] DTO 빌더에 모든 값을 정확히 채워 넣습니다.
                    return ReportListResDto.ReportSimpleDto.builder()
                            .id(report.getId())
                            .title(report.getTitle())
                            .name(writerName) // 수정된 부분
                            .reportCreatedAt(report.getReportCreatedAt() != null ? report.getReportCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                            .reportStatus(report.getReportStatus())
                            .currentApprover(null)
                            .approvalLine(approvalLineSimpleDtos) // 수정된 부분
                            .scheduledAt(report.getScheduledAt())
                            .build();
                })
                .collect(Collectors.toList());

        // 5. 최종적으로 페이징 정보와 함께 ReportListResDto를 만들어 반환합니다.
        return ReportListResDto.builder()
                .reports(reportDtos)
                .totalPages(scheduledReportsPage.getTotalPages())
                .totalElements(scheduledReportsPage.getTotalElements())
                .size(scheduledReportsPage.getSize())
                .number(scheduledReportsPage.getNumber())
                .build();
    }

    /**
     * 템플릿 기반 결재 문서 생성 및 상신
     */
    @Transactional
    public ReportCreateResDto reportFromTemplate(
            ReportFromTemplateReqDto req,
            String writerEmail,
            List<MultipartFile> files
    ) {
        // ... (1~3번 로직은 동일) ...
        // 1. 템플릿 조회
        ReportTemplate template = templateRepository.findById(req.getTemplateId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "템플릿을 찾을 수 없습니다."));

        // 2. 보고서 내용 생성
        String reportContent = generateContentFromTemplate(template.getTemplate(), req.getValues());

        // 3. 기존 DTO로 변환
        ReportCreateReqDto newProgressReq = new ReportCreateReqDto();
        newProgressReq.setTitle(req.getTitle());
        newProgressReq.setContent(reportContent);
        newProgressReq.setApprovalLine(req.getApprovalLine());
        newProgressReq.setReferences(req.getReferences());

        Long writerId;
        try {
            // (수정) 올바른 메소드 호출: `findIdByEmail`에 `writerEmail`을 파라미터로 전달
            ResponseEntity<Long> response = employeeFeignClient.findIdByEmail(writerEmail);

            // 응답 상태 코드 및 본문 null 체크
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 정보를 찾을 수 없습니다: " + writerEmail);
            }
            writerId = response.getBody();

        } catch (Exception e) {
            log.error("Failed to fetch writerId for email: {}", writerEmail, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "사용자 정보를 조회하는 중 오류가 발생했습니다.");
        }

        // 5. '즉시 상신' 메소드를 호출
        return progressReport(newProgressReq, writerId, files);
    }

    /**
     * 약속된 JSON 구조를 기반으로 보고서 내용을 생성합니다.
     */
    private String generateContentFromTemplate(String templateJson, Map<String, Object> values) {
        try {
            // 템플릿 전체 JSON을 파싱합니다.
            JsonNode root = objectMapper.readTree(templateJson);
            // "contentTemplate" 키에 해당하는 HTML 문자열을 가져옵니다.
            String contentTemplate = root.path("contentTemplate").asText();

            if (contentTemplate.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "템플릿 양식이 비어있습니다.");
            }

            String finalContent = contentTemplate;
            if (values != null) {
                // values 맵을 순회하며 {{key}} 형식의 플레이스홀더를 실제 값으로 치환합니다.
                for (Map.Entry<String, Object> entry : values.entrySet()) {
                    finalContent = finalContent.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
                }
            }

            return finalContent;

        } catch (JsonProcessingException e) {
            log.error("템플릿 contentTemplate 파싱 실패", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "템플릿 양식을 처리하는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 보고서 목록 조회
     */
    public ReportListResDto getReports(String role, ReportStatus status, String keyword,
                                       int page, int size, Long writerId,
                                       @RequestParam(defaultValue = "id") String sortBy,
                                       @RequestParam(defaultValue = "DESC") String sortOrder) {

        Sort.Direction direction = sortOrder.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = "createdAt".equalsIgnoreCase(sortBy) ? "reportCreatedAt" : "id";
        Sort sort = Sort.by(direction, sortProperty);

        // 2. 페이징(Pageable) 객체 생성
        Pageable pageable = PageRequest.of(page, size, sort);

        // 3. 동적 검색 조건(Specification) 생성
        // ReportSpecifications 클래스의 정적 메소드를 호출하여 조건을 조합합니다.
        Specification<Reports> spec = ReportSpecifications.withDynamicQuery(role, status, keyword, writerId);
        // 4. Repository의 findAll 메소드를 단 한 번만 호출하여 데이터 조회
        // JpaSpecificationExecutor를 상속받았기 때문에 이 메소드를 사용할 수 있습니다.
        Page<Reports> pr = reportsRepository.findAll(spec, pageable);

        Set<Long> employeeIdsToFetch = new HashSet<>();
        pr.getContent().forEach(report -> {
            employeeIdsToFetch.add(report.getWriterId());
            if (report.getCurrentApproverId() != null) {
                employeeIdsToFetch.add(report.getCurrentApproverId());
            }
            report.getApprovalLines().forEach(line -> employeeIdsToFetch.add(line.getEmployeeId()));
        });

        Map<Long, String> employeeNamesMap = Collections.emptyMap();

        if (!employeeIdsToFetch.isEmpty()) {
            try {
                ResponseEntity<Map<Long, String>> response = employeeFeignClient.getEmployeeNamesByEmployeeIds(new ArrayList<>(employeeIdsToFetch));
                employeeNamesMap = Optional.ofNullable(response.getBody()).orElse(Collections.emptyMap());
                log.info("Successfully fetched {} employee names.", employeeNamesMap.size());
            } catch (Exception e) {
                log.error("Error fetching employee names from hr-service", e);
                employeeNamesMap = Collections.emptyMap();
            }
        }

        final Map<Long, String> finalEmployeeNamesMap = employeeNamesMap;

        List<ReportListResDto.ReportSimpleDto> simples = pr.getContent().stream()
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
     *
     * @param reportId 조회할 보고서의 ID
     * @param writerId 현재 사용자의 직원 ID (권한 확인용)
     * @return 보고서 상세 정보 DTO
     */
    public ReportDetailResDto getReportDetail(Long reportId, Long writerId) {

        // 1. 보고서 엔티티를 조회합니다.
        Reports report = reportsRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다. id=" + reportId));

        // 리팩토링
        checkReadAccess(report, writerId);


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
        isUserInReferences(report, writerId);

        // 3. API 호출을 위한 모든 관련 직원 ID를 수집합니다.
        Set<Long> employeeIdsToFetch = new HashSet<>();

        employeeIdsToFetch.add(report.getWriterId()); // 작성자 ID
        if (report.getCurrentApproverId() != null) {
            employeeIdsToFetch.add(report.getCurrentApproverId()); // 현재 결재자 ID
        }
        report.getApprovalLines().forEach(line -> employeeIdsToFetch.add(line.getEmployeeId())); // 결재 라인의 모든 직원 ID

        Map<String, Object> templateStructure = new HashMap<>();
        Map<String, Object> formData = new HashMap<>();

        try {
            // 템플릿 ID가 있는지 확인
            if (report.getReportTemplateId() != null) {
                // 템플릿 ID로 ReportTemplate 엔티티를 DB에서 조회
                ReportTemplate template = templateRepository.findById(report.getReportTemplateId())
                        .orElse(null);

                // 템플릿이 존재하면, template JSON 문자열을 Map으로 변환
                if (template != null && template.getTemplate() != null) {
                    templateStructure = objectMapper.readValue(template.getTemplate(), new TypeReference<>() {
                    });
                }
            }

            // 템플릿에 입력된 데이터(formData)가 있는지 확인
            if (report.getReportTemplateData() != null && !report.getReportTemplateData().isBlank()) {
                // reportTemplateData JSON 문자열을 Map으로 변환
                formData = objectMapper.readValue(report.getReportTemplateData(), new TypeReference<>() {
                });
            }
        } catch (JsonProcessingException e) {
            log.error("템플릿 또는 폼 데이터 파싱 실패: reportId={}", reportId, e);
            // 파싱에 실패하더라도 에러를 발생시키지 않고, 빈 객체를 보내줍니다.
            // 프론트엔드가 null 대신 빈 객체를 받아 안정적으로 처리할 수 있도록 합니다.
        }

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
                        .map(arr -> objectMapper.convertValue(arr, new TypeReference<List<AttachmentJsonReqDto>>() {
                        }))
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(a -> new ReportDetailResDto.AttachmentResDto(a.getFileName(), a.getUrl()))
                        .collect(Collectors.toList());
                // 참조자 파싱
                refs = Optional.of(root.path("references"))
                        .filter(JsonNode::isArray)
                        .map(arr -> objectMapper.convertValue(arr, new TypeReference<List<ReferenceJsonReqDto>>() {
                        }))
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(rj -> new ReportDetailResDto.ReferenceJsonResDto(rj.getEmployeeId()))
                        .collect(Collectors.toList());
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "detail JSON 파싱 실패", e);
            }
        }
        List<ReportDetailResDto.AttachmentResDto> finalAttachments = atts.stream()
                .map(attachment -> {
                    try {
                        String fileKey = extractS3KeyFromUrl(attachment.getUrl());
                        // 'inline'은 브라우저에서 바로 열리도록 하는 옵션입니다.
                        String presignedUrl = s3Service.generatePresignedUrl(fileKey, "inline");
                        return new ReportDetailResDto.AttachmentResDto(attachment.getFileName(), presignedUrl);
                    } catch (Exception e) {
                        log.error("Pre-signed URL 생성 실패: {}", attachment.getUrl(), e);
                        // 실패 시에는 빈 URL이나 원본 URL을 그대로 반환할 수 있습니다.
                        return new ReportDetailResDto.AttachmentResDto(attachment.getFileName(), "");
                    }
                })
                .collect(Collectors.toList());

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
                .attachments(finalAttachments)
                .references(refs)
                .writer(ReportDetailResDto.WriterInfoDto.builder()
                        .id(report.getWriterId())
                        .name(writerName)
                        .build())
                .reportCreatedAt(report.getReportCreatedAt().format(fmt))
                .reportStatus(report.getReportStatus())
                .approvalLine(lines)
                .currentApprover(currentApprover)
                .template(templateStructure)
                .formData(formData)
                .templateId(report.getReportTemplateId())
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

        checkReadAccess(report, writerId);
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
        isUserInReferences(report, writerId);
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
    public ReportRecallResDto recallReport(Long reportId, Long writerId) {
        Reports report = reportsRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다. id=" + reportId));

        System.out.println("요청된 보고서 ID: " + reportId);
        System.out.println("토큰에서 추출한 작성자 ID (userInfo): " + writerId);
        System.out.println("보고서 엔티티의 작성자 ID: " + report.getWriterId());
        System.out.println("보고서 현재 상태: " + report.getReportStatus());

        if (!report.getWriterId().equals(writerId) && report.getReportStatus() != ReportStatus.IN_PROGRESS || report.getReportStatus() != ReportStatus.SCHEDULED) {
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

        // 반려(REJECTED) 또는 회수(RECALLED) 상태의 문서만 재상신을 허용합니다.
        if (originalReport.getReportStatus() != ReportStatus.REJECTED &&
                originalReport.getReportStatus() != ReportStatus.RECALLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "반려되거나 회수된 보고서만 재상신할 수 있습니다.");
        }

        // 재상신 횟수를 단 한 번의 쿼리로 계산합니다.
        Integer resubmitCount = reportsRepository.countResubmitChainDepth(originalReportId);
        int currentResubmits = (resubmitCount == null) ? 0 : resubmitCount;
        log.info("Report ID: {} 의 현재 재상신 횟수: {}", originalReportId, currentResubmits);

        // 재상신 횟수가 3회 이상이면 차단합니다.
        if (currentResubmits >= 3) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "재상신은 최대 3회까지 가능합니다.");
        }

        // 2) 새로운 라인 리스트와 제목·본문을 넘기도록 변경
        Reports newReport = originalReport.resubmit(
                req.getNewTitle(),
                req.getNewContent(),
                req.getApprovalLine(),
                req.getAttachments()
        );

        newReport.applyResubmitTemplateInfo(
                originalReport.getReportTemplateId(), // 원본의 템플릿 종류
                req.getReportTemplateData() != null ? req.getReportTemplateData() : originalReport.getReportTemplateData() // 사용자가 새로 입력한 데이터 (없으면 원본 데이터)
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


    @Transactional
    public ReferenceResDto addReference(Long reportId, Long writerId, ReferenceReqDto req) {
        Reports report = reportsRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다."));

        if (!report.getWriterId().equals(writerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "참조자 추가 권한이 없습니다.");
        }

        try {
            // 1. 기존 detail JSON을 Map으로 파싱
            Map<String, Object> detailMap;
            if (report.getDetail() != null && !report.getDetail().isBlank()) {
                detailMap = objectMapper.readValue(report.getDetail(), new TypeReference<>() {
                });
            } else {
                detailMap = new HashMap<>();
            }

            // 2. references 리스트를 가져오거나 새로 생성
            List<Map<String, Long>> references = (List<Map<String, Long>>) detailMap.getOrDefault("references", new ArrayList<>());

            // 3. 이미 존재하는 참조자인지 확인 (중복 추가 방지)
            boolean exists = references.stream().anyMatch(ref -> req.getEmployeeId().equals(ref.get("employeeId")));
            if (!exists) {
                references.add(Map.of("employeeId", req.getEmployeeId()));
            }

            // 4. 수정된 references 리스트를 다시 detailMap에 넣기
            detailMap.put("references", references);

            // 5. Map을 다시 JSON 문자열로 변환하여 report에 저장
            report.setDetail(objectMapper.writeValueAsString(detailMap));

            // Response DTO는 상황에 맞게 생성하여 반환 (이 부분은 단순 예시)
            return ReferenceResDto.builder()
                    .reportId(reportId)
                    .employeeId(req.getEmployeeId())
                    .build();

        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "참조자 정보 처리 중 오류가 발생했습니다.", e);
        }
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

    @Transactional(readOnly = true)
    public ReportFormResDto getReportForm(Long reportId, Long templateId, Long userId) {
        try {
            Reports report = null;
            ReportTemplate template;

            // ----------------------------------------------------
            // 1. 보고서(report)와 템플릿(template) 엔티티 조회 (이 부분은 그대로)
            // ----------------------------------------------------
            if (reportId != null) {
                report = reportsRepository.findById(reportId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다."));

                if (!report.getWriterId().equals(userId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");
                }

                Long reportTemplateId = report.getReportTemplateId();
                if (reportTemplateId == null) {
                    throw new IllegalStateException("DB 데이터 오류: report_id=" + reportId + "의 report_template_id가 NULL입니다.");
                }
                template = templateRepository.findById(reportTemplateId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "양식을 찾을 수 없습니다."));

            } else {
                template = templateRepository.findById(templateId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "양식을 찾을 수 없습니다."));
            }

            // ----------------------------------------------------
            // 2. '구조(template)'와 '데이터(formData)' 파싱
            // ----------------------------------------------------
            Map<String, Object> templateStructure = objectMapper.readValue(template.getTemplate(), new TypeReference<>() {
            });
            templateStructure.put("id", template.getTemplateId());


            Map<String, Object> formData;

            if (report != null && report.getReportTemplateData() != null && !report.getReportTemplateData().isBlank()) {
                formData = objectMapper.readValue(report.getReportTemplateData(), new TypeReference<>() {
                });
            } else {
                formData = new HashMap<>();
            }

            List<ReportDetailResDto.ApprovalLineResDto> approvalLineDtos = Collections.emptyList();
            List<ReportDetailResDto.AttachmentResDto> attachmentDtos = Collections.emptyList();

            return new ReportFormResDto(
                    templateStructure, // 이제 이 객체에는 { id: 21, title: ... } 처럼 id가 포함됩니다.
                    formData,
                    approvalLineDtos,
                    attachmentDtos
            );

        } catch (JsonProcessingException e) {
            log.error("JSON 파싱 실패", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "양식 데이터를 처리하는 중 오류가 발생했습니다.");
        }
    }

    private String extractS3KeyFromUrl(String fileUrl) {
        try {
            String path = new URL(fileUrl).getPath();
            // 경로의 맨 앞 '/'를 제거하고, URL 디코딩을 수행합니다.
            return URLDecoder.decode(path.substring(1), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("S3 URL 파싱 또는 디코딩 실패: {}", fileUrl, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 파일 URL 형식입니다.");
        }
    }

    /**
     * 사용자가 특정 보고서에 대한 읽기 권한(작성자, 결재자, 참조자)이 있는지 확인합니다.
     *
     * @param report 확인할 보고서 엔티티
     * @param userId 확인할 사용자의 ID
     * @throws ResponseStatusException 권한이 없을 경우 FORBIDDEN 예외 발생
     */
    private void checkReadAccess(Reports report, Long userId) {
        boolean isWriter = report.getWriterId().equals(userId);
        boolean isApprover = report.getApprovalLines().stream()
                .anyMatch(l -> l.getEmployeeId().equals(userId));
        boolean isReference = isUserInReferences(report, userId);

        if (!isWriter && !isApprover && !isReference) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "조회 권한이 없습니다.");
        }
    }

    /**
     * 보고서의 detail JSON을 파싱하여 참조자 목록에 특정 사용자가 있는지 확인하는 헬퍼 메소드
     */
    private boolean isUserInReferences(Reports report, Long userId) {
        if (report.getDetail() != null && !report.getDetail().isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(report.getDetail());
                JsonNode referencesNode = root.path("references");
                if (referencesNode.isArray()) {
                    for (JsonNode refNode : referencesNode) {
                        if (refNode.path("employeeId").asLong() == userId) {
                            return true;
                        }
                    }
                }
            } catch (JsonProcessingException e) {
                log.error("참조자 권한 체크 중 JSON 파싱 실패, reportId: {}", report.getId(), e);
            }
        }
        return false;
    }

    // ApprovalService.java

    /**
     * 문서 카운트 서비스 (실제 목록 조회 로직과 100% 일치하도록 수정)
     *
     * @param userId 현재 사용자의 ID
     * @return 각 문서함별 개수가 담긴 DTO
     */
    @Transactional(readOnly = true)
    public ReportCountResDto getReportCounts(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사용자 ID가 필요합니다.");
        }

        // Specification을 사용하여 각 조건에 맞는 문서 개수를 정확하게 카운트합니다.

        // 1. 내가 결재할 문서 (결재 대기)
        // role='approver'는 현재 결재자(currentApproverId)를 기준으로 하므로, 별도 count 메소드가 더 정확하고 빠릅니다.
        long pendingCount = reportsRepository.countByCurrentApproverIdAndReportStatus(userId, ReportStatus.IN_PROGRESS);

        // 2. 내가 올린 문서 (상태별)
        long inProgressCount = reportsRepository.countByWriterIdAndReportStatus(userId, ReportStatus.IN_PROGRESS);
        long rejectedCount = reportsRepository.countByWriterIdAndReportStatus(userId, ReportStatus.REJECTED);
        long draftsCount = reportsRepository.countByWriterIdAndReportStatusIn(
                userId,
                List.of(ReportStatus.DRAFT, ReportStatus.RECALLED)
        );
        long scheduledCount = reportsRepository.countByWriterIdAndReportStatus(userId, ReportStatus.SCHEDULED);

        // ★★★ 핵심 수정 부분: 참조 문서 개수 ★★★
        // 3. 내가 참조된 문서
        // 목록 조회와 동일한 Specification을 사용하여 개수를 셉니다.
        Specification<Reports> referenceSpec = ReportSpecifications.withDynamicQuery("reference", null, null, userId);
        long referenceCount = reportsRepository.count(referenceSpec);

        // ★★★ 핵심 수정 부분: 결재 완료 문서 개수 ★★★
        // 프론트엔드 요청에 따라, '내가 기안한(writer)' 완료 문서만 카운트합니다.
        long completedCount = reportsRepository.countByWriterIdAndReportStatus(userId, ReportStatus.APPROVED);

        // 4. 조회된 결과를 DTO에 담아 반환
        return new ReportCountResDto(
                pendingCount,
                inProgressCount,
                rejectedCount,
                draftsCount,
                scheduledCount,
                referenceCount,
                completedCount // ReportCountResDto에 completed 필드 추가 필요
        );
    }
}
