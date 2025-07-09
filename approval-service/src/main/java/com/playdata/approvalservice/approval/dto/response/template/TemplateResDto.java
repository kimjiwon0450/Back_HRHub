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

    /**
     * 엔티티를 DTO로 변환하는 정적 팩토리 메소드
     * @param entity ReportTemplate 엔티티
     * @param objectMapper JSON 파싱을 위한 ObjectMapper
     * @return TemplateResDto
     * @throws JsonProcessingException 파싱 실패 시 예외
     */
    public static TemplateResDto from(ReportTemplate entity, ObjectMapper objectMapper) throws JsonProcessingException {
        JsonNode jsonTemplate = objectMapper.readTree(entity.getTemplate());
        return TemplateResDto.builder()
                .templateId(entity.getTemplateId())
                .template(jsonTemplate)
                .build();
    }
}