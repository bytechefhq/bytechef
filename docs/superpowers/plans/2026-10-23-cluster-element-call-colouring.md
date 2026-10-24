# Cluster Element Call Colouring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Colour the tools (and Task Tool sub-agents and their tools) an AI Agent calls - live during editor test runs and from the stored run in the workflow execution sheets - with the same running/completed/failed look task nodes have.

**Architecture:** Agent code wraps each TOOLS element's callbacks (and each sub-agent run) in an observer that reports call start/finish/failure, keyed by the element's workflow node name, to a process-local `ClusterElementCallRecorder`. The editor test executor subscribes to the recorder per job and forwards `cluster_element_*` SSE events; a worker post-output processor, running after streaming ends, drains the recorder into the agent task execution's `metadata`, which the task-execution API exposes as `clusterElementCalls`. The client writes both into the existing per-node state map; the execution sheets render cluster roots as frames.

**Tech Stack:** Java 25 / Spring Boot 4 / Spring AI tool callbacks; React 19 / TypeScript / zustand / React Flow; OpenAPI generator (Spring + typescript-fetch); JUnit 5 + Mockito; Vitest.

**Spec:** `docs/superpowers/specs/2026-10-23-cluster-element-call-colouring-design.md`

**Execution:** native (this session), one whole-branch review at the end - chosen because the user asked to implement right after the plan.

## Global Constraints

- Branch `6333` in the `bytechef-node-color` worktree; PR #6336. Commit when green; push only on an explicit "push".
- Commit messages: server `6333 <description>`, client `6333 client - <description>`; generated code in separate `6333 Generate` / `6333 client - Generate` commits.
- No explanatory inline code comments; the why goes in the commit message. Javadoc on new public types is fine and matches the surrounding code.
- New files get the Apache 2.0 header copied from a neighbouring file and `@author Ivica Cardic`; files under `ee/` get the EE header and `@version ee`.
- Server: run `./gradlew spotlessApply` and `./gradlew :<module>:check` for each touched module (not `:test`). Gradle exit status: read the build output, never `echo $?` after a redirect.
- Client: `npx prettier --write` the touched files, `npx eslint` them, `npx tsc --project tsconfig.json --noEmit`, `npx vitest run` the touched suites; `npm run check` before the final review. sort-keys, `I`/`Props` interface suffixes, `twMerge`, hook ordering per `CLAUDE.md`.
- Coloured elements: only TOOLS-slot elements (component tools, MCP tool providers, the Task Tool) and, beneath a Task Tool, its SUBAGENT children and their own TOOLS elements. Model, memory, RAG, guardrails are never reported.
- Failure = the call threw, except a tool suspension. A returned error-shaped result is a success. Any failed call keeps the element FAILED for the rest of the run.
- Simulated calls (`TOOL_SIMULATIONS`) are reported like real calls.
- The agent's output (`${aiAgent_1}`) must not change.
- Only the automation and embedded workflow execution sheets draw cluster frames on the read-only canvas; every other read-only canvas keeps `ReadOnlyNode`'s icon row.

## Review Focus

1. Parallel tool calls on one element (Spring AI may run several concurrently): the recorder must stay consistent under concurrent `callStarted`/`callCompleted` for the same name. Test in Task 1.
2. An agent inside a loop: the same element is called from several task executions of one run. A failure in iteration 1 must keep the node red after iteration 2 succeeds, on the live canvas and in the sheet. Tests in Tasks 9 and 10.
3. A run stopped or failing while a tool is mid-call: the tool's blue spinner must clear like a task's. Test in Task 9.
4. Tenants: two tenants with the same task execution id must not see each other's calls. Test in Task 1.
5. Task executions with no `clusterElementCalls` (every non-agent task, every old row): API returns an empty list, client renders nothing new. Tests in Tasks 8 and 10.

---

### Task 1: `ClusterElementCallRecorder`

**Files:**
- Create: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/ai/agent/ClusterElementCall.java`
- Create: `.../ai/agent/ClusterElementCallEvent.java`
- Create: `.../ai/agent/ClusterElementCallRecorder.java`
- Test: `server/libs/platform/platform-component/platform-component-api/src/test/java/com/bytechef/platform/component/definition/ai/agent/ClusterElementCallRecorderTest.java`

**Interfaces:**
- Produces:
  - `public record ClusterElementCall(String workflowNodeName, Status status, Instant startDate, @Nullable Instant endDate, @Nullable String error)` with nested `public enum Status { STARTED, COMPLETED, FAILED }` and `Map<String, Object> toMap()` / `static ClusterElementCall fromMap(Map<String, ?>)` (keys `workflowNodeName`, `status`, `startDate`, `endDate`, `error`; dates as ISO-8601 strings).
  - `public record ClusterElementCallEvent(long jobId, long taskExecutionId, Type type, ClusterElementCall call)` with `enum Type { STARTED, COMPLETED, FAILED }` (the transition; `call` carries the merged state).
  - `public final class ClusterElementCallRecorder` with `public static ClusterElementCallRecorder getInstance()`, a public no-arg constructor for tests, and:
    - `void callStarted(long jobId, long taskExecutionId, String workflowNodeName)`
    - `void callCompleted(long jobId, long taskExecutionId, String workflowNodeName)`
    - `void callFailed(long jobId, long taskExecutionId, String workflowNodeName, @Nullable String error)`
    - `AutoCloseable addListener(long jobId, Consumer<ClusterElementCallEvent> listener)`
    - `Map<String, ClusterElementCall> drain(long taskExecutionId)` (removes and returns)
    - `static Map<String, ClusterElementCall> merge(Map<String, ClusterElementCall> existing, Map<String, ClusterElementCall> incoming)`

Merge rules (one per element name): status FAILED is sticky; otherwise the latest transition wins (STARTED on a new call, COMPLETED on finish). `startDate` is the first start, `endDate` the last end, `error` the first failure's message. Keys are `TenantCacheKeyUtils.getKey(id)` for both job and task execution maps. Listener exceptions are caught and logged at trace so a broken subscriber cannot fail a tool call.

- [ ] **Step 1: Write the failing tests** in `ClusterElementCallRecorderTest`:
  - `testStartedThenCompleted`: after `callStarted(1, 10, "httpClient_1")` and `callCompleted(1, 10, "httpClient_1")`, `drain(10)` returns one entry with status `COMPLETED`, non-null `startDate` and `endDate`, `endDate` not before `startDate`.
  - `testFailedIsSticky`: started, failed with `"boom"`, started, completed → drained status `FAILED`, error `"boom"`.
  - `testDrainRemoves`: a second `drain(10)` is empty.
  - `testListenerReceivesTransitions`: a listener on job 1 receives types `[STARTED, COMPLETED]` in order, each event's `call().workflowNodeName()` is `"httpClient_1"`; after closing the handle, further calls deliver nothing; a listener on job 2 receives nothing.
  - `testListenerFailureDoesNotPropagate`: a listener that throws does not make `callStarted` throw.
  - `testConcurrentCallsOnOneElement`: 50 threads each run started+completed on the same name → drained status `COMPLETED`, no exception.
  - `testTenantsAreSeparate`: with `TenantContext` set to `"tenantA"` record a call on task execution 10; switch to `"tenantB"`; `drain(10)` is empty; switch back; `drain(10)` has the call.
  - `testMapRoundTrip`: `ClusterElementCall.fromMap(call.toMap())` equals `call` (for a FAILED call with error and both dates).
  - `testMerge`: existing `{a: FAILED}` merged with incoming `{a: COMPLETED, b: COMPLETED}` → `a` FAILED with the existing start date and the incoming end date, `b` COMPLETED.

- [ ] **Step 2: Run them to see them fail**
  Run: `./gradlew :server:libs:platform:platform-component:platform-component-api:test --tests '*ClusterElementCallRecorderTest'`
  Expected: compilation failure (types missing).

- [ ] **Step 3: Implement** the two records and the recorder (`ConcurrentHashMap<String, ConcurrentHashMap<String, ClusterElementCall>>` updated with `compute`; listeners in `ConcurrentHashMap<String, CopyOnWriteArrayList<Consumer<...>>>`). Add `testImplementation` deps only if the tenant context needs one.

- [ ] **Step 4: Run tests and module check**
  Run: `./gradlew spotlessApply :server:libs:platform:platform-component:platform-component-api:check`
  Expected: BUILD SUCCESSFUL, the 9 tests pass.

- [ ] **Step 5: Commit** `6333 Add a process-local recorder for cluster element calls`

---

### Task 2: Task execution id on the action context

**Files:**
- Modify: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/ActionContextAware.java` (next to `getJobId`)
- Modify: `server/libs/platform/platform-component/platform-component-context/platform-component-context-service/src/main/java/com/bytechef/platform/component/context/ActionContextImpl.java`
- Test: the existing `ActionContextImplTest` in that module (create it beside the class if absent)

**Interfaces:**
- Produces: `@Nullable Long getTaskExecutionId()` on `ActionContextAware`; `null` when the context was built without a task execution id (builder default `0`).

- [ ] **Step 1: Failing tests** `testGetTaskExecutionId` (builder `.taskExecutionId(42L)` → `42L`) and `testGetTaskExecutionIdWhenUnset` (no call → `null`).
- [ ] **Step 2: Run** `./gradlew :server:libs:platform:platform-component:platform-component-context:platform-component-context-service:test --tests '*ActionContextImplTest'` - expect FAIL.
- [ ] **Step 3: Implement** the interface method and `ActionContextImpl.getTaskExecutionId()`.
- [ ] **Step 4: Run** `./gradlew spotlessApply` then `:check` on both touched modules - expect BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `6333 Expose the task execution id on the action context`

---

### Task 3: Observing tool callback

**Files:**
- Create: `server/libs/modules/components/ai/llm/src/main/java/com/bytechef/component/ai/llm/tool/ClusterElementCallObservingToolCallback.java`
- Test: `server/libs/modules/components/ai/llm/src/test/java/com/bytechef/component/ai/llm/tool/ClusterElementCallObservingToolCallbackTest.java`

**Interfaces:**
- Consumes: Task 1 recorder; Task 2 `ActionContextAware.getJobId()` / `getTaskExecutionId()`; `ToolSuspension.isSuspendedToolResult(String, Suspend)` and the suspension-failure check used by `AbstractAiAgentChatAction.isToolSuspensionFailure` (move that check to `ToolSuspension` as `public static boolean isToolSuspensionFailure(Throwable)` if it is not already reachable from the llm module, and make `AbstractAiAgentChatAction` call it).
- Produces: `public static List<ToolCallback> wrap(List<ToolCallback> toolCallbacks, String workflowNodeName, Context context)` and `ClusterElementCallObservingToolCallback(ToolCallback delegate, String workflowNodeName, long jobId, long taskExecutionId, ClusterElementCallRecorder recorder, @Nullable Supplier<@Nullable Suspend> suspendSupplier)`. `wrap` returns the list unchanged when the context is not `ActionContextAware` or either id is `null`.

Behaviour of `call(input)` and `call(input, toolContext)`: `callStarted`; delegate; on a suspended result report nothing; on a normal result `callCompleted`; on a `RuntimeException` that is a suspension failure report nothing and rethrow; on any other `RuntimeException` `callFailed(message or simple class name)` and rethrow. `getToolDefinition()` and `getToolMetadata()` delegate.

- [ ] **Step 1: Failing tests** (Mockito delegate, real recorder instance, a listener collecting event types):
  - `testReportsCompleted` → `[STARTED, COMPLETED]`, delegate result returned unchanged.
  - `testReportsFailedAndRethrows` → delegate throws `IllegalStateException("boom")`; assertThrows same exception; types `[STARTED, FAILED]`, error `"boom"`.
  - `testReturnedErrorResultIsCompleted` → delegate returns `"{\"error\":\"404\"}"` → `[STARTED, COMPLETED]`.
  - `testSuspendedResultReportsNoOutcome` → suspend supplier returns a suspend matching the result → `[STARTED]` only.
  - `testDelegatesDefinitionAndMetadata`.
  - `testWrapWithoutIdsReturnsSameCallbacks` → plain `ActionContext` mock → same list instance.
- [ ] **Step 2: Run** `./gradlew :server:libs:modules:components:ai:llm:test --tests '*ClusterElementCallObservingToolCallbackTest'` - expect FAIL.
- [ ] **Step 3: Implement.** The suspend supplier in `wrap` reads `((ActionContextAware) context).getSuspend()`.
- [ ] **Step 4: Run** `./gradlew spotlessApply :server:libs:modules:components:ai:llm:check` - expect BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `6333 Observe cluster element tool calls`

---

### Task 4: Agent wraps its TOOLS elements

**Files:**
- Modify: `server/libs/modules/components/ai/agent/src/main/java/com/bytechef/component/ai/agent/action/AbstractAiAgentChatAction.java` (`getToolCallbacks`, ~line 634)
- Test: `server/libs/modules/components/ai/agent/src/test/java/com/bytechef/component/ai/agent/action/AbstractAiAgentChatActionTest.java`

**Interfaces:**
- Consumes: Task 3 `ClusterElementCallObservingToolCallback.wrap`.

Restructure the loop so each element's callbacks go: `clusterElementToolCallbacks.build(...)` → simulation wrapper (per callback, when simulations are set) → `ClusterElementCallObservingToolCallback.wrap(callbacks, clusterElement.getWorkflowNodeName(), context)`; the observable/augmented/suspension wrappers stay outside, unchanged. This applies to both `chat` and `streamChat` because both call `getToolCallbacks`. Chat-memory tool callbacks (`concatToolCallbacks`) are not wrapped.

- [ ] **Step 1: Failing tests:**
  - `testToolCallbacksReportCallsByWorkflowNodeName`: two TOOLS elements `httpClient_1`, `slack_1`; invoking the returned callback for `httpClient_1` makes a recorder listener (job id/task execution id from a mocked `ActionContextAware`) receive one STARTED and one COMPLETED for `"httpClient_1"` and nothing for `"slack_1"`.
  - `testSimulatedToolCallIsReported`: with a simulation for that tool, the call still yields STARTED/COMPLETED.
  - `testChatMemoryToolCallbacksAreNotReported`.
- [ ] **Step 2: Run** `./gradlew :server:libs:modules:components:ai:agent:test --tests '*AbstractAiAgentChatActionTest'` - expect FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `./gradlew spotlessApply :server:libs:modules:components:ai:agent:check` - expect BUILD SUCCESSFUL (existing agent tests included).
- [ ] **Step 5: Commit** `6333 Report the AI Agent's tool calls by cluster element`

---

### Task 5: Task Tool sub-agents and their tools

**Files:**
- Modify: `server/libs/modules/components/ai/agent/utils/src/main/java/com/bytechef/component/ai/agent/utils/cluster/AiAgentUtilsTaskTool.java` (`apply`, `buildSubagentToolCallbacks`)
- Create: `.../utils/cluster/subagent/ClusterElementCallObservingSubagentExecutor.java`
- Test: `.../utils/src/test/java/.../cluster/AiAgentUtilsTaskToolTest.java`, `.../cluster/subagent/ClusterElementCallObservingSubagentExecutorTest.java`

**Interfaces:**
- Consumes: Tasks 1-3.
- Produces: `ClusterElementCallObservingSubagentExecutor implements SubagentExecutor` with constructor `(SubagentExecutor delegate, long jobId, long taskExecutionId, ClusterElementCallRecorder recorder)`; `execute(TaskCall, SubagentDefinition)` reports start/complete/fail under the sub-agent element's workflow node name when the definition is a `ClusterElementSubagentDefinition`, else delegates silently; `getKind()` delegates.

`buildSubagentToolCallbacks` wraps each nested TOOLS element's callbacks with `ClusterElementCallObservingToolCallback.wrap(..., toolClusterElement.getWorkflowNodeName(), context)`. `apply` wraps `subagentExecutor` in the observing executor when the context carries both ids (ids captured on the calling thread, so background runs report against the agent's task execution).

- [ ] **Step 1: Failing tests:**
  - `ClusterElementCallObservingSubagentExecutorTest.testReportsCompleted`, `testReportsFailedAndRethrows`, `testNonClusterElementDefinitionIsNotReported`.
  - `ClusterElementCallObservingSubagentExecutorTest.testBackgroundRunReportsAgainstCapturedIds`: execute on another thread → event's `taskExecutionId` equals the constructor's.
  - `AiAgentUtilsTaskToolTest.testSubagentToolCallsAreReported`: a nested tool `httpClient_9` under sub-agent `subagent_1` reports STARTED/COMPLETED under `"httpClient_9"`.
- [ ] **Step 2: Run** `./gradlew :server:libs:modules:components:ai:agent:utils:test --tests '*ClusterElementCallObservingSubagentExecutorTest' --tests '*AiAgentUtilsTaskToolTest'` - expect FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `./gradlew spotlessApply :server:libs:modules:components:ai:agent:utils:check` - expect BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `6333 Report Task Tool sub-agent runs and their tool calls`

---

### Task 6: Store the calls on the task execution

**Files:**
- Create: `server/libs/platform/platform-worker/src/main/java/com/bytechef/platform/worker/task/ClusterElementCallsTaskExecutionPostOutputProcessor.java`
- Modify: `server/libs/platform/platform-worker/src/main/java/com/bytechef/platform/worker/config/PlatformWorkerConfiguration.java` (new bean `@Order(4)`; shift Suspend to 5, WebhookResponse to 6)
- Modify: `server/libs/platform/platform-job-sync/src/main/java/com/bytechef/platform/job/sync/executor/JobSyncExecutor.java` (`createTaskExecutionPostOutputProcessors`: insert after the WebSocket processor, before Suspend)
- Test: `server/libs/platform/platform-worker/src/test/java/com/bytechef/platform/worker/task/ClusterElementCallsTaskExecutionPostOutputProcessorTest.java`; extend the existing `JobSyncExecutor` processor-order test if one exists

**Interfaces:**
- Consumes: Task 1 `drain`, `merge`, `ClusterElementCall.toMap/fromMap`.
- Produces: metadata key `public static final String CLUSTER_ELEMENT_CALLS = "clusterElementCalls"` on the processor class; value `List<Map<String, Object>>` (one `toMap()` per element, sorted by `startDate`). `process(taskExecution, output)` returns `output` unchanged.

Drain by `taskExecution.getId()`; merge into whatever the metadata already holds under the key (a resumed suspended agent runs the action again on the same task execution); write back with `taskExecution.putMetadata(...)` (or the existing setter); do nothing when both are empty.

- [ ] **Step 1: Failing tests:** `testWritesDrainedCallsToMetadata`, `testMergesWithExistingMetadata` (existing FAILED stays FAILED), `testLeavesOutputUnchanged`, `testNoCallsLeavesMetadataUntouched`.
- [ ] **Step 2: Run** `./gradlew :server:libs:platform:platform-worker:test --tests '*ClusterElementCallsTaskExecutionPostOutputProcessorTest'` - expect FAIL.
- [ ] **Step 3: Implement** and register in both places.
- [ ] **Step 4: Run** `./gradlew spotlessApply :server:libs:platform:platform-worker:check :server:libs:platform:platform-job-sync:check` - expect BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `6333 Store cluster element calls on the task execution after the task's output is processed`

---

### Task 7: Live `cluster_element_*` SSE events in editor test runs

**Files:**
- Modify: `server/libs/platform/platform-workflow/platform-workflow-test/platform-workflow-test-service/src/main/java/com/bytechef/platform/workflow/test/facade/TestWorkflowExecutorImpl.java` (`registerListeners`)
- Test: `.../platform-workflow-test-service/src/test/java/.../facade/TestWorkflowExecutorTest.java`

**Interfaces:**
- Consumes: Task 1 `addListener`.
- Produces SSE events `cluster_element_started`, `cluster_element_completed`, `cluster_element_failed` (from `ClusterElementCallEvent.type()`), payload map: `name` (workflow node name), `taskExecutionId` (string), `status` (merged), `startDate`, `endDate` (omitted when null), `error` (omitted when null).

Add the handle to the returned list so it closes with the others; request a progress snapshot after each event like the task listeners do.

- [ ] **Step 1: Failing test** `testForwardsClusterElementCallEvents`: register listeners for job 7, record started+failed on `"httpClient_1"` for job 7 → the bridge receives `{"event": "cluster_element_started", "payload": {name: "httpClient_1", status: "STARTED", ...}}` then `cluster_element_failed` with `status` `FAILED` and `error`; after the handles close, a further call sends nothing.
- [ ] **Step 2: Run** `./gradlew :server:libs:platform:platform-workflow:platform-workflow-test:platform-workflow-test-service:test --tests '*TestWorkflowExecutorTest'` - expect FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `./gradlew spotlessApply` and `:check` on the module - expect BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `6333 Stream cluster element call events during editor test runs`

---

### Task 8: `clusterElementCalls` on the task-execution API

**Files:**
- Modify: `server/libs/platform/platform-workflow/platform-workflow-execution/platform-workflow-execution-api/src/main/java/com/bytechef/platform/workflow/execution/dto/TaskExecutionDTO.java`
- Modify: `server/libs/platform/platform-workflow/platform-workflow-execution/platform-workflow-execution-rest/platform-workflow-execution-rest-impl/openapi.yaml` (`TaskExecution` schema + new `ClusterElementCall` schema)
- Modify (only if MapStruct cannot map it by name): `.../platform-workflow-execution-rest-api/src/main/java/com/bytechef/platform/workflow/execution/web/rest/mapper/TaskExecutionMapper.java`
- Generated: Spring models under `.../platform-workflow-execution-rest-api/generated/`, TS under `client/src/shared/middleware/platform/workflow/execution/` and `client/src/shared/middleware/platform/workflow/test/`
- Test: `TaskExecutionDTOTest` beside the DTO (create if absent), plus the mapper test if one exists

**Interfaces:**
- Consumes: Task 6 metadata key and `ClusterElementCall.fromMap`.
- Produces: `TaskExecutionDTO.clusterElementCalls()` - `List<ClusterElementCallDTO>` where `public record ClusterElementCallDTO(String name, String status, Instant startDate, @Nullable Instant endDate, @Nullable String error)`; empty list when the metadata has no key. OpenAPI `ClusterElementCall { name: string, status: enum [STARTED, COMPLETED, FAILED], startDate: date-time, endDate: date-time, error: string }`, `TaskExecution.clusterElementCalls: array of ClusterElementCall` (readOnly). Client type `TaskExecution.clusterElementCalls?: Array<ClusterElementCall>` in both generated clients.

- [ ] **Step 1: Failing tests** `testClusterElementCallsFromMetadata` and `testNoClusterElementCallsIsEmptyList`.
- [ ] **Step 2: Run** the DTO test - expect FAIL.
- [ ] **Step 3: Implement** the DTO field (the canonical constructor reads the metadata) and the schema.
- [ ] **Step 4: Generate** `./gradlew :server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-rest:platform-workflow-execution-rest-impl:generateOpenAPI` and the test module's `generateOpenAPI` (find the task with `./gradlew :server:libs:platform:platform-workflow:platform-workflow-test:platform-workflow-test-rest:tasks --all | grep -i openapi`). Revert files whose only change is a `@Generated` date.
- [ ] **Step 5: Run** `./gradlew spotlessApply` and `:check` on the execution api, rest-api and rest-impl modules; `cd client && npx tsc --project tsconfig.json --noEmit` - expect success.
- [ ] **Step 6: Commit** three commits: hand-written `6333 Expose cluster element calls on task executions`, `6333 Generate`, `6333 client - Generate`.

---

### Task 9: Client - live element states from the test stream

**Files:**
- Create: `client/src/pages/platform/workflow-editor/utils/toClusterElementNodeState.ts`
- Modify: `client/src/shared/hooks/useWorkflowTestStream.ts` (three handlers beside `task_started`)
- Test: `client/src/pages/platform/workflow-editor/utils/tests/toClusterElementNodeState.test.ts` (follow the neighbouring test folder convention), `client/src/shared/hooks/tests/useWorkflowTestStream.test.ts`

**Interfaces:**
- Consumes: Task 7 payload; store `setWorkflowTestNodeState(name, state)` and `workflowTestNodeStates`.
- Produces: `export default function toClusterElementNodeState(call: {endDate?: string | Date; error?: string; startDate?: string | Date; status: string}, current?: WorkflowTestNodeStateI): WorkflowTestNodeStateI` - STARTED → `RUNNING`; COMPLETED → `COMPLETED` with `durationMillis` = end − start; FAILED → `FAILED` with `error` and duration; if `current?.status === 'FAILED'` the result keeps `FAILED` (and the current error) whatever `call.status` says.

- [ ] **Step 1: Failing tests:**
  - `toClusterElementNodeState`: `maps STARTED to RUNNING`, `maps COMPLETED with its duration` (start `2026-10-23T10:00:00Z`, end `…:00.412Z` → `durationMillis: 412`), `maps FAILED with its error`, `keeps a FAILED node failed when a later call completes`.
  - `useWorkflowTestStream`: `should mark a cluster element running and then completed` (handlers `cluster_element_started` then `cluster_element_completed` for `httpClient_1` → `mockSetWorkflowTestNodeState` last called with `('httpClient_1', {durationMillis: 412, status: 'COMPLETED'})`); `should keep a cluster element failed across task executions` (failed on task execution 1, completed on task execution 2 → last state FAILED); `should clear a cluster element left running when the run is stopped` (started, then `close()` → `mockRemoveWorkflowTestNodeState('httpClient_1')`).
- [ ] **Step 2: Run** `cd client && npx vitest run src/pages/platform/workflow-editor/utils src/shared/hooks/tests/useWorkflowTestStream.test.ts` - expect FAIL.
- [ ] **Step 3: Implement.** Handlers read `useWorkflowEditorStore.getState().workflowTestNodeStates[name]` as `current`.
- [ ] **Step 4: Run** the same command, then prettier/eslint/tsc on the touched files - expect PASS and clean.
- [ ] **Step 5: Commit** `6333 client - Colour cluster element calls live during a workflow test run`

---

### Task 10: Client - element states from stored task executions

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/utils/buildNodeStatesFromTaskExecutions.ts`
- Test: `client/src/pages/platform/workflow-editor/utils/buildNodeStatesFromTaskExecutions.test.ts`

**Interfaces:**
- Consumes: Task 8 `TaskExecution.clusterElementCalls`; Task 9 `toClusterElementNodeState`.
- Produces: the returned map also holds one entry per stored call name (visiting the same nesting the function already walks: children, iterations), applying the FAILED-sticky rule across task executions. The editor's result backfill gets this for free if it goes through this function; if `backfillNodeStatesFromResult` in `useWorkflowTestStream.ts` builds states another way, call this function there for the element entries.

- [ ] **Step 1: Failing tests:** `adds a state for each stored cluster element call`, `keeps a cluster element failed when another iteration completed it`, `ignores task executions without cluster element calls` (input without the field → output equals today's).
- [ ] **Step 2: Run** `cd client && npx vitest run src/pages/platform/workflow-editor/utils/buildNodeStatesFromTaskExecutions.test.ts` - expect FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** the suite and the `useWorkflowTestStream` suite, prettier/eslint/tsc - expect PASS.
- [ ] **Step 5: Commit** `6333 client - Colour cluster element calls from stored task executions`

---

### Task 11: Client - colour element nodes and their wires in the editor frame

**Files:**
- Modify: the edge component used for cluster frame member edges (find it from the `type` the member edges get in `client/src/pages/platform/cluster-element-editor/hooks/useClusterElementNodes.ts`), and `nodes/WorkflowNode.tsx` only if member nodes do not already render `WorkflowTestNodeStatus`
- Test: that edge component's test file; `nodes/WorkflowNode.test.tsx`

**Interfaces:**
- Consumes: `useWorkflowTestNodeStates()`, `useExecutedEdgeStatus(edgeId)` / `getExecutedEdgeStatus`.

A member edge from the root's Tools slot to an element takes the element's status (COMPLETED → green, FAILED → red, RUNNING → the running style task edges use, none → default). Model, memory, RAG and guardrail edges stay default because those elements never get a state.

- [ ] **Step 1: Failing tests:** `WorkflowNode.test.tsx` `shows the test status of a called cluster element` (node data of a cluster element `httpClient_1` with state FAILED → badge with `border-red-500`); the edge test `colours a tools edge by the called element's status` (COMPLETED → the green class/stroke the task edges use) and `leaves a model edge uncoloured`.
- [ ] **Step 2: Run** the two suites - expect FAIL (or the node test already passing, in which case keep it as a regression pin and note it).
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** the suites, prettier/eslint/tsc - expect PASS.
- [ ] **Step 5: Commit** `6333 client - Colour called cluster elements and their wires in the cluster frame`

---

### Task 12: Client - cluster frames on the execution sheets' read-only canvas

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/hooks/useLayout.tsx` (`boxModeClusterRootIds`, the read-only node mapping)
- Modify: `client/src/pages/platform/cluster-element-editor/hooks/useClusterElementNodes.ts` (accept the definition's tasks)
- Create: `client/src/pages/platform/workflow-editor/nodes/ReadOnlyClusterElementNode.tsx`; register it in the editor's `nodeTypes` as `readonlyClusterElement`
- Test: `client/src/pages/platform/workflow-editor/nodes/ReadOnlyClusterElementNode.test.tsx`, `client/src/pages/platform/cluster-element-editor/hooks/useClusterElementNodes.test.ts` (or its existing test), `client/src/pages/platform/workflow-editor/hooks/useWorkflowEditorLayout.test.ts` or a new `useLayout` read-only test

**Interfaces:**
- Consumes: `useWorkflowExecutionOverlay()` (non-null only on the two execution sheets); Task 10 states via `useWorkflowTestNodeStates()`.
- Produces: `useClusterElementNodes(clusterRootIds: string[], definitionTasks?: WorkflowTask[])` - when `definitionTasks` is given it is used instead of parsing `useWorkflowDataStore`'s definition. In `useLayout`: with `readOnlyWorkflow` and an overlay, frame every cluster root of `readOnlyWorkflow.tasks` regardless of the user's view mode, pass `readOnlyWorkflow.tasks` as `definitionTasks`, and map member nodes of type `workflow` to `readonlyClusterElement`; with `readOnlyWorkflow` and no overlay, `boxModeClusterRootIds` is `[]` (icon row as today). `ReadOnlyClusterElementNode` renders the round element node (icon, operation, workflow node name) with `WorkflowTestNodeStatus` and `getTestNodeStateBorderClassName`, no menus, no handlers, `isConnectable={false}` handles.

- [ ] **Step 1: Failing tests:** `ReadOnlyClusterElementNode` `shows the element's stored status` and `renders no node menu`; `useClusterElementNodes` `builds member nodes from the given definition tasks`; layout `frames cluster roots on a read-only canvas with an execution overlay` and `keeps the icon row on a read-only canvas without an overlay`.
- [ ] **Step 2: Run** the suites - expect FAIL.
- [ ] **Step 3: Implement.** If the frame's root renders through `ReadOnlyNode`, suppress its icon row when the node is framed (the frame already shows the elements).
- [ ] **Step 4: Run** the suites plus `npx vitest run src/pages/platform/workflow-editor src/pages/platform/cluster-element-editor src/pages/automation src/ee/pages/embedded`, prettier/eslint/tsc - expect PASS.
- [ ] **Step 5: Commit** `6333 client - Draw cluster roots as frames on the workflow execution sheets`

---

### Task 13: Live verification and review

- [ ] **Step 1:** Full checks: `./gradlew :<each touched server module>:check`, `cd client && npm run check`. Expected: BUILD SUCCESSFUL / all green.
- [ ] **Step 2:** Restart the branch server on 9555 (DB `bytechef_e2e_6333`) and Vite on 5183. Ask the user for an LLM provider API key (or an existing connection) and create a model connection.
- [ ] **Step 3:** Editor: an AI Agent with three tools whose prompt makes it call one that succeeds (HTTP GET to `https://httpbin.org/get`), one that throws (HTTP GET to `http://127.0.0.1:1/fail`), and never the third. Sample during Test: the called tools go blue then green/red with duration badges; their Tools wires take the colour; the third tool and the model stay plain; the bottom panel stays live.
- [ ] **Step 4:** Deployments: publish, deploy with a webhook trigger copy, `curl` it, open the run in Workflow Executions: the sheet draws the agent as a frame with the same colours. A template preview / `ReadOnlyWorkflowSheet` of the same workflow still shows the icon row.
- [ ] **Step 5:** Whole-branch review by a fresh reviewer (most capable model) over `6243..HEAD`; fix findings; re-run Step 1.
- [ ] **Step 6:** Report to the user; push only on "push".
