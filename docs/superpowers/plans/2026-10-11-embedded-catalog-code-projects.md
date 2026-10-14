# Embedded Catalog Code Projects Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add "New Code Project" to Embedded → Automation Workflows, with a draft/publish lifecycle for catalog code
projects that the upload path shares.

**Architecture:** The draft logic private to the automation `ProjectCodeWorkflowFacadeImpl` moves into a shared
`ProjectCodeWorkflowDraftService` (interface in the EE automation API module, implementation in the EE automation
service module), which also owns a uuid fallback. The embedded `AutomationWorkflowProjectCodeWorkflowFacadeImpl` uses
it for create, edit and upload, and publishes through `AutomationWorkflowProjectFacade.publishProject`, which fires the
rollout event. The client adds a creation mode to the catalog project dialog and a code editor page keyed by catalog
project id.

**Tech Stack:** Java 25 / Spring Boot 4 / Spring for GraphQL / JUnit 5 + Mockito; React 19 / TypeScript / TanStack
Query / graphql-codegen / Vitest.

**Spec:** `docs/superpowers/specs/2026-10-11-embedded-catalog-code-projects-design.md`

## Global Constraints

- Branch `0_p_code_workflows`, worktree
  `/private/tmp/claude-502/-Volumes-Data-bytechef-bytechef/1d16d70c-d8e2-470d-8a0d-4e8645ea5998/scratchpad/wt-cw`.
- EE files: ByteChef Enterprise license header and `@version ee` Javadoc tag (copy the header from a sibling file).
- Commit subjects only (no body, no Co-Authored-By, no "Generated with" line). Client commits start with `client - `.
  GraphQL operations and the regenerated `src/shared/middleware/graphql.ts` go in separate commits.
- Never judge a Gradle run through a pipe: redirect to a log, check `$?`, then grep `^> Task .* FAILED`.
- New facade methods are admin-only: `@PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")`.
- No Java authoring in-app: create/edit reject `Language.JAVA` and `Language.RUBY`.
- Run `./gradlew spotlessApply` on touched modules before each server commit; `npx prettier --write` + `npx eslint`
  on touched client files before each client commit.
- Test naming: unit `<Class>Test`, integration `<Class>IntTest`; camelCase test method names, no underscores.

## Review Focus

1. **Upload in a different language over a pending draft** (draft is JavaScript, upload is Python): the stored
   container must take the uploaded language, not keep the draft's. Pinned in Task 1.
2. **A name starting with `__EMBEDDED`** passed to New Code Project: must fail with `INVALID_CODE_WORKFLOW_NAME`, not a
   500 from `createProject`'s `IllegalArgumentException`. Pinned in Task 4.
3. **Publishing an older uploaded project before any edit** (draft empty): must be refused with
   `CODE_WORKFLOW_DRAFT_EMPTY`, never roll out an empty version. Pinned in Task 3.
4. **Editing a visual (non-code) catalog project's source by id**: `getCodeWorkflowSource`/`updateCodeWorkflowSource`
   must fail cleanly, not NPE. Pinned in Task 4.
5. **A source edit that renames the project** (declared name changed): refused with `CODE_WORKFLOW_NAME_MISMATCH`, draft
   unchanged. Pinned in Task 4.

Test-placement note: no test context in this repository wires the real code-workflow stack (container facade, file
storage, polyglot loader); every existing code-workflow test mocks it. So the uuid-continuity checks the spec lists as
IntTests are written as unit tests over mocked containers, in the existing `ProjectCodeWorkflowFacadeDraftTest` style.
The spec's rollout check ("a reference survives an in-app edit and publish") has no separate test for the same reason:
code projects publish through the unchanged `AutomationWorkflowProjectFacade.publishProject`, whose rollout the
existing `ConnectedUserReferenceRolloutManagerIntTest` covers; the uuid half of that check is pinned in Task 1.

---

### Task 1: Shared draft service with uuid fallback

**Files:**
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-api/build.gradle.kts`
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration/service/ProjectCodeWorkflowDraftService.java`
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration/service/ProjectCodeWorkflowService.java`
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/repository/ProjectCodeWorkflowRepository.java`
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/service/ProjectCodeWorkflowServiceImpl.java`
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/service/ProjectCodeWorkflowDraftServiceImpl.java`
- Test: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/service/ProjectCodeWorkflowDraftServiceTest.java`

**Interfaces:**
- Produces: `ProjectCodeWorkflowDraftService.saveDraft(Project project, ProjectDefinition projectDefinition, byte[] bytes, Language language)`;
  `ProjectCodeWorkflowService.fetchPublishedProjectCodeWorkflow(long projectId, int draftProjectVersion): Optional<ProjectCodeWorkflow>`;
  `ProjectCodeWorkflowDraftServiceImpl(CodeWorkflowContainerFacade, CodeWorkflowContainerService, ProjectCodeWorkflowService, ProjectWorkflowService, WorkflowService, List<WorkflowPreDeleteListener>)`.

- [ ] **Step 1: Let the API module see `ProjectDefinition`**

In `automation-configuration-api/build.gradle.kts` add inside `dependencies`:

```kotlin
    api(project(":sdks:backend:automation:project-api"))
```

- [ ] **Step 2: Add the by-version lookup**

`ProjectCodeWorkflowRepository` — add:

```java
    @Query("""
        SELECT * FROM project_code_workflow
        WHERE project_id = :projectId AND project_version < :projectVersion
        ORDER BY id DESC
        LIMIT 1
        """)
    Optional<ProjectCodeWorkflow> findFirstByProjectIdAndProjectVersionLessThanOrderByIdDesc(
        @Param("projectId") Long projectId, @Param("projectVersion") int projectVersion);
```

`ProjectCodeWorkflowService` — add:

```java
    Optional<ProjectCodeWorkflow> fetchPublishedProjectCodeWorkflow(long projectId, int draftProjectVersion);
```

`ProjectCodeWorkflowServiceImpl` — add (annotate `@Transactional(readOnly = true)` like the sibling reads):

```java
    @Override
    @Transactional(readOnly = true)
    public Optional<ProjectCodeWorkflow> fetchPublishedProjectCodeWorkflow(long projectId, int draftProjectVersion) {
        return projectCodeWorkflowRepository.findFirstByProjectIdAndProjectVersionLessThanOrderByIdDesc(
            projectId, draftProjectVersion);
    }
```

Check the column name in the init changelog first: `grep -rn "project_code_workflow" server/libs/config/liquibase-config server/ee -l | head` and confirm `project_version`.

- [ ] **Step 3: Create the interface**

```java
package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;

/**
 * Writes a code workflow source into a project's draft version. Workflows keep their ids by name across drafts, and a
 * workflow added under a name the last published version had takes that version's uuid.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ProjectCodeWorkflowDraftService {

    void saveDraft(Project project, ProjectDefinition projectDefinition, byte[] bytes, Language language);
}
```

- [ ] **Step 4: Write the failing tests**

`ProjectCodeWorkflowDraftServiceTest` (package `com.bytechef.ee.automation.configuration.service`). Build the service
with mocks, as `ProjectCodeWorkflowFacadeDraftTest` builds the facade. Use this fixture helper and these tests:

```java
class ProjectCodeWorkflowDraftServiceTest {

    private final CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);
    private final CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);
    private final ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private final ProjectCodeWorkflowDraftServiceImpl projectCodeWorkflowDraftService =
        new ProjectCodeWorkflowDraftServiceImpl(
            codeWorkflowContainerFacade, codeWorkflowContainerService, projectCodeWorkflowService,
            projectWorkflowService, workflowService, List.of());

    @Test
    void testSaveDraftUpdatesTheDraftContainerInPlace() {
        Project project = project(2);
        CodeWorkflowContainer draftContainer = container(Language.JAVASCRIPT, Map.of("wf-a", "draft-a"));

        stubLatest(pcw(10L, 2));

        when(codeWorkflowContainerService.getCodeWorkflowContainer(10L)).thenReturn(draftContainer);
        when(codeWorkflowContainerFacade.update(eq(draftContainer), any(), any(), any(), eq(PlatformType.AUTOMATION)))
            .thenReturn(new CodeWorkflowReconciliation(draftContainer, Map.of(), Map.of()));

        projectCodeWorkflowDraftService.saveDraft(project, definition(), BYTES, Language.JAVASCRIPT);

        verify(codeWorkflowContainerFacade).update(eq(draftContainer), any(), any(), eq(BYTES), any());
        verify(projectCodeWorkflowService, never()).create(any(), any());
    }

    @Test
    void testSaveDraftAfterPublishReusesDraftWorkflowIdsByName() {
        Project project = project(2);
        UUID uuid = UUID.randomUUID();

        stubLatest(pcw(10L, 1));

        when(codeWorkflowContainerService.getCodeWorkflowContainer(10L))
            .thenReturn(container(Language.JAVASCRIPT, Map.of("wf-a", "pub-a")));
        when(projectWorkflowService.getProjectWorkflows(1L, 1))
            .thenReturn(List.of(new ProjectWorkflow(1L, 1, "pub-a", uuid)));
        when(projectWorkflowService.getProjectWorkflows(1L, 2))
            .thenReturn(List.of(new ProjectWorkflow(1L, 2, "draft-a", uuid)));

        CodeWorkflowContainer newContainer = container(Language.JAVASCRIPT, Map.of("wf-a", "draft-a"));

        when(codeWorkflowContainerFacade.create(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new CodeWorkflowReconciliation(newContainer, Map.of(), Map.of()));

        projectCodeWorkflowDraftService.saveDraft(project, definition(), BYTES, Language.JAVASCRIPT);

        verify(codeWorkflowContainerFacade).create(
            any(), any(), any(), eq(Language.JAVASCRIPT), eq(BYTES), eq(PlatformType.AUTOMATION),
            eq(Map.of("wf-a", "draft-a")));
        verify(projectCodeWorkflowService).create(newContainer, project);
    }

    @Test
    void testSaveDraftAddsAndRemovesDraftWorkflows() {
        Project project = project(2);
        CodeWorkflowContainer draftContainer = container(Language.JAVASCRIPT, Map.of());

        stubLatest(pcw(10L, 2));

        when(codeWorkflowContainerService.getCodeWorkflowContainer(10L)).thenReturn(draftContainer);
        when(codeWorkflowContainerFacade.update(any(), any(), any(), any(), any()))
            .thenReturn(new CodeWorkflowReconciliation(
                draftContainer, Map.of("wf-new", "id-new"), Map.of("wf-old", "id-old")));
        when(projectWorkflowService.addWorkflow(1L, 2, "id-new"))
            .thenReturn(new ProjectWorkflow(1L, 2, "id-new", UUID.randomUUID()));

        projectCodeWorkflowDraftService.saveDraft(project, definition(), BYTES, Language.JAVASCRIPT);

        verify(projectWorkflowService).addWorkflow(1L, 2, "id-new");
        verify(projectWorkflowService).delete(1L, 2, "id-old");
        verify(workflowService).delete("id-old");
    }

    @Test
    void testSaveDraftOnAnEmptyDraftRestoresThePublishedUuidByName() {
        Project project = project(2);
        UUID publishedUuid = UUID.randomUUID();
        ProjectCodeWorkflow publishedPcw = pcw(10L, 1);

        stubLatest(publishedPcw);

        when(projectCodeWorkflowService.fetchPublishedProjectCodeWorkflow(1L, 2)).thenReturn(Optional.of(publishedPcw));
        when(codeWorkflowContainerService.getCodeWorkflowContainer(10L))
            .thenReturn(container(Language.JAVASCRIPT, Map.of("wf-a", "pub-a")));
        when(projectWorkflowService.getProjectWorkflows(1L, 1))
            .thenReturn(List.of(new ProjectWorkflow(1L, 1, "pub-a", publishedUuid)));
        when(projectWorkflowService.getProjectWorkflows(1L, 2)).thenReturn(List.of());

        CodeWorkflowContainer newContainer = container(Language.JAVASCRIPT, Map.of("wf-a", "new-a"));

        when(codeWorkflowContainerFacade.create(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new CodeWorkflowReconciliation(newContainer, Map.of("wf-a", "new-a"), Map.of()));

        ProjectWorkflow addedRow = new ProjectWorkflow(1L, 2, "new-a", UUID.randomUUID());

        when(projectWorkflowService.addWorkflow(1L, 2, "new-a")).thenReturn(addedRow);

        projectCodeWorkflowDraftService.saveDraft(project, definition(), BYTES, Language.JAVASCRIPT);

        assertThat(addedRow.getUuid()).isEqualTo(publishedUuid);

        verify(projectWorkflowService).update(addedRow);
    }

    @Test
    void testSaveDraftKeepsAFreshUuidForANameThePublishedVersionLacked() {
        Project project = project(2);
        ProjectCodeWorkflow publishedPcw = pcw(10L, 1);

        stubLatest(publishedPcw);

        when(projectCodeWorkflowService.fetchPublishedProjectCodeWorkflow(1L, 2)).thenReturn(Optional.of(publishedPcw));
        when(codeWorkflowContainerService.getCodeWorkflowContainer(10L))
            .thenReturn(container(Language.JAVASCRIPT, Map.of("wf-a", "pub-a")));
        when(projectWorkflowService.getProjectWorkflows(anyLong(), anyInt())).thenReturn(List.of());

        CodeWorkflowContainer newContainer = container(Language.JAVASCRIPT, Map.of("wf-b", "new-b"));

        when(codeWorkflowContainerFacade.create(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new CodeWorkflowReconciliation(newContainer, Map.of("wf-b", "new-b"), Map.of()));
        when(projectWorkflowService.addWorkflow(1L, 2, "new-b"))
            .thenReturn(new ProjectWorkflow(1L, 2, "new-b", UUID.randomUUID()));

        projectCodeWorkflowDraftService.saveDraft(project, definition(), BYTES, Language.JAVASCRIPT);

        verify(projectWorkflowService, never()).update(any());
    }

    @Test
    void testSaveDraftInAnotherLanguageReplacesTheDraftContainerKeepingWorkflowIds() {
        Project project = project(2);

        stubLatest(pcw(10L, 2));

        when(codeWorkflowContainerService.getCodeWorkflowContainer(10L))
            .thenReturn(container(Language.JAVASCRIPT, Map.of("wf-a", "draft-a")));

        CodeWorkflowContainer pythonContainer = container(Language.PYTHON, Map.of("wf-a", "draft-a"));

        when(codeWorkflowContainerFacade.create(any(), any(), any(), eq(Language.PYTHON), any(), any(), any()))
            .thenReturn(new CodeWorkflowReconciliation(pythonContainer, Map.of(), Map.of()));

        projectCodeWorkflowDraftService.saveDraft(project, definition(), BYTES, Language.PYTHON);

        verify(codeWorkflowContainerFacade, never()).update(any(), any(), any(), any(), any());
        verify(codeWorkflowContainerFacade).create(
            any(), any(), any(), eq(Language.PYTHON), eq(BYTES), eq(PlatformType.AUTOMATION),
            eq(Map.of("wf-a", "draft-a")));
        verify(projectCodeWorkflowService).create(pythonContainer, project);
    }

    private static final byte[] BYTES = "source".getBytes(StandardCharsets.UTF_8);

    private void stubLatest(ProjectCodeWorkflow projectCodeWorkflow) {
        when(projectCodeWorkflowService.fetchProjectCodeWorkflow(1L)).thenReturn(Optional.of(projectCodeWorkflow));
    }

    private static Project project(int draftVersion) {
        Project project = new Project();

        project.setId(1L);
        project.setName("my-code-project");
        project.setProjectVersions(List.of(new ProjectVersion(draftVersion)));

        return project;
    }

    private static ProjectCodeWorkflow pcw(long containerId, int projectVersion) {
        ProjectCodeWorkflow projectCodeWorkflow = mock(ProjectCodeWorkflow.class);

        when(projectCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(containerId);
        when(projectCodeWorkflow.getProjectVersion()).thenReturn(projectVersion);

        return projectCodeWorkflow;
    }

    private static CodeWorkflowContainer container(Language language, Map<String, String> workflowNameIds) {
        CodeWorkflowContainer codeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(codeWorkflowContainer.getLanguage()).thenReturn(language);
        when(codeWorkflowContainer.getWorkflowNameIds()).thenReturn(workflowNameIds);

        return codeWorkflowContainer;
    }

    private static ProjectDefinition definition() {
        ProjectDefinition projectDefinition = mock(ProjectDefinition.class);

        when(projectDefinition.getName()).thenReturn("my-code-project");
        when(projectDefinition.getVersion()).thenReturn("1");
        when(projectDefinition.getWorkflows()).thenReturn(List.of());

        return projectDefinition;
    }
}
```

If `new ProjectVersion(int)` or `Project#setProjectVersions` is not how `ProjectCodeWorkflowFacadeDraftTest` builds a
draft version, use exactly what that test uses. If `ProjectWorkflow#getUuid` returns a `UUID` under another accessor
name, adapt the assertion to the real accessor.

- [ ] **Step 5: Run the tests to see them fail**

```bash
./gradlew --configure-on-demand :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests '*ProjectCodeWorkflowDraftServiceTest*' > /tmp/claude-502/t1.log 2>&1; echo $?
grep -E "^> Task .* FAILED|error:" /tmp/claude-502/t1.log | head
```

Expected: compilation fails (`ProjectCodeWorkflowDraftServiceImpl` does not exist).

- [ ] **Step 6: Implement the service**

`ProjectCodeWorkflowDraftServiceImpl`: move `saveDraft` and `resolveDraftWorkflowNameIds` out of
`ProjectCodeWorkflowFacadeImpl` (lines around 321–399) unchanged, then add the language branch and the uuid fallback:

```java
@Service
@Transactional
@ConditionalOnEEVersion
public class ProjectCodeWorkflowDraftServiceImpl implements ProjectCodeWorkflowDraftService {

    private final CodeWorkflowContainerFacade codeWorkflowContainerFacade;
    private final CodeWorkflowContainerService codeWorkflowContainerService;
    private final ProjectCodeWorkflowService projectCodeWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;
    private final List<WorkflowPreDeleteListener> workflowPreDeleteListeners;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ProjectCodeWorkflowDraftServiceImpl(
        CodeWorkflowContainerFacade codeWorkflowContainerFacade,
        CodeWorkflowContainerService codeWorkflowContainerService,
        ProjectCodeWorkflowService projectCodeWorkflowService, ProjectWorkflowService projectWorkflowService,
        WorkflowService workflowService, List<WorkflowPreDeleteListener> workflowPreDeleteListeners) {

        this.codeWorkflowContainerFacade = codeWorkflowContainerFacade;
        this.codeWorkflowContainerService = codeWorkflowContainerService;
        this.projectCodeWorkflowService = projectCodeWorkflowService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
        this.workflowPreDeleteListeners = workflowPreDeleteListeners;
    }

    @Override
    public void saveDraft(Project project, ProjectDefinition projectDefinition, byte[] bytes, Language language) {
        long projectId = Objects.requireNonNull(project.getId());
        int draftProjectVersion = project.getLastProjectVersion();

        ProjectCodeWorkflow latestProjectCodeWorkflow = projectCodeWorkflowService.fetchProjectCodeWorkflow(projectId)
            .orElse(null);

        CodeWorkflowReconciliation reconciliation;

        if (latestProjectCodeWorkflow != null && latestProjectCodeWorkflow.getProjectVersion() == draftProjectVersion) {
            CodeWorkflowContainer draftCodeWorkflowContainer = codeWorkflowContainerService.getCodeWorkflowContainer(
                latestProjectCodeWorkflow.getCodeWorkflowContainerId());

            if (draftCodeWorkflowContainer.getLanguage() == language) {
                reconciliation = codeWorkflowContainerFacade.update(
                    draftCodeWorkflowContainer, projectDefinition.getVersion(), projectDefinition.getWorkflows(), bytes,
                    PlatformType.AUTOMATION);
            } else {
                reconciliation = createDraftContainer(
                    project, projectDefinition, bytes, language, draftCodeWorkflowContainer.getWorkflowNameIds());
            }
        } else {
            Map<String, String> reusableWorkflowNameIds = latestProjectCodeWorkflow == null
                ? Map.of()
                : resolveDraftWorkflowNameIds(latestProjectCodeWorkflow, projectId, draftProjectVersion);

            reconciliation = createDraftContainer(
                project, projectDefinition, bytes, language, reusableWorkflowNameIds);
        }

        Map<String, UUID> publishedWorkflowUuidsByName = getPublishedWorkflowUuidsByName(projectId, draftProjectVersion);

        for (Map.Entry<String, String> entry : reconciliation.addedWorkflowNameIds()
            .entrySet()) {

            ProjectWorkflow projectWorkflow = projectWorkflowService.addWorkflow(
                projectId, draftProjectVersion, entry.getValue());

            UUID publishedUuid = publishedWorkflowUuidsByName.get(entry.getKey());

            if (publishedUuid != null) {
                projectWorkflow.setUuid(publishedUuid);

                projectWorkflowService.update(projectWorkflow);
            }
        }

        for (String workflowId : reconciliation.removedWorkflowNameIds()
            .values()) {

            projectWorkflowService.delete(projectId, draftProjectVersion, workflowId);

            for (WorkflowPreDeleteListener workflowPreDeleteListener : workflowPreDeleteListeners) {
                workflowPreDeleteListener.onWorkflowPreDelete(workflowId);
            }

            workflowService.delete(workflowId);
        }
    }

    private CodeWorkflowReconciliation createDraftContainer(
        Project project, ProjectDefinition projectDefinition, byte[] bytes, Language language,
        Map<String, String> reusableWorkflowNameIds) {

        CodeWorkflowReconciliation reconciliation = codeWorkflowContainerFacade.create(
            projectDefinition.getName(), projectDefinition.getVersion(), projectDefinition.getWorkflows(), language,
            bytes, PlatformType.AUTOMATION, reusableWorkflowNameIds);

        projectCodeWorkflowService.create(reconciliation.codeWorkflowContainer(), project);

        return reconciliation;
    }

    private Map<String, UUID> getPublishedWorkflowUuidsByName(long projectId, int draftProjectVersion) {
        Optional<ProjectCodeWorkflow> publishedProjectCodeWorkflow =
            projectCodeWorkflowService.fetchPublishedProjectCodeWorkflow(projectId, draftProjectVersion);

        if (publishedProjectCodeWorkflow.isEmpty()) {
            return Map.of();
        }

        ProjectCodeWorkflow projectCodeWorkflow = publishedProjectCodeWorkflow.get();

        Map<String, String> publishedWorkflowNameIds = codeWorkflowContainerService
            .getCodeWorkflowContainer(projectCodeWorkflow.getCodeWorkflowContainerId())
            .getWorkflowNameIds();

        Map<String, UUID> workflowIdUuids = projectWorkflowService.getProjectWorkflows(
            projectId, projectCodeWorkflow.getProjectVersion())
            .stream()
            .collect(Collectors.toMap(ProjectWorkflow::getWorkflowId, ProjectWorkflow::getUuid));

        Map<String, UUID> publishedWorkflowUuidsByName = new HashMap<>();

        for (Map.Entry<String, String> entry : publishedWorkflowNameIds.entrySet()) {
            UUID uuid = workflowIdUuids.get(entry.getValue());

            if (uuid != null) {
                publishedWorkflowUuidsByName.put(entry.getKey(), uuid);
            }
        }

        return publishedWorkflowUuidsByName;
    }

    // resolveDraftWorkflowNameIds: moved verbatim from ProjectCodeWorkflowFacadeImpl.
}
```

Replace the trailing comment with the moved `resolveDraftWorkflowNameIds` method body. Use the accessor
`ProjectWorkflow` actually exposes for the uuid (`getUuid()` returning `UUID`; if only `getUuidAsString()` exists, map
with `UUID.fromString`).

- [ ] **Step 7: Run the tests to see them pass**

Same command as Step 5. Expected: exit 0, 6 tests passing (`grep -o 'tests="[0-9]*"' build/test-results/...`).

- [ ] **Step 8: Commit**

```bash
./gradlew --configure-on-demand :server:ee:libs:automation:automation-configuration:automation-configuration-service:spotlessApply :server:ee:libs:automation:automation-configuration:automation-configuration-api:spotlessApply
git add server/ee/libs/automation/automation-configuration
git commit -m "Add a shared code workflow draft service that keeps published workflow uuids by name"
```

---

### Task 2: Automation facade delegates to the draft service

**Files:**
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/facade/ProjectCodeWorkflowFacadeImpl.java`
- Modify: every test constructing `new ProjectCodeWorkflowFacadeImpl(` (10 sites; find them with
  `git grep -n "new ProjectCodeWorkflowFacadeImpl(" -- server`)

**Interfaces:**
- Consumes: `ProjectCodeWorkflowDraftService`, `ProjectCodeWorkflowDraftServiceImpl` constructor (Task 1).
- Produces: `ProjectCodeWorkflowFacadeImpl(ApplicationProperties, CacheManager, ProjectService, ProjectWorkflowService, CodeWorkflowContainerFacade, ProjectCodeWorkflowService, CodeWorkflowContainerService, CodeWorkflowFileStorage, ProjectCodeWorkflowDraftService, TagService)`.

- [ ] **Step 1: Refactor the facade**

- Replace the constructor parameters `WorkflowService workflowService, List<WorkflowPreDeleteListener> workflowPreDeleteListeners`
  with `ProjectCodeWorkflowDraftService projectCodeWorkflowDraftService` (keep the rest in order; put the new one
  before `TagService`), and remove the now-unused fields.
- Delete the private `saveDraft` and `resolveDraftWorkflowNameIds` methods.
- Replace each `saveDraft(project, projectDefinition, bytes, language)` call with
  `projectCodeWorkflowDraftService.saveDraft(project, projectDefinition, bytes, language)`.
- Remove imports that become unused.

- [ ] **Step 2: Update the test helpers**

In each test helper that constructs the facade, build the real draft service from the same mocks and pass it:

```java
        ProjectCodeWorkflowDraftService projectCodeWorkflowDraftService = new ProjectCodeWorkflowDraftServiceImpl(
            codeWorkflowContainerFacade, codeWorkflowContainerService, projectCodeWorkflowService,
            projectWorkflowService, workflowService, List.of());

        return new ProjectCodeWorkflowFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), projectService, projectWorkflowService,
            codeWorkflowContainerFacade, projectCodeWorkflowService, codeWorkflowContainerService,
            codeWorkflowFileStorage, projectCodeWorkflowDraftService, mock(TagService.class));
```

Where a helper has no `workflowService` mock, pass `mock(WorkflowService.class)`. `ProjectCodeWorkflowFacadeIntTest`
autowires the facade; add `ProjectCodeWorkflowDraftService.class` to its `@MockitoBean(types = {...})` list.

- [ ] **Step 3: Run the existing facade tests**

```bash
./gradlew --configure-on-demand :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests '*ProjectCodeWorkflowFacade*' > /tmp/claude-502/t2.log 2>&1; echo $?
grep -E "^> Task .* FAILED| FAILED$" /tmp/claude-502/t2.log
```

Expected: exit 0. These tests are the regression net; do not change their assertions.

- [ ] **Step 4: Commit**

```bash
./gradlew --configure-on-demand :server:ee:libs:automation:automation-configuration:automation-configuration-service:spotlessApply
git add server/ee/libs/automation/automation-configuration/automation-configuration-service
git commit -m "Save automation code workflow drafts through the shared draft service"
```

---

### Task 3: Catalog upload writes the draft, publish is guarded

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/exception/CodeWorkflowErrorType.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectCodeWorkflowFacadeImpl.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectFacadeImpl.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectCodeWorkflowFacadeTest.java`
- Modify: every embedded test config that defines a `CodeWorkflowContainerFacade` mock bean (find with
  `git grep -ln "CodeWorkflowContainerFacade codeWorkflowContainerFacade()" -- server/ee/libs/embedded`)
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectFacadeTest.java`
  (create if absent; if a `AutomationWorkflowProjectFacadeIntTest` exists, put the guard test there instead)

**Interfaces:**
- Consumes: `ProjectCodeWorkflowDraftService.saveDraft` (Task 1).
- Produces: `CodeWorkflowErrorType.CODE_WORKFLOW_DRAFT_EMPTY` (embedded, key 106); constructor
  `AutomationWorkflowProjectCodeWorkflowFacadeImpl(ApplicationProperties, AutomationWorkflowProjectFacade, CacheManager, CodeWorkflowContainerService, CodeWorkflowFileStorage, ProjectCodeWorkflowDraftService, ProjectCodeWorkflowService, ProjectService)`.

- [ ] **Step 1: Add the error type**

```java
    public static final CodeWorkflowErrorType CODE_WORKFLOW_DRAFT_EMPTY = new CodeWorkflowErrorType(106);
```

- [ ] **Step 2: Write the failing tests**

In `AutomationWorkflowProjectCodeWorkflowFacadeTest`, replace the existing upload test(s) that assert a fresh container,
uuid copying and a direct `projectService.publishProject` with:

```java
    @Test
    void testSaveWritesTheDraftThenPublishesThroughTheCatalog() {
        when(automationWorkflowProjectFacade.fetchProjectIdByName("my-project")).thenReturn(Optional.of(7L));

        Project project = new Project();

        project.setId(7L);

        when(projectService.getProject(7L)).thenReturn(project);

        List<String> warnings = facade().save(jsProject("my-project"), Language.JAVASCRIPT);

        InOrder inOrder = inOrder(projectCodeWorkflowDraftService, automationWorkflowProjectFacade);

        inOrder.verify(projectCodeWorkflowDraftService)
            .saveDraft(eq(project), any(ProjectDefinition.class), any(), eq(Language.JAVASCRIPT));
        inOrder.verify(automationWorkflowProjectFacade).publishProject(7L);

        assertThat(warnings).isNotEmpty();
    }
```

`jsProject(name)` returns the bytes of a JavaScript source declaring one workflow without a request trigger (copy the
existing test's source literal). `facade()` builds the facade with the Task 3 constructor; keep the test's existing
mocks and add `ProjectCodeWorkflowDraftService projectCodeWorkflowDraftService = mock(...)`.

For the guard, in the catalog facade test:

```java
    @Test
    void testPublishingACodeProjectWithAnEmptyDraftIsRefused() {
        // project 7 is a marked catalog project at draft version 2; its latest code workflow row is at version 1
        ProjectCodeWorkflow publishedProjectCodeWorkflow = mock(ProjectCodeWorkflow.class);

        when(publishedProjectCodeWorkflow.getProjectVersion()).thenReturn(1);
        when(projectCodeWorkflowService.fetchProjectCodeWorkflow(7L))
            .thenReturn(Optional.of(publishedProjectCodeWorkflow));

        assertThatThrownBy(() -> automationWorkflowProjectFacade.publishProject(7L))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(CodeWorkflowErrorType.CODE_WORKFLOW_DRAFT_EMPTY.getErrorKey()));

        verify(projectService, never()).publishProject(anyLong(), any(), anyBoolean());
    }
```

Stub `projectService.getProject(7L)` to a project whose `getLastProjectVersion()` is 2, and
`automationWorkflowProjectService.fetchAutomationWorkflowProject(7L)` to a present value, so `getMarkedProject` passes.
Use the accessor `ConfigurationException`/`AbstractErrorType` actually expose for the error key.

- [ ] **Step 3: Run them to see them fail**

```bash
./gradlew --configure-on-demand :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests '*AutomationWorkflowProjectCodeWorkflowFacadeTest*' --tests '*AutomationWorkflowProjectFacadeTest*' > /tmp/claude-502/t3.log 2>&1; echo $?
```

Expected: compilation failure or assertion failures.

- [ ] **Step 4: Implement**

`AutomationWorkflowProjectFacadeImpl.publishProject` — right after `getMarkedProject` and reading
`oldProjectVersion`:

```java
        Optional<ProjectCodeWorkflow> latestProjectCodeWorkflow =
            projectCodeWorkflowService.fetchProjectCodeWorkflow(projectId);

        if (latestProjectCodeWorkflow.isPresent()
            && latestProjectCodeWorkflow.get()
                .getProjectVersion() != oldProjectVersion) {

            throw new ConfigurationException(
                "The code project has no draft source to publish; edit or upload it first",
                CodeWorkflowErrorType.CODE_WORKFLOW_DRAFT_EMPTY);
        }
```

`AutomationWorkflowProjectCodeWorkflowFacadeImpl.save` — after resolving `projectId`, replace everything from
`fetchPreviousWorkflowUuidsByName` through the `applicationEventPublisher.publishEvent(...)` line with:

```java
        Project project = projectService.getProject(projectId);

        projectCodeWorkflowDraftService.saveDraft(project, projectDefinition, bytes, language);

        automationWorkflowProjectFacade.publishProject(projectId);
```

Delete `fetchPreviousWorkflowUuidsByName`, and the fields `applicationEventPublisher`, `codeWorkflowContainerFacade`,
`projectWorkflowService` (the catalog `publishProject` fires the event). Add fields `CodeWorkflowContainerService`,
`CodeWorkflowFileStorage` (for Task 4) and `ProjectCodeWorkflowDraftService`, matching the constructor above.

Embedded test configs: next to each `CodeWorkflowContainerFacade` mock bean add

```java
    @Bean
    ProjectCodeWorkflowDraftService projectCodeWorkflowDraftService() {
        return mock(ProjectCodeWorkflowDraftService.class);
    }
```

- [ ] **Step 5: Run the embedded unit and integration tests**

```bash
./gradlew --configure-on-demand --continue :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:testIntegration > /tmp/claude-502/t3b.log 2>&1; echo $?
grep -E "^> Task .* FAILED| FAILED$" /tmp/claude-502/t3b.log
```

Expected: exit 0. A failure counts as pre-existing only if it also fails in a scratch worktree at `HEAD~1` (never use
`git stash` in this repository).

- [ ] **Step 6: Commit**

```bash
./gradlew --configure-on-demand :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:spotlessApply
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-service
git commit -m "Upload catalog code projects into the draft and refuse publishing an empty code draft"
```

---

### Task 4: Create and edit catalog code projects

**Files:**
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/dto/AutomationWorkflowProjectCodeWorkflowSourceDTO.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectCodeWorkflowFacade.java`
- Modify: `.../embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectCodeWorkflowFacadeImpl.java`
- Create: `.../embedded-configuration-service/src/main/resources/automation-workflow-project-code-workflow-templates/starter.js`
  and `starter.py` (copies of `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/resources/code-workflow-templates/starter.js` / `.py`)
- Test: `.../embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectCodeWorkflowFacadeTest.java`

**Interfaces:**
- Produces:
  `record AutomationWorkflowProjectCodeWorkflowSourceDTO(String content, Language language)`;
  `long createEmptyCodeWorkflow(String name, Language language, @Nullable String description, @Nullable String category, @Nullable List<String> tags, @Nullable String permissionExpression, @Nullable Boolean automationHubVisible)`;
  `AutomationWorkflowProjectCodeWorkflowSourceDTO getCodeWorkflowSource(long automationWorkflowProjectId)`;
  `void updateCodeWorkflowSource(long automationWorkflowProjectId, String content)`.

- [ ] **Step 1: Write the failing tests**

Add to `AutomationWorkflowProjectCodeWorkflowFacadeTest`:

```java
    @Test
    void testCreateEmptyCodeWorkflowCreatesAnUnpublishedCatalogDraft() {
        when(automationWorkflowProjectFacade.fetchProjectIdByName("orders")).thenReturn(Optional.empty());
        when(automationWorkflowProjectFacade.createProject(
            eq("orders"), any(), eq("Sales"), eq(List.of("crm")), eq("true"), eq(true))).thenReturn(7L);

        Project project = new Project();

        project.setId(7L);

        when(projectService.getProject(7L)).thenReturn(project);

        long id = facade().createEmptyCodeWorkflow(
            "orders", Language.JAVASCRIPT, null, "Sales", List.of("crm"), "true", true);

        assertThat(id).isEqualTo(7L);

        verify(projectCodeWorkflowDraftService)
            .saveDraft(eq(project), any(ProjectDefinition.class), any(), eq(Language.JAVASCRIPT));
        verify(automationWorkflowProjectFacade, never()).publishProject(anyLong());
    }

    @Test
    void testCreateEmptyCodeWorkflowRejectsAnExistingName() {
        when(automationWorkflowProjectFacade.fetchProjectIdByName("orders")).thenReturn(Optional.of(7L));

        assertErrorKey(
            () -> facade().createEmptyCodeWorkflow("orders", Language.JAVASCRIPT, null, null, null, null, null),
            CodeWorkflowErrorType.CODE_WORKFLOW_ALREADY_EXISTS);
    }

    @Test
    void testCreateEmptyCodeWorkflowRejectsTheReservedPrefix() {
        assertErrorKey(
            () -> facade().createEmptyCodeWorkflow(
                "__EMBEDDED_AUTOMATION__x", Language.JAVASCRIPT, null, null, null, null, null),
            CodeWorkflowErrorType.INVALID_CODE_WORKFLOW_NAME);
    }

    @Test
    void testCreateEmptyCodeWorkflowRejectsJava() {
        assertErrorKey(
            () -> facade().createEmptyCodeWorkflow("orders", Language.JAVA, null, null, null, null, null),
            CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED);
    }

    @Test
    void testUpdateCodeWorkflowSourceSavesADraftWithoutPublishing() {
        stubCatalogCodeProject(7L, "orders", Language.JAVASCRIPT);

        facade().updateCodeWorkflowSource(7L, jsSource("orders"));

        verify(projectCodeWorkflowDraftService)
            .saveDraft(any(Project.class), any(ProjectDefinition.class), any(), eq(Language.JAVASCRIPT));
        verify(automationWorkflowProjectFacade, never()).publishProject(anyLong());
    }

    @Test
    void testUpdateCodeWorkflowSourceRejectsARename() {
        stubCatalogCodeProject(7L, "orders", Language.JAVASCRIPT);

        assertErrorKey(
            () -> facade().updateCodeWorkflowSource(7L, jsSource("renamed")),
            CodeWorkflowErrorType.CODE_WORKFLOW_NAME_MISMATCH);

        verify(projectCodeWorkflowDraftService, never()).saveDraft(any(), any(), any(), any());
    }

    @Test
    void testUpdateCodeWorkflowSourceRejectsAnUnloadableSource() {
        stubCatalogCodeProject(7L, "orders", Language.JAVASCRIPT);

        assertErrorKey(
            () -> facade().updateCodeWorkflowSource(7L, "this is not javascript ("),
            CodeWorkflowErrorType.SOURCE_LOAD_FAILED);
    }

    @Test
    void testCodeWorkflowSourceOfAVisualCatalogProjectFailsCleanly() {
        when(automationWorkflowProjectFacade.getProject(8L)).thenReturn(mock(AutomationWorkflowProjectDTO.class));
        when(projectCodeWorkflowService.fetchProjectCodeWorkflow(8L)).thenReturn(Optional.empty());

        assertErrorKey(
            () -> facade().getCodeWorkflowSource(8L), CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED);
        assertErrorKey(
            () -> facade().updateCodeWorkflowSource(8L, jsSource("x")), CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED);
    }

    @Test
    void testGetCodeWorkflowSourceReturnsContentAndLanguage() {
        stubCatalogCodeProject(7L, "orders", Language.PYTHON);

        when(codeWorkflowFileStorage.readCodeWorkflowFileContent(any())).thenReturn("source");

        assertThat(facade().getCodeWorkflowSource(7L))
            .isEqualTo(new AutomationWorkflowProjectCodeWorkflowSourceDTO("source", Language.PYTHON));
    }
```

Helpers to add to the test class:

```java
    private void stubCatalogCodeProject(long projectId, String displayName, Language language) {
        when(automationWorkflowProjectFacade.getProject(projectId)).thenReturn(mock(AutomationWorkflowProjectDTO.class));

        Project project = new Project();

        project.setId(projectId);
        project.setName(SystemProjects.EMBEDDED_AUTOMATION_NAME_PREFIX + displayName);

        when(projectService.getProject(projectId)).thenReturn(project);

        ProjectCodeWorkflow projectCodeWorkflow = mock(ProjectCodeWorkflow.class);

        when(projectCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(50L);
        when(projectCodeWorkflowService.fetchProjectCodeWorkflow(projectId))
            .thenReturn(Optional.of(projectCodeWorkflow));

        CodeWorkflowContainer codeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(codeWorkflowContainer.getLanguage()).thenReturn(language);
        when(codeWorkflowContainerService.getCodeWorkflowContainer(50L)).thenReturn(codeWorkflowContainer);
    }

    private static void assertErrorKey(ThrowableAssert.ThrowingCallable callable, CodeWorkflowErrorType errorType) {
        assertThatThrownBy(callable)
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(errorType.getErrorKey()));
    }

    private static String jsSource(String name) {
        return "({name: \"" + name + "\", version: \"1\", workflows: [{name: \"my-workflow\", "
            + "label: \"My Workflow\", tasks: [{name: \"my-task\", label: \"My Task\", "
            + "perform: function() { return \"hello\"; }}]}]})";
    }
```

`jsSource` loads through the real polyglot loader, as `ProjectCodeWorkflowFacadeDraftTest.jsScript` does.

Pin the admin-only guards:

```java
    @ParameterizedTest
    @ValueSource(strings = {"createEmptyCodeWorkflow", "getCodeWorkflowSource", "updateCodeWorkflowSource", "save"})
    void testCodeWorkflowMethodsAreAdminOnly(String methodName) {
        Method method = Arrays.stream(AutomationWorkflowProjectCodeWorkflowFacadeImpl.class.getDeclaredMethods())
            .filter(declaredMethod -> declaredMethod.getName()
                .equals(methodName))
            .findFirst()
            .orElseThrow();

        assertThat(method.getAnnotation(PreAuthorize.class))
            .extracting(PreAuthorize::value)
            .isEqualTo("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")");
    }
```

- [ ] **Step 2: Run them to see them fail**

```bash
./gradlew --configure-on-demand :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests '*AutomationWorkflowProjectCodeWorkflowFacadeTest*' > /tmp/claude-502/t4.log 2>&1; echo $?
```

Expected: compilation failure (methods missing).

- [ ] **Step 3: Implement**

DTO (embedded-configuration-api, `dto` package):

```java
public record AutomationWorkflowProjectCodeWorkflowSourceDTO(String content, Language language) {
}
```

Interface — add:

```java
    long createEmptyCodeWorkflow(
        String name, Language language, @Nullable String description, @Nullable String category,
        @Nullable List<String> tags, @Nullable String permissionExpression, @Nullable Boolean automationHubVisible);

    AutomationWorkflowProjectCodeWorkflowSourceDTO getCodeWorkflowSource(long automationWorkflowProjectId);

    void updateCodeWorkflowSource(long automationWorkflowProjectId, String content);
```

Implementation (all three `@PreAuthorize` admin; `getCodeWorkflowSource` also `@Transactional(readOnly = true)`):

```java
    private static final String MARKER = SystemProjects.EMBEDDED_AUTOMATION_NAME_PREFIX;

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public long createEmptyCodeWorkflow(
        String name, Language language, @Nullable String description, @Nullable String category,
        @Nullable List<String> tags, @Nullable String permissionExpression, @Nullable Boolean automationHubVisible) {

        validateName(name);
        validateEditableLanguage(language);

        if (automationWorkflowProjectFacade.fetchProjectIdByName(name)
            .isPresent()) {

            throw new ConfigurationException(
                "A catalog project named '" + name + "' already exists",
                CodeWorkflowErrorType.CODE_WORKFLOW_ALREADY_EXISTS);
        }

        byte[] bytes = readTemplate(language).replace("__NAME__", name)
            .getBytes(StandardCharsets.UTF_8);

        ProjectDefinition projectDefinition = loadProjectDefinition(language, bytes);

        long projectId = automationWorkflowProjectFacade.createProject(
            name,
            description == null || description.isBlank() ? projectDefinition.getDescription()
                .orElse(null) : description,
            category, tags == null ? List.of() : tags, permissionExpression, automationHubVisible);

        projectCodeWorkflowDraftService.saveDraft(
            projectService.getProject(projectId), projectDefinition, bytes, language);

        return projectId;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AutomationWorkflowProjectCodeWorkflowSourceDTO getCodeWorkflowSource(long automationWorkflowProjectId) {
        CodeWorkflowContainer codeWorkflowContainer = getEditableCodeWorkflowContainer(automationWorkflowProjectId);

        return new AutomationWorkflowProjectCodeWorkflowSourceDTO(
            codeWorkflowFileStorage.readCodeWorkflowFileContent(codeWorkflowContainer.getWorkflows()),
            codeWorkflowContainer.getLanguage());
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void updateCodeWorkflowSource(long automationWorkflowProjectId, String content) {
        CodeWorkflowContainer codeWorkflowContainer = getEditableCodeWorkflowContainer(automationWorkflowProjectId);

        Language language = codeWorkflowContainer.getLanguage();
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        ProjectDefinition projectDefinition;

        try {
            projectDefinition = loadProjectDefinition(language, bytes);
        } catch (RuntimeException exception) {
            throw new ConfigurationException(
                "Failed to load code workflow source: " + exception.getMessage(),
                CodeWorkflowErrorType.SOURCE_LOAD_FAILED);
        }

        Project project = projectService.getProject(automationWorkflowProjectId);

        String expectedName = project.getName()
            .substring(MARKER.length());

        if (!Objects.equals(projectDefinition.getName(), expectedName)) {
            throw new ConfigurationException(
                "Renaming a code project by editing its source is not supported (expected name '" + expectedName +
                    "')",
                CodeWorkflowErrorType.CODE_WORKFLOW_NAME_MISMATCH);
        }

        projectCodeWorkflowDraftService.saveDraft(project, projectDefinition, bytes, language);
    }

    private CodeWorkflowContainer getEditableCodeWorkflowContainer(long automationWorkflowProjectId) {
        automationWorkflowProjectFacade.getProject(automationWorkflowProjectId);

        ProjectCodeWorkflow projectCodeWorkflow = projectCodeWorkflowService
            .fetchProjectCodeWorkflow(automationWorkflowProjectId)
            .orElseThrow(() -> new ConfigurationException(
                "Catalog project " + automationWorkflowProjectId + " is not a code project",
                CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED));

        CodeWorkflowContainer codeWorkflowContainer = codeWorkflowContainerService.getCodeWorkflowContainer(
            projectCodeWorkflow.getCodeWorkflowContainerId());

        validateEditableLanguage(codeWorkflowContainer.getLanguage());

        return codeWorkflowContainer;
    }

    private static void validateEditableLanguage(Language language) {
        if (language != Language.JAVASCRIPT && language != Language.PYTHON) {
            throw new ConfigurationException(
                "Only JavaScript and Python code projects can be created and edited in the app",
                CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED);
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.startsWith("__EMBEDDED") || name.indexOf('"') >= 0 ||
            name.indexOf('\\') >= 0 || name.indexOf('\n') >= 0 || name.indexOf('\r') >= 0) {

            throw new ConfigurationException(
                "Invalid code project name: must not be blank, start with '__EMBEDDED', or contain quotes, " +
                    "backslashes, or newlines",
                CodeWorkflowErrorType.INVALID_CODE_WORKFLOW_NAME);
        }
    }

    private static String readTemplate(Language language) {
        String resource = "automation-workflow-project-code-workflow-templates/starter." + language.getExtension();

        try (InputStream inputStream = AutomationWorkflowProjectCodeWorkflowFacadeImpl.class.getClassLoader()
            .getResourceAsStream(resource)) {

            if (inputStream == null) {
                throw new IllegalStateException("Missing starter template: " + resource);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
```

Check how `ProjectCodeWorkflowFacadeImpl.readTemplate` concatenates `language.getExtension()` (with or without a dot) and
match it. If `loadProjectDefinition` in this facade throws a checked exception, keep its existing wrapping and catch
what it actually throws.

- [ ] **Step 4: Run the tests to see them pass**

Same command as Step 2. Expected: exit 0.

- [ ] **Step 5: Commit**

```bash
./gradlew --configure-on-demand :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:spotlessApply :server:ee:libs:embedded:embedded-configuration:embedded-configuration-api:spotlessApply
git add server/ee/libs/embedded/embedded-configuration
git commit -m "Create and edit embedded catalog code projects as drafts"
```

---

### Task 5: GraphQL operations

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql/src/main/resources/graphql/code-workflow.graphqls`
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql/src/main/java/com/bytechef/ee/embedded/configuration/web/graphql/AutomationWorkflowProjectCodeWorkflowGraphQlController.java`
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql/src/test/java/com/bytechef/ee/embedded/configuration/web/graphql/AutomationWorkflowProjectCodeWorkflowGraphQlControllerIntTest.java`

**Interfaces:**
- Consumes: the three facade methods and the DTO (Task 4).
- Produces: GraphQL `createAutomationWorkflowProjectCodeWorkflow`, `updateAutomationWorkflowProjectCodeWorkflowSource`,
  `automationWorkflowProjectCodeWorkflowSource` (shapes below).

- [ ] **Step 1: Schema**

Append to `code-workflow.graphqls`:

```graphql
extend type Query {
    automationWorkflowProjectCodeWorkflowSource(automationWorkflowProjectId: ID!): AutomationWorkflowProjectCodeWorkflowSource!
}

extend type Mutation {
    createAutomationWorkflowProjectCodeWorkflow(name: String!, language: CodeWorkflowLanguage!, description: String, category: String, tags: [String!], permissionExpression: String, automationHubVisible: Boolean): ID!
    updateAutomationWorkflowProjectCodeWorkflowSource(automationWorkflowProjectId: ID!, content: String!): Boolean!
}

type AutomationWorkflowProjectCodeWorkflowSource {
    content: String!
    language: CodeWorkflowLanguage!
}
```

- [ ] **Step 2: Write the failing IntTest**

Copy `IntegrationCodeWorkflowGraphQlControllerIntTest`'s annotations, replacing the controller class:

```java
    @MockitoBean
    private AutomationWorkflowProjectCodeWorkflowFacade automationWorkflowProjectCodeWorkflowFacade;

    @Test
    void testAutomationWorkflowProjectCodeWorkflowSource() {
        when(automationWorkflowProjectCodeWorkflowFacade.getCodeWorkflowSource(7L))
            .thenReturn(new AutomationWorkflowProjectCodeWorkflowSourceDTO("print('hi')", Language.PYTHON));

        graphQlTester
            .document("query { automationWorkflowProjectCodeWorkflowSource(automationWorkflowProjectId: 7) " +
                "{ content language } }")
            .execute()
            .path("automationWorkflowProjectCodeWorkflowSource.content")
            .entity(String.class)
            .isEqualTo("print('hi')")
            .path("automationWorkflowProjectCodeWorkflowSource.language")
            .entity(String.class)
            .isEqualTo("PYTHON");
    }

    @Test
    void testUpdateAutomationWorkflowProjectCodeWorkflowSource() {
        graphQlTester
            .document("mutation { updateAutomationWorkflowProjectCodeWorkflowSource(automationWorkflowProjectId: 7, " +
                "content: \"x\") }")
            .execute()
            .path("updateAutomationWorkflowProjectCodeWorkflowSource")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationWorkflowProjectCodeWorkflowFacade).updateCodeWorkflowSource(7L, "x");
    }

    @Test
    void testCreateAutomationWorkflowProjectCodeWorkflow() {
        when(automationWorkflowProjectCodeWorkflowFacade.createEmptyCodeWorkflow(
            eq("orders"), eq(Language.JAVASCRIPT), isNull(), eq("Sales"), eq(List.of("crm")), isNull(), isNull()))
                .thenReturn(7L);

        graphQlTester
            .document("mutation { createAutomationWorkflowProjectCodeWorkflow(name: \"orders\", language: JAVASCRIPT, " +
                "category: \"Sales\", tags: [\"crm\"]) }")
            .execute()
            .path("createAutomationWorkflowProjectCodeWorkflow")
            .entity(String.class)
            .isEqualTo("7");
    }
```

- [ ] **Step 3: Run it to see it fail**

```bash
./gradlew --configure-on-demand :server:ee:libs:embedded:embedded-configuration:embedded-configuration-graphql:testIntegration --tests '*AutomationWorkflowProjectCodeWorkflowGraphQlControllerIntTest*' > /tmp/claude-502/t5.log 2>&1; echo $?
```

Expected: failure (controller missing).

- [ ] **Step 4: Implement the controller**

```java
@Controller
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AutomationWorkflowProjectCodeWorkflowGraphQlController {

    private final AutomationWorkflowProjectCodeWorkflowFacade automationWorkflowProjectCodeWorkflowFacade;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectCodeWorkflowGraphQlController(
        AutomationWorkflowProjectCodeWorkflowFacade automationWorkflowProjectCodeWorkflowFacade) {

        this.automationWorkflowProjectCodeWorkflowFacade = automationWorkflowProjectCodeWorkflowFacade;
    }

    @QueryMapping
    public AutomationWorkflowProjectCodeWorkflowSourceDTO automationWorkflowProjectCodeWorkflowSource(
        @Argument long automationWorkflowProjectId) {

        return automationWorkflowProjectCodeWorkflowFacade.getCodeWorkflowSource(automationWorkflowProjectId);
    }

    @MutationMapping
    public String createAutomationWorkflowProjectCodeWorkflow(
        @Argument String name, @Argument Language language, @Argument String description, @Argument String category,
        @Argument List<String> tags, @Argument String permissionExpression, @Argument Boolean automationHubVisible) {

        return String.valueOf(
            automationWorkflowProjectCodeWorkflowFacade.createEmptyCodeWorkflow(
                name, language, description, category, tags, permissionExpression, automationHubVisible));
    }

    @MutationMapping
    public boolean updateAutomationWorkflowProjectCodeWorkflowSource(
        @Argument long automationWorkflowProjectId, @Argument String content) {

        automationWorkflowProjectCodeWorkflowFacade.updateCodeWorkflowSource(automationWorkflowProjectId, content);

        return true;
    }
}
```

- [ ] **Step 5: Run the module's IntTests**

```bash
./gradlew --configure-on-demand :server:ee:libs:embedded:embedded-configuration:embedded-configuration-graphql:testIntegration > /tmp/claude-502/t5b.log 2>&1; echo $?
grep -E " FAILED$" /tmp/claude-502/t5b.log
```

Expected: exit 0. If other controller IntTests in the module fail with a missing
`AutomationWorkflowProjectCodeWorkflowFacade` bean, add that type to `EmbeddedConfigurationGraphQlConfigurationSharedMocks`.

- [ ] **Step 6: Commit**

```bash
./gradlew --configure-on-demand :server:ee:libs:embedded:embedded-configuration:embedded-configuration-graphql:spotlessApply
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql
git commit -m "Expose catalog code project create, source and update over GraphQL"
```

---

### Task 6: Client — operations, New Code Project dialog and menu

**Files:**
- Create: `client/src/graphql/embedded/configuration/automationWorkflowProjectCodeWorkflow.graphql`
- Regenerate: `client/src/shared/middleware/graphql.ts`
- Modify: `client/src/ee/pages/embedded/automation-workflows/components/automation-workflow-project-dialog/AutomationWorkflowProjectDialog.tsx`
- Modify: `client/src/ee/pages/embedded/automation-workflows/AutomationWorkflows.tsx`
- Test: `client/src/ee/pages/embedded/automation-workflows/tests/AutomationWorkflows.test.tsx`,
  `client/src/ee/pages/embedded/automation-workflows/components/automation-workflow-project-dialog/tests/AutomationWorkflowProjectDialog.test.tsx`

**Interfaces:**
- Consumes: GraphQL operations from Task 5.
- Produces: hooks `useCreateAutomationWorkflowProjectCodeWorkflowMutation`,
  `useAutomationWorkflowProjectCodeWorkflowSourceQuery`, `useUpdateAutomationWorkflowProjectCodeWorkflowSourceMutation`;
  `AutomationWorkflowProjectFormValuesI.language?: CodeWorkflowLanguage`; dialog prop `codeWorkflow?: boolean`;
  route path string `/embedded/automation-workflows/projects/${id}/code` (Task 7 registers it).

- [ ] **Step 1: Operations and codegen**

```graphql
query automationWorkflowProjectCodeWorkflowSource($automationWorkflowProjectId: ID!) {
    automationWorkflowProjectCodeWorkflowSource(automationWorkflowProjectId: $automationWorkflowProjectId) {
        content
        language
    }
}

mutation createAutomationWorkflowProjectCodeWorkflow(
    $name: String!
    $language: CodeWorkflowLanguage!
    $description: String
    $category: String
    $tags: [String!]
    $permissionExpression: String
    $automationHubVisible: Boolean
) {
    createAutomationWorkflowProjectCodeWorkflow(
        name: $name
        language: $language
        description: $description
        category: $category
        tags: $tags
        permissionExpression: $permissionExpression
        automationHubVisible: $automationHubVisible
    )
}

mutation updateAutomationWorkflowProjectCodeWorkflowSource($automationWorkflowProjectId: ID!, $content: String!) {
    updateAutomationWorkflowProjectCodeWorkflowSource(
        automationWorkflowProjectId: $automationWorkflowProjectId
        content: $content
    )
}
```

```bash
cd client && npx graphql-codegen
git add src/graphql/embedded/configuration/automationWorkflowProjectCodeWorkflow.graphql
git commit -m "client - Add the catalog code project GraphQL operations"
git add src/shared/middleware/graphql.ts
git commit -m "client - Regenerate the GraphQL client"
```

- [ ] **Step 2: Write the failing tests**

`AutomationWorkflowProjectDialog.test.tsx` — add:

```tsx
    it('asks for a language and submits it in code workflow mode', async () => {
        const onSubmit = vi.fn();

        render(<AutomationWorkflowProjectDialog categories={[]} codeWorkflow onClose={vi.fn()} onSubmit={onSubmit} tags={[]} />);

        expect(screen.getByText('New Code Project')).toBeInTheDocument();
        expect(screen.getByText('Language')).toBeInTheDocument();

        await userEvent.type(screen.getByPlaceholderText('My CRM Project'), 'orders');
        await userEvent.click(screen.getByRole('button', {name: /save|create/i}));

        await waitFor(() =>
            expect(onSubmit).toHaveBeenCalledWith(
                expect.objectContaining({language: CodeWorkflowLanguage.Javascript, name: 'orders'})
            )
        );
    });

    it('does not ask for a language for a visual project', () => {
        render(<AutomationWorkflowProjectDialog categories={[]} onClose={vi.fn()} onSubmit={vi.fn()} tags={[]} />);

        expect(screen.queryByText('Language')).not.toBeInTheDocument();
    });
```

`AutomationWorkflows.test.tsx` — change the menu-order test to expect
`['New Project', 'New Code Project', 'Import Code Project']`, and add:

```tsx
    it('creates a code project from the New Code Project menu item and opens its editor', async () => {
        const user = userEvent.setup();

        hoisted.projects = [];
        hoisted.createCodeWorkflowMutate.mockImplementation((_variables, options) =>
            options?.onSuccess?.({createAutomationWorkflowProjectCodeWorkflow: '7'})
        );

        renderWithProviders(<AutomationWorkflows />);

        await user.click(screen.getByRole('button', {name: /more project options/i}));
        await user.click(await screen.findByRole('menuitem', {name: 'New Code Project'}));
        await user.type(await screen.findByPlaceholderText('My CRM Project'), 'orders');
        await user.click(screen.getByRole('button', {name: /save|create/i}));

        await waitFor(() => expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/automation-workflows/projects/7/code'));
    });
```

Extend the file's `vi.hoisted` block with `createCodeWorkflowMutate: vi.fn()` and `navigate: vi.fn()` (keys sorted), mock
`useCreateAutomationWorkflowProjectCodeWorkflowMutation: () => ({isPending: false, mutate: hoisted.createCodeWorkflowMutate})`
in the existing `@/shared/middleware/graphql` mock, and mock `useNavigate` from `react-router-dom` to return
`hoisted.navigate` if the file does not already. Match the dialog's real submit button label.

- [ ] **Step 3: Run them to see them fail**

```bash
cd client && npx vitest run src/ee/pages/embedded/automation-workflows
```

Expected: the new tests fail.

- [ ] **Step 4: Implement**

Dialog:
- Add prop `codeWorkflow?: boolean`; add `language?: CodeWorkflowLanguage` to both form interfaces and default it to
  `CodeWorkflowLanguage.Javascript` when `codeWorkflow` is set.
- When `codeWorkflow` is set, render a Language field before Name (a `Select` with JavaScript and Python, exactly as in
  `client/src/pages/automation/projects/components/NewCodeWorkflowDialog.tsx`), and use the title "New Code Project".
- Include `language` in the values passed to `onSubmit`.

Page:
- `const [showCodeProjectDialog, setShowCodeProjectDialog] = useState(false);` (keep `useState` calls together, sorted).
- `const createCodeWorkflowMutation = useCreateAutomationWorkflowProjectCodeWorkflowMutation();`
- Menu (header and empty state): insert between New Project and Import Code Project:

```tsx
                                        <DropdownMenuItem onClick={() => setShowCodeProjectDialog(true)}>
                                            <CodeIcon className="mr-2 size-4" />
                                            New Code Project
                                        </DropdownMenuItem>
```

- Render:

```tsx
            {showCodeProjectDialog && (
                <AutomationWorkflowProjectDialog
                    categories={categories}
                    codeWorkflow
                    onClose={() => setShowCodeProjectDialog(false)}
                    onSubmit={(values) =>
                        createCodeWorkflowMutation.mutate(
                            {
                                automationHubVisible: values.automationHubVisible,
                                category: values.category || undefined,
                                description: values.description || undefined,
                                language: values.language!,
                                name: values.name,
                                permissionExpression: values.permissionExpression,
                                tags: values.tags,
                            },
                            {
                                onSuccess: (data) => {
                                    invalidateProjects();

                                    setShowCodeProjectDialog(false);

                                    navigate(
                                        `/embedded/automation-workflows/projects/${data.createAutomationWorkflowProjectCodeWorkflow}/code`
                                    );
                                },
                            }
                        )
                    }
                    tags={tags}
                />
            )}
```

  If `values.language!` trips `no-non-null-assertion`, default it in the dialog's submit so the type is non-optional in
  code mode, or fall back with `values.language || CodeWorkflowLanguage.Javascript`.
- Import Code Project dialog description: append " Unpublished edits made in the editor are replaced." to the existing
  description string.

- [ ] **Step 5: Run the tests, lint and typecheck**

```bash
cd client && npx vitest run src/ee/pages/embedded/automation-workflows && npx eslint src/ee/pages/embedded/automation-workflows && npx tsc -p tsconfig.json --noEmit --ignoreDeprecations 6.0
```

Expected: all pass. (`--ignoreDeprecations 6.0` only silences a TypeScript-version `baseUrl` notice from the shared
`node_modules`.)

- [ ] **Step 6: Commit**

```bash
npx prettier --write src/ee/pages/embedded/automation-workflows
git add src/ee/pages/embedded/automation-workflows
git commit -m "client - Create catalog code projects from the New Project menu"
```

---

### Task 7: Client — code editor page and entry points

**Files:**
- Create: `client/src/ee/pages/embedded/automation-workflow-project-code-workflow/AutomationWorkflowProjectCodeWorkflow.tsx`
- Test: `client/src/ee/pages/embedded/automation-workflow-project-code-workflow/AutomationWorkflowProjectCodeWorkflow.test.tsx`
- Modify: `client/src/routes.tsx` (route + lazy import next to `AutomationWorkflow`)
- Modify: `client/src/ee/pages/embedded/automation-workflows/AutomationWorkflows.tsx` (`openWorkflowEditor`)
- Modify: `client/src/ee/pages/embedded/automation-workflows/components/automation-workflow-project-list/AutomationWorkflowProjectListItem.tsx`
  and `AutomationWorkflowProjectList.tsx` (Edit Source)
- Test: `client/src/ee/pages/embedded/automation-workflows/tests/AutomationWorkflowProjectList.test.tsx`

**Interfaces:**
- Consumes: hooks from Task 6; `CodeWorkflowSourceEditor` (headerless mode) and `useCodeWorkflowHeaderStore`
  (`client/src/pages/platform/code-workflow/...`); `usePublishAutomationWorkflowProjectMutation`.
- Produces: route `/embedded/automation-workflows/projects/:automationWorkflowProjectId/code`; list prop
  `onEditProjectSource: (projectId: string) => void`.

- [ ] **Step 1: Write the failing tests**

`AutomationWorkflowProjectCodeWorkflow.test.tsx`:

```tsx
const hoisted = vi.hoisted(() => ({
    publishMutate: vi.fn(),
    updateMutateAsync: vi.fn().mockResolvedValue({}),
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql');

    return {
        ...actual,
        useAutomationWorkflowProjectCodeWorkflowSourceQuery: () => ({
            data: {automationWorkflowProjectCodeWorkflowSource: {content: 'source', language: 'JAVASCRIPT'}},
            error: null,
            isLoading: false,
        }),
        useAutomationWorkflowProjectsQuery: () => ({
            data: {automationWorkflowProjects: [{id: '7', name: 'orders'}]},
        }),
        usePublishAutomationWorkflowProjectMutation: () => ({isPending: false, mutate: hoisted.publishMutate}),
        useUpdateAutomationWorkflowProjectCodeWorkflowSourceMutation: () => ({
            isPending: false,
            mutateAsync: hoisted.updateMutateAsync,
        }),
    };
});

const renderPage = () =>
    render(
        <MemoryRouter initialEntries={['/embedded/automation-workflows/projects/7/code']}>
            <Routes>
                <Route
                    element={<AutomationWorkflowProjectCodeWorkflow />}
                    path="/embedded/automation-workflows/projects/:automationWorkflowProjectId/code"
                />
            </Routes>
        </MemoryRouter>
    );

describe('AutomationWorkflowProjectCodeWorkflow', () => {
    beforeEach(() => {
        useCodeWorkflowHeaderStore.getState().reset();
    });

    it('shows the project name and publishes it', async () => {
        renderPage();

        expect(screen.getByText('orders')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Publish'}));

        expect(hoisted.publishMutate).toHaveBeenCalledWith({id: '7'}, expect.anything());
    });

    it('disables Publish while there are unsaved edits', () => {
        renderPage();

        act(() => useCodeWorkflowHeaderStore.setState({dirty: true}));

        expect(screen.getByRole('button', {name: 'Publish'})).toBeDisabled();
    });
});
```

`AutomationWorkflowProjectList.test.tsx` — add a code project fixture (`codeWorkflowProject: true`) and assert its row
menu shows "Edit Source" and calls `onEditProjectSource('<id>')`, and that a visual project's menu does not show it.

- [ ] **Step 2: Run them to see them fail**

```bash
cd client && npx vitest run src/ee/pages/embedded/automation-workflow-project-code-workflow src/ee/pages/embedded/automation-workflows
```

- [ ] **Step 3: Implement the page**

```tsx
const AutomationWorkflowProjectCodeWorkflow = () => {
    const {automationWorkflowProjectId} = useParams();

    const dirty = useCodeWorkflowHeaderStore((state) => state.dirty);
    const onSaveClick = useCodeWorkflowHeaderStore((state) => state.onSaveClick);
    const saving = useCodeWorkflowHeaderStore((state) => state.saving);

    const queryClient = useQueryClient();

    const {data: projectsData} = useAutomationWorkflowProjectsQuery();

    const {
        data: sourceData,
        error: sourceError,
        isLoading: sourceLoading,
    } = useAutomationWorkflowProjectCodeWorkflowSourceQuery(
        {automationWorkflowProjectId: automationWorkflowProjectId!},
        {enabled: !!automationWorkflowProjectId}
    );

    const updateSourceMutation = useUpdateAutomationWorkflowProjectCodeWorkflowSourceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['automationWorkflowProjectCodeWorkflowSource']});
        },
    });

    const publishMutation = usePublishAutomationWorkflowProjectMutation();

    const project = projectsData?.automationWorkflowProjects.find(
        (automationWorkflowProject) => automationWorkflowProject.id === automationWorkflowProjectId
    );

    const source = sourceData?.automationWorkflowProjectCodeWorkflowSource;

    const handlePublish = () =>
        publishMutation.mutate(
            {id: automationWorkflowProjectId!},
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});

                    toast('Published. The change is rolling out to connected users.');
                },
            }
        );

    return (
        <div className="flex size-full flex-col">
            <header className="flex items-center justify-between border-b px-4 py-2">
                <div className="flex items-center gap-2">
                    <Link aria-label="Back to Automation Workflows" to="/embedded/automation-workflows">
                        <ArrowLeftIcon className="size-4" />
                    </Link>

                    <span className="font-semibold">{project?.name}</span>
                </div>

                <div className="flex items-center gap-2">
                    <Button
                        disabled={!dirty || saving}
                        label={saving ? 'Saving...' : 'Save'}
                        onClick={() => onSaveClick?.()}
                        size="sm"
                        variant="outline"
                    />

                    <Button
                        disabled={dirty || saving || publishMutation.isPending}
                        label="Publish"
                        onClick={handlePublish}
                        size="sm"
                    />
                </div>
            </header>

            <CodeWorkflowSourceEditor
                error={sourceError}
                headerless
                isLoading={sourceLoading}
                isSaving={updateSourceMutation.isPending}
                language={(source?.language ?? 'JAVASCRIPT').toLowerCase()}
                onSave={(content) =>
                    updateSourceMutation.mutateAsync({automationWorkflowProjectId: automationWorkflowProjectId!, content})
                }
                source={source?.content}
            />
        </div>
    );
};

export default AutomationWorkflowProjectCodeWorkflow;
```

Check what `language` string `ProjectCodeWorkflowDetail` passes to `CodeWorkflowSourceEditor` (e.g. `'javascript'` vs
`'JAVASCRIPT'`) and convert the same way. If non-null assertions are flagged by lint, guard with an early
`if (!automationWorkflowProjectId) return null;` after the hooks.

- [ ] **Step 4: Route and entry points**

`routes.tsx` — next to the `automation-workflows/:workflowUuid/editor` route, same wrappers:

```tsx
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <EEVersion>
                                                <LazyLoadWrapper>
                                                    <AutomationWorkflowProjectCodeWorkflow />
                                                </LazyLoadWrapper>
                                            </EEVersion>
                                        </PrivateRoute>
                                    ),
                                    path: 'automation-workflows/projects/:automationWorkflowProjectId/code',
                                },
```

and a lazy import declared the way `AutomationWorkflow` is.

`AutomationWorkflows.tsx` — `openWorkflowEditor`:

```tsx
    const openWorkflowEditor = (workflowUuid: string) => {
        const codeProject = projects.find(
            (project) =>
                project.codeWorkflowProject &&
                project.workflowTemplates.some((workflowTemplate) => workflowTemplate.workflowUuid === workflowUuid)
        );

        if (codeProject) {
            navigate(`/embedded/automation-workflows/projects/${codeProject.id}/code`);

            return;
        }

        navigate(`/embedded/automation-workflows/${workflowUuid}/editor`);
    };
```

and pass `onEditProjectSource={(projectId) => navigate(`/embedded/automation-workflows/projects/${projectId}/code`)}`
to `AutomationWorkflowProjectList`, which forwards it to each `AutomationWorkflowProjectListItem`. In the list item
menu, inside `{project.codeWorkflowProject && (...)}` next to View References, add:

```tsx
                                    <DropdownMenuItem onClick={() => onEditProjectSource(project.id)}>
                                        Edit Source
                                    </DropdownMenuItem>
```

(wrap the two items in a fragment; keep props interfaces' keys sorted).

- [ ] **Step 5: Run tests, lint, typecheck**

```bash
cd client && npx vitest run src/ee/pages/embedded && npx eslint src/ee/pages/embedded src/routes.tsx && npx tsc -p tsconfig.json --noEmit --ignoreDeprecations 6.0
```

Expected: all pass.

- [ ] **Step 6: Commit**

```bash
npx prettier --write src/ee/pages/embedded src/routes.tsx
git add src/ee/pages/embedded src/routes.tsx
git commit -m "client - Edit and publish catalog code projects in a code editor page"
```

---

### Task 8: Final verification

- [ ] **Step 1: Server checks on every touched module**

```bash
./gradlew --configure-on-demand --continue \
  :server:ee:libs:automation:automation-configuration:automation-configuration-api:check \
  :server:ee:libs:automation:automation-configuration:automation-configuration-service:check \
  :server:ee:libs:embedded:embedded-configuration:embedded-configuration-api:check \
  :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:check \
  :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:testIntegration \
  :server:ee:libs:embedded:embedded-configuration:embedded-configuration-graphql:check \
  :server:apps:server-app:compileJava > /tmp/claude-502/final.log 2>&1; echo $?
grep -E "^> Task .* FAILED| FAILED$" /tmp/claude-502/final.log
```

Expected: exit 0. Any failure must be shown to also fail on the commit before Task 1 before it is called pre-existing.

- [ ] **Step 2: Client checks**

```bash
cd client && npx vitest run src/ee/pages/embedded src/pages/platform/code-workflow src/pages/automation/projects && npx tsc -p tsconfig.json --noEmit --ignoreDeprecations 6.0
```

Expected: all pass, no "Unhandled" errors in the output.

- [ ] **Step 3: Manual smoke (report, don't assume)**

Restart the server and reload the app (on a branch containing this work): New Project ▾ → New Code Project → create
"orders" (JavaScript) → editor opens with the starter → edit, Save → Publish → the list shows the project published and
its workflow; Import `bytechef-samples/javascript/embedded-automation-code-project/sample-catalog-greeting.js` still
publishes and shows its warning.
