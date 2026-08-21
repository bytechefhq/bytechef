# AI Gateway — Experiment Executor Resilience Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the three concrete pain points with the current `@Async` experiment executor — lack of cancellation, no retry on transient gateway failures, and orphaned runs after JVM restart — using targeted small fixes rather than migrating to atlas-coordinator.

**Architecture:** All three fixes stay inside the existing `automation-ai-gateway-experiment-service` module; no new modules, no atlas wiring. Cancellation adds a `stop_requested` column to `ai_experiment` and a check at the top of each executor loop iteration. Retry wraps the `AiGatewayFacade.chatCompletion(...)` call in a Spring `RetryTemplate`. Orphan recovery runs on `ApplicationReadyEvent`, scans for stale `RUNNING` rows, and marks them `FAILED` with a diagnostic message.

**Tech Stack:** Java 25 · Spring Boot 4 · Spring Retry · Spring Data JDBC · Liquibase · Testcontainers

**Rationale for choosing this over atlas-coordinator migration:** See the "Why not atlas" discussion at the end of this document. Short version: atlas is a ~5x bigger scope, solves theoretical horizontal-scale problems ByteChef doesn't have (LLM replay is provider-bound, not CPU-bound), and the 3 real operational gains — restart durability, retry, and cancellation — are addressable in ~100 lines apiece.

**Depends on:** Spec C Phases 1–4 (current branch state). No other prerequisites.

---

## Task 1 — Cancellation

**Files:**
- Modify: `.../automation-ai-gateway-experiment-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/` — new migration `00000000000009_ai_experiment_stop_flag.xml`
- Modify: `.../automation-ai-gateway-experiment-api/.../domain/AiExperiment.java` — add `stopRequested` field + `stoppedDate` + `requestStop()` helper
- Modify: `.../automation-ai-gateway-experiment-api/.../service/AiExperimentService.java` — add `requestStop(long id)`
- Modify: `.../automation-ai-gateway-experiment-service/.../service/AiExperimentServiceImpl.java` — impl
- Modify: `.../automation-ai-gateway-experiment-service/.../executor/AiExperimentExecutor.java` — check `stopRequested` at top of each iteration
- Modify: `.../automation-ai-gateway-experiment-public-rest/.../public_/web/rest/AiExperimentController.java` — add `POST /{id}/stop`
- Modify: `.../automation-ai-gateway-experiment-remote-client/.../RemoteAiExperimentServiceClient.java` — add `requestStop` stub
- Test: update `AiExperimentExecutorTest` + `AiExperimentControllerTest`

### Step 1.1: Liquibase migration

Create `00000000000009_ai_experiment_stop_flag.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="20260423000008" author="ivicac">
        <addColumn tableName="ai_experiment">
            <column name="stop_requested" type="BOOLEAN" defaultValueBoolean="false">
                <constraints nullable="false"/>
            </column>
            <column name="stopped_date" type="DATETIME"/>
        </addColumn>
    </changeSet>
</databaseChangeLog>
```

NO `context="ee"`. Matches sibling convention.

### Step 1.2: Extend `AiExperiment.java`

Add fields (getters + setters in alphabetical order):

```java
@Column("stop_requested")
private boolean stopRequested;

@Column("stopped_date")
private Instant stoppedDate;
```

Add a convenience method (keeps the two fields in sync):

```java
public void requestStop() {
    this.stopRequested = true;
    this.stoppedDate = Instant.now();
}
```

### Step 1.3: Extend service

`AiExperimentService`:

```java
/**
 * Flags the experiment to stop after its next iteration boundary. Caller-visible status stays RUNNING
 * until the executor's next check flips it to FAILED with "cancelled by user".
 */
AiExperiment requestStop(long experimentId);
```

Impl: fetch, `experiment.requestStop()`, save, return.

### Step 1.4: Executor check

In `AiExperimentExecutor.execute(...)`, inside the per-item loop, add as the **first statement** of the loop body:

```java
// Re-fetch so we see the user's stop request
AiExperiment fresh = aiExperimentService.getExperiment(experimentId);

if (fresh.isStopRequested()) {
    logger.info("Experiment {} was stopped by user request after {}/{} runs", experimentId, index, runs.size());

    aiExperimentRunService.fail(runs.get(index).getId(), "Cancelled by user before run started");

    // Mark any remaining runs as cancelled
    for (int remaining = index + 1; remaining < runs.size(); remaining++) {
        aiExperimentRunService.fail(runs.get(remaining).getId(), "Cancelled — experiment stopped by user");
    }

    anyFailed = true;

    break;
}
```

The per-iteration `getExperiment` adds one query per item — acceptable for experiment sizes (tens to hundreds of items). If experiment sizes grow to thousands, we can batch by only re-checking every N iterations.

### Step 1.5: REST endpoint

Add to `AiExperimentController`:

```java
@PostMapping("/{id}/stop")
public ResponseEntity<Object> stop(
    @RequestHeader(name = "X-ByteChef-Workspace-Id", required = false) String workspaceHeader,
    @PathVariable("id") Long id) {

    Long workspaceId = parseWorkspaceId(workspaceHeader);

    if (workspaceId == null) {
        return missingWorkspaceResponse();
    }

    AiExperiment experiment = aiExperimentService.getExperiment(id);

    assertSameWorkspace(workspaceId, experiment);

    AiExperiment stopped = aiExperimentService.requestStop(id);

    return ResponseEntity.accepted()
        .body(Map.of("id", stopped.getId(), "stopRequested", true));
}
```

Returns `202 Accepted` — stop is best-effort, takes effect at the next iteration boundary.

### Step 1.6: Controller test

Add to `AiExperimentControllerTest`:

```java
@Test
void testStopReturns202() throws Exception {
    AiExperiment running = new AiExperiment(42L, 7L);
    ReflectionTestUtils.setField(running, "id", 123L);

    when(aiExperimentService.getExperiment(123L)).thenReturn(running);
    when(aiExperimentService.requestStop(123L)).thenReturn(running);

    mockMvc.perform(post("/api/ai-gateway/v1/experiments/123/stop")
        .header("X-ByteChef-Workspace-Id", "42"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.stopRequested").value(true));
}
```

### Step 1.7: Executor test

Append to `AiExperimentExecutorTest`:

```java
@Test
void testExecuteHonorsStopRequest() {
    // Seed 3 items + pre-create 3 runs. After the first successful run, make the stop_requested check return true.
    // Assert: run[0] = COMPLETED, run[1] + run[2] = FAILED ("Cancelled..."), experiment = FAILED.
}
```

### Step 1.8: Commit

Single commit for this task:

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-experiment/
git commit -m "$(cat <<'EOF'
Add experiment cancellation — POST /experiments/{id}/stop

Stop is best-effort: flag checked at each iteration boundary. Remaining
runs get failed with a diagnostic message. Single stop_requested column
+ stopped_date on ai_experiment.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2 — Per-Run Retry

**Files:**
- Modify: `.../automation-ai-gateway-experiment-service/build.gradle.kts` — add `spring-retry` dep
- Modify: `.../automation-ai-gateway-experiment-service/.../executor/AiExperimentExecutor.java` — wrap `chatCompletion` call in `RetryTemplate`
- Test: update `AiExperimentExecutorTest` with a "retry succeeds on second attempt" case

### Step 2.1: Add dependency

```kotlin
implementation("org.springframework.retry:spring-retry")
```

No version — inherited from the parent BOM. Spring Retry is already transitively available in many Spring Boot contexts; verify by checking another EE module that uses `@Retryable` or `RetryTemplate`. If not found, add the dep explicitly.

### Step 2.2: Inject `RetryTemplate`

Add a field + constructor param to `AiExperimentExecutor`:

```java
private final RetryTemplate chatCompletionRetryTemplate;
```

Build the template in a `@Configuration` class (or as a constructor-arg default) with sensible defaults: 3 attempts, exponential backoff (250ms → 500ms → 1000ms), retry only on specific exceptions.

Create `.../experiment/config/AiExperimentRetryConfiguration.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.experiment.config;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * @version ee
 */
@Configuration
public class AiExperimentRetryConfiguration {

    @Bean
    public RetryTemplate chatCompletionRetryTemplate() {
        RetryTemplate template = new RetryTemplate();

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(250L);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(2_000L);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, Map.of(
            IOException.class, true,
            SocketTimeoutException.class, true,
            RuntimeException.class, false /* don't retry on NPE / IllegalStateException / IllegalArgument */));

        template.setBackOffPolicy(backOffPolicy);
        template.setRetryPolicy(retryPolicy);

        return template;
    }
}
```

The `false` entry for `RuntimeException` is important — don't retry on programmer errors (NPE) or validation failures (IAE). Only network-adjacent throwables retry.

### Step 2.3: Wrap the call

In `AiExperimentExecutor.execute(...)`, where the current code does:

```java
aiGatewayFacade.chatCompletion(request, tracingHeaders, null);
```

...wrap with:

```java
chatCompletionRetryTemplate.execute(context -> {
    aiGatewayFacade.chatCompletion(request, tracingHeaders, null);
    return null;
});
```

The template rethrows the last exception if all attempts fail — existing try/catch catches it and marks the run FAILED.

### Step 2.4: Tests

Update `AiExperimentExecutorTest` to inject a mock `RetryTemplate` that just passes through. Add one new test where the stubbed `chatCompletion` throws `IOException` on first call + succeeds on second — assert the run completes successfully.

### Step 2.5: Commit

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-experiment/
git commit -m "$(cat <<'EOF'
Retry chatCompletion on transient failures during experiment replay

3 attempts with exponential backoff (250ms → 500ms → 1000ms). Retries
only on IOException / SocketTimeoutException — programmer errors and
validation failures fail fast.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3 — Orphan Recovery on Startup

**Files:**
- Create: `.../automation-ai-gateway-experiment-service/.../startup/AiExperimentOrphanRecoveryRunner.java`
- Test: `.../automation-ai-gateway-experiment-service/src/test/java/.../startup/AiExperimentOrphanRecoveryRunnerTest.java`

### Step 3.1: The runner

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.experiment.startup;

/* imports */

/**
 * On application startup, finds experiments stuck in {@code RUNNING} for more than
 * {@link #ORPHAN_THRESHOLD_MINUTES} minutes (presumably because the previous JVM crashed mid-experiment) and
 * marks them + their unfinished runs as {@code FAILED} with an orphan-recovery message. Experiments remain
 * queryable; the user can inspect partial results and re-trigger.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiExperimentOrphanRecoveryRunner {

    private static final Logger logger = LoggerFactory.getLogger(AiExperimentOrphanRecoveryRunner.class);

    /** How long a RUNNING experiment must be stale before we consider it orphaned. */
    static final int ORPHAN_THRESHOLD_MINUTES = 10;

    private final AiExperimentService aiExperimentService;
    private final AiExperimentRunService aiExperimentRunService;

    public AiExperimentOrphanRecoveryRunner(
        AiExperimentService aiExperimentService, AiExperimentRunService aiExperimentRunService) {

        this.aiExperimentService = aiExperimentService;
        this.aiExperimentRunService = aiExperimentRunService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverOrphanedExperiments() {
        Instant threshold = Instant.now()
            .minusSeconds(ORPHAN_THRESHOLD_MINUTES * 60L);

        List<AiExperiment> orphans = aiExperimentService.findRunningOlderThan(threshold);

        if (orphans.isEmpty()) {
            logger.debug("No orphaned experiments found");

            return;
        }

        logger.warn("Recovering {} orphaned experiment(s) after startup", orphans.size());

        for (AiExperiment orphan : orphans) {
            recoverOne(orphan);
        }
    }

    private void recoverOne(AiExperiment experiment) {
        try {
            // Fail any still-RUNNING or PENDING runs
            for (AiExperimentRun run : aiExperimentRunService.findAllByExperiment(experiment.getId())) {
                AiExperimentRunStatus status = run.getStatus();

                if (status == AiExperimentRunStatus.PENDING || status == AiExperimentRunStatus.RUNNING) {
                    aiExperimentRunService.fail(run.getId(), "Orphaned by JVM restart");
                }
            }

            // Fail the experiment itself
            aiExperimentService.markFinished(experiment.getId(), true);

            logger.warn("Experiment {} recovered from orphaned state", experiment.getId());
        } catch (Exception exception) {
            logger.error("Failed to recover orphaned experiment {}", experiment.getId(), exception);
        }
    }
}
```

### Step 3.2: Add query to the service

`AiExperimentService`:

```java
/**
 * @return experiments with status = RUNNING and startedDate before the given threshold.
 */
List<AiExperiment> findRunningOlderThan(Instant threshold);
```

Impl delegates to the repo. Add to `AiExperimentRepository`:

```java
/**
 * Find running experiments (status ordinal 1) started before the given threshold.
 */
List<AiExperiment> findAllByStatusAndStartedDateBefore(int status, Instant threshold);
```

Service impl:

```java
@Override
public List<AiExperiment> findRunningOlderThan(Instant threshold) {
    return aiExperimentRepository.findAllByStatusAndStartedDateBefore(AiExperimentStatus.RUNNING.ordinal(), threshold);
}
```

### Step 3.3: Remote-client stub

Append `findRunningOlderThan` method stub to `RemoteAiExperimentServiceClient`.

### Step 3.4: Test

```java
class AiExperimentOrphanRecoveryRunnerTest {

    @Test
    void testRecoversOrphanedExperiments() {
        AiExperimentService experimentService = mock(AiExperimentService.class);
        AiExperimentRunService runService = mock(AiExperimentRunService.class);

        AiExperiment orphan = new AiExperiment(42L, 7L);
        ReflectionTestUtils.setField(orphan, "id", 100L);

        AiExperimentRun pendingRun = new AiExperimentRun(100L, 1L);
        ReflectionTestUtils.setField(pendingRun, "id", 501L);

        AiExperimentRun runningRun = new AiExperimentRun(100L, 2L);
        ReflectionTestUtils.setField(runningRun, "id", 502L);
        runningRun.markRunning();

        AiExperimentRun completedRun = new AiExperimentRun(100L, 3L);
        ReflectionTestUtils.setField(completedRun, "id", 503L);
        completedRun.complete(9001L, 100, new BigDecimal("0.01"));

        when(experimentService.findRunningOlderThan(any())).thenReturn(List.of(orphan));
        when(runService.findAllByExperiment(100L)).thenReturn(List.of(pendingRun, runningRun, completedRun));

        AiExperimentOrphanRecoveryRunner runner =
            new AiExperimentOrphanRecoveryRunner(experimentService, runService);

        runner.recoverOrphanedExperiments();

        verify(runService).fail(501L, "Orphaned by JVM restart");
        verify(runService).fail(502L, "Orphaned by JVM restart");
        verify(runService, never()).fail(eq(503L), any());
        verify(experimentService).markFinished(100L, true);
    }

    @Test
    void testNoOpWhenNoOrphans() {
        AiExperimentService experimentService = mock(AiExperimentService.class);
        AiExperimentRunService runService = mock(AiExperimentRunService.class);

        when(experimentService.findRunningOlderThan(any())).thenReturn(List.of());

        AiExperimentOrphanRecoveryRunner runner =
            new AiExperimentOrphanRecoveryRunner(experimentService, runService);

        runner.recoverOrphanedExperiments();

        verifyNoInteractions(runService);
    }
}
```

### Step 3.5: Commit

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-experiment/
git commit -m "$(cat <<'EOF'
Recover orphaned experiments on startup

On ApplicationReadyEvent, scan for experiments stuck in RUNNING > 10 min.
Fail their PENDING/RUNNING runs with 'Orphaned by JVM restart' and mark
the experiment FAILED. Users can then inspect partial results and retrigger.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Why Not Atlas

The previous draft of this plan proposed a full migration to atlas-coordinator for experiment execution. Rejected after honest review. Summary:

| Benefit atlas would add | `@Async` alternative | Cost to add the alternative |
|-------------------------|----------------------|------------------------------|
| Restart durability | Startup orphan recovery hook (Task 3) | ~100 lines |
| Retry on transient failures | Spring `RetryTemplate` around the gateway call (Task 2) | ~30 lines |
| Cancellation | `stop_requested` column + per-iteration check (Task 1) | ~150 lines |
| Horizontal scale across workers | Not needed — LLM replay is provider-bound, not CPU-bound. Single-JVM `@Async` already parallelizes. | N/A |
| Observability UI | `ai_experiment_run` status + existing GraphQL already cover this | N/A |

Atlas-coordinator would be ~5× the scope of these three fixes (new Gradle module, task dispatcher + handler + completion handler, feature-flagged rollout across two executor paths, atlas workflow registration ceremony) for no operational benefit over what this plan delivers.

If experiment traffic ever genuinely saturates a single JVM (tens of thousands of concurrent runs), the atlas path stays available as a future migration — but the sub-tasks in this plan (`AiExperimentOrphanRecoveryRunner`, the stop flag, the retry template) all survive that migration unchanged. Nothing here blocks atlas if it becomes necessary later.

---

## Self-Review

**Spec coverage:**

| Pain point | Task |
|------------|------|
| No cancellation | 1 |
| No retry on transient gateway failures | 2 |
| Orphaned `RUNNING` runs after JVM restart | 3 |

**Risk profile:**
- **Task 1** is additive — flag defaults to false, no impact on existing experiments.
- **Task 2** is bounded — retry policy only matches network-adjacent exceptions; programmer errors still fail fast.
- **Task 3** runs once at startup; a bug would only affect orphan recovery itself, not live traffic.

**Plan drift hedging:** if `findAllByStatusAndStartedDateBefore` requires a custom `@Query` (Spring Data JDBC sometimes can't derive from method name alone with enum ordinals + DATETIME), drop to an explicit `@Query("SELECT * FROM ai_experiment WHERE status = :status AND started_date < :threshold")`.

**Explicit non-goals:**
- No UI changes for cancellation; REST endpoint only for now.
- Retry configuration is hardcoded; externalizing to `ApplicationProperties` is a follow-up if ops ever needs to tune it.
- Orphan threshold is hardcoded to 10 minutes; could be configurable later.
