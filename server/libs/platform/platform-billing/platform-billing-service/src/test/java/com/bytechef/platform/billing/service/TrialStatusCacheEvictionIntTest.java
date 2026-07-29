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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.repository.BillingSubscriptionRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Proves that mutating a subscription via {@link BillingSubscriptionServiceImpl#save(BillingSubscription)} evicts the
 * {@code trialStatus} cache so the next {@link TrialServiceImpl#validateTrial()} call recomputes instead of serving a
 * stale cached result. Uses a real Spring AOP cache proxy (via {@link AnnotationConfigApplicationContext}) rather than
 * Mockito, since {@code @Cacheable} behavior only exists through the proxy.
 *
 * @author Matija Petanjek
 */
class TrialStatusCacheEvictionIntTest {

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        ApplicationEventPublisher applicationEventPublisher() {
            return mock(ApplicationEventPublisher.class);
        }

        @Bean
        BillingSubscriptionCacheService billingSubscriptionCacheService(CacheManager cacheManager) {
            return new BillingSubscriptionCacheService(cacheManager);
        }

        @Bean
        BillingSubscriptionRepository billingSubscriptionRepository() {
            return mock(BillingSubscriptionRepository.class);
        }

        @Bean
        BillingSubscriptionService billingSubscriptionService(
            BillingSubscriptionCacheService billingSubscriptionCacheService,
            BillingSubscriptionRepository repository) {

            return new BillingSubscriptionServiceImpl(billingSubscriptionCacheService, repository);
        }

        @Bean
        BillingUsageService billingUsageService() {
            return mock(BillingUsageService.class);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        TrialService trialService(
            ApplicationEventPublisher applicationEventPublisher,
            BillingSubscriptionService billingSubscriptionService,
            BillingUsageService billingUsageService) {

            return new TrialServiceImpl(applicationEventPublisher, billingSubscriptionService, billingUsageService);
        }
    }

    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfig.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void testSaveEvictsTrialStatusCacheSoNextValidateTrialRecomputes() {
        BillingSubscriptionRepository repository = context.getBean(BillingSubscriptionRepository.class);
        BillingUsageService billingUsageService = context.getBean(BillingUsageService.class);
        BillingSubscriptionService billingSubscriptionService = context.getBean(BillingSubscriptionService.class);
        TrialService trialService = context.getBean(TrialService.class);

        BillingSubscription subscription = trialSubscription();

        when(repository.findFirstByOrderByCreatedDateDesc()).thenReturn(Optional.of(subscription));
        when(billingUsageService.countTaskExecutionsSince(any(), any())).thenReturn(100);

        trialService.validateTrial();
        trialService.validateTrial();

        verify(billingUsageService, times(1)).countTaskExecutionsSince(any(), any());

        billingSubscriptionService.save(subscription);

        trialService.validateTrial();

        verify(billingUsageService, times(2)).countTaskExecutionsSince(any(), any());
    }

    private static BillingSubscription trialSubscription() {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setPlanName("TRIAL");
        subscription.setStatus(BillingSubscription.Status.ACTIVE);
        subscription.setProductUnitLimit(5000);
        subscription.setCurrentPeriodStart(Instant.now()
            .minus(7, ChronoUnit.DAYS));
        subscription.setCurrentPeriodEnd(Instant.now()
            .plus(7, ChronoUnit.DAYS));

        return subscription;
    }
}
