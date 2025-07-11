package com.playdata.approvalservice.approval.repository;

import com.playdata.approvalservice.approval.entity.Reports;
import com.playdata.approvalservice.approval.entity.ReportStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReportsRepository extends JpaRepository<Reports, Long> {

    /**
     * 작성자 기준 페이징 조회
     */
    Page<Reports> findByWriterId(Long writerId, Pageable pageable);

    Page<Reports> findByWriterIdAndReportStatus(Long writerId, ReportStatus reportStatus, Pageable pageable);

    /**
     * 결재자(ApprovalLine)로 조회
     */
    @Query("select r from Reports r join r.approvalLines l where l.employeeId = :approverId")
    Page<Reports> findByApproverId(Long approverId, Pageable pageable);

    /**
     * 작성자 + 키워드 검색 (제목 or 본문)
     */
    @Query("select r from Reports r where r.writerId = :writerId and " +
            "(r.title like %:kw% or r.content like %:kw%)")
    Page<Reports> findByWriterIdAndKeyword(Long writerId, String kw, Pageable pageable);

    Optional<Reports> findByIdAndReportStatus(Long id, ReportStatus reportStatus);

    Page<Reports> findByCurrentApproverIdAndReportStatus(Long currentApproverId, ReportStatus reportStatus);

    Page<Reports> findByCurrentApproverIdAndReportStatus(Long currentApproverId, ReportStatus reportStatus, Pageable pageable);

}
