package com.playdata.approvalservice.approval.repository;

import com.playdata.approvalservice.approval.entity.ReportTemplate;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, Long> {

    @Query("SELECT rt FROM ReportTemplate rt WHERE rt.categoryId.categoryId = :categoryId")
    List<ReportTemplate> findByCategoryId(@Param("categoryId") Long categoryId);

    List<ReportTemplate> findByCategoryId_categoryId(Long categoryIdCategoryId);
}
