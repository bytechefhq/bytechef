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

import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.repository.BillingSubscriptionRepository;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Matija Petanjek
 */
@Service
@ConditionalOnProperty(prefix = "billing", name = "enabled", havingValue = "true")
@Transactional
class BillingSubscriptionServiceImpl implements BillingSubscriptionService {

    private final BillingSubscriptionCacheService billingSubscriptionCacheService;
    private final BillingSubscriptionRepository billingSubscriptionRepository;

    BillingSubscriptionServiceImpl(
        BillingSubscriptionCacheService billingSubscriptionCacheService,
        BillingSubscriptionRepository billingSubscriptionRepository) {

        this.billingSubscriptionCacheService = billingSubscriptionCacheService;
        this.billingSubscriptionRepository = billingSubscriptionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BillingSubscription> fetchCurrentSubscription() {
        Optional<BillingSubscription> subscription = billingSubscriptionRepository.findFirstByOrderByCreatedDateDesc()
            .filter(currentSubscription -> currentSubscription.getStatus() != BillingSubscription.Status.CANCELED);

        if (subscription.isPresent()) {
            return subscription;
        }

        return billingSubscriptionRepository.findFirstByPlanNameOrderByCreatedDateDesc("TRIAL");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BillingSubscription> fetchSubscriptionBySubscriptionId(String subscriptionId) {
        return billingSubscriptionRepository.findBySubscriptionId(subscriptionId);
    }

    @Override
    public BillingSubscription save(BillingSubscription subscription) {
        BillingSubscription savedSubscription = billingSubscriptionRepository.save(subscription);

        billingSubscriptionCacheService.evictTrialStatusCacheAfterCommit();

        return savedSubscription;
    }
}
