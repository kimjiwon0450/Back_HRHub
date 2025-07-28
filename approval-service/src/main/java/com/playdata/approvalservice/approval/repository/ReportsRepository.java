package com.playdata.approvalservice.approval.repository;

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

public interface ReportsRepository extends JpaRepository<Reports, Long> , JpaSpecificationExecutor<Reports> {

    /**
     * 작성자 기준 페이징 조회
     */
    Page<Reports> findByWriterId(Long writerId, Pageable pageable);

    Page<Reports> findByWriterIdAndReportStatus(Long writerId, ReportStatus reportStatus, Pageable pageable);

    Optional<Reports> findByIdAndReportStatus(Long id, ReportStatus reportStatus);

    // [수정] Pageable 파라미터를 가장 마지막으로 이동시킵니다.
    Page<Reports> findByCurrentApproverIdAndReportStatus(Long currentApproverId, ReportStatus reportStatus, Pageable pageable);

    // 내가 결재선에 포함되어 있으면서, 상태가 IN_PROGRESS인 문서만 조회
    @Query("SELECT r FROM Reports r WHERE r.reportStatus = 'IN_PROGRESS' AND " +
            "EXISTS (SELECT 1 FROM ApprovalLine l WHERE l.reports = r AND l.employeeId = :approverId)")
    Page<Reports> findInProgressReportsByApproverInLine(@Param("approverId") Long approverId, Pageable pageable);

    /**
     * 특정 사용자가 결재선에 포함되어 있으면서, 특정 상태(완료 또는 반려)인 문서 목록을 조회합니다.
     * (완료/반려 문서함 기능에 사용)
     * @param approverId 결재자 ID
     * @param status 조회할 보고서 상태 (APPROVED 또는 REJECTED)
     * @param pageable 페이징 정보
     * @return 보고서 페이지
     */
    @Query("SELECT r FROM Reports r WHERE r.reportStatus = :status AND " +
            "EXISTS (SELECT 1 FROM ApprovalLine l WHERE l.reports = r AND l.employeeId = :approverId)")
    Page<Reports> findByApproverIdAndStatus(@Param("approverId") Long approverId, @Param("status") ReportStatus status, Pageable pageable);

    @Query("SELECT r FROM Reports r WHERE r.reportStatus = 'IN_PROGRESS' AND " +
            "(r.writerId = :userId OR EXISTS (SELECT 1 FROM ApprovalLine l WHERE l.reports = r AND l.employeeId = :userId))")
    Page<Reports> findInProgressForUser(@Param("userId") Long userId, Pageable pageable);

    /**
     * 특정 보고서 ID를 시작으로 재상신 체인을 역추적하여, 재상신된 횟수(체인의 깊이)를 계산합니다.
     * MySQL 8.0 이상에서 지원하는 재귀적 CTE를 사용합니다.
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

    @Query(
            value = "SELECT * FROM reports r " +
                    // ★★★ 핵심 수정: CONCAT에서 대괄호 '[]'를 제거하여 JSON 객체를 만듭니다 ★★★
                    "WHERE JSON_CONTAINS(r.report_detail, CAST(CONCAT('{\"employeeId\":', :employeeId, '}') AS JSON), '$.references') " +
                    "AND r.report_status IN ('IN_PROGRESS', 'APPROVED', 'REJECTED')",
            countQuery = "SELECT count(*) FROM reports r " +
                    "WHERE JSON_CONTAINS(r.report_detail, CAST(CONCAT('{\"employeeId\":', :employeeId, '}') AS JSON), '$.references') " +
                    "AND r.report_status IN ('IN_PROGRESS', 'APPROVED', 'REJECTED')",
            nativeQuery = true
    )
    Page<Reports> findReferencedReportsByJsonContains(@Param("employeeId") Long employeeId, Pageable pageable);


    Page<Reports> findAll(Specification<Reports> spec, Pageable pageable);

    // NoticeRepository.java
    List<Reports> findByPublishedFalseAndScheduledAtBefore(ZonedDateTime time);

}
