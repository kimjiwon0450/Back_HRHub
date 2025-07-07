package com.playdata.approvalservice.approval.repository;

import com.playdata.approvalservice.approval.entity.ApprovalLine;
import com.playdata.approvalservice.approval.entity.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalRepository extends JpaRepository<ApprovalLine, Long> {

    Optional<ApprovalLine> findByReportsIdAndEmployeeIdAndApprovalStatus(Long reportsId, Long employeeId, ApprovalStatus approvalStatus);

    // (2) 리포트의 전체 결재 라인 이력 (순번 오름차순)
    List<ApprovalLine> findByReportsIdOrderByApprovalOrderAsc(Long reportId);

    // (3) 다음 결재(첫 PENDING) 한 건 조회
    Optional<ApprovalLine> findFirstByReportsIdAndApprovalStatusOrderByApprovalOrderAsc(Long reportId, ApprovalStatus approvalStatus);
}
