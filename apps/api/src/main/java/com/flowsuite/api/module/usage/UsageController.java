package com.flowsuite.api.module.usage;

import com.flowsuite.api.module.usage.dto.UsageDashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Usage dashboard (Owner/Admin): plan, seats/projects/API usage vs. limits (FR-50). */
@RestController
@RequestMapping("/api/v1/usage")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
@Tag(name = "Usage", description = "Usage dashboard")
public class UsageController {

    private final DashboardService dashboardService;

    public UsageController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Current plan with usage vs. limits")
    public UsageDashboardResponse dashboard() {
        return dashboardService.getDashboard();
    }
}
