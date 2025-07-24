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
                    // [수정] DB의 JSON_CONTAINS 함수를 사용하여 정확하게 검색
                    // 이 코드는 MySQL/MariaDB 기준입니다.

                    // 1. 우리가 찾고 싶은 JSON 객체 형태를 문자열로 만듭니다. 예: {"employeeId": 33}
                    String jsonObjectToFind = String.format("{\"employeeId\": %d}", userId);

                    // 2. criteriaBuilder.function()을 사용하여 JSON_CONTAINS 함수를 호출합니다.
                    //    JSON_CONTAINS(json_document, value_to_find, path)
                    //    '$.references'는 detail JSON의 'references' 배열 안에서 찾으라는 의미입니다.
                    Predicate jsonContainsPredicate = criteriaBuilder.equal(
                            criteriaBuilder.function(
                                    "JSON_CONTAINS",
                                    Boolean.class,
                                    root.get("detail"),               // 검색 대상 JSON 컬럼
                                    criteriaBuilder.literal(jsonObjectToFind), // 찾을 값
                                    criteriaBuilder.literal("$.references")    // JSON 내부 경로
                            ),
                            true
                    );
                case "all":
                    Join<Reports, ApprovalLine> joinForApprover = root.join("approvalLines");
                    // 참조자 검색도 JOIN이 아닌 LIKE 검색으로 변경합니다.
                    String patternForAll = "\"employeeId\":" + userId;

                    Predicate allConditions = criteriaBuilder.or(
                            criteriaBuilder.equal(root.get("writerId"), userId),
                            criteriaBuilder.equal(joinForApprover.get("employeeId"), userId),
                            criteriaBuilder.like(root.get("detail"), "%" + patternForAll + "%")
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