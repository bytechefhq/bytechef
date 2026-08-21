# Task Tool Subagents as Cluster Elements — Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make each Task Tool subagent's tools and model chosen by the workflow builder as `SUBAGENT` cluster elements, replacing the four library-defined Claude subagents.

**Architecture:** `AiAgentUtilsTaskTool` stops constructing `ClaudeSubagentType` and instead builds one `SubagentReference` per attached `SUBAGENT` cluster element, paired with a ByteChef `SubagentResolver`/`SubagentExecutor`. The executor builds a `ChatClient` per subagent from that subagent's own nested `MODEL` and `TOOLS` children. Because `ClaudeSubagentType` is never constructed, its private `defaultClaudeSubagentTools()` never runs.

**Tech Stack:** Java 25, Spring Boot 4.0.7, Spring AI 2.0.0, `org.springaicommunity:spring-ai-agent-utils:0.10.0` + `spring-ai-agent-utils-common:0.10.0`, JUnit 5, Mockito, AssertJ.

**Spec:** `docs/superpowers/specs/2026-08-03-task-tool-subagent-cluster-elements-design.md`

## Global Constraints

- Apache 2.0 licence header on every new file (module is under `server/libs`, not `server/ee`).
- `@author Ivica Cardic` Javadoc tag on every new class.
- One blank line before control statements and after a variable modification that a later statement uses; no blank line before a class's closing brace.
- Test classes end in `Test` (not `IntTest`); test method names are camelCase with no underscores.
- Never chain method calls except builders, streams, `Optional`, and the other idioms listed in `CLAUDE.md`.
- Run `./gradlew spotlessApply` before each commit.
- Phase 1 does **not** touch `ByteChefTaskRepository`, background tasks, or the drain seam — those are Phase 2 and blocked on spec open question 3.

## File Structure

Module: `server/libs/modules/components/ai/agent/utils`
Gradle path: `:server:libs:modules:components:ai:agent:utils`
Package root: `com.bytechef.component.ai.agent.utils`

| File | Responsibility |
|---|---|
| `sdks/backend/java/component-api/.../ai/agent/SubagentFunction.java` | Declares the `SUBAGENT` `ClusterElementType` constant |
| `.../utils/cluster/AiAgentUtilsSubagent.java` | The `SUBAGENT` cluster element definition (label, description, instructions properties) |
| `.../utils/cluster/subagent/ByteChefSubagentDefinition.java` | `SubagentDefinition` backed by one `ClusterElement` |
| `.../utils/cluster/subagent/ByteChefSubagentResolver.java` | Resolves a `SubagentReference` back to its definition |
| `.../utils/cluster/subagent/ByteChefSubagentExecutor.java` | Builds and runs a `ChatClient` per subagent |
| `.../utils/cluster/AiAgentUtilsTaskTool.java` *(modify)* | Swaps `ClaudeSubagentType` for the ByteChef type |

---

### Task 1: Declare the SUBAGENT cluster element type and definition

**Files:**
- Create: `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ai/agent/SubagentFunction.java`
- Create: `server/libs/modules/components/ai/agent/utils/src/main/java/com/bytechef/component/ai/agent/utils/cluster/AiAgentUtilsSubagent.java`
- Test: `server/libs/modules/components/ai/agent/utils/src/test/java/com/bytechef/component/ai/agent/utils/cluster/AiAgentUtilsSubagentTest.java`

**Interfaces:**
- Consumes: `ClusterElementDefinition.ClusterElementType(String name, String key, String label, boolean multipleElements, boolean required)` — note `multipleElements` precedes `required`; both are unlabelled booleans, so transposing them compiles and fails only at runtime
- Produces: `SubagentFunction.SUBAGENT` (the type constant) and `AiAgentUtilsSubagent.CLUSTER_ELEMENT_DEFINITION`

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.component.ai.agent.utils.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.ai.agent.SubagentFunction;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AiAgentUtilsSubagentTest {

    @Test
    void testSubagentTypeAllowsMultipleOptionalElements() {
        assertThat(SubagentFunction.SUBAGENT.key()).isEqualTo("subagent");
        assertThat(SubagentFunction.SUBAGENT.multipleElements()).isTrue();
        assertThat(SubagentFunction.SUBAGENT.required()).isFalse();
    }

    @Test
    void testClusterElementDefinitionIsNamedSubagent() {
        assertThat(AiAgentUtilsSubagent.CLUSTER_ELEMENT_DEFINITION.getName()).isEqualTo("subagent");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:modules:components:ai:agent:utils:test --tests "*AiAgentUtilsSubagentTest*"`
Expected: FAIL — `package com.bytechef.component.definition.ai.agent.SubagentFunction does not exist`

- [ ] **Step 3: Create the type constant**

`SubagentFunction.java` — mirrors `BaseToolFunction.TOOLS`, which is declared as
`new ClusterElementType("TOOLS", "tools", "Tools", true, false)`:

```java
package com.bytechef.component.definition.ai.agent;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;

/**
 * Marker for subagents attached to the Task Tool. Each SUBAGENT cluster element carries its own nested TOOLS (and
 * optionally MODEL) children, so the workflow builder — not the library — decides what a subagent may do.
 *
 * @author Ivica Cardic
 */
public interface SubagentFunction {

    ClusterElementType SUBAGENT = new ClusterElementType("SUBAGENT", "subagent", "Subagent", true, false);
}
```

- [ ] **Step 4: Create the cluster element definition**

```java
package com.bytechef.component.ai.agent.utils.cluster;

import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ai.agent.SubagentFunction.SUBAGENT;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;

/**
 * A builder-defined subagent. Its nested TOOLS children are the only tools it may call, and its optional MODEL child
 * overrides the Task Tool's model.
 *
 * @author Ivica Cardic
 */
public class AiAgentUtilsSubagent {

    public static final ClusterElementDefinition<?> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.clusterElement("subagent")
            .title("Subagent")
            .description("A specialized agent the Task Tool can delegate to.")
            .type(SUBAGENT)
            .properties(
                string("subagentName")
                    .label("Name")
                    .description("Identifier the parent agent uses to select this subagent.")
                    .required(true),
                string("description")
                    .label("Description")
                    .description("When the parent agent should delegate to this subagent.")
                    .required(true),
                string("instructions")
                    .label("Instructions")
                    .description("System prompt for this subagent.")
                    .required(true));

    private AiAgentUtilsSubagent() {
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :server:libs:modules:components:ai:agent:utils:test --tests "*AiAgentUtilsSubagentTest*"`
Expected: PASS (2 tests)

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ai/agent/SubagentFunction.java \
        server/libs/modules/components/ai/agent/utils/src/main/java/com/bytechef/component/ai/agent/utils/cluster/AiAgentUtilsSubagent.java \
        server/libs/modules/components/ai/agent/utils/src/test/java/com/bytechef/component/ai/agent/utils/cluster/AiAgentUtilsSubagentTest.java
git commit -m "4482 Add SUBAGENT cluster element type and definition"
```

---

### Task 2: Resolve SUBAGENT cluster elements into subagent definitions

**Files:**
- Create: `.../utils/cluster/subagent/ByteChefSubagentDefinition.java`
- Create: `.../utils/cluster/subagent/ByteChefSubagentResolver.java`
- Test: `.../utils/cluster/subagent/ByteChefSubagentResolverTest.java`

**Interfaces:**
- Consumes: `SubagentReference(String uri, String kind, Map<String, String> metadata)`; `SubagentDefinition` with `getName()`, `getDescription()`, `getKind()`, `getReference()`; `SubagentResolver` with `canResolve(SubagentReference)`, `resolve(SubagentReference)`; `ClusterElement.getParameters()`, `getWorkflowNodeName()`, `getExtensions()`
- Produces: `ByteChefSubagentDefinition.KIND` (`"bytechef"`), `ByteChefSubagentDefinition.of(ClusterElement)`, and `new ByteChefSubagentResolver(List<ClusterElement>)`

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.component.ai.agent.utils.cluster.subagent;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.configuration.domain.ClusterElement;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.common.task.subagent.SubagentDefinition;
import org.springaicommunity.agent.common.task.subagent.SubagentReference;

/**
 * @author Ivica Cardic
 */
class ByteChefSubagentResolverTest {

    @Test
    void testResolvesAttachedSubagentByWorkflowNodeName() {
        ClusterElement clusterElement = clusterElement("subagent_1", "Analyst", "Answers questions");

        ByteChefSubagentResolver resolver = new ByteChefSubagentResolver(List.of(clusterElement));

        SubagentReference reference = new SubagentReference("subagent_1", ByteChefSubagentDefinition.KIND);

        assertThat(resolver.canResolve(reference)).isTrue();

        SubagentDefinition subagentDefinition = resolver.resolve(reference);

        assertThat(subagentDefinition.getName()).isEqualTo("Analyst");
        assertThat(subagentDefinition.getDescription()).isEqualTo("Answers questions");
        assertThat(subagentDefinition.getKind()).isEqualTo(ByteChefSubagentDefinition.KIND);
    }

    @Test
    void testCannotResolveUnknownUriOrForeignKind() {
        ByteChefSubagentResolver resolver = new ByteChefSubagentResolver(
            List.of(clusterElement("subagent_1", "Analyst", "Answers questions")));

        assertThat(resolver.canResolve(new SubagentReference("subagent_2", ByteChefSubagentDefinition.KIND)))
            .isFalse();
        assertThat(resolver.canResolve(new SubagentReference("subagent_1", "claude")))
            .isFalse();
    }

    private static ClusterElement clusterElement(String workflowNodeName, String name, String description) {
        return new ClusterElement(
            null, null, Map.of(), null, "aiAgentUtils/v1/subagent",
            Map.of("subagentName", name, "description", description, "instructions", "Do the thing."),
            workflowNodeName);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:modules:components:ai:agent:utils:test --tests "*ByteChefSubagentResolverTest*"`
Expected: FAIL — `cannot find symbol: class ByteChefSubagentResolver`

- [ ] **Step 3: Add the common SPI dependency**

In `server/libs/modules/components/ai/agent/utils/build.gradle.kts`, add alongside the existing
`implementation(libs.org.springaicommunity.spring.ai.agent.utils)`:

```kotlin
    implementation("org.springaicommunity:spring-ai-agent-utils-common:0.10.0")
```

- [ ] **Step 4: Write the definition**

```java
package com.bytechef.component.ai.agent.utils.cluster.subagent;

import com.bytechef.platform.configuration.domain.ClusterElement;
import java.util.Map;
import org.springaicommunity.agent.common.task.subagent.SubagentDefinition;
import org.springaicommunity.agent.common.task.subagent.SubagentReference;

/**
 * A {@link SubagentDefinition} backed by a single SUBAGENT cluster element. The element's workflow node name is the
 * reference uri, so a resolver can map a reference back to the element that produced it.
 *
 * @author Ivica Cardic
 */
public class ByteChefSubagentDefinition implements SubagentDefinition {

    public static final String KIND = "bytechef";

    private final ClusterElement clusterElement;
    private final String description;
    private final String name;

    private ByteChefSubagentDefinition(ClusterElement clusterElement, String name, String description) {
        this.clusterElement = clusterElement;
        this.description = description;
        this.name = name;
    }

    public static ByteChefSubagentDefinition of(ClusterElement clusterElement) {
        Map<String, ?> parameters = clusterElement.getParameters();

        Object name = parameters.get("subagentName");
        Object description = parameters.get("description");

        return new ByteChefSubagentDefinition(
            clusterElement, name == null ? clusterElement.getWorkflowNodeName() : name.toString(),
            description == null ? "" : description.toString());
    }

    public ClusterElement getClusterElement() {
        return clusterElement;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getKind() {
        return KIND;
    }

    public String getInstructions() {
        Object instructions = clusterElement.getParameters()
            .get("instructions");

        return instructions == null ? "" : instructions.toString();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public SubagentReference getReference() {
        return new SubagentReference(clusterElement.getWorkflowNodeName(), KIND);
    }
}
```

- [ ] **Step 5: Write the resolver**

```java
package com.bytechef.component.ai.agent.utils.cluster.subagent;

import com.bytechef.platform.configuration.domain.ClusterElement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springaicommunity.agent.common.task.subagent.SubagentDefinition;
import org.springaicommunity.agent.common.task.subagent.SubagentReference;
import org.springaicommunity.agent.common.task.subagent.SubagentResolver;

/**
 * Resolves references minted from SUBAGENT cluster elements attached to one Task Tool instance. Fails closed: a
 * reference naming an unattached subagent, or one of another kind, is not resolvable.
 *
 * @author Ivica Cardic
 */
public class ByteChefSubagentResolver implements SubagentResolver {

    private final Map<String, ByteChefSubagentDefinition> subagentDefinitions;

    public ByteChefSubagentResolver(List<ClusterElement> clusterElements) {
        Map<String, ByteChefSubagentDefinition> definitions = new LinkedHashMap<>();

        for (ClusterElement clusterElement : clusterElements) {
            definitions.put(clusterElement.getWorkflowNodeName(), ByteChefSubagentDefinition.of(clusterElement));
        }

        this.subagentDefinitions = definitions;
    }

    @Override
    public boolean canResolve(SubagentReference subagentReference) {
        return Objects.equals(subagentReference.kind(), ByteChefSubagentDefinition.KIND) &&
            subagentDefinitions.containsKey(subagentReference.uri());
    }

    public List<SubagentReference> getReferences() {
        return subagentDefinitions.values()
            .stream()
            .map(SubagentDefinition::getReference)
            .toList();
    }

    @Override
    public SubagentDefinition resolve(SubagentReference subagentReference) {
        ByteChefSubagentDefinition subagentDefinition = subagentDefinitions.get(subagentReference.uri());

        if (subagentDefinition == null) {
            throw new IllegalArgumentException(
                "No subagent attached for reference '%s'".formatted(subagentReference.uri()));
        }

        return subagentDefinition;
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew :server:libs:modules:components:ai:agent:utils:test --tests "*ByteChefSubagentResolverTest*"`
Expected: PASS (2 tests)

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add server/libs/modules/components/ai/agent/utils/
git commit -m "4482 Resolve SUBAGENT cluster elements into subagent definitions"
```

---

### Task 3: Execute a subagent with its own model and tools

**Files:**
- Create: `.../utils/cluster/subagent/ByteChefSubagentExecutor.java`
- Test: `.../utils/cluster/subagent/ByteChefSubagentExecutorTest.java`

**Interfaces:**
- Consumes: `SubagentExecutor` with `String getKind()` and `String execute(TaskCall, SubagentDefinition)`; `TaskCall(description, prompt, subagent_type, model, resume, run_in_background)`; `ByteChefSubagentDefinition.getInstructions()`, `getClusterElement()`
- Produces: `new ByteChefSubagentExecutor(ChatModel defaultChatModel, Function<ClusterElement, List<ToolCallback>> toolCallbackResolver)`

The tool resolver is injected as a function so this class stays testable without a Spring context; Task 4 supplies the real implementation.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.component.ai.agent.utils.cluster.subagent;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.configuration.domain.ClusterElement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.common.task.subagent.TaskCall;

/**
 * @author Ivica Cardic
 */
class ByteChefSubagentExecutorTest {

    @Test
    void testExecutorReportsByteChefKind() {
        ByteChefSubagentExecutor executor = new ByteChefSubagentExecutor(null, clusterElement -> List.of());

        assertThat(executor.getKind()).isEqualTo(ByteChefSubagentDefinition.KIND);
    }

    @Test
    void testResolvesToolsOnlyFromTheSubagentsOwnElement() {
        AtomicReference<ClusterElement> resolvedElement = new AtomicReference<>();

        ByteChefSubagentExecutor executor = new ByteChefSubagentExecutor(null, clusterElement -> {
            resolvedElement.set(clusterElement);

            return List.of();
        });

        ClusterElement clusterElement = new ClusterElement(
            null, null, Map.of(), null, "aiAgentUtils/v1/subagent",
            Map.of("subagentName", "Analyst", "description", "Reads", "instructions", "Answer."), "subagent_1");

        ByteChefSubagentDefinition subagentDefinition = ByteChefSubagentDefinition.of(clusterElement);

        TaskCall taskCall = new TaskCall("d", "p", "Analyst", null, null, Boolean.FALSE);

        // No ChatModel is configured, so execution fails before any model call — what this pins is that the executor
        // asked for tools using this subagent's own element and no other.
        try {
            executor.execute(taskCall, subagentDefinition);
        } catch (RuntimeException runtimeException) {
            // expected: no model configured
        }

        assertThat(resolvedElement.get()).isSameAs(clusterElement);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:modules:components:ai:agent:utils:test --tests "*ByteChefSubagentExecutorTest*"`
Expected: FAIL — `cannot find symbol: class ByteChefSubagentExecutor`

- [ ] **Step 3: Write the executor**

```java
package com.bytechef.component.ai.agent.utils.cluster.subagent;

import com.bytechef.platform.configuration.domain.ClusterElement;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.agent.common.task.subagent.SubagentDefinition;
import org.springaicommunity.agent.common.task.subagent.SubagentExecutor;
import org.springaicommunity.agent.common.task.subagent.TaskCall;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

/**
 * Runs a builder-defined subagent. The subagent's system prompt, tools and model all come from its own SUBAGENT cluster
 * element, so it can call nothing the builder did not attach to it.
 *
 * @author Ivica Cardic
 */
public class ByteChefSubagentExecutor implements SubagentExecutor {

    private final @Nullable ChatModel defaultChatModel;
    private final Function<ClusterElement, List<ToolCallback>> toolCallbackResolver;

    public ByteChefSubagentExecutor(
        @Nullable ChatModel defaultChatModel, Function<ClusterElement, List<ToolCallback>> toolCallbackResolver) {

        this.defaultChatModel = defaultChatModel;
        this.toolCallbackResolver = toolCallbackResolver;
    }

    @Override
    public String execute(TaskCall taskCall, SubagentDefinition subagentDefinition) {
        if (!(subagentDefinition instanceof ByteChefSubagentDefinition byteChefSubagentDefinition)) {
            throw new IllegalArgumentException(
                "Unsupported subagent definition: " + subagentDefinition.getClass());
        }

        List<ToolCallback> toolCallbacks = toolCallbackResolver.apply(
            byteChefSubagentDefinition.getClusterElement());

        if (defaultChatModel == null) {
            throw new IllegalStateException(
                "Subagent '%s' has no model. Attach a Model to the Task Tool or to the subagent."
                    .formatted(byteChefSubagentDefinition.getName()));
        }

        ChatClient chatClient = ChatClient.builder(defaultChatModel)
            .defaultToolCallbacks(toolCallbacks)
            .build();

        return chatClient.prompt()
            .system(byteChefSubagentDefinition.getInstructions())
            .user(taskCall.prompt())
            .call()
            .content();
    }

    @Override
    public String getKind() {
        return ByteChefSubagentDefinition.KIND;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :server:libs:modules:components:ai:agent:utils:test --tests "*ByteChefSubagentExecutorTest*"`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/libs/modules/components/ai/agent/utils/
git commit -m "4482 Add ByteChef subagent executor scoped to its own cluster element"
```

---

### Task 4: Rewire the Task Tool and drop the Claude subagents

**Files:**
- Modify: `.../utils/cluster/AiAgentUtilsTaskTool.java`
- Modify: `.../utils/AiAgentUtilsComponentHandler.java`
- Modify: `server/libs/modules/components/ai/agent/utils/build.gradle.kts`
- Test: `.../utils/cluster/AiAgentUtilsTaskToolTest.java`

**Interfaces:**
- Consumes: `ByteChefSubagentResolver`, `ByteChefSubagentExecutor`, `SubagentType(SubagentResolver, SubagentExecutor)`, `TaskTool.builder().subagentTypes(...).subagentReferences(...).build()`, `AiAgentToolFacade.getFunctionToolCallback(ClusterElement, Map<String, ComponentConnection>, boolean)`, `((ActionContextAware) context).isEditorEnvironment()`
- Produces: a `taskTool` cluster element whose subagents come only from attached SUBAGENT elements

- [ ] **Step 1: Write the failing regression test**

This is the test that pins the whole point of the change.

```java
package com.bytechef.component.ai.agent.utils.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.ai.agent.SubagentFunction;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AiAgentUtilsTaskToolTest {

    @Test
    void testTaskToolNeverReferencesClaudeSubagentTypes() throws Exception {
        String source = java.nio.file.Files.readString(
            java.nio.file.Path.of(
                "src/main/java/com/bytechef/component/ai/agent/utils/cluster/AiAgentUtilsTaskTool.java"));

        assertThat(source).doesNotContain("ClaudeSubagentType");
        assertThat(source).doesNotContain("ShellTools");
        assertThat(source).doesNotContain("FileSystemTools");
    }

    @Test
    void testTaskToolDeclaresSubagentAsAcceptedChildType() {
        assertThat(SubagentFunction.SUBAGENT.multipleElements()).isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:modules:components:ai:agent:utils:test --tests "*AiAgentUtilsTaskToolTest*"`
Expected: FAIL — source still contains `ClaudeSubagentType`

- [ ] **Step 3: Add the agent module dependency**

`ai/agent` does not depend on `ai/agent/utils`, so this introduces no cycle. In
`server/libs/modules/components/ai/agent/utils/build.gradle.kts`:

```kotlin
    implementation(project(":server:libs:modules:components:ai:agent"))
```

- [ ] **Step 4: Rewire the Task Tool**

Replace the `apply` body's subagent construction. The `resolveChatModel` helper stays exactly as it is.

```java
    private ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections, Context context) throws Exception {

        ChatModel chatModel = resolveChatModel(extensions, componentConnections);

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        List<ClusterElement> subagentClusterElements = clusterElementMap.getClusterElements(SUBAGENT);

        ByteChefSubagentResolver subagentResolver = new ByteChefSubagentResolver(subagentClusterElements);

        boolean editorEnvironment = ((ActionContextAware) context).isEditorEnvironment();

        ByteChefSubagentExecutor subagentExecutor = new ByteChefSubagentExecutor(
            chatModel,
            subagentClusterElement -> buildSubagentToolCallbacks(
                subagentClusterElement, componentConnections, editorEnvironment));

        ToolCallback taskToolCallback = TaskTool.builder()
            .subagentTypes(new SubagentType(subagentResolver, subagentExecutor))
            .subagentReferences(subagentResolver.getReferences())
            .build();

        return ToolCallbackProvider.from(taskToolCallback);
    }
```

Add the constructor dependency and imports:

```java
    private final AiAgentToolFacade aiAgentToolFacade;
    private final ClusterElementDefinitionService clusterElementDefinitionService;

    @SuppressFBWarnings("EI")
    public AiAgentUtilsTaskTool(
        AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService) {

        this.aiAgentToolFacade = aiAgentToolFacade;
        this.clusterElementDefinitionService = clusterElementDefinitionService;

        this.clusterElementDefinition =
            ComponentDsl.<MultipleConnectionsToolCallbackProviderFunction>clusterElement("taskTool")
                .title("Task Tool")
                .description("Delegate tasks to subagents you define, each limited to the tools you attach to it.")
                .type(TOOLS)
                .object(() -> this::apply);
    }
```

Remove the now-unused imports `org.springaicommunity.agent.tools.task.claude.ClaudeSubagentType` and
`org.springframework.ai.chat.client.ChatClient`.

Add the helper that turns one subagent's nested TOOLS children into callbacks. It mirrors the
dispatch in `AbstractAiAgentChatAction.buildElementToolCallbacks`, which is private there — a
subagent may attach either provider-style tools (the `aiAgentUtils` tools themselves) or ordinary
component actions, and both must work:

```java
    private List<ToolCallback> buildSubagentToolCallbacks(
        ClusterElement subagentClusterElement, Map<String, ComponentConnection> componentConnections,
        boolean editorEnvironment, Context context) {

        ClusterElementMap subagentClusterElementMap = ClusterElementMap.of(subagentClusterElement.getExtensions());

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (ClusterElement toolClusterElement : subagentClusterElementMap.getClusterElements(TOOLS)) {
            Object clusterElementFunction = clusterElementDefinitionService.getClusterElement(
                toolClusterElement.getComponentName(), toolClusterElement.getComponentVersion(),
                toolClusterElement.getClusterElementName());

            if (clusterElementFunction instanceof MultipleConnectionsToolCallbackProviderFunction providerFunction) {
                ToolCallbackProvider toolCallbackProvider = providerFunction.apply(
                    ParametersFactory.create(toolClusterElement.getParameters()),
                    ParametersFactory.create(Map.of()),
                    ParametersFactory.create(toolClusterElement.getExtensions()), componentConnections, context);

                toolCallbacks.addAll(Arrays.asList(toolCallbackProvider.getToolCallbacks()));
            } else {
                toolCallbacks.add(
                    aiAgentToolFacade.getFunctionToolCallback(
                        toolClusterElement, componentConnections, editorEnvironment));
            }
        }

        return toolCallbacks;
    }
```

Note the scoping this enforces: the map is built from **this subagent's** extensions, so a subagent
can only ever receive tools attached beneath it.

- [ ] **Step 5: Register the subagent element and pass the facade**

In `AiAgentUtilsComponentHandler`, add `AiAgentToolFacade` to the constructor signature:

```java
    public AiAgentUtilsComponentHandler(
        AiAgentToolFacade aiAgentToolFacade, AiSkillFacade aiSkillFacade,
        List<AiAgentUtilsClusterElementContributor> clusterElementContributors,
        ClusterElementDefinitionService clusterElementDefinitionService, AiAutoMemoryService aiAutoMemoryService) {
```

Pass it when constructing the Task Tool:

```java
        AiAgentUtilsTaskTool agentUtilsTaskTool = new AiAgentUtilsTaskTool(
            aiAgentToolFacade, clusterElementDefinitionService);
```

And register the new element in the `clusterElements` list, immediately after
`agentUtilsTaskTool.clusterElementDefinition`:

```java
            agentUtilsTaskTool.clusterElementDefinition,
            AiAgentUtilsSubagent.CLUSTER_ELEMENT_DEFINITION,
```

- [ ] **Step 6: Run the full module test suite**

Run: `./gradlew :server:libs:modules:components:ai:agent:utils:test`
Expected: PASS, including the two new `AiAgentUtilsTaskToolTest` cases

- [ ] **Step 7: Verify the component still assembles**

Run: `./gradlew :server:libs:modules:components:ai:agent:utils:compileJava :server:apps:server-app:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
./gradlew spotlessApply
git add server/libs/modules/components/ai/agent/utils/
git commit -m "4482 Build Task Tool subagents from SUBAGENT cluster elements"
```

---

## Verification

After Task 4, confirm by inspection that `ClaudeSubagentType` has no remaining construction site:

```bash
grep -rn "ClaudeSubagentType\|ShellTools\|FileSystemTools" \
  server/libs/modules/components/ai/agent/utils/src/main/java/com/bytechef/component/ai/agent/utils/cluster/AiAgentUtilsTaskTool.java
```

Expected: no matches. The standalone `AiAgentUtilsShellTools` and `AiAgentUtilsFileSystemTools`
cluster elements remain registered and are deliberately out of scope — which tools a builder grants
an agent is the builder's decision.

## Known deviation from the spec

The spec's architecture diagram gives each SUBAGENT an optional `MODEL` child overriding the Task
Tool's model. Phase 1 does **not** implement that: `ByteChefSubagentExecutor` takes a single
`defaultChatModel` resolved once from the Task Tool's own MODEL child, and every subagent shares it.

This is deliberate scope control, not an oversight — per-subagent model selection is a convenience,
while builder-defined *tools* are the point of the change. Adding it later means widening the
executor's second constructor argument to a `Function<ClusterElement, ChatModel>` that resolves each
subagent's own MODEL and falls back to the Task Tool's; `resolveChatModel` already contains the
resolution logic and would be reused unchanged. Update the spec if this stays unimplemented after
Phase 2.

## Phase 2 — done

`ByteChefTaskRepository` wraps `DefaultTaskRepository`; `TaskOutputTool` is exposed alongside the task
tool callback so the model can read background results. Delivered as planned, plus one thing the spec
did not anticipate:

**Tenant propagation.** Tenant id is a thread local that selects the database schema. A task handed to
the executor runs on a pool thread that never had it, so any tool the subagent called would read the
wrong schema — or none. The repository captures the tenant on the submitting thread and reinstates it
around the task body via `TenantContext.callWithTenantId`. This was not in the design; it surfaced
only when deciding what actually crosses the thread boundary.

Two lifetime decisions worth keeping: the repository is built **per invocation**, so background task
ids cannot leak between runs; and the executor is **shared and virtual-threaded**, constructed once on
the singleton component handler, which is why the repository passes `ownsExecutor = false` and must
never shut it down.
