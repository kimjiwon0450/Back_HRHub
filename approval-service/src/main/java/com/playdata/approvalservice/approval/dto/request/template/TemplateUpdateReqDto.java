package com.playdata.approvalservice.approval.dto.request.template;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class TemplateUpdateReqDto {

    @NotNull
    private JsonNode template;
}
