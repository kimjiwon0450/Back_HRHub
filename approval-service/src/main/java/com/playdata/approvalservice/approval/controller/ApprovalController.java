package com.playdata.approvalservice.approval.controller;

import com.playdata.approvalservice.approval.dto.request.*;
import com.playdata.approvalservice.approval.dto.response.*;
import com.playdata.approvalservice.approval.entity.ReportStatus;
import com.playdata.approvalservice.approval.service.ApprovalService;
import com.playdata.approvalservice.common.auth.TokenUserInfo;
import com.playdata.approvalservice.common.dto.CommonResDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    /**
     * 보고서 생성 (초안 저장)
     */
    @PostMapping("/create")
    public ResponseEntity<CommonResDto> createReport(
            @RequestBody @Valid ReportCreateReqDto req,
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo
    ) {
        Long writerId = tokenUserInfo.getEmployeeId();
        ReportCreateResDto res = approvalService.createReport(req, writerId);
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
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo
    ) {
        Long writerId = tokenUserInfo.getEmployeeId();
        ReportUpdateResDto res = approvalService.updateReport(reportId, req, writerId);
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
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo
    ) {
        Long writerId = tokenUserInfo.getEmployeeId();
        ReportStatus statusEnum = (status != null && !status.isEmpty())
                ? ReportStatus.valueOf(status)
                : null;
        ReportListResDto res = approvalService.getReports(
                role, statusEnum, keyword, page, size, writerId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "보고서 목록 조회", res));
    }

    /**
     * 보고서 상세 조회
     */
    @GetMapping("/reports/{reportId}")
    public ResponseEntity<CommonResDto> getReportDetail(
            @PathVariable Long reportId,
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo
    ) {
        Long writerId = tokenUserInfo.getEmployeeId();
        ReportDetailResDto res = approvalService.getReportDetail(reportId, writerId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "보고서 상세 조회", res));
    }

    /**
     * 결재 처리 (Approve/Reject)
     */
    @PostMapping("/reports/{reportId}/approvals")
    public ResponseEntity<CommonResDto> processApproval(
            @PathVariable Long reportId,
            @RequestBody @Valid ApprovalProcessReqDto req,
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo
    ) {
        Long writerId = tokenUserInfo.getEmployeeId();
        ApprovalProcessResDto res = approvalService.processApproval(reportId, writerId, req);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "결재 처리", res));
    }

    /**
     * 보고서 회수
     */
    @PostMapping("/reports/{reportId}/recall")
    public ResponseEntity<CommonResDto> recallReport(
            @PathVariable Long reportId,
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo
    ) {
        Long writerId = tokenUserInfo.getEmployeeId();
        ReportRecallResDto res = approvalService.recallReport(reportId, writerId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "보고서 회수 완료", res));
    }

    /**
     * 리마인드 전송
     */
    @PostMapping("/reports/{reportId}/remind")
    public ResponseEntity<CommonResDto> remindReport(
            @PathVariable Long reportId,
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo
    ) {
        Long writerId = tokenUserInfo.getEmployeeId();
        ReportRemindResDto res = approvalService.remindReport(reportId, writerId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "리마인드 알림 완료", res));
    }

    /**
     * 재상신
     */
    @PostMapping("/reports/{reportId}/resubmit")
    public ResponseEntity<CommonResDto> resubmitReport(
            @PathVariable Long reportId,
            @RequestBody(required = false) ResubmitReqDto req,
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo
    ) {
        Long writerId = tokenUserInfo.getEmployeeId();
        ResubmitResDto res = approvalService.resubmitReport(reportId, writerId, req);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "보고서 재상신 완료", res));
    }

    /**
     * 참조자 추가
     */
    @PostMapping("/reports/{reportId}/references")
    public ResponseEntity<CommonResDto> addReference(
            @PathVariable Long reportId,
            @RequestBody @Valid ReferenceReqDto req,
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo
    ) {
        Long writerId = tokenUserInfo.getEmployeeId();
        ReferenceResDto res = approvalService.addReference(reportId, writerId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "참조자 추가 완료", res));
    }

    /**
     * 참조자 제거
     */
    @DeleteMapping("/reports/{reportId}/references/{employeeId}")
    public ResponseEntity<CommonResDto> deleteReferences(
            @PathVariable Long reportId,
            @PathVariable Long employeeId,
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo
    ) {
        Long writerId = tokenUserInfo.getEmployeeId();
        ReportReferencesResDto res = approvalService.deleteReferences(reportId, writerId, employeeId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "참조자 삭제 완료", res));
    }

    /**
     * 첨부파일 업로드
     */
    @PostMapping("/reports/{reportId}/attachments")
    public ResponseEntity<CommonResDto> uploadAttachment(
            @PathVariable Long reportId,
            @RequestBody @Valid AttachmentReqDto req,
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo
    ) {
        Long writerId = tokenUserInfo.getEmployeeId();
        AttachmentResDto res = approvalService.uploadAttachment(reportId,req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "첨부파일 등록 완료", res));
    }
}
