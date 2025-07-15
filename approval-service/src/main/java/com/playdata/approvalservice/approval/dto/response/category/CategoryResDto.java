package com.playdata.approvalservice.approval.dto.response.category;

import com.playdata.approvalservice.approval.entity.TemplateCategory;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryResDto {
    private Long id;
    private String categoryName;
    private String categoryDescription;

    // Entity -> DTO 변환을 위한 정적 팩토리 메소드
    public static CategoryResDto from(TemplateCategory category) {
        return CategoryResDto.builder()
                .id(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .categoryDescription(category.getCategoryDescription())
                .build();
    }
}
