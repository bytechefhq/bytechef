# Embedded Copilot — Restrict Workflow Generation to Enabled Integrations

**Date:** 2026-05-30
**Status:** Approved (design)
**Scope:** Enterprise Edition, embedded (iPaaS) path only

## Problem

When a connected user generates a workflow through the embedded AI copilot
(`AutomationWorkflowProjectFacadeImpl.generateProjectWorkflow`), the copilot can
currently wire up **any** of the 180+ platform components, regardless of whether
the connected user has actually enabled the corresponding integration. A generated
workflow may therefore reference integrations the user has never configured and that
cannot run (no enabled connection).

We want the embedded copilot to only consider **active, enabled integrations** —
i.e. integrations whose `IntegrationInstanceConfiguration` has `enabled == true` in
the relevant environment — plus connection-less built-in/utility components that
every workflow needs as glue.

## Decisions (locked during brainstorming)

1. **Filter set = strictly `enabled == true` IICs**, scoped to the call's `environment`.
2. **Connection-less built-in/utility components are always available** (never filtered) —
   e.g. control-flow task dispatchers, logger, data mappers, manual trigger, scripts.
3. **Scope = the embedded `generateProjectWorkflow` path only.** No change to the
   in-product (non-embedded) copilot or to the MCP server.
4. **Strictness = discovery-only (soft).** Filter the copilot's discovery tools so the
   LLM only ever *sees* enabled integrations + built-ins. We do **not** hard-reject at
   task/trigger creation. Disallowed components are simply never surfaced.
5. **Empty allow-list semantics = strict.** If a connected user has zero enabled IICs,
   the allow-list still contains the connection-less built-ins, so copilot can only
   assemble built-in/control-flow steps. We do **not** fall back to "all components".
   Note: because the embedded facade always unions in the connection-less built-ins, the
   set it passes is **never empty** — so the "absent/empty key ⇒ no filtering" opt-out
   below is only ever reachable by a deliberate non-embedded caller passing null/empty,
   never by the embedded path.

## Background: domain model

- **`Integration`** (embedded) wraps exactly one component: it carries `componentName`
  + `componentVersion`. One Integration ≈ one component (e.g. "Slack").
- **`IntegrationInstanceConfiguration`** (IIC) is an environment-scoped, configured
  instance of an Integration. The `enabled` boolean lives here (not on `Integration`),
  alongside `environment` (int ordinal) and `integrationId`
  (`AggregateReference<Integration, Long>`).
- So "active enabled integrations" = IICs where `enabled == true` for the given
  `environment`; the component each authorizes is reached via
  `IIC.integrationId → Integration.componentName`.

`IntegrationInstanceConfigurationService` already exposes the exact query needed:
`getIntegrationInstanceConfigurations(Environment environment, boolean enabled)`.

## Background: how per-run context reaches copilot tools

- `CopilotWorkflowGeneratorImpl.generateWorkflow` builds a per-invocation
  `com.agui.core.state.State` map and passes it via `RunAgentParameters` to
  `LocalAgent.runAgent(parameters, subscriber)`.
- Tool execution runs **async on a worker thread** (`LocalAgent.runAgent` →
  `CompletableFuture.runAsync`), so a `ThreadLocal` would not survive the thread hop.
- The idiomatic mechanism is Spring AI's **`ToolContext`**: a `*SpringAIAgent` overrides
  `toolContext(RunAgentInput input)` to turn per-run `State` into a context map, which
  Spring AI hands to any `@Tool` method that declares a `ToolContext` parameter.
  `AiHubSpringAIAgent` already does this in production.
- The embedded build agent (`workflow_editor_build`) exposes the tools
  `projectTools, projectWorkflowTools, taskTools, scriptTools`. `ComponentTools` is **not**
  directly exposed; the only component-discovery surface is **`TaskTools`**
  (`listTasks` / `searchTasks`), which delegate to
  `componentTools.listActions/listTriggers/searchActions/searchTriggers` and to
  `taskDispatcherTools` for control-flow.

## Approach (chosen: A)

Filter at the single `TaskTools` discovery chokepoint, with the **complete** allow-list
(enabled integration components ∪ connection-less components) computed in the EE/embedded
facade and threaded down via per-run `State` → `ToolContext`. `TaskTools` performs a pure
set-membership filter and never reasons about connections — the "connection-less always
available" rule is encoded as data, not branching logic in shared platform code.

Rejected alternatives:
- **B** (allow-list = enabled integrations only; `TaskTools` independently keeps
  connection-less components) — pushes connection policy into shared platform discovery
  code and adds a `ComponentDefinitionService` dependency there.
- **C** (filter inside `ComponentTools`) — changes 4+ platform method signatures and all
  their callers for a feature scoped to embedded copilot only; over-broad blast radius.

### Why this is safe to add to shared platform code

The filter is **opt-in by presence of the toolContext key**. Every existing caller of
`TaskTools` (MCP server, in-product copilot) passes no `allowedComponentNames` key, so the
filter is a no-op for them — zero behavior change outside the embedded path.

## Data flow

```
AutomationWorkflowProjectFacadeImpl.generateProjectWorkflow(externalUserId, prompt, environment)
  │  resolve Set<String> allowedComponentNames =
  │     componentNames of enabled IICs (environment)  ∪  componentNames of connection-less components
  ▼
CopilotWorkflowGenerator.generateWorkflow(workflowId, prompt, allowedComponentNames)
  │  put "allowedComponentNames" into per-run State
  ▼
WorkflowEditorSpringAIAgent.toolContext(RunAgentInput)   (NEW override)
  │  copy allowedComponentNames from State into the ToolContext map
  ▼
TaskTools.listTasks(..., ToolContext) / searchTasks(..., ToolContext)
     when key present & non-empty: drop actions/triggers whose componentName ∉ allow-list;
     task dispatchers always kept; key absent → unchanged
```

## Components to change

### 1. `AutomationWorkflowProjectFacadeImpl` (EE embedded — allow-list resolution)
- Inject `IntegrationInstanceConfigurationService` and `IntegrationService`.
- In `generateProjectWorkflow`:
  - `enabled = integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(environment, true)`
  - distinct `integrationId`s → `integrationService.getIntegrations(ids)` →
    collect `componentName`s.
  - connection-less: `componentDefinitionService.getComponentDefinitions()` filtered to
    those with no connection → `componentName`s. (`componentDefinitionService` is already
    injected.)
  - `Set<String> allowedComponentNames = enabledNames ∪ connectionlessNames`.
  - pass into `copilotWorkflowGenerator.generateWorkflow(workflowId, prompt, allowedComponentNames)`.

### 2. `CopilotWorkflowGenerator` (ai-copilot-api) + `CopilotWorkflowGeneratorImpl` (ai-copilot-service)
- Extend the interface to
  `generateWorkflow(String workflowId, String prompt, Set<String> allowedComponentNames)`.
- The impl puts the set into the per-run `State` under the key constant (see §5).
- Null/empty allow-list ⇒ the State key is omitted ⇒ downstream filter is a no-op (this is
  how any non-embedded caller, should one appear, opts out).

### 3. `WorkflowEditorSpringAIAgent` (ai-copilot-service — ToolContext bridge)
- Add an override of `toolContext(RunAgentInput input)` (the agent's first use of it),
  mirroring `AiHubSpringAIAgent`: read `allowedComponentNames` from `input.state()` and, if
  present & non-empty, place it into the returned context map under the shared key constant.

### 4. `TaskTools` (mcp-tool-platform — filter chokepoint)
- Add a `ToolContext` parameter to `listTasks` and `searchTasks`.
- Helper: read the allow-list `Set<String>` from the `ToolContext` map under the shared key.
- When present & non-empty: filter the action results and trigger results to those whose
  `componentName` is in the set. Task dispatcher results are always retained.
- When absent/empty: behavior unchanged.

### 5. Shared key constant
- A single string constant (e.g. `"allowedComponentNames"`) used as both the `State` key and
  the `ToolContext` key. It must be referenceable from both the EE agent module and the
  Apache-licensed `mcp-tool-platform` module. Define it on the platform side
  (`mcp-tool-platform`, alongside `TaskTools`/`ComponentTools`) and reference it from the EE
  generator/agent.

## Detail to verify during planning (not a design risk)

`ActionMinimalInfo` / `TriggerMinimalInfo` must expose the `componentName` used for the
membership check. If they only carry the composite `type` string
(`component/vN/name`), the filter parses the component prefix instead. This is an
implementation detail to confirm, not an architectural decision.

## Testing

- **Unit — `TaskTools` filtering:** with a `ToolContext` carrying an allow-list,
  `listTasks(ACTION)` / `searchTasks` return only allowed actions/triggers; task dispatchers
  always present; absent key ⇒ unchanged full list. (Component test naming: `TaskToolsTest`.)
- **Unit — allow-list resolution in the facade:** given mocked enabled IICs +
  `ComponentDefinitionService`, the produced set is `enabled integration componentNames ∪
  connection-less componentNames`; disabled IICs excluded; wrong-environment IICs excluded;
  zero enabled IICs ⇒ set contains only connection-less names.
- **Generator/agent wiring:** `generateWorkflow` places the set into `State`; the
  `toolContext` override surfaces it; empty/null ⇒ key omitted.
- Existing non-embedded `TaskTools` callers remain green (no key ⇒ no filtering).

## Out of scope

- Hard enforcement at task/trigger creation (rejecting disallowed components) — discovery-only
  by decision #4.
- The in-product (non-embedded) copilot and the standalone MCP server.
- Per-connected-user connection availability beyond the IIC `enabled` flag.
