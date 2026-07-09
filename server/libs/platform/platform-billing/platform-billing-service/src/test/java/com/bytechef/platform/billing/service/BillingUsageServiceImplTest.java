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

package com.bytechef.platform.billing.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.billing.client.StripeClient;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.repository.BillingSubscriptionRepository;
import java.time.Instant;
import java.util.Optional;

import com.bytechef.tenant.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Matija Petanjek
 */
@ExtendWith(MockitoExtension.class)
class BillingUsageServiceImplTest {

    @Mock
    private BillingSubscriptionRepository billingSubscriptionRepository;

    @Mock
    private BillingSubscriptionService billingSubscriptionService;

    @Mock
    private StripeClient stripeClient;

    @Mock
    private TenantService tenantService;

    private BillingUsageServiceImpl billingUsageService;

    @BeforeEach
    void setUp() {
        billingUsageService = new BillingUsageServiceImpl(
            billingSubscriptionRepository, billingSubscriptionService, stripeClient, tenantService);
    }

    @Test
    void reportUsageSkipsWhenNoSubscription() {
        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.empty());

        billingUsageService.reportUsage(Instant.now());

        verify(stripeClient, never()).reportMeterEvent(anyString(), anyInt(), anyString());
        verify(billingSubscriptionService, never()).save(any());
    }

    @Test
    void reportUsageSkipsStripeWhenCountIsZero() {
        BillingSubscription subscription = buildSubscription("cus_test", "sub_test", null);

        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(subscription));
        when(billingSubscriptionRepository.countCompletedTaskExecutions(any(), any())).thenReturn(0);

        Instant scheduledFireTime = Instant.now();

        billingUsageService.reportUsage(scheduledFireTime);

        verify(stripeClient, never()).reportMeterEvent(anyString(), anyInt(), anyString());
        verify(billingSubscriptionService).save(subscription);
    }

    @Test
    void reportUsageReportsCountAndUpdatesLastReportedAt() {
        Instant periodStart = Instant.parse("2026-06-01T00:00:00Z");
        BillingSubscription subscription = buildSubscription("cus_123", "sub_abc", null);

        subscription.setCurrentPeriodStart(periodStart);

        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(subscription));
        when(billingSubscriptionRepository.countCompletedTaskExecutions(any(), any())).thenReturn(42);

        Instant scheduledFireTime = Instant.parse("2026-06-01T01:00:00Z");

        billingUsageService.reportUsage(scheduledFireTime);

        verify(stripeClient).reportMeterEvent(
            eq("cus_123"), eq(42), eq("sub_abc_" + scheduledFireTime.getEpochSecond()));
        verify(billingSubscriptionService).save(subscription);
    }

    @Test
    void reportUsageUsesPeriodStartWhenLastReportedAtIsNull() {
        Instant periodStart = Instant.parse("2026-06-01T00:00:00Z");
        BillingSubscription subscription = buildSubscription("cus_123", "sub_abc", null);

        subscription.setCurrentPeriodStart(periodStart);

        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(subscription));
        when(billingSubscriptionRepository.countCompletedTaskExecutions(eq(periodStart), any())).thenReturn(10);

        billingUsageService.reportUsage(Instant.parse("2026-06-01T01:00:00Z"));

        verify(billingSubscriptionRepository).countCompletedTaskExecutions(eq(periodStart), any());
    }

    @Test
    void reportUsageUsesLastReportedAtWhenPresent() {
        Instant lastReportedAt = Instant.parse("2026-06-01T02:00:00Z");
        BillingSubscription subscription = buildSubscription("cus_123", "sub_abc", lastReportedAt);

        subscription.setCurrentPeriodStart(Instant.parse("2026-06-01T00:00:00Z"));

        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(subscription));
        when(billingSubscriptionRepository.countCompletedTaskExecutions(eq(lastReportedAt), any())).thenReturn(5);

        billingUsageService.reportUsage(Instant.parse("2026-06-01T03:00:00Z"));

        verify(billingSubscriptionRepository).countCompletedTaskExecutions(eq(lastReportedAt), any());
    }

    private BillingSubscription buildSubscription(String customerId, String subscriptionId, Instant lastReportedAt) {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setStripeCustomerId(customerId);
        subscription.setStripeSubscriptionId(subscriptionId);
        subscription.setStatus(BillingSubscription.Status.ACTIVE);
        subscription.setLastReportedAt(lastReportedAt);
        subscription.setCurrentPeriodStart(Instant.now()
            .minusSeconds(3600));

        return subscription;
    }
}
