package com.playdata.noticeservice.notice.controller;

import com.playdata.noticeservice.common.client.HrUserClient;
import com.playdata.noticeservice.common.dto.HrUserResponse;
import com.playdata.noticeservice.notice.dto.CommunityReportResponse;
import com.playdata.noticeservice.notice.dto.CommunityResponse;
import com.playdata.noticeservice.notice.dto.ReportRequest;
import com.playdata.noticeservice.notice.entity.Community;
import com.playdata.noticeservice.notice.entity.CommunityReport;
import com.playdata.noticeservice.notice.service.CommunityService;
import com.playdata.noticeservice.notice.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final CommunityService communityService;
    private final HrUserClient hrUserClient;

    @PostMapping("/{communityId:\\d+}")
    public ResponseEntity<?> reportCommunity(
            @RequestBody ReportRequest request) {
        log.info("request : {}", request);
        reportService.reportCommunity(request.getCommunityId() , request.getReporterId(), request.getReason());
        return ResponseEntity.ok("신고가 접수되었습니다.");
    }

    @GetMapping("/admin/list")
    public ResponseEntity<Map<String, Object>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest request) {

        String token = request.getHeader("Authorization");  // 토큰 꺼내기
        Page<CommunityReport> reports;
        reports = reportService.getUnresolvedReports(page, pageSize, sortBy, sortDir);

        Set<Long> employeeIds = reports.stream()
                .map(CommunityReport::getReporterId)
                .collect(Collectors.toSet());

        Map<Long, HrUserResponse> userMap = hrUserClient.getUserInfoBulk(employeeIds, token)
                .stream().collect(Collectors.toMap(HrUserResponse::getEmployeeId, Function.identity()));

        Map<String, Object> response = new HashMap<>();

        response.put("posts", reports.stream()
                .map(c -> {
                    // 신고자
                    HrUserResponse reporter = userMap.get(c.getReporterId());

                    // 글 작성자
                    Community community = communityService.findPostById(c.getCommunityId());
                    HrUserResponse writer = hrUserClient.getUserInfo(community.getEmployeeId());
                    CommunityResponse communityres = CommunityResponse.fromEntity(community, writer);
                    return CommunityReportResponse.fromEntity(c, reporter, communityres);
                }).toList());
        response.put("totalPages", reports.getTotalPages());
        response.put("currentPage", reports.getNumber());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/{communityId:\\d+}/recover")
    public ResponseEntity<?> recoverCommunity(@PathVariable Long communityId) {
        reportService.recoverCommunity(communityId);
        return ResponseEntity.ok("게시글이 공개 처리되었습니다.");
    }

    @PostMapping("/admin/{communityId:\\d+}/delete")
    public ResponseEntity<?> deleteCommunity(@PathVariable Long communityId) {
        reportService.deleteCommunity(communityId);
        return ResponseEntity.ok("게시글이 삭제 처리되었습니다.");
    }
}

