# AI Hub — Remaining Gaps & Tech-Debt Roadmap

> **For agentic workers:** Four consolidated phases (7–10). Each section below has its own scope, architecture, and task list. Implement in order 7 → 8 → 9 → 10 (later phases are independent but 10 includes cleanups touched by 7–9).

**Scope**: close the remaining Mothership parity gaps and burn down the accumulated technical debt identified after Phases 1–6 landed.

---

## Phase 7 — Workflow execution streaming into chat

**Goal:** Agent can fire a workflow run from chat; the workflow's output streams back into the chat bubble (for workflows that support streaming) or returns a single message on completion (for request/response workflows). Follows the existing ByteChef chat-workflow contract (`chat.newChatRequest` trigger + `chat.responseToRequest` or streaming action + webhook/SSE endpoints).

### Architectural alignment with existing chat-workflow contract

Today's `Chats.tsx` page works by:
1. Client POSTs to `/webhooks/{workflowExecutionId}` (non-streaming) or opens an SSE stream at `/webhooks/{workflowExecutionId}/sse` (streaming).
2. The workflow must have a `chat.newChatRequest` trigger configured.
3. Output comes back either as a single `chat.responseToRequest` response or as a `text/event-stream` of chunks from a streaming action.

Phase 7 reuses that machinery — the agent calls a new tool that invokes the same endpoints and pipes output into the AG-UI stream.

### Non-goals (v7)

- **Kickoff workflows that don't fit the chat-workflow contract.** Only workflows with a `chat.newChatRequest` trigger are eligible. The agent lists eligible workflows via a new filtered `listChatWorkflows` tool.
- **Mid-run cancellation from chat.** User can cancel via the standard workflow-executions page.
- **Multi-workflow composition** (fire two workflows in parallel from one chat turn).

### Architecture

**Eligibility check**: a new server-side `WorkspaceChatWorkflowFacade` query (reuse the existing `useWorkspaceChatWorkflowsQuery` backend — the GraphQL already exists per `ChatsSidebar.tsx`). Use it server-side from a new tool callback.

**Two new tool callbacks** on `aiHubBuildSpringAIAgent` (BUILD only — workflow runs can mutate data):

1. `listChatWorkflows({projectId?})` — returns eligible workflows for the current workspace (+ optional project filter). Shape: `[{projectId, projectDeploymentId, workflowExecutionTriggerId, workflowLabel, requiresInput: boolean, inputSchema?}]`. Uses the existing chat-workflows query.

2. `runChatWorkflow({projectDeploymentId, workflowExecutionTriggerId, input: object})` — creates a workflow execution and returns the `workflowExecutionId` + a `streamUrl` (one of `/webhooks/<id>` or `/webhooks/<id>/sse` based on whether the workflow's trigger declares streaming). Does **not** wait — returns immediately; the client's AG-UI subscriber drives the actual streaming and relays output chunks back into the chat.

**New AG-UI event-type proxy**: the existing `AiHubRuntimeProvider` subscriber gains a handler for the `runChatWorkflow` tool result. On seeing `{streaming: true, streamUrl, workflowExecutionId}`, the subscriber opens the SSE stream, appends each chunk to the current assistant message via `appendToLastAssistantMessage`, and emits a "completed" chat bubble annotation when the stream closes. For non-streaming workflows (`{streaming: false, responseUrl}`), the subscriber POSTs + appends the single response.

**Input collection**: if the workflow requires input fields and the agent's initial `runChatWorkflow` tool call lacks them, the tool returns `{awaitingInput: true, inputSchema}` instead of kicking off the run. The agent's next chat turn asks the user for the missing fields; it then calls `runChatWorkflow` again with the completed input. (Standard agent-loop behavior.)

### Server

| Action | Path | Responsibility |
|---|---|---|
| Create | `ai-copilot-service/.../tool/ListChatWorkflowsToolCallback.java` | List eligible chat workflows |
| Create | `.../tool/ListChatWorkflowsToolCallbackTest.java` | Unit tests |
| Create | `.../tool/RunChatWorkflowToolCallback.java` | Create execution, return stream/response URL + id |
| Create | `.../tool/RunChatWorkflowToolCallbackTest.java` | Unit tests |
| Modify | `CopilotConfiguration.aiHubBuildSpringAIAgent` | Register the two new callbacks (BUILD only) |
| Modify | `prompt_ai_hub_build.txt` | Add capability paragraph for the two new tools + routing guidance (prefer running workflows over re-creating the same functionality in code) |

### Client

| Action | Path | Responsibility |
|---|---|---|
| Modify | `runtime-providers/AiHubRuntimeProvider.tsx` | Subscriber handler for `runChatWorkflow` tool result: opens SSE for streaming, POSTs for non-streaming, appends content to the active assistant message |
| Create | `runtime-providers/workflowStreamHandler.ts` | Extracted streaming utility (SSE open + chunk pipe) + unit tests |
| Modify | `tests/AiHubRuntimeProvider.test.tsx` | Two new tests: streaming path, non-streaming path |

### Tasks (7 commits)

1. `CC7 Add ListChatWorkflowsToolCallback`
2. `CC7 Add RunChatWorkflowToolCallback`
3. `CC7 Register chat-workflow tools on BUILD + prompt update`
4. `CC7 client - Extract workflowStreamHandler utility`
5. `CC7 client - Intercept runChatWorkflow tool result in subscriber`
6. `CC7 client - Subscriber tests for streaming + non-streaming paths`
7. `CC7 Final formatting + lint fixes`

### Risks
- Non-EE / CE workspaces may lack the chat-workflow infra; tool callback must `@ConditionalOnBean` the facade (or return a clean error).
- SSE connection lifecycle during agent-turn: the subscriber must close the stream before completing the turn, otherwise AG-UI doesn't know the assistant message is done. A small state machine guards this.

---

## Phase 8 — Subagent progress streaming

**Goal:** When `research` / `workflow_builder` / `data_analyst` / `image_generator` / `slide_builder` run, surface their intermediate progress ("Searching Apollo competitors…", "Loading schema…") in the chat bubble instead of silent "thinking…" for 30–90 seconds.

### Non-goals (v8)

- Full browsing transcript in the chat (that's the whole point of context isolation — keep subagent reasoning hidden).
- User-cancellable progress.
- Per-tool-call progress UI (we only surface high-level status lines).

### Architecture

Subagent `ChatClient`s run inside their parent tool-call invocation. The parent agent's AG-UI stream is the only thing the client sees. Two approaches:

**Approach A (chosen): Subagent tool-call interception + relay.** Each subagent's `ChatClient` is instrumented with a Spring AI `ToolCallback`-decorating advisor that, before each tool call, publishes a brief `"<toolName>: <short input summary>"` line to a shared thread-local `ProgressChannel`. The parent `ToolCallback` (the hand-rolled `research`/`workflow_builder`/etc.) drains the channel periodically and emits AG-UI `CUSTOM` events on the parent stream with `{kind: 'subagent-progress', text: '...'}`. Client subscriber intercepts these and renders them in the active assistant bubble as a subtle status line above the content.

**Approach B (rejected)**: Have the subagent `ChatClient` stream directly to the client's SSE — requires tunneling two streams through AG-UI, which the existing protocol doesn't express cleanly.

### Server

| Action | Path | Responsibility |
|---|---|---|
| Create | `ai-copilot-service/.../progress/SubagentProgressChannel.java` | Thread-local per-tool-call channel holding a bounded queue of progress strings |
| Create | `.../progress/ProgressReportingToolCallbackAdvisor.java` | Spring AI advisor that wraps each subagent tool call and pushes a progress line when the tool starts |
| Modify | `ResearchConfiguration`, `WorkflowBuilderConfiguration`, `DataAnalystConfiguration`, `ImageGeneratorConfiguration`, `SlideBuilderConfiguration` | Wrap each subagent `ChatClient` with the advisor |
| Modify | `ResearchToolCallback`, `WorkflowBuilderToolCallback`, `DataAnalystToolCallback`, `ImageGeneratorToolCallback`, `SlideBuilderToolCallback` | After the `ChatClient.call(...)` invocation, drain the channel and emit captured progress via a parent-visible callback. Exact mechanism: each `ToolCallback` gets an optional `ProgressEmitter` injected via `ToolContext`; the parent agent's subscriber captures `CUSTOM` events with the progress payload |
| Modify | Parent agent `SpringAIAgent` subclasses | Expose a progress-emitter through the AG-UI `AgentSubscriber` → enqueue `CUSTOM` events on the parent stream |

### Client

| Action | Path | Responsibility |
|---|---|---|
| Modify | `AiHubRuntimeProvider.tsx` | Subscriber handler for `CUSTOM` events with `{kind: 'subagent-progress'}` — append as a dimmed status line to the active assistant message |
| Create | `components/SubagentProgressLine.tsx` | Small styled component for the status line (subtle gray, italic, auto-fades after 3s of no new progress) |
| Modify | `tests/AiHubRuntimeProvider.test.tsx` | Two tests: progress event renders line, multiple progress events stack |

### Tasks (8 commits)

1. `CC8 Add SubagentProgressChannel + ToolCallbackAdvisor`
2. `CC8 Wire progress advisor into 5 subagent configurations`
3. `CC8 Drain progress channel from 5 subagent entry ToolCallbacks`
4. `CC8 Emit progress as AG-UI CUSTOM events from parent subscriber`
5. `CC8 client - SubagentProgressLine component`
6. `CC8 client - Intercept subagent-progress CUSTOM events`
7. `CC8 client - Tests for progress rendering`
8. `CC8 Final formatting + lint fixes`

### Risks
- AG-UI `CUSTOM` event support — confirm the `com.agui.core` event set at plan-phase; if not supported, piggyback on `TEXT_MESSAGE_CONTENT` with a marker prefix the client subscriber parses out.
- Per-tool-call granularity: Spring AI's advisor API may fire before/after tool calls but not inside the LLM's reasoning; progress lines will be tool-call-grained, not token-grained. Acceptable for v8.

---

## Phase 9 — Composer polish + connection management

**Goal:** `@`-picker pagination + multi-project workflow aggregation, plus inline connection-request UX via a custom chat-message render with a "Connect" button that opens the existing `ConnectionDialog`.

**Dropped from original scope**: drag-drop into the composer. Revisit later if user demand materializes — covered by the existing `@`-picker click flow today.

### Scope breakdown

Two independent sub-features:

### 9A — `@`-picker pagination + search polish

**What**: today the picker fetches all files/workflows/tables/KBs upfront. For large workspaces, paginate with the existing REST/GraphQL pagination (each hook likely supports it) + debounce the search input; render 20 items per kind with a "Show more" link.

**Files**:
- Modify: `composer/AiHubComposer.tsx` — wrap each section in an infinite-query pattern using the existing hooks where they support pagination. For hooks that don't, fall back to client-side `slice(0, 20)` + "show more" button.
- Also address the **"workflow picker only fetches first project"** tech debt: use `useWorkspaceChatWorkflowsQuery` (aggregated — used by `ChatsSidebar`) or a new `useWorkspaceAllWorkflowsQuery` if such a hook doesn't exist; if only project-scoped hooks exist, add a thin aggregation hook that iterates projects.

**Commits**:
- `CC9 client - Aggregate workflow picker across all projects`
- `CC9 client - Paginate @-picker sections`

### 9B — Connection requests via custom message render

**What**: when the agent recognizes "the user wants me to connect Slack / Gmail / …", it calls a new `requestConnection({componentName})` tool. The tool returns a marker payload `{kind: 'request-connection', componentName, suggestedName?}` that the subscriber recognizes as a custom message type. The chat renders a "Connect <Component>" button. Clicking opens the existing `ConnectionDialog` prefilled with the component.

**Files**:
- Create: `ai-copilot-service/.../tool/RequestConnectionToolCallback.java` + test — validates componentName exists (via the existing component registry facade), returns `{kind: 'request-connection', componentName, suggestedName}`.
- Modify: `CopilotConfiguration.aiHubBuildSpringAIAgent` — register the new callback.
- Modify: `prompt_ai_hub_build.txt` — add a paragraph about using `requestConnection` when the user asks to connect something.
- Create client: `components/AiHubConnectRequestMessage.tsx` — renders in the chat thread when the subscriber sees a `request-connection` payload; shows "Connect <Component>" button.
- Modify: `AiHubRuntimeProvider.tsx` subscriber — on `requestConnection` tool result, instead of the default handling, store the payload into a dedicated per-message slot that the Thread renders via a custom assistant-ui `Message` renderer.
- Wire up `ConnectionDialog` open — use existing `ConnectionDialog` from `@/shared/components/connection/` (exact path confirmed during implementation).

**Commits**:
- `CC9 Add RequestConnectionToolCallback`
- `CC9 client - Render connection-request messages with Connect button`

### Tasks total (Phase 9: 4 commits across 2 sub-features)

### Risks
- `assistant-ui` custom message renderer: research whether `@assistant-ui/react` supports a custom `Message` primitive; if not, render the connect button in a parallel layer above the thread.
- `ConnectionDialog` API — confirm existing component accepts `defaultComponentName` prop or similar prefilling.

---

## Phase 10 — Technical debt burn-down

**Goal:** Eliminate the deferred tech-debt items from Phases 1–6 in one focused cleanup phase. No new user-facing features.

### Items

#### 10A — Environment-aware scoping

**Problem**: `QueryDataTableToolCallback`, `AddDataTableRowToolCallback`, `UpdateDataTableRowToolCallback`, `DeleteDataTableRowToolCallback`, `AddDataTableColumnToolCallback`, `AggregateDataTableToolCallback`, `ListKnowledgeBasesToolCallback` and their `MutationApplier` twins hardcode `environmentOrdinal = 0` (DEVELOPMENT).

**Fix**: add `environmentId` to the `WorkspaceInvocationContext` (already-existing thread-local/ToolContext on the server side). The client's `buildStateToSend` already injects `workspaceId`; add `environmentId` from `useEnvironmentStore.currentEnvironmentId`. Each tool callback reads `environmentId` from context instead of `0`.

**Files**: state contract update in `AiHubRuntimeProvider`, server-side `WorkspaceInvocationContext` extension + propagation through all affected ToolCallbacks (~10 files touched).

**Commits**:
- `CC10 Add environmentId to WorkspaceInvocationContext`
- `CC10 Route data-table callbacks through context environmentId`
- `CC10 Route KB callbacks through context environmentId`
- `CC10 client - Inject environmentId into AG-UI state`

#### 10B — Cache `OpenAiImageModel`

**Problem**: `GenerateImageToolCallback.call(...)` builds a fresh `OpenAiImageApi` + `OpenAiImageModel` per invocation.

**Fix**: lazily-initialized singleton + `ConcurrentHashMap<apiKey, OpenAiImageModel>` cache (in case the key changes across workspaces — unlikely in v5B but defensive).

**Commits**:
- `CC10 Cache OpenAiImageModel by API key in GenerateImageToolCallback`

#### 10C — AggregateDataTable memory + large-report asset-file spill

**Problem A**: Aggregation loads up to 50k rows in memory. Fine for most tables but not all.

**Fix A**: add a hard cap at 50k (existing) + emit a warning result `{truncated: true, scannedRows}` when capped. Longer-term: push aggregation down to SQL in a new service method. For v10 the truncation warning is the fix; SQL pushdown is deferred to v10.2.

**Problem B**: large data-analyst reports embed the whole markdown in the LLM's next-turn context. If the report is 3k words, the parent agent's context balloons.

**Fix B**: in `DataAnalystToolCallback`, if the subagent response exceeds N chars (e.g. 2000), save it as an asset file via `createAssetFile`, call `openFileTab`, and return a short summary `{reportFileId, reportName, summary: "<first 300 chars>..."}` to the parent. Keep reports under the threshold as-is (inline content in tool result).

**Commits**:
- `CC10 Emit truncation warning from AggregateDataTableToolCallback`
- `CC10 DataAnalystToolCallback spills large reports to asset files`

#### 10D — Pending-mutations beforeunload warning

**Problem**: closing a tab with pending mutations loses them silently.

**Fix**: client-side `beforeunload` handler that checks `aiHubPendingMutationsStore.getState().pending.length > 0` and returns a confirmation message. Browser renders "Leave site? You have N unapplied changes." Native UX, no custom dialog needed.

**Files**: new `usePendingMutationsBeforeUnload.ts` hook, subscribed in `AiHubPanel.tsx`.

**Commits**:
- `CC10 client - Warn on unload when pending mutations exist`

#### 10E — Error UX retry

**Problem**: tool-call failures mid-agent-turn toast a generic error; conversation stalls; no retry.

**Fix**: when a tool result payload has `{error: ...}`, the subscriber adds a `{role: 'assistant', content: 'I hit an error running X: <msg>', retryable: true}` message with a small "Retry this step" button that resends the last user message to the agent (same conversation, same thread id). Button disappears on success.

**Files**: subscriber extension + new `AiHubRetryBanner.tsx` (or inline in the message render).

**Commits**:
- `CC10 client - Add Retry affordance on tool-result errors`

#### 10F — End-to-end integration test

**Problem**: no test covers agent call → AG-UI event stream → client subscriber → store → REST apply, end-to-end.

**Fix**: add one integration test under `client/test/playwright/tests/ai-hub/` that:
1. Logs in to the dev server.
2. Opens `/automation/ai-hub`.
3. Sends a chat message that triggers a mutation stage.
4. Asserts the pending bar appears.
5. Clicks Apply; asserts the mutation's effect (e.g. a data-table row appears).

Uses the existing Playwright infra (`client/test/playwright/tests/asset-files/assetFiles.spec.ts` is a reference pattern).

**Commits**:
- `CC10 Add Playwright e2e for AI Hub stage+apply flow`

#### 10G — `@`-picker fetch-all (covered by 9B)

No additional commit here — handled by Phase 9B.

### Tasks total (Phase 10: ~10-12 commits)

### Risks
- Environment propagation through existing tool-contexts may surface subtle bugs in other callers that rely on the hardcoded `0`. Audit every usage during implementation.
- `beforeunload` custom messages are suppressed by modern browsers — the standard-issue "Changes may not be saved" is all users see. Acceptable.
- Retry UX: if the agent's last turn included multiple staged mutations that partially succeeded, "retry" is ambiguous. Scope v10 retry to single-tool-call failures only; batched retries are out of scope.

---

## Ordering

Suggested execution order:
1. **Phase 10** (tech debt) — safety net first; most items are small and unblock cleaner work later.
2. **Phase 7** (workflow execution) — biggest remaining feature.
3. **Phase 8** (subagent progress) — high UX value, medium complexity.
4. **Phase 9** (composer + connections) — polish; several independent sub-features that can land in any order.

Alternative: **Phase 7 first** if Mothership parity demos matter more than code hygiene — 10 is background cleanup that can ship alongside or after 7/8/9.

## Deferred beyond this roadmap

- **5A.2 undo within TTL** (spec'd in Phase 5A spec).
- **5A.3 audit viewer** (spec'd).
- **6.1 conversation-scoped artifacts + full-text search** (spec'd).
- **Mobile / responsive**.
- **i18n**.
- **Rate limiting / cost caps per workspace**.
- **Security review + a11y audit**.

## Commit convention

`CC7 …` / `CC8 …` / `CC9 …` / `CC10 …` with `client -` infix for client commits (matches every prior phase).
