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
    void testResetPlanUnitUsed() {
        prepaidBillingSubscriptionRepository.incrementPlanUnitUsed(subscriptionId);
        prepaidBillingSubscriptionRepository.incrementPlanUnitUsed(subscriptionId);

        prepaidBillingSubscriptionRepository.resetPlanUnitUsed(subscriptionId);

        assertThat(reload().getPlanUnitUsed()).isEqualTo(0);
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
