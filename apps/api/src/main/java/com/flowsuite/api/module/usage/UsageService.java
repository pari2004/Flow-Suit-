package com.flowsuite.api.module.usage;

import com.flowsuite.api.common.error.ApiException;
import com.flowsuite.api.common.error.ErrorCode;
import com.flowsuite.api.domain.entity.ApiUsage;
import com.flowsuite.api.domain.entity.Plan;
import com.flowsuite.api.domain.entity.Subscription;
import com.flowsuite.api.module.billing.EntitlementService;
import com.flowsuite.api.repository.ApiUsageRepository;
import com.flowsuite.api.repository.OrganizationRepository;
import com.flowsuite.api.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Per-organization monthly API usage, counted in Redis (fast atomic increments) and
 * enforced against the effective plan's limit (FR-45). Counters are keyed by UTC month, so
 * a new month starts at zero automatically; the scheduled {@link UsageResetJob} persists
 * the closing count of each period to {@code api_usage} for the dashboard/history.
 */
@Service
public class UsageService {

    private static final Logger log = LoggerFactory.getLogger(UsageService.class);
    private static final String KEY_PREFIX = "usage:api:";
    private static final Duration KEY_TTL = Duration.ofDays(40);

    private final StringRedisTemplate redis;
    private final EntitlementService entitlementService;
    private final SubscriptionRepository subscriptionRepository;
    private final ApiUsageRepository apiUsageRepository;
    private final OrganizationRepository organizationRepository;

    public UsageService(StringRedisTemplate redis,
                        EntitlementService entitlementService,
                        SubscriptionRepository subscriptionRepository,
                        ApiUsageRepository apiUsageRepository,
                        OrganizationRepository organizationRepository) {
        this.redis = redis;
        this.entitlementService = entitlementService;
        this.subscriptionRepository = subscriptionRepository;
        this.apiUsageRepository = apiUsageRepository;
        this.organizationRepository = organizationRepository;
    }

    /** Increment the current month's counter and reject the request if the plan limit is exceeded. */
    public void incrementAndEnforce(UUID organizationId) {
        String key = key(organizationId, currentPeriod());
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, KEY_TTL);
        }
        long limit = entitlementService.effectivePlan(organizationId).getApiRequestLimit();
        if (count != null && count > limit) {
            throw new ApiException(ErrorCode.API_QUOTA_EXCEEDED, String.format(
                    "You've reached your plan's limit of %,d API requests this month. "
                            + "Upgrade your plan for a higher quota.", limit));
        }
    }

    /** Live request count for the current month. */
    public long currentUsage(UUID organizationId) {
        return readCount(organizationId, currentPeriod());
    }

    public long currentLimit(UUID organizationId) {
        return entitlementService.effectivePlan(organizationId).getApiRequestLimit();
    }

    /**
     * Monthly reset (FR, scheduled job): persist the just-closed period's count to the
     * database for every org, then drop the Redis key. New-month counters already started
     * fresh because keys are month-scoped.
     */
    @Transactional
    public void runMonthlyReset() {
        YearMonth previous = YearMonth.now(ZoneOffset.UTC).minusMonths(1);
        int processed = 0;
        for (Subscription subscription : subscriptionRepository.findAll()) {
            UUID organizationId = subscription.getOrganization().getId();
            long count = readCount(organizationId, previous);
            persistUsage(organizationId, previous, count);
            redis.delete(key(organizationId, previous));
            processed++;
        }
        log.info("[usage-reset] persisted {} organization usage records for {}", processed, previous);
    }

    private void persistUsage(UUID organizationId, YearMonth period, long count) {
        LocalDate periodStart = period.atDay(1);
        ApiUsage usage = apiUsageRepository.findByOrganization_IdAndPeriodStart(organizationId, periodStart)
                .orElseGet(() -> {
                    ApiUsage created = new ApiUsage();
                    created.setOrganization(organizationRepository.getReferenceById(organizationId));
                    created.setPeriodStart(periodStart);
                    return created;
                });
        usage.setRequestCount(count);
        apiUsageRepository.save(usage);
    }

    private long readCount(UUID organizationId, YearMonth period) {
        String value = redis.opsForValue().get(key(organizationId, period));
        return value == null ? 0L : Long.parseLong(value);
    }

    private static YearMonth currentPeriod() {
        return YearMonth.now(ZoneOffset.UTC);
    }

    private static String key(UUID organizationId, YearMonth period) {
        return KEY_PREFIX + organizationId + ":" + period;
    }
}
