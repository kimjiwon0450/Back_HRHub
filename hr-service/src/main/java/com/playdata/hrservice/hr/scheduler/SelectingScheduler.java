package com.playdata.hrservice.hr.scheduler;

import com.playdata.hrservice.hr.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
@RequiredArgsConstructor
public class SelectingScheduler {

    private final EvaluationService evaluationService;

    @Scheduled(cron =
    "0 0 0 1 * ?", zone = "Asia/Seoul") // 매월 1일 자정에 실행, 서울시간 기준
    public void pickTop3Members(){

        YearMonth targetMonth = YearMonth.now().minusMonths(1); // 지난 달

        evaluationService.getEmployeesOfTop3(targetMonth);
    }

}
