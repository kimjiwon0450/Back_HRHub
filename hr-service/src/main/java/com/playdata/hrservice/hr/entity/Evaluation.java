package com.playdata.hrservice.hr.entity;

import com.playdata.hrservice.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

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
    private double total_evaluation;


}
