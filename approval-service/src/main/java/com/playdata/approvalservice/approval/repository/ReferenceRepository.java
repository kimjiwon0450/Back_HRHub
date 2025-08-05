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
    List<ReportReferences> findByReportsId(Long reportId);

    Optional<ReportReferences> findByReportsIdAndEmployeeId(Long reportsId, Long employeeId);

    void deleteByReportsIdAndEmployeeId(Long reportsId, Long employeeId);

    long countByEmployeeId(Long employeeId);
}
