# Server-side Workflow-Artifact Attach Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Record the AI Hub task artifact server-side the moment the workflow-editor agent persists a workflow (create or update), so attachment no longer depends on the LLM relaying ids back or the client `openWorkflowTab` hook firing.

**Architecture:** A neutral CE SPI `WorkflowArtifactRecorder` (in `automation-ai-tool`) is called by the CE persist tools `ProjectWorkflowTools.createProjectWorkflow`/`updateWorkflow` right after a successful save. The EE `ai-hub-service` provides the impl, which extracts the AI Hub `conversationId` (= threadId) + `userId` from the forwarded `ToolContext` via the existing `AgentToolInvocationContext`, then records a `WORKFLOW_CREATED`/`WORKFLOW_UPDATED` artifact through a new dedup-aware service method keyed on `(taskId, workflowId)`. The existing `openWorkflowTab` path is rerouted through the same dedup method so create + update + reference converge on one sidebar row. Surfaces without an AI Hub task (Copilot panel, embedded autonomous, plain editor) record nothing.

**Tech Stack:** Java 25, Spring Boot 4, Spring AI tool API (`@Tool`/`ToolContext`), Spring Data JDBC, JUnit 5 + Mockito + AssertJ. CE module under `server/libs/` (Apache header), EE modules under `server/ee/` (Enterprise header + `@version ee`).

---

## Background the implementer must know

- **CE persist tools:** `ProjectWorkflowTools` (`server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ProjectWorkflowTools.java`).
  - `createProjectWorkflow(long projectId, String definition)` calls `projectWorkflowFacade.addWorkflow(projectId, definition)` → returns `ProjectWorkflow` domain (`getWorkflowId()` = workflow string id, `getId()` = project_workflow row id (Long), `getProjectId()` = long).
  - `updateWorkflow(String workflowId, String definition)` fetches `ProjectWorkflowDTO` via `projectWorkflowFacade.getProjectWorkflow(workflowId)` (`getProjectWorkflowId()` = Long, `getLabel()`, but NO projectId), then calls `projectWorkflowFacade.updateWorkflow(dto.getId(), definition, dto.getVersion())`.
- **projectId on the update path** is resolved via `ProjectWorkflowService.getProjectWorkflow(long projectWorkflowId)` → `ProjectWorkflow.getProjectId()` (`com.bytechef.automation.configuration.service.ProjectWorkflowService`, available transitively through `automation-configuration-api`, already a dependency).
- **The editor subagent's tools receive the parent agent's `ToolContext`:** `WorkflowEditorAgentToolCallback` (EE `ai-copilot-tool`) forwards `toolContext.getContext()` into the subagent chat client via `.toolContext(forwardedContext)`. `AiHubSpringAIAgent.toolContext()` puts `bytechef.agentTool.conversationId = threadId` into that map. So once a `@Tool` method declares a `ToolContext` parameter, it receives the AI Hub conversation context. A `ToolContext` parameter is NOT a `@ToolParam`, so it does not appear in the tool's JSON input schema.
- **Neutral context reader:** `AgentToolInvocationContext.fromToolContext(ToolContext)` (EE `ai-copilot-tool`, `com.bytechef.ee.ai.copilot.tool`) returns `{workspaceId, userId, environmentId, conversationId}` or `null`. `ai-hub-service` already depends on `ai-copilot-tool`.
- **Artifact kinds:** `AiHubTaskArtifactKind` (EE `ai-hub-service`, `com.bytechef.ee.ai.hub.task`) already has `WORKFLOW_CREATED`, `WORKFLOW_UPDATED`, `WORKFLOW_REFERENCED` (append-only enum; ordinals pinned by `EnumOrdinalStabilityTest`).
- **Existing recording:** `AiHubTaskArtifactService.record(threadId, userId, kind, artifactId, artifactName, metadata)` (EE) inserts WITHOUT dedup (correct for append-only kinds like `FILE_CREATED`). `OpenWorkflowTabToolCallback.recordArtifact` calls the `AiHubTaskArtifactRecorder` SPI (`com.bytechef.ee.ai.hub.tool`) → `record(...)` with `WORKFLOW_REFERENCED` and metadata `Map.of("projectId", <String>, "projectWorkflowId", <Long>)`.
- **`ai-hub-service` depends on both** `automation-ai-tool` (CE) and `ai-copilot-tool` (EE) — so an SPI defined in CE `automation-ai-tool` can be implemented in EE `ai-hub-service`. CE never depends on EE.
- **Metadata type compatibility:** the existing working `WORKFLOW_REFERENCED` rows store `projectId` as a **String** and `projectWorkflowId` as a **Long**. The new recording MUST match these types so the client sidebar quick-open keeps working.
- **Module commands:**
  - CE tool module: `./gradlew :server:libs:automation:automation-ai:automation-ai-tool:test`
  - EE hub service module: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test`
  - Spotless (scope to module to avoid touching unrelated parallel work):
    `./gradlew :server:libs:automation:automation-ai:automation-ai-tool:spotlessApply`
    `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:spotlessApply`
- **Commit convention:** server-side → `0_732 <description>`. End commit messages with the `Co-Authored-By` trailer. NEVER `git commit --amend` (the user commits in parallel on `0_732`); only `git add` the files this task changed.

---

## File Structure

**Create:**
- `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/WorkflowArtifactRecorder.java` — CE SPI the persist tools call (Task 1).
- `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/task/WorkflowArtifactRecorderImpl.java` — EE impl of the CE SPI (Task 4).
- `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/task/WorkflowArtifactRecorderImplTest.java` — Task 4 test.

**Modify:**
- `…/automation-ai-tool/.../ProjectWorkflowTools.java` — record on create/update (Task 2).
- `…/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/ProjectWorkflowToolsTest.java` — Task 2 tests.
- `…/ai-hub-service/.../task/repository/AiHubTaskArtifactRepository.java` — dedup finder (Task 3).
- `…/ai-hub-api/.../task/AiHubTaskArtifactService.java` — `recordWorkflowArtifact` method (Task 3).
- `…/ai-hub-service/.../task/AiHubTaskArtifactServiceImpl.java` — impl + dedup (Task 3).
- `…/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/task/AiHubTaskArtifactServiceTest.java` — Task 3 tests.
- `…/ai-hub-service/.../tool/AiHubTaskArtifactRecorder.java` — `recordWorkflowReference` method (Task 5).
- `…/ai-hub-service/.../task/AiHubTaskArtifactRecorderImpl.java` — route to dedup method (Task 5).
- `…/ai-hub-service/.../tool/OpenWorkflowTabToolCallback.java` — use the new reference recorder (Task 5).
- `…/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/OpenWorkflowTabToolCallbackTest.java` — Task 5 tests.

---

## Task 1: CE SPI `WorkflowArtifactRecorder`

**Files:**
- Create: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/WorkflowArtifactRecorder.java`

This task introduces the contract only (an interface declaration — no behavior to test in isolation; it is exercised by Task 2's tests via a mock). Keep it minimal.

- [ ] **Step 1: Create the interface**

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

package com.bytechef.automation.ai.tool;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;

/**
 * Surface-neutral seam that lets the workflow-persist tools record the persisted workflow as a task
 * artifact, without coupling the (community) tool module to any enterprise AI Hub type. The
 * implementation is provided by the enterprise service layer and is optional: when absent (community
 * build, no AI Hub), the persist tools skip recording.
 *
 * <p>
 * The whole {@link ToolContext} is passed through so the implementation owns extraction of the AI Hub
 * conversation id and user id from it — the community module stays ignorant of the
 * {@code bytechef.agentTool.*} context keys.
 *
 * @author Ivica Cardic
 */
public interface WorkflowArtifactRecorder {

    /**
     * Records the just-persisted workflow as a task artifact when the invocation carries an AI Hub
     * conversation context; a no-op otherwise. Must be best-effort: never throw out of this method in
     * a way that fails the workflow persist.
     *
     * @param toolContext       the tool invocation context (carries the optional conversation id)
     * @param created           {@code true} for a freshly created workflow, {@code false} for an update
     * @param workflowId        the workflow id (artifact id used to open the workflow)
     * @param projectId         owning project id (routing metadata)
     * @param projectWorkflowId project-workflow id (routing metadata), may be {@code null}
     * @param workflowName      display-name snapshot
     */
    void recordWorkflowArtifact(
        @Nullable ToolContext toolContext, boolean created, String workflowId, long projectId,
        @Nullable Long projectWorkflowId, String workflowName);
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-tool:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/WorkflowArtifactRecorder.java
git commit -m "$(cat <<'EOF'
0_732 Add CE WorkflowArtifactRecorder SPI for workflow-tool artifact attach

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: CE — `ProjectWorkflowTools` records on create/update

**Files:**
- Modify: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ProjectWorkflowTools.java`
- Test: `server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/ProjectWorkflowToolsTest.java`

- [ ] **Step 1: Write the failing tests**

Replace the entire body of `ProjectWorkflowToolsTest` with the following. It mocks the facade,
service, and recorder; verifies the recorder is invoked with the right arguments on create and
update; verifies no-op when the recorder provider is empty; and verifies a recorder exception does
not fail the persist.

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

package com.bytechef.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.tool.testsupport.ObjectMapperSetupExtension;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Marko Kriskovic
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class ProjectWorkflowToolsTest {

    private static final String DEFINITION = """
        {"label": "My Flow", "triggers": [], "tasks": []}""";

    @Mock
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private ObjectProvider<WorkflowArtifactRecorder> recorderProvider;

    @Mock
    private WorkflowArtifactRecorder recorder;

    @Mock
    private ToolContext toolContext;

    @Test
    void testCreateProjectWorkflowRecordsArtifact() {
        when(recorderProvider.getIfAvailable()).thenReturn(recorder);

        ProjectWorkflow projectWorkflow = new ProjectWorkflow();

        projectWorkflow.setId(55L);
        projectWorkflow.setProjectId(7L);
        projectWorkflow.setWorkflowId("wf-uuid-1");

        when(projectWorkflowFacade.addWorkflow(7L, DEFINITION)).thenReturn(projectWorkflow);

        ProjectWorkflowTools tools = newTools();

        tools.createProjectWorkflow(7L, DEFINITION, toolContext);

        verify(recorder).recordWorkflowArtifact(toolContext, true, "wf-uuid-1", 7L, 55L, "My Flow");
    }

    @Test
    void testUpdateWorkflowRecordsArtifact() {
        when(recorderProvider.getIfAvailable()).thenReturn(recorder);

        ProjectWorkflowDTO dto = buildDto("wf-uuid-2", 88L, "Updated Flow", 3);

        when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-2")).thenReturn(dto);

        ProjectWorkflow projectWorkflow = new ProjectWorkflow();

        projectWorkflow.setProjectId(9L);

        when(projectWorkflowService.getProjectWorkflow(88L)).thenReturn(projectWorkflow);

        ProjectWorkflowTools tools = newTools();

        tools.updateWorkflow("wf-uuid-2", DEFINITION, toolContext);

        verify(recorder).recordWorkflowArtifact(toolContext, false, "wf-uuid-2", 9L, 88L, "My Flow");
    }

    @Test
    void testCreateProjectWorkflowSkipsRecordingWhenNoRecorder() {
        when(recorderProvider.getIfAvailable()).thenReturn(null);

        ProjectWorkflow projectWorkflow = new ProjectWorkflow();

        projectWorkflow.setId(55L);
        projectWorkflow.setProjectId(7L);
        projectWorkflow.setWorkflowId("wf-uuid-1");

        when(projectWorkflowFacade.addWorkflow(7L, DEFINITION)).thenReturn(projectWorkflow);

        ProjectWorkflowTools tools = newTools();

        tools.createProjectWorkflow(7L, DEFINITION, toolContext);

        verify(recorder, never()).recordWorkflowArtifact(any(), eq(true), any(), anyLong(), any(), any());
    }

    @Test
    void testCreateProjectWorkflowSucceedsWhenRecorderThrows() {
        when(recorderProvider.getIfAvailable()).thenReturn(recorder);

        ProjectWorkflow projectWorkflow = new ProjectWorkflow();

        projectWorkflow.setId(55L);
        projectWorkflow.setProjectId(7L);
        projectWorkflow.setWorkflowId("wf-uuid-1");

        when(projectWorkflowFacade.addWorkflow(7L, DEFINITION)).thenReturn(projectWorkflow);

        doThrow(new RuntimeException("boom"))
            .when(recorder)
            .recordWorkflowArtifact(any(), eq(true), any(), anyLong(), any(), any());

        ProjectWorkflowTools tools = newTools();

        assertThatCode(() -> tools.createProjectWorkflow(7L, DEFINITION, toolContext))
            .doesNotThrowAnyException();
    }

    private ProjectWorkflowTools newTools() {
        return new ProjectWorkflowTools(projectWorkflowFacade, projectWorkflowService, recorderProvider);
    }

    private static ProjectWorkflowDTO buildDto(String workflowUuid, long projectWorkflowId, String label, int version) {
        com.bytechef.atlas.configuration.domain.Workflow workflow = new com.bytechef.atlas.configuration.domain.Workflow();

        workflow.setId(workflowUuid);
        workflow.setLabel(label);
        workflow.setVersion(version);

        ProjectWorkflow projectWorkflow = new ProjectWorkflow();

        projectWorkflow.setId(projectWorkflowId);

        return new ProjectWorkflowDTO(workflow, projectWorkflow, false);
    }
}
```

> Note on `ObjectMapperSetupExtension`: it is required because the production code parses the workflow
> label out of the JSON `definition` via `JsonUtils`, and `JsonUtils` needs the static `ObjectMapper`
> configured. The extension lives in `test-support` (already a `testImplementation` dependency of
> `automation-ai-tool`). If the import path differs in this repo, locate it with
> `find server -name ObjectMapperSetupExtension.java` and use that package; do not hand-configure the
> mapper.
>
> Note on domain setters (`ProjectWorkflow.setId/setProjectId/setWorkflowId`,
> `Workflow.setId/setLabel/setVersion`): confirm these setters exist on the domain classes. If a
> field is constructor-only, build the object via its available constructor instead — the test only
> needs the getters the production code reads (`getWorkflowId`, `getProjectId`, `getId` on
> `ProjectWorkflow`; `getId`, `getLabel` on `ProjectWorkflowDTO`).

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-tool:test --tests "com.bytechef.automation.ai.tool.ProjectWorkflowToolsTest"`
Expected: FAIL to compile / fail — `ProjectWorkflowTools` has no 3-arg constructor and `createProjectWorkflow`/`updateWorkflow` take no `ToolContext`.

- [ ] **Step 3: Implement the production change**

Edit `ProjectWorkflowTools.java`:

1. Add imports:

```java
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.JsonUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.ObjectProvider;
```

2. Add fields + replace the constructor:

```java
    private final ProjectWorkflowFacade projectWorkflowFacade;
    private final ProjectWorkflowService projectWorkflowService;
    private final ObjectProvider<WorkflowArtifactRecorder> workflowArtifactRecorderProvider;

    @SuppressFBWarnings("EI")
    public ProjectWorkflowTools(
        ProjectWorkflowFacade projectWorkflowFacade, ProjectWorkflowService projectWorkflowService,
        ObjectProvider<WorkflowArtifactRecorder> workflowArtifactRecorderProvider) {

        this.projectWorkflowFacade = projectWorkflowFacade;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowArtifactRecorderProvider = workflowArtifactRecorderProvider;
    }
```

3. Change the `createProjectWorkflow` signature to accept a `ToolContext` (NOT a `@ToolParam`) and
   record after the successful `addWorkflow`. The method becomes:

```java
    @Tool(
        description = "Create a new workflow in a ByteChef project. Returns the created workflow information including id, project id, workflow id, and reference code.")
    public ProjectWorkflowInfo createProjectWorkflow(
        @ToolParam(description = "The ID of the project to add the workflow to") long projectId,
        @ToolParam(
            description = "The definition for the workflow. Needs to be in JSON format similar to " +
                DEFAULT_DEFINITION) String definition,
        ToolContext toolContext) {

        try {
            ProjectWorkflow projectWorkflow = projectWorkflowFacade.addWorkflow(projectId, definition);

            if (log.isDebugEnabled()) {
                log.debug("createProjectWorkflow({}): Created workflow for project {}", projectId, projectId);
            }

            recordWorkflowArtifact(
                toolContext, true, projectWorkflow.getWorkflowId(), projectWorkflow.getProjectId(),
                projectWorkflow.getId(), extractWorkflowName(definition));

            return new ProjectWorkflowInfo(
                projectWorkflow.getId(), projectWorkflow.getProjectId(), projectWorkflow.getProjectVersion(),
                projectWorkflow.getWorkflowId(), projectWorkflow.getUuidAsString(),
                projectWorkflow.getCreatedDate() != null ? projectWorkflow.getCreatedDate() : null,
                projectWorkflow.getLastModifiedDate() != null ? projectWorkflow.getLastModifiedDate() : null);
        } catch (Exception e) {
            log.error(
                "createProjectWorkflow({}): Failed to create workflow for project {}", projectId, projectId, e);

            throw new ExecutionException(
                "Failed to create project workflow: " + e.getMessage(), e,
                ProjectWorkflowToolErrorType.CREATE_WORKFLOW);
        }
    }
```

4. Change the `updateWorkflow` signature to accept a `ToolContext` and record after the successful
   `updateWorkflow`. The method becomes:

```java
    @Tool(
        description = "Update the workflow definition. Returns the updated workflow id, name and definition.")
    public WorkflowInfo updateWorkflow(
        @ToolParam(description = "The ID of the workflow to update") String workflowId,
        @ToolParam(
            description = "The new definition of the workflow. Needs to be in JSON format similar to " +
                DEFAULT_DEFINITION) String definition,
        ToolContext toolContext) {

        try {
            ProjectWorkflowDTO projectWorkflowDTO = projectWorkflowFacade.getProjectWorkflow(workflowId);

            projectWorkflowFacade.updateWorkflow(
                projectWorkflowDTO.getId(), definition, projectWorkflowDTO.getVersion());

            if (log.isDebugEnabled()) {
                log.debug(
                    "updateWorkflow({}): Updated workflow {} with name '{}'", workflowId, projectWorkflowDTO.getId(),
                    projectWorkflowDTO.getLabel());
            }

            long projectId = projectWorkflowService.getProjectWorkflow(projectWorkflowDTO.getProjectWorkflowId())
                .getProjectId();

            recordWorkflowArtifact(
                toolContext, false, workflowId, projectId, projectWorkflowDTO.getProjectWorkflowId(),
                extractWorkflowName(definition));

            return new WorkflowInfo(
                projectWorkflowDTO.getId(), projectWorkflowDTO.getProjectWorkflowId(),
                projectWorkflowDTO.getWorkflowUuid(), projectWorkflowDTO.getLabel(),
                projectWorkflowDTO.getDescription(), definition, projectWorkflowDTO.getVersion(),
                projectWorkflowDTO.getCreatedDate(), projectWorkflowDTO.getLastModifiedDate());
        } catch (Exception e) {
            log.error("updateWorkflow({}): Failed to update workflow {}", workflowId, workflowId, e);

            throw new ExecutionException(
                "Failed to update workflow: " + e.getMessage(), e, ProjectWorkflowToolErrorType.UPDATE_WORKFLOW);
        }
    }
```

> `ProjectWorkflowService.getProjectWorkflow(long)` returns the `ProjectWorkflow` domain object whose
> `getProjectId()` supplies the routing metadata the `WorkflowInfo` record does not carry.

5. Add the two private helpers at the end of the class (before the closing brace):

```java
    private void recordWorkflowArtifact(
        @Nullable ToolContext toolContext, boolean created, String workflowId, long projectId,
        @Nullable Long projectWorkflowId, String workflowName) {

        WorkflowArtifactRecorder recorder = workflowArtifactRecorderProvider.getIfAvailable();

        if (recorder == null) {
            return;
        }

        try {
            recorder.recordWorkflowArtifact(toolContext, created, workflowId, projectId, projectWorkflowId,
                workflowName);
        } catch (RuntimeException exception) {
            // Best-effort: artifact recording must never fail the workflow persist, which has already committed.
            log.warn("Failed to record workflow artifact (workflowId={})", workflowId, exception);
        }
    }

    private static String extractWorkflowName(String definition) {
        try {
            String label = JsonUtils.read(definition, "label", String.class);

            return label != null && !label.isBlank() ? label : "Workflow";
        } catch (RuntimeException exception) {
            return "Workflow";
        }
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-tool:test --tests "com.bytechef.automation.ai.tool.ProjectWorkflowToolsTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Format + commit**

```bash
./gradlew :server:libs:automation:automation-ai:automation-ai-tool:spotlessApply
git add server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ProjectWorkflowTools.java server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/ProjectWorkflowToolsTest.java
git commit -m "$(cat <<'EOF'
0_732 Record workflow artifact on create/update in ProjectWorkflowTools

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: EE — dedup-aware `recordWorkflowArtifact` service method

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/task/repository/AiHubTaskArtifactRepository.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-api/src/main/java/com/bytechef/ee/ai/hub/task/AiHubTaskArtifactService.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/task/AiHubTaskArtifactServiceImpl.java`
- Test: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/task/AiHubTaskArtifactServiceTest.java`

- [ ] **Step 1: Add the dedup finder to the repository**

In `AiHubTaskArtifactRepository.java`, add (with `import java.util.Collection;`):

```java
    /**
     * Cross-kind idempotency lookup for workflow artifacts. A single workflow maps to one sidebar row
     * regardless of whether it was created, updated, or merely referenced this turn, so the dedup key is
     * {@code (taskId, artifactId)} spanning all three workflow kinds (WORKFLOW_CREATED / WORKFLOW_UPDATED
     * / WORKFLOW_REFERENCED ordinals).
     */
    Optional<AiHubTaskArtifact> findFirstByTaskIdAndArtifactIdAndKindIn(
        long taskId, String artifactId, Collection<Integer> kinds);
```

- [ ] **Step 2: Add the method to the service interface**

In `AiHubTaskArtifactService.java` (ai-hub-api), add (the package already imports `Map`, `Nullable`):

```java
    /**
     * Records a workflow artifact, idempotent on {@code (taskId, workflowId)} across the three workflow
     * kinds. Resolves the task from {@code threadId + userId}; logs and drops when no task exists or
     * {@code userId} is null. When a workflow row already exists for the task it is refreshed (name +
     * routing metadata) and its existing kind is preserved (a created row is never downgraded to
     * referenced); otherwise a new row is inserted with the given {@code kind}.
     *
     * @param threadId          the AG-UI thread id identifying the active task
     * @param userId            the owner of the task; null is treated as "no binding" and skips recording
     * @param kind              the workflow kind for a fresh insert (WORKFLOW_CREATED / WORKFLOW_UPDATED /
     *                          WORKFLOW_REFERENCED)
     * @param workflowId        the workflow id (artifact id)
     * @param projectId         owning project id; stored in routing metadata as a String
     * @param projectWorkflowId project-workflow id; stored in routing metadata as a Long; may be null
     * @param workflowName      display-name snapshot
     */
    void recordWorkflowArtifact(
        String threadId, @Nullable Long userId, AiHubTaskArtifactKind kind,
        String workflowId, long projectId, @Nullable Long projectWorkflowId, String workflowName);
```

- [ ] **Step 3: Write the failing tests**

Append these tests to `AiHubTaskArtifactServiceTest.java`. They reuse the existing `buildTask`
helper and constants (`USER_ID`, `TASK_ID`, `THREAD_ID`). Add imports as needed
(`com.bytechef.ee.ai.hub.task.AiHubTaskArtifactKind`, `java.util.Collection`, `java.util.List`,
`org.mockito.ArgumentCaptor`, static `org.mockito.ArgumentMatchers.anyString`,
`org.mockito.ArgumentMatchers.anyLong`).

```java
    @Test
    void testRecordWorkflowArtifactInsertsWhenAbsent() {
        AiHubTask task = buildTask(TASK_ID, USER_ID, THREAD_ID);

        when(taskRepository.findByThreadIdAndUserId(THREAD_ID, USER_ID)).thenReturn(Optional.of(task));
        when(taskArtifactRepository.findFirstByTaskIdAndArtifactIdAndKindIn(eq(TASK_ID), eq("wf-1"), any()))
            .thenReturn(Optional.empty());
        when(jsonMapper.writeValueAsString(any())).thenReturn("{}");

        taskArtifactService.recordWorkflowArtifact(
            THREAD_ID, USER_ID, AiHubTaskArtifactKind.WORKFLOW_CREATED, "wf-1", 7L, 55L, "My Flow");

        ArgumentCaptor<AiHubTaskArtifact> captor = ArgumentCaptor.forClass(AiHubTaskArtifact.class);

        verify(taskArtifactRepository).save(captor.capture());

        AiHubTaskArtifact saved = captor.getValue();

        assertThat(saved.getKind()).isEqualTo(AiHubTaskArtifactKind.WORKFLOW_CREATED);
        assertThat(saved.getArtifactId()).isEqualTo("wf-1");
        assertThat(saved.getArtifactName()).isEqualTo("My Flow");
        assertThat(saved.getTaskId()).isEqualTo(TASK_ID);
    }

    @Test
    void testRecordWorkflowArtifactDedupsAndKeepsExistingKind() {
        AiHubTask task = buildTask(TASK_ID, USER_ID, THREAD_ID);

        AiHubTaskArtifact existing = new AiHubTaskArtifact();

        existing.setId(500L);
        existing.setTaskId(TASK_ID);
        existing.setKind(AiHubTaskArtifactKind.WORKFLOW_CREATED);
        existing.setArtifactId("wf-1");
        existing.setArtifactName("Old Name");

        when(taskRepository.findByThreadIdAndUserId(THREAD_ID, USER_ID)).thenReturn(Optional.of(task));
        when(taskArtifactRepository.findFirstByTaskIdAndArtifactIdAndKindIn(eq(TASK_ID), eq("wf-1"), any()))
            .thenReturn(Optional.of(existing));
        when(jsonMapper.writeValueAsString(any())).thenReturn("{}");

        taskArtifactService.recordWorkflowArtifact(
            THREAD_ID, USER_ID, AiHubTaskArtifactKind.WORKFLOW_REFERENCED, "wf-1", 7L, 55L, "New Name");

        ArgumentCaptor<AiHubTaskArtifact> captor = ArgumentCaptor.forClass(AiHubTaskArtifact.class);

        verify(taskArtifactRepository).save(captor.capture());

        AiHubTaskArtifact saved = captor.getValue();

        assertThat(saved.getId()).isEqualTo(500L);
        assertThat(saved.getKind()).isEqualTo(AiHubTaskArtifactKind.WORKFLOW_CREATED);
        assertThat(saved.getArtifactName()).isEqualTo("New Name");
    }

    @Test
    void testRecordWorkflowArtifactDropsWhenTaskMissing() {
        when(taskRepository.findByThreadIdAndUserId(THREAD_ID, USER_ID)).thenReturn(Optional.empty());

        taskArtifactService.recordWorkflowArtifact(
            THREAD_ID, USER_ID, AiHubTaskArtifactKind.WORKFLOW_CREATED, "wf-1", 7L, 55L, "My Flow");

        verify(taskArtifactRepository, never()).save(any());
    }

    @Test
    void testRecordWorkflowArtifactSkipsWhenUserIdNull() {
        taskArtifactService.recordWorkflowArtifact(
            THREAD_ID, null, AiHubTaskArtifactKind.WORKFLOW_CREATED, "wf-1", 7L, 55L, "My Flow");

        verify(taskRepository, never()).findByThreadIdAndUserId(anyString(), anyLong());
        verify(taskArtifactRepository, never()).save(any());
    }
```

> Confirm `buildTask(long, long, String)` and the `AiHubTaskArtifact` setters (`setId`, `setTaskId`,
> `setKind`, `setArtifactId`, `setArtifactName`) used above exist in this test file / domain (they are
> used by the existing tests and the `ARTIFACT_ROW_MAPPER`). If `buildTask`'s signature differs, match
> the existing helper.

- [ ] **Step 4: Run tests to verify they fail**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests "com.bytechef.ee.ai.hub.task.AiHubTaskArtifactServiceTest"`
Expected: FAIL to compile — `recordWorkflowArtifact` and `findFirstByTaskIdAndArtifactIdAndKindIn` do not exist yet.

- [ ] **Step 5: Implement the service method**

In `AiHubTaskArtifactServiceImpl.java`, add a constant near the top of the class body:

```java
    private static final List<Integer> WORKFLOW_KIND_ORDINALS = List.of(
        AiHubTaskArtifactKind.WORKFLOW_CREATED.ordinal(),
        AiHubTaskArtifactKind.WORKFLOW_UPDATED.ordinal(),
        AiHubTaskArtifactKind.WORKFLOW_REFERENCED.ordinal());
```

Add the method (anywhere among the other `@Override` methods, e.g. after `record`):

```java
    @Override
    public void recordWorkflowArtifact(
        String threadId, @Nullable Long userId, AiHubTaskArtifactKind kind,
        String workflowId, long projectId, @Nullable Long projectWorkflowId, String workflowName) {

        if (userId == null) {
            log.warn(
                "recordWorkflowArtifact called without a bound userId — skipping (threadId={}, workflowId={}). "
                    + "The persist has already committed; the user will not see an artifact for it.",
                threadId, workflowId);

            return;
        }

        Optional<AiHubTask> taskOptional = taskRepository.findByThreadIdAndUserId(threadId, userId);

        if (taskOptional.isEmpty()) {
            log.warn(
                "No task found for threadId={} userId={} — DROPPING workflow artifact (workflowId={}, name={}).",
                threadId, userId, workflowId, workflowName);

            return;
        }

        AiHubTask task = taskOptional.get();

        // Match the metadata shape of the legacy openWorkflowTab rows so the client sidebar quick-open keeps
        // working: projectId as a String, projectWorkflowId as a Long.
        Map<String, Object> metadata = new HashMap<>();

        metadata.put("projectId", String.valueOf(projectId));

        if (projectWorkflowId != null) {
            metadata.put("projectWorkflowId", projectWorkflowId);
        }

        Optional<AiHubTaskArtifact> existingOptional =
            taskArtifactRepository.findFirstByTaskIdAndArtifactIdAndKindIn(
                task.getId(), workflowId, WORKFLOW_KIND_ORDINALS);

        AiHubTaskArtifact artifact;

        if (existingOptional.isPresent()) {
            // One workflow → one row. Refresh the display name + routing metadata but preserve the existing
            // kind: a WORKFLOW_CREATED row must not be downgraded to WORKFLOW_REFERENCED by a later open.
            artifact = existingOptional.get();

            artifact.setArtifactName(workflowName);
            artifact.setMetadataJson(serializeMetadata(metadata, artifact.getKind(), workflowId));
        } else {
            artifact = new AiHubTaskArtifact();

            artifact.setTaskId(task.getId());
            artifact.setKind(kind);
            artifact.setArtifactId(workflowId);
            artifact.setArtifactName(workflowName);
            artifact.setEnvironment(task.getEnvironment());
            artifact.setCreatedAt(LocalDateTime.now(clock));
            artifact.setMetadataJson(serializeMetadata(metadata, kind, workflowId));
            artifact.setStatus(AiHubTaskArtifactStatus.APPLIED);
        }

        taskArtifactRepository.save(artifact);
    }
```

> `HashMap` is already imported via the existing code? Check the import block; if not, add
> `import java.util.HashMap;`. `List` is already imported. `serializeMetadata`,
> `AiHubTaskArtifactStatus`, `LocalDateTime`, `clock` are all already present in this file.

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests "com.bytechef.ee.ai.hub.task.AiHubTaskArtifactServiceTest"`
Expected: PASS (existing tests + 4 new).

- [ ] **Step 7: Format + commit**

```bash
./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:spotlessApply
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/task/repository/AiHubTaskArtifactRepository.java server/ee/libs/ai/ai-hub/ai-hub-api/src/main/java/com/bytechef/ee/ai/hub/task/AiHubTaskArtifactService.java server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/task/AiHubTaskArtifactServiceImpl.java server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/task/AiHubTaskArtifactServiceTest.java
git commit -m "$(cat <<'EOF'
0_732 Add dedup-aware recordWorkflowArtifact to AiHubTaskArtifactService

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: EE — `WorkflowArtifactRecorderImpl`

**Files:**
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/task/WorkflowArtifactRecorderImpl.java`
- Test: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/task/WorkflowArtifactRecorderImplTest.java`

- [ ] **Step 1: Write the failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.ee.ai.copilot.tool.AgentToolInvocationContext;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

/**
 * Unit tests for {@link WorkflowArtifactRecorderImpl}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkflowArtifactRecorderImplTest {

    @Mock
    private AiHubTaskArtifactService taskArtifactService;

    @Test
    void testDelegatesWhenConversationIdPresent() {
        WorkflowArtifactRecorderImpl recorder = new WorkflowArtifactRecorderImpl(taskArtifactService);

        ToolContext toolContext = new ToolContext(
            new AgentToolInvocationContext(7L, 42L, 0L, "thread-1").toToolContext());

        recorder.recordWorkflowArtifact(toolContext, true, "wf-1", 9L, 55L, "My Flow");

        verify(taskArtifactService).recordWorkflowArtifact(
            eq("thread-1"), eq(42L), eq(AiHubTaskArtifactKind.WORKFLOW_CREATED), eq("wf-1"), eq(9L), eq(55L),
            eq("My Flow"));
    }

    @Test
    void testMapsUpdateToWorkflowUpdatedKind() {
        WorkflowArtifactRecorderImpl recorder = new WorkflowArtifactRecorderImpl(taskArtifactService);

        ToolContext toolContext = new ToolContext(
            new AgentToolInvocationContext(7L, 42L, 0L, "thread-1").toToolContext());

        recorder.recordWorkflowArtifact(toolContext, false, "wf-1", 9L, 55L, "My Flow");

        verify(taskArtifactService).recordWorkflowArtifact(
            eq("thread-1"), eq(42L), eq(AiHubTaskArtifactKind.WORKFLOW_UPDATED), eq("wf-1"), eq(9L), eq(55L),
            eq("My Flow"));
    }

    @Test
    void testNoOpWhenNoConversationId() {
        WorkflowArtifactRecorderImpl recorder = new WorkflowArtifactRecorderImpl(taskArtifactService);

        // workspace/user present, but no conversation id → not an AI Hub task turn.
        ToolContext toolContext = new ToolContext(
            new AgentToolInvocationContext(7L, 42L, 0L, null).toToolContext());

        recorder.recordWorkflowArtifact(toolContext, true, "wf-1", 9L, 55L, "My Flow");

        verify(taskArtifactService, never()).recordWorkflowArtifact(
            any(), any(), any(), any(), anyLong(), any(), any());
    }

    @Test
    void testNoOpWhenToolContextNull() {
        WorkflowArtifactRecorderImpl recorder = new WorkflowArtifactRecorderImpl(taskArtifactService);

        recorder.recordWorkflowArtifact(null, true, "wf-1", 9L, 55L, "My Flow");

        verify(taskArtifactService, never()).recordWorkflowArtifact(
            any(), any(), any(), any(), anyLong(), any(), any());
    }

    @Test
    void testSwallowsServiceException() {
        WorkflowArtifactRecorderImpl recorder = new WorkflowArtifactRecorderImpl(taskArtifactService);

        ToolContext toolContext = new ToolContext(
            new AgentToolInvocationContext(7L, 42L, 0L, "thread-1").toToolContext());

        doThrow(new RuntimeException("boom"))
            .when(taskArtifactService)
            .recordWorkflowArtifact(any(), any(), any(), any(), anyLong(), any(), any());

        assertThatCode(() -> recorder.recordWorkflowArtifact(toolContext, true, "wf-1", 9L, 55L, "My Flow"))
            .doesNotThrowAnyException();
    }
}
```

> Confirm the `ToolContext` constructor `new ToolContext(Map<String, Object>)` exists in the Spring AI
> version on the classpath (it does in current Spring AI — `ToolContext` wraps a `Map`). If the test
> needs a non-empty-map guard, `AgentToolInvocationContext.toToolContext()` already returns a populated
> map for these inputs.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests "com.bytechef.ee.ai.hub.task.WorkflowArtifactRecorderImplTest"`
Expected: FAIL to compile — `WorkflowArtifactRecorderImpl` does not exist.

- [ ] **Step 3: Implement the class**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

import com.bytechef.automation.ai.tool.WorkflowArtifactRecorder;
import com.bytechef.ee.ai.copilot.tool.AgentToolInvocationContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Enterprise implementation of the community {@link WorkflowArtifactRecorder} SPI. Extracts the AI Hub
 * conversation context from the forwarded tool context and records the persisted workflow as a task
 * artifact. A no-op when there is no conversation id (the persist happened outside an AI Hub task —
 * Copilot in-editor panel, embedded autonomous generation, or the plain editor).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class WorkflowArtifactRecorderImpl implements WorkflowArtifactRecorder {

    private static final Logger log = LoggerFactory.getLogger(WorkflowArtifactRecorderImpl.class);

    private final AiHubTaskArtifactService taskArtifactService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkflowArtifactRecorderImpl(AiHubTaskArtifactService taskArtifactService) {
        this.taskArtifactService = taskArtifactService;
    }

    @Override
    public void recordWorkflowArtifact(
        @Nullable ToolContext toolContext, boolean created, String workflowId, long projectId,
        @Nullable Long projectWorkflowId, String workflowName) {

        AgentToolInvocationContext invocationContext = AgentToolInvocationContext.fromToolContext(toolContext);

        if (invocationContext == null || invocationContext.conversationId() == null) {
            return;
        }

        AiHubTaskArtifactKind kind = created
            ? AiHubTaskArtifactKind.WORKFLOW_CREATED : AiHubTaskArtifactKind.WORKFLOW_UPDATED;

        try {
            taskArtifactService.recordWorkflowArtifact(
                invocationContext.conversationId(), invocationContext.userId(), kind, workflowId, projectId,
                projectWorkflowId, workflowName);
        } catch (RuntimeException exception) {
            // Best-effort: the workflow persist has already committed; never propagate.
            log.warn("Failed to record workflow artifact (workflowId={})", workflowId, exception);
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests "com.bytechef.ee.ai.hub.task.WorkflowArtifactRecorderImplTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Format + commit**

```bash
./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:spotlessApply
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/task/WorkflowArtifactRecorderImpl.java server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/task/WorkflowArtifactRecorderImplTest.java
git commit -m "$(cat <<'EOF'
0_732 Add EE WorkflowArtifactRecorderImpl wiring persist to task artifact

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: EE — converge `openWorkflowTab` through the dedup path

**Why:** Without this, a normal AI Hub build records `WORKFLOW_CREATED` (server-side, Task 2–4) AND
`WORKFLOW_REFERENCED` (openWorkflowTab via the old non-dedup `record`) — two rows for one workflow.
Routing openWorkflowTab through `recordWorkflowArtifact` makes them converge on one row.

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/AiHubTaskArtifactRecorder.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/task/AiHubTaskArtifactRecorderImpl.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/OpenWorkflowTabToolCallback.java`
- Test: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/OpenWorkflowTabToolCallbackTest.java`

- [ ] **Step 1: Add the reference method to the recorder SPI**

In `AiHubTaskArtifactRecorder.java` (`com.bytechef.ee.ai.hub.tool`), add:

```java
    /**
     * Records (or de-duplicates onto) a workflow artifact for a workflow the agent opened/referenced.
     * Routes through the dedup-aware {@code recordWorkflowArtifact} so a referenced workflow collapses
     * onto an existing created/updated row for the same {@code (task, workflowId)} rather than producing
     * a second sidebar entry.
     *
     * @param threadId          the AG-UI thread id identifying the active task
     * @param userId            the owner of the task (may be {@code null} — treated as no binding)
     * @param workflowId        the workflow id (artifact id)
     * @param projectId         owning project id (routing metadata)
     * @param projectWorkflowId project-workflow id (routing metadata), may be {@code null}
     * @param workflowName      display-name snapshot
     */
    void recordWorkflowReference(
        String threadId, @Nullable Long userId, String workflowId, long projectId,
        @Nullable Long projectWorkflowId, String workflowName);
```

- [ ] **Step 2: Implement it in `AiHubTaskArtifactRecorderImpl`**

In `AiHubTaskArtifactRecorderImpl.java` (`com.bytechef.ee.ai.hub.task`), add the method (the class
already holds `taskArtifactService`; add `import com.bytechef.ee.ai.hub.task.AiHubTaskArtifactKind;`
only if not already resolvable — it is in the same package, so no import needed):

```java
    @Override
    public void recordWorkflowReference(
        String threadId, @Nullable Long userId, String workflowId, long projectId,
        @Nullable Long projectWorkflowId, String workflowName) {

        taskArtifactService.recordWorkflowArtifact(
            threadId, userId, AiHubTaskArtifactKind.WORKFLOW_REFERENCED, workflowId, projectId, projectWorkflowId,
            workflowName);
    }
```

- [ ] **Step 3: Write the failing test for the rerouted callback**

Open `OpenWorkflowTabToolCallbackTest.java`. Find the existing test that asserts the artifact is
recorded (it verifies `artifactRecorder.record(...)` with `"WORKFLOW_REFERENCED"`). Replace that
verification with one asserting the new `recordWorkflowReference(...)` is called. Add a fresh test if
none exists. The callback's `OpenWorkflowTabInput.projectId()` is a String, so the expected `long`
projectId is the parsed value.

```java
    @Test
    void testCallRecordsWorkflowReferenceViaDedupPath() {
        AiHubTaskArtifactRecorder artifactRecorder = mock(AiHubTaskArtifactRecorder.class);

        OpenWorkflowTabToolCallback callback = new OpenWorkflowTabToolCallback(artifactRecorder);

        ToolContext toolContext = new ToolContext(
            AiHubToolInvocationContext.toToolContext("thread-9", 42L));

        String input = """
            {"workflowId": "wf-1", "projectId": "9", "projectWorkflowId": 55, "name": "My Flow"}""";

        callback.call(input, toolContext);

        verify(artifactRecorder).recordWorkflowReference("thread-9", 42L, "wf-1", 9L, 55L, "My Flow");
    }
```

> The exact construction of an `AiHubToolInvocationContext`-bearing `ToolContext` must match how the
> existing tests in this file build one (the test already exercises `recordArtifact`, so copy its
> setup — it shows how `threadId`/`userId` are placed into the context). Use the existing test's helper
> rather than the illustrative `AiHubToolInvocationContext.toToolContext(...)` call above if the real
> API differs. Likewise reuse the file's existing `mock`/import style.

- [ ] **Step 4: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests "com.bytechef.ee.ai.hub.tool.OpenWorkflowTabToolCallbackTest"`
Expected: FAIL — `recordWorkflowReference` not called yet (still calls old `record`).

- [ ] **Step 5: Reroute the callback**

In `OpenWorkflowTabToolCallback.recordArtifact`, replace the `artifactRecorder.record(...)` call with
the new reference recorder. Parse the String `projectId` to a long; if it is not numeric, fall back
to recording with projectId `0` is undesirable — instead keep the old behavior for that edge by
skipping the long-typed metadata. Concretely:

```java
    private void recordArtifact(@Nullable ToolContext toolContext, OpenWorkflowTabInput input) {
        if (artifactRecorder == null) {
            return;
        }

        AiHubToolInvocationContext invocationContext = AiHubToolInvocationContext.fromToolContext(toolContext);

        if (invocationContext == null || invocationContext.threadId() == null) {
            return;
        }

        long projectId;

        try {
            projectId = Long.parseLong(input.projectId());
        } catch (NumberFormatException exception) {
            log.warn(
                "openWorkflowTab projectId '{}' is not numeric — skipping artifact record (workflowId={})",
                input.projectId(), input.workflowId());

            return;
        }

        try {
            artifactRecorder.recordWorkflowReference(
                invocationContext.threadId(), invocationContext.userId(), input.workflowId(), projectId,
                input.projectWorkflowId(), input.name());
        } catch (RuntimeException exception) {
            // Best-effort: the open-tab signal must still succeed even if artifact persistence fails.
            log.warn(
                "Failed to record workflow artifact for openWorkflowTab (workflowId={})", input.workflowId(),
                exception);
        }
    }
```

> Remove the now-unused `import java.util.Map;` from `OpenWorkflowTabToolCallback` if nothing else in
> the file uses it (the old `Map.of(...)` metadata is gone). Leave it if still referenced.

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests "com.bytechef.ee.ai.hub.tool.OpenWorkflowTabToolCallbackTest"`
Expected: PASS.

- [ ] **Step 7: Format + commit**

```bash
./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:spotlessApply
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/AiHubTaskArtifactRecorder.java server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/task/AiHubTaskArtifactRecorderImpl.java server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/OpenWorkflowTabToolCallback.java server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/OpenWorkflowTabToolCallbackTest.java
git commit -m "$(cat <<'EOF'
0_732 Converge openWorkflowTab artifact recording onto dedup path

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: Full verification

**Files:** none (verification only).

- [ ] **Step 1: Build + test both modules**

Run:
```bash
./gradlew :server:libs:automation:automation-ai:automation-ai-tool:check :server:ee:libs:ai:ai-hub:ai-hub-service:check
```
Expected: BUILD SUCCESSFUL — Spotless, Checkstyle, PMD, SpotBugs, and all tests pass for both
modules.

- [ ] **Step 2: Verify the AI Hub agent's other tool wiring still compiles against the changed callback**

The `OpenWorkflowTabToolCallback` and `ProjectWorkflowTools` are wired in `AiHubConfiguration` /
component scan. Confirm the broader server app compiles:

Run: `./gradlew :server:apps:server-app:compileJava`
Expected: BUILD SUCCESSFUL. If `ProjectWorkflowTools` is constructed manually anywhere (grep
`new ProjectWorkflowTools(`), update those call sites to the 3-arg constructor.

Run: `grep -rn "new ProjectWorkflowTools(" server --include=*.java`
Expected: only the (now-updated) test. Fix any production construction site to pass
`projectWorkflowService` + an `ObjectProvider<WorkflowArtifactRecorder>` (for `@Bean` methods,
inject both; `ObjectProvider` is resolvable from the bean method parameters).

- [ ] **Step 3: Manual smoke (optional, if a dev stack is running)**

Build a workflow through AI Hub chat ("create a workflow that …"), then confirm exactly one workflow
artifact row appears in the task sidebar and it opens the workflow. Then ask the agent to modify it
and confirm the SAME single row remains (name refreshed, not duplicated).

---

## Self-Review (completed by plan author)

**Spec coverage:**
- "Record artifact server-side on persist" → Tasks 1, 2, 4. ✓
- "Neutral CE SPI moved under CE" → Task 1 (`WorkflowArtifactRecorder` in `automation-ai-tool`). ✓
- "EE impl extracts conversationId via AgentToolInvocationContext, no-op when absent" → Task 4. ✓
- "Idempotent on (taskId, workflowId) across workflow kinds" → Task 3. ✓
- "openWorkflowTab converges, no duplicate row" → Task 5. ✓
- "No-op surfaces (Copilot panel, autonomous, plain editor)" → Task 4 (null conversationId / absent
  bean) + Task 2 (absent provider). ✓
- "Metadata type compatibility (projectId String, projectWorkflowId Long)" → Task 3 Step 5. ✓
- "EE conventions / CE Apache header" → headers specified per file. ✓

**Placeholder scan:** No "TBD"/"handle errors"-style gaps; every code step shows full code. The
"confirm this setter/helper exists" notes are verification guards against domain-API drift, not
missing content — each gives the fallback.

**Type consistency:** `recordWorkflowArtifact(String, Long, AiHubTaskArtifactKind, String, long,
Long, String)` is identical across the service interface (Task 3), impl (Task 3), and both callers
(Task 4 impl, Task 5 impl). The CE SPI `recordWorkflowArtifact(ToolContext, boolean, String, long,
Long, String)` is consistent between Task 1, Task 2 caller, and Task 4 impl.
`findFirstByTaskIdAndArtifactIdAndKindIn(long, String, Collection<Integer>)` matches between Task 3
repo + service impl. `recordWorkflowReference(String, Long, String, long, Long, String)` matches
Task 5 SPI + impl + caller.
