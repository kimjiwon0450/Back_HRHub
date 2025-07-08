package com.playdata.approvalservice.approval.repository;

import com.playdata.approvalservice.approval.entity.ApprovalLine;
import com.playdata.approvalservice.approval.entity.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApprovalRepository extends JpaRepository<ApprovalLine, Long> {

    Optional<ApprovalLine> findByReportsIdAndEmployeeIdAndApprovalStatus(Long reportsId, Long employeeId, ApprovalStatus approvalStatus);

    // (2) 리포트의 전체 결재 라인 이력 (순번 오름차순)
    @Query("SELECT al FROM ApprovalLine al WHERE al.reports.id = :reportId ORDER BY al.approvalContext ASC")
    List<ApprovalLine> findApprovalLinesByReportId(@Param("reportId") Long reportId);

}
