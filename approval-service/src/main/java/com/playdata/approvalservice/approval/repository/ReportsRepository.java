package com.playdata.approvalservice.approval.repository;

import com.playdata.approvalservice.approval.entity.Reports;
import com.playdata.approvalservice.approval.entity.ReportStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportsRepository extends JpaRepository<Reports, Long> {

    /**
     * 작성자 기준 페이징 조회
     */
    Page<Reports> findByWriterId(Long writerId, Pageable pageable);

    Page<Reports> findByWriterIdAndReportStatus(Long writerId, ReportStatus reportStatus, Pageable pageable);

    /**
     * 결재자(ApprovalLine)로 조회
     */
    @Query("SELECT r FROM Reports r JOIN r.approvalLines l " +
            "WHERE l.employeeId = :approverId " +
            "AND r.reportStatus IN ('IN_PROGRESS', 'APPROVED', 'REJECTED')")
    Page<Reports> findByApproverIdAndExcludeDraftRecalled(@Param("approverId") Long approverId, Pageable pageable);

    Optional<Reports> findByIdAndReportStatus(Long id, ReportStatus reportStatus);

    // [수정] Pageable 파라미터를 가장 마지막으로 이동시킵니다.
    Page<Reports> findByCurrentApproverIdAndReportStatus(Long currentApproverId, ReportStatus reportStatus, Pageable pageable);

    // 3. [수정] 참조자 기준 조회 - DRAFT, RECALLED 상태 제외
    // 네이티브 쿼리에 WHERE 조건을 추가합니다.
    @Query(
            value = "SELECT * FROM reports r " +
                    "WHERE JSON_CONTAINS(r.report_detail, JSON_OBJECT('employeeId', :employeeId), '$.references') " +
                    "AND r.report_status IN ('IN_PROGRESS', 'APPROVED', 'REJECTED')", // ★★★ 상태 필터링 추가 ★★★
            countQuery = "SELECT count(*) FROM reports r " +
                    "WHERE JSON_CONTAINS(r.report_detail, JSON_OBJECT('employeeId', :employeeId), '$.references') " +
                    "AND r.report_status IN ('IN_PROGRESS', 'APPROVED', 'REJECTED')",
            nativeQuery = true
    )
    Page<Reports> findByReferenceEmployeeIdInDetailJsonAndExcludeDraftRecalled(@Param("employeeId") Long employeeId, Pageable pageable);
}
