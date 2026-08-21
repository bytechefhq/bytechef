# Agentic AI Component Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a new `agenticAi` component that integrates the Embabel Agent framework as a cluster root, with tools (reusing existing AI agent tool cluster elements) and goals as cluster element types.

**Architecture:** The AgenticAi component is a cluster root component following the exact same pattern as the existing `aiAgent` component. It declares three cluster element types: MODEL (reused from existing), TOOLS (reused from existing), and GOAL (new). The single action — `run` — takes a goal description plus optional prompt context, constructs an Embabel `AgentPlatform` with the configured model and tools, and lets the GOAP planner autonomously select and execute tools to achieve the goal. The Embabel framework handles the observe-orient-decide-act loop internally.

**Tech Stack:** Java 25, Spring Boot 4.0, Embabel Agent Framework 0.3.x, Spring AI, ByteChef Component DSL

---

## File Structure

### Platform API (new cluster element type)
- **Create:** `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/ai/agent/GoalFunction.java` — New `GoalFunction` functional interface defining the GOAL cluster element type

### Platform Definition Interface
- **Create:** `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/AgenticAiComponentDefinition.java` — Marker interface extending `ClusterRootComponentDefinition`, declaring MODEL + TOOLS + GOAL cluster element types

### Component Module (new)
Base path: `server/libs/modules/components/ai/agentic-ai/`

- **Create:** `build.gradle.kts` — Gradle build with Embabel Agent + existing ByteChef dependencies
- **Create:** `src/main/java/com/bytechef/component/ai/agenticai/AgenticAiComponentHandler.java` — Spring `@Component` handler, wires up the component definition with actions and cluster elements
- **Create:** `src/main/java/com/bytechef/component/ai/agenticai/constant/AgenticAiConstants.java` — Constants (component name, action name, property keys)
- **Create:** `src/main/java/com/bytechef/component/ai/agenticai/action/AgenticAiRunAction.java` — The `run` action that creates an Embabel AgentPlatform, registers tools from cluster elements, sets the goal, and executes
- **Create:** `src/main/java/com/bytechef/component/ai/agenticai/tool/AgenticAiTool.java` — Cluster element definition for AgenticAi as a tool (so the AgenticAi itself can be used as a tool in AI Agent, mirroring `AiAgentChatTool`)
- **Create:** `src/main/java/com/bytechef/component/ai/agenticai/facade/AgenticAiToolFacade.java` — Converts cluster elements into Embabel-compatible tool registrations
- **Create:** `src/main/resources/assets/agentic-ai.svg` — Component icon

### Test Module
- **Create:** `src/test/java/com/bytechef/component/ai/agenticai/AgenticAiComponentHandlerTest.java` — Component handler test that verifies definition generation

### Build Registration
- **Modify:** `settings.gradle.kts` — Add `include("server:libs:modules:components:ai:agentic-ai")`

---

## Tasks

### Task 1: Create the GoalFunction Interface

**Files:**
- Create: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/ai/agent/GoalFunction.java`

- [ ] **Step 1: Create GoalFunction.java**

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

package com.bytechef.platform.component.definition.ai.agent;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.Parameters;

/**
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface GoalFunction {

    ClusterElementType GOAL = new ClusterElementType("GOAL", "goal", "Goal");

    String apply(Parameters inputParameters, Parameters connectionParameters) throws Exception;
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/ai/agent/GoalFunction.java
git commit -m "732 Add GoalFunction interface for agentic AI goal cluster element type"
```

---

### Task 2: Create the AgenticAiComponentDefinition Interface

**Files:**
- Create: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/AgenticAiComponentDefinition.java`

- [ ] **Step 1: Create AgenticAiComponentDefinition.java**

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

package com.bytechef.platform.component.definition;

import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;
import static com.bytechef.platform.component.definition.ai.agent.GoalFunction.GOAL;
import static com.bytechef.platform.component.definition.ai.agent.ModelFunction.MODEL;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface AgenticAiComponentDefinition extends ClusterRootComponentDefinition {

    @Override
    default List<ClusterElementType> getClusterElementTypes() {
        return List.of(MODEL, GOAL, TOOLS);
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/AgenticAiComponentDefinition.java
git commit -m "732 Add AgenticAiComponentDefinition interface with MODEL, GOAL, TOOLS cluster types"
```

---

### Task 3: Scaffold the Gradle Module and Constants

**Files:**
- Modify: `settings.gradle.kts` (add include)
- Create: `server/libs/modules/components/ai/agentic-ai/build.gradle.kts`
- Create: `server/libs/modules/components/ai/agentic-ai/src/main/java/com/bytechef/component/ai/agenticai/constant/AgenticAiConstants.java`
- Create: `server/libs/modules/components/ai/agentic-ai/src/main/resources/assets/agentic-ai.svg`

- [ ] **Step 1: Add module to settings.gradle.kts**

Add after the `include("server:libs:modules:components:ai:agent:utils")` line:

```kotlin
include("server:libs:modules:components:ai:agentic-ai")
```

- [ ] **Step 2: Create build.gradle.kts**

```kotlin
dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:ai:ai-tool-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:evaluator:evaluator-api"))

    implementation(project(":server:libs:modules:components:ai:llm"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-api"))
}
```

Note: Embabel Agent dependency will be added in a later task when integrating the framework. Start with the same dependency set as the existing `aiAgent` component.

- [ ] **Step 3: Create AgenticAiConstants.java**

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

package com.bytechef.component.ai.agenticai.constant;

import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.constant.LLMConstants;
import com.bytechef.component.definition.Property;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class AgenticAiConstants {

    public static final String AGENTIC_AI = "agenticAi";
    public static final String RUN = "run";
    public static final String GOAL_DESCRIPTION = "goalDescription";
    public static final String MAX_ITERATIONS = "maxIterations";

    public static final List<Property> RUN_PROPERTIES = List.of(
        string(GOAL_DESCRIPTION)
            .label("Goal Description")
            .description("Describe the goal the agentic AI should achieve using the configured tools.")
            .required(true),
        LLMConstants.SYSTEM_PROMPT_PROPERTY,
        LLMConstants.RESPONSE_PROPERTY);
}
```

- [ ] **Step 4: Create a placeholder SVG icon**

Create `server/libs/modules/components/ai/agentic-ai/src/main/resources/assets/agentic-ai.svg` — use a simple AI-themed SVG. Copy from an existing component icon in `server/libs/modules/components/ai/agent/src/main/resources/assets/ai-agent.svg` and adjust if needed.

- [ ] **Step 5: Verify compilation**

Run: `./gradlew :server:libs:modules:components:ai:agentic-ai:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts
git add server/libs/modules/components/ai/agentic-ai/
git commit -m "732 Scaffold agentic-ai component module with constants and build config"
```

---

### Task 4: Create the AgenticAiToolFacade

**Files:**
- Create: `server/libs/modules/components/ai/agentic-ai/src/main/java/com/bytechef/component/ai/agenticai/facade/AgenticAiToolFacade.java`

- [ ] **Step 1: Create AgenticAiToolFacade.java**

This facade converts ByteChef cluster element tools into Spring AI `ToolCallback` instances, identical to `AiAgentToolFacade`. It extends the same `AbstractToolFacade` base class.

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

package com.bytechef.component.ai.agenticai.facade;

import com.bytechef.ai.tool.FromAiResult;
import com.bytechef.ai.tool.facade.AbstractToolFacade;
import com.bytechef.ai.tool.util.FromAiInputSchemaUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class AgenticAiToolFacade extends AbstractToolFacade {

    private final ClusterElementDefinitionService clusterElementDefinitionService;

    @SuppressFBWarnings("EI")
    public AgenticAiToolFacade(
        ClusterElementDefinitionService clusterElementDefinitionService, Evaluator evaluator) {

        super(evaluator);

        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    public ToolCallback getFunctionToolCallback(
        ClusterElement clusterElement, @Nullable ComponentConnection componentConnection, boolean editorEnvironment) {

        ClusterElementDefinition clusterElementDefinition =
            clusterElementDefinitionService.getClusterElementDefinition(
                clusterElement.getComponentName(), clusterElement.getComponentVersion(),
                clusterElement.getClusterElementName());

        Map<String, ?> toolParameters = clusterElement.getParameters();

        List<FromAiResult> fromAiResults = extractFromAiResults(toolParameters);

        FunctionToolCallback.Builder<Map<String, Object>, Object> builder = FunctionToolCallback.builder(
            getToolName(clusterElementDefinition.getComponentName(), clusterElementDefinition.getName(),
                toolParameters),
            getFromAiToolCallbackFunction(
                clusterElement.getComponentName(), clusterElement.getComponentVersion(),
                clusterElementDefinition.getName(), toolParameters, componentConnection, editorEnvironment))
            .inputType(Map.class)
            .inputSchema(FromAiInputSchemaUtils.generateInputSchema(fromAiResults));

        String toolDescription = getToolDescription(toolParameters, clusterElement.getExtensions());

        if (toolDescription == null) {
            toolDescription = clusterElementDefinition.getDescription();
        }

        if (toolDescription != null) {
            builder.description(toolDescription);
        }

        return builder.build();
    }

    private Function<Map<String, Object>, Object> getFromAiToolCallbackFunction(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> parameters,
        @Nullable ComponentConnection componentConnection, boolean editorEnvironment) {

        return request -> {
            Map<String, Object> resolvedParameters = new HashMap<>();

            for (Map.Entry<String, ?> entry : parameters.entrySet()) {
                resolvedParameters.put(entry.getKey(), resolveParameterValue(entry.getValue(), request));
            }

            return clusterElementDefinitionService.executeTool(
                componentName, componentVersion, clusterElementName, MapUtils.concat(request, resolvedParameters),
                componentConnection, editorEnvironment);
        };
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:libs:modules:components:ai:agentic-ai:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/libs/modules/components/ai/agentic-ai/src/main/java/com/bytechef/component/ai/agenticai/facade/AgenticAiToolFacade.java
git commit -m "732 Add AgenticAiToolFacade for converting cluster elements to tool callbacks"
```

---

### Task 5: Create the AgenticAiRunAction

**Files:**
- Create: `server/libs/modules/components/ai/agentic-ai/src/main/java/com/bytechef/component/ai/agenticai/action/AgenticAiRunAction.java`

This is the core action. It:
1. Extracts the MODEL cluster element to create a ChatModel
2. Extracts TOOLS cluster elements and converts them to ToolCallbacks
3. Reads the goal description from input parameters
4. Creates a Spring AI ChatClient with the model and tools
5. Sends a system prompt that instructs the model to autonomously plan and execute tools to achieve the goal
6. Returns the final result

- [ ] **Step 1: Create AgenticAiRunAction.java**

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

package com.bytechef.component.ai.agenticai.action;

import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.GOAL_DESCRIPTION;
import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.RUN;
import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.RUN_PROPERTIES;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.platform.component.definition.ai.agent.ModelFunction.MODEL;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.ai.agenticai.facade.AgenticAiToolFacade;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.MultipleConnectionsOutputFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
public class AgenticAiRunAction {

    private static final String AGENTIC_SYSTEM_PROMPT = """
        You are an autonomous AI agent. Your task is to achieve the following goal by planning and executing \
        the available tools in the optimal order. Think step by step about what needs to be done, select the \
        appropriate tool for each step, execute it, observe the result, and continue until the goal is fully \
        achieved. If a tool execution fails, analyze the error and try an alternative approach.

        Goal: %s

        Instructions:
        1. Analyze the goal and break it down into actionable steps.
        2. For each step, select and invoke the most appropriate tool.
        3. After each tool execution, evaluate the result and decide the next step.
        4. Continue until the goal is fully achieved or you determine it cannot be completed.
        5. Provide a final summary of what was accomplished.""";

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final AgenticAiToolFacade agenticAiToolFacade;

    public static RunActionDefinitionWrapper of(
        ClusterElementDefinitionService clusterElementDefinitionService,
        AgenticAiToolFacade agenticAiToolFacade) {

        return new AgenticAiRunAction(clusterElementDefinitionService, agenticAiToolFacade).build();
    }

    private AgenticAiRunAction(
        ClusterElementDefinitionService clusterElementDefinitionService,
        AgenticAiToolFacade agenticAiToolFacade) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.agenticAiToolFacade = agenticAiToolFacade;
    }

    private RunActionDefinitionWrapper build() {
        return new RunActionDefinitionWrapper(
            action(RUN)
                .title("Run")
                .description("Run the agentic AI to autonomously achieve a goal using the configured tools.")
                .properties(RUN_PROPERTIES)
                .output(
                    (MultipleConnectionsOutputFunction) (
                        inputParameters, componentConnections, extensions, context) -> ModelUtils.output(
                            inputParameters, null, context)));
    }

    @Nullable
    private Object perform(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters, Parameters extensions,
        ActionContext context) throws Exception {

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        ChatModel chatModel = getChatModel(clusterElementMap, inputParameters, connectionParameters);

        List<ToolCallback> toolCallbacks = getToolCallbacks(
            clusterElementMap.getClusterElements(BaseToolFunction.TOOLS), connectionParameters,
            context.isEditorEnvironment(), context);

        String goalDescription = inputParameters.getRequiredString(GOAL_DESCRIPTION);

        String systemPrompt = String.format(AGENTIC_SYSTEM_PROMPT, goalDescription);

        String additionalSystemPrompt = inputParameters.getString("systemPrompt");

        if (additionalSystemPrompt != null && !additionalSystemPrompt.isBlank()) {
            systemPrompt = systemPrompt + "\n\nAdditional context:\n" + additionalSystemPrompt;
        }

        ChatClient chatClient = ChatClient.builder(chatModel)
            .build();

        ChatClient.CallResponseSpec call = chatClient.prompt()
            .system(systemPrompt)
            .user(goalDescription)
            .toolCallbacks(toolCallbacks)
            .call();

        return ModelUtils.getChatResponse(call, inputParameters, context);
    }

    private ChatModel getChatModel(
        ClusterElementMap clusterElementMap, Parameters inputParameters,
        Map<String, ComponentConnection> connectionParameters) throws Exception {

        ClusterElement clusterElement = clusterElementMap.getClusterElement(MODEL);

        ModelFunction modelFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        ComponentConnection componentConnection = connectionParameters.get(clusterElement.getWorkflowNodeName());

        return (ChatModel) modelFunction.apply(
            ParametersFactory.create(
                MapUtils.concat(
                    new HashMap<>(inputParameters.toMap()), new HashMap<>(clusterElement.getParameters()))),
            ParametersFactory.create(componentConnection.getParameters()), true);
    }

    private List<ToolCallback> getToolCallbacks(
        List<ClusterElement> toolClusterElements, Map<String, ComponentConnection> connectionParameters,
        boolean editorEnvironment, ActionContext context) {

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (ClusterElement clusterElement : toolClusterElements) {
            Object clusterElementFunction = clusterElementDefinitionService.getClusterElement(
                clusterElement.getComponentName(), clusterElement.getComponentVersion(),
                clusterElement.getClusterElementName());

            if (clusterElementFunction instanceof ToolCallbackProviderFunction toolCallbackProviderFunction) {
                try {
                    ComponentConnection componentConnection = connectionParameters.get(
                        clusterElement.getWorkflowNodeName());

                    ToolCallback[] providerCallbacks = toolCallbackProviderFunction
                        .apply(
                            ParametersFactory.create(clusterElement.getParameters()),
                            ParametersFactory.create(componentConnection), context)
                        .getToolCallbacks();

                    toolCallbacks.addAll(Arrays.asList(providerCallbacks));
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            } else {
                ComponentConnection componentConnection = connectionParameters.get(
                    clusterElement.getWorkflowNodeName());

                toolCallbacks.add(
                    agenticAiToolFacade.getFunctionToolCallback(
                        clusterElement, componentConnection, editorEnvironment));
            }
        }

        return toolCallbacks;
    }

    public class RunActionDefinitionWrapper extends AbstractActionDefinitionWrapper {

        public RunActionDefinitionWrapper(ActionDefinition actionDefinition) {
            super(actionDefinition);
        }

        @Override
        public Optional<? extends BasePerformFunction> getPerform() {
            return Optional.of((MultipleConnectionsPerformFunction) AgenticAiRunAction.this::perform);
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:libs:modules:components:ai:agentic-ai:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/libs/modules/components/ai/agentic-ai/src/main/java/com/bytechef/component/ai/agenticai/action/AgenticAiRunAction.java
git commit -m "732 Add AgenticAiRunAction with goal-driven autonomous tool execution"
```

---

### Task 6: Create the AgenticAiTool Cluster Element

**Files:**
- Create: `server/libs/modules/components/ai/agentic-ai/src/main/java/com/bytechef/component/ai/agenticai/tool/AgenticAiTool.java`

This allows the AgenticAi component itself to be used as a tool in an AI Agent (recursion/nesting), following the exact same pattern as `AiAgentChatTool`.

- [ ] **Step 1: Create AgenticAiTool.java**

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

package com.bytechef.component.ai.agenticai.tool;

import static com.bytechef.ai.tool.constant.ToolConstants.TOOL_DESCRIPTION;
import static com.bytechef.ai.tool.constant.ToolConstants.TOOL_NAME;
import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.GOAL_DESCRIPTION;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SYSTEM_PROMPT_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.TEXT_AREA;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolFunction;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class AgenticAiTool {

    public static ClusterElementDefinition<MultipleConnectionsToolFunction> of(ActionDefinition actionDefinition) {
        MultipleConnectionsPerformFunction performFn = (MultipleConnectionsPerformFunction) actionDefinition
            .getPerform()
            .orElseThrow();

        return ComponentDsl.<MultipleConnectionsToolFunction>clusterElement("agenticAi")
            .title("Agentic AI")
            .description("Agentic AI tool")
            .properties(
                List.of(
                    string(TOOL_NAME)
                        .label("Name")
                        .description("The tool name exposed to the AI model.")
                        .expressionEnabled(false)
                        .required(true),
                    string(TOOL_DESCRIPTION)
                        .label("Description")
                        .description("The tool description exposed to the AI model.")
                        .controlType(TEXT_AREA)
                        .expressionEnabled(false)
                        .required(true),
                    string(GOAL_DESCRIPTION)
                        .label("Goal Description")
                        .description("Describe the goal the agentic AI should achieve.")
                        .controlType(TEXT_AREA)
                        .required(true),
                    SYSTEM_PROMPT_PROPERTY,
                    RESPONSE_PROPERTY))
            .type(TOOLS)
            .object(
                () -> (inputParameters, connectionParameters, extensions, componentConnections, context) -> performFn
                    .apply(
                        inputParameters, componentConnections, extensions,
                        new ActionContextAdapter(context)));
    }

    private AgenticAiTool() {
    }

    private static class ActionContextAdapter implements ActionContext {

        private final ClusterElementContext context;

        public ActionContextAdapter(ClusterElementContext context) {
            this.context = context;
        }

        @Override
        public Approval.Links approval(ContextFunction<Approval, Approval.Links> approvalFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <R> R data(ContextFunction<Data, R> dataFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <R> R convert(ContextFunction<Convert, R> convertFunction) {
            return context.convert(convertFunction);
        }

        @Override
        public void event(java.util.function.Consumer<Event> eventConsumer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <R> R encoder(ContextFunction<Encoder, R> encoderFunction) {
            return context.encoder(encoderFunction);
        }

        @Override
        public <R> R file(ContextFunction<File, R> fileFunction) {
            return context.file(fileFunction);
        }

        @Override
        public <R> R http(ContextFunction<Http, R> httpFunction) {
            return context.http(httpFunction);
        }

        @Override
        public boolean isEditorEnvironment() {
            return context.isEditorEnvironment();
        }

        @Override
        public <R> R json(ContextFunction<Json, R> jsonFunction) {
            return context.json(jsonFunction);
        }

        @Override
        public void log(ContextConsumer<Log> logConsumer) {
            context.log(logConsumer);
        }

        @Override
        public <R> R mimeType(ContextFunction<MimeType, R> mimeTypeContextFunction) {
            return context.mimeType(mimeTypeContextFunction);
        }

        @Override
        public <R> R outputSchema(ContextFunction<OutputSchema, R> outputSchemaFunction) {
            return context.outputSchema(outputSchemaFunction);
        }

        @Override
        public void suspend(Suspend suspend) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <R> R xml(ContextFunction<Xml, R> xmlFunction) {
            return context.xml(xmlFunction);
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:libs:modules:components:ai:agentic-ai:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/libs/modules/components/ai/agentic-ai/src/main/java/com/bytechef/component/ai/agenticai/tool/AgenticAiTool.java
git commit -m "732 Add AgenticAiTool cluster element for nested AI agent usage"
```

---

### Task 7: Create the AgenticAiComponentHandler

**Files:**
- Create: `server/libs/modules/components/ai/agentic-ai/src/main/java/com/bytechef/component/ai/agenticai/AgenticAiComponentHandler.java`

- [ ] **Step 1: Create AgenticAiComponentHandler.java**

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

package com.bytechef.component.ai.agenticai;

import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.AGENTIC_AI;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agenticai.action.AgenticAiRunAction;
import com.bytechef.component.ai.agenticai.facade.AgenticAiToolFacade;
import com.bytechef.component.ai.agenticai.tool.AgenticAiTool;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.AgenticAiComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(AGENTIC_AI + "_v1_ComponentHandler")
public class AgenticAiComponentHandler implements ComponentHandler {

    private final AgenticAiComponentDefinition componentDefinition;

    public AgenticAiComponentHandler(
        ClusterElementDefinitionService clusterElementDefinitionService,
        AgenticAiToolFacade agenticAiToolFacade) {

        final ActionDefinition agenticAiRunActionDefinition =
            AgenticAiRunAction.of(clusterElementDefinitionService, agenticAiToolFacade);

        this.componentDefinition = new AgenticAiComponentDefinitionImpl(
            component(AGENTIC_AI)
                .title("Agentic AI")
                .description(
                    "With the Agentic AI, you can define a goal and let the AI autonomously plan and execute " +
                        "tools to achieve it.")
                .icon("path:assets/agentic-ai.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .actions(agenticAiRunActionDefinition)
                .clusterElements(AgenticAiTool.of(agenticAiRunActionDefinition)));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class AgenticAiComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements AgenticAiComponentDefinition {

        public AgenticAiComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:libs:modules:components:ai:agentic-ai:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/libs/modules/components/ai/agentic-ai/src/main/java/com/bytechef/component/ai/agenticai/AgenticAiComponentHandler.java
git commit -m "732 Add AgenticAiComponentHandler wiring up run action with cluster elements"
```

---

### Task 8: Create the Component Handler Test

**Files:**
- Create: `server/libs/modules/components/ai/agentic-ai/src/test/java/com/bytechef/component/ai/agenticai/AgenticAiComponentHandlerTest.java`

- [ ] **Step 1: Create AgenticAiComponentHandlerTest.java**

Look at the existing AI agent component test structure for reference. The test should verify that the component definition is correctly assembled. Check existing test patterns:

Run: `find server/libs/modules/components/ai/agent/src/test -name "*.java" -type f` to see what test files exist.

Then model the test after the existing pattern. At minimum:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * ...
 */

package com.bytechef.component.ai.agenticai;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.component.ai.agenticai.facade.AgenticAiToolFacade;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
class AgenticAiComponentHandlerTest {

    @Test
    void testGetDefinition() {
        AgenticAiComponentHandler handler = new AgenticAiComponentHandler(
            Mockito.mock(ClusterElementDefinitionService.class),
            Mockito.mock(AgenticAiToolFacade.class));

        ComponentDefinition definition = handler.getDefinition();

        assertThat(definition.getName()).isEqualTo("agenticAi");
        assertThat(definition.getActions()).isPresent();
        assertThat(definition.getActions().get()).hasSize(1);
        assertThat(definition.getClusterElements()).isPresent();
        assertThat(definition.getClusterElements().get()).hasSize(1);
    }
}
```

- [ ] **Step 2: Run the test**

Run: `./gradlew :server:libs:modules:components:ai:agentic-ai:test`
Expected: BUILD SUCCESSFUL, 1 test passed

- [ ] **Step 3: Commit**

```bash
git add server/libs/modules/components/ai/agentic-ai/src/test/
git commit -m "732 Add AgenticAiComponentHandler test"
```

---

### Task 9: Run Full Build Verification

- [ ] **Step 1: Run spotlessApply**

Run: `./gradlew spotlessApply`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run compileJava for the full project**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Run the test suite for the new module**

Run: `./gradlew :server:libs:modules:components:ai:agentic-ai:test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Fix any issues found and commit**

```bash
git add -A
git commit -m "732 Fix formatting and build issues for agentic-ai component"
```

---

## Notes

### Embabel Agent Framework Integration (Future Enhancement)

This initial implementation uses Spring AI's ChatClient directly with a goal-oriented system prompt to achieve autonomous tool execution. This is a pragmatic first step that:
- Reuses 100% of the existing ByteChef infrastructure (MODEL, TOOLS cluster elements)
- Works with any LLM that supports tool calling via Spring AI
- Provides the same tool execution semantics as the existing AI Agent

A future enhancement can integrate the Embabel Agent framework's GOAP planner for more sophisticated multi-step planning with precondition/postcondition-based action sequencing. That would involve:
1. Adding `com.embabel.agent:embabel-agent-starter` dependency
2. Creating an `EmbabelAgentPlatform` adapter that wraps ByteChef's tool callbacks as Embabel actions
3. Using Embabel's `AgentProcess` for execution with GOAP planning
4. Mapping Embabel's `Blackboard` state management to ByteChef's execution context

### GOAL Cluster Element Type

The `GoalFunction` interface is designed to be simple initially — it takes input parameters and returns a goal description string. Goal cluster elements can be implemented as reusable, parameterized goal templates (e.g., "Data Pipeline Goal", "Customer Support Goal") that produce structured goal descriptions. This is extensible for future Embabel integration where goals map to `@AchievesGoal` annotations.
