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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.billing.client.StripeClient;
import com.bytechef.platform.billing.config.BillingProperties;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import com.bytechef.platform.billing.service.BillingUsageService;
import com.bytechef.tenant.TenantContext;
import com.stripe.model.checkout.Session;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Mock
    private BillingSubscriptionService billingSubscriptionService;

    @Mock
    private BillingUsageService billingUsageService;

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
                CANCEL_URL, null, null));

        handler = new PrepaidSubscriptionModelHandler(
            billingProperties, billingSubscriptionService, billingUsageService, prepaidStripeClient, stripeClient);

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

    private BillingSubscription subscription(String planName) {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setSubscriptionId("sub_1");
        subscription.setProductId("si_flat");
        subscription.setPlanName(planName);
        subscription.setCurrentPeriodEnd(Instant.parse("2026-11-01T00:00:00Z"));

        return subscription;
    }
}
