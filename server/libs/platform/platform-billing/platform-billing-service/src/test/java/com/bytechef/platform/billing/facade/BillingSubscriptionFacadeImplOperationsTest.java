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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.billing.client.StripeClient;
import com.bytechef.platform.billing.config.BillingProperties;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import com.bytechef.platform.billing.service.BillingUsageService;
import com.bytechef.platform.billing.service.BillingWebhookEventService;
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
import tools.jackson.databind.ObjectMapper;

/**
 * @author Matija Petanjek
 */
@ExtendWith(MockitoExtension.class)
class BillingSubscriptionFacadeImplOperationsTest {

    private static final String PRODUCT_STARTER_ID = "prod_starter_test";
    private static final String PRODUCT_GROWTH_ID = "prod_growth_test";
    private static final String PRODUCT_USAGE_ID = "prod_usage_test";
    private static final String WEBHOOK_SECRET = "whsec_test";
    private static final String CHECKOUT_SUBSCRIPTION_ID = "sub_checkout_test";

    @Mock
    private BillingSubscriptionService billingSubscriptionService;

    @Mock
    private BillingUsageService billingUsageService;

    @Mock
    private BillingWebhookEventService billingWebhookEventService;

    @Mock
    private StripeClient stripeClient;

    @Mock
    private Subscription mockStripeSubscription;

    private BillingSubscriptionFacadeImpl facade;

    @BeforeEach
    void setUp() {
        BillingProperties billingProperties = new BillingProperties(
            new BillingProperties.Stripe(
                "sk_test_abc", null, PRODUCT_STARTER_ID, PRODUCT_GROWTH_ID, PRODUCT_USAGE_ID, null, "whsec_test",
                null, null));

        facade = new BillingSubscriptionFacadeImpl(
            billingProperties, billingSubscriptionService, billingUsageService, billingWebhookEventService,
            new ObjectMapper(), stripeClient);
    }

    @Test
    void testHandleCheckoutSessionCompletedCancelsExistingTrialSubscription() throws Exception {
        BillingSubscription trialSubscription = new BillingSubscription();

        trialSubscription.setPlanName("TRIAL");
        trialSubscription.setStatus(BillingSubscription.Status.ACTIVE);

        when(billingWebhookEventService.isEventProcessed(any())).thenReturn(false);
        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(trialSubscription));
        when(stripeClient.verifyWebhookSignature(any(), any())).thenAnswer(
            invocation -> Webhook.constructEvent(invocation.getArgument(0), invocation.getArgument(1),
                WEBHOOK_SECRET));
        when(stripeClient.retrieveSubscription(CHECKOUT_SUBSCRIPTION_ID)).thenReturn(checkoutStripeSubscription());
        when(stripeClient.retrievePrice(any())).thenReturn(priceWithTaskLimit(100L));
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String payload = checkoutSessionCompletedPayload(CHECKOUT_SUBSCRIPTION_ID, "cus_checkout", "STARTER");

        facade.handleWebhookEvent(payload, signPayload(payload, WEBHOOK_SECRET));

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
        when(stripeClient.verifyWebhookSignature(any(), any())).thenAnswer(
            invocation -> Webhook.constructEvent(invocation.getArgument(0), invocation.getArgument(1),
                WEBHOOK_SECRET));
        when(stripeClient.retrieveSubscription(CHECKOUT_SUBSCRIPTION_ID)).thenReturn(checkoutStripeSubscription());
        when(stripeClient.retrievePrice(any())).thenReturn(priceWithTaskLimit(100L));
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String payload = checkoutSessionCompletedPayload(CHECKOUT_SUBSCRIPTION_ID, "cus_checkout", "STARTER");

        facade.handleWebhookEvent(payload, signPayload(payload, WEBHOOK_SECRET));

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
        when(stripeClient.verifyWebhookSignature(any(), any())).thenAnswer(
            invocation -> Webhook.constructEvent(invocation.getArgument(0), invocation.getArgument(1),
                WEBHOOK_SECRET));
        when(stripeClient.retrieveSubscription(CHECKOUT_SUBSCRIPTION_ID)).thenReturn(checkoutStripeSubscription());
        when(stripeClient.retrievePrice(any())).thenReturn(priceWithTaskLimit(100L));
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String payload = checkoutSessionCompletedPayload(CHECKOUT_SUBSCRIPTION_ID, "cus_checkout", "STARTER");

        facade.handleWebhookEvent(payload, signPayload(payload, WEBHOOK_SECRET));

        verify(billingSubscriptionService, times(1)).save(any());
    }

    @Test
    void testUpgradeSubscriptionCallsUpgradeNowForUpdatePath() {
        BillingSubscription currentSubscription = starterSubscription();

        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(currentSubscription));
        when(stripeClient.retrieveSubscription("sub_starter")).thenReturn(mockStripeSubscription);
        when(stripeClient.fetchProductDefaultPriceId(PRODUCT_GROWTH_ID)).thenReturn("price_growth");
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        facade.updateSubscription("GROWTH");

        verify(stripeClient).upgradeSubscriptionNow(
            eq("sub_starter"), eq("si_flat_starter"), eq("price_growth"), eq("GROWTH"), any());
        verify(stripeClient, never()).scheduleDowngrade(any(), any(), any(), any(), any(), any(), any(), anyLong());
    }

    @Test
    void testUpdateSubscriptionSchedulesDowngradeAndSetsPlanName() {
        BillingSubscription currentSubscription = growthSubscription();

        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(currentSubscription));
        when(stripeClient.retrieveSubscription("sub_growth")).thenReturn(mockStripeSubscription);
        when(stripeClient.fetchProductDefaultPriceId(PRODUCT_STARTER_ID)).thenReturn("price_starter");
        when(stripeClient.fetchProductDefaultPriceId(PRODUCT_USAGE_ID)).thenReturn("price_usage");
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        facade.updateSubscription("STARTER");

        verify(stripeClient).scheduleDowngrade(
            eq("sub_growth"), eq("si_flat_growth"), eq("si_usage_growth"),
            eq("price_starter"), eq("price_usage"), eq("STARTER"), any(), anyLong());

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());
        assertThat(captor.getValue()
            .getScheduledPlanName()).isEqualTo("STARTER");
        verify(stripeClient, never()).upgradeSubscriptionNow(any(), any(), any(), any(), any());
    }

    @Test
    void testUpgradeSubscriptionClearsScheduledPlanNameOnUpdate() {
        BillingSubscription currentSubscription = starterSubscription();

        currentSubscription.setScheduledPlanName("GROWTH");

        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(currentSubscription));
        when(stripeClient.retrieveSubscription(any())).thenReturn(mockStripeSubscription);
        when(stripeClient.fetchProductDefaultPriceId(PRODUCT_GROWTH_ID)).thenReturn("price_growth");
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        facade.updateSubscription("GROWTH");

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());
        assertThat(captor.getValue()
            .getScheduledPlanName()).isNull();
    }

    @Test
    void testCancelSubscriptionThrowsWhenNoActiveSubscription() {
        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facade.cancelSubscription())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No active subscription found");
    }

    @Test
    void testReactivateSubscriptionThrowsWhenNoActiveSubscription() {
        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facade.reactivateSubscription())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No active subscription found");
    }

    @Test
    void testUpdateSubscriptionThrowsForUnknownPlanName() {
        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(starterSubscription()));
        when(stripeClient.retrieveSubscription(any())).thenReturn(mockStripeSubscription);

        assertThatThrownBy(() -> facade.updateSubscription("ENTERPRISE"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown plan: ENTERPRISE");
    }

    private BillingSubscription starterSubscription() {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setStripeSubscriptionId("sub_starter");
        subscription.setStripeProductId("si_flat_starter");
        subscription.setStripeUsageProductId("si_usage_starter");
        subscription.setPlanName("STARTER");
        subscription.setStatus(BillingSubscription.Status.ACTIVE);
        subscription.setCurrentPeriodEnd(Instant.parse("2026-07-01T00:00:00Z"));

        return subscription;
    }

    private BillingSubscription growthSubscription() {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setStripeSubscriptionId("sub_growth");
        subscription.setStripeProductId("si_flat_growth");
        subscription.setStripeUsageProductId("si_usage_growth");
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

    private Price priceWithTaskLimit(long upTo) {
        Price.Tier tier = new Price.Tier();

        tier.setUpTo(upTo);

        Price price = new Price();

        price.setTiers(List.of(tier));

        return price;
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
