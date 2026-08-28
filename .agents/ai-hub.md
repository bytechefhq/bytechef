# AI Hub (EE)

Chat kinds, the three-tier agent tool architecture, subagent memory and interactive questions, auto-memory storage, and the Copilot domain agent module map.

Extracted from `CLAUDE.md` to keep that file within its size budget;
read this before working in the areas below.

### AI Hub Chats (EE)

The AI Hub's conversation entity is a **chat** (`AiHubChat*`, package `com.bytechef.ee.ai.hub.chat`,
tables `ai_hub_chat` + `ai_hub_chat_artifact` / `_asset_file` / `_tool` / `_component`, route
`/automation/ai-hub/chats/:chatId`, sidebar sections "New Chat" and "Chats").

#### Chat kinds and threadId conventions

The `ai_hub_chat` table holds three discriminated kinds (`AiHubChatKind` enum, INT ordinal):

- `STANDARD` (ordinal 0) — default; runs through the LLM agent. `workflow_execution_id`
  and `project_deployment_id` are null. ThreadId is a random string from the client.
- `WORKFLOW_CHAT` (ordinal 1) — bound to a specific workflow execution; routed through
  `WebhookBridgeAgent` instead of the LLM. Carries `workflow_execution_id` +
  `project_deployment_id`. ThreadId is a plain UUID.
- `AGENT_CHAT` (ordinal 2) — bound to an **AI Agent's** generated workflow. Mechanically
  identical to `WORKFLOW_CHAT` (same `WebhookBridgeAgent`, same two columns, same plain-UUID
  threadId) and distinct only so the UI can name what the user picked: an agent, never the
  hidden `__AI_AGENT__` project's workflow behind it. Before it existed the client re-derived
  this per render by matching `workflow_execution_id` against `workspaceChatAgents`, which went
  blind the moment an agent was undeployed.

**Never test the two bridged kinds by enumerating constants.** Ask
`AiHubChatKind#isWebhookBridged()` server-side (both routing sites do: `AiHubRoutingAgent` and
`WebhookBridgeAgent`'s misroute guard) and `isWebhookBridgedChat(kind)` (`chats.api.ts`)
client-side, for every affordance that exists because a bridged chat has no LLM behind it — no
model picker, no artifacts, no suggestion chips, no attachments, cancel via
`cancelWorkflowChatTurn`, the auto-opened workflow panel. A missed site routes an agent chat to
the LLM, which answers plausibly in place of the agent's actual run and logs nothing. Beware the
inverse too: a `kind !== 'WORKFLOW_CHAT'` fallback silently absorbs each new
kind — the sidebar's default-icon branch tests `=== 'STANDARD'` positively for that reason, with
`toChat` funnelling unrecognised server kinds there.

Always-new chat semantics: every pick of an agent or a workflow starts a fresh chat rather
than restoring a prior thread — including `createWorkflowChatAiHubChat` and
`createAgentChatAiHubChat`, neither of which is idempotent despite a stale client-side comment
claiming otherwise. The two bridged mutations differ ONLY in the kind they stamp and share one
`createWebhookBridgedChat` implementation; they are separate mutations rather than one with a flag
because the caller already knows which cascade the user picked, and a boolean would let a client
silently create a mislabelled row. Each new row gets its own random UUID
threadId so session-store events are isolated per chat, not shared across a (user, workflow)
tuple. Past chats remain reachable through the chats list — they're just no longer
the default landing target. There is no partial unique index scoping rows per
(workspace, user, environment, workflow_execution_id); the `kind` column is the authoritative
discriminator.

Message bodies do **not** live in `ai_hub_chat` — the row is metadata only (title, preview,
`message_count`, status, kind, the kind-specific target ids). The transcript lives in Spring AI's
**session** store, keyed by `ai_hub_chat.thread_id` used verbatim as the session id: tables
`AI_SESSION` / `AI_SESSION_EVENT` under the default jdbc backend, selected by
`bytechef.ai.memory.provider` (`jdbc` | `redis` | `aws` | `in_memory`). It is NOT
`SPRING_AI_CHAT_MEMORY`. **Correction (ticket 732, prior drafts of this doc got this backwards):** the
AI Agent component's *builtin* chat-memory element (`chat-memory-builtin`'s `ChatMemory.java`) is
built on the SAME session primitives as the hub — `SessionRepository` + `DefaultSessionService` +
`SessionMemoryAdvisor` — not on `SPRING_AI_CHAT_MEMORY`; that table belongs only to whichever
*non-builtin* chat-memory cluster element a workflow author picks instead. Because the builtin element
writes to the session store under session id = the node's `conversationId`, an AI Agent using it
already has its transcript sitting exactly where the hub reads — see "Channel-born agent
conversations" below. Sessions are all written under the constant session user id `"ai-hub"`
(`AiHubSessionMemory.SESSION_USER_ID`), so the session store carries no authorization of its own:
ownership is enforced on the `ai_hub_chat` row by `AiHubChatServiceImpl`, which then reaches the
transcript by thread id.

Enum ordinals are pinned by `EnumOrdinalStabilityTest`; append new kinds at the end. The removed
`TASK` kind held ordinal 2 and `AGENT_CHAT` took its place — legal only because the AI Hub has never
shipped in a release tag, so no customer row carries either value. A dev database written before that
change must be reset, not migrated: an old `AGENT_CHAT` row (3) is now out of range and throws, and an
old `TASK` row (2) silently reads as `AGENT_CHAT` and routes to the webhook bridge.

#### Channel-born agent conversations (ticket 732)

When an AI Agent is reached through a channel (Slack, WhatsApp, a schedule, …) instead of the composer,
`AiHubAgentConversationRecorder` (EE `ai-hub-service`) find-or-creates the `AGENT_CHAT` row at turn
completion, through the CE SPI `com.bytechef.platform.ai.conversation.AgentConversationRecorder`
(`platform-ai-api`) that the AI Agent component calls. It writes **no transcript** — the builtin
chat-memory element already wrote both sides of the turn into the session store under
session id = `conversationId` (see the correction above); the recorder only points a metadata row at
that thread and refreshes `message_count`/preview through the hub's own read/patch paths.

- **`workflow_execution_id` null invariant.** Channel-born `AGENT_CHAT` rows always have a null
  `workflow_execution_id`; composer-created ones (`createAgentChatAiHubChat`) always carry one. Both
  the recorder's `adoptChat` hijack guard and the client's `isChannelAgentChat` discriminator
  (`chats.api.ts`) depend on this holding, combined with `aiAgentId != null` — changing either
  production path is a two-sided change.
- **Chat memory off ⇒ no chat, by design.** The agent's Settings card "Chat memory" toggle deletes the
  `CHAT_MEMORY` element when switched off (`AgentSettingsCard.tsx`); with no element there is no
  `conversationId`, no session, and the recorder is a silent no-op — not an error path.
- **Monolith only.** The recorder verifies the claimed workspace by resolving `workflowId` through
  `ProjectService#fetchWorkflowProject`. `RemoteProjectServiceClient.fetchWorkflowProject` is an
  `UnsupportedOperationException` stub like its siblings (see "EE Microservice Remote Client
  Pattern"), so on a distributed EE deployment verification throws, the recorder's fail-open path
  swallows it, and no row is ever created for a channel-born run — fail-closed and safe, but silent,
  the same shape as orphaned-job recovery and the error-workflow handler being monolith-only.
- **Attribution is only partly verified.** `workspaceId`/`aiAgentId`/`creatorUserId` are read off the
  workflow node's extension map by a component with no principal, under globally allowlisted reserved
  keys a hand-authored definition could set to anything. `workflowId` comes from the platform's own
  execution context instead, so the recorder resolves it back to the workflow's real project and
  rejects a `workspaceId` mismatch — cross-workspace forgery is closed. Still open: `creatorUserId` is
  unverified, so within one workspace a definition author can attribute a row to a colleague;
  `aiAgentId` is unverifiable by construction, since checking it would pull in the
  `automation-ai-agent` dependency this SPI is designed to avoid.
- **Privacy, decided deliberately.** A channel-born chat is readable by the agent's **creator** only
  (phase 1) — including previews of what external Slack/WhatsApp senders wrote. The external sender
  has no ByteChef account and no consent surface of their own.

#### Scheduling an agent

There is no AI Hub task entity. A recurring run is an **AI Agent with a `schedule` channel**: an
`ai_agent_channel` row whose `channelType` is `schedule`, carrying `expression` (a 5-field cron),
`timezone`, `prompt` and `name` in its `parameters` map. `AiAgentWorkflowGenerator` turns that row
into a `schedule/v1/cron` trigger on the agent's generated workflow, and — uniquely among channels —
substitutes the stored `prompt` into `branch_in`'s envelope `text` as a **literal**, because a
schedule has no incoming message to derive it from (`ChannelDefinitions.schedule()` therefore leaves
`EnvelopeMapping.text()` null). `aiAgent_1`'s `userPrompt` is `${branch_in.text}`.

Two consequences worth knowing: the prompt is baked into the generated workflow at save time rather
than read at fire time, and `MapUtils.getRequiredString` only rejects a *missing* prompt — an empty
string passes, so a blank prompt produces a schedule that fires on time and hands the agent nothing.
Client-side validation is the only gate.

The cadence UI is shared: `agentScheduleCron.ts` owns `toCronExpression` /
`toCadenceParameters` / `fromCadenceParameters` / `validateAgentScheduleCadence`, and
`AgentScheduleFrequencyFields` renders the per-kind fields. Note that component renders the fields
*belonging to* a frequency — the frequency `<Select>` itself lives in each caller
(`AgentScheduleDialog`, `AgentDialog`), so a caller that omits it is silently pinned to one cadence.
A schedule can be defined at agent-creation time in `AgentDialog` or afterwards on the detail page's
schedule card; both write the identical channel payload.

#### Chat launchers

There is no Workflow Chats page. The composer's **provider popup** (the `ModelPicker` dropdown) is the
single launcher, with two cascades above the provider list: **Agents** (first) and **Workflows**.
`useAiHubChatLaunchers` owns both and is wired into the home composer AND the in-chat composer, so a
launcher is reachable without navigating home. There is deliberately no scheduling cascade — a
scheduled agent runs on its own cron, so there is nothing to launch interactively.

Agents is backed by the `workspaceChatAgents` GraphQL query (`AiAgentFacadeImpl.getWorkspaceChatAgents`)
and lists `AiAgent`s whose **enabled** project deployment has a workflow with a hosted chat trigger;
those workflows live in hidden `__AI_AGENT__` projects that `workspaceChatWorkflows` filters out, so the
two cascades are disjoint. A disabled or missing deployment drops the agent from the listing entirely —
which is why both cascades render even when empty, with a sub-menu naming the reason ("No agent with an
enabled deployment."). Hiding an empty cascade made a launcher read as unimplemented rather than
unpopulated. The separate CE-only `/automation/chats` page renders an **Agents** group from the same
query above its per-project groups.

### Workflow-chat metrics

- `bytechef_workflow_chat_turn{outcome}` — global counter. Outcomes: `sync`, `streaming`, `resume`,
  `rate_limited`, `concurrency_blocked`.
- `bytechef_workflow_chat_turn_by_workspace{outcome,workspace}` — same outcomes plus a workspace tag
  for deployments with bounded workspace counts. The workspace dimension is **opt-in** via the
  separate counter so unbounded multi-tenant deployments don't pay the cardinality cost on every
  turn — pick the right counter for your tenant model.
- `bytechef_workflow_chat_resume{result}`, `bytechef_workflow_chat_unreachable{reason}`,
  `bytechef_workflow_chat_attachment_failure{reason}` — operational signals for resume HTTP outcome,
  unreachable workflows (disabled / deleted), and attachment promotion failures.

### AI Hub agent tool architecture (EE)

Tools reach the ai_hub ASK/BUILD agents through three tiers (wired in `AiHubConfiguration`):
**Tiers 1 and 2 are AI-Hub-only.** The Copilot panels and the management MCP surface register
every tool DIRECTLY via `.toolCallbacks(...)` with no tool-search advisor in the chain —
`PinnedToolSearchToolCallingAdvisor`, `AiHubGlobalToolCatalog`, `MultiSessionToolIndex` and the
`searchTool` itself live solely under `server/ee/libs/ai/ai-hub/`. `IntelligentToolCatalog` is a
shared registry of tool DEFINITIONS (all three surfaces partition it by name); only the hub puts a
pgvector search index in front of it. So "pinned vs catalog-demoted" is a choice that exists only
on the hub — everywhere else the tool is simply present and always callable.

1. **Pinned static list** — everything added via `toolCallbacks.add(...)` on the agent bean.
   `PinnedToolSearchToolCallingAdvisor` keeps the ENTIRE static list callable in every model
   iteration, so each entry costs schema tokens on every call. Reserve for interaction primitives
   (openResourceTab, askUserQuestion, connection pickers), self-configuration (attach/list task
   tools, state-visibility lookups), and the subagent delegate tools.
2. **Searchable catalog** — the per-mode `AiHubGlobalToolCatalog` beans
   (`aiHubAskGlobalToolCatalog` / `aiHubBuildGlobalToolCatalog`) feed the pgvector tool-search
   index; tools are callable only after a `searchTool` hit surfaces them. Catalog tools are
   security-context-rehydration-wrapped by `ToolSearchAdvisorConfiguration`, so
   `@PreAuthorize`-guarded facades work. Demote rarely-used tools here (createWorkflowChat,
   ASK's listApiCollections) and note them in the prompt as "find with searchTool first".
3. **Specialist subagents** — one-shot ChatClients registered as delegate tools. Two families, and
   this is now the COMPLETE list (ticket 732, CRUD-delegate unwind, complete): the
   nine catalog-backed intelligent delegates the hub surfaces — the catalog itself holds TEN; the hub
   omits the embedded-only `buildIntegrationWorkflow`, so "nine" and "all ten" below both hold and
   are counting different sets — shared with the Copilot panels and the management MCP
   surface (`authorSkill`, `configureClusterElement`, `writeScript`, `buildWorkflow`,
   `debugWorkflowExecution`, `importWorkflow` (BUILD-only), `buildCustomComponent`,
   `buildCodeWorkflow`, `configureMcpServer` (BUILD-only) — see `INTELLIGENT_TOOL_NAMES` and
   `registerIntelligentToolCallbacks`), and the four AI-hub-owned generative one-shots (`research`,
   `data_analyst`, `image_generator`, `slide_builder` via `registerSubAgentToolCallbacks`) that were
   deliberately never migrated into the catalog. Every other specialist that used to exist here —
   the Copilot CRUD specialists (context_store, knowledge_base, data_table, asset_file, ai_agent —
   `skills` was never one of these; `authorSkill` has always been catalog-backed) and the
   automation-owned CRUD specialists (mcp_agent, project_deployment_agent, api_collection_agent) —
   was dissolved; their tools are registered flat (or catalog-demoted for
   mutations) on whichever surface used to delegate to them, and
   `registerCopilotSubAgentToolCallbacks`/`registerSpecialistSubAgentToolCallbacks`/
   `SubAgentToolCallback` no longer exist. Delegates MUST forward the parent `ToolContext` into the
   inner ChatClient (`.toolContext(...)`) or workspace-scoped tools fail with "Workspace context
   unavailable". Wrap delegates in `ProgressReportingToolCallback` on the chat surface only (it
   narrates into the AG-UI stream).

Rules of thumb: streaming and suspend/resume contracts must stay pinned on the main agent —
`runChatWorkflow`'s SSE/awaitingInput contract is client-coupled to the MAIN agent's tool-call
event, and a subagent cannot pause mid-run and resume where it left off. It CAN, however, put a
rendered multiple-choice question to the user mid-delegation: it asks, stops, and the parent
re-delegates with the answer — see "Subagent conversation memory and interactive questions" for the
seam and for what does NOT survive that round trip. Per that plan's own conclusion, do NOT create a
specialist just to hide the number of CRUD tools in a domain — self-contained CRUD domains go flat
(on the hub, pinned or catalog-demoted per the schema-pressure guidance above; on Copilot and MCP,
simply registered) on whichever surface needs them.
Reserve a specialist for genuine multi-step reasoning over a domain (workflow editing, code
generation, research) that a flat tool list cannot express. Adding a subagent means:
enum entry in `AiHubAgentType` (auto-registered via `AiHubAgentTypeProvider`), a
`*Configuration` with a ChatClient bean + prompt resource + static `create*ToolCallback` factory,
registration via `ObjectProvider.ifAvailable`, and prompt documentation on the parent agent.

**Consolidated pinned tools** (AI Hub only; per-kind variants remain on the Copilot surface):
- `openResourceTab({type, name, ...ids})` replaces the seven per-resource open*Tab tools. The
  result echoes `type` plus the legacy field names; `AiHubRuntimeProvider` re-dispatches onto the
  legacy client branches. `openWorkflowChatTab` stays separate — its result drives a chat switch,
  not a resource-panel tab.
- `lookupPropertyOptions` / `selectPropertyOption` take `kind: ACTION | TRIGGER` +
  `operationName` (classes `LookupComponentPropertyOptionsToolCallback` /
  `SelectComponentPropertyOptionToolCallback`). The `selectPropertyOption` name and its
  `select-property-option` marker payload are client-load-bearing — do not rename.

**Subagent delegate model propagation (ticket 732).** Every catalog-backed intelligent delegate — all
ten in the catalog, i.e. the hub's nine plus the embedded-only `buildIntegrationWorkflow`, each
constructed with a `SubAgentChatModelResolver` by its `IntelligentToolContributor` — follows
the `ChatModel` the caller picked in the AI Hub composer or a Copilot panel toolbar; the management
MCP surface does not, structurally —
`WorkspaceScopedSubAgentToolCallback` never writes `AgentToolInvocationContext
.TOOL_CONTEXT_LLM_PROVIDER_KEY`/`TOOL_CONTEXT_LLM_MODEL_KEY` into the context it forwards, so those
delegates always run on the default. Resolution flows tool-context keys (populated by
`AiHubSpringAIAgent#toolContext` / `CopilotToolContextUtils#toToolContext`) →
`SubAgentChatModelResolver` (EE `CatalogSubAgentChatModelResolver`) →
`IntelligentToolChatClientFactory#get(ChatModel)`, which rebuilds the delegate's `ChatClient` over the
picked model with the same prompt and tools it would otherwise use. The hub's precedence —
user-selected beats task override, in `AiHubChatClientResolver` — must stay in step with what those
keys carry, or a delegate runs on a different model than the one its caller turn resolved to; and
resolution is fail-open everywhere (`SubAgentChatModelResolution`): a missing resolver, a missing
pick, or a resolver that throws all fall back to the delegate's default client, so a model preference
never fails a turn.

### Subagent conversation memory and interactive questions (EE, ticket 732)

Two features, and they no longer share a package. Memory is EE, in
`com.bytechef.ee.ai.hub.subagent`. The ask capability straddles editions: the seam, the decorator and
the renderer are CE in `com.bytechef.ai.copilot.tool.ask` (they have to be — the catalog that wires
them is CE), the channel, tool and relay implementation are EE in that same `…hub.subagent` package.
Specs: `docs/superpowers/specs/2026-08-07-subagent-conversation-memory-design.md` and
`docs/superpowers/specs/2026-08-07-subagent-interactive-questions-design.md` — read the second for
rationale, not wiring: it still describes the deleted `SubAgentToolCallback`/allowlist shape rather
than the catalog seam below.

**The contributor seam.** `SubAgentGuardrailedChatClient` does not hardcode advisors — it dispatches a
`List<SubAgentAdvisorContributor>`, each of which takes the forwarded `ToolContext` and returns the request
spec (a spec, not a `List<Advisor>`, because a contributor may need to set advisor *params*). The class is
~250 lines of `ChatClientRequestSpec` delegation boilerplate, which is exactly why a second decorator was
rejected: every method added upstream would have to be implemented twice. Its guardrails-era name is kept
deliberately. Two contributors ship today: `WorkspaceAdvisorContributor` (guardrails + workspace system
prompt) and `SubAgentSessionMemoryContributor` (this section) — both wired from
`AiHubConfiguration#wrapDelegate` and ONLY from there — which is what makes memory hub-only (below).

**Memory.** Each specialist gets a durable per-conversation session keyed
`<parentThreadId>:<agentTypeKey>` (`SubAgentSessionMemoryContributor.sessionKey`, public and unit-tested —
a bug there crosses conversations) over the SAME session store the parent uses, under
`AiHubSessionMemory.SESSION_USER_ID`. Resolution is per request from
`AgentToolInvocationContext.TOOL_CONTEXT_CONVERSATION_ID_KEY`, because delegate `ChatClient` beans are
singletons shared by every workspace *and every conversation*. `MAX_EVENTS = 10` is a **read** filter
(`EventFilter.lastN`) — it bounds replay, not storage; compaction would rewrite history and is deliberately
not used. Advisor order is the DEFAULT precedence, placing it OUTSIDE the tool-calling loop — unlike the
parent, which uses `TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER` to capture its full tool transcript; a
specialist's tool traffic is mostly listing calls it re-runs anyway. Fail-open on session-store errors, and
absent when the `ToolContext` carries no conversation id (the MCP manager surface, which stays stateless).

**Memory is hub-only, and that asymmetry is load-bearing.** `SubAgentSessionMemoryContributor` is
attached in `AiHubConfiguration#wrapDelegate` and nowhere else. Every other surface that builds the
same delegates passes an identity `chatClientDecorator` — both Copilot panel configurations
(`ProjectAgentConfiguration`, `McpServerAgentConfiguration`, via `IntelligentToolCatalog#getForPanel`)
and all three management-MCP contributors (via `#getByNames`) — and the MCP surface carries no
conversation id to key a session on in the first place. **A specialist on a panel or over MCP has NO
memory across delegations**: the call that brings a user's answer back starts from nothing. Nothing
user-facing may say otherwise. A shipped text once told MCP clients a specialist resumes "with its
context intact"; it was false, and the ask tool's description, its stop instruction,
`SubAgentQuestionFormatter`'s re-invocation sentence and `ManagementMcpServerConfiguration`'s
`instructions` were each corrected to tell the caller to restate what the specialist needs.

`agentTypeKey` MUST be a key registered with `AgentTypeRegistry` (which gained `keys()` for this): task
delete reconstructs the per-specialist keys from that registry to purge them, since `SessionRepository` has
no prefix listing. Note the skills specialist keys on `CopilotAgentType.SKILLS` (`"skills"`), NOT its
`"authorSkill"` tool name — there is no `authorSkill` agent type, and an unregistered key would
leave a session nothing ever deletes. Every AI Hub delegate site goes through
`AiHubConfiguration#wrapDelegate`, so no hub delegate can forget the memory contributor — but that
method is the hub's alone, not a shared path (see above).

**Interactive questions.** A delegated specialist can put a real multiple-choice question to the
user. It calls `askUserQuestion` (`SubagentAskUserQuestionToolCallback`, EE, which DECORATES the CE
`AskUserQuestionToolCallback` so the payload is byte-identical to the main agent's and inherits its
strict input validation); that writes the envelope into `SubagentAskChannel` — thread-bound,
`ThreadLocal` restored rather than cleared so nesting is LIFO-safe, holding at most ONE question per
delegation — and returns a stop instruction. `SubAgentAskRelayToolCallback` (CE) wraps the delegate,
runs the delegation through the `SubAgentAskRelay` seam (CE interface, EE impl
`SubagentAskChannelRelay`) and, when a question was raised, returns the payload as **its own** tool
result — which is what puts it on the parent's stream, since a delegate tool result IS a main-agent
tool result. The specialist's text summary is discarded in that branch by design.

**One seam, not per delegate.** Both the tool attachment and the wrapper are applied in
`IntelligentToolCatalog#buildToolCallback` — the private per-definition method that is the sole build
path behind BOTH `getByNames` and `getForPanel`. Every intelligent delegate is therefore ask-capable
from one edit and a delegate added later inherits it; there is no allowlist left to decay. The ask
tool is attached to the delegate's own `ChatClient` innermost, before the per-surface
`chatClientDecorator`, via `mutate().defaultTools(...)`, so a surface that wraps the client still
delegates to one carrying it. `SubAgentAskRelay` is an optional bean: absent it (a deployment without
the EE module) nothing is attached and nothing is wrapped, which is exactly the pre-ask behaviour.

**The one opt-out, and why it is a flag and not a set.** `IntelligentToolDefinition#askCapable()`
defaults to `true`; the converter delegate (`importWorkflow`) returns `false`, and the seam skips both
the tool attachment and the wrapper for it. That is not an oversight in the seam — it is the one
delegate for which "no allowlist" is wrong: `prompt_converter_build.txt` line 4 tells it to NEVER ask
and to answer with valid JSON and no explanations, so an attached tool inviting it to ask contradicts
its own prompt, and a question envelope returned where the caller expects a workflow definition is a
silent contract break. It lives on the definition rather than as a name-keyed `Set` in a configuration
class deliberately: that was the old `ASK_CAPABLE_AGENT_TYPE_KEYS` shape, whose failure mode was that
the gate sat far from what it gated and could silently empty. `IntelligentToolSurfaceParityTest`
pins both the flag and its premise (the prompt still carrying the never-ask directive), and asserts
every other definition is ask-capable.

**The renderer is per-surface.** `getByNames`/`getForPanel`'s four-argument forms default to
`SubAgentQuestionRenderer.JSON` — the AI Hub and both Copilot panels keep it, because their client
renders the `ask-user-question` envelope as a choice card. The three management-MCP contributors (CE
`ToolCallbackContributorConfiguration`, EE `AutomationCopilotMcpContributorConfiguration`, EE
`EmbeddedCopilotMcpContributorConfiguration`) pass `PLAIN_TEXT`, which `SubAgentQuestionFormatter`
renders as numbered human-readable text: there is no AG-UI stream and no envelope renderer there.

**Two invariants, and why.** The ask tool returns a **stop instruction, never the payload** —
returning the payload would make the specialist read its own question back as the answer and
fabricate the user's decision, a failure that is invisible because the run completes normally with a
plausible answer. And `SubAgentAskRelay#runWithChannel` returns `AskOutcome(result, pendingQuestion)`
rather than exposing a post-call `pending()` read: a delegate `ToolCallback` is a singleton serving
every concurrent delegation, so any stash outliving the binding would let two conversations race and
surface one user's question in another's chat. Read `pending()` only from inside `runWithChannel`.

**Two classes from the old stack did NOT come back and must not be described as existing**:
`SubAgentToolCallback` (the relay logic used to live inside it; today's delegates are per-domain
callback classes rather than one shared class, which is why that logic had to become a wrapper
applied once at the catalog) and `SubAgentAskToolContributor` (the
`ASK_CAPABLE_AGENT_TYPE_KEYS` gate, which the catalog seam replaced outright). Surviving javadoc
mentions of either are historical.

The contract reaches the specialist through the **tool's description**
(`SPECIALIST_CONTRACT_GUIDANCE`), not its prompt: every specialist prompt file is shared with a
Copilot panel agent, only `WORKFLOW_EDITOR_ASK`/`BUILD` have an `askUserQuestion` at all, and the
panel tool's contract is the opposite (it returns the envelope and keeps going) — one shared file
cannot state both truthfully, and naming a tool an agent lacks kills the turn.

Client: `toToolResultDataPart` dispatches on tool NAME first, then falls back to the payload's
`kind`, scoped to known kinds — a delegate tool is named e.g. `buildWorkflow`, so no name branch
would ever match. The fallback parses silently behind a `{` guard rather than via `parseJson`, which
would log a warning for every plain-text tool result in the app.

Known gaps, all deliberate: one question per delegation (`SubagentAskChannel.offer` returns `false`
on a second, and the tool turns that into a tool error); nothing enforces that the specialist stops
after asking (tool description and stop instruction only); `truncateMessagesFrom` does not rewind
specialist memory.

### Auto-memory storage providers

`bytechef.ai.auto-memory.provider` selects where AI auto-memories are stored:

- `JDBC` (**default**) — the relational `ai_auto_memory` table, which carries a nullable `workspace_id`
  column (the `workspace_ai_auto_memory` relation table was collapsed into it). Existing deployments are
  unaffected; the JDBC binding is gated with `matchIfMissing = true`.
- `FILESYSTEM` / `AWS` — one JSON object per memory through the shared `FileStorageService`
  (`platform-ai-auto-memory-repository-file-storage`). Both values use the SAME CE implementation and
  differ only in which service `FileStorageServiceRegistry` resolves, so `AWS` needs no module of its
  own — it works wherever the EE `file-storage-aws` module is on the classpath and configured.

Exactly one binding is ever active. Selecting a provider whose `FileStorageService` is not registered
**fails fast at startup** rather than falling back, since a fallback would write memories to storage
the operator did not choose.

File-backed layout:
`ai_auto_memory/{workspaceId}/{principalType}_{principalId}/{environment}/{id}.json`. Path segments use
only lowercase alphanumerics and `_` — `FilesystemFileStorageService` normalizes directories to that
alphabet, so a hyphen is stripped and distinct names could otherwise collide.

Limitations, by design: file backends are **best-effort single-writer** (whole-object writes,
last-write-wins on concurrent edits of the same memory, no cross-object transaction); the duplicate-name
check stays the service layer's job as it already is for JDBC; ordering comes from the stored
`updatedAt` because `FileEntry` carries no modification time; and there is **no migration path between
providers** — switching does not move existing memories.

### Copilot domain agent module map (revised 2026-08-05)

Copilot domain agents live one-config-per-activation-profile, per edition — NOT in ai-hub-service
(which holds only hub-owned agents: research, data_analyst, image_generator,
slide_builder):

- CE `ai-copilot-service`: `CopilotConfiguration` (copilot-only: workflow_editor, code_editor,
  cluster_element, skills, workflow_execution, converter, json_schema_builder, sample_output),
  `DataTableAgentConfiguration` (copilot∨hub), `AiAgentAgentConfiguration` (copilot∨hub — wraps
  `AiAgentFacade`, the automation-ai-agent domain), `KnowledgeBaseAgentConfiguration` (copilot∨hub ∧
  `bytechef.ai.knowledge-base.enabled`).
- EE `automation-ai-copilot`: `AutomationCopilotConfiguration` (copilot∨hub: custom_component +
  code_workflow — panel `*SpringAIAgent`s AND one-shot `*SubAgentChatClient`s in one class),
  `ContextStoreAgentConfiguration` (copilot∨hub ∧ `bytechef.context-store.enabled` ∧ `@ConditionalOnEEVersion`
  — the authorization-enforcing `ContextStoreSourceFacade` its tool factory needs is itself EE-gated. Consequence:
  on `edition != ee` with the context store enabled, the Copilot context-store panel agents are simply absent, and
  both surfaces tolerate that via `ObjectProvider`/`List` injection).

Per domain and mode there is ONE prompt file shared by the panel agent and the subagent client (no
`_copilot_` prompt twins). `*AskSubAgentChatClient` beans feed only the hub ASK agent;
`*BuildSubAgentChatClient` beans feed the hub BUILD agent AND the management MCP surface. MCP
contributors follow the beans' edition: CE `ToolCallbackContributorConfiguration` (ai-copilot-service)
contributes CE-bean domains; EE `AutomationCopilotMcpContributorConfiguration` (automation-ai-copilot)
contributes context_store/custom_component/code_workflow. A CE class must not hold `@Qualifier`
references to EE bean names. Panel agents are dispatched by `CopilotApiController` collecting every
`LocalAgent` bean by agentId (`<source>_<mode>`); adding a domain needs a controller branch + a client
`Source` enum entry (lowercase value = URL segment) + an editor trigger + post-turn invalidation via
`useCopilotPostTurnRegistry`. The automation-owned CRUD domains (API collections, project deployments,
MCP servers) have no subagent at all any more: their reads are flat tools on the hub ASK agent and
their writes flat on BUILD, so there is no ask/build ChatClient pair to split.

### Domain copilot slice pattern (context store / knowledge base / data table)

Each domain slice follows the same shape (see `docs/superpowers/plans/` for the slice plans):
shared tool callbacks + a `*ToolCallbacksFactory` (read list feeds ASK, write list feeds BUILD)
in `automation-ai-tool`; a `<Domain>AgentConfiguration` (module per the map above) defining the
source-panel agent; a source enum entry on both surfaces; the client detail page gets a copilot
trigger + post-turn query invalidation. The delegate half of this shape is GONE — there is no
`<domain>_agent` callback in `ai-copilot-tool` for any of these three (the six `*AgentToolCallback`
classes left in that package all back catalog-registered intelligent tools), and the AI Hub registers
each domain's tools directly: the read leg pinned on BOTH agents, the mutation leg catalog-demoted on
BUILD. Do not add a delegate for a new CRUD domain (see the AI Hub agent tool architecture rules of
thumb).
