# AI Hub Autonomous Tool Attach — Discovery, Wiring, and User Feedback

**Status**: **Shipped** (2026-05-14, plus follow-ups same day)
**Owner**: Ivica Cardic
**Scope**: server (EE, AI Hub agents) + client (AI Hub chat)
**Edition**: EE only (AI Hub is EE)

---

## 1. Problem

The LLM-facing surface for "attach a tool to this chat mid-conversation" had
the persistence layer wired (`AiHubTaskToolFacade.attachComponent` +
`addTool`, both idempotent, both exposed via `AttachTaskToolToolCallback` and
`RemoveTaskToolToolCallback` on the ASK and BUILD agents) but no agent-facing
plumbing to drive it. The agent could not:

1. Search the 180+ component catalog for an integration the user named.
2. Enumerate a component's actions or inspect an action's parameter schema.
3. List the existing workspace connections for a component before requesting
   a new one.
4. List tools already attached to the current task (avoid duplicates).
5. Learn the connection id the user picked in the chat picker UI without
   the user typing it back manually.
6. Update an attached component's connection in place — the facade's
   idempotency tuple included `connectionId`, so a "null then real" sequence
   from the autonomous flow would have produced duplicate rows.
7. Surface attached-tool results in chat cleanly — they fell through to the
   raw-JSON tool-call fallback renderer.
8. Pose structured multi-choice clarifying questions to the user — only
   free-text chat was available.

The system prompt did not mention `attachTaskTool` / `removeTaskTool` at all.

## 2. What shipped

Implemented as a sequence of commits on branch `0_732` between
2026-05-14 (afternoon) and 2026-05-14 (evening). Each phase is in its
own commit so revert / cherry-pick stays clean.

| Phase | Commit | Scope |
|---|---|---|
| A | `e0faa6d020f` | Discovery callbacks: `searchComponents`, `listComponentActions`, `describeComponentAction`. Wired on ASK + BUILD. |
| B | `730b77d557c` | State visibility: `listTaskTools` (platform), `listConnectionsForComponent` (automation). Same wiring. |
| D | `39727dc6b15` | Hardened `attachTaskTool` — new facade method `setComponentConnection` + new repo finder `findByTaskAndComponentIgnoringConnection`. Re-attach with a different connection id now rebinds the existing row instead of creating a duplicate. |
| E (prompt) | `cf0f669e9db` | System prompt updates on BUILD + ASK with the seven-step discover-attach flow. |
| E (renderer) | `1409aee8b61` | Compact `AttachTaskToolBody` / `RemoveTaskToolBody` chat result chips replacing raw JSON. |
| C demolition | `99a7007a4e7` | Removed the bespoke connection-choice store + tool callback + GraphQL mutation + client POST. See §5 for why. |
| F (AskUserQuestion) | `7fa347377ef` | Wired spring-ai-agent-utils' `AskUserQuestionTool` on both agents; client renderer; data-part registration. |
| F (UX polish + metrics) | `3f60d7a42c1` | System-role answer messages; persistent submitted state via fingerprint store; "Other…" free-text fallback; metrics counters across all callbacks. |

## 3. End-to-end flow (post-ship)

```
User: "Set me up a Slack daily reminder at 9am"
  │
  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ AI Hub agent (BUILD or ASK) — guided by prompt_ai_hub_*.txt                  │
│                                                                              │
│  1. searchComponents({query: "slack messaging"})                             │
│  2. listComponentActions({componentName: "slack"})                           │
│  3. describeComponentAction({componentName: "slack",                         │
│                              actionName: "sendMessage"})  (if needed)        │
│  4. listTaskTools()  — avoid duplicate attach                                │
│  5. listConnectionsForComponent({componentName: "slack"})                    │
│                                                                              │
│  Branch A (zero existing connections):                                       │
│     6a. requestConnection({componentName: "slack"})                          │
│         ─► chat UI renders AiHubConnectRequestMessage with a                 │
│            "Connect Slack" button → ConnectionDialog opens →                 │
│            user finishes OAuth → user types "ready" or similar               │
│     7a. listConnectionsForComponent (again) → now non-empty                  │
│                                                                              │
│  Branch B (one or more existing connections):                                │
│     6b. askUserQuestion({questions: [{question: "Which connection?",         │
│           options: [{label: "Slack Prod"}, {label: "Slack Team"}]}]})        │
│         ─► chat UI renders AiHubAskUserQuestionMessage with option buttons   │
│         ─► user clicks "Slack Prod" → assistant-ui posts a system message    │
│            "User picked: Slack Prod" into chat memory                        │
│     7b. agent reads the system message from chat memory on next turn         │
│                                                                              │
│  8. attachTaskTool({componentName: "slack",                                  │
│                     actionName: "sendMessage",                               │
│                     parameters: {...},                                       │
│                     connectionId: 42})                                       │
│     ─► chat UI renders AiHubToolCallRenderer's AttachTaskToolBody:           │
│        "✓ Attached: Slack → Send Message"                                    │
└─────────────────────────────────────────────────────────────────────────────┘
```

Idempotency at every step:

- `searchComponents` / `listComponentActions` / `describeComponentAction` /
  `listTaskTools` / `listConnectionsForComponent` / `askUserQuestion` are all
  read-only and safe to re-issue.
- `attachTaskTool` is the hardened version (§4). Re-issuing with the same
  tuple is a no-op; re-issuing with a different `connectionId` rebinds the
  existing component binding instead of creating a duplicate.

## 4. Hardened attachTaskTool

The original `AiHubTaskToolFacade.attachComponent` keyed its idempotency on
`(taskId, componentName, componentVersion, connectionId, environment)`.
The autonomous flow can legitimately issue an attach with `connectionId=null`
during discovery and a follow-up with `connectionId=42` after the user picks
— without intervention, that's two distinct rows.

Fix landed in commit `39727dc6b15`:

- `AiHubTaskToolFacade.setComponentConnection(taskComponentId, connectionId)`
  — rebinds an existing component binding in place. Idempotent on
  no-change; throws on unknown id.
- `AiHubTaskToolFacade.findTaskComponentIdIgnoringConnection(taskId,
  componentName, componentVersion, environment)` — returns any existing
  binding for the (task, component, version, environment) tuple ignoring
  the connection column.
- `AiHubTaskComponentRepository.findByTaskAndComponentIgnoringConnection`
  is the underlying repo finder (Spring Data JDBC `@Query` with `LIMIT 1`
  to silence multi-row returns from legacy double-attaches).
- `AttachTaskToolToolCallback.call()` now resolves the existing binding
  first; routes to `setComponentConnection` when found, falls back to
  `attachComponent` for genuinely new bindings.

The UI-driven attach path (`AiHubTaskToolGraphQlController.attachAiHubTaskTool`)
always supplies a `connectionId` up front, so the new lookup is a no-op there.
The chat-driven autonomous flow is the only caller exercising the rebind.

## 5. User-feedback contract: AskUserQuestion (not the connection-choice store)

The first iteration of this work shipped a bespoke async pattern for
"how does the user's connection pick get back to the agent?":

- `AiHubConnectionChoiceStore` — thread-scoped in-memory map of
  `(threadId, componentName) → (connectionId, name, chosenAt)`, with TTL.
- `GetPendingConnectionChoiceToolCallback` — the agent's next-turn read.
- `recordAiHubPendingConnectionChoice` GraphQL mutation — picker UI POSTs
  the user's choice into the store.

This shipped in commits `5a8dd03bc2c` (Phase C) and `cf0f669e9db` (Phase E
prompt), then was **removed entirely in `99a7007a4e7`** because
spring-ai-agent-utils' `AskUserQuestionTool` covers the same UX with a
better contract:

- Library-defined `Question` / `Option` schema — the LLM sees a canonical
  multi-choice tool definition.
- Renders as interactive buttons inline in chat via
  `AiHubAskUserQuestionMessage`.
- User's pick flows back as a chat message (role `system`, text
  `"User picked: <label>"`) via `threadRuntime.append`. The agent reads it
  from chat memory naturally on its next turn — no side-channel store, no
  GraphQL mutation, no correlation-id bookkeeping.
- Generalises beyond the connection-pick case: disambiguation across
  multiple component matches, action selection, yes/no confirmation gates.

### 5.1 What "wraps the library" means here

`AskUserQuestionToolCallback` (commit `7fa347377ef`) does NOT use the
library tool's `QuestionHandler` synchronously. The library expects
`QuestionHandler.handle(List<Question>)` to return the answers; for AI Hub
that would mean blocking the request thread on `CompletableFuture.get(5min)`,
which is incompatible with a streaming agent serving thousands of
concurrent chats.

Instead, the wrapper:

1. Builds an `AskUserQuestionTool` via the library's builder with
   `answersValidation(false)` and a `QuestionHandler` that captures the
   parsed `Question[]` into a `ThreadLocal` and returns placeholder empty
   answers.
2. Invokes the library tool's `call(...)` to drive parsing + validation +
   our QuestionHandler.
3. Discards the library's serialised placeholder response and emits its own
   `{kind: "ask-user-question", questions: [...], awaitingAnswer: true}`
   envelope.
4. Overrides the library's `getToolDefinition()` to rename the tool from
   `AskUserQuestionTool` (the library's PascalCase class name) to
   `askUserQuestion` (matching the AI Hub camelCase convention).

The LLM sees a canonical schema; the runtime stays streaming-friendly; the
answer flows via chat memory instead of a blocking future.

### 5.2 Why this is "good enough"

The async-via-chat-memory contract costs one extra LLM round-trip per
question vs. true synchronous handling. In chat UX this reads as "the agent
waits for me, then proceeds" — natural, not janky. The win of the library
contract is the schema + UI surface; the win of staying async is the thread
budget. The alternative (full sync block + resume POST + pending-question
registry) was considered and explicitly deferred — see §10.

## 6. Tool callback catalog (shipped)

All in
`server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/tool/`
except where noted. All wired on **both** the ASK and BUILD agents via
`automation-ai-hub-service`'s `AiHubConfiguration`.

| Tool | Class | Purpose |
|---|---|---|
| `searchComponents` | `SearchComponentsToolCallback` | Free-text catalog search with truncation envelope when `totalMatched > limit`. |
| `listComponentActions` | `ListComponentActionsToolCallback` | Enumerate a component's actions. |
| `describeComponentAction` | `DescribeComponentActionToolCallback` | Full input-property schema for one action + connection-required flag. |
| `listTaskTools` | `ListTaskToolsToolCallback` | Tools already attached to the current task (joined with component context). |
| `listConnectionsForComponent` | `ListConnectionsForComponentToolCallback` *(in `automation-ai-hub-service`)* | Existing workspace connections for a component, filtered by `connectionDefinition.version` (not the component version — see CLAUDE.md). |
| `attachTaskTool` | `AttachTaskToolToolCallback` *(hardened)* | Attach (or rebind connection on existing binding) + addTool. |
| `removeTaskTool` | `RemoveTaskToolToolCallback` | Detach an attached tool. |
| `askUserQuestion` | `AskUserQuestionToolCallback` | Multi-choice clarification with structured options. |
| `requestConnection` | `RequestConnectionToolCallback` | Render the connection-picker UI when no existing connection works. |

## 7. Client-side surface (shipped)

| File | Role |
|---|---|
| `pages/automation/ai-hub/messages/AiHubAskUserQuestionMessage.tsx` | Renders the `ask-user-question` data part: option buttons (single-select), checkboxes + Submit (multi-select), "Other…" affordance with text input (single-select only). Persists submitted state in `useAiHubAskedQuestionsStore` by content fingerprint. Submits via `threadRuntime.append({role: 'system', content: [{text: "User picked: <answer>"}]})`. |
| `pages/automation/ai-hub/connect/AiHubConnectRequestMessage.tsx` | Renders the `connect-request` data part: "Connect <Component>" button when no connections exist, `<Select>` + `+` button when ≥ 1 exists. Opens `ConnectionDialog` for new-connection creation. |
| `pages/automation/ai-hub/messages/AiHubToolCallRenderer.tsx` | Adds `AttachTaskToolBody` ("✓ Attached: …") and `RemoveTaskToolBody` ("✓ Detached: …") to the `SPECIAL_BODIES` registry. |
| `pages/automation/ai-hub/messages/AiHubMessageContent.tsx` | Registers `'ask-user-question': AskUserQuestionData` and `'connect-request': ConnectRequestData` in `MessagePrimitive.Parts.components.data.by_name`. |
| `pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider.tsx` | Adds the `askUserQuestion` tool-result handler that parses the JSON envelope and `addMessage`s a `data-ask-user-question` content part (mirrors the existing `requestConnection` handler). |
| `pages/automation/ai-hub/messages/stores/useAiHubAskedQuestionsStore.ts` | In-memory Zustand store for per-session "already answered" tracking, keyed by question content fingerprint. |

## 8. System-prompt guidance

Landed across `cf0f669e9db` (initial autonomous-attach copy) and
`7fa347377ef` (askUserQuestion section). Both `prompt_ai_hub_build.txt`
and `prompt_ai_hub_ask.txt` updated:

- BUILD: full 7-step "Attaching tools mid-chat" + "Clarifying questions"
  + "Removing tools" sections. Attach + remove + requestConnection are
  user-visible and require surfacing intent in chat first.
- ASK: read-only subset — discovery + state-visibility callbacks plus the
  askUserQuestion guidance. Suggests switching to BUILD for the actual
  attach.

## 9. Metrics (shipped, commit `3f60d7a42c1`)

`AiHubToolAttachMetrics` exposes four counters wired across all callbacks.
ObjectProvider-injected `MeterRegistry` so lightweight app variants without
actuator start cleanly.

| Counter | Tags | Records |
|---|---|---|
| `bytechef_ai_hub_tool_attach_discovery` | `tool` ∈ {`searchComponents`, `listComponentActions`, `describeComponentAction`}, `outcome` ∈ {`success`, `truncated`, `empty`, `error`} | Each discovery callback invocation. `truncated` only meaningful for `searchComponents`. |
| `bytechef_ai_hub_tool_attach_state_visibility` | `tool` ∈ {`listTaskTools`, `listConnectionsForComponent`}, `outcome` ∈ {`success`, `empty`, `error`} | State-read callbacks. `empty` on `listConnectionsForComponent` is the signal that drives the agent to escalate to `requestConnection`. |
| `bytechef_ai_hub_tool_attach` | `outcome` ∈ {`new_component`, `rebound_connection`, `error`} | `attachTaskTool` invocations. `rebound_connection` confirms the hardened-attach path is being exercised. |
| `bytechef_ai_hub_ask_user_question` | `outcome` ∈ {`success`, `empty`, `error`} | `askUserQuestion` invocations. |

No per-workspace tag (mirrors `WorkflowChatMetrics`' policy — autonomous-attach
is bursty so per-workspace cardinality would dominate the time-series).

## 10. Deferred work

- **True synchronous AskUserQuestion** — block the QuestionHandler on a
  `CompletableFuture` resolved by a `submitAiHubQuestionAnswer` GraphQL
  mutation. Saves one LLM round-trip per question; costs request-thread
  blocking time per pending question. Considered, deferred for thread-budget
  reasons. The chat-memory route is the right shape for AI Hub's
  concurrency profile.
- **Dynamic-options enumeration** in `describeComponentAction` — listing
  Slack channels for a chosen connection, etc. Currently the agent attaches
  with the user's verbatim parameters and lets them adjust in the workflow
  editor.
- **Confirmation gates** before mutations — currently relies on system-
  prompt discipline ("surface intent in chat BEFORE calling"). A blocking
  yes/no gate could be done via `askUserQuestion`; not currently wired.
- **Per-workspace metric counters** — see §9 rationale.
- **Detach UI confirmation** — `removeTaskTool` is immediate; no
  "are you sure?" prompt.

## 11. Tests

| Layer | Coverage |
|---|---|
| Unit (per callback) | 4–9 tests each, covering happy path / empty / error / context-missing / discriminator-specific outcomes (truncation, rebind, etc.) |
| Client renderer | 9 tests for `AiHubAskUserQuestionMessage` (header rendering, single-select submit, multi-select submit, Other… text input, persistent submitted state, empty questions noop) |
| Existing client tests | `AiHubConnectRequestMessage.test.tsx` retains 7 cases covering pick / create / button vs select branches |

End-to-end integration test of the full search → attach flow with a stub
`ChatModel`, plus a connection-picker coherence IntTest (when does
`requestConnection` UI fire vs `askUserQuestion`), are tracked as
follow-ups; see commits that postdate `3f60d7a42c1`.

## 12. Files touched (shipped)

**Server**:
- `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/task/AiHubTaskToolFacade.java` (added `setComponentConnection`, `findTaskComponentIdIgnoringConnection`)
- `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/tool/`:
  - `SearchComponentsToolCallback.java` (new)
  - `ListComponentActionsToolCallback.java` (new)
  - `DescribeComponentActionToolCallback.java` (new)
  - `ListTaskToolsToolCallback.java` (new)
  - `AskUserQuestionToolCallback.java` (new)
  - `AttachTaskToolToolCallback.java` (hardened + metrics)
- `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/metric/AiHubToolAttachMetrics.java` (new)
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/`:
  - `tool/ListConnectionsForComponentToolCallback.java` (new)
  - `task/AiHubTaskToolFacadeImpl.java` (setComponentConnection + finder impls)
  - `task/repository/AiHubTaskComponentRepository.java` (new `findByTaskAndComponentIgnoringConnection`)
  - `config/AiHubConfiguration.java` (wires everything on ASK + BUILD)
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/prompt_ai_hub_{ask,build}.txt`

**Client**:
- `client/src/pages/automation/ai-hub/messages/AiHubAskUserQuestionMessage.tsx` (new)
- `client/src/pages/automation/ai-hub/messages/stores/useAiHubAskedQuestionsStore.ts` (new)
- `client/src/pages/automation/ai-hub/messages/AiHubMessageContent.tsx` (data part registration)
- `client/src/pages/automation/ai-hub/messages/AiHubToolCallRenderer.tsx` (`SPECIAL_BODIES` additions)
- `client/src/pages/automation/ai-hub/connect/AiHubConnectRequestMessage.tsx` (picker UI — Select + plus + dialog)
- `client/src/pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider.tsx` (askUserQuestion tool-result handler)

**Removed** (deliberately, commit `99a7007a4e7`):
- `AiHubConnectionChoiceStore` interface + impl + test
- `GetPendingConnectionChoiceToolCallback` + test
- `recordAiHubPendingConnectionChoice` GraphQL mutation + client `.graphql` file + controller method

## 13. Open questions

None blocking. Follow-ups in §10. The implementation matches the
intent declared in the original draft of this spec; the spec itself was
revised in place to reflect the AskUserQuestion pivot (deliberately so —
the alternative was a separate "rev 2" doc that would have left this one
permanently stale, which is the failure mode the spec-update note in
`docs/superpowers/` exists to prevent).
