# Agent HITL: Approval in Chat, One Primitive Pair, Platform Tool Gate

*Design spec, July 21, 2026. Informed by competitive research on n8n's Chat-node HITL (see research
notes in the PR/issue thread). Goal stated by product: one clear path for every case — no
n8n-style matrix of "works here, unverified there, broken in embedded".*

## Scope

**In scope:** the AI Agent component and its surfaces — canvas workflows (Approval action), agent
runs reached through workflow chat (`WORKFLOW_CHAT` conversations / `runChatWorkflow`), and the
embedded chat surface. Headless callers (MCP tool serving, A2A) are in scope only for their
fallback behavior.

**Out of scope:** the AI Hub copilot chat. The hub keeps its existing pinned `askUserQuestion`
interaction primitive; hub agent tools are user-driven CRUD executed inside the conversation the
user is already steering, so they carry no autonomous-action risk and get no approval gating. If a
later hub feature runs autonomous actions (scheduled personal agents), it can adopt the same
approval pipeline then — nothing here blocks that.

## What exists today (inventory)

- `approval` component: `ApprovalRequestApprovalAction` canvas action with **APPROVAL_CHANNELS**
  cluster elements — Slack, Google Mail, Microsoft Outlook 365, approval-task, approval-link.
  Supports **fields** (`FieldType`: text, textarea, select, number …); a request with no fields is
  a plain approve/reject. Resolution via tokenized `/approvals/{id}` links →
  `JobFacade.resumeApproval(jobId, uuid, approved)`.
- `ApprovalRequestApprovalTool`: approval as an **agent-invoked tool** cluster element, riding the
  `SuspendableToolCallingManager` suspend/resume protocol; the agent loop re-enters with the
  human's answer patched into the suspended tool call's response.
- `askUserQuestionTool` (AI Agent utils cluster element): LLM-initiated clarification — SSE
  `ask_user_question` event, workflow suspends, client POSTs answers to the resume webhook.
- **Gap:** no chat approval channel — an approval raised by a run that started from a chat
  conversation cannot land in that conversation.

## Decisions

### D1 — One primitive pair, not three render modes

n8n exposes three response types (Approval / Free Text / Custom Form) on one send-and-wait node,
which produces its worst seams: free text on an approval gate needs hand-rolled branching, and
Approval mode can reject-with-note but never approve-with-note. We keep exactly two primitives
with disjoint jobs:

| Primitive | Job | Renders as |
|---|---|---|
| **Approval** | A decision, optionally with a structured payload | No fields → Approve/Reject buttons + optional comment box (valid on **both** outcomes). Fields → form; Approve submits the values, Reject cancels. |
| **AskUserQuestion** | Information — LLM-initiated clarification | Question card; the answer feeds the agent loop. No decision semantics, no audit weight. |

There is no free-text mode on Approval and no approve/reject on AskUserQuestion. "Approve with a
comment/modification" is first-class (the comment — and, with fields, edited values — travels back
in the approval outcome), which n8n cannot express in one step.

### D2 — Chat is just another approval channel

New `ChatApprovalChannel` APPROVAL_CHANNEL cluster element:

- Targets **the conversation that started the run** (workflow chat already anchors
  jobId ↔ conversation). Emits a persistent **approval card** into that conversation — over the
  live SSE stream when connected, re-rendered from the pending suspend state on reload.
- **Validity rule (the clear path):** the channel is legal only when the run has a chat origin. A
  run started by webhook/schedule whose approval step lists only the chat channel fails that step
  loudly ("no chat origin — configure a fallback channel"). Never a silent no-op.
- Channels remain a list: chat + Slack + email may fan out simultaneously; the first response
  wins and `resumeApproval` already rejects a second resolution.
- Because the card is part of the SSE/AG-UI protocol — not a hosted browser page — the embedded
  surface renders it inline for free. (n8n's embedded mode drops native HITL entirely.)

### D3 — AskUserQuestion is not the tool gate

Its core function stays what it is: the LLM asking the user questions. A gate needs properties a
question does not have — binary outcome, the tool's name and AI-chosen arguments shown to the
reviewer, an audit record, and enforcement the LLM cannot skip. Three approval entry points share
one suspend mechanism and one channel pipeline:

1. **Canvas action** (`ApprovalRequestApprovalAction`) — deterministic workflow step.
2. **Agent-invoked tool** (`ApprovalRequestApprovalTool`) — the agent *chooses* to ask.
3. **Platform tool gate (new)** — a per-tool `requiresApproval` flag on the TOOLS cluster element
   configuration. `SuspendableToolCallingManager` intercepts a flagged tool **before execution**
   and raises a standard approval request whose card body is the tool name + arguments. Approve →
   the tool executes with those arguments; reject → a "denied by reviewer: <comment>" tool
   response feeds back into the agent loop. Enforcement lives in the platform, not in graph
   topology or prompts — the agent cannot call a flagged tool un-gated.

### D4 — In chat, approval is a card, never an input-mode takeover

The approval arrives as a distinct card event carrying a `requestId` and resolves through its own
endpoint (the existing tokenized approval resolution, plus comment/field payload). The chat input
box remains the conversation: typing while an approval is pending is just conversation, and typed
text **never** resolves an approval in either direction. This removes both n8n seams at once — the
"same node cannot chat and gate" conflict and the "typing means disapproval" surprise.

### Fallback matrix (no unsupported combinations)

| Surface / caller | Approval | AskUserQuestion |
|---|---|---|
| Workflow chat (hosted) | Inline card | Inline question card (exists) |
| Embedded chat | Same card via SSE/AG-UI | Same question card |
| Canvas run without chat origin | Non-chat channels (Slack/email/link/task); chat channel alone = loud failure | N/A (agent without chat origin should not carry the tool; runtime = clear error) |
| MCP / A2A headless call | Non-chat channels only; a paused sync run returns "approval required — resolve at \<form URL\>". MCP: clients with the URL-elicitation capability get an `elicitation/create` pointing at the form, and an accepted elicitation re-awaits the run and returns its real output in the same `tools/call` (`ApprovalElicitingToolSpecifications`). A2A: the task surfaces with `input-required` status. | Form-mode MCP elicitation as later alignment |
| AI Hub copilot | Out of scope | Hub's own pinned `askUserQuestion` (unchanged) |

## Implementation phases

1. **Approve/reject-with-comment (+ edited field values in the outcome)** on the approval
   primitive — outcome payload extension through `resumeApproval` and the channel senders.
   *(Done.)*
2. **`ChatApprovalChannel`** + card events on the workflow-chat SSE contract + client card
   rendering (hosted + embedded), including re-render of pending approvals on reload. *(Done —
   see notes below.)*
3. **Platform tool gate**: `requiresApproval` on TOOLS cluster element config, denial feedback
   into the loop, audit via the existing tool execution recording. *(Done — implemented as
   `ApprovalGateToolCallback` wrapping flagged tools in `AbstractAiAgentChatAction.getToolCallbacks`
   (inside the observable/audit wrapper, so the existing tool-execution listener records gate
   outcomes) rather than inside `SuspendableToolCallingManager` itself; the manager's existing
   sentinel/suspend protocol carries the gate suspension unchanged. The AI Agent node gained the
   APPROVAL_CHANNELS section (`AiAgentComponentDefinition`), defaulting to the chat channel when
   empty. Resume: approve → the raw callback executes the original arguments and the result (plus
   reviewer comment) patches into the loop; reject → explicit denial JSON. A second flagged call
   in one tool round defers with a plain response to preserve the single-suspend-per-round
   invariant. Implementation anchors:
   `docs/superpowers/plans/2026-07-21-agent-hitl-phase3-tool-gate.md`.)*

Phases 1–2 are independent of 3 and deliver the visible differentiation first.

### Phase 2 implementation notes

- **Delivery mechanism.** The Approval action is a plain (non-streaming) perform, so it cannot
  obtain an `SseEmitter`; instead `ChatApprovalChannel` (cluster element `approval/chat`, no
  connection) publishes an `SseStreamEvent(jobId, EVENT_TYPE_DATA, payload)` directly on the
  `SSE_STREAM_EVENTS` broker route — the same route the worker's streaming post-output processor
  uses — with the tenant id stamped in metadata. The payload is a `__eventType: approval_request`
  map carrying `resumeId` (tokenized), `formUrl`, `formTitle`, `formDescription`, `inputs`.
  Whatever bridge is registered for the job (webhook SSE, workflow-chat `AgUiStreamBridge`)
  receives it; both existing bridges already map `__eventType` payloads onward (named SSE event /
  AG-UI `CustomEvent`) with no changes. `getJobId()` moved from `ActionContextAware` up to
  `JobContextAware` so approval channels can reach the job id from their
  `ClusterElementContextAware` context.
- **Both entry points covered.** `ApprovalRequestApprovalTool` delegates to the same action
  perform, so the chat channel works identically when the agent invokes approval as a tool.
- **Client rendering.** `ApprovalRequestMessage` (`data-approval-request` in
  `aiChatDataComponents`) wraps the standard `ApprovalForm` keyed by `resumeId` — fields,
  comment box, Approve/Discard, submitted/expired states all come from the form; resolution goes
  through the job-resume endpoint (D4: typing never resolves). The interactive card renders on:
  the **CE Chats page** (`/automation/chats`, restored from the pre-AI-Hub removal — the AI Hub
  workflow chat is EE, so the CE surface for deployed chat workflows is this page, talking to
  `/webhooks/{id}[/sse]` directly), the **AI Hub workflow chat** (EE, via the `approval_request`
  AG-UI CustomEvent, task marked paused), and the **canvas workflow-test chat**. The embeddable
  `@bytechef/chat` widget resolves field-less approvals inline (a pinned Approve/Discard card with a
  comment box above the composer, POSTing to the tokenized resume endpoint) and renders approvals
  with form fields as markdown with the hosted-form link. None of the
  surfaces registers a chat resume URL for approvals — typed input never resolves them.
- **Reload behavior.** `AgUiStreamBridge` folds a persist-only markdown marker
  (`Approval requested — [open the approval form](url)`) into the accumulated assistant text, so
  a reloaded conversation still surfaces the pending approval via the still-valid form link even
  though inline cards are client-only. A card re-rendered after resolution degrades to the form's
  "no longer available" state.
- **Loud-failure rule, current strength.** The channel throws when no `jobId` is present
  (editor/in-process runs). A run with a jobId but no chat listener (webhook/schedule origin)
  publishes an event nobody consumes — the run still surfaces in the pending-approvals inbox and
  stays resolvable via the hosted form, but no live card is delivered. CLOSED: trigger-type-aware
  validation is wired into `WorkflowValidator.validateChatOnlyApprovalChannels` — a warning fires
  when a task's approval channels are chat-only (or an AI agent has a gated tool with no channels,
  which defaults to chat) and the workflow has no `chat/` trigger.
- **Continuation streaming.** On the CE Chats page and the canvas workflow-test chat, the inline
  card resolves through the SSE-negotiated resume endpoint (`POST /job/resume/{id}` with
  `Accept: text/event-stream`) via the shared `ApprovalResolutionContext`: the provider points its
  existing SSE machinery at the resume stream, so the resumed run's output — stream deltas, nested
  ask-user-question or approval events — lands back in the conversation through the same event
  handlers as a normal turn. `JobResumeSseStreamBridge` maps `__eventType` payloads to named SSE
  events (mirroring the webhook bridge) so those nested interactive events arrive intact. AI Hub
  workflow chat provides the same context with its own `useSSE` reader (the resume stream is
  independent of the AG-UI turn model): the continuation streams into a fresh assistant bubble,
  and nested approval/ask events render live. When the continuation stream closes, the client
  flushes the accumulated text into the task's chat memory via the
  `appendAiHubTaskAssistantMessage` mutation (ownership-checked in `AiHubTaskService`), so the
  continuation survives a reload. The hosted form page has no context and keeps the plain resume
  mutation.
- **Editor test runs.** Channels are production transports and stay skipped in the editor, but the
  tool gate sends the `approval_request` event through the agent's ToolContext SSE emitter instead
  (the same path `ask_user_question` uses), so the canvas test chat renders the card for gated
  tools. The card itself is self-contained for field-less approvals — buttons + comment rendered
  from the event data, no approval-form endpoint dependency — and only embeds `ApprovalForm` when
  the approval carries form fields. CLOSED: the standalone Approval *action* now also surfaces the
  card in editor test runs — in the editor environment (with a jobId) `perform` returns a one-shot
  `SuspendAwareSseEmitterHandler` that sends the `approval_request` event
  (`ChatApprovalChannel.buildApprovalRequestEventData`, the same payload the chat channel
  publishes) and suspends inside the handler; the in-process post-output processor drains the
  event into the test-run stream bridges and finalizes the suspend, exactly like the agent's
  mid-stream suspends. Suspending before returning would not work — `checkSuspend` replaces the
  perform result with the `Suspend`, so the emitter output would never be drained.
- **SDK widget continuation.** The `@bytechef/chat` inline card resolves with
  `Accept: text/event-stream` and drains the resumed run's output through the widget's existing
  SSE event handlers — deltas stream into a fresh bubble, and a nested approval re-opens the card.
- **Known limitations (follow-ups).** (b) `WebhookBridgeAgent` routes runs onto the
  streaming/event-bridge path when the workflow has a streaming task OR an approval task
  (`WebhookWorkflowExecutor.hasApprovalTask`), so the card always has a listener to land on.
  Residual CLOSED: `SseStreamApplicationEventListener` now emits a named `result` data event on
  COMPLETED, carrying the message read back from the `WEBHOOK_RESPONSE`-tagged task execution
  (published before the terminal job-status event so bridges are still open). Chat surfaces render
  it via their existing `result` handlers; `AgUiStreamBridge` renders it only when nothing was
  streamed, so streamed runs don't double the reply.
