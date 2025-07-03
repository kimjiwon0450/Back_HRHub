package com.playdata.approvalservice.approval.repository;

import com.playdata.approvalservice.approval.entity.ReportAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

//ReportAttachment 엔티티(첨부파일) 관리를 위한 리포지토리
public interface ReportAttachmentRepository extends JpaRepository<ReportAttachment, Long> {
    /**
     * 특정 보고서에 등록된 모든 첨부파일 조회
     * @param reportApprovalId 보고서 ID
     */
    List<ReportAttachment> findByReportApprovalId(Long reportApprovalId);

}
