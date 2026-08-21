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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Matija Petanjek
 */
class BillingSubscriptionCacheServiceTest {

    private CacheManager cacheManager;
    private Cache trialStatusCache;
    private BillingSubscriptionCacheService billingSubscriptionCacheService;

    @BeforeEach
    void setUp() {
        cacheManager = new ConcurrentMapCacheManager(TrialServiceImpl.TRIAL_STATUS_CACHE);
        trialStatusCache = cacheManager.getCache(TrialServiceImpl.TRIAL_STATUS_CACHE);
        billingSubscriptionCacheService = new BillingSubscriptionCacheService(cacheManager);
    }

    @Test
    void testEvictTrialStatusCacheAfterCommitDefersEvictionWhenTransactionIsActive() {
        trialStatusCache.put("cachedTrialStatus", "stale");

        TransactionSynchronizationManager.initSynchronization();

        try {
            billingSubscriptionCacheService.evictTrialStatusCacheAfterCommit();

            assertThat(trialStatusCache.get("cachedTrialStatus")).isNotNull();

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            assertThat(trialStatusCache.get("cachedTrialStatus")).isNull();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void testEvictTrialStatusCacheAfterCommitEvictsImmediatelyWhenNoTransactionIsActive() {
        trialStatusCache.put("cachedTrialStatus", "stale");

        billingSubscriptionCacheService.evictTrialStatusCacheAfterCommit();

        assertThat(trialStatusCache.get("cachedTrialStatus")).isNull();
    }
}
