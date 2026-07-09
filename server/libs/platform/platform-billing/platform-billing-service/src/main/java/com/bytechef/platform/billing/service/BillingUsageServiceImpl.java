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

import com.bytechef.platform.billing.client.StripeClientService;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.job.StripeUsageReportingJob;
import com.bytechef.platform.billing.repository.BillingSubscriptionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author Matija Petanjek
 */
@Service
public class BillingUsageServiceImpl implements BillingUsageService {

    private static final Logger logger = LoggerFactory.getLogger(BillingUsageServiceImpl.class);

    private final BillingSubscriptionRepository billingSubscriptionRepository;
    private final BillingSubscriptionService billingSubscriptionService;
    private final StripeClientService stripeClientService;
    private final TenantService tenantService;

    public BillingUsageServiceImpl(
        BillingSubscriptionRepository billingSubscriptionRepository,
        BillingSubscriptionService billingSubscriptionService,
        StripeClientService stripeClientService, TenantService tenantService) {

        this.billingSubscriptionRepository = billingSubscriptionRepository;
        this.billingSubscriptionService = billingSubscriptionService;
        this.stripeClientService = stripeClientService;
        this.tenantService = tenantService;
    }

    @Override
    public void reportUsage(Instant scheduledFireTime) {
       for (String tenantId: tenantService.getTenantIds()) {
           logger.info("Reporting usage for tenant {}", tenantId);

           TenantContext.runWithTenantId(tenantId, () -> doReportUsage(scheduledFireTime));
       }
    }

    private void doReportUsage(Instant scheduledFireTime) {
        Optional<BillingSubscription> subscriptionOptional = billingSubscriptionService.fetchCurrentSubscription();

        if (subscriptionOptional.isEmpty()) {
            return;
        }

        BillingSubscription subscription = subscriptionOptional.get();

        Instant lowerBound = subscription.getLastReportedAt() != null
            ? subscription.getLastReportedAt()
            : subscription.getCurrentPeriodStart();

        int count = countTaskExecutionsSince(lowerBound, scheduledFireTime);

        if (count > 0) {
            String idempotencyKey = subscription.getStripeSubscriptionId() + "_" + scheduledFireTime.getEpochSecond();

            stripeClientService.reportMeterEvent(subscription.getStripeCustomerId(), count, idempotencyKey);
        }

        subscription.setLastReportedAt(scheduledFireTime);

        billingSubscriptionService.save(subscription);
    }

    @Override
    public int countTaskExecutionsSince(Instant from, Instant to) {
        return billingSubscriptionRepository.countCompletedTaskExecutions(from, to);
    }
}
