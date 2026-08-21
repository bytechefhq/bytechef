# Agents — design

**Date:** 2026-08-10
**Status:** Approved (brainstorming session)
**Edition:** CE

## Summary

An **Agent** is a first-class automation entity modeled on n8n Agents
(<https://docs.n8n.io/build/build-and-manage-agents>): one AI agent you configure once —
instructions, model, tools, skills, sub-agents, knowledge base, memory — and reach over many
channels at once (hosted chat, workflow call, schedule, Slack, Telegram, WhatsApp, Rocket.Chat).

Under the hood an Agent is a **generated single-workflow project**: multiple triggers fanning
into a `branch_in` dispatcher that normalizes the incoming payload, one `aiAgent/v1/streamChat`
node carrying every configured cluster element, and a `branch_out` dispatcher that routes the
reply back out on the channel that fired. The Agent entity owns the definition; the workflow is
regenerated wholesale on every save and is not user-editable (no canvas viewer in v1).

Reuses wholesale: the AI Agent component and its cluster-element ecosystem, the branch task
dispatcher, project publish/versioning, project deployments (+ `ProjectDeploymentDialog`),
webhook execution, and `workflow/v1/callWorkflow` for sub-agent composition.

## Decisions log

| Decision | Choice | Rejected alternatives |
| --- | --- | --- |
| Agent identity | Separate `agent` entity owning a hidden system project (`__AI_AGENT__<uuid>`) | Marker column on Project; derived (no marker); extending EE `AiHubPersonalAgent` |
| Channel normalization | UI-generated `branch_in`/`branch_out` branch dispatchers | New normalize/reply component actions; agent-node-resolves-input; one workflow per channel |
| Workflow ownership | Agent entity is the source of truth; definition fully generated; **no canvas viewer** in v1 | Canvas as alternate read/write view; hybrid locked-region canvas |
| Publish model | **Explicit Publish button** (draft edits → publish → deployment upgrade) | Auto-publish on save; deployments tracking drafts |
| Sub-agents | Existing `workflow/v1/callWorkflow` tool targeting the sub-agent's `workflowCall_1` trigger | New `callAgentTool`; inline `AiAgentUtilsSubagent` personas; A2A client tool |
| v1 channels | chat + workflowCall + schedule + Slack/Telegram/WhatsApp/Rocket.Chat (existing triggers only) | Defaults only; building Discord/Teams/Mattermost triggers in v1 |
| Edition | CE (`server/libs/automation/automation-agent`) | EE; CE core + EE channels |
| Agent node action | `aiAgent/v1/streamChat` (hosted chat streams; async channels use the final text) | `aiAgent/v1/chat` |

## Domain model

New CE module group `server/libs/automation/automation-agent` (`-api`, `-service`, `-graphql`),
following the MCP-server module shape. Authorization follows the workspace convention (nullable
`workspace_id` column); writes go through an `AgentApiFacade` that owns the ownership/admin
checks, per the API-facade-owns-authorization rule.

### `agent`

| Column | Type | Notes |
| --- | --- | --- |
| `id` | BIGINT PK | |
| `name` | VARCHAR(64) | slug, `^[a-z0-9_-]{1,64}$` (same pattern as `AiHubPersonalAgent`) |
| `title` | VARCHAR(255) | display label |
| `description` | VARCHAR(1024) | doubles as the sub-agent tool description |
| `instructions` | TEXT | system prompt for the aiAgent node |
| `workspace_id` | BIGINT NULL | workspace convention; null = none |
| `project_id` | BIGINT FK → project | the hidden backing project |
| `uuid` | UUID | stable external identity |
| audit columns | | `created_by/date`, `last_modified_by/date`, `@Version` |

### `agent_channel`

The trigger list. `chat` and `workflowCall` rows are created with the agent and are
**permanent — they cannot be deleted**.

| Column | Type | Notes |
| --- | --- | --- |
| `id`, `agent_id` | | FK CASCADE |
| `channel_type` | VARCHAR | `chat`, `workflowCall`, `schedule`, `slack`, `telegram`, `whatsapp`, `rocketchat` — STRING, not ordinal, matched against the channel registry |
| `position` | INT | stable generation order |
| `parameters` | JSON | per-channel config: schedule `{cron, prompt}`; messaging filters. workflowCall's input schema is predefined (`{message, conversationId?}`), not stored config |
| `connection_id` | BIGINT NULL | design-time connection for connected channels |

### `agent_element`

Everything attached to the aiAgent node.

| Column | Type | Notes |
| --- | --- | --- |
| `id`, `agent_id` | | FK CASCADE |
| `kind` | VARCHAR | `MODEL`, `TOOL`, `SKILL`, `SUB_AGENT`, `KNOWLEDGE_BASE`, `CHAT_MEMORY`, `AUTO_MEMORY` (GUARDRAIL reserved, post-v1) — STRING, not ordinal |
| `reference_id` | BIGINT NULL | skill id / knowledge base id / sub-agent's `agent.id` |
| `parameters` | JSON | e.g. model `{provider, model}`, tool `{componentName, componentVersion, actionName, ...}`, KB retrieval params |
| `connection_id` | BIGINT NULL | model/tool connections |
| `position` | INT | stable generation order |

Exactly one `MODEL` element is required before publish. `CHAT_MEMORY` defaults to present on
creation (built-in chat memory on).

### System project

The backing project is named `__AI_AGENT__<uuid>`; the prefix is appended to
`SystemProjects.NAME_PREFIXES`. **Included cleanup:** the drifted hand-rolled SQL exclusions in
`CustomProjectRepositoryImpl` (only `__EMBEDDED__%`, `__CONTEXT_STORE__%`) and
`CustomProjectDeploymentRepositoryImpl` (only `__API_COLLECTION__%`, `__MCP_SERVER__%`,
`__CONTEXT_STORE__%`) are consolidated to build their predicates from `SystemProjects`
(single source of truth), fixing the missing prefixes and escaping `_` (a `LIKE` wildcard
today).

### Delete rules

- Deleting an agent deletes its backing project (cascading workflows/versions) and is blocked
  while deployments exist or another agent references it as a sub-agent
  (typed errors `AGENT_HAS_DEPLOYMENTS`, `AGENT_REFERENCED_AS_SUB_AGENT`).
- `chat` and `workflowCall` channels: non-deletable, enforced in the facade.

## Workflow generation

`AgentWorkflowGenerator` (in `automation-agent-service`) renders the complete definition from
the agent aggregate on every save, writing it to the backing project's single workflow through
the existing draft-editing path. Generation is **deterministic** — elements ordered by
`position`, stable node names — so regenerating an unchanged agent yields a byte-identical
definition and project-version diffs stay meaningful.

### Generated shape

```
triggers:
  chat_1          chat/v1/newChatRequest          (permanent, sync)
  workflowCall_1  workflow/v1/newWorkflowCall     (permanent, sync;
                                                   inputSchema {message, conversationId?})
  schedule_N      schedule/v1/...                 (one per schedule channel row)
  slack_N, telegram_N, whatsapp_N, rocketchat_N   (one per messaging channel row)

tasks:
  branch_in    branch/v1
               expression: ${__triggerName}
               one case per channel; each case's task list ends in a var task whose output
               is the envelope:
                 {channel, text, conversationId, attachments, replyTo}
  aiAgent_1    aiAgent/v1/streamChat
               prompt: ${branch_in.text}
               attachments: ${branch_in.attachments}
               systemPrompt: <agent.instructions>
               conversationId (chat memory key): ${branch_in.conversationId}
               clusterElements: generated from agent_element rows
  branch_out   branch/v1
               expression: ${branch_in.channel}
               chat         → chat/v1/responseToRequest       (message: the aiAgent_1 output's
                              text field, exact path per streamChat's output schema)
               workflowCall → workflow/v1/responseToWorkflowCall
               telegram     → telegram/v1/sendMessage         (chatId: ${branch_in.replyTo})
               slack / whatsapp / rocketchat → their send actions analogously
               schedule     → empty case (no reply; the run's value is tool side effects)
```

Verified branch mechanics this relies on: a branch's output is its **last executed sub-task's
output** (`BranchTaskCompletionHandler`), so `${branch_in.text}` resolves; cases hold task
*lists*, so a channel mapping may be multi-step.

### Channel registry

> **Superseded by `2026-08-17-sdk-agent-channels-design.md`.** The hand-maintained registry described
> below was built and then removed: components now declare their own channels through the SDK
> (`agentRequest()` / `agentReply()` / `agentChannel(...)`) and the agent module discovers them.
> `replyTo` is gone — it was always character-identical to `conversationId`. The rest of this section
> is kept as the record of what was originally designed.

Code, not data: a `ChannelDefinition` record per channel type in `automation-agent-service`
declaring trigger workflow-node-type, whether a connection is required, envelope mapping
expressions (`text`, `conversationId`, `attachments`, `replyTo`), and the reply task template.
Adding a future channel (Discord, Teams, Mattermost) = component trigger/action work + one
registry record; the Agents UI picks it up automatically.

`conversationId` doubles as the chat-memory key, giving per-conversation isolation per channel:

| Channel | conversationId | replyTo |
| --- | --- | --- |
| chat | native `conversationId` | — (sync response) |
| workflowCall | caller-supplied or fresh UUID | — (sync response) |
| schedule | fresh UUID per run (no continuity) | — (no reply) |
| slack | channel + thread_ts | channel (+ thread_ts) |
| telegram | chat id | chat id |
| whatsapp | sender phone | sender phone |
| rocketchat | room id | room id |

### Engine change: `__triggerName` discriminator

Job inputs today carry the fired trigger's output under *its own name only*
(`WebhookWorkflowExecutorImpl` seeds `Map.of(triggerName, output)`), so no expression can
portably ask "which trigger fired". We add a reserved input key **`__triggerName`** seeded
alongside the trigger output at every job-creation path that seeds a trigger output:

- `WebhookWorkflowExecutorImpl.createJobParameters` (choke point for all webhook shapes)
- `WebhookWorkflowSyncExecutor`
- `WorkflowContinuationHelper` (websocket continuation)
- `TriggerCompletionServiceImpl` (polling / schedule path)
- MCP facade, A2A facade (callable paths)
- Subflow dispatch (`SubflowTaskDispatcher` / `AgentSubflowLauncher`) — so a sub-agent invoked
  via `callWorkflow` routes its `workflowCall` case

Platform-generic (useful for error workflows and logging too). Guard: the workflow input-schema
validation rejects user-defined input names starting with `__` so nothing can collide with the
reserved key. If during implementation a metadata-map slot proves cleaner than an input key,
that substitution is acceptable as long as the evaluator can reference it from the branch
expression.

## Element mapping

Every Agents-page element maps to existing machinery:

| Agent UI element | Generated as | Notes |
| --- | --- | --- |
| Model (required) | MODEL cluster element (e.g. `openAi/v1/model`) + connection | exactly one |
| Tool | TOOLS cluster element (component action) + connection | same catalog the canvas offers |
| Skill | `aiAgentUtils/v1/skillsTool` with `skills[].skillId` list | one cluster element carries all selected skills; removed entirely when the last skill is removed |
| Sub-agent | `workflow/v1/callWorkflow` TOOLS element | `toolName`/`toolDescription` from target agent's slug/description, `workflowUuid` = target's generated workflow, inputs `{message}` |
| Knowledge base | `questionAnswerRag/v1/rag` + `knowledgeBase` VECTOR_STORE child | reference by KB id; retrieval params (topK, threshold) in element parameters |
| Chat memory | built-in chat memory cluster element, key `${branch_in.conversationId}` | default on |
| Auto memory | `aiAgentUtils/v1/autoMemoryTool` | long-term memory toggle |
| Schedule | not a cluster element — an `agent_channel` row (trigger + branch_in case `{text: <stored prompt>}`) | each schedule carries its own prompt |
| Guardrails | post-v1 UI; EE workspace guardrails already apply via `AiGuardrailsAdvisorProvider` SPI | |

### Sub-agent semantics (inherited from `callWorkflow`)

- **Nesting is one level deep at runtime**: a sub-agent runs as a subflow job
  (`parentTaskExecutionId != null`), and `WorkflowCallWorkflowTool` refuses to suspend from a
  subflow (`ERROR_AGENT_IS_SUBFLOW`). A→B→A therefore cannot deadlock — it fails at B with an
  LLM-readable error. Save-time behavior: **warn** ("B has sub-agents; they won't be callable
  when B runs as a sub-agent"), no hard block.
- **One suspending tool call per LLM turn** (`ERROR_ALREADY_SUSPENDED`); sequential turns may
  each call a sub-agent.
- Resolution requires the target agent published + enabled in the current environment; failures
  surface as `callWorkflow`'s existing LLM-readable error tokens.

## Publish lifecycle & deployments

### Lifecycle

- **Save** — regenerates the draft workflow in place through existing draft editing. No version
  churn; live deployments are untouched.
- **Publish** (header button) — facade op: validate → regenerate deterministically → existing
  `publishProject`. Typed validation errors: `MODEL_MISSING`, `CHANNEL_CONNECTION_MISSING`,
  `SUB_AGENT_NOT_PUBLISHED`. The header shows an "unpublished changes" indicator derived the
  same way projects do.

### Agent Deployments page

Sidebar entry near Project Deployments (folded into the automation "Deployments" sidebar
group). Listing = deployments of agent-backed projects only (join through `agent.project_id`);
the `__AI_AGENT__` prefix keeps them **out** of the ordinary Project Deployments listing via the
consolidated `SystemProjects` filter.

- **Creating/editing a deployment reuses `ProjectDeploymentDialog`**, exactly as API
  Collections does (`ApiCollectionListItem` renders it against the backing project): pick
  environment + version, wire per-trigger/per-tool connections on the standard connections
  step, set enabled.
- **Per-trigger detail rows** — expanding a deployment lists its channels from the deployed
  workflow's triggers, each with what's needed to connect:
  - chat → hosted chat URL
  - workflowCall → note that it serves sub-agent/workflow calls
  - slack/telegram/whatsapp/rocketchat → the static webhook URL to register with the provider
    (copy button) + connection status
- **Enable/disable toggle** = existing `enableProjectDeployment`, which already
  enables/disables the workflow (and thus trigger registration) beneath. No new semantics.

## Client

- **Sidebar**: "Agents" below Projects. Routes `/automation/agents`, `/automation/agents/:id`.
- **Agents list**: MCP-servers-style rows — title, description, badges (published /
  unpublished changes / N deployments enabled); create dialog (title + description, slug
  derived); delete blocked per the delete rules.
- **Agent detail** (the "elements instead of workflows" view):
  - Header: title, unpublished-changes dot, **Publish**, link to Agent Deployments filtered to
    this agent.
  - Instructions textarea + Model picker at the top.
  - Element sections (add/remove lists): Triggers (chat + workflowCall pinned, non-deletable;
    add schedule/Slack/Telegram/WhatsApp/Rocket.Chat; per-row config popover — schedule
    cron + prompt, channel connection + filters; workflowCall's predefined input schema is
    shown read-only), Tools, Skills, Sub-agents, Knowledge Base, Memory (chat + auto toggles).
  - **Test chat panel** on the right: converses with the *draft* workflow's chat trigger via
    the existing webhook trigger test facade, before publishing.
- **GraphQL** follows the MCP-server slice shape: queries `agents(workspaceId)`, `agent(id)`;
  mutations `createAgent`, `updateAgent`, `add/update/deleteAgentChannel`,
  `add/update/deleteAgentElement`, `publishAgent`. Codegen + react-query per the standard
  workflow.

## Testing

- **Generator unit tests** (the heart): agent fixture → definition snapshot (JsonFileAssert
  pattern); determinism (regenerate twice → byte-identical); one mapping test per channel
  registry entry (envelope expressions + reply task).
- **Facade IntTests**: CRUD; publish creates a project version; delete blocking; `__AI_AGENT__`
  exclusion from project/deployment listings (extends `ProjectServiceIntTest`); typed publish
  validation errors.
- **Discriminator tests**: `__triggerName` seeded on webhook, sync, continuation,
  schedule/polling, callable, and subflow paths; `__`-prefixed input-name rejection.
- **End-to-end**: run a generated two-channel workflow through the sync executor with a chat
  payload and a messaging payload; assert `branch_in` routes, the agent node runs, and
  `branch_out` replies on the firing channel only.
- **Client**: vitest for the pages/stores per existing conventions.

## Error handling (runtime posture)

- A failed reply send fails the job — visible in executions, never silently dropped.
- Schedule runs complete normally through their empty `branch_out` case.
- Sub-agent failures surface as `callWorkflow`'s LLM-readable tool-result errors; the agent
  turn continues.
- EE guardrails/workspace system prompts apply unchanged through their existing advisor SPIs.

## Out of scope (v1)

- Canvas viewer / "Convert to Project" escape hatch for agents.
- Discord, MS Teams, Mattermost channels (need component triggers first; then one registry
  record each).
- Guardrails UI on the agent page (EE workspace-level guardrails still apply).
- Inline subagent personas (`AiAgentUtilsSubagent`) in the Agents UI.
- Duplicating agents; agent-level analytics.

## Relationship to existing "agent" features

- **EE Personal Agents (`ai_hub_personal_agent`)** remain a distinct, chat-surface-scoped
  feature; no schema or code sharing. Naming in UI copy should say "Personal Agents" (AI Hub)
  vs "Agents" (automation) consistently.
- **MCP / A2A**: an Agent's `workflowCall_1` trigger makes its workflow exposable through the
  existing MCP workflows-as-tools and A2A surfaces with zero extra work — post-v1 UI may
  surface this on the deployment detail.
