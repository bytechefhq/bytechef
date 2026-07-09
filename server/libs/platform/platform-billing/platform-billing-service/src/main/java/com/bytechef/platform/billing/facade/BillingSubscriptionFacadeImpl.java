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
import com.bytechef.platform.billing.config.BillingProperties;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.domain.BillingSubscriptionWebhookEvent;
import com.bytechef.platform.billing.dto.BillingSubscriptionDTO;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import com.bytechef.platform.billing.service.BillingUsageService;
import com.bytechef.platform.billing.service.BillingWebhookEventService;
import com.bytechef.tenant.TenantContext;
import com.stripe.model.Event;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Matija Petanjek
 */
@Service
public class BillingSubscriptionFacadeImpl implements BillingSubscriptionFacade {

    private final BillingProperties billingProperties;
    private final BillingSubscriptionService billingSubscriptionService;
    private final BillingUsageService billingUsageService;
    private final BillingWebhookEventService billingWebhookEventService;
    private final ObjectMapper objectMapper;
    private final StripeClient stripeClient;

    public BillingSubscriptionFacadeImpl(
        BillingProperties billingProperties,
        BillingSubscriptionService billingSubscriptionService,
        BillingUsageService billingUsageService,
        BillingWebhookEventService billingWebhookEventService,
        ObjectMapper objectMapper,
        StripeClient stripeClient) {

        this.billingProperties = billingProperties;
        this.billingSubscriptionService = billingSubscriptionService;
        this.billingUsageService = billingUsageService;
        this.billingWebhookEventService = billingWebhookEventService;
        this.objectMapper = objectMapper;
        this.stripeClient = stripeClient;
    }

    @Override
    public void cancelSubscription() {
        BillingSubscription subscription = billingSubscriptionService.fetchCurrentSubscription()
            .orElseThrow(() -> new IllegalStateException("No active subscription found"));

        stripeClient.cancelAtPeriodEnd(
            subscription.getStripeSubscriptionId(), TenantContext.getCurrentTenantId());
    }

    @Override
    public String createCheckoutSession(String planName) {
        String flatProductId = resolveFlatProductId(planName);
        String flatPriceId = stripeClient.fetchProductDefaultPriceId(flatProductId);
        String usagePriceId = stripeClient.fetchProductDefaultPriceId(billingProperties.stripe().productUsageId());

        String customerId = billingSubscriptionService.fetchExistingStripeCustomerId()
            .orElseGet(() -> {
                String tenantId = TenantContext.getCurrentTenantId();
                String userEmail = SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getName();

                return stripeClient.createCustomer(userEmail, tenantId);
            });

        Session session = stripeClient.createCheckoutSession(
            customerId, flatPriceId, usagePriceId, planName, billingProperties.stripe().successUrl(),
            billingProperties.stripe().cancelUrl(), TenantContext.getCurrentTenantId());

        return session.getUrl();
    }

    @Override
    public void handleWebhookEvent(String payload, String stripeSignatureHeader) {
        Event event = stripeClient.verifyWebhookSignature(payload, stripeSignatureHeader);

        TenantContext.runWithTenantId(extractTenantId(payload, event.getType()), () -> {
            if (billingWebhookEventService.isEventProcessed(event.getId())) {
                return;
            }

            BillingSubscription savedSubscription = null;

            if ("checkout.session.completed".equals(event.getType())) {
                savedSubscription = handleCheckoutSessionCompleted(event);
            } else if ("customer.subscription.updated".equals(event.getType())) {
                savedSubscription = handleSubscriptionUpdated(event);
            } else if ("customer.subscription.deleted".equals(event.getType())) {
                savedSubscription = handleSubscriptionDeleted(event);
            }

            BillingSubscriptionWebhookEvent webhookEvent = new BillingSubscriptionWebhookEvent();

            webhookEvent.setStripeEventId(event.getId());
            webhookEvent.setEventType(event.getType());

            if (savedSubscription != null) {
                webhookEvent.setSubscriptionId(savedSubscription.getId());
            }

            billingWebhookEventService.save(webhookEvent);
        });
    }

    @Override
    public void reactivateSubscription() {
        BillingSubscription subscription = billingSubscriptionService.fetchCurrentSubscription()
            .orElseThrow(() -> new IllegalStateException("No active subscription found"));

        stripeClient.reactivateSubscription(
            subscription.getStripeSubscriptionId(), TenantContext.getCurrentTenantId());
    }

    @Override
    public void upgradeSubscription(String planName) {
        BillingSubscription currentSubscription = billingSubscriptionService.fetchCurrentSubscription()
            .orElseThrow(() -> new IllegalStateException("No active subscription found"));

        String subscriptionId = currentSubscription.getStripeSubscriptionId();

        Subscription stripeSubscription = stripeClient.retrieveSubscription(subscriptionId);

        stripeClient.releaseSubscriptionScheduleIfPresent(stripeSubscription);

        String newFlatPriceId = stripeClient.fetchProductDefaultPriceId(resolveFlatProductId(planName));
        String tenantId = TenantContext.getCurrentTenantId();

        if (isUpgrade(currentSubscription.getPlanName(), planName)) {
            stripeClient.upgradeSubscriptionNow(
                subscriptionId, currentSubscription.getStripeProductId(), newFlatPriceId, planName, tenantId);

            currentSubscription.setScheduledPlanName(null);
        } else {
            String newMeteredPriceId =
                stripeClient.fetchProductDefaultPriceId(billingProperties.stripe().productUsageId());

            stripeClient.scheduleDowngrade(
                subscriptionId, currentSubscription.getStripeProductId(),
                currentSubscription.getStripeUsageProductId(), newFlatPriceId, newMeteredPriceId, planName,
                tenantId, currentSubscription.getCurrentPeriodEnd()
                    .getEpochSecond());

            currentSubscription.setScheduledPlanName(planName);
        }

        billingSubscriptionService.save(currentSubscription);
    }

    @Override
    public Optional<BillingSubscriptionDTO> fetchCurrentSubscription() {
        return billingSubscriptionService.fetchCurrentSubscription()
            .map(subscription -> {
                int tasksUsed = billingUsageService.countTaskExecutionsSince(
                    subscription.getCurrentPeriodStart(), Instant.now());

                return new BillingSubscriptionDTO(subscription, tasksUsed);
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

    private String extractTenantId(String payload, String eventType) {
        JsonNode dataObject = objectMapper.readTree(payload)
            .path("data")
            .path("object");

        if ("checkout.session.completed".equals(eventType)) {
            String clientReferenceId = dataObject.path("client_reference_id")
                .textValue();

            return clientReferenceId != null ? clientReferenceId : TenantContext.DEFAULT_TENANT_ID;
        }

        if ("customer.subscription.updated".equals(eventType) ||
            "customer.subscription.deleted".equals(eventType)) {

            String tenantId = dataObject.path("metadata")
                .path("tenantId")
                .textValue();

            return tenantId != null ? tenantId : TenantContext.DEFAULT_TENANT_ID;
        }

        return TenantContext.DEFAULT_TENANT_ID;
    }

    private BillingSubscription handleSubscriptionUpdated(Event event) {
        Subscription stripeSubscription = (Subscription) event.getDataObjectDeserializer()
            .getObject()
            .orElseThrow(() -> new RuntimeException("Failed to deserialize subscription"));

        return billingSubscriptionService.fetchSubscriptionByStripeSubscriptionId(stripeSubscription.getId())
            .map(subscription -> {
                subscription.setStatus(
                    BillingSubscription.Status.fromStripe(stripeSubscription.getStatus()));
                subscription.setCancelAtPeriodEnd(Boolean.TRUE.equals(stripeSubscription.getCancelAtPeriodEnd()));

                List<SubscriptionItem> items = stripeSubscription.getItems()
                    .getData();

                items.stream()
                    .filter(item -> !isMeteredItem(item))
                    .findFirst()
                    .ifPresent(flatItem -> {
                        subscription.setStripeProductId(flatItem.getId());

                        String productId = flatItem.getPrice()
                            .getProduct();
                        String newPlanName = null;

                        if (billingProperties.stripe().productStarterId()
                            .equals(productId)) {
                            newPlanName = "STARTER";
                        } else if (billingProperties.stripe().productGrowthId()
                            .equals(productId)) {
                            newPlanName = "GROWTH";
                        }

                        if (newPlanName != null && !newPlanName.equalsIgnoreCase(subscription.getPlanName())) {
                            subscription.setScheduledPlanName(null);
                        }

                        if (newPlanName != null) {
                            subscription.setPlanName(newPlanName);
                        }

                        Instant newPeriodStart = Instant.ofEpochSecond(flatItem.getCurrentPeriodStart());

                        if (!newPeriodStart.equals(subscription.getCurrentPeriodStart())) {
                            subscription.setCurrentPeriodStart(newPeriodStart);
                            subscription.setCurrentPeriodEnd(
                                Instant.ofEpochSecond(flatItem.getCurrentPeriodEnd()));
                            subscription.setLastReportedAt(null);
                        }
                    });

                items.stream()
                    .filter(this::isMeteredItem)
                    .findFirst()
                    .ifPresent(usageItem -> subscription.setStripeUsageProductId(usageItem.getId()));

                return billingSubscriptionService.save(subscription);
            })
            .orElse(null);
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

    private BillingSubscription handleSubscriptionDeleted(Event event) {
        Subscription stripeSubscription = (Subscription) event.getDataObjectDeserializer()
            .getObject()
            .orElseThrow(() -> new RuntimeException("Failed to deserialize subscription"));

        return billingSubscriptionService.fetchSubscriptionByStripeSubscriptionId(stripeSubscription.getId())
            .map(subscription -> {
                subscription.setStatus(BillingSubscription.Status.CANCELED);

                return billingSubscriptionService.save(subscription);
            })
            .orElse(null);
    }

    private String resolveFlatProductId(String planName) {
        if ("STARTER".equalsIgnoreCase(planName)) {
            return billingProperties.stripe().productStarterId();
        } else if ("GROWTH".equalsIgnoreCase(planName)) {
            return billingProperties.stripe().productGrowthId();
        }

        throw new IllegalArgumentException("Unknown plan: " + planName);
    }

    private BillingSubscription handleCheckoutSessionCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer()
            .getObject()
            .orElseThrow(() -> new RuntimeException("Failed to deserialize checkout session"));

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

        billingSubscription.setStripeCustomerId(session.getCustomer());
        billingSubscription.setStripeSubscriptionId(stripeSubscription.getId());
        billingSubscription.setStripeProductId(flatItem.getId());
        billingSubscription.setStripeUsageProductId(usageItem.getId());
        billingSubscription.setPlanName(planName);
        billingSubscription.setStatus(
            BillingSubscription.Status.fromStripe(stripeSubscription.getStatus()));
        billingSubscription.setTaskLimit(getTaskLimit(usageItem));
        billingSubscription.setCurrentPeriodStart(Instant.ofEpochSecond(flatItem.getCurrentPeriodStart()));
        billingSubscription.setCurrentPeriodEnd(Instant.ofEpochSecond(flatItem.getCurrentPeriodEnd()));
        billingSubscription.setCancelAtPeriodEnd(stripeSubscription.getCancelAtPeriodEnd());

        return billingSubscriptionService.save(billingSubscription);
    }

    private int getTaskLimit(SubscriptionItem usageItem) {
        Price usagePrice = stripeClient.retrievePrice(usageItem.getPrice().getId());

        return usagePrice.getTiers().getFirst().getUpTo().intValue();
    }
}
