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
import com.bytechef.platform.billing.repository.BillingSubscriptionRepository;
import com.bytechef.platform.billing.repository.PrepaidBillingSubscriptionRepository;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import com.bytechef.platform.billing.service.BillingUsageService;
import com.bytechef.tenant.TenantContext;
import com.stripe.model.LineItem;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final BillingSubscriptionRepository billingSubscriptionRepository;
    private final BillingSubscriptionService billingSubscriptionService;
    private final BillingUsageService billingUsageService;
    private final PrepaidBillingSubscriptionRepository prepaidBillingSubscriptionRepository;
    private final PrepaidStripeClient prepaidStripeClient;
    private final StripeClient stripeClient;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public PrepaidSubscriptionModelHandler(
        BillingProperties billingProperties, BillingSubscriptionRepository billingSubscriptionRepository,
        BillingSubscriptionService billingSubscriptionService, BillingUsageService billingUsageService,
        PrepaidBillingSubscriptionRepository prepaidBillingSubscriptionRepository,
        PrepaidStripeClient prepaidStripeClient, StripeClient stripeClient) {

        this.billingProperties = billingProperties;
        this.billingSubscriptionRepository = billingSubscriptionRepository;
        this.billingSubscriptionService = billingSubscriptionService;
        this.billingUsageService = billingUsageService;
        this.prepaidBillingSubscriptionRepository = prepaidBillingSubscriptionRepository;
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

    public String createJobExecutionsPackageCheckoutSession(int quantity) {
        String packagePriceId = stripeClient.fetchProductDefaultPriceId(
            billingProperties.stripe()
                .productJobExecutionsPackageId());

        String customerId = getCustomerId();

        Session session = prepaidStripeClient.createJobExecutionsPackageCheckoutSession(
            customerId, packagePriceId, quantity, billingProperties.stripe()
                .jobExecutionsPackageSuccessUrl(),
            billingProperties.stripe()
                .cancelUrl(),
            TenantContext.getCurrentTenantId());

        return session.getUrl();
    }

    /**
     * Grants the purchased units to the tenant's current row, whatever its state: the checkout may have been started
     * before the row became TRIAL or CANCELED. With no current row it throws so that Stripe retries the event.
     */
    public BillingSubscription handleJobExecutionsPackagePurchased(Session session, List<LineItem> lineItems) {
        BillingSubscription currentSubscription = billingSubscriptionService.fetchCurrentSubscription()
            .orElseThrow(() -> new IllegalStateException(
                "No subscription found to grant the job executions package of checkout session " + session.getId()));

        if (!"paid".equals(session.getPaymentStatus())) {
            log.warn(
                "Checkout session {} has payment status {}, not granting job executions package units",
                session.getId(), session.getPaymentStatus());

            return currentSubscription;
        }

        String packageProductId = billingProperties.stripe()
            .productJobExecutionsPackageId();

        int unitTotal = 0;

        for (LineItem lineItem : lineItems) {
            if (Objects.equals(packageProductId, lineItem.getPrice()
                .getProduct())) {

                unitTotal += PrepaidPriceUtils.getUnitTotal(lineItem.getPrice())
                    * Math.toIntExact(lineItem.getQuantity());
            }
        }

        prepaidBillingSubscriptionRepository.addExtraUnitTotal(currentSubscription.getId(), unitTotal);

        return currentSubscription;
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
        Subscription stripeSubscription = stripeClient.retrieveSubscription(session.getSubscription());

        SubscriptionItem flatItem = getFlatItem(stripeSubscription);

        Map<String, String> metadata = session.getMetadata();

        String planName = metadata != null ? metadata.getOrDefault("planName", "Starter") : "Starter";

        // Read before the insert below, otherwise the latest row would be the one being created.
        int unusedExtraUnits = billingSubscriptionRepository.findFirstByOrderByCreatedDateDesc()
            .map(previousSubscription -> Math.max(
                0, previousSubscription.getExtraUnitTotal() - previousSubscription.getExtraUnitUsed()))
            .orElse(0);

        BillingSubscription billingSubscription = new BillingSubscription();

        billingSubscription.setCustomerId(session.getCustomer());
        billingSubscription.setSubscriptionId(stripeSubscription.getId());
        billingSubscription.setProductId(flatItem.getId());
        billingSubscription.setPlanName(planName);
        billingSubscription.setStatus(
            BillingSubscription.Status.fromProviderStatus(stripeSubscription.getStatus()));
        billingSubscription.setPlanUnitTotal(PrepaidPriceUtils.getUnitTotal(flatItem.getPrice()));
        billingSubscription.setCurrentPeriodStart(Instant.ofEpochSecond(flatItem.getCurrentPeriodStart()));
        billingSubscription.setCurrentPeriodEnd(Instant.ofEpochSecond(flatItem.getCurrentPeriodEnd()));
        billingSubscription.setCancelAtPeriodEnd(Boolean.TRUE.equals(stripeSubscription.getCancelAtPeriodEnd()));
        billingSubscription.setSubscriptionModel(SubscriptionModel.PREPAID);

        BillingSubscription savedSubscription = billingSubscriptionService.save(billingSubscription);

        // extraUnitTotal is read-only on the entity, so the transfer is a separate atomic update after the insert.
        if (unusedExtraUnits > 0) {
            prepaidBillingSubscriptionRepository.addExtraUnitTotal(savedSubscription.getId(), unusedExtraUnits);
        }

        return savedSubscription;
    }

    @Override
    public BillingSubscription handleSubscriptionUpdated(
        BillingSubscription subscription, Subscription stripeSubscription) {

        SubscriptionItem flatItem = getFlatItem(stripeSubscription);

        Instant newPeriodStart = Instant.ofEpochSecond(flatItem.getCurrentPeriodStart());
        Instant storedPeriodStart = subscription.getCurrentPeriodStart();

        // Stripe does not guarantee webhook order: an event for an older period must not roll the period back, or the
        // next current-period event would reset plan_unit_used a second time.
        if (storedPeriodStart != null && newPeriodStart.isBefore(storedPeriodStart)) {
            log.info(
                "Ignoring stale subscription update for subscription {}: period start {} is before stored {}",
                subscription.getSubscriptionId(), newPeriodStart, storedPeriodStart);

            return subscription;
        }

        subscription.setStatus(
            BillingSubscription.Status.fromProviderStatus(stripeSubscription.getStatus()));

        subscription.setCancelAtPeriodEnd(Boolean.TRUE.equals(stripeSubscription.getCancelAtPeriodEnd()));

        subscription.setProductId(flatItem.getId());

        String newPlanName = resolvePlanName(flatItem.getPrice()
            .getProduct());

        boolean planChanged = newPlanName != null && !newPlanName.equalsIgnoreCase(subscription.getPlanName());

        if (newPlanName != null) {
            subscription.setPlanName(newPlanName);
        }

        boolean periodChanged = storedPeriodStart == null || newPeriodStart.isAfter(storedPeriodStart);

        if (periodChanged) {
            subscription.setCurrentPeriodStart(newPeriodStart);
            subscription.setCurrentPeriodEnd(Instant.ofEpochSecond(flatItem.getCurrentPeriodEnd()));
        }

        if (planChanged || periodChanged) {
            subscription.setPlanUnitTotal(PrepaidPriceUtils.getUnitTotal(flatItem.getPrice()));
        }

        BillingSubscription savedSubscription = billingSubscriptionService.save(subscription);

        if (periodChanged) {
            prepaidBillingSubscriptionRepository.resetPlanUnitUsed(savedSubscription.getId());
        }

        return savedSubscription;
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

    private SubscriptionItem getFlatItem(Subscription stripeSubscription) {
        List<SubscriptionItem> items = stripeSubscription.getItems()
            .getData();

        return items.stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Subscription " + stripeSubscription.getId() + " has no subscription item"));
    }

    private String resolvePlanName(String productId) {
        BillingProperties.Stripe stripe = billingProperties.stripe();

        if (Objects.equals(stripe.productStarterId(), productId)) {
            return "STARTER";
        } else if (Objects.equals(stripe.productGrowthId(), productId)) {
            return "GROWTH";
        }

        return null;
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
