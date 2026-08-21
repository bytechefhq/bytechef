# Centralize connection/property tools in `ai-copilot-tool` (server) — design

- **Date:** 2026-06-10
- **Branch:** `0_732`
- **Status:** Approved (design); pending implementation plan
- **Author:** Ivica Cardic
- **First of a series.** Follow-ons: (2) client interactive-UI pipeline sharing — gives the Copilot in-editor panel the same pickers; (3) server-side workflow-artifact attach.

## Context & goal

AI Hub's connection/property tools (`listConnectionsForComponent`, `selectConnection`,
`lookupAction/TriggerPropertyOptions`, `selectPropertyOption`/`selectTriggerPropertyOption`)
plus `PropertyOptionsResolver` live in `ai-hub-service` (`com.bytechef.ee.ai.hub.tool`). The
goal is **one shared implementation** so the same functionality can be reused by Copilot (the
in-editor workflow-editor panel) — "centralize behind Copilot." AI Hub already delegates the
workflow *build* to the Copilot `workflow_editor_agent`; this work is about the connection/
property tooling that feeds it.

**Dependency direction makes this clean:** `ai-hub-service` already depends on
`ai-copilot-tool` and `ai-copilot-api`; `ai-copilot-service` does **not** depend on `ai-hub`.
So the shared tools belong in a copilot module and AI Hub consumes them — no inversion.

**This spec is server-only.** Scope: relocate the tools + resolver into `ai-copilot-tool`
behind neutral abstractions, and have AI Hub consume them with **no behavior change**.
Registering the tools on the Copilot panel agent and rendering their pickers in the Copilot
panel is the **follow-on client spec** (so Copilot gets tools + rendering together, never
half-wired). `askUserQuestion` stays in AI Hub (general clarifying tool, not connection/
property).

## Home module

`server/ee/libs/ai/ai-copilot/ai-copilot-tool` (user's choice). It currently depends only on
`ai-api`. Add: `platform-component-api`, `platform-connection-api`, `platform-user-api`,
`platform-security-api` (for `SecurityUtils`), and `automation-configuration-api` (for
`WorkspaceConnectionFacade`). Tradeoff accepted: this couples the lean tool module to the
automation/platform facades. Add matching `testImplementation` service deps mirroring what
`ai-hub-service` uses for these tools' tests.

## Three couplings to break

The tools currently reference three AI-Hub-specific types. Since `ai-copilot-tool` must not
depend on `ai-hub` (would invert the dependency), each is replaced by a neutral abstraction.

### 1. Invocation context → `AgentToolInvocationContext` (in `ai-copilot-api`)

`AiHubToolInvocationContext` (in `ai-hub-api`) carries `workspaceId, userId, sourceOrdinal,
lastUserPrompt, environmentId, threadId`. The tools only use `workspaceId`, `userId`,
`environmentId` (via `resolveEnvironmentOrDefault`). Introduce a neutral record in
`ai-copilot-api` (`com.bytechef.ee.ai.copilot.tool` or `.context`):

```
record AgentToolInvocationContext(
    Long workspaceId, Long userId, Long environmentId, @Nullable String conversationId)
```
with `static @Nullable AgentToolInvocationContext fromToolContext(ToolContext)`,
`int resolveEnvironmentOrDefault()` (default 0), and `Map<String,Object> toToolContext()`
using its own `bytechef.agentTool.*` key namespace. `conversationId` is included now (unused
by these tools) so follow-on (3) can carry the AI-Hub task id without another context change.

- **AI Hub** populates the neutral keys into its agent `toolContext` (alongside or instead of
  the existing AiHub keys), built from its `AiHubToolInvocationContext` — a one-line mapping
  where the AiHub context is assembled.
- **Copilot** populates the same neutral keys from its request/security context (done in the
  follow-on client spec when Copilot registers the tools).

The tools call `AgentToolInvocationContext.fromToolContext(toolContext)` only.
`AiHubToolInvocationContext` stays in `ai-hub-api` for AI Hub's other tools.

### 2. Metrics → `ToolStateVisibilityMetrics` interface

The lookup/select tools call `AiHubToolAttachMetrics.recordStateVisibility(toolName, state)`.
Define a neutral interface in `ai-copilot-tool` (or `ai-copilot-api`):

```
interface ToolStateVisibilityMetrics { void recordStateVisibility(String toolName, String state); }
```
`AiHubToolAttachMetrics` (stays in `ai-hub-service`) implements it. Provide a no-op default
(`ToolStateVisibilityMetrics NOOP = (t, s) -> {};`) for surfaces without metrics. The moved
tools' constructors take `ToolStateVisibilityMetrics`.

### 3. `kind` markers stay identical

`selectConnection` → `select-connection`, `selectPropertyOption`/`selectTriggerPropertyOption`
→ `select-property-option`. Unchanged strings; the client pipeline-sharing follow-on lets the
Copilot panel render them. No server change here.

## What moves (into `ai-copilot-tool`, package `com.bytechef.ee.ai.copilot.tool`)

- `PropertyOptionsResolver` (incl. the `OptionsLookupResult` sealed type + `resolveAction/
  TriggerPropertyOptions` + envelope builders + `withUserSecurityContext` + `topPropertySegment`).
- `ListConnectionsForComponentToolCallback`, `SelectConnectionToolCallback`,
  `LookupActionPropertyOptionsToolCallback`, `LookupTriggerPropertyOptionsToolCallback`,
  `SelectPropertyOptionToolCallback`, `SelectTriggerPropertyOptionToolCallback`
  (+ their nested input/output records).
- Their test classes (`*Test`, `PropertyOptionsResolverTest`) move too, switching fixtures
  from `AiHubToolInvocationContext` to `AgentToolInvocationContext` and from
  `AiHubToolAttachMetrics` to a `ToolStateVisibilityMetrics` mock/no-op.

`ToolErrors` (in `ai-api`) is already shared — no change. `Option`, the facades, and services
are in `platform-component-api` — reachable once the deps are added.

## What AI Hub does after the move

- `AiHubConfiguration` imports the tools from `com.bytechef.ee.ai.copilot.tool` instead of
  `com.bytechef.ee.ai.hub.tool`; constructs them with `AiHubToolAttachMetrics` (which now
  implements `ToolStateVisibilityMetrics`) — registration is otherwise unchanged
  (`registerToolAttachStateVisibilityToolCallbacks` keeps adding the same tool names).
- AI Hub writes the neutral context keys into the agent `toolContext`. The mapping lives on the
  AI Hub side (AI Hub depends on copilot-api, not vice versa): where AI Hub builds/merges its
  `AiHubToolInvocationContext.toToolContext()`, it also constructs
  `new AgentToolInvocationContext(workspaceId, userId, environmentId, threadId)` and merges its
  `toToolContext()`, so both key sets are present and the moved tools find the neutral keys.
- Delete the old tool classes + tests from `ai-hub-service`.

**Behavior must be identical for AI Hub** — the relocated tools + existing AI Hub tests
(`PropertyOptionsToolWiringTest`, the runtime-provider/sidebar tests) are the guard.

## Testing

- Moved unit tests run in `ai-copilot-tool` (green after the context/metrics swap).
- `PropertyOptionsToolWiringTest` (in `ai-hub-service`) still asserts the tool names appear in
  the ASK/BUILD catalogs — unchanged behavior, tools now imported from `ai-copilot-tool`.
- `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-tool:check` and
  `:server:ee:libs:ai:ai-hub:ai-hub-service:check` both green.

## EE conventions

All moved/new files under `server/ee/` keep the ByteChef Enterprise license header +
`@version ee`. The neutral context/metrics get the same.

## Risks / notes

- `ai-copilot-tool` gaining `automation-configuration-api` couples copilot-tool to the
  automation package — accepted per the home-module decision; flag if it causes a cycle at
  build time (it shouldn't: automation-configuration-api doesn't depend on ai-copilot).
- The neutral context must cover every field the moved tools read; the per-tool audit
  (workspaceId/userId/environmentId only) confirms it does.
- Copilot's own population of the neutral context + the picker rendering is **deferred** to the
  client follow-on; until then the tools are simply shared code that only AI Hub invokes.

## Open questions

None for the server piece. Follow-on specs: (2) client interactive-UI pipeline sharing for the
Copilot panel; (3) server-side workflow-artifact attach using `conversationId` from the neutral
context.
