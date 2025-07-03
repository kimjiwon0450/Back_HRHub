package com.playdata.hrservice.hr.dto;

import com.playdata.hrservice.hr.entity.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class EvaluationReqDto {
    private Long evaluateeId;
    private Long evaluatorId;
    private String template;
    private String comment;
    private LocalDate interviewDate;
    private double totalEvaluation;
}
