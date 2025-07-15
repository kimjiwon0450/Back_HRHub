package com.playdata.hrservice.hr.service;

import com.playdata.hrservice.hr.dto.EmployeeResDto;
import lombok.RequiredArgsConstructor;
import org.hibernate.service.JavaServiceLoadable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PickTop3CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final EvaluationService evaluationService;

    private static final Duration TTL = Duration.ofDays(5);

    public List<EmployeeResDto> getCurrentTop3(){
        YearMonth ym = YearMonth.now().minusMonths(1);
        String key = "eom:%s:top3".formatted(ym);

        @SuppressWarnings("unchecked")
                List<EmployeeResDto> cached =
                (List<EmployeeResDto>) redisTemplate.opsForValue().get(key);

        if(cached != null) return cached;

        List<EmployeeResDto> top3 = evaluationService.getEmployeesOfTop3(ym);
        redisTemplate.opsForValue().set(key, top3, TTL);
        return top3;

    }

}
