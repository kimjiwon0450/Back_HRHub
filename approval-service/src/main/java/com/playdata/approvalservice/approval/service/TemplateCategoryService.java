package com.playdata.approvalservice.approval.service;

 // 이 서비스도 새로 만들어야 합니다.
import com.playdata.approvalservice.approval.dto.request.category.CategoryCreateReqDto;
import com.playdata.approvalservice.approval.dto.request.category.CategoryUpdateReqDto;
import com.playdata.approvalservice.approval.dto.response.category.CategoryResDto;
import com.playdata.approvalservice.approval.entity.TemplateCategory;
import com.playdata.approvalservice.approval.repository.TemplateCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TemplateCategoryService {

    private final TemplateCategoryService categoryService;

    /**
     * 모든 카테고리 목록 조회
     */
    public List<CategoryResDto> getAllCategories() {
        List<TemplateCategory> categories = TemplateCategoryRepository.findAll();
        return categories.stream()
                .map(CategoryResDto::from) // 정적 팩토리 메소드 사용
                .collect(Collectors.toList());
    }

    /**
     * 카테고리 단건 조회
     */
    public CategoryResDto getCategoryById(Long categoryId) {
        TemplateCategory category = findCategoryById(categoryId);
        return CategoryResDto.from(category);
    }

    /**
     * 새 카테고리 생성
     */
    @Transactional
    public CategoryResDto createCategory(CategoryCreateReqDto req) {
        // 혹시 모를 중복 이름 체크 (unique 제약 조건이 있지만, 더 친절한 예외 메시지를 위해)
        if (TemplateCategoryRepository.findByCategoryName(req.getCategoryName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 카테고리 이름입니다: " + req.getName());
        }

        TemplateCategory newCategory = TemplateCategory.builder()
                .name(req.getCategoryName())
                .description(req.getDescription())
                .build();

        TemplateCategory savedCategory = TemplateCategoryRepository.save(newCategory);
        log.info("New category created: id={}, name={}", savedCategory.getId(), savedCategory.getName());
        return CategoryResDto.from(savedCategory);
    }

    /**
     * 카테고리 수정
     */
    @Transactional
    public CategoryResDto updateCategory(Long categoryId, CategoryUpdateReqDto req) {
        TemplateCategory category = findCategoryById(categoryId);

        // 요청 DTO의 필드가 null이 아닐 경우에만 업데이트 (부분 수정 지원)
        if (req.getCategoryName() != null) {
            // 수정하려는 이름이 현재 이름과 다르고, 다른 카테고리에 이미 존재하는 이름인지 확인
            if (!req.getCategoryName().equals(category.getName()) && TemplateCategoryRepository.findByCategoryName(req.getCategoryName()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 카테고리 이름입니다: " + req.getCategoryName());
            }
            category.setCategoryName(req.getCategoryName());
        }
        if (req.getDescription() != null) {
            category.setDescription(req.getDescription());
        }

        // categoryRepository.save(category)는 @Transactional에 의해 더티 체킹되므로 생략 가능
        log.info("Category updated: id={}, name={}", category.getId(), category.getName());
        return CategoryResDto.from(category);
    }

    /**
     * 카테고리 삭제
     */
    @Transactional
    public void deleteCategory(Long categoryId) {
        TemplateCategory category = findCategoryById(categoryId);

        // 추가적인 비즈니스 로직: 해당 카테고리에 속한 템플릿이 하나라도 있으면 삭제를 막는다.
        // 이 로직을 위해서는 ReportTemplateRepository에 countByCategoryId 같은 메소드가 필요합니다.
        // if (reportTemplateRepository.countByCategoryId(categoryId) > 0) {
        //     throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 카테고리에 속한 템플릿이 있어 삭제할 수 없습니다.");
        // }

        TemplateCategoryRepository.delete(category);
        log.info("Category deleted: id={}", categoryId);
    }

    /**
     * (private 헬퍼 메소드) ID로 카테고리를 찾는 중복 로직 분리
     */
    private TemplateCategory findCategoryById(Long categoryId) {
        return TemplateCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다. id=" + categoryId));
    }
}