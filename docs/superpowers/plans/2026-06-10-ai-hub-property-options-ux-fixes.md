# AI Hub property-options UX & robustness fixes — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the AI Hub property-options lookup usable end-to-end: surface all fetched options, render large option sets as a searchable combobox, and make the lookup tool self-correct on wrong action/property names instead of dead-ending.

**Architecture:** Three independent fixes. (1) Server prompts tell the agent to pass all returned options, reuse successful lookups, and use canonical names. (2) Both lookup tool callbacks gain action/property existence checks that return `action_not_found`/`trigger_not_found`/`property_not_found` with the valid names, distinguishing a wrong name from a genuine `no_options_for_property`. (3) The client `askUserQuestion` renderer switches single-select to the existing `ComboBox` and multi-select to `MultiSelect` when option count exceeds a threshold.

**Tech Stack:** Java 25, Spring Boot 4, Spring AI, Jackson 3 (`tools.jackson`), JUnit 5 + Mockito + AssertJ (server); React 19 + TypeScript, Vitest + Testing Library + userEvent (client). EE server files keep the Enterprise license header + `@version ee`.

---

## Background facts (verified)

- `ActionDefinitionService` exposes `List<ActionDefinition> getActionDefinitions(String componentName, int componentVersion)` and `ActionDefinition getActionDefinition(String componentName, int componentVersion, String actionName)`. `TriggerDefinitionService` exposes the analogous `getTriggerDefinitions(...)` / `getTriggerDefinition(...)`.
- Domain `ActionDefinition`/`TriggerDefinition` have `String getName()` and `List<? extends Property> getProperties()`; domain `Property` has `String getName()`. Package: `com.bytechef.platform.component.domain`.
- The descriptor already emits canonical property names (`ToolUtils.generateParametersJson` → `property.getName()`), so the agent has them; the prompt must tell it to use them.
- Client `ComboBox` (`@/components/ComboBox`): props `{items: ComboBoxItemType[], onChange?: (item?) => void, value?, emptyMessage?}`; `ComboBoxItemType = {label, value, icon?}`. Searchable (cmdk), renders `role="option"` items inside a popover opened by a trigger button.
- Client `MultiSelect` (`@/components/MultiSelect/MultiSelect`): props `{options: {label, value}[], onValueChange: (string[]) => void, value?: string[], placeholder?}`.
- The lookup tool callbacks live in `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/`. `PropertyOptionsResolver` is in the same package.

Run server module tests with:
```bash
./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '<FQCN>'
```
Run client checks from `client/`:
```bash
npm run test -- src/pages/automation/ai-hub/messages/tests/AiHubAskUserQuestionMessage.test.tsx
npm run check
```

## File Structure

- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/PropertyOptionsResolver.java` — add `entityNotFoundEnvelope`.
- Modify: `.../tool/LookupActionPropertyOptionsToolCallback.java` — action/property existence checks.
- Modify: `.../tool/LookupTriggerPropertyOptionsToolCallback.java` — trigger/property existence checks.
- Modify tests: `PropertyOptionsResolverTest.java`, `LookupActionPropertyOptionsToolCallbackTest.java`, `LookupTriggerPropertyOptionsToolCallbackTest.java`.
- Modify: `.../resources/prompt_ai_hub_build.txt`, `.../resources/prompt_ai_hub_ask.txt`.
- Modify: `client/src/pages/automation/ai-hub/messages/AiHubAskUserQuestionMessage.tsx` — combobox/multiselect above threshold.
- Modify: `client/src/pages/automation/ai-hub/messages/tests/AiHubAskUserQuestionMessage.test.tsx`.

---

## Task 1: `PropertyOptionsResolver.entityNotFoundEnvelope`

**Files:**
- Test: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/PropertyOptionsResolverTest.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/PropertyOptionsResolver.java`

- [ ] **Step 1: Add the failing test**

Append these two tests inside the existing `PropertyOptionsResolverTest` class (before the closing brace):

```java
    @Test
    void testEntityNotFoundEnvelopeForAction() {
        Map<String, Object> envelope = resolver.entityNotFoundEnvelope(
            "action_not_found", "actionName", "sendMessage", List.of("sendChannelMessage", "sendDirectMessage"));

        assertThat(envelope.get("error")).isEqualTo("action_not_found");
        assertThat(envelope.get("actionName")).isEqualTo("sendMessage");
        assertThat(envelope.get("valid")).isEqualTo(List.of("sendChannelMessage", "sendDirectMessage"));
        assertThat(envelope.get("hint")).asString().contains("valid");
    }

    @Test
    void testEntityNotFoundEnvelopeForProperty() {
        Map<String, Object> envelope = resolver.entityNotFoundEnvelope(
            "property_not_found", "propertyName", "channelId", List.of("channel", "post_at", "text"));

        assertThat(envelope.get("error")).isEqualTo("property_not_found");
        assertThat(envelope.get("propertyName")).isEqualTo("channelId");
        assertThat(envelope.get("valid")).isEqualTo(List.of("channel", "post_at", "text"));
    }
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests 'com.bytechef.ee.ai.hub.tool.PropertyOptionsResolverTest'`
Expected: COMPILE FAILURE — `entityNotFoundEnvelope` does not exist.

- [ ] **Step 3: Add the helper**

In `PropertyOptionsResolver.java`, add this method after `noOptionsForPropertyEnvelope()` (before the class closing brace):

```java
    /**
     * Builds an {@code action_not_found} / {@code trigger_not_found} / {@code property_not_found} error envelope. The
     * {@code valid} list lets the LLM self-correct by retrying with a real name instead of treating a wrong-name guess
     * as "this entity has no options". {@code entityKey} is {@code "actionName"}, {@code "triggerName"}, or
     * {@code "propertyName"}.
     */
    public Map<String, Object> entityNotFoundEnvelope(
        String errorCode, String entityKey, String requested, List<String> valid) {

        Map<String, Object> envelope = new LinkedHashMap<>();

        envelope.put("error", errorCode);
        envelope.put(entityKey, requested);
        envelope.put("valid", valid);
        envelope.put(
            "hint",
            "No " + entityKey + " '" + requested + "' exists on this component. Retry with one of the names listed in"
                + " 'valid'.");

        return envelope;
    }
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests 'com.bytechef.ee.ai.hub.tool.PropertyOptionsResolverTest'`
Expected: PASS (7 tests: the prior 5 + these 2).

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/PropertyOptionsResolver.java \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/PropertyOptionsResolverTest.java
git commit -m "0_732 Add entityNotFoundEnvelope to PropertyOptionsResolver"
```

---

## Task 2: Action callback — action/property existence checks

**Files:**
- Test: `.../tool/LookupActionPropertyOptionsToolCallbackTest.java`
- Modify: `.../tool/LookupActionPropertyOptionsToolCallback.java`

The new existence checks run **before** `propertyHasOptionsDataSource`. Because of that, the existing tests (which only stub `propertyHasOptionsDataSource` etc.) must also stub `getActionDefinitions` + `getActionDefinition` so the action/property resolve as valid. We add a test helper for that.

- [ ] **Step 1: Add the new failing tests + update existing test mocks**

In `LookupActionPropertyOptionsToolCallbackTest.java`:

(a) Add imports at the top (alongside existing ones):

```java
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.Property;
```

(b) Add this private helper inside the test class (after the `toolContext()` helper):

```java
    private static void stubValidAction(
        ActionDefinitionService service, String component, int version, String action, String... propertyNames) {

        ActionDefinition listed = mock(ActionDefinition.class);

        when(listed.getName()).thenReturn(action);
        when(service.getActionDefinitions(component, version)).thenReturn(List.of(listed));

        ActionDefinition fetched = mock(ActionDefinition.class);

        List<Property> properties = new ArrayList<>();

        for (String name : propertyNames) {
            Property property = mock(Property.class);

            when(property.getName()).thenReturn(name);

            properties.add(property);
        }

        when(fetched.getProperties()).thenReturn(properties);
        when(service.getActionDefinition(component, version, action)).thenReturn(fetched);
    }
```

(c) In each of these existing tests, add the matching `stubValidAction(...)` call immediately after the `mock(ActionDefinitionService.class)` is created (so the action + property resolve as valid before the gate under test):

- `testReturnsNoOptionsWhenPropertyHasNoDataSource`: `stubValidAction(service, "slack", 1, "sendMessage", "text");`
- `testReturnsDependencyMissingWhenSiblingAbsent`: `stubValidAction(service, "googleSheets", 1, "appendRow", "sheetName");`
- `testReturnsConnectionRequiredWhenConnectionMissing`: `stubValidAction(service, "slack", 1, "sendMessage", "channel");`
- `testReturnsCappedOptionsAndTruncatedFlagOnSuccess`: `stubValidAction(service, "slack", 1, "sendMessage", "channel");`
- `testAcceptsDottedDependsOnSatisfiedByLastSegment`: `stubValidAction(service, "hubspot", 1, "createDeal", "pipelineStage");`

(`testRejectsBlankPropertyName` needs no stub — blank validation returns before the existence check.)

(d) Add the two new tests before the class closing brace:

```java
    @Test
    void testReturnsActionNotFoundWithValidNames() throws Exception {
        ActionDefinitionService service = mock(ActionDefinitionService.class);

        ActionDefinition real = mock(ActionDefinition.class);

        when(real.getName()).thenReturn("sendChannelMessage");
        when(service.getActionDefinitions("slack", 1)).thenReturn(List.of(real));

        LookupActionPropertyOptionsToolCallback callback = new LookupActionPropertyOptionsToolCallback(
            service, mock(ActionDefinitionFacade.class), resolver, mock(AiHubToolAttachMetrics.class), jsonMapper);

        String result = callback.call(
            "{\"componentName\":\"slack\",\"actionName\":\"sendMessage\",\"propertyName\":\"channel\"}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).isEqualTo("action_not_found");
        assertThat(node.get("valid")
            .get(0)
            .asText()).isEqualTo("sendChannelMessage");
    }

    @Test
    void testReturnsPropertyNotFoundWithValidNames() throws Exception {
        ActionDefinitionService service = mock(ActionDefinitionService.class);

        stubValidAction(service, "slack", 1, "sendChannelMessage", "channel", "post_at", "text");

        LookupActionPropertyOptionsToolCallback callback = new LookupActionPropertyOptionsToolCallback(
            service, mock(ActionDefinitionFacade.class), resolver, mock(AiHubToolAttachMetrics.class), jsonMapper);

        String result = callback.call(
            "{\"componentName\":\"slack\",\"actionName\":\"sendChannelMessage\",\"propertyName\":\"channelId\"}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).isEqualTo("property_not_found");

        List<String> validNames = new ArrayList<>();

        node.get("valid")
            .forEach(name -> validNames.add(name.asText()));

        assertThat(validNames).contains("channel");
    }
```

Ensure `java.util.ArrayList` is imported.

- [ ] **Step 2: Run to verify the new tests fail**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests 'com.bytechef.ee.ai.hub.tool.LookupActionPropertyOptionsToolCallbackTest'`
Expected: FAIL — `testReturnsActionNotFoundWithValidNames` / `testReturnsPropertyNotFoundWithValidNames` fail (the callback doesn't yet emit those errors; it currently returns `no_options_for_property`).

- [ ] **Step 3: Add the existence checks to the callback**

In `LookupActionPropertyOptionsToolCallback.java`:

(a) Add imports:

```java
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.Property;
```

(b) Insert this block immediately after the `int componentVersion = ...;` line and **before** the `if (!actionDefinitionService.propertyHasOptionsDataSource(...))` block:

```java
            List<String> validActionNames = actionDefinitionService.getActionDefinitions(componentName, componentVersion)
                .stream()
                .map(ActionDefinition::getName)
                .toList();

            if (!validActionNames.contains(actionName)) {
                metrics.recordStateVisibility(TOOL_NAME, "action_not_found");

                return jsonMapper.writeValueAsString(
                    resolver.entityNotFoundEnvelope("action_not_found", "actionName", actionName, validActionNames));
            }

            List<String> validPropertyNames = actionDefinitionService
                .getActionDefinition(componentName, componentVersion, actionName)
                .getProperties()
                .stream()
                .map(Property::getName)
                .toList();

            if (!validPropertyNames.contains(topPropertySegment(propertyName))) {
                metrics.recordStateVisibility(TOOL_NAME, "property_not_found");

                return jsonMapper.writeValueAsString(
                    resolver.entityNotFoundEnvelope(
                        "property_not_found", "propertyName", propertyName, validPropertyNames));
            }
```

(c) Add this private static helper (place it next to `toolError`):

```java
    /**
     * Returns the top-level container segment of a (possibly dotted / array) property path: {@code parent.child} →
     * {@code parent}, {@code items[].id} → {@code items}, {@code channel} → {@code channel}. Existence is checked only
     * at the top level; deeper resolution stays with the options engine.
     */
    private static String topPropertySegment(String propertyName) {
        int dot = propertyName.indexOf('.');
        int bracket = propertyName.indexOf('[');

        int cut;

        if (dot >= 0 && bracket >= 0) {
            cut = Math.min(dot, bracket);
        } else if (dot >= 0) {
            cut = dot;
        } else {
            cut = bracket;
        }

        return cut >= 0 ? propertyName.substring(0, cut) : propertyName;
    }
```

- [ ] **Step 4: Run to verify all pass**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests 'com.bytechef.ee.ai.hub.tool.LookupActionPropertyOptionsToolCallbackTest'`
Expected: PASS (all tests, including the updated existing ones and the 2 new ones).

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/LookupActionPropertyOptionsToolCallback.java \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/LookupActionPropertyOptionsToolCallbackTest.java
git commit -m "0_732 Distinguish action/property not-found from no-options in action lookup"
```

---

## Task 3: Trigger callback — trigger/property existence checks

**Files:**
- Test: `.../tool/LookupTriggerPropertyOptionsToolCallbackTest.java`
- Modify: `.../tool/LookupTriggerPropertyOptionsToolCallback.java`

Mirror of Task 2 using `TriggerDefinitionService.getTriggerDefinitions` / `getTriggerDefinition`, error code `trigger_not_found`, entityKey `triggerName`.

- [ ] **Step 1: Add new failing tests + update existing test mocks**

In `LookupTriggerPropertyOptionsToolCallbackTest.java`:

(a) Add imports:

```java
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.TriggerDefinition;
import java.util.ArrayList;
```

(b) Add the helper inside the class (after `toolContext()`):

```java
    private static void stubValidTrigger(
        TriggerDefinitionService service, String component, int version, String trigger, String... propertyNames) {

        TriggerDefinition listed = mock(TriggerDefinition.class);

        when(listed.getName()).thenReturn(trigger);
        when(service.getTriggerDefinitions(component, version)).thenReturn(List.of(listed));

        TriggerDefinition fetched = mock(TriggerDefinition.class);

        List<Property> properties = new ArrayList<>();

        for (String name : propertyNames) {
            Property property = mock(Property.class);

            when(property.getName()).thenReturn(name);

            properties.add(property);
        }

        when(fetched.getProperties()).thenReturn(properties);
        when(service.getTriggerDefinition(component, version, trigger)).thenReturn(fetched);
    }
```

(c) Add the matching `stubValidTrigger(...)` to each existing test right after the `mock(TriggerDefinitionService.class)`:

- `testReturnsNoOptionsWhenPropertyHasNoDataSource`: `stubValidTrigger(service, "slack", 1, "newMessage", "text");`
- `testReturnsDependencyMissingWhenSiblingAbsent`: `stubValidTrigger(service, "googleSheets", 1, "newRow", "sheetName");`
- `testReturnsConnectionRequiredWhenConnectionMissing`: `stubValidTrigger(service, "slack", 1, "newMessage", "channel");`
- `testReturnsCappedOptionsAndTruncatedFlagOnSuccess`: `stubValidTrigger(service, "slack", 1, "newMessage", "channel");`

(d) Add the two new tests before the class closing brace:

```java
    @Test
    void testReturnsTriggerNotFoundWithValidNames() throws Exception {
        TriggerDefinitionService service = mock(TriggerDefinitionService.class);

        TriggerDefinition real = mock(TriggerDefinition.class);

        when(real.getName()).thenReturn("newMessage");
        when(service.getTriggerDefinitions("slack", 1)).thenReturn(List.of(real));

        LookupTriggerPropertyOptionsToolCallback callback = new LookupTriggerPropertyOptionsToolCallback(
            service, mock(TriggerDefinitionFacade.class), resolver, mock(AiHubToolAttachMetrics.class), jsonMapper);

        String result = callback.call(
            "{\"componentName\":\"slack\",\"triggerName\":\"onMessage\",\"propertyName\":\"channel\"}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).isEqualTo("trigger_not_found");
        assertThat(node.get("valid")
            .get(0)
            .asText()).isEqualTo("newMessage");
    }

    @Test
    void testReturnsPropertyNotFoundWithValidNames() throws Exception {
        TriggerDefinitionService service = mock(TriggerDefinitionService.class);

        stubValidTrigger(service, "slack", 1, "newMessage", "channel", "text");

        LookupTriggerPropertyOptionsToolCallback callback = new LookupTriggerPropertyOptionsToolCallback(
            service, mock(TriggerDefinitionFacade.class), resolver, mock(AiHubToolAttachMetrics.class), jsonMapper);

        String result = callback.call(
            "{\"componentName\":\"slack\",\"triggerName\":\"newMessage\",\"propertyName\":\"channelId\"}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).isEqualTo("property_not_found");

        List<String> validNames = new ArrayList<>();

        node.get("valid")
            .forEach(name -> validNames.add(name.asText()));

        assertThat(validNames).contains("channel");
    }
```

- [ ] **Step 2: Run to verify new tests fail**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests 'com.bytechef.ee.ai.hub.tool.LookupTriggerPropertyOptionsToolCallbackTest'`
Expected: FAIL on the two new tests.

- [ ] **Step 3: Add the existence checks to the trigger callback**

In `LookupTriggerPropertyOptionsToolCallback.java`:

(a) Add imports:

```java
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.TriggerDefinition;
```

(b) Insert immediately after `int componentVersion = ...;` and before the `propertyHasOptionsDataSource` block:

```java
            List<String> validTriggerNames = triggerDefinitionService
                .getTriggerDefinitions(componentName, componentVersion)
                .stream()
                .map(TriggerDefinition::getName)
                .toList();

            if (!validTriggerNames.contains(triggerName)) {
                metrics.recordStateVisibility(TOOL_NAME, "trigger_not_found");

                return jsonMapper.writeValueAsString(
                    resolver.entityNotFoundEnvelope("trigger_not_found", "triggerName", triggerName, validTriggerNames));
            }

            List<String> validPropertyNames = triggerDefinitionService
                .getTriggerDefinition(componentName, componentVersion, triggerName)
                .getProperties()
                .stream()
                .map(Property::getName)
                .toList();

            if (!validPropertyNames.contains(topPropertySegment(propertyName))) {
                metrics.recordStateVisibility(TOOL_NAME, "property_not_found");

                return jsonMapper.writeValueAsString(
                    resolver.entityNotFoundEnvelope(
                        "property_not_found", "propertyName", propertyName, validPropertyNames));
            }
```

(c) Add the identical `topPropertySegment` private static helper as in Task 2 Step 3(c) (next to `toolError`):

```java
    /**
     * Returns the top-level container segment of a (possibly dotted / array) property path: {@code parent.child} →
     * {@code parent}, {@code items[].id} → {@code items}, {@code channel} → {@code channel}. Existence is checked only
     * at the top level; deeper resolution stays with the options engine.
     */
    private static String topPropertySegment(String propertyName) {
        int dot = propertyName.indexOf('.');
        int bracket = propertyName.indexOf('[');

        int cut;

        if (dot >= 0 && bracket >= 0) {
            cut = Math.min(dot, bracket);
        } else if (dot >= 0) {
            cut = dot;
        } else {
            cut = bracket;
        }

        return cut >= 0 ? propertyName.substring(0, cut) : propertyName;
    }
```

- [ ] **Step 4: Run to verify all pass**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests 'com.bytechef.ee.ai.hub.tool.LookupTriggerPropertyOptionsToolCallbackTest'`
Expected: PASS (all tests).

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/LookupTriggerPropertyOptionsToolCallback.java \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/LookupTriggerPropertyOptionsToolCallbackTest.java
git commit -m "0_732 Distinguish trigger/property not-found from no-options in trigger lookup"
```

---

## Task 4: Prompt updates (pass-all, reuse, canonical names)

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_build.txt`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_ask.txt`

- [ ] **Step 1: Append lookup-usage guidance to the BUILD prompt**

In `prompt_ai_hub_build.txt`, locate the paragraph that mentions `lookupActionPropertyOptions` (added previously). Immediately after that paragraph, insert this new paragraph (preserve one blank line before and after):

```
When you call lookupActionPropertyOptions / lookupTriggerPropertyOptions: pass the
property's canonical name and the action/trigger name EXACTLY as they appear in the
component descriptor — never the human label (the property is "channel", not the "Channel
ID" label; the action is "sendChannelMessage", not "sendMessage"). If the tool returns
action_not_found, trigger_not_found, or property_not_found, you used a wrong name: retry
with one of the names listed in the "valid" array — do NOT tell the user the property has
no options. Only no_options_for_property (for the correct name) means there is no dynamic
list. When the tool returns options, put EVERY returned option into askUserQuestion (do
not show only a few), state that the list was fetched from their connection, and reuse
that result — never re-query the same property with different names or contradict a list
you already fetched.
```

- [ ] **Step 2: Append the equivalent to the ASK prompt**

In `prompt_ai_hub_ask.txt`, after the paragraph mentioning `lookupActionPropertyOptions`, insert (preserve blank-line spacing):

```
When you call lookupActionPropertyOptions / lookupTriggerPropertyOptions: pass the
canonical property name and action/trigger name exactly as they appear in the descriptor,
never the human label. If the tool returns action_not_found / trigger_not_found /
property_not_found, retry with one of the names in the "valid" array instead of telling
the user there are no options. When options come back, present ALL of them in
askUserQuestion and reuse that result rather than re-querying or contradicting it.
```

- [ ] **Step 3: Verify**

Run:
```bash
grep -c "valid" server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_build.txt \
  server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_ask.txt
grep -c "EVERY returned option" server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_build.txt
```
Expected: both files report ≥1 for "valid"; build prompt reports 1 for "EVERY returned option".

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_build.txt \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_ask.txt
git commit -m "0_732 Tell AI Hub agent to use canonical names, self-correct, and show all options"
```

---

## Task 5: Client — searchable combobox for large single-select sets

**Files:**
- Modify: `client/src/pages/automation/ai-hub/messages/AiHubAskUserQuestionMessage.tsx`
- Test: `client/src/pages/automation/ai-hub/messages/tests/AiHubAskUserQuestionMessage.test.tsx`

- [ ] **Step 1: Write the failing test**

In `AiHubAskUserQuestionMessage.test.tsx`, add a data fixture near the others (with 9 options, above the threshold of 8):

```typescript
const LARGE_SINGLE_SELECT_DATA = {
    awaitingAnswer: true,
    kind: 'ask-user-question' as const,
    questions: [
        {
            header: 'Channel',
            multiSelect: false,
            options: [
                {label: 'general'},
                {label: 'random'},
                {label: 'testing'},
                {label: 'sales-team'},
                {label: 'tech-team'},
                {label: 'testing-again'},
                {label: 'intercapital-test'},
                {label: 'pevex-test'},
                {label: 'pto-test'},
            ],
            question: 'Which Slack channel?',
        },
    ],
};
```

Add this test inside the `describe` block:

```typescript
    it('renders a searchable combobox (not stacked buttons) when single-select options exceed the threshold', async () => {
        await renderMessage(LARGE_SINGLE_SELECT_DATA);

        // The options are not all rendered as top-level buttons at rest.
        expect(screen.queryByRole('button', {name: 'pto-test'})).not.toBeInTheDocument();

        // The combobox trigger is present; opening it reveals the options and selecting one submits the label.
        await userEvent.click(screen.getByRole('combobox'));

        await userEvent.click(screen.getByRole('option', {name: 'tech-team'}));

        expect(appendCalls).toHaveLength(1);
        expect(JSON.stringify(appendCalls[0])).toContain('User picked: tech-team');
    });
```

Note: if `getByRole('combobox')` does not match the `ComboBox` trigger, fall back to `screen.getByRole('button')` for the single trigger button — verify against the rendered DOM during implementation and use whichever role the `ComboBox` trigger exposes.

- [ ] **Step 2: Run to verify it fails**

Run (from `client/`): `npm run test -- src/pages/automation/ai-hub/messages/tests/AiHubAskUserQuestionMessage.test.tsx`
Expected: FAIL — with 9 options the current code renders 9 buttons, so `pto-test` button IS present.

- [ ] **Step 3: Implement the combobox branch**

In `AiHubAskUserQuestionMessage.tsx`:

(a) Add imports (keep alphabetical order within the import block):

```typescript
import ComboBox from '@/components/ComboBox/ComboBox';
```

(b) Add a module-level constant near `OTHER_OPTION_LABEL`:

```typescript
/** Above this many options, single/multi select render a searchable control instead of a stacked list. */
export const COMBOBOX_OPTION_THRESHOLD = 8;
```

(c) In `SingleSelectStep`, replace the final `return (` block (the one rendering the stacked option `<Button>`s and the injected `Other…` button) with a branch that uses `ComboBox` above the threshold:

```typescript
    if (question.options.length > COMBOBOX_OPTION_THRESHOLD) {
        const comboBoxItems = question.options.map((option) => ({label: option.label, value: option.label}));

        if (!llmSuppliedOther) {
            comboBoxItems.push({label: 'Other…', value: OTHER_OPTION_LABEL});
        }

        return (
            <ComboBox
                emptyMessage="No match"
                items={comboBoxItems}
                onChange={(item) => {
                    if (item) {
                        handleClick(item.value as string);
                    }
                }}
                value={undefined}
            />
        );
    }

    return (
        <div className="flex flex-col items-start gap-2">
            {question.options.map((option) => (
                <Button
                    key={option.label}
                    label={option.label}
                    onClick={() => handleClick(option.label)}
                    title={option.description}
                    variant="outline"
                />
            ))}

            {!llmSuppliedOther && (
                <Button
                    label="Other…"
                    onClick={() => setOtherTyping(true)}
                    title="Type a free-form answer if none of the listed options fit"
                    variant="outline"
                />
            )}
        </div>
    );
```

(The `otherTyping` free-form `<Input>` branch above this stays unchanged; selecting the `Other…` combobox item routes through `handleClick` → `OTHER_OPTION_LABEL` → `setOtherTyping(true)`, identical to the button path.)

- [ ] **Step 4: Run to verify it passes**

Run (from `client/`): `npm run test -- src/pages/automation/ai-hub/messages/tests/AiHubAskUserQuestionMessage.test.tsx`
Expected: PASS (the existing button-based tests still pass because they use ≤8 options; the new combobox test passes). If the combobox role assertion fails, adjust the selector per the implementation note in Step 1 and re-run.

- [ ] **Step 5: Commit**

```bash
git add client/src/pages/automation/ai-hub/messages/AiHubAskUserQuestionMessage.tsx \
        client/src/pages/automation/ai-hub/messages/tests/AiHubAskUserQuestionMessage.test.tsx
git commit -m "5169 client - Render searchable combobox for large askUserQuestion option sets"
```

---

## Task 6: Client — MultiSelect for large multi-select sets

**Files:**
- Modify: `client/src/pages/automation/ai-hub/messages/AiHubAskUserQuestionMessage.tsx`
- Test: `client/src/pages/automation/ai-hub/messages/tests/AiHubAskUserQuestionMessage.test.tsx`

- [ ] **Step 1: Write the failing test**

Add a fixture with 9 multi-select options:

```typescript
const LARGE_MULTI_SELECT_DATA = {
    awaitingAnswer: true,
    kind: 'ask-user-question' as const,
    questions: [
        {
            header: 'Channels',
            multiSelect: true,
            options: [
                {label: 'general'},
                {label: 'random'},
                {label: 'testing'},
                {label: 'sales-team'},
                {label: 'tech-team'},
                {label: 'testing-again'},
                {label: 'intercapital-test'},
                {label: 'pevex-test'},
                {label: 'pto-test'},
            ],
            question: 'Which channels?',
        },
    ],
};
```

Add this test:

```typescript
    it('renders a MultiSelect (not stacked checkboxes) when multi-select options exceed the threshold', async () => {
        await renderMessage(LARGE_MULTI_SELECT_DATA);

        // At rest the individual checkbox labels are not all rendered; the MultiSelect trigger shows a placeholder.
        expect(screen.queryByRole('checkbox', {name: 'pto-test'})).not.toBeInTheDocument();
        expect(screen.getByText(/select/i)).toBeInTheDocument();
    });
```

Note: confirm the `MultiSelect` default placeholder text (`'Select...'`) during implementation; adjust the `getByText` matcher if the rendered placeholder differs.

- [ ] **Step 2: Run to verify it fails**

Run (from `client/`): `npm run test -- src/pages/automation/ai-hub/messages/tests/AiHubAskUserQuestionMessage.test.tsx`
Expected: FAIL — current code renders 9 checkboxes, so `pto-test` checkbox IS present.

- [ ] **Step 3: Implement the MultiSelect branch**

In `AiHubAskUserQuestionMessage.tsx`:

(a) Add import (alphabetical within the block):

```typescript
import {MultiSelect} from '@/components/MultiSelect/MultiSelect';
```

(b) In `MultiSelectStep`, wrap the existing checkbox list `return` with a threshold branch. Replace the `return (` block with:

```typescript
    if (question.options.length > COMBOBOX_OPTION_THRESHOLD) {
        const multiSelectOptions = question.options.map((option) => ({label: option.label, value: option.label}));

        return (
            <div className="flex flex-col gap-2">
                <MultiSelect
                    onValueChange={(values) => setSelectedLabels(new Set(values))}
                    options={multiSelectOptions}
                    value={Array.from(selectedLabels)}
                />

                <div className="flex justify-end">
                    <Button
                        disabled={selectedLabels.size === 0}
                        label={isLastStep ? 'Submit all' : 'Next'}
                        onClick={handleSubmit}
                    />
                </div>
            </div>
        );
    }

    return (
        <div className="flex flex-col gap-2">
            {question.options.map((option) => (
                <label className="flex cursor-pointer items-start gap-2 text-sm hover:bg-muted/50" key={option.label}>
                    <input
                        checked={selectedLabels.has(option.label)}
                        className="mt-0.5"
                        onChange={() => toggle(option.label)}
                        type="checkbox"
                    />

                    <span className="flex flex-col">
                        <span className="font-medium">{option.label}</span>

                        {option.description && (
                            <span className="text-xs text-muted-foreground">{option.description}</span>
                        )}
                    </span>
                </label>
            ))}

            <div className="flex justify-end">
                <Button
                    disabled={selectedLabels.size === 0}
                    label={isLastStep ? 'Submit all' : 'Next'}
                    onClick={handleSubmit}
                />
            </div>
        </div>
    );
```

- [ ] **Step 4: Run to verify it passes**

Run (from `client/`): `npm run test -- src/pages/automation/ai-hub/messages/tests/AiHubAskUserQuestionMessage.test.tsx`
Expected: PASS (existing ≤8 multi-select test unchanged; new large multi-select test passes).

- [ ] **Step 5: Commit**

```bash
git add client/src/pages/automation/ai-hub/messages/AiHubAskUserQuestionMessage.tsx \
        client/src/pages/automation/ai-hub/messages/tests/AiHubAskUserQuestionMessage.test.tsx
git commit -m "5169 client - Render MultiSelect for large askUserQuestion multi-select sets"
```

---

## Task 7: Verification

- [ ] **Step 1: Server — spotless + full module check**

Run: `./gradlew spotlessApply :server:ee:libs:ai:ai-hub:ai-hub-service:check`
Expected: BUILD SUCCESSFUL. If SpotBugs flags `EI_EXPOSE` on any new code, follow the existing defensive-copy / `@SuppressFBWarnings` precedent in the module. If Spotless reformats, re-stage and amend the relevant commit.

- [ ] **Step 2: Client — full check**

Run (from `client/`): `npm run check`
Expected: lint + typecheck + tests pass. Fix any sort-keys / interface-naming / import-order violations per CLAUDE.md (ESLint `--fix` does not auto-fix sort-keys).

- [ ] **Step 3: Final commit (only if formatting changed files)**

```bash
git add -u server/ee/libs/ai/ai-hub/ai-hub-service client
git commit -m "0_732 Apply formatting for property-options UX fixes"
```

---

## Self-Review

**Spec coverage:**
- Fix 1 (prompt: pass-all, reuse, canonical names) → Task 4. ✓
- Fix 2 (tool: action/trigger/property not-found with valid names) → Tasks 1–3. ✓
- Fix 3 (client: combobox for single-select, MultiSelect for multi, threshold 8) → Tasks 5–6. ✓
- Tests: resolver helper (Task 1), both callbacks incl. wrong-name cases (Tasks 2–3), client combobox + multiselect (Tasks 5–6), prompt grep (Task 4). ✓
- EE header/`@version ee`: existing files retain them; no new files created server-side. ✓

**Placeholder scan:** No TBD/TODO; every code step shows complete code. Two client selector notes (combobox role, MultiSelect placeholder) are explicit "verify against DOM and adjust" instructions with a concrete default, not placeholders. ✓

**Type consistency:** `entityNotFoundEnvelope(String errorCode, String entityKey, String requested, List<String> valid)` defined in Task 1 and called identically in Tasks 2–3. `topPropertySegment` identical in both callbacks. `COMBOBOX_OPTION_THRESHOLD` defined in Task 5, reused in Task 6. `ComboBoxItemType.value`/`MultiSelect` `options`/`onValueChange` match the verified component props. ✓
