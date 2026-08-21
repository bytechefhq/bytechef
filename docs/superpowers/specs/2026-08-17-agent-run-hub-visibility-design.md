# Agent run visibility in AI Hub chats — design

**Date:** 2026-08-17 · **Ticket:** 732 · **Status:** IMPLEMENTED (branch `claude/sdk-agent-channels-e87f4d`, 18 commits `5267ae3bb69..d0bff3e98bc`, all tasks reviewed; final whole-branch review passed with two fixes applied). Not yet merged.
**Companion to:** `2026-08-17-sdk-agent-channels-design.md` (independent of it, but easier after it)

**Scope:** the **AI Agent** entity (`AiAgent` — channels, elements, generated workflow, the Agents UI).
Not the `aiAgent` component node on a hand-built canvas, and not hand-authored workflows that happen to
call an LLM. Everything below relies on the agent's workflow being *generated*, which is what makes its
shape knowable.

Within the `aiAgent` component, two of its three chat actions are wired to the recorder — `chat` and
`streamChat`, the ones `AiAgentWorkflowGenerator` emits. **`AiAgentRealtimeChatAction` deliberately is
not** (`AiAgentComponentHandler:75-78` constructs it without the
`ObjectProvider<AgentConversationRecorder>` the other two take), so voice-session turns are not
recorded and a realtime session is invisible in the Hub. That is a scope choice, not an oversight: a
realtime session is a bidirectional audio stream with no discrete per-turn completion point of the kind
`doOnComplete` gives the streaming action, and the generator does not emit the action, so nothing
reaches it through a channel today. Wiring it would mean first deciding what a "turn" is for audio.
Recorded here so the gap is a decision on the record rather than something a later reader has to
rediscover.

## 1. Problem

When a user messages an AI Agent through Slack — or an agent's schedule fires — the run is invisible in
the AI Hub. Verified: `ai_hub_chat` rows are created only from `AiHubChatGraphQlController`, i.e. by an
explicit action in the Hub composer, and neither `automation-ai-agent` nor the AI components contain a
single reference to `AiHubChat`. What a user gets instead is a workflow execution row, which shows the
agent's input and output as JSON on a node — accurate, and useless as a conversation.

This is about to matter much more, and the plan that makes it matter already exists.
`docs/superpowers/plans/2026-08-17-delegate-scheduled-to-ai-agent.md` deletes the AI Hub task subsystem
— the `ai_hub_task*` tables, the `ai_hub_chat.ai_hub_task_id` column, and **the `TASK` chat kind** —
moving scheduled runs onto the AI Agent's `schedule` channel. Today a Hub task creates an `ai_hub_chat`
row of kind `TASK` whose runs are readable in the chats list; that plan removes the row and adds no
replacement. **This spec is the replacement, and should land before or with it**, or the consolidation
becomes a regression.

Secondary symptom, worth stating because it surprises people: an AI Hub agent chat gets a fresh UUID
`thread_id`, while a Slack conversation is keyed by the Slack channel id. They are different
conversations against the same agent, so testing an agent in the Hub and then messaging it on Slack
shares no memory.

## 2. Goals and non-goals

Goals

- A conversation that reaches an agent through any channel is readable in the AI Hub chats list, as a
  chat, with both sides of each turn.
- One conversation per (agent, `conversationId`) — a Slack channel is one continuing chat, and every
  fire of one schedule appends to a single chat, matching the stable per-row `conversationId` the
  generator already produces.
- Whatever `AiHubTask` gives users today is still available once agent schedules replace it.

Non-goals

- Replying *from* the Hub into the external channel. Read-only first; §7 records what it would take.
- Changing how channels are declared or the generated workflow topology.
- Canvas `aiAgent` nodes and hand-built workflows (see Scope).
- The `AiHubTask` removal itself. This spec makes it possible; it does not perform it.

## 3. What exists today (verified, not assumed)

- **Hub transcripts** live in Spring AI's session store — `AI_SESSION` / `AI_SESSION_EVENT` under the
  default jdbc backend, selected by `bytechef.ai.memory.provider` — keyed by `ai_hub_chat.thread_id`
  used verbatim as the session id, all written under the constant session user id `"ai-hub"`
  (`AiHubSessionMemory.SESSION_USER_ID`). The `ai_hub_chat` row is metadata only.
- **Every AI Agent gets chat memory by default, and the user can switch it off.**
  `AiAgentFacadeImpl:230-233` creates a `KIND_CHAT_MEMORY` element on agent creation, alongside the
  pinned chat and workflowCall channels; it is a singleton kind (`AiAgentFacadeImpl:1274`). The Settings
  card's "Chat memory" toggle is **element-backed**, not settings-backed
  (`AgentSettingsCard.tsx:106-113`, `checked={!!chatMemoryElement}`): switching it off *deletes* the
  element. So the default is on, but an agent can legitimately run with no chat memory — and therefore no
  session and no transcript. That is accepted behaviour, not a gap to close (§4).
- **The generator emits a fixed implementation, and it is already session memory.**
  `buildChatMemoryElement` writes `type = "chatMemory/v1/chatMemory"` with exactly one parameter,
  `conversationId = ${branch_in.conversationId}` (`CHAT_MEMORY_CONVERSATION_ID_EXPRESSION`). That element
  (`chat-memory-builtin`, `ChatMemory.java`) is built on `SessionRepository` + `DefaultSessionService` +
  `SessionMemoryAdvisor` — **the same session store the Hub reads**, not `SPRING_AI_CHAT_MEMORY`.
  (CLAUDE.md's claim that `SPRING_AI_CHAT_MEMORY` "belongs to the AI Agent component's chat-memory
  cluster elements" is stale for this element and should be corrected when this lands.)
- **The user ids differ, and it does not matter.** The builtin element writes under
  `ChatMemoryConstants.DEFAULT_USER_ID = "bytechef"` and the Hub under
  `AiHubSessionMemory.SESSION_USER_ID = "ai-hub"` — but both are only a write-side `defaultUserId` on the
  advisor. **The Hub reads a transcript by thread id, not by user id**:
  `AiHubChatServiceImpl:641` and `:841` call `sessionMemory.sessionRepository().getEvents(threadId)`.
  User id partitions only the *enumeration* API (`findByUserId`), which the Hub does not use for reading —
  it lists chats from its own `ai_hub_chat` table.

So for an AI Agent the transcript **already exists, in the store the Hub already reads, under the exact
session id the Hub would look up**. The only thing missing is an `ai_hub_chat` row pointing at that thread.
That is the entire feature.

## 4. The decision

§3 dissolves this. There is no transcript to move, no second store to read, no duplication to weigh, and
no key to align — the conversation is already in the Hub's session store, under the session id the Hub
reads by. **The entire feature is: create the `ai_hub_chat` row.**

Nothing about the agent runtime changes. No change to the chat-memory element, to
`buildChatMemoryElement`, or to the session user ids — and therefore no disturbance to
`ChatMemoryUtils.getFirstMessages` or the chat-memory component's actions, which enumerate
`findByUserId(DEFAULT_USER_ID)` and would have been the blast radius of a re-keying.

**There is no migration.** Existing agent conversations become visible the moment a row points at their
thread; their sessions are already written and already readable. A backfill is optional — ⚑ propose
none, since a row appears on the conversation's next turn and a backfill would need to enumerate
sessions by user id to find them, which is exactly the API this design avoids depending on.

The alternatives, for the record, each rejected by a fact discovered after it was proposed:
**A — mirror turns into the Hub store**: would persist a second copy of a transcript already in the right
place. **B — Hub reads the agent's store conditionally**: the stores are the same one.
**C — switch the agent to session-backed memory**: already true. **D — align the session user ids**:
unnecessary, the read path does not filter by user id.

## 5. Design

- **Nothing in the agent runtime changes.** No new element property, no generator change, no session
  re-keying. Stated explicitly because three earlier drafts of this spec each proposed one.
- **Chat row creation:** an optional `AgentConversationRecorder` port in CE, no-op when absent,
  implemented in EE by the Hub, invoked on the agent chat action's completion — not a job-status
  listener, which would have to recover the turn text from node output and would double-report for
  subflow children. It writes **no transcript**: the session store already has it. Its only job is
  find-or-create the `ai_hub_chat` row and keep `message_count`/preview current.
- **Keying:** `thread_id = conversationId` verbatim, which the session already uses as its session id.
  Uniqueness must be scoped per agent — two agents reachable on the same Slack channel share a
  `conversationId` and must not share a chat. ⚑ This needs a nullable `ai_agent_id` column on
  `ai_hub_chat` plus a partial unique index on `(workspace_id, ai_agent_id, thread_id)`: the first schema
  change in this pair of specs, and the reason it could not live inside the channels spec.
- **Ownership — resolved (user, 2026-08-17): the agent's creator owns them, and decides their reach.**
  A channel run has no ByteChef user (a Slack account, or nobody at all for a schedule), while
  `ai_hub_chat` is user-owned — every creation path takes a `userId`, rows are built as
  `new AiHubChat(userId)`, uniqueness is `(threadId, userId)`, and the row *is* the access control since
  the session store carries none. So the chat is owned by the person who created the agent: they deployed
  it, and its conversations are theirs to see and to share.

  Two implementation notes this needs. First, `AiAgent.createdBy` is an **auditing username string**, not
  a user id, so the recorder must resolve it to a `userId` (or the agent gains an explicit owner column —
  ⚑ prefer resolving, to avoid a second source of truth). Second, "decides their reach" means a
  visibility control rather than a hardcoded rule: the agent has no `visibility` column today, and the
  sanctioned way to add one is the existing `ResourceVisibility` (`PRIVATE < WORKSPACE < ORGANIZATION`,
  `platform-api`) plus a `ResourceVisibilityPolicy` — a nullable column on `ai_agent`, per the
  workspace-scoping convention. ⚑ Ship owner-only first and let conversations inherit the agent's
  visibility once that column exists; nothing in this design assumes the creator is the only reader.

  Consequence to state plainly in the UI: with a shared agent, the creator can read every conversation
  users have with it over Slack or WhatsApp. That is the creator's call to make, which is precisely why
  reach is a control rather than a default.
- **Read-only:** the composer stays disabled, as for the other webhook-bridged kinds.
  `AiHubChatKind#isWebhookBridged()` and `isWebhookBridgedChat(kind)` already gate the model picker,
  artifacts, suggestion chips and attachments, so `AGENT_CHAT` inherits the right affordances with no new
  client branches.

## 6. What this must not break

- **Always-new-chat semantics.** Picking an agent in the composer starts a *fresh* chat every time
  (`createAgentChatAiHubChat` is deliberately not idempotent). Channel-born chats are the opposite:
  found-or-created. Both are kind `AGENT_CHAT`, so the lookup keys on `(agent, thread_id)` and cannot
  collide with composer rows, which carry a random UUID thread id.
- **Agent behaviour.** C changes where an agent's memory lives. The recall semantics of the session-backed
  element must match the built-in one closely enough that agents do not visibly change behaviour — verify
  before committing to C, and treat a mismatch as a reason to reconsider A.
- **Fail-open.** A recorder error must never fail an agent turn, on the same principle as the auto-memory
  and checkpoint paths.
- **Volume.** A busy Slack workspace produces far more conversations than a human-driven Hub ever did.
  ⚑ Assume existing chat retention applies until told otherwise.

## 7. Follow-ups

- Replying from the Hub into the channel: the companion spec already has each channel declaring its reply
  action, so the Hub would need to invoke it with `conversationId` + text. A genuinely new write path;
  its own spec.
- Whatever `AiHubTask` offers beyond a transcript (instructions overlay, per-task tools) must be checked
  against agent capabilities before the removal, not before this spec.

## 8. Decisions to review (⚑ = assumption made autonomously)

- **Create the chat row; change nothing else — revised three times, smaller each time.** Draft 1 assumed
  the memory element was a canvas choice (`AiAgentFacadeImpl:230`: auto-created). Draft 2 assumed it was
  backed by `SPRING_AI_CHAT_MEMORY` (`ChatMemory.java`: session-backed, the same store the Hub reads).
  Draft 3 assumed the session user ids had to be aligned (`AiHubChatServiceImpl:641`: the Hub reads by
  thread id, not user id). What remains is one missing `ai_hub_chat` row.
- **Resolved (user, 2026-08-17): Chat memory off ⇒ no session, no transcript, no chat. Accepted.** The
  toggle is element-backed (`AgentSettingsCard.tsx:106-113`), not part of `AiAgentSettings.builtInTools`
  (which governs `askUserQuestion`/`autoMemory`/`skillManagement`/`webSearch` — auto-memory being a
  different feature: facts *across* conversations, not turns within one). Such agents get **no**
  `ai_hub_chat` row: an empty chat would misrepresent an agent that genuinely remembers nothing, and
  mirroring turns just for them would reintroduce a second transcript store for the minority case. Their
  runs stay visible as workflow executions, exactly as today. The recorder must therefore treat a missing
  `CHAT_MEMORY` element as "do nothing", not as an error.
- **Resolved (user, 2026-08-17): the agent's creator owns the chats and decides their reach** (§5).
  Remaining ⚑ within that: resolve `AiAgent.createdBy` (a username) to a userId rather than adding an
  owner column, and express reach with the existing `ResourceVisibility` machinery when it is added.
- ⚑ New nullable `ai_agent_id` column + partial unique index on `ai_hub_chat`.
- ⚑ A recorder port on the agent chat action, rather than a job-status listener.
- ⚑ Existing chat retention applies unchanged.
