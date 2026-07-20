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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.dto.TrialDTO;
import com.bytechef.platform.billing.event.TrialExpiredEvent;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Matija Petanjek
 */
@ExtendWith(MockitoExtension.class)
class TrialServiceImplTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private BillingSubscriptionService billingSubscriptionService;

    @Mock
    private BillingUsageService billingUsageService;

    @InjectMocks
    private TrialServiceImpl trialService;

    @Test
    void testValidateTrialThrowsWhenNoSubscriptionExists() {
        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trialService.validateTrial())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No active subscription found");
    }

    @Test
    void testValidateTrialWhenPlanIsNotTrial() {
        BillingSubscription subscription = trialSubscription(Instant.now()
            .plus(7, ChronoUnit.DAYS));

        subscription.setPlanName("STARTER");

        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(subscription));

        TrialDTO status = trialService.validateTrial();

        assertThat(status.expired()).isTrue();
        assertThat(status.daysRemaining()).isZero();
        assertThat(status.tasksUsed()).isZero();
        assertThat(status.taskLimit()).isZero();
        verify(billingSubscriptionService, never()).save(any());
    }

    @Test
    void testValidateTrialWhenTrialActiveAndNotExpired() {
        BillingSubscription subscription = trialSubscription(Instant.now()
            .plus(7, ChronoUnit.DAYS));

        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(subscription));
        when(billingUsageService.countTaskExecutionsSince(any(), any())).thenReturn(100);

        TrialDTO status = trialService.validateTrial();

        assertThat(status.expired()).isFalse();
        assertThat(status.tasksUsed()).isEqualTo(100);
        assertThat(status.taskLimit()).isEqualTo(5000);
        assertThat(status.daysRemaining()).isGreaterThan(0);
        verify(billingSubscriptionService, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void testValidateTrialWhenTimeExpiredExpiresSubscription() {
        BillingSubscription subscription = trialSubscription(Instant.now()
            .minus(1, ChronoUnit.DAYS));

        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(subscription));
        when(billingUsageService.countTaskExecutionsSince(any(), any())).thenReturn(100);

        TrialDTO status = trialService.validateTrial();

        assertThat(status.expired()).isTrue();
        assertThat(status.daysRemaining()).isZero();

        ArgumentCaptor<BillingSubscription> captor = ArgumentCaptor.forClass(BillingSubscription.class);

        verify(billingSubscriptionService).save(captor.capture());
        assertThat(captor.getValue()
            .getStatus()).isEqualTo(BillingSubscription.Status.CANCELED);
        verify(applicationEventPublisher).publishEvent(any(TrialExpiredEvent.class));
    }

    @Test
    void testValidateTrialWhenTaskLimitReachedExpiresSubscription() {
        BillingSubscription subscription = trialSubscription(Instant.now()
            .plus(7, ChronoUnit.DAYS));

        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(subscription));
        when(billingUsageService.countTaskExecutionsSince(any(), any())).thenReturn(5000);

        TrialDTO status = trialService.validateTrial();

        assertThat(status.expired()).isTrue();
        verify(billingSubscriptionService).save(any());
        verify(applicationEventPublisher).publishEvent(any(TrialExpiredEvent.class));
    }

    @Test
    void testValidateTrialDoesNotExpireAgainWhenAlreadyCanceled() {
        BillingSubscription subscription = trialSubscription(Instant.now()
            .minus(1, ChronoUnit.DAYS));

        subscription.setStatus(BillingSubscription.Status.CANCELED);

        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(subscription));
        when(billingUsageService.countTaskExecutionsSince(any(), any())).thenReturn(100);

        TrialDTO status = trialService.validateTrial();

        assertThat(status.expired()).isTrue();
        verify(billingSubscriptionService, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    private BillingSubscription trialSubscription(Instant trialEnd) {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setPlanName("TRIAL");
        subscription.setStatus(BillingSubscription.Status.ACTIVE);
        subscription.setTaskLimit(5000);
        subscription.setCurrentPeriodStart(Instant.now()
            .minus(7, ChronoUnit.DAYS));
        subscription.setCurrentPeriodEnd(trialEnd);

        return subscription;
    }
}
