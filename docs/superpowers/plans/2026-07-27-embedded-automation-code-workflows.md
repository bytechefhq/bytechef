# Embedded Automation Code Workflows (Bridge + Shared Deploy) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a vendor author an automation-style code workflow (the `ProjectHandler` / `project-api` contract), deploy it once through an embedded admin endpoint, and serve it to connected users by *reference* (never a per-user copy) through the two existing public embedded invocation endpoints (`POST /workflows/{workflowUuid}` sync, `POST /app-events` async).

**Architecture:** The artifact is built exactly like an automation code workflow (`ProjectHandlerLoader` → `CodeWorkflowContainerFacade.create(..., PlatformType.AUTOMATION)`), but resolved/created behind the existing `AutomationWorkflowProjectFacade` `__EMBEDDED_AUTOMATION__` marker so it lands in the same hidden catalog as visual bridge templates. `ConnectedUserProjectWorkflow` gains a nullable `catalog_workflow_uuid` discriminator: non-null means "this row is a reference to a shared catalog workflow," not a per-user copy. A reference is backed by a dedicated per-(catalog project, connected user) `ProjectDeployment` — the exact principal shape `PlatformType.AUTOMATION` jobs already expect — so invocation reuses the **existing, unmodified** `AbstractWebhookTriggerController.doProcessTrigger` / `WebhookWorkflowExecutor` pipeline with `PlatformType.AUTOMATION` instead of `PlatformType.EMBEDDED`, inheriting connection-injection, response shaping, and sync/async semantics for free. Redeploy flips references whose workflow disappeared to a `dangling` state instead of deleting them.

**Tech Stack:** Java 25, Spring Boot 4.0.7, Spring Data JDBC, Liquibase, JUnit 5, Mockito, Testcontainers, GraalVM Polyglot (code workflow loading).

## Global Constraints

- Spec: `docs/superpowers/specs/2026-07-27-embedded-automation-code-workflows-design.md`. Read it before Task 1.
- **Decisions made while planning that the spec left open** (see "Ambiguities resolved" at the end of this file for the full rationale):
  1. The per-user reference identifier is the **catalog workflow's own uuid** (`ProjectWorkflow.uuid`) — a reference row has no uuid of its own; callers always address it by the catalog uuid, same as they already do for the catalog listing and the sync endpoint's path variable.
  2. Per-user connection wiring for a reference is **not** stored in `WorkflowTestConfiguration` (that table is keyed by `workflowId` alone, and a shared catalog workflow has exactly one `workflowId` shared by every referencing user — reusing it would let one user's connection choice leak into another user's run). It lives in a new join table, `connected_user_project_workflow_connection`, scoped by the join row's own id.
  3. A reference is backed by a **dedicated `ProjectDeployment`** row against the catalog project (one per connected user, looked up by name, not by `ProjectDeploymentService.fetchProjectDeployment(projectId, environment)` — that method assumes one deployment per project+environment, which holds for copy-mode's per-user private projects but not for a shared catalog project). This is what makes invocation reuse the existing per-task connection-injection pipeline (`ProjectDeploymentWorkflowConnection` → `ProjectTaskDispatcherPreSendProcessor`) instead of inventing a new one.
  4. Invocation reuses `AbstractWebhookTriggerController.doProcessTrigger` unmodified, with a `WorkflowExecutionId` built as `WorkflowExecutionId.of(PlatformType.AUTOMATION, projectDeploymentId, catalogWorkflowUuid, triggerName)` instead of `PlatformType.EMBEDDED`. This is an inferred-but-testable assumption (Task 4 Step 2 either confirms it against real Spring wiring or fails loudly) rather than something read directly out of `WebhookWorkflowExecutorImpl`'s source — flagged so the implementing agent double-checks it early.
- Embedded entities keep hiding the automation `Project`: nothing on any public embedded surface exposes a `projectId`.
- Integration-workflow resolution on both existing endpoints stays **byte-for-byte unchanged** — regression-pinned by dedicated tests in Tasks 4 and 5. All new logic is a fallback branch that only runs once the existing integration lookup comes back empty.
- References are **never editable**: no update-definition API exists or should exist for a `catalog_workflow_uuid != null` row.
- EE files (everything under `server/ee/`) use the ByteChef Enterprise license header and `@version ee` Javadoc tag. CE files (the two `automation-configuration` CE-module additions in Task 2/3) use the Apache 2.0 header, no `@version ee` tag.
- No SDK changes. `sdks/backend/java/workflow-api` / `automation/project-api` / `embedded/integration-api` are untouched.
- All new schema follows the workspace-relation convention: nullable discriminator/scope columns directly on the existing table, not a new relation table, because this is a genuinely 1:1 "what kind of row is this" distinction, not a many-to-many membership.
- Run `./gradlew spotlessApply` before every commit. Check Gradle results by redirecting to a file and testing `$?` — a run piped into `tail` reports the filter's exit code, not Gradle's. Never judge Gradle through a pipe.
- Mockito wrapper-default-0 trap: several new methods branch on a nullable `Long`/boxed id (`fetchProjectIdByName`, `catalogWorkflowUuid` presence). Stub `thenReturn(null)` explicitly wherever a test needs to observe the null branch — an unstubbed wrapper-returning mock method returns `0`/`false`, not `null`.

---

## File Structure

**Created:**
- `.../embedded-configuration-service/src/main/resources/config/liquibase/changelog/embedded/configuration/20260727120000_embedded_configuration_added_code_workflow_reference.xml` — schema
- `.../embedded-configuration-api/.../domain/ConnectedUserProjectWorkflowConnection.java` — per-reference connection row
- `.../embedded-configuration-service/.../repository/ConnectedUserProjectWorkflowConnectionRepository.java`
- `.../embedded-configuration-api/.../exception/MissingConnectionException.java`
- `.../embedded-configuration-api/.../facade/AutomationWorkflowProjectCodeWorkflowFacade.java`
- `.../embedded-configuration-service/.../facade/AutomationWorkflowProjectCodeWorkflowFacadeImpl.java`
- `.../embedded-configuration-rest-impl/.../web/rest/AutomationProjectCodeWorkflowApiController.java`
- `.../embedded-configuration-api/.../facade/ConnectedUserCodeWorkflowReferenceFacade.java`
- `.../embedded-configuration-service/.../facade/ConnectedUserCodeWorkflowReferenceFacadeImpl.java`
- `.../embedded-configuration-service/.../facade/ConnectedUserWorkflowConnectionResolver.java`
- `.../embedded-configuration-service/src/test/java/.../ee/embedded/configuration/AutomationCodeWorkflowBridgeIntTest.java`
- `docs/content/docs/embedded/automation-code-workflows.mdx`

**Modified:**
- `.../embedded-configuration-api/.../domain/ConnectedUserProjectWorkflow.java` — reference columns
- `.../embedded-configuration-service/.../repository/ConnectedUserProjectWorkflowRepository.java` — finder by catalog uuid
- `.../embedded-configuration-api/.../facade/AutomationWorkflowProjectFacade.java` (+ Impl) — `fetchProjectIdByName`
- `server/libs/automation/automation-configuration/automation-configuration-api/.../service/ProjectDeploymentService.java` (+ Impl in `automation-configuration-service`) — `fetchProjectDeploymentByName`
- `.../embedded-configuration-api/.../service/IntegrationWorkflowService.java` (+ Impl) — `fetchLastWorkflowId`
- `.../embedded-webhook-public-rest/.../RequestTriggerApiController.java` — sync bridge branch
- `.../embedded-webhook-public-rest/.../AppEventTriggerApiController.java` — async bridge branch
- `.../embedded-configuration-rest-impl/openapi.yaml` — `/automation/projects/deploy`
- `.../embedded-configuration-public-rest/openapi.yaml` — `kind` field, provision/deprovision paths
- `.../embedded-configuration-public-rest/.../web/rest/ConnectedUserProjectWorkflowApiController.java` — provision/deprovision handlers
- `.../embedded-configuration-public-rest/.../mapper/AutomationWorkflowProjectMapper.java` — `kind` mapping
- `.../embedded-configuration-service/build.gradle.kts` — new module dependencies
- `claude-code-plugin/bytechef-dev/skills/bytechef-code-workflow/SKILL.md` — bridge section
- `CLAUDE.md`

---

### Task 1: Schema and domain — reference, connection, and dangling state

**Files:**
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/resources/config/liquibase/changelog/embedded/configuration/20260727120000_embedded_configuration_added_code_workflow_reference.xml`
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/domain/ConnectedUserProjectWorkflowConnection.java`
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/repository/ConnectedUserProjectWorkflowConnectionRepository.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/domain/ConnectedUserProjectWorkflow.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/repository/ConnectedUserProjectWorkflowRepository.java`
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/repository/ConnectedUserProjectWorkflowReferenceColumnsIntTest.java`

**Interfaces:**
- Produces: `ConnectedUserProjectWorkflow.getCatalogWorkflowUuid(): String` / `setCatalogWorkflowUuid(String)`; `.getProjectDeploymentId(): Long` / `setProjectDeploymentId(Long)`; `.isEnabled(): boolean` / `setEnabled(boolean)`; `.isDangling(): boolean` / `setDangling(boolean)`; `.getDanglingReason(): String` / `setDanglingReason(String)`. `ConnectedUserProjectWorkflowRepository.findByConnectedUserProjectIdAndCatalogWorkflowUuid(long, String): Optional<ConnectedUserProjectWorkflow>`. `ConnectedUserProjectWorkflowConnection` domain + `ConnectedUserProjectWorkflowConnectionRepository`, consumed by Task 3.

- [ ] **Step 1: Write the failing test**

```java
@SpringBootTest
@ActiveProfiles("testint")
class ConnectedUserProjectWorkflowReferenceColumnsIntTest {

    @Autowired
    private ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;

    @Autowired
    private ConnectedUserProjectWorkflowConnectionRepository connectedUserProjectWorkflowConnectionRepository;

    @Test
    void testReferenceColumnsRoundTrip() {
        ConnectedUserProjectWorkflow connectedUserProjectWorkflow = new ConnectedUserProjectWorkflow();

        connectedUserProjectWorkflow.setConnectedUserProjectId(1L);
        connectedUserProjectWorkflow.setCatalogWorkflowUuid("11111111-1111-1111-1111-111111111111");
        connectedUserProjectWorkflow.setProjectDeploymentId(42L);
        connectedUserProjectWorkflow.setEnabled(true);
        connectedUserProjectWorkflow.setDangling(false);

        ConnectedUserProjectWorkflow saved = connectedUserProjectWorkflowRepository.save(connectedUserProjectWorkflow);

        ConnectedUserProjectWorkflow reloaded = connectedUserProjectWorkflowRepository.findById(saved.getId())
            .orElseThrow();

        Assertions.assertEquals("11111111-1111-1111-1111-111111111111", reloaded.getCatalogWorkflowUuid());
        Assertions.assertEquals(42L, reloaded.getProjectDeploymentId());
        Assertions.assertTrue(reloaded.isEnabled());
        Assertions.assertFalse(reloaded.isDangling());
        Assertions.assertNull(reloaded.getProjectWorkflowId(), "a reference row carries no per-user copy");
    }

    @Test
    void testCopyModeRowsAreUnaffected() {
        ConnectedUserProjectWorkflow copyModeRow = new ConnectedUserProjectWorkflow();

        copyModeRow.setConnectedUserProjectId(1L);
        copyModeRow.setProjectWorkflowId(7L);

        ConnectedUserProjectWorkflow saved = connectedUserProjectWorkflowRepository.save(copyModeRow);

        ConnectedUserProjectWorkflow reloaded = connectedUserProjectWorkflowRepository.findById(saved.getId())
            .orElseThrow();

        Assertions.assertNull(reloaded.getCatalogWorkflowUuid());
        Assertions.assertTrue(reloaded.isEnabled(), "default enabled must not break existing copy-mode rows");
        Assertions.assertFalse(reloaded.isDangling());
    }

    @Test
    void testConnectedUserProjectWorkflowConnectionRoundTrips() {
        ConnectedUserProjectWorkflowConnection connection = new ConnectedUserProjectWorkflowConnection();

        connection.setConnectedUserProjectWorkflowId(1L);
        connection.setWorkflowNodeName("slack");
        connection.setConnectionId(9L);

        ConnectedUserProjectWorkflowConnection saved = connectedUserProjectWorkflowConnectionRepository.save(
            connection);

        Assertions.assertEquals(
            9L, connectedUserProjectWorkflowConnectionRepository.findById(saved.getId())
                .orElseThrow()
                .getConnectionId());
    }

    @Test
    void testFindByConnectedUserProjectIdAndCatalogWorkflowUuid() {
        ConnectedUserProjectWorkflow connectedUserProjectWorkflow = new ConnectedUserProjectWorkflow();

        connectedUserProjectWorkflow.setConnectedUserProjectId(2L);
        connectedUserProjectWorkflow.setCatalogWorkflowUuid("22222222-2222-2222-2222-222222222222");

        connectedUserProjectWorkflowRepository.save(connectedUserProjectWorkflow);

        Optional<ConnectedUserProjectWorkflow> found = connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndCatalogWorkflowUuid(2L, "22222222-2222-2222-2222-222222222222");

        Assertions.assertTrue(found.isPresent());
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:testIntegration --tests "*ConnectedUserProjectWorkflowReferenceColumnsIntTest*" > /tmp/plan1.log 2>&1; echo $?`
Expected: non-zero. `/tmp/plan1.log` contains `cannot find symbol: method setCatalogWorkflowUuid` (and `ConnectedUserProjectWorkflowConnection` unresolved).

- [ ] **Step 3: Add the Liquibase changelog**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <!-- Reference mode: catalog_workflow_uuid non-null means this row points at a shared catalog
         ProjectWorkflow instead of holding a per-user copy. project_workflow_id becomes optional
         because a reference row has no copy to point at. -->
    <changeSet id="20260727120000-1" author="Ivica Cardic">
        <preConditions onFail="MARK_RAN">
            <not>
                <columnExists tableName="connected_user_project_workflow" columnName="catalog_workflow_uuid"/>
            </not>
        </preConditions>

        <addColumn tableName="connected_user_project_workflow">
            <column name="catalog_workflow_uuid" type="VARCHAR(36)"/>
            <column name="project_deployment_id" type="BIGINT"/>
            <column name="enabled" type="BOOLEAN" defaultValueBoolean="true">
                <constraints nullable="false"/>
            </column>
            <column name="dangling" type="BOOLEAN" defaultValueBoolean="false">
                <constraints nullable="false"/>
            </column>
            <column name="dangling_reason" type="VARCHAR(255)"/>
        </addColumn>

        <dropNotNullConstraint
            tableName="connected_user_project_workflow" columnName="project_workflow_id" columnDataType="BIGINT"/>
    </changeSet>

    <changeSet id="20260727120000-2" author="Ivica Cardic">
        <preConditions onFail="MARK_RAN">
            <not>
                <tableExists tableName="connected_user_project_workflow_connection"/>
            </not>
        </preConditions>

        <createTable tableName="connected_user_project_workflow_connection">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="connected_user_project_workflow_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="workflow_node_name" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="connection_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="created_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="created_by" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="last_modified_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="last_modified_by" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="version" type="BIGINT">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addUniqueConstraint
            tableName="connected_user_project_workflow_connection"
            columnNames="connected_user_project_workflow_id, workflow_node_name"/>

        <addForeignKeyConstraint
            baseTableName="connected_user_project_workflow_connection"
            baseColumnNames="connected_user_project_workflow_id"
            constraintName="fk_cupwc_connected_user_project_workflow"
            referencedTableName="connected_user_project_workflow"
            referencedColumnNames="id"/>
    </changeSet>
</databaseChangeLog>
```

- [ ] **Step 4: Add the `ConnectedUserProjectWorkflow` fields**

After the `workflowVersion` field:

```java
    @Column("catalog_workflow_uuid")
    private String catalogWorkflowUuid;

    @Column("project_deployment_id")
    private Long projectDeploymentId;

    @Column
    private boolean enabled = true;

    @Column
    private boolean dangling;

    @Column("dangling_reason")
    private String danglingReason;
```

And with the other accessors:

```java
    @Nullable
    public String getCatalogWorkflowUuid() {
        return catalogWorkflowUuid;
    }

    public void setCatalogWorkflowUuid(@Nullable String catalogWorkflowUuid) {
        this.catalogWorkflowUuid = catalogWorkflowUuid;
    }

    @Nullable
    public Long getProjectDeploymentId() {
        return projectDeploymentId;
    }

    public void setProjectDeploymentId(@Nullable Long projectDeploymentId) {
        this.projectDeploymentId = projectDeploymentId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDangling() {
        return dangling;
    }

    public void setDangling(boolean dangling) {
        this.dangling = dangling;
    }

    @Nullable
    public String getDanglingReason() {
        return danglingReason;
    }

    public void setDanglingReason(@Nullable String danglingReason) {
        this.danglingReason = danglingReason;
    }
```

Also update the `@PersistenceCreator` constructor to accept the five new fields (Spring Data JDBC binds it positionally against the row when present), and make `projectWorkflowId` nullable there:

```java
    @PersistenceCreator
    public ConnectedUserProjectWorkflow(
        Long id, Long connectedUserProjectId, @Nullable Long projectWorkflowId, Integer workflowVersion,
        @Nullable String catalogWorkflowUuid, @Nullable Long projectDeploymentId, boolean enabled, boolean dangling,
        @Nullable String danglingReason, int version) {

        this.id = id;
        this.connectedUserProjectId = AggregateReference.to(connectedUserProjectId);
        this.projectWorkflowId = projectWorkflowId == null ? null : AggregateReference.to(projectWorkflowId);
        this.workflowVersion = workflowVersion;
        this.catalogWorkflowUuid = catalogWorkflowUuid;
        this.projectDeploymentId = projectDeploymentId;
        this.enabled = enabled;
        this.dangling = dangling;
        this.danglingReason = danglingReason;
        this.version = version;
    }
```

- [ ] **Step 5: Write `ConnectedUserProjectWorkflowConnection`**

```java
@Table("connected_user_project_workflow_connection")
public final class ConnectedUserProjectWorkflowConnection {

    @Id
    private Long id;

    @Column("connected_user_project_workflow_id")
    private AggregateReference<ConnectedUserProjectWorkflow, Long> connectedUserProjectWorkflowId;

    @Column("workflow_node_name")
    private String workflowNodeName;

    @Column("connection_id")
    private long connectionId;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Version
    private int version;

    public ConnectedUserProjectWorkflowConnection() {
    }

    @PersistenceCreator
    public ConnectedUserProjectWorkflowConnection(
        Long id, Long connectedUserProjectWorkflowId, String workflowNodeName, long connectionId, int version) {

        this.id = id;
        this.connectedUserProjectWorkflowId = AggregateReference.to(connectedUserProjectWorkflowId);
        this.workflowNodeName = workflowNodeName;
        this.connectionId = connectionId;
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConnectedUserProjectWorkflowConnection that = (ConnectedUserProjectWorkflowConnection) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getId() {
        return id;
    }

    public Long getConnectedUserProjectWorkflowId() {
        return connectedUserProjectWorkflowId == null ? null : connectedUserProjectWorkflowId.getId();
    }

    public void setConnectedUserProjectWorkflowId(long connectedUserProjectWorkflowId) {
        this.connectedUserProjectWorkflowId = AggregateReference.to(connectedUserProjectWorkflowId);
    }

    public String getWorkflowNodeName() {
        return workflowNodeName;
    }

    public void setWorkflowNodeName(String workflowNodeName) {
        this.workflowNodeName = workflowNodeName;
    }

    public long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(long connectionId) {
        this.connectionId = connectionId;
    }

    public int getVersion() {
        return version;
    }
}
```

- [ ] **Step 6: Write the repository**

```java
@Repository
public interface ConnectedUserProjectWorkflowConnectionRepository
    extends ListCrudRepository<ConnectedUserProjectWorkflowConnection, Long> {

    List<ConnectedUserProjectWorkflowConnection> findAllByConnectedUserProjectWorkflowId(
        long connectedUserProjectWorkflowId);
}
```

- [ ] **Step 7: Add the repository finder**

In `ConnectedUserProjectWorkflowRepository.java`:

```java
    Optional<ConnectedUserProjectWorkflow> findByConnectedUserProjectIdAndCatalogWorkflowUuid(
        long connectedUserProjectId, String catalogWorkflowUuid);
```

- [ ] **Step 8: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:testIntegration --tests "*ConnectedUserProjectWorkflowReferenceColumnsIntTest*" > /tmp/plan1.log 2>&1; echo $?`
Expected: `0`, and `grep -c "FAILED" /tmp/plan1.log` is `0`.

- [ ] **Step 9: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration
git commit -m "Add code-workflow reference columns to connected_user_project_workflow

Reference mode is a nullable catalog_workflow_uuid discriminator, not a new
relation table: a reference row has no per-user copy, so project_workflow_id
becomes optional. Per-reference connection wiring gets its own join table
because WorkflowTestConfiguration is keyed by workflowId alone, and a shared
catalog workflow has exactly one workflowId across every referencing user."
```

---

### Task 2: Deploy facade and internal REST endpoint

**Files:**
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectCodeWorkflowFacade.java`
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectCodeWorkflowFacadeImpl.java`
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-rest/embedded-configuration-rest-impl/src/main/java/com/bytechef/ee/embedded/configuration/web/rest/AutomationProjectCodeWorkflowApiController.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectFacade.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectFacadeImpl.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-rest/embedded-configuration-rest-impl/openapi.yaml`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/build.gradle.kts`
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectCodeWorkflowFacadeTest.java`

**Interfaces:**
- Consumes: nothing from Task 1.
- Produces: `AutomationWorkflowProjectFacade.fetchProjectIdByName(String): Optional<Long>`; `AutomationWorkflowProjectCodeWorkflowFacade.save(byte[], Language)`. Task 3/4 read the resulting marked catalog `Project`/`ProjectWorkflow` rows through the existing `AutomationWorkflowProjectFacade`/`ProjectWorkflowService` reads — no new read surface is needed for them.

- [ ] **Step 1: Write the failing test**

```java
@ExtendWith(MockitoExtension.class)
class AutomationWorkflowProjectCodeWorkflowFacadeTest {

    @Mock
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @Mock
    private CodeWorkflowContainerFacade codeWorkflowContainerFacade;

    @Mock
    private ProjectCodeWorkflowService projectCodeWorkflowService;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    private AutomationWorkflowProjectCodeWorkflowFacadeImpl facade;

    @BeforeEach
    void setUp() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        facade = new AutomationWorkflowProjectCodeWorkflowFacadeImpl(
            applicationProperties, mock(CacheManager.class), automationWorkflowProjectFacade,
            codeWorkflowContainerFacade, projectCodeWorkflowService, projectService, projectWorkflowService);
    }

    @Test
    void testFirstDeployCreatesTheCatalogProject() {
        Mockito.when(automationWorkflowProjectFacade.fetchProjectIdByName("acme-billing"))
            .thenReturn(Optional.empty());
        Mockito.when(automationWorkflowProjectFacade.createProject(
            Mockito.eq("acme-billing"), Mockito.any(), Mockito.isNull(), Mockito.eq(List.of()), Mockito.isNull()))
            .thenReturn(100L);

        Project project = new Project(100L);

        Mockito.when(projectService.getProject(100L))
            .thenReturn(project);

        CodeWorkflowContainer container = codeWorkflowContainer(Map.of("charge", "wf-1"));

        Mockito.when(codeWorkflowContainerFacade.create(
            Mockito.eq("acme-billing"), Mockito.any(), Mockito.any(), Mockito.eq(Language.JAVASCRIPT),
            Mockito.any(), Mockito.eq(PlatformType.AUTOMATION)))
            .thenReturn(container);

        facade.save(fakeProjectDefinitionBytes("acme-billing"), Language.JAVASCRIPT);

        Mockito.verify(projectWorkflowService)
            .addWorkflow(100L, project.getLastProjectVersion(), "wf-1");
        Mockito.verify(projectService)
            .publishProject(100L, null, false);
        Mockito.verify(automationWorkflowProjectFacade, Mockito.never())
            .createProject(Mockito.eq("acme-billing"), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }
}
```

(`fakeProjectDefinitionBytes`/`codeWorkflowContainer` are small test-local helpers building a minimal JS `ProjectDefinition` source string and a stub `CodeWorkflowContainer`; exact bodies are the implementer's call — the assertions above are what matters.)

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "*AutomationWorkflowProjectCodeWorkflowFacadeTest*" > /tmp/plan2.log 2>&1; echo $?`
Expected: non-zero, `cannot find symbol: class AutomationWorkflowProjectCodeWorkflowFacadeImpl`.

- [ ] **Step 3: Add `fetchProjectIdByName` to `AutomationWorkflowProjectFacade`**

Interface:

```java
    Optional<Long> fetchProjectIdByName(String name);
```

Impl (uses the same `MARKER` the class already defines):

```java
    @Override
    public Optional<Long> fetchProjectIdByName(String name) {
        return projectService.fetchProject(MARKER + name)
            .map(Project::getId);
    }
```

- [ ] **Step 4: Add the module dependencies**

In `embedded-configuration-service/build.gradle.kts`:

```kotlin
    implementation(project(":server:ee:libs:automation:automation-code-workflow-loader"))
    implementation(project(":server:ee:libs:automation:automation-configuration:automation-configuration-api"))
```

and, for the integration test in Task 7:

```kotlin
    testImplementation(project(":server:ee:libs:automation:automation-configuration:automation-configuration-service"))
```

- [ ] **Step 5: Write the facade interface**

```java
public interface AutomationWorkflowProjectCodeWorkflowFacade {

    void save(byte[] bytes, Language language);
}
```

- [ ] **Step 6: Write the facade impl**

Structured identically to `ProjectCodeWorkflowFacadeImpl` (same `javaEnabled`/`javaLoader` gate, same `ProjectHandlerLoader` call), with exactly one structural change: the project is resolved/created through `AutomationWorkflowProjectFacade`'s marker convention instead of a bare `projectService.fetchProject(name)`, and publish goes straight through `projectService.publishProject(...)` — mirroring how `ProjectCodeWorkflowFacadeImpl` itself publishes, not through `AutomationWorkflowProjectFacade.publishProject` (which additionally duplicates workflow rows for the visual-editor versioning story that code workflows don't need — the exact behavior code-workflow redeploy already has today for plain automation projects).

```java
@Service
@Transactional
@ConditionalOnEEVersion
public class AutomationWorkflowProjectCodeWorkflowFacadeImpl implements AutomationWorkflowProjectCodeWorkflowFacade {

    private final CacheManager cacheManager;
    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade;
    private final CodeWorkflowContainerFacade codeWorkflowContainerFacade;
    private final ProjectCodeWorkflowService projectCodeWorkflowService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final boolean javaEnabled;
    private final ProjectHandlerLoader.JavaLoader javaLoader;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectCodeWorkflowFacadeImpl(
        ApplicationProperties applicationProperties, CacheManager cacheManager,
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade,
        CodeWorkflowContainerFacade codeWorkflowContainerFacade,
        ProjectCodeWorkflowService projectCodeWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService) {

        this.cacheManager = cacheManager;
        this.automationWorkflowProjectFacade = automationWorkflowProjectFacade;
        this.codeWorkflowContainerFacade = codeWorkflowContainerFacade;
        this.projectCodeWorkflowService = projectCodeWorkflowService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.javaEnabled = applicationProperties.getWorkflow()
            .getCodeWorkflow()
            .isJavaEnabled();
        this.javaLoader = applicationProperties.getWorkflow()
            .getCodeWorkflow()
            .getJavaLoader() == CodeWorkflow.JavaLoader.ESPRESSO
                ? ProjectHandlerLoader.JavaLoader.ESPRESSO
                : ProjectHandlerLoader.JavaLoader.CLASS_LOADER;
    }

    /**
     * Deploying a code workflow loads and executes the uploaded artifact on the server, so it is restricted to
     * administrators. The guard lives here, mirroring {@code IntegrationCodeWorkflowFacadeImpl#save} and
     * {@code ProjectCodeWorkflowFacadeImpl#save}, so it protects every caller, not only the REST entry point.
     */
    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void save(byte[] bytes, Language language) {
        if (!javaEnabled && language == Language.JAVA) {
            throw new ConfigurationException(
                "Uploading of Java code workflows is disabled",
                CodeWorkflowErrorType.JAVA_CODE_WORKFLOW_UPLOAD_DISABLED);
        }

        ProjectDefinition projectDefinition = loadProjectDefinition(language, bytes);

        long projectId = automationWorkflowProjectFacade.fetchProjectIdByName(projectDefinition.getName())
            .orElseGet(() -> automationWorkflowProjectFacade.createProject(
                projectDefinition.getName(),
                projectDefinition.getDescription()
                    .orElse(null),
                null, List.of(), null));

        Project project = projectService.getProject(projectId);

        CodeWorkflowContainer codeWorkflowContainer = codeWorkflowContainerFacade.create(
            projectDefinition.getName(), projectDefinition.getVersion(), projectDefinition.getWorkflows(), language,
            bytes, PlatformType.AUTOMATION);

        projectCodeWorkflowService.create(codeWorkflowContainer, project);

        for (Map.Entry<String, String> entry : codeWorkflowContainer.getWorkflowNameIds()
            .entrySet()) {

            projectWorkflowService.addWorkflow(project.getId(), project.getLastProjectVersion(), entry.getValue());
        }

        projectService.publishProject(project.getId(), null, false);
    }

    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    private ProjectDefinition loadProjectDefinition(Language language, byte[] bytes) {
        try {
            Path path = Files.createTempFile("embedded_automation_code_workflow", language.getExtension());

            Files.write(path, bytes);

            URI uri = path.toUri();

            try {
                ProjectHandler projectHandler = ProjectHandlerLoader.loadProjectHandler(
                    uri.toURL(), language, javaLoader, uri + UUID.randomUUID()
                        .toString(),
                    cacheManager);

                return projectHandler.getDefinition();
            } finally {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
```

- [ ] **Step 7: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "*AutomationWorkflowProjectCodeWorkflowFacadeTest*" > /tmp/plan2.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 8: Add the internal deploy endpoint**

Add to `embedded-configuration-rest-impl/openapi.yaml`, beside `/integrations/deploy`:

```yaml
  /automation/projects/deploy:
    post:
      description: "Deploy a new automation code workflow into the embedded catalog."
      summary: "Deploy a new automation code workflow into the embedded catalog"
      tags:
      - "automation-workflow-project-code-workflow"
      operationId: "deployAutomationProjectCodeWorkflow"
      requestBody:
        content:
          multipart/form-data:
            schema:
              type: "object"
              properties:
                projectFile:
                  description: "The file of a code-native automation project."
                  type: "string"
                  format: "binary"
        required: true
      responses:
        "204":
          description: "Successful operation."
```

Regenerate the interface: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-rest:embedded-configuration-rest-impl:openApiGenerate > /tmp/plan2b.log 2>&1; echo $?` (expected `0`; if the module uses a different generator task name, run `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-rest:embedded-configuration-rest-impl:tasks --all | grep -i openapi > /tmp/plan2c.log 2>&1; echo $?` first to find it).

- [ ] **Step 9: Write the controller**

```java
@RestController("com.bytechef.ee.embedded.configuration.web.rest.AutomationProjectCodeWorkflowApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/internal")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AutomationProjectCodeWorkflowApiController implements AutomationProjectCodeWorkflowApi {

    private final AutomationWorkflowProjectCodeWorkflowFacade automationWorkflowProjectCodeWorkflowFacade;

    @SuppressFBWarnings("EI")
    public AutomationProjectCodeWorkflowApiController(
        AutomationWorkflowProjectCodeWorkflowFacade automationWorkflowProjectCodeWorkflowFacade) {

        this.automationWorkflowProjectCodeWorkflowFacade = automationWorkflowProjectCodeWorkflowFacade;
    }

    /**
     * Authorization note: the {@code ROLE_ADMIN} guard lives on
     * {@code AutomationWorkflowProjectCodeWorkflowFacadeImpl#save}, exactly mirroring
     * {@code IntegrationCodeWorkflowApiController}, so a connected-user bearer token (which carries no authorities,
     * see {@code EmbeddedApiKeyAuthenticationProvider}) is denied at the facade regardless of what reaches this
     * controller.
     */
    @Override
    public ResponseEntity<Void> deployAutomationProjectCodeWorkflow(MultipartFile projectFile) {
        try {
            automationWorkflowProjectCodeWorkflowFacade.save(
                projectFile.getBytes(),
                Language.of(Objects.requireNonNull(projectFile.getOriginalFilename())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.noContent()
            .build();
    }
}
```

- [ ] **Step 10: Run the module's existing tests to confirm nothing broke**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-rest:embedded-configuration-rest-impl:test > /tmp/plan2d.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 11: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration
git commit -m "Add the embedded-to-automation code workflow deploy endpoint

Deploying through /api/embedded/internal/automation/projects/deploy resolves or
creates the catalog project through AutomationWorkflowProjectFacade's
__EMBEDDED_AUTOMATION__ marker convention, then reuses the exact
CodeWorkflowContainerFacade/PlatformType.AUTOMATION mechanics automation's own
code-workflow deploy already uses. Deploying the same bytes through the plain
automation endpoint still creates an unmarked, unrelated project -- the two
doors stay distinct, as decided in the design spec."
```

---

### Task 3: Reference lifecycle (provision, connection wiring, enable/disable, delete, dangling)

**Files:**
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/exception/MissingConnectionException.java`
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserCodeWorkflowReferenceFacade.java`
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserCodeWorkflowReferenceFacadeImpl.java`
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserWorkflowConnectionResolver.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/ProjectDeploymentService.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/ProjectDeploymentServiceImpl.java`
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserCodeWorkflowReferenceFacadeTest.java`

**Interfaces:**
- Consumes: `ConnectedUserProjectWorkflow` reference columns (Task 1), `AutomationWorkflowProjectFacade`/`ProjectWorkflowService` catalog reads (Task 2 output).
- Produces: `ConnectedUserCodeWorkflowReferenceFacade.getOrCreateReference(String externalUserId, String catalogWorkflowUuid, Environment environment): ConnectedUserProjectWorkflow`; `.enableReference(String, String, boolean, Environment)`; `.deleteReference(String, String, Environment)`; `.markDanglingReferences(long catalogProjectId, Set<String> currentCatalogWorkflowUuids)`. Task 4/5 call `getOrCreateReference` for implicit provisioning and read `.getProjectDeploymentId()` to build a `WorkflowExecutionId`.

- [ ] **Step 1: Write the failing test — the connection-isolation case first**

This is the test that keeps two connected users' connections from leaking into each other, which is the entire reason `WorkflowTestConfiguration` couldn't be reused for reference mode.

```java
@ExtendWith(MockitoExtension.class)
class ConnectedUserCodeWorkflowReferenceFacadeTest {

    @Mock
    private ComponentDefinitionService componentDefinitionService;

    @Mock
    private ConnectedUserProjectWorkflowConnectionRepository connectedUserProjectWorkflowConnectionRepository;

    @Mock
    private ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;

    @Mock
    private ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private ConnectedUserCodeWorkflowReferenceFacadeImpl facade;

    @Test
    void testTwoUsersReferencingTheSameCatalogWorkflowGetIndependentConnectionRows() {
        ProjectWorkflow catalogProjectWorkflow = new ProjectWorkflow(500L, 1, "catalog-wf-1");

        Mockito.when(projectWorkflowService.getLastPublishedWorkflowId("catalog-uuid"))
            .thenReturn("catalog-wf-1");
        Mockito.when(projectWorkflowService.getWorkflowProjectWorkflow("catalog-wf-1"))
            .thenReturn(catalogProjectWorkflow);

        Workflow workflow = new Workflow();
        workflow.setDefinition("{\"triggers\":[],\"tasks\":[{\"name\":\"t1\",\"type\":\"slack/v1/postMessage\"}]}");

        Mockito.when(workflowService.getWorkflow("catalog-wf-1"))
            .thenReturn(workflow);

        ConnectedUserProject userAProject = new ConnectedUserProject();
        userAProject.setId(10L);

        ConnectedUserProject userBProject = new ConnectedUserProject();
        userBProject.setId(20L);

        Mockito.when(connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            Mockito.eq("userA"), Mockito.any()))
            .thenReturn(userAProject);
        Mockito.when(connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            Mockito.eq("userB"), Mockito.any()))
            .thenReturn(userBProject);

        Mockito.when(connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndCatalogWorkflowUuid(Mockito.anyLong(), Mockito.eq("catalog-uuid")))
            .thenReturn(Optional.empty());
        Mockito.when(connectedUserProjectWorkflowRepository.save(Mockito.any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Connection userAConnection = connection(1L, "slack");
        Connection userBConnection = connection(2L, "slack");

        Mockito.when(connectionService.getConnections(PlatformType.EMBEDDED))
            .thenReturn(List.of(userAConnection))
            .thenReturn(List.of(userBConnection));

        facade.getOrCreateReference("userA", "catalog-uuid", Environment.PRODUCTION);
        facade.getOrCreateReference("userB", "catalog-uuid", Environment.PRODUCTION);

        ArgumentCaptor<ConnectedUserProjectWorkflowConnection> captor =
            ArgumentCaptor.forClass(ConnectedUserProjectWorkflowConnection.class);

        Mockito.verify(connectedUserProjectWorkflowConnectionRepository, Mockito.times(2))
            .save(captor.capture());

        List<Long> wiredConnectionIds = captor.getAllValues()
            .stream()
            .map(ConnectedUserProjectWorkflowConnection::getConnectionId)
            .toList();

        Assertions.assertEquals(List.of(1L, 2L), wiredConnectionIds);
    }

    private static Connection connection(long id, String componentName) {
        Connection connection = new Connection();

        connection.setId(id);
        connection.setComponentName(componentName);

        return connection;
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "*ConnectedUserCodeWorkflowReferenceFacadeTest*" > /tmp/plan3.log 2>&1; echo $?`
Expected: non-zero, `cannot find symbol: class ConnectedUserCodeWorkflowReferenceFacadeImpl`.

- [ ] **Step 3: Add `fetchProjectDeploymentByName` to `ProjectDeploymentService`**

`fetchProjectDeployment(long projectId, Environment environment)` assumes one deployment per project+environment — true for copy-mode's per-user private projects, false for a catalog project shared by many referencing users. Add a name-scoped sibling instead of changing that method's contract:

```java
    Optional<ProjectDeployment> fetchProjectDeploymentByName(long projectId, String name);
```

Impl (mirrors `ProjectServiceImpl.fetchProject` shape):

```java
    @Override
    @Transactional(readOnly = true)
    public Optional<ProjectDeployment> fetchProjectDeploymentByName(long projectId, String name) {
        return projectDeploymentRepository.findByProjectIdAndName(projectId, name);
    }
```

(add the matching `findByProjectIdAndName(long, String): Optional<ProjectDeployment>` derived method to `ProjectDeploymentRepository` if it isn't already there).

- [ ] **Step 4: Write `MissingConnectionException`**

```java
/**
 * Thrown when a reference cannot be auto-wired because a component it uses has no matching connection for the
 * connected user. Deliberately NOT an {@code AbstractException} subtype: those all map to HTTP 400 through
 * {@code GlobalResponseEntityExceptionHandler}, and this condition is a 409 (the reference is left in place,
 * disabled, so the caller can create the connection and retry without redoing provisioning).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class MissingConnectionException extends RuntimeException {

    private final String componentName;

    public MissingConnectionException(String componentName) {
        super("No connection found for component: " + componentName);

        this.componentName = componentName;
    }

    public String getComponentName() {
        return componentName;
    }
}
```

- [ ] **Step 5: Write the connection resolver**

A small, deliberately separate class rather than a refactor of `ConnectedUserProjectWorkflowManager`'s private `checkWorkflowNodeConnection(s)` methods — those write into `WorkflowTestConfiguration`, which is wrong for a shared catalog workflow (Global Constraints #2). Duplicating the ~15-line node-scanning loop here is safer than risking the existing, proven copy-mode path.

```java
@Component
@ConditionalOnEEVersion
public class ConnectedUserWorkflowConnectionResolver {

    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectionService connectionService;

    public ConnectedUserWorkflowConnectionResolver(
        ComponentDefinitionService componentDefinitionService, ConnectionService connectionService) {

        this.componentDefinitionService = componentDefinitionService;
        this.connectionService = connectionService;
    }

    /**
     * @throws MissingConnectionException if a node's component declares a connection definition and the connected
     *                                     user has no matching connection to auto-wire.
     */
    public Map<String, Long> resolve(String definition) {
        Map<String, ?> workflowMap = JsonUtils.readMap(definition);
        List<Connection> connections = connectionService.getConnections(PlatformType.EMBEDDED);

        Map<String, Long> resolved = new LinkedHashMap<>();

        for (Map<String, ?> nodeMap : allNodes(workflowMap)) {
            String nodeName = MapUtils.getString(nodeMap, "name");
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(MapUtils.getString(nodeMap, "type"));

            ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
                workflowNodeType.name(), workflowNodeType.version());

            if (componentDefinition.getConnection()
                .isEmpty()) {

                continue;
            }

            Connection connection = connections.stream()
                .filter(candidate -> Objects.equals(candidate.getComponentName(), workflowNodeType.name()))
                .findFirst()
                .orElseThrow(() -> new MissingConnectionException(workflowNodeType.name()));

            resolved.put(nodeName, connection.getId());
        }

        return resolved;
    }

    private static List<Map<String, ?>> allNodes(Map<String, ?> workflowMap) {
        List<Map<String, ?>> nodes = new ArrayList<>();

        nodes.addAll(MapUtils.getList(workflowMap, "triggers", new TypeReference<>() {}, List.of()));
        nodes.addAll(MapUtils.getList(workflowMap, "tasks", new TypeReference<>() {}, List.of()));

        return nodes;
    }
}
```

(the `componentDefinitionService.getComponentDefinition(name, version)` / `componentDefinition.getConnection(): Optional<...>` calls are the one spot in this plan not verified against source line-by-line — confirm the exact method names against `ComponentDefinitionService`/`ComponentDefinition` when implementing; the shape used above follows the standard `ComponentDsl` connection-declaration convention documented in `CLAUDE.md`.)

- [ ] **Step 6: Write the reference facade interface**

```java
public interface ConnectedUserCodeWorkflowReferenceFacade {

    void deleteReference(String externalUserId, String catalogWorkflowUuid, Environment environment);

    void enableReference(String externalUserId, String catalogWorkflowUuid, boolean enable, Environment environment);

    ConnectedUserProjectWorkflow getOrCreateReference(
        String externalUserId, String catalogWorkflowUuid, Environment environment);

    void markDanglingReferences(long catalogProjectId, Set<String> currentCatalogWorkflowUuids);
}
```

- [ ] **Step 7: Write the impl**

```java
@Service
@Transactional
@ConditionalOnEEVersion
@SkipAutomationAuthorization
public class ConnectedUserCodeWorkflowReferenceFacadeImpl implements ConnectedUserCodeWorkflowReferenceFacade {

    private static final String MARKER = "__EMBEDDED__";

    private final ConnectedUserProjectWorkflowConnectionRepository connectedUserProjectWorkflowConnectionRepository;
    private final ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;
    private final ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager;
    private final ConnectedUserWorkflowConnectionResolver connectedUserWorkflowConnectionResolver;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    public ConnectedUserCodeWorkflowReferenceFacadeImpl(
        ConnectedUserProjectWorkflowConnectionRepository connectedUserProjectWorkflowConnectionRepository,
        ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository,
        ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager,
        ConnectedUserWorkflowConnectionResolver connectedUserWorkflowConnectionResolver,
        ProjectDeploymentFacade projectDeploymentFacade, ProjectDeploymentService projectDeploymentService,
        ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {

        this.connectedUserProjectWorkflowConnectionRepository = connectedUserProjectWorkflowConnectionRepository;
        this.connectedUserProjectWorkflowRepository = connectedUserProjectWorkflowRepository;
        this.connectedUserProjectWorkflowManager = connectedUserProjectWorkflowManager;
        this.connectedUserWorkflowConnectionResolver = connectedUserWorkflowConnectionResolver;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectDeploymentService = projectDeploymentService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
    }

    @Override
    public ConnectedUserProjectWorkflow getOrCreateReference(
        String externalUserId, String catalogWorkflowUuid, Environment environment) {

        ConnectedUserProject connectedUserProject = connectedUserProjectWorkflowManager
            .getOrCreateConnectedUserProject(externalUserId, environment);

        Optional<ConnectedUserProjectWorkflow> existing = connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndCatalogWorkflowUuid(connectedUserProject.getId(), catalogWorkflowUuid);

        if (existing.isPresent()) {
            return existing.get();
        }

        String catalogWorkflowId = projectWorkflowService.getLastPublishedWorkflowId(catalogWorkflowUuid);
        ProjectWorkflow catalogProjectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(
            catalogWorkflowId);

        long catalogProjectId = catalogProjectWorkflow.getProjectId();

        Workflow catalogWorkflow = workflowService.getWorkflow(catalogWorkflowId);

        Map<String, Long> resolvedConnections = connectedUserWorkflowConnectionResolver.resolve(
            catalogWorkflow.getDefinition());

        long projectDeploymentId = getOrCreateProjectDeployment(
            catalogProjectId, externalUserId, environment, catalogWorkflowId, resolvedConnections);

        ConnectedUserProjectWorkflow connectedUserProjectWorkflow = new ConnectedUserProjectWorkflow();

        connectedUserProjectWorkflow.setConnectedUserProjectId(connectedUserProject.getId());
        connectedUserProjectWorkflow.setCatalogWorkflowUuid(catalogWorkflowUuid);
        connectedUserProjectWorkflow.setProjectDeploymentId(projectDeploymentId);
        connectedUserProjectWorkflow.setEnabled(true);

        ConnectedUserProjectWorkflow saved = connectedUserProjectWorkflowRepository.save(
            connectedUserProjectWorkflow);

        for (Map.Entry<String, Long> entry : resolvedConnections.entrySet()) {
            ConnectedUserProjectWorkflowConnection connection = new ConnectedUserProjectWorkflowConnection();

            connection.setConnectedUserProjectWorkflowId(saved.getId());
            connection.setWorkflowNodeName(entry.getKey());
            connection.setConnectionId(entry.getValue());

            connectedUserProjectWorkflowConnectionRepository.save(connection);
        }

        return saved;
    }

    private long getOrCreateProjectDeployment(
        long catalogProjectId, String externalUserId, Environment environment, String catalogWorkflowId,
        Map<String, Long> resolvedConnections) {

        String name = MARKER + externalUserId;

        List<ProjectDeploymentWorkflowConnection> connections = resolvedConnections.entrySet()
            .stream()
            .map(entry -> new ProjectDeploymentWorkflowConnection(entry.getValue(), entry.getKey(), entry.getKey()))
            .toList();

        return projectDeploymentService.fetchProjectDeploymentByName(catalogProjectId, name)
            .map(ProjectDeployment::getId)
            .orElseGet(() -> {
                ProjectDeployment projectDeployment = new ProjectDeployment();

                projectDeployment.setEnabled(true);
                projectDeployment.setEnvironment(environment);
                projectDeployment.setName(name);
                projectDeployment.setProjectId(catalogProjectId);
                projectDeployment.setProjectVersion(1);

                return projectDeploymentFacade.createProjectDeployment(
                    projectDeployment, catalogWorkflowId, connections);
            });
    }

    @Override
    public void enableReference(
        String externalUserId, String catalogWorkflowUuid, boolean enable, Environment environment) {

        ConnectedUserProjectWorkflow reference = requireReference(externalUserId, catalogWorkflowUuid, environment);

        reference.setEnabled(enable);

        connectedUserProjectWorkflowRepository.save(reference);

        String catalogWorkflowId = projectWorkflowService.getLastPublishedWorkflowId(catalogWorkflowUuid);

        projectDeploymentFacade.enableProjectDeploymentWorkflow(
            reference.getProjectDeploymentId(), catalogWorkflowId, enable);
    }

    @Override
    public void deleteReference(String externalUserId, String catalogWorkflowUuid, Environment environment) {
        ConnectedUserProjectWorkflow reference = requireReference(externalUserId, catalogWorkflowUuid, environment);

        for (ConnectedUserProjectWorkflowConnection connection : connectedUserProjectWorkflowConnectionRepository
            .findAllByConnectedUserProjectWorkflowId(reference.getId())) {

            connectedUserProjectWorkflowConnectionRepository.deleteById(connection.getId());
        }

        connectedUserProjectWorkflowRepository.deleteById(reference.getId());
    }

    @Override
    public void markDanglingReferences(long catalogProjectId, Set<String> currentCatalogWorkflowUuids) {
        for (ConnectedUserProjectWorkflow reference : connectedUserProjectWorkflowRepository.findAll()) {

            String catalogWorkflowUuid = reference.getCatalogWorkflowUuid();

            if (catalogWorkflowUuid == null || currentCatalogWorkflowUuids.contains(catalogWorkflowUuid)) {
                continue;
            }

            reference.setDangling(true);
            reference.setDanglingReason("Removed from the catalog project on redeploy");

            connectedUserProjectWorkflowRepository.save(reference);
        }
    }

    private ConnectedUserProjectWorkflow requireReference(
        String externalUserId, String catalogWorkflowUuid, Environment environment) {

        ConnectedUserProject connectedUserProject = connectedUserProjectWorkflowManager
            .getOrCreateConnectedUserProject(externalUserId, environment);

        return connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndCatalogWorkflowUuid(connectedUserProject.getId(), catalogWorkflowUuid)
            .orElseThrow(() -> new ConfigurationException(
                "No reference to catalog workflow: %s".formatted(catalogWorkflowUuid),
                WorkflowErrorType.WORKFLOW_NOT_FOUND));
    }
}
```

`markDanglingReferences` iterating `findAll()` is a deliberate first cut — fine at catalog scale (one row per connected-user reference, not per execution), and matches the plan's "no dedup on failure storms"-style pragmatism elsewhere in this codebase. A future pass can add a scoped finder if this becomes a hot path.

- [ ] **Step 8: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "*ConnectedUserCodeWorkflowReferenceFacadeTest*" > /tmp/plan3.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 9: Wire `markDanglingReferences` into redeploy**

In `AutomationWorkflowProjectCodeWorkflowFacadeImpl.save` (Task 2), after `projectService.publishProject(...)`, add:

```java
        Set<String> currentUuids = projectWorkflowService.getProjectWorkflows(project.getId(), project.getLastProjectVersion())
            .stream()
            .map(ProjectWorkflow::getUuidAsString)
            .collect(Collectors.toSet());

        connectedUserCodeWorkflowReferenceFacade.markDanglingReferences(project.getId(), currentUuids);
```

(add `ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade` to the constructor.) Add a regression test to `AutomationWorkflowProjectCodeWorkflowFacadeTest` asserting `markDanglingReferences` is invoked with the post-publish project id and uuid set.

- [ ] **Step 10: Run the full facade test file again**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "*AutomationWorkflowProjectCodeWorkflowFacadeTest*" --tests "*ConnectedUserCodeWorkflowReferenceFacadeTest*" > /tmp/plan3b.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 11: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration server/libs/automation/automation-configuration
git commit -m "Add code-workflow reference provisioning, connection wiring, and dangling

Each reference gets its own per-(catalog project, connected user)
ProjectDeployment, looked up by name via a new fetchProjectDeploymentByName --
ProjectDeploymentService's existing fetchProjectDeployment(projectId,
environment) assumes one deployment per project+environment, which only holds
because copy-mode gives each user their own private project. Per-user
connection wiring lives in a new join table rather than WorkflowTestConfiguration
so two users referencing the same catalog workflow never see each other's
connections. Redeploy flips references to a shared-workflow that disappeared
into a disabled dangling state instead of deleting them."
```

---

### Task 4: Sync invocation bridge — `POST /workflows/{workflowUuid}`

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/service/IntegrationWorkflowService.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/service/IntegrationWorkflowServiceImpl.java`
- Modify: `server/ee/libs/embedded/embedded-webhook/embedded-webhook-public-rest/src/main/java/com/bytechef/ee/embedded/webhook/public_/web/rest/RequestTriggerApiController.java`
- Test: `server/ee/libs/embedded/embedded-webhook/embedded-webhook-public-rest/src/test/java/com/bytechef/ee/embedded/webhook/public_/web/rest/RequestTriggerApiControllerAutomationBridgeTest.java`

**Interfaces:**
- Consumes: `ConnectedUserCodeWorkflowReferenceFacade.getOrCreateReference` (Task 3), `AutomationWorkflowProjectFacade.getPublishedProjects()` (existing, Task 2 output flows through it).
- Produces: nothing new — this task only extends an existing public endpoint's behavior.

- [ ] **Step 1: Write the regression-pin test FIRST**

The constraint that matters most here is that integration resolution never changes. Pin it before writing the new branch.

```java
@ExtendWith(MockitoExtension.class)
class RequestTriggerApiControllerAutomationBridgeTest {

    @Mock
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @Mock
    private ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade;

    @Mock
    private ConnectedUserService connectedUserService;

    @Mock
    private IntegrationInstanceService integrationInstanceService;

    @Mock
    private IntegrationWorkflowService integrationWorkflowService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private WebhookWorkflowExecutor webhookWorkflowExecutor;

    @Mock
    private WorkflowService workflowService;

    @Test
    void testIntegrationWorkflowResolutionIsUnchangedWhenOneExists() {
        RequestTriggerApiController controller = controller();

        Mockito.when(integrationWorkflowService.fetchLastWorkflowId(Mockito.eq("uuid-1"), Mockito.any()))
            .thenReturn(Optional.of("integration-wf-1"));

        // Existing behavior must run through unmodified: connectedUserCodeWorkflowReferenceFacade must never be
        // consulted once an integration workflow is found.
        ConnectedUser connectedUser = new ConnectedUser();
        connectedUser.setId(1L);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.anyString(), Mockito.any()))
            .thenReturn(connectedUser);
        Mockito.when(integrationInstanceService.getIntegrationInstance(1L, "integration-wf-1", Environment.PRODUCTION))
            .thenReturn(new IntegrationInstance());
        Mockito.when(workflowService.getWorkflow("integration-wf-1"))
            .thenReturn(new Workflow());

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("user-1"));

            controller.executeWorkflow("uuid-1", null);
        }

        Mockito.verifyNoInteractions(connectedUserCodeWorkflowReferenceFacade);
    }

    @Test
    void testUnknownWorkflowFallsThroughToTheAutomationBridge() {
        RequestTriggerApiController controller = controller();

        Mockito.when(integrationWorkflowService.fetchLastWorkflowId(Mockito.eq("uuid-2"), Mockito.any()))
            .thenReturn(Optional.empty());
        Mockito.when(automationWorkflowProjectFacade.getPublishedProjects())
            .thenReturn(List.of());

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("user-1"));

            ResponseEntity<Object> responseEntity = controller.executeWorkflow("uuid-2", null);

            Assertions.assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        }
    }

    private RequestTriggerApiController controller() {
        // constructor arg list per Step 3 below
        return new RequestTriggerApiController(
            new ApplicationProperties(), automationWorkflowProjectFacade, connectedUserCodeWorkflowReferenceFacade,
            connectedUserService, mock(EnvironmentService.class) /* delegates to real default methods via spy if needed */,
            mock(com.bytechef.file.storage.token.FileEntryTokens.class), mock(HttpServletRequest.class),
            mock(HttpServletResponse.class), mock(com.bytechef.platform.file.storage.TempFileStorage.class),
            webhookWorkflowExecutor, integrationInstanceService, integrationWorkflowService, projectWorkflowService,
            workflowService);
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:ee:libs:embedded:embedded-webhook:embedded-webhook-public-rest:test --tests "*RequestTriggerApiControllerAutomationBridgeTest*" > /tmp/plan4.log 2>&1; echo $?`
Expected: non-zero — `fetchLastWorkflowId` does not exist yet, and the constructor shape does not match.

- [ ] **Step 3: Add `fetchLastWorkflowId` to `IntegrationWorkflowService`**

The existing `getLastWorkflowId(String, Environment)` throws on an unknown uuid and its callers rely on that — it stays byte-for-byte unchanged. Add a non-throwing sibling used only for branch detection:

```java
    Optional<String> fetchLastWorkflowId(String workflowUuid, Environment environment);
```

Impl:

```java
    @Override
    @Transactional(readOnly = true)
    public Optional<String> fetchLastWorkflowId(String workflowUuid, Environment environment) {
        try {
            return Optional.of(getLastWorkflowId(workflowUuid, environment));
        } catch (IllegalArgumentException illegalArgumentException) {
            return Optional.empty();
        }
    }
```

- [ ] **Step 4: Extend `RequestTriggerApiController`**

Extract the existing body verbatim into a private method, add the new branch, add the two new constructor dependencies:

```java
    @CrossOrigin
    @Override
    public ResponseEntity<Object> executeWorkflow(String workflowUuid, EnvironmentModel xEnvironment) {
        Environment environment = environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(
            OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), environment);

        Optional<String> integrationWorkflowId = integrationWorkflowService.fetchLastWorkflowId(
            workflowUuid, environment);

        if (integrationWorkflowId.isPresent()) {
            return executeIntegrationWorkflow(connectedUser, workflowUuid, integrationWorkflowId.get(), environment);
        }

        return executeAutomationBridgeWorkflow(connectedUser, workflowUuid, environment);
    }

    // Unchanged body of the old executeWorkflow, moved here verbatim.
    private ResponseEntity<Object> executeIntegrationWorkflow(
        ConnectedUser connectedUser, String workflowUuid, String workflowId, Environment environment) {

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            connectedUser.getId(), workflowId, environment);

        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.EMBEDDED, integrationInstance.getId(), workflowUuid, findRequestTriggerName(workflow));

        return dispatch(workflowExecutionId);
    }

    /**
     * The automation-bridge branch: workflowUuid is not an integration workflow, so try it as a published catalog
     * ProjectWorkflow uuid. No published catalog workflow with this uuid, and no enabled reference to it, both
     * resolve to the SAME 404 an unknown workflowUuid always returned -- an existence leak would tell a caller
     * something about the catalog they otherwise couldn't see.
     */
    private ResponseEntity<Object> executeAutomationBridgeWorkflow(
        ConnectedUser connectedUser, String workflowUuid, Environment environment) {

        boolean isPublishedCatalogWorkflow = automationWorkflowProjectFacade.getPublishedProjects()
            .stream()
            .flatMap(project -> CollectionUtils.stream(project.workflowTemplates()))
            .anyMatch(workflowTemplate -> Objects.equals(workflowTemplate.id(), workflowUuid));

        if (!isPublishedCatalogWorkflow) {
            return ResponseEntity.notFound()
                .build();
        }

        ConnectedUserProjectWorkflow reference;

        try {
            reference = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
                connectedUser.getExternalId(), workflowUuid, environment);
        } catch (MissingConnectionException missingConnectionException) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("missingConnectionComponentName", missingConnectionException.getComponentName()));
        }

        if (!reference.isEnabled() || reference.isDangling()) {
            return ResponseEntity.notFound()
                .build();
        }

        String catalogWorkflowId = projectWorkflowService.getLastPublishedWorkflowId(workflowUuid);
        Workflow workflow = workflowService.getWorkflow(catalogWorkflowId);

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, reference.getProjectDeploymentId(), workflowUuid,
            findRequestTriggerName(workflow));

        return dispatch(workflowExecutionId);
    }

    private ResponseEntity<Object> dispatch(WorkflowExecutionId workflowExecutionId) {
        if (webhookWorkflowExecutor.isWorkflowDisabled(workflowExecutionId)) {
            return ResponseEntity.ok()
                .build();
        }

        try {
            return doProcessTrigger(workflowExecutionId, null, httpServletRequest, httpServletResponse)
                .join();
        } catch (IOException | ServletException e) {
            throw new RuntimeException(e);
        }
    }
```

Add `AutomationWorkflowProjectFacade automationWorkflowProjectFacade`, `ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade`, and `ProjectWorkflowService projectWorkflowService` to the constructor and field list.

**Flagged assumption** (Global Constraints): this step assumes `doProcessTrigger`/`WebhookWorkflowExecutor` already resolve `PlatformType.AUTOMATION` end-to-end (uuid → workflowId, connection injection via `ProjectDeploymentWorkflowConnection` at task dispatch) the same way they resolve `PlatformType.EMBEDDED` today, since vanilla automation projects already support webhook triggers through this same shared pipeline. Step 5's integration test is what actually proves or disproves this against real Spring wiring.

- [ ] **Step 5: Run the unit test to verify it passes**

Run: `./gradlew :server:ee:libs:embedded:embedded-webhook:embedded-webhook-public-rest:test --tests "*RequestTriggerApiControllerAutomationBridgeTest*" > /tmp/plan4.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration server/ee/libs/embedded/embedded-webhook
git commit -m "Extend POST /workflows/{workflowUuid} with an automation-bridge branch

Integration resolution is untouched and regression-pinned: the new branch only
runs once fetchLastWorkflowId (a new, non-throwing sibling of the unmodified
getLastWorkflowId) comes back empty. An unresolvable catalog uuid and a
disabled/dangling reference both fall through to the same 404 an unknown
workflowUuid already returned, so the endpoint never leaks which uuids exist in
the catalog. Dispatch reuses doProcessTrigger unmodified with
PlatformType.AUTOMATION and the reference's own ProjectDeployment id in place of
an IntegrationInstance id."
```

---

### Task 5: Async invocation bridge — `POST /app-events`

**Files:**
- Modify: `server/ee/libs/embedded/embedded-webhook/embedded-webhook-public-rest/src/main/java/com/bytechef/ee/embedded/webhook/public_/web/rest/AppEventTriggerApiController.java`
- Test: `server/ee/libs/embedded/embedded-webhook/embedded-webhook-public-rest/src/test/java/com/bytechef/ee/embedded/webhook/public_/web/rest/AppEventTriggerApiControllerAutomationBridgeTest.java`

**Interfaces:**
- Consumes: `ConnectedUserProjectWorkflowRepository` reference rows (Task 1), `ConnectedUserCodeWorkflowReferenceFacade` (Task 3) only for the dangling/enabled check — async fan-out has no implicit-provisioning story (there is no single "workflowUuid" the caller named, so there is nothing to provision on demand; only already-provisioned, enabled references participate).
- Produces: nothing new — extends an existing endpoint.

- [ ] **Step 1: Write the failing test**

```java
@ExtendWith(MockitoExtension.class)
class AppEventTriggerApiControllerAutomationBridgeTest {

    @Mock
    private ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;

    @Mock
    private ConnectedUserService connectedUserService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private WebhookWorkflowExecutor webhookWorkflowExecutor;

    @Mock
    private WorkflowService workflowService;

    @Test
    void testEnabledReferenceWithAppEventTriggerIsIncludedInFanOut() {
        ConnectedUser connectedUser = new ConnectedUser();
        connectedUser.setId(1L);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.anyString(), Mockito.any()))
            .thenReturn(connectedUser);

        ConnectedUserProjectWorkflow reference = new ConnectedUserProjectWorkflow();
        reference.setCatalogWorkflowUuid("catalog-uuid-1");
        reference.setProjectDeploymentId(77L);
        reference.setEnabled(true);
        reference.setDangling(false);

        Mockito.when(connectedUserProjectWorkflowRepository.findAllByConnectedUserId(1L))
            .thenReturn(List.of(reference));

        Mockito.when(projectWorkflowService.getLastPublishedWorkflowId("catalog-uuid-1"))
            .thenReturn("catalog-wf-1");

        Workflow workflow = new Workflow();
        workflow.setDefinition(
            "{\"triggers\":[{\"name\":\"t1\",\"type\":\"appEvent/v1/newEvent\"}],\"tasks\":[]}");

        Mockito.when(workflowService.getWorkflow("catalog-wf-1"))
            .thenReturn(workflow);
        Mockito.when(webhookWorkflowExecutor.isWorkflowDisabled(Mockito.any()))
            .thenReturn(false);

        ArgumentCaptor<WorkflowExecutionId> captor = ArgumentCaptor.forClass(WorkflowExecutionId.class);

        // controller() constructed with the existing integration-instance fields returning empty lists, per Step 3
        controller().executeWorkflows(null);

        Mockito.verify(webhookWorkflowExecutor)
            .isWorkflowDisabled(captor.capture());

        WorkflowExecutionId workflowExecutionId = captor.getValue();

        Assertions.assertEquals(PlatformType.AUTOMATION, workflowExecutionId.getType());
        Assertions.assertEquals(77L, workflowExecutionId.getInstanceId());
    }

    @Test
    void testDanglingReferenceIsSkipped() {
        ConnectedUser connectedUser = new ConnectedUser();
        connectedUser.setId(1L);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.anyString(), Mockito.any()))
            .thenReturn(connectedUser);

        ConnectedUserProjectWorkflow dangling = new ConnectedUserProjectWorkflow();
        dangling.setCatalogWorkflowUuid("catalog-uuid-2");
        dangling.setEnabled(true);
        dangling.setDangling(true);

        Mockito.when(connectedUserProjectWorkflowRepository.findAllByConnectedUserId(1L))
            .thenReturn(List.of(dangling));

        controller().executeWorkflows(null);

        Mockito.verifyNoInteractions(webhookWorkflowExecutor);
    }
}
```

(Add the needed `findAllByConnectedUserId(long): List<ConnectedUserProjectWorkflow>` derived-query method to `ConnectedUserProjectWorkflowRepository` as part of this task — the existing `findAllByConnectedUserProjectId` is scoped one level down and doesn't help here since a connected user's automation-bridge references hang off their `ConnectedUserProject`, but the controller only has the connected user's id at this point, mirroring exactly how `getConnectedUserIntegrationInstances(connectedUserId, ...)` already works for the integration side.)

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:ee:libs:embedded:embedded-webhook:embedded-webhook-public-rest:test --tests "*AppEventTriggerApiControllerAutomationBridgeTest*" > /tmp/plan5.log 2>&1; echo $?`
Expected: non-zero.

- [ ] **Step 3: Extend `AppEventTriggerApiController.executeWorkflows`**

Append a second fan-out loop, over references instead of integration instances, after the existing integration-instance loop (left unmodified):

```java
        List<ConnectedUserProjectWorkflow> references = connectedUserProjectWorkflowRepository
            .findAllByConnectedUserId(connectedUser.getId());

        for (ConnectedUserProjectWorkflow reference : references) {
            if (!reference.isEnabled() || reference.isDangling()) {
                continue;
            }

            String catalogWorkflowId = projectWorkflowService.getLastPublishedWorkflowId(
                reference.getCatalogWorkflowUuid());

            Workflow workflow = workflowService.getWorkflow(catalogWorkflowId);

            String appEventTriggerName = findAppEventTriggerName(workflow);

            if (appEventTriggerName == null) {
                continue;
            }

            WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                PlatformType.AUTOMATION, reference.getProjectDeploymentId(), reference.getCatalogWorkflowUuid(),
                appEventTriggerName);

            try {
                doProcessTrigger(workflowExecutionId, null, httpServletRequest, httpServletResponse)
                    .join();
            } catch (IOException | ServletException e) {
                throw new RuntimeException(e);
            }
        }

        return ResponseEntity.ok()
            .build();
```

Add `ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository` and `ProjectWorkflowService projectWorkflowService` to the constructor and field list.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:embedded:embedded-webhook:embedded-webhook-public-rest:test --tests "*AppEventTriggerApiControllerAutomationBridgeTest*" > /tmp/plan5.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-webhook
git commit -m "Extend POST /app-events with an automation-bridge fan-out branch

Runs after the existing integration-instance loop, unmodified, and iterates the
connected user's enabled, non-dangling references instead. Same appEvent/newEvent
trigger gate the integration side already uses, same doProcessTrigger dispatch
primitive, PlatformType.AUTOMATION in place of EMBEDDED."
```

---

### Task 6: Public API — provision/deprovision endpoints and catalog `kind`

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/src/main/java/com/bytechef/ee/embedded/configuration/public_/web/rest/ConnectedUserProjectWorkflowApiController.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/src/main/java/com/bytechef/ee/embedded/configuration/public_/web/rest/mapper/AutomationWorkflowProjectMapper.java`
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/src/test/java/com/bytechef/ee/embedded/configuration/public_/web/rest/ConnectedUserProjectWorkflowApiControllerReferenceIntTest.java`

**Interfaces:**
- Consumes: `ConnectedUserCodeWorkflowReferenceFacade` (Task 3).
- Produces: `POST /{externalUserId}/automation/workflow-templates/{workflowUuid}/provision`, `DELETE /{externalUserId}/automation/workflow-templates/{workflowUuid}/provision`, `AutomationWorkflowProject.kind` (`"COPY" | "REFERENCE"`).

- [ ] **Step 1: Write the failing test**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testint")
class ConnectedUserProjectWorkflowApiControllerReferenceIntTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    void testExplicitProvisionCreatesAReference() {
        // Fixture setup: deploy a code workflow via AutomationWorkflowProjectCodeWorkflowFacade, publish it,
        // then call the new endpoint on behalf of a connected user and assert 204 plus a persisted reference row
        // with catalogWorkflowUuid set and enabled true. Full fixture wiring is the implementer's call; the
        // assertion below is what the endpoint must satisfy.

        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(
            "/api/embedded/v1/{externalUserId}/automation/workflow-templates/{workflowUuid}/provision",
            HttpMethod.POST, new HttpEntity<>(authHeaders("ext-user-1")), Void.class, "ext-user-1", "catalog-uuid-1");

        Assertions.assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    }

    private static HttpHeaders authHeaders(String externalUserId) {
        HttpHeaders headers = new HttpHeaders();

        headers.setBearerAuth("<test bearer token minted for externalUserId per the module's existing test fixtures>");

        return headers;
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:testIntegration --tests "*ConnectedUserProjectWorkflowApiControllerReferenceIntTest*" > /tmp/plan6.log 2>&1; echo $?`
Expected: non-zero — `404` on the not-yet-defined path.

- [ ] **Step 3: Add the openapi.yaml paths**

```yaml
  /{externalUserId}/automation/workflow-templates/{workflowUuid}/provision:
    post:
      description: "Explicitly provision a reference to a catalog code workflow ahead of first invocation."
      summary: "Provision a reference to a catalog code workflow"
      tags:
        - "connected-user-project-workflow"
      operationId: "provisionWorkflowReference"
      parameters:
        - name: "X-Environment"
          in: "header"
          required: false
          schema:
            $ref: '#/components/schemas/Environment'
        - name: "externalUserId"
          in: "path"
          required: true
          schema:
            type: "string"
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
      security:
        - bearerAuth: [ ]
    delete:
      description: "De-provision a reference to a catalog code workflow."
      summary: "De-provision a reference to a catalog code workflow"
      tags:
        - "connected-user-project-workflow"
      operationId: "deprovisionWorkflowReference"
      parameters:
        - name: "X-Environment"
          in: "header"
          required: false
          schema:
            $ref: '#/components/schemas/Environment'
        - name: "externalUserId"
          in: "path"
          required: true
          schema:
            type: "string"
        - name: "workflowUuid"
          in: "path"
          required: true
          schema:
            type: "string"
      responses:
        "204":
          description: "Successful operation."
      security:
        - bearerAuth: [ ]
```

Add `kind` to the `AutomationWorkflowProject` schema:

```yaml
        kind:
          description: "Whether copying this project's templates creates a per-user copy or a shared reference."
          type: "string"
          enum:
            - "COPY"
            - "REFERENCE"
```

Regenerate: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:openApiGenerate > /tmp/plan6b.log 2>&1; echo $?` (expected `0`; substitute the module's actual generator task name if different, per the `tasks --all` lookup shown in Task 2 Step 8).

- [ ] **Step 4: Add the controller handlers**

```java
    @Override
    public ResponseEntity<Void> provisionWorkflowReference(
        String externalUserId, String workflowUuid, EnvironmentModel xEnvironment) {

        SecurityUtils.checkCurrentUserLogin(externalUserId);

        try {
            connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
                externalUserId, workflowUuid, getEnvironment(xEnvironment));
        } catch (MissingConnectionException missingConnectionException) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .build();
        }

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> deprovisionWorkflowReference(
        String externalUserId, String workflowUuid, EnvironmentModel xEnvironment) {

        SecurityUtils.checkCurrentUserLogin(externalUserId);

        connectedUserCodeWorkflowReferenceFacade.deleteReference(
            externalUserId, workflowUuid, getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }
```

Add `ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade` to the controller's constructor.

- [ ] **Step 5: Add `kind` to the mapper**

In `AutomationWorkflowProjectMapper.java`, add a `kind()` computation — `"REFERENCE"` when the project's code-workflow container is present (`projectCodeWorkflowService.fetchProjectCodeWorkflow(project.getId()).isPresent()`, mirroring the existence check Task 2 relies on), else `"COPY"`. Wire it as an `@AfterMapping` step or a source-side DTO field, matching whichever pattern the existing mapper already uses for computed fields (check the file's current `@Mapping`/`@AfterMapping` usage before choosing).

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:testIntegration --tests "*ConnectedUserProjectWorkflowApiControllerReferenceIntTest*" > /tmp/plan6.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration
git commit -m "Add explicit provision/deprovision endpoints and catalog kind field

Full public parity with the visual bridge's copy/delete pair, for backends that
want to pre-wire connections before the first invocation instead of relying on
implicit provisioning. kind lets clients tell reference-provisioning catalog
projects apart from copy-provisioning ones in the same listing."
```

---

### Task 7: End-to-end integration test

**Files:**
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/AutomationCodeWorkflowBridgeIntTest.java`

**Interfaces:**
- Consumes: everything from Tasks 1-6.
- Produces: nothing.

- [ ] **Step 1: Write the integration test**

```java
@SpringBootTest
@ActiveProfiles("testint")
class AutomationCodeWorkflowBridgeIntTest {

    @Autowired
    private AutomationWorkflowProjectCodeWorkflowFacade automationWorkflowProjectCodeWorkflowFacade;

    @Autowired
    private ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Test
    void testDeployThenTwoUsersReferenceTheSameWorkflowWithIndependentConnections() {
        automationWorkflowProjectCodeWorkflowFacade.save(
            fixtureProjectBytes("bridge-e2e"), CodeWorkflowContainer.Language.JAVASCRIPT);

        Project project = projectService.getProject(
            projectService.fetchProject("__EMBEDDED_AUTOMATION__bridge-e2e")
                .orElseThrow()
                .getId());

        ProjectWorkflow catalogProjectWorkflow = projectWorkflowService.getProjectWorkflows(
            project.getId(), project.getLastProjectVersion())
            .getFirst();

        String catalogWorkflowUuid = catalogProjectWorkflow.getUuidAsString();

        // fixture: create one embedded connection per user (userA/userB) for the fixture's component

        ConnectedUserProjectWorkflow referenceA = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            "userA", catalogWorkflowUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow referenceB = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            "userB", catalogWorkflowUuid, Environment.PRODUCTION);

        Assertions.assertNotEquals(referenceA.getProjectDeploymentId(), referenceB.getProjectDeploymentId());
    }

    @Test
    void testRedeployRemovingAWorkflowMarksExistingReferencesDangling() {
        automationWorkflowProjectCodeWorkflowFacade.save(
            fixtureProjectBytes("bridge-e2e-2"), CodeWorkflowContainer.Language.JAVASCRIPT);

        Project project = projectService.fetchProject("__EMBEDDED_AUTOMATION__bridge-e2e-2")
            .orElseThrow();

        String catalogWorkflowUuid = projectWorkflowService.getProjectWorkflows(
            project.getId(), project.getLastProjectVersion())
            .getFirst()
            .getUuidAsString();

        ConnectedUserProjectWorkflow reference = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            "userC", catalogWorkflowUuid, Environment.PRODUCTION);

        Assertions.assertFalse(reference.isDangling());

        automationWorkflowProjectCodeWorkflowFacade.save(
            fixtureProjectBytesWithoutTheOriginalWorkflow("bridge-e2e-2"), CodeWorkflowContainer.Language.JAVASCRIPT);

        connectedUserCodeWorkflowReferenceFacade.markDanglingReferences(
            project.getId(), Set.of() /* the redeployed catalog now has zero workflows matching the old uuid */);

        // re-fetch and assert dangling; exact re-fetch mechanism (repository vs facade) is the implementer's call
    }
}
```

`fixtureProjectBytes(name)` / `fixtureProjectBytesWithoutTheOriginalWorkflow(name)` build a minimal JavaScript `ProjectHandler` source (single workflow, one component-typed task) the same way the existing `ProjectCodeWorkflowFacadeImplIntTest`-style fixtures already do in this codebase — check for an existing polyglot JS fixture string in `automation-configuration-service`'s test sources first and reuse its shape rather than inventing a new one.

- [ ] **Step 2: Run it**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:testIntegration --tests "*AutomationCodeWorkflowBridgeIntTest*" > /tmp/plan7.log 2>&1; echo $?`
Expected: `0`. Confirm no failures: `grep -c "FAILED" /tmp/plan7.log` is `0`.

- [ ] **Step 3: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration
git commit -m "Add end-to-end embedded automation code workflow bridge test

Covers the two properties every other task's unit tests only assert in
isolation: two users referencing the same deployed catalog workflow get
independent ProjectDeployments, and a redeploy that drops a workflow marks
existing references dangling rather than deleting them."
```

---

### Task 8: Documentation

**Files:**
- Create: `docs/content/docs/embedded/automation-code-workflows.mdx`
- Modify: `CLAUDE.md`
- Modify: `claude-code-plugin/bytechef-dev/skills/bytechef-code-workflow/SKILL.md`

**Interfaces:**
- Consumes: the endpoint paths and semantics from Tasks 2-6.
- Produces: nothing.

- [ ] **Step 1: Write the user documentation**

Create `docs/content/docs/embedded/automation-code-workflows.mdx` with the `Coming Soon` callout (matching `docs/content/docs/automation/error-workflows.mdx`'s convention), covering: what the bridge is (deploy once as a plain automation code workflow, serve to connected users by reference); the deploy endpoint and its `ADMIN`-only, marker-hidden catalog project; the difference between reference-mode (code workflows, deploy-once/shared, never editable per user) and copy-mode (visual templates, per-user copy); implicit provisioning on first invocation plus the explicit provision/deprovision endpoints; the 409 contract for an unresolvable connection; the dangling state and what a user sees when a vendor redeploy removes a workflow they reference; and the two invocation endpoints unchanged in shape (`POST /workflows/{workflowUuid}`, `POST /app-events`), same `request`-trigger / app-event-trigger requirements integration workflows already have.

- [ ] **Step 2: Add the CLAUDE.md entry**

Add under a new `### Embedded automation code workflow bridge` heading, in the AI Hub / MCP section area:

```markdown
### Embedded automation code workflow bridge

`POST /api/embedded/internal/automation/projects/deploy` (ADMIN-only, same Java-hardening posture as
`/integrations/deploy`) deploys a plain automation code workflow (`ProjectHandler`/`project-api`)
behind `AutomationWorkflowProjectFacade`'s `__EMBEDDED_AUTOMATION__` marker via a new
`AutomationWorkflowProjectCodeWorkflowFacade` -- the SAME artifact deployed through the plain
automation endpoint creates an unmarked, unrelated project; the marker is what makes it embedded-
servable. `ConnectedUserProjectWorkflow` gained a nullable `catalog_workflow_uuid` discriminator:
non-null means the row is a reference to a shared catalog workflow (never a per-user copy, never
editable) instead of a copy-mode row. Per-user connection wiring for a reference lives in a new
`connected_user_project_workflow_connection` table, NOT `WorkflowTestConfiguration` (that table is
keyed by `workflowId` alone, and a shared catalog workflow has exactly one `workflowId` across every
referencing user -- reusing it would leak one user's connection into another's run). Each reference
gets its own per-(catalog project, connected user) `ProjectDeployment`, looked up by name via
`ProjectDeploymentService.fetchProjectDeploymentByName` (new; the existing
`fetchProjectDeployment(projectId, environment)` assumes one deployment per project+environment,
which only holds because copy-mode gives each user their own private project). `POST
/workflows/{workflowUuid}` and `POST /app-events` both gained an automation-bridge fallback branch
that only runs once the existing integration-workflow lookup comes back empty (regression-pinned
unchanged); dispatch reuses `AbstractWebhookTriggerController.doProcessTrigger` unmodified with
`PlatformType.AUTOMATION` and the reference's `ProjectDeploymentId` in place of an
`IntegrationInstance` id. A redeploy that drops a workflow flips existing references to a disabled
`dangling` state (`ConnectedUserCodeWorkflowReferenceFacade.markDanglingReferences`) instead of
deleting them. Spec: `docs/superpowers/specs/2026-07-27-embedded-automation-code-workflows-design.md`.
```

- [ ] **Step 3: Extend the `bytechef-code-workflow` skill**

Add a section to `claude-code-plugin/bytechef-dev/skills/bytechef-code-workflow/SKILL.md` covering the embedded bridge: same `ProjectHandler` artifact and CLI scaffolding as the two existing standalone surfaces, deployed via `POST /api/embedded/internal/automation/projects/deploy` instead of the plain automation endpoint when the intent is "serve this to embedded connected users"; deploy-once/reference-per-user model; no per-connected-user editing.

- [ ] **Step 4: Verify the docs build**

Run: `cd docs && npm run types:check > /tmp/plan8.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 5: Commit**

```bash
git add docs CLAUDE.md claude-code-plugin
git commit -m "Document the embedded automation code workflow bridge

Covers the deploy-once/reference-per-user model, the reference-vs-copy
distinction, implicit provisioning and the 409 contract, and the dangling
state -- plus a bridge section on the bytechef-code-workflow skill, which
previously only documented the two standalone deploy surfaces."
```

---

## Ambiguities resolved while planning

The spec settles *what* the feature does; these are *how* decisions the spec left to implementation, resolved here because the plan needs exact schemas and method signatures:

1. **Reference identity.** The spec says a reference points at "the catalog `ProjectWorkflow` (uuid-stable across versions)" but doesn't say whether a reference row gets its own identifier. Resolved: it doesn't need one — every public surface already addresses a workflow by the catalog uuid (listing, sync endpoint path variable), so a reference is looked up by `(connectedUserProjectId, catalogWorkflowUuid)`, not by a second uuid of its own.
2. **Where per-user connection choices live.** `WorkflowTestConfiguration` (the mechanism copy-mode and the visual bridge already use) is keyed by `workflowId` alone. A shared catalog workflow has exactly one `workflowId` for every referencing user, so reusing it would let one user's connection choice overwrite another's. Resolved with a new join table scoped by the reference row's own id.
3. **What backs invocation.** The spec says "run via `PrincipalJobFacade` with that user's connections" without specifying the mechanism. Tracing `ProjectTaskDispatcherPreSendProcessor` shows automation connections are injected per task at dispatch time from `ProjectDeploymentWorkflowConnection`, keyed by the job's `ProjectDeployment`. Resolved: each reference gets its own dedicated `ProjectDeployment` against the (shared) catalog project, found by name rather than by the existing `(projectId, environment)` lookup (which assumes one deployment per project — true only because copy-mode's projects are already per-user).
4. **Invocation dispatch primitive.** Resolved in favor of reusing `AbstractWebhookTriggerController.doProcessTrigger` unmodified with `PlatformType.AUTOMATION`, rather than a bespoke `PrincipalJobFacade.createJob` + manual `WEBHOOK_RESPONSE` extraction — the latter would have to re-derive the `request`-trigger's HTTP-payload plumbing (`WebhookRequestUtils`) that the existing pipeline already owns. This is flagged as a testable assumption in Task 4 rather than a fully source-verified fact, since `WebhookWorkflowExecutorImpl`'s internals were not read line-by-line.
5. **409 exception shape.** `ConfigurationException`/`AbstractException` all map to HTTP 400 in this codebase (`GlobalResponseEntityExceptionHandler.handleAbstractException`), so `MissingConnectionException` deliberately does not extend it, and both callers (sync bridge, explicit provision endpoint) catch it locally and return 409 — the same controller-local catch-and-translate pattern `copyWorkflowTemplate`/`copyFrontendWorkflowTemplate` already use for their own 404 case.
