package com.flowsuite.api.repository;

import com.flowsuite.api.domain.entity.ApiUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ApiUsageRepository extends JpaRepository<ApiUsage, UUID> {

    Optional<ApiUsage> findByOrganization_IdAndPeriodStart(UUID organizationId, LocalDate periodStart);
}
