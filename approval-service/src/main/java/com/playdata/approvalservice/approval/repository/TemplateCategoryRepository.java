package com.playdata.approvalservice.approval.repository;

import com.playdata.approvalservice.approval.entity.TemplateCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TemplateCategoryRepository extends JpaRepository<TemplateCategory, Long> {
    Optional<TemplateCategory> findByCategoryName(String categoryName);

}
