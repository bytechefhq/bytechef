# Sample-Output Copilot Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the one-shot "Generate with AI" bar in the *Upload Sample Output Data* dialog with an embedded, side-by-side CopilotPanel chat backed by a new `SAMPLE_OUTPUT` copilot agent, and delete the old generate feature (client + server).

**Architecture:** Clone the recently-added `JSON_SCHEMA_BUILDER` copilot source 1:1 into a new `SAMPLE_OUTPUT` source. A copilot "source" is a thin routing key: the client `Source` enum value lowercases into `/api/platform/internal/ai/chat/sample_output`; the EE `CopilotApiController` maps that + `Mode` to Spring beans `sample_output_ask` / `sample_output_build`. Apply-back to the Monaco editor is decoupled through the three shared copilot registries (state contributor, tool-result handler, post-turn). The tool persists nothing; the existing **Upload** button keeps handling persistence.

**Tech Stack:** Java 25 / Spring Boot 4 (CE module `server/libs/ai/ai-copilot/`, EE `ai-copilot-rest`), Spring AI + AG-UI agents; React 19 / TypeScript / Zustand / assistant-ui / Monaco (client). Vitest + JUnit 5.

## Global Constraints

- CE files under `server/libs/ai/ai-copilot/` use the **Apache 2.0** license header (mirror `JsonSchemaBuilderSpringAIAgent` / `JsonSchemaTools`). Only EE files under `server/ee/` use the ByteChef Enterprise header + `@version ee`.
- Append new enum members to the **end** of `Source` and `CopilotAgentType` (ordinal-stability convention).
- Java: one blank line before control statements and after a variable modification that precedes its use; no trailing blank line before a class's closing brace; no `_`-prefixed private methods; descriptive variable names.
- Client: interface names end in `I`/`Props`; object keys sorted ascending; named imports sorted alphabetically; icons imported with `Icon` suffix; use `twMerge` (not `cn()`); `useRef` vars end in `Ref`.
- Do **not** remove the shared feature flags `ff-1570` or `ai.copilot.enabled` — only remove their uses inside the deleted bar.
- Do **not** hand-edit generated GraphQL files — remove the `.graphql`/`.graphqls` sources and re-run codegen.
- Before committing: server `./gradlew spotlessApply && ./gradlew check`; client `cd client && npm run check`.
- Commit message convention: server `732 <description>`, client `732 client - <description>`.

## File Structure

**Server — new (CE `server/libs/ai/ai-copilot/`):**
- `ai-copilot-tool/.../tool/SampleOutputTools.java` — the `updateSampleOutput` tool (validate + echo).
- `ai-copilot-service/.../agent/SampleOutputSpringAIAgent.java` — agent that injects `currentSampleOutput` into the system message.
- `ai-copilot-service/src/main/resources/prompt_sample_output_ask.txt`, `prompt_sample_output_build.txt`
- `ai-copilot-tool/src/test/.../tool/SampleOutputToolsTest.java`

**Server — modified:**
- `ai-copilot-api/.../util/Source.java` — add `SAMPLE_OUTPUT`
- `ai-copilot-tool/.../tool/CopilotAgentType.java` — add 4 members
- `ai-copilot-service/.../config/CopilotConfiguration.java` — 2 prompt fields + 2 beans
- `server/ee/libs/ai/ai-copilot/ai-copilot-rest/.../web/rest/CopilotApiController.java` — routing branch

**Server — removed (EE `sampleoutput` package):** see Task 8.

**Client — new:**
- `pages/platform/workflow-editor/components/node-details-tabs/output-tab/hooks/useSampleOutputCopilot.ts` (+ `.test.ts`)

**Client — modified:**
- `shared/components/copilot/stores/useCopilotStore.ts` — add `SAMPLE_OUTPUT`
- `pages/.../output-tab/OutputTabSampleDataDialog.tsx` — remove bar, embed CopilotPanel, resize
- `pages/.../output-tab/OutputTab.tsx` — dialog props
- `client/tailwind.config.js` — dialog width/height tokens
- `pages/.../output-tab/OutputTabSampleDataDialog.test.tsx` — adapt

**Client — removed:** see Task 9.

---

### Task 1: Server — `SAMPLE_OUTPUT` enum members + `SampleOutputTools`

**Files:**
- Modify: `server/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ai/copilot/util/Source.java`
- Modify: `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/CopilotAgentType.java`
- Create: `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/SampleOutputTools.java`
- Test: `server/libs/ai/ai-copilot/ai-copilot-tool/src/test/java/com/bytechef/ai/copilot/tool/SampleOutputToolsTest.java`

**Interfaces:**
- Produces: `SampleOutputTools#updateSampleOutput(String) : String` returning `{"sampleOutput": <parsed>}` on success or `{"error": "..."}` on invalid JSON. Enum `Source.SAMPLE_OUTPUT`; `CopilotAgentType.SAMPLE_OUTPUT_ASK/_BUILD/SAMPLE_OUTPUT/_AGENT` with keys `sample_output_ask`, `sample_output_build`, `sample_output`, `sample_output_agent`.

- [ ] **Step 1: Write the failing test**

Create `SampleOutputToolsTest.java`:

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

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.jackson.config.JacksonConfiguration;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class SampleOutputToolsTest {

    @BeforeAll
    static void beforeAll() {
        new JacksonConfiguration(new JsonMapper()).afterPropertiesSet();
    }

    @Test
    void testUpdateSampleOutputEchoesValidJson() {
        SampleOutputTools sampleOutputTools = new SampleOutputTools();

        String result = sampleOutputTools.updateSampleOutput("{\"a\":1}");

        Map<?, ?> map = JsonUtils.read(result, Map.class);

        assertThat(map).containsKey("sampleOutput");
        assertThat(map).doesNotContainKey("error");
    }

    @Test
    void testUpdateSampleOutputRejectsInvalidJson() {
        SampleOutputTools sampleOutputTools = new SampleOutputTools();

        String result = sampleOutputTools.updateSampleOutput("not json");

        Map<?, ?> map = JsonUtils.read(result, Map.class);

        assertThat(map).containsKey("error");
    }
}
```

> Note: mirror the existing `JacksonConfiguration` bootstrap other `ai-copilot-tool` tests use. If those tests instead use `@ExtendWith(ObjectMapperSetupExtension.class)`, match that idiom and drop `@BeforeAll` — check a sibling test in the same package first.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:test --tests '*SampleOutputToolsTest'`
Expected: FAIL — `SampleOutputTools` does not exist (compilation error).

- [ ] **Step 3: Add enum members**

In `Source.java`, change the enum body to append `SAMPLE_OUTPUT` at the end:

```java
    WORKFLOW_EDITOR, CODE_EDITOR, CONVERTER, CLUSTER_ELEMENT, SKILLS, JSON_SCHEMA_BUILDER, WORKFLOW_EXECUTION,
    WORKFLOW_CODE_EDITOR, SAMPLE_OUTPUT
```

In `CopilotAgentType.java`, append after `JSON_SCHEMA_BUILDER_AGENT(...)` (change that line's trailing `;` to `,`):

```java
    JSON_SCHEMA_BUILDER_AGENT("json_schema_builder_agent", false),
    SAMPLE_OUTPUT_ASK("sample_output_ask", false),
    SAMPLE_OUTPUT_BUILD("sample_output_build", false),
    SAMPLE_OUTPUT("sample_output", true),
    SAMPLE_OUTPUT_AGENT("sample_output_agent", false);
```

- [ ] **Step 4: Create `SampleOutputTools.java`**

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

import com.bytechef.commons.util.JsonUtils;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Exposes the "apply sample output" tool to the Sample Output copilot agent. The generated sample output value is
 * applied on the client (the value is not persisted server-side by this tool), so this tool only validates and echoes
 * it back.
 *
 * @author Ivica Cardic
 */
public class SampleOutputTools {

    private static final Logger log = LoggerFactory.getLogger(SampleOutputTools.class);

    @Tool(
        description = "Apply the complete, updated sample output value to the editor. Pass the entire value as a "
            + "JSON string; the previous sample output is fully replaced.")
    public String updateSampleOutput(
        @ToolParam(description = "The complete updated sample output value as a JSON string") String sampleOutput) {

        try {
            Object parsed = JsonUtils.read(sampleOutput);

            return JsonUtils.write(Map.of("sampleOutput", parsed));
        } catch (RuntimeException exception) {
            log.warn("updateSampleOutput rejected invalid JSON: {}", exception.getMessage());

            return JsonUtils.write(Map.of("error", "Invalid JSON: " + exception.getMessage()));
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:test --tests '*SampleOutputToolsTest'`
Expected: PASS (2 tests).

- [ ] **Step 6: Format + commit**

```bash
./gradlew spotlessApply
git add server/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ai/copilot/util/Source.java \
        server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/CopilotAgentType.java \
        server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/SampleOutputTools.java \
        server/libs/ai/ai-copilot/ai-copilot-tool/src/test/java/com/bytechef/ai/copilot/tool/SampleOutputToolsTest.java
git commit -m "732 Add SAMPLE_OUTPUT copilot enum members and SampleOutputTools"
```

---

### Task 2: Server — `SampleOutputSpringAIAgent` + prompts + config beans

**Files:**
- Create: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/agent/SampleOutputSpringAIAgent.java`
- Create: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_sample_output_ask.txt`
- Create: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_sample_output_build.txt`
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/CopilotConfiguration.java`

**Interfaces:**
- Consumes: `SampleOutputTools` (Task 1), `CopilotSpringAIAgent` base, `Source.SAMPLE_OUTPUT`, `Mode.ASK/BUILD`.
- Produces: Spring beans `sampleOutputAskSpringAIAgent` / `sampleOutputBuildSpringAIAgent` with `agentId` `sample_output_ask` / `sample_output_build`, discoverable by the EE `CopilotApiController` `LocalAgent` map.

- [ ] **Step 1: Create `SampleOutputSpringAIAgent.java`**

Clone of `JsonSchemaBuilderSpringAIAgent` (same builder boilerplate). Only the class name and `createSystemMessage` differ:

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

package com.bytechef.ai.copilot.agent;

import com.agui.core.agent.RunAgentInput;
import com.agui.core.context.Context;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.SystemMessage;
import com.agui.core.state.State;
import com.agui.server.LocalAgent;
import com.agui.spring.ai.SpringAIAgent;
import com.bytechef.ai.copilot.util.CopilotToolContextUtils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
public class SampleOutputSpringAIAgent extends CopilotSpringAIAgent {

    protected SampleOutputSpringAIAgent(final Builder builder) throws AGUIException {
        super(builder, builder.overrideChatClientResolver);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected Map<String, Object> toolContext(RunAgentInput input) {
        return CopilotToolContextUtils.toToolContext(input.state());
    }

    @Override
    protected SystemMessage createSystemMessage(State state, List<Context> contexts) {
        Object currentSampleOutput = state.get("currentSampleOutput");

        List<String> contextStrings = contexts.stream()
            .map(Context::toString)
            .toList();

        String resolvedMessage = Objects.nonNull(this.systemMessageProvider)
            ? this.systemMessageProvider.apply(this) : this.systemMessage;

        String message = "%s%n%nCurrent sample output:%n%s%n%nState:%n%s%n%nContext:%n%s%n".formatted(
            resolvedMessage, currentSampleOutput, state, String.join("\n", contextStrings));

        SystemMessage systemMessage = new SystemMessage();

        systemMessage.setId(String.valueOf(UUID.randomUUID()));
        systemMessage.setContent(message);

        return systemMessage;
    }

    public static class Builder extends SpringAIAgent.Builder {

        private @Nullable OverrideChatClientResolver overrideChatClientResolver;

        public Builder overrideChatClientResolver(@Nullable OverrideChatClientResolver overrideChatClientResolver) {
            this.overrideChatClientResolver = overrideChatClientResolver;

            return this;
        }

        public Builder chatModel(ChatModel chatModel) {
            super.chatModel(chatModel);

            return this;
        }

        public Builder advisors(List<Advisor> advisors) {
            super.advisors(advisors);

            return this;
        }

        public Builder advisor(Advisor advisor) {
            super.advisor(advisor);

            return this;
        }

        public Builder tools(List<Object> tools) {
            super.tools(tools);

            return this;
        }

        public Builder tool(Object tool) {
            super.tool(tool);

            return this;
        }

        public Builder agentId(String agentId) {
            super.agentId(agentId);

            return this;
        }

        public Builder state(State state) {
            super.state(state);

            return this;
        }

        public Builder toolCallbacks(List<ToolCallback> toolCallbacks) {
            super.toolCallbacks(toolCallbacks);

            return this;
        }

        public Builder toolCallback(ToolCallback toolCallback) {
            super.toolCallback(toolCallback);

            return this;
        }

        public Builder systemMessage(String systemMessage) {
            super.systemMessage(systemMessage);

            return this;
        }

        public Builder systemMessageProvider(Function<LocalAgent, String> systemMessageProvider) {
            super.systemMessageProvider(systemMessageProvider);

            return this;
        }

        public Builder chatMemory(ChatMemory chatMemory) {
            super.chatMemory(chatMemory);

            return this;
        }

        public Builder messages(List<BaseMessage> messages) {
            super.messages(messages);

            return this;
        }

        public SampleOutputSpringAIAgent build() throws AGUIException {

            return new SampleOutputSpringAIAgent(this);
        }
    }
}
```

- [ ] **Step 2: Create `prompt_sample_output_ask.txt`**

```text
You are the Sample Output assistant inside ByteChef's low-code workflow editor.
You help the user understand, review, and refine a sample output value — a concrete example
of the JSON data a workflow node produces (NOT a JSON Schema; a realistic example value).

The user's current sample output is provided in the request state as the top-level key
"currentSampleOutput"; the workflow node it belongs to is provided in the context parameters
as "workflowNodeName" (and "workflowId").

In ASK mode you explain, critique, and answer questions about the sample output. You do NOT
modify it — describe the change you would make and let the user ask you to apply it. Be
concise and concrete; refer to specific fields by their JSON path.
```

- [ ] **Step 3: Create `prompt_sample_output_build.txt`**

```text
You are the Sample Output assistant inside ByteChef's low-code workflow editor.
You help the user construct and refine a sample output value — a concrete example of the JSON
data a workflow node produces (NOT a JSON Schema; a realistic example value with plausible
field names and values).

The user's current sample output is provided in the request state as the top-level key
"currentSampleOutput"; the workflow node it belongs to is provided in the context parameters
as "workflowNodeName" (and "workflowId").

When the user asks for a change, produce the COMPLETE updated sample output value (not a diff)
and apply it by calling the updateSampleOutput tool with the entire value as a JSON string.
The value must be valid JSON. After applying, briefly summarise what changed. Always start
from the current sample output in state rather than from scratch, unless the user asks to
replace it entirely.
```

- [ ] **Step 4: Add prompt fields + beans to `CopilotConfiguration.java`**

Add two fields next to the JSON schema builder fields (after line with `promptJsonSchemaBuilderBuildResource;`):

```java
    private final Resource promptSampleOutputAskResource;
    private final Resource promptSampleOutputBuildResource;
```

Add two constructor `@Value` params (after the `promptJsonSchemaBuilderBuildResource` param):

```java
        @Value("classpath:prompt_sample_output_ask.txt") Resource promptSampleOutputAskResource,
        @Value("classpath:prompt_sample_output_build.txt") Resource promptSampleOutputBuildResource,
```

Add two assignments (after `this.promptJsonSchemaBuilderBuildResource = ...;`):

```java
        this.promptSampleOutputAskResource = promptSampleOutputAskResource;
        this.promptSampleOutputBuildResource = promptSampleOutputBuildResource;
```

Add the import near the other agent imports:

```java
import com.bytechef.ai.copilot.agent.SampleOutputSpringAIAgent;
import com.bytechef.ai.copilot.tool.SampleOutputTools;
```

Add the two beans immediately after `jsonSchemaBuilderBuildSpringAIAgent`:

```java
    @Bean
    SampleOutputSpringAIAgent sampleOutputAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        String name = Source.SAMPLE_OUTPUT.name() + "_" + Mode.ASK.name();

        return SampleOutputSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptSampleOutputAskResource))
            .toolCallbacks(wrapTools(securityContextRehydrator, List.of()))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    SampleOutputSpringAIAgent sampleOutputBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        String name = Source.SAMPLE_OUTPUT.name() + "_" + Mode.BUILD.name();

        List<Object> tools = new ArrayList<>(List.of(new SampleOutputTools()));

        return SampleOutputSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptSampleOutputBuildResource))
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }
```

- [ ] **Step 5: Compile the module**

Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Format + commit**

```bash
./gradlew spotlessApply
git add server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/agent/SampleOutputSpringAIAgent.java \
        server/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_sample_output_ask.txt \
        server/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_sample_output_build.txt \
        server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/CopilotConfiguration.java
git commit -m "732 Add SampleOutput copilot agent, prompts and config beans"
```

---

### Task 3: Server — EE `CopilotApiController` routing branch

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-rest/src/main/java/com/bytechef/ee/ai/copilot/web/rest/CopilotApiController.java`

**Interfaces:**
- Consumes: beans `sample_output_ask` / `sample_output_build` (Task 2), request `mode` state.
- Produces: routes `POST /ai/chat/sample_output` to the correct bean by `Mode`.

- [ ] **Step 1: Add the routing branch**

After the `else if (agentId.equals("json_schema_builder")) { ... }` block (ends near line 129), add:

```java
        } else if (agentId.equals("sample_output")) {
            if (Mode.valueOf((String) mode) == Mode.BUILD) {
                agentId = "sample_output_build";
            } else {
                agentId = "sample_output_ask";
            }
```

(This slots into the existing `if/else if` chain before the final closing brace that precedes `LocalAgent localAgent = localAgentMap.get(agentId);`.)

- [ ] **Step 2: Compile**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-rest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Format + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/ai/ai-copilot/ai-copilot-rest/src/main/java/com/bytechef/ee/ai/copilot/web/rest/CopilotApiController.java
git commit -m "732 Route sample_output copilot source to ASK/BUILD agents"
```

---

### Task 4: Client — `SAMPLE_OUTPUT` source + `useSampleOutputCopilot` hook

**Files:**
- Modify: `client/src/shared/components/copilot/stores/useCopilotStore.ts`
- Create: `client/src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/hooks/useSampleOutputCopilot.ts`
- Test: `client/src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/hooks/useSampleOutputCopilot.test.ts`

**Interfaces:**
- Consumes: `Source.SAMPLE_OUTPUT`, the three copilot registries, `useCopilotStore`.
- Produces: `useSampleOutputCopilot({onApply, sampleOutputRef, workflowId, workflowNodeName}) => {copilotPanelOpen, handleCopilotClose, handleCopilotOpen}` where `onApply: (value: string) => void` receives a formatted JSON string; `sampleOutputRef: {current: object | undefined}`. Tool handled: `updateSampleOutput` (payload `{sampleOutput}`).

- [ ] **Step 1: Add the source enum value**

In `useCopilotStore.ts`, append to the `Source` enum (after `JSON_SCHEMA_BUILDER`):

```ts
    JSON_SCHEMA_BUILDER = 'JSON_SCHEMA_BUILDER',
    SAMPLE_OUTPUT = 'SAMPLE_OUTPUT',
```

- [ ] **Step 2: Write the failing hook test**

Create `useSampleOutputCopilot.test.ts` (mirrors the JSON schema builder hook test):

```ts
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import useCopilotStateContributorRegistry from '@/shared/components/copilot/stores/useCopilotStateContributorRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import useCopilotToolResultHandlerRegistry from '@/shared/components/copilot/stores/useCopilotToolResultHandlerRegistry';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useSampleOutputCopilot} from './useSampleOutputCopilot';

describe('useSampleOutputCopilot', () => {
    beforeEach(() => {
        useCopilotPostTurnRegistry.setState({callbacks: {}});
        useCopilotStateContributorRegistry.setState({contributors: []});
        useCopilotStore.setState({context: undefined, messages: []});
        useCopilotToolResultHandlerRegistry.setState({handlers: {}});
    });

    it('sets sample-output context on open and restores on close', () => {
        const restoreSpy = vi.spyOn(useCopilotStore.getState(), 'restoreConversationState');
        const saveSpy = vi.spyOn(useCopilotStore.getState(), 'saveConversationState');

        const {result} = renderHook(() =>
            useSampleOutputCopilot({
                onApply: vi.fn(),
                sampleOutputRef: {current: {a: 1}},
                workflowId: 'w1',
                workflowNodeName: 'node1',
            })
        );

        act(() => result.current.handleCopilotOpen());

        expect(saveSpy).toHaveBeenCalled();
        expect(useCopilotStore.getState().context).toMatchObject({
            mode: MODE.ASK,
            parameters: {workflowId: 'w1', workflowNodeName: 'node1'},
            source: Source.SAMPLE_OUTPUT,
        });
        expect(result.current.copilotPanelOpen).toBe(true);

        act(() => result.current.handleCopilotClose());

        expect(restoreSpy).toHaveBeenCalled();
        expect(result.current.copilotPanelOpen).toBe(false);
    });

    it('applies the value from an updateSampleOutput tool result', () => {
        const onApply = vi.fn();

        renderHook(() =>
            useSampleOutputCopilot({
                onApply,
                sampleOutputRef: {current: undefined},
                workflowId: 'w1',
                workflowNodeName: 'node1',
            })
        );

        act(() =>
            useCopilotToolResultHandlerRegistry
                .getState()
                .runFor('updateSampleOutput', JSON.stringify({sampleOutput: {a: 1}}))
        );

        expect(onApply).toHaveBeenCalledWith(JSON.stringify({a: 1}, null, 4));
    });
});
```

> `SPACE` in `@/shared/constants` is `4`; the assertion uses `4` to match the hook's `JSON.stringify(..., null, SPACE)`.

- [ ] **Step 3: Run the test to verify it fails**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/hooks/useSampleOutputCopilot.test.ts`
Expected: FAIL — cannot resolve `./useSampleOutputCopilot`.

- [ ] **Step 4: Create the hook**

```ts
import {parseJson} from '@/shared/components/ai-chat/messages/toToolResultDataPart';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import useCopilotStateContributorRegistry from '@/shared/components/copilot/stores/useCopilotStateContributorRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import useCopilotToolResultHandlerRegistry from '@/shared/components/copilot/stores/useCopilotToolResultHandlerRegistry';
import {SPACE} from '@/shared/constants';
import {useCallback, useEffect, useRef, useState} from 'react';

const APPLIED_MESSAGE = '✓ Applied the sample output.';

interface UseSampleOutputCopilotParamsI {
    onApply: (value: string) => void;
    sampleOutputRef: {current: object | undefined};
    workflowId?: string;
    workflowNodeName?: string;
}

interface UseSampleOutputCopilotResultI {
    copilotPanelOpen: boolean;
    handleCopilotClose: () => void;
    handleCopilotOpen: () => void;
}

export function useSampleOutputCopilot({
    onApply,
    sampleOutputRef,
    workflowId,
    workflowNodeName,
}: UseSampleOutputCopilotParamsI): UseSampleOutputCopilotResultI {
    const [copilotPanelOpen, setCopilotPanelOpen] = useState(false);

    const pendingAppliedRef = useRef<boolean>(false);

    const handleCopilotOpen = useCallback(() => {
        const {context, generateConversationId, resetMessages, saveConversationState, setContext} =
            useCopilotStore.getState();

        saveConversationState();
        resetMessages();
        generateConversationId();

        setContext({
            ...context,
            mode: MODE.ASK,
            parameters: {workflowId, workflowNodeName},
            source: Source.SAMPLE_OUTPUT,
        });

        setCopilotPanelOpen(true);
    }, [workflowId, workflowNodeName]);

    const handleCopilotClose = useCallback(() => {
        useCopilotStore.getState().restoreConversationState();

        setCopilotPanelOpen(false);
    }, []);

    useEffect(() => {
        const unregisterContributor = useCopilotStateContributorRegistry.getState().register(() => ({
            currentSampleOutput: sampleOutputRef.current,
            workflowId,
            workflowNodeName,
        }));

        const unregisterToolResult = useCopilotToolResultHandlerRegistry
            .getState()
            .register('updateSampleOutput', (content) => {
                const result = parseJson<{sampleOutput?: unknown}>(content, 'updateSampleOutput result');

                if (result?.sampleOutput !== undefined) {
                    pendingAppliedRef.current = true;

                    onApply(JSON.stringify(result.sampleOutput, null, SPACE));
                }
            });

        const unregisterPostTurn = useCopilotPostTurnRegistry.getState().register(Source.SAMPLE_OUTPUT, () => {
            if (!pendingAppliedRef.current) {
                return;
            }

            pendingAppliedRef.current = false;

            useCopilotStore.getState().appendToLastAssistantMessage(APPLIED_MESSAGE);
        });

        return () => {
            unregisterContributor();
            unregisterToolResult();
            unregisterPostTurn();
        };
    }, [onApply, sampleOutputRef, workflowId, workflowNodeName]);

    return {copilotPanelOpen, handleCopilotClose, handleCopilotOpen};
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/hooks/useSampleOutputCopilot.test.ts`
Expected: PASS (2 tests).

- [ ] **Step 6: Commit**

```bash
cd client && npm run format
git add src/shared/components/copilot/stores/useCopilotStore.ts \
        src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/hooks/useSampleOutputCopilot.ts \
        src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/hooks/useSampleOutputCopilot.test.ts
git commit -m "732 client - Add SAMPLE_OUTPUT source and useSampleOutputCopilot hook"
```

---

### Task 5: Client — resize dialog tokens + embed CopilotPanel in the dialog

**Files:**
- Modify: `client/tailwind.config.js:304,317`
- Modify: `client/src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTabSampleDataDialog.tsx`
- Modify: `client/src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTab.tsx`

**Interfaces:**
- Consumes: `useSampleOutputCopilot` (Task 4), `CopilotPanel`, `Source.SAMPLE_OUTPUT`, `useApplicationInfoStore`, `useFeatureFlagsStore`.
- Produces: `OutputTabSampleDataDialog` prop shape `{onClose, onUpload, open, placeholder?, workflowId?, workflowNodeName?}` (drops `environmentId`).

- [ ] **Step 1: Widen + heighten the dialog tokens**

In `client/tailwind.config.js`, update the two tokens:

```js
                'output-tab-sample-data-dialog-width': '1100px',
```
```js
                'output-tab-sample-data-dialog-height': '560px',
```

- [ ] **Step 2: Rewrite `OutputTabSampleDataDialog.tsx`**

Replace the file with the two-column layout (copilot column + editor column). Full file:

```tsx
import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import CopilotPanel from '@/shared/components/copilot/CopilotPanel';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {EDITOR_PLACEHOLDER, SPACE} from '@/shared/constants';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {Suspense, lazy, useEffect, useRef, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import type {StandaloneCodeEditorType} from '@/shared/components/MonacoTypes';

import {useSampleOutputCopilot} from './hooks/useSampleOutputCopilot';

const MonacoEditor = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

interface OutputTabSampleDataDialogProps {
    onClose: () => void;
    onUpload: (value: string) => void;
    open: boolean;
    placeholder?: object;
    workflowId?: string;
    workflowNodeName?: string;
}

const OutputTabSampleDataDialog = ({
    onClose,
    onUpload,
    open,
    placeholder,
    workflowId,
    workflowNodeName,
}: OutputTabSampleDataDialogProps) => {
    const [parsedValue, setParsedValue] = useState<object | undefined>();
    const [rawValue, setRawValue] = useState<string>('');

    const sampleOutputRef = useRef<object | undefined>(undefined);

    const ai = useApplicationInfoStore((state) => state.ai);
    const ff1570 = useFeatureFlagsStore()('ff-1570');

    const copilotAvailable = ai.copilot.enabled && ff1570;

    const applyValue = (value: string) => {
        setRawValue(value);

        try {
            const parsed = JSON.parse(value);

            setParsedValue(parsed);
            sampleOutputRef.current = parsed;
        } catch {
            setParsedValue(undefined);
            sampleOutputRef.current = undefined;
        }
    };

    const {copilotPanelOpen, handleCopilotClose, handleCopilotOpen} = useSampleOutputCopilot({
        onApply: applyValue,
        sampleOutputRef,
        workflowId,
        workflowNodeName,
    });

    const handleEditorOnChange = (editorValue: string | undefined) => {
        const placeholderElement = document.querySelector('#monaco-placeholder') as HTMLElement | null;

        if (placeholderElement) {
            placeholderElement.style.display = editorValue ? 'none' : 'block';
        }

        setRawValue(editorValue ?? '');

        if (editorValue) {
            try {
                const parsed = JSON.parse(editorValue);

                setParsedValue(parsed);
                sampleOutputRef.current = parsed;
            } catch {
                setParsedValue(undefined);
                sampleOutputRef.current = undefined;
            }
        } else {
            setParsedValue(undefined);
            sampleOutputRef.current = undefined;
        }
    };

    const handleEditorOnMount = (editor: StandaloneCodeEditorType) => {
        const placeholderElement = document.querySelector('#monaco-placeholder') as HTMLElement | null;

        if (placeholderElement) {
            placeholderElement.style.display = rawValue ? 'none' : 'block';
        }

        editor.focus();
    };

    const handleOpenChange = (isOpen: boolean) => {
        if (!isOpen) {
            const hasPlaceholder = placeholder !== undefined && Object.keys(placeholder).length > 0;

            setRawValue(hasPlaceholder ? JSON.stringify(placeholder, null, SPACE) : '');
            setParsedValue(hasPlaceholder ? placeholder : undefined);
            sampleOutputRef.current = hasPlaceholder ? placeholder : undefined;

            if (copilotAvailable) {
                handleCopilotClose();
            }

            onClose();
        }
    };

    useEffect(() => {
        if (placeholder !== undefined && Object.keys(placeholder).length) {
            const stringified = JSON.stringify(placeholder, null, SPACE);

            setRawValue(stringified);
            setParsedValue(placeholder);
            sampleOutputRef.current = placeholder;
        } else {
            setRawValue('');
            setParsedValue(undefined);
            sampleOutputRef.current = undefined;
        }
    }, [placeholder]);

    useEffect(() => {
        if (open && copilotAvailable) {
            handleCopilotOpen();
        }
    }, [copilotAvailable, handleCopilotOpen, open]);

    return (
        <Dialog modal={false} onOpenChange={handleOpenChange} open={open}>
            <DialogContent
                className={twMerge(
                    'flex flex-col',
                    copilotAvailable
                        ? 'max-w-output-tab-sample-data-dialog-width sm:max-w-output-tab-sample-data-dialog-width'
                        : 'max-w-[800px] sm:max-w-[800px]'
                )}
                onInteractOutside={(event) => event.preventDefault()}
            >
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>Upload Sample Output Data</DialogTitle>

                        <DialogDescription>
                            Chat with the assistant or edit the JSON directly. Click Upload when you&apos;re done.
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <div className="flex min-h-output-tab-sample-data-dialog-height flex-1 gap-4">
                    {copilotAvailable && (
                        <div className="flex w-96 shrink-0 flex-col overflow-hidden rounded-md border border-border/50">
                            <CopilotPanel
                                className="size-full"
                                onClose={handleCopilotClose}
                                open={copilotPanelOpen}
                                source={Source.SAMPLE_OUTPUT}
                            />
                        </div>
                    )}

                    <div className="flex min-w-0 flex-1 flex-col">
                        <div className="relative flex-1">
                            <div className="absolute inset-0">
                                <Suspense fallback={<MonacoEditorLoader />}>
                                    <MonacoEditor
                                        className="bg-transparent"
                                        defaultLanguage="json"
                                        onChange={handleEditorOnChange}
                                        onMount={handleEditorOnMount}
                                        value={rawValue}
                                    />
                                </Suspense>

                                <div
                                    className="pointer-events-none absolute top-[-2px] left-[70px] h-full text-sm text-muted-foreground"
                                    id="monaco-placeholder"
                                >
                                    {EDITOR_PLACEHOLDER}
                                </div>
                            </div>
                        </div>

                        <div className="mt-4 flex justify-end">
                            <Button
                                disabled={!parsedValue}
                                label="Upload"
                                onClick={() => {
                                    if (parsedValue) {
                                        onUpload(JSON.stringify(parsedValue));
                                    }
                                }}
                                type="submit"
                            />
                        </div>
                    </div>
                </div>
            </DialogContent>
        </Dialog>
    );
};

export default OutputTabSampleDataDialog;
```

> Import note: `CopilotPanel` is a **default** export (verified) — `import CopilotPanel from '@/shared/components/copilot/CopilotPanel';`, matching `PropertyJsonSchemaBuilderSheet.tsx`.

- [ ] **Step 3: Update `OutputTab.tsx` dialog usage**

Replace the `<OutputTabSampleDataDialog ... />` block (~lines 219–227): drop `environmentId`, add `workflowNodeName`:

```tsx
                    <OutputTabSampleDataDialog
                        onClose={() => setShowUploadDialog(false)}
                        onUpload={handleSampleDataDialogUpload}
                        open={showUploadDialog}
                        placeholder={placeholder || sampleOutput}
                        workflowId={workflowId}
                        workflowNodeName={currentNode.name}
                    />
```

- [ ] **Step 4: Typecheck + lint**

Run: `cd client && npm run check`
Expected: PASS (lint + typecheck + tests). Fix any import-order / sort-keys issues surfaced.

- [ ] **Step 5: Commit**

```bash
cd client && npm run format
git add tailwind.config.js \
        src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTabSampleDataDialog.tsx \
        src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTab.tsx
git commit -m "732 client - Embed sample-output CopilotPanel in Upload dialog"
```

---

### Task 6: Client — remove the "Generate with AI" bar + adapt dialog test

**Files:**
- Delete: `client/src/shared/components/copilot/SampleOutputCopilotBar.tsx`
- Delete: `client/src/shared/components/copilot/SampleOutputCopilotBar.test.tsx`
- Delete: `client/src/shared/components/copilot/useGenerateSampleOutput.ts`
- Delete: `client/src/graphql/platform/copilot/generateSampleOutput.graphql`
- Modify (regenerated): `client/src/shared/middleware/graphql.ts`, `client/src/shared/middleware/graphql-types.ts`
- Modify: `client/src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTabSampleDataDialog.test.tsx`

**Interfaces:**
- Consumes: nothing new. Removes `useGenerateSampleOutput`, `SampleOutputCopilotBar`, `generateSampleOutput` mutation.

- [ ] **Step 1: Delete the bar, hook, and GraphQL operation**

```bash
cd client
git rm src/shared/components/copilot/SampleOutputCopilotBar.tsx \
       src/shared/components/copilot/SampleOutputCopilotBar.test.tsx \
       src/shared/components/copilot/useGenerateSampleOutput.ts \
       src/graphql/platform/copilot/generateSampleOutput.graphql
```

- [ ] **Step 2: Verify no dangling references**

Run: `cd client && grep -rn "SampleOutputCopilotBar\|useGenerateSampleOutput\|generateSampleOutput" src | grep -v middleware/graphql`
Expected: no output. (If any surface other than the middleware appears, remove the reference.)

- [ ] **Step 3: Regenerate GraphQL client code**

Run: `cd client && npx graphql-codegen`
Expected: `graphql.ts` / `graphql-types.ts` regenerate without `GenerateSampleOutput*` symbols.
Verify: `grep -rn "GenerateSampleOutput" src/shared/middleware` → no output.

- [ ] **Step 4: Adapt the dialog test**

Open `OutputTabSampleDataDialog.test.tsx`. Remove any assertions/mocks referencing the "Generate with AI" bar (e.g. `SampleOutputCopilotBar`, "Describe the sample output", `useGenerateSampleOutput`). Ensure the test either mocks `useSampleOutputCopilot` or mocks `CopilotPanel` (default export) so the dialog renders without a live copilot runtime. Minimal mock to add near the top:

```tsx
vi.mock('@/shared/components/copilot/CopilotPanel', () => ({
    default: () => <div data-testid="copilot-panel" />,
}));
```

Keep/confirm existing assertions for the title, JSON editor, and the Upload button behavior.

- [ ] **Step 5: Run the dialog test**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTabSampleDataDialog.test.tsx`
Expected: PASS.

- [ ] **Step 6: Full client check + commit**

```bash
cd client && npm run check
git add -A src/shared/middleware/graphql.ts src/shared/middleware/graphql-types.ts \
          src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTabSampleDataDialog.test.tsx
git add -u
git commit -m "732 client - Remove Generate with AI sample-output bar"
```

---

### Task 7: Server — remove the EE `sampleoutput` GraphQL feature

**Files (delete):**
- `server/ee/libs/ai/ai-copilot/ai-copilot-graphql/src/main/java/com/bytechef/ee/ai/copilot/web/graphql/SampleOutputCopilotGraphQlController.java`
- `server/ee/libs/ai/ai-copilot/ai-copilot-graphql/src/main/resources/graphql/sample-output-copilot.graphqls`
- `server/ee/libs/ai/ai-copilot/ai-copilot-graphql/src/test/java/com/bytechef/ee/ai/copilot/web/graphql/SampleOutputCopilotGraphQlControllerTest.java`
- `server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/sampleoutput/SampleOutputCopilotGenerator.java`
- `.../ai-copilot-api/.../sampleoutput/SampleOutputCopilotRequest.java`
- `.../ai-copilot-api/.../sampleoutput/SampleOutputCopilotResult.java`
- `.../ai-copilot-service/.../sampleoutput/SampleOutputCopilotGeneratorImpl.java`
- `.../ai-copilot-service/.../sampleoutput/SampleOutputPromptBuilder.java`
- `.../ai-copilot-service/src/test/.../sampleoutput/SampleOutputCopilotGeneratorImplTest.java`
- `.../ai-copilot-service/src/test/.../sampleoutput/SampleOutputPromptBuilderTest.java`

**Interfaces:** removes the `generateSampleOutput` GraphQL mutation and its whole implementation package. No remaining consumer (the client operation is gone as of Task 6).

- [ ] **Step 1: Confirm exact paths**

Run: `cd /Volumes/Data/bytechef/bytechef && find server/ee/libs/ai/ai-copilot -path '*sampleoutput*' -o -name 'SampleOutputCopilotGraphQlController*' -o -name 'sample-output-copilot.graphqls'`
Expected: the files listed above (verify names/paths before deleting).

- [ ] **Step 2: Delete the package**

```bash
cd /Volumes/Data/bytechef/bytechef
git rm server/ee/libs/ai/ai-copilot/ai-copilot-graphql/src/main/java/com/bytechef/ee/ai/copilot/web/graphql/SampleOutputCopilotGraphQlController.java \
       server/ee/libs/ai/ai-copilot/ai-copilot-graphql/src/main/resources/graphql/sample-output-copilot.graphqls \
       server/ee/libs/ai/ai-copilot/ai-copilot-graphql/src/test/java/com/bytechef/ee/ai/copilot/web/graphql/SampleOutputCopilotGraphQlControllerTest.java
git rm -r server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/sampleoutput
git rm -r server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/sampleoutput
git rm -r server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/sampleoutput
```

> If any path 404s, re-derive it from Step 1's `find` output and adjust.

- [ ] **Step 3: Check for dangling references**

Run: `cd /Volumes/Data/bytechef/bytechef && grep -rn "SampleOutputCopilotGenerator\|SampleOutputCopilotRequest\|SampleOutputCopilotResult\|SampleOutputPromptBuilder\|generateSampleOutput" server --include='*.java' --include='*.graphqls' | grep -v build/`
Expected: no output. Remove any stray reference found (e.g. a wiring/config bean or a build copy under `src`).

- [ ] **Step 4: Build the affected modules**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-graphql:compileJava :server:ee:libs:ai:ai-copilot:ai-copilot-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git commit -m "732 Remove Generate with AI sample-output GraphQL feature"
```

---

### Task 8: Full verification

**Files:** none (verification only).

- [ ] **Step 1: Server checks**

Run: `./gradlew spotlessApply && ./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:check :server:libs:ai:ai-copilot:ai-copilot-tool:check :server:ee:libs:ai:ai-copilot:ai-copilot-rest:check :server:ee:libs:ai:ai-copilot:ai-copilot-graphql:check`
Expected: BUILD SUCCESSFUL (all module checks pass).

- [ ] **Step 2: Client checks**

Run: `cd client && npm run check`
Expected: lint + typecheck + tests all pass.

- [ ] **Step 3: Manual smoke test (documented, run locally)**

With the dev stack running, open a node's **Output** tab → **Upload Sample Output Data**. Confirm:
1. The dialog is taller/wider with a chat column on the left and the JSON editor on the right.
2. Asking the assistant (BUILD mode) to "generate a sample output" fills the editor and the assistant message shows `✓ Applied the sample output.`.
3. Editing JSON by hand still works; **Upload** persists and closes the dialog.
4. With copilot disabled (`ai.copilot.enabled=false` or `ff-1570` off), the dialog falls back to editor-only at the original width.

- [ ] **Step 4: Confirm the old feature is gone**

Run: `cd /Volumes/Data/bytechef/bytechef && grep -rn "Generate with AI\|generateSampleOutput\|SampleOutputCopilotBar" client/src server --include='*.tsx' --include='*.ts' --include='*.java' --include='*.graphql' --include='*.graphqls' | grep -v build/ | grep -v node_modules`
Expected: no output.

---

## Self-Review notes

- **Spec coverage:** new source (Tasks 1–3), client source + hook (Task 4), dialog embed + resize (Task 5), client removal (Task 6), server removal (Task 7), testing/verification (Task 8). All spec sections mapped.
- **Type consistency:** tool name `updateSampleOutput` and state key `currentSampleOutput` are identical across server tool (Task 1), server agent (Task 2), client hook + test (Task 4). `onApply: (value: string) => void` consistent between hook (Task 4) and dialog (Task 5). Enum keys `sample_output_ask`/`sample_output_build` match between `CopilotAgentType` (Task 1), config bean `agentId` (Task 2), and controller routing (Task 3).
- **Verified:** `CopilotPanel` is a default export; `SPACE = 4`. One open point flagged inline: the `ai-copilot-tool` test's ObjectMapper bootstrap idiom — "match the sibling test", not left as TODO.
```
