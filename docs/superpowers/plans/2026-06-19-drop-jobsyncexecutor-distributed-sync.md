# Drop `JobSyncExecutor`: distributed-coordinator synchronous execution — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the embedded `JobSyncExecutor` engine with distributed-coordinator execution (`createJob`) plus a shared broker-fed job-completion awaiter, so synchronous webhook and MCP-tool workflow runs use the worker fleet and the webhook controller becomes non-blocking (async-servlet), with no client-visible change.

**Architecture:** A new `JobCompletionAwaiter` in `platform-workflow-execution` exposes `CompletableFuture<Job> await(jobId, timeout)`, completed by a broker listener on the existing `SSE_STREAM_EVENTS` job-status signal (the only completion signal that crosses process boundaries). Each synchronous consumer becomes: `createJob` → `await` → post-hoc read of the tagged task output from durable storage. The webhook path returns `CompletableFuture` end-to-end; MCP facades `.join()`.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JDBC, Caffeine, ByteChef atlas/coordinator engine, Gradle (Kotlin DSL), JUnit 5 + Mockito, Testcontainers for `*IntTest`.

## Global Constraints

- Apache 2.0 license header on all CE files (`server/libs/**`); ByteChef Enterprise header + `@version ee` Javadoc on all EE files (`server/ee/**`), including tests. Spotless picks the header by file **content** (`@version ee`), not path.
- Java: one blank line before control statements and after a variable modification that precedes its use; no trailing blank line before a class's closing `}`; no method-chaining except the allowed fluent DSLs; descriptive variable names (no single letters / `_` prefixes).
- Unit test classes end in `Test`; integration test classes end in `IntTest`; test method names are camelCase with no underscores. Drop `Impl` from test class names (test the interface).
- Persist enums as INT ordinals; append new enum values at the end (not relevant unless an enum is added).
- Run `./gradlew spotlessApply` then `./gradlew check` before declaring server work done.
- Never `git commit --amend` on this branch (the user commits in parallel); always make fresh commits and stage only files this task touched.
- Verified facts to rely on (do not re-derive):
  - `Job.Status` = `CREATED, STARTED, COMPLETED, FAILED, STOPPED`. Terminal = `COMPLETED | FAILED | STOPPED`. Suspend maps to `STOPPED` (`SuspendTaskCompletionHandler` → `jobService.setStatusToStopped` + `JobStatusApplicationEvent(STOPPED)`).
  - `JobService`: `Job getJob(long)`, `Optional<Job> fetchJob(long)`, `Job update(Job)`.
  - `PrincipalJobFacade`: `long createJob(JobParametersDTO, long jobPrincipalId, PlatformType)`, `Job createSyncJob(...)` (creates an undispatched row only).
  - `SseStreamApplicationEventListener` (in `PlatformCoordinatorConfiguration`) republishes every `JobStatusApplicationEvent` as `SseStreamEvent(EVENT_TYPE_JOB_STATUS, status.name())` onto `SseStreamMessageRoute.SSE_STREAM_EVENTS`, with `CURRENT_TENANT_ID` metadata.
  - `SseStreamEvent` constants: `EVENT_TYPE_JOB_STATUS = "job_status"`; `getJobId()`, `getEventType()`, `getPayload()`, `getMetadata(String)`.
  - `WebhookResponseTaskExecutionPostOutputProcessor` tags the task: `putMetadata(MetadataConstants.WEBHOOK_RESPONSE, true)`; the value is the task output (a `WebhookResponse`).
  - Spec: `docs/superpowers/specs/2026-06-19-drop-jobsyncexecutor-distributed-sync-design.md`.

---

## File Structure

**Phase 1 — shared awaiter (`platform-workflow-execution`)**
- Create `platform-workflow-execution-api/.../execution/JobCompletionAwaiter.java` — interface.
- Create `platform-workflow-execution-api/.../execution/exception/JobErrorType.java` + `TaskExecutionErrorType.java` — relocated error types (from `platform-job-sync`).
- Create `platform-workflow-execution-service/.../execution/JobCompletionAwaiterImpl.java` — impl (Caffeine future cache + race guard + timeout).
- Create `platform-workflow-execution-service/.../execution/JobExecutionErrors.java` — relocated `checkForError(Job, TaskExecutionService)`.
- Create `platform-workflow-execution-service/.../execution/config/JobCompletionAwaiterConfiguration.java` — `MessageBrokerConfigurer` bean (`@ConditionalOnCoordinator`) wiring the `SSE_STREAM_EVENTS` listener to the awaiter.
- Modify both module `build.gradle.kts` (add `platform-webhook-api`, message-broker deps, caffeine, coordinator annotation).
- Tests: `platform-workflow-execution-service/src/test/.../JobCompletionAwaiterTest.java`.

**Phase 2 — webhook slice**
- Modify `platform-webhook-api/.../executor/WebhookWorkflowExecutor.java` — `executeSync` returns `CompletableFuture<@Nullable Object>`.
- Modify `platform-webhook-impl/.../executor/WebhookWorkflowExecutorImpl.java` — rewrite `executeSync`/`executeSyncJob`/`collectWebhookResponse`; drop `JobSyncExecutor` field.
- Modify `platform-webhook-impl/.../executor/config/WebhookConfiguration.java` — remove the inline engine; inject `JobCompletionAwaiter` + durable `TaskFileStorage`.
- Modify `platform-webhook-rest-api/.../rest/AbstractWebhookTriggerController.java` — `doProcessTrigger` → `CompletableFuture<ResponseEntity<Object>>`.
- Modify `platform-webhook-rest-impl/.../rest/WebhookTriggerController.java` — `executeWorkflow` → `CompletableFuture<ResponseEntity<?>>`.
- Modify `ai-hub-service/.../agent/WebhookBridgeAgent.java` — `.join()` the future.
- Tests: update `WebhookBridgeAgentTest`; add `WebhookWorkflowExecutorImplTest` collection test; webhook IntTest.

**Phase 3 — MCP facades**
- Modify `automation-ai-mcp-server/.../facade/AutomationMcpToolFacade.java` + `.../config/AutomationMcpServerConfiguration.java`.
- Modify `embedded-ai-mcp-server/.../facade/EmbeddedMcpToolFacade.java` + `.../config/EmbeddedMcpServerConfiguration.java`.

**Phase 4 — removal & rename**
- Delete `platform-job-sync/.../executor/JobSyncExecutor.java`, `JobServiceWrapper.java`, the `platform-job-sync` `SseStreamTaskExecutionPostOutputProcessor.java`, and now-dead support (`WebSocketEmitterRegistry`, `WebSocketStreamTaskExecutionPostOutputProcessor` if unused elsewhere — verify), and the relocated exception package.
- Rename `PrincipalJobFacade.createSyncJob` → `createJobWithoutDispatch` (+ impl, EE remote client/controller, `TriggerErrorHandler`).
- Remove `platform-job-sync` deps from the three consumer modules.

---

## Phase 1 — Shared completion awaiter

### Task 1: Relocate job/task error types into `platform-workflow-execution-api`

**Files:**
- Read: `server/libs/platform/platform-job-sync/src/main/java/com/bytechef/platform/job/sync/exception/JobErrorType.java`, `.../exception/TaskExecutionErrorType.java`
- Create: `server/libs/platform/platform-workflow/platform-workflow-execution/platform-workflow-execution-api/src/main/java/com/bytechef/platform/workflow/execution/exception/JobErrorType.java`
- Create: `.../platform-workflow-execution-api/.../exception/TaskExecutionErrorType.java`

**Interfaces:**
- Produces: `com.bytechef.platform.workflow.execution.exception.JobErrorType` and `TaskExecutionErrorType` — same enum constants and `ErrorType` contract as the originals.

- [ ] **Step 1: Read the originals and copy them verbatim into the new package.** Open both source files. Recreate them under the new package path, changing only the `package` declaration to `com.bytechef.platform.workflow.execution.exception` and keeping the Apache header, every enum constant, and the implemented interface (`com.bytechef.error.ErrorType` or equivalent) exactly. Do **not** delete the originals yet (Phase 4 deletes them once `platform-job-sync` is removed).

- [ ] **Step 2: Compile the module.**

Run: `./gradlew :server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit.**

```bash
git add server/libs/platform/platform-workflow/platform-workflow-execution/platform-workflow-execution-api/src/main/java/com/bytechef/platform/workflow/execution/exception/
git commit -m "732 Relocate job/task error types to platform-workflow-execution-api"
```

### Task 2: `JobCompletionAwaiter` interface

**Files:**
- Create: `.../platform-workflow-execution-api/src/main/java/com/bytechef/platform/workflow/execution/JobCompletionAwaiter.java`

**Interfaces:**
- Produces: `CompletableFuture<Job> await(long jobId, java.time.Duration timeout)` — resolves with the completed `Job` when it reaches a terminal status (`COMPLETED|FAILED|STOPPED`); completes exceptionally with `java.util.concurrent.TimeoutException` if the timeout elapses first.

- [ ] **Step 1: Write the interface.**

```java
/* Apache 2.0 header */
package com.bytechef.platform.workflow.execution;

import com.bytechef.atlas.execution.domain.Job;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Awaits the terminal status of a distributed-coordinator job. The completion signal is the
 * broker-published {@code SSE_STREAM_EVENTS} job-status event (the only completion signal that
 * crosses process boundaries), so this replaces the embedded {@code JobSyncExecutor}'s in-process
 * latch for synchronous webhook and MCP-tool execution.
 *
 * @author Ivica Cardic
 */
public interface JobCompletionAwaiter {

    /**
     * Returns a future that completes with the job once it reaches a terminal status
     * ({@code COMPLETED}, {@code FAILED}, or {@code STOPPED}; suspend maps to {@code STOPPED}).
     * Completes exceptionally with {@link java.util.concurrent.TimeoutException} if {@code timeout}
     * elapses first.
     */
    CompletableFuture<Job> await(long jobId, Duration timeout);
}
```

- [ ] **Step 2: Compile.**

Run: `./gradlew :server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit.**

```bash
git add .../platform-workflow-execution-api/src/main/java/com/bytechef/platform/workflow/execution/JobCompletionAwaiter.java
git commit -m "732 Add JobCompletionAwaiter interface"
```

### Task 3: `JobExecutionErrors.checkForError` util

**Files:**
- Create: `.../platform-workflow-execution-service/src/main/java/com/bytechef/platform/workflow/execution/JobExecutionErrors.java`
- Reference (copy logic from): `platform-job-sync/.../executor/JobSyncExecutor.java` `checkForError(Job)` (lines ~661–700).

**Interfaces:**
- Produces: `static void JobExecutionErrors.checkForError(Job job, TaskExecutionService taskExecutionService)` — throws `ExecutionException` on a `FAILED` last task or `FAILED` job; no-op for `COMPLETED`/`STOPPED`.

- [ ] **Step 1: Write the util**, copying the body of `JobSyncExecutor.checkForError` verbatim but using the relocated error types from Task 1:

```java
/* Apache 2.0 header */
package com.bytechef.platform.workflow.execution;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.error.ExecutionError;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.workflow.execution.exception.JobErrorType;
import com.bytechef.platform.workflow.execution.exception.TaskExecutionErrorType;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public final class JobExecutionErrors {

    private static final Logger log = LoggerFactory.getLogger(JobExecutionErrors.class);

    private JobExecutionErrors() {
    }

    public static void checkForError(Job job, TaskExecutionService taskExecutionService) {
        TaskExecution taskExecution = taskExecutionService
            .fetchLastJobTaskExecution(Validate.notNull(job.getId(), "id"))
            .orElse(null);

        if (taskExecution != null && taskExecution.getStatus() == TaskExecution.Status.FAILED) {
            ExecutionError error = taskExecution.getError();

            if (error != null && error.getMessage() != null) {
                throw new ExecutionException(error.getMessage(), TaskExecutionErrorType.TASK_EXECUTION_FAILED);
            }

            String message = "Task execution failed for job " + job.getId() + " but no error details are available.";

            if (log.isWarnEnabled()) {
                log.warn(
                    "Detected FAILED task execution without error details for jobId={}, taskExecutionId={}",
                    job.getId(), taskExecution.getId());
            }

            throw new ExecutionException(message, TaskExecutionErrorType.TASK_EXECUTION_FAILED);
        }

        if (job.getStatus() == Job.Status.FAILED) {
            ExecutionError error = job.getError();

            if (error != null && error.getMessage() != null) {
                throw new ExecutionException(error.getMessage(), JobErrorType.JOB_FAILED);
            }

            String message = "Job " + job.getId() + " failed but no error details are available.";

            if (log.isWarnEnabled()) {
                log.warn("Detected FAILED job without error details for jobId={}", job.getId());
            }

            throw new ExecutionException(message, JobErrorType.JOB_FAILED);
        }
    }
}
```

- [ ] **Step 2: Compile** (will fail until Task 4 adds the build deps — that is expected; verify by running after Task 4). For now:

Run: `./gradlew :server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-service:compileJava`
Expected: may FAIL on missing `commons-lang3`/`atlas-execution-api` only if not already present — both are already declared (see existing build). Expected SUCCESS.

- [ ] **Step 3: Commit.**

```bash
git add .../platform-workflow-execution-service/src/main/java/com/bytechef/platform/workflow/execution/JobExecutionErrors.java
git commit -m "732 Add JobExecutionErrors.checkForError util"
```

### Task 4: build deps for the awaiter impl + broker listener

**Files:**
- Modify: `.../platform-workflow-execution-service/build.gradle.kts`

**Interfaces:**
- Produces: the `-service` module can now reference `com.bytechef.platform.webhook.event.SseStreamEvent`, `SseStreamMessageRoute`, `MessageBrokerConfigurer`, `@ConditionalOnCoordinator`, and Caffeine.

- [ ] **Step 1: Add dependencies.** Append to the `dependencies {}` block:

```kotlin
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-api"))
    implementation(project(":server:libs:platform:platform-webhook:platform-webhook-api"))
```

(Verify exact coordinates against a sibling module that already uses each — `platform-webhook-impl/build.gradle.kts` for `message-broker-api` + `platform-webhook-api`; `JobSyncExecutor`'s module for the caffeine coordinate and `ConditionalOnCoordinator`'s module `atlas-coordinator-*`. Match the version-catalog/coordinate style already in the repo.)

- [ ] **Step 2: Verify no dependency cycle.**

Run: `./gradlew :server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-service:dependencies --configuration compileClasspath | grep -i "platform-webhook-api\|FAILED\|cycle"`
Expected: shows `platform-webhook-api`, no cycle error. (`platform-webhook-api` depends on `platform-workflow-execution-api`, not `-service`, so `-service → platform-webhook-api` is acyclic.)

- [ ] **Step 3: Compile (re-run Task 3 compile).**

Run: `./gradlew :server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit.**

```bash
git add .../platform-workflow-execution-service/build.gradle.kts
git commit -m "732 Add awaiter build deps to platform-workflow-execution-service"
```

### Task 5: `JobCompletionAwaiterImpl` — failing test first

**Files:**
- Test: `.../platform-workflow-execution-service/src/test/java/com/bytechef/platform/workflow/execution/JobCompletionAwaiterTest.java`

**Interfaces:**
- Consumes: `JobCompletionAwaiter.await`, `JobService`, `Job`, `SseStreamEvent`.
- Produces (impl, Task 6): `JobCompletionAwaiterImpl(JobService jobService)` with `void onSseStreamEvent(SseStreamEvent event)` and `CompletableFuture<Job> await(long, Duration)`.

- [ ] **Step 1: Write the failing test** (covers: completes on terminal event after await; race guard when job already terminal at await time; timeout):

```java
/* Apache 2.0 header */
package com.bytechef.platform.workflow.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobCompletionAwaiterTest {

    @Mock
    private JobService jobService;

    @Test
    void testCompletesOnTerminalEventAfterAwait() throws Exception {
        Job startedJob = job(1L, Job.Status.STARTED);
        Job completedJob = job(1L, Job.Status.COMPLETED);

        when(jobService.fetchJob(1L)).thenReturn(Optional.of(startedJob));
        when(jobService.getJob(1L)).thenReturn(completedJob);

        JobCompletionAwaiterImpl awaiter = new JobCompletionAwaiterImpl(jobService);

        CompletableFuture<Job> future = awaiter.await(1L, Duration.ofSeconds(5));

        assertThat(future).isNotDone();

        awaiter.onSseStreamEvent(jobStatusEvent(1L, "COMPLETED"));

        assertThat(future.get(2, TimeUnit.SECONDS)).isEqualTo(completedJob);
    }

    @Test
    void testRaceGuardCompletesWhenJobAlreadyTerminalAtAwait() throws Exception {
        Job completedJob = job(2L, Job.Status.COMPLETED);

        when(jobService.fetchJob(2L)).thenReturn(Optional.of(completedJob));

        JobCompletionAwaiterImpl awaiter = new JobCompletionAwaiterImpl(jobService);

        CompletableFuture<Job> future = awaiter.await(2L, Duration.ofSeconds(5));

        assertThat(future.get(2, TimeUnit.SECONDS)).isEqualTo(completedJob);
    }

    @Test
    void testStoppedIsTerminal() throws Exception {
        Job stoppedJob = job(3L, Job.Status.STOPPED);

        when(jobService.fetchJob(3L)).thenReturn(Optional.of(stoppedJob));

        JobCompletionAwaiterImpl awaiter = new JobCompletionAwaiterImpl(jobService);

        assertThat(awaiter.await(3L, Duration.ofSeconds(5)).get(2, TimeUnit.SECONDS)).isEqualTo(stoppedJob);
    }

    @Test
    void testTimeout() {
        Job startedJob = job(4L, Job.Status.STARTED);

        when(jobService.fetchJob(4L)).thenReturn(Optional.of(startedJob));

        JobCompletionAwaiterImpl awaiter = new JobCompletionAwaiterImpl(jobService);

        CompletableFuture<Job> future = awaiter.await(4L, Duration.ofMillis(100));

        assertThatThrownBy(() -> future.get(2, TimeUnit.SECONDS))
            .isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(TimeoutException.class);
    }

    private static Job job(long id, Job.Status status) {
        Job job = new Job();

        job.setId(id);
        job.setStatus(status);

        return job;
    }

    private static SseStreamEvent jobStatusEvent(long jobId, String status) {
        return new SseStreamEvent(jobId, SseStreamEvent.EVENT_TYPE_JOB_STATUS, status);
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails to compile/run (impl missing).**

Run: `./gradlew :server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-service:test --tests "com.bytechef.platform.workflow.execution.JobCompletionAwaiterTest"`
Expected: FAIL — `JobCompletionAwaiterImpl` does not exist.

- [ ] **Step 3: Commit the test.**

```bash
git add .../platform-workflow-execution-service/src/test/java/com/bytechef/platform/workflow/execution/JobCompletionAwaiterTest.java
git commit -m "732 Add failing JobCompletionAwaiter test"
```

### Task 6: `JobCompletionAwaiterImpl` — make it pass

**Files:**
- Create: `.../platform-workflow-execution-service/src/main/java/com/bytechef/platform/workflow/execution/JobCompletionAwaiterImpl.java`

**Interfaces:**
- Produces: `JobCompletionAwaiterImpl(JobService)`, `CompletableFuture<Job> await(long, Duration)`, `void onSseStreamEvent(SseStreamEvent)`.

- [ ] **Step 1: Write the impl.**

```java
/* Apache 2.0 header */
package com.bytechef.platform.workflow.execution;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class JobCompletionAwaiterImpl implements JobCompletionAwaiter {

    private static final Logger log = LoggerFactory.getLogger(JobCompletionAwaiterImpl.class);

    private final Cache<String, CompletableFuture<Job>> futures = Caffeine.newBuilder()
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .build();
    private final JobService jobService;

    @SuppressFBWarnings("EI")
    public JobCompletionAwaiterImpl(JobService jobService) {
        this.jobService = jobService;
    }

    @Override
    public CompletableFuture<Job> await(long jobId, Duration timeout) {
        String key = TenantCacheKeyUtils.getKey(jobId);

        CompletableFuture<Job> future = futures.get(key, k -> new CompletableFuture<>());

        future.whenComplete((job, throwable) -> futures.invalidate(key));

        // Race guard: the job may already have terminated before this future was registered, in which case the
        // job-status broker event was dropped (no future yet). Complete immediately if so.
        Optional<Job> jobOptional = jobService.fetchJob(jobId);

        if (jobOptional.isPresent() && isTerminal(jobOptional.get())) {
            future.complete(jobOptional.get());

            return future;
        }

        return future.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void onSseStreamEvent(SseStreamEvent sseStreamEvent) {
        if (!SseStreamEvent.EVENT_TYPE_JOB_STATUS.equals(sseStreamEvent.getEventType())) {
            return;
        }

        long jobId = sseStreamEvent.getJobId();
        String key = TenantCacheKeyUtils.getKey(jobId);

        CompletableFuture<Job> future = futures.getIfPresent(key);

        if (future == null) {
            return;
        }

        Object payload = sseStreamEvent.getPayload();
        String status = payload != null ? payload.toString() : "";

        if ("COMPLETED".equals(status) || "FAILED".equals(status) || "STOPPED".equals(status)) {
            try {
                future.complete(jobService.getJob(jobId));
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
        }
    }

    private static boolean isTerminal(Job job) {
        Job.Status status = job.getStatus();

        return status == Job.Status.COMPLETED || status == Job.Status.FAILED || status == Job.Status.STOPPED;
    }
}
```

- [ ] **Step 2: Run the test.**

Run: `./gradlew :server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-service:test --tests "com.bytechef.platform.workflow.execution.JobCompletionAwaiterTest"`
Expected: PASS (4 tests). If `Job` has no no-arg setter constructor, build the `Job` via its actual API — check `com.bytechef.atlas.execution.domain.Job` and adjust the `job(...)` helper accordingly.

- [ ] **Step 3: spotlessApply + commit.**

```bash
./gradlew spotlessApply
git add .../platform-workflow-execution-service/src/main/java/com/bytechef/platform/workflow/execution/JobCompletionAwaiterImpl.java
git commit -m "732 Implement JobCompletionAwaiterImpl (broker-fed, race-guarded, timeout)"
```

### Task 7: broker listener wiring (`MessageBrokerConfigurer`)

**Files:**
- Create: `.../platform-workflow-execution-service/src/main/java/com/bytechef/platform/workflow/execution/config/JobCompletionAwaiterConfiguration.java`
- Reference pattern: `platform-webhook-impl/.../executor/config/SseStreamMessageBrokerConfigurerConfiguration.java`.

**Interfaces:**
- Produces: a `JobCompletionAwaiter` Spring bean and a `MessageBrokerConfigurer` that routes `SSE_STREAM_EVENTS` to `JobCompletionAwaiterImpl.onSseStreamEvent` with tenant context restored.

- [ ] **Step 1: Write the config**, mirroring `SseStreamMessageBrokerConfigurerConfiguration` exactly (same `@ConditionalOnCoordinator`, same delegate + tenant restoration):

```java
/* Apache 2.0 header */
package com.bytechef.platform.workflow.execution.config;

import static com.bytechef.tenant.TenantContext.CURRENT_TENANT_ID;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.webhook.message.route.SseStreamMessageRoute;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiterImpl;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class JobCompletionAwaiterConfiguration {

    @Bean
    JobCompletionAwaiterImpl jobCompletionAwaiter(JobService jobService) {
        return new JobCompletionAwaiterImpl(jobService);
    }

    @Bean
    @ConditionalOnCoordinator
    MessageBrokerConfigurer<?> jobCompletionAwaiterMessageBrokerConfigurer(
        JobCompletionAwaiterImpl jobCompletionAwaiter) {

        JobCompletionAwaiterDelegate delegate = new JobCompletionAwaiterDelegate(jobCompletionAwaiter);

        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> messageBrokerListenerRegistrar
            .registerListenerEndpoint(
                listenerEndpointRegistrar, SseStreamMessageRoute.SSE_STREAM_EVENTS, 1, delegate,
                "onSseStreamEvent");
    }

    private record JobCompletionAwaiterDelegate(JobCompletionAwaiterImpl jobCompletionAwaiter) {

        @SuppressFBWarnings("UPM")
        public void onSseStreamEvent(SseStreamEvent sseStreamEvent) {
            TenantContext.runWithTenantId(
                (String) sseStreamEvent.getMetadata(CURRENT_TENANT_ID),
                () -> jobCompletionAwaiter.onSseStreamEvent(sseStreamEvent));
        }
    }
}
```

Note: exposing the bean as the concrete `JobCompletionAwaiterImpl` lets the configurer reach `onSseStreamEvent`; consumers inject the `JobCompletionAwaiter` interface (Spring resolves by type).

- [ ] **Step 2: Compile the module.**

Run: `./gradlew :server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-service:compileJava`
Expected: BUILD SUCCESSFUL. (If `MessageBrokerConfigurer`/`registerListenerEndpoint` signatures differ, match `SseStreamMessageBrokerConfigurerConfiguration` verbatim.)

- [ ] **Step 3: spotlessApply + commit.**

```bash
./gradlew spotlessApply
git add .../platform-workflow-execution-service/src/main/java/com/bytechef/platform/workflow/execution/config/JobCompletionAwaiterConfiguration.java
git commit -m "732 Wire JobCompletionAwaiter to SSE_STREAM_EVENTS broker route"
```

---

## Phase 2 — Webhook slice

### Task 8: change `executeSync` return type to `CompletableFuture`

**Files:**
- Modify: `platform-webhook-api/.../executor/WebhookWorkflowExecutor.java:76`

**Interfaces:**
- Produces: `CompletableFuture<@Nullable Object> executeSync(WorkflowExecutionId, WebhookRequest)`.

- [ ] **Step 1: Change the signature.** Replace:

```java
    Object executeSync(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest);
```

with:

```java
    java.util.concurrent.CompletableFuture<@org.jspecify.annotations.Nullable Object> executeSync(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest);
```

(Prefer top-of-file imports for `CompletableFuture` and `Nullable` per the file's existing import style.)

- [ ] **Step 2: Do not compile yet** (impl + callers updated in Tasks 9–11). Commit the interface change together with Task 9 to keep the module compiling. Skip the standalone commit here.

### Task 9: rewrite `WebhookWorkflowExecutorImpl.executeSync`

**Files:**
- Modify: `platform-webhook-impl/.../executor/WebhookWorkflowExecutorImpl.java`

**Interfaces:**
- Consumes: `JobCompletionAwaiter.await`, `PrincipalJobFacade.createJob`, durable `TaskFileStorage`, `JobExecutionErrors.checkForError`, `taskExecutionService`.
- Produces: `CompletableFuture<@Nullable Object> executeSync(...)`.

- [ ] **Step 1: Swap the constructor dependencies.** Remove the `JobSyncExecutor jobSyncExecutor` field/param. Add `JobCompletionAwaiter jobCompletionAwaiter`, `TaskExecutionService taskExecutionService`, and a `Duration syncTimeout` (default 300s — pass from config). Keep `principalJobFacade`, `taskFileStorage` (must be the **durable** instance — see Task 10), `webhookWorkflowSyncExecutor`, etc.

- [ ] **Step 2: Replace `executeSyncJob` + `collectWebhookResponse` + `executeSync`.** New code:

```java
    @Override
    public CompletableFuture<@Nullable Object> executeSync(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {

        TriggerOutput triggerOutput = webhookWorkflowSyncExecutor.execute(workflowExecutionId, webhookRequest);

        Map<String, ?> inputMap = getInputMap(workflowExecutionId);
        String workflowId = getWorkflowId(workflowExecutionId);

        if (!triggerOutput.batch() && triggerOutput.value() instanceof Collection<?> triggerOutputValues) {
            List<CompletableFuture<Map<String, ?>>> futures = new ArrayList<>();

            for (Object triggerOutputValue : triggerOutputValues) {
                futures.add(runJob(workflowExecutionId, workflowId, inputMap, triggerOutputValue));
            }

            return CompletableFuture
                .allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(unused -> futures.stream()
                    .map(CompletableFuture::join)
                    .toList());
        }

        return runJob(workflowExecutionId, workflowId, inputMap, triggerOutput.value())
            .thenApply(outputs -> outputs);
    }

    private CompletableFuture<Map<String, ?>> runJob(
        WorkflowExecutionId workflowExecutionId, String workflowId, Map<String, ?> inputMap,
        Object triggerOutputValue) {

        long jobId = TenantContext.callWithTenantId(
            workflowExecutionId.getTenantId(),
            () -> principalJobFacade.createJob(
                createJobParameters(workflowExecutionId, workflowId, inputMap, triggerOutputValue),
                workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getType()));

        return jobCompletionAwaiter.await(jobId, syncTimeout)
            .thenApply(job -> TenantContext.callWithTenantId(
                workflowExecutionId.getTenantId(), () -> collectOutputs(job)));
    }

    private Map<String, ?> collectOutputs(Job job) {
        JobExecutionErrors.checkForError(job, taskExecutionService);

        long jobId = Validate.notNull(job.getId(), "id");

        Object webhookResponse = readWebhookResponse(jobId);

        if (webhookResponse != null) {
            job.setOutputs(
                taskFileStorage.storeJobOutputs(jobId, Map.of(MetadataConstants.WEBHOOK_RESPONSE, webhookResponse)));
        }

        return job.getOutputs() == null ? Map.of() : taskFileStorage.readJobOutputs(job.getOutputs());
    }

    private @Nullable Object readWebhookResponse(long jobId) {
        return taskExecutionService.getJobTaskExecutions(jobId)
            .stream()
            .filter(taskExecution -> taskExecution.getMetadata()
                .containsKey(MetadataConstants.WEBHOOK_RESPONSE))
            .filter(taskExecution -> taskExecution.getOutput() != null)
            .max(Comparator
                .comparing(TaskExecution::getEndDate, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(TaskExecution::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
            .map(taskExecution -> taskFileStorage.readTaskExecutionOutput(taskExecution.getOutput()))
            .orElse(null);
    }
```

Notes: the non-batch branch previously could return a bare value when there was no `WEBHOOK_RESPONSE`; here we always return the job-outputs map (the controller already handles a non-`WEBHOOK_RESPONSE` map via `ResponseEntity.ok(outputs)`).

**Last-wins ordering is a deliberate, load-bearing requirement (do not "simplify").** A single run may execute multiple `WebhookResponse` actions — usually sequentially, but **possibly in parallel branches that race to finish** (uncommon but valid). Today's behavior is *last-to-complete wins* (the `AtomicReference.set` callback fires in completion order). To reproduce that exactly — including the parallel-race case — pick the tagged task with the **latest `endDate`** (completion time), with `getId` as a deterministic tiebreaker (which also removes the non-determinism today has when two finish at the same instant). Do NOT order by created-date and do NOT short-circuit on the first match: created-date order equals completion order only for sequential flows, and would diverge on a parallel race.

- [ ] **Step 3: Remove** the `JobSyncExecutor` import and the stale `executeSyncJob` Javadoc block.

- [ ] **Step 4: Compile** (needs Task 10 config change to supply the new constructor args; do Task 10 then compile).

- [ ] **Step 5: Commit** (with Task 8 + Task 10).

### Task 10: update `WebhookConfiguration` (remove embedded engine)

**Files:**
- Modify: `platform-webhook-impl/.../executor/config/WebhookConfiguration.java`

**Interfaces:**
- Consumes: `JobCompletionAwaiter` bean (from Phase 1), durable `TaskFileStorage` (`durableTaskFileStorage`), `TaskExecutionService`.
- Produces: `WebhookWorkflowExecutor` bean constructed without `JobSyncExecutor`.

- [ ] **Step 1: Delete the inline engine.** Remove the `new JobSyncExecutor(...)` construction and the now-unused private helpers (`getTaskCompletionHandlerFactories`, `getTaskDispatcherResolverFactories`, `getTaskDispatcherAdapterFactories`, `getAdditionalApplicationEventListeners`, `createEventPublisher`, the `AsyncMessageBroker` + `InMemoryTaskFileStorage` locals) **if** they are unused after the change. Keep `sseStreamBridgeRegistry()`.

- [ ] **Step 2: Rewrite the `webhookExecutor` bean** to inject `JobCompletionAwaiter jobCompletionAwaiter`, pass `durableTaskFileStorage` as the executor's `TaskFileStorage` (so post-hoc reads hit durable shared storage), `taskExecutionService`, and a `Duration.ofSeconds(300)` timeout (or a `@Value`-bound property `bytechef.webhook.sync-timeout` defaulting to 300s). Drop the params only used by the deleted engine.

```java
    @Bean
    WebhookWorkflowExecutor webhookExecutor(
        ApplicationEventPublisher eventPublisher, JobCompletionAwaiter jobCompletionAwaiter,
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, PrincipalJobFacade principalJobFacade,
        SseStreamBridgeRegistry sseStreamBridgeRegistry, TaskExecutionService taskExecutionService,
        TaskFileStorage durableTaskFileStorage, TriggerDefinitionService triggerDefinitionService,
        WebhookWorkflowSyncExecutor triggerSyncExecutor, WorkflowService workflowService) {

        return new WebhookWorkflowExecutorImpl(
            eventPublisher, jobCompletionAwaiter, jobPrincipalAccessorRegistry, principalJobFacade,
            sseStreamBridgeRegistry, taskExecutionService, durableTaskFileStorage, triggerDefinitionService,
            triggerSyncExecutor, workflowService, java.time.Duration.ofSeconds(300));
    }
```

(Match the final `WebhookWorkflowExecutorImpl` constructor parameter order from Task 9. The streaming `executeAsync(.., bridge)` path is unchanged and still uses `principalJobFacade.createJob` + `sseStreamBridgeRegistry`.)

- [ ] **Step 3: Add the `platform-workflow-execution` (api) dependency** to `platform-webhook-impl/build.gradle.kts` if not already present (it likely is, via `PrincipalJobFacade`). Verify.

- [ ] **Step 4: Compile both modules.**

Run: `./gradlew :server:libs:platform:platform-webhook:platform-webhook-api:compileJava :server:libs:platform:platform-webhook:platform-webhook-impl:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: spotlessApply + commit (Tasks 8–10 together).**

```bash
./gradlew spotlessApply
git add platform-webhook-api/.../WebhookWorkflowExecutor.java platform-webhook-impl/.../WebhookWorkflowExecutorImpl.java platform-webhook-impl/.../config/WebhookConfiguration.java platform-webhook-impl/build.gradle.kts
git commit -m "732 Run sync webhook execution on the distributed coordinator via JobCompletionAwaiter"
```

### Task 11: async-servlet controller

**Files:**
- Modify: `platform-webhook-rest-api/.../rest/AbstractWebhookTriggerController.java`
- Modify: `platform-webhook-rest-impl/.../rest/WebhookTriggerController.java`

**Interfaces:**
- Consumes: `executeSync` (now `CompletableFuture<Object>`).
- Produces: `CompletableFuture<ResponseEntity<Object>> doProcessTrigger(...)`; `CompletableFuture<ResponseEntity<?>> executeWorkflow(...)`.

- [ ] **Step 1: `doProcessTrigger` → future.** Change the return type to `CompletableFuture<ResponseEntity<Object>>`. In the `workflowSyncExecution()` branch, compose:

```java
        if (webhookTriggerFlags.workflowSyncExecution()) {
            WebhookRequest finalWebhookRequest = webhookRequest;

            return webhookWorkflowExecutor.executeSync(workflowExecutionId, finalWebhookRequest)
                .thenApply(outputs -> {
                    if (outputs instanceof Map<?, ?> responseMap
                        && responseMap.containsKey(MetadataConstants.WEBHOOK_RESPONSE)) {

                        return processWebhookResponse(httpServletRequest, httpServletResponse, responseMap);
                    }

                    return ResponseEntity.ok(outputs);
                });
        }
```

Wrap every other branch's already-built `ResponseEntity` in `CompletableFuture.completedFuture(...)`. Keep `processWebhookResponse` synchronous (it runs inside `thenApply`).

- [ ] **Step 2: `executeWorkflow` → future.** Change the return type to `CompletableFuture<ResponseEntity<?>>`. Keep the `TenantContext.callWithTenantId(...)` wrapper returning the future. The head/disabled branches return `CompletableFuture.completedFuture(responseEntity)`; the else branch returns `doProcessTrigger(...)` (already a future). Spring MVC releases the servlet thread while the returned future is pending. Tenant context for the async continuation is restored inside `runJob`'s `thenApply` (Task 9), so no servlet-thread tenant leak.

- [ ] **Step 3: Compile.**

Run: `./gradlew :server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-api:compileJava :server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-impl:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: spotlessApply + commit.**

```bash
./gradlew spotlessApply
git add platform-webhook-rest-api/.../AbstractWebhookTriggerController.java platform-webhook-rest-impl/.../WebhookTriggerController.java
git commit -m "732 Make webhook trigger controller non-blocking (async-servlet)"
```

### Task 12: update `WebhookBridgeAgent` caller

**Files:**
- Modify: `ai-hub-service/.../agent/WebhookBridgeAgent.java:421`

**Interfaces:**
- Consumes: `executeSync` future.

- [ ] **Step 1: `.join()` the future**, preserving the existing try/catch (`.join()` wraps the cause in `CompletionException` — unwrap to keep the current `onError` behavior):

```java
            Object outputs;

            try {
                outputs = webhookWorkflowExecutor.executeSync(workflowExecutionId, webhookRequest)
                    .join();
            } catch (java.util.concurrent.CompletionException completionException) {
                Throwable cause = completionException.getCause();

                throw cause instanceof RuntimeException runtimeException
                    ? runtimeException
                    : new IllegalStateException(cause);
            }

            handleSyncOutputs(outputs, bridge, subscriber, messageId, task, userMessageText);
```

This file is under `server/ee/` → ensure the ByteChef EE header + `@version ee` are already present (they are; do not change them).

- [ ] **Step 2: Compile the EE module.**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit.**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/agent/WebhookBridgeAgent.java
git commit -m "732 Adapt WebhookBridgeAgent to async executeSync"
```

### Task 13: fix `WebhookBridgeAgentTest`

**Files:**
- Modify: `ai-hub-service/src/test/.../agent/WebhookBridgeAgentTest.java`

- [ ] **Step 1: Update the stubs.** Every `when(webhookFacade.executeSync(...)).thenReturn(value)` must now return a future: `.thenReturn(CompletableFuture.completedFuture(value))`. `verify(...).executeSync(...)` calls are unchanged.

- [ ] **Step 2: Run the test.**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests "com.bytechef.ee.ai.hub.agent.WebhookBridgeAgentTest"`
Expected: PASS.

- [ ] **Step 3: Commit.**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/agent/WebhookBridgeAgentTest.java
git commit -m "732 Update WebhookBridgeAgentTest for async executeSync"
```

### Task 14: webhook sync integration test (distributed path)

**Files:**
- Create or extend: a `*IntTest` under `platform-webhook-rest-impl/src/test/.../web/rest/` (find an existing webhook IntTest to extend; else create `WebhookTriggerControllerIntTest`).

**Interfaces:**
- Consumes: real coordinator + broker + `JobCompletionAwaiter` (full Spring context).

- [ ] **Step 1: Write an IntTest** that POSTs to `/webhooks/{id}` for a `workflowSyncExecution` workflow with a `WEBHOOK_RESPONSE`-producing task and asserts: (a) HTTP 200 with the expected body, (b) a failed-job workflow surfaces the error, (c) a batch (collection) trigger output returns a list. Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@ActiveProfiles("testint")` + Testcontainers, matching sibling webhook IntTests. Assert against the real `SSE_STREAM_EVENTS` completion path (no mocking of the awaiter).

- [ ] **Step 2: Run it.**

Run: `./gradlew :server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-impl:testIntegration`
Expected: PASS. (Docker must be running for Testcontainers.)

- [ ] **Step 3: Commit.**

```bash
git add platform-webhook-rest-impl/src/test/...
git commit -m "732 Add distributed sync webhook integration test"
```

---

## Phase 3 — MCP facades

### Task 15: `AutomationMcpToolFacade` onto the awaiter

**Files:**
- Modify: `automation-ai-mcp-server/.../facade/AutomationMcpToolFacade.java:233`
- Modify: `automation-ai-mcp-server/.../config/AutomationMcpServerConfiguration.java`

**Interfaces:**
- Consumes: `JobCompletionAwaiter`, `PrincipalJobFacade.createJob`, `JobExecutionErrors.checkForError`.

- [ ] **Step 1: Replace the executor call.** Swap the `jobSyncExecutor.execute(...)` block for:

```java
            long jobId = principalJobFacade.createJob(
                new JobParametersDTO(projectDeploymentWorkflow.getWorkflowId(), inputs),
                projectDeploymentWorkflow.getProjectDeploymentId(), PlatformType.AUTOMATION);

            Job job = jobCompletionAwaiter.await(jobId, java.time.Duration.ofSeconds(300))
                .join();

            JobExecutionErrors.checkForError(job, taskExecutionService);

            if (job.getOutputs() == null) {
                return null;
            }

            return getCallableResponseOutput(job)
                .orElseGet(() -> taskFileStorage.readJobOutputs(job.getOutputs()));
```

`getCallableResponseOutput` is unchanged (post-hoc read of the `CALLABLE_RESPONSE`-tagged last task). Ensure `taskFileStorage` here is the durable storage the distributed job writes to.

- [ ] **Step 2: Swap constructor deps** in the facade + `AutomationMcpServerConfiguration`: remove `JobSyncExecutor` (field, ctor param, and the `new JobSyncExecutor(...)` construction with all its helper wiring); add `JobCompletionAwaiter jobCompletionAwaiter` (and `TaskExecutionService` if not already injected). Add the `platform-workflow-execution` (api) dependency to the module build if missing.

- [ ] **Step 3: Compile.**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-mcp-server:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: spotlessApply + commit.**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-ai/automation-ai-mcp-server/...
git commit -m "732 Run automation MCP tool workflows on the distributed coordinator"
```

### Task 16: `EmbeddedMcpToolFacade` onto the awaiter

**Files:**
- Modify: `embedded-ai-mcp-server/.../facade/EmbeddedMcpToolFacade.java:369`
- Modify: `embedded-ai-mcp-server/.../config/EmbeddedMcpServerConfiguration.java`

**Interfaces:**
- Consumes: same as Task 15. **EE module** — ByteChef EE header + `@version ee` on any edited/created files.

- [ ] **Step 1: Replace the executor call** exactly as in Task 15 but with the embedded principal/type:

```java
            long jobId = principalJobFacade.createJob(
                new JobParametersDTO(integrationInstanceConfigurationWorkflow.getWorkflowId(), inputs),
                integrationInstanceId, PlatformType.EMBEDDED);

            Job job = jobCompletionAwaiter.await(jobId, java.time.Duration.ofSeconds(300))
                .join();

            JobExecutionErrors.checkForError(job, taskExecutionService);

            if (job.getOutputs() == null) {
                return null;
            }

            return getCallableResponseOutput(job)
                .orElseGet(() -> taskFileStorage.readJobOutputs(job.getOutputs()));
```

- [ ] **Step 2: Swap constructor deps** in the facade + `EmbeddedMcpServerConfiguration` (remove `JobSyncExecutor` construction; add `JobCompletionAwaiter`). Add the build dependency if missing. Confirm the EE remote-client topology still satisfies `PrincipalJobFacade`/`JobService`/`JobCompletionAwaiter` injection in this app variant (the awaiter bean is `@ConditionalOnCoordinator` for the listener, but the `JobCompletionAwaiterImpl` bean itself is unconditional — verify the embedded MCP app runs a coordinator; if it dispatches to a remote coordinator, the awaiter listener must run where the SSE events are consumed — flag for the IntTest).

- [ ] **Step 3: Compile.**

Run: `./gradlew :server:ee:libs:embedded:embedded-ai:embedded-ai-mcp-server:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: spotlessApply + commit.**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-server/...
git commit -m "732 gecko Run embedded MCP tool workflows on the distributed coordinator"
```

---

## Phase 4 — Removal & rename

### Task 17: delete `JobSyncExecutor` and dead support

**Files:**
- Delete: `platform-job-sync/.../executor/JobSyncExecutor.java`, `JobServiceWrapper.java`, `platform-job-sync/.../executor/SseStreamTaskExecutionPostOutputProcessor.java`, `platform-job-sync/.../exception/JobErrorType.java` + `TaskExecutionErrorType.java`.
- Investigate: `WebSocketEmitterRegistry`, `WebSocketStreamTaskExecutionPostOutputProcessor`, `JobSyncAsyncTaskExecutor` — delete only if no remaining references.

- [ ] **Step 1: Grep for residual references** before deleting.

Run: `grep -rn "JobSyncExecutor\|JobServiceWrapper\|job.sync.exception" --include="*.java" server | grep -v build | grep -v "/test/"`
Expected: only the files about to be deleted (and the now-relocated exception imports already updated). If any production reference remains, fix it before deleting.

- [ ] **Step 2: Delete the files** and remove `platform-job-sync` deps from the three consumer modules' `build.gradle.kts`.

- [ ] **Step 3: Full build.**

Run: `./gradlew compileJava compileTestJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit.**

```bash
git add -A
git commit -m "732 Remove embedded JobSyncExecutor engine"
```

### Task 18: rename `createSyncJob` → `createJobWithoutDispatch`

**Files:**
- Modify: `PrincipalJobFacade.java:44`, `PrincipalJobFacadeImpl.java:110`, EE `RemotePrincipalJobFacadeClient.java:57`, EE `RemotePrincipalJobFacadeController.java:54`, `TriggerErrorHandler.java:88`.

- [ ] **Step 1: Rename** the method everywhere (it now only inserts an undispatched job row; "sync" is obsolete). Update the Javadoc to: "Creates a persisted job row without dispatching it to the coordinator."

- [ ] **Step 2: Grep to confirm no stragglers.**

Run: `grep -rn "createSyncJob" --include="*.java" server | grep -v build`
Expected: no matches.

- [ ] **Step 3: Full build + check.**

Run: `./gradlew spotlessApply check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit.**

```bash
git add -A
git commit -m "732 Rename createSyncJob to createJobWithoutDispatch"
```

---

## Final verification

- [ ] `./gradlew spotlessApply`
- [ ] `./gradlew check` — all static analysis + unit tests green.
- [ ] `./gradlew testIntegration` for the touched modules (Docker running) — webhook sync IntTest + any MCP IntTests green.
- [ ] Manual smoke (optional): a `workflowSyncExecution` webhook returns the same JSON body as before; an MCP workflow tool returns its `CALLABLE_RESPONSE`; a suspend/approval workflow returns (STOPPED) rather than hanging.

## Self-review notes (spec coverage)

- Awaiter + broker feed + race guard + timeout → Tasks 2, 5, 6, 7. ✅
- Cross-process completion via `SSE_STREAM_EVENTS` → Task 7 (mirrors `SseStreamMessageBrokerConfigurerConfiguration`), verified in Task 14. ✅
- Webhook JSON-body contract preserved + async-servlet → Tasks 8–12. ✅
- Durable storage for post-hoc reads → Task 10 Step 2. ✅
- MCP facades (both) → Tasks 15–16. ✅
- Error handling parity (`checkForError`, STOPPED not an error) → Tasks 3, 9, 15, 16. ✅
- Suspend = terminal STOPPED, no special handling → covered implicitly by terminal-status logic (Task 6) + checkForError ignoring STOPPED (Task 3). ✅
- `createSyncJob` kept + renamed → Task 18. ✅
- `JobSyncExecutor` removed → Task 17. ✅
- **Open risk to validate during execution:** the embedded MCP app's coordinator topology (Task 16 Step 2) — confirm the awaiter's broker listener runs where SSE job-status events land, or the MCP `.await` will time out.
