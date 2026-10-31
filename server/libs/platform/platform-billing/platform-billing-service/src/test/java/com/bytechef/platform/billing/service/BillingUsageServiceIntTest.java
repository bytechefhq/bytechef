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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.billing.client.StripeClient;
import com.bytechef.platform.billing.config.BillingIntTestConfiguration;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.repository.BillingSubscriptionRepository;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Matija Petanjek
 */
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
@SpringBootTest(classes = BillingIntTestConfiguration.class)
class BillingUsageServiceIntTest {

    private static final int STATUS_STARTED = 1;
    private static final int STATUS_COMPLETED = 2;
    private static final int STATUS_FAILED = 3;
    private static final int STATUS_STOPPED = 4;

    @Autowired
    private BillingSubscriptionRepository billingSubscriptionRepository;

    @Autowired
    private BillingUsageService billingUsageService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private StripeClient stripeClient;

    @MockitoBean
    private TenantService tenantService;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("DELETE FROM principal_job");
        jdbcTemplate.update("DELETE FROM job");

        billingSubscriptionRepository.deleteAll();

        when(tenantService.getTenantIds()).thenReturn(List.of(TenantContext.DEFAULT_TENANT_ID));
    }

    @Test
    void testReportUsageSkipsWhenNoSubscription() {
        seedJobs(3, STATUS_COMPLETED, Instant.now(), true);

        billingUsageService.reportUsage(Instant.now());

        verify(stripeClient, never()).reportMeterEvent(anyString(), anyInt(), anyString());

        assertThat(billingSubscriptionRepository.count()).isZero();
    }

    @Test
    void testReportUsageSkipsForTrialSubscription() {
        Instant periodStart = Instant.parse("2026-06-01T00:00:00Z");
        BillingSubscription subscription = saveSubscription("TRIAL", "cus_trial", "sub_trial", periodStart, null);

        seedJobs(7, STATUS_COMPLETED, Instant.parse("2026-06-01T00:30:00Z"), true);

        billingUsageService.reportUsage(Instant.parse("2026-06-01T01:00:00Z"));

        verify(stripeClient, never()).reportMeterEvent(anyString(), anyInt(), anyString());

        assertThat(findPersisted(subscription).getLastReportedAt())
            .as("a trial subscription is never saved, so lastReportedAt stays unset")
            .isNull();
    }

    @Test
    void testReportUsageSkipsStripeWhenCountIsZero() {
        Instant periodStart = Instant.parse("2026-06-01T00:00:00Z");
        BillingSubscription subscription = saveSubscription("STARTER", "cus_test", "sub_test", periodStart, null);

        seedJobs(2, STATUS_COMPLETED, Instant.parse("2026-05-31T23:00:00Z"), true);
        seedJobs(2, STATUS_COMPLETED, Instant.parse("2026-06-01T00:30:00Z"), false);
        seedJobs(2, STATUS_STARTED, Instant.parse("2026-06-01T00:30:00Z"), true);

        Instant scheduledFireTime = Instant.parse("2026-06-01T01:00:00Z");

        billingUsageService.reportUsage(scheduledFireTime);

        verify(stripeClient, never()).reportMeterEvent(anyString(), anyInt(), anyString());

        assertThat(findPersisted(subscription).getLastReportedAt()).isEqualTo(scheduledFireTime);
    }

    @Test
    void testReportUsageReportsCountAndUpdatesLastReportedAt() {
        Instant periodStart = Instant.parse("2026-06-01T00:00:00Z");
        BillingSubscription subscription = saveSubscription("STARTER", "cus_123", "sub_abc", periodStart, null);

        seedJobs(30, STATUS_COMPLETED, Instant.parse("2026-06-01T00:10:00Z"), true);
        seedJobs(8, STATUS_FAILED, Instant.parse("2026-06-01T00:20:00Z"), true);
        seedJobs(4, STATUS_STOPPED, Instant.parse("2026-06-01T00:30:00Z"), true);
        seedJobs(5, STATUS_COMPLETED, Instant.parse("2026-06-01T00:40:00Z"), false);
        seedJobs(6, STATUS_STARTED, Instant.parse("2026-06-01T00:50:00Z"), true);
        seedJobs(9, STATUS_COMPLETED, Instant.parse("2026-06-01T01:00:00Z"), true);

        Instant scheduledFireTime = Instant.parse("2026-06-01T01:00:00Z");

        billingUsageService.reportUsage(scheduledFireTime);

        verify(stripeClient).reportMeterEvent("cus_123", 42, "sub_abc_" + scheduledFireTime.getEpochSecond());

        assertThat(findPersisted(subscription).getLastReportedAt()).isEqualTo(scheduledFireTime);
    }

    @Test
    void testReportUsageUsesPeriodStartWhenLastReportedAtIsNull() {
        Instant periodStart = Instant.parse("2026-06-01T00:00:00Z");

        saveSubscription("STARTER", "cus_123", "sub_abc", periodStart, null);

        seedJobs(4, STATUS_COMPLETED, Instant.parse("2026-05-31T23:59:59Z"), true);
        seedJobs(10, STATUS_COMPLETED, periodStart, true);

        Instant scheduledFireTime = Instant.parse("2026-06-01T01:00:00Z");

        billingUsageService.reportUsage(scheduledFireTime);

        verify(stripeClient).reportMeterEvent("cus_123", 10, "sub_abc_" + scheduledFireTime.getEpochSecond());
    }

    @Test
    void testReportUsageUsesLastReportedAtWhenPresent() {
        Instant lastReportedAt = Instant.parse("2026-06-01T02:00:00Z");

        saveSubscription(
            "STARTER", "cus_123", "sub_abc", Instant.parse("2026-06-01T00:00:00Z"), lastReportedAt);

        seedJobs(3, STATUS_COMPLETED, Instant.parse("2026-06-01T01:00:00Z"), true);
        seedJobs(5, STATUS_COMPLETED, Instant.parse("2026-06-01T02:30:00Z"), true);

        Instant scheduledFireTime = Instant.parse("2026-06-01T03:00:00Z");

        billingUsageService.reportUsage(scheduledFireTime);

        verify(stripeClient).reportMeterEvent("cus_123", 5, "sub_abc_" + scheduledFireTime.getEpochSecond());
    }

    private BillingSubscription findPersisted(BillingSubscription subscription) {
        return billingSubscriptionRepository.findById(subscription.getId())
            .orElseThrow();
    }

    private BillingSubscription saveSubscription(
        String planName, String customerId, String subscriptionId, Instant periodStart, Instant lastReportedAt) {

        BillingSubscription subscription = new BillingSubscription();

        subscription.setCustomerId(customerId);
        subscription.setSubscriptionId(subscriptionId);
        subscription.setPlanName(planName);
        subscription.setStatus(BillingSubscription.Status.ACTIVE);
        subscription.setProductUnitLimit(5000);
        subscription.setCurrentPeriodStart(periodStart);
        subscription.setCurrentPeriodEnd(periodStart.plusSeconds(30L * 24 * 3600));
        subscription.setLastReportedAt(lastReportedAt);

        return billingSubscriptionRepository.save(subscription);
    }

    /**
     * Inserts {@code count} job rows ending at {@code endDate}. A deployed job gets a principal_job row; editor
     * test-run jobs never do, which is what the usage query relies on to exclude them.
     */
    private void seedJobs(int count, int status, Instant endDate, boolean deployed) {
        String workflowId = UUID.randomUUID()
            .toString();

        jdbcTemplate.update(
            """
                INSERT INTO job (workflow_id, status, current_task, priority, created_date, created_by,
                    last_modified_date, last_modified_by, version, end_date)
                SELECT ?, ?, 0, 0, now(), 'system', now(), 'system', 0, ? FROM generate_series(1, ?)
                """,
            workflowId, status, Timestamp.from(endDate), count);

        if (deployed) {
            jdbcTemplate.update(
                "INSERT INTO principal_job (job_id, principal_id, type) SELECT id, 1, 0 FROM job WHERE workflow_id = ?",
                workflowId);
        }
    }
}
