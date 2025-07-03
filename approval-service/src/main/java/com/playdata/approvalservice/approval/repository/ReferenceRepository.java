package com.playdata.approvalservice.approval.repository;

import com.playdata.approvalservice.approval.entity.ReportReferences;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferenceRepository extends JpaRepository<ReportReferences, Long> {

    /**
     * 특정 보고서의 모든 참조자 조회
     * @param reportId 보고서 ID
     */
    List<ReportReferences> findByReports_Id(Long reportId);

    /**
     * 특정 보고서·사원 조합 단일 조회
     * @param reportId 보고서 ID
     * @param employeeId 사원 ID
     */
    Optional<ReportReferences> findByReportIdAndEmployeeId(Long reportId, Long employeeId);

    /**
     * 특정 보고서·사원 조합 삭제
     * @param reportId 보고서 ID
     * @param employeeId 사원 ID
     */
    void deleteByReportIdAndEmployeeId(Long reportId, Long employeeId);
}
