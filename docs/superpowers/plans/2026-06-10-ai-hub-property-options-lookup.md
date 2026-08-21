# AI Hub property-options lookup tools — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the AI Hub agent two tools — `lookupActionPropertyOptions` and `lookupTriggerPropertyOptions` — that fetch the real dynamic options for a component property (e.g. Slack channels) so the agent renders them as `askUserQuestion` choices instead of hallucinating them.

**Architecture:** Two Spring-AI `ToolCallback`s in `ai-hub-service`, modeled on `ListConnectionsForComponentToolCallback`. They gate on the already-built (but currently dead) `ActionDefinitionService` / `TriggerDefinitionService` methods (`propertyHasOptionsDataSource`, `getPropertyLookupDependsOn`, `actionDefinesConnection` / `triggerDefinesConnection`), assemble response envelopes via the already-built (but dead) `PropertyOptionsResolver` helpers, and call the existing `ActionDefinitionFacade` / `TriggerDefinitionFacade` `executeOptions(...)` engine (the same one the workflow editor's option dropdowns use). They register in both the ASK and BUILD tool catalogs via the shared `registerToolAttachStateVisibilityToolCallbacks` helper. The build/ask prompts are updated to tell the agent to call these tools whenever a property descriptor shows `"lookupRequired": true`.

**Tech Stack:** Java 25, Spring Boot 4, Spring AI, Jackson 3 (`tools.jackson`), JUnit 5 + Mockito + AssertJ. EE module (ByteChef Enterprise license header + `@version ee` Javadoc on every new file).

---

## Background facts (verified against the codebase)

- The agent already learns a property needs a lookup: `ToolUtils.appendLookupMetadata` emits `"lookupRequired": true, "lookupDependsOn": [...]` into the component action/trigger catalog tool descriptor (`ComponentTools.generateParametersJson`). The missing piece is a tool to *perform* the lookup.
- Gate methods live on the **Service** interfaces; the options engine lives on the **Facade** interfaces:
  - `ActionDefinitionService`: `boolean propertyHasOptionsDataSource(String componentName, int componentVersion, String actionName, String propertyName)`, `List<String> getPropertyLookupDependsOn(...same args...)`, `boolean actionDefinesConnection(String componentName, int componentVersion, String actionName)`.
  - `TriggerDefinitionService`: same two property methods, plus `boolean triggerDefinesConnection(String componentName, int componentVersion, String triggerName)`.
  - `ActionDefinitionFacade.executeOptions(String componentName, int componentVersion, String actionName, String propertyName, Map<String,?> inputParameters, List<String> lookupDependsOnPaths, String searchText, @Nullable Long connectionId)` → `List<Option>`.
  - `TriggerDefinitionFacade.executeOptions(...same shape with triggerName...)` → `List<Option>`.
- `PropertyOptionsResolver` (`com.bytechef.ee.ai.hub.tool`) already provides `withUserSecurityContext`, `buildSuccessEnvelope`, `connectionRequiredEnvelope`, `dependencyMissingEnvelope`, `noOptionsForPropertyEnvelope`. Only `withUserSecurityContext` is currently used.
- `AiHubToolInvocationContext` (`com.bytechef.ee.ai.hub.tool`, in `ai-hub-api`): record `(Long workspaceId, Long userId, Short sourceOrdinal, String lastUserPrompt, Long environmentId, String threadId)`; helpers `fromToolContext(ToolContext)`, `resolveEnvironmentOrDefault(ctx)`, `toToolContext()`.
- `ToolErrors` (`com.bytechef.ee.ai.agent.tool`, in `ai-api`): `toolError(JsonMapper, String)`, `runtimeFailure(JsonMapper, Class<?>, String, RuntimeException)`.
- `Option` (`com.bytechef.platform.component.domain.Option`): `getLabel()`, `getValue()` (inherited from `BaseOption`); mock it in tests.
- `platform-component-api` is already a dependency of `ai-hub-service` (gives both facade + service interfaces) — no gradle change needed.

## File Structure

- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/PropertyOptionsResolver.java` — add `boolean truncated` param to `buildSuccessEnvelope`.
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/LookupActionPropertyOptionsToolCallback.java`
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/LookupTriggerPropertyOptionsToolCallback.java`
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/LookupActionPropertyOptionsToolCallbackTest.java`
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/LookupTriggerPropertyOptionsToolCallbackTest.java`
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/PropertyOptionsResolverTest.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubConfiguration.java` — thread the facades/services through the two bean methods + the shared helper.
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/config/PropertyOptionsToolWiringTest.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_build.txt`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_ask.txt`

A note on running module tests:

```bash
./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '<fully.qualified.TestClass>'
```

---

## Task 1: Add `truncated` flag to `PropertyOptionsResolver.buildSuccessEnvelope`

**Files:**
- Test: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/PropertyOptionsResolverTest.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/PropertyOptionsResolver.java`

- [ ] **Step 1: Write the failing test**

Create `PropertyOptionsResolverTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PropertyOptionsResolverTest {

    private final PropertyOptionsResolver resolver =
        new PropertyOptionsResolver(mock(UserService.class), mock(AuthorityService.class));

    @Test
    void testBuildSuccessEnvelopeCarriesOptionsAndTruncatedFlag() {
        Option option = mock(Option.class);

        when(option.getLabel()).thenReturn("#general");
        when(option.getValue()).thenReturn("C123");

        Map<String, Object> envelope =
            resolver.buildSuccessEnvelope("slack", "actionName", "sendMessage", "channel", List.of(option), true);

        assertThat(envelope.get("componentName")).isEqualTo("slack");
        assertThat(envelope.get("actionName")).isEqualTo("sendMessage");
        assertThat(envelope.get("propertyName")).isEqualTo("channel");
        assertThat(envelope.get("truncated")).isEqualTo(true);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) envelope.get("options");

        assertThat(options).hasSize(1);
        assertThat(options.get(0)
            .get("label")).isEqualTo("#general");
        assertThat(options.get(0)
            .get("value")).isEqualTo("C123");
    }

    @Test
    void testDependencyMissingEnvelope() {
        Map<String, Object> envelope = resolver.dependencyMissingEnvelope(List.of("spreadsheetId"));

        assertThat(envelope.get("error")).isEqualTo("dependency_missing");
        assertThat(envelope.get("missing")).isEqualTo(List.of("spreadsheetId"));
    }

    @Test
    void testNoOptionsForPropertyEnvelope() {
        assertThat(resolver.noOptionsForPropertyEnvelope()
            .get("error")).isEqualTo("no_options_for_property");
    }

    @Test
    void testConnectionRequiredEnvelope() {
        Map<String, Object> envelope = resolver.connectionRequiredEnvelope("slack");

        assertThat(envelope.get("error")).isEqualTo("connection_required");
        assertThat(envelope.get("componentName")).isEqualTo("slack");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests 'com.bytechef.ee.ai.hub.tool.PropertyOptionsResolverTest'`
Expected: COMPILE FAILURE — `buildSuccessEnvelope` has no 6-arg (boolean) overload.

- [ ] **Step 3: Add the `truncated` parameter**

In `PropertyOptionsResolver.java`, change the `buildSuccessEnvelope` signature and body. Replace:

```java
    public Map<String, Object> buildSuccessEnvelope(
        String componentName, String entityKey, String entityName, String propertyName, List<Option> options) {
```

with:

```java
    public Map<String, Object> buildSuccessEnvelope(
        String componentName, String entityKey, String entityName, String propertyName, List<Option> options,
        boolean truncated) {
```

and, in the same method, after the `envelope.put("options", optionRows);` line, add:

```java
        envelope.put("truncated", truncated);
```

Also update the method Javadoc's first sentence to mention the `truncated` flag (one short clause).

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests 'com.bytechef.ee.ai.hub.tool.PropertyOptionsResolverTest'`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/PropertyOptionsResolver.java \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/PropertyOptionsResolverTest.java
git commit -m "0_732 Add truncated flag to PropertyOptionsResolver success envelope"
```

---

## Task 2: Create `LookupActionPropertyOptionsToolCallback`

**Files:**
- Test: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/LookupActionPropertyOptionsToolCallbackTest.java`
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/LookupActionPropertyOptionsToolCallback.java`

- [ ] **Step 1: Write the failing test**

Create `LookupActionPropertyOptionsToolCallbackTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.metric.AiHubToolAttachMetrics;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class LookupActionPropertyOptionsToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    private final PropertyOptionsResolver resolver =
        new PropertyOptionsResolver(mock(UserService.class), mock(AuthorityService.class));

    private ToolContext toolContext() {
        return new ToolContext(
            new AiHubToolInvocationContext(1L, null, (short) 0, "x", 0L, "thread-1").toToolContext());
    }

    @Test
    void testReturnsNoOptionsWhenPropertyHasNoDataSource() throws Exception {
        ActionDefinitionService service = mock(ActionDefinitionService.class);

        when(service.propertyHasOptionsDataSource("slack", 1, "sendMessage", "text")).thenReturn(false);

        LookupActionPropertyOptionsToolCallback callback = new LookupActionPropertyOptionsToolCallback(
            service, mock(ActionDefinitionFacade.class), resolver, mock(AiHubToolAttachMetrics.class), jsonMapper);

        String result = callback.call(
            "{\"componentName\":\"slack\",\"actionName\":\"sendMessage\",\"propertyName\":\"text\"}", toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).isEqualTo("no_options_for_property");
    }

    @Test
    void testReturnsDependencyMissingWhenSiblingAbsent() throws Exception {
        ActionDefinitionService service = mock(ActionDefinitionService.class);

        when(service.propertyHasOptionsDataSource("googleSheets", 1, "appendRow", "sheetName")).thenReturn(true);
        when(service.getPropertyLookupDependsOn("googleSheets", 1, "appendRow", "sheetName"))
            .thenReturn(List.of("spreadsheetId"));

        LookupActionPropertyOptionsToolCallback callback = new LookupActionPropertyOptionsToolCallback(
            service, mock(ActionDefinitionFacade.class), resolver, mock(AiHubToolAttachMetrics.class), jsonMapper);

        String result = callback.call(
            "{\"componentName\":\"googleSheets\",\"actionName\":\"appendRow\",\"propertyName\":\"sheetName\"}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).isEqualTo("dependency_missing");
        assertThat(node.get("missing")
            .get(0)
            .asText()).isEqualTo("spreadsheetId");
    }

    @Test
    void testReturnsConnectionRequiredWhenConnectionMissing() throws Exception {
        ActionDefinitionService service = mock(ActionDefinitionService.class);

        when(service.propertyHasOptionsDataSource("slack", 1, "sendMessage", "channel")).thenReturn(true);
        when(service.getPropertyLookupDependsOn("slack", 1, "sendMessage", "channel")).thenReturn(List.of());
        when(service.actionDefinesConnection("slack", 1, "sendMessage")).thenReturn(true);

        LookupActionPropertyOptionsToolCallback callback = new LookupActionPropertyOptionsToolCallback(
            service, mock(ActionDefinitionFacade.class), resolver, mock(AiHubToolAttachMetrics.class), jsonMapper);

        String result = callback.call(
            "{\"componentName\":\"slack\",\"actionName\":\"sendMessage\",\"propertyName\":\"channel\"}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).isEqualTo("connection_required");
    }

    @Test
    void testReturnsCappedOptionsAndTruncatedFlagOnSuccess() throws Exception {
        ActionDefinitionService service = mock(ActionDefinitionService.class);
        ActionDefinitionFacade facade = mock(ActionDefinitionFacade.class);

        when(service.propertyHasOptionsDataSource("slack", 1, "sendMessage", "channel")).thenReturn(true);
        when(service.getPropertyLookupDependsOn("slack", 1, "sendMessage", "channel")).thenReturn(List.of());
        when(service.actionDefinesConnection("slack", 1, "sendMessage")).thenReturn(true);

        List<Option> options = new ArrayList<>();

        for (int i = 0; i < 30; i++) {
            Option option = mock(Option.class);

            when(option.getLabel()).thenReturn("#channel" + i);
            when(option.getValue()).thenReturn("C" + i);

            options.add(option);
        }

        when(facade.executeOptions(
            eq("slack"), anyInt(), eq("sendMessage"), eq("channel"), any(), any(), any(), eq(42L)))
                .thenReturn(options);

        LookupActionPropertyOptionsToolCallback callback = new LookupActionPropertyOptionsToolCallback(
            service, facade, resolver, mock(AiHubToolAttachMetrics.class), jsonMapper);

        String result = callback.call(
            "{\"componentName\":\"slack\",\"actionName\":\"sendMessage\",\"propertyName\":\"channel\","
                + "\"connectionId\":42}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("componentName")
            .asText()).isEqualTo("slack");
        assertThat(node.get("actionName")
            .asText()).isEqualTo("sendMessage");
        assertThat(node.get("options")
            .size()).isEqualTo(25);
        assertThat(node.get("truncated")
            .asBoolean()).isTrue();
        assertThat(node.get("options")
            .get(0)
            .get("value")
            .asText()).isEqualTo("C0");
    }

    @Test
    void testRejectsBlankPropertyName() throws Exception {
        LookupActionPropertyOptionsToolCallback callback = new LookupActionPropertyOptionsToolCallback(
            mock(ActionDefinitionService.class), mock(ActionDefinitionFacade.class), resolver,
            mock(AiHubToolAttachMetrics.class), jsonMapper);

        String result = callback.call(
            "{\"componentName\":\"slack\",\"actionName\":\"sendMessage\",\"propertyName\":\"\"}", toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).contains("propertyName is required");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests 'com.bytechef.ee.ai.hub.tool.LookupActionPropertyOptionsToolCallbackTest'`
Expected: COMPILE FAILURE — `LookupActionPropertyOptionsToolCallback` does not exist.

- [ ] **Step 3: Create the callback**

Create `LookupActionPropertyOptionsToolCallback.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ee.ai.agent.tool.ToolErrors;
import com.bytechef.ee.ai.hub.metric.AiHubToolAttachMetrics;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that fetches the real, dynamic options for a component action's property (e.g. the
 * channels available on a Slack connection). The agent learns a property needs a lookup from the
 * {@code "lookupRequired": true} marker that {@code ToolUtils.appendLookupMetadata} writes into the action's catalog
 * descriptor; this tool is what it calls to resolve them. Returned options are meant to be surfaced through
 * {@code askUserQuestion} so the user never has to type a raw id and the agent never has to guess.
 *
 * <p>
 * The call gates in order: a property with no dynamic options yields {@code no_options_for_property}; an unsatisfied
 * {@code lookupDependsOn} sibling yields {@code dependency_missing}; a connection-bearing component with no
 * {@code connectionId} yields {@code connection_required}. On success the option list is capped at {@value #MAX_OPTIONS}
 * with a {@code truncated} flag so the chat client never tries to render hundreds of buttons.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class LookupActionPropertyOptionsToolCallback implements ToolCallback {

    static final String TOOL_NAME = "lookupActionPropertyOptions";

    private static final int MAX_OPTIONS = 25;

    private static final Logger log = LoggerFactory.getLogger(LookupActionPropertyOptionsToolCallback.class);

    private static final String DESCRIPTION = """
        Fetch the real selectable options for a component ACTION property whose descriptor shows
        "lookupRequired": true (e.g. a Slack channel, a Google Sheets sheet). Call this before asking the user, then
        render the returned options via askUserQuestion. First satisfy any "lookupDependsOn" siblings by placing their
        values in inputParameters, and pass connectionId when the component needs a connection. Returns componentName,
        actionName, propertyName, an options array of {label, value}, and a truncated flag (true when the list was
        capped). Error envelopes: no_options_for_property, dependency_missing, connection_required.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "componentName": {"type": "string"},
                "componentVersion": {"type": "integer", "description": "Defaults to 1"},
                "actionName": {"type": "string"},
                "propertyName": {"type": "string", "description": "Dotted paths supported: parent.child, items[].id"},
                "inputParameters": {"type": "object", "description": "Sibling values required by lookupDependsOn"},
                "connectionId": {"type": "integer"},
                "searchText": {"type": "string", "description": "Optional filter passed to the options provider"}
            },
            "required": ["componentName", "actionName", "propertyName"]
        }""";

    private final ActionDefinitionService actionDefinitionService;
    private final ActionDefinitionFacade actionDefinitionFacade;
    private final PropertyOptionsResolver resolver;
    private final AiHubToolAttachMetrics metrics;
    private final JsonMapper jsonMapper;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public LookupActionPropertyOptionsToolCallback(
        ActionDefinitionService actionDefinitionService, ActionDefinitionFacade actionDefinitionFacade,
        PropertyOptionsResolver resolver, AiHubToolAttachMetrics metrics, JsonMapper jsonMapper) {

        this.actionDefinitionService = actionDefinitionService;
        this.actionDefinitionFacade = actionDefinitionFacade;
        this.resolver = resolver;
        this.metrics = metrics;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(TOOL_NAME)
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            LookupActionPropertyOptionsInput input =
                jsonMapper.readValue(toolInput, LookupActionPropertyOptionsInput.class);

            String componentName = input.componentName();
            String actionName = input.actionName();
            String propertyName = input.propertyName();

            if (componentName == null || componentName.isBlank()) {
                return toolError("componentName is required and must not be blank");
            }

            if (actionName == null || actionName.isBlank()) {
                return toolError("actionName is required and must not be blank");
            }

            if (propertyName == null || propertyName.isBlank()) {
                return toolError("propertyName is required and must not be blank");
            }

            AiHubToolInvocationContext invocationContext = AiHubToolInvocationContext.fromToolContext(toolContext);

            if (invocationContext == null || invocationContext.workspaceId() == null) {
                return toolError("Workspace context unavailable — open this chat from the AI Hub.");
            }

            int componentVersion = input.componentVersion() == null ? 1 : input.componentVersion();

            if (!actionDefinitionService.propertyHasOptionsDataSource(
                componentName, componentVersion, actionName, propertyName)) {

                metrics.recordStateVisibility(TOOL_NAME, "no_options");

                return jsonMapper.writeValueAsString(resolver.noOptionsForPropertyEnvelope());
            }

            List<String> lookupDependsOnPaths = actionDefinitionService.getPropertyLookupDependsOn(
                componentName, componentVersion, actionName, propertyName);

            Map<String, Object> inputParameters =
                input.inputParameters() == null ? Map.of() : input.inputParameters();

            List<String> missing = lookupDependsOnPaths.stream()
                .filter(path -> !inputParameters.containsKey(path))
                .toList();

            if (!missing.isEmpty()) {
                metrics.recordStateVisibility(TOOL_NAME, "dependency_missing");

                return jsonMapper.writeValueAsString(resolver.dependencyMissingEnvelope(missing));
            }

            Long connectionId = input.connectionId();

            if (connectionId == null
                && actionDefinitionService.actionDefinesConnection(componentName, componentVersion, actionName)) {

                metrics.recordStateVisibility(TOOL_NAME, "connection_required");

                return jsonMapper.writeValueAsString(resolver.connectionRequiredEnvelope(componentName));
            }

            int resolvedVersion = componentVersion;

            List<Option> options = resolver.withUserSecurityContext(
                invocationContext.userId(),
                () -> actionDefinitionFacade.executeOptions(
                    componentName, resolvedVersion, actionName, propertyName, inputParameters, lookupDependsOnPaths,
                    input.searchText(), connectionId));

            boolean truncated = options.size() > MAX_OPTIONS;

            List<Option> capped = truncated ? options.subList(0, MAX_OPTIONS) : options;

            metrics.recordStateVisibility(TOOL_NAME, options.isEmpty() ? "empty" : "success");

            return jsonMapper.writeValueAsString(
                resolver.buildSuccessEnvelope(componentName, "actionName", actionName, propertyName, capped,
                    truncated));
        } catch (JacksonException exception) {
            log.warn(
                "lookupActionPropertyOptions rejected malformed tool input: {} — first 200 chars: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            metrics.recordStateVisibility(TOOL_NAME, "error");

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            metrics.recordStateVisibility(TOOL_NAME, "error");

            return ToolErrors.runtimeFailure(
                jsonMapper, LookupActionPropertyOptionsToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record LookupActionPropertyOptionsInput(
        String componentName, @Nullable Integer componentVersion, String actionName, String propertyName,
        @Nullable Map<String, Object> inputParameters, @Nullable Long connectionId, @Nullable String searchText) {
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests 'com.bytechef.ee.ai.hub.tool.LookupActionPropertyOptionsToolCallbackTest'`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/LookupActionPropertyOptionsToolCallback.java \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/LookupActionPropertyOptionsToolCallbackTest.java
git commit -m "0_732 Add lookupActionPropertyOptions AI Hub tool callback"
```

---

## Task 3: Create `LookupTriggerPropertyOptionsToolCallback`

**Files:**
- Test: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/LookupTriggerPropertyOptionsToolCallbackTest.java`
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/LookupTriggerPropertyOptionsToolCallback.java`

This mirrors Task 2 with: `TriggerDefinitionService` + `TriggerDefinitionFacade`, `triggerName` instead of `actionName`, `triggerDefinesConnection` instead of `actionDefinesConnection`, tool name `lookupTriggerPropertyOptions`, and envelope `entityKey` of `"triggerName"`.

- [ ] **Step 1: Write the failing test**

Create `LookupTriggerPropertyOptionsToolCallbackTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.metric.AiHubToolAttachMetrics;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class LookupTriggerPropertyOptionsToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    private final PropertyOptionsResolver resolver =
        new PropertyOptionsResolver(mock(UserService.class), mock(AuthorityService.class));

    private ToolContext toolContext() {
        return new ToolContext(
            new AiHubToolInvocationContext(1L, null, (short) 0, "x", 0L, "thread-1").toToolContext());
    }

    @Test
    void testReturnsNoOptionsWhenPropertyHasNoDataSource() throws Exception {
        TriggerDefinitionService service = mock(TriggerDefinitionService.class);

        when(service.propertyHasOptionsDataSource("slack", 1, "newMessage", "text")).thenReturn(false);

        LookupTriggerPropertyOptionsToolCallback callback = new LookupTriggerPropertyOptionsToolCallback(
            service, mock(TriggerDefinitionFacade.class), resolver, mock(AiHubToolAttachMetrics.class), jsonMapper);

        String result = callback.call(
            "{\"componentName\":\"slack\",\"triggerName\":\"newMessage\",\"propertyName\":\"text\"}", toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).isEqualTo("no_options_for_property");
    }

    @Test
    void testReturnsConnectionRequiredWhenConnectionMissing() throws Exception {
        TriggerDefinitionService service = mock(TriggerDefinitionService.class);

        when(service.propertyHasOptionsDataSource("slack", 1, "newMessage", "channel")).thenReturn(true);
        when(service.getPropertyLookupDependsOn("slack", 1, "newMessage", "channel")).thenReturn(List.of());
        when(service.triggerDefinesConnection("slack", 1, "newMessage")).thenReturn(true);

        LookupTriggerPropertyOptionsToolCallback callback = new LookupTriggerPropertyOptionsToolCallback(
            service, mock(TriggerDefinitionFacade.class), resolver, mock(AiHubToolAttachMetrics.class), jsonMapper);

        String result = callback.call(
            "{\"componentName\":\"slack\",\"triggerName\":\"newMessage\",\"propertyName\":\"channel\"}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).isEqualTo("connection_required");
    }

    @Test
    void testReturnsCappedOptionsAndTruncatedFlagOnSuccess() throws Exception {
        TriggerDefinitionService service = mock(TriggerDefinitionService.class);
        TriggerDefinitionFacade facade = mock(TriggerDefinitionFacade.class);

        when(service.propertyHasOptionsDataSource("slack", 1, "newMessage", "channel")).thenReturn(true);
        when(service.getPropertyLookupDependsOn("slack", 1, "newMessage", "channel")).thenReturn(List.of());
        when(service.triggerDefinesConnection("slack", 1, "newMessage")).thenReturn(true);

        List<Option> options = new ArrayList<>();

        for (int i = 0; i < 30; i++) {
            Option option = mock(Option.class);

            when(option.getLabel()).thenReturn("#channel" + i);
            when(option.getValue()).thenReturn("C" + i);

            options.add(option);
        }

        when(facade.executeOptions(
            eq("slack"), anyInt(), eq("newMessage"), eq("channel"), any(), any(), any(), eq(42L)))
                .thenReturn(options);

        LookupTriggerPropertyOptionsToolCallback callback = new LookupTriggerPropertyOptionsToolCallback(
            service, facade, resolver, mock(AiHubToolAttachMetrics.class), jsonMapper);

        String result = callback.call(
            "{\"componentName\":\"slack\",\"triggerName\":\"newMessage\",\"propertyName\":\"channel\","
                + "\"connectionId\":42}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("triggerName")
            .asText()).isEqualTo("newMessage");
        assertThat(node.get("options")
            .size()).isEqualTo(25);
        assertThat(node.get("truncated")
            .asBoolean()).isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests 'com.bytechef.ee.ai.hub.tool.LookupTriggerPropertyOptionsToolCallbackTest'`
Expected: COMPILE FAILURE — `LookupTriggerPropertyOptionsToolCallback` does not exist.

- [ ] **Step 3: Create the callback**

Create `LookupTriggerPropertyOptionsToolCallback.java` — identical structure to Task 2's callback with these substitutions: imports `TriggerDefinitionFacade` / `TriggerDefinitionService`; `TOOL_NAME = "lookupTriggerPropertyOptions"`; the `DESCRIPTION` and `INPUT_SCHEMA` use `triggerName` in place of `actionName` (schema `required` becomes `["componentName", "triggerName", "propertyName"]`); fields/constructor take `TriggerDefinitionService triggerDefinitionService, TriggerDefinitionFacade triggerDefinitionFacade`; the connection gate calls `triggerDefinitionService.triggerDefinesConnection(componentName, componentVersion, triggerName)`; the success envelope uses `entityKey = "triggerName"`; the input record is `LookupTriggerPropertyOptionsInput(String componentName, @Nullable Integer componentVersion, String triggerName, String propertyName, @Nullable Map<String, Object> inputParameters, @Nullable Long connectionId, @Nullable String searchText)`. Full source:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ee.ai.agent.tool.ToolErrors;
import com.bytechef.ee.ai.hub.metric.AiHubToolAttachMetrics;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that fetches the real, dynamic options for a component TRIGGER's property. Trigger
 * twin of {@link LookupActionPropertyOptionsToolCallback}; see that class for the gating contract and envelope shapes.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class LookupTriggerPropertyOptionsToolCallback implements ToolCallback {

    static final String TOOL_NAME = "lookupTriggerPropertyOptions";

    private static final int MAX_OPTIONS = 25;

    private static final Logger log = LoggerFactory.getLogger(LookupTriggerPropertyOptionsToolCallback.class);

    private static final String DESCRIPTION = """
        Fetch the real selectable options for a component TRIGGER property whose descriptor shows
        "lookupRequired": true. Call this before asking the user, then render the returned options via askUserQuestion.
        First satisfy any "lookupDependsOn" siblings by placing their values in inputParameters, and pass connectionId
        when the component needs a connection. Returns componentName, triggerName, propertyName, an options array of
        {label, value}, and a truncated flag (true when the list was capped). Error envelopes: no_options_for_property,
        dependency_missing, connection_required.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "componentName": {"type": "string"},
                "componentVersion": {"type": "integer", "description": "Defaults to 1"},
                "triggerName": {"type": "string"},
                "propertyName": {"type": "string", "description": "Dotted paths supported: parent.child, items[].id"},
                "inputParameters": {"type": "object", "description": "Sibling values required by lookupDependsOn"},
                "connectionId": {"type": "integer"},
                "searchText": {"type": "string", "description": "Optional filter passed to the options provider"}
            },
            "required": ["componentName", "triggerName", "propertyName"]
        }""";

    private final TriggerDefinitionService triggerDefinitionService;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final PropertyOptionsResolver resolver;
    private final AiHubToolAttachMetrics metrics;
    private final JsonMapper jsonMapper;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public LookupTriggerPropertyOptionsToolCallback(
        TriggerDefinitionService triggerDefinitionService, TriggerDefinitionFacade triggerDefinitionFacade,
        PropertyOptionsResolver resolver, AiHubToolAttachMetrics metrics, JsonMapper jsonMapper) {

        this.triggerDefinitionService = triggerDefinitionService;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.resolver = resolver;
        this.metrics = metrics;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(TOOL_NAME)
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            LookupTriggerPropertyOptionsInput input =
                jsonMapper.readValue(toolInput, LookupTriggerPropertyOptionsInput.class);

            String componentName = input.componentName();
            String triggerName = input.triggerName();
            String propertyName = input.propertyName();

            if (componentName == null || componentName.isBlank()) {
                return toolError("componentName is required and must not be blank");
            }

            if (triggerName == null || triggerName.isBlank()) {
                return toolError("triggerName is required and must not be blank");
            }

            if (propertyName == null || propertyName.isBlank()) {
                return toolError("propertyName is required and must not be blank");
            }

            AiHubToolInvocationContext invocationContext = AiHubToolInvocationContext.fromToolContext(toolContext);

            if (invocationContext == null || invocationContext.workspaceId() == null) {
                return toolError("Workspace context unavailable — open this chat from the AI Hub.");
            }

            int componentVersion = input.componentVersion() == null ? 1 : input.componentVersion();

            if (!triggerDefinitionService.propertyHasOptionsDataSource(
                componentName, componentVersion, triggerName, propertyName)) {

                metrics.recordStateVisibility(TOOL_NAME, "no_options");

                return jsonMapper.writeValueAsString(resolver.noOptionsForPropertyEnvelope());
            }

            List<String> lookupDependsOnPaths = triggerDefinitionService.getPropertyLookupDependsOn(
                componentName, componentVersion, triggerName, propertyName);

            Map<String, Object> inputParameters =
                input.inputParameters() == null ? Map.of() : input.inputParameters();

            List<String> missing = lookupDependsOnPaths.stream()
                .filter(path -> !inputParameters.containsKey(path))
                .toList();

            if (!missing.isEmpty()) {
                metrics.recordStateVisibility(TOOL_NAME, "dependency_missing");

                return jsonMapper.writeValueAsString(resolver.dependencyMissingEnvelope(missing));
            }

            Long connectionId = input.connectionId();

            if (connectionId == null
                && triggerDefinitionService.triggerDefinesConnection(componentName, componentVersion, triggerName)) {

                metrics.recordStateVisibility(TOOL_NAME, "connection_required");

                return jsonMapper.writeValueAsString(resolver.connectionRequiredEnvelope(componentName));
            }

            int resolvedVersion = componentVersion;

            List<Option> options = resolver.withUserSecurityContext(
                invocationContext.userId(),
                () -> triggerDefinitionFacade.executeOptions(
                    componentName, resolvedVersion, triggerName, propertyName, inputParameters, lookupDependsOnPaths,
                    input.searchText(), connectionId));

            boolean truncated = options.size() > MAX_OPTIONS;

            List<Option> capped = truncated ? options.subList(0, MAX_OPTIONS) : options;

            metrics.recordStateVisibility(TOOL_NAME, options.isEmpty() ? "empty" : "success");

            return jsonMapper.writeValueAsString(
                resolver.buildSuccessEnvelope(componentName, "triggerName", triggerName, propertyName, capped,
                    truncated));
        } catch (JacksonException exception) {
            log.warn(
                "lookupTriggerPropertyOptions rejected malformed tool input: {} — first 200 chars: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            metrics.recordStateVisibility(TOOL_NAME, "error");

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            metrics.recordStateVisibility(TOOL_NAME, "error");

            return ToolErrors.runtimeFailure(
                jsonMapper, LookupTriggerPropertyOptionsToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record LookupTriggerPropertyOptionsInput(
        String componentName, @Nullable Integer componentVersion, String triggerName, String propertyName,
        @Nullable Map<String, Object> inputParameters, @Nullable Long connectionId, @Nullable String searchText) {
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests 'com.bytechef.ee.ai.hub.tool.LookupTriggerPropertyOptionsToolCallbackTest'`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/LookupTriggerPropertyOptionsToolCallback.java \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/LookupTriggerPropertyOptionsToolCallbackTest.java
git commit -m "0_732 Add lookupTriggerPropertyOptions AI Hub tool callback"
```

---

## Task 4: Wire both tools into the ASK and BUILD catalogs

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubConfiguration.java`

Both the `aiHubAskSpringAIAgent` (ASK) and `aiHubBuildSpringAIAgent` (BUILD) bean methods call the shared private static helper `registerToolAttachStateVisibilityToolCallbacks`. We extend that helper to also add the two new callbacks, and thread the four new dependencies through both bean methods.

- [ ] **Step 1: Add imports**

Near the other `com.bytechef.platform.component.*` imports in `AiHubConfiguration.java`, add:

```java
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
```

(`com.bytechef.platform.component.service.TriggerDefinitionService` is already imported — do not duplicate it.)

Also add the two tool imports near the other `com.bytechef.ee.ai.hub.tool.*` imports:

```java
import com.bytechef.ee.ai.hub.tool.LookupActionPropertyOptionsToolCallback;
import com.bytechef.ee.ai.hub.tool.LookupTriggerPropertyOptionsToolCallback;
```

- [ ] **Step 2: Extend the shared helper**

Replace the whole `registerToolAttachStateVisibilityToolCallbacks` method body (currently at ~line 735) with:

```java
    private static void registerToolAttachStateVisibilityToolCallbacks(
        List<ToolCallback> toolCallbacks, AiHubTaskService taskService, AiHubTaskToolFacade taskToolFacade,
        ConnectionDefinitionService connectionDefinitionService, WorkspaceConnectionFacade workspaceConnectionFacade,
        ActionDefinitionService actionDefinitionService, ActionDefinitionFacade actionDefinitionFacade,
        TriggerDefinitionService triggerDefinitionService, TriggerDefinitionFacade triggerDefinitionFacade,
        PropertyOptionsResolver propertyOptionsResolver, AiHubToolAttachMetrics aiHubToolAttachMetrics,
        JsonMapper jsonMapper) {

        toolCallbacks.add(new ListTaskToolsToolCallback(taskService, taskToolFacade, aiHubToolAttachMetrics,
            jsonMapper));
        toolCallbacks.add(
            new ListConnectionsForComponentToolCallback(
                connectionDefinitionService, workspaceConnectionFacade, propertyOptionsResolver,
                aiHubToolAttachMetrics, jsonMapper));
        toolCallbacks.add(
            new LookupActionPropertyOptionsToolCallback(
                actionDefinitionService, actionDefinitionFacade, propertyOptionsResolver, aiHubToolAttachMetrics,
                jsonMapper));
        toolCallbacks.add(
            new LookupTriggerPropertyOptionsToolCallback(
                triggerDefinitionService, triggerDefinitionFacade, propertyOptionsResolver, aiHubToolAttachMetrics,
                jsonMapper));
    }
```

- [ ] **Step 3: Inject the new beans into the ASK bean method and pass them to the helper**

In `aiHubAskSpringAIAgent(...)` (signature starts ~line 212), add these parameters to the method signature (place them next to the existing `connectionDefinitionService` / `workspaceConnectionFacade` params for readability):

```java
        ActionDefinitionService actionDefinitionService,
        ActionDefinitionFacade actionDefinitionFacade,
        TriggerDefinitionService triggerDefinitionService,
        TriggerDefinitionFacade triggerDefinitionFacade,
```

Then update the ASK-mode call site (~line 281) to pass them through:

```java
        registerToolAttachStateVisibilityToolCallbacks(
            toolCallbacks, taskService, taskToolFacade, connectionDefinitionService, workspaceConnectionFacade,
            actionDefinitionService, actionDefinitionFacade, triggerDefinitionService, triggerDefinitionFacade,
            propertyOptionsResolver, aiHubToolAttachMetrics, jsonMapper);
```

- [ ] **Step 4: Inject the new beans into the BUILD bean method and pass them to the helper**

In `aiHubBuildSpringAIAgent(...)` (the BUILD bean method; it already has `TriggerDefinitionService triggerDefinitionService` injected at ~line 371 — do NOT add it again), add the three still-missing parameters to the method signature:

```java
        ActionDefinitionService actionDefinitionService,
        ActionDefinitionFacade actionDefinitionFacade,
        TriggerDefinitionFacade triggerDefinitionFacade,
```

Then update the BUILD-mode call site (~line 483) to pass all of them through:

```java
        registerToolAttachStateVisibilityToolCallbacks(
            toolCallbacks, taskService, taskToolFacade, connectionDefinitionService, workspaceConnectionFacade,
            actionDefinitionService, actionDefinitionFacade, triggerDefinitionService, triggerDefinitionFacade,
            propertyOptionsResolver, aiHubToolAttachMetrics, jsonMapper);
```

- [ ] **Step 5: Compile to verify wiring**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:compileJava`
Expected: BUILD SUCCESSFUL (all four beans resolve from `platform-component-api`, which is already a dependency).

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubConfiguration.java
git commit -m "0_732 Register property-options lookup tools in AI Hub ASK and BUILD catalogs"
```

---

## Task 5: Wiring test — both tools present in the shared catalog helper

**Files:**
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/config/PropertyOptionsToolWiringTest.java`

Because both the ASK and BUILD bean methods register their state-visibility tools through the single private static `registerToolAttachStateVisibilityToolCallbacks` helper, asserting that the helper adds the two new tools proves they appear in both catalogs. We invoke the private static method reflectively with mocks.

- [ ] **Step 1: Write the test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.ee.ai.hub.metric.AiHubToolAttachMetrics;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.ee.ai.hub.task.AiHubTaskToolFacade;
import com.bytechef.ee.ai.hub.tool.PropertyOptionsResolver;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pins that the shared state-visibility registration helper adds the property-options lookup tools. Both the ASK and
 * BUILD agent bean methods delegate to this single helper, so presence here guarantees presence in both catalogs.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class PropertyOptionsToolWiringTest {

    @Test
    void testRegistersLookupOptionsTools() throws Exception {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        PropertyOptionsResolver resolver =
            new PropertyOptionsResolver(mock(UserService.class), mock(AuthorityService.class));

        Method method = AiHubConfiguration.class.getDeclaredMethod(
            "registerToolAttachStateVisibilityToolCallbacks", List.class, AiHubTaskService.class,
            AiHubTaskToolFacade.class, ConnectionDefinitionService.class, WorkspaceConnectionFacade.class,
            ActionDefinitionService.class, ActionDefinitionFacade.class, TriggerDefinitionService.class,
            TriggerDefinitionFacade.class, PropertyOptionsResolver.class, AiHubToolAttachMetrics.class,
            JsonMapper.class);

        method.setAccessible(true);

        method.invoke(
            null, toolCallbacks, mock(AiHubTaskService.class), mock(AiHubTaskToolFacade.class),
            mock(ConnectionDefinitionService.class), mock(WorkspaceConnectionFacade.class),
            mock(ActionDefinitionService.class), mock(ActionDefinitionFacade.class),
            mock(TriggerDefinitionService.class), mock(TriggerDefinitionFacade.class), resolver,
            mock(AiHubToolAttachMetrics.class), new JsonMapper());

        List<String> toolNames = toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();

        assertThat(toolNames).contains(
            "listConnectionsForComponent", "lookupActionPropertyOptions", "lookupTriggerPropertyOptions");
    }
}
```

- [ ] **Step 2: Run the test**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests 'com.bytechef.ee.ai.hub.config.PropertyOptionsToolWiringTest'`
Expected: PASS (1 test). If it fails with `NoSuchMethodException`, re-check the exact parameter order against the helper signature edited in Task 4 Step 2.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/config/PropertyOptionsToolWiringTest.java
git commit -m "0_732 Add wiring test for property-options lookup tools"
```

---

## Task 6: Update the BUILD and ASK prompts

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_build.txt`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_ask.txt`

- [ ] **Step 1: Replace the "no tool for third-party resources" paragraph in the BUILD prompt**

In `prompt_ai_hub_build.txt`, find the paragraph beginning "Never fabricate option values." (around lines 279–286) and replace its sentences about "You have no tool that enumerates a third-party service's own resources …" with the lookup-tool guidance. Replace the whole paragraph with:

```
Never fabricate option values. Every option in askUserQuestion MUST come from data a
tool actually returned (a connection from listConnectionsForComponent, a component from
the search, a project/workflow from the listing tools) — never a guess, an example, or a
typical default. When a property's descriptor shows "lookupRequired": true (Slack
channels, Notion databases, Airtable bases, etc.), call lookupActionPropertyOptions (or
lookupTriggerPropertyOptions for a trigger property) to fetch the real options before you
ask: first place any "lookupDependsOn" sibling values in inputParameters, and if the tool
returns connection_required, pick a connection via listConnectionsForComponent (or
createConnection) and retry with its connectionId. Render the returned options as
askUserQuestion buttons. If the success envelope has "truncated": true, ask the user to
narrow the list with searchText or to type the value rather than presenting an incomplete
set of buttons. Only when no tool can supply the values — and they can only come from the
user — ask in plain chat and let them type it. This matters more in BUILD mode: a
fabricated option the user picks gets written straight into the workflow as if it were
real.
```

- [ ] **Step 2: Replace the matching claim in the ASK prompt**

In `prompt_ai_hub_ask.txt`, find the paragraph beginning "Never fabricate option values." (around lines 109–116) containing "you have no tool that lists a third-party service's own resources …" and replace that paragraph with:

```
Never fabricate option values. Every option you put in askUserQuestion MUST come from
data a tool actually returned (e.g. a connection from listConnectionsForComponent, a
component from the search) — never a guess, an example, or a typical default. When a
property's descriptor shows "lookupRequired": true (Slack channels, Notion pages, Google
Sheets, etc.), call lookupActionPropertyOptions (or lookupTriggerPropertyOptions for a
trigger property) to fetch the real options — satisfy any "lookupDependsOn" siblings via
inputParameters and supply a connectionId when connection_required is returned. Render the
returned options as askUserQuestion buttons; if the result is "truncated": true, ask the
user to narrow with searchText or type the value. Only when no tool can supply the values
do you ask in plain chat and let them type it instead of presenting made-up buttons.
Inventing options the user then "picks" silently bakes a wrong guess into their setup.
```

- [ ] **Step 3: Verify the old claim is gone**

Run:

```bash
grep -rn "no tool that enumerates\|no tool that lists" \
  server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_build.txt \
  server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_ask.txt
```

Expected: no matches.

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_build.txt \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_ask.txt
git commit -m "0_732 Teach AI Hub prompts to call property-options lookup tools"
```

---

## Task 7: Module verification

- [ ] **Step 1: Run the full module test suite**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test`
Expected: BUILD SUCCESSFUL — all new tests plus the existing suite pass.

- [ ] **Step 2: Spotless + static analysis on the module**

Run: `./gradlew spotlessApply :server:ee:libs:ai:ai-hub:ai-hub-service:check`
Expected: BUILD SUCCESSFUL. If Spotless reformats, re-stage and amend the relevant commit. Confirm every new file carries the ByteChef Enterprise license header and a `@version ee` Javadoc tag (Spotless selects the header by file content via the `@version ee` marker).

- [ ] **Step 3: Final commit (only if Spotless changed files)**

```bash
git add -u server/ee/libs/ai/ai-hub/ai-hub-service
git commit -m "0_732 Apply spotless formatting to property-options lookup tools"
```

---

## Self-Review

**Spec coverage:**
- Two tool callbacks (action + trigger) → Tasks 2, 3. ✓
- Resolution flow (no-options / dependency-missing / connection-required / success) → Tasks 2, 3 tests + impl. ✓
- Search param + cap at 25 with `truncated` flag → Task 1 (`buildSuccessEnvelope`) + Tasks 2, 3 (`MAX_OPTIONS`, `searchText` passthrough). ✓
- Register in both ASK and BUILD catalogs via shared helper → Task 4; verified Task 5. ✓
- Prompt changes (build + ask) → Task 6. ✓
- Bring dead `PropertyOptionsResolver` helpers live + test → Tasks 1, 2, 3. ✓
- EE license header + `@version ee` on every new file → enforced/verified Task 7 Step 2. ✓

**Placeholder scan:** No TBD/TODO; every code step shows complete source; the trigger callback is fully written out (not "similar to Task 2"). ✓

**Type consistency:** `buildSuccessEnvelope(..., boolean truncated)` defined in Task 1 and called with 6 args in Tasks 2/3. Gate methods use `actionDefinesConnection` (action) vs `triggerDefinesConnection` (trigger) per the verified interfaces. Helper signature in Task 4 Step 2 matches the reflective lookup in Task 5 Step 1 (12 params, same order). Facade `executeOptions` 8-arg single-connection variant matches the mocked call. ✓

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-06-10-ai-hub-property-options-lookup.md`.
