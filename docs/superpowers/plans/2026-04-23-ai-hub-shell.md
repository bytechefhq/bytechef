# AI Hub Shell (v1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a full-page EE-only AI Hub at `/automation/ai-hub` that pairs the existing EE `CopilotPanel` with a collapsible resource panel hosting workspace files in closable tabs (Editor / Preview / Split view modes), driven by a new `ai_hub` Spring AI agent.

**Architecture:** A new `AiHubSpringAIAgent` (mirrors `FilesSpringAIAgent`) reuses the existing asset-file tool callbacks and adds one signaling tool `openFileTab` whose server-side call is a no-op; the client's `AiHubRuntimeProvider` intercepts that tool's result event to update a client-only `useAiHubTabsStore`. The `CopilotPanel` is refactored to accept `widthMode` and a custom `RuntimeProvider` so it can render full-height inside the new two-pane page. Tabs are pure client state. The agent learns the current tab context via AG-UI `state` injection on each turn (same pattern `WorkflowEditorSpringAIAgent` uses for `workflowId`).

**Tech Stack:** Java 25, Spring Boot 4.0.5, Spring AI, AG-UI server/core (`com.agui.*`), React 19, TypeScript 5.9, Vite, Zustand, `@assistant-ui/react`, `@ag-ui/client`, `react-resizable-panels` (already present), `react-markdown` (new dep, added in Task 7).

**Reference spec:** [docs/superpowers/specs/2026-04-23-ai-hub-shell-design.md](../specs/2026-04-23-ai-hub-shell-design.md).

---

## File structure

### Server (EE)

| Action | Path | Responsibility |
|---|---|---|
| Modify | `server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/util/Source.java` | Add `AI_HUB` enum value |
| Create | `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/tool/OpenFileTabToolCallback.java` | Signaling-only ToolCallback — echoes args back as JSON for the client subscriber to intercept |
| Create | `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/tool/OpenFileTabToolCallbackTest.java` | Unit test for the tool callback |
| Create | `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/agent/AiHubSpringAIAgent.java` | New SpringAIAgent, mirrors `FilesSpringAIAgent`; injects `currentTabs`/`activeFileId` as system-message context |
| Create | `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/agent/AiHubSpringAIAgentTest.java` | Unit test for `buildInvocationContext` and system-message context injection |
| Create | `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_ai_hub.txt` | System prompt for the agent |
| Modify | `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/config/CopilotConfiguration.java` | Register `aiHubSpringAIAgent` bean with direct `OpenFileTabToolCallback` instance (not a `@Bean` — keeps the signaling tool scoped to this agent) |
| Modify | `server/ee/libs/ai/ai-copilot/ai-copilot-rest/src/main/java/com/bytechef/ee/ai/copilot/web/rest/CopilotApiController.java` | Add `case "ai_hub"` in agentId switch (v1 is ASK-only server-side; no BUILD branch) |

### Client

| Action | Path | Responsibility |
|---|---|---|
| Modify | `client/package.json` | Add `react-markdown` dependency |
| Modify | `client/src/shared/components/copilot/stores/useCopilotStore.ts` | Add `AI_HUB` to `Source` enum |
| Create | `client/src/pages/automation/ai-hub/AiHubPanel.tsx` | Full-height chat panel for the AI Hub route. Mirrors `CopilotPanel`'s header (title + ASK/BUILD toggle + clean-messages button) and body (`<Thread />` inside a runtime provider), but uses `AiHubRuntimeProvider`, fills its container, and has no slide-in animation or close button. **Shared `CopilotPanel` is not modified.** |
| Create | `client/src/pages/automation/ai-hub/stores/useAiHubTabsStore.ts` | Zustand store — tabs, active tab, right-panel visibility, per-tab view mode, default-view-mode rule |
| Create | `client/src/pages/automation/ai-hub/stores/tests/useAiHubTabsStore.test.ts` | Tests for the store |
| Create | `client/src/pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider.tsx` | Wraps `<Thread />`; intercepts `openFileTab` tool result; injects `currentTabs`/`activeFileId` into AG-UI state |
| Create | `client/src/pages/automation/ai-hub/runtime-providers/tests/AiHubRuntimeProvider.test.tsx` | Tests for subscriber + state injection |
| Create | `client/src/pages/automation/ai-hub/AiHubFileViewer.tsx` | Editor / Preview / Split modes over a single file |
| Create | `client/src/pages/automation/ai-hub/tests/AiHubFileViewer.test.tsx` | Tests for each view mode |
| Create | `client/src/pages/automation/ai-hub/AiHubFilePicker.tsx` | Compact popover listing workspace asset files; triggered by `+` button |
| Create | `client/src/pages/automation/ai-hub/AiHubResourcePanel.tsx` | Tab strip + file viewer body + `+` picker + close-panel button |
| Create | `client/src/pages/automation/ai-hub/AiHub.tsx` | Route page; two-pane layout with `ResizablePanelGroup` |
| Modify | `client/src/routes.tsx` | Add lazy import + route at `/automation/ai-hub` wrapped in `EEVersion` |
| Modify | `client/src/App.tsx` | Add `{href: '/automation/ai-hub', …}` to `automationNavigation` array, EE-gated inline |

### Commit convention

Per CLAUDE.md, client-side changes use `<ticket> client - <description>` and server-side `<ticket> <description>`. This plan has no external ticket; every commit starts with `CC1 client - …` or `CC1 …` as a placeholder ticket the user can rewrite before pushing.

---

## Task 1: Add `AI_HUB` to the `Source` enum

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/util/Source.java`

- [ ] **Step 1: Modify the enum**

Replace the enum body to add the new value:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.util;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum Source {

    WORKFLOW_EDITOR, CODE_EDITOR, CLUSTER_ELEMENT, FILES, AI_HUB
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-api:compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/util/Source.java
git commit -m "CC1 Add AI_HUB to ai-copilot Source enum

New enum value for the AI Hub shell's Spring AI agent.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: Implement `OpenFileTabToolCallback` (TDD)

**Files:**
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/tool/OpenFileTabToolCallback.java`
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/tool/OpenFileTabToolCallbackTest.java`

- [ ] **Step 1: Write the failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class OpenFileTabToolCallbackTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testToolDefinitionExposesOpenFileTabName() {
        OpenFileTabToolCallback callback = new OpenFileTabToolCallback();

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("openFileTab");
        assertThat(definition.description()).isNotBlank();
        assertThat(definition.inputSchema()).contains("fileId");
        assertThat(definition.inputSchema()).contains("name");
    }

    @Test
    void testCallEchoesArgumentsAsOpenedPayload() throws Exception {
        OpenFileTabToolCallback callback = new OpenFileTabToolCallback();

        String result = callback.call("{\"fileId\":\"42\",\"name\":\"spec.md\"}");

        JsonNode node = objectMapper.readTree(result);

        assertThat(node.get("opened")
            .asBoolean()).isTrue();
        assertThat(node.get("fileId")
            .asText()).isEqualTo("42");
        assertThat(node.get("name")
            .asText()).isEqualTo("spec.md");
    }

    @Test
    void testCallReturnsErrorOnInvalidJson() throws Exception {
        OpenFileTabToolCallback callback = new OpenFileTabToolCallback();

        String result = callback.call("not-json");

        JsonNode node = objectMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }

    @Test
    void testCallReturnsErrorWhenFileIdMissing() throws Exception {
        OpenFileTabToolCallback callback = new OpenFileTabToolCallback();

        String result = callback.call("{\"name\":\"spec.md\"}");

        JsonNode node = objectMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests OpenFileTabToolCallbackTest`
Expected: FAIL — class `OpenFileTabToolCallback` not found.

- [ ] **Step 3: Implement the tool callback**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Signaling-only Spring AI {@link ToolCallback} that lets the AI Hub agent request a
 * workspace file to be opened in the client resource panel. The server-side implementation is a
 * no-op that echoes the arguments back as a JSON result; the AI Hub client subscriber
 * intercepts the tool-call result event and updates the tabs store.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class OpenFileTabToolCallback implements ToolCallback {

    private static final String DESCRIPTION = """
        Open a workspace asset file in the AI Hub resource panel so the user can see it.
        Call this after creating a file (via createAssetFile) or when referring to an existing
        file. Use the fileId returned from createAssetFile or listAssetFiles - never invent file
        IDs.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "fileId": {"type": "string", "description": "Workspace asset file id"},
                "name": {"type": "string", "description": "Display name for the tab"}
            },
            "required": ["fileId", "name"]
        }""";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("openFileTab")
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
            OpenFileTabInput input = objectMapper.readValue(toolInput, OpenFileTabInput.class);

            if (input.fileId() == null || input.fileId()
                .isBlank()) {
                return toolError("fileId is required");
            }

            if (input.name() == null || input.name()
                .isBlank()) {
                return toolError("name is required");
            }

            return objectMapper.writeValueAsString(
                new OpenFileTabOutput(true, input.fileId(), input.name()));
        } catch (JsonProcessingException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        }
    }

    private String toolError(String message) {
        try {
            return objectMapper.writeValueAsString(Map.of("error", message));
        } catch (JsonProcessingException exception) {
            return "{\"error\":\"serialization failure\"}";
        }
    }

    public record OpenFileTabInput(String fileId, String name) {
    }

    public record OpenFileTabOutput(boolean opened, String fileId, String name) {
    }
}
```

- [ ] **Step 4: Run the test to confirm it passes**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests OpenFileTabToolCallbackTest`
Expected: PASS (4 tests).

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply -p server/ee/libs/ai/ai-copilot/ai-copilot-service
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/tool/OpenFileTabToolCallback.java \
        server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/tool/OpenFileTabToolCallbackTest.java
git commit -m "CC1 Add OpenFileTabToolCallback signaling tool for AI Hub agent

Server-side no-op that echoes fileId/name back as a JSON tool result so the
AI Hub client subscriber can pick it up and open a file tab.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: Create AI Hub system prompt resource

**Files:**
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_ai_hub.txt`

- [ ] **Step 1: Write the prompt file**

```text
You are the AI Hub assistant for ByteChef. You help the user explore and author
workspace content from a chat-first interface paired with a resource panel that shows files
as tabs.

You can:
- List the user's workspace files by calling listAssetFiles.
- Read a file's content with getAssetFileContent.
- Create a new text file with createAssetFile (markdown, plain text, CSV, JSON, code).
- Open any workspace file in the user's resource panel by calling openFileTab({fileId, name}).

Always call openFileTab after creating or referencing a file so the user sees it in the
resource panel. Use the fileId returned from createAssetFile, or from listAssetFiles for
existing files. Never invent file IDs.

Prefer markdown for specs, notes, and reports; CSV for tabular data; code for executable
snippets.

If the user refers to "this file" or "the file I'm viewing" without specifying, consult the
State's activeFileId and the "Open Tabs" context block appended below. If neither is set,
ask a clarifying question before acting.

Keep responses focused. The user sees your files in the right-hand panel, so summarize
briefly in chat rather than restating the file contents.
```

- [ ] **Step 2: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_ai_hub.txt
git commit -m "CC1 Add AI Hub agent system prompt

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: Implement `AiHubSpringAIAgent` (TDD)

**Files:**
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/agent/AiHubSpringAIAgent.java`
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/agent/AiHubSpringAIAgentTest.java`

- [ ] **Step 1: Write the failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.agui.core.agent.RunAgentInput;
import com.agui.core.context.Context;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.SystemMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.bytechef.ee.ai.copilot.util.Source;
import com.bytechef.ee.automation.aihub.tool.WorkspaceInvocationContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubSpringAIAgentTest {

    @Test
    void testBuildInvocationContextExtractsWorkspaceIdFromState() throws AGUIException {
        AiHubSpringAIAgent agent = newAgent();

        State state = new State();

        state.set("workspaceId", 7L);

        UserMessage userMessage = new UserMessage();

        userMessage.setContent("Draft a spec");

        RunAgentInput input = new RunAgentInput(
            "thread", "run", state, List.of((BaseMessage) userMessage), List.of(), List.of(), null);

        WorkspaceInvocationContext context = agent.buildInvocationContext(input);

        assertThat(context.workspaceId()).isEqualTo(7L);
        assertThat(context.sourceOrdinal()).isEqualTo((short) Source.AI_HUB.ordinal());
        assertThat(context.lastUserPrompt()).isEqualTo("Draft a spec");
    }

    @Test
    void testCreateSystemMessageIncludesActiveFileAndOpenTabsContext() throws AGUIException {
        AiHubSpringAIAgent agent = newAgent();

        State state = new State();

        state.set("workspaceId", 7L);
        state.set("activeFileId", "42");
        state.set(
            "currentTabs",
            List.of(
                Map.of("fileId", "42", "name", "spec.md", "viewMode", "preview"),
                Map.of("fileId", "43", "name", "notes.md", "viewMode", "editor")));

        SystemMessage systemMessage = agent.createSystemMessage(state, new ArrayList<>());

        assertThat(systemMessage.getContent()).contains("Active File");
        assertThat(systemMessage.getContent()).contains("42");
        assertThat(systemMessage.getContent()).contains("Open Tabs");
        assertThat(systemMessage.getContent()).contains("spec.md");
        assertThat(systemMessage.getContent()).contains("notes.md");
    }

    @Test
    void testCreateSystemMessageOmitsTabBlocksWhenEmpty() throws AGUIException {
        AiHubSpringAIAgent agent = newAgent();

        State state = new State();

        state.set("workspaceId", 7L);

        SystemMessage systemMessage = agent.createSystemMessage(state, new ArrayList<>());

        assertThat(systemMessage.getContent()).doesNotContain("Active File");
        assertThat(systemMessage.getContent()).doesNotContain("Open Tabs");
    }

    private AiHubSpringAIAgent newAgent() throws AGUIException {
        return AiHubSpringAIAgent.builder()
            .agentId("ai_hub")
            .chatModel((ChatModel) (prompt -> null))
            .systemMessage("test")
            .state(new State())
            .build();
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests AiHubSpringAIAgentTest`
Expected: FAIL — `AiHubSpringAIAgent` class not found.

- [ ] **Step 3: Implement the agent**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.agent;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.context.Context;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.Role;
import com.agui.core.message.SystemMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.agui.server.LocalAgent;
import com.agui.spring.ai.SpringAIAgent;
import com.bytechef.ee.ai.copilot.util.Source;
import com.bytechef.ee.automation.assetfile.ai.tool.AgUiToolContextWorkspaceContextProvider;
import com.bytechef.ee.automation.aihub.tool.WorkspaceInvocationContext;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

/**
 * AI Hub copilot agent. Mirrors {@link FilesSpringAIAgent} for workspace-context
 * propagation and extends it by injecting the client's currently-open tabs and active file id
 * into the system message so the LLM can reason about "the file the user is viewing".
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AiHubSpringAIAgent extends SpringAIAgent {

    private static final String ADDITIONAL_RULES =
        """
            ## Additional Rules

            - The assistant must not produce visual representations of any kind, including diagrams, charts, UI sketches, images, or pseudo-visuals.
            - When the user asks for a file (spec, runbook, CSV, JSON, markdown note, code file), produce the content and save it by calling createAssetFile.
            - After creating or referencing a file, always call openFileTab({fileId, name}) so the user sees it in the right-hand resource panel.
            - Before referring to existing files, call listAssetFiles to discover what is available.
            - When editing an existing file, call getAssetFileContent first, then call createAssetFile with the updated content.
            """;

    protected AiHubSpringAIAgent(final Builder builder) throws AGUIException {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected void run(RunAgentInput input, AgentSubscriber subscriber) {
        WorkspaceInvocationContext context = buildInvocationContext(input);

        AgUiToolContextWorkspaceContextProvider.runWithContext(context, () -> super.run(input, subscriber));
    }

    WorkspaceInvocationContext buildInvocationContext(RunAgentInput input) {
        State state = input.state();

        Long workspaceId = state == null ? null : asLong(state.get("workspaceId"));
        Short sourceOrdinal = (short) Source.AI_HUB.ordinal();
        String lastUserPrompt = lastUserPrompt(input.messages());

        return new WorkspaceInvocationContext(workspaceId, sourceOrdinal, lastUserPrompt);
    }

    @Override
    protected SystemMessage createSystemMessage(State state, List<Context> contexts) {
        Object activeFileId = state == null ? null : state.get("activeFileId");

        if (activeFileId != null) {
            contexts.add(new Context("Active File", String.valueOf(activeFileId)));
        }

        Object currentTabs = state == null ? null : state.get("currentTabs");

        if (currentTabs instanceof List<?> tabs && !tabs.isEmpty()) {
            contexts.add(new Context("Open Tabs", formatTabs(tabs)));
        }

        List<String> contextStrings = contexts.stream()
            .map(Context::toString)
            .toList();

        String resolvedMessage = Objects.nonNull(this.systemMessageProvider)
            ? this.systemMessageProvider.apply(this) : this.systemMessage;

        String message = "%s%n%s%n%nState:%n%s%n%nContext:%n%s%n".formatted(
            resolvedMessage, ADDITIONAL_RULES, state, String.join("\n", contextStrings));

        SystemMessage systemMessage = new SystemMessage();

        systemMessage.setId(String.valueOf(UUID.randomUUID()));
        systemMessage.setContent(message);

        return systemMessage;
    }

    private static String formatTabs(List<?> tabs) {
        StringBuilder stringBuilder = new StringBuilder();

        for (Object tab : tabs) {
            if (tab instanceof Map<?, ?> tabMap) {
                stringBuilder.append("- fileId=")
                    .append(tabMap.get("fileId"))
                    .append(", name=")
                    .append(tabMap.get("name"))
                    .append(", viewMode=")
                    .append(tabMap.get("viewMode"))
                    .append("\n");
            }
        }

        return stringBuilder.toString();
    }

    private static Long asLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Long longValue) {
            return longValue;
        }

        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }

        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        return null;
    }

    private static String lastUserPrompt(List<BaseMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        for (int i = messages.size() - 1; i >= 0; i--) {
            BaseMessage message = messages.get(i);

            if (message instanceof UserMessage userMessage && Role.user.equals(userMessage.getRole())) {
                return userMessage.getContent();
            }
        }

        return null;
    }

    public static class Builder extends SpringAIAgent.Builder {

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

        public AiHubSpringAIAgent build() throws AGUIException {

            return new AiHubSpringAIAgent(this);
        }
    }
}
```

- [ ] **Step 4: Run the test to confirm it passes**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests AiHubSpringAIAgentTest`
Expected: PASS (3 tests).

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply -p server/ee/libs/ai/ai-copilot/ai-copilot-service
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/agent/AiHubSpringAIAgent.java \
        server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/agent/AiHubSpringAIAgentTest.java
git commit -m "CC1 Add AiHubSpringAIAgent

Mirrors FilesSpringAIAgent for workspace-context propagation. Appends active
file id and open-tabs list from AG-UI state as system-message context so the
LLM can reason about the file the user is currently viewing.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: Register the agent bean in `CopilotConfiguration`

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/config/CopilotConfiguration.java`

- [ ] **Step 1: Add the prompt resource field and constructor arg**

In `CopilotConfiguration.java`:

1. Add a new `private final Resource promptAiHubResource;` field next to `promptFilesResource` (~line 92).
2. Add constructor parameter `@Value("classpath:prompt_ai_hub.txt") Resource promptAiHubResource` after the existing `promptFiles` parameter (~line 104).
3. Add `this.promptAiHubResource = promptAiHubResource;` in the constructor body (~line 135).

- [ ] **Step 2: Add the agent `@Bean`**

Append this `@Bean` method to `CopilotConfiguration` (right after `filesSpringAIAgent`, ~line 293):

```java
@Bean
AiHubSpringAIAgent aiHubSpringAIAgent(
    ChatMemory chatMemory, ChatModel chatModel, ObjectProvider<ToolCallback> toolCallbackProvider)
    throws AGUIException {

    String name = Source.AI_HUB.name();

    List<ToolCallback> toolCallbacks = new ArrayList<>(toolCallbackProvider.orderedStream()
        .toList());

    toolCallbacks.add(new OpenFileTabToolCallback());

    return AiHubSpringAIAgent.builder()
        .agentId(name.toLowerCase())
        .chatMemory(chatMemory)
        .chatModel(chatModel)
        .systemMessage(getSystemPrompt(promptAiHubResource))
        .toolCallbacks(toolCallbacks)
        .state(state)
        .build();
}
```

- [ ] **Step 3: Add imports**

Add at the top of the file:

```java
import com.bytechef.ee.ai.copilot.agent.AiHubSpringAIAgent;
import com.bytechef.ee.ai.copilot.tool.OpenFileTabToolCallback;
```

- [ ] **Step 4: Compile**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply -p server/ee/libs/ai/ai-copilot/ai-copilot-service
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/config/CopilotConfiguration.java
git commit -m "CC1 Register AiHubSpringAIAgent in CopilotConfiguration

Scopes OpenFileTabToolCallback to this agent only (instantiated inline rather
than as a @Bean) so other agents are not prompted with a tool they have no
subscriber for.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: Route `ai_hub` in `CopilotApiController`

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-rest/src/main/java/com/bytechef/ee/ai/copilot/web/rest/CopilotApiController.java`

- [ ] **Step 1: Add the switch case**

Locate the `switch (agentId)` block at lines 60-70. Add the `ai_hub` case immediately before `default`:

```java
agentId = switch (agentId) {
    case "workflow_editor" -> Mode.valueOf((String) mode) == Mode.BUILD
        ? "workflow_editor_build" : "workflow_editor_ask";
    case "code_editor" -> Mode.valueOf((String) mode) == Mode.BUILD
        ? "code_editor_build" : "code_editor_ask";
    case "cluster_element" -> Mode.valueOf((String) mode) == Mode.BUILD
        ? "cluster_element_build" : "cluster_element_ask";
    // FilesSpringAIAgent has no ASK/BUILD split; dispatch directly by agentId.
    case "files" -> "files";
    // AiHubSpringAIAgent is ASK-only in v1; BUILD from the UI is a no-op and still
    // routes to the same agent.
    case "ai_hub" -> "ai_hub";
    default -> agentId;
};
```

- [ ] **Step 2: Compile**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-rest:compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Format and commit**

```bash
./gradlew spotlessApply -p server/ee/libs/ai/ai-copilot/ai-copilot-rest
git add server/ee/libs/ai/ai-copilot/ai-copilot-rest/src/main/java/com/bytechef/ee/ai/copilot/web/rest/CopilotApiController.java
git commit -m "CC1 Route ai_hub agentId in CopilotApiController

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: Add `AI_HUB` to client `Source` enum and install `react-markdown`

**Files:**
- Modify: `client/src/shared/components/copilot/stores/useCopilotStore.ts`
- Modify: `client/package.json`

- [ ] **Step 1: Install `react-markdown`**

```bash
cd client && npm install react-markdown@9 && cd ..
```
Expected: `react-markdown` appears in `client/package.json` under `dependencies`.

- [ ] **Step 2: Extend the `Source` enum**

Edit `client/src/shared/components/copilot/stores/useCopilotStore.ts` lines 14-19, replacing with:

```ts
export enum Source {
    WORKFLOW_EDITOR = 'WORKFLOW_EDITOR',
    CODE_EDITOR = 'CODE_EDITOR',
    CLUSTER_ELEMENT = 'CLUSTER_ELEMENT',
    FILES = 'FILES',
    AI_HUB = 'AI_HUB',
}
```

- [ ] **Step 3: Run client typecheck**

```bash
cd client && npm run typecheck && cd ..
```
Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add client/package.json client/package-lock.json client/src/shared/components/copilot/stores/useCopilotStore.ts
git commit -m "CC1 client - Add AI_HUB Source enum and react-markdown dep

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 8: Create `AiHubPanel` component (new file — shared `CopilotPanel` is not modified)

**Files:**
- Create: `client/src/pages/automation/ai-hub/AiHubPanel.tsx`
- Create: `client/src/pages/automation/ai-hub/tests/AiHubPanel.test.tsx`

- [ ] **Step 1: Write the component**

Create `client/src/pages/automation/ai-hub/AiHubPanel.tsx`:

```tsx
import Button from '@/components/Button/Button';
import {Thread} from '@/components/assistant-ui/thread';
import {ToggleGroup, ToggleGroupItem} from '@/components/ui/toggle-group';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {AiHubRuntimeProvider} from '@/pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider';
import CopilotPanelBoundary from '@/shared/components/copilot/CopilotPanelBoundary';
import {MODE, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {BotMessageSquareIcon, MessageSquareXIcon} from 'lucide-react';
import {useEffect, useRef} from 'react';
import {useLocation} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

const AiHubPanel = () => {
    const {context, generateConversationId, resetMessages, setContext} = useCopilotStore(
        useShallow((state) => ({
            context: state.context,
            generateConversationId: state.generateConversationId,
            resetMessages: state.resetMessages,
            setContext: state.setContext,
        }))
    );
    const location = useLocation();

    const previousPathnameRef = useRef(location.pathname);

    useEffect(() => {
        if (previousPathnameRef.current !== location.pathname) {
            previousPathnameRef.current = location.pathname;

            generateConversationId();
            resetMessages();
        }
    }, [generateConversationId, location.pathname, resetMessages]);

    const handleCleanMessages = () => {
        resetMessages();
        generateConversationId();
    };

    return (
        <CopilotPanelBoundary open={true}>
            <div className="relative flex size-full min-h-[50vh] flex-col bg-surface-main">
                <div className="flex items-center justify-between px-4 py-3">
                    <div className="flex items-center space-x-1">
                        <BotMessageSquareIcon className="size-6" /> <h4>AI Copilot</h4>
                    </div>

                    <div className="flex items-center gap-2">
                        <ToggleGroup
                            onValueChange={(value) => {
                                if (value) {
                                    setContext({
                                        ...context,
                                        mode: value as MODE,
                                    });
                                }
                            }}
                            type="single"
                            value={context?.mode}
                        >
                            <ToggleGroupItem value={MODE.ASK}>
                                {MODE.ASK.charAt(0) + MODE.ASK.slice(1).toLowerCase()}
                            </ToggleGroupItem>

                            <ToggleGroupItem value={MODE.BUILD}>
                                {MODE.BUILD.charAt(0) + MODE.BUILD.slice(1).toLowerCase()}
                            </ToggleGroupItem>
                        </ToggleGroup>

                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    icon={<MessageSquareXIcon />}
                                    onClick={handleCleanMessages}
                                    size="icon"
                                    variant="ghost"
                                />
                            </TooltipTrigger>

                            <TooltipContent>Clean messages</TooltipContent>
                        </Tooltip>
                    </div>
                </div>

                <div className="relative -mx-1 min-h-0 flex-1">
                    <AiHubRuntimeProvider>
                        <Thread />
                    </AiHubRuntimeProvider>
                </div>
            </div>
        </CopilotPanelBoundary>
    );
};

export default AiHubPanel;
```

This mirrors `CopilotPanel`'s header (title, ASK/BUILD toggle, clean-messages button) and body (`<Thread />` inside a runtime provider), but:
- Fills its container (`flex size-full flex-col`) — no hardcoded width, no slide-in animation.
- Uses `AiHubRuntimeProvider` (introduced in Task 10) instead of `CopilotRuntimeProvider`.
- No close button — the AI Hub is a dedicated route, not a floating panel.
- Shared `useCopilotStore` still holds `messages`, `context`, and `conversationId`, so state behavior matches the existing panel.

- [ ] **Step 2: Write the test**

Create `client/src/pages/automation/ai-hub/tests/AiHubPanel.test.tsx`:

```tsx
/* eslint-disable sort-keys */
import AiHubPanel from '@/pages/automation/ai-hub/AiHubPanel';
import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

vi.mock('@/pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider', () => ({
    AiHubRuntimeProvider: ({children}: {children: ReactNode}) => (
        <div data-testid="cc-runtime">{children}</div>
    ),
}));

vi.mock('@/components/assistant-ui/thread', () => ({
    Thread: () => <div data-testid="thread" />,
}));

const wrap = (ui: ReactNode) => render(<MemoryRouter><TooltipProvider>{ui}</TooltipProvider></MemoryRouter>);

describe('AiHubPanel', () => {
    it('renders the AI Copilot header, ASK/BUILD toggle, and the AiHubRuntimeProvider body', () => {
        wrap(<AiHubPanel />);

        expect(screen.getByText('AI Copilot')).toBeInTheDocument();
        expect(screen.getByRole('radio', {name: /ask/i})).toBeInTheDocument();
        expect(screen.getByRole('radio', {name: /build/i})).toBeInTheDocument();
        expect(screen.getByTestId('cc-runtime')).toBeInTheDocument();
        expect(screen.getByTestId('thread')).toBeInTheDocument();
    });

    it('does not render a close button', () => {
        wrap(<AiHubPanel />);

        // CopilotPanel renders a close button with the X icon; AiHubPanel must not.
        expect(screen.queryByRole('button', {name: /close/i})).toBeNull();
    });

    it('fills its container (no hardcoded w-[450px])', () => {
        const {container} = wrap(<AiHubPanel />);

        expect(container.querySelector('.w-\\[450px\\]')).toBeNull();
        expect(container.querySelector('.size-full')).not.toBeNull();
    });
});
```

- [ ] **Step 3: Run typecheck + tests**

```bash
cd client && npm run typecheck && npm run test -- AiHubPanel && cd ..
```
Expected: typecheck passes; 3 AiHubPanel tests pass.

- [ ] **Step 4: Commit**

```bash
git add client/src/pages/automation/ai-hub/AiHubPanel.tsx \
        client/src/pages/automation/ai-hub/tests/AiHubPanel.test.tsx
git commit -m "CC1 client - Add AiHubPanel component

Dedicated full-height chat panel for the AI Hub route. Mirrors the
shared CopilotPanel visually (AI Copilot header, ASK/BUILD toggle, clean
button) but uses AiHubRuntimeProvider, fills its container, and has
no close button or slide-in animation. Shared CopilotPanel is not modified.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 9: Implement `useAiHubTabsStore` (TDD)

**Files:**
- Create: `client/src/pages/automation/ai-hub/stores/useAiHubTabsStore.ts`
- Create: `client/src/pages/automation/ai-hub/stores/tests/useAiHubTabsStore.test.ts`

- [ ] **Step 1: Write the failing test**

```ts
/* eslint-disable sort-keys */
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import {
    aiHubTabsStore,
    inferDefaultViewMode,
    useAiHubTabsStore,
} from '../useAiHubTabsStore';

describe('useAiHubTabsStore', () => {
    beforeEach(() => {
        aiHubTabsStore.setState({
            activeTabId: undefined,
            openTabs: [],
            rightPanelOpen: false,
        });
    });

    it('opens a new tab, sets it active, and opens the right panel', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let tabId = '';

        act(() => {
            tabId = result.current.openFileTab('42', 'spec.md');
        });

        expect(result.current.openTabs).toHaveLength(1);
        expect(result.current.openTabs[0]!.fileId).toBe('42');
        expect(result.current.openTabs[0]!.name).toBe('spec.md');
        expect(result.current.openTabs[0]!.viewMode).toBe('preview');
        expect(result.current.activeTabId).toBe(tabId);
        expect(result.current.rightPanelOpen).toBe(true);
    });

    it('focuses an existing tab when the same fileId is opened again', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let firstTabId = '';

        act(() => {
            firstTabId = result.current.openFileTab('42', 'spec.md');
            result.current.openFileTab('43', 'notes.md');
        });

        expect(result.current.openTabs).toHaveLength(2);
        expect(result.current.activeTabId).not.toBe(firstTabId);

        act(() => {
            result.current.openFileTab('42', 'spec.md');
        });

        expect(result.current.openTabs).toHaveLength(2);
        expect(result.current.activeTabId).toBe(firstTabId);
    });

    it('removes a tab and picks the neighboring tab as active', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let firstTabId = '';
        let secondTabId = '';
        let thirdTabId = '';

        act(() => {
            firstTabId = result.current.openFileTab('1', 'a.md');
            secondTabId = result.current.openFileTab('2', 'b.md');
            thirdTabId = result.current.openFileTab('3', 'c.md');
            result.current.setActiveTab(secondTabId);
        });

        act(() => {
            result.current.closeTab(secondTabId);
        });

        expect(result.current.openTabs.map((tab) => tab.id)).toEqual([firstTabId, thirdTabId]);
        expect(result.current.activeTabId).toBe(thirdTabId);
    });

    it('clears activeTabId when the last tab is closed', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let tabId = '';

        act(() => {
            tabId = result.current.openFileTab('1', 'a.md');
        });

        act(() => {
            result.current.closeTab(tabId);
        });

        expect(result.current.openTabs).toHaveLength(0);
        expect(result.current.activeTabId).toBeUndefined();
        expect(result.current.rightPanelOpen).toBe(true);
    });

    it('updates view mode only for the target tab', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let firstTabId = '';
        let secondTabId = '';

        act(() => {
            firstTabId = result.current.openFileTab('1', 'a.md');
            secondTabId = result.current.openFileTab('2', 'b.md');
        });

        act(() => {
            result.current.setViewMode(firstTabId, 'split');
        });

        expect(result.current.openTabs.find((tab) => tab.id === firstTabId)!.viewMode).toBe('split');
        expect(result.current.openTabs.find((tab) => tab.id === secondTabId)!.viewMode).toBe('preview');
    });

    describe('inferDefaultViewMode', () => {
        it('returns preview for markdown files', () => {
            expect(inferDefaultViewMode('spec.md')).toBe('preview');
            expect(inferDefaultViewMode('notes.MARKDOWN')).toBe('preview');
        });

        it('returns preview for html files', () => {
            expect(inferDefaultViewMode('index.html')).toBe('preview');
            expect(inferDefaultViewMode('page.htm')).toBe('preview');
        });

        it('returns editor for code and data files', () => {
            expect(inferDefaultViewMode('config.json')).toBe('editor');
            expect(inferDefaultViewMode('script.py')).toBe('editor');
            expect(inferDefaultViewMode('data.csv')).toBe('editor');
            expect(inferDefaultViewMode('notes.txt')).toBe('editor');
        });

        it('returns preview for unknown extensions (shows metadata placeholder)', () => {
            expect(inferDefaultViewMode('image.png')).toBe('preview');
            expect(inferDefaultViewMode('archive.zip')).toBe('preview');
        });
    });
});
```

- [ ] **Step 2: Run the test to confirm it fails**

```bash
cd client && npm run test -- useAiHubTabsStore && cd ..
```
Expected: FAIL — module not found.

- [ ] **Step 3: Implement the store**

```ts
/* eslint-disable sort-keys */
import {getRandomId} from '@/shared/util/random-utils';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export type AiHubViewModeType = 'editor' | 'preview' | 'split';

export interface AiHubTabI {
    id: string;
    fileId: string;
    name: string;
    viewMode: AiHubViewModeType;
}

interface AiHubTabsStateI {
    activeTabId: string | undefined;
    openTabs: AiHubTabI[];
    rightPanelOpen: boolean;

    closeTab: (tabId: string) => void;
    openFileTab: (fileId: string, name: string) => string;
    setActiveTab: (tabId: string) => void;
    setRightPanelOpen: (open: boolean) => void;
    setViewMode: (tabId: string, mode: AiHubViewModeType) => void;
}

const EDITOR_EXTENSIONS = new Set([
    'txt',
    'csv',
    'json',
    'yaml',
    'yml',
    'java',
    'js',
    'jsx',
    'ts',
    'tsx',
    'py',
    'rb',
    'go',
    'rs',
    'sh',
    'sql',
    'css',
    'scss',
    'xml',
    'toml',
    'ini',
    'env',
]);

const PREVIEW_EXTENSIONS = new Set(['md', 'markdown', 'html', 'htm']);

export function inferDefaultViewMode(name: string): AiHubViewModeType {
    const dotIndex = name.lastIndexOf('.');

    if (dotIndex < 0) {
        return 'editor';
    }

    const extension = name.slice(dotIndex + 1).toLowerCase();

    if (PREVIEW_EXTENSIONS.has(extension)) {
        return 'preview';
    }

    if (EDITOR_EXTENSIONS.has(extension)) {
        return 'editor';
    }

    return 'preview';
}

export const aiHubTabsStore = create<AiHubTabsStateI>()(
    devtools((set) => ({
        activeTabId: undefined,
        openTabs: [],
        rightPanelOpen: false,

        closeTab: (tabId) =>
            set((state) => {
                const closingIndex = state.openTabs.findIndex((tab) => tab.id === tabId);

                if (closingIndex < 0) {
                    return state;
                }

                const openTabs = state.openTabs.filter((tab) => tab.id !== tabId);

                let activeTabId = state.activeTabId;

                if (state.activeTabId === tabId) {
                    if (openTabs.length === 0) {
                        activeTabId = undefined;
                    } else if (closingIndex >= openTabs.length) {
                        activeTabId = openTabs[openTabs.length - 1]!.id;
                    } else {
                        activeTabId = openTabs[closingIndex]!.id;
                    }
                }

                return {...state, activeTabId, openTabs};
            }),

        openFileTab: (fileId, name) => {
            let tabIdToReturn = '';

            set((state) => {
                const existing = state.openTabs.find((tab) => tab.fileId === fileId);

                if (existing) {
                    tabIdToReturn = existing.id;

                    return {
                        ...state,
                        activeTabId: existing.id,
                        rightPanelOpen: true,
                    };
                }

                const newTab: AiHubTabI = {
                    fileId,
                    id: getRandomId(),
                    name,
                    viewMode: inferDefaultViewMode(name),
                };

                tabIdToReturn = newTab.id;

                return {
                    ...state,
                    activeTabId: newTab.id,
                    openTabs: [...state.openTabs, newTab],
                    rightPanelOpen: true,
                };
            });

            return tabIdToReturn;
        },

        setActiveTab: (tabId) =>
            set((state) => {
                if (!state.openTabs.some((tab) => tab.id === tabId)) {
                    return state;
                }

                return {...state, activeTabId: tabId};
            }),

        setRightPanelOpen: (open) => set((state) => ({...state, rightPanelOpen: open})),

        setViewMode: (tabId, mode) =>
            set((state) => ({
                ...state,
                openTabs: state.openTabs.map((tab) => (tab.id === tabId ? {...tab, viewMode: mode} : tab)),
            })),
    }))
);

export const useAiHubTabsStore = aiHubTabsStore;
```

- [ ] **Step 4: Run the test to confirm it passes**

```bash
cd client && npm run test -- useAiHubTabsStore && cd ..
```
Expected: PASS (all tests).

- [ ] **Step 5: Commit**

```bash
git add client/src/pages/automation/ai-hub/stores/useAiHubTabsStore.ts \
        client/src/pages/automation/ai-hub/stores/tests/useAiHubTabsStore.test.ts
git commit -m "CC1 client - Add useAiHubTabsStore

Tabs state for the AI Hub resource panel. Includes default view mode
inference (preview for .md/.html, editor for code/data).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 10: Implement `AiHubRuntimeProvider` (TDD)

**Files:**
- Create: `client/src/pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider.tsx`
- Create: `client/src/pages/automation/ai-hub/runtime-providers/tests/AiHubRuntimeProvider.test.tsx`

- [ ] **Step 1: Write the failing test**

```tsx
/* eslint-disable sort-keys */
import {aiHubTabsStore} from '@/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {buildAiHubSubscriber, buildStateToSend} from '../AiHubRuntimeProvider';

describe('buildAiHubSubscriber', () => {
    beforeEach(() => {
        aiHubTabsStore.setState({
            activeTabId: undefined,
            openTabs: [],
            rightPanelOpen: false,
        });
    });

    it('opens a file tab when the openFileTab tool result arrives', () => {
        const subscriber = buildAiHubSubscriber({
            appendToLastAssistantMessage: vi.fn(),
        });

        subscriber.onToolCallStartEvent!({
            event: {toolCallId: 'call-1', toolCallName: 'openFileTab'},
        } as never);

        subscriber.onToolCallResultEvent!({
            event: {
                content: JSON.stringify({opened: true, fileId: '42', name: 'spec.md'}),
                toolCallId: 'call-1',
            },
        } as never);

        const state = aiHubTabsStore.getState();

        expect(state.openTabs).toHaveLength(1);
        expect(state.openTabs[0]!.fileId).toBe('42');
        expect(state.openTabs[0]!.name).toBe('spec.md');
    });

    it('ignores tool results that are not openFileTab', () => {
        const subscriber = buildAiHubSubscriber({
            appendToLastAssistantMessage: vi.fn(),
        });

        subscriber.onToolCallStartEvent!({
            event: {toolCallId: 'call-2', toolCallName: 'someOtherTool'},
        } as never);

        subscriber.onToolCallResultEvent!({
            event: {
                content: JSON.stringify({opened: true, fileId: '42', name: 'spec.md'}),
                toolCallId: 'call-2',
            },
        } as never);

        expect(aiHubTabsStore.getState().openTabs).toHaveLength(0);
    });

    it('ignores malformed tool results', () => {
        const subscriber = buildAiHubSubscriber({
            appendToLastAssistantMessage: vi.fn(),
        });

        subscriber.onToolCallStartEvent!({
            event: {toolCallId: 'call-3', toolCallName: 'openFileTab'},
        } as never);

        subscriber.onToolCallResultEvent!({
            event: {content: 'not-json', toolCallId: 'call-3'},
        } as never);

        expect(aiHubTabsStore.getState().openTabs).toHaveLength(0);
    });
});

describe('buildStateToSend', () => {
    beforeEach(() => {
        aiHubTabsStore.setState({
            activeTabId: undefined,
            openTabs: [],
            rightPanelOpen: false,
        });
    });

    it('includes currentTabs and activeFileId from the tabs store', () => {
        aiHubTabsStore.getState().openFileTab('42', 'spec.md');

        const state = buildStateToSend({
            context: {mode: 'ASK', parameters: {}, source: 'AI_HUB'} as never,
            workspaceId: 7,
        });

        expect(state.currentTabs).toHaveLength(1);
        expect(state.currentTabs[0]!.fileId).toBe('42');
        expect(state.activeFileId).toBe('42');
        expect(state.workspaceId).toBe('7');
        expect(state.source).toBe('AI_HUB');
        expect(state.mode).toBe('ASK');
    });

    it('uses null activeFileId when no tab is active', () => {
        const state = buildStateToSend({
            context: {mode: 'ASK', parameters: {}, source: 'AI_HUB'} as never,
            workspaceId: 7,
        });

        expect(state.activeFileId).toBeNull();
        expect(state.currentTabs).toHaveLength(0);
    });
});
```

- [ ] **Step 2: Run the test to confirm it fails**

```bash
cd client && npm run test -- AiHubRuntimeProvider && cd ..
```
Expected: FAIL — module not found.

- [ ] **Step 3: Implement the runtime provider**

```tsx
import {aiHubTabsStore} from '@/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ContextType, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {getCookie} from '@/shared/util/cookie-utils';
import {getRandomId} from '@/shared/util/random-utils';
import {AgentSubscriber, HttpAgent} from '@ag-ui/client';
import {AppendMessage, AssistantRuntimeProvider, ThreadMessageLike, useExternalStoreRuntime} from '@assistant-ui/react';
import {ReactNode, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

const convertMessage = (message: ThreadMessageLike): ThreadMessageLike => message;

interface OpenFileTabResultI {
    fileId?: string;
    name?: string;
    opened?: boolean;
    error?: string;
}

const parseJson = (content: string): OpenFileTabResultI | null => {
    try {
        return JSON.parse(content) as OpenFileTabResultI;
    } catch {
        return null;
    }
};

interface BuildSubscriberDepsI {
    appendToLastAssistantMessage: (text: string) => void;
}

export const buildAiHubSubscriber = ({
    appendToLastAssistantMessage,
}: BuildSubscriberDepsI): AgentSubscriber => {
    const toolCallNamesById = new Map<string, string>();

    return {
        onTextMessageContentEvent: ({event, textMessageBuffer}) => {
            appendToLastAssistantMessage(textMessageBuffer + event.delta);
        },
        onTextMessageEndEvent: ({textMessageBuffer}) => {
            appendToLastAssistantMessage(textMessageBuffer);
        },
        onToolCallResultEvent: ({event}) => {
            const toolCallName = toolCallNamesById.get(event.toolCallId);

            toolCallNamesById.delete(event.toolCallId);

            if (toolCallName !== 'openFileTab') {
                return;
            }

            const parsed = parseJson(event.content);

            if (!parsed || parsed.error || !parsed.opened || !parsed.fileId || !parsed.name) {
                return;
            }

            aiHubTabsStore.getState().openFileTab(parsed.fileId, parsed.name);
        },
        onToolCallStartEvent: ({event}) => {
            toolCallNamesById.set(event.toolCallId, event.toolCallName);
        },
    };
};

interface BuildStateToSendArgsI {
    context: ContextType | undefined;
    workspaceId: number | undefined;
}

export interface AiHubStateToSendI {
    activeFileId: string | null;
    currentTabs: Array<{fileId: string; name: string; viewMode: string}>;
    mode: string;
    parameters: Record<string, unknown>;
    source: string;
    workspaceId: string;
}

export const buildStateToSend = ({context, workspaceId}: BuildStateToSendArgsI): AiHubStateToSendI => {
    const tabsState = aiHubTabsStore.getState();

    const activeTab = tabsState.openTabs.find((tab) => tab.id === tabsState.activeTabId);

    return {
        activeFileId: activeTab?.fileId ?? null,
        currentTabs: tabsState.openTabs.map((tab) => ({
            fileId: tab.fileId,
            name: tab.name,
            viewMode: tab.viewMode,
        })),
        mode: context?.mode ?? 'ASK',
        parameters: context?.parameters ?? {},
        source: context?.source ?? 'AI_HUB',
        workspaceId: String(workspaceId ?? ''),
    };
};

export function AiHubRuntimeProvider({children}: Readonly<{children: ReactNode}>) {
    const [isRunning, setIsRunning] = useState(false);

    const {addMessage, appendToLastAssistantMessage, context, conversationId, messages} = useCopilotStore(
        useShallow((state) => ({
            addMessage: state.addMessage,
            appendToLastAssistantMessage: state.appendToLastAssistantMessage,
            context: state.context,
            conversationId: state.conversationId,
            messages: state.messages,
        }))
    );
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const agent = new HttpAgent({
        agentId: Source.AI_HUB,
        headers: {
            'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
        },
        threadId: conversationId!,
        url: `/api/platform/internal/ai/chat/${Source.AI_HUB.toLowerCase()}`,
    });

    const onNew = async (message: AppendMessage) => {
        if (message.content[0]?.type !== 'text') {
            throw new Error('Only text messages are supported');
        }

        const input = message.content[0].text;

        addMessage({content: input, role: 'user'});
        setIsRunning(true);

        agent.addMessage({
            content: input,
            id: getRandomId(),
            role: 'user',
        });

        agent.setState(buildStateToSend({context, workspaceId: currentWorkspaceId}));

        addMessage({content: '', role: 'assistant'});

        const subscriber = buildAiHubSubscriber({appendToLastAssistantMessage});

        await agent.runAgent({runId: getRandomId()}, subscriber);

        setIsRunning(false);
    };

    const runtime = useExternalStoreRuntime({
        convertMessage,
        isRunning,
        messages,
        onNew,
    });

    return <AssistantRuntimeProvider runtime={runtime}>{children}</AssistantRuntimeProvider>;
}
```

- [ ] **Step 4: Run the test to confirm it passes**

```bash
cd client && npm run test -- AiHubRuntimeProvider && cd ..
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add client/src/pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider.tsx \
        client/src/pages/automation/ai-hub/runtime-providers/tests/AiHubRuntimeProvider.test.tsx
git commit -m "CC1 client - Add AiHubRuntimeProvider

Intercepts openFileTab tool result events to update the tabs store, and
injects currentTabs + activeFileId into AG-UI state on each turn.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 11: Implement `AiHubFileViewer` (TDD)

**Files:**
- Create: `client/src/pages/automation/ai-hub/AiHubFileViewer.tsx`
- Create: `client/src/pages/automation/ai-hub/tests/AiHubFileViewer.test.tsx`

- [ ] **Step 1: Identify the existing asset-file content fetch hook**

Before writing the viewer, locate the existing query hook used by `AssetFiles.tsx` to load file content. Run:

```bash
cd client && grep -rn "asset-files.*content\|assetFileContent\|AssetFileContent" src/shared/middleware/ src/shared/queries/ src/pages/automation/asset-files/ 2>&1 | head -30 && cd ..
```

Use the resulting hook/query function in Step 3. If no content-fetch hook exists (only a file-list query), fall back to calling the REST URL `/api/automation/internal/asset-files/{fileId}/content` directly with `fetch` (same URL pattern `CreateAssetFileToolCallback` returns as `downloadUrl`).

- [ ] **Step 2: Write the failing test**

```tsx
/* eslint-disable sort-keys */
import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import AiHubFileViewer from '../AiHubFileViewer';

vi.mock('@/shared/components/MonacoEditorWrapper', () => ({
    default: ({value}: {value: string}) => <div data-testid="monaco-editor">{value}</div>,
}));

vi.mock('../hooks/useFileContent', () => ({
    default: () => ({content: '# Hello\n\nThis is **markdown**.', loading: false, mimeType: 'text/markdown'}),
}));

describe('AiHubFileViewer', () => {
    it('renders monaco editor in editor mode', () => {
        render(<AiHubFileViewer fileId="1" tabId="t1" name="spec.md" viewMode="editor" />);

        expect(screen.getByTestId('monaco-editor')).toHaveTextContent('# Hello');
    });

    it('renders markdown preview in preview mode for .md', () => {
        render(<AiHubFileViewer fileId="1" tabId="t1" name="spec.md" viewMode="preview" />);

        expect(screen.getByRole('heading', {level: 1, name: 'Hello'})).toBeInTheDocument();
        expect(screen.getByText('markdown')).toBeInTheDocument();
    });

    it('renders both panes in split mode', () => {
        render(<AiHubFileViewer fileId="1" tabId="t1" name="spec.md" viewMode="split" />);

        expect(screen.getByTestId('monaco-editor')).toBeInTheDocument();
        expect(screen.getByRole('heading', {level: 1, name: 'Hello'})).toBeInTheDocument();
    });
});

describe('AiHubFileViewer with unknown binary mime type', () => {
    it('renders metadata placeholder in preview mode', async () => {
        vi.doMock('../hooks/useFileContent', () => ({
            default: () => ({content: '', loading: false, mimeType: 'application/octet-stream'}),
        }));

        const {default: ViewerWithBinary} = await import('../AiHubFileViewer');

        render(<ViewerWithBinary fileId="2" tabId="t2" name="archive.zip" viewMode="preview" />);

        expect(screen.getByText(/Preview unavailable/i)).toBeInTheDocument();
        expect(screen.getByText('archive.zip')).toBeInTheDocument();
    });
});
```

- [ ] **Step 3: Run the test to confirm it fails**

```bash
cd client && npm run test -- AiHubFileViewer && cd ..
```
Expected: FAIL — module not found.

- [ ] **Step 4: Implement the hook**

Create `client/src/pages/automation/ai-hub/hooks/useFileContent.ts`:

```ts
import {useEffect, useState} from 'react';

interface FileContentResultI {
    content: string;
    loading: boolean;
    mimeType: string;
    error?: string;
}

export default function useFileContent(fileId: string): FileContentResultI {
    const [state, setState] = useState<FileContentResultI>({content: '', loading: true, mimeType: ''});

    useEffect(() => {
        let cancelled = false;

        setState({content: '', loading: true, mimeType: ''});

        fetch(`/api/automation/internal/asset-files/${fileId}/content`, {
            credentials: 'include',
        })
            .then(async (response) => {
                if (!response.ok) {
                    throw new Error(`Failed to load file (${response.status})`);
                }

                const mimeType = response.headers.get('Content-Type') ?? 'application/octet-stream';

                if (!mimeType.startsWith('text/') && mimeType !== 'application/json') {
                    if (!cancelled) {
                        setState({content: '', loading: false, mimeType});
                    }

                    return;
                }

                const content = await response.text();

                if (!cancelled) {
                    setState({content, loading: false, mimeType});
                }
            })
            .catch((error) => {
                if (!cancelled) {
                    setState({content: '', error: String(error), loading: false, mimeType: ''});
                }
            });

        return () => {
            cancelled = true;
        };
    }, [fileId]);

    return state;
}
```

- [ ] **Step 5: Implement the viewer**

Create `client/src/pages/automation/ai-hub/AiHubFileViewer.tsx`:

```tsx
import MonacoEditorWrapper from '@/shared/components/MonacoEditorWrapper';
import Markdown from 'react-markdown';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

import useFileContent from './hooks/useFileContent';

export type AiHubFileViewMode = 'editor' | 'preview' | 'split';

interface AiHubFileViewerPropsType {
    fileId: string;
    name: string;
    tabId: string;
    viewMode: AiHubFileViewMode;
}

const isTextualMimeType = (mimeType: string): boolean =>
    mimeType.startsWith('text/') || mimeType === 'application/json';

const EditorPane = ({content, mimeType, name}: {content: string; mimeType: string; name: string}) => {
    const language = useMemo(() => languageForMimeType(mimeType, name), [mimeType, name]);

    return (
        <div className="size-full">
            <MonacoEditorWrapper language={language} value={content} />
        </div>
    );
};

const PreviewPane = ({content, mimeType, name}: {content: string; mimeType: string; name: string}) => {
    if (!isTextualMimeType(mimeType)) {
        return (
            <div className="flex size-full flex-col items-center justify-center gap-2 p-8 text-center text-muted-foreground">
                <p className="text-sm font-medium">Preview unavailable for this file type.</p>

                <p className="text-xs">{name}</p>

                <p className="text-xs">{mimeType || 'unknown type'}</p>
            </div>
        );
    }

    if (mimeType === 'text/markdown') {
        return (
            <div className="prose prose-sm dark:prose-invert size-full overflow-auto p-4">
                <Markdown>{content}</Markdown>
            </div>
        );
    }

    if (mimeType === 'text/html') {
        return (
            <iframe
                className="size-full border-0"
                sandbox=""
                srcDoc={content}
                title={name}
            />
        );
    }

    return <pre className="size-full overflow-auto p-4 text-xs">{content}</pre>;
};

const languageForMimeType = (mimeType: string, name: string): string | undefined => {
    if (mimeType === 'application/json') return 'json';
    if (mimeType === 'text/markdown') return 'markdown';
    if (mimeType === 'text/html') return 'html';
    if (mimeType === 'text/css') return 'css';
    if (mimeType === 'text/javascript') return 'javascript';
    if (mimeType === 'text/x-python') return 'python';
    if (mimeType === 'text/x-java') return 'java';
    if (mimeType === 'text/yaml') return 'yaml';

    const dotIndex = name.lastIndexOf('.');

    if (dotIndex < 0) return undefined;

    const extension = name.slice(dotIndex + 1).toLowerCase();

    return (
        {
            css: 'css',
            html: 'html',
            java: 'java',
            js: 'javascript',
            json: 'json',
            md: 'markdown',
            py: 'python',
            sql: 'sql',
            ts: 'typescript',
            tsx: 'typescript',
            yaml: 'yaml',
            yml: 'yaml',
        } as Record<string, string>
    )[extension];
};

const AiHubFileViewer = ({fileId, name, viewMode}: AiHubFileViewerPropsType) => {
    const {content, error, loading, mimeType} = useFileContent(fileId);

    if (loading) {
        return <div className="flex size-full items-center justify-center text-sm text-muted-foreground">Loading…</div>;
    }

    if (error) {
        return (
            <div className="flex size-full items-center justify-center p-4 text-sm text-destructive">{error}</div>
        );
    }

    if (viewMode === 'editor') {
        return <EditorPane content={content} mimeType={mimeType} name={name} />;
    }

    if (viewMode === 'preview') {
        return <PreviewPane content={content} mimeType={mimeType} name={name} />;
    }

    return (
        <div className={twMerge('flex size-full')}>
            <div className="size-full flex-1 border-r">
                <EditorPane content={content} mimeType={mimeType} name={name} />
            </div>

            <div className="size-full flex-1">
                <PreviewPane content={content} mimeType={mimeType} name={name} />
            </div>
        </div>
    );
};

export default AiHubFileViewer;
```

- [ ] **Step 6: Run the test to confirm it passes**

```bash
cd client && npm run test -- AiHubFileViewer && cd ..
```
Expected: PASS (4 tests).

- [ ] **Step 7: Commit**

```bash
git add client/src/pages/automation/ai-hub/AiHubFileViewer.tsx \
        client/src/pages/automation/ai-hub/hooks/useFileContent.ts \
        client/src/pages/automation/ai-hub/tests/AiHubFileViewer.test.tsx
git commit -m "CC1 client - Add AiHubFileViewer with Editor/Preview/Split modes

Uses react-markdown for .md preview, sandboxed iframe for .html, Monaco for
editing, and a metadata placeholder for non-textual binary files.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 12: Implement `AiHubFilePicker`

**Files:**
- Create: `client/src/pages/automation/ai-hub/AiHubFilePicker.tsx`

- [ ] **Step 1: Implement the picker**

This is a compact popover listing workspace asset files using the existing `GetAssetFiles` GraphQL query (already present — referenced in `CopilotRuntimeProvider.tsx:5`).

```tsx
import {Button} from '@/components/ui/button';
import {Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList} from '@/components/ui/command';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import useAiHubTabsStore from '@/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {useGetAssetFilesQuery} from '@/shared/queries/automation/assetFiles.queries';
import {PlusIcon} from 'lucide-react';
import {useState} from 'react';

const AiHubFilePicker = () => {
    const [open, setOpen] = useState(false);

    const openFileTab = useAiHubTabsStore((state) => state.openFileTab);

    const {data: files = []} = useGetAssetFilesQuery();

    return (
        <Popover onOpenChange={setOpen} open={open}>
            <PopoverTrigger asChild>
                <Button aria-label="Open file" size="icon" variant="ghost">
                    <PlusIcon className="size-4" />
                </Button>
            </PopoverTrigger>

            <PopoverContent align="end" className="w-80 p-0">
                <Command>
                    <CommandInput placeholder="Search files…" />

                    <CommandList>
                        <CommandEmpty>No files found.</CommandEmpty>

                        <CommandGroup>
                            {files.map((file) => (
                                <CommandItem
                                    key={file.id}
                                    onSelect={() => {
                                        openFileTab(String(file.id), file.name);
                                        setOpen(false);
                                    }}
                                    value={file.name}
                                >
                                    {file.name}
                                </CommandItem>
                            ))}
                        </CommandGroup>
                    </CommandList>
                </Command>
            </PopoverContent>
        </Popover>
    );
};

export default AiHubFilePicker;
```

- [ ] **Step 2: Verify the import path for `useGetAssetFilesQuery`**

Run:

```bash
cd client && grep -rn "useGetAssetFilesQuery\|GetAssetFilesQuery" src/shared/queries/ src/shared/middleware/graphql.ts 2>&1 | head -10 && cd ..
```

If the hook name or import path differs, adjust the import accordingly. If no hook exists (only the raw query), generate one with the existing codegen conventions or use the lower-level `useQuery` + raw GraphQL operation as elsewhere in the codebase.

- [ ] **Step 3: Typecheck and commit**

```bash
cd client && npm run typecheck && cd ..
git add client/src/pages/automation/ai-hub/AiHubFilePicker.tsx
git commit -m "CC1 client - Add AiHubFilePicker popover

Lists workspace asset files and inserts the selected one into the tabs store
via openFileTab.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 13: Implement `AiHubResourcePanel`

**Files:**
- Create: `client/src/pages/automation/ai-hub/AiHubResourcePanel.tsx`

- [ ] **Step 1: Implement the panel**

```tsx
import {Button} from '@/components/ui/button';
import {ToggleGroup, ToggleGroupItem} from '@/components/ui/toggle-group';
import AiHubFilePicker from '@/pages/automation/ai-hub/AiHubFilePicker';
import AiHubFileViewer from '@/pages/automation/ai-hub/AiHubFileViewer';
import useAiHubTabsStore, {
    AiHubViewModeType,
} from '@/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {PanelRightCloseIcon, XIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

const AiHubResourcePanel = () => {
    const {activeTabId, closeTab, openTabs, setActiveTab, setRightPanelOpen, setViewMode} = useAiHubTabsStore(
        useShallow((state) => ({
            activeTabId: state.activeTabId,
            closeTab: state.closeTab,
            openTabs: state.openTabs,
            setActiveTab: state.setActiveTab,
            setRightPanelOpen: state.setRightPanelOpen,
            setViewMode: state.setViewMode,
        }))
    );

    const activeTab = openTabs.find((tab) => tab.id === activeTabId);

    return (
        <div className="flex size-full flex-col bg-surface-main">
            <div className="flex items-center gap-1 border-b px-2 py-1">
                <div className="flex flex-1 items-center gap-1 overflow-x-auto">
                    {openTabs.map((tab) => (
                        <div
                            key={tab.id}
                            className={twMerge(
                                'flex items-center gap-1 rounded-md border px-2 py-1 text-xs',
                                tab.id === activeTabId ? 'bg-accent' : 'bg-transparent hover:bg-accent/50'
                            )}
                        >
                            <button
                                className="max-w-40 truncate"
                                onClick={() => setActiveTab(tab.id)}
                                title={tab.name}
                                type="button"
                            >
                                {tab.name}
                            </button>

                            <button
                                aria-label={`Close ${tab.name}`}
                                className="text-muted-foreground hover:text-foreground"
                                onClick={() => closeTab(tab.id)}
                                type="button"
                            >
                                <XIcon className="size-3" />
                            </button>
                        </div>
                    ))}
                </div>

                <AiHubFilePicker />

                <Button
                    aria-label="Close resource panel"
                    onClick={() => setRightPanelOpen(false)}
                    size="icon"
                    variant="ghost"
                >
                    <PanelRightCloseIcon className="size-4" />
                </Button>
            </div>

            {activeTab ? (
                <>
                    <div className="flex items-center justify-end border-b px-2 py-1">
                        <ToggleGroup
                            onValueChange={(value) => {
                                if (value) {
                                    setViewMode(activeTab.id, value as AiHubViewModeType);
                                }
                            }}
                            size="sm"
                            type="single"
                            value={activeTab.viewMode}
                        >
                            <ToggleGroupItem value="editor">Editor</ToggleGroupItem>

                            <ToggleGroupItem value="preview">Preview</ToggleGroupItem>

                            <ToggleGroupItem value="split">Split</ToggleGroupItem>
                        </ToggleGroup>
                    </div>

                    <div className="min-h-0 flex-1">
                        <AiHubFileViewer
                            fileId={activeTab.fileId}
                            name={activeTab.name}
                            tabId={activeTab.id}
                            viewMode={activeTab.viewMode}
                        />
                    </div>
                </>
            ) : (
                <div className="flex flex-1 items-center justify-center text-sm text-muted-foreground">
                    No file open. Use the <PlusIcon className="mx-1 inline size-4" /> button or ask the assistant to create one.
                </div>
            )}
        </div>
    );
};

export default AiHubResourcePanel;
```

Add the `PlusIcon` import at the top:

```tsx
import {PanelRightCloseIcon, PlusIcon, XIcon} from 'lucide-react';
```

- [ ] **Step 2: Typecheck and commit**

```bash
cd client && npm run typecheck && cd ..
git add client/src/pages/automation/ai-hub/AiHubResourcePanel.tsx
git commit -m "CC1 client - Add AiHubResourcePanel

Tab strip with close buttons, view-mode toggle, + picker, and close-panel
button. Renders the active tab's AiHubFileViewer.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 14: Implement `AiHub` page

**Files:**
- Create: `client/src/pages/automation/ai-hub/AiHub.tsx`

- [ ] **Step 1: Implement the page**

```tsx
import {Button} from '@/components/ui/button';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import AiHubPanel from '@/pages/automation/ai-hub/AiHubPanel';
import AiHubResourcePanel from '@/pages/automation/ai-hub/AiHubResourcePanel';
import useAiHubTabsStore from '@/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {PanelRightOpenIcon} from 'lucide-react';
import {useEffect} from 'react';

const AiHub = () => {
    const setContext = useCopilotStore((state) => state.setContext);

    const {rightPanelOpen, setRightPanelOpen} = useAiHubTabsStore((state) => ({
        rightPanelOpen: state.rightPanelOpen,
        setRightPanelOpen: state.setRightPanelOpen,
    }));

    useEffect(() => {
        setContext({
            mode: MODE.ASK,
            parameters: {},
            source: Source.AI_HUB,
        });
    }, [setContext]);

    if (!rightPanelOpen) {
        return (
            <div className="relative size-full">
                <AiHubPanel />

                <Button
                    aria-label="Open resource panel"
                    className="absolute right-4 top-4"
                    onClick={() => setRightPanelOpen(true)}
                    size="icon"
                    variant="outline"
                >
                    <PanelRightOpenIcon className="size-4" />
                </Button>
            </div>
        );
    }

    return (
        <ResizablePanelGroup className="size-full" direction="horizontal">
            <ResizablePanel defaultSize={40} minSize={25}>
                <AiHubPanel />
            </ResizablePanel>

            <ResizableHandle withHandle />

            <ResizablePanel defaultSize={60} minSize={30}>
                <AiHubResourcePanel />
            </ResizablePanel>
        </ResizablePanelGroup>
    );
};

export default AiHub;
```

- [ ] **Step 2: Typecheck and commit**

```bash
cd client && npm run typecheck && cd ..
git add client/src/pages/automation/ai-hub/AiHub.tsx
git commit -m "CC1 client - Add AiHub route page

Two-pane layout: AiHubPanel (chat) on the left, AiHubResourcePanel
(file tabs) on the right. Right pane collapses when rightPanelOpen is false; a button
overlay lets the user reopen it.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 15: Register the route

**Files:**
- Modify: `client/src/routes.tsx`

- [ ] **Step 1: Add the lazy import**

Add (alphabetical with other automation page lazy imports, next to `AssetFiles` at line 63):

```tsx
const AiHub = lazy(() => import('@/pages/automation/ai-hub/AiHub'));
```

- [ ] **Step 2: Register the route**

Find the block that registers `/automation/asset-files` at line 754. Insert a sibling route for AI Hub:

```tsx
{
    element: (
        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
            <EEVersion>
                <LazyLoadWrapper hasLeftSidebar>
                    <AiHub />
                </LazyLoadWrapper>
            </EEVersion>
        </PrivateRoute>
    ),
    path: 'ai-hub',
},
```

Add it immediately before the `asset-files` route so the automation page order matches the nav order added in Task 16.

- [ ] **Step 3: Typecheck and commit**

```bash
cd client && npm run typecheck && cd ..
git add client/src/routes.tsx
git commit -m "CC1 client - Register /automation/ai-hub route (EE-only)

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 16: Add the sidebar nav entry

**Files:**
- Modify: `client/src/App.tsx`

- [ ] **Step 1: Locate and modify the nav array**

Inside `automationNavigation` (array literal starting at `client/src/App.tsx:51`), add the following entry immediately before the `Files` entry (`/automation/asset-files`, ~line 88):

```tsx
    {
        href: '/automation/ai-hub',
        icon: SparklesIcon,
        name: 'AI Hub',
    },
```

Add `SparklesIcon` to the existing `lucide-react` import at the top of `App.tsx` (preserve alphabetical order within the `{ ... }`).

- [ ] **Step 2: EE-gate the nav entry**

The `automationNavigation` array is rendered unconditionally. For an EE-only entry, filter it at render time. Search for `automationNavigation` occurrences in `App.tsx` to find the `map`/filter call. Wrap the mapping over `automationNavigation` so that entries with `href === '/automation/ai-hub'` are filtered out when `edition !== 'EE'`:

```tsx
const visibleAutomationNavigation = automationNavigation.filter((item) => {
    if (item.href === '/automation/ai-hub') {
        return edition === 'EE' && ai?.copilot?.enabled;
    }

    return true;
});
```

Use `visibleAutomationNavigation` in the subsequent render code in place of `automationNavigation`. (Inspect existing EE-gating patterns in this file first — `edition` comes from `useApplicationInfoStore` at line 141, and a similar `ai.copilot.enabled` check is used at line 337. Reuse the same references; do not introduce a new store hook.)

- [ ] **Step 3: Typecheck, lint, commit**

```bash
cd client && npm run check && cd ..
git add client/src/App.tsx
git commit -m "CC1 client - Add AI Hub entry to automation sidebar nav

EE-gated; hidden when edition != EE or bytechef.ai.copilot.enabled is false.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 17: Full server & client check, then manual verification

- [ ] **Step 1: Run server checks**

```bash
./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test \
          :server:ee:libs:ai:ai-copilot:ai-copilot-api:compileJava \
          :server:ee:libs:ai:ai-copilot:ai-copilot-rest:compileJava
./gradlew spotlessApply
```
Expected: all green.

- [ ] **Step 2: Run client checks**

```bash
cd client && npm run check && cd ..
```
Expected: lint + typecheck + tests all pass.

- [ ] **Step 3: Start infra and server**

```bash
cd server && docker compose -f docker-compose.dev.infra.yml up -d && cd ..
./gradlew -p server/apps/server-app bootRun
```
Ensure `bytechef.ai.copilot.enabled=true` and an appropriate `provider` (anthropic or openai) are configured in `application-dev.yml` or via env vars.

- [ ] **Step 4: Start client dev server**

In a second terminal:

```bash
cd client && npm run dev
```

- [ ] **Step 5: Manual verification checklist**

Log in as `admin@localhost.com / admin` and verify:

- [ ] **Nav entry appears** under Automation → "AI Hub" (only if EE build + copilot enabled).
- [ ] Clicking it navigates to `/automation/ai-hub` and the page renders without error.
- [ ] The right panel is **collapsed** by default; a panel-open button is visible in the top-right.
- [ ] Clicking the open button expands the resource panel empty-state.
- [ ] In the chat, type *"write a short technical spec about feature X as a markdown file"*.
- [ ] The assistant streams a response; after a few seconds, a tab with the new filename appears in the right panel with the file rendered in **Preview** mode.
- [ ] Toggle Editor → Split → Preview on the tab; view mode updates correctly.
- [ ] Click "+" picker → select an existing asset file → it opens as a new tab.
- [ ] Ask the assistant *"rewrite the introduction"* — it should reference the active file (verify the AG-UI `state` payload in browser devtools network tab includes `activeFileId` and `currentTabs`).
- [ ] Close a tab with the X button; an adjacent tab becomes active; closing the last tab returns to the empty-state.
- [ ] Click the panel-close button; only the chat remains, filling full width.

- [ ] **Step 6: EE-gate smoke check**

Stop the server, rebuild without EE, and confirm the `/automation/ai-hub` route returns the `PageNotFound` element and the nav entry is hidden. Resume EE build afterward.

- [ ] **Step 7: Final commit (no-op if nothing changed)**

If any formatting fixes were applied during the full-check run:

```bash
git add -A
git commit -m "CC1 Apply final formatting and lint fixes

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Out of scope (do not implement in this plan)

- Non-file resource tabs (workflows, data tables, knowledge bases)
- `@`-mentions or drag-drop of workspace resources
- Research agent, scheduled prompt-jobs, generative images/slides, KB connectors
- Cross-domain agent tools (workflow CRUD, table query, KB query)
- Server-side wiring of BUILD mode for the ai-hub agent (UI toggle is a no-op)

These are Phase 2+ sub-projects per the spec.
