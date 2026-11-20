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

package com.bytechef.platform.billing.facade;

import com.bytechef.platform.billing.client.StripeClient;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import com.bytechef.platform.billing.service.BillingUsageService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @author Matija Petanjek
 */
@Service
@ConditionalOnProperty(prefix = "billing", name = "enabled", havingValue = "true")
public class BillingUsageFacadeImpl implements BillingUsageFacade {

    private static final Logger log = LoggerFactory.getLogger(BillingUsageFacadeImpl.class);

    private final BillingSubscriptionService billingSubscriptionService;
    private final BillingUsageService billingUsageService;
    private final StripeClient stripeClient;
    private final TenantService tenantService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public BillingUsageFacadeImpl(
        BillingSubscriptionService billingSubscriptionService, BillingUsageService billingUsageService,
        StripeClient stripeClient, TenantService tenantService) {

        this.billingSubscriptionService = billingSubscriptionService;
        this.billingUsageService = billingUsageService;
        this.stripeClient = stripeClient;
        this.tenantService = tenantService;
    }

    @Override
    public void reportUsage(Instant scheduledFireTime) {
        log.info("Tenant usage report START");

        for (String tenantId : tenantService.getTenantIds()) {
            TenantContext.runWithTenantId(tenantId, () -> doReportUsage(scheduledFireTime));
        }

        log.info("Tenant usage report END");
    }

    private void doReportUsage(Instant scheduledFireTime) {
        Optional<BillingSubscription> subscriptionOptional = billingSubscriptionService.fetchCurrentSubscription();

        if (subscriptionOptional.isEmpty()) {
            return;
        }

        BillingSubscription subscription = subscriptionOptional.get();

        if ("TRIAL".equals(subscription.getPlanName())) {
            return;
        }

        if (subscription.getSubscriptionModel() != BillingSubscription.SubscriptionModel.METERED) {
            return;
        }

        Instant lowerBound = subscription.getLastReportedAt() != null
            ? subscription.getLastReportedAt()
            : subscription.getCurrentPeriodStart();

        int count = billingUsageService.countJobExecutionsSince(lowerBound, scheduledFireTime);

        if (count > 0) {
            String idempotencyKey = subscription.getSubscriptionId() + "_" + scheduledFireTime.getEpochSecond();

            stripeClient.reportMeterEvent(subscription.getCustomerId(), count, idempotencyKey);
        }

        subscription.setLastReportedAt(scheduledFireTime);

        billingSubscriptionService.save(subscription);
    }
}
