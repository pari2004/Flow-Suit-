package com.flowsuite.api.repository;

import com.flowsuite.api.domain.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Page<Customer> findByOrganization_Id(UUID organizationId, Pageable pageable);

    Page<Customer> findByOrganization_IdAndProject_Id(UUID organizationId, UUID projectId, Pageable pageable);

    Optional<Customer> findByIdAndOrganization_Id(UUID id, UUID organizationId);
}
