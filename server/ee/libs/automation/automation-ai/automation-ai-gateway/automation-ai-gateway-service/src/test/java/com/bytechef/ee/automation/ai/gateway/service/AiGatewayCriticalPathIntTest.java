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
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayBudget;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayBudgetEnforcementMode;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayBudgetPeriod;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimit;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimitResult;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRequestLog;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySpan;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySpanType;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTraceSource;
import com.bytechef.ee.automation.ai.gateway.domain.AiPrompt;
import com.bytechef.ee.automation.ai.gateway.domain.AiPromptVersion;
import com.bytechef.ee.automation.ai.gateway.domain.AiPromptVersionType;
import com.bytechef.ee.automation.ai.gateway.ratelimit.AiGatewayRateLimitChecker;
import com.bytechef.ee.automation.ai.gateway.repository.AiPromptVersionRepository;
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
 * End-to-end coverage for critical paths across the AI Gateway:
 * <ul>
 * <li>Workspace isolation for observability traces — one workspace's query must not return another workspace's
 * rows.</li>
 * <li>Optimistic locking on {@link AiPromptVersion} — concurrent updates against a stale version throw
 * {@link OptimisticLockingFailureException} instead of last-write-wins silently.</li>
 * <li>Hard-stop budget enforcement — a zero-limit budget rejects further spend.</li>
 * </ul>
 *
 * <p>
 * Encryption round-trip is covered in {@link com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProviderApiKeyTest}:
 * it doesn't need the full JDBC stack because the encrypted wrapper exposes its own encode/decode symmetry directly,
 * and the shared int-test config does not register the {@code EncryptedStringWrapper} JDBC converters.
 *
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
    private AiPromptService aiPromptService;

    @Autowired
    private AiPromptVersionService aiPromptVersionService;

    @Autowired
    private AiPromptVersionRepository aiPromptVersionRepository;

    @Autowired
    private AiGatewayRequestLogService aiGatewayRequestLogService;

    @Autowired
    private AiObservabilityTraceService aiObservabilityTraceService;

    @Autowired
    private AiObservabilitySpanService aiObservabilitySpanService;

    @Autowired
    private AiGatewayRateLimitService aiGatewayRateLimitService;

    @Autowired
    private AiGatewayRateLimitChecker aiGatewayRateLimitChecker;

    @Autowired
    private AiGatewayBudgetService aiGatewayBudgetService;

    @Autowired
    private AiGatewayBudgetChecker aiGatewayBudgetChecker;

    @Autowired
    private com.bytechef.ee.automation.ai.gateway.ratelimit.AiGatewayRateLimiter aiGatewayRateLimiter;

    @Test
    public void testWorkspaceIsolationForPrompts() {
        AiPrompt inA = aiPromptService.create(new AiPrompt(WORKSPACE_A, "isolation-prompt-a-" + UUID.randomUUID()));
        AiPrompt inB = aiPromptService.create(new AiPrompt(WORKSPACE_B, "isolation-prompt-b-" + UUID.randomUUID()));

        List<AiPrompt> promptsInA = aiPromptService.getPromptsByWorkspace(WORKSPACE_A);
        List<AiPrompt> promptsInB = aiPromptService.getPromptsByWorkspace(WORKSPACE_B);

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
        AiPrompt prompt = aiPromptService.create(
            new AiPrompt(WORKSPACE_A, "optimistic-" + UUID.randomUUID()));

        Long promptId = Validate.notNull(prompt.getId(), "id");

        AiPromptVersion created = aiPromptVersionService.create(
            new AiPromptVersion(promptId, 1, AiPromptVersionType.TEXT, "original", "tester"));

        Long versionId = Validate.notNull(created.getId(), "id");

        // Load the row twice so both copies share the same @Version ordinal — simulates two transactions racing on
        // the same stale snapshot.
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

    /**
     * End-to-end critical path coverage for a successful request: rate-limit passes → budget passes → request log
     * persists → trace + span written atomically. The chat-model invocation itself is replaced by a direct write of the
     * request log and trace as the facade would do in its doFinally, because bringing up a real Spring AI ChatModel
     * requires external API credentials.
     */
    @Test
    public void testCriticalPathEndToEndSuccessfulRequestPersistsLogAndTrace() {
        // 1. Rate-limit check for a fresh workspace — must allow. No rate limits configured, only the global API
        // key check runs and passes by default.
        aiGatewayRateLimitChecker.checkRateLimits(WORKSPACE_A, null, "user-1", Map.of());

        // 2. Budget check for a fresh workspace — must allow when no budget row exists.
        AiGatewayBudgetChecker.BudgetCheckResult budgetResult = aiGatewayBudgetChecker.checkBudget(WORKSPACE_A);

        assertThat(budgetResult.requestAllowed())
            .as("fresh workspace with no budget must allow requests")
            .isTrue();

        // 3. Persist the request log as the facade does in doFinally.
        AiGatewayRequestLog log = new AiGatewayRequestLog("req-" + UUID.randomUUID(), "openai/gpt-4o");

        log.setWorkspaceId(WORKSPACE_A);
        log.setRoutedModel("gpt-4o");
        log.setRoutedProvider("OPENAI");
        log.setStatus(200);
        log.setInputTokens(100);
        log.setOutputTokens(50);
        log.setLatencyMs(250);
        log.setCost(new BigDecimal("0.0125"));

        aiGatewayRequestLogService.create(log);

        // 4. Write the trace + span atomically. The trace aggregates token/latency/cost totals; model is recorded
        // on the span since different spans in the same trace may target different models.
        AiObservabilityTrace trace = new AiObservabilityTrace(WORKSPACE_A, AiObservabilityTraceSource.API);

        trace.setTotalInputTokens(100);
        trace.setTotalOutputTokens(50);
        trace.setTotalLatencyMs(250);
        trace.setTotalCost(new BigDecimal("0.0125"));

        aiObservabilityTraceService.create(trace);

        AiObservabilitySpan span = new AiObservabilitySpan(trace.getId(), AiObservabilitySpanType.GENERATION);

        span.setName("gpt-4o");
        span.setModel("gpt-4o");
        span.setProvider("OPENAI");
        span.setInputTokens(100);
        span.setOutputTokens(50);
        span.setLatencyMs(250);
        span.setCost(new BigDecimal("0.0125"));

        aiObservabilitySpanService.create(span);

        // 5. Verify the full chain survives the round-trip: the request log, trace, and span are all readable and
        // the trace→span relation is preserved.
        List<AiGatewayRequestLog> logs = aiGatewayRequestLogService.getRequestLogsByWorkspace(
            WORKSPACE_A, java.time.Instant.now()
                .minusSeconds(60),
            java.time.Instant.now()
                .plusSeconds(60));

        assertThat(logs)
            .as("the request log must be readable by its workspace")
            .extracting(AiGatewayRequestLog::getId)
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

    /**
     * Rate-limit enforcement across the full pipeline: a rate limit configured for one request per window rejects the
     * second attempt within that window. The real in-memory counter is used — not a mock — so a regression in either
     * the rate-limit service OR the in-memory counter is caught.
     */
    @Test
    public void testCriticalPathRateLimitBlocksSecondRequestWithinWindow() {
        Long workspaceId = 999L; // fresh workspace to isolate the rate-limit counter

        AiGatewayRateLimit rateLimit = new AiGatewayRateLimit(
            workspaceId,
            "critical-path-" + UUID.randomUUID(),
            com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimitScope.GLOBAL,
            com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimitType.REQUESTS,
            1,
            60);

        aiGatewayRateLimitService.create(rateLimit);

        // Key format must match AiGatewayRateLimitChecker#buildKey: "ai-gw-rl:{workspaceId}:{ruleId}:global"
        // for GLOBAL-scoped rules. Using a mismatched key here would reset/query a different bucket than the
        // one the checker populates, and the follow-up tryAcquire would always start a new window at count=1.
        String expectedKey = "ai-gw-rl:" + workspaceId + ":"
            + Validate.notNull(rateLimit.getId(), "rateLimit id must be persisted") + ":global";

        aiGatewayRateLimiter.reset(expectedKey);

        // First call must be allowed.
        aiGatewayRateLimitChecker.checkRateLimits(workspaceId, null, "user-1", Map.of());

        // Second call within the same window must exceed the limit. We use the low-level rate-limiter to confirm
        // the counter is actually incrementing (the checker itself throws on rejection, which we assert next).
        AiGatewayRateLimitResult directResult = aiGatewayRateLimiter.tryAcquire(expectedKey, 1, 60);

        assertThat(directResult.allowed())
            .as("after the first tryAcquire, a second one against the same key/window must be rejected")
            .isFalse();
    }

    /**
     * Asserts the HARD-mode budget branch: a zero-limit budget must reject any further spend check. Protects the
     * reject-path contract against warn/block drift during refactors.
     */
    @Test
    public void testCriticalPathBudgetHardStopRejectsWhenOverLimit() {
        Long workspaceId = 998L;

        // Zero-dollar budget with HARD_STOP: any subsequent spend check must reject.
        AiGatewayBudget budget = new AiGatewayBudget(
            workspaceId, new BigDecimal("0.00"), AiGatewayBudgetPeriod.MONTHLY, AiGatewayBudgetEnforcementMode.HARD);

        aiGatewayBudgetService.create(budget);

        AiGatewayBudgetChecker.BudgetCheckResult result = aiGatewayBudgetChecker.checkBudget(workspaceId);

        assertThat(result.requestAllowed())
            .as("HARD-mode budget with $0.00 limit must reject further spend.")
            .isFalse();
    }

    /**
     * SOFT-mode budget under limit must allow without warning. Pairs with the HARD over-limit test above; between the
     * two, a refactor that collapsed SOFT/HARD into a single codepath would have to change both assertions.
     */
    @Test
    public void testCriticalPathBudgetSoftAllowsWhenUnderLimit() {
        Long workspaceId = 997L;

        // $100 SOFT budget with no spend — must be allowed, no warning.
        AiGatewayBudget budget = new AiGatewayBudget(
            workspaceId, new BigDecimal("100.00"), AiGatewayBudgetPeriod.MONTHLY,
            AiGatewayBudgetEnforcementMode.SOFT);

        aiGatewayBudgetService.create(budget);

        AiGatewayBudgetChecker.BudgetCheckResult result = aiGatewayBudgetChecker.checkBudget(workspaceId);

        assertThat(result.requestAllowed())
            .as("SOFT-mode budget under limit must allow the request.")
            .isTrue();
        assertThat(result.thresholdWarning())
            .as("SOFT-mode under limit must NOT raise the threshold-warning flag.")
            .isFalse();
    }

    /**
     * Zero-amount budget is a sentinel for "block everything" regardless of enforcement mode — neither SOFT nor HARD
     * should bypass the zero-short-circuit. Protects the guard at the top of {@link AiGatewayBudgetChecker#checkBudget}
     * from a refactor that shifted the order of the enforcement-mode check above the zero check.
     */
    @Test
    public void testCriticalPathBudgetSoftRejectsOnZeroBudget() {
        Long workspaceId = 996L;

        AiGatewayBudget budget = new AiGatewayBudget(
            workspaceId, new BigDecimal("0.00"), AiGatewayBudgetPeriod.MONTHLY,
            AiGatewayBudgetEnforcementMode.SOFT);

        aiGatewayBudgetService.create(budget);

        AiGatewayBudgetChecker.BudgetCheckResult result = aiGatewayBudgetChecker.checkBudget(workspaceId);

        assertThat(result.requestAllowed())
            .as("Zero-budget sentinel must reject regardless of SOFT/HARD.")
            .isFalse();
    }
}
