# Embedded Automation Permission Expressions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add SpEL permission-expression support to embedded automation workflow projects and workflows, authored in `AutomationWorkflowProjectDialog.tsx` / `AutomationWorkflowDialog.tsx` and enforced for connected users — mirroring the existing `Integration` / `IntegrationWorkflow` feature.

**Architecture:** Store a nullable `permission_expression TEXT` column inline on the core `project` and `project_workflow` tables (the entities embedded automation reuses). The project expression is folded into the existing create/update project GraphQL mutations; the workflow expression uses a dedicated mutation and is also passed on create. Connected-user visibility is enforced in the public-REST catalog path via the existing `EmbeddedPermissionEvaluator`.

**Tech Stack:** Java 25 / Spring Boot 4, Spring Data JDBC, Liquibase, Spring GraphQL, React 19 + TypeScript, GraphQL Code Generator, JUnit 5, Vitest.

**Design spec:** `docs/superpowers/specs/2026-06-04-embedded-automation-permission-expressions-design.md`

**Reference (mirror these):**
- `IntegrationWorkflowServiceImpl.updatePermissionExpression` (load-set-save).
- `ConnectedUserIntegrationFacadeImpl.isIntegrationVisible` / `filterWorkflows` (per-resource filter).
- `ConnectedUserIntegrationFacadeFilterTest` (real-SpEL filter unit test — template for Task 9 tests).
- `EmbeddedPermissionEvaluator.evaluate(expr, connectedUser)` — blank ⇒ visible, false/throws ⇒ hidden.

**Conventions:**
- Files under `server/ee/**` use the ByteChef Enterprise license header and a `@version ee` Javadoc tag (incl. tests).
- Files under `server/libs/**` (core) use the Apache 2.0 header.
- Run `./gradlew spotlessApply` before each backend commit; `cd client && npm run check` before each client commit.
- Commit message convention: server `732 <description>`, client `732 client - <description>`.

---

## File Structure

**Core (CE, Apache header) — `server/libs/automation/automation-configuration/`**
- Create migration: `automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/202506041200010_automation_configuration_added_permission_expression.xml` (auto-included via `<includeAll>` in `server/libs/config/liquibase-config/.../master.xml` — no master edit).
- Modify: `automation-configuration-api/.../domain/Project.java` — add `permissionExpression` field + getter/setter.
- Modify: `automation-configuration-api/.../domain/ProjectWorkflow.java` — add `permissionExpression` field + getter/setter.

**EE (Enterprise header, `@version ee`) — `server/ee/libs/embedded/embedded-configuration/`**
- Modify: `embedded-configuration-graphql/.../graphql/automation-workflow-project.graphqls` — schema fields, mutation args, 2 new mutations.
- Modify: `embedded-configuration-api/.../dto/AutomationWorkflowProjectDTO.java` — add `permissionExpression`.
- Modify: `embedded-configuration-api/.../dto/ConnectedUserWorkflowTemplateDTO.java` — add `permissionExpression`.
- Modify: `embedded-configuration-api/.../facade/AutomationWorkflowProjectFacade.java` — new/changed method signatures.
- Modify: `embedded-configuration-service/.../facade/AutomationWorkflowProjectFacadeImpl.java` — persistence + filtering logic.
- Modify: `embedded-configuration-graphql/.../web/graphql/AutomationWorkflowProjectGraphQlController.java` — new args + mutations.
- Modify: `embedded-configuration-public-rest/.../public_/web/rest/AutomationWorkflowProjectApiController.java` — connected-user filtered catalog.
- Create test: `embedded-configuration-service/src/test/.../facade/AutomationWorkflowProjectFacadePermissionFilterTest.java`.

**Client (EE) — `client/src/ee/pages/embedded/automation-workflows/`**
- Modify: `client/src/graphql/embedded/configuration/automationWorkflowProjects.graphql` — select fields, args, 2 new mutations; regenerate `client/src/shared/middleware/graphql.ts`.
- Modify: `components/automation-workflow-project-dialog/AutomationWorkflowProjectDialog.tsx`.
- Modify: `components/automation-workflow-dialog/AutomationWorkflowDialog.tsx`.
- Modify: `components/automation-workflow-project-list/AutomationWorkflowProjectWorkflowListItem.tsx` (+ `AutomationWorkflowProjectWorkflowList.tsx`, `AutomationWorkflowProjectList.tsx` to thread `onEditWorkflow`).
- Modify: `AutomationWorkflows.tsx` — wire create/update args + edit-workflow flow.

---

## Task 1: Core DB migration + entity fields

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/202506041200010_automation_configuration_added_permission_expression.xml`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/domain/Project.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/domain/ProjectWorkflow.java`

- [ ] **Step 1: Create the Liquibase migration**

Create the file with this exact content (Apache header per core convention; `TEXT` matches the integration migration):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!--

    Copyright 2025 ByteChef

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="202506041200010" author="Ivica Cardic">
        <addColumn tableName="project">
            <column name="permission_expression" type="TEXT"/>
        </addColumn>
        <addColumn tableName="project_workflow">
            <column name="permission_expression" type="TEXT"/>
        </addColumn>
    </changeSet>
</databaseChangeLog>
```

- [ ] **Step 2: Add the field to `Project.java`**

In `Project.java`, add the field next to the other `@Column` fields (place it after the `description` field, before `@Id private Long id;`):

```java
    @Column("permission_expression")
    private String permissionExpression;
```

Add the getter near `getDescription()`:

```java
    public String getPermissionExpression() {
        return permissionExpression;
    }
```

Add the setter near `setDescription()`:

```java
    public void setPermissionExpression(String permissionExpression) {
        this.permissionExpression = permissionExpression;
    }
```

Do NOT add it to the `@PersistenceCreator` constructor — Spring Data JDBC populates non-constructor `@Column` fields via property access (same as the existing `workspaceId` / `createdBy` fields).

- [ ] **Step 3: Add the field to `ProjectWorkflow.java`**

In `ProjectWorkflow.java`, add the field after the `version` field (line 69):

```java
    @Column("permission_expression")
    private String permissionExpression;
```

Add the getter after `getVersion()`:

```java
    public String getPermissionExpression() {
        return permissionExpression;
    }
```

Add the setter after `setVersion(int version)`:

```java
    public void setPermissionExpression(String permissionExpression) {
        this.permissionExpression = permissionExpression;
    }
```

- [ ] **Step 4: Compile to verify**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-api:compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Format + commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/domain/Project.java \
        server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/domain/ProjectWorkflow.java \
        server/libs/automation/automation-configuration/automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/202506041200010_automation_configuration_added_permission_expression.xml
git commit -m "732 Add permission_expression column to project and project_workflow"
```

---

## Task 2: EE DTOs carry the permission expression

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/dto/AutomationWorkflowProjectDTO.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/dto/ConnectedUserWorkflowTemplateDTO.java`

- [ ] **Step 1: Add `permissionExpression` to `AutomationWorkflowProjectDTO`**

Replace the record header so the new component is the last field:

```java
public record AutomationWorkflowProjectDTO(
    long id, String name, String description, Long categoryId, List<Long> tagIds, boolean published, int version,
    Integer lastPublishedVersion, List<ConnectedUserWorkflowTemplateDTO> workflowTemplates,
    String permissionExpression) {
}
```

- [ ] **Step 2: Add `permissionExpression` to `ConnectedUserWorkflowTemplateDTO`**

Replace the record header (keep the nested `Component` record unchanged):

```java
public record ConnectedUserWorkflowTemplateDTO(
    String workflowUuid, String label, String description, String lastModifiedDate,
    List<Component> triggers, List<Component> components, String permissionExpression) {

    public record Component(String name, String title, String icon) {
    }
}
```

- [ ] **Step 3: Compile (expected to FAIL at call sites)**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:compileJava`
Expected: FAIL — constructor-arity errors in `AutomationWorkflowProjectFacadeImpl` (the `new ...DTO(...)` call sites). These are fixed in Task 3. This confirms the DTO change took effect.

No commit yet — commit together with Task 3.

---

## Task 3: Facade persistence + filtering logic

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectFacade.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectFacadeImpl.java`

- [ ] **Step 1: Update the facade interface**

In `AutomationWorkflowProjectFacade.java`, add these imports:

```java
import com.bytechef.platform.configuration.domain.Environment;
```

Change the existing method signatures and add the new ones (replace the matching lines):

```java
    long createProject(
        String name, String description, String category, List<String> tags, String permissionExpression);

    String createProjectWorkflow(long projectId, String definition, String permissionExpression);

    List<AutomationWorkflowProjectDTO> getPublishedProjects(String externalUserId, Environment environment);

    void updateProject(
        long projectId, String name, String description, String category, List<String> tags,
        String permissionExpression);

    void updateProjectWorkflow(String workflowId, String label, String description);

    void updateProjectWorkflowPermissionExpression(String workflowId, String permissionExpression);
```

Keep the existing `List<AutomationWorkflowProjectDTO> getPublishedProjects();` (no-arg, used by the admin frontend path) — the new overload is additive.

- [ ] **Step 2: Add imports + new collaborators to `AutomationWorkflowProjectFacadeImpl`**

Add imports:

```java
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.Environment;
import tools.jackson.core.type.TypeReference; // already imported — do not duplicate
```

Add two fields next to the existing `private final` fields:

```java
    private final ConnectedUserService connectedUserService;
    private final EmbeddedPermissionEvaluator embeddedPermissionEvaluator;
```

Update the constructor signature and body to accept and assign them (insert `connectedUserService` and `embeddedPermissionEvaluator` parameters, keeping alphabetical-ish grouping consistent with the file):

```java
    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectFacadeImpl(
        CategoryService categoryService, ComponentDefinitionService componentDefinitionService,
        ConnectedUserService connectedUserService, EmbeddedPermissionEvaluator embeddedPermissionEvaluator,
        ProjectService projectService, ProjectWorkflowFacade projectWorkflowFacade,
        ProjectWorkflowService projectWorkflowService, TagService tagService,
        WorkflowNodeTestOutputService workflowNodeTestOutputService, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.categoryService = categoryService;
        this.componentDefinitionService = componentDefinitionService;
        this.connectedUserService = connectedUserService;
        this.embeddedPermissionEvaluator = embeddedPermissionEvaluator;
        this.projectService = projectService;
        this.projectWorkflowFacade = projectWorkflowFacade;
        this.projectWorkflowService = projectWorkflowService;
        this.tagService = tagService;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }
```

- [ ] **Step 3: Fold the expression into `createProject`**

Replace the `createProject` method:

```java
    @Override
    public long createProject(
        String name, String description, String category, List<String> tags, String permissionExpression) {

        if (name != null && name.startsWith("__EMBEDDED")) {
            throw new IllegalArgumentException("Project name must not start with '__EMBEDDED': " + name);
        }

        Project project = new Project();

        project.setName(MARKER + name);
        project.setDescription(description);
        project.setWorkspaceId(Workspace.DEFAULT_WORKSPACE_ID);
        project.setCategoryId(resolveCategory(category));
        project.setTagIds(resolveTags(tags));
        project.setPermissionExpression(normalizePermissionExpression(permissionExpression));

        project = projectService.create(project);

        return project.getId();
    }
```

- [ ] **Step 4: Fold the expression into `updateProject` with the null-guard**

Replace the `updateProject` method. `null` means "leave unchanged" (protects the tag-only update path); a non-null value (including `""`) overwrites, with blank normalized to `null`:

```java
    @Override
    public void updateProject(
        long projectId, String name, String description, String category, List<String> tags,
        String permissionExpression) {

        Project project = getMarkedProject(projectId);

        project.setName(MARKER + name);
        project.setDescription(description);
        project.setCategoryId(resolveCategory(category));
        project.setTagIds(resolveTags(tags));

        if (permissionExpression != null) {
            project.setPermissionExpression(normalizePermissionExpression(permissionExpression));
        }

        projectService.update(project);
    }
```

- [ ] **Step 5: Pass the expression on workflow create**

Replace the `createProjectWorkflow` method:

```java
    @Override
    public String createProjectWorkflow(long projectId, String definition, String permissionExpression) {
        getMarkedProject(projectId);

        ProjectWorkflow projectWorkflow = projectWorkflowFacade.addWorkflow(
            projectId, StringUtils.isEmpty(definition) ? DEFAULT_DEFINITION : definition);

        String normalized = normalizePermissionExpression(permissionExpression);

        if (normalized != null) {
            projectWorkflow.setPermissionExpression(normalized);

            projectWorkflowService.update(projectWorkflow);
        }

        return projectWorkflow.getWorkflowId();
    }
```

- [ ] **Step 6: Add `updateProjectWorkflow` (label/description) and `updateProjectWorkflowPermissionExpression`**

Add both methods after `updateProject`. `updateProjectWorkflow` merges label/description into the workflow definition JSON (same JSON approach as `duplicateProjectWorkflow`); `updateProjectWorkflowPermissionExpression` writes the join entity only:

```java
    @Override
    public void updateProjectWorkflow(String workflowId, String label, String description) {
        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        getMarkedProject(projectWorkflow.getProjectId());

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Map<String, Object> definitionMap = JsonUtils.read(workflow.getDefinition(), new TypeReference<>() {});

        definitionMap.put("label", label);
        definitionMap.put("description", description == null ? "" : description);

        workflowService.update(
            workflowId, JsonUtils.writeWithDefaultPrettyPrinter(definitionMap), workflow.getVersion());
    }

    @Override
    public void updateProjectWorkflowPermissionExpression(String workflowId, String permissionExpression) {
        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        getMarkedProject(projectWorkflow.getProjectId());

        projectWorkflow.setPermissionExpression(normalizePermissionExpression(permissionExpression));

        projectWorkflowService.update(projectWorkflow);
    }
```

- [ ] **Step 7: Add the connected-user filtered catalog overload + helpers**

Add the overload and a private `normalizePermissionExpression` helper (place the overload after the existing no-arg `getPublishedProjects()`):

```java
    @Override
    public List<AutomationWorkflowProjectDTO> getPublishedProjects(String externalUserId, Environment environment) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        return getPublishedProjects().stream()
            .filter(project -> embeddedPermissionEvaluator.evaluate(project.permissionExpression(), connectedUser))
            .map(project -> filterWorkflowTemplates(project, connectedUser))
            .toList();
    }

    private AutomationWorkflowProjectDTO filterWorkflowTemplates(
        AutomationWorkflowProjectDTO project, ConnectedUser connectedUser) {

        List<ConnectedUserWorkflowTemplateDTO> visibleTemplates = project.workflowTemplates()
            .stream()
            .filter(template -> embeddedPermissionEvaluator.evaluate(
                template.permissionExpression(), connectedUser))
            .toList();

        return new AutomationWorkflowProjectDTO(
            project.id(), project.name(), project.description(), project.categoryId(), project.tagIds(),
            project.published(), project.version(), project.lastPublishedVersion(), visibleTemplates,
            project.permissionExpression());
    }

    private static String normalizePermissionExpression(String permissionExpression) {
        return StringUtils.isBlank(permissionExpression) ? null : permissionExpression.trim();
    }
```

- [ ] **Step 8: Populate `permissionExpression` in `toDTO`**

In `toDTO`, update the workflow-template mapping to read the join-entity expression, and add the project expression to the returned DTO. Replace the template `map(...)` lambda body and the final `return`:

```java
        List<ConnectedUserWorkflowTemplateDTO> workflowTemplates = projectWorkflows.stream()
            .map(projectWorkflow -> {
                Workflow workflow = workflowService.getWorkflow(projectWorkflow.getWorkflowId());

                return new ConnectedUserWorkflowTemplateDTO(
                    projectWorkflow.getWorkflowId(), workflow.getLabel(), workflow.getDescription(),
                    Objects.toString(workflow.getLastModifiedDate(), null), getTriggerComponents(workflow),
                    getTaskComponents(workflow), projectWorkflow.getPermissionExpression());
            })
            .filter(Objects::nonNull)
            .toList();
```

```java
        return new AutomationWorkflowProjectDTO(
            project.getId(), displayName, project.getDescription(), project.getCategoryId(),
            project.getTagIds(), published, project.getLastProjectVersion(), lastPublishedVersion, workflowTemplates,
            project.getPermissionExpression());
```

- [ ] **Step 9: Populate `permissionExpression` in `toPublishedDTO`**

In `toPublishedDTO`, update the template mapping and the final `return` the same way:

```java
            workflowTemplates = projectWorkflows.stream()
                .map(projectWorkflow -> {
                    Workflow workflow = workflowService.getWorkflow(projectWorkflow.getWorkflowId());

                    return new ConnectedUserWorkflowTemplateDTO(
                        projectWorkflow.getUuidAsString(), workflow.getLabel(), workflow.getDescription(),
                        Objects.toString(workflow.getLastModifiedDate(), null), getTriggerComponents(workflow),
                        getTaskComponents(workflow), projectWorkflow.getPermissionExpression());
                })
                .filter(Objects::nonNull)
                .toList();
```

```java
        return new AutomationWorkflowProjectDTO(
            project.getId(), displayName, project.getDescription(), project.getCategoryId(),
            project.getTagIds(), published, project.getLastProjectVersion(), lastPublishedVersion, workflowTemplates,
            project.getPermissionExpression());
```

- [ ] **Step 10: Compile**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:compileJava`
Expected: `BUILD SUCCESSFUL` (DTO arity errors from Task 2 are now resolved).

Note: the `embedded-configuration-graphql` module will not compile until Task 4 (it calls the old facade signatures). That is expected and fixed there.

No commit yet — commit with Task 4 so the GraphQL module compiles.

---

## Task 4: GraphQL schema + controller

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql/src/main/resources/graphql/automation-workflow-project.graphqls`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql/src/main/java/com/bytechef/ee/embedded/configuration/web/graphql/AutomationWorkflowProjectGraphQlController.java`

- [ ] **Step 1: Add schema fields + mutation args + new mutations**

In `automation-workflow-project.graphqls`:

Add `permissionExpression` to the `AutomationWorkflowProject` type (after `lastPublishedVersion`):

```graphql
    lastPublishedVersion: Int
    permissionExpression: String
    workflowTemplates: [AutomationWorkflowProjectWorkflowTemplate!]!
```

Add `permissionExpression` to `AutomationWorkflowProjectWorkflowTemplate` (after `description`):

```graphql
    label: String
    description: String
    permissionExpression: String
    lastModifiedDate: String
```

Replace the `Mutation` extension block with the new arguments and two new mutations:

```graphql
extend type Mutation {
    createAutomationWorkflowProject(name: String!, description: String, category: String, tags: [String!], permissionExpression: String): ID!
    updateAutomationWorkflowProject(id: ID!, name: String!, description: String, category: String, tags: [String!], permissionExpression: String): Boolean!
    deleteAutomationWorkflowProject(id: ID!): Boolean!
    publishAutomationWorkflowProject(id: ID!): Boolean!
    createAutomationWorkflowProjectWorkflow(projectId: ID!, definition: String, permissionExpression: String): ID!
    updateAutomationWorkflowProjectWorkflow(workflowUuid: ID!, label: String!, description: String): Boolean!
    updateAutomationWorkflowProjectWorkflowPermissionExpression(workflowUuid: ID!, permissionExpression: String): Boolean!
    deleteAutomationWorkflowProjectWorkflow(workflowUuid: ID!): Boolean!
    duplicateAutomationWorkflowProjectWorkflow(workflowUuid: ID!): ID!
    duplicateAutomationWorkflowProject(id: ID!): ID!
}
```

(No `@SchemaMapping` is needed for the new `permissionExpression` fields — Spring GraphQL maps them automatically from the matching record accessors added in Task 2.)

- [ ] **Step 2: Update the controller mutations**

In `AutomationWorkflowProjectGraphQlController.java`, replace `createAutomationWorkflowProject`, `updateAutomationWorkflowProject`, and `createAutomationWorkflowProjectWorkflow`, and add the two new mutations:

```java
    @MutationMapping
    public String createAutomationWorkflowProject(
        @Argument String name, @Argument String description, @Argument String category,
        @Argument List<String> tags, @Argument String permissionExpression) {

        return String.valueOf(
            automationWorkflowProjectFacade.createProject(
                name, description, category, tags == null ? List.of() : tags, permissionExpression));
    }

    @MutationMapping
    public boolean updateAutomationWorkflowProject(
        @Argument String id, @Argument String name, @Argument String description, @Argument String category,
        @Argument List<String> tags, @Argument String permissionExpression) {

        automationWorkflowProjectFacade.updateProject(
            Long.parseLong(id), name, description, category, tags == null ? List.of() : tags, permissionExpression);

        return true;
    }

    @MutationMapping
    public String createAutomationWorkflowProjectWorkflow(
        @Argument String projectId, @Argument String definition, @Argument String permissionExpression) {

        return automationWorkflowProjectFacade.createProjectWorkflow(
            Long.parseLong(projectId), definition, permissionExpression);
    }

    @MutationMapping
    public boolean updateAutomationWorkflowProjectWorkflow(
        @Argument String workflowUuid, @Argument String label, @Argument String description) {

        automationWorkflowProjectFacade.updateProjectWorkflow(workflowUuid, label, description);

        return true;
    }

    @MutationMapping
    public boolean updateAutomationWorkflowProjectWorkflowPermissionExpression(
        @Argument String workflowUuid, @Argument String permissionExpression) {

        automationWorkflowProjectFacade.updateProjectWorkflowPermissionExpression(workflowUuid, permissionExpression);

        return true;
    }
```

- [ ] **Step 3: Compile the GraphQL module**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-graphql:compileJava :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Check existing GraphQL controller test for arity breakage**

The existing `AutomationWorkflowProjectGraphQlControllerTest` constructs `AutomationWorkflowProjectDTO` / `ConnectedUserWorkflowTemplateDTO`. Compile the test source:

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-graphql:compileTestJava`
Expected: may FAIL with constructor-arity errors. If so, fix each `new AutomationWorkflowProjectDTO(...)` by appending a trailing `null` (or a sample expression) argument, and each `new ConnectedUserWorkflowTemplateDTO(...)` by appending a trailing `null` argument. Re-run until `BUILD SUCCESSFUL`.

- [ ] **Step 5: Format + commit Tasks 2–4 together**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-api \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-service \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql
git commit -m "732 Persist and expose permission expression on automation workflow projects/workflows"
```

---

## Task 5: Connected-user catalog filtering (public REST)

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/src/main/java/com/bytechef/ee/embedded/configuration/public_/web/rest/AutomationWorkflowProjectApiController.java`

- [ ] **Step 1: Filter the connected-user catalog by permission expression**

The connected-user endpoint is `getProjects(externalUserId, xEnvironment)`; the admin endpoint is `getFrontendProjects` (must stay unfiltered). Add `Environment` derivation and route the connected-user call through the new filtered facade overload.

Add imports:

```java
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.platform.configuration.domain.Environment;
```

Replace the `getProjects` method and add a `getEnvironment` helper (mirroring `IntegrationApiController`):

```java
    @Override
    public ResponseEntity<List<AutomationWorkflowProjectModel>> getProjects(
        String externalUserId, EnvironmentModel xEnvironment) {

        List<AutomationWorkflowProjectModel> models = automationWorkflowProjectFacade
            .getPublishedProjects(externalUserId, getEnvironment(xEnvironment))
            .stream()
            .map(project -> conversionService.convert(project, AutomationWorkflowProjectModel.class))
            .toList();

        return ResponseEntity.ok(models);
    }

    private Environment getEnvironment(EnvironmentModel xEnvironment) {
        return environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());
    }
```

Leave `getFrontendProjects` and `toAutomationWorkflowProjectModels()` unchanged (the admin frontend keeps seeing all published projects). The public REST `AutomationWorkflowProjectModel` does NOT include `permissionExpression`, so the expression is used only for filtering and never leaked to the connected user.

- [ ] **Step 2: Compile**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Fix the public-REST controller int test if its mock signature drifted**

`AutomationWorkflowProjectApiControllerIntTest` stubs `getPublishedProjects()`. Compile the test:

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:compileTestJava`
Expected: `BUILD SUCCESSFUL` (the no-arg overload still exists). If the `getProjects` test path now exercises the filtered overload and fails at runtime, update those stubs to
`when(automationWorkflowProjectFacade.getPublishedProjects(anyString(), any())).thenReturn(...)`.

- [ ] **Step 4: Format + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest
git commit -m "732 Filter connected-user automation workflow catalog by permission expression"
```

---

## Task 6: Backend tests — facade persistence + connected-user filter

**Files:**
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectFacadePermissionFilterTest.java`

This unit test mirrors `ConnectedUserIntegrationFacadeFilterTest`: it wires the facade with mocked collaborators but a **real** `EmbeddedPermissionEvaluator` (real SpEL), and asserts project-level + workflow-level visibility and the update/create persistence paths.

- [ ] **Step 1: Write the failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AutomationWorkflowProjectFacadePermissionFilterTest {

    private static final String MARKER = "__EMBEDDED_AUTOMATION__";

    private final CategoryService categoryService = mock(CategoryService.class);
    private final ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final EmbeddedPermissionEvaluator embeddedPermissionEvaluator =
        new EmbeddedPermissionEvaluator(SpelEvaluator.create());
    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectWorkflowFacade projectWorkflowFacade = mock(ProjectWorkflowFacade.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final TagService tagService = mock(TagService.class);
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService =
        mock(WorkflowNodeTestOutputService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);
    private final WorkflowTestConfigurationService workflowTestConfigurationService =
        mock(WorkflowTestConfigurationService.class);

    private final AutomationWorkflowProjectFacadeImpl facade = new AutomationWorkflowProjectFacadeImpl(
        categoryService, componentDefinitionService, connectedUserService, embeddedPermissionEvaluator,
        projectService, projectWorkflowFacade, projectWorkflowService, tagService, workflowNodeTestOutputService,
        workflowService, workflowTestConfigurationService);

    @Test
    void testGetPublishedProjectsHidesProjectWhenExpressionIsFalse() {
        ConnectedUser connectedUser = connectedUser(Map.of("plan", "free"));

        when(connectedUserService.getConnectedUser("user-1", Environment.PRODUCTION)).thenReturn(connectedUser);
        when(projectService.getProjects()).thenReturn(List.of(
            markedPublishedProject(1L, "Pro", "metadata['plan'] == 'pro'"),
            markedPublishedProject(2L, "Free", null)));
        when(projectWorkflowService.getProjectWorkflows(anyLong(), any(Integer.class))).thenReturn(List.of());

        List<AutomationWorkflowProjectDTO> projects = facade.getPublishedProjects("user-1", Environment.PRODUCTION);

        assertThat(projects)
            .extracting(AutomationWorkflowProjectDTO::name)
            .containsExactly("Free");
    }

    @Test
    void testUpdateProjectDoesNotClobberExpressionWhenArgumentIsNull() {
        Project project = markedPublishedProject(7L, "Pro", "metadata['plan'] == 'pro'");

        when(projectService.getProject(7L)).thenReturn(project);

        facade.updateProject(7L, "Pro", "desc", null, List.of(), null);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);

        verify(projectService).update(captor.capture());

        assertThat(captor.getValue()
            .getPermissionExpression()).isEqualTo("metadata['plan'] == 'pro'");
    }

    @Test
    void testUpdateProjectClearsExpressionWhenArgumentIsBlank() {
        Project project = markedPublishedProject(7L, "Pro", "metadata['plan'] == 'pro'");

        when(projectService.getProject(7L)).thenReturn(project);

        facade.updateProject(7L, "Pro", "desc", null, List.of(), "");

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);

        verify(projectService).update(captor.capture());

        assertThat(captor.getValue()
            .getPermissionExpression()).isNull();
    }

    @Test
    void testUpdateProjectWorkflowPermissionExpressionWritesJoinEntity() {
        ProjectWorkflow projectWorkflow = new ProjectWorkflow(1L, 1, "wf-1");

        when(projectWorkflowService.getWorkflowProjectWorkflow("wf-1")).thenReturn(projectWorkflow);
        when(projectService.getProject(1L)).thenReturn(markedPublishedProject(1L, "P", null));

        facade.updateProjectWorkflowPermissionExpression("wf-1", "metadata['tier'] == 'gold'");

        assertThat(projectWorkflow.getPermissionExpression()).isEqualTo("metadata['tier'] == 'gold'");

        verify(projectWorkflowService).update(projectWorkflow);
    }

    private static ConnectedUser connectedUser(Map<String, Object> metadata) {
        ConnectedUser connectedUser = new ConnectedUser();

        connectedUser.setExternalId("user-1");
        connectedUser.setEnvironment(Environment.PRODUCTION);
        connectedUser.setMetadata(metadata);

        return connectedUser;
    }

    private static Project markedPublishedProject(long id, String displayName, String permissionExpression) {
        Project project = new Project();

        project.setId(id);
        project.setName(MARKER + displayName);
        project.setPermissionExpression(permissionExpression);

        return project;
    }
}
```

- [ ] **Step 2: Run the test, expect it to compile and pass (or surface real wiring gaps)**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacadePermissionFilterTest"`
Expected: PASS.

Notes if it fails to compile/run:
- `Project.setId(...)` — if no public setter exists, use the `Project.builder()` API or reflection helper already used in sibling tests; check `Project` for an `id` setter and adapt. (`markedPublishedProject` only needs id + name + permissionExpression.)
- `markedPublishedProject` sets no `lastPublishedProjectVersion`, so `toPublishedDTO` yields empty `workflowTemplates` and never calls `getWorkflow` — keeping stubs minimal. If `toPublishedDTO` NPEs on a missing published version, give the project a published version via the same helper the existing `AutomationWorkflowProjectFacadeIntTest` uses, or assert via the int test instead.
- `ConnectedUser` setters — verify names against the `ConnectedUser` domain (`setExternalId`, `setEnvironment`, `setMetadata`); adapt if different.

If minimal mocking proves brittle for `toPublishedDTO`, instead add equivalent assertions to the existing `AutomationWorkflowProjectFacadeIntTest` (real Spring context), which already builds projects/workflows end-to-end — create a project with a permission expression, publish, seed a `ConnectedUser`, and assert `getPublishedProjects(externalUserId, env)` filtering. Prefer the unit test; fall back to the int test only if needed.

- [ ] **Step 3: Format + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test
git commit -m "732 Test permission expression persistence and connected-user filtering"
```

---

## Task 7: GraphQL operations + codegen (client)

**Files:**
- Modify: `client/src/graphql/embedded/configuration/automationWorkflowProjects.graphql`
- Regenerate: `client/src/shared/middleware/graphql.ts`

- [ ] **Step 1: Update the operations file**

Add `permissionExpression` selections and the new arguments/mutations. Replace the whole file with:

```graphql
query automationWorkflowProjects {
    automationWorkflowProjects {
        id
        name
        description
        categoryId
        tagIds
        published
        version
        lastPublishedVersion
        permissionExpression
        workflowTemplates {
            workflowUuid
            label
            description
            permissionExpression
            lastModifiedDate
            triggers {
                name
                title
                icon
            }
            components {
                name
                title
                icon
            }
        }
    }
}

mutation createAutomationWorkflowProject($name: String!, $description: String, $category: String, $tags: [String!], $permissionExpression: String) {
    createAutomationWorkflowProject(name: $name, description: $description, category: $category, tags: $tags, permissionExpression: $permissionExpression)
}

mutation updateAutomationWorkflowProject($id: ID!, $name: String!, $description: String, $category: String, $tags: [String!], $permissionExpression: String) {
    updateAutomationWorkflowProject(id: $id, name: $name, description: $description, category: $category, tags: $tags, permissionExpression: $permissionExpression)
}

mutation deleteAutomationWorkflowProject($id: ID!) {
    deleteAutomationWorkflowProject(id: $id)
}

mutation createAutomationWorkflowProjectWorkflow($projectId: ID!, $definition: String, $permissionExpression: String) {
    createAutomationWorkflowProjectWorkflow(projectId: $projectId, definition: $definition, permissionExpression: $permissionExpression)
}

mutation updateAutomationWorkflowProjectWorkflow($workflowUuid: ID!, $label: String!, $description: String) {
    updateAutomationWorkflowProjectWorkflow(workflowUuid: $workflowUuid, label: $label, description: $description)
}

mutation updateAutomationWorkflowProjectWorkflowPermissionExpression($workflowUuid: ID!, $permissionExpression: String) {
    updateAutomationWorkflowProjectWorkflowPermissionExpression(workflowUuid: $workflowUuid, permissionExpression: $permissionExpression)
}

mutation deleteAutomationWorkflowProjectWorkflow($workflowUuid: ID!) {
    deleteAutomationWorkflowProjectWorkflow(workflowUuid: $workflowUuid)
}

mutation publishAutomationWorkflowProject($id: ID!) {
    publishAutomationWorkflowProject(id: $id)
}
```

- [ ] **Step 2: Regenerate the typed client**

The codegen `schema` array reads the backend `.graphqls` (modified in Task 4). Run:

```bash
cd client && npx graphql-codegen
```

Expected: `client/src/shared/middleware/graphql.ts` regenerates with `useUpdateAutomationWorkflowProjectWorkflowMutation`, `useUpdateAutomationWorkflowProjectWorkflowPermissionExpressionMutation`, and `permissionExpression` on the project + template types. If codegen cannot find the new server schema, confirm the path to `automation-workflow-project.graphqls` is present in `client/codegen.ts`'s `schema` array (it already includes the embedded configuration graphql dir).

- [ ] **Step 3: Typecheck**

Run: `cd client && npm run typecheck`
Expected: PASS (no usages changed yet; generated types compile).

- [ ] **Step 4: Commit operations + generated file separately**

```bash
git add client/src/graphql/embedded/configuration/automationWorkflowProjects.graphql
git commit -m "732 client - Add permission expression to automation workflow project GraphQL operations"
git add client/src/shared/middleware/graphql.ts
git commit -m "732 client - Regenerate GraphQL types for automation workflow permission expression"
```

---

## Task 8: Project & workflow dialogs — add the field

**Files:**
- Modify: `client/src/ee/pages/embedded/automation-workflows/components/automation-workflow-project-dialog/AutomationWorkflowProjectDialog.tsx`
- Modify: `client/src/ee/pages/embedded/automation-workflows/components/automation-workflow-dialog/AutomationWorkflowDialog.tsx`

- [ ] **Step 1: Add `permissionExpression` to the project dialog form types**

In `AutomationWorkflowProjectDialog.tsx`, extend both interfaces (keep keys alphabetical per the `sort-keys` rule — `permissionExpression` sorts after `name`, before `tags`):

```tsx
export interface AutomationWorkflowProjectFormValuesI {
    category?: string;
    description: string;
    name: string;
    permissionExpression: string;
    tags: Array<string>;
}

interface AutomationWorkflowProjectFormI {
    category?: SelectOptionType;
    description: string;
    name: string;
    permissionExpression: string;
    tags: Array<SelectOptionType>;
}
```

- [ ] **Step 2: Default + persist the project value**

In the `useForm` `defaultValues`, add (alphabetical, after `name`):

```tsx
            name: project?.name || '',
            permissionExpression: project?.permissionExpression ?? '',
```

In `saveProject`, include it in the `onSubmit` payload (after `name`):

```tsx
    const saveProject = (formValues: AutomationWorkflowProjectFormI) => {
        onSubmit({
            category: formValues.category?.value || undefined,
            description: formValues.description,
            name: formValues.name,
            permissionExpression: formValues.permissionExpression,
            tags: (formValues.tags || []).map((tag) => tag.value),
        });
    };
```

- [ ] **Step 3: Render the project field**

Add this `FormField` after the `tags` field's `FormField` (before `<DialogFooter>`):

```tsx
                        <FormField
                            control={control}
                            name="permissionExpression"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Permission Expression</FormLabel>

                                    <FormControl>
                                        <Textarea
                                            placeholder="e.g. metadata['plan'] == 'pro'"
                                            rows={3}
                                            {...field}
                                        />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />
```

Note: `AutomationWorkflowProjectType` is derived from the query type, which now includes `permissionExpression` after Task 7 — so `project?.permissionExpression` typechecks.

- [ ] **Step 4: Add `permissionExpression` to the workflow dialog**

In `AutomationWorkflowDialog.tsx`, extend the form interface and the `workflow` prop type:

```tsx
export interface AutomationWorkflowFormValuesI {
    description: string;
    label: string;
    permissionExpression: string;
}

interface AutomationWorkflowDialogProps {
    onClose: () => void;
    onSubmit: (values: AutomationWorkflowFormValuesI) => void;
    workflow?: {description?: string | null; label?: string | null; permissionExpression?: string | null};
}
```

Add the default value (alphabetical, after `label`):

```tsx
        defaultValues: {
            description: workflow?.description ?? '',
            label: workflow?.label ?? '',
            permissionExpression: workflow?.permissionExpression ?? '',
        },
```

Add the field after the `description` `FormField` (before `<DialogFooter>`):

```tsx
                        <FormField
                            control={control}
                            name="permissionExpression"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Permission Expression</FormLabel>

                                    <FormControl>
                                        <Textarea
                                            placeholder="e.g. metadata['tier'] == 'gold'"
                                            rows={3}
                                            {...field}
                                        />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />
```

- [ ] **Step 5: Typecheck (expected to FAIL at the parent call site)**

Run: `cd client && npm run typecheck`
Expected: FAIL — `AutomationWorkflows.tsx` does not yet provide `permissionExpression` in its `onSubmit` payloads. Fixed in Task 10. This confirms the form-value contract changed.

No commit yet — commit with Task 10.

---

## Task 9: Workflow list — add the Edit action

**Files:**
- Modify: `client/src/ee/pages/embedded/automation-workflows/components/automation-workflow-project-list/AutomationWorkflowProjectWorkflowListItem.tsx`
- Modify: `client/src/ee/pages/embedded/automation-workflows/components/automation-workflow-project-list/AutomationWorkflowProjectWorkflowList.tsx`
- Modify: `client/src/ee/pages/embedded/automation-workflows/components/automation-workflow-project-list/AutomationWorkflowProjectList.tsx`

The `onEditWorkflow` callback receives the full workflow-template object (so the dialog can prefill label/description/permissionExpression).

- [ ] **Step 1: Add the Edit action to the row item**

In `AutomationWorkflowProjectWorkflowListItem.tsx`, update the imports to add `PencilIcon`:

```tsx
import {ComponentIcon, EllipsisVerticalIcon, PencilIcon, Trash2Icon} from 'lucide-react';
```

Add `onEditWorkflow` to the props interface (alphabetical, before `onSelectWorkflow`) — it receives the template object:

```tsx
interface AutomationWorkflowProjectWorkflowListItemProps {
    onDeleteWorkflow: (workflowUuid: string) => void;
    onEditWorkflow: (workflow: AutomationWorkflowProjectWorkflowTemplateType) => void;
    onSelectWorkflow: (workflowUuid: string) => void;
    workflow: AutomationWorkflowProjectWorkflowTemplateType;
}
```

Destructure it (alphabetical):

```tsx
const AutomationWorkflowProjectWorkflowListItem = ({
    onDeleteWorkflow,
    onEditWorkflow,
    onSelectWorkflow,
    workflow,
}: AutomationWorkflowProjectWorkflowListItemProps) => {
```

Add an Edit `DropdownMenuItem` immediately before the Delete item inside `DropdownMenuContent`:

```tsx
                        <DropdownMenuItem
                            aria-label="Edit Workflow"
                            onClick={(event) => {
                                event.stopPropagation();

                                onEditWorkflow(workflow);
                            }}
                        >
                            <PencilIcon /> Edit
                        </DropdownMenuItem>
```

- [ ] **Step 2: Thread `onEditWorkflow` through the workflow list**

In `AutomationWorkflowProjectWorkflowList.tsx`, add to the props interface (alphabetical, before `onSelectWorkflow`):

```tsx
    onEditWorkflow: (workflow: AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number]['workflowTemplates'][number]) => void;
```

If `AutomationWorkflowProjectsQuery` is not already imported in this file, import it from `@/shared/middleware/graphql` and define a local alias near the top to keep the prop readable:

```tsx
import {AutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';

type AutomationWorkflowProjectWorkflowTemplateType =
    AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number]['workflowTemplates'][number];
```

Then the prop becomes:

```tsx
    onEditWorkflow: (workflow: AutomationWorkflowProjectWorkflowTemplateType) => void;
```

Destructure `onEditWorkflow` in the component params (alphabetical) and pass it down to the item:

```tsx
                            <AutomationWorkflowProjectWorkflowListItem
                                key={workflow.workflowUuid}
                                onDeleteWorkflow={setWorkflowUuidToDelete}
                                onEditWorkflow={onEditWorkflow}
                                onSelectWorkflow={onSelectWorkflow}
                                workflow={workflow}
                            />
```

(Keep the existing `key`/prop ordering already in the file; only add the `onEditWorkflow` line.)

- [ ] **Step 3: Thread `onEditWorkflow` through the project list**

In `AutomationWorkflowProjectList.tsx`, add to the props interface (alphabetical, after `onDeleteWorkflow`):

```tsx
    onEditWorkflow: (workflow: AutomationWorkflowProjectType['workflowTemplates'][number]) => void;
```

(`AutomationWorkflowProjectType` is already aliased in this file. If the indexed access type is awkward, reuse the same `...workflowTemplates'][number]` alias as Step 2.)

Destructure `onEditWorkflow` (alphabetical) and pass it to `<AutomationWorkflowProjectWorkflowList>`:

```tsx
                        <AutomationWorkflowProjectWorkflowList
                            project={project}
                            onDeleteWorkflow={onDeleteWorkflow}
                            onEditWorkflow={onEditWorkflow}
                            onSelectWorkflow={onSelectWorkflow}
                        />
```

(Match the existing prop order in the file; only add the `onEditWorkflow` line.)

- [ ] **Step 4: Typecheck (still expected to FAIL until Task 10 supplies the prop)**

Run: `cd client && npm run typecheck`
Expected: FAIL — `AutomationWorkflows.tsx` doesn't pass `onEditWorkflow` yet. Fixed in Task 10.

No commit yet — commit with Task 10.

---

## Task 10: Wire `AutomationWorkflows.tsx` (create/update args + edit-workflow flow)

**Files:**
- Modify: `client/src/ee/pages/embedded/automation-workflows/AutomationWorkflows.tsx`

- [ ] **Step 1: Import the two new mutation hooks**

Add to the `@/shared/middleware/graphql` import block (alphabetical within the destructure):

```tsx
    useUpdateAutomationWorkflowProjectMutation,
    useUpdateAutomationWorkflowProjectWorkflowMutation,
    useUpdateAutomationWorkflowProjectWorkflowPermissionExpressionMutation,
```

- [ ] **Step 2: Add edit-workflow state + mutation instances**

Add state near the other `useState` calls (alphabetical grouping with existing state):

```tsx
    const [editWorkflow, setEditWorkflow] = useState<
        AutomationWorkflowProjectType['workflowTemplates'][number] | undefined
    >();
```

Add the mutation instances near the existing ones:

```tsx
    const updateWorkflowMutation = useUpdateAutomationWorkflowProjectWorkflowMutation();
    const updateWorkflowPermissionExpressionMutation =
        useUpdateAutomationWorkflowProjectWorkflowPermissionExpressionMutation();
```

- [ ] **Step 3: Pass `permissionExpression` on project create/update**

In `handleProjectDialogSubmit`, add `permissionExpression` to both `mutate` payloads (always a string; `""` clears, the backend null-guard leaves it unchanged only when truly omitted — here we always send it from the dialog):

Update path:

```tsx
            updateProjectMutation.mutate(
                {
                    category: values.category || undefined,
                    description: values.description || undefined,
                    id: editProject.id,
                    name: values.name,
                    permissionExpression: values.permissionExpression,
                    tags: values.tags,
                },
```

Create path:

```tsx
            createProjectMutation.mutate(
                {
                    category: values.category || undefined,
                    description: values.description || undefined,
                    name: values.name,
                    permissionExpression: values.permissionExpression,
                    tags: values.tags,
                },
```

- [ ] **Step 4: Keep `handleUpdateTags` from clobbering the expression**

`handleUpdateTags` updates only tags. The backend treats a `null`/omitted `permissionExpression` as "leave unchanged", so OMIT the field here (do not send `""`). Leave `handleUpdateTags`'s `mutate` payload unchanged — it already omits `permissionExpression`, which is the correct behavior. Add a clarifying comment above the `mutate` call:

```tsx
        // permissionExpression intentionally omitted: the backend leaves a stored expression unchanged
        // when the argument is null (tag-only update must not clobber it).
        updateProjectMutation.mutate(
```

- [ ] **Step 5: Pass `permissionExpression` on workflow create**

In `handleWorkflowDialogSubmit`, add the definition label/description from the form (already present) and pass the expression to the create mutation:

```tsx
        createWorkflowMutation.mutate(
            {definition, permissionExpression: values.permissionExpression, projectId: pendingWorkflowProjectId},
            {
                onSuccess: (data) => {
                    invalidateProjects();

                    openWorkflowEditor(data.createAutomationWorkflowProjectWorkflow);
                },
            }
        );
```

- [ ] **Step 6: Add the edit-workflow handlers**

Add an open handler (near `handleCreateWorkflow`) and a submit handler (near `handleWorkflowDialogSubmit`):

```tsx
    const handleEditWorkflow = (workflow: AutomationWorkflowProjectType['workflowTemplates'][number]) => {
        setEditWorkflow(workflow);
        setShowWorkflowDialog(true);
    };
```

```tsx
    const handleEditWorkflowSubmit = (values: AutomationWorkflowFormValuesI) => {
        if (!editWorkflow) {
            return;
        }

        const workflowUuid = editWorkflow.workflowUuid;

        updateWorkflowMutation.mutate(
            {description: values.description, label: values.label, workflowUuid},
            {
                onSuccess: () => {
                    updateWorkflowPermissionExpressionMutation.mutate(
                        {permissionExpression: values.permissionExpression, workflowUuid},
                        {
                            onSuccess: () => {
                                invalidateProjects();

                                handleWorkflowDialogClose();
                            },
                        }
                    );
                },
            }
        );
    };
```

- [ ] **Step 7: Reset `editWorkflow` on close**

Update `handleWorkflowDialogClose` to also clear the edit target:

```tsx
    const handleWorkflowDialogClose = () => {
        setShowWorkflowDialog(false);
        setPendingWorkflowProjectId(null);
        setEditWorkflow(undefined);
    };
```

- [ ] **Step 8: Pass `onEditWorkflow` to the list and render the dialog in edit/create mode**

Add `onEditWorkflow={handleEditWorkflow}` to `<AutomationWorkflowProjectList>` (alphabetical with the other `on*` props):

```tsx
                        onDeleteWorkflow={handleDeleteWorkflow}
                        onEditProject={handleEditProject}
                        onEditWorkflow={handleEditWorkflow}
                        onImportWorkflow={handleImportWorkflow}
```

Replace the workflow dialog render block so it handles both create and edit:

```tsx
            {showWorkflowDialog && (
                <AutomationWorkflowDialog
                    onClose={handleWorkflowDialogClose}
                    onSubmit={editWorkflow ? handleEditWorkflowSubmit : handleWorkflowDialogSubmit}
                    workflow={
                        editWorkflow
                            ? {
                                  description: editWorkflow.description,
                                  label: editWorkflow.label,
                                  permissionExpression: editWorkflow.permissionExpression,
                              }
                            : undefined
                    }
                />
            )}
```

- [ ] **Step 9: Full client check**

Run: `cd client && npm run check`
Expected: PASS (lint + typecheck + tests). Fix any `sort-keys` / import-sort violations the linter reports (these are not auto-fixed). Confirm there are no remaining type errors from Tasks 8–9.

- [ ] **Step 10: Format + commit (Tasks 8–10 together)**

```bash
cd client && npm run format
cd ..
git add client/src/ee/pages/embedded/automation-workflows
git commit -m "732 client - Add permission expression to automation workflow project and workflow dialogs"
```

---

## Task 11: Full backend verification

- [ ] **Step 1: Spotless + build the touched backend modules**

```bash
./gradlew spotlessApply
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:compileJava \
          :server:ee:libs:embedded:embedded-configuration:embedded-configuration-graphql:compileJava \
          :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:compileJava \
          :server:libs:automation:automation-configuration:automation-configuration-api:compileJava
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Run the embedded-configuration service tests**

```bash
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test \
          :server:ee:libs:embedded:embedded-configuration:embedded-configuration-graphql:test \
          :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:test
```

Expected: all green. If any existing test broke on DTO arity, fix the `new ...DTO(...)` call sites (append the trailing `permissionExpression` argument) and re-run.

- [ ] **Step 3: Commit any formatting/test fixups**

```bash
git add -A
git commit -m "732 Apply spotless and fix DTO call sites after permission expression changes"
```

(Skip this commit if there were no changes.)

---

## Self-Review Checklist (completed during planning)

- **Spec coverage:** DB columns (Task 1), entities (Task 1), GraphQL schema/fields/mutations (Task 4), DTOs (Task 2), facade fold + dedicated workflow mutation + null-guard (Task 3), connected-user filtering (Tasks 3+5), both dialogs (Task 8), Edit-Workflow entry point (Tasks 9–10), codegen (Task 7), tests (Task 6, 11). All spec sections mapped.
- **Type consistency:** facade method names (`createProject`/`updateProject`/`createProjectWorkflow`/`updateProjectWorkflow`/`updateProjectWorkflowPermissionExpression`/`getPublishedProjects`) match controller calls and GraphQL mutations (`updateAutomationWorkflowProjectWorkflow`, `updateAutomationWorkflowProjectWorkflowPermissionExpression`); DTO accessor `permissionExpression()` matches schema field `permissionExpression`; client hooks `useUpdateAutomationWorkflowProjectWorkflowMutation` / `useUpdateAutomationWorkflowProjectWorkflowPermissionExpressionMutation` match the generated names.
- **Identifier note:** the draft `workflowTemplates[].workflowUuid` carries the workflow's `workflowId` (string) in `toDTO`; the new mutations/facade resolve it via `projectWorkflowService.getWorkflowProjectWorkflow(workflowId)` / `workflowService.getWorkflow(workflowId)` — consistent with the existing delete/duplicate mutations.
- **No placeholders:** every code step contains complete content; verification commands have expected output.
```
