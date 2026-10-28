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

import com.bytechef.platform.billing.config.BillingProperties;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.domain.BillingSubscription.SubscriptionModel;
import com.bytechef.platform.billing.domain.BillingSubscriptionWebhookEvent;
import com.bytechef.platform.billing.handler.SubscriptionModelHandlerRegistry;
import com.bytechef.platform.billing.handler.metered.MeteredSubscriptionModelHandler;
import com.bytechef.platform.billing.handler.prepaid.PrepaidStripeClient;
import com.bytechef.platform.billing.handler.prepaid.PrepaidSubscriptionModelHandler;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import com.bytechef.platform.billing.service.BillingWebhookEventService;
import com.stripe.model.Event;
import com.stripe.model.LineItem;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(prefix = "billing", name = "enabled", havingValue = "true")
class BillingWebhookEventHandler {

    private static final Logger log = LoggerFactory.getLogger(BillingWebhookEventHandler.class);

    private final BillingProperties billingProperties;
    private final BillingSubscriptionService billingSubscriptionService;
    private final BillingWebhookEventService billingWebhookEventService;
    private final MeteredSubscriptionModelHandler meteredSubscriptionModelHandler;
    private final PrepaidStripeClient prepaidStripeClient;
    private final PrepaidSubscriptionModelHandler prepaidSubscriptionModelHandler;
    private final SubscriptionModelHandlerRegistry subscriptionModelHandlerRegistry;

    BillingWebhookEventHandler(
        BillingProperties billingProperties, BillingSubscriptionService billingSubscriptionService,
        BillingWebhookEventService billingWebhookEventService,
        MeteredSubscriptionModelHandler meteredSubscriptionModelHandler,
        PrepaidSubscriptionModelHandler prepaidSubscriptionModelHandler, PrepaidStripeClient prepaidStripeClient,
        SubscriptionModelHandlerRegistry subscriptionModelHandlerRegistry) {

        this.billingProperties = billingProperties;
        this.billingSubscriptionService = billingSubscriptionService;
        this.billingWebhookEventService = billingWebhookEventService;
        this.meteredSubscriptionModelHandler = meteredSubscriptionModelHandler;
        this.prepaidStripeClient = prepaidStripeClient;
        this.prepaidSubscriptionModelHandler = prepaidSubscriptionModelHandler;
        this.subscriptionModelHandlerRegistry = subscriptionModelHandlerRegistry;
    }

    @Transactional
    void handle(Event event) {
        if (billingWebhookEventService.isEventProcessed(event.getId())) {
            log.info("Ignoring already processed webhook event: {}", event.getId());

            return;
        }

        log.info("Processing webhook event: {}", event);

        BillingSubscription savedSubscription = null;

        if (Objects.equals(event.getType(), StripeWebhookEvent.CHECKOUT_SESSION_COMPLETED.getType())) {
            savedSubscription = handleCheckoutSessionCompleted(event);
        } else if (Objects.equals(event.getType(), StripeWebhookEvent.CUSTOMER_SUBSCRIPTION_UPDATED.getType())) {
            savedSubscription = handleSubscriptionUpdated(event);
        } else if (Objects.equals(event.getType(), StripeWebhookEvent.CUSTOMER_SUBSCRIPTION_DELETED.getType())) {
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
                if (subscription.getSubscriptionModel() == SubscriptionModel.METERED) {
                    meteredSubscriptionModelHandler.switchToPrepaidIfNotMetered(subscription, stripeSubscription);
                }

                return subscriptionModelHandlerRegistry
                    .getSubscriptionModelHandler(subscription.getSubscriptionModel())
                    .handleSubscriptionUpdated(subscription, stripeSubscription);
            })
            .orElse(null);
    }

    private BillingSubscription handleSubscriptionDeleted(Event event) {
        Subscription stripeSubscription = (Subscription) event.getDataObjectDeserializer()
            .getObject()
            .orElseThrow(() -> new RuntimeException("Failed to deserialize subscription"));

        return billingSubscriptionService.fetchSubscriptionBySubscriptionId(stripeSubscription.getId())
            .map(subscription -> subscriptionModelHandlerRegistry
                .getSubscriptionModelHandler(subscription.getSubscriptionModel())
                .handleSubscriptionDeleted(subscription))
            .orElse(null);
    }

    private BillingSubscription handleCheckoutSessionCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer()
            .getObject()
            .orElseThrow(() -> new RuntimeException("Failed to deserialize checkout session"));

        List<LineItem> lineItems = prepaidStripeClient.retrieveCheckoutSessionLineItems(session.getId());

        String packageProductId = billingProperties.stripe()
            .productJobExecutionsPackageId();

        boolean packagePurchase = lineItems.stream()
            .anyMatch(lineItem -> Objects.equals(packageProductId, lineItem.getPrice()
                .getProduct()));

        // A package purchase is a one-off payment: it neither replaces a trial nor creates a subscription row.
        if (packagePurchase) {
            return prepaidSubscriptionModelHandler.handleJobExecutionsPackagePurchased(session, lineItems);
        }

        cancelTrialSubscription();

        Map<String, String> metadata = session.getMetadata();

        String subscriptionModelName = metadata == null ? null : metadata.get("subscriptionModel");

        // Only sessions created before the PREPAID deploy lack the metadata; they are METERED.
        SubscriptionModel subscriptionModel = Objects.equals(SubscriptionModel.PREPAID.name(), subscriptionModelName)
            ? SubscriptionModel.PREPAID : SubscriptionModel.METERED;

        return subscriptionModelHandlerRegistry.getSubscriptionModelHandler(subscriptionModel)
            .handleCheckoutSessionCompleted(session);
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
}
