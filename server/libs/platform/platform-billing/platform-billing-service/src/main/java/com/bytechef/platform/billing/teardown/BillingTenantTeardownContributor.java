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

import com.bytechef.platform.billing.client.StripeClient;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.exception.PaymentClientException;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import com.bytechef.tenant.service.TenantDeletionService;
import com.bytechef.tenant.teardown.TenantTeardownContributor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@Order(500)
@ConditionalOnProperty(prefix = "billing", name = "enabled", havingValue = "true")
class BillingTenantTeardownContributor implements TenantTeardownContributor {

    private final BillingSubscriptionService billingSubscriptionService;
    private final ObjectProvider<TenantDeletionService> tenantDeletionServiceProvider;
    private final StripeClient stripeClient;

    @SuppressFBWarnings("EI")
    BillingTenantTeardownContributor(
        BillingSubscriptionService billingSubscriptionService, StripeClient stripeClient,
        ObjectProvider<TenantDeletionService> tenantDeletionServiceProvider) {

        this.billingSubscriptionService = billingSubscriptionService;
        this.stripeClient = stripeClient;
        this.tenantDeletionServiceProvider = tenantDeletionServiceProvider;
    }

    @Override
    public List<String> onDeletionRequested(String tenantId) {
        TenantDeletionService tenantDeletionService = tenantDeletionServiceProvider.getIfAvailable();

        if (tenantDeletionService == null) {
            return List.of();
        }

        Optional<BillingSubscription> subscriptionOptional = billingSubscriptionService.fetchCurrentSubscription();

        if (subscriptionOptional.isEmpty()) {
            return List.of();
        }

        BillingSubscription subscription = subscriptionOptional.get();

        String subscriptionId = subscription.getSubscriptionId();

        if (subscriptionId == null || subscriptionId.isBlank() ||
            subscription.getStatus() == BillingSubscription.Status.CANCELED) {

            return List.of();
        }

        tenantDeletionService.recordStripeIds(tenantId, subscription.getCustomerId(), subscriptionId);

        try {
            stripeClient.cancelImmediately(subscriptionId, tenantId);
        } catch (PaymentClientException paymentClientException) {
            return List.of("billing: " + paymentClientException.getMessage());
        }

        return List.of();
    }
}
