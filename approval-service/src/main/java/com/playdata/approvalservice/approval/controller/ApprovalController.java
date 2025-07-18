package com.playdata.approvalservice.approval.controller;

import com.playdata.approvalservice.approval.dto.request.*;
import com.playdata.approvalservice.approval.dto.request.template.ReportFromTemplateReqDto;
import com.playdata.approvalservice.approval.dto.response.*;
import com.playdata.approvalservice.approval.dto.response.template.ReportFormResDto;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 전자결재 API 컨트롤러
 */
@RestController
@RequestMapping("/approval")
@RequiredArgsConstructor
@Slf4j
public class ApprovalController {

    private final ApprovalService approvalService;
    private final EmployeeFeignClient employeeFeignClient;


    /**
     * employeeId 넣어주는 메서드
     * @param userInfo
     * @return
     */
    private Long getCurrentUserId(TokenUserInfo userInfo) {
        String email = userInfo.getEmail();
        ResponseEntity<EmployeeResDto> response = employeeFeignClient.getEmployeeByEmail(email);
        EmployeeResDto employee = response.getBody();
        if (employee == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "직원을 찾을 수 없습니다: " + email);
        }
        return employee.getEmployeeId();
    }

    /**
     * 결재선/참조자 지정을 위해 재직 중인 직원 목록을 조회합니다.
     */
    @GetMapping("/employees/active")
    public ResponseEntity<CommonResDto> getActiveEmployeesForApproval() {
        // Feign Client를 통해 HR 서비스 호출
        ResponseEntity<List<EmployeeResDto>> response = employeeFeignClient.getActiveEmployees();

        // 응답을 CommonResDto로 감싸서 프론트엔드에 반환
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "재직 중인 직원 목록 조회 성공", response.getBody())
        );
    }

    /**
     * 보고서 생성 (DRAFT)
     */
    @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResDto> createReport(
            @RequestPart @Valid ReportSaveReqDto req,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal TokenUserInfo userInfo// 필터에서 주입된 사용자 ID
    ) {

        Long writerId = getCurrentUserId(userInfo);
        ReportCreateResDto res = approvalService.createReport(req, writerId, files);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "보고서 저장", res));
    }

    /**
     * 보고서 생성 (IN_PROGRESS)
     */
    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResDto> progressReport(
            @RequestPart @Valid ReportCreateReqDto req,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal TokenUserInfo userInfo// 필터에서 주입된 사용자 ID
    ){
        Long writerId = getCurrentUserId(userInfo);
        ReportCreateResDto res = approvalService.progressReport(req, writerId, files);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "보고서가 상신 되었습니다.", res));
    }


    @PutMapping("/reports/{reportId}")
    public ResponseEntity<CommonResDto> updateReport(
            @PathVariable Long reportId,
            @RequestBody @Valid ReportUpdateReqDto req,
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {
        Long writerId = getCurrentUserId(userInfo);

        // 서비스에서 ReportDetailResDto를 반환하므로, 변수 타입도 맞춰줌
        ReportDetailResDto res = approvalService.updateReport(reportId, req, writerId);
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

        Long writerId = getCurrentUserId(userInfo);

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
        Long writerId = getCurrentUserId(userInfo);
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

        Long writerId = getCurrentUserId(userInfo);

        ApprovalProcessResDto res = approvalService.processApproval(reportId, writerId, req);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "결재 처리", res));
    }

    /**
     * 보고서의 전체 결재 이력 조회
     * @param reportId
     * @param userInfo
     * @return
     */
    @GetMapping("/reports/{reportId}/history")
    public ResponseEntity<CommonResDto> processApprovalHistory(
            @PathVariable Long reportId,
            @AuthenticationPrincipal TokenUserInfo userInfo
    ){
        Long writerId = getCurrentUserId(userInfo);

        List<ApprovalHistoryResDto> res = approvalService.getApprovalHistory(reportId, writerId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "결재 이력 조회", res));
    }

    /**
     * 보고서 회수 처리
     */
    @PostMapping("/reports/{reportId}/recall")
    public ResponseEntity<CommonResDto> recallReport(
            @PathVariable Long reportId,
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {

        Long writerId = getCurrentUserId(userInfo);

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

        Long writerId = getCurrentUserId(userInfo);

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
    ) {

        Long writerId = getCurrentUserId(userInfo);

        ResubmitResDto res = approvalService.resubmit(reportId, writerId, req);
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

        Long writerId = getCurrentUserId(userInfo);


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
        Long writerId = getCurrentUserId(userInfo);

        ReportReferencesResDto res = approvalService.deleteReferences(reportId, writerId, employeeId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "참조자 삭제 완료", res));
    }

    @PostMapping("/reports/category")
    public ResponseEntity<CommonResDto> ReportCategory(
            @RequestPart("req") @Valid ReportFromTemplateReqDto req,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal TokenUserInfo userInfo
            ){
        ReportCreateResDto resDto = approvalService.reportFromTemplate(req, userInfo.getEmail(), files);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "결재 문서가 성공적으로 상신되었습니다.", resDto));
    }

    // ApprovalController.java 에 추가

    /**
     * 결재 문서 작성/수정 화면을 위한 데이터 조회 API
     * @param reportId (Optional) 임시저장/수정할 문서 ID
     * @param templateId (Optional) 새로 작성할 양식 ID
     * @return 양식의 구조(template)와 입력된 데이터(formData)
     */
    @GetMapping("/form")
    public ResponseEntity<CommonResDto> getReportForm(
            @RequestParam(required = false) Long reportId,
            @RequestParam(required =false) Long templateId,
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {

        if (reportId == null && templateId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reportId 또는 templateId가 필요합니다.");
        }

        Long userId = getCurrentUserId(userInfo);
        ReportFormResDto res = approvalService.getReportForm(reportId, templateId, userId);

        // CommonResDto로 감싸서 반환하도록 수정
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "결재 양식 조회 성공", res));
    }
}
