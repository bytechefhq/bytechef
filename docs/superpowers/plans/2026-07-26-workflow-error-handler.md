# Workflow Error Handler Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When an automation workflow run ends `FAILED`, dispatch a designated workflow that starts with an Error Trigger and receives the failed run's details.

**Architecture:** Two nullable columns on `project` / `project_workflow` hold the handler reference. A new `workflow/newWorkflowError` trigger marks a workflow as a handler. A new coordinator listener on `JobStatusApplicationEvent` resolves the handler and submits it through `PrincipalJobFacade.createJob`, guarded against recursion by an `errorHandlerFor` job-metadata key.

**Tech Stack:** Java 25, Spring Boot 4.0.7, Spring Data JDBC, Liquibase, JUnit 5, Mockito, Testcontainers.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-07-26-workflow-error-handler-design.md`. Read it before Task 1.
- Automation only. Embedded is out of scope — do not add integration-side config.
- Handler target is a `projectWorkflowId` **in the same project**. Cross-project is out of scope.
- Fires on `Job.Status.FAILED` only. Never on `CANCELLED` or `STOPPED`.
- Payload carries **no task inputs or outputs**. Only the fields listed in Task 5.
- Recursion cap is depth 1, via job metadata key `errorHandlerFor`.
- Admission gates are **not** bypassed — always go through `PrincipalJobFacade.createJob`.
- All dispatch code is fail-open: log a warning, never rethrow, never alter the failed job.
- CE code (`server/libs/`) uses the Apache header. No `@version ee` tag; that header is selected by content and would be wrong here.
- Run `./gradlew spotlessApply` before every commit. Check Gradle results by redirecting to a file and testing `$?` — a run piped into `tail` reports the filter's exit code, not Gradle's.

---

## File Structure

**Created:**
- `.../automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/20260726120000_automation_configuration_added_error_workflow_columns.xml` — schema
- `.../components/workflow/src/main/java/com/bytechef/component/workflow/trigger/WorkflowNewWorkflowErrorTrigger.java` — the Error Trigger definition
- `.../automation-configuration-api/.../domain/ErrorWorkflowDispatch.java` — resolved handler + failed-run identity
- `.../automation-configuration-service/.../facade/ErrorWorkflowConfigurationValidator.java` — config-time validation
- `.../automation-configuration-service/.../service/ErrorWorkflowResolver.java` — override → project → none resolution
- `.../platform-coordinator/.../event/listener/ErrorWorkflowJobStatusApplicationEventListener.java` — dispatch
- `.../platform-coordinator/.../event/listener/ErrorWorkflowPayloadFactory.java` — payload assembly
- `.../platform-coordinator/.../ErrorWorkflowDispatchCounter.java` — metric

**Modified:**
- `.../automation-configuration-api/.../domain/Project.java` — add `errorProjectWorkflowId`
- `.../automation-configuration-api/.../domain/ProjectWorkflow.java` — add `errorProjectWorkflowId`, `errorWorkflowDisabled`
- `.../components/workflow/.../WorkflowComponentHandler.java` — register the trigger
- `.../platform-coordinator/.../config/PlatformCoordinatorConfiguration.java` — register the listener bean
- `server/libs/platform/platform-coordinator/build.gradle.kts` — add `platform-workflow-execution-api`, `automation-configuration-api`

---

### Task 1: Schema and domain columns

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/20260726120000_automation_configuration_added_error_workflow_columns.xml`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/domain/Project.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/domain/ProjectWorkflow.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/repository/ProjectErrorWorkflowColumnsIntTest.java`

**Interfaces:**
- Produces: `Project.getErrorProjectWorkflowId(): Long` / `setErrorProjectWorkflowId(Long)`; `ProjectWorkflow.getErrorProjectWorkflowId(): Long` / `setErrorProjectWorkflowId(Long)`; `ProjectWorkflow.isErrorWorkflowDisabled(): boolean` / `setErrorWorkflowDisabled(boolean)`

- [ ] **Step 1: Write the failing test**

```java
@SpringBootTest
@ActiveProfiles("testint")
class ProjectErrorWorkflowColumnsIntTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void testErrorProjectWorkflowIdRoundTrips() {
        Project project = new Project();

        project.setName("test-project");
        project.setErrorProjectWorkflowId(42L);

        Project saved = projectRepository.save(project);

        Project reloaded = projectRepository.findById(saved.getId())
            .orElseThrow();

        Assertions.assertEquals(42L, reloaded.getErrorProjectWorkflowId());
    }

    @Test
    void testErrorProjectWorkflowIdDefaultsToNull() {
        Project project = new Project();

        project.setName("test-project-2");

        Project saved = projectRepository.save(project);

        Assertions.assertNull(saved.getErrorProjectWorkflowId());
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:testIntegration --tests "*ProjectErrorWorkflowColumnsIntTest*" > /tmp/t1.log 2>&1; echo $?`
Expected: non-zero. `/tmp/t1.log` contains `cannot find symbol: method setErrorProjectWorkflowId`.

- [ ] **Step 3: Add the Liquibase changelog**

```xml
<?xml version="1.0" encoding="utf-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <!-- Error-workflow handler reference. Nullable on both tables: null on project means no project-wide
         handler, null on project_workflow means inherit the project's. The separate disabled flag exists
         because null already means inherit, so it cannot also mean "opt out". -->
    <changeSet id="20260726120000-1" author="Ivica Cardic">
        <preConditions onFail="MARK_RAN">
            <not>
                <columnExists tableName="project" columnName="error_project_workflow_id"/>
            </not>
        </preConditions>

        <addColumn tableName="project">
            <column name="error_project_workflow_id" type="BIGINT"/>
        </addColumn>
    </changeSet>

    <changeSet id="20260726120000-2" author="Ivica Cardic">
        <preConditions onFail="MARK_RAN">
            <not>
                <columnExists tableName="project_workflow" columnName="error_project_workflow_id"/>
            </not>
        </preConditions>

        <addColumn tableName="project_workflow">
            <column name="error_project_workflow_id" type="BIGINT"/>
            <column name="error_workflow_disabled" type="BOOLEAN" defaultValueBoolean="false">
                <constraints nullable="false"/>
            </column>
        </addColumn>
    </changeSet>
</databaseChangeLog>
```

- [ ] **Step 4: Add the Project field**

In `Project.java`, after the `description` field:

```java
    @Column("error_project_workflow_id")
    private Long errorProjectWorkflowId;
```

and with the other accessors:

```java
    @Nullable
    public Long getErrorProjectWorkflowId() {
        return errorProjectWorkflowId;
    }

    public void setErrorProjectWorkflowId(@Nullable Long errorProjectWorkflowId) {
        this.errorProjectWorkflowId = errorProjectWorkflowId;
    }
```

- [ ] **Step 5: Add the ProjectWorkflow fields**

In `ProjectWorkflow.java`, after the `workflowId` field:

```java
    @Column("error_project_workflow_id")
    private Long errorProjectWorkflowId;

    @Column("error_workflow_disabled")
    private boolean errorWorkflowDisabled;
```

and:

```java
    @Nullable
    public Long getErrorProjectWorkflowId() {
        return errorProjectWorkflowId;
    }

    public void setErrorProjectWorkflowId(@Nullable Long errorProjectWorkflowId) {
        this.errorProjectWorkflowId = errorProjectWorkflowId;
    }

    public boolean isErrorWorkflowDisabled() {
        return errorWorkflowDisabled;
    }

    public void setErrorWorkflowDisabled(boolean errorWorkflowDisabled) {
        this.errorWorkflowDisabled = errorWorkflowDisabled;
    }
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:testIntegration --tests "*ProjectErrorWorkflowColumnsIntTest*" > /tmp/t1.log 2>&1; echo $?`
Expected: `0`, and `grep -c "FAILED" /tmp/t1.log` is `0`.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-configuration
git commit -m "Add error-workflow handler columns to project and project_workflow

Nullable on both: null on project means no project-wide handler, null on
project_workflow means inherit. The separate disabled flag exists because null
already means inherit and so cannot also mean opt out."
```

---

### Task 2: The Error Trigger

**Files:**
- Create: `server/libs/modules/components/workflow/src/main/java/com/bytechef/component/workflow/trigger/WorkflowNewWorkflowErrorTrigger.java`
- Modify: `server/libs/modules/components/workflow/src/main/java/com/bytechef/component/workflow/WorkflowComponentHandler.java`
- Modify: `server/libs/modules/components/workflow/src/main/java/com/bytechef/component/workflow/constant/WorkflowConstants.java`
- Test: `server/libs/modules/components/workflow/src/test/java/com/bytechef/component/workflow/WorkflowComponentHandlerTest.java` (existing snapshot test)

**Interfaces:**
- Consumes: nothing from Task 1.
- Produces: constant `WorkflowConstants.NEW_WORKFLOW_ERROR = "newWorkflowError"`; `WorkflowNewWorkflowErrorTrigger.TRIGGER_DEFINITION`. Task 3 checks a workflow contains this trigger name.

- [ ] **Step 1: Add the constant**

In `WorkflowConstants.java`, beside `NEW_WORKFLOW_CALL`:

```java
    public static final String NEW_WORKFLOW_ERROR = "newWorkflowError";
```

- [ ] **Step 2: Write the trigger definition**

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

package com.bytechef.component.workflow.trigger;

import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.workflow.constant.WorkflowConstants.NEW_WORKFLOW_ERROR;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;

/**
 * Marks a workflow as an error handler. A workflow carrying this trigger can be selected as the error workflow for a
 * project or a single workflow; it is started by the coordinator when a run fails, not over HTTP.
 *
 * @author Ivica Cardic
 */
public class WorkflowNewWorkflowErrorTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger(NEW_WORKFLOW_ERROR)
        .title("New Workflow Error")
        .description(
            "Triggers when a workflow run fails. Set this workflow as the error workflow of a project or of a " +
                "single workflow to receive its failures.")
        .type(TriggerType.STATIC_WEBHOOK)
        .output(
            outputSchema(
                object()
                    .properties(
                        object("execution")
                            .properties(
                                string("jobId"),
                                string("url"),
                                object("error")
                                    .properties(string("message"), string("stackTrace")),
                                string("lastTaskExecuted"),
                                string("mode"),
                                string("resumeOf")),
                        object("workflow")
                            .properties(
                                string("projectId"),
                                string("projectWorkflowId"),
                                string("workflowId"),
                                string("label")),
                        string("environment"))));
}
```

- [ ] **Step 3: Register it on the component**

In `WorkflowComponentHandler.java`, change the `.triggers(...)` line to:

```java
            .triggers(
                WorkflowNewWorkflowCallTrigger.TRIGGER_DEFINITION,
                WorkflowNewWorkflowErrorTrigger.TRIGGER_DEFINITION)
```

and add the import `com.bytechef.component.workflow.trigger.WorkflowNewWorkflowErrorTrigger`.

- [ ] **Step 4: Delete both stale snapshot copies**

The definition test compares against a JSON snapshot, and the build-output copy shadows the source one. Deleting only one makes the test pass against stale data.

```bash
rm -f server/libs/modules/components/workflow/src/test/resources/definition/workflow_v1.json
rm -f server/libs/modules/components/workflow/build/resources/test/definition/workflow_v1.json
```

- [ ] **Step 5: Regenerate and verify**

Run: `./gradlew :server:libs:modules:components:workflow:test > /tmp/t2.log 2>&1; echo $?`
Expected: `0`. The snapshot is regenerated. Confirm it contains the new trigger:

```bash
grep -c newWorkflowError server/libs/modules/components/workflow/src/test/resources/definition/workflow_v1.json
```
Expected: at least `1`.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/libs/modules/components/workflow
git commit -m "Add the workflow/newWorkflowError Error Trigger

Marks a workflow as an error handler and declares the payload schema so the
editor renders data pills. The run is started by the coordinator through
PrincipalJobFacade, not over HTTP, so the trigger is a schema and a marker."
```

---

### Task 3: Handler resolution

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/domain/ErrorWorkflowDispatch.java`
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/ErrorWorkflowResolver.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/service/ErrorWorkflowResolverTest.java`

**Interfaces:**
- Consumes: `Project.getErrorProjectWorkflowId()`, `ProjectWorkflow.getErrorProjectWorkflowId()`, `ProjectWorkflow.isErrorWorkflowDisabled()` from Task 1.
- Produces: `ErrorWorkflowResolver.resolve(long jobPrincipalId, String failedWorkflowId): Optional<ErrorWorkflowDispatch>`; record `ErrorWorkflowDispatch(String handlerWorkflowId, long projectId, long failedProjectWorkflowId, String failedWorkflowId, String failedWorkflowLabel)`. Task 6 calls `resolve` and nothing else from automation-configuration.

**Why the resolver takes `jobPrincipalId`, not `projectId`:** `PrincipalJobService.fetchJobPrincipalId` returns the **project deployment** id, not the project id. The existing `WorkflowAlertApplicationEventListener` maps it with `projectDeploymentService::getProjectDeployment` then `ProjectDeployment::getProjectId`. Doing that mapping here keeps every project lookup inside automation-configuration, so platform-coordinator needs no `ProjectDeploymentService` or `WorkflowService` dependency.

**Why the record carries `failed*` fields:** the payload's `workflow` block describes the run that failed, not the handler — same as n8n, where `workflow.id`/`name` identify the failed workflow. Only `handlerWorkflowId` names the workflow to submit.

- [ ] **Step 1: Write the failing test**

```java
@ExtendWith(MockitoExtension.class)
class ErrorWorkflowResolverTest {

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private ErrorWorkflowResolver errorWorkflowResolver;

    @BeforeEach
    void setUp() {
        // resolve() takes the job principal id, which for automation is the project deployment id.
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setProjectId(1L);

        Mockito.lenient()
            .when(projectDeploymentService.getProjectDeployment(1L))
            .thenReturn(projectDeployment);

        Workflow failedWorkflow = new Workflow();

        failedWorkflow.setLabel("Failing Workflow");

        Mockito.lenient()
            .when(workflowService.getWorkflow("wf-1"))
            .thenReturn(failedWorkflow);
    }

    @Test
    void testWorkflowOverrideWins() {
        ProjectWorkflow failing = projectWorkflow(10L, 99L, false);
        Project project = project(7L);

        Mockito.when(projectWorkflowService.getProjectWorkflow(1L, "wf-1"))
            .thenReturn(failing);
        Mockito.when(projectWorkflowService.getProjectWorkflow(99L))
            .thenReturn(projectWorkflow(99L, null, false));

        Optional<ErrorWorkflowDispatch> result = errorWorkflowResolver.resolve(1L, "wf-1");

        Assertions.assertEquals("wf-99", result.orElseThrow().handlerWorkflowId());
        Mockito.verifyNoInteractions(projectService);
    }

    @Test
    void testFallsBackToProjectDefault() {
        Mockito.when(projectWorkflowService.getProjectWorkflow(1L, "wf-1"))
            .thenReturn(projectWorkflow(10L, null, false));
        Mockito.when(projectService.getProject(1L))
            .thenReturn(project(7L));
        Mockito.when(projectWorkflowService.getProjectWorkflow(7L))
            .thenReturn(projectWorkflow(7L, null, false));

        Optional<ErrorWorkflowDispatch> result = errorWorkflowResolver.resolve(1L, "wf-1");

        Assertions.assertEquals("wf-7", result.orElseThrow().handlerWorkflowId());
    }

    @Test
    void testDisabledBeatsInheritedDefault() {
        Mockito.when(projectWorkflowService.getProjectWorkflow(1L, "wf-1"))
            .thenReturn(projectWorkflow(10L, null, true));

        Assertions.assertTrue(errorWorkflowResolver.resolve(1L, "wf-1")
            .isEmpty());
        Mockito.verifyNoInteractions(projectService);
    }

    @Test
    void testNoConfigurationAnywhere() {
        Mockito.when(projectWorkflowService.getProjectWorkflow(1L, "wf-1"))
            .thenReturn(projectWorkflow(10L, null, false));
        Mockito.when(projectService.getProject(1L))
            .thenReturn(project(null));

        Assertions.assertTrue(errorWorkflowResolver.resolve(1L, "wf-1")
            .isEmpty());
    }

    @Test
    void testSelfReferenceIsRejected() {
        Mockito.when(projectWorkflowService.getProjectWorkflow(1L, "wf-1"))
            .thenReturn(projectWorkflow(10L, 10L, false));

        Assertions.assertTrue(errorWorkflowResolver.resolve(1L, "wf-1")
            .isEmpty());
    }

    private static Project project(Long errorProjectWorkflowId) {
        Project project = new Project();

        project.setId(1L);
        project.setErrorProjectWorkflowId(errorProjectWorkflowId);

        return project;
    }

    private static ProjectWorkflow projectWorkflow(long id, Long errorId, boolean disabled) {
        ProjectWorkflow projectWorkflow = new ProjectWorkflow();

        projectWorkflow.setId(id);
        projectWorkflow.setWorkflowId("wf-" + id);
        projectWorkflow.setErrorProjectWorkflowId(errorId);
        projectWorkflow.setErrorWorkflowDisabled(disabled);

        return projectWorkflow;
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "*ErrorWorkflowResolverTest*" > /tmp/t3.log 2>&1; echo $?`
Expected: non-zero, `cannot find symbol: class ErrorWorkflowResolver`.

- [ ] **Step 3: Write the reference record**

```java
package com.bytechef.automation.configuration.domain;

/**
 * Everything the coordinator needs to dispatch a handler: the workflow id to submit, plus the failed run's identity
 * for the payload. The {@code failed*} fields describe the run that failed, not the handler — the payload's workflow
 * block identifies the failed workflow, as it does in n8n.
 *
 * @author Ivica Cardic
 */
public record ErrorWorkflowDispatch(
    String handlerWorkflowId, long projectId, long failedProjectWorkflowId, String failedWorkflowId,
    String failedWorkflowLabel) {
}
```

(with the Apache header from Task 2 Step 2)

- [ ] **Step 4: Write the resolver**

```java
package com.bytechef.automation.configuration.service;

import com.bytechef.automation.configuration.domain.ErrorWorkflowDispatch;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Resolves which workflow handles a failed run: the failing workflow's own override, else the project default, else
 * none. An explicit disable on the workflow beats an inherited project default, which is why the disable flag is
 * separate from the nullable reference.
 *
 * @author Ivica Cardic
 */
@Service
public class ErrorWorkflowResolver {

    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    public ErrorWorkflowResolver(
        ProjectDeploymentService projectDeploymentService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {

        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
    }

    /**
     * @param jobPrincipalId the failed job's principal id, which for automation is the PROJECT DEPLOYMENT id, not the
     *                       project id — mapped here so the coordinator needs no project lookups of its own
     */
    public Optional<ErrorWorkflowDispatch> resolve(long jobPrincipalId, String failedWorkflowId) {
        long projectId = projectDeploymentService.getProjectDeployment(jobPrincipalId)
            .getProjectId();

        ProjectWorkflow failingProjectWorkflow =
            projectWorkflowService.getProjectWorkflow(projectId, failedWorkflowId);

        if (failingProjectWorkflow.isErrorWorkflowDisabled()) {
            return Optional.empty();
        }

        Long targetId = failingProjectWorkflow.getErrorProjectWorkflowId();

        if (targetId == null) {
            Project project = projectService.getProject(projectId);

            targetId = project.getErrorProjectWorkflowId();
        }

        if (targetId == null) {
            return Optional.empty();
        }

        // Defensive: configuration-time validation rejects self-reference, but a workflow can be re-pointed
        // afterwards, and a self-referencing handler would fail forever.
        if (targetId.equals(failingProjectWorkflow.getId())) {
            return Optional.empty();
        }

        ProjectWorkflow target = projectWorkflowService.getProjectWorkflow(targetId);

        Workflow failedWorkflow = workflowService.getWorkflow(failedWorkflowId);

        return Optional.of(
            new ErrorWorkflowDispatch(
                target.getWorkflowId(), projectId, failingProjectWorkflow.getId(), failedWorkflowId,
                failedWorkflow.getLabel()));
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "*ErrorWorkflowResolverTest*" > /tmp/t3.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-configuration
git commit -m "Add error-workflow resolution: workflow override, project default, none

An explicit disable on the workflow beats an inherited project default, and a
self-referencing handler resolves to none even though configuration-time
validation already rejects it, because a workflow can be re-pointed afterwards."
```

---

### Task 4: Configuration-time validation

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/ErrorWorkflowConfigurationValidator.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/ErrorWorkflowConfigurationValidatorTest.java`

**Interfaces:**
- Consumes: `WorkflowConstants.NEW_WORKFLOW_ERROR` from Task 2.
- Produces: `ErrorWorkflowConfigurationValidator.validate(long projectId, long targetProjectWorkflowId, Long configuredOnProjectWorkflowId)` — throws `IllegalArgumentException` on any violation.

- [ ] **Step 1: Write the failing test**

```java
@ExtendWith(MockitoExtension.class)
class ErrorWorkflowConfigurationValidatorTest {

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private ErrorWorkflowConfigurationValidator validator;

    @Test
    void testRejectsTargetInAnotherProject() {
        ProjectWorkflow target = new ProjectWorkflow();

        target.setId(5L);
        target.setProjectId(999L);

        Mockito.when(projectWorkflowService.getProjectWorkflow(5L))
            .thenReturn(target);

        Assertions.assertThrows(
            IllegalArgumentException.class, () -> validator.validate(1L, 5L, null));
    }

    @Test
    void testRejectsTargetWithoutErrorTrigger() {
        stubTarget(5L, 1L, "wf-5");

        Mockito.when(workflowService.getWorkflow("wf-5"))
            .thenReturn(workflowWithTriggers("workflow/newWorkflowCall"));

        Assertions.assertThrows(
            IllegalArgumentException.class, () -> validator.validate(1L, 5L, null));
    }

    @Test
    void testRejectsSelfReference() {
        stubTarget(5L, 1L, "wf-5");

        Mockito.when(workflowService.getWorkflow("wf-5"))
            .thenReturn(workflowWithTriggers("workflow/newWorkflowError"));

        Assertions.assertThrows(
            IllegalArgumentException.class, () -> validator.validate(1L, 5L, 5L));
    }

    @Test
    void testAcceptsValidTarget() {
        stubTarget(5L, 1L, "wf-5");

        Mockito.when(workflowService.getWorkflow("wf-5"))
            .thenReturn(workflowWithTriggers("workflow/newWorkflowError"));

        Assertions.assertDoesNotThrow(() -> validator.validate(1L, 5L, 9L));
    }

    private void stubTarget(long id, long projectId, String workflowId) {
        ProjectWorkflow target = new ProjectWorkflow();

        target.setId(id);
        target.setProjectId(projectId);
        target.setWorkflowId(workflowId);

        Mockito.when(projectWorkflowService.getProjectWorkflow(id))
            .thenReturn(target);
    }

    private static Workflow workflowWithTriggers(String triggerType) {
        Workflow workflow = new Workflow();

        workflow.setDefinition(
            "{\"triggers\":[{\"name\":\"t1\",\"type\":\"" + triggerType + "\"}],\"tasks\":[]}");

        return workflow;
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "*ErrorWorkflowConfigurationValidatorTest*" > /tmp/t4.log 2>&1; echo $?`
Expected: non-zero, `cannot find symbol: class ErrorWorkflowConfigurationValidator`.

- [ ] **Step 3: Write the validator**

```java
package com.bytechef.automation.configuration.facade;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.component.workflow.constant.WorkflowConstants;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Validates an error-workflow reference when it is configured, not when a run fails. A broken reference discovered at
 * failure time would surface as a second failure while the first is being handled.
 *
 * @author Ivica Cardic
 */
@Component
public class ErrorWorkflowConfigurationValidator {

    private static final String ERROR_TRIGGER_TYPE = "workflow/" + WorkflowConstants.NEW_WORKFLOW_ERROR;

    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    public ErrorWorkflowConfigurationValidator(
        ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {

        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
    }

    public void validate(
        long projectId, long targetProjectWorkflowId, @Nullable Long configuredOnProjectWorkflowId) {

        if (Long.valueOf(targetProjectWorkflowId)
            .equals(configuredOnProjectWorkflowId)) {

            throw new IllegalArgumentException("A workflow cannot be its own error workflow");
        }

        ProjectWorkflow target = projectWorkflowService.getProjectWorkflow(targetProjectWorkflowId);

        if (target.getProjectId() != projectId) {
            throw new IllegalArgumentException(
                "The error workflow must belong to the same project as the workflow it handles");
        }

        Workflow workflow = workflowService.getWorkflow(target.getWorkflowId());

        boolean hasErrorTrigger = workflow.getTriggers()
            .stream()
            .anyMatch(trigger -> ERROR_TRIGGER_TYPE.equals(trigger.getType()));

        if (!hasErrorTrigger) {
            throw new IllegalArgumentException(
                "The error workflow must contain a " + ERROR_TRIGGER_TYPE + " trigger");
        }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "*ErrorWorkflowConfigurationValidatorTest*" > /tmp/t4.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-configuration
git commit -m "Validate error-workflow references at configuration time

Same project, carries a workflow/newWorkflowError trigger, and is not the
workflow being configured. Validating at failure time instead would surface a
broken reference as a second failure while the first is being handled."
```

---

### Task 5: Payload assembly

**Files:**
- Create: `server/libs/platform/platform-coordinator/src/main/java/com/bytechef/platform/coordinator/event/listener/ErrorWorkflowPayloadFactory.java`
- Test: `server/libs/platform/platform-coordinator/src/test/java/com/bytechef/platform/coordinator/event/listener/ErrorWorkflowPayloadFactoryTest.java`

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces: constructor `ErrorWorkflowPayloadFactory(String publicUrl)`; method `build(Job job, @Nullable TaskExecution lastTaskExecution, ErrorWorkflowContext context): Map<String, Object>`; nested record `ErrorWorkflowPayloadFactory.ErrorWorkflowContext(long projectId, long projectWorkflowId, String workflowId, String label, String environment)`. Task 6 passes the returned map as the handler job's **inputs**.

- [ ] **Step 1: Write the failing contract test**

The payload is a public contract the moment a user builds a handler against it, so the field names are asserted explicitly rather than via a shape comparison.

```java
class ErrorWorkflowPayloadFactoryTest {

    @Test
    void testPayloadFieldNamesArePinned() {
        Job job = new Job();

        job.setId(11L);
        job.setStatus(Job.Status.FAILED);

        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setError(new ExecutionError("boom", List.of("frame-1")));

        Map<String, Object> payload = new ErrorWorkflowPayloadFactory("https://app.example.com")
            .build(job, taskExecution, context());

        Map<String, Object> execution = (Map<String, Object>) payload.get("execution");
        Map<String, Object> error = (Map<String, Object>) execution.get("error");
        Map<String, Object> workflow = (Map<String, Object>) payload.get("workflow");

        Assertions.assertEquals(
            Set.of("jobId", "url", "error", "lastTaskExecuted", "mode", "resumeOf"), execution.keySet());
        Assertions.assertEquals(Set.of("message", "stackTrace"), error.keySet());
        Assertions.assertEquals(
            Set.of("projectId", "projectWorkflowId", "workflowId", "label"), workflow.keySet());
        Assertions.assertEquals(Set.of("execution", "workflow", "environment"), payload.keySet());
        Assertions.assertEquals("boom", error.get("message"));
        Assertions.assertEquals("https://app.example.com/automation/executions/11", execution.get("url"));
    }

    @Test
    void testLastTaskExecutedIsNullWhenNoTaskRan() {
        Job job = new Job();

        job.setId(11L);

        Map<String, Object> payload = new ErrorWorkflowPayloadFactory("https://app.example.com")
            .build(job, null, context());

        Map<String, Object> execution = (Map<String, Object>) payload.get("execution");

        Assertions.assertNull(execution.get("lastTaskExecuted"));
    }

    @Test
    void testPayloadCarriesNoTaskInputsOrOutputs() {
        Job job = new Job();

        job.setId(11L);

        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setOutput("secret-output");

        String rendered = new ErrorWorkflowPayloadFactory("https://app.example.com")
            .build(job, taskExecution, context())
            .toString();

        Assertions.assertFalse(rendered.contains("secret-output"));
    }

    private static ErrorWorkflowPayloadFactory.ErrorWorkflowContext context() {
        return new ErrorWorkflowPayloadFactory.ErrorWorkflowContext(
            1L, 2L, "wf-1", "My Workflow", "PRODUCTION");
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:libs:platform:platform-coordinator:test --tests "*ErrorWorkflowPayloadFactoryTest*" > /tmp/t5.log 2>&1; echo $?`
Expected: non-zero, `cannot find symbol: class ErrorWorkflowPayloadFactory`.

- [ ] **Step 3: Write the factory**

```java
package com.bytechef.platform.coordinator.event.listener;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.error.ExecutionError;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Builds the Error Trigger payload. Deliberately carries no task inputs or outputs: the job id is the handle, and a
 * handler needing more fetches it through the existing APIs, which keeps copying run data into a second execution
 * history an explicit choice.
 *
 * @author Ivica Cardic
 */
public class ErrorWorkflowPayloadFactory {

    public record ErrorWorkflowContext(
        long projectId, long projectWorkflowId, String workflowId, String label, String environment) {
    }

    private final String publicUrl;

    public ErrorWorkflowPayloadFactory(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public Map<String, Object> build(
        Job job, @Nullable TaskExecution lastTaskExecution, ErrorWorkflowContext context) {

        Map<String, Object> error = new LinkedHashMap<>();

        ExecutionError executionError = job.getError();

        error.put("message", executionError == null ? null : executionError.getMessage());
        error.put("stackTrace", executionError == null ? null : String.join("\n", executionError.getStackTrace()));

        if (executionError == null && lastTaskExecution != null && lastTaskExecution.getError() != null) {
            ExecutionError taskError = lastTaskExecution.getError();

            error.put("message", taskError.getMessage());
            error.put("stackTrace", String.join("\n", taskError.getStackTrace()));
        }

        Map<String, Object> execution = new LinkedHashMap<>();

        execution.put("jobId", String.valueOf(job.getId()));
        execution.put("url", publicUrl + "/automation/executions/" + job.getId());
        execution.put("error", error);
        execution.put("lastTaskExecuted", lastTaskExecution == null ? null : lastTaskExecution.getName());
        execution.put("mode", job.getMetadata("mode"));
        execution.put("resumeOf", job.getMetadata("resumeOf"));

        Map<String, Object> workflow = new LinkedHashMap<>();

        workflow.put("projectId", String.valueOf(context.projectId()));
        workflow.put("projectWorkflowId", String.valueOf(context.projectWorkflowId()));
        workflow.put("workflowId", context.workflowId());
        workflow.put("label", context.label());

        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("execution", execution);
        payload.put("workflow", workflow);
        payload.put("environment", context.environment());

        return payload;
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :server:libs:platform:platform-coordinator:test --tests "*ErrorWorkflowPayloadFactoryTest*" > /tmp/t5.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-coordinator
git commit -m "Add the Error Trigger payload factory

Field names are pinned by test because the payload becomes a public contract the
moment a user builds a handler against it. Carries no task inputs or outputs; the
job id is the handle for anything more."
```

---

### Task 6: Dispatch listener

**Files:**
- Create: `server/libs/platform/platform-coordinator/src/main/java/com/bytechef/platform/coordinator/event/listener/ErrorWorkflowJobStatusApplicationEventListener.java`
- Create: `server/libs/platform/platform-coordinator/src/main/java/com/bytechef/platform/coordinator/ErrorWorkflowDispatchCounter.java`
- Modify: `server/libs/platform/platform-coordinator/src/main/java/com/bytechef/platform/coordinator/config/PlatformCoordinatorConfiguration.java`
- Modify: `server/libs/platform/platform-coordinator/build.gradle.kts`
- Test: `server/libs/platform/platform-coordinator/src/test/java/com/bytechef/platform/coordinator/event/listener/ErrorWorkflowJobStatusApplicationEventListenerTest.java`

**Interfaces:**
- Consumes: `ErrorWorkflowResolver.resolve(long, String)` (Task 3), `ErrorWorkflowPayloadFactory.build(...)` (Task 5).
- Produces: constant `ErrorWorkflowJobStatusApplicationEventListener.ERROR_HANDLER_FOR = "errorHandlerFor"`, used by Task 7.

- [ ] **Step 1: Write the recursion test FIRST**

This is the test that keeps an infinite job loop out of production. Write it before the others.

```java
@ExtendWith(MockitoExtension.class)
class ErrorWorkflowJobStatusApplicationEventListenerTest {

    @Mock
    private ErrorWorkflowResolver errorWorkflowResolver;

    @Mock
    private JobService jobService;

    @Mock
    private PrincipalJobFacade principalJobFacade;

    @Mock
    private PrincipalJobService principalJobService;

    @Mock
    private TaskExecutionService taskExecutionService;

    @Test
    void testDoesNotDispatchForAnErrorHandlerRun() {
        Job job = new Job();

        job.setId(11L);
        job.setMetadata(Map.of("errorHandlerFor", "10"));

        Mockito.when(jobService.getJob(11L))
            .thenReturn(job);

        listener().onApplicationEvent(new JobStatusApplicationEvent(11L, Job.Status.FAILED));

        Mockito.verifyNoInteractions(principalJobFacade);
        Mockito.verifyNoInteractions(errorWorkflowResolver);
    }

    @Test
    void testIgnoresNonFailedStatuses() {
        listener().onApplicationEvent(new JobStatusApplicationEvent(11L, Job.Status.COMPLETED));

        Mockito.verifyNoInteractions(jobService);
        Mockito.verifyNoInteractions(principalJobFacade);
    }

    @Test
    void testSkipsWhenNoHandlerConfigured() {
        Job job = new Job();

        job.setId(11L);
        job.setWorkflowId("wf-1");
        job.setMetadata(Map.of());

        Mockito.when(jobService.getJob(11L))
            .thenReturn(job);
        Mockito.when(principalJobService.fetchJobPrincipalId(11L, PlatformType.AUTOMATION))
            .thenReturn(Optional.of(3L));
        Mockito.when(errorWorkflowResolver.resolve(Mockito.anyLong(), Mockito.eq("wf-1")))
            .thenReturn(Optional.empty());

        listener().onApplicationEvent(new JobStatusApplicationEvent(11L, Job.Status.FAILED));

        Mockito.verifyNoInteractions(principalJobFacade);
    }

    @Test
    void testDispatchFailureIsSwallowed() {
        Job job = new Job();

        job.setId(11L);
        job.setWorkflowId("wf-1");
        job.setMetadata(Map.of());

        Mockito.when(jobService.getJob(11L))
            .thenReturn(job);
        Mockito.when(principalJobService.fetchJobPrincipalId(11L, PlatformType.AUTOMATION))
            .thenThrow(new IllegalStateException("boom"));

        Assertions.assertDoesNotThrow(
            () -> listener().onApplicationEvent(new JobStatusApplicationEvent(11L, Job.Status.FAILED)));
    }

    private ErrorWorkflowJobStatusApplicationEventListener listener() {
        return new ErrorWorkflowJobStatusApplicationEventListener(
            new ErrorWorkflowPayloadFactory("https://app.example.com"), errorWorkflowResolver, jobService,
            principalJobFacade, principalJobService, taskExecutionService, null);
    }
}
```

Note the documented Mockito trap: unstubbed wrapper-returning methods yield `0`, not `null`. This listener branches on nullable ids, so stub `thenReturn(null)` explicitly wherever a null must be observed.

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:libs:platform:platform-coordinator:test --tests "*ErrorWorkflowJobStatusApplicationEventListenerTest*" > /tmp/t6.log 2>&1; echo $?`
Expected: non-zero, `cannot find symbol: class ErrorWorkflowJobStatusApplicationEventListener`.

- [ ] **Step 3: Add the module dependencies**

In `server/libs/platform/platform-coordinator/build.gradle.kts`, add:

```kotlin
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
```

- [ ] **Step 4: Write the metric counter**

```java
package com.bytechef.platform.coordinator;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.jspecify.annotations.Nullable;

/**
 * Counts error-workflow dispatch outcomes. No workspace or project tag: this fires on every failed run, so the tag
 * set stays bounded.
 *
 * @author Ivica Cardic
 */
public class ErrorWorkflowDispatchCounter {

    private final @Nullable MeterRegistry meterRegistry;

    public ErrorWorkflowDispatchCounter(@Nullable MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(String outcome) {
        if (meterRegistry == null) {
            return;
        }

        Counter.builder("bytechef_error_workflow_dispatch")
            .tag("outcome", outcome)
            .register(meterRegistry)
            .increment();
    }
}
```

- [ ] **Step 5: Write the listener**

```java
package com.bytechef.platform.coordinator.event.listener;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.ErrorWorkflowDispatch;
import com.bytechef.automation.configuration.service.ErrorWorkflowResolver;
import com.bytechef.message.event.ApplicationEvent;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.coordinator.ErrorWorkflowDispatchCounter;
import com.bytechef.platform.workflow.execution.dto.JobParametersDTO;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatches the configured error workflow when a run ends FAILED.
 * <p>
 * Ordered after the cost and workflow-alert listeners so dispatch never delays alerting. Fail-open throughout:
 * handling an error must never manufacture a second one.
 *
 * @author Ivica Cardic
 */
public class ErrorWorkflowJobStatusApplicationEventListener implements ApplicationEventListener {

    public static final String ERROR_HANDLER_FOR = "errorHandlerFor";

    private static final Logger log =
        LoggerFactory.getLogger(ErrorWorkflowJobStatusApplicationEventListener.class);

    private final ErrorWorkflowPayloadFactory errorWorkflowPayloadFactory;
    private final ErrorWorkflowResolver errorWorkflowResolver;
    private final JobService jobService;
    private final PrincipalJobFacade principalJobFacade;
    private final PrincipalJobService principalJobService;
    private final TaskExecutionService taskExecutionService;
    private final @Nullable ErrorWorkflowDispatchCounter counter;

    public ErrorWorkflowJobStatusApplicationEventListener(
        ErrorWorkflowPayloadFactory errorWorkflowPayloadFactory, ErrorWorkflowResolver errorWorkflowResolver,
        JobService jobService, PrincipalJobFacade principalJobFacade, PrincipalJobService principalJobService,
        TaskExecutionService taskExecutionService, @Nullable ErrorWorkflowDispatchCounter counter) {

        this.errorWorkflowPayloadFactory = errorWorkflowPayloadFactory;
        this.errorWorkflowResolver = errorWorkflowResolver;
        this.jobService = jobService;
        this.principalJobFacade = principalJobFacade;
        this.principalJobService = principalJobService;
        this.taskExecutionService = taskExecutionService;
        this.counter = counter;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (!(applicationEvent instanceof JobStatusApplicationEvent jobStatusApplicationEvent)) {
            return;
        }

        if (jobStatusApplicationEvent.getStatus() != Job.Status.FAILED) {
            return;
        }

        try {
            dispatch(jobStatusApplicationEvent.getJobId());
        } catch (Exception exception) {
            record("failed");

            log.warn(
                "Error workflow dispatch failed for job {}", jobStatusApplicationEvent.getJobId(), exception);
        }
    }

    private void dispatch(long jobId) {
        Job job = jobService.getJob(jobId);

        // Recursion cap. Without this one check a persistently broken handler spawns jobs forever.
        if (job.getMetadata(ERROR_HANDLER_FOR) != null) {
            record("skipped_recursion");

            return;
        }

        Optional<Long> principalId = principalJobService.fetchJobPrincipalId(jobId, PlatformType.AUTOMATION);

        if (principalId.isEmpty()) {
            record("skipped_no_config");

            return;
        }

        Optional<ErrorWorkflowDispatch> reference =
            errorWorkflowResolver.resolve(principalId.get(), job.getWorkflowId());

        if (reference.isEmpty()) {
            record("skipped_no_config");

            return;
        }

        ErrorWorkflowDispatch dispatch = reference.get();

        // The payload is the handler's INPUT. Passing Map.of() here would dispatch a handler that receives
        // nothing, which is the whole point of the feature. The workflow block describes the FAILED run.
        Map<String, Object> inputs = errorWorkflowPayloadFactory.build(
            job, taskExecutionService.fetchLastTaskExecution(jobId)
                .orElse(null),
            new ErrorWorkflowPayloadFactory.ErrorWorkflowContext(
                dispatch.projectId(), dispatch.failedProjectWorkflowId(), dispatch.failedWorkflowId(),
                dispatch.failedWorkflowLabel(), String.valueOf(job.getMetadata("environment"))));

        JobParametersDTO jobParametersDTO = new JobParametersDTO(
            dispatch.handlerWorkflowId(), inputs, Map.of(ERROR_HANDLER_FOR, String.valueOf(jobId)));

        principalJobFacade.createJob(jobParametersDTO, principalId.get(), PlatformType.AUTOMATION);

        record("dispatched");
    }

    private void record(String outcome) {
        if (counter != null) {
            counter.record(outcome);
        }
    }
}
```

- [ ] **Step 6: Register the bean**

In `PlatformCoordinatorConfiguration.java`, beside the existing notification listener bean:

```java
    @Bean
    @Order(300)
    ErrorWorkflowJobStatusApplicationEventListener errorWorkflowJobStatusApplicationEventListener(
        ErrorWorkflowResolver errorWorkflowResolver, JobService jobService, PrincipalJobFacade principalJobFacade,
        PrincipalJobService principalJobService, TaskExecutionService taskExecutionService,
        ApplicationProperties applicationProperties, ObjectProvider<MeterRegistry> meterRegistryProvider) {

        return new ErrorWorkflowJobStatusApplicationEventListener(
            new ErrorWorkflowPayloadFactory(applicationProperties.getPublicUrl()), errorWorkflowResolver, jobService,
            principalJobFacade, principalJobService, taskExecutionService,
            new ErrorWorkflowDispatchCounter(meterRegistryProvider.getIfAvailable()));
    }
```

- [ ] **Step 7: Run the tests to verify they pass**

Run: `./gradlew :server:libs:platform:platform-coordinator:test --tests "*ErrorWorkflowJobStatusApplicationEventListenerTest*" > /tmp/t6.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 8: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-coordinator
git commit -m "Dispatch the configured error workflow when a run ends FAILED

Ordered at 300, after cost and workflow alerts, so dispatch never delays
alerting. The errorHandlerFor metadata key caps recursion at depth 1: without it
a persistently broken handler spawns jobs forever. Admission gates are not
bypassed, so a bad deploy failing thousands of runs is bounded by plan limits
rather than becoming an unbounded job storm."
```

---

### Task 7: End-to-end integration test

**Files:**
- Test: `server/libs/platform/platform-coordinator/src/test/java/com/bytechef/platform/coordinator/ErrorWorkflowIntTest.java`

**Interfaces:**
- Consumes: everything from Tasks 1-6.
- Produces: nothing.

- [ ] **Step 1: Write the integration test**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testint")
class ErrorWorkflowIntTest {

    @Autowired
    private ErrorWorkflowJobStatusApplicationEventListener listener;

    @Autowired
    private JobService jobService;

    @Test
    void testFailedRunDispatchesTheHandler() {
        long failedJobId = givenFailedJobWithConfiguredHandler();

        listener.onApplicationEvent(new JobStatusApplicationEvent(failedJobId, Job.Status.FAILED));

        Job handlerJob = jobService.getJobs()
            .stream()
            .filter(job -> job.getMetadata("errorHandlerFor") != null)
            .findFirst()
            .orElseThrow();

        Assertions.assertEquals(String.valueOf(failedJobId), handlerJob.getMetadata("errorHandlerFor"));
    }

    @Test
    void testFailingHandlerDoesNotSpawnAnother() {
        long handlerJobId = givenFailedJobCarryingErrorHandlerForMetadata();

        int before = jobService.getJobs()
            .size();

        listener.onApplicationEvent(new JobStatusApplicationEvent(handlerJobId, Job.Status.FAILED));

        Assertions.assertEquals(
            before, jobService.getJobs()
                .size(),
            "a failing error workflow must not spawn another error workflow");
    }
}
```

Implement `givenFailedJobWithConfiguredHandler()` and `givenFailedJobCarryingErrorHandlerForMetadata()` as private helpers in the test: create a project, two project workflows (one carrying a `workflow/newWorkflowError` trigger), point the first at the second via `setErrorProjectWorkflowId`, and persist a `FAILED` job linked to the first through `PrincipalJobService`. For the second helper, persist a `FAILED` job whose metadata already contains `errorHandlerFor`.

- [ ] **Step 2: Run it**

Run: `./gradlew :server:libs:platform:platform-coordinator:testIntegration --tests "*ErrorWorkflowIntTest*" > /tmp/t7.log 2>&1; echo $?`
Expected: `0`. Confirm no failures: `grep -c "FAILED" /tmp/t7.log` is `0`.

- [ ] **Step 3: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-coordinator
git commit -m "Add end-to-end error workflow integration test

Covers the dispatch path and its negative twin: a failing error workflow must not
spawn another. That second test is what keeps an infinite job loop out of
production."
```

---

### Task 8: Documentation

**Files:**
- Create: `docs/content/docs/automation/error-workflows.mdx`
- Modify: `CLAUDE.md`

**Interfaces:**
- Consumes: the field names from Task 5 and the config semantics from Tasks 1 and 3.
- Produces: nothing.

- [ ] **Step 1: Write the user documentation**

Create `docs/content/docs/automation/error-workflows.mdx` covering: what an error workflow is; how it differs from the `on-error` task dispatcher (intra- vs inter-workflow, and that a handled error ends `COMPLETED` so no handler fires); how to build one (add the **New Workflow Error** trigger); how to configure it on a project and override or disable it per workflow; the payload table with the exact field names from Task 5; and the limits — automation only, same project only, depth-1 recursion cap, no dedup on failure storms.

- [ ] **Step 2: Add the CLAUDE.md entry**

Add under a new `### Workflow error handler` heading:

```markdown
### Workflow error handler

When an automation run ends `FAILED`, `ErrorWorkflowJobStatusApplicationEventListener`
(platform-coordinator, `@Order(300)`, after cost and workflow alerts) dispatches the configured error
workflow through `PrincipalJobFacade.createJob`. Config is a nullable
`project.error_project_workflow_id` with a per-workflow override on `project_workflow`, plus a separate
`error_workflow_disabled` flag because null already means inherit. The handler must live in the same
project and carry a `workflow/newWorkflowError` trigger; both are validated when configured, not at
failure time. `errorHandlerFor` job metadata caps recursion at depth 1 — a failing handler does not
spawn another. Admission gates are deliberately not bypassed, so a failure storm is bounded by plan
limits. This layers on the `on-error` task dispatcher rather than competing with it: a handled error
ends the job `COMPLETED`, so an error workflow fires only on genuinely uncaught failures. Automation
only; embedded has no project to hang the config on.
```

- [ ] **Step 3: Verify the docs build**

Run: `cd docs && npm run types:check`
Expected: exit `0`.

- [ ] **Step 4: Commit**

```bash
git add docs CLAUDE.md
git commit -m "Document error workflows

Covers how they layer on the on-error dispatcher rather than competing with it,
the exact payload field names, and the deliberate limits: automation only, same
project, depth-1 recursion cap, no failure-storm dedup."
```

---

### Task 9: Configuration API

This is `ErrorWorkflowConfigurationValidator`'s production caller. Until this task lands, Task 4's validator is
correct but unreachable and the columns are settable only by direct SQL.

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/facade/ProjectFacade.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/ProjectFacadeImpl.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/com/bytechef/automation/configuration/web/graphql/ProjectGraphQlController.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/project.graphqls`
- Test: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/ProjectErrorWorkflowFacadeTest.java`

**Interfaces:**
- Consumes: `ErrorWorkflowConfigurationValidator.validate(long projectId, long targetProjectWorkflowId, Long configuredOnProjectWorkflowId)` from Task 4; `Project.setErrorProjectWorkflowId(Long)` from Task 1.
- Produces: `ProjectFacade.updateProjectErrorWorkflow(long projectId, @Nullable Long errorProjectWorkflowId)`.

- [ ] **Step 1: Write the failing test**

```java
@ExtendWith(MockitoExtension.class)
class ProjectErrorWorkflowFacadeTest {

    @Mock
    private ErrorWorkflowConfigurationValidator errorWorkflowConfigurationValidator;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private ProjectFacadeImpl projectFacade;

    @Test
    void testValidatesBeforeSaving() {
        Project project = new Project();

        project.setId(1L);

        Mockito.when(projectService.getProject(1L))
            .thenReturn(project);

        projectFacade.updateProjectErrorWorkflow(1L, 5L);

        Mockito.verify(errorWorkflowConfigurationValidator)
            .validate(1L, 5L, null);
        Assertions.assertEquals(5L, project.getErrorProjectWorkflowId());
    }

    @Test
    void testClearingSkipsValidation() {
        Project project = new Project();

        project.setId(1L);
        project.setErrorProjectWorkflowId(5L);

        Mockito.when(projectService.getProject(1L))
            .thenReturn(project);

        projectFacade.updateProjectErrorWorkflow(1L, null);

        Mockito.verifyNoInteractions(errorWorkflowConfigurationValidator);
        Assertions.assertNull(project.getErrorProjectWorkflowId());
    }

    @Test
    void testRejectedReferenceIsNotSaved() {
        Project project = new Project();

        project.setId(1L);

        Mockito.when(projectService.getProject(1L))
            .thenReturn(project);
        Mockito.doThrow(new IllegalArgumentException("nope"))
            .when(errorWorkflowConfigurationValidator)
            .validate(1L, 5L, null);

        Assertions.assertThrows(
            IllegalArgumentException.class, () -> projectFacade.updateProjectErrorWorkflow(1L, 5L));
        Mockito.verify(projectService, Mockito.never())
            .updateProject(Mockito.any());
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "*ProjectErrorWorkflowFacadeTest*" > /tmp/t9.log 2>&1; echo $?`
Expected: non-zero, `cannot find symbol: method updateProjectErrorWorkflow`.

- [ ] **Step 3: Add the facade method**

On `ProjectFacade`:

```java
    void updateProjectErrorWorkflow(long projectId, @Nullable Long errorProjectWorkflowId);
```

On `ProjectFacadeImpl` (inject `ErrorWorkflowConfigurationValidator` through the constructor alongside the existing
dependencies):

```java
    @Override
    public void updateProjectErrorWorkflow(long projectId, @Nullable Long errorProjectWorkflowId) {
        Project project = projectService.getProject(projectId);

        // Clearing needs no validation: there is no reference left to be invalid.
        if (errorProjectWorkflowId != null) {
            errorWorkflowConfigurationValidator.validate(projectId, errorProjectWorkflowId, null);
        }

        project.setErrorProjectWorkflowId(errorProjectWorkflowId);

        projectService.updateProject(project);
    }
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "*ProjectErrorWorkflowFacadeTest*" > /tmp/t9.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 5: Expose it over GraphQL**

In `project.graphqls`, add to the `Mutation` type:

```graphql
    updateProjectErrorWorkflow(projectId: ID!, errorProjectWorkflowId: ID): Boolean
```

In `ProjectGraphQlController`:

```java
    @MutationMapping
    public Boolean updateProjectErrorWorkflow(
        @Argument long projectId, @Argument @Nullable Long errorProjectWorkflowId) {

        projectFacade.updateProjectErrorWorkflow(projectId, errorProjectWorkflowId);

        return true;
    }
```

- [ ] **Step 6: Verify the module still builds and its tests pass**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-graphql:test :server:libs:automation:automation-configuration:automation-configuration-service:test > /tmp/t9b.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-configuration
git commit -m "Add the project error-workflow configuration API

Gives ErrorWorkflowConfigurationValidator its production caller: the facade
validates any non-null reference before saving, so a bad reference is rejected at
configuration time rather than surfacing as a second failure while the first is
being handled. Clearing skips validation because there is no reference left to be
invalid."
```

---

## Not in this plan

- **The per-workflow override API.** Task 9 adds the project-level setter only. Setting `error_project_workflow_id`
  or `error_workflow_disabled` on an individual `project_workflow` still requires direct SQL. The resolver (Task 3)
  reads both, and the integration test (Task 7) exercises the project-level path, so the override is implemented and
  tested but not yet settable through an API.
- **Client UI.** No editor surface for picking the error workflow; configuration is GraphQL-only after Task 9.
- **Stop And Error action** — out of scope per the spec.
- **Cross-project and embedded handlers, failure-storm dedup** — out of scope per the spec.
