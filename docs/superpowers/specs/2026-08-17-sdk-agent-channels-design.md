# SDK agent channels — design

**Date:** 2026-08-17 · **Ticket:** 732 · **Status:** draft for review
**Supersedes:** the "Channel registry" section of `2026-08-10-agents-design.md`.

## 1. Problem

An AI Agent (`.agents/agents.md`) is reachable through *channels*: a trigger that brings a
message in and, usually, an action that sends the agent's reply back out. Today the set of channels
is a hand-maintained registry, `ChannelDefinitions` in `automation-ai-agent-service`, which
transcribes each component's trigger/action names, output paths and reply parameters into a
`ChannelDefinition` record — 260 lines whose comments repeatedly say "NOT `sendMessage` as commonly
assumed", "despite the class name, the registered trigger is …", "follows the external API doc, not
this repo's source — flagged as uncertain". The client keeps five more hand-maintained mirrors of
the same list (`AgentChannelsCard.tsx`, `AgentApprovalSettings.tsx`, …), and adding a channel means
touching all of them.

Root cause: the knowledge lives *away from* the component that owns it. The component author knows
which trigger and which action operate as a request/response pair for an agent; the platform should
ask the component, not re-derive it.

## 2. Goals and non-goals

Goals

- A component declares, in the SDK, that a trigger and an action form an **agent channel**: the
  trigger's output has one fixed shape, the action accepts the reply. `AiAgentWorkflowGenerator`
  reads those declarations from the component registry and wires them without per-channel code.
- The declaration validates its contract at construction time, so a mismatched pair fails the
  component's own tests, not an agent run.
- The eight conversational channels (chat, workflowCall, slack, telegram, whatsApp, rocketchat,
  twilio, infobip) migrate to the new declaration; `ChannelDefinitions` and the client's channel maps
  are deleted. **`schedule` is deliberately not among them** — see §3, "What is not a channel".

Non-goals (kept exactly as they are)

- The workflow engine, webhook executor, trigger completion, `__triggerName` seeding, and the
  `ai_agent_channel` schema and stored values. **This work is SDK-level; nothing under the platform's
  execution path changes.**
- `HostedChatTriggers` / `WebhookBridgeAgent`'s hosted-chat detection (a different question: "does
  this workflow have a chat UI", not "is this an agent channel").
- `ApprovalChannelDefinitions` (approval delivery *elements*; a sibling registry with its own scope).
- The agent's own generated-workflow topology (triggers → `branch_in` → `aiAgent_1` → `branch_out`).
  Only how each case is filled changes.

## 3. The contract

An agent channel is a **request/reply pair on one component**:

- **Request trigger** — any `TriggerType`, whose output is exactly the shape
  `ChatNewRequestTrigger` already emits:

  | field            | type                 | required | meaning                                                  |
  |------------------|----------------------|----------|----------------------------------------------------------|
  | `conversationId` | string               | yes      | chat-memory key **and** the address a reply is sent to   |
  | `message`        | string               | no       | the incoming text                                        |
  | `attachments`    | array of `fileEntry` | no       | incoming files (`[]` when the channel has none)          |

  `conversationId` doubles as the reply address deliberately: every existing channel already uses
  the same value for both (`slack.channel`, `telegram.chat.id`, `twilio.From`, …), so a separate
  `replyTo` carried nothing.
- **Reply action** (optional — a schedule has nobody to answer): declares a `message` string
  property; may declare `conversationId` (channels that must address the reply) and `attachments`.
  Any **other** property it declares must share its name with a property of the paired trigger, so
  the channel row's single `parameters` map can feed both — e.g. twilio's `number`, the WhatsApp
  number that is both the inbound endpoint and the sender of the reply. Reply actions either call
  the external service (slack, telegram, …) or return the value as a synchronous webhook/subflow
  response (chat, workflowCall).
- **Declining to fire.** A request trigger that decides an event is not for the agent (Slack
  redelivering the bot's own reply) returns an **empty collection**. `TriggerCompletionHandler`
  already creates one job per element of a non-batch collection output — zero elements, zero jobs —
  so no LLM call, no `terminate/v1`, no generator special case. This is existing platform behaviour
  (`TriggerCompletionHandler.java:127`), not a change.

**What is not a channel.** A channel has someone on the other end. A **schedule** does not: it has no
reply action, no conversation partner, and nothing arrives from outside — the agent talks to itself on
a timer. It stays wired to the existing `schedule/v1/cron` trigger, whose output
(`{fireTime, dateTime, expression, timezone}`) is a published contract other workflows already read via
`${cron_1.dateTime}` and must not change. So the agent module keeps **one** explicit non-channel branch
for it: the trigger node type, the prompt taken from the channel row's `prompt` parameter, and the
stable per-row `conversationId` all stay exactly as they are today
(`AiAgentWorkflowGenerator.java:691`). This is the single deliberate exception to "the generator has no
per-channel code", and `AiAgentChannelType.SCHEDULE` is where it lives — one of the three constants §6
keeps for product semantics, so §8's guard test still holds without amendment.

The alternative — a conforming `aiAgentPrompt` trigger on the schedule component — was considered and
rejected: it would duplicate `ScheduleCronTrigger`'s scheduling plumbing and re-declare its
cadence properties purely to satisfy a contract whose two required fields (`conversationId`, `message`)
a schedule *configures* rather than receives. The cost of the exception is that `prompt` and `name`
remain row keys the generator must exclude when projecting parameters onto the trigger node
(`AiAgentWorkflowGenerator.java:546-554`) — though §6 notes that list becomes redundant once trigger parameters are restricted to the resolved trigger's declared property names.

## 4. SDK surface (`sdks/backend/java/component-api`)

Three types. Two **descriptors** that mark an operation as agent-capable and pin how its own fields bind
to the contract, and one **pair** that joins them into a channel.

```java
/** Marks a trigger as an agent request and states where the contract fields live in ITS output. Paths
    default to the contract names, so an already-conforming trigger declares agentRequest() and nothing
    else. A path may be dotted (telegram's "message.chat.id"). */
public interface AgentRequestDefinition {
    String getConversationIdPath();            // default CONVERSATION_ID
    String getMessagePath();                   // default MESSAGE
    Optional<String> getAttachmentsPath();     // absent -> the generator wires []
}

/** Marks an action as an agent reply and states which of ITS properties receive what. */
public interface AgentReplyDefinition {
    String getMessageProperty();                    // default MESSAGE
    Optional<String> getConversationIdProperty();   // absent -> the reply is not addressed (chat, workflowCall)
    Optional<String> getAttachmentsProperty();
    /** Channel-row parameter name -> action property name, for properties the channel configures on both
        ends (twilio's "number" -> "From"). The row key must also name a trigger property. */
    Map<String, String> getChannelParameters();
    /** Values always sent with the reply (twilio useTemplate=false, workflowCall's fixed outputSchema). */
    Map<String, Object> getFixedParameters();
}

public interface AgentChannelDefinition {

    String CONVERSATION_ID = "conversationId";
    String MESSAGE = "message";
    String ATTACHMENTS = "attachments";

    /** Channel key: stored verbatim in ai_agent_channel.channel_type and used as the generated trigger
        node-name prefix ("slack_1"). Globally unique across components by convention; the registry fails
        fast on a duplicate. */
    String getName();
    Optional<String> getTitle();
    Optional<String> getDescription();
    TriggerDefinition getTrigger();
    Optional<ActionDefinition> getReplyAction();
    /** Name of an APPROVAL_CHANNELS cluster element on the SAME component through which HITL approvals
        reach the human on this channel; empty when the channel cannot carry one. */
    Optional<String> getApprovalChannelName();
    /** Trigger parameters the generator must always emit for this channel (workflowCall's fixed
        inputSchema). Distinct from a property's own defaultValue, which the generator also honours. */
    Map<String, Object> getTriggerParameters();
}

public interface AgentChannelComponentDefinition {
    default List<AgentChannelDefinition> getAgentChannels() { return List.of(); }
}

// ComponentDefinition extends ClusterElementComponentDefinition, UnifiedApiComponentDefinition,
//                             WorkflowComponentDefinition, AgentChannelComponentDefinition   (new)

// ComponentDsl
public static ModifiableAgentRequestDefinition agentRequest();
public static ModifiableAgentReplyDefinition agentReply();
public static ModifiableAgentChannelDefinition agentChannel(String name, TriggerDefinition trigger);
public static ModifiableAgentChannelDefinition agentChannel(
    String name, TriggerDefinition trigger, ActionDefinition replyAction);
public static ModifiableObjectProperty agentChannelRequest();   // the contract object schema, for .output(outputSchema(...))

// ModifiableAgentRequestDefinition:  .conversationId(String path) .message(String path) .attachments(String path)
// ModifiableAgentReplyDefinition:    .conversationId(String property) .message(String property)
//                                    .attachments(String property) .channelParameter(String rowKey, String property)
//                                    .fixedParameter(String property, Object value)
// ModifiableAgentChannelDefinition:  .title(String) .description(String) .approvalChannel(String)
//                                    .triggerParameters(Map<String, Object>)
// ModifiableTriggerDefinition:       .agentRequest(AgentRequestDefinition)
// ModifiableActionDefinition:        .agentReply(AgentReplyDefinition)
// ModifiableComponentDefinition:     .agentChannels(AgentChannelDefinition...) / (List<...>)
```

Usage. The chat component is already conforming, so both descriptors are empty:

```java
// ChatNewRequestTrigger:      .agentRequest(agentRequest())
// ChatResponseToRequestAction: .agentReply(agentReply().attachments(ATTACHMENTS))

component(CHAT)
    .triggers(ChatNewRequestTrigger.TRIGGER_DEFINITION)
    .actions(ChatResponseToRequestAction.ACTION_DEFINITION)
    .clusterElements(ChatApprovalChannel.of(messageBroker))
    .agentChannels(
        agentChannel("chat", ChatNewRequestTrigger.TRIGGER_DEFINITION, ChatResponseToRequestAction.ACTION_DEFINITION)
            .title("Chat")
            .approvalChannel("chat"));
```

Telegram reuses both existing operations unchanged apart from the descriptors:

```java
// TelegramNewMessageTrigger:  .agentRequest(agentRequest().conversationId("message.chat.id").message("message.text"))
// TelegramSendMessageAction:  .agentReply(agentReply().conversationId(CHAT_ID).message(TEXT))
```

Twilio shows the two remaining features — a row-configured property on both ends, and a pinned value:

```java
// TwilioSendWhatsAppMessageAction:
//     .agentReply(agentReply()
//         .conversationId(TO)
//         .message(BODY)
//         .channelParameter("number", FROM)   // the channel row's "number" -> the action's From
//         .fixedParameter(USE_TEMPLATE, false))     // free text, never a template
```

**Construction-time validation.** From `agentReply()` / `.agentReply(...)` (`IllegalArgumentException`):
every mapped property name — message, conversationId, attachments, each `parameterProperties` value and
each `parameters` key — is a real property of the action. This is the strong half: reply mistakes fail
the component's own test. From `agentChannel(...)`:

1. `name` non-blank, `[a-zA-Z0-9]+` (it becomes a node-name prefix).
2. The trigger carries an `AgentRequestDefinition`; the reply action, when present, carries an
   `AgentReplyDefinition`.
3. Each `parameterProperties` **key** is a property name of the paired trigger, so one channel-row
   parameter map configures both ends.
4. Trigger paths are checked only when the trigger declares a static object output schema: each path's
   first segment must be one of its properties. A schema-less output (`slack/anyEvent`,
   `rocketchat/newMessage`) or a function-valued one (`workflow/newWorkflowCall`) is accepted unchecked —
   see the honest-cost note below.

**Registration-time validation** (in `ComponentDefinitionRegistry`, fail-fast at load): the pair's
trigger and reply action are among the component's own `getTriggers()`/`getActions()` (the DSL cannot
check ordering — a handler may call `.agentChannels(...)` before `.triggers(...)`), the
`approvalChannel` names one of the component's `APPROVAL_CHANNELS` cluster elements, and channel
names are unique across all components.

**Why the descriptor is a builder method, not a Java marker interface.**
`ModifiableTriggerDefinition` and `ModifiableActionDefinition` are `final`, so an interface could only be
applied to *all* triggers or none — it cannot mark a subset. `.agentRequest(...)` / `.agentReply(...)`
achieve what a marker was wanted for: the operation itself declares that it supports an agent and pins
the expected shape, so the generator binds uniformly and never names a component. Pairing stays a
separate call because it genuinely cannot be inferred — twilio and infobip each declare two inbound
triggers (`newWhatsappMessage`, `newSMS`/`inboundCall`), so "the marked trigger" is ambiguous.

**The honest cost of reusing existing operations.** A conforming trigger would let the DSL verify the
output schema declares the contract fields. Slack's `anyEvent` and rocketchat's `newMessage` declare
`.output()` with no schema at all, so for those the trigger-side path mapping cannot be checked at build
time and a wrong path surfaces at runtime as an unresolved expression. Reply-side validation is
unaffected. This is not a regression — today nothing is validated — but it is a goal partially given up
in exchange for not adding nine parallel triggers. The cure, when a component wants it, is to give the
existing trigger a real output schema, which is independently worth doing.

**Why `name` is explicit rather than derived from the trigger.** Deriving the key from the trigger's
node type (`chat/v1/newChatRequest`) is unambiguous but changes every stored `channel_type` value and
every generated node name — an infra migration this work rules out. Explicit names let each migrated
component declare exactly the key it stores today. Uniqueness is enforced at load, and a component
that later adds a second channel picks a distinct name (`twilioSms`).

## 5. Platform surface (read-only registry, no engine changes)

- `platform-component-api`: `domain.AgentChannelDefinition` DTO — `name, title, description,
  componentName, componentVersion, triggerName, replyActionName (nullable), approvalChannelName
  (nullable), triggerParameters`, plus the **flattened binding** the generator consumes:
  `conversationIdPath, messagePath, attachmentsPath (nullable)` from the trigger's
  `AgentRequestDefinition`, and `replyMessageProperty, replyConversationIdProperty (nullable),
  replyAttachmentsProperty (nullable), replyChannelParameters, replyFixedParameters` from the action's
  `AgentReplyDefinition`. Flattened rather than two nested DTOs because the generator wants nine field
  reads, not an object graph, and because a null `replyActionName` must make the whole reply half absent
  at once. `domain.ComponentDefinition` gains `List<AgentChannelDefinition> agentChannels` (mapped in its
  SDK→DTO constructor, next to `clusterElementTypes`).
- `ComponentDefinitionRegistry`: `getAgentChannelDefinitions()` (across every loaded component, both
  ServiceLoader and Spring-declared — chat is Spring-declared and therefore absent from the build
  index) and `fetchAgentChannelDefinition(String name)`.
- `ComponentDefinitionService` (+ impl, + `RemoteComponentDefinitionServiceClient` stub throwing
  `UnsupportedOperationException` per the EE remote-client convention): the same two methods,
  filtered through the existing component-visibility providers.
- Build-time index (`ComponentIndex.Entry`): a new `agentChannels` summary list carrying every DTO field
  above (the binding included — a stub that omitted the paths would generate a workflow wired to the
  wrong expressions, which is worse than not appearing at all), rebuilt on the stub via
  `ComponentDsl.agentChannel(...)` against the stub's own trigger/action objects. Because the
  declaration is SDK-level *data*, no `StubClusterRootComponentDefinition`-style wrapper is needed —
  the reason to keep it out of a platform-side interface. Rule from CLAUDE.md applies: a derived
  list-view field is empty on a stub unless the index carries it.
- REST/OpenAPI: `ComponentDefinition.agentChannels` (+ regenerated TS types). No new endpoint; the
  agent client reads through the agent GraphQL query below.

## 6. Agent module

- **Delete** `ChannelDefinitions`, `ChannelDefinition`, `ChannelDefinitionsTest`.
- **`AiAgentChannelType`** keeps only `CHAT = "chat"`, `WORKFLOW_CALL = "workflowCall"`,
  `SCHEDULE = "schedule"`. `CHAT` and `WORKFLOW_CALL` survive because the *facade* gives them product
  semantics (auto-created and pinned) even though both are ordinary discovered channels; `SCHEDULE`
  survives for that reason **and** because it is the generator's one non-channel branch (§3). Everything
  else is discovered.
- **`AiAgentWorkflowGenerator`** takes a resolver (`Function<String, ResolvedAgentChannel>` — the
  facade resolves once from `ComponentDefinitionService`, keeping the generator static and
  deterministic). `ResolvedAgentChannel` = the DTO + the trigger's and reply action's property
  definitions (names, `defaultValue`s, required flags) + `connectionRequired` from the component.
  Generic rules, replacing all per-channel branches:
  - trigger node: `type = <component>/v<version>/<triggerName>`; parameters = declared property
    `defaultValue`s ← `getTriggerParameters()` ← channel-row `parameters` restricted to declared
    trigger property names (so UI-only row keys like the schedule dialog's `name`/cadence fields no
    longer leak into the definition); `connections` when the component requires one;
  - `branch_in` case (unchanged key `${__triggerName}`): envelope
    `{text: ${<node>.<messagePath>}, conversationId: ${<node>.<conversationIdPath>}, attachments: ${<node>.<attachmentsPath>} or [], channel: <name>}`
    — the paths come from the trigger's `AgentRequestDefinition`, so a legacy shape
    (`message.chat.id`) and a conforming one (`conversationId`) produce identical generator code;
    `replyTo`/`replyFrom` are gone;
  - `branch_out`: **keyed on `${__triggerName}`, one case per channel row** (was: per channel type on
    `${branch_in.channel}`). Needed so a reply action's row-configured properties (twilio `number`) can
    take that row's own value; two rows of one type no longer share a case. Reply task parameters, all
    from the action's `AgentReplyDefinition`: `<replyMessageProperty> ← ${aiAgent_1}`;
    `<replyConversationIdProperty> ← ${branch_in.conversationId}` when declared; for each
    `replyChannelParameters` entry `rowKey → property`, `<property> ← ` the row's `rowKey` value; then
    `replyFixedParameters` verbatim (twilio `useTemplate=false`). `attachments` is left unwired (the agent's
    output is a bare string today) and reserved. Empty case when there is no reply action;
  - approval delivery: `(componentName, approvalChannelName)` per channel row, replacing
    `ApprovalDelivery`; the `ConnectionRef` ownership rule (`ConnectionRefOwnerKind.CHANNEL`) is
    unchanged;
  - the Slack bot-echo guard and the workflowCall input/output-schema constants move into their
    components (§7); the schedule `text`/`conversationId` overrides **stay** (§3) — `scheduleConversationId`,
    `SCHEDULE_PROMPT_PARAMETER`, the `if (AiAgentChannelType.SCHEDULE…)` envelope branch, the
    `schedule/v1/cron` trigger node type and the empty `branch_out` case are all retained. But
    `SCHEDULE_NAME_PARAMETER` and the bespoke prompt/name exclusion loop are **deleted**: trigger
    parameters are now restricted to the resolved trigger's declared property names, and `cron` declares
    only `expression`/`timezone`, so the UI-only row keys are excluded generically.
- **Facade**: `validateChannelType` → registry lookup **or** `AiAgentChannelType.SCHEDULE`; import
  continues to skip unknown names; `validateForPublish` reads `connectionRequired` from the resolved
  channel (`false` for schedule, which needs no connection). `aiAgentChannelDefinitions` **appends** a
  synthesized `schedule` entry — the registry cannot supply one — so the client's schedule card keeps
  its title/description/icon from a single source. (An earlier draft of this section said "prepends";
  as built, `AgentChannelResolver.resolveAll` appends, and the ordering is client-visible so the two
  had to be reconciled. Appending is the right way round: the list's order is the registry's order,
  and the one entry that is *not* a channel does not belong at the head of a list of channels. The
  client keys off `schedule`/`pinned` rather than position anyway — its schedule card is a separate
  component from the channel cards.) New GraphQL query
  `aiAgentChannelDefinitions: [AiAgentChannelDefinition!]!` —
  `{channelType, componentName, componentVersion, triggerName, replyActionName, title, description,
  icon, connectionRequired, approvalCapable, pinned, schedule}` (`pinned`/`schedule` derived from the
  three constants). `isAuthenticated()`-gated like the other reads.
- **Client**: `AgentChannelsCard`, `AgentApprovalSettings` and `AgentDeploymentChannelList` read that
  query instead of the eight hardcoded maps (five in the channels card alone, not the three an earlier
  draft of this line counted); the configure dialog keeps its `kind: 'TRIGGER'` target (all per-channel
  configuration is trigger properties by construction, §3). `AgentScheduleCard` deliberately does NOT
  read the query — schedule is not a channel (§3), its card is a separate section, and its one
  `CHANNEL_TYPE = 'schedule'` constant has nothing to derive.
  whatsApp returns to the add menu once its request descriptor reaches the real payload (§7).

## 7. Migrating the eight existing channels

Every channel keeps its stored key. **Existing operations are reused**: each declares an
`agentRequest`/`agentReply` descriptor and is otherwise untouched, so no channel adds an action and only
one adds a trigger. The table below is derived from the components' own definition snapshots (audited
2026-08-17), not from external API docs — the failure mode `ChannelDefinitions` demonstrated.

| key            | component   | request trigger                | request descriptor                                              | reply action                 | reply descriptor                                                                 | approval elem |
|----------------|-------------|--------------------------------|------------------------------------------------------------------|------------------------------|-----------------------------------------------------------------------------------|---------------|
| `chat`         | `chat`      | `newChatRequest`               | `agentRequest().attachments(ATTACHMENTS)` — identity, output is already the contract| `responseToRequest`          | `agentReply().attachments(ATTACHMENTS)`                                            | `chat`        |
| `workflowCall` | `workflow`  | `newWorkflowCall`              | `agentRequest().attachments(ATTACHMENTS)` + channel `triggerParameters{inputSchema: <contract JSON schema>}` — the output is function-valued off `inputSchema`, so pinning the schema is what makes it conform | `responseToWorkflowCall`     | `agentReply().message("response.message")` + `parameter(OUTPUT_SCHEMA, <fixed>)`; `response` is a `dynamicProperties` map, so the generator writes a nested parameter | —             |
| `slack`        | `slack`     | **new** `newMessage`    | `agentRequest().attachments(ATTACHMENTS)` — identity; the one new operation (see below)| `sendChannelMessage`         | `agentReply().conversationId(CHANNEL).message(TEXT)`                               | `slack`       |
| `telegram`     | `telegram`  | `newMessage`                   | `agentRequest().conversationId("message.chat.id").message("message.text")` | `sendMessage`       | `agentReply().conversationId(CHAT_ID).message(TEXT)`                               | `telegram`    |
| `whatsapp`     | `whatsApp`  | `messageReceived`              | `agentRequest().conversationId("entry.changes.value.messages.from").message("entry.changes.value.messages.text.body")` — paths reproduced verbatim (see note) | `sendMessage`                | `agentReply().conversationId(RECEIVE_USER).message(BODY)`                          | `whatsApp`    |
| `rocketchat`   | `rocketchat`| `newMessage`                   | `agentRequest().conversationId("channel_id").message("text")` — schema-less output, unvalidatable (§4) | `sendChannelMessage` | `agentReply().conversationId(ROOM_ID).message(TEXT)`                    | `rocketchat`  |
| `twilio`       | `twilio`    | `newWhatsappMessage`           | `agentRequest().conversationId("From").message("Body")` — schema-less output; the trigger gains an additive optional `number` property so the row can carry the reply sender | `sendWhatsAppMessage` | `agentReply().conversationId(TO).message(BODY).channelParameter("number", FROM).fixedParameter(USE_TEMPLATE, false)` | `whatsApp` |
| `infobip`      | `infobip`   | `newWhatsappMessage`           | `agentRequest().conversationId("results.from").message("results.message.text")` — schema-less output, paths reproduced verbatim (see note) | `sendWhatsappTextMessage` | `agentReply().conversationId(TO).message(TEXT).channelParameter("number", FROM)` | `whatsApp` |

`schedule` is absent by design — it is not a channel (§3) and its component declares nothing.

**The one new operation.** Slack's existing `anyEvent` fires for *every* workspace event including the
bot's own reply, and declining requires returning an empty collection — behaviour, not naming, so a
descriptor cannot express it. `slack/newMessage` reuses `anyEvent`'s webhook plumbing and emits the
contract directly (so its descriptor is identity). It returns `List.of()` when the event carries
`bot_id` or `subtype == bot_message`, and **also** when the event's `type` is not `message` or its text
is blank — the latter two are a deliberate improvement on today's behaviour, not a reproduction of it:
`anyEvent` plus `buildSlackBotEchoGuard` lets a `reaction_added` event through as a request with a null
message, producing a billed model turn on an empty prompt and a reply posted into the channel. This is
strictly better than today's arrangement in a second way too: that guard creates a job for every event
and then terminates it, whereas now no job is created at all. The guard and its constants are deleted
from the generator.

The operation is named `newMessage`, matching telegram's and rocketchat's operation of the same
purpose. Nothing about it is agent-specific — it is registered in `.triggers(...)` like any other
operation and is usable in ordinary hand-built workflows; the `agentRequest()` descriptor is metadata
read only by the workflow generator. Its output does carry the contract's field names rather than
Slack's own (`conversationId`, not `channel`), which is the price of the identity binding; the shared
`agentChannelRequest()` helper's property descriptions carry the mapping.

**WhatsApp and Infobip reproduce today's paths verbatim — resolved 2026-08-18, reversing the "open
question" this section previously posed.** whatsApp's declared output nests `entry`/`changes`/
`messages` as single objects while Meta documents arrays, and infobip documents `results` as an array
while declaring no output schema at all. The evaluator is SpEL, which indexes (`items[0]`) but does not
auto-flatten, so `results.message` over a list resolves to nothing. Neither payload shape is verifiable
in this repo: infobip's trigger returns `body.getContent()` raw and has no payload fixture, and
`ChannelDefinitions` flags both mappings as uncertain in its own comments. So today's expressions either
work or have never worked, and this plan cannot tell which.

This plan is a refactor, so both channels reproduce the existing paths unchanged and neither gets a new
operation nor a corrected schema. Retyping whatsApp's `entry` as an array would additionally make
registration validation reject the very paths the refactor must reproduce, since validation matches a
descriptor's first dot-segment against the declared properties. Both commits state the limitation as
inherited. Follow-up, out of scope here: confirm both payload shapes against a live webhook and correct
paths and schemas together.

Every touched component's definition-JSON snapshot regenerates (the descriptors surface in it); the two
IntTests (`AiAgentWorkflowExecutionIntTest`, snapshot `agent_workflow_two_channels.json`) are updated
for the new envelope/`branch_out` shape.

## 8. Testing

- SDK: `ComponentDslAgentChannelTest` — accepts a conforming pair; rejects bad name, missing
  `conversationId`, non-string `message`, reply action without `message`, reply property with no
  matching trigger property; accepts a function-valued trigger output. Plus, for the descriptors:
  `agentReply()` rejects a property name the action does not declare; `agentChannel(...)` rejects a
  trigger without an `AgentRequestDefinition`, a reply action without an `AgentReplyDefinition`, and a
  `parameterProperties` key that is not a trigger property; it accepts a schema-less and a
  function-valued trigger output without checking paths (the documented gap, §4).
- Registry: `ComponentDefinitionRegistryAgentChannelTest` — duplicate names fail load; unreferenced
  trigger/action fail load; `getAgentChannelDefinitions` includes a Spring-declared component;
  `ComponentIndexTest` round-trips `agentChannels` through the stub.
- Generator: existing test class updated — deterministic snapshot, per-row `branch_out`, no
  per-channel branches remain (a grep-style guard test asserts the generator source has no
  `AiAgentChannelType.SLACK`-shaped constants beyond the three pinned ones).
- Facade: unknown channel key rejected; `aiAgentChannelDefinitions` returns pinned/schedule flags.
- Components: a test per migrated component that the declared pair passes validation (implicitly, the
  handler constructing it) and that the descriptor's paths/properties match the operation's real shape;
  a per-operation test for Slack's new trigger, covering the two decline cases returning `List.of()`.
- Client: `AgentChannelsCard.test.tsx` etc. switch to a mocked query.

## 9. Decisions to review (⚑ = assumption made autonomously)

- **Resolved 2026-08-17:** schedule is not a channel and keeps `schedule/v1/cron` — see §3. The
  previously proposed `aiAgentPrompt` trigger is dropped, and with it the fourth ⚑ below (schedule
  `conversationId` continuity is not a new decision; today's behaviour is retained unchanged).
- **Resolved 2026-08-17:** existing triggers and actions are reused via `agentRequest`/`agentReply`
  descriptors rather than each component gaining a dedicated conforming pair. Twelve planned operations
  drop to one (Slack's trigger, possibly two with WhatsApp). This retires the "operation names" ⚑
  entirely — there are no new names to bikeshed — and the "twilio configured `number`" ⚑ becomes a
  declaration (`parameterProperty("number", FROM)`) rather than a new action's property, though the
  behaviour change it describes still stands: the reply's sender is the configured number, not the
  inbound `To`.
- ⚑ `branch_out` keyed per channel row (`${__triggerName}`), not per type. Generator-internal, but it
  changes the generated definition and the snapshot.
- ⚑ Slack threading not attempted (would encode `channel:thread_ts` in `conversationId`; a follow-up
  the contract permits without change).
- **Trigger-side path validation is absent, not "best-effort", where the trigger declares no output
  schema — corrected 2026-08-18 from the softer wording this entry previously carried.** `.output()`
  with no arguments yields an `OutputDefinition` with neither an output response nor an output function,
  so `validateAgentRequestPaths` returns early without checking even the FIRST segment. That covers
  rocketchat, twilio and infobip; only slack among the schema-less-adjacent components declares one
  (it uses `agentChannelRequest()`). The alternative — requiring a schema before a trigger may be
  marked — would block four components until they are amended, so the gap stands, with each affected
  component's own test pinning the payload shape it assumes and asserting its paths are undotted (a
  dotted path into a flat payload would be accepted at build time and read nothing at run time).
  Where a trigger DOES declare a schema, validation still checks only the first dot-segment; telegram's
  test walks the whole path itself. Tightening that for request paths (while keeping first-segment-only
  for reply properties, which descend into `dynamicProperties` maps) is a follow-up — it splits the rule
  by direction, so it wants its own change.
- **Follow-up, now specified separately:** agent runs are invisible in AI Hub chats — a Slack message or
  a schedule fire produces a workflow execution but no `ai_hub_chat` row (verified: the agent module has
  no `AiHubChat` reference). This matters more than it appears: `AiHubTask` is to be removed and its
  scheduling delegated to AI Agents, so without this the Hub loses a surface it has today. Being
  designed as its own spec; out of scope here because it writes Hub rows from a non-Hub path.
- Left for follow-up: fold `HostedChatTriggers`/`WebhookBridgeAgent`'s hardcoded `chat/` knowledge
  onto the registry; wire `attachments` on reply actions once `aiAgent/v1/streamChat` exposes them.
