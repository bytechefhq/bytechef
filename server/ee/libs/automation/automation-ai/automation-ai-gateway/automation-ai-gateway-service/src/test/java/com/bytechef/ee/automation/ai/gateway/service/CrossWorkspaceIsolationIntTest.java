/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.ai.gateway.config.AiGatewayIntTestConfiguration;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityAlertRuleService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilitySessionService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityWebhookSubscriptionService;
import com.bytechef.ee.platform.ai.llm.usage.AiLlmUsage;
import com.bytechef.ee.platform.ai.llm.usage.service.AiLlmUsageService;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertCondition;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertMetric;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertRule;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySession;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityWebhookSubscription;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityAlertRuleService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilitySessionService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityWebhookSubscriptionService;
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
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiGatewayIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@AiGatewayIntTestConfigurationSharedMocks
public class CrossWorkspaceIsolationIntTest {

    private static final Long WORKSPACE_A = 101L;
    private static final Long WORKSPACE_B = 202L;

    @Autowired
    private AiLlmUsageService aiGatewayRequestLogService;

    @Autowired
    private WorkspaceAiObservabilityAlertRuleService workspaceAiObservabilityAlertRuleService;

    @Autowired
    private WorkspaceAiObservabilityWebhookSubscriptionService workspaceAiObservabilityWebhookSubscriptionService;

    @Autowired
    private WorkspaceAiObservabilitySessionService workspaceAiObservabilitySessionService;

    @Autowired
    private WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService;

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
        AiObservabilitySession sessionA =
            workspaceAiObservabilitySessionService.getOrCreateSession(WORKSPACE_A, null, null);
        AiObservabilitySession sessionB =
            workspaceAiObservabilitySessionService.getOrCreateSession(WORKSPACE_B, null, null);

        AiObservabilityTrace traceA = new AiObservabilityTrace(AiObservabilityTraceSource.API);
        traceA.setSessionId(sessionA.getId());
        workspaceAiObservabilityTraceService.createInWorkspace(traceA, WORKSPACE_A);

        AiObservabilityTrace traceB = new AiObservabilityTrace(AiObservabilityTraceSource.API);
        traceB.setSessionId(sessionB.getId());
        workspaceAiObservabilityTraceService.createInWorkspace(traceB, WORKSPACE_B);

        List<AiObservabilityTrace> scopedA =
            workspaceAiObservabilityTraceService.getTracesBySessionAndWorkspace(sessionA.getId(), WORKSPACE_A);

        assertThat(scopedA)
            .hasSize(1)
            .allSatisfy(
                t -> assertThat(workspaceAiObservabilityTraceService.getWorkspaceId(t.getId())).isEqualTo(WORKSPACE_A));

        List<AiObservabilityTrace> scopedAasB =
            workspaceAiObservabilityTraceService.getTracesBySessionAndWorkspace(sessionA.getId(), WORKSPACE_B);

        assertThat(scopedAasB)
            .as("Querying session A's id with workspace B must return no rows — the scoped variant must " +
                "enforce tenant ownership even if the session id is known to workspace B")
            .isEmpty();
    }

    @Test
    void testDeleteOlderThanByWorkspaceDoesNotTouchOtherTenants() {
        AiObservabilityTrace traceA = new AiObservabilityTrace(AiObservabilityTraceSource.API);
        workspaceAiObservabilityTraceService.createInWorkspace(traceA, WORKSPACE_A);

        AiObservabilityTrace traceB = new AiObservabilityTrace(AiObservabilityTraceSource.API);
        workspaceAiObservabilityTraceService.createInWorkspace(traceB, WORKSPACE_B);

        workspaceAiObservabilityTraceService.deleteOlderThanByWorkspace(Instant.now(), WORKSPACE_A);

        List<AiObservabilityTrace> remainingB = workspaceAiObservabilityTraceService.getTracesByWorkspace(
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
            .allSatisfy(
                trace -> assertThat(workspaceAiObservabilityTraceService.getWorkspaceId(trace.getId()))
                    .isEqualTo(WORKSPACE_B));
    }

    @Test
    void testDeleteOlderThanByWorkspaceOnRequestLogIsScoped() {
        AiLlmUsage logA = new AiLlmUsage("req-A", "openai/gpt-4");
        logA.setStatus(200);
        logA.setCost(BigDecimal.ZERO);
        aiGatewayRequestLogService.create(logA, WORKSPACE_A);

        AiLlmUsage logB = new AiLlmUsage("req-B", "openai/gpt-4");
        logB.setStatus(200);
        logB.setCost(BigDecimal.ZERO);
        aiGatewayRequestLogService.create(logB, WORKSPACE_B);

        aiGatewayRequestLogService.deleteOlderThanByWorkspace(Instant.now(), WORKSPACE_A);

        List<AiLlmUsage> remainingB = aiGatewayRequestLogService.getRequestLogsByWorkspace(
            WORKSPACE_B, Instant.now()
                .minus(1, ChronoUnit.DAYS),
            Instant.now()
                .plus(1, ChronoUnit.DAYS));

        assertThat(remainingB)
            .as("Scoped request-log delete on workspace A must not affect workspace B")
            .hasSize(1);
    }

    @Test
    void testWebhookSubscriptionsByWorkspaceAreScoped() {
        AiObservabilityWebhookSubscription subscriptionA = new AiObservabilityWebhookSubscription(
            "subA", "https://example.com/a", "[]");
        workspaceAiObservabilityWebhookSubscriptionService.createInWorkspace(subscriptionA, WORKSPACE_A);

        AiObservabilityWebhookSubscription subscriptionB = new AiObservabilityWebhookSubscription(
            "subB", "https://example.com/b", "[]");
        workspaceAiObservabilityWebhookSubscriptionService.createInWorkspace(subscriptionB, WORKSPACE_B);

        List<AiObservabilityWebhookSubscription> aList =
            workspaceAiObservabilityWebhookSubscriptionService.getWebhookSubscriptionsByWorkspace(WORKSPACE_A);

        assertThat(aList).hasSize(1);

        List<AiObservabilityWebhookSubscription> bList =
            workspaceAiObservabilityWebhookSubscriptionService.getWebhookSubscriptionsByWorkspace(WORKSPACE_B);

        assertThat(bList).hasSize(1);
    }

    @Test
    void testAlertRulesByWorkspaceAreScoped() {
        AiObservabilityAlertRule ruleA = new AiObservabilityAlertRule(
            "ruleA", AiObservabilityAlertMetric.ERROR_RATE, AiObservabilityAlertCondition.GREATER_THAN,
            BigDecimal.ZERO, 5, 0);
        workspaceAiObservabilityAlertRuleService.createInWorkspace(ruleA, WORKSPACE_A);

        AiObservabilityAlertRule ruleB = new AiObservabilityAlertRule(
            "ruleB", AiObservabilityAlertMetric.ERROR_RATE, AiObservabilityAlertCondition.GREATER_THAN,
            BigDecimal.ZERO, 5, 0);
        workspaceAiObservabilityAlertRuleService.createInWorkspace(ruleB, WORKSPACE_B);

        List<AiObservabilityAlertRule> aList =
            workspaceAiObservabilityAlertRuleService.getAlertRulesByWorkspace(WORKSPACE_A);

        assertThat(aList).hasSize(1);
    }
}
