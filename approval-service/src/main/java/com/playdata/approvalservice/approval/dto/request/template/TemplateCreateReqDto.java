package com.playdata.approvalservice.approval.dto.request.template;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class TemplateCreateReqDto {
    @NotNull
    private JsonNode template;

    private Long categoryId;
}
