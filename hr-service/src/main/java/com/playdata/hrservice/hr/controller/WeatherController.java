package com.playdata.hrservice.hr.controller;

import com.playdata.hrservice.hr.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/hr")
@RequiredArgsConstructor
public class WeatherController {
    private final WeatherService weatherService;

    @GetMapping("/getVilageFcst")
    public ResponseEntity<?> getVilageFcst(@RequestParam Map<String, String> params) {
        try {
            String result = weatherService.getVilageFcst(params);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("날씨 데이터 조회 실패: " + e.getMessage());
        }
    }
}
