package com.playdata.noticeservice.notice.controller;

import com.playdata.noticeservice.notice.dto.ReportRequest;
import com.playdata.noticeservice.notice.entity.CommunityReport;
import com.playdata.noticeservice.notice.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/{communityId}")
    public ResponseEntity<?> reportCommunity(
            @PathVariable Long communityId,
            @RequestBody ReportRequest request) {
        reportService.reportCommunity(communityId, request.getReporterId(), request.getReason());
        return ResponseEntity.ok("신고가 접수되었습니다.");
    }

    @GetMapping("/admin/list")
    public ResponseEntity<List<CommunityReport>> getReports() {
        return ResponseEntity.ok(reportService.getUnresolvedReports());
    }

    @PostMapping("/admin/{communityId}/recover")
    public ResponseEntity<?> recoverCommunity(@PathVariable Long communityId) {
        reportService.recoverCommunity(communityId);
        return ResponseEntity.ok("게시글이 공개 처리되었습니다.");
    }

    @PostMapping("/admin/{communityId}/delete")
    public ResponseEntity<?> deleteCommunity(@PathVariable Long communityId) {
        reportService.deleteCommunity(communityId);
        return ResponseEntity.ok("게시글이 삭제 처리되었습니다.");
    }
}

