package com.flowsuite.api.repository;

import com.flowsuite.api.domain.entity.Invitation;
import com.flowsuite.api.domain.enums.InvitationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByTokenHash(String tokenHash);

    Optional<Invitation> findByIdAndOrganization_Id(UUID id, UUID organizationId);

    Page<Invitation> findByOrganization_Id(UUID organizationId, Pageable pageable);

    Optional<Invitation> findByOrganization_IdAndEmailIgnoreCaseAndStatus(
            UUID organizationId, String email, InvitationStatus status);
}
