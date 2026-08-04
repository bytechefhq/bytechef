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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Evicts the trial status cache programmatically through the {@link CacheManager}, deferred until after transaction
 * commit. {@code @CacheEvict} on a {@code @Transactional} method does not work reliably here: neither annotation
 * declares an explicit advisor order, so Spring does not guarantee the caching advisor runs outside the transactional
 * one, and an eviction that fires before commit can be immediately undone by a concurrent read (e.g.
 * {@code TrialFilter}, which runs on almost every request) repopulating the cache with pre-commit data.
 *
 * @author Matija Petanjek
 */
@Service
@ConditionalOnProperty(prefix = "billing", name = "enabled", havingValue = "true")
class BillingSubscriptionCacheService {

    private static final Logger log = LoggerFactory.getLogger(BillingSubscriptionCacheService.class);

    private final CacheManager cacheManager;

    BillingSubscriptionCacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    void evictTrialStatusCacheAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictTrialStatusCache();
                }
            });
        } else {
            evictTrialStatusCache();
        }
    }

    private void evictTrialStatusCache() {
        Cache cache = cacheManager.getCache(TrialServiceImpl.TRIAL_STATUS_CACHE);

        if (cache == null) {
            return;
        }

        try {
            cache.clear();
        } catch (RuntimeException exception) {
            log.error(
                "Failed to evict the trial status cache after a subscription save. Stale trial status may be " +
                    "served until the cache entry expires.",
                exception);
        }
    }
}
