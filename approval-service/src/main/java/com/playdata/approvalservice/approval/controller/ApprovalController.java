package com.playdata.approvalservice.approval.controller;

import com.playdata.approvalservice.approval.dto.request.*;
import com.playdata.approvalservice.approval.dto.response.*;
import com.playdata.approvalservice.approval.entity.ReportStatus;
import com.playdata.approvalservice.approval.service.ApprovalService;
import com.playdata.approvalservice.common.auth.JwtTokenProvider;
import com.playdata.approvalservice.common.dto.CommonResDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 전자결재 API 컨트롤러
 */
@RestController
@RequestMapping("/approval-service")
@RequiredArgsConstructor
@Slf4j
public class ApprovalController {

    private final ApprovalService approvalService;
    private final JwtTokenProvider jwtTokenProvider;

    private Long extractUserId(String authHeader) {
        return jwtTokenProvider.getUserIdFromToken(authHeader);
    }

    /**
     * 보고서 생성 (DRAFT)
     */
    @PostMapping("/create")
    public ResponseEntity<CommonResDto> createReport(
            @RequestBody @Valid ReportCreateReqDto req,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long userId = extractUserId(authHeader);
        ReportCreateResDto res = approvalService.createReport(req, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "보고서 생성", res));
    }

    /**
     * 보고서 수정 (Draft)
     */
    @PutMapping("/reports/{reportId}")
    public ResponseEntity<CommonResDto> updateReport(
            @PathVariable Long reportId,
            @RequestBody @Valid ReportUpdateReqDto req,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long userId = extractUserId(authHeader);
        ReportUpdateResDto res = approvalService.updateReport(reportId, req, userId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "보고서 수정 완료", res));
    }

    /**
     * 보고서 목록 조회
     */
    @GetMapping("/reports")
    public ResponseEntity<CommonResDto> getReports(
            @RequestParam String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long userId = extractUserId(authHeader);
        ReportStatus statusEnum = (status != null && !status.isEmpty())
                ? ReportStatus.valueOf(status)
                : null;
        ReportListResDto res = approvalService.getReports(
                role, statusEnum, keyword, page, size, userId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "보고서 목록 조회", res));
    }

    /**
     * 보고서 상세 조회
     */
    @GetMapping("/reports/{reportId}")
    public ResponseEntity<CommonResDto> getReportDetail(
            @PathVariable Long reportId,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long userId = extractUserId(authHeader);
        ReportDetailResDto res = approvalService.getReportDetail(reportId, userId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "보고서 상세 조회", res));
    }

    /**
     * 결재 처리 (APPROVE/REJECT)
     */
    @PostMapping("/reports/{reportId}/approvals")
    public ResponseEntity<CommonResDto> processApproval(
            @PathVariable Long reportId,
            @RequestBody @Valid ApprovalProcessReqDto req,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long userId = extractUserId(authHeader);
        ApprovalProcessResDto res = approvalService.processApproval(reportId, userId, req);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "결재 처리", res));
    }

    /**
     * 보고서 회수 처리
     */
    @PostMapping("/reports/{reportId}/recall")
    public ResponseEntity<CommonResDto> recallReport(
            @PathVariable Long reportId,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long userId = extractUserId(authHeader);
        ReportRecallResDto res = approvalService.recallReport(reportId, userId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "보고서 회수 완료", res));
    }

    /**
     * 리마인드 전송 처리
     */
    @PostMapping("/reports/{reportId}/remind")
    public ResponseEntity<CommonResDto> remindReport(
            @PathVariable Long reportId,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long userId = extractUserId(authHeader);
        ReportRemindResDto res = approvalService.remindReport(reportId, userId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "리마인드 알림 완료", res));
    }

    /**
     * 보고서 재상신 처리
     */
    @PostMapping("/reports/{reportId}/resubmit")
    public ResponseEntity<CommonResDto> resubmitReport(
            @PathVariable Long reportId,
            @RequestBody(required = false) ResubmitReqDto req,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long userId = extractUserId(authHeader);
        ResubmitResDto res = approvalService.resubmitReport(reportId, userId, req);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "보고서 재상신 완료", res));
    }

    /**
     * 참조자 추가 처리
     */
    @PostMapping("/reports/{reportId}/references")
    public ResponseEntity<CommonResDto> addReference(
            @PathVariable Long reportId,
            @RequestBody @Valid ReferenceReqDto req,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long userId = extractUserId(authHeader);
        ReferenceResDto res = approvalService.addReference(reportId, userId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "참조자 추가 완료", res));
    }

    /**
     * 참조자 제거 처리
     */
    @DeleteMapping("/reports/{reportId}/references/{employeeId}")
    public ResponseEntity<CommonResDto> deleteReferences(
            @PathVariable Long reportId,
            @PathVariable Long employeeId,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long userId = extractUserId(authHeader);
        ReportReferencesResDto res = approvalService.deleteReferences(reportId, userId, employeeId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "참조자 삭제 완료", res));
    }

    /**
     * 첨부파일 업로드 처리
     */
    @PostMapping("/reports/{reportId}/attachments")
    public ResponseEntity<CommonResDto> uploadAttachment(
            @PathVariable Long reportId,
            @RequestBody @Valid AttachmentReqDto req,
            @RequestHeader("Authorization") String authHeader
    ) {
        // userId removed as service doesn't need it
        AttachmentResDto res = approvalService.uploadAttachment(reportId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "첨부파일 등록 완료", res));
    }
}
