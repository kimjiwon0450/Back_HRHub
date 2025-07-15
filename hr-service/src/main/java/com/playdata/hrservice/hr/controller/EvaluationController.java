package com.playdata.hrservice.hr.controller;

import com.playdata.hrservice.common.dto.CommonResDto;
import com.playdata.hrservice.hr.dto.EmployeeResDto;
import com.playdata.hrservice.hr.dto.EvaluationListResDto;
import com.playdata.hrservice.hr.dto.EvaluationReqDto;
import com.playdata.hrservice.hr.dto.EvaluationResDto;
import com.playdata.hrservice.hr.entity.Evaluation;
import com.playdata.hrservice.hr.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/hr")
@RequiredArgsConstructor
@Slf4j
public class EvaluationController {
    private final EvaluationService evaluationService;

    // 인사 평가
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR_MANAGER')")
    @PostMapping("/evaluation/{id}")
    public ResponseEntity<CommonResDto> evaluateEmployee(@PathVariable Long id, @RequestBody EvaluationReqDto dto) {
        evaluationService.evaluateEmployee(id, dto);

        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "Success", null), HttpStatus.OK);
    }

    // 인사 평가 수정
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR_MANAGER')")
    @PatchMapping("/evaluation/{evaluationId}")
    public ResponseEntity<CommonResDto> updateEvaluation(@PathVariable Long evaluationId, @RequestBody EvaluationReqDto dto) {
        evaluationService.updateEvaluation(evaluationId, dto);

        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "Success", null), HttpStatus.OK);
    }

    // 인사평가 조회 By 직원 ID
    @GetMapping("/evaluation/{employeeId}")
    public ResponseEntity<CommonResDto<EvaluationResDto>> getEvaluation(@PathVariable Long employeeId) {
        CommonResDto<EvaluationResDto> commonResDto = new CommonResDto<>(HttpStatus.OK,
                "Success", evaluationService.getLatestEvaluation(employeeId));
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }

    // 인사평가 본인 이력 리스트 조회
    @GetMapping("/evaluations/{employeeId}")
    public ResponseEntity<CommonResDto<Page<EvaluationListResDto>>> getEvaluationListByEmployeeId(@PathVariable Long employeeId, Pageable pageable) {
        Page<EvaluationListResDto> dtos = evaluationService.getEvaluationListByEmployeeId(employeeId, pageable);
        log.info(dtos.toString());
        return new ResponseEntity<>(new CommonResDto<>(HttpStatus.OK, "Success", dtos), HttpStatus.OK);
    }

    // 인사평가 이력 상세 조회
    @GetMapping("/evaluation/detail/{evaluationId}")
    public ResponseEntity<CommonResDto<EvaluationResDto>> getEvaluationByEvaluationId(@PathVariable Long evaluationId) {
        EvaluationResDto dto = evaluationService.getEvaluationByEvaluationId(evaluationId);
        log.info(dto.toString());
        return new ResponseEntity<>(new CommonResDto<>(HttpStatus.OK, "Success", dto), HttpStatus.OK);
    }

    @GetMapping("/top/employee")
    public ResponseEntity<?> getTopEmployee() {
        YearMonth thisMonth = YearMonth.now();
        List<EmployeeResDto> employeesOfTop3 = evaluationService.getEmployeesOfTop3(thisMonth);


        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "Success", employeesOfTop3), HttpStatus.OK);
    }
}
