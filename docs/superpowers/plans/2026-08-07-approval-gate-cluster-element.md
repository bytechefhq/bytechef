# Approval Gate Cluster Element Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the hidden `requiresApproval: true` tool parameter with an explicit `approvalGate` cluster element that owns the tools it gates and the channels it delivers to.

**Architecture:** `approvalGate` becomes a `MultipleConnectionsToolCallbackProviderFunction` on the `aiAgentUtils` component, declaring `TOOLS` and `APPROVAL_CHANNELS` children. It builds its children's callbacks through a newly extracted shared collaborator and returns them pre-wrapped in `ApprovalGateToolCallback`, so the agent module loses its gate special case entirely and `APPROVAL_CHANNELS` comes off the AI Agent root. The shared collaborator and `AiAgentToolFacade` both land in `ai/llm`, which removes `agent-utils`' dependency on the `agent` component module.

**Tech Stack:** Java 25, Spring Boot 4, Spring AI, Gradle (Kotlin DSL), JUnit 5, AssertJ, Mockito; React 19 + TypeScript 5.9 + Vitest on the client.

**Spec:** `docs/superpowers/specs/2026-08-07-approval-gate-cluster-element-design.md`

## Global Constraints

- Branch is `0_732`. Never amend or force-push — the user commits in parallel. Always make fresh commits.
- Commit message convention: server changes `0 <description>`; client changes `0 client - <description>`.
- Every commit message ends with:
  ```
  Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
  Claude-Session: https://claude.ai/code/session_01K4Y64sa7FQTZoefx6Yd5RK
  ```
- Run `./gradlew spotlessApply` before every server commit. Run `npm run check` from `client/` before every client commit.
- **Never judge a Gradle run piped into `tail`/`grep`.** Redirect to a file, check `$?` on its own line, then grep the file for `^> Task .* FAILED`. Use `--continue`.
- Stage only files this task touched. Do not `git add -A`.
- Component definition snapshots regenerate by deleting the JSON from **both** `src/test/resources/definition/` and `build/resources/test/definition/`, then running the module's tests twice — the first run writes the file, the second compares against it.
- Snapshot filenames are not the component name: the aiAgentUtils snapshot is `ai_agent-utils_v1.json` and the aiAgent one is `ai-agent_v1.json`. Confirm with `ls` before deleting.
- `JsonFileAssert` compares JSON **semantically**, so hand-editing a snapshot to add or remove keys is valid and keeps diffs small. Prefer that over wholesale regeneration, which reformats every line (the committed files use Jackson 2 spacing, `"key": v`; the current toolchain emits `"key" : v`).
- Java style: one blank line before control statements; one blank line between a variable modification and the statement using it; no blank line before a class's closing brace; no `_` prefix on private methods; descriptive variable names (no `e`, `u`, `o`).
- Test class names end in `Test` (unit) or `IntTest` (integration). Test method names are camelCase with no underscores.
- Client: object keys sorted alphabetically (`sort-keys`), named imports sorted within `{}`, interface names end in `I` or `Props`, `useRef` variables end in `Ref`, Lucide icons imported with the `Icon` suffix, `twMerge` for class merging (never `cn()`).

---

## File Structure

**Created**

| File | Responsibility |
|---|---|
| `server/libs/modules/components/ai/llm/src/main/java/com/bytechef/component/ai/llm/facade/AiAgentToolFacade.java` | Moved verbatim from `ai/agent`; builds `FunctionToolCallback`s from a cluster element |
| `server/libs/modules/components/ai/llm/src/main/java/com/bytechef/component/ai/llm/tool/ClusterElementToolCallbacks.java` | The one place that answers "what shapes does a TOOLS cluster element come in, and how do I get its callbacks?" |
| `server/libs/modules/components/ai/llm/src/test/java/com/bytechef/component/ai/llm/tool/ClusterElementToolCallbacksTest.java` | Pins all four dispatch shapes |
| `server/libs/modules/components/ai/agent/utils/src/main/java/com/bytechef/component/ai/agent/utils/cluster/AiAgentUtilsApprovalGate.java` | The gate cluster element; wraps its TOOLS children with its own channels and expiry |
| `server/libs/modules/components/ai/agent/utils/src/main/java/com/bytechef/component/ai/agent/utils/cluster/ApprovalGateToolCallback.java` | Moved from `ai/agent` |
| `server/libs/modules/components/ai/agent/utils/src/test/java/com/bytechef/component/ai/agent/utils/cluster/AiAgentUtilsApprovalGateTest.java` | Gate wrapping, rejections |

**Modified**

| File | Change |
|---|---|
| `ai/llm/build.gradle.kts` | Add `evaluator-api`, `platform-component-api`, `platform-configuration-api`, `spring-context` |
| `ai/agent/build.gradle.kts` | Shared deps into a `subprojects` block |
| `ai/agent/utils/build.gradle.kts` | Delete `components:ai:agent` |
| `AbstractAiAgentChatAction.java` | Delegate to the collaborator; delete the gate branch, the `approvalChannelClusterElements` parameter, `getApprovalExpiry`, `isSuspendingApprovalTool` |
| `AiAgentUtilsTaskTool.java` | Delegate `buildSubagentToolCallbacks`; reject gates under subagents |
| `AiAgentUtilsComponentHandler.java` | Register the gate; add `APPROVAL_CHANNELS`; map `approvalGate` |
| `AiAgentComponentDefinition.java` | Remove `APPROVAL_CHANNELS` |
| `ToolConstants.java` | Delete `REQUIRES_APPROVAL` |
| `WorkflowValidator.java` | Per-gate channel scan |
| `useAiAgentTools.ts`, `AiAgentTools.tsx`, `AiAgentTool.tsx`, `AiAgentToolDropdownMenu.tsx`, `useAiAgentToolDropdownMenu.tsx` | Nested groups; drop the approval checkbox and expiry submenu |

**Deleted**

- `server/libs/modules/components/ai/agent/src/main/java/com/bytechef/component/ai/agent/facade/AiAgentToolFacade.java` (moved)
- `server/libs/modules/components/ai/agent/src/main/java/com/bytechef/component/ai/agent/tool/ApprovalGateToolCallback.java` (moved)

---

## Task 1: Move AiAgentToolFacade into ai/llm

Removes the component→component dependency. Nothing else changes behaviourally — this is a pure relocation, so the existing test suites are the regression check.

**Files:**
- Create: `server/libs/modules/components/ai/llm/src/main/java/com/bytechef/component/ai/llm/facade/AiAgentToolFacade.java`
- Delete: `server/libs/modules/components/ai/agent/src/main/java/com/bytechef/component/ai/agent/facade/AiAgentToolFacade.java`
- Modify: `server/libs/modules/components/ai/llm/build.gradle.kts`
- Modify: `server/libs/modules/components/ai/agent/build.gradle.kts`
- Modify: `server/libs/modules/components/ai/agent/utils/build.gradle.kts`

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces: `com.bytechef.component.ai.llm.facade.AiAgentToolFacade`, a `@Component` with the unchanged public surface:
  - `ToolCallback getFunctionToolCallback(ClusterElement clusterElement, @Nullable ComponentConnection componentConnection, boolean editorEnvironment)`
  - `ToolCallback getFunctionToolCallback(ClusterElement clusterElement, Map<String, ComponentConnection> componentConnections, boolean editorEnvironment)`

- [ ] **Step 1: Move the class with git so history follows it**

```bash
cd /Volumes/Data/bytechef/bytechef
mkdir -p server/libs/modules/components/ai/llm/src/main/java/com/bytechef/component/ai/llm/facade
git mv \
  server/libs/modules/components/ai/agent/src/main/java/com/bytechef/component/ai/agent/facade/AiAgentToolFacade.java \
  server/libs/modules/components/ai/llm/src/main/java/com/bytechef/component/ai/llm/facade/AiAgentToolFacade.java
```

- [ ] **Step 2: Rewrite the package declaration**

```bash
sed -i '' 's|^package com.bytechef.component.ai.agent.facade;|package com.bytechef.component.ai.llm.facade;|' \
  server/libs/modules/components/ai/llm/src/main/java/com/bytechef/component/ai/llm/facade/AiAgentToolFacade.java
```

- [ ] **Step 3: Repoint every import across the repo**

```bash
grep -rl "com\.bytechef\.component\.ai\.agent\.facade\.AiAgentToolFacade" \
  server/libs/modules/components/ai --include="*.java" \
  | xargs sed -i '' 's|com\.bytechef\.component\.ai\.agent\.facade\.AiAgentToolFacade|com.bytechef.component.ai.llm.facade.AiAgentToolFacade|g'
```

Then verify none remain (expect no output):

```bash
grep -rn "ai\.agent\.facade" server --include="*.java"
```

- [ ] **Step 4: Give ai/llm the facade's dependencies**

Edit `server/libs/modules/components/ai/llm/build.gradle.kts`. Insert these four lines into the existing `dependencies { }` block, keeping the current alphabetical grouping:

```kotlin
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:core:evaluator:evaluator-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
```

- [ ] **Step 5: Propagate ai:llm to the agent family**

A `subprojects` block does not configure the project that declares it, so `ai:llm` must appear in **both**
blocks: the top one for the agent module itself, the `subprojects` one for `utils`, `guardrails` and the
sixteen `chat-memory-*` modules. Replace the whole of
`server/libs/modules/components/ai/agent/build.gradle.kts` with:

```kotlin
dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:evaluator:evaluator-api"))

    implementation(project(":server:libs:modules:components:ai:llm"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-api"))
    implementation(project(":server:libs:platform:platform-tool-execution:platform-tool-execution-api"))
}

subprojects {
    dependencies {
        implementation(project(":server:libs:modules:components:ai:llm"))
    }
}
```

- [ ] **Step 6: Delete the component→component dependency**

In `server/libs/modules/components/ai/agent/utils/build.gradle.kts`, delete this line entirely (do not replace it — `ai:llm` now arrives via the `subprojects` block):

```kotlin
    implementation(project(":server:libs:modules:components:ai:agent"))
```

- [ ] **Step 7: Verify the dependency is gone and the build is green**

```bash
grep -rn "com\.bytechef\.component\.ai\.agent\." \
  server/libs/modules/components/ai/agent/utils/src \
  | grep -v "\.agent\.utils" || echo "CLEAN: no agent-module references remain"

grep -rn "components:ai:agent\"" server --include=build.gradle.kts || echo "CLEAN: no dependents"
```

Both must print their CLEAN message.

```bash
./gradlew :server:libs:modules:components:ai:llm:compileTestJava \
          :server:libs:modules:components:ai:agent:test \
          :server:libs:modules:components:ai:agent:utils:test \
          --continue > /tmp/t1.log 2>&1
echo "EXIT=$?"
grep -E "^> Task .* FAILED" /tmp/t1.log || echo "ALL GREEN"
```

Expected: `EXIT=0` and `ALL GREEN`.

- [ ] **Step 8: Commit**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1; echo "EXIT=$?"
git add server/libs/modules/components/ai/llm server/libs/modules/components/ai/agent
git commit -F - <<'MSG'
0 Move AiAgentToolFacade to the shared ai/llm module

agent-utils depended on the agent component module for exactly one symbol, in
both main and test sources: AiAgentToolFacade. ai/llm is the established home
for code AI components share -- it declares no ComponentHandler and is already
consumed by platform modules, EE modules and apps -- and the facade depends on
nothing from any component module.

Shared agent-family dependencies move into a subprojects block, since a plain
dependencies block does not reach child projects.

components:ai:agent now has no dependents anywhere in the repo.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01K4Y64sa7FQTZoefx6Yd5RK
MSG
```

---

## Task 2: Extract ClusterElementToolCallbacks and fix the subagent dispatch

This is a bug fix, not only de-duplication. `AiAgentUtilsTaskTool.buildSubagentToolCallbacks` omits the `ToolCallbackProviderFunction` branch, so ten of thirteen `aiAgentUtils` tools (`autoMemoryTool`, `agentClientTool`, `askUserQuestionTool`, `braveWebSearchTool`, `globTool`, `grepTool`, `fileSystemTools`, `listDirectoryTool`, `shellTools`, `todoWriteTool`) fall through to the facade, which builds a `FunctionToolCallback` from the element's parameters and never calls the provider's `apply()`. A subagent gets one malformed tool instead of the real ones.

**Files:**
- Create: `server/libs/modules/components/ai/llm/src/main/java/com/bytechef/component/ai/llm/tool/ClusterElementToolCallbacks.java`
- Create: `server/libs/modules/components/ai/llm/src/test/java/com/bytechef/component/ai/llm/tool/ClusterElementToolCallbacksTest.java`
- Modify: `server/libs/modules/components/ai/agent/src/main/java/com/bytechef/component/ai/agent/action/AbstractAiAgentChatAction.java`
- Modify: `server/libs/modules/components/ai/agent/utils/src/main/java/com/bytechef/component/ai/agent/utils/cluster/AiAgentUtilsTaskTool.java`

**Interfaces:**
- Consumes: `com.bytechef.component.ai.llm.facade.AiAgentToolFacade` (Task 1).
- Produces: `com.bytechef.component.ai.llm.tool.ClusterElementToolCallbacks` with
  `public List<ToolCallback> build(ClusterElement clusterElement, Map<String, ComponentConnection> componentConnections, boolean editorEnvironment, ActionContext context)`
  and constructor `ClusterElementToolCallbacks(AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService)`.
  Task 3 constructs it the same way.

- [ ] **Step 1: Write the failing test**

Create `server/libs/modules/components/ai/llm/src/test/java/com/bytechef/component/ai/llm/tool/ClusterElementToolCallbacksTest.java`:

```java
package com.bytechef.component.ai.llm.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolCallbackProviderFunction;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * @author Ivica Cardic
 */
class ClusterElementToolCallbacksTest {

    private final AiAgentToolFacade aiAgentToolFacade = mock(AiAgentToolFacade.class);
    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);
    private final ClusterElementToolCallbacks clusterElementToolCallbacks =
        new ClusterElementToolCallbacks(aiAgentToolFacade, clusterElementDefinitionService);

    /**
     * The branch AiAgentUtilsTaskTool omitted. Ten of the thirteen aiAgentUtils tool elements are
     * ToolCallbackProviderFunction, so without this branch a subagent received a facade-built
     * FunctionToolCallback instead of the provider's own callbacks.
     */
    @Test
    void testProviderFunctionElementReturnsTheProvidersCallbacks() {
        ToolCallback providerToolCallback = mock(ToolCallback.class);

        ToolCallbackProviderFunction toolCallbackProviderFunction =
            (inputParameters, connectionParameters, context) -> ToolCallbackProvider.from(providerToolCallback);

        when(clusterElementDefinitionService.getClusterElement(anyString(), anyInt(), anyString()))
            .thenReturn(toolCallbackProviderFunction);

        List<ToolCallback> toolCallbacks = clusterElementToolCallbacks.build(
            clusterElement("aiAgentUtils/v1/grepTool", "grepTool"), Map.of(), false, mock(ActionContext.class));

        assertThat(toolCallbacks).containsExactly(providerToolCallback);

        verifyNoInteractions(aiAgentToolFacade);
    }

    @Test
    void testMultipleConnectionsProviderFunctionElementReturnsTheProvidersCallbacks() {
        ToolCallback providerToolCallback = mock(ToolCallback.class);

        MultipleConnectionsToolCallbackProviderFunction providerFunction =
            (inputParameters, connectionParameters, extensions, componentConnections,
                context) -> ToolCallbackProvider.from(providerToolCallback);

        when(clusterElementDefinitionService.getClusterElement(anyString(), anyInt(), anyString()))
            .thenReturn(providerFunction);

        List<ToolCallback> toolCallbacks = clusterElementToolCallbacks.build(
            clusterElement("aiAgentUtils/v1/taskTool", "taskTool"), Map.of(), false, mock(ActionContext.class));

        assertThat(toolCallbacks).containsExactly(providerToolCallback);

        verifyNoInteractions(aiAgentToolFacade);
    }

    @Test
    void testPlainFunctionElementDelegatesToTheFacade() {
        ToolCallback facadeToolCallback = mock(ToolCallback.class);

        when(clusterElementDefinitionService.getClusterElement(anyString(), anyInt(), anyString()))
            .thenReturn(new Object());
        when(aiAgentToolFacade.getFunctionToolCallback(any(ClusterElement.class), any(), anyBoolean()))
            .thenReturn(facadeToolCallback);

        List<ToolCallback> toolCallbacks = clusterElementToolCallbacks.build(
            clusterElement("aiAgentUtils/v1/createAiSkill", "createAiSkill"), Map.of(), false,
            mock(ActionContext.class));

        assertThat(toolCallbacks).containsExactly(facadeToolCallback);
    }

    private static ClusterElement clusterElement(String type, String clusterElementName) {
        return new ClusterElement(
            "aiAgentUtils", 1, Map.of(), clusterElementName, type, Map.of(), clusterElementName + "_1");
    }
}
```

> If `ClusterElement`'s constructor argument order differs, read
> `server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/domain/ClusterElement.java`
> and copy the shape used in
> `server/libs/modules/components/ai/agent/utils/src/test/java/com/bytechef/component/ai/agent/utils/cluster/subagent/ByteChefSubagentExecutorTest.java:82-86`.

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :server:libs:modules:components:ai:llm:test --tests "*ClusterElementToolCallbacksTest*" \
  > /tmp/t2.log 2>&1; echo "EXIT=$?"
grep -E "error:|FAILED" /tmp/t2.log | head
```

Expected: compilation failure — `ClusterElementToolCallbacks` does not exist.

- [ ] **Step 3: Write the collaborator**

Create `server/libs/modules/components/ai/llm/src/main/java/com/bytechef/component/ai/llm/tool/ClusterElementToolCallbacks.java`. The body is `AbstractAiAgentChatAction.buildElementToolCallbacks` plus its two private helpers, verbatim:

```java
package com.bytechef.component.ai.llm.tool;

import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolCallbackProviderFunction;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolFunction;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;

/**
 * Builds the {@link ToolCallback}s a TOOLS cluster element contributes. A TOOLS element comes in four shapes and every
 * caller must handle all four; keeping the dispatch here means "what shapes are there?" has one answer instead of one
 * per call site. It previously had two, and the second silently omitted the {@link ToolCallbackProviderFunction}
 * branch.
 *
 * @author Ivica Cardic
 */
public class ClusterElementToolCallbacks {

    private static final Logger logger = LoggerFactory.getLogger(ClusterElementToolCallbacks.class);

    private final AiAgentToolFacade aiAgentToolFacade;
    private final ClusterElementDefinitionService clusterElementDefinitionService;

    @SuppressFBWarnings("EI")
    public ClusterElementToolCallbacks(
        AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService) {

        this.aiAgentToolFacade = aiAgentToolFacade;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    public List<ToolCallback> build(
        ClusterElement clusterElement, Map<String, ComponentConnection> componentConnections,
        boolean editorEnvironment, ActionContext context) {

        Object clusterElementFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        if (clusterElementFunction instanceof MultipleConnectionsToolCallbackProviderFunction providerFunction) {
            try {
                ToolCallback[] providerToolCallbacks = providerFunction
                    .apply(
                        ParametersFactory.create(clusterElement.getParameters()),
                        getConnectionParameters(componentConnections, clusterElement),
                        ParametersFactory.create(clusterElement.getExtensions()),
                        componentConnections, context)
                    .getToolCallbacks();

                return Arrays.asList(providerToolCallbacks);
            } catch (Exception exception) {
                throw initializationException(clusterElement, exception, context);
            }
        } else if (clusterElementFunction instanceof ToolCallbackProviderFunction toolCallbackProviderFunction) {
            try {
                ComponentConnection componentConnection = componentConnections.get(
                    clusterElement.getWorkflowNodeName());

                ToolCallback[] providerToolCallbacks = toolCallbackProviderFunction
                    .apply(
                        ParametersFactory.create(clusterElement.getParameters()),
                        ParametersFactory.create(componentConnection), context)
                    .getToolCallbacks();

                return Arrays.asList(providerToolCallbacks);
            } catch (Exception exception) {
                throw initializationException(clusterElement, exception, context);
            }
        } else if (clusterElementFunction instanceof MultipleConnectionsToolFunction) {
            return List.of(
                aiAgentToolFacade.getFunctionToolCallback(clusterElement, componentConnections, editorEnvironment));
        } else {
            ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

            return List.of(
                aiAgentToolFacade.getFunctionToolCallback(clusterElement, componentConnection, editorEnvironment));
        }
    }

    private static Parameters getConnectionParameters(
        Map<String, ComponentConnection> componentConnections, ClusterElement clusterElement) {

        ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

        return ParametersFactory.create(componentConnection);
    }

    private static RuntimeException initializationException(
        ClusterElement clusterElement, Throwable cause, ActionContext context) {

        Class<? extends Throwable> causeClass = cause.getClass();

        String message = String.format(
            "Failed to initialize tool callback for cluster element '%s' (component=%s v%d): %s",
            clusterElement.getClusterElementName(), clusterElement.getComponentName(),
            clusterElement.getComponentVersion(),
            cause.getMessage() == null ? causeClass.getSimpleName() : cause.getMessage());

        context.log(log -> log.error(message, cause));

        return new IllegalStateException(message, cause);
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
./gradlew :server:libs:modules:components:ai:llm:test --tests "*ClusterElementToolCallbacksTest*" \
  > /tmp/t2.log 2>&1; echo "EXIT=$?"
grep -E "^> Task .* FAILED" /tmp/t2.log || echo "GREEN"
```

Expected: `EXIT=0`, `GREEN`.

- [ ] **Step 5: Delegate from AbstractAiAgentChatAction**

In `AbstractAiAgentChatAction.java`:

1. Add the field and initialise it in the canonical constructor (the one all other overloads delegate to — the 4-arg private/protected one that assigns `this.aiAgentToolFacade`):

```java
    private final ClusterElementToolCallbacks clusterElementToolCallbacks;
```

```java
        this.clusterElementToolCallbacks =
            new ClusterElementToolCallbacks(aiAgentToolFacade, clusterElementDefinitionService);
```

2. Replace the body of `buildElementToolCallbacks` with a delegation, keeping the method as a thin private wrapper so its call sites are untouched:

```java
    private List<ToolCallback> buildElementToolCallbacks(
        ClusterElement clusterElement, Map<String, ComponentConnection> connectionParameters,
        boolean editorEnvironment, ActionContext context) {

        return clusterElementToolCallbacks.build(clusterElement, connectionParameters, editorEnvironment, context);
    }
```

3. Delete the now-unused private helpers `getConnectionParameters` and `clusterElementInitializationException` **only if** no other method calls them. Check first:

```bash
grep -n "getConnectionParameters\|clusterElementInitializationException" \
  server/libs/modules/components/ai/agent/src/main/java/com/bytechef/component/ai/agent/action/AbstractAiAgentChatAction.java
```

If either appears elsewhere (the advisor-building code also uses `clusterElementInitializationException`), keep it and leave it alone.

4. Add the import `com.bytechef.component.ai.llm.tool.ClusterElementToolCallbacks` and remove any imports left unused.

- [ ] **Step 6: Delegate from AiAgentUtilsTaskTool**

In `AiAgentUtilsTaskTool.java`, add a field initialised in the constructor:

```java
    private final ClusterElementToolCallbacks clusterElementToolCallbacks;
```

```java
        this.clusterElementToolCallbacks =
            new ClusterElementToolCallbacks(aiAgentToolFacade, clusterElementDefinitionService);
```

Replace the whole body of `buildSubagentToolCallbacks` with:

```java
    private List<ToolCallback> buildSubagentToolCallbacks(
        ClusterElement subagentClusterElement, Map<String, ComponentConnection> componentConnections,
        boolean editorEnvironment, Context context) {

        ClusterElementMap subagentClusterElementMap = ClusterElementMap.of(subagentClusterElement.getExtensions());

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (ClusterElement toolClusterElement : subagentClusterElementMap.getClusterElements(TOOLS)) {
            toolCallbacks.addAll(
                clusterElementToolCallbacks.build(
                    toolClusterElement, componentConnections, editorEnvironment, (ActionContext) context));
        }

        return toolCallbacks;
    }
```

Keep the existing Javadoc — the "built from this subagent's own extensions" paragraph is still the load-bearing invariant. Remove imports that are now unused (`ParametersFactory`, `ToolCallbackProvider`, `MultipleConnectionsToolCallbackProviderFunction`) only if nothing else in the file uses them; `ParametersFactory` is still used by `resolveChatModel`, so keep it.

- [ ] **Step 7: Run the full affected suites**

```bash
./gradlew :server:libs:modules:components:ai:llm:test \
          :server:libs:modules:components:ai:agent:test \
          :server:libs:modules:components:ai:agent:utils:test \
          --continue > /tmp/t2full.log 2>&1
echo "EXIT=$?"
grep -E "^> Task .* FAILED" /tmp/t2full.log || echo "ALL GREEN"
```

Expected: `EXIT=0`, `ALL GREEN`.

- [ ] **Step 8: Commit**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1; echo "EXIT=$?"
git add server/libs/modules/components/ai/llm server/libs/modules/components/ai/agent
git commit -F - <<'MSG'
0 Extract the cluster element tool-callback dispatch

A TOOLS cluster element comes in four shapes and every caller must handle all
four. The dispatch existed twice -- private in AbstractAiAgentChatAction and
copied into AiAgentUtilsTaskTool -- and the copies were not equivalent.

The task tool's version omitted the ToolCallbackProviderFunction branch, so ten
of the thirteen aiAgentUtils tool elements (grep, glob, shell, filesystem,
todoWrite, askUserQuestion, braveWebSearch, autoMemory, agentClient,
listDirectory) fell through to AiAgentToolFacade, which builds a
FunctionToolCallback from the element's parameters and never invokes the
provider. A subagent received one malformed tool with an empty schema instead of
the real ones, and failed quietly. It also passed empty connection parameters
where the agent path passes the resolved connection.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01K4Y64sa7FQTZoefx6Yd5RK
MSG
```

---

## Task 3: Add the approvalGate cluster element and delete the flag path

Adds the gate, moves `ApprovalGateToolCallback` into `agent-utils`, and removes the `requiresApproval` machinery plus `APPROVAL_CHANNELS` from the AI Agent root. These land together because the agent module's only reference to the gate class is inside the branch being deleted.

**Files:**
- Create: `.../ai/agent/utils/src/main/java/com/bytechef/component/ai/agent/utils/cluster/AiAgentUtilsApprovalGate.java`
- Move: `.../ai/agent/src/main/java/com/bytechef/component/ai/agent/tool/ApprovalGateToolCallback.java` → `.../ai/agent/utils/src/main/java/com/bytechef/component/ai/agent/utils/cluster/ApprovalGateToolCallback.java`
- Move: `.../ai/agent/src/test/java/com/bytechef/component/ai/agent/tool/ApprovalGateToolCallbackTest.java` → `.../ai/agent/utils/src/test/java/com/bytechef/component/ai/agent/utils/cluster/ApprovalGateToolCallbackTest.java`
- Create: `.../ai/agent/utils/src/test/java/com/bytechef/component/ai/agent/utils/cluster/AiAgentUtilsApprovalGateTest.java`
- Modify: `AiAgentUtilsComponentHandler.java`, `AiAgentUtilsTaskTool.java`, `AbstractAiAgentChatAction.java`, `AiAgentComponentDefinition.java`, `ToolConstants.java`
- Modify: `.../ai/agent/src/test/resources/definition/ai-agent_v1.json`, `.../ai/agent/utils/src/test/resources/definition/ai_agent-utils_v1.json`

**Interfaces:**
- Consumes: `ClusterElementToolCallbacks.build(...)` (Task 2).
- Produces: cluster element `aiAgentUtils/v1/approvalGate`, `MultipleConnectionsToolCallbackProviderFunction`, with properties `name` (string), `approvalExpiresIn` (integer), `approvalExpiresInUnit` (string: `HOURS` or `DAYS`). Task 4 matches on the cluster element name `approvalGate`; Task 5 reads the same three property names.

- [ ] **Step 1: Write the failing gate test**

Create `.../ai/agent/utils/src/test/java/com/bytechef/component/ai/agent/utils/cluster/AiAgentUtilsApprovalGateTest.java`:

```java
package com.bytechef.component.ai.agent.utils.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.component.definition.ClusterElementDefinition;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AiAgentUtilsApprovalGateTest {

    @Test
    void testGateIsATypeOfTool() {
        ClusterElementDefinition<?> clusterElementDefinition = AiAgentUtilsApprovalGate.CLUSTER_ELEMENT_DEFINITION;

        assertThat(clusterElementDefinition.getName()).isEqualTo("approvalGate");
        assertThat(clusterElementDefinition.getType()
            .name()).isEqualTo("TOOLS");
    }

    @Test
    void testGateDeclaresNameAndExpiryProperties() {
        ClusterElementDefinition<?> clusterElementDefinition = AiAgentUtilsApprovalGate.CLUSTER_ELEMENT_DEFINITION;

        assertThat(clusterElementDefinition.getProperties())
            .extracting("name")
            .containsExactly("name", "approvalExpiresIn", "approvalExpiresInUnit");
    }

    @Test
    void testRejectsTheSuspendingApprovalToolAsAChild() {
        assertThatThrownBy(
            () -> AiAgentUtilsApprovalGate.checkGatableChild("approval", "requestApproval"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requestApproval");
    }

    @Test
    void testRejectsANestedGate() {
        assertThatThrownBy(
            () -> AiAgentUtilsApprovalGate.checkGatableChild("aiAgentUtils", "approvalGate"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("approvalGate");
    }

    @Test
    void testAcceptsAnOrdinaryTool() {
        AiAgentUtilsApprovalGate.checkGatableChild("aiAgentUtils", "grepTool");
    }

    /**
     * Every callback a gated element contributes is wrapped exactly once. A tool element that yields two callbacks
     * must produce two gated callbacks, not one gate around a collection.
     */
    @Test
    void testWrapsEveryCallbackOfEveryGatedElementExactlyOnce() {
        ClusterElementToolCallbacks clusterElementToolCallbacks = mock(ClusterElementToolCallbacks.class);

        when(clusterElementToolCallbacks.build(any(), any(), anyBoolean(), any()))
            .thenReturn(List.of(mock(ToolCallback.class), mock(ToolCallback.class)));

        AiAgentUtilsApprovalGate approvalGate = new AiAgentUtilsApprovalGate(
            clusterElementToolCallbacks, mock(ClusterElementDefinitionService.class), null);

        List<ToolCallback> toolCallbacks = approvalGate.buildGatedToolCallbacks(
            ClusterElementMap.of(ParametersFactory.create(Map.of("tools", List.of(toolClusterElementMap())))),
            Map.of(), false, mock(ActionContext.class), null);

        assertThat(toolCallbacks).hasSize(2);
        assertThat(toolCallbacks).allMatch(ApprovalGateToolCallback.class::isInstance);
    }

    private static Map<String, Object> toolClusterElementMap() {
        return Map.of(
            "name", "grepTool_1",
            "type", "aiAgentUtils/v1/grepTool",
            "parameters", Map.of());
    }
}
```

> `ClusterElementMap.of(...)` takes `Parameters`. If constructing one from a literal map proves awkward,
> read `server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/domain/ClusterElementMap.java`
> and use whichever factory it exposes; the assertion — two callbacks in, two `ApprovalGateToolCallback`s
> out — is what matters, not how the map is built.

Add these imports to the test file:

```java
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.tool.ClusterElementToolCallbacks;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.ToolCallback;
```

- [ ] **Step 2: Run it to verify it fails**

```bash
./gradlew :server:libs:modules:components:ai:agent:utils:test --tests "*AiAgentUtilsApprovalGateTest*" \
  > /tmp/t3.log 2>&1; echo "EXIT=$?"
grep -E "error:" /tmp/t3.log | head -3
```

Expected: compilation failure — `AiAgentUtilsApprovalGate` does not exist.

- [ ] **Step 3: Move ApprovalGateToolCallback into agent-utils**

```bash
cd /Volumes/Data/bytechef/bytechef
git mv \
  server/libs/modules/components/ai/agent/src/main/java/com/bytechef/component/ai/agent/tool/ApprovalGateToolCallback.java \
  server/libs/modules/components/ai/agent/utils/src/main/java/com/bytechef/component/ai/agent/utils/cluster/ApprovalGateToolCallback.java
git mv \
  server/libs/modules/components/ai/agent/src/test/java/com/bytechef/component/ai/agent/tool/ApprovalGateToolCallbackTest.java \
  server/libs/modules/components/ai/agent/utils/src/test/java/com/bytechef/component/ai/agent/utils/cluster/ApprovalGateToolCallbackTest.java
sed -i '' 's|^package com.bytechef.component.ai.agent.tool;|package com.bytechef.component.ai.agent.utils.cluster;|' \
  server/libs/modules/components/ai/agent/utils/src/main/java/com/bytechef/component/ai/agent/utils/cluster/ApprovalGateToolCallback.java \
  server/libs/modules/components/ai/agent/utils/src/test/java/com/bytechef/component/ai/agent/utils/cluster/ApprovalGateToolCallbackTest.java
```

The class body needs no other change — every symbol it imports comes from `platform-ai-api`, `platform-component-api`, `platform-configuration-api` or `platform-tool-execution-api`, all of which `agent-utils` already has.

- [ ] **Step 4: Write the gate cluster element**

Create `.../ai/agent/utils/src/main/java/com/bytechef/component/ai/agent/utils/cluster/AiAgentUtilsApprovalGate.java`:

```java
package com.bytechef.component.ai.agent.utils.cluster;

import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.APPROVAL_CHANNELS;

import com.bytechef.component.ai.llm.tool.ClusterElementToolCallbacks;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.ai.tool.constant.ToolConstants;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolCallbackProviderFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * Gates the tools attached beneath it: every call raises an approval request through this gate's own channels and
 * suspends the run instead of executing. Enforcement is structural rather than a flag on a tool, so the gate is visible
 * on the canvas and the LLM cannot route around it -- it never sees the gate at all, only the tools, which arrive
 * already wrapped.
 *
 * <p>
 * Channels and expiry belong to the gate, not the agent, so one agent can escalate destructive tools to Slack with a
 * short expiry while routine ones go to chat.
 *
 * @author Ivica Cardic
 */
public class AiAgentUtilsApprovalGate {

    private static final String APPROVAL_GATE = "approvalGate";
    private static final String NAME = "name";
    private static final String REQUEST_APPROVAL = "requestApproval";

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ClusterElementToolCallbacks clusterElementToolCallbacks;

    @Nullable
    private final ToolExecutionRecorder toolExecutionRecorder;

    public final ClusterElementDefinition<MultipleConnectionsToolCallbackProviderFunction> clusterElementDefinition;

    @SuppressFBWarnings("EI")
    public AiAgentUtilsApprovalGate(
        ClusterElementToolCallbacks clusterElementToolCallbacks,
        ClusterElementDefinitionService clusterElementDefinitionService,
        @Nullable ToolExecutionRecorder toolExecutionRecorder) {

        this.clusterElementToolCallbacks = clusterElementToolCallbacks;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.toolExecutionRecorder = toolExecutionRecorder;

        this.clusterElementDefinition =
            ComponentDsl.<MultipleConnectionsToolCallbackProviderFunction>clusterElement(APPROVAL_GATE)
                .title("Approval Gate")
                .description("Require human approval before the agent may call any tool attached to this gate.")
                .type(TOOLS)
                .properties(
                    string(NAME)
                        .label("Name")
                        .description("Identifies this gate in the approval request and in the tools list.")
                        .required(true),
                    integer(ToolConstants.APPROVAL_EXPIRES_IN)
                        .label("Expires In")
                        .description("How long an approval request stays valid. Defaults to 60 days when blank.")
                        .minValue(1)
                        .required(false),
                    string(ToolConstants.APPROVAL_EXPIRES_IN_UNIT)
                        .label("Expires In Unit")
                        .options(
                            option("Hours", ToolConstants.APPROVAL_EXPIRES_IN_UNIT_HOURS),
                            option("Days", "DAYS"))
                        .defaultValue("DAYS")
                        .required(false))
                .object(() -> this::apply);
    }

    /**
     * Rejects children that cannot be gated. {@code requestApproval} already suspends, so gating it would deliver two
     * requests and set a suspend whose continueParameters cannot be resumed; a nested gate would do the same.
     * Package-private so a test can pin both rejections without building a whole cluster element map.
     */
    static void checkGatableChild(String componentName, String clusterElementName) {
        if ("approval".equals(componentName) && REQUEST_APPROVAL.equals(clusterElementName)) {
            throw new IllegalStateException(
                "'requestApproval' cannot be attached to an approval gate -- it already raises an approval of its "
                    + "own. Attach it directly to the agent instead.");
        }

        if (APPROVAL_GATE.equals(clusterElementName)) {
            throw new IllegalStateException(
                "An approval gate cannot be attached to another approval gate. Attach the tools to one gate.");
        }
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections, Context context) {

        ActionContextAware actionContextAware = (ActionContextAware) context;

        return ToolCallbackProvider.from(
            buildGatedToolCallbacks(
                ClusterElementMap.of(extensions), componentConnections, actionContextAware.isEditorEnvironment(),
                (ActionContext) context, getApprovalExpiry(inputParameters)));
    }

    /**
     * Wraps every callback of every gated element exactly once. Package-private so a test can pin that directly: a
     * tool element contributing two callbacks must produce two gated callbacks, not one gate around a collection.
     */
    List<ToolCallback> buildGatedToolCallbacks(
        ClusterElementMap clusterElementMap, Map<String, ComponentConnection> componentConnections,
        boolean editorEnvironment, ActionContext context, @Nullable Duration approvalExpiry) {

        List<ClusterElement> approvalChannelClusterElements = clusterElementMap.getClusterElements(APPROVAL_CHANNELS);

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (ClusterElement toolClusterElement : clusterElementMap.getClusterElements(TOOLS)) {
            checkGatableChild(toolClusterElement.getComponentName(), toolClusterElement.getClusterElementName());

            List<ToolCallback> elementToolCallbacks = clusterElementToolCallbacks.build(
                toolClusterElement, componentConnections, editorEnvironment, context);

            for (ToolCallback toolCallback : elementToolCallbacks) {
                toolCallbacks.add(
                    new ApprovalGateToolCallback(
                        toolCallback, approvalChannelClusterElements, componentConnections,
                        clusterElementDefinitionService, context, toolExecutionRecorder, approvalExpiry));
            }
        }

        return toolCallbacks;
    }

    /**
     * Resolves the gate's expiry, or null to let {@link ApprovalGateToolCallback} apply its 60-day default. Moved off
     * per-tool parameters; the stored key names are unchanged.
     */
    @Nullable
    private static Duration getApprovalExpiry(Parameters inputParameters) {
        Object approvalExpiresIn = inputParameters.get(ToolConstants.APPROVAL_EXPIRES_IN);

        if (!(approvalExpiresIn instanceof Number approvalExpiresInNumber) || approvalExpiresInNumber.longValue() < 1) {
            return null;
        }

        Object approvalExpiresInUnit = inputParameters.get(ToolConstants.APPROVAL_EXPIRES_IN_UNIT);

        if (ToolConstants.APPROVAL_EXPIRES_IN_UNIT_HOURS.equals(approvalExpiresInUnit)) {
            return Duration.ofHours(approvalExpiresInNumber.longValue());
        }

        return Duration.ofDays(approvalExpiresInNumber.longValue());
    }
}
```

- [ ] **Step 5: Register the gate and declare its child types**

In `AiAgentUtilsComponentHandler.java`:

1. Add the static import `com.bytechef.component.definition.approval.ApprovalChannelFunction.APPROVAL_CHANNELS`.
2. Add the constant `private static final String APPROVAL_GATE = "approvalGate";`
3. Inject the recorder and the collaborator — add these constructor parameters:
   `ObjectProvider<ToolExecutionRecorder> toolExecutionRecorderObjectProvider`
4. Build the gate before the `clusterElements` list and add `approvalGate.clusterElementDefinition` to it:

```java
        AiAgentUtilsApprovalGate agentUtilsApprovalGate = new AiAgentUtilsApprovalGate(
            new ClusterElementToolCallbacks(aiAgentToolFacade, clusterElementDefinitionService),
            clusterElementDefinitionService, toolExecutionRecorderObjectProvider.getIfAvailable());
```

5. Extend the declared types and the per-element map:

```java
        @Override
        public List<ClusterElementType> getClusterElementTypes() {
            return List.of(MODEL, SUBAGENT, TOOLS, APPROVAL_CHANNELS);
        }
```

```java
        clusterElementClusterElementTypes.put(APPROVAL_GATE, List.of(TOOLS.name(), APPROVAL_CHANNELS.name()));
```

placed next to the existing `TASK_TOOL` / `SUBAGENT_ELEMENT` / `SMART_WEB_FETCH_TOOL` puts in `buildClusterElementClusterElementTypes`.

- [ ] **Step 6: Reject gates under a subagent**

In `AiAgentUtilsTaskTool.buildSubagentToolCallbacks`, add the guard at the top of the loop:

```java
        for (ClusterElement toolClusterElement : subagentClusterElementMap.getClusterElements(TOOLS)) {
            if ("approvalGate".equals(toolClusterElement.getClusterElementName())) {
                throw new IllegalStateException(
                    "Approval gates cannot be attached to a subagent -- a suspended subagent run cannot be resumed. "
                        + "Attach the gate to the agent instead.");
            }

            toolCallbacks.addAll(
                clusterElementToolCallbacks.build(
                    toolClusterElement, componentConnections, editorEnvironment, (ActionContext) context));
        }
```

- [ ] **Step 7: Delete the flag path from the agent**

In `AbstractAiAgentChatAction.java`:

1. In `getToolCallbacks`, delete the `approvalChannelClusterElements` parameter and the entire `if (Boolean.TRUE.equals(clusterElementParameters.get(ToolConstants.REQUIRES_APPROVAL)) ...)` block, leaving:

```java
        for (ClusterElement clusterElement : toolClusterElements) {
            toolCallbacks.addAll(
                buildElementToolCallbacks(clusterElement, connectionParameters, editorEnvironment, context));
        }
```

2. Delete the private methods `getApprovalExpiry` and `isSuspendingApprovalTool`.
3. Update the single call site (around line 304) to drop the channels argument:

```java
            .tools(
                concatToolCallbacks(
                    getToolCallbacks(
                        clusterElementMap.getClusterElements(BaseToolFunction.TOOLS),
                        connectionParameters, context.isEditorEnvironment(), toolExecutionListener, toolSimulations,
                        chatModel, context),
                    chatMemoryResult)
                        .toArray());
```

4. Remove the now-unused imports: `ApprovalGateToolCallback`, `ApprovalChannelFunction`, and `java.time.Duration` **if** nothing else in the file uses it (`grep -n "Duration" <file>` first).

**Do not touch** the resume branch — `resolveGatedToolResumeData`, `buildPatchedRequestSpec`, and every use of `ToolSuspendConstants.GATED_TOOL_NAME`/`GATED_TOOL_INPUT` stay exactly as they are. That is the contract the gate still speaks to across the module boundary.

- [ ] **Step 8: Remove APPROVAL_CHANNELS from the agent root and delete the constant**

In `AiAgentComponentDefinition.java`, drop `APPROVAL_CHANNELS` from the list and its static import:

```java
    @Override
    default List<ClusterElementType> getClusterElementTypes() {
        return List.of(MODEL, CHAT_MEMORY, RAG, GUARDRAILS, TOOLS);
    }
```

In `ToolConstants.java`, delete `REQUIRES_APPROVAL` and its Javadoc. Keep `APPROVAL_EXPIRES_IN`, `APPROVAL_EXPIRES_IN_UNIT` and `APPROVAL_EXPIRES_IN_UNIT_HOURS` — the gate uses them. Verify nothing else references the deleted constant:

```bash
grep -rn "REQUIRES_APPROVAL\|requiresApproval" server --include="*.java" || echo "CLEAN"
```

- [ ] **Step 9: Update the definition snapshots by hand**

`JsonFileAssert` compares semantically, so edit rather than regenerate — wholesale regeneration rewrites every line with different Jackson spacing.

In `.../ai/agent/src/test/resources/definition/ai-agent_v1.json`, remove the `APPROVAL_CHANNELS` entry from the `clusterElementTypes` array.

`.../ai/agent/utils/src/test/resources/definition/ai_agent-utils_v1.json` gains a whole new `clusterElements`
entry with nested property JSON, so **regenerate this one** rather than hand-authoring it:

```bash
ls server/libs/modules/components/ai/agent/utils/src/test/resources/definition/
rm -f server/libs/modules/components/ai/agent/utils/src/test/resources/definition/ai_agent-utils_v1.json \
      server/libs/modules/components/ai/agent/utils/build/resources/test/definition/ai_agent-utils_v1.json
./gradlew :server:libs:modules:components:ai:agent:utils:test > /dev/null 2>&1   # writes the file
./gradlew :server:libs:modules:components:ai:agent:utils:test > /tmp/snap.log 2>&1; echo "EXIT=$?"
```

The first run writes the snapshot and fails (the classpath copy is still absent); the second compares and
must pass. Then confirm the regenerated file has the three expected additions:

```bash
python3 -c "
import json
d=json.load(open('server/libs/modules/components/ai/agent/utils/src/test/resources/definition/ai_agent-utils_v1.json'))
print('types:', [t['name'] for t in d['clusterElementTypes']])
print('gate ->', d['clusterElementClusterElementTypes'].get('approvalGate'))
print('elements:', sorted(c['name'] for c in d['clusterElements']))
"
```

Expected: `types` includes `APPROVAL_CHANNELS`; `gate -> ['TOOLS', 'APPROVAL_CHANNELS']`; `elements`
includes `approvalGate`.

Regenerating reformats every line (the committed file uses Jackson 2 spacing, `"key": v`; the current
toolchain emits `"key" : v`), so this file's diff will be large. That is expected and acceptable here
because its semantic change is large too. The `ai-agent_v1.json` edit above stays a hand-edit precisely
to keep *that* diff to one line.

- [ ] **Step 10: Run everything**

```bash
./gradlew :server:libs:modules:components:ai:agent:test \
          :server:libs:modules:components:ai:agent:utils:test \
          :server:libs:platform:platform-component:platform-component-api:test \
          --continue > /tmp/t3full.log 2>&1
echo "EXIT=$?"
grep -E "^> Task .* FAILED" /tmp/t3full.log || echo "ALL GREEN"
```

`AiAgentStreamChatActionResumeGateIntTest` and `ApprovalGateToolCallbackTest` must both still pass — they prove the suspend→resume protocol survived the module split.

- [ ] **Step 11: Commit**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1; echo "EXIT=$?"
git add server/libs/modules/components/ai/agent server/libs/platform/platform-ai server/libs/platform/platform-component
git commit -F - <<'MSG'
0 Make the approval gate a cluster element

Gating was a hidden boolean: a TOOLS entry carrying requiresApproval: true got
wrapped in ApprovalGateToolCallback. The flag was set from a checkbox in a
dropdown menu, never appeared in the Properties tab, and was invisible on the
canvas. Channels lived on the AI Agent root, so one list covered every gated
tool.

approvalGate is now a cluster element owning the tools it gates and the channels
it delivers to, so an agent can escalate destructive tools to Slack with a short
expiry while routine ones go to chat. It returns its children pre-wrapped, which
removes the agent module's special case entirely -- the flag branch, the
approvalChannelClusterElements parameter, getApprovalExpiry and
isSuspendingApprovalTool all go, and APPROVAL_CHANNELS comes off the agent root.

Simulation and observable wrapping still run over the flattened list afterwards,
so the audit trail's gate-inside-observable ordering is unchanged. Suspend and
resume now cross a module boundary as a ToolSuspendConstants protocol.

Gates are rejected under a subagent: the sentinel would be lost in the
subagent's own ChatClient and the suspend orphaned.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01K4Y64sa7FQTZoefx6Yd5RK
MSG
```

---

## Task 4: Rework the workflow validator

**Files:**
- Modify: `server/libs/platform/platform-workflow/platform-workflow-validator/platform-workflow-validator-service/src/main/java/com/bytechef/platform/workflow/validator/WorkflowValidator.java`
- Modify: `.../src/test/java/com/bytechef/platform/workflow/validator/WorkflowValidatorChatApprovalChannelTest.java`

**Interfaces:**
- Consumes: the `approvalGate` cluster element name and the `approvalChannels` key beneath it (Task 3).
- Produces: nothing other tasks consume.

- [ ] **Step 1: Update the test fixtures to the new shape**

In `WorkflowValidatorChatApprovalChannelTest.java`, each of the three fixtures currently nests `approvalChannels` at the task's `clusterElements` level with a gated tool alongside. Restructure each so the channels sit inside a gate. The gated-tool fixture becomes:

```json
"clusterElements": {
  "tools": [
    {
      "name": "approvalGate_1",
      "type": "aiAgentUtils/v1/approvalGate",
      "parameters": { "name": "Destructive" },
      "clusterElements": {
        "approvalChannels": [
          { "name": "chat_1", "type": "chat/v1/chat", "parameters": {} }
        ],
        "tools": [
          { "name": "deleteRecord_1", "type": "example/v1/deleteRecord", "parameters": {} }
        ]
      }
    }
  ]
}
```

For the "no channels configured" case, omit the `approvalChannels` array entirely but keep the gate — that is what now triggers the implicit-chat-default warning.

- [ ] **Step 2: Run to verify the tests fail**

```bash
./gradlew :server:libs:platform:platform-workflow:platform-workflow-validator:platform-workflow-validator-service:test \
  --tests "*WorkflowValidatorChatApprovalChannelTest*" > /tmp/t4.log 2>&1; echo "EXIT=$?"
grep -E "FAILED" /tmp/t4.log | head
```

Expected: failures — the validator still looks for top-level `approvalChannels` and `requiresApproval`.

- [ ] **Step 3: Rewrite the two scanners**

Replace `hasGatedTool` and `getApprovalChannelTypes` with gate-aware versions, and have `validateChatOnlyApprovalChannels` iterate gates:

```java
    private static List<JsonNode> getApprovalGates(JsonNode clusterElementsJsonNode) {
        JsonNode toolsJsonNode = clusterElementsJsonNode.get("tools");

        if (toolsJsonNode == null || !toolsJsonNode.isArray()) {
            return List.of();
        }

        List<JsonNode> approvalGateJsonNodes = new ArrayList<>();

        for (JsonNode toolJsonNode : toolsJsonNode) {
            if (!toolJsonNode.isObject() || !toolJsonNode.has("type")) {
                continue;
            }

            JsonNode typeJsonNode = toolJsonNode.get("type");

            String[] typeParts = typeJsonNode.asString()
                .split("/");

            if (typeParts.length == 3 && "approvalGate".equals(typeParts[2])) {
                approvalGateJsonNodes.add(toolJsonNode);
            }
        }

        return approvalGateJsonNodes;
    }

    private static List<String> getApprovalChannelTypes(JsonNode approvalGateJsonNode) {
        JsonNode clusterElementsJsonNode = approvalGateJsonNode.get("clusterElements");

        if (clusterElementsJsonNode == null || !clusterElementsJsonNode.isObject()) {
            return List.of();
        }

        JsonNode approvalChannelsJsonNode = clusterElementsJsonNode.get("approvalChannels");

        if (approvalChannelsJsonNode == null || !approvalChannelsJsonNode.isArray()) {
            return List.of();
        }

        List<String> approvalChannelTypes = new ArrayList<>();

        for (JsonNode approvalChannelJsonNode : approvalChannelsJsonNode) {
            if (approvalChannelJsonNode.isObject() && approvalChannelJsonNode.has("type")) {
                JsonNode typeJsonNode = approvalChannelJsonNode.get("type");

                approvalChannelTypes.add(typeJsonNode.asString());
            }
        }

        return approvalChannelTypes;
    }
```

In `validateChatOnlyApprovalChannels`, replace the body of the per-task loop with a per-gate loop:

```java
            for (JsonNode approvalGateJsonNode : getApprovalGates(clusterElementsJsonNode)) {
                List<String> approvalChannelTypes = getApprovalChannelTypes(approvalGateJsonNode);

                boolean chatOnlyChannels = !approvalChannelTypes.isEmpty() &&
                    approvalChannelTypes.stream()
                        .allMatch(WorkflowValidator::isChatApprovalChannelType);
                boolean implicitChatDefault = approvalChannelTypes.isEmpty();

                if (chatOnlyChannels || implicitChatDefault) {
                    StringUtils.appendWithNewline(
                        "Approval gate '" + getNodeName(approvalGateJsonNode) + "' in task '" +
                            getNodeName(taskJsonNode) + "' delivers approval requests only to the chat channel" +
                            (implicitChatDefault ? " (the default when no approval channels are configured)" : "") +
                            ", but the workflow does not start from a chat trigger. Runs started by webhook or " +
                            "schedule will pause without a live approval card and are only resolvable from the " +
                            "pending run approvals list or the hosted form. Configure a fallback approval channel " +
                            "(Slack, email, approval task) or use a chat trigger.",
                        warnings);
                }
            }
```

Delete the old `hasGatedTool` method — a gate's existence is now the signal.

- [ ] **Step 4: Run to verify the tests pass**

```bash
./gradlew :server:libs:platform:platform-workflow:platform-workflow-validator:platform-workflow-validator-service:test \
  > /tmp/t4.log 2>&1; echo "EXIT=$?"
grep -E "^> Task .* FAILED" /tmp/t4.log || echo "GREEN"
```

Expected: `EXIT=0`, `GREEN`.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1; echo "EXIT=$?"
git add server/libs/platform/platform-workflow
git commit -F - <<'MSG'
0 Warn about chat-only approval channels per gate

The validator scanned a task's top-level approvalChannels and looked for tools
carrying requiresApproval. Channels now belong to a gate, and a gate's existence
is the signal that approvals happen, so the scan walks gates and names the one
it is warning about.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01K4Y64sa7FQTZoefx6Yd5RK
MSG
```

---

## Task 5: Render gates as groups in the tools panel

**Files:**
- Modify: `client/src/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/components/hooks/useAiAgentTools.ts`
- Modify: `client/src/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/components/AiAgentTools.tsx`
- Modify: `client/src/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/components/AiAgentTool.tsx`
- Create: `client/src/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/components/hooks/useAiAgentTools.test.ts`

**Interfaces:**
- Consumes: cluster element type `aiAgentUtils/v1/approvalGate` with nested `clusterElements.tools` and `clusterElements.approvalChannels` (Task 3).
- Produces: `ToolItemI` (unchanged minus three fields), new `ToolGroupI`, and `useAiAgentTools()` returning `{configuredConnectionKeys, rootWorkflowNodeName, toolGroups, tools}`.

- [ ] **Step 1: Write the failing hook test**

Create `useAiAgentTools.test.ts`:

```ts
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {useWorkflowDataStoreMock, useWorkflowEditorStoreMock} = vi.hoisted(() => ({
    useWorkflowDataStoreMock: vi.fn(),
    useWorkflowEditorStoreMock: vi.fn(),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => ({
    default: useWorkflowDataStoreMock,
}));
vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => ({
    default: useWorkflowEditorStoreMock,
}));
vi.mock('@/shared/queries/platform/workflowTestConfigurations.queries', () => ({
    useGetWorkflowTestConfigurationConnectionsQuery: () => ({data: []}),
}));
vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: () => 1,
}));

import useAiAgentTools from './useAiAgentTools';

const rootClusterElementNodeData = {
    clusterElements: {
        tools: [
            {name: 'grepTool_1', type: 'aiAgentUtils/v1/grepTool'},
            {
                clusterElements: {
                    approvalChannels: [{name: 'chat_1', type: 'chat/v1/chat'}],
                    tools: [{name: 'deleteRecord_1', type: 'example/v1/deleteRecord'}],
                },
                name: 'approvalGate_1',
                parameters: {name: 'Destructive'},
                type: 'aiAgentUtils/v1/approvalGate',
            },
        ],
    },
    workflowNodeName: 'aiAgent_1',
};

describe('useAiAgentTools', () => {
    beforeEach(() => {
        useWorkflowEditorStoreMock.mockImplementation((selector) => selector({rootClusterElementNodeData}));
        useWorkflowDataStoreMock.mockImplementation((selector) =>
            selector({componentDefinitions: [], workflow: {id: 'workflow-1'}})
        );
    });

    it('keeps an ungated tool out of every group', () => {
        const {result} = renderHook(() => useAiAgentTools());

        expect(result.current.tools.map((tool) => tool.name)).toEqual(['grepTool_1']);
    });

    it('groups a gate with its gated tools and channels', () => {
        const {result} = renderHook(() => useAiAgentTools());

        expect(result.current.toolGroups).toHaveLength(1);

        const [group] = result.current.toolGroups;

        expect(group.label).toBe('Destructive');
        expect(group.name).toBe('approvalGate_1');
        expect(group.tools.map((tool) => tool.name)).toEqual(['deleteRecord_1']);
        expect(group.channelLabels).toEqual(['chat']);
    });
});
```

- [ ] **Step 2: Run it to verify it fails**

```bash
cd client
npx vitest run src/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/components/hooks/useAiAgentTools.test.ts
```

Expected: FAIL — `toolGroups` is undefined.

- [ ] **Step 3: Reshape the hook**

In `useAiAgentTools.ts`:

1. Remove `approvalExpiresIn`, `approvalExpiresInUnit` and `requiresApproval` from `ToolItemI`.
2. Add above `UseAiAgentToolsI`:

```ts
export interface ToolGroupI {
    channelLabels: string[];
    label: string;
    name: string;
    tools: ToolItemI[];
}
```

3. Change the return interface to:

```ts
interface UseAiAgentToolsI {
    configuredConnectionKeys: Set<string>;
    rootWorkflowNodeName?: string;
    toolGroups: ToolGroupI[];
    tools: ToolItemI[];
}
```

4. Add this module-level helper above the hook — it is the body of the current `toolElements.map(...)` callback with the three deleted fields removed:

```ts
function mapToolElement(
    toolElement: NodeDataType & {name?: string},
    definitionsMap: Map<string, {icon?: string; title?: string}>
): ToolItemI {
    // Tool elements can appear in either shape: NodeDataType (workflowNodeName)
    // when synced in-memory, or ClusterElementItemType (name) when loaded from
    // the workflow definition. Fall through both so the first load after an
    // Add also produces a non-empty identifier.
    const typeSegments = toolElement.type?.split('/') || [];
    const componentName = toolElement.componentName || typeSegments[0] || '';
    const componentVersion = parseInt(typeSegments[1]?.replace(/^v/, '')) || 1;
    const operationName = toolElement.operationName || typeSegments[2] || '';
    const componentDefinition = definitionsMap.get(componentName);
    const toolName = toolElement.workflowNodeName || toolElement.name || '';

    return {
        componentName,
        componentVersion,
        icon: componentDefinition?.icon,
        label: toolElement.label || toolName,
        name: toolName,
        operationName,
        title: componentDefinition?.title || componentName,
        type: toolElement.type || '',
    };
}
```

Then split the list:

```ts
    const {toolGroups, tools} = useMemo(() => {
        const clusterElements = rootClusterElementNodeData?.clusterElements;

        if (!clusterElements || Array.isArray(clusterElements)) {
            return {toolGroups: [], tools: []};
        }

        const toolElements = clusterElements['tools'];

        if (!Array.isArray(toolElements)) {
            return {toolGroups: [], tools: []};
        }

        const definitionsMap = new Map(componentDefinitions.map((definition) => [definition.name, definition]));

        const groups: ToolGroupI[] = [];
        const ungatedTools: ToolItemI[] = [];

        toolElements.forEach((toolElement) => {
            const element = toolElement as unknown as NodeDataType & {
                clusterElements?: Record<string, unknown>;
                name?: string;
            };

            const operationName = element.type?.split('/')[2] || '';

            if (operationName !== 'approvalGate') {
                ungatedTools.push(mapToolElement(element, definitionsMap));

                return;
            }

            const gateClusterElements = element.clusterElements || {};
            const gatedToolElements = gateClusterElements['tools'];
            const channelElements = gateClusterElements['approvalChannels'];
            const gateParameters = element.parameters as {name?: string} | undefined;
            const gateName = element.workflowNodeName || element.name || '';

            groups.push({
                channelLabels: Array.isArray(channelElements)
                    ? channelElements.map(
                          (channelElement) => (channelElement as {type?: string}).type?.split('/')[0] || ''
                      )
                    : [],
                label: gateParameters?.name || gateName,
                name: gateName,
                tools: Array.isArray(gatedToolElements)
                    ? gatedToolElements.map((gatedToolElement) =>
                          mapToolElement(gatedToolElement as NodeDataType & {name?: string}, definitionsMap)
                      )
                    : [],
            });
        });

        return {toolGroups: groups, tools: ungatedTools};
    }, [rootClusterElementNodeData?.clusterElements, componentDefinitions]);
```

5. Return `toolGroups` alongside `tools`.

- [ ] **Step 4: Run the hook test to verify it passes**

```bash
cd client
npx vitest run src/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/components/hooks/useAiAgentTools.test.ts
```

Expected: PASS.

- [ ] **Step 5: Render the groups**

In `AiAgentTools.tsx`, destructure `toolGroups` and render them beneath the ungated tools. Each group gets a header row with its own add-tool and add-channel popovers:

```tsx
            {toolGroups.map((toolGroup) => (
                <fieldset className="space-y-2 rounded border border-stroke-neutral-secondary p-2" key={toolGroup.name}>
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-1">
                            <ShieldCheckIcon className="size-3.5 text-amber-600" />

                            <span className="text-xs font-medium">{toolGroup.label}</span>
                        </div>

                        <WorkflowNodesPopoverMenu
                            clusterElementType="tools"
                            hideActionComponents
                            hideTaskDispatchers
                            hideTriggerComponents
                            multipleClusterElementsNode
                            sourceNodeId={toolGroup.name}
                        >
                            <Button icon={<PlusIcon />} label="Add Tool" size="sm" variant="ghost" />
                        </WorkflowNodesPopoverMenu>
                    </div>

                    {toolGroup.tools.map((tool, toolIndex) => (
                        <AiAgentTool
                            configuredConnectionKeys={configuredConnectionKeys}
                            key={`${tool.name}-${toolIndex}`}
                            tool={tool}
                        />
                    ))}

                    <div className="flex items-center justify-between">
                        <span className="text-xs text-muted-foreground">
                            {toolGroup.channelLabels.length
                                ? `Channels: ${toolGroup.channelLabels.join(', ')}`
                                : 'Channels: chat (default)'}
                        </span>

                        <WorkflowNodesPopoverMenu
                            clusterElementType="approvalChannels"
                            hideActionComponents
                            hideTaskDispatchers
                            hideTriggerComponents
                            multipleClusterElementsNode
                            sourceNodeId={toolGroup.name}
                        >
                            <Button icon={<PlusIcon />} label="Add Channel" size="sm" variant="ghost" />
                        </WorkflowNodesPopoverMenu>
                    </div>
                </fieldset>
            ))}
```

Add `ShieldCheckIcon` to the existing lucide import, keeping the names alphabetically sorted: `import {InfoIcon, PlusIcon, ShieldCheckIcon} from 'lucide-react';`

- [ ] **Step 6: Drop the per-tool approval badge**

In `AiAgentTool.tsx`, delete the `{tool.requiresApproval && (...)}` block and the now-unused `Tooltip`, `TooltipContent`, `TooltipTrigger` and `ShieldCheckIcon` imports. Gating is conveyed by the group, not by a badge on each row.

- [ ] **Step 7: Run the client checks**

```bash
cd client
npm run check
```

Expected: exit 0 — lint, typecheck and the full Vitest suite all pass.

- [ ] **Step 8: Commit**

```bash
cd /Volumes/Data/bytechef/bytechef
git add client/src/pages/platform/cluster-element-editor
git commit -F - <<'MSG'
0 client - Group gated tools under their approval gate

The tools list was flat and gating showed only as a badge, because approval was
a hidden per-tool flag. A gate now owns its tools and channels, so the list
renders it as a group with its own add-tool and add-channel actions and shows
which channels it delivers to.

The simple editor is the default view, so leaving gates to the canvas alone
would have kept approvals as undiscoverable as the checkbox was.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01K4Y64sa7FQTZoefx6Yd5RK
MSG
```

---

## Task 6: Remove the approval controls from the tool dropdown menu

**Files:**
- Modify: `client/src/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/components/AiAgentToolDropdownMenu.tsx`
- Modify: `client/src/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/components/hooks/useAiAgentToolDropdownMenu.tsx`

**Interfaces:**
- Consumes: `ToolItemI` without `requiresApproval` / `approvalExpiresIn` / `approvalExpiresInUnit` (Task 5).
- Produces: `useAiAgentToolDropdownMenu()` returning only `{handleConfigureTool, handleRemoveTool}`.

- [ ] **Step 1: Strip the menu**

In `AiAgentToolDropdownMenu.tsx`:
- delete the `ApprovalExpiryPresetI` interface, the `APPROVAL_EXPIRY_PRESETS` array and the `isPresetSelected` helper;
- delete the `<DropdownMenuCheckboxItem>` for "Requires approval" and the whole `{tool.requiresApproval && (<DropdownMenuSub>...)}` block;
- change the destructure to `const {handleConfigureTool, handleRemoveTool} = useAiAgentToolDropdownMenu();`
- remove imports left unused: `DropdownMenuCheckboxItem`, `DropdownMenuSub`, `DropdownMenuSubContent`, `DropdownMenuSubTrigger`, `ClockIcon`.

The menu keeps *Configure* and *Remove*.

- [ ] **Step 2: Strip the hook**

In `useAiAgentToolDropdownMenu.tsx`, delete `handleToggleRequiresApproval` and `handleSetApprovalExpiry` in full, remove them from the returned object, and delete any imports and store selectors that become unused (`updateClusterElementParameterMutation` is used by both — remove it only if nothing else in the file calls it).

- [ ] **Step 3: Verify nothing still references the removed fields**

```bash
cd client
grep -rn "requiresApproval\|approvalExpiresIn\|handleToggleRequiresApproval\|handleSetApprovalExpiry" src \
  || echo "CLEAN"
```

Expected: `CLEAN`.

- [ ] **Step 4: Run the client checks**

```bash
cd client
npm run check
```

Expected: exit 0.

- [ ] **Step 5: Commit**

```bash
cd /Volumes/Data/bytechef/bytechef
git add client/src/pages/platform/cluster-element-editor
git commit -F - <<'MSG'
0 client - Remove the approval controls from the tool menu

Gating is structural now, so a tool row has nothing to toggle: the "Requires
approval" checkbox and the expiry preset submenu described a flag that no longer
exists. Expiry is a property of the gate.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01K4Y64sa7FQTZoefx6Yd5RK
MSG
```

---

## Final verification

- [ ] **Full server check**

```bash
cd /Volumes/Data/bytechef/bytechef
./gradlew compileJava compileTestJava --continue > /tmp/final.log 2>&1
echo "EXIT=$?"
grep -E "^> Task .* FAILED" /tmp/final.log || echo "ALL GREEN"
```

- [ ] **Full client check**

```bash
cd client && npm run check
```

- [ ] **Docs**

Update `.agents/hitl-approvals.md`: the "Tool gate" bullet describes `requiresApproval: true` in a TOOLS entry's parameters and an editor checkbox in `AiAgentToolDropdownMenu`, and says the agent node declares APPROVAL_CHANNELS. Rewrite it to describe the `approvalGate` cluster element, its `TOOLS` and `APPROVAL_CHANNELS` children, and the per-gate expiry. Also update the chat-only validation bullet, which references "an AI agent has a `requiresApproval` tool with no channels".

Commit as `0 Update the HITL approvals reference for the approval gate element`.

---

## Notes for the implementer

- **The gate is invisible to the LLM.** `ApprovalGateToolCallback.getToolDefinition()` returns the delegate's definition, so the model sees `deleteRecord` and calls `deleteRecord`. The gate intercepts. Do not give the gate element a tool name of its own or try to expose it.
- **Wrapper order matters and is already correct.** Simulation and observable wrapping run over the flattened list in `getToolCallbacks` *after* the per-element loop, so the gate stays inside the observable wrapper without extra work. Do not move the gate wrapping outward.
- **Do not touch the resume path.** `resolveGatedToolResumeData` and the `ToolSuspendConstants.GATED_TOOL_NAME`/`GATED_TOOL_INPUT` reads stay in the agent module. That protocol is what lets the gate live in a different module.
- **`ClusterElement` field order** varies by constructor; copy the shape from a nearby existing test rather than guessing.
- **Snapshot filenames are hyphenated and do not match component names** (`ai_agent-utils_v1.json`, `ai-agent_v1.json`). `ls` the directory before deleting anything.
