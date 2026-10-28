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

package com.bytechef.platform.billing.handler.metered;

import com.bytechef.platform.billing.client.StripeClient;
import com.bytechef.platform.billing.config.BillingProperties;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.domain.BillingSubscription.SubscriptionModel;
import com.bytechef.platform.billing.dto.BillingSubscriptionDTO;
import com.bytechef.platform.billing.handler.SubscriptionModelHandler;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import com.bytechef.platform.billing.service.BillingUsageService;
import com.bytechef.tenant.TenantContext;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Billing behavior of the legacy {@link SubscriptionModel#METERED} model.
 *
 * @author Matija Petanjek
 */
@Component
@ConditionalOnProperty(prefix = "billing", name = "enabled", havingValue = "true")
public class MeteredSubscriptionModelHandler implements SubscriptionModelHandler {

    private static final Logger log = LoggerFactory.getLogger(MeteredSubscriptionModelHandler.class);

    private final BillingProperties billingProperties;
    private final BillingSubscriptionService billingSubscriptionService;
    private final BillingUsageService billingUsageService;
    private final StripeClient stripeClient;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public MeteredSubscriptionModelHandler(
        BillingProperties billingProperties,
        BillingSubscriptionService billingSubscriptionService,
        BillingUsageService billingUsageService,
        StripeClient stripeClient) {

        this.billingProperties = billingProperties;
        this.billingSubscriptionService = billingSubscriptionService;
        this.billingUsageService = billingUsageService;
        this.stripeClient = stripeClient;
    }

    @Override
    public SubscriptionModel getSubscriptionModel() {
        return SubscriptionModel.METERED;
    }

    @Override
    public void cancelSubscription(BillingSubscription subscription) {
        stripeClient.cancelAtPeriodEnd(
            subscription.getSubscriptionId(), TenantContext.getCurrentTenantId());
    }

    @Override
    public String createCheckoutSession(String planName) {
        String flatProductId = resolveFlatProductId(planName);
        String flatPriceId = stripeClient.fetchProductDefaultPriceId(flatProductId);
        String usagePriceId = stripeClient.fetchProductDefaultPriceId(billingProperties.stripe()
            .productUsageId());

        String customerId = getCustomerId();

        Session session = stripeClient.createCheckoutSession(
            customerId, flatPriceId, usagePriceId, planName, billingProperties.stripe()
                .successUrl(),
            billingProperties.stripe()
                .cancelUrl(),
            TenantContext.getCurrentTenantId());

        return session.getUrl();
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

    @Override
    public void reactivateSubscription(BillingSubscription subscription) {
        stripeClient.reactivateSubscription(
            subscription.getSubscriptionId(), TenantContext.getCurrentTenantId());
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
            String newMeteredPriceId =
                stripeClient.fetchProductDefaultPriceId(billingProperties.stripe()
                    .productUsageId());

            stripeClient.scheduleDowngrade(
                subscriptionId, currentSubscription.getProductId(),
                currentSubscription.getUsageProductId(), newFlatPriceId, newMeteredPriceId, newPlanName,
                tenantId, currentSubscription.getCurrentPeriodEnd()
                    .getEpochSecond());
        }
    }

    @Override
    public BillingSubscriptionDTO toBillingSubscriptionDTO(BillingSubscription subscription) {
        int jobsExecuted = billingUsageService.countJobExecutionsSince(
            subscription.getCurrentPeriodStart(), Instant.now());

        String scheduledPlanName = stripeClient.fetchScheduledPlanName(subscription.getSubscriptionId())
            .orElse(null);

        return new BillingSubscriptionDTO(subscription, jobsExecuted, scheduledPlanName);
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

    @Override
    public BillingSubscription handleSubscriptionUpdated(
        BillingSubscription subscription, Subscription stripeSubscription) {

        subscription.setStatus(
            BillingSubscription.Status.fromProviderStatus(stripeSubscription.getStatus()));

        subscription.setCancelAtPeriodEnd(Boolean.TRUE.equals(stripeSubscription.getCancelAtPeriodEnd()));

        List<SubscriptionItem> items = stripeSubscription.getItems()
            .getData();

        SubscriptionItem usageItem = items.stream()
            .filter(this::isMeteredItem)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Subscription missing usage product"));

        items.stream()
            .filter(item -> !isMeteredItem(item))
            .findFirst()
            .ifPresent(flatItem -> {
                subscription.setProductId(flatItem.getId());

                String productId = flatItem.getPrice()
                    .getProduct();
                String newPlanName = null;

                if (billingProperties.stripe()
                    .productStarterId()
                    .equals(productId)) {
                    newPlanName = "STARTER";
                } else if (billingProperties.stripe()
                    .productGrowthId()
                    .equals(productId)) {
                    newPlanName = "GROWTH";
                }

                if (newPlanName != null) {
                    subscription.setPlanName(newPlanName);
                }

                Instant newPeriodStart = Instant.ofEpochSecond(flatItem.getCurrentPeriodStart());

                if (!newPeriodStart.equals(subscription.getCurrentPeriodStart())) {
                    subscription.setProductUnitLimit(getProductUnitLimit(usageItem));
                    subscription.setCurrentPeriodStart(newPeriodStart);
                    subscription.setCurrentPeriodEnd(
                        Instant.ofEpochSecond(flatItem.getCurrentPeriodEnd()));
                    subscription.setLastReportedAt(null);
                }
            });

        subscription.setUsageProductId(usageItem.getId());

        return billingSubscriptionService.save(subscription);
    }

    private boolean isMeteredItem(SubscriptionItem item) {
        Price price = item.getPrice();

        if (price == null) {
            return false;
        }

        Price.Recurring recurring = price.getRecurring();

        if (recurring == null) {
            return false;
        }

        return "metered".equals(recurring.getUsageType()) || recurring.getMeter() != null;
    }

    @Override
    public BillingSubscription handleSubscriptionDeleted(BillingSubscription subscription) {
        subscription.setStatus(BillingSubscription.Status.CANCELED);

        return billingSubscriptionService.save(subscription);
    }

    @Override
    public BillingSubscription handleCheckoutSessionCompleted(Session session) {
        Subscription stripeSubscription = stripeClient.retrieveSubscription(session.getSubscription());

        List<SubscriptionItem> subscriptionItems = stripeSubscription.getItems()
            .getData();

        SubscriptionItem flatItem = subscriptionItems.stream()
            .filter(item -> !isMeteredItem(item))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No flat subscription item found"));

        SubscriptionItem usageItem = subscriptionItems.stream()
            .filter(this::isMeteredItem)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No metered subscription item found"));

        Map<String, String> metadata = session.getMetadata();

        String planName = metadata != null ? metadata.getOrDefault("planName", "Starter") : "Starter";

        BillingSubscription billingSubscription = new BillingSubscription();

        billingSubscription.setCustomerId(session.getCustomer());
        billingSubscription.setSubscriptionId(stripeSubscription.getId());
        billingSubscription.setProductId(flatItem.getId());
        billingSubscription.setUsageProductId(usageItem.getId());
        billingSubscription.setPlanName(planName);
        billingSubscription.setStatus(
            BillingSubscription.Status.fromProviderStatus(stripeSubscription.getStatus()));
        billingSubscription.setProductUnitLimit(getProductUnitLimit(usageItem));
        billingSubscription.setCurrentPeriodStart(Instant.ofEpochSecond(flatItem.getCurrentPeriodStart()));
        billingSubscription.setCurrentPeriodEnd(Instant.ofEpochSecond(flatItem.getCurrentPeriodEnd()));
        billingSubscription.setCancelAtPeriodEnd(stripeSubscription.getCancelAtPeriodEnd());
        billingSubscription.setSubscriptionModel(SubscriptionModel.METERED);

        return billingSubscriptionService.save(billingSubscription);
    }

    private int getProductUnitLimit(SubscriptionItem usageItem) {
        Price usagePrice = stripeClient.retrievePrice(usageItem.getPrice()
            .getId());

        return usagePrice.getTiers()
            .getFirst()
            .getUpTo()
            .intValue();
    }
}
