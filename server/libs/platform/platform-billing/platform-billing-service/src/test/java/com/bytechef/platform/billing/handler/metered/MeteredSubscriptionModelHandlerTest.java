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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.billing.client.StripeClient;
import com.bytechef.platform.billing.config.BillingProperties;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.dto.BillingSubscriptionDTO;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import com.bytechef.platform.billing.service.BillingUsageService;
import com.stripe.model.Event;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.SubscriptionItemCollection;
import com.stripe.model.checkout.Session;
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
class MeteredSubscriptionModelHandlerTest {

    private static final String PRODUCT_STARTER_ID = "prod_starter_test";
    private static final String PRODUCT_GROWTH_ID = "prod_growth_test";
    private static final String PRODUCT_USAGE_ID = "prod_usage_test";
    private static final String WEBHOOK_SECRET = "whsec_test_webhook_secret";
    private static final String SUBSCRIPTION_ID = "sub_test123";
    private static final String CHECKOUT_SUBSCRIPTION_ID = "sub_checkout_test";
    private static final String USAGE_ITEM_JSON = """
        {
          "id": "si_usage_item",
          "object": "subscription_item",
          "current_period_start": 1780272000,
          "current_period_end": 1782864000,
          "price": {
            "id": "price_usage",
            "object": "price",
            "product": "prod_usage",
            "recurring": {
              "interval": "month",
              "usage_type": "metered"
            }
          }
        }
        """;

    @Mock
    private BillingSubscriptionService billingSubscriptionService;

    @Mock
    private BillingUsageService billingUsageService;

    @Mock
    private StripeClient stripeClient;

    @Mock
    private Subscription mockStripeSubscription;

    private MeteredSubscriptionModelHandler handler;

    @BeforeEach
    void setUp() {
        BillingProperties billingProperties = new BillingProperties(
            new BillingProperties.Stripe(
                "sk_test_abc", null, PRODUCT_STARTER_ID, PRODUCT_GROWTH_ID, PRODUCT_USAGE_ID, null, WEBHOOK_SECRET,
                null, null, null, null));

        handler = new MeteredSubscriptionModelHandler(
            billingProperties, billingSubscriptionService, billingUsageService, stripeClient);
    }

    @Test
    void testToBillingSubscriptionDTOReturnsDtoWithJobsExecuted() {
        BillingSubscription subscription = starterSubscription();

        Instant periodStart = Instant.parse("2026-06-01T00:00:00Z");

        subscription.setCurrentPeriodStart(periodStart);

        when(billingUsageService.countJobExecutionsSince(eq(periodStart), any(Instant.class))).thenReturn(42);
        when(stripeClient.fetchScheduledPlanName(any())).thenReturn(Optional.of("GROWTH"));

        BillingSubscriptionDTO result = handler.toBillingSubscriptionDTO(subscription);

        assertThat(result.jobsExecuted()).isEqualTo(42);
        assertThat(result.subscription()).isSameAs(subscription);
        assertThat(result.scheduledPlanName()).isEqualTo("GROWTH");
    }

    @Test
    void testUpgradeSubscriptionCallsUpgradeNowForUpdatePath() {
        BillingSubscription currentSubscription = starterSubscription();

        when(stripeClient.retrieveSubscription("sub_starter")).thenReturn(mockStripeSubscription);
        when(stripeClient.fetchProductDefaultPriceId(PRODUCT_GROWTH_ID)).thenReturn("price_growth");

        handler.updateSubscription(currentSubscription, "GROWTH");

        verify(stripeClient).upgradeSubscriptionNow(
            eq("sub_starter"), eq("si_flat_starter"), eq("price_growth"), eq("GROWTH"), any());
        verify(stripeClient, never()).scheduleDowngrade(any(), any(), any(), any(), any(), any(), any(), anyLong());
    }

    @Test
    void testUpdateSubscriptionSchedulesDowngrade() {
        BillingSubscription currentSubscription = growthSubscription();

        when(stripeClient.retrieveSubscription("sub_growth")).thenReturn(mockStripeSubscription);
        when(stripeClient.fetchProductDefaultPriceId(PRODUCT_STARTER_ID)).thenReturn("price_starter");
        when(stripeClient.fetchProductDefaultPriceId(PRODUCT_USAGE_ID)).thenReturn("price_usage");

        handler.updateSubscription(currentSubscription, "STARTER");

        verify(stripeClient).scheduleDowngrade(
            eq("sub_growth"), eq("si_flat_growth"), eq("si_usage_growth"),
            eq("price_starter"), eq("price_usage"), eq("STARTER"), any(), anyLong());
        verify(stripeClient, never()).upgradeSubscriptionNow(any(), any(), any(), any(), any());
    }

    @Test
    void testUpgradeSubscriptionReleasesExistingScheduleBeforeUpgrading() {
        BillingSubscription currentSubscription = starterSubscription();

        when(stripeClient.retrieveSubscription(any())).thenReturn(mockStripeSubscription);
        when(stripeClient.fetchProductDefaultPriceId(PRODUCT_GROWTH_ID)).thenReturn("price_growth");

        handler.updateSubscription(currentSubscription, "GROWTH");

        verify(stripeClient).releaseSubscriptionScheduleIfPresent(mockStripeSubscription);
        verify(stripeClient).upgradeSubscriptionNow(any(), any(), eq("price_growth"), eq("GROWTH"), any());
    }

    @Test
    void testUpdateSubscriptionThrowsForUnknownPlanName() {
        when(stripeClient.retrieveSubscription(any())).thenReturn(mockStripeSubscription);

        assertThatThrownBy(() -> handler.updateSubscription(starterSubscription(), "ENTERPRISE"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown plan: ENTERPRISE");
    }

    @Test
    void testHandleSubscriptionUpdatedResetsLastReportedAtOnPeriodRollover() throws Exception {
        BillingSubscription subscription = titleCaseStarterSubscription();

        subscription.setSubscriptionId(SUBSCRIPTION_ID);
        subscription.setCurrentPeriodStart(Instant.ofEpochSecond(1777593600L));
        subscription.setLastReportedAt(Instant.parse("2026-05-15T10:00:00Z"));

        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stripeClient.retrievePrice("price_usage")).thenReturn(priceWithProductUnitLimit(500L));

        handler.handleSubscriptionUpdated(
            subscription,
            stripeSubscription(
                subscriptionUpdatedPayloadWithPeriodRollover(SUBSCRIPTION_ID, 1780272000L, 1782864000L)));

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());
        assertThat(captor.getValue()
            .getProductUnitLimit()).isEqualTo(500);
        assertThat(captor.getValue()
            .getLastReportedAt()).isNull();
        assertThat(captor.getValue()
            .getCurrentPeriodStart()).isEqualTo(Instant.ofEpochSecond(1780272000L));
        assertThat(captor.getValue()
            .getCurrentPeriodEnd()).isEqualTo(Instant.ofEpochSecond(1782864000L));
    }

    @Test
    void testHandleSubscriptionUpdatedDoesNotUpdatePlanName() throws Exception {
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        handler.handleSubscriptionUpdated(
            titleCaseStarterSubscription(), stripeSubscription(subscriptionUpdatedPayload(SUBSCRIPTION_ID, "GROWTH")));

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());
        assertThat(captor.getValue()
            .getPlanName()).isEqualTo("Starter");
    }

    @Test
    void testHandleSubscriptionUpdatedThrowsWhenMeteredItemMissing() throws Exception {
        BillingSubscription subscription = titleCaseStarterSubscription();
        Subscription stripeSubscription =
            stripeSubscription(subscriptionUpdatedPayloadWithoutUsageItem(SUBSCRIPTION_ID));

        assertThatThrownBy(() -> handler.handleSubscriptionUpdated(subscription, stripeSubscription))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Subscription missing usage product");

        verify(billingSubscriptionService, never()).save(any());
    }

    @Test
    void testHandleSubscriptionDeletedSetsStatusToCanceled() throws Exception {
        BillingSubscription subscription = titleCaseStarterSubscription();

        subscription.setSubscriptionId(SUBSCRIPTION_ID);

        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        handler.handleSubscriptionDeleted(subscription);

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

        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stripeClient.retrievePrice("price_usage")).thenReturn(priceWithProductUnitLimit(100L));

        handler.handleSubscriptionUpdated(
            subscription,
            stripeSubscription(subscriptionUpdatedPayloadWithProduct(SUBSCRIPTION_ID, PRODUCT_STARTER_ID)));

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

        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stripeClient.retrievePrice("price_usage")).thenReturn(priceWithProductUnitLimit(100L));

        handler.handleSubscriptionUpdated(
            subscription,
            stripeSubscription(subscriptionUpdatedPayloadWithProduct(SUBSCRIPTION_ID, PRODUCT_GROWTH_ID)));

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());
        assertThat(captor.getValue()
            .getPlanName()).isEqualTo("GROWTH");
    }

    @Test
    void testHandleSubscriptionUpdatedUpdatesCancelAtPeriodEnd() throws Exception {
        BillingSubscription subscription = titleCaseStarterSubscription();

        subscription.setSubscriptionId(SUBSCRIPTION_ID);

        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        handler.handleSubscriptionUpdated(
            subscription, stripeSubscription(subscriptionUpdatedPayloadCancelAtPeriodEnd(SUBSCRIPTION_ID)));

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());
        assertThat(captor.getValue()
            .isCancelAtPeriodEnd()).isTrue();
    }

    @Test
    void testHandleCheckoutSessionCompletedCreatesMeteredSubscription() throws Exception {
        when(stripeClient.retrieveSubscription(CHECKOUT_SUBSCRIPTION_ID)).thenReturn(checkoutStripeSubscription());
        when(stripeClient.retrievePrice(any())).thenReturn(priceWithProductUnitLimit(100L));
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        handler.handleCheckoutSessionCompleted(
            checkoutSession(checkoutSessionCompletedPayload(CHECKOUT_SUBSCRIPTION_ID, "cus_checkout", "STARTER")));

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());

        BillingSubscription newSubscription = captor.getValue();

        assertThat(newSubscription.getPlanName()).isEqualTo("STARTER");
        assertThat(newSubscription.getSubscriptionModel()).isEqualTo(BillingSubscription.SubscriptionModel.METERED);
    }

    private Subscription stripeSubscription(String payload) throws Exception {
        return (Subscription) buildEvent(payload).getDataObjectDeserializer()
            .getObject()
            .orElseThrow();
    }

    private Session checkoutSession(String payload) throws Exception {
        return (Session) buildEvent(payload).getDataObjectDeserializer()
            .getObject()
            .orElseThrow();
    }

    private BillingSubscription titleCaseStarterSubscription() {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setPlanName("Starter");
        subscription.setStatus(BillingSubscription.Status.ACTIVE);

        return subscription;
    }

    private BillingSubscription starterSubscription() {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setSubscriptionId("sub_starter");
        subscription.setProductId("si_flat_starter");
        subscription.setUsageProductId("si_usage_starter");
        subscription.setPlanName("STARTER");
        subscription.setStatus(BillingSubscription.Status.ACTIVE);
        subscription.setCurrentPeriodEnd(Instant.parse("2026-07-01T00:00:00Z"));

        return subscription;
    }

    private BillingSubscription growthSubscription() {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setSubscriptionId("sub_growth");
        subscription.setProductId("si_flat_growth");
        subscription.setUsageProductId("si_usage_growth");
        subscription.setPlanName("GROWTH");
        subscription.setStatus(BillingSubscription.Status.ACTIVE);
        subscription.setCurrentPeriodEnd(Instant.parse("2026-07-01T00:00:00Z"));

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
                      },
                      %s
                    ]
                  }
                }
              }
            }
            """.formatted(subscriptionId, periodStartEpoch, periodEndEpoch, USAGE_ITEM_JSON);
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
                  "items": { "object": "list", "data": [%s], "has_more": false, "url": "/v1/subscription_items" }
                }
              }
            }
            """.formatted(subscriptionId, planName, USAGE_ITEM_JSON);
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private String subscriptionUpdatedPayloadWithoutUsageItem(String subscriptionId) {
        return """
            {
              "id": "evt_sub_updated_without_usage",
              "object": "event",
              "api_version": "2026-04-22.dahlia",
              "type": "customer.subscription.updated",
              "data": {
                "object": {
                  "id": "%s",
                  "object": "subscription",
                  "status": "active",
                  "metadata": { "tenantId": "public" },
                  "items": { "object": "list", "data": [], "has_more": false, "url": "/v1/subscription_items" }
                }
              }
            }
            """.formatted(subscriptionId);
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
                      },
                      %s
                    ]
                  }
                }
              }
            }
            """.formatted(subscriptionId, productId, USAGE_ITEM_JSON);
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
                    "data": [%s],
                    "has_more": false,
                    "url": "/v1/subscription_items"
                  }
                }
              }
            }
            """.formatted(subscriptionId, USAGE_ITEM_JSON);
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
