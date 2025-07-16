package com.playdata.approvalservice.approval.dto.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSaveReqDto {
    @NotBlank
    private String title;

    private String content;

    private Long templateId;

    private List<ApprovalLineReqDto> approvalLine;

    private List<ReferenceJsonReqDto> references;

    private String reportTemplateData;

}