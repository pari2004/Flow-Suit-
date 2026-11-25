package com.flowsuite.api.repository;

import com.flowsuite.api.domain.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByOrganization_Id(UUID organizationId);

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
}
