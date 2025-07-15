package com.playdata.approvalservice.approval.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "template_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TemplateCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @Setter
    @Column(name = "category_name", nullable = false, unique = true)
    private String categoryName;

    @Setter
    @Column(name = "category_description")
    private String categoryDescription;

    @Builder
    public TemplateCategory(String categoryName, String categoryDescription) {
        this.categoryName = categoryName;
        this.categoryDescription = categoryDescription;
    }
}

