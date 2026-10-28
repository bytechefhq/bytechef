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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.domain.BillingSubscription.SubscriptionModel;
import com.bytechef.platform.billing.handler.SubscriptionModelHandler;
import com.bytechef.platform.billing.handler.SubscriptionModelHandlerRegistry;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import com.bytechef.platform.billing.service.BillingWebhookEventService;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Matija Petanjek
 */
@ExtendWith(MockitoExtension.class)
class BillingWebhookEventHandlerTest {

    private static final String WEBHOOK_SECRET = "whsec_test_webhook_secret";
    private static final String SUBSCRIPTION_ID = "sub_test123";
    private static final String CHECKOUT_SUBSCRIPTION_ID = "sub_checkout_test";

    @Mock
    private BillingSubscriptionService billingSubscriptionService;

    @Mock
    private BillingWebhookEventService billingWebhookEventService;

    @Mock
    private SubscriptionModelHandler subscriptionModelHandler;

    @Mock
    private SubscriptionModelHandlerRegistry subscriptionModelHandlerRegistry;

    private BillingWebhookEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new BillingWebhookEventHandler(
            billingSubscriptionService, billingWebhookEventService, subscriptionModelHandlerRegistry);
    }

    @Test
    void testHandleSkipsDuplicateEvent() throws Exception {
        when(billingWebhookEventService.isEventProcessed(any())).thenReturn(true);

        handler.handle(buildEvent(subscriptionUpdatedPayload(SUBSCRIPTION_ID, "STARTER")));

        verify(billingSubscriptionService, never()).fetchSubscriptionBySubscriptionId(any());
        verify(billingSubscriptionService, never()).save(any());
        verify(billingWebhookEventService, never()).save(any());
    }

    @Test
    void testHandleSubscriptionUpdatedRoutesBySubscriptionModel() throws Exception {
        BillingSubscription subscription = meteredSubscription();

        when(billingWebhookEventService.isEventProcessed(any())).thenReturn(false);
        when(billingSubscriptionService.fetchSubscriptionBySubscriptionId(SUBSCRIPTION_ID))
            .thenReturn(Optional.of(subscription));
        when(subscriptionModelHandlerRegistry.getSubscriptionModelHandler(SubscriptionModel.METERED))
            .thenReturn(subscriptionModelHandler);

        handler.handle(buildEvent(subscriptionUpdatedPayload(SUBSCRIPTION_ID, "STARTER")));

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);

        verify(subscriptionModelHandler).handleSubscriptionUpdated(any(), captor.capture());

        assertThat(captor.getValue()
            .getId()).isEqualTo(SUBSCRIPTION_ID);
    }

    @Test
    void testHandleCheckoutSessionCompletedCancelsExistingTrialSubscription() throws Exception {
        BillingSubscription trialSubscription = new BillingSubscription();

        trialSubscription.setPlanName("TRIAL");
        trialSubscription.setStatus(BillingSubscription.Status.ACTIVE);

        when(billingWebhookEventService.isEventProcessed(any())).thenReturn(false);
        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(trialSubscription));
        when(subscriptionModelHandlerRegistry.getSubscriptionModelHandler(SubscriptionModel.METERED))
            .thenReturn(subscriptionModelHandler);

        handler
            .handle(buildEvent(checkoutSessionCompletedPayload(CHECKOUT_SUBSCRIPTION_ID, "cus_checkout", "STARTER")));

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());

        BillingSubscription canceledTrial = captor.getValue();

        assertThat(canceledTrial).isSameAs(trialSubscription);
        assertThat(canceledTrial.getStatus()).isEqualTo(BillingSubscription.Status.CANCELED);

        verify(subscriptionModelHandler).handleCheckoutSessionCompleted(any(Session.class));
    }

    @Test
    void testHandleCheckoutSessionCompletedDoesNotCancelNonTrialSubscription() throws Exception {
        BillingSubscription existingSubscription = starterSubscription();

        when(billingWebhookEventService.isEventProcessed(any())).thenReturn(false);
        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(existingSubscription));
        when(subscriptionModelHandlerRegistry.getSubscriptionModelHandler(SubscriptionModel.METERED))
            .thenReturn(subscriptionModelHandler);

        handler
            .handle(buildEvent(checkoutSessionCompletedPayload(CHECKOUT_SUBSCRIPTION_ID, "cus_checkout", "STARTER")));

        verify(billingSubscriptionService, never()).save(any(BillingSubscription.class));
        assertThat(existingSubscription.getStatus()).isEqualTo(BillingSubscription.Status.ACTIVE);
    }

    @Test
    void testHandleCheckoutSessionCompletedSkipsAlreadyCanceledTrialSubscription() throws Exception {
        BillingSubscription canceledTrial = new BillingSubscription();

        canceledTrial.setPlanName("TRIAL");
        canceledTrial.setStatus(BillingSubscription.Status.CANCELED);

        when(billingWebhookEventService.isEventProcessed(any())).thenReturn(false);
        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(canceledTrial));
        when(subscriptionModelHandlerRegistry.getSubscriptionModelHandler(SubscriptionModel.METERED))
            .thenReturn(subscriptionModelHandler);

        handler
            .handle(buildEvent(checkoutSessionCompletedPayload(CHECKOUT_SUBSCRIPTION_ID, "cus_checkout", "STARTER")));

        verify(billingSubscriptionService, never()).save(any(BillingSubscription.class));
    }

    private BillingSubscription meteredSubscription() {
        BillingSubscription subscription = starterSubscription();

        subscription.setSubscriptionModel(SubscriptionModel.METERED);

        return subscription;
    }

    private BillingSubscription starterSubscription() {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setPlanName("Starter");
        subscription.setStatus(BillingSubscription.Status.ACTIVE);

        return subscription;
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private String subscriptionUpdatedPayload(String subscriptionId, String planName) {
        return """
            {
              "id": "evt_sub_updated",
              "object": "event",
              "api_version": "2026-04-22.dahlia",
              "type": "customer.subscription.updated",
              "data": {
                "object": {
                  "id": "%s",
                  "object": "subscription",
                  "status": "active",
                  "metadata": { "planName": "%s", "tenantId": "public" },
                  "items": { "object": "list", "data": [], "has_more": false, "url": "/v1/subscription_items" }
                }
              }
            }
            """.formatted(subscriptionId, planName);
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private String checkoutSessionCompletedPayload(String subscriptionId, String customerId, String planName) {
        return """
            {
              "id": "evt_checkout_completed",
              "object": "event",
              "api_version": "2026-04-22.dahlia",
              "type": "checkout.session.completed",
              "data": {
                "object": {
                  "id": "cs_test_checkout",
                  "object": "checkout.session",
                  "client_reference_id": "public",
                  "customer": "%s",
                  "subscription": "%s",
                  "metadata": { "planName": "%s" }
                }
              }
            }
            """.formatted(customerId, subscriptionId, planName);
    }

    private Event buildEvent(String payload) throws Exception {
        return Webhook.constructEvent(payload, signPayload(payload, WEBHOOK_SECRET), WEBHOOK_SECRET);
    }

    private String signPayload(String payload, String secret) throws Exception {
        long timestamp = Instant.now()
            .getEpochSecond();
        String signedPayload = timestamp + "." + payload;

        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

        String signature = HexFormat.of()
            .formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));

        return "t=" + timestamp + ",v1=" + signature;
    }
}
