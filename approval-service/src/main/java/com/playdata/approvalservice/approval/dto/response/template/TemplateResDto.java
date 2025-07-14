package com.playdata.approvalservice.approval.dto.response.template;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playdata.approvalservice.approval.entity.ReportTemplate;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class TemplateResDto {
    private Long templateId;
    private JsonNode template;
    private Long categoryId;
    private String categoryName;


    /**
     * 엔티티를 DTO로 변환하는 정적 팩토리 메소드
     * @param reportTemplate
     * @param objectMapper
     * @return
     * @throws JsonProcessingException
     */
    // from 정적 팩토리 메소드 수정
    public static TemplateResDto from(ReportTemplate reportTemplate, ObjectMapper objectMapper) throws JsonProcessingException {
        return TemplateResDto.builder()
                .templateId(reportTemplate.getTemplateId())
                .template(objectMapper.readTree(reportTemplate.getTemplate()))
                .categoryId(reportTemplate.getCategory().getId()) // 카테고리 정보 추가
                .categoryName(reportTemplate.getCategory().getName()) // 카테고리 정보 추가
                .build();
    }
}