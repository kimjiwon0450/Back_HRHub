package com.playdata.approvalservice.approval.repository;

import com.playdata.approvalservice.approval.entity.TemplateCategory;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Optional;

public interface TemplateCategoryRepository {

    static List<TemplateCategory> findAll() {
        return null;
    }

    static Optional<Object> findByCategoryName(@NotBlank(message = "카테고리 이름은 필수입니다.") String categoryName) {
    }

    static TemplateCategory save(TemplateCategory newCategory) {
    }

    static void delete(TemplateCategory category) {
    }

    static Optional<Object> findById(Long categoryId) {
    }

    Optional<TemplateCategory> findByCategoryId(Long categoryId);
}
