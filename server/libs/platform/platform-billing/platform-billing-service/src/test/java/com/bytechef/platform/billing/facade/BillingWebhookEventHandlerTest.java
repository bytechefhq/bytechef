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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.billing.client.StripeClient;
import com.bytechef.platform.billing.config.BillingProperties;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import com.bytechef.platform.billing.service.BillingWebhookEventService;
import com.stripe.model.Event;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.SubscriptionItemCollection;
import com.stripe.net.Webhook;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
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
    private static final String PRODUCT_STARTER_ID = "prod_starter_test";
    private static final String PRODUCT_GROWTH_ID = "prod_growth_test";
    private static final String PRODUCT_USAGE_ID = "prod_usage_test";
    private static final String SUBSCRIPTION_ID = "sub_test123";
    private static final String CHECKOUT_SUBSCRIPTION_ID = "sub_checkout_test";

    @Mock
    private BillingSubscriptionService billingSubscriptionService;

    @Mock
    private BillingWebhookEventService billingWebhookEventService;

    @Mock
    private StripeClient stripeClient;

    private BillingWebhookEventHandler handler;

    @BeforeEach
    void setUp() {
        BillingProperties billingProperties = new BillingProperties(
            new BillingProperties.Stripe(
                "sk_test_abc123", null, PRODUCT_STARTER_ID, PRODUCT_GROWTH_ID, PRODUCT_USAGE_ID, null,
                WEBHOOK_SECRET, null, null));

        handler = new BillingWebhookEventHandler(
            billingProperties, billingSubscriptionService, billingWebhookEventService, stripeClient);
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
    void testHandleSubscriptionUpdatedResetsLastReportedAtOnPeriodRollover() throws Exception {
        BillingSubscription subscription = starterSubscription();

        subscription.setSubscriptionId(SUBSCRIPTION_ID);
        subscription.setCurrentPeriodStart(Instant.ofEpochSecond(1777593600L));
        subscription.setLastReportedAt(Instant.parse("2026-05-15T10:00:00Z"));

        when(billingWebhookEventService.isEventProcessed(any())).thenReturn(false);
        when(billingSubscriptionService.fetchSubscriptionBySubscriptionId(SUBSCRIPTION_ID))
            .thenReturn(Optional.of(subscription));
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stripeClient.retrievePrice(any())).thenReturn(priceWithProductUnitLimit(100L));

        handler.handle(buildEvent(subscriptionUpdatedPayloadWithPeriodRollover(
            SUBSCRIPTION_ID, 1780272000L, 1782864000L)));

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());
        assertThat(captor.getValue()
            .getLastReportedAt()).isNull();
        assertThat(captor.getValue()
            .getCurrentPeriodStart()).isEqualTo(Instant.ofEpochSecond(1780272000L));
        assertThat(captor.getValue()
            .getCurrentPeriodEnd()).isEqualTo(Instant.ofEpochSecond(1782864000L));
    }

    @Test
    void testHandleSubscriptionUpdatedDoesNotUpdatePlanName() throws Exception {
        when(billingWebhookEventService.isEventProcessed(any())).thenReturn(false);
        when(billingSubscriptionService.fetchSubscriptionBySubscriptionId(SUBSCRIPTION_ID))
            .thenReturn(Optional.of(starterSubscription()));
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        handler.handle(buildEvent(subscriptionUpdatedPayload(SUBSCRIPTION_ID, "GROWTH")));

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());
        assertThat(captor.getValue()
            .getPlanName()).isEqualTo("Starter");
    }

    @Test
    void testHandleSubscriptionDeletedSetsStatusToCanceled() throws Exception {
        BillingSubscription subscription = starterSubscription();

        subscription.setSubscriptionId(SUBSCRIPTION_ID);

        when(billingWebhookEventService.isEventProcessed(any())).thenReturn(false);
        when(billingSubscriptionService.fetchSubscriptionBySubscriptionId(SUBSCRIPTION_ID))
            .thenReturn(Optional.of(subscription));
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        handler.handle(buildEvent(subscriptionDeletedPayload(SUBSCRIPTION_ID)));

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());
        assertThat(captor.getValue()
            .getStatus()).isEqualTo(BillingSubscription.Status.CANCELED);
    }

    @Test
    void testHandleSubscriptionUpdatedSetsStarterPlanName() throws Exception {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setPlanName("GROWTH");
        subscription.setStatus(BillingSubscription.Status.ACTIVE);
        subscription.setSubscriptionId(SUBSCRIPTION_ID);

        when(billingWebhookEventService.isEventProcessed(any())).thenReturn(false);
        when(billingSubscriptionService.fetchSubscriptionBySubscriptionId(SUBSCRIPTION_ID))
            .thenReturn(Optional.of(subscription));
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stripeClient.retrievePrice(any())).thenReturn(priceWithProductUnitLimit(100L));

        handler.handle(buildEvent(subscriptionUpdatedPayloadWithProduct(SUBSCRIPTION_ID, PRODUCT_STARTER_ID)));

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());
        assertThat(captor.getValue()
            .getPlanName()).isEqualTo("STARTER");
    }

    @Test
    void testHandleSubscriptionUpdatedSetsGrowthPlanName() throws Exception {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setPlanName("STARTER");
        subscription.setStatus(BillingSubscription.Status.ACTIVE);
        subscription.setSubscriptionId(SUBSCRIPTION_ID);

        when(billingWebhookEventService.isEventProcessed(any())).thenReturn(false);
        when(billingSubscriptionService.fetchSubscriptionBySubscriptionId(SUBSCRIPTION_ID))
            .thenReturn(Optional.of(subscription));
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stripeClient.retrievePrice(any())).thenReturn(priceWithProductUnitLimit(100L));

        handler.handle(buildEvent(subscriptionUpdatedPayloadWithProduct(SUBSCRIPTION_ID, PRODUCT_GROWTH_ID)));

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());
        assertThat(captor.getValue()
            .getPlanName()).isEqualTo("GROWTH");
    }

    @Test
    void testHandleSubscriptionUpdatedUpdatesCancelAtPeriodEnd() throws Exception {
        BillingSubscription subscription = starterSubscription();

        subscription.setSubscriptionId(SUBSCRIPTION_ID);

        when(billingWebhookEventService.isEventProcessed(any())).thenReturn(false);
        when(billingSubscriptionService.fetchSubscriptionBySubscriptionId(SUBSCRIPTION_ID))
            .thenReturn(Optional.of(subscription));
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        handler.handle(buildEvent(subscriptionUpdatedPayloadCancelAtPeriodEnd(SUBSCRIPTION_ID)));

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());
        assertThat(captor.getValue()
            .isCancelAtPeriodEnd()).isTrue();
    }

    @Test
    void testHandleCheckoutSessionCompletedCancelsExistingTrialSubscription() throws Exception {
        BillingSubscription trialSubscription = new BillingSubscription();

        trialSubscription.setPlanName("TRIAL");
        trialSubscription.setStatus(BillingSubscription.Status.ACTIVE);

        when(billingWebhookEventService.isEventProcessed(any())).thenReturn(false);
        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(trialSubscription));
        when(stripeClient.retrieveSubscription(CHECKOUT_SUBSCRIPTION_ID)).thenReturn(checkoutStripeSubscription());
        when(stripeClient.retrievePrice(any())).thenReturn(priceWithProductUnitLimit(100L));
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        handler
            .handle(buildEvent(checkoutSessionCompletedPayload(CHECKOUT_SUBSCRIPTION_ID, "cus_checkout", "STARTER")));

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService, times(2)).save(captor.capture());

        BillingSubscription canceledTrial = captor.getAllValues()
            .get(0);

        assertThat(canceledTrial).isSameAs(trialSubscription);
        assertThat(canceledTrial.getStatus()).isEqualTo(BillingSubscription.Status.CANCELED);

        BillingSubscription newSubscription = captor.getAllValues()
            .get(1);

        assertThat(newSubscription.getPlanName()).isEqualTo("STARTER");
    }

    @Test
    void testHandleCheckoutSessionCompletedDoesNotCancelNonTrialSubscription() throws Exception {
        BillingSubscription existingSubscription = starterSubscription();

        when(billingWebhookEventService.isEventProcessed(any())).thenReturn(false);
        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(existingSubscription));
        when(stripeClient.retrieveSubscription(CHECKOUT_SUBSCRIPTION_ID)).thenReturn(checkoutStripeSubscription());
        when(stripeClient.retrievePrice(any())).thenReturn(priceWithProductUnitLimit(100L));
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        handler
            .handle(buildEvent(checkoutSessionCompletedPayload(CHECKOUT_SUBSCRIPTION_ID, "cus_checkout", "STARTER")));

        verify(billingSubscriptionService, times(1)).save(any());
        assertThat(existingSubscription.getStatus()).isEqualTo(BillingSubscription.Status.ACTIVE);
    }

    @Test
    void testHandleCheckoutSessionCompletedSkipsAlreadyCanceledTrialSubscription() throws Exception {
        BillingSubscription canceledTrial = new BillingSubscription();

        canceledTrial.setPlanName("TRIAL");
        canceledTrial.setStatus(BillingSubscription.Status.CANCELED);

        when(billingWebhookEventService.isEventProcessed(any())).thenReturn(false);
        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(canceledTrial));
        when(stripeClient.retrieveSubscription(CHECKOUT_SUBSCRIPTION_ID)).thenReturn(checkoutStripeSubscription());
        when(stripeClient.retrievePrice(any())).thenReturn(priceWithProductUnitLimit(100L));
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        handler
            .handle(buildEvent(checkoutSessionCompletedPayload(CHECKOUT_SUBSCRIPTION_ID, "cus_checkout", "STARTER")));

        verify(billingSubscriptionService, times(1)).save(any());
    }

    private BillingSubscription starterSubscription() {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setPlanName("Starter");
        subscription.setStatus(BillingSubscription.Status.ACTIVE);

        return subscription;
    }

    private Subscription checkoutStripeSubscription() {
        Price.Recurring flatRecurring = new Price.Recurring();

        flatRecurring.setUsageType("licensed");

        Price flatPrice = new Price();

        flatPrice.setId("price_flat_checkout");
        flatPrice.setProduct(PRODUCT_STARTER_ID);
        flatPrice.setRecurring(flatRecurring);

        SubscriptionItem flatItem = new SubscriptionItem();

        flatItem.setId("si_flat_checkout");
        flatItem.setPrice(flatPrice);
        flatItem.setCurrentPeriodStart(1780272000L);
        flatItem.setCurrentPeriodEnd(1782864000L);

        Price.Recurring usageRecurring = new Price.Recurring();

        usageRecurring.setUsageType("metered");

        Price usagePrice = new Price();

        usagePrice.setId("price_usage_checkout");
        usagePrice.setRecurring(usageRecurring);

        SubscriptionItem usageItem = new SubscriptionItem();

        usageItem.setId("si_usage_checkout");
        usageItem.setPrice(usagePrice);

        SubscriptionItemCollection items = new SubscriptionItemCollection();

        items.setData(List.of(flatItem, usageItem));

        Subscription subscription = new Subscription();

        subscription.setId(CHECKOUT_SUBSCRIPTION_ID);
        subscription.setStatus("active");
        subscription.setCancelAtPeriodEnd(false);
        subscription.setItems(items);

        return subscription;
    }

    private Price priceWithProductUnitLimit(long upTo) {
        Price.Tier tier = new Price.Tier();

        tier.setUpTo(upTo);

        Price price = new Price();

        price.setTiers(List.of(tier));

        return price;
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private String subscriptionUpdatedPayloadWithPeriodRollover(
        String subscriptionId, long periodStartEpoch, long periodEndEpoch) {

        return """
            {
              "id": "evt_sub_rollover",
              "object": "event",
              "api_version": "2026-04-22.dahlia",
              "type": "customer.subscription.updated",
              "data": {
                "object": {
                  "id": "%s",
                  "object": "subscription",
                  "status": "active",
                  "metadata": { "planName": "Starter", "tenantId": "public" },
                  "items": {
                    "object": "list",
                    "has_more": false,
                    "url": "/v1/subscription_items",
                    "data": [
                      {
                        "id": "si_flat_item",
                        "object": "subscription_item",
                        "current_period_start": %d,
                        "current_period_end": %d,
                        "price": {
                          "id": "price_flat",
                          "object": "price",
                          "product": "prod_unknown",
                          "recurring": {
                            "interval": "month",
                            "usage_type": "licensed"
                          }
                        }
                      }
                    ]
                  }
                }
              }
            }
            """.formatted(subscriptionId, periodStartEpoch, periodEndEpoch);
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
    private String subscriptionDeletedPayload(String subscriptionId) {
        return """
            {
              "id": "evt_sub_deleted",
              "object": "event",
              "api_version": "2026-04-22.dahlia",
              "type": "customer.subscription.deleted",
              "data": {
                "object": {
                  "id": "%s",
                  "object": "subscription",
                  "status": "canceled",
                  "metadata": { "tenantId": "public" },
                  "items": {
                    "object": "list",
                    "data": [],
                    "has_more": false,
                    "url": "/v1/subscription_items"
                  }
                }
              }
            }
            """.formatted(subscriptionId);
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

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private String subscriptionUpdatedPayloadWithProduct(String subscriptionId, String productId) {
        return """
            {
              "id": "evt_sub_updated_product",
              "object": "event",
              "api_version": "2026-04-22.dahlia",
              "type": "customer.subscription.updated",
              "data": {
                "object": {
                  "id": "%s",
                  "object": "subscription",
                  "status": "active",
                  "cancel_at_period_end": false,
                  "metadata": { "tenantId": "public" },
                  "items": {
                    "object": "list",
                    "has_more": false,
                    "url": "/v1/subscription_items",
                    "data": [
                      {
                        "id": "si_flat_item",
                        "object": "subscription_item",
                        "current_period_start": 1780272000,
                        "current_period_end": 1782864000,
                        "price": {
                          "id": "price_flat",
                          "object": "price",
                          "product": "%s",
                          "recurring": {
                            "interval": "month",
                            "usage_type": "licensed"
                          }
                        }
                      }
                    ]
                  }
                }
              }
            }
            """.formatted(subscriptionId, productId);
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private String subscriptionUpdatedPayloadCancelAtPeriodEnd(String subscriptionId) {
        return """
            {
              "id": "evt_sub_cancel_at_period_end",
              "object": "event",
              "api_version": "2026-04-22.dahlia",
              "type": "customer.subscription.updated",
              "data": {
                "object": {
                  "id": "%s",
                  "object": "subscription",
                  "status": "active",
                  "cancel_at_period_end": true,
                  "metadata": { "tenantId": "public" },
                  "items": {
                    "object": "list",
                    "data": [],
                    "has_more": false,
                    "url": "/v1/subscription_items"
                  }
                }
              }
            }
            """.formatted(subscriptionId);
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
