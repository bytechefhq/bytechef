# Phase 2 — Project Slice, Intent-Aware Prompts, and the Generate-with-AI Entry Points — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Copilot opens from the Projects listing page scoped to a project agent that can create workflows, and the existing "Generate with AI" / "Create With AI" menu items open that conversation instead of a one-shot dialog.

**Architecture:** A new `PROJECT` domain slice follows the established shape — `Source.PROJECT` + `project_ask`/`project_build` agent beans + a prompt pair — with BUILD carrying the project/workflow write tools plus `workflow_editor_agent` and `converter_agent` as sub-agent tools. Two agents (`ProjectSpringAIAgent`, `SkillsSpringAIAgent`) gain an explicit read of `state.get("parameters")` so a caller can pass **scope** (which project) and **intent** (why the panel was opened). The client gets a shared `useOpenCopilot()` hook; three dropdown menu items call it and their dialogs are deleted.

**Tech Stack:** Java 25, Spring Boot 4, Spring AI, AG-UI; React 19, TypeScript 5.9, Zustand, TanStack Query, Vitest.

## Global Constraints

- Worktree: `/Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-phase2`, branch `claude/copilot-phase2-project-slice`, cut from 0_732 at `4467107d249`. Client commands run from its `client/` subdirectory.
- **0_732 is rewritten frequently.** Never `git rebase 0_732` — the branch point stops being an ancestor and the rebase replays other people's commits. Always `git rebase --onto 0_732 <original-branch-point>`, and back up the ref first.
- Spec: `docs/superpowers/specs/2026-08-12-copilot-automation-listing-pages-design.md`.
- Server: Apache 2.0 header under `server/libs/`, ByteChef Enterprise header **and** `@version ee` under `server/ee/` (Spotless selects by tag content, not path). `@author Ivica Cardic`. One blank line before control statements except right after an opening `{`; no blank line before a class's closing `}`. Test methods camelCase, no underscores, including private helpers. No `TODO:` comments.
- Client: object keys alphabetical (`sort-keys` — `--fix` does NOT repair it); named imports alphabetical inside `{}`; interfaces end `I` or `Props`; descriptive names including arrow-function parameters; `twMerge` not `cn()`; Lucide icons with `Icon` suffix; hook order with `useEffect`s last.
- `npm run format` then `npm run check` before client commits; module-scoped `spotlessApply` (never repo-wide) before server commits.
- Gradle exit codes are unreliable through a pipe — redirect to a file and grep it.
- Commit prefix `---` (server) and `--- client - ` (client).
- Stage explicit paths. Never `git add -A`.

## Key facts established by exploration (do not re-derive)

- **Agent ids are derived, not literal:** `Source.X.name() + "_" + Mode.Y.name()` lowercased. `Source.PROJECT` + `Mode.ASK` → `project_ask`, which is both the `LocalAgent` bean id the controller routes to and the `CopilotAgentType` key.
- **`state.get("parameters")` is read by exactly one agent today** — `CodeEditorSpringAIAgent`, to pick a language string. `SkillsSpringAIAgent` and `DataTableSpringAIAgent` have byte-identical `createSystemMessage` bodies that ignore it. The detail pages that pass `{dataTableId}`, `{knowledgeBaseId}`, `{contextStoreId}` are therefore passing them into the void — **out of scope here**, but it is why this phase reads `parameters` explicitly rather than relying on the incidental `State:` dump.
- `DataTableAgentConfiguration` is the cleanest config template: self-contained, field-level `@Value`, its own `wrapToolCallbacks`/`readPrompt`, gated `@ConditionalOnExpression("${bytechef.ai.copilot.enabled:false} or ${bytechef.ai.hub.enabled:false}")`.
- `ai-copilot-service` already depends on `automation-ai-tool`, and `CopilotConfiguration` already imports `ProjectTools`/`ProjectWorkflowTools`/`ReadProjectTools`/`ReadProjectWorkflowTools`. **No Gradle change is needed.**
- Sub-agent ChatClient qualifiers to wrap: `workflowEditorBuildSubAgentChatClient` (→ `new WorkflowEditorAgentToolCallback(chatClient)`) and `converterBuildSubAgentChatClientSupplier` (→ `new ConverterAgentToolCallback(supplier)`). EE code-workflow uses `codeWorkflowBuildSubAgentChatClient` → `new CodeWorkflowAgentToolCallback(chatClient)`, single-arg.

---

## File Structure

| File | Responsibility | Task |
|---|---|---|
| `.../ai-copilot-api/.../util/Source.java` | add `PROJECT` | 1 |
| `.../ai-copilot-tool/.../tool/CopilotAgentType.java` | add `PROJECT_ASK`/`PROJECT_BUILD`/`PROJECT` | 1 |
| `.../ai-copilot-service/.../agent/ProjectSpringAIAgent.java` | NEW — reads `parameters` for scope + intent | 1 |
| `.../ai-copilot-service/src/main/resources/prompt_project_ask.txt`, `prompt_project_build.txt` | NEW prompt pair | 2 |
| `.../ai-copilot-service/.../config/ProjectAgentConfiguration.java` | NEW — ask/build agents + sub-agent ChatClients | 2 |
| `.../ai-copilot-service/.../agent/SkillsSpringAIAgent.java` | read `parameters` for intent | 3 |
| `.../ai-copilot-service/src/main/resources/prompt_skills_build.txt` | document the intent marker | 3 |
| `server/ee/.../web/rest/CopilotApiController.java` | default-branch dispatch + 400 on unknown | 4 |
| `client/src/shared/components/copilot/hooks/useOpenCopilot.ts` | NEW shared open logic | 5 |
| `client/src/shared/components/copilot/CopilotButton.tsx` | consume the hook | 5 |
| `client/src/pages/automation/projects/Projects.tsx` | button + post-turn | 5 |
| `client/src/pages/automation/projects/components/project-list/ProjectListItem.tsx` | menu item → Copilot | 6 |
| `client/src/pages/automation/projects/components/project-workflow-list/ProjectWorkflowList.tsx` | menu item → Copilot | 6 |
| `client/src/pages/automation/ai/skills/components/AiSkillsCreateDropdown.tsx` | menu item → Copilot | 6 |
| DELETE `client/src/shared/components/workflow/GenerateWorkflowDialog.tsx`, `client/src/pages/automation/ai/skills/components/AiSkillGenerateDialog.tsx` | replaced by the panel | 6 |

---

### Task 1: `Source.PROJECT`, agent types, and `ProjectSpringAIAgent`

**Files:**
- Modify: `server/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ai/copilot/util/Source.java`
- Modify: `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/CopilotAgentType.java`
- Create: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/agent/ProjectSpringAIAgent.java`
- Test: `server/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ai/copilot/agent/ProjectSpringAIAgentTest.java`

**Interfaces:**
- Produces: `Source.PROJECT`; `CopilotAgentType.PROJECT_ASK/PROJECT_BUILD/PROJECT`; `ProjectSpringAIAgent.builder()` with the same builder surface as `DataTableSpringAIAgent`. Task 2 constructs the beans.

- [ ] **Step 1: Add the enum constants**

`Source.java` — append `PROJECT` to the constant list (the enum is a single comma-separated declaration; keep the existing wrapping style):

```java
    WORKFLOW_EDITOR, CODE_EDITOR, CONVERTER, CLUSTER_ELEMENT, SKILLS, JSON_SCHEMA_BUILDER, SAMPLE_OUTPUT,
    WORKFLOW_EXECUTION, WORKFLOW_CODE_EDITOR, CONTEXT_STORE, KNOWLEDGE_BASE, DATA_TABLE, PROJECT
```

`CopilotAgentType.java` — append three constants at the end of the list, matching the file's existing `(key, fallback)` pattern:

```java
    PROJECT_ASK("project_ask", false),
    PROJECT_BUILD("project_build", false),
    PROJECT("project", true);
```

Remember to move the semicolon off `DATA_TABLE_AGENT(...)` onto the new last constant.

- [ ] **Step 2: Write the failing test**

Create `ProjectSpringAIAgentTest.java`. It asserts the two behaviours that make this agent different from `DataTableSpringAIAgent`: scope and intent text appear in the system message when `parameters` carries them, and nothing is added when it does not.

```java
    @Test
    void testSystemMessageCarriesProjectScopeWhenParametersHaveProjectId() {
        State state = new State();

        state.set("parameters", Map.of("projectId", 42));

        SystemMessage systemMessage = buildAgent().createSystemMessage(state, List.of());

        assertThat(systemMessage.getContent()).contains("project with id 42");
    }

    @Test
    void testSystemMessageCarriesGenerateWorkflowIntent() {
        State state = new State();

        state.set("parameters", Map.of("intent", "generate_workflow", "projectId", 42));

        SystemMessage systemMessage = buildAgent().createSystemMessage(state, List.of());

        assertThat(systemMessage.getContent()).contains("open by asking what the workflow should do");
    }

    @Test
    void testSystemMessageOmitsScopeAndIntentWhenParametersAbsent() {
        SystemMessage systemMessage = buildAgent().createSystemMessage(new State(), List.of());

        assertThat(systemMessage.getContent()).doesNotContain("project with id");
        assertThat(systemMessage.getContent()).doesNotContain("open by asking what the workflow should do");
    }
```

Add a private `buildAgent()` helper constructing the agent via its builder with a mock `ChatModel` and a fixed `systemMessage`. **Read `DataTableSpringAIAgent` first** to see which builder calls are required for `build()` to succeed, and check whether `createSystemMessage` is accessible from the test package — if it is `protected`, the test lives in the same package (`com.bytechef.ai.copilot.agent`), which the path above already satisfies. If `State` has no `set` method, use whatever mutator it exposes; read `com.agui.core.state.State` before writing the test.

- [ ] **Step 3: Run the test to verify it fails**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-phase2 && ./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:test --tests "com.bytechef.ai.copilot.agent.ProjectSpringAIAgentTest" > /tmp/p2-t1-red.log 2>&1; echo "exit=$?"; grep -E "BUILD SUCCESSFUL|BUILD FAILED|error:|cannot find symbol" /tmp/p2-t1-red.log | head -10
```

Expected: compilation failure — `ProjectSpringAIAgent` does not exist.

- [ ] **Step 4: Create `ProjectSpringAIAgent`**

Copy `DataTableSpringAIAgent.java` verbatim, rename the three class-name occurrences (class declaration, protected constructor, `build()` return), and replace `createSystemMessage` with the version below. Everything else — the imports, `ADDITIONAL_RULES`, `toolContext`, and the whole `Builder` — stays identical.

```java
    @Override
    protected SystemMessage createSystemMessage(State state, List<Context> contexts) {
        List<String> contextStrings = contexts.stream()
            .map(Context::toString)
            .toList();

        String resolvedMessage = Objects.nonNull(this.systemMessageProvider)
            ? this.systemMessageProvider.apply(this) : this.systemMessage;

        String message = "%s%n%s%n%s%n%nState:%n%s%n%nContext:%n%s%n".formatted(
            resolvedMessage, createScopeAndIntentMessage(state), ADDITIONAL_RULES, state,
            String.join("\n", contextStrings));

        SystemMessage systemMessage = new SystemMessage();

        systemMessage.setId(String.valueOf(UUID.randomUUID()));
        systemMessage.setContent(message);

        return systemMessage;
    }

    /**
     * Turns the caller-supplied {@code parameters} into explicit instructions. Two callers exist: the Projects listing
     * page opens the panel with no parameters, and the "Generate with AI" menu items open it with a project id and an
     * intent. Read explicitly rather than relying on the {@code State:} dump appended below — that dump is a
     * {@code Map.toString()} the model would have to interpret unaided.
     */
    private String createScopeAndIntentMessage(State state) {
        if (!(state.get("parameters") instanceof Map<?, ?> parameters)) {
            return "";
        }

        StringBuilder message = new StringBuilder();

        Object projectId = parameters.get("projectId");

        if (projectId != null) {
            message.append(
                "The user is working in the project with id %s. Create and modify workflows in that project unless they name a different one.%n"
                    .formatted(projectId));
        }

        if ("generate_workflow".equals(parameters.get("intent"))) {
            message.append(
                "The user opened this conversation to create a new workflow. Do not wait for a detailed brief — open by asking what the workflow should do, then build it.%n");
        }

        return message.toString();
    }
```

Note `parameters` is read defensively with `instanceof` — unlike `CodeEditorSpringAIAgent`, which casts unconditionally and would throw for a caller that omits it. Every caller of THIS agent may legitimately omit parameters.

- [ ] **Step 5: Run the test to verify it passes**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-phase2 && ./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:test --tests "com.bytechef.ai.copilot.agent.ProjectSpringAIAgentTest" > /tmp/p2-t1-green.log 2>&1; echo "exit=$?"; grep -E "BUILD SUCCESSFUL|BUILD FAILED|^> Task .* FAILED" /tmp/p2-t1-green.log
```

Expected: `BUILD SUCCESSFUL`, 3 tests passing.

- [ ] **Step 6: Format and commit**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-phase2 && ./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:spotlessApply :server:libs:ai:ai-copilot:ai-copilot-api:spotlessApply :server:libs:ai:ai-copilot:ai-copilot-tool:spotlessApply > /tmp/p2-t1-spotless.log 2>&1; echo "exit=$?"
git add server/libs/ai/ai-copilot
git commit -m "--- Add the project copilot agent with scope and intent aware prompts"
```

---

### Task 2: `ProjectAgentConfiguration` and the prompt pair

**Files:**
- Create: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_project_ask.txt`
- Create: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_project_build.txt`
- Create: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/ProjectAgentConfiguration.java`
- Test: `server/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ai/copilot/config/ProjectAgentConfigurationTest.java`

**Interfaces:**
- Consumes: `ProjectSpringAIAgent`, `Source.PROJECT` (Task 1).
- Produces: `LocalAgent` beans with ids `project_ask` and `project_build`; `ChatClient` beans `projectAskSubAgentChatClient` and `projectBuildSubAgentChatClient`.

- [ ] **Step 1: Write the prompt pair**

Use `prompt_skills_build.txt` as the tone reference — imperative, tool-name-explicit, with an "Important Rules" section. Keep each under ~120 lines.

`prompt_project_ask.txt` covers: listing and searching projects, explaining a project's workflows and deployment status, and the read tools available (`listProjects`, `getProject`, `searchProjects`, `getProjectStatus`, `listWorkflows`, `getWorkflow`, `searchWorkflows`). State plainly that it cannot modify anything and should tell the user to switch to Build mode for changes.

`prompt_project_build.txt` covers the same plus the write tools (`createProject`, `updateProject`, `deleteProject`, `publishProject`, `createProjectWorkflow`, `updateWorkflow`, `deleteWorkflow`) and the two sub-agent tools. It MUST include:

- **Delegation rule:** for anything about the *content* of a workflow — tasks, triggers, conditions, loops — call `workflow_editor_agent` rather than writing definition JSON directly. Use `converter_agent` to import an n8n/Make/Zapier/Workato definition.
- **Confirm before destroying:** `deleteProject` and `deleteWorkflow` are irreversible; call `askUserQuestion` and get an explicit yes first.
- **After creating a workflow, tell the user where it is** — include the project and workflow names in the reply so the UI can link to it. (The listing page refreshes automatically after the turn.)
- A short section explaining that `parameters.projectId` and `parameters.intent` may be supplied by the caller, and that the instructions derived from them appear above.

- [ ] **Step 2: Write the failing test**

`ProjectAgentConfigurationTest` — a bean-wiring test in the style of the existing config tests in this package. Read one first (e.g. `McpServerToolCallbackContributorConfigurationTest`) for the mocking idiom. Assert:

```java
    @Test
    void testAskAgentUsesReadToolsAndBuildAgentUsesWriteTools() {
        // ask agent id is project_ask; build agent id is project_build
        // ask tool names contain listProjects and NOT createProject
        // build tool names contain createProject AND workflow_editor_agent
    }
```

Resolve tool names via `agent.getToolCallbacks()` (or whatever the `SpringAIAgent` surface exposes — read `DataTableSpringAIAgent`'s parent before writing). If the agent does not expose its callbacks, assert on what the configuration method returns instead and say so in the report rather than asserting nothing.

- [ ] **Step 3: Run the test to verify it fails**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-phase2 && ./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:test --tests "com.bytechef.ai.copilot.config.ProjectAgentConfigurationTest" > /tmp/p2-t2-red.log 2>&1; echo "exit=$?"; grep -E "BUILD FAILED|error:|cannot find symbol" /tmp/p2-t2-red.log | head -6
```

Expected: compilation failure — `ProjectAgentConfiguration` does not exist.

- [ ] **Step 4: Create the configuration**

Model it on `DataTableAgentConfiguration` — field-level `@Value` for the two prompts, a private `state`, private `wrapToolCallbacks`/`readPrompt` helpers, and the same `@ConditionalOnExpression("${bytechef.ai.copilot.enabled:false} or ${bytechef.ai.hub.enabled:false}")` gate.

Four beans:

```java
    @Bean
    ProjectSpringAIAgent projectAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.PROJECT.name() + "_" + Mode.ASK.name();

        return ProjectSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptProjectAskResource))
            .state(state)
            .toolCallbacks(wrapTools(securityContextRehydrator, List.of(readProjectTools, readProjectWorkflowTools)))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }
```

and a BUILD counterpart taking `ProjectTools projectTools, ProjectWorkflowTools projectWorkflowTools`, the two sub-agent providers, and appending the delegate tool callbacks:

```java
    @Bean
    ProjectSpringAIAgent projectBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ProjectTools projectTools,
        ProjectWorkflowTools projectWorkflowTools, SecurityContextRehydrator securityContextRehydrator,
        @Qualifier("workflowEditorBuildSubAgentChatClient") ObjectProvider<ChatClient> workflowEditorProvider,
        @Qualifier("converterBuildSubAgentChatClientSupplier") //
        ObjectProvider<Supplier<ChatClient>> converterSupplierProvider,
        @Qualifier("codeWorkflowBuildSubAgentChatClient") ObjectProvider<ChatClient> codeWorkflowProvider,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.PROJECT.name() + "_" + Mode.BUILD.name();

        List<ToolCallback> toolCallbacks =
            new ArrayList<>(wrapTools(securityContextRehydrator, List.of(projectTools, projectWorkflowTools)));

        workflowEditorProvider.ifAvailable(
            chatClient -> toolCallbacks.add(new WorkflowEditorAgentToolCallback(chatClient)));
        converterSupplierProvider.ifAvailable(
            supplier -> toolCallbacks.add(new ConverterAgentToolCallback(supplier)));
        codeWorkflowProvider.ifAvailable(
            chatClient -> toolCallbacks.add(new CodeWorkflowAgentToolCallback(chatClient)));

        return ProjectSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptProjectBuildResource))
            .state(state)
            .toolCallbacks(toolCallbacks)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }
```

**`CodeWorkflowAgentToolCallback` is EE.** `ai-copilot-service` is CE and cannot import it. Two options — pick one and say which in your report: (a) drop the code-workflow delegate from this task entirely and note it as a follow-up for the EE module to contribute, or (b) accept an `ObjectProvider<ToolCallback>` qualified by bean name so the EE module can register the callback itself. **Prefer (a)** — it keeps this task CE-clean, and the spec only requires workflow-editor and converter delegation. Do not add an EE dependency to a CE module.

Also add the two sub-agent `ChatClient` beans (`projectAskSubAgentChatClient`, `projectBuildSubAgentChatClient`) mirroring `DataTableAgentConfiguration`'s, so AI Hub and the MCP surface can consume this slice later.

- [ ] **Step 5: Run the test to verify it passes, then commit**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-phase2 && ./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:test > /tmp/p2-t2-green.log 2>&1; echo "exit=$?"; grep -E "BUILD SUCCESSFUL|BUILD FAILED|^> Task .* FAILED" /tmp/p2-t2-green.log
```

Then module-scoped `spotlessApply`, stage `server/libs/ai/ai-copilot`, and commit:

```
--- Register the project copilot ask and build agents
```

---

### Task 3: Skills agent reads intent

**Files:**
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/agent/SkillsSpringAIAgent.java`
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_skills_build.txt`
- Test: `server/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ai/copilot/agent/SkillsSpringAIAgentTest.java`

**Interfaces:** consumes nothing from earlier tasks; the client Task 6 supplies `{intent: 'create_skill'}`.

- [ ] **Step 1: Write the failing test**

Same shape as Task 1's test: with `parameters` carrying `intent: "create_skill"`, the system message contains a sentence instructing the agent to open by asking what the skill should do; without it, that sentence is absent.

- [ ] **Step 2: Run it to verify it fails**, then

- [ ] **Step 3: Add the explicit read**

Add a `createIntentMessage(State state)` private method to `SkillsSpringAIAgent` mirroring Task 1's, handling only `intent == "create_skill"`, and interpolate it into the existing `message` format string as a new `%s` between `resolvedMessage` and `ADDITIONAL_RULES`.

**Do not** remove or alter `ADDITIONAL_RULES`, even though its `state.workflowExecutionError` clause is nonsense for a skills agent — that is pre-existing copy-paste debt across three agents and cleaning it up here would be unreviewable scope creep. Note it in your report.

- [ ] **Step 4: Document the marker in `prompt_skills_build.txt`**

Add a short section near "Autonomous Generation Mode" explaining that when the caller supplies `parameters.intent = "create_skill"`, the instruction derived from it appears in the system message above, and the agent should open by asking what the skill should do rather than waiting for a full brief.

- [ ] **Step 5: Green, format, commit**

```
--- Make the skills copilot agent aware of a create-skill intent
```

---

### Task 4: Controller dispatch cleanup

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-rest/src/main/java/com/bytechef/ee/ai/copilot/web/rest/CopilotApiController.java`
- Test: `server/ee/libs/ai/ai-copilot/ai-copilot-rest/src/test/java/com/bytechef/ee/ai/copilot/web/rest/CopilotApiControllerTest.java` (create if absent — this module may have no test source set; if so, add the four `testImplementation` dependencies as phase 0's Task 4 did for `embedded-ai-copilot`)

**Interfaces:** consumes `Source.PROJECT` (Task 1) — the new source must route with no controller edit after this change.

- [ ] **Step 1: Write the failing test**

Assert three behaviours:
1. `chat("project", state with mode=BUILD)` resolves the agent id `project_build` — i.e. a source with NO explicit branch routes correctly.
2. `chat("converter", ...)` resolves `converter_build` regardless of mode (the one special case).
3. An agent id that resolves to no registered `LocalAgent` produces a client error rather than a `NullPointerException` from `agUiService.runAgent(null, ...)`.

The controller currently takes its collaborators via constructor, so construct it directly with mocks and a `List<LocalAgent>` of fakes rather than standing up Spring.

- [ ] **Step 2: Run to verify it fails**, then

- [ ] **Step 3: Replace the if/else chain**

Delete every branch except `converter`, and replace with:

```java
        if (agentId.equals("converter")) {
            agentId = "converter_build";
        } else {
            agentId = agentId + "_" + Mode.valueOf((String) mode)
                .name()
                .toLowerCase();
        }

        LocalAgent localAgent = localAgentMap.get(agentId);

        if (localAgent == null) {
            throw new IllegalArgumentException("No agent registered for id " + agentId);
        }

        return this.agUiService.runAgent(localAgent, agUiParameters);
```

**Verify before deleting:** every branch in the current chain is mechanically `<agentId>_<mode>` except `converter`. Read the whole chain and confirm — if any branch maps to something that is NOT `agentId + "_" + mode`, keep that branch and say so in your report. This is the one step where a wrong assumption silently breaks a working surface.

Check what exception type this codebase maps to a 400 — grep for an existing `@ExceptionHandler` or a `ResponseStatusException` usage in the REST modules, and use the same. `IllegalArgumentException` above is a placeholder; replace it with whatever actually yields a 4xx here.

- [ ] **Step 4: Green, format, commit**

```
--- Route copilot chat agents by convention instead of a branch per source
```

---

### Task 5: `useOpenCopilot` hook and the Projects listing page

**Files:**
- Create: `client/src/shared/components/copilot/hooks/useOpenCopilot.ts`
- Modify: `client/src/shared/components/copilot/CopilotButton.tsx`
- Modify: `client/src/pages/automation/projects/Projects.tsx`
- Test: `client/src/shared/components/copilot/hooks/tests/useOpenCopilot.test.ts`

**Interfaces:**
- Produces: `useOpenCopilot()` returning `(options: {source: Source; mode?: MODE; parameters?: Record<string, any>}) => void`. Task 6's three menu items consume it.

- [ ] **Step 1: Write the failing test** — the hook sets a FRESH context (no spread of prior state) and opens the panel; defaults are `MODE.ASK` and `{}`.

- [ ] **Step 2: Verify red**, then

- [ ] **Step 3: Create the hook and refactor `CopilotButton` to consume it**

The hook holds exactly what `CopilotButton.handleClick` does today. `CopilotButton` keeps its own `ai.copilot.enabled` gate and early return — the hook is about *opening*, not about *whether a trigger should render*. Menu items that are already inside a feature-gated dropdown call the hook directly.

- [ ] **Step 4: Wire the Projects page** — `<CopilotButton source={Source.PROJECT} />` in the header, plus a post-turn `useEffect` invalidating the projects list and the workflows queries. Read `DataTables.tsx` in this worktree for the established shape, and check whether the Projects header has the same `(length > 0 || !isLoading)` empty-state ternary — if so, apply the same rework so the button survives an empty list.

- [ ] **Step 5: `npm run format`, `npm run check`, commit**

```
--- client - Add Copilot to the projects listing page
```

---

### Task 6: Menu items open Copilot; delete the dialogs

**Files:**
- Modify: `client/src/pages/automation/projects/components/project-list/ProjectListItem.tsx` (~line 369-378)
- Modify: `client/src/pages/automation/projects/components/project-workflow-list/ProjectWorkflowList.tsx` (~line 204-207)
- Modify: `client/src/pages/automation/ai/skills/components/AiSkillsCreateDropdown.tsx` (~line 50, 89)
- Delete: `client/src/shared/components/workflow/GenerateWorkflowDialog.tsx`
- Delete: `client/src/pages/automation/ai/skills/components/AiSkillGenerateDialog.tsx`
- Modify: the copilot store — add a composer placeholder field (see below)

**Interfaces:** consumes `useOpenCopilot` (Task 5), `Source.PROJECT` (Task 1).

- [ ] **Step 1: Add the composer placeholder**

The dialogs had a textarea with guiding placeholder text; the Copilot composer is generic, so without this the menu items drop the user into a blank chat — less guided than today. Add an optional `composerPlaceholder` to `useCopilotStore`'s context (or a sibling field), set it from `useOpenCopilot`'s options, and have the composer in `CopilotPanelImpl` use it when present, falling back to today's text. **Read the composer component first** — if assistant-ui does not expose a placeholder prop, report that and fall back to leaving the composer unchanged rather than forcing it.

- [ ] **Step 2: Repoint the three menu items**

Each keeps its label, icon and position. Only `onClick` changes:

```tsx
    const openCopilot = useOpenCopilot();
```

- `ProjectListItem.tsx` and `ProjectWorkflowList.tsx`: `openCopilot({mode: MODE.BUILD, parameters: {intent: 'generate_workflow', projectId: project.id}, source: Source.PROJECT})`
- `AiSkillsCreateDropdown.tsx`: `openCopilot({mode: MODE.BUILD, parameters: {intent: 'create_skill'}, source: Source.SKILLS})`

Keep `event.stopPropagation()` in `ProjectListItem` — the menu item sits inside a clickable row.

- [ ] **Step 3: Delete the dialogs and their dead state**

Remove both dialog components, their `showGenerateWorkflowDialog` / dialog-open `useState` declarations, their JSX render blocks, and any now-unused imports and mutation wrappers. **Do NOT delete the server generate endpoints or the generated GraphQL/REST mutation hooks** — they have separate history and possible embedded consumers. Only the client dialog wrappers go.

Verify nothing else imports them:

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-phase2/client && grep -rn "GenerateWorkflowDialog\|AiSkillGenerateDialog" src || echo "no remaining references"
```

- [ ] **Step 4: `npm run format`, `npm run check`, commit**

```
--- client - Open Copilot from the generate-with-AI menu items instead of a dialog
```

---

## Phase verification

Server: one combined run over the touched modules. Client: `npm run check`.

Then a manual pass — this phase genuinely needs it, because its whole point is a conversation:

1. Projects page → sparkles → panel opens in ASK, scoped to no project. Ask "what projects do I have" and confirm it lists them.
2. Project row menu → "Generate with AI" → panel opens in BUILD. Confirm the agent **opens by asking what the workflow should do** rather than sitting silent, and that it knows which project it is in.
3. Answer it. Confirm a workflow is created in the right project, the listing refreshes without a reload, and the reply names the workflow so the user can find it.
4. Skills → "Create With AI" → same, for a skill.
5. Confirm the two dialogs are gone and nothing else opened them.
