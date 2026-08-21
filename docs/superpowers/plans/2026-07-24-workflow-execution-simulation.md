# Workflow Execution Simulation (dry-run) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dry-run a workflow through `JobSyncExecutor` so the real Atlas DAG executes (expressions, data flow, dispatchers) while every component action/trigger returns its **declared static output** instead of calling `perform()` — surfacing structural gaps at the exact failing task with zero real API/DB/connection calls.

**Architecture:** A `dryRun` boolean rides in job metadata (`JobParametersDTO.metadata` → `Job.getMetadata()`), is copied onto each `TaskExecution` by the existing platform-layer `TaskDispatcherPreSendProcessor`s, and is read in `AbstractTaskHandler.handle` / `AbstractTriggerHandler.handle`. When set, the handler calls a new facade method that returns the action/trigger's static `OutputResponse.sampleOutput()` (→ `placeholder` → empty) via `ActionDefinitionService.getActionDefinition(...).getOutputResponse()` — never the dynamic output function. A shared `WorkflowSimulationFacade` drives `JobSyncExecutor` in dry-run mode and maps the terminal Job to findings; a `simulateWorkflow` `@Tool` in `platform-ai-tool` exposes it to every building surface. **No `server/libs/atlas/` changes.**

**Tech Stack:** Java 25, Spring, Atlas workflow engine, `platform-job-sync`, Spring AI `@Tool`, JUnit 5 + Mockito + AssertJ.

## Global Constraints

- CE files (`server/libs/…`): Apache 2.0 header; no `@version ee`; no `com.bytechef.ee.*` imports.
- **Never put simulation logic inside `server/libs/atlas/`** — the flag threading uses the existing platform-layer `TaskDispatcherPreSendProcessor` SPI implementations only.
- The dry-run output resolver must use **only** `getActionDefinition(...).getOutputResponse()` (static) — never `executeOutput(...)` / any dynamic output function (those can hit the API).
- `dryRun` defaults **off**; real runs are byte-for-byte unchanged.
- Run `./gradlew spotlessApply` and the module `check` before each commit.

## Scope — first vertical slice (this plan)

In: `dryRun` flag threading; static-output substitution for actions **and** triggers; the simulation facade + `simulateWorkflow` tool; wiring onto the shared workflow-editor build subagent; tests.

Deferred (later specs/plans, noted here so coverage is honest): synthesizing sample data from `outputSchema` when no `sampleOutput`; running *input-derived* output functions for real; the opt-in HTTP-stub + cassette high-fidelity tier; script/polyglot + AI-agent node policy; an automation-scoped (project/workspace) tool variant.

## Module reference

- Metadata + facades: `server/libs/platform/platform-component/platform-component-api` (interfaces + `MetadataConstants`), `…/platform-component-service` (impls).
- Handlers: `server/libs/platform/platform-workflow/platform-workflow-worker/platform-workflow-worker-api/.../task/handler/AbstractTaskHandler.java` + `.../trigger/handler/AbstractTriggerHandler.java`.
- PreSend: `server/libs/platform/platform-workflow/platform-workflow-test/.../TestTaskDispatcherPreSendProcessor.java`; `server/libs/automation/automation-workflow/automation-workflow-coordinator/.../ProjectTaskDispatcherPreSendProcessor.java`.
- Sync exec: `server/libs/platform/platform-job-sync/.../executor/JobSyncExecutor.java`; template `…/platform-workflow-test/.../facade/TestWorkflowExecutorImpl.java`.
- Tool home: `server/libs/platform/platform-ai/platform-ai-tool` (mirror `TaskTools`).

---

### Task 1: `DRY_RUN` metadata key

**Files:**
- Modify: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/constant/MetadataConstants.java`

**Interfaces:**
- Produces: `MetadataConstants.DRY_RUN` (String key `"dryRun"`).

- [ ] **Step 1: Add the constant** next to `CONNECTION_IDS` / `EDITOR_ENVIRONMENT`:

```java
public static final String DRY_RUN = "dryRun";
```

- [ ] **Step 2: Compile.** Run: `./gradlew :server:libs:platform:platform-component:platform-component-api:compileJava -q` — Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit.** `git add -A && git commit -m "732 Add DRY_RUN task metadata key"`

---

### Task 2: Static-output resolver on `ActionDefinitionFacade`

**Files:**
- Create: `…/platform-component-service/.../facade/DryRunOutputResolver.java` (shared static resolver — reused by the trigger facade in Task 4)
- Modify: `…/platform-component-api/.../facade/ActionDefinitionFacade.java`
- Modify: `…/platform-component-service/.../facade/ActionDefinitionFacadeImpl.java`
- Test: `…/platform-component-service/src/test/java/.../facade/DryRunOutputResolverTest.java` + `…/facade/ActionDefinitionFacadeDryRunTest.java`

**Interfaces:**
- Produces: `DryRunOutputResolver.resolve(@Nullable OutputResponse) → Object` — returns `sampleOutput`, else `placeholder`, else empty `Map.of()`. The single home of the extraction logic (Task 4's trigger facade calls the same helper — no duplicated logic block).
- Produces: `Object executeDryRunPerform(String componentName, int componentVersion, String actionName)` — never invokes a dynamic output function.
- Consumes: `ActionDefinitionService.getActionDefinition(componentName, version, actionName)` → `ActionDefinition.getOutputResponse()` (`OutputResponse(outputSchema, sampleOutput, placeholder)`), guards `isOutputDefined()`.

- [ ] **Step 1: Add to the interface** (`ActionDefinitionFacade.java`):

```java
/**
 * Returns the action's declared STATIC output (sampleOutput, else placeholder, else empty) for a dry-run — never
 * invokes perform() or a dynamic output function, so it makes no API/connection call.
 */
Object executeDryRunPerform(String componentName, int componentVersion, String actionName);
```

- [ ] **Step 2: Write the failing test** (`ActionDefinitionFacadeDryRunTest.java`): mock `ActionDefinitionService.getActionDefinition(...)` to return an `ActionDefinition` whose `getOutputResponse()` yields `OutputResponse.of(schema, sampleOutput)`; assert `executeDryRunPerform` returns that `sampleOutput`; a second case with no output defined returns `Map.of()`.

```java
@Test
void testReturnsDeclaredSampleOutput() {
    ActionDefinition actionDefinition = mock(ActionDefinition.class);
    when(actionDefinition.isOutputDefined()).thenReturn(true);
    when(actionDefinition.getOutputResponse())
        .thenReturn(OutputResponse.of(Map.of("greeting", "hi")));
    when(actionDefinitionService.getActionDefinition("slack", 1, "sendMessage")).thenReturn(actionDefinition);

    assertThat(facade.executeDryRunPerform("slack", 1, "sendMessage")).isEqualTo(Map.of("greeting", "hi"));
}
```

- [ ] **Step 3: Run test to verify it fails.** Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests "*ActionDefinitionFacadeDryRunTest" -q` — Expected: FAIL (method missing).

- [ ] **Step 4a: Create the shared `DryRunOutputResolver`** (Apache header) with a `DryRunOutputResolverTest` first (RED): resolves sampleOutput, falls back to placeholder, then empty, and null → empty.

```java
public final class DryRunOutputResolver {

    private DryRunOutputResolver() {
    }

    public static Object resolve(@Nullable OutputResponse outputResponse) {
        if (outputResponse == null) {
            return Map.of();
        }

        if (outputResponse.sampleOutput() != null) {
            return outputResponse.sampleOutput();
        }

        return outputResponse.placeholder() != null ? outputResponse.placeholder() : Map.of();
    }
}
```

- [ ] **Step 4b: Implement `executeDryRunPerform` in `ActionDefinitionFacadeImpl`** delegating the extraction to the shared resolver:

```java
@Override
public Object executeDryRunPerform(String componentName, int componentVersion, String actionName) {
    ActionDefinition actionDefinition = actionDefinitionService.getActionDefinition(
        componentName, componentVersion, actionName);

    return DryRunOutputResolver.resolve(
        actionDefinition.isOutputDefined() ? actionDefinition.getOutputResponse() : null);
}
```

- [ ] **Step 5: Run tests to verify they pass.** Same command as Step 3 — Expected: PASS.

- [ ] **Step 6: spotlessApply + commit.** `./gradlew spotlessApply -q && git add -A && git commit -m "732 Add ActionDefinitionFacade.executeDryRunPerform static-output resolver"`

---

### Task 3: Dry-run branch in `AbstractTaskHandler`

**Files:**
- Modify: `…/platform-workflow-worker-api/.../task/handler/AbstractTaskHandler.java`
- Test: `…/platform-workflow-worker-api/src/test/java/.../task/handler/AbstractTaskHandlerDryRunTest.java`

**Interfaces:**
- Consumes: `MetadataConstants.DRY_RUN` (Task 1), `ActionDefinitionFacade.executeDryRunPerform` (Task 2).

- [ ] **Step 1: Write the failing test:** a concrete `AbstractTaskHandler` subclass over a mock `ActionDefinitionFacade`; a `TaskExecution` with metadata `{dryRun=true, jobId=1}`; assert `handle` returns the facade's `executeDryRunPerform` value and `executePerform` is **never** called.

```java
@Test
void testDryRunReturnsDeclaredOutputWithoutPerform() {
    when(actionDefinitionFacade.executeDryRunPerform("slack", 1, "sendMessage")).thenReturn(Map.of("ok", true));

    TaskExecution taskExecution = TaskExecution.builder()
        .jobId(1L)
        .metadata(Map.of(MetadataConstants.DRY_RUN, true))
        .workflowTask(new WorkflowTask(Map.of("type", "slack/v1/sendMessage")))
        .build();

    assertThat(handler.handle(taskExecution)).isEqualTo(Map.of("ok", true));
    verify(actionDefinitionFacade, never()).executePerform(any(), anyInt(), any(), any(), any(), any(), any(), any(),
        any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any());
}
```

- [ ] **Step 2: Run to verify it fails** (perform is called today). Run: `./gradlew :server:libs:platform:platform-workflow:platform-workflow-worker:platform-workflow-worker-api:test --tests "*AbstractTaskHandlerDryRunTest" -q` — Expected: FAIL.

- [ ] **Step 3: Add the branch at the top of `handle`** (before reading connectIdMap), reusing the handler's `componentName`/`componentVersion`/`actionName` fields:

```java
if (MapUtils.getBoolean(taskExecution.getMetadata(), MetadataConstants.DRY_RUN, false)) {
    return actionDefinitionFacade.executeDryRunPerform(componentName, componentVersion, actionName);
}
```

- [ ] **Step 4: Run to verify it passes.** Same command — Expected: PASS.

- [ ] **Step 5: spotlessApply + commit.** `... commit -m "732 Return declared output from AbstractTaskHandler on dry-run"`

---

### Task 4: Trigger dry-run branch

**Files:**
- Modify: `…/platform-component-api/.../facade/TriggerDefinitionFacade.java` + `…/platform-component-service/.../facade/TriggerDefinitionFacadeImpl.java`
- Modify: `…/platform-workflow-worker-api/.../trigger/handler/AbstractTriggerHandler.java`
- Test: `…/platform-component-service/.../facade/TriggerDefinitionFacadeDryRunTest.java`

**Interfaces:**
- Produces: `Object executeDryRunTrigger(String componentName, int componentVersion, String triggerName)` — returns the trigger's static `OutputResponse.sampleOutput()` (→ placeholder → empty), via `TriggerDefinitionService.getTriggerDefinition(...).getOutputResponse()`. Never the dynamic output function.

- [ ] **Step 1: Add `executeDryRunTrigger` to the interface + impl.** The impl fetches the trigger definition and delegates the extraction to the SHARED `DryRunOutputResolver.resolve(...)` from Task 2 — do NOT re-implement the sampleOutput/placeholder/empty logic:

```java
@Override
public Object executeDryRunTrigger(String componentName, int componentVersion, String triggerName) {
    TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
        componentName, componentVersion, triggerName);

    return DryRunOutputResolver.resolve(
        triggerDefinition.isOutputDefined() ? triggerDefinition.getOutputResponse() : null);
}
```

- [ ] **Step 2: Write + run the failing facade test** (mirror Task 2's test). Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests "*TriggerDefinitionFacadeDryRunTest" -q` — FAIL → implement → PASS.

- [ ] **Step 3: Add the dry-run branch to `AbstractTriggerHandler.handle`** (before the `executeTrigger` call), reusing its `componentName`/`componentVersion`/`triggerName` fields:

```java
if (MapUtils.getBoolean(triggerExecution.getMetadata(), MetadataConstants.DRY_RUN, false)) {
    return triggerDefinitionFacade.executeDryRunTrigger(componentName, componentVersion, triggerName);
}
```

- [ ] **Step 4: Compile the worker-api module.** Run: `./gradlew :server:libs:platform:platform-workflow:platform-workflow-worker:platform-workflow-worker-api:compileJava -q` — BUILD SUCCESSFUL.

- [ ] **Step 5: spotlessApply + commit.** `... commit -m "732 Return declared trigger output on dry-run"`

---

### Task 5: Copy `DRY_RUN` job→task metadata in the PreSend processors

**Files:**
- Modify: `…/platform-workflow-test/.../coordinator/task/dispatcher/TestTaskDispatcherPreSendProcessor.java`
- Modify: `…/automation-workflow-coordinator/.../task/dispatcher/ProjectTaskDispatcherPreSendProcessor.java`
- Test: extend each processor's existing test (or add one) asserting `DRY_RUN` propagates from `job.getMetadata()` to `taskExecution.getMetadata()`.

**Interfaces:**
- Consumes: `Job.getMetadata()`, `MetadataConstants.DRY_RUN`. Produces: `taskExecution.putMetadata(DRY_RUN, …)`.

- [ ] **Step 1:** In each processor's `process(TaskExecution)`, after it loads the `Job`, copy the flag through:

```java
taskExecution.putMetadata(
    MetadataConstants.DRY_RUN, MapUtils.getBoolean(job.getMetadata(), MetadataConstants.DRY_RUN, false));
```

- [ ] **Step 2: Write/extend a test** per processor: a `Job` with metadata `{dryRun=true}` → after `process`, `taskExecution.getMetadata()` contains `dryRun=true`.

- [ ] **Step 3: Run the two processor test suites.** Run: `./gradlew :server:libs:platform:platform-workflow:platform-workflow-test:test :server:libs:automation:automation-workflow:automation-workflow-coordinator:test -q` — PASS.

- [ ] **Step 4: spotlessApply + commit.** `... commit -m "732 Propagate DRY_RUN from job metadata onto task executions"`

---

### Task 6: `WorkflowSimulationFacade` over `JobSyncExecutor`

**Files:**
- Create: `server/libs/platform/platform-job-sync/.../simulation/WorkflowSimulationFacade.java` (+ `WorkflowSimulationResult` record)
- Create impl: `.../simulation/WorkflowSimulationFacadeImpl.java`
- Test: `.../simulation/WorkflowSimulationFacadeIntTest.java` (Testcontainers not required — sync in-memory)

**Interfaces:**
- Produces: `WorkflowSimulationResult simulate(String workflowId, Map<String,?> inputs)` where `WorkflowSimulationResult(Outcome outcome, @Nullable String failedTaskName, @Nullable String failedTaskType, @Nullable String reason, List<String> warnings)` and `enum Outcome { COMPLETED, FAILED }`.
- Consumes: `JobSyncExecutor.execute(JobParametersDTO, boolean checkForError)`; builds `JobParametersDTO(workflowId, inputs, metadata)` with `metadata = Map.of(MetadataConstants.DRY_RUN, true)` (plus `EDITOR_ENVIRONMENT=true`, mirroring `TestWorkflowExecutorImpl:418-424`).

- [ ] **Step 1: Define the result type** `WorkflowSimulationResult` (record + `Outcome` enum) as above.

- [ ] **Step 2: Implement `simulate`** — mirror `TestWorkflowExecutorImpl.executeSync` for the `JobSyncExecutor` wiring, but set `DRY_RUN=true` in the metadata and pass `checkForError=false` so a failed task returns a Job you can inspect rather than throwing:

```java
public WorkflowSimulationResult simulate(String workflowId, Map<String, ?> inputs) {
    Job job = jobSyncExecutor.execute(
        new JobParametersDTO(
            workflowId, inputs, Map.of(MetadataConstants.DRY_RUN, true, MetadataConstants.EDITOR_ENVIRONMENT, true)),
        false);

    if (job.getStatus() == Job.Status.COMPLETED) {
        return new WorkflowSimulationResult(Outcome.COMPLETED, null, null, null, List.of());
    }

    TaskExecution failed = taskExecutionService.getJobTaskExecutions(job.getId())
        .stream()
        .filter(taskExecution -> taskExecution.getStatus() == TaskExecution.Status.FAILED)
        .findFirst()
        .orElse(null);

    return new WorkflowSimulationResult(
        Outcome.FAILED,
        failed == null ? null : failed.getName(),
        failed == null ? null : failed.getType(),
        job.getError() == null ? (failed == null ? null : failed.getError().getMessage()) : job.getError().getMessage(),
        List.of());
}
```

  (Resolve the exact `Job.getError()`/`TaskExecution.getError()` accessors against the domain classes; the shape above is the target.)

- [ ] **Step 3: Wire the bean** in the platform-job-sync configuration (constructor: `JobSyncExecutor`, `TaskExecutionService`). Confirm `platform-job-sync` already exposes a `JobSyncExecutor` bean usable here; if the sync executor is request-scoped/built per-call in `TestWorkflowExecutorImpl`, mirror that construction instead of injecting a singleton.

- [ ] **Step 4: Write an integration test** driving a tiny two-task workflow (one component action referencing the other's output) through `simulate`, asserting `COMPLETED`; and a workflow with a task referencing a non-existent step asserting `FAILED` with that task's name.

- [ ] **Step 5: Run + spotlessApply + commit.** `... commit -m "732 Add WorkflowSimulationFacade driving JobSyncExecutor in dry-run"`

---

### Task 7: `simulateWorkflow` shared `@Tool`

**Files:**
- Create: `server/libs/platform/platform-ai/platform-ai-tool/.../tool/SimulationTools.java` (mirror `TaskTools`)
- Test: `.../tool/SimulationToolsTest.java`

**Interfaces:**
- Produces: a `@Component` `SimulationTools` with `@Tool String simulateWorkflow(String workflowId)` (workspace/env from `ToolContext` like `WorkflowExecutionTools`) → returns a JSON string of `WorkflowSimulationResult`.
- Consumes: `WorkflowSimulationFacade` (Task 6).

- [ ] **Step 1: Write `SimulationTools`** mirroring `TaskTools`/`WorkflowExecutionTools`: inject `WorkflowSimulationFacade`, one `@Tool` method that resolves the workflow, calls `simulate`, and returns `JsonUtils.write(result)`. `@Tool` description: "Dry-run a workflow: executes the DAG with mocked component outputs (no real calls) and returns COMPLETED or the first failing task + reason. Use to validate a workflow you just built."

- [ ] **Step 2: Unit test** `SimulationTools` with a mocked facade (COMPLETED and FAILED cases → correct JSON).

- [ ] **Step 3: Compile + test + spotlessApply + commit.** `... commit -m "732 Expose simulateWorkflow as a shared @Tool"`

---

### Task 8: Wire the tool onto the shared workflow-editor build subagent

**Files:**
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/.../config/CopilotConfiguration.java` (the `workflowEditorBuildSubAgentChatClient` bean)

**Interfaces:**
- Consumes: `SimulationTools` (Task 7).

- [ ] **Step 1: Inject `SimulationTools`** into `CopilotConfiguration` and add it to `workflowEditorBuildSubAgentChatClient`'s `.defaultTools(...)` (it's a `@Tool` object, so it goes in `defaultTools`, alongside `projectTools`, `workflowValidatorTools`, …). This single bean also backs the management MCP `workflow_editor` agent (via `WorkflowEditorAgentToolCallback`), so both surfaces gain `simulateWorkflow`.

- [ ] **Step 2: Update the workflow-editor BUILD prompt** (`prompt_workflow_editor_build.txt`) to instruct: after building/editing, call `simulateWorkflow` and fix any reported failing task before finishing.

- [ ] **Step 3: Compile ai-copilot-service + spotlessApply + commit.** `... commit -m "732 Give the workflow-editor build subagent the simulateWorkflow tool"`

---

### Task 9: Full verification

- [ ] **Step 1: `check` every touched module.** Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:check :server:libs:platform:platform-workflow:platform-workflow-worker:platform-workflow-worker-api:check :server:libs:platform:platform-workflow:platform-workflow-test:check :server:libs:automation:automation-workflow:automation-workflow-coordinator:check :server:libs:platform:platform-job-sync:check :server:libs:platform:platform-ai:platform-ai-tool:check :server:libs:ai:ai-copilot:ai-copilot-service:check` — all BUILD SUCCESSFUL.
- [ ] **Step 2: Assemble the monolith** to confirm wiring. Run: `./gradlew :server:apps:server-app:compileJava` — BUILD SUCCESSFUL.
- [ ] **Step 3: Manual smoke (optional):** run a real workflow with `dryRun` off (unchanged) and a built workflow via `simulateWorkflow` (COMPLETED / FAILED-at-task).

---

## Self-Review

**Spec coverage:** backbone (declared-output substitution, transport-agnostic) → Tasks 2–4; flag threading without atlas changes → Tasks 1, 5; shared surface-agnostic facade+tool → Tasks 6–8; findings contract → Task 6's `WorkflowSimulationResult`. Deferred items (schema-synthesis, HTTP cassettes, input-derived-run, script/AI policy, automation-scoped tool) are listed under Scope and not silently dropped.

**Placeholder scan:** the only "resolve against the domain" notes are `Job.getError()`/`TaskExecution.getError()` accessor names and the `JobSyncExecutor` bean-vs-per-call construction — real lookups at implementation, not logic placeholders.

**Type consistency:** `executeDryRunPerform`/`executeDryRunTrigger`, `MetadataConstants.DRY_RUN`, `WorkflowSimulationResult`/`Outcome`, and `SimulationTools.simulateWorkflow` are used with the same names across tasks.
