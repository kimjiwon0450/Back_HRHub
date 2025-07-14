package com.playdata.approvalservice.approval.controller;

import com.playdata.approvalservice.approval.dto.request.category.CategoryCreateReqDto;
import com.playdata.approvalservice.approval.dto.request.category.CategoryUpdateReqDto;
import com.playdata.approvalservice.approval.dto.response.category.CategoryResDto;
import com.playdata.approvalservice.approval.service.TemplateCategoryService;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/approval/category")
public class TemplateCategoryController {

    private final TemplateCategoryService categoryService;

    /**
     * 모든 카테고리 목록 조회
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommonResDto> getAllCategories() {
        List<CategoryResDto> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "템플릿 카테고리 목록 조회 성공", categories)
        );
    }

    /**
     * 카테고리 단건 조회
     */
    @GetMapping("/{categoryId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommonResDto> getCategoryById(@PathVariable Long categoryId) {
        CategoryResDto category = categoryService.getCategoryById(categoryId);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "템플릿 카테고리 조회 성공", category)
        );
    }

    /**
     * 새 카테고리 생성 (관리자 권한)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('HR_MANAGER','ADMIN')")
    public ResponseEntity<CommonResDto> createCategory(
            @RequestBody @Valid CategoryCreateReqDto req,
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {
        CategoryResDto createdCategory = categoryService.createCategory(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "새로운 카테고리가 생성되었습니다.", createdCategory));
    }

    /**
     * 카테고리 수정 (관리자 권한)
     */
    @PutMapping("/{categoryId}")
    @PreAuthorize("hasAnyRole('HR_MANAGER','ADMIN')")
    public ResponseEntity<CommonResDto> updateCategory(
            @PathVariable Long categoryId,
            @RequestBody @Valid CategoryUpdateReqDto req,
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {
        CategoryResDto updatedCategory = categoryService.updateCategory(categoryId, req);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "카테고리 정보가 수정되었습니다.", updatedCategory)
        );
    }

    /**
     * 카테고리 삭제 (관리자 권한)
     */
    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasAnyRole('HR_MANAGER','ADMIN')")
    public ResponseEntity<CommonResDto> deleteCategory(
            @PathVariable Long categoryId,
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "카테고리가 삭제되었습니다.", null)
        );
    }
}
