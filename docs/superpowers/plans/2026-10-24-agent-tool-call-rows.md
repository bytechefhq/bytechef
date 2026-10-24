# Agent Tool Call Rows Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show each AI Agent tool call (and Task Tool sub-agent run) as its own row under the agent's task row, with that call's Input, Output or Error, and Logs, in the editor's test output and the Workflow Executions sheets.

**Architecture:** The 6333 recorder gains an ordered list of per-call records beside its per-element aggregate. A thread-bound "current call" scope gives nested calls their parent and stamps log entries with the call id. The worker stores the records on the task execution's metadata, the API exposes them, and the client maps each record to a task-execution-shaped row so the existing nesting, selection and details pane are reused.

**Tech Stack:** Java 25, Spring Boot 4, Spring AI tool callbacks, spring-ai-agent-utils Task Tool, OpenAPI generator (Spring + typescript-fetch), GraphQL, React 19, Vitest.

**Spec:** `docs/superpowers/specs/2026-10-24-agent-tool-call-rows-design.md`

## Global Constraints

- Elements covered: TOOLS elements and Task Tool sub-agents (with their tools, including background runs). No rows for model, memory, RAG or guardrails.
- One row per call, in call order; a sub-agent's tool calls nest under the sub-agent's call row.
- Input and output are capped at 65,536 characters each; a truncated value ends with `\n... [truncated, <original length> characters]`.
- Call ids are random UUIDs.
- Calls that start or end after the agent task's drain are not written back; a call still started at the drain is stored as started.
- A suspended call is stored as SUSPENDED and, on resume, completes with the human's answer as its output.
- The agent's own Logs tab is unchanged; call rows filter logs by call id.
- Commit format: `6553 <desc>` (server), `6553 client - <desc>` (client), `6553 docs - <desc>`; generated code only in `6553 Generate` / `6553 client - Generate` commits.
- No explanatory inline code comments; the reasoning goes in the commit message. No em dashes in prose.
- A task is done only when `./gradlew <module>:check` (server) or `npm run check` (client, run from `client/`) passes for what it touched, not just `test`.

## Review Focus

- A tool that returns several megabytes: the stored input and output stay capped with the marker, and the SSE event carries the capped value. (Task 1 `testCapsLongValues`, Task 7 `testCallEventsCarryCappedValues`.)
- Two tool calls in flight at once (parallel tool execution): each gets its own record and id, and neither inherits the other as parent. (Task 2 `testConcurrentCallsKeepTheirOwnScope`.)
- A task execution stored before this feature (no `clusterElementCallRecords` key): no rows, no error, on both surfaces. (Task 9 `returns no rows for a task execution without call records`.)
- A resumed agent task: the stored SUSPENDED record and the resume-time completion merge into one COMPLETED record, not two rows. (Task 4 `testResumeCompletesTheSuspendedCall`, Task 5 `testMergesRecordsById`.)
- A background sub-agent still running when the agent finished: its rows read "Still running when the agent finished", not a spinner. (Task 9 `marks a call still running when its agent finished`.)

---

### Task 1: Per-call records in the recorder

**Files:**
- Create: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/ai/agent/ClusterElementCallRecord.java`
- Create: `.../definition/ai/agent/ClusterElementCallRef.java`
- Create: `.../definition/ai/agent/ClusterElementCallScope.java`
- Create: `.../definition/ai/agent/ClusterElementCallDrain.java`
- Modify: `.../definition/ai/agent/ClusterElementCallRecorder.java`, `.../definition/ai/agent/ClusterElementCallEvent.java`
- Test: `server/libs/platform/platform-component/platform-component-api/src/test/java/com/bytechef/platform/component/definition/ai/agent/ClusterElementCallRecorderTest.java`, `.../ClusterElementCallRecordTest.java`, `.../ClusterElementCallScopeTest.java`

**Interfaces:**
- Produces:
  - `record ClusterElementCallRecord(String id, @Nullable String parentId, @Nullable String workflowNodeName, @Nullable String componentName, @Nullable String toolName, Status status, @Nullable Instant startDate, @Nullable Instant endDate, @Nullable String input, @Nullable String output, @Nullable String error)` with `enum Status {STARTED, SUSPENDED, COMPLETED, FAILED}`, `static final int MAX_VALUE_LENGTH = 65_536`, `static @Nullable String cap(@Nullable String value)`, `Map<String, Object> toMap()`, `static ClusterElementCallRecord fromMap(Map<String, ?> map)`, `static List<ClusterElementCallRecord> merge(List<ClusterElementCallRecord> existing, List<ClusterElementCallRecord> incoming)`.
  - `record ClusterElementCallRef(String id, String workflowNodeName)`.
  - `final class ClusterElementCallScope` with `static @Nullable ClusterElementCallRef current()` and `static <T> T callWith(@Nullable ClusterElementCallRef ref, Supplier<T> supplier)` (ThreadLocal, restores the previous value in `finally`; a null ref clears it for the call).
  - `record ClusterElementCallDrain(Map<String, ClusterElementCall> calls, List<ClusterElementCallRecord> records)`.
  - Recorder (replaces the name-based 6333 methods):
    - `ClusterElementCallRef callStarted(long jobId, long taskExecutionId, String workflowNodeName, String componentName, @Nullable String toolName, @Nullable String parentId, @Nullable String input)`
    - `void callCompleted(long jobId, long taskExecutionId, ClusterElementCallRef ref, @Nullable String output)`
    - `void callFailed(long jobId, long taskExecutionId, ClusterElementCallRef ref, String error)`
    - `void callSuspended(long jobId, long taskExecutionId, ClusterElementCallRef ref)`
    - `Optional<ClusterElementCallRef> findSuspendedCall(long taskExecutionId)` (the last suspended call of that task execution)
    - `ClusterElementCallDrain drain(long taskExecutionId)`
  - `record ClusterElementCallEvent(long jobId, long taskExecutionId, Type type, ClusterElementCall call, ClusterElementCallRecord record)`.

- [ ] **Step 1: Write the failing tests**

`ClusterElementCallRecordTest`:
- `testCapsLongValues`: `cap("x".repeat(70_000))` starts with 65,536 `x` and ends with `"\n... [truncated, 70000 characters]"`; `cap("short")` returns `"short"`; `cap(null)` returns null.
- `testMapRoundTrip`: a record with every field set equals `fromMap(record.toMap())`; a record with only id, status and endDate round-trips too.
- `testMergesRecordsById`: existing `[a(STARTED, input "i"), b(COMPLETED)]`, incoming `[a(COMPLETED, output "o", startDate null), c(STARTED)]` gives `[a(COMPLETED, input "i", output "o", original startDate), b, c]`: incoming non-null fields win, order is existing then new ids.

`ClusterElementCallRecorderTest` (keep the 6333 tests, ported to the new API):
- `testRecordsEachCallWithItsInputAndOutput`: two `callStarted` on `httpClient_1` with inputs `"{\"a\":1}"` and `"{\"a\":2}"`, both completed with outputs; `drain(10).records()` has two records in start order with distinct UUID ids, their inputs and outputs, componentName `httpClient`, status COMPLETED; `drain(10)` afterwards is empty.
- `testFailedCallKeepsItsError`: `callFailed(..., ref, "boom")` gives a FAILED record with error `"boom"` and an endDate.
- `testRecordsTheParentCall`: a call started with `parentId` = the first call's id has that `parentId`.
- `testSuspendedCallIsFoundAndStoredAsSuspended`: after `callSuspended`, `findSuspendedCall(10)` returns its ref and the drained record is SUSPENDED.
- `testCompletingAnUnknownCallRecordsAPartialRecord`: `callCompleted` with a ref the recorder never saw (`new ClusterElementCallRef("id-1", "httpClient_1")`) drains a record with id `"id-1"`, workflowNodeName `httpClient_1`, status COMPLETED, an endDate and no startDate.
- `testEventsCarryTheCallRecord`: a listener's STARTED event has `record().input()` set; the COMPLETED event has `record().output()` set and the same `record().id()`.
- `testCapsRecordedValues`: a 70,000-character input is stored capped.

`ClusterElementCallScopeTest`:
- `testCallWithBindsAndRestores`: inside `callWith(ref, ...)` `current()` is `ref`; nested `callWith(inner, ...)` sees `inner` and the outer value returns afterwards; outside, `current()` is null, also when the supplier throws.

- [ ] **Step 2: Run them and watch them fail**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-api:test --tests '*ClusterElementCall*'`
Expected: compilation fails (new types and methods missing).

- [ ] **Step 3: Implement**

`TaskExecutionCalls` keeps a `LinkedHashMap<String, ClusterElementCallRecord>` beside the per-element aggregate and in-flight counts, and the last suspended ref. The aggregate is still keyed by workflow node name and merged as in 6333; a completion for an unknown ref updates the aggregate under `ref.workflowNodeName()`. Values pass through `cap` when recorded. Ids come from `UUID.randomUUID()`. Merging and publishing stay under the per-task-execution lock.

- [ ] **Step 4: Run them and watch them pass**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-api:check`
Expected: BUILD SUCCESSFUL. The modules that call the old methods fail to compile until Tasks 2, 3 and 5; do not run them yet.

- [ ] **Step 5: Commit**

`6553 Record each cluster element call with its input, output and parent`

### Task 2: Tool callbacks record their calls

**Files:**
- Modify: `server/libs/modules/components/ai/llm/src/main/java/com/bytechef/component/ai/llm/tool/ClusterElementCallObservingToolCallback.java`
- Modify: `server/libs/modules/components/ai/agent/src/main/java/com/bytechef/component/ai/agent/action/AbstractAiAgentChatAction.java` (the `wrap` call site)
- Test: `server/libs/modules/components/ai/llm/src/test/java/com/bytechef/component/ai/llm/tool/ClusterElementCallObservingToolCallbackTest.java`, `server/libs/modules/components/ai/agent/src/test/java/com/bytechef/component/ai/agent/action/AbstractAiAgentChatActionTest.java`

**Interfaces:**
- Consumes: Task 1 recorder API and `ClusterElementCallScope`.
- Produces: `static List<ToolCallback> wrap(List<ToolCallback> toolCallbacks, String workflowNodeName, String componentName, Context context)`; the constructor gains `String componentName`.

- [ ] **Step 1: Write the failing tests** (port the existing tests, then add)
- `testRecordsInputOutputAndToolName`: `call("{\"q\":1}")` returning `"result"` drains one COMPLETED record with input `{"q":1}`, output `result`, toolName = the delegate's `getToolDefinition().name()` (`get`), componentName `httpClient`.
- `testBindsTheCallWhileTheToolRuns`: the delegate captures `ClusterElementCallScope.current()`; it equals the record's id and workflow node name; afterwards `current()` is null.
- `testTakesItsParentFromTheEnclosingCall`: calling inside `ClusterElementCallScope.callWith(parentRef, ...)` records `parentId` = `parentRef.id()`.
- `testSuspendedResultMarksTheCallSuspended`: a suspended result drains a SUSPENDED record and `findSuspendedCall` returns its ref.
- `testConcurrentCallsKeepTheirOwnScope`: two calls on two threads, each delegate blocking on a latch until both started; both records have a null `parentId` and each delegate saw its own id.
- `AbstractAiAgentChatActionTest`: the existing wrapped-callback test asserts the component name passed for an `httpClient/v1/get` tool element is `httpClient`.

- [ ] **Step 2: Run and watch them fail**

Run: `./gradlew :server:libs:modules:components:ai:llm:test --tests '*ClusterElementCallObservingToolCallbackTest' :server:libs:modules:components:ai:agent:test --tests '*AbstractAiAgentChatActionTest'`
Expected: FAIL (compilation).

- [ ] **Step 3: Implement**

`observe` reads the parent from `ClusterElementCallScope.current()`, calls `callStarted`, runs the delegate under `callWith(ref, ...)`, then calls `callCompleted` with the result, `callFailed` with the message (or the exception's simple name), or `callSuspended` for a suspended result. A suspension failure (`ToolSuspension.isToolSuspensionFailure`) records nothing further, as in 6333.

- [ ] **Step 4: Run and watch them pass**

Run: `./gradlew :server:libs:modules:components:ai:llm:check :server:libs:modules:components:ai:agent:check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit** `6553 Record each tool call's input, output and tool name`

### Task 3: Sub-agent runs record their calls and parent their tools

**Files:**
- Modify: `server/libs/modules/components/ai/agent/utils/src/main/java/com/bytechef/component/ai/agent/utils/cluster/subagent/ClusterElementCallObservingSubagentExecutor.java`
- Modify: `.../cluster/subagent/TenantAwareTaskRepository.java`
- Modify: `.../cluster/AiAgentUtilsTaskTool.java` (`buildSubagentToolCallbacks` passes the tool element's component name to `wrap`)
- Test: `server/libs/modules/components/ai/agent/utils/src/test/java/com/bytechef/component/ai/agent/utils/cluster/subagent/ClusterElementCallObservingSubagentExecutorTest.java`, `.../subagent/TenantAwareTaskRepositoryTest.java`, `.../cluster/AiAgentUtilsTaskToolTest.java`

**Interfaces:**
- Consumes: Tasks 1 and 2.

- [ ] **Step 1: Write the failing tests**
- `testRecordsTheRunWithPromptAndResult`: a run drains a COMPLETED record with workflowNodeName `subagent_1`, componentName from the cluster element (`aiAgentUtils`), toolName = the sub-agent's name (`Analyst`), input = `taskCall.prompt()`, output `done`.
- `testParentsTheSubagentsToolCalls`: the delegate records a nested call through the recorder with `parentId` = `ClusterElementCallScope.current().id()`; that nested record's parent is the run's id.
- `testTakesItsParentFromTheTaskToolCall`: run inside `callWith(taskToolRef, ...)` records `parentId` = `taskToolRef.id()`.
- `TenantAwareTaskRepositoryTest.testCarriesTheCurrentCallToTheBackgroundTask`: `putTask` called inside `callWith(ref, ...)`; the supplier, run on the executor, sees `current()` equal to `ref`.
- `AiAgentUtilsTaskToolTest`: the existing sub-agent tool wrapping test asserts the component name of the wrapped tool.

- [ ] **Step 2: Run and watch them fail**

Run: `./gradlew :server:libs:modules:components:ai:agent:utils:test`
Expected: FAIL.

- [ ] **Step 3: Implement** `putTask` captures `ClusterElementCallScope.current()` beside the tenant id and runs the supplier under both. The executor observes as the tool callback does.

- [ ] **Step 4: Run and watch them pass**

Run: `./gradlew :server:libs:modules:components:ai:agent:utils:check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit** `6553 Record sub-agent runs and nest their tool calls under them`

### Task 4: A resumed agent completes its suspended call

**Files:**
- Modify: `server/libs/modules/components/ai/agent/src/main/java/com/bytechef/component/ai/agent/tool/AgentToolSuspension.java`
- Modify: `.../agent/tool/SuspendableToolCallingManager.java`
- Modify: `.../agent/action/AbstractAiAgentChatAction.java` (`buildResumedRequestSpec`)
- Test: `server/libs/modules/components/ai/agent/src/test/java/com/bytechef/component/ai/agent/tool/SuspendableToolCallingManagerTest.java`, `.../agent/action/AbstractAiAgentChatActionTest.java`, `.../agent/tool/AgentToolSuspensionTest.java`

**Interfaces:**
- Consumes: `ClusterElementCallRecorder.findSuspendedCall`, `callCompleted`.
- Produces: `AgentToolSuspension(ConversationState conversation, String pendingToolCallId, String suspendedToolResult, @Nullable String suspendedClusterElementCallId, @Nullable String suspendedClusterElementWorkflowNodeName)`.

- [ ] **Step 1: Write the failing tests**
- `SuspendableToolCallingManagerTest.testStoresTheSuspendedCall`: with the recorder holding a suspended ref for the task execution, the stored suspension carries its id and workflow node name.
- `AgentToolSuspensionTest.testReadsASuspensionStoredWithoutTheCall`: JSON without the two new fields deserializes with both null.
- `AbstractAiAgentChatActionTest.testResumeCompletesTheSuspendedCall`: resuming with data `{"approved":true}` and a suspension carrying call `id-1` / `approval_1` makes the recorder drain a COMPLETED record `id-1` with output `{"approved":true}`.

- [ ] **Step 2: Run and watch them fail**

Run: `./gradlew :server:libs:modules:components:ai:agent:test`
Expected: FAIL.

- [ ] **Step 3: Implement** Read the suspended ref with `findSuspendedCall(actionContext.getTaskExecutionId())` when building the suspension; on resume, when both fields are present, call `callCompleted` with a ref built from them and the tool result as output.

- [ ] **Step 4: Run and watch them pass**

Run: `./gradlew :server:libs:modules:components:ai:agent:check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit** `6553 Complete a suspended tool call when its agent resumes`

### Task 5: Store the records on the task execution

**Files:**
- Modify: `server/libs/platform/platform-workflow/platform-workflow-execution/platform-workflow-execution-api/src/main/java/com/bytechef/platform/workflow/execution/constant/TaskExecutionMetadataConstants.java`
- Modify: `server/libs/platform/platform-worker/src/main/java/com/bytechef/platform/worker/task/ClusterElementCallsTaskExecutionPostOutputProcessor.java`
- Test: `server/libs/platform/platform-worker/src/test/java/com/bytechef/platform/worker/task/ClusterElementCallsTaskExecutionPostOutputProcessorTest.java`

**Interfaces:**
- Consumes: `ClusterElementCallDrain`, `ClusterElementCallRecord.merge/toMap/fromMap`.
- Produces: `TaskExecutionMetadataConstants.CLUSTER_ELEMENT_CALL_RECORDS = "clusterElementCallRecords"` (a list of `toMap()` maps).

- [ ] **Step 1: Write the failing tests** (port the existing ones to the new recorder API)
- `testWritesRecordsToMetadata`: two completed calls give two maps under `clusterElementCallRecords` in call order, beside the existing `clusterElementCalls` aggregate.
- `testMergesRecordsWithStoredOnes`: stored `[a SUSPENDED]` plus a drained partial `a COMPLETED` gives one COMPLETED record `a` keeping its stored input.
- `testNoCallsLeavesMetadataUntouched` keeps passing for the new key too.

- [ ] **Step 2: Run and watch them fail**

Run: `./gradlew :server:libs:platform:platform-worker:test --tests '*ClusterElementCallsTaskExecutionPostOutputProcessorTest'`
Expected: FAIL.

- [ ] **Step 3: Implement** in `onTaskExecutionFinished`, alongside the aggregate merge.

- [ ] **Step 4: Run and watch them pass**

Run: `./gradlew :server:libs:platform:platform-worker:check :server:libs:platform:platform-job-sync:check :server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api:check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit** `6553 Store the cluster element call records on the task execution`

### Task 6: Expose the records on the task execution API

**Files:**
- Create: `server/libs/platform/platform-workflow/platform-workflow-execution/platform-workflow-execution-api/src/main/java/com/bytechef/platform/workflow/execution/dto/ClusterElementCallRecordDTO.java`
- Modify: `.../execution/dto/TaskExecutionDTO.java`
- Modify: `server/libs/platform/platform-workflow/platform-workflow-execution/platform-workflow-execution-rest/platform-workflow-execution-rest-impl/openapi.yaml`
- Modify: `server/libs/platform/platform-workflow/platform-workflow-test/platform-workflow-test-rest/build.gradle.kts` (map `ClusterElementCallRecord` in `schemaMappings` as 6333 did for `ClusterElementCall`)
- Test: `.../platform-workflow-execution-api/src/test/java/com/bytechef/platform/workflow/execution/dto/TaskExecutionDTOTest.java`

**Interfaces:**
- Produces: `TaskExecutionDTO.clusterElementCallRecords: List<ClusterElementCallRecordDTO>` (empty when absent); OpenAPI schema `ClusterElementCallRecord` with `id` (required), `parentId`, `workflowNodeName`, `componentName`, `toolName`, `status` (enum STARTED, SUSPENDED, COMPLETED, FAILED, required), `startDate`, `endDate` (date-time), `input`, `output`, `error` (strings); `TaskExecution.clusterElementCallRecords: ClusterElementCallRecord[]`.

- [ ] **Step 1: Write the failing test** `testReadsClusterElementCallRecordsFromMetadata`: metadata with two record maps gives two DTOs with all fields; a task execution without the key gives an empty list.
- [ ] **Step 2: Run and watch it fail**: `./gradlew :server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api:test --tests '*TaskExecutionDTOTest'`
- [ ] **Step 3: Implement**, and add the schema to `openapi.yaml`.
- [ ] **Step 4: Run and watch it pass**: same module `check`.
- [ ] **Step 5: Commit** `6553 Expose cluster element call records on task executions`
- [ ] **Step 6: Generate the server code** with the `generateOpenAPI` tasks of `platform-workflow-execution-rest-impl` and `platform-workflow-test-rest`, then `./gradlew` `check` on `platform-workflow-execution-rest-api`, `-rest-impl` and `platform-workflow-test-rest`. Commit only the generated files: `6553 Generate`.
- [ ] **Step 7: Generate the TypeScript clients** with the `generateOpenAPI` tasks of `platform-workflow-execution-rest-impl`, `automation-workflow-execution-rest`, `embedded-workflow-execution-rest` and `platform-workflow-test-rest`; confirm `ClusterElementCallRecord.ts` appears under `client/src/shared/middleware/platform/workflow/execution`, `.../platform/workflow/test`, `.../automation/workflow/execution` and `client/src/ee/shared/middleware/embedded/workflow/execution`. Commit: `6553 client - Generate`.

### Task 7: Live call events carry the record

**Files:**
- Modify: `server/libs/platform/platform-workflow/platform-workflow-test/platform-workflow-test-service/src/main/java/com/bytechef/platform/workflow/test/facade/TestWorkflowExecutorImpl.java` (`getClusterElementCallPayload`)
- Test: `.../platform-workflow-test-service/src/test/java/com/bytechef/platform/workflow/test/facade/TestWorkflowExecutorTest.java`

**Interfaces:**
- Produces: the `cluster_element_*` payload gains `callId`, `parentCallId`, `componentName`, `toolName`, `callStatus` (the record's status), `input` (started) and `output` (completed); the existing keys stay.

- [ ] **Step 1: Write the failing test** `testCallEventsCarryCappedValues`: a started then completed call yields payloads with the same `callId`, the started one carrying `input`, the completed one `output`, and a 70,000-character output arriving capped.
- [ ] **Step 2: Run and watch it fail**: `./gradlew :server:libs:platform:platform-workflow:platform-workflow-test:platform-workflow-test-service:test --tests '*TestWorkflowExecutorTest'`
- [ ] **Step 3: Implement**.
- [ ] **Step 4: Run and watch it pass**: same module `check`.
- [ ] **Step 5: Commit** `6553 Send each call's id, input and output on the live call events`

### Task 8: Logs know which call wrote them

**Files:**
- Modify: `server/libs/platform/platform-component/platform-component-log/platform-component-log-api/src/main/java/com/bytechef/platform/component/log/domain/LogEntry.java` (two components appended: `@Nullable String clusterElementCallId, @Nullable String clusterElementWorkflowNodeName`, and builder methods of the same names)
- Modify: `server/libs/platform/platform-component/platform-component-context/platform-component-context-service/src/main/java/com/bytechef/platform/component/context/ContextImpl.java` (`storeLogEntry`)
- Modify: `server/libs/platform/platform-component/platform-component-log/platform-component-log-graphql/src/main/java/com/bytechef/platform/component/log/web/graphql/LogFileGraphQlController.java` (`LogFilterInput` gains `String clusterElementCallId`; `applyFilters` filters on it)
- Modify: `server/libs/platform/platform-configuration/platform-configuration-graphql/src/main/java/com/bytechef/platform/configuration/web/graphql/EditorLogFileGraphQlController.java` (`applyFilters` filters on it)
- Modify: `.../platform-component-log-graphql/src/main/resources/graphql/component-log-file.graphqls` (`LogEntry` type gains `clusterElementCallId: String`, `clusterElementWorkflowNodeName: String`; `LogFilterInput` gains `clusterElementCallId: String`)
- Test: `ContextImplTest` (log section), `LogFileGraphQlControllerTest`, `EditorLogFileGraphQlControllerTest`, the log storage round-trip test in `platform-component-log-service`

- [ ] **Step 1: Write the failing tests**
- `testStampsLogEntriesWithTheCurrentCall`: logging inside `ClusterElementCallScope.callWith(ref, ...)` stores an entry with `ref.id()` and `ref.workflowNodeName()`; outside, both are null.
- `testFiltersByClusterElementCallId` (both controllers): of three entries for one task execution, two stamped with call `a` and `b`, a `clusterElementCallId: "a"` filter returns only `a`'s; no filter returns all three.
- Storage round-trip: an entry with both fields survives write and read; a stored line without them reads with both null.
- [ ] **Step 2: Run and watch them fail** with each module's `test`.
- [ ] **Step 3: Implement**.
- [ ] **Step 4: Run and watch them pass**: `check` on `platform-component-log-api`, `platform-component-log-service`, `platform-component-log-graphql`, `platform-component-context-service`, `platform-configuration-graphql`.
- [ ] **Step 5: Commit** `6553 Record which cluster element call wrote each log entry`

### Task 9: Call rows in the task list

**Files:**
- Create: `client/src/shared/components/workflow-executions/util/toClusterElementCallTaskExecutions.ts`
- Modify: `client/src/shared/components/workflow-executions/WorkflowExecutionsAccordionItem.tsx`, `WorkflowTaskExecutionItem.tsx`
- Test: `client/src/shared/components/workflow-executions/util/tests/toClusterElementCallTaskExecutions.test.ts`, `client/src/shared/components/workflow-executions/tests/WorkflowExecutionsAccordionItem.test.tsx`

**Interfaces:**
- Consumes: generated `ClusterElementCallRecord` and `TaskExecution.clusterElementCallRecords` (platform workflow execution client).
- Produces: `interface ClusterElementCallTaskExecutionI extends TaskExecution { clusterElementCallId: string; clusterElementCallNote?: string; parentTaskExecutionId: string }`; `export default function toClusterElementCallTaskExecutions(taskExecution: TaskExecution): ClusterElementCallTaskExecutionI[]` (top-level calls, each with its nested calls in `children`); `export function isClusterElementCallTaskExecution(execution: TaskExecution | TriggerExecution): execution is ClusterElementCallTaskExecutionI`.

Mapping per record: `id` = record id; `jobId` from the task; `parentTaskExecutionId` = the task's id; `title` = `toolName ?? workflowNodeName`, plus ` #n` when that element was called more than once in the task execution (n by start order across the whole task execution); `workflowTask` = `{label: title, name: workflowNodeName}`; `icon` = `/icons/<componentName>.svg`; `status` STARTED/SUSPENDED→`STARTED`, COMPLETED→`COMPLETED`, FAILED→`FAILED`; `error` = `{message: error}`; `input`/`output` parsed as JSON when they parse, else the raw string; `clusterElementCallNote` = `Waiting for a response` for SUSPENDED, `Still running when the agent finished` for STARTED when the task's status is not `STARTED`.

- [ ] **Step 1: Write the failing tests** (`toClusterElementCallTaskExecutions.test.ts`)
- `nests a sub-agent's tool calls under its run`: records task tool `t`, sub-agent `s` (parent `t`), tool `f` (parent `s`), tool `g` (no parent) give `[t(children [s(children [f])]), g]`.
- `numbers an element called more than once`: two `fetch_ok` calls titled `fetch_ok #1`, `fetch_ok #2`; a single call has no number.
- `marks a call still running when its agent finished`: STARTED record + task status `COMPLETED` → note `Still running when the agent finished`; same record + task status `STARTED` → no note.
- `marks a suspended call as waiting`.
- `parses JSON input and keeps plain text`.
- `returns no rows for a task execution without call records`.
- `WorkflowExecutionsAccordionItem.test.tsx`: `shows an agent's tool calls as nested rows` renders `fetch_ok #1` under the agent row; clicking it calls `onExecutionClick` with the call execution.
- [ ] **Step 2: Run and watch them fail**: `npx vitest run src/shared/components/workflow-executions`
- [ ] **Step 3: Implement**. The accordion uses `taskExecution.children` when present, otherwise the call executions; `WorkflowTaskExecutionItem` shows `clusterElementCallNote` under the title.
- [ ] **Step 4: Run and watch them pass**: same command, then `npm run check`.
- [ ] **Step 5: Commit** `6553 client - Show an agent's tool calls as nested rows in the task list`

### Task 10: Call rows show their own logs

**Files:**
- Modify: `client/src/shared/components/workflow-executions/WorkflowExecutionLogsContent.tsx` (prop `clusterElementCallId?: string`, added to `jobLogsFilter` and the editor filter)
- Modify: `client/src/shared/components/workflow-executions/WorkflowExecutionsTabsPanel.tsx` (for a call row pass `taskExecutionId={parentTaskExecutionId}` and `clusterElementCallId`)
- Modify: `client/src/graphql/platform/component-log/jobFileLogs.graphql`, `editorJobFileLogs.graphql` (select the two new `LogEntry` fields)
- Test: `client/src/shared/components/workflow-executions/tests/WorkflowExecutionLogsContent.test.tsx`, `.../tests/WorkflowExecutionsTabsPanel.test.tsx`

- [ ] **Step 1: Regenerate GraphQL types**: `cd client && npx graphql-codegen`; commit only `src/shared/middleware/graphql.ts` and the `.graphql` operations as `6553 client - Generate`.
- [ ] **Step 2: Write the failing tests**
- `filters the logs to a tool call`: with `clusterElementCallId="a"` and `taskExecutionId="10"`, the job logs query receives `filter: {clusterElementCallId: 'a', taskExecutionId: '10'}`; the same for the editor query.
- `asks for a call row's logs by its call and task execution`: selecting a call execution renders the logs content with the parent task execution id and the call id.
- [ ] **Step 3: Run and watch them fail**: `npx vitest run src/shared/components/workflow-executions/tests`
- [ ] **Step 4: Implement**.
- [ ] **Step 5: Run and watch them pass**, then `npm run check`.
- [ ] **Step 6: Commit** `6553 client - Show a tool call's own logs on its row`

### Task 11: Live call rows in the test output

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/stores/useWorkflowEditorStore.ts` (`workflowTestClusterElementCallRecords: Record<string, ClusterElementCallRecord[]>`, `upsertWorkflowTestClusterElementCallRecord(taskExecutionId: string, record: ClusterElementCallRecord)`, cleared inside `resetWorkflowTestNodeStates`)
- Modify: `client/src/shared/hooks/useWorkflowTestStream.ts` (`handleClusterElementCall` also upserts the record built from the payload's `callId`, `parentCallId`, `name`, `componentName`, `toolName`, `callStatus`, dates, `input`, `output`, `error`)
- Create: `client/src/pages/platform/workflow-editor/utils/withLiveClusterElementCallRecords.ts` (`export default function withLiveClusterElementCallRecords(workflowTestExecution: WorkflowTestExecution | undefined, recordsByTaskExecutionId: Record<string, ClusterElementCallRecord[]>): WorkflowTestExecution | undefined`)
- Modify: `client/src/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput.tsx` (passes the merged execution to `useWorkflowExecutions`)
- Test: `client/src/shared/hooks/tests/useWorkflowTestStream.test.ts`, `client/src/pages/platform/workflow-editor/utils/withLiveClusterElementCallRecords.test.ts`

Merge rule: per task execution, stored records come first and win for an id they contain; live records with other ids are appended in arrival order.

- [ ] **Step 1: Write the failing tests**
- `useWorkflowTestStream`: `records a live call from its events`: a started then completed event with `callId: 'a'` leave one record `a` with its input and output in `workflowTestClusterElementCallRecords['10']`.
- `withLiveClusterElementCallRecords`: `adds live calls the snapshot does not have yet`; `keeps the stored record over the live one`; `leaves an execution without live calls untouched` (same object).
- Store: `clears live call records with the node states`.
- [ ] **Step 2: Run and watch them fail**: `npx vitest run src/shared/hooks/tests/useWorkflowTestStream.test.ts src/pages/platform/workflow-editor/utils/withLiveClusterElementCallRecords.test.ts`
- [ ] **Step 3: Implement**.
- [ ] **Step 4: Run and watch them pass**, then `npm run check`.
- [ ] **Step 5: Commit** `6553 client - Add a tool call's row as soon as the call starts during a test run`

### Task 12: Docs and live verification

**Files:**
- Modify: `docs/content/docs/platform/automation/build/workflows/ai/agent/index.mdx` ("Seeing which tools ran": each call is a row under the agent with Input, Output or Error, and Logs; sub-agent calls nest; values over 65,536 characters are truncated; a background sub-agent's calls show their state when the agent finished)
- Modify: `docs/content/docs/platform/automation/monitor/workflow-executions.mdx` (the coming-soon note on the detail view mentions tool call rows)

- [ ] **Step 1: Update the two pages** inside their existing coming-soon markers.
- [ ] **Step 2: Live check** against a branch server with the "Agent Colour" workflow: during a test run, `fetch_ok` and `fetch_fail` rows appear under AI Agent, the first COMPLETED with its input and httpbin output, the second FAILED with its error; each Logs tab shows only its own entries; the deployed webhook run's executions sheet shows the same rows.
- [ ] **Step 3: Commit** `6553 docs - Describe tool call rows`
