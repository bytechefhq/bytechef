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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.domain.BillingSubscription.SubscriptionModel;
import com.bytechef.platform.billing.repository.PrepaidBillingSubscriptionRepository;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Matija Petanjek
 */
@ExtendWith(MockitoExtension.class)
class PrepaidUnitConsumptionServiceTest {

    private static final long JOB_ID = 42L;
    private static final long SUBSCRIPTION_ID = 7L;

    @Mock
    private BillingSubscriptionService billingSubscriptionService;

    @Mock
    private PrepaidBillingSubscriptionRepository prepaidBillingSubscriptionRepository;

    @InjectMocks
    private PrepaidUnitConsumptionService prepaidUnitConsumptionService;

    @Test
    void testNonDeployedJobIsNeverCounted() {
        when(prepaidBillingSubscriptionRepository.existsPrincipalJobByJobId(JOB_ID)).thenReturn(false);

        assertThat(prepaidUnitConsumptionService.tryStart(job(null))).isTrue();

        verify(prepaidBillingSubscriptionRepository).existsPrincipalJobByJobId(JOB_ID);

        verifyNoMoreInteractions(prepaidBillingSubscriptionRepository);

        verify(billingSubscriptionService, never()).fetchCurrentSubscription();
    }

    @Test
    void testSubflowJobIsNeverCounted() {
        assertThat(prepaidUnitConsumptionService.tryStart(job(3L))).isTrue();

        verifyNoMoreInteractions(prepaidBillingSubscriptionRepository, billingSubscriptionService);
    }

    @Test
    void testConsumesPlanUnitFirst() {
        deployedWithSubscription("GROWTH", SubscriptionModel.PREPAID);

        when(prepaidBillingSubscriptionRepository.incrementPlanUnitUsed(SUBSCRIPTION_ID)).thenReturn(1);

        assertThat(prepaidUnitConsumptionService.tryStart(job(null))).isTrue();

        verify(prepaidBillingSubscriptionRepository).incrementPlanUnitUsed(SUBSCRIPTION_ID);
        verify(prepaidBillingSubscriptionRepository, never()).incrementExtraUnitUsed(anyLong());
    }

    @Test
    void testFallsBackToExtraUnit() {
        deployedWithSubscription("GROWTH", SubscriptionModel.PREPAID);

        when(prepaidBillingSubscriptionRepository.incrementPlanUnitUsed(SUBSCRIPTION_ID)).thenReturn(0);
        when(prepaidBillingSubscriptionRepository.incrementExtraUnitUsed(SUBSCRIPTION_ID)).thenReturn(1);

        assertThat(prepaidUnitConsumptionService.tryStart(job(null))).isTrue();

        verify(prepaidBillingSubscriptionRepository).incrementExtraUnitUsed(SUBSCRIPTION_ID);
    }

    @Test
    void testBothPoolsExhaustedRefusesStart() {
        deployedWithSubscription("GROWTH", SubscriptionModel.PREPAID);

        when(prepaidBillingSubscriptionRepository.incrementPlanUnitUsed(SUBSCRIPTION_ID)).thenReturn(0);
        when(prepaidBillingSubscriptionRepository.incrementExtraUnitUsed(SUBSCRIPTION_ID)).thenReturn(0);

        assertThat(prepaidUnitConsumptionService.tryStart(job(null))).isFalse();

        verify(prepaidBillingSubscriptionRepository).incrementExtraUnitUsed(SUBSCRIPTION_ID);
    }

    @Test
    void testSkipsMeteredAndTrial() {
        deployedWithSubscription("GROWTH", SubscriptionModel.METERED);

        assertThat(prepaidUnitConsumptionService.tryStart(job(null))).isTrue();

        deployedWithSubscription("TRIAL", SubscriptionModel.PREPAID);

        assertThat(prepaidUnitConsumptionService.tryStart(job(null))).isTrue();

        verify(prepaidBillingSubscriptionRepository, never()).incrementPlanUnitUsed(anyLong());
        verify(prepaidBillingSubscriptionRepository, never()).incrementExtraUnitUsed(anyLong());
    }

    @Test
    void testNoCurrentSubscriptionCountsNothing() {
        when(prepaidBillingSubscriptionRepository.existsPrincipalJobByJobId(JOB_ID)).thenReturn(true);
        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.empty());

        assertThat(prepaidUnitConsumptionService.tryStart(job(null))).isTrue();

        verify(prepaidBillingSubscriptionRepository, never()).incrementPlanUnitUsed(anyLong());
    }

    private static Job job(Long parentTaskExecutionId) {
        Job job = new Job();

        job.setId(JOB_ID);
        job.setParentTaskExecutionId(parentTaskExecutionId);

        return job;
    }

    private void deployedWithSubscription(String planName, SubscriptionModel subscriptionModel) {
        BillingSubscription billingSubscription = new BillingSubscription();

        billingSubscription.setId(SUBSCRIPTION_ID);
        billingSubscription.setPlanName(planName);
        billingSubscription.setSubscriptionModel(subscriptionModel);

        when(prepaidBillingSubscriptionRepository.existsPrincipalJobByJobId(JOB_ID)).thenReturn(true);
        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(billingSubscription));
    }
}
