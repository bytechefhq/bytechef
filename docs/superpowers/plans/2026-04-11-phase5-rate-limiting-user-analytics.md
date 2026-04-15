# Phase 5: User/Tag Analytics & Rate Limiting -- Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add configurable rate limiting (per-user, per-property, global) with pluggable backends (in-memory, Redis), custom property tracking on traces/request logs, and user-level analytics aggregations to the AI Gateway.

**Architecture:** New domain entities (`AiGatewayRateLimit`, `AiGatewayCustomProperty`) in the existing `automation-ai-gateway` module. Rate limiter abstraction (`AiGatewayRateLimiter`) with two implementations conditionally registered via `@ConditionalOnProperty`. Rate limiting evaluated in `AiGatewayFacade` pre-request pipeline alongside budget checking. Custom properties extracted from `X-ByteChef-Property-*` headers. User analytics built as aggregation queries over traces and custom properties. New "Rate Limits" sidebar tab in client. Monitoring tab enhanced with user filter.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JDBC, PostgreSQL, Liquibase, Redis (optional), GraphQL (Spring GraphQL), React 19, TypeScript 5.9, TanStack Query, Tailwind CSS

**Reference spec:** `docs/superpowers/specs/2026-04-11-ai-gateway-observability-platform-design.md` -- Phase 5 section

**Depends on:** Phase 1 (Tracing & Sessions) -- uses `X-ByteChef-User-Id` header, `ai_observability_trace` table for user analytics

---

## File Map

### Server -- API module (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `src/main/java/.../domain/AiGatewayRateLimit.java` | Rate limit rule domain entity |
| Create | `src/main/java/.../domain/AiGatewayRateLimitScope.java` | Enum: GLOBAL, PER_USER, PER_PROPERTY |
| Create | `src/main/java/.../domain/AiGatewayRateLimitType.java` | Enum: REQUESTS, TOKENS, COST |
| Create | `src/main/java/.../domain/AiGatewayCustomProperty.java` | Custom property domain entity |
| Create | `src/main/java/.../domain/RateLimitExceededException.java` | Exception for HTTP 429 |
| Create | `src/main/java/.../dto/AiGatewayRateLimitResult.java` | Result DTO from rate limiter |
| Create | `src/main/java/.../dto/AiGatewayPropertyHeaders.java` | Parsed property headers DTO |
| Create | `src/main/java/.../dto/AiGatewayUserAnalytics.java` | User analytics aggregation DTO |
| Create | `src/main/java/.../ratelimit/AiGatewayRateLimiter.java` | Rate limiter abstraction interface |
| Create | `src/main/java/.../repository/AiGatewayRateLimitRepository.java` | Rate limit repository |
| Create | `src/main/java/.../repository/AiGatewayCustomPropertyRepository.java` | Custom property repository |
| Create | `src/main/java/.../service/AiGatewayRateLimitService.java` | Rate limit service interface |
| Create | `src/main/java/.../service/AiGatewayCustomPropertyService.java` | Custom property service interface |

### Server -- Service module (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/`)

| Action | Path | Purpose |
|---|---|---|
| Modify | `src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000001_ai_gateway_init.xml` | Add changesets for rate_limit and custom_property tables |
| Create | `src/main/java/.../ratelimit/InMemoryAiGatewayRateLimiter.java` | In-memory sliding window implementation |
| Create | `src/main/java/.../ratelimit/RedisAiGatewayRateLimiter.java` | Redis INCR+EXPIRE implementation |
| Create | `src/main/java/.../ratelimit/AiGatewayRateLimitChecker.java` | Orchestrates rate limit rule evaluation |
| Create | `src/main/java/.../service/AiGatewayRateLimitServiceImpl.java` | Rate limit service impl |
| Create | `src/main/java/.../service/AiGatewayCustomPropertyServiceImpl.java` | Custom property service impl |
| Modify | `src/main/java/.../facade/AiGatewayFacade.java` | Add rate limiting to pre-request pipeline, extract property headers |

### Server -- GraphQL module (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `src/main/resources/graphql/ai-gateway-rate-limit.graphqls` | Rate limit GraphQL schema |
| Create | `src/main/resources/graphql/ai-gateway-user-analytics.graphqls` | User analytics GraphQL schema |
| Create | `src/main/java/.../web/graphql/AiGatewayRateLimitGraphQlController.java` | Rate limit CRUD queries/mutations |
| Create | `src/main/java/.../web/graphql/AiGatewayUserAnalyticsGraphQlController.java` | User analytics query controller |

### Server -- Public REST module (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-public-rest/`)

| Action | Path | Purpose |
|---|---|---|
| Modify | `src/main/java/.../public_/web/rest/AiGatewayChatCompletionApiController.java` | Extract property headers, handle 429 response with rate limit headers |

### Client (`client/src/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `graphql/automation/ai-gateway/aiGatewayRateLimits.graphql` | Rate limit queries/mutations |
| Create | `graphql/automation/ai-gateway/aiGatewayUserAnalytics.graphql` | User analytics query |
| Modify | `pages/automation/ai-gateway/types.ts` | Add rate limit and user analytics types |
| Modify | `pages/automation/ai-gateway/AiGateway.tsx` | Add Rate Limits sidebar tab |
| Create | `pages/automation/ai-gateway/components/rate-limits/AiGatewayRateLimits.tsx` | Rate limits list page |
| Create | `pages/automation/ai-gateway/components/rate-limits/AiGatewayRateLimitDialog.tsx` | Create/edit rate limit dialog |
| Modify | `pages/automation/ai-gateway/components/monitoring/AiGatewayDashboard.tsx` | Add user filter dropdown |

---

## Task 1: Liquibase Migration -- Rate Limit and Custom Property Tables

**Files:**
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000001_ai_gateway_init.xml`

- [ ] **Step 1: Add new changesets to the existing file**

Add two new changesets before the closing `</databaseChangeLog>` tag. The existing file has changesets `00000000000001`, `00000000000001-10`, and `00000000000001-11`. Add `00000000000001-20` for rate limits and `00000000000001-21` for custom properties.

```xml
    <changeSet id="00000000000001-20" author="Ivica Cardic">
        <preConditions onFail="MARK_RAN">
            <not>
                <tableExists tableName="ai_gateway_rate_limit"/>
            </not>
        </preConditions>

        <createTable tableName="ai_gateway_rate_limit">
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
            <column name="scope" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="property_key" type="VARCHAR(256)"/>
            <column name="limit_type" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="limit_value" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="window_seconds" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="enabled" type="BOOLEAN" defaultValueBoolean="true">
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

        <createIndex tableName="ai_gateway_rate_limit" indexName="idx_ai_gw_rate_limit_workspace">
            <column name="workspace_id"/>
        </createIndex>

        <createIndex tableName="ai_gateway_rate_limit" indexName="idx_ai_gw_rate_limit_project">
            <column name="project_id"/>
        </createIndex>

        <createIndex tableName="ai_gateway_rate_limit" indexName="idx_ai_gw_rate_limit_enabled">
            <column name="enabled"/>
        </createIndex>
    </changeSet>

    <changeSet id="00000000000001-21" author="Ivica Cardic">
        <preConditions onFail="MARK_RAN">
            <not>
                <tableExists tableName="ai_gateway_custom_property"/>
            </not>
        </preConditions>

        <createTable tableName="ai_gateway_custom_property">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="trace_id" type="BIGINT"/>
            <column name="request_log_id" type="BIGINT"/>
            <column name="key" type="VARCHAR(256)">
                <constraints nullable="false"/>
            </column>
            <column name="value" type="VARCHAR(1024)">
                <constraints nullable="false"/>
            </column>
            <column name="workspace_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addForeignKeyConstraint constraintName="fk_ai_gw_custom_prop_trace"
                                 baseTableName="ai_gateway_custom_property" baseColumnNames="trace_id"
                                 referencedTableName="ai_observability_trace" referencedColumnNames="id"/>

        <addForeignKeyConstraint constraintName="fk_ai_gw_custom_prop_req_log"
                                 baseTableName="ai_gateway_custom_property" baseColumnNames="request_log_id"
                                 referencedTableName="ai_gateway_request_log" referencedColumnNames="id"/>

        <createIndex tableName="ai_gateway_custom_property" indexName="idx_ai_gw_custom_prop_trace">
            <column name="trace_id"/>
        </createIndex>

        <createIndex tableName="ai_gateway_custom_property" indexName="idx_ai_gw_custom_prop_req_log">
            <column name="request_log_id"/>
        </createIndex>

        <createIndex tableName="ai_gateway_custom_property" indexName="idx_ai_gw_custom_prop_ws_key">
            <column name="workspace_id"/>
            <column name="key"/>
        </createIndex>
    </changeSet>
```

- [ ] **Step 2: Verify the migration compiles**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000001_ai_gateway_init.xml
git commit -m "732 Add Liquibase changesets for rate_limit and custom_property tables"
```

---

## Task 2: Enum Domain Classes

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiGatewayRateLimitScope.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiGatewayRateLimitType.java`

- [ ] **Step 1: Create AiGatewayRateLimitScope**

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
public enum AiGatewayRateLimitScope {

    GLOBAL,
    PER_USER,
    PER_PROPERTY
}
```

- [ ] **Step 2: Create AiGatewayRateLimitType**

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
public enum AiGatewayRateLimitType {

    REQUESTS,
    TOKENS,
    COST
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiGatewayRateLimitScope.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiGatewayRateLimitType.java
git commit -m "732 Add rate limit enum types (scope, limit type)"
```

---

## Task 3: RateLimitExceededException

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/RateLimitExceededException.java`

- [ ] **Step 1: Create RateLimitExceededException**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

/**
 * Thrown when a rate limit rule is exceeded. The public REST controller translates this to HTTP 429.
 *
 * @version ee
 */
public class RateLimitExceededException extends RuntimeException {

    private final int remaining;
    private final long resetEpochSeconds;

    public RateLimitExceededException(String message, int remaining, long resetEpochSeconds) {
        super(message);

        this.remaining = remaining;
        this.resetEpochSeconds = resetEpochSeconds;
    }

    public int getRemaining() {
        return remaining;
    }

    public long getResetEpochSeconds() {
        return resetEpochSeconds;
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/RateLimitExceededException.java
git commit -m "732 Add RateLimitExceededException for HTTP 429 responses"
```

---

## Task 4: AiGatewayRateLimit Domain Entity

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiGatewayRateLimit.java`

- [ ] **Step 1: Create AiGatewayRateLimit**

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
@Table("ai_gateway_rate_limit")
public class AiGatewayRateLimit {

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

    @Column("limit_type")
    private int limitType;

    @Column("limit_value")
    private int limitValue;

    @Column
    private String name;

    @Column("project_id")
    private Long projectId;

    @Column("property_key")
    private String propertyKey;

    @Column
    private int scope;

    @Version
    private int version;

    @Column("window_seconds")
    private int windowSeconds;

    @Column("workspace_id")
    private Long workspaceId;

    private AiGatewayRateLimit() {
    }

    public AiGatewayRateLimit(
        Long workspaceId, String name, AiGatewayRateLimitScope scope,
        AiGatewayRateLimitType limitType, int limitValue, int windowSeconds) {

        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notBlank(name, "name must not be blank");
        Validate.notNull(scope, "scope must not be null");
        Validate.notNull(limitType, "limitType must not be null");
        Validate.isTrue(limitValue > 0, "limitValue must be positive");
        Validate.isTrue(windowSeconds > 0, "windowSeconds must be positive");

        this.enabled = true;
        this.limitType = limitType.ordinal();
        this.limitValue = limitValue;
        this.name = name;
        this.scope = scope.ordinal();
        this.windowSeconds = windowSeconds;
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiGatewayRateLimit aiGatewayRateLimit)) {
            return false;
        }

        return Objects.equals(id, aiGatewayRateLimit.id);
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

    public Long getId() {
        return id;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public AiGatewayRateLimitType getLimitType() {
        return AiGatewayRateLimitType.values()[limitType];
    }

    public int getLimitValue() {
        return limitValue;
    }

    public String getName() {
        return name;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public AiGatewayRateLimitScope getScope() {
        return AiGatewayRateLimitScope.values()[scope];
    }

    public int getVersion() {
        return version;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setLimitType(AiGatewayRateLimitType limitType) {
        Validate.notNull(limitType, "limitType must not be null");

        this.limitType = limitType.ordinal();
    }

    public void setLimitValue(int limitValue) {
        Validate.isTrue(limitValue > 0, "limitValue must be positive");

        this.limitValue = limitValue;
    }

    public void setName(String name) {
        Validate.notBlank(name, "name must not be blank");

        this.name = name;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setPropertyKey(String propertyKey) {
        this.propertyKey = propertyKey;
    }

    public void setScope(AiGatewayRateLimitScope scope) {
        Validate.notNull(scope, "scope must not be null");

        this.scope = scope.ordinal();
    }

    public void setWindowSeconds(int windowSeconds) {
        Validate.isTrue(windowSeconds > 0, "windowSeconds must be positive");

        this.windowSeconds = windowSeconds;
    }

    @Override
    public String toString() {
        return "AiGatewayRateLimit{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", name='" + name + '\'' +
            ", scope=" + getScope() +
            ", limitType=" + getLimitType() +
            ", limitValue=" + limitValue +
            ", windowSeconds=" + windowSeconds +
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
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiGatewayRateLimit.java
git commit -m "732 Add AiGatewayRateLimit domain entity"
```

---

## Task 5: AiGatewayCustomProperty Domain Entity

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiGatewayCustomProperty.java`

- [ ] **Step 1: Create AiGatewayCustomProperty**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_gateway_custom_property")
public class AiGatewayCustomProperty {

    @Id
    private Long id;

    @Column
    private String key;

    @Column("request_log_id")
    private Long requestLogId;

    @Column("trace_id")
    private Long traceId;

    @Column
    private String value;

    @Column("workspace_id")
    private Long workspaceId;

    private AiGatewayCustomProperty() {
    }

    public AiGatewayCustomProperty(Long workspaceId, String key, String value) {
        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notBlank(key, "key must not be blank");
        Validate.notBlank(value, "value must not be blank");

        this.key = key;
        this.value = value;
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiGatewayCustomProperty aiGatewayCustomProperty)) {
            return false;
        }

        return Objects.equals(id, aiGatewayCustomProperty.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public Long getRequestLogId() {
        return requestLogId;
    }

    public Long getTraceId() {
        return traceId;
    }

    public String getValue() {
        return value;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setRequestLogId(Long requestLogId) {
        this.requestLogId = requestLogId;
    }

    public void setTraceId(Long traceId) {
        this.traceId = traceId;
    }

    @Override
    public String toString() {
        return "AiGatewayCustomProperty{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", key='" + key + '\'' +
            ", value='" + value + '\'' +
            '}';
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiGatewayCustomProperty.java
git commit -m "732 Add AiGatewayCustomProperty domain entity"
```

---

## Task 6: DTOs -- RateLimitResult, PropertyHeaders, UserAnalytics

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/dto/AiGatewayRateLimitResult.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/dto/AiGatewayPropertyHeaders.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/dto/AiGatewayUserAnalytics.java`

- [ ] **Step 1: Create AiGatewayRateLimitResult**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.dto;

/**
 * Result of a rate limit acquisition attempt.
 *
 * @version ee
 */
public record AiGatewayRateLimitResult(boolean allowed, int remaining, long resetEpochSeconds) {

    public static AiGatewayRateLimitResult allowed(int remaining, long resetEpochSeconds) {
        return new AiGatewayRateLimitResult(true, remaining, resetEpochSeconds);
    }

    public static AiGatewayRateLimitResult denied(int remaining, long resetEpochSeconds) {
        return new AiGatewayRateLimitResult(false, remaining, resetEpochSeconds);
    }
}
```

- [ ] **Step 2: Create AiGatewayPropertyHeaders**

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
 * Parsed custom property and user headers from an incoming gateway request.
 *
 * @version ee
 */
public record AiGatewayPropertyHeaders(
    @Nullable String userId,
    Map<String, String> properties) {

    public static final String HEADER_USER_ID = "X-ByteChef-User-Id";
    public static final String HEADER_PROPERTY_PREFIX = "X-ByteChef-Property-";

    public boolean hasUserId() {
        return userId != null && !userId.isBlank();
    }

    public boolean hasProperties() {
        return !properties.isEmpty();
    }
}
```

- [ ] **Step 3: Create AiGatewayUserAnalytics**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.dto;

import java.math.BigDecimal;

/**
 * Aggregated analytics for a single user within a time window.
 *
 * @version ee
 */
public record AiGatewayUserAnalytics(
    String userId,
    long requestCount,
    BigDecimal totalCost,
    long totalInputTokens,
    long totalOutputTokens,
    double averageLatencyMs,
    double errorRate) {
}
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/dto/AiGatewayRateLimitResult.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/dto/AiGatewayPropertyHeaders.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/dto/AiGatewayUserAnalytics.java
git commit -m "732 Add DTOs for rate limit result, property headers, and user analytics"
```

---

## Task 7: Rate Limiter Abstraction Interface

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/ratelimit/AiGatewayRateLimiter.java`

- [ ] **Step 1: Create AiGatewayRateLimiter**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.ratelimit;

import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayRateLimitResult;

/**
 * Abstraction for rate limiting backends. Implementations provide sliding window counters
 * for tracking request/token/cost usage.
 *
 * @version ee
 */
public interface AiGatewayRateLimiter {

    /**
     * Attempt to acquire a permit for the given key.
     *
     * @param key           composite key identifying the rate limit bucket (e.g., "ws:1:rule:5:user:abc")
     * @param limit         maximum allowed count within the window
     * @param windowSeconds sliding window duration in seconds
     * @return result indicating whether the request is allowed, remaining quota, and reset time
     */
    AiGatewayRateLimitResult tryAcquire(String key, int limit, int windowSeconds);

    /**
     * Reset the counter for the given key.
     *
     * @param key the rate limit bucket key to reset
     */
    void reset(String key);
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/ratelimit/AiGatewayRateLimiter.java
git commit -m "732 Add AiGatewayRateLimiter abstraction interface"
```

---

## Task 8: Repository Interfaces

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiGatewayRateLimitRepository.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiGatewayCustomPropertyRepository.java`

- [ ] **Step 1: Create AiGatewayRateLimitRepository**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimit;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiGatewayRateLimitRepository extends ListCrudRepository<AiGatewayRateLimit, Long> {

    List<AiGatewayRateLimit> findAllByWorkspaceId(Long workspaceId);

    List<AiGatewayRateLimit> findAllByWorkspaceIdAndEnabled(Long workspaceId, boolean enabled);

    List<AiGatewayRateLimit> findAllByWorkspaceIdAndProjectId(Long workspaceId, Long projectId);
}
```

- [ ] **Step 2: Create AiGatewayCustomPropertyRepository**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayCustomProperty;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiGatewayCustomPropertyRepository extends ListCrudRepository<AiGatewayCustomProperty, Long> {

    List<AiGatewayCustomProperty> findAllByTraceId(Long traceId);

    List<AiGatewayCustomProperty> findAllByRequestLogId(Long requestLogId);

    List<AiGatewayCustomProperty> findAllByWorkspaceIdAndKey(Long workspaceId, String key);
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiGatewayRateLimitRepository.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiGatewayCustomPropertyRepository.java
git commit -m "732 Add repository interfaces for rate limit and custom property"
```

---

## Task 9: Service Interfaces

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiGatewayRateLimitService.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiGatewayCustomPropertyService.java`

- [ ] **Step 1: Create AiGatewayRateLimitService**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimit;
import java.util.List;

/**
 * @version ee
 */
public interface AiGatewayRateLimitService {

    AiGatewayRateLimit create(AiGatewayRateLimit rateLimit);

    void delete(long id);

    AiGatewayRateLimit getRateLimit(long id);

    List<AiGatewayRateLimit> getRateLimitsByWorkspace(Long workspaceId);

    List<AiGatewayRateLimit> getEnabledRateLimitsByWorkspace(Long workspaceId);

    AiGatewayRateLimit update(AiGatewayRateLimit rateLimit);
}
```

- [ ] **Step 2: Create AiGatewayCustomPropertyService**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayCustomProperty;
import java.util.List;

/**
 * @version ee
 */
public interface AiGatewayCustomPropertyService {

    AiGatewayCustomProperty create(AiGatewayCustomProperty customProperty);

    List<AiGatewayCustomProperty> createAll(List<AiGatewayCustomProperty> customProperties);

    List<AiGatewayCustomProperty> getCustomPropertiesByTrace(Long traceId);

    List<AiGatewayCustomProperty> getCustomPropertiesByRequestLog(Long requestLogId);

    List<AiGatewayCustomProperty> getCustomPropertiesByWorkspaceAndKey(Long workspaceId, String key);
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiGatewayRateLimitService.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiGatewayCustomPropertyService.java
git commit -m "732 Add service interfaces for rate limit and custom property"
```

---

## Task 10: Service Implementations

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiGatewayRateLimitServiceImpl.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiGatewayCustomPropertyServiceImpl.java`

- [ ] **Step 1: Create AiGatewayRateLimitServiceImpl**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimit;
import com.bytechef.ee.automation.ai.gateway.repository.AiGatewayRateLimitRepository;
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
class AiGatewayRateLimitServiceImpl implements AiGatewayRateLimitService {

    private final AiGatewayRateLimitRepository aiGatewayRateLimitRepository;

    public AiGatewayRateLimitServiceImpl(
        AiGatewayRateLimitRepository aiGatewayRateLimitRepository) {

        this.aiGatewayRateLimitRepository = aiGatewayRateLimitRepository;
    }

    @Override
    public AiGatewayRateLimit create(AiGatewayRateLimit rateLimit) {
        Validate.notNull(rateLimit, "rateLimit must not be null");
        Validate.isTrue(rateLimit.getId() == null, "rateLimit id must be null for creation");

        return aiGatewayRateLimitRepository.save(rateLimit);
    }

    @Override
    public void delete(long id) {
        aiGatewayRateLimitRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiGatewayRateLimit getRateLimit(long id) {
        return aiGatewayRateLimitRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiGatewayRateLimit not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayRateLimit> getRateLimitsByWorkspace(Long workspaceId) {
        return aiGatewayRateLimitRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayRateLimit> getEnabledRateLimitsByWorkspace(Long workspaceId) {
        return aiGatewayRateLimitRepository.findAllByWorkspaceIdAndEnabled(workspaceId, true);
    }

    @Override
    public AiGatewayRateLimit update(AiGatewayRateLimit rateLimit) {
        Validate.notNull(rateLimit, "rateLimit must not be null");
        Validate.notNull(rateLimit.getId(), "rateLimit id must not be null for update");

        return aiGatewayRateLimitRepository.save(rateLimit);
    }
}
```

- [ ] **Step 2: Create AiGatewayCustomPropertyServiceImpl**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayCustomProperty;
import com.bytechef.ee.automation.ai.gateway.repository.AiGatewayCustomPropertyRepository;
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
class AiGatewayCustomPropertyServiceImpl implements AiGatewayCustomPropertyService {

    private final AiGatewayCustomPropertyRepository aiGatewayCustomPropertyRepository;

    public AiGatewayCustomPropertyServiceImpl(
        AiGatewayCustomPropertyRepository aiGatewayCustomPropertyRepository) {

        this.aiGatewayCustomPropertyRepository = aiGatewayCustomPropertyRepository;
    }

    @Override
    public AiGatewayCustomProperty create(AiGatewayCustomProperty customProperty) {
        Validate.notNull(customProperty, "customProperty must not be null");
        Validate.isTrue(customProperty.getId() == null, "customProperty id must be null for creation");

        return aiGatewayCustomPropertyRepository.save(customProperty);
    }

    @Override
    public List<AiGatewayCustomProperty> createAll(List<AiGatewayCustomProperty> customProperties) {
        Validate.notNull(customProperties, "customProperties must not be null");

        return aiGatewayCustomPropertyRepository.saveAll(customProperties);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayCustomProperty> getCustomPropertiesByTrace(Long traceId) {
        return aiGatewayCustomPropertyRepository.findAllByTraceId(traceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayCustomProperty> getCustomPropertiesByRequestLog(Long requestLogId) {
        return aiGatewayCustomPropertyRepository.findAllByRequestLogId(requestLogId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayCustomProperty> getCustomPropertiesByWorkspaceAndKey(Long workspaceId, String key) {
        return aiGatewayCustomPropertyRepository.findAllByWorkspaceIdAndKey(workspaceId, key);
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiGatewayRateLimitServiceImpl.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiGatewayCustomPropertyServiceImpl.java
git commit -m "732 Add service implementations for rate limit and custom property"
```

---

## Task 11: InMemoryAiGatewayRateLimiter

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/ratelimit/InMemoryAiGatewayRateLimiter.java`

- [ ] **Step 1: Create InMemoryAiGatewayRateLimiter**

Uses a `ConcurrentHashMap` with sliding window counters. Each key maps to a deque of timestamps. On `tryAcquire`, expired entries outside the window are evicted, then the count is checked against the limit.

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.ratelimit;

import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayRateLimitResult;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * In-memory sliding window rate limiter using ConcurrentHashMap.
 * Suitable for single-instance deployments.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(
    prefix = "bytechef.ai.gateway.rate-limiting", name = "provider", havingValue = "memory",
    matchIfMissing = true)
public class InMemoryAiGatewayRateLimiter implements AiGatewayRateLimiter {

    private final ConcurrentHashMap<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    @Override
    public AiGatewayRateLimitResult tryAcquire(String key, int limit, int windowSeconds) {
        long nowMillis = Instant.now().toEpochMilli();
        long windowStartMillis = nowMillis - (windowSeconds * 1000L);

        Deque<Long> timestamps = windows.computeIfAbsent(key, unusedKey -> new ConcurrentLinkedDeque<>());

        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStartMillis) {
            timestamps.pollFirst();
        }

        long resetEpochSeconds = (nowMillis / 1000) + windowSeconds;

        if (timestamps.size() >= limit) {
            return AiGatewayRateLimitResult.denied(0, resetEpochSeconds);
        }

        timestamps.addLast(nowMillis);

        int remaining = limit - timestamps.size();

        return AiGatewayRateLimitResult.allowed(remaining, resetEpochSeconds);
    }

    @Override
    public void reset(String key) {
        windows.remove(key);
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/ratelimit/InMemoryAiGatewayRateLimiter.java
git commit -m "732 Add in-memory sliding window rate limiter implementation"
```

---

## Task 12: RedisAiGatewayRateLimiter

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/ratelimit/RedisAiGatewayRateLimiter.java`

- [ ] **Step 1: Create RedisAiGatewayRateLimiter**

Uses Redis `INCR` + `EXPIRE` for distributed sliding windows. Required for multi-instance deployments.

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.ratelimit;

import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayRateLimitResult;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed sliding window rate limiter using INCR + EXPIRE.
 * Required for multi-instance (clustered) deployments.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway.rate-limiting", name = "provider", havingValue = "redis")
@SuppressFBWarnings("EI")
public class RedisAiGatewayRateLimiter implements AiGatewayRateLimiter {

    private static final String KEY_PREFIX = "ai_gw_rate:";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisAiGatewayRateLimiter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public AiGatewayRateLimitResult tryAcquire(String key, int limit, int windowSeconds) {
        String redisKey = KEY_PREFIX + key;

        Long currentCount = stringRedisTemplate.opsForValue().increment(redisKey);

        if (currentCount == null) {
            return AiGatewayRateLimitResult.denied(0, Instant.now().getEpochSecond() + windowSeconds);
        }

        if (currentCount == 1) {
            stringRedisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
        }

        long resetEpochSeconds = Instant.now().getEpochSecond() + windowSeconds;

        if (currentCount > limit) {
            return AiGatewayRateLimitResult.denied(0, resetEpochSeconds);
        }

        int remaining = limit - currentCount.intValue();

        return AiGatewayRateLimitResult.allowed(remaining, resetEpochSeconds);
    }

    @Override
    public void reset(String key) {
        stringRedisTemplate.delete(KEY_PREFIX + key);
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava`
Expected: BUILD SUCCESSFUL

Note: If `StringRedisTemplate` is not on the classpath, check the service module's `build.gradle.kts` for a `spring-boot-starter-data-redis` dependency. If missing, add it as `implementation("org.springframework.boot:spring-boot-starter-data-redis")` with `optional = true` or use `compileOnly`. The `@ConditionalOnProperty` ensures this bean is only created when Redis is configured.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/ratelimit/RedisAiGatewayRateLimiter.java
git commit -m "732 Add Redis-backed rate limiter implementation"
```

---

## Task 13: AiGatewayRateLimitChecker

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/ratelimit/AiGatewayRateLimitChecker.java`

- [ ] **Step 1: Create AiGatewayRateLimitChecker**

Orchestrates evaluation of all applicable rate limit rules for a request. Builds composite keys based on rule scope (GLOBAL, PER_USER, PER_PROPERTY), calls the `AiGatewayRateLimiter`, and throws `RateLimitExceededException` if any rule is exceeded.

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.ratelimit;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimit;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimitScope;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimitType;
import com.bytechef.ee.automation.ai.gateway.domain.RateLimitExceededException;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayPropertyHeaders;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayRateLimitResult;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayRateLimitService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Evaluates all applicable rate limit rules for an incoming request.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway.rate-limiting", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
public class AiGatewayRateLimitChecker {

    private static final Logger logger = LoggerFactory.getLogger(AiGatewayRateLimitChecker.class);

    private final AiGatewayRateLimiter aiGatewayRateLimiter;
    private final AiGatewayRateLimitService aiGatewayRateLimitService;

    public AiGatewayRateLimitChecker(
        AiGatewayRateLimiter aiGatewayRateLimiter,
        AiGatewayRateLimitService aiGatewayRateLimitService) {

        this.aiGatewayRateLimiter = aiGatewayRateLimiter;
        this.aiGatewayRateLimitService = aiGatewayRateLimitService;
    }

    /**
     * Check all enabled rate limit rules for the given workspace. Only evaluates REQUESTS-type
     * limits pre-request. TOKENS and COST limits are evaluated post-request.
     *
     * @param workspaceId     the workspace
     * @param propertyHeaders parsed user ID and custom properties from headers
     * @throws RateLimitExceededException if any rate limit is exceeded
     */
    public void checkPreRequestLimits(long workspaceId, AiGatewayPropertyHeaders propertyHeaders) {
        List<AiGatewayRateLimit> enabledRules =
            aiGatewayRateLimitService.getEnabledRateLimitsByWorkspace(workspaceId);

        for (AiGatewayRateLimit rule : enabledRules) {
            if (rule.getLimitType() != AiGatewayRateLimitType.REQUESTS) {
                continue;
            }

            String rateLimitKey = buildKey(workspaceId, rule, propertyHeaders);

            if (rateLimitKey == null) {
                continue;
            }

            AiGatewayRateLimitResult result = aiGatewayRateLimiter.tryAcquire(
                rateLimitKey, rule.getLimitValue(), rule.getWindowSeconds());

            if (!result.allowed()) {
                logger.info(
                    "Rate limit exceeded for rule '{}' (id={}), key='{}'",
                    rule.getName(), rule.getId(), rateLimitKey);

                throw new RateLimitExceededException(
                    "Rate limit exceeded: " + rule.getName(),
                    result.remaining(), result.resetEpochSeconds());
            }
        }
    }

    private String buildKey(
        long workspaceId, AiGatewayRateLimit rule, AiGatewayPropertyHeaders propertyHeaders) {

        AiGatewayRateLimitScope scope = rule.getScope();
        String baseKey = "ws:" + workspaceId + ":rule:" + rule.getId();

        if (scope == AiGatewayRateLimitScope.GLOBAL) {
            return baseKey;
        }

        if (scope == AiGatewayRateLimitScope.PER_USER) {
            if (!propertyHeaders.hasUserId()) {
                logger.debug(
                    "Skipping PER_USER rate limit rule '{}' -- no user ID in request headers",
                    rule.getName());

                return null;
            }

            return baseKey + ":user:" + propertyHeaders.userId();
        }

        if (scope == AiGatewayRateLimitScope.PER_PROPERTY) {
            String propertyKey = rule.getPropertyKey();

            if (propertyKey == null) {
                logger.warn(
                    "PER_PROPERTY rate limit rule '{}' has no property_key configured",
                    rule.getName());

                return null;
            }

            String propertyValue = propertyHeaders.properties().get(propertyKey);

            if (propertyValue == null) {
                logger.debug(
                    "Skipping PER_PROPERTY rate limit rule '{}' -- property '{}' not in request headers",
                    rule.getName(), propertyKey);

                return null;
            }

            return baseKey + ":prop:" + propertyKey + ":" + propertyValue;
        }

        return null;
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/ratelimit/AiGatewayRateLimitChecker.java
git commit -m "732 Add rate limit checker orchestrating rule evaluation"
```

---

## Task 14: Facade Integration -- Rate Limiting and Property Headers

**Files:**
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/facade/AiGatewayFacade.java`
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-public-rest/src/main/java/com/bytechef/ee/automation/ai/gateway/public_/web/rest/AiGatewayChatCompletionApiController.java`

- [ ] **Step 1: Add rate limit checker and custom property service to AiGatewayFacade**

Add optional injection of `AiGatewayRateLimitChecker` (it is only created when `rate-limiting.enabled=true`) and `AiGatewayCustomPropertyService`:

```java
// Add imports
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayPropertyHeaders;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayCustomProperty;
import com.bytechef.ee.automation.ai.gateway.ratelimit.AiGatewayRateLimitChecker;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayCustomPropertyService;

// Add fields
private final AiGatewayCustomPropertyService aiGatewayCustomPropertyService;
private final AiGatewayRateLimitChecker aiGatewayRateLimitChecker;
```

Update the constructor to include the new dependencies. Use `@Nullable` for `AiGatewayRateLimitChecker` since it is conditionally created:

```java
public AiGatewayFacade(
    AiGatewayBudgetChecker aiGatewayBudgetChecker,
    AiGatewayChatModelFactory aiGatewayChatModelFactory,
    AiGatewayContextCompressor aiGatewayContextCompressor,
    AiGatewayCostCalculator aiGatewayCostCalculator,
    AiGatewayCustomPropertyService aiGatewayCustomPropertyService,
    AiGatewayEmbeddingModelFactory aiGatewayEmbeddingModelFactory,
    AiGatewayModelDeploymentService aiGatewayModelDeploymentService,
    AiGatewayModelService aiGatewayModelService,
    AiGatewayProjectService aiGatewayProjectService,
    AiGatewayProviderService aiGatewayProviderService,
    AiGatewayRequestLogService aiGatewayRequestLogService,
    AiGatewayResponseCache aiGatewayResponseCache,
    AiGatewayRetryHandler aiGatewayRetryHandler,
    AiGatewayRouter aiGatewayRouter,
    AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService,
    @Nullable AiGatewayRateLimitChecker aiGatewayRateLimitChecker,
    TagService tagService,
    PlatformTransactionManager transactionManager) {

    // ... assign all fields ...
    this.aiGatewayCustomPropertyService = aiGatewayCustomPropertyService;
    this.aiGatewayRateLimitChecker = aiGatewayRateLimitChecker;
}
```

- [ ] **Step 2: Add pre-request rate limiting call**

In the `chatCompletion` method, add rate limit checking after the budget check. The `AiGatewayPropertyHeaders` will be extracted from the request and passed through:

```java
// Add new method signature that accepts property headers
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public AiGatewayChatCompletionResponse chatCompletion(
    AiGatewayChatCompletionRequest request, AiGatewayPropertyHeaders propertyHeaders) {

    checkBudget(request.tags(), request.model());
    checkRateLimits(request, propertyHeaders);

    // ... existing routing logic ...
}
```

Add the rate limit checking method:

```java
private void checkRateLimits(
    AiGatewayChatCompletionRequest request, AiGatewayPropertyHeaders propertyHeaders) {

    if (aiGatewayRateLimitChecker == null) {
        return;
    }

    long workspaceId = resolveWorkspaceId(request);

    aiGatewayRateLimitChecker.checkPreRequestLimits(workspaceId, propertyHeaders);
}
```

- [ ] **Step 3: Add custom property persistence after request completion**

Add a method to save custom properties linked to the request log and optionally to a trace:

```java
private void saveCustomProperties(
    AiGatewayPropertyHeaders propertyHeaders, Long workspaceId,
    Long requestLogId, Long traceId) {

    if (!propertyHeaders.hasProperties()) {
        return;
    }

    List<AiGatewayCustomProperty> customProperties = propertyHeaders.properties()
        .entrySet()
        .stream()
        .map(entry -> {
            AiGatewayCustomProperty customProperty = new AiGatewayCustomProperty(
                workspaceId, entry.getKey(), entry.getValue());

            customProperty.setRequestLogId(requestLogId);
            customProperty.setTraceId(traceId);

            return customProperty;
        })
        .toList();

    aiGatewayCustomPropertyService.createAll(customProperties);
}
```

Call `saveCustomProperties()` after the request log is saved, alongside the existing request logging logic.

- [ ] **Step 4: Modify the public REST controller to extract property headers and handle 429**

In `AiGatewayChatCompletionApiController.java`, add header extraction and exception handling:

```java
// Add imports
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayPropertyHeaders;
import com.bytechef.ee.automation.ai.gateway.domain.RateLimitExceededException;

// Add private method to extract property headers:
private AiGatewayPropertyHeaders extractPropertyHeaders(HttpServletRequest httpServletRequest) {
    Map<String, String> properties = new HashMap<>();

    Enumeration<String> headerNames = httpServletRequest.getHeaderNames();

    while (headerNames.hasMoreElements()) {
        String headerName = headerNames.nextElement();

        if (headerName.toLowerCase().startsWith(
            AiGatewayPropertyHeaders.HEADER_PROPERTY_PREFIX.toLowerCase())) {

            String propertyKey = headerName.substring(
                AiGatewayPropertyHeaders.HEADER_PROPERTY_PREFIX.length());

            properties.put(propertyKey, httpServletRequest.getHeader(headerName));
        }
    }

    return new AiGatewayPropertyHeaders(
        httpServletRequest.getHeader(AiGatewayPropertyHeaders.HEADER_USER_ID),
        properties);
}
```

Update the `chatCompletions` method to pass property headers to the facade and catch `RateLimitExceededException`:

```java
// In the chatCompletions method:
try {
    AiGatewayPropertyHeaders propertyHeaders = extractPropertyHeaders(httpServletRequest);

    AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(
        request, propertyHeaders);

    // ... existing response handling ...
} catch (RateLimitExceededException rateLimitExceededException) {
    httpServletResponse.setStatus(429);
    httpServletResponse.setHeader(
        "X-RateLimit-Remaining",
        String.valueOf(rateLimitExceededException.getRemaining()));
    httpServletResponse.setHeader(
        "X-RateLimit-Reset",
        String.valueOf(rateLimitExceededException.getResetEpochSeconds()));

    return ResponseEntity.status(429).body(Map.of(
        "error", Map.of(
            "message", rateLimitExceededException.getMessage(),
            "type", "rate_limit_exceeded")));
}
```

- [ ] **Step 5: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava && ./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-public-rest:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/facade/AiGatewayFacade.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-public-rest/src/main/java/com/bytechef/ee/automation/ai/gateway/public_/web/rest/AiGatewayChatCompletionApiController.java
git commit -m "732 Integrate rate limiting and custom properties into gateway request pipeline"
```

---

## Task 15: GraphQL Schema for Rate Limits and User Analytics

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-gateway-rate-limit.graphqls`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-gateway-user-analytics.graphqls`

- [ ] **Step 1: Create ai-gateway-rate-limit.graphqls**

```graphql
extend type Query {
    aiGatewayRateLimit(id: ID!): AiGatewayRateLimit
    aiGatewayRateLimits(workspaceId: ID!): [AiGatewayRateLimit]
}

extend type Mutation {
    createAiGatewayRateLimit(input: CreateAiGatewayRateLimitInput!): AiGatewayRateLimit
    deleteAiGatewayRateLimit(id: ID!): Boolean
    updateAiGatewayRateLimit(id: ID!, input: UpdateAiGatewayRateLimitInput!): AiGatewayRateLimit
}

type AiGatewayRateLimit {
    createdDate: Long
    enabled: Boolean!
    id: ID!
    lastModifiedDate: Long
    limitType: AiGatewayRateLimitType!
    limitValue: Int!
    name: String!
    projectId: ID
    propertyKey: String
    scope: AiGatewayRateLimitScope!
    version: Int
    windowSeconds: Int!
    workspaceId: ID!
}

enum AiGatewayRateLimitScope {
    GLOBAL
    PER_PROPERTY
    PER_USER
}

enum AiGatewayRateLimitType {
    COST
    REQUESTS
    TOKENS
}

input CreateAiGatewayRateLimitInput {
    enabled: Boolean
    limitType: AiGatewayRateLimitType!
    limitValue: Int!
    name: String!
    projectId: ID
    propertyKey: String
    scope: AiGatewayRateLimitScope!
    windowSeconds: Int!
    workspaceId: ID!
}

input UpdateAiGatewayRateLimitInput {
    enabled: Boolean
    limitType: AiGatewayRateLimitType
    limitValue: Int
    name: String
    propertyKey: String
    scope: AiGatewayRateLimitScope
    windowSeconds: Int
}
```

- [ ] **Step 2: Create ai-gateway-user-analytics.graphqls**

```graphql
extend type Query {
    aiGatewayUserAnalytics(
        workspaceId: ID!
        startDate: Long!
        endDate: Long!
    ): [AiGatewayUserAnalytics]
}

type AiGatewayUserAnalytics {
    averageLatencyMs: Float!
    errorRate: Float!
    requestCount: Long!
    totalCost: Float!
    totalInputTokens: Long!
    totalOutputTokens: Long!
    userId: String!
}
```

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-gateway-rate-limit.graphqls \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-gateway-user-analytics.graphqls
git commit -m "732 Add GraphQL schema for rate limits and user analytics"
```

---

## Task 16: GraphQL Controllers for Rate Limits and User Analytics

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiGatewayRateLimitGraphQlController.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiGatewayUserAnalyticsGraphQlController.java`

- [ ] **Step 1: Create AiGatewayRateLimitGraphQlController**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimit;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimitScope;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimitType;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayRateLimitService;
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
class AiGatewayRateLimitGraphQlController {

    private final AiGatewayRateLimitService aiGatewayRateLimitService;

    @SuppressFBWarnings("EI")
    AiGatewayRateLimitGraphQlController(AiGatewayRateLimitService aiGatewayRateLimitService) {
        this.aiGatewayRateLimitService = aiGatewayRateLimitService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public AiGatewayRateLimit aiGatewayRateLimit(@Argument long id) {
        return aiGatewayRateLimitService.getRateLimit(id);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public List<AiGatewayRateLimit> aiGatewayRateLimits(@Argument long workspaceId) {
        return aiGatewayRateLimitService.getRateLimitsByWorkspace(workspaceId);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayRateLimit createAiGatewayRateLimit(@Argument CreateAiGatewayRateLimitInput input) {
        AiGatewayRateLimit rateLimit = new AiGatewayRateLimit(
            Long.valueOf(input.workspaceId()), input.name(), input.scope(),
            input.limitType(), input.limitValue(), input.windowSeconds());

        if (input.projectId() != null) {
            rateLimit.setProjectId(Long.valueOf(input.projectId()));
        }

        if (input.propertyKey() != null) {
            rateLimit.setPropertyKey(input.propertyKey());
        }

        if (input.enabled() != null) {
            rateLimit.setEnabled(input.enabled());
        }

        return aiGatewayRateLimitService.create(rateLimit);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean deleteAiGatewayRateLimit(@Argument long id) {
        aiGatewayRateLimitService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayRateLimit updateAiGatewayRateLimit(
        @Argument long id, @Argument UpdateAiGatewayRateLimitInput input) {

        AiGatewayRateLimit rateLimit = aiGatewayRateLimitService.getRateLimit(id);

        if (input.enabled() != null) {
            rateLimit.setEnabled(input.enabled());
        }

        if (input.limitType() != null) {
            rateLimit.setLimitType(input.limitType());
        }

        if (input.limitValue() != null) {
            rateLimit.setLimitValue(input.limitValue());
        }

        if (input.name() != null) {
            rateLimit.setName(input.name());
        }

        if (input.propertyKey() != null) {
            rateLimit.setPropertyKey(input.propertyKey());
        }

        if (input.scope() != null) {
            rateLimit.setScope(input.scope());
        }

        if (input.windowSeconds() != null) {
            rateLimit.setWindowSeconds(input.windowSeconds());
        }

        return aiGatewayRateLimitService.update(rateLimit);
    }

    @SuppressFBWarnings("EI")
    public record CreateAiGatewayRateLimitInput(
        Boolean enabled, AiGatewayRateLimitType limitType, int limitValue, String name,
        String projectId, String propertyKey, AiGatewayRateLimitScope scope,
        int windowSeconds, String workspaceId) {
    }

    @SuppressFBWarnings("EI")
    public record UpdateAiGatewayRateLimitInput(
        Boolean enabled, AiGatewayRateLimitType limitType, Integer limitValue, String name,
        String propertyKey, AiGatewayRateLimitScope scope, Integer windowSeconds) {
    }
}
```

- [ ] **Step 2: Create AiGatewayUserAnalyticsGraphQlController**

This controller queries traces aggregated by user_id within a time window. The aggregation is done as a JDBC query in a dedicated repository method (or inline via `JdbcTemplate`).

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayUserAnalytics;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
@SuppressFBWarnings("EI")
class AiGatewayUserAnalyticsGraphQlController {

    private static final String USER_ANALYTICS_QUERY = """
        SELECT
            user_id,
            COUNT(*) AS request_count,
            COALESCE(SUM(total_cost), 0) AS total_cost,
            COALESCE(SUM(total_input_tokens), 0) AS total_input_tokens,
            COALESCE(SUM(total_output_tokens), 0) AS total_output_tokens,
            COALESCE(AVG(total_latency_ms), 0) AS average_latency_ms,
            CASE WHEN COUNT(*) > 0
                THEN CAST(SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS DOUBLE PRECISION) / COUNT(*)
                ELSE 0
            END AS error_rate
        FROM ai_observability_trace
        WHERE workspace_id = ?
            AND user_id IS NOT NULL
            AND created_date >= ?
            AND created_date <= ?
        GROUP BY user_id
        ORDER BY request_count DESC
        """;

    private final JdbcTemplate jdbcTemplate;

    AiGatewayUserAnalyticsGraphQlController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public List<AiGatewayUserAnalytics> aiGatewayUserAnalytics(
        @Argument long workspaceId, @Argument long startDate, @Argument long endDate) {

        Instant start = Instant.ofEpochMilli(startDate);
        Instant end = Instant.ofEpochMilli(endDate);

        return jdbcTemplate.query(
            USER_ANALYTICS_QUERY,
            (ResultSet resultSet, int rowNumber) -> mapUserAnalytics(resultSet),
            workspaceId, start, end);
    }

    private AiGatewayUserAnalytics mapUserAnalytics(ResultSet resultSet) throws SQLException {
        return new AiGatewayUserAnalytics(
            resultSet.getString("user_id"),
            resultSet.getLong("request_count"),
            resultSet.getBigDecimal("total_cost"),
            resultSet.getLong("total_input_tokens"),
            resultSet.getLong("total_output_tokens"),
            resultSet.getDouble("average_latency_ms"),
            resultSet.getDouble("error_rate"));
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-graphql:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiGatewayRateLimitGraphQlController.java \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiGatewayUserAnalyticsGraphQlController.java
git commit -m "732 Add GraphQL controllers for rate limits and user analytics"
```

---

## Task 17: Client -- GraphQL Operations

**Files:**
- Create: `client/src/graphql/automation/ai-gateway/aiGatewayRateLimits.graphql`
- Create: `client/src/graphql/automation/ai-gateway/aiGatewayUserAnalytics.graphql`

- [ ] **Step 1: Create aiGatewayRateLimits.graphql**

```graphql
query aiGatewayRateLimits($workspaceId: ID!) {
    aiGatewayRateLimits(workspaceId: $workspaceId) {
        createdDate
        enabled
        id
        lastModifiedDate
        limitType
        limitValue
        name
        projectId
        propertyKey
        scope
        version
        windowSeconds
        workspaceId
    }
}

query aiGatewayRateLimit($id: ID!) {
    aiGatewayRateLimit(id: $id) {
        createdDate
        enabled
        id
        lastModifiedDate
        limitType
        limitValue
        name
        projectId
        propertyKey
        scope
        version
        windowSeconds
        workspaceId
    }
}

mutation createAiGatewayRateLimit($input: CreateAiGatewayRateLimitInput!) {
    createAiGatewayRateLimit(input: $input) {
        createdDate
        enabled
        id
        limitType
        limitValue
        name
        projectId
        propertyKey
        scope
        windowSeconds
        workspaceId
    }
}

mutation deleteAiGatewayRateLimit($id: ID!) {
    deleteAiGatewayRateLimit(id: $id)
}

mutation updateAiGatewayRateLimit($id: ID!, $input: UpdateAiGatewayRateLimitInput!) {
    updateAiGatewayRateLimit(id: $id, input: $input) {
        createdDate
        enabled
        id
        limitType
        limitValue
        name
        projectId
        propertyKey
        scope
        windowSeconds
        workspaceId
    }
}
```

- [ ] **Step 2: Create aiGatewayUserAnalytics.graphql**

```graphql
query aiGatewayUserAnalytics($workspaceId: ID!, $startDate: Long!, $endDate: Long!) {
    aiGatewayUserAnalytics(workspaceId: $workspaceId, startDate: $startDate, endDate: $endDate) {
        averageLatencyMs
        errorRate
        requestCount
        totalCost
        totalInputTokens
        totalOutputTokens
        userId
    }
}
```

- [ ] **Step 3: Run GraphQL codegen**

Run: `cd client && npx graphql-codegen`
Expected: generates updated `src/shared/middleware/graphql.ts` with new query/mutation hooks

- [ ] **Step 4: Commit**

```bash
cd client
git add src/graphql/automation/ai-gateway/aiGatewayRateLimits.graphql \
  src/graphql/automation/ai-gateway/aiGatewayUserAnalytics.graphql \
  src/shared/middleware/graphql.ts
git commit -m "732 client - Add GraphQL operations and codegen for rate limits and user analytics"
```

---

## Task 18: Client -- Types and Sidebar Update

**Files:**
- Modify: `client/src/pages/automation/ai-gateway/types.ts`
- Modify: `client/src/pages/automation/ai-gateway/AiGateway.tsx`

- [ ] **Step 1: Add types to types.ts**

Add to `client/src/pages/automation/ai-gateway/types.ts`:

```typescript
import {
    AiGatewayProjectsQuery,
    AiGatewayRateLimitsQuery,
    AiGatewayUserAnalyticsQuery,
    WorkspaceAiGatewayModelsQuery,
    WorkspaceAiGatewayProvidersQuery,
    WorkspaceAiGatewayRoutingPoliciesQuery,
} from '@/shared/middleware/graphql';

// ... existing types unchanged ...

export type AiGatewayRateLimitType = NonNullable<
    NonNullable<AiGatewayRateLimitsQuery['aiGatewayRateLimits']>[number]
>;

export type AiGatewayUserAnalyticsType = NonNullable<
    NonNullable<AiGatewayUserAnalyticsQuery['aiGatewayUserAnalytics']>[number]
>;
```

- [ ] **Step 2: Update AiGateway.tsx sidebar**

Update the type union to include `'rateLimits'`:

```typescript
type AiGatewayPageType = 'budget' | 'models' | 'monitoring' | 'projects' | 'providers' | 'rateLimits' | 'routing' | 'settings';
```

Add import for the new component:

```typescript
import AiGatewayRateLimits from './components/rate-limits/AiGatewayRateLimits';
```

Add a `LeftSidebarNavItem` entry after the "Budget" item:

```typescript
<LeftSidebarNavItem
    item={{
        current: activePage === 'rateLimits',
        name: 'Rate Limits',
        onItemClick: () => setActivePage('rateLimits'),
    }}
/>
```

Add conditional render:

```typescript
{activePage === 'rateLimits' && <AiGatewayRateLimits />}
```

- [ ] **Step 3: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/types.ts src/pages/automation/ai-gateway/AiGateway.tsx
git commit -m "732 client - Add Rate Limits sidebar tab to AI Gateway"
```

---

## Task 19: Client -- Rate Limits List Component

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/rate-limits/AiGatewayRateLimits.tsx`

- [ ] **Step 1: Create AiGatewayRateLimits.tsx**

```typescript
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import {Switch} from '@/components/ui/switch';
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@/components/ui/table';
import {
    useAiGatewayRateLimitsQuery,
    useDeleteAiGatewayRateLimitMutation,
    useUpdateAiGatewayRateLimitMutation,
} from '@/shared/middleware/graphql';
import {useWorkspaceStore} from '@/shared/stores/workspace.store';
import {PlusIcon, ShieldIcon, Trash2Icon} from 'lucide-react';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

import {AiGatewayRateLimitType} from '../../types';
import AiGatewayRateLimitDialog from './AiGatewayRateLimitDialog';

const SCOPE_LABELS: Record<string, string> = {
    GLOBAL: 'Global',
    PER_PROPERTY: 'Per Property',
    PER_USER: 'Per User',
};

const LIMIT_TYPE_LABELS: Record<string, string> = {
    COST: 'Cost',
    REQUESTS: 'Requests',
    TOKENS: 'Tokens',
};

const formatWindowSeconds = (seconds: number): string => {
    if (seconds < 60) {
        return `${seconds}s`;
    }

    if (seconds < 3600) {
        return `${Math.floor(seconds / 60)}m`;
    }

    if (seconds < 86400) {
        return `${Math.floor(seconds / 3600)}h`;
    }

    return `${Math.floor(seconds / 86400)}d`;
};

const AiGatewayRateLimits = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const queryClient = useQueryClient();

    const [dialogOpen, setDialogOpen] = useState(false);
    const [editingRateLimit, setEditingRateLimit] = useState<AiGatewayRateLimitType | undefined>();

    const {data: rateLimitsData, isLoading: rateLimitsIsLoading} = useAiGatewayRateLimitsQuery({
        workspaceId: currentWorkspaceId + '',
    });

    const deleteRateLimitMutation = useDeleteAiGatewayRateLimitMutation();
    const updateRateLimitMutation = useUpdateAiGatewayRateLimitMutation();

    const rateLimits = rateLimitsData?.aiGatewayRateLimits ?? [];

    const handleToggleEnabled = (rateLimit: AiGatewayRateLimitType) => {
        updateRateLimitMutation.mutate(
            {
                id: rateLimit.id,
                input: {enabled: !rateLimit.enabled},
            },
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['aiGatewayRateLimits']});
                },
            },
        );
    };

    const handleDelete = (rateLimitId: string) => {
        deleteRateLimitMutation.mutate(
            {id: rateLimitId},
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['aiGatewayRateLimits']});
                },
            },
        );
    };

    const handleEdit = (rateLimit: AiGatewayRateLimitType) => {
        setEditingRateLimit(rateLimit);
        setDialogOpen(true);
    };

    const handleCreate = () => {
        setEditingRateLimit(undefined);
        setDialogOpen(true);
    };

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <div className="mb-4 flex items-center justify-between">
                <h2 className="text-lg font-semibold">Rate Limits</h2>

                <Button onClick={handleCreate} size="sm">
                    <PlusIcon className="mr-1 size-4" />
                    New Rate Limit
                </Button>
            </div>

            {rateLimitsIsLoading ? (
                <PageLoader />
            ) : rateLimits.length === 0 ? (
                <EmptyList
                    icon={<ShieldIcon className="size-12 text-muted-foreground" />}
                    message="No rate limits configured yet."
                    title="No Rate Limits"
                />
            ) : (
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>Name</TableHead>
                            <TableHead>Scope</TableHead>
                            <TableHead>Type</TableHead>
                            <TableHead>Limit</TableHead>
                            <TableHead>Window</TableHead>
                            <TableHead>Enabled</TableHead>
                            <TableHead>Actions</TableHead>
                        </TableRow>
                    </TableHeader>

                    <TableBody>
                        {rateLimits.map((rateLimit) => (
                            <TableRow
                                className="cursor-pointer"
                                key={rateLimit.id}
                                onClick={() => handleEdit(rateLimit)}
                            >
                                <TableCell className="font-medium">{rateLimit.name}</TableCell>

                                <TableCell>
                                    <Badge variant="outline">
                                        {SCOPE_LABELS[rateLimit.scope] || rateLimit.scope}
                                    </Badge>

                                    {rateLimit.scope === 'PER_PROPERTY' && rateLimit.propertyKey && (
                                        <span className="ml-1 text-xs text-muted-foreground">
                                            ({rateLimit.propertyKey})
                                        </span>
                                    )}
                                </TableCell>

                                <TableCell>
                                    {LIMIT_TYPE_LABELS[rateLimit.limitType] || rateLimit.limitType}
                                </TableCell>

                                <TableCell>{rateLimit.limitValue.toLocaleString()}</TableCell>

                                <TableCell>{formatWindowSeconds(rateLimit.windowSeconds)}</TableCell>

                                <TableCell>
                                    <Switch
                                        checked={rateLimit.enabled}
                                        onClick={(event) => {
                                            event.stopPropagation();

                                            handleToggleEnabled(rateLimit);
                                        }}
                                    />
                                </TableCell>

                                <TableCell>
                                    <Button
                                        onClick={(event) => {
                                            event.stopPropagation();

                                            handleDelete(rateLimit.id);
                                        }}
                                        size="icon"
                                        variant="ghost"
                                    >
                                        <Trash2Icon className="size-4" />
                                    </Button>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            )}

            {dialogOpen && (
                <AiGatewayRateLimitDialog
                    onClose={() => {
                        setDialogOpen(false);
                        setEditingRateLimit(undefined);
                    }}
                    rateLimit={editingRateLimit}
                />
            )}
        </div>
    );
};

export default AiGatewayRateLimits;
```

- [ ] **Step 2: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/rate-limits/AiGatewayRateLimits.tsx
git commit -m "732 client - Add Rate Limits list component"
```

---

## Task 20: Client -- Rate Limit Create/Edit Dialog

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/rate-limits/AiGatewayRateLimitDialog.tsx`

- [ ] **Step 1: Create AiGatewayRateLimitDialog.tsx**

```typescript
import {Button} from '@/components/ui/button';
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import {
    AiGatewayRateLimitScope,
    AiGatewayRateLimitType as AiGatewayRateLimitTypeEnum,
    useCreateAiGatewayRateLimitMutation,
    useUpdateAiGatewayRateLimitMutation,
} from '@/shared/middleware/graphql';
import {useWorkspaceStore} from '@/shared/stores/workspace.store';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

import {AiGatewayRateLimitType} from '../../types';

interface AiGatewayRateLimitDialogProps {
    onClose: () => void;
    rateLimit?: AiGatewayRateLimitType;
}

const AiGatewayRateLimitDialog = ({onClose, rateLimit}: AiGatewayRateLimitDialogProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const queryClient = useQueryClient();

    const [limitType, setLimitType] = useState<string>(rateLimit?.limitType || 'REQUESTS');
    const [limitValue, setLimitValue] = useState<string>(rateLimit?.limitValue?.toString() || '100');
    const [name, setName] = useState(rateLimit?.name || '');
    const [propertyKey, setPropertyKey] = useState(rateLimit?.propertyKey || '');
    const [scope, setScope] = useState<string>(rateLimit?.scope || 'GLOBAL');
    const [windowSeconds, setWindowSeconds] = useState<string>(
        rateLimit?.windowSeconds?.toString() || '60',
    );

    const createMutation = useCreateAiGatewayRateLimitMutation();
    const updateMutation = useUpdateAiGatewayRateLimitMutation();

    const handleSubmit = () => {
        if (rateLimit) {
            updateMutation.mutate(
                {
                    id: rateLimit.id,
                    input: {
                        limitType: limitType as AiGatewayRateLimitTypeEnum,
                        limitValue: parseInt(limitValue),
                        name,
                        propertyKey: scope === 'PER_PROPERTY' ? propertyKey : null,
                        scope: scope as AiGatewayRateLimitScope,
                        windowSeconds: parseInt(windowSeconds),
                    },
                },
                {
                    onSuccess: () => {
                        queryClient.invalidateQueries({queryKey: ['aiGatewayRateLimits']});

                        onClose();
                    },
                },
            );
        } else {
            createMutation.mutate(
                {
                    input: {
                        limitType: limitType as AiGatewayRateLimitTypeEnum,
                        limitValue: parseInt(limitValue),
                        name,
                        propertyKey: scope === 'PER_PROPERTY' ? propertyKey : undefined,
                        scope: scope as AiGatewayRateLimitScope,
                        windowSeconds: parseInt(windowSeconds),
                        workspaceId: currentWorkspaceId + '',
                    },
                },
                {
                    onSuccess: () => {
                        queryClient.invalidateQueries({queryKey: ['aiGatewayRateLimits']});

                        onClose();
                    },
                },
            );
        }
    };

    const isEditing = !!rateLimit;

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open={true}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>{isEditing ? 'Edit Rate Limit' : 'Create Rate Limit'}</DialogTitle>
                </DialogHeader>

                <fieldset className="border-0">
                    <div className="space-y-4">
                        <div className="space-y-2">
                            <Label htmlFor="name">Name</Label>

                            <Input
                                id="name"
                                onChange={(event) => setName(event.target.value)}
                                placeholder="e.g., Global request limit"
                                value={name}
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="scope">Scope</Label>

                            <Select onValueChange={setScope} value={scope}>
                                <SelectTrigger>
                                    <SelectValue />
                                </SelectTrigger>

                                <SelectContent>
                                    <SelectItem value="GLOBAL">Global</SelectItem>
                                    <SelectItem value="PER_USER">Per User</SelectItem>
                                    <SelectItem value="PER_PROPERTY">Per Property</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>

                        {scope === 'PER_PROPERTY' && (
                            <div className="space-y-2">
                                <Label htmlFor="propertyKey">Property Key</Label>

                                <Input
                                    id="propertyKey"
                                    onChange={(event) => setPropertyKey(event.target.value)}
                                    placeholder="e.g., organization_id"
                                    value={propertyKey}
                                />
                            </div>
                        )}

                        <div className="space-y-2">
                            <Label htmlFor="limitType">Limit Type</Label>

                            <Select onValueChange={setLimitType} value={limitType}>
                                <SelectTrigger>
                                    <SelectValue />
                                </SelectTrigger>

                                <SelectContent>
                                    <SelectItem value="REQUESTS">Requests</SelectItem>
                                    <SelectItem value="TOKENS">Tokens</SelectItem>
                                    <SelectItem value="COST">Cost</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="limitValue">Limit Value</Label>

                            <Input
                                id="limitValue"
                                onChange={(event) => setLimitValue(event.target.value)}
                                type="number"
                                value={limitValue}
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="windowSeconds">Window (seconds)</Label>

                            <Input
                                id="windowSeconds"
                                onChange={(event) => setWindowSeconds(event.target.value)}
                                type="number"
                                value={windowSeconds}
                            />
                        </div>
                    </div>
                </fieldset>

                <DialogFooter>
                    <DialogClose asChild>
                        <Button variant="outline">Cancel</Button>
                    </DialogClose>

                    <Button
                        disabled={!name.trim() || !limitValue || !windowSeconds}
                        onClick={handleSubmit}
                    >
                        {isEditing ? 'Update' : 'Create'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default AiGatewayRateLimitDialog;
```

- [ ] **Step 2: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/rate-limits/AiGatewayRateLimitDialog.tsx
git commit -m "732 client - Add Rate Limit create/edit dialog"
```

---

## Task 21: Client -- Monitoring Dashboard User Filter Enhancement

**Files:**
- Modify: `client/src/pages/automation/ai-gateway/components/monitoring/AiGatewayDashboard.tsx`

- [ ] **Step 1: Add user filter dropdown and top users table**

Add a user filter dropdown at the top of the monitoring dashboard that populates from the `aiGatewayUserAnalytics` query. When a user is selected, filter the existing request log data by that user ID (requires adding `userId` to the request log query/schema if not already present, or filtering client-side from traces).

Add a "Top Users" table widget showing the top users by request count, with columns: User ID, Requests, Cost, Tokens, Avg Latency, Error Rate.

Key changes to `AiGatewayDashboard.tsx`:

1. Import `useAiGatewayUserAnalyticsQuery` from generated GraphQL
2. Add `selectedUserId` state
3. Fetch user analytics for the current time range
4. Add a `Select` dropdown above the existing stat cards:

```typescript
const [selectedUserId, setSelectedUserId] = useState<string>('all');

const {data: userAnalyticsData} = useAiGatewayUserAnalyticsQuery({
    endDate: endDate + '',
    startDate: startDate + '',
    workspaceId: currentWorkspaceId + '',
});

const userAnalytics = userAnalyticsData?.aiGatewayUserAnalytics ?? [];
```

```tsx
<div className="mb-4 flex items-center gap-2">
    <Label>User:</Label>

    <Select onValueChange={setSelectedUserId} value={selectedUserId}>
        <SelectTrigger className="w-48">
            <SelectValue placeholder="All Users" />
        </SelectTrigger>

        <SelectContent>
            <SelectItem value="all">All Users</SelectItem>

            {userAnalytics.map((userAnalyticsEntry) => (
                <SelectItem key={userAnalyticsEntry.userId} value={userAnalyticsEntry.userId}>
                    {userAnalyticsEntry.userId}
                </SelectItem>
            ))}
        </SelectContent>
    </Select>
</div>
```

5. Add a "Top Users" table at the bottom of the dashboard:

```tsx
{userAnalytics.length > 0 && (
    <div className="mt-6">
        <h3 className="mb-3 text-base font-semibold">Top Users</h3>

        <Table>
            <TableHeader>
                <TableRow>
                    <TableHead>User ID</TableHead>
                    <TableHead>Requests</TableHead>
                    <TableHead>Cost</TableHead>
                    <TableHead>Tokens</TableHead>
                    <TableHead>Avg Latency</TableHead>
                    <TableHead>Error Rate</TableHead>
                </TableRow>
            </TableHeader>

            <TableBody>
                {userAnalytics.slice(0, 10).map((userAnalyticsEntry) => (
                    <TableRow key={userAnalyticsEntry.userId}>
                        <TableCell className="font-medium">{userAnalyticsEntry.userId}</TableCell>
                        <TableCell>{userAnalyticsEntry.requestCount.toLocaleString()}</TableCell>
                        <TableCell>${userAnalyticsEntry.totalCost.toFixed(4)}</TableCell>
                        <TableCell>
                            {(userAnalyticsEntry.totalInputTokens + userAnalyticsEntry.totalOutputTokens).toLocaleString()}
                        </TableCell>
                        <TableCell>{userAnalyticsEntry.averageLatencyMs.toFixed(0)}ms</TableCell>
                        <TableCell>{(userAnalyticsEntry.errorRate * 100).toFixed(1)}%</TableCell>
                    </TableRow>
                ))}
            </TableBody>
        </Table>
    </div>
)}
```

- [ ] **Step 2: Run checks**

Run: `cd client && npm run check`
Expected: lint + typecheck pass

- [ ] **Step 3: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/monitoring/AiGatewayDashboard.tsx
git commit -m "732 client - Add user filter and top users table to monitoring dashboard"
```

---

## Task 22: Unit Tests -- InMemoryAiGatewayRateLimiter

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/test/java/com/bytechef/ee/automation/ai/gateway/ratelimit/InMemoryAiGatewayRateLimiterTest.java`

- [ ] **Step 1: Create InMemoryAiGatewayRateLimiterTest**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayRateLimitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class InMemoryAiGatewayRateLimiterTest {

    private InMemoryAiGatewayRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new InMemoryAiGatewayRateLimiter();
    }

    @Test
    void testTryAcquireAllowsWithinLimit() {
        AiGatewayRateLimitResult result = rateLimiter.tryAcquire("test-key", 5, 60);

        assertTrue(result.allowed());
        assertEquals(4, result.remaining());
    }

    @Test
    void testTryAcquireDeniesWhenLimitExceeded() {
        for (int iteration = 0; iteration < 5; iteration++) {
            rateLimiter.tryAcquire("test-key", 5, 60);
        }

        AiGatewayRateLimitResult result = rateLimiter.tryAcquire("test-key", 5, 60);

        assertFalse(result.allowed());
        assertEquals(0, result.remaining());
    }

    @Test
    void testTryAcquireIsolatesKeys() {
        for (int iteration = 0; iteration < 5; iteration++) {
            rateLimiter.tryAcquire("key-a", 5, 60);
        }

        AiGatewayRateLimitResult result = rateLimiter.tryAcquire("key-b", 5, 60);

        assertTrue(result.allowed());
    }

    @Test
    void testResetClearsCounter() {
        for (int iteration = 0; iteration < 5; iteration++) {
            rateLimiter.tryAcquire("test-key", 5, 60);
        }

        rateLimiter.reset("test-key");

        AiGatewayRateLimitResult result = rateLimiter.tryAcquire("test-key", 5, 60);

        assertTrue(result.allowed());
        assertEquals(4, result.remaining());
    }

    @Test
    void testTryAcquireReturnsResetEpochSeconds() {
        AiGatewayRateLimitResult result = rateLimiter.tryAcquire("test-key", 5, 120);

        long expectedMinReset = (System.currentTimeMillis() / 1000) + 119;
        long expectedMaxReset = (System.currentTimeMillis() / 1000) + 121;

        assertTrue(result.resetEpochSeconds() >= expectedMinReset);
        assertTrue(result.resetEpochSeconds() <= expectedMaxReset);
    }
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:test --tests "com.bytechef.ee.automation.ai.gateway.ratelimit.InMemoryAiGatewayRateLimiterTest"`
Expected: All tests pass

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/test/java/com/bytechef/ee/automation/ai/gateway/ratelimit/InMemoryAiGatewayRateLimiterTest.java
git commit -m "732 Add unit tests for InMemoryAiGatewayRateLimiter"
```

---

## Task 23: Unit Tests -- AiGatewayRateLimitChecker

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/test/java/com/bytechef/ee/automation/ai/gateway/ratelimit/AiGatewayRateLimitCheckerTest.java`

- [ ] **Step 1: Create AiGatewayRateLimitCheckerTest**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.ratelimit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimit;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimitScope;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimitType;
import com.bytechef.ee.automation.ai.gateway.domain.RateLimitExceededException;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayPropertyHeaders;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayRateLimitResult;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayRateLimitService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiGatewayRateLimitCheckerTest {

    @Mock
    private AiGatewayRateLimiter aiGatewayRateLimiter;

    @Mock
    private AiGatewayRateLimitService aiGatewayRateLimitService;

    private AiGatewayRateLimitChecker aiGatewayRateLimitChecker;

    @BeforeEach
    void setUp() {
        aiGatewayRateLimitChecker = new AiGatewayRateLimitChecker(
            aiGatewayRateLimiter, aiGatewayRateLimitService);
    }

    @Test
    void testCheckPreRequestLimitsAllowsWhenUnderLimit() {
        AiGatewayRateLimit rule = new AiGatewayRateLimit(
            1L, "Test Rule", AiGatewayRateLimitScope.GLOBAL,
            AiGatewayRateLimitType.REQUESTS, 100, 60);

        when(aiGatewayRateLimitService.getEnabledRateLimitsByWorkspace(1L))
            .thenReturn(List.of(rule));

        when(aiGatewayRateLimiter.tryAcquire(anyString(), anyInt(), anyInt()))
            .thenReturn(AiGatewayRateLimitResult.allowed(99, 1000L));

        AiGatewayPropertyHeaders headers = new AiGatewayPropertyHeaders(null, Map.of());

        assertDoesNotThrow(() -> aiGatewayRateLimitChecker.checkPreRequestLimits(1L, headers));
    }

    @Test
    void testCheckPreRequestLimitsThrowsWhenExceeded() {
        AiGatewayRateLimit rule = new AiGatewayRateLimit(
            1L, "Test Rule", AiGatewayRateLimitScope.GLOBAL,
            AiGatewayRateLimitType.REQUESTS, 100, 60);

        when(aiGatewayRateLimitService.getEnabledRateLimitsByWorkspace(1L))
            .thenReturn(List.of(rule));

        when(aiGatewayRateLimiter.tryAcquire(anyString(), anyInt(), anyInt()))
            .thenReturn(AiGatewayRateLimitResult.denied(0, 1000L));

        AiGatewayPropertyHeaders headers = new AiGatewayPropertyHeaders(null, Map.of());

        assertThrows(
            RateLimitExceededException.class,
            () -> aiGatewayRateLimitChecker.checkPreRequestLimits(1L, headers));
    }

    @Test
    void testCheckPreRequestLimitsSkipsPerUserWithoutUserId() {
        AiGatewayRateLimit rule = new AiGatewayRateLimit(
            1L, "Per User Rule", AiGatewayRateLimitScope.PER_USER,
            AiGatewayRateLimitType.REQUESTS, 100, 60);

        when(aiGatewayRateLimitService.getEnabledRateLimitsByWorkspace(1L))
            .thenReturn(List.of(rule));

        AiGatewayPropertyHeaders headers = new AiGatewayPropertyHeaders(null, Map.of());

        assertDoesNotThrow(() -> aiGatewayRateLimitChecker.checkPreRequestLimits(1L, headers));
    }

    @Test
    void testCheckPreRequestLimitsSkipsTokenAndCostRules() {
        AiGatewayRateLimit tokenRule = new AiGatewayRateLimit(
            1L, "Token Rule", AiGatewayRateLimitScope.GLOBAL,
            AiGatewayRateLimitType.TOKENS, 10000, 60);

        AiGatewayRateLimit costRule = new AiGatewayRateLimit(
            1L, "Cost Rule", AiGatewayRateLimitScope.GLOBAL,
            AiGatewayRateLimitType.COST, 100, 60);

        when(aiGatewayRateLimitService.getEnabledRateLimitsByWorkspace(1L))
            .thenReturn(List.of(tokenRule, costRule));

        AiGatewayPropertyHeaders headers = new AiGatewayPropertyHeaders(null, Map.of());

        assertDoesNotThrow(() -> aiGatewayRateLimitChecker.checkPreRequestLimits(1L, headers));
    }
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:test --tests "com.bytechef.ee.automation.ai.gateway.ratelimit.AiGatewayRateLimitCheckerTest"`
Expected: All tests pass

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/test/java/com/bytechef/ee/automation/ai/gateway/ratelimit/AiGatewayRateLimitCheckerTest.java
git commit -m "732 Add unit tests for AiGatewayRateLimitChecker"
```

---

## Task 24: Final Verification

- [ ] **Step 1: Run full server compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api:compileJava && ./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava && ./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-graphql:compileJava && ./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-public-rest:compileJava`
Expected: BUILD SUCCESSFUL for all four modules

- [ ] **Step 2: Run server formatting**

Run: `./gradlew spotlessApply`
Expected: BUILD SUCCESSFUL (may format some files)

- [ ] **Step 3: Run server checks**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:check`
Expected: BUILD SUCCESSFUL (checkstyle, PMD, SpotBugs pass)

- [ ] **Step 4: Run all service module tests**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:test`
Expected: All tests pass

- [ ] **Step 5: Run client checks**

Run: `cd client && npm run check`
Expected: lint + typecheck + tests pass

- [ ] **Step 6: Commit any formatting changes**

```bash
git add -u
git commit -m "732 Apply spotless formatting for Phase 5 rate limiting"
```
