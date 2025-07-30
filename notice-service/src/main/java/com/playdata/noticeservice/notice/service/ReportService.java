package com.playdata.noticeservice.notice.service;

import com.playdata.noticeservice.notice.entity.Community;
import com.playdata.noticeservice.notice.entity.CommunityReport;
import com.playdata.noticeservice.notice.repository.CommunityReportRepository;
import com.playdata.noticeservice.notice.repository.CommunityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final CommunityRepository communityRepository;
    private final CommunityReportRepository reportRepository;


    @Transactional
    public void reportCommunity(Long communityId, Long reporterId, String reason) {
        // ✅ 중복 신고 확인
        boolean alreadyReported = reportRepository.existsByCommunityIdAndReporterId(communityId, reporterId);
        if (alreadyReported) {
            throw new IllegalStateException("이미 신고한 게시글입니다.");
        }

        Community community = communityRepository.findByCommunityIdAndBoardStatusTrue(communityId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));
        log.info("community : {}", community);
        // 신고 저장
        CommunityReport report = new CommunityReport();
        report.setCommunityId(communityId);
        report.setReporterId(reporterId);
        report.setReason(reason);
        log.info("report : {}", report);
        reportRepository.save(report);

        // 게시글을 비공개 처리
        if (!community.isHidden()) {
            community.setHidden(true);
            communityRepository.save(community);
        }
    }

    public Page<CommunityReport> getUnresolvedReports(int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return reportRepository.findByResolvedFalse(pageable);
    }

    @Transactional
    public void deleteCommunity(Long communityId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new RuntimeException("게시글 없음"));
        community.setBoardStatus(false);
        community.setHidden(true);
        communityRepository.save(community);

        resolveReports(communityId);
    }

    @Transactional
    public void recoverCommunity(Long communityId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new RuntimeException("게시글 없음"));
        community.setHidden(false);
        communityRepository.save(community);

        resolveReports(communityId);
    }

    private void resolveReports(Long communityId) {
        List<CommunityReport> reports = reportRepository.findAllByCommunityId(communityId);
        for (CommunityReport report : reports) {
            report.setResolved(true);
        }
        reportRepository.saveAll(reports);
    }
}
