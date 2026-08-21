# AI Hub → Copilot Sub-Agent Delegation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stop hanging raw `ReadSkillsTools` / `SkillsTools` / etc. on the AiHub LLM agent; delegate to the Copilot specialists (skills, cluster element, code editor, workflow editor, converter) via sub-agent `ToolCallback`s — matching the existing pattern for `research` / `workflow_builder` / `data_analyst`.

**Architecture:** For each Copilot specialist `*SpringAIAgent`, add a sibling `*SubAgentChatClient` bean in `CopilotConfiguration` (same system prompt + same tools, no `ChatMemory`). Co-locate a hand-rolled `*AgentToolCallback` class with `ResearchToolCallback` in `platform-ai-hub-service`; each callback exposes a single `request: string` input, delegates to the ChatClient, and returns the synthesised content. `AiHubConfiguration` injects the new ChatClients by Spring qualifier (no compile-time module dependency between `automation-ai-hub-service` and `ai-copilot-service`), wraps them in `ProgressReportingToolCallback`, and removes the direct `ReadSkillsTools` / `SkillsTools` constructor parameters + registrations.

**Tech Stack:** Java 25, Spring Boot 4, Spring AI (`ChatClient`, `ToolCallback`), JUnit 5, Mockito, Spring AG-UI integration.

**Spec:** `docs/superpowers/specs/2026-05-25-aihub-delegate-to-copilot-subagents-design.md`

**Open question resolutions adopted in this plan:**
- RAG advisor on workflow-editor ASK sub-agent: **keep** the `QuestionAnswerAdvisor`.
- AiHub system prompts: minimal "delegate skills/cluster-element/code/workflow-editor/converter work to the matching sub-agent" tweak in this PR.
- Metering of sub-agent tools: **defer** — out of scope.
- Sub-agent error envelope: match `ResearchToolCallback`'s `ToolErrors` pattern.

---

## File Structure

**Create** (in `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/tool/`):
- `SkillsAgentToolCallback.java` — wraps a skills ChatClient
- `ClusterElementAgentToolCallback.java`
- `CodeEditorAgentToolCallback.java`
- `WorkflowEditorAgentToolCallback.java`
- `ConverterAgentToolCallback.java`

**Create** (in `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/test/java/com/bytechef/ee/platform/aihub/tool/`):
- `SkillsAgentToolCallbackTest.java`
- `ClusterElementAgentToolCallbackTest.java`
- `CodeEditorAgentToolCallbackTest.java`
- `WorkflowEditorAgentToolCallbackTest.java`
- `ConverterAgentToolCallbackTest.java`

**Modify:**
- `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/config/CopilotConfiguration.java` — add 9 new `*SubAgentChatClient` beans (one per specialist agent's ASK/BUILD variant; converter has BUILD only).
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/config/AiHubConfiguration.java` — remove `ReadSkillsTools` (ASK) and `SkillsTools` (BUILD) constructor params + their direct registrations (lines 12-13, 301, 445); add a new `registerCopilotSubAgentToolCallbacks` helper that registers the five new wrappers; call it from both ASK and BUILD bean methods.
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/prompt_ai_hub_ask.txt` — short paragraph listing the new sub-agent tools.
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/prompt_ai_hub_build.txt` — same.

---

## Task 1: SkillsAgentToolCallback — failing test

**Files:**
- Test: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/test/java/com/bytechef/ee/platform/aihub/tool/SkillsAgentToolCallbackTest.java`

- [ ] **Step 1: Write the failing test**

Mirror the structure of `ResearchToolCallbackTest.java` (existing file in the same directory — read it first for reference). The test pins three behaviours: (a) the callback delegates to the injected ChatClient with the parsed `request` string, (b) it returns the ChatClient's `content()` verbatim, (c) input without a `request` field returns a `ToolErrors` envelope rather than throwing.

```java
package com.bytechef.ee.platform.aihub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;

class SkillsAgentToolCallbackTest {

    @Test
    void testCallDelegatesRequestStringToChatClient() {
        ChatClient chatClient = mock(ChatClient.class, org.mockito.Answers.RETURNS_DEEP_STUBS);

        when(chatClient.prompt().user(anyString()).call().content())
            .thenReturn("synthesised skill plan");

        SkillsAgentToolCallback callback = new SkillsAgentToolCallback(chatClient);

        String result = callback.call("{\"request\":\"list my skills\"}");

        assertThat(result).isEqualTo("synthesised skill plan");
        verify(chatClient.prompt().user("list my skills"));
    }

    @Test
    void testCallReturnsErrorEnvelopeForMissingRequestField() {
        ChatClient chatClient = mock(ChatClient.class);

        SkillsAgentToolCallback callback = new SkillsAgentToolCallback(chatClient);

        String result = callback.call("{}");

        assertThat(result).contains("request");
        assertThat(result).contains("error");
    }

    @Test
    void testToolDefinitionExposesRequestSchema() {
        SkillsAgentToolCallback callback = new SkillsAgentToolCallback(mock(ChatClient.class));

        assertThat(callback.getToolDefinition().name()).isEqualTo("skills_agent");
        assertThat(callback.getToolDefinition().inputSchema()).contains("\"request\"");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:test --tests 'com.bytechef.ee.platform.aihub.tool.SkillsAgentToolCallbackTest'`
Expected: FAIL with `class SkillsAgentToolCallback not found` or `cannot find symbol`.

---

## Task 2: SkillsAgentToolCallback — implementation

**Files:**
- Create: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/tool/SkillsAgentToolCallback.java`

- [ ] **Step 1: Read the existing template**

Open `ResearchToolCallback.java` in the same `tool/` directory. Note its structure: `DESCRIPTION` constant (free-text), `INPUT_SCHEMA` constant (JSON schema), `getToolDefinition()`, `call(String toolInput)`, `call(String toolInput, ToolContext toolContext)`. Mirror it.

- [ ] **Step 2: Implement the class**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.aihub.tool;

import com.bytechef.ee.platform.aihub.util.ToolErrors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Hand-rolled Spring AI {@link ToolCallback} that exposes the Skills Copilot sub-agent to the parent AiHub LLM.
 *
 * <p>
 * The parent LLM invokes this tool with a {@code request} string when the user wants to list, explain, create,
 * or update reusable workflow Skills. The wrapped {@link ChatClient} is pre-configured with the Skills system
 * prompt and the {@code ReadSkillsTools} / {@code SkillsTools} catalog — the parent never sees the catalog,
 * only the synthesised result.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class SkillsAgentToolCallback implements ToolCallback {

    private static final Logger logger = LoggerFactory.getLogger(SkillsAgentToolCallback.class);

    private static final String NAME = "skills_agent";

    private static final String DESCRIPTION =
        """
            Delegate any user request that lists, explains, creates, updates, or composes
            reusable Skills — parameterised workflow templates the user can pull into projects.
            The Skills sub-agent owns the canonical behaviour for this domain; prefer calling it
            over reasoning about skills directly. Returns a synthesised report (or, in build mode,
            a summary of mutations performed).""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "request": {
                        "type": "string",
                        "description": "The user request in natural language. Pass through verbatim — the sub-agent does its own task decomposition."
                    }
                },
                "required": ["request"]
            }""";

    private final ChatClient chatClient;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SkillsAgentToolCallback(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(NAME)
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        String request;

        try {
            Map<String, Object> parsed = jsonMapper.readValue(toolInput, Map.class);

            Object value = parsed.get("request");

            if (!(value instanceof String stringValue) || stringValue.isBlank()) {
                return ToolErrors.error("Missing required field 'request' (non-blank string).");
            }

            request = stringValue;
        } catch (JacksonException exception) {
            return ToolErrors.error("Invalid JSON input: " + exception.getOriginalMessage());
        }

        try {
            return chatClient.prompt()
                .user(request)
                .call()
                .content();
        } catch (RuntimeException exception) {
            logger.warn("Skills sub-agent invocation failed", exception);

            return ToolErrors.error("Skills sub-agent failed: " + exception.getMessage());
        }
    }
}
```

- [ ] **Step 3: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:test --tests 'com.bytechef.ee.platform.aihub.tool.SkillsAgentToolCallbackTest'`
Expected: 3 tests PASS.

- [ ] **Step 4: Run spotless on the new file**

Run: `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:spotlessApply`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/tool/SkillsAgentToolCallback.java \
        server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/test/java/com/bytechef/ee/platform/aihub/tool/SkillsAgentToolCallbackTest.java
git commit -m "$(cat <<'EOF'
520 Add SkillsAgentToolCallback sub-agent wrapper

Hand-rolled ToolCallback that delegates a single-field {request:string}
input to a pre-configured Skills ChatClient. Mirrors ResearchToolCallback
shape — error envelope via ToolErrors, JsonMapper-parsed input, structured
exception handling. Not yet wired into AiHub; standalone class so the
Copilot bean wiring lands in a follow-up commit.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Skills ChatClient beans in CopilotConfiguration

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/config/CopilotConfiguration.java` (add two beans)

- [ ] **Step 1: Add `skillsAskSubAgentChatClient` bean**

Insert after `skillsAskSpringAIAgent` (around line 268). The bean carries the same system prompt and tools as `skillsAskSpringAIAgent`, no chat memory.

```java
    @Bean
    ChatClient skillsAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ReadSkillsTools readSkillsTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(getSystemPrompt(promptSkillsAskResource))
            .defaultTools(readSkillsTools, readProjectTools, readProjectWorkflowTools)
            .build();
    }
```

- [ ] **Step 2: Add `skillsBuildSubAgentChatClient` bean**

Insert after `skillsBuildSpringAIAgent` (around line 286).

```java
    @Bean
    ChatClient skillsBuildSubAgentChatClient(
        ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, SkillsTools skillsTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(getSystemPrompt(promptSkillsBuildResource))
            .defaultTools(skillsTools, readProjectTools, readProjectWorkflowTools)
            .build();
    }
```

- [ ] **Step 3: Compile the module**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run any existing tests in the module to confirm no regressions**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test`
Expected: tests PASS (or the module has no tests — verify with a clean `BUILD SUCCESSFUL`).

- [ ] **Step 5: Spotless and commit**

```bash
./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:spotlessApply
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/config/CopilotConfiguration.java
git commit -m "$(cat <<'EOF'
520 Add Skills sub-agent ChatClient beans

Two ChatClient beans (ASK + BUILD) carrying the existing Skills system
prompts and tool lists, no ChatMemory. Will be consumed by the AiHub
SkillsAgentToolCallback wrapper so AiHub can delegate skills work
without registering the underlying SkillsTools / ReadSkillsTools beans
directly on its own LLM agent.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: Wire SkillsAgentToolCallback into AiHubConfiguration

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/config/AiHubConfiguration.java`

- [ ] **Step 1: Remove `ReadSkillsTools` constructor parameter from `aiHubAskSpringAIAgent`**

Find line ~227 (the `ReadSkillsTools readSkillsTools,` parameter in the `aiHubAskSpringAIAgent` constructor) and delete that line. Also remove the import at line 12 (`import com.bytechef.ai.mcp.tool.automation.ReadSkillsTools;`).

- [ ] **Step 2: Remove the direct skills registration from `aiHubAskSpringAIAgent`**

Find lines 300-301 and delete:
```java
        // AI skill catalog — read-only on the ASK agent. Mutations (create/update) live on the BUILD agent.
        Collections.addAll(toolCallbacks, ToolCallbacks.from(readSkillsTools));
```

- [ ] **Step 3: Add `skillsAskSubAgentChatClient` ObjectProvider parameter and registration**

In the same `aiHubAskSpringAIAgent` constructor, add the parameter (group with the other `ObjectProvider<ChatClient>` params if present, otherwise add at the end of the ChatClient block):

```java
        @Qualifier("skillsAskSubAgentChatClient") ObjectProvider<ChatClient> skillsAskSubAgentChatClientProvider,
```

Then replace the deleted registration with:

```java
        // Skills delegation — read-only sub-agent. The Skills Copilot specialist owns the canonical behaviour;
        // AiHub delegates rather than wiring the raw skill tools onto its own agent.
        skillsAskSubAgentChatClientProvider.ifAvailable(skillsAskSubAgentChatClient -> toolCallbacks.add(
            new ProgressReportingToolCallback(
                new SkillsAgentToolCallback(skillsAskSubAgentChatClient), "skills_agent")));
```

- [ ] **Step 4: Add `SkillsAgentToolCallback` import**

Near the existing imports for tool callbacks (line ~102):
```java
import com.bytechef.ee.platform.aihub.tool.SkillsAgentToolCallback;
```

- [ ] **Step 5: Repeat steps 1-4 for `aiHubBuildSpringAIAgent` with `SkillsTools` / `skillsBuildSubAgentChatClient`**

- Remove the `SkillsTools skillsTools,` parameter from the `aiHubBuildSpringAIAgent` constructor (around line 364).
- Remove the import at line 13 (`import com.bytechef.ai.mcp.tool.automation.SkillsTools;`).
- Remove the registration at line 445 (`Collections.addAll(toolCallbacks, ToolCallbacks.from(skillsTools));`).
- Add the `skillsBuildSubAgentChatClientProvider` parameter.
- Add the BUILD-variant registration mirroring step 3.

- [ ] **Step 6: Compile the module**

Run: `./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Run AiHubConfiguration tests if any exist**

Run: `./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests 'com.bytechef.ee.automation.aihub.config.*'`
Expected: tests PASS.

If any test asserted constructor parameter shape (e.g. mocked `readSkillsTools` as a parameter to `aiHubAskSpringAIAgent`), update it to drop that parameter and add the new ObjectProvider mock.

- [ ] **Step 8: Spotless and commit**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:spotlessApply
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/config/AiHubConfiguration.java
git commit -m "$(cat <<'EOF'
520 Route AiHub skills work to Skills Copilot sub-agent

Removes the direct ReadSkillsTools / SkillsTools registrations on the
AiHub ASK / BUILD agents. AiHub now delegates skills work via the
SkillsAgentToolCallback wrapper around the new skillsAsk/Build
SubAgentChatClient beans — Copilot's Skills agent is the single source
of truth for skill behaviour.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: ClusterElementAgentToolCallback (full pattern, second specialist)

Same five-step structure as Tasks 1-4 collapsed. The class file mirrors `SkillsAgentToolCallback` exactly except for:

- `NAME = "cluster_element_agent"`
- DESCRIPTION (see below)
- The test class is `ClusterElementAgentToolCallbackTest` with the three-case structure of Task 1 but `cluster_element_agent` as the expected tool name.

**Files:**
- Create: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/tool/ClusterElementAgentToolCallback.java`
- Create: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/test/java/com/bytechef/ee/platform/aihub/tool/ClusterElementAgentToolCallbackTest.java`
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/config/CopilotConfiguration.java` (add `clusterElementAskSubAgentChatClient` + `clusterElementBuildSubAgentChatClient` beans)
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/config/AiHubConfiguration.java` (register the new wrapper on both ASK and BUILD agents)

- [ ] **Step 1: Write the failing test for `ClusterElementAgentToolCallback`**

Copy `SkillsAgentToolCallbackTest`, change the class name to `ClusterElementAgentToolCallbackTest`, change the expected tool name to `cluster_element_agent`. The behaviour assertions are identical.

- [ ] **Step 2: Run the test — expect compile failure (class missing)**

Run: `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:test --tests 'com.bytechef.ee.platform.aihub.tool.ClusterElementAgentToolCallbackTest'`
Expected: FAIL.

- [ ] **Step 3: Implement `ClusterElementAgentToolCallback`**

Copy `SkillsAgentToolCallback`, change `NAME` to `cluster_element_agent`, replace `DESCRIPTION` with:

```
Delegate any user request that designs, edits, or explains cluster elements —
the slotted child operations inside cluster-root components (AI Agent, Knowledge
Base, etc.). The Cluster Element Copilot specialist owns the canonical behaviour
for this domain; prefer calling it over reasoning about cluster element
configuration directly. Returns a synthesised plan or, in build mode, a summary
of mutations performed.
```

Update the class Javadoc to refer to "Cluster Element Copilot sub-agent" and the underlying tool catalog.

- [ ] **Step 4: Run the test — expect PASS**

- [ ] **Step 5: Add the two ChatClient beans in CopilotConfiguration**

```java
    @Bean
    ChatClient clusterElementAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(getSystemPrompt(promptClusterElementAskResource))
            .defaultTools(readProjectWorkflowTools, componentTools, taskTools)
            .build();
    }

    @Bean
    ChatClient clusterElementBuildSubAgentChatClient(
        ChatModel chatModel, ClusterElementTools clusterElementTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools, TaskTools taskTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(getSystemPrompt(promptClusterElementBuildResource))
            .defaultTools(readProjectWorkflowTools, clusterElementTools, componentTools, taskTools)
            .build();
    }
```

- [ ] **Step 6: Wire into AiHubConfiguration**

Add `clusterElementAskSubAgentChatClientProvider` + `clusterElementBuildSubAgentChatClientProvider` ObjectProvider parameters to the respective bean methods. Register via the helper introduced in Task 9 (or inline if you reach this task before Task 9).

For the ASK agent:
```java
clusterElementAskSubAgentChatClientProvider.ifAvailable(chatClient -> toolCallbacks.add(
    new ProgressReportingToolCallback(
        new ClusterElementAgentToolCallback(chatClient), "cluster_element_agent")));
```

Same shape for BUILD with `clusterElementBuildSubAgentChatClient`.

- [ ] **Step 7: Compile, run tests, spotless, commit**

```bash
./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:compileJava \
          :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:test \
          :server:ee:libs:ai:ai-copilot:ai-copilot-service:compileJava \
          :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:compileJava \
          spotlessApply
```

Expected: all green.

```bash
git add <all four modified files + the two new files>
git commit -m "520 Route AiHub cluster element work to Cluster Element Copilot sub-agent"
```

---

## Task 6: CodeEditorAgentToolCallback

Same five-step structure as Task 5. Class differences:

- `NAME = "code_editor_agent"`
- DESCRIPTION (see below)
- ChatClient beans inject `firecrawlTools` (optional) on ASK only — handle with `Optional<FirecrawlTools>` mirroring the existing `codeEditorAskSpringAIAgent`. BUILD uses `scriptTools`.

**Files:** same shape as Task 5 (one new callback + one new test + two new ChatClient beans + AiHubConfiguration wiring).

DESCRIPTION:
```
Delegate any user request that writes, edits, debugs, or explains script code
(JavaScript, Python, Ruby) embedded inside a workflow task. The Code Editor
Copilot specialist owns the canonical behaviour for this domain; prefer
calling it over generating script code directly. Returns the updated script
plus an explanation of changes.
```

Tasks 6.1-6.7 follow the same shape as 5.1-5.7.

ChatClient bean signatures (insert into `CopilotConfiguration` near the existing CodeEditor agents at line ~95):

```java
    @Bean
    ChatClient codeEditorAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, Optional<FirecrawlTools> firecrawlTools) {

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem(getSystemPrompt(promptCodeEditorAskResource));

        if (firecrawlTools.isPresent()) {
            builder.defaultTools(readProjectWorkflowTools, componentTools, firecrawlTools.get());
        } else {
            builder.defaultTools(readProjectWorkflowTools, componentTools);
        }

        return builder.build();
    }

    @Bean
    ChatClient codeEditorBuildSubAgentChatClient(
        ChatModel chatModel, ScriptTools scriptTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(getSystemPrompt(promptCodeEditorBuildResource))
            .defaultTools(readProjectWorkflowTools, scriptTools, componentTools)
            .build();
    }
```

Commit message: `520 Route AiHub code-editor work to Code Editor Copilot sub-agent`.

---

## Task 7: WorkflowEditorAgentToolCallback

Same shape. Class differences:

- `NAME = "workflow_editor_agent"`
- DESCRIPTION (see below)
- ASK ChatClient bean keeps the `QuestionAnswerAdvisor` RAG advisor (decided in spec open questions). BUILD uses different tools.

DESCRIPTION:
```
Delegate any user request that designs, edits, debugs, or explains an entire
workflow (orchestration of tasks, triggers, conditions, loops). The Workflow
Editor Copilot specialist owns the canonical behaviour for this domain;
prefer calling it over reasoning about workflow shape directly. ASK mode
returns analysis / explanation; BUILD mode returns the updated workflow JSON
plus a change rationale.
```

ChatClient bean signatures (insert near the existing WorkflowEditor agents at line ~182):

```java
    @Bean
    ChatClient workflowEditorAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools, TaskTools taskTools,
        Optional<FirecrawlTools> firecrawlTools, QuestionAnswerAdvisor questionAnswerAdvisor) {

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem(getSystemPrompt(promptWorkflowEditorAskResource))
            .defaultAdvisors(questionAnswerAdvisor);

        if (firecrawlTools.isPresent()) {
            builder.defaultTools(
                readProjectTools, readProjectWorkflowTools, componentTools, taskTools, firecrawlTools.get());
        } else {
            builder.defaultTools(readProjectTools, readProjectWorkflowTools, componentTools, taskTools);
        }

        return builder.build();
    }

    @Bean
    ChatClient workflowEditorBuildSubAgentChatClient(
        ChatModel chatModel, ProjectTools projectTools,
        ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools, ScriptTools scriptTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(getSystemPrompt(promptWorkflowEditorBuildResource))
            .defaultTools(projectTools, projectWorkflowTools, taskTools, scriptTools)
            .build();
    }
```

Commit message: `520 Route AiHub workflow-editor work to Workflow Editor Copilot sub-agent`.

---

## Task 8: ConverterAgentToolCallback (BUILD only)

Same shape as Tasks 5-7 but only one ChatClient bean and one registration (BUILD agent only — there is no `converterAskSpringAIAgent`).

- `NAME = "converter_agent"`
- DESCRIPTION:
```
Delegate any user request that converts an external workflow definition
(n8n, Make, Zapier, Workato, etc.) into a ByteChef workflow. The Converter
Copilot specialist owns the canonical behaviour for this domain. Returns
the resulting ByteChef workflow JSON plus a change rationale.
```

ChatClient bean (insert near `converterBuildSpringAIAgent` at line ~235):

```java
    @Bean
    ChatClient converterBuildSubAgentChatClient(
        ChatModel chatModel, ProjectTools projectTools,
        ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools, ScriptTools scriptTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(getSystemPrompt(promptConverterBuildResource))
            .defaultTools(projectTools, projectWorkflowTools, taskTools, scriptTools)
            .build();
    }
```

Wire into AiHubConfiguration's BUILD agent only:

```java
converterBuildSubAgentChatClientProvider.ifAvailable(chatClient -> toolCallbacks.add(
    new ProgressReportingToolCallback(
        new ConverterAgentToolCallback(chatClient), "converter_agent")));
```

Commit message: `520 Route AiHub converter work to Converter Copilot sub-agent`.

---

## Task 9: Extract registerCopilotSubAgentToolCallbacks helper

After Tasks 4-8 land, the ASK and BUILD AiHub bean methods each contain five (ASK) / four+converter (BUILD) `.ifAvailable(...)` registration blocks. Extract them into a helper to keep the bean methods under Checkstyle's per-method line limit and mirror the existing `registerSubAgentToolCallbacks` helper at lines 616-652.

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/config/AiHubConfiguration.java`

- [ ] **Step 1: Add the helper**

```java
    /**
     * Registers the Copilot specialist sub-agent ToolCallbacks (skills, cluster element, code editor, workflow
     * editor, converter) on the supplied tool list. Each is only added when its backing ChatClient bean is
     * present — deployments without Copilot enabled (or specific specialists missing) skip the registration
     * silently. Co-located here so the ASK and BUILD bean methods stay within Checkstyle's per-method line
     * limit; mirrors {@link #registerSubAgentToolCallbacks} for the existing ChatClient sub-agents.
     */
    private static void registerCopilotSubAgentToolCallbacks(
        List<ToolCallback> toolCallbacks,
        ObjectProvider<ChatClient> skillsSubAgentChatClientProvider,
        ObjectProvider<ChatClient> clusterElementSubAgentChatClientProvider,
        ObjectProvider<ChatClient> codeEditorSubAgentChatClientProvider,
        ObjectProvider<ChatClient> workflowEditorSubAgentChatClientProvider,
        ObjectProvider<ChatClient> converterSubAgentChatClientProvider) {

        skillsSubAgentChatClientProvider.ifAvailable(chatClient -> toolCallbacks.add(
            new ProgressReportingToolCallback(new SkillsAgentToolCallback(chatClient), "skills_agent")));

        clusterElementSubAgentChatClientProvider.ifAvailable(chatClient -> toolCallbacks.add(
            new ProgressReportingToolCallback(
                new ClusterElementAgentToolCallback(chatClient), "cluster_element_agent")));

        codeEditorSubAgentChatClientProvider.ifAvailable(chatClient -> toolCallbacks.add(
            new ProgressReportingToolCallback(
                new CodeEditorAgentToolCallback(chatClient), "code_editor_agent")));

        workflowEditorSubAgentChatClientProvider.ifAvailable(chatClient -> toolCallbacks.add(
            new ProgressReportingToolCallback(
                new WorkflowEditorAgentToolCallback(chatClient), "workflow_editor_agent")));

        if (converterSubAgentChatClientProvider != null) {
            converterSubAgentChatClientProvider.ifAvailable(chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new ConverterAgentToolCallback(chatClient), "converter_agent")));
        }
    }
```

- [ ] **Step 2: Replace inline registrations in `aiHubAskSpringAIAgent` with one helper call**

Pass `null` for the converter provider (ASK has no converter):

```java
        registerCopilotSubAgentToolCallbacks(
            toolCallbacks, skillsAskSubAgentChatClientProvider,
            clusterElementAskSubAgentChatClientProvider, codeEditorAskSubAgentChatClientProvider,
            workflowEditorAskSubAgentChatClientProvider, null);
```

- [ ] **Step 3: Replace inline registrations in `aiHubBuildSpringAIAgent`**

```java
        registerCopilotSubAgentToolCallbacks(
            toolCallbacks, skillsBuildSubAgentChatClientProvider,
            clusterElementBuildSubAgentChatClientProvider, codeEditorBuildSubAgentChatClientProvider,
            workflowEditorBuildSubAgentChatClientProvider, converterBuildSubAgentChatClientProvider);
```

- [ ] **Step 4: Compile + tests + spotless**

Run: `./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:check spotlessApply`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/config/AiHubConfiguration.java
git commit -m "520 Group Copilot sub-agent registrations in helper"
```

---

## Task 10: Update AiHub system prompts to teach sub-agent invocation

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/prompt_ai_hub_ask.txt`
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/prompt_ai_hub_build.txt`

- [ ] **Step 1: Find the existing tool-section in `prompt_ai_hub_ask.txt`**

Open the file. Locate the section that today lists / explains skills tools and component-discovery tools (if any). Identify where to insert sub-agent guidance — typically near the top of the tools section.

- [ ] **Step 2: Insert sub-agent guidance**

Add a short paragraph like:

```
## Specialist sub-agents

For domain-specific work, prefer delegating to the matching specialist sub-agent
rather than reasoning about primitives yourself. Each sub-agent has its own
catalog of tools and a tuned system prompt; pass through the user's request
verbatim and return the sub-agent's synthesised response to the user.

- `skills_agent` — listing, explaining, creating, updating, or composing Skills
  (reusable parameterised workflow templates).
- `cluster_element_agent` — designing, editing, or explaining cluster elements
  (the slotted child operations inside cluster-root components like AI Agent or
  Knowledge Base).
- `code_editor_agent` — writing, editing, debugging, or explaining script code
  (JavaScript / Python / Ruby) embedded inside a workflow task.
- `workflow_editor_agent` — designing, editing, debugging, or explaining whole
  workflows (orchestration of tasks, triggers, conditions, loops).
```

- [ ] **Step 3: Repeat for `prompt_ai_hub_build.txt`**

Same paragraph, plus an additional bullet:

```
- `converter_agent` — converting an external workflow definition (n8n, Make,
  Zapier, Workato, etc.) into a ByteChef workflow.
```

- [ ] **Step 4: Remove any now-stale references to `readSkillsTools` / `skillsTools` from both prompts**

Search both files for "skill" and remove or rewrite any line that names the old raw tools by name. The new prompt should not direct the LLM to use `readSkillsTools` directly.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/prompt_ai_hub_ask.txt \
        server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/prompt_ai_hub_build.txt
git commit -m "520 Teach AiHub prompts to delegate domain work to specialist sub-agents"
```

---

## Task 11: Remove workflow-CRUD callbacks superseded by workflow-editor sub-agent

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/config/AiHubConfiguration.java`

- [ ] **Step 1: Remove the four workflow callbacks**

In `aiHubAskSpringAIAgent`, delete:
```java
toolCallbacks.add(new ListWorkflowsToolCallback(projectFacade));
toolCallbacks.add(new GetWorkflowToolCallback(workflowService));
```

In `aiHubBuildSpringAIAgent`, delete the same two lines plus:
```java
toolCallbacks.add(new CreateWorkflowToolCallback(projectWorkflowFacade, projectService, taskArtifactService));
toolCallbacks.add(new UpdateWorkflowToolCallback(projectWorkflowFacade, projectService, taskArtifactService));
```

- [ ] **Step 2: Audit constructor params for unused services**

After the deletions, check each removed dependency:
- `projectFacade` (ASK) — still used by no-one in ASK after this removal. Verify with a `Find Usages` in the IDE on the parameter; if no usages remain in the ASK bean method body, delete the parameter.
- `workflowService` (ASK) — verify; likely still used by other callbacks (none in ASK after `GetWorkflowToolCallback` removal). If unused, drop.
- `workflowService` (BUILD) — still used by `ListChatWorkflowsToolCallback` and `RunChatWorkflowToolCallback`. Keep.
- `projectWorkflowFacade` (BUILD) — verify; was used only by Create/UpdateWorkflowToolCallback. If unused now, drop.
- `projectService` (BUILD) — verify; was used by Create/UpdateWorkflowToolCallback. If unused, drop.
- `taskArtifactService` — still used by many other callbacks; keep.

For each parameter you drop, also remove its import if it becomes unused.

- [ ] **Step 3: Compile + tests + spotless**

Run: `./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:check spotlessApply`
Expected: BUILD SUCCESSFUL.

If any test asserted these callbacks were registered, update the test to assert they are NOT registered (or delete the assertion if the test was a placeholder).

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/config/AiHubConfiguration.java
git commit -m "$(cat <<'EOF'
520 Remove AiHub workflow callbacks superseded by Workflow Editor sub-agent

Drops ListWorkflowsToolCallback and GetWorkflowToolCallback from both
agents, plus CreateWorkflowToolCallback and UpdateWorkflowToolCallback
from the BUILD agent. All four are covered by the workflow-editor
Copilot sub-agent's tool catalog (ReadProjectWorkflowTools /
ProjectWorkflowTools). Constructor params for facades that lose their
sole consumer are also dropped.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 12: Remove project-CRUD callbacks superseded by workflow-editor sub-agent

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/config/AiHubConfiguration.java`

- [ ] **Step 1: Remove the four project callbacks (BUILD only)**

In `aiHubBuildSpringAIAgent`, delete:
```java
toolCallbacks.add(new ListProjectsToolCallback(projectFacade));
toolCallbacks.add(new CreateProjectToolCallback(projectFacade));
toolCallbacks.add(new UpdateProjectToolCallback(projectFacade));
toolCallbacks.add(new DeleteProjectToolCallback(projectFacade));
```

- [ ] **Step 2: Audit `projectFacade` param**

After this removal, `projectFacade` may be the last reference; if not used elsewhere in the BUILD bean method, drop the parameter and its import.

- [ ] **Step 3: Compile + tests + spotless + commit**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:check spotlessApply
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/config/AiHubConfiguration.java
git commit -m "520 Remove AiHub project CRUD callbacks superseded by Workflow Editor sub-agent"
```

---

## Task 13: Remove component-discovery callbacks superseded by every editor sub-agent

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/config/AiHubConfiguration.java`

- [ ] **Step 1: Remove the two `registerComponentDiscoveryToolCallbacks(...)` invocations**

In `aiHubAskSpringAIAgent` (lines 275-276 of the original file, search for `registerComponentDiscoveryToolCallbacks`), delete the call. Do the same in `aiHubBuildSpringAIAgent` (around line 473).

- [ ] **Step 2: Delete the helper method itself**

Delete the entire `registerComponentDiscoveryToolCallbacks` method (originally lines 673-685). It has no remaining callers.

- [ ] **Step 3: Audit `actionDefinitionService` param**

`actionDefinitionService` was used only by `ListComponentActionsToolCallback` and `DescribeComponentActionToolCallback` (which were registered via this helper). After the helper deletion, both bean methods should drop the `actionDefinitionService` parameter and its import.

`componentDefinitionService` stays — it's still used by `SearchComponentsToolCallback` (wait, that's being removed too via this helper deletion) BUT also by `CreateConnectionToolCallback`, `SelectConnectionToolCallback`, and `DescribeSourceComponentEntitiesToolCallback`. So keep `componentDefinitionService`.

- [ ] **Step 4: Compile + tests + spotless + commit**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:check spotlessApply
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/config/AiHubConfiguration.java
git commit -m "$(cat <<'EOF'
520 Remove AiHub component discovery callbacks superseded by editor sub-agents

Search / list-actions / describe-action are covered by ComponentTools
on every editor sub-agent (skills, cluster element, code editor,
workflow editor, converter). Deletes the registerComponentDiscoveryToolCallbacks
helper entirely and drops actionDefinitionService from both bean
method signatures.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 14: Update AiHub prompts to reflect removed direct tools

After Tasks 11-13, the AiHub system prompts may still reference now-removed tools by name (e.g. "use `listWorkflows` to enumerate workflows"). Audit and rewrite.

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/prompt_ai_hub_ask.txt`
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/prompt_ai_hub_build.txt`

- [ ] **Step 1: Search each prompt for removed tool names**

For each file, grep for the names: `listWorkflows`, `getWorkflow`, `createWorkflow`, `updateWorkflow`, `listProjects`, `createProject`, `updateProject`, `deleteProject`, `searchComponents`, `listComponentActions`, `describeComponentAction`.

- [ ] **Step 2: Rewrite the matched sections**

Replace any imperative reference to a removed tool with the corresponding sub-agent invocation. Example: "Use `listWorkflows` to enumerate the user's workflows" → "Delegate to `workflow_editor_agent` for workflow enumeration and analysis."

If a section is no longer relevant at the AiHub level (e.g. a paragraph describing how to combine `searchComponents` + `describeComponentAction`), delete it — that knowledge now lives in the relevant Copilot sub-agent's prompt.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/prompt_ai_hub_ask.txt \
        server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/prompt_ai_hub_build.txt
git commit -m "520 Rewrite AiHub prompts to remove references to delegated tools"
```

---

## Task 15: End-to-end smoke

- [ ] **Step 1: Full build**

Run: `./gradlew clean compileJava check`
Expected: BUILD SUCCESSFUL across all modules.

- [ ] **Step 2: Server start + manual smoke**

```bash
cd server && docker compose -f docker-compose.dev.infra.yml up -d && cd ..
./gradlew -p server/apps/server-app bootRun
```

- [ ] **Step 3: Manual checks via AI Hub UI**

For each of the five specialist domains, send a representative prompt and confirm the AiHub LLM invokes the corresponding sub-agent tool (visible in the AG-UI tool-call stream):
- "list my skills" → `skills_agent`
- "explain the cluster elements in workflow X" → `cluster_element_agent`
- "what does this script do" (in a workflow with a script step) → `code_editor_agent`
- "design a workflow that summarises my unread emails" → `workflow_editor_agent`
- "convert this n8n workflow JSON to ByteChef" (BUILD) → `converter_agent`

Also confirm that an off-domain prompt (e.g. "list my projects") still goes through AiHub's own tools, not a sub-agent — i.e. the sub-agents pull only their intended traffic, not everything.

- [ ] **Step 4: Verify no orphaned imports and removed callbacks aren't sneaking back in**

```bash
grep -rn "ReadSkillsTools\|SkillsTools" server/ee/libs/automation/automation-ai-hub/ --include='*.java'
grep -rn "new ListWorkflowsToolCallback\|new GetWorkflowToolCallback\|new CreateWorkflowToolCallback\|new UpdateWorkflowToolCallback" \
    server/ee/libs/automation/automation-ai-hub/ --include='*.java'
grep -rn "new ListProjectsToolCallback\|new CreateProjectToolCallback\|new UpdateProjectToolCallback\|new DeleteProjectToolCallback" \
    server/ee/libs/automation/automation-ai-hub/ --include='*.java'
grep -rn "SearchComponentsToolCallback\|ListComponentActionsToolCallback\|DescribeComponentActionToolCallback\|registerComponentDiscoveryToolCallbacks" \
    server/ee/libs/automation/automation-ai-hub/ --include='*.java'
```

Expected: no matches in `automation-ai-hub-service/src/main/`. `SkillsTools` / `ReadSkillsTools` should only appear in `CopilotConfiguration.java`.

- [ ] **Step 5: Tag for review**

The full refactor is now end-to-end. Open a PR referencing the spec and plan and request review on:
- The new `*AgentToolCallback` classes (do the DESCRIPTIONs read well? Will the LLM pick them appropriately?).
- The AiHub system-prompt tweaks (Task 10).
- The `registerCopilotSubAgentToolCallbacks` shape — does the `null` for ASK-converter feel right or do we want an overload?

---

## Self-Review

**Spec coverage:**
- §"Wrap a stateless ChatClient" — Tasks 3, 5, 6, 7, 8 (one ChatClient bean per specialist × ASK/BUILD).
- §"New ToolCallback wrappers" — Tasks 1-2 (Skills as the canonical example), 5, 6, 7, 8.
- §"AiHubConfiguration changes (skills)" — Task 4 (Skills wiring + removal of old params), Task 9 (helper extraction).
- §"Broader removal: AiHub-direct ToolCallbacks superseded by sub-agents" — Tasks 11 (workflow CRUD), 12 (project CRUD), 13 (component discovery + helper deletion), and the existing `ReadSkillsTools` / `SkillsTools` removal already in Task 4.
- §"Constructor parameter cleanup" — Tasks 11 Step 2, 12 Step 2, 13 Step 3 each include explicit param-audit steps.
- §"Open question 1 (RAG advisor)" — Task 7 keeps `QuestionAnswerAdvisor` on the workflow-editor ASK ChatClient.
- §"Open question 2 (system prompt)" — Tasks 10 + 14 (the latter covers the broader prompt audit after callback removals).
- §"Open question 3 (metering)" — out of scope per the plan header.
- §"Open question 4 (error envelope)" — Task 2 uses `ToolErrors.error(...)`.
- §"What does NOT change" — preserved by what's absent from the plan: no `AiHubRoutingAgent` changes, no MCP-tool-bean deletions, no Copilot panel changes; the "Kept on AiHub" list (tabs, data tables, knowledge bases, AiHub-task, deployments, …) is left intact.

**Placeholder scan:** none in the canonical Task 1-2 + 4 + 9-10 + 11-14. Tasks 5-8 reference back to Tasks 1-4 for the five-step structure but include the unique content (NAME, DESCRIPTION, ChatClient bean signature) inline.

**Type consistency:**
- Callback class names: `SkillsAgentToolCallback`, `ClusterElementAgentToolCallback`, `CodeEditorAgentToolCallback`, `WorkflowEditorAgentToolCallback`, `ConverterAgentToolCallback` — consistent across tasks.
- Tool names: `skills_agent`, `cluster_element_agent`, `code_editor_agent`, `workflow_editor_agent`, `converter_agent` — consistent across implementation, test, prompt, and helper.
- ChatClient bean names: `<domain><Ask|Build>SubAgentChatClient` — consistent.
- Removed callback names in Tasks 11-13 match the spec's removal table exactly.

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-05-25-aihub-delegate-to-copilot-subagents.md`. Two execution options:

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration. Good fit here since each task ends in a clean compile + commit checkpoint.

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints for review.

Which approach?
