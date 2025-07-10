package com.playdata.hrservice.hr.service;

import com.playdata.hrservice.hr.dto.EmployeeResDto;
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
import software.amazon.awssdk.regions.regionmetadata.EuCentral1;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

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

    // 이달의 사원
    public List<EmployeeResDto> getEmployeesOfTop3() {

        // 1. 이번 달 평가 데이터 조회 (최신 평가 기준)
        YearMonth thisMonth = YearMonth.now();
        LocalDateTime monthStart = thisMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = thisMonth.atEndOfMonth().atTime(23, 59, 59);

        // 평가가 없을 수도 있으므로 Optional 처리
        List<Evaluation> thisMonthEvaluations = evaluationRepository.findAllByCreatedAtBetweenOrderByTotalEvaluationDesc(monthStart, monthEnd)
                .orElse(List.of()); // 평가가 없으면 빈 리스트 반환 (IllegalArgumentException 대신)

        if (thisMonthEvaluations.isEmpty()) {
            log.info("이번 달 평가 데이터가 없습니다. 상위 3명 조회 불가.");
            return List.of(); // 빈 리스트 반환
        }

        // 2. 상위 3명까지의 리스트와 3위 점수 확인
        List<Evaluation> currentTopEvaluations = thisMonthEvaluations.stream().limit(3).toList();

        // 3등까지 평가가 없는 경우 (예: 평가자가 1명이나 2명인 경우)
        if (currentTopEvaluations.size() < 3) {
            log.info("이번 달 평가자 수가 3명 미만입니다. 현재까지의 상위 평가자만 반환합니다.");
            return currentTopEvaluations.stream()
                    .map(eval -> employeeService.findById(eval.getEvaluatee().getEmployeeId()).toDto())
                    .collect(Collectors.toList());
        }



        // 3위의 점수
        double thirdPlaceScore = currentTopEvaluations.get(2).getTotalEvaluation();

        // 3. 3위 동점자 후보 필터링 (3위 점수와 같거나 높은 평가자들)
        // 1, 2위 + 3위 점수와 같고, 3위 아래에 있는 사람들까지 모두 포함
        List<Evaluation> potentialThirdPlaceCandidates = thisMonthEvaluations.stream()
                .filter(eval -> eval.getTotalEvaluation() >= thirdPlaceScore)
                .toList();


        // ---------------------------------------지난 달-----------------------------------------------------

        // 4. 지난 달 평가 데이터 조회 (동점자 처리를 위한 준비)
        YearMonth prevMonth = thisMonth.minusMonths(1);
        LocalDateTime prevMonthStart = prevMonth.atDay(1).atStartOfDay();
        LocalDateTime prevMonthEnd = prevMonth.atEndOfMonth().atTime(23, 59, 59);

        // Map<EmployeeId, Evaluation> 형태로 지난달 평가를 미리 가져옴 (성능 최적화)
        // 신입 직원 (지난달 평가 없음)은 기본적으로 0점으로 처리될 수 있도록 getOrDefault 사용
        Map<Long, Double> prevMonthScoresMap = new HashMap<>();
        // potentialThirdPlaceCandidates에 있는 모든 직원에 대해 지난달 평가를 찾아 Map에 저장
        for (Evaluation eval : potentialThirdPlaceCandidates) {
            Employee employee = employeeService.findById(eval.getEvaluatee().getEmployeeId());// 평가자의 아이디를 넣어 Employee 로 받아옴.
            double socre = evaluationRepository.findTopByEvaluateeAndCreatedAtBetweenOrderByCreatedAtDesc(employee, prevMonthStart, prevMonthEnd)
                    .map(Evaluation::getTotalEvaluation)// 점수가 있으면 그값을 넣고
                    .orElse(0.0); // 이전 달 점수가 없으면 0.0점을 넣을거임.
            prevMonthScoresMap.put(eval.getEvaluatee().getEmployeeId(), socre);

            }


        // 5. Map<이번달 동점자 id, 지난달 평가점수> 가 들어왔으니 그걸로 1) 최고 점수 확인, 2) 같은 점수가 있는지 확인
        // 1) 최고 점수 확인
        double bestPrevScore = prevMonthScoresMap.values().stream()
                .max(Double::compare)
                .orElse(0.0);
        // 2) 같은 점수가 Map 안에 있는 지 확인해서 List에 담음
        List<Long> finalThirdIds = prevMonthScoresMap.entrySet().stream()
                .filter(e -> Double.compare(e.getValue(), bestPrevScore) == 0) // 동점만
                .map(Map.Entry::getKey)                                         // employeeId
                .toList();

        // employee List에 담음
        List<EmployeeResDto> thirdRankDtos = finalThirdIds.stream()
                .map(id -> employeeService.findById(id).toDto())
                .toList();

        return thirdRankDtos;
    }
}


    // -----------------------------------------------------------------------------------------------------------------------------------

//    public List<EmployeeResDto> getEmployeesOfTop3() {
//
//        // 이번 달의 시작, 끝 계산
//        YearMonth thisMonth = YearMonth.now();
//        LocalDateTime monthStart = thisMonth.atDay(1).atStartOfDay();
//        LocalDateTime monthEnd = thisMonth.atEndOfMonth().atTime(23, 59, 59);
//        List<Evaluation> thisMonthEvaluations = evaluationRepository.findAllByCreatedAtBetweenOrderByTotalEvaluationDesc(monthStart, monthEnd)
//                .orElseThrow(() -> new IllegalArgumentException("평가를 찾을 수 없습니다!"));
//
//        // 상위 3건만 잘라내기
//        List<Evaluation> top3 = thisMonthEvaluations.stream().limit(3).toList();
//
//        // 3위 점수 기준으로 동점자가 있는지 확인
//        if (top3.size() == 3) {
//            int thirdScore = (int) top3.get(2).getTotalEvaluation();
//
//            // skip(3) : 4번째부터 필터링하여서 3등이랑 같은 점수가 있으면 equalList에 담아놓음(Type: Evaluation)
//            List<Evaluation> equalList = thisMonthEvaluations.stream().skip(3).filter(ev -> ev.getTotalEvaluation() == thirdScore).toList();
//
//            if (!equalList.isEmpty()) {
//                List<Long> candidateIds = new ArrayList<>();
//
//
//                // 이전 달
//                YearMonth prevMonth = thisMonth.minusMonths(1);
//                LocalDateTime prevMonthStart = prevMonth.atDay(1).atStartOfDay();
//                LocalDateTime prevMonthEnd = prevMonth.atEndOfMonth().atTime(23, 59, 59);
//
//                //이전 달 평가 가져오기
//                List<Evaluation> preEvalList = (List<Evaluation>) equalList.stream().map(
//                        e -> evaluationRepository.findTopByEvaluateeAndCreatedAtBetweenOrderByCreatedAtDesc(
//                                e.getEvaluatee(), prevMonthStart, prevMonthEnd
//                        ).orElseThrow(
//                                () -> new IllegalArgumentException("조회되지 않습니다.") // 해당부분은 따로 처리해야할것. (신입들은 이전달이 없음)
//                        )).toList();
//
//
//                // 이전 달 평가 기록 비교
//
//
//            }
//        }
//
//    return null;
//    }
    // ---------------------------------------------------------------------------------------------------------------------
//        List<EmployeeResDto> resDto = new ArrayList<>();
//
//        for (Evaluation e : evaluations) {
//            resDto.add(
//                    employeeService.findById(e.getEvaluatee().getEmployeeId()).toDto()
//                     );
//        }
//
//        return resDto;



