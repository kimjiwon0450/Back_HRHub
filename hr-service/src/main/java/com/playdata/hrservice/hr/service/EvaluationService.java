package com.playdata.hrservice.hr.service;

import com.playdata.hrservice.hr.dto.EvaluationReqDto;
import com.playdata.hrservice.hr.dto.EvaluationResDto;
import com.playdata.hrservice.hr.entity.Employee;
import com.playdata.hrservice.hr.entity.Evaluation;
import com.playdata.hrservice.hr.repository.EvalueationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EmployeeService employeeService;
    private final EvalueationRepository evalueationRepository;

    public void evaluateEmployee(Long id, EvaluationReqDto dto) {
        Employee evaluatee = employeeService.findById(id);
        Employee evaluator = employeeService.findById(dto.getEvaluatorId());

        Evaluation evaluation = Evaluation.builder()
                .evaluatee(evaluatee)
                .evaluator(evaluator)
                .template(dto.getTemplate())
                .comment(dto.getComment())
                .totalEvaluation(dto.getTotalEvaluation())
                .interviewDate(dto.getInterviewDate())
                .build();

        evalueationRepository.save(evaluation);
    }

    public EvaluationResDto getLatestEvaluation(Long id) {
        Evaluation evaluation = evalueationRepository.findByEvaluatee(employeeService.findById(id)).orElseThrow(
                () -> new RuntimeException("No evaluation found for id: " + id)
        );

        return evaluation.toDto();
    }
}
