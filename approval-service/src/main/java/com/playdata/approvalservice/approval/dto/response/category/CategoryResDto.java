package com.playdata.approvalservice.approval.dto.response.category;

import com.playdata.approvalservice.approval.entity.TemplateCategory;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryResDto {
    private Long id;
    private String name;
    private String description;

    // Entity -> DTO 변환을 위한 정적 팩토리 메소드
    public static CategoryResDto from(TemplateCategory category) {
        return CategoryResDto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}
