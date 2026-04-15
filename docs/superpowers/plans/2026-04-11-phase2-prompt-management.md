# Phase 2: Prompt Management — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add version-controlled prompt management to the AI Gateway with environment-based deployment, variable substitution, and linkage to tracing spans. Prompts are resolved at gateway time via HTTP headers and served to the LLM with variables substituted from the request body.

**Architecture:** New domain entities (`AiObservabilityPrompt`, `AiObservabilityPromptVersion`) in the existing `automation-ai-gateway` module. Callers reference prompts via HTTP headers (`X-ByteChef-Prompt-Name`, `X-ByteChef-Prompt-Environment`). The `AiGatewayFacade` resolves the active prompt version, substitutes `{{variable_name}}` placeholders, and records `prompt_id`/`prompt_version_id` on the span. GraphQL exposes CRUD operations. A new "Prompts" sidebar tab in the client provides list, detail, version editor, and environment controls.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JDBC, PostgreSQL, Liquibase, GraphQL (Spring GraphQL), React 19, TypeScript 5.9, TanStack Query, Tailwind CSS

**Reference spec:** `docs/superpowers/specs/2026-04-11-ai-gateway-observability-platform-design.md` — Phase 2 section

**Depends on:** Phase 1 (Tracing & Sessions) — the `ai_observability_span` table with `prompt_id` and `prompt_version_id` columns must already exist.

---

## File Map

### Server — API module (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `src/main/java/.../domain/AiObservabilityPrompt.java` | Prompt domain entity |
| Create | `src/main/java/.../domain/AiObservabilityPromptVersion.java` | Prompt version domain entity |
| Create | `src/main/java/.../domain/AiObservabilityPromptVersionType.java` | Enum: TEXT, CHAT |
| Create | `src/main/java/.../repository/AiObservabilityPromptRepository.java` | Prompt repository |
| Create | `src/main/java/.../repository/AiObservabilityPromptVersionRepository.java` | Prompt version repository |
| Create | `src/main/java/.../service/AiObservabilityPromptService.java` | Prompt service interface |
| Create | `src/main/java/.../service/AiObservabilityPromptVersionService.java` | Prompt version service interface |
| Create | `src/main/java/.../dto/AiObservabilityPromptHeaders.java` | DTO for parsed prompt headers |

### Server — Service module (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/`)

| Action | Path | Purpose |
|---|---|---|
| Modify | `src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000002_ai_observability_init.xml` | Add prompt tables to existing migration |
| Create | `src/main/java/.../service/AiObservabilityPromptServiceImpl.java` | Prompt service impl |
| Create | `src/main/java/.../service/AiObservabilityPromptVersionServiceImpl.java` | Prompt version service impl |
| Modify | `src/main/java/.../facade/AiGatewayFacade.java` | Add prompt resolution and variable substitution |

### Server — GraphQL module (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `src/main/resources/graphql/ai-observability-prompt.graphqls` | Prompt GraphQL schema |
| Create | `src/main/java/.../web/graphql/AiObservabilityPromptGraphQlController.java` | Prompt queries and mutations |

### Server — Public REST module (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-public-rest/`)

| Action | Path | Purpose |
|---|---|---|
| Modify | `src/main/java/.../public_/web/rest/AiGatewayChatCompletionApiController.java` | Extract prompt headers, pass to facade |

### Client (`client/src/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `graphql/automation/ai-gateway/aiObservabilityPrompts.graphql` | Prompt queries/mutations |
| Modify | `pages/automation/ai-gateway/types.ts` | Add prompt/version types |
| Modify | `pages/automation/ai-gateway/AiGateway.tsx` | Add Prompts sidebar tab |
| Create | `pages/automation/ai-gateway/components/prompts/AiObservabilityPrompts.tsx` | Prompts list page |
| Create | `pages/automation/ai-gateway/components/prompts/AiObservabilityPromptDetail.tsx` | Prompt detail with version history |
| Create | `pages/automation/ai-gateway/components/prompts/AiObservabilityPromptDialog.tsx` | Create/edit prompt dialog |
| Create | `pages/automation/ai-gateway/components/prompts/AiObservabilityPromptVersionDialog.tsx` | Create new version dialog |

---

## Task 1: Liquibase Migration — Add Prompt Tables

**Files:**
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000002_ai_observability_init.xml`

- [ ] **Step 1: Add a new changeset for prompt tables**

Append a new `<changeSet>` after the existing Phase 1 changeset (id `00000000000002`) inside the same file. Use changeset id `00000000000003`.

```xml
    <changeSet id="00000000000003" author="Ivica Cardic">
        <preConditions onFail="MARK_RAN">
            <not>
                <tableExists tableName="ai_observability_prompt"/>
            </not>
        </preConditions>

        <createTable tableName="ai_observability_prompt">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="workspace_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="project_id" type="BIGINT"/>
            <column name="name" type="VARCHAR(256)">
                <constraints nullable="false"/>
            </column>
            <column name="description" type="TEXT"/>
            <column name="created_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="last_modified_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="version" type="BIGINT">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addUniqueConstraint tableName="ai_observability_prompt"
                             columnNames="workspace_id, project_id, name"
                             constraintName="uq_ai_obs_prompt_workspace_project_name"/>

        <createIndex tableName="ai_observability_prompt" indexName="idx_ai_obs_prompt_workspace">
            <column name="workspace_id"/>
        </createIndex>

        <createIndex tableName="ai_observability_prompt" indexName="idx_ai_obs_prompt_project">
            <column name="project_id"/>
        </createIndex>

        <createTable tableName="ai_observability_prompt_version">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="prompt_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="version_number" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="type" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="content" type="TEXT">
                <constraints nullable="false"/>
            </column>
            <column name="variables" type="TEXT"/>
            <column name="environment" type="VARCHAR(64)"/>
            <column name="commit_message" type="VARCHAR(512)"/>
            <column name="active" type="BOOLEAN">
                <constraints nullable="false"/>
            </column>
            <column name="created_by" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="created_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addForeignKeyConstraint constraintName="fk_ai_obs_prompt_version_prompt"
                                 baseTableName="ai_observability_prompt_version" baseColumnNames="prompt_id"
                                 referencedTableName="ai_observability_prompt" referencedColumnNames="id"/>

        <addUniqueConstraint tableName="ai_observability_prompt_version"
                             columnNames="prompt_id, version_number"
                             constraintName="uq_ai_obs_prompt_version_number"/>

        <createIndex tableName="ai_observability_prompt_version" indexName="idx_ai_obs_prompt_version_prompt">
            <column name="prompt_id"/>
        </createIndex>

        <createIndex tableName="ai_observability_prompt_version" indexName="idx_ai_obs_prompt_version_env">
            <column name="environment"/>
        </createIndex>

        <createIndex tableName="ai_observability_prompt_version" indexName="idx_ai_obs_prompt_version_active">
            <column name="active"/>
        </createIndex>

        <addForeignKeyConstraint constraintName="fk_ai_obs_span_prompt"
                                 baseTableName="ai_observability_span" baseColumnNames="prompt_id"
                                 referencedTableName="ai_observability_prompt" referencedColumnNames="id"/>

        <addForeignKeyConstraint constraintName="fk_ai_obs_span_prompt_version"
                                 baseTableName="ai_observability_span" baseColumnNames="prompt_version_id"
                                 referencedTableName="ai_observability_prompt_version" referencedColumnNames="id"/>
    </changeSet>
```

This changeset must be inserted before the closing `</databaseChangeLog>` tag.

- [ ] **Step 2: Verify the migration compiles**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000002_ai_observability_init.xml
git commit -m "732 Add Liquibase migration for prompt management tables"
```

---

## Task 2: Prompt Version Type Enum

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityPromptVersionType.java`

- [ ] **Step 1: Create AiObservabilityPromptVersionType**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

/**
 * @version ee
 */
public enum AiObservabilityPromptVersionType {

    TEXT,
    CHAT
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityPromptVersionType.java
git commit -m "732 Add AiObservabilityPromptVersionType enum (TEXT, CHAT)"
```

---

## Task 3: Prompt Domain Entity

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityPrompt.java`

- [ ] **Step 1: Create AiObservabilityPrompt**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_observability_prompt")
public class AiObservabilityPrompt {

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private String description;

    @Id
    private Long id;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column
    private String name;

    @Column("project_id")
    private Long projectId;

    @Version
    private int version;

    @Column("workspace_id")
    private Long workspaceId;

    private AiObservabilityPrompt() {
    }

    public AiObservabilityPrompt(Long workspaceId, String name) {
        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notBlank(name, "name must not be blank");

        this.name = name;
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilityPrompt aiObservabilityPrompt)) {
            return false;
        }

        return Objects.equals(id, aiObservabilityPrompt.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getDescription() {
        return description;
    }

    public Long getId() {
        return id;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getName() {
        return name;
    }

    public Long getProjectId() {
        return projectId;
    }

    public int getVersion() {
        return version;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setName(String name) {
        Validate.notBlank(name, "name must not be blank");

        this.name = name;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    @Override
    public String toString() {
        return "AiObservabilityPrompt{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", name='" + name + '\'' +
            ", createdDate=" + createdDate +
            '}';
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityPrompt.java
git commit -m "732 Add AiObservabilityPrompt domain entity"
```

---

## Task 4: Prompt Version Domain Entity

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityPromptVersion.java`

- [ ] **Step 1: Create AiObservabilityPromptVersion**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_observability_prompt_version")
public class AiObservabilityPromptVersion {

    @Column
    private boolean active;

    @Column("commit_message")
    private String commitMessage;

    @Column
    private String content;

    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private String environment;

    @Id
    private Long id;

    @Column("prompt_id")
    private Long promptId;

    @Column
    private int type;

    @Column
    private String variables;

    @Column("version_number")
    private int versionNumber;

    private AiObservabilityPromptVersion() {
    }

    public AiObservabilityPromptVersion(
        Long promptId, int versionNumber, AiObservabilityPromptVersionType type, String content, String createdBy) {

        Validate.notNull(promptId, "promptId must not be null");
        Validate.notBlank(content, "content must not be blank");
        Validate.notBlank(createdBy, "createdBy must not be blank");

        this.content = content;
        this.createdBy = createdBy;
        this.promptId = promptId;
        this.type = type.ordinal();
        this.versionNumber = versionNumber;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilityPromptVersion aiObservabilityPromptVersion)) {
            return false;
        }

        return Objects.equals(id, aiObservabilityPromptVersion.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public boolean isActive() {
        return active;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public String getContent() {
        return content;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getEnvironment() {
        return environment;
    }

    public Long getId() {
        return id;
    }

    public Long getPromptId() {
        return promptId;
    }

    public AiObservabilityPromptVersionType getType() {
        return AiObservabilityPromptVersionType.values()[type];
    }

    public String getVariables() {
        return variables;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    public void setContent(String content) {
        Validate.notBlank(content, "content must not be blank");

        this.content = content;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public void setVariables(String variables) {
        this.variables = variables;
    }

    @Override
    public String toString() {
        return "AiObservabilityPromptVersion{" +
            "id=" + id +
            ", promptId=" + promptId +
            ", versionNumber=" + versionNumber +
            ", type=" + getType() +
            ", environment='" + environment + '\'' +
            ", active=" + active +
            ", createdBy='" + createdBy + '\'' +
            '}';
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityPromptVersion.java
git commit -m "732 Add AiObservabilityPromptVersion domain entity"
```

---

## Task 5: Repository Interfaces

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityPromptRepository.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityPromptVersionRepository.java`

- [ ] **Step 1: Create AiObservabilityPromptRepository**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityPrompt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiObservabilityPromptRepository extends ListCrudRepository<AiObservabilityPrompt, Long> {

    List<AiObservabilityPrompt> findAllByWorkspaceId(Long workspaceId);

    Optional<AiObservabilityPrompt> findByWorkspaceIdAndProjectIdAndName(
        Long workspaceId, Long projectId, String name);
}
```

- [ ] **Step 2: Create AiObservabilityPromptVersionRepository**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityPromptVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiObservabilityPromptVersionRepository
    extends ListCrudRepository<AiObservabilityPromptVersion, Long> {

    List<AiObservabilityPromptVersion> findAllByPromptId(Long promptId);

    List<AiObservabilityPromptVersion> findAllByPromptIdOrderByVersionNumberDesc(Long promptId);

    Optional<AiObservabilityPromptVersion> findByPromptIdAndActiveAndEnvironment(
        Long promptId, boolean active, String environment);

    Optional<AiObservabilityPromptVersion> findTopByPromptIdOrderByVersionNumberDesc(Long promptId);

    List<AiObservabilityPromptVersion> findAllByPromptIdAndEnvironmentAndActive(
        Long promptId, String environment, boolean active);
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityPromptRepository.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityPromptVersionRepository.java
git commit -m "732 Add prompt and prompt version repository interfaces"
```

---

## Task 6: Service Interfaces

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityPromptService.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityPromptVersionService.java`

- [ ] **Step 1: Create AiObservabilityPromptService**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityPrompt;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 */
public interface AiObservabilityPromptService {

    AiObservabilityPrompt create(AiObservabilityPrompt prompt);

    void delete(long id);

    AiObservabilityPrompt getPrompt(long id);

    List<AiObservabilityPrompt> getPromptsByWorkspace(Long workspaceId);

    Optional<AiObservabilityPrompt> getPromptByName(Long workspaceId, Long projectId, String name);

    AiObservabilityPrompt update(AiObservabilityPrompt prompt);
}
```

- [ ] **Step 2: Create AiObservabilityPromptVersionService**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityPromptVersion;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 */
public interface AiObservabilityPromptVersionService {

    AiObservabilityPromptVersion create(AiObservabilityPromptVersion promptVersion);

    List<AiObservabilityPromptVersion> getVersionsByPrompt(Long promptId);

    Optional<AiObservabilityPromptVersion> getActiveVersion(Long promptId, String environment);

    int getNextVersionNumber(Long promptId);

    void setActiveVersion(long promptVersionId, String environment);

    AiObservabilityPromptVersion update(AiObservabilityPromptVersion promptVersion);
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityPromptService.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityPromptVersionService.java
git commit -m "732 Add prompt and prompt version service interfaces"
```

---

## Task 7: Service Implementations

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityPromptServiceImpl.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityPromptVersionServiceImpl.java`

- [ ] **Step 1: Create AiObservabilityPromptServiceImpl**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityPrompt;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilityPromptRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class AiObservabilityPromptServiceImpl implements AiObservabilityPromptService {

    private final AiObservabilityPromptRepository aiObservabilityPromptRepository;

    public AiObservabilityPromptServiceImpl(
        AiObservabilityPromptRepository aiObservabilityPromptRepository) {

        this.aiObservabilityPromptRepository = aiObservabilityPromptRepository;
    }

    @Override
    public AiObservabilityPrompt create(AiObservabilityPrompt prompt) {
        Validate.notNull(prompt, "prompt must not be null");
        Validate.isTrue(prompt.getId() == null, "prompt id must be null for creation");

        return aiObservabilityPromptRepository.save(prompt);
    }

    @Override
    public void delete(long id) {
        aiObservabilityPromptRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiObservabilityPrompt getPrompt(long id) {
        return aiObservabilityPromptRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiObservabilityPrompt not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityPrompt> getPromptsByWorkspace(Long workspaceId) {
        return aiObservabilityPromptRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiObservabilityPrompt> getPromptByName(Long workspaceId, Long projectId, String name) {
        return aiObservabilityPromptRepository.findByWorkspaceIdAndProjectIdAndName(workspaceId, projectId, name);
    }

    @Override
    public AiObservabilityPrompt update(AiObservabilityPrompt prompt) {
        Validate.notNull(prompt, "prompt must not be null");
        Validate.notNull(prompt.getId(), "prompt id must not be null for update");

        return aiObservabilityPromptRepository.save(prompt);
    }
}
```

- [ ] **Step 2: Create AiObservabilityPromptVersionServiceImpl**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityPromptVersion;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilityPromptVersionRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class AiObservabilityPromptVersionServiceImpl implements AiObservabilityPromptVersionService {

    private final AiObservabilityPromptVersionRepository aiObservabilityPromptVersionRepository;

    public AiObservabilityPromptVersionServiceImpl(
        AiObservabilityPromptVersionRepository aiObservabilityPromptVersionRepository) {

        this.aiObservabilityPromptVersionRepository = aiObservabilityPromptVersionRepository;
    }

    @Override
    public AiObservabilityPromptVersion create(AiObservabilityPromptVersion promptVersion) {
        Validate.notNull(promptVersion, "promptVersion must not be null");
        Validate.isTrue(promptVersion.getId() == null, "promptVersion id must be null for creation");

        return aiObservabilityPromptVersionRepository.save(promptVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityPromptVersion> getVersionsByPrompt(Long promptId) {
        return aiObservabilityPromptVersionRepository.findAllByPromptIdOrderByVersionNumberDesc(promptId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiObservabilityPromptVersion> getActiveVersion(Long promptId, String environment) {
        return aiObservabilityPromptVersionRepository.findByPromptIdAndActiveAndEnvironment(
            promptId, true, environment);
    }

    @Override
    @Transactional(readOnly = true)
    public int getNextVersionNumber(Long promptId) {
        Optional<AiObservabilityPromptVersion> latestVersion =
            aiObservabilityPromptVersionRepository.findTopByPromptIdOrderByVersionNumberDesc(promptId);

        return latestVersion.map(version -> version.getVersionNumber() + 1).orElse(1);
    }

    @Override
    public void setActiveVersion(long promptVersionId, String environment) {
        AiObservabilityPromptVersion targetVersion = aiObservabilityPromptVersionRepository.findById(promptVersionId)
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "AiObservabilityPromptVersion not found with id: " + promptVersionId));

        List<AiObservabilityPromptVersion> currentlyActiveVersions =
            aiObservabilityPromptVersionRepository.findAllByPromptIdAndEnvironmentAndActive(
                targetVersion.getPromptId(), environment, true);

        for (AiObservabilityPromptVersion activeVersion : currentlyActiveVersions) {
            activeVersion.setActive(false);

            aiObservabilityPromptVersionRepository.save(activeVersion);
        }

        targetVersion.setActive(true);
        targetVersion.setEnvironment(environment);

        aiObservabilityPromptVersionRepository.save(targetVersion);
    }

    @Override
    public AiObservabilityPromptVersion update(AiObservabilityPromptVersion promptVersion) {
        Validate.notNull(promptVersion, "promptVersion must not be null");
        Validate.notNull(promptVersion.getId(), "promptVersion id must not be null for update");

        return aiObservabilityPromptVersionRepository.save(promptVersion);
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityPromptServiceImpl.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityPromptVersionServiceImpl.java
git commit -m "732 Add prompt and prompt version service implementations"
```

---

## Task 8: Prompt Headers DTO and Facade Integration

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/dto/AiObservabilityPromptHeaders.java`
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/facade/AiGatewayFacade.java`
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-public-rest/src/main/java/com/bytechef/ee/automation/ai/gateway/public_/web/rest/AiGatewayChatCompletionApiController.java`

- [ ] **Step 1: Create AiObservabilityPromptHeaders DTO**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.dto;

import jakarta.annotation.Nullable;

/**
 * Parsed prompt headers from an incoming gateway request.
 *
 * @version ee
 */
public record AiObservabilityPromptHeaders(
    @Nullable String promptName,
    @Nullable String environment) {

    public static final String HEADER_PROMPT_NAME = "X-ByteChef-Prompt-Name";
    public static final String HEADER_PROMPT_ENVIRONMENT = "X-ByteChef-Prompt-Environment";

    public static final String DEFAULT_ENVIRONMENT = "production";

    public boolean hasPromptEnabled() {
        return promptName != null;
    }

    public String resolvedEnvironment() {
        return environment != null ? environment : DEFAULT_ENVIRONMENT;
    }
}
```

- [ ] **Step 2: Modify AiGatewayChatCompletionApiController to extract prompt headers**

In `AiGatewayChatCompletionApiController.java`, add a method to extract prompt headers from the HTTP request. Add it alongside the existing `extractTracingHeaders` method (created in Phase 1):

```java
// Add import
import com.bytechef.ee.automation.ai.gateway.dto.AiObservabilityPromptHeaders;

// Add private method to extract prompt headers:
private AiObservabilityPromptHeaders extractPromptHeaders(HttpServletRequest httpServletRequest) {
    return new AiObservabilityPromptHeaders(
        httpServletRequest.getHeader(AiObservabilityPromptHeaders.HEADER_PROMPT_NAME),
        httpServletRequest.getHeader(AiObservabilityPromptHeaders.HEADER_PROMPT_ENVIRONMENT));
}
```

Update the `chatCompletions` and `chatCompletionsStream` method calls to pass `extractPromptHeaders(httpServletRequest)` to the facade alongside the existing tracing headers.

- [ ] **Step 3: Modify AiGatewayFacade to resolve prompts and substitute variables**

In `AiGatewayFacade.java`, inject the two new services:

```java
// Add fields
private final AiObservabilityPromptService aiObservabilityPromptService;
private final AiObservabilityPromptVersionService aiObservabilityPromptVersionService;
```

Add them to the constructor.

Add a constant and a private record:

```java
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityPrompt;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityPromptVersion;
import com.bytechef.ee.automation.ai.gateway.dto.AiObservabilityPromptHeaders;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityPromptService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityPromptVersionService;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

private record ResolvedPrompt(Long promptId, Long promptVersionId, String content) {
}
```

Add a method to resolve the prompt and substitute variables:

```java
/**
 * Resolves a prompt by name and environment, substitutes variables from the request body,
 * and returns the resolved content along with the prompt and version IDs for span linkage.
 */
private ResolvedPrompt resolvePrompt(
    AiObservabilityPromptHeaders promptHeaders, Long workspaceId, Long projectId,
    Map<String, Object> requestVariables) {

    if (!promptHeaders.hasPromptEnabled()) {
        return null;
    }

    Optional<AiObservabilityPrompt> promptOptional = aiObservabilityPromptService.getPromptByName(
        workspaceId, projectId, promptHeaders.promptName());

    if (promptOptional.isEmpty()) {
        throw new IllegalArgumentException(
            "Prompt not found: " + promptHeaders.promptName());
    }

    AiObservabilityPrompt prompt = promptOptional.get();

    String environment = promptHeaders.resolvedEnvironment();

    Optional<AiObservabilityPromptVersion> activeVersionOptional =
        aiObservabilityPromptVersionService.getActiveVersion(prompt.getId(), environment);

    if (activeVersionOptional.isEmpty()) {
        throw new IllegalArgumentException(
            "No active prompt version found for prompt '" + promptHeaders.promptName() +
                "' in environment '" + environment + "'");
    }

    AiObservabilityPromptVersion activeVersion = activeVersionOptional.get();

    String resolvedContent = substituteVariables(activeVersion.getContent(), requestVariables);

    return new ResolvedPrompt(prompt.getId(), activeVersion.getId(), resolvedContent);
}

private String substituteVariables(String content, Map<String, Object> variables) {
    if (variables == null || variables.isEmpty()) {
        return content;
    }

    Matcher matcher = VARIABLE_PATTERN.matcher(content);
    StringBuilder result = new StringBuilder();

    while (matcher.find()) {
        String variableName = matcher.group(1);
        Object value = variables.get(variableName);
        String replacement = value != null ? Matcher.quoteReplacement(value.toString()) : matcher.group(0);

        matcher.appendReplacement(result, replacement);
    }

    matcher.appendTail(result);

    return result.toString();
}
```

In the existing `chatCompletion` method flow, call `resolvePrompt()` before sending the request to the LLM. If a resolved prompt is returned, override the request messages with the resolved content. When creating the span (in `processTracingHeaders` from Phase 1), set `span.setPromptId(resolvedPrompt.promptId())` and `span.setPromptVersionId(resolvedPrompt.promptVersionId())`.

Update the `chatCompletion` method signature to accept `AiObservabilityPromptHeaders` as an additional parameter.

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava && ./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-public-rest:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/dto/AiObservabilityPromptHeaders.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/facade/AiGatewayFacade.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-public-rest/src/main/java/com/bytechef/ee/automation/ai/gateway/public_/web/rest/AiGatewayChatCompletionApiController.java
git commit -m "732 Integrate prompt resolution and variable substitution into gateway pipeline"
```

---

## Task 9: GraphQL Schema for Prompts

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-prompt.graphqls`

- [ ] **Step 1: Create ai-observability-prompt.graphqls**

```graphql
extend type Query {
    aiObservabilityPrompt(id: ID!): AiObservabilityPrompt
    aiObservabilityPrompts(workspaceId: ID!): [AiObservabilityPrompt]
    aiObservabilityPromptVersions(promptId: ID!): [AiObservabilityPromptVersion]
}

extend type Mutation {
    createAiObservabilityPrompt(input: CreateAiObservabilityPromptInput!): AiObservabilityPrompt
    createAiObservabilityPromptVersion(input: CreateAiObservabilityPromptVersionInput!): AiObservabilityPromptVersion
    deleteAiObservabilityPrompt(id: ID!): Boolean
    setActivePromptVersion(promptVersionId: ID!, environment: String!): Boolean
    updateAiObservabilityPrompt(id: ID!, input: UpdateAiObservabilityPromptInput!): AiObservabilityPrompt
}

type AiObservabilityPrompt {
    createdDate: Long
    description: String
    id: ID!
    lastModifiedDate: Long
    name: String!
    projectId: ID
    version: Int
    versions: [AiObservabilityPromptVersion]
    workspaceId: ID!
}

type AiObservabilityPromptVersion {
    active: Boolean!
    commitMessage: String
    content: String!
    createdBy: String!
    createdDate: Long
    environment: String
    id: ID!
    promptId: ID!
    type: AiObservabilityPromptVersionType!
    variables: String
    versionNumber: Int!
}

enum AiObservabilityPromptVersionType {
    CHAT
    TEXT
}

input CreateAiObservabilityPromptInput {
    description: String
    name: String!
    projectId: ID
    workspaceId: ID!
}

input CreateAiObservabilityPromptVersionInput {
    active: Boolean
    commitMessage: String
    content: String!
    environment: String
    promptId: ID!
    type: AiObservabilityPromptVersionType!
    variables: String
}

input UpdateAiObservabilityPromptInput {
    description: String
    name: String
}
```

- [ ] **Step 2: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-prompt.graphqls
git commit -m "732 Add GraphQL schema for observability prompts and prompt versions"
```

---

## Task 10: GraphQL Controller

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiObservabilityPromptGraphQlController.java`

- [ ] **Step 1: Create AiObservabilityPromptGraphQlController**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityPrompt;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityPromptVersion;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityPromptVersionType;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityPromptService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityPromptVersionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiObservabilityPromptGraphQlController {

    private final AiObservabilityPromptService aiObservabilityPromptService;
    private final AiObservabilityPromptVersionService aiObservabilityPromptVersionService;

    @SuppressFBWarnings("EI")
    AiObservabilityPromptGraphQlController(
        AiObservabilityPromptService aiObservabilityPromptService,
        AiObservabilityPromptVersionService aiObservabilityPromptVersionService) {

        this.aiObservabilityPromptService = aiObservabilityPromptService;
        this.aiObservabilityPromptVersionService = aiObservabilityPromptVersionService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityPrompt aiObservabilityPrompt(@Argument long id) {
        return aiObservabilityPromptService.getPrompt(id);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiObservabilityPrompt> aiObservabilityPrompts(@Argument Long workspaceId) {
        return aiObservabilityPromptService.getPromptsByWorkspace(workspaceId);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiObservabilityPromptVersion> aiObservabilityPromptVersions(@Argument Long promptId) {
        return aiObservabilityPromptVersionService.getVersionsByPrompt(promptId);
    }

    @SchemaMapping(typeName = "AiObservabilityPrompt", field = "versions")
    public List<AiObservabilityPromptVersion> versions(AiObservabilityPrompt prompt) {
        return aiObservabilityPromptVersionService.getVersionsByPrompt(prompt.getId());
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityPrompt createAiObservabilityPrompt(@Argument CreateAiObservabilityPromptInput input) {
        AiObservabilityPrompt prompt = new AiObservabilityPrompt(
            Long.parseLong(input.workspaceId()), input.name());

        if (input.description() != null) {
            prompt.setDescription(input.description());
        }

        if (input.projectId() != null) {
            prompt.setProjectId(Long.parseLong(input.projectId()));
        }

        return aiObservabilityPromptService.create(prompt);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityPromptVersion createAiObservabilityPromptVersion(
        @Argument CreateAiObservabilityPromptVersionInput input) {

        Long promptId = Long.parseLong(input.promptId());

        int nextVersionNumber = aiObservabilityPromptVersionService.getNextVersionNumber(promptId);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String createdBy = authentication != null ? authentication.getName() : "system";

        AiObservabilityPromptVersionType versionType =
            AiObservabilityPromptVersionType.valueOf(input.type());

        AiObservabilityPromptVersion promptVersion = new AiObservabilityPromptVersion(
            promptId, nextVersionNumber, versionType, input.content(), createdBy);

        if (input.commitMessage() != null) {
            promptVersion.setCommitMessage(input.commitMessage());
        }

        if (input.environment() != null) {
            promptVersion.setEnvironment(input.environment());
        }

        if (input.variables() != null) {
            promptVersion.setVariables(input.variables());
        }

        if (input.active() != null && input.active()) {
            promptVersion.setActive(true);
        }

        return aiObservabilityPromptVersionService.create(promptVersion);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean deleteAiObservabilityPrompt(@Argument long id) {
        aiObservabilityPromptService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean setActivePromptVersion(@Argument long promptVersionId, @Argument String environment) {
        aiObservabilityPromptVersionService.setActiveVersion(promptVersionId, environment);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityPrompt updateAiObservabilityPrompt(
        @Argument long id, @Argument UpdateAiObservabilityPromptInput input) {

        AiObservabilityPrompt prompt = aiObservabilityPromptService.getPrompt(id);

        if (input.description() != null) {
            prompt.setDescription(input.description());
        }

        if (input.name() != null) {
            prompt.setName(input.name());
        }

        return aiObservabilityPromptService.update(prompt);
    }

    @SuppressFBWarnings("EI")
    public record CreateAiObservabilityPromptInput(
        String description, String name, String projectId, String workspaceId) {
    }

    @SuppressFBWarnings("EI")
    public record CreateAiObservabilityPromptVersionInput(
        Boolean active, String commitMessage, String content, String environment, String promptId,
        String type, String variables) {
    }

    @SuppressFBWarnings("EI")
    public record UpdateAiObservabilityPromptInput(String description, String name) {
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-graphql:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiObservabilityPromptGraphQlController.java
git commit -m "732 Add GraphQL controller for prompt management CRUD operations"
```

---

## Task 11: Client — GraphQL Operations and Codegen

**Files:**
- Create: `client/src/graphql/automation/ai-gateway/aiObservabilityPrompts.graphql`

- [ ] **Step 1: Create aiObservabilityPrompts.graphql**

```graphql
query aiObservabilityPrompts($workspaceId: ID!) {
    aiObservabilityPrompts(workspaceId: $workspaceId) {
        createdDate
        description
        id
        lastModifiedDate
        name
        projectId
        version
        workspaceId
    }
}

query aiObservabilityPrompt($id: ID!) {
    aiObservabilityPrompt(id: $id) {
        createdDate
        description
        id
        lastModifiedDate
        name
        projectId
        version
        versions {
            active
            commitMessage
            content
            createdBy
            createdDate
            environment
            id
            promptId
            type
            variables
            versionNumber
        }
        workspaceId
    }
}

query aiObservabilityPromptVersions($promptId: ID!) {
    aiObservabilityPromptVersions(promptId: $promptId) {
        active
        commitMessage
        content
        createdBy
        createdDate
        environment
        id
        promptId
        type
        variables
        versionNumber
    }
}

mutation createAiObservabilityPrompt($input: CreateAiObservabilityPromptInput!) {
    createAiObservabilityPrompt(input: $input) {
        createdDate
        description
        id
        name
        projectId
        workspaceId
    }
}

mutation createAiObservabilityPromptVersion($input: CreateAiObservabilityPromptVersionInput!) {
    createAiObservabilityPromptVersion(input: $input) {
        active
        commitMessage
        content
        createdBy
        createdDate
        environment
        id
        promptId
        type
        variables
        versionNumber
    }
}

mutation deleteAiObservabilityPrompt($id: ID!) {
    deleteAiObservabilityPrompt(id: $id)
}

mutation setActivePromptVersion($promptVersionId: ID!, $environment: String!) {
    setActivePromptVersion(promptVersionId: $promptVersionId, environment: $environment)
}

mutation updateAiObservabilityPrompt($id: ID!, $input: UpdateAiObservabilityPromptInput!) {
    updateAiObservabilityPrompt(id: $id, input: $input) {
        description
        id
        name
    }
}
```

- [ ] **Step 2: Run GraphQL codegen**

Run: `cd client && npx graphql-codegen`
Expected: generates updated `src/shared/middleware/graphql.ts` with new query/mutation hooks

- [ ] **Step 3: Commit**

```bash
cd client
git add src/graphql/automation/ai-gateway/aiObservabilityPrompts.graphql \
  src/shared/middleware/graphql.ts
git commit -m "732 client - Add GraphQL operations and codegen for prompt management"
```

---

## Task 12: Client — Types and Sidebar Update

**Files:**
- Modify: `client/src/pages/automation/ai-gateway/types.ts`
- Modify: `client/src/pages/automation/ai-gateway/AiGateway.tsx`

- [ ] **Step 1: Add types to types.ts**

Add the following imports and types to the existing `types.ts`. Keep all existing imports and types unchanged. Add new imports sorted alphabetically within the existing import block, and add new types below existing types:

```typescript
import {
    AiGatewayProjectsQuery,
    AiObservabilityPromptQuery,
    AiObservabilityPromptsQuery,
    AiObservabilityPromptVersionsQuery,
    WorkspaceAiGatewayModelsQuery,
    WorkspaceAiGatewayProvidersQuery,
    WorkspaceAiGatewayRoutingPoliciesQuery,
} from '@/shared/middleware/graphql';

// ... existing types unchanged ...

export type AiObservabilityPromptType = NonNullable<
    NonNullable<AiObservabilityPromptsQuery['aiObservabilityPrompts']>[number]
>;

export type AiObservabilityPromptDetailType = NonNullable<
    AiObservabilityPromptQuery['aiObservabilityPrompt']
>;

export type AiObservabilityPromptVersionType = NonNullable<
    NonNullable<AiObservabilityPromptVersionsQuery['aiObservabilityPromptVersions']>[number]
>;
```

- [ ] **Step 2: Update AiGateway.tsx sidebar**

In `client/src/pages/automation/ai-gateway/AiGateway.tsx`:

Add import:
```typescript
import AiObservabilityPrompts from './components/prompts/AiObservabilityPrompts';
```

Update the type union (add `'prompts'` in alphabetical position):
```typescript
type AiGatewayPageType = 'budget' | 'models' | 'monitoring' | 'projects' | 'prompts' | 'providers' | 'routing' | 'settings';
```

Add a `LeftSidebarNavItem` entry after the "Monitoring" item:
```typescript
<LeftSidebarNavItem
    item={{
        current: activePage === 'prompts',
        name: 'Prompts',
        onItemClick: () => setActivePage('prompts'),
    }}
/>
```

Add conditional render after the monitoring render:
```typescript
{activePage === 'prompts' && <AiObservabilityPrompts />}
```

- [ ] **Step 3: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/types.ts src/pages/automation/ai-gateway/AiGateway.tsx
git commit -m "732 client - Add Prompts sidebar tab to AI Gateway"
```

---

## Task 13: Client — Prompts List Component

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/prompts/AiObservabilityPrompts.tsx`

- [ ] **Step 1: Create AiObservabilityPrompts.tsx**

```typescript
import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {useAiObservabilityPromptsQuery, useDeleteAiObservabilityPromptMutation} from '@/shared/middleware/graphql';
import {useWorkspaceStore} from '@/shared/stores/workspace.store';
import {useQueryClient} from '@tanstack/react-query';
import {FileTextIcon, PlusIcon, TrashIcon} from 'lucide-react';
import {useCallback, useState} from 'react';

import {AiObservabilityPromptType} from '../../types';
import AiObservabilityPromptDetail from './AiObservabilityPromptDetail';
import AiObservabilityPromptDialog from './AiObservabilityPromptDialog';

const AiObservabilityPrompts = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [selectedPrompt, setSelectedPrompt] = useState<AiObservabilityPromptType | undefined>();
    const [showDialog, setShowDialog] = useState(false);

    const queryClient = useQueryClient();

    const {data: promptsData, isLoading: promptsIsLoading} = useAiObservabilityPromptsQuery({
        workspaceId: currentWorkspaceId + '',
    });

    const deleteMutation = useDeleteAiObservabilityPromptMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiObservabilityPrompts']});
        },
    });

    const prompts = promptsData?.aiObservabilityPrompts ?? [];

    const handleDelete = useCallback(
        (event: React.MouseEvent, promptId: string) => {
            event.stopPropagation();

            deleteMutation.mutate({id: promptId});
        },
        [deleteMutation],
    );

    if (selectedPrompt) {
        return (
            <AiObservabilityPromptDetail
                onBack={() => setSelectedPrompt(undefined)}
                promptId={selectedPrompt.id}
            />
        );
    }

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <div className="mb-4 flex items-center justify-between">
                <h2 className="text-lg font-semibold">Prompts</h2>

                <Button
                    icon={<PlusIcon className="size-4" />}
                    label="New Prompt"
                    onClick={() => setShowDialog(true)}
                />
            </div>

            {promptsIsLoading ? (
                <PageLoader />
            ) : prompts.length === 0 ? (
                <EmptyList
                    icon={<FileTextIcon className="size-12 text-muted-foreground" />}
                    message="Create a prompt to manage versioned prompt templates served through the gateway."
                    title="No Prompts Found"
                />
            ) : (
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead>
                            <tr className="border-b text-muted-foreground">
                                <th className="px-3 py-2 font-medium">Name</th>
                                <th className="px-3 py-2 font-medium">Description</th>
                                <th className="px-3 py-2 font-medium">Created</th>
                                <th className="px-3 py-2 font-medium">Last Modified</th>
                                <th className="px-3 py-2 font-medium"></th>
                            </tr>
                        </thead>

                        <tbody>
                            {prompts.map((prompt) => (
                                <tr
                                    className="cursor-pointer border-b hover:bg-muted/50"
                                    key={prompt.id}
                                    onClick={() => setSelectedPrompt(prompt)}
                                >
                                    <td className="px-3 py-2 font-medium">{prompt.name}</td>
                                    <td className="px-3 py-2 text-muted-foreground">
                                        {prompt.description || '-'}
                                    </td>
                                    <td className="px-3 py-2 text-muted-foreground">
                                        {prompt.createdDate
                                            ? new Date(Number(prompt.createdDate)).toLocaleString()
                                            : '-'}
                                    </td>
                                    <td className="px-3 py-2 text-muted-foreground">
                                        {prompt.lastModifiedDate
                                            ? new Date(Number(prompt.lastModifiedDate)).toLocaleString()
                                            : '-'}
                                    </td>
                                    <td className="px-3 py-2">
                                        <button
                                            className="text-muted-foreground hover:text-destructive"
                                            onClick={(event) => handleDelete(event, prompt.id)}
                                        >
                                            <TrashIcon className="size-4" />
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {showDialog && (
                <AiObservabilityPromptDialog
                    onClose={() => setShowDialog(false)}
                    workspaceId={currentWorkspaceId + ''}
                />
            )}
        </div>
    );
};

export default AiObservabilityPrompts;
```

- [ ] **Step 2: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/prompts/AiObservabilityPrompts.tsx
git commit -m "732 client - Add Prompts list component with create and delete"
```

---

## Task 14: Client — Prompt Dialog (Create/Edit)

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/prompts/AiObservabilityPromptDialog.tsx`

- [ ] **Step 1: Create AiObservabilityPromptDialog.tsx**

```typescript
import Button from '@/components/Button/Button';
import {
    useCreateAiObservabilityPromptMutation,
    useUpdateAiObservabilityPromptMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {XIcon} from 'lucide-react';
import {useCallback, useState} from 'react';

import {AiObservabilityPromptType} from '../../types';

interface AiObservabilityPromptDialogProps {
    onClose: () => void;
    prompt?: AiObservabilityPromptType;
    workspaceId: string;
}

const AiObservabilityPromptDialog = ({onClose, prompt, workspaceId}: AiObservabilityPromptDialogProps) => {
    const [description, setDescription] = useState(prompt?.description ?? '');
    const [name, setName] = useState(prompt?.name ?? '');

    const queryClient = useQueryClient();

    const isEditMode = !!prompt;

    const createMutation = useCreateAiObservabilityPromptMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiObservabilityPrompts']});

            onClose();
        },
    });

    const updateMutation = useUpdateAiObservabilityPromptMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiObservabilityPrompts']});
            queryClient.invalidateQueries({queryKey: ['aiObservabilityPrompt']});

            onClose();
        },
    });

    const handleSubmit = useCallback(() => {
        if (isEditMode) {
            updateMutation.mutate({
                id: prompt.id,
                input: {
                    description: description || undefined,
                    name,
                },
            });
        } else {
            createMutation.mutate({
                input: {
                    description: description || undefined,
                    name,
                    workspaceId,
                },
            });
        }
    }, [createMutation, description, isEditMode, name, prompt, updateMutation, workspaceId]);

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-md rounded-lg bg-background p-6 shadow-lg">
                <div className="mb-4 flex items-center justify-between">
                    <h3 className="text-lg font-medium">{isEditMode ? 'Edit Prompt' : 'New Prompt'}</h3>

                    <button onClick={onClose}>
                        <XIcon className="size-4" />
                    </button>
                </div>

                <div className="space-y-4">
                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Name</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="e.g. customer-support-reply"
                            value={name}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Description (optional)</label>

                        <textarea
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setDescription(event.target.value)}
                            placeholder="Describe the purpose of this prompt"
                            rows={3}
                            value={description}
                        />
                    </fieldset>
                </div>

                <div className="mt-6 flex justify-end gap-2">
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button
                        disabled={!name || createMutation.isPending || updateMutation.isPending}
                        label={isEditMode ? 'Save' : 'Create'}
                        onClick={handleSubmit}
                    />
                </div>
            </div>
        </div>
    );
};

export default AiObservabilityPromptDialog;
```

- [ ] **Step 2: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/prompts/AiObservabilityPromptDialog.tsx
git commit -m "732 client - Add Prompt create/edit dialog"
```

---

## Task 15: Client — Prompt Version Dialog

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/prompts/AiObservabilityPromptVersionDialog.tsx`

- [ ] **Step 1: Create AiObservabilityPromptVersionDialog.tsx**

```typescript
import Button from '@/components/Button/Button';
import {useCreateAiObservabilityPromptVersionMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {XIcon} from 'lucide-react';
import {useCallback, useMemo, useState} from 'react';

interface AiObservabilityPromptVersionDialogProps {
    onClose: () => void;
    promptId: string;
}

const ENVIRONMENT_OPTIONS = ['development', 'production', 'staging'];

const TYPE_OPTIONS = ['TEXT', 'CHAT'];

const VARIABLE_PATTERN = /\{\{(\w+)}}/g;

const AiObservabilityPromptVersionDialog = ({onClose, promptId}: AiObservabilityPromptVersionDialogProps) => {
    const [active, setActive] = useState(false);
    const [commitMessage, setCommitMessage] = useState('');
    const [content, setContent] = useState('');
    const [environment, setEnvironment] = useState('development');
    const [type, setType] = useState('TEXT');

    const queryClient = useQueryClient();

    const extractedVariables = useMemo(() => {
        const variables: string[] = [];
        let match = VARIABLE_PATTERN.exec(content);

        while (match !== null) {
            if (!variables.includes(match[1])) {
                variables.push(match[1]);
            }

            match = VARIABLE_PATTERN.exec(content);
        }

        return variables;
    }, [content]);

    const createMutation = useCreateAiObservabilityPromptVersionMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiObservabilityPrompt']});
            queryClient.invalidateQueries({queryKey: ['aiObservabilityPromptVersions']});

            onClose();
        },
    });

    const handleSubmit = useCallback(() => {
        const variablesJson =
            extractedVariables.length > 0
                ? JSON.stringify(extractedVariables.map((variableName) => ({name: variableName, type: 'string'})))
                : undefined;

        createMutation.mutate({
            input: {
                active,
                commitMessage: commitMessage || undefined,
                content,
                environment,
                promptId,
                type,
                variables: variablesJson,
            },
        });
    }, [active, commitMessage, content, createMutation, environment, extractedVariables, promptId, type]);

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-2xl rounded-lg bg-background p-6 shadow-lg">
                <div className="mb-4 flex items-center justify-between">
                    <h3 className="text-lg font-medium">New Version</h3>

                    <button onClick={onClose}>
                        <XIcon className="size-4" />
                    </button>
                </div>

                <div className="max-h-[70vh] space-y-4 overflow-y-auto">
                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Type</label>

                        <select
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setType(event.target.value)}
                            value={type}
                        >
                            {TYPE_OPTIONS.map((option) => (
                                <option key={option} value={option}>
                                    {option}
                                </option>
                            ))}
                        </select>
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Content</label>

                        <textarea
                            className="w-full rounded-md border px-3 py-2 font-mono text-sm"
                            onChange={(event) => setContent(event.target.value)}
                            placeholder={
                                type === 'TEXT'
                                    ? 'You are a helpful assistant. Answer questions about {{topic}} in a {{tone}} tone.'
                                    : '[{"role": "system", "content": "You are a helpful assistant."}, {"role": "user", "content": "{{user_message}}"}]'
                            }
                            rows={10}
                            value={content}
                        />
                    </fieldset>

                    {extractedVariables.length > 0 && (
                        <div className="rounded-md border bg-muted/30 px-3 py-2">
                            <span className="text-xs font-medium text-muted-foreground">Detected Variables: </span>

                            <span className="text-xs">
                                {extractedVariables.map((variableName, index) => (
                                    <span key={variableName}>
                                        {index > 0 && ', '}

                                        <code className="rounded bg-muted px-1">{`{{${variableName}}}`}</code>
                                    </span>
                                ))}
                            </span>
                        </div>
                    )}

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Environment</label>

                        <select
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setEnvironment(event.target.value)}
                            value={environment}
                        >
                            {ENVIRONMENT_OPTIONS.map((option) => (
                                <option key={option} value={option}>
                                    {option}
                                </option>
                            ))}
                        </select>
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="flex items-center gap-2 text-sm font-medium">
                            <input
                                checked={active}
                                onChange={(event) => setActive(event.target.checked)}
                                type="checkbox"
                            />
                            Set as active version for this environment
                        </label>
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Commit Message (optional)</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setCommitMessage(event.target.value)}
                            placeholder="Describe what changed in this version"
                            value={commitMessage}
                        />
                    </fieldset>
                </div>

                <div className="mt-6 flex justify-end gap-2">
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button
                        disabled={!content || createMutation.isPending}
                        label="Create Version"
                        onClick={handleSubmit}
                    />
                </div>
            </div>
        </div>
    );
};

export default AiObservabilityPromptVersionDialog;
```

- [ ] **Step 2: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/prompts/AiObservabilityPromptVersionDialog.tsx
git commit -m "732 client - Add Prompt version creation dialog with variable extraction"
```

---

## Task 16: Client — Prompt Detail Component (Version History + Environment Controls)

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/prompts/AiObservabilityPromptDetail.tsx`

- [ ] **Step 1: Create AiObservabilityPromptDetail.tsx**

```typescript
import Button from '@/components/Button/Button';
import PageLoader from '@/components/PageLoader';
import {useAiObservabilityPromptQuery, useSetActivePromptVersionMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ArrowLeftIcon, CheckCircleIcon, PlusIcon} from 'lucide-react';
import {useCallback, useMemo, useState} from 'react';

import {AiObservabilityPromptVersionType} from '../../types';
import AiObservabilityPromptVersionDialog from './AiObservabilityPromptVersionDialog';

interface AiObservabilityPromptDetailProps {
    onBack: () => void;
    promptId: string;
}

const ENVIRONMENT_OPTIONS = ['development', 'production', 'staging'];

const AiObservabilityPromptDetail = ({onBack, promptId}: AiObservabilityPromptDetailProps) => {
    const [selectedVersion, setSelectedVersion] = useState<AiObservabilityPromptVersionType | undefined>();
    const [showVersionDialog, setShowVersionDialog] = useState(false);

    const queryClient = useQueryClient();

    const {data: promptData, isLoading: promptIsLoading} = useAiObservabilityPromptQuery({
        id: promptId,
    });

    const setActiveMutation = useSetActivePromptVersionMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiObservabilityPrompt']});
        },
    });

    const prompt = promptData?.aiObservabilityPrompt;
    const versions = prompt?.versions ?? [];

    const activeVersionsByEnvironment = useMemo(() => {
        const activeVersionMap = new Map<string, AiObservabilityPromptVersionType>();

        for (const version of versions) {
            if (version.active && version.environment) {
                activeVersionMap.set(version.environment, version);
            }
        }

        return activeVersionMap;
    }, [versions]);

    const handleSetActive = useCallback(
        (versionId: string, environment: string) => {
            setActiveMutation.mutate({
                environment,
                promptVersionId: versionId,
            });
        },
        [setActiveMutation],
    );

    if (promptIsLoading) {
        return <PageLoader />;
    }

    if (!prompt) {
        return <div className="p-4 text-muted-foreground">Prompt not found</div>;
    }

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <button
                className="mb-4 flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
                onClick={onBack}
            >
                <ArrowLeftIcon className="size-4" />
                Back to Prompts
            </button>

            <div className="mb-6 flex items-start justify-between">
                <div>
                    <h2 className="text-lg font-semibold">{prompt.name}</h2>

                    {prompt.description && (
                        <p className="mt-1 text-sm text-muted-foreground">{prompt.description}</p>
                    )}
                </div>

                <Button
                    icon={<PlusIcon className="size-4" />}
                    label="New Version"
                    onClick={() => setShowVersionDialog(true)}
                />
            </div>

            <div className="mb-6">
                <h3 className="mb-3 text-sm font-semibold">Active Versions by Environment</h3>

                <div className="flex gap-4">
                    {ENVIRONMENT_OPTIONS.map((environment) => {
                        const activeVersion = activeVersionsByEnvironment.get(environment);

                        return (
                            <div className="flex-1 rounded-md border px-4 py-3" key={environment}>
                                <div className="text-xs font-medium uppercase text-muted-foreground">
                                    {environment}
                                </div>

                                <div className="mt-1 text-sm font-medium">
                                    {activeVersion
                                        ? `v${activeVersion.versionNumber}`
                                        : 'No active version'}
                                </div>
                            </div>
                        );
                    })}
                </div>
            </div>

            <h3 className="mb-3 text-sm font-semibold">Version History ({versions.length} versions)</h3>

            <div className="space-y-2">
                {versions.map((version) => (
                    <div
                        className={`cursor-pointer rounded-md border px-4 py-3 ${
                            selectedVersion?.id === version.id ? 'border-primary bg-primary/5' : ''
                        }`}
                        key={version.id}
                        onClick={() =>
                            setSelectedVersion(selectedVersion?.id === version.id ? undefined : version)
                        }
                    >
                        <div className="flex items-center gap-3">
                            <span className="font-mono text-sm font-medium">v{version.versionNumber}</span>

                            <span
                                className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                                    version.type === 'TEXT'
                                        ? 'bg-blue-100 text-blue-800'
                                        : 'bg-purple-100 text-purple-800'
                                }`}
                            >
                                {version.type}
                            </span>

                            {version.active && version.environment && (
                                <span className="flex items-center gap-1 rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-800">
                                    <CheckCircleIcon className="size-3" />
                                    {version.environment}
                                </span>
                            )}

                            <span className="ml-auto text-xs text-muted-foreground">
                                {version.createdBy}
                                {version.createdDate &&
                                    ` · ${new Date(Number(version.createdDate)).toLocaleString()}`}
                            </span>
                        </div>

                        {version.commitMessage && (
                            <p className="mt-1 text-sm text-muted-foreground">{version.commitMessage}</p>
                        )}

                        {selectedVersion?.id === version.id && (
                            <div className="mt-3 space-y-3">
                                <div>
                                    <div className="mb-1 text-xs font-medium text-muted-foreground">Content</div>

                                    <pre className="max-h-60 overflow-auto rounded-md bg-muted p-3 font-mono text-xs">
                                        {version.content}
                                    </pre>
                                </div>

                                {version.variables && (
                                    <div>
                                        <div className="mb-1 text-xs font-medium text-muted-foreground">
                                            Variables
                                        </div>

                                        <pre className="overflow-auto rounded-md bg-muted p-3 font-mono text-xs">
                                            {version.variables}
                                        </pre>
                                    </div>
                                )}

                                <div className="flex gap-2">
                                    {ENVIRONMENT_OPTIONS.map((environment) => (
                                        <button
                                            className="rounded-md border px-3 py-1 text-xs hover:bg-muted"
                                            key={environment}
                                            onClick={(event) => {
                                                event.stopPropagation();

                                                handleSetActive(version.id, environment);
                                            }}
                                        >
                                            {version.active && version.environment === environment
                                                ? `Active in ${environment}`
                                                : `Deploy to ${environment}`}
                                        </button>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>
                ))}
            </div>

            {showVersionDialog && (
                <AiObservabilityPromptVersionDialog
                    onClose={() => setShowVersionDialog(false)}
                    promptId={promptId}
                />
            )}
        </div>
    );
};

export default AiObservabilityPromptDetail;
```

- [ ] **Step 2: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/prompts/AiObservabilityPromptDetail.tsx
git commit -m "732 client - Add Prompt detail component with version history and environment controls"
```

---

## Task 17: Client — Verify and Format

- [ ] **Step 1: Run format**

Run: `cd client && npm run format`
Expected: Files formatted

- [ ] **Step 2: Run lint and typecheck**

Run: `cd client && npm run check`
Expected: No errors

- [ ] **Step 3: Fix any issues found in step 2**

Address lint/typecheck errors as needed. Common fixes:
- Object keys not in alphabetical order (sort them manually)
- Import destructures not sorted (reorder alphabetically)
- Interface names not ending with `I` or `Props` (all interfaces in this plan already follow this convention)
- `useRef` variables missing `Ref` suffix (not applicable here)
- Lucide icon imports missing `Icon` suffix (all imports in this plan already follow this convention)

- [ ] **Step 4: Commit fixes if any**

```bash
cd client
git add -A
git commit -m "732 client - Fix lint and format issues for prompt management components"
```

---

## Task 18: Server — Verify Full Compilation

- [ ] **Step 1: Run spotlessApply**

Run: `./gradlew spotlessApply`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run compileJava for the full gateway module**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-graphql:compileJava :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-public-rest:compileJava`
Expected: BUILD SUCCESSFUL for all modules

- [ ] **Step 3: Commit formatting fixes if any**

```bash
git add -A
git commit -m "732 Apply spotless formatting to prompt management classes"
```
