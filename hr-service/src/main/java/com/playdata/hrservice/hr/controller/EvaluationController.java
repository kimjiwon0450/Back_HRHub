package com.playdata.hrservice.hr.controller;

import com.playdata.hrservice.common.dto.CommonResDto;
import com.playdata.hrservice.hr.dto.EvaluationReqDto;
import com.playdata.hrservice.hr.entity.Evaluation;
import com.playdata.hrservice.hr.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hr-service")
@RequiredArgsConstructor
@Slf4j
public class EvaluationController {
    private final EvaluationService evaluationService;

    @PostMapping("/evaluation/{id}")
    public ResponseEntity<CommonResDto> evaluateEmployee(@PathVariable Long id, @RequestBody EvaluationReqDto dto) {

    }
}
