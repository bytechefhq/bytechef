# Embedded Copilot — Restrict Workflow Generation to Enabled Integrations — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the embedded AI copilot's `generateProjectWorkflow` path only surface enabled integrations (enabled `IntegrationInstanceConfiguration`s for the environment) plus connection-less built-in components when generating a workflow.

**Architecture:** The embedded facade resolves a complete allow-list of component names (`componentName`s of enabled IICs ∪ `componentName`s of connection-less components) and passes it to the copilot generator. The generator stores it in the per-run agent `State`; a new `toolContext()` override on `WorkflowEditorSpringAIAgent` copies it into Spring AI's `ToolContext`; and `TaskTools.listTasks`/`searchTasks` (the build agent's only component-discovery surface) apply a pure set-membership filter. The filter is opt-in by presence of the toolContext key, so every other caller is unaffected.

**Tech Stack:** Java 25, Spring Boot, Spring AI (`@Tool` / `ToolContext`), the `com.agui.*` agent framework, JUnit 5 + Mockito.

**Spec:** `docs/superpowers/specs/2026-05-30-embedded-copilot-enabled-integrations-filter-design.md`

---

## File Structure

| File | Responsibility | Change |
| --- | --- | --- |
| `server/libs/ai/mcp/mcp-tool/mcp-tool-platform/src/main/java/com/bytechef/ai/mcp/tool/platform/TaskTools.java` | Discovery chokepoint; holds the shared toolContext key constant and performs the allow-list filter | Modify |
| `server/libs/ai/mcp/mcp-tool/mcp-tool-platform/src/test/java/com/bytechef/ai/mcp/tool/platform/TaskToolsTest.java` | Unit test for the filter | Create |
| `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/util/CopilotToolContextUtils.java` | Pure State→ToolContext mapping (testable read side) | Create |
| `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/util/CopilotToolContextUtilsTest.java` | Unit test for the mapping | Create |
| `server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/service/CopilotWorkflowGenerator.java` | Interface: add `allowedComponentNames` param | Modify |
| `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/service/CopilotWorkflowGeneratorImpl.java` | Seed allow-list into per-run `State` | Modify |
| `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/agent/WorkflowEditorSpringAIAgent.java` | Override `toolContext()` to surface the allow-list | Modify |
| `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectFacadeImpl.java` | Resolve the allow-list and pass it to the generator | Modify |
| `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectFacadeTest.java` | Unit test for allow-list resolution | Create (or modify if it already exists) |

---

## Task 1: TaskTools — shared key constant + allow-list filter

**Files:**
- Modify: `server/libs/ai/mcp/mcp-tool/mcp-tool-platform/src/main/java/com/bytechef/ai/mcp/tool/platform/TaskTools.java`
- Test: `server/libs/ai/mcp/mcp-tool/mcp-tool-platform/src/test/java/com/bytechef/ai/mcp/tool/platform/TaskToolsTest.java`

This is the heart of the change. `listTasks` (TaskTools.java:251-325) and `searchTasks` (TaskTools.java:327-410) each build a combined `List<TaskMinimalInfo>` from `componentTools.listActions()/listTriggers()` (or `searchActions(query)/searchTriggers(query)`) plus `taskDispatcherTools`. `TaskMinimalInfo` carries `componentName` (null for task dispatchers — see TaskTools.java:479-485). We add a `ToolContext` parameter, read a `Set<String>` allow-list from it, and keep only tasks whose `componentName` is null (dispatcher) or in the set. The filter runs **before** the `limit`/`subList` so we never under-fill the limit.

- [ ] **Step 1: Write the failing test**

Create `server/libs/ai/mcp/mcp-tool/mcp-tool-platform/src/test/java/com/bytechef/ai/mcp/tool/platform/TaskToolsTest.java`:

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

package com.bytechef.ai.mcp.tool.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.ai.mcp.tool.platform.ComponentTools.ActionMinimalInfo;
import com.bytechef.ai.mcp.tool.platform.ComponentTools.TriggerMinimalInfo;
import com.bytechef.ai.mcp.tool.platform.TaskDispatcherTools.TaskDispatcherMinimalInfo;
import com.bytechef.ai.mcp.tool.platform.TaskTools.TaskMinimalInfo;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.model.ToolContext;

class TaskToolsTest {

    @Mock
    private ComponentTools componentTools;

    @Mock
    private TaskDispatcherTools taskDispatcherTools;

    private TaskTools taskTools;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        taskTools = new TaskTools(componentTools, taskDispatcherTools);
    }

    @Test
    void testListTasksFiltersDisallowedComponentsButKeepsTaskDispatchers() {
        when(componentTools.listActions()).thenReturn(List.of(
            new ActionMinimalInfo("sendMessage", "Send a message", "slack"),
            new ActionMinimalInfo("createRow", "Create a row", "googleSheets")));
        when(componentTools.listTriggers()).thenReturn(List.of(
            new TriggerMinimalInfo("newMessage", "New message", "slack")));
        when(taskDispatcherTools.listTaskDispatchers()).thenReturn(List.of(
            new TaskDispatcherMinimalInfo("condition", "Branch on a condition", 1)));

        ToolContext toolContext = new ToolContext(Map.of(
            TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, Set.of("slack")));

        List<TaskMinimalInfo> result = taskTools.listTasks(null, null, toolContext);

        assertThat(result)
            .extracting(TaskMinimalInfo::componentName)
            .containsExactlyInAnyOrder("slack", "slack", null);
    }

    @Test
    void testListTasksWithoutToolContextReturnsEverything() {
        when(componentTools.listActions()).thenReturn(List.of(
            new ActionMinimalInfo("sendMessage", "Send a message", "slack"),
            new ActionMinimalInfo("createRow", "Create a row", "googleSheets")));
        when(componentTools.listTriggers()).thenReturn(List.of());
        when(taskDispatcherTools.listTaskDispatchers()).thenReturn(List.of());

        List<TaskMinimalInfo> result = taskTools.listTasks(null, null, null);

        assertThat(result)
            .extracting(TaskMinimalInfo::componentName)
            .containsExactlyInAnyOrder("slack", "googleSheets");
    }
}
```

- [ ] **Step 2: Run the test to verify it fails to compile/fail**

Run: `./gradlew :server:libs:ai:mcp:mcp-tool:mcp-tool-platform:test --tests "com.bytechef.ai.mcp.tool.platform.TaskToolsTest"`
Expected: FAIL — `TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY` does not exist and `listTasks` has no 3-arg overload (compilation error).

- [ ] **Step 3: Add the constant**

In `TaskTools.java`, just below the class declaration (after `public class TaskTools {` at line 48, alongside the existing `private static final String` error-message fields near line 50), add:

```java
    public static final String TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY = "allowedComponentNames";
```

- [ ] **Step 4: Add the `ToolContext` import**

In `TaskTools.java` imports, add (keep imports sorted):

```java
import org.springframework.ai.chat.model.ToolContext;
```

Also ensure these are imported (add any that are missing):

```java
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
```

- [ ] **Step 5: Add the filter helpers**

In `TaskTools.java`, add these two private static helpers (place them just above the `TaskMinimalInfo` record near line 479):

```java
    @SuppressWarnings("unchecked")
    private static @Nullable Set<String> getAllowedComponentNames(@Nullable ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }

        Object value = toolContext.getContext()
            .get(TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY);

        if (value instanceof Set<?> set) {
            return (Set<String>) set;
        }

        return null;
    }

    private static List<TaskMinimalInfo> filterByAllowedComponentNames(
        List<TaskMinimalInfo> tasks, @Nullable ToolContext toolContext) {

        Set<String> allowedComponentNames = getAllowedComponentNames(toolContext);

        if (allowedComponentNames == null || allowedComponentNames.isEmpty()) {
            return tasks;
        }

        return tasks.stream()
            .filter(task -> task.componentName() == null || allowedComponentNames.contains(task.componentName()))
            .collect(Collectors.toList());
    }
```

- [ ] **Step 6: Add the `ToolContext` parameter to `listTasks` and apply the filter**

In `TaskTools.java`, change the `listTasks` signature (line 253-257) to add a trailing `ToolContext` parameter:

```java
    public List<TaskMinimalInfo> listTasks(
        @ToolParam(
            required = false,
            description = "Type filter: 'action', 'trigger', 'taskDispatcher', or null for all types") String type,
        @ToolParam(required = false, description = "Limit on number of results returned") Integer limit,
        ToolContext toolContext) {
```

Then, inside `listTasks`, immediately **before** the limit block (currently line 305 `if (limit != null && limit > 0 && allTasks.size() > limit) {`), insert:

```java
            allTasks = filterByAllowedComponentNames(allTasks, toolContext);

```

(Note: `allTasks` is already a reassignable local `List<TaskMinimalInfo>`, so the reassignment compiles.)

- [ ] **Step 7: Add the `ToolContext` parameter to `searchTasks` and apply the filter**

In `TaskTools.java`, change the `searchTasks` signature (line 329-335) to add a trailing `ToolContext` parameter:

```java
    public List<TaskMinimalInfo> searchTasks(
        @ToolParam(description = "The search query to match against task names and descriptions") String query,
        @ToolParam(
            required = false,
            description = "Type filter: 'action', 'trigger', 'taskDispatcher', or null for all types") String type,
        @ToolParam(
            required = false, description = "Limit on number of results returned (defaults to 30)") Integer limit,
        ToolContext toolContext) {
```

Then, inside `searchTasks`, immediately **before** the sort block (currently line 385 `if (normalizedType == null) {`), insert:

```java
            matchingTasks = filterByAllowedComponentNames(matchingTasks, toolContext);

```

- [ ] **Step 8: Run the test to verify it passes**

Run: `./gradlew :server:libs:ai:mcp:mcp-tool:mcp-tool-platform:test --tests "com.bytechef.ai.mcp.tool.platform.TaskToolsTest"`
Expected: PASS (both tests).

- [ ] **Step 9: Check for other callers of `listTasks`/`searchTasks` that need the new argument**

Run: `grep -rn "\.listTasks(\|\.searchTasks(" server --include=*.java`
Expected: no production callers pass these directly (they are invoked by Spring AI tool dispatch, which injects `ToolContext` automatically); the only direct callers are the new `TaskToolsTest`. If any other direct caller exists (production or test), pass `null` as the `toolContext` argument. Document the result of the grep in the commit body.

- [ ] **Step 10: Compile the module to confirm no other breakage**

Run: `./gradlew :server:libs:ai:mcp:mcp-tool:mcp-tool-platform:compileJava :server:libs:ai:mcp:mcp-tool:mcp-tool-platform:compileTestJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 11: Commit**

```bash
git add server/libs/ai/mcp/mcp-tool/mcp-tool-platform/src/main/java/com/bytechef/ai/mcp/tool/platform/TaskTools.java \
        server/libs/ai/mcp/mcp-tool/mcp-tool-platform/src/test/java/com/bytechef/ai/mcp/tool/platform/TaskToolsTest.java
git commit -m "732 Add allow-list filtering to TaskTools discovery tools

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: CopilotToolContextUtils — State→ToolContext mapping

**Files:**
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/util/CopilotToolContextUtils.java`
- Test: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/util/CopilotToolContextUtilsTest.java`

A tiny, pure, testable unit that turns the per-run `State` into the toolContext map. The agent override (Task 3) delegates to it, keeping the agent change to one line and giving us a unit we can test without constructing the heavyweight agent.

- [ ] **Step 1: Write the failing test**

Create `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/util/CopilotToolContextUtilsTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.agui.core.state.State;
import com.bytechef.ai.mcp.tool.platform.TaskTools;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class CopilotToolContextUtilsTest {

    @Test
    void testToToolContextCopiesAllowedComponentNames() {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put("workflowId", "wf-1");
        stateMap.put(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, Set.of("slack", "logger"));

        Map<String, Object> toolContext = CopilotToolContextUtils.toToolContext(new State(stateMap));

        assertThat(toolContext)
            .containsEntry(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, Set.of("slack", "logger"));
    }

    @Test
    void testToToolContextWithoutAllowedComponentNamesIsEmpty() {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put("workflowId", "wf-1");

        Map<String, Object> toolContext = CopilotToolContextUtils.toToolContext(new State(stateMap));

        assertThat(toolContext).isEmpty();
    }

    @Test
    void testToToolContextWithNullStateIsEmpty() {
        assertThat(CopilotToolContextUtils.toToolContext(null)).isEmpty();
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests "com.bytechef.ee.ai.copilot.util.CopilotToolContextUtilsTest"`
Expected: FAIL — `CopilotToolContextUtils` does not exist (compilation error).

- [ ] **Step 3: Write the implementation**

Create `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/util/CopilotToolContextUtils.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.util;

import com.agui.core.state.State;
import com.bytechef.ai.mcp.tool.platform.TaskTools;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Maps per-run agent {@link State} entries into the Spring AI {@code ToolContext} map handed to copilot tools.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class CopilotToolContextUtils {

    private CopilotToolContextUtils() {
    }

    public static Map<String, Object> toToolContext(@Nullable State state) {
        if (state == null) {
            return Map.of();
        }

        Object allowedComponentNames = state.get(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY);

        if (allowedComponentNames == null) {
            return Map.of();
        }

        return Map.of(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, allowedComponentNames);
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests "com.bytechef.ee.ai.copilot.util.CopilotToolContextUtilsTest"`
Expected: PASS (all three tests).

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/util/CopilotToolContextUtils.java \
        server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/util/CopilotToolContextUtilsTest.java
git commit -m "732 Add CopilotToolContextUtils for State to ToolContext mapping

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: Wire the generator interface, State seeding, and agent override

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/service/CopilotWorkflowGenerator.java`
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/service/CopilotWorkflowGeneratorImpl.java`
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/agent/WorkflowEditorSpringAIAgent.java`

No new unit test here — this is type-checked wiring that connects the two already-tested ends (Task 1 filter, Task 2 mapping). Verified by compilation and the full build in Task 5.

- [ ] **Step 1: Find all implementors/callers of the interface method (so the signature change is complete)**

Run: `grep -rn "generateWorkflow(" server --include=*.java`
Expected implementors/callers: `CopilotWorkflowGenerator` (interface), `CopilotWorkflowGeneratorImpl` (impl), `AutomationWorkflowProjectFacadeImpl` (caller). If any additional implementor exists (e.g. a remote-client stub), it must also be updated in this task — update each to the new 3-arg signature, throwing/stubbing exactly as it does today for the other methods.

- [ ] **Step 2: Update the interface**

In `CopilotWorkflowGenerator.java`, replace the method declaration:

```java
    void generateWorkflow(String workflowId, String prompt);
```

with:

```java
    void generateWorkflow(String workflowId, String prompt, java.util.Set<String> allowedComponentNames);
```

If the file has an imports section, prefer adding `import java.util.Set;` and using `Set<String>` instead of the fully-qualified name, keeping imports sorted.

- [ ] **Step 3: Seed the allow-list into `State` in the impl**

In `CopilotWorkflowGeneratorImpl.java`:

Add imports (keep sorted):

```java
import com.bytechef.ai.mcp.tool.platform.TaskTools;
import java.util.Set;
```

Change the method signature (line 58) from:

```java
    public void generateWorkflow(String workflowId, String prompt) {
```

to:

```java
    public void generateWorkflow(String workflowId, String prompt, Set<String> allowedComponentNames) {
```

Replace the `State` construction (lines 67-69):

```java
        State state = new State(new HashMap<>(Map.of(
            "workflowId", workflowId,
            "mode", Mode.BUILD.name())));
```

with:

```java
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put("workflowId", workflowId);
        stateMap.put("mode", Mode.BUILD.name());

        if (allowedComponentNames != null && !allowedComponentNames.isEmpty()) {
            stateMap.put(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, allowedComponentNames);
        }

        State state = new State(stateMap);
```

(`HashMap` and `Map` are already imported in this file; `Map.of` is no longer used here but leaving the `Map` import is harmless — Spotless/`check` will flag it only if truly unused, in which case remove it.)

- [ ] **Step 4: Override `toolContext()` on the agent**

In `WorkflowEditorSpringAIAgent.java`:

Add imports (keep sorted):

```java
import com.bytechef.ai.mcp.tool.platform.TaskTools;
import com.bytechef.ee.ai.copilot.util.CopilotToolContextUtils;
import java.util.Map;
```

(`com.agui.core.agent.RunAgentInput` and `com.agui.core.state.State` are already imported.)

Add this method to the class body (place it next to the other `@Override protected` hooks such as `createSystemMessage`):

```java
    @Override
    protected Map<String, Object> toolContext(RunAgentInput input) {
        return CopilotToolContextUtils.toToolContext(input.state());
    }
```

(The `TaskTools` import is not strictly required by this method since the key lives in `CopilotToolContextUtils`; omit it here if your IDE flags it as unused. It is listed only in case you inline the logic.)

- [ ] **Step 5: Compile both copilot modules**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-api:compileJava :server:ee:libs:ai:ai-copilot:ai-copilot-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/service/CopilotWorkflowGenerator.java \
        server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/service/CopilotWorkflowGeneratorImpl.java \
        server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/agent/WorkflowEditorSpringAIAgent.java
git commit -m "732 Thread allowed component names through copilot generator and agent

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: Resolve the allow-list in the embedded facade

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectFacadeImpl.java`
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectFacadeTest.java`

Resolution rule: `allowedComponentNames = { Integration.componentName : IIC is enabled for this environment } ∪ { ComponentDefinition.name : !isConnectionRequired() }`. We extract a package-private `resolveAllowedComponentNames(Environment)` so it can be unit-tested directly without exercising the whole `generateProjectWorkflow` flow.

Confirmed signatures used:
- `IntegrationInstanceConfigurationService.getIntegrationInstanceConfigurations(Environment, boolean)` → `List<IntegrationInstanceConfiguration>`
- `IntegrationInstanceConfiguration.getIntegrationId()` → `Long`
- `IntegrationService.getIntegrations(List<Long>)` → `List<Integration>`
- `Integration.getComponentName()` → `String`
- `ComponentDefinitionService.getComponentDefinitions()` → `List<ComponentDefinition>`
- `ComponentDefinition.isConnectionRequired()` → `boolean`, `ComponentDefinition.getName()` → `String`

- [ ] **Step 1: Write the failing test**

Create `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectFacadeTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @version ee
 */
class AutomationWorkflowProjectFacadeTest {

    @Mock
    private ComponentDefinitionService componentDefinitionService;

    @Mock
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Mock
    private IntegrationService integrationService;

    private AutomationWorkflowProjectFacadeImpl facade;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        facade = new AutomationWorkflowProjectFacadeImpl(
            null, componentDefinitionService, null, null, null, null, null, null, null, null, null,
            integrationInstanceConfigurationService, integrationService);
    }

    @Test
    void testResolveAllowedComponentNamesUnionsEnabledIntegrationsAndConnectionlessComponents() {
        IntegrationInstanceConfiguration enabledConfiguration = new IntegrationInstanceConfiguration();

        enabledConfiguration.setIntegrationId(10L);

        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(Environment.PRODUCTION, true))
            .thenReturn(List.of(enabledConfiguration));

        Integration slackIntegration = new Integration();

        slackIntegration.setComponentName("slack");

        when(integrationService.getIntegrations(List.of(10L))).thenReturn(List.of(slackIntegration));

        // logger requires no connection (kept); httpClient requires a connection (filtered out unless enabled)
        ComponentDefinition loggerDefinition = mock(ComponentDefinition.class);

        when(loggerDefinition.getName()).thenReturn("logger");
        when(loggerDefinition.isConnectionRequired()).thenReturn(false);

        ComponentDefinition httpDefinition = mock(ComponentDefinition.class);

        when(httpDefinition.isConnectionRequired()).thenReturn(true);

        when(componentDefinitionService.getComponentDefinitions())
            .thenReturn(List.of(loggerDefinition, httpDefinition));

        Set<String> result = facade.resolveAllowedComponentNames(Environment.PRODUCTION);

        assertThat(result).containsExactlyInAnyOrder("slack", "logger");
    }

    @Test
    void testResolveAllowedComponentNamesWithNoEnabledIntegrationsKeepsOnlyConnectionlessComponents() {
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(Environment.PRODUCTION, true))
            .thenReturn(List.of());
        when(integrationService.getIntegrations(List.of())).thenReturn(List.of());

        ComponentDefinition loggerDefinition = mock(ComponentDefinition.class);

        when(loggerDefinition.getName()).thenReturn("logger");
        when(loggerDefinition.isConnectionRequired()).thenReturn(false);

        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(loggerDefinition));

        Set<String> result = facade.resolveAllowedComponentNames(Environment.PRODUCTION);

        assertThat(result).containsExactly("logger");
    }
}
```

> **Note on test setup:** The `AutomationWorkflowProjectFacadeImpl` constructor below ends with the two new parameters `integrationInstanceConfigurationService, integrationService`. The test passes `null` for the dependencies `resolveAllowedComponentNames` does not touch (the constructor only assigns fields, so null is safe). `ComponentDefinition` is mocked because its single-arg constructor cannot set `isConnectionRequired()=true`; `httpDefinition.getName()` is intentionally not stubbed since the connection-required filter drops it before `getName()` is read.

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacadeTest"`
Expected: FAIL — constructor has no 13-arg form and `resolveAllowedComponentNames` does not exist (compilation error).

- [ ] **Step 3: Add the two service fields + constructor params**

In `AutomationWorkflowProjectFacadeImpl.java`:

Add imports (keep sorted):

```java
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import java.util.HashSet;
```

Add two fields (alongside the existing `private final` fields near line 78-88, keeping alphabetical grouping consistent with the file):

```java
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationService integrationService;
```

Add the two parameters to the constructor (after the existing last parameter `WorkflowTestConfigurationService workflowTestConfigurationService` at line 97) and assign them. The constructor parameter list becomes:

```java
        TagService tagService, WorkflowNodeTestOutputService workflowNodeTestOutputService,
        WorkflowService workflowService, WorkflowTestConfigurationService workflowTestConfigurationService,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationService integrationService) {
```

and at the end of the constructor body (after line 109 `this.workflowTestConfigurationService = workflowTestConfigurationService;`) add:

```java
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationService = integrationService;
```

- [ ] **Step 4: Add the `resolveAllowedComponentNames` method**

In `AutomationWorkflowProjectFacadeImpl.java`, add this package-private method (place it near the other private helpers such as `getTaskComponents`/`resolveComponents`):

```java
    Set<String> resolveAllowedComponentNames(Environment environment) {
        List<IntegrationInstanceConfiguration> enabledConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(environment, true);

        List<Long> integrationIds = enabledConfigurations.stream()
            .map(IntegrationInstanceConfiguration::getIntegrationId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        Set<String> allowedComponentNames = integrationService.getIntegrations(integrationIds)
            .stream()
            .map(Integration::getComponentName)
            .collect(Collectors.toCollection(HashSet::new));

        componentDefinitionService.getComponentDefinitions()
            .stream()
            .filter(componentDefinition -> !componentDefinition.isConnectionRequired())
            .map(ComponentDefinition::getName)
            .forEach(allowedComponentNames::add);

        return allowedComponentNames;
    }
```

- [ ] **Step 5: Call it from `generateProjectWorkflow`**

In `AutomationWorkflowProjectFacadeImpl.java`, in `generateProjectWorkflow` (lines 131-149), replace:

```java
        copilotWorkflowGenerator.generateWorkflow(workflowId, prompt);
```

with:

```java
        Set<String> allowedComponentNames = resolveAllowedComponentNames(environment);

        copilotWorkflowGenerator.generateWorkflow(workflowId, prompt, allowedComponentNames);
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacadeTest"`
Expected: PASS (both tests). If the `ComponentDefinition` real constructor doesn't expose `isConnectionRequired()`, switch to mocks as noted in Step 1 and re-run.

- [ ] **Step 7: Commit**

```bash
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectFacadeImpl.java \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectFacadeTest.java
git commit -m "732 Resolve enabled-integration allow-list for embedded copilot generation

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: Full verification

**Files:** none (verification only)

- [ ] **Step 1: Format**

Run: `./gradlew spotlessApply`
Expected: BUILD SUCCESSFUL; re-stage any files Spotless reformats and amend the relevant commit if needed.

- [ ] **Step 2: Build + static analysis for the touched modules**

Run:
```bash
./gradlew :server:libs:ai:mcp:mcp-tool:mcp-tool-platform:check \
          :server:ee:libs:ai:ai-copilot:ai-copilot-api:check \
          :server:ee:libs:ai:ai-copilot:ai-copilot-service:check \
          :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:check
```
Expected: BUILD SUCCESSFUL (Checkstyle/PMD/SpotBugs clean, all tests pass).

- [ ] **Step 3: Confirm no behavior change for non-embedded callers**

Re-run the grep from Task 1 Step 9 and Task 3 Step 1 and confirm the only production caller of `generateWorkflow(...)` is `AutomationWorkflowProjectFacadeImpl`, and that no production code calls `listTasks`/`searchTasks` directly. State the outcome in the final summary.

- [ ] **Step 4: Final commit (only if Spotless or fixups produced uncommitted changes)**

```bash
git add -A
git commit -m "732 Apply spotless formatting for enabled-integration copilot filter

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Notes / Risks

- **Adding a `ToolContext` parameter to existing `@Tool` methods is transparent** to the LLM-facing JSON schema — Spring AI excludes `ToolContext` parameters from the generated tool schema and injects the per-request context. Existing callers (MCP server, in-product copilot) pass no allow-list key, so `getAllowedComponentNames` returns `null` and the filter is a no-op.
- **In-process object passing:** the `Set<String>` flows State → ToolContext → TaskTools by reference within one JVM/run; there is no JSON round-trip, so the value stays a `Set<String>` and the `instanceof Set<?>` check holds.
- **`ComponentDefinition` is mocked in the facade test** because its single-arg constructor leaves `isConnectionRequired()` at the `false` default and offers no way to set it `true`; mocking keeps the connection-required case explicit.
- **Empty allow-list cannot happen on the embedded path** because connection-less built-ins are always unioned in; the no-op opt-out is reachable only by a deliberate null/empty caller.
