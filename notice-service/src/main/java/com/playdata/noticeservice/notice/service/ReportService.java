package com.playdata.noticeservice.notice.service;

import com.playdata.noticeservice.notice.entity.Community;
import com.playdata.noticeservice.notice.entity.CommunityReport;
import com.playdata.noticeservice.notice.repository.CommunityReportRepository;
import com.playdata.noticeservice.notice.repository.CommunityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReportService {

    private final CommunityRepository communityRepository;
    private final CommunityReportRepository reportRepository;

    public ReportService(CommunityRepository communityRepository, CommunityReportRepository reportRepository) {
        this.communityRepository = communityRepository;
        this.reportRepository = reportRepository;
    }

    @Transactional
    public void reportCommunity(Long communityId, Long reporterId, String reason) {
        Community community = communityRepository.findByCommunityIdAndBoardStatusTrue(communityId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 신고 저장
        CommunityReport report = new CommunityReport();
        report.setCommunityId(communityId);
        report.setReporterId(reporterId);
        report.setReason(reason);
        reportRepository.save(report);

        // 게시글을 비공개 처리
        if (!community.isHidden()) {
            community.setHidden(true);
            communityRepository.save(community);
        }
    }

    public List<CommunityReport> getUnresolvedReports() {
        return reportRepository.findByResolvedFalse();
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
