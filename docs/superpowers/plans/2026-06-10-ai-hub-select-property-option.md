# AI Hub `selectPropertyOption` render tool — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **Git note (IMPORTANT):** the user commits to `0_732` in parallel. NEVER `git commit --amend` — HEAD may be the user's commit. Always make fresh commits.

**Goal:** Render component-property options (e.g. Slack channels) as a real client-driven picker by adding `selectPropertyOption`/`selectTriggerPropertyOption` tools whose result the AI Hub client renders as a searchable `<ComboBox>` of ALL options, submitting the option's real value (channel ID) — removing the LLM from the option list so it can't cherry-pick.

**Architecture:** Extract the shared gating+fetch sequence (currently duplicated in the two lookup callbacks) into `PropertyOptionsResolver.resolveAction/TriggerPropertyOptions(...)` returning a sealed `OptionsLookupResult` (`Success`/`Failure`). The two existing lookup callbacks and the two new select callbacks all call it; lookup formats success as an LLM envelope, select formats it as a `select-property-option` marker. Client: `AiHubRuntimeProvider` intercepts the marker → `data-select-property-option` part → `AiHubSelectPropertyOptionMessage` renders the picker (mirrors the `selectConnection` pipeline).

**Tech Stack:** Java 25, Spring Boot 4, Spring AI, Jackson 3 (`tools.jackson`) + Jackson 2 (`com.fasterxml.jackson` for `@JsonInclude`, as `SelectConnectionToolCallback` uses), JUnit 5 + Mockito + AssertJ (server); React 19 + TS, Vitest + Testing Library + userEvent (client). EE server files keep the Enterprise header + `@version ee`.

---

## Background facts (verified)

- The two lookup callbacks (`LookupActionPropertyOptionsToolCallback`, `LookupTriggerPropertyOptionsToolCallback`) currently run this sequence inline in `call(...)`: validate blanks + workspace ctx → existence checks (`getActionDefinitions`/`getTriggerDefinitions` → `action_not_found`/`trigger_not_found`; `getActionDefinition`/`getTriggerDefinition`.getProperties() + `topPropertySegment` → `property_not_found`) → `propertyHasOptionsDataSource` → `no_options` → `getPropertyLookupDependsOn` + missing-sibling (full path OR last segment) → `dependency_missing` → connection gate (`actionDefinesConnection`/`triggerDefinesConnection`) → `connection_required` → `executeOptions` inside `withUserSecurityContext` → cap at `MAX_OPTIONS=25` → `buildSuccessEnvelope`. Each gate records a metric tag.
- `PropertyOptionsResolver` (`com.bytechef.ee.ai.hub.tool`, EE) holds `withUserSecurityContext`, `buildSuccessEnvelope(componentName, entityKey, entityName, propertyName, options, truncated)`, `connectionRequiredEnvelope`, `dependencyMissingEnvelope`, `noOptionsForPropertyEnvelope`, `entityNotFoundEnvelope(errorCode, entityKey, requested, valid)`. It depends on `UserService`, `AuthorityService`.
- `SelectConnectionToolCallback` is the marker-tool pattern: returns `@JsonInclude(NON_NULL)` record `SelectConnectionOutput(kind, componentName, componentLabel)` with `kind="select-connection"`; uses `com.fasterxml.jackson.annotation.JsonInclude`.
- Client `AiHubRuntimeProvider.tsx`: interface `SelectConnectionResultI {componentName; componentLabel; kind:'select-connection'}` (~line 80); interceptor branch `else if (toolCallName === 'selectConnection') {...}` (~line 871) that `parseJson`, validates `kind`, and `addMessage({content:[{data:{...}, type:'data-select-connection'}], role:'assistant'})`. On malformed payload it calls `aiHubToolCallStore...completeToolCall(...)` + `aiHubRetryableErrorStore...setError(...)`.
- Client `AiHubMessageContent.tsx`: `data.by_name` registry maps `kind` → component (`'select-connection': SelectConnectionData`).
- Client `AiHubSelectConnectionMessage.tsx`: renders a control, on pick `threadRuntime.append({content:[{text:'User picked: <name> (ID: <id>)', type:'text'}], role:'system'})`, dims via `threadRuntime.subscribe` superseded-by-later-message.
- Client `ComboBox` (default export `@/components/ComboBox/ComboBox`): props `{items: {label, value}[], onChange?: (item?) => void, value?, emptyMessage?}`.
- `ActionDefinitionService`/`TriggerDefinitionService` + `ActionDefinitionFacade`/`TriggerDefinitionFacade` are in `platform-component-api` (already a dep). Domain `ActionDefinition`/`TriggerDefinition`/`Property` have `getName()`; definitions have `getProperties(): List<? extends Property>`.

Run server tests: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '<FQCN>'`. Run client message tests from `client/`: `npm run test -- src/pages/automation/ai-hub/...`.

## File Structure

- Modify: `.../tool/PropertyOptionsResolver.java` — add `OptionsLookupResult` sealed type + `resolveActionPropertyOptions` + `resolveTriggerPropertyOptions` + `topPropertySegment`.
- Modify: `.../tool/LookupActionPropertyOptionsToolCallback.java`, `.../tool/LookupTriggerPropertyOptionsToolCallback.java` — delegate to the resolver (behavior unchanged).
- Create: `.../tool/SelectPropertyOptionToolCallback.java`, `.../tool/SelectTriggerPropertyOptionToolCallback.java`.
- Create tests: `SelectPropertyOptionToolCallbackTest.java`, `SelectTriggerPropertyOptionToolCallbackTest.java`; extend `PropertyOptionsResolverTest.java`.
- Modify: `.../config/AiHubConfiguration.java` — register both select tools; extend `PropertyOptionsToolWiringTest.java`.
- Modify: `.../resources/prompt_ai_hub_build.txt`, `prompt_ai_hub_ask.txt`.
- Create: `client/.../ai-hub/messages/AiHubSelectPropertyOptionMessage.tsx`; modify `AiHubRuntimeProvider.tsx`, `AiHubMessageContent.tsx`; extend `AiHubAskUserQuestionMessage.test.tsx` or a new test file.

---

## Task 1: Shared resolution in `PropertyOptionsResolver`

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/PropertyOptionsResolver.java`
- Test: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/PropertyOptionsResolverTest.java`

- [ ] **Step 1: Write failing tests**

Add to `PropertyOptionsResolverTest` (it already has `resolver`, imports `List`, `Map`, `mock`, `when`, `@Test`). Add imports:
```java
import com.bytechef.ee.ai.hub.tool.PropertyOptionsResolver.OptionsLookupResult.Failure;
import com.bytechef.ee.ai.hub.tool.PropertyOptionsResolver.OptionsLookupResult.Success;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import java.util.ArrayList;
```
Add tests:
```java
    private static ActionDefinitionService stubActionService(
        String component, int version, String action, String... propertyNames) {

        ActionDefinitionService service = mock(ActionDefinitionService.class);

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

        when(fetched.getProperties()).thenAnswer(invocation -> properties);
        when(service.getActionDefinition(component, version, action)).thenReturn(fetched);

        return service;
    }

    @Test
    void testResolveActionPropertyOptionsReturnsActionNotFound() {
        ActionDefinitionService service = mock(ActionDefinitionService.class);

        ActionDefinition real = mock(ActionDefinition.class);

        when(real.getName()).thenReturn("sendChannelMessage");
        when(service.getActionDefinitions("slack", 1)).thenReturn(List.of(real));

        OptionsLookupResult result = resolver.resolveActionPropertyOptions(
            service, mock(ActionDefinitionFacade.class), null, "slack", 1, "sendMessage", "channel", null, null, null,
            25);

        assertThat(result).isInstanceOf(Failure.class);
        assertThat(((Failure) result).metricTag()).isEqualTo("action_not_found");
        assertThat(((Failure) result).envelope()).containsEntry("error", "action_not_found");
    }

    @Test
    void testResolveActionPropertyOptionsReturnsPropertyNotFound() {
        ActionDefinitionService service = stubActionService("slack", 1, "sendChannelMessage", "channel", "text");

        OptionsLookupResult result = resolver.resolveActionPropertyOptions(
            service, mock(ActionDefinitionFacade.class), null, "slack", 1, "sendChannelMessage", "channelId", null,
            null, null, 25);

        assertThat(((Failure) result).metricTag()).isEqualTo("property_not_found");
    }

    @Test
    void testResolveActionPropertyOptionsReturnsConnectionRequired() {
        ActionDefinitionService service = stubActionService("slack", 1, "sendChannelMessage", "channel");

        when(service.propertyHasOptionsDataSource("slack", 1, "sendChannelMessage", "channel")).thenReturn(true);
        when(service.getPropertyLookupDependsOn("slack", 1, "sendChannelMessage", "channel")).thenReturn(List.of());
        when(service.actionDefinesConnection("slack", 1, "sendChannelMessage")).thenReturn(true);

        OptionsLookupResult result = resolver.resolveActionPropertyOptions(
            service, mock(ActionDefinitionFacade.class), null, "slack", 1, "sendChannelMessage", "channel", null, null,
            null, 25);

        assertThat(((Failure) result).metricTag()).isEqualTo("connection_required");
    }

    @Test
    void testResolveActionPropertyOptionsReturnsCappedSuccess() {
        ActionDefinitionService service = stubActionService("slack", 1, "sendChannelMessage", "channel");
        ActionDefinitionFacade facade = mock(ActionDefinitionFacade.class);

        when(service.propertyHasOptionsDataSource("slack", 1, "sendChannelMessage", "channel")).thenReturn(true);
        when(service.getPropertyLookupDependsOn("slack", 1, "sendChannelMessage", "channel")).thenReturn(List.of());
        when(service.actionDefinesConnection("slack", 1, "sendChannelMessage")).thenReturn(true);

        List<Option> options = new ArrayList<>();

        for (int i = 0; i < 30; i++) {
            Option option = mock(Option.class);

            when(option.getValue()).thenReturn("C" + i);

            options.add(option);
        }

        when(facade.executeOptions(
            org.mockito.ArgumentMatchers.eq("slack"), org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.eq("sendChannelMessage"), org.mockito.ArgumentMatchers.eq("channel"),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(7L)))
                .thenReturn(options);

        OptionsLookupResult result = resolver.resolveActionPropertyOptions(
            service, facade, null, "slack", 1, "sendChannelMessage", "channel", null, 7L, null, 25);

        assertThat(result).isInstanceOf(Success.class);
        assertThat(((Success) result).options()).hasSize(25);
        assertThat(((Success) result).truncated()).isTrue();
    }
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests 'com.bytechef.ee.ai.hub.tool.PropertyOptionsResolverTest'`
Expected: COMPILE FAILURE — `OptionsLookupResult` / `resolveActionPropertyOptions` don't exist.

- [ ] **Step 3: Implement in `PropertyOptionsResolver.java`**

Add imports:
```java
import com.bytechef.ee.ai.hub.tool.AiHubToolInvocationContext;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
```
(`AiHubToolInvocationContext` is in the same package `com.bytechef.ee.ai.hub.tool` — no import needed if same package; it actually lives in `ai-hub-api` under that package, so no import statement is required.)

Add the sealed result type as a nested type:
```java
    /**
     * Outcome of a property-options resolution: either a structured {@link Failure} envelope (with the metric tag the
     * caller should record) or a {@link Success} carrying the capped options. Lets the lookup callbacks (which format
     * success as an LLM envelope) and the select callbacks (which format it as a client render marker) share the entire
     * gating + fetch sequence.
     */
    public sealed interface OptionsLookupResult {

        record Failure(Map<String, Object> envelope, String metricTag) implements OptionsLookupResult {
        }

        record Success(List<Option> options, boolean truncated) implements OptionsLookupResult {
        }
    }
```

Add the two resolve methods + the path helper:
```java
    /**
     * Runs the full action-property options gating + fetch sequence. Returns a {@link OptionsLookupResult.Failure} with
     * the matching error envelope + metric tag at the first failing gate, or {@link OptionsLookupResult.Success} with
     * the options capped at {@code maxOptions}.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OptionsLookupResult resolveActionPropertyOptions(
        ActionDefinitionService actionDefinitionService, ActionDefinitionFacade actionDefinitionFacade,
        @Nullable AiHubToolInvocationContext invocationContext, String componentName, int componentVersion,
        String actionName, String propertyName, @Nullable Map<String, Object> inputParameters,
        @Nullable Long connectionId, @Nullable String searchText, int maxOptions) {

        List<String> validActionNames = actionDefinitionService.getActionDefinitions(componentName, componentVersion)
            .stream()
            .map(ActionDefinition::getName)
            .toList();

        if (!validActionNames.contains(actionName)) {
            return new OptionsLookupResult.Failure(
                entityNotFoundEnvelope("action_not_found", "actionName", actionName, validActionNames),
                "action_not_found");
        }

        ActionDefinition actionDefinition =
            actionDefinitionService.getActionDefinition(componentName, componentVersion, actionName);

        List<String> validPropertyNames = actionDefinition.getProperties()
            .stream()
            .map(Property::getName)
            .toList();

        if (!validPropertyNames.contains(topPropertySegment(propertyName))) {
            return new OptionsLookupResult.Failure(
                entityNotFoundEnvelope("property_not_found", "propertyName", propertyName, validPropertyNames),
                "property_not_found");
        }

        if (!actionDefinitionService.propertyHasOptionsDataSource(
            componentName, componentVersion, actionName, propertyName)) {

            return new OptionsLookupResult.Failure(noOptionsForPropertyEnvelope(), "no_options");
        }

        List<String> lookupDependsOnPaths = actionDefinitionService.getPropertyLookupDependsOn(
            componentName, componentVersion, actionName, propertyName);

        Map<String, Object> parameters = inputParameters == null ? Map.of() : inputParameters;

        List<String> missing = lookupDependsOnPaths.stream()
            .filter(path -> !parameters.containsKey(path)
                && !parameters.containsKey(path.substring(path.lastIndexOf('.') + 1)))
            .toList();

        if (!missing.isEmpty()) {
            return new OptionsLookupResult.Failure(dependencyMissingEnvelope(missing), "dependency_missing");
        }

        if (connectionId == null
            && actionDefinitionService.actionDefinesConnection(componentName, componentVersion, actionName)) {

            return new OptionsLookupResult.Failure(connectionRequiredEnvelope(componentName), "connection_required");
        }

        int resolvedVersion = componentVersion;

        List<Option> options = withUserSecurityContext(
            invocationContext == null ? null : invocationContext.userId(),
            () -> actionDefinitionFacade.executeOptions(
                componentName, resolvedVersion, actionName, propertyName, parameters, lookupDependsOnPaths, searchText,
                connectionId));

        boolean truncated = options.size() > maxOptions;

        List<Option> capped = truncated ? options.subList(0, maxOptions) : options;

        return new OptionsLookupResult.Success(capped, truncated);
    }

    /**
     * Trigger twin of {@link #resolveActionPropertyOptions}; uses {@code trigger_not_found} and
     * {@code triggerDefinesConnection}.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OptionsLookupResult resolveTriggerPropertyOptions(
        TriggerDefinitionService triggerDefinitionService, TriggerDefinitionFacade triggerDefinitionFacade,
        @Nullable AiHubToolInvocationContext invocationContext, String componentName, int componentVersion,
        String triggerName, String propertyName, @Nullable Map<String, Object> inputParameters,
        @Nullable Long connectionId, @Nullable String searchText, int maxOptions) {

        List<String> validTriggerNames = triggerDefinitionService.getTriggerDefinitions(componentName, componentVersion)
            .stream()
            .map(TriggerDefinition::getName)
            .toList();

        if (!validTriggerNames.contains(triggerName)) {
            return new OptionsLookupResult.Failure(
                entityNotFoundEnvelope("trigger_not_found", "triggerName", triggerName, validTriggerNames),
                "trigger_not_found");
        }

        TriggerDefinition triggerDefinition =
            triggerDefinitionService.getTriggerDefinition(componentName, componentVersion, triggerName);

        List<String> validPropertyNames = triggerDefinition.getProperties()
            .stream()
            .map(Property::getName)
            .toList();

        if (!validPropertyNames.contains(topPropertySegment(propertyName))) {
            return new OptionsLookupResult.Failure(
                entityNotFoundEnvelope("property_not_found", "propertyName", propertyName, validPropertyNames),
                "property_not_found");
        }

        if (!triggerDefinitionService.propertyHasOptionsDataSource(
            componentName, componentVersion, triggerName, propertyName)) {

            return new OptionsLookupResult.Failure(noOptionsForPropertyEnvelope(), "no_options");
        }

        List<String> lookupDependsOnPaths = triggerDefinitionService.getPropertyLookupDependsOn(
            componentName, componentVersion, triggerName, propertyName);

        Map<String, Object> parameters = inputParameters == null ? Map.of() : inputParameters;

        List<String> missing = lookupDependsOnPaths.stream()
            .filter(path -> !parameters.containsKey(path)
                && !parameters.containsKey(path.substring(path.lastIndexOf('.') + 1)))
            .toList();

        if (!missing.isEmpty()) {
            return new OptionsLookupResult.Failure(dependencyMissingEnvelope(missing), "dependency_missing");
        }

        if (connectionId == null
            && triggerDefinitionService.triggerDefinesConnection(componentName, componentVersion, triggerName)) {

            return new OptionsLookupResult.Failure(connectionRequiredEnvelope(componentName), "connection_required");
        }

        int resolvedVersion = componentVersion;

        List<Option> options = withUserSecurityContext(
            invocationContext == null ? null : invocationContext.userId(),
            () -> triggerDefinitionFacade.executeOptions(
                componentName, resolvedVersion, triggerName, propertyName, parameters, lookupDependsOnPaths, searchText,
                connectionId));

        boolean truncated = options.size() > maxOptions;

        List<Option> capped = truncated ? options.subList(0, maxOptions) : options;

        return new OptionsLookupResult.Success(capped, truncated);
    }

    /**
     * Top-level container segment of a (possibly dotted / array) property path: {@code parent.child} → {@code parent},
     * {@code items[].id} → {@code items}, {@code channel} → {@code channel}. Existence is checked only at the top level.
     */
    public static String topPropertySegment(String propertyName) {
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

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests 'com.bytechef.ee.ai.hub.tool.PropertyOptionsResolverTest'`
Expected: PASS (prior tests + 4 new).

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/PropertyOptionsResolver.java \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/PropertyOptionsResolverTest.java
git commit -m "0_732 Add shared resolveAction/TriggerPropertyOptions to PropertyOptionsResolver"
```

---

## Task 2: Refactor the two lookup callbacks to delegate

**Files:**
- Modify: `.../tool/LookupActionPropertyOptionsToolCallback.java`, `.../tool/LookupTriggerPropertyOptionsToolCallback.java`

Behavior must stay identical (their existing tests are the guard). Replace the inline gating with a single resolver call.

- [ ] **Step 1: Refactor the action callback**

In `LookupActionPropertyOptionsToolCallback.java`, inside `call(...)`, KEEP the blank-validation of `componentName`/`actionName`/`propertyName`, the `invocationContext` null/workspace check, and the `componentVersion` resolution. REPLACE everything from the `validActionNames` block through the `buildSuccessEnvelope` return with:
```java
            PropertyOptionsResolver.OptionsLookupResult result = resolver.resolveActionPropertyOptions(
                actionDefinitionService, actionDefinitionFacade, invocationContext, componentName, componentVersion,
                actionName, propertyName, input.inputParameters(), input.connectionId(), input.searchText(),
                MAX_OPTIONS);

            if (result instanceof PropertyOptionsResolver.OptionsLookupResult.Failure failure) {
                metrics.recordStateVisibility(TOOL_NAME, failure.metricTag());

                return jsonMapper.writeValueAsString(failure.envelope());
            }

            PropertyOptionsResolver.OptionsLookupResult.Success success =
                (PropertyOptionsResolver.OptionsLookupResult.Success) result;

            metrics.recordStateVisibility(TOOL_NAME, success.options()
                .isEmpty() ? "empty" : "success");

            return jsonMapper.writeValueAsString(
                resolver.buildSuccessEnvelope(
                    componentName, "actionName", actionName, propertyName, success.options(), success.truncated()));
```
Delete the now-unused `topPropertySegment` private helper from this class (it moved to the resolver) and remove now-unused imports (`ActionDefinition`, `Property` if no longer referenced; keep `ActionDefinitionService`/`ActionDefinitionFacade`/`Option` as needed — `Option` may no longer be referenced, remove if so). Let the compiler guide unused-import removal.

- [ ] **Step 2: Refactor the trigger callback**

In `LookupTriggerPropertyOptionsToolCallback.java`, same shape, calling `resolver.resolveTriggerPropertyOptions(triggerDefinitionService, triggerDefinitionFacade, invocationContext, componentName, componentVersion, triggerName, propertyName, input.inputParameters(), input.connectionId(), input.searchText(), MAX_OPTIONS)` and `buildSuccessEnvelope(componentName, "triggerName", triggerName, ...)`. Delete the local `topPropertySegment`; clean unused imports.

- [ ] **Step 3: Run both existing test suites (the behavior guard)**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests 'com.bytechef.ee.ai.hub.tool.LookupActionPropertyOptionsToolCallbackTest' --tests 'com.bytechef.ee.ai.hub.tool.LookupTriggerPropertyOptionsToolCallbackTest'`
Expected: PASS — all existing tests still green (same behavior, logic relocated).

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/LookupActionPropertyOptionsToolCallback.java \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/LookupTriggerPropertyOptionsToolCallback.java
git commit -m "0_732 Delegate lookup callbacks to shared PropertyOptionsResolver resolution"
```

---

## Task 3: `SelectPropertyOptionToolCallback` (action)

**Files:**
- Test: `.../tool/SelectPropertyOptionToolCallbackTest.java`
- Create: `.../tool/SelectPropertyOptionToolCallback.java`

- [ ] **Step 1: Write the failing test**

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
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
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
class SelectPropertyOptionToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    private final PropertyOptionsResolver resolver =
        new PropertyOptionsResolver(mock(UserService.class), mock(AuthorityService.class));

    private ToolContext toolContext() {
        return new ToolContext(
            new AiHubToolInvocationContext(1L, null, (short) 0, "x", 0L, "thread-1").toToolContext());
    }

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

        when(fetched.getProperties()).thenAnswer(invocation -> properties);
        when(service.getActionDefinition(component, version, action)).thenReturn(fetched);
    }

    @Test
    void testReturnsSelectMarkerWithAllOptionsOnSuccess() throws Exception {
        ActionDefinitionService service = mock(ActionDefinitionService.class);
        ActionDefinitionFacade facade = mock(ActionDefinitionFacade.class);

        stubValidAction(service, "slack", 1, "sendChannelMessage", "channel");

        when(service.propertyHasOptionsDataSource("slack", 1, "sendChannelMessage", "channel")).thenReturn(true);
        when(service.getPropertyLookupDependsOn("slack", 1, "sendChannelMessage", "channel")).thenReturn(List.of());
        when(service.actionDefinesConnection("slack", 1, "sendChannelMessage")).thenReturn(true);

        Option general = mock(Option.class);
        Option random = mock(Option.class);

        when(general.getLabel()).thenReturn("general");
        when(general.getValue()).thenReturn("C06H2PR8LSV");
        when(random.getLabel()).thenReturn("random");
        when(random.getValue()).thenReturn("C06GSJ5RPBN");

        when(facade.executeOptions(eq("slack"), anyInt(), eq("sendChannelMessage"), eq("channel"), any(), any(),
            any(), eq(7L))).thenReturn(List.of(general, random));

        SelectPropertyOptionToolCallback callback = new SelectPropertyOptionToolCallback(
            service, facade, resolver, mock(AiHubToolAttachMetrics.class), jsonMapper);

        String result = callback.call(
            "{\"componentName\":\"slack\",\"actionName\":\"sendChannelMessage\",\"propertyName\":\"channel\","
                + "\"connectionId\":7}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("kind")
            .asText()).isEqualTo("select-property-option");
        assertThat(node.get("componentName")
            .asText()).isEqualTo("slack");
        assertThat(node.get("propertyName")
            .asText()).isEqualTo("channel");
        assertThat(node.get("options")
            .size()).isEqualTo(2);
        assertThat(node.get("options")
            .get(0)
            .get("label")
            .asText()).isEqualTo("general");
        assertThat(node.get("options")
            .get(0)
            .get("value")
            .asText()).isEqualTo("C06H2PR8LSV");
        assertThat(node.get("truncated")
            .asBoolean()).isFalse();
    }

    @Test
    void testReturnsPropertyNotFoundEnvelope() throws Exception {
        ActionDefinitionService service = mock(ActionDefinitionService.class);

        stubValidAction(service, "slack", 1, "sendChannelMessage", "channel", "text");

        SelectPropertyOptionToolCallback callback = new SelectPropertyOptionToolCallback(
            service, mock(ActionDefinitionFacade.class), resolver, mock(AiHubToolAttachMetrics.class), jsonMapper);

        String result = callback.call(
            "{\"componentName\":\"slack\",\"actionName\":\"sendChannelMessage\",\"propertyName\":\"channelId\"}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).isEqualTo("property_not_found");
    }

    @Test
    void testRejectsBlankPropertyName() throws Exception {
        SelectPropertyOptionToolCallback callback = new SelectPropertyOptionToolCallback(
            mock(ActionDefinitionService.class), mock(ActionDefinitionFacade.class), resolver,
            mock(AiHubToolAttachMetrics.class), jsonMapper);

        String result = callback.call(
            "{\"componentName\":\"slack\",\"actionName\":\"sendChannelMessage\",\"propertyName\":\"\"}", toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).contains("propertyName is required");
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests 'com.bytechef.ee.ai.hub.tool.SelectPropertyOptionToolCallbackTest'`
Expected: COMPILE FAILURE — class doesn't exist.

- [ ] **Step 3: Create the callback**

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
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
 * Spring AI {@link ToolCallback} that asks the AI Hub client to render a searchable picker of an ACTION property's
 * dynamic options (e.g. Slack channels). Unlike {@link LookupActionPropertyOptionsToolCallback} (which returns the
 * options to the LLM), this returns a {@code select-property-option} marker carrying ALL fetched options so the client
 * renders them directly — the LLM never re-emits the list and cannot drop options. The picker submits the option's
 * real value (e.g. the channel id), not its label. Failures reuse the same envelopes as the lookup tool so the agent
 * self-corrects.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class SelectPropertyOptionToolCallback implements ToolCallback {

    static final String TOOL_NAME = "selectPropertyOption";

    private static final int MAX_OPTIONS = 25;

    private static final Logger log = LoggerFactory.getLogger(SelectPropertyOptionToolCallback.class);

    private static final String DESCRIPTION = """
        Ask the user to PICK a value for a component ACTION property that has a dynamic option list (its descriptor
        shows "lookupRequired": true) — e.g. a Slack channel. The client renders a searchable dropdown of ALL options
        fetched from the connection; the user's pick (its real value/id) is captured as their next message. Satisfy any
        "lookupDependsOn" siblings via inputParameters and pass connectionId when required. Pass the canonical property
        name and actionName from the descriptor, never the human label. Use this instead of hand-building
        askUserQuestion for option properties. On a wrong name the tool returns action_not_found / property_not_found
        with the valid names — retry with one of those.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "componentName": {"type": "string"},
                "componentVersion": {"type": "integer", "description": "Defaults to 1"},
                "actionName": {"type": "string"},
                "propertyName": {"type": "string", "description": "Canonical property name (not the label)"},
                "inputParameters": {"type": "object", "description": "Sibling values required by lookupDependsOn"},
                "connectionId": {"type": "integer"},
                "searchText": {"type": "string"}
            },
            "required": ["componentName", "actionName", "propertyName"]
        }""";

    private final ActionDefinitionService actionDefinitionService;
    private final ActionDefinitionFacade actionDefinitionFacade;
    private final PropertyOptionsResolver resolver;
    private final AiHubToolAttachMetrics metrics;
    private final JsonMapper jsonMapper;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SelectPropertyOptionToolCallback(
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
            SelectPropertyOptionInput input = jsonMapper.readValue(toolInput, SelectPropertyOptionInput.class);

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

            PropertyOptionsResolver.OptionsLookupResult result = resolver.resolveActionPropertyOptions(
                actionDefinitionService, actionDefinitionFacade, invocationContext, componentName, componentVersion,
                actionName, propertyName, input.inputParameters(), input.connectionId(), input.searchText(),
                MAX_OPTIONS);

            if (result instanceof PropertyOptionsResolver.OptionsLookupResult.Failure failure) {
                metrics.recordStateVisibility(TOOL_NAME, failure.metricTag());

                return jsonMapper.writeValueAsString(failure.envelope());
            }

            PropertyOptionsResolver.OptionsLookupResult.Success success =
                (PropertyOptionsResolver.OptionsLookupResult.Success) result;

            metrics.recordStateVisibility(TOOL_NAME, success.options()
                .isEmpty() ? "empty" : "success");

            return jsonMapper.writeValueAsString(
                buildMarker(componentName, propertyName, success.options(), success.truncated()));
        } catch (JacksonException exception) {
            log.warn(
                "selectPropertyOption rejected malformed tool input: {} — first 200 chars: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            metrics.recordStateVisibility(TOOL_NAME, "error");

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            metrics.recordStateVisibility(TOOL_NAME, "error");

            return ToolErrors.runtimeFailure(jsonMapper, SelectPropertyOptionToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String buildMarker(String componentName, String propertyName, List<Option> options, boolean truncated) {
        List<Map<String, Object>> optionRows = new ArrayList<>(options.size());

        for (Option option : options) {
            Map<String, Object> optionRow = new LinkedHashMap<>();

            optionRow.put("label", option.getLabel());
            optionRow.put("value", option.getValue());

            optionRows.add(optionRow);
        }

        Map<String, Object> marker = new LinkedHashMap<>();

        marker.put("kind", "select-property-option");
        marker.put("componentName", componentName);
        marker.put("propertyName", propertyName);
        marker.put("options", optionRows);
        marker.put("truncated", truncated);

        try {
            return jsonMapper.writeValueAsString(marker);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize select-property-option marker", exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SelectPropertyOptionInput(
        String componentName, @Nullable Integer componentVersion, String actionName, String propertyName,
        @Nullable Map<String, Object> inputParameters, @Nullable Long connectionId, @Nullable String searchText) {

        // Defensive-copy via compact constructor — sidesteps SpotBugs EI on the inputParameters Map.
        public SelectPropertyOptionInput {
            inputParameters = inputParameters == null ? null : Map.copyOf(inputParameters);
        }
    }
}
```
NOTE: `call(...)` serializes the marker via `buildMarker`, which itself serializes — so the outer `return jsonMapper.writeValueAsString(buildMarker(...))` would double-serialize. FIX: `buildMarker` returns the JSON `String` already, so change the success return to `return buildMarker(componentName, propertyName, success.options(), success.truncated());` (no outer `writeValueAsString`). Apply this correction when implementing.

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests 'com.bytechef.ee.ai.hub.tool.SelectPropertyOptionToolCallbackTest'`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/SelectPropertyOptionToolCallback.java \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/SelectPropertyOptionToolCallbackTest.java
git commit -m "0_732 Add selectPropertyOption render tool"
```

---

## Task 4: `SelectTriggerPropertyOptionToolCallback` (trigger)

**Files:**
- Test: `.../tool/SelectTriggerPropertyOptionToolCallbackTest.java`
- Create: `.../tool/SelectTriggerPropertyOptionToolCallback.java`

Trigger twin of Task 3: `TriggerDefinitionService`/`TriggerDefinitionFacade`, tool name `selectTriggerPropertyOption`, input field `triggerName`, calls `resolver.resolveTriggerPropertyOptions(...)`, emits the SAME `kind:"select-property-option"` marker (omit the trigger name — the client doesn't need it). DESCRIPTION says "TRIGGER".

- [ ] **Step 1: Write the failing test** — mirror `SelectPropertyOptionToolCallbackTest` with `TriggerDefinitionService`/`TriggerDefinitionFacade`, `triggerName: "newMessage"`, `triggerDefinesConnection`, and a `stubValidTrigger` helper (use `thenAnswer` for `getProperties()`). Assert success marker `kind=="select-property-option"`, `componentName=="slack"`, `propertyName=="channel"`, 2 options; and a `property_not_found` case. Full mirror of Task 3's test, substituting trigger types/names.

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests 'com.bytechef.ee.ai.hub.tool.SelectTriggerPropertyOptionToolCallbackTest'`
Expected: COMPILE FAILURE.

- [ ] **Step 3: Create the callback** — copy Task 3's `SelectPropertyOptionToolCallback.java` verbatim and apply these substitutions: imports `TriggerDefinitionFacade`/`TriggerDefinitionService`; `TOOL_NAME = "selectTriggerPropertyOption"`; fields/ctor `triggerDefinitionService`/`triggerDefinitionFacade`; `call(...)` reads `triggerName` (record field `triggerName`, validation message "triggerName is required", schema required `["componentName","triggerName","propertyName"]`); calls `resolver.resolveTriggerPropertyOptions(triggerDefinitionService, triggerDefinitionFacade, invocationContext, componentName, componentVersion, triggerName, propertyName, input.inputParameters(), input.connectionId(), input.searchText(), MAX_OPTIONS)`; `buildMarker` unchanged (omits trigger name); DESCRIPTION says TRIGGER; record `SelectTriggerPropertyOptionInput(componentName, componentVersion, triggerName, propertyName, inputParameters, connectionId, searchText)` with the same defensive-copy compact constructor. Apply the same "buildMarker returns the JSON string — don't double-serialize" correction.

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests 'com.bytechef.ee.ai.hub.tool.SelectTriggerPropertyOptionToolCallbackTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/SelectTriggerPropertyOptionToolCallback.java \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/SelectTriggerPropertyOptionToolCallbackTest.java
git commit -m "0_732 Add selectTriggerPropertyOption render tool"
```

---

## Task 5: Register both select tools + wiring test

**Files:**
- Modify: `.../config/AiHubConfiguration.java`
- Test: `.../config/PropertyOptionsToolWiringTest.java`

- [ ] **Step 1: Register in the shared helper**

In `AiHubConfiguration.java`, add imports for `SelectPropertyOptionToolCallback` and `SelectTriggerPropertyOptionToolCallback` (alongside the `LookupXxx` imports). In `registerToolAttachStateVisibilityToolCallbacks(...)` (which already receives `actionDefinitionService`, `actionDefinitionFacade`, `triggerDefinitionService`, `triggerDefinitionFacade`, `propertyOptionsResolver`, `aiHubToolAttachMetrics`, `jsonMapper`), append after the two `LookupXxx` registrations:
```java
        toolCallbacks.add(
            new SelectPropertyOptionToolCallback(
                actionDefinitionService, actionDefinitionFacade, propertyOptionsResolver, aiHubToolAttachMetrics,
                jsonMapper));
        toolCallbacks.add(
            new SelectTriggerPropertyOptionToolCallback(
                triggerDefinitionService, triggerDefinitionFacade, propertyOptionsResolver, aiHubToolAttachMetrics,
                jsonMapper));
```
No new bean-method parameters are needed (the helper already has all four facades/services from the prior feature).

- [ ] **Step 2: Extend the wiring test**

In `PropertyOptionsToolWiringTest.java`, extend the existing `contains(...)` assertion to also require the two new tool names:
```java
        assertThat(toolNames).contains(
            "listConnectionsForComponent", "lookupActionPropertyOptions", "lookupTriggerPropertyOptions",
            "selectPropertyOption", "selectTriggerPropertyOption");
```

- [ ] **Step 3: Run**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests 'com.bytechef.ee.ai.hub.config.PropertyOptionsToolWiringTest'`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubConfiguration.java \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/config/PropertyOptionsToolWiringTest.java
git commit -m "0_732 Register selectPropertyOption tools in AI Hub catalogs"
```

---

## Task 6: Prompt updates

**Files:**
- Modify: `.../resources/prompt_ai_hub_build.txt`, `prompt_ai_hub_ask.txt`

- [ ] **Step 1: BUILD prompt** — after the existing lookup-usage paragraph, insert:
```
To let the user choose a value for a property whose descriptor shows "lookupRequired": true,
call selectPropertyOption (action property) or selectTriggerPropertyOption (trigger property)
INSTEAD of hand-listing options in askUserQuestion. These render a searchable picker of ALL
options fetched from the connection and return the option's real value (e.g. the channel id)
— so never copy option lists into askUserQuestion yourself. Pass the canonical property name
and action/trigger name (not the human label) and a connectionId when required; on
action_not_found / trigger_not_found / property_not_found retry with a name from the "valid"
array.
```

- [ ] **Step 2: ASK prompt** — insert the same guidance (condensed to match the ASK prompt's terser style), naming both tools and the "renders a picker of all options, returns the real value" behavior.

- [ ] **Step 3: Verify**

Run:
```bash
grep -c "selectPropertyOption" server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_build.txt server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_ask.txt
```
Expected: ≥1 in each.

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_build.txt \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_ask.txt
git commit -m "0_732 Tell AI Hub agent to use selectPropertyOption to present option choices"
```

---

## Task 7: Client — interceptor + renderer + registry

**Files:**
- Create: `client/src/pages/automation/ai-hub/messages/AiHubSelectPropertyOptionMessage.tsx`
- Modify: `client/src/pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider.tsx`
- Modify: `client/src/pages/automation/ai-hub/messages/AiHubMessageContent.tsx`
- Test: `client/src/pages/automation/ai-hub/messages/tests/AiHubSelectPropertyOptionMessage.test.tsx`

- [ ] **Step 1: Write the failing renderer test**

Create `AiHubSelectPropertyOptionMessage.test.tsx`:
```typescript
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const appendCalls: Array<unknown> = [];

vi.mock('@assistant-ui/react', async () => {
    const actual = await vi.importActual<typeof import('@assistant-ui/react')>('@assistant-ui/react');

    return {
        ...actual,
        useThreadRuntime: vi.fn(() => ({
            append: (message: unknown) => appendCalls.push(message),
            getState: () => ({messages: []}),
            subscribe: () => () => {},
        })),
    };
});

import AiHubSelectPropertyOptionMessage from '../AiHubSelectPropertyOptionMessage';

const DATA = {
    componentName: 'slack',
    kind: 'select-property-option' as const,
    options: [
        {label: 'general', value: 'C06H2PR8LSV'},
        {label: 'random', value: 'C06GSJ5RPBN'},
    ],
    propertyName: 'channel',
    truncated: false,
};

describe('AiHubSelectPropertyOptionMessage', () => {
    beforeEach(() => {
        appendCalls.length = 0;
    });

    it('renders a picker of all options and submits the picked option value', async () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        render(<AiHubSelectPropertyOptionMessage data={DATA as any} />);

        await userEvent.click(screen.getByRole('combobox'));

        await userEvent.click(screen.getByRole('option', {name: 'general'}));

        expect(appendCalls).toHaveLength(1);
        expect(JSON.stringify(appendCalls[0])).toContain('User picked: general (value: C06H2PR8LSV)');
    });
});
```
(Confirm the `ComboBox` trigger role is `combobox` — Task 5 of the prior feature established it is. If the renderer chooses a different control, adjust the selector.)

- [ ] **Step 2: Run to verify failure**

Run (from `client/`): `npm run test -- src/pages/automation/ai-hub/messages/tests/AiHubSelectPropertyOptionMessage.test.tsx`
Expected: FAIL — module doesn't exist.

- [ ] **Step 3: Create the renderer**

```typescript
import ComboBox from '@/components/ComboBox/ComboBox';
import {DataMessagePartProps, useThreadRuntime} from '@assistant-ui/react';
import {CheckIcon} from 'lucide-react';
import {useEffect, useState} from 'react';

export interface SelectPropertyOptionItemI {
    label: string;
    value: string;
}

export interface SelectPropertyOptionDataI {
    componentName: string;
    kind: 'select-property-option';
    options: SelectPropertyOptionItemI[];
    propertyName: string;
    truncated?: boolean;
}

/**
 * Renders the selectPropertyOption / selectTriggerPropertyOption tool result as a searchable picker of ALL options the
 * tool fetched from the connection. The options come straight from the tool result (not re-emitted by the LLM), so the
 * full list is always shown. On pick, the option's real value is submitted as a system message
 * {@code "User picked: <label> (value: <value>)"} so the agent writes the value (e.g. a channel id) into the workflow.
 * Surface-agnostic: depends only on the assistant-ui thread runtime + the data prop, so it can later move to a shared
 * module reused by Copilot.
 */
const AiHubSelectPropertyOptionMessage = ({data}: DataMessagePartProps<SelectPropertyOptionDataI>) => {
    const [picked, setPicked] = useState<SelectPropertyOptionItemI | undefined>();
    const [superseded, setSuperseded] = useState(false);

    const threadRuntime = useThreadRuntime();

    useEffect(() => {
        const initialCount = threadRuntime.getState().messages.length;

        return threadRuntime.subscribe(() => {
            if (threadRuntime.getState().messages.length > initialCount) {
                setSuperseded(true);
            }
        });
    }, [threadRuntime]);

    if (picked) {
        return (
            <div className="mt-2 flex items-center gap-2 text-sm">
                <CheckIcon className="size-4 text-emerald-600" />

                <span>
                    Picked: <span className="font-medium">{picked.label}</span>
                </span>
            </div>
        );
    }

    const options = data.options ?? [];

    if (options.length === 0) {
        return (
            <div className="mt-2 rounded-md border border-border bg-muted/30 p-3 text-sm text-muted-foreground">
                No options available for {data.propertyName}.
            </div>
        );
    }

    const items = options.map((option) => ({label: option.label, value: option.value}));

    return (
        <div className={`mt-2 flex w-full min-w-0 flex-col gap-1${superseded ? ' opacity-60' : ''}`}>
            <ComboBox
                emptyMessage="No match"
                items={items}
                onChange={(item) => {
                    if (!item) {
                        return;
                    }

                    const option = options.find((candidate) => candidate.value === item.value);

                    if (!option) {
                        return;
                    }

                    setPicked(option);

                    threadRuntime.append({
                        content: [{text: `User picked: ${option.label} (value: ${option.value})`, type: 'text'}],
                        role: 'system',
                    });
                }}
                value={undefined}
            />

            {data.truncated && (
                <span className="text-xs text-muted-foreground">
                    Showing the first {options.length}. Narrow with a search term if you don&apos;t see yours.
                </span>
            )}
        </div>
    );
};

export default AiHubSelectPropertyOptionMessage;
```

- [ ] **Step 4: Run the renderer test to verify pass**

Run (from `client/`): `npm run test -- src/pages/automation/ai-hub/messages/tests/AiHubSelectPropertyOptionMessage.test.tsx`
Expected: PASS.

- [ ] **Step 5: Wire the interceptor + registry**

(a) In `AiHubRuntimeProvider.tsx`, add a result interface near `SelectConnectionResultI`:
```typescript
interface SelectPropertyOptionResultI {
    componentName: string;
    kind: 'select-property-option';
    options: Array<{label: string; value: string}>;
    propertyName: string;
    truncated?: boolean;
}
```
Then add a branch after the `selectConnection` branch in the tool-result handler:
```typescript
            } else if (toolCallName === 'selectPropertyOption' || toolCallName === 'selectTriggerPropertyOption') {
                const parsed = parseJson<SelectPropertyOptionResultI>(event.content, 'selectPropertyOption result');

                if (!parsed || parsed.kind !== 'select-property-option' || !Array.isArray(parsed.options)) {
                    const errorMessage = !parsed
                        ? 'selectPropertyOption returned an unparseable payload'
                        : 'selectPropertyOption returned a malformed payload (missing kind or options)';

                    aiHubToolCallStore.getState().completeToolCall(event.toolCallId, {error: errorMessage}, true);

                    aiHubRetryableErrorStore.getState().setError({
                        errorMessage,
                        lastUserMessage: getLastUserMessage(),
                        toolName: toolCallName,
                    });

                    return;
                }

                addMessage({
                    content: [
                        {
                            data: {
                                componentName: parsed.componentName,
                                kind: parsed.kind,
                                options: parsed.options,
                                propertyName: parsed.propertyName,
                                truncated: parsed.truncated,
                            },
                            type: 'data-select-property-option',
                        },
                    ],
                    role: 'assistant',
                });
            }
```
(Match the exact `addMessage`/`parseJson`/store call shape of the adjacent `selectConnection` branch — names like `aiHubToolCallStore`, `aiHubRetryableErrorStore`, `getLastUserMessage`, `addMessage` are already in scope there.)

(b) In `AiHubMessageContent.tsx`, add the import + wrapper + registry entry mirroring `SelectConnectionData`:
```typescript
import AiHubSelectPropertyOptionMessage, {
    SelectPropertyOptionDataI,
} from '@/pages/automation/ai-hub/messages/AiHubSelectPropertyOptionMessage';
```
```typescript
const SelectPropertyOptionData = (props: DataMessagePartProps<SelectPropertyOptionDataI>) => (
    <AiHubSelectPropertyOptionMessage {...props} />
);
```
Add to `data.by_name` (alphabetical): `'select-property-option': SelectPropertyOptionData,`.

- [ ] **Step 6: Lint/typecheck, run message tests, commit**

Run (from `client/`):
```bash
npx eslint src/pages/automation/ai-hub/messages/AiHubSelectPropertyOptionMessage.tsx \
  src/pages/automation/ai-hub/messages/AiHubMessageContent.tsx \
  src/pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider.tsx --max-warnings=0
npx tsc --project tsconfig.json --noEmit
npm run test -- src/pages/automation/ai-hub/messages/tests/AiHubSelectPropertyOptionMessage.test.tsx
```
Expected: all clean/pass. Fix any sort-keys/import-order in the touched files (manual). Then:
```bash
git add client/src/pages/automation/ai-hub/messages/AiHubSelectPropertyOptionMessage.tsx \
        client/src/pages/automation/ai-hub/messages/tests/AiHubSelectPropertyOptionMessage.test.tsx \
        client/src/pages/automation/ai-hub/messages/AiHubMessageContent.tsx \
        client/src/pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider.tsx
git commit -m "5169 client - Render selectPropertyOption tool result as a searchable picker"
```

---

## Task 8: Verification

- [ ] **Step 1: Server** — `./gradlew spotlessApply :server:ee:libs:ai:ai-hub:ai-hub-service:check` → BUILD SUCCESSFUL. If Spotless reformats touched files, stage them in a NEW commit (do NOT `--amend`): `git commit -m "0_732 Apply spotless formatting for selectPropertyOption"`. If SpotBugs flags `EI_EXPOSE` on a new record carrying a `Map`, the defensive-copy compact constructor (already in the input records) covers it; for any new finding, follow the module's existing `@SuppressFBWarnings`/defensive-copy precedent.
- [ ] **Step 2: Client** — `npx eslint src --max-warnings=0`, `npx tsc --project tsconfig.json --noEmit`, `npx vitest run` (the project's `npm run check` may also be blocked by 3 PRE-EXISTING prettier-drift files unrelated to this work — `AiHubTasksSidebar.tsx`, `ClusterElementsWorkflowEditor.tsx`, `AppSidebarFooter.test.tsx`; do not fix those). Confirm the new/touched files are prettier-clean: `npx prettier --check client/src/pages/automation/ai-hub/messages/AiHubSelectPropertyOptionMessage.tsx ...`.
- [ ] **Step 3:** Final formatting commit only if needed (NEW commit, never amend).

---

## Self-Review

**Spec coverage:** Shared `resolveAction/TriggerPropertyOptions` + sealed result → Task 1; lookup callbacks delegate (DRY, behavior unchanged) → Task 2; `selectPropertyOption` (action) → Task 3; `selectTriggerPropertyOption` (trigger) → Task 4; registration in both catalogs + wiring test → Task 5; prompt → Task 6; client interceptor (both tool names → one `data-select-property-option`) + renderer (searchable ComboBox of ALL options, submits `value`) + registry → Task 7; verification → Task 8. ✓ Marker is surface-agnostic (omits action/trigger name) and renderer depends only on `threadRuntime` + `data` (extraction-friendly per spec). ✓

**Placeholder scan:** No TBD/TODO. Task 4 says "copy Task 3 verbatim and substitute" with the exact substitution list — acceptable because Task 3's full code is present and the substitutions are enumerated; the trigger types/method are all named. The buildMarker double-serialize correction is called out explicitly. ✓

**Type consistency:** `OptionsLookupResult.Failure(envelope, metricTag)` / `Success(options, truncated)` defined in Task 1, used identically in Tasks 2–4. `resolveActionPropertyOptions`/`resolveTriggerPropertyOptions` signatures match between definition (Task 1) and callers (Tasks 2–4). Marker keys `{kind, componentName, propertyName, options:[{label,value}], truncated}` match between server (Tasks 3–4), interceptor (Task 7a), and renderer `SelectPropertyOptionDataI` (Task 7). `kind === 'select-property-option'` consistent server↔client. ✓
