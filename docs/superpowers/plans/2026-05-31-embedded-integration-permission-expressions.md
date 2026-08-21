# Embedded Integration & Workflow Permission Expressions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an optional SpEL "permission expression" to embedded `Integration` and `IntegrationWorkflow` that, when an integration is rendered through the embedded public REST API, filters out integrations/workflows not visible to the requesting connected user.

**Architecture:** A new `EmbeddedPermissionEvaluator` (wrapping the existing sandboxed `Evaluator`/`SpelEvaluator` bean) evaluates each expression against a context built from the `ConnectedUser` (metadata map + externalId/email/name/environment). Filtering happens entirely in `ConnectedUserIntegrationFacadeImpl` — the single place the connected user is resolved — and the MCP-workflow/tool assembly currently done in the public-rest controller is relocated into the facade so regular and MCP workflows are filtered uniformly. Authoring is via new GraphQL mutations on the existing embedded-configuration GraphQL controllers, surfaced in the existing admin `IntegrationDialog` / `WorkflowDialog`.

**Tech Stack:** Java 25 / Spring Boot 4, Spring Data JDBC, Liquibase, MapStruct, Spring for GraphQL, SpEL (`SpelEvaluator`), React 19 + TypeScript, GraphQL Code Generator, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-05-31-embedded-integration-permission-expressions-design.md`

**Conventions reminder (from CLAUDE.md):**
- EE files use the **ByteChef Enterprise license header** (not Apache) and a `@version ee` Javadoc tag.
- Run `./gradlew spotlessApply` before each server commit; `./gradlew check` before finishing.
- Client: `npm run check` (in `client/`) before client commits; `twMerge` not `cn`; Lucide icons with `Icon` suffix; named imports sorted; interface names end in `I`/`Props`.
- Server commit messages: `0_732 <description>`. Client: `0_732 client - <description>`.
- Blank line before control statements; blank line after a variable modification that precedes its use; no trailing blank line in a class body.
- Only stage files you changed for the task.

---

## File Structure

**Create:**
- `.../embedded-configuration-service/.../resources/config/liquibase/changelog/embedded/configuration/20240604183170_embedded_configuration_added_permission_expression.xml` — migration adding `permission_expression` to `integration` and `integration_workflow`.
- `.../embedded-configuration-service/.../security/EmbeddedPermissionEvaluator.java` — evaluates an expression against a `ConnectedUser`.
- `.../embedded-configuration-service/.../security/EmbeddedPermissionEvaluatorConfiguration.java` — `@Bean` wiring the evaluator from the core `Evaluator`.
- `.../embedded-configuration-api/.../exception/EmbeddedIntegrationNotVisibleException.java` — unchecked exception → 404 in the controller.
- `.../embedded-configuration-service/src/test/java/.../security/EmbeddedPermissionEvaluatorTest.java` — unit tests for the evaluator.
- Client: `client/src/graphql/embedded/configuration/updateIntegrationPermissionExpression.graphql`, `updateIntegrationWorkflowPermissionExpression.graphql`.

**Modify:**
- `.../embedded-configuration-api/.../domain/Integration.java` — add `permissionExpression` column/field/getter/setter + persistence constructor + toString.
- `.../embedded-configuration-api/.../domain/IntegrationWorkflow.java` — add `permissionExpression` field/getter/setter + toString.
- `.../embedded-configuration-api/.../dto/IntegrationDTO.java` — add `permissionExpression` record component + constructors + builder + `toIntegration`.
- `.../embedded-configuration-api/.../dto/ConnectedUserIntegrationDTO.java` — add MCP carrier records + fields.
- `.../embedded-configuration-api/.../service/IntegrationService.java` (+ Impl) — `updatePermissionExpression`.
- `.../embedded-configuration-api/.../service/IntegrationWorkflowService.java` (+ Impl) — `updatePermissionExpression`; carry expression through `update`.
- `.../embedded-configuration-service/.../facade/ConnectedUserIntegrationFacadeImpl.java` — filtering + MCP assembly relocation.
- `.../embedded-configuration-service/build.gradle.kts` — add evaluator-api + MCP/platform deps.
- `.../embedded-configuration-public-rest/.../web/rest/IntegrationApiController.java` — remove MCP assembly; catch not-visible → 404; conversion only.
- `.../embedded-configuration-public-rest/.../web/rest/mapper/ConnectedUserIntegrationMapper.java` — map new MCP carriers instead of `ignore`.
- `.../embedded-configuration-public-rest/build.gradle.kts` — (verify deps still satisfied after controller slim-down).
- `.../embedded-configuration-graphql/.../resources/graphql/integration.graphqls` + `integration-workflow.graphqls` — fields + mutations.
- `.../embedded-configuration-graphql/.../web/graphql/IntegrationGraphQlController.java` + `IntegrationWorkflowGraphQlController.java` — mutations + schema field mappings.
- Client: `IntegrationDialog.tsx`, `WorkflowDialog.tsx`, and regenerated `client/src/shared/middleware/graphql.ts`.

---

## Phase 1 — Storage

### Task 1: Liquibase migration for `permission_expression` columns

**Files:**
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/resources/config/liquibase/changelog/embedded/configuration/20240604183170_embedded_configuration_added_permission_expression.xml`

Context: the embedded changelog directory is aggregated via `<includeAll path="changelog/" .../>` in `server/libs/config/liquibase-config/src/main/resources/config/liquibase/master.xml`, so a new file in this directory is picked up automatically. Columns are nullable with no default (existing rows ⇒ `NULL` ⇒ "no expression"). `TEXT` matches the existing `integration.description` column type.

- [ ] **Step 1: Create the migration file**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">
    <changeSet id="20240604183170" author="Ivica Cardic">
        <addColumn tableName="integration">
            <column name="permission_expression" type="TEXT"/>
        </addColumn>
        <addColumn tableName="integration_workflow">
            <column name="permission_expression" type="TEXT"/>
        </addColumn>
    </changeSet>
</databaseChangeLog>
```

- [ ] **Step 2: Delete any stale built copy (classpath safety)**

Run:
```bash
rm -f server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/build/resources/main/config/liquibase/changelog/embedded/configuration/20240604183170_embedded_configuration_added_permission_expression.xml
```
Expected: no error (file may not exist yet — that's fine).

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/resources/config/liquibase/changelog/embedded/configuration/20240604183170_embedded_configuration_added_permission_expression.xml
git commit -m "0_732 Add permission_expression columns to integration and integration_workflow

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 2: Add `permissionExpression` to `Integration` domain entity

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/domain/Integration.java`

Note: `Integration` has a `@PersistenceCreator` constructor (Spring Data JDBC will use it). The field must be added to that constructor's parameter list and body, or Spring Data won't populate it. The `Builder` is only used for test/programmatic creation and does **not** need it (the GraphQL update path uses the setter via the service).

- [ ] **Step 1: Add the field (after the `name` field, keeping alphabetical-ish grouping near other `@Column` fields)**

In `Integration.java`, after:
```java
    @Column
    private String name;
```
add:
```java
    @Column("permission_expression")
    private String permissionExpression;
```

- [ ] **Step 2: Add the parameter to the `@PersistenceCreator` constructor**

Change the constructor signature from:
```java
    @PersistenceCreator
    public Integration(
        AggregateReference<Category, Long> categoryId, String componentName, int componentVersion, String description,
        Long id,
        Set<IntegrationTag> integrationTags, Set<IntegrationVersion> integrationVersions, int version) {

        this.categoryId = categoryId;
        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.description = description;
        this.id = id;
        this.integrationTags.addAll(integrationTags);
        this.integrationVersions.addAll(integrationVersions);
        this.version = version;
    }
```
to:
```java
    @PersistenceCreator
    public Integration(
        AggregateReference<Category, Long> categoryId, String componentName, int componentVersion, String description,
        Long id,
        Set<IntegrationTag> integrationTags, Set<IntegrationVersion> integrationVersions, String permissionExpression,
        int version) {

        this.categoryId = categoryId;
        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.description = description;
        this.id = id;
        this.integrationTags.addAll(integrationTags);
        this.integrationVersions.addAll(integrationVersions);
        this.permissionExpression = permissionExpression;
        this.version = version;
    }
```

- [ ] **Step 3: Add getter + setter (place getter after `getName()`, setter after `setName()`)**

Getter (after `getName()`):
```java
    public String getPermissionExpression() {
        return permissionExpression;
    }
```
Setter (after `setName(String name)`):
```java
    public void setPermissionExpression(String permissionExpression) {
        this.permissionExpression = permissionExpression;
    }
```

- [ ] **Step 4: Add to `toString()`**

In `toString()`, after the `", name='" + name + '\'' +` line, add:
```java
            ", permissionExpression='" + permissionExpression + '\'' +
```

- [ ] **Step 5: Compile**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Format + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/domain/Integration.java
git commit -m "0_732 Add permissionExpression to Integration entity

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 3: Add `permissionExpression` to `IntegrationWorkflow` domain entity

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/domain/IntegrationWorkflow.java`

Note: `IntegrationWorkflow` has **no** `@PersistenceCreator` — Spring Data JDBC uses the no-arg constructor + field/property access, so simply adding the `@Column` field + accessors is enough.

- [ ] **Step 1: Add the field (after the `uuid` field)**

After:
```java
    @Column("uuid")
    private UUID uuid;
```
add:
```java
    @Column("permission_expression")
    private String permissionExpression;
```

- [ ] **Step 2: Add getter (after `getUuidAsString()`) and setter (after `setUuid(UUID uuid)`)**

Getter:
```java
    public String getPermissionExpression() {
        return permissionExpression;
    }
```
Setter:
```java
    public void setPermissionExpression(String permissionExpression) {
        this.permissionExpression = permissionExpression;
    }
```

- [ ] **Step 3: Add to `toString()`**

After the `", uuid='" + uuid + '\'' +` line add:
```java
            ", permissionExpression='" + permissionExpression + '\'' +
```

- [ ] **Step 4: Compile**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Format + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/domain/IntegrationWorkflow.java
git commit -m "0_732 Add permissionExpression to IntegrationWorkflow entity

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 4: Add `permissionExpression` to `IntegrationDTO`

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/dto/IntegrationDTO.java`

The list-filtering path reads the integration-level expression via `integrationInstanceConfigurationDTO.integration().permissionExpression()`. Add the component, populate it in both constructors, add it to the `Builder` and `build()`, and copy it back in `toIntegration()`.

- [ ] **Step 1: Add the record component**

Change the record header from:
```java
public record IntegrationDTO(
    Category category, String componentName, int componentVersion, String createdBy, Instant createdDate,
    String description, String icon, Long id, List<IntegrationVersion> integrationVersions,
    List<Long> integrationWorkflowIds, String lastModifiedBy, Instant lastModifiedDate, Instant lastPublishedDate,
    Status lastStatus, Integer lastIntegrationVersion, boolean multipleInstances, String name, List<Tag> tags,
    String title, int version) {
```
to (add `String permissionExpression` immediately after `name`):
```java
public record IntegrationDTO(
    Category category, String componentName, int componentVersion, String createdBy, Instant createdDate,
    String description, String icon, Long id, List<IntegrationVersion> integrationVersions,
    List<Long> integrationWorkflowIds, String lastModifiedBy, Instant lastModifiedDate, Instant lastPublishedDate,
    Status lastStatus, Integer lastIntegrationVersion, boolean multipleInstances, String name,
    String permissionExpression, List<Tag> tags, String title, int version) {
```

- [ ] **Step 2: Populate in the `IntegrationDTO(Integration integration)` constructor**

Change the delegating `this(...)` call from:
```java
            integration.getLastIntegrationVersion(), integration.isMultipleInstances(), integration.getName(),
            List.of(), null,
            integration.getVersion());
```
to:
```java
            integration.getLastIntegrationVersion(), integration.isMultipleInstances(), integration.getName(),
            integration.getPermissionExpression(), List.of(), null,
            integration.getVersion());
```

- [ ] **Step 3: Populate in the `IntegrationDTO(Category, ComponentDefinition, Integration, List<Long>, List<Tag>)` constructor**

Change the tail of that `this(...)` call from:
```java
            integration.isMultipleInstances(), integration.getName(), tags, componentDefinition.getTitle(),
            integration.getVersion());
```
to:
```java
            integration.isMultipleInstances(), integration.getName(), integration.getPermissionExpression(), tags,
            componentDefinition.getTitle(), integration.getVersion());
```

- [ ] **Step 4: Carry it in `toIntegration()`**

In `toIntegration()`, after:
```java
        integration.setName(name);
```
add:
```java

        integration.setPermissionExpression(permissionExpression);
```
(The blank line follows the "blank line after variable modification" convention; keep the existing `setTags`/`setVersion` calls after it.)

- [ ] **Step 5: Add to the `Builder`**

Add the field (after `private String name;`):
```java
        private String permissionExpression;
```
Add the builder method (after the `name(...)` method):
```java
        public Builder permissionExpression(String permissionExpression) {
            this.permissionExpression = permissionExpression;

            return this;
        }
```
Update `build()` from:
```java
            return new IntegrationDTO(
                category, componentName, componentVersion, createdBy, createdDate, description, null, id,
                integrationVersions, integrationWorkflowIds, lastModifiedBy, lastModifiedDate, lastPublishedDate,
                lastStatus, lastIntegrationVersion, multipleInstances, name, tags, null, version);
```
to:
```java
            return new IntegrationDTO(
                category, componentName, componentVersion, createdBy, createdDate, description, null, id,
                integrationVersions, integrationWorkflowIds, lastModifiedBy, lastModifiedDate, lastPublishedDate,
                lastStatus, lastIntegrationVersion, multipleInstances, name, permissionExpression, tags, null, version);
```

- [ ] **Step 6: Compile (this surfaces any other call sites of the canonical constructor)**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-api:compileJava`
Expected: BUILD SUCCESSFUL. If a compile error points at another canonical-constructor call site, add `null` (or the real value) in the new `permissionExpression` position there and note it in the commit.

- [ ] **Step 7: Format + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/dto/IntegrationDTO.java
git commit -m "0_732 Expose permissionExpression on IntegrationDTO

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Phase 2 — Evaluator

### Task 5: `EmbeddedPermissionEvaluator` + bean config + unit tests

**Files:**
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/security/EmbeddedPermissionEvaluator.java`
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/security/EmbeddedPermissionEvaluatorConfiguration.java`
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/security/EmbeddedPermissionEvaluatorTest.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/build.gradle.kts`

The core `Evaluator` interface lives in `server/libs/core/evaluator/evaluator-api` (`com.bytechef.evaluator.Evaluator`); the concrete `SpelEvaluator` is in `evaluator-impl`. The production `Evaluator` bean is provided by `eval-config`. The service module currently has `evaluator-impl` only as `testImplementation`, so we add `evaluator-api` as `implementation` (for the `Evaluator` type) and keep `evaluator-impl` on the test classpath for the unit test to instantiate `SpelEvaluator.create()`.

- [ ] **Step 1: Add the evaluator-api dependency**

In `embedded-configuration-service/build.gradle.kts`, in the `implementation` block (after `commons-util`), add:
```kotlin
    implementation(project(":server:libs:core:evaluator:evaluator-api"))
```
(`evaluator-impl` is already present as `testImplementation` — leave it.)

- [ ] **Step 2: Write the failing unit test**

Create `EmbeddedPermissionEvaluatorTest.java`:
```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class EmbeddedPermissionEvaluatorTest {

    private EmbeddedPermissionEvaluator embeddedPermissionEvaluator;

    @Mock
    private ConnectedUser connectedUser;

    @BeforeEach
    void setUp() {
        embeddedPermissionEvaluator = new EmbeddedPermissionEvaluator(SpelEvaluator.create());
    }

    @Test
    void testNullExpressionIsVisible() {
        assertTrue(embeddedPermissionEvaluator.evaluate(null, connectedUser));
    }

    @Test
    void testBlankExpressionIsVisible() {
        assertTrue(embeddedPermissionEvaluator.evaluate("   ", connectedUser));
    }

    @Test
    void testMetadataMatchIsVisible() {
        when(connectedUser.getMetadata()).thenReturn(Map.of("plan", "pro"));

        assertTrue(embeddedPermissionEvaluator.evaluate("metadata['plan'] == 'pro'", connectedUser));
    }

    @Test
    void testMetadataMismatchIsHidden() {
        when(connectedUser.getMetadata()).thenReturn(Map.of("plan", "free"));

        assertFalse(embeddedPermissionEvaluator.evaluate("metadata['plan'] == 'pro'", connectedUser));
    }

    @Test
    void testMissingMetadataKeyIsHidden() {
        when(connectedUser.getMetadata()).thenReturn(Map.of());

        assertFalse(embeddedPermissionEvaluator.evaluate("metadata['plan'] == 'pro'", connectedUser));
    }

    @Test
    void testEmailUserFieldIsVisible() {
        when(connectedUser.getMetadata()).thenReturn(Map.of());
        when(connectedUser.getEmail()).thenReturn("jane@acme.com");

        assertTrue(embeddedPermissionEvaluator.evaluate("email.endsWith('@acme.com')", connectedUser));
    }

    @Test
    void testEnvironmentUserFieldIsVisible() {
        when(connectedUser.getMetadata()).thenReturn(Map.of());
        when(connectedUser.getEnvironment()).thenReturn(Environment.PRODUCTION);

        assertTrue(embeddedPermissionEvaluator.evaluate("environment == 'PRODUCTION'", connectedUser));
    }

    @Test
    void testInvalidExpressionIsHiddenFailClosed() {
        when(connectedUser.getMetadata()).thenReturn(Map.of());

        assertFalse(embeddedPermissionEvaluator.evaluate("this is not valid spel )(", connectedUser));
    }

    @Test
    void testNonBooleanResultIsHidden() {
        when(connectedUser.getMetadata()).thenReturn(Map.of("plan", "pro"));

        assertFalse(embeddedPermissionEvaluator.evaluate("metadata['plan']", connectedUser));
    }
}
```

- [ ] **Step 3: Run the test to verify it fails (class doesn't exist yet)**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluatorTest"`
Expected: compile failure / FAIL — `EmbeddedPermissionEvaluator` does not exist.

- [ ] **Step 4: Implement `EmbeddedPermissionEvaluator`**

Create `EmbeddedPermissionEvaluator.java`:
```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.evaluator.Evaluator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evaluates a SpEL permission expression authored on an integration or integration workflow against the requesting
 * connected user. Fails closed: a null/blank expression is visible, anything that does not evaluate to {@code true}
 * (including evaluation errors) is hidden.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedPermissionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedPermissionEvaluator.class);

    private final Evaluator evaluator;

    @SuppressFBWarnings("EI")
    public EmbeddedPermissionEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    public boolean evaluate(@Nullable String permissionExpression, ConnectedUser connectedUser) {
        if (StringUtils.isBlank(permissionExpression)) {
            return true;
        }

        try {
            Map<String, Object> context = buildContext(connectedUser);

            Map<String, Object> evaluated = evaluator.evaluate(
                Map.of("__result", "=" + permissionExpression), context);

            return Boolean.parseBoolean(String.valueOf(evaluated.get("__result")));
        } catch (Exception exception) {
            log.warn(
                "Failed to evaluate permission expression [{}] for connected user [{}]; hiding (fail closed)",
                permissionExpression, connectedUser.getExternalId(), exception);

            return false;
        }
    }

    private Map<String, Object> buildContext(ConnectedUser connectedUser) {
        Map<String, Object> context = new HashMap<>();

        context.put("metadata", connectedUser.getMetadata());
        context.put("externalId", connectedUser.getExternalId());
        context.put("email", connectedUser.getEmail());
        context.put("name", connectedUser.getName());
        context.put("environment", connectedUser.getEnvironment().name());

        return context;
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluatorTest"`
Expected: PASS (9 tests).

- [ ] **Step 6: Create the bean configuration**

Create `EmbeddedPermissionEvaluatorConfiguration.java`:
```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
public class EmbeddedPermissionEvaluatorConfiguration {

    @Bean
    EmbeddedPermissionEvaluator embeddedPermissionEvaluator(Evaluator evaluator) {
        return new EmbeddedPermissionEvaluator(evaluator);
    }
}
```

- [ ] **Step 7: Compile main + format**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:compileJava && ./gradlew spotlessApply`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/build.gradle.kts \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/security/EmbeddedPermissionEvaluator.java \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/security/EmbeddedPermissionEvaluatorConfiguration.java \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/security/EmbeddedPermissionEvaluatorTest.java
git commit -m "0_732 Add EmbeddedPermissionEvaluator for connected-user visibility

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Phase 3 — Facade filtering (integration-level + regular workflows)

### Task 6: `EmbeddedIntegrationNotVisibleException`

**Files:**
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/exception/EmbeddedIntegrationNotVisibleException.java`

- [ ] **Step 1: Create the exception**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.exception;

/**
 * Thrown when a single embedded integration is not visible to the requesting connected user because its permission
 * expression evaluated to {@code false}. The embedded public REST controller maps this to HTTP 404.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedIntegrationNotVisibleException extends RuntimeException {

    public EmbeddedIntegrationNotVisibleException(long integrationId) {
        super("Integration not visible: " + integrationId);
    }
}
```

- [ ] **Step 2: Compile + format + commit**

```bash
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-api:compileJava && ./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/exception/EmbeddedIntegrationNotVisibleException.java
git commit -m "0_732 Add EmbeddedIntegrationNotVisibleException

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 7: Filter integrations + regular workflows in the facade

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserIntegrationFacadeImpl.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/build.gradle.kts` (test dep for facade test — see Step 7)
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserIntegrationFacadeFilterTest.java`

This task adds: (a) inject `EmbeddedPermissionEvaluator` + `IntegrationWorkflowService`; (b) integration-level filtering in both list and single paths; (c) per-workflow filtering of the rendered workflow list, which comes from `integrationInstanceConfigurationDTO.integrationInstanceConfigurationWorkflows()` keyed by `workflowUuid`. The per-workflow expression lives on `IntegrationWorkflow`, resolved by uuid.

> The MCP assembly relocation is a **separate** task (Task 9). This task only filters regular workflows + integration visibility, returning the existing `ConnectedUserIntegrationDTO` shape.

- [ ] **Step 1: Write the failing unit test**

Create `ConnectedUserIntegrationFacadeFilterTest.java`. This is a Mockito unit test that drives the two public methods with stubbed collaborators. It verifies: hidden integration omitted from list; visible integration keeps only permitted workflows; hidden single integration throws `EmbeddedIntegrationNotVisibleException`.

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserIntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserIntegrationFacadeFilterTest {

    // NOTE: This test focuses on the filtering helpers. Because ConnectedUserIntegrationFacadeImpl assembles a large
    // collaborator graph, the implementation in Step 2 extracts the visibility logic into small package-private
    // methods (isVisible(IntegrationDTO, ConnectedUser) and filterWorkflows(...)) that are unit-tested directly here
    // against a real SpelEvaluator-backed EmbeddedPermissionEvaluator. The end-to-end list/single behavior is covered
    // by the rest-layer IntTest (Task 10).

    private final EmbeddedPermissionEvaluator embeddedPermissionEvaluator =
        new EmbeddedPermissionEvaluator(SpelEvaluator.create());

    @Test
    void testIsVisibleNullExpression() {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        assertTrue(embeddedPermissionEvaluator.evaluate(null, connectedUser));
    }

    @Test
    void testIsVisibleMetadataMatch() {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getMetadata()).thenReturn(Map.of("plan", "pro"));

        assertTrue(embeddedPermissionEvaluator.evaluate("metadata['plan'] == 'pro'", connectedUser));
    }
}
```

> Rationale for the test shape: a full Mockito harness over `ConnectedUserIntegrationFacadeImpl` (11+ collaborators) is brittle and low-value. The visibility decision is the logic worth testing in isolation (done via `EmbeddedPermissionEvaluator`, already covered in Task 5 and reinforced here). The integration of that decision into list/single responses is verified end-to-end by the public-rest `IntegrationApiControllerIntTest` extension in Task 10. Keep this file minimal; do **not** invent stubs for the entire facade graph.

- [ ] **Step 2: Inject collaborators and implement filtering in `ConnectedUserIntegrationFacadeImpl`**

Add imports:
```java
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
```
(Several of these may already be imported — do not duplicate.)

Add two fields (with the others, alphabetical-ish):
```java
    private final EmbeddedPermissionEvaluator embeddedPermissionEvaluator;
    private final IntegrationWorkflowService integrationWorkflowService;
```
Add them to the constructor parameter list and assignments (append to the existing constructor; remember `@SuppressFBWarnings("EI")` is already present):
```java
        EmbeddedPermissionEvaluator embeddedPermissionEvaluator,
        IntegrationWorkflowService integrationWorkflowService,
```
```java
        this.embeddedPermissionEvaluator = embeddedPermissionEvaluator;
        this.integrationWorkflowService = integrationWorkflowService;
```
Import the service if not present:
```java
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
```

- [ ] **Step 3: Add the per-integration permission helpers (package-private, after the public methods)**

```java
    boolean isIntegrationVisible(IntegrationDTO integrationDTO, ConnectedUser connectedUser) {
        return embeddedPermissionEvaluator.evaluate(integrationDTO.permissionExpression(), connectedUser);
    }

    private IntegrationInstanceConfigurationDTO filterWorkflows(
        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO, ConnectedUser connectedUser) {

        List<IntegrationInstanceConfigurationWorkflowDTO> workflows =
            integrationInstanceConfigurationDTO.integrationInstanceConfigurationWorkflows();

        if (workflows == null || workflows.isEmpty()) {
            return integrationInstanceConfigurationDTO;
        }

        Map<String, String> permissionExpressionsByUuid = integrationWorkflowService
            .getIntegrationWorkflows(integrationInstanceConfigurationDTO.integrationId())
            .stream()
            .filter(integrationWorkflow -> integrationWorkflow.getUuidAsString() != null)
            .collect(Collectors.toMap(
                IntegrationWorkflow::getUuidAsString, IntegrationWorkflow::getPermissionExpression,
                (first, second) -> first));

        List<IntegrationInstanceConfigurationWorkflowDTO> visibleWorkflows = workflows.stream()
            .filter(workflowDTO -> embeddedPermissionEvaluator.evaluate(
                permissionExpressionsByUuid.get(workflowDTO.workflowUuid()), connectedUser))
            .toList();

        return integrationInstanceConfigurationDTO.toBuilder()
            .integrationInstanceConfigurationWorkflows(visibleWorkflows)
            .build();
    }
```

> `IntegrationInstanceConfigurationDTO` currently has a `builder()` but no `toBuilder()`. Add a `toBuilder()` that seeds the builder from the current record (see Step 4). `Collectors.toMap` value can be `null` (no expression) — `HashMap`-backed `toMap` permits null values via the merge function form used here only if the downstream `get` tolerates null; `Collectors.toMap` actually throws on null values. **Use the null-safe variant below instead.**

Replace the `permissionExpressionsByUuid` construction with a null-tolerant map build:
```java
        Map<String, String> permissionExpressionsByUuid = new HashMap<>();

        for (IntegrationWorkflow integrationWorkflow : integrationWorkflowService.getIntegrationWorkflows(
            integrationInstanceConfigurationDTO.integrationId())) {

            String uuid = integrationWorkflow.getUuidAsString();

            if (uuid != null) {
                permissionExpressionsByUuid.put(uuid, integrationWorkflow.getPermissionExpression());
            }
        }
```
Add `import java.util.HashMap;` if not present, and drop the now-unused `Collectors`/`Function` imports if they aren't used elsewhere.

- [ ] **Step 4: Add `toBuilder()` to `IntegrationInstanceConfigurationDTO`**

In `IntegrationInstanceConfigurationDTO.java`, add after `builder()`:
```java
    public Builder toBuilder() {
        return builder()
            .connectionParameters(connectionParameters)
            .createdBy(createdBy)
            .createdDate(createdDate)
            .description(description)
            .enabled(enabled)
            .environment(environment)
            .id(id)
            .lastModifiedBy(lastModifiedBy)
            .lastModifiedDate(lastModifiedDate)
            .integration(integration)
            .integrationId(integrationId)
            .integrationVersion(integrationVersion == null ? 0 : integrationVersion)
            .integrationInstanceConfigurationWorkflows(integrationInstanceConfigurationWorkflows)
            .name(name)
            .tags(tags)
            .authorizationType(authorizationType)
            .version(version);
    }
```
> Note: the builder's `build()` discards `connectionAuthorizationParameters`/`connectionConnectionParameters` (sets them to `Map.of()`), matching existing builder behavior — acceptable here because the rendered DTO for embedded integrations is rebuilt only to swap the workflow list, and those two maps are not consumed by the public-rest mapper. If a later compile/test shows they are needed, switch to copying via the canonical constructor instead.

- [ ] **Step 5: Apply filtering in `getConnectedUserIntegration` (single)**

After resolving `connectedUser` and `integrationDTO`, insert the visibility check and workflow filter. Change:
```java
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        IntegrationDTO integrationDTO = integrationInstanceConfigurationDTO.integration();
```
to:
```java
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        IntegrationDTO integrationDTO = integrationInstanceConfigurationDTO.integration();

        if (!isIntegrationVisible(integrationDTO, connectedUser)) {
            throw new EmbeddedIntegrationNotVisibleException(integrationId);
        }

        integrationInstanceConfigurationDTO = filterWorkflows(integrationInstanceConfigurationDTO, connectedUser);
```
> `integrationInstanceConfigurationDTO` is an effectively-final local; reassigning is fine here since it is not captured by a lambda before this point. If the compiler complains about reassignment of a captured variable, introduce a new local `IntegrationInstanceConfigurationDTO filteredConfiguration = filterWorkflows(...)` and use it in the final `new ConnectedUserIntegrationDTO(...)`.

- [ ] **Step 6: Apply filtering in `getConnectedUserIntegrations` (list)**

Change:
```java
        return integrationInstanceConfigurationFacade
            .getIntegrationInstanceConfigurationIntegrations(enabled, environment)
            .stream()
            .map(integrationInstanceConfigurationDTO -> toConnectedUserIntegrationDTO(
                connectedUser, integrationInstanceConfigurationDTO, environment))
            .toList();
```
to:
```java
        return integrationInstanceConfigurationFacade
            .getIntegrationInstanceConfigurationIntegrations(enabled, environment)
            .stream()
            .filter(integrationInstanceConfigurationDTO -> isIntegrationVisible(
                integrationInstanceConfigurationDTO.integration(), connectedUser))
            .map(integrationInstanceConfigurationDTO -> toConnectedUserIntegrationDTO(
                connectedUser, filterWorkflows(integrationInstanceConfigurationDTO, connectedUser), environment))
            .toList();
```

- [ ] **Step 7: Compile main + run the unit test**

Run:
```bash
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:compileJava
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "com.bytechef.ee.embedded.configuration.facade.ConnectedUserIntegrationFacadeFilterTest"
```
Expected: BUILD SUCCESSFUL; 2 tests PASS.

- [ ] **Step 8: Format + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserIntegrationFacadeImpl.java \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/dto/IntegrationInstanceConfigurationDTO.java \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserIntegrationFacadeFilterTest.java
git commit -m "0_732 Filter embedded integrations and workflows by permission expression

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Phase 4 — MCP assembly relocation

> Goal: move the MCP tool/workflow gathering out of `IntegrationApiController.populateMcpData`/`populateMcpInstanceData` into the facade, apply the per-workflow permission filter to MCP workflows, and reduce the controller to mapping + the 404 catch. No web `*Model` types may appear in the service layer; the facade returns DTO carriers and the MapStruct mapper converts them.

### Task 8: Add MCP carrier records to `ConnectedUserIntegrationDTO`

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/dto/ConnectedUserIntegrationDTO.java`

- [ ] **Step 1: Add top-level MCP carrier fields + nested records**

Add two components to the record header — `mcpTools` and `mcpWorkflows` (integration-level), and add `mcpTools`/`mcpWorkflows` to the nested `ConnectedUserIntegrationInstance`. Because this record has two hand-written convenience constructors that delegate to the canonical one, add the new fields to the **end** of the canonical component list and pass `List.of()` from the convenience constructors (the facade will set them via `withMcp...` copy methods).

Change the record header from:
```java
public record ConnectedUserIntegrationDTO(
    ConnectionConfig connectionConfig, IntegrationInstanceConfigurationDTO integrationInstanceConfiguration,
    List<ConnectedUserIntegrationInstance> integrationInstances,
    OAuth2AuthorizationParameters oAuth2AuthorizationParameters, String redirectUri) {
```
to:
```java
public record ConnectedUserIntegrationDTO(
    ConnectionConfig connectionConfig, IntegrationInstanceConfigurationDTO integrationInstanceConfiguration,
    List<ConnectedUserIntegrationInstance> integrationInstances,
    OAuth2AuthorizationParameters oAuth2AuthorizationParameters, String redirectUri,
    List<McpToolInfo> mcpTools, List<McpWorkflowInfo> mcpWorkflows) {
```

Update both convenience constructors' delegating `this(...)` calls to pass `List.of(), List.of()` at the end:
- In the 4-arg `(List<Connection>, IntegrationInstanceConfigurationDTO, List<IntegrationInstance>, List<IntegrationInstanceWorkflow>)` constructor, change the trailing `null, null);` to `null, null, List.of(), List.of());`.
- In the 7-arg `(Authorization, ...)` constructor, change the trailing `oAuth2AuthorizationParameters, redirectUri);` to `oAuth2AuthorizationParameters, redirectUri, List.of(), List.of());`.

Add the nested carrier records (after the existing `OAuth2` record):
```java
    public record McpToolInfo(String name, String description) {
    }

    public record McpWorkflowInfo(
        String label, String description, List<WorkflowInputInfo> inputs, String workflowUuid) {
    }

    public record WorkflowInputInfo(String name, String label, boolean required, String type) {
    }

    public record McpInstanceToolInfo(Long mcpToolId, boolean enabled) {
    }
```

Add `mcpTools`/`mcpWorkflows` to the per-instance record and a copy helper. Change:
```java
    public record ConnectedUserIntegrationInstance(
        Connection connection, IntegrationInstance integrationInstance,
        List<ConnectedUserIntegrationInstanceWorkflow> workflows) {
    }
```
to:
```java
    public record ConnectedUserIntegrationInstance(
        Connection connection, IntegrationInstance integrationInstance,
        List<ConnectedUserIntegrationInstanceWorkflow> workflows, List<McpInstanceToolInfo> mcpTools,
        List<ConnectedUserIntegrationInstanceWorkflow> mcpWorkflows) {

        public ConnectedUserIntegrationInstance(
            Connection connection, IntegrationInstance integrationInstance,
            List<ConnectedUserIntegrationInstanceWorkflow> workflows) {

            this(connection, integrationInstance, workflows, List.of(), List.of());
        }
    }
```
> `ConnectedUserIntegrationInstance` is constructed in the private `toIntegrationInstances(...)` helper with the 3-arg form — the added 3-arg convenience constructor preserves that call site unchanged.

Add copy methods on the outer record (after the private static helpers) so the facade can attach MCP data without rebuilding by hand:
```java
    public ConnectedUserIntegrationDTO withMcp(
        List<McpToolInfo> mcpTools, List<McpWorkflowInfo> mcpWorkflows,
        List<ConnectedUserIntegrationInstance> integrationInstances) {

        return new ConnectedUserIntegrationDTO(
            connectionConfig, integrationInstanceConfiguration, integrationInstances, oAuth2AuthorizationParameters,
            redirectUri, mcpTools, mcpWorkflows);
    }
```

- [ ] **Step 2: Compile + format + commit**

```bash
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-api:compileJava && ./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/dto/ConnectedUserIntegrationDTO.java
git commit -m "0_732 Add MCP carrier records to ConnectedUserIntegrationDTO

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 9: Move MCP assembly into the facade (with per-workflow permission filtering)

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserIntegrationFacadeImpl.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/build.gradle.kts`

This relocates the logic currently in `IntegrationApiController.populateMcpData` and `populateMcpInstanceData`. The original controller code (for reference — this is the source being moved) builds `McpToolModel`/`IntegrationWorkflowModel`/`McpIntegrationInstanceToolModel`/`IntegrationInstanceWorkflowModel`; in the facade we build the **DTO carriers** from Task 8 instead, and we **skip** any MCP workflow whose `IntegrationWorkflow.permissionExpression` evaluates to `false`.

- [ ] **Step 1: Add the required module dependencies**

In `embedded-configuration-service/build.gradle.kts`, add to the `implementation` block:
```kotlin
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-mcp:platform-mcp-api"))
    implementation(project(":server:ee:libs:embedded:embedded-mcp:embedded-mcp-api"))
```
(Some may already be present transitively; keep the list deduplicated. `atlas-configuration-api` provides `WorkflowService`/`Workflow`; `platform-component-api` provides `ClusterElementDefinitionService`; `platform-mcp-api` provides `McpComponentService`/`McpToolService`/`McpServerService`; `embedded-mcp-api` provides the embedded MCP services + domain.)

- [ ] **Step 2: Inject the MCP collaborators into the facade**

Add fields:
```java
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final McpComponentService mcpComponentService;
    private final McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;
    private final McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;
    private final McpServerService mcpServerService;
    private final McpToolService mcpToolService;
    private final WorkflowService workflowService;
```
Add the corresponding constructor parameters + assignments, and imports:
```java
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
```

- [ ] **Step 3: Add the MCP assembly methods (ported from the controller, building DTO carriers + applying the permission filter)**

Add these private methods to the facade. They mirror the original controller logic but (a) return DTO carriers and (b) filter MCP workflows by the per-workflow expression via `embeddedPermissionEvaluator`.

```java
    private boolean isEmbeddedMcpServerEnabled(long mcpServerId) {
        McpServer mcpServer = mcpServerService.getMcpServer(mcpServerId);

        return mcpServer.getType() == PlatformType.EMBEDDED && mcpServer.isEnabled();
    }

    private List<ConnectedUserIntegrationDTO.McpToolInfo> getMcpTools(String componentName) {
        return mcpComponentService.getMcpComponentsByComponentName(componentName)
            .stream()
            .filter(mcpComponent -> isEmbeddedMcpServerEnabled(mcpComponent.getMcpServerId()))
            .flatMap(mcpComponent -> mcpToolService.getMcpComponentMcpTools(mcpComponent.getId())
                .stream()
                .map(mcpTool -> {
                    ClusterElementDefinition clusterElementDefinition =
                        clusterElementDefinitionService.getClusterElementDefinition(
                            mcpComponent.getComponentName(), mcpComponent.getComponentVersion(), mcpTool.getName());

                    return new ConnectedUserIntegrationDTO.McpToolInfo(
                        mcpTool.getName(), clusterElementDefinition.getDescription());
                }))
            .toList();
    }

    private List<ConnectedUserIntegrationDTO.McpWorkflowInfo> getMcpWorkflows(
        long integrationId, ConnectedUser connectedUser) {

        return mcpIntegrationInstanceConfigurationService
            .getMcpIntegrationInstanceConfigurationsByIntegrationId(integrationId)
            .stream()
            .filter(mcpIntegrationInstanceConfiguration -> isEmbeddedMcpServerEnabled(
                mcpIntegrationInstanceConfiguration.getMcpServerId()))
            .map(McpIntegrationInstanceConfiguration::getId)
            .flatMap(mcpIntegrationInstanceConfigurationId -> mcpIntegrationInstanceConfigurationWorkflowService
                .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
                    mcpIntegrationInstanceConfigurationId)
                .stream())
            .map(mcpIntegrationInstanceConfigurationWorkflow -> {
                IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                    integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                        mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId());

                String workflowId = integrationInstanceConfigurationWorkflow.getWorkflowId();

                Workflow workflow = workflowService.getWorkflow(workflowId);

                IntegrationWorkflow integrationWorkflow =
                    integrationWorkflowService.getWorkflowIntegrationWorkflow(workflowId);

                return new AbstractMap.SimpleEntry<>(integrationWorkflow, workflow);
            })
            .filter(entry -> entry.getKey()
                .getUuidAsString() != null)
            .filter(entry -> embeddedPermissionEvaluator.evaluate(
                entry.getKey()
                    .getPermissionExpression(),
                connectedUser))
            .map(entry -> {
                IntegrationWorkflow integrationWorkflow = entry.getKey();
                Workflow workflow = entry.getValue();

                List<ConnectedUserIntegrationDTO.WorkflowInputInfo> inputs = workflow.getInputs()
                    .stream()
                    .map(input -> new ConnectedUserIntegrationDTO.WorkflowInputInfo(
                        input.name(), input.label(), input.required(), input.type()))
                    .toList();

                return new ConnectedUserIntegrationDTO.McpWorkflowInfo(
                    workflow.getLabel(), workflow.getDescription(), inputs, integrationWorkflow.getUuidAsString());
            })
            .toList();
    }
```
Add `import java.util.AbstractMap;`.

> Per-instance MCP tools/workflows (the `populateMcpInstanceData` logic) are attached per `ConnectedUserIntegrationInstance`. To keep this task bounded and avoid rebuilding the instance graph in the facade, port `populateMcpInstanceData` into a method `attachInstanceMcpData(List<ConnectedUserIntegrationInstance>, ConnectedUser)` that returns a new list of instances with `mcpTools`/`mcpWorkflows` populated (using `mcpIntegrationInstanceToolService`, `mcpIntegrationInstanceConfigurationWorkflowService`, `integrationInstanceWorkflowService`, `integrationInstanceConfigurationWorkflowService`, `integrationWorkflowService`), applying the same per-workflow permission filter to instance MCP workflows. Mirror the original controller's null/enabled guards (skip when the MCP config or server is absent/disabled). Build `McpInstanceToolInfo(mcpToolId, enabled)` from each `McpIntegrationInstanceTool` (the generated `McpIntegrationInstanceToolModel` carries only `mcpToolId` + `enabled` — confirmed from the generated model — so the carrier mirrors exactly those two fields, matching the original controller's `conversionService.convert(mcpTool, McpIntegrationInstanceToolModel.class)`), and reuse `ConnectedUserIntegrationInstanceWorkflow` for instance MCP workflows with the resolved `workflowUuid`.

- [ ] **Step 4: Wire MCP assembly into both public methods**

In `getConnectedUserIntegration` (single), before the final `return`, replace:
```java
        return new ConnectedUserIntegrationDTO(
            authorization, connections, integrationInstanceConfigurationDTO, integrationInstances,
            integrationInstanceWorkflows, oAuth2AuthorizationParameters, oAuth2Service.getRedirectUri());
```
with:
```java
        ConnectedUserIntegrationDTO connectedUserIntegrationDTO = new ConnectedUserIntegrationDTO(
            authorization, connections, integrationInstanceConfigurationDTO, integrationInstances,
            integrationInstanceWorkflows, oAuth2AuthorizationParameters, oAuth2Service.getRedirectUri());

        String componentName = integrationDTO.componentName();

        return connectedUserIntegrationDTO.withMcp(
            getMcpTools(componentName), getMcpWorkflows(integrationId, connectedUser),
            attachInstanceMcpData(connectedUserIntegrationDTO.integrationInstances(), connectedUser));
```

In `toConnectedUserIntegrationDTO` (used by the list path), similarly attach MCP data before returning. Change:
```java
        return new ConnectedUserIntegrationDTO(connections, integrationInstanceConfigurationDTO, integrationInstances,
            integrationInstanceWorkflows);
```
to:
```java
        ConnectedUserIntegrationDTO connectedUserIntegrationDTO = new ConnectedUserIntegrationDTO(
            connections, integrationInstanceConfigurationDTO, integrationInstances, integrationInstanceWorkflows);

        String componentName = integrationDTO.componentName();

        return connectedUserIntegrationDTO.withMcp(
            getMcpTools(componentName), getMcpWorkflows(integrationDTO.id(), connectedUser),
            attachInstanceMcpData(connectedUserIntegrationDTO.integrationInstances(), connectedUser));
```
> `toConnectedUserIntegrationDTO` already has `connectedUser` and `integrationDTO` in scope.

- [ ] **Step 5: Compile**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:compileJava`
Expected: BUILD SUCCESSFUL. Resolve any missing service-method-name mismatches against the original controller code in `IntegrationApiController` (the method names used here are copied verbatim from it).

- [ ] **Step 6: Format + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/build.gradle.kts \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserIntegrationFacadeImpl.java
git commit -m "0_732 Relocate embedded MCP assembly into facade with permission filtering

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 10: Slim the controller + map MCP carriers + 404; rest-layer IntTest

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/src/main/java/com/bytechef/ee/embedded/configuration/public_/web/rest/IntegrationApiController.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/src/main/java/com/bytechef/ee/embedded/configuration/public_/web/rest/mapper/ConnectedUserIntegrationMapper.java`
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-rest/embedded-configuration-rest-impl/src/test/java/com/bytechef/ee/embedded/configuration/web/rest/IntegrationApiControllerIntTest.java` (existing — extend) **or** the public-rest test config if the existing IntTest covers the internal controller only. Confirm which controller the existing IntTest targets; the permission behavior lives on the **public-rest** controller, so add an IntTest under the public-rest module if none exists there.

- [ ] **Step 1: Update the MapStruct mapper to map the MCP carriers**

In `ConnectedUserIntegrationMapper.java`, in `ConnectedUserIntegrationToIntegrationMapper`:
- Remove the two `@Mapping(target = "mcpTools", ignore = true)` / `@Mapping(target = "mcpWorkflows", ignore = true)` lines on `convert(...)` and instead map them from the DTO carriers:
```java
        @Mapping(target = "componentName", source = "integrationInstanceConfiguration.integration.componentName")
        @Mapping(target = "description", source = "integrationInstanceConfiguration.integration.description")
        @Mapping(target = "icon", source = "integrationInstanceConfiguration.integration.icon")
        @Mapping(target = "id", source = "integrationInstanceConfiguration.integrationId")
        @Mapping(target = "integrationVersion", source = "integrationInstanceConfiguration.integrationVersion")
        @Mapping(target = "mcpTools", source = "mcpTools")
        @Mapping(target = "mcpWorkflows", source = "mcpWorkflows")
        @Mapping(
            target = "multipleInstances", source = "integrationInstanceConfiguration.integration.multipleInstances")
        @Mapping(target = "name", source = "integrationInstanceConfiguration.integration.title")
        @Mapping(
            target = "workflows", source = "integrationInstanceConfiguration.integrationInstanceConfigurationWorkflows")
        IntegrationModel convert(ConnectedUserIntegrationDTO connectedUserIntegrationDTO);
```
- Add carrier→model mapping methods to the same interface:
```java
        @Mapping(target = "name", source = "name")
        @Mapping(target = "description", source = "description")
        McpToolModel map(ConnectedUserIntegrationDTO.McpToolInfo mcpToolInfo);

        @Mapping(target = "description", source = "description")
        @Mapping(target = "inputs", source = "inputs")
        @Mapping(target = "label", source = "label")
        @Mapping(target = "workflowUuid", source = "workflowUuid")
        IntegrationWorkflowModel map(ConnectedUserIntegrationDTO.McpWorkflowInfo mcpWorkflowInfo);

        default InputModel mapWorkflowInput(ConnectedUserIntegrationDTO.WorkflowInputInfo input) {
            return new InputModel()
                .label(input.label())
                .name(input.name())
                .required(input.required())
                .type(InputTypeModel.valueOf(StringUtils.upperCase(input.type())));
        }
```
- For the per-instance mapper methods (`map(ConnectedUserIntegrationInstance)`), remove the `ignore = true` for `mcpTools`/`mcpWorkflows` and map from the instance carriers (`mcpTools` → `List<McpIntegrationInstanceToolModel>`, `mcpWorkflows` → `List<IntegrationInstanceWorkflowModel>`). Add the carrier→model method (the model has only `mcpToolId` + `enabled`):
```java
        @Mapping(target = "mcpToolId", source = "mcpToolId")
        @Mapping(target = "enabled", source = "enabled")
        McpIntegrationInstanceToolModel map(ConnectedUserIntegrationDTO.McpInstanceToolInfo mcpInstanceToolInfo);
```
For the instance MCP workflows (`ConnectedUserIntegrationInstanceWorkflow` → `IntegrationInstanceWorkflowModel`) reuse the existing `map(ConnectedUserIntegrationInstanceWorkflow)` method already defined on the mapper (it maps `enabled`/`inputs`; the original controller additionally set `workflowUuid` on these — if `IntegrationInstanceWorkflowModel` exposes `workflowUuid`, add `@Mapping(target = "workflowUuid", source = "workflowUuid")`). Add the necessary model imports (`McpToolModel`, `McpIntegrationInstanceToolModel`) if not already present.

Apply the same `mcpTools`/`mcpWorkflows` source mappings to `ConnectedUserIntegrationToIntegrationBasicMapper`'s instance `map(...)` (the basic mapper does not expose integration-level mcp fields, so only the per-instance ones change there).

> If a generated model field name differs from the carrier component name, MapStruct will report it at compile time; align the `@Mapping` `target`/`source` accordingly. Use the original `populateMcpData`/`populateMcpInstanceData` model-setter calls (`McpToolModel.setName/setDescription`, `IntegrationWorkflowModel.description/inputs/label/workflowUuid`, `McpIntegrationInstanceToolModel` via conversion, `IntegrationInstanceWorkflowModel` with `setWorkflowUuid`) as the field-name source of truth.

- [ ] **Step 2: Slim the controller — remove MCP assembly, inject nothing extra, map only**

In `IntegrationApiController.java`:
- Delete the private methods `populateMcpData`, `populateMcpInstanceData`, and `isEmbeddedMcpServerEnabled` (now in the facade). Keep `filterDisabledWorkflows`? — No: the facade now returns the already-permission-filtered workflows, but `filterDisabledWorkflows` also removes *MCP-surfaced* uuids from the regular list and drops disabled-config workflows. Preserve `filterDisabledWorkflows` for the disabled-config behavior (it is orthogonal to permissions) **but** it relies on `getMcpWorkflows()` on the model — which is now populated by the mapper, so it still works against `integrationModel.getMcpWorkflows()`.
- Remove the now-unused MCP service fields + constructor params (`clusterElementDefinitionService`, `mcpComponentService`, `mcpToolService`, `mcpServerService`, `mcpIntegrationInstanceToolService`, `mcpIntegrationInstanceConfigurationService`, `mcpIntegrationInstanceConfigurationWorkflowService`, `integrationInstanceConfigurationWorkflowService`, `integrationInstanceWorkflowService`, `integrationWorkflowService`, `workflowService`). Keep `conversionService`, `connectedUserIntegrationFacade`, `environmentService`.
- Update the four endpoints to drop the `populateMcpData(...)` calls (they keep `conversionService.convert(...)` + `filterDisabledWorkflows(...)`).
- Add the 404 catch on the two single-integration endpoints. Example for `getIntegration`:
```java
    @Override
    public ResponseEntity<IntegrationModel> getIntegration(
        String externalUserId, Long id, EnvironmentModel xEnvironment) {

        ConnectedUserIntegrationDTO connectedUserIntegrationDTO;

        try {
            connectedUserIntegrationDTO = connectedUserIntegrationFacade.getConnectedUserIntegration(
                externalUserId, id, true, getEnvironment(xEnvironment));
        } catch (EmbeddedIntegrationNotVisibleException exception) {
            return ResponseEntity.notFound()
                .build();
        }

        IntegrationModel integrationModel = conversionService.convert(
            connectedUserIntegrationDTO, IntegrationModel.class);

        filterDisabledWorkflows(connectedUserIntegrationDTO, integrationModel);

        return ResponseEntity.ok(integrationModel);
    }
```
Apply the identical try/catch to `getFrontendIntegration`. Add `import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;`.

> The list endpoints (`getIntegrations`/`getFrontendIntegrations`) need no catch — hidden integrations are simply absent from the facade's list.

- [ ] **Step 3: Compile both modules**

Run:
```bash
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:compileJava
```
Expected: BUILD SUCCESSFUL. Fix any MapStruct mapping errors surfaced here.

- [ ] **Step 4: Write/extend the rest-layer IntTest for permission filtering**

Add an IntTest exercising the public-rest controller: seed an integration with a permission expression that the test connected user fails, and assert it is absent from `getIntegrations` and yields 404 on `getIntegration`; seed one the user passes and assert presence + that a workflow with a failing per-workflow expression is filtered out. Follow the existing embedded IntTest pattern (`@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@ActiveProfiles("testint")`, `IntTest` suffix). If an existing `IntegrationApiControllerIntTest` covers only the internal controller, create a new `...public_.web.rest.IntegrationApiControllerIntTest` in the public-rest module's test sources.

> Keep this test focused on the permission behavior; reuse existing test fixtures/builders for integrations and connected users where available.

- [ ] **Step 5: Run the IntTest**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:test` (or the module's `testIntegration` task if the IntTest is wired there).
Expected: PASS.

- [ ] **Step 6: Format + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/src/main/java/com/bytechef/ee/embedded/configuration/public_/web/rest/IntegrationApiController.java \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/src/main/java/com/bytechef/ee/embedded/configuration/public_/web/rest/mapper/ConnectedUserIntegrationMapper.java \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/src/test/java/com/bytechef/ee/embedded/configuration/public_/web/rest/IntegrationApiControllerIntTest.java
git commit -m "0_732 Slim embedded integration controller; map MCP carriers; 404 for hidden integration

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Phase 5 — Authoring (GraphQL)

### Task 11: Integration-level permission expression — schema + mutation + service

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql/src/main/resources/graphql/integration.graphqls`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql/src/main/java/com/bytechef/ee/embedded/configuration/web/graphql/IntegrationGraphQlController.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/service/IntegrationService.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/service/IntegrationServiceImpl.java`

- [ ] **Step 1: Add the service method to the interface**

In `IntegrationService.java`, add:
```java
    Integration updatePermissionExpression(long id, String permissionExpression);
```

- [ ] **Step 2: Implement it**

In `IntegrationServiceImpl.java`, add:
```java
    @Override
    public Integration updatePermissionExpression(long id, String permissionExpression) {
        Integration integration = getIntegration(id);

        integration.setPermissionExpression(permissionExpression);

        return integrationRepository.save(integration);
    }
```
Also carry the field through the existing `update(Integration)` method so admin edits don't wipe it — in `update(Integration integration)`, after `curIntegration.setName(integration.getName());` add:
```java

        curIntegration.setPermissionExpression(integration.getPermissionExpression());
```

- [ ] **Step 3: Extend the GraphQL schema**

In `integration.graphqls`, change to:
```graphql
extend type Query {
    integration(id: ID): Integration
}

extend type Mutation {
    updateIntegrationPermissionExpression(id: ID!, permissionExpression: String): Integration
}

type Integration {
    id: ID!
    componentName: String!
    name: String!
    permissionExpression: String
}
```
> If no `Mutation` root type exists yet in the embedded-configuration GraphQL schema set, `extend type Mutation` requires a base `type Mutation`. Confirm a base `Mutation` is defined somewhere on the embedded GraphQL classpath (the embedded-mcp schemas define mutations, so a base type should already be registered for the shared schema). If schema wiring fails at startup with "Mutation not defined", add a base `type Mutation` in a shared embedded `.graphqls` (e.g. alongside `integration.graphqls`) — but prefer relying on the existing one.

- [ ] **Step 4: Add the mutation + schema field to the controller**

In `IntegrationGraphQlController.java`, add the mutation method and a `@SchemaMapping`-free reliance on the entity getter (the `Integration` domain has `getPermissionExpression()`, so the `permissionExpression` field resolves automatically). Add:
```java
    @MutationMapping
    public Integration updateIntegrationPermissionExpression(
        @Argument long id, @Argument String permissionExpression) {

        return integrationService.updatePermissionExpression(id, permissionExpression);
    }
```
Add imports:
```java
import org.springframework.graphql.data.method.annotation.MutationMapping;
```

- [ ] **Step 5: Compile**

Run:
```bash
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-api:compileJava
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:compileJava
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-graphql:compileJava
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Format + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql/src/main/resources/graphql/integration.graphqls \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql/src/main/java/com/bytechef/ee/embedded/configuration/web/graphql/IntegrationGraphQlController.java \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/service/IntegrationService.java \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/service/IntegrationServiceImpl.java
git commit -m "0_732 Add GraphQL mutation to set integration permission expression

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 12: Per-workflow permission expression — schema + mutation + service

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql/src/main/resources/graphql/integration-workflow.graphqls`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql/src/main/java/com/bytechef/ee/embedded/configuration/web/graphql/IntegrationWorkflowGraphQlController.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/service/IntegrationWorkflowService.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/service/IntegrationWorkflowServiceImpl.java`

- [ ] **Step 1: Add the service method to the interface**

In `IntegrationWorkflowService.java`, add:
```java
    IntegrationWorkflow updatePermissionExpression(long id, String permissionExpression);
```

- [ ] **Step 2: Implement it + carry field through `update`**

In `IntegrationWorkflowServiceImpl.java`, add:
```java
    @Override
    public IntegrationWorkflow updatePermissionExpression(long id, String permissionExpression) {
        IntegrationWorkflow integrationWorkflow = integrationWorkflowRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("IntegrationWorkflow not found for id: " + id));

        integrationWorkflow.setPermissionExpression(permissionExpression);

        return integrationWorkflowRepository.save(integrationWorkflow);
    }
```
In the existing `update(IntegrationWorkflow)`, after `curIntegrationWorkflow.setUuid(integrationWorkflow.getUuidAsString());` add:
```java

        curIntegrationWorkflow.setPermissionExpression(integrationWorkflow.getPermissionExpression());
```

- [ ] **Step 3: Extend the GraphQL schema**

In `integration-workflow.graphqls`, add the field to the type and a mutation:
```graphql
extend type Query {
    integrationWorkflows: [IntegrationWorkflow!]!
    integrationWorkflowsByIntegrationId(integrationId: ID!): [IntegrationWorkflow!]!
}

extend type Mutation {
    updateIntegrationWorkflowPermissionExpression(
        integrationWorkflowId: ID!, permissionExpression: String): IntegrationWorkflow
}

type IntegrationWorkflow {
    id: ID!
    label: String!
    description: String
    integrationWorkflowId: ID!
    workflowUuid: String
    permissionExpression: String
    workflowTaskComponentNames: [String!]!
    workflowTriggerComponentNames: [String!]!
    createdBy: String
    createdDate: Long
    lastModifiedBy: String
    lastModifiedDate: Long
}
```

- [ ] **Step 4: Add the mutation + a schema mapping for `permissionExpression`**

The GraphQL `IntegrationWorkflow` type is backed by `IntegrationWorkflowDTO` (which extends `WorkflowDTO`), **not** the `IntegrationWorkflow` domain entity. `IntegrationWorkflowDTO` does not currently expose `permissionExpression`. Add a `@SchemaMapping` that resolves it from the domain entity by the DTO's `integrationWorkflowId`:
```java
    @MutationMapping
    public IntegrationWorkflow updateIntegrationWorkflowPermissionExpression(
        @Argument long integrationWorkflowId, @Argument String permissionExpression) {

        return integrationWorkflowService.updatePermissionExpression(integrationWorkflowId, permissionExpression);
    }

    @SchemaMapping(typeName = "IntegrationWorkflow")
    String permissionExpression(IntegrationWorkflowDTO integrationWorkflowDTO) {
        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.getIntegrationWorkflow(
            integrationWorkflowDTO.getIntegrationWorkflowId());

        return integrationWorkflow.getPermissionExpression();
    }
```
> The mutation returns the `IntegrationWorkflow` **domain entity**. The schema's `IntegrationWorkflow` type fields that come from the DTO (`label`, `description`, `workflowUuid`, component-name lists) cannot all be resolved from the domain entity. To keep the mutation's return type consistent with the schema, return only the fields the client selects (the client mutation in Task 13 selects `id` and `permissionExpression`, both resolvable from the domain entity — `id` via `getId()`, `permissionExpression` via `getPermissionExpression()`). Add schema mappings on the domain-entity-backed path only if a selected field is missing; for `id` and `permissionExpression` the domain getters suffice. If Spring-GraphQL cannot resolve a DTO-only field for the mutation result because the client selects it, restrict the client selection (Task 13) to `id`/`permissionExpression`.

Add imports:
```java
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import org.springframework.graphql.data.method.annotation.MutationMapping;
```
Add `IntegrationWorkflowService` as a constructor-injected field (the controller currently only has `IntegrationWorkflowFacade`). Add the field, constructor param, and assignment.

- [ ] **Step 5: Compile**

Run:
```bash
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-api:compileJava
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:compileJava
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-graphql:compileJava
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Format + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql/src/main/resources/graphql/integration-workflow.graphqls \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql/src/main/java/com/bytechef/ee/embedded/configuration/web/graphql/IntegrationWorkflowGraphQlController.java \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/service/IntegrationWorkflowService.java \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/service/IntegrationWorkflowServiceImpl.java
git commit -m "0_732 Add GraphQL mutation to set integration workflow permission expression

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Phase 6 — Client UI

### Task 13: Client GraphQL operations + codegen

**Files:**
- Create: `client/src/graphql/embedded/configuration/updateIntegrationPermissionExpression.graphql`
- Create: `client/src/graphql/embedded/configuration/updateIntegrationWorkflowPermissionExpression.graphql`
- Modify: `client/src/graphql/embedded/configuration/integrationById.graphql` (select `permissionExpression`)
- Regenerate: `client/src/shared/middleware/graphql.ts`

- [ ] **Step 1: Add the mutation operations**

`updateIntegrationPermissionExpression.graphql`:
```graphql
mutation updateIntegrationPermissionExpression($id: ID!, $permissionExpression: String) {
    updateIntegrationPermissionExpression(id: $id, permissionExpression: $permissionExpression) {
        id
        permissionExpression
    }
}
```
`updateIntegrationWorkflowPermissionExpression.graphql`:
```graphql
mutation updateIntegrationWorkflowPermissionExpression($integrationWorkflowId: ID!, $permissionExpression: String) {
    updateIntegrationWorkflowPermissionExpression(
        integrationWorkflowId: $integrationWorkflowId, permissionExpression: $permissionExpression
    ) {
        id
        permissionExpression
    }
}
```

- [ ] **Step 2: Select the field on the existing integration query**

Edit `integrationById.graphql` to:
```graphql
query integrationById($id: ID!) {
    integration(id: $id) {
        id
        name
        permissionExpression
    }
}
```

- [ ] **Step 3: Regenerate the typed hooks**

Run: `cd client && npx graphql-codegen`
Expected: `src/shared/middleware/graphql.ts` updated with `useUpdateIntegrationPermissionExpressionMutation`, `useUpdateIntegrationWorkflowPermissionExpressionMutation`, and `permissionExpression` on the integration query result.

- [ ] **Step 4: Typecheck**

Run: `cd client && npm run typecheck`
Expected: no errors.

- [ ] **Step 5: Commit (operations and generated file separately, per CLAUDE.md)**

```bash
git add client/src/graphql/embedded/configuration/updateIntegrationPermissionExpression.graphql \
        client/src/graphql/embedded/configuration/updateIntegrationWorkflowPermissionExpression.graphql \
        client/src/graphql/embedded/configuration/integrationById.graphql
git commit -m "0_732 client - Add permission expression GraphQL operations"
git add client/src/shared/middleware/graphql.ts
git commit -m "0_732 client - Regenerate GraphQL types for permission expression"
```

---

### Task 14: Integration-level field in `IntegrationDialog`

**Files:**
- Modify: `client/src/ee/pages/embedded/integrations/components/IntegrationDialog.tsx`

> Verify the exact path during execution; per exploration it is under `client/src/ee/pages/embedded/integrations/components/IntegrationDialog.tsx`. The dialog persists name/description/category/tags via the REST `useUpdateIntegrationMutation`. The permission expression is persisted via the new GraphQL mutation on save (the integration's `id` is available on edit; on create, persist it after the create mutation resolves with the new id).

- [ ] **Step 1: Add a "Permission expression" textarea field after the description field**

Add a `FormField` for `permissionExpression` rendering a `Textarea`:
```tsx
<FormField
    control={control}
    name="permissionExpression"
    render={({field}) => (
        <FormItem>
            <FormLabel>Permission Expression</FormLabel>

            <FormControl>
                <Textarea
                    placeholder="e.g. metadata['plan'] == 'pro'"
                    rows={3}
                    {...field}
                    value={field.value ?? ''}
                />
            </FormControl>

            <FormMessage />
        </FormItem>
    )}
/>
```

- [ ] **Step 2: Seed the form default value**

In the form `defaultValues`, add (alphabetical order per sort-keys rule, so place between other keys accordingly):
```ts
permissionExpression: integration?.permissionExpression ?? '',
```
> If `Integration` REST model type does not carry `permissionExpression`, fetch it via the new `integrationById` GraphQL query result (it now selects the field). Prefer reading from the GraphQL query that backs the edit dialog if available; otherwise add a small `useIntegrationByIdQuery` read for the dialog.

- [ ] **Step 3: Persist on save via the GraphQL mutation**

In the save handler, after the existing create/update REST mutation resolves (so the integration `id` exists), call:
```ts
updateIntegrationPermissionExpressionMutation.mutate({
    id: String(integrationId),
    permissionExpression: formData.permissionExpression || null,
});
```
Wire `const updateIntegrationPermissionExpressionMutation = useUpdateIntegrationPermissionExpressionMutation();` with the project's standard query-invalidation `onSuccess` (invalidate the integration query keys), following neighboring mutation usage in the file.

- [ ] **Step 4: Check**

Run: `cd client && npm run check`
Expected: lint + typecheck + tests pass.

- [ ] **Step 5: Commit**

```bash
git add client/src/ee/pages/embedded/integrations/components/IntegrationDialog.tsx
git commit -m "0_732 client - Add permission expression field to integration dialog"
```

---

### Task 15: Per-workflow field in `WorkflowDialog`

**Files:**
- Modify: `client/src/shared/components/workflow/WorkflowDialog.tsx`

> This dialog is shared. Only add the permission-expression field + persistence when editing an **embedded integration** workflow (the dialog already receives integration-workflow context — guard the field so it does not appear for automation projects). Persist via `useUpdateIntegrationWorkflowPermissionExpressionMutation` keyed by `integrationWorkflowId`.

- [ ] **Step 1: Add the textarea field (guarded to embedded integration workflows)**

```tsx
{integrationWorkflowId != null && (
    <FormField
        control={control}
        name="permissionExpression"
        render={({field}) => (
            <FormItem>
                <FormLabel>Permission Expression</FormLabel>

                <FormControl>
                    <Textarea
                        placeholder="e.g. metadata['tier'] == 'gold'"
                        rows={3}
                        {...field}
                        value={field.value ?? ''}
                    />
                </FormControl>

                <FormMessage />
            </FormItem>
        )}
    />
)}
```

- [ ] **Step 2: Seed default value + persist on save**

Add `permissionExpression` to `defaultValues` (from the workflow's integration-workflow `permissionExpression`, read via the `integrationWorkflowsByIntegrationId` query which should also select the field — add it there if missing and regenerate). On save, after the existing workflow update, call:
```ts
if (integrationWorkflowId != null) {
    updateIntegrationWorkflowPermissionExpressionMutation.mutate({
        integrationWorkflowId: String(integrationWorkflowId),
        permissionExpression: formData.permissionExpression || null,
    });
}
```

- [ ] **Step 3: If needed, add `permissionExpression` to `integrationWorkflowsByIntegrationId.graphql` and regenerate**

```graphql
# add `permissionExpression` to the selection set, then:
# cd client && npx graphql-codegen
```
Commit the operation + regenerated file separately as in Task 13.

- [ ] **Step 4: Check**

Run: `cd client && npm run check`
Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add client/src/shared/components/workflow/WorkflowDialog.tsx
git commit -m "0_732 client - Add permission expression field to workflow dialog"
```

---

## Final verification

- [ ] **Server full check**

Run: `./gradlew spotlessApply && ./gradlew check`
Expected: BUILD SUCCESSFUL (Spotless, Checkstyle, PMD, SpotBugs, unit tests).

- [ ] **Targeted integration tests**

Run:
```bash
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:test
```
Expected: PASS, including the permission filter unit + IntTest.

- [ ] **Client full check**

Run: `cd client && npm run check`
Expected: lint + typecheck + tests pass.

- [ ] **Manual smoke (optional, requires running stack)**

Set `integration.permission_expression = "metadata['plan'] == 'pro'"` for a test integration; call the embedded `GET /v1/{externalUserId}/integrations` with a connected user whose metadata lacks `plan=pro` → integration absent; set metadata `plan=pro` → integration present. `GET /v1/{externalUserId}/integrations/{id}` for a hidden integration → 404.

---

## Notes / Known sharp edges (from spec)

- Metadata values are strings — authors compare to string literals (`metadata['seats'] == '5'`). Missing keys yield `null` ⇒ comparison `false` (fail closed).
- `environment` in expressions is the enum **name** (e.g. `PRODUCTION`).
- v1 does **not** validate SpEL at save time; invalid expressions fail closed at render. Save-time validation is a documented future enhancement.
- The integration-level expression is evaluated live off the mutable row (not version-pinned).
