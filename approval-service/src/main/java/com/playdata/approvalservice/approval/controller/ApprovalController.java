package com.playdata.approvalservice.approval.controller;

import com.playdata.approvalservice.approval.dto.request.ReportCreateReqDto;
import com.playdata.approvalservice.approval.dto.request.SubmitReportReqDto;
import com.playdata.approvalservice.approval.dto.request.ApprovalProcessReqDto;
import com.playdata.approvalservice.approval.dto.response.ReportCreateResDto;
import com.playdata.approvalservice.approval.dto.response.ReportListResDto;
import com.playdata.approvalservice.approval.dto.response.ReportDetailResDto;
import com.playdata.approvalservice.approval.dto.response.ApprovalProcessResDto;
import com.playdata.approvalservice.approval.dto.response.ApprovalHistoryResDto;
import com.playdata.approvalservice.approval.dto.response.ReportSubmitResDto;
import com.playdata.approvalservice.approval.dto.response.ReportRecallResDto;
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

    /**
     * 보고서 생성 (DRAFT)
     */
    @PostMapping("/create")
    public ResponseEntity<CommonResDto> createReport(
            @RequestBody @Valid ReportCreateReqDto req,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long userId = jwtTokenProvider.getUserIdFromToken(authHeader);
        ReportCreateResDto res = approvalService.createReport(req, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "보고서 생성", res));
    }

    /**
     * 보고서 목록 조회 (writer/approver)
     */
    @GetMapping("/reports")
    public ResponseEntity<CommonResDto> getReports(
            @RequestParam String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long userId = jwtTokenProvider.getUserIdFromToken(authHeader);
        ReportListResDto res = approvalService.getReports(
                role, status, page, size, keyword, from, to, userId);
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
        ReportDetailResDto res = approvalService.getReportDetail(reportId);
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
        Long userId = jwtTokenProvider.getUserIdFromToken(authHeader);
        ApprovalProcessResDto res = approvalService.processApproval(reportId, userId, req);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "결재 처리", res));
    }

    /**
     * 결재 이력 조회
     */
    @GetMapping("/reports/{reportId}/history")
    public ResponseEntity<CommonResDto> getApprovalHistory(
            @PathVariable Long reportId,
            @RequestHeader("Authorization") String authHeader
    ) {
        ApprovalHistoryResDto res = approvalService.getApprovalHistory(reportId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "결재 이력 조회", res));
    }

    /**
     * 결재 상신 (제출)
     */
    @PostMapping("/reports/{reportId}/submit")
    public ResponseEntity<CommonResDto> submitReport(
            @PathVariable Long reportId,
            @RequestBody(required = false) SubmitReportReqDto req,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long userId = jwtTokenProvider.getUserIdFromToken(authHeader);
        ReportSubmitResDto res = approvalService.submitReport(reportId, userId, req);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "보고서 상신", res));
    }

    /**
     * 보고서 삭제 (DRAFT)
     */
    @DeleteMapping("/reports/{reportId}")
    public ResponseEntity<CommonResDto> deleteReport(
            @PathVariable Long reportId,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long userId = jwtTokenProvider.getUserIdFromToken(authHeader);
        approvalService.deleteReport(reportId, userId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "보고서 삭제", reportId));
    }

    /**
     * 보고서 결재 회수
     */
    @PostMapping("/reports/{reportId}/recall")
    public ResponseEntity<CommonResDto> recallReport(
            @PathVariable Long reportId,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long userId = jwtTokenProvider.getUserIdFromToken(authHeader);
        ReportRecallResDto res = approvalService.recallReport(reportId, userId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "보고서 회수", res));
    }
}
