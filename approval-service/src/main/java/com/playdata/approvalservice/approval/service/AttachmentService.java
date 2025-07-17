package com.playdata.approvalservice.approval.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playdata.approvalservice.approval.entity.Reports;
import com.playdata.approvalservice.approval.repository.ReportsRepository;
import com.playdata.approvalservice.approval.feign.EmployeeFeignClient;
import com.playdata.approvalservice.common.dto.EmployeeResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true) // DB 조회가 있으므로 readOnly 트랜잭션 추가
public class AttachmentService {

    private final ReportsRepository reportsRepository;
    private final EmployeeFeignClient employeeFeignClient;
    private final ObjectMapper objectMapper;
    private final S3Service s3Service;

    public String getPresignedUrlForAction(Long reportId, String encodedFileUrl, String userEmail, String dispositionType) {
        // 1. URL 디코딩
        String fileUrl = URLDecoder.decode(encodedFileUrl, StandardCharsets.UTF_8);

        // 2. 접근 권한 확인 (모든 사전 준비 및 검사를 이 메소드에서 수행)
        checkAccessPermission(reportId, userEmail, fileUrl);

        // 3. 전체 URL에서 S3 파일 키(key) 추출
        String fileKey = extractS3KeyFromUrl(fileUrl);

        // 4. S3 서비스에 Pre-signed URL 생성 위임
        return s3Service.generatePresignedUrl(fileKey, dispositionType);
    }

    /**
     * 접근 권한 확인을 위한 모든 로직을 포함하는 완전한 메소드
     */
    private void checkAccessPermission(Long reportId, String userEmail, String fileUrl) {
        // A. 사용자 정보 조회: 이메일로 userId를 가져옵니다.
        Long userId;
        try {
            ResponseEntity<EmployeeResDto> response = employeeFeignClient.getEmployeeByEmail(userEmail);
            if (response.getBody() == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 정보를 찾을 수 없습니다: " + userEmail);
            }
            userId = response.getBody().getEmployeeId();
        } catch (Exception e) {
            log.error("Employee-Service 통신 실패: {}", userEmail, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "사용자 정보를 조회하는 중 오류가 발생했습니다.");
        }

        // B. 문서 정보 조회: reportId로 Reports 엔티티를 가져옵니다.
        Reports report = reportsRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다. ID: " + reportId));

        // C. 권한 검사 로직 실행: 조회된 userId와 report 객체로 실제 검사를 수행합니다.
        boolean isWriter = report.getWriterId().equals(userId);
        boolean isApprover = report.getApprovalLines().stream()
                .anyMatch(l -> l.getEmployeeId().equals(userId));
        boolean isReference = isUserInReferences(report, userId);

        if (!isWriter && !isApprover && !isReference) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "파일에 접근할 권한이 없습니다.");
        }

        // D. (보안 강화) 요청된 파일이 해당 문서의 첨부파일이 맞는지 최종 확인합니다.
        if (report.getDetail() == null || !report.getDetail().contains(fileUrl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청한 파일이 해당 문서의 첨부파일이 아닙니다.");
        }
    }

    // 참조자 목록에 사용자가 있는지 확인하는 헬퍼 메소드
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

    // URL에서 S3 키를 추출하는 헬퍼 메소드 (기존과 동일)
    private String extractS3KeyFromUrl(String fileUrl) {
        try {
            return new URL(fileUrl).getPath().substring(1);
        } catch (Exception e) {
            log.error("S3 URL 파싱 실패: {}", fileUrl, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 파일 URL 형식입니다.");
        }
    }
}