package com.flowsuite.api.repository;

import com.flowsuite.api.domain.entity.Plan;
import com.flowsuite.api.domain.enums.PlanCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {

    Optional<Plan> findByCode(PlanCode code);
}
