package com.playdata.approvalservice.approval.service;

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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TemplateCategoryService {

    private final TemplateCategoryRepository categoryRepository;

    public List<CategoryResDto> getAllCategories() {
        List<TemplateCategory> categories = categoryRepository.findAll();
        return categories.stream()
                .map(CategoryResDto::from)
                .collect(Collectors.toList());
    }

    public CategoryResDto getCategoryById(Long categoryId) {
        TemplateCategory category = findCategoryById(categoryId);
        return CategoryResDto.from(category);
    }

    @Transactional
    public CategoryResDto createCategory(CategoryCreateReqDto req) {

        if (categoryRepository.findByCategoryName(req.getCategoryName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 카테고리 이름입니다: " + req.getCategoryName());
        }

        TemplateCategory newCategory = TemplateCategory.builder()
                .categoryName(req.getCategoryName())
                .description(req.getDescription())
                .build();

        TemplateCategory savedCategory = categoryRepository.save(newCategory);
        log.info("New category created: id={}, name={}", savedCategory.getCategoryId(), savedCategory.getCategoryName());
        return CategoryResDto.from(savedCategory);
    }

    @Transactional
    public CategoryResDto updateCategory(Long categoryId, CategoryUpdateReqDto req) {
        TemplateCategory category = findCategoryById(categoryId);


        if (req.getCategoryName() != null && !req.getCategoryName().isBlank()) {

            if (!req.getCategoryName().equals(category.getCategoryName()) &&
                    categoryRepository.findByCategoryName(req.getCategoryName()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 카테고리 이름입니다: " + req.getCategoryName());
            }
            category.setCategoryName(req.getCategoryName());
        }
        if (req.getDescription() != null) {
            category.setDescription(req.getDescription());
        }


        log.info("Category updated: id={}, name={}", category.getCategoryId(), category.getCategoryName());
        return CategoryResDto.from(category);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        categoryRepository.deleteById(categoryId);
        log.info("Category deleted: id={}", categoryId);
    }

    private TemplateCategory findCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다. id=" + categoryId));
    }
}