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
import static org.mockito.Mockito.when;

import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.repository.BillingSubscriptionRepository;
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
class BillingSubscriptionServiceImplTest {

    @Mock
    private BillingSubscriptionRepository billingSubscriptionRepository;

    private BillingSubscriptionServiceImpl billingSubscriptionService;

    @BeforeEach
    void setUp() {
        billingSubscriptionService = new BillingSubscriptionServiceImpl(billingSubscriptionRepository);
    }

    @Test
    void testFetchCurrentSubscriptionReturnsEmptyWhenSubscriptionIsCanceled() {
        BillingSubscription canceledSubscription = new BillingSubscription();

        canceledSubscription.setStatus(BillingSubscription.Status.CANCELED);

        when(billingSubscriptionRepository.findFirstByOrderByCreatedDateDesc())
            .thenReturn(Optional.of(canceledSubscription));

        Optional<BillingSubscription> result = billingSubscriptionService.fetchCurrentSubscription();

        assertThat(result).isEmpty();
    }

    @Test
    void testFetchCurrentSubscriptionReturnsActiveSubscription() {
        BillingSubscription activeSubscription = new BillingSubscription();

        activeSubscription.setStatus(BillingSubscription.Status.ACTIVE);

        when(billingSubscriptionRepository.findFirstByOrderByCreatedDateDesc())
            .thenReturn(Optional.of(activeSubscription));

        Optional<BillingSubscription> result = billingSubscriptionService.fetchCurrentSubscription();

        assertThat(result).isPresent();
        assertThat(result.get()
            .getStatus()).isEqualTo(BillingSubscription.Status.ACTIVE);
    }

    @Test
    void testFetchExistingStripeCustomerIdReturnsIdFromLatestSubscription() {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setStripeCustomerId("cus_existing_123");
        subscription.setStatus(BillingSubscription.Status.ACTIVE);

        when(billingSubscriptionRepository.findFirstByOrderByCreatedDateDesc())
            .thenReturn(Optional.of(subscription));

        Optional<String> result = billingSubscriptionService.fetchExistingStripeCustomerId();

        assertThat(result).contains("cus_existing_123");
    }
}
