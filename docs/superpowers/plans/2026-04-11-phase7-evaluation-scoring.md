# Phase 7: Evaluation & Scoring — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add gateway-native evaluation and scoring for production traces. Supports manual annotation from the UI, programmatic scoring via REST API, and automated LLM-as-judge evaluation with configurable rules, filters, and sampling.

**Architecture:** Four new domain entities (`AiObservabilityScore`, `AiObservabilityScoreConfig`, `AiObservabilityEvalRule`, `AiObservabilityEvalExecution`) in the existing `automation-ai-gateway` module. Manual scores are created via GraphQL mutation. Programmatic scores via `POST /api/ai-gateway/v1/scores`. LLM-as-judge evaluation runs when a trace completes: check enabled eval rules, apply filters and sampling, delay, call LLM via `AiGatewayChatModelFactory`, parse response, create score. New "Scores" sidebar tab in the client with score configs, eval rules management, and inline scoring in trace detail.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JDBC, PostgreSQL, Liquibase, GraphQL (Spring GraphQL), React 19, TypeScript 5.9, TanStack Query, Tailwind CSS

**Reference spec:** `docs/superpowers/specs/2026-04-11-ai-gateway-observability-platform-design.md` — Phase 7 section

---

## File Map

### Server — API module (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `src/main/java/.../domain/AiObservabilityScore.java` | Score domain entity |
| Create | `src/main/java/.../domain/AiObservabilityScoreDataType.java` | Enum: NUMERIC, BOOLEAN, CATEGORICAL |
| Create | `src/main/java/.../domain/AiObservabilityScoreSource.java` | Enum: MANUAL, API, LLM_JUDGE |
| Create | `src/main/java/.../domain/AiObservabilityScoreConfig.java` | Score config domain entity |
| Create | `src/main/java/.../domain/AiObservabilityEvalRule.java` | Eval rule domain entity |
| Create | `src/main/java/.../domain/AiObservabilityEvalExecution.java` | Eval execution domain entity |
| Create | `src/main/java/.../domain/AiObservabilityEvalExecutionStatus.java` | Enum: PENDING, COMPLETED, ERROR |
| Create | `src/main/java/.../repository/AiObservabilityScoreRepository.java` | Score repository |
| Create | `src/main/java/.../repository/AiObservabilityScoreConfigRepository.java` | Score config repository |
| Create | `src/main/java/.../repository/AiObservabilityEvalRuleRepository.java` | Eval rule repository |
| Create | `src/main/java/.../repository/AiObservabilityEvalExecutionRepository.java` | Eval execution repository |
| Create | `src/main/java/.../service/AiObservabilityScoreService.java` | Score service interface |
| Create | `src/main/java/.../service/AiObservabilityScoreConfigService.java` | Score config service interface |
| Create | `src/main/java/.../service/AiObservabilityEvalRuleService.java` | Eval rule service interface |
| Create | `src/main/java/.../service/AiObservabilityEvalExecutionService.java` | Eval execution service interface |

### Server — Service module (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/`)

| Action | Path | Purpose |
|---|---|---|
| Modify | `src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000002_ai_observability_init.xml` | Add Phase 7 tables |
| Create | `src/main/java/.../service/AiObservabilityScoreServiceImpl.java` | Score service impl |
| Create | `src/main/java/.../service/AiObservabilityScoreConfigServiceImpl.java` | Score config service impl |
| Create | `src/main/java/.../service/AiObservabilityEvalRuleServiceImpl.java` | Eval rule service impl |
| Create | `src/main/java/.../service/AiObservabilityEvalExecutionServiceImpl.java` | Eval execution service impl |
| Create | `src/main/java/.../evaluation/AiObservabilityEvalExecutor.java` | LLM-as-judge evaluation executor |
| Modify | `src/main/java/.../facade/AiGatewayFacade.java` | Trigger eval on trace completion |

### Server — GraphQL module (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `src/main/resources/graphql/ai-observability-score.graphqls` | Score GraphQL schema |
| Create | `src/main/resources/graphql/ai-observability-score-config.graphqls` | Score config GraphQL schema |
| Create | `src/main/resources/graphql/ai-observability-eval-rule.graphqls` | Eval rule GraphQL schema |
| Create | `src/main/resources/graphql/ai-observability-eval-execution.graphqls` | Eval execution GraphQL schema |
| Create | `src/main/java/.../web/graphql/AiObservabilityScoreGraphQlController.java` | Score queries and mutations |
| Create | `src/main/java/.../web/graphql/AiObservabilityScoreConfigGraphQlController.java` | Score config CRUD |
| Create | `src/main/java/.../web/graphql/AiObservabilityEvalRuleGraphQlController.java` | Eval rule CRUD |

### Server — Public REST module (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-public-rest/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `src/main/java/.../public_/web/rest/AiGatewayScoreApiController.java` | POST /api/ai-gateway/v1/scores |

### Client (`client/src/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `graphql/automation/ai-gateway/aiObservabilityScores.graphql` | Score queries/mutations |
| Create | `graphql/automation/ai-gateway/aiObservabilityScoreConfigs.graphql` | Score config queries/mutations |
| Create | `graphql/automation/ai-gateway/aiObservabilityEvalRules.graphql` | Eval rule queries/mutations |
| Modify | `pages/automation/ai-gateway/types.ts` | Add score/eval types |
| Modify | `pages/automation/ai-gateway/AiGateway.tsx` | Add Scores sidebar tab |
| Create | `pages/automation/ai-gateway/components/scores/AiObservabilityScores.tsx` | Scores tab root component |
| Create | `pages/automation/ai-gateway/components/scores/AiObservabilityScoreConfigDialog.tsx` | Score config create/edit dialog |
| Create | `pages/automation/ai-gateway/components/scores/AiObservabilityEvalRules.tsx` | Eval rules list |
| Create | `pages/automation/ai-gateway/components/scores/AiObservabilityEvalRuleDialog.tsx` | Eval rule create/edit dialog |
| Modify | `pages/automation/ai-gateway/components/traces/AiObservabilityTraceDetail.tsx` | Add inline scoring controls |

---

## Task 1: Liquibase Migration — Phase 7 Tables

**Files:**
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000002_ai_observability_init.xml`

- [ ] **Step 1: Add Phase 7 changeset to the existing migration file**

Append a new changeset (id `00000000000002-phase7`) after the existing Phase 1 changeset closing tag:

```xml
    <changeSet id="00000000000002-phase7" author="Ivica Cardic">
        <preConditions onFail="MARK_RAN">
            <not>
                <tableExists tableName="ai_observability_score_config"/>
            </not>
        </preConditions>

        <createTable tableName="ai_observability_score_config">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="workspace_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="name" type="VARCHAR(256)">
                <constraints nullable="false"/>
            </column>
            <column name="data_type" type="INT"/>
            <column name="min_value" type="DECIMAL(10,4)"/>
            <column name="max_value" type="DECIMAL(10,4)"/>
            <column name="categories" type="TEXT"/>
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

        <addUniqueConstraint tableName="ai_observability_score_config"
                             columnNames="workspace_id, name"
                             constraintName="uk_ai_obs_score_config_workspace_name"/>

        <createTable tableName="ai_observability_eval_rule">
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
            <column name="score_config_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="prompt_template" type="TEXT">
                <constraints nullable="false"/>
            </column>
            <column name="model" type="VARCHAR(256)">
                <constraints nullable="false"/>
            </column>
            <column name="filters" type="TEXT"/>
            <column name="sampling_rate" type="DECIMAL(5,4)">
                <constraints nullable="false"/>
            </column>
            <column name="delay_seconds" type="INT"/>
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

        <addForeignKeyConstraint constraintName="fk_ai_obs_eval_rule_score_config"
                                 baseTableName="ai_observability_eval_rule" baseColumnNames="score_config_id"
                                 referencedTableName="ai_observability_score_config" referencedColumnNames="id"/>

        <createTable tableName="ai_observability_score">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="workspace_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="trace_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="span_id" type="BIGINT"/>
            <column name="name" type="VARCHAR(256)">
                <constraints nullable="false"/>
            </column>
            <column name="value" type="DECIMAL(10,4)"/>
            <column name="string_value" type="VARCHAR(256)"/>
            <column name="data_type" type="INT"/>
            <column name="source" type="INT"/>
            <column name="comment" type="TEXT"/>
            <column name="eval_rule_id" type="BIGINT"/>
            <column name="created_by" type="VARCHAR(50)"/>
            <column name="created_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addForeignKeyConstraint constraintName="fk_ai_obs_score_trace"
                                 baseTableName="ai_observability_score" baseColumnNames="trace_id"
                                 referencedTableName="ai_observability_trace" referencedColumnNames="id"/>

        <addForeignKeyConstraint constraintName="fk_ai_obs_score_span"
                                 baseTableName="ai_observability_score" baseColumnNames="span_id"
                                 referencedTableName="ai_observability_span" referencedColumnNames="id"/>

        <addForeignKeyConstraint constraintName="fk_ai_obs_score_eval_rule"
                                 baseTableName="ai_observability_score" baseColumnNames="eval_rule_id"
                                 referencedTableName="ai_observability_eval_rule" referencedColumnNames="id"/>

        <createIndex tableName="ai_observability_score" indexName="idx_ai_obs_score_trace">
            <column name="trace_id"/>
        </createIndex>

        <createIndex tableName="ai_observability_score" indexName="idx_ai_obs_score_span">
            <column name="span_id"/>
        </createIndex>

        <createIndex tableName="ai_observability_score" indexName="idx_ai_obs_score_name">
            <column name="name"/>
        </createIndex>

        <createIndex tableName="ai_observability_score" indexName="idx_ai_obs_score_source">
            <column name="source"/>
        </createIndex>

        <createIndex tableName="ai_observability_score" indexName="idx_ai_obs_score_created">
            <column name="created_date"/>
        </createIndex>

        <createTable tableName="ai_observability_eval_execution">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="eval_rule_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="trace_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="score_id" type="BIGINT"/>
            <column name="status" type="INT"/>
            <column name="error_message" type="TEXT"/>
            <column name="created_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addForeignKeyConstraint constraintName="fk_ai_obs_eval_exec_rule"
                                 baseTableName="ai_observability_eval_execution" baseColumnNames="eval_rule_id"
                                 referencedTableName="ai_observability_eval_rule" referencedColumnNames="id"/>

        <addForeignKeyConstraint constraintName="fk_ai_obs_eval_exec_trace"
                                 baseTableName="ai_observability_eval_execution" baseColumnNames="trace_id"
                                 referencedTableName="ai_observability_trace" referencedColumnNames="id"/>

        <addForeignKeyConstraint constraintName="fk_ai_obs_eval_exec_score"
                                 baseTableName="ai_observability_eval_execution" baseColumnNames="score_id"
                                 referencedTableName="ai_observability_score" referencedColumnNames="id"/>
    </changeSet>
```

- [ ] **Step 2: Verify the migration compiles**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000002_ai_observability_init.xml
git commit -m "732 Add Liquibase migration for evaluation and scoring tables (score, score_config, eval_rule, eval_execution)"
```

---

## Task 2: Enum Domain Classes

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityScoreDataType.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityScoreSource.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityEvalExecutionStatus.java`

- [ ] **Step 1: Create AiObservabilityScoreDataType**

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
public enum AiObservabilityScoreDataType {

    NUMERIC,
    BOOLEAN,
    CATEGORICAL
}
```

- [ ] **Step 2: Create AiObservabilityScoreSource**

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
public enum AiObservabilityScoreSource {

    MANUAL,
    API,
    LLM_JUDGE
}
```

- [ ] **Step 3: Create AiObservabilityEvalExecutionStatus**

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
public enum AiObservabilityEvalExecutionStatus {

    PENDING,
    COMPLETED,
    ERROR
}
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityScoreDataType.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityScoreSource.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityEvalExecutionStatus.java
git commit -m "732 Add evaluation and scoring enum types (ScoreDataType, ScoreSource, EvalExecutionStatus)"
```

---

## Task 3: Score Domain Entity

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityScore.java`

- [ ] **Step 1: Create AiObservabilityScore**

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
@Table("ai_observability_score")
public class AiObservabilityScore {

    @Column
    private String comment;

    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("data_type")
    private int dataType;

    @Column("eval_rule_id")
    private Long evalRuleId;

    @Id
    private Long id;

    @Column
    private String name;

    @Column
    private int source;

    @Column("span_id")
    private Long spanId;

    @Column("string_value")
    private String stringValue;

    @Column("trace_id")
    private Long traceId;

    @Column
    private BigDecimal value;

    @Column("workspace_id")
    private Long workspaceId;

    private AiObservabilityScore() {
    }

    public AiObservabilityScore(
        Long workspaceId, Long traceId, String name,
        AiObservabilityScoreDataType dataType, AiObservabilityScoreSource source) {

        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notNull(traceId, "traceId must not be null");
        Validate.notBlank(name, "name must not be blank");
        Validate.notNull(dataType, "dataType must not be null");
        Validate.notNull(source, "source must not be null");

        this.dataType = dataType.ordinal();
        this.name = name;
        this.source = source.ordinal();
        this.traceId = traceId;
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilityScore aiObservabilityScore)) {
            return false;
        }

        return Objects.equals(id, aiObservabilityScore.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public String getComment() {
        return comment;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public AiObservabilityScoreDataType getDataType() {
        return AiObservabilityScoreDataType.values()[dataType];
    }

    public Long getEvalRuleId() {
        return evalRuleId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AiObservabilityScoreSource getSource() {
        return AiObservabilityScoreSource.values()[source];
    }

    public Long getSpanId() {
        return spanId;
    }

    public String getStringValue() {
        return stringValue;
    }

    public Long getTraceId() {
        return traceId;
    }

    public BigDecimal getValue() {
        return value;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setEvalRuleId(Long evalRuleId) {
        this.evalRuleId = evalRuleId;
    }

    public void setSpanId(Long spanId) {
        this.spanId = spanId;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "AiObservabilityScore{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", traceId=" + traceId +
            ", name='" + name + '\'' +
            ", source=" + getSource() +
            ", dataType=" + getDataType() +
            '}';
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityScore.java
git commit -m "732 Add AiObservabilityScore domain entity"
```

---

## Task 4: ScoreConfig Domain Entity

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityScoreConfig.java`

- [ ] **Step 1: Create AiObservabilityScoreConfig**

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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_observability_score_config")
public class AiObservabilityScoreConfig {

    @Column
    private String categories;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("data_type")
    private Integer dataType;

    @Column
    private String description;

    @Id
    private Long id;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column("max_value")
    private BigDecimal maxValue;

    @Column("min_value")
    private BigDecimal minValue;

    @Column
    private String name;

    @Version
    private int version;

    @Column("workspace_id")
    private Long workspaceId;

    private AiObservabilityScoreConfig() {
    }

    public AiObservabilityScoreConfig(Long workspaceId, String name) {
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

        if (!(object instanceof AiObservabilityScoreConfig aiObservabilityScoreConfig)) {
            return false;
        }

        return Objects.equals(id, aiObservabilityScoreConfig.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public String getCategories() {
        return categories;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public AiObservabilityScoreDataType getDataType() {
        if (dataType == null) {
            return null;
        }

        return AiObservabilityScoreDataType.values()[dataType];
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

    public BigDecimal getMaxValue() {
        return maxValue;
    }

    public BigDecimal getMinValue() {
        return minValue;
    }

    public String getName() {
        return name;
    }

    public int getVersion() {
        return version;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    public void setDataType(AiObservabilityScoreDataType dataType) {
        this.dataType = dataType == null ? null : dataType.ordinal();
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setMaxValue(BigDecimal maxValue) {
        this.maxValue = maxValue;
    }

    public void setMinValue(BigDecimal minValue) {
        this.minValue = minValue;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "AiObservabilityScoreConfig{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", name='" + name + '\'' +
            ", dataType=" + getDataType() +
            '}';
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityScoreConfig.java
git commit -m "732 Add AiObservabilityScoreConfig domain entity"
```

---

## Task 5: EvalRule Domain Entity

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityEvalRule.java`

- [ ] **Step 1: Create AiObservabilityEvalRule**

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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_observability_eval_rule")
public class AiObservabilityEvalRule {

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("delay_seconds")
    private Integer delaySeconds;

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
    private String model;

    @Column
    private String name;

    @Column("project_id")
    private Long projectId;

    @Column("prompt_template")
    private String promptTemplate;

    @Column("sampling_rate")
    private BigDecimal samplingRate;

    @Column("score_config_id")
    private Long scoreConfigId;

    @Version
    private int version;

    @Column("workspace_id")
    private Long workspaceId;

    private AiObservabilityEvalRule() {
    }

    public AiObservabilityEvalRule(
        Long workspaceId, String name, Long scoreConfigId,
        String promptTemplate, String model, BigDecimal samplingRate) {

        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notBlank(name, "name must not be blank");
        Validate.notNull(scoreConfigId, "scoreConfigId must not be null");
        Validate.notBlank(promptTemplate, "promptTemplate must not be blank");
        Validate.notBlank(model, "model must not be blank");
        Validate.notNull(samplingRate, "samplingRate must not be null");

        this.enabled = false;
        this.model = model;
        this.name = name;
        this.promptTemplate = promptTemplate;
        this.samplingRate = samplingRate;
        this.scoreConfigId = scoreConfigId;
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilityEvalRule aiObservabilityEvalRule)) {
            return false;
        }

        return Objects.equals(id, aiObservabilityEvalRule.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Integer getDelaySeconds() {
        return delaySeconds;
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

    public String getModel() {
        return model;
    }

    public String getName() {
        return name;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public BigDecimal getSamplingRate() {
        return samplingRate;
    }

    public Long getScoreConfigId() {
        return scoreConfigId;
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

    public void setDelaySeconds(Integer delaySeconds) {
        this.delaySeconds = delaySeconds;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setFilters(String filters) {
        this.filters = filters;
    }

    public void setModel(String model) {
        Validate.notBlank(model, "model must not be blank");

        this.model = model;
    }

    public void setName(String name) {
        Validate.notBlank(name, "name must not be blank");

        this.name = name;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setPromptTemplate(String promptTemplate) {
        Validate.notBlank(promptTemplate, "promptTemplate must not be blank");

        this.promptTemplate = promptTemplate;
    }

    public void setSamplingRate(BigDecimal samplingRate) {
        Validate.notNull(samplingRate, "samplingRate must not be null");

        this.samplingRate = samplingRate;
    }

    public void setScoreConfigId(Long scoreConfigId) {
        Validate.notNull(scoreConfigId, "scoreConfigId must not be null");

        this.scoreConfigId = scoreConfigId;
    }

    @Override
    public String toString() {
        return "AiObservabilityEvalRule{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", name='" + name + '\'' +
            ", model='" + model + '\'' +
            ", enabled=" + enabled +
            ", samplingRate=" + samplingRate +
            '}';
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityEvalRule.java
git commit -m "732 Add AiObservabilityEvalRule domain entity"
```

---

## Task 6: EvalExecution Domain Entity

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityEvalExecution.java`

- [ ] **Step 1: Create AiObservabilityEvalExecution**

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
@Table("ai_observability_eval_execution")
public class AiObservabilityEvalExecution {

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("error_message")
    private String errorMessage;

    @Column("eval_rule_id")
    private Long evalRuleId;

    @Id
    private Long id;

    @Column("score_id")
    private Long scoreId;

    @Column
    private int status;

    @Column("trace_id")
    private Long traceId;

    private AiObservabilityEvalExecution() {
    }

    public AiObservabilityEvalExecution(Long evalRuleId, Long traceId) {
        Validate.notNull(evalRuleId, "evalRuleId must not be null");
        Validate.notNull(traceId, "traceId must not be null");

        this.evalRuleId = evalRuleId;
        this.status = AiObservabilityEvalExecutionStatus.PENDING.ordinal();
        this.traceId = traceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilityEvalExecution aiObservabilityEvalExecution)) {
            return false;
        }

        return Objects.equals(id, aiObservabilityEvalExecution.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Long getEvalRuleId() {
        return evalRuleId;
    }

    public Long getId() {
        return id;
    }

    public Long getScoreId() {
        return scoreId;
    }

    public AiObservabilityEvalExecutionStatus getStatus() {
        return AiObservabilityEvalExecutionStatus.values()[status];
    }

    public Long getTraceId() {
        return traceId;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setScoreId(Long scoreId) {
        this.scoreId = scoreId;
    }

    public void setStatus(AiObservabilityEvalExecutionStatus status) {
        Validate.notNull(status, "status must not be null");

        this.status = status.ordinal();
    }

    @Override
    public String toString() {
        return "AiObservabilityEvalExecution{" +
            "id=" + id +
            ", evalRuleId=" + evalRuleId +
            ", traceId=" + traceId +
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
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiObservabilityEvalExecution.java
git commit -m "732 Add AiObservabilityEvalExecution domain entity"
```

---

## Task 7: Repository Interfaces

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityScoreRepository.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityScoreConfigRepository.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityEvalRuleRepository.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityEvalExecutionRepository.java`

- [ ] **Step 1: Create AiObservabilityScoreRepository**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityScore;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiObservabilityScoreRepository extends ListCrudRepository<AiObservabilityScore, Long> {

    List<AiObservabilityScore> findAllByTraceId(Long traceId);

    List<AiObservabilityScore> findAllByWorkspaceId(Long workspaceId);

    List<AiObservabilityScore> findAllByWorkspaceIdAndName(Long workspaceId, String name);
}
```

- [ ] **Step 2: Create AiObservabilityScoreConfigRepository**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityScoreConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiObservabilityScoreConfigRepository extends ListCrudRepository<AiObservabilityScoreConfig, Long> {

    List<AiObservabilityScoreConfig> findAllByWorkspaceId(Long workspaceId);

    Optional<AiObservabilityScoreConfig> findByWorkspaceIdAndName(Long workspaceId, String name);
}
```

- [ ] **Step 3: Create AiObservabilityEvalRuleRepository**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityEvalRule;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiObservabilityEvalRuleRepository extends ListCrudRepository<AiObservabilityEvalRule, Long> {

    List<AiObservabilityEvalRule> findAllByWorkspaceId(Long workspaceId);

    List<AiObservabilityEvalRule> findAllByWorkspaceIdAndEnabled(Long workspaceId, boolean enabled);
}
```

- [ ] **Step 4: Create AiObservabilityEvalExecutionRepository**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityEvalExecution;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiObservabilityEvalExecutionRepository extends ListCrudRepository<AiObservabilityEvalExecution, Long> {

    List<AiObservabilityEvalExecution> findAllByEvalRuleId(Long evalRuleId);

    List<AiObservabilityEvalExecution> findAllByTraceId(Long traceId);

    List<AiObservabilityEvalExecution> findAllByStatus(int status);
}
```

- [ ] **Step 5: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityScoreRepository.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityScoreConfigRepository.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityEvalRuleRepository.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiObservabilityEvalExecutionRepository.java
git commit -m "732 Add evaluation and scoring repository interfaces"
```

---

## Task 8: Service Interfaces

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityScoreService.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityScoreConfigService.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityEvalRuleService.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityEvalExecutionService.java`

- [ ] **Step 1: Create AiObservabilityScoreService**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityScore;
import java.util.List;

/**
 * @version ee
 */
public interface AiObservabilityScoreService {

    AiObservabilityScore create(AiObservabilityScore score);

    void delete(long id);

    AiObservabilityScore getScore(long id);

    List<AiObservabilityScore> getScoresByTrace(Long traceId);

    List<AiObservabilityScore> getScoresByWorkspace(Long workspaceId);

    List<AiObservabilityScore> getScoresByWorkspaceAndName(Long workspaceId, String name);
}
```

- [ ] **Step 2: Create AiObservabilityScoreConfigService**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityScoreConfig;
import java.util.List;

/**
 * @version ee
 */
public interface AiObservabilityScoreConfigService {

    AiObservabilityScoreConfig create(AiObservabilityScoreConfig scoreConfig);

    void delete(long id);

    AiObservabilityScoreConfig getScoreConfig(long id);

    List<AiObservabilityScoreConfig> getScoreConfigsByWorkspace(Long workspaceId);

    AiObservabilityScoreConfig update(AiObservabilityScoreConfig scoreConfig);
}
```

- [ ] **Step 3: Create AiObservabilityEvalRuleService**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityEvalRule;
import java.util.List;

/**
 * @version ee
 */
public interface AiObservabilityEvalRuleService {

    AiObservabilityEvalRule create(AiObservabilityEvalRule evalRule);

    void delete(long id);

    AiObservabilityEvalRule getEvalRule(long id);

    List<AiObservabilityEvalRule> getEvalRulesByWorkspace(Long workspaceId);

    List<AiObservabilityEvalRule> getEnabledEvalRulesByWorkspace(Long workspaceId);

    AiObservabilityEvalRule update(AiObservabilityEvalRule evalRule);
}
```

- [ ] **Step 4: Create AiObservabilityEvalExecutionService**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityEvalExecution;
import java.util.List;

/**
 * @version ee
 */
public interface AiObservabilityEvalExecutionService {

    AiObservabilityEvalExecution create(AiObservabilityEvalExecution evalExecution);

    List<AiObservabilityEvalExecution> getExecutionsByEvalRule(Long evalRuleId);

    List<AiObservabilityEvalExecution> getExecutionsByTrace(Long traceId);

    List<AiObservabilityEvalExecution> getPendingExecutions();

    AiObservabilityEvalExecution update(AiObservabilityEvalExecution evalExecution);
}
```

- [ ] **Step 5: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityScoreService.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityScoreConfigService.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityEvalRuleService.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityEvalExecutionService.java
git commit -m "732 Add evaluation and scoring service interfaces"
```

---

## Task 9: Service Implementations

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityScoreServiceImpl.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityScoreConfigServiceImpl.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityEvalRuleServiceImpl.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityEvalExecutionServiceImpl.java`

- [ ] **Step 1: Create AiObservabilityScoreServiceImpl**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityScore;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilityScoreRepository;
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
class AiObservabilityScoreServiceImpl implements AiObservabilityScoreService {

    private final AiObservabilityScoreRepository aiObservabilityScoreRepository;

    public AiObservabilityScoreServiceImpl(
        AiObservabilityScoreRepository aiObservabilityScoreRepository) {

        this.aiObservabilityScoreRepository = aiObservabilityScoreRepository;
    }

    @Override
    public AiObservabilityScore create(AiObservabilityScore score) {
        Validate.notNull(score, "score must not be null");
        Validate.isTrue(score.getId() == null, "score id must be null for creation");

        return aiObservabilityScoreRepository.save(score);
    }

    @Override
    public void delete(long id) {
        aiObservabilityScoreRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiObservabilityScore getScore(long id) {
        return aiObservabilityScoreRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiObservabilityScore not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityScore> getScoresByTrace(Long traceId) {
        return aiObservabilityScoreRepository.findAllByTraceId(traceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityScore> getScoresByWorkspace(Long workspaceId) {
        return aiObservabilityScoreRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityScore> getScoresByWorkspaceAndName(Long workspaceId, String name) {
        return aiObservabilityScoreRepository.findAllByWorkspaceIdAndName(workspaceId, name);
    }
}
```

- [ ] **Step 2: Create AiObservabilityScoreConfigServiceImpl**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityScoreConfig;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilityScoreConfigRepository;
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
class AiObservabilityScoreConfigServiceImpl implements AiObservabilityScoreConfigService {

    private final AiObservabilityScoreConfigRepository aiObservabilityScoreConfigRepository;

    public AiObservabilityScoreConfigServiceImpl(
        AiObservabilityScoreConfigRepository aiObservabilityScoreConfigRepository) {

        this.aiObservabilityScoreConfigRepository = aiObservabilityScoreConfigRepository;
    }

    @Override
    public AiObservabilityScoreConfig create(AiObservabilityScoreConfig scoreConfig) {
        Validate.notNull(scoreConfig, "scoreConfig must not be null");
        Validate.isTrue(scoreConfig.getId() == null, "scoreConfig id must be null for creation");

        return aiObservabilityScoreConfigRepository.save(scoreConfig);
    }

    @Override
    public void delete(long id) {
        aiObservabilityScoreConfigRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiObservabilityScoreConfig getScoreConfig(long id) {
        return aiObservabilityScoreConfigRepository.findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException("AiObservabilityScoreConfig not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityScoreConfig> getScoreConfigsByWorkspace(Long workspaceId) {
        return aiObservabilityScoreConfigRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    public AiObservabilityScoreConfig update(AiObservabilityScoreConfig scoreConfig) {
        Validate.notNull(scoreConfig, "scoreConfig must not be null");
        Validate.notNull(scoreConfig.getId(), "scoreConfig id must not be null for update");

        return aiObservabilityScoreConfigRepository.save(scoreConfig);
    }
}
```

- [ ] **Step 3: Create AiObservabilityEvalRuleServiceImpl**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityEvalRule;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilityEvalRuleRepository;
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
class AiObservabilityEvalRuleServiceImpl implements AiObservabilityEvalRuleService {

    private final AiObservabilityEvalRuleRepository aiObservabilityEvalRuleRepository;

    public AiObservabilityEvalRuleServiceImpl(
        AiObservabilityEvalRuleRepository aiObservabilityEvalRuleRepository) {

        this.aiObservabilityEvalRuleRepository = aiObservabilityEvalRuleRepository;
    }

    @Override
    public AiObservabilityEvalRule create(AiObservabilityEvalRule evalRule) {
        Validate.notNull(evalRule, "evalRule must not be null");
        Validate.isTrue(evalRule.getId() == null, "evalRule id must be null for creation");

        return aiObservabilityEvalRuleRepository.save(evalRule);
    }

    @Override
    public void delete(long id) {
        aiObservabilityEvalRuleRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiObservabilityEvalRule getEvalRule(long id) {
        return aiObservabilityEvalRuleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiObservabilityEvalRule not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityEvalRule> getEvalRulesByWorkspace(Long workspaceId) {
        return aiObservabilityEvalRuleRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityEvalRule> getEnabledEvalRulesByWorkspace(Long workspaceId) {
        return aiObservabilityEvalRuleRepository.findAllByWorkspaceIdAndEnabled(workspaceId, true);
    }

    @Override
    public AiObservabilityEvalRule update(AiObservabilityEvalRule evalRule) {
        Validate.notNull(evalRule, "evalRule must not be null");
        Validate.notNull(evalRule.getId(), "evalRule id must not be null for update");

        return aiObservabilityEvalRuleRepository.save(evalRule);
    }
}
```

- [ ] **Step 4: Create AiObservabilityEvalExecutionServiceImpl**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityEvalExecution;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityEvalExecutionStatus;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilityEvalExecutionRepository;
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
class AiObservabilityEvalExecutionServiceImpl implements AiObservabilityEvalExecutionService {

    private final AiObservabilityEvalExecutionRepository aiObservabilityEvalExecutionRepository;

    public AiObservabilityEvalExecutionServiceImpl(
        AiObservabilityEvalExecutionRepository aiObservabilityEvalExecutionRepository) {

        this.aiObservabilityEvalExecutionRepository = aiObservabilityEvalExecutionRepository;
    }

    @Override
    public AiObservabilityEvalExecution create(AiObservabilityEvalExecution evalExecution) {
        Validate.notNull(evalExecution, "evalExecution must not be null");
        Validate.isTrue(evalExecution.getId() == null, "evalExecution id must be null for creation");

        return aiObservabilityEvalExecutionRepository.save(evalExecution);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityEvalExecution> getExecutionsByEvalRule(Long evalRuleId) {
        return aiObservabilityEvalExecutionRepository.findAllByEvalRuleId(evalRuleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityEvalExecution> getExecutionsByTrace(Long traceId) {
        return aiObservabilityEvalExecutionRepository.findAllByTraceId(traceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityEvalExecution> getPendingExecutions() {
        return aiObservabilityEvalExecutionRepository.findAllByStatus(
            AiObservabilityEvalExecutionStatus.PENDING.ordinal());
    }

    @Override
    public AiObservabilityEvalExecution update(AiObservabilityEvalExecution evalExecution) {
        Validate.notNull(evalExecution, "evalExecution must not be null");
        Validate.notNull(evalExecution.getId(), "evalExecution id must not be null for update");

        return aiObservabilityEvalExecutionRepository.save(evalExecution);
    }
}
```

- [ ] **Step 5: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityScoreServiceImpl.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityScoreConfigServiceImpl.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityEvalRuleServiceImpl.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiObservabilityEvalExecutionServiceImpl.java
git commit -m "732 Add evaluation and scoring service implementations"
```

---

## Task 10: LLM-as-Judge Evaluation Executor

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/evaluation/AiObservabilityEvalExecutor.java`

- [ ] **Step 1: Create AiObservabilityEvalExecutor**

This component runs LLM-as-judge evaluations. It is invoked when a trace completes and processes matching eval rules.

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.evaluation;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityEvalExecution;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityEvalExecutionStatus;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityEvalRule;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityScore;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityScoreConfig;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityScoreDataType;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityScoreSource;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.provider.AiGatewayChatModelFactory;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityEvalExecutionService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityEvalRuleService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityScoreConfigService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityScoreService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityTraceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Executes LLM-as-judge evaluations for completed traces. For each enabled eval rule that matches the trace and passes
 * sampling, builds a prompt from the rule's template, calls the specified model via the gateway's own
 * {@link AiGatewayChatModelFactory}, parses the response as a score, and persists the result.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
public class AiObservabilityEvalExecutor {

    private static final Logger logger = LoggerFactory.getLogger(AiObservabilityEvalExecutor.class);

    private final AiObservabilityEvalExecutionService aiObservabilityEvalExecutionService;
    private final AiObservabilityEvalRuleService aiObservabilityEvalRuleService;
    private final AiObservabilityScoreConfigService aiObservabilityScoreConfigService;
    private final AiObservabilityScoreService aiObservabilityScoreService;
    private final AiObservabilityTraceService aiObservabilityTraceService;

    public AiObservabilityEvalExecutor(
        AiObservabilityEvalExecutionService aiObservabilityEvalExecutionService,
        AiObservabilityEvalRuleService aiObservabilityEvalRuleService,
        AiObservabilityScoreConfigService aiObservabilityScoreConfigService,
        AiObservabilityScoreService aiObservabilityScoreService,
        AiObservabilityTraceService aiObservabilityTraceService) {

        this.aiObservabilityEvalExecutionService = aiObservabilityEvalExecutionService;
        this.aiObservabilityEvalRuleService = aiObservabilityEvalRuleService;
        this.aiObservabilityScoreConfigService = aiObservabilityScoreConfigService;
        this.aiObservabilityScoreService = aiObservabilityScoreService;
        this.aiObservabilityTraceService = aiObservabilityTraceService;
    }

    /**
     * Evaluate a completed trace against all enabled eval rules for the workspace. Called asynchronously after a trace
     * transitions to COMPLETED status.
     */
    @Async
    public void evaluateTrace(long traceId, Long workspaceId, ChatModel chatModel) {
        AiObservabilityTrace trace = aiObservabilityTraceService.getTrace(traceId);

        List<AiObservabilityEvalRule> enabledRules =
            aiObservabilityEvalRuleService.getEnabledEvalRulesByWorkspace(workspaceId);

        for (AiObservabilityEvalRule evalRule : enabledRules) {
            if (!passesSampling(evalRule)) {
                continue;
            }

            AiObservabilityEvalExecution evalExecution = new AiObservabilityEvalExecution(
                evalRule.getId(), traceId);

            evalExecution = aiObservabilityEvalExecutionService.create(evalExecution);

            if (evalRule.getDelaySeconds() != null && evalRule.getDelaySeconds() > 0) {
                try {
                    Thread.sleep(evalRule.getDelaySeconds() * 1000L);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();

                    return;
                }
            }

            executeEvaluation(evalExecution, evalRule, trace, chatModel);
        }
    }

    private void executeEvaluation(
        AiObservabilityEvalExecution evalExecution, AiObservabilityEvalRule evalRule,
        AiObservabilityTrace trace, ChatModel chatModel) {

        try {
            String promptText = buildPrompt(evalRule.getPromptTemplate(), trace);

            ChatResponse chatResponse = chatModel.call(new Prompt(promptText));

            String responseContent = chatResponse.getResult().getOutput().getText();

            AiObservabilityScoreConfig scoreConfig =
                aiObservabilityScoreConfigService.getScoreConfig(evalRule.getScoreConfigId());

            AiObservabilityScore score = new AiObservabilityScore(
                trace.getWorkspaceId(), trace.getId(), scoreConfig.getName(),
                scoreConfig.getDataType() != null ? scoreConfig.getDataType() : AiObservabilityScoreDataType.NUMERIC,
                AiObservabilityScoreSource.LLM_JUDGE);

            score.setEvalRuleId(evalRule.getId());
            score.setCreatedBy("system");

            parseAndSetScoreValue(score, responseContent, scoreConfig);

            AiObservabilityScore savedScore = aiObservabilityScoreService.create(score);

            evalExecution.setStatus(AiObservabilityEvalExecutionStatus.COMPLETED);
            evalExecution.setScoreId(savedScore.getId());

            aiObservabilityEvalExecutionService.update(evalExecution);
        } catch (Exception exception) {
            logger.error("Evaluation failed for rule {} on trace {}", evalRule.getId(), trace.getId(), exception);

            evalExecution.setStatus(AiObservabilityEvalExecutionStatus.ERROR);
            evalExecution.setErrorMessage(exception.getMessage());

            aiObservabilityEvalExecutionService.update(evalExecution);
        }
    }

    private String buildPrompt(String promptTemplate, AiObservabilityTrace trace) {
        String result = promptTemplate;

        result = result.replace("{{input}}", trace.getInput() != null ? trace.getInput() : "");
        result = result.replace("{{output}}", trace.getOutput() != null ? trace.getOutput() : "");
        result = result.replace("{{metadata}}", trace.getMetadata() != null ? trace.getMetadata() : "");

        return result;
    }

    private boolean passesSampling(AiObservabilityEvalRule evalRule) {
        BigDecimal samplingRate = evalRule.getSamplingRate();

        if (samplingRate.compareTo(BigDecimal.ONE) >= 0) {
            return true;
        }

        if (samplingRate.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        return ThreadLocalRandom.current().nextDouble() < samplingRate.doubleValue();
    }

    private void parseAndSetScoreValue(
        AiObservabilityScore score, String responseContent, AiObservabilityScoreConfig scoreConfig) {

        String trimmedResponse = responseContent.trim();

        AiObservabilityScoreDataType dataType =
            scoreConfig.getDataType() != null ? scoreConfig.getDataType() : AiObservabilityScoreDataType.NUMERIC;

        switch (dataType) {
            case NUMERIC -> {
                try {
                    score.setValue(new BigDecimal(trimmedResponse));
                } catch (NumberFormatException numberFormatException) {
                    score.setValue(BigDecimal.ZERO);
                    score.setComment("Failed to parse numeric score from LLM response: " + trimmedResponse);
                }
            }

            case BOOLEAN -> {
                String lowerResponse = trimmedResponse.toLowerCase();
                boolean booleanValue =
                    lowerResponse.equals("true") || lowerResponse.equals("yes") || lowerResponse.equals("1");

                score.setValue(booleanValue ? BigDecimal.ONE : BigDecimal.ZERO);
                score.setStringValue(String.valueOf(booleanValue));
            }

            case CATEGORICAL -> {
                score.setStringValue(trimmedResponse);
            }
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/evaluation/AiObservabilityEvalExecutor.java
git commit -m "732 Add LLM-as-judge evaluation executor with sampling and delay support"
```

---

## Task 11: Facade Integration — Trigger Eval on Trace Completion

**Files:**
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/facade/AiGatewayFacade.java`

- [ ] **Step 1: Add evaluation executor to AiGatewayFacade**

Add new fields and constructor parameter:

```java
// Add import
import com.bytechef.ee.automation.ai.gateway.evaluation.AiObservabilityEvalExecutor;

// Add field
private final AiObservabilityEvalExecutor aiObservabilityEvalExecutor;
```

Add the field to the constructor and inject it.

- [ ] **Step 2: Trigger evaluation after trace completion**

In the `processTracingHeaders` method (or the place where trace status is set to COMPLETED), add a call to the eval executor after the trace is saved:

```java
// After saving the completed trace and span:
if (savedTrace.getStatus() == AiObservabilityTraceStatus.COMPLETED) {
    aiObservabilityEvalExecutor.evaluateTrace(
        savedTrace.getId(), workspaceId, chatModel);
}
```

The `chatModel` parameter comes from the `AiGatewayChatModelFactory` that is already available in the facade for routing requests. Pass the resolved `ChatModel` to the eval executor so it can call the LLM for judge evaluations.

Note: The exact integration point depends on the current facade code. The eval executor call must happen after the trace is persisted with COMPLETED status. The `@Async` annotation on `evaluateTrace` ensures this runs in a separate thread and does not block the request response.

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/facade/AiGatewayFacade.java
git commit -m "732 Trigger LLM-as-judge evaluation on trace completion in AiGatewayFacade"
```

---

## Task 12: Public REST Endpoint — Programmatic Scoring

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-public-rest/src/main/java/com/bytechef/ee/automation/ai/gateway/public_/web/rest/AiGatewayScoreApiController.java`

- [ ] **Step 1: Create AiGatewayScoreApiController**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.public_.web.rest;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityScore;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityScoreDataType;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityScoreSource;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityScoreService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityTraceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 */
@RestController
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@RequestMapping("/api/ai-gateway/v1")
@SuppressFBWarnings("EI")
class AiGatewayScoreApiController {

    private final AiObservabilityScoreService aiObservabilityScoreService;
    private final AiObservabilityTraceService aiObservabilityTraceService;

    AiGatewayScoreApiController(
        AiObservabilityScoreService aiObservabilityScoreService,
        AiObservabilityTraceService aiObservabilityTraceService) {

        this.aiObservabilityScoreService = aiObservabilityScoreService;
        this.aiObservabilityTraceService = aiObservabilityTraceService;
    }

    @PostMapping("/scores")
    public ResponseEntity<ScoreResponseModel> createScore(@RequestBody ScoreRequestModel scoreRequestModel) {
        AiObservabilityTrace trace = aiObservabilityTraceService.getTrace(scoreRequestModel.traceId());

        AiObservabilityScoreDataType dataType = AiObservabilityScoreDataType.valueOf(
            scoreRequestModel.dataType().toUpperCase());

        AiObservabilityScore score = new AiObservabilityScore(
            trace.getWorkspaceId(), scoreRequestModel.traceId(), scoreRequestModel.name(),
            dataType, AiObservabilityScoreSource.API);

        if (scoreRequestModel.spanId() != null) {
            score.setSpanId(scoreRequestModel.spanId());
        }

        if (scoreRequestModel.value() != null) {
            score.setValue(scoreRequestModel.value());
        }

        if (scoreRequestModel.stringValue() != null) {
            score.setStringValue(scoreRequestModel.stringValue());
        }

        if (scoreRequestModel.comment() != null) {
            score.setComment(scoreRequestModel.comment());
        }

        AiObservabilityScore savedScore = aiObservabilityScoreService.create(score);

        return ResponseEntity.ok(new ScoreResponseModel(savedScore.getId(), savedScore.getName()));
    }

    record ScoreRequestModel(
        String comment,
        String dataType,
        String name,
        Long spanId,
        String stringValue,
        Long traceId,
        BigDecimal value) {
    }

    record ScoreResponseModel(Long id, String name) {
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-public-rest:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-public-rest/src/main/java/com/bytechef/ee/automation/ai/gateway/public_/web/rest/AiGatewayScoreApiController.java
git commit -m "732 Add public REST endpoint POST /api/ai-gateway/v1/scores for programmatic scoring"
```

---

## Task 13: GraphQL Schema — Scores, ScoreConfigs, EvalRules, EvalExecutions

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-score.graphqls`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-score-config.graphqls`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-eval-rule.graphqls`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-eval-execution.graphqls`

- [ ] **Step 1: Create ai-observability-score.graphqls**

```graphql
extend type Query {
    aiObservabilityScores(workspaceId: ID!): [AiObservabilityScore]
    aiObservabilityScoresByTrace(traceId: ID!): [AiObservabilityScore]
}

extend type Mutation {
    createAiObservabilityScore(
        comment: String,
        dataType: AiObservabilityScoreDataType!,
        name: String!,
        source: AiObservabilityScoreSource!,
        spanId: ID,
        stringValue: String,
        traceId: ID!,
        value: Float,
        workspaceId: ID!
    ): AiObservabilityScore

    deleteAiObservabilityScore(id: ID!): Boolean
}

type AiObservabilityScore {
    comment: String
    createdBy: String
    createdDate: Long
    dataType: AiObservabilityScoreDataType
    evalRuleId: ID
    id: ID!
    name: String!
    source: AiObservabilityScoreSource!
    spanId: ID
    stringValue: String
    traceId: ID!
    value: Float
    workspaceId: ID!
}

enum AiObservabilityScoreDataType {
    BOOLEAN
    CATEGORICAL
    NUMERIC
}

enum AiObservabilityScoreSource {
    API
    LLM_JUDGE
    MANUAL
}
```

- [ ] **Step 2: Create ai-observability-score-config.graphqls**

```graphql
extend type Query {
    aiObservabilityScoreConfig(id: ID!): AiObservabilityScoreConfig
    aiObservabilityScoreConfigs(workspaceId: ID!): [AiObservabilityScoreConfig]
}

extend type Mutation {
    createAiObservabilityScoreConfig(
        categories: String,
        dataType: AiObservabilityScoreDataType,
        description: String,
        maxValue: Float,
        minValue: Float,
        name: String!,
        workspaceId: ID!
    ): AiObservabilityScoreConfig

    deleteAiObservabilityScoreConfig(id: ID!): Boolean

    updateAiObservabilityScoreConfig(
        categories: String,
        dataType: AiObservabilityScoreDataType,
        description: String,
        id: ID!,
        maxValue: Float,
        minValue: Float,
        name: String!
    ): AiObservabilityScoreConfig
}

type AiObservabilityScoreConfig {
    categories: String
    createdDate: Long
    dataType: AiObservabilityScoreDataType
    description: String
    id: ID!
    lastModifiedDate: Long
    maxValue: Float
    minValue: Float
    name: String!
    version: Int
    workspaceId: ID!
}
```

- [ ] **Step 3: Create ai-observability-eval-rule.graphqls**

```graphql
extend type Query {
    aiObservabilityEvalRule(id: ID!): AiObservabilityEvalRule
    aiObservabilityEvalRules(workspaceId: ID!): [AiObservabilityEvalRule]
}

extend type Mutation {
    createAiObservabilityEvalRule(
        delaySeconds: Int,
        enabled: Boolean!,
        filters: String,
        model: String!,
        name: String!,
        projectId: ID,
        promptTemplate: String!,
        samplingRate: Float!,
        scoreConfigId: ID!,
        workspaceId: ID!
    ): AiObservabilityEvalRule

    deleteAiObservabilityEvalRule(id: ID!): Boolean

    updateAiObservabilityEvalRule(
        delaySeconds: Int,
        enabled: Boolean!,
        filters: String,
        id: ID!,
        model: String!,
        name: String!,
        promptTemplate: String!,
        samplingRate: Float!,
        scoreConfigId: ID!
    ): AiObservabilityEvalRule
}

type AiObservabilityEvalRule {
    createdDate: Long
    delaySeconds: Int
    enabled: Boolean!
    filters: String
    id: ID!
    lastModifiedDate: Long
    model: String!
    name: String!
    projectId: ID
    promptTemplate: String!
    samplingRate: Float!
    scoreConfigId: ID!
    version: Int
    workspaceId: ID!
}
```

- [ ] **Step 4: Create ai-observability-eval-execution.graphqls**

```graphql
extend type Query {
    aiObservabilityEvalExecutions(evalRuleId: ID!): [AiObservabilityEvalExecution]
    aiObservabilityEvalExecutionsByTrace(traceId: ID!): [AiObservabilityEvalExecution]
}

type AiObservabilityEvalExecution {
    createdDate: Long
    errorMessage: String
    evalRuleId: ID!
    id: ID!
    scoreId: ID
    status: AiObservabilityEvalExecutionStatus!
    traceId: ID!
}

enum AiObservabilityEvalExecutionStatus {
    COMPLETED
    ERROR
    PENDING
}
```

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-score.graphqls \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-score-config.graphqls \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-eval-rule.graphqls \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-observability-eval-execution.graphqls
git commit -m "732 Add GraphQL schema for scores, score configs, eval rules, eval executions"
```

---

## Task 14: GraphQL Controllers

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiObservabilityScoreGraphQlController.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiObservabilityScoreConfigGraphQlController.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiObservabilityEvalRuleGraphQlController.java`

- [ ] **Step 1: Create AiObservabilityScoreGraphQlController**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityScore;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityScoreDataType;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityScoreSource;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityScoreService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
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
class AiObservabilityScoreGraphQlController {

    private final AiObservabilityScoreService aiObservabilityScoreService;

    @SuppressFBWarnings("EI")
    AiObservabilityScoreGraphQlController(AiObservabilityScoreService aiObservabilityScoreService) {
        this.aiObservabilityScoreService = aiObservabilityScoreService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiObservabilityScore> aiObservabilityScores(@Argument Long workspaceId) {
        return aiObservabilityScoreService.getScoresByWorkspace(workspaceId);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiObservabilityScore> aiObservabilityScoresByTrace(@Argument Long traceId) {
        return aiObservabilityScoreService.getScoresByTrace(traceId);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityScore createAiObservabilityScore(
        @Argument Long workspaceId, @Argument Long traceId, @Argument Long spanId,
        @Argument String name, @Argument Double value, @Argument String stringValue,
        @Argument AiObservabilityScoreDataType dataType, @Argument AiObservabilityScoreSource source,
        @Argument String comment) {

        AiObservabilityScore score = new AiObservabilityScore(
            workspaceId, traceId, name, dataType, source);

        if (spanId != null) {
            score.setSpanId(spanId);
        }

        if (value != null) {
            score.setValue(BigDecimal.valueOf(value));
        }

        if (stringValue != null) {
            score.setStringValue(stringValue);
        }

        if (comment != null) {
            score.setComment(comment);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            score.setCreatedBy(authentication.getName());
        }

        return aiObservabilityScoreService.create(score);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean deleteAiObservabilityScore(@Argument long id) {
        aiObservabilityScoreService.delete(id);

        return true;
    }
}
```

- [ ] **Step 2: Create AiObservabilityScoreConfigGraphQlController**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityScoreConfig;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityScoreDataType;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityScoreConfigService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
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
class AiObservabilityScoreConfigGraphQlController {

    private final AiObservabilityScoreConfigService aiObservabilityScoreConfigService;

    @SuppressFBWarnings("EI")
    AiObservabilityScoreConfigGraphQlController(
        AiObservabilityScoreConfigService aiObservabilityScoreConfigService) {

        this.aiObservabilityScoreConfigService = aiObservabilityScoreConfigService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityScoreConfig aiObservabilityScoreConfig(@Argument long id) {
        return aiObservabilityScoreConfigService.getScoreConfig(id);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiObservabilityScoreConfig> aiObservabilityScoreConfigs(@Argument Long workspaceId) {
        return aiObservabilityScoreConfigService.getScoreConfigsByWorkspace(workspaceId);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityScoreConfig createAiObservabilityScoreConfig(
        @Argument Long workspaceId, @Argument String name, @Argument AiObservabilityScoreDataType dataType,
        @Argument Double minValue, @Argument Double maxValue, @Argument String categories,
        @Argument String description) {

        AiObservabilityScoreConfig scoreConfig = new AiObservabilityScoreConfig(workspaceId, name);

        scoreConfig.setDataType(dataType);

        if (minValue != null) {
            scoreConfig.setMinValue(BigDecimal.valueOf(minValue));
        }

        if (maxValue != null) {
            scoreConfig.setMaxValue(BigDecimal.valueOf(maxValue));
        }

        scoreConfig.setCategories(categories);
        scoreConfig.setDescription(description);

        return aiObservabilityScoreConfigService.create(scoreConfig);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean deleteAiObservabilityScoreConfig(@Argument long id) {
        aiObservabilityScoreConfigService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityScoreConfig updateAiObservabilityScoreConfig(
        @Argument long id, @Argument String name, @Argument AiObservabilityScoreDataType dataType,
        @Argument Double minValue, @Argument Double maxValue, @Argument String categories,
        @Argument String description) {

        AiObservabilityScoreConfig scoreConfig = aiObservabilityScoreConfigService.getScoreConfig(id);

        scoreConfig.setName(name);
        scoreConfig.setDataType(dataType);
        scoreConfig.setMinValue(minValue != null ? BigDecimal.valueOf(minValue) : null);
        scoreConfig.setMaxValue(maxValue != null ? BigDecimal.valueOf(maxValue) : null);
        scoreConfig.setCategories(categories);
        scoreConfig.setDescription(description);

        return aiObservabilityScoreConfigService.update(scoreConfig);
    }
}
```

- [ ] **Step 3: Create AiObservabilityEvalRuleGraphQlController**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityEvalExecution;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityEvalRule;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityEvalExecutionService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityEvalRuleService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
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
class AiObservabilityEvalRuleGraphQlController {

    private final AiObservabilityEvalExecutionService aiObservabilityEvalExecutionService;
    private final AiObservabilityEvalRuleService aiObservabilityEvalRuleService;

    @SuppressFBWarnings("EI")
    AiObservabilityEvalRuleGraphQlController(
        AiObservabilityEvalExecutionService aiObservabilityEvalExecutionService,
        AiObservabilityEvalRuleService aiObservabilityEvalRuleService) {

        this.aiObservabilityEvalExecutionService = aiObservabilityEvalExecutionService;
        this.aiObservabilityEvalRuleService = aiObservabilityEvalRuleService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityEvalRule aiObservabilityEvalRule(@Argument long id) {
        return aiObservabilityEvalRuleService.getEvalRule(id);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiObservabilityEvalRule> aiObservabilityEvalRules(@Argument Long workspaceId) {
        return aiObservabilityEvalRuleService.getEvalRulesByWorkspace(workspaceId);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiObservabilityEvalExecution> aiObservabilityEvalExecutions(@Argument Long evalRuleId) {
        return aiObservabilityEvalExecutionService.getExecutionsByEvalRule(evalRuleId);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiObservabilityEvalExecution> aiObservabilityEvalExecutionsByTrace(@Argument Long traceId) {
        return aiObservabilityEvalExecutionService.getExecutionsByTrace(traceId);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityEvalRule createAiObservabilityEvalRule(
        @Argument Long workspaceId, @Argument String name, @Argument Long scoreConfigId,
        @Argument String promptTemplate, @Argument String model, @Argument Double samplingRate,
        @Argument String filters, @Argument Integer delaySeconds, @Argument boolean enabled,
        @Argument Long projectId) {

        AiObservabilityEvalRule evalRule = new AiObservabilityEvalRule(
            workspaceId, name, scoreConfigId, promptTemplate, model,
            BigDecimal.valueOf(samplingRate));

        evalRule.setEnabled(enabled);
        evalRule.setFilters(filters);
        evalRule.setDelaySeconds(delaySeconds);
        evalRule.setProjectId(projectId);

        return aiObservabilityEvalRuleService.create(evalRule);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean deleteAiObservabilityEvalRule(@Argument long id) {
        aiObservabilityEvalRuleService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityEvalRule updateAiObservabilityEvalRule(
        @Argument long id, @Argument String name, @Argument Long scoreConfigId,
        @Argument String promptTemplate, @Argument String model, @Argument Double samplingRate,
        @Argument String filters, @Argument Integer delaySeconds, @Argument boolean enabled) {

        AiObservabilityEvalRule evalRule = aiObservabilityEvalRuleService.getEvalRule(id);

        evalRule.setName(name);
        evalRule.setScoreConfigId(scoreConfigId);
        evalRule.setPromptTemplate(promptTemplate);
        evalRule.setModel(model);
        evalRule.setSamplingRate(BigDecimal.valueOf(samplingRate));
        evalRule.setFilters(filters);
        evalRule.setDelaySeconds(delaySeconds);
        evalRule.setEnabled(enabled);

        return aiObservabilityEvalRuleService.update(evalRule);
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-graphql:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiObservabilityScoreGraphQlController.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiObservabilityScoreConfigGraphQlController.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiObservabilityEvalRuleGraphQlController.java
git commit -m "732 Add GraphQL controllers for scores, score configs, and eval rules"
```

---

## Task 15: Client — GraphQL Operations and Codegen

**Files:**
- Create: `client/src/graphql/automation/ai-gateway/aiObservabilityScores.graphql`
- Create: `client/src/graphql/automation/ai-gateway/aiObservabilityScoreConfigs.graphql`
- Create: `client/src/graphql/automation/ai-gateway/aiObservabilityEvalRules.graphql`

- [ ] **Step 1: Create aiObservabilityScores.graphql**

```graphql
query aiObservabilityScores($workspaceId: ID!) {
    aiObservabilityScores(workspaceId: $workspaceId) {
        comment
        createdBy
        createdDate
        dataType
        evalRuleId
        id
        name
        source
        spanId
        stringValue
        traceId
        value
        workspaceId
    }
}

query aiObservabilityScoresByTrace($traceId: ID!) {
    aiObservabilityScoresByTrace(traceId: $traceId) {
        comment
        createdBy
        createdDate
        dataType
        evalRuleId
        id
        name
        source
        spanId
        stringValue
        traceId
        value
        workspaceId
    }
}

mutation createAiObservabilityScore(
    $comment: String,
    $dataType: AiObservabilityScoreDataType!,
    $name: String!,
    $source: AiObservabilityScoreSource!,
    $spanId: ID,
    $stringValue: String,
    $traceId: ID!,
    $value: Float,
    $workspaceId: ID!
) {
    createAiObservabilityScore(
        comment: $comment,
        dataType: $dataType,
        name: $name,
        source: $source,
        spanId: $spanId,
        stringValue: $stringValue,
        traceId: $traceId,
        value: $value,
        workspaceId: $workspaceId
    ) {
        id
        name
    }
}

mutation deleteAiObservabilityScore($id: ID!) {
    deleteAiObservabilityScore(id: $id)
}
```

- [ ] **Step 2: Create aiObservabilityScoreConfigs.graphql**

```graphql
query aiObservabilityScoreConfigs($workspaceId: ID!) {
    aiObservabilityScoreConfigs(workspaceId: $workspaceId) {
        categories
        createdDate
        dataType
        description
        id
        lastModifiedDate
        maxValue
        minValue
        name
        version
        workspaceId
    }
}

query aiObservabilityScoreConfig($id: ID!) {
    aiObservabilityScoreConfig(id: $id) {
        categories
        createdDate
        dataType
        description
        id
        lastModifiedDate
        maxValue
        minValue
        name
        version
        workspaceId
    }
}

mutation createAiObservabilityScoreConfig(
    $categories: String,
    $dataType: AiObservabilityScoreDataType,
    $description: String,
    $maxValue: Float,
    $minValue: Float,
    $name: String!,
    $workspaceId: ID!
) {
    createAiObservabilityScoreConfig(
        categories: $categories,
        dataType: $dataType,
        description: $description,
        maxValue: $maxValue,
        minValue: $minValue,
        name: $name,
        workspaceId: $workspaceId
    ) {
        id
        name
    }
}

mutation deleteAiObservabilityScoreConfig($id: ID!) {
    deleteAiObservabilityScoreConfig(id: $id)
}

mutation updateAiObservabilityScoreConfig(
    $categories: String,
    $dataType: AiObservabilityScoreDataType,
    $description: String,
    $id: ID!,
    $maxValue: Float,
    $minValue: Float,
    $name: String!
) {
    updateAiObservabilityScoreConfig(
        categories: $categories,
        dataType: $dataType,
        description: $description,
        id: $id,
        maxValue: $maxValue,
        minValue: $minValue,
        name: $name
    ) {
        id
        name
    }
}
```

- [ ] **Step 3: Create aiObservabilityEvalRules.graphql**

```graphql
query aiObservabilityEvalRules($workspaceId: ID!) {
    aiObservabilityEvalRules(workspaceId: $workspaceId) {
        createdDate
        delaySeconds
        enabled
        filters
        id
        lastModifiedDate
        model
        name
        projectId
        promptTemplate
        samplingRate
        scoreConfigId
        version
        workspaceId
    }
}

query aiObservabilityEvalRule($id: ID!) {
    aiObservabilityEvalRule(id: $id) {
        createdDate
        delaySeconds
        enabled
        filters
        id
        lastModifiedDate
        model
        name
        projectId
        promptTemplate
        samplingRate
        scoreConfigId
        version
        workspaceId
    }
}

query aiObservabilityEvalExecutions($evalRuleId: ID!) {
    aiObservabilityEvalExecutions(evalRuleId: $evalRuleId) {
        createdDate
        errorMessage
        evalRuleId
        id
        scoreId
        status
        traceId
    }
}

mutation createAiObservabilityEvalRule(
    $delaySeconds: Int,
    $enabled: Boolean!,
    $filters: String,
    $model: String!,
    $name: String!,
    $projectId: ID,
    $promptTemplate: String!,
    $samplingRate: Float!,
    $scoreConfigId: ID!,
    $workspaceId: ID!
) {
    createAiObservabilityEvalRule(
        delaySeconds: $delaySeconds,
        enabled: $enabled,
        filters: $filters,
        model: $model,
        name: $name,
        projectId: $projectId,
        promptTemplate: $promptTemplate,
        samplingRate: $samplingRate,
        scoreConfigId: $scoreConfigId,
        workspaceId: $workspaceId
    ) {
        id
        name
    }
}

mutation deleteAiObservabilityEvalRule($id: ID!) {
    deleteAiObservabilityEvalRule(id: $id)
}

mutation updateAiObservabilityEvalRule(
    $delaySeconds: Int,
    $enabled: Boolean!,
    $filters: String,
    $id: ID!,
    $model: String!,
    $name: String!,
    $promptTemplate: String!,
    $samplingRate: Float!,
    $scoreConfigId: ID!
) {
    updateAiObservabilityEvalRule(
        delaySeconds: $delaySeconds,
        enabled: $enabled,
        filters: $filters,
        id: $id,
        model: $model,
        name: $name,
        promptTemplate: $promptTemplate,
        samplingRate: $samplingRate,
        scoreConfigId: $scoreConfigId
    ) {
        id
        name
    }
}
```

- [ ] **Step 4: Run GraphQL codegen**

Run: `cd client && npx graphql-codegen`
Expected: generates updated `src/shared/middleware/graphql.ts` with new query/mutation hooks

- [ ] **Step 5: Commit**

```bash
cd client
git add src/graphql/automation/ai-gateway/aiObservabilityScores.graphql \
  src/graphql/automation/ai-gateway/aiObservabilityScoreConfigs.graphql \
  src/graphql/automation/ai-gateway/aiObservabilityEvalRules.graphql \
  src/shared/middleware/graphql.ts
git commit -m "732 client - Add GraphQL operations and codegen for evaluation and scoring"
```

---

## Task 16: Client — Types and Sidebar Update

**Files:**
- Modify: `client/src/pages/automation/ai-gateway/types.ts`
- Modify: `client/src/pages/automation/ai-gateway/AiGateway.tsx`

- [ ] **Step 1: Add types to types.ts**

Add to `client/src/pages/automation/ai-gateway/types.ts`:

```typescript
import {
    AiObservabilityEvalRulesQuery,
    AiObservabilityScoreConfigsQuery,
    AiObservabilityScoresByTraceQuery,
    AiObservabilityScoresQuery,
} from '@/shared/middleware/graphql';

// ... existing types unchanged ...

export type AiObservabilityEvalRuleType = NonNullable<
    NonNullable<AiObservabilityEvalRulesQuery['aiObservabilityEvalRules']>[number]
>;

export type AiObservabilityScoreConfigType = NonNullable<
    NonNullable<AiObservabilityScoreConfigsQuery['aiObservabilityScoreConfigs']>[number]
>;

export type AiObservabilityScoreType = NonNullable<
    NonNullable<AiObservabilityScoresQuery['aiObservabilityScores']>[number]
>;

export type AiObservabilityTraceScoreType = NonNullable<
    NonNullable<AiObservabilityScoresByTraceQuery['aiObservabilityScoresByTrace']>[number]
>;
```

Note: Add these type aliases alongside existing ones. Ensure the import block is sorted alphabetically per existing conventions.

- [ ] **Step 2: Update AiGateway.tsx sidebar**

In `client/src/pages/automation/ai-gateway/AiGateway.tsx`:

Update the type union to add `'scores'`:
```typescript
type AiGatewayPageType = 'budget' | 'models' | 'monitoring' | 'projects' | 'providers' | 'routing' | 'scores' | 'sessions' | 'settings' | 'traces';
```

Add import for new component:
```typescript
import AiObservabilityScores from './components/scores/AiObservabilityScores';
```

Add a `LeftSidebarNavItem` entry after the "Sessions" item:
```typescript
<LeftSidebarNavItem
    item={{
        current: activePage === 'scores',
        name: 'Scores',
        onItemClick: () => setActivePage('scores'),
    }}
/>
```

Add conditional render:
```typescript
{activePage === 'scores' && <AiObservabilityScores />}
```

- [ ] **Step 3: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/types.ts src/pages/automation/ai-gateway/AiGateway.tsx
git commit -m "732 client - Add Scores sidebar tab to AI Gateway and evaluation types"
```

---

## Task 17: Client — Scores Tab Root Component

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/scores/AiObservabilityScores.tsx`

- [ ] **Step 1: Create AiObservabilityScores.tsx**

This is the main Scores tab component with three sub-sections: Score Configs, Eval Rules, and Score Analytics.

```typescript
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {
    useAiObservabilityEvalRulesQuery,
    useAiObservabilityScoreConfigsQuery,
    useAiObservabilityScoresQuery,
} from '@/shared/middleware/graphql';
import {useWorkspaceStore} from '@/shared/stores/workspace.store';
import {PlusIcon, StarIcon} from 'lucide-react';
import {useMemo, useState} from 'react';

import {AiObservabilityScoreConfigType} from '../../types';
import AiObservabilityEvalRuleDialog from './AiObservabilityEvalRuleDialog';
import AiObservabilityEvalRules from './AiObservabilityEvalRules';
import AiObservabilityScoreConfigDialog from './AiObservabilityScoreConfigDialog';

type ScoresTabType = 'analytics' | 'configs' | 'rules';

const AiObservabilityScores = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [activeTab, setActiveTab] = useState<ScoresTabType>('configs');
    const [showConfigDialog, setShowConfigDialog] = useState(false);
    const [showEvalRuleDialog, setShowEvalRuleDialog] = useState(false);
    const [editingConfig, setEditingConfig] = useState<AiObservabilityScoreConfigType | undefined>();

    const {data: scoreConfigsData, isLoading: scoreConfigsIsLoading} = useAiObservabilityScoreConfigsQuery({
        workspaceId: currentWorkspaceId + '',
    });

    const {data: evalRulesData, isLoading: evalRulesIsLoading} = useAiObservabilityEvalRulesQuery({
        workspaceId: currentWorkspaceId + '',
    });

    const {data: scoresData, isLoading: scoresIsLoading} = useAiObservabilityScoresQuery({
        workspaceId: currentWorkspaceId + '',
    });

    const scoreConfigs = scoreConfigsData?.aiObservabilityScoreConfigs ?? [];
    const evalRules = evalRulesData?.aiObservabilityEvalRules ?? [];
    const scores = scoresData?.aiObservabilityScores ?? [];

    const scoreDistribution = useMemo(() => {
        const distributionMap = new Map<string, number>();

        for (const score of scores) {
            const count = distributionMap.get(score.name) || 0;

            distributionMap.set(score.name, count + 1);
        }

        return distributionMap;
    }, [scores]);

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <div className="mb-4 flex items-center justify-between">
                <h2 className="text-lg font-semibold">Scores</h2>

                <div className="flex gap-2">
                    {activeTab === 'configs' && (
                        <button
                            className="flex items-center gap-1 rounded-md bg-primary px-3 py-1.5 text-sm text-primary-foreground hover:bg-primary/90"
                            onClick={() => {
                                setEditingConfig(undefined);
                                setShowConfigDialog(true);
                            }}
                        >
                            <PlusIcon className="size-4" />
                            New Score Config
                        </button>
                    )}

                    {activeTab === 'rules' && (
                        <button
                            className="flex items-center gap-1 rounded-md bg-primary px-3 py-1.5 text-sm text-primary-foreground hover:bg-primary/90"
                            onClick={() => setShowEvalRuleDialog(true)}
                        >
                            <PlusIcon className="size-4" />
                            New Eval Rule
                        </button>
                    )}
                </div>
            </div>

            <div className="mb-4 flex gap-1 border-b">
                {(['configs', 'rules', 'analytics'] as ScoresTabType[]).map((tab) => (
                    <button
                        className={`px-4 py-2 text-sm font-medium ${
                            activeTab === tab
                                ? 'border-b-2 border-primary text-primary'
                                : 'text-muted-foreground hover:text-foreground'
                        }`}
                        key={tab}
                        onClick={() => setActiveTab(tab)}
                    >
                        {tab === 'configs' ? 'Score Configs' : tab === 'rules' ? 'Eval Rules' : 'Analytics'}
                    </button>
                ))}
            </div>

            {activeTab === 'configs' && (
                <>
                    {scoreConfigsIsLoading ? (
                        <PageLoader />
                    ) : scoreConfigs.length === 0 ? (
                        <EmptyList
                            icon={<StarIcon className="size-12 text-muted-foreground" />}
                            message="Define score dimensions like relevance, helpfulness, or safety."
                            title="No Score Configs"
                        />
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="w-full text-left text-sm">
                                <thead>
                                    <tr className="border-b text-muted-foreground">
                                        <th className="px-3 py-2 font-medium">Name</th>
                                        <th className="px-3 py-2 font-medium">Data Type</th>
                                        <th className="px-3 py-2 font-medium">Range</th>
                                        <th className="px-3 py-2 font-medium">Description</th>
                                    </tr>
                                </thead>

                                <tbody>
                                    {scoreConfigs.map((config) => (
                                        <tr
                                            className="cursor-pointer border-b hover:bg-muted/50"
                                            key={config.id}
                                            onClick={() => {
                                                setEditingConfig(config);
                                                setShowConfigDialog(true);
                                            }}
                                        >
                                            <td className="px-3 py-2 font-medium">{config.name}</td>
                                            <td className="px-3 py-2">{config.dataType || '-'}</td>
                                            <td className="px-3 py-2">
                                                {config.minValue != null && config.maxValue != null
                                                    ? `${config.minValue} - ${config.maxValue}`
                                                    : '-'}
                                            </td>
                                            <td className="px-3 py-2 text-muted-foreground">
                                                {config.description || '-'}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </>
            )}

            {activeTab === 'rules' && (
                <AiObservabilityEvalRules evalRules={evalRules} isLoading={evalRulesIsLoading} />
            )}

            {activeTab === 'analytics' && (
                <>
                    {scoresIsLoading ? (
                        <PageLoader />
                    ) : scores.length === 0 ? (
                        <EmptyList
                            icon={<StarIcon className="size-12 text-muted-foreground" />}
                            message="Scores will appear here once traces are evaluated."
                            title="No Scores Yet"
                        />
                    ) : (
                        <div className="space-y-4">
                            <h3 className="text-sm font-semibold">Score Distribution</h3>

                            <div className="grid grid-cols-2 gap-4 md:grid-cols-3">
                                {Array.from(scoreDistribution.entries()).map(([scoreName, count]) => (
                                    <div className="rounded-lg border p-4" key={scoreName}>
                                        <div className="text-sm font-medium">{scoreName}</div>

                                        <div className="mt-1 text-2xl font-bold">{count}</div>

                                        <div className="text-xs text-muted-foreground">scores recorded</div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}
                </>
            )}

            {showConfigDialog && (
                <AiObservabilityScoreConfigDialog
                    editingConfig={editingConfig}
                    onClose={() => {
                        setShowConfigDialog(false);
                        setEditingConfig(undefined);
                    }}
                />
            )}

            {showEvalRuleDialog && (
                <AiObservabilityEvalRuleDialog
                    onClose={() => setShowEvalRuleDialog(false)}
                    scoreConfigs={scoreConfigs}
                />
            )}
        </div>
    );
};

export default AiObservabilityScores;
```

- [ ] **Step 2: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/scores/AiObservabilityScores.tsx
git commit -m "732 client - Add Scores tab root component with configs, rules, and analytics sub-tabs"
```

---

## Task 18: Client — Score Config Dialog

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/scores/AiObservabilityScoreConfigDialog.tsx`

- [ ] **Step 1: Create AiObservabilityScoreConfigDialog.tsx**

```typescript
import {
    useCreateAiObservabilityScoreConfigMutation,
    useDeleteAiObservabilityScoreConfigMutation,
    useUpdateAiObservabilityScoreConfigMutation,
} from '@/shared/middleware/graphql';
import {useWorkspaceStore} from '@/shared/stores/workspace.store';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

import {AiObservabilityScoreConfigType} from '../../types';

interface AiObservabilityScoreConfigDialogProps {
    editingConfig?: AiObservabilityScoreConfigType;
    onClose: () => void;
}

const AiObservabilityScoreConfigDialog = ({editingConfig, onClose}: AiObservabilityScoreConfigDialogProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const queryClient = useQueryClient();

    const [categories, setCategories] = useState(editingConfig?.categories || '');
    const [dataType, setDataType] = useState(editingConfig?.dataType || 'NUMERIC');
    const [description, setDescription] = useState(editingConfig?.description || '');
    const [maxValue, setMaxValue] = useState(editingConfig?.maxValue?.toString() || '1');
    const [minValue, setMinValue] = useState(editingConfig?.minValue?.toString() || '0');
    const [name, setName] = useState(editingConfig?.name || '');

    const createMutation = useCreateAiObservabilityScoreConfigMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiObservabilityScoreConfigs']});
            onClose();
        },
    });

    const updateMutation = useUpdateAiObservabilityScoreConfigMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiObservabilityScoreConfigs']});
            onClose();
        },
    });

    const deleteMutation = useDeleteAiObservabilityScoreConfigMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiObservabilityScoreConfigs']});
            onClose();
        },
    });

    const handleSubmit = () => {
        if (editingConfig) {
            updateMutation.mutate({
                categories: dataType === 'CATEGORICAL' ? categories : undefined,
                dataType: dataType as 'BOOLEAN' | 'CATEGORICAL' | 'NUMERIC',
                description: description || undefined,
                id: editingConfig.id,
                maxValue: dataType === 'NUMERIC' ? parseFloat(maxValue) : undefined,
                minValue: dataType === 'NUMERIC' ? parseFloat(minValue) : undefined,
                name,
            });
        } else {
            createMutation.mutate({
                categories: dataType === 'CATEGORICAL' ? categories : undefined,
                dataType: dataType as 'BOOLEAN' | 'CATEGORICAL' | 'NUMERIC',
                description: description || undefined,
                maxValue: dataType === 'NUMERIC' ? parseFloat(maxValue) : undefined,
                minValue: dataType === 'NUMERIC' ? parseFloat(minValue) : undefined,
                name,
                workspaceId: currentWorkspaceId + '',
            });
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-md rounded-lg bg-background p-6 shadow-lg">
                <h3 className="mb-4 text-lg font-semibold">
                    {editingConfig ? 'Edit Score Config' : 'New Score Config'}
                </h3>

                <fieldset className="space-y-4 border-0">
                    <div>
                        <label className="mb-1 block text-sm font-medium" htmlFor="scoreConfigName">
                            Name
                        </label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            id="scoreConfigName"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="e.g., relevance, helpfulness, safety"
                            value={name}
                        />
                    </div>

                    <div>
                        <label className="mb-1 block text-sm font-medium" htmlFor="scoreConfigDataType">
                            Data Type
                        </label>

                        <select
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            id="scoreConfigDataType"
                            onChange={(event) => setDataType(event.target.value)}
                            value={dataType}
                        >
                            <option value="NUMERIC">Numeric</option>
                            <option value="BOOLEAN">Boolean</option>
                            <option value="CATEGORICAL">Categorical</option>
                        </select>
                    </div>

                    {dataType === 'NUMERIC' && (
                        <div className="flex gap-4">
                            <div className="flex-1">
                                <label className="mb-1 block text-sm font-medium" htmlFor="scoreConfigMinValue">
                                    Min Value
                                </label>

                                <input
                                    className="w-full rounded-md border px-3 py-2 text-sm"
                                    id="scoreConfigMinValue"
                                    onChange={(event) => setMinValue(event.target.value)}
                                    type="number"
                                    value={minValue}
                                />
                            </div>

                            <div className="flex-1">
                                <label className="mb-1 block text-sm font-medium" htmlFor="scoreConfigMaxValue">
                                    Max Value
                                </label>

                                <input
                                    className="w-full rounded-md border px-3 py-2 text-sm"
                                    id="scoreConfigMaxValue"
                                    onChange={(event) => setMaxValue(event.target.value)}
                                    type="number"
                                    value={maxValue}
                                />
                            </div>
                        </div>
                    )}

                    {dataType === 'CATEGORICAL' && (
                        <div>
                            <label className="mb-1 block text-sm font-medium" htmlFor="scoreConfigCategories">
                                Categories (JSON array)
                            </label>

                            <input
                                className="w-full rounded-md border px-3 py-2 text-sm"
                                id="scoreConfigCategories"
                                onChange={(event) => setCategories(event.target.value)}
                                placeholder='["good", "bad", "neutral"]'
                                value={categories}
                            />
                        </div>
                    )}

                    <div>
                        <label className="mb-1 block text-sm font-medium" htmlFor="scoreConfigDescription">
                            Description
                        </label>

                        <textarea
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            id="scoreConfigDescription"
                            onChange={(event) => setDescription(event.target.value)}
                            placeholder="What does this score measure?"
                            rows={2}
                            value={description}
                        />
                    </div>
                </fieldset>

                <div className="mt-6 flex justify-between">
                    <div>
                        {editingConfig && (
                            <button
                                className="rounded-md px-3 py-1.5 text-sm text-red-600 hover:bg-red-50"
                                onClick={() => deleteMutation.mutate({id: editingConfig.id})}
                            >
                                Delete
                            </button>
                        )}
                    </div>

                    <div className="flex gap-2">
                        <button
                            className="rounded-md px-3 py-1.5 text-sm text-muted-foreground hover:bg-muted"
                            onClick={onClose}
                        >
                            Cancel
                        </button>

                        <button
                            className="rounded-md bg-primary px-3 py-1.5 text-sm text-primary-foreground hover:bg-primary/90"
                            disabled={!name}
                            onClick={handleSubmit}
                        >
                            {editingConfig ? 'Update' : 'Create'}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AiObservabilityScoreConfigDialog;
```

- [ ] **Step 2: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/scores/AiObservabilityScoreConfigDialog.tsx
git commit -m "732 client - Add Score Config create/edit dialog"
```

---

## Task 19: Client — Eval Rules List and Dialog

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/scores/AiObservabilityEvalRules.tsx`
- Create: `client/src/pages/automation/ai-gateway/components/scores/AiObservabilityEvalRuleDialog.tsx`

- [ ] **Step 1: Create AiObservabilityEvalRules.tsx**

```typescript
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {BrainCircuitIcon} from 'lucide-react';

import {AiObservabilityEvalRuleType} from '../../types';

interface AiObservabilityEvalRulesProps {
    evalRules: AiObservabilityEvalRuleType[];
    isLoading: boolean;
}

const AiObservabilityEvalRules = ({evalRules, isLoading}: AiObservabilityEvalRulesProps) => {
    if (isLoading) {
        return <PageLoader />;
    }

    if (evalRules.length === 0) {
        return (
            <EmptyList
                icon={<BrainCircuitIcon className="size-12 text-muted-foreground" />}
                message="Create eval rules to automatically score traces using LLM-as-judge."
                title="No Eval Rules"
            />
        );
    }

    return (
        <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
                <thead>
                    <tr className="border-b text-muted-foreground">
                        <th className="px-3 py-2 font-medium">Name</th>
                        <th className="px-3 py-2 font-medium">Model</th>
                        <th className="px-3 py-2 font-medium">Sampling Rate</th>
                        <th className="px-3 py-2 font-medium">Delay</th>
                        <th className="px-3 py-2 font-medium">Status</th>
                    </tr>
                </thead>

                <tbody>
                    {evalRules.map((rule) => (
                        <tr className="border-b hover:bg-muted/50" key={rule.id}>
                            <td className="px-3 py-2 font-medium">{rule.name}</td>
                            <td className="px-3 py-2">{rule.model}</td>
                            <td className="px-3 py-2">{(Number(rule.samplingRate) * 100).toFixed(0)}%</td>
                            <td className="px-3 py-2">
                                {rule.delaySeconds != null ? `${rule.delaySeconds}s` : '-'}
                            </td>
                            <td className="px-3 py-2">
                                <span
                                    className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                                        rule.enabled
                                            ? 'bg-green-100 text-green-800'
                                            : 'bg-gray-100 text-gray-800'
                                    }`}
                                >
                                    {rule.enabled ? 'Enabled' : 'Disabled'}
                                </span>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};

export default AiObservabilityEvalRules;
```

- [ ] **Step 2: Create AiObservabilityEvalRuleDialog.tsx**

```typescript
import {useCreateAiObservabilityEvalRuleMutation} from '@/shared/middleware/graphql';
import {useWorkspaceStore} from '@/shared/stores/workspace.store';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

import {AiObservabilityScoreConfigType} from '../../types';

interface AiObservabilityEvalRuleDialogProps {
    onClose: () => void;
    scoreConfigs: AiObservabilityScoreConfigType[];
}

const AiObservabilityEvalRuleDialog = ({onClose, scoreConfigs}: AiObservabilityEvalRuleDialogProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const queryClient = useQueryClient();

    const [delaySeconds, setDelaySeconds] = useState('0');
    const [enabled, setEnabled] = useState(false);
    const [model, setModel] = useState('');
    const [name, setName] = useState('');
    const [promptTemplate, setPromptTemplate] = useState(
        'Evaluate the following LLM interaction.\n\nInput: {{input}}\n\nOutput: {{output}}\n\nRespond with a score from 0.0 to 1.0.',
    );
    const [samplingRate, setSamplingRate] = useState('1.0');
    const [scoreConfigId, setScoreConfigId] = useState('');

    const createMutation = useCreateAiObservabilityEvalRuleMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiObservabilityEvalRules']});
            onClose();
        },
    });

    const handleSubmit = () => {
        createMutation.mutate({
            delaySeconds: parseInt(delaySeconds) || undefined,
            enabled,
            model,
            name,
            promptTemplate,
            samplingRate: parseFloat(samplingRate),
            scoreConfigId,
            workspaceId: currentWorkspaceId + '',
        });
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-lg rounded-lg bg-background p-6 shadow-lg">
                <h3 className="mb-4 text-lg font-semibold">New Eval Rule</h3>

                <fieldset className="space-y-4 border-0">
                    <div>
                        <label className="mb-1 block text-sm font-medium" htmlFor="evalRuleName">
                            Name
                        </label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            id="evalRuleName"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="e.g., Relevance check on production"
                            value={name}
                        />
                    </div>

                    <div>
                        <label className="mb-1 block text-sm font-medium" htmlFor="evalRuleScoreConfig">
                            Score Config
                        </label>

                        <select
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            id="evalRuleScoreConfig"
                            onChange={(event) => setScoreConfigId(event.target.value)}
                            value={scoreConfigId}
                        >
                            <option value="">Select a score config...</option>

                            {scoreConfigs.map((config) => (
                                <option key={config.id} value={config.id}>
                                    {config.name}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div>
                        <label className="mb-1 block text-sm font-medium" htmlFor="evalRuleModel">
                            Model
                        </label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            id="evalRuleModel"
                            onChange={(event) => setModel(event.target.value)}
                            placeholder="e.g., openai/gpt-4o-mini"
                            value={model}
                        />
                    </div>

                    <div>
                        <label className="mb-1 block text-sm font-medium" htmlFor="evalRulePromptTemplate">
                            Prompt Template
                        </label>

                        <textarea
                            className="w-full rounded-md border px-3 py-2 text-sm font-mono"
                            id="evalRulePromptTemplate"
                            onChange={(event) => setPromptTemplate(event.target.value)}
                            rows={5}
                            value={promptTemplate}
                        />

                        <p className="mt-1 text-xs text-muted-foreground">
                            Available variables: {'{{input}}'}, {'{{output}}'}, {'{{metadata}}'}
                        </p>
                    </div>

                    <div className="flex gap-4">
                        <div className="flex-1">
                            <label className="mb-1 block text-sm font-medium" htmlFor="evalRuleSamplingRate">
                                Sampling Rate (0.0 - 1.0)
                            </label>

                            <input
                                className="w-full rounded-md border px-3 py-2 text-sm"
                                id="evalRuleSamplingRate"
                                max="1"
                                min="0"
                                onChange={(event) => setSamplingRate(event.target.value)}
                                step="0.01"
                                type="number"
                                value={samplingRate}
                            />
                        </div>

                        <div className="flex-1">
                            <label className="mb-1 block text-sm font-medium" htmlFor="evalRuleDelaySeconds">
                                Delay (seconds)
                            </label>

                            <input
                                className="w-full rounded-md border px-3 py-2 text-sm"
                                id="evalRuleDelaySeconds"
                                min="0"
                                onChange={(event) => setDelaySeconds(event.target.value)}
                                type="number"
                                value={delaySeconds}
                            />
                        </div>
                    </div>

                    <div className="flex items-center gap-2">
                        <input
                            checked={enabled}
                            id="evalRuleEnabled"
                            onChange={(event) => setEnabled(event.target.checked)}
                            type="checkbox"
                        />

                        <label className="text-sm font-medium" htmlFor="evalRuleEnabled">
                            Enable immediately
                        </label>
                    </div>
                </fieldset>

                <div className="mt-6 flex justify-end gap-2">
                    <button
                        className="rounded-md px-3 py-1.5 text-sm text-muted-foreground hover:bg-muted"
                        onClick={onClose}
                    >
                        Cancel
                    </button>

                    <button
                        className="rounded-md bg-primary px-3 py-1.5 text-sm text-primary-foreground hover:bg-primary/90"
                        disabled={!name || !model || !scoreConfigId || !promptTemplate}
                        onClick={handleSubmit}
                    >
                        Create
                    </button>
                </div>
            </div>
        </div>
    );
};

export default AiObservabilityEvalRuleDialog;
```

- [ ] **Step 3: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/scores/AiObservabilityEvalRules.tsx \
  src/pages/automation/ai-gateway/components/scores/AiObservabilityEvalRuleDialog.tsx
git commit -m "732 client - Add Eval Rules list and create dialog components"
```

---

## Task 20: Client — Inline Scoring in Trace Detail

**Files:**
- Modify: `client/src/pages/automation/ai-gateway/components/traces/AiObservabilityTraceDetail.tsx`

- [ ] **Step 1: Add inline scoring to trace detail**

In `AiObservabilityTraceDetail.tsx`, add a scores section below the trace header that displays existing scores and allows creating new manual scores.

Add imports:
```typescript
import {
    useAiObservabilityScoresByTraceQuery,
    useAiObservabilityScoreConfigsQuery,
    useCreateAiObservabilityScoreMutation,
} from '@/shared/middleware/graphql';
import {useWorkspaceStore} from '@/shared/stores/workspace.store';
import {useQueryClient} from '@tanstack/react-query';
import {StarIcon, ThumbsDownIcon, ThumbsUpIcon} from 'lucide-react';
import {useState} from 'react';
```

After the trace header info (`<div className="mb-6">...</div>`) and before the span tree, add:

```typescript
{/* Scores section */}
<div className="mb-6">
    <h3 className="mb-3 flex items-center gap-2 text-sm font-semibold">
        <StarIcon className="size-4" />
        Scores ({traceScores.length})
    </h3>

    {traceScores.length > 0 && (
        <div className="mb-3 flex flex-wrap gap-2">
            {traceScores.map((traceScore) => (
                <div className="rounded-md border px-3 py-2 text-sm" key={traceScore.id}>
                    <span className="font-medium">{traceScore.name}:</span>{' '}
                    {traceScore.value != null
                        ? Number(traceScore.value).toFixed(2)
                        : traceScore.stringValue || '-'}
                    <span className="ml-2 text-xs text-muted-foreground">{traceScore.source}</span>
                </div>
            ))}
        </div>
    )}

    {/* Quick score buttons */}
    <div className="flex items-center gap-2">
        <span className="text-sm text-muted-foreground">Quick score:</span>
        <button
            className="flex items-center gap-1 rounded-md border px-2 py-1 text-sm hover:bg-green-50"
            onClick={() => handleQuickScore(1)}
        >
            <ThumbsUpIcon className="size-3" />
        </button>
        <button
            className="flex items-center gap-1 rounded-md border px-2 py-1 text-sm hover:bg-red-50"
            onClick={() => handleQuickScore(0)}
        >
            <ThumbsDownIcon className="size-3" />
        </button>
    </div>
</div>
```

Add the query and mutation hooks inside the component:
```typescript
const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
const queryClient = useQueryClient();

const {data: scoresData} = useAiObservabilityScoresByTraceQuery({
    traceId: traceId,
});

const traceScores = scoresData?.aiObservabilityScoresByTrace ?? [];

const createScoreMutation = useCreateAiObservabilityScoreMutation({
    onSuccess: () => {
        queryClient.invalidateQueries({queryKey: ['aiObservabilityScoresByTrace']});
    },
});

const handleQuickScore = (scoreValue: number) => {
    createScoreMutation.mutate({
        dataType: 'BOOLEAN' as const,
        name: 'quality',
        source: 'MANUAL' as const,
        traceId: traceId,
        value: scoreValue,
        workspaceId: currentWorkspaceId + '',
    });
};
```

- [ ] **Step 2: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/traces/AiObservabilityTraceDetail.tsx
git commit -m "732 client - Add inline scoring controls to trace detail view"
```

---

## Task 21: Client — Verify and Format

- [ ] **Step 1: Run format**

Run: `cd client && npm run format`
Expected: Files formatted

- [ ] **Step 2: Run lint and typecheck**

Run: `cd client && npm run check`
Expected: No errors

- [ ] **Step 3: Fix any issues found in step 2**

Address lint/typecheck errors as needed (sort-keys, import ordering, naming conventions, ref-name-suffix).

- [ ] **Step 4: Commit fixes if any**

```bash
cd client
git add -A
git commit -m "732 client - Fix lint and format issues for evaluation and scoring components"
```

---

## Task 22: Server — Verify Full Compilation

- [ ] **Step 1: Run spotlessApply**

Run: `./gradlew spotlessApply`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run compileJava for the full gateway module**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-graphql:compileJava :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-public-rest:compileJava`
Expected: BUILD SUCCESSFUL for all modules

- [ ] **Step 3: Commit formatting fixes if any**

```bash
git add -A
git commit -m "732 Apply spotless formatting to evaluation and scoring classes"
```
