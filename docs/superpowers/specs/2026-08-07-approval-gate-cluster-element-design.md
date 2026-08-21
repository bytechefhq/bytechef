# Approval gate as a cluster element

Date: 2026-08-07
Status: approved, not implemented

## Problem

The AI agent's per-tool approval gate is configured by a hidden boolean. A TOOLS cluster element whose
parameters carry `requiresApproval: true` gets wrapped in `ApprovalGateToolCallback`, which delivers an
approval request and suspends the run instead of executing. The flag is set from a checkbox in the tool
row's `⋮` menu inside the AI Agent configuration panel — it is not a declared property, so it never
appears in the Properties tab, and it is invisible on the canvas.

Three consequences:

- **Undiscoverable.** Nothing about a gated tool looks different anywhere except one dropdown menu.
- **Channels sit on the wrong node.** `APPROVAL_CHANNELS` is declared on the AI Agent root, so one
  channel list covers every gated tool. Routing a destructive action to Slack and a routine one to chat
  is unexpressible.
- **The agent module carries a special case.** `getToolCallbacks` branches on the flag, threads an
  `approvalChannelClusterElements` parameter, and resolves a per-tool expiry from parameters.

## Solution

Replace the flag with an explicit `approvalGate` cluster element on the `aiAgentUtils` component,
carrying two child cluster element types: `TOOLS` (the tools it gates) and `APPROVAL_CHANNELS` (where
requests are delivered). Gating becomes a visible structure on the canvas, channels become local to the
gate that uses them, and `APPROVAL_CHANNELS` comes off the AI Agent root.

### Structure

```
MOVE     AiAgentToolFacade            ai/agent → ai/llm
MOVE     ApprovalGateToolCallback     ai/agent → ai/agent/utils
NEW      ClusterElementToolCallbacks  ai/llm
NEW      AiAgentUtilsApprovalGate     ai/agent/utils
DELETE   agent-utils → components:ai:agent
RESULT   components:ai:agent has no dependents

aiAgentUtils  clusterElementTypes: [MODEL, SUBAGENT, TOOLS, APPROVAL_CHANNELS]
  approvalGate  → [TOOLS, APPROVAL_CHANNELS]     (new)
  taskTool      → [MODEL, SUBAGENT]
  subagent      → [MODEL, TOOLS]
  everything else → []

aiAgent  clusterElementTypes: [MODEL, CHAT_MEMORY, RAG, GUARDRAILS, TOOLS]
                                                    APPROVAL_CHANNELS removed
```

An agent may carry several gates. Each owns its channels and expiry, so different tools can have
different approval policies:

```
AI Agent
└─ Tools
   ├─ searchDocs                (ungated)
   ├─ approvalGate "Destructive"
   │  ├─ Tools    → deleteRecord, dropTable
   │  └─ Channels → Slack, Gmail
   └─ approvalGate "Outbound"
      ├─ Tools    → sendEmail
      └─ Channels → chat
```

`approvalGate` declares two properties: a **name**, used in the approval card title and as the panel's
group header, and **expiry** (`approvalExpiresIn` + `approvalExpiresInUnit`), moved off per-tool
parameters and keeping the current 60-day default.

`ToolConstants.REQUIRES_APPROVAL` is deleted — nothing sets or reads it once the gate is structural.
`APPROVAL_EXPIRES_IN`, `APPROVAL_EXPIRES_IN_UNIT` and `APPROVAL_EXPIRES_IN_UNIT_HOURS` survive as the
gate's own property names, so the stored key names are unchanged; only their location moves, from a
gated tool's parameters to the gate's.

Removing `APPROVAL_CHANNELS` from the agent root is safe: it has exactly one reader,
`AbstractAiAgentChatAction:308`, which feeds the gate and nothing else.

## Prerequisite: extract the tool-callback dispatch

The gate must build its children's callbacks. That dispatch — resolve the cluster element function, then
branch on its shape — exists twice today: private as `AbstractAiAgentChatAction.buildElementToolCallbacks`,
and copied into `AiAgentUtilsTaskTool.buildSubagentToolCallbacks`. The gate would be a third copy.

The copies are not equivalent. The task tool's version is a **degraded** copy, and the divergence is a
live bug:

| dispatch branch | `AbstractAiAgentChatAction` | `AiAgentUtilsTaskTool` |
|---|---|---|
| `MultipleConnectionsToolCallbackProviderFunction` | connection params via `getConnectionParameters(...)` | `ParametersFactory.create(Map.of())` — empty |
| `ToolCallbackProviderFunction` | handled | **missing** — falls to `else` |
| `MultipleConnectionsToolFunction` | facade, map overload | facade, map overload |
| plain function tool | facade, single-connection overload | facade, map overload |
| failure | `clusterElementInitializationException(...)` | bare `IllegalStateException` |

Ten of the thirteen `aiAgentUtils` tool elements are `ToolCallbackProviderFunction`: `autoMemoryTool`,
`agentClientTool`, `askUserQuestionTool`, `braveWebSearchTool`, `globTool`, `grepTool`,
`fileSystemTools`, `listDirectoryTool`, `shellTools`, `todoWriteTool`. Attaching any of them under a
subagent today lands in the `else` branch, which calls `AiAgentToolFacade.getFunctionToolCallback(...)`.
That method builds a `FunctionToolCallback` from the element's parameters and a `fromAi` input schema —
it never invokes the provider's `apply()`. The subagent receives one malformed tool with an empty schema
instead of the real ones, and fails quietly.

Extraction is therefore a bug fix, not only de-duplication. The agent's version is a strict superset, and
both call sites already pass exactly the four values the shared signature needs:

```java
List<ToolCallback> build(
    ClusterElement clusterElement,
    Map<String, ComponentConnection> componentConnections,
    boolean editorEnvironment,
    ActionContext context)
```

`clusterElementInitializationException` (`AbstractAiAgentChatAction:346-360`) moves with it as a private
method, which also upgrades the task tool's bare `IllegalStateException` to a message naming component
and version.

### Where it lives

`ai/llm` — `server/libs/modules/components/ai/llm`. Despite its path, it is a shared library, not a
component: it declares no `ComponentHandler` and is already consumed by platform modules, EE modules,
apps and config. `AiAgentToolFacade` moves there too, since it already extends
`com.bytechef.platform.ai.tool.facade.AbstractToolFacade` and depends on nothing from any component
module.

That move deletes the component→component dependency. `agent-utils` reaches into the agent module for
exactly one symbol, in both main and test sources:

```
import com.bytechef.component.ai.agent.facade.AiAgentToolFacade;
```

**Verifiable outcome:** after the move,
`grep -r "com\.bytechef\.component\.ai\.agent\." server/libs/modules/components/ai/agent/utils/src | grep -v "\.agent\.utils"`
returns nothing, and `implementation(project(":server:libs:modules:components:ai:agent"))` is removed
from `agent-utils`' build file. `components:ai:agent` then has no dependents anywhere in the repo.

`agent-utils` does not declare `ai:llm` itself. Shared agent-family dependencies move into a
`subprojects` block in `ai/agent/build.gradle.kts`, matching how `components/build.gradle.kts`,
`components/ai/build.gradle.kts` and `ai/llm/build.gradle.kts` already propagate dependencies. A plain
`dependencies` block does not reach child projects; only `subprojects`/`allprojects` do.

```kotlin
dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:evaluator:evaluator-api"))
    implementation(project(":server:libs:platform:platform-tool-execution:platform-tool-execution-api"))
}

subprojects {
    dependencies {
        implementation(project(":server:libs:modules:components:ai:llm"))
    }
}
```

This reaches every agent descendant — `utils`, `guardrails`, and the sixteen `chat-memory-*` modules.
It is `implementation`-scoped and introduces no cycle, so the extra reach is harmless.

`ai/llm/build.gradle.kts` gains what the facade needs and does not already have: `evaluator-api`,
`platform-component-api`, `platform-configuration-api`, `spring-context`.

`ClusterElementToolCallbacks` is a plain collaborator, not a Spring bean. Both consumers already hold
`AiAgentToolFacade` and `ClusterElementDefinitionService` as constructor-injected fields, so each
constructs it once in its canonical constructor. Making it a bean would mean threading a new parameter
through `AbstractAiAgentChatAction`'s four constructor overloads and both component handlers.

## Runtime behaviour

`AiAgentUtilsApprovalGate` is a `MultipleConnectionsToolCallbackProviderFunction`, the same shape as
`taskTool`:

```java
apply(inputParameters, connectionParameters, extensions, componentConnections, context) {
    ClusterElementMap gateMap = ClusterElementMap.of(extensions);

    List<ClusterElement> channels = gateMap.getClusterElements(APPROVAL_CHANNELS);
    Duration expiry = resolveExpiry(inputParameters);

    return ToolCallbackProvider.from(
        gateMap.getClusterElements(TOOLS)
            .stream()
            .flatMap(el -> clusterElementToolCallbacks
                .build(el, componentConnections, editorEnvironment, actionContext).stream())
            .map(cb -> new ApprovalGateToolCallback(
                cb, channels, componentConnections, clusterElementDefinitionService,
                actionContext, toolExecutionRecorder, expiry))
            .toList());
}
```

`ApprovalGateToolCallback` moves from `ai/agent` to `ai/agent/utils` with the gate.

The agent never learns gates exist. `AbstractAiAgentChatAction.getToolCallbacks` loses the
`requiresApproval` branch, the `approvalChannelClusterElements` parameter, `getApprovalExpiry` and
`isSuspendingApprovalTool`. It builds TOOLS children as before; some arrive pre-wrapped.

**Wrapper ordering is preserved without effort.** Simulation and observable wrapping already run over the
flattened callback list *after* the per-element loop, so the existing "gate inside the observable
wrapper" invariant — which the audit trail depends on — continues to hold.

**Suspend and resume cross the module boundary as a constants protocol.** The gate (agent-utils) writes
`GATED_TOOL_NAME`, `GATED_TOOL_INPUT` and `formUrl` into the suspend's continueParameters and returns
`SUSPENDED_SENTINEL`. `AbstractAiAgentChatAction` (agent) reads the same `ToolSuspendConstants` on
resume. Both modules depend on `platform-ai-api`, so neither references a class in the other.

**Gates are rejected under a subagent.** `buildSubagentToolCallbacks` throws when a child's cluster
element name is `approvalGate`: *"Approval gates cannot be attached to a subagent — a suspended subagent
run cannot be resumed."*

The reason is structural. `buildSubagentToolCallbacks` passes the *agent's* `ActionContext`, so a gate
under a subagent would call `actionContext.suspend(...)` and return `SUSPENDED_SENTINEL` into the
subagent's private `ChatClient`, which treats it as an ordinary string result and continues. The parent's
`SuspendableToolCallingManager` would then search for that sentinel among the parent's tool responses and
not find it, leaving an orphaned suspend. Supporting this properly requires checkpointing and restoring
subagent conversation state — a separate design.

**`requestApproval` is rejected as a gate child**, preserving today's `isSuspendingApprovalTool` guard.
Gating a suspending tool would deliver two approval requests and strand the run.

## Client

The canvas needs no new mechanism: `approvalGate` is a nested cluster root exactly like `taskTool`.
Depth is unchanged — gate at depth 1, gated tools at depth 2, matching taskTool→subagent. A gated tool
that is itself a nested root (`smartWebFetchTool → MODEL`) reaches depth 3, the same territory
`subagent → MODEL` already opened.

Adding a gate needs no new affordance: it is a TOOLS element on `aiAgentUtils`, so it appears in the
existing "Add Tool" popover.

`AiAgentTools` renders gates as groups:

```
Tools this agent can use:                    [+ Add Tool]

  🔍 searchDocs                                    ⋮
  🛡 Destructive                          (gate)   ⋮   [+]
     └ 🗑 deleteRecord                              ⋮
     └ 🗑 dropTable                                 ⋮
     Channels: Slack, Gmail · expires 4h      [+ Channel]
```

The group's `[+]` and `[+ Channel]` reuse `WorkflowNodesPopoverMenu` with `sourceNodeId` set to the
gate's `workflowNodeName` and `clusterElementType` of `tools` / `approvalChannels` — the same component
that powers the top-level "Add Tool".

| File | Change |
|---|---|
| `useAiAgentTools.ts` | `ToolItemI` drops `requiresApproval`, `approvalExpiresIn`, `approvalExpiresInUnit`; returns groups plus ungated tools |
| `AiAgentTools.tsx` | renders groups with per-group add buttons |
| `AiAgentToolDropdownMenu.tsx` | drops the "Requires approval" checkbox, the expiry submenu, `APPROVAL_EXPIRY_PRESETS`, `isPresetSelected` |
| `useAiAgentToolDropdownMenu.tsx` | deletes `handleToggleRequiresApproval` and `handleSetApprovalExpiry` |

A gate row's `⋮` keeps *Configure* (name, expiry) and *Remove*.

Rendering gates in the simple editor is load-bearing rather than cosmetic. `showAiAgentEditor` defaults
to true, so the panel is what most builders see; leaving gates to the canvas alone would reproduce the
discoverability failure this design exists to fix.

## Validation and error handling

`WorkflowValidator.hasGatedTool` currently scans `clusterElements.tools[].parameters.requiresApproval`;
it becomes "any TOOLS child named `approvalGate`". `getApprovalChannelTypes` currently reads the task's
top-level `approvalChannels`; it becomes per-gate. The warning text is unchanged except that it names the
gate. The validator remains advisory — it runs through the GraphQL/copilot path, not on save.

| Condition | Behaviour |
|---|---|
| Gate with no channels | chat fallback, plus the validator warning |
| Gate under a subagent | throw, naming the constraint |
| `requestApproval` under a gate | throw |
| Gate with no tools | no-op: contributes zero callbacks, no error |
| Channel delivery, some fail | best-effort, unchanged |
| Channel delivery, all fail | throw, unchanged |

The empty-channel fallback is deliberately unchanged from today's behaviour. The gate's Channels
placeholder is now visible on the canvas, so the fallback is discoverable rather than hidden, and a gate
remains useful the moment it is dropped onto the canvas.

## Migration

None. `ApprovalGateToolCallback` and `ToolConstants.REQUIRES_APPROVAL` are both absent from `v1.1.5`, so
no stored workflow definition carries `requiresApproval: true`. This is the same unreleased window that
allowed the chat approval channel to move off the `approval` component.

## Testing

Three independently reviewable commits:

1. **Seam extraction.** `ClusterElementToolCallbacksTest` covering all four dispatch shapes, plus a
   regression test proving a `ToolCallbackProviderFunction` tool under a subagent now yields real
   callbacks. This is a bug fix and should be reviewable on its own.
2. **Facade move and build files.** Verified by the grep assertion above and the deletion of the
   `components:ai:agent` dependency.
3. **The gate.** `AiAgentUtilsApprovalGateTest` — each child wrapped exactly once, per-gate channels and
   expiry reaching the callback, subagent and `requestApproval` rejections. Regenerated
   `ai-agent_v1.json` and `ai_agent-utils_v1.json` snapshots. Updated
   `WorkflowValidatorChatApprovalChannelTest`. The existing `ApprovalGateToolCallbackTest` and
   `AiAgentStreamChatActionResumeGateIntTest` stay green, proving the suspend→resume protocol survived
   the module split.

## Decisions

| Question | Decision | Why |
|---|---|---|
| One gate per agent or many? | Many | Per-gate channels and expiry; different policies for destructive and routine tools |
| Gate with no channels? | Chat fallback plus validator warning | Unchanged runtime contract; the empty placeholder is now visible |
| Panel rendering? | Nested groups | The panel is the default view; canvas-only gating would stay undiscoverable |
| Where does the shared dispatch live? | `ai/llm`, plain collaborator | No component→component edge; both consumers already hold its two dependencies |
| Gate under a subagent? | Rejected, fail fast | The sentinel would be lost in the subagent's own `ChatClient` and the suspend orphaned |
| Gate on `aiAgent` or `aiAgentUtils`? | `aiAgentUtils` | The gate is one of the agent's attachable tools, and it lives beside `taskTool`, which already nests. The agent module ends up simpler — no gate special case, no gate class — and, once the facade moves to `ai/llm`, no dependents at all |

## Out of scope

- **`agent-utils` → `components:script`.** A second component→component dependency, for `ScriptConstants`
  and `PolyglotEngine`. Unlike `ai:llm`, `script` is a real component with a `ScriptComponentHandler`,
  and CLAUDE.md names `platform-component-polyglot` as the CE home for shared polyglot machinery. Its own
  ticket.
- **Gating inside subagents.** Requires a subagent conversation checkpoint and resume protocol.
- **Depth-3 canvas layout.** `subagent → MODEL` already reaches depth 3; ELK layout and edge routing at
  that depth are unverified and need eyes on the canvas independently of this design.
