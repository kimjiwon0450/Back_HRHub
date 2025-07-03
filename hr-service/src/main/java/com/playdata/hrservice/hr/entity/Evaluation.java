package com.playdata.hrservice.hr.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playdata.hrservice.common.entity.BaseTimeEntity;
import com.playdata.hrservice.hr.dto.EvaluationResDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Map;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Evaluation extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "evaluateeId")
    private Employee evaluatee;

    @ManyToOne
    @JoinColumn(name = "evaluatorId")
    private Employee evaluator;

    @Column(nullable = false, columnDefinition = "json")
    private String template;
//    private double leadership;
//    private double creativity;
//    private double cooperation;
//    private double problem_solving;
    private String comment;
    private double totalEvaluation;

    private LocalDate interviewDate;


    public EvaluationResDto toDto() {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> templateMap = null;
        try {
            templateMap = objectMapper.readValue(this.getTemplate(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            // 예외처리: 실제 운영에서는 로깅 또는 디폴트 값 반환
            templateMap = Map.of();
        }

        return EvaluationResDto.builder()
                .evaluationId(this.getId())
                .evaluateeId(this.getEvaluatee().getEmployeeId())
                .evaluatorId(this.getEvaluator().getEmployeeId())
                .template(templateMap) // Map으로 넣어줌!
                .comment(this.getComment())
                .interviewDate(this.getInterviewDate())
                .totalEvaluation(this.totalEvaluation)
                .build();
    }
}
