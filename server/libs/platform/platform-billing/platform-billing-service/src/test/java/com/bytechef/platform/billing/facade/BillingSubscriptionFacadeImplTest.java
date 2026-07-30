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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.billing.client.StripeClient;
import com.bytechef.platform.billing.config.BillingProperties;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.dto.BillingSubscriptionDTO;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import com.bytechef.platform.billing.service.BillingUsageService;
import com.stripe.model.Subscription;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Matija Petanjek
 */
@ExtendWith(MockitoExtension.class)
class BillingSubscriptionFacadeImplTest {

    private static final String PRODUCT_STARTER_ID = "prod_starter_test";
    private static final String PRODUCT_GROWTH_ID = "prod_growth_test";
    private static final String PRODUCT_USAGE_ID = "prod_usage_test";

    @Mock
    private BillingSubscriptionService billingSubscriptionService;

    @Mock
    private BillingUsageService billingUsageService;

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
            billingProperties, billingSubscriptionService, billingUsageService, stripeClient);
    }

    @Test
    void testFetchCurrentSubscriptionReturnsDtoWithTasksUsed() {
        BillingSubscription subscription = starterSubscription();

        Instant periodStart = Instant.parse("2026-06-01T00:00:00Z");

        subscription.setCurrentPeriodStart(periodStart);

        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(subscription));
        when(billingUsageService.countTaskExecutionsSince(eq(periodStart), any(Instant.class))).thenReturn(42);
        when(stripeClient.fetchScheduledPlanName(any())).thenReturn(Optional.of("GROWTH"));

        Optional<BillingSubscriptionDTO> result = facade.fetchCurrentSubscription();

        assertThat(result).isPresent();
        assertThat(result.get()
            .tasksUsed()).isEqualTo(42);
        assertThat(result.get()
            .subscription()).isSameAs(subscription);
        assertThat(result.get()
            .scheduledPlanName()).isEqualTo("GROWTH");
    }

    @Test
    void testUpgradeSubscriptionCallsUpgradeNowForUpdatePath() {
        BillingSubscription currentSubscription = starterSubscription();

        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(currentSubscription));
        when(stripeClient.retrieveSubscription("sub_starter")).thenReturn(mockStripeSubscription);
        when(stripeClient.fetchProductDefaultPriceId(PRODUCT_GROWTH_ID)).thenReturn("price_growth");

        facade.updateSubscription("GROWTH");

        verify(stripeClient).upgradeSubscriptionNow(
            eq("sub_starter"), eq("si_flat_starter"), eq("price_growth"), eq("GROWTH"), any());
        verify(stripeClient, never()).scheduleDowngrade(any(), any(), any(), any(), any(), any(), any(), anyLong());
    }

    @Test
    void testUpdateSubscriptionSchedulesDowngrade() {
        BillingSubscription currentSubscription = growthSubscription();

        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(currentSubscription));
        when(stripeClient.retrieveSubscription("sub_growth")).thenReturn(mockStripeSubscription);
        when(stripeClient.fetchProductDefaultPriceId(PRODUCT_STARTER_ID)).thenReturn("price_starter");
        when(stripeClient.fetchProductDefaultPriceId(PRODUCT_USAGE_ID)).thenReturn("price_usage");

        facade.updateSubscription("STARTER");

        verify(stripeClient).scheduleDowngrade(
            eq("sub_growth"), eq("si_flat_growth"), eq("si_usage_growth"),
            eq("price_starter"), eq("price_usage"), eq("STARTER"), any(), anyLong());
        verify(stripeClient, never()).upgradeSubscriptionNow(any(), any(), any(), any(), any());
    }

    @Test
    void testUpgradeSubscriptionReleasesExistingScheduleBeforeUpgrading() {
        BillingSubscription currentSubscription = starterSubscription();

        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(currentSubscription));
        when(stripeClient.retrieveSubscription(any())).thenReturn(mockStripeSubscription);
        when(stripeClient.fetchProductDefaultPriceId(PRODUCT_GROWTH_ID)).thenReturn("price_growth");

        facade.updateSubscription("GROWTH");

        verify(stripeClient).releaseSubscriptionScheduleIfPresent(mockStripeSubscription);
        verify(stripeClient).upgradeSubscriptionNow(any(), any(), eq("price_growth"), eq("GROWTH"), any());
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
}
