/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.tenant.domain.TenantDeletion;
import com.bytechef.tenant.domain.TenantDeletion.Status;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SpringBootTest(classes = TenantDeletionRepositoryIntTest.IntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
class TenantDeletionRepositoryIntTest {

    private static final String TENANT_ID = "000001";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TenantDeletionRepository tenantDeletionRepository;

    @AfterEach
    void afterEach() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update("DELETE FROM public.tenant_deletion");
    }

    @Test
    void testClaimForPurgeIsExclusive() {
        Instant now = Instant.now()
            .truncatedTo(ChronoUnit.SECONDS);

        tenantDeletionRepository.markPendingDeletion(TENANT_ID, "admin@x", now);

        assertThat(tenantDeletionRepository.claimForPurge(TENANT_ID, now)).isTrue();
        assertThat(tenantDeletionRepository.claimForPurge(TENANT_ID, now)).isFalse();
        assertThat(tenantDeletionRepository.claimForPurge(TENANT_ID, now.plus(Duration.ofHours(2)))).isTrue();
    }

    @Test
    void testClaimForPurgeRejectsFuturePurgeAfter() {
        Instant now = Instant.now()
            .truncatedTo(ChronoUnit.SECONDS);

        tenantDeletionRepository.markPendingDeletion(TENANT_ID, "admin@x", now.plus(Duration.ofDays(30)));

        assertThat(tenantDeletionRepository.claimForPurge(TENANT_ID, now)).isFalse();
    }

    @Test
    void testGetDueForPurgeReturnsOnlyElapsedPendingRows() {
        Instant now = Instant.now()
            .truncatedTo(ChronoUnit.SECONDS);

        tenantDeletionRepository.markPendingDeletion("000001", "admin@x", now.minusSeconds(60));
        tenantDeletionRepository.markPendingDeletion("000002", "admin@x", now.plusSeconds(3600));
        tenantDeletionRepository.markPendingDeletion("000003", "admin@x", now.minusSeconds(60));
        tenantDeletionRepository.markPurged("000003");

        List<TenantDeletion> dueTenantDeletions = tenantDeletionRepository.getDueForPurge(now);

        assertThat(dueTenantDeletions).extracting(TenantDeletion::tenantId)
            .containsExactly("000001");
        assertThat(dueTenantDeletions.getFirst()
            .status()).isEqualTo(Status.PENDING_DELETION);
    }

    @Test
    void testMarkPendingDeletionIsIdempotent() {
        Instant purgeAfter = Instant.now()
            .truncatedTo(ChronoUnit.SECONDS);

        assertThat(tenantDeletionRepository.markPendingDeletion(TENANT_ID, "admin@x", purgeAfter)).isTrue();
        assertThat(tenantDeletionRepository.markPendingDeletion(TENANT_ID, "admin@x", purgeAfter.plusSeconds(60)))
            .isFalse();

        TenantDeletion tenantDeletion = tenantDeletionRepository.fetchTenantDeletion(TENANT_ID)
            .orElseThrow();

        assertThat(tenantDeletion.purgeAfter()).isEqualTo(purgeAfter);
    }

    @Test
    void testMarkPendingDeletionAfterCancelResetsRow() {
        Instant firstPurgeAfter = Instant.now()
            .truncatedTo(ChronoUnit.SECONDS);
        Instant secondPurgeAfter = firstPurgeAfter.plus(Duration.ofDays(30));

        tenantDeletionRepository.markPendingDeletion(TENANT_ID, "admin@x", firstPurgeAfter);
        tenantDeletionRepository.recordStripeIds(TENANT_ID, "cus_1", "sub_1");
        tenantDeletionRepository.recordTeardownFailures(TENANT_ID, List.of("a"));
        tenantDeletionRepository.claimForPurge(TENANT_ID, firstPurgeAfter);

        new JdbcTemplate(dataSource).update(
            "UPDATE public.tenant_deletion SET status = ? WHERE tenant_id = ?", Status.CANCELLED.ordinal(),
            TENANT_ID);

        assertThat(tenantDeletionRepository.markPendingDeletion(TENANT_ID, "other@x", secondPurgeAfter)).isTrue();

        TenantDeletion tenantDeletion = tenantDeletionRepository.fetchTenantDeletion(TENANT_ID)
            .orElseThrow();

        assertThat(tenantDeletion.status()).isEqualTo(Status.PENDING_DELETION);
        assertThat(tenantDeletion.requestedBy()).isEqualTo("other@x");
        assertThat(tenantDeletion.purgeAfter()).isEqualTo(secondPurgeAfter);
        assertThat(tenantDeletion.stripeCustomerId()).isNull();
        assertThat(tenantDeletion.stripeSubscriptionId()).isNull();
        assertThat(tenantDeletion.purgeClaimedDate()).isNull();
        assertThat(tenantDeletion.teardownFailures()).isEmpty();
    }

    @Test
    void testMarkPendingDeletionOnPurgedRowReturnsFalse() {
        Instant purgeAfter = Instant.now()
            .truncatedTo(ChronoUnit.SECONDS);

        tenantDeletionRepository.markPendingDeletion(TENANT_ID, "admin@x", purgeAfter);
        tenantDeletionRepository.markPurged(TENANT_ID);

        assertThat(tenantDeletionRepository.markPendingDeletion(TENANT_ID, "other@x", purgeAfter.plusSeconds(60)))
            .isFalse();

        TenantDeletion tenantDeletion = tenantDeletionRepository.fetchTenantDeletion(TENANT_ID)
            .orElseThrow();

        assertThat(tenantDeletion.status()).isEqualTo(Status.PURGED);
        assertThat(tenantDeletion.purgeAfter()).isEqualTo(purgeAfter);
    }

    @Test
    void testRecordStripeIdsRoundTrip() {
        tenantDeletionRepository.markPendingDeletion(TENANT_ID, "admin@x", Instant.now());
        tenantDeletionRepository.recordStripeIds(TENANT_ID, "cus_1", "sub_1");

        TenantDeletion tenantDeletion = tenantDeletionRepository.fetchTenantDeletion(TENANT_ID)
            .orElseThrow();

        assertThat(tenantDeletion.stripeCustomerId()).isEqualTo("cus_1");
        assertThat(tenantDeletion.stripeSubscriptionId()).isEqualTo("sub_1");
    }

    @Test
    void testTeardownFailuresRoundTrip() {
        tenantDeletionRepository.markPendingDeletion(TENANT_ID, "admin@x", Instant.now());

        assertThat(tenantDeletionRepository.fetchTenantDeletion(TENANT_ID)
            .orElseThrow()
            .teardownFailures()).isEmpty();

        tenantDeletionRepository.recordTeardownFailures(TENANT_ID, List.of("a", "b"));

        assertThat(tenantDeletionRepository.fetchTenantDeletion(TENANT_ID)
            .orElseThrow()
            .teardownFailures()).containsExactly("a", "b");
    }

    @Test
    void testFindInactiveTenantIdsReturnsPendingAndPurged() {
        Instant now = Instant.now();

        tenantDeletionRepository.markPendingDeletion("000001", "admin@x", now);
        tenantDeletionRepository.markPendingDeletion("000002", "admin@x", now);
        tenantDeletionRepository.markPurged("000002");

        assertThat(tenantDeletionRepository.findInactiveTenantIds()).containsExactlyInAnyOrder("000001", "000002");
    }

    @Configuration
    @EnableAutoConfiguration
    @Import({
        LiquibaseConfiguration.class, TenantDeletionRepository.class
    })
    static class IntTestConfiguration {
    }
}
