# Custom Component Copilot Subagent (SP-C) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the AI Hub a dedicated custom-component copilot subagent (`custom_component_ask` + `custom_component_build`) that owns all custom-component CRUD, with the main AI Hub agent merely delegating via a `custom_component_agent` tool — mirroring the skills subagent.

**Architecture:** A CE delegating `ToolCallback` (`CustomComponentAgentToolCallback`, clone of `SkillsAgentToolCallback`) wraps an EE-defined sub-agent `ChatClient`. Two `ChatClient` beans (ask/build) live in a new EE `CustomComponentAgentConfiguration` (they wire EE `CustomComponentTools`/`ReadCustomComponentTools`, so they cannot live in the CE `CopilotConfiguration` where skills' CE beans live). `AiHubConfiguration` registers the delegating tool at both ASK and BUILD sites and unwinds SP-B's direct catalog registration of the CRUD tools.

**Tech Stack:** Java 25, Spring Boot 4, Spring AI 2.0.0 (`ChatClient`, `ToolCallback`, `ToolDefinition`), JUnit 5 + Mockito + AssertJ.

## Global Constraints

- EE files (`server/ee/**`) use the ByteChef Enterprise license header (not Apache 2.0) and carry a `@version ee` Javadoc tag. CE files (`server/libs/**`) use the Apache 2.0 header.
- `CopilotAgentType` enum additions are **append-only** (add at the end).
- The custom-component **build** subagent carries CRUD+read tools but **not** `openCustomComponentTab` — the main agent owns tab-opening (exactly like skills / `openSkillTab`).
- Do not touch SP-A settings behavior or SP-B's client panel / artifact wiring. SP-C is server-side only; no client changes, no enum/GraphQL/schema changes.
- Commit only files directly modified by each task. Never amend on `0_732`.
- Run `./gradlew spotlessApply` before each commit; `./gradlew check` at the end.

---

### Task 1: CE delegating tool — `CopilotAgentType` value + `CustomComponentAgentToolCallback`

**Files:**
- Modify: `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/CopilotAgentType.java`
- Create: `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/CustomComponentAgentToolCallback.java`
- Test: `server/libs/ai/ai-copilot/ai-copilot-tool/src/test/java/com/bytechef/ai/copilot/tool/CustomComponentAgentToolCallbackTest.java`

**Interfaces:**
- Consumes: `com.bytechef.ai.agent.tool.CurrentAgentContext`, `CurrentAgentContext.AgentBinding`, `com.bytechef.ai.agent.tool.AgentType`, `com.bytechef.ai.agent.tool.ToolErrors`, `com.bytechef.commons.util.JsonUtils`, Spring AI `ChatClient`/`ToolCallback`/`ToolDefinition`/`ToolContext`.
- Produces: `public class CustomComponentAgentToolCallback implements ToolCallback` with constructor `CustomComponentAgentToolCallback(ChatClient customComponentChatClient)`, tool name `"custom_component_agent"`, input record `CustomComponentAgentInput(String request)`. New enum constant `CopilotAgentType.CUSTOM_COMPONENT_AGENT` (key `"custom_component_agent"`).

- [ ] **Step 1: Append the enum constant**

In `CopilotAgentType.java`, add the new constant at the **end** of the enum constant list (after `SAMPLE_OUTPUT_AGENT("sample_output_agent", false)`), changing that line's trailing `;` appropriately:

```java
    SAMPLE_OUTPUT_AGENT("sample_output_agent", false),
    CUSTOM_COMPONENT_AGENT("custom_component_agent", false);
```

- [ ] **Step 2: Write the delegating callback**

Create `CustomComponentAgentToolCallback.java` (Apache 2.0 header — CE module). This is a clone of `SkillsAgentToolCallback` with custom-component wording:

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

package com.bytechef.ai.copilot.tool;

import com.bytechef.ai.agent.tool.AgentType;
import com.bytechef.ai.agent.tool.CurrentAgentContext;
import com.bytechef.ai.agent.tool.CurrentAgentContext.AgentBinding;
import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.commons.util.JsonUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * Hand-rolled Spring AI {@link ToolCallback} that exposes the Custom Component Copilot subagent to the parent ai_hub
 * agent.
 *
 * @author Ivica Cardic
 */
public class CustomComponentAgentToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(CustomComponentAgentToolCallback.class);

    private static final String DESCRIPTION =
        """
            Delegate a user request about custom components to a specialised Custom Component subagent.
            Custom components are single-file, user-authored components (JavaScript, Python, Ruby) that add
            new actions to the platform. The subagent owns the canonical behaviour for listing, explaining,
            creating, updating, and deleting custom components, including authoring and iterating the source
            until it compiles. Prefer calling it over reasoning about custom components directly. The result
            is a synthesised markdown report or, in build mode, a summary of the mutations performed including
            the affected custom component id and name.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "request": {
                        "type": "string",
                        "description": "The user request in natural language. Pass through verbatim — the subagent does its own task decomposition."
                    }
                },
                "required": ["request"]
            }""";

    private final ChatClient customComponentChatClient;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CustomComponentAgentToolCallback(ChatClient customComponentChatClient) {
        this.customComponentChatClient = customComponentChatClient;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("custom_component_agent")
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
            CustomComponentAgentInput input = JsonUtils.read(toolInput, CustomComponentAgentInput.class);

            String request = input.request();

            if (request == null || request.isBlank()) {
                return toolError("request is required and must not be blank");
            }

            AgentBinding parent = CurrentAgentContext.current();
            AgentType parentAgent = parent != null ? parent.agentName() : null;

            Map<String, Object> forwardedContext = toolContext == null ? Map.of() : toolContext.getContext();

            String result = CurrentAgentContext.callWith(
                CopilotAgentType.CUSTOM_COMPONENT_AGENT, parentAgent,
                () -> customComponentChatClient.prompt(request)
                    .toolContext(forwardedContext)
                    .call()
                    .content());

            if (result == null) {
                log.warn("custom_component subagent returned null for request='{}'", request);

                return ToolErrors.toolError("custom_component subagent returned null");
            }

            return result;
        } catch (JacksonException exception) {
            log.warn(
                "custom_component_agent rejected malformed tool input: {} — first 200 chars of input: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                CustomComponentAgentToolCallback.class, "custom_component_agent", exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(message);
    }

    public record CustomComponentAgentInput(String request) {
    }
}
```

- [ ] **Step 3: Write the test**

Create `CustomComponentAgentToolCallbackTest.java` — a clone of `SkillsAgentToolCallbackTest` with `custom_component_agent` / `CustomComponentAgentToolCallback` substituted. Full content:

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

package com.bytechef.ai.copilot.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CustomComponentAgentToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testCallReturnsResultWhenSubagentSucceeds() {
        String synthesised = "## Custom Components\n\n1. my-component — created and compiled.";

        ChatClient customComponentChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(customComponentChatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(synthesised);

        CustomComponentAgentToolCallback callback = new CustomComponentAgentToolCallback(customComponentChatClient);

        String result = callback.call("{\"request\":\"list my custom components\"}");

        assertThat(result).isEqualTo(synthesised);
    }

    @Test
    void testCallReturnsErrorWhenRequestIsBlank() {
        ChatClient customComponentChatClient = mock(ChatClient.class);

        CustomComponentAgentToolCallback callback = new CustomComponentAgentToolCallback(customComponentChatClient);

        String result = callback.call("{\"request\":\"   \"}");

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("request is required");
    }

    @Test
    void testCallReturnsErrorOnInvalidJson() {
        ChatClient customComponentChatClient = mock(ChatClient.class);

        CustomComponentAgentToolCallback callback = new CustomComponentAgentToolCallback(customComponentChatClient);

        String result = callback.call("not-json");

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("invalid tool input");
    }

    @Test
    void testCallReturnsToolErrorWhenSubagentReturnsNull() throws Exception {
        ChatClient customComponentChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(customComponentChatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(null);

        CustomComponentAgentToolCallback callback = new CustomComponentAgentToolCallback(customComponentChatClient);

        String result = callback.call("{\"request\":\"any request\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error"))
            .as("null subagent result must surface as a tool error")
            .isTrue();
        assertThat(node.get("error")
            .asText()).containsIgnoringCase("returned null");
    }

    @Test
    void testCallReturnsToolErrorWhenSubagentThrows() throws Exception {
        ChatClient customComponentChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(customComponentChatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenThrow(new RuntimeException("component repository unavailable"));

        CustomComponentAgentToolCallback callback = new CustomComponentAgentToolCallback(customComponentChatClient);

        String result = callback.call("{\"request\":\"any\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText())
                .as("payload must surface tool name for the LLM to recover")
                .contains("custom_component_agent failed")
                .as("payload must NOT leak the exception's getMessage() text — see ToolErrors.runtimeFailure")
                .doesNotContain("component repository unavailable");
    }

    @Test
    void testCallForwardsParentToolContextToSubagent() {
        ChatClient customComponentChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(customComponentChatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("ok");

        Map<String, Object> parentContextMap = Map.of("workspaceId", 11L, "userId", 42L);

        ToolContext parentToolContext = new ToolContext(parentContextMap);

        CustomComponentAgentToolCallback callback = new CustomComponentAgentToolCallback(customComponentChatClient);

        callback.call("{\"request\":\"any\"}", parentToolContext);

        verify(requestSpec).toolContext(parentContextMap);
    }

    @Test
    void testCallForwardsEmptyMapWhenParentToolContextIsNull() {
        ChatClient customComponentChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(customComponentChatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("ok");

        CustomComponentAgentToolCallback callback = new CustomComponentAgentToolCallback(customComponentChatClient);

        callback.call("{\"request\":\"any\"}", null);

        verify(requestSpec).toolContext(Map.of());
    }

    @Test
    void testToolDefinitionExposesCustomComponentAgentNameAndRequestSchema() {
        CustomComponentAgentToolCallback callback = new CustomComponentAgentToolCallback(mock(ChatClient.class));

        assertThat(callback.getToolDefinition()
            .name()).isEqualTo("custom_component_agent");
        assertThat(callback.getToolDefinition()
            .inputSchema()).contains("\"request\"");
    }

    private static Stream<Arguments> upstreamFailures() {
        return Stream.of(
            Arguments.of(WebClientResponseException.create(400, "Bad Request", null, null, null)),
            Arguments.of(WebClientResponseException.create(503, "Service Unavailable", null, null, null)),
            Arguments.of(new RuntimeException(new IOException("connection reset"))),
            Arguments.of(new RuntimeException(new TimeoutException("upstream timeout"))),
            Arguments.of(new NullPointerException("malformed response")));
    }

    @ParameterizedTest
    @MethodSource("upstreamFailures")
    void testCallSurfacesAllRuntimeExceptionTypesAsToolError(RuntimeException upstreamException) throws Exception {
        ChatClient customComponentChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(customComponentChatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenThrow(upstreamException);

        CustomComponentAgentToolCallback callback = new CustomComponentAgentToolCallback(customComponentChatClient);

        String result = callback.call("{\"request\":\"any\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error"))
            .as("every upstream RuntimeException must produce a typed tool-error payload, not propagate")
            .isTrue();
        assertThat(node.get("error")
            .asText()).contains("custom_component_agent failed");
    }

    private static void stubToolContext(ChatClientRequestSpec requestSpec) {
        when(requestSpec.toolContext(anyMap())).thenReturn(requestSpec);
    }
}
```

- [ ] **Step 4: Format, run the test**

Run: `./gradlew spotlessApply`
Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:test --tests "com.bytechef.ai.copilot.tool.CustomComponentAgentToolCallbackTest"`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 5: Commit**

```bash
git add server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/CopilotAgentType.java \
        server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/CustomComponentAgentToolCallback.java \
        server/libs/ai/ai-copilot/ai-copilot-tool/src/test/java/com/bytechef/ai/copilot/tool/CustomComponentAgentToolCallbackTest.java
git commit -m "732 Add CustomComponentAgentToolCallback delegating tool"
```

---

### Task 2: EE subagent prompts + `CustomComponentAgentConfiguration` (ChatClient beans)

**Files:**
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_custom_component_ask.txt`
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_custom_component_build.txt`
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/CustomComponentAgentConfiguration.java`

**Interfaces:**
- Consumes: EE `com.bytechef.ee.automation.ai.tool.CustomComponentTools`, `...ReadCustomComponentTools` (SP-B); Spring AI `ChatClient`, `ChatModel`.
- Produces: two `@Bean ChatClient` methods named exactly `customComponentAskSubAgentChatClient` and `customComponentBuildSubAgentChatClient` (bean names = method names; consumed by `AiHubConfiguration` via `@Qualifier` in Task 3).

- [ ] **Step 1: Write the ask prompt**

Create `prompt_custom_component_ask.txt`:

```
You are the Custom Component ASK subagent for ByteChef.

Custom components are single-file, user-authored components that add new actions to the platform.
Supported languages: JavaScript, Python, Ruby. (New components are created in JavaScript.)

Your job is READ-ONLY. Answer the user's questions about their existing custom components. You may:
- listCustomComponents — enumerate the user's custom components (id, name, title, language).
- getCustomComponentSource — read the full source of a custom component by id.

Never attempt to create, update, or delete a component in ASK mode. If the user asks to build or
change a component, tell them to switch to build mode (the parent agent will route the request to the
build subagent).

Respond with a concise markdown report. When you reference a component, include its id and name so the
parent agent can open it.
```

- [ ] **Step 2: Write the build prompt**

Create `prompt_custom_component_build.txt`:

```
You are the Custom Component BUILD subagent for ByteChef. You author and iterate single-file custom
components until they compile, then report the result to the parent agent.

## The component contract

A custom component is ONE source file that is evaluated by a GraalVM polyglot engine. The evaluated
result must be an object exposing these members:

- name        (String, REQUIRED) — the component key, e.g. "my-weather". Lowercase, digits, and
              hyphens only. This is the component's identity and CANNOT be changed by an update once
              the component exists.
- version     (Integer, REQUIRED) — start at 1.
- title       (String) — human-readable title.
- description (String) — short description.
- actions     (list of objects, each with):
    - name        (String, REQUIRED) — action key, e.g. "getWeather".
    - title       (String)
    - description (String)
    - perform     (function) — receives the action's input parameters and connection/context and
                  returns the action's output.

### JavaScript shape (the language you create in)

```
export default {
    name: 'my-component',
    version: 1,
    title: 'My Component',
    description: 'Does something useful.',
    actions: [
        {
            name: 'doThing',
            title: 'Do Thing',
            description: 'Performs the thing.',
            perform: (inputParameters, connectionParameters, context) => {
                // return the action output
                return {result: 'ok'};
            }
        }
    ]
};
```

## Your tools

- createCustomComponent(name, language) — creates an EMPTY component from a starter template and
  returns its id. Always pass language "JAVASCRIPT". The `name` must match the component-name rules
  above (lowercase/digits/hyphens); an invalid name is rejected.
- updateCustomComponentSource(id, content) — replaces the source. This is COMPILE-GATED: if the source
  does not evaluate to a valid component object, the call returns an error describing the failure.
  Iterate on the source and call again until it succeeds. Note: you cannot change the component `name`
  via an update — keep the same name you created it with.
- getCustomComponentSource(id) — read the current source (use it to inspect before editing an existing
  component).
- listCustomComponents() — enumerate existing components.
- deleteCustomComponent(id) — delete a component by id.

## Workflow for "build a component that does X"

1. Choose a valid component name and call createCustomComponent(name, "JAVASCRIPT") to get its id.
2. Write the full JavaScript source implementing the requested action(s) following the contract above.
3. Call updateCustomComponentSource(id, source). If it returns an error, fix the source and retry.
4. When it compiles, STOP and return a short markdown summary that INCLUDES the component id and name,
   so the parent agent can open it in the panel.

Do not open tabs yourself — the parent agent owns opening the component in the UI. Do not delete a
component unless the user explicitly asked to.
```

- [ ] **Step 3: Write the EE configuration**

Create `CustomComponentAgentConfiguration.java` (Enterprise license header + `@version ee`):

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import com.bytechef.ee.automation.ai.tool.CustomComponentTools;
import com.bytechef.ee.automation.ai.tool.ReadCustomComponentTools;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;

/**
 * Registers the custom-component Copilot subagent {@link ChatClient} beans consumed by the ai_hub agents via
 * {@code CustomComponentAgentToolCallback}. The ASK client is read-only; the BUILD client owns the full CRUD tool set
 * and authors/iterates component source until it compiles.
 *
 * <p>
 * These beans live in EE (not the CE {@code CopilotConfiguration}, where the skills subagent chat clients live) because
 * {@link CustomComponentTools}/{@link ReadCustomComponentTools} are EE tools. The subagents deliberately omit
 * {@code openCustomComponentTab} — the parent ai_hub agent owns opening the component in the panel.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class CustomComponentAgentConfiguration {

    @Bean
    ChatClient customComponentAskSubAgentChatClient(
        ChatModel chatModel, ReadCustomComponentTools readCustomComponentTools,
        @Value("classpath:prompt_custom_component_ask.txt") Resource promptResource) {

        return ChatClient.builder(chatModel)
            .defaultSystem(readPrompt(promptResource))
            .defaultTools(readCustomComponentTools)
            .build();
    }

    @Bean
    ChatClient customComponentBuildSubAgentChatClient(
        ChatModel chatModel, CustomComponentTools customComponentTools,
        ReadCustomComponentTools readCustomComponentTools,
        @Value("classpath:prompt_custom_component_build.txt") Resource promptResource) {

        return ChatClient.builder(chatModel)
            .defaultSystem(readPrompt(promptResource))
            .defaultTools(customComponentTools, readCustomComponentTools)
            .build();
    }

    private String readPrompt(Resource resource) {
        try {
            InputStream inputStream = resource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read custom component prompt resource: " + resource.getDescription(), exception);
        }
    }
}
```

- [ ] **Step 4: Verify EE header + compile**

Run: `./gradlew spotlessApply`
Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:compileJava`
Expected: BUILD SUCCESSFUL. (Spotless applies the EE header because the file contains `@version ee`.)

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_custom_component_ask.txt \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_custom_component_build.txt \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/CustomComponentAgentConfiguration.java
git commit -m "732 Add custom component subagent ChatClient beans + prompts"
```

---

### Task 3: Wire delegation into `AiHubConfiguration` + unwind SP-B direct catalog + main prompts

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubConfiguration.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_ask.txt`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_build.txt`

**Interfaces:**
- Consumes: `CustomComponentAgentToolCallback` (Task 1); `customComponentAskSubAgentChatClient` / `customComponentBuildSubAgentChatClient` (Task 2); existing `ProgressReportingToolCallback`.
- Produces: `custom_component_agent` tool registered on both ASK and BUILD ai_hub agents; SP-B's direct `CustomComponentTools`/`ReadCustomComponentTools` removed from the global catalogs.

- [ ] **Step 1: Add the import**

In `AiHubConfiguration.java`, add near the other copilot-tool imports (e.g. after the `SkillsAgentToolCallback` import at line 25):

```java
import com.bytechef.ai.copilot.tool.CustomComponentAgentToolCallback;
```

- [ ] **Step 2: Add the ASK-agent chat-client provider parameter + call-site arg**

In the ASK agent bean method, add the qualifier parameter next to `skillsAskSubAgentChatClientProvider` (line ~235):

```java
        @Qualifier("customComponentAskSubAgentChatClient") //
        ObjectProvider<ChatClient> customComponentAskSubAgentChatClientProvider,
```

Then extend the `registerCopilotSubAgentToolCallbacks(...)` call in that method (line ~334) to pass it as the final argument (see Step 4 for the new signature):

```java
        registerCopilotSubAgentToolCallbacks(
            toolCallbacks, skillsAskSubAgentChatClientProvider, clusterElementAskSubAgentChatClientProvider,
            codeEditorAskSubAgentChatClientProvider, workflowEditorAskSubAgentChatClientProvider, null,
            workflowExecutionAskSubAgentChatClientProvider, customComponentAskSubAgentChatClientProvider);
```

- [ ] **Step 3: Add the BUILD-agent chat-client provider parameter + call-site arg**

In the BUILD agent bean method, add the qualifier parameter next to `skillsBuildSubAgentChatClientProvider` (line ~397):

```java
        @Qualifier("customComponentBuildSubAgentChatClient") //
        ObjectProvider<ChatClient> customComponentBuildSubAgentChatClientProvider,
```

Then extend the `registerCopilotSubAgentToolCallbacks(...)` call in that method (line ~506) to pass it as the final argument:

```java
        registerCopilotSubAgentToolCallbacks(
            toolCallbacks, skillsBuildSubAgentChatClientProvider, clusterElementBuildSubAgentChatClientProvider,
            codeEditorBuildSubAgentChatClientProvider, workflowEditorBuildSubAgentChatClientProvider,
            converterBuildSubAgentChatClientSupplierProvider, workflowExecutionBuildSubAgentChatClientProvider,
            customComponentBuildSubAgentChatClientProvider);
```

- [ ] **Step 4: Extend `registerCopilotSubAgentToolCallbacks` to register `custom_component_agent`**

Change the method signature (line ~765) to add the new trailing parameter, and add the registration block at the end of the method body (after the converter block):

```java
    private static void registerCopilotSubAgentToolCallbacks(
        List<ToolCallback> toolCallbacks,
        ObjectProvider<ChatClient> skillsSubAgentChatClientProvider,
        ObjectProvider<ChatClient> clusterElementSubAgentChatClientProvider,
        ObjectProvider<ChatClient> codeEditorSubAgentChatClientProvider,
        ObjectProvider<ChatClient> workflowEditorSubAgentChatClientProvider,
        @Nullable ObjectProvider<Supplier<ChatClient>> converterSubAgentChatClientSupplierProvider,
        ObjectProvider<ChatClient> workflowExecutionSubAgentChatClientProvider,
        ObjectProvider<ChatClient> customComponentSubAgentChatClientProvider) {
```

At the end of the method body, add:

```java
        customComponentSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new CustomComponentAgentToolCallback(chatClient), "custom_component_agent")));
```

- [ ] **Step 5: Unwind SP-B's direct catalog registration**

In `aiHubAskGlobalToolCatalog` (line ~658), remove the `ReadCustomComponentTools` parameter and its use:

```java
    @Bean
    AiHubGlobalToolCatalog aiHubAskGlobalToolCatalog(
        ReadProjectTools readProjectTools, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools, TaskDispatcherTools taskDispatcherTools) {

        return globalToolCatalog(
            ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID, readProjectTools, readProjectWorkflowTools, componentTools,
            taskTools, taskDispatcherTools);
    }
```

In `aiHubBuildGlobalToolCatalog` (line ~670), remove both `CustomComponentTools` and `ReadCustomComponentTools`:

```java
    @Bean
    AiHubGlobalToolCatalog aiHubBuildGlobalToolCatalog(
        ProjectTools projectTools, ProjectWorkflowTools projectWorkflowTools, ComponentTools componentTools,
        TaskTools taskTools, TaskDispatcherTools taskDispatcherTools, ScriptTools scriptTools,
        ClusterElementTools clusterElementTools) {

        return globalToolCatalog(
            ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID, projectTools, projectWorkflowTools, componentTools,
            taskTools, taskDispatcherTools, scriptTools, clusterElementTools);
    }
```

Then remove the now-unused imports if they are no longer referenced anywhere else in the file:

```java
import com.bytechef.ee.automation.ai.tool.CustomComponentTools;
import com.bytechef.ee.automation.ai.tool.ReadCustomComponentTools;
```

Verify with: `grep -n "CustomComponentTools\|ReadCustomComponentTools" server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubConfiguration.java` — expect **no** matches after removal (the subagent beans in Task 2 own these tools now). Keep the `OpenCustomComponentTabToolCallback` import and its two `toolCallbacks.add(...)` sites (lines ~292, ~463) untouched.

- [ ] **Step 6: Update the main-agent prompts**

In `prompt_ai_hub_build.txt`, find the custom-component guidance SP-B added (it references creating/updating/deleting custom components directly and `openCustomComponentTab`). Replace the direct-CRUD instructions with delegation, keeping the tab-open guidance. Add/replace with a block like:

```
### Custom components

Custom components are single-file, user-authored components that add new actions to the platform. Do
NOT create, update, or delete custom components directly. Instead delegate to the custom_component_agent
tool, passing the user's request verbatim. It returns a summary including the affected component's id
and name. After a component is built or changed, call openCustomComponentTab({customComponentId, name})
to show it to the user in the panel.
```

In `prompt_ai_hub_ask.txt`, replace any direct read guidance with:

```
### Custom components

To answer questions about the user's custom components (listing, explaining, reading source), delegate
to the custom_component_agent tool, passing the user's request verbatim. To show a specific component,
call openCustomComponentTab({customComponentId, name}).
```

(If SP-B did not add explicit custom-component prose to these prompts, add the blocks above near the other tool guidance. Keep the existing `openCustomComponentTab` documentation intact.)

- [ ] **Step 7: Format + compile + spotbugs**

Run: `./gradlew spotlessApply`
Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:compileJava :server:ee:libs:ai:ai-hub:ai-hub-service:spotbugsMain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubConfiguration.java \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_ask.txt \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_build.txt
git commit -m "732 Delegate custom component work to subagent; unwind direct tools"
```

---

### Task 4: Full verification

**Files:** none (verification only).

- [ ] **Step 1: Build the two affected modules + tests**

Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:check :server:ee:libs:ai:ai-hub:ai-hub-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Confirm the Spring context wiring is consistent**

Run: `grep -n "customComponent" server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubConfiguration.java`
Expected: matches only for the two `@Qualifier(...customComponent...SubAgentChatClient)` provider params, the two `registerCopilotSubAgentToolCallbacks` call-site args, and the `CustomComponentAgentToolCallback` registration — and NO `CustomComponentTools`/`ReadCustomComponentTools` references remain.

- [ ] **Step 3: Run the broader check on the copilot-tool module**

Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:test`
Expected: all tests pass, including `CustomComponentAgentToolCallbackTest`.

- [ ] **Step 4: Final spotless + no commit needed**

Run: `./gradlew spotlessCheck` (targeted to the two modules if faster).
Expected: no formatting violations. No new commit unless spotless changed files (then commit them per the affected task).

---

## Self-Review

**Spec coverage:**
- Spec §1 (build prompt / ask prompt knowledge) → Task 2 Steps 1–2.
- Spec §2 (EE subagent ChatClient beans, module-placement note) → Task 2 Step 3.
- Spec §3 (`CustomComponentAgentToolCallback` + `CopilotAgentType.CUSTOM_COMPONENT_AGENT`) → Task 1.
- Spec §4 (AiHubConfiguration wiring, unwind SP-B, main prompt updates) → Task 3.
- Spec "Testing" → Task 1 Step 3 (callback test), Task 4 (build/wiring verification).
- Spec "Rollout" (no enum/schema/client changes) → honored: only CE `CopilotAgentType` append + EE beans/prompts + AiHubConfiguration edits.

**Placeholder scan:** none — all code blocks are complete; prompt text is final content.

**Type consistency:** bean names `customComponentAskSubAgentChatClient` / `customComponentBuildSubAgentChatClient` (Task 2) match the `@Qualifier` strings and the call-site provider names (Task 3). Tool name `custom_component_agent` and enum constant `CUSTOM_COMPONENT_AGENT` are consistent across Task 1 and Task 3. `CustomComponentAgentToolCallback(ChatClient)` constructor signature matches its use in Task 3 Step 4.

**Note on ask vs build routing:** `registerCopilotSubAgentToolCallbacks` is a shared helper called once from the ASK bean method and once from the BUILD bean method. The ASK call passes `customComponentAskSubAgentChatClient`; the BUILD call passes `customComponentBuildSubAgentChatClient`. Same `custom_component_agent` tool name on both agents, backed by the mode-appropriate sub-agent — identical to how skills routes `skills_agent`.
