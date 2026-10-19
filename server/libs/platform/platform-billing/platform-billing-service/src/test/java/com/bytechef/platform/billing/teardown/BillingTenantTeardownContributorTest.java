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

package com.bytechef.platform.billing.teardown;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.billing.client.StripeClient;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.exception.PaymentClientException;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import com.bytechef.tenant.service.TenantDeletionService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class BillingTenantTeardownContributorTest {

    private static final String TENANT_ID = "000001";

    @Mock
    private BillingSubscriptionService billingSubscriptionService;

    @Mock
    private ObjectProvider<TenantDeletionService> tenantDeletionServiceProvider;

    @Mock
    private StripeClient stripeClient;

    @Mock
    private TenantDeletionService tenantDeletionService;

    private BillingTenantTeardownContributor contributor;

    @BeforeEach
    void setUp() {
        contributor = new BillingTenantTeardownContributor(
            billingSubscriptionService, stripeClient, tenantDeletionServiceProvider);
    }

    @Test
    void testSnapshotsIdsThenCancels() {
        BillingSubscription subscription = mock(BillingSubscription.class);

        when(subscription.getCustomerId()).thenReturn("cus_1");
        when(subscription.getSubscriptionId()).thenReturn("sub_1");
        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(subscription));
        when(tenantDeletionServiceProvider.getIfAvailable()).thenReturn(tenantDeletionService);

        List<String> failures = contributor.onDeletionRequested(TENANT_ID);

        assertThat(failures).isEmpty();

        InOrder inOrder = inOrder(tenantDeletionService, stripeClient);

        inOrder.verify(tenantDeletionService)
            .recordStripeIds(TENANT_ID, "cus_1", "sub_1");
        inOrder.verify(stripeClient)
            .cancelImmediately("sub_1", TENANT_ID);
    }

    @Test
    void testNoSubscriptionIsNoOp() {
        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.empty());
        when(tenantDeletionServiceProvider.getIfAvailable()).thenReturn(tenantDeletionService);

        assertThat(contributor.onDeletionRequested(TENANT_ID)).isEmpty();

        verify(stripeClient, never()).cancelImmediately(anyString(), anyString());
    }

    @Test
    void testAbsentDeletionServiceIsNoOp() {
        when(tenantDeletionServiceProvider.getIfAvailable()).thenReturn(null);

        assertThat(contributor.onDeletionRequested(TENANT_ID)).isEmpty();

        verify(stripeClient, never()).cancelImmediately(anyString(), anyString());
    }

    @Test
    void testStripeFailureIsReturnedAsFailureLine() {
        BillingSubscription subscription = mock(BillingSubscription.class);

        when(subscription.getCustomerId()).thenReturn("cus_1");
        when(subscription.getSubscriptionId()).thenReturn("sub_1");
        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(subscription));
        when(tenantDeletionServiceProvider.getIfAvailable()).thenReturn(tenantDeletionService);
        doThrow(new PaymentClientException("stripe down", new RuntimeException())).when(stripeClient)
            .cancelImmediately("sub_1", TENANT_ID);

        assertThat(contributor.onDeletionRequested(TENANT_ID)).containsExactly("billing: stripe down");
    }
}
