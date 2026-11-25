package com.flowsuite.api.module.usage;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled monthly usage-reset job. Runs at 00:05 UTC on the first of each month and
 * delegates to {@link UsageService#runMonthlyReset()} to persist the closed period and
 * clear its Redis counter.
 */
@Component
public class UsageResetJob {

    private final UsageService usageService;

    public UsageResetJob(UsageService usageService) {
        this.usageService = usageService;
    }

    @Scheduled(cron = "0 5 0 1 * *", zone = "UTC")
    public void resetMonthlyUsage() {
        usageService.runMonthlyReset();
    }
}
