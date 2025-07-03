package com.playdata.approvalservice.approval.controller;

import com.playdata.approvalservice.approval.dto.request.*;
import com.playdata.approvalservice.approval.dto.response.*;
import com.playdata.approvalservice.approval.entity.ReportStatus;
import com.playdata.approvalservice.approval.feign.EmployeeFeignClient;
import com.playdata.approvalservice.approval.service.ApprovalService;
import com.playdata.approvalservice.common.auth.TokenUserInfo;
import com.playdata.approvalservice.common.dto.CommonResDto;
import com.playdata.approvalservice.common.dto.EmployeeResDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * 전자결재 API 컨트롤러
 */
@RestController
@RequestMapping("/approval-service")
@RequiredArgsConstructor
@Slf4j
public class ApprovalController {

    private final ApprovalService approvalService;
    private final EmployeeFeignClient employeeFeignClient; // ✅ 추가: FeignClient 주입


    /**
     * 보고서 생성 (DRAFT)
     */
    @PostMapping("/create")
    public ResponseEntity<CommonResDto> createReport(
            @RequestBody @Valid ReportCreateReqDto req,
            @AuthenticationPrincipal TokenUserInfo userInfo// 필터에서 주입된 사용자 ID
    ) {
        String email = userInfo.getEmail();
        EmployeeResDto employee = employeeFeignClient.getEmployeeByEmail(email).getBody();
        if (employee == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "직원을 찾을 수 없습니다.");
        }
        Long writerId = employee.getEmployeeId();

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
            @AuthenticationPrincipal TokenUserInfo userInfo
            // [수정] 파라미터로 writerId 주입
    ) {
        String email = userInfo.getEmail();
        EmployeeResDto employee = employeeFeignClient.getEmployeeByEmail(email).getBody();
        if (employee == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "직원을 찾을 수 없습니다.");
        }
        Long writerId = employee.getEmployeeId();

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
            @AuthenticationPrincipal TokenUserInfo userInfo
            // [수정] 파라미터로 writerId 주입
    ) {
        String email = userInfo.getEmail();
        EmployeeResDto employee = employeeFeignClient.getEmployeeByEmail(email).getBody();
        if (employee == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "직원을 찾을 수 없습니다.");
        }
        Long writerId = employee.getEmployeeId();

        ReportStatus statusEnum = (status != null && !status.isEmpty())
                ? ReportStatus.valueOf(status.toUpperCase())
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
            @AuthenticationPrincipal TokenUserInfo userInfo
            // [수정] 파라미터로 writerId 주입
    ) {
        String email = userInfo.getEmail();
        EmployeeResDto employee = employeeFeignClient.getEmployeeByEmail(email).getBody();
        if (employee == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "직원을 찾을 수 없습니다.");
        }
        Long writerId = employee.getEmployeeId();

        ReportDetailResDto res = approvalService.getReportDetail(reportId, writerId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "보고서 상세 조회", res));
    }

    /**
     * 결재 처리 (APPROVE/REJECT)
     */
    @PostMapping("/reports/{reportId}/approvals")
    public ResponseEntity<CommonResDto> processApproval(
            @PathVariable Long reportId,
            @RequestBody @Valid ApprovalProcessReqDto req,
            @AuthenticationPrincipal TokenUserInfo userInfo
            // [수정] 파라미터로 writerId 주입
    ) {
        String email = userInfo.getEmail();
        EmployeeResDto employee = employeeFeignClient.getEmployeeByEmail(email).getBody();
        if (employee == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "직원을 찾을 수 없습니다.");
        }
        Long writerId = employee.getEmployeeId();

        ApprovalProcessResDto res = approvalService.processApproval(reportId, writerId, req);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "결재 처리", res));
    }

    /**
     * 보고서 회수 처리
     */
    @PostMapping("/reports/{reportId}/recall")
    public ResponseEntity<CommonResDto> recallReport(
            @PathVariable Long reportId,
            @AuthenticationPrincipal TokenUserInfo userInfo
            // [수정] 파라미터로 writerId 주입
    ) {
        String email = userInfo.getEmail();
        EmployeeResDto employee = employeeFeignClient.getEmployeeByEmail(email).getBody();
        if (employee == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "직원을 찾을 수 없습니다.");
        }
        Long writerId = employee.getEmployeeId();

        ReportRecallResDto res = approvalService.recallReport(reportId, writerId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "보고서 회수 완료", res));
    }

    /**
     * 리마인드 전송 처리
     */
    @PostMapping("/reports/{reportId}/remind")
    public ResponseEntity<CommonResDto> remindReport(
            @PathVariable Long reportId,
            @AuthenticationPrincipal TokenUserInfo userInfo
            // [수정] 파라미터로 writerId 주입
    ) {
        String email = userInfo.getEmail();
        EmployeeResDto employee = employeeFeignClient.getEmployeeByEmail(email).getBody();
        if (employee == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "직원을 찾을 수 없습니다.");
        }
        Long writerId = employee.getEmployeeId();

        ReportRemindResDto res = approvalService.remindReport(reportId, writerId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "리마인드 알림 완료", res));
    }

    /**
     * 보고서 재상신 처리
     */
    @PostMapping("/reports/{reportId}/resubmit")
    public ResponseEntity<CommonResDto> resubmitReport(
            @PathVariable Long reportId,
            @RequestBody(required = false) ResubmitReqDto req,
            @AuthenticationPrincipal TokenUserInfo userInfo
            // [수정] 파라미터로 writerId 주입
    ) {
        String email = userInfo.getEmail();
        EmployeeResDto employee = employeeFeignClient.getEmployeeByEmail(email).getBody();
        if (employee == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "직원을 찾을 수 없습니다.");
        }
        Long writerId = employee.getEmployeeId();

        ResubmitResDto res = approvalService.resubmitReport(reportId, writerId, req);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "보고서 재상신 완료", res));
    }

    /**
     * 참조자 추가 처리
     */
    @PostMapping("/reports/{reportId}/references")
    public ResponseEntity<CommonResDto> addReference(
            @PathVariable Long reportId,
            @RequestBody @Valid ReferenceReqDto req,
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {
        String email = userInfo.getEmail();
        EmployeeResDto employee = employeeFeignClient.getEmployeeByEmail(email).getBody();
        if (employee == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "직원을 찾을 수 없습니다.");
        }
        Long writerId = employee.getEmployeeId();


        ReferenceResDto res = approvalService.addReference(reportId, writerId, req);
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
            @AuthenticationPrincipal TokenUserInfo userInfo // [수정] 파라미터로 writerId 주입
    ) {
        String email = userInfo.getEmail();
        EmployeeResDto employee = employeeFeignClient.getEmployeeByEmail(email).getBody();
        if (employee == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "직원을 찾을 수 없습니다.");
        }
        Long writerId = employee.getEmployeeId();

        ReportReferencesResDto res = approvalService.deleteReferences(reportId, writerId, employeeId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "참조자 삭제 완료", res));
    }

    /**
     * 첨부파일 업로드 처리
     * (기존 코드에서 writerId를 사용하지 않으므로 여기서는 주입받지 않습니다.)
     */
    @PostMapping("/reports/{reportId}/attachments")
    public ResponseEntity<CommonResDto> uploadAttachment(
            @PathVariable Long reportId,
            @RequestBody @Valid AttachmentReqDto req
    ) {
        AttachmentResDto res = approvalService.uploadAttachment(reportId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "첨부파일 등록 완료", res));
    }
}
