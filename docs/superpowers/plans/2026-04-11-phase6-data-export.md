# Phase 6: Data Export — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable users to export observability data (traces, request logs, sessions, prompts) in CSV/JSON/JSONL formats via on-demand or scheduled jobs, and to subscribe to real-time webhook notifications for key events (trace completed, alert triggered, budget exceeded).

**Architecture:** Two new domain entities (`AiObservabilityExportJob`, `AiObservabilityWebhookSubscription`) in the existing `automation-ai-gateway` module. Export jobs write to `platform-file-storage`. Webhook delivery uses HMAC-SHA256 signing with exponential backoff retry (3 attempts). New "Exports" sidebar tab in the client with export creation, history, and webhook subscription management.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JDBC, PostgreSQL, Liquibase, GraphQL (Spring GraphQL), React 19, TypeScript 5.9, TanStack Query, Tailwind CSS

**Reference spec:** `docs/superpowers/specs/2026-04-11-ai-gateway-observability-platform-design.md` — Phase 6 section

**Dependencies:** Phase 1 (tracing/sessions must exist for TRACES/SESSIONS export scope), Phase 3 (scheduler abstraction for scheduled exports, alert events for webhook events).

---

## File Map

### Server — API module (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `src/main/java/.../domain/AiObservabilityExportJob.java` | Export job domain entity |
| Create | `src/main/java/.../domain/AiObservabilityExportJobType.java` | Enum: ON_DEMAND, SCHEDULED |
| Create | `src/main/java/.../domain/AiObservabilityExportFormat.java` | Enum: CSV, JSON, JSONL |
| Create | `src/main/java/.../domain/AiObservabilityExportScope.java` | Enum: TRACES, REQUEST_LOGS, SESSIONS, PROMPTS |
| Create | `src/main/java/.../domain/AiObservabilityExportJobStatus.java` | Enum: PENDING, PROCESSING, COMPLETED, FAILED |
| Create | `src/main/java/.../domain/AiObservabilityWebhookSubscription.java` | Webhook subscription domain entity |
| Create | `src/main/java/.../repository/AiObservabilityExportJobRepository.java` | Export job repository |
| Create | `src/main/java/.../repository/AiObservabilityWebhookSubscriptionRepository.java` | Webhook subscription repository |
| Create | `src/main/java/.../service/AiObservabilityExportJobService.java` | Export job service interface |
| Create | `src/main/java/.../service/AiObservabilityWebhookSubscriptionService.java` | Webhook subscription service interface |
| Create | `src/main/java/.../service/AiObservabilityWebhookDeliveryService.java` | Webhook delivery service interface |

### Server — Service module (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/`)

| Action | Path | Purpose |
|---|---|---|
| Modify | `src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000002_ai_observability_init.xml` | Add export_job and webhook_subscription tables |
| Create | `src/main/java/.../service/AiObservabilityExportJobServiceImpl.java` | Export job service impl |
| Create | `src/main/java/.../service/AiObservabilityWebhookSubscriptionServiceImpl.java` | Webhook subscription service impl |
| Create | `src/main/java/.../service/AiObservabilityWebhookDeliveryServiceImpl.java` | Webhook delivery with HMAC signing and retry |
| Create | `src/main/java/.../service/AiObservabilityExportExecutor.java` | Async export execution (writes files to storage) |
| Modify | `build.gradle.kts` | Add file-storage-api dependency |

### Server — GraphQL module (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `src/main/resources/graphql/ai-observability-export-job.graphqls` | Export job GraphQL schema |
| Create | `src/main/resources/graphql/ai-observability-webhook-subscription.graphqls` | Webhook subscription GraphQL schema |
| Create | `src/main/java/.../web/graphql/AiObservabilityExportJobGraphQlController.java` | Export job queries and mutations |
| Create | `src/main/java/.../web/graphql/AiObservabilityWebhookSubscriptionGraphQlController.java` | Webhook subscription CRUD |

### Client (`client/src/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `graphql/automation/ai-gateway/aiObservabilityExportJobs.graphql` | Export job queries/mutations |
| Create | `graphql/automation/ai-gateway/aiObservabilityWebhookSubscriptions.graphql` | Webhook subscription queries/mutations |
| Modify | `pages/automation/ai-gateway/types.ts` | Add export job and webhook subscription types |
| Modify | `pages/automation/ai-gateway/AiGateway.tsx` | Add Exports sidebar tab |
| Create | `pages/automation/ai-gateway/components/exports/AiObservabilityExports.tsx` | Exports tab main component |
| Create | `pages/automation/ai-gateway/components/exports/AiObservabilityExportJobDialog.tsx` | Create export dialog |
| Create | `pages/automation/ai-gateway/components/exports/AiObservabilityWebhookSubscriptions.tsx` | Webhook subscription list |
| Create | `pages/automation/ai-gateway/components/exports/AiObservabilityWebhookSubscriptionDialog.tsx` | Create/edit webhook dialog |

---

## Task 1: Liquibase Migration

**Files:**
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000002_ai_observability_init.xml`

Note: This file is created by Phase 1 with changeset `00000000000002`. Phase 6 adds a new changeset with a unique ID to the same file.

- [ ] **Step 1: Add the export_job and webhook_subscription changeset**

Append the following changeset inside the existing `<databaseChangeLog>` element, after the Phase 1 changeset:

```xml
    <changeSet id="00000000000002-6" author="Ivica Cardic">
        <preConditions onFail="MARK_RAN">
            <not>
                <tableExists tableName="ai_observability_export_job"/>
            </not>
        </preConditions>

        <createTable tableName="ai_observability_export_job">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="workspace_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="project_id" type="BIGINT"/>
            <column name="type" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="format" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="scope" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="filters" type="TEXT"/>
            <column name="status" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="file_path" type="VARCHAR(512)"/>
            <column name="record_count" type="INT"/>
            <column name="error_message" type="TEXT"/>
            <column name="created_by" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="created_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <createIndex tableName="ai_observability_export_job" indexName="idx_ai_obs_export_job_workspace">
            <column name="workspace_id"/>
        </createIndex>

        <createIndex tableName="ai_observability_export_job" indexName="idx_ai_obs_export_job_status">
            <column name="status"/>
        </createIndex>

        <createIndex tableName="ai_observability_export_job" indexName="idx_ai_obs_export_job_created">
            <column name="created_date"/>
        </createIndex>

        <createTable tableName="ai_observability_webhook_subscription">
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
            <column name="url" type="VARCHAR(1024)">
                <constraints nullable="false"/>
            </column>
            <column name="secret" type="VARCHAR(256)"/>
            <column name="events" type="TEXT">
                <constraints nullable="false"/>
            </column>
            <column name="enabled" type="BOOLEAN">
                <constraints nullable="false"/>
            </column>
            <column name="last_triggered_date" type="TIMESTAMP"/>
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

        <createIndex tableName="ai_observability_webhook_subscription" indexName="idx_ai_obs_webhook_sub_workspace">
            <column name="workspace_id"/>
        </createIndex>

        <createIndex tableName="ai_observability_webhook_subscription" indexName="idx_ai_obs_webhook_sub_enabled">
            <column name="enabled"/>
        </createIndex>
    </changeSet>
```

- [ ] **Step 2: Verify the migration compiles**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000002_ai_observability_init.xml
git commit -m "732 Add Liquibase migration for export job and webhook subscription tables"
```

---

## Task 2: Enum Domain Classes

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityExportJobType.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityExportFormat.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityExportScope.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityExportJobStatus.java`

- [ ] **Step 1: Create AiObservabilityExportJobType**

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
public enum AiObservabilityExportJobType {

    ON_DEMAND,
    SCHEDULED
}
```

- [ ] **Step 2: Create AiObservabilityExportFormat**

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
public enum AiObservabilityExportFormat {

    CSV,
    JSON,
    JSONL
}
```

- [ ] **Step 3: Create AiObservabilityExportScope**

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
public enum AiObservabilityExportScope {

    TRACES,
    REQUEST_LOGS,
    SESSIONS,
    PROMPTS
}
```

- [ ] **Step 4: Create AiObservabilityExportJobStatus**

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
public enum AiObservabilityExportJobStatus {

    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
```

- [ ] **Step 5: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityExportJobType.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityExportFormat.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityExportScope.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityExportJobStatus.java
git commit -m "732 Add export enum types (job type, format, scope, status)"
```

---

## Task 3: ExportJob Domain Entity

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityExportJob.java`

- [ ] **Step 1: Create AiObservabilityExportJob**

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
@Table("ai_observability_export_job")
public class AiObservabilityExportJob {

    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("error_message")
    private String errorMessage;

    @Column("file_path")
    private String filePath;

    @Column
    private int format;

    @Id
    private Long id;

    @Column("project_id")
    private Long projectId;

    @Column("record_count")
    private Integer recordCount;

    @Column
    private int scope;

    @Column
    private int status;

    @Column
    private int type;

    @Column
    private String filters;

    @Column("workspace_id")
    private Long workspaceId;

    private AiObservabilityExportJob() {
    }

    public AiObservabilityExportJob(
        Long workspaceId, AiObservabilityExportJobType type, AiObservabilityExportFormat format,
        AiObservabilityExportScope scope, String createdBy) {

        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notNull(type, "type must not be null");
        Validate.notNull(format, "format must not be null");
        Validate.notNull(scope, "scope must not be null");
        Validate.notNull(createdBy, "createdBy must not be null");

        this.createdBy = createdBy;
        this.format = format.ordinal();
        this.scope = scope.ordinal();
        this.status = AiObservabilityExportJobStatus.PENDING.ordinal();
        this.type = type.ordinal();
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilityExportJob aiObservabilityExportJob)) {
            return false;
        }

        return Objects.equals(id, aiObservabilityExportJob.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFilters() {
        return filters;
    }

    public AiObservabilityExportFormat getFormat() {
        return AiObservabilityExportFormat.values()[format];
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Integer getRecordCount() {
        return recordCount;
    }

    public AiObservabilityExportScope getScope() {
        return AiObservabilityExportScope.values()[scope];
    }

    public AiObservabilityExportJobStatus getStatus() {
        return AiObservabilityExportJobStatus.values()[status];
    }

    public AiObservabilityExportJobType getType() {
        return AiObservabilityExportJobType.values()[type];
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setFilters(String filters) {
        this.filters = filters;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setRecordCount(Integer recordCount) {
        this.recordCount = recordCount;
    }

    public void setStatus(AiObservabilityExportJobStatus status) {
        Validate.notNull(status, "status must not be null");

        this.status = status.ordinal();
    }

    @Override
    public String toString() {
        return "AiObservabilityExportJob{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", type=" + getType() +
            ", format=" + getFormat() +
            ", scope=" + getScope() +
            ", status=" + getStatus() +
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
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityExportJob.java
git commit -m "732 Add AiObservabilityExportJob domain entity"
```

---

## Task 4: WebhookSubscription Domain Entity

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityWebhookSubscription.java`

- [ ] **Step 1: Create AiObservabilityWebhookSubscription**

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
@Table("ai_observability_webhook_subscription")
public class AiObservabilityWebhookSubscription {

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private boolean enabled;

    @Column
    private String events;

    @Id
    private Long id;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column("last_triggered_date")
    private Instant lastTriggeredDate;

    @Column
    private String name;

    @Column("project_id")
    private Long projectId;

    @Column
    private String secret;

    @Column
    private String url;

    @Version
    private int version;

    @Column("workspace_id")
    private Long workspaceId;

    private AiObservabilityWebhookSubscription() {
    }

    public AiObservabilityWebhookSubscription(Long workspaceId, String name, String url, String events) {
        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notNull(name, "name must not be null");
        Validate.notNull(url, "url must not be null");
        Validate.notNull(events, "events must not be null");

        this.enabled = true;
        this.events = events;
        this.name = name;
        this.url = url;
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilityWebhookSubscription aiObservabilityWebhookSubscription)) {
            return false;
        }

        return Objects.equals(id, aiObservabilityWebhookSubscription.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getEvents() {
        return events;
    }

    public Long getId() {
        return id;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Instant getLastTriggeredDate() {
        return lastTriggeredDate;
    }

    public String getName() {
        return name;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getSecret() {
        return secret;
    }

    public String getUrl() {
        return url;
    }

    public int getVersion() {
        return version;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setEvents(String events) {
        this.events = events;
    }

    public void setLastTriggeredDate(Instant lastTriggeredDate) {
        this.lastTriggeredDate = lastTriggeredDate;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "AiObservabilityWebhookSubscription{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", name='" + name + '\'' +
            ", url='" + url + '\'' +
            ", enabled=" + enabled +
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
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityWebhookSubscription.java
git commit -m "732 Add AiObservabilityWebhookSubscription domain entity"
```

---

## Task 5: Repository Interfaces

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityExportJobRepository.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityWebhookSubscriptionRepository.java`

- [ ] **Step 1: Create AiObservabilityExportJobRepository**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportJob;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiObservabilityExportJobRepository extends ListCrudRepository<AiObservabilityExportJob, Long> {

    List<AiObservabilityExportJob> findAllByWorkspaceIdOrderByCreatedDateDesc(Long workspaceId);

    List<AiObservabilityExportJob> findAllByStatus(int status);
}
```

- [ ] **Step 2: Create AiObservabilityWebhookSubscriptionRepository**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityWebhookSubscription;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiObservabilityWebhookSubscriptionRepository
    extends ListCrudRepository<AiObservabilityWebhookSubscription, Long> {

    List<AiObservabilityWebhookSubscription> findAllByWorkspaceId(Long workspaceId);

    List<AiObservabilityWebhookSubscription> findAllByWorkspaceIdAndEnabled(Long workspaceId, boolean enabled);
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityExportJobRepository.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityWebhookSubscriptionRepository.java
git commit -m "732 Add export job and webhook subscription repository interfaces"
```

---

## Task 6: Service Interfaces

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityExportJobService.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityWebhookSubscriptionService.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityWebhookDeliveryService.java`

- [ ] **Step 1: Create AiObservabilityExportJobService**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportJob;
import java.util.List;

/**
 * @version ee
 */
public interface AiObservabilityExportJobService {

    AiObservabilityExportJob create(AiObservabilityExportJob exportJob);

    AiObservabilityExportJob getExportJob(long id);

    List<AiObservabilityExportJob> getExportJobsByWorkspace(Long workspaceId);

    AiObservabilityExportJob update(AiObservabilityExportJob exportJob);
}
```

- [ ] **Step 2: Create AiObservabilityWebhookSubscriptionService**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityWebhookSubscription;
import java.util.List;

/**
 * @version ee
 */
public interface AiObservabilityWebhookSubscriptionService {

    AiObservabilityWebhookSubscription create(AiObservabilityWebhookSubscription subscription);

    void delete(long id);

    AiObservabilityWebhookSubscription getWebhookSubscription(long id);

    List<AiObservabilityWebhookSubscription> getWebhookSubscriptionsByWorkspace(Long workspaceId);

    List<AiObservabilityWebhookSubscription> getEnabledWebhookSubscriptionsByWorkspace(Long workspaceId);

    AiObservabilityWebhookSubscription update(AiObservabilityWebhookSubscription subscription);
}
```

- [ ] **Step 3: Create AiObservabilityWebhookDeliveryService**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import java.util.Map;

/**
 * Service for delivering webhook payloads to subscribed endpoints with HMAC-SHA256 signing
 * and exponential backoff retry (3 attempts).
 *
 * @version ee
 */
public interface AiObservabilityWebhookDeliveryService {

    void deliverEvent(Long workspaceId, String eventType, Map<String, Object> payload);

    void deliverTestEvent(long subscriptionId);
}
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityExportJobService.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityWebhookSubscriptionService.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityWebhookDeliveryService.java
git commit -m "732 Add export job, webhook subscription, and webhook delivery service interfaces"
```

---

## Task 7: Service Implementations

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityExportJobServiceImpl.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityWebhookSubscriptionServiceImpl.java`

- [ ] **Step 1: Create AiObservabilityExportJobServiceImpl**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportJob;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilityExportJobRepository;
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
class AiObservabilityExportJobServiceImpl implements AiObservabilityExportJobService {

    private final AiObservabilityExportJobRepository aiObservabilityExportJobRepository;

    public AiObservabilityExportJobServiceImpl(
        AiObservabilityExportJobRepository aiObservabilityExportJobRepository) {

        this.aiObservabilityExportJobRepository = aiObservabilityExportJobRepository;
    }

    @Override
    public AiObservabilityExportJob create(AiObservabilityExportJob exportJob) {
        Validate.notNull(exportJob, "exportJob must not be null");
        Validate.isTrue(exportJob.getId() == null, "exportJob id must be null for creation");

        return aiObservabilityExportJobRepository.save(exportJob);
    }

    @Override
    @Transactional(readOnly = true)
    public AiObservabilityExportJob getExportJob(long id) {
        return aiObservabilityExportJobRepository.findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException("AiObservabilityExportJob not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityExportJob> getExportJobsByWorkspace(Long workspaceId) {
        return aiObservabilityExportJobRepository.findAllByWorkspaceIdOrderByCreatedDateDesc(workspaceId);
    }

    @Override
    public AiObservabilityExportJob update(AiObservabilityExportJob exportJob) {
        Validate.notNull(exportJob, "exportJob must not be null");
        Validate.notNull(exportJob.getId(), "exportJob id must not be null for update");

        return aiObservabilityExportJobRepository.save(exportJob);
    }
}
```

- [ ] **Step 2: Create AiObservabilityWebhookSubscriptionServiceImpl**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityWebhookSubscription;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilityWebhookSubscriptionRepository;
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
class AiObservabilityWebhookSubscriptionServiceImpl implements AiObservabilityWebhookSubscriptionService {

    private final AiObservabilityWebhookSubscriptionRepository aiObservabilityWebhookSubscriptionRepository;

    public AiObservabilityWebhookSubscriptionServiceImpl(
        AiObservabilityWebhookSubscriptionRepository aiObservabilityWebhookSubscriptionRepository) {

        this.aiObservabilityWebhookSubscriptionRepository = aiObservabilityWebhookSubscriptionRepository;
    }

    @Override
    public AiObservabilityWebhookSubscription create(AiObservabilityWebhookSubscription subscription) {
        Validate.notNull(subscription, "subscription must not be null");
        Validate.isTrue(subscription.getId() == null, "subscription id must be null for creation");

        return aiObservabilityWebhookSubscriptionRepository.save(subscription);
    }

    @Override
    public void delete(long id) {
        aiObservabilityWebhookSubscriptionRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiObservabilityWebhookSubscription getWebhookSubscription(long id) {
        return aiObservabilityWebhookSubscriptionRepository.findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "AiObservabilityWebhookSubscription not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityWebhookSubscription> getWebhookSubscriptionsByWorkspace(Long workspaceId) {
        return aiObservabilityWebhookSubscriptionRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityWebhookSubscription> getEnabledWebhookSubscriptionsByWorkspace(Long workspaceId) {
        return aiObservabilityWebhookSubscriptionRepository.findAllByWorkspaceIdAndEnabled(workspaceId, true);
    }

    @Override
    public AiObservabilityWebhookSubscription update(AiObservabilityWebhookSubscription subscription) {
        Validate.notNull(subscription, "subscription must not be null");
        Validate.notNull(subscription.getId(), "subscription id must not be null for update");

        return aiObservabilityWebhookSubscriptionRepository.save(subscription);
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityExportJobServiceImpl.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityWebhookSubscriptionServiceImpl.java
git commit -m "732 Add export job and webhook subscription service implementations"
```

---

## Task 8: Webhook Delivery Service Implementation

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityWebhookDeliveryServiceImpl.java`

- [ ] **Step 1: Create AiObservabilityWebhookDeliveryServiceImpl**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityWebhookSubscription;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Delivers webhook payloads to subscribed endpoints with HMAC-SHA256 signing
 * and exponential backoff retry (3 attempts).
 *
 * @version ee
 */
@Service
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class AiObservabilityWebhookDeliveryServiceImpl implements AiObservabilityWebhookDeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(AiObservabilityWebhookDeliveryServiceImpl.class);

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final AiObservabilityWebhookSubscriptionService aiObservabilityWebhookSubscriptionService;
    private final HttpClient httpClient;

    public AiObservabilityWebhookDeliveryServiceImpl(
        AiObservabilityWebhookSubscriptionService aiObservabilityWebhookSubscriptionService) {

        this.aiObservabilityWebhookSubscriptionService = aiObservabilityWebhookSubscriptionService;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    @Override
    @Async
    public void deliverEvent(Long workspaceId, String eventType, Map<String, Object> payload) {
        List<AiObservabilityWebhookSubscription> subscriptions =
            aiObservabilityWebhookSubscriptionService.getEnabledWebhookSubscriptionsByWorkspace(workspaceId);

        for (AiObservabilityWebhookSubscription subscription : subscriptions) {
            if (!isSubscribedToEvent(subscription, eventType)) {
                continue;
            }

            String jsonPayload = JsonUtils.write(Map.of(
                "event", eventType,
                "data", payload,
                "timestamp", Instant.now().toString()));

            deliverWithRetry(subscription, jsonPayload);
        }
    }

    @Override
    @Async
    public void deliverTestEvent(long subscriptionId) {
        AiObservabilityWebhookSubscription subscription =
            aiObservabilityWebhookSubscriptionService.getWebhookSubscription(subscriptionId);

        String jsonPayload = JsonUtils.write(Map.of(
            "event", "test",
            "data", Map.of("message", "Test webhook delivery from ByteChef AI Gateway"),
            "timestamp", Instant.now().toString()));

        deliverWithRetry(subscription, jsonPayload);
    }

    private boolean isSubscribedToEvent(AiObservabilityWebhookSubscription subscription, String eventType) {
        String events = subscription.getEvents();

        return events != null && events.contains("\"" + eventType + "\"");
    }

    private void deliverWithRetry(AiObservabilityWebhookSubscription subscription, String jsonPayload) {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(subscription.getUrl()))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload));

                if (subscription.getSecret() != null && !subscription.getSecret().isEmpty()) {
                    String signature = computeHmacSignature(jsonPayload, subscription.getSecret());

                    requestBuilder.header("X-ByteChef-Signature", signature);
                }

                HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    subscription.setLastTriggeredDate(Instant.now());

                    aiObservabilityWebhookSubscriptionService.update(subscription);

                    return;
                }

                logger.warn(
                    "Webhook delivery to {} returned status {} (attempt {}/{})",
                    subscription.getUrl(), response.statusCode(), attempt, MAX_RETRY_ATTEMPTS);
            } catch (Exception exception) {
                logger.warn(
                    "Webhook delivery to {} failed (attempt {}/{}): {}",
                    subscription.getUrl(), attempt, MAX_RETRY_ATTEMPTS, exception.getMessage());
            }

            if (attempt < MAX_RETRY_ATTEMPTS) {
                long backoffMs = (long) Math.pow(2, attempt) * 1000;

                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();

                    return;
                }
            }
        }

        logger.error(
            "Webhook delivery to {} failed after {} attempts", subscription.getUrl(), MAX_RETRY_ATTEMPTS);
    }

    private String computeHmacSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);

            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));

            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte hmacByte : hmacBytes) {
                String hex = Integer.toHexString(0xff & hmacByte);

                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return "sha256=" + hexString;
        } catch (Exception exception) {
            throw new RuntimeException("Failed to compute HMAC signature", exception);
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityWebhookDeliveryServiceImpl.java
git commit -m "732 Add webhook delivery service with HMAC-SHA256 signing and exponential backoff retry"
```

---

## Task 9: Export Executor (Async File Generation)

**Files:**
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/build.gradle.kts`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityExportExecutor.java`

- [ ] **Step 1: Add file-storage-api dependency to build.gradle.kts**

Add to `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/build.gradle.kts`:

```kotlin
    implementation(project(":server:libs:core:file-storage:file-storage-api"))
```

- [ ] **Step 2: Create AiObservabilityExportExecutor**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportFormat;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportJob;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportJobStatus;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportScope;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityRequestLog;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySession;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Asynchronously executes export jobs by querying the appropriate data source,
 * formatting the results, and writing to file storage.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class AiObservabilityExportExecutor {

    private static final Logger logger = LoggerFactory.getLogger(AiObservabilityExportExecutor.class);

    private static final String EXPORT_DIRECTORY = "ai-gateway-exports";

    private final AiObservabilityExportJobService aiObservabilityExportJobService;
    private final AiObservabilityTraceService aiObservabilityTraceService;
    private final AiObservabilitySessionService aiObservabilitySessionService;
    private final FileStorageService fileStorageService;

    public AiObservabilityExportExecutor(
        AiObservabilityExportJobService aiObservabilityExportJobService,
        AiObservabilityTraceService aiObservabilityTraceService,
        AiObservabilitySessionService aiObservabilitySessionService,
        FileStorageService fileStorageService) {

        this.aiObservabilityExportJobService = aiObservabilityExportJobService;
        this.aiObservabilityTraceService = aiObservabilityTraceService;
        this.aiObservabilitySessionService = aiObservabilitySessionService;
        this.fileStorageService = fileStorageService;
    }

    @Async
    public void executeExport(long exportJobId) {
        AiObservabilityExportJob exportJob = aiObservabilityExportJobService.getExportJob(exportJobId);

        exportJob.setStatus(AiObservabilityExportJobStatus.PROCESSING);

        aiObservabilityExportJobService.update(exportJob);

        try {
            List<Map<String, Object>> records = fetchRecords(exportJob);
            String content = formatRecords(records, exportJob.getFormat());
            String filename = generateFilename(exportJob);

            FileEntry fileEntry = fileStorageService.storeFileContent(EXPORT_DIRECTORY, filename, content);

            exportJob = aiObservabilityExportJobService.getExportJob(exportJobId);

            exportJob.setStatus(AiObservabilityExportJobStatus.COMPLETED);
            exportJob.setFilePath(fileEntry.getUrl());
            exportJob.setRecordCount(records.size());

            aiObservabilityExportJobService.update(exportJob);
        } catch (Exception exception) {
            logger.error("Export job {} failed: {}", exportJobId, exception.getMessage(), exception);

            exportJob = aiObservabilityExportJobService.getExportJob(exportJobId);

            exportJob.setStatus(AiObservabilityExportJobStatus.FAILED);
            exportJob.setErrorMessage(exception.getMessage());

            aiObservabilityExportJobService.update(exportJob);
        }
    }

    private List<Map<String, Object>> fetchRecords(AiObservabilityExportJob exportJob) {
        AiObservabilityExportScope scope = exportJob.getScope();
        Long workspaceId = exportJob.getWorkspaceId();

        return switch (scope) {
            case TRACES -> {
                Instant end = Instant.now();
                Instant start = end.minusSeconds(30L * 24 * 60 * 60);

                List<AiObservabilityTrace> traces =
                    aiObservabilityTraceService.getTracesByWorkspace(workspaceId, start, end);

                yield traces.stream()
                    .map(trace -> Map.<String, Object>of(
                        "id", trace.getId(),
                        "name", trace.getName() != null ? trace.getName() : "",
                        "userId", trace.getUserId() != null ? trace.getUserId() : "",
                        "status", trace.getStatus().name(),
                        "source", trace.getSource().name(),
                        "totalCost", trace.getTotalCost() != null ? trace.getTotalCost().toString() : "",
                        "totalInputTokens",
                            trace.getTotalInputTokens() != null ? trace.getTotalInputTokens().toString() : "",
                        "totalOutputTokens",
                            trace.getTotalOutputTokens() != null ? trace.getTotalOutputTokens().toString() : "",
                        "totalLatencyMs",
                            trace.getTotalLatencyMs() != null ? trace.getTotalLatencyMs().toString() : "",
                        "createdDate", trace.getCreatedDate().toString()))
                    .toList();
            }
            case SESSIONS -> {
                List<AiObservabilitySession> sessions =
                    aiObservabilitySessionService.getSessionsByWorkspace(workspaceId);

                yield sessions.stream()
                    .map(session -> Map.<String, Object>of(
                        "id", session.getId(),
                        "name", session.getName() != null ? session.getName() : "",
                        "userId", session.getUserId() != null ? session.getUserId() : "",
                        "createdDate", session.getCreatedDate().toString()))
                    .toList();
            }
            case REQUEST_LOGS, PROMPTS ->
                List.of();
        };
    }

    private String formatRecords(List<Map<String, Object>> records, AiObservabilityExportFormat format) {
        if (records.isEmpty()) {
            return "";
        }

        return switch (format) {
            case JSON -> JsonUtils.write(records);
            case JSONL -> {
                StringBuilder stringBuilder = new StringBuilder();

                for (Map<String, Object> record : records) {
                    stringBuilder.append(JsonUtils.write(record));
                    stringBuilder.append('\n');
                }

                yield stringBuilder.toString();
            }
            case CSV -> {
                StringBuilder stringBuilder = new StringBuilder();
                List<String> headers = records.getFirst().keySet().stream().sorted().toList();

                stringBuilder.append(String.join(",", headers));
                stringBuilder.append('\n');

                for (Map<String, Object> record : records) {
                    List<String> values = headers.stream()
                        .map(header -> escapeCsvValue(String.valueOf(record.getOrDefault(header, ""))))
                        .toList();

                    stringBuilder.append(String.join(",", values));
                    stringBuilder.append('\n');
                }

                yield stringBuilder.toString();
            }
        };
    }

    private String escapeCsvValue(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }

    private String generateFilename(AiObservabilityExportJob exportJob) {
        String extension = switch (exportJob.getFormat()) {
            case CSV -> "csv";
            case JSON -> "json";
            case JSONL -> "jsonl";
        };

        return "export_" + exportJob.getScope().name().toLowerCase() + "_" + exportJob.getId() + "." + extension;
    }
}
```

Note: The `REQUEST_LOGS` and `PROMPTS` scopes return empty lists as placeholders. The `REQUEST_LOGS` scope will be connected to `AiGatewayRequestLogService` once the integration is wired, and `PROMPTS` scope will be connected after Phase 2 (Prompt Management) is implemented.

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/build.gradle.kts \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityExportExecutor.java
git commit -m "732 Add async export executor with file storage integration and CSV/JSON/JSONL formatting"
```

---

## Task 10: GraphQL Schema for Export Jobs and Webhook Subscriptions

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-export-job.graphqls`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-webhook-subscription.graphqls`

- [ ] **Step 1: Create ai-observability-export-job.graphqls**

```graphql
extend type Query {
    aiObservabilityExportJob(id: ID!): AiObservabilityExportJob
    aiObservabilityExportJobs(workspaceId: ID!): [AiObservabilityExportJob]
}

extend type Mutation {
    createAiObservabilityExportJob(
        workspaceId: ID!
        projectId: ID
        format: AiObservabilityExportFormat!
        scope: AiObservabilityExportScope!
        filters: String
    ): AiObservabilityExportJob
}

type AiObservabilityExportJob {
    createdBy: String!
    createdDate: Long
    errorMessage: String
    filePath: String
    filters: String
    format: AiObservabilityExportFormat!
    id: ID!
    projectId: ID
    recordCount: Int
    scope: AiObservabilityExportScope!
    status: AiObservabilityExportJobStatus!
    type: AiObservabilityExportJobType!
    workspaceId: ID!
}

enum AiObservabilityExportJobType {
    ON_DEMAND
    SCHEDULED
}

enum AiObservabilityExportFormat {
    CSV
    JSON
    JSONL
}

enum AiObservabilityExportScope {
    PROMPTS
    REQUEST_LOGS
    SESSIONS
    TRACES
}

enum AiObservabilityExportJobStatus {
    COMPLETED
    FAILED
    PENDING
    PROCESSING
}
```

- [ ] **Step 2: Create ai-observability-webhook-subscription.graphqls**

```graphql
extend type Query {
    aiObservabilityWebhookSubscription(id: ID!): AiObservabilityWebhookSubscription
    aiObservabilityWebhookSubscriptions(workspaceId: ID!): [AiObservabilityWebhookSubscription]
}

extend type Mutation {
    createAiObservabilityWebhookSubscription(
        workspaceId: ID!
        projectId: ID
        name: String!
        url: String!
        secret: String
        events: String!
        enabled: Boolean!
    ): AiObservabilityWebhookSubscription

    updateAiObservabilityWebhookSubscription(
        id: ID!
        name: String!
        url: String!
        secret: String
        events: String!
        enabled: Boolean!
    ): AiObservabilityWebhookSubscription

    deleteAiObservabilityWebhookSubscription(id: ID!): Boolean

    testAiObservabilityWebhookSubscription(id: ID!): Boolean
}

type AiObservabilityWebhookSubscription {
    createdDate: Long
    enabled: Boolean!
    events: String!
    id: ID!
    lastModifiedDate: Long
    lastTriggeredDate: Long
    name: String!
    projectId: ID
    url: String!
    version: Int
    workspaceId: ID!
}
```

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-export-job.graphqls \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-webhook-subscription.graphqls
git commit -m "732 Add GraphQL schema for export jobs and webhook subscriptions"
```

---

## Task 11: GraphQL Controllers

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiObservabilityExportJobGraphQlController.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiObservabilityWebhookSubscriptionGraphQlController.java`

- [ ] **Step 1: Create AiObservabilityExportJobGraphQlController**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportFormat;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportJob;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportJobType;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportScope;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityExportExecutor;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityExportJobService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiObservabilityExportJobGraphQlController {

    private final AiObservabilityExportExecutor aiObservabilityExportExecutor;
    private final AiObservabilityExportJobService aiObservabilityExportJobService;

    @SuppressFBWarnings("EI")
    AiObservabilityExportJobGraphQlController(
        AiObservabilityExportExecutor aiObservabilityExportExecutor,
        AiObservabilityExportJobService aiObservabilityExportJobService) {

        this.aiObservabilityExportExecutor = aiObservabilityExportExecutor;
        this.aiObservabilityExportJobService = aiObservabilityExportJobService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityExportJob aiObservabilityExportJob(@Argument long id) {
        return aiObservabilityExportJobService.getExportJob(id);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiObservabilityExportJob> aiObservabilityExportJobs(@Argument Long workspaceId) {
        return aiObservabilityExportJobService.getExportJobsByWorkspace(workspaceId);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityExportJob createAiObservabilityExportJob(
        @Argument Long workspaceId, @Argument Long projectId,
        @Argument AiObservabilityExportFormat format, @Argument AiObservabilityExportScope scope,
        @Argument String filters) {

        String currentUserLogin = SecurityUtils.getCurrentUserLogin();

        AiObservabilityExportJob exportJob = new AiObservabilityExportJob(
            workspaceId, AiObservabilityExportJobType.ON_DEMAND, format, scope, currentUserLogin);

        exportJob.setProjectId(projectId);
        exportJob.setFilters(filters);

        AiObservabilityExportJob savedExportJob = aiObservabilityExportJobService.create(exportJob);

        aiObservabilityExportExecutor.executeExport(savedExportJob.getId());

        return savedExportJob;
    }
}
```

- [ ] **Step 2: Create AiObservabilityWebhookSubscriptionGraphQlController**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityWebhookSubscription;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityWebhookDeliveryService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityWebhookSubscriptionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiObservabilityWebhookSubscriptionGraphQlController {

    private final AiObservabilityWebhookDeliveryService aiObservabilityWebhookDeliveryService;
    private final AiObservabilityWebhookSubscriptionService aiObservabilityWebhookSubscriptionService;

    @SuppressFBWarnings("EI")
    AiObservabilityWebhookSubscriptionGraphQlController(
        AiObservabilityWebhookDeliveryService aiObservabilityWebhookDeliveryService,
        AiObservabilityWebhookSubscriptionService aiObservabilityWebhookSubscriptionService) {

        this.aiObservabilityWebhookDeliveryService = aiObservabilityWebhookDeliveryService;
        this.aiObservabilityWebhookSubscriptionService = aiObservabilityWebhookSubscriptionService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityWebhookSubscription aiObservabilityWebhookSubscription(@Argument long id) {
        return aiObservabilityWebhookSubscriptionService.getWebhookSubscription(id);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiObservabilityWebhookSubscription> aiObservabilityWebhookSubscriptions(
        @Argument Long workspaceId) {

        return aiObservabilityWebhookSubscriptionService.getWebhookSubscriptionsByWorkspace(workspaceId);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityWebhookSubscription createAiObservabilityWebhookSubscription(
        @Argument Long workspaceId, @Argument Long projectId, @Argument String name,
        @Argument String url, @Argument String secret, @Argument String events,
        @Argument boolean enabled) {

        AiObservabilityWebhookSubscription subscription =
            new AiObservabilityWebhookSubscription(workspaceId, name, url, events);

        subscription.setProjectId(projectId);
        subscription.setSecret(secret);
        subscription.setEnabled(enabled);

        return aiObservabilityWebhookSubscriptionService.create(subscription);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityWebhookSubscription updateAiObservabilityWebhookSubscription(
        @Argument long id, @Argument String name, @Argument String url,
        @Argument String secret, @Argument String events, @Argument boolean enabled) {

        AiObservabilityWebhookSubscription subscription =
            aiObservabilityWebhookSubscriptionService.getWebhookSubscription(id);

        subscription.setName(name);
        subscription.setUrl(url);
        subscription.setSecret(secret);
        subscription.setEvents(events);
        subscription.setEnabled(enabled);

        return aiObservabilityWebhookSubscriptionService.update(subscription);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean deleteAiObservabilityWebhookSubscription(@Argument long id) {
        aiObservabilityWebhookSubscriptionService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean testAiObservabilityWebhookSubscription(@Argument long id) {
        aiObservabilityWebhookDeliveryService.deliverTestEvent(id);

        return true;
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-graphql:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiObservabilityExportJobGraphQlController.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiObservabilityWebhookSubscriptionGraphQlController.java
git commit -m "732 Add GraphQL controllers for export jobs and webhook subscriptions"
```

---

## Task 12: Client -- GraphQL Operations and Codegen

**Files:**
- Create: `client/src/graphql/automation/ai-gateway/aiObservabilityExportJobs.graphql`
- Create: `client/src/graphql/automation/ai-gateway/aiObservabilityWebhookSubscriptions.graphql`

- [ ] **Step 1: Create aiObservabilityExportJobs.graphql**

```graphql
query aiObservabilityExportJobs($workspaceId: ID!) {
    aiObservabilityExportJobs(workspaceId: $workspaceId) {
        createdBy
        createdDate
        errorMessage
        filePath
        filters
        format
        id
        projectId
        recordCount
        scope
        status
        type
        workspaceId
    }
}

query aiObservabilityExportJob($id: ID!) {
    aiObservabilityExportJob(id: $id) {
        createdBy
        createdDate
        errorMessage
        filePath
        filters
        format
        id
        projectId
        recordCount
        scope
        status
        type
        workspaceId
    }
}

mutation createAiObservabilityExportJob(
    $workspaceId: ID!
    $projectId: ID
    $format: AiObservabilityExportFormat!
    $scope: AiObservabilityExportScope!
    $filters: String
) {
    createAiObservabilityExportJob(
        workspaceId: $workspaceId
        projectId: $projectId
        format: $format
        scope: $scope
        filters: $filters
    ) {
        createdBy
        createdDate
        format
        id
        scope
        status
        type
        workspaceId
    }
}
```

- [ ] **Step 2: Create aiObservabilityWebhookSubscriptions.graphql**

```graphql
query aiObservabilityWebhookSubscriptions($workspaceId: ID!) {
    aiObservabilityWebhookSubscriptions(workspaceId: $workspaceId) {
        createdDate
        enabled
        events
        id
        lastModifiedDate
        lastTriggeredDate
        name
        projectId
        url
        version
        workspaceId
    }
}

query aiObservabilityWebhookSubscription($id: ID!) {
    aiObservabilityWebhookSubscription(id: $id) {
        createdDate
        enabled
        events
        id
        lastModifiedDate
        lastTriggeredDate
        name
        projectId
        url
        version
        workspaceId
    }
}

mutation createAiObservabilityWebhookSubscription(
    $workspaceId: ID!
    $projectId: ID
    $name: String!
    $url: String!
    $secret: String
    $events: String!
    $enabled: Boolean!
) {
    createAiObservabilityWebhookSubscription(
        workspaceId: $workspaceId
        projectId: $projectId
        name: $name
        url: $url
        secret: $secret
        events: $events
        enabled: $enabled
    ) {
        createdDate
        enabled
        events
        id
        name
        url
        version
        workspaceId
    }
}

mutation updateAiObservabilityWebhookSubscription(
    $id: ID!
    $name: String!
    $url: String!
    $secret: String
    $events: String!
    $enabled: Boolean!
) {
    updateAiObservabilityWebhookSubscription(
        id: $id
        name: $name
        url: $url
        secret: $secret
        events: $events
        enabled: $enabled
    ) {
        createdDate
        enabled
        events
        id
        name
        url
        version
        workspaceId
    }
}

mutation deleteAiObservabilityWebhookSubscription($id: ID!) {
    deleteAiObservabilityWebhookSubscription(id: $id)
}

mutation testAiObservabilityWebhookSubscription($id: ID!) {
    testAiObservabilityWebhookSubscription(id: $id)
}
```

- [ ] **Step 3: Run GraphQL codegen**

Run: `cd client && npx graphql-codegen`
Expected: generates updated `src/shared/middleware/graphql.ts` with new query and mutation hooks

- [ ] **Step 4: Commit**

```bash
cd client
git add src/graphql/automation/ai-gateway/aiObservabilityExportJobs.graphql \
  src/graphql/automation/ai-gateway/aiObservabilityWebhookSubscriptions.graphql \
  src/shared/middleware/graphql.ts
git commit -m "732 client - Add GraphQL operations and codegen for export jobs and webhook subscriptions"
```

---

## Task 13: Client -- Types and Sidebar Update

**Files:**
- Modify: `client/src/pages/automation/ai-gateway/types.ts`
- Modify: `client/src/pages/automation/ai-gateway/AiGateway.tsx`

- [ ] **Step 1: Add types to types.ts**

Add to `client/src/pages/automation/ai-gateway/types.ts`:

```typescript
import {
    AiObservabilityExportJobQuery,
    AiObservabilityExportJobsQuery,
    AiObservabilityWebhookSubscriptionQuery,
    AiObservabilityWebhookSubscriptionsQuery,
} from '@/shared/middleware/graphql';

// ... existing types unchanged ...

export type AiObservabilityExportJobType = NonNullable<
    NonNullable<AiObservabilityExportJobsQuery['aiObservabilityExportJobs']>[number]
>;

export type AiObservabilityExportJobDetailType = NonNullable<
    AiObservabilityExportJobQuery['aiObservabilityExportJob']
>;

export type AiObservabilityWebhookSubscriptionType = NonNullable<
    NonNullable<AiObservabilityWebhookSubscriptionsQuery['aiObservabilityWebhookSubscriptions']>[number]
>;

export type AiObservabilityWebhookSubscriptionDetailType = NonNullable<
    AiObservabilityWebhookSubscriptionQuery['aiObservabilityWebhookSubscription']
>;
```

- [ ] **Step 2: Update AiGateway.tsx sidebar**

In `client/src/pages/automation/ai-gateway/AiGateway.tsx`:

Update the type union to include `'exports'`:
```typescript
type AiGatewayPageType = 'budget' | 'exports' | 'models' | 'monitoring' | 'projects' | 'providers' | 'routing' | 'sessions' | 'settings' | 'traces';
```

Add import:
```typescript
import AiObservabilityExports from './components/exports/AiObservabilityExports';
```

Add `LeftSidebarNavItem` entry after the Sessions item (or after the last observability tab):
```typescript
<LeftSidebarNavItem
    item={{
        current: activePage === 'exports',
        name: 'Exports',
        onItemClick: () => setActivePage('exports'),
    }}
/>
```

Add conditional render:
```typescript
{activePage === 'exports' && <AiObservabilityExports />}
```

- [ ] **Step 3: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/types.ts src/pages/automation/ai-gateway/AiGateway.tsx
git commit -m "732 client - Add Exports sidebar tab to AI Gateway"
```

---

## Task 14: Client -- Exports Main Component

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/exports/AiObservabilityExports.tsx`

- [ ] **Step 1: Create AiObservabilityExports.tsx**

```typescript
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {useAiObservabilityExportJobsQuery, useAiObservabilityWebhookSubscriptionsQuery} from '@/shared/middleware/graphql';
import {useWorkspaceStore} from '@/shared/stores/workspace.store';
import {DownloadIcon, WebhookIcon} from 'lucide-react';
import {useState} from 'react';

import AiObservabilityExportJobDialog from './AiObservabilityExportJobDialog';
import AiObservabilityWebhookSubscriptions from './AiObservabilityWebhookSubscriptions';

type ExportsTabType = 'history' | 'webhooks';

const AiObservabilityExports = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [activeTab, setActiveTab] = useState<ExportsTabType>('history');
    const [showExportDialog, setShowExportDialog] = useState(false);

    const {data: exportJobsData, isLoading: exportJobsIsLoading} = useAiObservabilityExportJobsQuery({
        workspaceId: currentWorkspaceId + '',
    });

    const exportJobs = exportJobsData?.aiObservabilityExportJobs ?? [];

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <div className="mb-4 flex items-center justify-between">
                <h2 className="text-lg font-semibold">Exports</h2>

                <div className="flex gap-2">
                    <div className="flex gap-1">
                        <button
                            className={`rounded-md px-3 py-1 text-sm ${
                                activeTab === 'history'
                                    ? 'bg-primary text-primary-foreground'
                                    : 'bg-muted text-muted-foreground hover:bg-muted/80'
                            }`}
                            onClick={() => setActiveTab('history')}
                        >
                            Export History
                        </button>

                        <button
                            className={`rounded-md px-3 py-1 text-sm ${
                                activeTab === 'webhooks'
                                    ? 'bg-primary text-primary-foreground'
                                    : 'bg-muted text-muted-foreground hover:bg-muted/80'
                            }`}
                            onClick={() => setActiveTab('webhooks')}
                        >
                            Webhooks
                        </button>
                    </div>

                    {activeTab === 'history' && (
                        <button
                            className="rounded-md bg-primary px-3 py-1 text-sm text-primary-foreground hover:bg-primary/90"
                            onClick={() => setShowExportDialog(true)}
                        >
                            New Export
                        </button>
                    )}
                </div>
            </div>

            {activeTab === 'history' && (
                <>
                    {exportJobsIsLoading ? (
                        <PageLoader />
                    ) : exportJobs.length === 0 ? (
                        <EmptyList
                            icon={<DownloadIcon className="size-12 text-muted-foreground" />}
                            message="Create an export to download observability data in CSV, JSON, or JSONL format."
                            title="No Exports Yet"
                        />
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="w-full text-left text-sm">
                                <thead>
                                    <tr className="border-b text-muted-foreground">
                                        <th className="px-3 py-2 font-medium">Created</th>
                                        <th className="px-3 py-2 font-medium">Scope</th>
                                        <th className="px-3 py-2 font-medium">Format</th>
                                        <th className="px-3 py-2 font-medium">Status</th>
                                        <th className="px-3 py-2 font-medium">Records</th>
                                        <th className="px-3 py-2 font-medium">Created By</th>
                                        <th className="px-3 py-2 font-medium">Download</th>
                                    </tr>
                                </thead>

                                <tbody>
                                    {exportJobs.map((exportJob) => (
                                        <tr className="border-b" key={exportJob.id}>
                                            <td className="px-3 py-2 text-muted-foreground">
                                                {exportJob.createdDate
                                                    ? new Date(Number(exportJob.createdDate)).toLocaleString()
                                                    : '-'}
                                            </td>
                                            <td className="px-3 py-2">{exportJob.scope}</td>
                                            <td className="px-3 py-2">{exportJob.format}</td>
                                            <td className="px-3 py-2">
                                                <span
                                                    className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                                                        exportJob.status === 'COMPLETED'
                                                            ? 'bg-green-100 text-green-800'
                                                            : exportJob.status === 'FAILED'
                                                              ? 'bg-red-100 text-red-800'
                                                              : exportJob.status === 'PROCESSING'
                                                                ? 'bg-blue-100 text-blue-800'
                                                                : 'bg-yellow-100 text-yellow-800'
                                                    }`}
                                                >
                                                    {exportJob.status}
                                                </span>
                                            </td>
                                            <td className="px-3 py-2">
                                                {exportJob.recordCount != null ? exportJob.recordCount : '-'}
                                            </td>
                                            <td className="px-3 py-2">{exportJob.createdBy}</td>
                                            <td className="px-3 py-2">
                                                {exportJob.status === 'COMPLETED' && exportJob.filePath ? (
                                                    <a
                                                        className="text-primary hover:underline"
                                                        href={exportJob.filePath}
                                                    >
                                                        Download
                                                    </a>
                                                ) : exportJob.status === 'FAILED' && exportJob.errorMessage ? (
                                                    <span
                                                        className="text-xs text-red-600"
                                                        title={exportJob.errorMessage}
                                                    >
                                                        Error
                                                    </span>
                                                ) : (
                                                    '-'
                                                )}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}

                    {showExportDialog && (
                        <AiObservabilityExportJobDialog onClose={() => setShowExportDialog(false)} />
                    )}
                </>
            )}

            {activeTab === 'webhooks' && <AiObservabilityWebhookSubscriptions />}
        </div>
    );
};

export default AiObservabilityExports;
```

- [ ] **Step 2: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/exports/AiObservabilityExports.tsx
git commit -m "732 client - Add Exports main component with export history table and tab navigation"
```

---

## Task 15: Client -- Export Job Dialog

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/exports/AiObservabilityExportJobDialog.tsx`

- [ ] **Step 1: Create AiObservabilityExportJobDialog.tsx**

```typescript
import {
    AiObservabilityExportFormat,
    AiObservabilityExportScope,
    useCreateAiObservabilityExportJobMutation,
} from '@/shared/middleware/graphql';
import {useWorkspaceStore} from '@/shared/stores/workspace.store';
import {XIcon} from 'lucide-react';
import {useState} from 'react';

interface AiObservabilityExportJobDialogProps {
    onClose: () => void;
}

const SCOPE_OPTIONS: {label: string; value: AiObservabilityExportScope}[] = [
    {label: 'Traces', value: AiObservabilityExportScope.Traces},
    {label: 'Request Logs', value: AiObservabilityExportScope.RequestLogs},
    {label: 'Sessions', value: AiObservabilityExportScope.Sessions},
    {label: 'Prompts', value: AiObservabilityExportScope.Prompts},
];

const FORMAT_OPTIONS: {label: string; value: AiObservabilityExportFormat}[] = [
    {label: 'CSV', value: AiObservabilityExportFormat.Csv},
    {label: 'JSON', value: AiObservabilityExportFormat.Json},
    {label: 'JSONL', value: AiObservabilityExportFormat.Jsonl},
];

const AiObservabilityExportJobDialog = ({onClose}: AiObservabilityExportJobDialogProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [format, setFormat] = useState<AiObservabilityExportFormat>(AiObservabilityExportFormat.Json);
    const [scope, setScope] = useState<AiObservabilityExportScope>(AiObservabilityExportScope.Traces);

    const createExportJobMutation = useCreateAiObservabilityExportJobMutation({});

    const handleCreate = () => {
        createExportJobMutation.mutate(
            {
                format: format,
                scope: scope,
                workspaceId: currentWorkspaceId + '',
            },
            {
                onSuccess: () => {
                    onClose();
                },
            },
        );
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-md rounded-lg bg-background p-6 shadow-lg">
                <div className="mb-4 flex items-center justify-between">
                    <h3 className="text-lg font-semibold">New Export</h3>

                    <button className="text-muted-foreground hover:text-foreground" onClick={onClose}>
                        <XIcon className="size-5" />
                    </button>
                </div>

                <fieldset className="mb-4 border-0 p-0">
                    <label className="mb-1 block text-sm font-medium">Scope</label>

                    <select
                        className="w-full rounded-md border bg-background px-3 py-2 text-sm"
                        onChange={(event) => setScope(event.target.value as AiObservabilityExportScope)}
                        value={scope}
                    >
                        {SCOPE_OPTIONS.map((option) => (
                            <option key={option.value} value={option.value}>
                                {option.label}
                            </option>
                        ))}
                    </select>
                </fieldset>

                <fieldset className="mb-6 border-0 p-0">
                    <label className="mb-1 block text-sm font-medium">Format</label>

                    <select
                        className="w-full rounded-md border bg-background px-3 py-2 text-sm"
                        onChange={(event) => setFormat(event.target.value as AiObservabilityExportFormat)}
                        value={format}
                    >
                        {FORMAT_OPTIONS.map((option) => (
                            <option key={option.value} value={option.value}>
                                {option.label}
                            </option>
                        ))}
                    </select>
                </fieldset>

                <div className="flex justify-end gap-2">
                    <button
                        className="rounded-md bg-muted px-4 py-2 text-sm text-muted-foreground hover:bg-muted/80"
                        onClick={onClose}
                    >
                        Cancel
                    </button>

                    <button
                        className="rounded-md bg-primary px-4 py-2 text-sm text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
                        disabled={createExportJobMutation.isPending}
                        onClick={handleCreate}
                    >
                        {createExportJobMutation.isPending ? 'Creating...' : 'Create Export'}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default AiObservabilityExportJobDialog;
```

- [ ] **Step 2: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/exports/AiObservabilityExportJobDialog.tsx
git commit -m "732 client - Add export job creation dialog with scope and format selection"
```

---

## Task 16: Client -- Webhook Subscriptions Components

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/exports/AiObservabilityWebhookSubscriptions.tsx`
- Create: `client/src/pages/automation/ai-gateway/components/exports/AiObservabilityWebhookSubscriptionDialog.tsx`

- [ ] **Step 1: Create AiObservabilityWebhookSubscriptions.tsx**

```typescript
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {
    useAiObservabilityWebhookSubscriptionsQuery,
    useDeleteAiObservabilityWebhookSubscriptionMutation,
    useTestAiObservabilityWebhookSubscriptionMutation,
} from '@/shared/middleware/graphql';
import {useWorkspaceStore} from '@/shared/stores/workspace.store';
import {PlayIcon, PencilIcon, Trash2Icon, WebhookIcon} from 'lucide-react';
import {useState} from 'react';

import {AiObservabilityWebhookSubscriptionType} from '../../types';
import AiObservabilityWebhookSubscriptionDialog from './AiObservabilityWebhookSubscriptionDialog';

const AiObservabilityWebhookSubscriptions = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [editingSubscription, setEditingSubscription] = useState<
        AiObservabilityWebhookSubscriptionType | undefined
    >();
    const [showCreateDialog, setShowCreateDialog] = useState(false);

    const {data: subscriptionsData, isLoading: subscriptionsIsLoading} =
        useAiObservabilityWebhookSubscriptionsQuery({
            workspaceId: currentWorkspaceId + '',
        });

    const deleteSubscriptionMutation = useDeleteAiObservabilityWebhookSubscriptionMutation({});
    const testSubscriptionMutation = useTestAiObservabilityWebhookSubscriptionMutation({});

    const subscriptions = subscriptionsData?.aiObservabilityWebhookSubscriptions ?? [];

    const handleDelete = (subscriptionId: string) => {
        deleteSubscriptionMutation.mutate({id: subscriptionId});
    };

    const handleTest = (subscriptionId: string) => {
        testSubscriptionMutation.mutate({id: subscriptionId});
    };

    return (
        <div>
            <div className="mb-4 flex justify-end">
                <button
                    className="rounded-md bg-primary px-3 py-1 text-sm text-primary-foreground hover:bg-primary/90"
                    onClick={() => setShowCreateDialog(true)}
                >
                    New Webhook
                </button>
            </div>

            {subscriptionsIsLoading ? (
                <PageLoader />
            ) : subscriptions.length === 0 ? (
                <EmptyList
                    icon={<WebhookIcon className="size-12 text-muted-foreground" />}
                    message="Subscribe to events like trace.completed, alert.triggered, and budget.exceeded."
                    title="No Webhook Subscriptions"
                />
            ) : (
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead>
                            <tr className="border-b text-muted-foreground">
                                <th className="px-3 py-2 font-medium">Name</th>
                                <th className="px-3 py-2 font-medium">URL</th>
                                <th className="px-3 py-2 font-medium">Events</th>
                                <th className="px-3 py-2 font-medium">Enabled</th>
                                <th className="px-3 py-2 font-medium">Last Triggered</th>
                                <th className="px-3 py-2 font-medium">Actions</th>
                            </tr>
                        </thead>

                        <tbody>
                            {subscriptions.map((subscription) => (
                                <tr className="border-b" key={subscription.id}>
                                    <td className="px-3 py-2 font-medium">{subscription.name}</td>
                                    <td className="max-w-xs truncate px-3 py-2 text-muted-foreground">
                                        {subscription.url}
                                    </td>
                                    <td className="px-3 py-2">
                                        <div className="flex flex-wrap gap-1">
                                            {JSON.parse(subscription.events).map((event: string) => (
                                                <span
                                                    className="rounded-full bg-muted px-2 py-0.5 text-xs"
                                                    key={event}
                                                >
                                                    {event}
                                                </span>
                                            ))}
                                        </div>
                                    </td>
                                    <td className="px-3 py-2">
                                        <span
                                            className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                                                subscription.enabled
                                                    ? 'bg-green-100 text-green-800'
                                                    : 'bg-gray-100 text-gray-800'
                                            }`}
                                        >
                                            {subscription.enabled ? 'Active' : 'Disabled'}
                                        </span>
                                    </td>
                                    <td className="px-3 py-2 text-muted-foreground">
                                        {subscription.lastTriggeredDate
                                            ? new Date(
                                                  Number(subscription.lastTriggeredDate),
                                              ).toLocaleString()
                                            : 'Never'}
                                    </td>
                                    <td className="px-3 py-2">
                                        <div className="flex gap-1">
                                            <button
                                                className="rounded p-1 text-muted-foreground hover:bg-muted hover:text-foreground"
                                                onClick={() => handleTest(subscription.id)}
                                                title="Test webhook"
                                            >
                                                <PlayIcon className="size-4" />
                                            </button>

                                            <button
                                                className="rounded p-1 text-muted-foreground hover:bg-muted hover:text-foreground"
                                                onClick={() => setEditingSubscription(subscription)}
                                                title="Edit"
                                            >
                                                <PencilIcon className="size-4" />
                                            </button>

                                            <button
                                                className="rounded p-1 text-muted-foreground hover:bg-muted hover:text-red-600"
                                                onClick={() => handleDelete(subscription.id)}
                                                title="Delete"
                                            >
                                                <Trash2Icon className="size-4" />
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {(showCreateDialog || editingSubscription) && (
                <AiObservabilityWebhookSubscriptionDialog
                    onClose={() => {
                        setShowCreateDialog(false);
                        setEditingSubscription(undefined);
                    }}
                    subscription={editingSubscription}
                />
            )}
        </div>
    );
};

export default AiObservabilityWebhookSubscriptions;
```

- [ ] **Step 2: Create AiObservabilityWebhookSubscriptionDialog.tsx**

```typescript
import {
    useCreateAiObservabilityWebhookSubscriptionMutation,
    useUpdateAiObservabilityWebhookSubscriptionMutation,
} from '@/shared/middleware/graphql';
import {useWorkspaceStore} from '@/shared/stores/workspace.store';
import {XIcon} from 'lucide-react';
import {useState} from 'react';

import {AiObservabilityWebhookSubscriptionType} from '../../types';

interface AiObservabilityWebhookSubscriptionDialogProps {
    onClose: () => void;
    subscription?: AiObservabilityWebhookSubscriptionType;
}

const AVAILABLE_EVENTS = ['alert.triggered', 'budget.exceeded', 'trace.completed'];

const AiObservabilityWebhookSubscriptionDialog = ({
    onClose,
    subscription,
}: AiObservabilityWebhookSubscriptionDialogProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [enabled, setEnabled] = useState(subscription?.enabled ?? true);
    const [name, setName] = useState(subscription?.name ?? '');
    const [secret, setSecret] = useState('');
    const [selectedEvents, setSelectedEvents] = useState<string[]>(
        subscription ? JSON.parse(subscription.events) : [],
    );
    const [url, setUrl] = useState(subscription?.url ?? '');

    const createMutation = useCreateAiObservabilityWebhookSubscriptionMutation({});
    const updateMutation = useUpdateAiObservabilityWebhookSubscriptionMutation({});

    const isEditing = !!subscription;
    const isPending = createMutation.isPending || updateMutation.isPending;

    const handleEventToggle = (event: string) => {
        setSelectedEvents((previousEvents) =>
            previousEvents.includes(event)
                ? previousEvents.filter((previousEvent) => previousEvent !== event)
                : [...previousEvents, event],
        );
    };

    const handleSave = () => {
        const eventsJson = JSON.stringify(selectedEvents);

        if (isEditing) {
            updateMutation.mutate(
                {
                    enabled,
                    events: eventsJson,
                    id: subscription.id,
                    name,
                    secret: secret || undefined,
                    url,
                },
                {
                    onSuccess: () => {
                        onClose();
                    },
                },
            );
        } else {
            createMutation.mutate(
                {
                    enabled,
                    events: eventsJson,
                    name,
                    secret: secret || undefined,
                    url,
                    workspaceId: currentWorkspaceId + '',
                },
                {
                    onSuccess: () => {
                        onClose();
                    },
                },
            );
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-md rounded-lg bg-background p-6 shadow-lg">
                <div className="mb-4 flex items-center justify-between">
                    <h3 className="text-lg font-semibold">
                        {isEditing ? 'Edit Webhook' : 'New Webhook Subscription'}
                    </h3>

                    <button className="text-muted-foreground hover:text-foreground" onClick={onClose}>
                        <XIcon className="size-5" />
                    </button>
                </div>

                <fieldset className="mb-3 border-0 p-0">
                    <label className="mb-1 block text-sm font-medium">Name</label>

                    <input
                        className="w-full rounded-md border bg-background px-3 py-2 text-sm"
                        onChange={(event) => setName(event.target.value)}
                        placeholder="My Webhook"
                        value={name}
                    />
                </fieldset>

                <fieldset className="mb-3 border-0 p-0">
                    <label className="mb-1 block text-sm font-medium">URL</label>

                    <input
                        className="w-full rounded-md border bg-background px-3 py-2 text-sm"
                        onChange={(event) => setUrl(event.target.value)}
                        placeholder="https://example.com/webhook"
                        value={url}
                    />
                </fieldset>

                <fieldset className="mb-3 border-0 p-0">
                    <label className="mb-1 block text-sm font-medium">
                        Secret (HMAC-SHA256)
                    </label>

                    <input
                        className="w-full rounded-md border bg-background px-3 py-2 text-sm"
                        onChange={(event) => setSecret(event.target.value)}
                        placeholder={isEditing ? 'Leave empty to keep existing' : 'Optional signing secret'}
                        type="password"
                        value={secret}
                    />
                </fieldset>

                <fieldset className="mb-3 border-0 p-0">
                    <label className="mb-1 block text-sm font-medium">Events</label>

                    <div className="flex flex-wrap gap-2">
                        {AVAILABLE_EVENTS.map((event) => (
                            <label className="flex items-center gap-1.5 text-sm" key={event}>
                                <input
                                    checked={selectedEvents.includes(event)}
                                    onChange={() => handleEventToggle(event)}
                                    type="checkbox"
                                />
                                {event}
                            </label>
                        ))}
                    </div>
                </fieldset>

                <fieldset className="mb-6 border-0 p-0">
                    <label className="flex items-center gap-2 text-sm">
                        <input
                            checked={enabled}
                            onChange={(event) => setEnabled(event.target.checked)}
                            type="checkbox"
                        />
                        Enabled
                    </label>
                </fieldset>

                <div className="flex justify-end gap-2">
                    <button
                        className="rounded-md bg-muted px-4 py-2 text-sm text-muted-foreground hover:bg-muted/80"
                        onClick={onClose}
                    >
                        Cancel
                    </button>

                    <button
                        className="rounded-md bg-primary px-4 py-2 text-sm text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
                        disabled={isPending || !name || !url || selectedEvents.length === 0}
                        onClick={handleSave}
                    >
                        {isPending ? 'Saving...' : isEditing ? 'Update' : 'Create'}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default AiObservabilityWebhookSubscriptionDialog;
```

- [ ] **Step 3: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/exports/AiObservabilityWebhookSubscriptions.tsx \
  src/pages/automation/ai-gateway/components/exports/AiObservabilityWebhookSubscriptionDialog.tsx
git commit -m "732 client - Add webhook subscription list and create/edit dialog components"
```

---

## Task 17: Client -- Verification and Final Checks

- [ ] **Step 1: Run client lint and typecheck**

```bash
cd client
npm run check
```

Expected: All lint, typecheck, and test checks pass. Fix any ESLint sort-keys violations, naming conventions, or import order issues.

- [ ] **Step 2: Run server compilation**

```bash
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava && \
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava && \
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-graphql:compileJava
```

Expected: BUILD SUCCESSFUL for all three modules.

- [ ] **Step 3: Run spotless formatting**

```bash
./gradlew spotlessApply
```

Fix any formatting violations, then commit if there are changes:

```bash
git add -u
git commit -m "732 Apply spotless formatting to Phase 6 export files"
```
