# Personal Agent Composer Resources — Server Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a Personal Agent pre-declare any of 8 AI Hub resource kinds (workflows, files, data tables, knowledge bases, MCP servers, API collections, workflow executions, previous tasks); copy them onto every task spawned from the agent as task artifacts.

**Architecture:** Replicate the existing agent tool-template pattern (`ai_hub_personal_agent_tool` → copied into `ai_hub_task_tool` on task spawn) for resources: a new `ai_hub_personal_agent_resource` table, a parallel GraphQL surface, and a parallel copy step in `createAiHubPersonalAgentChat` that writes `ai_hub_task_artifact` rows.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JDBC, Spring for GraphQL, Liquibase, JUnit 5 + Mockito + AssertJ. All code is Enterprise Edition (`server/ee/`) — use the ByteChef Enterprise license header and `@version ee` Javadoc tag.

**Scope:** This plan is the **server half** of the feature (spec §5.1–§5.3). It produces working, testable software on its own — the new GraphQL mutations and the task-spawn copy path are fully functional and verified by tests. The **client half** (`ResourcePickerMenu` extraction, the agent-form picker, and surfacing copied resources in the LLM context — spec §5.4, §5.5, §6) is a separate follow-up plan; see "Follow-up" at the end.

**Spec:** `docs/superpowers/specs/2026-05-21-personal-agent-composer-resources-design.md`

**Module / Gradle paths:**
- `platform-ai-hub-api` → `:server:ee:libs:platform:platform-ai-hub:platform-ai-hub-api`
- `automation-ai-hub-service` → `:server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service`
- `automation-ai-hub-graphql` → `:server:ee:libs:automation:automation-ai-hub:automation-ai-hub-graphql`

**Reference files (existing, mirror these):**
- Entity: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/.../personalagent/AiHubPersonalAgentTool.java`
- Repository: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/.../personalagent/repository/AiHubPersonalAgentToolRepository.java`
- Service: `.../personalagent/AiHubPersonalAgentServiceImpl.java` (methods `listTools` / `addTool` / `removeTool`)
- GraphQL: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/resources/graphql/ai-hub-personal-agent.graphqls`
- Controller: `.../web/graphql/AiHubPersonalAgentGraphQlController.java`
- Copy path: `.../automation/aihub/task/AiHubTaskServiceImpl.java` (`copyAgentToolTemplate`)

---

## Task 1: Extend `AiHubTaskArtifactKind` with 4 referenced-resource kinds

**Files:**
- Modify: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/task/AiHubTaskArtifactKind.java`
- Modify: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/test/java/com/bytechef/ee/platform/aihub/util/EnumOrdinalStabilityTest.java`

- [ ] **Step 1: Update the ordinal stability test first (it will fail)**

In `EnumOrdinalStabilityTest.testTaskArtifactKindOrdinalsAreStable()`, add four entries after `expected.put("KB_REFERENCED", 18);`:

```java
        expected.put("MCP_SERVER_REFERENCED", 19);
        expected.put("API_COLLECTION_REFERENCED", 20);
        expected.put("WORKFLOW_EXECUTION_REFERENCED", 21);
        expected.put("TASK_REFERENCED", 22);
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-api:test --tests "com.bytechef.ee.platform.aihub.util.EnumOrdinalStabilityTest.testTaskArtifactKindOrdinalsAreStable"`
Expected: FAIL — the expected map has 23 entries but the enum still has 19 values.

- [ ] **Step 3: Append the four new enum values**

In `AiHubTaskArtifactKind.java`, after the line `KB_REFERENCED(false);` change it to `KB_REFERENCED(false),` and append:

```java
    // Agent-template referenced resources. Appended at the END per the JDBC enum-storage convention so the
    // ordinals of all earlier values stay pinned. These cover the four composer resource kinds that did not
    // previously have an artifact kind; they are written by the personal-agent task-spawn copy path
    // (copyAgentResourceTemplate). reversible=false — un-referencing is UI bookkeeping, no side effect to undo.
    MCP_SERVER_REFERENCED(false),
    API_COLLECTION_REFERENCED(false),
    WORKFLOW_EXECUTION_REFERENCED(false),
    TASK_REFERENCED(false);
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-api:test --tests "com.bytechef.ee.platform.aihub.util.EnumOrdinalStabilityTest.testTaskArtifactKindOrdinalsAreStable"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/task/AiHubTaskArtifactKind.java server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/test/java/com/bytechef/ee/platform/aihub/util/EnumOrdinalStabilityTest.java
git commit -m "732 Add 4 referenced-resource kinds to AiHubTaskArtifactKind"
```

---

## Task 2: Create `AiHubPersonalAgentResourceKind` enum

**Files:**
- Create: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/personalagent/AiHubPersonalAgentResourceKind.java`
- Create: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/test/java/com/bytechef/ee/platform/aihub/personalagent/AiHubPersonalAgentResourceKindTest.java`
- Modify: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/test/java/com/bytechef/ee/platform/aihub/util/EnumOrdinalStabilityTest.java`

- [ ] **Step 1: Write the failing test for the kind→artifact mapping**

Create `AiHubPersonalAgentResourceKindTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.aihub.personalagent;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.aihub.task.AiHubTaskArtifactKind;
import org.junit.jupiter.api.Test;

/**
 * Pins the {@link AiHubPersonalAgentResourceKind#toArtifactKind()} mapping. The task-spawn copy path translates each
 * agent resource kind into the corresponding {@link AiHubTaskArtifactKind} reference kind; a wrong mapping would record
 * the resource under the wrong artifact kind and surface it incorrectly in the right-panel artifact list.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubPersonalAgentResourceKindTest {

    @Test
    void testToArtifactKindMapsEveryKind() {
        assertThat(AiHubPersonalAgentResourceKind.WORKFLOW.toArtifactKind())
            .isEqualTo(AiHubTaskArtifactKind.WORKFLOW_REFERENCED);
        assertThat(AiHubPersonalAgentResourceKind.FILE.toArtifactKind())
            .isEqualTo(AiHubTaskArtifactKind.FILE_REFERENCED);
        assertThat(AiHubPersonalAgentResourceKind.DATA_TABLE.toArtifactKind())
            .isEqualTo(AiHubTaskArtifactKind.DATA_TABLE_REFERENCED);
        assertThat(AiHubPersonalAgentResourceKind.KNOWLEDGE_BASE.toArtifactKind())
            .isEqualTo(AiHubTaskArtifactKind.KB_REFERENCED);
        assertThat(AiHubPersonalAgentResourceKind.MCP_SERVER.toArtifactKind())
            .isEqualTo(AiHubTaskArtifactKind.MCP_SERVER_REFERENCED);
        assertThat(AiHubPersonalAgentResourceKind.API_COLLECTION.toArtifactKind())
            .isEqualTo(AiHubTaskArtifactKind.API_COLLECTION_REFERENCED);
        assertThat(AiHubPersonalAgentResourceKind.WORKFLOW_EXECUTION.toArtifactKind())
            .isEqualTo(AiHubTaskArtifactKind.WORKFLOW_EXECUTION_REFERENCED);
        assertThat(AiHubPersonalAgentResourceKind.TASK.toArtifactKind())
            .isEqualTo(AiHubTaskArtifactKind.TASK_REFERENCED);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-api:test --tests "com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentResourceKindTest"`
Expected: FAIL — compilation error, `AiHubPersonalAgentResourceKind` does not exist.

- [ ] **Step 3: Create the enum**

Create `AiHubPersonalAgentResourceKind.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.aihub.personalagent;

import com.bytechef.ee.platform.aihub.task.AiHubTaskArtifactKind;

/**
 * Classifies a resource a Personal Agent has pre-declared in its template — one of the AI Hub composer's reference-style
 * resource kinds. Tools are NOT in this enum: a tool is a (component, version, operation) triple persisted in
 * {@code ai_hub_personal_agent_tool}, structurally different from a flat (kind, id, name) reference.
 *
 * <p>
 * <b>Append-only.</b> Persisted as an INT ordinal on {@code ai_hub_personal_agent_resource.kind}. Reordering or deleting
 * a value silently re-maps every historical row. New values MUST be appended at the end;
 * {@code EnumOrdinalStabilityTest#testPersonalAgentResourceKindOrdinals} enforces this at build time.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AiHubPersonalAgentResourceKind {

    // append-only
    WORKFLOW,
    FILE,
    DATA_TABLE,
    KNOWLEDGE_BASE,
    MCP_SERVER,
    API_COLLECTION,
    WORKFLOW_EXECUTION,
    TASK;

    /**
     * Maps this agent resource kind to the {@link AiHubTaskArtifactKind} reference kind the task-spawn copy path
     * ({@code AiHubTaskServiceImpl#copyAgentResourceTemplate}) records it under.
     */
    public AiHubTaskArtifactKind toArtifactKind() {
        return switch (this) {
            case WORKFLOW -> AiHubTaskArtifactKind.WORKFLOW_REFERENCED;
            case FILE -> AiHubTaskArtifactKind.FILE_REFERENCED;
            case DATA_TABLE -> AiHubTaskArtifactKind.DATA_TABLE_REFERENCED;
            case KNOWLEDGE_BASE -> AiHubTaskArtifactKind.KB_REFERENCED;
            case MCP_SERVER -> AiHubTaskArtifactKind.MCP_SERVER_REFERENCED;
            case API_COLLECTION -> AiHubTaskArtifactKind.API_COLLECTION_REFERENCED;
            case WORKFLOW_EXECUTION -> AiHubTaskArtifactKind.WORKFLOW_EXECUTION_REFERENCED;
            case TASK -> AiHubTaskArtifactKind.TASK_REFERENCED;
        };
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-api:test --tests "com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentResourceKindTest"`
Expected: PASS.

- [ ] **Step 5: Add the ordinal stability test**

In `EnumOrdinalStabilityTest.java`, add this method after `testTaskArtifactKindOrdinalsAreStable()` (add the import `com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentResourceKind` at the top):

```java
    @Test
    void testPersonalAgentResourceKindOrdinals() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("WORKFLOW", 0);
        expected.put("FILE", 1);
        expected.put("DATA_TABLE", 2);
        expected.put("KNOWLEDGE_BASE", 3);
        expected.put("MCP_SERVER", 4);
        expected.put("API_COLLECTION", 5);
        expected.put("WORKFLOW_EXECUTION", 6);
        expected.put("TASK", 7);

        OrdinalStabilityAssertions.assertOrdinalsMatch(
            AiHubPersonalAgentResourceKind.values(), expected,
            AiHubPersonalAgentResourceKind.class.getSimpleName());
    }
```

- [ ] **Step 6: Run the ordinal stability test**

Run: `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-api:test --tests "com.bytechef.ee.platform.aihub.util.EnumOrdinalStabilityTest"`
Expected: PASS (all methods).

- [ ] **Step 7: Commit**

```bash
git add server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/personalagent/AiHubPersonalAgentResourceKind.java server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/test/java/com/bytechef/ee/platform/aihub/personalagent/AiHubPersonalAgentResourceKindTest.java server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/test/java/com/bytechef/ee/platform/aihub/util/EnumOrdinalStabilityTest.java
git commit -m "732 Add AiHubPersonalAgentResourceKind enum"
```

---

## Task 3: Create the `AiHubPersonalAgentResource` entity

**Files:**
- Create: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/personalagent/AiHubPersonalAgentResource.java`

There is no isolated behavior to unit-test on a plain Spring Data JDBC entity (it is verified by the repository and service tests in Tasks 5 and 8). This task is a single create + compile + commit.

- [ ] **Step 1: Create the entity**

Mirror `AiHubPersonalAgentTool.java`. The `kind` column is stored as an INT ordinal — the entity holds a raw `int` and converts via `EnumOrdinals`, exactly as `AiHubTaskArtifact` does for its `kind` column.

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.aihub.personalagent;

import com.bytechef.ee.platform.aihub.util.EnumOrdinals;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Per-agent resource template. One row = one (kind, resourceId, resourceName) reference the agent pre-declares — a
 * workflow, file, data table, knowledge base, MCP server, API collection, workflow execution, or previous task. When a
 * task is created against this agent, the routing layer copies these rows into {@code ai_hub_task_artifact} as
 * {@code *_REFERENCED} artifacts so the resources surface in the spawned task — mirroring how
 * {@link AiHubPersonalAgentTool} rows are copied into {@code ai_hub_task_tool}.
 *
 * <p>
 * {@code resourceId} is VARCHAR rather than BIGINT: composer resource ids are strings and workflow ids are
 * non-numeric, so uniform string storage avoids per-kind columns.
 * </p>
 *
 * <p>
 * <b>Lifecycle:</b> rows live as long as their parent {@link AiHubPersonalAgent}; the FK cascades on agent delete.
 * Tasks already spawned from the agent keep their copied artifact rows (those live in {@code ai_hub_task_artifact} and
 * are not FK-linked to this table).
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_hub_personal_agent_resource")
public class AiHubPersonalAgentResource {

    @Id
    private Long id;

    @Column("ai_hub_personal_agent_id")
    private long aiHubPersonalAgentId;

    @Column("kind")
    private int kind;

    @Column("resource_id")
    private String resourceId;

    @Column("resource_name")
    private String resourceName;

    @Column("created_at")
    private LocalDateTime createdAt;

    public AiHubPersonalAgentResource() {
    }

    public AiHubPersonalAgentResource(
        long aiHubPersonalAgentId, AiHubPersonalAgentResourceKind kind, String resourceId, String resourceName) {

        this.aiHubPersonalAgentId = aiHubPersonalAgentId;
        this.kind = kind.ordinal();
        this.resourceId = resourceId;
        this.resourceName = resourceName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getAiHubPersonalAgentId() {
        return aiHubPersonalAgentId;
    }

    public void setAiHubPersonalAgentId(long aiHubPersonalAgentId) {
        this.aiHubPersonalAgentId = aiHubPersonalAgentId;
    }

    public AiHubPersonalAgentResourceKind getKind() {
        return EnumOrdinals.fromOrdinal(kind, AiHubPersonalAgentResourceKind.class);
    }

    public void setKind(AiHubPersonalAgentResourceKind kind) {
        this.kind = kind.ordinal();
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AiHubPersonalAgentResource that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AiHubPersonalAgentResource{" +
            "id=" + id +
            ", aiHubPersonalAgentId=" + aiHubPersonalAgentId +
            ", kind=" + kind +
            ", resourceId='" + resourceId + '\'' +
            ", resourceName='" + resourceName + '\'' +
            '}';
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/personalagent/AiHubPersonalAgentResource.java
git commit -m "732 Add AiHubPersonalAgentResource entity"
```

---

## Task 4: Add the Liquibase changelog

**Files:**
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/config/liquibase/changelog/automation/aihub/20260521000001_ai_hub_personal_agent_resource_init.xml`

The `changelog/automation/aihub` directory is consumed by `<includeAll>` in the global `master.xml` — a new file is picked up by filename order automatically, no master edit needed.

- [ ] **Step 1: Create the changelog**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">
    <!--
        Adds ai_hub_personal_agent_resource — a per-agent template list of (kind, resourceId, resourceName)
        references the agent pre-declares (workflows, files, data tables, knowledge bases, MCP servers, API
        collections, workflow executions, previous tasks). When a task is created against the agent, these rows
        are copied into ai_hub_task_artifact as *_REFERENCED artifacts. Mirrors ai_hub_personal_agent_tool.

        resource_id is VARCHAR, not BIGINT: composer resource ids are strings and workflow ids are non-numeric,
        so a uniform string column avoids per-kind columns. kind is an INT ordinal of
        AiHubPersonalAgentResourceKind.
    -->
    <changeSet id="20260521000001" author="Ivica Cardic">
        <createTable tableName="ai_hub_personal_agent_resource">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="ai_hub_personal_agent_id" type="BIGINT">
                <constraints nullable="false"
                             foreignKeyName="fk_ai_hub_personal_agent_resource_agent"
                             references="ai_hub_personal_agent(id)"
                             deleteCascade="true"/>
            </column>
            <column name="kind" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="resource_id" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="resource_name" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="created_at" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <!--
            Unique on (agent, kind, resource_id): adding the same resource twice is a no-op rather than producing
            duplicate rows that would later attach the same artifact twice to a spawned task.
        -->
        <addUniqueConstraint tableName="ai_hub_personal_agent_resource"
                             columnNames="ai_hub_personal_agent_id, kind, resource_id"
                             constraintName="uk_ai_hub_personal_agent_resource"/>

        <createIndex tableName="ai_hub_personal_agent_resource"
                     indexName="idx_ai_hub_personal_agent_resource_agent">
            <column name="ai_hub_personal_agent_id"/>
        </createIndex>

        <rollback>
            <dropIndex tableName="ai_hub_personal_agent_resource"
                       indexName="idx_ai_hub_personal_agent_resource_agent"/>
            <dropUniqueConstraint tableName="ai_hub_personal_agent_resource"
                                  constraintName="uk_ai_hub_personal_agent_resource"/>
            <dropTable tableName="ai_hub_personal_agent_resource"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

- [ ] **Step 2: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/config/liquibase/changelog/automation/aihub/20260521000001_ai_hub_personal_agent_resource_init.xml
git commit -m "732 Add ai_hub_personal_agent_resource Liquibase changelog"
```

---

## Task 5: Create the `AiHubPersonalAgentResourceRepository`

**Files:**
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/repository/AiHubPersonalAgentResourceRepository.java`

The repository lands in the existing `...personalagent.repository` package, already covered by the module's `@EnableJdbcRepositories` scan — no autoconfiguration change needed. Derived queries are exercised by the service tests (Task 8); no isolated test here.

- [ ] **Step 1: Create the repository**

The `kind` parameter is `int` (the ordinal) because the column is an INT — the service converts the enum to `kind.ordinal()` before calling.

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.aihub.personalagent.repository;

import com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentResource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data JDBC repository for {@link AiHubPersonalAgentResource}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubPersonalAgentResourceRepository extends CrudRepository<AiHubPersonalAgentResource, Long> {

    /**
     * Lists all resource template rows for an agent ordered by creation time so the form and the task copy path see
     * the resources in the order the user added them.
     */
    List<AiHubPersonalAgentResource> findByAiHubPersonalAgentIdOrderByCreatedAtAsc(long aiHubPersonalAgentId);

    /**
     * Lookup by the natural-key tuple. Used to make {@code addResource} idempotent — the unique constraint catches the
     * race; the pre-flight check lets the service return a typed "already added" rather than a 500 from the DB
     * exception. {@code kind} is the INT ordinal of {@code AiHubPersonalAgentResourceKind}.
     */
    Optional<AiHubPersonalAgentResource> findByAiHubPersonalAgentIdAndKindAndResourceId(
        long aiHubPersonalAgentId, int kind, String resourceId);
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/repository/AiHubPersonalAgentResourceRepository.java
git commit -m "732 Add AiHubPersonalAgentResourceRepository"
```

---

## Task 6: Add audit events for resource add/remove

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/audit/AiHubAuditEvent.java`

`AiHubAuditEvent` is not ordinal-persisted, but append the new constants at the end for consistency.

- [ ] **Step 1: Append two constants**

In `AiHubAuditEvent.java`, change the last constant `AI_HUB_WORKSPACE_SETTINGS_UPDATED(false);` to end with `,` and append:

```java
    /**
     * A resource template was attached to an agent. Payload: {@code workspaceId}, {@code agentId}, {@code kind},
     * {@code resourceId}.
     */
    AI_HUB_PERSONAL_AGENT_RESOURCE_ADDED(false),

    /**
     * A resource template was detached from an agent. Payload: {@code workspaceId},
     * {@code personalAgentResourceId}. The parent {@code agentId} is omitted — {@code removeResource} returns
     * {@code void} and the row is gone by {@code @AfterReturning} time.
     */
    AI_HUB_PERSONAL_AGENT_RESOURCE_REMOVED(false);
```

- [ ] **Step 2: Compile**

Run: `./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/audit/AiHubAuditEvent.java
git commit -m "732 Add personal agent resource audit events"
```

---

## Task 7: Add resource methods to the `AiHubPersonalAgentService` interface

**Files:**
- Modify: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/personalagent/AiHubPersonalAgentService.java`

- [ ] **Step 1: Add three methods to the interface**

Add these after `updateToolConfig(...)` (before `cloneToEnvironment`). Add the import `java.util.List` is already present.

```java
    /**
     * Lists the resource template rows associated with the given agent, ordered by creation time. Returns an empty
     * list if the agent has no resources (or doesn't exist). The caller is expected to have ownership-validated the
     * agent id via {@link #findOwned} or equivalent.
     */
    List<AiHubPersonalAgentResource> listResources(long agentId);

    /**
     * Adds a resource template row to the agent. Idempotent: re-adding the same (kind, resourceId) for the agent
     * returns the existing row rather than creating a duplicate, matching the unique constraint. Throws when the
     * agent doesn't belong to the caller — defense in depth alongside the controller's ownership check.
     */
    AiHubPersonalAgentResource addResource(
        long agentId, long workspaceId, long userId, AiHubPersonalAgentResourceKind kind, String resourceId,
        String resourceName);

    /**
     * Removes the resource template row by id. The id is verified to belong to an agent owned by the caller before
     * the delete fires. Idempotent: removing an already-deleted (or non-existent) resource is a no-op.
     */
    void removeResource(long personalAgentResourceId, long workspaceId, long userId);
```

- [ ] **Step 2: Compile (expect failure — impl does not yet implement these)**

Run: `./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-api:compileJava`
Expected: BUILD SUCCESSFUL (the interface module compiles; the impl module in Task 8 is what currently breaks). If the impl module is compiled, it fails — that is expected and fixed in Task 8.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/personalagent/AiHubPersonalAgentService.java
git commit -m "732 Add resource methods to AiHubPersonalAgentService interface"
```

---

## Task 8: Implement the resource methods in `AiHubPersonalAgentServiceImpl`

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentServiceImpl.java`
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentServiceTest.java`

- [ ] **Step 1: Write the failing tests**

In `AiHubPersonalAgentServiceTest.java`:

1. Add a `@Mock` field next to the other repository mocks (after `aiHubPersonalAgentToolRepository`):

```java
    @Mock
    private com.bytechef.ee.automation.aihub.personalagent.repository.AiHubPersonalAgentResourceRepository
        aiHubPersonalAgentResourceRepository;
```

2. The test constructs the service in a single `newService()` helper at the bottom of the file. Update its `new AiHubPersonalAgentServiceImpl(...)` call to pass `aiHubPersonalAgentResourceRepository` as the **third** argument (after `aiHubPersonalAgentToolRepository`, before `workspaceAiHubPersonalAgentRepository`) — matching the constructor order set in Step 3:

```java
        return new AiHubPersonalAgentServiceImpl(
            aiHubPersonalAgentRepository, aiHubPersonalAgentToolRepository, aiHubPersonalAgentResourceRepository,
            workspaceAiHubPersonalAgentRepository, emptyValidatorProvider);
```

3. Add these test methods (mirroring the existing `addTool` / `removeTool` ownership + idempotency tests). Each test calls `newService()` to obtain the `AiHubPersonalAgentServiceImpl` instance — match the existing tests, which assign it to a local variable named `service`:

```java
    @Test
    void testAddResourceReturnsExistingRowWhenAlreadyPresent() {
        AiHubPersonalAgentServiceImpl service = newService();

        AiHubPersonalAgent agent = new AiHubPersonalAgent(USER_ID);
        agent.setId(5L);

        when(aiHubPersonalAgentRepository.findById(5L)).thenReturn(Optional.of(agent));
        when(workspaceAiHubPersonalAgentRepository
            .findByWorkspaceIdAndAiHubPersonalAgentId(WORKSPACE_ID, 5L))
                .thenReturn(Optional.of(new WorkspaceAiHubPersonalAgent(WORKSPACE_ID, 5L)));

        AiHubPersonalAgentResource existing = new AiHubPersonalAgentResource(
            5L, AiHubPersonalAgentResourceKind.WORKFLOW, "wf-1", "Daily standup");

        when(aiHubPersonalAgentResourceRepository.findByAiHubPersonalAgentIdAndKindAndResourceId(
            5L, AiHubPersonalAgentResourceKind.WORKFLOW.ordinal(), "wf-1"))
                .thenReturn(Optional.of(existing));

        AiHubPersonalAgentResource result = service.addResource(
            5L, WORKSPACE_ID, USER_ID, AiHubPersonalAgentResourceKind.WORKFLOW, "wf-1", "Daily standup");

        assertThat(result).isSameAs(existing);
        verify(aiHubPersonalAgentResourceRepository, never()).save(any());
    }

    @Test
    void testAddResourceRejectsCrossUserAgent() {
        AiHubPersonalAgentServiceImpl service = newService();

        AiHubPersonalAgent agent = new AiHubPersonalAgent(OTHER_USER_ID);
        agent.setId(5L);

        when(aiHubPersonalAgentRepository.findById(5L)).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> service.addResource(
            5L, WORKSPACE_ID, USER_ID, AiHubPersonalAgentResourceKind.FILE, "file-1", "notes.md"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(aiHubPersonalAgentResourceRepository, never()).save(any());
    }

    @Test
    void testRemoveResourceIsNoOpWhenResourceMissing() {
        AiHubPersonalAgentServiceImpl service = newService();

        when(aiHubPersonalAgentResourceRepository.findById(404L)).thenReturn(Optional.empty());

        service.removeResource(404L, WORKSPACE_ID, USER_ID);

        verify(aiHubPersonalAgentResourceRepository, never()).delete(any());
    }

    @Test
    void testRemoveResourceRejectsCrossUserAgent() {
        AiHubPersonalAgentServiceImpl service = newService();

        AiHubPersonalAgentResource resource = new AiHubPersonalAgentResource(
            5L, AiHubPersonalAgentResourceKind.FILE, "file-1", "notes.md");
        resource.setId(7L);

        AiHubPersonalAgent agent = new AiHubPersonalAgent(OTHER_USER_ID);
        agent.setId(5L);

        when(aiHubPersonalAgentResourceRepository.findById(7L)).thenReturn(Optional.of(resource));
        when(aiHubPersonalAgentRepository.findById(5L)).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> service.removeResource(7L, WORKSPACE_ID, USER_ID))
            .isInstanceOf(IllegalArgumentException.class);

        verify(aiHubPersonalAgentResourceRepository, never()).delete(any());
    }
```

> **Imports:** add `com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentResource`, `com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentResourceKind`, and `com.bytechef.ee.automation.aihub.personalagent.WorkspaceAiHubPersonalAgent` if not already imported.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "com.bytechef.ee.automation.aihub.personalagent.AiHubPersonalAgentServiceTest"`
Expected: FAIL — compilation error, `addResource` / `removeResource` not implemented and the constructor arity changed.

- [ ] **Step 3: Implement the methods**

In `AiHubPersonalAgentServiceImpl.java`:

1. Add the import `com.bytechef.ee.automation.aihub.personalagent.repository.AiHubPersonalAgentResourceRepository`, `com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentResource`, and `com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentResourceKind`.

2. Add the field after `aiHubPersonalAgentToolRepository`:

```java
    private final AiHubPersonalAgentResourceRepository aiHubPersonalAgentResourceRepository;
```

3. Add the constructor parameter as the **third** parameter (after `aiHubPersonalAgentToolRepository`, before `workspaceAiHubPersonalAgentRepository`) and assign it:

```java
        AiHubPersonalAgentResourceRepository aiHubPersonalAgentResourceRepository,
```
```java
        this.aiHubPersonalAgentResourceRepository = aiHubPersonalAgentResourceRepository;
```

4. Add the three methods after `updateToolConfig(...)` (before `cloneToEnvironment`):

```java
    @Override
    @Transactional(readOnly = true)
    public List<AiHubPersonalAgentResource> listResources(long agentId) {
        return aiHubPersonalAgentResourceRepository.findByAiHubPersonalAgentIdOrderByCreatedAtAsc(agentId);
    }

    @AuditAiHub(
        event = AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_RESOURCE_ADDED,
        data = {
            @AuditAiHub.AuditData(key = "workspaceId", value = "#workspaceId"),
            @AuditAiHub.AuditData(key = "agentId", value = "#agentId"),
            @AuditAiHub.AuditData(key = "kind", value = "#kind"),
            @AuditAiHub.AuditData(key = "resourceId", value = "#resourceId")
        })
    @Override
    @Transactional
    public AiHubPersonalAgentResource addResource(
        long agentId, long workspaceId, long userId, AiHubPersonalAgentResourceKind kind, String resourceId,
        String resourceName) {

        // Defense in depth: verify the agent belongs to (workspaceId, userId) before mutating its resource list.
        findOwned(agentId, workspaceId, userId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Personal agent " + agentId + " not found for workspace " + workspaceId + " user " + userId));

        if (resourceId == null || resourceId.isBlank()) {
            throw new IllegalArgumentException("resourceId must not be blank");
        }

        if (resourceName == null || resourceName.isBlank()) {
            throw new IllegalArgumentException("resourceName must not be blank");
        }

        // Pre-flight idempotency check so the caller gets a typed "already added" rather than a 500 from the DB
        // integrity exception. The unique constraint catches a TOCTOU race between this lookup and the save.
        Optional<AiHubPersonalAgentResource> existing = aiHubPersonalAgentResourceRepository
            .findByAiHubPersonalAgentIdAndKindAndResourceId(agentId, kind.ordinal(), resourceId);

        if (existing.isPresent()) {
            return existing.get();
        }

        AiHubPersonalAgentResource resource =
            new AiHubPersonalAgentResource(agentId, kind, resourceId, resourceName);

        resource.setCreatedAt(LocalDateTime.now(clock));

        try {
            return aiHubPersonalAgentResourceRepository.save(resource);
        } catch (DataIntegrityViolationException exception) {
            // TOCTOU race: a concurrent request inserted the same tuple. Re-fetch and return the existing row.
            return aiHubPersonalAgentResourceRepository
                .findByAiHubPersonalAgentIdAndKindAndResourceId(agentId, kind.ordinal(), resourceId)
                .orElseThrow(() -> new ConflictException(
                    "Personal agent resource conflict for agent " + agentId, exception));
        }
    }

    @AuditAiHub(
        event = AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_RESOURCE_REMOVED,
        data = {
            @AuditAiHub.AuditData(key = "workspaceId", value = "#workspaceId"),
            @AuditAiHub.AuditData(key = "personalAgentResourceId", value = "#personalAgentResourceId")
        })
    @Override
    @Transactional
    public void removeResource(long personalAgentResourceId, long workspaceId, long userId) {
        Optional<AiHubPersonalAgentResource> resourceOptional =
            aiHubPersonalAgentResourceRepository.findById(personalAgentResourceId);

        if (resourceOptional.isEmpty()) {
            // Idempotent: removing an already-deleted resource is a no-op rather than a 404.
            return;
        }

        AiHubPersonalAgentResource resource = resourceOptional.get();

        // Verify the parent agent belongs to (workspaceId, userId) before deleting — a forged id from the client
        // must not reach a resource row whose parent agent is owned by another user.
        findOwned(resource.getAiHubPersonalAgentId(), workspaceId, userId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Personal agent resource " + personalAgentResourceId + " not found for workspace " + workspaceId
                    + " user " + userId));

        aiHubPersonalAgentResourceRepository.delete(resource);
    }
```

> **Clone note:** `cloneToEnvironment` is intentionally **not** changed to clone resources. Resource ids are environment-specific (a DEV workflow / file / execution id does not resolve in PROD), so carrying them across an environment clone would produce dangling references — the same reasoning that makes `cloneToEnvironment` skip `connectionId` on tool clones. Leave `cloneToEnvironment` untouched.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "com.bytechef.ee.automation.aihub.personalagent.AiHubPersonalAgentServiceTest"`
Expected: PASS (all methods, including the pre-existing ones).

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentServiceImpl.java server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentServiceTest.java
git commit -m "732 Implement personal agent resource service methods"
```

---

## Task 9: Extend the GraphQL schema

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/resources/graphql/ai-hub-personal-agent.graphqls`

- [ ] **Step 1: Add the enum and type**

At the top of the file, before `type AiHubPersonalAgent {`, add:

```graphql
enum AiHubPersonalAgentResourceKind {
    WORKFLOW
    FILE
    DATA_TABLE
    KNOWLEDGE_BASE
    MCP_SERVER
    API_COLLECTION
    WORKFLOW_EXECUTION
    TASK
}

type AiHubPersonalAgentResource {
    id: ID!
    aiHubPersonalAgentId: Long!
    kind: AiHubPersonalAgentResourceKind!
    """
    The referenced entity's id (a workflow id, file id, …). String because composer resource ids are strings and
    workflow ids are non-numeric.
    """
    resourceId: String!
    resourceName: String!
    createdAt: Long
}
```

- [ ] **Step 2: Add the `resources` field to `AiHubPersonalAgent`**

Inside `type AiHubPersonalAgent { ... }`, add this field immediately after the `tools` field:

```graphql
    """
    The agent's resource template. Each entry is a (kind, resourceId, resourceName) reference. When a task is
    created against this agent, these are copied into ai_hub_task_artifact as *_REFERENCED artifacts.
    """
    resources: [AiHubPersonalAgentResource!]!
```

- [ ] **Step 3: Add the mutations**

Inside `extend type Mutation { ... }`, after `removeAiHubPersonalAgentTool`, add:

```graphql
    """
    Adds a (kind, resourceId) reference to the agent's resource template. Idempotent — adding the same resource
    twice returns the existing row. Future tasks spawned from this agent start with this resource attached.
    """
    addAiHubPersonalAgentResource(input: AddAiHubPersonalAgentResourceInput!): AiHubPersonalAgentResource!

    """
    Removes a resource from the agent's template by row id. Already-spawned tasks keep their copied artifact rows;
    only future tasks are affected. Idempotent — removing a non-existent id is a no-op.
    """
    removeAiHubPersonalAgentResource(workspaceId: ID!, id: ID!): Boolean!
```

- [ ] **Step 4: Add the input type**

After `input AddAiHubPersonalAgentToolInput { ... }`, add:

```graphql
input AddAiHubPersonalAgentResourceInput {
    workspaceId: ID!
    aiHubPersonalAgentId: ID!
    kind: AiHubPersonalAgentResourceKind!
    """
    The referenced entity's id (a workflow id, file id, …) — NOT the personal-agent-resource row id.
    """
    resourceId: String!
    resourceName: String!
}
```

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/resources/graphql/ai-hub-personal-agent.graphqls
git commit -m "732 Add personal agent resource GraphQL schema"
```

---

## Task 10: Extend `AiHubPersonalAgentGraphQlController`

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/java/com/bytechef/ee/automation/aihub/web/graphql/AiHubPersonalAgentGraphQlController.java`

- [ ] **Step 1: Add imports**

Add:

```java
import com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentResource;
import com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentResourceKind;
```

- [ ] **Step 2: Add the two mutations**

After the `removeAiHubPersonalAgentTool` method, add:

```java
    @MutationMapping
    public AiHubPersonalAgentResource
        addAiHubPersonalAgentResource(@Argument AddAiHubPersonalAgentResourceInput input) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, input.workspaceId());

        return aiHubPersonalAgentService.addResource(
            input.aiHubPersonalAgentId(), input.workspaceId(), userId, input.kind(), input.resourceId(),
            input.resourceName());
    }

    @MutationMapping
    public boolean removeAiHubPersonalAgentResource(@Argument long workspaceId, @Argument long id) {
        long userId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        aiHubPersonalAgentService.removeResource(id, workspaceId, userId);

        return true;
    }
```

- [ ] **Step 3: Add the `resources` schema-mapping resolver**

After the `tools(AiHubPersonalAgent agent)` resolver, add:

```java
    /**
     * Resolver for the {@code resources} field on {@code AiHubPersonalAgent}. Returns an empty list when the agent has
     * no resources rather than null, keeping the schema's non-null array contract intact.
     */
    @SchemaMapping(typeName = "AiHubPersonalAgent", field = "resources")
    public List<AiHubPersonalAgentResource> resources(AiHubPersonalAgent agent) {
        return aiHubPersonalAgentService.listResources(agent.getId());
    }
```

- [ ] **Step 4: Add the `createdAt` resolver for the resource type**

After the `toolCreatedAt(AiHubPersonalAgentTool tool)` resolver, add:

```java
    @SchemaMapping(typeName = "AiHubPersonalAgentResource", field = "createdAt")
    @Nullable
    public Long resourceCreatedAt(AiHubPersonalAgentResource resource) {
        return resource.getCreatedAt() == null ? null
            : resource.getCreatedAt()
                .toInstant(java.time.ZoneOffset.UTC)
                .toEpochMilli();
    }
```

- [ ] **Step 5: Add the input record**

After the `AddAiHubPersonalAgentToolInput` record, add:

```java
    public record AddAiHubPersonalAgentResourceInput(
        long workspaceId, long aiHubPersonalAgentId, AiHubPersonalAgentResourceKind kind, String resourceId,
        String resourceName) {
    }
```

- [ ] **Step 6: Compile**

Run: `./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-graphql:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/java/com/bytechef/ee/automation/aihub/web/graphql/AiHubPersonalAgentGraphQlController.java
git commit -m "732 Add personal agent resource GraphQL controller mutations"
```

---

## Task 11: Copy the agent's resources onto spawned tasks

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/task/AiHubTaskServiceImpl.java`

`createAiHubPersonalAgentChat` already calls `copyAgentToolTemplate` after saving the task. Add a parallel `copyAgentResourceTemplate` that records the agent's resources as `ai_hub_task_artifact` rows via `AiHubTaskArtifactService.recordReference` (idempotent on `(taskId, kind, artifactId)`).

The `AiHubTaskArtifactService` dependency is added as an `ObjectProvider` so a deployment without it degrades to a no-op, exactly like `aiHubPersonalAgentServiceProvider` and `taskToolFacadeProvider`. The full copy mirrors `copyAgentToolTemplate`'s resilience: ObjectProvider null-guards and per-row try/catch. `AiHubTaskServiceTest` uses `@InjectMocks`, which does not populate `ObjectProvider` fields — so the new provider is null in the unit test and the copy is a no-op there, exactly as `copyAgentToolTemplate` already behaves. The mapping logic (`toArtifactKind`) is the part that can break and is unit-tested in Task 2.

- [ ] **Step 1: Add imports**

Add:

```java
import com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentResource;
import com.bytechef.ee.platform.aihub.task.AiHubTaskArtifactService;
```

- [ ] **Step 2: Add the `ObjectProvider<AiHubTaskArtifactService>` field and constructor parameter**

Add the field after `toolSearchCatalogFeederProvider`:

```java
    private final ObjectProvider<AiHubTaskArtifactService> taskArtifactServiceProvider;
```

Add the constructor parameter as the **last** parameter of the constructor:

```java
        ObjectProvider<AiHubTaskArtifactService> taskArtifactServiceProvider) {
```

Add the assignment at the end of the constructor body:

```java
        // ObjectProvider so a deployment without the artifact service still gets a working AiHubTaskService —
        // the agent resource-template copy below is then a no-op, mirroring copyAgentToolTemplate.
        this.taskArtifactServiceProvider = taskArtifactServiceProvider;
```

- [ ] **Step 3: Call `copyAgentResourceTemplate` from `createAiHubPersonalAgentChat`**

In `createAiHubPersonalAgentChat`, the existing block is:

```java
        if (saved.getId() != null) {
            copyAgentToolTemplate(saved.getId(), aiHubPersonalAgentId, environment);
        }
```

Change it to:

```java
        if (saved.getId() != null) {
            copyAgentToolTemplate(saved.getId(), aiHubPersonalAgentId, environment);
            copyAgentResourceTemplate(saved.getId(), workspaceId, userId, aiHubPersonalAgentId);
        }
```

- [ ] **Step 4: Add the `copyAgentResourceTemplate` method**

Add this method immediately after `copyAgentToolTemplate(...)`:

```java
    /**
     * Copies the personal agent's resource template rows into {@code ai_hub_task_artifact} for the new task. Each
     * {@link AiHubPersonalAgentResource} becomes one {@code *_REFERENCED} artifact via
     * {@link AiHubTaskArtifactService#recordReference} (idempotent on {@code (taskId, kind, artifactId)}), so the
     * spawned task surfaces the agent's pre-declared resources in the right-panel artifact list — mirroring how
     * {@code copyAgentToolTemplate} seeds {@code ai_hub_task_tool}. Failures are logged and swallowed per-row so a
     * stale resource (e.g. a workflow that no longer exists) does not block task creation.
     */
    private void copyAgentResourceTemplate(
        long taskId, long workspaceId, long userId, long aiHubPersonalAgentId) {

        // Defensive against unit-test wiring where @InjectMocks doesn't supply ObjectProvider mocks. Production
        // wiring always provides them; a null provider means we silently skip the copy.
        if (aiHubPersonalAgentServiceProvider == null || taskArtifactServiceProvider == null) {
            return;
        }

        AiHubPersonalAgentService aiHubPersonalAgentService = aiHubPersonalAgentServiceProvider.getIfAvailable();
        AiHubTaskArtifactService taskArtifactService = taskArtifactServiceProvider.getIfAvailable();

        if (aiHubPersonalAgentService == null || taskArtifactService == null) {
            return;
        }

        List<AiHubPersonalAgentResource> templateResources;

        try {
            templateResources = aiHubPersonalAgentService.listResources(aiHubPersonalAgentId);
        } catch (RuntimeException exception) {
            logger.warn(
                "Failed to read resource template for personal agent {} on task {}; task continues with no "
                    + "attached resources.",
                aiHubPersonalAgentId, taskId, exception);

            return;
        }

        for (AiHubPersonalAgentResource templateResource : templateResources) {
            try {
                taskArtifactService.recordReference(
                    taskId, workspaceId, userId, templateResource.getKind()
                        .toArtifactKind(),
                    templateResource.getResourceId(), templateResource.getResourceName());
            } catch (RuntimeException exception) {
                // Per-resource failure is logged but doesn't abort the loop — partial-attach is preferable to a
                // fully empty task when one of the agent's resources references a stale entity.
                logger.warn(
                    "Failed to record template resource {}:{} to task {} (agent {}); skipping.",
                    templateResource.getKind(), templateResource.getResourceId(), taskId, aiHubPersonalAgentId,
                    exception);
            }
        }
    }
```

- [ ] **Step 5: Update `AiHubTaskServiceTest` constructor wiring**

`AiHubTaskServiceTest` uses `@InjectMocks` — Mockito injects mocks by type. The new `ObjectProvider<AiHubTaskArtifactService>` field will be left null (Mockito does not synthesize `ObjectProvider` mocks), which is the intended no-op path. No test change is required. Run the existing suite in the next step to confirm no regression.

- [ ] **Step 6: Run the task service tests to verify no regression**

Run: `./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "com.bytechef.ee.automation.aihub.task.AiHubTaskServiceTest"`
Expected: PASS — in particular `testCreateAiHubPersonalAgentChatPersistsNewRowWithUuidThreadId` and `testCreateAiHubPersonalAgentChatProducesDistinctRowsOnEveryCall` still pass (the resource copy is a no-op under `@InjectMocks`).

- [ ] **Step 7: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/task/AiHubTaskServiceImpl.java
git commit -m "732 Copy personal agent resources onto spawned tasks"
```

---

## Final verification

- [ ] **Step 1: Format**

Run: `./gradlew spotlessApply`

- [ ] **Step 2: Full check on the touched modules**

Run:
```bash
./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-api:check \
          :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:check \
          :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-graphql:check
```
Expected: BUILD SUCCESSFUL — Checkstyle, PMD, SpotBugs, and all tests pass.

- [ ] **Step 3: Commit any formatting changes**

```bash
git add -A
git commit -m "732 Apply spotless formatting"
```

(Skip this commit if `spotlessApply` produced no changes.)

---

## Follow-up: client plan

The client half is a separate plan, to be written after this server plan lands (the GraphQL schema must exist before `graphql-codegen` can regenerate `client/src/shared/middleware/graphql.ts`). It covers spec §5.5 and §6:

1. **GraphQL client operations + codegen** — `addAiHubPersonalAgentResource.graphql`, `removeAiHubPersonalAgentResource.graphql`, and the `resources { ... }` selection on `aiHubPersonalAgent.graphql`.
2. **`ResourcePickerMenu` extraction** — pull the composer's nested "Search resources…" dropdown into a shared component (the Explore-agent map of `AiHubComposer.tsx` is captured in the brainstorming notes).
3. **`AiHubPersonalAgentResourcesCard`** — replace `AiHubPersonalAgentToolsCard`; one "Add" button → 9-kind unified picker; create/edit parity with the pending-list pattern.
4. **Spec §6 — surfacing copied resources in the LLM context.** This needs a short investigation before it can be planned without placeholders: `aiHubComposerStore.referencedResources` is cleared after **every** message send (`AiHubRuntimeProvider.tsx:1673`), i.e. it is per-message state, not persistent task context. So "the agent's resources reach every turn" cannot be a one-time client seed — the mechanism is genuinely unresolved (re-seed before each send, vs. the agent runtime reading the copied `*_REFERENCED` artifacts directly). Resolve this with a focused `brainstorming` pass at the start of the client plan.
