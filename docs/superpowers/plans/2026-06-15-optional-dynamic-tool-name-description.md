# Optional, Dynamically-Injected `toolName` / `toolDescription` — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `toolName` and `toolDescription` optional override fields that the platform injects dynamically into every TOOL cluster element, instead of being statically declared as `required(true)` in each tool.

**Architecture:** A single canonical key constant lives in `platform-component-api`. The platform `ClusterElementDefinitionServiceImpl` prepends two **optional** `toolName`/`toolDescription` properties to any cluster element whose type is `TOOLS` (idempotently — skipped if a property of that name already exists), via a new immutable `ClusterElementDefinition.withPrependedProperties(...)`. The seven tools that declared the fields statically have those blocks removed; runtime defaults are unchanged because consumption already falls back to action-derived name/description.

**Tech Stack:** Java 25, Spring, ByteChef component DSL (`ComponentDsl`), JUnit 5 + Mockito.

**Spec:** `docs/superpowers/specs/2026-06-15-optional-dynamic-tool-name-description-design.md`

---

> **Revision (during implementation):**
> 1. **Constant home (Tasks 1 & 3 superseded).** Instead of creating a new
>    `ToolConstants` in `platform-component-api` and having `platform-ai` delegate to it,
>    the implementation keeps the **single** existing
>    `com.bytechef.platform.ai.tool.constant.ToolConstants` and adds an
>    `implementation(platform-ai-api)` dependency to `platform-component-service` (the
>    `automation-ai-mcp-api` edge was first removed from `platform-ai-api` so this pulls
>    no automation layer in). Task 4's service code imports `platform.ai...ToolConstants`.
> 2. **Tasks 5 & 6 DROPPED.** The seven tools that statically declare `toolName`/
>    `toolDescription` as `required(true)` are **left unchanged** — those required fields
>    are intentional. The idempotent injection already adds the *optional* override fields
>    only where they are absent, so no tool edits and no definition-snapshot regeneration
>    are needed. The feature is delivered entirely by Tasks 2 & 4 (+ the constant wiring).

## File Structure

- **Create:** `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/tool/constant/ToolConstants.java` — canonical key strings.
- **Create:** `server/libs/platform/platform-component/platform-component-api/src/test/java/com/bytechef/platform/component/domain/ClusterElementDefinitionTest.java` — domain method test.
- **Modify:** `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/domain/ClusterElementDefinition.java` — add `withPrependedProperties` + private copy constructor.
- **Modify:** `server/libs/platform/platform-ai/platform-ai-api/src/main/java/com/bytechef/platform/ai/tool/constant/ToolConstants.java` — reference the new constants (single source).
- **Modify:** `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/ClusterElementDefinitionServiceImpl.java` — injection helper + route the 5 construction sites.
- **Modify:** `server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/service/ClusterElementDefinitionServiceTest.java` — 3 injection tests.
- **Modify (remove static blocks):** `HttpClientTool.java`, `AiAgentChatTool.java`, `AgenticAiTool.java`, `WorkflowCallWorkflowTool.java`, `ScriptPythonTool.java`, `ScriptJavaScriptTool.java`, `ScriptRubyTool.java`.

---

## Task 1: Canonical key constants in platform-component-api

**Files:**
- Create: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/tool/constant/ToolConstants.java`

- [ ] **Step 1: Create the constants class**

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

package com.bytechef.platform.component.tool.constant;

/**
 * Canonical parameter keys for the AI tool name/description overrides. This is the single source of truth shared by the
 * platform component layer (which injects the optional properties) and the platform AI layer (which reads them).
 *
 * @author Ivica Cardic
 */
public class ToolConstants {

    public static final String TOOL_DESCRIPTION = "toolDescription";
    public static final String TOOL_NAME = "toolName";

    private ToolConstants() {
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/tool/constant/ToolConstants.java
git commit -m "Add canonical tool name/description key constants to platform-component-api"
```

---

## Task 2: Immutable `withPrependedProperties` on the domain

**Files:**
- Modify: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/domain/ClusterElementDefinition.java`
- Test: `server/libs/platform/platform-component/platform-component-api/src/test/java/com/bytechef/platform/component/domain/ClusterElementDefinitionTest.java`

- [ ] **Step 1: Write the failing test**

Create `ClusterElementDefinitionTest.java`:

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

package com.bytechef.platform.component.domain;

import static com.bytechef.component.definition.ComponentDsl.clusterElement;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ClusterElementDefinitionTest {

    @Test
    void testWithPrependedProperties() {
        com.bytechef.component.definition.ClusterElementDefinition<?> sdkDefinition = clusterElement("test")
            .title("Test")
            .description("A test element")
            .type(TOOLS)
            .properties(string("foo"));

        ClusterElementDefinition definition = new ClusterElementDefinition(sdkDefinition, "comp", 1, "icon");

        Property barProperty = Property.toProperty(string("bar"));
        Property bazProperty = Property.toProperty(string("baz"));

        ClusterElementDefinition result = definition.withPrependedProperties(List.of(barProperty, bazProperty));

        assertEquals(
            List.of("bar", "baz", "foo"),
            result.getProperties()
                .stream()
                .map(Property::getName)
                .toList());

        // original is unmodified
        assertEquals(
            List.of("foo"),
            definition.getProperties()
                .stream()
                .map(Property::getName)
                .toList());

        // other fields copied
        assertEquals("comp", result.getComponentName());
        assertEquals(1, result.getComponentVersion());
        assertEquals("test", result.getName());
        assertEquals(TOOLS, result.getType());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-api:test --tests "com.bytechef.platform.component.domain.ClusterElementDefinitionTest"`
Expected: FAIL — `withPrependedProperties` does not exist (compile error).

- [ ] **Step 3: Add the copy constructor and method to `ClusterElementDefinition.java`**

Add `import java.util.ArrayList;` to the imports (after `import java.util.Collections;`).

Add this private constructor immediately after the existing public constructor (after the closing `}` of the constructor ending at line 82):

```java
    private ClusterElementDefinition(
        ClusterElementDefinition clusterElementDefinition, List<? extends Property> prependedProperties) {

        this.componentName = clusterElementDefinition.componentName;
        this.componentVersion = clusterElementDefinition.componentVersion;
        this.description = clusterElementDefinition.description;
        this.help = clusterElementDefinition.help;
        this.icon = clusterElementDefinition.icon;
        this.name = clusterElementDefinition.name;
        this.outputDefined = clusterElementDefinition.outputDefined;
        this.outputFunctionDefined = clusterElementDefinition.outputFunctionDefined;
        this.outputResponse = clusterElementDefinition.outputResponse;
        this.outputSchemaDefined = clusterElementDefinition.outputSchemaDefined;

        List<Property> mergedProperties = new ArrayList<>(prependedProperties);

        mergedProperties.addAll(clusterElementDefinition.properties);

        this.properties = mergedProperties;
        this.title = clusterElementDefinition.title;
        this.type = clusterElementDefinition.type;
    }
```

Add this public method immediately after the existing `getProperties()` method (after line 139):

```java
    public ClusterElementDefinition withPrependedProperties(List<? extends Property> prependedProperties) {
        return new ClusterElementDefinition(this, prependedProperties);
    }
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-api:test --tests "com.bytechef.platform.component.domain.ClusterElementDefinitionTest"`
Expected: PASS

- [ ] **Step 5: Format and commit**

```bash
./gradlew :server:libs:platform:platform-component:platform-component-api:spotlessApply
git add server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/domain/ClusterElementDefinition.java server/libs/platform/platform-component/platform-component-api/src/test/java/com/bytechef/platform/component/domain/ClusterElementDefinitionTest.java
git commit -m "Add immutable withPrependedProperties to ClusterElementDefinition"
```

---

## Task 3: Repoint platform-ai `ToolConstants` to the canonical source

**Files:**
- Modify: `server/libs/platform/platform-ai/platform-ai-api/src/main/java/com/bytechef/platform/ai/tool/constant/ToolConstants.java`

**Context:** `platform-ai-api` already depends on `platform-component-api` via `api(...)`. All `ToolConstants.TOOL_NAME` / `TOOL_DESCRIPTION` usages across the codebase are method arguments (verified — none in annotations or `case` labels), so the fields may reference another constant safely.

- [ ] **Step 1: Replace the literals with references**

Replace the class body so the two fields delegate to the canonical constants:

```java
package com.bytechef.platform.ai.tool.constant;

/**
 * @author Ivica Cardic
 */
public class ToolConstants {

    public static final String TOOL_DESCRIPTION =
        com.bytechef.platform.component.tool.constant.ToolConstants.TOOL_DESCRIPTION;
    public static final String TOOL_NAME =
        com.bytechef.platform.component.tool.constant.ToolConstants.TOOL_NAME;

}
```

(Keep the existing license header at the top of the file unchanged.)

- [ ] **Step 2: Compile platform-ai-api and a downstream consumer**

Run: `./gradlew :server:libs:platform:platform-ai:platform-ai-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/libs/platform/platform-ai/platform-ai-api/src/main/java/com/bytechef/platform/ai/tool/constant/ToolConstants.java
git commit -m "Reference platform-component tool key constants from platform-ai ToolConstants"
```

---

## Task 4: Inject the optional override properties in the platform service

**Files:**
- Modify: `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/ClusterElementDefinitionServiceImpl.java`
- Test: `server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/service/ClusterElementDefinitionServiceTest.java`

- [ ] **Step 1: Write the failing tests**

Add these three tests to `ClusterElementDefinitionServiceTest.java` (the class already mocks `componentDefinitionRegistry` and has `createMatchableClusterElementDefinition(name, type)` / `createComponentDefinitionForMatch(list)` helpers). Add the imports `import static com.bytechef.component.definition.ComponentDsl.string;`, `import com.bytechef.platform.component.domain.Property;`, and `import static org.junit.jupiter.api.Assertions.assertFalse;` (the existing file already imports `assertEquals`; `mock`, `when`, `Optional`, and `List` are also already present).

```java
    @Test
    void testGetClusterElementDefinitionInjectsToolOverrideProperties() {
        String clusterElementName = "openai";
        ClusterElementType toolsType = new ClusterElementType("TOOLS", "tools", "Tools");

        com.bytechef.component.definition.ClusterElementDefinition<?> elementDefinition =
            createMatchableClusterElementDefinition(clusterElementName, toolsType);

        ComponentDefinition componentDefinition = createComponentDefinitionForMatch(List.of(elementDefinition));

        when(componentDefinitionRegistry.getComponentDefinition(COMPONENT_NAME, COMPONENT_VERSION))
            .thenReturn(componentDefinition);

        ClusterElementDefinition result = clusterElementDefinitionService.getClusterElementDefinition(
            COMPONENT_NAME, COMPONENT_VERSION, clusterElementName, "TOOLS");

        List<String> propertyNames = result.getProperties()
            .stream()
            .map(Property::getName)
            .toList();

        assertEquals(List.of("toolName", "toolDescription"), propertyNames);
        assertFalse(result.getProperties()
            .get(0)
            .getRequired());
        assertFalse(result.getProperties()
            .get(1)
            .getRequired());
    }

    @Test
    void testGetClusterElementDefinitionDoesNotInjectForNonToolsType() {
        String clusterElementName = "openai";
        ClusterElementType chatMemoryType = new ClusterElementType("CHAT_MEMORY", "chatMemory", "Chat Memory");

        com.bytechef.component.definition.ClusterElementDefinition<?> elementDefinition =
            createMatchableClusterElementDefinition(clusterElementName, chatMemoryType);

        ComponentDefinition componentDefinition = createComponentDefinitionForMatch(List.of(elementDefinition));

        when(componentDefinitionRegistry.getComponentDefinition(COMPONENT_NAME, COMPONENT_VERSION))
            .thenReturn(componentDefinition);

        ClusterElementDefinition result = clusterElementDefinitionService.getClusterElementDefinition(
            COMPONENT_NAME, COMPONENT_VERSION, clusterElementName, "CHAT_MEMORY");

        List<String> propertyNames = result.getProperties()
            .stream()
            .map(Property::getName)
            .toList();

        assertFalse(propertyNames.contains("toolName"));
        assertFalse(propertyNames.contains("toolDescription"));
    }

    @Test
    void testGetClusterElementDefinitionDoesNotDuplicateExistingToolName() {
        String clusterElementName = "openai";
        ClusterElementType toolsType = new ClusterElementType("TOOLS", "tools", "Tools");

        com.bytechef.component.definition.ClusterElementDefinition<?> elementDefinition =
            mock(com.bytechef.component.definition.ClusterElementDefinition.class);

        when(elementDefinition.getName()).thenReturn(clusterElementName);
        when(elementDefinition.getType()).thenReturn(toolsType);
        when(elementDefinition.getDescription()).thenReturn(Optional.empty());
        when(elementDefinition.getHelp()).thenReturn(Optional.empty());
        when(elementDefinition.getTitle()).thenReturn(Optional.of(clusterElementName));
        when(elementDefinition.getProperties()).thenReturn(Optional.of(List.of(string("toolName"))));
        when(elementDefinition.getOutputDefinition()).thenReturn(Optional.empty());

        ComponentDefinition componentDefinition = createComponentDefinitionForMatch(List.of(elementDefinition));

        when(componentDefinitionRegistry.getComponentDefinition(COMPONENT_NAME, COMPONENT_VERSION))
            .thenReturn(componentDefinition);

        ClusterElementDefinition result = clusterElementDefinitionService.getClusterElementDefinition(
            COMPONENT_NAME, COMPONENT_VERSION, clusterElementName, "TOOLS");

        List<String> propertyNames = result.getProperties()
            .stream()
            .map(Property::getName)
            .toList();

        // toolName already declared -> not duplicated; toolDescription still injected at the front
        assertEquals(1, propertyNames.stream()
            .filter("toolName"::equals)
            .count());
        assertEquals(List.of("toolDescription", "toolName"), propertyNames);
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests "com.bytechef.platform.component.service.ClusterElementDefinitionServiceTest"`
Expected: FAIL — the new tests fail because no properties are injected (asserts on `toolName`/`toolDescription` fail).

- [ ] **Step 3: Add the injection logic to `ClusterElementDefinitionServiceImpl.java`**

Add these imports (alongside the existing imports — keep alphabetical grouping where the file already groups):

```java
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.TEXT_AREA;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;

import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.tool.constant.ToolConstants;
import java.util.ArrayList;
```

(Note: `com.bytechef.platform.component.domain.Property` is already used by this class for `executeDynamicProperties` — if the import is already present, do not duplicate it. `ArrayList` may already be imported.)

Add these two private constants near the top of the class body (with the other fields):

```java
    private static final Property TOOL_NAME_PROPERTY = Property.toProperty(
        string(ToolConstants.TOOL_NAME)
            .label("Name")
            .description("The tool name exposed to the AI model. Defaults to the action name when left blank.")
            .expressionEnabled(false)
            .required(false));

    private static final Property TOOL_DESCRIPTION_PROPERTY = Property.toProperty(
        string(ToolConstants.TOOL_DESCRIPTION)
            .label("Description")
            .description(
                "The tool description exposed to the AI model. Defaults to the action description when left blank.")
            .controlType(TEXT_AREA)
            .expressionEnabled(false)
            .required(false));
```

Add these two private helper methods (place them near the other private helpers, e.g. after `findClusterElementType`):

```java
    private static ClusterElementDefinition toClusterElementDefinition(
        com.bytechef.component.definition.ClusterElementDefinition<?> clusterElementDefinition, String componentName,
        int componentVersion, String icon) {

        return injectToolOverrideProperties(
            new ClusterElementDefinition(clusterElementDefinition, componentName, componentVersion, icon));
    }

    private static ClusterElementDefinition injectToolOverrideProperties(
        ClusterElementDefinition clusterElementDefinition) {

        ClusterElementType type = clusterElementDefinition.getType();

        // Identify TOOLS by type name (the codebase matches cluster element types by name, not full-record equality:
        // the canonical BaseToolFunction.TOOLS sets multipleElements=true, which other constructions may not mirror).
        if (type == null || !TOOLS.name()
            .equals(type.name())) {

            return clusterElementDefinition;
        }

        List<String> propertyNames = clusterElementDefinition.getProperties()
            .stream()
            .map(Property::getName)
            .toList();

        List<Property> additionalProperties = new ArrayList<>();

        if (!propertyNames.contains(ToolConstants.TOOL_NAME)) {
            additionalProperties.add(TOOL_NAME_PROPERTY);
        }

        if (!propertyNames.contains(ToolConstants.TOOL_DESCRIPTION)) {
            additionalProperties.add(TOOL_DESCRIPTION_PROPERTY);
        }

        if (additionalProperties.isEmpty()) {
            return clusterElementDefinition;
        }

        return clusterElementDefinition.withPrependedProperties(additionalProperties);
    }
```

- [ ] **Step 4: Route all five construction sites through the helper**

Replace each `new ClusterElementDefinition(...)` that builds an editor-facing definition with `toClusterElementDefinition(...)`:

1. In `getClusterElementDefinition(componentName, clusterElementName)` (~line 326):
```java
        return toClusterElementDefinition(
            result.clusterElementDefinition, componentDefinition.getName(), componentDefinition.getVersion(),
            getIcon(componentDefinition));
```

2. In `getClusterElementDefinition(componentName, componentVersion, clusterElementName)` (~line 340):
```java
        return toClusterElementDefinition(
            result.clusterElementDefinition, componentDefinition.getName(), componentVersion,
            getIcon(componentDefinition));
```

3. In `getClusterElementDefinition(componentName, componentVersion, clusterElementName, clusterElementTypeName)` (~line 369):
```java
        return toClusterElementDefinition(
            matchedDefinition, componentDefinition.getName(), componentVersion, getIcon(componentDefinition));
```

4. In `getClusterElementDefinitions(ClusterElementType)` (~line 385), inside the `.map(...)`:
```java
                    .map(clusterElementDefinition -> toClusterElementDefinition(
                        clusterElementDefinition, componentDefinition.getName(), componentDefinition.getVersion(),
                        getIcon(componentDefinition)))
```

5. In `getClusterElementDefinitions(componentName, componentVersion, ClusterElementType)` (~line 406), inside the `.map(...)`:
```java
            .map(clusterElementDefinition -> toClusterElementDefinition(
                clusterElementDefinition, componentDefinition.getName(), componentVersion, icon))
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests "com.bytechef.platform.component.service.ClusterElementDefinitionServiceTest"`
Expected: PASS (all tests, including the 3 new ones and the pre-existing ones).

- [ ] **Step 6: Format and commit**

```bash
./gradlew :server:libs:platform:platform-component:platform-component-service:spotlessApply
git add server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/ClusterElementDefinitionServiceImpl.java server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/service/ClusterElementDefinitionServiceTest.java
git commit -m "Inject optional toolName/toolDescription into TOOLS cluster elements in platform service"
```

---

## Task 5: Remove the static `toolName`/`toolDescription` blocks from the seven tools

**Files:** the seven tool classes listed below.

Each edit removes the two `string(TOOL_NAME)` / `string(TOOL_DESCRIPTION)` property blocks and the now-unused imports. After each module's edit, compile that module.

- [ ] **Step 1: `HttpClientTool.java`** — `tool(actionDefinition)` already carries the action's title/description/type/properties/output/perform, so the method collapses to a single return.

Replace the whole `of(...)` method body and trim imports. The file becomes:

```java
package com.bytechef.component.http.client.cluster;

import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.ai.agent.ToolFunction;

/**
 * @author Ivica Cardic
 */
public class HttpClientTool {

    public static ModifiableClusterElementDefinition<ToolFunction> of(ActionDefinition actionDefinition) {
        return tool(actionDefinition);
    }

    private HttpClientTool() {
    }
}
```

(Removed imports: `ComponentDsl.string`, `Property.ControlType.TEXT_AREA`, both `ToolConstants` statics, `com.bytechef.component.definition.Property`, `java.util.ArrayList`, `java.util.List`. Keep the license header at the top.)

- [ ] **Step 2: `AiAgentChatTool.java`** — remove the two blocks from the `properties(List.of(...))` call (lines 52–62) so the list starts at `FORMAT_PROPERTY`. Remove imports `ComponentDsl.string` (line 25), `Property.ControlType.TEXT_AREA` (line 26), and both `ToolConstants` statics (lines 28–29). The `.properties(...)` becomes:

```java
            .properties(
                List.of(
                    FORMAT_PROPERTY,
                    PROMPT_PROPERTY,
                    SYSTEM_PROMPT_PROPERTY,
                    ATTACHMENTS_PROPERTY,
                    MESSAGES_PROPERTY,
                    RESPONSE_PROPERTY))
```

- [ ] **Step 3: `AgenticAiTool.java`** — remove the two blocks from `properties(List.of(...))` (lines 52–62). Remove imports for both `ToolConstants` statics (lines 25–26). **Keep** `ComponentDsl.string` (still used by `string(GOAL_DESCRIPTION)`) and **keep** `Property.ControlType.TEXT_AREA` (still used by `GOAL_DESCRIPTION`). The `.properties(...)` becomes:

```java
            .properties(
                List.of(
                    string(GOAL_DESCRIPTION)
                        .label("Goal Description")
                        .description("Describe the goal the agentic AI should achieve using the configured tools.")
                        .controlType(TEXT_AREA)
                        .required(true),
                    SYSTEM_PROMPT_PROPERTY,
                    RESPONSE_PROPERTY))
```

- [ ] **Step 4: `WorkflowCallWorkflowTool.java`** — remove the two blocks from `.properties(...)` (lines 93–103) so the list starts at `string(WORKFLOW_UUID)`. Remove imports for both `ToolConstants` statics (lines 25–26) and `Property.ControlType.TEXT_AREA` (line 23 — only the removed description block used it). **Keep** `ComponentDsl.string` (used by `string(WORKFLOW_UUID)` and `string(name)`). The `.properties(...)` becomes:

```java
            .properties(
                string(WORKFLOW_UUID)
                    .label("Workflow")
                    .description("The workflow to call when this tool is invoked.")
                    .options(getWorkflowOptionsFunction(subflowDataSource))
                    .required(true),
                dynamicProperties(INPUTS)
                    .description("The input parameters for the sub-workflow.")
                    .propertiesLookupDependsOn(WORKFLOW_UUID)
                    .properties(getPropertiesFunction(subflowDataSource)))
```

- [ ] **Step 5: `ScriptPythonTool.java`** — remove the two blocks from `.properties(...)` (lines 53–63) so the list starts at `object(INPUT)`. Remove imports `Property.ControlType.TEXT_AREA` (line 31) and both `ToolConstants` statics (lines 34–35). **Keep** `ComponentDsl.string` (used by `string(SCRIPT)` and `string()` in `additionalProperties`). The `.properties(...)` becomes:

```java
                .properties(
                    object(INPUT)
                        .label("Input")
                        .description("Initialize parameter values used in the custom code.")
                        .additionalProperties(
                            array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(),
                            time()),
                    string(SCRIPT)
                        .label("Python Code")
                        .description("Custom Python code to execute as a tool.")
                        .controlType(CODE_EDITOR)
                        .languageId("python")
                        .defaultValue("def perform(input, context):\n\treturn None")
                        .required(true)),
```

- [ ] **Step 6: `ScriptJavaScriptTool.java`** — identical change to Step 5. Remove the two blocks (lines 53–63), remove `Property.ControlType.TEXT_AREA` (line 31) and both `ToolConstants` statics (lines 34–35), keep `ComponentDsl.string`. The `.properties(...)` becomes:

```java
                .properties(
                    object(INPUT)
                        .label("Input")
                        .description("Initialize parameter values used in the custom code.")
                        .additionalProperties(
                            array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(),
                            time()),
                    string(SCRIPT)
                        .label("JavaScript Code")
                        .description("Custom JavaScript code to execute as a tool.")
                        .controlType(CODE_EDITOR)
                        .languageId("javascript")
                        .defaultValue("function perform(input, context) {\n\treturn null;\n}")
                        .required(true)),
```

- [ ] **Step 7: `ScriptRubyTool.java`** — identical change to Step 5. Remove the two blocks (lines 53–63), remove `Property.ControlType.TEXT_AREA` (line 31) and both `ToolConstants` statics (lines 34–35), keep `ComponentDsl.string`. The `.properties(...)` becomes:

```java
                .properties(
                    object(INPUT)
                        .label("Input")
                        .description("Initialize parameter values used in the custom code.")
                        .additionalProperties(
                            array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(),
                            time()),
                    string(SCRIPT)
                        .label("Ruby Code")
                        .description("Custom Ruby code to execute as a tool.")
                        .controlType(CODE_EDITOR)
                        .languageId("ruby")
                        .defaultValue("def perform(input, context)\n\treturn nil;\nend")
                        .required(true)),
```

- [ ] **Step 8: Compile the affected component modules**

Run:
```bash
./gradlew \
  :server:libs:modules:components:http-client:compileJava \
  :server:libs:modules:components:script:compileJava \
  :server:libs:modules:components:ai:agent:compileJava \
  :server:libs:modules:components:ai:agenticai:compileJava \
  :server:libs:modules:components:workflow:compileJava
```
Expected: BUILD SUCCESSFUL (no unused-import or unresolved-symbol errors). If a module path is wrong, confirm it in `settings.gradle.kts`.

- [ ] **Step 9: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/modules/components/http-client/src/main/java/com/bytechef/component/http/client/cluster/HttpClientTool.java server/libs/modules/components/ai/agent/src/main/java/com/bytechef/component/ai/agent/tool/AiAgentChatTool.java server/libs/modules/components/ai/agentic-ai/src/main/java/com/bytechef/component/ai/agenticai/tool/AgenticAiTool.java server/libs/modules/components/workflow/src/main/java/com/bytechef/component/workflow/cluster/WorkflowCallWorkflowTool.java server/libs/modules/components/script/src/main/java/com/bytechef/component/script/cluster/tool/ScriptPythonTool.java server/libs/modules/components/script/src/main/java/com/bytechef/component/script/cluster/tool/ScriptJavaScriptTool.java server/libs/modules/components/script/src/main/java/com/bytechef/component/script/cluster/tool/ScriptRubyTool.java
git commit -m "Remove static toolName/toolDescription blocks from action-backed tools"
```

---

## Task 6: Regenerate affected component definition snapshots

**Context:** Component tests assert the component's generated definition JSON against `src/test/resources/definition/<component>.json`. Removing the two properties changes the static definitions of the `script`, `ai-agent`, `agentic-ai`, and `workflow` components (whose tools are static cluster elements). `http-client`'s tool is action-derived and may have no static snapshot. Per the project convention, delete the stale snapshot from BOTH `src/test/resources/definition/` and `build/resources/test/definition/`, then rerun to regenerate.

- [ ] **Step 1: Run the affected component tests to surface snapshot diffs**

Run:
```bash
./gradlew \
  :server:libs:modules:components:script:test \
  :server:libs:modules:components:ai:agent:test \
  :server:libs:modules:components:ai:agenticai:test \
  :server:libs:modules:components:workflow:test \
  :server:libs:modules:components:http-client:test
```
Expected: definition snapshot tests may FAIL with a JSON mismatch (the snapshot still contains `toolName`/`toolDescription`).

- [ ] **Step 2: Regenerate snapshots for each module whose definition test failed**

For each failing module `<modulePath>` with module directory `<moduleDir>`, delete the stale JSON and rerun:
```bash
rm -f <moduleDir>/src/test/resources/definition/*.json
rm -rf <moduleDir>/build/resources/test/definition
./gradlew <modulePath>:test
```
For example, for `script`:
```bash
rm -f server/libs/modules/components/script/src/test/resources/definition/*.json
rm -rf server/libs/modules/components/script/build/resources/test/definition
./gradlew :server:libs:modules:components:script:test
```
Expected: PASS (the test regenerates the JSON on first run, then asserts equal).

- [ ] **Step 3: Sanity-check the regenerated JSON**

Run: `git diff --stat server/libs/modules/components`
Expected: only definition JSON files changed, and the diff for each shows the `toolName`/`toolDescription` properties **removed** from the relevant cluster element(s). No other definition changes.

- [ ] **Step 4: Commit the regenerated snapshots**

```bash
git add server/libs/modules/components
git commit -m "Regenerate component definitions after removing static tool name/description"
```

---

## Task 7: Full verification

- [ ] **Step 1: Compile the whole server**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run the platform component + AI test modules**

Run:
```bash
./gradlew \
  :server:libs:platform:platform-component:platform-component-api:test \
  :server:libs:platform:platform-component:platform-component-service:test \
  :server:libs:platform:platform-ai:platform-ai-api:test
```
Expected: PASS

- [ ] **Step 3: Spotless + checks on touched modules**

Run: `./gradlew spotlessApply`
Then: `./gradlew :server:libs:platform:platform-component:platform-component-service:check`
Expected: BUILD SUCCESSFUL (Checkstyle/PMD/SpotBugs clean for the touched module).

- [ ] **Step 4: Final commit if spotless changed anything**

```bash
git status --porcelain
# if any files changed:
git add -A && git commit -m "Apply spotless formatting"
```

---

## Notes for the implementer

- **Do not** create a git worktree; work inline on the current branch (`0_732`). Make fresh commits — never `git commit --amend`.
- Only stage files you changed for the task at hand (the branch may carry unrelated parallel work).
- The runtime defaults are intentionally unchanged: when `toolName`/`toolDescription` are blank, `AbstractToolFacade.getToolName` derives the `COMPONENT_ELEMENT` name and `getToolDescription` falls back to the cluster element description. No consumption-side changes are in scope.
- `mcp-client` and `ai-agent-utils` are intentionally untouched: they use `toolName`/`TOOL_NAME` for unrelated purposes. The injection's idempotency guard (skip if a property of that name already exists) protects `mcp-client`'s functional `toolName` property automatically.
