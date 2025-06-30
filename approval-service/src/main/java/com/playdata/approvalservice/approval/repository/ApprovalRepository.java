package com.playdata.approvalservice.approval.repository;

import com.playdata.approvalservice.approval.entity.Approval;
import com.playdata.approvalservice.approval.entity.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Approval 엔티티(결재 단계) 관리를 위한 리포지토리
public interface ApprovalRepository extends JpaRepository<Approval, Long> {

    /**
     * 보고서의 전체 결재라인을 순서대로 조회
     * @param reportApprovalId 보고서 ID
     */
    List<Approval> findByReportApprovalIdOrderByOrderSequenceAsc(Long reportApprovalId);

    /**
     * 대기 중인 첫 번째 결재 단계 조회
     * @param reportApprovalId 보고서 ID
     * @param status 대기 상태 (IN_PROGRESS)
     */
    Optional<Approval> findFirstByReportApprovalIdAndStatusOrderByOrderSequenceAsc(Long reportApprovalId, ReportType status);

    /**
     * 보고서 & 결재자 기준 개별 결재 정보 조회
     * @param reportApprovalId 보고서 ID
     * @param employeeId 결재자 ID
     */
    Optional<Approval> findByReportApprovalIdAndEmployeeId(Long reportApprovalId, Long employeeId);

    /**
     * 특정 보고서의 특정 단계가 완료됐는지 확인
     * @param reportApprovalId 보고서 ID
     * @param orderSequence 단계 순서
     */
    boolean existsByReportApprovalIdAndOrderSequenceAndStatus(Long reportApprovalId, Integer orderSequence, ReportType status);

}
