# MCP Tool Enable/Disable Toggle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a per-tool enable/disable toggle to the automation and embedded MCP-server-definition pages; a disabled tool is hidden from `tools/list` and rejected on `tools/call`.

**Architecture:** Add one `enabled` flag to the shared platform `McpTool` (entity + `mcp_tool` column, default `true`). Expose it through a dedicated `updateMcpToolEnabled` GraphQL mutation and a service `updateEnabled` method. Filter disabled tools at the two serve-time choke points (`AutomationMcpServerConfiguration.buildToolSpecifications` for automation; `EmbeddedMcpToolFacade.getFunctionToolCallback` for embedded). Add a `Switch` to both client tool-row components, mirroring the existing server-level toggle.

**Tech Stack:** Java 25 / Spring Boot 4 / Spring Data JDBC / Liquibase / Spring GraphQL; React 19 / TypeScript / TanStack Query / graphql-codegen / Vitest.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-07-06-mcp-tool-enable-disable-toggle-design.md`.
- Default state is **enabled**: `McpTool.enabled` defaults to `true` in the constructor; the DB column is `BOOLEAN NOT NULL DEFAULT true` (backfills existing rows).
- Disabled semantics: **hidden + not callable** — filter happens only at serve time, NEVER in `McpToolService.getMcpComponentMcpTools` (the management UI must still list disabled tools).
- EE files (`server/ee/**`, `client/src/ee/**`) use the ByteChef Enterprise license header; EE Java classes carry the `@version ee` Javadoc tag. CE files keep the Apache 2.0 header. The EE license header is triggered by `@version ee` content — add it to every touched `server/ee` Java file including tests.
- Java: blank line before control statements and after a variable modification that precedes its use; no trailing blank line before a class's closing brace; no `_`-prefixed or cryptic names.
- Client: ESLint `sort-keys` (object keys ascending), sorted named imports within `{}`, interface names end in `I`/`Props`, `useRef` vars end in `Ref`, `twMerge` (not `cn`), Lucide icons imported with `Icon` suffix.
- Before finishing a server change run `./gradlew spotlessApply`. Before finishing a client change run `npm run check` from `client/`.
- Commit messages: server `<description>`; client `client - <description>`. End each commit body with `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`. Use a real ticket number if one is assigned; otherwise omit the number prefix.

---

### Task 1: `McpTool.enabled` field, migration, and `updateEnabled` service (CE)

**Files:**
- Modify: `server/libs/platform/platform-mcp/platform-mcp-api/src/main/java/com/bytechef/platform/mcp/domain/McpTool.java`
- Create: `server/libs/platform/platform-mcp/platform-mcp-service/src/main/resources/config/liquibase/changelog/platform/mcp/20260706000001_mcp_add_enabled_column_mcp_tool.xml`
- Modify: `server/libs/platform/platform-mcp/platform-mcp-api/src/main/java/com/bytechef/platform/mcp/service/McpToolService.java`
- Modify: `server/libs/platform/platform-mcp/platform-mcp-service/src/main/java/com/bytechef/platform/mcp/service/McpToolServiceImpl.java`
- Test: `server/libs/platform/platform-mcp/platform-mcp-service/src/test/java/com/bytechef/platform/mcp/service/McpToolServiceIntTest.java`

**Interfaces:**
- Consumes: nothing new.
- Produces: `McpTool.isEnabled()` / `McpTool.setEnabled(boolean)`; `McpToolService.updateEnabled(long mcpToolId, boolean enabled)`; `mcp_tool.enabled` column.

The changelog is auto-discovered — `master.xml` uses `<includeAll path=".../changelog/platform/mcp/"/>`, so a new file with a lexically-later name is picked up automatically. No master file edit needed.

- [ ] **Step 1: Write the failing test**

Add these two tests to `McpToolServiceIntTest.java` (after `testUpdate`):

```java
    @Test
    public void testCreateDefaultsToEnabled() {
        McpTool mcpTool = mcpToolService.create(getMcpTool());

        assertThat(mcpTool.isEnabled()).isTrue();
    }

    @Test
    public void testUpdateEnabled() {
        McpTool mcpTool = mcpToolRepository.save(getMcpTool());

        mcpToolService.updateEnabled(mcpTool.getId(), false);

        assertThat(mcpToolRepository.findById(mcpTool.getId()))
            .get()
            .extracting(McpTool::isEnabled)
            .isEqualTo(false);
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-mcp:platform-mcp-service:test --tests 'com.bytechef.platform.mcp.service.McpToolServiceIntTest'`
Expected: FAIL — `isEnabled()` / `updateEnabled` do not exist (compile error).

- [ ] **Step 3: Add the field to `McpTool.java`**

Add the column field after the `parameters` field (around line 48):

```java
    @Column
    private boolean enabled;
```

Initialize it to `true` in each non-default constructor. In `McpTool(String name, Map<String, ?> parameters)`:

```java
    public McpTool(String name, Map<String, ?> parameters) {
        this.name = name;
        this.parameters = new MapWrapper(parameters);
        this.enabled = true;
    }
```

In `McpTool(String name, Map<String, ?> parameters, long mcpComponentId)`:

```java
    public McpTool(String name, Map<String, ?> parameters, long mcpComponentId) {
        this.name = name;
        this.parameters = new MapWrapper(parameters);
        this.mcpComponentId = AggregateReference.to(mcpComponentId);
        this.enabled = true;
    }
```

In `McpTool(Long id, String name, Map<String, ?> parameters, Long mcpComponentId)`:

```java
    public McpTool(Long id, String name, Map<String, ?> parameters, Long mcpComponentId) {
        this.id = id;
        this.name = name;
        this.parameters = new MapWrapper(parameters);
        this.mcpComponentId = AggregateReference.to(mcpComponentId);
        this.enabled = true;
    }
```

Add the getter (place it near the other getters, e.g. after `getCreatedDate()`):

```java
    public boolean isEnabled() {
        return enabled;
    }
```

Add the setter (near the other setters, e.g. before `setId`):

```java
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
```

Add `enabled` to `toString()` (append before the closing `'}'`):

```java
            ", enabled=" + enabled +
```

- [ ] **Step 4: Create the Liquibase changelog**

Create `20260706000001_mcp_add_enabled_column_mcp_tool.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">
    <changeSet id="20260706000001" author="Ivica Cardic">
        <preConditions onFail="MARK_RAN">
            <not>
                <columnExists tableName="mcp_tool" columnName="enabled"/>
            </not>
        </preConditions>

        <addColumn tableName="mcp_tool">
            <column name="enabled" type="BOOLEAN" defaultValueBoolean="true">
                <constraints nullable="false"/>
            </column>
        </addColumn>
    </changeSet>
</databaseChangeLog>
```

- [ ] **Step 5: Add `updateEnabled` to the service interface**

In `McpToolService.java`, add after the `update` method:

```java
    /**
     * Updates the enabled state of an MCP tool.
     *
     * @param mcpToolId the ID of the MCP tool
     * @param enabled   the new enabled state
     */
    void updateEnabled(long mcpToolId, boolean enabled);
```

- [ ] **Step 6: Implement `updateEnabled` in `McpToolServiceImpl.java`**

This impl carries `@PreAuthorize` RBAC annotations on its mutating methods (e.g. `update` has `@PreAuthorize("hasPermission(#mcpTool.id, 'McpTool', 'MCP_EDIT')")`). Add `updateEnabled` with a matching annotation so it enforces the same edit permission. The `org.springframework.security.access.prepost.PreAuthorize` import is already present. Add after the `update` method:

```java
    @Override
    @PreAuthorize("hasPermission(#mcpToolId, 'McpTool', 'MCP_EDIT')")
    public void updateEnabled(long mcpToolId, boolean enabled) {
        McpTool mcpTool = OptionalUtils.get(mcpToolRepository.findById(mcpToolId));

        mcpTool.setEnabled(enabled);

        mcpToolRepository.save(mcpTool);
    }
```

- [ ] **Step 7: Run test to verify it passes**

Run: `./gradlew :server:libs:platform:platform-mcp:platform-mcp-service:test --tests 'com.bytechef.platform.mcp.service.McpToolServiceIntTest'`
Expected: PASS (all tests, including the two new ones).

- [ ] **Step 8: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-mcp/platform-mcp-api/src/main/java/com/bytechef/platform/mcp/domain/McpTool.java \
        server/libs/platform/platform-mcp/platform-mcp-api/src/main/java/com/bytechef/platform/mcp/service/McpToolService.java \
        server/libs/platform/platform-mcp/platform-mcp-service/src/main/java/com/bytechef/platform/mcp/service/McpToolServiceImpl.java \
        server/libs/platform/platform-mcp/platform-mcp-service/src/main/resources/config/liquibase/changelog/platform/mcp/20260706000001_mcp_add_enabled_column_mcp_tool.xml \
        server/libs/platform/platform-mcp/platform-mcp-service/src/test/java/com/bytechef/platform/mcp/service/McpToolServiceIntTest.java
git commit -m "$(cat <<'EOF'
Add enabled flag to McpTool with updateEnabled service

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: GraphQL `enabled` field + `updateMcpToolEnabled` mutation (CE)

**Files:**
- Modify: `server/libs/platform/platform-mcp/platform-mcp-graphql/src/main/resources/graphql/mcp-tool.graphqls`
- Modify: `server/libs/platform/platform-mcp/platform-mcp-graphql/src/main/java/com/bytechef/platform/mcp/web/graphql/McpToolGraphQlController.java`
- Test: `server/libs/platform/platform-mcp/platform-mcp-graphql/src/test/java/com/bytechef/platform/mcp/web/graphql/McpToolGraphQlControllerIntTest.java`

**Interfaces:**
- Consumes: `McpToolService.updateEnabled(long, boolean)`, `McpTool.isEnabled()` (Task 1).
- Produces: GraphQL `McpTool.enabled: Boolean!`, mutation `updateMcpToolEnabled(id: ID!, enabled: Boolean!): McpTool`.

- [ ] **Step 1: Write the failing test**

Add to `McpToolGraphQlControllerIntTest.java` (after `testCreateMcpToolWithEmptyParameters`):

```java
    @Test
    void testUpdateMcpToolEnabled() {
        // Given
        McpTool mockTool = createMockMcpTool(1L, "test-tool", Map.of("param1", "value1"), 1L);

        mockTool.setEnabled(false);

        when(mcpToolService.fetchMcpTool(1L)).thenReturn(Optional.of(mockTool));

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    updateMcpToolEnabled(id: "1", enabled: false) {
                        id
                        enabled
                    }
                }
                """)
            .execute()
            .path("updateMcpToolEnabled.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("updateMcpToolEnabled.enabled")
            .entity(Boolean.class)
            .isEqualTo(false);

        verify(mcpToolService).updateEnabled(1L, false);
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-mcp:platform-mcp-graphql:test --tests 'com.bytechef.platform.mcp.web.graphql.McpToolGraphQlControllerIntTest'`
Expected: FAIL — mutation `updateMcpToolEnabled` is not in the schema / `enabled` not a field.

- [ ] **Step 3: Add schema field and mutation**

In `mcp-tool.graphqls`, add the mutation inside `extend type Mutation`:

```graphql
    updateMcpToolEnabled(id: ID!, enabled: Boolean!): McpTool
```

And add the field to the `McpTool` type (after `parameters: Map`):

```graphql
    enabled: Boolean!
```

- [ ] **Step 4: Add the controller mapping**

In `McpToolGraphQlController.java`, add after `updateMcpTool`:

```java
    @MutationMapping
    public McpTool updateMcpToolEnabled(@Argument long id, @Argument boolean enabled) {
        mcpToolService.updateEnabled(id, enabled);

        return mcpToolService.fetchMcpTool(id)
            .orElseThrow(() -> new IllegalArgumentException("MCP tool not found: " + id));
    }
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :server:libs:platform:platform-mcp:platform-mcp-graphql:test --tests 'com.bytechef.platform.mcp.web.graphql.McpToolGraphQlControllerIntTest'`
Expected: PASS.

- [ ] **Step 6: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-mcp/platform-mcp-graphql/src/main/resources/graphql/mcp-tool.graphqls \
        server/libs/platform/platform-mcp/platform-mcp-graphql/src/main/java/com/bytechef/platform/mcp/web/graphql/McpToolGraphQlController.java \
        server/libs/platform/platform-mcp/platform-mcp-graphql/src/test/java/com/bytechef/platform/mcp/web/graphql/McpToolGraphQlControllerIntTest.java
git commit -m "$(cat <<'EOF'
Add updateMcpToolEnabled GraphQL mutation and enabled field

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: Automation serve-time filter (CE)

**Files:**
- Modify: `server/libs/automation/automation-ai/automation-ai-mcp-server/src/main/java/com/bytechef/automation/ai/mcp/server/config/AutomationMcpServerConfiguration.java` (the component-tool stream inside `buildToolSpecifications`, ~lines 145-151)
- Test (MODIFY — file already exists): `server/libs/automation/automation-ai/automation-ai-mcp-server/src/test/java/com/bytechef/automation/ai/mcp/server/config/AutomationMcpServerConfigurationTest.java`

**Interfaces:**
- Consumes: `McpTool.isEnabled()` (Task 1); the package-private static `AutomationMcpServerConfiguration.buildToolSpecifications(...)`.
- Produces: automation `tools/list` excludes disabled tools.

The module ALREADY has a test source set and JUnit/Mockito/AssertJ test deps (the existing `AutomationMcpServerConfigurationTest` uses them) — do NOT touch `build.gradle.kts`, do NOT create a new test file. Add one test method to the existing test class, following its established style (AssertJ `assertThat`, `mock`/`when` static imports, `mcpServer` built via `new McpServer(...).setId(MCP_SERVER_ID)` in `setUp`). The existing `setUp()` already creates the `mcpComponentService`/`mcpProjectService`/`mcpServerService`/`mcpToolService`/`mcpToolFacade`/`workspaceMcpServerService` mocks and the `mcpServer` with `MCP_SERVER_ID = 42L`, `SECRET_KEY = "secret-key"`, and stubs `getMcpServer(SECRET_KEY)`, `getMcpServerMcpComponents(MCP_SERVER_ID) -> List.of()`, `getMcpServerMcpProjects(MCP_SERVER_ID) -> List.of()`.

- [ ] **Step 1: Write the failing test**

Add these imports to `AutomationMcpServerConfigurationTest.java` (the class already imports `mock`/`when` statically, `McpServer`, `List`, `Optional`, `ObjectProvider`, etc. — add only what's missing, keeping imports sorted):

```java
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpTool;
import java.util.Map;
import java.util.function.Function;
import org.springframework.ai.tool.function.FunctionToolCallback;
```

Add this test method to the class (it overrides the `setUp` default `getMcpServerMcpComponents` stub with a component that has a mix of enabled/disabled tools):

```java
    @Test
    void testBuildToolSpecificationsExcludesDisabledTools() {
        McpComponent mcpComponent = mock(McpComponent.class);

        when(mcpComponent.getId()).thenReturn(10L);
        when(mcpComponentService.getMcpServerMcpComponents(MCP_SERVER_ID)).thenReturn(List.of(mcpComponent));

        McpTool enabledTool = new McpTool("enabled-tool", Map.of(), 10L);

        enabledTool.setId(1L);

        McpTool disabledTool = new McpTool("disabled-tool", Map.of(), 10L);

        disabledTool.setId(2L);
        disabledTool.setEnabled(false);

        when(mcpToolService.getMcpComponentMcpTools(10L)).thenReturn(List.of(enabledTool, disabledTool));

        FunctionToolCallback<Map<String, Object>, Object> enabledCallback = FunctionToolCallback
            .builder("enabled-tool", (Function<Map<String, Object>, Object>) input -> "ok")
            .inputType(Map.class)
            .build();

        when(mcpToolFacade.getFunctionToolCallback(enabledTool)).thenReturn(enabledCallback);
        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(MCP_SERVER_ID)).thenReturn(Optional.empty());

        List<McpServerFeatures.AsyncToolSpecification> specifications =
            AutomationMcpServerConfiguration.buildToolSpecifications(
                SECRET_KEY, mcpComponentService, mcpProjectService, mcpServerService, mcpToolService, mcpToolFacade,
                stubProvider(List.of()), workspaceMcpServerService);

        assertThat(specifications)
            .extracting(specification -> specification.tool()
                .name())
            .containsExactly("enabled-tool");

        verify(mcpToolFacade).getFunctionToolCallback(enabledTool);
        verify(mcpToolFacade, never()).getFunctionToolCallback(disabledTool);
    }
```

(`stubProvider(List.of())` is the existing private helper in the class that returns an empty `ObjectProvider`.)

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-mcp-server:test --tests 'com.bytechef.automation.ai.mcp.server.config.AutomationMcpServerConfigurationTest'`
Expected: FAIL on `testBuildToolSpecificationsExcludesDisabledTools` — without the filter, `getFunctionToolCallback` is invoked for the disabled tool (the `never()` verify fails; the spec list contains 2 names). The other three existing tests still pass.

- [ ] **Step 3: Add the filter**

In `AutomationMcpServerConfiguration.buildToolSpecifications`, insert `.filter(McpTool::isEnabled)` into the component-tool stream (~lines 145-151) so it reads:

```java
        mcpComponentService.getMcpServerMcpComponents(mcpServer.getId())
            .stream()
            .flatMap(
                mcpComponent -> CollectionUtils.stream(
                    mcpToolService.getMcpComponentMcpTools(mcpComponent.getId())))
            .filter(McpTool::isEnabled)
            .map(mcpTool -> McpToolUtils.toAsyncToolSpecification(mcpToolFacade.getFunctionToolCallback(mcpTool)))
            .forEach(tools::add);
```

Add `import com.bytechef.platform.mcp.domain.McpTool;` to `AutomationMcpServerConfiguration.java` if not already present (it currently imports `McpServer` but may not import `McpTool` — add it, keeping imports sorted).

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-mcp-server:test --tests 'com.bytechef.automation.ai.mcp.server.config.AutomationMcpServerConfigurationTest'`
Expected: PASS (all four tests).

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-ai/automation-ai-mcp-server/src/main/java/com/bytechef/automation/ai/mcp/server/config/AutomationMcpServerConfiguration.java \
        server/libs/automation/automation-ai/automation-ai-mcp-server/src/test/java/com/bytechef/automation/ai/mcp/server/config/AutomationMcpServerConfigurationTest.java
git commit -m "$(cat <<'EOF'
Exclude disabled MCP tools from automation tool list

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: Embedded serve-time filter (EE)

**Files:**
- Modify: `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-server/src/main/java/com/bytechef/ee/embedded/ai/mcp/server/facade/EmbeddedMcpToolFacade.java:148-156`
- Test: `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-server/src/test/java/com/bytechef/ee/embedded/ai/mcp/server/facade/EmbeddedMcpToolFacadeEnabledTest.java`

**Interfaces:**
- Consumes: `McpTool.isEnabled()` (Task 1); the existing `EmbeddedMcpToolFacade` 21-arg constructor.
- Produces: `getFunctionToolCallback` returns `null` for a definition-disabled tool, short-circuiting before any per-instance lookup.

The guard goes at the very top of `getFunctionToolCallback`, before `fetchIntegrationInstanceId`, so a disabled tool never touches the injected collaborators. The test constructs the facade with all-mock collaborators and asserts the short-circuit.

- [ ] **Step 1: Write the failing test**

Create `EmbeddedMcpToolFacadeEnabledTest.java` (EE license header, `@version ee`):

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.facade;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class EmbeddedMcpToolFacadeEnabledTest {

    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);
    private final McpComponentService mcpComponentService = mock(McpComponentService.class);
    private final McpIntegrationInstanceToolService mcpIntegrationInstanceToolService =
        mock(McpIntegrationInstanceToolService.class);

    @Test
    void testGetFunctionToolCallbackReturnsNullWhenToolDisabled() {
        EmbeddedMcpToolFacade facade = new EmbeddedMcpToolFacade(
            mock(ClusterElementDefinitionFacade.class), clusterElementDefinitionService,
            mock(ComponentDefinitionService.class), mock(ConnectedUserService.class), mock(Evaluator.class),
            mock(IntegrationInstanceConfigurationService.class),
            mock(IntegrationInstanceConfigurationWorkflowService.class), mock(IntegrationInstanceService.class),
            mock(IntegrationInstanceWorkflowService.class), mock(IntegrationService.class),
            mock(JobCompletionAwaiter.class), mock(JwtTokenService.class), mcpComponentService,
            mock(McpIntegrationInstanceConfigurationWorkflowService.class), mcpIntegrationInstanceToolService,
            mock(McpServerService.class), mock(PrincipalJobFacade.class), "http://localhost:9555",
            mock(TaskExecutionService.class), mock(TaskFileStorage.class), mock(WorkflowService.class));

        McpTool disabledTool = new McpTool("disabled-tool", Map.of(), 10L);

        disabledTool.setId(1L);
        disabledTool.setEnabled(false);

        assertNull(facade.getFunctionToolCallback(disabledTool, "external-user", Environment.PRODUCTION, "tenant"));

        verifyNoInteractions(mcpComponentService, clusterElementDefinitionService, mcpIntegrationInstanceToolService);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:embedded:embedded-ai:embedded-ai-mcp-server:test --tests 'com.bytechef.ee.embedded.ai.mcp.server.facade.EmbeddedMcpToolFacadeEnabledTest'`
Expected: FAIL — without the guard the method calls `fetchIntegrationInstanceId` and other collaborators (interactions occur / non-null or NPE).

- [ ] **Step 3: Add the guard**

In `EmbeddedMcpToolFacade.getFunctionToolCallback`, add the enabled check as the first statement (before `fetchIntegrationInstanceId`):

```java
    public @Nullable FunctionToolCallback<Map<String, Object>, Object> getFunctionToolCallback(
        McpTool mcpTool, String externalUserId, Environment environment, String tenantId) {

        if (!mcpTool.isEnabled()) {
            return null;
        }

        Long integrationInstanceId = fetchIntegrationInstanceId(
            externalUserId, mcpTool.getMcpComponentId(), environment);

        if (integrationInstanceId != null && !isToolEnabled(integrationInstanceId, mcpTool.getId())) {
            return null;
        }
```

(Leave the rest of the method unchanged.)

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:ee:libs:embedded:embedded-ai:embedded-ai-mcp-server:test --tests 'com.bytechef.ee.embedded.ai.mcp.server.facade.EmbeddedMcpToolFacadeEnabledTest'`
Expected: PASS.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-server/src/main/java/com/bytechef/ee/embedded/ai/mcp/server/facade/EmbeddedMcpToolFacade.java \
        server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-server/src/test/java/com/bytechef/ee/embedded/ai/mcp/server/facade/EmbeddedMcpToolFacadeEnabledTest.java
git commit -m "$(cat <<'EOF'
Exclude disabled MCP tools from embedded tool list

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: Client GraphQL operations + codegen

**Files:**
- Modify: `client/src/graphql/platform/configuration/mcpComponentsByServerId.graphql`
- Modify: `client/src/graphql/platform/configuration/mcpToolsByComponentId.graphql`
- Modify: `client/src/graphql/embedded/configuration/embeddedMcpServers.graphql`
- Create: `client/src/graphql/platform/configuration/updateMcpToolEnabled.graphql`
- Regenerate: `client/src/shared/middleware/graphql.ts` (via codegen — do not hand-edit)

**Interfaces:**
- Consumes: server schema field `McpTool.enabled` and mutation `updateMcpToolEnabled` (Task 2).
- Produces: generated hook `useUpdateMcpToolEnabledMutation`; `McpTool.enabled` present on query results for both pages.

- [ ] **Step 1: Add `enabled` to the automation component query**

In `mcpComponentsByServerId.graphql`, add `enabled` to the nested `mcpTools` selection:

```graphql
        mcpTools {
            id,
            enabled
            mcpComponentId
            name
            parameters
            title
            version
        }
```

- [ ] **Step 2: Add `enabled` to `mcpToolsByComponentId.graphql`**

```graphql
query mcpToolsByComponentId($mcpComponentId: ID!) {
    mcpToolsByComponentId(mcpComponentId: $mcpComponentId) {
        id
        enabled
        name
        title
        mcpComponentId
        parameters
        version
    }
}
```

- [ ] **Step 3: Add `enabled` to the embedded query**

In `embeddedMcpServers.graphql`, add `enabled` to the nested `mcpTools` selection:

```graphql
            mcpTools {
                id
                enabled
                mcpComponentId
                name
                title
                parameters
            }
```

- [ ] **Step 4: Create the mutation operation**

Create `updateMcpToolEnabled.graphql`:

```graphql
mutation updateMcpToolEnabled($id: ID!, $enabled: Boolean!) {
    updateMcpToolEnabled(id: $id, enabled: $enabled) {
        id
        enabled
    }
}
```

- [ ] **Step 5: Regenerate the GraphQL client**

Run: `cd client && npx graphql-codegen`
Expected: `src/shared/middleware/graphql.ts` regenerates; `useUpdateMcpToolEnabledMutation` and `enabled` on the `McpTool` type now exist. Verify:

Run: `cd client && grep -c "useUpdateMcpToolEnabledMutation" src/shared/middleware/graphql.ts`
Expected: `1` or more.

- [ ] **Step 6: Typecheck and commit**

Run: `cd client && npm run typecheck`
Expected: passes.

```bash
git add client/src/graphql/platform/configuration/mcpComponentsByServerId.graphql \
        client/src/graphql/platform/configuration/mcpToolsByComponentId.graphql \
        client/src/graphql/platform/configuration/updateMcpToolEnabled.graphql \
        client/src/graphql/embedded/configuration/embeddedMcpServers.graphql \
        client/src/shared/middleware/graphql.ts
git commit -m "$(cat <<'EOF'
client - Add updateMcpToolEnabled operation and tool enabled field

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: Automation tool-row Switch (CE client)

**Files:**
- Modify: `client/src/pages/automation/mcp-servers/components/mcp-component-list/McpComponentToolListItem.tsx`
- Test: `client/src/pages/automation/mcp-servers/components/mcp-component-list/McpComponentToolListItem.test.tsx` (create)

**Interfaces:**
- Consumes: `useUpdateMcpToolEnabledMutation`, `McpTool.enabled` (Task 5).
- Produces: a `Switch` on each automation tool row that calls `updateMcpToolEnabled` and invalidates `['mcpComponentsByServerId']`.

- [ ] **Step 1: Write the failing test**

Create `McpComponentToolListItem.test.tsx`:

```tsx
import {McpActivePopoverProvider} from '@/shared/contexts/McpActivePopoverContext';
import {McpTool} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import McpComponentToolListItem from './McpComponentToolListItem';

const {mutateMock} = vi.hoisted(() => ({mutateMock: vi.fn()}));

vi.mock('./hooks/useMcpProjectComponentToolDropdownMenu', () => ({
    default: () => ({
        handleConfirmDelete: vi.fn(),
        setShowDeleteDialog: vi.fn(),
        showDeleteDialog: false,
    }),
}));

vi.mock('./McpComponentToolPropertiesPopover', () => ({
    default: () => <div>tool-properties-popover</div>,
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useUpdateMcpToolEnabledMutation: () => ({mutate: mutateMock}),
}));

const mcpTool = {enabled: true, id: '42', name: 'createOpportunity', title: 'Create Opportunity'} as McpTool;

const renderItem = () =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <McpActivePopoverProvider>
                <McpComponentToolListItem
                    componentName="affinity"
                    componentVersion={1}
                    connectionId={null}
                    mcpTool={mcpTool}
                />
            </McpActivePopoverProvider>
        </QueryClientProvider>
    );

describe('McpComponentToolListItem', () => {
    it('disables the tool via the enabled switch', () => {
        renderItem();

        fireEvent.click(screen.getByRole('switch'));

        expect(mutateMock).toHaveBeenCalledWith(
            {enabled: false, id: '42'},
            expect.objectContaining({onSettled: expect.any(Function), onSuccess: expect.any(Function)})
        );
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd client && npx vitest run src/pages/automation/mcp-servers/components/mcp-component-list/McpComponentToolListItem.test.tsx`
Expected: FAIL — no element with `role="switch"`.

- [ ] **Step 3: Add the switch and handler**

Edit `McpComponentToolListItem.tsx`. Update imports (keep named imports sorted):

```tsx
import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import Switch from '@/components/Switch/Switch';
import {Popover, PopoverAnchor} from '@/components/ui/popover';
import {useCloseActivePopoverOnUnmount, useMcpActivePopover} from '@/shared/contexts/McpActivePopoverContext';
import {McpTool, useUpdateMcpToolEnabledMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {BoltIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';

import McpComponentToolPropertiesPopover from './McpComponentToolPropertiesPopover';
import useMcpProjectComponentToolDropdownMenu from './hooks/useMcpProjectComponentToolDropdownMenu';
```

Inside the component body, add state and the handler. Place `useState` first, then the existing hooks, then the query hooks and handler (respecting the project's hook-ordering rule):

```tsx
    const [isEnablePending, setIsEnablePending] = useState(false);

    const {handleConfirmDelete, setShowDeleteDialog, showDeleteDialog} = useMcpProjectComponentToolDropdownMenu({
        mcpTool,
    });

    const {activePopoverId, closePopover, openPopover} = useMcpActivePopover();

    const queryClient = useQueryClient();

    const updateMcpToolEnabledMutation = useUpdateMcpToolEnabledMutation();

    const popoverId = `component-tool-${mcpTool.id}`;
    const isPopoverOpen = activePopoverId === popoverId;

    useCloseActivePopoverOnUnmount(isPopoverOpen);

    const handleEnabledChange = (value: boolean) => {
        setIsEnablePending(true);

        updateMcpToolEnabledMutation.mutate(
            {enabled: value, id: mcpTool.id},
            {
                onSettled: () => {
                    setIsEnablePending(false);
                },
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['mcpComponentsByServerId']});
                },
            }
        );
    };
```

Add the `Switch` as the first child of the actions container (the `<div className="flex shrink-0 items-center gap-0.5">`), before the `PopoverAnchor`:

```tsx
                    <div className="flex shrink-0 items-center gap-0.5">
                        <Switch
                            aria-label="Enable tool"
                            checked={mcpTool.enabled}
                            disabled={isEnablePending}
                            onCheckedChange={handleEnabledChange}
                        />

                        {/* Anchor the popover to the Configure button so it opens right-aligned to that button. */}

                        <PopoverAnchor asChild>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd client && npx vitest run src/pages/automation/mcp-servers/components/mcp-component-list/McpComponentToolListItem.test.tsx`
Expected: PASS.

- [ ] **Step 5: Full client check and commit**

Run: `cd client && npm run check`
Expected: lint + typecheck + tests pass.

```bash
git add client/src/pages/automation/mcp-servers/components/mcp-component-list/McpComponentToolListItem.tsx \
        client/src/pages/automation/mcp-servers/components/mcp-component-list/McpComponentToolListItem.test.tsx
git commit -m "$(cat <<'EOF'
client - Add enable/disable switch to automation MCP tool rows

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
EOF
)"
```

---

### Task 7: Embedded tool-row Switch (EE client)

**Files:**
- Modify: `client/src/ee/pages/embedded/mcp-servers/components/mcp-component-list/McpComponentToolListItem.tsx`
- Test: `client/src/ee/pages/embedded/mcp-servers/components/mcp-component-list/McpComponentToolListItem.test.tsx` (existing — add a case)

**Interfaces:**
- Consumes: `useUpdateMcpToolEnabledMutation`, `McpTool.enabled` (Task 5).
- Produces: a `Switch` on each embedded tool row that calls `updateMcpToolEnabled` and invalidates `['embeddedMcpServers']`.

This mirrors Task 6, but the embedded page's dropdown hook is `useMcpComponentToolDropdownMenu` and the invalidation key is `['embeddedMcpServers']`.

- [ ] **Step 1: Add the failing test case**

Edit the existing `McpComponentToolListItem.test.tsx` (ee). Add the hoisted mutate mock and the graphql module mock at the top (after the existing `vi.mock` calls), give the mock tool an `enabled` field, wrap the render in a `QueryClientProvider`, and add the toggle test. Updated file:

```tsx
import {McpActivePopoverProvider, useMcpActivePopover} from '@/shared/contexts/McpActivePopoverContext';
import {McpTool} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
import {useState} from 'react';
import {describe, expect, it, vi} from 'vitest';

import McpComponentToolListItem from './McpComponentToolListItem';

const {mutateMock} = vi.hoisted(() => ({mutateMock: vi.fn()}));

vi.mock('./hooks/useMcpComponentToolDropdownMenu', () => ({
    default: () => ({
        handleConfirmDelete: vi.fn(),
        setShowDeleteDialog: vi.fn(),
        showDeleteDialog: false,
    }),
}));

vi.mock('./McpComponentToolPropertiesPopover', () => ({
    default: () => <div>tool-properties-popover</div>,
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useUpdateMcpToolEnabledMutation: () => ({mutate: mutateMock}),
}));

const mcpTool = {enabled: true, id: '42', name: 'createOpportunity', title: 'Create Opportunity'} as McpTool;

const ActivePopoverProbe = () => {
    const {activePopoverId} = useMcpActivePopover();

    return <div data-testid="active-popover-id">{activePopoverId ?? 'NONE'}</div>;
};

const Harness = () => {
    const [mounted, setMounted] = useState(true);

    return (
        <QueryClientProvider client={new QueryClient()}>
            <McpActivePopoverProvider>
                <ActivePopoverProbe />

                <button onClick={() => setMounted(false)} type="button">
                    collapse
                </button>

                {mounted && (
                    <McpComponentToolListItem
                        componentName="affinity"
                        componentVersion={1}
                        connectionId={null}
                        mcpTool={mcpTool}
                    />
                )}
            </McpActivePopoverProvider>
        </QueryClientProvider>
    );
};

describe('McpComponentToolListItem', () => {
    it('clears the active popover when the item unmounts (card collapse)', () => {
        render(<Harness />);

        expect(screen.getByTestId('active-popover-id')).toHaveTextContent('NONE');

        fireEvent.click(screen.getByTitle('Configure'));

        expect(screen.getByTestId('active-popover-id')).toHaveTextContent('component-tool-42');
        expect(screen.getByText('tool-properties-popover')).toBeInTheDocument();

        // Simulate the Collapsible card collapsing, which unmounts the tool item.
        fireEvent.click(screen.getByText('collapse'));

        // The active popover must reset so re-expanding the card does not reopen it.
        expect(screen.getByTestId('active-popover-id')).toHaveTextContent('NONE');
    });

    it('disables the tool via the enabled switch', () => {
        render(<Harness />);

        fireEvent.click(screen.getByRole('switch'));

        expect(mutateMock).toHaveBeenCalledWith(
            {enabled: false, id: '42'},
            expect.objectContaining({onSettled: expect.any(Function), onSuccess: expect.any(Function)})
        );
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd client && npx vitest run src/ee/pages/embedded/mcp-servers/components/mcp-component-list/McpComponentToolListItem.test.tsx`
Expected: FAIL — no `role="switch"` for the new case.

- [ ] **Step 3: Add the switch and handler**

Edit the ee `McpComponentToolListItem.tsx`. Update imports (sorted):

```tsx
import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import Switch from '@/components/Switch/Switch';
import {Popover, PopoverAnchor} from '@/components/ui/popover';
import {useCloseActivePopoverOnUnmount, useMcpActivePopover} from '@/shared/contexts/McpActivePopoverContext';
import {McpTool, useUpdateMcpToolEnabledMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {BoltIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';

import McpComponentToolPropertiesPopover from './McpComponentToolPropertiesPopover';
import useMcpComponentToolDropdownMenu from './hooks/useMcpComponentToolDropdownMenu';
```

Inside the component body, add state, query hooks, and handler:

```tsx
    const [isEnablePending, setIsEnablePending] = useState(false);

    const {handleConfirmDelete, setShowDeleteDialog, showDeleteDialog} = useMcpComponentToolDropdownMenu({mcpTool});

    const {activePopoverId, closePopover, openPopover} = useMcpActivePopover();

    const queryClient = useQueryClient();

    const updateMcpToolEnabledMutation = useUpdateMcpToolEnabledMutation();

    const popoverId = `component-tool-${mcpTool.id}`;
    const isPopoverOpen = activePopoverId === popoverId;

    useCloseActivePopoverOnUnmount(isPopoverOpen);

    const handleEnabledChange = (value: boolean) => {
        setIsEnablePending(true);

        updateMcpToolEnabledMutation.mutate(
            {enabled: value, id: mcpTool.id},
            {
                onSettled: () => {
                    setIsEnablePending(false);
                },
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['embeddedMcpServers']});
                },
            }
        );
    };
```

Add the `Switch` as the first child of the actions container (the `<div className="flex shrink-0 items-center gap-0.5">`), before the `PopoverAnchor`:

```tsx
                    <div className="flex shrink-0 items-center gap-0.5">
                        <Switch
                            aria-label="Enable tool"
                            checked={mcpTool.enabled}
                            disabled={isEnablePending}
                            onCheckedChange={handleEnabledChange}
                        />

                        <PopoverAnchor asChild>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd client && npx vitest run src/ee/pages/embedded/mcp-servers/components/mcp-component-list/McpComponentToolListItem.test.tsx`
Expected: PASS (both cases).

- [ ] **Step 5: Full client check and commit**

Run: `cd client && npm run check`
Expected: passes.

```bash
git add client/src/ee/pages/embedded/mcp-servers/components/mcp-component-list/McpComponentToolListItem.tsx \
        client/src/ee/pages/embedded/mcp-servers/components/mcp-component-list/McpComponentToolListItem.test.tsx
git commit -m "$(cat <<'EOF'
client - Add enable/disable switch to embedded MCP tool rows

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
EOF
)"
```

---

## Notes for the implementer

- Commit messages carry NO ticket number (per user 2026-07-06): server commits are just the description; client commits are `client - <description>`.
- This feature targets the `claude/awesome-goodall-86926b` worktree branch, NOT the 5338 main checkout. All work happens in the worktree; that branch already carries RBAC `@PreAuthorize` annotations on MCP service methods, so new service mutations match that pattern.
- Task order matters: 1 → 2 → 3/4 (either order) all precede 5; 5 precedes 6/7. Tasks 3 and 4 are independent of each other; 6 and 7 are independent of each other.
- Do not filter disabled tools inside `McpToolService.getMcpComponentMcpTools` — the management UI relies on it to list disabled tools with their switches.
- If `npm run check` flags `sort-keys` on the new object literals, remember `--fix` does not auto-fix that rule; reorder keys by hand (all the literals above are already alphabetized: `{enabled, id}`, `{onSettled, onSuccess}`).
