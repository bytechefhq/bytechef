# Phase 3: Alerting & Notifications — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add threshold-based alerting on AI Gateway metrics (error rate, latency P95, cost, token usage, request volume) with multi-channel notifications (webhook, email, Slack) and alert history tracking.

**Architecture:** New domain entities (`AiObservabilityAlertRule`, `AiObservabilityNotificationChannel`, `AiObservabilityAlertRuleChannel`, `AiObservabilityAlertEvent`) in the existing `automation-ai-gateway` module. A new `AlertScheduler` interface in `platform-scheduler-api` (Apache 2.0 licensed) follows the `TriggerScheduler` pattern. A Quartz-based implementation in `platform-scheduler-impl` schedules periodic alert evaluation. The gateway service module contains `AiObservabilityAlertEvaluator` which queries request log data within the configured time window, checks thresholds, enforces cooldown, creates alert events, and dispatches notifications. GraphQL exposes CRUD for alert rules and notification channels plus alert event queries. A new "Alerts" sidebar tab in the client provides the management UI.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JDBC, PostgreSQL, Liquibase, Quartz Scheduler, GraphQL (Spring GraphQL), React 19, TypeScript 5.9, TanStack Query, Tailwind CSS

**Reference spec:** `docs/superpowers/specs/2026-04-11-ai-gateway-observability-platform-design.md` — Phase 3 section

**Prerequisites:** Phase 1 (Tracing & Sessions) must be implemented first — the `00000000000002_ai_observability_init.xml` Liquibase file must already exist.

---

## File Map

### Server — Platform Scheduler API (`server/libs/platform/platform-scheduler/platform-scheduler-api/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `src/main/java/com/bytechef/platform/scheduler/AlertScheduler.java` | Alert scheduler interface |

### Server — Platform Scheduler Impl (`server/libs/platform/platform-scheduler/platform-scheduler-impl/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `src/main/java/com/bytechef/platform/scheduler/QuartzAlertScheduler.java` | Quartz-based alert scheduler |
| Create | `src/main/java/com/bytechef/platform/scheduler/job/AlertEvaluationJob.java` | Quartz job for alert tick |
| Modify | `src/main/java/com/bytechef/platform/scheduler/config/QuartzTriggerSchedulerConfiguration.java` | Add `AlertScheduler` bean |

### Server — API module (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `src/main/java/.../domain/AiObservabilityAlertRule.java` | Alert rule domain entity |
| Create | `src/main/java/.../domain/AiObservabilityAlertMetric.java` | Enum: ERROR_RATE, LATENCY_P95, COST, TOKEN_USAGE, REQUEST_VOLUME |
| Create | `src/main/java/.../domain/AiObservabilityAlertCondition.java` | Enum: GREATER_THAN, LESS_THAN, EQUALS |
| Create | `src/main/java/.../domain/AiObservabilityAlertRuleChannel.java` | Join table entity |
| Create | `src/main/java/.../domain/AiObservabilityNotificationChannel.java` | Notification channel domain entity |
| Create | `src/main/java/.../domain/AiObservabilityNotificationChannelType.java` | Enum: WEBHOOK, EMAIL, SLACK |
| Create | `src/main/java/.../domain/AiObservabilityAlertEvent.java` | Alert event domain entity |
| Create | `src/main/java/.../domain/AiObservabilityAlertEventStatus.java` | Enum: TRIGGERED, RESOLVED, ACKNOWLEDGED |
| Create | `src/main/java/.../repository/AiObservabilityAlertRuleRepository.java` | Alert rule repository |
| Create | `src/main/java/.../repository/AiObservabilityNotificationChannelRepository.java` | Notification channel repository |
| Create | `src/main/java/.../repository/AiObservabilityAlertEventRepository.java` | Alert event repository |
| Create | `src/main/java/.../service/AiObservabilityAlertRuleService.java` | Alert rule service interface |
| Create | `src/main/java/.../service/AiObservabilityNotificationChannelService.java` | Notification channel service interface |
| Create | `src/main/java/.../service/AiObservabilityAlertEventService.java` | Alert event service interface |

### Server — Service module (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/`)

| Action | Path | Purpose |
|---|---|---|
| Modify | `src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000002_ai_observability_init.xml` | Add Phase 3 changesets |
| Create | `src/main/java/.../service/AiObservabilityAlertRuleServiceImpl.java` | Alert rule service impl |
| Create | `src/main/java/.../service/AiObservabilityNotificationChannelServiceImpl.java` | Notification channel service impl |
| Create | `src/main/java/.../service/AiObservabilityAlertEventServiceImpl.java` | Alert event service impl |
| Create | `src/main/java/.../service/AiObservabilityAlertEvaluator.java` | Alert evaluation logic |
| Create | `src/main/java/.../service/AiObservabilityNotificationDispatcher.java` | Multi-channel notification sender |

### Server — GraphQL module (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `src/main/resources/graphql/ai-observability-alert-rule.graphqls` | Alert rule GraphQL schema |
| Create | `src/main/resources/graphql/ai-observability-notification-channel.graphqls` | Notification channel GraphQL schema |
| Create | `src/main/resources/graphql/ai-observability-alert-event.graphqls` | Alert event GraphQL schema |
| Create | `src/main/java/.../web/graphql/AiObservabilityAlertRuleGraphQlController.java` | Alert rule CRUD controller |
| Create | `src/main/java/.../web/graphql/AiObservabilityNotificationChannelGraphQlController.java` | Notification channel CRUD controller |
| Create | `src/main/java/.../web/graphql/AiObservabilityAlertEventGraphQlController.java` | Alert event query controller |

### Client (`client/src/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `graphql/automation/ai-gateway/aiObservabilityAlertRules.graphql` | Alert rule queries/mutations |
| Create | `graphql/automation/ai-gateway/aiObservabilityNotificationChannels.graphql` | Notification channel queries/mutations |
| Create | `graphql/automation/ai-gateway/aiObservabilityAlertEvents.graphql` | Alert event queries |
| Modify | `pages/automation/ai-gateway/types.ts` | Add alert/notification/event types |
| Modify | `pages/automation/ai-gateway/AiGateway.tsx` | Add Alerts sidebar tab |
| Create | `pages/automation/ai-gateway/components/alerts/AiObservabilityAlerts.tsx` | Alerts page with tabs |
| Create | `pages/automation/ai-gateway/components/alerts/AiObservabilityAlertRuleDialog.tsx` | Create/edit alert rule dialog |
| Create | `pages/automation/ai-gateway/components/alerts/AiObservabilityAlertRuleDeleteDialog.tsx` | Delete confirmation dialog |
| Create | `pages/automation/ai-gateway/components/alerts/AiObservabilityNotificationChannels.tsx` | Notification channels sub-section |
| Create | `pages/automation/ai-gateway/components/alerts/AiObservabilityNotificationChannelDialog.tsx` | Create/edit notification channel dialog |
| Create | `pages/automation/ai-gateway/components/alerts/AiObservabilityAlertHistory.tsx` | Alert history timeline |

---

## Task 1: Liquibase Migration — Phase 3 Tables

**Files:**
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000002_ai_observability_init.xml`

- [ ] **Step 1: Add Phase 3 changesets to the existing migration file**

Append the following changesets inside the `<databaseChangeLog>` element, after the existing Phase 1 changeset:

```xml
    <changeSet id="00000000000002-10" author="Ivica Cardic">
        <preConditions onFail="MARK_RAN">
            <not>
                <tableExists tableName="ai_observability_alert_rule"/>
            </not>
        </preConditions>

        <createTable tableName="ai_observability_alert_rule">
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
            <column name="metric" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="condition" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="threshold" type="DECIMAL(12,4)">
                <constraints nullable="false"/>
            </column>
            <column name="window_minutes" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="cooldown_minutes" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="filters" type="TEXT"/>
            <column name="enabled" type="BOOLEAN">
                <constraints nullable="false"/>
            </column>
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

        <createIndex tableName="ai_observability_alert_rule" indexName="idx_ai_obs_alert_rule_workspace">
            <column name="workspace_id"/>
        </createIndex>

        <createIndex tableName="ai_observability_alert_rule" indexName="idx_ai_obs_alert_rule_enabled">
            <column name="enabled"/>
        </createIndex>
    </changeSet>

    <changeSet id="00000000000002-11" author="Ivica Cardic">
        <preConditions onFail="MARK_RAN">
            <not>
                <tableExists tableName="ai_observability_notification_channel"/>
            </not>
        </preConditions>

        <createTable tableName="ai_observability_notification_channel">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="workspace_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="name" type="VARCHAR(256)">
                <constraints nullable="false"/>
            </column>
            <column name="type" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="config" type="TEXT">
                <constraints nullable="false"/>
            </column>
            <column name="enabled" type="BOOLEAN">
                <constraints nullable="false"/>
            </column>
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

        <createIndex tableName="ai_observability_notification_channel" indexName="idx_ai_obs_notif_channel_workspace">
            <column name="workspace_id"/>
        </createIndex>
    </changeSet>

    <changeSet id="00000000000002-12" author="Ivica Cardic">
        <preConditions onFail="MARK_RAN">
            <not>
                <tableExists tableName="ai_observability_alert_rule_channel"/>
            </not>
        </preConditions>

        <createTable tableName="ai_observability_alert_rule_channel">
            <column name="ai_observability_alert_rule" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="notification_channel_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addForeignKeyConstraint constraintName="fk_ai_obs_arc_alert_rule"
                                 baseTableName="ai_observability_alert_rule_channel" baseColumnNames="ai_observability_alert_rule"
                                 referencedTableName="ai_observability_alert_rule" referencedColumnNames="id"/>

        <addForeignKeyConstraint constraintName="fk_ai_obs_arc_notif_channel"
                                 baseTableName="ai_observability_alert_rule_channel" baseColumnNames="notification_channel_id"
                                 referencedTableName="ai_observability_notification_channel" referencedColumnNames="id"/>
    </changeSet>

    <changeSet id="00000000000002-13" author="Ivica Cardic">
        <preConditions onFail="MARK_RAN">
            <not>
                <tableExists tableName="ai_observability_alert_event"/>
            </not>
        </preConditions>

        <createTable tableName="ai_observability_alert_event">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="alert_rule_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="triggered_value" type="DECIMAL(12,4)"/>
            <column name="message" type="TEXT"/>
            <column name="status" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="created_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addForeignKeyConstraint constraintName="fk_ai_obs_alert_event_rule"
                                 baseTableName="ai_observability_alert_event" baseColumnNames="alert_rule_id"
                                 referencedTableName="ai_observability_alert_rule" referencedColumnNames="id"/>

        <createIndex tableName="ai_observability_alert_event" indexName="idx_ai_obs_alert_event_rule">
            <column name="alert_rule_id"/>
        </createIndex>

        <createIndex tableName="ai_observability_alert_event" indexName="idx_ai_obs_alert_event_created">
            <column name="created_date"/>
        </createIndex>
    </changeSet>
```

- [ ] **Step 2: Verify the migration compiles**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000002_ai_observability_init.xml
git commit -m "732 Add Liquibase migration for alerting tables (alert_rule, notification_channel, alert_event)"
```

---

## Task 2: Enum Domain Classes

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityAlertMetric.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityAlertCondition.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityNotificationChannelType.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityAlertEventStatus.java`

- [ ] **Step 1: Create AiObservabilityAlertMetric**

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
public enum AiObservabilityAlertMetric {

    ERROR_RATE,
    LATENCY_P95,
    COST,
    TOKEN_USAGE,
    REQUEST_VOLUME
}
```

- [ ] **Step 2: Create AiObservabilityAlertCondition**

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
public enum AiObservabilityAlertCondition {

    GREATER_THAN,
    LESS_THAN,
    EQUALS
}
```

- [ ] **Step 3: Create AiObservabilityNotificationChannelType**

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
public enum AiObservabilityNotificationChannelType {

    WEBHOOK,
    EMAIL,
    SLACK
}
```

- [ ] **Step 4: Create AiObservabilityAlertEventStatus**

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
public enum AiObservabilityAlertEventStatus {

    TRIGGERED,
    RESOLVED,
    ACKNOWLEDGED
}
```

- [ ] **Step 5: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityAlertMetric.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityAlertCondition.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityNotificationChannelType.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityAlertEventStatus.java
git commit -m "732 Add alerting enum types (metric, condition, channel type, event status)"
```

---

## Task 3: AlertRule Domain Entity

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityAlertRuleChannel.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityAlertRule.java`

- [ ] **Step 1: Create AiObservabilityAlertRuleChannel**

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
@Table("ai_observability_alert_rule_channel")
public record AiObservabilityAlertRuleChannel(long notificationChannelId) {
}
```

- [ ] **Step 2: Create AiObservabilityAlertRule**

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
@Table("ai_observability_alert_rule")
public class AiObservabilityAlertRule {

    @MappedCollection(idColumn = "ai_observability_alert_rule")
    private Set<AiObservabilityAlertRuleChannel> channels = new HashSet<>();

    @Column("condition")
    private int condition;

    @Column("cooldown_minutes")
    private int cooldownMinutes;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private boolean enabled;

    @Column
    private String filters;

    @Id
    private Long id;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column
    private int metric;

    @Column
    private String name;

    @Column("project_id")
    private Long projectId;

    @Column
    private BigDecimal threshold;

    @Version
    private int version;

    @Column("window_minutes")
    private int windowMinutes;

    @Column("workspace_id")
    private Long workspaceId;

    private AiObservabilityAlertRule() {
    }

    public AiObservabilityAlertRule(Long workspaceId, String name, AiObservabilityAlertMetric metric,
        AiObservabilityAlertCondition condition, BigDecimal threshold, int windowMinutes, int cooldownMinutes) {

        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notBlank(name, "name must not be blank");
        Validate.notNull(metric, "metric must not be null");
        Validate.notNull(condition, "condition must not be null");
        Validate.notNull(threshold, "threshold must not be null");

        this.condition = condition.ordinal();
        this.cooldownMinutes = cooldownMinutes;
        this.enabled = true;
        this.metric = metric.ordinal();
        this.name = name;
        this.threshold = threshold;
        this.windowMinutes = windowMinutes;
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilityAlertRule aiObservabilityAlertRule)) {
            return false;
        }

        return Objects.equals(id, aiObservabilityAlertRule.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Set<AiObservabilityAlertRuleChannel> getChannels() {
        return channels;
    }

    public AiObservabilityAlertCondition getCondition() {
        return AiObservabilityAlertCondition.values()[condition];
    }

    public int getCooldownMinutes() {
        return cooldownMinutes;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getFilters() {
        return filters;
    }

    public Long getId() {
        return id;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public AiObservabilityAlertMetric getMetric() {
        return AiObservabilityAlertMetric.values()[metric];
    }

    public String getName() {
        return name;
    }

    public Long getProjectId() {
        return projectId;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public int getVersion() {
        return version;
    }

    public int getWindowMinutes() {
        return windowMinutes;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setChannels(Set<AiObservabilityAlertRuleChannel> channels) {
        this.channels = channels;
    }

    public void setCondition(AiObservabilityAlertCondition condition) {
        Validate.notNull(condition, "condition must not be null");

        this.condition = condition.ordinal();
    }

    public void setCooldownMinutes(int cooldownMinutes) {
        this.cooldownMinutes = cooldownMinutes;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setFilters(String filters) {
        this.filters = filters;
    }

    public void setMetric(AiObservabilityAlertMetric metric) {
        Validate.notNull(metric, "metric must not be null");

        this.metric = metric.ordinal();
    }

    public void setName(String name) {
        Validate.notBlank(name, "name must not be blank");

        this.name = name;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setThreshold(BigDecimal threshold) {
        Validate.notNull(threshold, "threshold must not be null");

        this.threshold = threshold;
    }

    public void setWindowMinutes(int windowMinutes) {
        this.windowMinutes = windowMinutes;
    }

    @Override
    public String toString() {
        return "AiObservabilityAlertRule{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", name='" + name + '\'' +
            ", metric=" + getMetric() +
            ", enabled=" + enabled +
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
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityAlertRuleChannel.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityAlertRule.java
git commit -m "732 Add AiObservabilityAlertRule and AiObservabilityAlertRuleChannel domain entities"
```

---

## Task 4: NotificationChannel Domain Entity

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityNotificationChannel.java`

- [ ] **Step 1: Create AiObservabilityNotificationChannel**

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
@Table("ai_observability_notification_channel")
public class AiObservabilityNotificationChannel {

    @Column
    private String config;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private boolean enabled;

    @Id
    private Long id;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column
    private String name;

    @Column
    private int type;

    @Version
    private int version;

    @Column("workspace_id")
    private Long workspaceId;

    private AiObservabilityNotificationChannel() {
    }

    public AiObservabilityNotificationChannel(
        Long workspaceId, String name, AiObservabilityNotificationChannelType type, String config) {

        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notBlank(name, "name must not be blank");
        Validate.notNull(type, "type must not be null");
        Validate.notBlank(config, "config must not be blank");

        this.config = config;
        this.enabled = true;
        this.name = name;
        this.type = type.ordinal();
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilityNotificationChannel aiObservabilityNotificationChannel)) {
            return false;
        }

        return Objects.equals(id, aiObservabilityNotificationChannel.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public String getConfig() {
        return config;
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

    public AiObservabilityNotificationChannelType getType() {
        return AiObservabilityNotificationChannelType.values()[type];
    }

    public int getVersion() {
        return version;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setConfig(String config) {
        Validate.notBlank(config, "config must not be blank");

        this.config = config;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setName(String name) {
        Validate.notBlank(name, "name must not be blank");

        this.name = name;
    }

    public void setType(AiObservabilityNotificationChannelType type) {
        Validate.notNull(type, "type must not be null");

        this.type = type.ordinal();
    }

    @Override
    public String toString() {
        return "AiObservabilityNotificationChannel{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", name='" + name + '\'' +
            ", type=" + getType() +
            ", enabled=" + enabled +
            '}';
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityNotificationChannel.java
git commit -m "732 Add AiObservabilityNotificationChannel domain entity"
```

---

## Task 5: AlertEvent Domain Entity

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityAlertEvent.java`

- [ ] **Step 1: Create AiObservabilityAlertEvent**

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
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_observability_alert_event")
public class AiObservabilityAlertEvent {

    @Column("alert_rule_id")
    private Long alertRuleId;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Id
    private Long id;

    @Column
    private String message;

    @Column
    private int status;

    @Column("triggered_value")
    private BigDecimal triggeredValue;

    private AiObservabilityAlertEvent() {
    }

    public AiObservabilityAlertEvent(Long alertRuleId, BigDecimal triggeredValue, String message) {
        Validate.notNull(alertRuleId, "alertRuleId must not be null");

        this.alertRuleId = alertRuleId;
        this.message = message;
        this.status = AiObservabilityAlertEventStatus.TRIGGERED.ordinal();
        this.triggeredValue = triggeredValue;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilityAlertEvent aiObservabilityAlertEvent)) {
            return false;
        }

        return Objects.equals(id, aiObservabilityAlertEvent.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getAlertRuleId() {
        return alertRuleId;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public AiObservabilityAlertEventStatus getStatus() {
        return AiObservabilityAlertEventStatus.values()[status];
    }

    public BigDecimal getTriggeredValue() {
        return triggeredValue;
    }

    public void setStatus(AiObservabilityAlertEventStatus status) {
        Validate.notNull(status, "status must not be null");

        this.status = status.ordinal();
    }

    @Override
    public String toString() {
        return "AiObservabilityAlertEvent{" +
            "id=" + id +
            ", alertRuleId=" + alertRuleId +
            ", triggeredValue=" + triggeredValue +
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
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityAlertEvent.java
git commit -m "732 Add AiObservabilityAlertEvent domain entity"
```

---

## Task 6: Repository Interfaces

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityAlertRuleRepository.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityNotificationChannelRepository.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityAlertEventRepository.java`

- [ ] **Step 1: Create AiObservabilityAlertRuleRepository**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertRule;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiObservabilityAlertRuleRepository extends ListCrudRepository<AiObservabilityAlertRule, Long> {

    List<AiObservabilityAlertRule> findAllByWorkspaceId(Long workspaceId);

    List<AiObservabilityAlertRule> findAllByEnabled(boolean enabled);
}
```

- [ ] **Step 2: Create AiObservabilityNotificationChannelRepository**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityNotificationChannel;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiObservabilityNotificationChannelRepository
    extends ListCrudRepository<AiObservabilityNotificationChannel, Long> {

    List<AiObservabilityNotificationChannel> findAllByWorkspaceId(Long workspaceId);
}
```

- [ ] **Step 3: Create AiObservabilityAlertEventRepository**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiObservabilityAlertEventRepository extends ListCrudRepository<AiObservabilityAlertEvent, Long> {

    List<AiObservabilityAlertEvent> findAllByAlertRuleIdOrderByCreatedDateDesc(Long alertRuleId);

    Optional<AiObservabilityAlertEvent> findFirstByAlertRuleIdOrderByCreatedDateDesc(Long alertRuleId);

    List<AiObservabilityAlertEvent> findAllByAlertRuleIdAndCreatedDateAfter(Long alertRuleId, Instant after);
}
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityAlertRuleRepository.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityNotificationChannelRepository.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityAlertEventRepository.java
git commit -m "732 Add alerting repository interfaces (alert rule, notification channel, alert event)"
```

---

## Task 7: Service Interfaces

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityAlertRuleService.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityNotificationChannelService.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityAlertEventService.java`

- [ ] **Step 1: Create AiObservabilityAlertRuleService**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertRule;
import java.util.List;

/**
 * @version ee
 */
public interface AiObservabilityAlertRuleService {

    AiObservabilityAlertRule create(AiObservabilityAlertRule alertRule);

    void delete(long id);

    AiObservabilityAlertRule getAlertRule(long id);

    List<AiObservabilityAlertRule> getAlertRulesByWorkspace(Long workspaceId);

    List<AiObservabilityAlertRule> getEnabledAlertRules();

    AiObservabilityAlertRule update(AiObservabilityAlertRule alertRule);
}
```

- [ ] **Step 2: Create AiObservabilityNotificationChannelService**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityNotificationChannel;
import java.util.List;

/**
 * @version ee
 */
public interface AiObservabilityNotificationChannelService {

    AiObservabilityNotificationChannel create(AiObservabilityNotificationChannel notificationChannel);

    void delete(long id);

    AiObservabilityNotificationChannel getNotificationChannel(long id);

    List<AiObservabilityNotificationChannel> getNotificationChannelsByWorkspace(Long workspaceId);

    AiObservabilityNotificationChannel update(AiObservabilityNotificationChannel notificationChannel);
}
```

- [ ] **Step 3: Create AiObservabilityAlertEventService**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 */
public interface AiObservabilityAlertEventService {

    AiObservabilityAlertEvent create(AiObservabilityAlertEvent alertEvent);

    List<AiObservabilityAlertEvent> getAlertEventsByRule(Long alertRuleId);

    Optional<AiObservabilityAlertEvent> getLatestEventByRule(Long alertRuleId);

    List<AiObservabilityAlertEvent> getAlertEventsByRuleAfter(Long alertRuleId, Instant after);

    AiObservabilityAlertEvent update(AiObservabilityAlertEvent alertEvent);
}
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityAlertRuleService.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityNotificationChannelService.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityAlertEventService.java
git commit -m "732 Add alerting service interfaces (alert rule, notification channel, alert event)"
```

---

## Task 8: Service Implementations

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityAlertRuleServiceImpl.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityNotificationChannelServiceImpl.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityAlertEventServiceImpl.java`

- [ ] **Step 1: Create AiObservabilityAlertRuleServiceImpl**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertRule;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilityAlertRuleRepository;
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
class AiObservabilityAlertRuleServiceImpl implements AiObservabilityAlertRuleService {

    private final AiObservabilityAlertRuleRepository aiObservabilityAlertRuleRepository;

    public AiObservabilityAlertRuleServiceImpl(
        AiObservabilityAlertRuleRepository aiObservabilityAlertRuleRepository) {

        this.aiObservabilityAlertRuleRepository = aiObservabilityAlertRuleRepository;
    }

    @Override
    public AiObservabilityAlertRule create(AiObservabilityAlertRule alertRule) {
        Validate.notNull(alertRule, "alertRule must not be null");
        Validate.isTrue(alertRule.getId() == null, "alertRule id must be null for creation");

        return aiObservabilityAlertRuleRepository.save(alertRule);
    }

    @Override
    public void delete(long id) {
        aiObservabilityAlertRuleRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiObservabilityAlertRule getAlertRule(long id) {
        return aiObservabilityAlertRuleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiObservabilityAlertRule not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityAlertRule> getAlertRulesByWorkspace(Long workspaceId) {
        return aiObservabilityAlertRuleRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityAlertRule> getEnabledAlertRules() {
        return aiObservabilityAlertRuleRepository.findAllByEnabled(true);
    }

    @Override
    public AiObservabilityAlertRule update(AiObservabilityAlertRule alertRule) {
        Validate.notNull(alertRule, "alertRule must not be null");
        Validate.notNull(alertRule.getId(), "alertRule id must not be null for update");

        return aiObservabilityAlertRuleRepository.save(alertRule);
    }
}
```

- [ ] **Step 2: Create AiObservabilityNotificationChannelServiceImpl**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityNotificationChannel;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilityNotificationChannelRepository;
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
class AiObservabilityNotificationChannelServiceImpl implements AiObservabilityNotificationChannelService {

    private final AiObservabilityNotificationChannelRepository aiObservabilityNotificationChannelRepository;

    public AiObservabilityNotificationChannelServiceImpl(
        AiObservabilityNotificationChannelRepository aiObservabilityNotificationChannelRepository) {

        this.aiObservabilityNotificationChannelRepository = aiObservabilityNotificationChannelRepository;
    }

    @Override
    public AiObservabilityNotificationChannel create(AiObservabilityNotificationChannel notificationChannel) {
        Validate.notNull(notificationChannel, "notificationChannel must not be null");
        Validate.isTrue(notificationChannel.getId() == null, "notificationChannel id must be null for creation");

        return aiObservabilityNotificationChannelRepository.save(notificationChannel);
    }

    @Override
    public void delete(long id) {
        aiObservabilityNotificationChannelRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiObservabilityNotificationChannel getNotificationChannel(long id) {
        return aiObservabilityNotificationChannelRepository.findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException("AiObservabilityNotificationChannel not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityNotificationChannel> getNotificationChannelsByWorkspace(Long workspaceId) {
        return aiObservabilityNotificationChannelRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    public AiObservabilityNotificationChannel update(AiObservabilityNotificationChannel notificationChannel) {
        Validate.notNull(notificationChannel, "notificationChannel must not be null");
        Validate.notNull(notificationChannel.getId(), "notificationChannel id must not be null for update");

        return aiObservabilityNotificationChannelRepository.save(notificationChannel);
    }
}
```

- [ ] **Step 3: Create AiObservabilityAlertEventServiceImpl**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertEvent;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilityAlertEventRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
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
class AiObservabilityAlertEventServiceImpl implements AiObservabilityAlertEventService {

    private final AiObservabilityAlertEventRepository aiObservabilityAlertEventRepository;

    public AiObservabilityAlertEventServiceImpl(
        AiObservabilityAlertEventRepository aiObservabilityAlertEventRepository) {

        this.aiObservabilityAlertEventRepository = aiObservabilityAlertEventRepository;
    }

    @Override
    public AiObservabilityAlertEvent create(AiObservabilityAlertEvent alertEvent) {
        Validate.notNull(alertEvent, "alertEvent must not be null");
        Validate.isTrue(alertEvent.getId() == null, "alertEvent id must be null for creation");

        return aiObservabilityAlertEventRepository.save(alertEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityAlertEvent> getAlertEventsByRule(Long alertRuleId) {
        return aiObservabilityAlertEventRepository.findAllByAlertRuleIdOrderByCreatedDateDesc(alertRuleId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiObservabilityAlertEvent> getLatestEventByRule(Long alertRuleId) {
        return aiObservabilityAlertEventRepository.findFirstByAlertRuleIdOrderByCreatedDateDesc(alertRuleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityAlertEvent> getAlertEventsByRuleAfter(Long alertRuleId, Instant after) {
        return aiObservabilityAlertEventRepository.findAllByAlertRuleIdAndCreatedDateAfter(alertRuleId, after);
    }

    @Override
    public AiObservabilityAlertEvent update(AiObservabilityAlertEvent alertEvent) {
        Validate.notNull(alertEvent, "alertEvent must not be null");
        Validate.notNull(alertEvent.getId(), "alertEvent id must not be null for update");

        return aiObservabilityAlertEventRepository.save(alertEvent);
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityAlertRuleServiceImpl.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityNotificationChannelServiceImpl.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityAlertEventServiceImpl.java
git commit -m "732 Add alerting service implementations (alert rule, notification channel, alert event)"
```

---

## Task 9: AlertScheduler Interface (platform-scheduler-api)

**Files:**
- Create: `server/libs/platform/platform-scheduler/platform-scheduler-api/src/main/java/com/bytechef/platform/scheduler/AlertScheduler.java`

Note: This file uses the **Apache 2.0 license** (not EE), matching `TriggerScheduler.java` in the same package.

- [ ] **Step 1: Create AlertScheduler**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.scheduler;

/**
 * @author Ivica Cardic
 */
public interface AlertScheduler {

    void scheduleAlertEvaluation(long alertRuleId, int windowMinutes);

    void cancelAlertEvaluation(long alertRuleId);
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:libs:platform:platform-scheduler:platform-scheduler-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/libs/platform/platform-scheduler/platform-scheduler-api/src/main/java/com/bytechef/platform/scheduler/AlertScheduler.java
git commit -m "732 Add AlertScheduler interface to platform-scheduler-api"
```

---

## Task 10: AlertEvaluationJob and QuartzAlertScheduler (platform-scheduler-impl)

**Files:**
- Create: `server/libs/platform/platform-scheduler/platform-scheduler-impl/src/main/java/com/bytechef/platform/scheduler/job/AlertEvaluationJob.java`
- Create: `server/libs/platform/platform-scheduler/platform-scheduler-impl/src/main/java/com/bytechef/platform/scheduler/QuartzAlertScheduler.java`
- Modify: `server/libs/platform/platform-scheduler/platform-scheduler-impl/src/main/java/com/bytechef/platform/scheduler/config/QuartzTriggerSchedulerConfiguration.java`

Note: All files in platform-scheduler-impl use the **Apache 2.0 license** (not EE).

- [ ] **Step 1: Create AlertEvaluationJob**

This Quartz job publishes an `AlertEvaluationEvent` Spring event when its timer fires. The gateway service module listens for this event and evaluates the alert rule.

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.scheduler.job;

import com.bytechef.platform.scheduler.event.AlertEvaluationEvent;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
public class AlertEvaluationJob implements Job {

    private ApplicationEventPublisher eventPublisher;

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap jobDataMap = context.getMergedJobDataMap();

        long alertRuleId = jobDataMap.getLong("alertRuleId");

        eventPublisher.publishEvent(new AlertEvaluationEvent(alertRuleId));
    }

    @Autowired
    public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
}
```

- [ ] **Step 2: Create AlertEvaluationEvent**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.scheduler.event;

/**
 * @author Ivica Cardic
 */
public record AlertEvaluationEvent(long alertRuleId) {
}
```

File location: `server/libs/platform/platform-scheduler/platform-scheduler-api/src/main/java/com/bytechef/platform/scheduler/event/AlertEvaluationEvent.java`

Note: Place the event record in `platform-scheduler-api` so that both the impl module (which publishes it) and the gateway service module (which listens for it) can depend on the API module.

- [ ] **Step 3: Create QuartzAlertScheduler**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.scheduler;

import com.bytechef.platform.scheduler.job.AlertEvaluationJob;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

/**
 * @author Ivica Cardic
 */
public class QuartzAlertScheduler implements AlertScheduler {

    private final Scheduler scheduler;

    @SuppressFBWarnings("EI")
    public QuartzAlertScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void scheduleAlertEvaluation(long alertRuleId, int windowMinutes) {
        String alertRuleIdStr = String.valueOf(alertRuleId);

        JobDetail jobDetail = JobBuilder.newJob(AlertEvaluationJob.class)
            .withIdentity(JobKey.jobKey(alertRuleIdStr, "AlertEvaluation"))
            .usingJobData("alertRuleId", alertRuleId)
            .build();

        Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(TriggerKey.triggerKey(alertRuleIdStr, "AlertEvaluation"))
            .withSchedule(
                SimpleScheduleBuilder.repeatMinutelyForever(windowMinutes))
            .startNow()
            .build();

        try {
            scheduler.deleteJob(jobDetail.getKey());

            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException schedulerException) {
            throw new RuntimeException(schedulerException);
        }
    }

    @Override
    public void cancelAlertEvaluation(long alertRuleId) {
        try {
            scheduler.deleteJob(JobKey.jobKey(String.valueOf(alertRuleId), "AlertEvaluation"));
        } catch (SchedulerException schedulerException) {
            throw new RuntimeException(schedulerException);
        }
    }
}
```

- [ ] **Step 4: Add AlertScheduler bean to QuartzTriggerSchedulerConfiguration**

In `server/libs/platform/platform-scheduler/platform-scheduler-impl/src/main/java/com/bytechef/platform/scheduler/config/QuartzTriggerSchedulerConfiguration.java`, add the following bean method:

```java
    @Bean
    AlertScheduler quartzAlertScheduler(@Lazy Scheduler scheduler) {
        return new QuartzAlertScheduler(scheduler);
    }
```

Add these imports:
```java
import com.bytechef.platform.scheduler.AlertScheduler;
import com.bytechef.platform.scheduler.QuartzAlertScheduler;
```

- [ ] **Step 5: Verify compilation**

Run: `./gradlew :server:libs:platform:platform-scheduler:platform-scheduler-impl:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add server/libs/platform/platform-scheduler/platform-scheduler-api/src/main/java/com/bytechef/platform/scheduler/AlertScheduler.java \
  server/libs/platform/platform-scheduler/platform-scheduler-api/src/main/java/com/bytechef/platform/scheduler/event/AlertEvaluationEvent.java \
  server/libs/platform/platform-scheduler/platform-scheduler-impl/src/main/java/com/bytechef/platform/scheduler/job/AlertEvaluationJob.java \
  server/libs/platform/platform-scheduler/platform-scheduler-impl/src/main/java/com/bytechef/platform/scheduler/QuartzAlertScheduler.java \
  server/libs/platform/platform-scheduler/platform-scheduler-impl/src/main/java/com/bytechef/platform/scheduler/config/QuartzTriggerSchedulerConfiguration.java
git commit -m "732 Add QuartzAlertScheduler, AlertEvaluationJob, and AlertEvaluationEvent"
```

---

## Task 11: Notification Dispatcher

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityNotificationDispatcher.java`

- [ ] **Step 1: Create AiObservabilityNotificationDispatcher**

This service sends notifications to channels linked to an alert rule. It supports WEBHOOK (HTTP POST), EMAIL (via Spring `JavaMailSender`), and SLACK (webhook URL POST).

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertEvent;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertRule;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertRuleChannel;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityNotificationChannel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @version ee
 */
@Service
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class AiObservabilityNotificationDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(AiObservabilityNotificationDispatcher.class);

    private final AiObservabilityNotificationChannelService aiObservabilityNotificationChannelService;
    private final HttpClient httpClient;

    AiObservabilityNotificationDispatcher(
        AiObservabilityNotificationChannelService aiObservabilityNotificationChannelService) {

        this.aiObservabilityNotificationChannelService = aiObservabilityNotificationChannelService;
        this.httpClient = HttpClient.newHttpClient();
    }

    void dispatch(AiObservabilityAlertRule alertRule, AiObservabilityAlertEvent alertEvent) {
        Set<AiObservabilityAlertRuleChannel> channels = alertRule.getChannels();

        for (AiObservabilityAlertRuleChannel ruleChannel : channels) {
            try {
                AiObservabilityNotificationChannel notificationChannel =
                    aiObservabilityNotificationChannelService.getNotificationChannel(
                        ruleChannel.notificationChannelId());

                if (!notificationChannel.isEnabled()) {
                    continue;
                }

                switch (notificationChannel.getType()) {
                    case WEBHOOK -> sendWebhookNotification(notificationChannel, alertRule, alertEvent);
                    case EMAIL -> sendEmailNotification(notificationChannel, alertRule, alertEvent);
                    case SLACK -> sendSlackNotification(notificationChannel, alertRule, alertEvent);
                }
            } catch (Exception exception) {
                logger.error(
                    "Failed to dispatch notification to channel {} for alert rule {}",
                    ruleChannel.notificationChannelId(), alertRule.getId(), exception);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void sendWebhookNotification(
        AiObservabilityNotificationChannel notificationChannel,
        AiObservabilityAlertRule alertRule, AiObservabilityAlertEvent alertEvent) {

        Map<String, Object> config = JsonUtils.read(notificationChannel.getConfig(), Map.class);

        String url = (String) config.get("url");

        String payload = JsonUtils.write(Map.of(
            "alertRuleId", alertRule.getId(),
            "alertRuleName", alertRule.getName(),
            "metric", alertRule.getMetric().name(),
            "threshold", alertRule.getThreshold(),
            "triggeredValue", alertEvent.getTriggeredValue(),
            "message", alertEvent.getMessage(),
            "status", alertEvent.getStatus().name()));

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload));

        Map<String, String> headers = (Map<String, String>) config.get("headers");

        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                requestBuilder.header(header.getKey(), header.getValue());
            }
        }

        try {
            httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception exception) {
            logger.error("Failed to send webhook notification to {}", url, exception);
        }
    }

    @SuppressWarnings("unchecked")
    private void sendEmailNotification(
        AiObservabilityNotificationChannel notificationChannel,
        AiObservabilityAlertRule alertRule, AiObservabilityAlertEvent alertEvent) {

        // Email sending requires JavaMailSender integration.
        // Log the notification for now; full email implementation
        // depends on the mail configuration available in the deployment.

        logger.info(
            "Email notification for alert rule '{}': {} (value: {})",
            alertRule.getName(), alertEvent.getMessage(), alertEvent.getTriggeredValue());
    }

    @SuppressWarnings("unchecked")
    private void sendSlackNotification(
        AiObservabilityNotificationChannel notificationChannel,
        AiObservabilityAlertRule alertRule, AiObservabilityAlertEvent alertEvent) {

        Map<String, Object> config = JsonUtils.read(notificationChannel.getConfig(), Map.class);

        String webhookUrl = (String) config.get("webhookUrl");

        String slackPayload = JsonUtils.write(Map.of(
            "text", String.format(
                ":rotating_light: *Alert: %s*\n%s\nTriggered value: %s",
                alertRule.getName(), alertEvent.getMessage(), alertEvent.getTriggeredValue())));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(webhookUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(slackPayload))
            .build();

        try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception exception) {
            logger.error("Failed to send Slack notification to {}", webhookUrl, exception);
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityNotificationDispatcher.java
git commit -m "732 Add AiObservabilityNotificationDispatcher for multi-channel alert notifications"
```

---

## Task 12: Alert Evaluator

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityAlertEvaluator.java`

- [ ] **Step 1: Create AiObservabilityAlertEvaluator**

This service listens for `AlertEvaluationEvent` from the scheduler. On each tick it:
1. Loads the alert rule
2. Queries `AiGatewayRequestLog` within `window_minutes`
3. Computes the metric value
4. Checks the condition against the threshold
5. Enforces cooldown by checking the latest `AiObservabilityAlertEvent`
6. Creates an alert event and dispatches notifications if threshold is breached

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRequestLog;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertCondition;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertEvent;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertRule;
import com.bytechef.ee.automation.ai.gateway.repository.AiGatewayRequestLogRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.scheduler.event.AlertEvaluationEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
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
class AiObservabilityAlertEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(AiObservabilityAlertEvaluator.class);

    private final AiGatewayRequestLogRepository aiGatewayRequestLogRepository;
    private final AiObservabilityAlertEventService aiObservabilityAlertEventService;
    private final AiObservabilityAlertRuleService aiObservabilityAlertRuleService;
    private final AiObservabilityNotificationDispatcher aiObservabilityNotificationDispatcher;

    AiObservabilityAlertEvaluator(
        AiGatewayRequestLogRepository aiGatewayRequestLogRepository,
        AiObservabilityAlertEventService aiObservabilityAlertEventService,
        AiObservabilityAlertRuleService aiObservabilityAlertRuleService,
        AiObservabilityNotificationDispatcher aiObservabilityNotificationDispatcher) {

        this.aiGatewayRequestLogRepository = aiGatewayRequestLogRepository;
        this.aiObservabilityAlertEventService = aiObservabilityAlertEventService;
        this.aiObservabilityAlertRuleService = aiObservabilityAlertRuleService;
        this.aiObservabilityNotificationDispatcher = aiObservabilityNotificationDispatcher;
    }

    @EventListener
    public void onAlertEvaluation(AlertEvaluationEvent alertEvaluationEvent) {
        long alertRuleId = alertEvaluationEvent.alertRuleId();

        try {
            AiObservabilityAlertRule alertRule = aiObservabilityAlertRuleService.getAlertRule(alertRuleId);

            if (!alertRule.isEnabled()) {
                return;
            }

            evaluate(alertRule);
        } catch (Exception exception) {
            logger.error("Failed to evaluate alert rule {}", alertRuleId, exception);
        }
    }

    private void evaluate(AiObservabilityAlertRule alertRule) {
        Instant windowEnd = Instant.now();
        Instant windowStart = windowEnd.minus(alertRule.getWindowMinutes(), ChronoUnit.MINUTES);

        List<AiGatewayRequestLog> requestLogs =
            aiGatewayRequestLogRepository.findAllByWorkspaceIdAndCreatedDateBetween(
                alertRule.getWorkspaceId(), windowStart, windowEnd);

        if (requestLogs.isEmpty()) {
            return;
        }

        BigDecimal metricValue = computeMetric(alertRule, requestLogs);

        if (metricValue == null) {
            return;
        }

        boolean breached = evaluateCondition(alertRule.getCondition(), metricValue, alertRule.getThreshold());

        if (!breached) {
            return;
        }

        if (isCooldownActive(alertRule)) {
            return;
        }

        String message = String.format(
            "Alert '%s': %s %s threshold %s (actual: %s) over %d minute window",
            alertRule.getName(),
            alertRule.getMetric().name(),
            alertRule.getCondition().name().toLowerCase().replace('_', ' '),
            alertRule.getThreshold().toPlainString(),
            metricValue.toPlainString(),
            alertRule.getWindowMinutes());

        AiObservabilityAlertEvent alertEvent = new AiObservabilityAlertEvent(
            alertRule.getId(), metricValue, message);

        AiObservabilityAlertEvent savedAlertEvent = aiObservabilityAlertEventService.create(alertEvent);

        aiObservabilityNotificationDispatcher.dispatch(alertRule, savedAlertEvent);
    }

    private BigDecimal computeMetric(
        AiObservabilityAlertRule alertRule, List<AiGatewayRequestLog> requestLogs) {

        return switch (alertRule.getMetric()) {
            case ERROR_RATE -> computeErrorRate(requestLogs);
            case LATENCY_P95 -> computeLatencyP95(requestLogs);
            case COST -> computeTotalCost(requestLogs);
            case TOKEN_USAGE -> computeTotalTokens(requestLogs);
            case REQUEST_VOLUME -> BigDecimal.valueOf(requestLogs.size());
        };
    }

    private BigDecimal computeErrorRate(List<AiGatewayRequestLog> requestLogs) {
        long totalCount = requestLogs.size();
        long errorCount = requestLogs.stream()
            .filter(requestLog -> requestLog.getStatus() != null && requestLog.getStatus() >= 400)
            .count();

        if (totalCount == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(errorCount)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal computeLatencyP95(List<AiGatewayRequestLog> requestLogs) {
        List<Integer> latencies = requestLogs.stream()
            .map(AiGatewayRequestLog::getLatencyMs)
            .filter(latencyMs -> latencyMs != null)
            .sorted()
            .toList();

        if (latencies.isEmpty()) {
            return null;
        }

        int p95Index = (int) Math.ceil(latencies.size() * 0.95) - 1;

        p95Index = Math.max(0, Math.min(p95Index, latencies.size() - 1));

        return BigDecimal.valueOf(latencies.get(p95Index));
    }

    private BigDecimal computeTotalCost(List<AiGatewayRequestLog> requestLogs) {
        return requestLogs.stream()
            .map(AiGatewayRequestLog::getCost)
            .filter(cost -> cost != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal computeTotalTokens(List<AiGatewayRequestLog> requestLogs) {
        int totalTokens = requestLogs.stream()
            .mapToInt(requestLog -> {
                int inputTokens = requestLog.getInputTokens() != null ? requestLog.getInputTokens() : 0;
                int outputTokens = requestLog.getOutputTokens() != null ? requestLog.getOutputTokens() : 0;

                return inputTokens + outputTokens;
            })
            .sum();

        return BigDecimal.valueOf(totalTokens);
    }

    private boolean evaluateCondition(
        AiObservabilityAlertCondition condition, BigDecimal metricValue, BigDecimal threshold) {

        int comparison = metricValue.compareTo(threshold);

        return switch (condition) {
            case GREATER_THAN -> comparison > 0;
            case LESS_THAN -> comparison < 0;
            case EQUALS -> comparison == 0;
        };
    }

    private boolean isCooldownActive(AiObservabilityAlertRule alertRule) {
        Optional<AiObservabilityAlertEvent> latestEvent =
            aiObservabilityAlertEventService.getLatestEventByRule(alertRule.getId());

        if (latestEvent.isEmpty()) {
            return false;
        }

        Instant cooldownEnd = latestEvent.get()
            .getCreatedDate()
            .plus(alertRule.getCooldownMinutes(), ChronoUnit.MINUTES);

        return Instant.now().isBefore(cooldownEnd);
    }
}
```

- [ ] **Step 2: Add `platform-scheduler-api` dependency to gateway service build.gradle.kts**

Verify the `automation-ai-gateway-service` module's `build.gradle.kts` includes a dependency on `platform-scheduler-api`. If not already present, add:

```kotlin
implementation(project(":server:libs:platform:platform-scheduler:platform-scheduler-api"))
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityAlertEvaluator.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/build.gradle.kts
git commit -m "732 Add AiObservabilityAlertEvaluator with metric computation and cooldown enforcement"
```

---

## Task 13: Scheduler Integration in Alert Rule Lifecycle

**Files:**
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityAlertRuleServiceImpl.java`

- [ ] **Step 1: Inject AlertScheduler and manage schedule lifecycle**

Update `AiObservabilityAlertRuleServiceImpl` to inject `AlertScheduler` and call `scheduleAlertEvaluation`/`cancelAlertEvaluation` during create/update/delete:

```java
// Add field
private final AlertScheduler alertScheduler;

// Update constructor
public AiObservabilityAlertRuleServiceImpl(
    AiObservabilityAlertRuleRepository aiObservabilityAlertRuleRepository,
    AlertScheduler alertScheduler) {

    this.aiObservabilityAlertRuleRepository = aiObservabilityAlertRuleRepository;
    this.alertScheduler = alertScheduler;
}

// Update create method — after save, schedule if enabled
@Override
public AiObservabilityAlertRule create(AiObservabilityAlertRule alertRule) {
    Validate.notNull(alertRule, "alertRule must not be null");
    Validate.isTrue(alertRule.getId() == null, "alertRule id must be null for creation");

    AiObservabilityAlertRule savedAlertRule = aiObservabilityAlertRuleRepository.save(alertRule);

    if (savedAlertRule.isEnabled()) {
        alertScheduler.scheduleAlertEvaluation(savedAlertRule.getId(), savedAlertRule.getWindowMinutes());
    }

    return savedAlertRule;
}

// Update delete method — cancel before deleting
@Override
public void delete(long id) {
    alertScheduler.cancelAlertEvaluation(id);

    aiObservabilityAlertRuleRepository.deleteById(id);
}

// Update update method — reschedule or cancel based on enabled flag
@Override
public AiObservabilityAlertRule update(AiObservabilityAlertRule alertRule) {
    Validate.notNull(alertRule, "alertRule must not be null");
    Validate.notNull(alertRule.getId(), "alertRule id must not be null for update");

    AiObservabilityAlertRule savedAlertRule = aiObservabilityAlertRuleRepository.save(alertRule);

    if (savedAlertRule.isEnabled()) {
        alertScheduler.scheduleAlertEvaluation(savedAlertRule.getId(), savedAlertRule.getWindowMinutes());
    } else {
        alertScheduler.cancelAlertEvaluation(savedAlertRule.getId());
    }

    return savedAlertRule;
}
```

Add import:
```java
import com.bytechef.platform.scheduler.AlertScheduler;
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityAlertRuleServiceImpl.java
git commit -m "732 Integrate AlertScheduler into alert rule create/update/delete lifecycle"
```

---

## Task 14: GraphQL Schema for Alerts, Notification Channels, Alert Events

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-alert-rule.graphqls`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-notification-channel.graphqls`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-alert-event.graphqls`

- [ ] **Step 1: Create ai-observability-alert-rule.graphqls**

```graphql
extend type Query {
    aiObservabilityAlertRule(id: ID!): AiObservabilityAlertRule
    aiObservabilityAlertRules(workspaceId: ID!): [AiObservabilityAlertRule]
}

extend type Mutation {
    createAiObservabilityAlertRule(input: AiObservabilityAlertRuleInput!): AiObservabilityAlertRule
    deleteAiObservabilityAlertRule(id: ID!): Boolean
    updateAiObservabilityAlertRule(id: ID!, input: AiObservabilityAlertRuleInput!): AiObservabilityAlertRule
}

type AiObservabilityAlertRule {
    channelIds: [ID]
    condition: AiObservabilityAlertCondition!
    cooldownMinutes: Int!
    createdDate: Long
    enabled: Boolean!
    filters: String
    id: ID!
    lastModifiedDate: Long
    metric: AiObservabilityAlertMetric!
    name: String!
    projectId: ID
    threshold: Float!
    version: Int
    windowMinutes: Int!
    workspaceId: ID!
}

input AiObservabilityAlertRuleInput {
    channelIds: [ID]
    condition: AiObservabilityAlertCondition!
    cooldownMinutes: Int!
    enabled: Boolean!
    filters: String
    metric: AiObservabilityAlertMetric!
    name: String!
    projectId: ID
    threshold: Float!
    windowMinutes: Int!
    workspaceId: ID!
}

enum AiObservabilityAlertMetric {
    COST
    ERROR_RATE
    LATENCY_P95
    REQUEST_VOLUME
    TOKEN_USAGE
}

enum AiObservabilityAlertCondition {
    EQUALS
    GREATER_THAN
    LESS_THAN
}
```

- [ ] **Step 2: Create ai-observability-notification-channel.graphqls**

```graphql
extend type Query {
    aiObservabilityNotificationChannel(id: ID!): AiObservabilityNotificationChannel
    aiObservabilityNotificationChannels(workspaceId: ID!): [AiObservabilityNotificationChannel]
}

extend type Mutation {
    createAiObservabilityNotificationChannel(input: AiObservabilityNotificationChannelInput!): AiObservabilityNotificationChannel
    deleteAiObservabilityNotificationChannel(id: ID!): Boolean
    updateAiObservabilityNotificationChannel(id: ID!, input: AiObservabilityNotificationChannelInput!): AiObservabilityNotificationChannel
}

type AiObservabilityNotificationChannel {
    config: String!
    createdDate: Long
    enabled: Boolean!
    id: ID!
    lastModifiedDate: Long
    name: String!
    type: AiObservabilityNotificationChannelType!
    version: Int
    workspaceId: ID!
}

input AiObservabilityNotificationChannelInput {
    config: String!
    enabled: Boolean!
    name: String!
    type: AiObservabilityNotificationChannelType!
    workspaceId: ID!
}

enum AiObservabilityNotificationChannelType {
    EMAIL
    SLACK
    WEBHOOK
}
```

- [ ] **Step 3: Create ai-observability-alert-event.graphqls**

```graphql
extend type Query {
    aiObservabilityAlertEvents(alertRuleId: ID!): [AiObservabilityAlertEvent]
}

extend type Mutation {
    acknowledgeAiObservabilityAlertEvent(id: ID!): AiObservabilityAlertEvent
}

type AiObservabilityAlertEvent {
    alertRuleId: ID!
    createdDate: Long
    id: ID!
    message: String
    status: AiObservabilityAlertEventStatus!
    triggeredValue: Float
}

enum AiObservabilityAlertEventStatus {
    ACKNOWLEDGED
    RESOLVED
    TRIGGERED
}
```

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-alert-rule.graphqls \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-notification-channel.graphqls \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-alert-event.graphqls
git commit -m "732 Add GraphQL schema for alert rules, notification channels, and alert events"
```

---

## Task 15: GraphQL Controllers

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiObservabilityAlertRuleGraphQlController.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiObservabilityNotificationChannelGraphQlController.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiObservabilityAlertEventGraphQlController.java`

- [ ] **Step 1: Create AiObservabilityAlertRuleGraphQlController**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertCondition;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertMetric;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertRule;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertRuleChannel;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityAlertRuleService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
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
class AiObservabilityAlertRuleGraphQlController {

    private final AiObservabilityAlertRuleService aiObservabilityAlertRuleService;

    @SuppressFBWarnings("EI")
    AiObservabilityAlertRuleGraphQlController(
        AiObservabilityAlertRuleService aiObservabilityAlertRuleService) {

        this.aiObservabilityAlertRuleService = aiObservabilityAlertRuleService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityAlertRule aiObservabilityAlertRule(@Argument long id) {
        return aiObservabilityAlertRuleService.getAlertRule(id);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiObservabilityAlertRule> aiObservabilityAlertRules(@Argument Long workspaceId) {
        return aiObservabilityAlertRuleService.getAlertRulesByWorkspace(workspaceId);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityAlertRule createAiObservabilityAlertRule(@Argument Map<String, Object> input) {
        AiObservabilityAlertRule alertRule = new AiObservabilityAlertRule(
            Long.valueOf((String) input.get("workspaceId")),
            (String) input.get("name"),
            AiObservabilityAlertMetric.valueOf((String) input.get("metric")),
            AiObservabilityAlertCondition.valueOf((String) input.get("condition")),
            BigDecimal.valueOf(((Number) input.get("threshold")).doubleValue()),
            (int) input.get("windowMinutes"),
            (int) input.get("cooldownMinutes"));

        alertRule.setEnabled((boolean) input.get("enabled"));

        if (input.get("projectId") != null) {
            alertRule.setProjectId(Long.valueOf((String) input.get("projectId")));
        }

        if (input.get("filters") != null) {
            alertRule.setFilters((String) input.get("filters"));
        }

        if (input.get("channelIds") != null) {
            @SuppressWarnings("unchecked")
            List<String> channelIds = (List<String>) input.get("channelIds");

            Set<AiObservabilityAlertRuleChannel> channels = channelIds.stream()
                .map(channelId -> new AiObservabilityAlertRuleChannel(Long.parseLong(channelId)))
                .collect(Collectors.toSet());

            alertRule.setChannels(channels);
        }

        return aiObservabilityAlertRuleService.create(alertRule);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean deleteAiObservabilityAlertRule(@Argument long id) {
        aiObservabilityAlertRuleService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityAlertRule updateAiObservabilityAlertRule(
        @Argument long id, @Argument Map<String, Object> input) {

        AiObservabilityAlertRule alertRule = aiObservabilityAlertRuleService.getAlertRule(id);

        alertRule.setName((String) input.get("name"));
        alertRule.setMetric(AiObservabilityAlertMetric.valueOf((String) input.get("metric")));
        alertRule.setCondition(AiObservabilityAlertCondition.valueOf((String) input.get("condition")));
        alertRule.setThreshold(BigDecimal.valueOf(((Number) input.get("threshold")).doubleValue()));
        alertRule.setWindowMinutes((int) input.get("windowMinutes"));
        alertRule.setCooldownMinutes((int) input.get("cooldownMinutes"));
        alertRule.setEnabled((boolean) input.get("enabled"));

        if (input.get("filters") != null) {
            alertRule.setFilters((String) input.get("filters"));
        }

        if (input.get("channelIds") != null) {
            @SuppressWarnings("unchecked")
            List<String> channelIds = (List<String>) input.get("channelIds");

            Set<AiObservabilityAlertRuleChannel> channels = channelIds.stream()
                .map(channelId -> new AiObservabilityAlertRuleChannel(Long.parseLong(channelId)))
                .collect(Collectors.toSet());

            alertRule.setChannels(channels);
        }

        return aiObservabilityAlertRuleService.update(alertRule);
    }

    @SchemaMapping(typeName = "AiObservabilityAlertRule", field = "channelIds")
    public List<String> channelIds(AiObservabilityAlertRule alertRule) {
        return alertRule.getChannels()
            .stream()
            .map(channel -> String.valueOf(channel.notificationChannelId()))
            .toList();
    }
}
```

- [ ] **Step 2: Create AiObservabilityNotificationChannelGraphQlController**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityNotificationChannel;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityNotificationChannelType;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityNotificationChannelService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
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
class AiObservabilityNotificationChannelGraphQlController {

    private final AiObservabilityNotificationChannelService aiObservabilityNotificationChannelService;

    @SuppressFBWarnings("EI")
    AiObservabilityNotificationChannelGraphQlController(
        AiObservabilityNotificationChannelService aiObservabilityNotificationChannelService) {

        this.aiObservabilityNotificationChannelService = aiObservabilityNotificationChannelService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityNotificationChannel aiObservabilityNotificationChannel(@Argument long id) {
        return aiObservabilityNotificationChannelService.getNotificationChannel(id);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiObservabilityNotificationChannel> aiObservabilityNotificationChannels(
        @Argument Long workspaceId) {

        return aiObservabilityNotificationChannelService.getNotificationChannelsByWorkspace(workspaceId);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityNotificationChannel createAiObservabilityNotificationChannel(
        @Argument Map<String, Object> input) {

        AiObservabilityNotificationChannel notificationChannel = new AiObservabilityNotificationChannel(
            Long.valueOf((String) input.get("workspaceId")),
            (String) input.get("name"),
            AiObservabilityNotificationChannelType.valueOf((String) input.get("type")),
            (String) input.get("config"));

        notificationChannel.setEnabled((boolean) input.get("enabled"));

        return aiObservabilityNotificationChannelService.create(notificationChannel);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean deleteAiObservabilityNotificationChannel(@Argument long id) {
        aiObservabilityNotificationChannelService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityNotificationChannel updateAiObservabilityNotificationChannel(
        @Argument long id, @Argument Map<String, Object> input) {

        AiObservabilityNotificationChannel notificationChannel =
            aiObservabilityNotificationChannelService.getNotificationChannel(id);

        notificationChannel.setName((String) input.get("name"));
        notificationChannel.setType(AiObservabilityNotificationChannelType.valueOf((String) input.get("type")));
        notificationChannel.setConfig((String) input.get("config"));
        notificationChannel.setEnabled((boolean) input.get("enabled"));

        return aiObservabilityNotificationChannelService.update(notificationChannel);
    }
}
```

- [ ] **Step 3: Create AiObservabilityAlertEventGraphQlController**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertEvent;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertEventStatus;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityAlertEventService;
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
class AiObservabilityAlertEventGraphQlController {

    private final AiObservabilityAlertEventService aiObservabilityAlertEventService;

    @SuppressFBWarnings("EI")
    AiObservabilityAlertEventGraphQlController(
        AiObservabilityAlertEventService aiObservabilityAlertEventService) {

        this.aiObservabilityAlertEventService = aiObservabilityAlertEventService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiObservabilityAlertEvent> aiObservabilityAlertEvents(@Argument Long alertRuleId) {
        return aiObservabilityAlertEventService.getAlertEventsByRule(alertRuleId);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityAlertEvent acknowledgeAiObservabilityAlertEvent(@Argument long id) {
        AiObservabilityAlertEvent alertEvent = aiObservabilityAlertEventService.getAlertEventsByRule(id)
            .stream()
            .filter(event -> event.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("AiObservabilityAlertEvent not found with id: " + id));

        alertEvent.setStatus(AiObservabilityAlertEventStatus.ACKNOWLEDGED);

        return aiObservabilityAlertEventService.update(alertEvent);
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-graphql:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiObservabilityAlertRuleGraphQlController.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiObservabilityNotificationChannelGraphQlController.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiObservabilityAlertEventGraphQlController.java
git commit -m "732 Add GraphQL controllers for alert rules, notification channels, and alert events"
```

---

## Task 16: Client — GraphQL Operations and Codegen

**Files:**
- Create: `client/src/graphql/automation/ai-gateway/aiObservabilityAlertRules.graphql`
- Create: `client/src/graphql/automation/ai-gateway/aiObservabilityNotificationChannels.graphql`
- Create: `client/src/graphql/automation/ai-gateway/aiObservabilityAlertEvents.graphql`

- [ ] **Step 1: Create aiObservabilityAlertRules.graphql**

```graphql
query aiObservabilityAlertRules($workspaceId: ID!) {
    aiObservabilityAlertRules(workspaceId: $workspaceId) {
        channelIds
        condition
        cooldownMinutes
        createdDate
        enabled
        filters
        id
        lastModifiedDate
        metric
        name
        projectId
        threshold
        version
        windowMinutes
        workspaceId
    }
}

query aiObservabilityAlertRule($id: ID!) {
    aiObservabilityAlertRule(id: $id) {
        channelIds
        condition
        cooldownMinutes
        createdDate
        enabled
        filters
        id
        lastModifiedDate
        metric
        name
        projectId
        threshold
        version
        windowMinutes
        workspaceId
    }
}

mutation createAiObservabilityAlertRule($input: AiObservabilityAlertRuleInput!) {
    createAiObservabilityAlertRule(input: $input) {
        id
    }
}

mutation deleteAiObservabilityAlertRule($id: ID!) {
    deleteAiObservabilityAlertRule(id: $id)
}

mutation updateAiObservabilityAlertRule($id: ID!, $input: AiObservabilityAlertRuleInput!) {
    updateAiObservabilityAlertRule(id: $id, input: $input) {
        id
    }
}
```

- [ ] **Step 2: Create aiObservabilityNotificationChannels.graphql**

```graphql
query aiObservabilityNotificationChannels($workspaceId: ID!) {
    aiObservabilityNotificationChannels(workspaceId: $workspaceId) {
        config
        createdDate
        enabled
        id
        lastModifiedDate
        name
        type
        version
        workspaceId
    }
}

mutation createAiObservabilityNotificationChannel($input: AiObservabilityNotificationChannelInput!) {
    createAiObservabilityNotificationChannel(input: $input) {
        id
    }
}

mutation deleteAiObservabilityNotificationChannel($id: ID!) {
    deleteAiObservabilityNotificationChannel(id: $id)
}

mutation updateAiObservabilityNotificationChannel($id: ID!, $input: AiObservabilityNotificationChannelInput!) {
    updateAiObservabilityNotificationChannel(id: $id, input: $input) {
        id
    }
}
```

- [ ] **Step 3: Create aiObservabilityAlertEvents.graphql**

```graphql
query aiObservabilityAlertEvents($alertRuleId: ID!) {
    aiObservabilityAlertEvents(alertRuleId: $alertRuleId) {
        alertRuleId
        createdDate
        id
        message
        status
        triggeredValue
    }
}

mutation acknowledgeAiObservabilityAlertEvent($id: ID!) {
    acknowledgeAiObservabilityAlertEvent(id: $id) {
        id
        status
    }
}
```

- [ ] **Step 4: Run GraphQL codegen**

Run: `cd client && npx graphql-codegen`
Expected: generates updated `src/shared/middleware/graphql.ts` with new query/mutation hooks

- [ ] **Step 5: Commit**

```bash
cd client
git add src/graphql/automation/ai-gateway/aiObservabilityAlertRules.graphql \
  src/graphql/automation/ai-gateway/aiObservabilityNotificationChannels.graphql \
  src/graphql/automation/ai-gateway/aiObservabilityAlertEvents.graphql \
  src/shared/middleware/graphql.ts
git commit -m "732 client - Add GraphQL operations and codegen for alerting"
```

---

## Task 17: Client — Types and Sidebar Update

**Files:**
- Modify: `client/src/pages/automation/ai-gateway/types.ts`
- Modify: `client/src/pages/automation/ai-gateway/AiGateway.tsx`

- [ ] **Step 1: Add types to types.ts**

Add to `client/src/pages/automation/ai-gateway/types.ts`:

```typescript
import {
    AiObservabilityAlertEventsQuery,
    AiObservabilityAlertRulesQuery,
    AiObservabilityNotificationChannelsQuery,
    // ... existing imports ...
} from '@/shared/middleware/graphql';

// ... existing types unchanged ...

export type AiObservabilityAlertRuleType = NonNullable<
    NonNullable<AiObservabilityAlertRulesQuery['aiObservabilityAlertRules']>[number]
>;

export type AiObservabilityNotificationChannelType = NonNullable<
    NonNullable<AiObservabilityNotificationChannelsQuery['aiObservabilityNotificationChannels']>[number]
>;

export type AiObservabilityAlertEventType = NonNullable<
    NonNullable<AiObservabilityAlertEventsQuery['aiObservabilityAlertEvents']>[number]
>;
```

- [ ] **Step 2: Update AiGateway.tsx sidebar**

In `client/src/pages/automation/ai-gateway/AiGateway.tsx`:

Update the type union:
```typescript
type AiGatewayPageType = 'alerts' | 'budget' | 'models' | 'monitoring' | 'projects' | 'providers' | 'routing' | 'settings';
```

Add import:
```typescript
import AiObservabilityAlerts from './components/alerts/AiObservabilityAlerts';
```

Add `LeftSidebarNavItem` entry after the "Monitoring" item:
```typescript
<LeftSidebarNavItem
    item={{
        current: activePage === 'alerts',
        name: 'Alerts',
        onItemClick: () => setActivePage('alerts'),
    }}
/>
```

Add conditional render:
```typescript
{activePage === 'alerts' && <AiObservabilityAlerts />}
```

- [ ] **Step 3: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/types.ts src/pages/automation/ai-gateway/AiGateway.tsx
git commit -m "732 client - Add Alerts sidebar tab to AI Gateway"
```

---

## Task 18: Client — Alerts Page Component

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/alerts/AiObservabilityAlerts.tsx`

- [ ] **Step 1: Create AiObservabilityAlerts.tsx**

This component provides the main alerts page with three sub-tabs: Alert Rules, Notification Channels, and Alert History.

```typescript
import {useState} from 'react';

import AiObservabilityAlertHistory from './AiObservabilityAlertHistory';
import AiObservabilityAlertRuleDialog from './AiObservabilityAlertRuleDialog';
import AiObservabilityAlertRuleDeleteDialog from './AiObservabilityAlertRuleDeleteDialog';
import AiObservabilityNotificationChannels from './AiObservabilityNotificationChannels';

import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {
    useAiObservabilityAlertRulesQuery,
    useDeleteAiObservabilityAlertRuleMutation,
} from '@/shared/middleware/graphql';
import {useWorkspaceStore} from '@/shared/stores/workspace.store';
import {BellIcon, PlusIcon} from 'lucide-react';

import {AiObservabilityAlertRuleType} from '../../types';

type AlertsTabType = 'channels' | 'history' | 'rules';

const METRIC_LABELS: Record<string, string> = {
    COST: 'Cost',
    ERROR_RATE: 'Error Rate',
    LATENCY_P95: 'Latency P95',
    REQUEST_VOLUME: 'Request Volume',
    TOKEN_USAGE: 'Token Usage',
};

const CONDITION_LABELS: Record<string, string> = {
    EQUALS: '=',
    GREATER_THAN: '>',
    LESS_THAN: '<',
};

const AiObservabilityAlerts = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [activeTab, setActiveTab] = useState<AlertsTabType>('rules');
    const [editingAlertRule, setEditingAlertRule] = useState<AiObservabilityAlertRuleType | undefined>();
    const [deletingAlertRule, setDeletingAlertRule] = useState<AiObservabilityAlertRuleType | undefined>();
    const [showCreateDialog, setShowCreateDialog] = useState(false);

    const {data: alertRulesData, isLoading: alertRulesIsLoading} = useAiObservabilityAlertRulesQuery({
        workspaceId: currentWorkspaceId + '',
    });

    const alertRules = alertRulesData?.aiObservabilityAlertRules ?? [];

    if (activeTab === 'channels') {
        return (
            <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
                <TabBar activeTab={activeTab} setActiveTab={setActiveTab} />

                <AiObservabilityNotificationChannels />
            </div>
        );
    }

    if (activeTab === 'history') {
        return (
            <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
                <TabBar activeTab={activeTab} setActiveTab={setActiveTab} />

                <AiObservabilityAlertHistory alertRules={alertRules} />
            </div>
        );
    }

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <TabBar activeTab={activeTab} setActiveTab={setActiveTab} />

            <div className="mb-4 flex items-center justify-between">
                <h2 className="text-lg font-semibold">Alert Rules</h2>

                <button
                    className="flex items-center gap-1 rounded-md bg-primary px-3 py-1.5 text-sm text-primary-foreground hover:bg-primary/90"
                    onClick={() => setShowCreateDialog(true)}
                >
                    <PlusIcon className="size-4" />
                    New Alert Rule
                </button>
            </div>

            {alertRulesIsLoading ? (
                <PageLoader />
            ) : alertRules.length === 0 ? (
                <EmptyList
                    icon={<BellIcon className="size-12 text-muted-foreground" />}
                    message="Create an alert rule to monitor gateway metrics."
                    title="No Alert Rules"
                />
            ) : (
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead>
                            <tr className="border-b text-muted-foreground">
                                <th className="px-3 py-2 font-medium">Name</th>
                                <th className="px-3 py-2 font-medium">Metric</th>
                                <th className="px-3 py-2 font-medium">Condition</th>
                                <th className="px-3 py-2 font-medium">Window</th>
                                <th className="px-3 py-2 font-medium">Cooldown</th>
                                <th className="px-3 py-2 font-medium">Enabled</th>
                                <th className="px-3 py-2 font-medium">Actions</th>
                            </tr>
                        </thead>

                        <tbody>
                            {alertRules.map((alertRule) => (
                                <tr className="border-b hover:bg-muted/50" key={alertRule.id}>
                                    <td className="px-3 py-2 font-medium">{alertRule.name}</td>
                                    <td className="px-3 py-2">
                                        {METRIC_LABELS[alertRule.metric] || alertRule.metric}
                                    </td>
                                    <td className="px-3 py-2">
                                        {CONDITION_LABELS[alertRule.condition] || alertRule.condition}{' '}
                                        {alertRule.threshold}
                                    </td>
                                    <td className="px-3 py-2">{alertRule.windowMinutes}m</td>
                                    <td className="px-3 py-2">{alertRule.cooldownMinutes}m</td>
                                    <td className="px-3 py-2">
                                        <span
                                            className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                                                alertRule.enabled
                                                    ? 'bg-green-100 text-green-800'
                                                    : 'bg-gray-100 text-gray-800'
                                            }`}
                                        >
                                            {alertRule.enabled ? 'Yes' : 'No'}
                                        </span>
                                    </td>
                                    <td className="flex gap-2 px-3 py-2">
                                        <button
                                            className="text-sm text-primary hover:underline"
                                            onClick={() => setEditingAlertRule(alertRule)}
                                        >
                                            Edit
                                        </button>

                                        <button
                                            className="text-sm text-destructive hover:underline"
                                            onClick={() => setDeletingAlertRule(alertRule)}
                                        >
                                            Delete
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {showCreateDialog && (
                <AiObservabilityAlertRuleDialog onClose={() => setShowCreateDialog(false)} />
            )}

            {editingAlertRule && (
                <AiObservabilityAlertRuleDialog
                    alertRule={editingAlertRule}
                    onClose={() => setEditingAlertRule(undefined)}
                />
            )}

            {deletingAlertRule && (
                <AiObservabilityAlertRuleDeleteDialog
                    alertRule={deletingAlertRule}
                    onClose={() => setDeletingAlertRule(undefined)}
                />
            )}
        </div>
    );
};

interface TabBarProps {
    activeTab: AlertsTabType;
    setActiveTab: (tab: AlertsTabType) => void;
}

const TabBar = ({activeTab, setActiveTab}: TabBarProps) => {
    const tabs: {label: string; value: AlertsTabType}[] = [
        {label: 'Alert Rules', value: 'rules'},
        {label: 'Notification Channels', value: 'channels'},
        {label: 'Alert History', value: 'history'},
    ];

    return (
        <div className="mb-4 flex gap-1 border-b">
            {tabs.map((tab) => (
                <button
                    className={`px-4 py-2 text-sm font-medium ${
                        activeTab === tab.value
                            ? 'border-b-2 border-primary text-primary'
                            : 'text-muted-foreground hover:text-foreground'
                    }`}
                    key={tab.value}
                    onClick={() => setActiveTab(tab.value)}
                >
                    {tab.label}
                </button>
            ))}
        </div>
    );
};

export default AiObservabilityAlerts;
```

- [ ] **Step 2: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/alerts/AiObservabilityAlerts.tsx
git commit -m "732 client - Add Alerts page with alert rules list and sub-tab navigation"
```

---

## Task 19: Client — Alert Rule Create/Edit Dialog

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/alerts/AiObservabilityAlertRuleDialog.tsx`

- [ ] **Step 1: Create AiObservabilityAlertRuleDialog.tsx**

```typescript
import {
    AiObservabilityAlertCondition,
    AiObservabilityAlertMetric,
    useAiObservabilityNotificationChannelsQuery,
    useCreateAiObservabilityAlertRuleMutation,
    useUpdateAiObservabilityAlertRuleMutation,
} from '@/shared/middleware/graphql';
import {useWorkspaceStore} from '@/shared/stores/workspace.store';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

import {AiObservabilityAlertRuleType} from '../../types';

interface AiObservabilityAlertRuleDialogProps {
    alertRule?: AiObservabilityAlertRuleType;
    onClose: () => void;
}

const METRIC_OPTIONS: {label: string; value: AiObservabilityAlertMetric}[] = [
    {label: 'Error Rate', value: AiObservabilityAlertMetric.ErrorRate},
    {label: 'Latency P95', value: AiObservabilityAlertMetric.LatencyP95},
    {label: 'Cost', value: AiObservabilityAlertMetric.Cost},
    {label: 'Token Usage', value: AiObservabilityAlertMetric.TokenUsage},
    {label: 'Request Volume', value: AiObservabilityAlertMetric.RequestVolume},
];

const CONDITION_OPTIONS: {label: string; value: AiObservabilityAlertCondition}[] = [
    {label: 'Greater Than (>)', value: AiObservabilityAlertCondition.GreaterThan},
    {label: 'Less Than (<)', value: AiObservabilityAlertCondition.LessThan},
    {label: 'Equals (=)', value: AiObservabilityAlertCondition.Equals},
];

const AiObservabilityAlertRuleDialog = ({alertRule, onClose}: AiObservabilityAlertRuleDialogProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const queryClient = useQueryClient();

    const [cooldownMinutes, setCooldownMinutes] = useState(alertRule?.cooldownMinutes ?? 30);
    const [condition, setCondition] = useState<AiObservabilityAlertCondition>(
        (alertRule?.condition as AiObservabilityAlertCondition) ?? AiObservabilityAlertCondition.GreaterThan,
    );
    const [enabled, setEnabled] = useState(alertRule?.enabled ?? true);
    const [metric, setMetric] = useState<AiObservabilityAlertMetric>(
        (alertRule?.metric as AiObservabilityAlertMetric) ?? AiObservabilityAlertMetric.ErrorRate,
    );
    const [name, setName] = useState(alertRule?.name ?? '');
    const [selectedChannelIds, setSelectedChannelIds] = useState<string[]>(
        (alertRule?.channelIds as string[]) ?? [],
    );
    const [threshold, setThreshold] = useState(alertRule?.threshold ?? 0);
    const [windowMinutes, setWindowMinutes] = useState(alertRule?.windowMinutes ?? 5);

    const {data: channelsData} = useAiObservabilityNotificationChannelsQuery({
        workspaceId: currentWorkspaceId + '',
    });

    const channels = channelsData?.aiObservabilityNotificationChannels ?? [];

    const createMutation = useCreateAiObservabilityAlertRuleMutation();
    const updateMutation = useUpdateAiObservabilityAlertRuleMutation();

    const handleSubmit = () => {
        const input = {
            channelIds: selectedChannelIds,
            condition,
            cooldownMinutes,
            enabled,
            metric,
            name,
            threshold,
            windowMinutes,
            workspaceId: currentWorkspaceId + '',
        };

        const onSuccess = () => {
            queryClient.invalidateQueries({queryKey: ['aiObservabilityAlertRules']});

            onClose();
        };

        if (alertRule) {
            updateMutation.mutate({id: alertRule.id, input}, {onSuccess});
        } else {
            createMutation.mutate({input}, {onSuccess});
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-lg rounded-lg bg-background p-6 shadow-xl">
                <h3 className="mb-4 text-lg font-semibold">
                    {alertRule ? 'Edit Alert Rule' : 'Create Alert Rule'}
                </h3>

                <fieldset className="border-0 p-0">
                    <div className="mb-3">
                        <label className="mb-1 block text-sm font-medium">Name</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="Alert rule name"
                            value={name}
                        />
                    </div>

                    <div className="mb-3 grid grid-cols-2 gap-3">
                        <div>
                            <label className="mb-1 block text-sm font-medium">Metric</label>

                            <select
                                className="w-full rounded-md border px-3 py-2 text-sm"
                                onChange={(event) => setMetric(event.target.value as AiObservabilityAlertMetric)}
                                value={metric}
                            >
                                {METRIC_OPTIONS.map((option) => (
                                    <option key={option.value} value={option.value}>
                                        {option.label}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div>
                            <label className="mb-1 block text-sm font-medium">Condition</label>

                            <select
                                className="w-full rounded-md border px-3 py-2 text-sm"
                                onChange={(event) =>
                                    setCondition(event.target.value as AiObservabilityAlertCondition)
                                }
                                value={condition}
                            >
                                {CONDITION_OPTIONS.map((option) => (
                                    <option key={option.value} value={option.value}>
                                        {option.label}
                                    </option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <div className="mb-3">
                        <label className="mb-1 block text-sm font-medium">Threshold</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setThreshold(Number(event.target.value))}
                            step="0.01"
                            type="number"
                            value={threshold}
                        />
                    </div>

                    <div className="mb-3 grid grid-cols-2 gap-3">
                        <div>
                            <label className="mb-1 block text-sm font-medium">Window (minutes)</label>

                            <input
                                className="w-full rounded-md border px-3 py-2 text-sm"
                                min={1}
                                onChange={(event) => setWindowMinutes(Number(event.target.value))}
                                type="number"
                                value={windowMinutes}
                            />
                        </div>

                        <div>
                            <label className="mb-1 block text-sm font-medium">Cooldown (minutes)</label>

                            <input
                                className="w-full rounded-md border px-3 py-2 text-sm"
                                min={0}
                                onChange={(event) => setCooldownMinutes(Number(event.target.value))}
                                type="number"
                                value={cooldownMinutes}
                            />
                        </div>
                    </div>

                    {channels.length > 0 && (
                        <div className="mb-3">
                            <label className="mb-1 block text-sm font-medium">Notification Channels</label>

                            <div className="space-y-1">
                                {channels.map((channel) => (
                                    <label className="flex items-center gap-2 text-sm" key={channel.id}>
                                        <input
                                            checked={selectedChannelIds.includes(channel.id)}
                                            onChange={(event) => {
                                                if (event.target.checked) {
                                                    setSelectedChannelIds([...selectedChannelIds, channel.id]);
                                                } else {
                                                    setSelectedChannelIds(
                                                        selectedChannelIds.filter(
                                                            (channelId) => channelId !== channel.id,
                                                        ),
                                                    );
                                                }
                                            }}
                                            type="checkbox"
                                        />
                                        {channel.name} ({channel.type})
                                    </label>
                                ))}
                            </div>
                        </div>
                    )}

                    <div className="mb-4">
                        <label className="flex items-center gap-2 text-sm">
                            <input
                                checked={enabled}
                                onChange={(event) => setEnabled(event.target.checked)}
                                type="checkbox"
                            />
                            Enabled
                        </label>
                    </div>
                </fieldset>

                <div className="flex justify-end gap-2">
                    <button
                        className="rounded-md px-4 py-2 text-sm text-muted-foreground hover:bg-muted"
                        onClick={onClose}
                    >
                        Cancel
                    </button>

                    <button
                        className="rounded-md bg-primary px-4 py-2 text-sm text-primary-foreground hover:bg-primary/90"
                        disabled={!name.trim()}
                        onClick={handleSubmit}
                    >
                        {alertRule ? 'Update' : 'Create'}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default AiObservabilityAlertRuleDialog;
```

- [ ] **Step 2: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/alerts/AiObservabilityAlertRuleDialog.tsx
git commit -m "732 client - Add alert rule create/edit dialog"
```

---

## Task 20: Client — Alert Rule Delete Dialog

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/alerts/AiObservabilityAlertRuleDeleteDialog.tsx`

- [ ] **Step 1: Create AiObservabilityAlertRuleDeleteDialog.tsx**

```typescript
import {useDeleteAiObservabilityAlertRuleMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

import {AiObservabilityAlertRuleType} from '../../types';

interface AiObservabilityAlertRuleDeleteDialogProps {
    alertRule: AiObservabilityAlertRuleType;
    onClose: () => void;
}

const AiObservabilityAlertRuleDeleteDialog = ({
    alertRule,
    onClose,
}: AiObservabilityAlertRuleDeleteDialogProps) => {
    const queryClient = useQueryClient();

    const deleteMutation = useDeleteAiObservabilityAlertRuleMutation();

    const handleDelete = () => {
        deleteMutation.mutate(
            {id: alertRule.id},
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['aiObservabilityAlertRules']});

                    onClose();
                },
            },
        );
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-md rounded-lg bg-background p-6 shadow-xl">
                <h3 className="mb-2 text-lg font-semibold">Delete Alert Rule</h3>

                <p className="mb-4 text-sm text-muted-foreground">
                    Are you sure you want to delete the alert rule &quot;{alertRule.name}&quot;? This action cannot
                    be undone.
                </p>

                <div className="flex justify-end gap-2">
                    <button
                        className="rounded-md px-4 py-2 text-sm text-muted-foreground hover:bg-muted"
                        onClick={onClose}
                    >
                        Cancel
                    </button>

                    <button
                        className="rounded-md bg-destructive px-4 py-2 text-sm text-destructive-foreground hover:bg-destructive/90"
                        onClick={handleDelete}
                    >
                        Delete
                    </button>
                </div>
            </div>
        </div>
    );
};

export default AiObservabilityAlertRuleDeleteDialog;
```

- [ ] **Step 2: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/alerts/AiObservabilityAlertRuleDeleteDialog.tsx
git commit -m "732 client - Add alert rule delete confirmation dialog"
```

---

## Task 21: Client — Notification Channels Sub-Section

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/alerts/AiObservabilityNotificationChannels.tsx`
- Create: `client/src/pages/automation/ai-gateway/components/alerts/AiObservabilityNotificationChannelDialog.tsx`

- [ ] **Step 1: Create AiObservabilityNotificationChannels.tsx**

```typescript
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {
    useAiObservabilityNotificationChannelsQuery,
    useDeleteAiObservabilityNotificationChannelMutation,
} from '@/shared/middleware/graphql';
import {useWorkspaceStore} from '@/shared/stores/workspace.store';
import {useQueryClient} from '@tanstack/react-query';
import {MailIcon, PlusIcon} from 'lucide-react';
import {useState} from 'react';

import {AiObservabilityNotificationChannelType} from '../../types';
import AiObservabilityNotificationChannelDialog from './AiObservabilityNotificationChannelDialog';

const TYPE_LABELS: Record<string, string> = {
    EMAIL: 'Email',
    SLACK: 'Slack',
    WEBHOOK: 'Webhook',
};

const AiObservabilityNotificationChannels = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const queryClient = useQueryClient();

    const [editingChannel, setEditingChannel] = useState<AiObservabilityNotificationChannelType | undefined>();
    const [showCreateDialog, setShowCreateDialog] = useState(false);

    const {data: channelsData, isLoading: channelsIsLoading} = useAiObservabilityNotificationChannelsQuery({
        workspaceId: currentWorkspaceId + '',
    });

    const channels = channelsData?.aiObservabilityNotificationChannels ?? [];

    const deleteMutation = useDeleteAiObservabilityNotificationChannelMutation();

    const handleDelete = (channel: AiObservabilityNotificationChannelType) => {
        deleteMutation.mutate(
            {id: channel.id},
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['aiObservabilityNotificationChannels']});
                },
            },
        );
    };

    return (
        <>
            <div className="mb-4 flex items-center justify-between">
                <h2 className="text-lg font-semibold">Notification Channels</h2>

                <button
                    className="flex items-center gap-1 rounded-md bg-primary px-3 py-1.5 text-sm text-primary-foreground hover:bg-primary/90"
                    onClick={() => setShowCreateDialog(true)}
                >
                    <PlusIcon className="size-4" />
                    New Channel
                </button>
            </div>

            {channelsIsLoading ? (
                <PageLoader />
            ) : channels.length === 0 ? (
                <EmptyList
                    icon={<MailIcon className="size-12 text-muted-foreground" />}
                    message="Add a notification channel to receive alert notifications."
                    title="No Notification Channels"
                />
            ) : (
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead>
                            <tr className="border-b text-muted-foreground">
                                <th className="px-3 py-2 font-medium">Name</th>
                                <th className="px-3 py-2 font-medium">Type</th>
                                <th className="px-3 py-2 font-medium">Enabled</th>
                                <th className="px-3 py-2 font-medium">Actions</th>
                            </tr>
                        </thead>

                        <tbody>
                            {channels.map((channel) => (
                                <tr className="border-b hover:bg-muted/50" key={channel.id}>
                                    <td className="px-3 py-2 font-medium">{channel.name}</td>
                                    <td className="px-3 py-2">{TYPE_LABELS[channel.type] || channel.type}</td>
                                    <td className="px-3 py-2">
                                        <span
                                            className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                                                channel.enabled
                                                    ? 'bg-green-100 text-green-800'
                                                    : 'bg-gray-100 text-gray-800'
                                            }`}
                                        >
                                            {channel.enabled ? 'Yes' : 'No'}
                                        </span>
                                    </td>
                                    <td className="flex gap-2 px-3 py-2">
                                        <button
                                            className="text-sm text-primary hover:underline"
                                            onClick={() => setEditingChannel(channel)}
                                        >
                                            Edit
                                        </button>

                                        <button
                                            className="text-sm text-destructive hover:underline"
                                            onClick={() => handleDelete(channel)}
                                        >
                                            Delete
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {showCreateDialog && (
                <AiObservabilityNotificationChannelDialog onClose={() => setShowCreateDialog(false)} />
            )}

            {editingChannel && (
                <AiObservabilityNotificationChannelDialog
                    channel={editingChannel}
                    onClose={() => setEditingChannel(undefined)}
                />
            )}
        </>
    );
};

export default AiObservabilityNotificationChannels;
```

- [ ] **Step 2: Create AiObservabilityNotificationChannelDialog.tsx**

```typescript
import {
    AiObservabilityNotificationChannelType as NotificationChannelTypeEnum,
    useCreateAiObservabilityNotificationChannelMutation,
    useUpdateAiObservabilityNotificationChannelMutation,
} from '@/shared/middleware/graphql';
import {useWorkspaceStore} from '@/shared/stores/workspace.store';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

import {AiObservabilityNotificationChannelType} from '../../types';

interface AiObservabilityNotificationChannelDialogProps {
    channel?: AiObservabilityNotificationChannelType;
    onClose: () => void;
}

const TYPE_OPTIONS: {label: string; value: NotificationChannelTypeEnum}[] = [
    {label: 'Webhook', value: NotificationChannelTypeEnum.Webhook},
    {label: 'Email', value: NotificationChannelTypeEnum.Email},
    {label: 'Slack', value: NotificationChannelTypeEnum.Slack},
];

const AiObservabilityNotificationChannelDialog = ({
    channel,
    onClose,
}: AiObservabilityNotificationChannelDialogProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const queryClient = useQueryClient();

    const [config, setConfig] = useState(channel?.config ?? '');
    const [enabled, setEnabled] = useState(channel?.enabled ?? true);
    const [name, setName] = useState(channel?.name ?? '');
    const [type, setType] = useState<NotificationChannelTypeEnum>(
        (channel?.type as NotificationChannelTypeEnum) ?? NotificationChannelTypeEnum.Webhook,
    );

    const createMutation = useCreateAiObservabilityNotificationChannelMutation();
    const updateMutation = useUpdateAiObservabilityNotificationChannelMutation();

    const handleSubmit = () => {
        const input = {
            config,
            enabled,
            name,
            type,
            workspaceId: currentWorkspaceId + '',
        };

        const onSuccess = () => {
            queryClient.invalidateQueries({queryKey: ['aiObservabilityNotificationChannels']});

            onClose();
        };

        if (channel) {
            updateMutation.mutate({id: channel.id, input}, {onSuccess});
        } else {
            createMutation.mutate({input}, {onSuccess});
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-lg rounded-lg bg-background p-6 shadow-xl">
                <h3 className="mb-4 text-lg font-semibold">
                    {channel ? 'Edit Notification Channel' : 'Create Notification Channel'}
                </h3>

                <fieldset className="border-0 p-0">
                    <div className="mb-3">
                        <label className="mb-1 block text-sm font-medium">Name</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="Channel name"
                            value={name}
                        />
                    </div>

                    <div className="mb-3">
                        <label className="mb-1 block text-sm font-medium">Type</label>

                        <select
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setType(event.target.value as NotificationChannelTypeEnum)}
                            value={type}
                        >
                            {TYPE_OPTIONS.map((option) => (
                                <option key={option.value} value={option.value}>
                                    {option.label}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="mb-3">
                        <label className="mb-1 block text-sm font-medium">Configuration (JSON)</label>

                        <textarea
                            className="w-full rounded-md border px-3 py-2 font-mono text-sm"
                            onChange={(event) => setConfig(event.target.value)}
                            placeholder={
                                type === NotificationChannelTypeEnum.Webhook
                                    ? '{"url": "https://...", "headers": {}}'
                                    : type === NotificationChannelTypeEnum.Email
                                      ? '{"recipients": ["admin@example.com"]}'
                                      : '{"webhookUrl": "https://hooks.slack.com/..."}'
                            }
                            rows={4}
                            value={config}
                        />
                    </div>

                    <div className="mb-4">
                        <label className="flex items-center gap-2 text-sm">
                            <input
                                checked={enabled}
                                onChange={(event) => setEnabled(event.target.checked)}
                                type="checkbox"
                            />
                            Enabled
                        </label>
                    </div>
                </fieldset>

                <div className="flex justify-end gap-2">
                    <button
                        className="rounded-md px-4 py-2 text-sm text-muted-foreground hover:bg-muted"
                        onClick={onClose}
                    >
                        Cancel
                    </button>

                    <button
                        className="rounded-md bg-primary px-4 py-2 text-sm text-primary-foreground hover:bg-primary/90"
                        disabled={!name.trim() || !config.trim()}
                        onClick={handleSubmit}
                    >
                        {channel ? 'Update' : 'Create'}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default AiObservabilityNotificationChannelDialog;
```

- [ ] **Step 3: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/alerts/AiObservabilityNotificationChannels.tsx \
  src/pages/automation/ai-gateway/components/alerts/AiObservabilityNotificationChannelDialog.tsx
git commit -m "732 client - Add notification channels management sub-section"
```

---

## Task 22: Client — Alert History Timeline

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/alerts/AiObservabilityAlertHistory.tsx`

- [ ] **Step 1: Create AiObservabilityAlertHistory.tsx**

```typescript
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {
    useAcknowledgeAiObservabilityAlertEventMutation,
    useAiObservabilityAlertEventsQuery,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ClockIcon} from 'lucide-react';
import {useState} from 'react';

import {AiObservabilityAlertRuleType} from '../../types';

interface AiObservabilityAlertHistoryProps {
    alertRules: AiObservabilityAlertRuleType[];
}

const STATUS_STYLES: Record<string, string> = {
    ACKNOWLEDGED: 'bg-blue-100 text-blue-800',
    RESOLVED: 'bg-green-100 text-green-800',
    TRIGGERED: 'bg-red-100 text-red-800',
};

const AiObservabilityAlertHistory = ({alertRules}: AiObservabilityAlertHistoryProps) => {
    const queryClient = useQueryClient();

    const [selectedRuleId, setSelectedRuleId] = useState<string>(alertRules[0]?.id ?? '');

    const {data: eventsData, isLoading: eventsIsLoading} = useAiObservabilityAlertEventsQuery(
        {alertRuleId: selectedRuleId},
        {enabled: !!selectedRuleId},
    );

    const events = eventsData?.aiObservabilityAlertEvents ?? [];

    const acknowledgeMutation = useAcknowledgeAiObservabilityAlertEventMutation();

    const handleAcknowledge = (eventId: string) => {
        acknowledgeMutation.mutate(
            {id: eventId},
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['aiObservabilityAlertEvents']});
                },
            },
        );
    };

    return (
        <>
            <div className="mb-4 flex items-center justify-between">
                <h2 className="text-lg font-semibold">Alert History</h2>

                {alertRules.length > 0 && (
                    <select
                        className="rounded-md border px-3 py-1.5 text-sm"
                        onChange={(event) => setSelectedRuleId(event.target.value)}
                        value={selectedRuleId}
                    >
                        {alertRules.map((alertRule) => (
                            <option key={alertRule.id} value={alertRule.id}>
                                {alertRule.name}
                            </option>
                        ))}
                    </select>
                )}
            </div>

            {eventsIsLoading ? (
                <PageLoader />
            ) : events.length === 0 ? (
                <EmptyList
                    icon={<ClockIcon className="size-12 text-muted-foreground" />}
                    message="No alerts have been triggered yet."
                    title="No Alert Events"
                />
            ) : (
                <div className="space-y-2">
                    {events.map((event) => (
                        <div className="flex items-start gap-3 rounded-md border px-4 py-3" key={event.id}>
                            <div className="flex-1">
                                <div className="mb-1 flex items-center gap-2">
                                    <span
                                        className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                                            STATUS_STYLES[event.status] || ''
                                        }`}
                                    >
                                        {event.status}
                                    </span>

                                    <span className="text-xs text-muted-foreground">
                                        {event.createdDate
                                            ? new Date(Number(event.createdDate)).toLocaleString()
                                            : '-'}
                                    </span>
                                </div>

                                <p className="text-sm">{event.message || '-'}</p>

                                {event.triggeredValue != null && (
                                    <p className="mt-1 text-xs text-muted-foreground">
                                        Triggered value: {event.triggeredValue}
                                    </p>
                                )}
                            </div>

                            {event.status === 'TRIGGERED' && (
                                <button
                                    className="rounded-md border px-3 py-1 text-xs hover:bg-muted"
                                    onClick={() => handleAcknowledge(event.id)}
                                >
                                    Acknowledge
                                </button>
                            )}
                        </div>
                    ))}
                </div>
            )}
        </>
    );
};

export default AiObservabilityAlertHistory;
```

- [ ] **Step 2: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/alerts/AiObservabilityAlertHistory.tsx
git commit -m "732 client - Add alert history timeline with acknowledge action"
```

---

## Task 23: Client — Format, Lint, Type Check

- [ ] **Step 1: Run client checks**

```bash
cd client
npm run format
npm run check
```

Expected: All checks pass. Fix any ESLint sort-keys, import ordering, or TypeScript issues.

- [ ] **Step 2: Commit any formatting fixes**

```bash
cd client
git add -A
git commit -m "732 client - Fix lint and formatting in alerting components"
```

---

## Task 24: Final Verification

- [ ] **Step 1: Full server compilation**

```bash
./gradlew clean compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Server formatting**

```bash
./gradlew spotlessApply
```

- [ ] **Step 3: Commit any spotless fixes**

```bash
git add -A
git commit -m "732 Apply Spotless formatting to alerting code"
```
