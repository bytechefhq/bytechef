# Embedded Automation Hub Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship an embeddable end-user Automation Hub (Templates / My Automations / Connections / builder) as a dedicated iframe entry, an `AutomationHub` React component in the embedded SDK that loads it, and an "Automation Hub" tab in the embedded sample app that showcases it.

**Architecture:** A new Vite HTML entry (`automation-hub.html` → `src/ee/automation-hub.tsx`) hosts an internal React Router with three tab views and the existing `WorkflowBuilder` page as a child route. Hub views call the public frontend API (`/api/embedded/v1`, JWT-scoped) through a newly generated `typescript-fetch` client; the builder keeps its internal API. The SDK component mirrors `EmbeddedWorkflowBuilder`'s `EMBED_READY` → `EMBED_INIT` handshake, extended with `tabs` and `theme`.

**Tech Stack:** Java 25 / Spring Boot 4 (EE `embedded-configuration-public-rest`, MapStruct, `@WebMvcTest` slices), React 19 + TypeScript + Vite 8 + Tailwind v4 + TanStack Query + Zustand + Vitest, embedded SDK (Vite lib build, Vitest, Next.js test app).

**Spec:** `docs/superpowers/specs/2026-08-17-embedded-automation-hub-design.md`

## Global Constraints

- EE code under `server/ee/` uses the ByteChef Enterprise license header and `@version ee` Javadoc tag.
- Every new public route with no `{externalUserId}` in its path adds its first segment to `ConnectedUserConstants.FRONTEND_RESERVED_PATH_SEGMENTS` and to the `@ValueSource` in `EmbeddedApiKeyAuthenticationConverterTest`.
- Foreign-owned ids return **404**, never 403.
- No changes under `server/libs/atlas/`; no new tables; no liquibase changesets.
- Client: object keys sorted (`sort-keys`), interfaces end in `I`/`Props`, `useRef` vars end in `Ref`, lucide icons imported with `Icon` suffix, `twMerge` not `cn`, hook order `useState → useRef → stores → custom hooks → derived → useEffect → return`.
- Java: blank line before control statements and after a variable modification that the next statement uses; no `TODO:` comments; test method names camelCase without underscores.
- Commit messages: server `732 <description>`, client `732 client - <description>`.
- Before each commit: server `./gradlew spotlessApply`; client `cd client && npm run check` (redirect gradle output to a file and check `$?`, never judge a piped run).
- Spec sections referenced below as §N.

---

## File map

**Server (EE)**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml`
- Modify: `server/ee/libs/embedded/embedded-connected-user/embedded-connected-user-api/src/main/java/com/bytechef/ee/embedded/connected/user/constant/ConnectedUserConstants.java`
- Modify: `server/ee/libs/embedded/embedded-security-web/embedded-security-web-impl/src/test/java/com/bytechef/ee/embedded/security/web/configurer/EmbeddedApiKeyAuthenticationConverterTest.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/dto/ConnectedUserProjectWorkflowDTO.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserProjectFacadeImpl.java`
- Modify: `…/public_/web/rest/mapper/ConnectUserProjectWorkflowMapper.java`, `…/mapper/ConnectionMapper.java`
- Modify: `…/public_/web/rest/ConnectedUserProjectWorkflowApiController.java`, `…/ConnectionApiController.java`
- Modify: `…/embedded-configuration-api/…/facade/ConnectedUserConnectionFacade.java`, `…-service/…/facade/ConnectedUserConnectionFacadeImpl.java`
- Modify: `server/libs/platform/platform-connection/platform-connection-api/…/facade/ConnectionFacade.java`, `…-service/…/facade/ConnectionFacadeImpl.java`
- Modify: `server/libs/config/security-config/src/main/java/com/bytechef/security/web/filter/SpaWebFilter.java`
- Test: `…/public_/web/rest/ConnectedUserProjectWorkflowApiControllerFrontendProvisionIntTest.java`, `…/ConnectionApiControllerFrontendIntTest.java`, `server/libs/config/security-config/src/test/java/com/bytechef/security/web/rest/filter/SpaWebFilterIntTest.java`

**Client**
- Create: `client/automation-hub.html`, `client/src/ee/automation-hub.tsx`, `client/src/ee/EmbeddedAutomationHubApp.tsx`
- Modify: `client/vite.config.mts`
- Generate: `client/src/ee/shared/middleware/embedded/public/**` (gradle `generateOpenAPITypeScriptFetch` on public-rest)
- Create: `client/src/ee/pages/embedded/shared/useEmbedHandshake.ts` (+ `tests/`)
- Modify: `client/src/ee/pages/embedded/workflow-builder/hooks/useWorkflowBuilder.ts`, `…/config/useFetchInterceptor.ts`, `…/WorkflowBuilder.tsx`
- Create under `client/src/ee/pages/embedded/automation-hub/`:
  - `AutomationHubLayout.tsx`, `HubBuilderView.tsx`
  - `stores/useAutomationHubStore.ts`
  - `theme/applyHubTheme.ts`
  - `queries/automationHub.queries.ts`, `mutations/automationHub.mutations.ts`
  - `views/TemplatesView.tsx`, `views/AutomationsView.tsx`, `views/ConnectionsView.tsx`
  - `wizard/activationReducer.ts`, `wizard/ActivationWizard.tsx`, `wizard/ConnectAccountsStep.tsx`, `wizard/ConfigureStep.tsx`, `wizard/ActivateStep.tsx`
  - `tests/` next to each

**SDK**
- Create: `sdks/frontend/embedded/library/src/components/automation-hub/{AutomationHub.tsx,index.ts,AutomationHub.test.tsx}`
- Modify: `sdks/frontend/embedded/library/src/main.ts`, `sdks/frontend/embedded/library/README.md`
- Create: `sdks/frontend/embedded/test-apps/app/hub/page.tsx`

---

## Phase 1 — Server

### Task 1: Public OpenAPI additions + reserved segment

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml`
- Modify: `…/embedded-connected-user-api/…/constant/ConnectedUserConstants.java:49-51`
- Modify: `…/embedded-security-web-impl/src/test/…/EmbeddedApiKeyAuthenticationConverterTest.java:244-247`

**Interfaces:**
- Produces (generated Java, `com.bytechef.ee.embedded.configuration.public_.web.rest`): `ConnectedUserProjectWorkflowApi#provisionFrontendWorkflowReference(String workflowUuid, EnvironmentModel xEnvironment)`, `#deprovisionFrontendWorkflowReference(String, EnvironmentModel)`; `ConnectionApi#getAllFrontendConnections(EnvironmentModel)`, `#createFrontendConnection(String componentName, CreateConnectionRequestModel, EnvironmentModel)`, `#deleteFrontendConnection(Long id, EnvironmentModel)`, `#reauthorizeFrontendConnection(Long id, ReauthorizeConnectionRequestModel, EnvironmentModel)`; models `ConnectedUserProjectWorkflowModel` gains `kind` (enum `COPY|REFERENCE`), `catalogWorkflowUuid`, `dangling`, `components: List<AutomationWorkflowProjectComponentModel>`; `ConnectionModel` gains `componentName`, `connectionVersion`, `authorizationType`, `createdDate`.

- [ ] **Step 1: Add the reserved segment test value first (fails until the constant is updated)**

In `EmbeddedApiKeyAuthenticationConverterTest` `@ValueSource` add `"connections"`:

```java
    @ValueSource(strings = {
        "app-events", "automation", "components", "connections", "external", "integration-instances", "integrations",
        "me", "unified", "workflows"
    })
```

- [ ] **Step 2: Run it to see the new case fail**

Run: `./gradlew :server:ee:libs:embedded:embedded-security-web:embedded-security-web-impl:test --tests '*EmbeddedApiKeyAuthenticationConverterTest*' > /tmp/t1.log 2>&1; echo $?`
Expected: non-zero; `grep -c "connections" /tmp/t1.log` shows the failing parameter.

- [ ] **Step 3: Add `"connections"` to `ConnectedUserConstants.FRONTEND_RESERVED_PATH_SEGMENTS`**

```java
    public static final Set<String> FRONTEND_RESERVED_PATH_SEGMENTS = Set.of(
        "app-events", "automation", "components", "connections", "external", "integration-instances", "integrations",
        "me", "unified", "workflows");
```

- [ ] **Step 4: Re-run — PASS**

- [ ] **Step 5: Extend `openapi.yaml` schemas**

Under `components.schemas`:

`ConnectedUserProjectWorkflow` — add properties:
```yaml
        kind:
          description: "COPY when the workflow is the user's own editable copy; REFERENCE when it points at a shared catalog workflow."
          type: "string"
          enum:
            - "COPY"
            - "REFERENCE"
        catalogWorkflowUuid:
          description: "For REFERENCE rows, the uuid of the catalog workflow being referenced."
          type: "string"
        dangling:
          description: "True when a REFERENCE points at a catalog workflow that is no longer served."
          type: "boolean"
        components:
          description: "The components used by the workflow."
          type: "array"
          items:
            $ref: "#/components/schemas/AutomationWorkflowProjectComponent"
```

`Connection` — add properties:
```yaml
        componentName:
          description: "The component name."
          type: "string"
        connectionVersion:
          description: "The connection version."
          type: "integer"
        authorizationType:
          description: "The authorization type name."
          type: "string"
        createdDate:
          description: "The created date."
          type: "string"
          format: "date-time"
```

New schemas:
```yaml
    CreateConnectionRequest:
      type: "object"
      required:
        - "name"
        - "connectionVersion"
        - "parameters"
      properties:
        name:
          type: "string"
        authorizationType:
          description: "The authorization type name; null for connections without authorization."
          type: "string"
        connectionVersion:
          type: "integer"
        parameters:
          type: "object"
          additionalProperties: true
    ReauthorizeConnectionRequest:
      type: "object"
      required:
        - "parameters"
      properties:
        parameters:
          type: "object"
          additionalProperties: true
    ConnectionInUseError:
      type: "object"
      properties:
        reason:
          type: "string"
          enum:
            - "CONNECTION_IS_USED"
```

- [ ] **Step 6: Add the paths**

Insert next to `/automation/workflow-templates/{workflowUuid}/copy` (all with the same `X-Environment` header parameter block the neighbours use and `security: - jwtBearerAuth: [ ]`):

```yaml
  /automation/workflow-templates/{workflowUuid}/provision:
    post:
      description: "Provision a reference to a catalog code workflow for the authenticated connected user."
      summary: "Provision a workflow reference"
      tags:
        - "connected-user-project-workflow"
      operationId: "provisionFrontendWorkflowReference"
      parameters:
        - name: "X-Environment"
          in: "header"
          required: false
          schema:
            $ref: '#/components/schemas/Environment'
        - name: "workflowUuid"
          in: "path"
          required: true
          schema:
            type: "string"
      responses:
        "204":
          description: "Successful operation."
        "409":
          description: "A required connection could not be auto-wired."
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/MissingConnectionError"
      security:
        - jwtBearerAuth: [ ]
    delete:
      description: "De-provision a reference to a catalog code workflow for the authenticated connected user."
      summary: "De-provision a workflow reference"
      tags:
        - "connected-user-project-workflow"
      operationId: "deprovisionFrontendWorkflowReference"
      parameters:
        - name: "X-Environment"
          in: "header"
          required: false
          schema:
            $ref: '#/components/schemas/Environment'
        - name: "workflowUuid"
          in: "path"
          required: true
          schema:
            type: "string"
      responses:
        "204":
          description: "Successful operation."
      security:
        - jwtBearerAuth: [ ]
  /connections:
    get:
      description: "Get every connection owned by the authenticated connected user."
      summary: "Get all connected user's connections"
      tags:
        - "connection"
      operationId: "getAllFrontendConnections"
      parameters:
        - name: "X-Environment"
          in: "header"
          required: false
          schema:
            $ref: '#/components/schemas/Environment'
      responses:
        "200":
          description: "The list of connections."
          content:
            application/json:
              schema:
                type: "array"
                items:
                  $ref: "#/components/schemas/Connection"
      security:
        - jwtBearerAuth: [ ]
  /connections/{id}:
    delete:
      description: "Delete a connection owned by the authenticated connected user."
      summary: "Delete a connection"
      tags:
        - "connection"
      operationId: "deleteFrontendConnection"
      parameters:
        - name: "X-Environment"
          in: "header"
          required: false
          schema:
            $ref: '#/components/schemas/Environment'
        - name: "id"
          in: "path"
          required: true
          schema:
            type: "integer"
            format: "int64"
      responses:
        "204":
          description: "Successful operation."
        "404":
          description: "The connection does not exist or is not owned by the caller."
        "409":
          description: "The connection is still used by an automation."
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ConnectionInUseError"
      security:
        - jwtBearerAuth: [ ]
  /connections/{id}/reauthorize:
    post:
      description: "Replace the credentials of a connection owned by the authenticated connected user, keeping its id."
      summary: "Reauthorize a connection"
      tags:
        - "connection"
      operationId: "reauthorizeFrontendConnection"
      parameters:
        - name: "X-Environment"
          in: "header"
          required: false
          schema:
            $ref: '#/components/schemas/Environment'
        - name: "id"
          in: "path"
          required: true
          schema:
            type: "integer"
            format: "int64"
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/ReauthorizeConnectionRequest"
      responses:
        "204":
          description: "Successful operation."
        "404":
          description: "The connection does not exist or is not owned by the caller."
      security:
        - jwtBearerAuth: [ ]
```

And add `post` to the existing `/components/{componentName}/connections` path:

```yaml
    post:
      description: "Create a connection for the authenticated connected user."
      summary: "Create a connected user connection"
      tags:
        - "connection"
      operationId: "createFrontendConnection"
      parameters:
        - name: "X-Environment"
          in: "header"
          required: false
          schema:
            $ref: '#/components/schemas/Environment'
        - name: "componentName"
          in: "path"
          required: true
          schema:
            type: "string"
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/CreateConnectionRequest"
      responses:
        "200":
          description: "The id of the created connection."
          content:
            application/json:
              schema:
                type: "integer"
                format: "int64"
      security:
        - jwtBearerAuth: [ ]
```

- [ ] **Step 7: Regenerate and confirm compile fails only on the missing controller methods**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:generateOpenAPI > /tmp/gen.log 2>&1; echo $?` → 0.
Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:compileJava > /tmp/c1.log 2>&1; echo $?` → non-zero **only** if the generated interfaces have non-default methods; the Spring generator emits `default` bodies returning 501, so expect **0**. Check `grep -c "provisionFrontendWorkflowReference" …/generated/src/main/java/com/bytechef/ee/embedded/configuration/public_/web/rest/ConnectedUserProjectWorkflowApi.java` → ≥1.

- [ ] **Step 8: Commit**

```bash
./gradlew spotlessApply > /tmp/sp.log 2>&1; echo $?
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/generated \
        server/ee/libs/embedded/embedded-connected-user \
        server/ee/libs/embedded/embedded-security-web
git commit -m "732 Add Automation Hub frontend routes to the embedded public OpenAPI"
```

---

### Task 2: `ConnectedUserProjectWorkflowDTO` kind/reference fields + reference rows in the frontend list

**Files:**
- Modify: `…/embedded-configuration-api/…/dto/ConnectedUserProjectWorkflowDTO.java`
- Modify: `…/embedded-configuration-service/…/facade/ConnectedUserProjectFacadeImpl.java` (private `getConnectedUserProjectWorkflows(ConnectedUserProject, Environment)`)
- Modify: `…/public_/web/rest/mapper/ConnectUserProjectWorkflowMapper.java`
- Test: `…/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserProjectFacadeTest.java` (create if absent — pure Mockito, no Spring)

**Interfaces:**
- Produces: `ConnectedUserProjectWorkflowDTO(long id, long connectedUserId, boolean enabled, Instant lastExecutionDate, long projectId, WorkflowDTO workflow, String workflowUuid, Integer workflowVersion, Kind kind, String catalogWorkflowUuid, boolean dangling, List<ConnectedUserWorkflowTemplateDTO.Component> components)` with nested `enum Kind {COPY, REFERENCE}`.
- Consumes: `ConnectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(long)`, `AutomationWorkflowProjectFacade.getPublishedProjects(String externalUserId, Environment)` → `AutomationWorkflowProjectDTO.workflowTemplates()` (`ConnectedUserWorkflowTemplateDTO(workflowUuid, label, description, lastModifiedDate, triggers, components, permissionExpression)`).

- [ ] **Step 1: Write the failing facade unit test**

```java
@ExtendWith(MockitoExtension.class)
class ConnectedUserProjectFacadeTest {

    // construct ConnectedUserProjectFacadeImpl with @Mock collaborators for every constructor arg (see its ctor)

    @Test
    void testGetConnectedUserProjectWorkflowsAppendsReferenceRows() {
        ConnectedUserProject connectedUserProject = new ConnectedUserProject();
        connectedUserProject.setId(10L);
        connectedUserProject.setConnectedUserId(7L);
        connectedUserProject.setProjectId(20L);

        // own project has zero workflows
        Project project = new Project(); project.setId(20L);
        when(projectService.getProject(20L)).thenReturn(project);
        when(projectWorkflowService.getProjectWorkflows(eq(20L), anyInt())).thenReturn(List.of());
        when(workflowService.getWorkflows(List.of())).thenReturn(List.of());

        ConnectedUserProjectWorkflow reference = new ConnectedUserProjectWorkflow();
        reference.setId(99L);
        reference.setCatalogWorkflowUuid("cat-1");
        reference.setEnabled(true);
        reference.setDangling(false);
        when(connectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(7L)).thenReturn(List.of(reference));

        when(automationWorkflowProjectFacade.getPublishedProjects("ext-1", Environment.PRODUCTION)).thenReturn(List.of(
            new AutomationWorkflowProjectDTO(1L, "Catalog", "desc", 1,
                List.of(new ConnectedUserWorkflowTemplateDTO("cat-1", "Sync leads", "d", null, List.of(),
                    List.of(new ConnectedUserWorkflowTemplateDTO.Component("slack", "Slack", "icon")), null)),
                null, true)));
        when(connectedUserService.getConnectedUser(7L)).thenReturn(connectedUserWithExternalId("ext-1"));

        List<ConnectedUserProjectWorkflowDTO> result =
            facade.getConnectedUserProjectWorkflows(connectedUserProject, Environment.PRODUCTION);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().kind()).isEqualTo(ConnectedUserProjectWorkflowDTO.Kind.REFERENCE);
        assertThat(result.getFirst().catalogWorkflowUuid()).isEqualTo("cat-1");
        assertThat(result.getFirst().workflowUuid()).isEqualTo("cat-1");
        assertThat(result.getFirst().workflow().getLabel()).isEqualTo("Sync leads");
        assertThat(result.getFirst().components()).extracting("name").containsExactly("slack");
    }
}
```

(Match `AutomationWorkflowProjectDTO`'s actual record parameter order from the file; make the private list method package-private for the test, or test through the public `getConnectedUserProjectWorkflows(String externalUserId, Environment)` by also stubbing `connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject`.)

- [ ] **Step 2: Run to verify it fails** (`kind()` does not exist).

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests '*ConnectedUserProjectFacadeTest*' > /tmp/t2.log 2>&1; echo $?`

- [ ] **Step 3: Extend the DTO**

```java
public record ConnectedUserProjectWorkflowDTO(
    long id, long connectedUserId, boolean enabled, Instant lastExecutionDate, long projectId, WorkflowDTO workflow,
    String workflowUuid, Integer workflowVersion, Kind kind, String catalogWorkflowUuid, boolean dangling,
    List<ConnectedUserWorkflowTemplateDTO.Component> components) {

    public enum Kind {
        COPY, REFERENCE
    }

    /** Copy-mode row (the user's own project workflow). */
    public ConnectedUserProjectWorkflowDTO(
        long connectedUserId, ConnectedUserProjectWorkflow connectedUserProjectWorkflow, boolean enabled,
        Instant lastExecutionDate, ProjectWorkflow projectWorkflow, WorkflowDTO workflow,
        List<ConnectedUserWorkflowTemplateDTO.Component> components) {

        this(
            connectedUserProjectWorkflow.getId(), connectedUserId, enabled, lastExecutionDate,
            projectWorkflow.getProjectId(), workflow, projectWorkflow.getUuidAsString(),
            connectedUserProjectWorkflow.getWorkflowVersion(), Kind.COPY, null, false, components);
    }

    /** Reference-mode row (points at a shared catalog workflow). */
    public static ConnectedUserProjectWorkflowDTO ofReference(
        long connectedUserId, ConnectedUserProjectWorkflow reference, WorkflowDTO catalogWorkflow,
        List<ConnectedUserWorkflowTemplateDTO.Component> components) {

        return new ConnectedUserProjectWorkflowDTO(
            reference.getId(), connectedUserId, reference.isEnabled(), null, 0L, catalogWorkflow,
            reference.getCatalogWorkflowUuid(), reference.getWorkflowVersion(), Kind.REFERENCE,
            reference.getCatalogWorkflowUuid(), reference.isDangling(), components);
    }
}
```

Keep the old 6-arg constructor delegating with `List.of()` components so existing callers compile; then update the two callers in `ConnectedUserProjectFacadeImpl` to pass components derived from the workflow (see Step 4).

- [ ] **Step 4: Append references in `ConnectedUserProjectFacadeImpl`**

Inject `ConnectedUserCodeWorkflowReferenceFacade` (already a field — it is used by `enableProjectWorkflow`) and `AutomationWorkflowProjectFacade` (add constructor arg + field; it lives in `embedded-configuration-api`, so no new module dependency). Replace the tail of the private list method:

```java
        List<ConnectedUserProjectWorkflowDTO> copies = workflowService.getWorkflows(latestWorkflowIds)
            .stream()
            .map(workflow -> { /* existing mapping, now passing toComponents(workflow) */ })
            .toList();

        List<ConnectedUserProjectWorkflowDTO> references = getReferenceRows(connectedUserProject, environment);

        return Stream.concat(copies.stream(), references.stream())
            .toList();
    }

    private List<ConnectedUserProjectWorkflowDTO> getReferenceRows(
        ConnectedUserProject connectedUserProject, Environment environment) {

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserProject.getConnectedUserId());

        Map<String, ConnectedUserWorkflowTemplateDTO> templatesByUuid = automationWorkflowProjectFacade
            .getPublishedProjects(connectedUser.getExternalId(), environment)
            .stream()
            .flatMap(project -> project.workflowTemplates().stream())
            .collect(Collectors.toMap(ConnectedUserWorkflowTemplateDTO::workflowUuid, Function.identity(), (a, b) -> a));

        return connectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUser.getId())
            .stream()
            .filter(row -> row.getCatalogWorkflowUuid() != null)
            .map(reference -> {
                ConnectedUserWorkflowTemplateDTO template = templatesByUuid.get(reference.getCatalogWorkflowUuid());

                String label = template == null ? reference.getCatalogWorkflowUuid() : template.label();
                String description = template == null ? "" : Objects.requireNonNullElse(template.description(), "");

                Workflow workflow = new Workflow(
                    reference.getCatalogWorkflowUuid(),
                    JsonUtils.write(Map.of("label", label, "description", description)), Workflow.Format.JSON);

                return ConnectedUserProjectWorkflowDTO.ofReference(
                    connectedUser.getId(), reference, new WorkflowDTO(workflow, List.of(), List.of()),
                    template == null ? List.of() : template.components());
            })
            .toList();
    }

    private List<ConnectedUserWorkflowTemplateDTO.Component> toComponents(Workflow workflow) {
        return workflowComponentResolver.getComponents(workflow);
    }
```

`WorkflowComponentResolver` is a new `@Component` in `embedded-configuration-service`
(`com.bytechef.ee.embedded.configuration.facade.WorkflowComponentResolver`) created by **moving** the three private
helpers `getTaskComponents`, `getTriggerComponents`, `resolveComponents` out of
`AutomationWorkflowProjectFacadeImpl` (they take a `Workflow` and use `componentDefinitionService`):

```java
@Component
@ConditionalOnEEVersion
public class WorkflowComponentResolver {

    private final ComponentDefinitionService componentDefinitionService;

    public WorkflowComponentResolver(ComponentDefinitionService componentDefinitionService) { ... }

    /** Distinct components used by the workflow's triggers followed by its tasks, in first-seen order. */
    public List<ConnectedUserWorkflowTemplateDTO.Component> getComponents(Workflow workflow) {
        Map<String, WorkflowNodeType> componentsByName = new LinkedHashMap<>();

        collect(componentsByName, WorkflowTrigger.of(workflow).stream().map(WorkflowTrigger::getType));
        collect(componentsByName, workflow.getTasks(true).stream().map(WorkflowTask::getType));

        return resolveComponents(componentsByName);   // moved verbatim
    }

    public List<ConnectedUserWorkflowTemplateDTO.Component> getTriggerComponents(Workflow workflow) { ... } // moved
    public List<ConnectedUserWorkflowTemplateDTO.Component> getTaskComponents(Workflow workflow) { ... }    // moved

    private void collect(Map<String, WorkflowNodeType> componentsByName, Stream<String> types) {
        types.map(WorkflowNodeType::ofType)
            .filter(nodeType -> nodeType.operation() != null)
            .forEach(nodeType -> componentsByName.putIfAbsent(nodeType.name(), nodeType));
    }
}
```

`AutomationWorkflowProjectFacadeImpl` injects it and calls `getTriggerComponents`/`getTaskComponents` where it
used to call its own private methods (behavior unchanged; its existing tests still pass). Because
`ConnectedUserProjectFacadeImpl` is a scanned `@Service`, grep for `*IntTestConfiguration`/`@TestConfiguration`
classes that hand-assemble it and add a mock `WorkflowComponentResolver` bean there.

The reference row builds a `Workflow` via its `Workflow(String id, String definition, Format format)`-style
constructor with a definition `{"label": <template label>, "description": <template description>}` (see how
`ConnectedUserProjectFacadeImpl.getConnectedUserProjectWorkflows` already wraps `Workflow` in `WorkflowDTO`); the goal
is only label/description/id in the DTO. A dangling reference whose template is gone still appears (label = uuid,
`dangling = true`).

- [ ] **Step 5: Extend the MapStruct mapper**

```java
    @Mapping(target = "createdDate", source = "workflow.createdDate")
    @Mapping(target = "definition", source = "workflow.definition")
    @Mapping(target = "description", source = "workflow.description")
    @Mapping(target = "label", source = "workflow.label")
    @Mapping(target = "lastModifiedDate", source = "workflow.lastModifiedDate")
    @Mapping(target = "kind", source = "kind")
    @Mapping(target = "catalogWorkflowUuid", source = "catalogWorkflowUuid")
    @Mapping(target = "dangling", source = "dangling")
    @Mapping(target = "components", source = "components")
    ConnectedUserProjectWorkflowModel convert(ConnectedUserProjectWorkflowDTO connectedUserProjectWorkflowDTO);
```

MapStruct maps `ConnectedUserWorkflowTemplateDTO.Component(name,title,icon)` → `AutomationWorkflowProjectComponentModel` by name automatically; the `Kind` enum maps to the generated `KindEnum` by constant name.

- [ ] **Step 6: Run the unit test + module compile — PASS**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests '*ConnectedUserProjectFacadeTest*' :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:compileJava > /tmp/t2b.log 2>&1; echo $?` → 0. Also run the existing `ConnectedUserProjectWorkflowApiControllerCopyIntTest` — must still pass.

- [ ] **Step 7: Commit**

```bash
git commit -am "732 List reference rows with kind metadata in the embedded frontend workflows API"
```

---

### Task 3: Frontend provision / de-provision controller methods

**Files:**
- Modify: `…/public_/web/rest/ConnectedUserProjectWorkflowApiController.java`
- Test: `…/public_/web/rest/ConnectedUserProjectWorkflowApiControllerFrontendProvisionIntTest.java`

- [ ] **Step 1: Write the failing `@WebMvcTest` (copy the class header from `…ReferenceIntTest`)**

```java
@ContextConfiguration(classes = EmbeddedConfigurationPublicRestTestConfiguration.class)
@TestPropertySource(properties = "bytechef.edition=ee")
@WebMvcTest(ConnectedUserProjectWorkflowApiController.class)
@EmbeddedConfigurationPublicRestSharedMocks
public class ConnectedUserProjectWorkflowApiControllerFrontendProvisionIntTest {

    private static final String EXTERNAL_USER_ID = "ext-user-1";
    private static final String WORKFLOW_UUID = "catalog-uuid-1";

    @Autowired private ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade;
    @MockitoBean private EnvironmentService environmentService;
    @Autowired private MockMvc mockMvc;
    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        webTestClient = MockMvcWebTestClient.bindTo(mockMvc).build();
        when(environmentService.getEnvironment(any())).thenReturn(Environment.PRODUCTION);
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testFrontendProvisionUsesThePrincipalAsExternalUserId() {
        when(connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class)))
                .thenReturn(new ConnectedUserProjectWorkflow());

        webTestClient.post()
            .uri("/v1/automation/workflow-templates/{workflowUuid}/provision", WORKFLOW_UUID)
            .exchange()
            .expectStatus().isNoContent();

        verify(connectedUserCodeWorkflowReferenceFacade)
            .getOrCreateReference(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class));
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testFrontendProvisionMissingConnectionReturns409() {
        when(connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(any(), any(), any()))
            .thenThrow(new MissingConnectionException("slack"));

        webTestClient.post()
            .uri("/v1/automation/workflow-templates/{workflowUuid}/provision", WORKFLOW_UUID)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody().jsonPath("$.missingConnectionComponentName").isEqualTo("slack");
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testFrontendDeprovisionDeletesTheReference() {
        webTestClient.delete()
            .uri("/v1/automation/workflow-templates/{workflowUuid}/provision", WORKFLOW_UUID)
            .exchange()
            .expectStatus().isNoContent();

        verify(connectedUserCodeWorkflowReferenceFacade)
            .deleteReference(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class));
    }
}
```

- [ ] **Step 2: Run — FAIL with 501 (generated default)**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:test --tests '*FrontendProvisionIntTest*' > /tmp/t3.log 2>&1; echo $?`

- [ ] **Step 3: Implement**

```java
    @Override
    public ResponseEntity<Object> provisionFrontendWorkflowReference(String workflowUuid, EnvironmentModel xEnvironment) {
        String externalUserId = OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found");

        try {
            connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
                externalUserId, workflowUuid, getEnvironment(xEnvironment));
        } catch (MissingConnectionException missingConnectionException) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("missingConnectionComponentName", missingConnectionException.getComponentName()));
        }

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> deprovisionFrontendWorkflowReference(String workflowUuid, EnvironmentModel xEnvironment) {
        String externalUserId = OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found");

        connectedUserCodeWorkflowReferenceFacade.deleteReference(externalUserId, workflowUuid, getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }
```

- [ ] **Step 4: Run — PASS. Commit**

```bash
git commit -am "732 Add frontend provision and de-provision routes for catalog workflow references"
```

---

### Task 4: Connections — facade methods, platform `updateAuthorization`, controller methods

**Files:**
- Modify: `server/libs/platform/platform-connection/platform-connection-api/…/facade/ConnectionFacade.java`, `…-service/…/facade/ConnectionFacadeImpl.java`
- Modify: `…/embedded-configuration-api/…/facade/ConnectedUserConnectionFacade.java`, `…-service/…/facade/ConnectedUserConnectionFacadeImpl.java`
- Modify: `…/public_/web/rest/mapper/ConnectionMapper.java`, `…/public_/web/rest/ConnectionApiController.java`
- Test: `server/libs/platform/platform-connection/platform-connection-service/src/test/…/facade/ConnectionFacadeTest.java` (extend or create), `…-service/src/test/…/facade/ConnectedUserConnectionFacadeTest.java`, `…/public_/web/rest/ConnectionApiControllerFrontendIntTest.java`

**Interfaces:**
- Produces: `ConnectionFacade#updateAuthorization(long id, Map<String, ?> parameters)`; `ConnectedUserConnectionFacade#getConnections(Long connectedUserId, @Nullable String componentName, List<Long> connectionIds)` (null = all components), `#deleteConnectedUserConnection(long connectedUserId, long connectionId)`, `#reauthorizeConnectedUserConnection(long connectedUserId, long connectionId, Map<String, ?> parameters)`. Both new EE methods throw `NoSuchElementException` when the connection is not owned by the connected user (→ 404 via `GlobalResponseEntityExceptionHandler`).

- [ ] **Step 1: Platform — failing test for `updateAuthorization`**

In `ConnectionFacadeTest` (Mockito): stub `connectionService.getConnection(5L)` with a non-OAuth connection (`authorizationType = null`), call `facade.updateAuthorization(5L, Map.of("apiKey", "new"))`, verify `connectionService.updateConnectionParameters(5L, Map.of("apiKey", "new"))`. Second test: an OAuth2 authorization-code connection with `parameters` containing `code` → verify `connectionDefinitionService.executeAuthorizationCallback(...)` is invoked and its `result()` map is merged into the parameters passed to `updateConnectionParameters`.

- [ ] **Step 2: Run — FAIL (method missing)**

- [ ] **Step 3: Implement in `ConnectionFacadeImpl`**

Extract the block at `create` lines 150-190 (the `if (connection.getAuthorizationType() != null && connection.containsParameter(Authorization.CODE))` branch) into `private void resolveOAuth2AuthorizationCode(Connection connection)`; call it from `create` where the block was. Add:

```java
    @Override
    public void updateAuthorization(long id, Map<String, ?> parameters) {
        Connection connection = connectionService.getConnection(id);

        connection.putAllParameters(parameters);

        resolveOAuth2AuthorizationCode(connection);

        connectionService.updateConnectionParameters(id, connection.getParameters());
    }
```

Add the signature to `ConnectionFacade` with Javadoc: replaces credentials in place, re-running the OAuth2 code exchange when a `code` is present; ownership is enforced by `ConnectionService#updateConnectionParameters`. Any `RemoteConnectionFacadeClient` in EE `*-remote-client` modules gets a stub throwing `UnsupportedOperationException` (grep `implements ConnectionFacade`).

- [ ] **Step 4: Run — PASS**

- [ ] **Step 5: EE facade — failing tests**

`ConnectedUserConnectionFacadeTest` (Mockito, construct `ConnectedUserConnectionFacadeImpl` with mocks):
- `testGetConnectionsWithNullComponentNameReturnsAllOwnedConnections`: stub `connectedUserConnectionService.getConnectionIds(7L)` → `[1,2]`, `integrationInstanceService.getConnectedUserIntegrationInstances(7L, env)` → instance with connectionId 3, `connectionFacade.getConnections([1,2,3], EMBEDDED)` → three DTOs with different componentNames; assert all three returned.
- `testDeleteConnectedUserConnectionRejectsForeignId`: `getConnectionIds(7L)` → `[1]`; `deleteConnectedUserConnection(7L, 9L)` → `assertThrows(NoSuchElementException.class)`; `verify(connectionFacade, never()).delete(any())`.
- `testDeleteConnectedUserConnectionDelegates`: owned id → `verify(connectionFacade).delete(1L)`.
- `testReauthorizeDelegates`: owned id → `verify(connectionFacade).updateAuthorization(1L, params)`; foreign → `NoSuchElementException`.

- [ ] **Step 6: Run — FAIL. Implement**

```java
    @Override
    public List<ConnectionDTO> getConnections(Long connectedUserId, @Nullable String componentName, List<Long> connectionIds) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        Set<Long> allConnectionIds = new LinkedHashSet<>();

        List<IntegrationInstance> integrationInstances = componentName == null
            ? integrationInstanceService.getConnectedUserIntegrationInstances(connectedUser.getId(), connectedUser.getEnvironment())
            : integrationInstanceService.getIntegrationInstances(connectedUser.getId(), componentName, connectedUser.getEnvironment());

        allConnectionIds.addAll(integrationInstances.stream().map(IntegrationInstance::getConnectionId).toList());
        allConnectionIds.addAll(connectedUserConnectionService.getConnectionIds(connectedUser.getId()));
        allConnectionIds.addAll(connectionIds);

        return connectionFacade.getConnections(new ArrayList<>(allConnectionIds), PlatformType.EMBEDDED)
            .stream()
            .filter(connectionDTO -> componentName == null || componentName.equals(connectionDTO.componentName()))
            .toList();
    }

    @Override
    public void deleteConnectedUserConnection(long connectedUserId, long connectionId) {
        requireOwned(connectedUserId, connectionId);

        connectionFacade.delete(connectionId);
    }

    @Override
    public void reauthorizeConnectedUserConnection(long connectedUserId, long connectionId, Map<String, ?> parameters) {
        requireOwned(connectedUserId, connectionId);

        connectionFacade.updateAuthorization(connectionId, parameters);
    }

    private void requireOwned(long connectedUserId, long connectionId) {
        boolean owned = getConnections(connectedUserId, null, List.of())
            .stream()
            .anyMatch(connectionDTO -> Objects.equals(connectionDTO.id(), connectionId));

        if (!owned) {
            throw new NoSuchElementException("Connection id=%s not found".formatted(connectionId));
        }
    }
```

Note the shared-connection case: `sharedConnectionIds` are only ever passed by callers into `connectionIds`; `requireOwned` passes `List.of()`, so a vendor-shared connection is never deletable/reauthorizable by an end user.

- [ ] **Step 7: Run — PASS**

- [ ] **Step 8: Mapper + controller failing WebMvc test**

`ConnectionMapper`: add `@Mapping(target = "createdDate", source = "createdDate")` and let `componentName`, `connectionVersion` map by name; `authorizationType` needs `@Mapping(target = "authorizationType", expression = "java(source.authorizationType() == null ? null : source.authorizationType().name())")`.

`ConnectionApiControllerFrontendIntTest` (`@WebMvcTest(ConnectionApiController.class)`, same header pattern; `@MockitoBean ConnectedUserConnectionFacade`, `ConnectedUserService`, `EnvironmentService`; stub `connectedUserService.getConnectedUser("ext-user-1", PRODUCTION)` → connectedUser id 7):
- `testGetAllFrontendConnectionsListsEveryComponent`: facade returns two DTOs (slack, hubspot) → `GET /v1/connections` → 200, `$.length()==2`, `$[0].componentName=="slack"`.
- `testCreateFrontendConnectionReturnsId`: `POST /v1/components/slack/connections` body `{"name":"My Slack","authorizationType":"OAUTH2_AUTHORIZATION_CODE","connectionVersion":1,"parameters":{"code":"abc"}}` → 200 body `42`; captor asserts the `ConnectionDTO` passed to `createConnectedUserConnection(7L, dto)` has `componentName=="slack"`, `name=="My Slack"`.
- `testDeleteFrontendConnectionMapsInUseTo409`: facade throws `new ConfigurationException("used", ConnectionErrorType.CONNECTION_IS_USED)` → 409, `$.reason=="CONNECTION_IS_USED"`.
- `testDeleteFrontendConnectionForeignReturns404`: facade throws `NoSuchElementException` → 404.
- `testReauthorizeFrontendConnection`: `POST /v1/connections/5/reauthorize` `{"parameters":{"apiKey":"x"}}` → 204, verify facade call.

- [ ] **Step 9: Run — FAIL. Implement controller methods**

```java
    @Override
    public ResponseEntity<List<ConnectionModel>> getAllFrontendConnections(EnvironmentModel xEnvironment) {
        ConnectedUser connectedUser = getCurrentConnectedUser(xEnvironment);

        return ResponseEntity.ok(
            connectedUserConnectionFacade.getConnections(connectedUser.getId(), null, List.of())
                .stream()
                .map(connectionDTO -> conversionService.convert(connectionDTO, ConnectionModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<Long> createFrontendConnection(
        String componentName, CreateConnectionRequestModel request, EnvironmentModel xEnvironment) {

        ConnectedUser connectedUser = getCurrentConnectedUser(xEnvironment);

        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .componentName(componentName)
            .connectionVersion(request.getConnectionVersion())
            .authorizationType(request.getAuthorizationType() == null ? null : AuthorizationType.valueOf(request.getAuthorizationType()))
            .environmentId(getEnvironment(xEnvironment).ordinal())
            .name(request.getName())
            .parameters(request.getParameters())
            .build();

        return ResponseEntity.ok(connectedUserConnectionFacade.createConnectedUserConnection(connectedUser.getId(), connectionDTO));
    }

    @Override
    public ResponseEntity<Object> deleteFrontendConnection(Long id, EnvironmentModel xEnvironment) {
        ConnectedUser connectedUser = getCurrentConnectedUser(xEnvironment);

        try {
            connectedUserConnectionFacade.deleteConnectedUserConnection(connectedUser.getId(), id);
        } catch (ConfigurationException configurationException) {
            if (configurationException.getErrorType() == ConnectionErrorType.CONNECTION_IS_USED) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("reason", "CONNECTION_IS_USED"));
            }

            throw configurationException;
        }

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> reauthorizeFrontendConnection(
        Long id, ReauthorizeConnectionRequestModel request, EnvironmentModel xEnvironment) {

        ConnectedUser connectedUser = getCurrentConnectedUser(xEnvironment);

        connectedUserConnectionFacade.reauthorizeConnectedUserConnection(connectedUser.getId(), id, request.getParameters());

        return ResponseEntity.noContent().build();
    }

    private ConnectedUser getCurrentConnectedUser(EnvironmentModel xEnvironment) {
        String externalUserId = OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not authenticated");

        return connectedUserService.getConnectedUser(externalUserId, getEnvironment(xEnvironment));
    }
```

(Check `ConnectionDTO.builder()` field names against the record; if the builder lacks `environmentId`, use the canonical constructor as `getFrontendConnections`' neighbours do. Verify `ConfigurationException#getErrorType()` accessor name in `AbstractException`.) Also route the existing `getFrontendConnections` through `getCurrentConnectedUser`.

- [ ] **Step 10: Run all public-rest tests + spotless — PASS. Commit**

```bash
./gradlew spotlessApply > /tmp/sp.log 2>&1; echo $?
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:test \
          :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test \
          :server:libs:platform:platform-connection:platform-connection-service:test > /tmp/t4.log 2>&1; echo $?
git add -A server/libs/platform/platform-connection server/ee/libs/embedded/embedded-configuration
git commit -m "732 Add connected-user connection list, create, delete and reauthorize frontend routes"
```

---

### Task 5: `SpaWebFilter` forwards `/embedded/hub*` to `automation-hub.html`

**Files:**
- Modify: `server/libs/config/security-config/src/main/java/com/bytechef/security/web/filter/SpaWebFilter.java:80-86`
- Test: `server/libs/config/security-config/src/test/java/com/bytechef/security/web/rest/filter/SpaWebFilterIntTest.java`

- [ ] **Step 1: Failing tests (next to `testFilterForwardsEmbeddedBuilderToWorkflowBuilderHtml`)**

```java
    @Test
    void testFilterForwardsEmbeddedHubRootToAutomationHubHtml() throws Exception {
        mockMvc.perform(get("/embedded/hub"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/automation-hub.html"));
    }

    @Test
    void testFilterForwardsEmbeddedHubDeepLinkToAutomationHubHtml() throws Exception {
        mockMvc.perform(get("/embedded/hub/builder/0199f000-aaaa-bbbb-cccc-000000000001"))
            .andExpect(forwardedUrl("/automation-hub.html"));
    }

    @Test
    void testFilterDoesNotForwardEmbeddedHubAssets() throws Exception {
        mockMvc.perform(get("/embedded/hub/assets/app.js"))
            .andExpect(forwardedUrl(null));
    }
```

- [ ] **Step 2: Run — FAIL**

Run: `./gradlew :server:libs:config:security-config:testIntegration --tests '*SpaWebFilterIntTest*' > /tmp/t5.log 2>&1; echo $?`

- [ ] **Step 3: Implement**

```java
        if (path.startsWith("/embedded/hub") && !path.contains(".")) {
            RequestDispatcher requestDispatcher = request.getRequestDispatcher("/automation-hub.html");

            requestDispatcher.forward(request, response);

            return;
        }
```

Place it right after the `/embedded/builder/` branch and update the class Javadoc list of forwarded entries.

- [ ] **Step 4: Run — PASS. Commit** `732 Forward embedded hub paths to the automation hub entry`

---

## Phase 2 — Client

### Task 6: Generated public TS client + fetch interceptor for `/api/embedded/v1/`

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/build.gradle.kts`
- Generate: `client/src/ee/shared/middleware/embedded/public/**`
- Modify: `client/src/ee/pages/embedded/workflow-builder/config/useFetchInterceptor.ts:56-70`
- Test: `client/src/ee/pages/embedded/workflow-builder/config/tests/useFetchInterceptor.test.ts`

**Interfaces:**
- Produces: `import {AutomationWorkflowProjectApi, ConnectedUserProjectWorkflowApi, ConnectionApi, ConnectedUserProjectWorkflow, AutomationWorkflowProject, Connection} from '@/ee/shared/middleware/embedded/public'` — generated `typescript-fetch` classes with `BASE_PATH = "/api/embedded/v1"`.

- [ ] **Step 1: Add the generator task**

```kotlin
val generateOpenAPITypeScriptFetch by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    generatorName.set("typescript-fetch")
    inputSpec.set("$projectDir/openapi.yaml")
    outputDir.set("$rootDir/client/src/ee/shared/middleware/embedded/public")
}

tasks.register("generateOpenAPI") {
    dependsOn(generateOpenAPISpring, generateOpenAPITypeScriptFetch)
}
```

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:generateOpenAPITypeScriptFetch > /tmp/g6.log 2>&1; echo $?` → 0; `ls client/src/ee/shared/middleware/embedded/public/apis` shows `ConnectionApi.ts` etc. (`**/middleware` is already prettier/eslint-ignored.)

- [ ] **Step 2: Failing interceptor test** — add to the existing test file a case: register the interceptor, call `fetch('/api/embedded/v1/connections')` with `sessionStorage.jwtToken='t'`, assert the intercepted request has `Authorization: Bearer t` and `X-ENVIRONMENT`. Mirror how the existing tests exercise `/internal/`.

- [ ] **Step 3: Run — FAIL. Implement**

```ts
                if (url.includes('/internal/') || url.includes('/graphql') || url.includes('/api/embedded/v1/')) {
```

- [ ] **Step 4: Run — PASS. Commit**

```bash
cd client && npm run check > /tmp/c6.log 2>&1; echo $?
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/build.gradle.kts client/src/ee/shared/middleware/embedded/public client/src/ee/pages/embedded/workflow-builder/config
git commit -m "732 client - Generate the embedded public API client and authorize it in the embed interceptor"
```

---

### Task 7: Extract `useEmbedHandshake`

**Files:**
- Create: `client/src/ee/pages/embedded/shared/useEmbedHandshake.ts`, `client/src/ee/pages/embedded/shared/tests/useEmbedHandshake.test.tsx`
- Modify: `client/src/ee/pages/embedded/workflow-builder/hooks/useWorkflowBuilder.ts:130-198`

**Interfaces:**
- Produces:
```ts
export interface EmbedInitParamsI {
    connectionDialogAllowed?: boolean;
    environment?: string;
    includeComponents?: string[];
    jwtToken?: string;
    sharedConnectionIds?: number[];
    tabs?: {automations?: boolean; connections?: boolean; newWorkflow?: boolean; templates?: boolean};
    theme?: {borderRadius?: string; fontFamily?: string; mode?: 'dark' | 'light'; primaryColor?: string};
}
export function useEmbedHandshake(onInit: (params: EmbedInitParamsI) => void): void;
```
Behavior (unchanged from the builder): reads `VITE_EMBEDDED_PARENT_ORIGINS`, ignores messages not from `window.parent` or from disallowed origins, on `EMBED_INIT` writes `sessionStorage.jwtToken` + `sessionStorage.environment` (uppercased default `PRODUCTION`) when a token is present, then calls `onInit(params)`; on mount posts `EMBED_READY` to `'*'` (no allow-list) or to each allowed origin.

- [ ] **Step 1: Failing tests**

```tsx
describe('useEmbedHandshake', () => {
    it('posts EMBED_READY to the parent on mount', () => {
        const postMessage = vi.fn();
        vi.spyOn(window, 'parent', 'get').mockReturnValue({postMessage} as unknown as Window);

        renderHook(() => useEmbedHandshake(vi.fn()));

        expect(postMessage).toHaveBeenCalledWith({type: 'EMBED_READY'}, '*');
    });

    it('stores the token and forwards params on EMBED_INIT from the parent', () => {
        const parent = {postMessage: vi.fn()} as unknown as Window;
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
        const onInit = vi.fn();

        renderHook(() => useEmbedHandshake(onInit));

        act(() => {
            window.dispatchEvent(new MessageEvent('message', {
                data: {params: {environment: 'staging', jwtToken: 'jwt-1', tabs: {connections: false}}, type: 'EMBED_INIT'},
                origin: 'https://host.example',
                source: parent,
            }));
        });

        expect(sessionStorage.getItem('jwtToken')).toBe('jwt-1');
        expect(sessionStorage.getItem('environment')).toBe('STAGING');
        expect(onInit).toHaveBeenCalledWith(expect.objectContaining({tabs: {connections: false}}));
    });

    it('ignores EMBED_INIT that does not come from the parent window', () => { /* source: window → onInit not called */ });
});
```

- [ ] **Step 2: Run — FAIL. Implement the hook** by moving the listener/broadcast code verbatim from `useWorkflowBuilder`'s effect (lines 138-190) into the hook; the hook's `onInit` receives `event.data.params`.

- [ ] **Step 3: Rewire `useWorkflowBuilder`**

```ts
    useEmbedHandshake((params) => {
        setConnectionDialogAllowed(params.connectionDialogAllowed ?? false);
        setIncludeComponents(params.includeComponents);
        setSharedConnectionIds(params.sharedConnectionIds);
        setInitialized(true);
    });
```

Keep the bottom-panel reset and the `setRightSidebarOpen(false)` cleanup in their own `useEffect`.

- [ ] **Step 4: Run the hook tests and the whole client suite — PASS. Commit** `732 client - Extract the embed handshake into a shared hook`

---

### Task 8: Hub entry, store, layout, theme

**Files:**
- Create: `client/automation-hub.html`, `client/src/ee/automation-hub.tsx`, `client/src/ee/EmbeddedAutomationHubApp.tsx`
- Modify: `client/vite.config.mts:23-27`
- Create: `client/src/ee/pages/embedded/automation-hub/stores/useAutomationHubStore.ts`, `…/theme/applyHubTheme.ts`, `…/AutomationHubLayout.tsx`
- Test: `…/automation-hub/tests/useAutomationHubStore.test.ts`, `…/tests/applyHubTheme.test.ts`, `…/tests/AutomationHubLayout.test.tsx`

**Interfaces:**
- Produces:
```ts
export interface AutomationHubTabsI {automations: boolean; connections: boolean; newWorkflow: boolean; templates: boolean}
export interface AutomationHubThemeI {borderRadius?: string; fontFamily?: string; mode?: 'dark' | 'light'; primaryColor?: string}
interface AutomationHubStateI {
    connectionDialogAllowed: boolean; includeComponents?: string[]; initialized: boolean;
    sharedConnectionIds: number[]; tabs: AutomationHubTabsI; theme: AutomationHubThemeI;
    initialize: (params: EmbedInitParamsI) => void;
}
export const useAutomationHubStore = create<AutomationHubStateI>(...);   // defaults: all tabs true, connectionDialogAllowed true
export function applyHubTheme(theme: AutomationHubThemeI, root?: HTMLElement): 'dark' | 'light';
```

- [ ] **Step 1: Store test (failing)** — `initialize({tabs: {connections: false}, sharedConnectionIds: [1]})` → `tabs` equals `{automations: true, connections: false, newWorkflow: true, templates: true}`, `initialized === true`, `sharedConnectionIds` `[1]`; `initialize({})` keeps defaults.

- [ ] **Step 2: Implement store**

```ts
const DEFAULT_TABS: AutomationHubTabsI = {automations: true, connections: true, newWorkflow: true, templates: true};

export const useAutomationHubStore = create<AutomationHubStateI>()((set) => ({
    connectionDialogAllowed: true,
    includeComponents: undefined,
    initialize: (params) =>
        set({
            connectionDialogAllowed: params.connectionDialogAllowed ?? true,
            includeComponents: params.includeComponents,
            initialized: true,
            sharedConnectionIds: params.sharedConnectionIds ?? [],
            tabs: {...DEFAULT_TABS, ...(params.tabs ?? {})},
            theme: params.theme ?? {},
        }),
    initialized: false,
    sharedConnectionIds: [],
    tabs: DEFAULT_TABS,
    theme: {},
}));
```

- [ ] **Step 3: Theme test (failing)** — `applyHubTheme({primaryColor: '#ff0000', borderRadius: '4px', fontFamily: 'Inter'}, root)` sets `--primary: #ff0000`, `--ring: #ff0000`, `--primary-foreground` to `#ffffff` (dark red → white text), `--radius: 4px`, `--font-sans: Inter`; returns `'light'`; `applyHubTheme({mode: 'dark'})` returns `'dark'`; `applyHubTheme({primaryColor: 'not-a-color'})` leaves `--primary` unset.

- [ ] **Step 4: Implement**

```ts
const HEX_COLOR = /^#([0-9a-f]{3}|[0-9a-f]{6})$/i;

function contrastForeground(hex: string): string {
    const full = hex.length === 4 ? `#${[...hex.slice(1)].map((char) => char + char).join('')}` : hex;
    const [red, green, blue] = [1, 3, 5].map((offset) => parseInt(full.slice(offset, offset + 2), 16) / 255);
    const luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue;

    return luminance > 0.5 ? '#111111' : '#ffffff';
}

const CSS_LENGTH = /^\d+(\.\d+)?(px|rem|em|%)$/;

function isSupportedColor(value: string): boolean {
    if (HEX_COLOR.test(value)) {
        return true;
    }

    // jsdom has no CSS.supports; non-hex colors are only accepted where the browser can vouch for them
    return typeof CSS !== 'undefined' && typeof CSS.supports === 'function' && CSS.supports('color', value);
}

export function applyHubTheme(theme: AutomationHubThemeI, root: HTMLElement = document.documentElement): 'dark' | 'light' {
    if (theme.primaryColor && isSupportedColor(theme.primaryColor)) {
        root.style.setProperty('--primary', theme.primaryColor);
        root.style.setProperty('--ring', theme.primaryColor);

        if (HEX_COLOR.test(theme.primaryColor)) {
            root.style.setProperty('--primary-foreground', contrastForeground(theme.primaryColor));
        }
    }

    if (theme.fontFamily) {
        root.style.setProperty('--font-sans', theme.fontFamily);
    }

    if (theme.borderRadius && CSS_LENGTH.test(theme.borderRadius)) {
        root.style.setProperty('--radius', theme.borderRadius);
    }

    return theme.mode === 'dark' ? 'dark' : 'light';
}
```

- [ ] **Step 5: Layout test (failing)** — render `<AutomationHubLayout/>` inside a `MemoryRouter` with the store set to `tabs.connections=false`; assert tabs "Templates" and "My Automations" are rendered as links and "Connections" is not; with only `templates` enabled, no tab strip is rendered (`queryByRole('tablist')` null); before `initialized`, a loading indicator is shown.

- [ ] **Step 6: Implement `AutomationHubLayout`**

```tsx
const AutomationHubLayout = () => {
    const {initialized, tabs, theme} = useAutomationHubStore(useShallow((state) => ({initialized: state.initialized, tabs: state.tabs, theme: state.theme})));
    const {setTheme} = useTheme();
    const location = useLocation();

    const visibleTabs = useMemo(
        () => [
            {enabled: tabs.templates, label: 'Templates', to: '/embedded/hub'},
            {enabled: tabs.automations, label: 'My Automations', to: '/embedded/hub/automations'},
            {enabled: tabs.connections, label: 'Connections', to: '/embedded/hub/connections'},
        ].filter((tab) => tab.enabled),
        [tabs]
    );

    useEffect(() => {
        if (initialized) {
            setTheme(applyHubTheme(theme));
        }
    }, [initialized, setTheme, theme]);

    if (!initialized) {
        return <div className="flex size-full items-center justify-center"><LoadingDots /></div>;
    }

    return (
        <div className="flex size-full flex-col bg-background text-foreground">
            <header className="flex items-center justify-between border-b px-6 py-3">
                <h1 className="text-lg font-semibold">Automations</h1>
                {visibleTabs.length > 1 && (
                    <nav role="tablist" className="flex gap-1">
                        {visibleTabs.map((tab) => (
                            <Link key={tab.to} role="tab" aria-selected={location.pathname === tab.to} to={tab.to}
                                className={twMerge('rounded-md px-3 py-1.5 text-sm', location.pathname === tab.to ? 'bg-muted font-medium' : 'text-muted-foreground hover:bg-muted/60')}>
                                {tab.label}
                            </Link>
                        ))}
                    </nav>
                )}
            </header>
            <main className="flex-1 overflow-auto p-6"><Outlet /></main>
        </div>
    );
};
```

If a hidden tab is deep-linked, redirect to the first visible tab (`<Navigate>` in the child route element via a small `RequireTab` guard component reading the store).

- [ ] **Step 7: Entry files**

`client/automation-hub.html`: copy `workflow-builder.html`, title `Automation Hub - ByteChef`, script `/src/ee/automation-hub.tsx`.

`client/src/ee/EmbeddedAutomationHubApp.tsx`:
```tsx
const EmbeddedAutomationHubApp = () => {
    const initialize = useAutomationHubStore((state) => state.initialize);

    useFetchInterceptor();
    useEmbedHandshake(initialize);

    return (<><Outlet /><Toaster /></>);
};
```

`client/src/ee/automation-hub.tsx`: copy `workflow-builder.tsx`; router:
```tsx
const router = createBrowserRouter([
    {
        children: [
            {
                children: [
                    {element: <TemplatesView />, index: true},
                    {element: <AutomationsView />, path: 'automations'},
                    {element: <ConnectionsView />, path: 'connections'},
                ],
                element: <AutomationHubLayout />,
                path: 'hub',
            },
            {element: <HubBuilderView />, path: 'hub/builder/:workflowUuid'},
        ],
        element: <EmbeddedAutomationHubApp />,
        path: '/embedded',
    },
]);
```
Until Tasks 9-11/14 exist, point the three views and `HubBuilderView` at placeholder components that render their name — replaced in later tasks (a placeholder *file* that a later task overwrites is fine; a placeholder *step* is not).

`vite.config.mts`: `automationHub: resolve(import.meta.dirname, 'automation-hub.html'),`.

- [ ] **Step 8: Run tests + `npm run check`; verify the entry serves** — `cd client && npm run dev` then open `http://localhost:5173/automation-hub.html`; the loading dots render (no handshake in a top-level window; the layout still mounts). Commit `732 client - Add the Automation Hub entry, store, layout and theming`.

---

### Task 9: Queries/mutations + Templates view

**Files:**
- Create: `…/automation-hub/queries/automationHub.queries.ts`, `…/mutations/automationHub.mutations.ts`, `…/views/TemplatesView.tsx`, `…/views/components/TemplateCard.tsx`
- Test: `…/automation-hub/tests/TemplatesView.test.tsx`

**Interfaces:**
- Produces:
```ts
export const AutomationHubKeys = {
    automations: ['automationHub', 'automations'] as const,
    connections: ['automationHub', 'connections'] as const,
    connectionsByComponent: (componentName: string) => ['automationHub', 'connections', componentName] as const,
    templates: ['automationHub', 'templates'] as const,
    workflow: (workflowUuid: string) => ['automationHub', 'workflow', workflowUuid] as const,
};
export const useGetTemplateProjectsQuery = () => useQuery<AutomationWorkflowProject[]>({queryKey: AutomationHubKeys.templates, queryFn: () => new AutomationWorkflowProjectApi().getFrontendProjects({})});
export const useGetMyAutomationsQuery = () => useQuery<ConnectedUserProjectWorkflow[]>({queryKey: AutomationHubKeys.automations, queryFn: () => new ConnectedUserProjectWorkflowApi().getFrontendProjectWorkflows({})});
export const useGetMyConnectionsQuery = () => useQuery<Connection[]>({queryKey: AutomationHubKeys.connections, queryFn: () => new ConnectionApi().getAllFrontendConnections({})});
export const useGetComponentConnectionsQuery = (componentName: string, enabled = true) => useQuery<Connection[]>({enabled, queryKey: AutomationHubKeys.connectionsByComponent(componentName), queryFn: () => new ConnectionApi().getFrontendConnections({componentName})});
export const useGetMyWorkflowQuery = (workflowUuid?: string) => useQuery<ConnectedUserProjectWorkflow>({enabled: !!workflowUuid, queryKey: AutomationHubKeys.workflow(workflowUuid!), queryFn: () => new ConnectedUserProjectWorkflowApi().getFrontendProjectWorkflow({workflowUuid: workflowUuid!})});
// mutations (all useMutation; each invalidates the keys named in parentheses on success)
useCopyTemplateMutation()          // (workflowUuid) => copyFrontendWorkflowTemplate → returns copy uuid   (automations)
useProvisionReferenceMutation()    // (workflowUuid) => provisionFrontendWorkflowReference               (automations)
useDeprovisionReferenceMutation()  // (workflowUuid) => deprovisionFrontendWorkflowReference             (automations)
useSetAutomationEnabledMutation()  // ({workflowUuid, enabled}) => enable/disableFrontendProjectWorkflow (automations)
useDeleteAutomationMutation()      // (workflowUuid) => deleteFrontendProjectWorkflow                    (automations)
usePublishAutomationMutation()     // (workflowUuid) => publishFrontendProjectWorkflow({description: ''}) (automations)
useCreateBlankAutomationMutation() // () => createFrontendProjectWorkflow({definition: BLANK_DEFINITION}) → uuid (automations)
useWireNodeConnectionMutation()    // ({workflowUuid, workflowNodeName, workflowConnectionKey, connectionId}) => updateFrontendWorkflowConfigurationConnection
useCreateHubConnectionMutation()   // ({componentName, request}) => createFrontendConnection → id         (connections, connectionsByComponent)
useDeleteHubConnectionMutation()   // (id) => deleteFrontendConnection                                    (connections)
useReauthorizeHubConnectionMutation() // ({id, parameters}) => reauthorizeFrontendConnection             (connections)
export const BLANK_DEFINITION = JSON.stringify({description: '', label: 'New automation', tasks: [], triggers: []});
```
Use the exact generated method/param names from `client/src/ee/shared/middleware/embedded/public/apis/*.ts` (`operationId` → camelCase method; request objects keyed by parameter name).

- [ ] **Step 1: Failing view test** — mock the queries module: two projects, one with two templates (one template has components `[{name:'slack', title:'Slack', icon:'<svg/>'}]`), assert group headings, card labels, a search box filtering by label (`userEvent.type` "sync" → only matching card), empty-state text when no projects, and that clicking "Use template" calls the `onActivate` handler with the template + project kind (render `TemplatesView` with an injected `onActivate` prop for the test; the real page passes the wizard opener).

- [ ] **Step 2: Implement `TemplatesView` + `TemplateCard`**

`TemplatesView`: `useState(search)`, `useGetTemplateProjectsQuery()`, `useMemo` filtered groups; `LoadingDots` while loading; error → inline `Alert`; sections per project with `<h2>{project.name}</h2>` + description; grid `grid gap-4 sm:grid-cols-2 lg:grid-cols-3` of `TemplateCard`; state `activeTemplate` opens `<ActivationWizard template kind onClose/>` (Task 13; until then render nothing when `activeTemplate` is set — the test injects `onActivate`).

`TemplateCard`: `label`, `description` (line-clamped), component icons row (`<img>` from `icon` when it is a URL, otherwise `dangerouslySetInnerHTML` for inline svg — mirror how `AutomationWorkflowProjectList` renders component icons), footer `Button` "Use template".

- [ ] **Step 3: Run — PASS. `npm run check`. Commit** `732 client - Add Automation Hub queries and the Templates view`

---

### Task 10: My Automations view

**Files:**
- Create: `…/automation-hub/views/AutomationsView.tsx`, `…/views/components/AutomationRow.tsx`
- Test: `…/automation-hub/tests/AutomationsView.test.tsx`

- [ ] **Step 1: Failing test** — mock queries/mutations (hoisted `vi.fn`s): rows for a COPY (enabled) and a REFERENCE (dangling); assert: label + "Needs attention" badge on the dangling row, "Open in builder" present only on the COPY row menu, toggling the switch calls `setEnabledMutate({enabled: false, workflowUuid})`, delete on COPY calls `deleteAutomationMutate('copy-uuid')` after confirming, delete on REFERENCE calls `deprovisionMutate('cat-uuid')`, "New automation" button hidden when `tabs.newWorkflow=false`, clicking it calls `createBlankMutate` and then `navigate('/embedded/hub/builder/<uuid>')`; empty state links to Templates.

- [ ] **Step 2: Implement**

`AutomationsView`: `useNavigate`, store `tabs.newWorkflow`, `useGetMyAutomationsQuery`, mutations; header row with title + `New automation` button (`useCreateBlankAutomationMutation({onSuccess: (uuid) => navigate(`/embedded/hub/builder/${uuid}`)})`); table (`Table` from `@/components/ui/table`) columns Name / Apps / Status / Enabled / actions; each row `AutomationRow`.

`AutomationRow`: `Switch checked={automation.enabled}` → `setEnabled`; badge: `dangling ? 'Needs attention' : enabled ? 'Enabled' : 'Disabled'`; `DropdownMenu` with **Open in builder** (`kind === 'COPY'` → `navigate(`/embedded/hub/builder/${workflowUuid}`)`), **Delete** (opens `AlertDialog`; on confirm `kind === 'COPY' ? deleteAutomation(workflowUuid) : deprovision(catalogWorkflowUuid ?? workflowUuid)`).

- [ ] **Step 3: Run — PASS. Commit** `732 client - Add the My Automations view`

---

### Task 11: Connections view

**Files:**
- Create: `…/automation-hub/views/ConnectionsView.tsx`, `…/views/components/HubConnectionDialog.tsx`
- Test: `…/automation-hub/tests/ConnectionsView.test.tsx`

**Interfaces:**
- Produces: `HubConnectionDialog({componentName, existingConnectionId?, onCreated?: (id: number) => void, onClose, triggerNode?})` — a wrapper over `@/shared/components/connection/ConnectionDialog` that supplies `componentDefinition` (via `useGetComponentDefinitionQuery({componentName})`), `componentDefinitions` (via `useGetComponentDefinitionsQuery({})`), `connectionTagsQueryKey`/`connectionsQueryKey` = hub keys, `useGetConnectionTagsQuery = () => useQuery({queryKey: ['automationHub','tags'], queryFn: async () => []})`, and `useCreateConnectionMutation` mapping `ConnectionI` → `createFrontendConnection({componentName, createConnectionRequest: {authorizationType: connection.authorizationType, connectionVersion: connection.connectionVersion, name: connection.name, parameters: connection.parameters}})`; when `existingConnectionId` is set, `useUpdateConnectionMutation` maps to `reauthorizeFrontendConnection({id, reauthorizeConnectionRequest: {parameters: connection.parameters}})` and the dialog is opened with `connection` prefilled (`{componentName, connectionVersion, name, parameters: {}}`).

- [ ] **Step 1: Failing test** — mock `useGetMyConnectionsQuery` (two connections: slack, hubspot) and `useGetComponentDefinitionsQuery` (titles/icons); assert rows show connection name + component title; delete → confirm → `deleteHubConnectionMutate(1)`; when the delete mutation rejects with a `Response` whose json is `{reason: 'CONNECTION_IS_USED'}`, the inline message "This connection is still used by an enabled automation." appears; "Reconnect" opens `HubConnectionDialog` (assert by a mocked dialog receiving `existingConnectionId`).

- [ ] **Step 2: Implement `ConnectionsView`** — table Name / App / Created; row menu Reconnect / Delete; `HubConnectionDialog` mounted for `reconnecting` state; delete error parsed via `error instanceof Response ? await error.json() : null` (the generated client throws `ResponseError` with `.response`; check `runtime.ts` and read `error.response.json()`).

- [ ] **Step 3: Implement `HubConnectionDialog`** per the interface above.

- [ ] **Step 4: Run — PASS. Commit** `732 client - Add the Connections view with reconnect and delete`

---

### Task 12: Activation wizard reducer

**Files:**
- Create: `…/automation-hub/wizard/activationReducer.ts`
- Test: `…/automation-hub/tests/activationReducer.test.ts`

**Interfaces:**
```ts
export type ActivationStepType = 'connect' | 'configure' | 'activate' | 'done';
export interface ActivationStateI {
    error?: string;
    highlightedComponent?: string;
    kind: 'COPY' | 'REFERENCE';
    requiredComponents: string[];           // component names that need a connection
    selections: Record<string, number | undefined>;
    step: ActivationStepType;
    workflowUuid?: string;                  // copy uuid (COPY) or catalog uuid (REFERENCE)
}
export type ActivationActionType =
    | {type: 'SELECT_CONNECTION'; componentName: string; connectionId: number}
    | {type: 'NEXT'} | {type: 'BACK'}
    | {type: 'COPIED'; workflowUuid: string} | {type: 'PROVISIONED'; workflowUuid: string}
    | {type: 'MISSING_CONNECTION'; componentName: string}
    | {type: 'ACTIVATED'} | {type: 'FAILED'; error: string};
export function initialActivationState(kind, requiredComponents): ActivationStateI;   // step 'connect', or 'configure' when requiredComponents is empty
export function activationReducer(state, action): ActivationStateI;
export function canProceed(state): boolean;  // connect: every required component selected; configure: workflowUuid set; activate: true
```

- [ ] **Step 1: Failing tests** — (a) initial state skips connect when no required components; (b) `NEXT` on connect is a no-op until all selections exist; (c) `MISSING_CONNECTION` from configure returns to `connect` with `highlightedComponent` and clears `workflowUuid`; (d) `COPIED` sets `workflowUuid` and stays on configure; (e) `NEXT` from configure with `workflowUuid` → `activate`; (f) `ACTIVATED` → `done`; (g) `FAILED` sets `error` without changing step; (h) `SELECT_CONNECTION` clears `highlightedComponent` for that component.

- [ ] **Step 2: Implement** the pure reducer accordingly (switch on `action.type`; `NEXT` uses `canProceed`).

- [ ] **Step 3: Run — PASS. Commit** `732 client - Add the template activation reducer`

---

### Task 13: Activation wizard UI

**Files:**
- Create: `…/automation-hub/wizard/ActivationWizard.tsx`, `…/wizard/ConnectAccountsStep.tsx`, `…/wizard/ConfigureStep.tsx`, `…/wizard/ActivateStep.tsx`, `…/wizard/useActivationFlow.ts`
- Modify: `…/views/TemplatesView.tsx` (open the wizard)
- Test: `…/automation-hub/tests/ActivationWizard.test.tsx`

**Interfaces:**
- `ActivationWizard({kind, onClose, template}: {kind: 'COPY'|'REFERENCE'; onClose: () => void; template: AutomationWorkflowProjectWorkflowTemplate})`.
- `useActivationFlow(template, kind)` owns the reducer + mutations and exposes `{state, dispatch, connect(componentName), configure(), activate(), openInBuilder()}`.

- [ ] **Step 1: Failing tests** — mock the mutations module + `useGetComponentConnectionsQuery` + `useGetComponentDefinitionQuery` (slack has a connection definition; a `math` component does not) + `useGetMyWorkflowQuery`:
  - COPY happy path: step 1 shows only "Slack"; select an existing connection → Next; step 2 calls `copyTemplateMutate('tpl-1')` (resolves `'copy-1'`), then `wireNodeConnectionMutate` for each slack node in the copied definition (`useGetMyWorkflowQuery('copy-1')` returns a definition with one `slack/v1/sendMessage` task whose `connections[0].key === 'slack'`), shows "Slack → My Slack" in the wired-connections summary → Next; step 3 Activate → `publishAutomationMutate('copy-1')` then `setEnabledMutate({enabled: true, workflowUuid: 'copy-1'})` → done screen with "Open in builder".
  - REFERENCE 409 loop: `provisionReferenceMutate` rejects with a `ResponseError` (status 409, json `{missingConnectionComponentName:'slack'}`) → wizard is back on step 1 with the Slack row highlighted (`aria-invalid` / "Connect Slack to continue" text); after selecting, configure calls `deprovisionReferenceMutate('tpl-1')` then `provisionReferenceMutate('tpl-1')` again; step 3 activate → `setEnabledMutate` only (no publish); done screen has no "Open in builder".
  - "Connect" button opens `HubConnectionDialog` (mocked) with the row's `componentName`; `onCreated(id)` dispatches `SELECT_CONNECTION`.

- [ ] **Step 2: Implement `useActivationFlow`**

```ts
export function useActivationFlow(template, kind) {
    const requiredComponents = useRequiredComponents(template);   // template.components filtered by componentDefinition.connection presence (useQueries over useGetComponentDefinitionQuery)
    const [state, dispatch] = useReducer(activationReducer, initialActivationState(kind, requiredComponents));
    const navigate = useNavigate();
    const copyTemplate = useCopyTemplateMutation(); /* + provision, deprovision, wire, publish, setEnabled */
    const {data: copiedWorkflow} = useGetMyWorkflowQuery(kind === 'COPY' ? state.workflowUuid : undefined);

    const configure = useCallback(async () => {
        try {
            if (kind === 'COPY') {
                const workflowUuid = await copyTemplate.mutateAsync(template.id!);
                dispatch({type: 'COPIED', workflowUuid});
            } else {
                if (state.highlightedComponent) {           // a previous 409 left a disabled row behind
                    await deprovision.mutateAsync(template.id!);
                }
                await provision.mutateAsync(template.id!);
                dispatch({type: 'PROVISIONED', workflowUuid: template.id!});
            }
        } catch (error) {
            const missing = await readMissingConnection(error);   // ResponseError 409 → componentName | undefined
            if (missing) { dispatch({type: 'MISSING_CONNECTION', componentName: missing}); return; }
            dispatch({type: 'FAILED', error: toMessage(error)});
        }
    }, [...]);

    // wire connections once the copied definition is loaded (COPY only)
    useEffect(() => {
        if (kind !== 'COPY' || !copiedWorkflow?.definition || !state.workflowUuid) return;
        const definition = JSON.parse(copiedWorkflow.definition);
        for (const task of [...(definition.triggers ?? []), ...(definition.tasks ?? [])]) {
            const componentName = task.type.split('/')[0];
            const connectionId = state.selections[componentName];
            for (const connection of task.connections ?? []) {
                if (connectionId) wire.mutate({connectionId, workflowConnectionKey: connection.key, workflowNodeName: task.name, workflowUuid: state.workflowUuid});
            }
        }
    }, [copiedWorkflow, kind, state.selections, state.workflowUuid]);

    const activate = useCallback(async () => {
        try {
            if (kind === 'COPY') await publish.mutateAsync(state.workflowUuid!);
            await setEnabled.mutateAsync({enabled: true, workflowUuid: state.workflowUuid!});
            dispatch({type: 'ACTIVATED'});
        } catch (error) { dispatch({type: 'FAILED', error: toMessage(error)}); }
    }, [...]);

    const openInBuilder = () => navigate(`/embedded/hub/builder/${state.workflowUuid}`);
    return {activate, configure, dispatch, openInBuilder, state};
}
```

Task `connections` in the definition may not carry a `key` for every component; when absent, use `componentName` as the key (this is what the builder does when it auto-wires the first connection).

- [ ] **Step 3: Implement the four components** — `ActivationWizard` (`Dialog` open, stepper header showing Connect → Configure → Activate, body switching on `state.step`, inline `Alert` for `state.error`, footer Back/Next/Activate/Done); `ConnectAccountsStep` (rows with `Select` over `useGetComponentConnectionsQuery(componentName)` + `HubConnectionDialog` trigger; highlighted row styled and labeled); `ConfigureStep` (calls `configure()` on mount if `!state.workflowUuid`; shows a spinner while the copy/provision runs, then the template description and a "Connected accounts" list — one line per required component: `<component title> → <selected connection name>` (names from `useGetComponentConnectionsQuery`); no inputs form in v1, see spec §4); `ActivateStep` (summary list; `done` state renders success + buttons).

- [ ] **Step 4: Wire `TemplatesView`** to render `<ActivationWizard>` for the selected template. Run tests, `npm run check`. Commit `732 client - Add the template activation wizard`.

---

### Task 14: `HubBuilderView` and builder context injection

**Files:**
- Create: `…/automation-hub/HubBuilderView.tsx`, `…/automation-hub/hubBuilderContext.ts`
- Modify: `client/src/ee/pages/embedded/workflow-builder/hooks/useWorkflowBuilder.ts`
- Test: `…/automation-hub/tests/HubBuilderView.test.tsx`

**Interfaces:**
- `HubBuilderContext = createContext<{connectionDialogAllowed: boolean; includeComponents?: string[]; sharedConnectionIds: number[]} | null>(null)`.
- `useWorkflowBuilder()` reads `useContext(HubBuilderContext)`; when non-null it skips `useEmbedHandshake` (the hub already did it), sets `initialized=true`, and takes the three values from context. Otherwise unchanged.

- [ ] **Step 1: Failing test** — render `HubBuilderView` under `MemoryRouter` at `/embedded/hub/builder/wf-1` with `WorkflowBuilder` mocked to a stub that renders `useContext(HubBuilderContext)?.sharedConnectionIds.join(',')`; store `sharedConnectionIds=[3,4]`; assert "3,4" is rendered, the top bar shows a back button that navigates to `/embedded/hub/automations` and invalidates `AutomationHubKeys.automations` (spy on `queryClient.invalidateQueries`).

- [ ] **Step 2: Implement**

```tsx
const HubBuilderView = () => {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const {connectionDialogAllowed, includeComponents, sharedConnectionIds} = useAutomationHubStore(useShallow(...));
    const {workflowUuid} = useParams();
    const {data: automation} = useGetMyWorkflowQuery(workflowUuid);

    const handleBack = () => {
        queryClient.invalidateQueries({queryKey: AutomationHubKeys.automations});
        navigate('/embedded/hub/automations');
    };

    return (
        <HubBuilderContext.Provider value={{connectionDialogAllowed, includeComponents, sharedConnectionIds}}>
            <div className="flex size-full flex-col">
                <div className="flex items-center gap-2 border-b px-4 py-2">
                    <Button aria-label="Back to automations" onClick={handleBack} size="icon" variant="ghost"><ArrowLeftIcon /></Button>
                    <span className="text-sm font-medium">{automation?.label}</span>
                </div>
                <div className="relative flex-1"><WorkflowBuilder /></div>
            </div>
        </HubBuilderContext.Provider>
    );
};
```

`useWorkflowBuilder` change:
```ts
    const hubContext = useContext(HubBuilderContext);
    ...
    useEmbedHandshake((params) => { if (hubContext) return; /* existing */ });
    useEffect(() => {
        if (hubContext) {
            setConnectionDialogAllowed(hubContext.connectionDialogAllowed);
            setIncludeComponents(hubContext.includeComponents);
            setSharedConnectionIds(hubContext.sharedConnectionIds);
            setInitialized(true);
        }
    }, [hubContext, ...setters]);
```
(`useEmbedHandshake` still posts `EMBED_READY` a second time on mount; the SDK component answers with another `EMBED_INIT`, which the layout's `initialize` handles idempotently. If that double handshake proves noisy, give `useEmbedHandshake` an `enabled` flag and pass `!hubContext`.)

- [ ] **Step 3: Run — PASS. Manual check**: with the server running and a JWT, open the hub via the SDK test app (Task 15) → activate a template → Open in builder → the editor renders inside the hub. Commit `732 client - Open the embedded builder inside the Automation Hub`.

---

## Phase 3 — SDK

### Task 15: `AutomationHub` SDK component, export, docs, test app

**Files:**
- Create: `sdks/frontend/embedded/library/src/components/automation-hub/AutomationHub.tsx`, `…/index.ts`, `…/AutomationHub.test.tsx`
- Modify: `sdks/frontend/embedded/library/src/main.ts`, `sdks/frontend/embedded/library/README.md`
- Create: `sdks/frontend/embedded/test-apps/app/hub/page.tsx`

- [ ] **Step 1: Failing test**

```tsx
describe('AutomationHub', () => {
    it('renders the hub iframe and answers EMBED_READY with EMBED_INIT params', () => {
        const {container} = render(
            <AutomationHub baseUrl="https://app.example" className="h-96" environment="STAGING" jwtToken="jwt-1"
                tabs={{connections: false}} theme={{primaryColor: '#123456'}} />
        );
        const iframe = container.querySelector('iframe')!;
        expect(iframe.getAttribute('src')).toBe('https://app.example/embedded/hub');
        expect(container.firstElementChild).toHaveClass('h-96');

        const postMessage = vi.fn();
        Object.defineProperty(iframe, 'contentWindow', {value: {postMessage}});

        act(() => { window.dispatchEvent(new MessageEvent('message', {data: {type: 'EMBED_READY'}, origin: 'https://app.example'})); });

        expect(postMessage).toHaveBeenCalledWith(
            {params: {connectionDialogAllowed: true, environment: 'STAGING', includeComponents: undefined, jwtToken: 'jwt-1',
                sharedConnectionIds: [], tabs: {connections: false}, theme: {primaryColor: '#123456'}}, type: 'EMBED_INIT'},
            'https://app.example');
    });

    it('ignores EMBED_READY from another origin', () => { /* origin https://evil.example → postMessage not called */ });
});
```

- [ ] **Step 2: Implement** — copy `EmbeddedWorkflowBuilder.tsx`, rename, props per spec §2, `propsRef` holds `{connectionDialogAllowed, environment, includeComponents, jwtToken, sharedConnectionIds, tabs, theme}` with defaults `connectionDialogAllowed = true`, `environment = 'PRODUCTION'`, `sharedConnectionIds = []`; iframe `src={`${baseUrl}/embedded/hub`}`, wrapper `<div className={className}>` (default `undefined`), `title="Automation Hub"`. Export the `AutomationHubTabsConfig` / `AutomationHubTheme` types. `index.ts`: `export {default} from './AutomationHub'; export type {AutomationHubTabsConfig, AutomationHubTheme} from './AutomationHub';`. `main.ts`: add `import AutomationHub from './components/automation-hub'; export {AutomationHub}; export type {AutomationHubTabsConfig, AutomationHubTheme} from './components/automation-hub';`.

- [ ] **Step 3: Run `cd sdks/frontend/embedded/library && npm test -- --run && npm run lint && npm run build`** — PASS.

- [ ] **Step 4: README section** under the `EmbeddedWorkflowBuilder` docs:

```md
### AutomationHub

Embeds the end-user Automation Hub (Templates, My Automations, Connections, and the workflow builder) in an iframe.

```tsx
<AutomationHub
    baseUrl="https://app.bytechef.io"
    className="h-[800px] w-full"
    environment="PRODUCTION"
    jwtToken={jwtToken}
    tabs={{connections: true, newWorkflow: false}}
    theme={{mode: 'light', primaryColor: '#2563eb', borderRadius: '0.5rem'}}
/>
```

`theme.fontFamily` must be a font the iframe can load (system/web-safe); host-page `@font-face` does not cross the iframe boundary.
```

- [ ] **Step 5: Test app page** `test-apps/app/hub/page.tsx` — reuse the JWT-minting form from `app/page.tsx` (extract the token form into `app/components/TokenForm.tsx` if it is not already a component) and render `<AutomationHub baseUrl={baseUrl} className="h-[85vh] w-full border" environment={environment} jwtToken={jwtToken} />` once a token exists; add a link to `/hub` on the home page.

- [ ] **Step 6: Manual verification** — server running (`./gradlew -p server/apps/server-app bootRun`), client `npm run dev`, SDK `npm run dev`; in the test app mint a JWT, open `/hub`, confirm: tabs render, a published catalog project shows in Templates, wizard activates a COPY (publish + enable), My Automations lists it, Connections lists the connection created in the wizard, Open in builder loads the editor, Back returns to My Automations. Then commit:

```bash
git add sdks/frontend/embedded
git commit -m "732 client - Add the AutomationHub embedded SDK component"
```

---

### Task 16: Sample app — "Automation Hub" tab (separate repo)

**Repo:** `/Volumes/Data/bytechef/bytechef-samples` (branch `master`), app `bytechef-embedded-sample-app/front-end`. It consumes the SDK through **yalc** (`"@bytechef/embedded": "file:.yalc/@bytechef/embedded"`), so the SDK must be rebuilt and pushed first — plain SDK edits never reach it.

**Files:**
- Modify: `bytechef-embedded-sample-app/front-end/src/app/layout.tsx` (the `navigation` array)
- Create: `bytechef-embedded-sample-app/front-end/src/app/automation-hub/page.tsx`

**Interfaces:**
- Consumes: `AutomationHub` from `@bytechef/embedded` (Task 15) and `getToken()` from `@/lib/api` (already used by `src/app/automations/[workflowUuid]/page.tsx`).

- [ ] **Step 1: Publish the SDK build into the sample app**

```bash
cd /Volumes/Data/bytechef/bytechef/sdks/frontend/embedded/library && npm run build && yalc push
ls -ld /Volumes/Data/bytechef/bytechef-samples/bytechef-embedded-sample-app/front-end/node_modules/@bytechef/embedded   # must be a real dir (drwx), not a symlink
grep -c "AutomationHub" /Volumes/Data/bytechef/bytechef-samples/bytechef-embedded-sample-app/front-end/node_modules/@bytechef/embedded/dist/main.d.ts   # ≥ 1
```

If `yalc push` reports no target, re-init once: `yalc publish` (in the SDK) then `yalc add @bytechef/embedded` (in `front-end`). Never `yalc link` — it symlinks outside the project root and Turbopack refuses to resolve it.

- [ ] **Step 2: Add the sidebar item** — in `layout.tsx` `navigation`, directly after `Automations`:

```ts
  { name: 'Automation Hub', href: '/automation-hub', icon: LayoutGridIcon },
```

and add `LayoutGridIcon` to the `lucide-react` import.

- [ ] **Step 3: Add the page** (server component, same shape as `automations/[workflowUuid]/page.tsx`):

```tsx
import { AutomationHub } from "@bytechef/embedded";
import { getToken } from "@/lib/api";

export default async function AutomationHubPage() {
  const jwtToken = await getToken();

  return (
    <div className="absolute inset-0 lg:pl-72">
      <AutomationHub
        baseUrl={`${process.env.BYTECHEF_APP_BASE_URL ?? 'http://127.0.0.1:5173'}`}
        className="size-full"
        connectionDialogAllowed={true}
        environment={'DEVELOPMENT'}
        jwtToken={jwtToken}
        sharedConnectionIds={[1072]}
        theme={{ primaryColor: '#111827' }}
      />
    </div>
  );
}
```

The `absolute inset-0 lg:pl-72` wrapper lives **here** (it matches this app's fixed 72-wide sidebar) — that is exactly the layout concern the SDK component no longer hardcodes.

- [ ] **Step 4: Verify** — `cd front-end && npm run dev`; open the app, click **Automation Hub**: the iframe loads `/embedded/hub`, the tab strip renders, Templates lists the DEVELOPMENT catalog, activating a template and opening it in the builder both work inside the tab. If Turbopack serves a stale bundle after `yalc push`, delete `.next` and restart.

- [ ] **Step 5: Commit in the samples repo**

```bash
cd /Volumes/Data/bytechef/bytechef-samples
git add bytechef-embedded-sample-app/front-end/src/app/layout.tsx bytechef-embedded-sample-app/front-end/src/app/automation-hub/page.tsx
git commit -m "Add Automation Hub tab to the embedded sample app"
```

(`.yalc/` and `yalc.lock` are gitignored there; do not stage `package-lock.json` unless yalc changed it — check `git diff` first.)

---

## Self-review checklist (run before handing off)

- Spec §1 entry/routing/SPA filter → Tasks 5, 8. §2 SDK → Task 15. §3 views → Tasks 9-11, 14. §4 wizard → Tasks 12-13 (incl. publish-before-enable and de-provision-before-retry). §5 server → Tasks 1-4 (all six routes + schema fields + reference rows). §6 theming → Task 8. §7 testing → each task. §8 out of scope respected (no logs, no standalone create, no host events).
- Names used across tasks: `useEmbedHandshake`/`EmbedInitParamsI` (7, 8, 14), `useAutomationHubStore`/`AutomationHubTabsI`/`AutomationHubThemeI` (8-14), `AutomationHubKeys` + hook names (9-14), `HubConnectionDialog` (11, 13), `activationReducer` types (12, 13), `HubBuilderContext` (14), generated method names from Task 1's `operationId`s (6, 9-13).

---

## Revision 2026-08-18 — plan amendments for the two-view design

The spec gained a **"Revision 2026-08-18 — two views, usage state on the template card"** section,
directed by the product owner after Tasks 1–10 had landed. The spec is the binding authority; this
section records what that means for the tasks below, which were written against the superseded
three-tab design. **Where a task above conflicts with this section, this section wins.**

### Tasks already executed, and what changed

| Task | Status |
|---|---|
| 1–7 | Unaffected. |
| 8 | Amended by R2 below: `AutomationHubTabsI` drops `templates`; the layout renders two tabs; the `/embedded/hub/automations` route is removed and the index route is the merged view. |
| 9 | `TemplatesView` became `TemplateGridSection`, a section of the merged view rather than a route. Its grouping, search, loading/error and empty states are unchanged. The queries and mutations it introduced are unchanged. |
| 10 | Built and committed as specified (`47aee86cbfc`), then **superseded**: its `AutomationRow` is reused as the "Your automations" section instead of backing its own tab. It was deliberately not reviewed as a standalone view. |

### Task R1 (new, server) — expose `copiedFromWorkflowUuid`

Matching a `COPY` automation back to its source template needs the source uuid on the public API.
`copied_from_workflow_uuid` already exists on `connected_user_project_workflow` and is already written by
`ConnectedUserProjectFacadeImpl#copyWorkflowTemplate` for every copy the wizard creates; it was simply
absent from the public schema. Added through the same path as `kind`/`catalogWorkflowUuid`/`dangling`
(commit `2a4ad6dd4fd`): `openapi.yaml` → `ConnectedUserProjectWorkflowDTO` → `ConnectUserProjectWorkflowMapper`
→ regenerate Java + TypeScript. No new table, column or changeset. Landed as `45f71841d1a`.

### Task R2 (new, client) — merge the hub into two views

Brief: `.superpowers/sdd/2026-08-17-embedded-automation-hub/task-R2-brief.md`. Landed as `02ddbbec86b`
plus a fix round. Shape:

- **Automations** at `/embedded/hub` (index) = a template grid with per-card usage state, then a
  "Your automations" section for automations matching no published template. **Connections** at
  `/embedded/hub/connections`. No other tabs.
- Card states: *unused* → **Use template** (opens the wizard); *activated* → an enable/disable toggle
  plus a ⋮ menu holding **Customize** (COPY only — a REFERENCE is never editable) and **Remove**
  (confirm, then delete the copy or de-provision the reference by kind).
- Matching: `REFERENCE` → `catalogWorkflowUuid`; `COPY` → `copiedFromWorkflowUuid`; guarded by
  `!dangling`. The card represents the first match; **additional automations matching the same template
  fall through to "Your automations"** so nothing is ever hidden or unremovable. The copy endpoint's
  always-create-a-new-copy contract is deliberately untouched — the sync bridge's dedup depends on it.

### Amendments to the remaining tasks

- **Task 11 (Connections view)** — unaffected.
- **Tasks 12–13 (activation wizard)** — unaffected in substance; the wizard is launched from an unused
  template card in the merged view rather than from a Templates tab.
- **Task 14 (`HubBuilderView`)** — back navigation returns to **`/embedded/hub`**, not
  `/embedded/hub/automations`, and invalidates `AutomationHubKeys.automations` as before.
- **Task 15 (SDK component)** — `AutomationHubTabsConfig` drops `templates`; the remaining keys are
  `automations`, `connections` and `newWorkflow`. The README example must not show a `templates` key.
- **Task 16 (sample app)** — unaffected beyond inheriting the SDK's prop shape.

### Self-review checklist amendment

The checklist's "§3 views → Tasks 9-11, 14" line now reads: §3 views → Tasks 9/R2 (merged Automations
view), 11 (Connections), 14 (builder). Spec §5's server surface is Tasks 1–4 **plus R1**.
