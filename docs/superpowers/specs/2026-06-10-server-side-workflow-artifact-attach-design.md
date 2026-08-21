# Server-side workflow-artifact attach — design

- **Date:** 2026-06-10
- **Branch:** `0_732`
- **Status:** Draft (design); pending user review
- **Author:** Ivica Cardic
- **Spec #3 of 3.** Builds on the shipped #1 (shared tools + neutral `AgentToolInvocationContext` in
  `ai-copilot-tool`) and #2a (shared client `ai-chat` pipeline). Independent of the deferred #2b
  (Copilot panel wiring).

## Problem

When the workflow-editor agent builds a workflow for an AI Hub task, the workflow attaches to the
task as an `ai_hub_task_artifact` row only through a **fragile LLM relay**:

1. The editor subagent must END its turn with `workflowId: <id> | projectId: <id> |
   projectWorkflowId: <id>` (prompt contract in `prompt_workflow_editor_build.txt`).
2. The parent AI Hub agent must parse those ids and call the client-signaling `openWorkflowTab` tool.
3. `OpenWorkflowTabToolCallback` records a `WORKFLOW_REFERENCED` artifact (and the client tab-watcher
   also records on tab open).

Every link can break: the model omits or invents an id, forgets to call `openWorkflowTab`, or the
client hook doesn't fire. The observed symptom (reported by the user) is the agent claiming it built
the workflow while **no artifact is attached to the task** — the user is left thinking nothing
happened.

## Goal

Record the workflow artifact **server-side, as a direct side-effect of the persist tool succeeding**,
so attachment no longer depends on the model echoing ids back or any client hook firing. Keep the
behavior surface-neutral: surfaces without an AI Hub task (Copilot in-editor panel, embedded
autonomous generation, the plain editor) record nothing.

## What's already true (verified)

- The workflow-persist tools are **CE**: `ProjectWorkflowTools.createProjectWorkflow(long projectId,
  String definition)` → `ProjectWorkflowInfo` and `updateWorkflow(String workflowId, String
  definition)` → `WorkflowInfo`, both in
  `server/libs/automation/automation-ai/automation-ai-tool` (`com.bytechef.automation.ai.tool`).
  The module already uses the Spring AI tool API (`@Tool`/`@ToolParam`), so a `ToolContext` parameter
  is available with no new dependency.
- `ai-hub-service` (EE) **already depends on both** `automation-ai-tool` (CE) and `ai-copilot-tool`
  (EE), so an SPI defined in CE `automation-ai-tool` can be implemented by EE `ai-hub-service`. CE
  must NOT depend on EE; this direction is legal.
- The parent agent's `ToolContext` (carrying the neutral `bytechef.agentTool.conversationId` =
  AI Hub threadId, populated by `AiHubSpringAIAgent`) **is forwarded to the editor subagent's tools**:
  `WorkflowEditorAgentToolCallback` calls `.toolContext(toolContext.getContext())` on the subagent
  chat client. So `ProjectWorkflowTools`' `@Tool` methods receive it once they declare a `ToolContext`
  param.
- `AiHubTaskArtifactKind` (EE) already has dedicated `WORKFLOW_CREATED` and `WORKFLOW_UPDATED` values
  (append-only enum, ordinals pinned by `EnumOrdinalStabilityTest`). `WORKFLOW_REFERENCED` is what the
  existing `openWorkflowTab` path emits.
- `AiHubTaskService.findByThreadId(String)` exists (ai-hub-api). The existing
  `AiHubTaskArtifactRecorderImpl` already maps `String kind → enum`, guards a null `userId` (with a
  `bytechef.artifact.record.missing_userid_total` counter), and delegates to
  `AiHubTaskArtifactService.record(threadId, userId, kind, artifactId, artifactName, metadata)`.
- **Dedup gap:** the `record(threadId, …)` path does NOT dedup (it always inserts — correct for
  append-only audit kinds like `FILE_CREATED`). Only `recordReference(taskId, …)` dedups, via
  `AiHubTaskArtifactRepository.findFirstByTaskIdAndKindAndArtifactId`.

## Architecture

A neutral CE SPI the persist tool calls, implemented by EE where the AI Hub task lives.

```
[CE] ProjectWorkflowTools.createProjectWorkflow / updateWorkflow
        │  (after successful persist)
        ▼
[CE] WorkflowArtifactRecorder  (interface, optional bean)        ← "moved under CE"
        │
        ▼
[EE] WorkflowArtifactRecorderImpl  (@ConditionalOnProperty bytechef.ai.hub.enabled)
        │  extracts conversationId(threadId)+userId from ToolContext
        │  via AgentToolInvocationContext.fromToolContext(...)
        │  conversationId == null  → no-op
        ▼
[EE] AiHubTaskArtifactService.recordWorkflowArtifact(...)  ← new, dedups on (taskId, workflowId)
```

### CE: the SPI

New interface in `com.bytechef.automation.ai.tool` (CE `automation-ai-tool`):

```java
public interface WorkflowArtifactRecorder {
    /**
     * Records the persisted workflow as a task artifact, when the invocation carries an AI Hub
     * conversation context. A no-op otherwise. Best-effort: never fails the persist.
     *
     * @param toolContext       the tool invocation context (carries the optional conversation id)
     * @param created           true for a freshly created workflow, false for an update
     * @param workflowId        the workflow UUID (artifact id)
     * @param projectId         owning project id (routing metadata)
     * @param projectWorkflowId project-workflow id (routing metadata), may be null
     * @param workflowName      display-name snapshot
     */
    void recordWorkflowArtifact(
        @Nullable ToolContext toolContext, boolean created,
        String workflowId, long projectId, @Nullable Long projectWorkflowId, String workflowName);
}
```

Rationale for passing `ToolContext` (not pre-extracted `conversationId`/`userId`): it keeps the
`bytechef.agentTool.*` key knowledge entirely in EE (`AgentToolInvocationContext`), so CE never
duplicates those constants. `automation-ai-tool` already depends on the Spring AI tool API, so
`org.springframework.ai.chat.model.ToolContext` is on its classpath.

### CE: tool wiring

`ProjectWorkflowTools`:
- Inject `@Nullable WorkflowArtifactRecorder workflowArtifactRecorder` (constructor; optional so CE /
  non-AI-Hub deployments start cleanly — `ObjectProvider` or `@Nullable` autowire-required=false).
- Add a `ToolContext toolContext` parameter to `createProjectWorkflow` and `updateWorkflow` (Spring AI
  injects it; it is NOT a `@ToolParam`, so it does not appear in the tool's input schema).
- After the successful `projectWorkflowFacade.addWorkflow(...)` / `updateWorkflow(...)`, if the
  recorder is non-null, call `recordWorkflowArtifact(...)` inside a try/catch that logs and swallows —
  artifact recording must never fail the persist (mirrors `OpenWorkflowTabToolCallback.recordArtifact`).

Field mapping:
- **create:** `workflowId = ProjectWorkflowInfo.workflowId()`, `projectId =
  ProjectWorkflowInfo.projectId()`, `projectWorkflowId = ProjectWorkflowInfo.id()`, `created = true`.
- **update:** `workflowId = WorkflowInfo.getWorkflowUuid()`, `projectWorkflowId =
  WorkflowInfo.getId()`, `created = false`. `WorkflowInfo` does not carry `projectId`; resolve it from
  the `ProjectWorkflowDTO` already fetched in `updateWorkflow` (it exposes the owning project) or via
  the facade. (Confirm the exact accessor during planning.)
- **name:** create derives the display name from the `definition` JSON `label` field (the create path
  has only the definition + ids); update uses `WorkflowInfo.getLabel()`.

### EE: the impl

`WorkflowArtifactRecorderImpl` in `ai-hub-service` (`com.bytechef.ee.ai.hub.task` or `.tool`),
`@Component @ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue =
"true")`:
- `AgentToolInvocationContext invocationContext =
  AgentToolInvocationContext.fromToolContext(toolContext);`
- If `invocationContext == null || invocationContext.conversationId() == null` → return (no-op:
  Copilot panel, embedded autonomous, plain editor).
- Else delegate to `AiHubTaskArtifactService.recordWorkflowArtifact(conversationId, userId, created,
  workflowId, projectId, projectWorkflowId, workflowName)`, wrapped in try/catch (best-effort).

`@version ee` + Enterprise header (it lives under `server/ee/`).

### EE: the dedup-aware service method

New method on `AiHubTaskArtifactService` (+ impl):

```java
void recordWorkflowArtifact(
    String threadId, @Nullable Long userId, boolean created,
    String workflowId, long projectId, @Nullable Long projectWorkflowId, String workflowName);
```

Behavior:
1. Null-`userId` guard (reuse the existing missing-userid counter pattern).
2. Resolve the task via `findByThreadIdAndUserId(threadId, userId)`; if absent, warn + drop (same as
   `record`).
3. **Dedup on `(taskId, workflowId)` across workflow kinds.** Look up an existing artifact for the
   task whose `artifactId == workflowId` and whose kind ∈ {`WORKFLOW_CREATED`, `WORKFLOW_UPDATED`,
   `WORKFLOW_REFERENCED`}. New repository method, e.g.
   `findFirstByTaskIdAndArtifactIdAndKindIn(taskId, workflowId, kindOrdinals)`.
   - **Exists:** refresh `artifactName` + `metadataJson` (so a rename/route change is reflected); do
     NOT downgrade the kind (a created row stays `WORKFLOW_CREATED`). Save.
   - **Absent:** insert with kind = `created ? WORKFLOW_CREATED : WORKFLOW_UPDATED`, metadata
     `{projectId, projectWorkflowId}`, status `APPLIED`, environment denormalized from the task.

This single method makes create + N updates + a later `openWorkflowTab` reference converge on one
sidebar row.

### `openWorkflowTab` interaction

`OpenWorkflowTabToolCallback` keeps recording `WORKFLOW_REFERENCED` (its tab-open signal still serves
the case where the agent opens a **pre-existing** workflow it did not build this turn — no
create/update fired). To make the two paths converge rather than duplicate, route its recording
through the same dedup: either (a) point its recorder at the new dedup-aware method, or (b) have the
new `(taskId, workflowId)`-spanning dedup catch the reference. Plan picks the smaller change; the
guarantee is **one workflow → one artifact row per task**, regardless of create/update/open ordering.

## Out of scope

- **Embedded autonomous generation** (`prompt_workflow_editor_build.txt` "Autonomous Generation
  Mode") uses `submitWorkflow` on a pre-created workflow with no AI Hub task and no `conversationId` —
  it records nothing, which is correct.
- **Copilot in-editor panel** has no AI Hub task; `conversationId` is null → no-op. (Spec #2b wires
  the panel's pickers; it does not introduce a task to attach to.)
- The client tab-open behavior of `openWorkflowTab` is unchanged.

## Testing

- **CE (`automation-ai-tool`):** unit test that `createProjectWorkflow`/`updateWorkflow` invoke
  `WorkflowArtifactRecorder.recordWorkflowArtifact(...)` with the right `created` flag + mapped ids
  when a recorder bean is present (mock recorder), and do NOT fail the persist when the recorder
  throws. Also: no recorder bean → persist still succeeds.
- **EE (`ai-hub-service`):**
  - `WorkflowArtifactRecorderImpl`: conversationId present → delegates; conversationId null → no-op;
    recorder service throws → swallowed.
  - `AiHubTaskArtifactService.recordWorkflowArtifact`: insert-then-dedup (create, then update same
    workflowId → one row, kind stays `WORKFLOW_CREATED`, name refreshed); update-only → one
    `WORKFLOW_UPDATED` row; missing task → drop + warn; null userId → skip + counter.
  - Regression: an existing AI Hub build flow (`openWorkflowTab` after create) yields exactly one
    workflow artifact row, not two.
- **Idempotency** is the key behavior — give it explicit coverage (TDD: write the
  one-row-after-create-update-open assertion first; watch it fail against the duplicate-row status
  quo).

## EE conventions

New EE files keep the Enterprise header + `@version ee`. The new CE files
(`WorkflowArtifactRecorder`, the `ProjectWorkflowTools` changes) keep the Apache header (they live
under `server/libs/`). Spotless picks the header by file content/path per the project convention.

## Risks / open questions (resolve during planning)

- **`projectId` on the update path.** `WorkflowInfo` lacks `projectId`; confirm the accessor on the
  `ProjectWorkflowDTO` already fetched in `updateWorkflow` (or the facade) to populate routing
  metadata.
- **Recorder bean optionality in CE.** Confirm the cleanest optional-injection idiom so the AI-Hub-
  less server / CLI / embedded apps that wire `ProjectWorkflowTools` start without the bean.
- **Dedup repository method.** Spring Data JDBC derived query
  `findFirstByTaskIdAndArtifactIdAndKindIn(long, String, Collection<Integer>)` against INT-ordinal
  `kind` — verify the derived-query naming maps to the ordinal column, else use a `@Query`.
- **`openWorkflowTab` convergence.** Decide (a) reroute its recording through the dedup-aware method
  vs (b) rely on the spanning dedup. Smaller change wins; the invariant is one row per workflow/task.
- **ToolContext reaches `ProjectWorkflowTools` in the AI Hub flow** via the editor-subagent
  forwarding — add a wiring assertion if cheap, since the whole feature hinges on it.
