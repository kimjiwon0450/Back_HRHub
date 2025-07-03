package com.playdata.hrservice.hr.dto;

import lombok.*;

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
    private String template;
    private String comment;
    private double total_evaluation;
}
