# LLM API Gateway Implementation Plan

> **Status (2026-04-13):** Phases 1–6 of this plan ship in the module at `server/ee/libs/automation/automation-ai/automation-ai-gateway/` (renamed from `automation-ai-llm-gateway`). Subsequent gap-closure work, deviations, and residual items are tracked in `docs/superpowers/plans/2026-04-12-phase8-gap-remediation.md` — that file is the source of truth for what remains. **Known WONTFIX items**:
>
> - **F4 Gateway API Key Management**: reuses the platform `ApiKey` entity via `AiGatewayApiKeyAuthenticationProvider`. No dedicated `AiGatewayApiKey` table/entity/UI. See spec §8 "Deviations".
> - **Module name**: shipped as `automation-ai-gateway`, not `automation-ai-llm-gateway` as originally planned.
> - **Workspace-level configuration overrides**: persisted via the platform `Property` store (scope=WORKSPACE), not a dedicated `ai_gateway_workspace_settings` table.
> - **Budget hard-limit response**: 402 Payment Required (not 429 Too Many Requests).

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Build an LLM API Gateway under `automation-ai-llm-gateway` that provides a unified OpenAI-compatible API, intelligent routing, failover, spend tracking, caching, and a web UI for configuration and monitoring.

**Architecture:** Spring Boot + Spring AI backend with Spring Data JDBC persistence, GraphQL for internal UI, REST for public gateway API. React frontend with shadcn/ui and shadcn charts. Follows existing ByteChef module patterns (api/service/graphql/rest submodules). Reuses existing API key infrastructure for gateway authentication.

**Tech Stack:** Java 25, Spring Boot 4, Spring AI 2.0, Spring Data JDBC, PostgreSQL, Redis (caching), React 19, TypeScript, shadcn/ui, shadcn charts (Recharts)

---

## Phase 1 — Foundation: Module Structure, Providers, Models, Basic Gateway

### Task 1.1: Create Gradle Module Structure

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/build.gradle.kts`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/build.gradle.kts`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-rest/build.gradle.kts`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-graphql/build.gradle.kts`
- Modify: `settings.gradle.kts` (after the `automation-ai-mcp-server` line)

- [x] **Step 1: Create API module build.gradle.kts**

```kotlin
dependencies {
    api("org.springframework.data:spring-data-commons")

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:encryption:encryption-api"))
}
```

- [x] **Step 2: Create service module build.gradle.kts**

```kotlin
dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.ai:spring-ai-client-chat")
    implementation("org.springframework.ai:spring-ai-model")
    implementation("org.springframework.ai:spring-ai-openai")
    implementation("org.springframework.ai:spring-ai-anthropic")
    implementation("org.springframework.ai:spring-ai-vertex-ai-gemini")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:automation:automation-ai:automation-ai-llm-gateway:automation-ai-llm-gateway-api"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:encryption:encryption-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-security:platform-security-api"))
    implementation(project(":server:libs:platform:platform-security-web:platform-security-web-api"))
}
```

- [x] **Step 3: Create REST module build.gradle.kts**

```kotlin
dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:automation:automation-ai:automation-ai-llm-gateway:automation-ai-llm-gateway-api"))
    implementation(project(":server:libs:platform:platform-security:platform-security-api"))
    implementation(project(":server:libs:platform:platform-security-web:platform-security-web-api"))
}
```

- [x] **Step 4: Create GraphQL module build.gradle.kts**

```kotlin
dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.graphql:spring-graphql")
    implementation(project(":server:libs:automation:automation-ai:automation-ai-llm-gateway:automation-ai-llm-gateway-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-api"))
}
```

- [x] **Step 5: Register modules in settings.gradle.kts**

Add after the `include("server:libs:automation:automation-ai:automation-ai-mcp-server")` line:

```kotlin
include("server:libs:automation:automation-ai:automation-ai-llm-gateway:automation-ai-llm-gateway-api")
include("server:libs:automation:automation-ai:automation-ai-llm-gateway:automation-ai-llm-gateway-graphql")
include("server:libs:automation:automation-ai:automation-ai-llm-gateway:automation-ai-llm-gateway-rest")
include("server:libs:automation:automation-ai:automation-ai-llm-gateway:automation-ai-llm-gateway-service")
```

- [x] **Step 6: Verify build compiles**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-llm-gateway:automation-ai-llm-gateway-api:compileJava`
Expected: BUILD SUCCESSFUL

- [x] **Step 7: Commit**

```bash
git add settings.gradle.kts server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/
git commit -m "Add automation-ai-llm-gateway module structure"
```

---

### Task 1.2: Provider Domain Model, Repository, and Service Interface

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/domain/AiLlmGatewayProvider.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/domain/AiLlmGatewayProviderType.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/repository/AiLlmGatewayProviderRepository.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/service/AiLlmGatewayProviderService.java`

- [x] **Step 1: Create AiLlmGatewayProviderType enum**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * ...
 */

package com.bytechef.automation.ai.llmgateway.domain;

public enum AiLlmGatewayProviderType {

    ANTHROPIC,
    AZURE_OPENAI,
    COHERE,
    DEEPSEEK,
    GOOGLE_GEMINI,
    GROQ,
    MISTRAL,
    OPENAI;
}
```

- [x] **Step 2: Create AiLlmGatewayProvider domain entity**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * ...
 */

package com.bytechef.automation.ai.llmgateway.domain;

import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("ai_llm_gateway_provider")
public class AiLlmGatewayProvider {

    @Column("api_key")
    private String apiKey;

    @Column("base_url")
    private String baseUrl;

    @Column("config")
    private String config;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("enabled")
    private boolean enabled;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column("name")
    private String name;

    @Column("type")
    private AiLlmGatewayProviderType type;

    @Version
    private int version;

    private AiLlmGatewayProvider() {
    }

    public AiLlmGatewayProvider(String name, AiLlmGatewayProviderType type, String apiKey) {
        this.name = name;
        this.type = type;
        this.apiKey = apiKey;
        this.enabled = true;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getConfig() {
        return config;
    }

    public String getCreatedBy() {
        return createdBy;
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

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getName() {
        return name;
    }

    public AiLlmGatewayProviderType getType() {
        return type;
    }

    public int getVersion() {
        return version;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(AiLlmGatewayProviderType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        AiLlmGatewayProvider provider = (AiLlmGatewayProvider) object;

        return Objects.equals(id, provider.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AiLlmGatewayProvider{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", type=" + type +
            ", enabled=" + enabled +
            ", createdDate=" + createdDate +
            ", version=" + version +
            '}';
    }
}
```

- [x] **Step 3: Create repository interface**

```java
package com.bytechef.automation.ai.llmgateway.repository;

import com.bytechef.automation.ai.llmgateway.domain.AiLlmGatewayProvider;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

public interface AiLlmGatewayProviderRepository extends ListCrudRepository<AiLlmGatewayProvider, Long> {

    List<AiLlmGatewayProvider> findAllByEnabled(boolean enabled);
}
```

- [x] **Step 4: Create service interface**

```java
package com.bytechef.automation.ai.llmgateway.service;

import com.bytechef.automation.ai.llmgateway.domain.AiLlmGatewayProvider;
import java.util.List;

public interface AiLlmGatewayProviderService {

    AiLlmGatewayProvider create(AiLlmGatewayProvider provider);

    void delete(long id);

    AiLlmGatewayProvider getProvider(long id);

    List<AiLlmGatewayProvider> getProviders();

    List<AiLlmGatewayProvider> getEnabledProviders();

    AiLlmGatewayProvider update(AiLlmGatewayProvider provider);
}
```

- [x] **Step 5: Verify compilation**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-llm-gateway:automation-ai-llm-gateway-api:compileJava`

- [x] **Step 6: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/
git commit -m "Add AiLlmGatewayProvider domain model, repository, and service interface"
```

---

### Task 1.3: Model Domain, Repository, and Service Interface

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/domain/AiLlmGatewayModel.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/repository/AiLlmGatewayModelRepository.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/service/AiLlmGatewayModelService.java`

- [x] **Step 1: Create AiLlmGatewayModel entity**

```java
package com.bytechef.automation.ai.llmgateway.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("ai_llm_gateway_model")
public class AiLlmGatewayModel {

    @Column("alias")
    private String alias;

    @Column("capabilities")
    private String capabilities;

    @Column("context_window")
    private Integer contextWindow;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("enabled")
    private boolean enabled;

    @Id
    private Long id;

    @Column("input_cost_per_m_tokens")
    private BigDecimal inputCostPerMTokens;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column("name")
    private String name;

    @Column("output_cost_per_m_tokens")
    private BigDecimal outputCostPerMTokens;

    @Column("provider_id")
    private Long providerId;

    @Version
    private int version;

    private AiLlmGatewayModel() {
    }

    public AiLlmGatewayModel(Long providerId, String name) {
        this.providerId = providerId;
        this.name = name;
        this.enabled = true;
    }

    // All getters
    public String getAlias() { return alias; }
    public String getCapabilities() { return capabilities; }
    public Integer getContextWindow() { return contextWindow; }
    public Instant getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
    public Long getId() { return id; }
    public BigDecimal getInputCostPerMTokens() { return inputCostPerMTokens; }
    public Instant getLastModifiedDate() { return lastModifiedDate; }
    public String getName() { return name; }
    public BigDecimal getOutputCostPerMTokens() { return outputCostPerMTokens; }
    public Long getProviderId() { return providerId; }
    public int getVersion() { return version; }

    // Setters for mutable fields
    public void setAlias(String alias) { this.alias = alias; }
    public void setCapabilities(String capabilities) { this.capabilities = capabilities; }
    public void setContextWindow(Integer contextWindow) { this.contextWindow = contextWindow; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setInputCostPerMTokens(BigDecimal inputCostPerMTokens) { this.inputCostPerMTokens = inputCostPerMTokens; }
    public void setName(String name) { this.name = name; }
    public void setOutputCostPerMTokens(BigDecimal outputCostPerMTokens) { this.outputCostPerMTokens = outputCostPerMTokens; }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        AiLlmGatewayModel model = (AiLlmGatewayModel) object;
        return Objects.equals(id, model.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AiLlmGatewayModel{" +
            "id=" + id +
            ", providerId=" + providerId +
            ", name='" + name + '\'' +
            ", alias='" + alias + '\'' +
            ", enabled=" + enabled +
            '}';
    }
}
```

- [x] **Step 2: Create repository**

```java
package com.bytechef.automation.ai.llmgateway.repository;

import com.bytechef.automation.ai.llmgateway.domain.AiLlmGatewayModel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

public interface AiLlmGatewayModelRepository extends ListCrudRepository<AiLlmGatewayModel, Long> {

    List<AiLlmGatewayModel> findAllByProviderId(Long providerId);

    List<AiLlmGatewayModel> findAllByEnabled(boolean enabled);

    Optional<AiLlmGatewayModel> findByProviderIdAndName(Long providerId, String name);
}
```

- [x] **Step 3: Create service interface**

```java
package com.bytechef.automation.ai.llmgateway.service;

import com.bytechef.automation.ai.llmgateway.domain.AiLlmGatewayModel;
import java.util.List;

public interface AiLlmGatewayModelService {

    AiLlmGatewayModel create(AiLlmGatewayModel model);

    void delete(long id);

    AiLlmGatewayModel getModel(long id);

    AiLlmGatewayModel getModel(long providerId, String name);

    List<AiLlmGatewayModel> getModels();

    List<AiLlmGatewayModel> getModelsByProviderId(long providerId);

    List<AiLlmGatewayModel> getEnabledModels();

    AiLlmGatewayModel update(AiLlmGatewayModel model);
}
```

- [x] **Step 4: Verify compilation and commit**

```bash
./gradlew :server:libs:automation:automation-ai:automation-ai-llm-gateway:automation-ai-llm-gateway-api:compileJava
git add server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/
git commit -m "Add AiLlmGatewayModel domain model, repository, and service interface"
```

---

### Task 1.4: Liquibase Migrations

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_llm_gateway/00000000000001_ai_llm_gateway_init.xml`
- Modify: `server/libs/config/liquibase-config/src/main/resources/config/liquibase/master.xml`

- [x] **Step 1: Create init changelog**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">
    <changeSet id="00000000000001" author="Ivica Cardic">
        <preConditions onFail="MARK_RAN">
            <not>
                <tableExists tableName="ai_llm_gateway_provider"/>
            </not>
        </preConditions>

        <createTable tableName="ai_llm_gateway_provider">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="name" type="VARCHAR(256)">
                <constraints nullable="false"/>
            </column>
            <column name="type" type="VARCHAR(64)">
                <constraints nullable="false"/>
            </column>
            <column name="api_key" type="VARCHAR(1024)">
                <constraints nullable="false"/>
            </column>
            <column name="base_url" type="VARCHAR(512)"/>
            <column name="enabled" type="BOOLEAN" defaultValueBoolean="true">
                <constraints nullable="false"/>
            </column>
            <column name="config" type="TEXT"/>
            <column name="created_by" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="created_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="last_modified_by" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="last_modified_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="version" type="BIGINT">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <createTable tableName="ai_llm_gateway_model">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="provider_id" type="BIGINT">
                <constraints nullable="false" foreignKeyName="fk_ai_llm_gateway_model_provider" references="ai_llm_gateway_provider(id)"/>
            </column>
            <column name="name" type="VARCHAR(256)">
                <constraints nullable="false"/>
            </column>
            <column name="alias" type="VARCHAR(256)"/>
            <column name="context_window" type="INT"/>
            <column name="input_cost_per_m_tokens" type="DECIMAL(10,4)"/>
            <column name="output_cost_per_m_tokens" type="DECIMAL(10,4)"/>
            <column name="capabilities" type="VARCHAR(256)"/>
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
    </changeSet>
</databaseChangeLog>
```

- [x] **Step 2: Add to master.xml**

Add after the `automation/mcp/` includeAll line in `master.xml`:

```xml
    <includeAll path="classpath:config/liquibase/changelog/automation/ai_llm_gateway/" relativeToChangelogFile="false" errorIfMissingOrEmpty="false" contextFilter="mono or configuration or multitenant" />
```

- [x] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/resources/ server/libs/config/liquibase-config/
git commit -m "Add Liquibase migrations for ai_llm_gateway_provider and ai_llm_gateway_model tables"
```

---

### Task 1.5: Provider and Model Service Implementations

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/java/com/bytechef/ee/automation/ai/llmgateway/service/AiLlmGatewayProviderServiceImpl.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/java/com/bytechef/ee/automation/ai/llmgateway/service/AiLlmGatewayModelServiceImpl.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/java/com/bytechef/ee/automation/ai/llmgateway/config/AiLlmGatewayJdbcRepositoryConfiguration.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- [x] **Step 1: Create JDBC repository configuration**

```java
package com.bytechef.automation.ai.llmgateway.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@AutoConfiguration
@ConditionalOnBean(JdbcMappingContext.class)
@EnableJdbcRepositories(basePackages = "com.bytechef.automation.ai.llmgateway.repository")
class AiLlmGatewayJdbcRepositoryConfiguration {
}
```

- [x] **Step 2: Create AutoConfiguration.imports**

```
com.bytechef.automation.ai.llmgateway.config.AiLlmGatewayJdbcRepositoryConfiguration
```

- [x] **Step 3: Create AiLlmGatewayProviderServiceImpl**

```java
package com.bytechef.automation.ai.llmgateway.service;

import com.bytechef.automation.ai.llmgateway.domain.AiLlmGatewayProvider;
import com.bytechef.automation.ai.llmgateway.repository.AiLlmGatewayProviderRepository;
import com.bytechef.encryption.Encryption;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AiLlmGatewayProviderServiceImpl implements AiLlmGatewayProviderService {

    private final AiLlmGatewayProviderRepository aiLlmGatewayProviderRepository;
    private final Encryption encryption;

    public AiLlmGatewayProviderServiceImpl(
        AiLlmGatewayProviderRepository aiLlmGatewayProviderRepository, Encryption encryption) {

        this.aiLlmGatewayProviderRepository = aiLlmGatewayProviderRepository;
        this.encryption = encryption;
    }

    @Override
    public AiLlmGatewayProvider create(AiLlmGatewayProvider provider) {
        Validate.notNull(provider, "'provider' must not be null");
        Validate.isTrue(provider.getId() == null, "'id' must be null");

        provider.setApiKey(encryption.encrypt(provider.getApiKey()));

        return aiLlmGatewayProviderRepository.save(provider);
    }

    @Override
    public void delete(long id) {
        aiLlmGatewayProviderRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiLlmGatewayProvider getProvider(long id) {
        return aiLlmGatewayProviderRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiLlmGatewayProvider> getProviders() {
        return aiLlmGatewayProviderRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiLlmGatewayProvider> getEnabledProviders() {
        return aiLlmGatewayProviderRepository.findAllByEnabled(true);
    }

    @Override
    public AiLlmGatewayProvider update(AiLlmGatewayProvider provider) {
        Validate.notNull(provider, "'provider' must not be null");

        AiLlmGatewayProvider existingProvider = getProvider(provider.getId());

        existingProvider.setName(provider.getName());
        existingProvider.setType(provider.getType());
        existingProvider.setBaseUrl(provider.getBaseUrl());
        existingProvider.setConfig(provider.getConfig());
        existingProvider.setEnabled(provider.isEnabled());

        if (provider.getApiKey() != null && !provider.getApiKey().isEmpty()) {
            existingProvider.setApiKey(encryption.encrypt(provider.getApiKey()));
        }

        return aiLlmGatewayProviderRepository.save(existingProvider);
    }
}
```

- [x] **Step 4: Create AiLlmGatewayModelServiceImpl**

```java
package com.bytechef.automation.ai.llmgateway.service;

import com.bytechef.automation.ai.llmgateway.domain.AiLlmGatewayModel;
import com.bytechef.automation.ai.llmgateway.repository.AiLlmGatewayModelRepository;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AiLlmGatewayModelServiceImpl implements AiLlmGatewayModelService {

    private final AiLlmGatewayModelRepository aiLlmGatewayModelRepository;

    public AiLlmGatewayModelServiceImpl(AiLlmGatewayModelRepository aiLlmGatewayModelRepository) {
        this.aiLlmGatewayModelRepository = aiLlmGatewayModelRepository;
    }

    @Override
    public AiLlmGatewayModel create(AiLlmGatewayModel model) {
        Validate.notNull(model, "'model' must not be null");
        Validate.isTrue(model.getId() == null, "'id' must be null");

        return aiLlmGatewayModelRepository.save(model);
    }

    @Override
    public void delete(long id) {
        aiLlmGatewayModelRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiLlmGatewayModel getModel(long id) {
        return aiLlmGatewayModelRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Model not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public AiLlmGatewayModel getModel(long providerId, String name) {
        return aiLlmGatewayModelRepository.findByProviderIdAndName(providerId, name)
            .orElseThrow(() -> new IllegalArgumentException(
                "Model not found: providerId=" + providerId + ", name=" + name));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiLlmGatewayModel> getModels() {
        return aiLlmGatewayModelRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiLlmGatewayModel> getModelsByProviderId(long providerId) {
        return aiLlmGatewayModelRepository.findAllByProviderId(providerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiLlmGatewayModel> getEnabledModels() {
        return aiLlmGatewayModelRepository.findAllByEnabled(true);
    }

    @Override
    public AiLlmGatewayModel update(AiLlmGatewayModel model) {
        Validate.notNull(model, "'model' must not be null");

        AiLlmGatewayModel existingModel = getModel(model.getId());

        existingModel.setAlias(model.getAlias());
        existingModel.setCapabilities(model.getCapabilities());
        existingModel.setContextWindow(model.getContextWindow());
        existingModel.setEnabled(model.isEnabled());
        existingModel.setInputCostPerMTokens(model.getInputCostPerMTokens());
        existingModel.setName(model.getName());
        existingModel.setOutputCostPerMTokens(model.getOutputCostPerMTokens());

        return aiLlmGatewayModelRepository.save(existingModel);
    }
}
```

- [x] **Step 5: Verify compilation and commit**

```bash
./gradlew :server:libs:automation:automation-ai:automation-ai-llm-gateway:automation-ai-llm-gateway-service:compileJava
git add server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/
git commit -m "Add provider and model service implementations with encryption and JDBC config"
```

---

### Task 1.6: ChatModel Factory

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/java/com/bytechef/ee/automation/ai/llmgateway/provider/AiLlmGatewayChatModelFactory.java`

- [x] **Step 1: Create ChatModelFactory**

```java
package com.bytechef.automation.ai.llmgateway.provider;

import com.bytechef.automation.ai.llmgateway.domain.AiLlmGatewayProvider;
import com.bytechef.automation.ai.llmgateway.domain.AiLlmGatewayProviderType;
import com.bytechef.encryption.Encryption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

@Component
public class AiLlmGatewayChatModelFactory {

    private final Map<Long, ChatModel> chatModelCache = new ConcurrentHashMap<>();
    private final Encryption encryption;

    public AiLlmGatewayChatModelFactory(Encryption encryption) {
        this.encryption = encryption;
    }

    public ChatModel getChatModel(AiLlmGatewayProvider provider) {
        return chatModelCache.computeIfAbsent(provider.getId(), providerId -> createChatModel(provider));
    }

    public void evict(long providerId) {
        chatModelCache.remove(providerId);
    }

    public void evictAll() {
        chatModelCache.clear();
    }

    private ChatModel createChatModel(AiLlmGatewayProvider provider) {
        String decryptedApiKey = encryption.decrypt(provider.getApiKey());

        return switch (provider.getType()) {
            case OPENAI, DEEPSEEK, GROQ, MISTRAL -> createOpenAiCompatibleChatModel(
                decryptedApiKey, provider.getBaseUrl(), provider.getType());
            case ANTHROPIC -> createAnthropicChatModel(decryptedApiKey, provider.getBaseUrl());
            case GOOGLE_GEMINI -> createOpenAiCompatibleChatModel(
                decryptedApiKey, provider.getBaseUrl(), provider.getType());
            case AZURE_OPENAI -> createOpenAiCompatibleChatModel(
                decryptedApiKey, provider.getBaseUrl(), provider.getType());
            case COHERE -> createOpenAiCompatibleChatModel(
                decryptedApiKey, provider.getBaseUrl(), provider.getType());
        };
    }

    private ChatModel createOpenAiCompatibleChatModel(
        String apiKey, String baseUrl, AiLlmGatewayProviderType type) {

        String resolvedBaseUrl = resolveBaseUrl(baseUrl, type);

        OpenAiApi openAiApi = OpenAiApi.builder()
            .apiKey(apiKey)
            .baseUrl(resolvedBaseUrl)
            .build();

        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .build();
    }

    private ChatModel createAnthropicChatModel(String apiKey, String baseUrl) {
        AnthropicApi.Builder anthropicApiBuilder = AnthropicApi.builder()
            .apiKey(apiKey);

        if (baseUrl != null && !baseUrl.isEmpty()) {
            anthropicApiBuilder.baseUrl(baseUrl);
        }

        return AnthropicChatModel.builder()
            .anthropicApi(anthropicApiBuilder.build())
            .build();
    }

    private String resolveBaseUrl(String baseUrl, AiLlmGatewayProviderType type) {
        if (baseUrl != null && !baseUrl.isEmpty()) {
            return baseUrl;
        }

        return switch (type) {
            case OPENAI -> "https://api.openai.com";
            case DEEPSEEK -> "https://api.deepseek.com";
            case GROQ -> "https://api.groq.com/openai";
            case MISTRAL -> "https://api.mistral.ai";
            case COHERE -> "https://api.cohere.com/compatibility";
            case GOOGLE_GEMINI -> "https://generativelanguage.googleapis.com";
            default -> throw new IllegalArgumentException("No default base URL for: " + type);
        };
    }
}
```

- [x] **Step 2: Verify and commit**

```bash
./gradlew :server:libs:automation:automation-ai:automation-ai-llm-gateway:automation-ai-llm-gateway-service:compileJava
git add server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/
git commit -m "Add AiLlmGatewayChatModelFactory with provider-specific ChatModel creation"
```

---

### Task 1.7: Gateway Facade and Chat Completions Logic

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/java/com/bytechef/ee/automation/ai/llmgateway/facade/AiLlmGatewayFacade.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/dto/AiLlmGatewayChatCompletionRequest.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/dto/AiLlmGatewayChatCompletionResponse.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/dto/AiLlmGatewayChatMessage.java`

- [x] **Step 1: Create request/response DTOs in API module**

`AiLlmGatewayChatMessage.java`:
```java
package com.bytechef.automation.ai.llmgateway.dto;

public record AiLlmGatewayChatMessage(String role, String content) {
}
```

`AiLlmGatewayChatCompletionRequest.java`:
```java
package com.bytechef.automation.ai.llmgateway.dto;

import java.util.List;

public record AiLlmGatewayChatCompletionRequest(
    String model,
    List<AiLlmGatewayChatMessage> messages,
    Double temperature,
    Integer maxTokens,
    Double topP,
    boolean stream,
    String routingPolicy,
    Boolean cache) {
}
```

`AiLlmGatewayChatCompletionResponse.java`:
```java
package com.bytechef.automation.ai.llmgateway.dto;

import java.util.List;

public record AiLlmGatewayChatCompletionResponse(
    String id,
    String object,
    long created,
    String model,
    List<Choice> choices,
    Usage usage) {

    public record Choice(int index, AiLlmGatewayChatMessage message, String finishReason) {
    }

    public record Usage(int promptTokens, int completionTokens, int totalTokens) {
    }
}
```

- [x] **Step 2: Create AiLlmGatewayFacade**

```java
package com.bytechef.automation.ai.llmgateway.facade;

import com.bytechef.automation.ai.llmgateway.domain.AiLlmGatewayModel;
import com.bytechef.automation.ai.llmgateway.domain.AiLlmGatewayProvider;
import com.bytechef.automation.ai.llmgateway.dto.AiLlmGatewayChatCompletionRequest;
import com.bytechef.automation.ai.llmgateway.dto.AiLlmGatewayChatCompletionResponse;
import com.bytechef.automation.ai.llmgateway.dto.AiLlmGatewayChatMessage;
import com.bytechef.automation.ai.llmgateway.provider.AiLlmGatewayChatModelFactory;
import com.bytechef.automation.ai.llmgateway.service.AiLlmGatewayModelService;
import com.bytechef.automation.ai.llmgateway.service.AiLlmGatewayProviderService;
import java.util.List;
import java.util.UUID;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

@Component
public class AiLlmGatewayFacade {

    private final AiLlmGatewayChatModelFactory aiLlmGatewayChatModelFactory;
    private final AiLlmGatewayModelService aiLlmGatewayModelService;
    private final AiLlmGatewayProviderService aiLlmGatewayProviderService;

    public AiLlmGatewayFacade(
        AiLlmGatewayChatModelFactory aiLlmGatewayChatModelFactory,
        AiLlmGatewayModelService aiLlmGatewayModelService,
        AiLlmGatewayProviderService aiLlmGatewayProviderService) {

        this.aiLlmGatewayChatModelFactory = aiLlmGatewayChatModelFactory;
        this.aiLlmGatewayModelService = aiLlmGatewayModelService;
        this.aiLlmGatewayProviderService = aiLlmGatewayProviderService;
    }

    public AiLlmGatewayChatCompletionResponse chatCompletion(AiLlmGatewayChatCompletionRequest request) {
        ModelResolution modelResolution = resolveModel(request.model());

        AiLlmGatewayProvider provider = modelResolution.provider();
        AiLlmGatewayModel model = modelResolution.model();

        ChatModel chatModel = aiLlmGatewayChatModelFactory.getChatModel(provider);

        List<Message> messages = request.messages().stream()
            .map(this::toSpringAiMessage)
            .toList();

        ChatOptions.Builder chatOptionsBuilder = ChatOptions.builder()
            .model(model.getName());

        if (request.temperature() != null) {
            chatOptionsBuilder.temperature(request.temperature());
        }

        if (request.maxTokens() != null) {
            chatOptionsBuilder.maxTokens(request.maxTokens());
        }

        if (request.topP() != null) {
            chatOptionsBuilder.topP(request.topP());
        }

        Prompt prompt = new Prompt(messages, chatOptionsBuilder.build());

        ChatResponse chatResponse = chatModel.call(prompt);

        return toResponse(chatResponse, request.model());
    }

    private ModelResolution resolveModel(String modelIdentifier) {
        String[] parts = modelIdentifier.split("/", 2);

        if (parts.length != 2) {
            throw new IllegalArgumentException(
                "Model must be in format 'provider/model', got: " + modelIdentifier);
        }

        String providerTypeName = parts[0].toUpperCase().replace("-", "_");
        String modelName = parts[1];

        List<AiLlmGatewayProvider> providers = aiLlmGatewayProviderService.getEnabledProviders();

        AiLlmGatewayProvider provider = providers.stream()
            .filter(existingProvider -> existingProvider.getType().name().equalsIgnoreCase(providerTypeName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No enabled provider found for type: " + providerTypeName));

        AiLlmGatewayModel model = aiLlmGatewayModelService.getModel(provider.getId(), modelName);

        return new ModelResolution(provider, model);
    }

    private Message toSpringAiMessage(AiLlmGatewayChatMessage chatMessage) {
        return switch (chatMessage.role()) {
            case "system" -> new SystemMessage(chatMessage.content());
            case "assistant" -> new AssistantMessage(chatMessage.content());
            default -> new UserMessage(chatMessage.content());
        };
    }

    private AiLlmGatewayChatCompletionResponse toResponse(ChatResponse chatResponse, String requestedModel) {
        var generation = chatResponse.getResult();

        var choice = new AiLlmGatewayChatCompletionResponse.Choice(
            0,
            new AiLlmGatewayChatMessage("assistant", generation.getOutput().getText()),
            generation.getMetadata().getFinishReason());

        var usage = chatResponse.getMetadata().getUsage();

        var responseUsage = new AiLlmGatewayChatCompletionResponse.Usage(
            (int) usage.getPromptTokens(),
            (int) usage.getCompletionTokens(),
            (int) usage.getTotalTokens());

        return new AiLlmGatewayChatCompletionResponse(
            UUID.randomUUID().toString(),
            "chat.completion",
            System.currentTimeMillis() / 1000,
            requestedModel,
            List.of(choice),
            responseUsage);
    }

    private record ModelResolution(AiLlmGatewayProvider provider, AiLlmGatewayModel model) {
    }
}
```

- [x] **Step 3: Verify and commit**

```bash
./gradlew :server:libs:automation:automation-ai:automation-ai-llm-gateway:automation-ai-llm-gateway-service:compileJava
git add server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/
git commit -m "Add gateway facade with chat completion logic and DTOs"
```

---

### Task 1.8: REST Controllers for Public Gateway API

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-rest/src/main/java/com/bytechef/ee/automation/ai/llmgateway/web/rest/AiLlmGatewayChatCompletionController.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-rest/src/main/java/com/bytechef/ee/automation/ai/llmgateway/web/rest/AiLlmGatewayModelController.java`

- [x] **Step 1: Create chat completions controller**

```java
package com.bytechef.automation.ai.llmgateway.web.rest;

import com.bytechef.automation.ai.llmgateway.dto.AiLlmGatewayChatCompletionRequest;
import com.bytechef.automation.ai.llmgateway.dto.AiLlmGatewayChatCompletionResponse;
import com.bytechef.automation.ai.llmgateway.facade.AiLlmGatewayFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(prefix = "bytechef.ai.llm-gateway", name = "enabled", havingValue = "true")
@RequestMapping("/api/gateway/v1")
@SuppressFBWarnings("EI")
class AiLlmGatewayChatCompletionController {

    private final AiLlmGatewayFacade aiLlmGatewayFacade;

    @SuppressFBWarnings("EI")
    AiLlmGatewayChatCompletionController(AiLlmGatewayFacade aiLlmGatewayFacade) {
        this.aiLlmGatewayFacade = aiLlmGatewayFacade;
    }

    @PostMapping("/chat/completions")
    ResponseEntity<AiLlmGatewayChatCompletionResponse> chatCompletions(
        @RequestBody AiLlmGatewayChatCompletionRequest request) {

        return ResponseEntity.ok(aiLlmGatewayFacade.chatCompletion(request));
    }
}
```

- [x] **Step 2: Create models list controller**

```java
package com.bytechef.automation.ai.llmgateway.web.rest;

import com.bytechef.automation.ai.llmgateway.domain.AiLlmGatewayModel;
import com.bytechef.automation.ai.llmgateway.domain.AiLlmGatewayProvider;
import com.bytechef.automation.ai.llmgateway.service.AiLlmGatewayModelService;
import com.bytechef.automation.ai.llmgateway.service.AiLlmGatewayProviderService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(prefix = "bytechef.ai.llm-gateway", name = "enabled", havingValue = "true")
@RequestMapping("/api/gateway/v1")
@SuppressFBWarnings("EI")
class AiLlmGatewayModelController {

    private final AiLlmGatewayModelService aiLlmGatewayModelService;
    private final AiLlmGatewayProviderService aiLlmGatewayProviderService;

    @SuppressFBWarnings("EI")
    AiLlmGatewayModelController(
        AiLlmGatewayModelService aiLlmGatewayModelService,
        AiLlmGatewayProviderService aiLlmGatewayProviderService) {

        this.aiLlmGatewayModelService = aiLlmGatewayModelService;
        this.aiLlmGatewayProviderService = aiLlmGatewayProviderService;
    }

    @GetMapping("/models")
    ResponseEntity<Map<String, Object>> listModels() {
        List<AiLlmGatewayModel> models = aiLlmGatewayModelService.getEnabledModels();

        Map<Long, AiLlmGatewayProvider> providerMap = aiLlmGatewayProviderService.getEnabledProviders().stream()
            .collect(Collectors.toMap(AiLlmGatewayProvider::getId, Function.identity()));

        List<Map<String, Object>> modelData = models.stream()
            .map(model -> {
                AiLlmGatewayProvider provider = providerMap.get(model.getProviderId());
                String modelId = provider.getType().name().toLowerCase() + "/" + model.getName();

                return Map.<String, Object>of(
                    "id", modelId,
                    "object", "model",
                    "owned_by", provider.getType().name().toLowerCase());
            })
            .toList();

        return ResponseEntity.ok(Map.of("object", "list", "data", modelData));
    }

    @GetMapping("/models/{modelId}")
    ResponseEntity<Map<String, Object>> getModel(@PathVariable String modelId) {
        String[] parts = modelId.split("/", 2);

        if (parts.length != 2) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(Map.of(
            "id", modelId,
            "object", "model",
            "owned_by", parts[0]));
    }
}
```

- [x] **Step 3: Verify and commit**

```bash
./gradlew :server:libs:automation:automation-ai:automation-ai-llm-gateway:automation-ai-llm-gateway-rest:compileJava
git add server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-rest/
git commit -m "Add REST controllers for chat completions and models list endpoints"
```

---

### Task 1.9: Security Configuration for Gateway API

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/java/com/bytechef/ee/automation/ai/llmgateway/security/web/authentication/AiLlmGatewayApiKeyAuthenticationToken.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/java/com/bytechef/ee/automation/ai/llmgateway/security/web/authentication/AiLlmGatewayApiKeyAuthenticationProvider.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/java/com/bytechef/ee/automation/ai/llmgateway/security/web/configurer/AiLlmGatewaySecurityConfigurer.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/java/com/bytechef/ee/automation/ai/llmgateway/config/AiLlmGatewaySecurityConfiguration.java`

- [x] **Step 1: Create authentication token**

Follow existing `AutomationApiKeyAuthenticationToken` pattern. The token extends `AbstractApiKeyAuthenticationToken` and carries environment, secretKey, tenantId.

```java
package com.bytechef.automation.ai.llmgateway.security.web.authentication;

import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import org.springframework.security.core.userdetails.User;

public class AiLlmGatewayApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

    public AiLlmGatewayApiKeyAuthenticationToken(int environmentId, String secretKey, String tenantId) {
        super(environmentId, secretKey, tenantId);
    }

    public AiLlmGatewayApiKeyAuthenticationToken(User user) {
        super(user);
    }
}
```

- [x] **Step 2: Create authentication provider**

Reuse existing `ApiKeyService` to validate keys.

```java
package com.bytechef.automation.ai.llmgateway.security.web.authentication;

import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class AiLlmGatewayApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final ApiKeyService apiKeyService;
    private final AuthorityService authorityService;
    private final UserService userService;

    public AiLlmGatewayApiKeyAuthenticationProvider(
        ApiKeyService apiKeyService, AuthorityService authorityService, UserService userService) {

        this.apiKeyService = apiKeyService;
        this.authorityService = authorityService;
        this.userService = userService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        AiLlmGatewayApiKeyAuthenticationToken token =
            (AiLlmGatewayApiKeyAuthenticationToken) authentication;

        ApiKey apiKey = apiKeyService.getApiKey(
            (String) token.getCredentials(), token.getEnvironmentId());

        User user = userService.fetchUser(apiKey.getUserId());

        if (user == null || !user.isActivated()) {
            throw new BadCredentialsException("User not found or not activated");
        }

        List<SimpleGrantedAuthority> authorities = authorityService.getAuthorities(user.getId()).stream()
            .map(Authority::getName)
            .map(SimpleGrantedAuthority::new)
            .toList();

        org.springframework.security.core.userdetails.User securityUser =
            new org.springframework.security.core.userdetails.User(
                user.getLogin(), "", authorities);

        return new AiLlmGatewayApiKeyAuthenticationToken(securityUser);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return AiLlmGatewayApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
```

- [x] **Step 3: Create security configurer**

```java
package com.bytechef.automation.ai.llmgateway.security.web.configurer;

import com.bytechef.automation.ai.llmgateway.security.web.authentication.AiLlmGatewayApiKeyAuthenticationProvider;
import com.bytechef.automation.ai.llmgateway.security.web.authentication.AiLlmGatewayApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.configurer.AbstractApiKeyHttpConfigurer;
import com.bytechef.platform.security.web.filter.AbstractApiKeyAuthenticationConverter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

public class AiLlmGatewaySecurityConfigurer
    extends AbstractApiKeyHttpConfigurer<AiLlmGatewaySecurityConfigurer> {

    public AiLlmGatewaySecurityConfigurer(AiLlmGatewayApiKeyAuthenticationProvider authenticationProvider) {
        super(
            new RegexRequestMatcher("^/api/gateway/v[0-9]+/.+", null),
            new AiLlmGatewayApiKeyAuthenticationConverter(),
            authenticationProvider);
    }

    private static class AiLlmGatewayApiKeyAuthenticationConverter extends AbstractApiKeyAuthenticationConverter {

        @Override
        protected AiLlmGatewayApiKeyAuthenticationToken doConvert(
            int environmentId, String secretKey, String tenantId) {

            return new AiLlmGatewayApiKeyAuthenticationToken(environmentId, secretKey, tenantId);
        }
    }
}
```

- [x] **Step 4: Create security configuration bean**

```java
package com.bytechef.automation.ai.llmgateway.config;

import com.bytechef.automation.ai.llmgateway.security.web.authentication.AiLlmGatewayApiKeyAuthenticationProvider;
import com.bytechef.automation.ai.llmgateway.security.web.configurer.AiLlmGatewaySecurityConfigurer;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.config.SecurityConfigurerContributor;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.llm-gateway", name = "enabled", havingValue = "true")
class AiLlmGatewaySecurityConfiguration {

    @Bean
    SecurityConfigurerContributor aiLlmGatewaySecurityConfigurerContributor(
        ApiKeyService apiKeyService, AuthorityService authorityService, UserService userService) {

        return new SecurityConfigurerContributor() {

            @Override
            @SuppressWarnings("unchecked")
            public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>> T
                getSecurityConfigurerAdapter() {

                return (T) new AiLlmGatewaySecurityConfigurer(
                    new AiLlmGatewayApiKeyAuthenticationProvider(apiKeyService, authorityService, userService));
            }
        };
    }
}
```

- [x] **Step 5: Add user service dependencies to service build.gradle.kts**

Add to the service module's build.gradle.kts:
```kotlin
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))
```

- [x] **Step 6: Verify and commit**

```bash
./gradlew :server:libs:automation:automation-ai:automation-ai-llm-gateway:automation-ai-llm-gateway-service:compileJava
git add server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/
git commit -m "Add security configuration for gateway API using existing API key infrastructure"
```

---

### Task 1.10: Application Properties Configuration

**Files:**
- Modify: `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java`

- [x] **Step 1: Add LlmGateway inner class to Ai class**

Add inside the `Ai` class in `ApplicationProperties.java`:

```java
    private LlmGateway llmGateway;

    public LlmGateway getLlmGateway() {
        return llmGateway;
    }

    public void setLlmGateway(LlmGateway llmGateway) {
        this.llmGateway = llmGateway;
    }

    public static class LlmGateway {

        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
```

- [x] **Step 2: Verify and commit**

```bash
./gradlew :server:libs:config:app-config:compileJava
git add server/libs/config/app-config/
git commit -m "Add LLM Gateway configuration properties"
```

---

### Task 1.11: Wire Modules into Server App

**Files:**
- Modify: `server/apps/server-app/build.gradle.kts` (add gateway dependencies)

- [x] **Step 1: Add gateway module dependencies**

Add the LLM gateway modules to the server app's dependencies:

```kotlin
    implementation(project(":server:libs:automation:automation-ai:automation-ai-llm-gateway:automation-ai-llm-gateway-rest"))
    implementation(project(":server:libs:automation:automation-ai:automation-ai-llm-gateway:automation-ai-llm-gateway-service"))
    implementation(project(":server:libs:automation:automation-ai:automation-ai-llm-gateway:automation-ai-llm-gateway-graphql"))
```

- [x] **Step 2: Verify full build compiles**

```bash
./gradlew :server:apps:server-app:compileJava
```

- [x] **Step 3: Commit**

```bash
git add server/apps/server-app/build.gradle.kts
git commit -m "Wire LLM gateway modules into server application"
```

---

## Phase 2 — Routing & Reliability

### Task 2.1: Routing Policy Domain Model

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/domain/AiLlmGatewayRoutingPolicy.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/domain/AiLlmGatewayRoutingStrategyType.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/domain/AiLlmGatewayModelDeployment.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/repository/AiLlmGatewayRoutingPolicyRepository.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/repository/AiLlmGatewayModelDeploymentRepository.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/service/AiLlmGatewayRoutingPolicyService.java`

- [x] **Step 1: Create `AiLlmGatewayRoutingStrategyType` enum with values: `SIMPLE`, `WEIGHTED_RANDOM`, `COST_OPTIMIZED`, `LATENCY_OPTIMIZED`, `PRIORITY_FALLBACK`**

- [x] **Step 2: Create `AiLlmGatewayRoutingPolicy` entity with fields: id, name (unique), strategy (AiLlmGatewayRoutingStrategyType), fallbackModel, config (TEXT/JSON), enabled, audit fields, version. Table: `ai_llm_gateway_routing_policy`**

- [x] **Step 3: Create `AiLlmGatewayModelDeployment` entity with fields: id, routingPolicyId, modelId, weight (default 1), priorityOrder (default 0), maxRpm, maxTpm, enabled. Table: `ai_llm_gateway_model_deployment`**

- [x] **Step 4: Create repositories for both entities**

- [x] **Step 5: Create `AiLlmGatewayRoutingPolicyService` interface with CRUD + `getRoutingPolicyByName(String name)`**

- [x] **Step 6: Commit**

---

### Task 2.2: Liquibase Migration for Routing Tables

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_llm_gateway/00000000000002_ai_llm_gateway_routing.xml`

- [x] **Step 1: Create changeset with `ai_llm_gateway_routing_policy` and `ai_llm_gateway_model_deployment` tables matching domain entities**

- [x] **Step 2: Commit**

---

### Task 2.3: Routing Engine

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/java/com/bytechef/ee/automation/ai/llmgateway/routing/AiLlmGatewayRoutingStrategy.java` (interface)
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/java/com/bytechef/ee/automation/ai/llmgateway/routing/SimpleRoutingStrategy.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/java/com/bytechef/ee/automation/ai/llmgateway/routing/WeightedRandomRoutingStrategy.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/java/com/bytechef/ee/automation/ai/llmgateway/routing/CostOptimizedRoutingStrategy.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/java/com/bytechef/ee/automation/ai/llmgateway/routing/LatencyOptimizedRoutingStrategy.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/java/com/bytechef/ee/automation/ai/llmgateway/routing/PriorityFallbackRoutingStrategy.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/java/com/bytechef/ee/automation/ai/llmgateway/routing/AiLlmGatewayRouter.java`

- [x] **Step 1: Create `AiLlmGatewayRoutingStrategy` interface with `selectDeployment(List<AiLlmGatewayModelDeployment> deployments, RoutingContext context)` method**

- [x] **Step 2: Implement all 5 strategies**

- [x] **Step 3: Create `AiLlmGatewayRouter` that takes strategy type, resolves strategy, and calls `selectDeployment`**

- [x] **Step 4: Commit**

---

### Task 2.4: Reliability — Retry, Failover, Cooldown

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/java/com/bytechef/ee/automation/ai/llmgateway/reliability/AiLlmGatewayCooldownTracker.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/java/com/bytechef/ee/automation/ai/llmgateway/reliability/AiLlmGatewayRetryHandler.java`

- [x] **Step 1: Create `AiLlmGatewayCooldownTracker` with ConcurrentHashMap tracking consecutive failures per deployment, cooldown timestamps, `isCooledDown(long deploymentId)`, `recordFailure(long deploymentId)`, `recordSuccess(long deploymentId)` methods**

- [x] **Step 2: Create `AiLlmGatewayRetryHandler` with retry loop: try deployment → on failure record cooldown → try next → exponential backoff between retries**

- [x] **Step 3: Commit**

---

### Task 2.5: Integrate Routing into Gateway Facade

**Files:**
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/java/com/bytechef/ee/automation/ai/llmgateway/facade/AiLlmGatewayFacade.java`

- [x] **Step 1: Update `chatCompletion` method to check for `routingPolicy` in request, resolve policy, use router to select deployment, use retry handler for failover**

- [x] **Step 2: Commit**

---

### Task 2.6: Routing Policy Service Implementation and CRUD Controller

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/java/com/bytechef/ee/automation/ai/llmgateway/service/AiLlmGatewayRoutingPolicyServiceImpl.java`

- [x] **Step 1: Implement service with CRUD operations**

- [x] **Step 2: Commit**

---

## Phase 3 — Request Logging

### Task 3.1: Request Log Domain Model

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/domain/AiLlmGatewayRequestLog.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/repository/AiLlmGatewayRequestLogRepository.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/service/AiLlmGatewayRequestLogService.java`

- [x] **Step 1: Create entity with fields: id, requestId (UUID), apiKeyId, requestedModel, routedModel, routedProvider, routingPolicyId, routingStrategy, latencyMs, inputTokens, outputTokens, cost (BigDecimal), status, errorMessage, cacheHit, createdDate**

- [x] **Step 2: Create repository with query methods: findAllByCreatedDateBetween, custom paginated queries**

- [x] **Step 3: Create service interface**

- [x] **Step 4: Commit**

---

### Task 3.2: Request Log Liquibase Migration

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_llm_gateway/00000000000003_ai_llm_gateway_request_log.xml`

- [x] **Step 1: Create `ai_llm_gateway_request_log` table with indexes on created_date, api_key_id, routed_model**

- [x] **Step 2: Commit**

---

### Task 3.3: Request Logging Service and Integration

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/java/com/bytechef/ee/automation/ai/llmgateway/service/AiLlmGatewayRequestLogServiceImpl.java`

- [x] **Step 1: Implement service with async logging via `@Async` and paginated query methods**

- [x] **Step 2: Integrate into `AiLlmGatewayFacade` — log every chat completion request with timing, tokens, cost, routing info**

- [x] **Step 3: Commit**

---

## Phase 4 — Spend Tracking & Caching

### Task 4.1: Cost Calculation

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/java/com/bytechef/ee/automation/ai/llmgateway/cost/AiLlmGatewayCostCalculator.java`

- [x] **Step 1: Create calculator using model pricing: `(inputTokens / 1_000_000.0 * inputCostPerMTokens) + (outputTokens / 1_000_000.0 * outputCostPerMTokens)`**

- [x] **Step 2: Commit**

---

### Task 4.2: Spend Summary Domain and Aggregation

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/domain/AiLlmGatewaySpendSummary.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/repository/AiLlmGatewaySpendSummaryRepository.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/service/AiLlmGatewaySpendService.java`
- Create: Liquibase migration for spend summary table

- [x] **Step 1: Create domain, repository, service interface**

- [x] **Step 2: Create Liquibase migration `00000000000004_ai_llm_gateway_spend_summary.xml`**

- [x] **Step 3: Implement spend service with aggregation queries and scheduled hourly rollup**

- [x] **Step 4: Commit**

---

### Task 4.3: Response Caching

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/src/main/java/com/bytechef/ee/automation/ai/llmgateway/cache/AiLlmGatewayResponseCache.java`

- [x] **Step 1: Create cache with ConcurrentHashMap (dev) and Redis support (production). Cache key = SHA-256(model + messages + temperature + maxTokens + topP). Configurable TTL.**

- [x] **Step 2: Integrate into facade — check cache before routing, store on miss, respect `cache: false` flag**

- [x] **Step 3: Commit**

---

### Task 4.4: Embeddings Endpoint

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/dto/AiLlmGatewayEmbeddingRequest.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/src/main/java/com/bytechef/ee/automation/ai/llmgateway/dto/AiLlmGatewayEmbeddingResponse.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-rest/src/main/java/com/bytechef/ee/automation/ai/llmgateway/web/rest/AiLlmGatewayEmbeddingController.java`

- [x] **Step 1: Create request/response DTOs matching OpenAI embeddings format**

- [x] **Step 2: Add embeddings support to facade using Spring AI's EmbeddingModel**

- [x] **Step 3: Create REST controller at `POST /api/gateway/v1/embeddings`**

- [x] **Step 4: Commit**

---

## Phase 5 — UI Configuration

### Task 5.1: GraphQL Schema for Gateway

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-graphql/src/main/resources/graphql/ai-llm-gateway-provider.graphqls`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-graphql/src/main/resources/graphql/ai-llm-gateway-model.graphqls`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-graphql/src/main/resources/graphql/ai-llm-gateway-routing-policy.graphqls`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-graphql/src/main/resources/graphql/ai-llm-gateway-request-log.graphqls`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-graphql/src/main/resources/graphql/ai-llm-gateway-spend.graphqls`

- [x] **Step 1: Define provider schema (Query: aiLlmGatewayProviders, aiLlmGatewayProvider(id); Mutation: createAiLlmGatewayProvider, updateAiLlmGatewayProvider, deleteAiLlmGatewayProvider)**

- [x] **Step 2: Define model schema (Query: aiLlmGatewayModels, aiLlmGatewayModelsByProvider; Mutation: createAiLlmGatewayModel, updateAiLlmGatewayModel, deleteAiLlmGatewayModel)**

- [x] **Step 3: Define routing policy schema (Query: aiLlmGatewayRoutingPolicies; Mutation: create/update/delete)**

- [x] **Step 4: Define request log schema (Query: aiLlmGatewayRequestLogs with filter inputs for date range, model, status)**

- [x] **Step 5: Define spend schema (Query: aiLlmGatewaySpendSummary with time range and groupBy)**

- [x] **Step 6: Commit**

---

### Task 5.2: GraphQL Controllers

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/llmgateway/web/graphql/AiLlmGatewayProviderGraphQlController.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/llmgateway/web/graphql/AiLlmGatewayModelGraphQlController.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/llmgateway/web/graphql/AiLlmGatewayRoutingPolicyGraphQlController.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/llmgateway/web/graphql/AiLlmGatewayRequestLogGraphQlController.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/llmgateway/web/graphql/AiLlmGatewaySpendGraphQlController.java`

- [x] **Step 1: Create all 5 controllers following the `@Controller` + `@ConditionalOnCoordinator` + `@QueryMapping`/`@MutationMapping` pattern. Each delegates to the corresponding service.**

- [x] **Step 2: Add GraphQL codegen to `client/codegen.ts` schema array**

- [x] **Step 3: Run `cd client && npx graphql-codegen` to generate types**

- [x] **Step 4: Commit**

---

### Task 5.3: Sidebar Entry and Routing

**Files:**
- Modify: `client/src/App.tsx` (add navigation item)
- Modify: `client/src/routes.tsx` (add route)
- Create: `client/src/pages/automation/ai-gateway/AiGateway.tsx`

- [x] **Step 1: Add to `automationNavigation` as last item: `{ href: '/automation/ai-gateway', icon: BrainCircuitIcon, name: 'AI Gateway' }`**

- [x] **Step 2: Add lazy import and route in `routes.tsx` under automation routes: path `'ai-gateway'` and `'ai-gateway/*'` for sub-routes**

- [x] **Step 3: Create `AiGateway.tsx` main page with `LayoutContainer`, left sidebar nav with items: Providers, Models, Routing, Settings, Monitoring**

- [x] **Step 4: Commit**

---

### Task 5.4: Providers Configuration Page

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/providers/AiGatewayProviders.tsx`
- Create: `client/src/pages/automation/ai-gateway/components/providers/AiGatewayProviderDialog.tsx`

- [x] **Step 1: Create provider list component with table (name, type, enabled toggle, actions) and "Add Provider" button**

- [x] **Step 2: Create provider dialog with form fields: name, type (dropdown of provider types), API key (password input), base URL (optional), enabled toggle. Uses GraphQL mutations.**

- [x] **Step 3: Commit**

---

### Task 5.5: Models Configuration Page

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/models/AiGatewayModels.tsx`
- Create: `client/src/pages/automation/ai-gateway/components/models/AiGatewayModelDialog.tsx`

- [x] **Step 1: Create model list grouped by provider, showing: name, alias, context window, pricing, capabilities, enabled**

- [x] **Step 2: Create model dialog for add/edit with all fields**

- [x] **Step 3: Commit**

---

### Task 5.6: Routing Policies Page

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/routing/AiGatewayRoutingPolicies.tsx`
- Create: `client/src/pages/automation/ai-gateway/components/routing/AiGatewayRoutingPolicyDialog.tsx`

- [x] **Step 1: Create policy list with name, strategy, deployment count, enabled**

- [x] **Step 2: Create policy dialog with: name, strategy dropdown, model deployment list (add/remove/reorder with weight and priority fields)**

- [x] **Step 3: Commit**

---

### Task 5.7: Settings Page

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/settings/AiGatewaySettings.tsx`

- [x] **Step 1: Create settings form with: default retry count, default timeout, caching enabled toggle, cache TTL, log retention days**

- [x] **Step 2: Commit**

---

## Phase 6 — UI Monitoring Dashboard

### Task 6.1: Dashboard Layout with Summary Cards

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/monitoring/AiGatewayDashboard.tsx`

- [x] **Step 1: Create dashboard with time range selector (1h, 6h, 24h, 7d, 30d) and summary stat cards: total requests, avg latency (ms), error rate (%), total spend ($)**

- [x] **Step 2: Wire to GraphQL queries for aggregated data**

- [x] **Step 3: Commit**

---

### Task 6.2: Request Volume and Latency Charts

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/monitoring/charts/RequestVolumeChart.tsx`
- Create: `client/src/pages/automation/ai-gateway/components/monitoring/charts/LatencyChart.tsx`

- [x] **Step 1: Create request volume line chart using shadcn charts (Recharts). X-axis: time, Y-axis: request count, grouped by model/provider.**

- [x] **Step 2: Create latency chart with P50, P95, P99 lines over time**

- [x] **Step 3: Commit**

---

### Task 6.3: Error Rate and Cost Charts

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/monitoring/charts/ErrorRateChart.tsx`
- Create: `client/src/pages/automation/ai-gateway/components/monitoring/charts/CostBreakdownChart.tsx`

- [x] **Step 1: Create error rate bar chart grouped by provider**

- [x] **Step 2: Create cost breakdown pie/bar chart by model, provider, or API key**

- [x] **Step 3: Commit**

---

### Task 6.4: Request Log Viewer

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/monitoring/AiGatewayRequestLog.tsx`

- [x] **Step 1: Create searchable, filterable table with columns: timestamp, model, provider, latency, tokens, cost, status. Filters: model, status, date range. Expandable rows for full details.**

- [x] **Step 2: Wire to paginated GraphQL query**

- [x] **Step 3: Commit**

---

### Task 6.5: Final Integration and Verification

- [x] **Step 1: Run `./gradlew spotlessApply` on all new server files**

- [x] **Step 2: Run `./gradlew :server:apps:server-app:compileJava` to verify full server build**

- [x] **Step 3: Run `cd client && npm run check` to verify client build**

- [x] **Step 4: Final commit**

```bash
git commit -m "Complete LLM API Gateway - all 6 phases implemented"
```

---

## Audit 2026-04-13 — Remaining Unfinished Items

All 126 checkboxes in this plan were verified against the shipped implementation under
`server/ee/libs/automation/automation-ai/automation-ai-gateway/` and
`client/src/pages/automation/ai-gateway/` and marked complete.

Notes on naming drift:
- The plan uses the module name `automation-ai-llm-gateway` and package `com.bytechef.automation.ai.llmgateway`.
  The shipped implementation uses `automation-ai-gateway` and `com.bytechef.ee.automation.ai.gateway`.
  Entity prefixes were changed from `AiLlmGateway*` to `AiGateway*`, and Liquibase tables from
  `ai_llm_gateway_*` to `ai_gateway_*`. All functional requirements are satisfied.
- Client sidebar label is "LLM Gateway" (App.tsx, not a separate `automationNavigation` constant)
  and route is `/automation/ai-gateway` — equivalent to the plan.

Group F / ApiKey reuse (per user instruction):
- The plan never defined a dedicated gateway API-key entity/table/CRUD. Task 1.9 wraps the
  platform `ApiKeyService` via `AiGatewayApiKeyAuthenticationProvider` — i.e., it already
  reuses the platform `ApiKey` entity. No tasks required WONTFIX marking.

No remaining `- [ ]` items.

Scope shipped beyond this plan (tracked in later phase plans, not here):
observability (traces, spans, sessions, alerts, webhooks, exports), evaluations
(rules, scores, configs, executions), prompts (registry + versions), playground,
budgets, rate limiting (in-memory + Redis), tags, projects, workspace settings,
custom properties, metrics, context compression, guardrails via AiEval rules.
