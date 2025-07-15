package com.playdata.hrservice.hr.scheduler;

import com.playdata.hrservice.hr.dto.EmployeeResDto;
import com.playdata.hrservice.hr.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SelectingScheduler {

    private final EvaluationService evaluationService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Scheduled(cron =
    "0 0 0 1 * ?", zone = "Asia/Seoul") // 매월 1일 자정에 실행, 서울시간 기준
    public void pickTop3Members(){

        YearMonth targetMonth = YearMonth.now().minusMonths(1); // 지난 달

        String key = "eom:%s:top3".formatted(targetMonth);

        List<EmployeeResDto> employeesOfTop3 = evaluationService.getEmployeesOfTop3(targetMonth);

        redisTemplate.opsForValue().set(key, employeesOfTop3, Duration.ofDays(31));

        log.info("{} Top-3 캐싱 완료(key={}, size={})",
                targetMonth, key, employeesOfTop3.size());
    }

}
