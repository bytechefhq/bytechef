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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.billing.client.StripeClientImpl;
import com.bytechef.platform.billing.config.BillingProperties;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.domain.BillingSubscriptionWebhookEvent;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import com.bytechef.platform.billing.service.BillingUsageService;
import com.bytechef.platform.billing.service.BillingWebhookEventService;
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
import tools.jackson.databind.ObjectMapper;

/**
 * @author Matija Petanjek
 */
@ExtendWith(MockitoExtension.class)
class BillingSubscriptionFacadeImplTest {

    private static final String WEBHOOK_SECRET = "whsec_test_webhook_secret";
    private static final String STRIPE_API_KEY = "sk_test_abc123";
    private static final String STRIPE_SUBSCRIPTION_ID = "sub_test123";
    private static final String PRODUCT_STARTER_ID = "prod_starter_test";
    private static final String PRODUCT_GROWTH_ID = "prod_growth_test";
    private static final String PRODUCT_USAGE_ID = "prod_usage_test";

    @Mock
    private BillingSubscriptionService billingSubscriptionService;

    @Mock
    private BillingUsageService billingUsageService;

    @Mock
    private BillingWebhookEventService billingWebhookEventService;

    private BillingSubscriptionFacadeImpl facade;

    @BeforeEach
    void setUp() {
        BillingProperties billingProperties = new BillingProperties(
            new BillingProperties.Stripe(
                STRIPE_API_KEY, null, PRODUCT_STARTER_ID, PRODUCT_GROWTH_ID, PRODUCT_USAGE_ID, null, WEBHOOK_SECRET,
                null, null));

        facade = new BillingSubscriptionFacadeImpl(
            billingProperties, billingSubscriptionService, billingUsageService, billingWebhookEventService,
            new ObjectMapper(), new StripeClientImpl(billingProperties));
    }

    @Test
    void testFetchCurrentSubscriptionReturnsDtoWithTasksUsed() {
        BillingSubscription subscription = starterSubscription();

        Instant periodStart = Instant.parse("2026-06-01T00:00:00Z");

        subscription.setCurrentPeriodStart(periodStart);

        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(subscription));
        when(billingUsageService.countTaskExecutionsSince(eq(periodStart), any(Instant.class))).thenReturn(42);

        Optional<com.bytechef.platform.billing.dto.BillingSubscriptionDTO> result =
            facade.fetchCurrentSubscription();

        assertThat(result).isPresent();
        assertThat(result.get()
            .tasksUsed()).isEqualTo(42);
        assertThat(result.get()
            .subscription()).isSameAs(subscription);
    }

    @Test
    void testHandleSubscriptionUpdatedResetsLastReportedAtOnPeriodRollover() throws Exception {
        BillingSubscription subscription = starterSubscription();

        subscription.setStripeSubscriptionId(STRIPE_SUBSCRIPTION_ID);
        subscription.setCurrentPeriodStart(Instant.ofEpochSecond(1777593600L));
        subscription.setLastReportedAt(Instant.parse("2026-05-15T10:00:00Z"));

        when(billingWebhookEventService.isEventProcessed(any())).thenReturn(false);
        when(billingSubscriptionService.fetchSubscriptionByStripeSubscriptionId(STRIPE_SUBSCRIPTION_ID))
            .thenReturn(Optional.of(subscription));
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String payload = subscriptionUpdatedPayloadWithPeriodRollover(
            STRIPE_SUBSCRIPTION_ID, 1780272000L, 1782864000L);

        facade.handleWebhookEvent(payload, signPayload(payload, WEBHOOK_SECRET));

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
        when(billingSubscriptionService.fetchSubscriptionByStripeSubscriptionId(STRIPE_SUBSCRIPTION_ID))
            .thenReturn(Optional.of(starterSubscription()));
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String payload = subscriptionUpdatedPayload(STRIPE_SUBSCRIPTION_ID, "GROWTH");
        facade.handleWebhookEvent(payload, signPayload(payload, WEBHOOK_SECRET));

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());
        assertThat(captor.getValue()
            .getPlanName()).isEqualTo("Starter");
    }

    @Test
    void testHandleWebhookEventSkipsDuplicateEvent() throws Exception {
        when(billingWebhookEventService.isEventProcessed(any())).thenReturn(true);

        String payload = subscriptionUpdatedPayload(STRIPE_SUBSCRIPTION_ID, "STARTER");

        facade.handleWebhookEvent(payload, signPayload(payload, WEBHOOK_SECRET));

        verify(billingSubscriptionService, never()).fetchSubscriptionByStripeSubscriptionId(any());
        verify(billingSubscriptionService, never()).save(any());
        verify(billingWebhookEventService, never()).save(any());
    }

    @Test
    void testHandleSubscriptionDeletedSetsStatusToCanceled() throws Exception {
        BillingSubscription subscription = starterSubscription();

        subscription.setStripeSubscriptionId(STRIPE_SUBSCRIPTION_ID);

        when(billingWebhookEventService.isEventProcessed(any())).thenReturn(false);
        when(billingSubscriptionService.fetchSubscriptionByStripeSubscriptionId(STRIPE_SUBSCRIPTION_ID))
            .thenReturn(Optional.of(subscription));
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String payload = subscriptionDeletedPayload(STRIPE_SUBSCRIPTION_ID);

        facade.handleWebhookEvent(payload, signPayload(payload, WEBHOOK_SECRET));

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());
        assertThat(captor.getValue()
            .getStatus()).isEqualTo(BillingSubscription.Status.CANCELED);
    }

    @Test
    void testHandleUnknownEventTypeSavesWebhookEventWithoutSubscriptionLink() throws Exception {
        when(billingWebhookEventService.isEventProcessed(any())).thenReturn(false);

        String payload = unknownEventPayload();

        facade.handleWebhookEvent(payload, signPayload(payload, WEBHOOK_SECRET));

        verify(billingSubscriptionService, never()).fetchSubscriptionByStripeSubscriptionId(any());
        verify(billingSubscriptionService, never()).save(any());

        ArgumentCaptor<BillingSubscriptionWebhookEvent> captor =
            ArgumentCaptor.forClass(BillingSubscriptionWebhookEvent.class);

        verify(billingWebhookEventService).save(captor.capture());
        assertThat(captor.getValue()
            .getSubscriptionId()).isNull();
    }

    @Test
    void testHandleSubscriptionUpdatedSetsStarterPlanName() throws Exception {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setPlanName("GROWTH");
        subscription.setStatus(BillingSubscription.Status.ACTIVE);
        subscription.setStripeSubscriptionId(STRIPE_SUBSCRIPTION_ID);

        when(billingWebhookEventService.isEventProcessed(any())).thenReturn(false);
        when(billingSubscriptionService.fetchSubscriptionByStripeSubscriptionId(STRIPE_SUBSCRIPTION_ID))
            .thenReturn(Optional.of(subscription));
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String payload = subscriptionUpdatedPayloadWithProduct(STRIPE_SUBSCRIPTION_ID, PRODUCT_STARTER_ID);

        facade.handleWebhookEvent(payload, signPayload(payload, WEBHOOK_SECRET));

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
        subscription.setStripeSubscriptionId(STRIPE_SUBSCRIPTION_ID);

        when(billingWebhookEventService.isEventProcessed(any())).thenReturn(false);
        when(billingSubscriptionService.fetchSubscriptionByStripeSubscriptionId(STRIPE_SUBSCRIPTION_ID))
            .thenReturn(Optional.of(subscription));
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String payload = subscriptionUpdatedPayloadWithProduct(STRIPE_SUBSCRIPTION_ID, PRODUCT_GROWTH_ID);

        facade.handleWebhookEvent(payload, signPayload(payload, WEBHOOK_SECRET));

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());
        assertThat(captor.getValue()
            .getPlanName()).isEqualTo("GROWTH");
    }

    @Test
    void testHandleSubscriptionUpdatedClearsScheduledPlanNameOnPlanChange() throws Exception {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setPlanName("GROWTH");
        subscription.setScheduledPlanName("STARTER");
        subscription.setStatus(BillingSubscription.Status.ACTIVE);
        subscription.setStripeSubscriptionId(STRIPE_SUBSCRIPTION_ID);

        when(billingWebhookEventService.isEventProcessed(any())).thenReturn(false);
        when(billingSubscriptionService.fetchSubscriptionByStripeSubscriptionId(STRIPE_SUBSCRIPTION_ID))
            .thenReturn(Optional.of(subscription));
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String payload = subscriptionUpdatedPayloadWithProduct(STRIPE_SUBSCRIPTION_ID, PRODUCT_STARTER_ID);

        facade.handleWebhookEvent(payload, signPayload(payload, WEBHOOK_SECRET));

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());
        assertThat(captor.getValue()
            .getScheduledPlanName()).isNull();
    }

    @Test
    void testHandleSubscriptionUpdatedUpdatesCancelAtPeriodEnd() throws Exception {
        BillingSubscription subscription = starterSubscription();

        subscription.setStripeSubscriptionId(STRIPE_SUBSCRIPTION_ID);

        when(billingWebhookEventService.isEventProcessed(any())).thenReturn(false);
        when(billingSubscriptionService.fetchSubscriptionByStripeSubscriptionId(STRIPE_SUBSCRIPTION_ID))
            .thenReturn(Optional.of(subscription));
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String payload = subscriptionUpdatedPayloadCancelAtPeriodEnd(STRIPE_SUBSCRIPTION_ID);

        facade.handleWebhookEvent(payload, signPayload(payload, WEBHOOK_SECRET));

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());
        assertThat(captor.getValue()
            .isCancelAtPeriodEnd()).isTrue();
    }

    private BillingSubscription starterSubscription() {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setPlanName("Starter");
        subscription.setStatus(BillingSubscription.Status.ACTIVE);

        return subscription;
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

    private String unknownEventPayload() {
        return """
            {
              "id": "evt_invoice_paid",
              "object": "event",
              "api_version": "2026-04-22.dahlia",
              "type": "invoice.payment_succeeded",
              "data": {
                "object": {
                  "id": "in_test123",
                  "object": "invoice"
                }
              }
            }
            """;
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
