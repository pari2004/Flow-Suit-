package com.flowsuite.api.module.member;

import com.flowsuite.api.common.dto.PageResponse;
import com.flowsuite.api.module.member.dto.InvitationResponse;
import com.flowsuite.api.module.member.dto.InviteMemberRequest;
import com.flowsuite.api.module.member.dto.MemberResponse;
import com.flowsuite.api.module.member.dto.UpdateRoleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/members")
@Tag(name = "Members", description = "Organization membership and invitations")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    @Operation(summary = "List members of the organization")
    public PageResponse<MemberResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return memberService.listMembers(pageable);
    }

    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @Operation(summary = "Invite a member by email (Owner/Admin)")
    public InvitationResponse invite(@Valid @RequestBody InviteMemberRequest request) {
        return memberService.invite(request);
    }

    @GetMapping("/invitations")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @Operation(summary = "List invitations (Owner/Admin)")
    public PageResponse<InvitationResponse> listInvitations(@PageableDefault(size = 20) Pageable pageable) {
        return memberService.listInvitations(pageable);
    }

    @DeleteMapping("/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @Operation(summary = "Revoke a pending invitation (Owner/Admin)")
    public void revokeInvitation(@PathVariable UUID invitationId) {
        memberService.revokeInvitation(invitationId);
    }

    @PatchMapping("/{membershipId}/role")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Change a member's role (Owner only)")
    public MemberResponse updateRole(@PathVariable UUID membershipId,
                                     @Valid @RequestBody UpdateRoleRequest request) {
        return memberService.updateRole(membershipId, request);
    }

    @DeleteMapping("/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Remove a member (Owner only)")
    public void remove(@PathVariable UUID membershipId) {
        memberService.removeMember(membershipId);
    }
}
