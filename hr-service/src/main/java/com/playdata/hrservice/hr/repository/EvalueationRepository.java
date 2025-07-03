package com.playdata.hrservice.hr.repository;

import com.playdata.hrservice.hr.entity.Employee;
import com.playdata.hrservice.hr.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EvalueationRepository extends JpaRepository<Evaluation, Long> {
    Optional<Evaluation> findByEvaluatee(Employee evaluatee);
}
