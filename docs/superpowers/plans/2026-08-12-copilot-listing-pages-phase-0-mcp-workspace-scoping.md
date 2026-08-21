# Phase 0 — MCP Workspace Scoping for Sub-Agent Delegates — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every sub-agent delegate exposed on the management MCP server resolves a workspace, so MCP clients stop getting "Workspace context unavailable - open this chat from the AI Hub of a workspace."

**Architecture:** The management MCP surface has no AI Hub chat state, so nothing seeds the workspace id into the Spring AI `ToolContext`. Manager delegates already solve this: `WorkspaceScopedManagerToolCallback` wraps them, adds an optional `workspaceId` input, resolves it (explicit → tenant's sole workspace → typed `workspace_required` error listing candidates), and forwards it under `AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY`. The 12 copilot-domain delegates are contributed raw and therefore forward `Map.of()`. This phase widens that wrapper to accept any `ToolCallback` and applies it to all 12. The Copilot panel and AI Hub surfaces are untouched — they already seed workspace context from chat state.

**Tech Stack:** Java 25, Spring Boot 4, Spring AI (`ToolCallback`, `ToolContext`, `ToolDefinition`), JUnit 5, AssertJ, Mockito, Gradle Kotlin DSL.

## Global Constraints

- Base branch: `0_732`. Worktree: `/Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732`, branch `claude/copilot-listing-pages-spec`.
- Spec: `docs/superpowers/specs/2026-08-12-copilot-automation-listing-pages-design.md`, section "MCP management surface".
- License headers: Apache 2.0 for files under `server/libs/`; ByteChef Enterprise header **and** a `@version ee` Javadoc tag for files under `server/ee/`. Copy the header verbatim from a sibling file in the same directory.
- `@author Ivica Cardic` on every new class.
- Test class names end in `Test` (unit) — never `IntTest` here. Test method names are camelCase with no underscores (Checkstyle enforces this for **all** methods in test sources, including private helpers).
- Blank line before control statements (`if`, `for`, `try`, …) except immediately after an opening `{`; blank line after a variable modification that a following statement uses; no blank line before a class's closing `}`.
- No `TODO:` comments (Checkstyle `TodoComment` forbids them).
- Descriptive variable names — no single letters, no abbreviations, including lambda parameters.
- Run `./gradlew spotlessApply` before every commit. Spotless picks the EE license header from the `@version ee` **content**, not the path.
- Gradle exit codes are unreliable through a pipe: redirect output to a file, check `$?`, and grep for `^> Task .* FAILED`.
- Commit message prefix: `---` (this work has no ticket number; matches `--- Add the AI model catalog extraction design` on this branch).
- Never `git commit --amend` on shared history; always fresh commits.

---

## File Structure

| File | Responsibility | Task |
|---|---|---|
| `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/WorkspaceScopedSubAgentToolCallback.java` | Renamed + generalized wrapper: any `ToolCallback` delegate | 1 |
| `.../automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/WorkspaceScopedSubAgentToolCallbackTest.java` | Wrapper unit tests, incl. a non-manager delegate | 1 |
| `.../automation-ai-tool/.../ManagerMcpContributorConfiguration.java` | 2 CE manager wrap sites — rename only | 1 |
| `server/ee/libs/automation/automation-ai/automation-ai-tool/.../ApiCollectionManagerMcpContributorConfiguration.java` | 1 EE manager wrap site — rename only | 1 |
| `server/ee/libs/ai/ai-hub/ai-hub-service/.../config/AiHubManagerMcpContributorConfiguration.java` | 1 EE manager wrap site — rename only | 1 |
| `server/libs/ai/ai-copilot/ai-copilot-service/.../config/ToolCallbackContributorConfiguration.java` | Wrap the 8 CE copilot delegates | 2 |
| `.../ai-copilot-service/src/test/.../config/McpServerToolCallbackContributorConfigurationTest.java` | Updated arity + schema assertions | 2 |
| `server/ee/libs/automation/automation-ai/automation-ai-copilot/.../config/AutomationCopilotMcpContributorConfiguration.java` (+ `build.gradle.kts`) | Wrap the 3 EE automation delegates | 3 |
| `.../automation-ai-copilot/src/test/.../AutomationCopilotMcpContributorConfigurationTest.java` | Updated arity + schema assertions | 3 |
| `server/ee/libs/embedded/embedded-ai/embedded-ai-copilot/.../config/EmbeddedCopilotMcpContributorConfiguration.java` (+ `build.gradle.kts`) | Wrap the embedded delegate | 4 |
| `.../embedded-ai-copilot/src/test/.../config/EmbeddedCopilotMcpContributorConfigurationTest.java` | New test (no test source set exists in this module yet) | 4 |

**The 12 delegates being wrapped:** CE (8) `workflow_editor_agent`, `code_editor_agent`, `cluster_element_agent`, `skills_agent`, `workflow_execution_agent`, `converter_agent`, `knowledge_base_agent`, `data_table_agent`; EE automation (3) `context_store_agent`, `custom_component_agent`, `code_workflow_agent`; EE embedded (1) `workflow_editor_embedded_agent`.

**Design constraint to preserve:** the wrapper re-serializes delegate input as `Map.of("request", request)`, dropping every other field. All 12 delegates (and all 4 managers) declare an input schema of exactly `{"request": string}`, so this is safe today. Task 1 documents it in Javadoc rather than building pass-through machinery no delegate needs (YAGNI).

---

### Task 1: Generalize and rename the wrapper

Widen the delegate type from `ManagerSubAgentToolCallback` to `ToolCallback` and rename the class to match its new scope. All four existing construction sites pass a `ManagerSubAgentToolCallback`, which **is** a `ToolCallback`, so no call site needs a cast — only the type name changes.

**Files:**
- Rename: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/WorkspaceScopedManagerToolCallback.java` → `WorkspaceScopedSubAgentToolCallback.java`
- Rename: `server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/WorkspaceScopedManagerToolCallbackTest.java` → `WorkspaceScopedSubAgentToolCallbackTest.java`
- Modify: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ManagerMcpContributorConfiguration.java` (lines 61, 66)
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/ee/automation/ai/tool/ApiCollectionManagerMcpContributorConfiguration.java` (line 54 + import)
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubManagerMcpContributorConfiguration.java` (line 54 + import)
- Modify: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/AutomationToolInvocationContext.java` (Javadoc reference on line 26)

**Interfaces:**
- Consumes: nothing from earlier tasks (first task).
- Produces: `public class WorkspaceScopedSubAgentToolCallback implements ToolCallback` with constructor
  `WorkspaceScopedSubAgentToolCallback(ToolCallback delegate, WorkspaceService workspaceService)`.
  Tasks 2–4 construct it with copilot-domain delegates.

> **Ordering correction (found during execution).** Steps 1–5 as originally written renamed the *file* before the *class declaration*, so the module could not compile in between and Step 3 failed with `class WorkspaceScopedManagerToolCallback is public, should be declared in a file named …` instead of the intended `incompatible types`. That is a false red — it proves nothing about the widening. Do the rename **atomically first** (file, class declaration, `catch` block class literal, all four call sites, and the living docs — but do NOT widen the delegate field yet), then run the new test to get the genuine `incompatible types` failure, then widen the field and constructor parameter to `ToolCallback`. Steps 1, 3, 4, and 5 below are annotated accordingly.

- [ ] **Step 1: Rename both files with git so history follows** — and in the same step rename the class declaration inside the file, update the `catch (RuntimeException)` class literal, apply the Step 5 sed across the four call sites and living docs, and apply the Step 4 Javadoc. Leave the delegate field typed `ManagerSubAgentToolCallback` for now: the tree must compile so Step 3 can produce a meaningful failure.

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732
git mv server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/WorkspaceScopedManagerToolCallback.java \
       server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/WorkspaceScopedSubAgentToolCallback.java
git mv server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/WorkspaceScopedManagerToolCallbackTest.java \
       server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/WorkspaceScopedSubAgentToolCallbackTest.java
```

- [ ] **Step 2: Add the failing test for a non-manager delegate**

Append this test and its fake to `WorkspaceScopedSubAgentToolCallbackTest.java`, and rename the existing class + the three `WorkspaceScopedManagerToolCallback` references inside it (lines 36, 40, 46) to `WorkspaceScopedSubAgentToolCallback`. The new fake is a plain `ToolCallback` — it does **not** extend `ManagerSubAgentToolCallback`, which is exactly what fails to compile before the widening.

```java
    @Test
    void testWrapsPlainToolCallbackDelegate() {
        PlainCopilotDelegate copilotDelegate = new PlainCopilotDelegate();

        WorkspaceScopedSubAgentToolCallback copilotToolCallback =
            new WorkspaceScopedSubAgentToolCallback(copilotDelegate, workspaceService);

        String result = copilotToolCallback.call("{\"request\": \"list tables\", \"workspaceId\": 9}");

        assertThat(result).isEqualTo("delegated");
        assertThat(copilotToolCallback.getToolDefinition()
            .name()).isEqualTo("data_table_agent");
        assertThat(copilotToolCallback.getToolDefinition()
            .inputSchema()).contains("workspaceId");
        assertThat(copilotDelegate.capturedContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 9L);
    }

    /**
     * Fake copilot-domain delegate: a plain {@link ToolCallback} with the same {@code {request}} input schema the real
     * delegates declare, proving the wrapper no longer requires a {@link ManagerSubAgentToolCallback}.
     */
    private static final class PlainCopilotDelegate implements ToolCallback {

        private Map<String, Object> capturedContext;

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name("data_table_agent")
                .description("Delegates data table work.")
                .inputSchema("{\"type\": \"object\", \"properties\": {\"request\": {\"type\": \"string\"}}}")
                .build();
        }

        @Override
        public String call(String toolInput) {
            return call(toolInput, null);
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            capturedContext = toolContext == null ? null : toolContext.getContext();

            return "delegated";
        }
    }
```

Add these imports to the test file:

```java
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
```

- [ ] **Step 3: Run the test to verify it fails**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732 && ./gradlew :server:libs:automation:automation-ai:automation-ai-tool:test --tests "com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallbackTest" > /tmp/phase0-task1.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED|error:|cannot find symbol|incompatible types" /tmp/phase0-task1.log | head -20
```

Expected: compilation failure — `incompatible types: PlainCopilotDelegate cannot be converted to ManagerSubAgentToolCallback`, reported against the new test's `new WorkspaceScopedSubAgentToolCallback(copilotDelegate, workspaceService)` line. If instead you see `class ... should be declared in a file named ...`, the Step 1 rename was not atomic — finish it before continuing, because that error masks the signal this step exists to produce.

**Gradle is slow under concurrent builds** (7–9 minutes per invocation when another full-repo `check` holds the locks). Do not mistake a slow build for a hung one, and combine invocations wherever the steps below allow.

- [ ] **Step 4: Generalize the wrapper** — only the field/constructor widening remains here if Step 1 was done atomically as corrected above.

In `WorkspaceScopedSubAgentToolCallback.java` make exactly these changes:

1. Class declaration and constructor — widen the delegate type:

```java
public class WorkspaceScopedSubAgentToolCallback implements ToolCallback {
```

```java
    private final ToolCallback delegate;
    private final WorkspaceService workspaceService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkspaceScopedSubAgentToolCallback(ToolCallback delegate, WorkspaceService workspaceService) {
        this.delegate = delegate;
        this.workspaceService = workspaceService;
    }
```

2. The `catch (RuntimeException)` block at the end of `call` — update the class literal:

```java
            return ToolErrors.runtimeFailure(
                jsonMapper, WorkspaceScopedSubAgentToolCallback.class, toolDefinition.name(), exception);
```

3. Replace the class Javadoc with one that states the new scope and the input-schema constraint:

```java
/**
 * Adapts a sub-agent delegate {@link ToolCallback} for the management MCP server, where no AI Hub chat state exists to
 * inject the workspace-scoped {@link AutomationToolInvocationContext}. The wrapper extends the delegate's input with an
 * optional {@code workspaceId}, resolves it (explicit input, else the tenant's sole workspace, else a typed error
 * listing the candidates), and forwards it to the specialist through the {@link ToolContext} under
 * {@link AutomationToolInvocationContext#TOOL_CONTEXT_WORKSPACE_ID_KEY} — exactly what the specialist's
 * workspace-scoped tools read on the chat surface.
 *
 * <p>
 * Applies to both manager delegates ({@link ManagerSubAgentToolCallback}) and the copilot-domain delegates
 * ({@code data_table_agent}, {@code workflow_editor_agent}, …). Delegates must declare an input schema of exactly
 * {@code {"request": string}}: this wrapper re-serializes the delegate input as {@code {"request": ...}} and drops any
 * other field. Every delegate on this surface satisfies that today.
 * </p>
 *
 * <p>
 * No authorization is added or bypassed here: the management MCP request is already authenticated, and every mutation
 * behind the specialist goes through {@code @PreAuthorize}-guarded facades. Workspace selection only scopes lookups.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
```

Leave the `INPUT_SCHEMA` constant, `getToolDefinition()`, the resolution logic, and the `WorkspaceScopedInput` record unchanged.

- [ ] **Step 5: Rename the four existing construction sites**

Each is a pure identifier rename (type name in `new …(…)` plus the import where present). `ManagerMcpContributorConfiguration` is in the same package and has no import to change.

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732
sed -i '' 's/WorkspaceScopedManagerToolCallback/WorkspaceScopedSubAgentToolCallback/g' \
  server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ManagerMcpContributorConfiguration.java \
  server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/AutomationToolInvocationContext.java \
  server/ee/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/ee/automation/ai/tool/ApiCollectionManagerMcpContributorConfiguration.java \
  server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubManagerMcpContributorConfiguration.java
grep -rn "WorkspaceScopedManagerToolCallback" server/ --include="*.java" | grep -v build/
```

The final `grep` must print nothing.

- [ ] **Step 6: Run the tests to verify they pass**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732 && ./gradlew :server:libs:automation:automation-ai:automation-ai-tool:test --tests "com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallbackTest" --tests "com.bytechef.automation.ai.tool.ManagerMcpContributorConfigurationTest" > /tmp/phase0-task1.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/phase0-task1.log
```

Expected: `BUILD SUCCESSFUL`, all 6 wrapper tests plus the 2 contributor tests passing.

- [ ] **Step 7: Compile the three modules that reference the renamed class**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732 && ./gradlew :server:ee:libs:automation:automation-ai:automation-ai-tool:compileJava :server:ee:libs:ai:ai-hub:ai-hub-service:compileJava > /tmp/phase0-task1-compile.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED|error:|BUILD (SUCCESSFUL|FAILED)" /tmp/phase0-task1-compile.log | head -20
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Format and commit**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732 && ./gradlew spotlessApply > /tmp/phase0-spotless.log 2>&1; echo "exit=$?"
git add server/libs/automation/automation-ai/automation-ai-tool server/ee/libs/automation/automation-ai/automation-ai-tool server/ee/libs/ai/ai-hub/ai-hub-service
git commit -m "--- Generalize the workspace-scoping MCP wrapper to any sub-agent delegate"
```

---

### Task 2: Workspace-scope the 8 CE copilot delegates

**Files:**
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/ToolCallbackContributorConfiguration.java`
- Test: `server/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ai/copilot/config/McpServerToolCallbackContributorConfigurationTest.java`

No Gradle change: `ai-copilot-service` already depends on both `automation-ai-tool` (the wrapper) and `automation-configuration-api` (`WorkspaceService`).

**Interfaces:**
- Consumes: `WorkspaceScopedSubAgentToolCallback(ToolCallback, WorkspaceService)` from Task 1.
- Produces: `copilotAgentToolCallbackContributor(...)` gains a trailing `WorkspaceService workspaceService` parameter. Tool **names are unchanged** — the wrapper preserves the delegate's name.

- [ ] **Step 1: Write the failing test**

In `McpServerToolCallbackContributorConfigurationTest`, update the existing `contributesAgentCallbacksWhenChatClientsPresent` call to pass `mock(WorkspaceService.class)` as the new trailing argument, and add this test:

```java
    @Test
    void testContributedAgentToolsAcceptWorkspaceId() {
        McpServerToolCallbackContributor contributor = configuration.copilotAgentToolCallbackContributor(
            emptyProvider(), presentProvider(), presentProvider(), presentProvider(), presentProvider(),
            presentProvider(), presentSupplierProvider(), presentProvider(), presentProvider(),
            mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks())
            .allSatisfy(toolCallback -> assertThat(toolCallback.getToolDefinition()
                .inputSchema()).contains("workspaceId"));
    }
```

Add the import:

```java
import com.bytechef.automation.configuration.service.WorkspaceService;
```

Match the existing helper names in this file (`presentProvider()` / `emptyProvider()` / the converter supplier helper) rather than the names above if they differ — read the file first and reuse what is there.

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732 && ./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:test --tests "com.bytechef.ai.copilot.config.McpServerToolCallbackContributorConfigurationTest" > /tmp/phase0-task2.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED|error:|method .* cannot be applied" /tmp/phase0-task2.log | head -20
```

Expected: compilation failure — the factory method does not take a `WorkspaceService` argument yet.

- [ ] **Step 3: Wrap the delegates**

In `ToolCallbackContributorConfiguration.java`, add the parameter and wrap each `toolCallbacks.add(...)`. `SkillsTools` is a plain tool set, not a delegate — leave it unwrapped.

```java
    @Bean
    McpServerToolCallbackContributor copilotAgentToolCallbackContributor(
        ObjectProvider<SkillsTools> skillsToolsProvider,
        @Qualifier("workflowEditorBuildSubAgentChatClient") ObjectProvider<ChatClient> workflowEditorProvider,
        @Qualifier("codeEditorBuildSubAgentChatClient") ObjectProvider<ChatClient> codeEditorProvider,
        @Qualifier("clusterElementBuildSubAgentChatClient") ObjectProvider<ChatClient> clusterElementProvider,
        @Qualifier("skillsBuildSubAgentChatClient") ObjectProvider<ChatClient> skillsProvider,
        @Qualifier("workflowExecutionBuildSubAgentChatClient") ObjectProvider<ChatClient> workflowExecutionProvider,
        @Qualifier("converterBuildSubAgentChatClientSupplier") //
        ObjectProvider<Supplier<ChatClient>> converterSupplierProvider,
        @Qualifier("knowledgeBaseBuildSubAgentChatClient") ObjectProvider<ChatClient> knowledgeBaseProvider,
        @Qualifier("dataTableBuildSubAgentChatClient") ObjectProvider<ChatClient> dataTableProvider,
        WorkspaceService workspaceService) {

        return () -> {
            List<ToolCallback> toolCallbacks = new ArrayList<>();

            skillsToolsProvider.ifAvailable(
                skillsTools -> toolCallbacks.addAll(List.of(ToolCallbacks.from(skillsTools))));

            workflowEditorProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new WorkflowEditorAgentToolCallback(chatClient), workspaceService)));
            codeEditorProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new CodeEditorAgentToolCallback(chatClient), workspaceService)));
            clusterElementProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new ClusterElementAgentToolCallback(chatClient), workspaceService)));
            skillsProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new SkillsAgentToolCallback(chatClient), workspaceService)));
            workflowExecutionProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new WorkflowExecutionAgentToolCallback(chatClient), workspaceService)));
            converterSupplierProvider.ifAvailable(
                converterChatClientSupplier -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new ConverterAgentToolCallback(converterChatClientSupplier), workspaceService)));
            knowledgeBaseProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new KnowledgeBaseAgentToolCallback(chatClient), workspaceService)));
            dataTableProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new DataTableAgentToolCallback(chatClient), workspaceService)));

            return toolCallbacks;
        };
    }
```

Add these imports:

```java
import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
import com.bytechef.automation.configuration.service.WorkspaceService;
```

- [ ] **Step 4: Run the tests to verify they pass**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732 && ./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:test --tests "com.bytechef.ai.copilot.config.McpServerToolCallbackContributorConfigurationTest" > /tmp/phase0-task2.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/phase0-task2.log
```

Expected: `BUILD SUCCESSFUL`. The pre-existing `hasSize(8)` assertion still holds — wrapping preserves both the count and each delegate's tool name.

- [ ] **Step 5: Format and commit**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732 && ./gradlew spotlessApply > /tmp/phase0-spotless.log 2>&1; echo "exit=$?"
git add server/libs/ai/ai-copilot/ai-copilot-service
git commit -m "--- Workspace-scope the CE copilot subagents on the management MCP server"
```

---

### Task 3: Workspace-scope the 3 EE automation copilot delegates

**Files:**
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-copilot/build.gradle.kts`
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-copilot/src/main/java/com/bytechef/ee/automation/ai/copilot/config/AutomationCopilotMcpContributorConfiguration.java`
- Test: `server/ee/libs/automation/automation-ai/automation-ai-copilot/src/test/java/com/bytechef/ee/automation/ai/copilot/config/AutomationCopilotMcpContributorConfigurationTest.java`

This module lacks both required Gradle edges — CE `automation-ai-tool` (the wrapper) and `automation-configuration-api` (`WorkspaceService`). No cycle: `automation-ai-tool` does not depend on any EE module.

**Interfaces:**
- Consumes: `WorkspaceScopedSubAgentToolCallback(ToolCallback, WorkspaceService)` from Task 1.
- Produces: `automationCopilotAgentToolCallbackContributor(...)` gains a trailing `WorkspaceService workspaceService` parameter.

- [ ] **Step 1: Add the two Gradle dependencies**

In the `dependencies` block of `server/ee/libs/automation/automation-ai/automation-ai-copilot/build.gradle.kts`, add these two lines among the existing `implementation(project(...))` entries, keeping the block's alphabetical ordering:

```kotlin
    implementation(project(":server:libs:automation:automation-ai:automation-ai-tool"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
```

- [ ] **Step 2: Write the failing test**

In `AutomationCopilotMcpContributorConfigurationTest`, pass `mock(WorkspaceService.class)` as a new trailing argument to every existing `automationCopilotAgentToolCallbackContributor(...)` call, and add:

```java
    @Test
    void testContributedAgentToolsAcceptWorkspaceId() {
        McpServerToolCallbackContributor contributor = configuration.automationCopilotAgentToolCallbackContributor(
            toPresentProvider(), toPresentProvider(), toPresentProvider(), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks())
            .allSatisfy(toolCallback -> assertThat(toolCallback.getToolDefinition()
                .inputSchema()).contains("workspaceId"));
    }
```

Add the import:

```java
import com.bytechef.automation.configuration.service.WorkspaceService;
```

Use whatever provider-helper names already exist in this file.

- [ ] **Step 3: Run the test to verify it fails**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732 && ./gradlew :server:ee:libs:automation:automation-ai:automation-ai-copilot:test --tests "com.bytechef.ee.automation.ai.copilot.config.AutomationCopilotMcpContributorConfigurationTest" > /tmp/phase0-task3.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED|error:|method .* cannot be applied" /tmp/phase0-task3.log | head -20
```

Expected: compilation failure — the factory method does not take a `WorkspaceService` argument yet.

- [ ] **Step 4: Wrap the delegates**

Replace the bean method body in `AutomationCopilotMcpContributorConfiguration.java`:

```java
    @Bean
    McpServerToolCallbackContributor automationCopilotAgentToolCallbackContributor(
        @Qualifier("contextStoreBuildSubAgentChatClient") ObjectProvider<ChatClient> contextStoreProvider,
        @Qualifier("customComponentBuildSubAgentChatClient") ObjectProvider<ChatClient> customComponentProvider,
        @Qualifier("codeWorkflowBuildSubAgentChatClient") ObjectProvider<ChatClient> codeWorkflowProvider,
        WorkspaceService workspaceService) {

        return () -> {
            List<ToolCallback> toolCallbacks = new ArrayList<>();

            contextStoreProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new ContextStoreAgentToolCallback(chatClient), workspaceService)));
            customComponentProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new CustomComponentAgentToolCallback(chatClient), workspaceService)));
            codeWorkflowProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new CodeWorkflowAgentToolCallback(chatClient), workspaceService)));

            return toolCallbacks;
        };
    }
```

Add these imports:

```java
import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
import com.bytechef.automation.configuration.service.WorkspaceService;
```

Keep the existing EE license header, the `@version ee` tag, and the class Javadoc; extend the Javadoc's final sentence with: `Each delegate is wrapped in {@link WorkspaceScopedSubAgentToolCallback} because the management MCP surface has no AI Hub chat state to supply workspace scope.`

- [ ] **Step 5: Run the tests to verify they pass**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732 && ./gradlew :server:ee:libs:automation:automation-ai:automation-ai-copilot:test --tests "com.bytechef.ee.automation.ai.copilot.config.AutomationCopilotMcpContributorConfigurationTest" > /tmp/phase0-task3.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/phase0-task3.log
```

Expected: `BUILD SUCCESSFUL`. The pre-existing `containsExactly("context_store_agent", "custom_component_agent", "code_workflow_agent")` assertion still holds.

- [ ] **Step 6: Format and commit**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732 && ./gradlew spotlessApply > /tmp/phase0-spotless.log 2>&1; echo "exit=$?"
git add server/ee/libs/automation/automation-ai/automation-ai-copilot
git commit -m "--- Workspace-scope the EE automation copilot subagents on the management MCP server"
```

---

### Task 4: Workspace-scope the embedded delegate

**Files:**
- Modify: `server/ee/libs/embedded/embedded-ai/embedded-ai-copilot/build.gradle.kts`
- Modify: `server/ee/libs/embedded/embedded-ai/embedded-ai-copilot/src/main/java/com/bytechef/ee/embedded/ai/copilot/config/EmbeddedCopilotMcpContributorConfiguration.java`
- Create: `server/ee/libs/embedded/embedded-ai/embedded-ai-copilot/src/test/java/com/bytechef/ee/embedded/ai/copilot/config/EmbeddedCopilotMcpContributorConfigurationTest.java`

This module has `automation-configuration-api` already but not CE `automation-ai-tool`, and it has **no test source set at all** — the directory tree and the `testImplementation` dependencies must be created.

**Interfaces:**
- Consumes: `WorkspaceScopedSubAgentToolCallback(ToolCallback, WorkspaceService)` from Task 1.
- Produces: `embeddedWorkflowEditorMcpToolCallbackContributor(...)` gains a trailing `WorkspaceService workspaceService` parameter.

- [ ] **Step 1: Add the Gradle dependencies**

In `server/ee/libs/embedded/embedded-ai/embedded-ai-copilot/build.gradle.kts`, add the wrapper module among the existing `implementation(project(...))` entries (alphabetical):

```kotlin
    implementation(project(":server:libs:automation:automation-ai:automation-ai-tool"))
```

and add the test dependencies at the end of the `dependencies` block (this module has none today):

```kotlin
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
```

- [ ] **Step 2: Write the failing test**

Create `server/ee/libs/embedded/embedded-ai/embedded-ai-copilot/src/test/java/com/bytechef/ee/embedded/ai/copilot/config/EmbeddedCopilotMcpContributorConfigurationTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.copilot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.configuration.service.WorkspaceService;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Verifies the embedded contributor exposes the workflow_editor_embedded_agent delegate workspace-scoped, and skips an
 * absent ChatClient bean.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedCopilotMcpContributorConfigurationTest {

    private final EmbeddedCopilotMcpContributorConfiguration configuration =
        new EmbeddedCopilotMcpContributorConfiguration();

    @Test
    void testContributesWorkspaceScopedEmbeddedAgent() {
        McpServerToolCallbackContributor contributor =
            configuration.embeddedWorkflowEditorMcpToolCallbackContributor(
                toPresentProvider(), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks()).singleElement()
            .satisfies(toolCallback -> {
                assertThat(toolCallback.getToolDefinition()
                    .name()).isEqualTo("workflow_editor_embedded_agent");
                assertThat(toolCallback.getToolDefinition()
                    .inputSchema()).contains("workspaceId");
            });
    }

    @Test
    void testMissingChatClientIsSkipped() {
        McpServerToolCallbackContributor contributor =
            configuration.embeddedWorkflowEditorMcpToolCallbackContributor(
                toAbsentProvider(), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    private ObjectProvider<ChatClient> toPresentProvider() {
        ChatClient chatClient = mock(ChatClient.class);

        @SuppressWarnings("unchecked")
        ObjectProvider<ChatClient> chatClientProvider = mock(ObjectProvider.class);

        doAnswer(invocation -> {
            Consumer<ChatClient> dependencyConsumer = invocation.getArgument(0);

            dependencyConsumer.accept(chatClient);

            return null;
        }).when(chatClientProvider)
            .ifAvailable(any());

        return chatClientProvider;
    }

    private ObjectProvider<ChatClient> toAbsentProvider() {
        @SuppressWarnings("unchecked")
        ObjectProvider<ChatClient> chatClientProvider = mock(ObjectProvider.class);

        return chatClientProvider;
    }
}
```

Note the unused-import guard: `java.util.List` is imported by the sibling tests but is unnecessary here — drop it if Checkstyle flags it.

- [ ] **Step 3: Run the test to verify it fails**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732 && ./gradlew :server:ee:libs:embedded:embedded-ai:embedded-ai-copilot:test --tests "com.bytechef.ee.embedded.ai.copilot.config.EmbeddedCopilotMcpContributorConfigurationTest" > /tmp/phase0-task4.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED|error:|method .* cannot be applied" /tmp/phase0-task4.log | head -20
```

Expected: compilation failure — the factory method takes only the provider argument.

- [ ] **Step 4: Wrap the delegate**

Replace the bean method in `EmbeddedCopilotMcpContributorConfiguration.java`:

```java
    @Bean
    McpServerToolCallbackContributor embeddedWorkflowEditorMcpToolCallbackContributor(
        @Qualifier("workflowEditorEmbeddedBuildSubAgentChatClient") //
        ObjectProvider<ChatClient> workflowEditorEmbeddedBuildSubAgentChatClientProvider,
        WorkspaceService workspaceService) {

        return () -> {
            List<ToolCallback> toolCallbacks = new ArrayList<>();

            workflowEditorEmbeddedBuildSubAgentChatClientProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new WorkflowEditorAgentToolCallback(
                            chatClient, "workflow_editor_embedded_agent", DESCRIPTION,
                            CopilotAgentType.WORKFLOW_EDITOR_EMBEDDED_AGENT),
                        workspaceService)));

            return toolCallbacks;
        };
    }
```

Add these imports:

```java
import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
import com.bytechef.automation.configuration.service.WorkspaceService;
```

Keep the EE header, `@version ee`, `DESCRIPTION`, and the class Javadoc; extend the Javadoc's second paragraph with: `The delegate is wrapped in {@link WorkspaceScopedSubAgentToolCallback} so the MCP client can supply workspace scope this surface cannot infer.`

- [ ] **Step 5: Run the tests to verify they pass**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732 && ./gradlew :server:ee:libs:embedded:embedded-ai:embedded-ai-copilot:test --tests "com.bytechef.ee.embedded.ai.copilot.config.EmbeddedCopilotMcpContributorConfigurationTest" > /tmp/phase0-task4.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/phase0-task4.log
```

Expected: `BUILD SUCCESSFUL`, 2 tests passing.

- [ ] **Step 6: Verify the whole phase — all four modules plus static analysis**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732 && ./gradlew :server:libs:automation:automation-ai:automation-ai-tool:check :server:libs:ai:ai-copilot:ai-copilot-service:check :server:ee:libs:automation:automation-ai:automation-ai-copilot:check :server:ee:libs:embedded:embedded-ai:embedded-ai-copilot:check :server:ee:libs:ai:ai-hub:ai-hub-service:check > /tmp/phase0-check.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/phase0-check.log
```

Expected: `BUILD SUCCESSFUL`. If Checkstyle/PMD/SpotBugs flag anything, fix it before committing.

- [ ] **Step 7: Format and commit**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-listing-pages-0732 && ./gradlew spotlessApply > /tmp/phase0-spotless.log 2>&1; echo "exit=$?"
git add server/ee/libs/embedded/embedded-ai/embedded-ai-copilot
git commit -m "--- Workspace-scope the embedded workflow editor subagent on the management MCP server"
```

---

## Manual verification (after all four tasks)

With `bytechef.ai.mcp.server.enabled=true` and a tenant that has two or more workspaces, call `data_table_agent` from an MCP client with no `workspaceId`. Before this phase it returned "Workspace context unavailable - open this chat from the AI Hub of a workspace."; it must now return the typed `workspace_required` error listing the candidate workspaces. Re-calling with an explicit `workspaceId` must succeed. Verify `deployment_manager` still behaves identically (regression check on the rename).
