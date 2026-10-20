# Apps — Workflow Bindings (A4) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** An app can bind deployed **Call workflow** workflows and run them through `/api/app/v1`, governed like its
connections: allow-listed by an admin, logged per app, stopped by disable or key rotation. A run that does not finish
within the wait (or waits on an approval) answers 202 with a `runId` the app polls.

**Architecture:** Everything lives in the existing EE `automation-app` module, beside the connection bindings, but in
classes of its own: `AppWorkflow`/`AppWorkflowRun` (domain), `AppWorkflowService` (persistence and bind rules),
`AppCallableWorkflowResolver` (binding → current workflow, availability, schemas), `AppWorkflowFacade` (admin, gated)
and `AppWorkflowExecutionFacade` (app-facing). Runs are created exactly as MCP and webhooks create them
(`PrincipalJobFacade.createJob` with the project deployment as the principal) and awaited with `JobCompletionAwaiter`.
MCP's result and approval helpers move into `WorkflowCallResults` in `platform-workflow-execution-api` so both paths
share them.

**Tech Stack:** Java 25 / Spring Boot 4, Spring Data JDBC, Liquibase, Spring GraphQL, openapi-generator 7.25
(`spring`, `interfaceOnly`), networknt `json-schema-validator` 3.x (already on the classpath through Spring AI),
Jackson 3 (`tools.jackson`), React 19 + generated GraphQL hooks, Vitest.

**Spec:** `docs/superpowers/specs/2026-10-06-apps-workflows-and-sdk-design.md` (section "Design — A4. Workflow
bindings", "Testing — A4 server"). Background: `docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md`
and `.agents/apps.md`.

## Spec status

The spec was reviewed against this plan on 2026-10-07 and synced: facade names (`AppWorkflowFacade`,
`AppWorkflowExecutionFacade`), the `app_workflow_run` shape and its retention job, the extra availability causes
(disabled deployment, unreadable `inputSchema`), the bind-time error codes, the approval-link capability note and the
test list all now agree with the tasks below. There are no open deviations. `automation-app-service` has no workflow
engine on its test classpath, so no task runs a real workflow; the spec's Testing section says so.

## Global Constraints

- All new files under `server/ee/` carry the ByteChef Enterprise header and `@version ee` Javadoc. Header, verbatim:
  ```java
  /*
   * Copyright 2025 ByteChef
   *
   * Licensed under the ByteChef Enterprise license (the "Enterprise License");
   * you may not use this file except in compliance with the Enterprise License.
   */
  ```
  Files under `server/libs/` (CE) keep the Apache 2.0 header of their neighbours.
- Every new Spring bean in `automation-app-service` carries `@ConditionalOnEEVersion`.
- Enum ordinals are persisted as INT: append only. `ToolExecutionSurface` gains `AUTOMATION_APP_WORKFLOW` at the end.
  `AppAuditEvent` values are appended at the end.
- New `AppErrorType` keys continue from 118. Adding one means: a `NAMES` entry, the count in `AppErrorTypeTest`, and a
  branch in `AbstractAppApiController.toAppStatus` when its status is not 400.
- Workflow keys follow the binding-key rule `^[a-z][a-z0-9_-]{0,63}$` and are unique per app after
  `AppBindingKeys.toFlatKey` normalisation (among workflow bindings).
- Bindings store `(project_deployment_id, workflow_uuid)`; the workflow id is resolved on every call through
  `ProjectWorkflowService.fetchProjectWorkflowWorkflowId(projectDeploymentId, workflowUuid)`.
- Settings `bytechef.app.workflow.default-wait` (30s), `bytechef.app.workflow.max-wait` (60s) and
  `bytechef.app.workflow.run-retention-days` (90) bind to `ApplicationProperties.App.WorkflowRun` (field `workflow`;
  the class is not named `Workflow` because `ApplicationProperties.Workflow` already exists at the top level);
  defaults live in `application-bytechef.yml`, never in Java initialisers.
- App-facing paths: `GET /workflows`, `POST /workflows/{workflowKey}`, `GET /workflow-runs/{runId}` under
  `${openapi.openAPIDefinition.base-path.app:/api/app}/v1`. Management paths: `GET /apps/{id}/workflows`,
  `PUT|DELETE /apps/{id}/workflows/{workflowKey}` under the automation base path.
- Facades use services, never repositories. Controllers go through the facades; the GraphQL controller owns no
  authorization (`@PreAuthorize` lives on the facade).
- Commit subjects: server `- <description>`, client `- client - <description>`, docs `- docs - <description>`. No
  `Co-Authored-By` or "Generated with" trailers. Never amend; commit by path, never `git add -A`.
- Before each server commit run `./gradlew spotlessApply` and revert the two unrelated files it touches if it does
  (`AbstractAiAgentChatActionTest.java`, `WebhookWorkflowExecutorImpl.java`). Before each client commit run
  `cd client && npx prettier --write <touched files>`.
- Gradle: pass `timeout: 600000` on every Gradle and npm Bash call; never background a command and never wait on one;
  never pipe Gradle into `tail`/`grep` — redirect to a file, check `$?` on its own line, then grep the file for
  `^> Task .* FAILED`. Run tests scoped to one class (`--tests "<FQCN>"`); the controller runs module-wide `check`.
- Checkstyle: test method names camelCase, no underscores; no `TODO:`; no empty blocks. No rationale `//` comments in
  committed code; Javadoc that states a contract is fine.
- Client: object keys sorted, interfaces end in `I` or `Props`, Lucide icons with the `Icon` suffix, `twMerge` not
  `cn()`, hooks in the order `useState` → `useRef` → store hooks → other hooks → derived → `useEffect`.

## Review Focus

Inputs the spec implies but no task's tests would otherwise exercise; each now has a test in the task named.

1. **A malformed `workflowUuid` on bind** (`PUT /apps/1/workflows/approve` with `"workflowUuid": "nope"`) must be 400
   `APP_WORKFLOW_NOT_BINDABLE`, not the 500 `UUID.fromString` would raise — Task 5
   (`AppWorkflowServiceTest.testBindRejectsAMalformedWorkflowUuid`).
2. **A run whose job was deleted by execution retention** must poll as 404 `APP_WORKFLOW_RUN_NOT_FOUND`, not 500 —
   Task 7 (`AppWorkflowExecutionFacadeTest.testGetRunOfAPurgedJobIsNotFound`).
3. **A trigger with no `inputSchema`** accepts any object input; **an unparseable one** makes the binding unavailable
   — Task 4 (`AppWorkflowSchemasTest`) and Task 7 (`testUnparseableInputSchemaIsUnavailable`).
4. **`waitSeconds: 0`** returns immediately with the job's current state and never calls the awaiter; **negative or
   above `max-wait`** is 400 — Task 7 (`testZeroWaitReadsTheJobWithoutAwaiting`, `testWaitOutOfRangeIsRejected`).
5. **Another app's `runId`** polls as 404 exactly like an unknown id — Task 7
   (`testAnotherAppsRunIsNotFound`), Task 3 (`AppWorkflowRunRepositoryIntTest.testAnotherAppsRunIdIsNotFound`) and
   Task 11 (`AppWorkflowExecutionFacadeIntTest.testAnotherAppCannotPollTheRun`).

---

## File structure

```
server/libs/platform/platform-workflow/platform-workflow-execution/platform-workflow-execution-api/
  build.gradle.kts                                                  (modify: two implementation deps)
  src/main/java/com/bytechef/platform/workflow/execution/WorkflowCallResults.java          (create)
  src/test/java/com/bytechef/platform/workflow/execution/WorkflowCallResultsTest.java      (create)
server/libs/automation/automation-ai/automation-ai-mcp-server/
  src/main/java/com/bytechef/automation/ai/mcp/server/facade/AutomationMcpToolFacade.java (modify: delegate)
server/libs/platform/platform-tool-execution/platform-tool-execution-api/
  src/main/java/com/bytechef/platform/tool/execution/ToolExecutionSurface.java             (modify: append)
  src/test/java/com/bytechef/platform/tool/execution/ToolExecutionEnumOrdinalStabilityTest.java (modify)
server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java (modify: App.WorkflowRun)
server/apps/server-app/src/main/resources/config/application-bytechef.yml                (modify: defaults)

server/ee/libs/automation/automation-app/
  automation-app-api/src/main/java/com/bytechef/ee/automation/app/
    domain/AppWorkflow.java, AppWorkflowRun.java                                          (create)
    dto/AppWorkflowBindingDTO.java, AppBindableWorkflowDTO.java, AppWorkflowDTO.java,
        AppWorkflowRunDTO.java, AppWorkflowRunStatus.java                                 (create)
    exception/AppErrorType.java                                                           (modify)
    facade/AppWorkflowFacade.java, AppWorkflowExecutionFacade.java                        (create)
    service/AppWorkflowService.java                                                       (create)
    util/AppBindingKeys.java                                                              (modify: isValidKey)
  automation-app-service/
    build.gradle.kts                                                                      (modify)
    src/main/java/com/bytechef/ee/automation/app/
      audit/AppAuditEvent.java                                                            (modify)
      repository/AppWorkflowRepository.java, AppWorkflowRunRepository.java               (create)
      service/AppWorkflowServiceImpl.java                                                 (create)
      service/AppConnectionServiceImpl.java                                               (modify: isValidKey)
      workflow/AppCallableWorkflowResolver.java, AppWorkflowSchemas.java,
               AppWorkflowRunRetentionJob.java                                            (create)
      facade/AppWorkflowFacadeImpl.java, AppWorkflowExecutionFacadeImpl.java              (create)
    src/main/resources/config/liquibase/changelog/automation/app/
      20261007000001_automation_app_workflow.xml                                          (create)
    src/test/java/com/bytechef/ee/automation/app/
      config/AppIntTestConfiguration.java                                                 (modify: mocks)
      repository/AppWorkflowRepositoryIntTest.java, AppWorkflowRunRepositoryIntTest.java (create)
      service/AppWorkflowServiceTest.java                                                 (create)
      workflow/AppCallableWorkflowResolverTest.java, AppWorkflowSchemasTest.java,
               AppWorkflowRunRetentionJobTest.java                                        (create)
      facade/AppWorkflowFacadeTest.java, AppWorkflowExecutionFacadeTest.java              (create)
  automation-app-graphql/
    src/main/java/com/bytechef/ee/automation/app/web/graphql/AppWorkflowGraphQlController.java (create)
    src/main/resources/graphql/app.graphqls                                               (modify)
    src/test/java/com/bytechef/ee/automation/app/web/graphql/AppWorkflowGraphQlControllerTest.java (create)
  automation-app-public-rest/
    openapi.yaml, openapi-management.yaml                                                 (modify)
    generated/...                                                                         (regenerated)
    src/main/java/com/bytechef/ee/automation/app/public_/web/rest/
      AbstractAppApiController.java                                                       (modify: statuses)
      AppWorkflowApiController.java, AppWorkflowBindingApiController.java                 (create)
    src/test/java/com/bytechef/ee/automation/app/public_/web/rest/
      AppWorkflowApiControllerTest.java, AppWorkflowBindingApiControllerTest.java         (create)
      AppApiControllerTest.java                                                           (modify: error rows)

client/src/graphql/automation/app/
  appWorkflows.graphql, appBindableWorkflows.graphql, bindAppWorkflow.graphql, unbindAppWorkflow.graphql (create)
client/src/shared/middleware/graphql.ts, graphql-types.ts                                 (regenerated)
client/src/pages/automation/apps/
  App.tsx                                                                                 (modify: tab)
  components/AppWorkflowsTab.tsx, AppWorkflowBindingDialog.tsx                            (create)
  tests/AppWorkflowsTab.test.tsx, AppWorkflowBindingDialog.test.tsx                       (create)
client/src/pages/automation/tool-invocations/
  components/ToolInvocationsTable.tsx, ToolInvocations.tsx, components/ToolInvocationsTable.test.tsx (modify)

.agents/apps.md, docs/superpowers/specs/2026-10-06-apps-workflows-and-sdk-design.md        (modify, Task 11)
```

Gradle project paths used below:

| Short | Gradle path |
|-------|-------------|
| `WFX_API` | `:server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api` |
| `MCP` | `:server:libs:automation:automation-ai:automation-ai-mcp-server` |
| `TOOL_API` | `:server:libs:platform:platform-tool-execution:platform-tool-execution-api` |
| `APP_API` | `:server:ee:libs:automation:automation-app:automation-app-api` |
| `APP_SVC` | `:server:ee:libs:automation:automation-app:automation-app-service` |
| `APP_GQL` | `:server:ee:libs:automation:automation-app:automation-app-graphql` |
| `APP_REST` | `:server:ee:libs:automation:automation-app:automation-app-public-rest` |
| `APP_CONFIG` | `:server:libs:config:app-config` |

---

### Task 1: Shared workflow-call results (`WorkflowCallResults`) and MCP delegation

**Files:**
- Modify: `server/libs/platform/platform-workflow/platform-workflow-execution/platform-workflow-execution-api/build.gradle.kts`
- Create: `.../platform-workflow-execution-api/src/main/java/com/bytechef/platform/workflow/execution/WorkflowCallResults.java`
- Create: `.../platform-workflow-execution-api/src/test/java/com/bytechef/platform/workflow/execution/WorkflowCallResultsTest.java`
- Modify: `server/libs/automation/automation-ai/automation-ai-mcp-server/src/main/java/com/bytechef/automation/ai/mcp/server/facade/AutomationMcpToolFacade.java`

**Interfaces:**
- Produces (used by Tasks 7):
  ```java
  public static Optional<Object> fetchCallableResponseOutput(
      Job job, TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage);
  public static @Nullable Object readResult(
      Job job, TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage);
  public static boolean isPendingApproval(Job job);
  public static Optional<String> fetchApprovalFormUrl(
      Job job, @Nullable String publicUrl, @Nullable ApprovalTokens approvalTokens);
  public static Optional<String> fetchResumeToken(Job job, @Nullable ApprovalTokens approvalTokens);
  ```
  all in `com.bytechef.platform.workflow.execution.WorkflowCallResults`.

- [ ] **Step 1: Add the two dependencies** to `platform-workflow-execution-api/build.gradle.kts`, inside `dependencies { }`
  after the existing `implementation(project(":server:libs:core:commons:commons-util"))` line:

  ```kotlin
      implementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))
      implementation(project(":server:libs:platform:platform-component:platform-component-api"))
  ```

  and add to the test block:

  ```kotlin
      testImplementation("org.mockito:mockito-core")
  ```

  (Neither module depends back on `platform-workflow-execution-api`; check with
  `grep -n "platform-workflow-execution-api" server/libs/atlas/atlas-file-storage/atlas-file-storage-api/build.gradle.kts server/libs/platform/platform-component/platform-component-api/build.gradle.kts`
  — expected: no output.)

- [ ] **Step 2: Write the failing test** `WorkflowCallResultsTest.java` (Apache 2.0 header, package
  `com.bytechef.platform.workflow.execution`):

  ```java
  package com.bytechef.platform.workflow.execution;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.mockito.Mockito.mock;
  import static org.mockito.Mockito.when;

  import com.bytechef.atlas.execution.domain.Job;
  import com.bytechef.atlas.execution.domain.TaskExecution;
  import com.bytechef.atlas.execution.service.TaskExecutionService;
  import com.bytechef.atlas.file.storage.TaskFileStorage;
  import com.bytechef.component.definition.ActionDefinition;
  import com.bytechef.file.storage.domain.FileEntry;
  import com.bytechef.platform.component.constant.MetadataConstants;
  import com.bytechef.test.extension.ObjectMapperSetupExtension;
  import java.util.Map;
  import java.util.Optional;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.api.extension.ExtendWith;

  @ExtendWith(ObjectMapperSetupExtension.class)
  class WorkflowCallResultsTest {

      private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
      private final TaskFileStorage taskFileStorage = mock(TaskFileStorage.class);

      @Test
      void testReadResultPrefersTheCallableResponse() {
          Job job = job(7L, Job.Status.COMPLETED);
          FileEntry outputEntry = mock(FileEntry.class);
          TaskExecution lastTaskExecution = mock(TaskExecution.class);

          when(lastTaskExecution.getMetadata()).thenReturn(Map.of(MetadataConstants.CALLABLE_RESPONSE, true));
          when(lastTaskExecution.getOutput()).thenReturn(outputEntry);
          when(taskExecutionService.fetchLastJobTaskExecution(7L)).thenReturn(Optional.of(lastTaskExecution));
          when(taskFileStorage.readTaskExecutionOutput(outputEntry))
              .thenReturn(new ActionDefinition.CallableResponse(Map.of("approved", true)));

          assertThat(WorkflowCallResults.readResult(job, taskExecutionService, taskFileStorage))
              .isEqualTo(Map.of("approved", true));
      }

      @Test
      void testReadResultFallsBackToTheJobOutputs() {
          Job job = job(7L, Job.Status.COMPLETED);
          FileEntry jobOutputs = mock(FileEntry.class);

          job.setOutputs(jobOutputs);

          when(taskExecutionService.fetchLastJobTaskExecution(7L)).thenReturn(Optional.empty());
          when(taskFileStorage.readJobOutputs(jobOutputs)).thenReturn(Map.of("total", 3));

          assertThat(WorkflowCallResults.readResult(job, taskExecutionService, taskFileStorage))
              .isEqualTo(Map.of("total", 3));
      }

      @Test
      void testReadResultWithoutOutputsIsNull() {
          Job job = job(7L, Job.Status.COMPLETED);

          when(taskExecutionService.fetchLastJobTaskExecution(7L)).thenReturn(Optional.empty());

          assertThat(WorkflowCallResults.readResult(job, taskExecutionService, taskFileStorage)).isNull();
      }

      @Test
      void testStoppedJobWithAResumeIdIsPendingApproval() {
          Job job = job(7L, Job.Status.STOPPED);

          job.setMetadata(Map.of(MetadataConstants.JOB_RESUME_ID, "resume-1"));

          assertThat(WorkflowCallResults.isPendingApproval(job)).isTrue();
          assertThat(WorkflowCallResults.fetchApprovalFormUrl(job, "https://bc.example", null))
              .contains("https://bc.example/resume/resume-1");
          assertThat(WorkflowCallResults.fetchResumeToken(job, null)).contains("resume-1");
      }

      @Test
      void testStoppedJobWithoutAResumeIdIsNotPendingApproval() {
          Job job = job(7L, Job.Status.STOPPED);

          assertThat(WorkflowCallResults.isPendingApproval(job)).isFalse();
          assertThat(WorkflowCallResults.fetchApprovalFormUrl(job, "https://bc.example", null)).isEmpty();
      }

      @Test
      void testCompletedJobIsNotPendingApprovalEvenWithAResumeId() {
          Job job = job(7L, Job.Status.COMPLETED);

          job.setMetadata(Map.of(MetadataConstants.JOB_RESUME_ID, "resume-1"));

          assertThat(WorkflowCallResults.isPendingApproval(job)).isFalse();
      }

      private static Job job(long id, Job.Status status) {
          Job job = new Job();

          job.setId(id);
          job.setStatus(status);

          return job;
      }
  }
  ```

  `Job` has `setId(Long)`, `setStatus(Status)`, `setOutputs(FileEntry)` and `setMetadata(Map<String, ?>)`
  (`Job.java:307-345`); `getMetadata()` returns a wildcard map, which is why the test uses the setter.
  `FileEntry` is `com.bytechef.file.storage.domain.FileEntry`. `ConvertUtils` needs the static mapper the extension
  installs; `test-support` is already a test dependency of this module.

- [ ] **Step 3: Run it to verify it fails**

  Run: `./gradlew <WFX_API>:test --tests "com.bytechef.platform.workflow.execution.WorkflowCallResultsTest" > /tmp/t1.log 2>&1; echo $?`
  Expected: non-zero; `/tmp/t1.log` contains `cannot find symbol` … `WorkflowCallResults`.

- [ ] **Step 4: Implement** `WorkflowCallResults.java` (Apache 2.0 header copied from `JobExecutionErrors.java` in the
  same package):

  ```java
  package com.bytechef.platform.workflow.execution;

  import com.bytechef.atlas.execution.domain.Job;
  import com.bytechef.atlas.execution.domain.TaskExecution;
  import com.bytechef.atlas.execution.service.TaskExecutionService;
  import com.bytechef.atlas.file.storage.TaskFileStorage;
  import com.bytechef.commons.util.ConvertUtils;
  import com.bytechef.component.definition.ActionDefinition;
  import com.bytechef.platform.component.constant.MetadataConstants;
  import com.bytechef.platform.workflow.execution.token.ApprovalFormUrls;
  import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
  import java.util.Map;
  import java.util.Objects;
  import java.util.Optional;
  import org.jspecify.annotations.Nullable;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;

  /**
   * Reads the outcome of a workflow run started through a {@code workflow/newWorkflowCall} trigger: the
   * {@code responseToWorkflowCall} output when the workflow responded, otherwise the job outputs; and, for a run paused
   * on a human approval, the hosted form URL and the resume token. Shared by every caller that starts such runs on
   * someone's behalf (MCP servers, apps) so they report results and approvals the same way.
   *
   * @author Ivica Cardic
   */
  public final class WorkflowCallResults {

      private static final Logger log = LoggerFactory.getLogger(WorkflowCallResults.class);

      private WorkflowCallResults() {
      }

      public static Optional<Object> fetchCallableResponseOutput(
          Job job, TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

          try {
              return taskExecutionService.fetchLastJobTaskExecution(Objects.requireNonNull(job.getId()))
                  .filter(WorkflowCallResults::isCallableResponse)
                  .map(lastTaskExecution -> {
                      ActionDefinition.CallableResponse callableResponse = ConvertUtils.convertValue(
                          taskFileStorage.readTaskExecutionOutput(lastTaskExecution.getOutput()),
                          ActionDefinition.CallableResponse.class);

                      return callableResponse.output();
                  });
          } catch (Exception exception) {
              log.warn(
                  "Failed to extract callable response output from job {}: {}", job.getId(), exception.getMessage());

              return Optional.empty();
          }
      }

      public static Optional<String> fetchApprovalFormUrl(
          Job job, @Nullable String publicUrl, @Nullable ApprovalTokens approvalTokens) {

          if (!isPendingApproval(job)) {
              return Optional.empty();
          }

          return ApprovalFormUrls.buildFormUrl(publicUrl, getJobResumeId(job), approvalTokens);
      }

      public static Optional<String> fetchResumeToken(Job job, @Nullable ApprovalTokens approvalTokens) {
          if (!isPendingApproval(job)) {
              return Optional.empty();
          }

          return ApprovalFormUrls.buildResumeToken(getJobResumeId(job), approvalTokens);
      }

      /**
       * A {@code STOPPED} run that carries a resume id is paused on a human approval; a {@code STOPPED} run without one
       * was stopped.
       */
      public static boolean isPendingApproval(Job job) {
          return job.getStatus() == Job.Status.STOPPED && getJobResumeId(job) != null;
      }

      public static @Nullable Object readResult(
          Job job, TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

          Optional<Object> callableResponseOutput = fetchCallableResponseOutput(job, taskExecutionService,
              taskFileStorage);

          if (callableResponseOutput.isPresent()) {
              return callableResponseOutput.get();
          }

          if (job.getOutputs() == null) {
              return null;
          }

          return taskFileStorage.readJobOutputs(job.getOutputs());
      }

      private static @Nullable String getJobResumeId(Job job) {
          Object jobResumeId = job.getMetadata(MetadataConstants.JOB_RESUME_ID);

          return jobResumeId == null ? null : jobResumeId.toString();
      }

      private static boolean isCallableResponse(TaskExecution taskExecution) {
          Map<String, ?> metadata = taskExecution.getMetadata();

          return metadata.containsKey(MetadataConstants.CALLABLE_RESPONSE);
      }
  }
  ```

  Note the order in `readResult`: the callable response is checked before `job.getOutputs()`. MCP checked
  `getOutputs() == null` first and returned null; a workflow that responds through `responseToWorkflowCall` but writes
  no job outputs therefore returned null to MCP clients. Step 6 keeps MCP's own null check, so MCP's behaviour does not
  change in this task.

- [ ] **Step 5: Run the test to verify it passes**

  Run: `./gradlew <WFX_API>:test --tests "com.bytechef.platform.workflow.execution.WorkflowCallResultsTest" > /tmp/t1.log 2>&1; echo $?`
  Expected: `0`.

- [ ] **Step 6: Make MCP delegate.** In `AutomationMcpToolFacade.java`:
  - add `import com.bytechef.platform.workflow.execution.WorkflowCallResults;`
  - replace the body of `getCallableResponseOutput(Job job)` with
    `return WorkflowCallResults.fetchCallableResponseOutput(job, taskExecutionService, taskFileStorage);`
  - in `describePendingApproval(Job job)`, replace the local `formUrl` computation with
    ```java
    ApprovalTokens approvalTokens = approvalTokensObjectProvider.getIfAvailable();

    String formUrl = WorkflowCallResults.fetchApprovalFormUrl(job, publicUrl, approvalTokens)
        .orElse(null);
    ```
    and the `ApprovalFormUrls.buildResumeToken(...)` call with
    `WorkflowCallResults.fetchResumeToken(job, approvalTokens).ifPresent(resumeToken -> result.put("resumeToken", resumeToken));`
    Delete the now-unused local `jobResumeId` / `jobResumeIdString` variables. `describePendingApproval` is only called
    for `STOPPED` jobs, so `isPendingApproval`'s status guard does not change its output.
  - leave `resolvePendingApprovalResumeToken` and `resolvePendingApprovalFormUrl` as they are (they fetch the job by id
    and already guard on status); remove any import that becomes unused.

- [ ] **Step 7: Run MCP's tests**

  Run: `./gradlew <MCP>:compileJava <MCP>:compileTestJava > /tmp/t1m.log 2>&1; echo $?` then
  `./gradlew <MCP>:test --tests "*AutomationMcpToolFacade*" > /tmp/t1m.log 2>&1; echo $?`
  Expected: `0` both times.

- [ ] **Step 8: Commit**

  ```bash
  git add server/libs/platform/platform-workflow/platform-workflow-execution/platform-workflow-execution-api/build.gradle.kts \
    server/libs/platform/platform-workflow/platform-workflow-execution/platform-workflow-execution-api/src/main/java/com/bytechef/platform/workflow/execution/WorkflowCallResults.java \
    server/libs/platform/platform-workflow/platform-workflow-execution/platform-workflow-execution-api/src/test/java/com/bytechef/platform/workflow/execution/WorkflowCallResultsTest.java \
    server/libs/automation/automation-ai/automation-ai-mcp-server/src/main/java/com/bytechef/automation/ai/mcp/server/facade/AutomationMcpToolFacade.java
  git commit -m "- Share workflow-call result and approval reading between MCP and other callers"
  ```

---

### Task 2: Error types, audit events, the tool-execution surface, settings

**Files:**
- Modify: `automation-app-api/.../exception/AppErrorType.java`, `automation-app-api/src/test/.../exception/AppErrorTypeTest.java`
- Modify: `automation-app-service/.../audit/AppAuditEvent.java`
- Modify: `automation-app-public-rest/.../AbstractAppApiController.java`, `.../AppApiControllerTest.java`
- Modify: `platform-tool-execution-api/.../ToolExecutionSurface.java`, `.../ToolExecutionEnumOrdinalStabilityTest.java`
- Modify: `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java`
- Modify: `server/apps/server-app/src/main/resources/config/application-bytechef.yml`
- Modify (client): `client/src/pages/automation/tool-invocations/components/ToolInvocationsTable.tsx`,
  `client/src/pages/automation/tool-invocations/ToolInvocations.tsx`,
  `client/src/pages/automation/tool-invocations/components/ToolInvocationsTable.test.tsx`

**Interfaces:**
- Produces: `AppErrorType.APP_WORKFLOW_NOT_FOUND` (118, 404), `APP_WORKFLOW_UNAVAILABLE` (119, 409),
  `APP_WORKFLOW_INPUT_INVALID` (120, 400), `APP_WORKFLOW_RUN_NOT_FOUND` (121, 404), `APP_WORKFLOW_NOT_BINDABLE`
  (122, 400); `AppAuditEvent.APP_WORKFLOW_BOUND`, `APP_WORKFLOW_UNBOUND`;
  `ToolExecutionSurface.AUTOMATION_APP_WORKFLOW`; `ApplicationProperties.App.WorkflowRun` (reached through
  `getApp().getWorkflow()`) with `Duration getDefaultWait()`, `Duration getMaxWait()` and `long getRunRetentionDays()`.

- [ ] **Step 1: Extend the failing tests first.**
  - `AppErrorTypeTest.testNameOfNamesEveryConstant`: change `isEqualTo(18)` to `isEqualTo(23)`.
  - `ToolExecutionEnumOrdinalStabilityTest.testSurfaceOrdinals`: append `"AUTOMATION_APP_WORKFLOW"` after
    `"AUTOMATION_APP_PROXY"` in `containsExactly(...)`.
  - `AppApiControllerTest.appErrors()`: add, before the closing `)`:
    ```java
            Arguments.of(AppErrorType.APP_WORKFLOW_NOT_FOUND, 404, "APP_WORKFLOW_NOT_FOUND"),
            Arguments.of(AppErrorType.APP_WORKFLOW_UNAVAILABLE, 409, "APP_WORKFLOW_UNAVAILABLE"),
            Arguments.of(AppErrorType.APP_WORKFLOW_INPUT_INVALID, 400, "APP_WORKFLOW_INPUT_INVALID"),
            Arguments.of(AppErrorType.APP_WORKFLOW_RUN_NOT_FOUND, 404, "APP_WORKFLOW_RUN_NOT_FOUND"),
            Arguments.of(AppErrorType.APP_WORKFLOW_NOT_BINDABLE, 400, "APP_WORKFLOW_NOT_BINDABLE"),
    ```
    (put a comma after the previous last row `APP_FIELD_TOO_LONG`.)

- [ ] **Step 2: Run them to verify they fail**

  Run each with `> /tmp/t2.log 2>&1; echo $?`:
  `./gradlew <APP_API>:test --tests "com.bytechef.ee.automation.app.exception.AppErrorTypeTest"`,
  `./gradlew <TOOL_API>:test --tests "*ToolExecutionEnumOrdinalStabilityTest"`.
  Expected: non-zero (count 18 ≠ 23; `AUTOMATION_APP_WORKFLOW` missing). The REST test does not compile yet.

- [ ] **Step 3: Implement.**
  - `AppErrorType`: append after `APP_FIELD_TOO_LONG`:
    ```java
    public static final AppErrorType APP_WORKFLOW_NOT_FOUND = new AppErrorType(118);
    public static final AppErrorType APP_WORKFLOW_UNAVAILABLE = new AppErrorType(119);
    public static final AppErrorType APP_WORKFLOW_INPUT_INVALID = new AppErrorType(120);
    public static final AppErrorType APP_WORKFLOW_RUN_NOT_FOUND = new AppErrorType(121);
    public static final AppErrorType APP_WORKFLOW_NOT_BINDABLE = new AppErrorType(122);
    ```
    and append to `NAMES`:
    ```java
        Map.entry(APP_WORKFLOW_NOT_FOUND.getErrorKey(), "APP_WORKFLOW_NOT_FOUND"),
        Map.entry(APP_WORKFLOW_UNAVAILABLE.getErrorKey(), "APP_WORKFLOW_UNAVAILABLE"),
        Map.entry(APP_WORKFLOW_INPUT_INVALID.getErrorKey(), "APP_WORKFLOW_INPUT_INVALID"),
        Map.entry(APP_WORKFLOW_RUN_NOT_FOUND.getErrorKey(), "APP_WORKFLOW_RUN_NOT_FOUND"),
        Map.entry(APP_WORKFLOW_NOT_BINDABLE.getErrorKey(), "APP_WORKFLOW_NOT_BINDABLE"));
    ```
    (the previous last entry gains a trailing comma.)
  - `AbstractAppApiController.toAppStatus`: extend the NOT_FOUND branch condition with
    `|| errorKey == AppErrorType.APP_WORKFLOW_NOT_FOUND.getErrorKey() || errorKey == AppErrorType.APP_WORKFLOW_RUN_NOT_FOUND.getErrorKey()`
    and the CONFLICT branch with `|| errorKey == AppErrorType.APP_WORKFLOW_UNAVAILABLE.getErrorKey()`.
    `APP_WORKFLOW_INPUT_INVALID` and `APP_WORKFLOW_NOT_BINDABLE` fall through to 400.
  - `AppAuditEvent`: append after `APP_OPERATIONS_CHANGED` (it gains a trailing comma):
    ```java
        /**
         * A workflow was bound to an app. Payload: {@code workflowKey}, {@code projectDeploymentId},
         * {@code workflowUuid}; {@code previousProjectDeploymentId} and {@code previousWorkflowUuid} when the key
         * pointed at another workflow before.
         */
        APP_WORKFLOW_BOUND,

        /**
         * A workflow binding was removed. Payload: {@code workflowKey}.
         */
        APP_WORKFLOW_UNBOUND
    ```
  - `ToolExecutionSurface`: append `AUTOMATION_APP_WORKFLOW` after `AUTOMATION_APP_PROXY` (which gains a comma).
  - `ApplicationProperties.App`: add a field, getter and setter, and a nested class, following `Execution`. The
    class is named `WorkflowRun`, not `Workflow`: `ApplicationProperties.Workflow` already exists at the top level
    (`bytechef.workflow.*`) and a nested `Workflow` would shadow it inside `App`. The yml key stays `workflow`.
    ```java
        /**
         * Limits of an app running a bound workflow
         */
        private WorkflowRun workflow = new WorkflowRun();

        public WorkflowRun getWorkflow() {
            return workflow;
        }

        public void setWorkflow(WorkflowRun workflow) {
            this.workflow = workflow;
        }

        /**
         * Limits of an app running a bound workflow.
         */
        public static class WorkflowRun {

            /**
             * How long a workflow run requested by an app is waited for when the request names no wait
             */
            private Duration defaultWait;

            /**
             * The longest wait an app may request for a workflow run
             */
            private Duration maxWait;

            /**
             * How many days an app's workflow run records are kept before the retention job deletes them
             */
            private long runRetentionDays;

            public Duration getDefaultWait() {
                return defaultWait;
            }

            public Duration getMaxWait() {
                return maxWait;
            }

            public long getRunRetentionDays() {
                return runRetentionDays;
            }

            public void setDefaultWait(Duration defaultWait) {
                this.defaultWait = defaultWait;
            }

            public void setMaxWait(Duration maxWait) {
                this.maxWait = maxWait;
            }

            public void setRunRetentionDays(long runRetentionDays) {
                this.runRetentionDays = runRetentionDays;
            }
        }
    ```
  - `application-bytechef.yml`, under `bytechef.app:` after the `proxy:` block:
    ```yaml
        workflow:
          # How long a workflow run requested by an app is waited for when the request names no wait.
          default-wait: 30s
          # The longest wait an app may request for a workflow run.
          max-wait: 60s
          # How many days an app's workflow run records are kept; the retention job deletes older ones daily.
          run-retention-days: 90
    ```
  - `ApplicationPropertiesTest`: add a case binding `bytechef.app.workflow.default-wait=30s`, `max-wait=60s` and
    `run-retention-days=90` and asserting the three getters, in the style of the file's existing `app.*` cases
    (`grep -n "app\." server/libs/config/app-config/src/test/java/com/bytechef/config/ApplicationPropertiesTest.java`).
  - Client: in `ToolInvocationsTable.tsx` add `AUTOMATION_APP_WORKFLOW: 'App workflow',` after
    `AUTOMATION_APP_TOOL` (keys stay sorted); in `ToolInvocations.tsx` add
    `{label: 'App workflow', value: 'AUTOMATION_APP_WORKFLOW'},` after the `App proxy` option; in
    `ToolInvocationsTable.test.tsx` add `{...baseLog, appId: 5, id: '6', surface: 'AUTOMATION_APP_WORKFLOW'},` to the
    same list as the other app rows and assert its label the same way the neighbouring rows are asserted.

- [ ] **Step 4: Run the tests to verify they pass**

  `./gradlew <APP_API>:test --tests "com.bytechef.ee.automation.app.exception.AppErrorTypeTest"`,
  `./gradlew <TOOL_API>:test --tests "*ToolExecutionEnumOrdinalStabilityTest"`,
  `./gradlew <APP_REST>:test --tests "com.bytechef.ee.automation.app.public_.web.rest.AppApiControllerTest"`,
  `./gradlew <APP_CONFIG>:test --tests "com.bytechef.config.ApplicationPropertiesTest"`,
  each redirected to a file with `$?` checked. Expected: `0`.
  Client: `cd client && npx vitest run src/pages/automation/tool-invocations` (timeout 600000). Expected: pass.

- [ ] **Step 5: Commit** (two commits, server then client)

  ```bash
  git add server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/exception/AppErrorType.java \
    server/ee/libs/automation/automation-app/automation-app-api/src/test/java/com/bytechef/ee/automation/app/exception/AppErrorTypeTest.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/audit/AppAuditEvent.java \
    server/ee/libs/automation/automation-app/automation-app-public-rest/src/main/java/com/bytechef/ee/automation/app/public_/web/rest/AbstractAppApiController.java \
    server/ee/libs/automation/automation-app/automation-app-public-rest/src/test/java/com/bytechef/ee/automation/app/public_/web/rest/AppApiControllerTest.java \
    server/libs/platform/platform-tool-execution/platform-tool-execution-api/src/main/java/com/bytechef/platform/tool/execution/ToolExecutionSurface.java \
    server/libs/platform/platform-tool-execution/platform-tool-execution-api/src/test/java/com/bytechef/platform/tool/execution/ToolExecutionEnumOrdinalStabilityTest.java \
    server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java \
    server/apps/server-app/src/main/resources/config/application-bytechef.yml
  git commit -m "- Add the workflow binding error types, audit events, invocation surface and wait settings"
  git add client/src/pages/automation/tool-invocations/components/ToolInvocationsTable.tsx \
    client/src/pages/automation/tool-invocations/ToolInvocations.tsx \
    client/src/pages/automation/tool-invocations/components/ToolInvocationsTable.test.tsx
  git commit -m "- client - Label app workflow invocations"
  ```

---

### Task 3: Persistence — `AppWorkflow`, `AppWorkflowRun`, repositories, changelog

**Files:**
- Create: `automation-app-api/src/main/java/com/bytechef/ee/automation/app/domain/AppWorkflow.java`, `AppWorkflowRun.java`
- Create: `automation-app-service/src/main/java/com/bytechef/ee/automation/app/repository/AppWorkflowRepository.java`, `AppWorkflowRunRepository.java`
- Create: `automation-app-service/src/main/resources/config/liquibase/changelog/automation/app/20261007000001_automation_app_workflow.xml`
- Create: `automation-app-service/src/test/java/com/bytechef/ee/automation/app/repository/AppWorkflowRepositoryIntTest.java`, `AppWorkflowRunRepositoryIntTest.java`

**Interfaces:**
- Produces:
  ```java
  // domain (automation-app-api)
  public class AppWorkflow {           // table app_workflow
      Long getAppId(); void setAppId(Long);  Long getId(); void setId(Long);
      Long getProjectDeploymentId(); void setProjectDeploymentId(Long);
      int getVersion(); void setVersion(int);
      String getWorkflowKey(); void setWorkflowKey(String);
      String getWorkflowUuid(); void setWorkflowUuid(String);
  }
  public class AppWorkflowRun {        // table app_workflow_run
      Long getAppId(); void setAppId(Long);  Instant getCreatedDate(); void setCreatedDate(Instant);
      Long getId(); void setId(Long);  Long getJobId(); void setJobId(Long);
      String getRunId(); void setRunId(String);  String getWorkflowKey(); void setWorkflowKey(String);
  }
  // repositories (automation-app-service)
  interface AppWorkflowRepository extends ListCrudRepository<AppWorkflow, Long> {
      List<AppWorkflow> findAllByAppIdOrderByWorkflowKey(long appId);
      Optional<AppWorkflow> findByAppIdAndWorkflowKey(long appId, String workflowKey);
  }
  interface AppWorkflowRunRepository extends ListCrudRepository<AppWorkflowRun, Long> {
      Optional<AppWorkflowRun> findByAppIdAndRunId(long appId, String runId);

      @Modifying
      @Query("DELETE FROM app_workflow_run WHERE created_date < :instant")
      int deleteByCreatedDateBefore(@Param("instant") Instant instant);
  }
  ```
  (`org.springframework.data.jdbc.repository.query.Modifying`, `org.springframework.data.jdbc.repository.query.Query`,
  `org.springframework.data.repository.query.Param`, as `ToolInvocationLogRepository` declares the same method.)

- [ ] **Step 1: Write the changelog** `20261007000001_automation_app_workflow.xml`. The `includeAll` on
  `config/liquibase/changelog/automation/app` in `master.xml` picks it up; do not edit the init changelog.

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <databaseChangeLog
      xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

      <!--
          app_workflow binds a deployed Call-workflow workflow to an app under a workflow key. It references the project
          deployment and the workflow's stable uuid, never a version-specific workflow id, so a redeploy changes what
          the app runs. No foreign key on project_deployment_id: deleting a deployment is not blocked by app bindings,
          the binding becomes unavailable instead.

          app_workflow_run records which app started which job, under a public run id; polling authorizes against it.
      -->
      <changeSet id="20261007000001-1" author="Ivica Cardic">
          <createTable tableName="app_workflow">
              <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                  <constraints primaryKey="true" nullable="false"/>
              </column>
              <column name="app_id" type="BIGINT">
                  <constraints nullable="false"/>
              </column>
              <column name="workflow_key" type="VARCHAR(64)">
                  <constraints nullable="false"/>
              </column>
              <column name="project_deployment_id" type="BIGINT">
                  <constraints nullable="false"/>
              </column>
              <column name="workflow_uuid" type="VARCHAR(36)">
                  <constraints nullable="false"/>
              </column>
              <column name="version" type="INT" defaultValueNumeric="0">
                  <constraints nullable="false"/>
              </column>
          </createTable>

          <addUniqueConstraint tableName="app_workflow" columnNames="app_id, workflow_key"
              constraintName="uk_app_workflow_app_workflow_key"/>

          <addForeignKeyConstraint
              baseTableName="app_workflow"
              baseColumnNames="app_id"
              constraintName="fk_app_workflow_app"
              referencedTableName="app"
              referencedColumnNames="id"
              onDelete="CASCADE"/>

          <createTable tableName="app_workflow_run">
              <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                  <constraints primaryKey="true" nullable="false"/>
              </column>
              <column name="run_id" type="VARCHAR(36)">
                  <constraints nullable="false"/>
              </column>
              <column name="app_id" type="BIGINT">
                  <constraints nullable="false"/>
              </column>
              <column name="workflow_key" type="VARCHAR(64)">
                  <constraints nullable="false"/>
              </column>
              <column name="job_id" type="BIGINT">
                  <constraints nullable="false"/>
              </column>
              <column name="created_date" type="TIMESTAMP">
                  <constraints nullable="false"/>
              </column>
          </createTable>

          <addUniqueConstraint tableName="app_workflow_run" columnNames="run_id"
              constraintName="uk_app_workflow_run_run_id"/>

          <addForeignKeyConstraint
              baseTableName="app_workflow_run"
              baseColumnNames="app_id"
              constraintName="fk_app_workflow_run_app"
              referencedTableName="app"
              referencedColumnNames="id"
              onDelete="CASCADE"/>

          <createIndex tableName="app_workflow_run" indexName="idx_app_workflow_run_app">
              <column name="app_id"/>
          </createIndex>

          <createIndex tableName="app_workflow_run" indexName="idx_app_workflow_run_created_date">
              <column name="created_date"/>
          </createIndex>
      </changeSet>
  </databaseChangeLog>
  ```

- [ ] **Step 2: Write the domain classes** in `automation-app-api`, modelled on `AppConnection.java` (same imports:
  `java.util.Objects`, `org.springframework.data.annotation.Id`, `org.springframework.data.annotation.Version`,
  `org.springframework.data.jdbc.core.mapping.AggregateReference`, `org.springframework.data.relational.core.mapping.Column`,
  `org.springframework.data.relational.core.mapping.Table`). Enterprise header, `@version ee`.

  ```java
  package com.bytechef.ee.automation.app.domain;

  /**
   * A deployed Call-workflow workflow bound to an app under a workflow key. The workflow is identified by its project
   * deployment and its stable uuid; the version-specific workflow id is resolved on every call.
   *
   * @version ee
   *
   * @author Ivica Cardic
   */
  @Table("app_workflow")
  public class AppWorkflow {

      @Column("app_id")
      private AggregateReference<App, Long> appId;

      @Id
      private Long id;

      @Column("project_deployment_id")
      private Long projectDeploymentId;

      @Version
      private int version;

      @Column("workflow_key")
      private String workflowKey;

      @Column("workflow_uuid")
      private String workflowUuid;

      public AppWorkflow() {
      }

      public Long getAppId() {
          return appId.getId();
      }

      public void setAppId(Long appId) {
          this.appId = AggregateReference.to(appId);
      }

      public Long getId() {
          return id;
      }

      public void setId(Long id) {
          this.id = id;
      }

      public Long getProjectDeploymentId() {
          return projectDeploymentId;
      }

      public void setProjectDeploymentId(Long projectDeploymentId) {
          this.projectDeploymentId = projectDeploymentId;
      }

      public int getVersion() {
          return version;
      }

      public void setVersion(int version) {
          this.version = version;
      }

      public String getWorkflowKey() {
          return workflowKey;
      }

      public void setWorkflowKey(String workflowKey) {
          this.workflowKey = workflowKey;
      }

      public String getWorkflowUuid() {
          return workflowUuid;
      }

      public void setWorkflowUuid(String workflowUuid) {
          this.workflowUuid = workflowUuid;
      }

      @Override
      public boolean equals(Object object) {
          if (this == object) {
              return true;
          }

          if (!(object instanceof AppWorkflow that)) {
              return false;
          }

          return Objects.equals(id, that.id);
      }

      @Override
      public int hashCode() {
          return Objects.hashCode(id);
      }
  }
  ```

  `AppWorkflowRun` follows the same shape with fields `appId` (`AggregateReference<App, Long>`, column `app_id`),
  `createdDate` (`Instant`, column `created_date`), `id` (`@Id Long`), `jobId` (`Long`, column `job_id`), `runId`
  (`String`, column `run_id`), `workflowKey` (`String`, column `workflow_key`), getters and setters for each (the
  `appId` pair converts through `AggregateReference` exactly as above), `equals`/`hashCode` on `id`, and the Javadoc
  "Which app started which workflow run, under the public run id the app polls with." It has no `@Version` (rows are
  insert-only).

- [ ] **Step 3: Write the repositories** in `automation-app-service/.../repository/` (same package as
  `AppConnectionRepository`, so `AppJdbcRepositoryConfiguration` already scans them), with the signatures in
  **Interfaces**. Each is `public interface … extends ListCrudRepository<…, Long>` with the Enterprise header and
  `@version ee` Javadoc.

- [ ] **Step 4: Write the failing integration tests.** Model both on `AppConnectionRepositoryIntTest.java` (read it
  first: `@ActiveProfiles("testint")`, `@SpringBootTest(classes = AppIntTestConfiguration.class)`,
  `@Import(PostgreSQLContainerConfiguration.class)`, tenant handling, cleanup). Each test creates an app through
  `AppRepository` (copy that file's helper) and then:

  `AppWorkflowRepositoryIntTest`:
  - `testSaveAndFindByAppIdAndWorkflowKey` — save `{appId, "approve_refund", projectDeploymentId 11, uuid}`;
    `findByAppIdAndWorkflowKey(appId, "approve_refund")` returns it with all four fields.
  - `testFindAllByAppIdIsOrderedByWorkflowKey` — save keys `"b"`, `"a"`; expect `["a", "b"]`.
  - `testDuplicateWorkflowKeyPerAppIsRejected` — second save with the same `(appId, key)` and a new instance throws
    `org.springframework.dao.DataIntegrityViolationException` (assert with `assertThatThrownBy`).
  - `testDeletingTheAppDeletesItsWorkflowBindings` — delete the app through `AppRepository.deleteById`;
    `findAllByAppIdOrderByWorkflowKey(appId)` is empty.

  `AppWorkflowRunRepositoryIntTest`:
  - `testFindByAppIdAndRunIdReturnsTheRun` — save `{appId, runId UUID, "approve_refund", jobId 99, createdDate now}`;
    found by `(appId, runId)`.
  - `testAnotherAppsRunIdIsNotFound` — save a run for app A; `findByAppIdAndRunId(appB, runIdOfA)` is empty.
  - `testDeletingTheAppDeletesItsRuns` — after `AppRepository.deleteById(appId)`, `findAll()` has no row for it.
  - `testDeleteByCreatedDateBeforeKeepsNewerRuns` — save one run with `createdDate` 100 days ago and one from now;
    `deleteByCreatedDateBefore(now minus 90 days)` returns 1 and only the newer run remains.

- [ ] **Step 5: Run them to verify they fail, then pass.** Compile first, as its own invocation (new code, cold build):
  `./gradlew <APP_API>:compileJava <APP_SVC>:compileJava <APP_SVC>:compileTestJava > /tmp/t3.log 2>&1; echo $?` →
  expected `0`. Then
  `./gradlew <APP_SVC>:testIntegration --tests "com.bytechef.ee.automation.app.repository.AppWorkflowRepositoryIntTest" > /tmp/t3.log 2>&1; echo $?`
  and the same for `AppWorkflowRunRepositoryIntTest`. Expected: `0` (Docker must be running). If `testIntegration`
  does not pick the class up, run the module's `check` task with `--tests` instead: the module's IntTests are wired
  into one of the two; confirm with `grep -n "testIntegration\|IntTest" server/ee/libs/automation/automation-app/automation-app-service/build.gradle.kts build-logic -r | head`.

- [ ] **Step 6: Commit**

  ```bash
  git add server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/domain/AppWorkflow.java \
    server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/domain/AppWorkflowRun.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/repository/AppWorkflowRepository.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/repository/AppWorkflowRunRepository.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/main/resources/config/liquibase/changelog/automation/app/20261007000001_automation_app_workflow.xml \
    server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/repository/AppWorkflowRepositoryIntTest.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/repository/AppWorkflowRunRepositoryIntTest.java
  git commit -m "- Add the app workflow binding and workflow run tables"
  ```

---

### Task 4: `AppCallableWorkflowResolver` and `AppWorkflowSchemas`

**Files:**
- Modify: `automation-app-service/build.gradle.kts`
- Create: `automation-app-service/src/main/java/com/bytechef/ee/automation/app/workflow/AppCallableWorkflowResolver.java`
- Create: `automation-app-service/src/main/java/com/bytechef/ee/automation/app/workflow/AppWorkflowSchemas.java`
- Create: `automation-app-service/src/test/java/com/bytechef/ee/automation/app/workflow/AppCallableWorkflowResolverTest.java`, `AppWorkflowSchemasTest.java`
- Modify: `automation-app-service/src/test/java/com/bytechef/ee/automation/app/config/AppIntTestConfiguration.java`

**Interfaces:**
- Consumes: `ProjectDeploymentService.fetchProjectDeployment(long)`,
  `ProjectDeploymentService.getProjectDeployments(Boolean, Environment, Long, Long, Long)`,
  `ProjectService.fetchProject(long)`, `ProjectWorkflowService.fetchProjectWorkflowWorkflowId(long, String)`,
  `ProjectWorkflowService.getProjectWorkflowUuid(long, String)`,
  `ProjectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(long, String)`,
  `ProjectDeploymentWorkflowService.getProjectDeploymentWorkflows(long)`, `WorkflowService.fetchWorkflow(String)`,
  `WorkflowTrigger.of(Workflow)`, `WorkflowNodeType.ofType(String)`, `WorkflowConstants.{WORKFLOW, NEW_WORKFLOW_CALL,
  RESPONSE_TO_WORKFLOW_CALL, INPUT_SCHEMA, OUTPUT_SCHEMA}` (`com.bytechef.platform.component.constant`).
- Produces:
  ```java
  // AppCallableWorkflowResolver (@Component @ConditionalOnEEVersion)
  public record CallableWorkflow(
      String workflowId, String triggerName, String label, @Nullable String description,
      @Nullable String inputSchema, @Nullable String outputSchema, Map<String, ?> deploymentInputs) {}
  public record BindableWorkflow(
      ProjectDeployment projectDeployment, Project project, String workflowUuid, CallableWorkflow callableWorkflow) {}
  public Optional<CallableWorkflow> resolve(App app, long projectDeploymentId, String workflowUuid);
  public List<BindableWorkflow> getBindableWorkflows(App app);
  // AppWorkflowSchemas (final, static)
  public static @Nullable Map<String, Object> parse(@Nullable String jsonSchema);   // throws IllegalArgumentException on unparseable
  public static List<String> validate(@Nullable String inputSchema, Map<String, ?> input); // throws IllegalArgumentException on unparseable schema
  ```

- [ ] **Step 1: Dependencies.** In `automation-app-service/build.gradle.kts` add under `dependencies`:

  ```kotlin
      implementation("tools.jackson.core:jackson-databind")
      implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
      implementation(project(":server:libs:atlas:atlas-execution:atlas-execution-api"))
      implementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))
      implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))
  ```

  `com.networknt.schema` comes transitively through `platform-ai-api` → `spring-ai-client-chat` (already a
  dependency). Confirm before relying on it:
  `./gradlew <APP_SVC>:dependencies --configuration compileClasspath > /tmp/t4d.log 2>&1; echo $?` then
  `grep -n "networknt" /tmp/t4d.log` — expected a `com.networknt:json-schema-validator:3.x` line. If absent, add
  `implementation("com.networknt:json-schema-validator:3.0.6")` instead (the version already in the Gradle cache).
  Commit this dependency edit on its own **before** the first compile (Step 6 commits it with the task; if the cold
  compile is the first thing that runs after the edit, run `./gradlew <APP_SVC>:compileJava` as its own invocation so
  a slow first build is not mistaken for a hang).

- [ ] **Step 2: Write `AppWorkflowSchemasTest` (failing).**

  ```java
  package com.bytechef.ee.automation.app.workflow;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.assertThatThrownBy;

  import java.util.Map;
  import org.junit.jupiter.api.Test;

  /**
   * @version ee
   */
  class AppWorkflowSchemasTest {

      private static final String SCHEMA = """
          {"type": "object",
           "properties": {"orderId": {"type": "string"}, "amount": {"type": "number"}},
           "required": ["orderId"]}
          """;

      @Test
      void testValidInputHasNoErrors() {
          assertThat(AppWorkflowSchemas.validate(SCHEMA, Map.of("orderId", "A-1", "amount", 12.5))).isEmpty();
      }

      @Test
      void testMissingRequiredFieldIsReported() {
          assertThat(AppWorkflowSchemas.validate(SCHEMA, Map.of("amount", 12.5)))
              .singleElement()
              .asString()
              .contains("orderId");
      }

      @Test
      void testWrongTypeIsReported() {
          assertThat(AppWorkflowSchemas.validate(SCHEMA, Map.of("orderId", 7))).isNotEmpty();
      }

      @Test
      void testNoSchemaAcceptsAnyInput() {
          assertThat(AppWorkflowSchemas.validate(null, Map.of("anything", true))).isEmpty();
          assertThat(AppWorkflowSchemas.validate("  ", Map.of("anything", true))).isEmpty();
      }

      @Test
      void testUnparseableSchemaThrows() {
          assertThatThrownBy(() -> AppWorkflowSchemas.validate("{not json", Map.of()))
              .isInstanceOf(IllegalArgumentException.class);
      }

      @Test
      void testParseReturnsTheSchemaAsAMap() {
          assertThat(AppWorkflowSchemas.parse(SCHEMA)).containsEntry("type", "object");
          assertThat(AppWorkflowSchemas.parse(null)).isNull();
      }
  }
  ```

- [ ] **Step 3: Implement `AppWorkflowSchemas`.**

  ```java
  package com.bytechef.ee.automation.app.workflow;

  import com.networknt.schema.Error;
  import com.networknt.schema.Schema;
  import com.networknt.schema.SchemaRegistry;
  import com.networknt.schema.SpecificationVersion;
  import java.util.List;
  import java.util.Map;
  import org.jspecify.annotations.Nullable;
  import tools.jackson.core.JacksonException;
  import tools.jackson.databind.JsonNode;
  import tools.jackson.databind.json.JsonMapper;

  /**
   * Reads and applies the JSON Schema a Call-workflow trigger declares for its input. A blank schema declares nothing
   * and accepts any input; a schema that cannot be read is reported with {@link IllegalArgumentException}.
   *
   * @version ee
   *
   * @author Ivica Cardic
   */
  public final class AppWorkflowSchemas {

      private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
          .build();
      private static final SchemaRegistry SCHEMA_REGISTRY = SchemaRegistry.withDefaultDialect(
          SpecificationVersion.DRAFT_2020_12);

      private AppWorkflowSchemas() {
      }

      @SuppressWarnings("unchecked")
      public static @Nullable Map<String, Object> parse(@Nullable String jsonSchema) {
          if (jsonSchema == null || jsonSchema.isBlank()) {
              return null;
          }

          try {
              return JSON_MAPPER.readValue(jsonSchema, Map.class);
          } catch (JacksonException jacksonException) {
              throw new IllegalArgumentException("The schema is not valid JSON", jacksonException);
          }
      }

      public static List<String> validate(@Nullable String inputSchema, Map<String, ?> input) {
          if (inputSchema == null || inputSchema.isBlank()) {
              return List.of();
          }

          Schema schema;

          try {
              schema = SCHEMA_REGISTRY.getSchema(JSON_MAPPER.readTree(inputSchema));
          } catch (JacksonException jacksonException) {
              throw new IllegalArgumentException("The input schema is not valid JSON", jacksonException);
          }

          JsonNode inputNode = JSON_MAPPER.valueToTree(input);

          return schema.validate(inputNode)
              .stream()
              .map(Error::getMessage)
              .toList();
      }
  }
  ```

  If `SCHEMA_REGISTRY.getSchema(...)` throws a networknt exception (not `JacksonException`) for a structurally
  invalid schema, catch that type too and rethrow as `IllegalArgumentException`; add a test case
  (`{"type": 5}`) only if you add that catch.

- [ ] **Step 4: Write `AppCallableWorkflowResolverTest` (failing)** with Mockito mocks for the six collaborators and a
  real `Workflow`. Build the workflow from JSON so the trigger extension is real:

  ```java
  private static Workflow workflow(String id, @Nullable String inputSchema) {
      String triggerParameters = inputSchema == null
          ? "{}" : "{\"inputSchema\": " + JsonUtils.write(inputSchema) + "}";

      return new Workflow(
          id,
          """
          {"label": "Approve refund", "description": "Asks a manager",
           "triggers": [{"name": "trigger_1", "type": "workflow/v1/newWorkflowCall", "parameters": %s}],
           "tasks": [{"name": "workflow_1", "type": "workflow/v1/responseToWorkflowCall",
                      "parameters": {"outputSchema": "{\\"type\\": \\"object\\"}"}}]}
          """.formatted(triggerParameters),
          Workflow.Format.JSON);
  }
  ```

  Check the `Workflow` constructor first:
  `grep -n "public Workflow(" server/libs/atlas/atlas-configuration/atlas-configuration-api/src/main/java/com/bytechef/atlas/configuration/domain/Workflow.java`
  and how `AutomationMcpToolFacadeWorkflowToolsTest` builds its workflows (it uses the same trigger type literal) —
  copy that construction if it differs. Parsing a `Workflow` from JSON goes through `JsonUtils`, so the class is
  annotated `@ExtendWith({MockitoExtension.class, ObjectMapperSetupExtension.class})` unconditionally
  (`com.bytechef.test.extension.ObjectMapperSetupExtension`, from `test-support`).

  Tests (the app is `workspaceId 7`, `Environment.PRODUCTION`, enabled):
  - `testResolvesTheCurrentWorkflowOfTheDeployment` — deployment 11 (PRODUCTION, enabled, project 3), project 3
    (workspace 7), `fetchProjectWorkflowWorkflowId(11, UUID)` → `"wf-2"`, deployment workflow enabled, workflow
    `"wf-2"` with a schema. Expect a `CallableWorkflow` with `workflowId "wf-2"`, `triggerName "trigger_1"`,
    `label "Approve refund"`, the input schema string, an output schema of `{"type": "object"}` and the deployment
    workflow's inputs.
  - `testDeploymentInAnotherWorkspaceIsUnresolvable` (project workspace 8) → empty.
  - `testDeploymentInAnotherEnvironmentIsUnresolvable` (STAGING) → empty.
  - `testDisabledDeploymentIsUnresolvable` → empty.
  - `testDisabledDeploymentWorkflowIsUnresolvable` → empty.
  - `testMissingDeploymentIsUnresolvable` (`fetchProjectDeployment` empty) → empty.
  - `testWorkflowWithoutACallWorkflowTriggerIsUnresolvable` (trigger type `"webhook/v1/autoRespondWithHTTP200"`) →
    empty.
  - `testMalformedWorkflowUuidIsUnresolvable` — `fetchProjectWorkflowWorkflowId` throws
    `IllegalArgumentException` → empty (the resolver catches it).
  - `testResolveReadsTheCurrentWorkflowIdOnEveryCall` — stub `fetchProjectWorkflowWorkflowId(11, UUID)` to answer
    `"wf-2"` then `"wf-3"` (`thenReturn(Optional.of("wf-2"), Optional.of("wf-3"))`), with deployment workflows and
    workflows for both ids; two `resolve` calls return `workflowId` `"wf-2"` then `"wf-3"` — a redeploy changes what
    runs, nothing is cached.
  - `testBindableWorkflowsListOnlyCallWorkflowWorkflowsOfEnabledDeployments` — two deployments from
    `getProjectDeployments(false, PRODUCTION, null, null, 7L)`, one disabled; the enabled one has two workflows, one
    callable and one webhook-triggered; expect a single `BindableWorkflow` whose `workflowUuid` comes from
    `getProjectWorkflowUuid(11, "wf-2")`.

- [ ] **Step 5: Implement `AppCallableWorkflowResolver`.**

  ```java
  package com.bytechef.ee.automation.app.workflow;

  import com.bytechef.atlas.configuration.domain.Workflow;
  import com.bytechef.atlas.configuration.domain.WorkflowTask;
  import com.bytechef.atlas.configuration.service.WorkflowService;
  import com.bytechef.automation.configuration.domain.Project;
  import com.bytechef.automation.configuration.domain.ProjectDeployment;
  import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
  import com.bytechef.automation.configuration.service.ProjectDeploymentService;
  import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
  import com.bytechef.automation.configuration.service.ProjectService;
  import com.bytechef.automation.configuration.service.ProjectWorkflowService;
  import com.bytechef.commons.util.MapUtils;
  import com.bytechef.ee.automation.app.domain.App;
  import com.bytechef.platform.annotation.ConditionalOnEEVersion;
  import com.bytechef.platform.component.constant.WorkflowConstants;
  import com.bytechef.platform.configuration.domain.WorkflowTrigger;
  import com.bytechef.platform.definition.WorkflowNodeType;
  import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
  import java.util.ArrayList;
  import java.util.List;
  import java.util.Map;
  import java.util.Objects;
  import java.util.Optional;
  import org.jspecify.annotations.Nullable;
  import org.springframework.stereotype.Component;

  /**
   * Resolves a workflow binding to the workflow it runs now. A binding names a project deployment and a workflow uuid;
   * the workflow id is looked up on every call, so a redeploy changes what the app runs. A binding resolves only while
   * the deployment exists, is enabled, belongs to the app's workspace and environment, its deployment workflow exists
   * and is enabled, and the workflow is triggered by {@code workflow/newWorkflowCall}. This is the one definition of
   * a callable workflow: binding, listing and execution all use it.
   *
   * @version ee
   *
   * @author Ivica Cardic
   */
  @Component
  @ConditionalOnEEVersion
  public class AppCallableWorkflowResolver {

      private final ProjectDeploymentService projectDeploymentService;
      private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
      private final ProjectService projectService;
      private final ProjectWorkflowService projectWorkflowService;
      private final WorkflowService workflowService;

      @SuppressFBWarnings("EI")
      public AppCallableWorkflowResolver(
          ProjectDeploymentService projectDeploymentService,
          ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
          ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {

          this.projectDeploymentService = projectDeploymentService;
          this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
          this.projectService = projectService;
          this.projectWorkflowService = projectWorkflowService;
          this.workflowService = workflowService;
      }

      public List<BindableWorkflow> getBindableWorkflows(App app) {
          List<BindableWorkflow> bindableWorkflows = new ArrayList<>();

          List<ProjectDeployment> projectDeployments = projectDeploymentService.getProjectDeployments(
              false, app.getEnvironment(), null, null, Objects.requireNonNull(app.getWorkspaceId(), "workspaceId"));

          for (ProjectDeployment projectDeployment : projectDeployments) {
              if (!projectDeployment.isEnabled()) {
                  continue;
              }

              Optional<Project> project = fetchProjectInWorkspace(app, projectDeployment);

              if (project.isEmpty()) {
                  continue;
              }

              long projectDeploymentId = Objects.requireNonNull(projectDeployment.getId());

              for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflowService
                  .getProjectDeploymentWorkflows(projectDeploymentId)) {

                  if (!projectDeploymentWorkflow.isEnabled()) {
                      continue;
                  }

                  toCallableWorkflow(projectDeploymentWorkflow).ifPresent(callableWorkflow -> bindableWorkflows.add(
                      new BindableWorkflow(
                          projectDeployment, project.get(),
                          projectWorkflowService.getProjectWorkflowUuid(
                              projectDeploymentId, projectDeploymentWorkflow.getWorkflowId()),
                          callableWorkflow)));
              }
          }

          return bindableWorkflows;
      }

      public Optional<CallableWorkflow> resolve(App app, long projectDeploymentId, String workflowUuid) {
          Optional<ProjectDeployment> projectDeployment = projectDeploymentService.fetchProjectDeployment(
              projectDeploymentId);

          if (projectDeployment.isEmpty() || !projectDeployment.get()
              .isEnabled() || projectDeployment.get()
                  .getEnvironmentId() != app.getEnvironmentId()) {

              return Optional.empty();
          }

          if (fetchProjectInWorkspace(app, projectDeployment.get()).isEmpty()) {
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

      private Optional<CallableWorkflow> toCallableWorkflow(ProjectDeploymentWorkflow projectDeploymentWorkflow) {
          String workflowId = projectDeploymentWorkflow.getWorkflowId();

          return workflowService.fetchWorkflow(workflowId)
              .flatMap(workflow -> fetchCallableTrigger(workflow).map(trigger -> new CallableWorkflow(
                  workflowId, trigger.getName(), workflow.getLabel(), workflow.getDescription(),
                  blankToNull(MapUtils.getString(trigger.getParameters(), WorkflowConstants.INPUT_SCHEMA)),
                  fetchOutputSchema(workflow), projectDeploymentWorkflow.getInputs())));
      }

      private static @Nullable String blankToNull(@Nullable String value) {
          return value == null || value.isBlank() ? null : value;
      }

      private static Optional<WorkflowTrigger> fetchCallableTrigger(Workflow workflow) {
          for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
              if (isWorkflowOperation(workflowTrigger.getType(), WorkflowConstants.NEW_WORKFLOW_CALL)) {
                  return Optional.of(workflowTrigger);
              }
          }

          return Optional.empty();
      }

      private static @Nullable String fetchOutputSchema(Workflow workflow) {
          for (WorkflowTask workflowTask : workflow.getTasks(true)) {
              if (isWorkflowOperation(workflowTask.getType(), WorkflowConstants.RESPONSE_TO_WORKFLOW_CALL)) {
                  return blankToNull(MapUtils.getString(workflowTask.getParameters(), WorkflowConstants.OUTPUT_SCHEMA));
              }
          }

          return null;
      }

      private static boolean isWorkflowOperation(String type, String operation) {
          WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

          return Objects.equals(workflowNodeType.name(), WorkflowConstants.WORKFLOW) &&
              Objects.equals(workflowNodeType.operation(), operation);
      }

      /**
       * A project deployment workflow resolved to the workflow it runs, its Call-workflow trigger and its schemas.
       */
      @SuppressFBWarnings("EI")
      public record CallableWorkflow(
          String workflowId, String triggerName, String label, @Nullable String description,
          @Nullable String inputSchema, @Nullable String outputSchema, Map<String, ?> deploymentInputs) {
      }

      @SuppressFBWarnings("EI")
      public record BindableWorkflow(
          ProjectDeployment projectDeployment, Project project, String workflowUuid,
          CallableWorkflow callableWorkflow) {
      }
  }
  ```

  Verify the referenced signatures exist before compiling (they were read from source, but confirm):
  `MapUtils.getString(Map<String, ?>, String)`, `Workflow.getTasks(boolean)`, `WorkflowTask.getParameters()`,
  `ProjectDeployment.getEnvironmentId()` (returns `long`), `App.getEnvironmentId()` (returns `int`),
  `App.getEnvironment()`, `App.getWorkspaceId()`. The first argument of `getProjectDeployments` is `false` on
  purpose: `null` would also list embedded projects' deployments, which an automation app must never be offered.

- [ ] **Step 6: Add the new collaborators to the IntTest context.** `AppIntTestConfiguration` component-scans
  `com.bytechef.ee.automation.app`, so the new `@Component` needs its collaborators there. Add `@Bean` methods
  returning `Mockito.mock(...)` for `ProjectDeploymentService`, `ProjectDeploymentWorkflowService`, `ProjectService`,
  `ProjectWorkflowService` and `WorkflowService` (imports from `com.bytechef.automation.configuration.service` and
  `com.bytechef.atlas.configuration.service`). Add `testImplementation` nothing — the api modules are already on the
  classpath through Step 1.

- [ ] **Step 7: Run the tests**

  `./gradlew <APP_SVC>:compileJava <APP_SVC>:compileTestJava > /tmp/t4.log 2>&1; echo $?` → `0`;
  `./gradlew <APP_SVC>:test --tests "com.bytechef.ee.automation.app.workflow.AppWorkflowSchemasTest" > /tmp/t4.log 2>&1; echo $?` → `0`;
  same for `AppCallableWorkflowResolverTest` → `0`;
  `./gradlew <APP_SVC>:testIntegration --tests "com.bytechef.ee.automation.app.repository.AppWorkflowRepositoryIntTest" > /tmp/t4.log 2>&1; echo $?` → `0`
  (proves the context still starts with the new component).

- [ ] **Step 8: Commit**

  ```bash
  git add server/ee/libs/automation/automation-app/automation-app-service/build.gradle.kts \
    server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/workflow/AppCallableWorkflowResolver.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/workflow/AppWorkflowSchemas.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/workflow/AppCallableWorkflowResolverTest.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/workflow/AppWorkflowSchemasTest.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/config/AppIntTestConfiguration.java
  git commit -m "- Resolve an app's workflow binding to the Call-workflow workflow it runs now"
  ```

---

### Task 5: `AppWorkflowService`

**Files:**
- Modify: `automation-app-api/.../util/AppBindingKeys.java` (+ `AppBindingKeysTest.java`)
- Modify: `automation-app-service/.../service/AppConnectionServiceImpl.java`
- Create: `automation-app-api/.../service/AppWorkflowService.java`
- Create: `automation-app-service/.../service/AppWorkflowServiceImpl.java`
- Create: `automation-app-service/src/test/.../service/AppWorkflowServiceTest.java`

**Interfaces:**
- Consumes: `AppWorkflowRepository`, `AppWorkflowRunRepository` (Task 3), `AppCallableWorkflowResolver.resolve`
  (Task 4), `AppService.getApp(long)`, `AppErrorType.APP_BINDING_KEY_INVALID`,
  `APP_BINDING_KEY_ALREADY_EXISTS`, `APP_WORKFLOW_NOT_BINDABLE` (Task 2).
- Produces:
  ```java
  // AppBindingKeys
  public static boolean isValidKey(@Nullable String key);   // ^[a-z][a-z0-9_-]{0,63}$
  // AppWorkflowService (automation-app-api)
  AppWorkflow bind(long appId, String workflowKey, long projectDeploymentId, String workflowUuid);
  AppWorkflowRun createRun(long appId, String workflowKey, long jobId);
  int deleteRunsCreatedBefore(Instant instant);
  Optional<AppWorkflow> fetchBinding(long appId, String workflowKey);
  Optional<AppWorkflowRun> fetchRun(long appId, String runId);
  List<AppWorkflow> getBindings(long appId);
  boolean unbind(long appId, String workflowKey);
  // AppWorkflowRunRetentionJob (@Component @ConditionalOnEEVersion, automation-app-service, package ...app.workflow)
  @Scheduled(cron = "${bytechef.app.workflow.run-retention-cron:0 40 2 * * *}") public void purgeExpiredRuns();
  ```

- [ ] **Step 1: `AppBindingKeys.isValidKey`.** Add to `AppBindingKeysTest`:

  ```java
  @Test
  void testIsValidKey() {
      assertThat(AppBindingKeys.isValidKey("approve_refund")).isTrue();
      assertThat(AppBindingKeys.isValidKey("a-b")).isTrue();
      assertThat(AppBindingKeys.isValidKey("Approve")).isFalse();
      assertThat(AppBindingKeys.isValidKey("1approve")).isFalse();
      assertThat(AppBindingKeys.isValidKey(null)).isFalse();
      assertThat(AppBindingKeys.isValidKey("a".repeat(65))).isFalse();
  }
  ```

  Implement in `AppBindingKeys`:

  ```java
  private static final Pattern KEY_PATTERN = Pattern.compile("^[a-z][a-z0-9_-]{0,63}$");

  /**
   * Whether {@code key} is a valid binding or workflow key: a lowercase letter followed by up to 63 lowercase letters,
   * digits, {@code -} or {@code _}.
   */
  public static boolean isValidKey(@Nullable String key) {
      return key != null && KEY_PATTERN.matcher(key)
          .matches();
  }

  public static String keyPattern() {
      return KEY_PATTERN.pattern();
  }
  ```

  (import `org.jspecify.annotations.Nullable`.) In `AppConnectionServiceImpl`, delete `BINDING_KEY_PATTERN` and
  replace the check with:

  ```java
  if (!AppBindingKeys.isValidKey(bindingKey)) {
      throw new ConfigurationException(
          "Binding key must match " + AppBindingKeys.keyPattern(), AppErrorType.APP_BINDING_KEY_INVALID);
  }
  ```

  Run `./gradlew <APP_API>:test --tests "*AppBindingKeysTest" > /tmp/t5.log 2>&1; echo $?` and
  `./gradlew <APP_SVC>:test --tests "com.bytechef.ee.automation.app.service.AppConnectionServiceTest" > /tmp/t5.log 2>&1; echo $?` → `0`.

- [ ] **Step 2: Write `AppWorkflowServiceTest` (failing).** `@ExtendWith(MockitoExtension.class)`, mocks
  `AppWorkflowRepository`, `AppWorkflowRunRepository`, `AppCallableWorkflowResolver`, `AppService`; the service built
  by its constructor. The app: id 1, workspace 7, PRODUCTION. A valid uuid constant
  `UUID = "0b0f3a3e-0000-4000-8000-000000000001"`. `CallableWorkflow` built with
  `new CallableWorkflow("wf-2", "trigger_1", "Approve refund", null, null, null, Map.of())`.

  ```java
  @Test
  void testBindCreatesTheBinding() {
      when(appResolve(11L, UUID)).thenReturn(Optional.of(callableWorkflow));
      when(appWorkflowRepository.findAllByAppIdOrderByWorkflowKey(1L)).thenReturn(List.of());
      when(appWorkflowRepository.findByAppIdAndWorkflowKey(1L, "approve_refund")).thenReturn(Optional.empty());
      when(appWorkflowRepository.save(any(AppWorkflow.class))).thenAnswer(invocation -> invocation.getArgument(0));

      AppWorkflow binding = appWorkflowService.bind(1L, "approve_refund", 11L, UUID);

      assertThat(binding.getAppId()).isEqualTo(1L);
      assertThat(binding.getProjectDeploymentId()).isEqualTo(11L);
      assertThat(binding.getWorkflowUuid()).isEqualTo(UUID);
  }
  ```

  (`appResolve(...)` stands for `appCallableWorkflowResolver.resolve(app, 11L, UUID)` with `app` the stubbed
  `appService.getApp(1L)` result — write it inline.) Further tests:
  - `testBindReplacesAnExistingBindingInPlace` — `findByAppIdAndWorkflowKey` returns an existing row with id 5;
    the saved row keeps id 5 and gets the new deployment and uuid.
  - `testBindRejectsAnInvalidKey` — key `"Approve"` → `ConfigurationException` with
    `APP_BINDING_KEY_INVALID`; repository never saved.
  - `testBindRejectsAKeyWhoseFlatFormCollides` — existing binding `"approve-refund"`; binding `"approve_refund"` →
    `APP_BINDING_KEY_ALREADY_EXISTS`.
  - `testBindRejectsAMalformedWorkflowUuid` — uuid `"nope"` → `APP_WORKFLOW_NOT_BINDABLE`; the resolver is never
    called.
  - `testBindRejectsAWorkflowThatIsNotCallable` — resolver returns empty → `APP_WORKFLOW_NOT_BINDABLE`.
  - `testUnbindReportsWhetherABindingWasDeleted` — present → `true` and `delete(row)` verified; absent → `false`.
  - `testCreateRunAssignsAFreshRunId` — `save` echoes its argument; the returned run has a 36-character `runId` that
    parses as a UUID, `appId 1`, the key, `jobId 99` and a non-null `createdDate`; two calls give different run ids.
  - `testFetchRunIsScopedToTheApp` — delegates to `findByAppIdAndRunId(1L, "r")`.
  - `testDeleteRunsCreatedBeforeDelegates` — returns what `deleteByCreatedDateBefore(instant)` returns.

  Use the `assertConfigurationError` helper pattern from `AppExecutionFacadeTest` (copy it).

  Also write `AppWorkflowRunRetentionJobTest` (`automation-app-service/src/test/.../workflow/`), modelled on
  `server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-service/src/test/java/com/bytechef/ee/platform/tool/invocation/log/ToolInvocationLogRetentionJobTest.java`
  (read it first): with `runRetentionDays` 90, `purgeExpiredRuns()` calls `deleteRunsCreatedBefore` with an instant
  between 90 days ago minus 5 s and 90 days ago (capture the argument with `ArgumentCaptor<Instant>`).

- [ ] **Step 3: Implement.** `AppWorkflowService` interface in `automation-app-api/.../service/` (signatures in
  **Interfaces**, each with a one-line Javadoc: `bind` "Creates the binding, or replaces the one under the same key
  in place. Rejects keys that break the key rule or collide after normalisation, and workflows that are not callable
  by the app."; `unbind` "@return {@code true} when a binding was deleted"). Then:

  ```java
  package com.bytechef.ee.automation.app.service;

  @Service
  @Transactional
  @ConditionalOnEEVersion
  public class AppWorkflowServiceImpl implements AppWorkflowService {

      private final AppCallableWorkflowResolver appCallableWorkflowResolver;
      private final AppService appService;
      private final AppWorkflowRepository appWorkflowRepository;
      private final AppWorkflowRunRepository appWorkflowRunRepository;

      @SuppressFBWarnings("EI")
      public AppWorkflowServiceImpl(
          AppCallableWorkflowResolver appCallableWorkflowResolver, AppService appService,
          AppWorkflowRepository appWorkflowRepository, AppWorkflowRunRepository appWorkflowRunRepository) {

          this.appCallableWorkflowResolver = appCallableWorkflowResolver;
          this.appService = appService;
          this.appWorkflowRepository = appWorkflowRepository;
          this.appWorkflowRunRepository = appWorkflowRunRepository;
      }

      @Override
      public AppWorkflow bind(long appId, String workflowKey, long projectDeploymentId, String workflowUuid) {
          if (!AppBindingKeys.isValidKey(workflowKey)) {
              throw new ConfigurationException(
                  "Workflow key must match " + AppBindingKeys.keyPattern(), AppErrorType.APP_BINDING_KEY_INVALID);
          }

          checkFlatKeyUnique(appId, workflowKey);

          if (!isUuid(workflowUuid)) {
              throw new ConfigurationException(
                  "Workflow uuid '%s' is not a uuid".formatted(workflowUuid), AppErrorType.APP_WORKFLOW_NOT_BINDABLE);
          }

          App app = appService.getApp(appId);

          if (appCallableWorkflowResolver.resolve(app, projectDeploymentId, workflowUuid)
              .isEmpty()) {

              throw new ConfigurationException(
                  ("Workflow %s of project deployment %s cannot be bound to app %s: it must be an enabled Call-workflow " +
                      "workflow of an enabled deployment in the app's workspace and environment")
                          .formatted(workflowUuid, projectDeploymentId, appId),
                  AppErrorType.APP_WORKFLOW_NOT_BINDABLE);
          }

          AppWorkflow appWorkflow = appWorkflowRepository.findByAppIdAndWorkflowKey(appId, workflowKey)
              .orElseGet(AppWorkflow::new);

          appWorkflow.setAppId(appId);
          appWorkflow.setProjectDeploymentId(projectDeploymentId);
          appWorkflow.setWorkflowKey(workflowKey);
          appWorkflow.setWorkflowUuid(workflowUuid);

          return appWorkflowRepository.save(appWorkflow);
      }

      @Override
      public AppWorkflowRun createRun(long appId, String workflowKey, long jobId) {
          AppWorkflowRun appWorkflowRun = new AppWorkflowRun();

          appWorkflowRun.setAppId(appId);
          appWorkflowRun.setCreatedDate(Instant.now());
          appWorkflowRun.setJobId(jobId);
          appWorkflowRun.setRunId(UUID.randomUUID()
              .toString());
          appWorkflowRun.setWorkflowKey(workflowKey);

          return appWorkflowRunRepository.save(appWorkflowRun);
      }

      @Override
      public int deleteRunsCreatedBefore(Instant instant) {
          return appWorkflowRunRepository.deleteByCreatedDateBefore(instant);
      }

      @Override
      @Transactional(readOnly = true)
      public Optional<AppWorkflow> fetchBinding(long appId, String workflowKey) {
          return appWorkflowRepository.findByAppIdAndWorkflowKey(appId, workflowKey);
      }

      @Override
      @Transactional(readOnly = true)
      public Optional<AppWorkflowRun> fetchRun(long appId, String runId) {
          return appWorkflowRunRepository.findByAppIdAndRunId(appId, runId);
      }

      @Override
      @Transactional(readOnly = true)
      public List<AppWorkflow> getBindings(long appId) {
          return appWorkflowRepository.findAllByAppIdOrderByWorkflowKey(appId);
      }

      @Override
      public boolean unbind(long appId, String workflowKey) {
          Optional<AppWorkflow> binding = appWorkflowRepository.findByAppIdAndWorkflowKey(appId, workflowKey);

          binding.ifPresent(appWorkflowRepository::delete);

          return binding.isPresent();
      }

      private void checkFlatKeyUnique(long appId, String workflowKey) {
          String flatKey = AppBindingKeys.toFlatKey(workflowKey);

          boolean collides = appWorkflowRepository.findAllByAppIdOrderByWorkflowKey(appId)
              .stream()
              .map(AppWorkflow::getWorkflowKey)
              .anyMatch(existingKey -> !existingKey.equals(workflowKey) && AppBindingKeys.toFlatKey(existingKey)
                  .equals(flatKey));

          if (collides) {
              throw new ConfigurationException(
                  "Workflow key '%s' collides with an existing workflow key of the app: both normalize to '%s'"
                      .formatted(workflowKey, flatKey),
                  AppErrorType.APP_BINDING_KEY_ALREADY_EXISTS);
          }
      }

      private static boolean isUuid(String value) {
          try {
              UUID.fromString(value);

              return value.length() == 36;
          } catch (IllegalArgumentException illegalArgumentException) {
              return false;
          }
      }
  }
  ```

  Imports: the domain/repository/resolver/util types above, `com.bytechef.exception.ConfigurationException`,
  `com.bytechef.platform.annotation.ConditionalOnEEVersion`, `edu.umd.cs.findbugs.annotations.SuppressFBWarnings`,
  `java.time.Instant`, `java.util.{List, Optional, UUID}`, `org.springframework.stereotype.Service`,
  `org.springframework.transaction.annotation.Transactional`.

  Then the retention job, `automation-app-service/.../workflow/AppWorkflowRunRetentionJob.java`, the shape of
  `ToolInvocationLogRetentionJob` (same imports: `java.time.Instant`, `java.time.temporal.ChronoUnit`, slf4j,
  `org.springframework.scheduling.annotation.Scheduled`, `org.springframework.stereotype.Component`):

  ```java
  /**
   * Deletes app workflow run records older than {@code bytechef.app.workflow.run-retention-days}. Runs daily at
   * 02:40 local time to limit growth of {@code app_workflow_run}; a purged run polls as not found.
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

      @SuppressFBWarnings("EI")
      public AppWorkflowRunRetentionJob(
          ApplicationProperties applicationProperties, AppWorkflowService appWorkflowService) {

          this.applicationProperties = applicationProperties;
          this.appWorkflowService = appWorkflowService;
      }

      @Scheduled(cron = "${bytechef.app.workflow.run-retention-cron:0 40 2 * * *}")
      public void purgeExpiredRuns() {
          long retentionDays = applicationProperties.getApp()
              .getWorkflow()
              .getRunRetentionDays();

          Instant cutoff = Instant.now()
              .minus(retentionDays, ChronoUnit.DAYS);

          int deleted = appWorkflowService.deleteRunsCreatedBefore(cutoff);

          if (deleted > 0) {
              log.info("App workflow run retention: deleted {} row(s) older than {}", deleted, cutoff);
          } else {
              log.debug("App workflow run retention: no rows older than {}", cutoff);
          }
      }
  }
  ```

  `@Scheduled` needs scheduling enabled in the application; `ToolInvocationLogRetentionJob` relies on the same, so
  nothing to add. The cron default lives in the annotation like its sibling's; the retention days come from yml.

- [ ] **Step 4: Run tests** — `./gradlew <APP_SVC>:test --tests "com.bytechef.ee.automation.app.service.AppWorkflowServiceTest" > /tmp/t5.log 2>&1; echo $?` → `0`,
  and the same for `com.bytechef.ee.automation.app.workflow.AppWorkflowRunRetentionJobTest` → `0`.

- [ ] **Step 5: Commit**

  ```bash
  git add server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/util/AppBindingKeys.java \
    server/ee/libs/automation/automation-app/automation-app-api/src/test/java/com/bytechef/ee/automation/app/util/AppBindingKeysTest.java \
    server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/service/AppWorkflowService.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/service/AppConnectionServiceImpl.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/service/AppWorkflowServiceImpl.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/workflow/AppWorkflowRunRetentionJob.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/service/AppWorkflowServiceTest.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/workflow/AppWorkflowRunRetentionJobTest.java
  git commit -m "- Add the app workflow binding service, run records and their retention"
  ```

---

### Task 6: `AppWorkflowFacade` (gated admin surface) and DTOs

**Files:**
- Create: `automation-app-api/.../dto/AppWorkflowBindingDTO.java`, `AppBindableWorkflowDTO.java`
- Create: `automation-app-api/.../facade/AppWorkflowFacade.java`
- Create: `automation-app-service/.../facade/AppWorkflowFacadeImpl.java`
- Create: `automation-app-service/src/test/.../facade/AppWorkflowFacadeTest.java`

**Interfaces:**
- Consumes: `AppWorkflowService` (Task 5), `AppCallableWorkflowResolver` (Task 4), `AppService.getApp`,
  `AppAuditPublisher.publish(AppAuditEvent, long, Map)`, `ResourceVisibilityResolver.filterVisibleIds(String, long,
  List<VisibilityRecord>)`, `ProjectDeploymentService.fetchProjectDeployment(long)`, `ProjectService.fetchProject(long)`.
- Produces:
  ```java
  public record AppWorkflowBindingDTO(
      long id, long appId, String workflowKey, long projectDeploymentId, String workflowUuid,
      @Nullable String projectDeploymentName, @Nullable String workflowName, boolean available) {}
  public record AppBindableWorkflowDTO(
      long projectDeploymentId, String projectDeploymentName, String projectName, String workflowUuid,
      String workflowName, @Nullable String description) {}
  public interface AppWorkflowFacade {
      List<AppWorkflowBindingDTO> getWorkflowBindings(long appId);                  // APP_VIEW
      List<AppBindableWorkflowDTO> getBindableWorkflows(long appId);                 // APP_MANAGE
      AppWorkflowBindingDTO bindWorkflow(long appId, String workflowKey, long projectDeploymentId, String workflowUuid); // APP_MANAGE
      void unbindWorkflow(long appId, String workflowKey);                          // APP_MANAGE
  }
  ```

- [ ] **Step 1: DTO records** with the components above (Enterprise header, `@version ee`, `@SuppressFBWarnings("EI")`
  not needed — no collections).

- [ ] **Step 2: Write `AppWorkflowFacadeTest` (failing).** Mocks: `AppAuditPublisher`, `AppCallableWorkflowResolver`,
  `AppService`, `AppWorkflowService`, `ProjectDeploymentService`, `ResourceVisibilityResolver`. App 1, workspace 7.
  Tests:
  - `testGetWorkflowBindingsMarksUnresolvableBindingsUnavailable` — two bindings; the resolver resolves the first
    (label `"Approve refund"`) and not the second; deployment 11 named `"Refunds"` exists, deployment 12 is gone.
    Expect `[available true, workflowName "Approve refund", projectDeploymentName "Refunds"]` and
    `[available false, workflowName null, projectDeploymentName null]`.
  - `testGetBindableWorkflowsLeavesOutProjectsTheCallerCannotSee` — the resolver lists workflows of projects 3 and 4;
    `filterVisibleIds("Project", 7L, records)` returns `Set.of(3L)`; expect only project 3's workflow, and assert the
    records passed carry each project's id, visibility and `createdBy`.
  - `testBindWorkflowAuditsANewBinding` — no previous binding; `APP_WORKFLOW_BOUND` published with
    `{workflowKey, projectDeploymentId, workflowUuid}`.
  - `testRebindingToAnotherWorkflowAuditsThePreviousOne` — previous binding pointed at deployment 12 / another uuid;
    payload also carries `previousProjectDeploymentId` and `previousWorkflowUuid`.
  - `testBindWorkflowRefusesAProjectTheCallerCannotSee` — the deployment's project is not in the visible set →
    `ConfigurationException` `APP_WORKFLOW_NOT_BINDABLE`; `appWorkflowService.bind` never called.
  - `testUnbindAuditsOnlyWhenABindingWasDeleted` — `unbind` true → `APP_WORKFLOW_UNBOUND` published; false → nothing.
  - `testEveryFacadeMethodIsGated` — reflect over `AppWorkflowFacadeImpl` public methods declared by
    `AppWorkflowFacade`; each has `@PreAuthorize` whose value is exactly the one listed in **Interfaces**
    (`hasPermission(#appId, 'App', 'APP_VIEW')` for `getWorkflowBindings`, `'APP_MANAGE'` for the rest).

- [ ] **Step 3: Implement `AppWorkflowFacadeImpl`.**

  ```java
  @Service
  @Transactional
  @ConditionalOnEEVersion
  public class AppWorkflowFacadeImpl implements AppWorkflowFacade {

      private static final String PROJECT = "Project";

      private final AppAuditPublisher appAuditPublisher;
      private final AppCallableWorkflowResolver appCallableWorkflowResolver;
      private final AppService appService;
      private final AppWorkflowService appWorkflowService;
      private final ProjectDeploymentService projectDeploymentService;
      private final ProjectService projectService;
      private final ResourceVisibilityResolver resourceVisibilityResolver;

      @SuppressFBWarnings("EI")
      public AppWorkflowFacadeImpl(
          AppAuditPublisher appAuditPublisher, AppCallableWorkflowResolver appCallableWorkflowResolver,
          AppService appService, AppWorkflowService appWorkflowService,
          ProjectDeploymentService projectDeploymentService, ProjectService projectService,
          ResourceVisibilityResolver resourceVisibilityResolver) {

          this.appAuditPublisher = appAuditPublisher;
          this.appCallableWorkflowResolver = appCallableWorkflowResolver;
          this.appService = appService;
          this.appWorkflowService = appWorkflowService;
          this.projectDeploymentService = projectDeploymentService;
          this.projectService = projectService;
          this.resourceVisibilityResolver = resourceVisibilityResolver;
      }

      @Override
      @PreAuthorize("hasPermission(#appId, 'App', 'APP_VIEW')")
      @Transactional(readOnly = true)
      public List<AppWorkflowBindingDTO> getWorkflowBindings(long appId) {
          App app = appService.getApp(appId);

          return appWorkflowService.getBindings(appId)
              .stream()
              .map(binding -> toBindingDTO(app, binding))
              .toList();
      }

      @Override
      @PreAuthorize("hasPermission(#appId, 'App', 'APP_MANAGE')")
      @Transactional(readOnly = true)
      public List<AppBindableWorkflowDTO> getBindableWorkflows(long appId) {
          App app = appService.getApp(appId);

          List<BindableWorkflow> bindableWorkflows = appCallableWorkflowResolver.getBindableWorkflows(app);

          Set<Long> visibleProjectIds = getVisibleProjectIds(
              app, bindableWorkflows.stream()
                  .map(BindableWorkflow::project)
                  .toList());

          return bindableWorkflows.stream()
              .filter(bindableWorkflow -> visibleProjectIds.contains(bindableWorkflow.project()
                  .getId()))
              .map(bindableWorkflow -> new AppBindableWorkflowDTO(
                  Objects.requireNonNull(bindableWorkflow.projectDeployment()
                      .getId()),
                  bindableWorkflow.projectDeployment()
                      .getName(),
                  bindableWorkflow.project()
                      .getName(),
                  bindableWorkflow.workflowUuid(),
                  bindableWorkflow.callableWorkflow()
                      .label(),
                  bindableWorkflow.callableWorkflow()
                      .description()))
              .toList();
      }

      @Override
      @PreAuthorize("hasPermission(#appId, 'App', 'APP_MANAGE')")
      public AppWorkflowBindingDTO bindWorkflow(
          long appId, String workflowKey, long projectDeploymentId, String workflowUuid) {

          App app = appService.getApp(appId);

          checkProjectVisible(app, projectDeploymentId);

          Optional<AppWorkflow> existingBinding = appWorkflowService.fetchBinding(appId, workflowKey);

          AppWorkflow appWorkflow = appWorkflowService.bind(appId, workflowKey, projectDeploymentId, workflowUuid);

          Map<String, Object> data = new HashMap<>();

          data.put("workflowKey", workflowKey);
          data.put("projectDeploymentId", projectDeploymentId);
          data.put("workflowUuid", workflowUuid);

          existingBinding
              .filter(previous -> !Objects.equals(previous.getProjectDeploymentId(), projectDeploymentId) ||
                  !Objects.equals(previous.getWorkflowUuid(), workflowUuid))
              .ifPresent(previous -> {
                  data.put("previousProjectDeploymentId", previous.getProjectDeploymentId());
                  data.put("previousWorkflowUuid", previous.getWorkflowUuid());
              });

          appAuditPublisher.publish(AppAuditEvent.APP_WORKFLOW_BOUND, appId, data);

          return toBindingDTO(app, appWorkflow);
      }

      @Override
      @PreAuthorize("hasPermission(#appId, 'App', 'APP_MANAGE')")
      public void unbindWorkflow(long appId, String workflowKey) {
          if (appWorkflowService.unbind(appId, workflowKey)) {
              appAuditPublisher.publish(AppAuditEvent.APP_WORKFLOW_UNBOUND, appId, Map.of("workflowKey", workflowKey));
          }
      }

      private void checkProjectVisible(App app, long projectDeploymentId) {
          Optional<Project> project = projectDeploymentService.fetchProjectDeployment(projectDeploymentId)
              .flatMap(projectDeployment -> projectService.fetchProject(projectDeployment.getProjectId()));

          if (project.isEmpty() || !getVisibleProjectIds(app, List.of(project.get())).contains(project.get()
              .getId())) {

              throw new ConfigurationException(
                  "Project deployment %s cannot be bound to app %s".formatted(projectDeploymentId, app.getId()),
                  AppErrorType.APP_WORKFLOW_NOT_BINDABLE);
          }
      }

      private Set<Long> getVisibleProjectIds(App app, List<Project> projects) {
          if (projects.isEmpty()) {
              return Set.of();
          }

          return resourceVisibilityResolver.filterVisibleIds(
              PROJECT, Objects.requireNonNull(app.getWorkspaceId(), "workspaceId"),
              projects.stream()
                  .map(project -> new VisibilityRecord(project.getId(), project.getVisibility(), project.getCreatedBy()))
                  .distinct()
                  .toList());
      }

      private AppWorkflowBindingDTO toBindingDTO(App app, AppWorkflow binding) {
          Optional<CallableWorkflow> callableWorkflow = appCallableWorkflowResolver.resolve(
              app, binding.getProjectDeploymentId(), binding.getWorkflowUuid());
          String projectDeploymentName = projectDeploymentService.fetchProjectDeployment(
              binding.getProjectDeploymentId())
              .map(ProjectDeployment::getName)
              .orElse(null);

          return new AppWorkflowBindingDTO(
              Objects.requireNonNull(binding.getId()), binding.getAppId(), binding.getWorkflowKey(),
              binding.getProjectDeploymentId(), binding.getWorkflowUuid(), projectDeploymentName,
              callableWorkflow.map(CallableWorkflow::label)
                  .orElse(null),
              callableWorkflow.isPresent());
      }
  }
  ```

  Replace the `// fields + …` line with the seven `private final` fields and the constructor, written out in the
  style of `AppFacadeImpl`. Imports as in `AppFacadeImpl` plus the new types. Check `VisibilityRecord`'s components
  (`grep -n "record VisibilityRecord" -r server/libs/automation/automation-configuration/automation-configuration-api`)
  and that `Project.getId()` / `getName()` / `getVisibility()` / `getCreatedBy()` exist (they do, per
  `Project.java:155,237`). `PROJECT = "Project"` must be the resource type name the visibility resolver uses for
  projects; confirm with `grep -rn "filterVisibleIds(\"Project\"\|PROJECT_RESOURCE\|\"Project\"" server/libs/automation --include='*.java' | head`.

- [ ] **Step 4: IntTest context.** Nothing to add: `ProjectDeploymentService`/`ProjectService` mocks exist from
  Task 4; `ResourceVisibilityResolver`, `AppAuditPublisher` are already beans.

- [ ] **Step 5: Run** `./gradlew <APP_SVC>:test --tests "com.bytechef.ee.automation.app.facade.AppWorkflowFacadeTest" > /tmp/t6.log 2>&1; echo $?` → `0`.

- [ ] **Step 6: Commit**

  ```bash
  git add server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/dto/AppWorkflowBindingDTO.java \
    server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/dto/AppBindableWorkflowDTO.java \
    server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/facade/AppWorkflowFacade.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/facade/AppWorkflowFacadeImpl.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/facade/AppWorkflowFacadeTest.java
  git commit -m "- Add the gated admin facade for app workflow bindings"
  ```

---

### Task 7: `AppWorkflowExecutionFacade` — run, wait, poll, record

**Files:**
- Create: `automation-app-api/.../dto/AppWorkflowDTO.java`, `AppWorkflowRunDTO.java`, `AppWorkflowRunStatus.java`
- Create: `automation-app-api/.../facade/AppWorkflowExecutionFacade.java`
- Create: `automation-app-service/.../facade/AppWorkflowExecutionFacadeImpl.java`
- Create: `automation-app-service/src/test/.../facade/AppWorkflowExecutionFacadeTest.java`
- Modify: `automation-app-service/src/test/.../config/AppIntTestConfiguration.java`

**Interfaces:**
- Consumes: `AppWorkflowService` (Task 5), `AppCallableWorkflowResolver` (Task 4), `AppWorkflowSchemas` (Task 4),
  `WorkflowCallResults` (Task 1), `ApplicationProperties.App.WorkflowRun` (Task 2), `PrincipalJobFacade.createJob(
  JobParametersDTO, long, PlatformType)`, `JobCompletionAwaiter.await(long, Duration)`, `JobService.{getJob, fetchJob}`,
  `JobExecutionErrors.checkForError(Job, TaskExecutionService)`, `ToolExecutionRecorder.record(ToolExecutionEvent)`,
  `AppPrincipal.getCurrent()`.
- Produces:
  ```java
  public enum AppWorkflowRunStatus { RUNNING, APPROVAL_REQUIRED, COMPLETED, FAILED, CANCELLED }  // not persisted
  public record AppWorkflowDTO(
      String workflowKey, String name, @Nullable String description, @Nullable Map<String, Object> inputSchema,
      @Nullable Map<String, Object> outputSchema, boolean available) {}
  public record AppWorkflowRunDTO(
      String runId, String workflowKey, AppWorkflowRunStatus status, @Nullable Object result,
      @Nullable String errorMessage, @Nullable String approvalFormUrl) {
      public boolean isFinished() { return status == COMPLETED || status == FAILED || status == CANCELLED; }
  }
  public interface AppWorkflowExecutionFacade {
      List<AppWorkflowDTO> getWorkflows();
      AppWorkflowRunDTO executeWorkflow(String workflowKey, Map<String, Object> input, @Nullable Integer waitSeconds);
      AppWorkflowRunDTO getWorkflowRun(String runId);
  }
  ```

- [ ] **Step 1: DTOs and the interface** as above (Enterprise header; `AppWorkflowDTO` and `AppWorkflowRunDTO` carry
  `@SuppressFBWarnings("EI")`). `AppWorkflowExecutionFacade` Javadoc: "Workflow operations of the calling app. Every
  method resolves the app from the request's {@link AppPrincipal} and refuses a disabled app."

- [ ] **Step 2: Write `AppWorkflowExecutionFacadeTest` (failing).** `@ExtendWith(MockitoExtension.class)`,
  `@MockitoSettings(strictness = Strictness.LENIENT)`. Mocks: `AppCallableWorkflowResolver`, `AppService`,
  `AppWorkflowService`, `ApprovalTokens` provider (`ObjectProvider<ApprovalTokens>` whose `getIfAvailable()` returns
  null), `JobCompletionAwaiter`, `JobService`, `PrincipalJobFacade`, `TaskExecutionService`, `TaskFileStorage`,
  `ToolExecutionRecorder`. `ApplicationProperties` real, with `getApp().getWorkflow()` `defaultWait 30s`,
  `maxWait 60s`. Public URL `"https://bc.example"`. Security context:
  `new TestingAuthenticationToken(new AppPrincipal("public", 1L, 7L, Environment.PRODUCTION), null)` in `beforeEach`,
  cleared in `afterEach`. App 1 enabled, workspace 7, PRODUCTION. Binding `approve_refund` → deployment 11, `UUID`.
  Callable workflow:
  `new CallableWorkflow("wf-2", "trigger_1", "Approve refund", "Asks a manager", SCHEMA, null, Map.of("region", "eu"))`
  with `SCHEMA` requiring a string `orderId`. `appWorkflowService.createRun(1L, "approve_refund", 99L)` returns a run
  with `runId "run-1"`, `jobId 99`. Helper `job(Job.Status)` builds a `Job` with id 99.

  Tests:
  - `testCompletedRunReturnsTheResultAndCreatesTheJobLikeMcp` — `createJob` answers 99;
    `jobCompletionAwaiter.await(99L, Duration.ofSeconds(30))` → `CompletableFuture.completedFuture(job(COMPLETED))`
    with job outputs `{"approved": true}` (stub `taskFileStorage.readJobOutputs` and an empty
    `fetchLastJobTaskExecution`). Expect `COMPLETED`, result `{"approved": true}`, `runId "run-1"`. Capture the
    `JobParametersDTO`: workflow id `"wf-2"`, inputs
    `{"region": "eu", "trigger_1": {"orderId": "A-1"}, "__triggerName": "trigger_1"}`; principal id 11;
    `PlatformType.AUTOMATION`. Inside the `createJob` stub, assert the security context's principal is still the
    `AppPrincipal` (no `runAsSystem` wrapper).
  - `testRunStillGoingAfterTheWaitIsRunning` — `await` returns
    `CompletableFuture.failedFuture(new TimeoutException())`; `jobService.getJob(99L)` → `job(STARTED)`. Expect
    `RUNNING`, `isFinished()` false. (`.join()` wraps it in `CompletionException`; the facade unwraps.)
  - `testRunWaitingOnAnApprovalReportsTheFormUrl` — `await` → `job(STOPPED)` with metadata
    `jobResumeId = "resume-1"`. Expect `APPROVAL_REQUIRED`, `approvalFormUrl "https://bc.example/resume/resume-1"`.
  - `testStoppedRunWithoutAResumeIdIsCancelled` → `CANCELLED`.
  - `testFailedRunIsDataNotAnException` — `await` → `job(FAILED)`; stub `taskExecutionService` so that
    `JobExecutionErrors.checkForError` throws (simplest: a `FAILED` job makes it throw on its own — check
    `JobExecutionErrors.checkForError`'s code and stub `fetchLastJobTaskExecution` accordingly). Expect `FAILED` and a
    non-blank `errorMessage`; no exception escapes.
  - `testZeroWaitReadsTheJobWithoutAwaiting` — `waitSeconds 0`; `jobService.getJob(99L)` → `job(CREATED)`; expect
    `RUNNING`; `verifyNoInteractions(jobCompletionAwaiter)`.
  - `testWaitOutOfRangeIsRejected` — `-1` and `61` → `APP_WORKFLOW_INPUT_INVALID`; `createJob` never called.
  - `testInvalidInputIsRejectedBeforeAnyJob` — input `{"amount": 3}` → `APP_WORKFLOW_INPUT_INVALID` whose message
    contains `orderId`; `verifyNoInteractions(principalJobFacade)`.
  - `testUnparseableInputSchemaIsUnavailable` — callable workflow with schema `"{not json"` →
    `APP_WORKFLOW_UNAVAILABLE`.
  - `testUnknownWorkflowKeyIsNotFound` → `APP_WORKFLOW_NOT_FOUND`.
  - `testUnresolvableBindingIsUnavailable` — resolver empty → `APP_WORKFLOW_UNAVAILABLE`.
  - `testDisabledAppIsRefused` — app status DISABLED → `APP_DISABLED` for `executeWorkflow`, `getWorkflowRun` and
    `getWorkflows`.
  - `testExecutionIsRecordedWithTheJobAndOutcome` — capture the `ToolExecutionEvent` passed to
    `toolExecutionRecorder.record(ToolExecutionEvent)`: surface `AUTOMATION_APP_WORKFLOW`, kind `WORKFLOW`, toolName
    and operationName `"approve_refund"`, tenant `"public"`, appId 1, workspaceId 7, environment PRODUCTION ordinal,
    jobId 99, outcome `SUCCESS`; and for the approval case outcome `APPROVAL_REQUIRED`, for the failed case `ERROR`.
  - `testGetRunMapsTheCurrentJob` — `fetchRun(1L, "run-1")` → run; `jobService.fetchJob(99L)` → `job(COMPLETED)` →
    `COMPLETED`.
  - `testAnotherAppsRunIsNotFound` — `fetchRun(1L, "run-x")` empty → `APP_WORKFLOW_RUN_NOT_FOUND`.
  - `testGetRunOfAPurgedJobIsNotFound` — run present, `jobService.fetchJob(99L)` empty →
    `APP_WORKFLOW_RUN_NOT_FOUND`.
  - `testGetWorkflowsListsAvailabilityAndSchemas` — two bindings, one resolvable; expect the first with
    `name "Approve refund"`, `inputSchema` as a map containing `"type" → "object"`, `available true`; the second with
    `name` equal to its key, null schemas, `available false`.

- [ ] **Step 3: Implement `AppWorkflowExecutionFacadeImpl`.**

  ```java
  package com.bytechef.ee.automation.app.facade;

  @Service
  @ConditionalOnEEVersion
  public class AppWorkflowExecutionFacadeImpl implements AppWorkflowExecutionFacade {

      private final AppCallableWorkflowResolver appCallableWorkflowResolver;
      private final ApplicationProperties applicationProperties;
      private final ObjectProvider<ApprovalTokens> approvalTokensObjectProvider;
      private final AppService appService;
      private final AppWorkflowService appWorkflowService;
      private final JobCompletionAwaiter jobCompletionAwaiter;
      private final JobService jobService;
      private final PrincipalJobFacade principalJobFacade;
      private final @Nullable String publicUrl;
      private final TaskExecutionService taskExecutionService;
      private final TaskFileStorage taskFileStorage;
      private final ToolExecutionRecorder toolExecutionRecorder;

      @SuppressFBWarnings("EI")
      public AppWorkflowExecutionFacadeImpl(
          AppCallableWorkflowResolver appCallableWorkflowResolver, ApplicationProperties applicationProperties,
          ObjectProvider<ApprovalTokens> approvalTokensObjectProvider, AppService appService,
          AppWorkflowService appWorkflowService, JobCompletionAwaiter jobCompletionAwaiter, JobService jobService,
          PrincipalJobFacade principalJobFacade, @Value("${bytechef.public-url:#{null}}") @Nullable String publicUrl,
          TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage,
          ToolExecutionRecorder toolExecutionRecorder) {

          this.appCallableWorkflowResolver = appCallableWorkflowResolver;
          this.applicationProperties = applicationProperties;
          this.approvalTokensObjectProvider = approvalTokensObjectProvider;
          this.appService = appService;
          this.appWorkflowService = appWorkflowService;
          this.jobCompletionAwaiter = jobCompletionAwaiter;
          this.jobService = jobService;
          this.principalJobFacade = principalJobFacade;
          this.publicUrl = publicUrl;
          this.taskExecutionService = taskExecutionService;
          this.taskFileStorage = taskFileStorage;
          this.toolExecutionRecorder = toolExecutionRecorder;
      }

      @Override
      public AppWorkflowRunDTO executeWorkflow(
          String workflowKey, Map<String, Object> input, @Nullable Integer waitSeconds) {

          AppPrincipal principal = AppPrincipal.getCurrent();
          App app = getEnabledApp(principal);

          AppWorkflow binding = appWorkflowService.fetchBinding(Objects.requireNonNull(app.getId()), workflowKey)
              .orElseThrow(() -> new ConfigurationException(
                  "No workflow is bound to the app under '%s'".formatted(workflowKey),
                  AppErrorType.APP_WORKFLOW_NOT_FOUND));
          CallableWorkflow callableWorkflow = appCallableWorkflowResolver.resolve(
              app, binding.getProjectDeploymentId(), binding.getWorkflowUuid())
              .orElseThrow(() -> unavailable(workflowKey));

          Duration wait = resolveWait(waitSeconds);

          checkInput(workflowKey, callableWorkflow, input);

          Map<String, Object> inputs = new HashMap<>(callableWorkflow.deploymentInputs());

          inputs.put(callableWorkflow.triggerName(), input);
          inputs.put(JobInputConstants.TRIGGER_NAME_INPUT, callableWorkflow.triggerName());

          long start = System.nanoTime();

          long jobId = principalJobFacade.createJob(
              new JobParametersDTO(callableWorkflow.workflowId(), inputs), binding.getProjectDeploymentId(),
              PlatformType.AUTOMATION);

          AppWorkflowRun appWorkflowRun = appWorkflowService.createRun(app.getId(), workflowKey, jobId);

          AppWorkflowRunDTO appWorkflowRunDTO = toRunDTO(appWorkflowRun, awaitJob(jobId, wait));

          record(principal, app, workflowKey, jobId, appWorkflowRunDTO, Duration.ofNanos(System.nanoTime() - start));

          return appWorkflowRunDTO;
      }

      @Override
      public AppWorkflowRunDTO getWorkflowRun(String runId) {
          App app = getEnabledApp(AppPrincipal.getCurrent());

          AppWorkflowRun appWorkflowRun = appWorkflowService.fetchRun(Objects.requireNonNull(app.getId()), runId)
              .orElseThrow(() -> runNotFound(runId));
          Job job = jobService.fetchJob(appWorkflowRun.getJobId())
              .orElseThrow(() -> runNotFound(runId));

          return toRunDTO(appWorkflowRun, job);
      }

      @Override
      public List<AppWorkflowDTO> getWorkflows() {
          App app = getEnabledApp(AppPrincipal.getCurrent());

          return appWorkflowService.getBindings(Objects.requireNonNull(app.getId()))
              .stream()
              .map(binding -> appCallableWorkflowResolver.resolve(
                  app, binding.getProjectDeploymentId(), binding.getWorkflowUuid())
                  .map(callableWorkflow -> toWorkflowDTO(binding, callableWorkflow))
                  .orElseGet(() -> new AppWorkflowDTO(binding.getWorkflowKey(), binding.getWorkflowKey(), null, null,
                      null, false)))
              .toList();
      }

      private Job awaitJob(long jobId, Duration wait) {
          if (wait.isZero()) {
              return jobService.getJob(jobId);
          }

          try {
              return jobCompletionAwaiter.await(jobId, wait)
                  .join();
          } catch (CompletionException completionException) {
              if (completionException.getCause() instanceof TimeoutException) {
                  return jobService.getJob(jobId);
              }

              throw completionException;
          }
      }

      private void checkInput(String workflowKey, CallableWorkflow callableWorkflow, Map<String, Object> input) {
          List<String> errors;

          try {
              errors = AppWorkflowSchemas.validate(callableWorkflow.inputSchema(), input);
          } catch (IllegalArgumentException illegalArgumentException) {
              throw new ConfigurationException(
                  "The input schema of workflow '%s' cannot be read".formatted(workflowKey),
                  AppErrorType.APP_WORKFLOW_UNAVAILABLE);
          }

          if (!errors.isEmpty()) {
              throw new ConfigurationException(
                  "Invalid input for workflow '%s': %s".formatted(workflowKey, String.join("; ", errors)),
                  AppErrorType.APP_WORKFLOW_INPUT_INVALID);
          }
      }

      private App getEnabledApp(AppPrincipal principal) {
          App app = appService.getApp(principal.appId());

          if (app.getStatus() != AppStatus.ENABLED) {
              throw new ConfigurationException("App is disabled", AppErrorType.APP_DISABLED);
          }

          return app;
      }

      private void record(
          AppPrincipal principal, App app, String workflowKey, long jobId, AppWorkflowRunDTO appWorkflowRunDTO,
          Duration duration) {

          ToolExecutionEvent.Builder builder = ToolExecutionEvent
              .builder(ToolExecutionSurface.AUTOMATION_APP_WORKFLOW, ToolExecutionKind.WORKFLOW, workflowKey)
              .tenantId(principal.tenantId())
              .appId(app.getId())
              .workspaceId(app.getWorkspaceId())
              .environment(app.getEnvironmentId())
              .jobId(jobId)
              .operationName(workflowKey)
              .durationMs(duration.toMillis());

          switch (appWorkflowRunDTO.status()) {
              case FAILED -> builder.outcome(ToolExecutionOutcome.ERROR)
                  .errorMessage(appWorkflowRunDTO.errorMessage());
              case APPROVAL_REQUIRED -> builder.outcome(ToolExecutionOutcome.APPROVAL_REQUIRED);
              default -> builder.outcome(ToolExecutionOutcome.SUCCESS);
          }

          toolExecutionRecorder.record(builder.build());
      }

      private Duration resolveWait(@Nullable Integer waitSeconds) {
          ApplicationProperties.App.WorkflowRun workflow = applicationProperties.getApp()
              .getWorkflow();

          if (waitSeconds == null) {
              return workflow.getDefaultWait();
          }

          Duration maxWait = workflow.getMaxWait();

          if (waitSeconds < 0 || waitSeconds > maxWait.toSeconds()) {
              throw new ConfigurationException(
                  "waitSeconds must be between 0 and %s".formatted(maxWait.toSeconds()),
                  AppErrorType.APP_WORKFLOW_INPUT_INVALID);
          }

          return Duration.ofSeconds(waitSeconds);
      }

      private AppWorkflowRunDTO toRunDTO(AppWorkflowRun appWorkflowRun, Job job) {
          String runId = appWorkflowRun.getRunId();
          String workflowKey = appWorkflowRun.getWorkflowKey();

          return switch (job.getStatus()) {
              case CREATED, STARTED -> new AppWorkflowRunDTO(
                  runId, workflowKey, AppWorkflowRunStatus.RUNNING, null, null, null);
              case COMPLETED -> new AppWorkflowRunDTO(
                  runId, workflowKey, AppWorkflowRunStatus.COMPLETED,
                  WorkflowCallResults.readResult(job, taskExecutionService, taskFileStorage), null, null);
              case FAILED -> new AppWorkflowRunDTO(
                  runId, workflowKey, AppWorkflowRunStatus.FAILED, null, getErrorMessage(job), null);
              case STOPPED -> WorkflowCallResults.isPendingApproval(job)
                  ? new AppWorkflowRunDTO(
                      runId, workflowKey, AppWorkflowRunStatus.APPROVAL_REQUIRED, null, null,
                      WorkflowCallResults.fetchApprovalFormUrl(
                          job, publicUrl, approvalTokensObjectProvider.getIfAvailable())
                          .orElse(null))
                  : new AppWorkflowRunDTO(runId, workflowKey, AppWorkflowRunStatus.CANCELLED, null, null, null);
              case CANCELLED -> new AppWorkflowRunDTO(
                  runId, workflowKey, AppWorkflowRunStatus.CANCELLED, null, null, null);
          };
      }

      private String getErrorMessage(Job job) {
          try {
              JobExecutionErrors.checkForError(job, taskExecutionService);
          } catch (ExecutionException executionException) {
              return executionException.getMessage();
          }

          return "The workflow run failed";
      }

      private static AppWorkflowDTO toWorkflowDTO(AppWorkflow binding, CallableWorkflow callableWorkflow) {
          return new AppWorkflowDTO(
              binding.getWorkflowKey(), callableWorkflow.label(), callableWorkflow.description(),
              parseSchemaOrNull(callableWorkflow.inputSchema()), parseSchemaOrNull(callableWorkflow.outputSchema()),
              true);
      }

      private static @Nullable Map<String, Object> parseSchemaOrNull(@Nullable String schema) {
          try {
              return AppWorkflowSchemas.parse(schema);
          } catch (IllegalArgumentException illegalArgumentException) {
              return null;
          }
      }

      private static ConfigurationException runNotFound(String runId) {
          return new ConfigurationException(
              "No workflow run '%s' of this app".formatted(runId), AppErrorType.APP_WORKFLOW_RUN_NOT_FOUND);
      }

      private static ConfigurationException unavailable(String workflowKey) {
          return new ConfigurationException(
              ("Workflow '%s' is unavailable: its deployment or workflow is gone or disabled, or the workflow is no " +
                  "longer triggered by Call workflow").formatted(workflowKey),
              AppErrorType.APP_WORKFLOW_UNAVAILABLE);
      }
  }
  ```

  Notes for the implementer:
  - `ExecutionException` here is `com.bytechef.exception.ExecutionException` (what `JobExecutionErrors` throws);
    confirm with `grep -n "throw new\|^import" server/libs/platform/platform-workflow/platform-workflow-execution/platform-workflow-execution-api/src/main/java/com/bytechef/platform/workflow/execution/JobExecutionErrors.java`.
  - `ToolExecutionOutcome` is in `com.bytechef.platform.tool.execution`. Confirm the builder has `outcome(...)` and
    `durationMs(long)` and that `record(ToolExecutionEvent)` exists (both per the fact sheet); if the single-event
    `record` requires fields the builder does not default, set them.
  - The `switch` over `Job.Status` must be exhaustive; if the enum gains a value later the compiler will say so.
  - `principal.tenantId()` is used for the event because the request thread already runs in that tenant (the app
    filter set it). Do not add a comment explaining this.
  - `createJob` is called as the app request, not under `SecurityUtils.runAsSystem`: `PrincipalJobFacadeImpl.createJob`
    has no authorization check (MCP and webhooks call it with no user at all), and the job's `createdBy` must not
    claim a system actor.

- [ ] **Step 4: IntTest context.** Add mock `@Bean`s to `AppIntTestConfiguration` for `JobCompletionAwaiter`,
  `JobService`, `PrincipalJobFacade`, `TaskExecutionService` and `TaskFileStorage` (the context has
  `TempFileStorage` but not `TaskFileStorage`). `ObjectProvider<ApprovalTokens>` resolves to empty without a bean;
  nothing to add. Search for other hand-assembled contexts that scan `com.bytechef.ee.automation.app`:
  `grep -rln "com.bytechef.ee.automation.app" --include='*IntTestConfiguration.java' --include='*TestConfiguration.java' server | grep -v automation-app-service`
  — add the same mocks to any hit.

- [ ] **Step 5: Run**
  `./gradlew <APP_SVC>:compileJava <APP_SVC>:compileTestJava > /tmp/t7.log 2>&1; echo $?` → `0`;
  `./gradlew <APP_SVC>:test --tests "com.bytechef.ee.automation.app.facade.AppWorkflowExecutionFacadeTest" > /tmp/t7.log 2>&1; echo $?` → `0`;
  `./gradlew <APP_SVC>:testIntegration --tests "com.bytechef.ee.automation.app.facade.AppExecutionFacadeIntTest" > /tmp/t7.log 2>&1; echo $?` → `0`
  (the existing IntTest proves the context still assembles).

- [ ] **Step 6: Commit**

  ```bash
  git add server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/dto/AppWorkflowDTO.java \
    server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/dto/AppWorkflowRunDTO.java \
    server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/dto/AppWorkflowRunStatus.java \
    server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/facade/AppWorkflowExecutionFacade.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/facade/AppWorkflowExecutionFacadeImpl.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/facade/AppWorkflowExecutionFacadeTest.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/config/AppIntTestConfiguration.java
  git commit -m "- Run bound workflows for apps, wait a bounded time and let apps poll their runs"
  ```

---

### Task 8: App-facing REST — `/workflows` and `/workflow-runs`

**Files:**
- Modify: `automation-app-public-rest/openapi.yaml`; regenerate into `generated/`
- Create: `automation-app-public-rest/.../AppWorkflowApiController.java`
- Create: `automation-app-public-rest/src/test/.../AppWorkflowApiControllerTest.java`
- Modify: `automation-app-public-rest/src/test/.../AppExecutionControllersGlobalHandlerTest.java`

**Interfaces:**
- Consumes: `AppWorkflowExecutionFacade` (Task 7).
- Produces: generated `AppWorkflowApi` (tag `app-workflow`) with `getWorkflows()`, `executeWorkflow(String,
  ExecuteWorkflowRequestModel)`, `getWorkflowRun(String)`; models `AppWorkflowModel`, `ExecuteWorkflowRequestModel`,
  `WorkflowRunModel`, `WorkflowRunStatusModel`, `WorkflowRunErrorModel` in `…public_.web.rest.model`.

- [ ] **Step 1: Extend `openapi.yaml`.** Add the tag:

  ```yaml
    - name: "app-workflow"
      description: "The App Workflow Public API"
  ```

  the paths (after `/connections/{bindingKey}/proxy`):

  ```yaml
    /workflows:
      get:
        description: "Lists the workflows bound to the calling app, with their input and output schemas."
        summary: "Lists the workflows bound to the calling app"
        tags:
          - "app-workflow"
        operationId: "getWorkflows"
        security:
          - bearerAuth: []
        responses:
          "200":
            description: "The workflows."
            content:
              application/json:
                schema:
                  type: "array"
                  items:
                    $ref: "#/components/schemas/AppWorkflow"
          "401":
            $ref: "#/components/responses/unauthorized"
          "403":
            $ref: "#/components/responses/forbidden"
          "500":
            $ref: "#/components/responses/internalServerError"
    /workflows/{workflowKey}:
      post:
        description: "Starts a run of a bound workflow and waits up to waitSeconds for it. A run that finished in time\
          \ answers 200; a run still going or waiting on an approval answers 202 with the runId to poll."
        summary: "Runs a bound workflow"
        tags:
          - "app-workflow"
        operationId: "executeWorkflow"
        security:
          - bearerAuth: []
        parameters:
          - name: "workflowKey"
            description: "The key a workflow is bound to the calling app under."
            in: "path"
            required: true
            schema:
              type: "string"
        requestBody:
          required: true
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ExecuteWorkflowRequest"
        responses:
          "200":
            description: "The run finished: COMPLETED, FAILED or CANCELLED."
            content:
              application/json:
                schema:
                  $ref: "#/components/schemas/WorkflowRun"
          "202":
            description: "The run is RUNNING or waiting on an approval (APPROVAL_REQUIRED)."
            content:
              application/json:
                schema:
                  $ref: "#/components/schemas/WorkflowRun"
          "400":
            $ref: "#/components/responses/badRequest"
          "401":
            $ref: "#/components/responses/unauthorized"
          "403":
            $ref: "#/components/responses/forbidden"
          "404":
            $ref: "#/components/responses/notFound"
          "409":
            $ref: "#/components/responses/conflict"
          "500":
            $ref: "#/components/responses/internalServerError"
    /workflow-runs/{runId}:
      get:
        description: "Returns a workflow run the calling app started."
        summary: "Returns a workflow run"
        tags:
          - "app-workflow"
        operationId: "getWorkflowRun"
        security:
          - bearerAuth: []
        parameters:
          - name: "runId"
            description: "The id of a run the calling app started."
            in: "path"
            required: true
            schema:
              type: "string"
        responses:
          "200":
            description: "The run."
            content:
              application/json:
                schema:
                  $ref: "#/components/schemas/WorkflowRun"
          "401":
            $ref: "#/components/responses/unauthorized"
          "403":
            $ref: "#/components/responses/forbidden"
          "404":
            $ref: "#/components/responses/notFound"
          "500":
            $ref: "#/components/responses/internalServerError"
  ```

  and the schemas (after `ProxyResponse`):

  ```yaml
      AppWorkflow:
        description: "A workflow bound to the calling app."
        type: "object"
        required:
          - "workflowKey"
          - "name"
          - "available"
        properties:
          workflowKey:
            type: "string"
          name:
            type: "string"
          description:
            type: "string"
          inputSchema:
            description: "JSON Schema of the run input, as declared by the workflow's Call workflow trigger."
            type: "object"
            additionalProperties: true
          outputSchema:
            description: "JSON Schema of the result, as declared by the workflow's Respond to workflow call step."
            type: "object"
            additionalProperties: true
          available:
            description: "False when the deployment or workflow is gone or disabled; runs then fail with 409."
            type: "boolean"
      ExecuteWorkflowRequest:
        type: "object"
        required:
          - "input"
        properties:
          input:
            description: "The run input; validated against the workflow's inputSchema."
            type: "object"
            additionalProperties: true
          waitSeconds:
            description: "How long to wait for the run before answering 202, from 0 to the server's maximum; the\
              \ server's default when omitted."
            type: "integer"
      WorkflowRunStatus:
        type: "string"
        enum:
          - "RUNNING"
          - "APPROVAL_REQUIRED"
          - "COMPLETED"
          - "FAILED"
          - "CANCELLED"
      WorkflowRunError:
        type: "object"
        required:
          - "message"
        properties:
          message:
            type: "string"
      WorkflowRun:
        type: "object"
        required:
          - "runId"
          - "workflowKey"
          - "status"
        properties:
          runId:
            type: "string"
          workflowKey:
            type: "string"
          status:
            $ref: "#/components/schemas/WorkflowRunStatus"
          result:
            description: "The run's result on COMPLETED."
          error:
            $ref: "#/components/schemas/WorkflowRunError"
          approvalFormUrl:
            description: "The hosted approval form on APPROVAL_REQUIRED, when the server has a public URL."
            type: "string"
  ```

  (`result` has no `type`: any JSON value. `waitSeconds` has no `minimum` on purpose: the facade owns the range check,
  so a negative value answers `APP_WORKFLOW_INPUT_INVALID` like an over-long one, not a bean-validation
  `INVALID_REQUEST`.)

- [ ] **Step 2: Regenerate.** `./gradlew <APP_REST>:generateOpenAPI > /tmp/t8g.log 2>&1; echo $?` → `0`. Then
  `git status --short server/ee/libs/automation/automation-app/automation-app-public-rest/generated` and revert every
  file whose only change is the generator's timestamp line
  (`git diff --stat` per file; `git checkout -- <file>` for timestamp-only diffs). Keep the new files and
  `.openapi-generator/FILES`. Read the generated `AppWorkflowApi.java` and `WorkflowRunModel.java` to learn the exact
  method names, the `result` field type (`Object` or `JsonNullable<Object>`) and the setter style before Step 3.

- [ ] **Step 3: Write `AppWorkflowApiControllerTest` (failing)**, standalone MockMvc like
  `AppConnectionApiControllerTest` (`mock(AppWorkflowExecutionFacade.class)`):
  - `testExecuteWorkflowThatFinishedIsOk` — facade returns `COMPLETED` with result `{"approved": true}` →
    200, `$.status` `COMPLETED`, `$.result.approved` true, `$.runId`; verify the facade got `input {"orderId":"A-1"}`
    and `waitSeconds 5` from body `{"input":{"orderId":"A-1"},"waitSeconds":5}`.
  - `testExecuteWorkflowStillRunningIsAccepted` — `RUNNING` → 202.
  - `testExecuteWorkflowWaitingOnApprovalIsAccepted` — `APPROVAL_REQUIRED` with form URL → 202 and
    `$.approvalFormUrl`.
  - `testFailedRunCarriesTheErrorMessage` — `FAILED`, message "boom" → 200, `$.error.message` "boom".
  - `testOmittedWaitPassesNull` — body `{"input":{}}` → facade called with `null` wait.
  - `testNegativeWaitReachesTheFacade` — body `{"input":{},"waitSeconds":-1}`; the facade (stubbed to throw
    `APP_WORKFLOW_INPUT_INVALID`) is called with `-1` and the answer is 400 with that code, not `INVALID_REQUEST`.
  - `testMissingInputIsBadRequest` — body `{}` → 400 `INVALID_REQUEST` (bean validation through the generated
    `@Valid`, which `AbstractAppApiController` maps).
  - `testUnavailableWorkflowIsConflict` — facade throws `APP_WORKFLOW_UNAVAILABLE` → 409 with that code.
  - `testGetWorkflowRunReturnsTheRun` and `testUnknownRunIsNotFound` (`APP_WORKFLOW_RUN_NOT_FOUND` → 404).
  - `testGetWorkflowsReturnsSchemasAsObjects` — DTO with `inputSchema {"type":"object"}` → `$[0].inputSchema.type`
    `"object"`, `$[0].available` true.

  Add two cases to `AppExecutionControllersGlobalHandlerTest` (it installs the global handler): an
  `APP_WORKFLOW_RUN_NOT_FOUND` from `getWorkflowRun` answers 404 with that code, and an `APP_WORKFLOW_INPUT_INVALID`
  from `executeWorkflow` answers 400 with that code — proving the controller-local handlers still win.

  In `server/ee/libs/automation/automation-security-web/automation-security-web-impl/src/test/java/com/bytechef/ee/automation/security/web/app/AppApiKeyFilterChainTest.java`
  add a constant `APP_WORKFLOW_RUN_PATH = "/api/app/v1/workflow-runs/run-1"`, add it to the `PingController`'s
  `@GetMapping` paths, and add `testAppKeyIsAcceptedOnTheWorkflowRunPath` (200 with `AppPrincipal`) and
  `testUserKeyIsRejectedOnTheWorkflowRunPath` (401), copying the two neighbouring tests' setup. Run it with
  `./gradlew :server:ee:libs:automation:automation-security-web:automation-security-web-impl:test --tests "*AppApiKeyFilterChainTest" > /tmp/t8f.log 2>&1; echo $?`
  → `0`, and include it in this task's commit.

- [ ] **Step 4: Implement `AppWorkflowApiController`** (Enterprise header; same annotations as
  `AppConnectionApiController`: `@RestController("com.bytechef.ee.automation.app.public_.web.rest.AppWorkflowApiController")`,
  `@RequestMapping("${openapi.openAPIDefinition.base-path.app:/api/app}/v1")`, `@ConditionalOnCoordinator`,
  `@ConditionalOnEEVersion`; extends `AbstractAppApiController implements AppWorkflowApi`):

  ```java
  @Override
  public ResponseEntity<WorkflowRunModel> executeWorkflow(
      String workflowKey, ExecuteWorkflowRequestModel executeWorkflowRequestModel) {

      AppWorkflowRunDTO appWorkflowRunDTO = appWorkflowExecutionFacade.executeWorkflow(
          workflowKey, executeWorkflowRequestModel.getInput(), executeWorkflowRequestModel.getWaitSeconds());

      return ResponseEntity.status(appWorkflowRunDTO.isFinished() ? HttpStatus.OK : HttpStatus.ACCEPTED)
          .body(toModel(appWorkflowRunDTO));
  }

  @Override
  public ResponseEntity<WorkflowRunModel> getWorkflowRun(String runId) {
      return ResponseEntity.ok(toModel(appWorkflowExecutionFacade.getWorkflowRun(runId)));
  }

  @Override
  public ResponseEntity<List<AppWorkflowModel>> getWorkflows() {
      return ResponseEntity.ok(
          appWorkflowExecutionFacade.getWorkflows()
              .stream()
              .map(AppWorkflowApiController::toModel)
              .toList());
  }

  private static AppWorkflowModel toModel(AppWorkflowDTO appWorkflowDTO) {
      AppWorkflowModel appWorkflowModel = new AppWorkflowModel();

      appWorkflowModel.setAvailable(appWorkflowDTO.available());
      appWorkflowModel.setDescription(appWorkflowDTO.description());
      appWorkflowModel.setInputSchema(appWorkflowDTO.inputSchema());
      appWorkflowModel.setName(appWorkflowDTO.name());
      appWorkflowModel.setOutputSchema(appWorkflowDTO.outputSchema());
      appWorkflowModel.setWorkflowKey(appWorkflowDTO.workflowKey());

      return appWorkflowModel;
  }

  private static WorkflowRunModel toModel(AppWorkflowRunDTO appWorkflowRunDTO) {
      WorkflowRunModel workflowRunModel = new WorkflowRunModel();

      workflowRunModel.setApprovalFormUrl(appWorkflowRunDTO.approvalFormUrl());
      workflowRunModel.setResult(appWorkflowRunDTO.result());
      workflowRunModel.setRunId(appWorkflowRunDTO.runId());
      workflowRunModel.setStatus(WorkflowRunStatusModel.fromValue(appWorkflowRunDTO.status()
          .name()));
      workflowRunModel.setWorkflowKey(appWorkflowRunDTO.workflowKey());

      if (appWorkflowRunDTO.errorMessage() != null) {
          WorkflowRunErrorModel workflowRunErrorModel = new WorkflowRunErrorModel();

          workflowRunErrorModel.setMessage(appWorkflowRunDTO.errorMessage());

          workflowRunModel.setError(workflowRunErrorModel);
      }

      return workflowRunModel;
  }
  ```

  Adjust the model calls to what Step 2 showed (e.g. a `JsonNullable` setter, a required-args constructor). The
  `getInput()` type is `Map<String, Object>`; if the generator made it `Object`, cast with
  `@SuppressWarnings("unchecked")`. A null `input` cannot reach the facade (it is `required`; `@Valid` rejects it).

- [ ] **Step 5: Run** `./gradlew <APP_REST>:test --tests "com.bytechef.ee.automation.app.public_.web.rest.AppWorkflowApiControllerTest" > /tmp/t8.log 2>&1; echo $?`
  and the same for `AppExecutionControllersGlobalHandlerTest` → `0`.

- [ ] **Step 6: Commit** — the yaml, the controller, the two tests and every kept generated file
  (`git add` each generated path explicitly, from `git status --short …/generated`).

  ```bash
  git commit -m "- Add the app-facing workflow run and poll endpoints"
  ```

---

### Task 9: Management REST and GraphQL for workflow bindings

**Files:**
- Modify: `automation-app-public-rest/openapi-management.yaml`; regenerate
- Create: `automation-app-public-rest/.../AppWorkflowBindingApiController.java`, test `AppWorkflowBindingApiControllerTest.java`
- Modify: `automation-app-graphql/src/main/resources/graphql/app.graphqls`
- Create: `automation-app-graphql/.../AppWorkflowGraphQlController.java`, test `AppWorkflowGraphQlControllerTest.java`

**Interfaces:**
- Consumes: `AppWorkflowFacade` (Task 6).
- Produces: management `AppWorkflowBindingApi` (tag `app-workflow-binding`, a tag of its own so its default methods do
  not register on `AppApiController` too): `getAppWorkflowBindings(Long id)`, `bindAppWorkflow(Long id, String
  workflowKey, AppWorkflowBindingInputModel)`, `unbindAppWorkflow(Long id, String workflowKey)`. GraphQL queries
  `appWorkflows(appId)`, `appBindableWorkflows(appId)`, mutations `bindAppWorkflow(appId, input)`,
  `unbindAppWorkflow(appId, workflowKey)`.

- [ ] **Step 1: `openapi-management.yaml`.** Add a tag `app-workflow-binding` ("The Automation App Workflow Binding
  Management Public API"), a parameter

  ```yaml
      workflowKey:
        name: "workflowKey"
        description: "The workflow key, matching ^[a-z][a-z0-9_-]{0,63}$."
        in: "path"
        required: true
        schema:
          type: "string"
  ```

  paths `/apps/{id}/workflows` (GET `getAppWorkflowBindings` → array of `AppWorkflowBinding`) and
  `/apps/{id}/workflows/{workflowKey}` (PUT `bindAppWorkflow` with body `AppWorkflowBindingInput` → `AppWorkflowBinding`;
  DELETE `unbindAppWorkflow` → 204), each with the same response list as the connection binding paths and
  `tags: ["app-workflow-binding"]`; and schemas:

  ```yaml
      AppWorkflowBinding:
        description: "A workflow bound to an app under a workflow key."
        type: "object"
        properties:
          id:
            type: "integer"
            format: "int64"
          appId:
            type: "integer"
            format: "int64"
          workflowKey:
            type: "string"
          projectDeploymentId:
            type: "integer"
            format: "int64"
          projectDeploymentName:
            type: "string"
          workflowUuid:
            type: "string"
          workflowName:
            type: "string"
          available:
            type: "boolean"
      AppWorkflowBindingInput:
        description: "The deployed workflow to bind."
        type: "object"
        required:
          - "projectDeploymentId"
          - "workflowUuid"
        properties:
          projectDeploymentId:
            type: "integer"
            format: "int64"
          workflowUuid:
            type: "string"
  ```

  Regenerate (`<APP_REST>:generateOpenAPI`), revert timestamp-only changes, read the generated
  `management/AppWorkflowBindingApi.java`.

- [ ] **Step 2: Failing tests.** `AppWorkflowBindingApiControllerTest` (standalone MockMvc,
  `mock(AppWorkflowFacade.class)`, URLs `/v1/apps/...` because the management base path defaults to empty):
  `testGetAppWorkflowBindings`, `testBindAppWorkflowPassesTheInput` (PUT
  `{"projectDeploymentId":11,"workflowUuid":"…"}` → facade `bindWorkflow(1L, "approve_refund", 11L, "…")`),
  `testBindAppWorkflowWithoutAUuidIsBadRequest` (body `{"projectDeploymentId":11}` → 400),
  `testUnbindAppWorkflowReturnsNoContent`, `testNotBindableIsBadRequest` (`APP_WORKFLOW_NOT_BINDABLE` → 400).

  `AppWorkflowGraphQlControllerTest` (plain unit test like `AppGraphQlControllerTest`): delegation for the four
  operations, `unbindAppWorkflow` returns `true`, and `testControllerOwnsNoAuthorization` (no method carries
  `@PreAuthorize` — copy the reflection loop from `AppGraphQlControllerTest`).

- [ ] **Step 3: Implement.**
  - `AppWorkflowBindingApiController` — `@RestController("com.bytechef.ee.automation.app.public_.web.rest.AppWorkflowBindingApiController")`,
    `@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/v1")`, `@ConditionalOnCoordinator`,
    `@ConditionalOnEEVersion`, `extends AbstractAppApiController implements AppWorkflowBindingApi`; constructor takes
    `AppWorkflowFacade`; methods delegate and map `AppWorkflowBindingDTO` to `AppWorkflowBindingModel` fluently
    (`new AppWorkflowBindingModel().id(...).appId(...)...available(...)`); `unbindAppWorkflow` answers
    `ResponseEntity.noContent().build()`.
  - `app.graphqls`: add to `extend type Query`
    `appWorkflows(appId: ID!): [AppWorkflowBinding!]!` and
    `appBindableWorkflows(appId: ID!): [AppBindableWorkflow!]!`; to `extend type Mutation`
    `bindAppWorkflow(appId: ID!, input: AppWorkflowBindingInput!): AppWorkflowBinding!` and
    `unbindAppWorkflow(appId: ID!, workflowKey: String!): Boolean!`; and the types:
    ```graphql
    type AppWorkflowBinding {
        id: ID!
        appId: ID!
        workflowKey: String!
        projectDeploymentId: ID!
        projectDeploymentName: String
        workflowUuid: String!
        workflowName: String
        available: Boolean!
    }

    type AppBindableWorkflow {
        projectDeploymentId: ID!
        projectDeploymentName: String!
        projectName: String!
        workflowUuid: String!
        workflowName: String!
        description: String
    }

    input AppWorkflowBindingInput {
        workflowKey: String!
        projectDeploymentId: ID!
        workflowUuid: String!
    }
    ```
  - `AppWorkflowGraphQlController` — package-private `@Controller @ConditionalOnEEVersion @ConditionalOnCoordinator
    class`, package-private constructor taking `AppWorkflowFacade`; `@QueryMapping appWorkflows(@Argument long
    appId)`, `@QueryMapping appBindableWorkflows(@Argument long appId)`, `@MutationMapping bindAppWorkflow(@Argument
    long appId, @Argument AppWorkflowBindingInput input)`, `@MutationMapping boolean unbindAppWorkflow(@Argument long
    appId, @Argument String workflowKey)`; nested records `AppWorkflowBindingInput(String workflowKey, long
    projectDeploymentId, String workflowUuid)`, `AppWorkflowBindingResponse(...)` and `AppBindableWorkflowResponse(...)`
    mirroring the DTOs. No `@PreAuthorize`.

- [ ] **Step 4: Run** the two new test classes (one invocation each) → `0`.

- [ ] **Step 5: Commit**

  ```bash
  R=server/ee/libs/automation/automation-app
  git add $R/automation-app-public-rest/openapi-management.yaml \
    $R/automation-app-public-rest/src/main/java/com/bytechef/ee/automation/app/public_/web/rest/AppWorkflowBindingApiController.java \
    $R/automation-app-public-rest/src/test/java/com/bytechef/ee/automation/app/public_/web/rest/AppWorkflowBindingApiControllerTest.java \
    $R/automation-app-graphql/src/main/resources/graphql/app.graphqls \
    $R/automation-app-graphql/src/main/java/com/bytechef/ee/automation/app/web/graphql/AppWorkflowGraphQlController.java \
    $R/automation-app-graphql/src/test/java/com/bytechef/ee/automation/app/web/graphql/AppWorkflowGraphQlControllerTest.java
  git status --short $R/automation-app-public-rest/generated
  git add <each kept generated file listed by the previous command>
  git commit -m "- Add management REST and GraphQL operations for app workflow bindings"
  ```

---

### Task 10: Client — Workflows tab

**Files:**
- Create: `client/src/graphql/automation/app/appWorkflows.graphql`, `appBindableWorkflows.graphql`,
  `bindAppWorkflow.graphql`, `unbindAppWorkflow.graphql`
- Regenerate: `client/src/shared/middleware/graphql.ts`, `graphql-types.ts`
- Create: `client/src/pages/automation/apps/components/AppWorkflowsTab.tsx`, `AppWorkflowBindingDialog.tsx`
- Create: `client/src/pages/automation/apps/tests/AppWorkflowsTab.test.tsx`, `AppWorkflowBindingDialog.test.tsx`
- Modify: `client/src/pages/automation/apps/App.tsx`

**Interfaces:**
- Consumes: GraphQL operations from Task 9.
- Produces: generated hooks `useAppWorkflowsQuery` (key `['appWorkflows', variables]`),
  `useAppBindableWorkflowsQuery`, `useBindAppWorkflowMutation`, `useUnbindAppWorkflowMutation`.

- [ ] **Step 1: Operations.**

  ```graphql
  query appWorkflows($appId: ID!) {
      appWorkflows(appId: $appId) {
          id
          appId
          workflowKey
          projectDeploymentId
          projectDeploymentName
          workflowUuid
          workflowName
          available
      }
  }
  ```

  ```graphql
  query appBindableWorkflows($appId: ID!) {
      appBindableWorkflows(appId: $appId) {
          projectDeploymentId
          projectDeploymentName
          projectName
          workflowUuid
          workflowName
          description
      }
  }
  ```

  ```graphql
  mutation bindAppWorkflow($appId: ID!, $input: AppWorkflowBindingInput!) {
      bindAppWorkflow(appId: $appId, input: $input) {
          id
          appId
          workflowKey
          projectDeploymentId
          projectDeploymentName
          workflowUuid
          workflowName
          available
      }
  }
  ```

  ```graphql
  mutation unbindAppWorkflow($appId: ID!, $workflowKey: String!) {
      unbindAppWorkflow(appId: $appId, workflowKey: $workflowKey)
  }
  ```

  Run `cd client && npx graphql-codegen` (timeout 600000) → exit 0.

- [ ] **Step 2: Failing tests.** Follow `AppBindingsTab.test.tsx` / `AppBindingDialog.test.tsx` exactly (`vi.mock`
  of `@/shared/middleware/graphql` spreading `vi.importActual`, top-level `await import`, `stubMutation`,
  `windowResizeObserver()` and `mockScrollIntoView()` for the dialog).

  `AppWorkflowsTab.test.tsx`:
  - renders a row per binding with key, workflow name and deployment name;
  - an `available: false` binding shows `Unavailable` and no workflow name;
  - with `canManage` false there is no "Bind Workflow" button and no Unbind buttons;
  - clicking Unbind then confirming calls `unbindAppWorkflow.mutate({appId: '3', workflowKey: 'approve_refund'})`.

  `AppWorkflowBindingDialog.test.tsx`:
  - rejects an invalid key (`Approve Refund`) with the same message the connection dialog uses;
  - binds: type `approve_refund`, pick `Approve refund — Refunds (Refunds production)` from the combobox
    (`ariaLabel="Workflow"`), click Bind → `mutate({appId: '3', input: {projectDeploymentId: '11', workflowKey:
    'approve_refund', workflowUuid: 'uuid-1'}})`.

- [ ] **Step 3: Implement.**
  - `AppWorkflowBindingDialog.tsx` — a copy of `AppBindingDialog`'s structure without the operations and proxy fields:
    zod schema `{bindingTarget: z.string().min(1, 'Workflow is required'), workflowKey: z.string().regex(/^[a-z][a-z0-9_-]{0,63}$/, '<same message as AppBindingDialog with "Workflow key" in place of "Binding key">')}`;
    `useAppBindableWorkflowsQuery({appId: app.id}, {enabled: isOpen})`; ComboBox items
    `{label: \`${workflow.workflowName} — ${workflow.projectName} (${workflow.projectDeploymentName})\`, value: \`${workflow.projectDeploymentId}:${workflow.workflowUuid}\`}`
    (sorted keys in the object literal: `label`, `value`); on submit split `bindingTarget` at the first `:` into
    `projectDeploymentId` and `workflowUuid`; `useBindAppWorkflowMutation({onSuccess: () => {queryClient.invalidateQueries({queryKey: ['appWorkflows']}); closeDialog();}})`.
    Labels "Workflow key" (placeholder `approve_refund`) and "Workflow"; header "Bind Workflow"; empty message
    "No Call-workflow workflow is deployed in this app's workspace and environment."
  - `AppWorkflowsTab.tsx` — a copy of `AppBindingsTab`'s structure: intro text "The deployed workflows this app can
    run. Only workflows triggered by Call workflow can be bound."; columns Workflow key / Workflow / Deployment /
    (actions); the Workflow cell shows `workflowName` or, when `!available`, the `Unavailable` marker with title
    "This workflow's deployment or workflow is gone or disabled, so runs through this binding fail."; unbind through
    `useUnbindAppWorkflowMutation` with the same AlertDialog ("Unbind workflow?", "Unbind \"<key>\"? The app can no
    longer run this workflow."), invalidating `['appWorkflows']`; empty state "No workflows are bound to this app
    yet."
  - `App.tsx` — import `WorkflowIcon` from `lucide-react` (add to the sorted destructure) and `AppWorkflowsTab`; add
    after the Connections trigger and content:
    ```tsx
    <TabsTrigger value="workflows">
        <WorkflowIcon className="mr-2 size-4" />
        Workflows
    </TabsTrigger>
    ```
    ```tsx
    <TabsContent className="flex-1" value="workflows">
        <AppWorkflowsTab app={app} canManage={canManage} />
    </TabsContent>
    ```

- [ ] **Step 4: Verify** (each with timeout 600000, from `client/`): `npx vitest run src/pages/automation/apps`,
  `npm run typecheck`, `npx eslint --max-warnings=0 <the touched .ts/.tsx files>`, `npx prettier --check <same>`.
  Expected: all pass.

- [ ] **Step 5: Commit** (two commits: operations + generated first, then the UI)

  ```bash
  git add client/src/graphql/automation/app/appWorkflows.graphql client/src/graphql/automation/app/appBindableWorkflows.graphql \
    client/src/graphql/automation/app/bindAppWorkflow.graphql client/src/graphql/automation/app/unbindAppWorkflow.graphql \
    client/src/shared/middleware/graphql.ts client/src/shared/middleware/graphql-types.ts
  git commit -m "- client - Add the app workflow binding GraphQL operations"
  git add client/src/pages/automation/apps/App.tsx client/src/pages/automation/apps/components/AppWorkflowsTab.tsx \
    client/src/pages/automation/apps/components/AppWorkflowBindingDialog.tsx \
    client/src/pages/automation/apps/tests/AppWorkflowsTab.test.tsx \
    client/src/pages/automation/apps/tests/AppWorkflowBindingDialog.test.tsx
  git commit -m "- client - Add a Workflows tab to bind deployed workflows to an app"
  ```

---

### Task 11: Run ownership against Postgres, docs, full check

**Files:**
- Create: `automation-app-service/src/test/.../facade/AppWorkflowExecutionFacadeIntTest.java`
- Modify: `.agents/apps.md`, `docs/superpowers/specs/2026-10-06-apps-workflows-and-sdk-design.md`

- [ ] **Step 1: `AppWorkflowExecutionFacadeIntTest`** (Postgres via `PostgreSQLContainerConfiguration`, context
  `AppIntTestConfiguration`; model setup and cleanup on `AppExecutionFacadeIntTest`). Two apps A and B in workspace 1,
  PRODUCTION. Stub the mocked `AppCallableWorkflowResolver` collaborators so a binding resolves — simplest: stub the
  five project/workflow service mocks as in `AppCallableWorkflowResolverTest`'s happy path — then bind
  `approve_refund` on A through `AppWorkflowService.bind`. Stub `principalJobFacade.createJob` → 99,
  `jobCompletionAwaiter.await` → a `STARTED` job's failed timeout, `jobService.getJob(99L)` / `fetchJob(99L)` → the
  `STARTED` job. Tests:
  - `testRunIsPollableByTheAppThatStartedIt` — as A, `executeWorkflow` → `RUNNING`; `getWorkflowRun(runId)` as A →
    `RUNNING`.
  - `testAnotherAppCannotPollTheRun` — switch the security context to B's principal; `getWorkflowRun(runIdOfA)` →
    `APP_WORKFLOW_RUN_NOT_FOUND`.
  - `testDeletingTheAppRemovesItsRunsAndBindings` — delete A via `AppRepository`; then
    `jdbcTemplate.queryForObject("SELECT count(*) FROM app_workflow_run", Long.class)` and the same for
    `app_workflow` are 0.

  Run `./gradlew <APP_SVC>:testIntegration --tests "com.bytechef.ee.automation.app.facade.AppWorkflowExecutionFacadeIntTest" > /tmp/t11.log 2>&1; echo $?` → `0`.

- [ ] **Step 2: Docs.**
  - `.agents/apps.md`: a "Workflow bindings" section: what can be bound (Call workflow, same workspace and
    environment, enabled deployment and deployment workflow), `(project_deployment_id, workflow_uuid)` and redeploy
    resolution through `fetchProjectWorkflowWorkflowId`, availability causes (including the disabled deployment and
    unparseable schema from this plan's deviations), the 200/202 contract and `waitSeconds` bounds
    (`bytechef.app.workflow.default-wait`/`max-wait`), run ownership through `app_workflow_run` (404 for another
    app's run) and its retention job, runs created as the project deployment principal with no `runAsSystem`,
    `WorkflowCallResults` shared with MCP, recording under `AUTOMATION_APP_WORKFLOW` with outcome mapping, the
    approval form link being an anonymous capability the app holds, and that deleting a deployment is not blocked.
  - Spec `2026-10-06-apps-workflows-and-sdk-design.md` was synced with this plan before execution; fix only drift that
    implementation introduced (a renamed class, a changed status, a test the plan dropped). If nothing drifted, the
    docs commit touches `.agents/apps.md` alone.

- [ ] **Step 3: Full check of the touched modules** (one invocation per module, each `> /tmp/t11c-<n>.log 2>&1; echo $?`,
  `--continue`): `<WFX_API>:check`, `<MCP>:check`, `<TOOL_API>:check`, `<APP_CONFIG>:check`, `<APP_API>:check`,
  `<APP_SVC>:check`, `<APP_GQL>:check`, `<APP_REST>:check`. Then the whole-repo
  `./gradlew compileJava compileTestJava --continue > /tmp/t11all.log 2>&1; echo $?`. Client:
  `npx vitest run src/pages/automation/apps src/pages/automation/tool-invocations`, `npm run typecheck`. Every exit
  code `0`; `grep "^> Task .* FAILED"` finds nothing. If a module's `check` exceeds the 600 s Bash cap, report it to
  the controller instead of backgrounding it.

- [ ] **Step 4: Commit**

  ```bash
  git add server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/facade/AppWorkflowExecutionFacadeIntTest.java
  git commit -m "- Prove app workflow run ownership and cleanup against Postgres"
  git add .agents/apps.md docs/superpowers/specs/2026-10-06-apps-workflows-and-sdk-design.md
  git commit -m "- docs - Document app workflow bindings and sync the spec with the plan"
  ```
