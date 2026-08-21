# Workflow Execution Agent Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a dedicated Workflow Execution AI specialist — execution-inspection tools in the shared CE `mcp-tool-automation` module, a Copilot sub-agent that powers the `WorkflowExecutionDetails` sheet panel, and AI Hub delegation via the existing specialist-as-tool pattern.

**Architecture:** Tools live in CE (`mcp-tool-automation`) wrapping `ProjectWorkflowExecutionFacade`. A new `WorkflowExecutionSpringAIAgent` (EE, `ai-copilot-service`) is registered as `workflow_execution_ask`/`workflow_execution_build` beans (served to the sheet via `CopilotApiController`'s `localAgentMap`) plus two stateless sub-agent `ChatClient` beans. AI Hub reaches the specialist through a `WorkflowExecutionAgentToolCallback` (`platform-ai-hub-service`) registered by bean-name `ObjectProvider<ChatClient>` in `AiHubConfiguration` — no new module dependency edge.

**Tech Stack:** Java 25 / Spring Boot 4, Spring AI (`@Tool`, `ToolCallback`, `ChatClient`), `ag-ui` `SpringAIAgent`, React 19 / TypeScript / Zustand, Gradle, JUnit 5 / Mockito / AssertJ, Vitest.

**Spec:** `docs/superpowers/specs/2026-06-01-workflow-execution-agent-design.md`

---

## Conventions (apply to every Java task)

- **License header:** EE files (`server/ee/...`) use the ByteChef Enterprise license header + `@version ee` Javadoc tag. CE files (`server/libs/...`) use the Apache 2.0 header. Copy the header from a neighbouring file in the same module.
- **Blank lines:** one blank line before control statements and after a variable modification that precedes its use; no trailing blank line before a class's closing `}`.
- **Naming:** descriptive variable names (no single letters); private methods without `_` prefix; unit test classes end in `Test` (no `Impl`); test method names camelCase, no underscores.
- **Formatting:** run `./gradlew spotlessApply` before each commit. Imports sorted; no method chaining except the allowed fluent/builder/stream cases.
- **Commit messages:** server changes `0_732 <description>`; client changes `0_732 client - <description>`; end with the `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>` trailer.

---

## File Structure

**Phase 1 — CE tools** (`server/libs/ai/mcp/mcp-tool/mcp-tool-automation/`)
- Create `.../automation/WorkflowExecutionTools.java` — `@Tool` methods `getWorkflowExecution`, `listWorkflowExecutions`.
- Create `.../automation/model/WorkflowExecutionDetailInfo.java`, `.../model/TaskExecutionInfo.java`, `.../model/TriggerExecutionInfo.java`, `.../model/WorkflowExecutionSummary.java` — record DTOs.
- Create `.../automation/WorkflowExecutionToolContextKeys.java` — CE constants mirroring AI Hub's ToolContext key strings.
- Create `.../automation/exception/WorkflowExecutionToolErrorType.java` — typed errors.
- Modify `build.gradle.kts` — add `automation-workflow-execution-api` dependency.
- Test `src/test/java/.../automation/WorkflowExecutionToolsTest.java`.

**Phase 2 — Copilot specialist** (`server/ee/libs/ai/ai-copilot/`)
- Modify `ai-copilot-api/.../util/Source.java` — add `WORKFLOW_EXECUTION`.
- Create `ai-copilot-service/.../agent/WorkflowExecutionSpringAIAgent.java`.
- Create `ai-copilot-service/.../resources/prompt_workflow_execution_ask.txt`, `prompt_workflow_execution_build.txt`.
- Modify `ai-copilot-service/.../config/CopilotConfiguration.java` — 2 agent beans + 2 sub-agent ChatClient beans + 2 `@Value` prompt resources.
- Test `ai-copilot-service/src/test/java/.../config/CopilotConfigurationWorkflowExecutionTest.java`.

**Phase 3 — AI Hub delegation** (`server/ee/libs/...`)
- Modify `platform-ai-hub/platform-ai-hub-api/.../usage/Agent.java` — append `WORKFLOW_EXECUTION_AGENT`.
- Create `platform-ai-hub/platform-ai-hub-service/.../tool/WorkflowExecutionAgentToolCallback.java`.
- Modify `automation-ai-hub/automation-ai-hub-service/.../config/AiHubConfiguration.java` — register the callback (ASK + BUILD); remove `ListWorkflowExecutionsToolCallback` registrations.
- Delete `automation-ai-hub/automation-ai-hub-service/.../tool/ListWorkflowExecutionsToolCallback.java` (+ its test if any).
- Test `platform-ai-hub-service/src/test/java/.../tool/WorkflowExecutionAgentToolCallbackTest.java`.

**Phase 4 — Client** (`client/`)
- Modify `src/shared/components/copilot/stores/useCopilotStore.ts` — add `WORKFLOW_EXECUTION` to `Source`.
- Modify `src/pages/automation/workflow-executions/.../hooks/useWorkflowExecutionSheet.ts` — repoint source + parameters + state contributor.
- Update test `src/pages/automation/workflow-executions/.../tests/WorkflowExecutionDetail.test.tsx` if it asserts the source.

---

## Phase 1 — CE Tools (`mcp-tool-automation`)

### Task 1: Add the execution-facade build dependency

**Files:**
- Modify: `server/libs/ai/mcp/mcp-tool/mcp-tool-automation/build.gradle.kts`

- [ ] **Step 1: Add the dependency**

In the `dependencies { ... }` block, add this line in alphabetical position among the other `implementation(project(...))` lines:

```kotlin
    implementation(project(":server:libs:automation:automation-workflow:automation-workflow-execution:automation-workflow-execution-api"))
```

- [ ] **Step 2: Verify it resolves**

Run: `./gradlew :server:libs:ai:mcp:mcp-tool:mcp-tool-automation:compileJava`
Expected: BUILD SUCCESSFUL (no new code yet; just confirms the dependency path is valid).

- [ ] **Step 3: Commit**

```bash
git add server/libs/ai/mcp/mcp-tool/mcp-tool-automation/build.gradle.kts
git commit -m "0_732 Add automation-workflow-execution-api dep to mcp-tool-automation

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 2: ToolContext key constants (CE)

**Files:**
- Create: `server/libs/ai/mcp/mcp-tool/mcp-tool-automation/src/main/java/com/bytechef/ai/mcp/tool/automation/WorkflowExecutionToolContextKeys.java`

- [ ] **Step 1: Create the constants holder**

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

package com.bytechef.ai.mcp.tool.automation;

/**
 * Spring AI {@link org.springframework.ai.chat.model.ToolContext} keys read by {@link WorkflowExecutionTools} to scope
 * workspace-bounded queries.
 *
 * <p>
 * The literal values intentionally match the keys written by the EE {@code AiHubToolInvocationContext} so the AI Hub
 * runtime needs no change to populate them. The Copilot runtime populates the same keys via the specialist agent's
 * {@code toolContext()} override. If the EE key strings ever change, these must change in lockstep.
 * </p>
 *
 * @author Ivica Cardic
 */
public final class WorkflowExecutionToolContextKeys {

    public static final String WORKSPACE_ID = "bytechef.assetFile.workspaceId";
    public static final String ENVIRONMENT_ID = "bytechef.assetFile.environmentId";

    private WorkflowExecutionToolContextKeys() {
    }
}
```

- [ ] **Step 2: Verify the EE key strings still match**

Run: `grep -n "TOOL_CONTEXT_WORKSPACE_ID_KEY\|TOOL_CONTEXT_ENVIRONMENT_ID_KEY" server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/tool/AiHubToolInvocationContext.java`
Expected: the two constants equal `"bytechef.assetFile.workspaceId"` and `"bytechef.assetFile.environmentId"`. If they differ, update the constants in Step 1 to match.

- [ ] **Step 3: Commit**

```bash
git add server/libs/ai/mcp/mcp-tool/mcp-tool-automation/src/main/java/com/bytechef/ai/mcp/tool/automation/WorkflowExecutionToolContextKeys.java
git commit -m "0_732 Add WorkflowExecutionToolContextKeys to mcp-tool-automation

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 3: DTO records

**Files:**
- Create: `.../automation/model/WorkflowExecutionSummary.java`
- Create: `.../automation/model/TaskExecutionInfo.java`
- Create: `.../automation/model/TriggerExecutionInfo.java`
- Create: `.../automation/model/WorkflowExecutionDetailInfo.java`

(Base dir: `server/libs/ai/mcp/mcp-tool/mcp-tool-automation/src/main/java/com/bytechef/ai/mcp/tool/automation/model/`)

- [ ] **Step 1: Create `WorkflowExecutionSummary.java`**

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

package com.bytechef.ai.mcp.tool.automation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public record WorkflowExecutionSummary(
    @JsonProperty("id") @JsonPropertyDescription("The workflow execution id") long id,
    @JsonProperty("workflowLabel") @JsonPropertyDescription("The workflow label") @Nullable String workflowLabel,
    @JsonProperty("projectName") @JsonPropertyDescription("The project name") @Nullable String projectName,
    @JsonProperty("status") @JsonPropertyDescription("The job status") @Nullable String status,
    @JsonProperty("startDate") @JsonPropertyDescription("When the run started") @Nullable Instant startDate,
    @JsonProperty("endDate") @JsonPropertyDescription("When the run ended") @Nullable Instant endDate) {
}
```

- [ ] **Step 2: Create `TaskExecutionInfo.java`**

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

package com.bytechef.ai.mcp.tool.automation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public record TaskExecutionInfo(
    @JsonProperty("name") @JsonPropertyDescription("The task name (workflow task label or type)") @Nullable String name,
    @JsonProperty("type") @JsonPropertyDescription("The component/action type, e.g. httpClient/v1/get") @Nullable String type,
    @JsonProperty("status") @JsonPropertyDescription("The task execution status") @Nullable String status,
    @JsonProperty("input") @JsonPropertyDescription("The resolved task input") @Nullable Map<String, ?> input,
    @JsonProperty("output") @JsonPropertyDescription("The task output value") @Nullable Object output,
    @JsonProperty("error") @JsonPropertyDescription("The error message if the task failed") @Nullable String error) {
}
```

- [ ] **Step 3: Create `TriggerExecutionInfo.java`**

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

package com.bytechef.ai.mcp.tool.automation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public record TriggerExecutionInfo(
    @JsonProperty("type") @JsonPropertyDescription("The trigger type") @Nullable String type,
    @JsonProperty("status") @JsonPropertyDescription("The trigger execution status") @Nullable String status,
    @JsonProperty("output") @JsonPropertyDescription("The trigger output value") @Nullable Object output,
    @JsonProperty("error") @JsonPropertyDescription("The error message if the trigger failed") @Nullable String error) {
}
```

- [ ] **Step 4: Create `WorkflowExecutionDetailInfo.java`**

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

package com.bytechef.ai.mcp.tool.automation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public record WorkflowExecutionDetailInfo(
    @JsonProperty("id") @JsonPropertyDescription("The workflow execution id") long id,
    @JsonProperty("workflowId") @JsonPropertyDescription("The workflow id") @Nullable String workflowId,
    @JsonProperty("workflowLabel") @JsonPropertyDescription("The workflow label") @Nullable String workflowLabel,
    @JsonProperty("projectName") @JsonPropertyDescription("The project name") @Nullable String projectName,
    @JsonProperty("status") @JsonPropertyDescription("The job status") @Nullable String status,
    @JsonProperty("startDate") @JsonPropertyDescription("When the run started") @Nullable Instant startDate,
    @JsonProperty("endDate") @JsonPropertyDescription("When the run ended") @Nullable Instant endDate,
    @JsonProperty("error") @JsonPropertyDescription("The job-level error message, if any") @Nullable String error,
    @JsonProperty("trigger") @JsonPropertyDescription("The trigger execution") @Nullable TriggerExecutionInfo trigger,
    @JsonProperty("taskExecutions") @JsonPropertyDescription("The ordered task executions") List<TaskExecutionInfo> taskExecutions) {
}
```

- [ ] **Step 5: Compile**

Run: `./gradlew :server:libs:ai:mcp:mcp-tool:mcp-tool-automation:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add server/libs/ai/mcp/mcp-tool/mcp-tool-automation/src/main/java/com/bytechef/ai/mcp/tool/automation/model/
git commit -m "0_732 Add workflow-execution tool DTO records

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 4: Typed error type

**Files:**
- Create: `server/libs/ai/mcp/mcp-tool/mcp-tool-automation/src/main/java/com/bytechef/ai/mcp/tool/automation/exception/WorkflowExecutionToolErrorType.java`

- [ ] **Step 1: Inspect the existing pattern**

Run: `cat server/libs/ai/mcp/mcp-tool/mcp-tool-automation/src/main/java/com/bytechef/ai/mcp/tool/automation/exception/ProjectToolErrorType.java`
Note the `extends AbstractErrorType` shape and the `super(<Class>.class, errorKey)` constructor call.

- [ ] **Step 2: Create the error type**

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

package com.bytechef.ai.mcp.tool.automation.exception;

import com.bytechef.exception.AbstractErrorType;

/**
 * @author Ivica Cardic
 */
public class WorkflowExecutionToolErrorType extends AbstractErrorType {

    public static final WorkflowExecutionToolErrorType GET_WORKFLOW_EXECUTION = new WorkflowExecutionToolErrorType(100);
    public static final WorkflowExecutionToolErrorType LIST_WORKFLOW_EXECUTIONS =
        new WorkflowExecutionToolErrorType(101);

    private WorkflowExecutionToolErrorType(int errorKey) {
        super(WorkflowExecutionToolErrorType.class, errorKey);
    }
}
```

> If Step 1 shows `ProjectToolErrorType` uses a different base class or import (e.g. the package of `AbstractErrorType`), copy that exact import and base instead.

- [ ] **Step 3: Compile**

Run: `./gradlew :server:libs:ai:mcp:mcp-tool:mcp-tool-automation:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add server/libs/ai/mcp/mcp-tool/mcp-tool-automation/src/main/java/com/bytechef/ai/mcp/tool/automation/exception/WorkflowExecutionToolErrorType.java
git commit -m "0_732 Add WorkflowExecutionToolErrorType

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 5: `WorkflowExecutionTools` (TDD)

**Files:**
- Create: `server/libs/ai/mcp/mcp-tool/mcp-tool-automation/src/main/java/com/bytechef/ai/mcp/tool/automation/WorkflowExecutionTools.java`
- Test: `server/libs/ai/mcp/mcp-tool/mcp-tool-automation/src/test/java/com/bytechef/ai/mcp/tool/automation/WorkflowExecutionToolsTest.java`

- [ ] **Step 1: Write the failing test**

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

package com.bytechef.ai.mcp.tool.automation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.mcp.tool.automation.model.WorkflowExecutionDetailInfo;
import com.bytechef.exception.ExecutionException;
import com.bytechef.ai.mcp.tool.automation.model.WorkflowExecutionSummary;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacade;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

class WorkflowExecutionToolsTest {

    private final ProjectWorkflowExecutionFacade facade = mock(ProjectWorkflowExecutionFacade.class);
    private final WorkflowExecutionTools workflowExecutionTools = new WorkflowExecutionTools(facade);

    @Test
    void testGetWorkflowExecutionMapsTaskFailure() {
        Job job = new Job();
        job.setId(7L);
        job.setStatus(Job.Status.FAILED);
        job.setWorkflowId("wf-1");

        TaskExecution taskExecution = TaskExecution.builder()
            .id(11L)
            .type("httpClient/v1/get")
            .status(TaskExecution.Status.FAILED)
            .error(new ExecutionError("boom", List.of("at x", "at y")))
            .build();

        TaskExecutionDTO taskExecutionDTO = new TaskExecutionDTO(
            taskExecution, "Get user", "icon", Map.of("url", "https://x"), null, null);

        JobDTO jobDTO = new JobDTO(job, Map.of(), List.of(taskExecutionDTO));

        Project project = new Project();
        project.setName("Demo");
        project.setWorkspaceId(42L);

        Workflow workflow = new Workflow();
        workflow.setLabel("My Workflow");

        WorkflowExecutionDTO dto = new WorkflowExecutionDTO(7L, project, null, jobDTO, workflow, null);

        when(facade.getWorkflowExecution(7L)).thenReturn(dto);

        ToolContext toolContext = new ToolContext(Map.of(WorkflowExecutionToolContextKeys.WORKSPACE_ID, 42L));

        WorkflowExecutionDetailInfo result = workflowExecutionTools.getWorkflowExecution(7L, toolContext);

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.workflowLabel()).isEqualTo("My Workflow");
        assertThat(result.taskExecutions()).hasSize(1);
        assertThat(result.taskExecutions()
            .get(0)
            .error()).isEqualTo("boom");
        assertThat(result.taskExecutions()
            .get(0)
            .type()).isEqualTo("httpClient/v1/get");
    }

    @Test
    void testGetWorkflowExecutionDeniesCrossWorkspaceAccess() {
        Job job = new Job();
        job.setId(7L);
        job.setStatus(Job.Status.COMPLETED);

        JobDTO jobDTO = new JobDTO(job, Map.of(), List.of());

        Project project = new Project();
        project.setName("Other workspace");
        project.setWorkspaceId(99L);

        WorkflowExecutionDTO dto = new WorkflowExecutionDTO(7L, project, null, jobDTO, null, null);

        when(facade.getWorkflowExecution(7L)).thenReturn(dto);

        // Caller is scoped to workspace 42 but the execution belongs to workspace 99.
        ToolContext toolContext = new ToolContext(Map.of(WorkflowExecutionToolContextKeys.WORKSPACE_ID, 42L));

        assertThatThrownBy(() -> workflowExecutionTools.getWorkflowExecution(7L, toolContext))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void testListWorkflowExecutionsScopesByWorkspaceFromToolContext() {
        Job job = new Job();
        job.setId(7L);
        job.setStatus(Job.Status.COMPLETED);

        JobDTO jobDTO = new JobDTO(job, Map.of(), List.of());

        Project project = new Project();
        project.setName("Demo");

        Workflow workflow = new Workflow();
        workflow.setLabel("My Workflow");

        WorkflowExecutionDTO dto = new WorkflowExecutionDTO(7L, project, null, jobDTO, workflow, null);

        Page<WorkflowExecutionDTO> page = new PageImpl<>(List.of(dto));

        when(
            facade.getWorkflowExecutions(
                eq(false), eq(2L), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(42L), anyInt()))
                    .thenReturn(page);

        ToolContext toolContext = new ToolContext(
            Map.of(
                WorkflowExecutionToolContextKeys.WORKSPACE_ID, 42L,
                WorkflowExecutionToolContextKeys.ENVIRONMENT_ID, 2L));

        List<WorkflowExecutionSummary> result =
            workflowExecutionTools.listWorkflowExecutions(null, null, toolContext);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)
            .id()).isEqualTo(7L);
        assertThat(result.get(0)
            .status()).isEqualTo("COMPLETED");
    }

    @Test
    void testListWorkflowExecutionsReturnsEmptyWhenWorkspaceMissing() {
        ToolContext toolContext = new ToolContext(Map.of());

        List<WorkflowExecutionSummary> result =
            workflowExecutionTools.listWorkflowExecutions(null, null, toolContext);

        assertThat(result).isEmpty();
    }
}
```

> Before relying on the `Job`, `TaskExecution`, `Project`, `Workflow`, `ExecutionError` constructors/builders above, confirm them:
> `grep -n "public Job(\|setStatus\|setWorkflowId" server/libs/atlas/atlas-execution/atlas-execution-api/src/main/java/com/bytechef/atlas/execution/domain/Job.java` and
> `grep -n "builder()\|public ExecutionError(" server/libs/atlas/atlas-execution/atlas-execution-api/src/main/java/com/bytechef/atlas/execution/domain/TaskExecution.java server/libs/core/exception/exception-api/src/main/java/com/bytechef/error/ExecutionError.java`.
> If a builder/constructor differs, adapt the test setup (this is test data plumbing only — the assertions stay the same).

- [ ] **Step 2: Run the test — verify it fails to compile**

Run: `./gradlew :server:libs:ai:mcp:mcp-tool:mcp-tool-automation:test --tests "com.bytechef.ai.mcp.tool.automation.WorkflowExecutionToolsTest"`
Expected: compilation failure — `WorkflowExecutionTools` does not exist yet.

- [ ] **Step 3: Implement `WorkflowExecutionTools`**

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

package com.bytechef.ai.mcp.tool.automation;

import com.bytechef.ai.mcp.tool.automation.exception.WorkflowExecutionToolErrorType;
import com.bytechef.ai.mcp.tool.automation.model.TaskExecutionInfo;
import com.bytechef.ai.mcp.tool.automation.model.TriggerExecutionInfo;
import com.bytechef.ai.mcp.tool.automation.model.WorkflowExecutionDetailInfo;
import com.bytechef.ai.mcp.tool.automation.model.WorkflowExecutionSummary;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacade;
import com.bytechef.error.ExecutionError;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.platform.workflow.execution.dto.TriggerExecutionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Read-only Spring AI tools for inspecting workflow executions. {@code getWorkflowExecution} resolves the full detail
 * of a single run by id (context-free); {@code listWorkflowExecutions} returns a workspace-scoped summary page,
 * reading the workspace / environment from {@link ToolContext} (see {@link WorkflowExecutionToolContextKeys}).
 *
 * @author Ivica Cardic
 */
@Component
public class WorkflowExecutionTools {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionTools.class);

    private final ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkflowExecutionTools(ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade) {
        this.projectWorkflowExecutionFacade = projectWorkflowExecutionFacade;
    }

    @Tool(
        description = "Get the full detail of a single workflow execution by id: job status, dates, job-level error, " +
            "the trigger execution, and every task execution with its input, output, and error. Use this to diagnose " +
            "why a specific run behaved the way it did.")
    public WorkflowExecutionDetailInfo getWorkflowExecution(
        @ToolParam(description = "The workflow execution id") long workflowExecutionId, ToolContext toolContext) {

        try {
            Long workspaceId = asLong(toolContext, WorkflowExecutionToolContextKeys.WORKSPACE_ID);

            WorkflowExecutionDTO execution = projectWorkflowExecutionFacade.getWorkflowExecution(workflowExecutionId);

            // Fail-closed workspace ownership check (IDOR guard): the LLM supplies the execution id, so an
            // hallucinated or injected id could otherwise read another workspace's run. Deny when the caller's
            // workspace is unknown or does not own the execution; do not reveal that the id exists elsewhere.
            Project project = execution == null ? null : execution.project();
            Long executionWorkspaceId = project == null ? null : project.getWorkspaceId();

            if (workspaceId == null || executionWorkspaceId == null || !workspaceId.equals(executionWorkspaceId)) {
                throw new ExecutionException(
                    "Workflow execution " + workflowExecutionId + " not found",
                    WorkflowExecutionToolErrorType.GET_WORKFLOW_EXECUTION);
            }

            return toDetailInfo(execution);
        } catch (ExecutionException executionException) {
            throw executionException;
        } catch (Exception exception) {
            log.error("getWorkflowExecution(): Failed for id={}", workflowExecutionId, exception);

            throw new ExecutionException(
                "Failed to get workflow execution: " + exception.getMessage(), exception,
                WorkflowExecutionToolErrorType.GET_WORKFLOW_EXECUTION);
        }
    }

    @Tool(
        description = "List recent workflow executions for the current workspace. Use this to resolve a run the user " +
            "describes (\"the failed run yesterday\", \"the last execution of my onboarding workflow\") to a concrete " +
            "execution id. Returns a JSON array of {id, workflowLabel, projectName, status, startDate, endDate}, " +
            "newest first. Optional filters: jobStatus narrows to a single status; workflowId filters to one workflow.")
    public List<WorkflowExecutionSummary> listWorkflowExecutions(
        @ToolParam(
            required = false,
            description = "Optional job status filter (CREATED, STARTED, COMPLETED, STOPPED, FAILED)") @Nullable String jobStatus,
        @ToolParam(
            required = false, description = "Optional workflow id to filter a specific workflow's history") @Nullable String workflowId,
        ToolContext toolContext) {

        try {
            Long workspaceId = asLong(toolContext, WorkflowExecutionToolContextKeys.WORKSPACE_ID);

            if (workspaceId == null) {
                log.warn("listWorkflowExecutions(): workspace id absent from tool context; returning empty list");

                return List.of();
            }

            Long environmentId = asLong(toolContext, WorkflowExecutionToolContextKeys.ENVIRONMENT_ID);
            Status status = parseStatus(jobStatus);

            Page<WorkflowExecutionDTO> page = projectWorkflowExecutionFacade.getWorkflowExecutions(
                false, environmentId, status, null, null, null, null, workflowId, workspaceId, 0);

            return page.getContent()
                .stream()
                .map(WorkflowExecutionTools::toSummary)
                .toList();
        } catch (Exception exception) {
            log.error("listWorkflowExecutions(): Failed", exception);

            throw new ExecutionException(
                "Failed to list workflow executions: " + exception.getMessage(), exception,
                WorkflowExecutionToolErrorType.LIST_WORKFLOW_EXECUTIONS);
        }
    }

    private static WorkflowExecutionDetailInfo toDetailInfo(WorkflowExecutionDTO execution) {
        JobDTO job = execution.job();
        Workflow workflow = execution.workflow();
        Project project = execution.project();

        List<TaskExecutionInfo> taskExecutionInfos = job == null || job.taskExecutions() == null ? List.of()
            : job.taskExecutions()
                .stream()
                .map(WorkflowExecutionTools::toTaskExecutionInfo)
                .toList();

        return new WorkflowExecutionDetailInfo(
            execution.id(),
            job == null ? null : job.workflowId(),
            workflow == null ? null : workflow.getLabel(),
            project == null ? null : project.getName(),
            job == null || job.status() == null ? null : job.status()
                .name(),
            job == null ? null : job.startDate(),
            job == null ? null : job.endDate(),
            job == null ? null : errorMessage(job.error()),
            toTriggerExecutionInfo(execution.triggerExecution()),
            taskExecutionInfos);
    }

    private static TaskExecutionInfo toTaskExecutionInfo(TaskExecutionDTO taskExecution) {
        return new TaskExecutionInfo(
            taskExecution.title() != null ? taskExecution.title() : taskExecution.type(),
            taskExecution.type(),
            taskExecution.status() == null ? null : taskExecution.status()
                .name(),
            taskExecution.input(),
            taskExecution.output(),
            errorMessage(taskExecution.error()));
    }

    private static @Nullable TriggerExecutionInfo toTriggerExecutionInfo(@Nullable TriggerExecutionDTO triggerExecution) {
        if (triggerExecution == null) {
            return null;
        }

        return new TriggerExecutionInfo(
            triggerExecution.type(),
            triggerExecution.status() == null ? null : triggerExecution.status()
                .name(),
            triggerExecution.output(),
            errorMessage(triggerExecution.error()));
    }

    private static WorkflowExecutionSummary toSummary(WorkflowExecutionDTO execution) {
        JobDTO job = execution.job();
        Workflow workflow = execution.workflow();
        Project project = execution.project();

        return new WorkflowExecutionSummary(
            execution.id(),
            workflow == null ? null : workflow.getLabel(),
            project == null ? null : project.getName(),
            job == null || job.status() == null ? null : job.status()
                .name(),
            job == null ? null : job.startDate(),
            job == null ? null : job.endDate());
    }

    private static @Nullable String errorMessage(@Nullable ExecutionError error) {
        return error == null ? null : error.getMessage();
    }

    private static @Nullable Long asLong(ToolContext toolContext, String key) {
        Object value = toolContext == null ? null : toolContext.getContext()
            .get(key);

        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String string && !string.isBlank()) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException numberFormatException) {
                return null;
            }
        }

        return null;
    }

    private static @Nullable Status parseStatus(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Status.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }
}
```

> Confirm `ExecutionError` exposes `getMessage()`:
> `grep -n "getMessage\|getStackTrace" server/libs/core/exception/exception-api/src/main/java/com/bytechef/error/ExecutionError.java`.
> If the accessor differs, adjust `errorMessage(...)` accordingly. Confirm `ExecutionException(String, Throwable, ErrorType)` exists by checking `ProjectTools` usage (it throws the same way).

- [ ] **Step 4: Run the test — verify it passes**

Run: `./gradlew :server:libs:ai:mcp:mcp-tool:mcp-tool-automation:test --tests "com.bytechef.ai.mcp.tool.automation.WorkflowExecutionToolsTest"`
Expected: 3 tests PASS.

- [ ] **Step 5: Format & commit**

```bash
./gradlew :server:libs:ai:mcp:mcp-tool:mcp-tool-automation:spotlessApply
git add server/libs/ai/mcp/mcp-tool/mcp-tool-automation/src/main/java/com/bytechef/ai/mcp/tool/automation/WorkflowExecutionTools.java \
        server/libs/ai/mcp/mcp-tool/mcp-tool-automation/src/test/java/com/bytechef/ai/mcp/tool/automation/WorkflowExecutionToolsTest.java
git commit -m "0_732 Add WorkflowExecutionTools to mcp-tool-automation

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Phase 2 — Copilot Specialist (`ai-copilot`)

### Task 6: Add `Source.WORKFLOW_EXECUTION`

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/util/Source.java`

- [ ] **Step 1: Add the enum value**

Add `WORKFLOW_EXECUTION` to the `Source` enum (append at the end of the value list):

```java
public enum Source {
    WORKFLOW_EDITOR, CODE_EDITOR, CONVERTER, CLUSTER_ELEMENT, SKILLS, WORKFLOW_EXECUTION
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/util/Source.java
git commit -m "0_732 Add WORKFLOW_EXECUTION copilot source

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 7: `WorkflowExecutionSpringAIAgent`

**Files:**
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/agent/WorkflowExecutionSpringAIAgent.java`

- [ ] **Step 1: Copy the template**

Copy `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/agent/CodeEditorSpringAIAgent.java` to `WorkflowExecutionSpringAIAgent.java` in the same package.

- [ ] **Step 2: Rename the class and constructor/builder**

Replace every `CodeEditorSpringAIAgent` identifier with `WorkflowExecutionSpringAIAgent` (class declaration, constructor, logger, `build()` return, the two `Builder` references). Keep the `OverrideChatClientResolver` field and `resolveChatClient` override exactly as in the template (update the log message class name to `WorkflowExecutionSpringAIAgent`).

- [ ] **Step 3: Replace `ADDITIONAL_RULES`**

```java
    private static final String ADDITIONAL_RULES =
        """
            ## Additional Rules

            - You are diagnosing and (in BUILD mode) helping fix workflow executions. Always inspect the actual run before drawing conclusions: call getWorkflowExecution with the workflowExecutionId from state, or listWorkflowExecutions to resolve a run the user describes.
            - Ground every claim about a failure in the task execution's real error / input / output. Do not speculate about causes you have not read from the execution data.
            - In ASK mode, explain the failure and the fix in plain language; do not mutate the workflow.
            - In BUILD mode, you may modify the workflow definition to fix the problem using the workflow / script tools, then summarise the change and why it addresses the observed error.
            - Do not produce diagrams, charts, or other visual representations.
            """;
```

- [ ] **Step 4: Replace `createSystemMessage` with an execution-aware version**

Replace the whole `createSystemMessage` method body with:

```java
    @Override
    protected SystemMessage createSystemMessage(State state, List<Context> contexts) {
        Object parametersObject = state.get("parameters");

        String executionLine = "";

        if (parametersObject instanceof Map<?, ?> parameters) {
            Object workflowExecutionId = parameters.get("workflowExecutionId");

            if (workflowExecutionId != null) {
                executionLine =
                    "The workflow execution currently on screen has id %s. Inspect it with getWorkflowExecution."
                        .formatted(workflowExecutionId);
            }
        }

        List<String> contextStrings = contexts.stream()
            .map(Context::toString)
            .toList();

        String resolvedMessage = Objects.nonNull(this.systemMessageProvider)
            ? this.systemMessageProvider.apply(this) : this.systemMessage;

        String message = "%s%n%s%n%s%n%nState:%n%s%n%nContext:%n%s%n".formatted(
            resolvedMessage, executionLine, ADDITIONAL_RULES, state, String.join("\n", contextStrings));

        SystemMessage systemMessage = new SystemMessage();

        systemMessage.setId(String.valueOf(UUID.randomUUID()));
        systemMessage.setContent(message);

        return systemMessage;
    }
```

- [ ] **Step 5: Add a `toolContext()` override to populate workspace/environment**

Add this method after `createSystemMessage` (inside the class). It mirrors `WorkflowEditorSpringAIAgent.toolContext` but writes the workspace/environment keys read by `WorkflowExecutionTools`:

```java
    @Override
    protected Map<String, Object> toolContext(RunAgentInput input) {
        State state = input.state();

        Map<String, Object> toolContext = new HashMap<>();

        Object parametersObject = state.get("parameters");

        if (parametersObject instanceof Map<?, ?> parameters) {
            putLong(toolContext, WorkflowExecutionToolContextKeys.WORKSPACE_ID, parameters.get("workspaceId"));
            putLong(toolContext, WorkflowExecutionToolContextKeys.ENVIRONMENT_ID, parameters.get("environmentId"));
        }

        return toolContext;
    }

    private static void putLong(Map<String, Object> target, String key, @Nullable Object value) {
        if (value instanceof Number number) {
            target.put(key, number.longValue());
        } else if (value instanceof String string && !string.isBlank()) {
            try {
                target.put(key, Long.parseLong(string));
            } catch (NumberFormatException numberFormatException) {
                // Leave the key unset — a malformed workspace/environment id degrades to "unscoped",
                // which listWorkflowExecutions treats as an empty result rather than a hard failure.
            }
        }
    }
```

- [ ] **Step 6: Fix imports**

Add these imports (and keep all template imports already present):

```java
import com.bytechef.ai.mcp.tool.automation.WorkflowExecutionToolContextKeys;
import java.util.HashMap;
```

(`Map`, `RunAgentInput`, `State`, `Nullable` are already imported by the template.)

- [ ] **Step 7: Compile**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:compileJava`
Expected: failure — `WorkflowExecutionToolContextKeys` is not yet visible unless `ai-copilot-service` depends on `mcp-tool-automation`. Confirm the dependency exists:
`grep -n "mcp-tool-automation" server/ee/libs/ai/ai-copilot/ai-copilot-service/build.gradle.kts`
It should already be present (the existing copilot beans use `ProjectTools`/`ReadProjectTools` from that module). If missing, add:
```kotlin
    implementation(project(":server:libs:ai:mcp:mcp-tool:mcp-tool-automation"))
```
Then re-run compile. Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Format & commit**

```bash
./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:spotlessApply
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/agent/WorkflowExecutionSpringAIAgent.java \
        server/ee/libs/ai/ai-copilot/ai-copilot-service/build.gradle.kts
git commit -m "0_732 Add WorkflowExecutionSpringAIAgent

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 8: System prompt resources

**Files:**
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_workflow_execution_ask.txt`
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_workflow_execution_build.txt`

- [ ] **Step 1: Confirm the resource directory**

Run: `ls server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/ | grep prompt_workflow_editor`
Expected: `prompt_workflow_editor_ask.txt` and `prompt_workflow_editor_build.txt` exist (confirms the directory/convention).

- [ ] **Step 2: Create `prompt_workflow_execution_ask.txt`**

```
You are the ByteChef Workflow Execution assistant operating in ASK (read-only) mode.

Your job is to help the user understand a workflow execution: whether it succeeded or failed, which task failed, and why. You explain; you do not modify anything.

How to work:
- If the state references a workflowExecutionId, call getWorkflowExecution with that id first.
- If the user describes a run instead of giving an id ("the failed run yesterday"), call listWorkflowExecutions to resolve it, then getWorkflowExecution for the detail.
- Read the actual task executions: their status, input, output, and error. Identify the deepest failed task.
- Explain the failure in plain language and tell the user concretely how to fix it (e.g. correct an input value, fix a connection, adjust a trigger). If the fix requires changing the workflow definition, tell the user to switch to BUILD mode.
- Never invent execution ids, task names, or error messages. Ground every statement in the data you read.
- Do not produce diagrams, charts, images, or other visual representations.
```

- [ ] **Step 3: Create `prompt_workflow_execution_build.txt`**

```
You are the ByteChef Workflow Execution assistant operating in BUILD mode.

Your job is to diagnose a workflow execution and, when appropriate, fix the underlying workflow so the failure does not recur.

How to work:
- First diagnose: call getWorkflowExecution (using the workflowExecutionId from state) or listWorkflowExecutions to resolve the run the user means, and read the failing task's status, input, output, and error.
- Then, if the root cause is in the workflow definition, fix it using the available workflow and script tools. Make the smallest change that addresses the observed error.
- After changing the workflow, summarise exactly what you changed and why it resolves the error you observed in the execution.
- If the cause is data or configuration the user controls (a bad input value, a missing/expired connection), explain that instead of changing the workflow.
- Never invent execution ids, task names, or error messages. Ground every statement in the data you read.
- Do not produce diagrams, charts, images, or other visual representations.
```

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_workflow_execution_ask.txt \
        server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_workflow_execution_build.txt
git commit -m "0_732 Add workflow-execution copilot system prompts

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 9: Register copilot beans (agents + sub-agent ChatClients)

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/config/CopilotConfiguration.java`
- Test: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/config/CopilotConfigurationWorkflowExecutionTest.java`

- [ ] **Step 1: Read the existing wiring to mirror it exactly**

Run:
```bash
sed -n '90,140p' server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/config/CopilotConfiguration.java   # codeEditorAskSpringAIAgent / codeEditorBuildSpringAIAgent
sed -n '290,370p' server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/config/CopilotConfiguration.java   # *SubAgentChatClient beans
grep -n "@Value(\"classpath:prompt_\|promptWorkflowEditor\|getSystemPrompt\|private.*State state\|WorkflowExecutionTools" server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/config/CopilotConfiguration.java
```
Note: the exact builder call chain used by `codeEditorAskSpringAIAgent`, how `state` is obtained, how `getSystemPrompt(resource)` is called, and the `@Value("classpath:prompt_*")` `Resource` field declarations.

- [ ] **Step 2: Add the two prompt `Resource` fields**

Near the other `@Value("classpath:prompt_*")` fields, add:

```java
    @Value("classpath:prompt_workflow_execution_ask.txt")
    private Resource promptWorkflowExecutionAskResource;

    @Value("classpath:prompt_workflow_execution_build.txt")
    private Resource promptWorkflowExecutionBuildResource;
```

- [ ] **Step 3: Add the two agent beans (served to the sheet via `localAgentMap`)**

Add these two beans, mirroring `codeEditorAskSpringAIAgent` / `codeEditorBuildSpringAIAgent` exactly (same `chatMemory`, `chatModel`, `state`, `overrideChatClientResolver` wiring — copy the parameter list and builder chain from those beans, changing only the tools, prompt resource, agentId, and return type). ASK tools = read inspection + read workflow tools; BUILD tools = inspection + write workflow/script tools.

```java
    @Bean
    WorkflowExecutionSpringAIAgent workflowExecutionAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, WorkflowExecutionTools workflowExecutionTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools,
        Optional<FirecrawlTools> firecrawlTools,
        ObjectProvider<CopilotChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        String name = Source.WORKFLOW_EXECUTION.name() + "_" + Mode.ASK.name();

        List<Object> tools = new ArrayList<>(
            List.of(workflowExecutionTools, readProjectWorkflowTools, componentTools));

        firecrawlTools.ifPresent(tools::add);

        return WorkflowExecutionSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowExecutionAskResource))
            .tools(tools)
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    WorkflowExecutionSpringAIAgent workflowExecutionBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, WorkflowExecutionTools workflowExecutionTools,
        ProjectWorkflowTools projectWorkflowTools, ScriptTools scriptTools, TaskTools taskTools,
        ObjectProvider<CopilotChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        String name = Source.WORKFLOW_EXECUTION.name() + "_" + Mode.BUILD.name();

        List<Object> tools = new ArrayList<>(
            List.of(workflowExecutionTools, projectWorkflowTools, scriptTools, taskTools));

        return WorkflowExecutionSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowExecutionBuildResource))
            .tools(tools)
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }
```

> If `codeEditorAskSpringAIAgent` obtains `state` differently (e.g. as a method parameter or a `State.builder()` local), match that exactly rather than referencing a field named `state`.

- [ ] **Step 4: Add the two stateless sub-agent ChatClient beans (for AI Hub delegation)**

Mirror `workflowEditorAskSubAgentChatClient` / `workflowEditorBuildSubAgentChatClient` (no `ChatMemory`):

```java
    @Bean
    ChatClient workflowExecutionAskSubAgentChatClient(
        ChatModel chatModel, WorkflowExecutionTools workflowExecutionTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools,
        Optional<FirecrawlTools> firecrawlTools) {

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem(getSystemPrompt(promptWorkflowExecutionAskResource));

        if (firecrawlTools.isPresent()) {
            builder.defaultTools(
                workflowExecutionTools, readProjectWorkflowTools, componentTools, firecrawlTools.get());
        } else {
            builder.defaultTools(workflowExecutionTools, readProjectWorkflowTools, componentTools);
        }

        return builder.build();
    }

    @Bean
    ChatClient workflowExecutionBuildSubAgentChatClient(
        ChatModel chatModel, WorkflowExecutionTools workflowExecutionTools, ProjectWorkflowTools projectWorkflowTools,
        ScriptTools scriptTools, TaskTools taskTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(getSystemPrompt(promptWorkflowExecutionBuildResource))
            .defaultTools(workflowExecutionTools, projectWorkflowTools, scriptTools, taskTools)
            .build();
    }
```

- [ ] **Step 5: Add imports**

Add to `CopilotConfiguration.java` imports (skip any already present):

```java
import com.bytechef.ai.mcp.tool.automation.WorkflowExecutionTools;
import com.bytechef.ee.ai.copilot.agent.WorkflowExecutionSpringAIAgent;
```

- [ ] **Step 6: Write the wiring test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.ai.copilot.util.Source;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CopilotConfigurationWorkflowExecutionTest {

    @Test
    void testWorkflowExecutionSourceExists() {
        assertThat(Source.valueOf("WORKFLOW_EXECUTION")).isEqualTo(Source.WORKFLOW_EXECUTION);
    }

    @Test
    void testAgentIdsAreLowerSnakeCase() {
        assertThat(Source.WORKFLOW_EXECUTION.name()
            .toLowerCase() + "_ask").isEqualTo("workflow_execution_ask");
        assertThat(Source.WORKFLOW_EXECUTION.name()
            .toLowerCase() + "_build").isEqualTo("workflow_execution_build");
    }
}
```

> A full `@SpringBootTest` context test is heavier than this module's existing test style; this lightweight test pins the contract (`Source` value + the agentId strings the sheet/router depend on). If the module already has a Spring context wiring test for copilot beans, add `workflowExecutionAskSpringAIAgent`/`workflowExecutionBuildSpringAIAgent` bean-presence assertions there instead.

- [ ] **Step 7: Compile, test, format**

Run:
```bash
./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:compileJava
./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests "com.bytechef.ee.ai.copilot.config.CopilotConfigurationWorkflowExecutionTest"
./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:spotlessApply
```
Expected: compile SUCCESS, 2 tests PASS.

- [ ] **Step 8: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/config/CopilotConfiguration.java \
        server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/config/CopilotConfigurationWorkflowExecutionTest.java
git commit -m "0_732 Wire workflow-execution copilot agent + sub-agent ChatClient beans

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 9b: Route the `workflow_execution` source in `CopilotApiController` (ADDED during execution)

> Discovered by the final integration review: `CopilotApiController.chat` does NOT generically derive
> `{agentId}_{mode}` — it has an explicit per-source `if/else` chain. Without a `workflow_execution`
> branch, the client's POST to `/ai/chat/workflow_execution` leaves `agentId` unmapped, so
> `localAgentMap.get("workflow_execution")` returns null and the sheet panel NPEs. The agent beans are
> registered as `workflow_execution_ask`/`workflow_execution_build`, so the router must translate.

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-rest/src/main/java/com/bytechef/ee/ai/copilot/web/rest/CopilotApiController.java`

- [ ] **Step 1: Add the routing branch** (place it right after the `workflow_editor` block):

```java
        } else if (agentId.equals("workflow_execution")) {
            if (Mode.valueOf((String) mode) == Mode.BUILD) {
                agentId = "workflow_execution_build";
            } else {
                agentId = "workflow_execution_ask";
            }
```

- [ ] **Step 2: Compile + format + commit**

```bash
./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-rest:spotlessApply :server:ee:libs:ai:ai-copilot:ai-copilot-rest:compileJava
git add server/ee/libs/ai/ai-copilot/ai-copilot-rest/src/main/java/com/bytechef/ee/ai/copilot/web/rest/CopilotApiController.java
git commit -m "0_732 Route workflow_execution source to ask/build copilot agents

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Phase 3 — AI Hub Delegation

### Task 10: Append `Agent.WORKFLOW_EXECUTION_AGENT`

**Files:**
- Modify: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/usage/Agent.java`

- [ ] **Step 1: Append the enum value**

The enum is persisted by ordinal — **append at the very end** (after `CONVERTER_AGENT`). Change the line:

```java
    CONVERTER_AGENT("converter_agent", false);
```
to:
```java
    CONVERTER_AGENT("converter_agent", false),
    WORKFLOW_EXECUTION_AGENT("workflow_execution_agent", false);
```

- [ ] **Step 2: Check for an ordinal-stability test**

Run: `grep -rln "EnumOrdinalStability\|Agent.values()\|ordinal" server/ee/libs/platform/platform-ai-hub --include="*.java"`
If a test pins `Agent` ordinals/count, update its expected list to include `WORKFLOW_EXECUTION_AGENT` at the end.

- [ ] **Step 3: Compile**

Run: `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/usage/Agent.java
git commit -m "0_732 Append WORKFLOW_EXECUTION_AGENT to Agent enum

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 11: `WorkflowExecutionAgentToolCallback` (TDD)

**Files:**
- Create: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/tool/WorkflowExecutionAgentToolCallback.java`
- Test: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/test/java/com/bytechef/ee/platform/aihub/tool/WorkflowExecutionAgentToolCallbackTest.java`

- [ ] **Step 1: Write the failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.aihub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkflowExecutionAgentToolCallbackTest {

    @Test
    void testToolDefinitionName() {
        ChatClient chatClient = mock(ChatClient.class);

        WorkflowExecutionAgentToolCallback callback = new WorkflowExecutionAgentToolCallback(chatClient);

        assertThat(callback.getToolDefinition()
            .name()).isEqualTo("workflow_execution_agent");
    }

    @Test
    void testCallRejectsBlankRequest() {
        ChatClient chatClient = mock(ChatClient.class);

        WorkflowExecutionAgentToolCallback callback = new WorkflowExecutionAgentToolCallback(chatClient);

        String result = callback.call("{\"request\": \"\"}", null);

        assertThat(result).contains("request");
    }
}
```

> Mirror the existing `WorkflowEditorAgentToolCallbackTest` if one exists (`ls server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/test/java/com/bytechef/ee/platform/aihub/tool/`). Match its mocking style for the `ChatClient.prompt(...).tools(...).call().content()` fluent chain if a delegation-path test is included there.

- [ ] **Step 2: Run — verify it fails to compile**

Run: `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:test --tests "com.bytechef.ee.platform.aihub.tool.WorkflowExecutionAgentToolCallbackTest"`
Expected: compilation failure — class does not exist.

- [ ] **Step 3: Implement by copying the template**

Copy `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/tool/WorkflowEditorAgentToolCallback.java` to `WorkflowExecutionAgentToolCallback.java` and make exactly these edits:
- Rename the class to `WorkflowExecutionAgentToolCallback`; rename the field `workflowEditorChatClient` → `workflowExecutionChatClient` and the input record `WorkflowEditorAgentInput` → `WorkflowExecutionAgentInput`.
- Change the tool name in `getToolDefinition()` from `"workflow_editor_agent"` to `"workflow_execution_agent"`.
- Change `CurrentAgentContext.callWith(Agent.WORKFLOW_EDITOR_AGENT, ...)` to `Agent.WORKFLOW_EXECUTION_AGENT`.
- Replace `DESCRIPTION` with:

```java
    private static final String DESCRIPTION =
        """
            Delegate a user request about a workflow execution (a past run) to a specialised Workflow Execution
            subagent. Use this to inspect or diagnose a run — why it failed, which task errored, what a task's
            input/output was — and, in BUILD mode, to fix the underlying workflow. Pass the user request verbatim;
            the subagent resolves the execution and does its own analysis. Returns the synthesised analysis (ASK) or
            the applied fix plus rationale (BUILD).""";
```
- Keep everything else identical (the `INPUT_SCHEMA` `{request}` shape, the `call(...)` body, `ProgressReportingToolCallback`-friendly signatures, log lines updated to the new tool name).

- [ ] **Step 4: Run — verify it passes**

Run: `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:test --tests "com.bytechef.ee.platform.aihub.tool.WorkflowExecutionAgentToolCallbackTest"`
Expected: 2 tests PASS.

- [ ] **Step 5: Format & commit**

```bash
./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:spotlessApply
git add server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/tool/WorkflowExecutionAgentToolCallback.java \
        server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/test/java/com/bytechef/ee/platform/aihub/tool/WorkflowExecutionAgentToolCallbackTest.java
git commit -m "0_732 Add WorkflowExecutionAgentToolCallback

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 12: Register the delegation tool in `AiHubConfiguration`

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/config/AiHubConfiguration.java`

- [ ] **Step 1: Add the ASK-agent ChatClient provider parameter**

In the ASK agent bean method (the one whose parameter list around line 197–210 declares `@Qualifier("workflowEditorAskSubAgentChatClient") ObjectProvider<ChatClient> workflowEditorAskSubAgentChatClientProvider`), add a sibling parameter:

```java
        @Qualifier("workflowExecutionAskSubAgentChatClient") //
        ObjectProvider<ChatClient> workflowExecutionAskSubAgentChatClientProvider,
```

- [ ] **Step 2: Add the BUILD-agent ChatClient provider parameter**

In the BUILD agent bean method (parameter list around line 324–339), add:

```java
        @Qualifier("workflowExecutionBuildSubAgentChatClient") //
        ObjectProvider<ChatClient> workflowExecutionBuildSubAgentChatClientProvider,
```

- [ ] **Step 3: Extend `registerCopilotSubAgentToolCallbacks` signature + body**

Add a parameter and registration to the helper (around line 665–698). Add this parameter at the end of the parameter list:

```java
        ObjectProvider<ChatClient> workflowExecutionSubAgentChatClientProvider,
```

And add this registration block alongside the others (after the `workflowEditorSubAgentChatClientProvider` block):

```java
        workflowExecutionSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new WorkflowExecutionAgentToolCallback(chatClient), "workflow_execution_agent")));
```

- [ ] **Step 4: Pass the new providers at both call sites**

In the ASK call site (around line 287) add `workflowExecutionAskSubAgentChatClientProvider` as the new trailing argument; in the BUILD call site (around line 431) add `workflowExecutionBuildSubAgentChatClientProvider`. Keep argument order matching the updated signature.

- [ ] **Step 5: Add the import**

```java
import com.bytechef.ee.platform.aihub.tool.WorkflowExecutionAgentToolCallback;
```

- [ ] **Step 6: Compile**

Run: `./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:spotlessApply
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/config/AiHubConfiguration.java
git commit -m "0_732 Register workflow_execution_agent delegation tool in AI Hub

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 13: Remove the old `ListWorkflowExecutionsToolCallback`

**Files:**
- Delete: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/ListWorkflowExecutionsToolCallback.java`
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/config/AiHubConfiguration.java`

- [ ] **Step 1: Find every reference**

Run: `grep -rn "ListWorkflowExecutionsToolCallback" server/ee/libs/automation/automation-ai-hub`
Expected: the class file, its two registrations in `AiHubConfiguration` (around lines 271 and 470), the import, and possibly a test.

- [ ] **Step 2: Remove the two registrations + import**

In `AiHubConfiguration.java`, delete the two lines that do `toolCallbacks.add(new ListWorkflowExecutionsToolCallback(...))` (ASK ~271, BUILD ~470) and the `import ...ListWorkflowExecutionsToolCallback;` line. If the `ProjectWorkflowExecutionFacade` constructor parameter / field was used *only* by that registration, remove it too; if it is used elsewhere, leave it.

- [ ] **Step 3: Delete the class (and its test if present)**

```bash
git rm server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/ListWorkflowExecutionsToolCallback.java
# If a test exists (from Step 1), remove it too:
# git rm server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/tool/ListWorkflowExecutionsToolCallbackTest.java
```

- [ ] **Step 4: Compile the module**

Run: `./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:compileJava`
Expected: BUILD SUCCESSFUL (no dangling references).

- [ ] **Step 5: Commit**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:spotlessApply
git add -A server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service
git commit -m "0_732 Remove AI Hub ListWorkflowExecutionsToolCallback (moved to mcp-tool-automation)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Phase 4 — Client

### Task 14: Add `Source.WORKFLOW_EXECUTION`

**Files:**
- Modify: `client/src/shared/components/copilot/stores/useCopilotStore.ts`

- [ ] **Step 1: Add the enum member (keep keys alphabetical per sort-keys rule)**

The `Source` enum currently is:

```ts
export enum Source {
    WORKFLOW_EDITOR = 'WORKFLOW_EDITOR',
    CODE_EDITOR = 'CODE_EDITOR',
    CLUSTER_ELEMENT = 'CLUSTER_ELEMENT',
    SKILLS = 'SKILLS',
}
```

Add `WORKFLOW_EXECUTION`. The existing enum is not alphabetically ordered, so match the existing local style (append after `SKILLS`) unless ESLint flags `sort-keys` on enums — if it does, place members in ascending order:

```ts
export enum Source {
    CLUSTER_ELEMENT = 'CLUSTER_ELEMENT',
    CODE_EDITOR = 'CODE_EDITOR',
    SKILLS = 'SKILLS',
    WORKFLOW_EDITOR = 'WORKFLOW_EDITOR',
    WORKFLOW_EXECUTION = 'WORKFLOW_EXECUTION',
}
```

- [ ] **Step 2: Typecheck**

Run (from `client/`): `npm run typecheck`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add client/src/shared/components/copilot/stores/useCopilotStore.ts
git commit -m "0_732 client - Add WORKFLOW_EXECUTION copilot source

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 15: Repoint the execution-sheet Copilot panel

**Files:**
- Modify: `client/src/pages/automation/workflow-executions/components/workflow-execution-sheet/hooks/useWorkflowExecutionSheet.ts`
- Test: `client/src/pages/automation/workflow-executions/components/workflow-execution-sheet/tests/WorkflowExecutionDetail.test.tsx` (only if it asserts the source)

- [ ] **Step 1: Read the current hook**

Run: `sed -n '1,90p' client/src/pages/automation/workflow-executions/components/workflow-execution-sheet/hooks/useWorkflowExecutionSheet.ts`
Note how `handleCopilotClick` builds `setContext({...})`, where `workflowExecutionId` is available in the hook (it comes from `useWorkflowExecutionSheetStore`), and whether `workspaceId` is available (e.g. via a current-workspace store/hook used elsewhere on the page).

- [ ] **Step 2: Update `handleCopilotClick`**

Change the `setContext(...)` call so the source is `WORKFLOW_EXECUTION` and the parameters carry the execution id (and workspace/environment when available for the list tool). Replace the existing object:

```ts
        setContext({
            ...currentContext,
            mode: MODE.ASK,
            parameters: {},
            source: Source.WORKFLOW_EDITOR,
        });
```
with:
```ts
        setContext({
            ...currentContext,
            mode: MODE.ASK,
            parameters: {
                environmentId: currentEnvironmentId,
                workflowExecutionId,
                workflowId,
                workspaceId: currentWorkspaceId,
            },
            source: Source.WORKFLOW_EXECUTION,
        });
```

> Keep object keys in ascending order (`environmentId`, `workflowExecutionId`, `workflowId`, `workspaceId`, then `source`) to satisfy `sort-keys`. Source `workflowExecutionId`/`workflowId` from the sheet store/detail (they are already used to fetch the execution).
>
> **`workspaceId` is REQUIRED** — the server `getWorkflowExecution` tool fails closed (returns a "not found" error) when the caller's workspace is unknown, as an IDOR guard. Read the current workspace id from the same store/hook the rest of the automation page uses (find it with: `grep -rn "currentWorkspaceId\|useWorkspaceStore\|workspaceId" client/src/pages/automation/workflow-executions client/src/shared/stores | head`). Wire it into `parameters.workspaceId`. If a current workspace id is genuinely unavailable on this surface, STOP and report it — the Copilot panel cannot function without it. `environmentId` is best-effort (improves `listWorkflowExecutions` scoping); omit it if not readily available.

- [ ] **Step 3: Typecheck + lint + tests**

Run (from `client/`):
```bash
npm run typecheck
npm run test -- WorkflowExecutionDetail
```
Expected: typecheck clean; existing sheet tests pass. If a test asserted `Source.WORKFLOW_EDITOR`, update it to `Source.WORKFLOW_EXECUTION`.

- [ ] **Step 4: Full client check**

Run (from `client/`): `npm run check`
Expected: lint + typecheck + tests all pass.

- [ ] **Step 5: Commit**

```bash
git add client/src/pages/automation/workflow-executions/components/workflow-execution-sheet/
git commit -m "0_732 client - Point execution-sheet Copilot panel at WORKFLOW_EXECUTION agent

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Phase 5 — Full Verification

### Task 16: Build & check everything touched

- [ ] **Step 1: Server compile + targeted tests**

Run:
```bash
./gradlew spotlessApply
./gradlew :server:libs:ai:mcp:mcp-tool:mcp-tool-automation:test \
          :server:ee:libs:ai:ai-copilot:ai-copilot-service:test \
          :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:test \
          :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:compileJava
```
Expected: all green.

- [ ] **Step 2: Broader server check (optional but recommended before PR)**

Run: `./gradlew check`
Expected: BUILD SUCCESSFUL. Investigate any checkstyle/PMD/SpotBugs findings in the new files and fix per the Conventions section.

- [ ] **Step 3: Client check**

Run (from `client/`): `npm run check`
Expected: pass.

- [ ] **Step 4: Manual smoke (when running locally)**

1. Open a workflow execution → open the Copilot panel (Sparkles, behind `ff_4077`). Ask "why did this run fail?" — the agent should call `getWorkflowExecution` and explain the failing task.
2. In AI Hub, ask about "the last failed run of <workflow>" — the parent agent should call `workflow_execution_agent`, which resolves via `listWorkflowExecutions` + `getWorkflowExecution`.

- [ ] **Step 5: Finalize**

Use the `superpowers:finishing-a-development-branch` skill to decide on merge/PR.

---

## Self-Review Notes (coverage map)

| Spec section | Task(s) |
| --- | --- |
| §3 L1 — tools in `mcp-tool-automation` (getWorkflowExecution, listWorkflowExecutions) | 1–5 |
| §3 L1 — remove old `ListWorkflowExecutionsToolCallback` | 13 |
| §3 L2 — `Source.WORKFLOW_EXECUTION` | 6 |
| §3 L2 — `WorkflowExecutionSpringAIAgent` + prompts | 7, 8 |
| §3 L2 — agent beans + sub-agent ChatClient beans | 9 |
| §3 L2 — `CopilotApiController` source→agent routing | 9b |
| §3 L3 — `Agent.WORKFLOW_EXECUTION_AGENT` | 10 |
| §3 L3 — `WorkflowExecutionAgentToolCallback` | 11 |
| §3 L3 — register in `AiHubConfiguration` | 12 |
| §3 L4 — client Source + sheet repoint | 14, 15 |
| §3 L5 — tests | 5, 9, 11, 15 |
| §4 — workspace/env scoping (CE keys + toolContext override) | 2, 5 (tool), 7 (agent override) |
