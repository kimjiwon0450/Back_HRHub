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
    public List<EmployeeResDto> getEmployeesOfTop3(YearMonth targetMonth) {

        // 1. 지난 달 평가 데이터 조회 (최신 평가 기준) -> 스케쥴러에서 변수로 month를 인자로 받음
        LocalDateTime monthStart = targetMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = targetMonth.atEndOfMonth().atTime(23, 59, 59);

        // 평가가 없을 수도 있으므로 Optional 처리
        List<Evaluation> targetMonthEvaluations = evaluationRepository.findAllByCreatedAtBetweenOrderByTotalEvaluationDesc(monthStart, monthEnd)
                .orElse(List.of()); // 평가가 없으면 빈 리스트 반환 (IllegalArgumentException 대신)

        if (targetMonthEvaluations.isEmpty()) {
            log.info("이번 달 평가 데이터가 없습니다. 상위 3명 조회 불가.", targetMonth);
            return List.of(); // 빈 리스트 반환
        }

        // 2. 상위 3명까지의 리스트와 3위 점수 확인
        List<Evaluation> currentTopEvaluations = targetMonthEvaluations.stream().limit(3).toList();

        // 3등까지 평가가 없는 경우 (예: 평가자가 1명이나 2명인 경우)
        if (currentTopEvaluations.size() < 3) {
            log.info("이번 달 평가자 수가 3명 미만입니다. 현재까지의 상위 평가자만 반환합니다.");
            return currentTopEvaluations.stream()
                    .map(eval -> employeeService.findById(eval.getEvaluatee().getEmployeeId()).toDto())
                    .collect(Collectors.toList());
        }

        // 3. 3위 동점자 후보 필터링 (3위 점수와 같은 평가자)

        // 3위의 점수
        double thirdPlaceScore = currentTopEvaluations.get(2).getTotalEvaluation();

        // 3위와 점수 같은 평가를 걸러내기.(공동 3위가 3명이라면??)
        // 1등, 2등 아이디
        Set<Long> top2Ids = currentTopEvaluations.stream().limit(2).map(e->e.getEvaluatee().getEmployeeId()).collect(Collectors.toSet());

        // 3위 동점 후보 걸러내기
        List<Evaluation>  thirdEvaluationsList = targetMonthEvaluations.stream()
                .filter(eval -> Double.compare(eval.getTotalEvaluation(),thirdPlaceScore)==0)
                .filter(eval -> !top2Ids.contains(eval.getEvaluatee().getEmployeeId()))
                .toList();
// 1위 ~ 3위까지 동점일때
        if(thirdEvaluationsList.isEmpty()) {
            thirdEvaluationsList =
                    currentTopEvaluations.stream()
                            .skip(2)
                            .limit(1)
                            .toList();
        }

        // ---------------------------------------동점자 지난달 평가 비교-----------------------------------------------------

        // 4. 지난 달 평가 데이터 조회 (동점자 처리를 위한 준비)
        YearMonth prevMonth = targetMonth.minusMonths(1);
        LocalDateTime prevMonthStart = prevMonth.atDay(1).atStartOfDay();
        LocalDateTime prevMonthEnd = prevMonth.atEndOfMonth().atTime(23, 59, 59);

        // Map<EmployeeId, Evaluation> 형태.
        // 신입 직원 (지난달 평가 없음)은 기본적으로 0점으로 처리될 수 있도록 getOrDefault 사용
        Map<Long, Double> prevMonthScoresMap = new HashMap<>();

        // thirdPlaceCandidates에 있는 모든 직원에 대해 지난달 평가를 찾아 Map에 저장
        for (Evaluation thirdEval : thirdEvaluationsList) {
            Employee employee = employeeService.findById(thirdEval.getEvaluatee().getEmployeeId());// 평가자의 아이디를 넣어 동점자 마다 Employee 인스턴스로 받아옴.
            double score = evaluationRepository.findTopByEvaluateeAndCreatedAtBetweenOrderByCreatedAtDesc(employee, prevMonthStart, prevMonthEnd) // findTopby 인 이유는 평가가 수정된 기록도 있기때문에
                    .map(Evaluation::getTotalEvaluation)// 점수가 있으면 그값을 넣고
                    .orElse(0.0); // 이전 달 점수가 없으면 0.0점을 넣을거임.
            prevMonthScoresMap.put(employee.getEmployeeId(), score);
            }


        // 5. Map<이번달 동점자 id, 지난달 평가점수> 가 들어온 형태
        // 1) 최고 점수 확인, 2) 같은 점수가 있는지 확인

        //1) 해당 Map 안에서 가장 높은 점 수 확인
        double bestPrevScore = prevMonthScoresMap.values().stream()
                .max(Double::compare)
                .orElse(0.0);

        // 2) 같은 점수가 Map 안에 있는 지 확인해서 employeeId를 List에 담음
        List<Long> finalThirdIds = prevMonthScoresMap.entrySet().stream()
                .filter(e -> Double.compare(e.getValue(), bestPrevScore)==0) //  동점자 필터, Double.compare(1, 2)는 두 수의 차이를 값으로 알려주는 거임
                .map(Map.Entry::getKey)                                         // employeeId
                .toList();

        // employee List에 담음
        List<EmployeeResDto> thirdRankDtos = finalThirdIds.stream()
                .map(id -> employeeService.findById(id).toDto())
                .toList();

        // 최종
        //  1,2 등과 3등(들) 을 합쳐서 반환 해줄 거임.
        Set<Long> allIds = new LinkedHashSet<>();
        allIds.addAll(top2Ids);
        allIds.addAll(finalThirdIds);


        return allIds.stream()
                .map(id->employeeService.findById(id).toDto())
                .toList();
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



