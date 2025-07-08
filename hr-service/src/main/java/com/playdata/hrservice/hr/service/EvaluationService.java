package com.playdata.hrservice.hr.service;

import com.playdata.hrservice.hr.dto.EvaluationListResDto;
import com.playdata.hrservice.hr.dto.EvaluationReqDto;
import com.playdata.hrservice.hr.dto.EvaluationResDto;
import com.playdata.hrservice.hr.entity.Employee;
import com.playdata.hrservice.hr.entity.Evaluation;
import com.playdata.hrservice.hr.repository.EvaluationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EmployeeService employeeService;
    private final EvaluationRepository evaluationRepository;


    public void evaluateEmployee(Long id, EvaluationReqDto dto) {
        Employee evaluatee = employeeService.findById(id);
        Employee evaluator = employeeService.findById(dto.getEvaluatorId());

        // 1. 이번 달의 시작, 끝 계산
        YearMonth thisMonth = YearMonth.now();
        LocalDateTime monthStart = thisMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = thisMonth.atEndOfMonth().atTime(23, 59, 59);

        // 2. 이번 달에 평가 기록이 있는지 확인
        boolean exists = evaluationRepository.existsByEvaluateeAndCreatedAtBetween(evaluatee, monthStart, monthEnd);

        if (exists) {
            throw new IllegalStateException("해당 직원의 이번 달 평가가 이미 존재합니다.");
        }

        // 3. 평가 등록
        Evaluation evaluation = Evaluation.builder()
                .evaluatee(evaluatee)
                .evaluator(evaluator)
                .template(dto.getTemplate())
                .comment(dto.getComment())
                .totalEvaluation(dto.getTotalEvaluation())
                .interviewDate(dto.getInterviewDate())
                .updateMemo("해당 없음.")
                .build();

        evaluationRepository.save(evaluation);
    }

    // 평가 수정
    public void updateEvaluation(Long evaluationId, EvaluationReqDto dto) {
        Employee evaluatee = employeeService.findById(dto.getEvaluateeId());
        Employee evaluator = employeeService.findById(dto.getEvaluatorId());
        // 이번 달의 시작, 끝 계산
        YearMonth thisMonth = YearMonth.now();
        LocalDateTime monthStart = thisMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = thisMonth.atEndOfMonth().atTime(23, 59, 59);

        Evaluation evaluation = evaluationRepository
                .findTopByEvaluateeAndCreatedAtBetweenOrderByCreatedAtDesc(evaluatee, monthStart, monthEnd)
                .orElseThrow(() -> new RuntimeException("이번 달의 평가가 존재하지 않습니다."));
        evaluation.updateEvaluator(evaluator);
        evaluation.updateFromReqDto(dto);
        evaluationRepository.save(evaluation);
    }

    // 평가 조회
    public EvaluationResDto getLatestEvaluation(Long id) {
        Employee evaluatee = employeeService.findById(id);

        // 이번 달의 시작, 끝 계산
        YearMonth thisMonth = YearMonth.now();
        LocalDateTime monthStart = thisMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = thisMonth.atEndOfMonth().atTime(23, 59, 59);

        Evaluation evaluation = evaluationRepository
                .findTopByEvaluateeAndCreatedAtBetweenOrderByCreatedAtDesc(evaluatee, monthStart, monthEnd)
                .orElseThrow(() -> new RuntimeException("이번 달의 평가가 존재하지 않습니다."));

        return evaluation.toDto();
    }

    public Page<EvaluationListResDto> getEvaluationListByEmployeeId(Long employeeId, Pageable pageable) {

        Page<Evaluation> evaluationPage = evaluationRepository.findAllByEvaluatee(
                employeeService.findById(employeeId), pageable
        );

        Page<EvaluationListResDto> resDtos = evaluationPage.map(
                evaluation -> EvaluationListResDto.builder()
                        .evaluationId(evaluation.getId())
                        .evaluatorId(evaluation.getEvaluator().getEmployeeId())
                        .interviewDate(evaluation.getInterviewDate())
                        .totalEvaluation(evaluation.getTotalEvaluation())
                        .createdAt(evaluation.getCreatedAt())
                        .build()
        );
        return resDtos;
    }

    public EvaluationResDto getEvaluationByEvaluationId(Long evaluationId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId).orElseThrow(
                () -> new EntityNotFoundException("해당 평가가 존재하지 않습니다.")
        );
        return evaluation.toDto();
    }
}
