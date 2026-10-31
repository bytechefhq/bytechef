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

import com.bytechef.platform.billing.config.BillingIntTestConfiguration;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.repository.BillingSubscriptionRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Matija Petanjek
 */
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
@SpringBootTest(classes = BillingIntTestConfiguration.class)
class BillingSubscriptionServiceIntTest {

    private static final Instant NOW = Instant.now()
        .truncatedTo(ChronoUnit.SECONDS);

    @Autowired
    private BillingSubscriptionRepository billingSubscriptionRepository;

    @Autowired
    private BillingSubscriptionService billingSubscriptionService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void beforeEach() {
        billingSubscriptionRepository.deleteAll();

        getTrialStatusCache().clear();
    }

    @Test
    void testFetchCurrentSubscriptionReturnsEmptyWhenSubscriptionIsCanceledAndNoTrialExists() {
        saveSubscription("STARTER", BillingSubscription.Status.CANCELED, NOW);

        Optional<BillingSubscription> result = billingSubscriptionService.fetchCurrentSubscription();

        assertThat(result).isEmpty();
    }

    @Test
    void testFetchCurrentSubscriptionFallsBackToTrialWhenCurrentIsCanceled() {
        saveSubscription("TRIAL", BillingSubscription.Status.ACTIVE, NOW.minus(10, ChronoUnit.DAYS));
        saveSubscription("STARTER", BillingSubscription.Status.CANCELED, NOW);

        Optional<BillingSubscription> result = billingSubscriptionService.fetchCurrentSubscription();

        assertThat(result).isPresent();
        assertThat(result.get()
            .getPlanName()).isEqualTo("TRIAL");
    }

    @Test
    void testFetchCurrentSubscriptionReturnsActiveSubscription() {
        saveSubscription("TRIAL", BillingSubscription.Status.CANCELED, NOW.minus(10, ChronoUnit.DAYS));
        saveSubscription("STARTER", BillingSubscription.Status.ACTIVE, NOW);

        Optional<BillingSubscription> result = billingSubscriptionService.fetchCurrentSubscription();

        assertThat(result).isPresent();
        assertThat(result.get()
            .getStatus()).isEqualTo(BillingSubscription.Status.ACTIVE);
        assertThat(result.get()
            .getPlanName()).isEqualTo("STARTER");
    }

    @Test
    void testSaveEvictsTrialStatusCacheAfterCommit() {
        Cache trialStatusCache = getTrialStatusCache();

        trialStatusCache.put("trialStatus", "cached");

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        BillingSubscription result = transactionTemplate.execute(transactionStatus -> {
            BillingSubscription savedSubscription = billingSubscriptionService.save(
                newSubscription("STARTER", BillingSubscription.Status.ACTIVE));

            assertThat(trialStatusCache.get("trialStatus"))
                .as("the cache must not be evicted before the subscription row is committed")
                .isNotNull();

            return savedSubscription;
        });

        assertThat(result).isNotNull();
        assertThat(billingSubscriptionRepository.findById(result.getId())).isPresent();
        assertThat(trialStatusCache.get("trialStatus")).isNull();
    }

    private Cache getTrialStatusCache() {
        Cache cache = cacheManager.getCache(TrialServiceImpl.TRIAL_STATUS_CACHE);

        assertThat(cache).isNotNull();

        return cache;
    }

    private BillingSubscription newSubscription(String planName, BillingSubscription.Status status) {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setPlanName(planName);
        subscription.setStatus(status);
        subscription.setProductUnitLimit(5000);
        subscription.setCurrentPeriodStart(NOW.minus(7, ChronoUnit.DAYS));
        subscription.setCurrentPeriodEnd(NOW.plus(7, ChronoUnit.DAYS));

        return subscription;
    }

    private void saveSubscription(String planName, BillingSubscription.Status status, Instant createdDate) {
        BillingSubscription subscription = billingSubscriptionRepository.save(newSubscription(planName, status));

        jdbcTemplate.update(
            "UPDATE billing_subscription SET created_date = ? WHERE id = ?", Timestamp.from(createdDate),
            subscription.getId());
    }
}
