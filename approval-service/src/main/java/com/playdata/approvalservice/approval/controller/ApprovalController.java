package com.playdata.approvalservice.approval.controller;

import com.playdata.approvalservice.approval.dto.request.*;
import com.playdata.approvalservice.approval.dto.response.*;
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
     * @param req
     * @param authHeader
     * @return
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
     * @param role
     * @param status
     * @param page
     * @param size
     * @param keyword
     * @param from
     * @param to
     * @param authHeader
     * @return
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
     * @param reportId
     * @param authHeader
     * @return
     */
    @GetMapping("/reports/{reportId}")
    public ResponseEntity<CommonResDto> getReportDetail(
            @PathVariable Long reportId,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long userId = jwtTokenProvider.getUserIdFromToken(authHeader);
        ReportDetailResDto res = approvalService.getReportDetail(reportId, userId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "보고서 상세 조회", res));
    }


    /**
     * 결재 처리 (APPROVE/REJECT)
     * @param reportId
     * @param req
     * @param authHeader
     * @return
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
     * @param reportId
     * @param authHeader
     * @return
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
     * @param reportId
     * @param req
     * @param authHeader
     * @return
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
     * 보고서 삭제
     * @param reportId
     * @param authHeader
     * @return
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
     * @param reportId
     * @param authHeader
     * @return
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

    /**
     * 결재 코멘트
     * @param reportId
     * @param req
     * @param authHeader
     * @return
     */
    @PostMapping("/reports/{reportId}/comments")
    public ResponseEntity<CommonResDto> createComment(
            @PathVariable Long reportId,
            @RequestBody @Valid CommentReqDto req,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long userId = jwtTokenProvider.getUserIdFromToken(authHeader);
        CommentResDto res = approvalService.createComment(reportId, userId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "코멘트 등록 완료", res));
    }

    /**
     * 첨부파일 업로드
     * @param reportId
     * @param req
     * @param authHeader
     * @return
     */
    @PostMapping("/reports/{reportId}/attachments")
    public ResponseEntity<CommonResDto> uploadAttachment(
            @PathVariable Long reportId,
            @RequestBody @Valid AttachmentReqDto req,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long userId = jwtTokenProvider.getUserIdFromToken(authHeader);
        AttachmentResDto res = approvalService.uploadAttachment(reportId, userId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "첨부파일 등록 완료", res));
    }

    /**
     * 참조자 추가
     * @param reportId
     * @param req
     * @param authHeader
     * @return
     */
    @PostMapping("/reports/{reportId}/references")
    public ResponseEntity<CommonResDto> addReference(
            @PathVariable Long reportId,
            @RequestBody @Valid ReferenceReqDto req,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long userId = jwtTokenProvider.getUserIdFromToken(authHeader);
        ReferenceResDto res = approvalService.addReference(reportId, userId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "참조자 추가 완료", res));
    }

    /**
     * 참조자 제거
     * @param reportId
     * @param employeeId
     * @param authHeader
     * @return
     */
    @DeleteMapping("/reports/{reportId}/references/{employeeId}")
    public ResponseEntity<CommonResDto> deleteReferences(
            @PathVariable Long reportId,
            @PathVariable Long employeeId,
            @RequestHeader("Authorization") String authHeader
    ){
        Long userId = jwtTokenProvider.getUserIdFromToken(authHeader);
        ReportReferencesResDto res = approvalService.deleteReferences(reportId, userId, employeeId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "참조자 삭제 완료", res));
    }

    /**
     * 리마인더
     * @param reportId
     * @param authHeader
     * @return
     */
    @PostMapping("/reports/{reportId}/remind")
    public ResponseEntity<CommonResDto> remindReport(
            @PathVariable Long reportId,
            @RequestHeader("Authorization") String authHeader
    ){
        Long userId = jwtTokenProvider.getUserIdFromToken(authHeader);
        ReportRemindResDto res = approvalService.reportRemind(reportId, userId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "결재 리마인드 완료", res));
    }

    /**
     * 보고서 재상신
     * @param reportId
     * @param req
     * @param authHeader
     * @return
     */
    @PostMapping("/reports/{reportId}/resubmit")
    public ResponseEntity<CommonResDto> resubmitReport(
            @PathVariable Long reportId,
            @RequestBody(required=false) ResubmitReqDto req,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long userId = jwtTokenProvider.getUserIdFromToken(authHeader);
        ResubmitResDto res = approvalService.resubmitReport(reportId, userId, req);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "보고서 재상신 완료", res));
    }


}
