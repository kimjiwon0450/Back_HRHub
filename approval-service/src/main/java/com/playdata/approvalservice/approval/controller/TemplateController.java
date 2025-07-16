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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/approval/templates")
public class TemplateController {

    private final TemplateService templateService;

    /**
     * 템플릿 생성
     */
    @PreAuthorize("hasAnyRole('HR_MANAGER','ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<CommonResDto> templateCreate(
            @RequestBody @Valid TemplateCreateReqDto req,
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {
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
     * 템플릿 목록 조회 (카테고리 ID로 필터링 가능)
     * @param categoryId 쿼리 파라미터 (필수 아님)
     * @return 템플릿 목록 응답
     */
    @GetMapping("/list")
    public ResponseEntity<CommonResDto> getTemplates(
            @RequestParam(name = "categoryId", required = false) Long categoryId
    ) {
        List<TemplateResDto> res = templateService.getTemplates(categoryId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "보고서 템플릿 조회 완료", res));
    }

    /**
     * 템플릿 수정
     */
    @PreAuthorize("hasAnyRole('HR_MANAGER','ADMIN')")
    @PutMapping("/{templateId}")
    public ResponseEntity<CommonResDto> templateUpdate(
            @PathVariable Long templateId,
            @RequestBody @Valid TemplateUpdateReqDto req,
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {
        TemplateResDto res = templateService.updateTemplate(templateId, req);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "템플릿 수정 완료", res));
    }

    /**
     * 템플릿 삭제
     */
    @PreAuthorize("hasAnyRole('HR_MANAGER','ADMIN')")
    @DeleteMapping("/{templateId}")
    public ResponseEntity<CommonResDto> templateDelete(
            @PathVariable Long templateId,
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {
        templateService.deleteTemplate(templateId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "템플릿 삭제 완료", null));
    }
}