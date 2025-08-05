package com.playdata.approvalservice.approval.repository;

import com.playdata.approvalservice.approval.dto.response.ReportCountResDto;
import com.playdata.approvalservice.approval.entity.Reports;
import com.playdata.approvalservice.approval.entity.ReportStatus;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportsRepository extends JpaRepository<Reports, Long>, JpaSpecificationExecutor<Reports> {

    Page<Reports> findByWriterIdAndReportStatus(Long writerId, ReportStatus reportStatus, Pageable pageable);

    Optional<Reports> findByIdAndReportStatus(Long id, ReportStatus reportStatus);

    long countByCurrentApproverIdAndReportStatus(Long userId, ReportStatus status); // 결재할 문서

    long countByWriterIdAndReportStatus(Long writerId, ReportStatus status);

    long countByWriterIdAndReportStatusIn(Long writerId, List<ReportStatus> statuses);    // 내가 쓴 문서 (상태별)

    /**
     * 특정 보고서 ID를 시작으로 재상신 체인을 역추적하여, 재상신된 횟수(체인의 깊이)를 계산합니다.
     * MySQL 8.0 이상에서 지원하는 재귀적 CTE를 사용합니다.
     *
     * @param reportId 재상신 횟수를 계산할 기준 보고서의 ID
     * @return 재상신 횟수 (자기 자신을 포함한 체인의 총 문서 개수 - 1)
     */
    @Query(value =
            "WITH RECURSIVE ResubmitChain AS (" +
                    "    SELECT report_id, previous_report_id, 1 AS depth " +
                    "    FROM reports " +
                    "    WHERE report_id = :reportId " +
                    "    UNION ALL " +
                    "    SELECT r.report_id, r.previous_report_id, rc.depth + 1 " +
                    "    FROM reports r " +
                    "    INNER JOIN ResubmitChain rc ON r.report_id = rc.previous_report_id " +
                    "    WHERE r.previous_report_id IS NOT NULL" +
                    ") " +
                    "SELECT MAX(depth) - 1 FROM ResubmitChain",
            nativeQuery = true)
    Integer countResubmitChainDepth(@Param("reportId") Long reportId);

    Page<Reports> findAll(Specification<Reports> spec, Pageable pageable);

    List<Reports> findByPublishedFalseAndScheduledAtBefore(ZonedDateTime time);


    @Query("SELECT " +
            "   COUNT(CASE WHEN r.reportStatus = 'IN_PROGRESS' AND r.currentApproverId = :userId THEN 1 END), " +
            "   COUNT(CASE WHEN r.reportStatus = 'IN_PROGRESS' AND r.writerId = :userId THEN 1 END), " +
            "   COUNT(CASE WHEN r.reportStatus = 'REJECTED' AND r.writerId = :userId THEN 1 END), " +
            "   COUNT(CASE WHEN r.reportStatus = 'DRAFT' AND r.writerId = :userId THEN 1 END), " +
            "   COUNT(CASE WHEN r.reportStatus = 'SCHEDULED' AND r.writerId = :userId THEN 1 END), " +
            "   (SELECT COUNT(rr) FROM ReportReferences rr WHERE rr.reports.id IN (SELECT r.id FROM Reports r WHERE rr.employeeId = :userId)) " +
            "FROM Reports r")
    Object[] countAllByUserId(@Param("userId") Long userId);
}
