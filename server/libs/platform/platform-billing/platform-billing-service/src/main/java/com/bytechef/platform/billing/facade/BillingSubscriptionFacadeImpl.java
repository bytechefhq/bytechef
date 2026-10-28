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

import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.domain.BillingSubscription.SubscriptionModel;
import com.bytechef.platform.billing.dto.BillingSubscriptionDTO;
import com.bytechef.platform.billing.handler.SubscriptionModelHandlerRegistry;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @author Matija Petanjek
 */
@Service
@ConditionalOnProperty(prefix = "billing", name = "enabled", havingValue = "true")
public class BillingSubscriptionFacadeImpl implements BillingSubscriptionFacade {

    private final BillingSubscriptionService billingSubscriptionService;
    private final SubscriptionModelHandlerRegistry subscriptionModelHandlerRegistry;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public BillingSubscriptionFacadeImpl(
        BillingSubscriptionService billingSubscriptionService,
        SubscriptionModelHandlerRegistry subscriptionModelHandlerRegistry) {

        this.billingSubscriptionService = billingSubscriptionService;
        this.subscriptionModelHandlerRegistry = subscriptionModelHandlerRegistry;
    }

    @Override
    public void cancelSubscription() {
        BillingSubscription subscription = billingSubscriptionService.fetchCurrentSubscription()
            .orElseThrow(() -> new IllegalStateException("No active subscription found"));

        subscriptionModelHandlerRegistry.getSubscriptionModelHandler(subscription.getSubscriptionModel())
            .cancelSubscription(subscription);
    }

    @Override
    public String createCheckoutSession(String planName) {
        return subscriptionModelHandlerRegistry.getSubscriptionModelHandler(SubscriptionModel.PREPAID)
            .createCheckoutSession(planName);
    }

    @Override
    public void reactivateSubscription() {
        BillingSubscription subscription = billingSubscriptionService.fetchCurrentSubscription()
            .orElseThrow(() -> new IllegalStateException("No active subscription found"));

        subscriptionModelHandlerRegistry.getSubscriptionModelHandler(subscription.getSubscriptionModel())
            .reactivateSubscription(subscription);
    }

    @Override
    public void updateSubscription(String newPlanName) {
        BillingSubscription currentSubscription = billingSubscriptionService.fetchCurrentSubscription()
            .orElseThrow(() -> new IllegalStateException("No active subscription found"));

        subscriptionModelHandlerRegistry.getSubscriptionModelHandler(currentSubscription.getSubscriptionModel())
            .updateSubscription(currentSubscription, newPlanName);
    }

    @Override
    public Optional<BillingSubscriptionDTO> fetchCurrentSubscription() {
        return billingSubscriptionService.fetchCurrentSubscription()
            .map(subscription -> subscriptionModelHandlerRegistry
                .getSubscriptionModelHandler(subscription.getSubscriptionModel())
                .toBillingSubscriptionDTO(subscription));
    }
}
