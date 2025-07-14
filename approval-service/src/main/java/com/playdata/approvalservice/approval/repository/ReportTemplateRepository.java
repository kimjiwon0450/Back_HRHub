package com.playdata.approvalservice.approval.repository;

import com.playdata.approvalservice.approval.entity.ReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, Long> {

    List<ReportTemplate> findByCategoryId(Long categoryId);
}
