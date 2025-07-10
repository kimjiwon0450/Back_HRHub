package com.playdata.hrservice.hr.repository;

import com.playdata.hrservice.hr.entity.Employee;
import com.playdata.hrservice.hr.entity.Evaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    Optional<Evaluation> findByEvaluatee(Employee evaluatee);
    // 이번 달에 이미 평가가 있는지 체크
    boolean existsByEvaluateeAndCreatedAtBetween(Employee evaluatee, LocalDateTime start, LocalDateTime end);

    // 이번 달 평가 조회(1건만, 최신순)
    Optional<Evaluation> findTopByEvaluateeAndCreatedAtBetweenOrderByCreatedAtDesc(Employee evaluatee, LocalDateTime start, LocalDateTime end);

    Optional<List<Evaluation>> findAllByCreatedAtBetweenOrderByTotalEvaluationDesc(LocalDateTime createdAtAfter, LocalDateTime createdAtBefore);

    Page<Evaluation> findAllByEvaluatee(Employee evaluatee, Pageable pageable);
}
