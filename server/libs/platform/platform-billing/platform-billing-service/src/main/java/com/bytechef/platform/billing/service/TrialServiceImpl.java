/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.billing.service;

import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.dto.TrialDTO;
import com.bytechef.platform.billing.event.TrialExpiredEvent;
import com.bytechef.tenant.TenantContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * @author Matija Petanjek
 */
@Service
@ConditionalOnProperty(prefix = "billing", name = "enabled", havingValue = "true")
public class TrialServiceImpl implements TrialService {

    static final String TRIAL_STATUS_CACHE = "trialStatus";

    private final ApplicationEventPublisher applicationEventPublisher;
    private final BillingSubscriptionService billingSubscriptionService;
    private final BillingUsageService billingUsageService;

    public TrialServiceImpl(
        ApplicationEventPublisher applicationEventPublisher,
        BillingSubscriptionService billingSubscriptionService,
        BillingUsageService billingUsageService) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.billingSubscriptionService = billingSubscriptionService;
        this.billingUsageService = billingUsageService;
    }

    @Override
    @Cacheable(cacheNames = TRIAL_STATUS_CACHE)
    public TrialDTO validateTrial() {
        Optional<BillingSubscription> subscriptionOptional = billingSubscriptionService.fetchCurrentSubscription();

        if (subscriptionOptional.isEmpty()) {
            throw new IllegalStateException("No active subscription found");
        }

        BillingSubscription subscription = subscriptionOptional.get();

        if (!"TRIAL".equals(subscription.getPlanName())) {
            return new TrialDTO(false, 0, 0, 0);
        }

        Instant now = Instant.now();

        int tasksUsed = billingUsageService.countTaskExecutionsSince(
            subscription.getCurrentPeriodStart(), now);

        boolean timeExpired = now.isAfter(subscription.getCurrentPeriodEnd());
        boolean productUnitLimitReached = tasksUsed >= subscription.getProductUnitLimit();
        boolean expired = timeExpired || productUnitLimitReached;

        if (subscription.getStatus() == BillingSubscription.Status.ACTIVE && expired) {
            expireTrial(subscription);
        }

        long daysRemaining = Math.max(0, ChronoUnit.DAYS.between(now, subscription.getCurrentPeriodEnd()));

        return new TrialDTO(expired, daysRemaining, tasksUsed, subscription.getProductUnitLimit());
    }

    private void expireTrial(BillingSubscription subscription) {
        subscription.setStatus(BillingSubscription.Status.CANCELED);
        billingSubscriptionService.save(subscription);

        applicationEventPublisher.publishEvent(
            new TrialExpiredEvent(this, TenantContext.getCurrentTenantId()));
    }

}
