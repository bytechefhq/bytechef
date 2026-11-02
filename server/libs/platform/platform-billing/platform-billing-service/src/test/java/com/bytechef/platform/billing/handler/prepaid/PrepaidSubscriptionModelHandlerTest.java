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

package com.bytechef.platform.billing.handler.prepaid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.billing.client.StripeClient;
import com.bytechef.platform.billing.config.BillingProperties;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.domain.BillingSubscription.SubscriptionModel;
import com.bytechef.platform.billing.repository.BillingSubscriptionRepository;
import com.bytechef.platform.billing.repository.PrepaidBillingSubscriptionRepository;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import com.bytechef.platform.billing.service.BillingUsageService;
import com.bytechef.tenant.TenantContext;
import com.stripe.model.Event;
import com.stripe.model.LineItem;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Matija Petanjek
 */
@ExtendWith(MockitoExtension.class)
class PrepaidSubscriptionModelHandlerTest {

    private static final String TENANT_ID = "tenant_1";
    private static final String STARTER_PRODUCT_ID = "prod_starter";
    private static final String GROWTH_PRODUCT_ID = "prod_growth";
    private static final String SUCCESS_URL = "https://app/success";
    private static final String CANCEL_URL = "https://app/cancel";
    private static final String PACKAGE_PRODUCT_ID = "prod_package";
    private static final String PACKAGE_SUCCESS_URL = "https://app/package-success";
    private static final String WEBHOOK_SECRET = "whsec_test_webhook_secret";
    private static final String CHECKOUT_SUBSCRIPTION_ID = "sub_checkout_test";
    private static final long PERIOD_START = 1780272000L;
    private static final long PERIOD_END = 1782864000L;
    private static final long NEXT_PERIOD_START = 1782864000L;
    private static final long NEXT_PERIOD_END = 1785542400L;
    private static final long SUBSCRIPTION_ROW_ID = 42L;

    @Mock
    private BillingSubscriptionRepository billingSubscriptionRepository;

    @Mock
    private BillingSubscriptionService billingSubscriptionService;

    @Mock
    private BillingUsageService billingUsageService;

    @Mock
    private PrepaidBillingSubscriptionRepository prepaidBillingSubscriptionRepository;

    @Mock
    private PrepaidStripeClient prepaidStripeClient;

    @Mock
    private StripeClient stripeClient;

    private PrepaidSubscriptionModelHandler handler;

    @BeforeEach
    void setUp() {
        BillingProperties billingProperties = new BillingProperties(
            new BillingProperties.Stripe(
                "sk_test", null, STARTER_PRODUCT_ID, GROWTH_PRODUCT_ID, "prod_usage", null, "whsec", SUCCESS_URL,
                CANCEL_URL, PACKAGE_PRODUCT_ID, PACKAGE_SUCCESS_URL));

        handler = new PrepaidSubscriptionModelHandler(
            billingProperties, billingSubscriptionRepository, billingSubscriptionService, billingUsageService,
            prepaidBillingSubscriptionRepository, prepaidStripeClient, stripeClient);

        TenantContext.setCurrentTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testCreateCheckoutSessionUsesOnlyFlatPrice() {
        SecurityContextHolder.getContext()
            .setAuthentication(new TestingAuthenticationToken("user@test.com", "pw"));

        Session session = new Session();

        session.setUrl("https://checkout");

        when(stripeClient.fetchProductDefaultPriceId(STARTER_PRODUCT_ID)).thenReturn("price_starter");
        when(stripeClient.fetchCustomerId("user@test.com", TENANT_ID)).thenReturn(Optional.of("cus_1"));
        when(prepaidStripeClient.createSubscriptionCheckoutSession(
            "cus_1", "price_starter", "STARTER", SUCCESS_URL, CANCEL_URL, TENANT_ID)).thenReturn(session);

        assertThat(handler.createCheckoutSession("STARTER")).isEqualTo("https://checkout");

        verify(prepaidStripeClient).createSubscriptionCheckoutSession(
            "cus_1", "price_starter", "STARTER", SUCCESS_URL, CANCEL_URL, TENANT_ID);
        verify(stripeClient, never()).createCheckoutSession(
            any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testUpgradeSwapsFlatItemNow() {
        BillingSubscription subscription = subscription("STARTER");

        when(stripeClient.fetchProductDefaultPriceId(GROWTH_PRODUCT_ID)).thenReturn("price_growth");

        handler.updateSubscription(subscription, "GROWTH");

        verify(stripeClient).upgradeSubscriptionNow("sub_1", "si_flat", "price_growth", "GROWTH", TENANT_ID);
    }

    @Test
    void testDowngradeSchedulesFlatOnlyPhase() {
        BillingSubscription subscription = subscription("GROWTH");

        when(stripeClient.fetchProductDefaultPriceId(STARTER_PRODUCT_ID)).thenReturn("price_starter");

        handler.updateSubscription(subscription, "STARTER");

        verify(prepaidStripeClient).scheduleDowngrade(
            "sub_1", "price_starter", "STARTER", TENANT_ID, subscription.getCurrentPeriodEnd()
                .getEpochSecond());
    }

    @Test
    void testCancelAndReactivateUseSharedClient() {
        BillingSubscription subscription = subscription("STARTER");

        handler.cancelSubscription(subscription);
        handler.reactivateSubscription(subscription);

        verify(stripeClient).cancelAtPeriodEnd("sub_1", TENANT_ID);
        verify(stripeClient).reactivateSubscription("sub_1", TENANT_ID);
    }

    @Test
    void testCheckoutCompletedCreatesPrepaidRowWithPlanUnitTotal() throws Exception {
        when(billingSubscriptionRepository.findFirstByOrderByCreatedDateDesc()).thenReturn(Optional.empty());
        when(stripeClient.retrieveSubscription(CHECKOUT_SUBSCRIPTION_ID)).thenReturn(
            prepaidStripeSubscription(CHECKOUT_SUBSCRIPTION_ID, STARTER_PRODUCT_ID, "5000", PERIOD_START, PERIOD_END));
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> assignId(invocation.getArgument(0)));

        handler.handleCheckoutSessionCompleted(checkoutSession());

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());

        BillingSubscription newSubscription = captor.getValue();

        assertThat(newSubscription.getSubscriptionModel()).isEqualTo(SubscriptionModel.PREPAID);
        assertThat(newSubscription.getPlanUnitTotal()).isEqualTo(5000);
        assertThat(newSubscription.getUsageProductId()).isNull();
        assertThat(newSubscription.getPlanName()).isEqualTo("STARTER");
        assertThat(newSubscription.getSubscriptionId()).isEqualTo(CHECKOUT_SUBSCRIPTION_ID);
        assertThat(newSubscription.getCustomerId()).isEqualTo("cus_checkout");
        assertThat(newSubscription.getProductId()).isEqualTo("si_flat_item");
        assertThat(newSubscription.getStatus()).isEqualTo(BillingSubscription.Status.ACTIVE);
        assertThat(newSubscription.getCurrentPeriodStart()).isEqualTo(Instant.ofEpochSecond(PERIOD_START));
        assertThat(newSubscription.getCurrentPeriodEnd()).isEqualTo(Instant.ofEpochSecond(PERIOD_END));

        verify(prepaidBillingSubscriptionRepository, never()).addExtraUnitTotal(anyLong(), anyInt());
    }

    @Test
    void testCheckoutCompletedTransfersUnusedExtraUnits() throws Exception {
        BillingSubscription previousSubscription = previousSubscription(3000, 1200);

        when(billingSubscriptionRepository.findFirstByOrderByCreatedDateDesc())
            .thenReturn(Optional.of(previousSubscription));
        when(stripeClient.retrieveSubscription(CHECKOUT_SUBSCRIPTION_ID)).thenReturn(
            prepaidStripeSubscription(CHECKOUT_SUBSCRIPTION_ID, STARTER_PRODUCT_ID, "5000", PERIOD_START, PERIOD_END));
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> assignId(invocation.getArgument(0)));

        handler.handleCheckoutSessionCompleted(checkoutSession());

        InOrder inOrder = inOrder(
            billingSubscriptionRepository, billingSubscriptionService, prepaidBillingSubscriptionRepository);

        inOrder.verify(billingSubscriptionRepository)
            .findFirstByOrderByCreatedDateDesc();
        inOrder.verify(billingSubscriptionService)
            .save(any());
        inOrder.verify(prepaidBillingSubscriptionRepository)
            .addExtraUnitTotal(SUBSCRIPTION_ROW_ID, 1800);
    }

    @Test
    void testCheckoutCompletedTransfersNothingWhenPreviousHasNoBalance() throws Exception {
        when(stripeClient.retrieveSubscription(CHECKOUT_SUBSCRIPTION_ID)).thenAnswer(
            invocation -> prepaidStripeSubscription(
                CHECKOUT_SUBSCRIPTION_ID, STARTER_PRODUCT_ID, "5000", PERIOD_START, PERIOD_END));
        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> assignId(invocation.getArgument(0)));

        when(billingSubscriptionRepository.findFirstByOrderByCreatedDateDesc())
            .thenReturn(Optional.of(previousSubscription(1000, 1000)));

        handler.handleCheckoutSessionCompleted(checkoutSession());

        when(billingSubscriptionRepository.findFirstByOrderByCreatedDateDesc())
            .thenReturn(Optional.of(previousSubscription(1000, 1500)));

        handler.handleCheckoutSessionCompleted(checkoutSession());

        verify(prepaidBillingSubscriptionRepository, never()).addExtraUnitTotal(anyLong(), anyInt());
    }

    @Test
    void testSubscriptionUpdatedPeriodChangeResetsUnitUsed() throws Exception {
        BillingSubscription subscription = prepaidSubscription("STARTER", 1000);

        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        handler.handleSubscriptionUpdated(
            subscription,
            prepaidStripeSubscription(
                "sub_1", STARTER_PRODUCT_ID, "5000", NEXT_PERIOD_START, NEXT_PERIOD_END));

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());

        BillingSubscription savedSubscription = captor.getValue();

        assertThat(savedSubscription.getPlanUnitTotal()).isEqualTo(5000);
        assertThat(savedSubscription.getPlanName()).isEqualTo("STARTER");
        assertThat(savedSubscription.getCurrentPeriodStart()).isEqualTo(Instant.ofEpochSecond(NEXT_PERIOD_START));
        assertThat(savedSubscription.getCurrentPeriodEnd()).isEqualTo(Instant.ofEpochSecond(NEXT_PERIOD_END));

        verify(prepaidBillingSubscriptionRepository).resetUnitUsedForNewPeriod(SUBSCRIPTION_ROW_ID);
    }

    @Test
    void testSubscriptionUpdatedIgnoresStaleOlderPeriodEvent() throws Exception {
        BillingSubscription subscription = prepaidSubscription("GROWTH", 15000);

        subscription.setCancelAtPeriodEnd(true);

        BillingSubscription result = handler.handleSubscriptionUpdated(
            subscription,
            prepaidStripeSubscription(
                "sub_1", STARTER_PRODUCT_ID, "5000", PERIOD_START - 2678400L, PERIOD_START));

        assertThat(result).isSameAs(subscription);
        assertThat(subscription.getPlanName()).isEqualTo("GROWTH");
        assertThat(subscription.getPlanUnitTotal()).isEqualTo(15000);
        assertThat(subscription.isCancelAtPeriodEnd()).isTrue();
        assertThat(subscription.getProductId()).isEqualTo("si_flat");
        assertThat(subscription.getCurrentPeriodStart()).isEqualTo(Instant.ofEpochSecond(PERIOD_START));
        assertThat(subscription.getCurrentPeriodEnd()).isEqualTo(Instant.ofEpochSecond(PERIOD_END));

        verify(billingSubscriptionService, never()).save(any());
        verify(prepaidBillingSubscriptionRepository, never()).resetUnitUsedForNewPeriod(anyLong());
    }

    @Test
    void testSubscriptionUpdatedIgnoresNonPlanItems() throws Exception {
        BillingSubscription subscription = prepaidSubscription("STARTER", 5000);

        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String payload = subscriptionPayload("sub_1", STARTER_PRODUCT_ID, "5000", PERIOD_START, PERIOD_END)
            .replace(
                "\"data\": [",
                "\"data\": [{\"id\": \"si_metered_item\", \"object\": \"subscription_item\","
                    + " \"current_period_start\": 1, \"current_period_end\": 2, \"price\": {\"id\": \"price_metered\","
                    + " \"object\": \"price\", \"product\": \"prod_usage\"}},");

        Subscription stripeSubscription = (Subscription) buildEvent(payload).getDataObjectDeserializer()
            .getObject()
            .orElseThrow();

        assertThat(stripeSubscription.getItems()
            .getData()).hasSize(2);

        handler.handleSubscriptionUpdated(subscription, stripeSubscription);

        assertThat(subscription.getProductId()).isEqualTo("si_flat_item");
        assertThat(subscription.getPlanName()).isEqualTo("STARTER");
    }

    @Test
    void testSubscriptionUpdatedPlanChangeKeepsPlanUnitUsed() throws Exception {
        BillingSubscription subscription = prepaidSubscription("STARTER", 5000);

        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        handler.handleSubscriptionUpdated(
            subscription,
            prepaidStripeSubscription("sub_1", GROWTH_PRODUCT_ID, "15000", PERIOD_START, PERIOD_END));

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());

        BillingSubscription savedSubscription = captor.getValue();

        assertThat(savedSubscription.getPlanName()).isEqualTo("GROWTH");
        assertThat(savedSubscription.getPlanUnitTotal()).isEqualTo(15000);
        assertThat(savedSubscription.getCurrentPeriodStart()).isEqualTo(Instant.ofEpochSecond(PERIOD_START));

        verify(prepaidBillingSubscriptionRepository, never()).resetUnitUsedForNewPeriod(anyLong());
    }

    @Test
    void testSubscriptionUpdatedWithoutChangesKeepsUnits() throws Exception {
        BillingSubscription subscription = prepaidSubscription("STARTER", 5000);

        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        handler.handleSubscriptionUpdated(
            subscription,
            prepaidStripeSubscription("sub_1", STARTER_PRODUCT_ID, "9999", PERIOD_START, PERIOD_END));

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());

        assertThat(captor.getValue()
            .getPlanUnitTotal()).isEqualTo(5000);

        verify(prepaidBillingSubscriptionRepository, never()).resetUnitUsedForNewPeriod(anyLong());
    }

    @Test
    void testSubscriptionUpdatedSetsCancelAtPeriodEnd() throws Exception {
        BillingSubscription subscription = prepaidSubscription("STARTER", 5000);
        Subscription stripeSubscription = prepaidStripeSubscription(
            "sub_1", STARTER_PRODUCT_ID, "5000", PERIOD_START, PERIOD_END);

        stripeSubscription.setCancelAtPeriodEnd(true);

        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        handler.handleSubscriptionUpdated(subscription, stripeSubscription);

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());

        BillingSubscription savedSubscription = captor.getValue();

        assertThat(savedSubscription.isCancelAtPeriodEnd()).isTrue();
        assertThat(savedSubscription.getStatus()).isEqualTo(BillingSubscription.Status.ACTIVE);
        assertThat(savedSubscription.getPlanUnitTotal()).isEqualTo(5000);

        verify(prepaidBillingSubscriptionRepository, never()).resetUnitUsedForNewPeriod(anyLong());
    }

    @Test
    void testSubscriptionDeletedSetsStatusToCanceled() {
        BillingSubscription subscription = prepaidSubscription("STARTER", 5000);

        when(billingSubscriptionService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        handler.handleSubscriptionDeleted(subscription);

        assertThat(subscription.getStatus()).isEqualTo(BillingSubscription.Status.CANCELED);

        verify(billingSubscriptionService).save(subscription);
    }

    @Test
    void testCreateJobExecutionsPackageCheckoutSessionUsesPackagePrice() {
        SecurityContextHolder.getContext()
            .setAuthentication(new TestingAuthenticationToken("user@test.com", "pw"));

        Session session = new Session();

        session.setUrl("https://package-checkout");

        when(stripeClient.fetchProductDefaultPriceId(PACKAGE_PRODUCT_ID)).thenReturn("price_package");
        when(stripeClient.fetchCustomerId("user@test.com", TENANT_ID)).thenReturn(Optional.of("cus_1"));
        when(prepaidStripeClient.createJobExecutionsPackageCheckoutSession(
            "cus_1", "price_package", 3L, PACKAGE_SUCCESS_URL, CANCEL_URL, TENANT_ID)).thenReturn(session);

        assertThat(handler.createJobExecutionsPackageCheckoutSession(3)).isEqualTo("https://package-checkout");
    }

    @Test
    void testPackagePurchasedGrantsUnitTotalTimesQuantity() {
        BillingSubscription currentSubscription = assignId(prepaidSubscription("STARTER", 1000));

        when(billingSubscriptionRepository.findFirstByOrderByCreatedDateDesc())
            .thenReturn(Optional.of(currentSubscription));

        BillingSubscription result = handler.handleJobExecutionsPackagePurchased(
            packageSession("paid"), List.of(packageLineItem(PACKAGE_PRODUCT_ID, "1000", 3L)));

        verify(prepaidBillingSubscriptionRepository).addExtraUnitTotal(SUBSCRIPTION_ROW_ID, 3000);

        assertThat(result).isSameAs(currentSubscription);
    }

    @Test
    void testPackagePurchasedIgnoresNonPackageLineItems() {
        BillingSubscription currentSubscription = assignId(prepaidSubscription("STARTER", 1000));

        when(billingSubscriptionRepository.findFirstByOrderByCreatedDateDesc())
            .thenReturn(Optional.of(currentSubscription));

        handler.handleJobExecutionsPackagePurchased(
            packageSession("paid"),
            List.of(packageLineItem(PACKAGE_PRODUCT_ID, "500", 2L), packageLineItem("prod_other", "999", 5L),
                packageLineItem(PACKAGE_PRODUCT_ID, "500", 1L)));

        verify(prepaidBillingSubscriptionRepository).addExtraUnitTotal(SUBSCRIPTION_ROW_ID, 1500);
    }

    @Test
    void testPackagePurchasedGrantsToLatestRowEvenWhenCanceled() {
        BillingSubscription canceledSubscription = assignId(prepaidSubscription("STARTER", 5000));

        canceledSubscription.setStatus(BillingSubscription.Status.CANCELED);

        when(billingSubscriptionRepository.findFirstByOrderByCreatedDateDesc())
            .thenReturn(Optional.of(canceledSubscription));

        handler.handleJobExecutionsPackagePurchased(
            packageSession("paid"), List.of(packageLineItem(PACKAGE_PRODUCT_ID, "1000", 2L)));

        verify(prepaidBillingSubscriptionRepository).addExtraUnitTotal(SUBSCRIPTION_ROW_ID, 2000);
    }

    @Test
    void testPackagePurchasedUnpaidDoesNotLookUpRow() {
        BillingSubscription result = handler.handleJobExecutionsPackagePurchased(
            packageSession("unpaid"), List.of(packageLineItem(PACKAGE_PRODUCT_ID, "1000", 2L)));

        assertThat(result).isNull();

        verify(billingSubscriptionRepository, never()).findFirstByOrderByCreatedDateDesc();
        verify(prepaidBillingSubscriptionRepository, never()).addExtraUnitTotal(anyLong(), anyInt());
    }

    @Test
    void testPackagePurchasedWithoutCurrentRowThrows() {
        when(billingSubscriptionRepository.findFirstByOrderByCreatedDateDesc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handleJobExecutionsPackagePurchased(
            packageSession("paid"), List.of(packageLineItem(PACKAGE_PRODUCT_ID, "1000", 2L))))
                .isInstanceOf(IllegalStateException.class);

        verify(prepaidBillingSubscriptionRepository, never()).addExtraUnitTotal(anyLong(), anyInt());
    }

    private Session packageSession(String paymentStatus) {
        Session session = new Session();

        session.setId("cs_package");
        session.setPaymentStatus(paymentStatus);

        return session;
    }

    private LineItem packageLineItem(String productId, String unitTotal, long quantity) {
        Price price = new Price();

        price.setId("price_" + productId);
        price.setProduct(productId);
        price.setMetadata(Map.of("unit_total", unitTotal));

        LineItem lineItem = new LineItem();

        lineItem.setPrice(price);
        lineItem.setQuantity(quantity);

        return lineItem;
    }

    private BillingSubscription assignId(BillingSubscription subscription) {
        subscription.setId(SUBSCRIPTION_ROW_ID);

        return subscription;
    }

    private BillingSubscription previousSubscription(int extraUnitTotal, int extraUnitUsed) {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setId(7L);
        subscription.setPlanName("STARTER");
        subscription.setStatus(BillingSubscription.Status.CANCELED);
        subscription.setExtraUnitTotal(extraUnitTotal);
        subscription.setExtraUnitUsed(extraUnitUsed);

        return subscription;
    }

    private BillingSubscription prepaidSubscription(String planName, int planUnitTotal) {
        BillingSubscription subscription = subscription(planName);

        subscription.setId(SUBSCRIPTION_ROW_ID);
        subscription.setStatus(BillingSubscription.Status.ACTIVE);
        subscription.setSubscriptionModel(SubscriptionModel.PREPAID);
        subscription.setPlanUnitTotal(planUnitTotal);
        subscription.setCurrentPeriodStart(Instant.ofEpochSecond(PERIOD_START));
        subscription.setCurrentPeriodEnd(Instant.ofEpochSecond(PERIOD_END));

        return subscription;
    }

    private Session checkoutSession() throws Exception {
        return (Session) buildEvent(checkoutSessionCompletedPayload()).getDataObjectDeserializer()
            .getObject()
            .orElseThrow();
    }

    private Subscription prepaidStripeSubscription(
        String subscriptionId, String productId, String unitTotal, long periodStart, long periodEnd) throws Exception {

        return (Subscription) buildEvent(
            subscriptionPayload(subscriptionId, productId, unitTotal, periodStart, periodEnd))
                .getDataObjectDeserializer()
                .getObject()
                .orElseThrow();
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private String subscriptionPayload(
        String subscriptionId, String productId, String unitTotal, long periodStart, long periodEnd) {

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
                        "current_period_start": %d,
                        "current_period_end": %d,
                        "price": {
                          "id": "price_flat",
                          "object": "price",
                          "product": "%s",
                          "metadata": { "unit_total": "%s" },
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
            """.formatted(subscriptionId, periodStart, periodEnd, productId, unitTotal);
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private String checkoutSessionCompletedPayload() {
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
                  "customer": "cus_checkout",
                  "subscription": "%s",
                  "metadata": { "planName": "STARTER", "subscriptionModel": "PREPAID", "tenantId": "public" }
                }
              }
            }
            """.formatted(CHECKOUT_SUBSCRIPTION_ID);
    }

    private Event buildEvent(String payload) throws Exception {
        return Webhook.constructEvent(payload, signPayload(payload), WEBHOOK_SECRET);
    }

    private String signPayload(String payload) throws Exception {
        long timestamp = Instant.now()
            .getEpochSecond();
        String signedPayload = timestamp + "." + payload;

        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

        String signature = HexFormat.of()
            .formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));

        return "t=" + timestamp + ",v1=" + signature;
    }

    private BillingSubscription subscription(String planName) {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setSubscriptionId("sub_1");
        subscription.setProductId("si_flat");
        subscription.setPlanName(planName);
        subscription.setCurrentPeriodEnd(Instant.parse("2026-11-01T00:00:00Z"));

        return subscription;
    }
}
