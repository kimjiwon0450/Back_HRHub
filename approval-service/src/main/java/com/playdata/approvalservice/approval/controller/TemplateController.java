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

@RestController
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('HR_MANAGER','ADMIN')")
@RequestMapping("/approval-service/templates")
public class TemplateController {

    private final TemplateService templateService;

    /**
     * 템플릿 생성
     */
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
     * 템플릿 수정
     */
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
    @DeleteMapping("/{templateId}")
    public ResponseEntity<CommonResDto> templateDelete(
            @PathVariable Long templateId,
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {
        templateService.deleteTemplate(templateId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "템플릿 삭제 완료", null));
    }
}