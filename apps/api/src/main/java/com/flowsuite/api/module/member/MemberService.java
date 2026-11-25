package com.flowsuite.api.module.member;

import com.flowsuite.api.common.audit.AuditAction;
import com.flowsuite.api.common.audit.AuditService;
import com.flowsuite.api.common.dto.PageResponse;
import com.flowsuite.api.common.error.ApiException;
import com.flowsuite.api.common.error.ErrorCode;
import com.flowsuite.api.common.mail.EmailService;
import com.flowsuite.api.common.util.TokenHasher;
import com.flowsuite.api.domain.entity.Invitation;
import com.flowsuite.api.domain.entity.Membership;
import com.flowsuite.api.domain.entity.User;
import com.flowsuite.api.domain.enums.InvitationStatus;
import com.flowsuite.api.domain.enums.MembershipStatus;
import com.flowsuite.api.domain.enums.Role;
import com.flowsuite.api.module.auth.AuthTokenIssuer;
import com.flowsuite.api.module.auth.dto.AuthResponse;
import com.flowsuite.api.module.billing.EntitlementService;
import com.flowsuite.api.module.member.dto.AcceptInvitationRequest;
import com.flowsuite.api.module.member.dto.InvitationResponse;
import com.flowsuite.api.module.member.dto.InviteMemberRequest;
import com.flowsuite.api.module.member.dto.MemberResponse;
import com.flowsuite.api.module.member.dto.UpdateRoleRequest;
import com.flowsuite.api.repository.InvitationRepository;
import com.flowsuite.api.repository.MembershipRepository;
import com.flowsuite.api.repository.OrganizationRepository;
import com.flowsuite.api.repository.UserRepository;
import com.flowsuite.api.security.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Organization membership + invitation management (FR-10..FR-14). */
@Service
public class MemberService {

    private static final long INVITATION_TTL_DAYS = 7;

    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final InvitationRepository invitationRepository;
    private final EntitlementService entitlementService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenIssuer tokenIssuer;
    private final EmailService emailService;

    public MemberService(MembershipRepository membershipRepository,
                         UserRepository userRepository,
                         OrganizationRepository organizationRepository,
                         InvitationRepository invitationRepository,
                         EntitlementService entitlementService,
                         AuditService auditService,
                         PasswordEncoder passwordEncoder,
                         AuthTokenIssuer tokenIssuer,
                         EmailService emailService) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.invitationRepository = invitationRepository;
        this.entitlementService = entitlementService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public PageResponse<MemberResponse> listMembers(Pageable pageable) {
        UUID organizationId = SecurityUtils.currentOrganizationId();
        return PageResponse.from(
                membershipRepository.findByOrganization_Id(organizationId, pageable), MemberResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<InvitationResponse> listInvitations(Pageable pageable) {
        UUID organizationId = SecurityUtils.currentOrganizationId();
        return PageResponse.from(
                invitationRepository.findByOrganization_Id(organizationId, pageable), InvitationResponse::from);
    }

    /** FR-10: Owners/Admins invite by email; enforces the seat limit (FR-45). */
    @Transactional
    public InvitationResponse invite(InviteMemberRequest request) {
        UUID organizationId = SecurityUtils.currentOrganizationId();
        if (request.role() == Role.OWNER) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "A member cannot be invited as Owner.");
        }
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(ErrorCode.CONFLICT,
                    "A user with this email already has an account and belongs to an organization.");
        }
        invitationRepository
                .findByOrganization_IdAndEmailIgnoreCaseAndStatus(organizationId, email, InvitationStatus.PENDING)
                .ifPresent(existing -> {
                    throw new ApiException(ErrorCode.CONFLICT, "An invitation is already pending for this email.");
                });

        entitlementService.assertCanAddMember(organizationId);

        String rawToken = TokenHasher.generateToken();
        Invitation invitation = new Invitation();
        invitation.setOrganization(organizationRepository.getReferenceById(organizationId));
        invitation.setEmail(email);
        invitation.setRole(request.role());
        invitation.setTokenHash(TokenHasher.hash(rawToken));
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setInvitedBy(userRepository.getReferenceById(SecurityUtils.currentUserId()));
        invitation.setExpiresAt(Instant.now().plus(INVITATION_TTL_DAYS, ChronoUnit.DAYS));
        invitationRepository.save(invitation);

        auditService.record(AuditAction.MEMBER_INVITED, "Invitation", invitation.getId(),
                Map.of("email", email, "role", request.role().name()));

        emailService.sendInvitationEmail(
                email, invitation.getOrganization().getName(), request.role(), rawToken);

        return InvitationResponse.withToken(invitation, rawToken);
    }

    @Transactional
    public void revokeInvitation(UUID invitationId) {
        UUID organizationId = SecurityUtils.currentOrganizationId();
        Invitation invitation = invitationRepository.findByIdAndOrganization_Id(invitationId, organizationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Invitation not found."));
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ApiException(ErrorCode.CONFLICT, "Only pending invitations can be revoked.");
        }
        invitation.setStatus(InvitationStatus.REVOKED);
        auditService.record(AuditAction.INVITATION_REVOKED, "Invitation", invitation.getId(),
                Map.of("email", invitation.getEmail()));
    }

    /**
     * FR-11: an unregistered invitee registers into the org. Public (no authenticated
     * principal) — the tenant is taken from the invitation itself.
     */
    @Transactional
    public AuthResponse acceptInvitation(AcceptInvitationRequest request) {
        Invitation invitation = invitationRepository.findByTokenHash(TokenHasher.hash(request.token()))
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID, "Invalid invitation token."));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ApiException(ErrorCode.TOKEN_INVALID, "This invitation is no longer valid.");
        }
        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            throw new ApiException(ErrorCode.TOKEN_INVALID, "This invitation has expired.");
        }

        String email = invitation.getEmail();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(ErrorCode.CONFLICT, "An account already exists for this email.");
        }

        UUID organizationId = invitation.getOrganization().getId();
        entitlementService.assertCanAddMember(organizationId);

        User user = new User();
        user.setEmail(email);
        user.setName(request.name().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        Membership membership = new Membership();
        membership.setOrganization(invitation.getOrganization());
        membership.setUser(user);
        membership.setRole(invitation.getRole());
        membership.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(membership);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());

        auditService.recordFor(organizationId, user.getId(), AuditAction.MEMBER_JOINED,
                "Membership", membership.getId(), Map.of("email", email, "role", invitation.getRole().name()));

        return tokenIssuer.issue(user, membership);
    }

    /** FR-13: Owners change a member's role. Cannot touch the Owner or assign Owner. */
    @Transactional
    public MemberResponse updateRole(UUID membershipId, UpdateRoleRequest request) {
        if (request.role() == Role.OWNER) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Ownership cannot be reassigned through this endpoint.");
        }
        UUID organizationId = SecurityUtils.currentOrganizationId();
        Membership membership = membershipRepository.findByIdAndOrganization_Id(membershipId, organizationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Member not found."));
        if (membership.getRole() == Role.OWNER) {
            throw new ApiException(ErrorCode.FORBIDDEN, "The organization owner's role cannot be changed.");
        }

        Role previous = membership.getRole();
        membership.setRole(request.role());
        auditService.record(AuditAction.MEMBER_ROLE_CHANGED, "Membership", membership.getId(),
                Map.of("userId", membership.getUser().getId().toString(),
                        "from", previous.name(), "to", request.role().name()));
        return MemberResponse.from(membership);
    }

    /** FR-13: Owners remove a member (soft-remove, frees the seat). */
    @Transactional
    public void removeMember(UUID membershipId) {
        UUID organizationId = SecurityUtils.currentOrganizationId();
        Membership membership = membershipRepository.findByIdAndOrganization_Id(membershipId, organizationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Member not found."));
        if (membership.getRole() == Role.OWNER) {
            throw new ApiException(ErrorCode.FORBIDDEN, "The organization owner cannot be removed.");
        }
        if (membership.getUser().getId().equals(SecurityUtils.currentUserId())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "You cannot remove yourself.");
        }
        membership.setStatus(MembershipStatus.REMOVED);
        auditService.record(AuditAction.MEMBER_REMOVED, "Membership", membership.getId(),
                Map.of("userId", membership.getUser().getId().toString(),
                        "email", membership.getUser().getEmail()));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
