package com.playdata.approvalservice.approval.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playdata.approvalservice.approval.dto.request.template.TemplateCreateReqDto;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter // updateTemplate에서 setter를 사용하므로 유지합니다.
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

    @Lob // JSON 데이터가 길어질 수 있으므로 TEXT 타입으로 매핑
    @Column(name = "template", nullable = false, columnDefinition = "JSON")
    private String template;

    /**
     * DTO를 엔티티로 변환하는 정적 팩토리 메소드
     * @param dto 생성 요청 DTO
     * @param objectMapper JSON 직렬화를 위한 ObjectMapper
     * @return ReportTemplate 엔티티
     * @throws JsonProcessingException 직렬화 실패 시 예외
     */
    public static ReportTemplate of(TemplateCreateReqDto dto, ObjectMapper objectMapper) throws JsonProcessingException {
        String jsonTemplate = objectMapper.writeValueAsString(dto.getTemplate());
        return ReportTemplate.builder()
                .template(jsonTemplate)
                .build();
    }
}