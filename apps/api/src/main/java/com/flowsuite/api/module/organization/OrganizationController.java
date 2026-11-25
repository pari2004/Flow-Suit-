package com.flowsuite.api.module.organization;

import com.flowsuite.api.module.organization.dto.OrganizationResponse;
import com.flowsuite.api.module.organization.dto.UpdateOrganizationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organization")
@Tag(name = "Organization", description = "The authenticated user's organization")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    @Operation(summary = "Get the current organization")
    public OrganizationResponse getCurrent() {
        return organizationService.getCurrent();
    }

    @PatchMapping
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Update organization settings (Owner only)")
    public OrganizationResponse update(@Valid @RequestBody UpdateOrganizationRequest request) {
        return organizationService.update(request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Delete the organization and all its data (Owner only)")
    public void delete() {
        organizationService.delete();
    }
}
