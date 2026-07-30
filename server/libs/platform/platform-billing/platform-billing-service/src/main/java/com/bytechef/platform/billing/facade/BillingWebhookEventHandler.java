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
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import com.bytechef.platform.billing.service.BillingWebhookEventService;
import com.stripe.model.Event;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies an already signature-verified Stripe {@link Event} to billing state. Split out from
 * {@link BillingSubscriptionFacadeImpl} so that {@code handle} can be reached through the Spring proxy (and therefore
 * honor {@link Transactional}) when invoked from {@link BillingWebhookFacadeImpl} - a plain internal call would bypass
 * the proxy and silently skip the transaction.
 *
 * @author Matija Petanjek
 */
@Service
class BillingWebhookEventHandler {

    private static final Logger log = LoggerFactory.getLogger(BillingWebhookEventHandler.class);

    private final BillingProperties billingProperties;
    private final BillingSubscriptionService billingSubscriptionService;
    private final BillingWebhookEventService billingWebhookEventService;
    private final StripeClient stripeClient;

    BillingWebhookEventHandler(
        BillingProperties billingProperties, BillingSubscriptionService billingSubscriptionService,
        BillingWebhookEventService billingWebhookEventService, StripeClient stripeClient) {

        this.billingProperties = billingProperties;
        this.billingSubscriptionService = billingSubscriptionService;
        this.billingWebhookEventService = billingWebhookEventService;
        this.stripeClient = stripeClient;
    }

    @Transactional
    void handle(Event event) {
        if (billingWebhookEventService.isEventProcessed(event.getId())) {
            log.info("Ignoring already processed webhook event: {}", event.getId());

            return;
        }

        log.info("Processing webhook event: {}", event);

        BillingSubscription savedSubscription = null;

        if ("checkout.session.completed".equals(event.getType())) {
            savedSubscription = handleCheckoutSessionCompleted(event);
        } else if ("customer.subscription.updated".equals(event.getType())) {
            savedSubscription = handleSubscriptionUpdated(event);
        } else if ("customer.subscription.deleted".equals(event.getType())) {
            savedSubscription = handleSubscriptionDeleted(event);
        }

        BillingSubscriptionWebhookEvent webhookEvent = new BillingSubscriptionWebhookEvent();

        webhookEvent.setEventId(event.getId());
        webhookEvent.setEventType(event.getType());

        if (savedSubscription != null) {
            webhookEvent.setSubscriptionId(savedSubscription.getId());
        }

        billingWebhookEventService.save(webhookEvent);
    }

    private BillingSubscription handleSubscriptionUpdated(Event event) {
        Subscription stripeSubscription = (Subscription) event.getDataObjectDeserializer()
            .getObject()
            .orElseThrow(() -> new RuntimeException("Failed to deserialize subscription"));

        return billingSubscriptionService.fetchSubscriptionBySubscriptionId(stripeSubscription.getId())
            .map(subscription -> {
                subscription.setStatus(
                    BillingSubscription.Status.fromProviderStatus(stripeSubscription.getStatus()));

                subscription.setCancelAtPeriodEnd(Boolean.TRUE.equals(stripeSubscription.getCancelAtPeriodEnd()));

                List<SubscriptionItem> items = stripeSubscription.getItems()
                    .getData();

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
                            subscription.setProductUnitLimit(getProductUnitLimit(flatItem));
                            subscription.setCurrentPeriodStart(newPeriodStart);
                            subscription.setCurrentPeriodEnd(
                                Instant.ofEpochSecond(flatItem.getCurrentPeriodEnd()));
                            subscription.setLastReportedAt(null);
                        }
                    });

                items.stream()
                    .filter(this::isMeteredItem)
                    .findFirst()
                    .ifPresent(usageItem -> subscription.setUsageProductId(usageItem.getId()));

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

        return billingSubscriptionService.fetchSubscriptionBySubscriptionId(stripeSubscription.getId())
            .map(subscription -> {
                subscription.setStatus(BillingSubscription.Status.CANCELED);

                return billingSubscriptionService.save(subscription);
            })
            .orElse(null);
    }

    private BillingSubscription handleCheckoutSessionCompleted(Event event) {
        cancelTrialSubscription();

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

        return billingSubscriptionService.save(billingSubscription);
    }

    private void cancelTrialSubscription() {
        billingSubscriptionService.fetchCurrentSubscription()
            .filter(subscription -> "TRIAL".equals(subscription.getPlanName()))
            .filter(subscription -> subscription.getStatus() != BillingSubscription.Status.CANCELED)
            .ifPresent(trialSubscription -> {
                trialSubscription.setStatus(BillingSubscription.Status.CANCELED);

                billingSubscriptionService.save(trialSubscription);
            });
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
