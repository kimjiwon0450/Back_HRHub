package com.playdata.approvalservice.approval.dto.response.template;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class TemplateResDto {
    private Long templateId;
    private JsonNode template;
}
