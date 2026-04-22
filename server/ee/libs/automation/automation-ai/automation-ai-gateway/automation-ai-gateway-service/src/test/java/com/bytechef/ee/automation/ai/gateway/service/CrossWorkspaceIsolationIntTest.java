/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.ai.gateway.config.AiGatewayIntTestConfiguration;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRequestLog;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertCondition;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertMetric;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertRule;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySession;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTraceSource;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityWebhookSubscription;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Cross-workspace isolation guarantees for every tenant-scoped service reachable via a workspaceId argument. Each test
 * seeds rows in two workspaces and asserts that a query/mutation scoped to workspace A never reads or touches rows from
 * workspace B.
 *
 * <p>
 * A dedicated suite surfaces tenant-isolation regressions at a glance rather than burying them across per-service
 * tests; one file here means a broken scoping contract fails fast.
 *
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiGatewayIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@AiGatewayIntTestConfigurationSharedMocks
public class CrossWorkspaceIsolationIntTest {

    private static final Long WORKSPACE_A = 101L;
    private static final Long WORKSPACE_B = 202L;

    @Autowired
    private AiGatewayRequestLogService aiGatewayRequestLogService;

    @Autowired
    private AiObservabilityAlertRuleService aiObservabilityAlertRuleService;

    @Autowired
    private AiObservabilitySessionService aiObservabilitySessionService;

    @Autowired
    private AiObservabilityTraceService aiObservabilityTraceService;

    @Autowired
    private AiObservabilityWebhookSubscriptionService aiObservabilityWebhookSubscriptionService;

    @Test
    void testGetTracesBySessionAndWorkspaceRejectsCrossTenantRead() {
        // Seed one session per workspace, then verify getTracesBySessionAndWorkspace is tenant-scoped so a
        // colliding or guessed session id from workspace B cannot surface workspace A's traces.
        AiObservabilitySession sessionA = aiObservabilitySessionService.getOrCreateSession(WORKSPACE_A, null, null);
        AiObservabilitySession sessionB = aiObservabilitySessionService.getOrCreateSession(WORKSPACE_B, null, null);

        AiObservabilityTrace traceA = new AiObservabilityTrace(WORKSPACE_A, AiObservabilityTraceSource.API);
        traceA.setSessionId(sessionA.getId());
        aiObservabilityTraceService.create(traceA);

        AiObservabilityTrace traceB = new AiObservabilityTrace(WORKSPACE_B, AiObservabilityTraceSource.API);
        traceB.setSessionId(sessionB.getId());
        aiObservabilityTraceService.create(traceB);

        List<AiObservabilityTrace> scopedA =
            aiObservabilityTraceService.getTracesBySessionAndWorkspace(sessionA.getId(), WORKSPACE_A);

        assertThat(scopedA)
            .hasSize(1)
            .allSatisfy(t -> assertThat(t.getWorkspaceId()).isEqualTo(WORKSPACE_A));

        List<AiObservabilityTrace> scopedAasB =
            aiObservabilityTraceService.getTracesBySessionAndWorkspace(sessionA.getId(), WORKSPACE_B);

        assertThat(scopedAasB)
            .as("Querying session A's id with workspace B must return no rows — the scoped variant must " +
                "enforce tenant ownership even if the session id is known to workspace B")
            .isEmpty();
    }

    @Test
    void testDeleteOlderThanByWorkspaceDoesNotTouchOtherTenants() {
        // Seed a trace in each workspace. Both are older than the cutoff; the scoped delete must only hit workspace A.
        AiObservabilityTrace traceA = new AiObservabilityTrace(WORKSPACE_A, AiObservabilityTraceSource.API);
        aiObservabilityTraceService.create(traceA);

        AiObservabilityTrace traceB = new AiObservabilityTrace(WORKSPACE_B, AiObservabilityTraceSource.API);
        aiObservabilityTraceService.create(traceB);

        aiObservabilityTraceService.deleteOlderThanByWorkspace(Instant.now(), WORKSPACE_A);

        // Trace in workspace B must still be readable. Prior tests in this class may have left other traces
        // in WORKSPACE_B (there is no @AfterEach cleanup), so assert by id rather than by collection size —
        // the isolation contract is "B's rows survive", not "B has exactly one row".
        List<AiObservabilityTrace> remainingB = aiObservabilityTraceService.getTracesByWorkspace(
            WORKSPACE_B, Instant.now()
                .minus(180, ChronoUnit.DAYS),
            Instant.now()
                .plus(1, ChronoUnit.DAYS));

        assertThat(remainingB)
            .as("Scoped delete on workspace A must not affect workspace B's data")
            .extracting(AiObservabilityTrace::getId)
            .contains(traceB.getId());
        assertThat(remainingB)
            .as("Scoped delete on workspace A must not return any workspace A rows for workspace B")
            .allSatisfy(trace -> assertThat(trace.getWorkspaceId()).isEqualTo(WORKSPACE_B));
    }

    @Test
    void testRequestLogWorkspaceIdCannotBeRestamped() {
        // The hardened setter rejects any retroactive tenant stamp; once a log row is stamped, subsequent setter
        // calls must throw so cross-tenant overwrite is impossible even from internal callers.
        AiGatewayRequestLog log = new AiGatewayRequestLog("req-123", "openai/gpt-4");

        log.setWorkspaceId(WORKSPACE_A);

        // First stamp succeeds; a second stamp — even with the same value — is rejected as a class of bug.
        assertThat(log.getWorkspaceId()).isEqualTo(WORKSPACE_A);

        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class, () -> log.setWorkspaceId(WORKSPACE_B));

        // Workspace ownership must still be the original one after the rejected attempt.
        assertThat(log.getWorkspaceId()).isEqualTo(WORKSPACE_A);
    }

    @Test
    void testDeleteOlderThanByWorkspaceOnRequestLogIsScoped() {
        AiGatewayRequestLog logA = new AiGatewayRequestLog("req-A", "openai/gpt-4");
        logA.setWorkspaceId(WORKSPACE_A);
        logA.setStatus(200);
        logA.setCost(BigDecimal.ZERO);
        aiGatewayRequestLogService.create(logA);

        AiGatewayRequestLog logB = new AiGatewayRequestLog("req-B", "openai/gpt-4");
        logB.setWorkspaceId(WORKSPACE_B);
        logB.setStatus(200);
        logB.setCost(BigDecimal.ZERO);
        aiGatewayRequestLogService.create(logB);

        aiGatewayRequestLogService.deleteOlderThanByWorkspace(Instant.now(), WORKSPACE_A);

        List<AiGatewayRequestLog> remainingB = aiGatewayRequestLogService.getRequestLogsByWorkspace(
            WORKSPACE_B, Instant.now()
                .minus(1, ChronoUnit.DAYS),
            Instant.now()
                .plus(1, ChronoUnit.DAYS));

        assertThat(remainingB)
            .as("Scoped request-log delete on workspace A must not affect workspace B")
            .hasSize(1)
            .allSatisfy(l -> assertThat(l.getWorkspaceId()).isEqualTo(WORKSPACE_B));
    }

    @Test
    void testWebhookSubscriptionsByWorkspaceAreScoped() {
        // Seed one subscription per workspace. A workspace-scoped list query must never see another workspace's row
        // — the review flagged webhook subs as a tenant-isolation gap because the earlier test only covered traces.
        AiObservabilityWebhookSubscription subscriptionA = new AiObservabilityWebhookSubscription(
            WORKSPACE_A, "subA", "https://example.com/a", "[]");
        aiObservabilityWebhookSubscriptionService.create(subscriptionA);

        AiObservabilityWebhookSubscription subscriptionB = new AiObservabilityWebhookSubscription(
            WORKSPACE_B, "subB", "https://example.com/b", "[]");
        aiObservabilityWebhookSubscriptionService.create(subscriptionB);

        List<AiObservabilityWebhookSubscription> aList =
            aiObservabilityWebhookSubscriptionService.getWebhookSubscriptionsByWorkspace(WORKSPACE_A);

        assertThat(aList)
            .hasSize(1)
            .allSatisfy(s -> assertThat(s.getWorkspaceId()).isEqualTo(WORKSPACE_A));

        List<AiObservabilityWebhookSubscription> bList =
            aiObservabilityWebhookSubscriptionService.getWebhookSubscriptionsByWorkspace(WORKSPACE_B);

        assertThat(bList)
            .hasSize(1)
            .allSatisfy(s -> assertThat(s.getWorkspaceId()).isEqualTo(WORKSPACE_B));
    }

    @Test
    void testAlertRulesByWorkspaceAreScoped() {
        // Alert rules bind notification channels; a cross-tenant leak here would let workspace B query workspace A's
        // rules and enumerate channel IDs. Keep this guard visible in the shared suite.
        AiObservabilityAlertRule ruleA = new AiObservabilityAlertRule(
            WORKSPACE_A, "ruleA", AiObservabilityAlertMetric.ERROR_RATE, AiObservabilityAlertCondition.GREATER_THAN,
            BigDecimal.ZERO, 5, 0);
        aiObservabilityAlertRuleService.create(ruleA);

        AiObservabilityAlertRule ruleB = new AiObservabilityAlertRule(
            WORKSPACE_B, "ruleB", AiObservabilityAlertMetric.ERROR_RATE, AiObservabilityAlertCondition.GREATER_THAN,
            BigDecimal.ZERO, 5, 0);
        aiObservabilityAlertRuleService.create(ruleB);

        List<AiObservabilityAlertRule> aList = aiObservabilityAlertRuleService.getAlertRulesByWorkspace(WORKSPACE_A);

        assertThat(aList)
            .hasSize(1)
            .allSatisfy(r -> assertThat(r.getWorkspaceId()).isEqualTo(WORKSPACE_A));
    }
}
