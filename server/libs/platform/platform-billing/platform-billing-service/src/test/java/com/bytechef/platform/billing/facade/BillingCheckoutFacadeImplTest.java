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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.billing.client.StripeClientImpl;
import com.bytechef.platform.billing.config.BillingProperties;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import com.bytechef.platform.billing.service.BillingUsageService;
import com.bytechef.platform.billing.service.BillingWebhookEventService;
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
class BillingCheckoutFacadeImplTest {

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

    private BillingCheckoutFacadeImpl facade;

    @BeforeEach
    void setUp() {
        BillingProperties billingProperties = new BillingProperties(
            STRIPE_API_KEY, null, PRODUCT_STARTER_ID, PRODUCT_GROWTH_ID, PRODUCT_USAGE_ID, null, WEBHOOK_SECRET,
            null, null);

        facade = new BillingCheckoutFacadeImpl(
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

    private BillingSubscription starterSubscription() {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setPlanName("Starter");
        subscription.setStatus(BillingSubscription.Status.ACTIVE);

        return subscription;
    }

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
