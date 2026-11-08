/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.automation.ai.gateway.budget.AiGatewayBudgetChecker;
import com.bytechef.ee.automation.ai.gateway.config.AiGatewayIntTestConfiguration;
import com.bytechef.ee.automation.ai.gateway.ratelimit.AiGatewayRateLimitChecker;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilitySessionService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.automation.ai.prompt.service.WorkspaceAiPromptService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayBudget;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayBudgetEnforcementMode;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayBudgetPeriod;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimit;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimitResult;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayBudgetService;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayRateLimitService;
import com.bytechef.ee.platform.ai.llm.usage.AiLlmUsage;
import com.bytechef.ee.platform.ai.llm.usage.service.AiLlmUsageService;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpan;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpanType;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilitySpanService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import com.bytechef.ee.platform.ai.prompt.AiPrompt;
import com.bytechef.ee.platform.ai.prompt.AiPromptVersion;
import com.bytechef.ee.platform.ai.prompt.AiPromptVersionService;
import com.bytechef.ee.platform.ai.prompt.AiPromptVersionType;
import com.bytechef.ee.platform.ai.prompt.repository.AiPromptVersionRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Ivica Cardic
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiGatewayIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@AiGatewayIntTestConfigurationSharedMocks
public class AiGatewayCriticalPathIntTest {

    private static final Long WORKSPACE_A = 1L;
    private static final Long WORKSPACE_B = 2L;

    @Autowired
    private WorkspaceAiPromptService workspaceAiPromptService;

    @Autowired
    private WorkspaceAiObservabilitySessionService workspaceAiObservabilitySessionService;

    @Autowired
    private WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService;

    @Autowired
    private AiPromptVersionService aiPromptVersionService;

    @Autowired
    private AiPromptVersionRepository aiPromptVersionRepository;

    @Autowired
    private AiLlmUsageService aiGatewayRequestLogService;

    @Autowired
    private AiObservabilityTraceService aiObservabilityTraceService;

    @Autowired
    private AiObservabilitySpanService aiObservabilitySpanService;

    @Autowired
    private AiGatewayRateLimitService aiGatewayRateLimitService;

    @Autowired
    private WorkspaceAiGatewayRateLimitService workspaceAiGatewayRateLimitService;

    @Autowired
    private AiGatewayRateLimitChecker aiGatewayRateLimitChecker;

    @Autowired
    private AiGatewayBudgetService aiGatewayBudgetService;

    @Autowired
    private WorkspaceAiGatewayBudgetService workspaceAiGatewayBudgetService;

    @Autowired
    private AiGatewayBudgetChecker aiGatewayBudgetChecker;

    @Autowired
    private com.bytechef.ee.platform.ai.gateway.ratelimit.AiGatewayRateLimiter aiGatewayRateLimiter;

    @Test
    public void testWorkspaceIsolationForPrompts() {
        AiPrompt inA = workspaceAiPromptService.createInWorkspace(
            new AiPrompt("isolation-prompt-a-" + UUID.randomUUID()), WORKSPACE_A);
        AiPrompt inB = workspaceAiPromptService.createInWorkspace(
            new AiPrompt("isolation-prompt-b-" + UUID.randomUUID()), WORKSPACE_B);

        List<AiPrompt> promptsInA = workspaceAiPromptService.getPromptsByWorkspace(WORKSPACE_A);
        List<AiPrompt> promptsInB = workspaceAiPromptService.getPromptsByWorkspace(WORKSPACE_B);

        assertThat(promptsInA)
            .as("workspace A query must not return workspace B's prompt")
            .extracting(AiPrompt::getId)
            .contains(inA.getId())
            .doesNotContain(inB.getId());

        assertThat(promptsInB)
            .as("workspace B query must not return workspace A's prompt")
            .extracting(AiPrompt::getId)
            .contains(inB.getId())
            .doesNotContain(inA.getId());
    }

    @Test
    public void testAiPromptVersionOptimisticLock() {
        AiPrompt prompt = workspaceAiPromptService.createInWorkspace(
            new AiPrompt("optimistic-" + UUID.randomUUID()), WORKSPACE_A);

        Long promptId = Validate.notNull(prompt.getId(), "id");

        AiPromptVersion created = aiPromptVersionService.create(
            new AiPromptVersion(promptId, 1, AiPromptVersionType.TEXT, "original", "tester"));

        Long versionId = Validate.notNull(created.getId(), "id");

        AiPromptVersion staleCopyA = aiPromptVersionRepository.findById(versionId)
            .orElseThrow();
        AiPromptVersion staleCopyB = aiPromptVersionRepository.findById(versionId)
            .orElseThrow();

        staleCopyA.setContent("winner writes first");

        AiPromptVersion winner = aiPromptVersionService.update(staleCopyA);

        assertThat(winner.getContent()).isEqualTo("winner writes first");

        staleCopyB.setContent("loser tries to write over stale version");

        assertThatThrownBy(() -> aiPromptVersionService.update(staleCopyB))
            .as("second update against the stale snapshot must fail with an optimistic-lock exception")
            .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    public void testCriticalPathEndToEndSuccessfulRequestPersistsLogAndTrace() {
        aiGatewayRateLimitChecker.checkRateLimits(WORKSPACE_A, null, "user-1", Map.of());

        AiGatewayBudgetChecker.BudgetCheckResult budgetResult = aiGatewayBudgetChecker.checkBudget(WORKSPACE_A);

        assertThat(budgetResult.requestAllowed())
            .as("fresh workspace with no budget must allow requests")
            .isTrue();

        AiLlmUsage log = new AiLlmUsage("req-" + UUID.randomUUID(), "openai/gpt-4o");

        log.setRoutedModel("gpt-4o");
        log.setRoutedProvider("OPENAI");
        log.setStatus(200);
        log.setInputTokens(100);
        log.setOutputTokens(50);
        log.setLatencyMs(250);
        log.setCost(new BigDecimal("0.0125"));

        aiGatewayRequestLogService.create(log, WORKSPACE_A);

        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.API);

        trace.setTotalInputTokens(100);
        trace.setTotalOutputTokens(50);
        trace.setTotalLatencyMs(250);
        trace.setTotalCost(new BigDecimal("0.0125"));

        workspaceAiObservabilityTraceService.createInWorkspace(trace, WORKSPACE_A);

        AiObservabilitySpan span = new AiObservabilitySpan(trace.getId(), AiObservabilitySpanType.GENERATION);

        span.setName("gpt-4o");
        span.setModel("gpt-4o");
        span.setProvider("OPENAI");
        span.setInputTokens(100);
        span.setOutputTokens(50);
        span.setLatencyMs(250);
        span.setCost(new BigDecimal("0.0125"));

        aiObservabilitySpanService.create(span);

        List<AiLlmUsage> logs = aiGatewayRequestLogService.getRequestLogsByWorkspace(
            WORKSPACE_A, java.time.Instant.now()
                .minusSeconds(60),
            java.time.Instant.now()
                .plusSeconds(60));

        assertThat(logs)
            .as("the request log must be readable by its workspace")
            .extracting(AiLlmUsage::getId)
            .contains(log.getId());

        List<AiObservabilitySpan> spans = aiObservabilitySpanService.getSpansByTrace(trace.getId());

        assertThat(spans)
            .as("trace→span relation must survive the persistence round-trip")
            .hasSize(1)
            .first()
            .satisfies(retrievedSpan -> {
                assertThat(retrievedSpan.getModel()).isEqualTo("gpt-4o");
                assertThat(retrievedSpan.getTraceId()).isEqualTo(trace.getId());
            });
    }

    @Test
    public void testCriticalPathRateLimitBlocksSecondRequestWithinWindow() {
        Long workspaceId = 999L;

        AiGatewayRateLimit rateLimit = new AiGatewayRateLimit(
            "critical-path-" + UUID.randomUUID(),
            com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimitScope.GLOBAL,
            com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimitType.REQUESTS,
            1,
            60);

        workspaceAiGatewayRateLimitService.createInWorkspace(rateLimit, workspaceId);

        String expectedKey = "ai-gw-rl:" + workspaceId + ":"
            + Validate.notNull(rateLimit.getId(), "rateLimit id must be persisted") + ":global";

        aiGatewayRateLimiter.reset(expectedKey);

        aiGatewayRateLimitChecker.checkRateLimits(workspaceId, null, "user-1", Map.of());

        AiGatewayRateLimitResult directResult = aiGatewayRateLimiter.tryAcquire(expectedKey, 1, 60);

        assertThat(directResult.allowed())
            .as("after the first tryAcquire, a second one against the same key/window must be rejected")
            .isFalse();
    }

    @Test
    public void testCriticalPathBudgetHardStopRejectsWhenOverLimit() {
        Long workspaceId = 998L;

        AiGatewayBudget budget = new AiGatewayBudget(
            new BigDecimal("0.00"), AiGatewayBudgetPeriod.MONTHLY, AiGatewayBudgetEnforcementMode.HARD);

        workspaceAiGatewayBudgetService.createInWorkspace(budget, workspaceId);

        AiGatewayBudgetChecker.BudgetCheckResult result = aiGatewayBudgetChecker.checkBudget(workspaceId);

        assertThat(result.requestAllowed())
            .as("HARD-mode budget with $0.00 limit must reject further spend.")
            .isFalse();
    }

    @Test
    public void testCriticalPathBudgetSoftAllowsWhenUnderLimit() {
        Long workspaceId = 997L;

        AiGatewayBudget budget = new AiGatewayBudget(
            new BigDecimal("100.00"), AiGatewayBudgetPeriod.MONTHLY,
            AiGatewayBudgetEnforcementMode.SOFT);

        workspaceAiGatewayBudgetService.createInWorkspace(budget, workspaceId);

        AiGatewayBudgetChecker.BudgetCheckResult result = aiGatewayBudgetChecker.checkBudget(workspaceId);

        assertThat(result.requestAllowed())
            .as("SOFT-mode budget under limit must allow the request.")
            .isTrue();
        assertThat(result.thresholdWarning())
            .as("SOFT-mode under limit must NOT raise the threshold-warning flag.")
            .isFalse();
    }

    @Test
    public void testCriticalPathBudgetSoftRejectsOnZeroBudget() {
        Long workspaceId = 996L;

        AiGatewayBudget budget = new AiGatewayBudget(
            new BigDecimal("0.00"), AiGatewayBudgetPeriod.MONTHLY,
            AiGatewayBudgetEnforcementMode.SOFT);

        workspaceAiGatewayBudgetService.createInWorkspace(budget, workspaceId);

        AiGatewayBudgetChecker.BudgetCheckResult result = aiGatewayBudgetChecker.checkBudget(workspaceId);

        assertThat(result.requestAllowed())
            .as("Zero-budget sentinel must reject regardless of SOFT/HARD.")
            .isFalse();
    }
}
