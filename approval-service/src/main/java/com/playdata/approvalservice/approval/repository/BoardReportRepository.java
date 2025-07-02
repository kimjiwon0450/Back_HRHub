package com.playdata.approvalservice.approval.repository;

import com.playdata.approvalservice.approval.entity.Reports;
import com.playdata.approvalservice.approval.entity.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 보고서(BoardReport) 엔티티에 대한 CRUD 및 페이징 지원 리포지토리
public interface BoardReportRepository extends JpaRepository<Reports, Long> {
    /**
     * 작성자 ID로 보고서 목록 조회
     * @param submitId 작성자 사원 ID
     * @return 보고서 리스트
     */
    List<Reports> findBySubmitId(Long submitId);

    /**
     * 특정 상태의 보고서 페이징용 조회
     * @param status 보고서 상태
     * @return 보고서 리스트
     */
    List<Reports> findByStatus(ReportStatus status);

    /**
     * 보고서 삭제 전 존재 여부 체크
     * @param id 보고서 ID
     * @param submitId 작성자 ID
     * @return 존재 여부
     */
    boolean existsByIdAndSubmitId(Long id, Long submitId);
}
