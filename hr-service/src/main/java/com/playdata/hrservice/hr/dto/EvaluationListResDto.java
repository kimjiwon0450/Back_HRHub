package com.playdata.hrservice.hr.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationListResDto {
    private Long evaluationId;
    private Long evaluatorId;
    private LocalDate interviewDate;
    private LocalDateTime createdAt;
    private double totalEvaluation;
}
