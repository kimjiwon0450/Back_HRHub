package com.playdata.noticeservice.notice.repository;

import com.playdata.noticeservice.notice.entity.CommunityReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityReportRepository extends JpaRepository<CommunityReport, Long> {
    Page<CommunityReport> findByResolvedFalse(Pageable pageable); // 미처리된 신고 목록
    List<CommunityReport> findAllByCommunityId(Long communityId);
    boolean existsByCommunityIdAndReporterIdAndResolvedFalse(Long communityId, Long reporterId);

}
