# SDK Agent Channels Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a component declare, in the SDK, that a trigger + action pair is an AI Agent channel — marking each operation with a descriptor that says where the contract fields live on it, so **existing** triggers and actions can be reused; have the agent workflow generator read those declarations from the component registry and name no component; delete the hardcoded `ChannelDefinitions` registry and the client's channel maps; migrate the eight existing channels (`schedule` is not one — spec §3).

**Architecture:** Three new SDK types. `AgentRequestDefinition` marks a trigger as an agent request and says where `conversationId`/`message`/`attachments` live in *its* output (paths default to the contract names, so an already-conforming trigger declares nothing); `AgentReplyDefinition` marks an action as the reply and maps the contract — plus any row-configured or fixed values — onto *its* properties; `AgentChannelDefinition` (built by `ComponentDsl.agentChannel(name, trigger[, replyAction])`) pairs them, validates, and is exposed through `ComponentDefinition.getAgentChannels()`. The platform mirrors the pair plus its flattened binding as a DTO, indexes it, and offers read-only `ComponentDefinitionService.getAgentChannelDefinitions()`. `AiAgentWorkflowGenerator` becomes a generic consumer of resolved channels that reads only descriptors. Existing triggers and actions are reused; the sole new operation is `slack/newMessage`, because declining an event is behaviour a descriptor cannot express. **No engine, webhook, coordinator or schema changes.**

**Tech Stack:** Java 25 / Spring Boot 4, component SDK (`sdks/backend/java/component-api`), JUnit 5 + Mockito, Vitest 4 + React 19 for the client, GraphQL codegen.

**Spec:** `docs/superpowers/specs/2026-08-17-sdk-agent-channels-design.md`

## Global Constraints

- SDK-level only: nothing under `server/libs/atlas/`, `platform-webhook`, `platform-workflow-coordinator`, or Liquibase changes. Stored `ai_agent_channel.channel_type` values stay `chat`, `workflowCall`, `schedule`, `slack`, `telegram`, `whatsapp`, `rocketchat`, `twilio`, `infobip`.
- Contract fields: `conversationId` (string, required), `message` (string), `attachments` (array of fileEntry). Constants live on `AgentChannelDefinition`.
- Reply-action non-contract properties must share a name with a trigger property (validated in the DSL).
- Channel names: `[a-zA-Z0-9]+`, globally unique (registry fail-fast).
- **Existing triggers and actions are reused** (spec §7). No component gains an action; only slack gains a trigger, `newMessage`, because declining an event is behaviour a descriptor cannot express. WhatsApp may gain one — decided inside Task 10. Everything else is a descriptor on an operation that already exists.
- Java style per CLAUDE.md: blank line before control statements, blank line after variable modification, no `_` in test method names, no `TODO:` comments, descriptive names, `./gradlew spotlessApply` before every commit.
- Commit convention: server `732 <description>`, client `732 client - <description>`; never amend on `0_732`.
- Never judge a Gradle run through a pipe: redirect to a file, check `$?`, then grep `^> Task .* FAILED`.
- Component definition snapshots: delete `src/test/resources/definition/<name>_v1.json` AND `build/resources/test/definition/` before rerunning the handler test to regenerate.

---

## File map

**Create**
- `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/AgentChannelDefinition.java`
- `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/AgentRequestDefinition.java`
- `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/AgentReplyDefinition.java`
- `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/AgentChannelComponentDefinition.java`
- `sdks/backend/java/component-api/src/test/java/com/bytechef/component/definition/ComponentDslAgentChannelTest.java`
- `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/domain/AgentChannelDefinition.java`
- `server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/ComponentDefinitionRegistryAgentChannelTest.java`
- `server/libs/modules/components/slack/src/main/java/com/bytechef/component/slack/trigger/SlackNewAiAgentMessageTrigger.java` — **the only new operation** (bot-echo decline; see spec §7)
- *(conditional)* a WhatsApp request trigger, only if the evaluator cannot index arrays — decided inside Task 10
- `server/libs/automation/automation-ai/automation-ai-agent/automation-ai-agent-service/src/main/java/com/bytechef/automation/ai/agent/channel/ResolvedAgentChannel.java`
- `server/libs/automation/automation-ai/automation-ai-agent/automation-ai-agent-service/src/main/java/com/bytechef/automation/ai/agent/channel/AgentChannelResolver.java`
- `client/src/graphql/automation/agent/aiAgentChannelDefinitions.graphql`
- `client/src/pages/automation/agents/hooks/useAiAgentChannelDefinitions.ts`

**Modify**
- `sdks/backend/java/component-api/.../ComponentDefinition.java`, `ComponentDsl.java`
- `server/libs/platform/platform-component/platform-component-api/.../domain/ComponentDefinition.java`, `.../service/ComponentDefinitionService.java`
- `server/libs/platform/platform-component/platform-component-service/.../ComponentDefinitionRegistry.java`, `.../service/ComponentDefinitionServiceImpl.java`, `.../index/ComponentIndex.java`, `.../index/ComponentIndexGenerator.java`
- `server/ee/libs/platform/platform-component/platform-component-remote-client/.../RemoteComponentDefinitionServiceClient.java`
- `server/libs/platform/platform-configuration/platform-configuration-rest/platform-configuration-rest-impl/openapi.yaml`
- Seven component handlers (chat, workflow, slack, telegram, whatsApp, rocketchat, twilio, infobip —
  **not** schedule) plus the existing triggers/actions they pair, each gaining an `.agentRequest(...)` /
  `.agentReply(...)` descriptor; `WorkflowConstants`
- `automation-ai-agent-api/.../channel/AiAgentChannelType.java`; `automation-ai-agent-service`: `AiAgentWorkflowGenerator.java`, `AiAgentFacadeImpl.java`, `build.gradle.kts`, tests, `definition/agent_workflow_two_channels.json`
- `automation-ai-agent-graphql/.../AiAgentGraphQlController.java`, `ai-agent.graphqls`
- Client: `AgentChannelsCard.tsx`, `AgentApprovalSettings.tsx`, `AgentScheduleCard.tsx`, `agent-deployments/components/AgentDeploymentChannelList.tsx` (+ tests)
- Docs: `.agents/agents.md`, `CLAUDE.md` (Agents section), `docs/content/docs/developer-guide/component-specification/component.mdx`, `docs/superpowers/specs/2026-08-10-agents-design.md` (supersede note)

**Delete**
- `automation-ai-agent-service/.../channel/ChannelDefinitions.java`, `ChannelDefinition.java`, and `ChannelDefinitionsTest.java`

---

### Task 1: SDK — agent descriptors (`agentRequest`/`agentReply`), `AgentChannelDefinition`, DSL + validation

Read spec §4 first — it is the authority on the three types and every validation rule. This task builds
exactly what it specifies.

**Files:**
- Create: `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/AgentRequestDefinition.java`
- Create: `.../definition/AgentReplyDefinition.java`
- Create: `.../definition/AgentChannelDefinition.java`
- Create: `.../definition/AgentChannelComponentDefinition.java`
- Modify: `.../definition/ComponentDefinition.java:28-29` (extend `AgentChannelComponentDefinition`)
- Modify: `.../definition/ComponentDsl.java` — factories near `tool(...)` (~line 248); `ModifiableComponentDefinition` (~1298); `ModifiableTriggerDefinition.agentRequest(...)`; `ModifiableActionDefinition.agentReply(...)`; three new `Modifiable*` classes next to `ModifiableUnifiedApiDefinition` (~3941)
- Test: `.../src/test/java/com/bytechef/component/definition/ComponentDslAgentChannelTest.java`

**Interfaces produced:**

```java
public interface AgentRequestDefinition {
    String getConversationIdPath();          // defaults to AgentChannelDefinition.CONVERSATION_ID
    String getMessagePath();                 // defaults to AgentChannelDefinition.MESSAGE
    Optional<String> getAttachmentsPath();
}

public interface AgentReplyDefinition {
    String getMessageProperty();                    // defaults to AgentChannelDefinition.MESSAGE
    Optional<String> getConversationIdProperty();
    Optional<String> getAttachmentsProperty();
    Map<String, String> getChannelParameters();   // channel-row key -> action property
    Map<String, Object> getFixedParameters();            // fixed values
}

public interface AgentChannelDefinition {
    String ATTACHMENTS = "attachments";
    String CONVERSATION_ID = "conversationId";
    String MESSAGE = "message";
    List<String> CONTRACT_PROPERTY_NAMES = List.of(ATTACHMENTS, CONVERSATION_ID, MESSAGE);

    String getName();
    Optional<String> getTitle();
    Optional<String> getDescription();
    TriggerDefinition getTrigger();
    Optional<ActionDefinition> getReplyAction();
    Optional<String> getApprovalChannelName();
    Map<String, Object> getTriggerParameters();
}

public interface AgentChannelComponentDefinition {
    default List<AgentChannelDefinition> getAgentChannels() { return List.of(); }
}
```

`TriggerDefinition` gains `Optional<AgentRequestDefinition> getAgentRequestDefinition()`;
`ActionDefinition` gains `Optional<AgentReplyDefinition> getAgentReplyDefinition()`. Both default to
`Optional.empty()` so no existing implementor breaks — check whether these interfaces already use
default methods before adding (`TriggerDefinition.java`, `ActionDefinition.java`).

**DSL surface:** `agentRequest()`, `agentReply()`, `agentChannel(name, trigger)`,
`agentChannel(name, trigger, replyAction)`, `agentChannelRequest()` (the contract object schema, used by
Slack's new trigger in Task 8), `ModifiableComponentDefinition.agentChannels(...)`.

**Validation** — spec §4 is normative; restated here as the acceptance criteria:

*From `ModifiableAgentReplyDefinition`, when attached via `.agentReply(...)` on an action* (it needs the
action's properties, so validate at attach time, not in the `agentReply()` factory): every mapped name —
`messageProperty`, `conversationIdProperty`, `attachmentsProperty`, each `parameterProperties` **value**,
each `parameters` **key** — must be a declared property of that action. A name may be **dotted**, in
which case only its first segment is checked: `workflow/responseToWorkflowCall` receives the agent's text
at `response.message`, where `response` is a `dynamicProperties` map whose members are not statically
known (Task 6). This is the strong half of validation.

*From `agentChannel(...)`:*
1. `name` matches `[a-zA-Z0-9]+`.
2. Trigger carries an `AgentRequestDefinition`; reply action, when present, carries an `AgentReplyDefinition`.
3. Each `parameterProperties` **key** is a property name of the paired trigger.
4. Trigger paths checked **only** when the trigger declares a static object output schema: each path's
   first dot-segment must be one of its properties. Schema-less (`.output()`) and function-valued
   outputs are accepted unchecked — this is the documented gap in spec §4, not an oversight. Do **not**
   require the trigger's output to declare the contract fields; that requirement is gone.

- [ ] **Step 1: Write the failing test** — `ComponentDslAgentChannelTest`, one case per rule:

  - accepts a conforming identity pair (chat-shaped: trigger output declares the contract, reply
    declares `message`), asserting `getName`/`getTitle`/`getTrigger`/`getReplyAction`/
    `getApprovalChannelName`/`getTriggerParameters`
  - accepts a legacy pair via paths: trigger output `{update_id, message:{...}}`, request descriptor
    `conversationId("message.chat.id").message("message.text")`, reply mapping onto `chat_id`/`text`
  - accepts a schema-less trigger output (`.output()`) with arbitrary paths — no exception
  - accepts a function-valued trigger output
  - accepts a channel with no reply action
  - rejects a bad `name` (`"bad-name"`), message mentions `name`
  - rejects `.agentReply(...)` naming a property the action does not declare, message names the property
  - rejects a `parameterProperties` key that is not a trigger property, message names the key
  - rejects a trigger with no `AgentRequestDefinition`, and a reply action with no `AgentReplyDefinition`
  - rejects a path whose first segment is absent from a **declared** trigger output schema
  - `ComponentDsl.component("plain").getAgentChannels()` is `List.of()`; a component with
    `.agentChannels(definition)` returns exactly it

- [ ] **Step 2: Run to verify it fails**

`./gradlew :sdks:backend:java:component-api:test --tests '*ComponentDslAgentChannelTest*' > /tmp/t1.log 2>&1; echo $?; grep -c 'error:' /tmp/t1.log`
Expected: non-zero exit, compilation errors.

- [ ] **Step 3: Implement the four interfaces + three modifiable classes + DSL factories**

Follow the existing `ModifiableUnifiedApiDefinition` style: private constructor, fluent setters
returning `this`, `Optional.ofNullable` getters, `Map.copyOf` on stored maps. `ModifiableComponentDefinition`
gets the `agentChannels` field/builders/getter and includes it in `equals`/`hashCode`/`toString`.

Path checking needs the trigger's declared output; reuse whatever `Optional<OutputDefinition>
getOutputDefinition()` / `getOutputResponse()` accessors the file already uses (confirm names in
`TriggerDefinition.java` / `BaseOutputDefinition.java`) and treat *absent output response* as
"unvalidatable", not as an error.

- [ ] **Step 4: Run the test to verify it passes**

`./gradlew :sdks:backend:java:component-api:test --tests '*ComponentDslAgentChannelTest*' > /tmp/t1.log 2>&1; echo $?; grep '^> Task .* FAILED' /tmp/t1.log`
Expected: exit 0, no FAILED tasks.

- [ ] **Step 5: Format and commit**

```bash
./gradlew :sdks:backend:java:component-api:spotlessApply
git add sdks/backend/java/component-api
git commit -m "732 Add SDK agent channel and request/reply descriptors"
```

---

### Task 2: Platform DTO, registry lookup + load-time validation, service methods, remote stub

**Files:**
- Create: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/domain/AgentChannelDefinition.java`
- Modify: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/domain/ComponentDefinition.java:41-143`
- Modify: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/service/ComponentDefinitionService.java`
- Modify: `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/ComponentDefinitionRegistry.java` (`getComponentDefinitions()` at 482; `validate(...)` at 847)
- Modify: `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/ComponentDefinitionServiceImpl.java`
- Modify: `server/ee/libs/platform/platform-component/platform-component-remote-client/src/main/java/com/bytechef/ee/platform/component/remote/client/service/RemoteComponentDefinitionServiceClient.java`
- Test: `server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/ComponentDefinitionRegistryAgentChannelTest.java`

**Interfaces:**
- Consumes: SDK `AgentChannelDefinition` (Task 1).
- Produces: platform `com.bytechef.platform.component.domain.AgentChannelDefinition(name, title, description, componentName, componentVersion, triggerName, replyActionName, approvalChannelName, triggerParameters)` **plus the flattened binding the generator reads** — `conversationIdPath`, `messagePath`, `attachmentsPath` (nullable) copied from the trigger's `AgentRequestDefinition`, and `replyMessageProperty`, `replyConversationIdProperty` (nullable), `replyAttachmentsProperty` (nullable), `replyChannelParameters`, `replyFixedParameters` copied from the reply action's `AgentReplyDefinition` (all null/empty when there is no reply action). Flattened, not nested — spec §5 explains why; `ComponentDefinition.getAgentChannels()`; `ComponentDefinitionRegistry.getAgentChannelDefinitions()` / `fetchAgentChannelDefinition(String name)`; `ComponentDefinitionService.getAgentChannelDefinitions()` / `fetchAgentChannelDefinition(String name)`.

- [ ] **Step 1: Write the failing registry test**

Look at an existing test in that module that constructs a `ComponentDefinitionRegistry` with hand-built handlers (search `new ComponentDefinitionRegistry(` under `platform-component-service/src/test`) and mirror its constructor arguments (an `ApplicationProperties`, a `List<ComponentHandler>`, a `Supplier<List<ComponentHandlerEntry>>` returning `List.of()`, an empty dynamic-registry list, and `() -> Optional.empty()` for the index). Then:

```java
package com.bytechef.platform.component;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.agentChannel;
import static com.bytechef.component.definition.ComponentDsl.agentChannelRequest;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.AgentChannelDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ComponentDefinitionRegistryAgentChannelTest {

    private static final ModifiableTriggerDefinition TRIGGER = trigger("newAiAgentMessage")
        .type(TriggerType.STATIC_WEBHOOK)
        .output(outputSchema(agentChannelRequest()));
    private static final ModifiableActionDefinition ACTION = action("sendAiAgentReply")
        .properties(string(AgentChannelDefinition.MESSAGE), string(AgentChannelDefinition.CONVERSATION_ID));

    @Test
    void testGetAgentChannelDefinitionsCollectsAcrossComponents() {
        ComponentDefinitionRegistry registry = registry(
            handler(component("acme").triggers(TRIGGER).actions(ACTION)
                .agentChannels(agentChannel("acme", TRIGGER, ACTION).title("Acme"))),
            handler(component("beta").triggers(TRIGGER)
                .agentChannels(agentChannel("beta", TRIGGER))));

        List<com.bytechef.platform.component.domain.AgentChannelDefinition> definitions =
            registry.getAgentChannelDefinitions();

        assertThat(definitions).extracting("name").containsExactly("acme", "beta");
        assertThat(registry.fetchAgentChannelDefinition("acme"))
            .map(com.bytechef.platform.component.domain.AgentChannelDefinition::getReplyActionName)
            .contains("sendAiAgentReply");
        assertThat(registry.fetchAgentChannelDefinition("beta"))
            .map(com.bytechef.platform.component.domain.AgentChannelDefinition::getReplyActionName)
            .contains((String) null);
        assertThat(registry.fetchAgentChannelDefinition("nope")).isEmpty();
    }

    @Test
    void testDuplicateAgentChannelNamesFailFast() {
        ComponentDefinitionRegistry registry = registry(
            handler(component("acme").triggers(TRIGGER).agentChannels(agentChannel("dup", TRIGGER))),
            handler(component("beta").triggers(TRIGGER).agentChannels(agentChannel("dup", TRIGGER))));

        assertThatThrownBy(registry::getAgentChannelDefinitions)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("dup");
    }

    @Test
    void testAgentChannelReferencingUnregisteredTriggerFailsFast() {
        ComponentDefinitionRegistry registry = registry(
            handler(component("acme").agentChannels(agentChannel("acme", TRIGGER))));

        assertThatThrownBy(registry::getAgentChannelDefinitions)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("newAiAgentMessage");
    }

    private static ComponentHandler handler(ComponentDefinition componentDefinition) {
        return () -> componentDefinition;
    }

    private static ComponentDefinitionRegistry registry(ComponentHandler... componentHandlers) {
        // mirror the constructor call used by the existing registry tests in this module
        return new ComponentDefinitionRegistry(
            new com.bytechef.config.ApplicationProperties(), List.of(componentHandlers), List::of, List.of(),
            Optional::empty);
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests '*ComponentDefinitionRegistryAgentChannelTest*' > /tmp/t2.log 2>&1; echo $?`
Expected: non-zero (compilation errors — `getAgentChannelDefinitions` missing).

- [ ] **Step 3: Add the platform DTO**

```java
package com.bytechef.platform.component.domain;

import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Platform mirror of the SDK {@link com.bytechef.component.definition.AgentChannelDefinition}: functions and
 * definition objects collapse to names, exactly as {@link ActionDefinition}/{@link TriggerDefinition} do.
 *
 * @author Ivica Cardic
 */
public class AgentChannelDefinition {

    private @Nullable String approvalChannelName;
    private @Nullable String attachmentsPath;
    private String componentName;
    private int componentVersion;
    private String conversationIdPath;
    private @Nullable String description;
    private String messagePath;
    private String name;
    private @Nullable String replyActionName;
    private @Nullable String replyAttachmentsProperty;
    private @Nullable String replyConversationIdProperty;
    private @Nullable String replyMessageProperty;
    private Map<String, String> replyChannelParameters;
    private Map<String, Object> replyFixedParameters;
    private @Nullable String title;
    private String triggerName;
    private Map<String, Object> triggerParameters;

    private AgentChannelDefinition() {
    }

    public AgentChannelDefinition(
        com.bytechef.component.definition.AgentChannelDefinition agentChannelDefinition, String componentName,
        int componentVersion) {

        this.approvalChannelName = agentChannelDefinition.getApprovalChannelName()
            .orElse(null);
        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.description = agentChannelDefinition.getDescription()
            .orElse(null);
        this.name = agentChannelDefinition.getName();
        this.replyActionName = agentChannelDefinition.getReplyAction()
            .map(com.bytechef.component.definition.ActionDefinition::getName)
            .orElse(null);
        this.title = agentChannelDefinition.getTitle()
            .orElse(null);
        this.triggerName = agentChannelDefinition.getTrigger()
            .getName();
        this.triggerParameters = Map.copyOf(agentChannelDefinition.getTriggerParameters());

        // The binding, flattened. The request descriptor is always present (Task 1 validates it); the reply
        // half is null/empty as a block when there is no reply action.
        AgentRequestDefinition agentRequestDefinition = agentChannelDefinition.getTrigger()
            .getAgentRequestDefinition()
            .orElseThrow();

        this.attachmentsPath = agentRequestDefinition.getAttachmentsPath()
            .orElse(null);
        this.conversationIdPath = agentRequestDefinition.getConversationIdPath();
        this.messagePath = agentRequestDefinition.getMessagePath();

        Optional<AgentReplyDefinition> agentReplyDefinition = agentChannelDefinition.getReplyAction()
            .flatMap(com.bytechef.component.definition.ActionDefinition::getAgentReplyDefinition);

        this.replyAttachmentsProperty = agentReplyDefinition
            .flatMap(AgentReplyDefinition::getAttachmentsProperty)
            .orElse(null);
        this.replyConversationIdProperty = agentReplyDefinition
            .flatMap(AgentReplyDefinition::getConversationIdProperty)
            .orElse(null);
        this.replyMessageProperty = agentReplyDefinition
            .map(AgentReplyDefinition::getMessageProperty)
            .orElse(null);
        this.replyChannelParameters = agentReplyDefinition
            .map(AgentReplyDefinition::getChannelParameters)
            .orElseGet(Map::of);
        this.replyFixedParameters = agentReplyDefinition
            .map(AgentReplyDefinition::getFixedParameters)
            .orElseGet(Map::of);
    }

    // getters for every field; equals/hashCode on all fields; toString.
}
```

Write the getters (`getApprovalChannelName`, `getComponentName`, `getComponentVersion`, `getDescription`, `getName`, `getReplyActionName`, `getTitle`, `getTriggerName`, `getTriggerParameters`), `equals`/`hashCode` (`Objects.equals` over all fields / `Objects.hash`) and `toString` in the style of `ClusterElementDefinition` in the same package.

- [ ] **Step 4: Expose it on the platform `ComponentDefinition` DTO**

In `domain/ComponentDefinition.java`: add field `private List<AgentChannelDefinition> agentChannels;`, in the SDK-mapping constructor after `this.actions = getActions(componentDefinition);` add

```java
        this.agentChannels = componentDefinition.getAgentChannels()
            .stream()
            .map(
                agentChannelDefinition -> new AgentChannelDefinition(
                    agentChannelDefinition, componentDefinition.getName(), componentDefinition.getVersion()))
            .toList();
```

plus `public List<AgentChannelDefinition> getAgentChannels()`, and include the field in `equals`/`hashCode`/`toString`. Also add `agentChannels` to the copy/`with*` constructors if the class has any (check `ComponentDefinition(ComponentDefinition componentDefinition, ...)` overloads and keep them field-complete).

- [ ] **Step 5: Registry lookup + validation**

In `ComponentDefinitionRegistry` add (near `getComponentDefinitions()`):

```java
    /**
     * Every agent channel declared by every loaded component (ServiceLoader-indexed, Spring-declared and dynamic
     * alike — this goes through the full-load path, never the index stubs), validated fail-fast: unique names, and
     * trigger/reply action/approval element must be registered on the declaring component.
     */
    public List<com.bytechef.platform.component.domain.AgentChannelDefinition> getAgentChannelDefinitions() {
        Map<String, com.bytechef.platform.component.domain.AgentChannelDefinition> definitionsByName =
            new LinkedHashMap<>();

        for (ComponentDefinition componentDefinition : getComponentDefinitions()) {
            for (AgentChannelDefinition agentChannelDefinition : componentDefinition.getAgentChannels()) {
                validateAgentChannel(componentDefinition, agentChannelDefinition);

                com.bytechef.platform.component.domain.AgentChannelDefinition previous = definitionsByName.putIfAbsent(
                    agentChannelDefinition.getName(),
                    new com.bytechef.platform.component.domain.AgentChannelDefinition(
                        agentChannelDefinition, componentDefinition.getName(), componentDefinition.getVersion()));

                if (previous != null) {
                    throw new IllegalStateException(
                        "Agent channel name '%s' is declared by both component '%s' and component '%s'".formatted(
                            agentChannelDefinition.getName(), previous.getComponentName(),
                            componentDefinition.getName()));
                }
            }
        }

        return List.copyOf(definitionsByName.values());
    }

    public Optional<com.bytechef.platform.component.domain.AgentChannelDefinition> fetchAgentChannelDefinition(
        String name) {

        return getAgentChannelDefinitions()
            .stream()
            .filter(agentChannelDefinition -> Objects.equals(agentChannelDefinition.getName(), name))
            .findFirst();
    }

    private static void validateAgentChannel(
        ComponentDefinition componentDefinition, AgentChannelDefinition agentChannelDefinition) {

        String componentName = componentDefinition.getName();
        String triggerName = agentChannelDefinition.getTrigger()
            .getName();

        if (componentDefinition.getTriggers()
            .stream()
            .noneMatch(triggerDefinition -> Objects.equals(triggerDefinition.getName(), triggerName))) {

            throw new IllegalStateException(
                "Agent channel '%s' references trigger '%s' which component '%s' does not register".formatted(
                    agentChannelDefinition.getName(), triggerName, componentName));
        }

        agentChannelDefinition.getReplyAction()
            .ifPresent(replyAction -> {
                if (componentDefinition.getActions()
                    .stream()
                    .noneMatch(actionDefinition -> Objects.equals(actionDefinition.getName(), replyAction.getName()))) {

                    throw new IllegalStateException(
                        "Agent channel '%s' references action '%s' which component '%s' does not register".formatted(
                            agentChannelDefinition.getName(), replyAction.getName(), componentName));
                }
            });

        agentChannelDefinition.getApprovalChannelName()
            .ifPresent(approvalChannelName -> {
                if (componentDefinition.getClusterElements()
                    .stream()
                    .noneMatch(
                        clusterElementDefinition -> Objects.equals(clusterElementDefinition.getName(), approvalChannelName)
                            && Objects.equals(clusterElementDefinition.getType(), APPROVAL_CHANNELS))) {

                    throw new IllegalStateException(
                        "Agent channel '%s' references approval channel '%s' which component '%s' does not declare".formatted(
                            agentChannelDefinition.getName(), approvalChannelName, componentName));
                }
            });
    }
```

Import `com.bytechef.platform.component.definition.ClusterElementTypes.APPROVAL_CHANNELS` (or wherever the constant lives — grep `APPROVAL_CHANNELS` in `platform-component-api`) and use the same comparison the existing `getClusterElementDefinition(componentName, name, ClusterElementType)` (line 568) uses for type equality.

Memoize: wrap the body in a `Supplier` created with `MemoizationUtils.memoize(...)` stored in a field (`agentChannelDefinitionsSupplier`), initialised in the constructor, exactly like `allComponentDefinitionsSupplier` — the scan touches every handler and must run once. Note that memoizing means the fail-fast exception surfaces on the first call; that is intended.

- [ ] **Step 6: Service interface, impl, remote stub**

`ComponentDefinitionService`:

```java
    Optional<AgentChannelDefinition> fetchAgentChannelDefinition(String name);

    List<AgentChannelDefinition> getAgentChannelDefinitions();
```

`ComponentDefinitionServiceImpl` — filter through visibility like `getComponentDefinitions(...)` does (`isComponentVisible(componentName)`):

```java
    @Override
    public Optional<AgentChannelDefinition> fetchAgentChannelDefinition(String name) {
        return componentDefinitionRegistry.fetchAgentChannelDefinition(name)
            .filter(agentChannelDefinition -> isComponentVisible(agentChannelDefinition.getComponentName()));
    }

    @Override
    public List<AgentChannelDefinition> getAgentChannelDefinitions() {
        return componentDefinitionRegistry.getAgentChannelDefinitions()
            .stream()
            .filter(agentChannelDefinition -> isComponentVisible(agentChannelDefinition.getComponentName()))
            .toList();
    }
```

`RemoteComponentDefinitionServiceClient` (EE): both methods `throw new UnsupportedOperationException();`, matching the file's other stubs.

- [ ] **Step 7: Run tests**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests '*ComponentDefinitionRegistryAgentChannelTest*' > /tmp/t2.log 2>&1; echo $?; grep '^> Task .* FAILED' /tmp/t2.log`
Expected: exit 0.

Then compile everything that implements the service interface: `./gradlew compileJava --continue > /tmp/c2.log 2>&1; echo $?; grep '^> Task .* FAILED' /tmp/c2.log` — expected exit 0 (if any other `ComponentDefinitionService` implementation exists, add the two methods there too).

- [ ] **Step 8: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-component server/ee/libs/platform/platform-component
git commit -m "732 Expose agent channel definitions through the component registry"
```

---

### Task 3: Component index carries agent channels; stub rebuilds them

**Files:**
- Modify: `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/index/ComponentIndex.java` (`Entry` at 286, `toStubComponentDefinition` at 129, summary records at 393-419)
- Modify: `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/index/ComponentIndexGenerator.java` (`toEntry` at 101)
- Test: `server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/index/ComponentIndexTest.java`

**Interfaces:**
- Consumes: SDK `agentChannel(...)`.
- Produces: `ComponentIndex.AgentChannelSummary(name, title, description, triggerName, replyActionName, approvalChannelName)`; `Entry.agentChannels`.

- [ ] **Step 1: Write the failing test** — add to `ComponentIndexTest`:

```java
    @Test
    void testAgentChannelsRoundTripThroughStub() {
        ModifiableTriggerDefinition triggerDefinition = trigger("newAiAgentMessage")
            .type(TriggerType.STATIC_WEBHOOK)
            .output(outputSchema(agentChannelRequest()));
        ModifiableActionDefinition actionDefinition = action("sendAiAgentReply")
            .properties(string(AgentChannelDefinition.MESSAGE));
        ComponentDefinition componentDefinition = component("acme")
            .triggers(triggerDefinition)
            .actions(actionDefinition)
            .agentChannels(agentChannel("acme", triggerDefinition, actionDefinition).title("Acme"));

        ComponentIndex.Entry entry = ComponentIndexGenerator.toEntry(componentDefinition, "Provider", "service-loader");
        ComponentDefinition stub = ComponentIndex.toStubComponentDefinition(entry);

        assertThat(stub.getAgentChannels()).hasSize(1);
        assertThat(stub.getAgentChannels().getFirst().getName()).isEqualTo("acme");
        assertThat(stub.getAgentChannels().getFirst().getTrigger().getName()).isEqualTo("newAiAgentMessage");
        assertThat(stub.getAgentChannels().getFirst().getReplyAction()).map(ActionDefinition::getName)
            .contains("sendAiAgentReply");
    }
```

(`toEntry` is `private static` today — make it package-private, or drive it through the existing public generator entry point the test file already uses; keep whichever the file's existing tests do.)

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests '*ComponentIndexTest*' > /tmp/t3.log 2>&1; echo $?`
Expected: non-zero.

- [ ] **Step 3: Implement**

`ComponentIndex`: add record `public record AgentChannelSummary(String name, @Nullable String title, @Nullable String description, String triggerName, @Nullable String replyActionName, @Nullable String approvalChannelName, String conversationIdPath, String messagePath, @Nullable String attachmentsPath, @Nullable String replyMessageProperty, @Nullable String replyConversationIdProperty, @Nullable String replyAttachmentsProperty, Map<String, String> replyChannelParameters, Map<String, Object> replyFixedParameters) {}` — the binding rides in the index because a stub without it generates a **wrong** workflow, which is worse than a stub that does not appear at all (spec §5); add `@Nullable List<AgentChannelSummary> agentChannels` to `Entry` **immediately after `clusterElementTypes`** (record component order is JSON field order; every constructor call site — generator, tests — must pass the new component). In `toStubComponentDefinition`, before the `clusterElementTypes` early-return, and only when `entry.agentChannels()` is non-empty:

```java
        List<AgentChannelSummary> agentChannels = entry.agentChannels();

        if (agentChannels != null && !agentChannels.isEmpty()) {
            componentDefinition.agentChannels(
                agentChannels
                    .stream()
                    .map(agentChannel -> toStubAgentChannelDefinition(componentDefinition, agentChannel))
                    .toList());
        }
```

with

```java
    private static AgentChannelDefinition toStubAgentChannelDefinition(
        ModifiableComponentDefinition componentDefinition, AgentChannelSummary summary) {

        TriggerDefinition triggerDefinition = componentDefinition.getTriggers()
            .stream()
            .filter(candidate -> Objects.equals(candidate.getName(), summary.triggerName()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Index entry '%s' agent channel '%s' references unknown trigger '%s'".formatted(
                    componentDefinition.getName(), summary.name(), summary.triggerName())));

        // Rebuild the request descriptor from the summary. Stub triggers carry no output schema, so Task 1's
        // path check is skipped for them anyway — the descriptor is what must survive, since the generator
        // reads its paths and a stub missing them would wire the workflow to the wrong expressions.
        if (triggerDefinition instanceof ModifiableTriggerDefinition modifiableTrigger) {
            modifiableTrigger.agentRequest(
                agentRequest()
                    .conversationId(summary.conversationIdPath())
                    .message(summary.messagePath())
                    .attachments(summary.attachmentsPath()));
        }

        ModifiableAgentChannelDefinition definition;

        if (summary.replyActionName() == null) {
            definition = agentChannel(summary.name(), triggerDefinition);
        } else {
            ActionDefinition actionDefinition = componentDefinition.getActions()
                .stream()
                .filter(candidate -> Objects.equals(candidate.getName(), summary.replyActionName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                    "Index entry '%s' agent channel '%s' references unknown action '%s'".formatted(
                        componentDefinition.getName(), summary.name(), summary.replyActionName())));

            // Stub actions carry no properties, so declare the mapped ones before attaching the reply
            // descriptor — Task 1 validates every mapped name against the action's properties, and a stub
            // with none would fail that check. Stubs never reach detail/execution paths (class Javadoc).
            if (actionDefinition instanceof ModifiableActionDefinition modifiableAction
                && modifiableAction.getProperties().isEmpty()) {

                modifiableAction.properties(
                    Stream.of(
                        Stream.of(summary.replyMessageProperty(), summary.replyConversationIdProperty(),
                            summary.replyAttachmentsProperty()),
                        summary.replyChannelParameters().values().stream(),
                        summary.replyFixedParameters().keySet().stream())
                        .flatMap(Function.identity())
                        .filter(Objects::nonNull)
                        .map(propertyName -> string(propertyName.split("\\.")[0]))
                        .distinct()
                        .toList());

                modifiableAction.agentReply(
                    agentReply()
                        .conversationId(summary.replyConversationIdProperty())
                        .message(summary.replyMessageProperty())
                        .attachments(summary.replyAttachmentsProperty())
                        .channelParameters(summary.replyChannelParameters())
                        .fixedParameters(summary.replyFixedParameters()));
            }

            definition = agentChannel(summary.name(), triggerDefinition, actionDefinition);
        }

        return definition
            .title(summary.title())
            .description(summary.description())
            .approvalChannel(summary.approvalChannelName());
    }
```

Note the ordering constraint: the stub's `triggers(...)`/`actions(...)` must already be applied when this runs — put the block after those calls in `toStubComponentDefinition`.

`ComponentIndexGenerator.toEntry`: pass

```java
            componentDefinition.getAgentChannels()
                .stream()
                .map(agentChannel -> new AgentChannelSummary(
                    agentChannel.getName(),
                    agentChannel.getTitle().orElse(null),
                    agentChannel.getDescription().orElse(null),
                    agentChannel.getTrigger().getName(),
                    agentChannel.getReplyAction().map(ActionDefinition::getName).orElse(null),
                    agentChannel.getApprovalChannelName().orElse(null)))
                .toList(),
```

in the `Entry` constructor at the new position.

- [ ] **Step 4: Run tests, then regenerate the index once to be sure it still builds**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests '*ComponentIndexTest*' > /tmp/t3.log 2>&1; echo $?; grep '^> Task .* FAILED' /tmp/t3.log`
Run: `./gradlew :server:apps:server-app:generateComponentIndex > /tmp/idx.log 2>&1; echo $?`
Expected: both exit 0.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-component
git commit -m "732 Record agent channels in the build-time component index"
```

---

### Task 4: REST/OpenAPI + generated TS types

**Files:**
- Modify: `server/libs/platform/platform-configuration/platform-configuration-rest/platform-configuration-rest-impl/openapi.yaml` (`ComponentDefinition:` ~1975; add a new `AgentChannelDefinition` schema)
- Regenerate: the REST server model (`./gradlew :server:libs:platform:platform-configuration:platform-configuration-rest:platform-configuration-rest-impl:openApiGenerate` — check the module's build file for the exact generate task name) and the client TS (`cd client && npm run generate` or the script the repo uses for `src/shared/middleware/platform/configuration` — grep `package.json` for `openapi`).

- [ ] **Step 1: Add the schema**

Under `components.schemas` add:

```yaml
    AgentChannelDefinition:
      description: "A trigger/reply-action pair through which an AI Agent can be reached."
      type: "object"
      required:
        - "componentName"
        - "componentVersion"
        - "name"
        - "triggerName"
      properties:
        approvalChannelName:
          type: "string"
        componentName:
          type: "string"
        componentVersion:
          type: "integer"
        description:
          type: "string"
        name:
          type: "string"
        replyActionName:
          type: "string"
        title:
          type: "string"
        triggerName:
          type: "string"
        triggerParameters:
          type: "object"
          additionalProperties: true
```

and on `ComponentDefinition.properties` add:

```yaml
        agentChannels:
          description: "The AI Agent channels this component declares."
          type: "array"
          items:
            $ref: "#/components/schemas/AgentChannelDefinition"
```

- [ ] **Step 2: Regenerate + map**

Run the module's OpenAPI generate task; if the REST mapper (`ComponentDefinitionMapper` / MapStruct in `platform-configuration-rest-impl`) is field-name based it maps automatically — run `./gradlew :server:libs:platform:platform-configuration:platform-configuration-rest:platform-configuration-rest-impl:compileJava > /tmp/c4.log 2>&1; echo $?` and fix any unmapped-target warnings that fail the build. Regenerate the client TS types and run `cd client && npm run typecheck`.

- [ ] **Step 3: Commit (server and client separately)**

```bash
git add server/libs/platform/platform-configuration
git commit -m "732 Add agentChannels to the component definition REST model"
git add client/src/shared/middleware
git commit -m "732 client - Regenerate component definition types with agentChannels"
```

---

## Tasks 5–13: component declarations

Spec §7 holds the authoritative table. Tasks 5, 6, 8–13 are independent of each other once Tasks 1–4
land and may run in parallel worktrees. **Task 7 is dropped** (see below) — the numbering is preserved
so task references elsewhere in this plan stay valid.

**The shared recipe** (every component task follows it; only the descriptor values differ):

1. Add `.agentRequest(agentRequest()…)` to the request trigger's definition and
   `.agentReply(agentReply()…)` to the reply action's definition. Nothing else about those operations
   changes — same name, same properties, same perform/webhookRequest.
2. Add `.agentChannels(agentChannel(<storedKey>, <trigger>, <replyAction>).title(…).approvalChannel(…))`
   to the component handler. The stored key must match the existing `ai_agent_channel.channel_type`
   value exactly — see the Global Constraints list.
3. Write/extend a component test asserting the declared pair constructs (it will throw from
   `agentChannel(...)` otherwise) **and** that each descriptor path/property matches the operation's real
   shape. For a trigger with a declared output schema, assert the path resolves against it; for a
   schema-less one, assert against the payload the existing `webhookRequest` returns.
4. Regenerate the definition snapshot and **read it**: `rm -f src/test/resources/definition/<name>_v1.json`,
   `rm -rf build/resources/test/definition`, rerun the module's tests, then open the JSON and confirm the
   `agentChannels` block and the descriptors on the trigger/action.
5. `./gradlew spotlessApply`, then commit.

Property-name constants: use each component's existing constants (`SlackConstants.CHANNEL`, etc.), never
string literals — a renamed constant must break compilation, which is the whole point of declaring this
on the component.

---

### Task 5: `chat` — identity descriptors

`newChatRequest` already emits `{conversationId, message, attachments}` and `responseToRequest` already
declares `message`/`attachments`, so both descriptors are near-empty:

- trigger: `.agentRequest(agentRequest())`
- action: `.agentReply(agentReply().attachments(ATTACHMENTS))`
- handler: `agentChannel("chat", …).title("Chat").approvalChannel("chat")`

This task is the proof that an already-conforming component pays almost nothing.

```bash
git commit -m "732 Declare the chat agent channel"
```

---

### Task 6: `workflow` — `workflowCall` channel over the existing operations

No new action (the previously planned `responseToAiAgentCall` is dropped). Reuse
`WorkflowNewWorkflowCallTrigger` and `WorkflowResponseToWorkflowCallAction`, both untouched apart from
descriptors.

- The trigger's output is function-valued off its `inputSchema` property, so conformance comes from
  pinning that schema on the channel:
  `agentChannel("workflowCall", TRIGGER_DEFINITION, ACTION_DEFINITION).triggerParameters(Map.of(INPUT_SCHEMA, <contract JSON schema>))`
  where the contract schema is `{conversationId (required, string), message (string), attachments (array)}`.
  Put it on `WorkflowConstants` as `AI_AGENT_CALL_INPUT_SCHEMA`; Task 14's generator test references it.
- trigger: `.agentRequest(agentRequest())` — identity, because the pinned schema produces the contract names.
- action: `responseToWorkflowCall` declares `outputSchema` plus `dynamicProperties(response)`. The agent's
  text goes **inside** the response map, so:
  `.agentReply(agentReply().message(RESPONSE + ".message").fixedParameter(OUTPUT_SCHEMA, AI_AGENT_CALL_OUTPUT_SCHEMA))`
  This is the one dotted **reply** target in the codebase. Confirm Task 1's reply validation accepts a
  dotted name by checking its first segment (`response`) against the action's declared properties — if
  Task 1 did not implement it that way, fix Task 1 rather than working around it here.
- no `approvalChannel` (a workflow caller is not a human).
- The generator must therefore write a **nested** parameter (`{outputSchema: …, response: {message: "${aiAgent_1}"}}`).
  Verify that in Task 14's snapshot, not here.

**Do not rename or re-register the trigger.** `SubflowResolver`, `AutomationMcpToolFacade` and
`AutomationA2AServerFacade` resolve `workflow/v1/newWorkflowCall` by name.

```bash
git commit -m "732 Declare the workflowCall agent channel"
```

---

### Task 7: DROPPED — `schedule` is not an agent channel

Spec §3 settles this: `schedule` keeps the existing `schedule/v1/cron` trigger, the schedule component
declares nothing, and the agent module keeps one explicit non-channel branch for it (handled in Task 14).
**No work in this task.** Do not add a trigger, a descriptor or an `agentChannels(...)` call to the
schedule component.

---

### Task 8: `slack` — the one new trigger, plus reply descriptor

The only new operation in this plan. `anyEvent` fires for every workspace event including the bot's own
reply, and declining means returning an empty collection — behaviour a descriptor cannot express.

- Create `SlackNewAiAgentMessageTrigger`: same `TriggerType` and webhook plumbing as
  `SlackAnyEventTrigger` (read it and mirror it), output `outputSchema(agentChannelRequest())`,
  `webhookRequest` returning **`List.of()`** when the event carries `bot_id` or
  `subtype == "bot_message"`, otherwise a single-element list (or the object) with
  `conversationId = channel`, `message = text`, `attachments = []`.
  Empty collection ⇒ `TriggerCompletionHandler` creates zero jobs (`TriggerCompletionHandler.java:127`).
- trigger: `.agentRequest(agentRequest())` — identity, it emits the contract.
- action: `SlackSendChannelMessageAction` gains
  `.agentReply(agentReply().conversationId(CHANNEL).message(TEXT))`. Note `sendChannelMessage` also
  declares `post_at`; leave it unmapped so replies post immediately.
- handler: `agentChannel("slack", …).title("Slack").approvalChannel("slack")`.
- **Failing test first**, per TDD: `SlackNewAiAgentMessageTriggerTest` with three cases — a real user
  message maps through; a `bot_id` event returns an empty collection; a `subtype == bot_message` event
  returns an empty collection.

```bash
git commit -m "732 Add Slack AI agent message trigger and declare the slack agent channel"
```

---

### Task 9: `telegram` — descriptors only

- trigger `newMessage` (declares output `{update_id, message:OBJECT}`):
  `.agentRequest(agentRequest().conversationId("message.chat.id").message("message.text"))`.
  `message.text` is a declared STRING in the snapshot; `message.chat` is a declared OBJECT — confirm
  `chat.id` in `MESSAGE_OUTPUT_PROPERTIES` and use the existing constants for the path segments.
- action `sendMessage` (`chat_id`, `text`, `direct_messages_topic_id`):
  `.agentReply(agentReply().conversationId(CHAT_ID).message(TEXT))`.
- handler: `agentChannel("telegram", …).title("Telegram").approvalChannel("telegram")`.

```bash
git commit -m "732 Declare the telegram agent channel"
```

---

### Task 10: `whatsApp` — descriptors, after settling the array-path question

**Start with the open question, before writing anything.** `messageReceived` declares its output as
`{object: STRING, entry: OBJECT}`, but Meta's real payload is `entry: [ { changes: [ { value: { messages: [ … ] } } ] } ]`.
Its `webhookRequest` returns the raw body (like telegram's and rocketchat's), so the declared schema may
simply be imprecise.

1. Determine whether the evaluator resolves array indices in an expression path (`${node.entry[0].changes[0]…}`).
   Look in `server/libs/core/evaluator/` and its tests; write a small evaluator test if the answer is not
   obvious from existing ones.
2. **If it does:** no new operation. Declare
   `.agentRequest(agentRequest().conversationId(<path to the sender wa_id>).message(<path to text.body>))`
   and fix the trigger's declared output schema to say `entry` is an ARRAY (it is currently wrong, and a
   correct schema is what makes the path checkable).
3. **If it does not:** create a request trigger on the Slack model that parses the payload in
   `webhookRequest` and emits the contract directly, with an identity descriptor. Record which branch you
   took in the commit message.

- action `sendMessage` (`body`, `to`): `.agentReply(agentReply().conversationId(TO).message(BODY))`.
- handler: `agentChannel("whatsapp", …).title("WhatsApp").approvalChannel("whatsApp")` — stored key is
  lowercase `whatsapp`, the component is `whatsApp`; do not "fix" either.
- The trigger also declares `senderNumber`; leave it as ordinary configuration.
- **whatsApp stays OUT of the client's add menu** (revised 2026-08-18). This line previously said Task 16 re-enables it, which assumed this task would settle the array-shape question; it does not — see the spec §7 note. `ADDABLE_CHANNEL_TYPES` in `AgentChannelsCard.tsx` keeps excluding `whatsapp`.

```bash
git commit -m "732 Declare the whatsApp agent channel"
```

---

### Task 11: `rocketchat` — descriptors only

- trigger `newMessage` declares `.output()` with **no schema**, and `webhookRequest` returns the raw
  outgoing-webhook body. Paths are therefore unvalidatable at build time (spec §4's documented gap) —
  read the payload the webhook actually sends and pin the paths in the component test:
  `.agentRequest(agentRequest().conversationId("channel_id").message("text"))`.
- action `sendChannelMessage` (`roomId`, `text`): `.agentReply(agentReply().conversationId(ROOM_ID).message(TEXT))`.
- handler: `agentChannel("rocketchat", …).title("Rocket.Chat").approvalChannel("rocketchat")`.

```bash
git commit -m "732 Declare the rocketchat agent channel"
```

---

### Task 12: `twilio` — descriptors plus one additive trigger property

Twilio is the case that motivated `parameterProperties`: the reply's sender number is configured on the
channel row and must reach the action's `From`.

- **`newWhatsappMessage` currently declares no properties at all**, so there is no `number` to key the
  row on. Add one optional property to the existing trigger:
  `string(NUMBER).label("Number").description("The WhatsApp-enabled Twilio number this channel listens on.")`
  — additive, so no existing workflow breaks. Add `NUMBER` to `TwilioConstants` if absent.
- trigger: `.agentRequest(agentRequest().conversationId("From").message("Body"))` — schema-less output,
  so pin the payload shape in the component test.
- action `sendWhatsAppMessage` (`To`, `From`, `useTemplate`, `ContentSid`, `ContentVariables`, `Body`):
  `.agentReply(agentReply().conversationId(TO).message(BODY).channelParameter(NUMBER, FROM).fixedParameter(USE_TEMPLATE, false))`.
  `useTemplate` defaults to `true` and gates `Body` behind `displayCondition("useTemplate == false")`, so
  pinning it false is what makes free-text replies work at all.
- handler: `agentChannel("twilio", …).title("Twilio").approvalChannel("whatsApp")` — the approval element
  is `whatsApp`, per spec §7; verify that element exists on the twilio component before declaring it, or
  registration validation (Task 2) fails at load.
- Behaviour change to state in the commit: the reply's sender is the configured number, not the inbound
  `To`. Per-row `branch_out` (Task 14) is what lets two numbers coexist.

```bash
git commit -m "732 Declare the twilio agent channel"
```

---

### Task 13: `infobip` — descriptors only

Infobip needs no trigger change: `newWhatsappMessage` **already declares `number`** (plus `keyword`), so
the row key exists.

- trigger: `.agentRequest(agentRequest()…)` with paths into the Infobip inbound payload — schema-less
  output, so read `webhookRequest` and pin the shape in the component test.
- action `sendWhatsappTextMessage` (`from`, `to`, `text`):
  `.agentReply(agentReply().conversationId(TO).message(TEXT).channelParameter(NUMBER, FROM))`.
- handler: `agentChannel("infobip", …).title("Infobip").approvalChannel("whatsApp")` — same
  approval-element check as Task 12.
- `newSMS` also declares `number`/`keyword`. Do **not** declare a second channel for it in this plan;
  the pairing call exists precisely so that adding `infobipSms` later is a one-line change.

```bash
git commit -m "732 Declare the infobip agent channel"
```

---

### Task 14: Agent module — resolver, generic generator, delete `ChannelDefinitions`

> **Carry-forward from the Hub-visibility plan (executed first).**
> `docs/superpowers/plans/2026-08-17-agent-run-hub-visibility.md` Task 3 stamps `aiAgentId` and
> `ownerUserId` onto the `aiAgent_1` node in `AiAgentWorkflowGenerator`, so the agent chat action can
> report a conversation without depending on `automation-ai-agent`. **This task's rewrite must preserve
> that stamp.** Dropping it silently stops agent chats being created — grep the generator for the stamp
> before and after, and keep the Hub-visibility plan's Task 4/5 tests green.


**Files:**
- Create: `automation-ai-agent-service/.../channel/ResolvedAgentChannel.java`, `.../channel/AgentChannelResolver.java`
- Modify: `automation-ai-agent-api/.../channel/AiAgentChannelType.java` (keep CHAT/WORKFLOW_CALL/SCHEDULE only)
- Modify: `automation-ai-agent-service/build.gradle.kts` (add `implementation(project(":server:libs:platform:platform-component:platform-component-api"))`)
- Modify: `automation-ai-agent-service/.../util/AiAgentWorkflowGenerator.java`, `.../facade/AiAgentFacadeImpl.java`
- Delete: `.../channel/ChannelDefinitions.java`, `.../channel/ChannelDefinition.java`, `src/test/.../channel/ChannelDefinitionsTest.java`
- Test: `AiAgentWorkflowGeneratorTest.java`, `AiAgentWorkflowExecutionIntTest.java`, snapshot `src/test/resources/definition/agent_workflow_two_channels.json`, `workflows/agent_workflow_slack_bot_guard.json` (delete — the guard is gone)

**Interfaces:**
- Consumes: `ComponentDefinitionService.getAgentChannelDefinitions()` / `fetchAgentChannelDefinition(String)`, `ComponentDefinitionService.getComponentDefinition(name, version)` (for trigger/action property definitions and `isConnectionRequired()`).
- Produces: `ResolvedAgentChannel` record; `AiAgentWorkflowGenerator.generate(agent, channels, elements, subAgentResolver, Function<String, ResolvedAgentChannel> channelResolver)`.

- [ ] **Step 1: `ResolvedAgentChannel` + `AgentChannelResolver`**

```java
/**
 * Everything the generator needs about one channel key, resolved once per generation from the component registry.
 *
 * @param name                    the channel key (ai_agent_channel.channel_type)
 * @param triggerType             "<component>/v<version>/<triggerName>"
 * @param replyActionType         "<component>/v<version>/<actionName>", or null when the channel has no reply
 * @param connectionRequired      whether the component needs a connection (trigger and reply share it)
 * @param triggerParameters       fixed parameters from the SDK definition (workflowCall's inputSchema)
 * @param triggerPropertyDefaults declared trigger property defaultValues by name (only non-null defaults)
 * @param triggerPropertyNames    every declared trigger property name — the row-parameter allow-list
 * @param replyPropertyNames      every declared reply-action property name
 * @param approvalDelivery        (componentName, approvalChannelName) or null
 */
public record ResolvedAgentChannel(
    String name, String triggerType, @Nullable String replyActionType, boolean connectionRequired,
    Map<String, Object> triggerParameters, Map<String, Object> triggerPropertyDefaults,
    Set<String> triggerPropertyNames, Set<String> replyPropertyNames, Binding binding,
    @Nullable ApprovalDelivery approvalDelivery) {

    /**
     * Where the contract fields live on each end, copied from the DTO. This is what makes the generator
     * component-agnostic: telegram's "message.chat.id" and chat's "conversationId" take the same code path.
     */
    public record Binding(
        String conversationIdPath, String messagePath, @Nullable String attachmentsPath,
        @Nullable String replyMessageProperty, @Nullable String replyConversationIdProperty,
        @Nullable String replyAttachmentsProperty, Map<String, String> replyChannelParameters,
        Map<String, Object> replyFixedParameters) {
    }

    public record ApprovalDelivery(String componentName, String elementName) {
    }

    public String componentName() {
        return triggerType.substring(0, triggerType.indexOf('/'));
    }
}
```

`AgentChannelResolver` (`@Component`, constructor-injects `ComponentDefinitionService`):

**The schedule exception lives here, once.** `schedule` is not a channel (spec §3), so the registry
cannot supply it. Rather than branching in the generator's every method, the resolver synthesizes a
`ResolvedAgentChannel` for it: `triggerType = "schedule/v1/cron"`, no reply action,
`connectionRequired = false`, and `triggerPropertyNames`/`triggerPropertyDefaults` read from the **real**
`schedule` component's `cron` trigger via `ComponentDefinitionService` — so the row-parameter allow-list
stays correct without hardcoding `expression`/`timezone`. Its `Binding` is unused: the generator's
one remaining `if (AiAgentChannelType.SCHEDULE…)` in `buildEnvelope` overrides `text` with the row's
`prompt` parameter and `conversationId` with `scheduleConversationId(agent, channel)` before the binding
paths would be consulted. Give the synthesized binding null-ish placeholders and assert in a test that
the schedule envelope never contains a `${schedule_1.…}` expression.

```java
    public Optional<ResolvedAgentChannel> resolve(String channelName) {
        if (AiAgentChannelType.SCHEDULE.equals(channelName)) {
            return Optional.of(resolveSchedule());
        }

        return componentDefinitionService.fetchAgentChannelDefinition(channelName)
            .map(this::toResolvedAgentChannel);
    }

    public List<ResolvedAgentChannel> resolveAll() {
        return componentDefinitionService.getAgentChannelDefinitions()
            .stream()
            .map(this::toResolvedAgentChannel)
            .toList();
    }

    private ResolvedAgentChannel toResolvedAgentChannel(AgentChannelDefinition definition) {
        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            definition.getComponentName(), definition.getComponentVersion());
        TriggerDefinition triggerDefinition = componentDefinition.getTriggers()
            .stream()
            .filter(candidate -> Objects.equals(candidate.getName(), definition.getTriggerName()))
            .findFirst()
            .orElseThrow();
        String prefix = definition.getComponentName() + "/v" + definition.getComponentVersion() + "/";
        Map<String, Object> triggerPropertyDefaults = new LinkedHashMap<>();
        Set<String> triggerPropertyNames = new LinkedHashSet<>();

        for (Property property : triggerDefinition.getProperties()) {
            triggerPropertyNames.add(property.getName());

            if (property instanceof ValueProperty<?> valueProperty && valueProperty.getDefaultValue() != null) {
                triggerPropertyDefaults.put(property.getName(), valueProperty.getDefaultValue());
            }
        }

        Set<String> replyPropertyNames = new LinkedHashSet<>();
        String replyActionType = null;

        if (definition.getReplyActionName() != null) {
            replyActionType = prefix + definition.getReplyActionName();

            componentDefinition.getActions()
                .stream()
                .filter(candidate -> Objects.equals(candidate.getName(), definition.getReplyActionName()))
                .findFirst()
                .orElseThrow()
                .getProperties()
                .forEach(property -> replyPropertyNames.add(property.getName()));
        }

        return new ResolvedAgentChannel(
            definition.getName(), prefix + definition.getTriggerName(), replyActionType,
            componentDefinition.isConnectionRequired(), definition.getTriggerParameters(), triggerPropertyDefaults,
            triggerPropertyNames, replyPropertyNames,
            definition.getApprovalChannelName() == null
                ? null
                : new ResolvedAgentChannel.ApprovalDelivery(definition.getComponentName(), definition.getApprovalChannelName()));
    }
```

(Use the platform `Property`/`ValueProperty` DTO types from `com.bytechef.platform.component.domain` — check their `getDefaultValue()` accessor name.)

- [ ] **Step 2: Rewrite the generator's channel handling** (keep everything about elements/tools/HITL untouched)

- `generate(...)` gains a fifth parameter `Function<String, ResolvedAgentChannel> channelResolver`; `ChannelNode` becomes `record ChannelNode(AiAgentChannel channel, String nodeName, ResolvedAgentChannel definition)`; `buildChannelNodes` calls `channelResolver.apply(channelType)` and throws `IllegalArgumentException("Unknown agent channel type: " + channelType)` on null.
- `buildTrigger`: `type = definition.triggerType()`; parameters = `buildTriggerParameters(channel, definition)`:

```java
    private static Map<String, Object> buildTriggerParameters(AiAgentChannel channel, ResolvedAgentChannel definition) {
        Map<String, Object> parameters = new LinkedHashMap<>(definition.triggerPropertyDefaults());

        parameters.putAll(definition.triggerParameters());

        // Only declared trigger properties ride onto the node: UI-only row keys (a schedule's label/cadence
        // fields) stay on the row.
        for (Map.Entry<String, ?> entry : channel.getParameters().entrySet()) {
            if (definition.triggerPropertyNames().contains(entry.getKey())) {
                parameters.put(entry.getKey(), entry.getValue());
            }
        }

        return parameters;
    }
```

  `connections` when `definition.connectionRequired()`.
- `buildBranchIn`: every case is `List.of(envelopeSetTask)` — delete `buildSlackBotEchoGuard` and its constants; `buildEnvelope`:

```java
        ResolvedAgentChannel.Binding binding = definition.binding();

        envelope.put(FIELD_TEXT, "${" + nodeName + "." + binding.messagePath() + "}");
        envelope.put(FIELD_CONVERSATION_ID, "${" + nodeName + "." + binding.conversationIdPath() + "}");
        envelope.put(
            FIELD_ATTACHMENTS,
            binding.attachmentsPath() == null ? List.of() : "${" + nodeName + "." + binding.attachmentsPath() + "}");
        envelope.put(FIELD_CHANNEL, channel.getChannelType());
```

  **Except for `schedule`** (spec §3 — it is not a channel): keep the existing
  `if (AiAgentChannelType.SCHEDULE.equals(channel.getChannelType()))` branch that overrides `text` with the row's
  `prompt` parameter and `conversationId` with `scheduleConversationId(agent, channel)`. Keep `scheduleConversationId`
  and `SCHEDULE_PROMPT_PARAMETER`. **Delete `SCHEDULE_NAME_PARAMETER`** and the whole prompt/name exclusion loop at
  the old lines 546-554: `buildTriggerParameters` now restricts row parameters to `definition.triggerPropertyNames()`
  (cron declares only `expression`/`timezone`), so `prompt` and `name` are excluded generically and the bespoke
  exclusion list is dead code.

  Delete `FIELD_REPLY_TO/FROM`, `substitute*`, `WORKFLOW_CALL_*_SCHEMA`, `INPUT_SCHEMA_PARAMETER`, `OUTPUT_SCHEMA_PARAMETER`, `AGENT_OUTPUT_TOKEN`, `ENVELOPE_TOKEN_PREFIX`.
- `buildBranchOut`: expression `${__triggerName}`, one case per `ChannelNode` keyed by `nodeName`:

```java
    private static Map<String, Object> buildBranchOutCase(ChannelNode channelNode) {
        Map<String, Object> oneCase = new LinkedHashMap<>();

        oneCase.put("key", channelNode.nodeName());
        oneCase.put("tasks", channelNode.definition().replyActionType() == null
            ? List.of()
            : List.of(buildReplyTask(channelNode)));

        return oneCase;
    }

    private static Map<String, Object> buildReplyTask(ChannelNode channelNode) {
        ResolvedAgentChannel definition = channelNode.definition();
        Map<String, Object> parameters = new LinkedHashMap<>();

        ResolvedAgentChannel.Binding binding = definition.binding();

        // Every name below comes from the reply action's own descriptor — no component is named here.
        putReplyParameter(parameters, binding.replyMessageProperty(), AGENT_OUTPUT_EXPRESSION);

        if (binding.replyConversationIdProperty() != null) {
            putReplyParameter(
                parameters, binding.replyConversationIdProperty(),
                "${" + BRANCH_IN_NAME + "." + FIELD_CONVERSATION_ID + "}");
        }

        // attachments: deliberately unwired — aiAgent_1's output is a bare string today.

        // Row-configured properties (twilio's number -> From).
        for (Map.Entry<String, String> entry : binding.replyChannelParameters().entrySet()) {
            Object value = channelNode.channel().getParameters().get(entry.getKey());

            if (value != null) {
                putReplyParameter(parameters, entry.getValue(), value);
            }
        }

        // Pinned values (twilio useTemplate=false, workflowCall's outputSchema).
        for (Map.Entry<String, Object> entry : binding.replyFixedParameters().entrySet()) {
            putReplyParameter(parameters, entry.getKey(), entry.getValue());
        }

        Map<String, Object> replyTask = new LinkedHashMap<>();

        replyTask.put("name", "reply_" + channelNode.nodeName());
        replyTask.put("type", definition.replyActionType());
        replyTask.put("parameters", parameters);

        if (definition.connectionRequired()) {
            replyTask.put(WorkflowExtConstants.CONNECTIONS, buildConnections(definition.replyActionType()));
        }

        return replyTask;
    }

    /**
     * A descriptor may target a nested parameter — workflow/responseToWorkflowCall receives the agent's text at
     * "response.message", where "response" is a dynamicProperties map. A dotted name therefore becomes a nested
     * map rather than a key containing a dot.
     */
    @SuppressWarnings("unchecked")
    private static void putReplyParameter(Map<String, Object> parameters, String propertyName, Object value) {
        int index = propertyName.indexOf('.');

        if (index < 0) {
            parameters.put(propertyName, value);

            return;
        }

        Map<String, Object> nested = (Map<String, Object>) parameters.computeIfAbsent(
            propertyName.substring(0, index), key -> new LinkedHashMap<String, Object>());

        putReplyParameter(nested, propertyName.substring(index + 1), value);
    }
```

- `buildApprovalDeliveryChannels`: read `channelResolver.apply(channel.getChannelType()).approvalDelivery()`.
- Any remaining `AiAgentChannelType.SLACK/TELEGRAM/...` references must go; `CHAT`/`WORKFLOW_CALL` ordering in `buildChannelNodes` stays.

- [ ] **Step 3: Facade**

Inject `AgentChannelResolver`; `regenerateAndSaveWorkflow` / the other `generate` call pass `channelName -> agentChannelResolver.resolve(channelName).orElse(null)` (resolve once into a `Map<String, ResolvedAgentChannel>` for the agent's channel names to keep generation deterministic and cheap). `validateChannelType`/`isKnownChannelType` → `agentChannelResolver.resolve(channelType).isPresent()`; `validateForPublish` uses `resolved.connectionRequired()`. Add a facade method `List<ResolvedAgentChannel> getAgentChannelDefinitions()` → `agentChannelResolver.resolveAll()` (used by Task 15).

- [ ] **Step 4: Tests**

- `AiAgentWorkflowGeneratorTest`: build a `Map<String, ResolvedAgentChannel>` fixture in a test helper, mirroring the real migrated components (spec §7):
  - `chat` — `chat/v1/newChatRequest` + `chat/v1/responseToRequest`, identity binding, defaults `{mode: 1}`
  - `workflowCall` — `workflow/v1/newWorkflowCall` + `workflow/v1/responseToWorkflowCall`, identity request binding, `triggerParameters {inputSchema: WorkflowConstants.AI_AGENT_CALL_INPUT_SCHEMA}`, reply `messageProperty = "response.message"` and `replyFixedParameters {outputSchema: …}`
  - `schedule` — `schedule/v1/cron`, no reply, trigger property names `expression,timezone` (**not** `prompt`)
  - `slack` — `slack/v1/newMessage` + `slack/v1/sendChannelMessage`, reply binding `conversationId→channel`, `message→text`, connectionRequired, approval `(slack, slack)`
  - `telegram` — request binding `conversationId→"message.chat.id"`, `message→"message.text"`; reply `chat_id`/`text`
  - `twilio` — `replyChannelParameters {number: From}` and `replyFixedParameters {useTemplate: false}`
- Rewrite `testBranchOutCaseKeysAreChannelTypes` → `testBranchOutCaseKeysAreTriggerNodeNames`; delete the two slack-guard tests. Add:
  - `testEnvelopeUsesRequestBindingPaths` — telegram's envelope reads `${telegram_1.message.chat.id}`, proving no component name reaches the generator
  - `testReplyTaskWiresRowParameterProperty` (twilio `number` → `From`) and `testReplyTaskWiresPinnedParameters` (`useTemplate: false`)
  - `testReplyTaskWritesNestedParameterForDottedProperty` — workflowCall produces `{response: {message: "${aiAgent_1}"}}`, not a key literally named `response.message`
  - `testScheduleEnvelopeUsesRowPromptAndStableConversationId` — and asserts the schedule envelope contains no `${schedule_1.…}` expression
  - `testTriggerParametersExcludeUiOnlyRowKeys` — a schedule row carrying `prompt`/`name` yields a `cron` node with only `expression`/`timezone`, which is what lets `SCHEDULE_NAME_PARAMETER` and the old exclusion loop be deleted
- Regenerate `agent_workflow_two_channels.json` (delete then rerun; then READ the new file and check: chat trigger `{"mode":1}`, telegram case key `telegram_1`, `reply_telegram_1` with `chat_id: "${branch_in.conversationId}"`, no `replyTo` in any envelope).
- `AiAgentWorkflowExecutionIntTest`: delete the slack-guard workflow fixture and its test; keep the two-channel envelope test (envelope now `{text, conversationId, attachments, channel}`).
- Delete `ChannelDefinitionsTest`.

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-agent:automation-ai-agent-service:test :server:libs:automation:automation-ai:automation-ai-agent:automation-ai-agent-service:testIntegration --continue > /tmp/t14.log 2>&1; echo $?; grep '^> Task .* FAILED' /tmp/t14.log`
Expected: exit 0.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-ai/automation-ai-agent
git commit -m "732 Generate agent workflows from registry-declared agent channels"
```

---

### Task 15: GraphQL `aiAgentChannelDefinitions` query + client hook

**Files:**
- Modify: `automation-ai-agent-graphql/src/main/resources/graphql/ai-agent.graphqls`, `.../AiAgentGraphQlController.java`, `AiAgentFacade` (api) + impl
- Create: `client/src/graphql/automation/agent/aiAgentChannelDefinitions.graphql`, `client/src/pages/automation/agents/hooks/useAiAgentChannelDefinitions.ts`
- Regenerate: `cd client && npx graphql-codegen`

- [ ] **Step 1: Schema**

```graphql
extend type Query {
    aiAgentChannelDefinitions: [AiAgentChannelDefinition!]!
}

type AiAgentChannelDefinition {
    channelType: String!
    componentName: String!
    componentVersion: Int!
    triggerName: String!
    replyActionName: String
    title: String!
    description: String
    icon: String
    connectionRequired: Boolean!
    approvalCapable: Boolean!
    pinned: Boolean!
    schedule: Boolean!
}
```

- [ ] **Step 2: Controller + facade**

Facade method `List<AiAgentChannelDefinitionDTO> getAiAgentChannelDefinitions()` — builds from `AgentChannelResolver.resolveAll()` plus the component's `title`/`icon` (via `ComponentDefinitionService.getComponentDefinition`), with `pinned = CHAT.equals(name) || WORKFLOW_CALL.equals(name)` (the existing client concept: auto-created, lock icon, no delete — see `AgentChannelsCard.tsx:23`, whose `PINNED_CHANNEL_TYPES` array this replaces), `schedule = SCHEDULE.equals(name)`, `approvalCapable = approvalDelivery != null`, `title = definition title || component title`.

`resolveAll()` must include the synthesized `schedule` entry (it is not in the registry — spec §3), otherwise the client's schedule card loses its metadata. Assert that in the facade test: the returned list contains `schedule` with `schedule = true`, `pinned = false`, `connectionRequired = false`, `replyActionName = null`. Controller:

```java
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<AiAgentChannelDefinitionDTO> aiAgentChannelDefinitions() {
        return agentFacade.getAiAgentChannelDefinitions();
    }
```

Add a controller unit test if the file has a test class (mirror `workspaceChatAgents`' test).

- [ ] **Step 3: Client operation + hook**

`aiAgentChannelDefinitions.graphql`:

```graphql
query aiAgentChannelDefinitions {
    aiAgentChannelDefinitions {
        approvalCapable
        channelType
        componentName
        componentVersion
        connectionRequired
        description
        icon
        pinned
        replyActionName
        schedule
        title
        triggerName
    }
}
```

Run `cd client && npx graphql-codegen`. Hook:

```ts
import {useAiAgentChannelDefinitionsQuery} from '@/shared/middleware/graphql';

export function useAiAgentChannelDefinitions() {
    const {data, isLoading} = useAiAgentChannelDefinitionsQuery();

    const definitions = data?.aiAgentChannelDefinitions ?? [];

    return {
        addableDefinitions: definitions.filter((definition) => !definition.pinned && !definition.schedule),
        definitionsByType: Object.fromEntries(definitions.map((definition) => [definition.channelType, definition])),
        definitions,
        isLoading,
    };
}
```

- [ ] **Step 4: Commit (server, then client operations + generated file separately per convention)**

```bash
./gradlew spotlessApply && git add server/libs/automation/automation-ai/automation-ai-agent && git commit -m "732 Add aiAgentChannelDefinitions GraphQL query"
cd client && npm run format && cd ..
git add client/src/graphql/automation/agent/aiAgentChannelDefinitions.graphql client/src/pages/automation/agents/hooks && git commit -m "732 client - Add aiAgentChannelDefinitions operation and hook"
git add client/src/shared/middleware/graphql.ts client/src/shared/middleware/graphql-types.ts && git commit -m "732 client - Regenerate GraphQL client for aiAgentChannelDefinitions"
```

---

### Task 16: Client — replace the hardcoded channel maps

**Files:**
- Modify: `client/src/pages/automation/agents/components/detail/AgentChannelsCard.tsx:21-84` and the two `ComponentConfigDialog` targets (~335, ~364)
- Modify: `client/src/pages/automation/agents/components/detail/AgentApprovalSettings.tsx:24-32`
- Modify: `client/src/pages/automation/agent-deployments/components/AgentDeploymentChannelList.tsx`
- Modify: `client/src/pages/automation/agents/components/detail/AgentScheduleCard.tsx` (keep `CHANNEL_TYPE = 'schedule'`; nothing else to derive)
- Tests: `AgentChannelsCard.test.tsx`, `AgentApprovalSettings.test.tsx`, `AgentDeploymentChannelList.test.tsx`

- [ ] **Step 1: Failing test** — in `AgentChannelsCard.test.tsx` mock the hook via `vi.hoisted` (CLAUDE.md pattern):

```ts
const {channelDefinitionsMock} = vi.hoisted(() => ({
    channelDefinitionsMock: vi.fn(),
}));

vi.mock('@/pages/automation/agents/hooks/useAiAgentChannelDefinitions', () => ({
    useAiAgentChannelDefinitions: channelDefinitionsMock,
}));

// in beforeEach:
channelDefinitionsMock.mockReturnValue({
    addableDefinitions: [{approvalCapable: true, channelType: 'slack', componentName: 'slack', componentVersion: 1,
        connectionRequired: true, description: null, icon: null, pinned: false, replyActionName: 'sendChannelMessage',
        schedule: false, title: 'Slack', triggerName: 'newMessage'}],
    definitions: [...],
    definitionsByType: {...},
    isLoading: false,
});

it('lists addable channels from the definitions query', async () => { /* open Add Channel, expect 'Slack' option, expect no 'Telegram' */ });
```

- [ ] **Step 2: Run to fail** — `cd client && npx vitest run src/pages/automation/agents/components/detail/AgentChannelsCard.test.tsx`

- [ ] **Step 3: Implement**

Delete `PINNED_CHANNEL_TYPES`, `ADDABLE_CHANNEL_TYPES`, `CHANNEL_TYPE_LABELS`, `MESSAGING_CHANNEL_COMPONENT_NAMES`, `CHANNEL_TRIGGER_NAMES`, `CHANNEL_ICON_COMPONENT_NAMES`. Use `const {addableDefinitions, definitionsByType} = useAiAgentChannelDefinitions();` — labels `definitionsByType[type]?.title || type`, icons by `definitionsByType[type]?.componentName`, pinned via `definitionsByType[type]?.pinned`. Config-dialog targets:

```tsx
target={{
    clusterElementName: definitionsByType[editingChannel.channelType]?.triggerName ?? '',
    componentName: definitionsByType[editingChannel.channelType]?.componentName ?? '',
    componentVersion: definitionsByType[editingChannel.channelType]?.componentVersion ?? 1,
    kind: 'TRIGGER',
    title: definitionsByType[editingChannel.channelType]?.title || editingChannel.channelType,
}}
```

`AgentApprovalSettings`: replace `APPROVAL_DELIVERY_CHANNEL_LABELS` with `definitions.filter((definition) => definition.approvalCapable)`. `AgentDeploymentChannelList`: labels from `definitionsByType`. Keep sort-keys order and the `I`/`Props` interface suffix rules.

- [ ] **Step 4: Verify + commit**

```bash
cd client && npm run format && npm run check
git add client/src/pages/automation/agents client/src/pages/automation/agent-deployments
git commit -m "732 client - Drive agent channel UI from aiAgentChannelDefinitions"
```

---

### Task 17: Docs + full verification

**Files:**
- Modify: `.agents/agents.md` (§"Channel registry" 118-167, the generated-shape diagram 56-116, HITL table 323-345)
- Modify: `CLAUDE.md` "Agents (automation)" paragraph — replace "The channel registry (`…ChannelDefinitions.java`) is the single place to add a new channel" with the SDK declaration rule
- Modify: `docs/content/docs/developer-guide/component-specification/component.mdx` — add `agentChannels(...)` with the chat example and the contract table
- Modify: `docs/superpowers/specs/2026-08-10-agents-design.md` — add a one-line "Channel registry: superseded by `2026-08-17-sdk-agent-channels-design.md`" note at the top of that section

- [ ] **Step 1: Rewrite `.agents/agents.md` "Channel registry"** to describe: the SDK contract, `agentChannel(...)`, `getAgentChannelDefinitions()`, `AgentChannelResolver`, per-row `branch_out`, decline-to-fire via empty collection, the three pinned constants, and "To add a channel: declare a conforming trigger/action pair on the component and call `.agentChannels(...)` — nothing else". Update the shape diagram (`reply_<node>` per row) and remove the WhatsApp known-disabled paragraph, the Slack guard paragraph (now trigger-side), and `replyFrom`.
- [ ] **Step 2: CLAUDE.md** — in the Agents section replace the channel-registry sentence with: "Channels are declared by components: a trigger carries `.agentRequest(...)` and an action `.agentReply(...)` stating where the contract fields (`conversationId`, `message`, `attachments`) live on each end, and `ComponentDsl.agentChannel(name, trigger[, replyAction])` pairs them (spec `2026-08-17-sdk-agent-channels-design.md`). Existing operations are reused — only `slack/newMessage` was added, because declining an event means returning an empty collection. The generator reads the descriptors through `ComponentDefinitionService.getAgentChannelDefinitions()` and names no component. `AiAgentChannelType` keeps only `chat`/`workflowCall`/`schedule`: the first two because the facade pins them (auto-created, undeletable), `schedule` because it is **not a channel** — it has no reply and no conversation partner, stays on `schedule/v1/cron`, and is the generator's one deliberate branch."
- [ ] **Step 3: Full server + client verification**

```bash
./gradlew spotlessApply
./gradlew compileJava compileTestJava --continue > /tmp/final-compile.log 2>&1; echo $?; grep '^> Task .* FAILED' /tmp/final-compile.log
./gradlew :sdks:backend:java:component-api:test :server:libs:platform:platform-component:platform-component-service:test :server:libs:modules:components:chat:test :server:libs:modules:components:workflow:test :server:libs:modules:components:schedule:test :server:libs:modules:components:slack:test :server:libs:modules:components:telegram:test :server:libs:modules:components:whatsapp:test :server:libs:modules:components:rocketchat:test :server:libs:modules:components:twilio:test :server:libs:modules:components:infobip:test :server:libs:automation:automation-ai:automation-ai-agent:automation-ai-agent-service:test :server:libs:automation:automation-ai:automation-ai-agent:automation-ai-agent-graphql:test --continue > /tmp/final-test.log 2>&1; echo $?; grep '^> Task .* FAILED' /tmp/final-test.log
./gradlew :server:apps:server-app:generateComponentIndex > /tmp/final-idx.log 2>&1; echo $?
cd client && npm run check; cd ..
```

Expected: every `echo $?` is 0 and no `FAILED` lines. Then a smoke run: start the server (`./gradlew -p server/apps/server-app bootRun`), create an agent, add a Slack channel, publish, and confirm the generated workflow's `branch_out` has a `slack_1` case with `reply_slack_1`.

- [ ] **Step 4: Commit docs**

```bash
git add .agents/agents.md CLAUDE.md docs/content/docs/developer-guide/component-specification/component.mdx docs/superpowers/specs/2026-08-10-agents-design.md
git commit -m "732 Document SDK-declared agent channels"
```

---

## Self-review notes

- Spec §3 contract → Task 1 (descriptors + validation), Tasks 5–13 (each pair), Task 14 (`buildReplyTask` wires only names the reply descriptor declares).
- Spec §3 decline-to-fire → Task 8 (`List.of()` in Slack's new trigger), and Task 10 only if it takes the new-trigger branch.
- Spec §3 "what is not a channel" → Task 7 (dropped, deliberately), Task 14 (`AgentChannelResolver.resolveSchedule` + the single `if (SCHEDULE)` envelope branch), Task 15 (facade prepends the synthesized entry).
- Spec §4 SDK surface → Task 1 (all listed methods present, names match: `agentRequest`, `agentReply`, `agentChannel`, `agentChannelRequest`, `agentChannels`, `approvalChannel`, `triggerParameters`).
- Spec §4 documented validation gap (schema-less trigger outputs) → Task 1 accepts them; Tasks 11/12/13 compensate by pinning the payload shape in each component test.
- Spec §5 platform surface → Tasks 2 (DTO/registry/service/remote stub), 3 (index), 4 (REST).
- Spec §6 agent module → Task 14 (generator/facade/deletions), Task 15 (query), Task 16 (client).
- Spec §7 eight channels → Tasks 5, 6, 8–13 (Task 7 dropped; `schedule` is not a channel).
- Spec §8 testing → embedded in each task; the "no per-channel constants remain" guard is `AiAgentChannelType` shrinking (compile-time), which is stronger than a grep test — the grep test from the spec is therefore dropped.
- Type consistency: `ResolvedAgentChannel` field names used in Task 14's generator code match its record; `AiAgentChannelDefinition` GraphQL fields match the hook's return in Task 15/16.
