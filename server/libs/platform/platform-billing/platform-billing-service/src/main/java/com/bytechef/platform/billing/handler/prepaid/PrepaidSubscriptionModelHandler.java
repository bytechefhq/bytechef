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

package com.bytechef.platform.billing.handler.prepaid;

import com.bytechef.platform.billing.client.StripeClient;
import com.bytechef.platform.billing.config.BillingProperties;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.domain.BillingSubscription.SubscriptionModel;
import com.bytechef.platform.billing.dto.BillingSubscriptionDTO;
import com.bytechef.platform.billing.handler.SubscriptionModelHandler;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import com.bytechef.platform.billing.service.BillingUsageService;
import com.bytechef.tenant.TenantContext;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Billing behavior of the {@link SubscriptionModel#PREPAID} model.
 *
 * @author Matija Petanjek
 */
@Component
@ConditionalOnProperty(prefix = "billing", name = "enabled", havingValue = "true")
public class PrepaidSubscriptionModelHandler implements SubscriptionModelHandler {

    private static final Logger log = LoggerFactory.getLogger(PrepaidSubscriptionModelHandler.class);

    private final BillingProperties billingProperties;
    private final BillingSubscriptionService billingSubscriptionService;
    private final BillingUsageService billingUsageService;
    private final PrepaidStripeClient prepaidStripeClient;
    private final StripeClient stripeClient;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public PrepaidSubscriptionModelHandler(
        BillingProperties billingProperties, BillingSubscriptionService billingSubscriptionService,
        BillingUsageService billingUsageService, PrepaidStripeClient prepaidStripeClient,
        StripeClient stripeClient) {

        this.billingProperties = billingProperties;
        this.billingSubscriptionService = billingSubscriptionService;
        this.billingUsageService = billingUsageService;
        this.prepaidStripeClient = prepaidStripeClient;
        this.stripeClient = stripeClient;
    }

    @Override
    public SubscriptionModel getSubscriptionModel() {
        return SubscriptionModel.PREPAID;
    }

    @Override
    public String createCheckoutSession(String planName) {
        String flatPriceId = stripeClient.fetchProductDefaultPriceId(resolveFlatProductId(planName));

        String customerId = getCustomerId();

        Session session = prepaidStripeClient.createSubscriptionCheckoutSession(
            customerId, flatPriceId, planName, billingProperties.stripe()
                .successUrl(),
            billingProperties.stripe()
                .cancelUrl(),
            TenantContext.getCurrentTenantId());

        return session.getUrl();
    }

    @Override
    public void updateSubscription(BillingSubscription currentSubscription, String newPlanName) {
        String subscriptionId = currentSubscription.getSubscriptionId();

        Subscription stripeSubscription = stripeClient.retrieveSubscription(subscriptionId);

        stripeClient.releaseSubscriptionScheduleIfPresent(stripeSubscription);

        String newFlatPriceId = stripeClient.fetchProductDefaultPriceId(resolveFlatProductId(newPlanName));
        String tenantId = TenantContext.getCurrentTenantId();

        if (isUpgrade(currentSubscription.getPlanName(), newPlanName)) {
            stripeClient.upgradeSubscriptionNow(
                subscriptionId, currentSubscription.getProductId(), newFlatPriceId, newPlanName, tenantId);
        } else {
            prepaidStripeClient.scheduleDowngrade(
                subscriptionId, newFlatPriceId, newPlanName, tenantId, currentSubscription.getCurrentPeriodEnd()
                    .getEpochSecond());
        }
    }

    @Override
    public void cancelSubscription(BillingSubscription currentSubscription) {
        stripeClient.cancelAtPeriodEnd(currentSubscription.getSubscriptionId(), TenantContext.getCurrentTenantId());
    }

    @Override
    public void reactivateSubscription(BillingSubscription currentSubscription) {
        stripeClient.reactivateSubscription(
            currentSubscription.getSubscriptionId(), TenantContext.getCurrentTenantId());
    }

    @Override
    public BillingSubscriptionDTO toBillingSubscriptionDTO(BillingSubscription subscription) {
        int jobsExecuted = billingUsageService.countJobExecutionsSince(
            subscription.getCurrentPeriodStart(), Instant.now());

        String scheduledPlanName = stripeClient.fetchScheduledPlanName(subscription.getSubscriptionId())
            .orElse(null);

        return new BillingSubscriptionDTO(subscription, jobsExecuted, scheduledPlanName);
    }

    @Override
    public BillingSubscription handleCheckoutSessionCompleted(Session session) {
        throw new IllegalStateException("Prepaid webhook handling not implemented yet");
    }

    @Override
    public BillingSubscription handleSubscriptionUpdated(
        BillingSubscription subscription, Subscription stripeSubscription) {

        throw new IllegalStateException("Prepaid webhook handling not implemented yet");
    }

    @Override
    public BillingSubscription handleSubscriptionDeleted(BillingSubscription subscription) {
        subscription.setStatus(BillingSubscription.Status.CANCELED);

        return billingSubscriptionService.save(subscription);
    }

    private String getCustomerId() {
        String tenantId = TenantContext.getCurrentTenantId();
        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();

        if (authentication == null) {
            throw new IllegalStateException("Current user is not set!");
        }

        String userEmail = authentication.getName();

        return stripeClient.fetchCustomerId(userEmail, tenantId)
            .orElseGet(() -> {
                log.info("No existing Stripe customer found for email {} and tenant id {}", userEmail, tenantId);

                return stripeClient.createCustomer(userEmail, tenantId);
            });
    }

    private boolean isUpgrade(String currentPlanName, String newPlanName) {
        return planTier(newPlanName) > planTier(currentPlanName);
    }

    private int planTier(String planName) {
        if ("STARTER".equalsIgnoreCase(planName)) {
            return 1;
        } else if ("GROWTH".equalsIgnoreCase(planName)) {
            return 2;
        }

        return 0;
    }

    private String resolveFlatProductId(String planName) {
        if ("STARTER".equalsIgnoreCase(planName)) {
            return billingProperties.stripe()
                .productStarterId();
        } else if ("GROWTH".equalsIgnoreCase(planName)) {
            return billingProperties.stripe()
                .productGrowthId();
        }

        throw new IllegalArgumentException("Unknown plan: " + planName);
    }
}
