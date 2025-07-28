package com.playdata.approvalservice.approval.service;

import com.playdata.approvalservice.approval.entity.Reports;
import com.playdata.approvalservice.approval.repository.ReportsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalSchedulerService {

    private final ReportsRepository reportsRepository;
    private final ApprovalService approvalService;

    // 자기 자신을 주입받기 위한 필드 (Lazy 로딩으로 순환참조 방지)
    @Autowired
    @Lazy
    private ApprovalSchedulerService self;

    /**
     * 매 분 0초마다 실행되어 예약된 결재 문서를 찾아 상신 처리합니다.
     * cron = "초 분 시 일 월 요일"
     * "0 * * * * *" -> 매 분 0초에 실행
     */
    @Scheduled(cron = "0 * * * * *")
    public void publishScheduledReports() {
        log.info("[스케줄러 시작] 예약된 결재 문서 확인을 시작합니다.");

        List<Reports> reportsToPublish = reportsRepository.findByPublishedFalseAndScheduledAtBefore(ZonedDateTime.now());

        if (reportsToPublish.isEmpty()) {
            log.info("[스케줄러] 처리할 예약 결재 문서가 없습니다.");
            return;
        }

        log.info("[스케줄러] 총 {}건의 예약 결재 문서를 처리합니다.", reportsToPublish.size());

        for (Reports report : reportsToPublish) {
            try {
                // ApprovalService를 직접 호출하는 대신, @Transactional이 붙은 자기 자신의 메소드를 호출
                self.processScheduledSubmission(report.getId());
                log.info("[스케줄러 성공] 보고서 ID: {} 예약 상신 처리 완료", report.getId());
            } catch (Exception e) {
                log.error("[스케줄러 실패] 보고서 ID: {} 처리 중 오류 발생: {}", report.getId(), e.getMessage());
            }
        }

        log.info("[스케줄러 종료] 예약된 결재 문서 확인을 종료합니다.");
    }

    /**
     * 실제 발행 처리를 담당하는 트랜잭션 메소드.
     * 이 메소드를 ApprovalService에서 이곳으로 이동시킵니다.
     */
    @Transactional
    public void processScheduledSubmission(Long reportId) {
        Reports report = reportsRepository.findById(reportId)
                .orElseThrow(() -> {
                    // orElseThrow에는 예외를 생성하는 람다를 전달해야 합니다.
                    log.error("예약 문서를 찾을 수 없습니다: " + reportId);
                    return new RuntimeException("예약 문서를 찾을 수 없습니다: " + reportId);
                });

        // 엔티티의 publish() 메소드를 호출하여 '발행 행위'를 위임
        report.publish();
    }
}
