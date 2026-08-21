# Copilot: Workflow Code Editor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the Workflow Code editor (`WorkflowCodeEditorSheet`, the full workflow-definition Monaco editor) its own workflow-definition-aware copilot — a new `WORKFLOW_CODE_EDITOR` source routed to dedicated `workflow_code_editor_ask`/`workflow_code_editor_build` agents — instead of the per-node Script agent it wrongly reuses today, with a client "Apply to editor" action for Build output (#4076).

**Architecture:** New `Source.WORKFLOW_CODE_EDITOR` (server + client) → `CopilotApiController` routing branch → two `LocalAgent` beans backed by a new `WorkflowCodeEditorSpringAIAgent` (injects the editor's real format, yaml/json) + two prompt resources + workflow tools (no `ScriptTools`). Client: `useWorkflowCodeEditorSheet` sends the new source + real format; `CopilotPanel` gains an optional `onApply` that, for this source, extracts the latest assistant code block and loads it into the editor's `definition` buffer (review + Save stay manual). The per-node Script copilot is untouched.

**Tech Stack:** Java 25 / Spring, ag-ui `LocalAgent`/`SpringAIAgent`, Spring AI `ChatModel`; React 19 + TypeScript, assistant-ui, Zustand, Monaco, Vitest. EE modules under `server/ee/libs/ai/ai-copilot/`.

**Spec:** `docs/superpowers/specs/2026-06-03-workflow-code-editor-copilot-design.md`

**Conventions:** EE license header + `@version ee` on every new `server/ee` file (incl. tests). Client: `sort-keys` alphabetical, interfaces `…PropsI`/`…Props`, sorted destructured imports, Lucide `…Icon`, `twMerge`, hook ordering, no `_`-prefixed methods. Java: one blank line before control statements.

---

## File Structure

**Backend:**
- Modify: `ai-copilot-api/.../util/Source.java` — add `WORKFLOW_CODE_EDITOR`.
- Create: `ai-copilot-service/.../agent/WorkflowCodeEditorSpringAIAgent.java`.
- Create: `ai-copilot-service/src/main/resources/prompt_workflow_code_editor_ask.txt`, `prompt_workflow_code_editor_build.txt`.
- Modify: `ai-copilot-service/.../config/CopilotConfiguration.java` — two new `@Bean`s + prompt resource fields.
- Modify: `ai-copilot-rest/.../web/rest/CopilotApiController.java` — routing branch.
- Test: `ai-copilot-service/.../agent/WorkflowCodeEditorSpringAIAgentTest.java`; extend `ai-copilot-rest/.../web/rest/CopilotApiControllerTest.java`.

**Frontend:**
- Modify: `client/src/shared/components/copilot/stores/useCopilotStore.ts` — add `Source.WORKFLOW_CODE_EDITOR`.
- Create: `client/src/shared/components/copilot/utils/extractDefinitionFromMessage.ts` (+ `.test.ts`).
- Modify: `client/src/shared/components/copilot/CopilotPanel.tsx` — optional `onApply` + Apply action.
- Modify: `client/src/pages/platform/workflow-editor/hooks/useWorkflowCodeEditorSheet.ts` — new source + `{format}`.
- Modify: `client/src/pages/platform/workflow-editor/components/WorkflowCodeEditorSheet.tsx` — pass `source` + `onApply`.

---

## Part A — Backend

### Task 1: Add `WORKFLOW_CODE_EDITOR` to server `Source`

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/util/Source.java`

- [ ] **Step 1: Add the enum constant (append last)**

```java
public enum Source {

    WORKFLOW_EDITOR, CODE_EDITOR, CONVERTER, CLUSTER_ELEMENT, SKILLS, WORKFLOW_EXECUTION, WORKFLOW_CODE_EDITOR
}
```

- [ ] **Step 2: Compile + commit**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-api:compileJava`
Expected: BUILD SUCCESSFUL.

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/util/Source.java
git commit -m "4076 Add WORKFLOW_CODE_EDITOR source"
```

---

### Task 2: `WorkflowCodeEditorSpringAIAgent` (format-aware system message)

**Files:**
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/agent/WorkflowCodeEditorSpringAIAgent.java`
- Test: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/agent/WorkflowCodeEditorSpringAIAgentTest.java`

The agent mirrors `CodeEditorSpringAIAgent` but injects the workflow-definition **format** instead of a script language. The format→instruction mapping is a package-private static method so it is unit-testable without building a full agent.

- [ ] **Step 1: Write the failing test (static helper)**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkflowCodeEditorSpringAIAgentTest {

    @Test
    void testFormatInstructionYaml() {
        assertThat(WorkflowCodeEditorSpringAIAgent.formatInstruction("yaml"))
            .contains("YAML");
    }

    @Test
    void testFormatInstructionJson() {
        assertThat(WorkflowCodeEditorSpringAIAgent.formatInstruction("json"))
            .contains("JSON");
    }

    @Test
    void testFormatInstructionDefaultsToJson() {
        assertThat(WorkflowCodeEditorSpringAIAgent.formatInstruction(null))
            .contains("JSON");
    }
}
```

- [ ] **Step 2: Run to verify fail**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests "*WorkflowCodeEditorSpringAIAgentTest"`
Expected: FAIL — class missing.

- [ ] **Step 3: Implement the agent**

Create `WorkflowCodeEditorSpringAIAgent.java`. It is a near-copy of `CodeEditorSpringAIAgent` (same Builder boilerplate and `resolveChatClient` override) with two changes: (1) `createSystemMessage` injects `formatInstruction(parameters.get("format"))` rather than a language switch; (2) a workflow-tailored `ADDITIONAL_RULES`. Paste this complete file:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.agent;

import com.agui.core.agent.RunAgentInput;
import com.agui.core.context.Context;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.SystemMessage;
import com.agui.core.state.State;
import com.agui.server.LocalAgent;
import com.agui.spring.ai.SpringAIAgent;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class WorkflowCodeEditorSpringAIAgent extends SpringAIAgent {

    private static final Logger log = LoggerFactory.getLogger(WorkflowCodeEditorSpringAIAgent.class);

    private static final String ADDITIONAL_RULES =
        """
            ## Additional Rules

            - You assist with the full workflow definition (its triggers, tasks, component `type` references, `parameters`, and `${...}` datapill references), NOT with a single script component's code.
            - The assistant must not produce visual representations of any kind, including diagrams, charts, UI sketches, images, or pseudo-visuals.
            - When you produce a workflow definition, validate it first and return it as a single fenced code block in the editor's format. If it's impossible to resolve an error, instruct the user to raise an issue on our GitHub https://github.com/bytechefhq/bytechef/issues.
            """;

    private final @Nullable OverrideChatClientResolver overrideChatClientResolver;

    protected WorkflowCodeEditorSpringAIAgent(final Builder builder) throws AGUIException {
        super(builder);

        this.overrideChatClientResolver = builder.overrideChatClientResolver;
    }

    public static Builder builder() {
        return new Builder();
    }

    static String formatInstruction(@Nullable String format) {
        if (Objects.equals(format, "yaml")) {
            return "The workflow definition format you have to assist with is YAML.";
        }

        return "The workflow definition format you have to assist with is JSON.";
    }

    @Override
    protected ChatClient resolveChatClient(RunAgentInput input) {
        if (overrideChatClientResolver == null) {
            return super.resolveChatClient(input);
        }

        try {
            ChatClient override = overrideChatClientResolver.resolve(input.state());

            if (override != null) {
                return override;
            }
        } catch (RuntimeException exception) {
            log.warn(
                "WorkflowCodeEditorSpringAIAgent: override ChatClient resolver threw; falling back to default. {}",
                exception.getMessage());
        }

        return super.resolveChatClient(input);
    }

    @Override
    protected SystemMessage createSystemMessage(State state, List<Context> contexts) {
        Map<?, ?> parameters = (Map<?, ?>) state.get("parameters");

        String formatInstruction = formatInstruction(parameters == null ? null : (String) parameters.get("format"));

        List<String> contextStrings = contexts.stream()
            .map(Context::toString)
            .toList();

        String resolvedMessage = Objects.nonNull(this.systemMessageProvider)
            ? this.systemMessageProvider.apply(this) : this.systemMessage;

        String message = "%s%n%s%n%s%n%nState:%n%s%n%nContext:%n%s%n".formatted(
            resolvedMessage, formatInstruction, ADDITIONAL_RULES, state, String.join("\n", contextStrings));

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

        public WorkflowCodeEditorSpringAIAgent build() throws AGUIException {

            return new WorkflowCodeEditorSpringAIAgent(this);
        }
    }
}
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests "*WorkflowCodeEditorSpringAIAgentTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/agent/WorkflowCodeEditorSpringAIAgent.java \
        server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/agent/WorkflowCodeEditorSpringAIAgentTest.java
git commit -m "4076 Add WorkflowCodeEditorSpringAIAgent"
```

---

### Task 3: Prompt resources

**Files:**
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_workflow_code_editor_ask.txt`
- Create: `.../resources/prompt_workflow_code_editor_build.txt`

- [ ] **Step 1: Create the ASK prompt**

```
Your role is to help the user understand, debug, and improve a ByteChef workflow definition that they are editing as raw text (YAML or JSON) in the Workflow Code editor.

A workflow definition contains a label, description, inputs, triggers, and tasks. Each task/trigger has a name, a component `type` reference (e.g. "componentName/v1/operation"), a label, and a `parameters` map. Data flows between steps via `${nodeName.path}` datapill references.

Help the user by:
- Explaining what the definition (or a part of it) does.
- Diagnosing validation errors — use the workflow validation tool to check the current definition and explain how to fix the reported problems.
- Suggesting how to reference component actions/triggers and their parameters correctly (consult the component definitions).

Do not rewrite the whole definition unless explicitly asked. Be concise. Do not produce diagrams or images.
```

- [ ] **Step 2: Create the BUILD prompt**

```
Your role is to help the user write a ByteChef workflow definition that they are editing as raw text (YAML or JSON) in the Workflow Code editor.

A workflow definition contains a label, description, inputs, triggers, and tasks. Each task/trigger has a name, a component `type` reference (e.g. "componentName/v1/operation"), a label, and a `parameters` map. Data flows between steps via `${nodeName.path}` datapill references.

When the user asks you to create or modify the workflow:
- Consult the available component definitions to use correct component `type` references and parameter names.
- Produce a COMPLETE, valid workflow definition in the editor's format (YAML or JSON, as indicated).
- Validate the definition with the workflow validation tool before answering; fix any errors you find.
- Return the definition as a SINGLE fenced code block (```yaml or ```json) preceded by a short explanation of what you changed. The user will apply it into the editor and save.

Do not produce diagrams or images.
```

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_workflow_code_editor_ask.txt \
        server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_workflow_code_editor_build.txt
git commit -m "4076 Add workflow code editor copilot prompts"
```

---

### Task 4: Register the two agent beans in `CopilotConfiguration`

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/config/CopilotConfiguration.java`

- [ ] **Step 1: Add imports + prompt resource fields**

Add the import near the other agent imports:

```java
import com.bytechef.ee.ai.copilot.agent.WorkflowCodeEditorSpringAIAgent;
```

Add two fields beside `promptCodeEditorAskResource`/`promptCodeEditorBuildResource`:

```java
    private final Resource promptWorkflowCodeEditorAskResource;
    private final Resource promptWorkflowCodeEditorBuildResource;
```

Add two constructor parameters (next to the code-editor prompt params) and assign them:

```java
        @Value("classpath:prompt_workflow_code_editor_ask.txt") Resource promptWorkflowCodeEditorAskResource,
        @Value("classpath:prompt_workflow_code_editor_build.txt") Resource promptWorkflowCodeEditorBuildResource,
```
```java
        this.promptWorkflowCodeEditorAskResource = promptWorkflowCodeEditorAskResource;
        this.promptWorkflowCodeEditorBuildResource = promptWorkflowCodeEditorBuildResource;
```

- [ ] **Step 2: Add the two `@Bean` methods**

Place these right after `codeEditorBuildSpringAIAgent`. They use the workflow tool set (no `ScriptTools`); `workflowValidatorTools` and `workflowInstructionTools` are existing instance fields of this class (as used by `codeEditorAskSpringAIAgent`).

```java
    @Bean
    WorkflowCodeEditorSpringAIAgent workflowCodeEditorAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools, Optional<FirecrawlTools> firecrawlTools,
        ObjectProvider<CopilotChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        String name = Source.WORKFLOW_CODE_EDITOR.name() + "_" + Mode.ASK.name();

        List<Object> tools = new ArrayList<>(
            List.of(
                readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

        firecrawlTools.ifPresent(tools::add);

        return WorkflowCodeEditorSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowCodeEditorAskResource))
            .tools(tools)
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    WorkflowCodeEditorSpringAIAgent workflowCodeEditorBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools, Optional<FirecrawlTools> firecrawlTools,
        ObjectProvider<CopilotChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        String name = Source.WORKFLOW_CODE_EDITOR.name() + "_" + Mode.BUILD.name();

        List<Object> tools = new ArrayList<>(
            List.of(
                readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

        firecrawlTools.ifPresent(tools::add);

        return WorkflowCodeEditorSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowCodeEditorBuildResource))
            .tools(tools)
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }
```

- [ ] **Step 3: Compile**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:compileJava`
Expected: BUILD SUCCESSFUL. (If `TaskTools` is not yet imported in this file, add `import com.bytechef.ai.mcp.tool.platform.TaskTools;` — it is already imported per the existing cluster-element beans.)

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/config/CopilotConfiguration.java
git commit -m "4076 Register workflow code editor ask/build agents"
```

---

### Task 5: Route `workflow_code_editor` in `CopilotApiController`

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-rest/src/main/java/com/bytechef/ee/ai/copilot/web/rest/CopilotApiController.java`
- Test: `server/ee/libs/ai/ai-copilot/ai-copilot-rest/src/test/java/com/bytechef/ee/ai/copilot/web/rest/CopilotApiControllerTest.java`

- [ ] **Step 1: Write the failing routing test**

First read the existing `CopilotApiControllerTest` to match its construction (it builds the controller with mocked `LocalAgent`s whose `getAgentId()` is stubbed, and an `AgUiService`). Append two tests mirroring the existing `code_editor` routing test (use the same mock-setup helpers already present in the file — `agUiParameters` with a `mode` in state, and a captured `LocalAgent`). The assertions verify that posting agentId `workflow_code_editor` with mode `ASK` runs the agent whose id is `workflow_code_editor_ask`, and with mode `BUILD` runs `workflow_code_editor_build`:

```java
    @Test
    void testChatRoutesWorkflowCodeEditorAsk() {
        LocalAgent agent = localAgent("workflow_code_editor_ask");
        CopilotApiController controller = controllerWith(agent);

        controller.chat("workflow_code_editor", agUiParameters("ASK"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesWorkflowCodeEditorBuild() {
        LocalAgent agent = localAgent("workflow_code_editor_build");
        CopilotApiController controller = controllerWith(agent);

        controller.chat("workflow_code_editor", agUiParameters("BUILD"));

        verify(agUiService).runAgent(eq(agent), any());
    }
```

If the existing test does not already have `localAgent(...)`, `controllerWith(...)`, and `agUiParameters(...)` helpers, add them by following the construction the existing `code_editor` test uses (same mocks: `AgUiService agUiService`, a `State` whose map carries `mode` and a `workflowId`, and `Optional.empty()` permission/projectWorkflow services so authorization is skipped). Match the file's existing style exactly.

- [ ] **Step 2: Run to verify fail**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-rest:test --tests "*CopilotApiControllerTest"`
Expected: FAIL — `workflow_code_editor` is not routed, so `localAgentMap.get("workflow_code_editor")` is null and no agent with the `_ask`/`_build` id is run.

- [ ] **Step 3: Add the routing branch**

In `CopilotApiController.chat(...)`, add after the `code_editor` branch:

```java
        } else if (agentId.equals("workflow_code_editor")) {
            if (Mode.valueOf((String) mode) == Mode.BUILD) {
                agentId = "workflow_code_editor_build";
            } else {
                agentId = "workflow_code_editor_ask";
            }
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-rest:test --tests "*CopilotApiControllerTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-rest/src/main/java/com/bytechef/ee/ai/copilot/web/rest/CopilotApiController.java \
        server/ee/libs/ai/ai-copilot/ai-copilot-rest/src/test/java/com/bytechef/ee/ai/copilot/web/rest/CopilotApiControllerTest.java
git commit -m "4076 Route workflow_code_editor to ask/build agents"
```

---

## Part B — Frontend

### Task 6: Add `WORKFLOW_CODE_EDITOR` to client `Source`

**Files:**
- Modify: `client/src/shared/components/copilot/stores/useCopilotStore.ts`

- [ ] **Step 1: Add the enum value (keep existing order; append last to mirror server)**

```typescript
export enum Source {
    WORKFLOW_EDITOR = 'WORKFLOW_EDITOR',
    CODE_EDITOR = 'CODE_EDITOR',
    CLUSTER_ELEMENT = 'CLUSTER_ELEMENT',
    SKILLS = 'SKILLS',
    WORKFLOW_EXECUTION = 'WORKFLOW_EXECUTION',
    WORKFLOW_CODE_EDITOR = 'WORKFLOW_CODE_EDITOR',
}
```

- [ ] **Step 2: Type-check + commit**

Run: `cd client && npm run typecheck`
Expected: no errors.

```bash
git add client/src/shared/components/copilot/stores/useCopilotStore.ts
git commit -m "4076 client - Add WORKFLOW_CODE_EDITOR copilot source"
```

---

### Task 7: `extractDefinitionFromMessage` util

**Files:**
- Create: `client/src/shared/components/copilot/utils/extractDefinitionFromMessage.ts`
- Create: `client/src/shared/components/copilot/utils/extractDefinitionFromMessage.test.ts`

Pulls the workflow definition out of an assistant message: the first fenced code block (```yaml/```json/```) if present, else the trimmed whole text. Handles assistant-ui `ThreadMessageLike` content that is either a string or an array of `{type:'text', text}` parts.

- [ ] **Step 1: Write the failing test**

```typescript
import {describe, expect, it} from 'vitest';

import {extractDefinitionFromMessage} from './extractDefinitionFromMessage';

describe('extractDefinitionFromMessage', () => {
    it('extracts a fenced json block', () => {
        const content = 'Here you go:\n```json\n{"label":"x"}\n```\nDone.';

        expect(extractDefinitionFromMessage(content)).toBe('{"label":"x"}');
    });

    it('extracts a fenced yaml block', () => {
        const content = '```yaml\nlabel: x\n```';

        expect(extractDefinitionFromMessage(content)).toBe('label: x');
    });

    it('falls back to trimmed whole text when no fence', () => {
        expect(extractDefinitionFromMessage('  label: x  ')).toBe('label: x');
    });

    it('joins array text parts before extracting', () => {
        const content = [
            {text: 'intro ```json\n{"a":1}', type: 'text'},
            {text: '}\n```', type: 'text'},
        ];

        expect(extractDefinitionFromMessage(content)).toBe('{"a":1}\n}');
    });

    it('returns empty string for empty content', () => {
        expect(extractDefinitionFromMessage(undefined)).toBe('');
    });
});
```

- [ ] **Step 2: Run to verify fail**

Run: `cd client && npx vitest run src/shared/components/copilot/utils/extractDefinitionFromMessage.test.ts`
Expected: FAIL — module missing.

- [ ] **Step 3: Implement**

```typescript
type MessageTextPart = {text?: string; type?: string};
type MessageContent = string | MessageTextPart[] | undefined;

const FENCE_REGEX = /```(?:[a-zA-Z]*)\n([\s\S]*?)```/;

export function extractDefinitionFromMessage(content: MessageContent): string {
    let text = '';

    if (typeof content === 'string') {
        text = content;
    } else if (Array.isArray(content)) {
        text = content.map((part) => part?.text ?? '').join('');
    }

    const match = FENCE_REGEX.exec(text);

    if (match) {
        return match[1].trim();
    }

    return text.trim();
}
```

- [ ] **Step 4: Run to verify pass**

Run: `cd client && npx vitest run src/shared/components/copilot/utils/extractDefinitionFromMessage.test.ts`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add client/src/shared/components/copilot/utils/extractDefinitionFromMessage.ts \
        client/src/shared/components/copilot/utils/extractDefinitionFromMessage.test.ts
git commit -m "4076 client - Add extractDefinitionFromMessage util"
```

---

### Task 8: `CopilotPanel` optional `onApply` + Apply-to-editor action

**Files:**
- Modify: `client/src/shared/components/copilot/CopilotPanel.tsx`

Add an optional `onApply` prop. When `source === Source.WORKFLOW_CODE_EDITOR` and `onApply` is provided, render an "Apply to editor" header button that extracts the latest assistant message's definition and calls `onApply`.

- [ ] **Step 1: Add the prop to both interfaces and thread it through**

In `CopilotPanelProps` add (alphabetical):

```typescript
    onApply?: (value: string) => void;
```

`CopilotPanelContent` is `Omit<CopilotPanelProps, 'open'>` — add `onApply` to its destructured params. Ensure the outer `CopilotPanel` passes `onApply` into `CopilotPanelContent`.

- [ ] **Step 2: Select `messages` from the store and add the handler**

Add `messages: state.messages` to the `useCopilotStore(useShallow(...))` selector. Add imports:

```typescript
import {extractDefinitionFromMessage} from '@/shared/components/copilot/utils/extractDefinitionFromMessage';
import {SparklesIcon} from 'lucide-react';
```

Add a derived value + handler (after the store hooks, before the existing handlers):

```typescript
    const lastAssistantMessage = [...messages].reverse().find((message) => message.role === 'assistant');

    const handleApplyClick = () => {
        if (!onApply || !lastAssistantMessage) {
            return;
        }

        const definition = extractDefinitionFromMessage(lastAssistantMessage.content);

        if (definition) {
            onApply(definition);
        }
    };
```

- [ ] **Step 3: Render the Apply button in the header**

In the header action area (next to the existing clean-messages/close buttons), add — gated so it only appears for this source when an apply handler and an assistant message exist:

```tsx
                {source === Source.WORKFLOW_CODE_EDITOR && onApply && lastAssistantMessage && (
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button
                                aria-label="Apply to editor"
                                icon={<SparklesIcon />}
                                onClick={handleApplyClick}
                                size="icon"
                                variant="ghost"
                            />
                        </TooltipTrigger>

                        <TooltipContent>Apply to editor</TooltipContent>
                    </Tooltip>
                )}
```

- [ ] **Step 4: Type-check + lint**

Run: `cd client && npm run typecheck && npx eslint src/shared/components/copilot/CopilotPanel.tsx`
Expected: clean. (`message.role`/`message.content` come from assistant-ui `ThreadMessageLike`; if `content` typing complains, the util's `MessageContent` parameter accepts `string | MessageTextPart[] | undefined` — cast `lastAssistantMessage.content as never` is NOT needed; pass it directly since `ThreadMessageLike['content']` is assignable.)

- [ ] **Step 5: Commit**

```bash
git add client/src/shared/components/copilot/CopilotPanel.tsx
git commit -m "4076 client - Add Apply-to-editor action to CopilotPanel for workflow code editor"
```

---

### Task 9: Wire the new source + format + apply into the sheet

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/hooks/useWorkflowCodeEditorSheet.ts`
- Modify: `client/src/pages/platform/workflow-editor/components/WorkflowCodeEditorSheet.tsx`

- [ ] **Step 1: Change the source + parameters in the hook**

In `useWorkflowCodeEditorSheet.ts`, `handleCopilotClick`, replace the `setContext({...})` call's `parameters`/`source`:

```typescript
        setContext({
            ...currentContext,
            mode: MODE.ASK,
            parameters: {format: workflow.format?.toLowerCase() ?? 'json'},
            source: Source.WORKFLOW_CODE_EDITOR,
        });
```

- [ ] **Step 2: Pass `source` + `onApply` to the panel in the sheet**

In `WorkflowCodeEditorSheet.tsx`, the `<CopilotPanel ... />` (around line 299) — add the source and an apply handler that loads the definition into the editor buffer via the existing `handleDefinitionChange`:

```tsx
                <CopilotPanel
                    onApply={(value) => handleDefinitionChange(value)}
                    onClose={handleCopilotClose}
                    open={copilotPanelOpen}
                    source={Source.WORKFLOW_CODE_EDITOR}
                />
```

Ensure `Source` is imported in `WorkflowCodeEditorSheet.tsx` (from `@/shared/components/copilot/stores/useCopilotStore`). `handleDefinitionChange` is already destructured from the hook.

- [ ] **Step 3: Type-check + lint**

Run: `cd client && npm run typecheck && npx eslint src/pages/platform/workflow-editor/hooks/useWorkflowCodeEditorSheet.ts src/pages/platform/workflow-editor/components/WorkflowCodeEditorSheet.tsx`
Expected: clean.

- [ ] **Step 4: Manual-behavior note (no brittle hook test)**

`useWorkflowCodeEditorSheet` has many providers/stores as dependencies; the codebase does not unit-test it. The source/format change is verified by typecheck + the `extractDefinitionFromMessage` and routing tests. Confirm no regression by running the workflow-editor component test bucket:

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTabSampleDataDialog.test.tsx`
Expected: still PASS (sanity that shared copilot changes didn't break adjacent components).

- [ ] **Step 5: Commit**

```bash
git add client/src/pages/platform/workflow-editor/hooks/useWorkflowCodeEditorSheet.ts \
        client/src/pages/platform/workflow-editor/components/WorkflowCodeEditorSheet.tsx
git commit -m "4076 client - Route workflow code editor to dedicated copilot source with apply"
```

---

## Part C — Verification

### Task 10: Full server + client checks

- [ ] **Step 1: Server format + checks**

```bash
./gradlew spotlessApply
```
Revert any reformatting in files NOT part of this change (stage only your own files). Then:

```bash
./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-api:check \
          :server:ee:libs:ai:ai-copilot:ai-copilot-service:test \
          :server:ee:libs:ai:ai-copilot:ai-copilot-rest:test
```
Expected: api `check` SUCCESSFUL; service + rest `test` SUCCESSFUL. (Use `:ai-copilot-service:test` not `:check` — there is a KNOWN pre-existing PMD `EmptyCatchBlock` failure in `agent/WorkflowExecutionSpringAIAgent.java`, unrelated to this work.)

- [ ] **Step 2: Client full check**

```bash
cd client && npm run check
```
Expected: lint + typecheck + tests pass.

- [ ] **Step 3: Commit any spotless reformatting of your files**

```bash
git add -A
git commit -m "4076 Apply spotless formatting" || echo "nothing to format"
```

---

## Self-Review

**Spec coverage:**
- New `WORKFLOW_CODE_EDITOR` source (server + client) → Tasks 1, 6. ✓
- Routing branch → Task 5. ✓
- Dedicated agent (format-aware) + prompts + workflow tools (no ScriptTools) → Tasks 2, 3, 4. ✓
- Client sends new source + real format → Task 9. ✓
- Apply-to-editor (Build output into Monaco buffer; review + Save manual) → Tasks 7, 8, 9. ✓
- Per-node Script copilot untouched (only added a branch/source; `code_editor` beans + `Source.CODE_EDITOR` usage unchanged). ✓
- `ff-4076` gating unchanged (button still gated by `ff_4076 && copilotEnabled` in `WorkflowCodeEditorSheet`). ✓

**Deviation from spec (documented):** the spec floated a Zustand apply-handler *registry*; the plan uses a simpler optional `onApply` prop on `CopilotPanel` (the sheet renders the panel directly, so no global registry is needed). Same behavior, fewer moving parts — within the spec's "exact hook finalized in plan grounding" latitude. Also: Apply is a single header action that grabs the latest assistant code block (robust) rather than a per-code-block button (would require customizing assistant-ui message rendering).

**Type consistency:** `Source.WORKFLOW_CODE_EDITOR` string `'WORKFLOW_CODE_EDITOR'` → lowercased agentId `workflow_code_editor` → routed to `workflow_code_editor_ask`/`_build` = the bean `agentId`s (`Source.WORKFLOW_CODE_EDITOR.name() + "_" + Mode.X.name()` lowercased). `formatInstruction(String)` used in agent + test. `onApply(value: string)` consistent across CopilotPanel ↔ sheet ↔ `handleDefinitionChange(value)`. `extractDefinitionFromMessage(content)` signature consistent in util + CopilotPanel.

**Placeholder scan:** every code step has full content; prompt files are complete; the one place that says "match the existing test's helpers" (Task 5 Step 1) points at a concrete existing test to mirror because that file's exact mock-helper names must be read first — the two new test methods and the routing code are given in full.
