package com.playdata.approvalservice.approval.repository;

import com.playdata.approvalservice.approval.entity.ReportAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

//ReportAttachment 엔티티(첨부파일) 관리를 위한 리포지토리
public interface ReportAttachmentRepository extends JpaRepository<ReportAttachment, Long> {

    /**
     *
     * @param reportsId
     * @return
     */
    List<ReportAttachment> findByReports_Id(Long reportsId);

}
