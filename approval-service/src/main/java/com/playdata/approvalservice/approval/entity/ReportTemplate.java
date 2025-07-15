package com.playdata.approvalservice.approval.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "report_template")
public class ReportTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "template", nullable = false, columnDefinition = "JSON")
    private String template;

    @ManyToOne(fetch = FetchType.LAZY) // 템플릿 조회 시 카테고리는 필요할 때만 가져오도록 지연 로딩
    @JoinColumn(name = "category_id", nullable = false) // 외래 키 설정
    private TemplateCategory categoryId;

    @Builder
    public ReportTemplate(String template, TemplateCategory categoryId) {
        this.template = template;
        this.categoryId = categoryId;
    }

}