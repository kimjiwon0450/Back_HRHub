package com.playdata.approvalservice.approval.repository;

import com.playdata.approvalservice.approval.entity.ApprovalLine;
import com.playdata.approvalservice.approval.entity.ReportReferences;
import com.playdata.approvalservice.approval.entity.Reports;
import com.playdata.approvalservice.approval.entity.ReportStatus;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;
import java.util.stream.Collectors;

public class ReportSpecifications {

    /**
     * 역할(다중 지원), 상태, 키워드를 기반으로 동적 쿼리를 생성하는 Specification을 반환합니다.
     *
     * @param role    쉼표로 구분된 역할 문자열 (예: "writer,approver")
     * @param status  필터링할 보고서 상태
     * @param keyword 필터링할 제목 또는 내용 키워드
     * @param userId  현재 사용자의 ID
     * @return 생성된 Specification 객체
     */
    public static Specification<Reports> withDynamicQuery(
            String role,
            ReportStatus status,
            String keyword,
            Long userId
    ) {
        return (root, query, criteriaBuilder) -> {
            // 모든 조건을 AND로 묶기 위한 기본 Predicate
            Predicate predicate = criteriaBuilder.conjunction();

            // ★★★ 1. 상태(status) 필터링 로직 수정 ★★★
            if (status != null) {
                // 전달받은 Enum 리스트를 사용하여 `IN` 절을 생성
                predicate = criteriaBuilder.and(predicate, root.get("reportStatus").in(status));
            }

            // 2. 키워드(keyword) 필터링: 제목 또는 내용에 키워드가 포함되는 경우
            if (keyword != null && !keyword.isBlank()) {
                Predicate titleLike = criteriaBuilder.like(root.get("title"), "%" + keyword + "%");
                Predicate contentLike = criteriaBuilder.like(root.get("content"), "%" + keyword + "%");
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.or(titleLike, contentLike));
            }

            // 3. 역할(role) 필터링
            if (role != null && !role.isBlank()) {
                // 쉼표로 구분된 role 문자열을 파싱하여 리스트로 변환
                List<String> roles = Arrays.stream(role.toLowerCase().split(","))
                        .map(String::trim)
                        .distinct() // 중복된 역할 제거 (예: "writer,writer")
                        .collect(Collectors.toList());

                // 역할별 조건을 OR로 묶기 위한 리스트
                List<Predicate> rolePredicates = new ArrayList<>();
                // 중복 JOIN을 방지하기 위해 Join 객체를 캐싱하는 맵
                Map<String, Join<Reports, ?>> joins = new HashMap<>();

                for (String r : roles) {
                    switch (r) {
                        case "writer":
                            rolePredicates.add(criteriaBuilder.equal(root.get("writerId"), userId));
                            break;

                        case "approver":
                            // "approvalLines" 키로 Join이 이미 존재하는지 확인하고, 없으면 새로 생성하여 맵에 저장
                            @SuppressWarnings("unchecked")
                            Join<Reports, ApprovalLine> approvalLineJoin = (Join<Reports, ApprovalLine>) joins.computeIfAbsent(
                                    "approvalLines",
                                    key -> root.join(key, JoinType.LEFT)
                            );
                            rolePredicates.add(criteriaBuilder.equal(approvalLineJoin.get("employeeId"), userId));
                            break;

                        case "reference":
                            // 1. `detail` 컬럼에서 모든 참조자의 employeeId 값만 추출하여 JSON 배열을 만듭니다.
                            //    SQL 예시: JSON_EXTRACT(detail, '$.references[*].employeeId') -> 결과: [5, 7, 8]
                            Expression<String> employeeIdsJsonArray = criteriaBuilder.function(
                                    "JSON_EXTRACT",
                                    String.class,
                                    root.get("detail"),
                                    criteriaBuilder.literal("$.references[*].employeeId")
                            );
                            // 2. `JSON_CONTAINS` 함수를 사용하여 위에서 만든 JSON 배열에 userId가 포함되어 있는지 확인합니다.
                            //    SQL 예시: JSON_CONTAINS('[5, 7, 8]', CAST('1' AS JSON))
                            Predicate jsonContainsPredicate = criteriaBuilder.isTrue(
                                    criteriaBuilder.function(
                                            "JSON_CONTAINS",
                                            Boolean.class,
                                            employeeIdsJsonArray, // 검색 대상: employeeId 값들로 이루어진 JSON 배열
                                            criteriaBuilder.literal(String.valueOf(userId)) // 찾을 값: userId를 문자열로 변환
                                    )
                            );


                            // 3. 생성된 조건을 리스트에 추가합니다.
                            rolePredicates.add(jsonContainsPredicate);
                            break;

                        case "involved":
                            // '결재 관여'는 '내 기안이 아니면서, 결재선에 포함된' 문서를 의미합니다.
                            @SuppressWarnings("unchecked")
                            Join<Reports, ApprovalLine> involvedJoin = (Join<Reports, ApprovalLine>) joins.computeIfAbsent(
                                    "approvalLines",
                                    key -> root.join(key, JoinType.LEFT)
                            );
                            Predicate notMyReport = criteriaBuilder.notEqual(root.get("writerId"), userId);
                            Predicate inLine = criteriaBuilder.equal(involvedJoin.get("employeeId"), userId);
                            rolePredicates.add(criteriaBuilder.and(notMyReport, inLine));
                            break;
                    }
                }

                // 생성된 역할 조건들이 있다면, 모두 OR로 묶어서 최종 predicate에 AND 조건으로 추가
                if (!rolePredicates.isEmpty()) {
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.or(rolePredicates.toArray(new Predicate[0])));
                }
            }

            // JOIN으로 인해 발생할 수 있는 중복된 Reports 결과를 제거
            query.distinct(true);

            return predicate;
        };
    }
}