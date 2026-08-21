# Phase 1: Tracing & Sessions — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add hierarchical tracing (trace → span tree) and session grouping to the AI Gateway, transforming flat request logs into structured observability data.

**Architecture:** New domain entities (`AiObservabilityTrace`, `AiObservabilitySpan`, `AiObservabilitySession`) in the existing `automation-ai-gateway` module. Callers opt in via HTTP headers (`X-ByteChef-Trace-Id`, etc.). The `AiGatewayFacade` creates trace/span records alongside existing request logs. GraphQL exposes queries for the UI. Two new sidebar tabs (Traces, Sessions) in the client.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JDBC, PostgreSQL, Liquibase, GraphQL (Spring GraphQL), React 19, TypeScript 5.9, TanStack Query, Recharts, Tailwind CSS

**Reference spec:** `docs/superpowers/specs/2026-04-11-ai-gateway-observability-platform-design.md` — Phase 1 section

---

## File Map

### Server — API module (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `src/main/java/.../domain/AiObservabilityTrace.java` | Trace domain entity |
| Create | `src/main/java/.../domain/AiObservabilityTraceSource.java` | Enum: API, PLAYGROUND |
| Create | `src/main/java/.../domain/AiObservabilityTraceStatus.java` | Enum: ACTIVE, COMPLETED, ERROR |
| Create | `src/main/java/.../domain/AiObservabilityTraceTag.java` | Join table entity for tags |
| Create | `src/main/java/.../domain/AiObservabilitySpan.java` | Span domain entity |
| Create | `src/main/java/.../domain/AiObservabilitySpanType.java` | Enum: GENERATION, SPAN, EVENT, TOOL_CALL |
| Create | `src/main/java/.../domain/AiObservabilitySpanStatus.java` | Enum: ACTIVE, COMPLETED, ERROR |
| Create | `src/main/java/.../domain/AiObservabilitySpanLevel.java` | Enum: DEBUG, DEFAULT, WARNING, ERROR |
| Create | `src/main/java/.../domain/AiObservabilitySession.java` | Session domain entity |
| Create | `src/main/java/.../repository/AiObservabilityTraceRepository.java` | Trace repository |
| Create | `src/main/java/.../repository/AiObservabilitySpanRepository.java` | Span repository |
| Create | `src/main/java/.../repository/AiObservabilitySessionRepository.java` | Session repository |
| Create | `src/main/java/.../service/AiObservabilityTraceService.java` | Trace service interface |
| Create | `src/main/java/.../service/AiObservabilitySpanService.java` | Span service interface |
| Create | `src/main/java/.../service/AiObservabilitySessionService.java` | Session service interface |

### Server — Service module (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000002_ai_observability_init.xml` | Liquibase migration |
| Create | `src/main/java/.../service/AiObservabilityTraceServiceImpl.java` | Trace service impl |
| Create | `src/main/java/.../service/AiObservabilitySpanServiceImpl.java` | Span service impl |
| Create | `src/main/java/.../service/AiObservabilitySessionServiceImpl.java` | Session service impl |
| Create | `src/main/java/.../dto/AiObservabilityTracingHeaders.java` | DTO for parsed tracing headers |
| Modify | `src/main/java/.../facade/AiGatewayFacade.java` | Add tracing header processing |

### Server — GraphQL module (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `src/main/resources/graphql/ai-observability-trace.graphqls` | Trace GraphQL schema |
| Create | `src/main/resources/graphql/ai-observability-span.graphqls` | Span GraphQL schema |
| Create | `src/main/resources/graphql/ai-observability-session.graphqls` | Session GraphQL schema |
| Create | `src/main/java/.../web/graphql/AiObservabilityTraceGraphQlController.java` | Trace queries |
| Create | `src/main/java/.../web/graphql/AiObservabilitySessionGraphQlController.java` | Session queries |

### Server — Public REST module (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-public-rest/`)

| Action | Path | Purpose |
|---|---|---|
| Modify | `src/main/java/.../public_/web/rest/AiGatewayChatCompletionApiController.java` | Extract tracing headers, pass to facade |

### Client (`client/src/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `graphql/automation/ai-gateway/aiObservabilityTraces.graphql` | Trace queries/mutations |
| Create | `graphql/automation/ai-gateway/aiObservabilitySessions.graphql` | Session queries |
| Modify | `pages/automation/ai-gateway/types.ts` | Add trace/session/span types |
| Modify | `pages/automation/ai-gateway/AiGateway.tsx` | Add Traces and Sessions sidebar tabs |
| Create | `pages/automation/ai-gateway/components/traces/AiObservabilityTraces.tsx` | Traces list page |
| Create | `pages/automation/ai-gateway/components/traces/AiObservabilityTraceDetail.tsx` | Trace detail with span tree |
| Create | `pages/automation/ai-gateway/components/sessions/AiObservabilitySessions.tsx` | Sessions list page |
| Create | `pages/automation/ai-gateway/components/sessions/AiObservabilitySessionDetail.tsx` | Session detail with traces |

---

## Task 1: Liquibase Migration

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000002_ai_observability_init.xml`

- [ ] **Step 1: Create the migration file**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">
    <changeSet id="00000000000002" author="Ivica Cardic">
        <preConditions onFail="MARK_RAN">
            <not>
                <tableExists tableName="ai_observability_session"/>
            </not>
        </preConditions>

        <createTable tableName="ai_observability_session">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="workspace_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="project_id" type="BIGINT"/>
            <column name="name" type="VARCHAR(256)"/>
            <column name="user_id" type="VARCHAR(256)"/>
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

        <createIndex tableName="ai_observability_session" indexName="idx_ai_obs_session_workspace">
            <column name="workspace_id"/>
        </createIndex>

        <createIndex tableName="ai_observability_session" indexName="idx_ai_obs_session_project">
            <column name="project_id"/>
        </createIndex>

        <createIndex tableName="ai_observability_session" indexName="idx_ai_obs_session_user">
            <column name="user_id"/>
        </createIndex>

        <createTable tableName="ai_observability_trace">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="workspace_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="project_id" type="BIGINT"/>
            <column name="session_id" type="BIGINT"/>
            <column name="name" type="VARCHAR(256)"/>
            <column name="user_id" type="VARCHAR(256)"/>
            <column name="input" type="TEXT"/>
            <column name="output" type="TEXT"/>
            <column name="metadata" type="TEXT"/>
            <column name="source" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="status" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="total_cost" type="DECIMAL(10,6)"/>
            <column name="total_input_tokens" type="INT"/>
            <column name="total_output_tokens" type="INT"/>
            <column name="total_latency_ms" type="INT"/>
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

        <addForeignKeyConstraint constraintName="fk_ai_obs_trace_session"
                                 baseTableName="ai_observability_trace" baseColumnNames="session_id"
                                 referencedTableName="ai_observability_session" referencedColumnNames="id"/>

        <createIndex tableName="ai_observability_trace" indexName="idx_ai_obs_trace_workspace">
            <column name="workspace_id"/>
        </createIndex>

        <createIndex tableName="ai_observability_trace" indexName="idx_ai_obs_trace_project">
            <column name="project_id"/>
        </createIndex>

        <createIndex tableName="ai_observability_trace" indexName="idx_ai_obs_trace_session">
            <column name="session_id"/>
        </createIndex>

        <createIndex tableName="ai_observability_trace" indexName="idx_ai_obs_trace_user">
            <column name="user_id"/>
        </createIndex>

        <createIndex tableName="ai_observability_trace" indexName="idx_ai_obs_trace_created">
            <column name="created_date"/>
        </createIndex>

        <createIndex tableName="ai_observability_trace" indexName="idx_ai_obs_trace_source">
            <column name="source"/>
        </createIndex>

        <createTable tableName="ai_observability_trace_tag">
            <column name="ai_observability_trace" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="tag_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addForeignKeyConstraint baseTableName="ai_observability_trace_tag" baseColumnNames="ai_observability_trace"
                                 referencedTableName="ai_observability_trace" referencedColumnNames="id"
                                 constraintName="fk_ai_obs_trace_tag_trace"/>

        <createTable tableName="ai_observability_span">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="trace_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="parent_span_id" type="BIGINT"/>
            <column name="name" type="VARCHAR(256)"/>
            <column name="type" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="model" type="VARCHAR(256)"/>
            <column name="provider" type="VARCHAR(64)"/>
            <column name="prompt_id" type="BIGINT"/>
            <column name="prompt_version_id" type="BIGINT"/>
            <column name="input" type="TEXT"/>
            <column name="output" type="TEXT"/>
            <column name="metadata" type="TEXT"/>
            <column name="input_tokens" type="INT"/>
            <column name="output_tokens" type="INT"/>
            <column name="cost" type="DECIMAL(10,6)"/>
            <column name="latency_ms" type="INT"/>
            <column name="status" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="level" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="start_time" type="TIMESTAMP"/>
            <column name="end_time" type="TIMESTAMP"/>
            <column name="created_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="version" type="BIGINT">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addForeignKeyConstraint constraintName="fk_ai_obs_span_trace"
                                 baseTableName="ai_observability_span" baseColumnNames="trace_id"
                                 referencedTableName="ai_observability_trace" referencedColumnNames="id"/>

        <addForeignKeyConstraint constraintName="fk_ai_obs_span_parent"
                                 baseTableName="ai_observability_span" baseColumnNames="parent_span_id"
                                 referencedTableName="ai_observability_span" referencedColumnNames="id"/>

        <createIndex tableName="ai_observability_span" indexName="idx_ai_obs_span_trace">
            <column name="trace_id"/>
        </createIndex>

        <createIndex tableName="ai_observability_span" indexName="idx_ai_obs_span_parent">
            <column name="parent_span_id"/>
        </createIndex>

        <createIndex tableName="ai_observability_span" indexName="idx_ai_obs_span_type">
            <column name="type"/>
        </createIndex>

        <createIndex tableName="ai_observability_span" indexName="idx_ai_obs_span_model">
            <column name="model"/>
        </createIndex>

        <createIndex tableName="ai_observability_span" indexName="idx_ai_obs_span_created">
            <column name="created_date"/>
        </createIndex>
    </changeSet>
</databaseChangeLog>
```

- [ ] **Step 2: Verify the migration compiles**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000002_ai_observability_init.xml
git commit -m "732 Add Liquibase migration for observability tables (trace, span, session)"
```

---

## Task 2: Enum Domain Classes

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityTraceSource.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityTraceStatus.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilitySpanType.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilitySpanStatus.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilitySpanLevel.java`

- [ ] **Step 1: Create AiObservabilityTraceSource**

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
public enum AiObservabilityTraceSource {

    API,
    PLAYGROUND
}
```

- [ ] **Step 2: Create AiObservabilityTraceStatus**

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
public enum AiObservabilityTraceStatus {

    ACTIVE,
    COMPLETED,
    ERROR
}
```

- [ ] **Step 3: Create AiObservabilitySpanType**

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
public enum AiObservabilitySpanType {

    GENERATION,
    SPAN,
    EVENT,
    TOOL_CALL
}
```

- [ ] **Step 4: Create AiObservabilitySpanStatus**

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
public enum AiObservabilitySpanStatus {

    ACTIVE,
    COMPLETED,
    ERROR
}
```

- [ ] **Step 5: Create AiObservabilitySpanLevel**

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
public enum AiObservabilitySpanLevel {

    DEBUG,
    DEFAULT,
    WARNING,
    ERROR
}
```

- [ ] **Step 6: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityTraceSource.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityTraceStatus.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilitySpanType.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilitySpanStatus.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilitySpanLevel.java
git commit -m "732 Add observability enum types (trace source/status, span type/status/level)"
```

---

## Task 3: Session Domain Entity

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilitySession.java`

- [ ] **Step 1: Create AiObservabilitySession**

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
@Table("ai_observability_session")
public class AiObservabilitySession {

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Id
    private Long id;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column
    private String name;

    @Column("project_id")
    private Long projectId;

    @Column("user_id")
    private String userId;

    @Version
    private int version;

    @Column("workspace_id")
    private Long workspaceId;

    private AiObservabilitySession() {
    }

    public AiObservabilitySession(Long workspaceId) {
        Validate.notNull(workspaceId, "workspaceId must not be null");

        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilitySession aiObservabilitySession)) {
            return false;
        }

        return Objects.equals(id, aiObservabilitySession.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Instant getCreatedDate() {
        return createdDate;
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

    public String getUserId() {
        return userId;
    }

    public int getVersion() {
        return version;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "AiObservabilitySession{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", name='" + name + '\'' +
            ", userId='" + userId + '\'' +
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
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilitySession.java
git commit -m "732 Add AiObservabilitySession domain entity"
```

---

## Task 4: Trace Domain Entity

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityTrace.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityTraceTag.java`

- [ ] **Step 1: Create AiObservabilityTraceTag**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_observability_trace_tag")
public record AiObservabilityTraceTag(long tagId) {
}
```

- [ ] **Step 2: Create AiObservabilityTrace**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_observability_trace")
public class AiObservabilityTrace {

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Id
    private Long id;

    @Column
    private String input;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column
    private String metadata;

    @Column
    private String name;

    @Column
    private String output;

    @Column("project_id")
    private Long projectId;

    @Column("session_id")
    private Long sessionId;

    @Column
    private int source;

    @Column
    private int status;

    @MappedCollection(idColumn = "ai_observability_trace")
    private Set<AiObservabilityTraceTag> tags = new HashSet<>();

    @Column("total_cost")
    private BigDecimal totalCost;

    @Column("total_input_tokens")
    private Integer totalInputTokens;

    @Column("total_latency_ms")
    private Integer totalLatencyMs;

    @Column("total_output_tokens")
    private Integer totalOutputTokens;

    @Column("user_id")
    private String userId;

    @Version
    private int version;

    @Column("workspace_id")
    private Long workspaceId;

    private AiObservabilityTrace() {
    }

    public AiObservabilityTrace(Long workspaceId, AiObservabilityTraceSource source) {
        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notNull(source, "source must not be null");

        this.source = source.ordinal();
        this.status = AiObservabilityTraceStatus.ACTIVE.ordinal();
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilityTrace aiObservabilityTrace)) {
            return false;
        }

        return Objects.equals(id, aiObservabilityTrace.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Long getId() {
        return id;
    }

    public String getInput() {
        return input;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getMetadata() {
        return metadata;
    }

    public String getName() {
        return name;
    }

    public String getOutput() {
        return output;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public AiObservabilityTraceSource getSource() {
        return AiObservabilityTraceSource.values()[source];
    }

    public AiObservabilityTraceStatus getStatus() {
        return AiObservabilityTraceStatus.values()[status];
    }

    public Set<AiObservabilityTraceTag> getTags() {
        return tags;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public Integer getTotalInputTokens() {
        return totalInputTokens;
    }

    public Integer getTotalLatencyMs() {
        return totalLatencyMs;
    }

    public Integer getTotalOutputTokens() {
        return totalOutputTokens;
    }

    public String getUserId() {
        return userId;
    }

    public int getVersion() {
        return version;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public void setStatus(AiObservabilityTraceStatus status) {
        Validate.notNull(status, "status must not be null");

        this.status = status.ordinal();
    }

    public void setTags(Set<AiObservabilityTraceTag> tags) {
        this.tags = tags;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public void setTotalInputTokens(Integer totalInputTokens) {
        this.totalInputTokens = totalInputTokens;
    }

    public void setTotalLatencyMs(Integer totalLatencyMs) {
        this.totalLatencyMs = totalLatencyMs;
    }

    public void setTotalOutputTokens(Integer totalOutputTokens) {
        this.totalOutputTokens = totalOutputTokens;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "AiObservabilityTrace{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", name='" + name + '\'' +
            ", status=" + getStatus() +
            ", source=" + getSource() +
            ", createdDate=" + createdDate +
            '}';
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityTrace.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityTraceTag.java
git commit -m "732 Add AiObservabilityTrace and AiObservabilityTraceTag domain entities"
```

---

## Task 5: Span Domain Entity

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilitySpan.java`

- [ ] **Step 1: Create AiObservabilitySpan**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_observability_span")
public class AiObservabilitySpan {

    @Column
    private BigDecimal cost;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("end_time")
    private Instant endTime;

    @Id
    private Long id;

    @Column
    private String input;

    @Column("input_tokens")
    private Integer inputTokens;

    @Column("latency_ms")
    private Integer latencyMs;

    @Column
    private int level;

    @Column
    private String metadata;

    @Column
    private String model;

    @Column
    private String name;

    @Column
    private String output;

    @Column("output_tokens")
    private Integer outputTokens;

    @Column("parent_span_id")
    private Long parentSpanId;

    @Column
    private String provider;

    @Column("prompt_id")
    private Long promptId;

    @Column("prompt_version_id")
    private Long promptVersionId;

    @Column("start_time")
    private Instant startTime;

    @Column
    private int status;

    @Column("trace_id")
    private Long traceId;

    @Column
    private int type;

    @Version
    private int version;

    private AiObservabilitySpan() {
    }

    public AiObservabilitySpan(Long traceId, AiObservabilitySpanType type) {
        Validate.notNull(traceId, "traceId must not be null");
        Validate.notNull(type, "type must not be null");

        this.level = AiObservabilitySpanLevel.DEFAULT.ordinal();
        this.startTime = Instant.now();
        this.status = AiObservabilitySpanStatus.ACTIVE.ordinal();
        this.traceId = traceId;
        this.type = type.ordinal();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilitySpan aiObservabilitySpan)) {
            return false;
        }

        return Objects.equals(id, aiObservabilitySpan.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public BigDecimal getCost() {
        return cost;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public Long getId() {
        return id;
    }

    public String getInput() {
        return input;
    }

    public Integer getInputTokens() {
        return inputTokens;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public AiObservabilitySpanLevel getLevel() {
        return AiObservabilitySpanLevel.values()[level];
    }

    public String getMetadata() {
        return metadata;
    }

    public String getModel() {
        return model;
    }

    public String getName() {
        return name;
    }

    public String getOutput() {
        return output;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public Long getParentSpanId() {
        return parentSpanId;
    }

    public String getProvider() {
        return provider;
    }

    public Long getPromptId() {
        return promptId;
    }

    public Long getPromptVersionId() {
        return promptVersionId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public AiObservabilitySpanStatus getStatus() {
        return AiObservabilitySpanStatus.values()[status];
    }

    public Long getTraceId() {
        return traceId;
    }

    public AiObservabilitySpanType getType() {
        return AiObservabilitySpanType.values()[type];
    }

    public int getVersion() {
        return version;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public void setInputTokens(Integer inputTokens) {
        this.inputTokens = inputTokens;
    }

    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    public void setLevel(AiObservabilitySpanLevel level) {
        Validate.notNull(level, "level must not be null");

        this.level = level.ordinal();
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public void setOutputTokens(Integer outputTokens) {
        this.outputTokens = outputTokens;
    }

    public void setParentSpanId(Long parentSpanId) {
        this.parentSpanId = parentSpanId;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setPromptId(Long promptId) {
        this.promptId = promptId;
    }

    public void setPromptVersionId(Long promptVersionId) {
        this.promptVersionId = promptVersionId;
    }

    public void setStatus(AiObservabilitySpanStatus status) {
        Validate.notNull(status, "status must not be null");

        this.status = status.ordinal();
    }

    @Override
    public String toString() {
        return "AiObservabilitySpan{" +
            "id=" + id +
            ", traceId=" + traceId +
            ", name='" + name + '\'' +
            ", type=" + getType() +
            ", model='" + model + '\'' +
            ", status=" + getStatus() +
            '}';
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilitySpan.java
git commit -m "732 Add AiObservabilitySpan domain entity"
```

---

## Task 6: Repository Interfaces

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilitySessionRepository.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityTraceRepository.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilitySpanRepository.java`

- [ ] **Step 1: Create AiObservabilitySessionRepository**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySession;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiObservabilitySessionRepository extends ListCrudRepository<AiObservabilitySession, Long> {

    List<AiObservabilitySession> findAllByWorkspaceId(Long workspaceId);

    List<AiObservabilitySession> findAllByWorkspaceIdAndUserId(Long workspaceId, String userId);
}
```

- [ ] **Step 2: Create AiObservabilityTraceRepository**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import java.time.Instant;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiObservabilityTraceRepository extends ListCrudRepository<AiObservabilityTrace, Long> {

    List<AiObservabilityTrace> findAllByWorkspaceIdAndCreatedDateBetween(
        Long workspaceId, Instant start, Instant end);

    List<AiObservabilityTrace> findAllByWorkspaceIdAndSourceAndCreatedDateBetween(
        Long workspaceId, int source, Instant start, Instant end);

    List<AiObservabilityTrace> findAllBySessionId(Long sessionId);
}
```

- [ ] **Step 3: Create AiObservabilitySpanRepository**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySpan;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiObservabilitySpanRepository extends ListCrudRepository<AiObservabilitySpan, Long> {

    List<AiObservabilitySpan> findAllByTraceId(Long traceId);
}
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilitySessionRepository.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityTraceRepository.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilitySpanRepository.java
git commit -m "732 Add observability repository interfaces (session, trace, span)"
```

---

## Task 7: Service Interfaces

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilitySessionService.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityTraceService.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilitySpanService.java`

- [ ] **Step 1: Create AiObservabilitySessionService**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySession;
import java.util.List;

/**
 * @version ee
 */
public interface AiObservabilitySessionService {

    AiObservabilitySession create(AiObservabilitySession session);

    AiObservabilitySession getSession(long id);

    List<AiObservabilitySession> getSessionsByWorkspace(Long workspaceId);

    List<AiObservabilitySession> getSessionsByWorkspaceAndUser(Long workspaceId, String userId);

    AiObservabilitySession getOrCreateSession(Long workspaceId, Long projectId, String userId);
}
```

- [ ] **Step 2: Create AiObservabilityTraceService**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTraceSource;
import java.time.Instant;
import java.util.List;

/**
 * @version ee
 */
public interface AiObservabilityTraceService {

    AiObservabilityTrace create(AiObservabilityTrace trace);

    AiObservabilityTrace getTrace(long id);

    List<AiObservabilityTrace> getTracesByWorkspace(Long workspaceId, Instant start, Instant end);

    List<AiObservabilityTrace> getTracesByWorkspaceAndSource(
        Long workspaceId, AiObservabilityTraceSource source, Instant start, Instant end);

    List<AiObservabilityTrace> getTracesBySession(Long sessionId);

    AiObservabilityTrace update(AiObservabilityTrace trace);
}
```

- [ ] **Step 3: Create AiObservabilitySpanService**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySpan;
import java.util.List;

/**
 * @version ee
 */
public interface AiObservabilitySpanService {

    AiObservabilitySpan create(AiObservabilitySpan span);

    List<AiObservabilitySpan> getSpansByTrace(Long traceId);

    AiObservabilitySpan update(AiObservabilitySpan span);
}
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilitySessionService.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityTraceService.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilitySpanService.java
git commit -m "732 Add observability service interfaces (session, trace, span)"
```

---

## Task 8: Service Implementations

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilitySessionServiceImpl.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityTraceServiceImpl.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilitySpanServiceImpl.java`

- [ ] **Step 1: Create AiObservabilitySessionServiceImpl**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySession;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilitySessionRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
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
class AiObservabilitySessionServiceImpl implements AiObservabilitySessionService {

    private final AiObservabilitySessionRepository aiObservabilitySessionRepository;

    public AiObservabilitySessionServiceImpl(
        AiObservabilitySessionRepository aiObservabilitySessionRepository) {

        this.aiObservabilitySessionRepository = aiObservabilitySessionRepository;
    }

    @Override
    public AiObservabilitySession create(AiObservabilitySession session) {
        Validate.notNull(session, "session must not be null");
        Validate.isTrue(session.getId() == null, "session id must be null for creation");

        return aiObservabilitySessionRepository.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    public AiObservabilitySession getSession(long id) {
        return aiObservabilitySessionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiObservabilitySession not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilitySession> getSessionsByWorkspace(Long workspaceId) {
        return aiObservabilitySessionRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilitySession> getSessionsByWorkspaceAndUser(Long workspaceId, String userId) {
        return aiObservabilitySessionRepository.findAllByWorkspaceIdAndUserId(workspaceId, userId);
    }

    @Override
    public AiObservabilitySession getOrCreateSession(Long workspaceId, Long projectId, String userId) {
        AiObservabilitySession session = new AiObservabilitySession(workspaceId);

        session.setProjectId(projectId);
        session.setUserId(userId);

        return aiObservabilitySessionRepository.save(session);
    }
}
```

- [ ] **Step 2: Create AiObservabilityTraceServiceImpl**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTraceSource;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilityTraceRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
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
class AiObservabilityTraceServiceImpl implements AiObservabilityTraceService {

    private final AiObservabilityTraceRepository aiObservabilityTraceRepository;

    public AiObservabilityTraceServiceImpl(
        AiObservabilityTraceRepository aiObservabilityTraceRepository) {

        this.aiObservabilityTraceRepository = aiObservabilityTraceRepository;
    }

    @Override
    public AiObservabilityTrace create(AiObservabilityTrace trace) {
        Validate.notNull(trace, "trace must not be null");
        Validate.isTrue(trace.getId() == null, "trace id must be null for creation");

        return aiObservabilityTraceRepository.save(trace);
    }

    @Override
    @Transactional(readOnly = true)
    public AiObservabilityTrace getTrace(long id) {
        return aiObservabilityTraceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiObservabilityTrace not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityTrace> getTracesByWorkspace(Long workspaceId, Instant start, Instant end) {
        return aiObservabilityTraceRepository.findAllByWorkspaceIdAndCreatedDateBetween(workspaceId, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityTrace> getTracesByWorkspaceAndSource(
        Long workspaceId, AiObservabilityTraceSource source, Instant start, Instant end) {

        return aiObservabilityTraceRepository.findAllByWorkspaceIdAndSourceAndCreatedDateBetween(
            workspaceId, source.ordinal(), start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityTrace> getTracesBySession(Long sessionId) {
        return aiObservabilityTraceRepository.findAllBySessionId(sessionId);
    }

    @Override
    public AiObservabilityTrace update(AiObservabilityTrace trace) {
        Validate.notNull(trace, "trace must not be null");
        Validate.notNull(trace.getId(), "trace id must not be null for update");

        return aiObservabilityTraceRepository.save(trace);
    }
}
```

- [ ] **Step 3: Create AiObservabilitySpanServiceImpl**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySpan;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilitySpanRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
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
class AiObservabilitySpanServiceImpl implements AiObservabilitySpanService {

    private final AiObservabilitySpanRepository aiObservabilitySpanRepository;

    public AiObservabilitySpanServiceImpl(
        AiObservabilitySpanRepository aiObservabilitySpanRepository) {

        this.aiObservabilitySpanRepository = aiObservabilitySpanRepository;
    }

    @Override
    public AiObservabilitySpan create(AiObservabilitySpan span) {
        Validate.notNull(span, "span must not be null");
        Validate.isTrue(span.getId() == null, "span id must be null for creation");

        return aiObservabilitySpanRepository.save(span);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilitySpan> getSpansByTrace(Long traceId) {
        return aiObservabilitySpanRepository.findAllByTraceId(traceId);
    }

    @Override
    public AiObservabilitySpan update(AiObservabilitySpan span) {
        Validate.notNull(span, "span must not be null");
        Validate.notNull(span.getId(), "span id must not be null for update");

        return aiObservabilitySpanRepository.save(span);
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilitySessionServiceImpl.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityTraceServiceImpl.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilitySpanServiceImpl.java
git commit -m "732 Add observability service implementations (session, trace, span)"
```

---

## Task 9: Tracing Headers DTO and Facade Integration

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/dto/AiObservabilityTracingHeaders.java`
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/facade/AiGatewayFacade.java`
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-public-rest/src/main/java/com/bytechef/ee/automation/ai/gateway/public_/web/rest/AiGatewayChatCompletionApiController.java`

- [ ] **Step 1: Create AiObservabilityTracingHeaders DTO**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.dto;

import jakarta.annotation.Nullable;
import java.util.Map;

/**
 * Parsed tracing headers from an incoming gateway request.
 *
 * @version ee
 */
public record AiObservabilityTracingHeaders(
    @Nullable String traceId,
    @Nullable String sessionId,
    @Nullable String spanName,
    @Nullable String parentSpanId,
    @Nullable String userId,
    Map<String, String> metadata) {

    public static final String HEADER_TRACE_ID = "X-ByteChef-Trace-Id";
    public static final String HEADER_SESSION_ID = "X-ByteChef-Session-Id";
    public static final String HEADER_SPAN_NAME = "X-ByteChef-Span-Name";
    public static final String HEADER_PARENT_SPAN_ID = "X-ByteChef-Parent-Span-Id";
    public static final String HEADER_USER_ID = "X-ByteChef-User-Id";
    public static final String HEADER_METADATA_PREFIX = "X-ByteChef-Metadata-";

    public boolean hasTracingEnabled() {
        return traceId != null;
    }
}
```

- [ ] **Step 2: Modify AiGatewayChatCompletionApiController to extract headers**

Add to the non-streaming `chatCompletions` method and streaming `chatCompletionsStream` method: extract tracing headers from `HttpServletRequest` and pass them to the facade.

In `AiGatewayChatCompletionApiController.java`, add `HttpServletRequest` parameter to both methods and extract headers:

```java
// Add import
import com.bytechef.ee.automation.ai.gateway.dto.AiObservabilityTracingHeaders;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;

// Add private method to extract headers:
private AiObservabilityTracingHeaders extractTracingHeaders(HttpServletRequest httpServletRequest) {
    Map<String, String> metadata = new HashMap<>();

    Enumeration<String> headerNames = httpServletRequest.getHeaderNames();

    while (headerNames.hasMoreElements()) {
        String headerName = headerNames.nextElement();

        if (headerName.toLowerCase().startsWith(
            AiObservabilityTracingHeaders.HEADER_METADATA_PREFIX.toLowerCase())) {

            String metadataKey = headerName.substring(
                AiObservabilityTracingHeaders.HEADER_METADATA_PREFIX.length());

            metadata.put(metadataKey, httpServletRequest.getHeader(headerName));
        }
    }

    return new AiObservabilityTracingHeaders(
        httpServletRequest.getHeader(AiObservabilityTracingHeaders.HEADER_TRACE_ID),
        httpServletRequest.getHeader(AiObservabilityTracingHeaders.HEADER_SESSION_ID),
        httpServletRequest.getHeader(AiObservabilityTracingHeaders.HEADER_SPAN_NAME),
        httpServletRequest.getHeader(AiObservabilityTracingHeaders.HEADER_PARENT_SPAN_ID),
        httpServletRequest.getHeader(AiObservabilityTracingHeaders.HEADER_USER_ID),
        metadata);
}
```

Update `chatCompletions` to accept `HttpServletRequest` and pass tracing headers to facade:

```java
// Change method signature to include HttpServletRequest
// Pass extractTracingHeaders(httpServletRequest) to facade.chatCompletion()
```

Note: The exact facade method signature change depends on the current `chatCompletion` method. Add `AiObservabilityTracingHeaders` as an additional parameter.

- [ ] **Step 3: Modify AiGatewayFacade to accept and process tracing headers**

In `AiGatewayFacade.java`, inject the three new services:

```java
// Add fields
private final AiObservabilitySessionService aiObservabilitySessionService;
private final AiObservabilitySpanService aiObservabilitySpanService;
private final AiObservabilityTraceService aiObservabilityTraceService;
```

Add them to the constructor.

Add a method to create trace and span records when tracing headers are present:

```java
private void processTracingHeaders(
    AiObservabilityTracingHeaders tracingHeaders, Long workspaceId, Long projectId,
    String requestInput, String responseOutput, String routedModel, String routedProvider,
    Integer inputTokens, Integer outputTokens, BigDecimal cost, Integer latencyMs,
    boolean isError) {

    if (!tracingHeaders.hasTracingEnabled()) {
        return;
    }

    AiObservabilityTrace trace = new AiObservabilityTrace(
        workspaceId, AiObservabilityTraceSource.API);

    trace.setName(tracingHeaders.spanName());
    trace.setProjectId(projectId);
    trace.setUserId(tracingHeaders.userId());
    trace.setInput(requestInput);
    trace.setOutput(responseOutput);
    trace.setTotalCost(cost);
    trace.setTotalInputTokens(inputTokens);
    trace.setTotalOutputTokens(outputTokens);
    trace.setTotalLatencyMs(latencyMs);
    trace.setStatus(isError ? AiObservabilityTraceStatus.ERROR : AiObservabilityTraceStatus.COMPLETED);

    if (!tracingHeaders.metadata().isEmpty()) {
        trace.setMetadata(JsonUtils.write(tracingHeaders.metadata()));
    }

    if (tracingHeaders.sessionId() != null) {
        AiObservabilitySession session = aiObservabilitySessionService.getOrCreateSession(
            workspaceId, projectId, tracingHeaders.userId());

        trace.setSessionId(session.getId());
    }

    AiObservabilityTrace savedTrace = aiObservabilityTraceService.create(trace);

    AiObservabilitySpan span = new AiObservabilitySpan(
        savedTrace.getId(), AiObservabilitySpanType.GENERATION);

    span.setName(tracingHeaders.spanName());
    span.setModel(routedModel);
    span.setProvider(routedProvider);
    span.setInput(requestInput);
    span.setOutput(responseOutput);
    span.setInputTokens(inputTokens);
    span.setOutputTokens(outputTokens);
    span.setCost(cost);
    span.setLatencyMs(latencyMs);
    span.setEndTime(Instant.now());
    span.setStatus(isError ? AiObservabilitySpanStatus.ERROR : AiObservabilitySpanStatus.COMPLETED);

    if (tracingHeaders.parentSpanId() != null) {
        span.setParentSpanId(Long.valueOf(tracingHeaders.parentSpanId()));
    }

    aiObservabilitySpanService.create(span);
}
```

Call `processTracingHeaders()` after successful and error request processing in the existing `chatCompletion` method flow, alongside the existing request log creation.

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava && ./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-public-rest:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/dto/AiObservabilityTracingHeaders.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/facade/AiGatewayFacade.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-public-rest/src/main/java/com/bytechef/ee/automation/ai/gateway/public_/web/rest/AiGatewayChatCompletionApiController.java
git commit -m "732 Integrate tracing headers into gateway request pipeline"
```

---

## Task 10: GraphQL Schema for Traces, Spans, Sessions

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-trace.graphqls`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-span.graphqls`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-session.graphqls`

- [ ] **Step 1: Create ai-observability-trace.graphqls**

```graphql
extend type Query {
    aiObservabilityTrace(id: ID!): AiObservabilityTrace
    aiObservabilityTraces(workspaceId: ID!, startDate: Long!, endDate: Long!): [AiObservabilityTrace]
}

type AiObservabilityTrace {
    createdDate: Long
    id: ID!
    input: String
    lastModifiedDate: Long
    metadata: String
    name: String
    output: String
    projectId: ID
    sessionId: ID
    source: AiObservabilityTraceSource!
    spans: [AiObservabilitySpan]
    status: AiObservabilityTraceStatus!
    totalCost: Float
    totalInputTokens: Int
    totalLatencyMs: Int
    totalOutputTokens: Int
    userId: String
    version: Int
    workspaceId: ID!
}

enum AiObservabilityTraceSource {
    API
    PLAYGROUND
}

enum AiObservabilityTraceStatus {
    ACTIVE
    COMPLETED
    ERROR
}
```

- [ ] **Step 2: Create ai-observability-span.graphqls**

```graphql
type AiObservabilitySpan {
    cost: Float
    createdDate: Long
    endTime: Long
    id: ID!
    input: String
    inputTokens: Int
    latencyMs: Int
    level: AiObservabilitySpanLevel!
    metadata: String
    model: String
    name: String
    output: String
    outputTokens: Int
    parentSpanId: ID
    promptId: ID
    promptVersionId: ID
    provider: String
    startTime: Long
    status: AiObservabilitySpanStatus!
    traceId: ID!
    type: AiObservabilitySpanType!
    version: Int
}

enum AiObservabilitySpanType {
    EVENT
    GENERATION
    SPAN
    TOOL_CALL
}

enum AiObservabilitySpanStatus {
    ACTIVE
    COMPLETED
    ERROR
}

enum AiObservabilitySpanLevel {
    DEBUG
    DEFAULT
    ERROR
    WARNING
}
```

- [ ] **Step 3: Create ai-observability-session.graphqls**

```graphql
extend type Query {
    aiObservabilitySession(id: ID!): AiObservabilitySession
    aiObservabilitySessions(workspaceId: ID!): [AiObservabilitySession]
}

type AiObservabilitySession {
    createdDate: Long
    id: ID!
    lastModifiedDate: Long
    name: String
    projectId: ID
    traces: [AiObservabilityTrace]
    userId: String
    version: Int
    workspaceId: ID!
}
```

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-trace.graphqls \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-span.graphqls \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-session.graphqls
git commit -m "732 Add GraphQL schema for observability traces, spans, sessions"
```

---

## Task 11: GraphQL Controllers

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiObservabilityTraceGraphQlController.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiObservabilitySessionGraphQlController.java`

- [ ] **Step 1: Create AiObservabilityTraceGraphQlController**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySpan;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilitySpanService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityTraceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiObservabilityTraceGraphQlController {

    private final AiObservabilitySpanService aiObservabilitySpanService;
    private final AiObservabilityTraceService aiObservabilityTraceService;

    @SuppressFBWarnings("EI")
    AiObservabilityTraceGraphQlController(
        AiObservabilitySpanService aiObservabilitySpanService,
        AiObservabilityTraceService aiObservabilityTraceService) {

        this.aiObservabilitySpanService = aiObservabilitySpanService;
        this.aiObservabilityTraceService = aiObservabilityTraceService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityTrace aiObservabilityTrace(@Argument long id) {
        return aiObservabilityTraceService.getTrace(id);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiObservabilityTrace> aiObservabilityTraces(
        @Argument Long workspaceId, @Argument long startDate, @Argument long endDate) {

        Instant start = Instant.ofEpochMilli(startDate);
        Instant end = Instant.ofEpochMilli(endDate);

        return aiObservabilityTraceService.getTracesByWorkspace(workspaceId, start, end);
    }

    @SchemaMapping(typeName = "AiObservabilityTrace", field = "spans")
    public List<AiObservabilitySpan> spans(AiObservabilityTrace trace) {
        return aiObservabilitySpanService.getSpansByTrace(trace.getId());
    }
}
```

- [ ] **Step 2: Create AiObservabilitySessionGraphQlController**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySession;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilitySessionService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityTraceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiObservabilitySessionGraphQlController {

    private final AiObservabilitySessionService aiObservabilitySessionService;
    private final AiObservabilityTraceService aiObservabilityTraceService;

    @SuppressFBWarnings("EI")
    AiObservabilitySessionGraphQlController(
        AiObservabilitySessionService aiObservabilitySessionService,
        AiObservabilityTraceService aiObservabilityTraceService) {

        this.aiObservabilitySessionService = aiObservabilitySessionService;
        this.aiObservabilityTraceService = aiObservabilityTraceService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilitySession aiObservabilitySession(@Argument long id) {
        return aiObservabilitySessionService.getSession(id);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiObservabilitySession> aiObservabilitySessions(@Argument Long workspaceId) {
        return aiObservabilitySessionService.getSessionsByWorkspace(workspaceId);
    }

    @SchemaMapping(typeName = "AiObservabilitySession", field = "traces")
    public List<AiObservabilityTrace> traces(AiObservabilitySession session) {
        return aiObservabilityTraceService.getTracesBySession(session.getId());
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-graphql:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiObservabilityTraceGraphQlController.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiObservabilitySessionGraphQlController.java
git commit -m "732 Add GraphQL controllers for observability traces and sessions"
```

---

## Task 12: Client — GraphQL Operations and Codegen

**Files:**
- Create: `client/src/graphql/automation/ai-gateway/aiObservabilityTraces.graphql`
- Create: `client/src/graphql/automation/ai-gateway/aiObservabilitySessions.graphql`

- [ ] **Step 1: Create aiObservabilityTraces.graphql**

```graphql
query aiObservabilityTraces($workspaceId: ID!, $startDate: Long!, $endDate: Long!) {
    aiObservabilityTraces(workspaceId: $workspaceId, startDate: $startDate, endDate: $endDate) {
        createdDate
        id
        input
        lastModifiedDate
        metadata
        name
        output
        projectId
        sessionId
        source
        status
        totalCost
        totalInputTokens
        totalLatencyMs
        totalOutputTokens
        userId
        version
        workspaceId
    }
}

query aiObservabilityTrace($id: ID!) {
    aiObservabilityTrace(id: $id) {
        createdDate
        id
        input
        lastModifiedDate
        metadata
        name
        output
        projectId
        sessionId
        source
        spans {
            cost
            createdDate
            endTime
            id
            input
            inputTokens
            latencyMs
            level
            metadata
            model
            name
            output
            outputTokens
            parentSpanId
            provider
            startTime
            status
            traceId
            type
            version
        }
        status
        totalCost
        totalInputTokens
        totalLatencyMs
        totalOutputTokens
        userId
        version
        workspaceId
    }
}
```

- [ ] **Step 2: Create aiObservabilitySessions.graphql**

```graphql
query aiObservabilitySessions($workspaceId: ID!) {
    aiObservabilitySessions(workspaceId: $workspaceId) {
        createdDate
        id
        lastModifiedDate
        name
        projectId
        userId
        version
        workspaceId
    }
}

query aiObservabilitySession($id: ID!) {
    aiObservabilitySession(id: $id) {
        createdDate
        id
        lastModifiedDate
        name
        projectId
        traces {
            createdDate
            id
            name
            source
            status
            totalCost
            totalInputTokens
            totalLatencyMs
            totalOutputTokens
            userId
        }
        userId
        version
        workspaceId
    }
}
```

- [ ] **Step 3: Run GraphQL codegen**

Run: `cd client && npx graphql-codegen`
Expected: generates updated `src/shared/middleware/graphql.ts` with new query hooks

- [ ] **Step 4: Commit**

```bash
cd client
git add src/graphql/automation/ai-gateway/aiObservabilityTraces.graphql \
  src/graphql/automation/ai-gateway/aiObservabilitySessions.graphql \
  src/shared/middleware/graphql.ts
git commit -m "732 client - Add GraphQL operations and codegen for observability traces and sessions"
```

---

## Task 13: Client — Types and Sidebar Update

**Files:**
- Modify: `client/src/pages/automation/ai-gateway/types.ts`
- Modify: `client/src/pages/automation/ai-gateway/AiGateway.tsx`

- [ ] **Step 1: Add types to types.ts**

Add to `client/src/pages/automation/ai-gateway/types.ts`:

```typescript
import {
    AiGatewayProjectsQuery,
    AiObservabilitySessionQuery,
    AiObservabilitySessionsQuery,
    AiObservabilityTraceQuery,
    AiObservabilityTracesQuery,
    WorkspaceAiGatewayModelsQuery,
    WorkspaceAiGatewayProvidersQuery,
    WorkspaceAiGatewayRoutingPoliciesQuery,
} from '@/shared/middleware/graphql';

// ... existing types unchanged ...

export type AiObservabilitySessionType = NonNullable<
    NonNullable<AiObservabilitySessionsQuery['aiObservabilitySessions']>[number]
>;

export type AiObservabilitySessionDetailType = NonNullable<
    AiObservabilitySessionQuery['aiObservabilitySession']
>;

export type AiObservabilitySpanType = NonNullable<
    NonNullable<NonNullable<AiObservabilityTraceQuery['aiObservabilityTrace']>['spans']>[number]
>;

export type AiObservabilityTraceDetailType = NonNullable<
    AiObservabilityTraceQuery['aiObservabilityTrace']
>;

export type AiObservabilityTraceType = NonNullable<
    NonNullable<AiObservabilityTracesQuery['aiObservabilityTraces']>[number]
>;
```

- [ ] **Step 2: Update AiGateway.tsx sidebar**

In `client/src/pages/automation/ai-gateway/AiGateway.tsx`:

Update the type union:
```typescript
type AiGatewayPageType = 'budget' | 'models' | 'monitoring' | 'projects' | 'providers' | 'routing' | 'sessions' | 'settings' | 'traces';
```

Add imports for new components (placeholder — will be created in next tasks):
```typescript
import AiObservabilitySessions from './components/sessions/AiObservabilitySessions';
import AiObservabilityTraces from './components/traces/AiObservabilityTraces';
```

Add two `LeftSidebarNavItem` entries after the "Monitoring" item:
```typescript
<LeftSidebarNavItem
    item={{
        current: activePage === 'traces',
        name: 'Traces',
        onItemClick: () => setActivePage('traces'),
    }}
/>

<LeftSidebarNavItem
    item={{
        current: activePage === 'sessions',
        name: 'Sessions',
        onItemClick: () => setActivePage('sessions'),
    }}
/>
```

Add conditional renders:
```typescript
{activePage === 'traces' && <AiObservabilityTraces />}

{activePage === 'sessions' && <AiObservabilitySessions />}
```

- [ ] **Step 3: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/types.ts src/pages/automation/ai-gateway/AiGateway.tsx
git commit -m "732 client - Add Traces and Sessions sidebar tabs to AI Gateway"
```

---

## Task 14: Client — Traces List Component

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/traces/AiObservabilityTraces.tsx`

- [ ] **Step 1: Create AiObservabilityTraces.tsx**

```typescript
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {useAiObservabilityTracesQuery} from '@/shared/middleware/graphql';
import {useWorkspaceStore} from '@/shared/stores/workspace.store';
import {ActivityIcon} from 'lucide-react';
import {useMemo, useState} from 'react';

import {AiObservabilityTraceType} from '../../types';
import AiObservabilityTraceDetail from './AiObservabilityTraceDetail';

type TimeRangeType = '1h' | '24h' | '30d' | '6h' | '7d';

const TIME_RANGE_OPTIONS: TimeRangeType[] = ['1h', '6h', '24h', '7d', '30d'];

const TIME_RANGE_MS: Record<TimeRangeType, number> = {
    '1h': 3600000,
    '24h': 86400000,
    '30d': 2592000000,
    '6h': 21600000,
    '7d': 604800000,
};

const AiObservabilityTraces = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [selectedTrace, setSelectedTrace] = useState<AiObservabilityTraceType | undefined>();
    const [timeRange, setTimeRange] = useState<TimeRangeType>('24h');

    const endDate = useMemo(() => Date.now(), [timeRange]);
    const startDate = useMemo(() => endDate - TIME_RANGE_MS[timeRange], [endDate, timeRange]);

    const {data: tracesData, isLoading: tracesIsLoading} = useAiObservabilityTracesQuery({
        endDate: endDate + '',
        startDate: startDate + '',
        workspaceId: currentWorkspaceId + '',
    });

    const traces = tracesData?.aiObservabilityTraces ?? [];

    if (selectedTrace) {
        return (
            <AiObservabilityTraceDetail
                onBack={() => setSelectedTrace(undefined)}
                traceId={selectedTrace.id}
            />
        );
    }

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <div className="mb-4 flex items-center justify-between">
                <h2 className="text-lg font-semibold">Traces</h2>

                <div className="flex gap-1">
                    {TIME_RANGE_OPTIONS.map((option) => (
                        <button
                            className={`rounded-md px-3 py-1 text-sm ${
                                timeRange === option
                                    ? 'bg-primary text-primary-foreground'
                                    : 'bg-muted text-muted-foreground hover:bg-muted/80'
                            }`}
                            key={option}
                            onClick={() => setTimeRange(option)}
                        >
                            {option}
                        </button>
                    ))}
                </div>
            </div>

            {tracesIsLoading ? (
                <PageLoader />
            ) : traces.length === 0 ? (
                <EmptyList
                    icon={<ActivityIcon className="size-12 text-muted-foreground" />}
                    message="Send requests with X-ByteChef-Trace-Id header to start tracing."
                    title="No Traces Found"
                />
            ) : (
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead>
                            <tr className="border-b text-muted-foreground">
                                <th className="px-3 py-2 font-medium">Time</th>
                                <th className="px-3 py-2 font-medium">Name</th>
                                <th className="px-3 py-2 font-medium">User</th>
                                <th className="px-3 py-2 font-medium">Status</th>
                                <th className="px-3 py-2 font-medium">Latency</th>
                                <th className="px-3 py-2 font-medium">Tokens</th>
                                <th className="px-3 py-2 font-medium">Cost</th>
                            </tr>
                        </thead>

                        <tbody>
                            {traces.map((trace) => (
                                <tr
                                    className="cursor-pointer border-b hover:bg-muted/50"
                                    key={trace.id}
                                    onClick={() => setSelectedTrace(trace)}
                                >
                                    <td className="px-3 py-2 text-muted-foreground">
                                        {trace.createdDate
                                            ? new Date(Number(trace.createdDate)).toLocaleString()
                                            : '-'}
                                    </td>
                                    <td className="px-3 py-2 font-medium">{trace.name || '-'}</td>
                                    <td className="px-3 py-2">{trace.userId || '-'}</td>
                                    <td className="px-3 py-2">
                                        <span
                                            className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                                                trace.status === 'COMPLETED'
                                                    ? 'bg-green-100 text-green-800'
                                                    : trace.status === 'ERROR'
                                                      ? 'bg-red-100 text-red-800'
                                                      : 'bg-yellow-100 text-yellow-800'
                                            }`}
                                        >
                                            {trace.status}
                                        </span>
                                    </td>
                                    <td className="px-3 py-2">
                                        {trace.totalLatencyMs != null ? `${trace.totalLatencyMs}ms` : '-'}
                                    </td>
                                    <td className="px-3 py-2">
                                        {trace.totalInputTokens != null || trace.totalOutputTokens != null
                                            ? `${trace.totalInputTokens ?? 0} / ${trace.totalOutputTokens ?? 0}`
                                            : '-'}
                                    </td>
                                    <td className="px-3 py-2">
                                        {trace.totalCost != null ? `$${Number(trace.totalCost).toFixed(4)}` : '-'}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};

export default AiObservabilityTraces;
```

- [ ] **Step 2: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/traces/AiObservabilityTraces.tsx
git commit -m "732 client - Add Traces list component with time range filter"
```

---

## Task 15: Client — Trace Detail Component (Span Tree)

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/traces/AiObservabilityTraceDetail.tsx`

- [ ] **Step 1: Create AiObservabilityTraceDetail.tsx**

```typescript
import PageLoader from '@/components/PageLoader';
import {useAiObservabilityTraceQuery} from '@/shared/middleware/graphql';
import {ArrowLeftIcon, LayersIcon} from 'lucide-react';
import {useMemo} from 'react';

import {AiObservabilitySpanType} from '../../types';

interface AiObservabilityTraceDetailProps {
    onBack: () => void;
    traceId: string;
}

const AiObservabilityTraceDetail = ({onBack, traceId}: AiObservabilityTraceDetailProps) => {
    const {data: traceData, isLoading: traceIsLoading} = useAiObservabilityTraceQuery({
        id: traceId,
    });

    const trace = traceData?.aiObservabilityTrace;
    const spans = trace?.spans ?? [];

    const rootSpans = useMemo(
        () => spans.filter((span) => !span.parentSpanId),
        [spans],
    );

    const childSpansByParent = useMemo(() => {
        const childSpanMap = new Map<string, AiObservabilitySpanType[]>();

        for (const span of spans) {
            if (span.parentSpanId) {
                const children = childSpanMap.get(span.parentSpanId) || [];

                children.push(span);
                childSpanMap.set(span.parentSpanId, children);
            }
        }

        return childSpanMap;
    }, [spans]);

    if (traceIsLoading) {
        return <PageLoader />;
    }

    if (!trace) {
        return <div className="p-4 text-muted-foreground">Trace not found</div>;
    }

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <button
                className="mb-4 flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
                onClick={onBack}
            >
                <ArrowLeftIcon className="size-4" />
                Back to Traces
            </button>

            <div className="mb-6">
                <div className="flex items-center gap-3">
                    <h2 className="text-lg font-semibold">{trace.name || `Trace ${trace.id}`}</h2>

                    <span
                        className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                            trace.status === 'COMPLETED'
                                ? 'bg-green-100 text-green-800'
                                : trace.status === 'ERROR'
                                  ? 'bg-red-100 text-red-800'
                                  : 'bg-yellow-100 text-yellow-800'
                        }`}
                    >
                        {trace.status}
                    </span>
                </div>

                <div className="mt-2 flex gap-6 text-sm text-muted-foreground">
                    {trace.userId && <span>User: {trace.userId}</span>}
                    {trace.totalLatencyMs != null && <span>Latency: {trace.totalLatencyMs}ms</span>}
                    {trace.totalCost != null && <span>Cost: ${Number(trace.totalCost).toFixed(4)}</span>}
                    {(trace.totalInputTokens != null || trace.totalOutputTokens != null) && (
                        <span>
                            Tokens: {trace.totalInputTokens ?? 0} in / {trace.totalOutputTokens ?? 0} out
                        </span>
                    )}
                </div>
            </div>

            <h3 className="mb-3 flex items-center gap-2 text-sm font-semibold">
                <LayersIcon className="size-4" />
                Span Tree ({spans.length} spans)
            </h3>

            <div className="space-y-1">
                {rootSpans.map((span) => (
                    <SpanTreeNode childSpansByParent={childSpansByParent} depth={0} key={span.id} span={span} />
                ))}
            </div>
        </div>
    );
};

interface SpanTreeNodeProps {
    childSpansByParent: Map<string, AiObservabilitySpanType[]>;
    depth: number;
    span: AiObservabilitySpanType;
}

const SpanTreeNode = ({childSpansByParent, depth, span}: SpanTreeNodeProps) => {
    const children = childSpansByParent.get(span.id) || [];

    const typeColor: Record<string, string> = {
        EVENT: 'bg-purple-100 text-purple-800',
        GENERATION: 'bg-blue-100 text-blue-800',
        SPAN: 'bg-gray-100 text-gray-800',
        TOOL_CALL: 'bg-orange-100 text-orange-800',
    };

    return (
        <div style={{marginLeft: depth * 24}}>
            <div className="flex items-center gap-2 rounded-md border px-3 py-2 text-sm">
                <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${typeColor[span.type] || ''}`}>
                    {span.type}
                </span>

                <span className="font-medium">{span.name || '-'}</span>

                {span.model && <span className="text-muted-foreground">{span.model}</span>}

                {span.latencyMs != null && (
                    <span className="ml-auto text-muted-foreground">{span.latencyMs}ms</span>
                )}

                {span.cost != null && (
                    <span className="text-muted-foreground">${Number(span.cost).toFixed(4)}</span>
                )}

                <span
                    className={`rounded-full px-2 py-0.5 text-xs ${
                        span.status === 'COMPLETED'
                            ? 'bg-green-100 text-green-800'
                            : span.status === 'ERROR'
                              ? 'bg-red-100 text-red-800'
                              : 'bg-yellow-100 text-yellow-800'
                    }`}
                >
                    {span.status}
                </span>
            </div>

            {children.map((childSpan) => (
                <SpanTreeNode
                    childSpansByParent={childSpansByParent}
                    depth={depth + 1}
                    key={childSpan.id}
                    span={childSpan}
                />
            ))}
        </div>
    );
};

export default AiObservabilityTraceDetail;
```

- [ ] **Step 2: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/traces/AiObservabilityTraceDetail.tsx
git commit -m "732 client - Add Trace detail component with span tree visualization"
```

---

## Task 16: Client — Sessions Components

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/sessions/AiObservabilitySessions.tsx`
- Create: `client/src/pages/automation/ai-gateway/components/sessions/AiObservabilitySessionDetail.tsx`

- [ ] **Step 1: Create AiObservabilitySessions.tsx**

```typescript
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {useAiObservabilitySessionsQuery} from '@/shared/middleware/graphql';
import {useWorkspaceStore} from '@/shared/stores/workspace.store';
import {MessagesSquareIcon} from 'lucide-react';
import {useState} from 'react';

import {AiObservabilitySessionType} from '../../types';
import AiObservabilitySessionDetail from './AiObservabilitySessionDetail';

const AiObservabilitySessions = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [selectedSession, setSelectedSession] = useState<AiObservabilitySessionType | undefined>();

    const {data: sessionsData, isLoading: sessionsIsLoading} = useAiObservabilitySessionsQuery({
        workspaceId: currentWorkspaceId + '',
    });

    const sessions = sessionsData?.aiObservabilitySessions ?? [];

    if (selectedSession) {
        return (
            <AiObservabilitySessionDetail
                onBack={() => setSelectedSession(undefined)}
                sessionId={selectedSession.id}
            />
        );
    }

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <div className="mb-4">
                <h2 className="text-lg font-semibold">Sessions</h2>
            </div>

            {sessionsIsLoading ? (
                <PageLoader />
            ) : sessions.length === 0 ? (
                <EmptyList
                    icon={<MessagesSquareIcon className="size-12 text-muted-foreground" />}
                    message="Send requests with X-ByteChef-Session-Id header to group traces into sessions."
                    title="No Sessions Found"
                />
            ) : (
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead>
                            <tr className="border-b text-muted-foreground">
                                <th className="px-3 py-2 font-medium">Created</th>
                                <th className="px-3 py-2 font-medium">Name</th>
                                <th className="px-3 py-2 font-medium">User</th>
                            </tr>
                        </thead>

                        <tbody>
                            {sessions.map((session) => (
                                <tr
                                    className="cursor-pointer border-b hover:bg-muted/50"
                                    key={session.id}
                                    onClick={() => setSelectedSession(session)}
                                >
                                    <td className="px-3 py-2 text-muted-foreground">
                                        {session.createdDate
                                            ? new Date(Number(session.createdDate)).toLocaleString()
                                            : '-'}
                                    </td>
                                    <td className="px-3 py-2 font-medium">{session.name || `Session ${session.id}`}</td>
                                    <td className="px-3 py-2">{session.userId || '-'}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};

export default AiObservabilitySessions;
```

- [ ] **Step 2: Create AiObservabilitySessionDetail.tsx**

```typescript
import PageLoader from '@/components/PageLoader';
import {useAiObservabilitySessionQuery} from '@/shared/middleware/graphql';
import {ArrowLeftIcon} from 'lucide-react';

interface AiObservabilitySessionDetailProps {
    onBack: () => void;
    sessionId: string;
}

const AiObservabilitySessionDetail = ({onBack, sessionId}: AiObservabilitySessionDetailProps) => {
    const {data: sessionData, isLoading: sessionIsLoading} = useAiObservabilitySessionQuery({
        id: sessionId,
    });

    const session = sessionData?.aiObservabilitySession;
    const traces = session?.traces ?? [];

    if (sessionIsLoading) {
        return <PageLoader />;
    }

    if (!session) {
        return <div className="p-4 text-muted-foreground">Session not found</div>;
    }

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <button
                className="mb-4 flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
                onClick={onBack}
            >
                <ArrowLeftIcon className="size-4" />
                Back to Sessions
            </button>

            <div className="mb-6">
                <h2 className="text-lg font-semibold">{session.name || `Session ${session.id}`}</h2>

                <div className="mt-1 text-sm text-muted-foreground">
                    {session.userId && <span>User: {session.userId}</span>}
                    {session.userId && traces.length > 0 && <span> · </span>}
                    <span>{traces.length} traces</span>
                </div>
            </div>

            <div className="space-y-2">
                {traces.map((trace) => (
                    <div className="flex items-center gap-3 rounded-md border px-4 py-3 text-sm" key={trace.id}>
                        <span
                            className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                                trace.status === 'COMPLETED'
                                    ? 'bg-green-100 text-green-800'
                                    : trace.status === 'ERROR'
                                      ? 'bg-red-100 text-red-800'
                                      : 'bg-yellow-100 text-yellow-800'
                            }`}
                        >
                            {trace.status}
                        </span>

                        <span className="font-medium">{trace.name || `Trace ${trace.id}`}</span>

                        {trace.totalLatencyMs != null && (
                            <span className="text-muted-foreground">{trace.totalLatencyMs}ms</span>
                        )}

                        {trace.totalCost != null && (
                            <span className="ml-auto text-muted-foreground">
                                ${Number(trace.totalCost).toFixed(4)}
                            </span>
                        )}

                        <span className="text-muted-foreground">
                            {trace.createdDate ? new Date(Number(trace.createdDate)).toLocaleString() : ''}
                        </span>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default AiObservabilitySessionDetail;
```

- [ ] **Step 3: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/sessions/AiObservabilitySessions.tsx \
  src/pages/automation/ai-gateway/components/sessions/AiObservabilitySessionDetail.tsx
git commit -m "732 client - Add Sessions list and detail components"
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

Address lint/typecheck errors as needed (sort-keys, import ordering, naming conventions).

- [ ] **Step 4: Commit fixes if any**

```bash
cd client
git add -A
git commit -m "732 client - Fix lint and format issues for observability components"
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
git commit -m "732 Apply spotless formatting to observability classes"
```
