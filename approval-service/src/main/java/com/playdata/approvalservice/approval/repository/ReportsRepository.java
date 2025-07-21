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

    Optional<Reports> findByIdAndReportStatus(Long id, ReportStatus reportStatus);

    // [수정] Pageable 파라미터를 가장 마지막으로 이동시킵니다.
    Page<Reports> findByCurrentApproverIdAndReportStatus(Long currentApproverId, ReportStatus reportStatus, Pageable pageable);

    // 3. [수정] 참조자 기준 조회 - DRAFT, RECALLED 상태 제외
    // 네이티브 쿼리에 WHERE 조건을 추가합니다.
    @Query(
            value = "SELECT * FROM reports r " +
                    "WHERE JSON_CONTAINS(r.report_detail, JSON_OBJECT('employeeId', :employeeId), '$.references') " +
                    "AND r.report_status IN ('IN_PROGRESS', 'APPROVED', 'REJECTED')",
            countQuery = "SELECT count(*) FROM reports r " +
                    "WHERE JSON_CONTAINS(r.report_detail, JSON_OBJECT('employeeId', :employeeId), '$.references') " +
                    "AND r.report_status IN ('IN_PROGRESS', 'APPROVED', 'REJECTED')",
            nativeQuery = true
    )
    Page<Reports> findByReferenceEmployeeIdInDetailJsonAndExcludeDraftRecalled(@Param("employeeId") Long employeeId, Pageable pageable);


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
}
