package com.flowsuite.api.repository;

import com.flowsuite.api.domain.entity.Membership;
import com.flowsuite.api.domain.enums.MembershipStatus;
import com.flowsuite.api.domain.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    /**
     * Resolves the tenant membership for a user. Loaded on every authenticated request to
     * determine the active organization and role, so the organization is eagerly fetched.
     * There is at most one row per user (unique constraint on {@code user_id}).
     */
    @EntityGraph(attributePaths = {"organization", "user"})
    Optional<Membership> findByUser_Id(UUID userId);

    Optional<Membership> findByUser_IdAndStatus(UUID userId, MembershipStatus status);

    long countByOrganization_IdAndStatus(UUID organizationId, MembershipStatus status);

    @EntityGraph(attributePaths = {"user"})
    Page<Membership> findByOrganization_Id(UUID organizationId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "organization"})
    Optional<Membership> findByIdAndOrganization_Id(UUID id, UUID organizationId);

    @EntityGraph(attributePaths = {"user", "organization"})
    Optional<Membership> findByOrganization_IdAndUser_Id(UUID organizationId, UUID userId);

    /** Count members holding a given role (e.g. to prevent removing the last Owner). */
    long countByOrganization_IdAndRoleAndStatus(UUID organizationId, Role role, MembershipStatus status);
}
