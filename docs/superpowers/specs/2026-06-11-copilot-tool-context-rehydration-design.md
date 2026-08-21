# Copilot Tool Context Rehydration — Eliminate the Workflow Generation Submission Store

- **Date:** 2026-06-11
- **Status:** Draft (design)
- **Area:** `server/ee/libs/ai/ai-copilot`, `server/ee/libs/ai/ai-hub`
- **Author:** Ivica Cardic

## Problem

ByteChef generates/edits workflows with the `workflow_editor_build` AI agent through **two** entry points that share the *same* `LocalAgent` bean:

1. **Interactive** — the Copilot panel streams the agent via `CopilotApiController` (`POST /internal/ai/chat/workflow_editor`). The agent persists by calling the `updateWorkflow` tool inline.
2. **Autonomous / headless** — the embedded public API (`/from-prompt`) and the (now-removed) sample-app chat drive the agent via `CopilotWorkflowGeneratorImpl.generateWorkflow`. Here the agent is **forbidden** from persisting and must call a `submitWorkflow` tool that does an in-memory put into `WorkflowGenerationSubmissionStore`; the generator then polls the store on the original request thread and performs the real persistence.

The autonomous hand-off exists because the agent runs its tools on a worker thread that does not carry the request's **tenant** (`TenantContext` `ThreadLocal`, defaults to `public`) or **Spring Security `Authentication`**. The thread is lost at `LocalAgent.runAgent` → `CompletableFuture.runAsync(...)` (ForkJoinPool), so the agent's tools see the wrong tenant schema (`ProjectWorkflow not found`) and no principal.

The interactive path appears to work today only because it leans on those same defaults: dev is single-tenant (`public`) and `ProjectWorkflowFacade.updateWorkflow` has no `@PreAuthorize`. Under real multi-tenancy, interactive edits would silently target the `public` schema. So the store is a workaround for a context-propagation gap that **also latently affects interactive mode**.

## Goal

- Make the `workflow_editor_build` agent's persistence tools work correctly on worker threads for **both** entry points, by rehydrating `{tenant, user}` inside the tool from data carried on the `ToolContext`.
- Delete `WorkflowGenerationSubmissionStore`, the `submitWorkflow` tool, and the poll/persist tail in `CopilotWorkflowGeneratorImpl`.
- Fix the interactive multi-tenant gap as a consequence (same agent, same wrapper).
- Avoid duplicated rehydration logic: one shared wrapper used by both Copilot and AI Hub.

## Non-goals

- No change to the vendored `spring-ag-ui` threading model (no executor/`TaskDecorator`/Reactor-hook surgery). That "transparent propagation" approach (Approach B) is explicitly out of scope; this spec uses Approach A (rehydrate-in-tool).
- No consolidation of `AiHubToolInvocationContext` into `AgentToolInvocationContext` (bridge, don't merge — YAGNI).
- No change to how the LLM decides *what* workflow to build.

## Key idea

The Spring AI `ToolContext` is a **method parameter** (`call(toolInput, toolContext)`), not a `ThreadLocal` — it rides through every async hop intact. So identity travels as **data** in the `ToolContext` and is rehydrated as **context** inside a wrapper immediately before the tool body runs. This is the pattern ByteChef already uses for picker tools (`PropertyOptionsResolver.withUserSecurityContext`) and AI Hub tools (`RehydrateSecurityContextToolCallback`); it has simply never been applied to the workflow-persistence tools, and neither existing variant carries the **tenant**.

## Data flow

```
request thread (has TenantContext + Authentication)
  ├─ interactive: CopilotApiController injects {userId, tenantId} into state
  └─ autonomous:  CopilotWorkflowGeneratorImpl captures {tenantId, userId} into state
                                                                          │
        CopilotToolContextUtils.toToolContext(state)                      ▼
            → AgentToolInvocationContext{ tenantId, userId, workspaceId, environmentId, ... }
                                                                          │  (rides ToolContext across thread hops)
        RehydrateContextToolCallback.call(input, toolContext):           ▼
            TenantContext.callWithTenantId(tenantId, () ->
                securityContextRehydrator.withUserSecurityContext(userId, () ->
                    delegate.call(input, toolContext)))                   →  updateWorkflow persists in the
                                                                              correct tenant schema, with a principal
```

Each half is independent: if `tenantId` is absent the tenant is left as-is; if `userId` is absent the security rehydration is a no-op. The embedded autonomous path typically has a tenant but no platform `User` — acceptable, because the persistence facade requires no `Authentication`.

## Architecture & components

### New (shared, in `ai-copilot-tool`, EE — the module ai-hub already depends on)

- **`RehydrateContextToolCallback`** — a `ToolCallback` decorator that reads `AgentToolInvocationContext.fromToolContext(toolContext)` and wraps `delegate.call(...)` in `TenantContext.callWithTenantId(tenantId, () -> rehydrator.withUserSecurityContext(userId, () -> delegate.call(...)))`.
  - Idempotent (`wrap()` returns the delegate unchanged if already wrapped).
  - `call(String)` (no `ToolContext`) passthrough — transparent for non-AG-UI call sites and tests.
  - Missing tenant → no tenant change; missing user → no security change (fail-closed for any `@PreAuthorize`, never silently bypassed).
- **`SecurityContextRehydrator`** — extract the `userId → User → authorities → SecurityUtils.runAs` logic (currently duplicated in `PropertyOptionsResolver.withUserSecurityContext` and ai-hub's `RehydrateSecurityContextToolCallback`) into one component. `PropertyOptionsResolver` is refactored to delegate to it. `RehydrateContextToolCallback` uses it for the security half.
- **`CopilotToolCallbackWrappers`** (or fold into existing wrapper utility) — applies `RehydrateContextToolCallback` (and any future shared layers) at registration, mirroring `AiHubToolCallbackWrappers`.

### Changed — `ai-copilot-tool`

- **`AgentToolInvocationContext`** — add `@Nullable String tenantId` + `TOOL_CONTEXT_TENANT_ID_KEY = "bytechef.agentTool.tenantId"`; thread it through the constructor, `toToolContext`, and `fromToolContext`.
- **`PropertyOptionsResolver`** — `withUserSecurityContext` delegates to `SecurityContextRehydrator` (no behavior change).
- **`build.gradle.kts`** — add dependency on `:server:libs:core:tenant:tenant-api` (for `TenantContext`).

### Changed — `ai-copilot-service`

- **`CopilotToolContextUtils.toToolContext(state)`** — read `tenantId` from state into the `AgentToolInvocationContext`.
- **`CopilotApiController`** — alongside `injectAuthenticatedUserId`, capture `TenantContext.getCurrentTenantId()` into state (interactive tenant fix).
- **`CopilotWorkflowGeneratorImpl.generateWorkflow`** — capture `{tenantId, userId}` on the request thread and seed into the state map; **remove** the `WorkflowGenerationSubmissionStore.poll(...)` + `workflowService.update(...)` tail. New shape: seed context → `runAgent` → await latch → return uuid (persistence already done by the tool mid-run). The `autonomous` state flag is retained but now means *interaction style only*.
- **`CopilotConfiguration`** — wrap the `workflow_editor_build` agent's tools with `CopilotToolCallbackWrappers.wrap(...)` (convert the `@Tool` beans to `ToolCallback`s via `ToolCallbacks.from(...)` and apply the wrapper). Apply to all build-agent tools (registration-time wrapping, per AI Hub precedent — covers current and future tenant/auth-scoped tools).

### Changed — `prompt_workflow_editor_build.txt`

- Remove the "Autonomous Generation Mode" sub-section's persistence prohibitions: *you CANNOT persist*, *do NOT call `updateWorkflow`/`getWorkflow`/...*, *call `submitWorkflow` exactly once*.
- Keep the behavioral half: *never ask follow-up questions / never wait for confirmation / run straight through*.
- Persistence instruction becomes uniform: build + validate, then `updateWorkflow(workflowId, workflow)` — same in both modes.

### Changed — `ai-hub` (migration onto the shared wrapper)

- **`AiHubToolCallbackWrappers.wrap`** — replace the `RehydrateSecurityContextToolCallback` layer with the shared `RehydrateContextToolCallback`. Keep the `NonEmptyToolCallback` layer.
- Ensure AI Hub populates `AgentToolInvocationContext.{tenantId, userId}` into its agent `ToolContext` (it already carries `userId` via the shared record for component tools; add tenant capture in the AI Hub chat controller mirroring `CopilotApiController`).
- **Delete `RehydrateSecurityContextToolCallback`** (ai-hub-service) once nothing references it. AI Hub gains tenant correctness for free.
- `AiHubToolInvocationContext` is left in place (bridge, not merge).

### Deleted

- **`WorkflowGenerationSubmissionStore`** (ai-copilot-service) and its `submitWorkflow` `@Tool`.
- The submission-store reference from the `workflow_editor_build` agent's tool list in `CopilotConfiguration`.

## Module/boundary notes

- All rehydration lives in EE modules. `ProjectWorkflowTools` (non-EE `automation-ai-tool`, Apache) is **untouched** — no EE dependency leaks into Apache code; wrapping happens at the EE registration layer.
- `ai-copilot-tool` is the single source of truth for the wrapper + rehydrator; ai-hub consumes it (existing dependency direction).
- EE conventions: ByteChef Enterprise license header + `@version ee` on all new/changed EE files.

## Error handling

- Wrapper with missing `tenantId`/`userId` → falls through for that half (never bypasses auth silently). Autonomous always seeds the tenant; the embedded path's missing platform `userId` is a no-op by design.
- `CopilotWorkflowGeneratorImpl` keeps the `CountDownLatch` + 10-minute timeout and the failure propagation. If the agent never calls `updateWorkflow`, the workflow stays the empty shell — same observable outcome as today's "submitted-but-never-polled", minus the dead store entry. (Optional follow-up: post-run assertion that the definition changed.)
- `SecurityContextRehydrator`: user-not-found / authority-resolution failures degrade to "no authorities" and log at debug — preserving `PropertyOptionsResolver`'s current behavior.

## Testing

- **Unit `RehydrateContextToolCallback`** — a probe delegate asserts, inside `call`, that `TenantContext.getCurrentTenantId()` equals the seeded tenant and that the current principal matches the seeded user; plus idempotent-wrap and missing-context-passthrough cases.
- **Unit `SecurityContextRehydrator`** — user→authorities→`runAs`; null user / missing user fallbacks.
- **Unit `AgentToolInvocationContext`** — `toToolContext`/`fromToolContext` round-trip including `tenantId`.
- **Unit `CopilotWorkflowGeneratorImpl`** — seeds `{tenantId, userId}` into state; no store interaction (store deleted).
- **Integration** — invoke the wrapped `updateWorkflow` callback with a `ToolContext` carrying a non-default `tenantId` + a `userId`; assert the row persists in that tenant's schema with a principal set. This exercises the real correctness win without needing a live LLM.
- **AI Hub regression** — existing ai-hub tool-callback tests pass after the wrapper swap; add a tenant-rehydration assertion.

## Risks / open questions

- **AI Hub tenant capture point** — confirm the AI Hub chat controller is the right seam to capture `TenantContext.getCurrentTenantId()` into state (mirror `CopilotApiController`). To pin down in the implementation plan.
- **`ToolCallbacks.from(...)` conversion in `CopilotConfiguration`** — verify the `WorkflowEditorSpringAIAgent` builder accepts pre-wrapped `ToolCallback`s in its `tools(...)` list (Spring AI accepts both bean objects and `ToolCallback`s; confirm during implementation).
- **Per-call cost** — one `fetchUser` (+ authorities) per wrapped tool call on a context-bearing tool; mitigated by `UserService` caching and bounded turn length. Same cost profile AI Hub already accepts.

## Rollout

Single PR on the current EE branch. No DB migration, no API change. Behavior-preserving for the happy path; the user-visible win is multi-tenant correctness and the removal of the store hand-off.
