<!-- Extracted from CLAUDE.md so the agent-facing reference does not sit in every prompt.
     Load this when working on the Agents feature (server/libs/automation/automation-ai/automation-ai-agent/*,
     client/src/pages/automation/agents + agent-deployments). -->

# Agents (automation)

A chat-first alternative to hand-built workflows: a user configures a persona (model,
instructions, tools) and a set of inbound channels; the feature generates and owns a regular
ByteChef workflow definition behind the scenes. Spec:
`docs/superpowers/specs/2026-08-10-agents-design.md`. Plan (task-by-task history):
`docs/superpowers/plans/2026-08-10-agents.md`.

## Entity model

Three tables, all under `server/libs/automation/automation-ai/automation-ai-agent/automation-ai-agent-api/.../domain/`,
Liquibase changelog
`automation-ai-agent-service/.../config/liquibase/changelog/automation/ai_agent/00000000000001_automation_ai_agent_init.xml`:

- **`ai_agent`** (`AiAgent.java`) — `name` (slug, `^[a-z0-9_-]{1,64}$`, enforced in `setName` AND by a DB
  check constraint `ck_ai_agent_name_slug`), `title`, `description`, `instructions` (system prompt
  text), `workspaceId`, `projectId` (FK to the hidden backing project — see "System project"
  below), `uuid`, `settings` (nullable JSON — per-agent built-in-tool on/off switches, see "Built-in
  tool settings" below).
- **`ai_agent_channel`** (`AiAgentChannel.java`) — `agentId` (FK, cascade delete), `channelType`
  (string: whatever key a component's `agentChannel(...)` declaration named — `slack`, `telegram`,
  `whatsapp`, `rocketchat`, `twilio`, `infobip` today — plus `chat`, `workflowCall` and `schedule`,
  the only three with constants in `AiAgentChannelType`; see "Channels are declared by components"
  below), `position` (trigger ordering), `parameters` (JSON), `connectionId`.
- **`ai_agent_element`** (`AiAgentElement.java`) — `agentId` (FK, cascade delete), `kind` (`MODEL`,
  `TOOL`, `SKILL`, `SUB_AGENT`, `KNOWLEDGE_BASE`, `CHAT_MEMORY`, `APPROVAL_GATE`,
  `APPROVAL_TOOL` — the last two are HITL approvals, see below), `referenceId`
  (nullable — e.g. the target `AiAgent.id` when `kind=SUB_AGENT`, a skill id when `kind=SKILL`),
  `parameters` (JSON), `connectionId`, `position`. There is no `AUTO_MEMORY` kind — that built-in is
  now controlled by `ai_agent.settings.builtInTools.autoMemory` instead of a stored row (branch-only
  feature, retired with no compat shim).

- **`ai_agent_tag`** (`AiAgentTag.java`) — the `project_tag`/`data_table_tag` join shape: `ai_agent_id`
  + `tag_id`, composite PK, `ai_agent_id` cascade-deletes. A relation table rather than the
  `workspace_id`-column convention because tags are genuinely many-to-many (a tag labels many agents,
  an agent carries many tags) — the sanctioned exception in CLAUDE.md's workspace-scoping rule, which
  governs *reach*, not membership. Read/written through `aiAgentTags(workspaceId)` /
  `updateAiAgentTags(input)`; `AiAgentDTO.tags()` resolves them per agent (an N+1 the list path
  already pays for its Project and workflow reads).

One agent has many channels and many elements. Both FKs cascade-delete at the DB level.

## System project

Every agent owns a hidden backing `Project` named `SystemProjects.AI_AGENT_NAME_PREFIX + uuid`
(`__AI_AGENT__<uuid>`, in `automation-configuration-api`'s `SystemProjects.java`) — this is what
the generated workflow definition actually lives in. `SystemProjects.projectNameNotLikePredicates`
excludes every `__`-prefixed system project (agents, knowledge bases, context stores, embedded
automation) from ordinary project/deployment listings via a SQL `NOT LIKE`
(`CustomProjectRepositoryImpl`, `CustomProjectDeploymentRepositoryImpl`) — an agent's project never
appears in the plain Projects or Deployments pages.

## Generated workflow shape

`AiAgentWorkflowGenerator.generate(agent, channels, elements, subAgentResolver, creatorUserId, channelResolver)`
(`automation-ai-agent-service/.../util/AiAgentWorkflowGenerator.java`) renders the whole aggregate into
one workflow-definition JSON, deterministically (byte-identical output for identical input — no
clock/random reads except a content-derived UUID for the schedule channel's chat-memory key). The
`channelResolver` is what makes it component-agnostic: the facade resolves each channel row once
through `AgentChannelResolver` (below) and hands the generator a lookup, so the generator stays
static and names no component.

```
 chat_1         ─┐                                                    ┌─ reply_chat_1
 workflowCall_1 ─┤                                                    ├─ reply_workflowCall_1
 telegram_1     ─┼─▶ branch_in ─▶ aiAgent_1 ─▶ branch_out ────────────┼─ reply_telegram_1
 slack_1        ─┤                (streamChat, clusterElements)       ├─ reply_slack_1
 schedule_1     ─┘                                                    └─ (schedule: no reply task)
```

`branch_in` and `branch_out` are BOTH keyed on `${__triggerName}` and both carry one case per
channel **row**, keyed by that row's trigger node name — so a reply node is `reply_<nodeName>`, per
row rather than per channel type.

- **Triggers** — one per `AiAgentChannel`, named `<channelType>_<n>` (chat and workflowCall sort
  first, then the rest by `position`; `n` counts per channel type). Trigger type, parameters and
  connection-requirement come from the resolved channel (below), never from a table in this module.
  Row `parameters` reach the trigger node only when they name a property the trigger actually
  declares (`ResolvedAgentChannel.triggerPropertyNames`), which is what keeps UI-only row keys — a
  schedule row's `prompt`/`name`, the cadence fields — out of the definition without any per-channel
  exclusion list.
- **`branch_in`** — a `branch/v1` dispatcher keyed on `${__triggerName}` (the reserved job input,
  below), one case per trigger node name. Each case runs exactly one `var/v1/set` task
  (`envelope_<nodeName>`) that builds a normalized envelope from that channel's payload shape:
  `{text, conversationId, attachments, channel}`, reading `${<node>.<path>}` for each of the three
  contract fields at the paths the channel's own request descriptor declares — so telegram's
  `message.chat.id` and chat's `conversationId` take the same code path. A channel whose descriptor
  binds no attachments path gets a real empty array rather than an expression that reads nothing.
  This is the only inbound-routing task — `AiAgentWorkflowExecutionIntTest` executes just this task
  to verify real per-channel SpEL evaluation. There is no per-channel branch left here except
  `schedule` (see below): the Slack bot-echo guard that used to wrap the slack case now lives inside
  `slack/v1/newMessage` itself, which declines to fire rather than firing and terminating.
  `envelope.channel` is now vestigial — `branch_out` stopped keying on it (below) and nothing else
  reads it; it is still emitted so stored definitions keep their shape.
- **`aiAgent_1`** — a single `aiAgent/v1` node, `streamChat` or `chat` depending on
  `settings.streamResponse` (default ON ⇒ `streamChat`). `userPrompt` and `attachments` read from
  `branch_in`'s envelope; `systemPrompt` is `agent.instructions` (omitted, not empty-string, when
  blank). `clusterElements` are built from `AiAgentElement` rows plus `agent.settings` into
  `tools[]`, in this fixed group order: `TOOL` rows (the HITL gate, if any gated tool exists, sits
  inline at the position of the first gated row — see "HITL approvals" below), then a `SKILL`
  aggregate (`aiAgentUtils/v1/skillsTool`, emitted whenever the agent has any `SKILL` row; its
  `parameters.skills` is a FLAT array of skill ids, since `AiAgentUtilsSkillsTool` declares
  `array("skills").items(integer("skillId"))` and both readers — that tool's `perform` and
  `SkillComponentConnectionFactory.create` — call `getList(SKILLS, Long.class)`), then
  `SUB_AGENT` rows (`workflow/v1/callAiAgent`), then the settings-driven built-ins in
  `AiAgentSettings`' table order (`askUserQuestion`, `autoMemory`, `skillManagement`'s five
  `AiSkill` actions, `webSearch`), then — last of all, never inside the gate — the `APPROVAL_TOOL`
  element (`approval/v1/requestApproval`). A `MODEL` element becomes the `model` entry (its
  `parameters.parameters` nested map — temperature, maxTokens, responseFormat, … — is emitted
  alongside `model`, the same round-trip a `TOOL` row gets; the element's own `model` key is written
  last so a stale copy inside the nested map cannot shadow it); a
  `KNOWLEDGE_BASE` row → one `rag` entry (nesting a `vectorStore`) — `rag` is a MULTIPLE cluster
  element type, so an agent can retrieve from several knowledge bases and the entries are emitted as
  an ARRAY, each carrying its own `topK`/`similarityThreshold`; `AbstractAiAgentChatAction` adds one
  retrieval advisor per entry. `ClusterElementMap.getClusterElements` reads both stored shapes (a
  bare object from a definition written while `rag` was single, or an array) so stored definitions
  never need migrating. A `CHAT_MEMORY` row → `chatMemory`
  (`conversationId` is always `${branch_in.conversationId}`, never read from the element). Node
  names inside `clusterElements` share ONE counter across all groups (`<componentName>_<n>`) so a
  real TOOL row on e.g. the `chatMemory` component can never collide with the singleton
  `chatMemory` entry.
- **`branch_out`** — a `branch/v1` dispatcher keyed on `${__triggerName}`, one case per channel
  **row**, keyed by that row's trigger node name. (It used to key on `${branch_in.channel}`, one
  case per channel *type*. A reply action may take values configured on the row — twilio's WhatsApp
  number, which the reply is sent AS — and two rows of one type would then have had to share a
  single case carrying one of the two numbers.) Each case runs `reply_<nodeName>`, built entirely
  from the reply action's own descriptor: the declared message property receives `${aiAgent_1}` (an
  unnamed string output, not `.text}`), the declared conversation-id property receives
  `${branch_in.conversationId}` when the descriptor names one, each `replyChannelParameters` entry
  copies that row's value into the property it maps to, and `replyFixedParameters` are written
  verbatim (twilio's `useTemplate: false`). A dotted target such as `workflowCall`'s
  `response.message` becomes a nested map, not a key containing a dot. Outgoing attachments are
  deliberately unwired — `aiAgent_1`'s output is a bare string today. `schedule` has no reply task
  (empty case — the run's value is tool side effects).

### Running an agent on a schedule

A `schedule` channel is how an agent runs on a cron — it replaces the removed AI Hub task entity.
Its `parameters` map carries `expression` (a 5-field cron), `timezone`, `prompt` and `name`.

The schedule channel is the one case where `branch_in`'s envelope `text` is a **literal** rather
than an expression over the trigger's output: a cron fire carries no incoming message, so
the resolver's synthesized `schedule` entry carries no request paths at all, and
`AiAgentWorkflowGenerator` substitutes the channel's stored `prompt` directly
(`AiAgentWorkflowGenerator.java`, the one remaining `AiAgentChannelType.SCHEDULE` branch —
schedule is deliberately not a channel, so no component declares it). `aiAgent_1`'s
`userPrompt` is then `${branch_in.text}` as usual. Two consequences follow:

- The prompt is baked into the generated workflow **at save time**, not read at fire time.
- `MapUtils.getRequiredString` rejects only a *missing* prompt; an empty string passes, so a blank
  prompt yields a schedule that fires on time and hands the agent nothing. Client-side validation is
  the only gate.

`prompt` and `name` are deliberately excluded from the trigger's own parameters
(`buildTriggerParameters`) — `schedule/v1/cron` declares only `expression` and `timezone`, and an
undeclared property would be emitted as an unknown trigger input.

The cadence UI is shared client-side: `agentScheduleCron.ts` owns `toCronExpression`,
`toCadenceParameters`, `fromCadenceParameters` and `validateAgentScheduleCadence`, and
`AgentScheduleFrequencyFields` renders the fields belonging to the selected frequency. That
component does **not** render the frequency selector itself — each caller does
(`AgentScheduleDialog` on the detail page, `AgentDialog` at creation time), so a caller that omits
it is silently pinned to one cadence. Both callers write an identical channel payload.

## Channels are declared by components

There is no channel registry in this module any more. `ChannelDefinitions` / `ChannelDefinition` —
one hand-maintained entry per `AiAgentChannelType`, transcribing each component's field names into
expression strings — are **deleted**. A component now declares its own channel through the SDK, and
the agent module discovers it. Spec:
`docs/superpowers/specs/2026-08-17-sdk-agent-channels-design.md`.

**The contract is three fields**: `conversationId` (who to answer), `message` (the incoming text),
`attachments` (incoming files). A channel is a trigger that produces them and, usually, an action
that consumes them. Each end says where they live on it, in its own vocabulary:

```java
// trigger: where the contract's fields live in MY output
.agentRequest(
    agentRequest()
        .conversationId("message.chat.id")
        .message("message.text"))

// action: which of MY properties carry them back
.agentReply(
    agentReply()
        .conversationId("chatId")
        .message("text"))

// component handler: pair them, and name the channel
.agentChannels(
    agentChannel("telegram", TelegramNewMessageTrigger.TRIGGER_DEFINITION,
        TelegramSendMessageAction.ACTION_DEFINITION)
            .title("Telegram")
            .approvalChannel("telegram"))
```

`conversationId` and `message` default to those literal names on the request side, so a trigger whose
output already IS the contract (`ComponentDsl.agentChannelRequest()`) needs a bare `agentRequest()`.
`attachments` deliberately does **not** default: absent means "this channel carries no attachments"
and the generator wires an empty array, so a channel that does carry them must say so — telegram is
the one that genuinely does not. `agentChannel(...)` validates the whole pairing in the component's
static initializer, so a descriptor naming a property the trigger or action does not declare is a
hard component-load failure rather than a silent runtime mis-wire.

Further declaration surface. On `agentChannel(...)`: `.title(...)`/`.description(...)` (the client
renders these; twilio and infobip use them to say "Twilio (WhatsApp)" rather than the bare component
name), `.approvalChannel(...)` (which of the component's `APPROVAL_CHANNELS` cluster elements an
approval reaches a human through — see "HITL approvals" below), and `.triggerParameters(...)`
(parameters the channel pins onto the trigger node, e.g. `workflowCall`'s input schema). On
`agentReply()`: `.attachments(...)` (reserved — see `branch_out` above), `.channelParameter(rowKey,
property)` (a value configured on the channel ROW that the reply needs — twilio's and infobip's
`number` becomes the reply's `From`), and `.fixedParameter(property, value)` (twilio's
`useTemplate: false`, `workflowCall`'s output schema).

**How it reaches the generator.** `ComponentDefinitionService.getAgentChannelDefinitions()` collects
every declaration from the component registry (the full-load path, not the build-time index — `chat`
and `schedule` are Spring-declared handlers and absent from the index), and
`AgentChannelResolver` (`automation-ai-agent-service/.../channel/AgentChannelResolver.java`) projects
one onto a `ResolvedAgentChannel` (`-api`): trigger/reply node types, `connectionRequired`, the
trigger's declared property names and defaults, the reply action's required property names, the
binding, and the approval delivery. `resolveAll()` is what the client's channel cards read, through
the `aiAgentChannelDefinitions` GraphQL query. A stored row naming a channel no component declares —
an uninstalled component, a renamed channel — makes generation throw rather than quietly produce a
workflow missing that trigger. Channel names are globally unique; the registry rejects two components
claiming one name, which can only be detected at full load and so never in a per-module test.

**To add a channel** (Discord, Teams, Mattermost, …): declare a conforming trigger/action pair on the
component and call `.agentChannels(...)`. Nothing else — no constant in `AiAgentChannelType`, no
entry in this module, no client map.

**Declining to fire is a channel's own job.** A trigger that should not start a run returns an empty
collection from its `webhookRequest` (`TriggerCompletionHandler` creates one job for a `null` output
and one per element of a collection, so an empty collection creates zero). This is why the plan added
exactly one operation, `slack/v1/newMessage`: `slack/v1/anyEvent` fires on every subscribed event, so
the agent workflow used to carry a generated bot-echo guard. `newMessage` declines unless the event
is a `message` carrying non-blank text — which covers the bot's own redelivered reply, non-message
events, and `message_changed` alike — and the generator lost its one component-specific branch. It is
an ordinary trigger, usable in hand-built workflows; its output carries the contract's names
(`conversationId`/`message`/`attachments`) rather than Slack's (`channel`/`text`), with property
descriptions explaining the mapping.

**The eight channels**, all declared this way:

| channel | component | trigger | reply action |
|---|---|---|---|
| `chat` | `chat` | `newChatRequest` | `responseToRequest` |
| `workflowCall` | `workflow` | `newWorkflowCall` | `responseToWorkflowCall` |
| `slack` | `slack` | `newMessage` | `sendChannelMessage` |
| `telegram` | `telegram` | `newMessage` | `sendMessage` |
| `whatsapp` | `whatsApp` | `messageReceived` | `sendMessage` |
| `rocketchat` | `rocketchat` | `newMessage` | `sendChannelMessage` |
| `twilio` | `twilio` | `newWhatsappMessage` | `sendWhatsAppMessage` |
| `infobip` | `infobip` | `newWhatsappMessage` | `sendWhatsappTextMessage` |

`chat` and `workflowCall` are ordinary discovered channels; they keep constants in
`AiAgentChannelType` only because the *facade* auto-creates them with every agent and refuses to
delete them. `schedule` is the third constant and is **not a channel** — nothing arrives, there is
nobody to answer, and no component declares it. `AgentChannelResolver` synthesizes its entry from the
real `schedule/v1/cron` trigger so the client's schedule card has a title/description/icon from the
same source as everything else, and it is the generator's one deliberate branch: a schedule's `text`
is the row's stored prompt and its `conversationId` a content-derived UUID.

**Twilio and Infobip are WhatsApp channels**, not SMS ones — which is why both declare
`.approvalChannel("whatsApp")` rather than letting `ApprovalChannelDefinitions`' `sms` default apply.
An approval sent by SMS for a conversation held on WhatsApp would reach a different inbox. Twilio's
trigger also declares a `number` property (the WhatsApp-enabled number the channel listens on) that
its channel maps onto the reply's required `From`; publish validation refuses an agent whose twilio
row leaves it unset, since the reply would otherwise fail at run time.

**`workflowCall`'s input schema** is pinned by the channel declaration
(`WorkflowConstants.AI_AGENT_CALL_INPUT_SCHEMA`) rather than by the trigger's own default, so only
agent-generated workflows get it — a hand-authored `newWorkflowCall` trigger is untouched.
`conversationId` and `message` are both required; the MCP/A2A tool schema derived from it is
therefore strictly tighter than it was, and a caller omitting `conversationId` is newly rejected.

**Unconfirmed payload shapes: `whatsapp` and `infobip`.** Both channels reproduce, verbatim, the
paths the deleted registry used, and neither can be verified from this repo. The `whatsApp` trigger's
declared output nests `entry`/`changes`/`value`/`messages` as single objects while Meta's documented
webhook sends arrays and puts `messages` inside `value`, so the declared schema and the path disagree
about structure as well as array-ness; `infobip` declares no output schema at all, so its path is not
validated even to the first segment. Either both have always worked or they have never worked. The
follow-up is a live webhook confirmation that corrects paths and schemas together — not a guess made
inside a refactor. Consequences today: `whatsapp` is excluded from the client's add-channel menu
(`NON_ADDABLE_CHANNEL_TYPES` in `useAiAgentChannelDefinitions.ts`) and `infobip` is not, which is
historical rather than principled — the same confirmation settles both.

**Known limitation: Slack has no threaded replies.** `slack/v1/newMessage` binds `conversationId` to
the Slack `channel` id, not a thread/message id — the component declares no `thread_ts` field on
either the trigger or `sendChannelMessage`. A slack channel's chat-memory conversation is therefore
scoped to the whole Slack channel, not to an individual thread, and every agent reply lands as a new
top-level message rather than a threaded reply.

## The `__triggerName` reserved input

`JobInputConstants.TRIGGER_NAME_INPUT = "__triggerName"` (`platform-api`) is seeded platform-wide
into every job's inputs at dispatch time (webhook, polling/schedule, websocket continuation, MCP,
A2A, subflow call-sites) so `branch_in`'s `${__triggerName}` expression always resolves. Because
this and every other `__`-prefixed name is reserved,
`WorkflowValidatorFacade.validateNoReservedInputNames`/`validateNoReservedNodeNames`
(`platform-workflow-validator-api`, implemented in `platform-workflow-validator-service`) reject any
hand-authored workflow that declares an input name or a top-level trigger/task node name starting
with `__` (`WorkflowValidatorErrorType.RESERVED_INPUT_NAME` / `RESERVED_NODE_NAME`) — this is what
keeps a human-authored workflow from ever shadowing the platform's own reserved names.

## Publish flow

`AiAgentFacadeImpl` (`automation-ai-agent-service/.../facade/AiAgentFacadeImpl.java`) regenerates and
saves the workflow **draft** after every mutating call (`addAgentChannel`, `updateAgentChannel`,
`deleteAgentChannel`, `addAgentElement`, `updateAgentElement`, `deleteAgentElement`, `updateAgent`)
via `regenerateAndSaveWorkflow` → `AiAgentWorkflowGenerator.generate` → `workflowService.update`. This
keeps the draft always current, but publishing is a **separate, explicit** action:

```java
public int publishAgent(long id, String description) {
    AiAgent agent = agentService.getAgent(id);
    validateForPublish(agent);      // exactly one MODEL; connectionRequired channels have a connection;
                                     // a channel row feeding a REQUIRED reply property has it set;
                                     // every SUB_AGENT target has a PUBLISHED version
    regenerateAndSaveWorkflow(agent);
    return publishProjectVersion(agent.getProjectId(), description);
}
```

`publishProjectVersion` replicates `ProjectFacadeImpl.publishProject`'s body rather than calling it,
because that facade's `@PreAuthorize(hasPermission(Project, ...))` would reject the hidden
`__AI_AGENT__` project (same pattern used by the KB/context-store facades). Each publish bumps
`lastProjectVersion` and duplicates every workflow into a **new** workflow row
(`workflowService.duplicateWorkflow` + `projectWorkflowService.publishWorkflow`) — a published
agent accumulates one workflow row per published version, never mutated in place.
`AiAgentDTO.unpublishedChanges` is a **content diff** between the draft definition and the last
published version's definition, not a timestamp comparison (a timestamp would falsely report
changes immediately after every publish, since `regenerateAndSaveWorkflow` always touches the
draft).

## Deployments

`client/src/pages/automation/agent-deployments/AgentDeployments.tsx` reuses the SAME
`ProjectDeploymentDialog` the ordinary human-authored-workflow Deployments page uses, seeded with a
synthetic `ProjectDeployment` (`projectId`, `projectVersion: agent.lastPublishedVersion`) — no new
deployment UI. Only agents with `lastPublishedVersion > 0` (i.e. published at least once) are
selectable. Backed by the `aiAgentDeployments(workspaceId)` GraphQL query →
`AiAgentGraphQlController.aiAgentDeployments` → `AiAgentFacade.getAgentDeployments`, which does **not**
reuse the ordinary filtered `ProjectDeploymentService` listing methods (those exclude
`__AI_AGENT__` projects by name prefix, per "System project" above) — it instead looks up each
agent's known `projectId` directly per `Environment` via
`projectDeploymentService.fetchProjectDeployment(projectId, environment)`, which has no
name-prefix filter.

## Sub-agents

A `SUB_AGENT` element becomes a `workflow/v1/callAiAgent` cluster element
(`AiAgentWorkflowGenerator.buildSubAgentElement`) — `toolName` (slugified agent title),
`toolDescription`, `agentUuid` (the target `AiAgent`'s own uuid — `CallableAiAgentDataSource.resolveAgent`
maps it to the target's published workflow at dispatch time, so the generator no longer resolves or
embeds a workflow uuid itself), a `message` parameter pre-wired to a `fromAi(...)` expression so the
LLM fills it via the tool's own function-calling schema, and a `conversationId` pinned to
`${branch_in.conversationId}` (the CALLING agent's own envelope conversation id) so every sub-agent
invocation from a given parent conversation lands in the same chat-memory thread — left unset, the
sub-workflow's `workflowCall` trigger's `conversationId` field is whatever (if anything) the LLM
happens to fill in on the tool call, pooling every invocation of that sub-agent across every parent
conversation into one shared thread. The only hard server-side guard is **cycle prevention**:
`AiAgentFacadeImpl.validateSubAgentReference`/`isReachable` walks the SUB_AGENT reference graph and
rejects a reference that would create a cycle (`AiAgentErrorType.SUB_AGENT_CYCLE`, including
self-reference) — arbitrarily deep acyclic chains are NOT blocked by this check. The practical
"1-level nesting" limit instead comes from the shared subflow-tool runtime behavior
(`SubflowToolSupport`, `ERROR_AGENT_IS_SUBFLOW`, used by both `WorkflowCallWorkflowTool` and
`WorkflowCallAiAgentTool`): an agent already running as a subflow refuses to suspend for a further
nested tool call. This is surfaced as a **save-time warning only, never a hard block** —
client-side in `AgentSubAgentsCard.tsx` ("This agent has sub-agents of its own; they won't be
callable when it runs as a sub-agent"). See the spec's "Sub-agent semantics (inherited from
callWorkflow)" section for the full rationale (still applicable — `callAiAgent` shares the same
durable-subflow-suspend runtime as `callWorkflow`, just with an agent-uuid-keyed resolution step in
front of it).

**Dedicated canvas element.** `callAiAgent` (`WorkflowCallAiAgentTool`, in the `workflow` component
beside `callWorkflow`) is also a first-class TOOLS cluster element a user can add directly to any
hand-authored workflow's `aiAgent`/`streamChat` node from the canvas — not just something the Agents
feature generates. Its `agentUuid` picker (`CallableAiAgentDataSource.getCallableAgents`) lists every
agent with at least one published project version; `toolName`/`toolDescription` are required (the
LLM tool-name/description schema is built from the cluster element's static configured parameters
before the tool function ever runs — see `AiAgentToolFacade.getFunctionToolCallback` — so, like
`callWorkflow`, there is no resolved-agent identity available yet to default them from). The SPI
seam that lets the CE `workflow` component see agents without a hard compile-time dependency on
`automation-ai-agent` is `CallableAiAgentDataSource`
(`platform-workflow-task-dispatcher-api`, `com.bytechef.platform.workflow.task.dispatcher.subflow`
package, beside `SubflowDataSource`/`SubflowResolver`), implemented by
`CallableAiAgentDataSourceImpl` in `automation-ai-agent-service` and injected into
`WorkflowComponentHandler` via `ObjectProvider<CallableAiAgentDataSource>` — the `callAiAgent` element is
simply omitted from the component's cluster elements if no such bean is present.

## Built-in tool settings

`ai_agent.settings` (nullable JSON, `AiAgent.getSettings()`/`setSettings(...)`, `MapWrapper`-backed
like `parameters` on channel/element rows) holds `{streamResponse?, builtInTools: {askUserQuestion,
autoMemory, skillManagement, webSearch, webSearchConnectionId?}}` — per-agent on/off switches for the
`aiAgentUtils` built-in tools `AiAgentWorkflowGenerator` emits, read via
`com.bytechef.automation.ai.agent.util.AiAgentSettings` (shared by the generator and
`AiAgentFacadeImpl`'s publish validation so the two can never read a different default). Absence of
a key — including a wholly empty/null `settings` column — means that key's default applies:

| settings key | element(s) emitted | default | connection |
|---|---|---|---|
| `askUserQuestion` | `aiAgentUtils/v1/askUserQuestionTool` | ON | none |
| `autoMemory` | `aiAgentUtils/v1/autoMemoryTool` | ON | none |
| `skillManagement` | the 5 `AiSkill` actions as TOOL entries — `aiAgentUtils/v1/createAiSkill`, `updateAiSkill`, `deleteAiSkill`, `appendFilesToAiSkill`, `removeFileFromAiSkill` | ON | none |
| `webSearch` | `brave/v1/webSearch` | OFF | REQUIRED to publish — `webSearchConnectionId`, resolved on the `brave` component (v1) |

`autoMemory` replaces the retired `AUTO_MEMORY` element kind (branch-only feature, no compat shim).
There is no `skills` key either: attaching a `SKILL` row IS the opt-in, so the `skillsTool` aggregate
is emitted whenever any exists. A separate toggle only added a way for a stale or hostile settings map
to suppress a skill the user had explicitly attached, and left the UI with two controls for one
decision.
`askUserQuestion`/`autoMemory`/`skillManagement`'s five tools are bare `aiAgentUtils/v1/<name>`
entries with empty `parameters` — every property each one declares is optional/LLM-fillable, same
convention as the pre-existing built-ins. `webSearch` is the exception in a different way: it is NOT
an `aiAgentUtils` element at all but `brave/v1/webSearch`, the `brave` component's own tool cluster
element (`BraveComponentHandler` registers `BraveWebSearchAction` via `ComponentDsl.tool(...)`), so
it draws from the `brave` node-name counter and carries its own connection like any other component
element. The generator never emits `aiAgentUtils/v1/braveWebSearchTool`, which sits on a component
with no connection of its own and so could only reach Brave through a `connections` block naming a
different component than its own type. That element still exists and is still offered in the tool
picker for a user to add by hand; it used to read its connection under `"braveApiKey"` alone, while
`BraveConnection` stores `Authorization.API_TOKEN = "api_token"`, so the key never arrived and its
`apply` silently returned zero tools — it now accepts both keys.

**Facade.** `AiAgentFacadeImpl.updateAgentSettings(long id, Map<String,Object> settings)` REPLACES
the agent's entire `settings` map (not a per-key merge — the shape is small and entirely
client-owned, so there is no partial-edit use case) and regenerates the draft workflow, same as
every other draft-affecting mutation. Exposed on `AiAgentDTO.settings()` (a flattened read of
`AiAgent.getSettings()`) and via GraphQL: `AiAgent.settings: Map` field,
`updateAiAgentSettings(id: ID!, settings: Map!): Boolean!` mutation.

**Publish validation.** `AiAgentFacadeImpl.validateForPublish` rejects publishing when
`settings.builtInTools.webSearch` is on but `webSearchConnectionId` is absent
(`AiAgentErrorType.BUILT_IN_TOOL_CONNECTION_MISSING`) — the generated `brave/v1/webSearch` node
would otherwise have no connection to read its key from. A `webSearchConnectionId` set at draft
time (with `webSearch` on or off) still feeds `AiAgentWorkflowGenerator.buildConnectionRefs` /
`AiAgentFacadeImpl.syncTestConnections` for the draft's test-chat panel, same as any other
channel/element `connectionId` — the real connection only binds at deployment time via the
generated node's `connections` block.

## HITL approvals (tool gate + LLM-invocable tool)

Per-tool "requires approval", generated as the platform's existing
`aiAgentUtils/v1/approvalGateTool` cluster element (`AiAgentUtilsApprovalGateTool`, `components/ai/agent/utils`
— see `.agents/hitl-approvals.md` for the full HITL runtime design). No DB migration: both new
`AiAgentElement` kinds are stored the same way every other kind is, `kind` as a `STRING` and `parameters`
as `JSON`.

**Where approvals go is not configured — it is derived from the agent's own channels.** An approval
is delivered over every channel that can carry one, reusing that `ai_agent_channel` row's own
connection, so an agent already listening on Slack asks for approval on Slack without a second Slack
setup. The mapping is the channel declaration's own `.approvalChannel(<elementName>)`, resolved onto
`ResolvedAgentChannel.ApprovalDelivery(componentName, elementName)` and applied by
`AiAgentWorkflowGenerator.buildApprovalDeliveryChannels`. The component is always the channel's own,
so only the element name is declared — which matters for twilio and infobip, whose `APPROVAL_CHANNELS`
default to `sms` while their agent channels are WhatsApp. A channel that declares no approval channel
simply cannot carry one:

| channel type | approval delivered through | needs a connection |
|---|---|---|
| `chat` | `chat/v1/chat` | no (publishes onto the run's own job SSE stream) |
| `slack` | `slack/v1/slack` | yes |
| `telegram` | `telegram/v1/telegram` | yes |
| `rocketchat` | `rocketchat/v1/rocketchat` | yes |
| `whatsapp` | `whatsApp/v1/whatsApp` | yes |
| `twilio` | `twilio/v1/whatsApp` | yes |
| `infobip` | `infobip/v1/whatsApp` | yes |
| `schedule` | — nobody to ask | — |
| `workflowCall` | — the caller is another workflow | — |

*Accepted loss:* email, approval task, Discord and Mattermost are no longer reachable as approval
destinations — they are not agent channel types. `ApprovalChannelDefinitions` still lists them (the
generator asks it whether a delivery component needs a `connections` block, and its entries document
what each component exposes), but nothing maps a channel onto them. Any `APPROVAL_CHANNEL` element
row left in an existing database is ignored rather than migrated; the kind, its validation, and error
keys 110/111 are retired.

- **`TOOL` rows** — an optional boolean `AiAgentElement.PARAM_REQUIRES_APPROVAL` ("requiresApproval") key
  inside `parameters`, alongside the existing `TOOL_PARAM_*` convention documented in
  `AiAgentWorkflowGenerator`. `true` pulls that tool into the generated gate; absent/`false` leaves it in
  the flat `tools[]` array as before. The flag only takes effect while the `APPROVAL_GATE` row exists —
  it is stored on the row either way, so toggling the gate off and back on restores the previous gating
  rather than clearing each tool's flag.
- **`AiAgentElement.KIND_APPROVAL_GATE`** — singleton (rejected like `MODEL`/`CHAT_MEMORY`/
  `APPROVAL_TOOL` if a second one is added), `parameters` optionally carrying
  `approvalExpiresIn` (integer) and `approvalExpiresInUnit` (`"HOURS"`/`"DAYS"`), mirroring
  `AiAgentUtilsApprovalGateTool`'s own properties. Doubles as the agent-level **master switch** for tool
  gating: with no `APPROVAL_GATE` row, `buildToolSequence` emits every tool ungated no matter what its
  `requiresApproval` flag says. Its expiry settings are dormant until at least one gated `TOOL` row exists.
  In the UI it is the "Tool approval" switch on the Settings tab, off by default — absence of the row is
  off, so no default is declared anywhere. The expiry field belongs to this switch (it bounds how long a
  gated tool waits), not to "Agent may request approval", whose own expiry is part of the LLM-filled
  `requestApproval` schema.
- **`AiAgentElement.KIND_APPROVAL_TOOL`** — singleton, no `parameters` (every property the
  underlying `approval` component's `requestApproval` action declares — `formTitle`,
  `formDescription`, `inputs`, `expiresIn`, `expiresInUnit` — is optional and, being a `TOOLS`-type
  cluster element, becomes the LLM's own function-calling schema at call time). In the UI it is the
  "Agent may request approval" switch, beside "Tool approval" on the Settings tab and, like it, off
  by default and independent of it. Emits the platform's
  `approval/v1/requestApproval` cluster element (`ApprovalRequestApprovalTool`,
  `components/approval`) directly as a top-level tool an agent's LLM can invoke to ask a human a
  question mid-turn — distinct from the gate above, which only ever wraps OTHER tools.
  `AiAgentUtilsApprovalGateTool.checkGatableChild` rejects nesting `requestApproval` inside a gate
  (double-suspend), so this element is NEVER placed inside the gate, always in the flat `tools[]`
  array (last, after every other group — see "Generated workflow shape" above). Carries the same
  channel-derived destinations as the gate, in its own `clusterElements.approvalChannels` —
  a fresh set of channel node instances each time, since the gate and the tool are different parents
  in the `clusterElements` tree (`AiAgentWorkflowGenerator.buildApprovalChannelEntries` is shared by
  both so this logic is never duplicated).

**Generated shape.** `AiAgentWorkflowGenerator.buildTools` pulls every `requiresApproval=true` `TOOL` row
out of the flat array and nests them inside ONE `aiAgentUtils/v1/approvalGateTool` entry
(`AiAgentWorkflowGenerator.buildApprovalGateElement`), placed deterministically at the position the FIRST
gated tool would otherwise have occupied among the (still-flat) ungated tools
(`AiAgentWorkflowGenerator.buildToolSequence`):

```json
{
  "name": "aiAgentUtils_<n>",
  "type": "aiAgentUtils/v1/approvalGateTool",
  "parameters": {
    "name": "Requires approval",
    "approvalExpiresIn": 4,
    "approvalExpiresInUnit": "HOURS"
  },
  "clusterElements": {
    "tools": [ "...gated TOOL entries, same shape as an ungated one..." ],
    "approvalChannels": [
      { "name": "chat_<n>", "type": "chat/v1/chat", "parameters": {} },
      { "name": "slack_<n>", "type": "slack/v1/slack", "parameters": {},
        "connections": { "slack": { "componentName": "slack", "componentVersion": 1 } } }
    ]
  }
}
```

The gate is emitted only when the `APPROVAL_GATE` row is present AND at least one gated tool exists; a
stray `APPROVAL_GATE` row with no gated tools contributes nothing to the generated
workflow (harmless), and a `requiresApproval` tool with no `APPROVAL_GATE` row runs ungated.
`clusterElements.approvalChannels` is omitted entirely (not an empty array) when the agent has no
approval-capable channel — the gate's own runtime then defaults to the chat channel. Every node name
inside the gate — its own name, each gated tool's name, each channel's name — is drawn from the SAME
shared node-name counter as the rest of `aiAgent_1`'s `clusterElements` (the single-source
node-naming invariant), consumed in a fixed order (gate name, then gated tools, then channels) that
`AiAgentWorkflowGenerator.buildConnectionRefs` mirrors exactly, so `AiAgentFacadeImpl.syncTestConnections`
(the draft test-run connection seam) stays correct for both the nested gated tool's connection and
each delivery channel's connection. A delivery node's `ConnectionRef` is owned by the
`ai_agent_channel` row it was derived from (`ConnectionRefOwnerKind.CHANNEL`), which is what makes it
reuse that channel's connection; there is no element-side fallback any more.

**Publish validation: none for approvals.** There used to be one — publishing was rejected when a
gated `TOOL` or an `APPROVAL_TOOL` existed with no `APPROVAL_CHANNEL` row. It is gone, not rewritten:
the chat channel is created with every agent and cannot be deleted, so there is no configuration in
which an approval has nowhere to ask. What that check was really guarding against — an approval that
technically has a destination nobody is watching, on an agent reachable only by schedule or
workflowCall — remains covered as an advisory warning by
`WorkflowValidator.validateChatOnlyApprovalChannels` (see `.agents/hitl-approvals.md`), which is
the right severity for it: chat is a real destination, reachable through the pending-approvals inbox
and the hosted form.

## Client surfaces (agent detail + deployments)

`client/src/pages/automation/agents` (list, detail, publish, test chat) and
`client/src/pages/automation/agent-deployments`. Four non-obvious invariants:

- **Tools are cluster elements, not actions.** The Add Tool picker reads
  `useGetRootComponentClusterElementDefinitions({clusterElementType: 'tools', rootComponentName:
  'aiAgent', rootComponentVersion: 1})` — the same source the workflow editor's tool picker uses. A
  component declares a TOOLS element per action explicitly (`ComponentDsl.tool(actionDefinition)`),
  so listing a component's *actions* instead offers tools that do not exist: `accelo` declares three
  actions and no tools, and picking `createCompany` failed at save with "Cluster element definition
  createCompany not found in component accelo". Cluster ROOTS are filtered out of the list (an
  `aiAgent` inside an `aiAgent` is not a tool), which is why the build-time component index records
  `clusterElementTypes` — see CLAUDE.md's note on stubs and derived flags. The picker is two
  `ComboBox`es, component then tool.
- **One dialog configures channels, model and tools.** `shared/components/component-config/
  ComponentConfigDialog.tsx` (renamed from the tool-only `ToolConfigDialog`) takes
  `kind: 'CLUSTER_ELEMENT' | 'TRIGGER'` and an optional `picker` slot, so the ADD flows show
  connection + properties in one step rather than add-then-edit. Properties render through the same
  `Property` components the workflow editor uses, in form mode.
- **Display conditions need a server round-trip outside the editor.** A property form with no
  workflow node cannot evaluate `displayCondition` locally, so
  `shared/queries/platform/useFormDisplayConditions.ts` (300 ms debounce) asks
  `componentPropertyDisplayConditions(componentName, componentVersion, operationName, operationType,
  parameters)` and passes the result as `formDisplayConditions`. `undefined` means "not evaluated
  yet" and leaves every conditional property visible — never hidden by default.
- **Version history is the backing project's.** The header's history button opens the SAME
  `ProjectVersionHistorySheet` the project header uses (it gained a `title` prop for this), fed by
  `aiAgentVersions(id)` → `AiAgentFacade.getAgentVersions` → the backing project's `ProjectVersion`
  rows, newest first, mapped client-side into the sheet's REST shape. Fetched only while the sheet is
  open. Publishing goes through the project header's own `PublishPopover` (parameterised with
  `title`/`tooltip` so it never says "project" on an agent), so a publish carries the description that
  makes that history worth reading.
- **Export/import move configuration, not state.** `exportAiAgent(id)` returns a JSON document
  (`exportVersion`, title, description, instructions, settings, channels, elements);
  `importAiAgent(workspaceId, json)` builds a new agent from one, through `createAgent` so it gets the
  same backing project, permanent channels and draft workflow as any other. Deliberately lossy in two
  ways, both stated in `AiAgentFacade`'s javadoc and in the import toast: **connection ids are never
  exported** (a connection belongs to a workspace and an environment — carrying one would dangle or
  point at someone else's credential), and **`SKILL` / `SUB_AGENT` / `KNOWLEDGE_BASE` elements are
  skipped on import** (their `referenceId`s mean nothing in the target workspace, and importing them
  would generate a workflow pointing at rows that do not exist). Unknown channel types are skipped
  rather than failing the import, so a document from a newer ByteChef still imports what it can. In
  the UI: Export in both ⋮ menus, Import on the New Agent split button — it makes a new agent, so it
  belongs beside the other way of making one, which is where the projects page keeps Import Project.
- **An agent deployment IS a `ProjectDeployment`.** Same row, same id, same REST endpoints — the
  Agent Deployments page is a different view of one, not a parallel entity. Its tags are therefore
  ordinary `project_deployment_tag` rows reached through agent-specific operations
  (`aiAgentDeploymentTags` / `updateAiAgentDeploymentTags` → `projectDeploymentService.update(id,
  tagIds)`), the API-Collections shape: a separate operation pair over the same table, so the
  deployments list can be filtered by tag without loading the ordinary Deployments page's tags.

## Testing

- `AiAgentWorkflowGeneratorTest` (automation-ai-agent-service) — structural/snapshot pinning of
  `AiAgentWorkflowGenerator`'s output against `definition/agent_workflow_two_channels.json` and
  `definition/agent_workflow_full_elements.json` (both now include the default-ON built-ins in
  `tools[]`, since a fixture agent carries no explicit `settings`); determinism tests. HITL is pinned
  separately against `definition/agent_workflow_hitl.json` (one ungated + one gated `TOOL`, an
  `APPROVAL_GATE` expiry override, and the agent's own chat + slack channels supplying delivery): the
  gated tool nests inside the gate and drops out of the flat `tools[]` list, `approvalChannels`
  entries/types are asserted, the expiry parameters round-trip, `buildConnectionRefs` includes both
  the nested gated tool's and the slack channel's (but not the connection-free chat channel's) refs,
  and generation is deterministic. The `APPROVAL_TOOL` element is pinned separately against
  `definition/agent_workflow_approval_tool.json` (a singleton `APPROVAL_TOOL` row, no gate): the
  tool is the last `tools[]` entry (after the default
  built-ins), carries its own fresh `approvalChannels` node instances, and
  `buildConnectionRefs`/determinism are asserted the same way as the gate. Built-in settings:
  default-agent emission (`askUserQuestion`/`autoMemory`/`skillManagement` present, no
  `brave/v1/webSearch`), `webSearch` on emits `brave/v1/webSearch` with its own `brave` connection
  (`buildConnectionRefs` resolves its design-time `webSearchConnectionId` to an `AGENT_SETTINGS`-kind
  ref), and a `SKILL` row alone emits `skillsTool` regardless of what the settings map says
  (`testSkillRowsAloneEmitSkillsToolRegardlessOfSettings`, `testNoSkillRowsOmitsSkillsTool`).
- `AiAgentWorkflowExecutionIntTest` (automation-ai-agent-service) — end-to-end: builds a real two-channel
  (chat, telegram) agent, generates its workflow, and drives just the `branch_in` task through a
  `JobSyncExecutor`-based harness (copied from
  `server/libs/modules/task-dispatchers/branch/src/test`'s own `BranchTaskDispatcherIntTest`
  pattern) with realistic per-channel job inputs, asserting the resulting envelope. The trimmed
  fixture workflow (`src/test/resources/workflows/agent_workflow_branch_in.json` — `branch_in`
  only, no `aiAgent_1`/`branch_out`, since those need a live LLM call and channel side effects) is
  pinned against the real generator's output by
  `testGeneratedBranchInMatchesExecutionFixture`, so the fixture can never silently drift from the
  generator it's meant to exercise. A second fixture,
  `workflows/agent_workflow_slack_bot_guard.json` (single slack channel), exercises the Slack
  echo-loop guard end-to-end — `condition/v1` + `terminate/v1` dispatchers are wired into the same
  harness — asserting a bot-originated event (`bot_id` or `subtype: "bot_message"`) leaves the job
  `STOPPED` with no envelope output, while a human message routes through normally.
- `AiAgentFacadeIntTest` / `AiAgentServiceIntTest` — CRUD, publish validation, sub-agent cycle
  rejection, deployments listing. HITL: duplicate `APPROVAL_GATE` rejected
  (`ELEMENT_KIND_ALREADY_PRESENT`), an approval delivered over Slack reusing the agent's own Slack
  channel connection (`testApprovalDeliveryReusesTheAgentChannelConnection`, asserting the
  `aiAgent_1`/`slack_2` test-configuration entry carries the CHANNEL row's connection id), a gated
  `TOOL` publishing a workflow containing the generated `aiAgentUtils/v1/approvalGateTool` alongside
  the derived `chat/v1/chat` delivery, and a leftover `requiresApproval` flag with no gate publishing
  ungated. `APPROVAL_TOOL`: duplicate rejected
  (`ELEMENT_KIND_ALREADY_PRESENT`), adding it regenerates the draft with `approval/v1/requestApproval`,
  and publishing it emits it into the published workflow. Settings: `updateAgentSettings` regenerates the draft and syncs
  the `webSearch` built-in's `WorkflowTestConfiguration` connection at its computed node name,
  publishing with `webSearch` on and no `webSearchConnectionId` rejected
  (`BUILT_IN_TOOL_CONNECTION_MISSING`), and with a connection id set it publishes.
- `AiAgentGraphQlControllerTest` (automation-ai-agent-graphql) — slice test asserting
  `updateAiAgentSettings` passes its `settings` argument through to the facade untouched and returns
  `true`, and that the `AiAgent.settings` GraphQL field reflects `AiAgentDTO.settings()`.

## Copilot / AI Hub support (agent builder specialist)

`AiAgentFacade` is exposed as its own domain slice, following the data-table/knowledge-base/context-store
pattern (see CLAUDE.md's "Domain copilot slice pattern"): read tools (`listAiAgents`, `getAiAgent`) feed the
`ai_agent_ask` Copilot panel agent and the AI Hub ASK agent (via the `ai_agent_agent` delegate); write tools
(`createAiAgent`, `updateAiAgent`, `addAiAgentChannel`, `deleteAiAgentChannel`, `addAiAgentElement`,
`updateAiAgentElement`, `deleteAiAgentElement`, `updateAiAgentSettings`, `publishAiAgent`) additionally feed
`ai_agent_build` and the AI Hub BUILD agent. Tool callbacks live in `automation-ai-tool`'s
`com.bytechef.automation.ai.tool.aiagent` package (`AiAgentToolCallbacksFactory` + one `ToolCallback` class
per operation); the panel/subagent configuration is `AiAgentAgentConfiguration` in CE `ai-copilot-service`
(prompts `prompt_ai_agent_ask.txt`/`prompt_ai_agent_build.txt`); the AI Hub delegate is
`AiAgentAgentToolCallback` (CE `ai-copilot-tool`, tool name `ai_agent_agent`), wired into
`AiHubConfiguration.registerCopilotSubAgentToolCallbacks` (EE `ai-hub-service`) the same way as every other
Copilot specialist. Client: `Source.AI_AGENT`, triggered from the "Ask Copilot" button in
`AgentDetailHeader.tsx` (owned by `AgentDetail.tsx`), with a `useCopilotPostTurnRegistry` registration that
calls `invalidateAgentQueries` after a BUILD-mode turn. Authorization follows the facade's own
`@PreAuthorize("isAuthenticated()")` — no extra tool-level auth is layered on.

## AI Hub visibility (ticket 732)

An agent's conversation is readable in the AI Hub chats list whenever it was reached through a
channel — Slack, WhatsApp, a schedule, etc. — not only when started from the composer's Agents
cascade. No transcript is copied: the builtin chat-memory element (see "Sub-agents" above for how the
`aiAgent_1` node is wired, and CLAUDE.md's "AI Hub Chats" section for the session-store detail) already
writes both sides of every turn into the same Spring AI session store the Hub reads, keyed by
`conversationId`. At turn completion the agent calls the CE SPI
`com.bytechef.platform.ai.conversation.AgentConversationRecorder` (`platform-ai-api`), and the EE AI
Hub's implementation find-or-creates an `ai_hub_chat` metadata row (kind `AGENT_CHAT`) pointing at that
thread — see CLAUDE.md's "Channel-born agent conversations" for the full breakdown (the
`workflow_execution_id` null invariant, the monolith-only caveat, what attribution is and isn't
verified, and the creator-only privacy decision).

Conditions under which a run is **not** visible in the Hub: the agent's Chat Memory element is off (no
`conversationId`, nothing to point a row at — this is intended, not a bug); **the agent was last
published before this feature shipped** (see below); the executing job reports
no `workflowId`/`environmentId` (happens outside a real workflow execution context, e.g. some test
paths); the claimed workspace doesn't match the workflow's actual owning project (rejected as a forgery
attempt); the run went through the **realtime (voice) chat action**, which is deliberately not wired to
the recorder (see the design spec's Scope section); or the deployment is distributed EE, where the
workspace-verification lookup is unimplemented and the recorder fails closed. In every one of these
cases the agent's turn still completes normally — recording is best-effort and never blocks or fails
the run.

**Agents published before this feature need one re-save to become visible, and there is no backfill.**
The `aiHubWorkspaceId`/`aiHubAgentId`/`aiHubCreatorUserId` identity stamp the recorder reads is written
onto the `aiAgent_1` node by `AiAgentWorkflowGenerator.buildAiAgentNode` at *generate* time, so it
exists only in workflow definitions generated on or after this change. An agent whose published
definition predates it carries a stampless node, the recorder takes its all-null early return, and the
feature silently does nothing for that agent — no warning is logged, because an absent stamp is also
what a legitimate hand-built canvas `aiAgent` node looks like. Re-saving the agent regenerates the draft
and re-publishing promotes it; both are needed, since only the published definition runs. This is the
most likely reason an operator sees nothing on day one.

## Known follow-ups (tracked, non-blocking)

- ~~**`AiAgentUtilsBraveWebSearchTool` is orphaned and broken**~~ — FIXED by fixing the key, not by
  deleting the element: it now reads `Authorization.API_TOKEN` as well as its own `"braveApiKey"`, so
  a user who picks it from the tool picker gets a working tool. The generator still emits
  `brave/v1/webSearch` for the `webSearch` setting.
- **Authorization is `isAuthenticated()`-only** across `AiAgentFacade` / the agent GraphQL surface —
  CE parity with workspace-shared projects, but real `AGENT_*` permissions are a hard prerequisite
  for EE/multi-workspace. Documented on the facade.
- **Stale draft test-config connections**: `syncTestConnections` adds/replaces
  `WorkflowTestConfiguration` connections on every draft regeneration but never removes rows for
  deleted channels/elements; a deleted element followed by node-name reuse can apply a stale
  connection in DRAFT TEST runs only. Fix: prune rows absent from `buildConnectionRefs` output.
- `AiAgentWorkflowGenerator.buildConnectionRefs` mirrors `buildTools`' MODEL/TOOL-first processing
  order by documented invariant, not shared code — add a pinning test over a full-elements fixture.
- ~~**Cluster-element connection keys disagree with the platform's**~~ — FIXED. A cluster element's
  `connections` block is now keyed by the element's OWN node name
  (`buildClusterElementConnections`), and `buildConnectionRefs` emits
  `(workflowNodeName = aiAgent_1, workflowConnectionKey = <elementNodeName>)` for every ELEMENT and
  `AGENT_SETTINGS` ref. Triggers and reply tasks keep the component-name key — they are ordinary
  workflow nodes.

  That single convention fixes both halves at once. The deployment dialog no longer lists the same
  connection twice: `ClusterRootComponentConnectionFactory.getComponentConnections` still derives one
  `ComponentConnection` from the element's node name and one from the `connections` block, but they
  are now identical records, so its `HashSet` collapses them (`ComponentConnection` is a record, so
  equality covers all five fields). And `WorkflowTestConfiguration` rows now match what both readers
  query — every runtime consumer resolves with
  `componentConnections.get(clusterElement.getWorkflowNodeName())`, and the simple editor's
  `useAiAgentTools` looks the test configuration up by the root node name and matches each tool
  against `workflowConnectionKey`. Before this, agent test runs filed the model connection under a
  node nobody queried.

  The `connections` block is emitted, not dropped, because it is load-bearing for built-ins whose
  element component differs from the connection's: the `webSearch` tool is an `aiAgentUtils` element
  needing a `brave` connection, and `aiAgentUtils` has no connection of its own, so the factory
  derives nothing for it and the block is that connection's only source.

  (`ConnectionRef` briefly carried a `componentName` field to let approval channels inherit an agent
  channel's connection by component name. Approval delivery is derived from the channel row itself
  now, so the ref is owned by that row and the field is gone.)
- `ai_agent` table: add a `(workspace_id, name)` unique index + FK indexes on
  `ai_agent_channel.agent_id` / `ai_agent_element.agent_id` in a follow-up migration.
- Server-side "clear connection" update semantics (today: delete-then-add from the client, which
  can strand state on partial failure).
- `getAgentDeployments` is O(agents x envs x workflows x triggers); batch when agent counts grow.
- WhatsApp channel re-enable requires fixing the whatsapp component's declared trigger output
  (single object vs the real Cloud API's arrays).
- Pre-existing (not this feature): `ProjectDeploymentWorkflowGraphQlController` reads
  `@Value("${bytechef.webhook.url}")` but the real property is `bytechef.webhook-url`.


## Appendix: extracted from CLAUDE.md

### Agents (automation)

A chat-first alternative to hand-built workflows: an `AiAgent` (+ `AiAgentChannel` rows for inbound
triggers, `AiAgentElement` rows for model/tools/skills/sub-agents/RAG/chat-memory) owns a generated
workflow definition (`AiAgentWorkflowGenerator`: triggers → `branch_in` keyed on `${__triggerName}` →
`aiAgent/v1/streamChat` → `branch_out` also keyed on `${__triggerName}`, one case per channel ROW so
two rows of one type can reply with their own row-configured values) living in a hidden
`__AI_AGENT__<uuid>`-prefixed system project (`SystemProjects`, excluded from ordinary
project/deployment listings by name prefix). **Channels are declared by components**: a trigger
carries `.agentRequest(...)` and an action `.agentReply(...)` stating where the contract fields
(`conversationId`, `message`, `attachments`) live on each end, and
`ComponentDsl.agentChannel(name, trigger[, replyAction])` pairs them (spec
`2026-08-17-sdk-agent-channels-design.md`). Existing operations are reused — only `slack/newMessage`
was added, because declining an event means returning an empty collection. The generator reads the
descriptors through `ComponentDefinitionService.getAgentChannelDefinitions()` and names no component.
`AiAgentChannelType` keeps only `chat`/`workflowCall`/`schedule`: the first two because the facade
pins them (auto-created, undeletable), `schedule` because it is **not a channel** — it has no reply
and no conversation partner, stays on `schedule/v1/cron`, and is the generator's one deliberate
branch. Every save regenerates the draft workflow; publishing is a separate explicit action that
duplicates each workflow into a new version (never in-place). `__triggerName`
(`JobInputConstants.TRIGGER_NAME_INPUT`) is a platform-wide reserved job input, and any
`__`-prefixed input/node name in a hand-authored workflow is rejected by
`WorkflowValidatorFacade.validateNoReservedInputNames`/`validateNoReservedNodeNames`. Sub-agents
wire in via `workflow/v1/callAiAgent`; only cycles are hard-blocked, deeper-than-1-level nesting
is a save-time warning only (`callAiAgent` itself refuses to suspend when already running as a
subflow). HITL is two INDEPENDENT singleton element rows, both surfaced as Settings-tab switches
that are off by default because absence of the row IS off: `KIND_APPROVAL_TOOL` emits the platform's
`approval/v1/requestApproval` LLM-invocable tool directly into the tools array (never inside the
gate), and `KIND_APPROVAL_GATE` is the agent-level MASTER SWITCH for per-tool gating — with no such
row `buildToolSequence` emits every tool ungated regardless of its own `requiresApproval` flag, and
the flags stay on their TOOL rows so switching the gate back on restores the previous gating.
`AiAgent.settings.streamResponse` (default ON) picks which `aiAgent` action the generated node runs —
`streamChat` on, `chat` off — the same choice the workflow editor's AI Agent panel offers as its
"Stream response" switch; it sits at the top level of `settings`, not inside `builtInTools`, because it
is not a tool. Switching it changes no other part of the generated shape: the generator emits no
`response` parameter for either action, so `__AGENT_OUTPUT__` stays the bare `${aiAgent_1}`.
`AiAgent.settings.builtInTools` (`AiAgentSettings`) turns the generator's `aiAgentUtils` built-ins
on/off per agent — `askUserQuestion`/`autoMemory`/`skillManagement` default ON, `webSearch`
(`braveWebSearchTool`) defaults OFF and needs a `webSearchConnectionId` to publish; this replaced the
old `AUTO_MEMORY` element kind. There is deliberately NO `skills` key: attaching a `SKILL` row is
itself the opt-in, so `skillsTool` is emitted whenever any exists (and its `parameters.skills` is a
FLAT array of ids, not `[{skillId}]` objects — both readers call `getList(SKILLS, Long.class)`). Tags
live in an `ai_agent_tag` join table, the `project_tag` shape. Client pages:
`src/pages/automation/agents` (list/detail/publish/test chat) and
`src/pages/automation/agent-deployments` (reuses `ProjectDeploymentDialog`). See
`.agents/agents.md` for the full breakdown and `docs/superpowers/specs/2026-08-10-agents-design.md`
for the design rationale.
