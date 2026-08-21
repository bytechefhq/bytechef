# Durable Sub-workflow Execution for AI Agent Tools — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When an AI agent calls a workflow as a tool, run the sub-workflow as a real asynchronous job; the agent suspends while it runs and resumes its LLM turn with the result — so suspending sub-workflow steps (Request Approval / Wait) are honored instead of silently swallowed.

**Architecture:** Replace the blocking in-process `JobSyncExecutor` with two independent jobs joined by an agent-agnostic coordinator bridge. `WorkflowCallWorkflowTool` suspends the agent (storing a `PendingSubflowRequest` in `Suspend.continueParameters`); `AgentSubflowLauncher` (a `JobStatusApplicationEvent` listener) starts the sub-workflow as a top-level job once the agent is durably `STOPPED` (race-free deferred launch); `AgentSubflowResumeListener` resumes the agent via `JobFacade.resumeJob` when the sub-workflow terminates. Builds on the resumable agent tool-calling loop from `docs/superpowers/specs/2026-05-20-resumable-agent-tool-calls-design.md`.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JDBC, Atlas workflow engine, JUnit 5, Mockito, Testcontainers, Gradle.

**Spec:** `docs/superpowers/specs/2026-05-21-agent-tool-durable-subflow-design.md`

**Key facts verified against the codebase (do not re-derive):**
- `SubflowSyncExecutor` has exactly one consumer: `WorkflowComponentHandler` → `WorkflowCallWorkflowTool`. Plus the test `WorkflowComponentHandlerTest`.
- `SuspendableToolCallingManager.executeToolCalls` already does `new HashMap<>(suspend.continueParameters())` — a tool's own `continueParameters` entries survive into the agent's final `Suspend`. It also throws on two suspend sentinels in one turn.
- `JobServiceImpl.resumeToStatusStarted` asserts `parentTaskExecutionId == null` — the sub-workflow MUST be a top-level job (`parentTaskExecutionId` unset).
- `ToolFunction.apply(Parameters, Parameters, ClusterElementContext)` — the tool receives a `ClusterElementContext`.
- `ClusterElementContextImpl` (the impl) holds `private @Nullable ActionContext agentActionContext` and implements `ClusterElementContextAware`; the interface currently has **no** accessor for it.
- `MetadataConstants` (`com.bytechef.platform.component.constant`): `JOB_RESUME_ID="jobResumeId"`, `SUSPEND="suspend"`, `RESUME_DATA="__resumeData"`, `CALLABLE_RESPONSE="__callableResponse"`.
- Job-metadata write pattern (`SuspendTaskCompletionHandler`): `Map<String,Object> m = new HashMap<>(job.getMetadata()); m.put(k,v); job.setMetadata(m); jobService.update(job);`
- `JobParametersDTO(String workflowId, Map<String,?> inputs, Map<String,?> metadata)` constructor leaves `parentTaskExecutionId` null.
- `SubflowResolver.resolveSubflow(workflowUuid, triggerName, editorEnvironment) → Subflow(String workflowId, String inputsName)`.
- `JobFacade.resumeJob(long id, @Nullable Map<String,?> data)` publishes `ResumeJobEvent` — the internal, no-token resume path.

---

## File Structure

| File | Responsibility |
|---|---|
| `platform-workflow-task-dispatcher-api/.../subflow/PendingSubflowRequest.java` (new) | The contract record stored in the agent's `Suspend.continueParameters`. |
| `platform-workflow-task-dispatcher-api/.../subflow/SubflowRequestConstants.java` (new) | Metadata/continueParameters key constants. |
| `platform-workflow-task-dispatcher-api/.../subflow/ChildJobPrincipalFactory.java` (modify) | Add `createPrincipalLinkedJob` — a top-level job linked to a reference job's principal. |
| `automation-configuration-service/.../subflow/ChildJobPrincipalFactoryImpl.java` (modify) | Implement `createPrincipalLinkedJob`. |
| `platform-component-api/.../definition/ClusterElementContextAware.java` (modify) | Add `getAgentActionContext()` accessor. |
| `platform-component-context-service/.../ClusterElementContextImpl.java` (modify) | Implement `getAgentActionContext()`. |
| `components/workflow/.../cluster/WorkflowCallWorkflowTool.java` (modify) | Suspend the agent with a `PendingSubflowRequest` instead of calling `SubflowSyncExecutor`. |
| `components/workflow/.../WorkflowComponentHandler.java` (modify) | Inject `SubflowResolver` instead of `SubflowSyncExecutor`. |
| `components/workflow/.../subflow/sync/SubflowSyncExecutor.java` (delete) | Obsolete. |
| `components/workflow/.../subflow/sync/config/WorkflowSubflowSyncExecutorConfiguration.java` (delete) | Obsolete. |
| `task-dispatchers/subflow/.../event/listener/AgentSubflowLauncher.java` (new) | On agent job `STOPPED` with a pending request, start the sub-workflow job. |
| `task-dispatchers/subflow/.../event/listener/AgentSubflowResumeListener.java` (new) | On sub-workflow job `COMPLETED`/`FAILED`, resume the agent job. |
| `task-dispatchers/subflow/.../config/SubflowTaskDispatcherConfiguration.java` (modify) | Register the two new listeners as coordinator beans. |

---

## Task 1: Contract types — `PendingSubflowRequest` + `SubflowRequestConstants`

**Files:**
- Create: `server/libs/platform/platform-workflow/platform-workflow-task-dispatcher/platform-workflow-task-dispatcher-api/src/main/java/com/bytechef/platform/workflow/task/dispatcher/subflow/PendingSubflowRequest.java`
- Create: `server/libs/platform/platform-workflow/platform-workflow-task-dispatcher/platform-workflow-task-dispatcher-api/src/main/java/com/bytechef/platform/workflow/task/dispatcher/subflow/SubflowRequestConstants.java`
- Test: `server/libs/platform/platform-workflow/platform-workflow-task-dispatcher/platform-workflow-task-dispatcher-api/src/test/java/com/bytechef/platform/workflow/task/dispatcher/subflow/PendingSubflowRequestTest.java`

- [ ] **Step 1: Write the failing test**

`PendingSubflowRequest` is persisted inside `Suspend.continueParameters` → `TaskState`, which round-trips through Jackson. This test pins that it survives a serialize/deserialize cycle.

```java
package com.bytechef.platform.workflow.task.dispatcher.subflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.platform.constant.PlatformType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PendingSubflowRequestTest {

    @Test
    void testJacksonRoundTrip() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        PendingSubflowRequest request = new PendingSubflowRequest(
            "wf-123", "newWorkflowCall", Map.of("amount", 42), false, PlatformType.AUTOMATION);

        String json = objectMapper.writeValueAsString(request);
        PendingSubflowRequest result = objectMapper.readValue(json, PendingSubflowRequest.class);

        assertEquals(request, result);
    }

    @Test
    void testConvertValueFromMap() {
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> map = Map.of(
            "workflowId", "wf-123", "inputsName", "newWorkflowCall", "inputs", Map.of("amount", 42),
            "editorEnvironment", false, "platformType", "AUTOMATION");

        PendingSubflowRequest result = objectMapper.convertValue(map, PendingSubflowRequest.class);

        assertEquals("wf-123", result.workflowId());
        assertEquals("newWorkflowCall", result.inputsName());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-api:test --tests "*PendingSubflowRequestTest"`
Expected: FAIL — compilation error, `PendingSubflowRequest` does not exist.

(If the api module has no `src/test` source set or no Jackson test dependency, add a `testImplementation` for `com.fasterxml.jackson.core:jackson-databind` to its `build.gradle.kts`, matching how sibling api modules declare test deps.)

- [ ] **Step 3: Write the contract types**

`PendingSubflowRequest.java`:

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

package com.bytechef.platform.workflow.task.dispatcher.subflow;

import com.bytechef.platform.constant.PlatformType;
import java.util.Map;

/**
 * The request an AI agent's Call Workflow tool stores in its {@code Suspend.continueParameters} so the coordinator
 * bridge can launch the sub-workflow as a top-level job after the agent is suspended.
 *
 * @author Ivica Cardic
 */
public record PendingSubflowRequest(
    String workflowId, String inputsName, Map<String, ?> inputs, boolean editorEnvironment,
    PlatformType platformType) {
}
```

`SubflowRequestConstants.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * ... (full ByteChef Apache header) ...
 */

package com.bytechef.platform.workflow.task.dispatcher.subflow;

/**
 * Keys for the agent-tool durable sub-workflow bridge.
 *
 * @author Ivica Cardic
 */
public final class SubflowRequestConstants {

    /** Key in the agent's {@code Suspend.continueParameters} holding a {@link PendingSubflowRequest}. */
    public static final String PENDING_SUBFLOW = "__bytechef_pending_subflow__";

    /** Key in the sub-workflow job's metadata holding the agent job id (a {@code Long}). */
    public static final String AGENT_JOB_ID = "__bytechef_agent_job_id__";

    /** Key in the agent job's metadata holding the launched sub-workflow job id (idempotency guard). */
    public static final String LAUNCHED_SUBFLOW_JOB_ID = "__bytechef_launched_subflow_job_id__";

    private SubflowRequestConstants() {
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-api:test --tests "*PendingSubflowRequestTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-workflow/platform-workflow-task-dispatcher/platform-workflow-task-dispatcher-api/
git commit -m "$(cat <<'EOF'
5055 Add PendingSubflowRequest contract for agent-tool sub-workflow bridge

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: Expose the agent `ActionContext` on `ClusterElementContextAware`

The Call Workflow tool is a `ToolFunction` and receives a `ClusterElementContext`. To suspend the agent it needs the agent's `ActionContext`, which `ClusterElementContextImpl` already holds as `agentActionContext` but does not expose.

**Files:**
- Modify: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/ClusterElementContextAware.java`
- Modify: `server/libs/platform/platform-component/platform-component-context/platform-component-context-service/src/main/java/com/bytechef/platform/component/context/ClusterElementContextImpl.java`

- [ ] **Step 1: Add the accessor to the interface**

In `ClusterElementContextAware`, add the import `com.bytechef.component.definition.ActionContext` and `org.jspecify.annotations.Nullable`, and add the method:

```java
    /**
     * Returns the {@link ActionContext} of the AI agent action that invoked this cluster element tool, or
     * {@code null} when the cluster element was not invoked as an agent tool.
     *
     * @return the agent's action context, or {@code null}
     */
    @Nullable
    ActionContext getAgentActionContext();
```

- [ ] **Step 2: Implement it in `ClusterElementContextImpl`**

`ClusterElementContextImpl` already has `private final @Nullable ActionContext agentActionContext;`. Add:

```java
    @Override
    public @Nullable ActionContext getAgentActionContext() {
        return agentActionContext;
    }
```

- [ ] **Step 3: Find other implementers**

Run: `grep -rln "implements ClusterElementContextAware\|ClusterElementContextAware {" server --include='*.java'`
Expected: only `ClusterElementContextImpl`. If a test stub or other implementer exists, add the same method returning `null` there.

- [ ] **Step 4: Compile**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-api:compileJava :server:libs:platform:platform-component:platform-component-context:platform-component-context-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-component/platform-component-api/ server/libs/platform/platform-component/platform-component-context/
git commit -m "$(cat <<'EOF'
5055 Expose agent ActionContext on ClusterElementContextAware

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Rewrite `WorkflowCallWorkflowTool` to suspend the agent

**Files:**
- Modify: `server/libs/modules/components/workflow/src/main/java/com/bytechef/component/workflow/cluster/WorkflowCallWorkflowTool.java`
- Modify: `server/libs/modules/components/workflow/src/main/java/com/bytechef/component/workflow/WorkflowComponentHandler.java`
- Delete: `server/libs/modules/components/workflow/src/main/java/com/bytechef/component/workflow/subflow/sync/SubflowSyncExecutor.java`
- Delete: `server/libs/modules/components/workflow/src/main/java/com/bytechef/component/workflow/subflow/sync/config/WorkflowSubflowSyncExecutorConfiguration.java`
- Modify: `server/libs/modules/components/workflow/src/test/java/com/bytechef/component/workflow/WorkflowComponentHandlerTest.java`
- Test: `server/libs/modules/components/workflow/src/test/java/com/bytechef/component/workflow/cluster/WorkflowCallWorkflowToolTest.java` (new)

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.component.workflow.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.ai.agent.ToolFunction;
import com.bytechef.platform.ai.constant.ToolSuspendConstants;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import com.bytechef.platform.workflow.task.dispatcher.subflow.PendingSubflowRequest;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowRequestConstants;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowDataSource;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver.Subflow;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

// An ActionContext that records the Suspend it received. The real context implements both
// ActionContext and ActionContextAware; the test double mirrors that.
class WorkflowCallWorkflowToolTest {

    interface TestAgentContext extends ActionContext, ActionContextAware {
    }

    @Test
    void testToolSuspendsWithPendingSubflowRequest() throws Exception {
        SubflowDataSource subflowDataSource = mock(SubflowDataSource.class);
        SubflowResolver subflowResolver = mock(SubflowResolver.class);

        when(subflowResolver.resolveSubflow("uuid-1", "newWorkflowCall", false))
            .thenReturn(new Subflow("wf-99", "newWorkflowCall"));

        AtomicReference<ActionContext.Suspend> suspendRef = new AtomicReference<>();

        TestAgentContext agentContext = mock(TestAgentContext.class);
        when(agentContext.getSuspend()).thenReturn(null);
        org.mockito.Mockito.doAnswer(invocation -> {
            suspendRef.set(invocation.getArgument(0));
            return null;
        })
            .when(agentContext)
            .suspend(org.mockito.ArgumentMatchers.any());

        ClusterElementContextAware context = mock(
            ClusterElementContextAware.class,
            org.mockito.Mockito.withSettings().extraInterfaces(ClusterElementContext.class));
        when(context.getAgentActionContext()).thenReturn(agentContext);
        when(((ClusterElementContext) context).isEditorEnvironment()).thenReturn(false);

        ToolFunction toolFunction = getToolFunction(subflowDataSource, subflowResolver);

        Parameters inputParameters = com.bytechef.component.test.definition.MockParametersFactory.create(
            Map.of("workflowUuid", "uuid-1", "inputs", Map.of("amount", 5)));

        Object result = toolFunction.apply(
            inputParameters, com.bytechef.component.test.definition.MockParametersFactory.create(Map.of()),
            (ClusterElementContext) context);

        assertEquals(ToolSuspendConstants.SUSPENDED_SENTINEL, result);

        ActionContext.Suspend suspend = suspendRef.get();
        assertNotNull(suspend);

        Object pending = suspend.continueParameters()
            .get(SubflowRequestConstants.PENDING_SUBFLOW);
        assertInstanceOf(PendingSubflowRequest.class, pending);
        assertEquals("wf-99", ((PendingSubflowRequest) pending).workflowId());
    }

    @Test
    void testToolReturnsErrorWhenSuspendAlreadyPending() throws Exception {
        SubflowDataSource subflowDataSource = mock(SubflowDataSource.class);
        SubflowResolver subflowResolver = mock(SubflowResolver.class);

        TestAgentContext agentContext = mock(TestAgentContext.class);
        when(agentContext.getSuspend()).thenReturn(new ActionContext.Suspend(Map.of(), null));

        ClusterElementContextAware context = mock(
            ClusterElementContextAware.class,
            org.mockito.Mockito.withSettings().extraInterfaces(ClusterElementContext.class));
        when(context.getAgentActionContext()).thenReturn(agentContext);

        ToolFunction toolFunction = getToolFunction(subflowDataSource, subflowResolver);

        Object result = toolFunction.apply(
            com.bytechef.component.test.definition.MockParametersFactory.create(Map.of("workflowUuid", "uuid-1")),
            com.bytechef.component.test.definition.MockParametersFactory.create(Map.of()),
            (ClusterElementContext) context);

        assertInstanceOf(String.class, result);
        org.junit.jupiter.api.Assertions.assertTrue(((String) result).toLowerCase().contains("one"));
    }

    // Reaches the package-private tool function under test.
    private static ToolFunction getToolFunction(
        SubflowDataSource subflowDataSource, SubflowResolver subflowResolver) {

        ClusterElementDefinition<ToolFunction> definition = WorkflowCallWorkflowTool.of(
            subflowDataSource, subflowResolver);

        return definition.getElement()
            .get();
    }
}
```

Note: align the `Parameters` factory and the `ClusterElementDefinition` element accessor with the conventions already used in `WorkflowComponentHandlerTest` and other component tests in this repo (e.g. how they obtain a `Parameters` instance and how they read a cluster element's `object()` supplier). Adjust the two helper calls if the repo uses a different factory/accessor name.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:modules:components:workflow:test --tests "*WorkflowCallWorkflowToolTest"`
Expected: FAIL — compilation errors (`WorkflowCallWorkflowTool.of` still takes `SubflowSyncExecutor`).

- [ ] **Step 3: Rewrite `WorkflowCallWorkflowTool`**

Replace the `SubflowSyncExecutor` import with `SubflowResolver` and rewrite `of(...)` and `getToolFunction(...)`. Keep `getWorkflowOptionsFunction`, `getOutputFunction`, `getPropertiesFunction`, and `toComponentProperty` unchanged.

```java
// --- imports: remove ---
// import com.bytechef.component.workflow.subflow.sync.SubflowSyncExecutor;
// --- imports: add ---
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.platform.ai.constant.ToolSuspendConstants;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import com.bytechef.platform.workflow.task.dispatcher.subflow.PendingSubflowRequest;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowRequestConstants;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver.Subflow;
import com.bytechef.platform.constant.PlatformType;
import java.util.HashMap;
```

Change the factory signature:

```java
    public static ClusterElementDefinition<ToolFunction> of(
        SubflowDataSource subflowDataSource, SubflowResolver subflowResolver) {

        return ComponentDsl.<ToolFunction>clusterElement("callWorkflow")
            .title("Call Workflow")
            .description("Calls another workflow as an AI agent tool.")
            .type(TOOLS)
            .properties(
                string(TOOL_NAME)
                    .label("Name")
                    .description("The tool name exposed to the AI model.")
                    .expressionEnabled(false)
                    .required(true),
                string(TOOL_DESCRIPTION)
                    .label("Description")
                    .description("The tool description exposed to the AI model.")
                    .controlType(TEXT_AREA)
                    .expressionEnabled(false)
                    .required(true),
                string(WORKFLOW_UUID)
                    .label("Workflow")
                    .description("The workflow to call when this tool is invoked.")
                    .options(getWorkflowOptionsFunction(subflowDataSource))
                    .required(true),
                dynamicProperties(INPUTS)
                    .description("The input parameters for the sub-workflow.")
                    .propertiesLookupDependsOn(WORKFLOW_UUID)
                    .properties(getPropertiesFunction(subflowDataSource)))
            .object(() -> getToolFunction(subflowResolver))
            .output(getOutputFunction(subflowDataSource));
    }
```

Replace `getToolFunction`:

```java
    private static ToolFunction getToolFunction(SubflowResolver subflowResolver) {
        return (inputParameters, connectionParameters, context) -> {
            ActionContext agentActionContext = resolveAgentActionContext(context);

            if (agentActionContext == null) {
                return "Error: the Call Workflow tool can only be used by an AI agent.";
            }

            ActionContextAware actionContextAware = (ActionContextAware) agentActionContext;

            if (actionContextAware.getSuspend() != null) {
                return "Error: another tool already suspended the agent in this turn; only one " +
                    "suspending tool call (including Call Workflow) is supported per turn.";
            }

            String workflowUuid = inputParameters.getRequiredString(WORKFLOW_UUID);
            Map<String, ?> inputs = inputParameters.getMap(INPUTS, Collections.emptyMap());

            boolean editorEnvironment = context.isEditorEnvironment();

            Subflow subflow = subflowResolver.resolveSubflow(workflowUuid, NEW_WORKFLOW_CALL, editorEnvironment);

            PendingSubflowRequest request = new PendingSubflowRequest(
                subflow.workflowId(), subflow.inputsName(), inputs, editorEnvironment, PlatformType.AUTOMATION);

            Map<String, Object> continueParameters = new HashMap<>();

            continueParameters.put(SubflowRequestConstants.PENDING_SUBFLOW, request);

            agentActionContext.suspend(new ActionContext.Suspend(continueParameters, null));

            return ToolSuspendConstants.SUSPENDED_SENTINEL;
        };
    }

    private static ActionContext resolveAgentActionContext(ClusterElementContext context) {
        if (context instanceof ClusterElementContextAware clusterElementContextAware) {
            return clusterElementContextAware.getAgentActionContext();
        }

        return null;
    }
```

- [ ] **Step 4: Update `WorkflowComponentHandler`**

```java
// import: remove com.bytechef.component.workflow.subflow.sync.SubflowSyncExecutor;
// import: add com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;

    public WorkflowComponentHandler(SubflowDataSource subflowDataSource, SubflowResolver subflowResolver) {
        this.componentDefinition = component(WORKFLOW)
            .title("Workflow")
            .description("Triggers and actions for workflow-to-workflow communication.")
            .icon("path:assets/workflow.svg")
            .categories(ComponentCategory.HELPERS)
            .triggers(WorkflowNewWorkflowCallTrigger.TRIGGER_DEFINITION)
            .actions(WorkflowResponseToWorkflowCallAction.ACTION_DEFINITION)
            .clusterElements(WorkflowCallWorkflowTool.of(subflowDataSource, subflowResolver));
    }
```

- [ ] **Step 5: Delete the obsolete sync executor**

```bash
git rm server/libs/modules/components/workflow/src/main/java/com/bytechef/component/workflow/subflow/sync/SubflowSyncExecutor.java
git rm server/libs/modules/components/workflow/src/main/java/com/bytechef/component/workflow/subflow/sync/config/WorkflowSubflowSyncExecutorConfiguration.java
```

Then check the `subflow/sync` package is empty and remove the now-empty directories. Search for any remaining import of either deleted class:
`grep -rln "subflow.sync" server/libs/modules/components/workflow/src --include='*.java'` — fix every hit.

- [ ] **Step 6: Update `WorkflowComponentHandlerTest`**

The test constructs `WorkflowComponentHandler` with a `SubflowSyncExecutor`. Replace that mock with a `SubflowResolver` mock:

```java
// was: new WorkflowComponentHandler(subflowDataSource, subflowSyncExecutor)
WorkflowComponentHandler handler = new WorkflowComponentHandler(
    mock(SubflowDataSource.class), mock(SubflowResolver.class));
```

Remove the `SubflowSyncExecutor` import and any related stubbing.

- [ ] **Step 7: Check the workflow `build.gradle.kts` dependency**

The module already depends on `platform-workflow-task-dispatcher-api` and `platform-component-api` (it imports `SubflowDataSource` and `WorkflowConstants`). Confirm `platform-ai-api` is on the path for `ToolSuspendConstants` (it already imports `com.bytechef.platform.ai.tool.constant.ToolConstants`, so `platform-ai-api` is present). No dependency change expected; if `compileJava` reports a missing class, add the matching `implementation(project(...))` line.

- [ ] **Step 8: Run the test to verify it passes**

Run: `./gradlew :server:libs:modules:components:workflow:test --tests "*WorkflowCallWorkflowToolTest" --tests "*WorkflowComponentHandlerTest"`
Expected: PASS.

- [ ] **Step 9: Regenerate the component definition snapshot**

The `callWorkflow` cluster element changed. Per the component-test convention:

```bash
rm -f server/libs/modules/components/workflow/src/test/resources/definition/workflow_v1.json
rm -rf server/libs/modules/components/workflow/build/resources/test/definition
./gradlew :server:libs:modules:components:workflow:test
```

Inspect the regenerated `workflow_v1.json` diff — it should reflect only the `callWorkflow` element, no unrelated churn.

- [ ] **Step 10: Commit**

```bash
./gradlew spotlessApply
git add server/libs/modules/components/workflow/
git commit -m "$(cat <<'EOF'
5055 Suspend the agent from the Call Workflow tool instead of blocking

Replaces the isolated JobSyncExecutor with a context.suspend() carrying
a PendingSubflowRequest; deletes SubflowSyncExecutor.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: `ChildJobPrincipalFactory.createPrincipalLinkedJob`

The launcher must create the sub-workflow as a **top-level** job (no `parentTaskExecutionId`) yet linked to the agent's principal instance. `ChildJobPrincipalFactory` is the existing api-level abstraction the subflow task-dispatcher module uses; extend it rather than depending on `PrincipalJobFacade` directly.

**Files:**
- Modify: `server/libs/platform/platform-workflow/platform-workflow-task-dispatcher/platform-workflow-task-dispatcher-api/src/main/java/com/bytechef/platform/workflow/task/dispatcher/subflow/ChildJobPrincipalFactory.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/subflow/ChildJobPrincipalFactoryImpl.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/subflow/ChildJobPrincipalFactoryImplTest.java` (new, or extend existing)

- [ ] **Step 1: Read the existing impl**

Read `ChildJobPrincipalFactoryImpl` and `PrincipalJobFacadeImpl` fully. Note exactly how `createChildJob` resolves the parent job's principal and `PlatformType` and calls `PrincipalJobFacade`. `createPrincipalLinkedJob` mirrors that, but calls `PrincipalJobFacade.createJob(jobParametersDTO, principalId, type)` (top-level) instead of `createChildJob`.

- [ ] **Step 2: Write the failing test**

```java
@Test
void testCreatePrincipalLinkedJobUsesReferenceJobPrincipal() {
    // Arrange: a reference (agent) job id whose principal resolves to principalId 7.
    // Mock PrincipalJobService / PrincipalJobFacade as the existing createChildJob test does.
    // Act: childJobPrincipalFactory.createPrincipalLinkedJob(referenceJobId, jobParametersDTO)
    // Assert: PrincipalJobFacade.createJob(jobParametersDTO, 7L, <type>) was invoked
    //         and the returned job id is propagated.
}
```

Model the mocks on the existing `createChildJob` test in this module (read it first; reuse its mock setup verbatim, swapping `createChildJob` for `createJob`).

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "*ChildJobPrincipalFactoryImplTest"`
Expected: FAIL — `createPrincipalLinkedJob` does not exist.

- [ ] **Step 4: Add the interface method**

In `ChildJobPrincipalFactory`:

```java
    /**
     * Creates a top-level job (no parent task execution) linked to the same principal instance as
     * {@code referenceJobId}. Used by the agent-tool sub-workflow bridge, where the sub-workflow must be
     * independently resumable and therefore cannot be a child job.
     *
     * @param referenceJobId   the job whose principal the new job is linked to
     * @param jobParametersDTO the new job's parameters
     * @return the created job id
     */
    long createPrincipalLinkedJob(long referenceJobId, JobParametersDTO jobParametersDTO);
```

- [ ] **Step 5: Implement it**

In `ChildJobPrincipalFactoryImpl`, mirror `createChildJob`'s principal/type resolution but call the non-child facade method. Sketch (adjust to the real `PrincipalJobFacadeImpl` API observed in Step 1):

```java
    @Override
    public long createPrincipalLinkedJob(long referenceJobId, JobParametersDTO jobParametersDTO) {
        return principalJobFacade.createJob(
            jobParametersDTO,
            principalJobService.getJobPrincipalId(referenceJobId, <resolvedType>),
            <resolvedType>);
    }
```

Resolve `<resolvedType>` the same way `createChildJob` does. If `createChildJob` delegates type resolution into `PrincipalJobFacadeImpl`, add a sibling method there (`createJob` linked by reference job) and keep this impl thin.

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "*ChildJobPrincipalFactoryImplTest"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-workflow/platform-workflow-task-dispatcher/platform-workflow-task-dispatcher-api/ server/libs/automation/automation-configuration/automation-configuration-service/
git commit -m "$(cat <<'EOF'
5055 Add ChildJobPrincipalFactory.createPrincipalLinkedJob

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: `AgentSubflowLauncher`

**Files:**
- Create: `server/libs/modules/task-dispatchers/subflow/src/main/java/com/bytechef/task/dispatcher/subflow/event/listener/AgentSubflowLauncher.java`
- Modify: `server/libs/modules/task-dispatchers/subflow/src/main/java/com/bytechef/task/dispatcher/subflow/config/SubflowTaskDispatcherConfiguration.java`
- Test: `server/libs/modules/task-dispatchers/subflow/src/test/java/com/bytechef/task/dispatcher/subflow/event/listener/AgentSubflowLauncherTest.java` (new)

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.task.dispatcher.subflow.event.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import com.bytechef.platform.workflow.task.dispatcher.subflow.PendingSubflowRequest;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowRequestConstants;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AgentSubflowLauncherTest {

    @Test
    void testLaunchesSubflowOnAgentStop() {
        ChildJobPrincipalFactory childJobPrincipalFactory = mock(ChildJobPrincipalFactory.class);
        JobService jobService = mock(JobService.class);
        TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);

        long agentJobId = 100L;

        PendingSubflowRequest request = new PendingSubflowRequest(
            "wf-99", "newWorkflowCall", Map.of("amount", 5), false, PlatformType.AUTOMATION);

        ActionContext.Suspend suspend = new ActionContext.Suspend(
            Map.of(SubflowRequestConstants.PENDING_SUBFLOW, request), null);

        Job agentJob = mock(Job.class);
        when(agentJob.getId()).thenReturn(agentJobId);
        when(agentJob.getMetadata()).thenReturn(new HashMap<>());
        when(jobService.getJob(agentJobId)).thenReturn(agentJob);

        TaskExecution suspendedTask = mock(TaskExecution.class);
        when(suspendedTask.getMetadata()).thenReturn(Map.of(MetadataConstants.SUSPEND, suspend));
        when(taskExecutionService.fetchLastJobTaskExecution(agentJobId)).thenReturn(Optional.of(suspendedTask));

        when(childJobPrincipalFactory.createPrincipalLinkedJob(eq(agentJobId), any())).thenReturn(200L);

        AgentSubflowLauncher launcher = new AgentSubflowLauncher(
            childJobPrincipalFactory, jobService, taskExecutionService);

        launcher.onApplicationEvent(new JobStatusApplicationEvent(agentJobId, Job.Status.STOPPED));

        verify(childJobPrincipalFactory).createPrincipalLinkedJob(eq(agentJobId), any());
        verify(jobService).update(agentJob); // idempotency marker written
    }

    @Test
    void testIgnoresStopWithoutPendingSubflowRequest() {
        ChildJobPrincipalFactory childJobPrincipalFactory = mock(ChildJobPrincipalFactory.class);
        JobService jobService = mock(JobService.class);
        TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);

        Job job = mock(Job.class);
        when(job.getId()).thenReturn(100L);
        when(job.getMetadata()).thenReturn(new HashMap<>());
        when(jobService.getJob(100L)).thenReturn(job);
        when(taskExecutionService.fetchLastJobTaskExecution(100L)).thenReturn(Optional.empty());

        AgentSubflowLauncher launcher = new AgentSubflowLauncher(
            childJobPrincipalFactory, jobService, taskExecutionService);

        launcher.onApplicationEvent(new JobStatusApplicationEvent(100L, Job.Status.STOPPED));

        verify(childJobPrincipalFactory, never()).createPrincipalLinkedJob(anyLong(), any());
    }

    @Test
    void testIdempotentWhenAlreadyLaunched() {
        ChildJobPrincipalFactory childJobPrincipalFactory = mock(ChildJobPrincipalFactory.class);
        JobService jobService = mock(JobService.class);
        TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);

        Job agentJob = mock(Job.class);
        when(agentJob.getId()).thenReturn(100L);
        when(agentJob.getMetadata()).thenReturn(Map.of(SubflowRequestConstants.LAUNCHED_SUBFLOW_JOB_ID, 200L));
        when(jobService.getJob(100L)).thenReturn(agentJob);

        AgentSubflowLauncher launcher = new AgentSubflowLauncher(
            childJobPrincipalFactory, jobService, taskExecutionService);

        launcher.onApplicationEvent(new JobStatusApplicationEvent(100L, Job.Status.STOPPED));

        verify(childJobPrincipalFactory, never()).createPrincipalLinkedJob(anyLong(), any());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:modules:task-dispatchers:subflow:test --tests "*AgentSubflowLauncherTest"`
Expected: FAIL — `AgentSubflowLauncher` does not exist.

- [ ] **Step 3: Implement `AgentSubflowLauncher`**

```java
/*
 * Copyright 2025 ByteChef
 * ... (full ByteChef Apache header) ...
 */

package com.bytechef.task.dispatcher.subflow.event.listener;

import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import com.bytechef.platform.workflow.task.dispatcher.subflow.PendingSubflowRequest;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowRequestConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinator listener for the agent-tool durable sub-workflow bridge. When an agent job suspends with a
 * {@link PendingSubflowRequest}, starts the requested sub-workflow as a top-level job once the agent is durably
 * {@code STOPPED} (deferred launch — the sub-workflow cannot complete before the agent is resumable).
 *
 * @author Ivica Cardic
 */
public class AgentSubflowLauncher implements ApplicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(AgentSubflowLauncher.class);

    private final ChildJobPrincipalFactory childJobPrincipalFactory;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI2")
    public AgentSubflowLauncher(
        ChildJobPrincipalFactory childJobPrincipalFactory, JobService jobService,
        TaskExecutionService taskExecutionService) {

        this.childJobPrincipalFactory = childJobPrincipalFactory;
        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (!(applicationEvent instanceof JobStatusApplicationEvent jobStatusApplicationEvent)) {
            return;
        }

        if (jobStatusApplicationEvent.getStatus() != Job.Status.STOPPED) {
            return;
        }

        long agentJobId = jobStatusApplicationEvent.getJobId();

        Job agentJob = jobService.getJob(agentJobId);

        if (agentJob.getMetadata()
            .containsKey(SubflowRequestConstants.LAUNCHED_SUBFLOW_JOB_ID)) {

            return; // already launched (broker redelivery)
        }

        PendingSubflowRequest request = extractPendingSubflowRequest(agentJobId);

        if (request == null) {
            return; // an ordinary stop -- nothing to do
        }

        JobParametersDTO jobParametersDTO = new JobParametersDTO(
            request.workflowId(), Map.of(request.inputsName(), request.inputs()),
            Map.of(SubflowRequestConstants.AGENT_JOB_ID, agentJobId));

        long subflowJobId = childJobPrincipalFactory.createPrincipalLinkedJob(agentJobId, jobParametersDTO);

        Map<String, Object> agentJobMetadata = new HashMap<>(agentJob.getMetadata());

        agentJobMetadata.put(SubflowRequestConstants.LAUNCHED_SUBFLOW_JOB_ID, subflowJobId);

        agentJob.setMetadata(agentJobMetadata);

        jobService.update(agentJob);

        if (log.isDebugEnabled()) {
            log.debug("Launched sub-workflow job {} for suspended agent job {}", subflowJobId, agentJobId);
        }
    }

    private PendingSubflowRequest extractPendingSubflowRequest(long agentJobId) {
        Optional<TaskExecution> taskExecution = taskExecutionService.fetchLastJobTaskExecution(agentJobId);

        if (taskExecution.isEmpty()) {
            return null;
        }

        Map<String, ?> metadata = taskExecution.get()
            .getMetadata();

        Suspend suspend = MapUtils.get(metadata, MetadataConstants.SUSPEND, Suspend.class);

        if (suspend == null) {
            return null;
        }

        return MapUtils.get(
            suspend.continueParameters(), SubflowRequestConstants.PENDING_SUBFLOW, PendingSubflowRequest.class);
    }
}
```

Notes for the implementer:
- `MapUtils.get(map, key, Class)` uses Jackson `convertValue` — it reconstructs the `Suspend` and `PendingSubflowRequest` records from the round-tripped maps (see project memory; the same pattern is used by `SuspendTaskCompletionHandler`).
- Confirm `JobStatusApplicationEvent.getJobId()` returns `long` and `getStatus()` returns `Job.Status` (used identically in `SubflowJobStatusEventListener`).
- Tenant context: `JobStatusApplicationEvent`s are delivered inside the coordinator's tenant-scoped dispatch (the same context `SubflowJobStatusEventListener` runs in). No extra `TenantContext` handling is needed here; verify against `SubflowJobStatusEventListener`, which also performs `jobService` reads with no explicit tenant handling.

- [ ] **Step 4: Register the listener**

In `SubflowTaskDispatcherConfiguration`, add a bean inside a nested `@ConditionalOnCoordinator` configuration (mirroring `SubflowJobStatusEventListenerConfiguration`):

```java
    @Configuration
    @ConditionalOnCoordinator
    public static class AgentSubflowBridgeConfiguration {

        @Bean
        AgentSubflowLauncher agentSubflowLauncher(
            ChildJobPrincipalFactory childJobPrincipalFactory, JobService jobService,
            TaskExecutionService taskExecutionService) {

            return new AgentSubflowLauncher(childJobPrincipalFactory, jobService, taskExecutionService);
        }
    }
```

(`AgentSubflowResumeListener`'s bean is added to this same nested class in Task 6.)

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :server:libs:modules:task-dispatchers:subflow:test --tests "*AgentSubflowLauncherTest"`
Expected: PASS (3 tests).

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/libs/modules/task-dispatchers/subflow/
git commit -m "$(cat <<'EOF'
5055 Add AgentSubflowLauncher to start the sub-workflow on agent suspend

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: `AgentSubflowResumeListener`

**Files:**
- Create: `server/libs/modules/task-dispatchers/subflow/src/main/java/com/bytechef/task/dispatcher/subflow/event/listener/AgentSubflowResumeListener.java`
- Modify: `server/libs/modules/task-dispatchers/subflow/src/main/java/com/bytechef/task/dispatcher/subflow/config/SubflowTaskDispatcherConfiguration.java`
- Test: `server/libs/modules/task-dispatchers/subflow/src/test/java/com/bytechef/task/dispatcher/subflow/event/listener/AgentSubflowResumeListenerTest.java` (new)

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.task.dispatcher.subflow.event.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowRequestConstants;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentSubflowResumeListenerTest {

    @Test
    void testResumesAgentWithSubflowOutputOnCompleted() {
        JobFacade jobFacade = mock(JobFacade.class);
        JobService jobService = mock(JobService.class);
        TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
        TaskFileStorage taskFileStorage = mock(TaskFileStorage.class);

        long subflowJobId = 200L;
        long agentJobId = 100L;

        Job subflowJob = mock(Job.class);
        when(subflowJob.getId()).thenReturn(subflowJobId);
        when(subflowJob.getMetadata()).thenReturn(Map.of(SubflowRequestConstants.AGENT_JOB_ID, agentJobId));
        when(subflowJob.getOutputs()).thenReturn(null);
        when(jobService.getJob(subflowJobId)).thenReturn(subflowJob);
        when(taskExecutionService.fetchLastJobTaskExecution(subflowJobId))
            .thenReturn(java.util.Optional.empty());

        Job agentJob = mock(Job.class);
        when(agentJob.getStatus()).thenReturn(Job.Status.STOPPED);
        when(jobService.getJob(agentJobId)).thenReturn(agentJob);

        AgentSubflowResumeListener listener = new AgentSubflowResumeListener(
            jobFacade, jobService, taskExecutionService, taskFileStorage);

        listener.onApplicationEvent(new JobStatusApplicationEvent(subflowJobId, Job.Status.COMPLETED));

        verify(jobFacade).resumeJob(eq(agentJobId), any());
    }

    @Test
    void testResumesAgentWithErrorOnFailed() {
        JobFacade jobFacade = mock(JobFacade.class);
        JobService jobService = mock(JobService.class);
        TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
        TaskFileStorage taskFileStorage = mock(TaskFileStorage.class);

        Job subflowJob = mock(Job.class);
        when(subflowJob.getId()).thenReturn(200L);
        when(subflowJob.getWorkflowId()).thenReturn("wf-99");
        when(subflowJob.getMetadata()).thenReturn(Map.of(SubflowRequestConstants.AGENT_JOB_ID, 100L));
        when(jobService.getJob(200L)).thenReturn(subflowJob);

        Job agentJob = mock(Job.class);
        when(agentJob.getStatus()).thenReturn(Job.Status.STOPPED);
        when(jobService.getJob(100L)).thenReturn(agentJob);

        AgentSubflowResumeListener listener = new AgentSubflowResumeListener(
            jobFacade, jobService, taskExecutionService, taskFileStorage);

        listener.onApplicationEvent(new JobStatusApplicationEvent(200L, Job.Status.FAILED));

        // resume data is a map carrying an "error" entry
        verify(jobFacade).resumeJob(eq(100L), org.mockito.ArgumentMatchers.argThat(
            data -> data != null && data.containsKey("error")));
    }

    @Test
    void testIgnoresNonAgentSubflowJob() {
        JobFacade jobFacade = mock(JobFacade.class);
        JobService jobService = mock(JobService.class);
        TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
        TaskFileStorage taskFileStorage = mock(TaskFileStorage.class);

        Job job = mock(Job.class);
        when(job.getId()).thenReturn(200L);
        when(job.getMetadata()).thenReturn(Map.of());
        when(jobService.getJob(200L)).thenReturn(job);

        AgentSubflowResumeListener listener = new AgentSubflowResumeListener(
            jobFacade, jobService, taskExecutionService, taskFileStorage);

        listener.onApplicationEvent(new JobStatusApplicationEvent(200L, Job.Status.COMPLETED));

        verify(jobFacade, never()).resumeJob(anyLong(), any());
    }

    @Test
    void testNoOpWhenAgentNotStopped() {
        JobFacade jobFacade = mock(JobFacade.class);
        JobService jobService = mock(JobService.class);
        TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
        TaskFileStorage taskFileStorage = mock(TaskFileStorage.class);

        Job subflowJob = mock(Job.class);
        when(subflowJob.getId()).thenReturn(200L);
        when(subflowJob.getMetadata()).thenReturn(Map.of(SubflowRequestConstants.AGENT_JOB_ID, 100L));
        when(subflowJob.getOutputs()).thenReturn(null);
        when(jobService.getJob(200L)).thenReturn(subflowJob);
        when(taskExecutionService.fetchLastJobTaskExecution(200L)).thenReturn(java.util.Optional.empty());

        Job agentJob = mock(Job.class);
        when(agentJob.getStatus()).thenReturn(Job.Status.STARTED);
        when(jobService.getJob(100L)).thenReturn(agentJob);

        AgentSubflowResumeListener listener = new AgentSubflowResumeListener(
            jobFacade, jobService, taskExecutionService, taskFileStorage);

        listener.onApplicationEvent(new JobStatusApplicationEvent(200L, Job.Status.COMPLETED));

        verify(jobFacade, never()).resumeJob(anyLong(), any());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:modules:task-dispatchers:subflow:test --tests "*AgentSubflowResumeListenerTest"`
Expected: FAIL — `AgentSubflowResumeListener` does not exist.

- [ ] **Step 3: Implement `AgentSubflowResumeListener`**

```java
/*
 * Copyright 2025 ByteChef
 * ... (full ByteChef Apache header) ...
 */

package com.bytechef.task.dispatcher.subflow.event.listener;

import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.component.definition.ActionDefinition.CallableResponse;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowRequestConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinator listener for the agent-tool durable sub-workflow bridge. When a sub-workflow job launched by
 * {@link AgentSubflowLauncher} reaches a terminal state, resumes the suspended agent job with the sub-workflow
 * result (or an LLM-readable error).
 *
 * @author Ivica Cardic
 */
public class AgentSubflowResumeListener implements ApplicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(AgentSubflowResumeListener.class);

    private final JobFacade jobFacade;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI2")
    public AgentSubflowResumeListener(
        JobFacade jobFacade, JobService jobService, TaskExecutionService taskExecutionService,
        TaskFileStorage taskFileStorage) {

        this.jobFacade = jobFacade;
        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (!(applicationEvent instanceof JobStatusApplicationEvent jobStatusApplicationEvent)) {
            return;
        }

        Job.Status status = jobStatusApplicationEvent.getStatus();

        if (status != Job.Status.COMPLETED && status != Job.Status.FAILED) {
            return;
        }

        Job subflowJob = jobService.getJob(jobStatusApplicationEvent.getJobId());

        Object agentJobIdObject = subflowJob.getMetadata()
            .get(SubflowRequestConstants.AGENT_JOB_ID);

        if (agentJobIdObject == null) {
            return; // not an agent-initiated sub-workflow
        }

        long agentJobId = ((Number) agentJobIdObject).longValue();

        Job agentJob = jobService.getJob(agentJobId);

        if (agentJob.getStatus() != Job.Status.STOPPED) {
            log.warn(
                "Agent job {} is {} (expected STOPPED) when sub-workflow job {} terminated; skipping resume",
                agentJobId, agentJob.getStatus(), subflowJob.getId());

            return;
        }

        Map<String, ?> resumeData = status == Job.Status.COMPLETED
            ? buildCompletedResumeData(subflowJob)
            : Map.of("error", "Sub-workflow '%s' failed.".formatted(subflowJob.getWorkflowId()));

        jobFacade.resumeJob(agentJobId, resumeData);

        if (log.isDebugEnabled()) {
            log.debug("Resumed agent job {} after sub-workflow job {} {}", agentJobId, subflowJob.getId(), status);
        }
    }

    private Map<String, ?> buildCompletedResumeData(Job subflowJob) {
        Object output = getCallableResponseOutput(subflowJob)
            .orElseGet(() -> subflowJob.getOutputs() == null
                ? Map.of()
                : taskFileStorage.readJobOutputs(subflowJob.getOutputs()));

        if (output instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, ?> typed = (Map<String, ?>) map;

            return typed;
        }

        return Map.of("result", output);
    }

    private Optional<Object> getCallableResponseOutput(Job job) {
        try {
            return taskExecutionService.fetchLastJobTaskExecution(Objects.requireNonNull(job.getId()))
                .filter(lastTaskExecution -> lastTaskExecution.getMetadata()
                    .containsKey(MetadataConstants.CALLABLE_RESPONSE))
                .map(lastTaskExecution -> {
                    CallableResponse callableResponse = ConvertUtils.convertValue(
                        taskFileStorage.readTaskExecutionOutput(lastTaskExecution.getOutput()),
                        CallableResponse.class);

                    return callableResponse.output();
                });
        } catch (Exception exception) {
            log.warn("Failed to extract callable response from job {}: {}", job.getId(), exception.getMessage());

            return Optional.empty();
        }
    }
}
```

Note: `getCallableResponseOutput` mirrors `SubflowJobStatusEventListener.getCallableResponseOutput` exactly — copy that method's behavior to preserve the existing sub-workflow output semantics. The FAILED message may optionally be enriched by reading the last task execution's `getError()` if present; keep it generic if that adds noise.

- [ ] **Step 4: Register the listener**

In `SubflowTaskDispatcherConfiguration`'s `AgentSubflowBridgeConfiguration` (created in Task 5), add:

```java
        @Bean
        AgentSubflowResumeListener agentSubflowResumeListener(
            JobFacade jobFacade, JobService jobService, TaskExecutionService taskExecutionService,
            TaskFileStorage taskFileStorage) {

            return new AgentSubflowResumeListener(jobFacade, jobService, taskExecutionService, taskFileStorage);
        }
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :server:libs:modules:task-dispatchers:subflow:test --tests "*AgentSubflowResumeListenerTest"`
Expected: PASS (4 tests).

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/libs/modules/task-dispatchers/subflow/
git commit -m "$(cat <<'EOF'
5055 Add AgentSubflowResumeListener to resume the agent on sub-workflow end

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: Integration test — the silent-swallow regression

This is the invariant from the issue: a suspending sub-workflow must never collapse into an ordinary completed agent tool result.

**Files:**
- Test: locate the existing AI agent integration test harness. Search: `grep -rln "AiAgentChatAction" server --include='*IntTest.java'` and inspect how it stubs a `ChatModel` and drives `perform`/`resumePerform`. Add a new `*IntTest` for the durable subflow path under the same module (likely `server/libs/modules/components/ai/agent/src/test/java/...` or a dedicated agent integration-test module). If no agent `IntTest` exists yet (the 2026-05-20 work may not have landed its IntTest), create one following the pattern in that spec's Testing section.

- [ ] **Step 1: Write the integration test**

Scenarios (each its own `@Test`, `@SpringBootTest` + `@ActiveProfiles("testint")` + Testcontainers, real coordinator):

1. `testSuspendingSubflowParksAgentThenResumesWithApprovalResult` — agent workflow with a `callWorkflow` tool pointing at a sub-workflow that contains a Request Approval step; a stub `ChatModel` emits one tool call. Assert: after `perform`, the agent job is `STOPPED` (a `Suspend` with `JOB_RESUME_ID`); a sub-workflow job was created (top-level, `parentTaskExecutionId == null`) and is itself `STOPPED`. Then resume the sub-workflow via its own `JobResumeId` with an approval payload; assert the sub-workflow completes, the agent job resumes, and the stub model is re-invoked with the pending tool response patched to the real approval result.
2. `testTrivialFastSubflowStillResumesAgent` — sub-workflow with a single non-suspending task. Exercises the startup-race window: assert the agent still ends up resumed with the sub-workflow output (the launcher's deferred launch guarantees the agent is `STOPPED` first).
3. `testFailedSubflowResumesAgentWithError` — sub-workflow whose task fails. Assert the agent job resumes (not fails), and the patched tool response carries an `error` entry.
4. `testWaitSubflowParksAgent` — sub-workflow containing a Wait action; same parking/resume invariant as scenario 1.

Write the actual test bodies using the agent IntTest harness's stub `ChatModel` and the workflow fixtures it already provides; reuse its sub-workflow JSON fixture creation. If the harness lacks a fixture for a sub-workflow with Request Approval, add one under that module's `src/test/resources`.

- [ ] **Step 2: Run the integration test**

Run: `./gradlew :server:libs:modules:components:ai:agent:testIntegration` (adjust the project path to wherever the agent `IntTest` lives)
Expected: PASS (4 tests). Docker must be running for Testcontainers.

- [ ] **Step 3: Commit**

```bash
./gradlew spotlessApply
git add <the integration test files and resources>
git commit -m "$(cat <<'EOF'
5055 Add integration tests for durable agent sub-workflow execution

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: Full verification

- [ ] **Step 1: Compile everything**

Run: `./gradlew clean compileJava`
Expected: BUILD SUCCESSFUL. Fix any remaining reference to the deleted `SubflowSyncExecutor` / `WorkflowSubflowSyncExecutorConfiguration`.

- [ ] **Step 2: Run the affected module tests**

Run:
```bash
./gradlew :server:libs:modules:components:workflow:test \
  :server:libs:modules:task-dispatchers:subflow:test \
  :server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-api:test \
  :server:libs:automation:automation-configuration:automation-configuration-service:test \
  :server:libs:platform:platform-component:platform-component-context:platform-component-context-service:test
```
Expected: all PASS.

- [ ] **Step 3: Run static checks**

Run: `./gradlew spotlessApply check -x test -x testIntegration` (or the project's standard `./gradlew check` if time allows)
Expected: Checkstyle / PMD / SpotBugs clean. Fix violations per `CLAUDE.md` (blank lines before control statements, no method-name underscores, etc.).

- [ ] **Step 4: Confirm `JobSyncExecutor` is no longer referenced for this path**

Run: `grep -rln "SubflowSyncExecutor" server --include='*.java'`
Expected: no matches.

- [ ] **Step 5: Final commit (if Step 3 produced formatting changes)**

```bash
git add -A server/
git commit -m "$(cat <<'EOF'
5055 Apply formatting and static-analysis fixes

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Notes & Known Limitations

- **Agent that is itself a sub-workflow:** if an AI-agent workflow is run as a sub-workflow (its job has a `parentTaskExecutionId`), `JobFacade.resumeJob` on it would hit `JobServiceImpl`'s `"Can't resume a subflow"` assertion. This is a pre-existing limitation shared by all agent suspending tools (`requestApproval`, `askUserQuestion` from the 2026-05-20 spec) — not introduced here. Out of scope for this plan; not guarded.
- **Lost sub-workflow job:** the agent suspends with `expiresAt = null` (waits indefinitely). No reconciliation safety-net in v1 — see the spec's Open items.
- **One suspending sub-workflow per turn:** enforced by the tool's `getSuspend() != null` check plus `SuspendableToolCallingManager`'s two-sentinel backstop.
