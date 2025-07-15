package com.playdata.approvalservice.approval.dto.request.category;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class CategoryCreateReqDto {

    @NotBlank(message = "카테고리 이름은 필수입니다.")
    private String categoryName;
    private String categoryDescription;
}