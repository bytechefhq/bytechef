# AI Hub Port, Plan A: `0_p_1570` Prerequisites Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Land on `0_p_1570` everything the AI Hub needs that the Copilot and the management MCP share: one workflow-authoring path for the MCP, the artifact recorder SPI, the sub-agent question relay, and one picker list for Copilot and hub.

**Architecture:** Three groups of commits on branch `0_p_1570`, each independently green. D0 (ticket `5539`) swaps the management MCP onto `ProjectWorkflowLifecycleTools`, pins the single authoring path with a reflection test, and adds three inert CE seams. D13 (ticket `1570`) ports the CE ask seam into `IntelligentToolCatalog` and places its EE relay beside the Copilot. D14 (ticket `1570`) extracts the Copilot's eight picker callbacks into a shared factory and ports the shared client pieces. Plan B (the hub itself, on `0_p_ai_hub`) is written after this plan lands.

**Tech Stack:** Java 25 / Spring Boot 4 / Spring AI (`ToolCallback`, `ChatClient`), JUnit 5 + Mockito + AssertJ, Gradle; React 19 / TypeScript / Vitest for the client.

**Spec:** `docs/superpowers/specs/2026-10-17-ai-hub-phase1-port-design.md` (D0, D13, D14, "Where things land"). Source of every ported file: branch `0_732`, working tree at `/Volumes/Data/bytechef/bytechef` (referred to as SOURCE below). Read the SOURCE file before porting; copy, then trim to what the task names.

## Global Constraints

- Work in a worktree on branch `0_p_1570` (Task 0). Never touch `0_p_ai_hub` in this plan.
- Commit tickets: D0 tasks `5539`, D13 and D14 tasks `1570`. Server: `<ticket> <desc>`; client: `<ticket> client - <desc>`. No `Co-Authored-By` trailer, no attribution footer.
- Files under `server/ee/` carry the ByteChef Enterprise license header and `@version ee`; everything else Apache 2.0.
- No explanatory inline comments; the why goes in the commit message. No em dashes in prose or messages.
- Checkstyle: test method names camelCase without underscores, including private helpers; no `TODO:`; no empty blocks.
- Verification is `./gradlew <module>:check` written to a log file, exit code checked on its own line, then `grep '^> Task .* FAILED' <log>`; never pipe Gradle into `tail`/`grep`. Client: `cd client && npm run check`.
- Keep this line's extras (`ScopedProjectGuard`, `ScopedConnectionGuard`, `AllowedComponentNamesUtils`, `awaitingAnswer`, `LookupPropertyTracker`); never port a SOURCE deletion of them.
- Ported SOURCE Javadoc may stay; ported SOURCE `//` rationale comments are dropped.

## Review Focus

1. `createProjectWorkflow` over the MCP with a full `definition` argument (old contract): the tool now takes `name` + optional `description`; the MCP client gets a schema error, not a silently empty workflow. Pinned by Task 2's tool-definition assertion.
2. `WorkflowArtifactRecorder` throwing from inside `updateWorkflow`: the workflow update must still succeed and the failure be logged only. Pinned in Task 4.
3. A delegate asking twice in one delegation: the second `askUserQuestion` returns a tool error and the first question is what the parent relays. Pinned in Task 8.
4. The specialist's ask naming a `lookupRequired` property: must not be rejected with "call `selectPropertyOption`" (the delegate has no such tool). Pinned in Task 9.
5. A tool result that is prose starting with `{` but is not JSON (for example a code sample): `readPayloadKind` returns `undefined` with no console warning and the result is left to the generic fallback. Pinned in Task 11.

---

### Task 0: Worktree for `0_p_1570`

**Files:** none in the repo.

- [ ] **Step 1: Create the worktree**

Run from `/Volumes/Data/bytechef/bytechef-master`:
```bash
git worktree add /Volumes/Data/bytechef/bytechef-worktrees/p1570 0_p_1570
```
Expected: `HEAD is now at a5e04295fdd 1570 client - Paint outline badge borders in their fill color` (or the current tip of `0_p_1570`).

- [ ] **Step 2: Confirm the baseline compiles**

Run in the new worktree: `./gradlew :server:libs:automation:automation-ai:automation-ai-tool:compileJava :server:libs:ai:ai-mcp:ai-mcp-server:compileJava > /tmp/p1570-baseline.log 2>&1; echo "EXIT=$?"` on two lines (the `echo` on its own line after checking `$?`).
Expected: `EXIT=0`.

All later tasks run in `/Volumes/Data/bytechef/bytechef-worktrees/p1570`.

---

### Task 1: `ProjectWorkflowLifecycleTools` (D0, ticket 5539)

**Files:**
- Create: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ProjectWorkflowLifecycleTools.java` (from SOURCE, same path)
- Test: `server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/ProjectWorkflowLifecycleToolsTest.java` (from SOURCE, same path, 95 lines)

**Interfaces:**
- Produces: `@Component public class ProjectWorkflowLifecycleTools` with `@Tool` methods `getWorkflow(String workflowId, ToolContext)`, `listWorkflows(long projectId, ToolContext)`, `searchWorkflows(String query, Long projectId, Long workspaceId, ToolContext)`, `createProjectWorkflow(long projectId, String name, @Nullable String description, ToolContext)`, `deleteWorkflow(String workflowId, ToolContext)`, `saveWorkflowTestConnection(String workflowId, String workflowNodeName, String connectionKey, long connectionId, ToolContext)`; package-private `static String emptyDefinition(String name, @Nullable String description)`.

- [ ] **Step 1: Port the test**

Copy SOURCE's `ProjectWorkflowLifecycleToolsTest`. Its two tests are `testExposesNoToolThatWritesAWorkflowDefinition` (reflects over `@Tool` methods: none named `updateWorkflow`, and `createProjectWorkflow` has no parameter named `definition`) and `testCreateProjectWorkflowDelegatesWithAnEmptyDefinition` (mocks `ProjectWorkflowTools`, calls `createProjectWorkflow(7L, "Daily digest", null, toolContext)`, captures the `definition` string passed to the delegate and asserts it parses to `label == "Daily digest"`, `tasks` empty, one trigger of type `manual/v1/manual` named `trigger_1`). Adapt the delegate verifications to this line's signatures: `getWorkflow(workflowId, toolContext)`, `listWorkflows(projectId, toolContext)`, `deleteWorkflow(workflowId, toolContext)`.

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-tool:test --tests '*ProjectWorkflowLifecycleToolsTest' > /tmp/t1.log 2>&1; echo "EXIT=$?"`
Expected: `EXIT=1`, compilation error `cannot find symbol ProjectWorkflowLifecycleTools`.

- [ ] **Step 3: Port the class**

Copy SOURCE's `ProjectWorkflowLifecycleTools.java`. Three call sites change to this line's delegate signatures: `delegate.getWorkflow(workflowId, toolContext)`, `delegate.listWorkflows(projectId, toolContext)`, `delegate.deleteWorkflow(workflowId, toolContext)`; add the `ToolContext toolContext` parameter to those three `@Tool` methods. Keep `emptyDefinition` and every `@Tool` description verbatim.

- [ ] **Step 4: Run the test**

Same command as Step 2. Expected: `EXIT=0`.

- [ ] **Step 5: Commit**

```bash
git add server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ProjectWorkflowLifecycleTools.java server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/ProjectWorkflowLifecycleToolsTest.java
git commit -m "5539 Add ProjectWorkflowLifecycleTools, the workflow tools without updateWorkflow"
```

---

### Task 2: Management MCP server moves to the lifecycle tools (D0, ticket 5539)

**Files:**
- Modify: `server/libs/ai/ai-mcp/ai-mcp-server/src/main/java/com/bytechef/ai/mcp/server/config/ManagementMcpServerConfiguration.java:22,59-72,93,102-110,146-148`
- Test: `server/libs/ai/ai-mcp/ai-mcp-server/src/test/java/com/bytechef/ai/mcp/server/config/ManagementMcpServerConfigurationIntTest.java:26,53`

**Interfaces:**
- Consumes: `ProjectWorkflowLifecycleTools` (Task 1).
- Produces: `ManagementMcpServerConfiguration` constructor takes `ProjectWorkflowLifecycleTools projectWorkflowLifecycleTools` in place of `ProjectWorkflowTools`; `toolCallbackProvider()` exposes no `updateWorkflow`.

- [ ] **Step 1: Extend the IntTest**

In `ManagementMcpServerConfigurationIntTest`, replace the `ProjectWorkflowTools` mock bean (line 53) with `.withBean(ProjectWorkflowLifecycleTools.class, () -> mock(ProjectWorkflowLifecycleTools.class))` and fix the import. Add to the existing tool-names test (around line 98-110):

```java
assertThat(toolNames)
    .contains("createProjectWorkflow", "deleteWorkflow", "saveWorkflowTestConnection")
    .doesNotContain("updateWorkflow");
```

and a new test `testCreateProjectWorkflowTakesNameNotDefinition` that finds the `createProjectWorkflow` `ToolDefinition` and asserts `inputSchema()` contains `"name"` and does not contain `"definition"`.

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:libs:ai:ai-mcp:ai-mcp-server:testIntegration --tests '*ManagementMcpServerConfigurationIntTest' > /tmp/t2.log 2>&1; echo "EXIT=$?"`
Expected: `EXIT=1` (context fails: no `ProjectWorkflowTools` bean, or `doesNotContain("updateWorkflow")` fails).

- [ ] **Step 3: Switch the configuration**

In `ManagementMcpServerConfiguration`: import `ProjectWorkflowLifecycleTools` instead of `ProjectWorkflowTools`; field, constructor parameter and assignment become `projectWorkflowLifecycleTools`; the `tools` list in `toolCallbackProvider()` uses it. In `INSTRUCTIONS`, after "To build a workflow: createProject (if needed) → createProjectWorkflow → buildWorkflow with the workflowId and a plain-language instruction." insert: `createProjectWorkflow creates an EMPTY workflow (manual trigger, no tasks) from a name and description; there is no tool that writes a workflow definition directly - buildWorkflow and importWorkflow are the only ways to author content.`

- [ ] **Step 4: Run the IntTest**

Same command as Step 2. Expected: `EXIT=0`.

- [ ] **Step 5: Commit**

```bash
git add server/libs/ai/ai-mcp/ai-mcp-server/src/main/java/com/bytechef/ai/mcp/server/config/ManagementMcpServerConfiguration.java server/libs/ai/ai-mcp/ai-mcp-server/src/test/java/com/bytechef/ai/mcp/server/config/ManagementMcpServerConfigurationIntTest.java
git commit -m "5539 Author workflow content over the management MCP only through buildWorkflow and importWorkflow"
```

Commit body (one paragraph): raw `updateWorkflow` and the definition-taking `createProjectWorkflow` leave the MCP door so the MCP, the Copilot and the AI Hub produce the same workflow for the same ask.

---

### Task 3: `WorkflowContentSinglePathTest` (D0, ticket 5539)

**Files:**
- Create: `server/apps/server-app/src/test/java/com/bytechef/server/config/WorkflowContentSinglePathTest.java` (from SOURCE, same path)

**Interfaces:**
- Produces: `BUILD_WORKFLOW_SURFACES` list that Plan B extends with the hub configuration.

- [ ] **Step 1: Port the test with this line's surfaces**

Copy SOURCE's test. `BUILD_WORKFLOW_SURFACES = List.of(ManagementMcpServerConfiguration.class)` (drop `ProjectAgentConfiguration` and `AiHubConfiguration`, which do not exist here). Keep `testNoBuildWorkflowSurfaceInjectsProjectWorkflowTools` and `takesProjectWorkflowTools` verbatim. Confirm `server-app`'s test classpath sees `ai-mcp-server` (it is a runtime dependency of the app; if the compile fails on the import, add `testImplementation(project(":server:libs:ai:ai-mcp:ai-mcp-server"))` to `server/apps/server-app/build.gradle.kts`).

- [ ] **Step 2: Run it**

Run: `./gradlew :server:apps:server-app:test --tests '*WorkflowContentSinglePathTest' > /tmp/t3.log 2>&1; echo "EXIT=$?"`
Expected: `EXIT=0` (Task 2 already removed the offender; this test now guards it).

- [ ] **Step 3: Prove it bites**

Temporarily add a `ProjectWorkflowTools` parameter to any method of `ManagementMcpServerConfiguration`, rerun Step 2, expect `EXIT=1` with the offender named; revert the change.

- [ ] **Step 4: Commit**

```bash
git add server/apps/server-app/src/test/java/com/bytechef/server/config/WorkflowContentSinglePathTest.java
git commit -m "5539 Pin that no buildWorkflow surface injects ProjectWorkflowTools"
```

---

### Task 4: Artifact recorder SPI and the `ProjectWorkflowTools` hook (D0, ticket 5539)

**Files:**
- Create: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/WorkflowArtifactRecorder.java` (SOURCE, 50 lines)
- Create: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ToolArtifactRecorder.java` (SOURCE, 67 lines)
- Modify: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ProjectWorkflowTools.java:90-100,264,346,501-547`
- Test: `server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/ProjectWorkflowToolsTest.java`

**Interfaces:**
- Produces: `interface WorkflowArtifactRecorder { void recordWorkflowArtifact(@Nullable ToolContext toolContext, boolean created, String workflowId, long projectId, @Nullable Long projectWorkflowId, String workflowName); }`; `interface ToolArtifactRecorder` with `record(threadId, userId, artifactKind, artifactId, artifactName[, metadata])`, `recordWorkflowReference(threadId, userId, workflowId, projectId, projectWorkflowId, workflowName)`, `recordReference(threadId, userId, artifactKind, artifactId, artifactName)` exactly as SOURCE. `ProjectWorkflowTools` constructor gains a trailing `ObjectProvider<WorkflowArtifactRecorder> workflowArtifactRecorderProvider` parameter (before `WorkspaceFacade`, matching SOURCE's order).

- [ ] **Step 1: Write the failing tests**

In `ProjectWorkflowToolsTest` (update its `new ProjectWorkflowTools(...)` to pass an `ObjectProvider<WorkflowArtifactRecorder>`; use a small `fixedObjectProvider(recorder)` helper or `mock(ObjectProvider.class)` with `getIfAvailable()` stubbed), add:

```java
@Test
void testCreateProjectWorkflowRecordsCreatedArtifact() {
    // arrange a facade returning a ProjectWorkflowDTO with workflowId "wf-1", projectWorkflowId 11L
    projectWorkflowTools.createProjectWorkflow(5L, definitionJson, toolContext);

    verify(workflowArtifactRecorder).recordWorkflowArtifact(toolContext, true, "wf-1", 5L, 11L, "Daily digest");
}

@Test
void testUpdateWorkflowRecordsUpdatedArtifact() {
    projectWorkflowTools.updateWorkflow("wf-1", definitionJson, 1, toolContext);

    verify(workflowArtifactRecorder).recordWorkflowArtifact(toolContext, false, "wf-1", 5L, 11L, "Daily digest");
}

@Test
void testRecorderFailureDoesNotFailUpdateWorkflow() {
    doThrow(new IllegalStateException("db down")).when(workflowArtifactRecorder)
        .recordWorkflowArtifact(any(), anyBoolean(), any(), anyLong(), any(), any());

    WorkflowInfo workflowInfo = projectWorkflowTools.updateWorkflow("wf-1", definitionJson, 1, toolContext);

    assertThat(workflowInfo).isNotNull();
}

@Test
void testNoRecorderBeanIsIgnored() {
    // provider.getIfAvailable() returns null
    assertThatCode(() -> projectWorkflowTools.updateWorkflow("wf-1", definitionJson, 1, toolContext))
        .doesNotThrowAnyException();
}
```

Use the existing test's facade stubs for `projectWorkflowFacade.getProjectWorkflow` / `getWorkflowProjectId` / `updateWorkflow`.

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-tool:test --tests '*ProjectWorkflowToolsTest' > /tmp/t4.log 2>&1; echo "EXIT=$?"`
Expected: `EXIT=1`, compilation error on the new constructor parameter / missing `WorkflowArtifactRecorder`.

- [ ] **Step 3: Port the two interfaces and add the hook**

Copy both SOURCE interfaces unchanged. In `ProjectWorkflowTools`: add the field and constructor parameter; port SOURCE's `private void recordWorkflowArtifact(@Nullable ToolContext, boolean created, String workflowId, long projectId, @Nullable Long projectWorkflowId, String workflowName)` (gets the recorder via `getIfAvailable()`, returns if null, wraps the call in `try/catch (RuntimeException)` logging `warn`); call it right after `capturePersistedWorkflow(...)` at line 264 (`created = true`) and inside `captureUpdatedWorkflow` after its `capturePersistedWorkflow(...)` (`created = false`), passing the same arguments.

- [ ] **Step 4: Run the tests**

Same command as Step 2. Expected: `EXIT=0`.

- [ ] **Step 5: Commit**

```bash
git add server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/WorkflowArtifactRecorder.java server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ToolArtifactRecorder.java server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ProjectWorkflowTools.java server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/ProjectWorkflowToolsTest.java
git commit -m "5539 Let an optional WorkflowArtifactRecorder observe workflow creates and updates"
```

---

### Task 5: Context keys and `DelegatingToolCallback` (D0, ticket 5539)

**Files:**
- Modify: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/AutomationToolInvocationContext.java:34-36`
- Create: `server/libs/modules/components/ai/llm/src/main/java/com/bytechef/component/ai/llm/tool/DelegatingToolCallback.java` (SOURCE, 53 lines)
- Test: `server/libs/modules/components/ai/llm/src/test/java/com/bytechef/component/ai/llm/tool/DelegatingToolCallbackTest.java` (new)

**Interfaces:**
- Produces: constants `TOOL_CONTEXT_USER_ID_KEY = "bytechef.automationTool.userId"`, `TOOL_CONTEXT_SOURCE_ORDINAL_KEY = "bytechef.automationTool.sourceOrdinal"`, `TOOL_CONTEXT_LAST_USER_PROMPT_KEY = "bytechef.automationTool.lastUserPrompt"`, `TOOL_CONTEXT_THREAD_ID_KEY = "bytechef.automationTool.threadId"` on `AutomationToolInvocationContext`; `interface DelegatingToolCallback extends ToolCallback { ToolCallback getDelegate(); static ToolCallback unwrap(ToolCallback); }`.

- [ ] **Step 1: Write the failing test**

```java
class DelegatingToolCallbackTest {

    @Test
    void testUnwrapWalksNestedDelegates() {
        ToolCallback inner = mock(ToolCallback.class);
        DelegatingToolCallback middle = delegating(inner);
        DelegatingToolCallback outer = delegating(middle);

        assertThat(DelegatingToolCallback.unwrap(outer)).isSameAs(inner);
    }

    @Test
    void testUnwrapReturnsPlainCallbackUnchanged() {
        ToolCallback plain = mock(ToolCallback.class);

        assertThat(DelegatingToolCallback.unwrap(plain)).isSameAs(plain);
    }
}
```
`delegating(delegate)` is a private helper returning an anonymous `DelegatingToolCallback` whose `getDelegate()` returns the argument and whose `getToolDefinition()`/`call()` forward to it.

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:libs:modules:components:ai:llm:test --tests '*DelegatingToolCallbackTest' > /tmp/t5.log 2>&1; echo "EXIT=$?"`
Expected: `EXIT=1`, `cannot find symbol DelegatingToolCallback`.

- [ ] **Step 3: Port the interface and add the constants**

Copy SOURCE's `DelegatingToolCallback.java`. Add the four `public static final String` constants to `AutomationToolInvocationContext` directly under `TOOL_CONTEXT_ENVIRONMENT_ID_KEY`, values as in Interfaces. The record's components do not change.

- [ ] **Step 4: Run the test and the context module's tests**

Run: `./gradlew :server:libs:modules:components:ai:llm:test --tests '*DelegatingToolCallbackTest' :server:libs:automation:automation-ai:automation-ai-tool:test > /tmp/t5.log 2>&1; echo "EXIT=$?"`
Expected: `EXIT=0`.

- [ ] **Step 5: Commit (two commits)**

```bash
git add server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/AutomationToolInvocationContext.java
git commit -m "5539 Name the user, source, last prompt and thread tool-context keys"
git add server/libs/modules/components/ai/llm/src/main/java/com/bytechef/component/ai/llm/tool/DelegatingToolCallback.java server/libs/modules/components/ai/llm/src/test/java/com/bytechef/component/ai/llm/tool/DelegatingToolCallbackTest.java
git commit -m "5539 Add DelegatingToolCallback so wrappers can be unwrapped to their delegate"
```

---

### Task 6: CE ask seam (D13, ticket 1570)

**Files:**
- Create (from SOURCE, same paths, package `com.bytechef.ai.copilot.tool.ask` in `server/libs/ai/ai-copilot/ai-copilot-tool`): `SubAgentAskRelay.java` (39), `SubAgentAskRelayToolCallback.java` (131), `SubAgentPendingQuestionSignal.java` (73), `SubAgentQuestionFormatter.java` (102), `SubAgentQuestionRenderer.java` (25)
- Modify: `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/AskUserQuestionToolCallback.java:49` (`static final String KIND` → `public static final String KIND`)
- Test (from SOURCE): `.../ai-copilot-tool/src/test/java/com/bytechef/ai/copilot/tool/ask/SubAgentAskRelayToolCallbackTest.java` (234), `SubAgentQuestionFormatterTest.java` (168)

**Interfaces:**
- Produces: `interface SubAgentAskRelay { ToolCallback askUserQuestionToolCallback(); <T> AskOutcome<T> runWithChannel(Supplier<T>); record AskOutcome<T>(@Nullable T result, @Nullable String pendingQuestion) {} }`; `final class SubAgentAskRelayToolCallback implements ToolCallback` with constructor `(ToolCallback delegate, SubAgentAskRelay relay, SubAgentQuestionRenderer renderer)`; `enum SubAgentQuestionRenderer { JSON, PLAIN_TEXT }`; `SubAgentQuestionFormatter.format(String toolName, String pendingQuestionJson)`; `SubAgentPendingQuestionSignal.withSignal(Map)`, `.markPending(ToolContext)`, `.isPending(Map)`; `AskUserQuestionToolCallback.KIND` public.

- [ ] **Step 1: Port the two tests**

Copy both SOURCE tests unchanged. They build a fake `SubAgentAskRelay` inline; no EE dependency.

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:test --tests 'com.bytechef.ai.copilot.tool.ask.*' > /tmp/t6.log 2>&1; echo "EXIT=$?"`
Expected: `EXIT=1`, package `com.bytechef.ai.copilot.tool.ask` does not exist.

- [ ] **Step 3: Port the five classes and make `KIND` public**

Copy the five SOURCE files unchanged (they depend only on Spring AI, `ToolErrors`, `JsonUtils`, SLF4J). Change `KIND`'s visibility.

- [ ] **Step 4: Run the module's tests**

Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:test > /tmp/t6.log 2>&1; echo "EXIT=$?"`
Expected: `EXIT=0`.

- [ ] **Step 5: Commit**

```bash
git add server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/ask server/libs/ai/ai-copilot/ai-copilot-tool/src/test/java/com/bytechef/ai/copilot/tool/ask server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/AskUserQuestionToolCallback.java
git commit -m "1570 Add the sub-agent ask relay seam"
```

---

### Task 7: `askCapable` and the catalog's ask attachment (D13, ticket 1570)

**Files:**
- Modify: `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/catalog/IntelligentToolDefinition.java` (add `default boolean askCapable() { return true; }`)
- Modify: `.../catalog/SimpleIntelligentToolDefinition.java:37-41` (add a 6-arg constructor with trailing `boolean askCapable`; the 5-arg one delegates with `true`; override `askCapable()`; keep `AgentType agentType`, do NOT port SOURCE's `agentTypeKey` rename)
- Modify: `.../catalog/IntelligentToolCatalog.java:37,44-56,71-100`
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/CopilotIntelligentToolContributorConfiguration.java` (the `importWorkflow` definition passes `false`)
- Test: `.../ai-copilot-tool/src/test/java/com/bytechef/ai/copilot/tool/catalog/IntelligentToolCatalogTest.java` (extend), `server/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ai/copilot/config/IntelligentToolAskCapabilityTest.java` (new)

**Interfaces:**
- Consumes: Task 6.
- Produces: `IntelligentToolCatalog` constructors `(ObjectProvider<IntelligentToolContributor>)` (kept, relay null) and `@Autowired (ObjectProvider<IntelligentToolContributor>, ObjectProvider<SubAgentAskRelay>)`; `getByNames(names, variant, chatClientDecorator, callbackDecorator)` (kept, `JSON` renderer) and the 5-arg overload with trailing `SubAgentQuestionRenderer questionRenderer`; `getForPanel` unchanged (`JSON`); `public List<String> getNames()`.

- [ ] **Step 1: Write the failing tests**

In `IntelligentToolCatalogTest` add (modelled on its existing fixture at line ~232 that builds a catalog from one contributor):

```java
@Test
void testWithoutRelayDelegateIsNotWrapped() {
    IntelligentToolCatalog catalog = new IntelligentToolCatalog(fixedObjectProvider(contributor));

    ToolCallback toolCallback = single(catalog.getByNames(Set.of("buildWorkflow"), BUILD, identity(), identity()));

    assertThat(toolCallback).isNotInstanceOf(SubAgentAskRelayToolCallback.class);
}

@Test
void testWithRelayAskCapableDelegateIsWrappedAndGetsAskTool() {
    SubAgentAskRelay relay = fakeRelay();
    IntelligentToolCatalog catalog = new IntelligentToolCatalog(fixedObjectProvider(contributor), fixedObjectProvider(relay));

    ToolCallback toolCallback = single(catalog.getByNames(Set.of("buildWorkflow"), BUILD, identity(), identity()));

    assertThat(toolCallback).isInstanceOf(SubAgentAskRelayToolCallback.class);
    // the chatClientDecorator receives a ChatClient whose default tools contain relay.askUserQuestionToolCallback()
}

@Test
void testWithRelayNonAskCapableDelegateIsLeftAlone() {
    // contributor whose definition has askCapable=false
    assertThat(toolCallback).isNotInstanceOf(SubAgentAskRelayToolCallback.class);
}

@Test
void testGetNamesListsEveryDefinition() {
    assertThat(catalog.getNames()).containsExactlyInAnyOrder("buildWorkflow", "importWorkflow");
}
```
For the default-tools assertion, do what SOURCE's `IntelligentToolCatalogTest` does (lines 165-175 and 372-380): the fixture's `rawChatClientFactory` returns a mocked `ChatClient` whose `mutate().defaultTools(any(Object[].class)).build()` chain is stubbed; assert `verify(builder).defaultTools(relay.askUserQuestionToolCallback())` when the relay is present and `verify(chatClient, never()).mutate()` when it is absent or the definition is not ask-capable. Copy SOURCE's `fakeRelay()` helper from that test.

New `IntelligentToolAskCapabilityTest` in `ai-copilot-service` tests (the slice of SOURCE's `IntelligentToolSurfaceParityTest` that is about asking):

```java
@Test
void testOnlyImportWorkflowIsNotAskCapable() {
    List<IntelligentToolDefinition> definitions = contributor().definitions();

    assertThat(definitions).filteredOn(definition -> !definition.askCapable())
        .extracting(IntelligentToolDefinition::name)
        .containsExactly("importWorkflow");
}

@Test
void testConverterPromptStillForbidsAsking() throws IOException {
    String prompt = read("prompt_converter_build.txt");

    assertThat(prompt).contains("You must NEVER ask the user any questions");
}
```
`contributor()` instantiates `CopilotIntelligentToolContributorConfiguration` the way `CopilotIntelligentToolContributorConfigurationTest` does (reuse its fixture).

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:test --tests '*IntelligentToolCatalogTest' :server:libs:ai:ai-copilot:ai-copilot-service:test --tests '*IntelligentToolAskCapabilityTest' > /tmp/t7.log 2>&1; echo "EXIT=$?"`
Expected: `EXIT=1`, compilation errors (`askCapable`, two-arg constructor, `getNames`).

- [ ] **Step 3: Implement**

Apply SOURCE's `IntelligentToolCatalog` diff (the `askRelay` field, the three constructors, the 5-arg `getByNames`, `getNames()`, `buildToolCallbacks`/`buildToolCallback` taking `questionRenderer`, `withAskTool`, `withAskRelay`), keeping this line's `AgentType agentType()` on the definition. `SimpleIntelligentToolDefinition`: 6-arg constructor with `boolean askCapable`, 5-arg delegates with `true`, `askCapable()` override. `CopilotIntelligentToolContributorConfiguration`: the `importWorkflow` `SimpleIntelligentToolDefinition` gets the trailing `false`.

- [ ] **Step 4: Run the tests and both modules' checks**

Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:check :server:libs:ai:ai-copilot:ai-copilot-service:check > /tmp/t7.log 2>&1; echo "EXIT=$?"`, then `grep '^> Task .* FAILED' /tmp/t7.log`.
Expected: `EXIT=0`, no matches.

- [ ] **Step 5: Commit**

```bash
git add server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/catalog server/libs/ai/ai-copilot/ai-copilot-tool/src/test/java/com/bytechef/ai/copilot/tool/catalog/IntelligentToolCatalogTest.java server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/CopilotIntelligentToolContributorConfiguration.java server/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ai/copilot/config/IntelligentToolAskCapabilityTest.java
git commit -m "1570 Make every ask-capable catalog delegate able to put a question to the user"
```

---

### Task 8: EE relay beside the Copilot (D13, ticket 1570)

**Files:**
- Create (from SOURCE `server/ee/libs/ai/ai-hub/ai-hub-service/.../ee/ai/hub/subagent/`, relocated to `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/subagent/`): `SubagentAskChannel.java` (87), `SubagentAskChannelRelay.java` (56), `SubagentAskUserQuestionToolCallback.java` (153)
- Test (from SOURCE `.../ee/ai/hub/subagent/`, relocated to `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/subagent/`): `SubagentAskChannelTest.java` (106), `SubagentAskUserQuestionToolCallbackTest.java` (179), `SubagentAskRelayEndToEndTest.java` (175)
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-service/build.gradle.kts` only if a dependency is missing (it already has `ai-copilot-tool`; add `commons-util` is present; add `:server:libs:ai:ai-api` for `ToolErrors` if not transitively available)

**Interfaces:**
- Consumes: Task 6's seam, `AskUserQuestionToolCallback(ToolStateVisibilityMetrics)`, `ToolStateVisibilityMetrics.NOOP`.
- Produces: `@Component @ConditionalOnEEVersion class SubagentAskChannelRelay implements SubAgentAskRelay` (package `com.bytechef.ee.ai.copilot.subagent`); `SubagentAskChannel.runWithChannel(Supplier<T>)`, `.offer(String)`, `.pending()`; `SubagentAskUserQuestionToolCallback()` no-arg constructor.

- [ ] **Step 1: Port the three tests into the new package**

Copy and change only the `package` line. `SubagentAskThenAnswerContinuityTest` is NOT ported (it depends on hub session memory). Add to `SubagentAskUserQuestionToolCallbackTest`:

```java
@Test
void testSecondQuestionInOneDelegationIsRefused() {
    SubagentAskChannel.runWithChannel(() -> {
        callback.call(validQuestionJson, toolContext);

        String second = callback.call(validQuestionJson, toolContext);

        assertThat(second).contains("A question is already pending for this delegation");
        assertThat(SubagentAskChannel.pending()).isEqualTo(firstEnvelope());

        return null;
    });
}
```

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests 'com.bytechef.ee.ai.copilot.subagent.*' > /tmp/t8.log 2>&1; echo "EXIT=$?"`
Expected: `EXIT=1`, package does not exist.

- [ ] **Step 3: Port the three classes**

Copy, change the package, keep the EE header and `@version ee`. `SubagentAskUserQuestionToolCallback` constructs `new AskUserQuestionToolCallback(ToolStateVisibilityMetrics.NOOP)` as on SOURCE.

- [ ] **Step 4: Run the module check**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:check > /tmp/t8.log 2>&1; echo "EXIT=$?"`, then `grep '^> Task .* FAILED' /tmp/t8.log`.
Expected: `EXIT=0`, no matches.

- [ ] **Step 5: Boot check**

Run: `./gradlew :server:apps:server-app:testIntegration --tests '*ServerApplicationIntTest' > /tmp/t8b.log 2>&1; echo "EXIT=$?"`
Expected: `EXIT=0` (the EE context starts with exactly one `SubAgentAskRelay` bean).

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/subagent server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/subagent server/ee/libs/ai/ai-copilot/ai-copilot-service/build.gradle.kts
git commit -m "1570 Relay a delegated specialist's question to the user through the parent turn"
```

---

### Task 9: MCP renderers, the tracker exemption and the sub-agent prompt (D13, ticket 1570)

**Files:**
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/ToolCallbackContributorConfiguration.java:66-70` (5-arg `getByNames` with `SubAgentQuestionRenderer.PLAIN_TEXT`)
- Modify: `server/ee/libs/embedded/embedded-ai/embedded-ai-copilot/src/main/java/com/bytechef/ee/embedded/ai/copilot/config/EmbeddedCopilotMcpContributorConfiguration.java:34-36` (same)
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/subagent/SubagentAskUserQuestionToolCallback.java` (`call`: hand the delegate a `ToolContext` whose map omits `LookupPropertyTracker.TOOL_CONTEXT_LOOKUP_PROPERTY_TRACKER_KEY`)
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_workflow_editor_build_sub_agent.txt` (append a section)
- Test: `.../ai-copilot-service/src/test/java/com/bytechef/ai/copilot/config/ToolCallbackContributorConfigurationTest.java`, `.../embedded-ai-copilot/src/test/java/.../EmbeddedCopilotMcpContributorConfigurationTest.java`, `.../ee/ai/copilot/subagent/SubagentAskUserQuestionToolCallbackTest.java`

- [ ] **Step 1: Write the failing tests**

Contributor tests: where each test builds the catalog, use a `mock(IntelligentToolCatalog.class)` and `verify(catalog).getByNames(eq(INTELLIGENT_TOOL_NAMES), eq(BUILD), any(), any(), eq(SubAgentQuestionRenderer.PLAIN_TEXT))`.

Tracker exemption:
```java
@Test
void testSpecialistAskIgnoresTheLookupPropertyTracker() {
    ToolContext toolContext = toolContextWithUnresolvedLookup("channel");

    SubagentAskChannel.runWithChannel(() -> {
        String result = callback.call(questionMentioning("channel"), toolContext);

        assertThat(result).doesNotContain("Call selectPropertyOption");
        assertThat(SubagentAskChannel.pending()).isNotNull();

        return null;
    });
}
```
`toolContextWithUnresolvedLookup(propertyName)` installs the tracker (`LookupPropertyTracker.install(map)`) and seeds it through `LookupPropertyTracker.record(toolContext, "lookupActionPropertyOptions", input, result)` with the same input/result JSON this line's `AskUserQuestionToolCallbackTest` uses for its own rejection test (copy that fixture; it is the one place the shape is already right).

Prompt: in `IntelligentToolAskCapabilityTest` (Task 7) add `testSubAgentPromptSaysItHasNoConnectionPickers` asserting the resource contains `no selectConnection` and `askUserQuestion`.

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:test --tests '*ToolCallbackContributorConfigurationTest' --tests '*IntelligentToolAskCapabilityTest' :server:ee:libs:embedded:embedded-ai:embedded-ai-copilot:test --tests '*EmbeddedCopilotMcpContributorConfigurationTest' :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests '*SubagentAskUserQuestionToolCallbackTest' > /tmp/t9.log 2>&1; echo "EXIT=$?"`
Expected: `EXIT=1`.

- [ ] **Step 3: Implement**

Contributors: switch to the 5-arg `getByNames` with `PLAIN_TEXT`. Exemption: in `SubagentAskUserQuestionToolCallback.call`, if `toolContext` is non-null and its map contains the tracker key, build `new ToolContext(mapWithoutTrackerKey)` for the delegate call; otherwise pass it through. Prompt: append to `prompt_workflow_editor_build_sub_agent.txt`:

```
## Connections and lookup values

You have no selectConnection, createConnection, listConnectionsForComponent, selectPropertyOption or
selectTriggerPropertyOption tools: the calling agent binds connections and resolves lookup values after you
return. Leave connections unbound and never write connection ids into the definition. Use
lookupComponentPropertyOptions to read a property's valid values; when the choice is genuinely the user's,
ask ONE question with askUserQuestion and stop. End your report with any lookupRequired parameters you left
empty so the calling agent can resolve them.
```

- [ ] **Step 4: Run the four modules' checks**

Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:check :server:ee:libs:ai:ai-copilot:ai-copilot-service:check :server:ee:libs:embedded:embedded-ai:embedded-ai-copilot:check > /tmp/t9.log 2>&1; echo "EXIT=$?"`, then `grep '^> Task .* FAILED' /tmp/t9.log`.
Expected: `EXIT=0`, no matches.

- [ ] **Step 5: Commit (two commits)**

```bash
git add server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/ToolCallbackContributorConfiguration.java server/ee/libs/embedded/embedded-ai/embedded-ai-copilot/src/main/java/com/bytechef/ee/embedded/ai/copilot/config/EmbeddedCopilotMcpContributorConfiguration.java server/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ai/copilot/config/ToolCallbackContributorConfigurationTest.java server/ee/libs/embedded/embedded-ai/embedded-ai-copilot/src/test
git commit -m "1570 Render a specialist's question as plain text over the management MCP"
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/subagent/SubagentAskUserQuestionToolCallback.java server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/subagent/SubagentAskUserQuestionToolCallbackTest.java server/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_workflow_editor_build_sub_agent.txt server/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ai/copilot/config/IntelligentToolAskCapabilityTest.java
git commit -m "1570 Let the buildWorkflow specialist ask about lookup values it cannot pick itself"
```

---

### Task 10: One picker list for Copilot and hub (D14, ticket 1570)

**Files:**
- Create: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/InteractivePickerToolCallbacks.java`
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/CopilotConfiguration.java:167-175,198-204,420-450,471,503`
- Test: `.../ai-copilot-service/src/test/java/com/bytechef/ai/copilot/config/InteractivePickerToolCallbacksTest.java` (new), `CopilotConfigurationTest.java:67` (constructor call)

**Interfaces:**
- Produces: `@Component public class InteractivePickerToolCallbacks` with constructor `(ConnectionDefinitionService, WorkspaceConnectionFacade, ComponentDefinitionService, ActionDefinitionService, ActionDefinitionFacade, TriggerDefinitionService, TriggerDefinitionFacade, PropertyOptionsResolver, ObjectProvider<CopilotConnectionLister>, ScopedConnectionGuard)` and methods `List<ToolCallback> read()` (= `listConnectionsForComponent`, `lookupActionPropertyOptions`, `lookupTriggerPropertyOptions`, `askUserQuestion`) and `List<ToolCallback> write()` (= all eight, in today's order: list, selectConnection, lookupAction, lookupTrigger, selectPropertyOption, selectTriggerPropertyOption, askUserQuestion, createConnection). Each call builds fresh instances (callbacks are stateless; a list per call keeps today's semantics). `CopilotConfiguration` constructor takes `InteractivePickerToolCallbacks interactivePickerToolCallbacks` in place of the eight collaborators that only served `interactivePickerToolCallbacks()`; keep any of them that other beans still use.

- [ ] **Step 1: Write the failing test**

```java
class InteractivePickerToolCallbacksTest {

    @Test
    void testWriteExposesTheEightPickersInOrder() {
        assertThat(names(pickers.write())).containsExactly(
            "listConnectionsForComponent", "selectConnection", "lookupActionPropertyOptions",
            "lookupTriggerPropertyOptions", "selectPropertyOption", "selectTriggerPropertyOption",
            "askUserQuestion", "createConnection");
    }

    @Test
    void testReadExposesOnlyNonMutatingPickers() {
        assertThat(names(pickers.read())).containsExactly(
            "listConnectionsForComponent", "lookupActionPropertyOptions", "lookupTriggerPropertyOptions",
            "askUserQuestion");
    }

    @Test
    void testEmbeddedListerIsIncludedWhenPresent() {
        // provider returns a lister: ListConnectionsForComponentToolCallback is built with two listers
    }
}
```
`names(list)` maps `getToolDefinition().name()`. Build `pickers` with Mockito mocks for every collaborator and `fixedObjectProvider(null)` / `fixedObjectProvider(lister)`.

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:test --tests '*InteractivePickerToolCallbacksTest' > /tmp/t10.log 2>&1; echo "EXIT=$?"`
Expected: `EXIT=1`, class missing.

- [ ] **Step 3: Extract**

Move the body of `CopilotConfiguration#interactivePickerToolCallbacks()` into `InteractivePickerToolCallbacks.write()`; implement `read()` by building only the four read callbacks (same constructors). Replace the two `tools.addAll(interactivePickerToolCallbacks())` call sites with `tools.addAll(interactivePickerToolCallbacks.write())`; delete the private method and the now-unused fields/constructor parameters; update `CopilotConfigurationTest`'s constructor call (pass `new InteractivePickerToolCallbacks(...)` built from its existing mocks). Gate the component on nothing (it is plain and cheap); `CopilotConfiguration` remains gated on `bytechef.ai.copilot.enabled`.

- [ ] **Step 4: Run the module check**

Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:check > /tmp/t10.log 2>&1; echo "EXIT=$?"`, then `grep '^> Task .* FAILED' /tmp/t10.log`.
Expected: `EXIT=0`, no matches.

- [ ] **Step 5: Commit**

```bash
git add server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/InteractivePickerToolCallbacks.java server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/CopilotConfiguration.java server/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ai/copilot/config/InteractivePickerToolCallbacksTest.java server/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ai/copilot/config/CopilotConfigurationTest.java
git commit -m "1570 Build the interactive picker tools once so every chat surface shares them"
```

---

### Task 11: Shared client pieces (D14, ticket 1570, client)

**Files:**
- Modify: `client/src/shared/components/ai-chat/messages/toToolResultDataPart.ts:128-152`
- Modify: `client/src/shared/components/ai-chat/messages/SelectConnectionMessage.tsx` (filter)
- Modify: `client/src/shared/components/ai-chat/stores/useAiChatToolCallStore.ts` (rename)
- Create: `client/src/shared/error/useReportQueryError.ts` (SOURCE, verbatim)
- Test: `client/src/shared/components/ai-chat/messages/tests/toToolResultDataPart.test.ts` (extend), `client/src/shared/components/ai-chat/messages/tests/SelectConnectionMessage.test.tsx` (new), `client/src/shared/components/ai-chat/stores/tests/useAiChatToolCallStore.test.ts` (rename), `client/src/shared/error/tests/useReportQueryError.test.ts` (new)

**Interfaces:**
- Produces: `toToolResultDataPart` maps any tool whose result payload has `kind: 'ask-user-question'` to `data-ask-user-question`; `readPayloadKind(eventContent): string | undefined` (module-private); `useAiChatToolCallStore` methods `resetForChat(chatId)`, `failRunningInChat(chatId, ...)` and field `chatId` replace the `*Task` / `taskId` names; `useReportQueryError(action: string, error: Error | null)` and `reportMutationError(action: string, error: Error)`.

- [ ] **Step 1: Write the failing tests**

`toToolResultDataPart.test.ts`:
```ts
it('maps a delegate tool result whose payload kind is ask-user-question', () => {
    const result = toToolResultDataPart('buildWorkflow', JSON.stringify({awaitingAnswer: true, kind: 'ask-user-question', questions: [question]}));

    expect(result).toMatchObject({ok: true, type: 'data-ask-user-question'});
});

it('leaves a brace-prefixed non-JSON result to the generic fallback without warning', () => {
    const warn = vi.spyOn(console, 'warn');

    expect(toToolResultDataPart('someTool', '{not json')).toBeUndefined();
    expect(warn).not.toHaveBeenCalled();
});

it('ignores a JSON result with an unknown kind', () => {
    expect(toToolResultDataPart('someTool', JSON.stringify({kind: 'something-else'}))).toBeUndefined();
});
```
`SelectConnectionMessage.test.tsx`: render with connections `[{id: 3, name: 'Slack prod'}, {id: -1, name: 'OpenAI (provider)'}]` (mock the connections query the component uses) and assert the select offers `Slack prod` and not `OpenAI (provider)`.
`useAiChatToolCallStore.test.ts`: rename every `resetForTask`/`failRunningInTask`/`taskId` to the `chat` names.
`useReportQueryError.test.ts`: `toast.error` called once for the same error across re-renders, called again for a new message, not called when `error` is null (mock `sonner`).

- [ ] **Step 2: Run them to verify they fail**

Run: `cd client && npx vitest run src/shared/components/ai-chat src/shared/error`
Expected: failures on the new assertions and the renamed methods.

- [ ] **Step 3: Implement**

`toToolResultDataPart.ts`: extract the existing `askUserQuestion` branch into `toAskUserQuestionDataPart(eventContent, toolName)` (SOURCE lines 223-243; use `toolName` in the error object), add `readPayloadKind` (SOURCE lines 250-268) and the `fallbackKind === 'ask-user-question'` branch before the final `return undefined`. Do not add the `tool-approval-request` branch (Plan B). `SelectConnectionMessage.tsx`: add SOURCE's `selectableConnections` memo (`id != null && id > 0`) and use it for the options. Store: rename (methods, field, types). Copy `useReportQueryError.ts`.

- [ ] **Step 4: Full client check**

Run: `cd client && npm run format && npm run check`
Expected: lint, typecheck and tests pass.

- [ ] **Step 5: Commit (three commits)**

```bash
git add client/src/shared/components/ai-chat/messages/toToolResultDataPart.ts client/src/shared/components/ai-chat/messages/tests/toToolResultDataPart.test.ts
git commit -m "1570 client - Render a delegate's question by its payload kind, not its tool name"
git add client/src/shared/components/ai-chat/messages/SelectConnectionMessage.tsx client/src/shared/components/ai-chat/messages/tests/SelectConnectionMessage.test.tsx
git commit -m "1570 client - Hide virtual AI-provider connections from the connection picker"
git add client/src/shared/components/ai-chat/stores client/src/shared/error
git commit -m "1570 client - Key the tool-call store by chat and add useReportQueryError"
```

---

### Task 12: Whole-branch verification and rebase of `0_p_ai_hub`

**Files:** none.

- [ ] **Step 1: Server check across touched modules**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-tool:check :server:libs:ai:ai-mcp:ai-mcp-server:check :server:libs:modules:components:ai:llm:check :server:libs:ai:ai-copilot:ai-copilot-tool:check :server:libs:ai:ai-copilot:ai-copilot-service:check :server:ee:libs:ai:ai-copilot:ai-copilot-service:check :server:ee:libs:embedded:embedded-ai:embedded-ai-copilot:check :server:apps:server-app:test --continue > /tmp/planA-check.log 2>&1; echo "EXIT=$?"`, then `grep '^> Task .* FAILED' /tmp/planA-check.log`.
Expected: `EXIT=0`, no matches. Then `./gradlew :server:libs:ai:ai-mcp:ai-mcp-server:testIntegration` the same way (needs Docker; see memory note on `DOCKER_HOST` for OrbStack).

- [ ] **Step 2: Compile everything once**

Run: `./gradlew compileJava compileTestJava --continue > /tmp/planA-compile.log 2>&1; echo "EXIT=$?"`
Expected: `EXIT=0`.

- [ ] **Step 3: Rebase the hub branch**

Run from `/Volumes/Data/bytechef/bytechef-worktrees/ai-hub`: `git rebase 0_p_1570` (the branch holds only the design doc, so no conflicts). Expected: `Successfully rebased`.

- [ ] **Step 4: Report**

Do not push. List the 14 commits (`git log --oneline a5e04295fdd..0_p_1570`) in the hand-back.
