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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bytechef.platform.billing.client.StripeClient;
import com.bytechef.platform.billing.config.BillingIntTestConfiguration;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.domain.BillingSubscriptionWebhookEvent;
import com.bytechef.platform.billing.exception.InvalidWebhookSignatureException;
import com.bytechef.platform.billing.repository.BillingSubscriptionRepository;
import com.bytechef.platform.billing.repository.BillingWebhookEventRepository;
import com.bytechef.platform.billing.service.BillingWebhookEventService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
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
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @author Matija Petanjek
 */
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
@SpringBootTest(classes = BillingIntTestConfiguration.class)
class BillingWebhookFacadeIntTest {

    private static final String WEBHOOK_SECRET = "whsec_test_webhook_secret";
    private static final String CHECKOUT_SUBSCRIPTION_ID = "sub_checkout_test";
    private static final String SUBSCRIPTION_ID = "sub_test123";
    private static final String PRODUCT_STARTER_ID = "prod_starter_test";
    private static final long PERIOD_START_EPOCH = 1780272000L;
    private static final long PERIOD_END_EPOCH = 1782864000L;

    @Autowired
    private BillingSubscriptionRepository billingSubscriptionRepository;

    @Autowired
    private BillingWebhookEventRepository billingWebhookEventRepository;

    @MockitoSpyBean
    private BillingWebhookEventService billingWebhookEventService;

    @Autowired
    private BillingWebhookFacade billingWebhookFacade;

    @MockitoBean
    private StripeClient stripeClient;

    @BeforeEach
    void beforeEach() {
        billingWebhookEventRepository.deleteAll();
        billingSubscriptionRepository.deleteAll();

        doAnswer(invocation -> Webhook.constructEvent(
            invocation.getArgument(0), invocation.getArgument(1), WEBHOOK_SECRET))
                .when(stripeClient)
                .verifyWebhookSignature(any(), any());
    }

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testProcessWebhookEventVerifiesSignatureExactlyOnce() throws Exception {
        String payload = checkoutSessionCompletedPayload("acme");

        stubCheckoutStripeCalls(new AtomicReference<>());

        billingWebhookFacade.processWebhookEvent(payload, signPayload(payload));

        verify(stripeClient, times(1)).verifyWebhookSignature(any(), any());
    }

    @Test
    void testProcessWebhookEventHandlesEventWithinResolvedTenantContext() throws Exception {
        String payload = checkoutSessionCompletedPayload("acme");

        AtomicReference<String> tenantIdDuringHandling = new AtomicReference<>();

        stubCheckoutStripeCalls(tenantIdDuringHandling);

        billingWebhookFacade.processWebhookEvent(payload, signPayload(payload));

        assertThat(tenantIdDuringHandling.get())
            .as("tenant must already be resolved in TenantContext before handle() is invoked, since handle() is "
                + "@Transactional and acquires its database connection as soon as it is entered")
            .isEqualTo("acme");

        BillingSubscription subscription = billingSubscriptionRepository.findAll()
            .stream()
            .filter(candidate -> CHECKOUT_SUBSCRIPTION_ID.equals(candidate.getSubscriptionId()))
            .findFirst()
            .orElseThrow();

        assertThat(subscription.getPlanName()).isEqualTo("STARTER");
        assertThat(subscription.getCustomerId()).isEqualTo("cus_checkout");
        assertThat(subscription.getProductUnitLimit()).isEqualTo(5000);

        List<BillingSubscriptionWebhookEvent> webhookEvents = billingWebhookEventRepository.findAll();

        assertThat(webhookEvents).hasSize(1);
        assertThat(webhookEvents.getFirst()
            .getEventType()).isEqualTo("checkout.session.completed");
        assertThat(webhookEvents.getFirst()
            .getSubscriptionId()).isEqualTo(subscription.getId());
    }

    @Test
    void testProcessWebhookEventResetsTenantContextAfterHandling() throws Exception {
        String payload = checkoutSessionCompletedPayload("acme");

        stubCheckoutStripeCalls(new AtomicReference<>());

        billingWebhookFacade.processWebhookEvent(payload, signPayload(payload));

        assertThat(TenantContext.getCurrentTenantId())
            .as("runWithTenantId must restore the previous tenant once handling completes")
            .isEqualTo(TenantContext.DEFAULT_TENANT_ID);
        assertThat(billingWebhookEventRepository.existsByEventId("evt_checkout_completed")).isTrue();
    }

    @Test
    void testProcessWebhookEventExtractsMetadataTenantIdForSubscriptionUpdated() throws Exception {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setSubscriptionId(SUBSCRIPTION_ID);
        subscription.setPlanName("STARTER");
        subscription.setStatus(BillingSubscription.Status.ACTIVE);
        subscription.setProductUnitLimit(100);
        subscription.setCurrentPeriodStart(Instant.parse("2026-05-01T00:00:00Z"));
        subscription.setCurrentPeriodEnd(Instant.parse("2026-06-01T00:00:00Z"));

        BillingSubscription savedSubscription = billingSubscriptionRepository.save(subscription);

        AtomicReference<String> tenantIdDuringHandling = new AtomicReference<>();

        doAnswer(invocation -> {
            tenantIdDuringHandling.set(TenantContext.getCurrentTenantId());

            return priceWithProductUnitLimit(5000L);
        }).when(stripeClient)
            .retrievePrice("price_usage");

        String payload = subscriptionUpdatedPayload(SUBSCRIPTION_ID, "public");

        billingWebhookFacade.processWebhookEvent(payload, signPayload(payload));

        assertThat(tenantIdDuringHandling.get()).isEqualTo("public");

        BillingSubscription updatedSubscription = billingSubscriptionRepository.findById(savedSubscription.getId())
            .orElseThrow();

        assertThat(updatedSubscription.getProductUnitLimit()).isEqualTo(5000);
        assertThat(updatedSubscription.getCurrentPeriodStart()).isEqualTo(Instant.ofEpochSecond(PERIOD_START_EPOCH));
        assertThat(billingWebhookEventRepository.existsByEventId("evt_sub_updated")).isTrue();
    }

    @Test
    void testProcessWebhookEventThrowsWhenTenantIdMetadataMissing() throws Exception {
        String payload = unknownEventPayload();
        String signature = signPayload(payload);

        assertThatThrownBy(() -> billingWebhookFacade.processWebhookEvent(payload, signature))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Missing tenantId metadata in invoice.payment_succeeded webhook event");

        assertThat(billingWebhookEventRepository.count()).isZero();
    }

    @Test
    void testProcessWebhookEventHandlesUnrecognizedEventTypeWhenTenantIdPresent() throws Exception {
        String payload = unknownEventPayloadWithTenantId("acme");

        AtomicReference<String> tenantIdDuringHandling = new AtomicReference<>();

        doAnswer(invocation -> {
            tenantIdDuringHandling.set(TenantContext.getCurrentTenantId());

            return invocation.callRealMethod();
        }).when(billingWebhookEventService)
            .isEventProcessed("evt_invoice_paid");

        billingWebhookFacade.processWebhookEvent(payload, signPayload(payload));

        assertThat(tenantIdDuringHandling.get()).isEqualTo("acme");

        List<BillingSubscriptionWebhookEvent> webhookEvents = billingWebhookEventRepository.findAll();

        assertThat(webhookEvents).hasSize(1);
        assertThat(webhookEvents.getFirst()
            .getEventType()).isEqualTo("invoice.payment_succeeded");
        assertThat(webhookEvents.getFirst()
            .getSubscriptionId()).isNull();
    }

    @Test
    void testProcessWebhookEventPropagatesInvalidSignature() {
        String payload = subscriptionUpdatedPayload(SUBSCRIPTION_ID, "public");

        doThrow(new InvalidWebhookSignatureException("Invalid Stripe signature", null))
            .when(stripeClient)
            .verifyWebhookSignature(eq(payload), eq("t=1,v1=invalid"));

        assertThatThrownBy(() -> billingWebhookFacade.processWebhookEvent(payload, "t=1,v1=invalid"))
            .isInstanceOf(InvalidWebhookSignatureException.class)
            .hasMessage("Invalid Stripe signature");

        assertThat(billingWebhookEventRepository.count()).isZero();

        verify(stripeClient, never()).retrievePrice(any());
    }

    private void stubCheckoutStripeCalls(AtomicReference<String> tenantIdDuringHandling) {
        doAnswer(invocation -> {
            tenantIdDuringHandling.set(TenantContext.getCurrentTenantId());

            return checkoutStripeSubscription();
        }).when(stripeClient)
            .retrieveSubscription(CHECKOUT_SUBSCRIPTION_ID);

        doAnswer(invocation -> priceWithProductUnitLimit(5000L))
            .when(stripeClient)
            .retrievePrice(any());
    }

    private static Subscription checkoutStripeSubscription() {
        Price.Recurring flatRecurring = new Price.Recurring();

        flatRecurring.setUsageType("licensed");

        Price flatPrice = new Price();

        flatPrice.setId("price_flat_checkout");
        flatPrice.setProduct(PRODUCT_STARTER_ID);
        flatPrice.setRecurring(flatRecurring);

        SubscriptionItem flatItem = new SubscriptionItem();

        flatItem.setId("si_flat_checkout");
        flatItem.setPrice(flatPrice);
        flatItem.setCurrentPeriodStart(PERIOD_START_EPOCH);
        flatItem.setCurrentPeriodEnd(PERIOD_END_EPOCH);

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

    private static Price priceWithProductUnitLimit(long upTo) {
        Price.Tier tier = new Price.Tier();

        tier.setUpTo(upTo);

        Price price = new Price();

        price.setTiers(List.of(tier));

        return price;
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private String checkoutSessionCompletedPayload(String tenantId) {
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
                  "client_reference_id": "%s",
                  "customer": "cus_checkout",
                  "subscription": "%s",
                  "metadata": { "planName": "STARTER", "tenantId": "%s" }
                }
              }
            }
            """.formatted(tenantId, CHECKOUT_SUBSCRIPTION_ID, tenantId);
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private String subscriptionUpdatedPayload(String subscriptionId, String tenantId) {
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
                  "metadata": { "planName": "STARTER", "tenantId": "%s" },
                  "items": {
                    "object": "list",
                    "data": [
                      {
                        "id": "si_flat",
                        "object": "subscription_item",
                        "current_period_start": %d,
                        "current_period_end": %d,
                        "price": {
                          "id": "price_flat",
                          "object": "price",
                          "product": "%s",
                          "recurring": { "usage_type": "licensed" }
                        }
                      },
                      {
                        "id": "si_usage",
                        "object": "subscription_item",
                        "price": {
                          "id": "price_usage",
                          "object": "price",
                          "recurring": { "usage_type": "metered" }
                        }
                      }
                    ],
                    "has_more": false,
                    "url": "/v1/subscription_items"
                  }
                }
              }
            }
            """.formatted(subscriptionId, tenantId, PERIOD_START_EPOCH, PERIOD_END_EPOCH, PRODUCT_STARTER_ID);
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
    private String unknownEventPayloadWithTenantId(String tenantId) {
        return """
            {
              "id": "evt_invoice_paid",
              "object": "event",
              "api_version": "2026-04-22.dahlia",
              "type": "invoice.payment_succeeded",
              "data": {
                "object": {
                  "id": "in_test123",
                  "object": "invoice",
                  "metadata": { "tenantId": "%s" }
                }
              }
            }
            """.formatted(tenantId);
    }

    private static String signPayload(String payload) throws Exception {
        long timestamp = Instant.now()
            .getEpochSecond();
        String signedPayload = timestamp + "." + payload;

        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

        String signature = HexFormat.of()
            .formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));

        return "t=" + timestamp + ",v1=" + signature;
    }
}
