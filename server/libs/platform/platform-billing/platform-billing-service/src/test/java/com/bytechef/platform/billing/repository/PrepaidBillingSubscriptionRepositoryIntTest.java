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

package com.bytechef.platform.billing.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.billing.config.BillingIntTestConfiguration;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Matija Petanjek
 */
@SpringBootTest(classes = BillingIntTestConfiguration.class, properties = "billing.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
class PrepaidBillingSubscriptionRepositoryIntTest {

    @Autowired
    private BillingSubscriptionRepository billingSubscriptionRepository;

    @Autowired
    private PrepaidBillingSubscriptionRepository prepaidBillingSubscriptionRepository;

    private long subscriptionId;

    @BeforeEach
    void beforeEach() {
        BillingSubscription billingSubscription = new BillingSubscription();

        billingSubscription.setCurrentPeriodEnd(Instant.parse("2026-11-01T00:00:00Z"));
        billingSubscription.setCurrentPeriodStart(Instant.parse("2026-10-01T00:00:00Z"));
        billingSubscription.setCustomerId("cus_test");
        billingSubscription.setPlanName("STARTER");
        billingSubscription.setPlanUnitTotal(2);
        billingSubscription.setProductId("prod_test");
        billingSubscription.setStatus(BillingSubscription.Status.ACTIVE);
        billingSubscription.setSubscriptionId("sub_test");
        billingSubscription.setSubscriptionModel(BillingSubscription.SubscriptionModel.PREPAID);

        subscriptionId = billingSubscriptionRepository.save(billingSubscription)
            .getId();
    }

    @AfterEach
    void afterEach() {
        billingSubscriptionRepository.deleteAll();
    }

    @Test
    void testIncrementPlanUnitUsedUntilPlanTotal() {
        assertThat(prepaidBillingSubscriptionRepository.incrementPlanUnitUsed(subscriptionId)).isEqualTo(1);
        assertThat(prepaidBillingSubscriptionRepository.incrementPlanUnitUsed(subscriptionId)).isEqualTo(1);
        assertThat(prepaidBillingSubscriptionRepository.incrementPlanUnitUsed(subscriptionId)).isEqualTo(0);

        assertThat(reload().getPlanUnitUsed()).isEqualTo(2);
    }

    @Test
    void testIncrementExtraUnitUsedUntilExtraTotal() {
        prepaidBillingSubscriptionRepository.addExtraUnitTotal(subscriptionId, 1);

        assertThat(prepaidBillingSubscriptionRepository.incrementExtraUnitUsed(subscriptionId)).isEqualTo(1);
        assertThat(prepaidBillingSubscriptionRepository.incrementExtraUnitUsed(subscriptionId)).isEqualTo(0);

        assertThat(reload().getExtraUnitUsed()).isEqualTo(1);
    }

    @Test
    void testConcurrentConsumptionNeverExceedsTotals() throws Exception {
        BillingSubscription billingSubscription = reload();

        billingSubscription.setPlanUnitTotal(50);

        billingSubscriptionRepository.save(billingSubscription);

        prepaidBillingSubscriptionRepository.addExtraUnitTotal(subscriptionId, 30);

        int attempts = 200;
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Callable<Integer>> consumptions = new ArrayList<>();

        for (int index = 0; index < attempts; index++) {
            consumptions.add(() -> {
                startLatch.await();

                if (prepaidBillingSubscriptionRepository.incrementPlanUnitUsed(subscriptionId) == 1) {
                    return 1;
                }

                return prepaidBillingSubscriptionRepository.incrementExtraUnitUsed(subscriptionId);
            });
        }

        int consumed = 0;

        try (ExecutorService executorService = Executors.newFixedThreadPool(8)) {
            List<Future<Integer>> futures = new ArrayList<>();

            for (Callable<Integer> consumption : consumptions) {
                futures.add(executorService.submit(consumption));
            }

            startLatch.countDown();

            for (Future<Integer> future : futures) {
                consumed += future.get(30, TimeUnit.SECONDS);
            }
        }

        BillingSubscription consumedSubscription = reload();

        assertThat(consumed).isEqualTo(80);
        assertThat(consumedSubscription.getPlanUnitUsed()).isEqualTo(50);
        assertThat(consumedSubscription.getExtraUnitUsed()).isEqualTo(30);
    }

    @Test
    void testResetPlanUnitUsed() {
        prepaidBillingSubscriptionRepository.incrementPlanUnitUsed(subscriptionId);
        prepaidBillingSubscriptionRepository.incrementPlanUnitUsed(subscriptionId);

        prepaidBillingSubscriptionRepository.resetPlanUnitUsed(subscriptionId);

        assertThat(reload().getPlanUnitUsed()).isEqualTo(0);
    }

    @Test
    void testResetUnitUsedForNewPeriodKeepsOnlyUnusedExtraUnits() {
        prepaidBillingSubscriptionRepository.incrementPlanUnitUsed(subscriptionId);
        prepaidBillingSubscriptionRepository.addExtraUnitTotal(subscriptionId, 3);
        prepaidBillingSubscriptionRepository.incrementExtraUnitUsed(subscriptionId);

        prepaidBillingSubscriptionRepository.resetUnitUsedForNewPeriod(subscriptionId);

        BillingSubscription billingSubscription = reload();

        assertThat(billingSubscription.getPlanUnitUsed()).isEqualTo(0);
        assertThat(billingSubscription.getExtraUnitTotal()).isEqualTo(2);
        assertThat(billingSubscription.getExtraUnitUsed()).isEqualTo(0);
    }

    @Test
    void testSaveDoesNotOverwriteExtraUnitTotal() {
        BillingSubscription staleBillingSubscription = reload();

        prepaidBillingSubscriptionRepository.addExtraUnitTotal(subscriptionId, 1000);

        billingSubscriptionRepository.save(staleBillingSubscription);

        assertThat(reload().getExtraUnitTotal()).isEqualTo(1000);
    }

    private BillingSubscription reload() {
        return billingSubscriptionRepository.findById(subscriptionId)
            .orElseThrow();
    }
}
