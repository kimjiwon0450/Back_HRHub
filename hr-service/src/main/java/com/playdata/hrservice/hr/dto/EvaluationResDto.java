package com.playdata.hrservice.hr.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EvaluationResDto {
    private Long evaluationId;
    private Long evaluateeId;
    private Long evaluatorId;
    private Map<String, Object> template;
    private String comment;
    private LocalDate interviewDate;
    private double totalEvaluation;
    private String updateMemo;
}
