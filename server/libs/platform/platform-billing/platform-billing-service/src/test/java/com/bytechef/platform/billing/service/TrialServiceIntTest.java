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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.billing.config.BillingIntTestConfiguration;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.dto.TrialDTO;
import com.bytechef.platform.billing.event.TrialExpiredEvent;
import com.bytechef.platform.billing.repository.BillingSubscriptionRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * @author Matija Petanjek
 */
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
@RecordApplicationEvents
@SpringBootTest(classes = BillingIntTestConfiguration.class)
class TrialServiceIntTest {

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private BillingSubscriptionRepository billingSubscriptionRepository;

    @Autowired
    private BillingSubscriptionService billingSubscriptionService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TrialService trialService;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("DELETE FROM principal_job");
        jdbcTemplate.update("DELETE FROM job");

        billingSubscriptionRepository.deleteAll();

        Cache cache = cacheManager.getCache(TrialServiceImpl.TRIAL_STATUS_CACHE);

        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void testValidateTrialThrowsWhenNoSubscriptionExists() {
        assertThatThrownBy(() -> trialService.validateTrial())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No active subscription found");
    }

    @Test
    void testValidateTrialWhenPlanIsNotTrial() {
        BillingSubscription subscription = trialSubscription(Instant.now()
            .plus(7, ChronoUnit.DAYS));

        subscription.setPlanName("STARTER");

        BillingSubscription savedSubscription = billingSubscriptionRepository.save(subscription);

        seedDeployedJobs(6000);

        TrialDTO status = trialService.validateTrial();

        assertThat(status.expired())
            .as("a non-trial (paying) subscription must never be reported as expired, since TrialFilter blocks "
                + "requests with a 402 response whenever expired() is true")
            .isFalse();
        assertThat(status.daysRemaining()).isZero();
        assertThat(status.jobsExecuted()).isZero();
        assertThat(status.productUnitLimit()).isZero();
        assertThat(findPersisted(savedSubscription).getStatus()).isEqualTo(BillingSubscription.Status.ACTIVE);
    }

    @Test
    void testValidateTrialWhenTrialActiveAndNotExpired() {
        BillingSubscription savedSubscription = billingSubscriptionRepository.save(
            trialSubscription(Instant.now()
                .plus(7, ChronoUnit.DAYS)));

        seedDeployedJobs(100);

        TrialDTO status = trialService.validateTrial();

        assertThat(status.expired()).isFalse();
        assertThat(status.jobsExecuted()).isEqualTo(100);
        assertThat(status.productUnitLimit()).isEqualTo(5000);
        assertThat(status.daysRemaining()).isGreaterThan(0);
        assertThat(findPersisted(savedSubscription).getStatus()).isEqualTo(BillingSubscription.Status.ACTIVE);
        assertThat(applicationEvents.stream(TrialExpiredEvent.class)).isEmpty();
    }

    @Test
    void testValidateTrialWhenTimeExpiredExpiresSubscription() {
        BillingSubscription savedSubscription = billingSubscriptionRepository.save(
            trialSubscription(Instant.now()
                .minus(1, ChronoUnit.DAYS)));

        seedDeployedJobs(100);

        TrialDTO status = trialService.validateTrial();

        assertThat(status.expired()).isTrue();
        assertThat(status.daysRemaining()).isZero();
        assertThat(findPersisted(savedSubscription).getStatus()).isEqualTo(BillingSubscription.Status.CANCELED);
        assertThat(applicationEvents.stream(TrialExpiredEvent.class)).hasSize(1);
    }

    @Test
    void testValidateTrialWhenTaskLimitReachedExpiresSubscription() {
        BillingSubscription savedSubscription = billingSubscriptionRepository.save(
            trialSubscription(Instant.now()
                .plus(7, ChronoUnit.DAYS)));

        seedDeployedJobs(5000);

        TrialDTO status = trialService.validateTrial();

        assertThat(status.expired()).isTrue();
        assertThat(status.jobsExecuted()).isEqualTo(5000);
        assertThat(findPersisted(savedSubscription).getStatus()).isEqualTo(BillingSubscription.Status.CANCELED);
        assertThat(applicationEvents.stream(TrialExpiredEvent.class)).hasSize(1);
    }

    @Test
    void testValidateTrialDoesNotExpireAgainWhenAlreadyCanceled() {
        BillingSubscription subscription = trialSubscription(Instant.now()
            .minus(1, ChronoUnit.DAYS));

        subscription.setStatus(BillingSubscription.Status.CANCELED);

        BillingSubscription savedSubscription = billingSubscriptionRepository.save(subscription);

        Instant lastModifiedDate = findPersisted(savedSubscription).getLastModifiedDate();

        seedDeployedJobs(100);

        TrialDTO status = trialService.validateTrial();

        assertThat(status.expired()).isTrue();
        assertThat(findPersisted(savedSubscription).getLastModifiedDate())
            .as("an already canceled trial is not saved again")
            .isEqualTo(lastModifiedDate);
        assertThat(applicationEvents.stream(TrialExpiredEvent.class)).isEmpty();
    }

    @Test
    void testSaveEvictsTrialStatusCacheSoNextValidateTrialRecomputes() {
        BillingSubscription subscription = billingSubscriptionRepository.save(
            trialSubscription(Instant.now()
                .plus(7, ChronoUnit.DAYS)));

        seedDeployedJobs(100);

        assertThat(trialService.validateTrial()
            .jobsExecuted()).isEqualTo(100);

        seedDeployedJobs(50);

        assertThat(trialService.validateTrial()
            .jobsExecuted())
                .as("the second call is served from the trialStatus cache")
                .isEqualTo(100);

        billingSubscriptionService.save(subscription);

        assertThat(trialService.validateTrial()
            .jobsExecuted())
                .as("saving a subscription evicts the cache, so the next call recomputes")
                .isEqualTo(150);
    }

    private BillingSubscription findPersisted(BillingSubscription subscription) {
        return billingSubscriptionRepository.findById(subscription.getId())
            .orElseThrow();
    }

    private void seedDeployedJobs(int count) {
        String workflowId = UUID.randomUUID()
            .toString();
        Instant endDate = Instant.now()
            .minus(1, ChronoUnit.HOURS);

        jdbcTemplate.update(
            """
                INSERT INTO job (workflow_id, status, current_task, priority, created_date, created_by,
                    last_modified_date, last_modified_by, version, end_date)
                SELECT ?, 2, 0, 0, now(), 'system', now(), 'system', 0, ? FROM generate_series(1, ?)
                """,
            workflowId, Timestamp.from(endDate), count);
        jdbcTemplate.update(
            "INSERT INTO principal_job (job_id, principal_id, type) SELECT id, 1, 0 FROM job WHERE workflow_id = ?",
            workflowId);
    }

    private static BillingSubscription trialSubscription(Instant trialEnd) {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setPlanName("TRIAL");
        subscription.setStatus(BillingSubscription.Status.ACTIVE);
        subscription.setProductUnitLimit(5000);
        subscription.setCurrentPeriodStart(Instant.now()
            .minus(7, ChronoUnit.DAYS));
        subscription.setCurrentPeriodEnd(trialEnd);

        return subscription;
    }
}
