package com.playdata.approvalservice.approval.controller;

import com.playdata.approvalservice.approval.dto.request.template.TemplateCreateReqDto;
import com.playdata.approvalservice.approval.dto.request.template.TemplateUpdateReqDto;
import com.playdata.approvalservice.approval.dto.response.template.TemplateResDto;
import com.playdata.approvalservice.approval.service.TemplateService;
import com.playdata.approvalservice.common.auth.TokenUserInfo;
import com.playdata.approvalservice.common.dto.CommonResDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/approval-service/templates")
public class TemplateController {

    private final TemplateService templateService;
    // EmployeeFeignClient는 여기서는 필요 없으므로 제거하고, 권한 검사만 수행합니다.

    /**
     * 템플릿 관리 권한이 있는지 확인하는 메소드 (HR_MANAGER, ADMIN)
     * @param userInfo 토큰에서 추출된 사용자 정보
     */
    private void checkAdminPermission(TokenUserInfo userInfo) {
        // "HR_MANAGER" 또는 "ADMIN" 역할이 아니면 예외 발생
        List<String> adminRoles = List.of("HR_MANAGER", "ADMIN");
        if (userInfo == null || !adminRoles.contains(userInfo.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "템플릿을 관리할 권한이 없습니다.");
        }
    }

    /**
     * 템플릿 생성
     */
    @PostMapping("/create")
    public ResponseEntity<CommonResDto> templateCreate(
            @RequestBody @Valid TemplateCreateReqDto req,
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {
        checkAdminPermission(userInfo); // 권한 확인
        TemplateResDto res = templateService.createTemplate(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "보고서 템플릿 생성 완료", res));
    }

    /**
     * 템플릿 단건 조회
     */
    @GetMapping("/{templateId}")
    public ResponseEntity<CommonResDto> findByIdTemplate(
            @PathVariable Long templateId,
            @AuthenticationPrincipal TokenUserInfo userInfo // 조회는 모든 사용자가 가능하도록 권한 검사 제거
    ) {
        TemplateResDto res = templateService.getTemplate(templateId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "보고서 템플릿 조회 완료", res));
    }

    /**
     * 템플릿 수정
     */
    @PutMapping("/{templateId}")
    public ResponseEntity<CommonResDto> templateUpdate(
            @PathVariable Long templateId,
            @RequestBody @Valid TemplateUpdateReqDto req,
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {
        checkAdminPermission(userInfo); // 권한 확인
        TemplateResDto res = templateService.updateTemplate(templateId, req);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "템플릿 수정 완료", res));
    }

    /**
     * 템플릿 삭제
     */
    @DeleteMapping("/{templateId}")
    public ResponseEntity<CommonResDto> templateDelete(
            @PathVariable Long templateId,
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {
        checkAdminPermission(userInfo); // 권한 확인
        templateService.deleteTemplate(templateId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "템플릿 삭제 완료", null));
    }
}