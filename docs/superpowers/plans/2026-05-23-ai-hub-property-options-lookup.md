# AI Hub Property Options Lookup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stop AI Hub from writing literal natural-language values (e.g. `"channel": "standup"`) into workflow properties that require IDs resolved from the user's connection. Add `lookupRequired`/`lookupDependsOn` metadata to property schemas, two new tool callbacks (`lookupActionPropertyOptions`, `lookupTriggerPropertyOptions`) that wrap `executeOptions`, and a system-prompt rule that pins strict semantics.

**Architecture:** Three coordinated changes, all additive: (1) extend `ToolUtils.generateParametersJson` in `mcp-tool-platform` to emit `lookupRequired: true` + `lookupDependsOn: [...]` for any property implementing `OptionsDataSourceAware` with a non-null `OptionsDataSource`; (2) two `ToolCallback` classes in `automation-ai-hub-service` (with a shared `PropertyOptionsResolver` helper) that wrap the `ActionDefinitionFacade.executeOptions` / `TriggerDefinitionFacade.executeOptions` facades and return structured envelopes for connection/dependency/runtime errors; (3) register both on the workflow_builder subagent's `ChatClient` and add the rule to `prompt_workflow_builder.txt`.

**Tech Stack:** Java 25, Spring Boot 4.0, Spring AI (ToolCallback, ToolDefinition), Jackson (tools.jackson.databind), JUnit 5, Mockito, Micrometer.

**Related spec:** `docs/superpowers/specs/2026-05-23-ai-hub-property-options-lookup-design.md`

---

## File structure

**New files:**

- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/PropertyOptionsResolver.java` — shared helper for SecurityContext rehydration and envelope building
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/LookupActionPropertyOptionsToolCallback.java`
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/LookupTriggerPropertyOptionsToolCallback.java`
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/tool/LookupActionPropertyOptionsToolCallbackTest.java`
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/tool/LookupTriggerPropertyOptionsToolCallbackTest.java`
- `server/libs/ai/mcp/mcp-tool/mcp-tool-platform/src/test/java/com/bytechef/ai/mcp/tool/platform/util/ToolUtilsLookupMetadataTest.java`

**Modified files:**

- `server/libs/ai/mcp/mcp-tool/mcp-tool-platform/src/main/java/com/bytechef/ai/mcp/tool/platform/util/ToolUtils.java` (and possibly `PropertyDecorator` — Task 1 investigates)
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/config/WorkflowBuilderConfiguration.java`
- `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_workflow_builder.txt`

---

## Task 1: Investigate property JSON emit point

**Why this is task 1, not part of task 2:** The spec says "extend `ToolUtils.generateParametersJson`" but the actual emit of per-property fields happens inside `PropertyDecorator` (referenced at `ToolUtils.java:183`). Before writing test/impl, confirm exactly which class produces the property JSON object that includes `type`, `title`, `description`, `required`.

**Files:**
- Read: `server/libs/ai/mcp/mcp-tool/mcp-tool-platform/src/main/java/com/bytechef/ai/mcp/tool/platform/util/ToolUtils.java`
- Read: `server/libs/ai/mcp/mcp-tool/mcp-tool-platform/src/main/java/com/bytechef/ai/mcp/tool/platform/util/PropertyDecorator.java` (or wherever it lives — find via `find ... -name "PropertyDecorator.java"`)
- Read: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/domain/OptionsDataSource.java`
- Read: One implementer of `OptionsDataSourceAware`, e.g. `StringProperty.java` in the same domain package

- [ ] **Step 1: Locate PropertyDecorator**

```bash
find /Volumes/Data/bytechef/bytechef/server -name "PropertyDecorator.java" -not -path "*/build/*"
```
Expected: one or two file paths. The one used by `ToolUtils.generateParametersJson` is the target.

- [ ] **Step 2: Read PropertyDecorator and confirm the per-property emit method**

Look for the method that produces a per-property JSON fragment that already includes `"type":`, `"title":`, `"description":`. The emit may go through `generateObjectValue(...)` in `ToolUtils.java` (called at line 185), which calls into the decorator. Confirm:
- Does `PropertyDecorator` wrap the underlying `BaseProperty`? If yes, does it expose `getOptionsDataSource()` or do you need to downcast?
- Where exactly does the JSON for one property get assembled? Is it `PropertyDecorator#toJson()` or a helper in `ToolUtils`?

- [ ] **Step 3: Document the emit point (no code change yet)**

Write down (in this plan, or in a scratch note) the exact method that builds one property's JSON. Use that location in Task 2 below. If the emit happens in a method named differently than expected, replace `ToolUtils.generateParametersJson` references in Task 2 with the actual method name.

- [ ] **Step 4: Confirm `BaseProperty` exposes `getOptionsDataSource()`**

```bash
grep -n "getOptionsDataSource\|OptionsDataSourceAware" /Volumes/Data/bytechef/bytechef/server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/domain/BaseProperty.java
```
Expected: either a method on `BaseProperty` directly, OR (more likely) the property must be downcast to its concrete type (e.g. `StringProperty`) to access `getOptionsDataSource()`. If downcast is required, Task 2 uses `if (property instanceof OptionsDataSourceAware aware) { ... }`.

Commit nothing — this is investigation only.

---

## Task 2: Add lookupRequired and lookupDependsOn to property JSON serialization

**Files:**
- Modify: `server/libs/ai/mcp/mcp-tool/mcp-tool-platform/src/main/java/com/bytechef/ai/mcp/tool/platform/util/ToolUtils.java` AND/OR `PropertyDecorator.java` (per Task 1's finding)
- Create: `server/libs/ai/mcp/mcp-tool/mcp-tool-platform/src/test/java/com/bytechef/ai/mcp/tool/platform/util/ToolUtilsLookupMetadataTest.java`

- [ ] **Step 1: Write a failing test for the simplest case (property with OptionsDataSource, no dependencies)**

Create `ToolUtilsLookupMetadataTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.mcp.tool.platform.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.StringProperty;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToolUtilsLookupMetadataTest {

    @Test
    void emitsLookupRequiredWhenPropertyHasOptionsDataSource() {
        ActionOptionsFunction<String> optionsFunction = (params, conn, lookups, search, ctx) -> List.of();

        com.bytechef.component.definition.Property.StringProperty source = ComponentDsl.string("channel")
            .options(optionsFunction)
            .build();

        StringProperty property = new StringProperty(source);

        String json = ToolUtils.generateParametersJson(List.<Property>of(property));

        assertTrue(json.contains("\"lookupRequired\": true"),
            "Expected lookupRequired:true for property with OptionsDataSource. Got: " + json);
        assertTrue(json.contains("\"lookupDependsOn\": []"),
            "Expected lookupDependsOn:[] when no dependencies declared. Got: " + json);
    }

    @Test
    void omitsLookupFieldsWhenPropertyHasNoOptionsDataSource() {
        com.bytechef.component.definition.Property.StringProperty source = ComponentDsl.string("text")
            .build();

        StringProperty property = new StringProperty(source);

        String json = ToolUtils.generateParametersJson(List.<Property>of(property));

        assertFalse(json.contains("lookupRequired"),
            "Expected no lookupRequired field for plain property. Got: " + json);
        assertFalse(json.contains("lookupDependsOn"),
            "Expected no lookupDependsOn field for plain property. Got: " + json);
    }
}
```

Note: the exact `ComponentDsl.string(...).options(...)` API may differ — adjust per the actual `ComponentDsl` interface. If the DSL builder doesn't have `.options(ActionOptionsFunction)`, use the variant that does (check `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ComponentDsl.java` for `string()` builder).

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :server:libs:ai:mcp:mcp-tool:mcp-tool-platform:test --tests "com.bytechef.ai.mcp.tool.platform.util.ToolUtilsLookupMetadataTest" -i
```
Expected: FAIL — the assertion `json.contains("\"lookupRequired\": true")` is false because the current serialization doesn't emit those fields.

- [ ] **Step 3: Modify the property emit point to include the new fields**

In the per-property emit method identified in Task 1, after the existing `"required":` field emission, add:

```java
// Inside the per-property JSON emission, after existing fields are written:
if (property instanceof OptionsDataSourceAware aware && aware.getOptionsDataSource() != null) {
    parameters.append(", \"lookupRequired\": true");

    List<String> dependsOn = aware.getOptionsDataSource().getOptionsLookupDependsOn();

    parameters.append(", \"lookupDependsOn\": [");

    for (int i = 0; i < dependsOn.size(); i++) {
        if (i > 0) {
            parameters.append(", ");
        }

        parameters.append("\"")
            .append(dependsOn.get(i))
            .append("\"");
    }

    parameters.append("]");
}
```

Important caveats:
- `OptionsDataSourceAware` is in `com.bytechef.platform.component.domain` — add the import.
- The exact field-append idiom must match the surrounding code style (some methods build JSON via `StringBuilder`, some use Jackson — match what's there).
- If the property is iterated as `PropertyDecorator`, you may need to expose `getOptionsDataSource()` on the decorator (or pull the original `BaseProperty` via a getter on the decorator).

- [ ] **Step 4: Run test to verify the simple case passes**

```bash
./gradlew :server:libs:ai:mcp:mcp-tool:mcp-tool-platform:test --tests "com.bytechef.ai.mcp.tool.platform.util.ToolUtilsLookupMetadataTest.emitsLookupRequiredWhenPropertyHasOptionsDataSource" -i
```
Expected: PASS

- [ ] **Step 5: Run the omission test to verify no regression**

```bash
./gradlew :server:libs:ai:mcp:mcp-tool:mcp-tool-platform:test --tests "com.bytechef.ai.mcp.tool.platform.util.ToolUtilsLookupMetadataTest.omitsLookupFieldsWhenPropertyHasNoOptionsDataSource" -i
```
Expected: PASS

- [ ] **Step 6: Add a test for populated lookupDependsOn**

Append to `ToolUtilsLookupMetadataTest`:

```java
@Test
void emitsLookupDependsOnWhenDataSourceDeclaresDependencies() {
    ActionOptionsFunction<String> optionsFunction = (params, conn, lookups, search, ctx) -> List.of();

    com.bytechef.component.definition.Property.StringProperty source = ComponentDsl.string("sheet")
        .options(optionsFunction, "spreadsheetId", "sheetTab")
        .build();

    StringProperty property = new StringProperty(source);

    String json = ToolUtils.generateParametersJson(List.<Property>of(property));

    assertTrue(json.contains("\"lookupDependsOn\": [\"spreadsheetId\", \"sheetTab\"]"),
        "Expected lookupDependsOn list. Got: " + json);
}
```

Note: `.options(function, "dep1", "dep2")` is the assumed DSL for declaring lookup deps — verify in `ComponentDsl` and adjust. The Slack action at `SlackSendChannelMessageAction.java:49` uses `.options((OptionsFunction<String>) SlackUtils::getChannelIdOptions)` with no deps, so a deps-bearing example may need a different component.

- [ ] **Step 7: Run all three tests**

```bash
./gradlew :server:libs:ai:mcp:mcp-tool:mcp-tool-platform:test --tests "com.bytechef.ai.mcp.tool.platform.util.ToolUtilsLookupMetadataTest" -i
```
Expected: 3 tests PASS.

- [ ] **Step 8: Run the existing module tests to confirm no regression**

```bash
./gradlew :server:libs:ai:mcp:mcp-tool:mcp-tool-platform:test
```
Expected: all tests PASS.

- [ ] **Step 9: Run spotless and commit**

```bash
./gradlew :server:libs:ai:mcp:mcp-tool:mcp-tool-platform:spotlessApply
git add server/libs/ai/mcp/mcp-tool/mcp-tool-platform/src/main/java server/libs/ai/mcp/mcp-tool/mcp-tool-platform/src/test/java
git commit -m "$(printf 'Add lookupRequired and lookupDependsOn to property JSON\n\nProperty JSON emitted by ToolUtils.generateParametersJson now includes\nlookupRequired:true and lookupDependsOn:[...] for any property whose\nruntime type implements OptionsDataSourceAware with a non-null\nOptionsDataSource. Additive change — properties without dynamic\noptions are unaffected. Foundation for AI Hub property options lookup\n(spec: 2026-05-23-ai-hub-property-options-lookup-design.md).\n\nCo-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>')"
```

---

## Task 3: Create PropertyOptionsResolver shared helper

This carries the logic shared between the action and trigger tool callbacks: SecurityContext rehydration, error envelope construction, success envelope assembly.

**Files:**
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/PropertyOptionsResolver.java`

- [ ] **Step 1: Create the helper class**

Write the file:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.aihub.tool;

import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Shared helper for the two property-options lookup tool callbacks (action and trigger). Owns the
 * SecurityContext rehydration pattern (Reactor scheduler threads don't inherit the request principal)
 * and the success/error envelope shape so the two callbacks stay in lockstep.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class PropertyOptionsResolver {

    private static final Logger logger = LoggerFactory.getLogger(PropertyOptionsResolver.class);

    private final UserService userService;
    private final AuthorityService authorityService;

    public PropertyOptionsResolver(UserService userService, AuthorityService authorityService) {
        this.userService = userService;
        this.authorityService = authorityService;
    }

    /**
     * Executes {@code action} inside a temporary {@link org.springframework.security.core.context.SecurityContext}
     * resolved from {@code userId}. Falls through (runs without rehydration) when {@code userId} is null or the user
     * record can't be loaded — matches the same fall-through path as
     * {@code ListConnectionsForComponentToolCallback#withUserSecurityContext}.
     */
    public <T> T withUserSecurityContext(@Nullable Long userId, Supplier<T> action) {
        if (userId == null) {
            return action.get();
        }

        Optional<User> userOptional = userService.fetchUser(userId);

        if (userOptional.isEmpty()) {
            logger.debug("Skipping SecurityContext rehydration: user id {} not found", userId);

            return action.get();
        }

        User user = userOptional.get();

        List<GrantedAuthority> authorities = new ArrayList<>();

        for (Long authorityId : user.getAuthorityIds()) {
            Optional<Authority> authorityOptional = authorityService.fetchAuthority(authorityId);

            authorityOptional.map(Authority::getName)
                .map(SimpleGrantedAuthority::new)
                .ifPresent(authorities::add);
        }

        return SecurityUtils.runAs(user.getLogin(), authorities, action);
    }

    /**
     * Builds the success envelope shape pinned by the spec: {@code {componentName, actionName|triggerName, propertyName, options:[{label,value}]}}.
     * Caller passes the right key for the action/trigger discriminator.
     */
    public Map<String, Object> buildSuccessEnvelope(
        String componentName, String entityKey, String entityName, String propertyName, List<Option> options) {

        List<Map<String, Object>> serialized = new ArrayList<>(options.size());

        for (Option option : options) {
            Map<String, Object> row = new LinkedHashMap<>();

            row.put("label", option.getLabel());
            row.put("value", option.getValue());

            serialized.add(row);
        }

        Map<String, Object> envelope = new LinkedHashMap<>();

        envelope.put("componentName", componentName);
        envelope.put(entityKey, entityName);
        envelope.put("propertyName", propertyName);
        envelope.put("options", serialized);

        return envelope;
    }

    /**
     * Returns the structured error envelope for the missing-connection case.
     */
    public Map<String, Object> connectionRequiredEnvelope(String componentName) {
        Map<String, Object> envelope = new LinkedHashMap<>();

        envelope.put("error", "connection_required");
        envelope.put("componentName", componentName);
        envelope.put("hint",
            "No connectionId supplied. Call listConnectionsForComponent for '" + componentName +
                "' to pick an existing one, or createConnection to make a new one, then retry.");

        return envelope;
    }

    /**
     * Returns the structured error envelope for the missing-dependency case.
     */
    public Map<String, Object> dependencyMissingEnvelope(List<String> missing) {
        Map<String, Object> envelope = new LinkedHashMap<>();

        envelope.put("error", "dependency_missing");
        envelope.put("missing", missing);
        envelope.put("hint",
            "Place values for these siblings first and include them in inputParameters, then retry.");

        return envelope;
    }

    /**
     * Returns the structured error envelope when the LLM called the tool for a property that has no
     * {@code OptionsDataSource}.
     */
    public Map<String, Object> noOptionsForPropertyEnvelope() {
        Map<String, Object> envelope = new LinkedHashMap<>();

        envelope.put("error", "no_options_for_property");
        envelope.put("hint",
            "This property does not have dynamic options. Set the value directly per the property's description.");

        return envelope;
    }
}
```

- [ ] **Step 2: Compile to verify imports resolve**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:compileJava
```
Expected: BUILD SUCCESSFUL. If imports of `Option`, `SecurityUtils`, `UserService`, `AuthorityService` fail, the module is missing their dependencies — add to `build.gradle.kts` (cross-reference what `ListConnectionsForComponentToolCallback` already imports).

- [ ] **Step 3: Run spotless and commit**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:spotlessApply
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/PropertyOptionsResolver.java
git commit -m "$(printf 'Add PropertyOptionsResolver helper for AI Hub options lookup tools\n\nShared logic between the upcoming LookupAction\/Trigger\nPropertyOptionsToolCallback classes: SecurityContext rehydration\n(matching ListConnectionsForComponentToolCallback#withUserSecurityContext),\nsuccess envelope assembly, and structured error envelopes for the\nconnection_required \/ dependency_missing \/ no_options_for_property cases.\n\nCo-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>')"
```

---

## Task 4: Create LookupActionPropertyOptionsToolCallback with full test coverage

**Files:**
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/LookupActionPropertyOptionsToolCallback.java`
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/tool/LookupActionPropertyOptionsToolCallbackTest.java`

- [ ] **Step 1: Write the failing happy-path test**

Create `LookupActionPropertyOptionsToolCallbackTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.aihub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.aihub.metric.AiHubToolAttachMetrics;
import com.bytechef.ee.platform.aihub.tool.AiHubToolInvocationContext;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.json.JsonMapper;

class LookupActionPropertyOptionsToolCallbackTest {

    private ActionDefinitionFacade actionDefinitionFacade;
    private UserService userService;
    private AuthorityService authorityService;
    private AiHubToolAttachMetrics metrics;
    private JsonMapper jsonMapper;
    private LookupActionPropertyOptionsToolCallback toolCallback;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        actionDefinitionFacade = mock(ActionDefinitionFacade.class);
        userService = mock(UserService.class);
        authorityService = mock(AuthorityService.class);

        ObjectProvider<io.micrometer.core.instrument.MeterRegistry> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(new SimpleMeterRegistry());

        metrics = new AiHubToolAttachMetrics(provider);
        jsonMapper = new JsonMapper();

        PropertyOptionsResolver resolver = new PropertyOptionsResolver(userService, authorityService);

        toolCallback = new LookupActionPropertyOptionsToolCallback(
            actionDefinitionFacade, resolver, metrics, jsonMapper);
    }

    @Test
    void returnsOptionsEnvelopeOnHappyPath() {
        when(actionDefinitionFacade.executeOptions(
            eq("slack"), eq(1), eq("sendChannelMessage"), eq("channel"),
            any(), any(), eq("standup"), eq(7421L)))
            .thenReturn(List.of(
                new Option("standup", "C0123ABCD"),
                new Option("standup-eu", "C0987XYZW")));

        ToolContext toolContext = toolContextWithWorkspace(11L, 42L, 1L);

        String result = toolCallback.call(
            """
            {
              "componentName": "slack",
              "componentVersion": 1,
              "actionName": "sendChannelMessage",
              "propertyName": "channel",
              "connectionId": 7421,
              "searchText": "standup"
            }""",
            toolContext);

        assertThat(result).contains("\"componentName\":\"slack\"");
        assertThat(result).contains("\"actionName\":\"sendChannelMessage\"");
        assertThat(result).contains("\"propertyName\":\"channel\"");
        assertThat(result).contains("\"label\":\"standup\"");
        assertThat(result).contains("\"value\":\"C0123ABCD\"");
    }

    private ToolContext toolContextWithWorkspace(long workspaceId, long userId, long environmentId) {
        // Use whatever helper AiHubToolInvocationContext exposes for synthesising a ToolContext;
        // cross-reference ListConnectionsForComponentToolCallbackTest for the established pattern.
        // Pseudocode placeholder — replace with actual builder call.
        AiHubToolInvocationContext invocationContext =
            AiHubToolInvocationContext.builder()
                .workspaceId(workspaceId)
                .userId(userId)
                .environmentId(environmentId)
                .build();

        return invocationContext.toToolContext();
    }
}
```

Note: the exact `AiHubToolInvocationContext` builder/factory API may differ — cross-reference `ListConnectionsForComponentToolCallbackTest` (path: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/tool/ListConnectionsForComponentToolCallbackTest.java`) and copy its `ToolContext` synthesis verbatim into the helper above.

Note 2: `Option` constructor may take a 3-arg form (label, value, description); adapt to the actual signature in `com.bytechef.platform.component.domain.Option`.

- [ ] **Step 2: Run test to verify it fails (class doesn't exist yet)**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "com.bytechef.ee.automation.aihub.tool.LookupActionPropertyOptionsToolCallbackTest.returnsOptionsEnvelopeOnHappyPath" -i
```
Expected: FAIL — compile error, `LookupActionPropertyOptionsToolCallback` class does not exist.

- [ ] **Step 3: Create the tool callback class**

Write `LookupActionPropertyOptionsToolCallback.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.aihub.tool;

import com.bytechef.ee.platform.aihub.metric.AiHubToolAttachMetrics;
import com.bytechef.ee.platform.aihub.tool.AiHubToolInvocationContext;
import com.bytechef.ee.platform.aihub.util.LogSanitizer;
import com.bytechef.ee.platform.aihub.util.ToolErrors;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
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
 * Spring AI {@link ToolCallback} that resolves a dynamic-options property to its canonical value list. The LLM
 * calls this BEFORE writing a value into a property whose schema includes {@code "lookupRequired": true}; the
 * returned envelope contains {@code options:[{label, value}]} and the LLM must write {@code value} into the
 * workflow JSON, never {@code label}.
 *
 * <p>
 * See {@code docs/superpowers/specs/2026-05-23-ai-hub-property-options-lookup-design.md} for the full contract,
 * including the four structured error envelopes ({@code connection_required}, {@code dependency_missing},
 * {@code no_options_for_property}, {@code lookup_failed}).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class LookupActionPropertyOptionsToolCallback implements ToolCallback {

    static final String TOOL_NAME = "lookupActionPropertyOptions";

    private static final Logger logger = LoggerFactory.getLogger(LookupActionPropertyOptionsToolCallback.class);

    private static final String DESCRIPTION = """
        Resolve a dynamic-options action property to its canonical value list. Call this BEFORE writing a value
        into any action property whose schema includes "lookupRequired": true. Pass the connectionId resolved
        for this component (via listConnectionsForComponent or createConnection); pass already-chosen sibling
        property values in inputParameters when the schema lists lookupDependsOn. Returns {options:[{label,value}]}
        — write `value` into the workflow JSON, never `label`. On error returns a structured envelope with `error`
        and `hint` fields.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "componentName":    {"type": "string"},
                "componentVersion": {"type": "integer", "description": "Defaults to 1"},
                "actionName":       {"type": "string"},
                "propertyName":     {"type": "string", "description": "Dotted path for nested properties"},
                "connectionId":     {"type": "integer", "description": "Required when the action defines a connection"},
                "searchText":       {"type": "string", "description": "Optional user-supplied keyword"},
                "inputParameters":  {"type": "object",  "description": "Sibling property values for lookupDependsOn"}
            },
            "required": ["componentName", "actionName", "propertyName"]
        }""";

    private final ActionDefinitionFacade actionDefinitionFacade;
    private final PropertyOptionsResolver resolver;
    private final AiHubToolAttachMetrics metrics;
    private final JsonMapper jsonMapper;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public LookupActionPropertyOptionsToolCallback(
        ActionDefinitionFacade actionDefinitionFacade, PropertyOptionsResolver resolver,
        AiHubToolAttachMetrics metrics, JsonMapper jsonMapper) {

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
            LookupActionInput input = jsonMapper.readValue(toolInput, LookupActionInput.class);

            if (isBlank(input.componentName()) || isBlank(input.actionName()) || isBlank(input.propertyName())) {
                metrics.recordStateVisibility(TOOL_NAME, "error");

                return ToolErrors.toolError(jsonMapper,
                    "componentName, actionName, and propertyName are required and must not be blank");
            }

            AiHubToolInvocationContext invocationContext = AiHubToolInvocationContext.fromToolContext(toolContext);

            if (invocationContext == null || invocationContext.workspaceId() == null) {
                metrics.recordStateVisibility(TOOL_NAME, "error");

                return ToolErrors.toolError(jsonMapper, "Workspace context unavailable.");
            }

            int version = input.componentVersion() == null ? 1 : input.componentVersion();

            Map<String, ?> params = input.inputParameters() == null ? Map.of() : input.inputParameters();

            List<Option> options = resolver.withUserSecurityContext(
                invocationContext.userId(),
                () -> actionDefinitionFacade.executeOptions(
                    input.componentName(), version, input.actionName(), input.propertyName(),
                    params, List.of(), input.searchText(), input.connectionId()));

            metrics.recordStateVisibility(TOOL_NAME, options.isEmpty() ? "empty" : "success");

            return jsonMapper.writeValueAsString(
                resolver.buildSuccessEnvelope(
                    input.componentName(), "actionName", input.actionName(), input.propertyName(), options));
        } catch (JacksonException exception) {
            logger.warn("lookupActionPropertyOptions rejected malformed input: {}",
                LogSanitizer.sanitizeForLog(exception.getMessage()));

            metrics.recordStateVisibility(TOOL_NAME, "error");

            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            metrics.recordStateVisibility(TOOL_NAME, "error");

            return ToolErrors.runtimeFailure(
                jsonMapper, LookupActionPropertyOptionsToolCallback.class, TOOL_NAME, exception);
        }
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }

    public record LookupActionInput(
        String componentName,
        @Nullable Integer componentVersion,
        String actionName,
        String propertyName,
        @Nullable Long connectionId,
        @Nullable String searchText,
        @Nullable Map<String, Object> inputParameters) {
    }
}
```

**NOTE — connection_required and dependency_missing envelopes are deliberately NOT yet enforced** in this minimal first version. The happy path works; the structured precondition envelopes come in Step 6 below after the happy-path test is green.

- [ ] **Step 4: Run the happy-path test**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "com.bytechef.ee.automation.aihub.tool.LookupActionPropertyOptionsToolCallbackTest.returnsOptionsEnvelopeOnHappyPath" -i
```
Expected: PASS.

- [ ] **Step 5: Commit the happy-path slice**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:spotlessApply
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/LookupActionPropertyOptionsToolCallback.java server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/tool/LookupActionPropertyOptionsToolCallbackTest.java
git commit -m "$(printf 'Add LookupActionPropertyOptionsToolCallback happy path\n\nNew Spring AI ToolCallback wraps ActionDefinitionFacade.executeOptions\nand returns a structured success envelope the LLM consumes. This commit\ncovers the happy path only; precondition checks (connection_required,\ndependency_missing, no_options_for_property) follow in the next commits\nto keep slices small.\n\nCo-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>')"
```

- [ ] **Step 6: Add failing test for `connection_required` envelope**

Append to `LookupActionPropertyOptionsToolCallbackTest`:

```java
@Test
void returnsConnectionRequiredWhenActionNeedsConnectionAndConnectionIdMissing() {
    // Need to teach the tool which actions require a connection. The cleanest path is to query
    // ComponentDefinitionService or ActionDefinitionService for the action's connection-required flag.
    // For this test, configure the mock to indicate the slack/sendChannelMessage action requires a connection.

    // Pseudo-setup (adapt to whichever service the tool calls to check the connection requirement):
    // when(actionDefinitionService.requiresConnection("slack", 1, "sendChannelMessage")).thenReturn(true);

    ToolContext toolContext = toolContextWithWorkspace(11L, 42L, 1L);

    String result = toolCallback.call(
        """
        {
          "componentName": "slack",
          "componentVersion": 1,
          "actionName": "sendChannelMessage",
          "propertyName": "channel"
        }""",
        toolContext);

    assertThat(result).contains("\"error\":\"connection_required\"");
    assertThat(result).contains("\"componentName\":\"slack\"");
    assertThat(result).contains("listConnectionsForComponent");
}
```

- [ ] **Step 7: Run the test to verify it fails**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "com.bytechef.ee.automation.aihub.tool.LookupActionPropertyOptionsToolCallbackTest.returnsConnectionRequiredWhenActionNeedsConnectionAndConnectionIdMissing" -i
```
Expected: FAIL — the tool currently calls `executeOptions(... connectionId=null)` unconditionally instead of returning the envelope.

- [ ] **Step 8: Add the precondition check to the tool**

Inject the service that exposes the action's connection requirement (`ActionDefinitionService` or `ComponentDefinitionService` — find which one and pick whichever already has the data; cross-reference `ListConnectionsForComponentToolCallback` to see how `ConnectionDefinitionService` is used). Update the constructor to take it.

Inside `call(...)`, before invoking `executeOptions`, add:

```java
boolean actionDefinesConnection = actionDefinitionService.actionDefinesConnection(
    input.componentName(), version, input.actionName());

if (actionDefinesConnection && input.connectionId() == null) {
    metrics.recordStateVisibility(TOOL_NAME, "connection_required");

    return jsonMapper.writeValueAsString(resolver.connectionRequiredEnvelope(input.componentName()));
}
```

The method `actionDefinesConnection(componentName, version, actionName)` may not exist on `ActionDefinitionService` — if not, the cleanest add is a new method there that returns `true` when the action's owning component declares a connection definition. Add the method in a separate atomic commit before continuing.

- [ ] **Step 9: Run the test to verify it passes**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "com.bytechef.ee.automation.aihub.tool.LookupActionPropertyOptionsToolCallbackTest.returnsConnectionRequiredWhenActionNeedsConnectionAndConnectionIdMissing" -i
```
Expected: PASS.

- [ ] **Step 10: Commit the connection-required slice**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:spotlessApply
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/LookupActionPropertyOptionsToolCallback.java server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/tool/LookupActionPropertyOptionsToolCallbackTest.java
git commit -m "$(printf 'Enforce connection_required precondition in lookupActionPropertyOptions\n\nWhen the action defines a connection and the LLM did not supply\nconnectionId, return the structured connection_required envelope\ninstead of calling the facade with null. Drives the agent to call\nlistConnectionsForComponent or createConnection first.\n\nCo-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>')"
```

- [ ] **Step 11: Add `dependency_missing` envelope test and implementation**

Add test:

```java
@Test
void returnsDependencyMissingWhenLookupDependsOnNotSatisfied() {
    // Configure the mock so the property's OptionsDataSource declares lookupDependsOn:["spreadsheetId"].
    // The exact mock surface depends on which service the tool consults; cross-reference
    // ActionDefinitionService for a method that returns the property's lookupDependsOn list.

    ToolContext toolContext = toolContextWithWorkspace(11L, 42L, 1L);

    String result = toolCallback.call(
        """
        {
          "componentName": "googleSheets",
          "componentVersion": 1,
          "actionName": "appendRow",
          "propertyName": "sheet",
          "connectionId": 7421,
          "inputParameters": {}
        }""",
        toolContext);

    assertThat(result).contains("\"error\":\"dependency_missing\"");
    assertThat(result).contains("\"missing\":[\"spreadsheetId\"]");
}
```

Add to the tool's `call(...)` (after the connection check, before `executeOptions`):

```java
List<String> dependsOn = actionDefinitionService.getPropertyLookupDependsOn(
    input.componentName(), version, input.actionName(), input.propertyName());

if (!dependsOn.isEmpty()) {
    List<String> missing = new ArrayList<>();

    for (String path : dependsOn) {
        if (!params.containsKey(path)) {
            missing.add(path);
        }
    }

    if (!missing.isEmpty()) {
        metrics.recordStateVisibility(TOOL_NAME, "dependency_missing");

        return jsonMapper.writeValueAsString(resolver.dependencyMissingEnvelope(missing));
    }
}
```

Note: `getPropertyLookupDependsOn(...)` may not exist on `ActionDefinitionService` — add it (returning the property's `OptionsDataSource.getOptionsLookupDependsOn()` or `List.of()` when the property has no data source). Add the service method as a separate atomic commit before this step.

Also: the `executeOptions` call's `lookupDependsOnPaths` argument (currently `List.of()` in the happy path) should be replaced with `dependsOn` once you have it:

```java
() -> actionDefinitionFacade.executeOptions(
    input.componentName(), version, input.actionName(), input.propertyName(),
    params, dependsOn, input.searchText(), input.connectionId())
```

- [ ] **Step 12: Run, verify pass, commit**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "com.bytechef.ee.automation.aihub.tool.LookupActionPropertyOptionsToolCallbackTest" -i
```
Expected: all tests PASS.

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:spotlessApply
git add -A server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src
git commit -m "$(printf 'Enforce dependency_missing precondition in lookupActionPropertyOptions\n\nWhen the property declares lookupDependsOn and any required sibling\nvalue is absent from inputParameters, return the structured\ndependency_missing envelope. Passes the resolved deps as\nlookupDependsOnPaths to the facade so server-side resolution sees them.\n\nCo-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>')"
```

- [ ] **Step 13: Add `no_options_for_property` envelope test and implementation**

Add test:

```java
@Test
void returnsNoOptionsForPropertyWhenLlmCallsForNonLookupProperty() {
    // Configure mock so the property does NOT have an OptionsDataSource.
    // ActionDefinitionService.propertyHasOptionsDataSource("slack", 1, "sendChannelMessage", "text") -> false

    ToolContext toolContext = toolContextWithWorkspace(11L, 42L, 1L);

    String result = toolCallback.call(
        """
        {
          "componentName": "slack",
          "componentVersion": 1,
          "actionName": "sendChannelMessage",
          "propertyName": "text",
          "connectionId": 7421
        }""",
        toolContext);

    assertThat(result).contains("\"error\":\"no_options_for_property\"");
}
```

Add a check in `call(...)` early in the flow (after the connection check):

```java
if (!actionDefinitionService.propertyHasOptionsDataSource(
    input.componentName(), version, input.actionName(), input.propertyName())) {

    metrics.recordStateVisibility(TOOL_NAME, "no_options_for_property");

    return jsonMapper.writeValueAsString(resolver.noOptionsForPropertyEnvelope());
}
```

Add the service method `propertyHasOptionsDataSource(...)` as a separate atomic commit.

- [ ] **Step 14: Verify, commit**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "com.bytechef.ee.automation.aihub.tool.LookupActionPropertyOptionsToolCallbackTest" -i
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:spotlessApply
git add -A server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src server/libs/platform/platform-component/platform-component-service/src
git commit -m "$(printf 'Enforce no_options_for_property envelope in lookupActionPropertyOptions\n\nWhen the LLM calls the tool on a property that has no OptionsDataSource,\nreturn the structured no_options_for_property envelope guiding the\nagent to set the value directly per the property description.\n\nCo-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>')"
```

- [ ] **Step 15: Add runtime-failure envelope test**

```java
@Test
void returnsRuntimeFailureEnvelopeWhenFacadeThrows() {
    when(actionDefinitionFacade.executeOptions(anyString(), anyInt(), anyString(), anyString(),
        any(), any(), anyString(), any()))
        .thenThrow(new RuntimeException("Slack API returned 429: rate_limited"));

    ToolContext toolContext = toolContextWithWorkspace(11L, 42L, 1L);

    String result = toolCallback.call(
        """
        {
          "componentName": "slack",
          "componentVersion": 1,
          "actionName": "sendChannelMessage",
          "propertyName": "channel",
          "connectionId": 7421,
          "searchText": "standup"
        }""",
        toolContext);

    assertThat(result).contains("\"error\"");
    assertThat(result).contains("rate_limited");
}
```

Existing `catch (RuntimeException exception) { return ToolErrors.runtimeFailure(...); }` handles this — no impl change needed.

- [ ] **Step 16: Verify and commit the runtime-failure test**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "com.bytechef.ee.automation.aihub.tool.LookupActionPropertyOptionsToolCallbackTest" -i
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test
git commit -m "$(printf 'Add runtime-failure coverage to lookupActionPropertyOptions tests\n\nVerifies the existing ToolErrors.runtimeFailure envelope is emitted\nwhen the component facade throws. No impl change — catches the\nexisting code path.\n\nCo-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>')"
```

---

## Task 5: Create LookupTriggerPropertyOptionsToolCallback (mirror of Task 4)

The structure is identical to Task 4. Trigger facade signature mirrors action (verified in spec). All envelope shapes are identical except `actionName` → `triggerName`.

**Files:**
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/LookupTriggerPropertyOptionsToolCallback.java`
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/tool/LookupTriggerPropertyOptionsToolCallbackTest.java`

- [ ] **Step 1: Copy the action callback, rename**

```bash
cp server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/LookupActionPropertyOptionsToolCallback.java server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/LookupTriggerPropertyOptionsToolCallback.java
```

- [ ] **Step 2: Edit the copy — replace action terms with trigger terms**

In the new file, replace (in order):
- Class name: `LookupActionPropertyOptionsToolCallback` → `LookupTriggerPropertyOptionsToolCallback`
- Tool name: `lookupActionPropertyOptions` → `lookupTriggerPropertyOptions`
- Logger class reference: same
- `ActionDefinitionFacade` → `TriggerDefinitionFacade`
- `actionDefinitionFacade` field → `triggerDefinitionFacade`
- `ActionDefinitionService` → `TriggerDefinitionService` (whichever the action variant uses)
- `actionDefinitionService` field → `triggerDefinitionService`
- Input record: `LookupActionInput` → `LookupTriggerInput`
- Input field: `actionName` → `triggerName`
- Envelope key passed to `resolver.buildSuccessEnvelope(...)`: `"actionName"` → `"triggerName"`
- Description text mentioning "action property" → "trigger property"
- Input schema `actionName` → `triggerName`

- [ ] **Step 3: Copy the action test, rename, edit**

```bash
cp server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/tool/LookupActionPropertyOptionsToolCallbackTest.java server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/tool/LookupTriggerPropertyOptionsToolCallbackTest.java
```

Apply the same renames in the test file. Replace component/action choice (`slack`/`sendChannelMessage`) with a trigger pairing (e.g. `slack`/`newMessage` if it exists; otherwise pick any trigger from `server/libs/modules/components/`).

- [ ] **Step 4: Compile and run all trigger tests**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "com.bytechef.ee.automation.aihub.tool.LookupTriggerPropertyOptionsToolCallbackTest" -i
```
Expected: all tests PASS.

- [ ] **Step 5: Commit**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:spotlessApply
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/LookupTriggerPropertyOptionsToolCallback.java server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/tool/LookupTriggerPropertyOptionsToolCallbackTest.java
git commit -m "$(printf 'Add LookupTriggerPropertyOptionsToolCallback\n\nMirror of LookupActionPropertyOptionsToolCallback for trigger property\nlookups. Same envelope shapes (action_name swapped for trigger_name),\nsame error envelopes, same SecurityContext rehydration via\nPropertyOptionsResolver, wraps TriggerDefinitionFacade.executeOptions.\n\nCo-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>')"
```

---

## Task 6: Register both tool callbacks on workflow_builder subagent + verify SecurityContext propagation

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/config/WorkflowBuilderConfiguration.java`
- Read: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/WorkflowBuilderToolCallback.java` (Task 1 already read the first 100 lines; read the rest to see how ToolContext is forwarded to the subagent)

- [ ] **Step 1: Read WorkflowBuilderToolCallback fully to check ToolContext forwarding**

```bash
sed -n '100,200p' server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/WorkflowBuilderToolCallback.java
```

Look at the `call(toolInput, toolContext)` body. Confirm whether the subagent's `ChatClient.prompt(...).user(...).call()` invocation propagates the parent's `ToolContext` (or `AiHubToolInvocationContext`) to the subagent's tool calls. If it doesn't propagate, the new ToolCallbacks will get `toolContext == null` when invoked by the subagent — `AiHubToolInvocationContext.fromToolContext(null)` returns null, and both tools will return the "Workspace context unavailable" error envelope.

- [ ] **Step 2: If propagation is missing, add it**

If `WorkflowBuilderToolCallback.call(...)` does not forward `toolContext` to the subagent's chat client, add the forwarding. The typical Spring AI pattern is `.prompt(...).toolContext(parentToolContext.getContext())` or `.prompt(...).toolContext(Map.of(...))`. Cross-reference `ResearchToolCallback` (mentioned in WorkflowBuilderToolCallback's Javadoc as "the same architecture") to see the established pattern.

If propagation IS already in place, no change is needed — skip to Step 4.

- [ ] **Step 3: If forwarding was added, commit it separately**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:spotlessApply
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/WorkflowBuilderToolCallback.java
git commit -m "$(printf 'Forward AiHubToolInvocationContext into workflow_builder subagent\n\nWithout this, ToolCallbacks invoked by the subagent (including the\nupcoming lookupActionPropertyOptions \/ lookupTriggerPropertyOptions)\nreceive a null ToolContext and cannot resolve the workspace, environment,\nor user id required for connection-scoped lookups.\n\nCo-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>')"
```

- [ ] **Step 4: Modify WorkflowBuilderConfiguration to register both new callbacks**

Update `workflowBuilderChatClient(...)` bean method to inject the two callbacks and chain them onto `defaultToolCallbacks`:

```java
@Bean
ChatClient workflowBuilderChatClient(
    ChatModel chatModel,
    ReadProjectTools readProjectTools,
    ReadProjectWorkflowTools readProjectWorkflowTools,
    ComponentTools componentTools,
    TaskTools taskTools,
    LookupActionPropertyOptionsToolCallback lookupActionPropertyOptionsToolCallback,
    LookupTriggerPropertyOptionsToolCallback lookupTriggerPropertyOptionsToolCallback,
    @Value("classpath:prompt_workflow_builder.txt") Resource promptResource) {

    String systemPrompt = readPrompt(promptResource);

    return ChatClient.builder(chatModel)
        .defaultSystem(systemPrompt)
        .defaultTools(readProjectTools, readProjectWorkflowTools, componentTools, taskTools)
        .defaultToolCallbacks(lookupActionPropertyOptionsToolCallback, lookupTriggerPropertyOptionsToolCallback)
        .build();
}
```

The two callbacks need to be Spring beans for autowiring. Add `@Component` to both callback classes (top of the class declaration, same level as `@version ee` Javadoc):

```java
@Component
public class LookupActionPropertyOptionsToolCallback implements ToolCallback {
```

Same for the trigger variant.

The `PropertyOptionsResolver` also needs to be a bean. Either:
- Add `@Component` to it (preferred — keeps wiring simple), or
- Make the callbacks construct it inline if you want to keep the helper stateless and non-managed.

If `@Component` is added to `PropertyOptionsResolver`, ensure `UserService` and `AuthorityService` are autowire-able from the `automation-ai-hub-service` module — they should be (already used by `ListConnectionsForComponentToolCallback`).

- [ ] **Step 5: Verify the module compiles and existing tests still pass**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:compileJava
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test
```
Expected: BUILD SUCCESSFUL; all tests PASS.

- [ ] **Step 6: Commit the registration**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:spotlessApply
git add -A server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src
git commit -m "$(printf 'Register lookup tool callbacks on workflow_builder subagent\n\nWorkflowBuilderConfiguration now wires LookupActionPropertyOptions\nToolCallback and LookupTriggerPropertyOptionsToolCallback onto the\nworkflow_builder subagent chat client via defaultToolCallbacks(...).\nBoth callbacks plus PropertyOptionsResolver are now @Component-managed\nSpring beans for autowiring.\n\nCo-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>')"
```

---

## Task 7: Add the system-prompt rule to prompt_workflow_builder.txt

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_workflow_builder.txt`

- [ ] **Step 1: Read the current prompt to find the right insertion point**

```bash
cat server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_workflow_builder.txt
```

Find the section that talks about property values or component-action selection. The rule should sit near where the prompt teaches the model how to fill properties — typically a section labelled something like "When setting property values" or "Filling in action parameters".

- [ ] **Step 2: Insert the rule**

Add (or amend) a section in the prompt:

```
### Resolving property values that require IDs

When a property's schema includes `"lookupRequired": true`, you MUST call
`lookupActionPropertyOptions` (or `lookupTriggerPropertyOptions` for trigger
properties) before writing a value. Never invent IDs. Never write the
user's natural-language description as the literal value for these
properties.

Steps for each `lookupRequired` property:

1. Confirm you have already resolved a `connectionId` for this component.
   If not, call `listConnectionsForComponent` first, then `createConnection`
   if none exist.
2. If the property's schema lists `lookupDependsOn`, set values for those
   sibling properties first (recursively applying this same rule), and
   include them in `inputParameters` when you call the lookup.
3. Call `lookupActionPropertyOptions` (or trigger variant) passing
   `componentName`, `actionName` / `triggerName`, `propertyName`,
   `connectionId`, the user's keyword as `searchText` if any, and
   `inputParameters` for dependencies.
4. Write the returned `value` (NOT `label`) into the workflow JSON.
5. If the lookup returns an empty list or multiple ambiguous candidates,
   ask the user to clarify — never guess.

The lookup tool may return structured error envelopes you must handle:

- `{"error": "connection_required", ...}` — call `listConnectionsForComponent`
  or `createConnection` first, then retry.
- `{"error": "dependency_missing", "missing": [...]}` — set values for the
  listed sibling properties first, then retry with them in `inputParameters`.
- `{"error": "no_options_for_property", ...}` — the property does not have
  dynamic options; set its value directly per the property's description.
- `{"error": "lookup_failed", "reason": "..."}` — surface the reason to the
  user; do not invent a value.
```

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_workflow_builder.txt
git commit -m "$(printf 'Add property options lookup rule to workflow_builder prompt\n\nPins the strict semantics for lookupRequired properties: must call\nlookupActionPropertyOptions \/ lookupTriggerPropertyOptions, write the\nreturned value (not label), recurse for lookupDependsOn siblings, ask\nthe user rather than guess on empty\/ambiguous results. Documents the\nfour structured error envelopes the tools may return.\n\nThis prompt is loaded by both ai-copilot-service and\nautomation-ai-hub-service via shared classpath; the rule applies to\nboth surfaces.\n\nCo-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>')"
```

---

## Task 8: Add new metrics outcomes to AiHubToolAttachMetrics docs

The existing `AiHubToolAttachMetrics.recordStateVisibility(tool, outcome)` accepts any outcome string, so the new outcomes (`connection_required`, `dependency_missing`, `no_options_for_property`) work without code changes. But the class Javadoc lists `success`, `empty`, `error` as the canonical set — update it so future readers know about the new outcomes.

**Files:**
- Modify: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/metric/AiHubToolAttachMetrics.java`

- [ ] **Step 1: Update the `recordStateVisibility` Javadoc**

In `AiHubToolAttachMetrics.java`, replace the `recordStateVisibility` Javadoc with:

```java
/**
 * Records a state-visibility callback invocation ({@code listTaskTools}, {@code listConnectionsForComponent},
 * {@code lookupActionPropertyOptions}, {@code lookupTriggerPropertyOptions}). Tag {@code tool} discriminates;
 * tag {@code outcome} is one of {@code success}, {@code empty}, {@code error}, plus the lookup-specific
 * outcomes {@code connection_required}, {@code dependency_missing}, {@code no_options_for_property}.
 * {@code empty} on {@code listConnectionsForComponent} is the signal that drives the agent to fall back to
 * {@code createConnection}, so a spike there indicates users connecting integrations for the first time.
 * Spikes in {@code connection_required} on the lookup tools indicate agents skipping the listConnections step.
 */
```

- [ ] **Step 2: Commit**

```bash
./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:spotlessApply
git add server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/metric/AiHubToolAttachMetrics.java
git commit -m "$(printf 'Document lookup-tool outcomes in AiHubToolAttachMetrics\n\nExtends the recordStateVisibility Javadoc to list the new outcome\nstrings emitted by lookupActionPropertyOptions and\nlookupTriggerPropertyOptions: connection_required, dependency_missing,\nno_options_for_property. The counter API itself is unchanged —\noutcome is a free-form tag.\n\nCo-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>')"
```

---

## Task 9: Run full module checks and the broader checks

- [ ] **Step 1: Run the touched module tests**

```bash
./gradlew :server:libs:ai:mcp:mcp-tool:mcp-tool-platform:check
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:check
./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:check
```
Expected: BUILD SUCCESSFUL for all three.

- [ ] **Step 2: Run spotless across the touched modules**

```bash
./gradlew :server:libs:ai:mcp:mcp-tool:mcp-tool-platform:spotlessApply :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:spotlessApply :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:spotlessApply
```

- [ ] **Step 3: Commit any spotless changes (typically none)**

```bash
git status --short
# If any files show, commit:
git add -A && git commit -m "Apply spotless formatting"
```

---

## Task 10: Manual smoke test

This validates the LLM actually exercises the new tools end-to-end. Cannot be automated reliably.

- [ ] **Step 1: Start the dev infrastructure and server**

```bash
cd server
docker compose -f docker-compose.dev.infra.yml up -d
cd ..
./gradlew -p server/apps/server-app bootRun
```

- [ ] **Step 2: In another terminal, start the client**

```bash
cd client
npm install
npm run dev
```

- [ ] **Step 3: Log in and prepare workspace**

- Open http://localhost:3000
- Log in as admin@localhost.com / admin
- Create or open a workspace
- Connect a Slack workspace containing at least one channel named `standup` (use the in-app Slack connection flow)

- [ ] **Step 4: Open AI Hub and prompt**

In AI Hub, send:

> Send a "good morning" message to the slack channel standup every day at 9am

- [ ] **Step 5: Verify the produced workflow**

- Inspect the generated workflow JSON.
- Verify `parameters.channel` is the Slack channel ID format (`C` followed by alphanumeric chars), NOT the literal `"standup"`.
- Verify the schedule trigger is set to fire daily at 9am.

- [ ] **Step 6: Repeat without a Slack connection**

- Delete the Slack connection in the workspace.
- Send the same prompt.
- Verify the agent surfaces a `createConnection` step before placing the channel value (it should not silently write `"channel": "standup"`).

- [ ] **Step 7: Repeat with ambiguity**

- Add a second Slack connection in a different Slack workspace that also has a `#standup` channel.
- Send the same prompt.
- Verify the agent asks the user to clarify which connection / channel to use, rather than picking one arbitrarily.

- [ ] **Step 8: Record observations in the spec's Risks section**

If any smoke test step fails:
- Append a note to `docs/superpowers/specs/2026-05-23-ai-hub-property-options-lookup-design.md` under Risks describing the failure mode.
- File a follow-up issue or task for the gap.

If all pass: tick this task off and move on.

---

## Self-review

Performed by the plan author:

**1. Spec coverage:**
- `lookupRequired` + `lookupDependsOn` in property JSON — Task 2 ✓
- `LookupActionPropertyOptionsToolCallback` — Task 4 ✓
- `LookupTriggerPropertyOptionsToolCallback` — Task 5 ✓
- `PropertyOptionsResolver` shared helper — Task 3 ✓
- Four error envelopes (connection_required, dependency_missing, no_options_for_property, lookup_failed) — Task 4 steps 6–16 ✓
- Connection-less action edge case — covered by `actionDefinesConnection` check in Task 4 step 8 (when false, skips the connection_required branch) — could use an explicit test; flagging as a small gap.
- Registration on workflow_builder subagent — Task 6 ✓
- System-prompt rule — Task 7 ✓
- Metrics outcomes — Task 8 ✓
- Smoke test — Task 10 ✓

**Gap surfaced:** No explicit unit test for the connection-less edge case. Adding a follow-up step inside Task 4:

- [ ] **Task 4 follow-up step: Add connection-less action edge-case test**

Append to `LookupActionPropertyOptionsToolCallbackTest`:

```java
@Test
void allowsLookupForConnectionLessActionWhenConnectionIdNull() {
    // Configure mock so the action does NOT define a connection.
    // when(actionDefinitionService.actionDefinesConnection("util", 1, "formatDate")).thenReturn(false);
    // when(actionDefinitionService.propertyHasOptionsDataSource("util", 1, "formatDate", "format")).thenReturn(true);

    when(actionDefinitionFacade.executeOptions(eq("util"), eq(1), eq("formatDate"), eq("format"),
        any(), any(), any(), eq(null)))
        .thenReturn(List.of(new Option("ISO 8601", "iso")));

    ToolContext toolContext = toolContextWithWorkspace(11L, 42L, 1L);

    String result = toolCallback.call(
        """
        {
          "componentName": "util",
          "actionName": "formatDate",
          "propertyName": "format"
        }""",
        toolContext);

    assertThat(result).contains("\"value\":\"iso\"");
    assertThat(result).doesNotContain("connection_required");
}
```

Run, verify pass, commit:

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "com.bytechef.ee.automation.aihub.tool.LookupActionPropertyOptionsToolCallbackTest.allowsLookupForConnectionLessActionWhenConnectionIdNull" -i
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test
git commit -m "$(printf 'Cover connection-less action edge case in lookupActionPropertyOptions tests\n\nVerifies a property whose owning action does NOT define a connection\nallows lookup with connectionId=null instead of returning the\nconnection_required envelope.\n\nCo-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>')"
```

**2. Placeholder scan:** No "TBD" / "TODO" / "implement later" / "add appropriate error handling" / "similar to Task N" in the plan body. The investigation step in Task 1 is explicit ("Document the emit point") and feeds Task 2, which has full code.

**3. Type consistency:**
- Tool name string is consistent: `lookupActionPropertyOptions` (action) and `lookupTriggerPropertyOptions` (trigger) used in tool name, description, prompt rule, and metrics tag.
- Envelope shape consistent: `{componentName, actionName|triggerName, propertyName, options:[{label,value}]}`.
- Error envelope keys consistent: `error`, `hint`, `componentName`, `missing` (only for `dependency_missing`).
- `PropertyOptionsResolver` method names match between definition (Task 3) and call sites (Task 4): `withUserSecurityContext`, `buildSuccessEnvelope`, `connectionRequiredEnvelope`, `dependencyMissingEnvelope`, `noOptionsForPropertyEnvelope`.

Plan is internally consistent.
