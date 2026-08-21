# Interactive connection/property pickers in the Copilot panel — design

- **Date:** 2026-06-10
- **Branch:** `0_732`
- **Status:** Approved (design); pending implementation plan
- **Author:** Ivica Cardic
- **Spec #2 of 3.** Builds on the shipped #1 (shared tools in `ai-copilot-tool`). Follow-on #3: server-side workflow-artifact attach.

## Goal

Give the in-editor **Copilot panel** the same interactive pickers AI Hub has — `selectConnection`
and `selectPropertyOption`/`selectTriggerPropertyOption` render in the Copilot chat thread, the
user picks, and the choice returns as their next message. Today the Copilot panel is text-only.

## What's already true (verified)

- Both surfaces stream over the **same AG-UI stack** (`@ag-ui/client` `HttpAgent` + `AgentSubscriber`).
  AI Hub's `AiHubRuntimeProvider` subscriber intercepts tool-result events and converts the
  `select-*` markers into `data-*` message parts; `AiHubMessageContent` maps each `kind` to a
  renderer. **Copilot's `CopilotRuntimeProvider` subscriber only handles text events**, and
  `CopilotPanel` uses the **generic** `<Thread>` (`@/components/assistant-ui/thread`) with no
  data-part component registry.
- The Copilot panel's agent is a `SpringAIAgent` (`WorkflowEditorSpringAIAgent` in
  `ai-copilot-service`) that registers tool callbacks and streams via AG-UI — so it **can**
  register the shared tools and populate the neutral context in a `toolContext()` override, exactly
  like `AiHubSpringAIAgent`.
- The shared tools + `PropertyOptionsResolver` + `AgentToolInvocationContext` +
  `ToolStateVisibilityMetrics` already live in `ai-copilot-tool` (spec #1).

## Scope

- **Server:** only `WorkflowEditorSpringAIAgent` gains the interactive tools (the build/edit agent
  where connections/channels matter). Cluster-element / code-editor agents unchanged.
- **Client:** extract AI Hub's **entire** interactive-chat-UI pipeline into a new **shared `ai-chat`
  module** — all data-part renderers, the common stores, the interception mapper, and the registry.
  AI Hub refactors to consume it (behavior-preserving); Copilot wires it in.
- **Data-part kinds shared (all of them):** `select-connection`, `select-property-option`,
  `ask-user-question`, `create-connection`, `run-error`. (Per the user's call, the whole pipeline is
  shared, not just the connection/property pickers.)
- **Tools that must also move to `ai-copilot-tool`** (so the Copilot editor agent can emit the
  markers): in addition to the connection/property tools moved in spec #1, **`askUserQuestion`
  (`AskUserQuestionToolCallback`) and `createConnection` (`CreateConnectionToolCallback`) relocate to
  `ai-copilot-tool`** behind the same neutral context/metrics. (`run-error` is a RUN_ERROR event, not
  a tool — no relocation; just shared rendering.)
- **Common stores:** `aiHubToolCallStore` → `aiChatToolCallStore`, `aiHubRetryableErrorStore` →
  `aiChatRetryableErrorStore`, and `useAiHubAskedQuestionsStore` → `useAiChatAskedQuestionsStore`
  (needed by the ask-user-question renderer) move into the shared module; AI Hub + Copilot both use
  them. Any other store the moved renderers read moves too (audit during planning).

## Server design (`ai-copilot-service` + `ai-copilot-tool`)

**Relocate two more tools into `ai-copilot-tool`** (extends spec #1): `AskUserQuestionToolCallback`
and `CreateConnectionToolCallback` move from `ai-hub-service` (`com.bytechef.ee.ai.hub.tool`) to
`com.bytechef.ee.ai.copilot.tool`, swapping `AiHubToolInvocationContext`→`AgentToolInvocationContext`
and `AiHubToolAttachMetrics`→`ToolStateVisibilityMetrics` (same mechanical transform as spec #1). AI
Hub re-imports them from the new package (behavior-preserving; AI Hub tests are the guard). Their
deps (connection facades for createConnection; metrics/jsonMapper for askUserQuestion) are already on
`ai-copilot-tool` from spec #1 (verify; add if missing).

`WorkflowEditorSpringAIAgent`:
1. Register the shared tool callbacks in its catalog — `listConnectionsForComponent`,
   `selectConnection`, `lookupActionPropertyOptions`, `lookupTriggerPropertyOptions`,
   `selectPropertyOption`, `selectTriggerPropertyOption`, **`askUserQuestion`, `createConnection`**
   (all from `ai-copilot-tool`, with the facades/services it can reach + a
   `ToolStateVisibilityMetrics` — `NOOP` unless Copilot has a metrics bean) + the shared
   `PropertyOptionsResolver` bean.
2. Override `toolContext(RunAgentInput)` to populate `AgentToolInvocationContext`
   (`workspaceId`, `userId`, `environmentId`, optional `conversationId`) from the Copilot request's
   verified workspace/user/environment — merged with whatever keys it already sets. Mirror
   `AiHubSpringAIAgent.buildInvocationContext` (audit the verified-value source during planning).
3. Prompt note: tell the editor agent to use the pickers (`selectConnection` /
   `selectPropertyOption`) and `askUserQuestion` when building via chat, so it actually invokes them.

If `PropertyOptionsResolver`/`ToolStateVisibilityMetrics`/the relocated tool beans aren't
component-scanned into the copilot app context, wire them explicitly.

## Client design — new shared `ai-chat` module

New module `client/src/shared/components/ai-chat/` (repo convention for cross-feature UI is
`src/shared/components/...`). Move and de-`AiHub`-prefix the **full** set:

- `messages/SelectConnectionMessage.tsx` (from `ai-hub/connect/AiHubSelectConnectionMessage.tsx`)
- `messages/SelectPropertyOptionMessage.tsx` (from `ai-hub/messages/AiHubSelectPropertyOptionMessage.tsx`)
- `messages/AskUserQuestionMessage.tsx` (from `ai-hub/messages/AiHubAskUserQuestionMessage.tsx`)
- `messages/CreateConnectionMessage.tsx` (from `ai-hub/connect/AiHubCreateConnectionMessage.tsx`)
- `messages/RunErrorMessage.tsx` (from `ai-hub/messages/AiHubRunErrorMessage.tsx`)

These depend on `useThreadRuntime` + `data` (+ shared workspace/connection queries for the connection
picker, + the asked-questions store for ask-user-question). Keep their `*DataI` interfaces exported.

**Common stores** move into `ai-chat/stores/` and are renamed:
- `aiHubToolCallStore` → `aiChatToolCallStore` (`useAiChatToolCallStore`)
- `aiHubRetryableErrorStore` → `aiChatRetryableErrorStore` (`useAiChatRetryableErrorStore`)
- `useAiHubAskedQuestionsStore` → `useAiChatAskedQuestionsStore` (+ its `aiChatAskedQuestionsStore`)
Update every AI Hub reference to the renamed stores (broad rename — `AiHubRuntimeProvider`,
`AiHubMessageContent`, the renderers, tests). Copilot then imports the same stores.

Add two shared helpers:
- `messages/toolResultDataParts.ts` — `toToolResultDataPart(toolCallName, eventContent)` returning a
  discriminated result: `{ok: true, type, data}` for `selectConnection`, `selectPropertyOption`,
  `selectTriggerPropertyOption`, `askUserQuestion`, `createConnection`; or
  `{ok: false, toolName, errorMessage}` on a malformed payload. This is the parse+validate logic now
  inline in `AiHubRuntimeProvider`'s subscriber branches. Each provider applies the result with the
  **common** `aiChatRetryableErrorStore`/`aiChatToolCallStore`.
- `messages/aiChatDataComponents.ts` — the shared `by_name` registry mapping ALL kinds
  (`select-connection`, `select-property-option`, `ask-user-question`, `create-connection`,
  `run-error`) to the moved renderers.

### AI Hub refactor (behavior-preserving)

- `AiHubRuntimeProvider`'s tool-result branches call `toToolResultDataPart(...)` and apply the result
  via the common stores; the `addMessage(data-*)` shape and the RUN_ERROR→`data-run-error` path are
  unchanged.
- `AiHubMessageContent`'s `data.by_name` registry IS the shared registry (all kinds now live there).
  Existing AI Hub message tests are the guard.

### Copilot wiring

- `CopilotRuntimeProvider`'s subscriber gains an `onToolCallResultEvent` handler that calls
  `toToolResultDataPart(...)` and, on `ok`, `addMessage`es the `data-*` part (mirroring AI Hub's
  `addMessage`). (Confirm the AG-UI subscriber hook name Copilot uses for tool results; AI Hub's
  provider shows the exact hook.)
- The generic `<Thread>` (`@/components/assistant-ui/thread`) gains an **optional**
  `dataComponents?: Record<string, ComponentType>` prop, wired into its `ThreadMessage`'s
  `MessagePrimitive.Parts components={{data: {by_name: dataComponents}}}`. `CopilotPanel` passes the
  shared registry; existing `<Thread>` callers pass nothing (no behavior change).

## Testing

- **Client:** the moved renderers keep their existing tests (now in the shared module). New unit test
  for `toToolResultDataPart` (ok + malformed for each kind). AI Hub message tests stay green
  (behavior guard for the refactor). A Copilot-side test that a `select-property-option` tool result
  renders the picker in the Copilot thread (mirroring the AI Hub renderer test).
- **Server:** a wiring test that `WorkflowEditorSpringAIAgent`'s catalog contains the shared tool
  names, and that its `toolContext()` emits the neutral `bytechef.agentTool.*` keys.

## Risks / open questions (resolve during planning)

- **Store-rename blast radius** — renaming `aiHubToolCallStore`/`aiHubRetryableErrorStore`/
  `useAiHubAskedQuestionsStore` to the common names touches many AI Hub files + tests. It's a
  mechanical rename, but large; the AI Hub test suite is the behavior guard. (Resolved the earlier
  "Copilot error wiring" question — Copilot now uses the same common stores.)
- **`WorkflowEditorSpringAIAgent` workspace/user resolution** — confirm it has verified workspace/user
  to populate the neutral context (it resolves them for its existing tools; audit the exact source).
- **Relocating `askUserQuestion`/`createConnection` tools** — like spec #1's moves, confirm their only
  AI-Hub couplings are the context + metrics (swappable to neutral). `createConnection` may emit a
  surface-specific marker/state — verify it's surface-agnostic before sharing.
- **Editor vs chat picking** — the editor already has native option dropdowns; the chat pickers are
  additive for the conversational build flow. The agent prompt note (Server design #3) makes the
  editor agent use them when building via chat.
- **Decomposition** — this spec is large (whole-pipeline client extraction + store renames + two tool
  relocations + Copilot wiring). If the plan gets unwieldy, split into (2a) client share + store
  rename + AI Hub refactor, and (2b) Copilot server registration + panel wiring.

## EE conventions

New/changed server files under `server/ee/` keep the Enterprise header + `@version ee`. Client follows
CLAUDE.md (interface names end `I`/`Props`, sorted keys/imports, `twMerge`, Lucide `*Icon`).
