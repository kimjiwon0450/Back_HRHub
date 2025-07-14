package com.playdata.approvalservice.approval.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TemplateCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long CategoryId;

    @Setter
    @Column(nullable = false, unique = true)
    private String categoryName;

    @Setter
    private String description;

    @Builder
    public TemplateCategory(String categoryName, String description) {
        this.categoryName = categoryName;
        this.description = description;
    }
}

