# Environment Promotion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a user promote an API collection, MCP server, or A2A server from one environment to another (create the counterpart on first promotion, sync it on re-promotion) with connections re-bound to the target environment.

**Architecture:** A new EE module `server/ee/libs/automation/automation-promotion` (`-api`/`-service`/`-graphql`) exposes one generic GraphQL query+mutation over `PromotionResourceType {API_COLLECTION, MCP_SERVER, A2A_SERVER}`, dispatching to one `EnvironmentPromotionHandler` bean per resource type. Handlers reuse each surface's existing create path to mint the target row + its synthetic `ProjectDeployment`, then a shared `ProjectDeploymentPromoter.sync` reconciles the deployment (version, per-workflow connections/inputs) by `project_workflow.uuid` through `ProjectDeploymentFacade.updateProjectDeployment`, which updates `project_deployment_workflow` rows in place so the mapping rows' FKs stay valid. Lineage across environments is a new `uuid` column on `api_collection`, `mcp_server`, `a2a_server`. One EE client dialog (`EnvironmentPromotionDialog`) drives preview → connection picks → promote.

**Tech Stack:** Java 25 / Spring Boot 4 / Spring Data JDBC / Liquibase / Spring GraphQL / JUnit 5 + Mockito / Testcontainers; React 19 + TypeScript + TanStack Query + graphql-codegen + Vitest.

**Spec:** `docs/superpowers/specs/2026-08-17-environment-promotion-design.md`

## Global Constraints

- All new server files under `server/ee/` carry the ByteChef Enterprise license header and a `@version ee` Javadoc tag; beans are `@ConditionalOnEEVersion` (`com.bytechef.platform.annotation.ConditionalOnEEVersion`).
- Enum ordinals are persisted as INT — never reorder; `Environment {DEVELOPMENT=0, STAGING=1, PRODUCTION=2}`; "environmentId" everywhere is the ordinal.
- Released changelogs are never edited: `api_collection` and `mcp_server` get NEW changesets; the `a2a_server` init changelog is unreleased (not on `master`) and is edited in place; run `scripts/dev/sync-local-schema-after-collapse.sh` for local DBs after the init edit.
- Java style: one blank line before control statements, one blank line after a variable modification and its use, no trailing blank line before `}`, descriptive variable names, no method-chaining beyond builders/streams/Optional; test method names camelCase without underscores; test class names end in `Test` (unit) / `IntTest` (integration), no `Impl` in test names.
- Client: interface names end in `I`/`Props`; object keys sorted; icons imported with `Icon` suffix; `twMerge` not `cn`; hooks ordered `useState → useRef → stores → custom hooks → derived → useEffect → return`; CE code never statically imports `@/ee/`.
- Commit messages: server `732 <description>`, client `732 client - <description>`. Stage only files you touched; after each commit run `git show --name-only HEAD` and confirm the file list is yours (the user commits in parallel on this branch). Never `--amend`.
- Never judge a Gradle run through a pipe: redirect to a file, check `$?` on its own line, then grep `^> Task .* FAILED`.
- Run `./gradlew spotlessApply` before every server commit; `cd client && npm run check` before every client commit.
- Sync-vs-local rule (spec §4 ⚑3): after the first promotion only the exposed surface syncs (project version, endpoint/tool/skill SET + mapping metadata, `contextPath`/`collectionVersion`); name, description, tags, enabled flags, auth settings, secret keys, and connection bindings / inputs already present in the target are environment-local and never overwritten.

---

## Phase 0 — Prerequisites (independent; can run in parallel)

### Task 1: A2A projects inherit the server's environment

**Files:**
- Modify: `server/libs/automation/automation-ai/automation-ai-a2a/automation-ai-a2a-service/src/main/java/com/bytechef/automation/ai/a2a/facade/A2aProjectFacadeImpl.java:65-89`
- Test: `server/libs/automation/automation-ai/automation-ai-a2a/automation-ai-a2a-service/src/test/java/com/bytechef/automation/ai/a2a/facade/A2aProjectFacadeTest.java` (create if absent)

**Interfaces:**
- Consumes: `A2aServerService.getA2aServer(long)` → `A2aServer` (`getEnvironment()`).
- Produces: `createA2aProject` mints its deployment in the owning server's environment (Task 12 relies on this).

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.automation.ai.a2a.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.service.A2aProjectService;
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowService;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class A2aProjectFacadeTest {

    private final A2aProjectService a2aProjectService = mock(A2aProjectService.class);
    private final A2aProjectWorkflowService a2aProjectWorkflowService = mock(A2aProjectWorkflowService.class);
    private final A2aServerService a2aServerService = mock(A2aServerService.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);

    @Test
    void testCreateA2aProjectUsesServerEnvironment() {
        A2aServer a2aServer = new A2aServer("prod-agent", null, PlatformType.AUTOMATION, Environment.PRODUCTION);

        a2aServer.setId(7L);

        when(a2aServerService.getA2aServer(7L)).thenReturn(a2aServer);

        ProjectDeployment savedProjectDeployment = new ProjectDeployment();

        savedProjectDeployment.setId(100L);

        when(projectDeploymentService.create(any())).thenReturn(savedProjectDeployment);
        when(a2aProjectService.create(100L, 7L)).thenReturn(new A2aProject());

        A2aProjectFacadeImpl a2aProjectFacade = new A2aProjectFacadeImpl(
            a2aProjectService, a2aProjectWorkflowService, a2aServerService, projectDeploymentService,
            projectDeploymentWorkflowService);

        a2aProjectFacade.createA2aProject(7L, 42L, 3, List.of());

        ArgumentCaptor<ProjectDeployment> captor = ArgumentCaptor.forClass(ProjectDeployment.class);

        verify(projectDeploymentService).create(captor.capture());

        assertThat(captor.getValue()
            .getEnvironment()).isEqualTo(Environment.PRODUCTION);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-a2a:automation-ai-a2a-service:test --tests '*A2aProjectFacadeTest*' > /tmp/t1.log 2>&1; echo $?`
Expected: non-zero (compilation error — constructor has no `A2aServerService` parameter).

- [ ] **Step 3: Inject `A2aServerService` and use the server's environment**

In `A2aProjectFacadeImpl`: add a `private final A2aServerService a2aServerService;` field, add it as the third constructor parameter (keep alphabetical order of the existing fields), and replace

```java
        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
```
with
```java
        A2aServer a2aServer = a2aServerService.getA2aServer(a2aServerId);

        projectDeployment.setEnvironment(a2aServer.getEnvironment());
```
(placed before the `new ProjectDeployment()` block, with the blank-line rules). Remove the now-unused `Environment` import if nothing else uses it.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-a2a:automation-ai-a2a-service:test --tests '*A2aProjectFacadeTest*' > /tmp/t1.log 2>&1; echo $?` then `grep -c "FAILED" /tmp/t1.log`
Expected: exit 0, no FAILED.

- [ ] **Step 5: Fix any hand-assembled test contexts**

Run: `grep -rln "A2aProjectFacadeImpl" server --include="*.java" | grep -v build`
For every `@TestConfiguration`/IntTest that `new`s or wires the impl, add the `A2aServerService` collaborator (mock or real bean).

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply > /tmp/sp.log 2>&1; echo $?
git add server/libs/automation/automation-ai/automation-ai-a2a/automation-ai-a2a-service/src/main/java/com/bytechef/automation/ai/a2a/facade/A2aProjectFacadeImpl.java server/libs/automation/automation-ai/automation-ai-a2a/automation-ai-a2a-service/src/test/java/com/bytechef/automation/ai/a2a/facade/A2aProjectFacadeTest.java
git commit -m "732 Create A2A project deployments in the owning server's environment"
git show --name-only HEAD
```

---

### Task 2: `api_collection.uuid`, drop the global name constraint, app-level per-environment name check

**Files:**
- Create: `server/ee/libs/automation/automation-api-platform/automation-api-platform-configuration/automation-api-platform-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/20260817000001_automation_api_platform_added_column_uuid.xml`
- Modify: `…/automation-api-platform-configuration-api/src/main/java/com/bytechef/ee/automation/apiplatform/configuration/domain/ApiCollection.java`
- Modify: `…/automation-api-platform-configuration-api/…/dto/ApiCollectionDTO.java`
- Modify: `…/automation-api-platform-configuration-api/…/exception/ApiCollectionErrorType.java`
- Modify: `…/automation-api-platform-configuration-api/…/service/ApiCollectionService.java`
- Modify: `…/automation-api-platform-configuration-service/…/repository/CustomApiCollectionRepository.java` + `CustomApiCollectionRepositoryImpl.java`
- Modify: `…/automation-api-platform-configuration-service/…/service/ApiCollectionServiceImpl.java`
- Modify: `…/automation-api-platform-configuration-service/…/facade/ApiCollectionFacadeImpl.java:109-141`
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/ee/automation/ai/tool/CloneApiCollectionToolCallback.java:29-30,62-63` (doc only)
- Test: `…/automation-api-platform-configuration-service/src/test/java/com/bytechef/ee/automation/apiplatform/configuration/service/ApiCollectionServiceTest.java`
- Also: every REST mapper / OpenAPI model that builds an `ApiCollectionDTO` via the canonical constructor (grep `new ApiCollectionDTO(`) — add the new `uuid` argument.

**Interfaces:**
- Produces: `ApiCollection#getUuid(): UUID`, `#getUuidAsString(): String`, `#setUuid(UUID)`; `ApiCollectionDTO` record gains a trailing component `String uuid` (nullable on create → random); `ApiCollectionService#fetchApiCollection(UUID uuid, Environment environment): Optional<ApiCollection>`; `ApiCollectionService#existsByNameAndEnvironment(String name, long workspaceId, Environment environment, @Nullable Long excludeId): boolean`; `ApiCollectionErrorType.NAME_ALREADY_EXISTS` (key 102).

- [ ] **Step 1: Write the changeset**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">
    <changeSet id="20260817000001-1" author="Ivica Cardic">
        <addColumn tableName="api_collection">
            <column name="uuid" type="${uuid_type}"/>
        </addColumn>
    </changeSet>

    <changeSet id="20260817000001-2" author="Ivica Cardic" dbms="postgresql">
        <sql>
            UPDATE api_collection
            SET uuid = gen_random_uuid()
            WHERE uuid IS NULL;
        </sql>
    </changeSet>

    <changeSet id="20260817000001-3" author="Ivica Cardic">
        <addNotNullConstraint tableName="api_collection" columnName="uuid"/>
        <createIndex indexName="idx_api_collection_uuid" tableName="api_collection">
            <column name="uuid"/>
        </createIndex>
    </changeSet>

    <changeSet id="20260817000001-4" author="Ivica Cardic">
        <dropUniqueConstraint tableName="api_collection" constraintName="uk_api_collection_name"/>
    </changeSet>
</databaseChangeLog>
```

- [ ] **Step 2: Domain + DTO**

`ApiCollection`: add after `projectDeploymentId`
```java
    @Column("uuid")
    private UUID uuid;
```
plus
```java
    public UUID getUuid() {
        return uuid;
    }

    public String getUuidAsString() {
        return uuid == null ? null : uuid.toString();
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
```
`ApiCollectionDTO`: append a component `String uuid` at the END of the record (after `int version`); the convenience constructor passes `apiCollection.getUuidAsString()`; `toApiCollection()` adds `apiCollection.setUuid(uuid == null ? null : UUID.fromString(uuid));`. Update every `new ApiCollectionDTO(` call site (REST mapper, `CloneApiCollectionToolCallback:150-155` → pass `null`, tests) to supply the new trailing argument.

- [ ] **Step 3: Write the failing service tests**

```java
package com.bytechef.ee.automation.apiplatform.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.repository.ApiCollectionRepository;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApiCollectionServiceTest {

    private final ApiCollectionRepository apiCollectionRepository = mock(ApiCollectionRepository.class);
    private final ApiCollectionServiceImpl apiCollectionService = new ApiCollectionServiceImpl(apiCollectionRepository);

    @Test
    void testCreateAssignsUuidWhenMissing() {
        ApiCollection apiCollection = newApiCollection(null);

        when(apiCollectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ApiCollection created = apiCollectionService.create(apiCollection);

        assertThat(created.getUuid()).isNotNull();
    }

    @Test
    void testCreateKeepsProvidedUuid() {
        UUID uuid = UUID.randomUUID();
        ApiCollection apiCollection = newApiCollection(uuid);

        when(apiCollectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(apiCollectionService.create(apiCollection)
            .getUuid()).isEqualTo(uuid);
    }

    @Test
    void testFetchApiCollectionByUuidAndEnvironmentDelegatesOrdinal() {
        UUID uuid = UUID.randomUUID();
        ApiCollection apiCollection = newApiCollection(uuid);

        when(apiCollectionRepository.findByUuidAndEnvironment(uuid, 1)).thenReturn(Optional.of(apiCollection));

        assertThat(apiCollectionService.fetchApiCollection(uuid, Environment.STAGING)).contains(apiCollection);
    }

    @Test
    void testExistsByNameAndEnvironmentDelegates() {
        when(apiCollectionRepository.existsByNameAndWorkspaceIdAndEnvironment("billing", 5L, 2, 9L))
            .thenReturn(true);

        assertThat(apiCollectionService.existsByNameAndEnvironment("billing", 5L, Environment.PRODUCTION, 9L))
            .isTrue();
    }

    private static ApiCollection newApiCollection(UUID uuid) {
        ApiCollection apiCollection = new ApiCollection();

        apiCollection.setCollectionVersion(1);
        apiCollection.setName("billing");
        apiCollection.setProjectDeploymentId(11L);
        apiCollection.setUuid(uuid);

        return apiCollection;
    }
}
```

- [ ] **Step 4: Run to verify failure**

Run: `./gradlew :server:ee:libs:automation:automation-api-platform:automation-api-platform-configuration:automation-api-platform-configuration-service:test --tests '*ApiCollectionServiceTest*' > /tmp/t2.log 2>&1; echo $?`
Expected: non-zero (missing methods).

- [ ] **Step 5: Repository queries**

`CustomApiCollectionRepository` — add:
```java
    Optional<ApiCollection> findByUuidAndEnvironment(UUID uuid, int environment);

    boolean existsByNameAndWorkspaceIdAndEnvironment(
        String name, long workspaceId, int environment, @Nullable Long excludeId);
```
`CustomApiCollectionRepositoryImpl` — implement with `JdbcClient` (same style as `findAllApiCollections`):
```java
    @Override
    public Optional<ApiCollection> findByUuidAndEnvironment(UUID uuid, int environment) {
        List<ApiCollection> apiCollections = jdbcClient
            .sql("""
                SELECT api_collection.* FROM api_collection
                JOIN project_deployment ON api_collection.project_deployment_id = project_deployment.id
                WHERE api_collection.uuid = ? AND project_deployment.environment = ?
                ORDER BY api_collection.id ASC
                """)
            .params(List.of(uuid, environment))
            .query(ApiCollection.class)
            .list();

        return apiCollections.isEmpty() ? Optional.empty() : Optional.of(apiCollections.getFirst());
    }

    @Override
    public boolean existsByNameAndWorkspaceIdAndEnvironment(
        String name, long workspaceId, int environment, @Nullable Long excludeId) {

        List<Object> arguments = new ArrayList<>(List.of(name, workspaceId, environment));

        String query = """
            SELECT COUNT(api_collection.id) FROM api_collection
            JOIN project_deployment ON api_collection.project_deployment_id = project_deployment.id
            JOIN project ON project_deployment.project_id = project.id
            WHERE api_collection.name = ? AND project.workspace_id = ? AND project_deployment.environment = ?
            """;

        if (excludeId != null) {
            arguments.add(excludeId);

            query += " AND api_collection.id <> ?";
        }

        Long count = jdbcClient.sql(query)
            .params(arguments)
            .query(Long.class)
            .single();

        return count > 0;
    }
```
Note: `ApiCollectionRepository extends CustomApiCollectionRepository` already, so `apiCollectionRepository.findByUuidAndEnvironment(...)` compiles once added to the custom interface. If Spring Data tries to derive `existsByNameAndWorkspaceIdAndEnvironment` as a query method (property `workspaceId` doesn't exist on the entity and would fail at startup), the custom-repository fragment wins because the method is declared on the fragment interface — verify by starting an IntTest context in Task 15.

- [ ] **Step 6: Service + error type + facade check**

`ApiCollectionErrorType`: `public static final ApiCollectionErrorType NAME_ALREADY_EXISTS = new ApiCollectionErrorType(102);`

`ApiCollectionService`: add
```java
    Optional<ApiCollection> fetchApiCollection(UUID uuid, Environment environment);

    boolean existsByNameAndEnvironment(String name, long workspaceId, Environment environment, @Nullable Long excludeId);
```
`ApiCollectionServiceImpl.create`: before `save`, `if (apiCollection.getUuid() == null) { apiCollection.setUuid(UUID.randomUUID()); }`; implement the two new methods by delegating with `environment.ordinal()`.

`ApiCollectionFacadeImpl.createApiCollection` (before `projectDeploymentService.create`) and `updateApiCollection` (before `apiCollectionService.update`): resolve the workspace id (`projectService.getProject(projectId).getWorkspaceId()`; on update read the project through the collection's deployment) and
```java
        if (apiCollectionService.existsByNameAndEnvironment(
            apiCollection.getName(), workspaceId, environment, apiCollection.getId())) {

            throw new ConfigurationException(
                "API collection with name '%s' already exists in environment %s".formatted(
                    apiCollection.getName(), environment),
                ApiCollectionErrorType.NAME_ALREADY_EXISTS);
        }
```
(`ConfigurationException` from `com.bytechef.exception`).

- [ ] **Step 7: Doc fix on the clone tool**

In `CloneApiCollectionToolCallback` replace the two "context paths are unique per environment" sentences (class javadoc L29-30, tool description L62-63) with: "Collection names are unique per workspace and environment; the clone keeps the source name unless `newName` is supplied. Context paths carry no uniqueness constraint — the runtime dispatches by (environment, contextPath, collectionVersion, path)."

- [ ] **Step 8: Run tests, spotless, commit**

Run: `./gradlew :server:ee:libs:automation:automation-api-platform:automation-api-platform-configuration:automation-api-platform-configuration-service:test :server:ee:libs:automation:automation-ai:automation-ai-tool:compileJava --continue > /tmp/t2.log 2>&1; echo $?` then `grep "^> Task .* FAILED" /tmp/t2.log`
Expected: exit 0, no FAILED lines.

```bash
./gradlew spotlessApply > /tmp/sp.log 2>&1; echo $?
git add server/ee/libs/automation/automation-api-platform server/ee/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/ee/automation/ai/tool/CloneApiCollectionToolCallback.java
git commit -m "732 Add api_collection.uuid lineage column and per-environment name uniqueness"
git show --name-only HEAD
```

---

### Task 3: `mcp_server.uuid` + `(name, environment)` / `(uuid, environment)` constraints + create overload

**Files:**
- Create: `server/libs/platform/platform-mcp/platform-mcp-service/src/main/resources/config/liquibase/changelog/platform/mcp/20260817000001_platform_mcp_added_column_uuid.xml`
- Modify: `server/libs/platform/platform-mcp/platform-mcp-api/src/main/java/com/bytechef/platform/mcp/domain/McpServer.java`
- Modify: `server/libs/platform/platform-mcp/platform-mcp-service/src/main/java/com/bytechef/platform/mcp/repository/McpServerRepository.java`
- Modify: `server/libs/platform/platform-mcp/platform-mcp-api/src/main/java/com/bytechef/platform/mcp/service/McpServerService.java` + `…/platform-mcp-service/…/service/McpServerServiceImpl.java`
- Modify: `server/libs/automation/automation-ai/automation-ai-mcp/automation-ai-mcp-api/src/main/java/com/bytechef/automation/ai/mcp/facade/WorkspaceMcpServerFacade.java` + `…-service/…/facade/WorkspaceMcpServerFacadeImpl.java:110-131`
- Modify: `server/libs/automation/automation-ai/automation-ai-mcp/automation-ai-mcp-api/src/main/java/com/bytechef/automation/ai/mcp/facade/McpProjectFacade.java:58-77` (javadoc only)
- Test: `server/libs/platform/platform-mcp/platform-mcp-service/src/test/java/com/bytechef/platform/mcp/service/McpServerServiceTest.java` (extend or create)
- Also: any `Remote*WorkspaceMcpServerFacadeClient` stub in `server/ee/libs/**/remote-client` (grep `implements WorkspaceMcpServerFacade`).

**Interfaces:**
- Produces: `McpServer#getUuid(): UUID`, `#setUuid(UUID)`; both existing constructors assign `UUID.randomUUID()`; `McpServerService#fetchMcpServer(UUID uuid, Environment environment): Optional<McpServer>`; `WorkspaceMcpServerFacade#createWorkspaceMcpServer(String name, PlatformType type, Environment environment, Boolean enabled, Boolean authenticationRequired, Boolean enforceToolAuthorization, Long workspaceId, @Nullable UUID uuid): McpServer` (new overload; the existing 6-arg one delegates with `null, null`).

- [ ] **Step 1: Changeset**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">
    <changeSet id="20260817000001-1" author="Ivica Cardic">
        <addColumn tableName="mcp_server">
            <column name="uuid" type="${uuid_type}"/>
        </addColumn>
    </changeSet>

    <changeSet id="20260817000001-2" author="Ivica Cardic" dbms="postgresql">
        <sql>
            UPDATE mcp_server
            SET uuid = gen_random_uuid()
            WHERE uuid IS NULL;
        </sql>
    </changeSet>

    <changeSet id="20260817000001-3" author="Ivica Cardic">
        <addNotNullConstraint tableName="mcp_server" columnName="uuid"/>
        <dropUniqueConstraint tableName="mcp_server" constraintName="uk_mcp_server_name"/>
        <addUniqueConstraint constraintName="uk_mcp_server_name_environment" tableName="mcp_server" columnNames="name,environment"/>
        <addUniqueConstraint constraintName="uk_mcp_server_uuid_environment" tableName="mcp_server" columnNames="uuid,environment"/>
    </changeSet>
</databaseChangeLog>
```

- [ ] **Step 2: Failing tests**

Add to `McpServerServiceTest` (create the class with the repository mocked if it does not exist; mirror the constructor of `McpServerServiceImpl`):
```java
    @Test
    void testConstructorAssignsUuid() {
        McpServer mcpServer = new McpServer("s", PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        assertThat(mcpServer.getUuid()).isNotNull();
    }

    @Test
    void testUpdateDoesNotChangeUuidOrEnvironment() {
        McpServer current = new McpServer("s", PlatformType.AUTOMATION, Environment.DEVELOPMENT);
        UUID currentUuid = current.getUuid();

        current.setId(1L);

        when(mcpServerRepository.findById(1L)).thenReturn(Optional.of(current));
        when(mcpServerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        McpServer incoming = new McpServer("renamed", PlatformType.AUTOMATION, Environment.PRODUCTION);

        incoming.setId(1L);
        incoming.setUuid(UUID.randomUUID());

        McpServer updated = mcpServerService.update(incoming);

        assertThat(updated.getUuid()).isEqualTo(currentUuid);
        assertThat(updated.getEnvironment()).isEqualTo(Environment.DEVELOPMENT);
        assertThat(updated.getName()).isEqualTo("renamed");
    }

    @Test
    void testFetchMcpServerByUuidAndEnvironment() {
        UUID uuid = UUID.randomUUID();
        McpServer mcpServer = new McpServer("s", PlatformType.AUTOMATION, Environment.STAGING);

        when(mcpServerRepository.findByUuidAndEnvironment(uuid, 1)).thenReturn(Optional.of(mcpServer));

        assertThat(mcpServerService.fetchMcpServer(uuid, Environment.STAGING)).contains(mcpServer);
    }
```

- [ ] **Step 3: Run to verify failure**

Run: `./gradlew :server:libs:platform:platform-mcp:platform-mcp-service:test --tests '*McpServerServiceTest*' > /tmp/t3.log 2>&1; echo $?` → non-zero.

- [ ] **Step 4: Implement**

`McpServer`: field `@Column private UUID uuid;` + getter/setter; in BOTH constructors add `this.uuid = UUID.randomUUID();`.
`McpServerRepository`: `Optional<McpServer> findByUuidAndEnvironment(UUID uuid, int environment);` (derived query — both are entity properties).
`McpServerService`: `Optional<McpServer> fetchMcpServer(UUID uuid, Environment environment);` → impl `mcpServerRepository.findByUuidAndEnvironment(uuid, environment.ordinal())`. `update(McpServer)` unchanged (it copies neither `uuid` nor `environment` — the test pins that).
`WorkspaceMcpServerFacade` + impl — new overload:
```java
    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'MCP_CREATE')")
    public McpServer createWorkspaceMcpServer(
        String name, PlatformType type, Environment environment, Boolean enabled, Boolean authenticationRequired,
        Boolean enforceToolAuthorization, Long workspaceId, @Nullable UUID uuid) {

        McpServer mcpServer = new McpServer(name, type, environment);

        if (enabled != null) {
            mcpServer.setEnabled(enabled);
        }

        if (authenticationRequired != null) {
            mcpServer.setAuthenticationRequired(authenticationRequired);
        }

        if (enforceToolAuthorization != null) {
            mcpServer.setEnforceToolAuthorization(enforceToolAuthorization);
        }

        if (uuid != null) {
            mcpServer.setUuid(uuid);
        }

        mcpServer = mcpServerService.create(mcpServer);

        workspaceMcpServerService.assignMcpServerToWorkspace(mcpServer.getId(), workspaceId);

        return mcpServer;
    }
```
and make the existing 6-arg method delegate: `return createWorkspaceMcpServer(name, type, environment, enabled, authenticationRequired, null, workspaceId, null);`. Update any remote-client stub to add the overload (throw `UnsupportedOperationException`).
`McpProjectFacade.cloneMcpProject` javadoc: delete the sentence claiming the deployment "is implicitly bound to `Environment.DEVELOPMENT`"; replace with "The clone's deployment inherits the TARGET server's environment, so cloning onto a server in another environment does move the project across environments (without connection re-binding — use environment promotion for that)."

- [ ] **Step 5: Run tests, compile dependents, commit**

Run: `./gradlew :server:libs:platform:platform-mcp:platform-mcp-service:test :server:libs:automation:automation-ai:automation-ai-mcp:automation-ai-mcp-service:compileJava compileJava --continue > /tmp/t3.log 2>&1; echo $?` then `grep "^> Task .* FAILED" /tmp/t3.log`
Expected: exit 0.

```bash
./gradlew spotlessApply > /tmp/sp.log 2>&1; echo $?
git add server/libs/platform/platform-mcp server/libs/automation/automation-ai/automation-ai-mcp $(git status --short | grep -i "remote" | awk '{print $2}')
git commit -m "732 Add mcp_server.uuid lineage column and per-environment name uniqueness"
git show --name-only HEAD
```

---

### Task 4: `a2a_server.uuid` (init changelog edited in place)

**Files:**
- Modify: `server/libs/automation/automation-ai/automation-ai-a2a/automation-ai-a2a-service/src/main/resources/config/liquibase/changelog/automation/a2a/00000000000001_automation_a2a_init.xml`
- Modify: `server/libs/automation/automation-ai/automation-ai-a2a/automation-ai-a2a-api/src/main/java/com/bytechef/automation/ai/a2a/domain/A2aServer.java`
- Modify: `…/automation-ai-a2a-service/…/repository/A2aServerRepository.java`
- Modify: `…/automation-ai-a2a-api/…/service/A2aServerService.java` + `…-service/…/service/A2aServerServiceImpl.java`
- Test: `…/automation-ai-a2a-service/src/test/java/com/bytechef/automation/ai/a2a/service/A2aServerServiceTest.java`

**Interfaces:**
- Produces: `A2aServer#getUuid()/#setUuid(UUID)`, constructor `A2aServer(String, String, PlatformType, Environment)` assigns a random uuid; `A2aServerService#fetchA2aServer(UUID uuid, Environment environment): Optional<A2aServer>`; `A2aServerService#create(A2aServer)` keeps a provided uuid (assigns one if null).

- [ ] **Step 1: Init changelog edit** — in the `a2a_server` `createTable`, after `secret_key`, add
```xml
            <column name="uuid" type="${uuid_type}">
                <constraints nullable="false"/>
            </column>
```
and after `uk_a2a_server_secret_key` add
```xml
        <addUniqueConstraint constraintName="uk_a2a_server_uuid_environment" tableName="a2a_server" columnNames="uuid,environment" />
```
Verify unreleased first (do not skip): `git log --diff-filter=A --format=%H -- <that file> | tail -1 | xargs -I{} git merge-base --is-ancestor {} master; echo $?` → `1` (not on master) and `git ls-tree -r --name-only v1.1.5 | grep automation_a2a_init` → empty.

- [ ] **Step 2: Failing tests** — `A2aServerServiceTest` (mock `A2aServerRepository`):
```java
    @Test
    void testCreateAssignsUuidWhenMissing() {
        when(a2aServerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        A2aServer created = a2aServerService.create("agent", null, PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        assertThat(created.getUuid()).isNotNull();
    }

    @Test
    void testUpdateKeepsUuid() {
        A2aServer current = new A2aServer("agent", null, PlatformType.AUTOMATION, Environment.DEVELOPMENT);
        UUID currentUuid = current.getUuid();

        current.setId(3L);

        when(a2aServerRepository.findById(3L)).thenReturn(Optional.of(current));
        when(a2aServerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        A2aServer incoming = new A2aServer("agent2", null, PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        incoming.setId(3L);
        incoming.setUuid(UUID.randomUUID());

        assertThat(a2aServerService.update(incoming)
            .getUuid()).isEqualTo(currentUuid);
    }

    @Test
    void testFetchByUuidAndEnvironment() {
        UUID uuid = UUID.randomUUID();
        A2aServer a2aServer = new A2aServer("agent", null, PlatformType.AUTOMATION, Environment.PRODUCTION);

        when(a2aServerRepository.findByUuidAndEnvironment(uuid, 2)).thenReturn(Optional.of(a2aServer));

        assertThat(a2aServerService.fetchA2aServer(uuid, Environment.PRODUCTION)).contains(a2aServer);
    }
```

- [ ] **Step 3: Implement** — `A2aServer`: `@Column private UUID uuid;` + getter/setter; the 4-arg constructor sets `this.uuid = UUID.randomUUID();`. `A2aServerRepository`: `Optional<A2aServer> findByUuidAndEnvironment(UUID uuid, int environment);`. `A2aServerService`: `Optional<A2aServer> fetchA2aServer(UUID uuid, Environment environment);`. `A2aServerServiceImpl.create(A2aServer)`: `if (a2aServer.getUuid() == null) { a2aServer.setUuid(UUID.randomUUID()); }` before save. `update` unchanged (does not copy uuid).

- [ ] **Step 4: Run + local schema sync + commit**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-a2a:automation-ai-a2a-service:test > /tmp/t4.log 2>&1; echo $?` → 0. Then `scripts/dev/sync-local-schema-after-collapse.sh` (local dev DB only).

```bash
./gradlew spotlessApply > /tmp/sp.log 2>&1; echo $?
git add server/libs/automation/automation-ai/automation-ai-a2a
git commit -m "732 Add a2a_server.uuid lineage column"
git show --name-only HEAD
```

---

## Phase 1 — Module skeleton and shared core

### Task 5: `automation-promotion-api` — types, SPI, facade interface, wiring

**Files:**
- Create: `server/ee/libs/automation/automation-promotion/automation-promotion-api/build.gradle.kts`
- Create: `server/ee/libs/automation/automation-promotion/automation-promotion-service/build.gradle.kts`
- Create: `server/ee/libs/automation/automation-promotion/automation-promotion-graphql/build.gradle.kts`
- Create (package `com.bytechef.ee.automation.promotion`, under `automation-promotion-api/src/main/java/com/bytechef/ee/automation/promotion/`):
  `PromotionResourceType.java`, `dto/PromotionConnectionMapping.java`, `dto/PromotionProjectPreview.java`, `dto/EnvironmentPromotionPreview.java`, `dto/EnvironmentPromotionResult.java`, `exception/EnvironmentPromotionErrorType.java`, `facade/EnvironmentPromotionFacade.java`, `handler/EnvironmentPromotionHandler.java`
- Modify: `settings.gradle.kts` (3 `include` lines next to `automation-workflow-alert`), `server/apps/server-app/build.gradle.kts` (add `-graphql` and `-service` next to workflow-alert lines 292-293).

**Interfaces (Produces — used verbatim by every later task):**

```java
public enum PromotionResourceType { API_COLLECTION, MCP_SERVER, A2A_SERVER }

public record PromotionConnectionMapping(
    long sourceConnectionId, String sourceConnectionName, String componentName, int connectionVersion,
    @Nullable Long suggestedTargetConnectionId, List<String> usedBy) {}

public record PromotionProjectPreview(
    long projectId, String projectName, int sourceProjectVersion, @Nullable Integer targetProjectVersion) {}

public record EnvironmentPromotionPreview(
    PromotionResourceType resourceType, long sourceId, Environment sourceEnvironment,
    Environment targetEnvironment, @Nullable Long existingTargetId, @Nullable String existingTargetName,
    List<PromotionProjectPreview> projects, List<PromotionConnectionMapping> connections,
    List<String> warnings) {}

public record EnvironmentPromotionResult(
    long targetId, boolean created, @Nullable String targetUrl, List<Long> unresolvedConnectionIds) {}

public class EnvironmentPromotionErrorType extends AbstractErrorType {
    public static final EnvironmentPromotionErrorType SAME_ENVIRONMENT = new EnvironmentPromotionErrorType(100);
    public static final EnvironmentPromotionErrorType ENVIRONMENT_NOT_AVAILABLE = new EnvironmentPromotionErrorType(101);
    public static final EnvironmentPromotionErrorType SOURCE_NOT_FOUND = new EnvironmentPromotionErrorType(102);
    public static final EnvironmentPromotionErrorType TARGET_CONNECTION_INVALID = new EnvironmentPromotionErrorType(103);
    public static final EnvironmentPromotionErrorType TARGET_NAME_CONFLICT = new EnvironmentPromotionErrorType(104);
    public static final EnvironmentPromotionErrorType UNSUPPORTED_RESOURCE_TYPE = new EnvironmentPromotionErrorType(105);

    private EnvironmentPromotionErrorType(int errorKey) {
        super(EnvironmentPromotionErrorType.class, errorKey);
    }
}

public interface EnvironmentPromotionFacade {
    EnvironmentPromotionPreview preview(PromotionResourceType resourceType, long sourceId, long targetEnvironmentId);

    EnvironmentPromotionResult promote(
        PromotionResourceType resourceType, long sourceId, long targetEnvironmentId,
        Map<Long, Long> connectionMappings);
}

public interface EnvironmentPromotionHandler {
    PromotionResourceType getResourceType();

    EnvironmentPromotionPreview preview(long sourceId, Environment targetEnvironment);

    EnvironmentPromotionResult promote(long sourceId, Environment targetEnvironment, Map<Long, Long> connectionMappings);
}
```
(`Environment` = `com.bytechef.platform.configuration.domain.Environment`; `AbstractErrorType` = `com.bytechef.exception.AbstractErrorType`; `@Nullable` = `org.jspecify.annotations.Nullable`.)

- [ ] **Step 1: build files**

`automation-promotion-api/build.gradle.kts`:
```kotlin
dependencies {
    api(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    implementation("org.jspecify:jspecify")
    implementation(project(":server:libs:core:exception:exception-api"))
}
```
(`AbstractErrorType` lives in `server/libs/core/exception/exception-api` — path confirmed.)

`automation-promotion-service/build.gradle.kts`:
```kotlin
dependencies {
    api(project(":server:ee:libs:automation:automation-promotion:automation-promotion-api"))

    implementation("io.micrometer:micrometer-core")
    implementation("org.jspecify:jspecify")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:exception:exception-api"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:automation:automation-ai:automation-ai-mcp:automation-ai-mcp-api"))
    implementation(project(":server:libs:automation:automation-ai:automation-ai-a2a:automation-ai-a2a-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:libs:platform:platform-connection:platform-connection-api"))
    implementation(project(":server:libs:platform:platform-mcp:platform-mcp-api"))
    implementation(project(":server:libs:platform:platform-tag:platform-tag-api"))
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    implementation(project(":server:ee:libs:automation:automation-api-platform:automation-api-platform-configuration:automation-api-platform-configuration-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
```
(Verify each `project(":…")` path exists in `settings.gradle.kts` before committing. `WorkflowService` is `com.bytechef.atlas.configuration.service.WorkflowService` from `atlas-configuration-api`; `AbstractErrorType`/`ConfigurationException` come from `:server:libs:core:exception:exception-api`.)

`automation-promotion-graphql/build.gradle.kts`:
```kotlin
dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.graphql:spring-graphql")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:ee:libs:automation:automation-promotion:automation-promotion-api"))
}
```
`settings.gradle.kts` (after line 681):
```kotlin
include("server:ee:libs:automation:automation-promotion:automation-promotion-api")
include("server:ee:libs:automation:automation-promotion:automation-promotion-graphql")
include("server:ee:libs:automation:automation-promotion:automation-promotion-service")
```
`server/apps/server-app/build.gradle.kts` (after line 293):
```kotlin
    implementation(project(":server:ee:libs:automation:automation-promotion:automation-promotion-graphql"))
    implementation(project(":server:ee:libs:automation:automation-promotion:automation-promotion-service"))
```

- [ ] **Step 2: Create the API types** exactly as in the Interfaces block above, one file each, with the EE header and `@version ee` Javadoc. `EnvironmentPromotionHandler` Javadoc must state: "`@PreAuthorize` guards live on the implementing beans; the facade never checks authorization itself."

- [ ] **Step 3: Compile + commit**

Run: `./gradlew :server:ee:libs:automation:automation-promotion:automation-promotion-api:compileJava :server:apps:server-app:compileJava > /tmp/t5.log 2>&1; echo $?` → 0.

```bash
./gradlew spotlessApply > /tmp/sp.log 2>&1; echo $?
git add settings.gradle.kts server/apps/server-app/build.gradle.kts server/ee/libs/automation/automation-promotion
git commit -m "732 Add automation-promotion module with environment promotion API types"
git show --name-only HEAD
```

---

### Task 6: `ConnectionEnvironmentMapper`

**Files:**
- Create: `server/ee/libs/automation/automation-promotion/automation-promotion-service/src/main/java/com/bytechef/ee/automation/promotion/connection/ConnectionEnvironmentMapper.java`
- Test: `…/automation-promotion-service/src/test/java/com/bytechef/ee/automation/promotion/connection/ConnectionEnvironmentMapperTest.java`

**Interfaces:**
- Consumes: `ConnectionService#getConnections(List<Long>)` (`com.bytechef.platform.connection.service.ConnectionService`), `WorkspaceConnectionFacade#getConnections(long workspaceId, String componentName, Integer connectionVersion, Long environmentId, Long tagId): List<ConnectionDTO>` (`com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade` — visibility-aware read path).
- Produces:
```java
@Component @ConditionalOnEEVersion
public class ConnectionEnvironmentMapper {
    /** source connection id → target connection id for every source id that has EXACTLY ONE visible
     *  target-environment connection with the same (componentName, connectionVersion, name). */
    public Map<Long, Long> suggest(long workspaceId, Set<Long> sourceConnectionIds, Environment targetEnvironment);

    /** Rejects any requested target that does not exist, is not in targetEnvironment, is not visible in the
     *  workspace, or belongs to a different component than its source. Returns the validated map. */
    public Map<Long, Long> validate(long workspaceId, Environment targetEnvironment, Map<Long, Long> requested);
}
```

- [ ] **Step 1: Failing test**

```java
package com.bytechef.ee.automation.promotion.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.service.ConnectionService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConnectionEnvironmentMapperTest {

    private final ConnectionService connectionService = mock(ConnectionService.class);
    private final WorkspaceConnectionFacade workspaceConnectionFacade = mock(WorkspaceConnectionFacade.class);
    private final ConnectionEnvironmentMapper mapper =
        new ConnectionEnvironmentMapper(connectionService, workspaceConnectionFacade);

    @Test
    void testSuggestMapsUnambiguousNameMatch() {
        Connection source = connection(10L, "slack", 1, "Team Slack", Environment.DEVELOPMENT);

        when(connectionService.getConnections(List.of(10L))).thenReturn(List.of(source));
        when(workspaceConnectionFacade.getConnections(5L, "slack", 1, 1L, null))
            .thenReturn(List.of(dto(20L, "slack", 1, "Team Slack"), dto(21L, "slack", 1, "Other")));

        assertThat(mapper.suggest(5L, Set.of(10L), Environment.STAGING)).containsExactly(Map.entry(10L, 20L));
    }

    @Test
    void testSuggestSkipsAmbiguousAndMissing() {
        Connection ambiguous = connection(10L, "slack", 1, "Team Slack", Environment.DEVELOPMENT);
        Connection missing = connection(11L, "github", 1, "GH", Environment.DEVELOPMENT);

        when(connectionService.getConnections(List.of(10L, 11L))).thenReturn(List.of(ambiguous, missing));
        when(workspaceConnectionFacade.getConnections(5L, "slack", 1, 1L, null))
            .thenReturn(List.of(dto(20L, "slack", 1, "Team Slack"), dto(22L, "slack", 1, "Team Slack")));
        when(workspaceConnectionFacade.getConnections(5L, "github", 1, 1L, null)).thenReturn(List.of());

        assertThat(mapper.suggest(5L, Set.of(10L, 11L), Environment.STAGING)).isEmpty();
    }

    @Test
    void testValidateRejectsWrongEnvironment() {
        Connection source = connection(10L, "slack", 1, "Team Slack", Environment.DEVELOPMENT);
        Connection target = connection(20L, "slack", 1, "Team Slack", Environment.PRODUCTION);

        when(connectionService.getConnections(List.of(10L))).thenReturn(List.of(source));
        when(connectionService.getConnections(List.of(20L))).thenReturn(List.of(target));
        when(workspaceConnectionFacade.getConnections(5L, "slack", 1, 1L, null))
            .thenReturn(List.of(dto(20L, "slack", 1, "Team Slack")));

        assertThatThrownBy(() -> mapper.validate(5L, Environment.STAGING, Map.of(10L, 20L)))
            .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testValidateRejectsInvisibleTarget() {
        Connection source = connection(10L, "slack", 1, "Team Slack", Environment.DEVELOPMENT);
        Connection target = connection(20L, "slack", 1, "Team Slack", Environment.STAGING);

        when(connectionService.getConnections(List.of(10L))).thenReturn(List.of(source));
        when(connectionService.getConnections(List.of(20L))).thenReturn(List.of(target));
        when(workspaceConnectionFacade.getConnections(5L, "slack", 1, 1L, null)).thenReturn(List.of());

        assertThatThrownBy(() -> mapper.validate(5L, Environment.STAGING, Map.of(10L, 20L)))
            .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testValidateAcceptsVisibleSameComponentTarget() {
        Connection source = connection(10L, "slack", 1, "Team Slack", Environment.DEVELOPMENT);
        Connection target = connection(20L, "slack", 1, "Prod Slack", Environment.STAGING);

        when(connectionService.getConnections(List.of(10L))).thenReturn(List.of(source));
        when(connectionService.getConnections(List.of(20L))).thenReturn(List.of(target));
        when(workspaceConnectionFacade.getConnections(5L, "slack", 1, 1L, null))
            .thenReturn(List.of(dto(20L, "slack", 1, "Prod Slack")));

        assertThat(mapper.validate(5L, Environment.STAGING, Map.of(10L, 20L))).containsExactly(Map.entry(10L, 20L));
    }

    private static Connection connection(long id, String componentName, int version, String name, Environment env) {
        Connection connection = new Connection();

        connection.setId(id);
        connection.setComponentName(componentName);
        connection.setConnectionVersion(version);
        connection.setName(name);
        connection.setEnvironment(env);

        return connection;
    }

    private static ConnectionDTO dto(long id, String componentName, int version, String name) {
        return ConnectionDTO.builder()
            .id(id)
            .componentName(componentName)
            .connectionVersion(version)
            .name(name)
            .build();
    }
}
```
(If `ConnectionDTO.builder()` does not exist, use the canonical constructor with `ConnectionStatus.ACTIVE` / `ResourceVisibility.WORKSPACE` and nulls elsewhere; check `Connection#setEnvironment` exists — if the setter takes an int, pass `env.ordinal()`.)

- [ ] **Step 2: Run → fails (class missing).**

- [ ] **Step 3: Implement**

```java
@Component
@ConditionalOnEEVersion
public class ConnectionEnvironmentMapper {

    private final ConnectionService connectionService;
    private final WorkspaceConnectionFacade workspaceConnectionFacade;

    @SuppressFBWarnings("EI")
    public ConnectionEnvironmentMapper(
        ConnectionService connectionService, WorkspaceConnectionFacade workspaceConnectionFacade) {

        this.connectionService = connectionService;
        this.workspaceConnectionFacade = workspaceConnectionFacade;
    }

    public Map<Long, Long> suggest(long workspaceId, Set<Long> sourceConnectionIds, Environment targetEnvironment) {
        if (sourceConnectionIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> suggestions = new HashMap<>();

        for (Connection sourceConnection : connectionService.getConnections(sorted(sourceConnectionIds))) {
            List<ConnectionDTO> candidates = workspaceConnectionFacade.getConnections(
                workspaceId, sourceConnection.getComponentName(), sourceConnection.getConnectionVersion(),
                (long) targetEnvironment.ordinal(), null);

            List<ConnectionDTO> sameName = candidates.stream()
                .filter(candidate -> Objects.equals(candidate.name(), sourceConnection.getName()))
                .toList();

            if (sameName.size() == 1) {
                suggestions.put(sourceConnection.getId(), sameName.getFirst().id());
            }
        }

        return suggestions;
    }

    public Map<Long, Long> validate(long workspaceId, Environment targetEnvironment, Map<Long, Long> requested) {
        if (requested.isEmpty()) {
            return Map.of();
        }

        Map<Long, Connection> sources = byId(connectionService.getConnections(sorted(requested.keySet())));
        Map<Long, Connection> targets = byId(connectionService.getConnections(sorted(new HashSet<>(requested.values()))));

        for (Map.Entry<Long, Long> entry : requested.entrySet()) {
            Connection source = sources.get(entry.getKey());
            Connection target = targets.get(entry.getValue());

            if (source == null || target == null) {
                throw invalid("connection %s -> %s does not exist".formatted(entry.getKey(), entry.getValue()));
            }

            if (target.getEnvironmentId() != targetEnvironment.ordinal()) {
                throw invalid("connection %s is not in environment %s".formatted(target.getId(), targetEnvironment));
            }

            if (!Objects.equals(source.getComponentName(), target.getComponentName())) {
                throw invalid("connection %s belongs to component %s, expected %s".formatted(
                    target.getId(), target.getComponentName(), source.getComponentName()));
            }

            boolean visible = workspaceConnectionFacade
                .getConnections(
                    workspaceId, target.getComponentName(), target.getConnectionVersion(),
                    (long) targetEnvironment.ordinal(), null)
                .stream()
                .anyMatch(candidate -> Objects.equals(candidate.id(), target.getId()));

            if (!visible) {
                throw invalid("connection %s is not visible in workspace %s".formatted(target.getId(), workspaceId));
            }
        }

        return Map.copyOf(requested);
    }

    private static ConfigurationException invalid(String message) {
        return new ConfigurationException(message, EnvironmentPromotionErrorType.TARGET_CONNECTION_INVALID);
    }

    private static Map<Long, Connection> byId(List<Connection> connections) { … Collectors.toMap(Connection::getId, c -> c) … }

    private static List<Long> sorted(Set<Long> ids) { return ids.stream().sorted().toList(); }
}
```

- [ ] **Step 4: Run tests → pass. Commit**

```bash
./gradlew spotlessApply > /tmp/sp.log 2>&1; echo $?
git add server/ee/libs/automation/automation-promotion/automation-promotion-service
git commit -m "732 Add ConnectionEnvironmentMapper for cross-environment connection re-binding"
git show --name-only HEAD
```

---

### Task 7: `ProjectDeploymentPromoter`

**Files:**
- Create: `…/automation-promotion-service/src/main/java/com/bytechef/ee/automation/promotion/deployment/ProjectDeploymentPromoter.java`
- Test: `…/automation-promotion-service/src/test/java/com/bytechef/ee/automation/promotion/deployment/ProjectDeploymentPromoterTest.java`

**Interfaces:**
- Consumes: `ProjectService#getProject(long)` (`Project#isPublished()`, `#getLastProjectVersion()`), `ProjectWorkflowService#getProjectWorkflows(long projectId, int projectVersion)` (`ProjectWorkflow#getWorkflowId()`, `#getUuidAsString()`), `ProjectDeploymentWorkflowService#getProjectDeploymentWorkflows(long projectDeploymentId)`, `ProjectDeploymentFacade#updateProjectDeployment(ProjectDeployment, List<ProjectDeploymentWorkflow>, List<Tag>)`, `WorkflowService#getWorkflow(String workflowId)` (`com.bytechef.atlas.configuration.service.WorkflowService`, `Workflow#getLabel()`), `ProjectDeploymentErrorType.PROJECT_NOT_PUBLISHED / INVALID_PROJECT_VERSION`.
- Produces:
```java
@Component @ConditionalOnEEVersion
public class ProjectDeploymentPromoter {
    public record SourceBinding(String workflowUuid, String workflowLabel, String nodeName, String key, long connectionId) {}
    /** warnings: e.g. "workflow <label> no longer exists in version N" for source rows dropped by the sync. */
    public record SyncResult(Map<Long, Long> workflowIdMapping, List<Long> unresolvedConnectionIds, List<String> warnings) {}

    public void validatePromotable(long projectId, int projectVersion);                       // throws ConfigurationException
    public List<SourceBinding> collectSourceBindings(ProjectDeployment source);              // every pdw connection of the source
    public Map<Long, Long> existingTargetBindings(ProjectDeployment source, ProjectDeployment target); // source connId → target connId where the same (uuid,node,key) is bound in target
    public SyncResult sync(ProjectDeployment source, ProjectDeployment target,
                           Map<Long, Long> requestedMappings, Map<Long, Long> suggestedMappings, boolean targetIsNew);
}
```

- [ ] **Step 1: Failing tests** (Mockito; construct entities directly)

```java
class ProjectDeploymentPromoterTest {

    private final ProjectDeploymentFacade projectDeploymentFacade = mock(ProjectDeploymentFacade.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService = mock(ProjectDeploymentWorkflowService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);
    private final ProjectDeploymentPromoter promoter = new ProjectDeploymentPromoter(
        projectDeploymentFacade, projectDeploymentWorkflowService, projectService, projectWorkflowService, workflowService);

    @Test
    void testValidatePromotableRejectsUnpublishedProject() {
        Project project = new Project();          // isPublished() false when no published version

        when(projectService.getProject(1L)).thenReturn(project);

        assertThatThrownBy(() -> promoter.validatePromotable(1L, 1)).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testValidatePromotableRejectsDraftVersion() {
        Project project = publishedProjectWithLastVersion(3);   // helper: publishes v1,v2 → last (draft) = 3

        when(projectService.getProject(1L)).thenReturn(project);

        assertThatThrownBy(() -> promoter.validatePromotable(1L, 3)).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testSyncOnNewTargetCopiesSourceBindingsThroughMappingsAndReportsUnresolved() {
        ProjectDeployment source = deployment(1L, 100L, 2, Environment.DEVELOPMENT);
        ProjectDeployment target = deployment(2L, 100L, 2, Environment.STAGING);

        ProjectWorkflow projectWorkflow = new ProjectWorkflow(100L, 2, "wf-v2", UUID.fromString("11111111-1111-1111-1111-111111111111"));

        when(projectWorkflowService.getProjectWorkflows(100L, 2)).thenReturn(List.of(projectWorkflow));

        ProjectDeploymentWorkflow sourcePdw = pdw(10L, 1L, "wf-v2", true, Map.of("k", "v"),
            List.of(new ProjectDeploymentWorkflowConnection(500L, "slack", "sendMessage_1"),
                new ProjectDeploymentWorkflowConnection(501L, "github", "createIssue_1")));

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(1L)).thenReturn(List.of(sourcePdw));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(2L))
            .thenReturn(List.of())                                    // before sync
            .thenReturn(List.of(pdw(20L, 2L, "wf-v2", true, Map.of(), List.of())));   // after sync

        SyncResult result = promoter.sync(source, target, Map.of(500L, 600L), Map.of(), true);

        ArgumentCaptor<List<ProjectDeploymentWorkflow>> captor = ArgumentCaptor.forClass(List.class);

        verify(projectDeploymentFacade).updateProjectDeployment(eq(target), captor.capture(), eq(List.of()));

        ProjectDeploymentWorkflow synced = captor.getValue().getFirst();

        assertThat(synced.getWorkflowId()).isEqualTo("wf-v2");
        assertThat(synced.isEnabled()).isTrue();
        assertThat(synced.getInputs()).containsEntry("k", "v");
        assertThat(synced.getConnections()).extracting(ProjectDeploymentWorkflowConnection::getConnectionId).containsExactly(600L);
        assertThat(result.unresolvedConnectionIds()).containsExactly(501L);
        assertThat(result.workflowIdMapping()).containsExactly(Map.entry(10L, 20L));
        assertThat(target.getProjectVersion()).isEqualTo(2);
    }

    @Test
    void testSyncOnExistingTargetKeepsTargetBindingsInputsAndEnabled() {
        ProjectDeployment source = deployment(1L, 100L, 3, Environment.DEVELOPMENT);
        ProjectDeployment target = deployment(2L, 100L, 2, Environment.STAGING);
        UUID uuid = UUID.randomUUID();

        when(projectWorkflowService.getProjectWorkflows(100L, 3)).thenReturn(List.of(new ProjectWorkflow(100L, 3, "wf-v3", uuid)));
        when(projectWorkflowService.getProjectWorkflows(100L, 2)).thenReturn(List.of(new ProjectWorkflow(100L, 2, "wf-v2", uuid)));

        ProjectDeploymentWorkflow sourcePdw = pdw(10L, 1L, "wf-v3", true, Map.of("k", "source"),
            List.of(new ProjectDeploymentWorkflowConnection(500L, "slack", "sendMessage_1")));
        ProjectDeploymentWorkflow targetPdw = pdw(20L, 2L, "wf-v2", false, Map.of("k", "target"),
            List.of(new ProjectDeploymentWorkflowConnection(650L, "slack", "sendMessage_1")));

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(1L)).thenReturn(List.of(sourcePdw));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(2L)).thenReturn(List.of(targetPdw));

        SyncResult result = promoter.sync(source, target, Map.of(), Map.of(500L, 600L), false);

        ArgumentCaptor<List<ProjectDeploymentWorkflow>> captor = ArgumentCaptor.forClass(List.class);

        verify(projectDeploymentFacade).updateProjectDeployment(eq(target), captor.capture(), eq(List.of()));

        ProjectDeploymentWorkflow synced = captor.getValue().getFirst();

        assertThat(synced.getWorkflowId()).isEqualTo("wf-v3");
        assertThat(synced.isEnabled()).isFalse();                                   // target's
        assertThat(synced.getInputs()).containsEntry("k", "target");                 // target's
        assertThat(synced.getConnections()).extracting(ProjectDeploymentWorkflowConnection::getConnectionId).containsExactly(650L); // existing target binding wins over suggestion
        assertThat(result.unresolvedConnectionIds()).isEmpty();
        assertThat(target.getProjectVersion()).isEqualTo(3);
    }

    @Test
    void testExistingTargetBindingsCorrelatesByUuidNodeAndKey() { … source 500L on (uuid, "sendMessage_1", "slack") and target 650L on the same triple → {500L: 650L}; a target binding on a different node is not returned … }

    @Test
    void testCollectSourceBindingsIncludesWorkflowLabel() { … workflowService.getWorkflow("wf-v2") returns label "Notify" → SourceBinding("<uuid>", "Notify", "sendMessage_1", "slack", 500L) … }
}
```
Helpers: `deployment(id, projectId, version, env)`, `pdw(id, deploymentId, workflowId, enabled, inputs, connections)` build the domain objects with setters (`ProjectDeploymentWorkflow#setConnections(List)`, `#setInputs(Map)`, `#setEnabled`, `#setWorkflowId`, `#setId`, `#setProjectDeploymentId`; `ProjectDeployment#setId/#setProjectId/#setProjectVersion/#setEnvironment`). `publishedProjectWithLastVersion` — inspect `Project` for the mutators that mark versions published (`Project#addVersion`/`publish` — read `ProjectFacadeImpl.publishProject` for the exact calls) and use them.

- [ ] **Step 2: Run → fails.**

- [ ] **Step 3: Implement**

```java
@Component
@ConditionalOnEEVersion
public class ProjectDeploymentPromoter {

    public record SourceBinding(String workflowUuid, String workflowLabel, String nodeName, String key, long connectionId) {}

    public record SyncResult(
        Map<Long, Long> workflowIdMapping, List<Long> unresolvedConnectionIds, List<String> warnings) {}

    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    // constructor …

    public void validatePromotable(long projectId, int projectVersion) {
        Project project = projectService.getProject(projectId);

        if (!project.isPublished()) {
            throw new ConfigurationException(
                "Project id=%s is not published".formatted(projectId), ProjectDeploymentErrorType.PROJECT_NOT_PUBLISHED);
        }

        if (project.getLastProjectVersion() == projectVersion) {
            throw new ConfigurationException(
                "Project version v=%s cannot be in DRAFT".formatted(projectVersion),
                ProjectDeploymentErrorType.INVALID_PROJECT_VERSION);
        }
    }

    public List<SourceBinding> collectSourceBindings(ProjectDeployment source) {
        Map<String, String> uuidByWorkflowId = uuidByWorkflowId(source.getProjectId(), source.getProjectVersion());
        List<SourceBinding> bindings = new ArrayList<>();

        for (ProjectDeploymentWorkflow pdw : projectDeploymentWorkflowService.getProjectDeploymentWorkflows(source.getId())) {
            String label = workflowService.getWorkflow(pdw.getWorkflowId()).getLabel();

            for (ProjectDeploymentWorkflowConnection connection : pdw.getConnections()) {
                bindings.add(new SourceBinding(
                    uuidByWorkflowId.get(pdw.getWorkflowId()), label, connection.getWorkflowNodeName(),
                    connection.getWorkflowConnectionKey(), connection.getConnectionId()));
            }
        }

        return bindings;
    }

    public Map<Long, Long> existingTargetBindings(ProjectDeployment source, ProjectDeployment target) {
        Map<String, Long> targetByTriple = new HashMap<>();
        Map<String, String> targetUuids = uuidByWorkflowId(target.getProjectId(), target.getProjectVersion());

        for (ProjectDeploymentWorkflow pdw : projectDeploymentWorkflowService.getProjectDeploymentWorkflows(target.getId())) {
            for (ProjectDeploymentWorkflowConnection connection : pdw.getConnections()) {
                targetByTriple.put(
                    triple(targetUuids.get(pdw.getWorkflowId()), connection), connection.getConnectionId());
            }
        }

        Map<Long, Long> result = new HashMap<>();
        Map<String, String> sourceUuids = uuidByWorkflowId(source.getProjectId(), source.getProjectVersion());

        for (ProjectDeploymentWorkflow pdw : projectDeploymentWorkflowService.getProjectDeploymentWorkflows(source.getId())) {
            for (ProjectDeploymentWorkflowConnection connection : pdw.getConnections()) {
                Long targetConnectionId = targetByTriple.get(triple(sourceUuids.get(pdw.getWorkflowId()), connection));

                if (targetConnectionId != null) {
                    result.putIfAbsent(connection.getConnectionId(), targetConnectionId);
                }
            }
        }

        return result;
    }

    public SyncResult sync(
        ProjectDeployment source, ProjectDeployment target, Map<Long, Long> requestedMappings,
        Map<Long, Long> suggestedMappings, boolean targetIsNew) {

        Map<String, ProjectWorkflow> newVersionByUuid = new HashMap<>();

        for (ProjectWorkflow projectWorkflow : projectWorkflowService.getProjectWorkflows(source.getProjectId(), source.getProjectVersion())) {
            newVersionByUuid.put(projectWorkflow.getUuidAsString(), projectWorkflow);
        }

        Map<String, String> sourceUuids = uuidByWorkflowId(source.getProjectId(), source.getProjectVersion());
        Map<String, String> targetUuids = uuidByWorkflowId(target.getProjectId(), target.getProjectVersion());

        Map<String, ProjectDeploymentWorkflow> targetByUuid = new HashMap<>();

        for (ProjectDeploymentWorkflow pdw : projectDeploymentWorkflowService.getProjectDeploymentWorkflows(target.getId())) {
            targetByUuid.put(targetUuids.get(pdw.getWorkflowId()), pdw);
        }

        Map<Long, Long> existingBindings = targetIsNew ? Map.of() : existingTargetBindings(source, target);
        Set<Long> unresolved = new LinkedHashSet<>();
        List<String> warnings = new ArrayList<>();
        List<ProjectDeploymentWorkflow> synced = new ArrayList<>();
        List<ProjectDeploymentWorkflow> sourcePdws = projectDeploymentWorkflowService.getProjectDeploymentWorkflows(source.getId());

        for (ProjectDeploymentWorkflow sourcePdw : sourcePdws) {
            String uuid = sourceUuids.get(sourcePdw.getWorkflowId());
            ProjectWorkflow newVersion = newVersionByUuid.get(uuid);

            if (newVersion == null) {
                warnings.add("Workflow %s no longer exists in version %s and was skipped".formatted(
                    workflowService.getWorkflow(sourcePdw.getWorkflowId()).getLabel(), source.getProjectVersion()));

                continue;
            }

            ProjectDeploymentWorkflow existing = targetByUuid.get(uuid);
            ProjectDeploymentWorkflow pdw = new ProjectDeploymentWorkflow();

            pdw.setProjectDeploymentId(target.getId());
            pdw.setWorkflowId(newVersion.getWorkflowId());

            boolean keepTarget = !targetIsNew && existing != null;

            pdw.setEnabled(keepTarget ? existing.isEnabled() : sourcePdw.isEnabled());
            pdw.setInputs(keepTarget ? existing.getInputs() : sourcePdw.getInputs());

            List<ProjectDeploymentWorkflowConnection> connections = new ArrayList<>();

            for (ProjectDeploymentWorkflowConnection sourceConnection : sourcePdw.getConnections()) {
                Long resolved = requestedMappings.get(sourceConnection.getConnectionId());

                if (resolved == null) {
                    resolved = existingBindings.get(sourceConnection.getConnectionId());
                }

                if (resolved == null) {
                    resolved = suggestedMappings.get(sourceConnection.getConnectionId());
                }

                if (resolved == null) {
                    unresolved.add(sourceConnection.getConnectionId());
                } else {
                    connections.add(new ProjectDeploymentWorkflowConnection(
                        resolved, sourceConnection.getWorkflowConnectionKey(), sourceConnection.getWorkflowNodeName()));
                }
            }

            pdw.setConnections(connections);
            synced.add(pdw);
        }

        target.setProjectVersion(source.getProjectVersion());

        projectDeploymentFacade.updateProjectDeployment(target, synced, List.of());

        Map<String, String> syncedUuids = uuidByWorkflowId(target.getProjectId(), source.getProjectVersion());
        Map<String, Long> targetIdByUuid = new HashMap<>();

        for (ProjectDeploymentWorkflow pdw : projectDeploymentWorkflowService.getProjectDeploymentWorkflows(target.getId())) {
            targetIdByUuid.put(syncedUuids.get(pdw.getWorkflowId()), pdw.getId());
        }

        Map<Long, Long> workflowIdMapping = new LinkedHashMap<>();

        for (ProjectDeploymentWorkflow sourcePdw : sourcePdws) {
            workflowIdMapping.put(sourcePdw.getId(), targetIdByUuid.get(sourceUuids.get(sourcePdw.getWorkflowId())));
        }

        return new SyncResult(workflowIdMapping, List.copyOf(unresolved), List.copyOf(warnings));
    }

    private Map<String, String> uuidByWorkflowId(long projectId, int projectVersion) {
        Map<String, String> result = new HashMap<>();

        for (ProjectWorkflow projectWorkflow : projectWorkflowService.getProjectWorkflows(projectId, projectVersion)) {
            result.put(projectWorkflow.getWorkflowId(), projectWorkflow.getUuidAsString());
        }

        return result;
    }

    private static String triple(String uuid, ProjectDeploymentWorkflowConnection connection) {
        return uuid + "|" + connection.getWorkflowNodeName() + "|" + connection.getWorkflowConnectionKey();
    }
}
```
Note for the implementer: `updateProjectDeployment(pd, list, tags)` calls `projectDeploymentService.update(pd)`, which copies `name`, `description`, `enabled`, `projectVersion`, `tagIds`, `version` from the passed object — pass the loaded `target` (unchanged except `projectVersion`) so `enabled` and `name` are preserved. Both sync tests above must also assert `result.warnings()` is empty; add a sixth test `testSyncSkipsWorkflowsMissingFromTheNewVersionWithWarning` where the source pdw's uuid has no `ProjectWorkflow` at the source version and assert the pdw list passed to the facade is empty and `warnings()` has one entry. Handlers (Tasks 9–11) log `syncResult.warnings()` at WARN and append them to the preview's `warnings` list when running preview-time dry checks is not possible — i.e. they surface only in logs on `promote`; the preview computes the same condition itself by comparing source pdw uuids against `projectWorkflowService.getProjectWorkflows(projectId, sourceVersion)` and adds the same message.

- [ ] **Step 4: Run tests → pass; commit**

```bash
./gradlew spotlessApply > /tmp/sp.log 2>&1; echo $?
git add server/ee/libs/automation/automation-promotion/automation-promotion-service
git commit -m "732 Add ProjectDeploymentPromoter to sync synthetic deployments across environments"
git show --name-only HEAD
```

---

### Task 8: `EnvironmentPromotionFacadeImpl` (dispatch, env checks, metric)

**Files:**
- Create: `…/automation-promotion-service/src/main/java/com/bytechef/ee/automation/promotion/facade/EnvironmentPromotionFacadeImpl.java`
- Test: `…/automation-promotion-service/src/test/java/com/bytechef/ee/automation/promotion/facade/EnvironmentPromotionFacadeTest.java`

**Interfaces:**
- Consumes: `EnvironmentService#getEnvironments(): List<Environment>`, `#getEnvironment(long)`; `List<EnvironmentPromotionHandler>`; `ObjectProvider<MeterRegistry>`.
- Produces: the `EnvironmentPromotionFacade` bean; metric `bytechef_environment_promotion{resource,outcome}` with `resource` = lower-case enum name, `outcome ∈ created|updated|failed`.

- [ ] **Step 1: Failing tests**

```java
class EnvironmentPromotionFacadeTest {

    private final EnvironmentService environmentService = mock(EnvironmentService.class);
    private final EnvironmentPromotionHandler handler = mock(EnvironmentPromotionHandler.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private EnvironmentPromotionFacadeImpl facade;

    @BeforeEach
    void setUp() {
        when(handler.getResourceType()).thenReturn(PromotionResourceType.MCP_SERVER);
        when(environmentService.getEnvironments())
            .thenReturn(List.of(Environment.DEVELOPMENT, Environment.STAGING, Environment.PRODUCTION));
        when(environmentService.getEnvironment(1L)).thenReturn(Environment.STAGING);

        ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(meterRegistry);

        facade = new EnvironmentPromotionFacadeImpl(environmentService, List.of(handler), provider);
    }

    @Test
    void testPreviewDispatchesToHandlerByResourceType() {
        EnvironmentPromotionPreview preview = new EnvironmentPromotionPreview(
            PromotionResourceType.MCP_SERVER, 9L, Environment.DEVELOPMENT, Environment.STAGING, null, null,
            List.of(), List.of(), List.of());

        when(handler.preview(9L, Environment.STAGING)).thenReturn(preview);

        assertThat(facade.preview(PromotionResourceType.MCP_SERVER, 9L, 1L)).isSameAs(preview);
    }

    @Test
    void testUnavailableTargetEnvironmentIsRejected() {
        when(environmentService.getEnvironments()).thenReturn(List.of(Environment.DEVELOPMENT));

        assertThatThrownBy(() -> facade.preview(PromotionResourceType.MCP_SERVER, 9L, 1L))
            .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testUnknownResourceTypeIsRejected() {
        assertThatThrownBy(() -> facade.preview(PromotionResourceType.A2A_SERVER, 9L, 1L))
            .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testPromoteRecordsCreatedOutcome() {
        when(handler.promote(9L, Environment.STAGING, Map.of()))
            .thenReturn(new EnvironmentPromotionResult(77L, true, null, List.of()));

        facade.promote(PromotionResourceType.MCP_SERVER, 9L, 1L, Map.of());

        assertThat(meterRegistry.counter("bytechef_environment_promotion", "resource", "mcp_server", "outcome", "created")
            .count()).isEqualTo(1.0);
    }

    @Test
    void testPromoteRecordsFailedOutcomeAndRethrows() {
        when(handler.promote(9L, Environment.STAGING, Map.of())).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> facade.promote(PromotionResourceType.MCP_SERVER, 9L, 1L, Map.of()))
            .isInstanceOf(IllegalStateException.class);
        assertThat(meterRegistry.counter("bytechef_environment_promotion", "resource", "mcp_server", "outcome", "failed")
            .count()).isEqualTo(1.0);
    }
}
```

- [ ] **Step 2: Run → fails.**

- [ ] **Step 3: Implement**

```java
@Service
@ConditionalOnEEVersion
public class EnvironmentPromotionFacadeImpl implements EnvironmentPromotionFacade {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentPromotionFacadeImpl.class);

    private final EnvironmentService environmentService;
    private final Map<PromotionResourceType, EnvironmentPromotionHandler> handlers;
    private final @Nullable MeterRegistry meterRegistry;

    public EnvironmentPromotionFacadeImpl(
        EnvironmentService environmentService, List<EnvironmentPromotionHandler> handlers,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.environmentService = environmentService;
        this.handlers = handlers.stream()
            .collect(Collectors.toUnmodifiableMap(EnvironmentPromotionHandler::getResourceType, handler -> handler));
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    @Override
    public EnvironmentPromotionPreview preview(PromotionResourceType resourceType, long sourceId, long targetEnvironmentId) {
        return handler(resourceType).preview(sourceId, targetEnvironment(targetEnvironmentId));
    }

    @Override
    public EnvironmentPromotionResult promote(
        PromotionResourceType resourceType, long sourceId, long targetEnvironmentId, Map<Long, Long> connectionMappings) {

        EnvironmentPromotionHandler handler = handler(resourceType);
        Environment targetEnvironment = targetEnvironment(targetEnvironmentId);

        try {
            EnvironmentPromotionResult result = handler.promote(sourceId, targetEnvironment, connectionMappings);

            record(resourceType, result.created() ? "created" : "updated");

            log.info(
                "Promoted {} id={} to {} (targetId={}, created={}, unresolvedConnections={})", resourceType, sourceId,
                targetEnvironment, result.targetId(), result.created(), result.unresolvedConnectionIds().size());

            return result;
        } catch (RuntimeException exception) {
            record(resourceType, "failed");

            throw exception;
        }
    }

    private EnvironmentPromotionHandler handler(PromotionResourceType resourceType) {
        EnvironmentPromotionHandler handler = handlers.get(resourceType);

        if (handler == null) {
            throw new ConfigurationException(
                "No promotion handler for %s".formatted(resourceType), EnvironmentPromotionErrorType.UNSUPPORTED_RESOURCE_TYPE);
        }

        return handler;
    }

    private Environment targetEnvironment(long targetEnvironmentId) {
        Environment environment = environmentService.getEnvironment(targetEnvironmentId);

        if (!environmentService.getEnvironments().contains(environment)) {
            throw new ConfigurationException(
                "Environment %s is not available".formatted(environment), EnvironmentPromotionErrorType.ENVIRONMENT_NOT_AVAILABLE);
        }

        return environment;
    }

    private void record(PromotionResourceType resourceType, String outcome) {
        if (meterRegistry != null) {
            meterRegistry.counter(
                "bytechef_environment_promotion", "resource", resourceType.name().toLowerCase(Locale.ROOT), "outcome", outcome)
                .increment();
        }
    }
}
```
The `SAME_ENVIRONMENT` check happens inside each handler (it needs the loaded source) — see Task 9-11.

- [ ] **Step 4: Run → pass; commit** `732 Add EnvironmentPromotionFacade dispatching to per-resource promotion handlers`.

---

## Phase 2 — Handlers

### Task 9: `ApiCollectionPromotionHandler`

**Files:**
- Create: `…/automation-promotion-service/src/main/java/com/bytechef/ee/automation/promotion/handler/ApiCollectionPromotionHandler.java`
- Test: `…/automation-promotion-service/src/test/java/com/bytechef/ee/automation/promotion/handler/ApiCollectionPromotionHandlerTest.java`

**Interfaces:**
- Consumes: `ApiCollectionFacade` (`getApiCollection(long): ApiCollectionDTO` — has `endpoints()`, `projectDeployment()`, `projectId()`, `projectVersion()`, `environment()`, `uuid()`, `name()`, `description()`, `contextPath()`, `collectionVersion()`, `tags()`; `createApiCollection(ApiCollectionDTO)`; `createApiCollectionEndpoint(ApiCollectionEndpointDTO)`; `updateApiCollectionEndpoint(ApiCollectionEndpointDTO)`), `ApiCollectionService` (`fetchApiCollection(UUID, Environment)`, `getApiCollection(long)`, `update(ApiCollection)`, `existsByNameAndEnvironment(...)`), `ApiCollectionEndpointService` (`getApiEndpoints(long)`, `delete(long)`), `ProjectDeploymentService#getProjectDeployment(long)`, `ProjectService#getProject(long)` (`getWorkspaceId()`, `getName()`), `ProjectDeploymentPromoter`, `ConnectionEnvironmentMapper`, `ApiCollectionAuditPublisher` — NO: that class is in the api-platform `-service` module which the promotion module must not import; audit comes from `PermissionAuditAspect` on the guarded method.
- Produces: `EnvironmentPromotionHandler` bean for `API_COLLECTION`. Both methods `@PreAuthorize("hasPermission(@apiCollectionPromotionHandler.projectIdOf(#sourceId), 'Project', 'DEPLOYMENT_PUSH')")` where `public long projectIdOf(long sourceId)` is a public helper on the same bean (bean name `apiCollectionPromotionHandler`). If SpEL bean references are not resolvable in this project's `MethodSecurityExpressionHandler` (check with the authorization pin test in Task 12 — it only asserts the string — AND with the IntTest in Task 14 which exercises the real proxy), fall back to a package-private `ProjectDeploymentPushAuthorizer` bean whose single method is guarded `hasPermission(#projectId, 'Project', 'DEPLOYMENT_PUSH')` and is called first thing in `preview`/`promote`.

- [ ] **Step 1: Failing tests** — cover: (a) preview on a fresh target reports `existingTargetId == null`, one `PromotionProjectPreview` with `targetProjectVersion == null`, connection suggestions from mapper, a warning when source `contextPath` differs from an existing target's; (b) `promote` create path: calls `createApiCollection` with `uuid = source.uuid`, `environment = target`, copied name/description/contextPath/collectionVersion/tags, then `promoter.sync(..., targetIsNew=true)`, then one `createApiCollectionEndpoint` per source endpoint with the same `httpMethod/path/name/enabled/workflowUuid` and `apiCollectionId = created.id`; result `created == true`; (c) `promote` update path: existing target found by `(uuid, env)`; `promoter.sync(..., targetIsNew=false)`; `apiCollectionService.update` called with target id + source `contextPath`/`collectionVersion` but target `name`/`description`/`tagIds`; endpoints reconciled — matching `(httpMethod, path)` → `updateApiCollectionEndpoint` with source `name`/`workflowUuid` and target id, missing → `createApiCollectionEndpoint`, extra target endpoint → `apiCollectionEndpointService.delete`; result `created == false`; (d) same-environment → `ConfigurationException` (`SAME_ENVIRONMENT`); (e) `validatePromotable` invoked before any write (verify order with `InOrder`); (f) `TARGET_NAME_CONFLICT` when `existsByNameAndEnvironment(source.name, workspaceId, target, null)` is true and no lineage target exists.

Write each as a separate `@Test` with Mockito; build DTOs with the canonical constructors (`ApiCollectionDTO` has 20 components incl. the new trailing `uuid`; `ApiCollectionEndpointDTO` has 13).

- [ ] **Step 2: Run → fails.**

- [ ] **Step 3: Implement**

```java
@Component("apiCollectionPromotionHandler")
@ConditionalOnEEVersion
public class ApiCollectionPromotionHandler implements EnvironmentPromotionHandler {

    // fields: apiCollectionEndpointService, apiCollectionFacade, apiCollectionService, connectionEnvironmentMapper,
    //         projectDeploymentPromoter, projectDeploymentService, projectService  (+ constructor)

    @Override
    public PromotionResourceType getResourceType() { return PromotionResourceType.API_COLLECTION; }

    public long projectIdOf(long sourceId) {
        return apiCollectionFacade.getApiCollection(sourceId).projectId();
    }

    @Override
    @PreAuthorize("hasPermission(@apiCollectionPromotionHandler.projectIdOf(#sourceId), 'Project', 'DEPLOYMENT_PUSH')")
    @Transactional(readOnly = true)
    public EnvironmentPromotionPreview preview(long sourceId, Environment targetEnvironment) {
        ApiCollectionDTO source = load(sourceId, targetEnvironment);
        Project project = projectService.getProject(source.projectId());
        Optional<ApiCollection> existing = apiCollectionService.fetchApiCollection(UUID.fromString(source.uuid()), targetEnvironment);
        ProjectDeployment sourceDeployment = source.projectDeployment();

        List<ProjectDeploymentPromoter.SourceBinding> bindings = projectDeploymentPromoter.collectSourceBindings(sourceDeployment);
        Map<Long, Long> existingBindings = existing
            .map(target -> projectDeploymentPromoter.existingTargetBindings(
                sourceDeployment, projectDeploymentService.getProjectDeployment(target.getProjectDeploymentId())))
            .orElse(Map.of());
        Map<Long, Long> suggested = connectionEnvironmentMapper.suggest(
            project.getWorkspaceId(), bindings.stream().map(SourceBinding::connectionId).collect(Collectors.toSet()), targetEnvironment);

        List<String> warnings = new ArrayList<>();

        existing.ifPresent(target -> {
            if (!Objects.equals(target.getContextPath(), source.contextPath())) {
                warnings.add("Context path will change from '%s' to '%s'".formatted(target.getContextPath(), source.contextPath()));
            }

            if (!Objects.equals(target.getCollectionVersion(), source.collectionVersion())) {
                warnings.add("Collection version will change from v%s to v%s".formatted(target.getCollectionVersion(), source.collectionVersion()));
            }
        });

        if (existing.isEmpty()) {
            warnings.add("The promoted collection is created disabled; enable it after reviewing connections.");
        }

        Integer targetVersion = existing
            .map(target -> projectDeploymentService.getProjectDeployment(target.getProjectDeploymentId()).getProjectVersion())
            .orElse(null);

        return new EnvironmentPromotionPreview(
            PromotionResourceType.API_COLLECTION, sourceId, source.environment(), targetEnvironment,
            existing.map(ApiCollection::getId).orElse(null), existing.map(ApiCollection::getName).orElse(null),
            List.of(new PromotionProjectPreview(project.getId(), project.getName(), source.projectVersion(), targetVersion)),
            PromotionPreviews.connectionMappings(bindings, existingBindings, suggested, connectionService),   // shared helper, see below
            warnings);
    }

    @Override
    @PreAuthorize("hasPermission(@apiCollectionPromotionHandler.projectIdOf(#sourceId), 'Project', 'DEPLOYMENT_PUSH')")
    @Transactional
    public EnvironmentPromotionResult promote(long sourceId, Environment targetEnvironment, Map<Long, Long> connectionMappings) {
        ApiCollectionDTO source = load(sourceId, targetEnvironment);
        Project project = projectService.getProject(source.projectId());
        long workspaceId = project.getWorkspaceId();

        projectDeploymentPromoter.validatePromotable(source.projectId(), source.projectVersion());

        Map<Long, Long> requested = connectionEnvironmentMapper.validate(workspaceId, targetEnvironment, connectionMappings);
        Set<Long> sourceConnectionIds = projectDeploymentPromoter.collectSourceBindings(source.projectDeployment())
            .stream().map(SourceBinding::connectionId).collect(Collectors.toSet());
        Map<Long, Long> suggested = connectionEnvironmentMapper.suggest(workspaceId, sourceConnectionIds, targetEnvironment);

        Optional<ApiCollection> existing = apiCollectionService.fetchApiCollection(UUID.fromString(source.uuid()), targetEnvironment);
        boolean created = existing.isEmpty();
        ApiCollection target;

        if (created) {
            if (apiCollectionService.existsByNameAndEnvironment(source.name(), workspaceId, targetEnvironment, null)) {
                throw new ConfigurationException(
                    "An API collection named '%s' already exists in %s".formatted(source.name(), targetEnvironment),
                    EnvironmentPromotionErrorType.TARGET_NAME_CONFLICT);
            }

            ApiCollectionDTO createdDto = apiCollectionFacade.createApiCollection(new ApiCollectionDTO(
                source.collectionVersion(), source.contextPath(), null, null, source.description(), false, List.of(),
                targetEnvironment, null, null, null, source.name(), null, source.projectId(), null, 0,
                source.projectVersion(), source.tags(), 0, source.uuid()));

            target = apiCollectionService.getApiCollection(createdDto.id());
        } else {
            target = existing.get();
        }

        ProjectDeployment targetDeployment = projectDeploymentService.getProjectDeployment(target.getProjectDeploymentId());

        SyncResult syncResult = projectDeploymentPromoter.sync(
            source.projectDeployment(), targetDeployment, requested, suggested, created);

        if (!created) {
            target.setContextPath(source.contextPath());
            target.setCollectionVersion(source.collectionVersion());

            apiCollectionService.update(target);
        }

        reconcileEndpoints(source, target.getId(), created);

        return new EnvironmentPromotionResult(target.getId(), created, null, syncResult.unresolvedConnectionIds());
    }

    private void reconcileEndpoints(ApiCollectionDTO source, long targetId, boolean created) {
        Map<String, ApiCollectionEndpoint> targetByKey = new HashMap<>();

        if (!created) {
            for (ApiCollectionEndpoint endpoint : apiCollectionEndpointService.getApiEndpoints(targetId)) {
                targetByKey.put(endpoint.getHttpMethod() + " " + endpoint.getPath(), endpoint);
            }
        }

        for (ApiCollectionEndpointDTO sourceEndpoint : source.endpoints()) {
            ApiCollectionEndpoint existing = targetByKey.remove(sourceEndpoint.httpMethod() + " " + sourceEndpoint.path());

            if (existing == null) {
                apiCollectionFacade.createApiCollectionEndpoint(new ApiCollectionEndpointDTO(
                    targetId, null, null, sourceEndpoint.enabled(), sourceEndpoint.httpMethod(), null, null, null,
                    sourceEndpoint.name(), sourceEndpoint.path(), 0, 0, sourceEndpoint.workflowUuid()));
            } else {
                apiCollectionFacade.updateApiCollectionEndpoint(new ApiCollectionEndpointDTO(
                    targetId, null, null, false, sourceEndpoint.httpMethod(), existing.getId(), null, null,
                    sourceEndpoint.name(), sourceEndpoint.path(), existing.getProjectDeploymentWorkflowId(),
                    existing.getVersion(), sourceEndpoint.workflowUuid()));
            }
        }

        for (ApiCollectionEndpoint stale : targetByKey.values()) {
            apiCollectionEndpointService.delete(stale.getId());
        }
    }

    private ApiCollectionDTO load(long sourceId, Environment targetEnvironment) {
        ApiCollectionDTO source = apiCollectionFacade.getApiCollection(sourceId);

        if (source.environment() == targetEnvironment) {
            throw new ConfigurationException(
                "Source is already in %s".formatted(targetEnvironment), EnvironmentPromotionErrorType.SAME_ENVIRONMENT);
        }

        return source;
    }
}
```
`updateApiCollectionEndpoint` today only rewrites `httpMethod/name/path` (via `toApiCollectionEndpoint`) — if the source endpoint's `workflowUuid` changed for the same `(method, path)`, the pdw pointer must move too: extend `ApiCollectionFacadeImpl.updateApiCollectionEndpoint` so that when `workflowUuid` is non-null it re-resolves the pdw the same way `createApiCollectionEndpoint` does (find-or-create) and sets `projectDeploymentWorkflowId` — add a unit test for that in the api-platform module (`ApiCollectionFacadeTest`, create if absent).

Create a small package-private helper `PromotionPreviews.connectionMappings(List<SourceBinding> bindings, Map<Long,Long> existingBindings, Map<Long,Long> suggested, ConnectionService connectionService): List<PromotionConnectionMapping>` in the `handler` package that groups bindings by `connectionId`, loads the connections in one `getConnections(ids)` call, builds `usedBy = "<workflowLabel> › <nodeName>"` (or `"component: <name>"` entries passed in by the MCP handler through an overload accepting extra `Map<Long, List<String>> extraUsages`), and picks `suggestedTargetConnectionId = existingBindings.getOrDefault(id, suggested.get(id))`. Unit-test it in `PromotionPreviewsTest`.

- [ ] **Step 4: Run → pass; spotless; commit** `732 Add API collection environment promotion handler`.

---

### Task 10: `McpServerPromotionHandler`

**Files:**
- Create: `…/handler/McpServerPromotionHandler.java`
- Test: `…/handler/McpServerPromotionHandlerTest.java`

**Interfaces:**
- Consumes: `McpServerService` (`getMcpServer(long)`, `fetchMcpServer(UUID, Environment)`, `getMcpServerSecretKey(long)`), `WorkspaceMcpServerService#fetchWorkspaceIdByMcpServerId(Long): Optional<Long>`, `WorkspaceMcpServerFacade#createWorkspaceMcpServer(name, type, env, enabled, authenticationRequired, enforceToolAuthorization, workspaceId, uuid)`, `McpServerFacade` (`getMcpServerTags(List<McpServer>)`, `updateMcpServerTags(long, List<Tag>)`, `create(McpComponent, List<McpTool>)`, `update(McpComponent, List<McpTool>)`, `deleteMcpComponent(long)`), `McpComponentService#getMcpServerMcpComponents(long)`, `McpToolService` (`getMcpComponentMcpTools(long)`, `updateEnabled(long, boolean)`), `McpProjectService#getMcpServerMcpProjects(long)`, `McpProjectFacade` (`createMcpProject(long mcpServerId, long projectId, int projectVersion, List<String> selectedWorkflowIds)`, `updateMcpProject(long, List<String>)`, `deleteMcpProject(long)`), `McpProjectWorkflowService` (`getMcpProjectMcpProjectWorkflows(Long)`, `updateParameters(long, Map)`), `ProjectDeploymentService#getProjectDeployment(long)`, `ProjectDeploymentWorkflowService#getProjectDeploymentWorkflows(long)`, `ProjectWorkflowService#getProjectWorkflows(long, int)`, `ProjectService#getProject(long)`, `ProjectDeploymentPromoter`, `ConnectionEnvironmentMapper`, `ApplicationProperties#getPublicUrl()` (for `targetUrl` — see how `McpServerGraphQlController.updateMcpServerUrl` builds `publicUrl + "/api/automation/" + secretKey + "/mcp"`; reuse the same property).
- Produces: handler bean for `MCP_SERVER`; both methods `@PreAuthorize("hasPermission(@mcpServerPromotionHandler.workspaceIdOf(#sourceId), 'Workspace', 'MCP_CREATE')")` with `public long workspaceIdOf(long sourceId)`.

- [ ] **Step 1: Failing tests** — (a) preview: create vs update detection by `(uuid, env)` within the same workspace (a same-uuid server in another workspace is NOT the target → treated as create); one `PromotionProjectPreview` per source `mcp_project` with the target's version when matched by `projectId`; connection ids = pdw connections ∪ component `connection_id`s; warning "A new server URL/secret key will be generated" on create; ambiguity warning when two source projects share a `projectId`; (b) promote create: `createWorkspaceMcpServer("<source name>", AUTOMATION, target, false, source.authenticationRequired, source.enforceToolAuthorization, workspaceId, source.uuid)`, tags copied via `updateMcpServerTags`, per project `createMcpProject(targetId, projectId, sourceVersion, workflowIdsOfSourceSelection)` then `promoter.sync(..., true)` then `updateParameters(targetMpwId, sourceParameters)` for each workflow matched through `SyncResult.workflowIdMapping`; components created with `connection_id` = requested → suggested → null and tools `(name, parameters)`; `result.targetUrl` non-null; (c) promote update: existing server; enabled/name/auth untouched (verify `mcpServerService.update` never called); project matched by `projectId` → `updateMcpProject(selectedWorkflowIds@sourceVersion)` if the selection changed, `promoter.sync(..., false)`, parameters overwritten; source project absent in target → created; target project absent in source → `deleteMcpProject`; components reconciled by `(componentName, componentVersion)`: existing → `update(component-with-existing-connection-unless-mapped, tools)` then `updateEnabled` re-applied per tool name from the pre-update tool list; missing → `create`; stale → `deleteMcpComponent`; (d) same env rejected; (e) `validatePromotable` per project before writes.

- [ ] **Step 2: Run → fails.**

- [ ] **Step 3: Implement** — structure:

```java
@Component("mcpServerPromotionHandler")
@ConditionalOnEEVersion
public class McpServerPromotionHandler implements EnvironmentPromotionHandler {

    public long workspaceIdOf(long sourceId) {
        return workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(sourceId)
            .orElseThrow(() -> new ConfigurationException("MCP server %s has no workspace".formatted(sourceId), EnvironmentPromotionErrorType.SOURCE_NOT_FOUND));
    }

    @Override @PreAuthorize("hasPermission(@mcpServerPromotionHandler.workspaceIdOf(#sourceId), 'Workspace', 'MCP_CREATE')") @Transactional(readOnly = true)
    public EnvironmentPromotionPreview preview(long sourceId, Environment targetEnvironment) { … }

    @Override @PreAuthorize("hasPermission(@mcpServerPromotionHandler.workspaceIdOf(#sourceId), 'Workspace', 'MCP_CREATE')") @Transactional
    public EnvironmentPromotionResult promote(long sourceId, Environment targetEnvironment, Map<Long, Long> connectionMappings) {
        McpServer source = load(sourceId, targetEnvironment);
        long workspaceId = workspaceIdOf(sourceId);
        List<SourceProject> sourceProjects = sourceProjects(source);            // record SourceProject(McpProject mcpProject, ProjectDeployment deployment, List<McpProjectWorkflow> workflows)

        for (SourceProject sourceProject : sourceProjects) {
            projectDeploymentPromoter.validatePromotable(sourceProject.deployment().getProjectId(), sourceProject.deployment().getProjectVersion());
        }

        Map<Long, Long> requested = connectionEnvironmentMapper.validate(workspaceId, targetEnvironment, connectionMappings);
        Map<Long, Long> suggested = connectionEnvironmentMapper.suggest(workspaceId, sourceConnectionIds(source, sourceProjects), targetEnvironment);

        Optional<McpServer> existing = findTarget(source, workspaceId, targetEnvironment);   // fetchMcpServer(uuid, env) filtered by fetchWorkspaceIdByMcpServerId == workspaceId
        boolean created = existing.isEmpty();
        McpServer target = existing.orElseGet(() -> createTargetServer(source, workspaceId, targetEnvironment));
        Set<Long> unresolved = new LinkedHashSet<>();

        Map<Long, TargetProject> targetProjectsByProjectId = created ? Map.of() : targetProjects(target);   // keyed by deployment.projectId; ascending id order, first wins

        for (SourceProject sourceProject : sourceProjects) {
            long projectId = sourceProject.deployment().getProjectId();
            TargetProject targetProject = targetProjectsByProjectId.remove(projectId);
            List<String> selectedWorkflowIds = workflowIds(sourceProject);       // pdw workflowIds of the source selection (source version)
            boolean projectIsNew = targetProject == null;

            if (projectIsNew) {
                McpProject createdProject = mcpProjectFacade.createMcpProject(target.getId(), projectId, sourceProject.deployment().getProjectVersion(), selectedWorkflowIds);

                targetProject = loadTargetProject(createdProject);
            } else if (!sameSelection(targetProject, sourceProject)) {         // compare by workflow uuid sets
                mcpProjectFacade.updateMcpProject(targetProject.mcpProject().getId(), selectedWorkflowIdsAtVersion(sourceProject, targetProject.deployment().getProjectVersion()));
                targetProject = loadTargetProject(targetProject.mcpProject());
            }

            SyncResult syncResult = projectDeploymentPromoter.sync(sourceProject.deployment(), targetProject.deployment(), requested, suggested, projectIsNew);

            unresolved.addAll(syncResult.unresolvedConnectionIds());
            syncParameters(sourceProject, loadTargetProject(targetProject.mcpProject()), syncResult.workflowIdMapping());   // targetMpw by pdw id → updateParameters(source params)
        }

        for (TargetProject stale : targetProjectsByProjectId.values()) {
            mcpProjectFacade.deleteMcpProject(stale.mcpProject().getId());
        }

        unresolved.addAll(reconcileComponents(source, target, requested, suggested, created));

        String targetUrl = applicationProperties.getPublicUrl() + "/api/automation/" + mcpServerService.getMcpServerSecretKey(target.getId()) + "/mcp";

        return new EnvironmentPromotionResult(target.getId(), created, targetUrl, List.copyOf(unresolved));
    }
}
```
`selectedWorkflowIdsAtVersion(sourceProject, version)` maps the source selection's uuids to workflowIds at the TARGET's current version (needed because `updateMcpProject` runs before `sync` bumps the version) — if a uuid has no row at that version, skip it (sync will add the row after the bump; then a second `updateMcpProject` pass after `sync` with the source-version ids finishes the reconciliation). Simplest correct order: (1) `sync` first (bumps version, reconciles pdw rows to exactly the source selection — `checkProjectDeploymentWorkflows` deletes pdw rows not in the passed list, so `mcp_project_workflow` rows pointing at them must be deleted BEFORE calling sync: delete stale `mcp_project_workflow` rows whose uuid is not in the source selection first via `mcpProjectWorkflowService.delete(id)`; the FK is on `mcp_project_workflow → project_deployment_workflow` so the child must go first), (2) create `mcp_project_workflow` rows for pdw ids in `workflowIdMapping` that have no row yet (`mcpProjectWorkflowService.create(mcpProjectId, targetPdwId)`), (3) `updateParameters`. Do NOT call `updateMcpProject` at all — it recreates pdw rows itself and would fight `sync`. Write the implementation with that ordering and drop `sameSelection`/`selectedWorkflowIdsAtVersion`.

`reconcileComponents`: load source components + tools; target components (if not created); for each source `(componentName, componentVersion)`: connection = `requested.get(srcConn) ?: (existingTargetComponent?.connectionId) ?: suggested.get(srcConn)`; unresolved when source had a connection and nothing resolved; `create`/`update` through `McpServerFacade` with `new McpTool(name, parameters)` per source tool; after `update`, re-apply `updateEnabled(newToolId, previousEnabledByName.getOrDefault(name, true))`; delete stale target components. Authorities are not copied.

- [ ] **Step 4: Run → pass; commit** `732 Add MCP server environment promotion handler`.

---

### Task 11: `A2aServerPromotionHandler`

**Files:**
- Create: `…/handler/A2aServerPromotionHandler.java`
- Test: `…/handler/A2aServerPromotionHandlerTest.java`

**Interfaces:**
- Consumes: `A2aServerService` (`getA2aServer(long)`, `fetchA2aServer(UUID, Environment)`, `create(A2aServer)`), `A2aProjectService#getA2aServerA2aProjects(long)` (verify the exact name in `A2aProjectService`; if it is `getA2aProjects(a2aServerId)` use that), `A2aProjectFacade` (`createA2aProject(long, long, int, List<String>)`, `deleteA2aProject(long)`), `A2aProjectWorkflowService` (`getA2aProjectA2aProjectWorkflows(Long)`, `create(Long, Long)`, `delete(long)`, `updateParameters(long, Map)`), `ProjectDeploymentService`, `ProjectDeploymentWorkflowService`, `ProjectWorkflowService`, `ProjectService`, `ProjectDeploymentPromoter`, `ConnectionEnvironmentMapper`, `ApplicationProperties#getPublicUrl()`.
- Produces: handler bean for `A2A_SERVER`; both methods `@PreAuthorize("hasAuthority('ROLE_ADMIN')")`. Workspace for connection mapping = `projectService.getProject(firstSourceProject.projectId).getWorkspaceId()` (A2A servers are tenant-global; a server with zero projects has no connections to map — pass any workspace id `0` guard: skip mapping when there are no projects).

- [ ] **Step 1: Failing tests** — mirror Task 10's (a)-(e) minus workspace/tags/components: create path builds `new A2aServer(source.name, source.description, AUTOMATION, target)` with `setEnabled(false)`, `setAuthenticationRequired(source.isAuthenticationRequired())`, `setUuid(source.uuid)` and calls `a2aServerService.create`; projects via `createA2aProject`; per-workflow parameters (`skillName`, `skillDescription`, `skillTags`) copied on create and overwritten on update; `targetUrl = publicUrl + "/api/automation/a2a/" + secretKey + "/.well-known/agent-card.json"`.

- [ ] **Step 2: Run → fails. Step 3: Implement** with the same sync ordering as Task 10 (delete stale `a2a_project_workflow` rows first → `sync` → create missing rows from `workflowIdMapping` → `updateParameters`). **Step 4: pass; commit** `732 Add A2A server environment promotion handler`.

---

### Task 12: Authorization pin test

**Files:**
- Test: `…/automation-promotion-service/src/test/java/com/bytechef/ee/automation/promotion/handler/PromotionHandlerAuthorizationTest.java`

- [ ] **Step 1: Write the test** in the style of `ApiClientServiceAuthorizationTest` (reflection over `preview`/`promote` on each handler class asserting the exact `@PreAuthorize` value):

| Class | Expression |
|---|---|
| `ApiCollectionPromotionHandler` | `hasPermission(@apiCollectionPromotionHandler.projectIdOf(#sourceId), 'Project', 'DEPLOYMENT_PUSH')` |
| `McpServerPromotionHandler` | `hasPermission(@mcpServerPromotionHandler.workspaceIdOf(#sourceId), 'Workspace', 'MCP_CREATE')` |
| `A2aServerPromotionHandler` | `hasAuthority('ROLE_ADMIN')` |

- [ ] **Step 2: Run → pass (Tasks 9-11 already carry the annotations). Commit** `732 Pin promotion handler authorization expressions`.

---

## Phase 3 — GraphQL

### Task 13: Schema + controller

**Files:**
- Create: `…/automation-promotion-graphql/src/main/resources/graphql/environment-promotion.graphqls`
- Create: `…/automation-promotion-graphql/src/main/java/com/bytechef/ee/automation/promotion/web/graphql/EnvironmentPromotionGraphQlController.java`
- Test: `…/automation-promotion-graphql/src/test/java/com/bytechef/ee/automation/promotion/web/graphql/EnvironmentPromotionGraphQlControllerTest.java`

- [ ] **Step 1: Schema** (paste verbatim from spec §7.4, plus a `description` on each type):

```graphql
enum PromotionResourceType { API_COLLECTION MCP_SERVER A2A_SERVER }

type PromotionConnectionMapping {
    sourceConnectionId: ID!
    sourceConnectionName: String!
    componentName: String!
    connectionVersion: Int!
    suggestedTargetConnectionId: ID
    usedBy: [String!]!
}

type PromotionProjectPreview {
    projectId: ID!
    projectName: String!
    sourceProjectVersion: Int!
    targetProjectVersion: Int
}

type EnvironmentPromotionPreview {
    resourceType: PromotionResourceType!
    sourceId: ID!
    sourceEnvironmentId: ID!
    targetEnvironmentId: ID!
    existingTargetId: ID
    existingTargetName: String
    projects: [PromotionProjectPreview!]!
    connections: [PromotionConnectionMapping!]!
    warnings: [String!]!
}

input PromotionConnectionMappingInput { sourceConnectionId: ID!, targetConnectionId: ID! }

input PromoteToEnvironmentInput {
    resourceType: PromotionResourceType!
    sourceId: ID!
    targetEnvironmentId: ID!
    connectionMappings: [PromotionConnectionMappingInput!]!
}

type EnvironmentPromotionResult {
    targetId: ID!
    created: Boolean!
    targetUrl: String
    unresolvedConnectionIds: [ID!]!
}

extend type Query {
    """Preview what promoting a resource into another environment will do (create vs. update, version moves,
    suggested connection re-bindings, warnings)."""
    environmentPromotionPreview(resourceType: PromotionResourceType!, sourceId: ID!, targetEnvironmentId: ID!): EnvironmentPromotionPreview!
}

extend type Mutation {
    """Promote a resource into another environment: creates the counterpart on first promotion, syncs it afterwards."""
    promoteToEnvironment(input: PromoteToEnvironmentInput!): EnvironmentPromotionResult!
}
```

- [ ] **Step 2: Controller**

```java
@Controller
@ConditionalOnEEVersion
public class EnvironmentPromotionGraphQlController {

    private final EnvironmentPromotionFacade environmentPromotionFacade;

    // ctor

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public EnvironmentPromotionPreviewModel environmentPromotionPreview(
        @Argument PromotionResourceType resourceType, @Argument long sourceId, @Argument long targetEnvironmentId) {

        return toModel(environmentPromotionFacade.preview(resourceType, sourceId, targetEnvironmentId));
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public EnvironmentPromotionResult promoteToEnvironment(@Argument PromoteToEnvironmentInput input) {
        Map<Long, Long> mappings = new HashMap<>();

        for (PromotionConnectionMappingInput mapping : input.connectionMappings()) {
            mappings.put(mapping.sourceConnectionId(), mapping.targetConnectionId());
        }

        return environmentPromotionFacade.promote(input.resourceType(), input.sourceId(), input.targetEnvironmentId(), mappings);
    }

    record PromoteToEnvironmentInput(PromotionResourceType resourceType, long sourceId, long targetEnvironmentId, List<PromotionConnectionMappingInput> connectionMappings) {}
    record PromotionConnectionMappingInput(long sourceConnectionId, long targetConnectionId) {}
    /** GraphQL exposes environments as ordinal ids; the facade DTO carries the enum. */
    record EnvironmentPromotionPreviewModel(PromotionResourceType resourceType, long sourceId, long sourceEnvironmentId, long targetEnvironmentId, @Nullable Long existingTargetId, @Nullable String existingTargetName, List<PromotionProjectPreview> projects, List<PromotionConnectionMapping> connections, List<String> warnings) {}
}
```

- [ ] **Step 3: Controller unit test** — mock the facade; assert `promoteToEnvironment` folds the input list into a `Map<Long,Long>` and `environmentPromotionPreview` converts environments to ordinals.

- [ ] **Step 4: Compile server-app; run test; commit** `732 Add environment promotion GraphQL query and mutation`.

---

## Phase 4 — Integration test

### Task 14: `EnvironmentPromotionIntTest` (Testcontainers)

**Files:**
- Test: `…/automation-promotion-service/src/test/java/com/bytechef/ee/automation/promotion/EnvironmentPromotionIntTest.java`
- Test config: `…/automation-promotion-service/src/test/java/com/bytechef/ee/automation/promotion/config/EnvironmentPromotionIntTestConfiguration.java`
- Test resources: `…/automation-promotion-service/src/test/resources/config/application-testint.yml`
- Modify: `automation-promotion-service/build.gradle.kts` (add `testImplementation` for `automation-configuration-service`, `automation-api-platform-configuration-service`, `automation-ai-mcp-service`, `platform-mcp-service`, `automation-ai-a2a-service`, `platform-connection-service`, `liquibase-config`, `test-int-support` / Testcontainers, `spring-boot-starter-test`, `spring-security-test` — copy the dependency block from an existing IntTest module such as `automation-api-platform-configuration-service` or `automation-ai-mcp-service` and their `*IntTestConfiguration` classes as the template).

- [ ] **Step 1: Assemble the context** — `@SpringBootTest(classes = EnvironmentPromotionIntTestConfiguration.class)`, `@ActiveProfiles("testint")`, `@Import(PostgreSQLContainerConfiguration.class)` (find the exact shared Testcontainers configuration class name with `grep -rn "PostgreSQLContainer" server/libs --include=*Configuration.java -l | head`), `@ComponentScan` over `com.bytechef.ee.automation.promotion`, `com.bytechef.automation.configuration`, `com.bytechef.ee.automation.apiplatform.configuration`, `com.bytechef.automation.ai.mcp`, `com.bytechef.platform.mcp`, `com.bytechef.automation.ai.a2a`, `com.bytechef.platform.connection`, `com.bytechef.platform.tag`; mock beans for anything unrelated that fails to wire (`WorkflowService`? no — real; `MailService`, `MessageBroker`, etc. → `@MockitoBean` as the template modules do). Run with `@WithMockUser(authorities = "ROLE_ADMIN")` and `bytechef.edition=ee` in the yml.

- [ ] **Step 2: Scenarios**

```java
@Test
void testApiCollectionFirstPromotionCreatesCounterpartAndRePromotionSyncsInPlace() {
    // arrange: workspace, project with workflow W (uuid U), publish v1, connection devSlack (DEV) + stagingSlack (STAGING, same name/component)
    //          api collection "billing" in DEV pinned v1, endpoint GET /x → U, pdw connection node "n1"/key "slack" → devSlack
    // act 1: promote(API_COLLECTION, id, STAGING, {})
    // assert: new api_collection with same uuid, deployment env STAGING, enabled=false, endpoint GET /x present,
    //         pdw connection → stagingSlack (auto-mapped), result.created=true
    // arrange 2: publish v2 (W keeps uuid U, new workflowId), change-version the DEV collection to v2, add endpoint POST /y → U
    // act 2: promote again
    // assert: same target id (created=false), target deployment version 2, the ORIGINAL target pdw row id unchanged
    //         (SELECT id FROM project_deployment_workflow WHERE project_deployment_id = target AND …), endpoint GET /x's
    //         project_deployment_workflow_id unchanged, POST /y created, stagingSlack binding preserved
}

@Test
void testMcpServerPromotionCopiesParametersAndSyncsOnRePromotion() { … same shape: mcp server DEV with one project + workflow tool
    (parameters toolName/toolDescription) + one component with a connection → promote → target server (uuid, env=STAGING,
    enabled=false, new secretKey), mcp_project_workflow.parameters equal, component connection remapped; re-promote after
    changing toolDescription in DEV → target parameters updated, target `enabled` untouched … }

@Test
void testA2aServerPromotionCreatesTargetInTargetEnvironment() { … a2a server DEV with a project → promote → deployment env STAGING (Task 1 fix), parameters copied … }

@Test
void testUniqueConstraintsMigrated() {
    // jdbcTemplate: assert no constraint named uk_api_collection_name / uk_mcp_server_name in information_schema.table_constraints;
    // assert uk_mcp_server_name_environment, uk_mcp_server_uuid_environment, uk_a2a_server_uuid_environment exist;
    // insert two mcp_server rows same name different environment → OK; same name same environment → DataIntegrityViolationException
}
```

- [ ] **Step 3: Run** `./gradlew :server:ee:libs:automation:automation-promotion:automation-promotion-service:testIntegration > /tmp/t14.log 2>&1; echo $?` → 0. Fix wiring until green (this is where the SpEL bean-reference guard in Tasks 9/10 is proven; if it fails to resolve, switch to the authorizer-bean fallback described in Task 9 and update the pin test in Task 12).

- [ ] **Step 4: Commit** `732 Add environment promotion integration test`.

---

## Phase 5 — Client

### Task 15: GraphQL operations + codegen

**Files:**
- Modify: `client/codegen.ts` — add `'../server/ee/libs/automation/automation-promotion/automation-promotion-graphql/src/main/resources/graphql/*.graphqls',` after the a2a line (77).
- Create: `client/src/graphql/automation/promotion/environmentPromotionPreview.graphql`, `client/src/graphql/automation/promotion/promoteToEnvironment.graphql`
- Regenerate: `client/src/shared/middleware/graphql.ts`

- [ ] **Step 1: Operations**

```graphql
query environmentPromotionPreview($resourceType: PromotionResourceType!, $sourceId: ID!, $targetEnvironmentId: ID!) {
    environmentPromotionPreview(resourceType: $resourceType, sourceId: $sourceId, targetEnvironmentId: $targetEnvironmentId) {
        resourceType
        sourceId
        sourceEnvironmentId
        targetEnvironmentId
        existingTargetId
        existingTargetName
        projects { projectId projectName sourceProjectVersion targetProjectVersion }
        connections { sourceConnectionId sourceConnectionName componentName connectionVersion suggestedTargetConnectionId usedBy }
        warnings
    }
}
```
```graphql
mutation promoteToEnvironment($input: PromoteToEnvironmentInput!) {
    promoteToEnvironment(input: $input) {
        targetId
        created
        targetUrl
        unresolvedConnectionIds
    }
}
```

- [ ] **Step 2: `cd client && npx graphql-codegen`** → generates `useEnvironmentPromotionPreviewQuery`, `usePromoteToEnvironmentMutation`, `PromotionResourceType`.

- [ ] **Step 3: Commit operations and generated file separately**

```bash
git add client/codegen.ts client/src/graphql/automation/promotion
git commit -m "732 client - Add environment promotion GraphQL operations"
git add client/src/shared/middleware/graphql.ts
git commit -m "732 client - Regenerate GraphQL client for environment promotion"
```

---

### Task 16: `EnvironmentPromotionDialog` (EE)

**Files:**
- Create: `client/src/ee/shared/components/environment-promotion/EnvironmentPromotionDialog.tsx`
- Create: `client/src/ee/shared/components/environment-promotion/EnvironmentPromotionConnectionRow.tsx`
- Create: `client/src/ee/shared/components/environment-promotion/hooks/useEnvironmentPromotionDialog.ts`
- Test: `client/src/ee/shared/components/environment-promotion/EnvironmentPromotionDialog.test.tsx`

**Interfaces:**
- Consumes: `useEnvironmentsQuery`, `useEnvironmentPromotionPreviewQuery`, `usePromoteToEnvironmentMutation`, `useGetWorkspaceConnectionsQuery` (from `@/shared/middleware/automation/configuration`, same import as `ProjectDeploymentDialogWorkflowsStepItemConnection.tsx:14`), `useEnvironmentStore`, `ENVIRONMENT_CONFIGS` (`@/shared/constants/environmentConfigs`), `useToast` (grep the project's toast hook: `@/hooks/use-toast`), Dialog/Select/Button primitives from `@/components/ui/*`.
- Produces:
```ts
export interface EnvironmentPromotionDialogProps {
    onClose: () => void;
    onPromoted?: (result: {created: boolean; targetEnvironmentId: number; targetId: string}) => void;
    resourceType: PromotionResourceType;
    sourceEnvironmentId: number;
    sourceId: string;
    sourceName: string;
    workspaceId: number;
}
export default function EnvironmentPromotionDialog(props: EnvironmentPromotionDialogProps): JSX.Element
```

- [ ] **Step 1: Failing tests** (`vitest`, `@testing-library/react`; mock the three GraphQL hooks + `useGetWorkspaceConnectionsQuery` with `vi.hoisted` refs per CLAUDE.md):
  - default target = next environment after the source (source DEV → STAGING; source PROD → DEV; environments from `useEnvironmentsQuery` = 3);
  - renders "Will create" when `existingTargetId == null`, "Will update <name>" otherwise; renders each warning; renders "v1 → v3" per project;
  - one row per connection with the suggested target pre-selected; choosing "Unresolved" removes it from the payload;
  - Promote calls the mutation with `{input: {resourceType, sourceId, targetEnvironmentId, connectionMappings: [{sourceConnectionId, targetConnectionId}]}}` containing only resolved rows;
  - on success `onPromoted` is called and the toast action "View in STAGING" sets `useEnvironmentStore.getState().currentEnvironmentId === 1` (assert via `waitFor` on the store, never a fixed sleep).

- [ ] **Step 2: Implement** — hook `useEnvironmentPromotionDialog({resourceType, sourceId, sourceEnvironmentId, workspaceId})` owns: `targetEnvironmentId` state (default = `(sourceEnvironmentId + 1) % environments.length` skipping the source), the preview query (`enabled: targetEnvironmentId != null`), `mappings: Record<string, string | undefined>` initialised from `preview.connections[].suggestedTargetConnectionId` whenever the preview changes, the mutation, and `handlePromote`. Component: `Dialog` with title `Promote "${sourceName}"`, `Select` of environments (excluding the source, labelled via `ENVIRONMENT_CONFIGS[id].label`), a summary block (create/update badge, projects list, warnings as `Alert`), a `fieldset className="border-0"` of `EnvironmentPromotionConnectionRow`s (each: source name + component + `usedBy` chips, a `Select` fed by `useGetWorkspaceConnectionsQuery({componentName, connectionVersion, environmentId: targetEnvironmentId, id: workspaceId})` plus an "Unresolved" option), footer Cancel/Promote (Promote disabled while preview loading or mutation pending). On success: `toast({title: \`Promoted to ${label}\`, action: <ToastAction onClick={() => setCurrentEnvironmentId(targetEnvironmentId)}>View in {label}</ToastAction>})`, `onPromoted?.(…)`, `onClose()`. When `result.unresolvedConnectionIds.length > 0` add a second line "N connections still need a binding".

- [ ] **Step 3: `npm run check` → green; commit** `732 client - Add EnvironmentPromotionDialog`.

---

### Task 17: Menu wiring on the three list rows

**Files:**
- Modify: `client/src/ee/pages/automation/api-platform/api-collections/components/ApiCollectionListItemDropDownMenu.tsx` (+ `ApiCollectionListItem.tsx` to hold dialog state; static import of the EE dialog is fine — this page is EE)
- Modify: `client/src/pages/automation/mcp-servers/components/mcp-server-list/McpServerListItemDropdownMenu.tsx` + `McpServerListItem.tsx`
- Modify: `client/src/pages/automation/a2a-servers/components/A2aServerListItem.tsx`
- Tests: extend the existing `*.test.tsx` next to each list item if present (`McpProjectWorkflowDialog.test.tsx` shows the file's test conventions), else add `A2aServerListItem.test.tsx` covering "menu item hidden in CE / shown in EE".

- [ ] **Step 1: API collections** — add `onPromoteClick` prop and a `<DropdownMenuItem onClick={onPromoteClick}>Promote to environment…</DropdownMenuItem>` after "Change Project Version"; in `ApiCollectionListItem` hold `showPromotionDialog` state and render `<EnvironmentPromotionDialog resourceType="API_COLLECTION" sourceId={String(apiCollection.id)} sourceName={apiCollection.name} sourceEnvironmentId={apiCollection.environmentId} workspaceId={currentWorkspaceId} onClose=… onPromoted={() => queryClient.invalidateQueries({queryKey: ApiCollectionKeys.apiCollections})} />` when open. Hide the item when `useEnvironmentsQuery().data?.environments.length < 2`.

- [ ] **Step 2: MCP + A2A (CE files)** — same item, wrapped:
```tsx
const EnvironmentPromotionDialog = lazy(
    () => import('@/ee/shared/components/environment-promotion/EnvironmentPromotionDialog')
);
…
<EEVersion hidden={true}>
    <DropdownMenuItem onClick={onPromoteClick}>Promote to environment…</DropdownMenuItem>
</EEVersion>
…
{showPromotionDialog && (
    <EEVersion hidden={true}>
        <Suspense fallback={null}>
            <EnvironmentPromotionDialog resourceType="MCP_SERVER" … onPromoted={() => queryClient.invalidateQueries({queryKey: ['workspaceMcpServers']})} />
        </Suspense>
    </EEVersion>
)}
```
(A2A: `resourceType="A2A_SERVER"`, invalidate `['a2aServers']`; `sourceEnvironmentId={+a2aServer.environmentId}`.) `EEVersion` from `@/shared/edition/EEVersion` — a CE import; the only `@/ee` reference is inside the dynamic `import()`, which is the sanctioned seam.

- [ ] **Step 3: `npm run check`; commit** `732 client - Add Promote to environment action to API collection, MCP server and A2A server rows`.

---

## Phase 6 — Docs

### Task 18: User docs + CLAUDE.md

**Files:**
- Modify: `docs/content/docs/platform/automation/deploy/api-platform.mdx`, `mcp-servers.mdx`, `a2a-servers.mdx` — add a `## Promote to another environment` section: how to open it (row menu → Promote to environment…), what is copied on first promotion vs. synced afterwards (spec §4 ⚑3, verbatim list), that the counterpart is created disabled, that MCP/A2A counterparts get a new secret key/URL, connection mapping and "Unresolved", the published/non-draft requirement.
- Modify: `docs/content/docs/platform/enterprise/collaboration-devops/build-once-deploy-many.mdx` — under "The promotion gate", add one sentence linking the three surface docs.
- Modify: `CLAUDE.md` — add subsection "### Environment promotion (EE)" after "### A2A servers (Agent2Agent, automation)": module `server/ee/libs/automation/automation-promotion`; lineage = `uuid` on `api_collection`/`mcp_server`/`a2a_server`, counterpart = `(uuid, environment)`; handlers mint via each surface's create path then `ProjectDeploymentPromoter.sync` (in-place-by-uuid keeps mapping-row FKs); sync-vs-local rule; `uk_api_collection_name` gone → app-level `(workspace, environment)` check; MCP `(name, environment)` UK; created counterparts disabled; metric `bytechef_environment_promotion{resource,outcome}`; guards per surface; A2A deployments now inherit the server environment.

- [ ] **Step 1: Write the docs. Step 2: Commit** `732 Document environment promotion for API collections, MCP servers and A2A servers` (docs + CLAUDE.md in one commit; no ticket-less prefix — repo history uses `732` for these).

---

## Self-review against the spec (done while writing; re-run after execution)

| Spec section | Task |
|---|---|
| §4 1 lineage uuid, 5.1–5.3 schema | 2, 3, 4 |
| §4 2 any target env | 8 (facade validates availability), 16 (dialog offers all but source) |
| §4 3 sync-vs-local | 7 (`targetIsNew`), 9–11 (per-surface field lists), 14 (IntTest pins) |
| §4 4 exact-match mapping, existing wins | 6, 7 (`existingTargetBindings`) |
| §4 5 unresolved allowed, created disabled | 7 (unresolved list), 9–11 (`enabled=false` on create), 16 (warning) |
| §4 6 api_collection app-level uniqueness | 2 |
| §4 7 EE placement | 5 |
| §4 8 generic GraphQL | 13, 15 |
| §4 9 guards | 9, 10, 11, 12 |
| §4 10 published/non-draft | 7 (`validatePromotable`), 9–11 call it first |
| §4 11 / §7.6 A2A env fix | 1 |
| §4 12 surface create paths mint | 9–11 |
| §6.5 MCP project matching + ambiguity warning | 10 |
| §6.6 URLs/secret keys | 10, 11 (`targetUrl`), 16 (shown), 18 |
| §7.5 clone tool doc fixes | 2, 3 |
| §8 client | 15, 16, 17 |
| §10 metric + audit-by-aspect | 8, 12 |
| §11 tests | 1–4, 6–13 unit; 14 IntTest; 16 client |
| §7.7 docs / CLAUDE.md | 18 |

Placeholder scan: every task carries concrete code or an exact field/behaviour list; the two "verify the exact name" notes (Task 11 `A2aProjectService` finder, Task 5 module paths) are lookups, not deferred design.

Type consistency: `SyncResult(workflowIdMapping, unresolvedConnectionIds, warnings)` is the canonical shape (Task 7 Interfaces + implementation agree); Tasks 9–11 read `unresolvedConnectionIds()` into the result and log `warnings()`. `PromotionResourceType`, `EnvironmentPromotionPreview`, `EnvironmentPromotionResult`, `PromotionConnectionMapping`, `PromotionProjectPreview` are used with the Task 5 component order everywhere (Tasks 8, 9, 13, 15). Handler bean names `apiCollectionPromotionHandler` / `mcpServerPromotionHandler` match between the `@Component("…")` value (Tasks 9, 10) and the SpEL in the pin test (Task 12).
