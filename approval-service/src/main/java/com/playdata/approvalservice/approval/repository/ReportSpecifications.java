package com.playdata.approvalservice.approval.repository;

import com.playdata.approvalservice.approval.entity.ApprovalLine;
import com.playdata.approvalservice.approval.entity.Reports;
import com.playdata.approvalservice.approval.entity.ReportStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import com.playdata.approvalservice.approval.entity.ReportReferences;


public class ReportSpecifications {

    public static Specification<Reports> withDynamicQuery(
            String role,
            ReportStatus status,
            String keyword,
            Long userId
    ) {
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction(); // 모든 조건을 AND로 묶기 시작

            // 1. 상태(status) 필터링
            if (status != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("reportStatus"), status));
            }

            // 2. 키워드(keyword) 필터링
            if (keyword != null && !keyword.isBlank()) {
                Predicate titleLike = criteriaBuilder.like(root.get("title"), "%" + keyword + "%");
                Predicate contentLike = criteriaBuilder.like(root.get("content"), "%" + keyword + "%");
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.or(titleLike, contentLike));
            }

            // 3. 역할(role)에 따른 필터링
            switch (role.toLowerCase()) {
                case "writer":
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("writerId"), userId));
                    break;
                case "approver":
                    Join<Reports, ApprovalLine> approvalLineJoin = root.join("approvalLines");
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(approvalLineJoin.get("employeeId"), userId));
                    break;
                case "reference":
                    Join<Reports, ReportReferences> referencesJoin = root.join("reportReferences");
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(referencesJoin.get("employeeId"), userId));
                    break;
                case "all":
                    Join<Reports, ApprovalLine> joinForApprover = root.join("approvalLines");
                    Join<Reports, ReportReferences> joinForReference = root.join("reportReferences"); // 참조자 조인 추가

                    Predicate allConditions = criteriaBuilder.or(
                            criteriaBuilder.equal(root.get("writerId"), userId),
                            criteriaBuilder.equal(joinForApprover.get("employeeId"), userId),
                            criteriaBuilder.equal(joinForReference.get("employeeId"), userId) // LIKE 대신 EQUAL 사용
                    );
                    predicate = criteriaBuilder.and(predicate, allConditions);
                    break;
                // 'admin' 역할은 필터링 없이 모든 문서를 보여주므로 아무 조건도 추가하지 않음
                case "admin":
                    break;
                default:
                    // 지원하지 않는 role이면 아무 결과도 나오지 않도록 처리
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.disjunction());
                    break;
            }

            // 중복 결과 제거
            query.distinct(true);

            return predicate;
        };
    }
}