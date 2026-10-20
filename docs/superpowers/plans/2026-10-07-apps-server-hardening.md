# Apps — Server Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close seven server-side gaps in Apps (EE): per-tenant retention sweeps, system deployments refused by the
workflow resolver, a per-definition schema fallback in the action/tool listings, an `APP_UPDATED` audit event, a
dedicated app executor, an in-flight proxy response cap with a typed storage-failure code, and authorization of the
tool invocation log split by audience.

**Architecture:** Each design item is one task, except the proxy cap (item 2), which splits into the platform-wide SDK
option plus its `HttpClientExecutor` enforcement (Task 6) and the proxy's use of it (Task 7), and the tool invocation
log (item 9), which splits into server (Task 8) and client (Task 9). Order: 1, 4, 5, 6, 3, 2, 9 — the small isolated
service changes first; 5, 3 and 2 all edit `AppExecutionFacadeImpl`, so they run in that order (3 renames the lane
that 2's facade change sits beside); 9 last because its client half needs the server schema for codegen.

**Tech Stack:** Java 25 / Spring Boot 4.0.7 (EE modules, Spring Data JDBC, Spring GraphQL, Spring Security method
security), JUnit 5 / Mockito / AssertJ / Testcontainers, React 19 + TypeScript 6 + TanStack Query (generated hooks),
Vitest 4.

**Spec:** `docs/superpowers/specs/2026-10-07-apps-hardening-and-polish-design.md`, section "Server hardening (items 1-6, 9)" (binding).
Context: `docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md` and `.agents/apps.md`.

## Rulings on points the design leaves open (settled against the code)

1. **Retention jobs take `ObjectProvider<TenantService>`, not `TenantService`.** `ToolInvocationLogRetentionJob`'s
   module also ships in `execution-app`, which carries no `tenant-single-service`/`tenant-remote-client`, so a hard
   `TenantService` dependency could fail that app's boot. Both jobs follow `AssetFileOrphanBlobCleaner`: iterate
   `tenantService.getTenantIds()` under `TenantContext.runWithTenantId`, catch and WARN per tenant (the
   `JobRetentionMonitor` shape the design names), and sweep the current tenant when no `TenantService` bean exists.
2. **`ProxyStorageException` is translated in `AppExecutionFacadeImpl.executeProxy`, not by a controller
   `@ExceptionHandler`.** The exception lives in `automation-app-service`; `automation-app-public-rest` does not depend
   on that module and cannot name it. The facade rethrows it as `ConfigurationException(..., APP_PROXY_STORAGE_FAILED)`,
   which the controller's existing `ConfigurationException` handler answers with `{"code": "APP_PROXY_STORAGE_FAILED"}`;
   `toAppStatus` maps 123 to 500.
3. **The new exception is `com.bytechef.component.exception.HttpResponseTooLargeException` in
   `sdks/backend/java/component-api`.** That module defines the option, and it is the only module both
   `platform-component-context-service` (which throws) and `automation-app-service` (which maps it) depend on. It is a
   plain `RuntimeException`, deliberately not a `ProviderException` (`ContextImpl` rethrows those unwrapped and
   `AbstractAppApiController` answers them 422 `PROVIDER_ERROR`) and not an `IOException` (the facade classifies an
   `IOException` anywhere in the chain as an upstream outage).
4. **`maxResponseBytes` applies to `ResponseType.BINARY` only** — the only response type the client reads as a stream.
   The Javadoc says so. No other `Http.Configuration` implementation or copy exists (`Context.java` holds the only
   `interface Http`; `HostContextBridge` maps a fixed subset for the sandbox and rejects BINARY bodies, so it needs no
   change). No declared-`Content-Length` pre-check: a `HEAD` or `304` legitimately declares a large length with an empty
   body.
5. **`appExecutor` exists only when `bytechef.app.execution.concurrency-limit` is set** (`@ConditionalOnProperty`, no
   Java default). `AsyncConfiguration` is loaded by every app; only `server-app` runs `AppExecutionFacadeImpl` and only
   `server-app`'s `application-bytechef.yml` sets the key. A missing key there fails boot on the facade's qualifier; a
   value below 1 fails bean creation with the property name in the message. `ApplicationProperties.App.Execution` gains
   `concurrencyLimit` (no initializer) because `ApplicationProperties` binds with `ignoreUnknownFields = false`.
   Every Spring test context that builds `AppExecutionFacadeImpl` was checked: the seven `automation-app-service`
   IntTests all boot `AppIntTestConfiguration` (component scan of `com.bytechef.ee.automation.app`, edition `ee` from
   its `application-testint.yml`, which does not set the key) — Task 5 replaces its `syncWorkerExecutor` bean with an
   `appExecutor` bean, so they need no property; the `server-app` `@SpringBootTest`s (`ServerApplicationIntTest`,
   `ServerApplicationAuthorizationServerEnabledIntTest`) load `src/main/resources/config/application.yml`, which
   imports `application-bytechef.yml`, so they get the key from Task 5's yml edit (the only test yml there,
   `application-testint.yml`, does not shadow it). No other module depends on `automation-app-service`; the
   ai-eval test configurations that touch `AsyncConfiguration` never build the facade. No further edit is needed.
6. **`APP_UPDATED` is published only when at least one editable field changed.** The facade captures the six editable
   values (`name`, `description`, `hostingTarget`, `url`, `lastDeployedDate`, `visibility`) before `appService.update`
   and compares them with the saved app.
7. **Tool invocation log:** `ToolInvocationLogFacade` (interface) lives in `platform-tool-invocation-log-api`, its impl
   in `-service` (which gains `spring-security-core`). The service keeps its tenant-wide `getToolInvocationLogs` (the
   embedded public REST `ToolInvocationApiController` also calls it) and gains `getWorkspaceToolInvocationLogs`; the
   repository's two queries gain nullable `workspace_id`/`environment` predicates. `environment` is an `INT` column
   holding the `Environment` ordinal, which is what the client's `currentEnvironmentId` is. No new index (not in the
   design). The plain `ToolInvocationLogGraphQlControllerTest` is replaced by a `@GraphQlTest` IntTest.
8. **No spool survives an oversized response.** The counting stream never throws mid-copy (a mid-copy throw would
   leave `Files.copy`'s partial target behind under a name nobody learns). Past the limit it closes the upstream,
   sets `exceeded` and reports end of stream; the storage provider completes and returns the spool's entry, and
   `HttpClientExecutor` (which already holds `tempFileStorage`) deletes that spool through
   `TempFileStorage.deleteFile(FileEntry)` and throws `HttpResponseTooLargeException`. A delete failure is logged at
   WARN through the context and attached as suppressed; it never replaces the size error. `maxResponseBytes` below 1
   is refused by the builder, so an aborted spool is never empty (the filesystem provider refuses empty files).

## Global Constraints

- Work only in `/Volumes/Data/bytechef/bytechef/.claude/worktrees/helix-bytechef-integration-086416` (the session's
  working directory). Never `cd` to the main repository. `cd client` inside the worktree is fine.
- Gradle: redirect to a log in your session scratchpad, check `$?` on its own line, then grep the log. Never pipe
  Gradle into `tail`/`grep`. Always `--continue`. Foreground only, Bash timeout `600000`, no `&`, no `sleep`. Shell
  state does not persist between Bash calls, so set `SCRATCH` in every call. Pattern:
  ```bash
  SCRATCH=<your session scratchpad directory>
  ./gradlew --continue <tasks> > "$SCRATCH/<name>.log" 2>&1
  echo "exit=$?"
  grep -E '^> Task .* FAILED' "$SCRATCH/<name>.log"
  ```
  Exit 0 and no `FAILED` line is green. Testcontainers IntTests need Docker running.
- Every task ends with `:<module>:spotlessApply` then `:<module>:check` on every Gradle module it touched (`check`
  runs `test`, `testIntegration`, Checkstyle, PMD and SpotBugs including test sources).
- Commits: explicit paths only (never `git add -A` / `git add .`), never `--amend`, never `git stash`, no
  `Co-Authored-By` trailer, no "Generated with Claude Code" line. Subjects: server commits are a plain imperative
  sentence (this branch's style, e.g. `Hide policy-hidden components and actions from the app listings`); client
  commits `client - <sentence>`; documentation commits `docs - <sentence>`. Run `git branch --show-current` before each
  commit and confirm `claude/helix-bytechef-integration-086416`.
- Java (CLAUDE.md): one blank line before control statements and after a variable modification that the next
  statement uses; no blank line before a class's closing `}`; no method chaining outside builders/streams/Optional/
  AssertJ/Mockito; descriptive names (no single letters, also in lambdas); no `//` rationale comments in code; no
  `TODO`; test methods camelCase without underscores; integration tests end in `IntTest`, unit tests in `Test`.
- Files under `server/ee/` use the ByteChef Enterprise header and carry `@version ee` in the class Javadoc:
  ```java
  /*
   * Copyright 2025 ByteChef
   *
   * Licensed under the ByteChef Enterprise license (the "Enterprise License");
   * you may not use this file except in compliance with the Enterprise License.
   */
  ```
  Files elsewhere (`sdks/`, `server/libs/`) use the Apache 2.0 header copied from a neighbouring file.
- Authorization lives on facades (`@PreAuthorize`); services stay unguarded; controllers carry no `@PreAuthorize` and
  call only the facade; facades never call repositories.
- Client: object keys sorted, interface names end in `I` or `Props`, named imports sorted, `useRef` names end in
  `Ref`, hooks ordered `useState` → `useRef` → store hooks → other hooks → derived values → `useEffect`. Operations
  (`.graphql`) and the regenerated client (`graphql.ts`, `graphql-types.ts`) are committed separately.
- Enum ordinals persisted as INT: append only (`AppAuditEvent.APP_UPDATED` goes last).

## Review Focus

1. **An oversized upstream body must release its connection and leave no spool behind.** Expected: the counting
   stream closes the upstream `InputStream` (which cancels the JDK exchange), the client deletes the partial spool,
   and a failing delete still surfaces as the size error. Pinned by
   `testBinaryBodyOverTheLimitIsAbortedAndTheUpstreamClosed` and `testDeleteFailureOfAnOversizedSpoolStillThrowsTooLarge`
   (Task 6).
2. **A body of exactly `max-response-bytes` must still pass** (off-by-one between "exceeds" and "reaches"). Pinned by
   `testBinaryBodyAtTheLimitIsStored` (Task 6).
3. **The new `bytechef.app.execution.concurrency-limit` key must bind under `ignoreUnknownFields = false`, and a
   missing or non-positive value must not boot a silently broken lane.** Pinned by
   `testAppExecutionBindsWithoutUnboundElements` (ApplicationPropertiesTest), `testAppExecutorIsNotRegisteredWithoutItsConcurrencyLimit`
   and `testAppExecutorRejectsANonPositiveLimit` (AsyncConfigurationTest) — Task 5.
4. **The automation Tool Invocations page must never fall back to the tenant-wide (admin-only) query while the
   workspace store is still empty.** Expected: both queries disabled until a workspace id exists. Pinned by
   `waits for the workspace instead of falling back to the tenant-wide log` (Task 9).
5. **A workspace member filtering by another workspace's `appId` must get nothing**, not that app's rows. Pinned by
   `testWorkspaceQueryNamingAnotherWorkspacesAppMatchesNothing` (Task 8, real PostgreSQL).

---

### Task 1: Sweep app workflow run and tool invocation log retention across every tenant (design item 1)

**Files:**
- Modify: `server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/workflow/AppWorkflowRunRetentionJob.java`
- Modify: `server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-service/src/main/java/com/bytechef/ee/platform/tool/invocation/log/ToolInvocationLogRetentionJob.java`
- Test: `server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/workflow/AppWorkflowRunRetentionJobTest.java`
- Test: `server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-service/src/test/java/com/bytechef/ee/platform/tool/invocation/log/ToolInvocationLogRetentionJobTest.java`
- Docs: `.agents/apps.md`

Both modules already depend on `:server:libs:core:tenant:tenant-api`. `TenantContext.runWithTenantId(String,
TenantContext.Runnable)` restores the previous tenant in `finally` and wraps any exception in a `RuntimeException`.
Tenant ids must match `^[a-zA-Z0-9_]+$`.

**Interfaces:**
- Produces: `AppWorkflowRunRetentionJob(ApplicationProperties, AppWorkflowService, ObjectProvider<TenantService>)`;
  `ToolInvocationLogRetentionJob(long retentionDays, ToolInvocationLogService, ObjectProvider<TenantService>)`.

- [ ] **Step 1: Write the failing tests**

Replace the whole of `AppWorkflowRunRetentionJobTest.java` with:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.app.service.AppWorkflowService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AppWorkflowRunRetentionJobTest {

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testPurgeDeletesRunsOlderThanRetentionWindow() {
        AppWorkflowService appWorkflowService = mock(AppWorkflowService.class);
        TenantService tenantService = mock(TenantService.class);

        when(tenantService.getTenantIds()).thenReturn(List.of(TenantContext.DEFAULT_TENANT_ID));
        when(appWorkflowService.deleteRunsCreatedBefore(any())).thenReturn(3);

        AppWorkflowRunRetentionJob retentionJob = new AppWorkflowRunRetentionJob(
            applicationProperties(30), appWorkflowService, tenantServiceProvider(tenantService));

        Instant before = Instant.now()
            .minus(30, ChronoUnit.DAYS);

        retentionJob.purgeExpiredRuns();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);

        verify(appWorkflowService).deleteRunsCreatedBefore(captor.capture());

        Instant after = Instant.now()
            .minus(30, ChronoUnit.DAYS);

        assertThat(captor.getValue()).isBetween(before.minusSeconds(5), after);
    }

    @Test
    void testPurgeIsSkippedWhenRetentionIsNotConfigured() {
        AppWorkflowService appWorkflowService = mock(AppWorkflowService.class);
        TenantService tenantService = mock(TenantService.class);

        AppWorkflowRunRetentionJob retentionJob = new AppWorkflowRunRetentionJob(
            applicationProperties(0), appWorkflowService, tenantServiceProvider(tenantService));

        retentionJob.purgeExpiredRuns();

        verify(appWorkflowService, never()).deleteRunsCreatedBefore(any());
        verifyNoInteractions(tenantService);
    }

    @Test
    void testPurgeSweepsEveryTenantAndAFailingTenantDoesNotStopTheRest() {
        AppWorkflowService appWorkflowService = mock(AppWorkflowService.class);
        TenantService tenantService = mock(TenantService.class);
        List<String> sweptTenantIds = new ArrayList<>();

        when(tenantService.getTenantIds()).thenReturn(List.of("tenant1", "tenant2"));
        when(appWorkflowService.deleteRunsCreatedBefore(any())).thenAnswer(invocation -> {
            String tenantId = TenantContext.getCurrentTenantId();

            sweptTenantIds.add(tenantId);

            if ("tenant1".equals(tenantId)) {
                throw new IllegalStateException("schema unavailable");
            }

            return 2;
        });

        AppWorkflowRunRetentionJob retentionJob = new AppWorkflowRunRetentionJob(
            applicationProperties(30), appWorkflowService, tenantServiceProvider(tenantService));

        retentionJob.purgeExpiredRuns();

        assertThat(sweptTenantIds).containsExactly("tenant1", "tenant2");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testPurgeRunsInTheCurrentTenantWhenNoTenantServiceIsAvailable() {
        AppWorkflowService appWorkflowService = mock(AppWorkflowService.class);

        AppWorkflowRunRetentionJob retentionJob = new AppWorkflowRunRetentionJob(
            applicationProperties(30), appWorkflowService, tenantServiceProvider(null));

        retentionJob.purgeExpiredRuns();

        verify(appWorkflowService, times(1)).deleteRunsCreatedBefore(any());
    }

    private static ApplicationProperties applicationProperties(long retentionDays) {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.App.WorkflowRun workflowRun = new ApplicationProperties.App.WorkflowRun();

        workflowRun.setRunRetentionDays(retentionDays);

        ApplicationProperties.App appProperties = new ApplicationProperties.App();

        appProperties.setWorkflow(workflowRun);

        applicationProperties.setApp(appProperties);

        return applicationProperties;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<TenantService> tenantServiceProvider(@Nullable TenantService tenantService) {
        ObjectProvider<TenantService> tenantServiceProvider = mock(ObjectProvider.class);

        when(tenantServiceProvider.getIfAvailable()).thenReturn(tenantService);

        return tenantServiceProvider;
    }
}
```

(`ApplicationProperties.App.WorkflowRun#setRunRetentionDays` takes a `long`.)

Replace the whole of `ToolInvocationLogRetentionJobTest.java` with:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ToolInvocationLogRetentionJobTest {

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testPurgeDeletesRowsOlderThanRetentionWindow() {
        ToolInvocationLogService toolInvocationLogService = mock(ToolInvocationLogService.class);
        TenantService tenantService = mock(TenantService.class);

        when(tenantService.getTenantIds()).thenReturn(List.of(TenantContext.DEFAULT_TENANT_ID));
        when(toolInvocationLogService.deleteToolInvocationLogsCreatedBefore(any())).thenReturn(3);

        ToolInvocationLogRetentionJob retentionJob = new ToolInvocationLogRetentionJob(
            90, toolInvocationLogService, tenantServiceProvider(tenantService));

        Instant before = Instant.now()
            .minus(90, ChronoUnit.DAYS);

        retentionJob.purgeExpiredLogs();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);

        verify(toolInvocationLogService).deleteToolInvocationLogsCreatedBefore(captor.capture());

        Instant cutoff = captor.getValue();

        assertThat(cutoff).isBetween(before.minusSeconds(60), before.plusSeconds(60));
    }

    @Test
    void testPurgeSweepsEveryTenantAndAFailingTenantDoesNotStopTheRest() {
        ToolInvocationLogService toolInvocationLogService = mock(ToolInvocationLogService.class);
        TenantService tenantService = mock(TenantService.class);
        List<String> sweptTenantIds = new ArrayList<>();

        when(tenantService.getTenantIds()).thenReturn(List.of("tenant1", "tenant2"));
        when(toolInvocationLogService.deleteToolInvocationLogsCreatedBefore(any())).thenAnswer(invocation -> {
            String tenantId = TenantContext.getCurrentTenantId();

            sweptTenantIds.add(tenantId);

            if ("tenant1".equals(tenantId)) {
                throw new IllegalStateException("schema unavailable");
            }

            return 4;
        });

        ToolInvocationLogRetentionJob retentionJob = new ToolInvocationLogRetentionJob(
            90, toolInvocationLogService, tenantServiceProvider(tenantService));

        retentionJob.purgeExpiredLogs();

        assertThat(sweptTenantIds).containsExactly("tenant1", "tenant2");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testPurgeRunsInTheCurrentTenantWhenNoTenantServiceIsAvailable() {
        ToolInvocationLogService toolInvocationLogService = mock(ToolInvocationLogService.class);

        ToolInvocationLogRetentionJob retentionJob = new ToolInvocationLogRetentionJob(
            90, toolInvocationLogService, tenantServiceProvider(null));

        retentionJob.purgeExpiredLogs();

        verify(toolInvocationLogService, times(1)).deleteToolInvocationLogsCreatedBefore(any());
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<TenantService> tenantServiceProvider(@Nullable TenantService tenantService) {
        ObjectProvider<TenantService> tenantServiceProvider = mock(ObjectProvider.class);

        when(tenantServiceProvider.getIfAvailable()).thenReturn(tenantService);

        return tenantServiceProvider;
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
SCRATCH=<your session scratchpad directory>
./gradlew --continue :server:ee:libs:automation:automation-app:automation-app-service:test --tests 'com.bytechef.ee.automation.app.workflow.AppWorkflowRunRetentionJobTest' :server:ee:libs:platform:platform-tool-invocation-log:platform-tool-invocation-log-service:test --tests 'com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogRetentionJobTest' > "$SCRATCH/t1-red.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED|error:' "$SCRATCH/t1-red.log"
```

Expected: non-zero exit; `compileTestJava` FAILED with "constructor ... cannot be applied to given types".

- [ ] **Step 3: Implement the per-tenant sweep in `AppWorkflowRunRetentionJob`**

Replace the class (everything after the imports) and the imports with:

```java
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.app.service.AppWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Deletes app workflow run records older than {@code bytechef.app.workflow.run-retention-days}. Runs on
 * {@code bytechef.app.workflow.run-retention-cron} (daily at 02:40 local time by default) to limit growth of
 * {@code app_workflow_run}; a purged run polls as not found. A retention of zero or fewer days disables the sweep.
 *
 * <p>
 * Every tenant is swept under its own context, because the delete runs on the tenant's schema; a tenant whose sweep
 * fails is logged and the remaining tenants are still swept. A deployment without a tenant service sweeps the
 * scheduler thread's current tenant.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AppWorkflowRunRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(AppWorkflowRunRetentionJob.class);

    private final ApplicationProperties applicationProperties;
    private final AppWorkflowService appWorkflowService;
    private final ObjectProvider<TenantService> tenantServiceObjectProvider;

    @SuppressFBWarnings("EI")
    public AppWorkflowRunRetentionJob(
        ApplicationProperties applicationProperties, AppWorkflowService appWorkflowService,
        ObjectProvider<TenantService> tenantServiceObjectProvider) {

        this.applicationProperties = applicationProperties;
        this.appWorkflowService = appWorkflowService;
        this.tenantServiceObjectProvider = tenantServiceObjectProvider;
    }

    @Scheduled(cron = "${bytechef.app.workflow.run-retention-cron:0 40 2 * * *}")
    public void purgeExpiredRuns() {
        long retentionDays = applicationProperties.getApp()
            .getWorkflow()
            .getRunRetentionDays();

        if (retentionDays <= 0) {
            log.debug("App workflow run retention is disabled");

            return;
        }

        Instant cutoff = Instant.now()
            .minus(retentionDays, ChronoUnit.DAYS);

        TenantService tenantService = tenantServiceObjectProvider.getIfAvailable();

        if (tenantService == null) {
            purgeExpiredRunsForCurrentTenant(cutoff);

            return;
        }

        for (String tenantId : tenantService.getTenantIds()) {
            try {
                TenantContext.runWithTenantId(tenantId, () -> purgeExpiredRunsForCurrentTenant(cutoff));
            } catch (RuntimeException exception) {
                log.warn("App workflow run retention sweep failed for tenant {}", tenantId, exception);
            }
        }
    }

    private void purgeExpiredRunsForCurrentTenant(Instant cutoff) {
        int deleted = appWorkflowService.deleteRunsCreatedBefore(cutoff);

        if (deleted > 0) {
            log.info(
                "App workflow run retention: deleted {} row(s) older than {} for tenant {}", deleted, cutoff,
                TenantContext.getCurrentTenantId());
        } else {
            log.debug(
                "App workflow run retention: no rows older than {} for tenant {}", cutoff,
                TenantContext.getCurrentTenantId());
        }
    }
}
```

- [ ] **Step 4: Implement the per-tenant sweep in `ToolInvocationLogRetentionJob`**

Replace the imports and class with:

```java
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Deletes tool invocation log rows older than {@code bytechef.tool-invocation-log.retention-days} (default 90). Runs
 * daily at 02:30 local time to limit growth of {@code tool_invocation_log}.
 *
 * <p>
 * Every tenant is swept under its own context, because the delete runs on the tenant's schema; a tenant whose sweep
 * fails is logged and the remaining tenants are still swept. A deployment without a tenant service (the distributed
 * {@code execution-app} carries none) sweeps the scheduler thread's current tenant.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ToolInvocationLogRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(ToolInvocationLogRetentionJob.class);

    private final long retentionDays;
    private final ObjectProvider<TenantService> tenantServiceObjectProvider;
    private final ToolInvocationLogService toolInvocationLogService;

    @SuppressFBWarnings("EI")
    public ToolInvocationLogRetentionJob(
        @Value("${bytechef.tool-invocation-log.retention-days:90}") long retentionDays,
        ToolInvocationLogService toolInvocationLogService,
        ObjectProvider<TenantService> tenantServiceObjectProvider) {

        this.retentionDays = retentionDays;
        this.tenantServiceObjectProvider = tenantServiceObjectProvider;
        this.toolInvocationLogService = toolInvocationLogService;
    }

    @Scheduled(cron = "${bytechef.tool-invocation-log.retention-cron:0 30 2 * * *}")
    public void purgeExpiredLogs() {
        Instant cutoff = Instant.now()
            .minus(retentionDays, ChronoUnit.DAYS);

        TenantService tenantService = tenantServiceObjectProvider.getIfAvailable();

        if (tenantService == null) {
            purgeExpiredLogsForCurrentTenant(cutoff);

            return;
        }

        for (String tenantId : tenantService.getTenantIds()) {
            try {
                TenantContext.runWithTenantId(tenantId, () -> purgeExpiredLogsForCurrentTenant(cutoff));
            } catch (RuntimeException exception) {
                log.warn("Tool invocation log retention sweep failed for tenant {}", tenantId, exception);
            }
        }
    }

    private void purgeExpiredLogsForCurrentTenant(Instant cutoff) {
        int deleted = toolInvocationLogService.deleteToolInvocationLogsCreatedBefore(cutoff);

        if (deleted > 0) {
            log.info(
                "Tool invocation log retention: deleted {} row(s) older than {} for tenant {}", deleted, cutoff,
                TenantContext.getCurrentTenantId());
        } else {
            log.debug(
                "Tool invocation log retention: no rows older than {} for tenant {}", cutoff,
                TenantContext.getCurrentTenantId());
        }
    }
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Same command as Step 2 with log `t1-green.log`. Expected: `exit=0`, no `FAILED` line, both suites
`Results: SUCCESS`.

- [ ] **Step 6: Update `.agents/apps.md`**

In the "Run ownership" bullet, replace `A retention of zero or fewer days disables the sweep rather than deleting every row.`
with `A retention of zero or fewer days disables the sweep rather than deleting every row. The sweep runs once per
tenant under that tenant's context (a failing tenant is logged and skipped), as does
\`ToolInvocationLogRetentionJob\`'s.` (without the backslashes).

Delete the whole bullet that starts `- **Known gap: retention is single-schema.**` (three lines, ending `(ruling R8).`).

- [ ] **Step 7: Format, full check, commit**

```bash
SCRATCH=<your session scratchpad directory>
./gradlew --continue :server:ee:libs:automation:automation-app:automation-app-service:spotlessApply :server:ee:libs:platform:platform-tool-invocation-log:platform-tool-invocation-log-service:spotlessApply > "$SCRATCH/t1-spotless.log" 2>&1
echo "exit=$?"
./gradlew --continue :server:ee:libs:automation:automation-app:automation-app-service:check :server:ee:libs:platform:platform-tool-invocation-log:platform-tool-invocation-log-service:check > "$SCRATCH/t1-check.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' "$SCRATCH/t1-check.log"
```

Expected: both `exit=0`, no `FAILED`. Then:

```bash
git branch --show-current
git add server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/workflow/AppWorkflowRunRetentionJob.java server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/workflow/AppWorkflowRunRetentionJobTest.java server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-service/src/main/java/com/bytechef/ee/platform/tool/invocation/log/ToolInvocationLogRetentionJob.java server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-service/src/test/java/com/bytechef/ee/platform/tool/invocation/log/ToolInvocationLogRetentionJobTest.java
git commit -m "Sweep app workflow run and tool invocation log retention across every tenant"
git add .agents/apps.md
git commit -m "docs - Note the per-tenant retention sweeps in the Apps deep-dive"
```

---

### Task 2: Refuse system deployments when resolving an app's workflow binding (design item 4)

**Files:**
- Modify: `server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/workflow/AppCallableWorkflowResolver.java:107-140`
- Test: `server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/workflow/AppCallableWorkflowResolverTest.java`
- Docs: `.agents/apps.md`

`resolve()` is the one definition every caller uses: `AppWorkflowServiceImpl.bind` (line 70) refuses a binding
`resolve()` cannot resolve, and `AppWorkflowExecutionFacadeImpl` resolves on every run and listing — so closing
`resolve()` closes binding, listing and execution together. Prefixes come from
`com.bytechef.automation.configuration.domain.SystemProjects` (all `public static final String` literals, so usable in
`@ValueSource`): deployment-name markers `API_COLLECTION_DEPLOYMENT_NAME_PREFIX` (`__API_COLLECTION__`),
`MCP_SERVER_DEPLOYMENT_NAME_PREFIX` (`__MCP_SERVER__`), `A2A_SERVER_DEPLOYMENT_NAME_PREFIX` (`__A2A_SERVER__`); project
names: `SystemProjects.isSystemProjectName(String)` (`__KNOWLEDGE_BASE__`, `__CONTEXT_STORE__`,
`__EMBEDDED_AUTOMATION__`) plus `EMBEDDED_DEPLOYMENT_NAME_PREFIX` (`__EMBEDDED__`). `ProjectDeployment.getName()` and
`Project.getName()` can be null (the test helpers leave them unset).

**Interfaces:**
- Consumes: `SystemProjects` constants and `isSystemProjectName(String)`.
- Produces: no signature change; `resolve(App, long, String)` now returns `Optional.empty()` for system deployments.

- [ ] **Step 1: Write the failing tests**

Add imports to `AppCallableWorkflowResolverTest`:

```java
import com.bytechef.automation.configuration.domain.SystemProjects;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
```

Add these tests after `testMissingDeploymentIsUnresolvable`:

```java
    @ParameterizedTest
    @ValueSource(strings = {
        SystemProjects.API_COLLECTION_DEPLOYMENT_NAME_PREFIX, SystemProjects.MCP_SERVER_DEPLOYMENT_NAME_PREFIX,
        SystemProjects.A2A_SERVER_DEPLOYMENT_NAME_PREFIX
    })
    void testSystemDeploymentIsUnresolvable(String deploymentNamePrefix) {
        ProjectDeployment projectDeployment = deployment(DEPLOYMENT_ID, Environment.PRODUCTION, true);

        projectDeployment.setName(deploymentNamePrefix + "orders");

        stubCallablePath(projectDeployment, project(WORKSPACE_ID), true);

        assertThat(resolver.resolve(app, DEPLOYMENT_ID, WORKFLOW_UUID)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        SystemProjects.KNOWLEDGE_BASE_NAME_PREFIX, SystemProjects.CONTEXT_STORE_NAME_PREFIX,
        SystemProjects.EMBEDDED_AUTOMATION_NAME_PREFIX, SystemProjects.EMBEDDED_DEPLOYMENT_NAME_PREFIX
    })
    void testDeploymentOfASystemProjectIsUnresolvable(String projectNamePrefix) {
        Project project = project(WORKSPACE_ID);

        project.setName(projectNamePrefix + "7");

        stubCallablePath(deployment(DEPLOYMENT_ID, Environment.PRODUCTION, true), project, true);

        assertThat(resolver.resolve(app, DEPLOYMENT_ID, WORKFLOW_UUID)).isEmpty();
    }

    @Test
    void testUserNamesThatOnlyContainAMarkerStayResolvable() {
        ProjectDeployment projectDeployment = deployment(DEPLOYMENT_ID, Environment.PRODUCTION, true);
        Project project = project(WORKSPACE_ID);

        projectDeployment.setName("orders " + SystemProjects.MCP_SERVER_DEPLOYMENT_NAME_PREFIX);
        project.setName("my " + SystemProjects.EMBEDDED_DEPLOYMENT_NAME_PREFIX + " project");

        stubCallablePath(projectDeployment, project, true);

        assertThat(resolver.resolve(app, DEPLOYMENT_ID, WORKFLOW_UUID)).isPresent();
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
SCRATCH=<your session scratchpad directory>
./gradlew --continue :server:ee:libs:automation:automation-app:automation-app-service:test --tests 'com.bytechef.ee.automation.app.workflow.AppCallableWorkflowResolverTest' > "$SCRATCH/t2-red.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED|FAILED$' "$SCRATCH/t2-red.log"
```

Expected: non-zero exit; the seven parameterized system cases FAIL ("Expecting empty but was Optional[...]");
`testUserNamesThatOnlyContainAMarkerStayResolvable` passes.

- [ ] **Step 3: Implement**

Add `import com.bytechef.automation.configuration.domain.SystemProjects;` to `AppCallableWorkflowResolver`. Replace
`resolve` and `fetchProjectInWorkspace` (lines 107-140) with:

```java
    public Optional<CallableWorkflow> resolve(App app, long projectDeploymentId, String workflowUuid) {
        Optional<ProjectDeployment> projectDeployment = projectDeploymentService.fetchProjectDeployment(
            projectDeploymentId);

        if (projectDeployment.isEmpty() || !projectDeployment.get()
            .isEnabled()
            || projectDeployment.get()
                .getEnvironmentId() != app.getEnvironmentId()
            || isSystemDeployment(projectDeployment.get())) {

            return Optional.empty();
        }

        Optional<Project> project = fetchProjectInWorkspace(app, projectDeployment.get());

        if (project.isEmpty() || isSystemProject(project.get())) {
            return Optional.empty();
        }

        Optional<String> workflowId;

        try {
            workflowId = projectWorkflowService.fetchProjectWorkflowWorkflowId(projectDeploymentId, workflowUuid);
        } catch (IllegalArgumentException illegalArgumentException) {
            return Optional.empty();
        }

        return workflowId
            .flatMap(id -> projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(projectDeploymentId, id))
            .filter(ProjectDeploymentWorkflow::isEnabled)
            .flatMap(this::toCallableWorkflow);
    }

    private Optional<Project> fetchProjectInWorkspace(App app, ProjectDeployment projectDeployment) {
        return projectService.fetchProject(projectDeployment.getProjectId())
            .filter(project -> Objects.equals(project.getWorkspaceId(), app.getWorkspaceId()));
    }

    /**
     * The deployments backing API collections, MCP servers and A2A servers, which every deployment listing hides and
     * which no app may bind.
     */
    private static boolean isSystemDeployment(ProjectDeployment projectDeployment) {
        String name = projectDeployment.getName();

        return name != null && (name.startsWith(SystemProjects.API_COLLECTION_DEPLOYMENT_NAME_PREFIX)
            || name.startsWith(SystemProjects.MCP_SERVER_DEPLOYMENT_NAME_PREFIX)
            || name.startsWith(SystemProjects.A2A_SERVER_DEPLOYMENT_NAME_PREFIX));
    }

    /**
     * Projects a feature provisions for itself, including the embedded deployment marker the binding picker excludes
     * through {@code getProjectDeployments(false, ...)}.
     */
    private static boolean isSystemProject(Project project) {
        String name = project.getName();

        return SystemProjects.isSystemProjectName(name)
            || (name != null && name.startsWith(SystemProjects.EMBEDDED_DEPLOYMENT_NAME_PREFIX));
    }
```

In the class Javadoc, replace `its deployment workflow exists and` with `is not a system deployment (an API collection,
MCP server or A2A server deployment, or a deployment of a system project), its deployment workflow exists and`.

- [ ] **Step 4: Run the tests to verify they pass**

Same command as Step 2 with log `t2-green.log`. Expected: `exit=0`, no `FAILED`.

- [ ] **Step 5: Update `.agents/apps.md`**

Replace the bullet starting `- **Known gap: \`resolve\` is wider than the bindable list.**` (three lines, ending
`(ruling R7). Left as is; see the multi-tenant follow-up.`) with:

```markdown
- **`resolve` refuses system deployments.** A deployment named with an API collection, MCP server or A2A server marker,
  or one whose project is a system project (`SystemProjects.isSystemProjectName` or the `__EMBEDDED__` marker), never
  resolves — the same set `getBindableWorkflows` hides. Bind validates through `resolve`, so such a deployment cannot
  be bound through the REST `PUT /apps/{id}/workflows/{workflowKey}` either, and an existing binding to one stops
  executing.
```

- [ ] **Step 6: Format, full check, commit**

```bash
SCRATCH=<your session scratchpad directory>
./gradlew --continue :server:ee:libs:automation:automation-app:automation-app-service:spotlessApply > "$SCRATCH/t2-spotless.log" 2>&1
echo "exit=$?"
./gradlew --continue :server:ee:libs:automation:automation-app:automation-app-service:check > "$SCRATCH/t2-check.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' "$SCRATCH/t2-check.log"
git branch --show-current
git add server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/workflow/AppCallableWorkflowResolver.java server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/workflow/AppCallableWorkflowResolverTest.java
git commit -m "Refuse system deployments when resolving an app's workflow binding"
git add .agents/apps.md
git commit -m "docs - Record that app workflow bindings refuse system deployments"
```

---

### Task 3: Keep the app action and tool listings when one definition's schema fails (design item 5)

**Files:**
- Modify: `server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/facade/AppExecutionFacadeImpl.java` (logger, `getActions()` at 175-219, `getTools()` at 239-283)
- Test: `server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/facade/AppExecutionFacadeTest.java`
- Docs: `.agents/apps.md`

`AppActionDTO.inputSchema` is `Map<String, Object>`; `AppToolDTO.parameters` is a JSON `String`. The class has no
logger yet; `automation-app-service` already depends on `org.slf4j:slf4j-api`.

**Interfaces:**
- Produces: listings unchanged in shape; a definition whose schema generation throws a `RuntimeException` is listed
  with `{"type": "object"}` (actions: `Map.of("type", "object")`; tools: `"{\"type\":\"object\"}"`).

- [ ] **Step 1: Write the failing tests**

Add to `AppExecutionFacadeTest` (after `testActionsListLeavesOutAnActionHiddenByAVisibilityProvider`):

```java
    @Test
    void testActionWhoseSchemaFailsIsListedWithAnOpenObjectSchema() {
        ActionDefinition create = new ActionDefinition(ComponentDsl.action("createRecord"), "salesforce", 3);
        ActionDefinition broken = mock(ActionDefinition.class);

        when(broken.getName()).thenReturn("updateRecord");
        when(broken.getDescription()).thenReturn("Updates a record");
        when(broken.getProperties()).thenThrow(new IllegalStateException("unsupported property"));

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getActions()).thenReturn(List.of(create, broken));
        when(componentDefinitionService.fetchComponentDefinition("salesforce", null))
            .thenReturn(Optional.of(componentDefinition));

        List<AppActionDTO> actions = facade.getActions();

        assertThat(actions).extracting(AppActionDTO::actionName)
            .containsExactly("createRecord", "updateRecord");
        assertThat(actions.get(1)
            .inputSchema()).isEqualTo(Map.of("type", "object"));
        assertThat(actions.get(1)
            .description()).isEqualTo("Updates a record");
    }

    @Test
    void testToolWhoseSchemaFailsIsListedWithAnOpenObjectSchema() {
        ClusterElementDefinition search = mock(ClusterElementDefinition.class);

        when(search.getName()).thenReturn("searchRecords");
        when(search.getProperties()).thenThrow(new IllegalStateException("unsupported property"));
        when(clusterElementDefinitionService.getClusterElementDefinitions("salesforce", 3, BaseToolFunction.TOOLS))
            .thenReturn(List.of(search));

        List<AppToolDTO> tools = facade.getTools();

        assertThat(tools).extracting(AppToolDTO::name)
            .containsExactly("CRM__SEARCH_RECORDS");
        assertThat(tools.getFirst()
            .parameters()).isEqualTo("{\"type\":\"object\"}");
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
SCRATCH=<your session scratchpad directory>
./gradlew --continue :server:ee:libs:automation:automation-app:automation-app-service:test --tests 'com.bytechef.ee.automation.app.facade.AppExecutionFacadeTest' > "$SCRATCH/t3-red.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED|FAILED$' "$SCRATCH/t3-red.log"
```

Expected: non-zero exit; both new tests FAIL with `IllegalStateException: unsupported property`.

- [ ] **Step 3: Implement**

Add imports `org.slf4j.Logger` and `org.slf4j.LoggerFactory`. Add as the first constants of the class (before
`MAX_CAUSE_DEPTH`):

```java
    private static final Logger log = LoggerFactory.getLogger(AppExecutionFacadeImpl.class);

    private static final String OPEN_OBJECT_SCHEMA_JSON = "{\"type\":\"object\"}";
```

In `getActions()` replace

```java
                actions.add(
                    new AppActionDTO(
                        binding.getBindingKey(), connection.getComponentName(), definition.getName(),
                        definition.getDescription(),
                        ActionInputSchemaGenerator.generateInputSchema(definition.getProperties())));
```

with

```java
                actions.add(
                    new AppActionDTO(
                        binding.getBindingKey(), connection.getComponentName(), definition.getName(),
                        definition.getDescription(), generateActionInputSchema(connection.getComponentName(), definition)));
```

In `getTools()` replace

```java
                        connection.getComponentName(), definition.getName(), definition.getDescription(),
                        JsonSchemaGeneratorUtils.generateInputSchema(
                            ToolPropertyUtils.withoutToolOverrideProperties(definition.getProperties()))));
```

with

```java
                        connection.getComponentName(), definition.getName(), definition.getDescription(),
                        generateToolInputSchema(connection.getComponentName(), definition)));
```

Add these private static methods directly after `flatToolName(...)`:

```java
    /**
     * One action whose properties cannot be turned into a schema must not take down the listing of every binding: it
     * is listed with an open object schema and the failure is logged with the component and action names.
     */
    private static Map<String, Object> generateActionInputSchema(String componentName, ActionDefinition definition) {
        try {
            return ActionInputSchemaGenerator.generateInputSchema(definition.getProperties());
        } catch (RuntimeException runtimeException) {
            log.warn(
                "Unable to generate the input schema of action '{}' of component '{}', listing it as an open object",
                definition.getName(), componentName, runtimeException);

            return Map.of("type", "object");
        }
    }

    /**
     * The tool counterpart of {@link #generateActionInputSchema(String, ActionDefinition)}.
     */
    private static String generateToolInputSchema(String componentName, ClusterElementDefinition definition) {
        try {
            return JsonSchemaGeneratorUtils.generateInputSchema(
                ToolPropertyUtils.withoutToolOverrideProperties(definition.getProperties()));
        } catch (RuntimeException runtimeException) {
            log.warn(
                "Unable to generate the input schema of tool '{}' of component '{}', listing it as an open object",
                definition.getName(), componentName, runtimeException);

            return OPEN_OBJECT_SCHEMA_JSON;
        }
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Same command as Step 2 with log `t3-green.log`. Expected: `exit=0`, no `FAILED`.

- [ ] **Step 5: Update `.agents/apps.md`**

In the "Action listing" section, after the bullet that starts `- **Listings do not gate on connection state.**`, add:

```markdown
- **One bad definition does not fail a listing.** `getActions()` and `getTools()` catch a `RuntimeException` from
  schema generation per definition, list that action or tool with `{"type": "object"}`, and log a WARN naming the
  component and the action or tool.
```

- [ ] **Step 6: Format, full check, commit**

```bash
SCRATCH=<your session scratchpad directory>
./gradlew --continue :server:ee:libs:automation:automation-app:automation-app-service:spotlessApply > "$SCRATCH/t3-spotless.log" 2>&1
echo "exit=$?"
./gradlew --continue :server:ee:libs:automation:automation-app:automation-app-service:check > "$SCRATCH/t3-check.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' "$SCRATCH/t3-check.log"
git branch --show-current
git add server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/facade/AppExecutionFacadeImpl.java server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/facade/AppExecutionFacadeTest.java
git commit -m "Keep the app action and tool listings when one definition's schema fails"
git add .agents/apps.md
git commit -m "docs - Note the per-definition schema fallback in the app listings"
```

---

### Task 4: Audit app updates with the fields that changed (design item 6)

**Files:**
- Modify: `server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/audit/AppAuditEvent.java:68-74`
- Modify: `server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/facade/AppFacadeImpl.java:170-181` (plus private helpers)
- Test: `server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/facade/AppFacadeTest.java`
- Docs: `.agents/apps.md`, `docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md`

`AppServiceImpl.update` loads the app through `getApp(id)` and mutates that instance, so the facade must capture the
previous values as immutable values (strings, enums, `Instant`) before calling it. `AppAuditPublisher.publish` adds
`appId`; the platform-audit converter stores each value through `toString()`, so a `List<String>` persists as
`[name, visibility]`.

**Interfaces:**
- Produces: `AppAuditEvent.APP_UPDATED` (last constant). Payload keys: `changedFields` (`List<String>`, order `name`,
  `description`, `hostingTarget`, `url`, `lastDeployedDate`, `visibility`), and `name` (String), `visibility` /
  `hostingTarget` (enum names) only when that field changed.

- [ ] **Step 1: Write the failing tests**

In `AppFacadeTest`, add `import static org.mockito.Mockito.never;`. Replace `updateWithNullFieldsPassesNothingToChangeAndSkipsTheVisibilityCheck`
and `updatePassesTheEditableFieldsThroughAndReturnsTheSavedApp` with:

```java
    @Test
    void updateWithNullFieldsPassesNothingToChangeAndSkipsTheVisibilityCheck() {
        when(appService.getApp(1L)).thenReturn(persisted(new App(), 1L));
        when(appService.update(1L, null, null, null, null, null, null)).thenReturn(persisted(new App(), 1L));

        appFacade.updateApp(1L, null, null, null, null, null, null);

        verify(appService).update(1L, null, null, null, null, null, null);
    }

    @Test
    void updatePassesTheEditableFieldsThroughAndReturnsTheSavedApp() {
        Instant deployed = Instant.parse("2026-10-01T00:00:00Z");
        App saved = persisted(new App(), 1L);

        when(appService.getApp(1L)).thenReturn(persisted(new App(), 1L));
        when(
            appService.update(
                1L, "crm", "desc", AppHostingTarget.VERCEL, "https://x.test", deployed, ResourceVisibility.WORKSPACE))
                    .thenReturn(saved);

        AppDTO result = appFacade.updateApp(
            1L, "crm", "desc", AppHostingTarget.VERCEL, "https://x.test", deployed, ResourceVisibility.WORKSPACE);

        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void updatePublishesTheChangedFieldsWithTheNewNameAndVisibility() {
        App current = persisted(new App(), 1L);

        current.setHostingTarget(AppHostingTarget.VERCEL);
        current.setVisibility(ResourceVisibility.PRIVATE);

        App saved = persisted(new App(), 1L);

        saved.setName("crm-2");
        saved.setDescription("New description");
        saved.setHostingTarget(AppHostingTarget.VERCEL);
        saved.setVisibility(ResourceVisibility.WORKSPACE);

        when(appService.getApp(1L)).thenReturn(current);
        when(appService.update(1L, "crm-2", "New description", null, null, null, ResourceVisibility.WORKSPACE))
            .thenReturn(saved);

        appFacade.updateApp(1L, "crm-2", "New description", null, null, null, ResourceVisibility.WORKSPACE);

        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(appAuditPublisher).publish(eq(AppAuditEvent.APP_UPDATED), eq(1L), dataCaptor.capture());

        assertThat(dataCaptor.getValue()).containsOnly(
            Map.entry("changedFields", List.of("name", "description", "visibility")),
            Map.entry("name", "crm-2"),
            Map.entry("visibility", "WORKSPACE"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void updatePublishesTheNewHostingTargetWhenOnlyItChanged() {
        App current = persisted(new App(), 1L);

        current.setHostingTarget(AppHostingTarget.VERCEL);

        App saved = persisted(new App(), 1L);

        saved.setHostingTarget(AppHostingTarget.SELF_HOSTED);

        when(appService.getApp(1L)).thenReturn(current);
        when(appService.update(1L, null, null, AppHostingTarget.SELF_HOSTED, null, null, null)).thenReturn(saved);

        appFacade.updateApp(1L, null, null, AppHostingTarget.SELF_HOSTED, null, null, null);

        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(appAuditPublisher).publish(eq(AppAuditEvent.APP_UPDATED), eq(1L), dataCaptor.capture());

        assertThat(dataCaptor.getValue()).containsOnly(
            Map.entry("changedFields", List.of("hostingTarget")), Map.entry("hostingTarget", "SELF_HOSTED"));
    }

    @Test
    void updateThatChangesNothingPublishesNoEvent() {
        when(appService.getApp(1L)).thenReturn(persisted(new App(), 1L));
        when(appService.update(1L, "crm", null, null, null, null, null)).thenReturn(persisted(new App(), 1L));

        appFacade.updateApp(1L, "crm", null, null, null, null, null);

        verify(appAuditPublisher, never()).publish(any(), anyLong(), anyMap());
    }
```

(`persisted(...)` already sets the name `crm`; `ArgumentCaptor`, `eq`, `anyLong`, `anyMap`, `any` are already imported.)

- [ ] **Step 2: Run the tests to verify they fail**

```bash
SCRATCH=<your session scratchpad directory>
./gradlew --continue :server:ee:libs:automation:automation-app:automation-app-service:test --tests 'com.bytechef.ee.automation.app.facade.AppFacadeTest' > "$SCRATCH/t4-red.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED|error:' "$SCRATCH/t4-red.log"
```

Expected: `compileTestJava` FAILED with "cannot find symbol ... APP_UPDATED".

- [ ] **Step 3: Implement**

In `AppAuditEvent`, replace

```java
    /**
     * A workflow binding was removed. Payload: {@code workflowKey}.
     */
    APP_WORKFLOW_UNBOUND
}
```

with

```java
    /**
     * A workflow binding was removed. Payload: {@code workflowKey}.
     */
    APP_WORKFLOW_UNBOUND,

    /**
     * An app's editable fields were changed. Payload: {@code changedFields}, the names of the fields whose value
     * changed among {@code name}, {@code description}, {@code hostingTarget}, {@code url}, {@code lastDeployedDate}
     * and {@code visibility}; plus the new {@code name}, {@code visibility} and {@code hostingTarget} when that field
     * changed. Published only when at least one field changed.
     */
    APP_UPDATED
}
```

In `AppFacadeImpl`, add `import java.util.ArrayList;` and replace `updateApp` with:

```java
    @Override
    @PreAuthorize("hasPermission(#id, 'App', 'APP_MANAGE')")
    public AppDTO updateApp(
        long id, @Nullable String name, @Nullable String description, @Nullable AppHostingTarget hostingTarget,
        @Nullable String url, @Nullable Instant lastDeployedDate, @Nullable ResourceVisibility visibility) {

        if (visibility != null) {
            checkVisibilitySupported(visibility);
        }

        EditableFields previousFields = EditableFields.of(appService.getApp(id));

        App app = appService.update(id, name, description, hostingTarget, url, lastDeployedDate, visibility);

        List<String> changedFieldNames = previousFields.getChangedFieldNames(EditableFields.of(app));

        if (!changedFieldNames.isEmpty()) {
            appAuditPublisher.publish(AppAuditEvent.APP_UPDATED, id, toUpdatedAuditData(changedFieldNames, app));
        }

        return AppDTO.of(app);
    }
```

Add these members just before the class's existing `private void checkVisibilitySupported(...)`:

```java
    private static Map<String, Object> toUpdatedAuditData(List<String> changedFieldNames, App app) {
        Map<String, Object> data = new HashMap<>();

        data.put("changedFields", changedFieldNames);

        if (changedFieldNames.contains("name")) {
            data.put("name", app.getName());
        }

        if (changedFieldNames.contains("hostingTarget")) {
            data.put("hostingTarget", String.valueOf(app.getHostingTarget()));
        }

        if (changedFieldNames.contains("visibility")) {
            data.put("visibility", String.valueOf(app.getVisibility()));
        }

        return data;
    }
```

and, as the last member of the class (no blank line before the closing `}`):

```java
    /**
     * The values an update may change, copied out of the entity because the service mutates the instance it loads.
     */
    private record EditableFields(
        @Nullable String name, @Nullable String description, @Nullable AppHostingTarget hostingTarget,
        @Nullable String url, @Nullable Instant lastDeployedDate, @Nullable ResourceVisibility visibility) {

        static EditableFields of(App app) {
            return new EditableFields(
                app.getName(), app.getDescription(), app.getHostingTarget(), app.getUrl(), app.getLastDeployedDate(),
                app.getVisibility());
        }

        List<String> getChangedFieldNames(EditableFields updatedFields) {
            List<String> changedFieldNames = new ArrayList<>();

            if (!Objects.equals(name, updatedFields.name)) {
                changedFieldNames.add("name");
            }

            if (!Objects.equals(description, updatedFields.description)) {
                changedFieldNames.add("description");
            }

            if (hostingTarget != updatedFields.hostingTarget) {
                changedFieldNames.add("hostingTarget");
            }

            if (!Objects.equals(url, updatedFields.url)) {
                changedFieldNames.add("url");
            }

            if (!Objects.equals(lastDeployedDate, updatedFields.lastDeployedDate)) {
                changedFieldNames.add("lastDeployedDate");
            }

            if (visibility != updatedFields.visibility) {
                changedFieldNames.add("visibility");
            }

            return changedFieldNames;
        }
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Same command as Step 2 with log `t4-green.log`. Expected: `exit=0`, no `FAILED`.

- [ ] **Step 5: Update the docs**

`.agents/apps.md`: replace `\`updateApp\` publishes no audit event (no spec event covers it).` with
`\`updateApp\` publishes \`APP_UPDATED\` when at least one editable field changed: \`changedFields\` lists them, and the
new \`name\`, \`visibility\` and \`hostingTarget\` ride along when they changed.` (without the backslashes).

`docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md`: replace
`\`APP_CONNECTION_BOUND\`, \`APP_CONNECTION_UNBOUND\`, \`APP_OPERATIONS_CHANGED\`. \`APP_CONNECTION_UNBOUND\` is published`
with
`\`APP_CONNECTION_BOUND\`, \`APP_CONNECTION_UNBOUND\`, \`APP_OPERATIONS_CHANGED\`, \`APP_UPDATED\` (payload
\`changedFields\` plus the new \`name\`, \`visibility\` and \`hostingTarget\` when changed; only when something changed).
\`APP_CONNECTION_UNBOUND\` is published` (without the backslashes; rewrap to 120 columns).

- [ ] **Step 6: Format, full check, commit**

```bash
SCRATCH=<your session scratchpad directory>
./gradlew --continue :server:ee:libs:automation:automation-app:automation-app-service:spotlessApply > "$SCRATCH/t4-spotless.log" 2>&1
echo "exit=$?"
./gradlew --continue :server:ee:libs:automation:automation-app:automation-app-service:check > "$SCRATCH/t4-check.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' "$SCRATCH/t4-check.log"
git branch --show-current
git add server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/audit/AppAuditEvent.java server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/facade/AppFacadeImpl.java server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/facade/AppFacadeTest.java
git commit -m "Audit app updates with the fields that changed"
git add .agents/apps.md docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md
git commit -m "docs - Add the APP_UPDATED audit event to the Apps spec and deep-dive"
```

---

### Task 5: Run app actions, tools and proxy calls on a dedicated app executor (design item 3)

**Files:**
- Modify: `server/libs/config/async-config/src/main/java/com/bytechef/async/config/AsyncConfiguration.java`
- Modify: `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java` (`App.Execution`, ~line 3689)
- Modify: `server/apps/server-app/src/main/resources/config/application-bytechef.yml:77-79`
- Modify: `server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/facade/AppExecutionFacadeImpl.java`
- Modify: `server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/proxy/AppProxyExecutor.java:80`
- Modify: `server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/config/AppIntTestConfiguration.java:60-62,111-122`
- Test: `server/libs/config/async-config/src/test/java/com/bytechef/async/config/AsyncConfigurationTest.java`
- Test: `server/libs/config/app-config/src/test/java/com/bytechef/config/ApplicationPropertiesTest.java`
- Test: `server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/facade/AppExecutionFacadeTest.java`
- Docs: `CLAUDE.md:554-573`, `.agents/apps.md`, `docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md`

`AsyncConfiguration` reads its limits from `Environment` (`async-config` does not depend on `app-config`);
`ApplicationProperties` binds `bytechef.*` with `ignoreUnknownFields = false`, so the yml key needs a field. Only
`server-app`'s `application-bytechef.yml` (imported by its `application.yml`) carries `bytechef.app.*`; no test yml sets
it. `AppWorkflowExecutionFacadeImpl` is untouched (workflow runs go through the job path).

**Interfaces:**
- Produces: `AsyncConfiguration.APP_EXECUTOR = "appExecutor"`; package-private `AsyncTaskExecutor appExecutor()`;
  `ApplicationProperties.App.Execution#getConcurrencyLimit()/setConcurrencyLimit(int)`;
  `AppExecutionFacadeImpl` constructor parameter `@Qualifier(AsyncConfiguration.APP_EXECUTOR) AsyncTaskExecutor appExecutor`
  (same position and type as before, so positional callers are unchanged).

- [ ] **Step 1: Write the failing tests**

`AsyncConfigurationTest`: add `import static org.junit.jupiter.api.Assertions.assertThrows;`. In `setUp()`, add
`environment.setProperty("bytechef.app.execution.concurrency-limit", "4");` before the worker property. In
`testContextPropagatingTaskDecoratorAppliedByEveryExecutor`, add
`recordingEnvironment.setProperty("bytechef.app.execution.concurrency-limit", "4");`, make the list

```java
        List<TaskExecutor> executors = List.of(
            recordingAsyncConfiguration.getAsyncExecutor(), recordingAsyncConfiguration.messageEventExecutor(),
            recordingAsyncConfiguration.syncWorkerExecutor(), recordingAsyncConfiguration.workerExecutor(),
            recordingAsyncConfiguration.appExecutor());
```

and change `assertEquals(4, recordingContextPropagatingTaskDecorator.decorateCount.get());` to
`assertEquals(5, ...)`. In `testEveryExecutorPropagatesTenantId` make the list

```java
        List<TaskExecutor> executors = List.of(
            asyncConfiguration.getAsyncExecutor(), asyncConfiguration.messageEventExecutor(),
            asyncConfiguration.syncWorkerExecutor(), asyncConfiguration.workerExecutor(),
            asyncConfiguration.appExecutor());
```

Add after `testSyncWorkerExecutorIsADistinctBeanWithItsOwnLimit`:

```java
    @Test
    void testAppExecutorLimitMatchesAppConcurrencyProperty() {
        SimpleAsyncTaskExecutor appExecutor = (SimpleAsyncTaskExecutor) asyncConfiguration.appExecutor();

        assertEquals(4, appExecutor.getConcurrencyLimit());
    }

    @Test
    void testAppExecutorIsADistinctBeanWithItsOwnLimit() {
        new ApplicationContextRunner()
            .withUserConfiguration(AsyncConfiguration.class)
            .withBean(ContextPropagatingTaskDecorator.class, ContextPropagatingTaskDecorator::new)
            .withBean(TaskExecutionProperties.class, TaskExecutionProperties::new)
            .withPropertyValues(
                "bytechef.app.execution.concurrency-limit=4",
                "bytechef.worker.task.subscriptions.default=3",
                "bytechef.worker.task.sync-concurrency-limit=2",
                "spring.threads.virtual.enabled=true")
            .run(context -> {
                assertNull(context.getStartupFailure(), "context failed to start");

                SimpleAsyncTaskExecutor appExecutor = context.getBean("appExecutor", SimpleAsyncTaskExecutor.class);
                SimpleAsyncTaskExecutor syncWorkerExecutor = context.getBean(
                    "syncWorkerExecutor", SimpleAsyncTaskExecutor.class);

                assertNotSame(
                    appExecutor, syncWorkerExecutor,
                    "apps must have their own lane, or a burst from apps starves the editor's Test button");
                assertEquals(4, appExecutor.getConcurrencyLimit());
                assertEquals(2, syncWorkerExecutor.getConcurrencyLimit());
            });
    }

    @Test
    void testAppExecutorIsNotRegisteredWithoutItsConcurrencyLimit() {
        new ApplicationContextRunner()
            .withUserConfiguration(AsyncConfiguration.class)
            .withBean(ContextPropagatingTaskDecorator.class, ContextPropagatingTaskDecorator::new)
            .withBean(TaskExecutionProperties.class, TaskExecutionProperties::new)
            .withPropertyValues(
                "bytechef.worker.task.subscriptions.default=3",
                "spring.threads.virtual.enabled=true")
            .run(context -> {
                assertNull(context.getStartupFailure(), "context failed to start");
                assertFalse(context.containsBean("appExecutor"));
            });
    }

    @Test
    void testAppExecutorRejectsANonPositiveLimit() {
        environment.setProperty("bytechef.app.execution.concurrency-limit", "0");

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> asyncConfiguration.appExecutor());

        assertTrue(exception.getMessage()
            .contains("bytechef.app.execution.concurrency-limit"));
    }
```

`ApplicationPropertiesTest`: add

```java
    @Test
    void testAppExecutionBindsWithoutUnboundElements() {
        ConfigurationPropertySource source = new MapConfigurationPropertySource(
            Map.of(
                "bytechef.app.execution.concurrency-limit", "10",
                "bytechef.app.execution.timeout", "60s"));

        ApplicationProperties applicationProperties = new Binder(source)
            .bind(
                "bytechef", Bindable.of(ApplicationProperties.class),
                new NoUnboundElementsBindHandler(BindHandler.DEFAULT))
            .get();

        App.Execution execution = applicationProperties.getApp()
            .getExecution();

        assertThat(execution.getConcurrencyLimit()).isEqualTo(10);
        assertThat(execution.getTimeout()).isEqualTo(Duration.ofSeconds(60));
    }
```

`AppExecutionFacadeTest`: add imports `com.bytechef.async.config.AsyncConfiguration`, `java.lang.reflect.Constructor`,
`java.util.Arrays`, `org.springframework.beans.factory.annotation.Qualifier`, `org.springframework.core.task.AsyncTaskExecutor`,
and the test:

```java
    @Test
    void testComponentCallsAreSubmittedToTheAppExecutor() {
        Constructor<?>[] constructors = AppExecutionFacadeImpl.class.getConstructors();

        assertThat(constructors).hasSize(1);

        List<String> executorQualifiers = Arrays.stream(constructors[0].getParameters())
            .filter(parameter -> AsyncTaskExecutor.class.equals(parameter.getType()))
            .map(parameter -> parameter.getAnnotation(Qualifier.class))
            .filter(Objects::nonNull)
            .map(Qualifier::value)
            .toList();

        assertThat(executorQualifiers).containsExactly(AsyncConfiguration.APP_EXECUTOR);
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
SCRATCH=<your session scratchpad directory>
./gradlew --continue :server:libs:config:async-config:test :server:libs:config:app-config:test --tests 'com.bytechef.config.ApplicationPropertiesTest' :server:ee:libs:automation:automation-app:automation-app-service:test --tests 'com.bytechef.ee.automation.app.facade.AppExecutionFacadeTest' > "$SCRATCH/t5-red.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED|error:' "$SCRATCH/t5-red.log"
```

Expected: non-zero exit; compile errors for `appExecutor()`, `getConcurrencyLimit()` and `APP_EXECUTOR`.

- [ ] **Step 3: Implement the property and the executor**

`ApplicationProperties.App.Execution` — replace the class body with:

```java
        public static class Execution {

            /**
             * Concurrent action, tool and proxy calls requested by apps. They run on their own executor so a burst from
             * apps cannot take permits from the editor's Test button or the API Platform sync path
             */
            private int concurrencyLimit;

            /**
             * Maximum time an action or tool execution requested by an app may run
             */
            private Duration timeout;

            public int getConcurrencyLimit() {
                return concurrencyLimit;
            }

            public Duration getTimeout() {
                return timeout;
            }

            public void setConcurrencyLimit(int concurrencyLimit) {
                this.concurrencyLimit = concurrencyLimit;
            }

            public void setTimeout(Duration timeout) {
                this.timeout = timeout;
            }
        }
```

`application-bytechef.yml` — replace

```yaml
    execution:
      # Maximum time an action or tool execution requested by an app may run.
      timeout: 60s
```

with

```yaml
    execution:
      # Concurrent action, tool and proxy calls requested by apps. They run on their own lane (appExecutor), so a burst
      # from apps cannot leave the editor's Test button or the API Platform sync path waiting for a permit.
      concurrency-limit: 10
      # Maximum time an action or tool execution requested by an app may run.
      timeout: 60s
```

`AsyncConfiguration` — add `import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;`, add
`public static final String APP_EXECUTOR = "appExecutor";` as the first constant and
`private static final String APP_CONCURRENCY_PROPERTY = "bytechef.app.execution.concurrency-limit";` before
`DEFAULT_SYNC_WORKER_CONCURRENCY`, and this bean before `messageEventExecutor()`:

```java
    /**
     * The lane of the actions, tools and proxy calls apps request. Registered only when its limit is configured, so the
     * apps that never run them do not need the property.
     */
    @Bean(name = APP_EXECUTOR)
    @ConditionalOnProperty(APP_CONCURRENCY_PROPERTY)
    AsyncTaskExecutor appExecutor() {
        int concurrencyLimit = environment.getRequiredProperty(APP_CONCURRENCY_PROPERTY, Integer.class);

        if (concurrencyLimit < 1) {
            throw new IllegalStateException(
                APP_CONCURRENCY_PROPERTY + " must be a positive number, but was " + concurrencyLimit);
        }

        return createExecutor("app-", concurrencyLimit);
    }
```

- [ ] **Step 4: Move the facade onto the app lane**

In `AppExecutionFacadeImpl`:
- Field: `private final AsyncTaskExecutor syncWorkerExecutor;` becomes `private final AsyncTaskExecutor appExecutor;`
  (rename in place).
- Constructor parameter: `@Qualifier(AsyncConfiguration.SYNC_WORKER_EXECUTOR) AsyncTaskExecutor syncWorkerExecutor,`
  becomes `@Qualifier(AsyncConfiguration.APP_EXECUTOR) AsyncTaskExecutor appExecutor,` (same position, so
  `AppExecutionFacadeTest`'s positional `new SimpleAsyncTaskExecutor("test-")` still lands on it);
  `this.syncWorkerExecutor = syncWorkerExecutor;` becomes `this.appExecutor = appExecutor;`.
- Rename every `runOnSyncWorkerLane(` to `runOnAppLane(` (three call sites and the declaration), and in it
  `syncWorkerExecutor.submit(callable)` becomes `appExecutor.submit(callable)`.
- Class Javadoc: replace `only the component call itself runs on the sync worker lane, so a burst from one app takes
  permits from the same bulkhead as the API Platform sync path and the editor's Test button, never from scheduled
  runs.` with `only the component call itself runs on the app lane ({@code appExecutor}), a bulkhead of its own, so a
  burst from apps takes permits neither from scheduled runs nor from the API Platform sync path and the editor's Test
  button.`
- `executeProxy` Javadoc: `Runs on the sync worker lane.` becomes `Runs on the app lane.`
- `runOnAppLane` Javadoc: `Submits the platform call to the sync worker lane.` becomes `Submits the platform call to
  the app lane.`

In `AppProxyExecutor` line 80: `the caller is responsible for running it on the sync worker lane as the` becomes
`the caller is responsible for running it on the app lane as the`.

In `AppIntTestConfiguration`: the Javadoc sentence `The sync worker lane is a real executor carrying the same tenant
and context-propagation decoration as production (so the request's security context reaches the lane), so a test can
prove the tenant reaches it.` becomes `The app lane is a real executor carrying the same tenant and context-propagation
decoration as production (so the request's security context reaches the lane), so a test can prove the tenant reaches
it.`, and the bean becomes:

```java
    @Bean(AsyncConfiguration.APP_EXECUTOR)
    SimpleAsyncTaskExecutor appExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("app-");

        TenantTaskDecorator tenantTaskDecorator = new TenantTaskDecorator();
        ContextPropagatingTaskDecorator contextPropagatingTaskDecorator = new ContextPropagatingTaskDecorator();

        executor.setTaskDecorator(
            runnable -> tenantTaskDecorator.decorate(contextPropagatingTaskDecorator.decorate(runnable)));

        return executor;
    }
```

(Move it to keep the config's beans in their existing order; it replaces `syncWorkerExecutor()`.)

- [ ] **Step 5: Run the tests to verify they pass**

Same command as Step 2 with log `t5-green.log`. Expected: `exit=0`, no `FAILED`.

- [ ] **Step 6: Update the docs**

`CLAUDE.md` — replace the whole bullet that starts `- **Four executors, no queues.**` (lines 554-573) with:

```markdown
- **Five executors, no queues.** `AsyncConfiguration` defines `taskExecutor` (default `@Async`, limit
  `bytechef.async.concurrency-limit`), `workerExecutor` (task handlers + trigger executions, limit
  `bytechef.worker.task.subscriptions.default`), `syncWorkerExecutor` (the API-Platform sync path and the editor's
  Test button, limit `bytechef.worker.task.sync-concurrency-limit`), `appExecutor` (the actions, tools and proxy calls
  Apps request, limit `bytechef.app.execution.concurrency-limit`; registered only when that property is set, which only
  `server-app`'s `application-bytechef.yml` does) and `messageEventExecutor` (Spring-event→broker bridge, unbounded).
  The sync lane is a bulkhead, not extra capacity: without it a burst of scheduled runs saturates the worker lane and
  an interactive caller blocks on a permit while holding a request thread. The app lane is the same bulkhead one step
  further: apps are external, machine-driven callers, and on the sync lane a burst from them would starve the editor's
  Test button and the API-Platform sync path. App workflow runs do not use it — they go through the job path on
  `workerExecutor`. All are queue-less `SimpleAsyncTaskExecutor`s: a submitter waits for a permit. Anything that runs
  component code or blocks on I/O goes on `workerExecutor`, never the default. Virtual threads change only the thread
  factory — they do not lift a limit, which is why these are not `ThreadPoolTaskExecutor`s. The bridge stays unbounded
  because it only hands messages to the broker: starving it stalls every workflow, while the real work is already
  bounded at `workerExecutor` (and DB connections at Hikari's `maximum-pool-size`). Don't bound
  `AsyncMessageBroker`'s own dispatch executor either — it carries work-initiating and completion events together,
  and each `JobSyncExecutor` builds a private instance whose latch waits on events from it, so a bound there can
  wedge the API-Platform sync path and the editor's Test button. Its per-route ordered executors (single thread,
  bounded queue, blocking put) are a separate, deliberate case and stay bounded. `AsyncConfigurationTest`
  enumerates them, so it catches the decorator being dropped from `createExecutor` — a sixth executor built
  outside that factory isn't covered; every executor must go through `createExecutor`. The one deliberate exception is
  `WorkflowSubflowSyncExecutorConfiguration`, which builds its own unbounded executor: subflows nest through a
  `SubflowTaskDispatcher`, so a parent holds a permit while its child needs one, and a shared bounded lane would
  deadlock past the nesting depth.
```

`.agents/apps.md`:
- "Execution lane" paragraph: replace `component call on **\`syncWorkerExecutor\`** (the bulkhead shared with the API
  Platform sync path and the editor's Test button, so a burst from one app takes no permit from scheduled runs on
  \`workerExecutor\`), bounded by` with `component call on **\`appExecutor\`** (a bulkhead of its own, limit
  \`bytechef.app.execution.concurrency-limit\`, default 10, so a burst from apps takes no permit from scheduled runs on
  \`workerExecutor\` nor from the API Platform sync path and the editor's Test button on \`syncWorkerExecutor\`), bounded
  by` (no backslashes; rewrap).
- `waits (no \`syncWorkerExecutor\`, no cap on concurrent waits).` becomes `waits (not on \`appExecutor\`, no cap on
  concurrent waits).`
- "Configuration": `\`Execution{timeout}\`` becomes `\`Execution{concurrencyLimit, timeout}\``, and `execution timeout
  60 s;` becomes `execution concurrency limit 10 and timeout 60 s;`.

`docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md`:
- Step 4 of the execution path: `4. Execute **on \`syncWorkerExecutor\`**, the bulkhead the API Platform sync path and
  the editor's Test button already\n   share, with` becomes `4. Execute **on \`appExecutor\`**, a bulkhead of its own
  (\`bytechef.app.execution.concurrency-limit\`, default 10) so apps cannot starve the API Platform sync path or the
  editor's Test button, with` (rewrap the list item).
- Testing: `execution runs on \`syncWorkerExecutor\` as the system principal;` becomes `execution runs on
  \`appExecutor\` as the system principal;`.

- [ ] **Step 7: Format, full check, commit**

```bash
SCRATCH=<your session scratchpad directory>
./gradlew --continue :server:libs:config:async-config:spotlessApply :server:libs:config:app-config:spotlessApply :server:ee:libs:automation:automation-app:automation-app-service:spotlessApply > "$SCRATCH/t5-spotless.log" 2>&1
echo "exit=$?"
./gradlew --continue :server:libs:config:async-config:check :server:libs:config:app-config:check :server:ee:libs:automation:automation-app:automation-app-service:check > "$SCRATCH/t5-check.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' "$SCRATCH/t5-check.log"
git branch --show-current
git add server/libs/config/async-config/src/main/java/com/bytechef/async/config/AsyncConfiguration.java server/libs/config/async-config/src/test/java/com/bytechef/async/config/AsyncConfigurationTest.java server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java server/libs/config/app-config/src/test/java/com/bytechef/config/ApplicationPropertiesTest.java server/apps/server-app/src/main/resources/config/application-bytechef.yml server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/facade/AppExecutionFacadeImpl.java server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/proxy/AppProxyExecutor.java server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/config/AppIntTestConfiguration.java server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/facade/AppExecutionFacadeTest.java
git commit -m "Run app actions, tools and proxy calls on a dedicated app executor"
git add CLAUDE.md .agents/apps.md docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md
git commit -m "docs - Describe the dedicated app executor"
```

Before committing, also boot-check the property wiring: `grep -n 'concurrency-limit' server/apps/server-app/src/main/resources/config/application-bytechef.yml`
must show the new key under `app.execution`.

---

### Task 6: Add an in-flight response size limit to the component HTTP client (design item 2, platform half)

**Files:**
- Create: `sdks/backend/java/component-api/src/main/java/com/bytechef/component/exception/HttpResponseTooLargeException.java`
- Modify: `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/Context.java` (`Http.Configuration` ~906-1192)
- Create: `server/libs/platform/platform-component/platform-component-context/platform-component-context-service/src/main/java/com/bytechef/platform/component/context/MaxResponseBytesInputStream.java`
- Modify: `server/libs/platform/platform-component/platform-component-context/platform-component-context-service/src/main/java/com/bytechef/platform/component/context/HttpClientExecutor.java:309-312,600-620`
- Test: `sdks/backend/java/component-api/src/test/java/com/bytechef/component/definition/ContextHttpConfigurationTest.java` (create)
- Test: `server/libs/platform/platform-component/platform-component-context/platform-component-context-service/src/test/java/com/bytechef/platform/component/context/HttpClientExecutorTest.java`

`HttpClientExecutor` reads a BINARY body with `BodyHandlers.ofInputStream()` and hands the stream to
`tempFileStorage.storeFileContent(filename, InputStream)`; the storage providers read it with `Files.copy`,
`readNBytes` or `readAllBytes`, all of which go through `read(byte[], int, int)` (`FilterInputStream` on JDK 25
overrides only `read()`, `read(byte[])`, `read(byte[],int,int)`, `skip`, `available`, `close`, `mark`, `reset`,
`markSupported`). Closing a JDK HTTP response stream before EOF cancels the exchange. `HttpClientExecutor` already
holds `tempFileStorage` (`TempFileStorage.deleteFile(com.bytechef.file.storage.domain.FileEntry)` exists).

**Interfaces:**
- Produces: `Http.Configuration.ConfigurationBuilder#maxResponseBytes(long)`;
  `Http.Configuration#getMaxResponseBytes(): @Nullable Long`;
  `com.bytechef.component.exception.HttpResponseTooLargeException(long maxResponseBytes)` with
  `getMaxResponseBytes(): long`, thrown out of `Http.Executor.execute()` (after the spool is deleted) wrapped by
  `ContextImpl` in `RuntimeException("Unable to execute HTTP request", e)`. `maxResponseBytes(long)` throws
  `IllegalArgumentException` for a value below 1.

- [ ] **Step 1: Write the failing tests**

Create `ContextHttpConfigurationTest.java` (Apache header copied from `ContextHttpResponseTypeTest.java`):

```java
package com.bytechef.component.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ContextHttpConfigurationTest {

    @Test
    void testMaxResponseBytesIsUnsetByDefault() {
        Configuration configuration = Configuration.newConfiguration()
            .build();

        assertNull(configuration.getMaxResponseBytes());
    }

    @Test
    void testMaxResponseBytesIsCarriedToTheConfiguration() {
        Configuration configuration = Http.responseType(Http.ResponseType.BINARY)
            .maxResponseBytes(1024)
            .build();

        assertEquals(Long.valueOf(1024), configuration.getMaxResponseBytes());
    }

    @Test
    void testBuildersDifferingOnlyInMaxResponseBytesAreNotEqual() {
        assertNotEquals(
            Configuration.newConfiguration()
                .maxResponseBytes(1),
            Configuration.newConfiguration()
                .maxResponseBytes(2));
        assertEquals(
            Configuration.newConfiguration()
                .maxResponseBytes(1),
            Configuration.newConfiguration()
                .maxResponseBytes(1));
    }

    @Test
    void testMaxResponseBytesBelowOneIsRefused() {
        ConfigurationBuilder configurationBuilder = Configuration.newConfiguration();

        assertThrows(IllegalArgumentException.class, () -> configurationBuilder.maxResponseBytes(0));
    }
}
```

(Add `import static org.junit.jupiter.api.Assertions.assertThrows;` and
`import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;`.)

In `HttpClientExecutorTest`, add the imports `com.bytechef.component.exception.HttpResponseTooLargeException` and
`java.io.ByteArrayOutputStream`, and insert this nested class and helper directly before
`    private static class TestHttpResponse implements HttpResponse<Object> {`:

```java
    @Nested
    @DisplayName("maxResponseBytes tests")
    class MaxResponseBytesTests {

        private static final com.bytechef.file.storage.domain.FileEntry STORED_FILE_ENTRY =
            new com.bytechef.file.storage.domain.FileEntry("download.bin", "file://stored");

        @Test
        @DisplayName("Should store a binary body exactly at the limit")
        void testBinaryBodyAtTheLimitIsStored() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.BINARY)
                .filename("download.bin")
                .maxResponseBytes(4)
                .build();

            ByteArrayOutputStream storedBytes = stubStoreCopyingTheStream();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                new ByteArrayInputStream(new byte[] {
                    1, 2, 3, 4
                }),
                HttpHeaders.of(Map.of("content-type", List.of("application/octet-stream")), (name, value) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            FileEntryImpl resultFileEntry = response.getBody(FileEntryImpl.class);

            assertEquals("file://stored", resultFileEntry.getUrl());
            assertEquals(4, storedBytes.size());

            Mockito.verify(tempFileStorage, Mockito.never())
                .deleteFile(Mockito.any());
        }

        @Test
        @DisplayName("Should stop reading past the limit, close the upstream, delete the spool and throw")
        void testBinaryBodyOverTheLimitIsAbortedAndTheUpstreamClosed() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.BINARY)
                .filename("download.bin")
                .maxResponseBytes(4)
                .build();

            ByteArrayOutputStream storedBytes = stubStoreCopyingTheStream();

            CloseTrackingInputStream upstream = new CloseTrackingInputStream(new byte[] {
                1, 2, 3, 4, 5, 6, 7, 8
            });

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                upstream,
                HttpHeaders.of(Map.of("content-type", List.of("application/octet-stream")), (name, value) -> true),
                200);

            HttpResponseTooLargeException exception = assertThrows(
                HttpResponseTooLargeException.class,
                () -> httpClientExecutor.handleResponse(testHttpResponse, configuration, context));

            assertEquals(4, exception.getMaxResponseBytes());
            assertTrue(upstream.isClosed());
            assertTrue(storedBytes.size() <= 4);

            Mockito.verify(tempFileStorage)
                .deleteFile(STORED_FILE_ENTRY);
        }

        @Test
        @DisplayName("Should still throw the size error when deleting the oversized spool fails")
        void testDeleteFailureOfAnOversizedSpoolStillThrowsTooLarge() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.BINARY)
                .filename("download.bin")
                .maxResponseBytes(4)
                .build();

            stubStoreCopyingTheStream();

            IllegalStateException deleteFailure = new IllegalStateException("storage down");

            Mockito.doThrow(deleteFailure)
                .when(tempFileStorage)
                .deleteFile(STORED_FILE_ENTRY);

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                new ByteArrayInputStream(new byte[16]),
                HttpHeaders.of(Map.of("content-type", List.of("application/octet-stream")), (name, value) -> true),
                200);

            HttpResponseTooLargeException exception = assertThrows(
                HttpResponseTooLargeException.class,
                () -> httpClientExecutor.handleResponse(testHttpResponse, configuration, context));

            assertEquals(List.of(deleteFailure), List.of(exception.getSuppressed()));
        }

        @Test
        @DisplayName("Should not limit a binary body when no maximum is set")
        void testBinaryBodyWithoutALimitIsStoredWhole() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.BINARY)
                .filename("download.bin")
                .build();

            ByteArrayOutputStream storedBytes = stubStoreCopyingTheStream();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                new ByteArrayInputStream(new byte[65536]),
                HttpHeaders.of(Map.of("content-type", List.of("application/octet-stream")), (name, value) -> true),
                200);

            httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertEquals(65536, storedBytes.size());
        }

        private ByteArrayOutputStream stubStoreCopyingTheStream() {
            ByteArrayOutputStream storedBytes = new ByteArrayOutputStream();

            Mockito.when(tempFileStorage.storeFileContent(Mockito.anyString(), Mockito.any(InputStream.class)))
                .thenAnswer(invocation -> {
                    InputStream inputStream = invocation.getArgument(1);

                    inputStream.transferTo(storedBytes);

                    return STORED_FILE_ENTRY;
                });

            return storedBytes;
        }
    }

    private static final class CloseTrackingInputStream extends ByteArrayInputStream {

        private boolean closed;

        private CloseTrackingInputStream(byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() throws IOException {
            closed = true;

            super.close();
        }

        private boolean isClosed() {
            return closed;
        }
    }
```

(`ByteArrayInputStream`, `InputStream`, `IOException`, `HttpHeaders`, `assertThrows`, `assertTrue` are already
imported. The stubs copy through `transferTo`, i.e. through `read(byte[], int, int)`, as every storage provider does;
the storage `FileEntry` has value equality, which `verify(...).deleteFile(STORED_FILE_ENTRY)` relies on — check
`com.bytechef.file.storage.domain.FileEntry#equals` and use `Mockito.same(STORED_FILE_ENTRY)` if it has none.)

- [ ] **Step 2: Run the tests to verify they fail**

```bash
SCRATCH=<your session scratchpad directory>
./gradlew --continue :sdks:backend:java:component-api:test --tests 'com.bytechef.component.definition.ContextHttpConfigurationTest' :server:libs:platform:platform-component:platform-component-context:platform-component-context-service:test --tests 'com.bytechef.platform.component.context.HttpClientExecutorTest' > "$SCRATCH/t6-red.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED|error:' "$SCRATCH/t6-red.log"
```

Expected: non-zero exit; compile errors for `maxResponseBytes` / `getMaxResponseBytes` / `HttpResponseTooLargeException`.

- [ ] **Step 3: Add the exception**

Create `HttpResponseTooLargeException.java` with the Apache header copied from `ProviderException.java` and:

```java
package com.bytechef.component.exception;

/**
 * Thrown when an HTTP response body is larger than the limit set with
 * {@code Http.Configuration.ConfigurationBuilder#maxResponseBytes(long)}. Before throwing, the client stops reading,
 * releases the connection and deletes whatever part of the body it had stored, so no more than the limit is ever
 * received and nothing of it is kept.
 *
 * @author Ivica Cardic
 */
public class HttpResponseTooLargeException extends RuntimeException {

    private final long maxResponseBytes;

    /**
     * Creates the exception for a body that passed the given limit.
     *
     * @param maxResponseBytes the limit, in bytes, the body passed
     */
    public HttpResponseTooLargeException(long maxResponseBytes) {
        super("HTTP response body exceeds %d bytes".formatted(maxResponseBytes));

        this.maxResponseBytes = maxResponseBytes;
    }

    /**
     * Returns the limit, in bytes, the body passed.
     *
     * @return the configured maximum response size
     */
    public long getMaxResponseBytes() {
        return maxResponseBytes;
    }
}
```

- [ ] **Step 4: Add the SDK option**

In `Context.java`, `Http.Configuration`:
- Add field `private Long maxResponseBytes;` after `private boolean disableAuthorization;` in **both** `Configuration`
  and `ConfigurationBuilder`.
- After `isDisableAuthorization()` in `Configuration`, add:

```java
            /**
             * Returns the largest response body, in bytes, the client reads before it aborts the request with
             * {@link com.bytechef.component.exception.HttpResponseTooLargeException}. Honoured for
             * {@link ResponseType#BINARY} responses, whose body is read as a stream.
             *
             * @return the configured maximum, or {@code null} when the body is not limited
             */
            public @Nullable Long getMaxResponseBytes() {
                return maxResponseBytes;
            }
```

- In `ConfigurationBuilder.equals`, append `&& Objects.equals(maxResponseBytes, that.maxResponseBytes)` to the
  returned expression; in `hashCode`, add `maxResponseBytes` as the last argument of `Objects.hash(...)`.
- After the builder's `disableAuthorization(boolean)` method and before `build()`, add:

```java
                /**
                 * Sets the largest response body, in bytes, the client reads. A larger body is cut off as soon as the
                 * limit is passed, before the rest is received: the connection is released, whatever was stored is
                 * deleted and the request fails with
                 * {@link com.bytechef.component.exception.HttpResponseTooLargeException}. Honoured for
                 * {@link ResponseType#BINARY} responses.
                 *
                 * @param maxResponseBytes the maximum number of response body bytes, at least 1
                 * @return this builder
                 */
                public ConfigurationBuilder maxResponseBytes(long maxResponseBytes) {
                    if (maxResponseBytes < 1) {
                        throw new IllegalArgumentException(
                            "maxResponseBytes must be at least 1, but was " + maxResponseBytes);
                    }

                    this.maxResponseBytes = maxResponseBytes;

                    return this;
                }
```

- In `build()`, add `configuration.maxResponseBytes = this.maxResponseBytes;` after
  `configuration.disableAuthorization = this.disableAuthorization;`.

- [ ] **Step 5: Enforce it in the HTTP client**

The stream never throws mid-copy. Past the limit it closes the upstream (cancelling the exchange), sets `exceeded` and
reports end of stream, so every storage provider finishes normally and returns the spool's entry; the client then
deletes that spool and throws. No spool survives an oversized response on any provider. Bytes past the limit are
never handed to the storage, so a spool holds at most `maxResponseBytes` bytes; because the builder refuses a limit
below 1, an aborted spool is never empty (the filesystem provider refuses to store an empty file).

Create `MaxResponseBytesInputStream.java` (Apache header copied from `HttpClientExecutor.java`):

```java
package com.bytechef.platform.component.context;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.jspecify.annotations.Nullable;

/**
 * Hands at most {@code maxResponseBytes} bytes of an HTTP response body to its reader. When the upstream has more, the
 * stream closes the upstream (which cancels the exchange and releases the connection), records that the limit was
 * exceeded and reports end of stream, so the reader finishes normally and the caller decides what to do with what was
 * stored. Mark and reset are not supported, and skipping reads through the counter.
 *
 * @author Ivica Cardic
 */
final class MaxResponseBytesInputStream extends FilterInputStream {

    private static final int SKIP_BUFFER_SIZE = 8192;

    private @Nullable IOException closeException;
    private boolean exceeded;
    private final long maxResponseBytes;
    private long readBytes;

    MaxResponseBytesInputStream(InputStream inputStream, long maxResponseBytes) {
        super(inputStream);

        this.maxResponseBytes = maxResponseBytes;
    }

    @Override
    public int available() throws IOException {
        if (exceeded) {
            return 0;
        }

        return super.available();
    }

    /**
     * The failure closing the upstream after the limit was exceeded, if closing failed.
     */
    @Nullable
    IOException getCloseException() {
        return closeException;
    }

    boolean isExceeded() {
        return exceeded;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int read() throws IOException {
        if (exceeded) {
            return -1;
        }

        int value = super.read();

        if (value == -1) {
            return -1;
        }

        if (readBytes >= maxResponseBytes) {
            markExceeded();

            return -1;
        }

        readBytes++;

        return value;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (exceeded) {
            return -1;
        }

        int read = super.read(buffer, offset, length);

        if (read == -1) {
            return -1;
        }

        long allowedBytes = maxResponseBytes - readBytes;

        if (read > allowedBytes) {
            markExceeded();

            if (allowedBytes <= 0) {
                return -1;
            }

            readBytes = maxResponseBytes;

            return (int) allowedBytes;
        }

        readBytes += read;

        return read;
    }

    @Override
    public long skip(long length) throws IOException {
        byte[] buffer = new byte[SKIP_BUFFER_SIZE];
        long remaining = length;

        while (remaining > 0) {
            int read = read(buffer, 0, (int) Math.min(buffer.length, remaining));

            if (read == -1) {
                break;
            }

            remaining -= read;
        }

        return length - remaining;
    }

    private void markExceeded() {
        exceeded = true;

        try {
            close();
        } catch (IOException ioException) {
            closeException = ioException;
        }
    }
}
```

In `HttpClientExecutor`:
- `handleResponse`'s BINARY branch passes the context:
  `storeBinaryResponseBody(configuration, httpHeaders.map(), (InputStream) httpResponseBody, context), statusCode);`
- Add `import com.bytechef.component.exception.HttpResponseTooLargeException;`.
- Change the signature to
  `private FileEntry storeBinaryResponseBody(Configuration configuration, Map<String, List<String>> headers, InputStream httpResponseBody, Context context) {`
  (formatted like the existing one) and replace its last statement

```java
        return new FileEntryImpl(tempFileStorage.storeFileContent(filename, httpResponseBody));
```

with

```java
        Long maxResponseBytes = configuration.getMaxResponseBytes();

        if (maxResponseBytes == null) {
            return new FileEntryImpl(tempFileStorage.storeFileContent(filename, httpResponseBody));
        }

        MaxResponseBytesInputStream maxResponseBytesInputStream = new MaxResponseBytesInputStream(
            httpResponseBody, maxResponseBytes);

        com.bytechef.file.storage.domain.FileEntry storedFileEntry = tempFileStorage.storeFileContent(
            filename, maxResponseBytesInputStream);

        if (!maxResponseBytesInputStream.isExceeded()) {
            return new FileEntryImpl(storedFileEntry);
        }

        HttpResponseTooLargeException httpResponseTooLargeException = new HttpResponseTooLargeException(
            maxResponseBytes);

        IOException closeException = maxResponseBytesInputStream.getCloseException();

        if (closeException != null) {
            httpResponseTooLargeException.addSuppressed(closeException);
        }

        try {
            tempFileStorage.deleteFile(storedFileEntry);
        } catch (RuntimeException deleteException) {
            httpResponseTooLargeException.addSuppressed(deleteException);

            context.log(log -> log.warn("Unable to delete the spool of an oversized HTTP response", deleteException));
        }

        throw httpResponseTooLargeException;
```

(`IOException` is already imported. The storage `FileEntry` is spelled fully qualified because the class imports the
component `FileEntry`.)

- [ ] **Step 6: Run the tests to verify they pass**

Same command as Step 2 with log `t6-green.log`. Expected: `exit=0`, no `FAILED`.

- [ ] **Step 7: Format, full check, commit**

```bash
SCRATCH=<your session scratchpad directory>
./gradlew --continue :sdks:backend:java:component-api:spotlessApply :server:libs:platform:platform-component:platform-component-context:platform-component-context-service:spotlessApply > "$SCRATCH/t6-spotless.log" 2>&1
echo "exit=$?"
./gradlew --continue :sdks:backend:java:component-api:check :server:libs:platform:platform-component:platform-component-context:platform-component-context-service:check > "$SCRATCH/t6-check.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' "$SCRATCH/t6-check.log"
git branch --show-current
git add sdks/backend/java/component-api/src/main/java/com/bytechef/component/exception/HttpResponseTooLargeException.java sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/Context.java sdks/backend/java/component-api/src/test/java/com/bytechef/component/definition/ContextHttpConfigurationTest.java server/libs/platform/platform-component/platform-component-context/platform-component-context-service/src/main/java/com/bytechef/platform/component/context/MaxResponseBytesInputStream.java server/libs/platform/platform-component/platform-component-context/platform-component-context-service/src/main/java/com/bytechef/platform/component/context/HttpClientExecutor.java server/libs/platform/platform-component/platform-component-context/platform-component-context-service/src/test/java/com/bytechef/platform/component/context/HttpClientExecutorTest.java
git commit -m "Add an in-flight response size limit to the component HTTP client"
```

---

### Task 7: Cap proxied app responses in flight and type proxy storage failures (design item 2, app half)

**Files:**
- Modify: `server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/exception/AppErrorType.java`
- Modify: `server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/proxy/AppProxyExecutor.java` (class Javadoc 72-77, `send` 232-250, new helper)
- Modify: `server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/facade/AppExecutionFacadeImpl.java` (`executeProxy`)
- Modify: `server/ee/libs/automation/automation-app/automation-app-public-rest/src/main/java/com/bytechef/ee/automation/app/public_/web/rest/AbstractAppApiController.java` (`toAppStatus`)
- Test: `server/ee/libs/automation/automation-app/automation-app-api/src/test/java/com/bytechef/ee/automation/app/exception/AppErrorTypeTest.java`
- Test: `server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/proxy/AppProxyExecutorTest.java`
- Test: `server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/facade/AppExecutionFacadeTest.java`
- Test: `server/ee/libs/automation/automation-app/automation-app-public-rest/src/test/java/com/bytechef/ee/automation/app/public_/web/rest/AppApiControllerTest.java`
- Docs: `.agents/apps.md`, `docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md`

**Interfaces:**
- Consumes (Task 6): `ConfigurationBuilder#maxResponseBytes(long)`, `HttpResponseTooLargeException`.
- Produces: `AppErrorType.APP_PROXY_STORAGE_FAILED` (key 123, HTTP 500). The read-back cap in `readBounded` stays as a
  backstop.

- [ ] **Step 1: Write the failing tests**

`AppErrorTypeTest`: `assertThat(constantCount).isEqualTo(23);` becomes `isEqualTo(24)`.

`AppApiControllerTest.appErrors()`: change the last entry
`Arguments.of(AppErrorType.APP_WORKFLOW_NOT_BINDABLE, 400, "APP_WORKFLOW_NOT_BINDABLE"));` to

```java
            Arguments.of(AppErrorType.APP_WORKFLOW_NOT_BINDABLE, 400, "APP_WORKFLOW_NOT_BINDABLE"),
            Arguments.of(AppErrorType.APP_PROXY_STORAGE_FAILED, 500, "APP_PROXY_STORAGE_FAILED"));
```

`AppProxyExecutorTest`: add `import com.bytechef.component.exception.HttpResponseTooLargeException;` and, after
`testRefusesOversizedResponseWhoseStoredLengthIsUnderReported`:

```java
    @Test
    void testSendsTheResponseCapToTheHttpClient() throws Exception {
        FileEntry responseSpool = storedBody("{}");

        stubExecute(response(200, Map.of("content-type", List.of("application/json")), responseSpool));

        appProxyExecutor.execute(binding, connection, request("GET", "/items"));

        ArgumentCaptor<Http.Configuration.ConfigurationBuilder> configurationCaptor = ArgumentCaptor.forClass(
            Http.Configuration.ConfigurationBuilder.class);

        verify(executor).configuration(configurationCaptor.capture());

        Http.Configuration configuration = configurationCaptor.getValue()
            .build();

        assertThat(configuration.getMaxResponseBytes()).isEqualTo(1024L);
    }

    @Test
    void testAbortedOversizedResponseIsRefusedAsTooLarge() {
        when(executor.execute()).thenThrow(
            new RuntimeException("Unable to execute HTTP request", new HttpResponseTooLargeException(1024)));

        assertAppError(
            () -> appProxyExecutor.execute(binding, connection, request("GET", "/items")),
            AppErrorType.APP_PROXY_RESPONSE_TOO_LARGE);
    }

    @Test
    void testAbortedOversizedResponseStillDeletesTheRequestSpool() throws Exception {
        FileEntry requestSpool = spoolFileEntry();

        when(file.storeContent(anyString(), any(InputStream.class))).thenReturn(requestSpool);
        when(executor.execute()).thenThrow(
            new RuntimeException("Unable to execute HTTP request", new HttpResponseTooLargeException(1024)));

        assertAppError(
            () -> appProxyExecutor.execute(
                binding, connection,
                new ProxyRequest("POST", "/items", Map.of(), Map.of(), "AAEC", BodyEncoding.BASE64, null)),
            AppErrorType.APP_PROXY_RESPONSE_TOO_LARGE);

        verifyDeleted(requestSpool);
    }
```

`AppExecutionFacadeTest`: replace `testProxyLocalStorageFailureIsNotMistakenForAnUpstreamOutage` with:

```java
    @Test
    void testProxyLocalStorageFailureIsReportedAsAStorageFailure() {
        ProxyStorageException failure = new ProxyStorageException(
            "Unable to read the proxied response body", new IOException("disk gone"));

        when(appProxyExecutor.execute(any(), any(), any())).thenThrow(failure);

        assertThatThrownBy(() -> facade.proxy("crm", proxyRequest("GET", "/items")))
            .isInstanceOfSatisfying(ConfigurationException.class, exception -> {
                assertThat(exception.getErrorKey()).isEqualTo(AppErrorType.APP_PROXY_STORAGE_FAILED.getErrorKey());
                assertThat(exception.getCause()).isSameAs(failure);
            });
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
SCRATCH=<your session scratchpad directory>
./gradlew --continue :server:ee:libs:automation:automation-app:automation-app-api:test :server:ee:libs:automation:automation-app:automation-app-service:test --tests 'com.bytechef.ee.automation.app.proxy.AppProxyExecutorTest' --tests 'com.bytechef.ee.automation.app.facade.AppExecutionFacadeTest' :server:ee:libs:automation:automation-app:automation-app-public-rest:test --tests 'com.bytechef.ee.automation.app.public_.web.rest.AppApiControllerTest' > "$SCRATCH/t7-red.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED|error:' "$SCRATCH/t7-red.log"
```

Expected: non-zero exit; compile errors for `APP_PROXY_STORAGE_FAILED`.

- [ ] **Step 3: Implement**

`AppErrorType`: add `public static final AppErrorType APP_PROXY_STORAGE_FAILED = new AppErrorType(123);` after
`APP_WORKFLOW_NOT_BINDABLE`, and replace the last map entry
`Map.entry(APP_WORKFLOW_NOT_BINDABLE.getErrorKey(), "APP_WORKFLOW_NOT_BINDABLE"));` with

```java
        Map.entry(APP_WORKFLOW_NOT_BINDABLE.getErrorKey(), "APP_WORKFLOW_NOT_BINDABLE"),
        Map.entry(APP_PROXY_STORAGE_FAILED.getErrorKey(), "APP_PROXY_STORAGE_FAILED"));
```

`AbstractAppApiController.toAppStatus`: before the final `return HttpStatus.BAD_REQUEST;` add

```java
        if (errorKey == AppErrorType.APP_PROXY_STORAGE_FAILED.getErrorKey()) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

```

`AppProxyExecutor`:
- Add `import com.bytechef.component.exception.HttpResponseTooLargeException;` and the constant
  `private static final int MAX_CAUSE_DEPTH = 16;` before `MAX_CONTENT_TYPE_LENGTH`.
- Class Javadoc: replace `The response is requested as binary, so the client spools the body to temporary file storage
  and the read-back is capped at {@code bytechef.app.proxy.max-response-bytes}. The client stores the whole upstream
  body before the cap is checked; a true in-flight cap would need a change to the component HTTP client.` with `The
  response is requested as binary with {@code bytechef.app.proxy.max-response-bytes} as the client's in-flight limit:
  the client counts the body while spooling it to temporary file storage and aborts the exchange once the limit is
  passed, which is answered as {@code APP_PROXY_RESPONSE_TOO_LARGE}. The read-back keeps the same bound as a backstop.`
- In `send`, add `.maxResponseBytes(exchange.maxResponseBytes())` to the configuration chain right after
  `.disableAuthorization(true)`, and replace `Http.Response response = executor.execute();` with:

```java
        Http.Response response;

        try {
            response = executor.execute();
        } catch (RuntimeException runtimeException) {
            if (isResponseTooLarge(runtimeException)) {
                throw responseTooLarge(exchange.maxResponseBytes());
            }

            throw runtimeException;
        }
```

- Add after `responseTooLarge(...)`:

```java
    private static boolean isResponseTooLarge(Throwable throwable) {
        Throwable cause = throwable;

        for (int depth = 0; cause != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (cause instanceof HttpResponseTooLargeException) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }
```

`AppExecutionFacadeImpl.executeProxy` — replace the method and its Javadoc with:

```java
    /**
     * Runs on the app lane. A transport failure reaching the upstream (an {@link IOException} somewhere in the cause
     * chain of the exception the component HTTP client throws) is reported with a fixed message: the original message
     * can carry upstream body text, which must not reach the app or the execution record. A local storage failure is
     * reported as {@code APP_PROXY_STORAGE_FAILED}, never as an unavailable upstream. Errors the executor already
     * classified (a failed token refresh, a refused path, a size limit) pass through unchanged.
     */
    private ProxyResponse executeProxy(ResolvedBinding resolved, ProxyRequest request) {
        try {
            return appProxyExecutor.execute(resolved.binding(), resolved.connection(), request);
        } catch (ConfigurationException configurationException) {
            throw configurationException;
        } catch (ProxyStorageException proxyStorageException) {
            throw new ConfigurationException(
                "The proxy could not store or read one of its temporary files", proxyStorageException,
                AppErrorType.APP_PROXY_STORAGE_FAILED);
        } catch (RuntimeException runtimeException) {
            if (isUpstreamTransportFailure(runtimeException)) {
                throw new ConfigurationException(
                    "The upstream is unavailable", runtimeException, AppErrorType.APP_PROXY_UPSTREAM_UNAVAILABLE);
            }

            throw runtimeException;
        }
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Same command as Step 2 with log `t7-green.log`. Expected: `exit=0`, no `FAILED`.

- [ ] **Step 5: Update the docs**

`.agents/apps.md`:
- "Spool files" bullet: replace `the read-back is\n  capped at \`bytechef.app.proxy.max-response-bytes\`` with
  `the client\n  aborts the exchange as soon as the body passes \`bytechef.app.proxy.max-response-bytes\` (the SDK's
  \`Http.Configuration\` \`maxResponseBytes\`, counted while spooling; \`HttpResponseTooLargeException\` in the cause
  chain becomes 502 \`APP_PROXY_RESPONSE_TOO_LARGE\`), the read-back keeps the same cap as a backstop` (no
  backslashes; rewrap).
- Replace the whole `- **Known gap: no in-flight cap.**` bullet (four lines) with:

```markdown
- **In-flight cap.** `HttpClientExecutor` wraps a BINARY body in a counting stream when the configuration carries
  `maxResponseBytes`; past the limit the stream closes the upstream (cancelling the exchange) and reports end of
  stream, the client deletes the partial spool (a delete failure is logged and never masks the size error) and throws
  `HttpResponseTooLargeException`. No spool survives an oversized response on any storage provider.
```

- "Errors" bullet: `A local spool read/store failure is \`ProxyStorageException\` -> 500.` becomes `A local spool
  read/store failure is \`ProxyStorageException\`, rethrown by the facade as 500 \`APP_PROXY_STORAGE_FAILED\` (123).`
- "Error mapping" list: after `502 \`APP_PROXY_RESPONSE_TOO_LARGE\`,\n\`APP_PROXY_UPSTREAM_UNAVAILABLE\`;` insert
  ` 500 \`APP_PROXY_STORAGE_FAILED\`;`.
- "Open items": `- The proxy's in-flight cap (above); response-spool write failures` becomes `- Response-spool write
  failures`.

`docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md`:
- In "Limits", replace from `and the read-back\n  is bounded at 10 MB (502 \`APP_PROXY_RESPONSE_TOO_LARGE\`). **There is
  no in-flight cap**:` through `ends a slow read.` with `and the client\n  aborts the exchange as soon as the body passes
  10 MB (\`Http.Configuration\` \`maxResponseBytes\`, 502 \`APP_PROXY_RESPONSE_TOO_LARGE\`); the read-back keeps the same
  bound as a backstop.` (rewrap).
- Error table: after the `| Upstream unreachable ...` row add
  `| Proxy could not store or read its own temporary file | 500 | \`APP_PROXY_STORAGE_FAILED\` |`.

- [ ] **Step 6: Format, full check, commit**

```bash
SCRATCH=<your session scratchpad directory>
./gradlew --continue :server:ee:libs:automation:automation-app:automation-app-api:spotlessApply :server:ee:libs:automation:automation-app:automation-app-service:spotlessApply :server:ee:libs:automation:automation-app:automation-app-public-rest:spotlessApply > "$SCRATCH/t7-spotless.log" 2>&1
echo "exit=$?"
./gradlew --continue :server:ee:libs:automation:automation-app:automation-app-api:check :server:ee:libs:automation:automation-app:automation-app-service:check :server:ee:libs:automation:automation-app:automation-app-public-rest:check > "$SCRATCH/t7-check.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' "$SCRATCH/t7-check.log"
git branch --show-current
git add server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/exception/AppErrorType.java server/ee/libs/automation/automation-app/automation-app-api/src/test/java/com/bytechef/ee/automation/app/exception/AppErrorTypeTest.java server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/proxy/AppProxyExecutor.java server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/facade/AppExecutionFacadeImpl.java server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/proxy/AppProxyExecutorTest.java server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/facade/AppExecutionFacadeTest.java server/ee/libs/automation/automation-app/automation-app-public-rest/src/main/java/com/bytechef/ee/automation/app/public_/web/rest/AbstractAppApiController.java server/ee/libs/automation/automation-app/automation-app-public-rest/src/test/java/com/bytechef/ee/automation/app/public_/web/rest/AppApiControllerTest.java
git commit -m "Cap proxied app responses in flight and report proxy storage failures as APP_PROXY_STORAGE_FAILED"
git add .agents/apps.md docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md
git commit -m "docs - Describe the proxy's in-flight response cap and storage failure code"
```

---

### Task 8: Authorize the tool invocation log through a facade and add a workspace-scoped query (design item 9, server)

**Files:**
- Create: `server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-api/src/main/java/com/bytechef/ee/platform/tool/invocation/log/facade/ToolInvocationLogFacade.java`
- Create: `server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-service/src/main/java/com/bytechef/ee/platform/tool/invocation/log/facade/ToolInvocationLogFacadeImpl.java`
- Modify: `server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-api/src/main/java/com/bytechef/ee/platform/tool/invocation/log/ToolInvocationLogService.java`
- Modify: `server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-service/src/main/java/com/bytechef/ee/platform/tool/invocation/log/ToolInvocationLogServiceImpl.java`
- Modify: `server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-service/src/main/java/com/bytechef/ee/platform/tool/invocation/log/repository/ToolInvocationLogRepository.java`
- Modify: `server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-service/build.gradle.kts`
- Modify: `server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-graphql/src/main/java/com/bytechef/ee/platform/tool/invocation/log/web/graphql/ToolInvocationLogGraphQlController.java`
- Modify: `server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-graphql/src/main/resources/graphql/tool-invocation-log.graphqls`
- Modify: `server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-graphql/build.gradle.kts`
- Delete: `server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-graphql/src/test/java/com/bytechef/ee/platform/tool/invocation/log/web/graphql/ToolInvocationLogGraphQlControllerTest.java`
- Create (test): `.../platform-tool-invocation-log-graphql/src/test/java/com/bytechef/ee/platform/tool/invocation/log/web/graphql/ToolInvocationLogGraphQlControllerIntTest.java`
- Create (test): `.../platform-tool-invocation-log-graphql/src/test/java/com/bytechef/ee/platform/tool/invocation/log/web/graphql/config/ToolInvocationLogGraphQlTestConfiguration.java`
- Create (test): `.../platform-tool-invocation-log-graphql/src/test/resources/graphql/test.graphqls`
- Create (test): `.../platform-tool-invocation-log-service/src/test/java/com/bytechef/ee/platform/tool/invocation/log/facade/ToolInvocationLogFacadeTest.java`
- Test: `.../platform-tool-invocation-log-service/src/test/java/com/bytechef/ee/platform/tool/invocation/log/ToolInvocationLogServiceIntTest.java`
- Docs: `.agents/apps.md`, `docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md`

Verified facts: `isTenantAdmin()` and `hasWorkspaceScopeInEnvironmentId(long, String, Long)` are methods of
`AutomationMethodSecurityExpressionRoot`; `EXECUTION_VIEW` is an `ExecutionPermissionScope` (minimum VIEWER).
`tool_invocation_log.workspace_id` is `BIGINT` and `environment` is `INT` (`20260718000001_tool_invocation_log_init.xml`),
holding the `Environment` ordinal (`ToolExecutionEvent.environment(Integer)`). Embedded surfaces leave both null.
The repo's facade guard tests pin `@PreAuthorize` values by reflection (`AuditEventFacadeAuthorizationTest`,
`AppFacadeTest.preAuthorizeExpressionsArePinned`); GraphQL controller tests are `@GraphQlTest` IntTests with a mocked
facade (`AuditEventGraphQlControllerIntTest`). `GraphQLScalarTypes.longScalar()` lives in `test-int-support`.

**Interfaces:**
- Produces (Java):
  ```java
  Page<ToolInvocationLog> ToolInvocationLogFacade.getToolInvocationLogs(
      @Nullable ToolExecutionSurface surface, @Nullable ToolExecutionOutcome outcome, @Nullable Long mcpServerId,
      @Nullable Long connectedUserId, @Nullable Long integrationInstanceId, @Nullable Long appId,
      @Nullable Instant startDate, @Nullable Instant endDate, int pageNumber);            // isTenantAdmin()
  Page<ToolInvocationLog> ToolInvocationLogFacade.getWorkspaceToolInvocationLogs(
      long workspaceId, long environmentId, /* the same nine parameters */);              // EXECUTION_VIEW
  Page<ToolInvocationLog> ToolInvocationLogService.getWorkspaceToolInvocationLogs(
      long workspaceId, int environment, /* the same nine parameters */);
  ```
- Produces (GraphQL, consumed by Task 9):
  `workspaceToolInvocationLogs(workspaceId: ID!, environmentId: ID!, surface: String, outcome: String, mcpServerId: Long,
  connectedUserId: Long, integrationInstanceId: Long, appId: Long, fromDate: Long, toDate: Long, page: Int = 0):
  ToolInvocationLogPageType!`. `toolInvocationLogs(...)` keeps its signature and becomes tenant-admin only.

- [ ] **Step 1: Write the failing tests**

`ToolInvocationLogServiceIntTest` — add these tests and helper:

```java
    @Test
    void testWorkspaceQueryReturnsOnlyThatWorkspacesRowsInThatEnvironment() {
        createInWorkspace(7L, 2, 5L);
        createInWorkspace(7L, 0, 5L);
        createInWorkspace(8L, 2, 6L);
        createForApp(ToolExecutionSurface.AUTOMATION_APP_ACTION, 5L);

        Page<ToolInvocationLog> page = toolInvocationLogService.getWorkspaceToolInvocationLogs(
            7L, 2, null, null, null, null, null, null, null, null, 0);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(ToolInvocationLog::getWorkspaceId)
            .containsOnly(7L);
        assertThat(page.getContent()).extracting(ToolInvocationLog::getEnvironment)
            .containsOnly(2);
    }

    @Test
    void testWorkspaceQueryNamingAnotherWorkspacesAppMatchesNothing() {
        createInWorkspace(8L, 2, 6L);

        Page<ToolInvocationLog> page = toolInvocationLogService.getWorkspaceToolInvocationLogs(
            7L, 2, null, null, null, null, null, 6L, null, null, 0);

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    private void createInWorkspace(long workspaceId, int environment, long appId) {
        ToolInvocationLog toolInvocationLog = new ToolInvocationLog();

        toolInvocationLog.setTenantId("public");
        toolInvocationLog.setSurface(ToolExecutionSurface.AUTOMATION_APP_ACTION);
        toolInvocationLog.setKind(ToolExecutionKind.COMPONENT);
        toolInvocationLog.setToolName("slack_sendMessage");
        toolInvocationLog.setWorkspaceId(workspaceId);
        toolInvocationLog.setEnvironment(environment);
        toolInvocationLog.setAppId(appId);
        toolInvocationLog.setOutcome(ToolExecutionOutcome.SUCCESS);
        toolInvocationLog.setDurationMs(5);
        toolInvocationLog.setCreatedDate(Instant.now());

        toolInvocationLogService.create(toolInvocationLog);
    }
```

Create `ToolInvocationLogFacadeTest.java` (EE header):

```java
package com.bytechef.ee.platform.tool.invocation.log.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLog;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogService;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the guards of {@link ToolInvocationLogFacadeImpl}: the tenant-wide log is for tenant admins, a workspace's log
 * for members holding {@code EXECUTION_VIEW} in that environment. Runtime enforcement of these expressions is proven
 * generically by {@code PreAuthorizeProxyEnforcementIntTest}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ToolInvocationLogFacadeTest {

    @Mock
    private ToolInvocationLogService toolInvocationLogService;

    @Test
    void testPreAuthorizeExpressionsArePinned() throws NoSuchMethodException {
        assertThat(
            preAuthorize(
                "getToolInvocationLogs", ToolExecutionSurface.class, ToolExecutionOutcome.class, Long.class, Long.class,
                Long.class, Long.class, Instant.class, Instant.class, int.class))
                    .isEqualTo("isTenantAdmin()");
        assertThat(
            preAuthorize(
                "getWorkspaceToolInvocationLogs", long.class, long.class, ToolExecutionSurface.class,
                ToolExecutionOutcome.class, Long.class, Long.class, Long.class, Long.class, Instant.class,
                Instant.class, int.class))
                    .isEqualTo("hasWorkspaceScopeInEnvironmentId(#workspaceId, 'EXECUTION_VIEW', #environmentId)");
    }

    @Test
    void testWorkspaceQueryPassesTheWorkspaceAndEnvironmentToTheService() {
        Page<ToolInvocationLog> page = new PageImpl<>(List.of());

        when(
            toolInvocationLogService.getWorkspaceToolInvocationLogs(
                7L, 2, ToolExecutionSurface.AUTOMATION_APP_ACTION, null, null, null, null, 5L, null, null, 1))
                    .thenReturn(page);

        ToolInvocationLogFacadeImpl toolInvocationLogFacade = new ToolInvocationLogFacadeImpl(toolInvocationLogService);

        assertThat(
            toolInvocationLogFacade.getWorkspaceToolInvocationLogs(
                7L, 2L, ToolExecutionSurface.AUTOMATION_APP_ACTION, null, null, null, null, 5L, null, null, 1))
                    .isSameAs(page);

        verifyNoMoreInteractions(toolInvocationLogService);
    }

    @Test
    void testTenantQueryDelegatesToTheTenantWideServiceQuery() {
        Page<ToolInvocationLog> page = new PageImpl<>(List.of());

        when(toolInvocationLogService.getToolInvocationLogs(null, null, null, 3L, null, null, null, null, 0))
            .thenReturn(page);

        ToolInvocationLogFacadeImpl toolInvocationLogFacade = new ToolInvocationLogFacadeImpl(toolInvocationLogService);

        assertThat(toolInvocationLogFacade.getToolInvocationLogs(null, null, null, 3L, null, null, null, null, 0))
            .isSameAs(page);
    }

    private static String preAuthorize(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = ToolInvocationLogFacadeImpl.class.getMethod(methodName, parameterTypes);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("Method '%s' must carry @PreAuthorize; without it any authenticated user reads the log", methodName)
            .isNotNull();

        return preAuthorize.value();
    }
}
```

Create `src/test/resources/graphql/test.graphqls` in the graphql module:

```graphql
# Test-only GraphQL base operations for GraphQL slice tests in this module.

# Base operation type so that 'extend type Query' from main resources can merge.
type Query {
    _placeholder: Boolean
}

scalar Long
```

Create `ToolInvocationLogGraphQlTestConfiguration.java` (EE header):

```java
package com.bytechef.ee.platform.tool.invocation.log.web.graphql.config;

import com.bytechef.test.config.graphql.GraphQLScalarTypes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class ToolInvocationLogGraphQlTestConfiguration {

    @Bean
    RuntimeWiringConfigurer longScalarWiringConfigurer() {
        return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.longScalar());
    }
}
```

Create `ToolInvocationLogGraphQlControllerIntTest.java` (EE header):

```java
package com.bytechef.ee.platform.tool.invocation.log.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLog;
import com.bytechef.ee.platform.tool.invocation.log.facade.ToolInvocationLogFacade;
import com.bytechef.ee.platform.tool.invocation.log.web.graphql.config.ToolInvocationLogGraphQlTestConfiguration;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies the controller maps both queries onto {@link ToolInvocationLogFacade}, which owns their authorization.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    ToolInvocationLogGraphQlTestConfiguration.class,
    ToolInvocationLogGraphQlController.class
})
@GraphQlTest(
    controllers = ToolInvocationLogGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
class ToolInvocationLogGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ToolInvocationLogFacade toolInvocationLogFacade;

    @Test
    void testWorkspaceToolInvocationLogsPassesTheWorkspaceAndEnvironmentToTheFacade() {
        when(
            toolInvocationLogFacade.getWorkspaceToolInvocationLogs(
                eq(7L), eq(2L), eq(ToolExecutionSurface.AUTOMATION_APP_ACTION), isNull(), isNull(), isNull(), isNull(),
                eq(5L), isNull(), isNull(), eq(0)))
                    .thenReturn(new PageImpl<>(List.of(toolInvocationLog()), PageRequest.of(0, 50), 1));

        graphQlTester
            .document("""
                query {
                    workspaceToolInvocationLogs(
                        workspaceId: "7", environmentId: "2", surface: "AUTOMATION_APP_ACTION", appId: 5
                    ) {
                        content {
                            surface
                            kind
                            outcome
                            appId
                            workspaceId
                            createdDate
                        }
                        totalElements
                    }
                }
                """)
            .execute()
            .path("workspaceToolInvocationLogs.content[0].surface")
            .entity(String.class)
            .isEqualTo("AUTOMATION_APP_ACTION")
            .path("workspaceToolInvocationLogs.content[0].appId")
            .entity(Long.class)
            .isEqualTo(5L)
            .path("workspaceToolInvocationLogs.content[0].workspaceId")
            .entity(Long.class)
            .isEqualTo(7L)
            .path("workspaceToolInvocationLogs.content[0].createdDate")
            .entity(Long.class)
            .isEqualTo(1_000L)
            .path("workspaceToolInvocationLogs.totalElements")
            .entity(Integer.class)
            .isEqualTo(1);

        verify(toolInvocationLogFacade, never()).getToolInvocationLogs(
            any(), any(), any(), any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void testToolInvocationLogsUsesTheTenantWideFacadeQuery() {
        when(
            toolInvocationLogFacade.getToolInvocationLogs(
                isNull(), isNull(), isNull(), eq(3L), isNull(), isNull(), isNull(), isNull(), eq(0)))
                    .thenReturn(new PageImpl<>(List.of()));

        graphQlTester
            .document("""
                query {
                    toolInvocationLogs(connectedUserId: 3) {
                        totalElements
                    }
                }
                """)
            .execute()
            .path("toolInvocationLogs.totalElements")
            .entity(Integer.class)
            .isEqualTo(0);

        verify(toolInvocationLogFacade, never()).getWorkspaceToolInvocationLogs(
            anyLong(), anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void testControllerCarriesNoAuthorizationOfItsOwn() {
        List<Method> guardedMethods = Arrays.stream(ToolInvocationLogGraphQlController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
            .toList();

        assertThat(guardedMethods).isEmpty();
    }

    private static ToolInvocationLog toolInvocationLog() {
        ToolInvocationLog toolInvocationLog = new ToolInvocationLog();

        toolInvocationLog.setId(1L);
        toolInvocationLog.setSurface(ToolExecutionSurface.AUTOMATION_APP_ACTION);
        toolInvocationLog.setKind(ToolExecutionKind.COMPONENT);
        toolInvocationLog.setOutcome(ToolExecutionOutcome.SUCCESS);
        toolInvocationLog.setDurationMs(42);
        toolInvocationLog.setAppId(5L);
        toolInvocationLog.setWorkspaceId(7L);
        toolInvocationLog.setEnvironment(2);
        toolInvocationLog.setCreatedDate(Instant.ofEpochMilli(1_000L));

        return toolInvocationLog;
    }
}
```

Delete the plain controller unit test (its mapping assertions now live in the IntTest):

```bash
git rm server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-graphql/src/test/java/com/bytechef/ee/platform/tool/invocation/log/web/graphql/ToolInvocationLogGraphQlControllerTest.java
```

Add test dependencies to `platform-tool-invocation-log-graphql/build.gradle.kts` (after the existing
`testImplementation` lines):

```kotlin
    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-graphql-test")
    testImplementation(project(":server:libs:test:test-int-support"))
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
SCRATCH=<your session scratchpad directory>
./gradlew --continue :server:ee:libs:platform:platform-tool-invocation-log:platform-tool-invocation-log-service:test --tests 'com.bytechef.ee.platform.tool.invocation.log.facade.ToolInvocationLogFacadeTest' :server:ee:libs:platform:platform-tool-invocation-log:platform-tool-invocation-log-service:testIntegration --tests 'com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogServiceIntTest' :server:ee:libs:platform:platform-tool-invocation-log:platform-tool-invocation-log-graphql:testIntegration --tests 'com.bytechef.ee.platform.tool.invocation.log.web.graphql.ToolInvocationLogGraphQlControllerIntTest' > "$SCRATCH/t8-red.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED|error:' "$SCRATCH/t8-red.log"
```

Expected: non-zero exit; compile errors for `ToolInvocationLogFacade`, `ToolInvocationLogFacadeImpl` and
`getWorkspaceToolInvocationLogs`.

- [ ] **Step 3: Add the repository predicates and the service method**

Replace the two queries of `ToolInvocationLogRepository` (keep the cast comment above `findFiltered`) with:

```java
    @Query("""
        SELECT * FROM tool_invocation_log
        WHERE (:workspaceId IS NULL OR workspace_id = :workspaceId)
            AND (:environment IS NULL OR environment = :environment)
            AND (:surface IS NULL OR surface = :surface)
            AND (:outcome IS NULL OR outcome = :outcome)
            AND (:mcpServerId IS NULL OR mcp_server_id = :mcpServerId)
            AND (:connectedUserId IS NULL OR connected_user_id = :connectedUserId)
            AND (:integrationInstanceId IS NULL OR integration_instance_id = :integrationInstanceId)
            AND (:appId IS NULL OR app_id = :appId)
            AND (CAST(:startDate AS TIMESTAMP) IS NULL OR created_date >= CAST(:startDate AS TIMESTAMP))
            AND (CAST(:endDate AS TIMESTAMP) IS NULL OR created_date <= CAST(:endDate AS TIMESTAMP))
        ORDER BY created_date DESC
        LIMIT :limit OFFSET :offset
        """)
    List<ToolInvocationLog> findFiltered(
        @Param("workspaceId") @Nullable Long workspaceId, @Param("environment") @Nullable Integer environment,
        @Param("surface") @Nullable Integer surface, @Param("outcome") @Nullable Integer outcome,
        @Param("mcpServerId") @Nullable Long mcpServerId, @Param("connectedUserId") @Nullable Long connectedUserId,
        @Param("integrationInstanceId") @Nullable Long integrationInstanceId, @Param("appId") @Nullable Long appId,
        @Param("startDate") @Nullable Instant startDate, @Param("endDate") @Nullable Instant endDate,
        @Param("limit") int limit, @Param("offset") long offset);

    @Query("""
        SELECT COUNT(*) FROM tool_invocation_log
        WHERE (:workspaceId IS NULL OR workspace_id = :workspaceId)
            AND (:environment IS NULL OR environment = :environment)
            AND (:surface IS NULL OR surface = :surface)
            AND (:outcome IS NULL OR outcome = :outcome)
            AND (:mcpServerId IS NULL OR mcp_server_id = :mcpServerId)
            AND (:connectedUserId IS NULL OR connected_user_id = :connectedUserId)
            AND (:integrationInstanceId IS NULL OR integration_instance_id = :integrationInstanceId)
            AND (:appId IS NULL OR app_id = :appId)
            AND (CAST(:startDate AS TIMESTAMP) IS NULL OR created_date >= CAST(:startDate AS TIMESTAMP))
            AND (CAST(:endDate AS TIMESTAMP) IS NULL OR created_date <= CAST(:endDate AS TIMESTAMP))
        """)
    long countFiltered(
        @Param("workspaceId") @Nullable Long workspaceId, @Param("environment") @Nullable Integer environment,
        @Param("surface") @Nullable Integer surface, @Param("outcome") @Nullable Integer outcome,
        @Param("mcpServerId") @Nullable Long mcpServerId, @Param("connectedUserId") @Nullable Long connectedUserId,
        @Param("integrationInstanceId") @Nullable Long integrationInstanceId, @Param("appId") @Nullable Long appId,
        @Param("startDate") @Nullable Instant startDate, @Param("endDate") @Nullable Instant endDate);
```

`ToolInvocationLogService` — add after `getToolInvocationLogs`:

```java
    /**
     * The rows of one workspace in one environment ({@code environment} is the {@code Environment} ordinal), narrowed by
     * the given filters. Rows without a workspace (the embedded surfaces) are never returned.
     */
    Page<ToolInvocationLog> getWorkspaceToolInvocationLogs(
        long workspaceId, int environment, @Nullable ToolExecutionSurface surface,
        @Nullable ToolExecutionOutcome outcome, @Nullable Long mcpServerId, @Nullable Long connectedUserId,
        @Nullable Long integrationInstanceId, @Nullable Long appId, @Nullable Instant startDate,
        @Nullable Instant endDate, int pageNumber);
```

`ToolInvocationLogServiceImpl` — replace `getToolInvocationLogs` with:

```java
    @Override
    @Transactional(readOnly = true)
    public Page<ToolInvocationLog> getToolInvocationLogs(
        @Nullable ToolExecutionSurface surface, @Nullable ToolExecutionOutcome outcome, @Nullable Long mcpServerId,
        @Nullable Long connectedUserId, @Nullable Long integrationInstanceId, @Nullable Long appId,
        @Nullable Instant startDate, @Nullable Instant endDate, int pageNumber) {

        return findPage(
            null, null, surface, outcome, mcpServerId, connectedUserId, integrationInstanceId, appId, startDate,
            endDate, pageNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ToolInvocationLog> getWorkspaceToolInvocationLogs(
        long workspaceId, int environment, @Nullable ToolExecutionSurface surface,
        @Nullable ToolExecutionOutcome outcome, @Nullable Long mcpServerId, @Nullable Long connectedUserId,
        @Nullable Long integrationInstanceId, @Nullable Long appId, @Nullable Instant startDate,
        @Nullable Instant endDate, int pageNumber) {

        return findPage(
            workspaceId, environment, surface, outcome, mcpServerId, connectedUserId, integrationInstanceId, appId,
            startDate, endDate, pageNumber);
    }

    private Page<ToolInvocationLog> findPage(
        @Nullable Long workspaceId, @Nullable Integer environment, @Nullable ToolExecutionSurface surface,
        @Nullable ToolExecutionOutcome outcome, @Nullable Long mcpServerId, @Nullable Long connectedUserId,
        @Nullable Long integrationInstanceId, @Nullable Long appId, @Nullable Instant startDate,
        @Nullable Instant endDate, int pageNumber) {

        Integer surfaceOrdinal = surface == null ? null : surface.ordinal();
        Integer outcomeOrdinal = outcome == null ? null : outcome.ordinal();

        PageRequest pageRequest = PageRequest.of(pageNumber, DEFAULT_PAGE_SIZE);

        List<ToolInvocationLog> content = toolInvocationLogRepository.findFiltered(
            workspaceId, environment, surfaceOrdinal, outcomeOrdinal, mcpServerId, connectedUserId,
            integrationInstanceId, appId, startDate, endDate, pageRequest.getPageSize(), pageRequest.getOffset());

        long total = toolInvocationLogRepository.countFiltered(
            workspaceId, environment, surfaceOrdinal, outcomeOrdinal, mcpServerId, connectedUserId,
            integrationInstanceId, appId, startDate, endDate);

        return new PageImpl<>(content, pageRequest, total);
    }
```

- [ ] **Step 4: Add the facade**

`platform-tool-invocation-log-service/build.gradle.kts`: add `implementation("org.springframework.security:spring-security-core")`
after `implementation("org.springframework.data:spring-data-jdbc")`.

Create `ToolInvocationLogFacade.java` (EE header):

```java
package com.bytechef.ee.platform.tool.invocation.log.facade;

import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLog;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;

/**
 * The HTTP-facing read side of the tool invocation log; implementations own authorization. The log mixes automation
 * rows, which carry a workspace and an environment, with embedded rows, which carry neither, so it is read two ways:
 * workspace members read one workspace's rows in one environment, tenant admins read every row.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ToolInvocationLogFacade {

    /**
     * Every row of the tenant, narrowed by the given filters. Tenant admins only.
     */
    Page<ToolInvocationLog> getToolInvocationLogs(
        @Nullable ToolExecutionSurface surface, @Nullable ToolExecutionOutcome outcome, @Nullable Long mcpServerId,
        @Nullable Long connectedUserId, @Nullable Long integrationInstanceId, @Nullable Long appId,
        @Nullable Instant startDate, @Nullable Instant endDate, int pageNumber);

    /**
     * The rows of one workspace in one environment, narrowed by the given filters; a filter naming another
     * workspace's resource matches nothing.
     */
    Page<ToolInvocationLog> getWorkspaceToolInvocationLogs(
        long workspaceId, long environmentId, @Nullable ToolExecutionSurface surface,
        @Nullable ToolExecutionOutcome outcome, @Nullable Long mcpServerId, @Nullable Long connectedUserId,
        @Nullable Long integrationInstanceId, @Nullable Long appId, @Nullable Instant startDate,
        @Nullable Instant endDate, int pageNumber);
}
```

Create `ToolInvocationLogFacadeImpl.java` (EE header):

```java
package com.bytechef.ee.platform.tool.invocation.log.facade;

import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLog;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class ToolInvocationLogFacadeImpl implements ToolInvocationLogFacade {

    private final ToolInvocationLogService toolInvocationLogService;

    @SuppressFBWarnings("EI")
    public ToolInvocationLogFacadeImpl(ToolInvocationLogService toolInvocationLogService) {
        this.toolInvocationLogService = toolInvocationLogService;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public Page<ToolInvocationLog> getToolInvocationLogs(
        @Nullable ToolExecutionSurface surface, @Nullable ToolExecutionOutcome outcome, @Nullable Long mcpServerId,
        @Nullable Long connectedUserId, @Nullable Long integrationInstanceId, @Nullable Long appId,
        @Nullable Instant startDate, @Nullable Instant endDate, int pageNumber) {

        return toolInvocationLogService.getToolInvocationLogs(
            surface, outcome, mcpServerId, connectedUserId, integrationInstanceId, appId, startDate, endDate,
            pageNumber);
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEnvironmentId(#workspaceId, 'EXECUTION_VIEW', #environmentId)")
    public Page<ToolInvocationLog> getWorkspaceToolInvocationLogs(
        long workspaceId, long environmentId, @Nullable ToolExecutionSurface surface,
        @Nullable ToolExecutionOutcome outcome, @Nullable Long mcpServerId, @Nullable Long connectedUserId,
        @Nullable Long integrationInstanceId, @Nullable Long appId, @Nullable Instant startDate,
        @Nullable Instant endDate, int pageNumber) {

        return toolInvocationLogService.getWorkspaceToolInvocationLogs(
            workspaceId, Math.toIntExact(environmentId), surface, outcome, mcpServerId, connectedUserId,
            integrationInstanceId, appId, startDate, endDate, pageNumber);
    }
}
```

- [ ] **Step 5: Route the controller through the facade and add the query**

`tool-invocation-log.graphqls` — replace the `extend type Query { ... }` block with:

```graphql
extend type Query {
    toolInvocationLogs(
        surface: String
        outcome: String
        mcpServerId: Long
        connectedUserId: Long
        integrationInstanceId: Long
        appId: Long
        fromDate: Long
        toDate: Long
        page: Int = 0
    ): ToolInvocationLogPageType!
    workspaceToolInvocationLogs(
        workspaceId: ID!
        environmentId: ID!
        surface: String
        outcome: String
        mcpServerId: Long
        connectedUserId: Long
        integrationInstanceId: Long
        appId: Long
        fromDate: Long
        toDate: Long
        page: Int = 0
    ): ToolInvocationLogPageType!
}
```

`ToolInvocationLogGraphQlController` — replace the `ToolInvocationLogService` import with
`import com.bytechef.ee.platform.tool.invocation.log.facade.ToolInvocationLogFacade;`; the class Javadoc becomes
`GraphQL controller exposing read-only access to the tool invocation log. {@link ToolInvocationLogFacade} owns all
authorization, so this controller carries no {@code @PreAuthorize}: {@code toolInvocationLogs} is the tenant-wide log
for tenant admins, {@code workspaceToolInvocationLogs} one workspace's log in one environment.` (keep `@version ee`
and `@author`). Replace the field, constructor and `toolInvocationLogs` method with:

```java
    private final ToolInvocationLogFacade toolInvocationLogFacade;

    @SuppressFBWarnings("EI")
    public ToolInvocationLogGraphQlController(ToolInvocationLogFacade toolInvocationLogFacade) {
        this.toolInvocationLogFacade = toolInvocationLogFacade;
    }

    @QueryMapping(name = "toolInvocationLogs")
    public ToolInvocationLogPage toolInvocationLogs(
        @Argument @Nullable String surface, @Argument @Nullable String outcome, @Argument @Nullable Long mcpServerId,
        @Argument @Nullable Long connectedUserId, @Argument @Nullable Long integrationInstanceId,
        @Argument @Nullable Long appId, @Argument @Nullable Long fromDate, @Argument @Nullable Long toDate,
        @Argument @Nullable Integer page) {

        return toPage(
            toolInvocationLogFacade.getToolInvocationLogs(
                toSurface(surface), toOutcome(outcome), mcpServerId, connectedUserId, integrationInstanceId, appId,
                toInstant(fromDate), toInstant(toDate), page == null ? 0 : page));
    }

    @QueryMapping(name = "workspaceToolInvocationLogs")
    public ToolInvocationLogPage workspaceToolInvocationLogs(
        @Argument long workspaceId, @Argument long environmentId, @Argument @Nullable String surface,
        @Argument @Nullable String outcome, @Argument @Nullable Long mcpServerId,
        @Argument @Nullable Long connectedUserId, @Argument @Nullable Long integrationInstanceId,
        @Argument @Nullable Long appId, @Argument @Nullable Long fromDate, @Argument @Nullable Long toDate,
        @Argument @Nullable Integer page) {

        return toPage(
            toolInvocationLogFacade.getWorkspaceToolInvocationLogs(
                workspaceId, environmentId, toSurface(surface), toOutcome(outcome), mcpServerId, connectedUserId,
                integrationInstanceId, appId, toInstant(fromDate), toInstant(toDate), page == null ? 0 : page));
    }

    private static ToolInvocationLogPage toPage(Page<ToolInvocationLog> toolInvocationLogPage) {
        List<ToolInvocationLogItem> content = toolInvocationLogPage.getContent()
            .stream()
            .map(ToolInvocationLogGraphQlController::toItem)
            .toList();

        return new ToolInvocationLogPage(
            content, toolInvocationLogPage.getNumber(), toolInvocationLogPage.getSize(),
            toolInvocationLogPage.getTotalElements(), toolInvocationLogPage.getTotalPages());
    }
```

(`toSurface`, `toOutcome`, `toInstant`, `toItem` and the two records stay as they are.)

- [ ] **Step 6: Run the tests to verify they pass**

Same command as Step 2 with log `t8-green.log`. Expected: `exit=0`, no `FAILED`.

- [ ] **Step 7: Update the docs**

`.agents/apps.md` (module list, line ~40): replace `an \`appId\` filter on \`toolInvocationLogs\`.` with `an \`appId\`
filter on \`toolInvocationLogs\`. Reads go through \`ToolInvocationLogFacade\`: \`workspaceToolInvocationLogs(workspaceId,
environmentId, ...)\` needs \`EXECUTION_VIEW\` in that workspace and environment and is what the automation Tool
Invocations page and the app Activity tab call; the tenant-wide \`toolInvocationLogs\` is \`isTenantAdmin()\` and serves
the embedded Tool Invocations page and Connected User sheet.` (no backslashes; rewrap).

`docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md`: replace `Authorization of
\`ToolInvocationLogGraphQlController\`, which has no \`@PreAuthorize\` today, is a pre-existing gap fixed separately
and is out of this spec's scope.` with `Reads are authorized on \`ToolInvocationLogFacade\`: the Activity tab and the
automation Tool Invocations page use \`workspaceToolInvocationLogs\` (\`EXECUTION_VIEW\` in the workspace and
environment, rows filtered by \`workspace_id\` and \`environment\`); \`toolInvocationLogs\` is tenant-admin only.`

- [ ] **Step 8: Format, full check, commit**

```bash
SCRATCH=<your session scratchpad directory>
./gradlew --continue :server:ee:libs:platform:platform-tool-invocation-log:platform-tool-invocation-log-api:spotlessApply :server:ee:libs:platform:platform-tool-invocation-log:platform-tool-invocation-log-service:spotlessApply :server:ee:libs:platform:platform-tool-invocation-log:platform-tool-invocation-log-graphql:spotlessApply > "$SCRATCH/t8-spotless.log" 2>&1
echo "exit=$?"
./gradlew --continue :server:ee:libs:platform:platform-tool-invocation-log:platform-tool-invocation-log-api:check :server:ee:libs:platform:platform-tool-invocation-log:platform-tool-invocation-log-service:check :server:ee:libs:platform:platform-tool-invocation-log:platform-tool-invocation-log-graphql:check :server:ee:libs:embedded:embedded-execution:embedded-execution-public-rest:compileTestJava > "$SCRATCH/t8-check.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' "$SCRATCH/t8-check.log"
git branch --show-current
git add server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-api/src/main/java/com/bytechef/ee/platform/tool/invocation/log/ToolInvocationLogService.java server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-api/src/main/java/com/bytechef/ee/platform/tool/invocation/log/facade/ToolInvocationLogFacade.java server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-service/build.gradle.kts server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-service/src/main/java/com/bytechef/ee/platform/tool/invocation/log/ToolInvocationLogServiceImpl.java server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-service/src/main/java/com/bytechef/ee/platform/tool/invocation/log/repository/ToolInvocationLogRepository.java server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-service/src/main/java/com/bytechef/ee/platform/tool/invocation/log/facade/ToolInvocationLogFacadeImpl.java server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-service/src/test/java/com/bytechef/ee/platform/tool/invocation/log/ToolInvocationLogServiceIntTest.java server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-service/src/test/java/com/bytechef/ee/platform/tool/invocation/log/facade/ToolInvocationLogFacadeTest.java server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-graphql/build.gradle.kts server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-graphql/src/main/java/com/bytechef/ee/platform/tool/invocation/log/web/graphql/ToolInvocationLogGraphQlController.java server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-graphql/src/main/resources/graphql/tool-invocation-log.graphqls server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-graphql/src/test/java/com/bytechef/ee/platform/tool/invocation/log/web/graphql/ToolInvocationLogGraphQlControllerIntTest.java server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-graphql/src/test/java/com/bytechef/ee/platform/tool/invocation/log/web/graphql/config/ToolInvocationLogGraphQlTestConfiguration.java server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-graphql/src/test/resources/graphql/test.graphqls
git status --short server/ee/libs/platform/platform-tool-invocation-log
git commit -m "Authorize the tool invocation log through a facade and add a workspace-scoped query"
git add .agents/apps.md docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md
git commit -m "docs - Describe the tool invocation log authorization split"
```

`git status --short` must show the deleted `ToolInvocationLogGraphQlControllerTest.java` as staged (`D `) before the
commit, so the commit carries the deletion.

---

### Task 9: Scope the automation tool invocation views to the workspace and environment (design item 9, client)

**Files:**
- Create: `client/src/graphql/tool-invocation-log/workspaceToolInvocationLogs.graphql`
- Regenerate: `client/src/shared/middleware/graphql.ts`, `client/src/shared/middleware/graphql-types.ts`
- Modify: `client/src/pages/automation/tool-invocations/hooks/useToolInvocations.ts`
- Modify: `client/src/pages/automation/tool-invocations/ToolInvocations.tsx:43-48,70`
- Modify: `client/src/pages/automation/tool-invocations/AutomationToolInvocations.tsx`
- Modify: `client/src/pages/automation/apps/components/AppActivityTab.tsx`
- Test: `client/src/pages/automation/tool-invocations/hooks/useToolInvocations.test.tsx`
- Test: `client/src/pages/automation/apps/tests/AppActivityTab.test.tsx`

Unchanged by design: the embedded route renders `ToolInvocations` with no props (tenant-wide query, now admin-only),
and `ConnectedUserSheetPanelToolInvocationList` keeps `useToolInvocationLogsQuery({connectedUserId})`.
`client/codegen.ts` already lists the tool-invocation-log `*.graphqls` path. `useWorkspaceStore` /
`useEnvironmentStore` expose `currentWorkspaceId` / `currentEnvironmentId` (numbers; the workspace one is `undefined`
until hydrated). Generated hooks take `(variables, options)`.

**Interfaces:**
- Consumes (Task 8): GraphQL `workspaceToolInvocationLogs`.
- Produces: `useWorkspaceToolInvocationLogsQuery` (generated);
  `export interface ToolInvocationsWorkspaceScopeI {environmentId: number | undefined; workspaceId: number | undefined}`;
  `useToolInvocations(workspaceScope?: ToolInvocationsWorkspaceScopeI)`; `ToolInvocations` prop
  `workspaceScope?: ToolInvocationsWorkspaceScopeI`.

- [ ] **Step 1: Add the operation and commit it**

Create `workspaceToolInvocationLogs.graphql`:

```graphql
query WorkspaceToolInvocationLogs(
    $workspaceId: ID!
    $environmentId: ID!
    $surface: String
    $outcome: String
    $mcpServerId: Long
    $connectedUserId: Long
    $integrationInstanceId: Long
    $appId: Long
    $fromDate: Long
    $toDate: Long
    $page: Int
) {
    workspaceToolInvocationLogs(
        workspaceId: $workspaceId
        environmentId: $environmentId
        surface: $surface
        outcome: $outcome
        mcpServerId: $mcpServerId
        connectedUserId: $connectedUserId
        integrationInstanceId: $integrationInstanceId
        appId: $appId
        fromDate: $fromDate
        toDate: $toDate
        page: $page
    ) {
        content {
            id
            surface
            kind
            toolName
            componentName
            componentVersion
            operationName
            connectionId
            environment
            externalUserId
            connectedUserId
            integrationInstanceId
            mcpServerId
            jobId
            appId
            outcome
            errorType
            errorMessage
            durationMs
            createdDate
        }
        number
        size
        totalElements
        totalPages
    }
}
```

```bash
git branch --show-current
git add client/src/graphql/tool-invocation-log/workspaceToolInvocationLogs.graphql
git commit -m "client - Add the workspace tool invocation log GraphQL operation"
```

- [ ] **Step 2: Regenerate the GraphQL client and commit it**

```bash
SCRATCH=<your session scratchpad directory>
cd client && npx graphql-codegen > "$SCRATCH/t9-codegen.log" 2>&1
echo "exit=$?"
```

Expected: `exit=0`. Then from the worktree root run `git diff --stat` and `git diff client/src/shared/middleware`: the
only hunks must add the `WorkspaceToolInvocationLogs` document, its query/variables types,
`useWorkspaceToolInvocationLogsQuery` and the `workspaceToolInvocationLogs` field on `Query`. If any unrelated hunk
appears (schema drift from other modules), stop and report instead of committing.

```bash
git add client/src/shared/middleware/graphql.ts client/src/shared/middleware/graphql-types.ts
git commit -m "client - Regenerate the GraphQL client"
```

(If `graphql-types.ts` did not change, leave it out of `git add`.)

- [ ] **Step 3: Write the failing tests**

Replace `useToolInvocations.test.tsx` with:

```tsx
import {act, renderHook} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {ToolInvocationsWorkspaceScopeI, useToolInvocations} from './useToolInvocations';

const hoisted = vi.hoisted(() => ({
    useToolInvocationLogsQueryMock: vi.fn(),
    useWorkspaceToolInvocationLogsQueryMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useToolInvocationLogsQuery: hoisted.useToolInvocationLogsQueryMock,
    useWorkspaceToolInvocationLogsQuery: hoisted.useWorkspaceToolInvocationLogsQueryMock,
}));

const emptyQueryResult = () => ({
    data: undefined,
    error: null,
    isFetching: false,
    isLoading: false,
    refetch: vi.fn(),
});

const renderUseToolInvocations = (initialEntry: string, workspaceScope?: ToolInvocationsWorkspaceScopeI) =>
    renderHook(() => useToolInvocations(workspaceScope), {
        wrapper: ({children}) => <MemoryRouter initialEntries={[initialEntry]}>{children}</MemoryRouter>,
    });

describe('useToolInvocations', () => {
    beforeEach(() => {
        hoisted.useToolInvocationLogsQueryMock.mockReset();
        hoisted.useToolInvocationLogsQueryMock.mockReturnValue(emptyQueryResult());
        hoisted.useWorkspaceToolInvocationLogsQueryMock.mockReset();
        hoisted.useWorkspaceToolInvocationLogsQueryMock.mockReturnValue(emptyQueryResult());
    });

    it('reads the appId search parameter into the initial filter', () => {
        const {result} = renderUseToolInvocations('/automation/executions/tool-invocations?appId=7');

        expect(result.current.filterAppId).toBe('7');
        expect(hoisted.useToolInvocationLogsQueryMock).toHaveBeenCalledWith(
            expect.objectContaining({appId: 7, page: 0}),
            {enabled: true}
        );
    });

    it('ignores a malformed appId search parameter', () => {
        const {result} = renderUseToolInvocations('/automation/executions/tool-invocations?appId=abc');

        expect(result.current.filterAppId).toBeUndefined();
        expect(hoisted.useToolInvocationLogsQueryMock).toHaveBeenCalledWith(
            expect.objectContaining({appId: undefined}),
            {enabled: true}
        );
    });

    it('does not filter by app without the search parameter', () => {
        const {result} = renderUseToolInvocations('/automation/executions/tool-invocations');

        expect(result.current.filterAppId).toBeUndefined();
        expect(hoisted.useToolInvocationLogsQueryMock).toHaveBeenCalledWith(
            expect.objectContaining({appId: undefined}),
            {enabled: true}
        );
    });

    it('changes the app filter and resets the page', () => {
        const {result} = renderUseToolInvocations('/automation/executions/tool-invocations');

        act(() => result.current.handlePaginationClick(3));

        expect(result.current.filterPageNumber).toBe(3);

        act(() => result.current.handleAppChange({label: 'CRM', value: '9'}));

        expect(result.current.filterAppId).toBe('9');
        expect(result.current.filterPageNumber).toBe(0);

        act(() => result.current.handleAppChange({label: 'Any', value: ''}));

        expect(result.current.filterAppId).toBeUndefined();
    });

    it('queries the tenant-wide log and leaves the workspace query off without a workspace scope', () => {
        renderUseToolInvocations('/embedded/executions/tool-invocations');

        expect(hoisted.useToolInvocationLogsQueryMock).toHaveBeenCalledWith(expect.anything(), {enabled: true});
        expect(hoisted.useWorkspaceToolInvocationLogsQueryMock).toHaveBeenCalledWith(expect.anything(), {
            enabled: false,
        });
    });

    it('queries the workspace log with the workspace and environment of the scope', () => {
        hoisted.useWorkspaceToolInvocationLogsQueryMock.mockReturnValue({
            ...emptyQueryResult(),
            data: {
                workspaceToolInvocationLogs: {
                    content: [{id: '1', toolName: 'CRM__SEARCH_RECORDS'}],
                    number: 0,
                    size: 50,
                    totalElements: 1,
                    totalPages: 1,
                },
            },
        });

        const {result} = renderUseToolInvocations('/automation/executions/tool-invocations?appId=7', {
            environmentId: 2,
            workspaceId: 1049,
        });

        expect(hoisted.useWorkspaceToolInvocationLogsQueryMock).toHaveBeenCalledWith(
            expect.objectContaining({appId: 7, environmentId: '2', page: 0, workspaceId: '1049'}),
            {enabled: true}
        );
        expect(hoisted.useToolInvocationLogsQueryMock).toHaveBeenCalledWith(expect.anything(), {enabled: false});
        expect(result.current.toolInvocationLogs).toEqual([{id: '1', toolName: 'CRM__SEARCH_RECORDS'}]);
        expect(result.current.toolInvocationLogPage?.totalElements).toBe(1);
    });

    it('waits for the workspace instead of falling back to the tenant-wide log', () => {
        renderUseToolInvocations('/automation/executions/tool-invocations', {
            environmentId: 2,
            workspaceId: undefined,
        });

        expect(hoisted.useWorkspaceToolInvocationLogsQueryMock).toHaveBeenCalledWith(expect.anything(), {
            enabled: false,
        });
        expect(hoisted.useToolInvocationLogsQueryMock).toHaveBeenCalledWith(expect.anything(), {enabled: false});
    });
});
```

In `AppActivityTab.test.tsx`:
- Add imports `import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';` and
  `import {environmentStore} from '@/shared/stores/useEnvironmentStore';` (sorted among the existing imports).
  `useEnvironmentStore` is a selector function over the vanilla `environmentStore`, so state is set on the latter;
  `useWorkspaceStore` is a zustand hook and carries `setState` itself.
- Rename `useToolInvocationLogsQueryMock` to `useWorkspaceToolInvocationLogsQueryMock` everywhere, and make the module
  mock `useWorkspaceToolInvocationLogsQuery: hoisted.useWorkspaceToolInvocationLogsQueryMock`.
- Every `toolInvocationLogs:` key inside a mocked `data` object becomes `workspaceToolInvocationLogs:`.
- In `beforeEach`, after the reset, add:

```tsx
        environmentStore.setState({currentEnvironmentId: 2});
        useWorkspaceStore.setState({currentWorkspaceId: 7});
```

- `toHaveBeenCalledWith({appId: 1, page: 0})` becomes
  `toHaveBeenCalledWith({appId: 1, environmentId: '2', page: 0, workspaceId: '7'}, {enabled: true})`, and
  `toHaveBeenLastCalledWith({appId: 1, page: 1})` becomes
  `toHaveBeenLastCalledWith({appId: 1, environmentId: '2', page: 1, workspaceId: '7'}, {enabled: true})`.

- [ ] **Step 4: Run the tests to verify they fail**

```bash
SCRATCH=<your session scratchpad directory>
cd client && npx vitest run src/pages/automation/tool-invocations/hooks/useToolInvocations.test.tsx src/pages/automation/apps/tests/AppActivityTab.test.tsx > "$SCRATCH/t9-red.log" 2>&1
echo "exit=$?"
grep -E 'FAIL|✗|×' "$SCRATCH/t9-red.log"
```

Expected: non-zero exit; the new workspace tests and every `AppActivityTab` test fail (the hook ignores the scope; the
tab still calls the tenant query).

- [ ] **Step 5: Implement the hook**

Replace `useToolInvocations.ts` with:

```ts
import {ComboBoxItemType} from '@/components/ComboBox/ComboBox';
import {parseAppId} from '@/pages/automation/tool-invocations/utils/parseAppId';
import {
    ToolInvocationLogsQuery,
    ToolInvocationLogsQueryVariables,
    useToolInvocationLogsQuery,
    useWorkspaceToolInvocationLogsQuery,
} from '@/shared/middleware/graphql';
import {useState} from 'react';
import {useSearchParams} from 'react-router-dom';

type ToolInvocationLogItemType = ToolInvocationLogsQuery['toolInvocationLogs']['content'][number];

export interface ToolInvocationsWorkspaceScopeI {
    environmentId: number | undefined;
    workspaceId: number | undefined;
}

interface ToolInvocationsFiltersI {
    appId: string | undefined;
    endDate: Date | undefined;
    mcpServerId: string | undefined;
    outcome: string | undefined;
    pageNumber: number;
    startDate: Date | undefined;
    surface: string | undefined;
}

interface UseToolInvocationsI {
    filterAppId: string | undefined;
    filterEndDate: Date | undefined;
    filterMcpServerId: string | undefined;
    filterOutcome: string | undefined;
    filterPageNumber: number;
    filterStartDate: Date | undefined;
    filterSurface: string | undefined;
    handleAppChange: (item?: ComboBoxItemType) => void;
    handleEndDateChange: (date: Date | undefined) => void;
    handleMcpServerChange: (item?: ComboBoxItemType) => void;
    handleOutcomeChange: (item?: ComboBoxItemType) => void;
    handlePaginationClick: (pageNumber: number) => void;
    handleStartDateChange: (date: Date | undefined) => void;
    handleSurfaceChange: (item?: ComboBoxItemType) => void;
    refetchToolInvocations: () => void;
    toolInvocationLogPage: ToolInvocationLogsQuery['toolInvocationLogs'] | undefined;
    toolInvocationLogs: Array<ToolInvocationLogItemType>;
    toolInvocationLogsError: Error | null;
    toolInvocationLogsIsFetching: boolean;
    toolInvocationLogsIsLoading: boolean;
}

const toFilterVariables = (filters: ToolInvocationsFiltersI): ToolInvocationLogsQueryVariables => ({
    appId: parseAppId(filters.appId),
    fromDate: filters.startDate ? filters.startDate.getTime() : undefined,
    mcpServerId: filters.mcpServerId ? Number(filters.mcpServerId) : undefined,
    outcome: filters.outcome || undefined,
    page: filters.pageNumber,
    surface: filters.surface || undefined,
    toDate: filters.endDate ? filters.endDate.getTime() : undefined,
});

/**
 * With a workspace scope the hook reads that workspace's log in that environment and never the tenant-wide log, even
 * while the scope is incomplete; without one it reads the tenant-wide log, which the server allows tenant admins only.
 */
export const useToolInvocations = (workspaceScope?: ToolInvocationsWorkspaceScopeI): UseToolInvocationsI => {
    const [searchParams] = useSearchParams();

    const [filterAppId, setFilterAppId] = useState<string | undefined>(() => {
        const appId = parseAppId(searchParams.get('appId'));

        return appId === undefined ? undefined : String(appId);
    });
    const [filterEndDate, setFilterEndDate] = useState<Date | undefined>(undefined);
    const [filterMcpServerId, setFilterMcpServerId] = useState<string | undefined>(undefined);
    const [filterOutcome, setFilterOutcome] = useState<string | undefined>(undefined);
    const [filterPageNumber, setFilterPageNumber] = useState<number>(0);
    const [filterStartDate, setFilterStartDate] = useState<Date | undefined>(undefined);
    const [filterSurface, setFilterSurface] = useState<string | undefined>(undefined);

    const filterVariables = toFilterVariables({
        appId: filterAppId,
        endDate: filterEndDate,
        mcpServerId: filterMcpServerId,
        outcome: filterOutcome,
        pageNumber: filterPageNumber,
        startDate: filterStartDate,
        surface: filterSurface,
    });
    const workspaceScoped = workspaceScope !== undefined;

    const tenantQuery = useToolInvocationLogsQuery(filterVariables, {enabled: !workspaceScoped});

    const workspaceQuery = useWorkspaceToolInvocationLogsQuery(
        {
            ...filterVariables,
            environmentId: String(workspaceScope?.environmentId),
            workspaceId: String(workspaceScope?.workspaceId),
        },
        {enabled: workspaceScope?.environmentId != null && workspaceScope?.workspaceId != null}
    );

    const activeQuery = workspaceScoped ? workspaceQuery : tenantQuery;
    const toolInvocationLogPage = workspaceScoped
        ? workspaceQuery.data?.workspaceToolInvocationLogs
        : tenantQuery.data?.toolInvocationLogs;

    const handleAppChange = (item?: ComboBoxItemType) => {
        const appId = parseAppId(item?.value ? String(item.value) : undefined);

        setFilterAppId(appId === undefined ? undefined : String(appId));
        setFilterPageNumber(0);
    };

    const handleEndDateChange = (date: Date | undefined) => {
        setFilterEndDate(date);
        setFilterPageNumber(0);
    };

    const handleMcpServerChange = (item?: ComboBoxItemType) => {
        setFilterMcpServerId(item?.value ? String(item.value) : undefined);
        setFilterPageNumber(0);
    };

    const handleOutcomeChange = (item?: ComboBoxItemType) => {
        setFilterOutcome(item?.value ? String(item.value) : undefined);
        setFilterPageNumber(0);
    };

    const handlePaginationClick = (pageNumber: number) => setFilterPageNumber(pageNumber);

    const handleStartDateChange = (date: Date | undefined) => {
        setFilterStartDate(date);
        setFilterPageNumber(0);
    };

    const handleSurfaceChange = (item?: ComboBoxItemType) => {
        setFilterSurface(item?.value ? String(item.value) : undefined);
        setFilterPageNumber(0);
    };

    return {
        filterAppId,
        filterEndDate,
        filterMcpServerId,
        filterOutcome,
        filterPageNumber,
        filterStartDate,
        filterSurface,
        handleAppChange,
        handleEndDateChange,
        handleMcpServerChange,
        handleOutcomeChange,
        handlePaginationClick,
        handleStartDateChange,
        handleSurfaceChange,
        refetchToolInvocations: activeQuery.refetch,
        toolInvocationLogPage,
        toolInvocationLogs: toolInvocationLogPage?.content ?? [],
        toolInvocationLogsError: (activeQuery.error as Error | null) ?? null,
        toolInvocationLogsIsFetching: activeQuery.isFetching,
        toolInvocationLogsIsLoading: activeQuery.isLoading,
    };
};
```

- [ ] **Step 6: Pass the scope from the automation page and switch the Activity tab**

`ToolInvocations.tsx`: add `ToolInvocationsWorkspaceScopeI` to the `useToolInvocations` import
(`import {ToolInvocationsWorkspaceScopeI, useToolInvocations} from '@/pages/automation/tool-invocations/hooks/useToolInvocations';`),
make the props

```tsx
interface ToolInvocationsProps {
    appOptions?: Array<ComboBoxItemType>;
    mcpServerOptions?: Array<ComboBoxItemType>;
    workspaceScope?: ToolInvocationsWorkspaceScopeI;
}

export const ToolInvocations = ({appOptions, mcpServerOptions, workspaceScope}: ToolInvocationsProps) => {
```

and change `} = useToolInvocations();` to `} = useToolInvocations(workspaceScope);`.

`AutomationToolInvocations.tsx`: after the `mcpServerOptions` `useMemo`, add

```tsx
    const workspaceScope = useMemo(
        () => ({environmentId: currentEnvironmentId, workspaceId: currentWorkspaceId}),
        [currentEnvironmentId, currentWorkspaceId]
    );
```

and render
`<ToolInvocations appOptions={appOptions} mcpServerOptions={mcpServerOptions} workspaceScope={workspaceScope} />`.

`AppActivityTab.tsx`: replace the imports and `AppActivityLogList` head with:

```tsx
import Button from '@/components/Button/Button';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ToolInvocationsTable from '@/pages/automation/tool-invocations/components/ToolInvocationsTable';
import {parseAppId} from '@/pages/automation/tool-invocations/utils/parseAppId';
import {WorkspaceToolInvocationLogsQuery, useWorkspaceToolInvocationLogsQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ArrowUpRightIcon} from 'lucide-react';
import {useEffect, useState} from 'react';
import {Link} from 'react-router-dom';

type ToolInvocationLogItemType =
    WorkspaceToolInvocationLogsQuery['workspaceToolInvocationLogs']['content'][number];

interface AppActivityTabProps {
    appId: string;
}

const AppActivityLogList = ({appId}: {appId: number}) => {
    const [loadedLogs, setLoadedLogs] = useState<Array<ToolInvocationLogItemType>>([]);
    const [pageNumber, setPageNumber] = useState(0);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data, isLoading} = useWorkspaceToolInvocationLogsQuery(
        {
            appId,
            environmentId: String(currentEnvironmentId),
            page: pageNumber,
            workspaceId: String(currentWorkspaceId),
        },
        {enabled: currentEnvironmentId != null && currentWorkspaceId != null}
    );

    const toolInvocationLogPage = data?.workspaceToolInvocationLogs;
```

(The rest of the component is unchanged.)

- [ ] **Step 7: Run the tests to verify they pass**

Same command as Step 4 with log `t9-green.log`. Expected: `exit=0`, all tests pass.

- [ ] **Step 8: Format, full client check, commit**

```bash
SCRATCH=<your session scratchpad directory>
cd client && npm run format > "$SCRATCH/t9-format.log" 2>&1
echo "exit=$?"
npm run check > "$SCRATCH/t9-check.log" 2>&1
echo "exit=$?"
grep -nE 'error|✗|FAIL|Unhandled' "$SCRATCH/t9-check.log"
```

Expected: `exit=0` for both. `.agents/apps.md` records that `npm run check` may fail on this branch only because of
pre-existing `no-restricted-imports` warnings in files this plan does not touch. If `check` exits non-zero, prove the
touched files are clean, each with its own `$?`:

```bash
SCRATCH=<your session scratchpad directory>
cd client && npx eslint --max-warnings=0 src/pages/automation/tool-invocations src/pages/automation/apps/components/AppActivityTab.tsx src/pages/automation/apps/tests/AppActivityTab.test.tsx > "$SCRATCH/t9-eslint.log" 2>&1
echo "exit=$?"
npm run typecheck > "$SCRATCH/t9-tsc.log" 2>&1
echo "exit=$?"
npx vitest run > "$SCRATCH/t9-vitest.log" 2>&1
echo "exit=$?"
grep -n 'Unhandled' "$SCRATCH/t9-vitest.log"
```

All three must be `exit=0` with no `Unhandled`, and every warning left in `t9-check.log` must be in a file outside this
task's list. Then, from the worktree root:

```bash
git branch --show-current
git add client/src/pages/automation/tool-invocations/hooks/useToolInvocations.ts client/src/pages/automation/tool-invocations/hooks/useToolInvocations.test.tsx client/src/pages/automation/tool-invocations/ToolInvocations.tsx client/src/pages/automation/tool-invocations/AutomationToolInvocations.tsx client/src/pages/automation/apps/components/AppActivityTab.tsx client/src/pages/automation/apps/tests/AppActivityTab.test.tsx
git commit -m "client - Scope the automation tool invocation views to the current workspace and environment"
```

If `npm run format` reformatted any other file, do not stage it.
