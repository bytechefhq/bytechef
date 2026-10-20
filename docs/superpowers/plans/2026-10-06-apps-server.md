# Apps — Server (A1–A3) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the server side of Apps: an `App` registry with connection bindings and per-binding allow-lists, an app-scoped secret key, app-facing action / tool / proxy endpoints on `/api/app/v1`, invocation logging attributed to the app, and the admin UI.

**Architecture:** One new EE module `server/ee/libs/automation/automation-app/` (api / service / graphql / public-rest). Admin operations go through a gated `AppFacade` (GraphQL for the UI, REST under `/api/automation/v1/apps` for CLI and MCP). App calls authenticate through a second `ApiKeyAuthenticationFilter` whose principal is `AppPrincipal`, never a user, and execute through the existing platform facades (`ActionDefinitionFacade.executePerform`, `ClusterElementDefinitionFacade.executeTool`, `ContextFactory.createContext(...).http(...)`) on `syncWorkerExecutor`. Every call is recorded through `ToolExecutionRecorder` with a new `appId` dimension.

**Tech Stack:** Java 25 / Spring Boot 4, Spring Data JDBC, Liquibase, Spring Security (`AbstractApiKeyHttpConfigurer`), Spring GraphQL, openapi-generator (`spring`, `interfaceOnly`), React 19 + generated GraphQL hooks, Vitest.

**Spec:** `docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md` (sections "Design — A. Server", "Error handling summary", "Testing").

## One deviation from the spec to confirm

The spec says the proxy's 10 MB response cap is "enforced while streaming (`BodyHandlers.ofInputStream` with a bounded copy)". `HttpClientExecutor` is package-private and its body handlers are chosen internally; the only public way to attach a connection's credentials is `ContextFactory.createContext(componentName, componentConnection).http(...)`. Task 20 therefore requests `ResponseType.BINARY`, which makes the executor spool the body to `TempFileStorage`, and applies the cap with a bounded read of that file. Memory is bounded and the app never receives more than 10 MB; disk use during the download is bounded only by the 30 s request timeout. A true in-flight cap needs a new option on the SDK's `Context.Http.Configuration`, which this plan does not add. After the plan is approved, update the spec's "Limits" bullet to match.

## Global Constraints

- All files under `server/ee/` carry the ByteChef Enterprise header and `@version ee` Javadoc (verbatim header in Task 1).
- Enum ordinals are persisted as INT: append only, never reorder (`AppStatus`, `AppHostingTarget`, `AppOperationType`, `ToolExecutionSurface`, `ToolExecutionKind`).
- `workspace_id` is a nullable `BIGINT` column with a `Long` field; no `workspace_app` relation table.
- `binding_key` matches `^[a-z][a-z0-9_-]{0,63}$`.
- App key format: `TenantKey.of()` (so the tenant can be read from the key); stored as hex SHA-256 in `secret_key_hash` (64 chars); hint = last 8 characters with `=` padding stripped.
- `APP_VIEW` → `WorkspaceRole.VIEWER`, `APP_MANAGE` → `WorkspaceRole.ADMIN`. Every class in `…configuration.security.scope` carries `@ConditionalOnEEVersion` (`PermissionScopeProviderEditionGatingTest`).
- App-facing base path `${openapi.openAPIDefinition.base-path.app:/api/app}/v1`; the default is non-empty.
- App calls run on `syncWorkerExecutor` with timeout `bytechef.app.execution.timeout` (default 60 s). Proxy limits under `bytechef.app.proxy.*`: `max-request-bytes` 10485760, `max-response-bytes` 10485760, `timeout` 30 s. Defaults live in yml, never in Java field initializers.
- Flat tool name: `<BINDING_KEY>__<TOOL_NAME>` upper snake case, e.g. `SALES_SLACK__SEND_MESSAGE`.
- The key lookup on the request path is **not cached**.
- Commit messages: server `- <description>`, client `- client - <description>`. No `Co-Authored-By` or "Generated with" trailers. Run `./gradlew spotlessApply` before every server commit and `cd client && npm run format` before every client commit. Judge Gradle runs by redirecting to a file and checking `$?`, never through a pipe.
- Checkstyle: test method names are camelCase with no underscores; no `TODO:` comments; no empty blocks.

## Review Focus

Inputs the spec implies but no task's tests exercised at first; each now has a test in the task named.

1. **A request whose bearer value is not base64 / not a `TenantKey`** must be 401, not 500 — Task 12 (`AppApiKeyAuthenticationConverterTest.malformedKeyIsBadCredentials`).
2. **Two apps in the same tenant bind the same connection, one is disabled** — the other must still execute, and deleting the connection must still be refused because of the enabled one — Task 7 (`AppConnectionUsageCheckerTest.disabledAppsStillCountAsUsage`) and Task 15.
3. **`componentVersion` in the request that the component does not have** — 400 with the component's own `ConfigurationException`, not a 500 — Task 15 (`AppExecutionFacadeTest.unknownComponentVersionIsConfigurationError`).
4. **Proxy path that is valid after percent-decoding but encodes `..` (`/%2e%2e/b`) or a CRLF (`/a%0d%0ainjected: x`)** — rejected — Task 17 path table.
5. **Rotate while a request is in flight** — the in-flight request completes (it already resolved its app), the next request with the old key is 401 — Task 4 (`AppServiceIntTest.rotateInvalidatesOldHashOnly`).

---

## File structure

```
server/ee/libs/automation/automation-app/
  automation-app-api/
    build.gradle.kts
    src/main/java/com/bytechef/ee/automation/app/
      domain/App.java, AppStatus.java, AppHostingTarget.java, AppConnection.java,
             AppConnectionOperation.java, AppOperationType.java
      dto/AppDTO.java, AppConnectionDTO.java, AppSecretKeyDTO.java, AppToolDTO.java, AppBindingDTO.java
      exception/AppErrorType.java
      service/AppService.java, AppConnectionService.java
      facade/AppFacade.java, AppExecutionFacade.java
      security/AppPrincipal.java
      util/AppSecretKeys.java
      proxy/ProxyRequest.java, ProxyResponse.java
  automation-app-service/
    build.gradle.kts
    src/main/java/com/bytechef/ee/automation/app/
      config/AppJdbcRepositoryConfiguration.java, AppProperties.java
      repository/AppRepository.java, AppConnectionRepository.java
      service/AppServiceImpl.java, AppConnectionServiceImpl.java
      facade/AppFacadeImpl.java, AppExecutionFacadeImpl.java
      security/AppVisibilityPolicy.java, AppVisibilityProvider.java, AppOwnershipResolver.java,
               AppEnvironmentResolver.java, AppConnectionUsageChecker.java
      audit/AppAuditEvent.java, AppAuditPublisher.java
      proxy/AppProxyExecutor.java, ProxyPathValidator.java, ProxyHeaders.java
    src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
    src/main/resources/config/liquibase/changelog/automation/app/20261006000001_automation_app_init.xml
  automation-app-graphql/
    build.gradle.kts
    src/main/java/com/bytechef/ee/automation/app/web/graphql/AppGraphQlController.java
    src/main/resources/graphql/app.graphqls
  automation-app-public-rest/
    build.gradle.kts
    openapi.yaml                       (app-facing, servers /api/app/v1)
    openapi-management.yaml            (management, servers /api/automation/v1)
    generated/…                        (committed)
    src/main/java/com/bytechef/ee/automation/app/public_/web/rest/
      AppApiController.java (management), AppConnectionApiController.java, AppToolApiController.java,
      AppProxyApiController.java, AppRestExceptionHandler.java
server/ee/libs/automation/automation-security-web/automation-security-web-impl/
  src/main/java/com/bytechef/ee/automation/security/web/
    app/AppAuthenticationToken.java, AppApiKeyAuthenticationProvider.java,
        AppApiKeySecurityConfigurer.java, AppApiKeySecurityConfigurerContributor.java
server/libs/platform/platform-security-web/platform-security-web-api/…/AbstractApiKeyAuthenticationToken.java  (one new ctor)
server/libs/platform/platform-connection/platform-connection-api/…/usage/ConnectionUsageChecker.java            (new SPI)
server/libs/platform/platform-connection/platform-connection-service/…/facade/ConnectionFacadeImpl.java        (consume SPI)
server/libs/platform/platform-tool-execution/platform-tool-execution-api/…/ToolExecutionEvent.java, ToolExecutionSurface.java, ToolExecutionKind.java
server/ee/libs/platform/platform-tool-invocation-log/…                                                           (app_id column, listener, graphql arg)
server/ee/libs/automation/automation-configuration/automation-configuration-service/…/security/scope/AppPermissionScope.java, AppPermissionScopeProvider.java
client/src/graphql/automation/app/*.graphql
client/src/pages/automation/apps/Apps.tsx, App.tsx, components/…
```

Each sub-part ends in a working state: after A1 an admin can create, bind and rotate from the UI; after A2 an app can call actions and tools; after A3 the proxy works.

---

# Part A1 — model, key, admin surface

### Task 1: Module scaffolding, domain classes, schema

**Files:**
- Create: `server/ee/libs/automation/automation-app/automation-app-api/build.gradle.kts`
- Create: `server/ee/libs/automation/automation-app/automation-app-service/build.gradle.kts`
- Create: `server/ee/libs/automation/automation-app/automation-app-graphql/build.gradle.kts`
- Create: `server/ee/libs/automation/automation-app/automation-app-public-rest/build.gradle.kts` (dependencies only; the generator task comes in Task 10)
- Modify: `settings.gradle.kts` (after the line `include("server:ee:libs:automation:automation-workflow-alert:automation-workflow-alert-service")`)
- Modify: `server/apps/server-app/build.gradle.kts` (after the `automation-workflow-alert-service` line)
- Create: `automation-app-api/src/main/java/com/bytechef/ee/automation/app/domain/{App,AppStatus,AppHostingTarget,AppConnection,AppConnectionOperation,AppOperationType}.java`
- Create: `automation-app-service/src/main/java/com/bytechef/ee/automation/app/repository/{AppRepository,AppConnectionRepository}.java`
- Create: `automation-app-service/src/main/java/com/bytechef/ee/automation/app/config/AppJdbcRepositoryConfiguration.java`
- Create: `automation-app-service/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `automation-app-service/src/main/resources/config/liquibase/changelog/automation/app/20261006000001_automation_app_init.xml`
- Modify: `server/libs/config/liquibase-config/src/main/resources/config/liquibase/master.xml` (after the `automation/workflow_alert` `includeAll`)
- Test: `automation-app-service/src/test/java/com/bytechef/ee/automation/app/config/AppIntTestConfiguration.java`, `…/repository/AppRepositoryIntTest.java`, `automation-app-service/src/test/resources/config/application-testint.yml`

**Interfaces:**
- Produces: `App` aggregate (fields below, `getStatus()`/`setStatus(AppStatus)` etc.), `AppConnection` with `Set<AppConnectionOperation> operations`, `AppRepository extends ListCrudRepository<App, Long>` with `findAllByWorkspaceIdAndEnvironment(long, int)`, `findBySecretKeyHash(String)`, `findByWorkspaceIdAndEnvironmentAndName(long, int, String)`, `findByUuid(String)`, `touchSecretKeyLastUsedDate(long, Instant, Instant)`; `AppConnectionRepository extends ListCrudRepository<AppConnection, Long>` with `findAllByAppId(long)`, `findByAppIdAndBindingKey(long, String)`, `findAllByConnectionId(long)`.

Every Java file under `server/ee/` starts with:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */
```

and every class Javadoc ends with:

```java
/**
 * …
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
```

- [ ] **Step 1: Register the four submodules**

In `settings.gradle.kts`, after `include("server:ee:libs:automation:automation-workflow-alert:automation-workflow-alert-service")`:

```kotlin
include("server:ee:libs:automation:automation-app:automation-app-api")
include("server:ee:libs:automation:automation-app:automation-app-graphql")
include("server:ee:libs:automation:automation-app:automation-app-public-rest")
include("server:ee:libs:automation:automation-app:automation-app-service")
```

In `server/apps/server-app/build.gradle.kts`, after the `automation-workflow-alert-service` line:

```kotlin
    implementation(project(":server:ee:libs:automation:automation-app:automation-app-graphql"))
    implementation(project(":server:ee:libs:automation:automation-app:automation-app-public-rest"))
    implementation(project(":server:ee:libs:automation:automation-app:automation-app-service"))
```

- [ ] **Step 2: Build files**

`automation-app-api/build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:exception:exception-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(project(":server:libs:test:test-support"))
}
```

`automation-app-service/build.gradle.kts`:

```kotlin
dependencies {
    api(project(":server:ee:libs:automation:automation-app:automation-app-api"))

    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-actuator")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":sdks:backend:java:component-api"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:exception:exception-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-context:platform-component-context-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:libs:platform:platform-connection:platform-connection-api"))
    implementation(project(":server:libs:platform:platform-tool-execution:platform-tool-execution-api"))
    implementation(project(":server:ee:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:ee:libs:platform:platform-resource-grant:platform-resource-grant-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(project(":server:libs:config:jackson-config"))
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
    testImplementation(project(":server:libs:test:test-support"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
```

`automation-app-graphql/build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.graphql:spring-graphql")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:ee:libs:automation:automation-app:automation-app-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}
```

`automation-app-public-rest/build.gradle.kts` (the generator block is added in Task 10):

```kotlin
dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation(libs.io.swagger.core.v3.swagger.annotations)
    implementation(libs.org.openapitools.jackson.databind.nullable)
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:exception:exception-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:ee:libs:automation:automation-app:automation-app-api"))

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
}
```

- [ ] **Step 3: Enums**

`domain/AppStatus.java`:

```java
package com.bytechef.ee.automation.app.domain;

/**
 * Persisted as INT ordinal — append-only, never reorder.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AppStatus {

    ENABLED, DISABLED
}
```

`domain/AppHostingTarget.java` — same shape, values `VERCEL, SELF_HOSTED`.
`domain/AppOperationType.java` — same shape, values `ACTION, TOOL`.

- [ ] **Step 4: `App` aggregate**

`domain/App.java`:

```java
package com.bytechef.ee.automation.app.domain;

import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * An AI-built application that reaches third-party systems through ByteChef. The row holds the app's one secret key
 * as a hash; the key itself is shown once at create and rotate and never stored.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("app")
public class App {

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private @Nullable String description;

    @Column
    private int environment;

    @Column("hosting_target")
    private int hostingTarget;

    @Id
    private Long id;

    @Column("last_deployed_date")
    private @Nullable Instant lastDeployedDate;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column
    private String name;

    @Column("owner_user_id")
    private Long ownerUserId;

    @Column("secret_key_hash")
    private String secretKeyHash;

    @Column("secret_key_hint")
    private String secretKeyHint;

    @Column("secret_key_last_used_date")
    private @Nullable Instant secretKeyLastUsedDate;

    @Column
    private int status;

    @Column
    private @Nullable String url;

    @Column
    private String uuid;

    @Version
    private int version;

    @Column
    private int visibility;

    /**
     * Boxed on purpose: a primitive would collapse "no workspace" into workspace 0, which is a real workspace id.
     */
    @Column("workspace_id")
    private @Nullable Long workspaceId;

    public App() {
    }

    public com.bytechef.platform.configuration.domain.Environment getEnvironment() {
        return com.bytechef.platform.configuration.domain.Environment.values()[environment];
    }

    public void setEnvironment(com.bytechef.platform.configuration.domain.Environment environment) {
        this.environment = environment.ordinal();
    }

    public int getEnvironmentId() {
        return environment;
    }

    public AppHostingTarget getHostingTarget() {
        return AppHostingTarget.values()[hostingTarget];
    }

    public void setHostingTarget(AppHostingTarget hostingTarget) {
        this.hostingTarget = hostingTarget.ordinal();
    }

    public AppStatus getStatus() {
        return AppStatus.values()[status];
    }

    public void setStatus(AppStatus status) {
        this.status = status.ordinal();
    }

    public com.bytechef.platform.security.domain.ResourceVisibility getVisibility() {
        return com.bytechef.platform.security.domain.ResourceVisibility.values()[visibility];
    }

    public void setVisibility(com.bytechef.platform.security.domain.ResourceVisibility visibility) {
        this.visibility = visibility.ordinal();
    }

    // Plain getters and setters for every remaining field, in alphabetical order:
    // createdBy, createdDate, description, id, lastDeployedDate, lastModifiedBy, lastModifiedDate, name,
    // ownerUserId, secretKeyHash, secretKeyHint, secretKeyLastUsedDate, url, uuid, version, workspaceId.

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof App that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "App{id=" + id + ", name='" + name + "', workspaceId=" + workspaceId + ", environment=" + environment
            + ", status=" + status + '}';
    }
}
```

Write the enum-typed accessors exactly as above and generate the plain accessors (`getX()` / `setX(x)`) for the listed fields — they are the only ones; `toString` must never include `secretKeyHash`.

- [ ] **Step 5: `AppConnection` and its operations**

`domain/AppConnectionOperation.java`:

```java
package com.bytechef.ee.automation.app.domain;

import java.util.Objects;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * One allow-list entry of a binding. No {@code @Id}: the composite key is (app_connection_id, operation_type,
 * operation_name) in Liquibase.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("app_connection_operation")
public class AppConnectionOperation {

    @Column("operation_name")
    private String operationName;

    @Column("operation_type")
    private int operationType;

    private AppConnectionOperation() {
    }

    public AppConnectionOperation(AppOperationType operationType, String operationName) {
        this.operationType = operationType.ordinal();
        this.operationName = operationName;
    }

    public String getOperationName() {
        return operationName;
    }

    public AppOperationType getOperationType() {
        return AppOperationType.values()[operationType];
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AppConnectionOperation that)) {
            return false;
        }

        return operationType == that.operationType && Objects.equals(operationName, that.operationName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operationType, operationName);
    }
}
```

`domain/AppConnection.java`:

```java
package com.bytechef.ee.automation.app.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A connection bound to an app under a binding key, with the actions and tools the app may call through it.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("app_connection")
public class AppConnection {

    @Column("app_id")
    private AggregateReference<App, Long> appId;

    @Column("binding_key")
    private String bindingKey;

    @Column("connection_id")
    private Long connectionId;

    @Id
    private Long id;

    @MappedCollection(idColumn = "app_connection_id")
    private Set<AppConnectionOperation> operations = new HashSet<>();

    @Column("proxy_allowed_methods")
    private String proxyAllowedMethods = "";

    @Column("proxy_enabled")
    private boolean proxyEnabled;

    @Version
    private int version;

    public AppConnection() {
    }

    public Long getAppId() {
        return appId.getId();
    }

    public void setAppId(Long appId) {
        this.appId = AggregateReference.to(appId);
    }

    public boolean allows(AppOperationType type, String operationName) {
        return operations.contains(new AppConnectionOperation(type, operationName));
    }

    public List<String> getOperationNames(AppOperationType type) {
        return operations.stream()
            .filter(operation -> operation.getOperationType() == type)
            .map(AppConnectionOperation::getOperationName)
            .sorted()
            .toList();
    }

    public void setOperations(List<String> actionNames, List<String> toolNames) {
        operations = new HashSet<>();

        for (String actionName : actionNames) {
            operations.add(new AppConnectionOperation(AppOperationType.ACTION, actionName));
        }

        for (String toolName : toolNames) {
            operations.add(new AppConnectionOperation(AppOperationType.TOOL, toolName));
        }
    }

    public List<String> getProxyAllowedMethodList() {
        if (proxyAllowedMethods == null || proxyAllowedMethods.isBlank()) {
            return List.of();
        }

        return List.of(proxyAllowedMethods.split(","));
    }

    public void setProxyAllowedMethodList(List<String> methods) {
        this.proxyAllowedMethods = String.join(",", methods);
    }

    // Plain getters and setters: bindingKey, connectionId, id, proxyEnabled, version.

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AppConnection that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
```

- [ ] **Step 6: Repositories and JDBC config**

`repository/AppRepository.java`:

```java
package com.bytechef.ee.automation.app.repository;

import com.bytechef.ee.automation.app.domain.App;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface AppRepository extends ListCrudRepository<App, Long> {

    List<App> findAllByWorkspaceIdAndEnvironmentOrderByName(long workspaceId, int environment);

    Optional<App> findBySecretKeyHash(String secretKeyHash);

    Optional<App> findByUuid(String uuid);

    Optional<App> findByWorkspaceIdAndEnvironmentAndName(long workspaceId, int environment, String name);

    /**
     * Writes the last-used mark without touching {@code version} or the audit columns, and only when the stored
     * value is older than {@code cutoff}, so a busy app does not write on every request and never races an
     * admin's optimistic-locked save.
     */
    @Modifying
    @Query("""
        UPDATE app SET secret_key_last_used_date = :now
        WHERE id = :id
            AND (secret_key_last_used_date IS NULL OR secret_key_last_used_date < CAST(:cutoff AS TIMESTAMP))
        """)
    int touchSecretKeyLastUsedDate(@Param("id") long id, @Param("now") Instant now, @Param("cutoff") Instant cutoff);
}
```

`repository/AppConnectionRepository.java`:

```java
package com.bytechef.ee.automation.app.repository;

import com.bytechef.ee.automation.app.domain.AppConnection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface AppConnectionRepository extends ListCrudRepository<AppConnection, Long> {

    List<AppConnection> findAllByAppIdOrderByBindingKey(long appId);

    List<AppConnection> findAllByConnectionId(long connectionId);

    Optional<AppConnection> findByAppIdAndBindingKey(long appId, String bindingKey);
}
```

`config/AppJdbcRepositoryConfiguration.java`:

```java
package com.bytechef.ee.automation.app.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@AutoConfiguration(afterName = "org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration")
@ConditionalOnBean(AbstractJdbcConfiguration.class)
@EnableJdbcRepositories(basePackages = "com.bytechef.ee.automation.app.repository")
public class AppJdbcRepositoryConfiguration {
}
```

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
com.bytechef.ee.automation.app.config.AppJdbcRepositoryConfiguration
```

- [ ] **Step 7: Liquibase**

`config/liquibase/changelog/automation/app/20261006000001_automation_app_init.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <!--
        app is the registry of AI-built applications that reach third-party systems through ByteChef. The row holds
        the app's single secret key as a SHA-256 hash; the key itself is never stored. status, hosting_target and
        visibility are enum ordinals (append-only).
    -->
    <changeSet id="20261006000001-1" author="Ivica Cardic">
        <createTable tableName="app">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="uuid" type="VARCHAR(36)">
                <constraints nullable="false"/>
            </column>
            <column name="name" type="VARCHAR(256)">
                <constraints nullable="false"/>
            </column>
            <column name="description" type="TEXT"/>
            <column name="workspace_id" type="BIGINT"/>
            <column name="environment" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="owner_user_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="status" type="INT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
            <column name="visibility" type="INT" defaultValueNumeric="1">
                <constraints nullable="false"/>
            </column>
            <column name="hosting_target" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="url" type="VARCHAR(2048)"/>
            <column name="last_deployed_date" type="TIMESTAMP"/>
            <column name="secret_key_hash" type="VARCHAR(64)">
                <constraints nullable="false"/>
            </column>
            <column name="secret_key_hint" type="VARCHAR(16)">
                <constraints nullable="false"/>
            </column>
            <column name="secret_key_last_used_date" type="TIMESTAMP"/>
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
            <column name="version" type="INT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addUniqueConstraint tableName="app" columnNames="uuid" constraintName="uk_app_uuid"/>
        <addUniqueConstraint tableName="app" columnNames="secret_key_hash" constraintName="uk_app_secret_key_hash"/>
        <addUniqueConstraint tableName="app" columnNames="workspace_id, environment, name"
            constraintName="uk_app_workspace_environment_name"/>

        <createIndex tableName="app" indexName="idx_app_workspace_environment">
            <column name="workspace_id"/>
            <column name="environment"/>
        </createIndex>

        <createTable tableName="app_connection">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="app_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="connection_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="binding_key" type="VARCHAR(64)">
                <constraints nullable="false"/>
            </column>
            <column name="proxy_enabled" type="BOOLEAN" defaultValueBoolean="false">
                <constraints nullable="false"/>
            </column>
            <column name="proxy_allowed_methods" type="VARCHAR(64)" defaultValue="">
                <constraints nullable="false"/>
            </column>
            <column name="version" type="INT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addUniqueConstraint tableName="app_connection" columnNames="app_id, binding_key"
            constraintName="uk_app_connection_app_binding_key"/>

        <addForeignKeyConstraint
            baseTableName="app_connection"
            baseColumnNames="app_id"
            constraintName="fk_app_connection_app"
            referencedTableName="app"
            referencedColumnNames="id"
            onDelete="CASCADE"/>
        <!-- No cascade on purpose: deleting a bound connection is refused in ConnectionFacadeImpl. -->
        <addForeignKeyConstraint
            baseTableName="app_connection"
            baseColumnNames="connection_id"
            constraintName="fk_app_connection_connection"
            referencedTableName="connection"
            referencedColumnNames="id"/>

        <createIndex tableName="app_connection" indexName="idx_app_connection_connection">
            <column name="connection_id"/>
        </createIndex>

        <createTable tableName="app_connection_operation">
            <column name="app_connection_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="operation_type" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="operation_name" type="VARCHAR(256)">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addPrimaryKey tableName="app_connection_operation"
            columnNames="app_connection_id, operation_type, operation_name"/>

        <addForeignKeyConstraint
            baseTableName="app_connection_operation"
            baseColumnNames="app_connection_id"
            constraintName="fk_app_connection_operation_app_connection"
            referencedTableName="app_connection"
            referencedColumnNames="id"
            onDelete="CASCADE"/>
    </changeSet>
</databaseChangeLog>
```

In `master.xml`, directly after the `automation/workflow_alert` `includeAll`:

```xml
    <!--
        automation/app holds the Apps registry (AI-built apps calling actions, tools and a connection proxy through
        ByteChef). app_connection references connection(id), so this loads after platform/connection.
    -->
    <includeAll path="classpath:config/liquibase/changelog/automation/app" relativeToChangelogFile="false" errorIfMissingOrEmpty="false" contextFilter="mono or configuration or multitenant" />
```

- [ ] **Step 8: Write the failing IntTest**

`src/test/resources/config/application-testint.yml`:

```yaml
spring:
  liquibase:
    contexts: mono

bytechef:
  edition: ee
```

`src/test/java/com/bytechef/ee/automation/app/config/AppIntTestConfiguration.java`:

```java
package com.bytechef.ee.automation.app.config;

import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

/**
 * Repositories are not enabled here: the production {@code AppJdbcRepositoryConfiguration} auto-configuration is
 * gated on an {@code AbstractJdbcConfiguration} bean, which the nested class supplies.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = "com.bytechef.ee.automation.app.repository")
@EnableAutoConfiguration
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class
})
@Configuration
public class AppIntTestConfiguration {

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class AppIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
```

Because the changelog references `connection(id)`, the test classpath must carry the connection changelog: add `testImplementation(project(":server:libs:platform:platform-connection:platform-connection-service"))` to the service `build.gradle.kts` with the comment `// app_connection carries an FK to the connection table, so its changelog has to be on the test classpath.`

`src/test/java/com/bytechef/ee/automation/app/repository/AppRepositoryIntTest.java`:

```java
package com.bytechef.ee.automation.app.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.app.config.AppIntTestConfiguration;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AppIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class AppRepositoryIntTest {

    @Autowired
    private AppRepository appRepository;

    @AfterEach
    void afterEach() {
        appRepository.deleteAll();
    }

    @Test
    void savesAndReadsBackEnumsAndKeyColumns() {
        App app = newApp("crm-dashboard", "a".repeat(64));

        App saved = appRepository.save(app);

        App found = appRepository.findById(saved.getId())
            .orElseThrow();

        assertThat(found.getStatus()).isEqualTo(AppStatus.ENABLED);
        assertThat(found.getHostingTarget()).isEqualTo(AppHostingTarget.VERCEL);
        assertThat(found.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(found.getVisibility()).isEqualTo(ResourceVisibility.WORKSPACE);
        assertThat(found.getSecretKeyHash()).isEqualTo("a".repeat(64));
        assertThat(appRepository.findBySecretKeyHash("a".repeat(64))).isPresent();
    }

    @Test
    void touchSecretKeyLastUsedDateHonoursCutoffAndSkipsVersion() {
        App saved = appRepository.save(newApp("crm-dashboard", "b".repeat(64)));
        Instant now = Instant.now()
            .truncatedTo(ChronoUnit.MILLIS);

        int first = appRepository.touchSecretKeyLastUsedDate(saved.getId(), now, now.minusSeconds(60));
        int second = appRepository.touchSecretKeyLastUsedDate(
            saved.getId(), now.plusSeconds(1), now.plusSeconds(1)
                .minusSeconds(60));

        App found = appRepository.findById(saved.getId())
            .orElseThrow();

        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(0);
        assertThat(found.getSecretKeyLastUsedDate()).isEqualTo(now);
        assertThat(found.getVersion()).isEqualTo(saved.getVersion());
    }

    private static App newApp(String name, String hash) {
        App app = new App();

        app.setUuid(UUID.randomUUID()
            .toString());
        app.setName(name);
        app.setWorkspaceId(1L);
        app.setEnvironment(Environment.PRODUCTION);
        app.setOwnerUserId(1L);
        app.setStatus(AppStatus.ENABLED);
        app.setVisibility(ResourceVisibility.WORKSPACE);
        app.setHostingTarget(AppHostingTarget.VERCEL);
        app.setSecretKeyHash(hash);
        app.setSecretKeyHint("abcdefgh");

        return app;
    }
}
```

- [ ] **Step 9: Run the IntTest and watch it fail**

```bash
./gradlew :server:ee:libs:automation:automation-app:automation-app-service:testIntegration --tests '*AppRepositoryIntTest*' > /tmp/apps-t1.log 2>&1; echo "exit=$?"
```

Expected: `exit=1` before the domain/changelog exist (compilation or schema error).

- [ ] **Step 10: Run again after Steps 3–7 are complete**

Same command. Expected: `exit=0`, two tests passed. If Liquibase fails on `fk_app_connection_connection`, the connection changelog is missing from the test classpath (Step 8's `testImplementation`).

- [ ] **Step 11: Commit**

```bash
./gradlew spotlessApply > /tmp/apps-spotless.log 2>&1; echo "exit=$?"
git add settings.gradle.kts server/apps/server-app/build.gradle.kts server/libs/config/liquibase-config/src/main/resources/config/liquibase/master.xml server/ee/libs/automation/automation-app
git commit -m "- Add the automation-app module with the app, app_connection and app_connection_operation schema"
```

---

### Task 2: Secret key utility and error types

**Files:**
- Create: `automation-app-api/src/main/java/com/bytechef/ee/automation/app/util/AppSecretKeys.java`
- Create: `automation-app-api/src/main/java/com/bytechef/ee/automation/app/exception/AppErrorType.java`
- Test: `automation-app-api/src/test/java/com/bytechef/ee/automation/app/util/AppSecretKeysTest.java`

**Interfaces:**
- Produces: `AppSecretKeys.generate()` → `GeneratedKey(String key, String hash, String hint)`; `AppSecretKeys.hash(String key)` → 64-char lowercase hex; `AppSecretKeys.hint(String key)`; `AppErrorType` constants.

- [ ] **Step 1: Failing test**

```java
package com.bytechef.ee.automation.app.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class AppSecretKeysTest {

    @Test
    void generatedKeyCarriesTheTenantAndHashesDeterministically() {
        AppSecretKeys.GeneratedKey generated = TenantContext.callWithTenantId(
            "tenant42", () -> AppSecretKeys.generate());

        assertThat(TenantKey.parse(generated.key())
            .getTenantId()).isEqualTo("tenant42");
        assertThat(generated.hash()).hasSize(64)
            .matches("[0-9a-f]+")
            .isEqualTo(AppSecretKeys.hash(generated.key()));
    }

    @Test
    void hintIsTheLastEightCharactersWithoutPadding() {
        assertThat(AppSecretKeys.hint("abcdefghijklmnop==")).isEqualTo("ijklmnop");
        assertThat(AppSecretKeys.hint("short")).isEqualTo("short");
    }

    @Test
    void differentKeysHashDifferently() {
        assertThat(AppSecretKeys.hash("k1")).isNotEqualTo(AppSecretKeys.hash("k2"));
    }
}
```

- [ ] **Step 2: Run, expect compilation failure**

```bash
./gradlew :server:ee:libs:automation:automation-app:automation-app-api:test > /tmp/apps-t2.log 2>&1; echo "exit=$?"
```

- [ ] **Step 3: Implement**

`util/AppSecretKeys.java`:

```java
package com.bytechef.ee.automation.app.util;

import com.bytechef.tenant.domain.TenantKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Generates and hashes app secret keys. The key is a {@link TenantKey} so the tenant can be read from it before any
 * lookup; only its SHA-256 is stored. Unsalted on purpose: the key carries 24 bytes of CSPRNG entropy and the
 * equality lookup needs a deterministic hash.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AppSecretKeys {

    private static final int HINT_LENGTH = 8;

    private AppSecretKeys() {
    }

    public static GeneratedKey generate() {
        String key = String.valueOf(TenantKey.of());

        return new GeneratedKey(key, hash(key), hint(key));
    }

    public static String hash(String key) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of()
                .formatHex(messageDigest.digest(key.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException("SHA-256 is not available", noSuchAlgorithmException);
        }
    }

    public static String hint(String key) {
        String stripped = key.replaceAll("=+$", "");

        if (stripped.length() <= HINT_LENGTH) {
            return stripped;
        }

        return stripped.substring(stripped.length() - HINT_LENGTH);
    }

    public record GeneratedKey(String key, String hash, String hint) {
    }
}
```

`exception/AppErrorType.java`:

```java
package com.bytechef.ee.automation.app.exception;

import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.exception.AbstractErrorType;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AppErrorType extends AbstractErrorType {

    public static final AppErrorType APP_NAME_ALREADY_EXISTS = new AppErrorType(100);
    public static final AppErrorType APP_NOT_FOUND = new AppErrorType(101);
    public static final AppErrorType APP_DISABLED = new AppErrorType(102);
    public static final AppErrorType APP_CONNECTION_NOT_FOUND = new AppErrorType(103);
    public static final AppErrorType APP_OPERATION_NOT_ALLOWED = new AppErrorType(104);
    public static final AppErrorType APP_CONNECTION_UNAVAILABLE = new AppErrorType(105);
    public static final AppErrorType APP_PROXY_PATH_REJECTED = new AppErrorType(106);
    public static final AppErrorType APP_PROXY_UNSUPPORTED = new AppErrorType(107);
    public static final AppErrorType APP_EXECUTION_TIMEOUT = new AppErrorType(108);
    public static final AppErrorType APP_BINDING_KEY_INVALID = new AppErrorType(109);
    public static final AppErrorType APP_BINDING_KEY_ALREADY_EXISTS = new AppErrorType(110);
    public static final AppErrorType APP_CONNECTION_NOT_BINDABLE = new AppErrorType(111);
    public static final AppErrorType APP_PROXY_REQUEST_TOO_LARGE = new AppErrorType(112);
    public static final AppErrorType APP_PROXY_RESPONSE_TOO_LARGE = new AppErrorType(113);

    private AppErrorType(int errorKey) {
        super(App.class, errorKey);
    }
}
```

- [ ] **Step 4: Run, expect pass**

Same command, `exit=0`.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply > /tmp/apps-spotless.log 2>&1; echo "exit=$?"
git add server/ee/libs/automation/automation-app/automation-app-api
git commit -m "- Add app secret key generation and hashing and the App error types"
```

---

### Task 3: Permission scopes, visibility policy, ownership and environment resolvers

**Files:**
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/security/scope/AppPermissionScope.java`
- Create: `…/security/scope/AppPermissionScopeProvider.java`
- Create: `automation-app-service/src/main/java/com/bytechef/ee/automation/app/security/{AppVisibilityPolicy,AppVisibilityProvider,AppOwnershipResolver,AppEnvironmentResolver}.java`
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/security/EnvironmentAwareGateCoverageTest.java` (`RESOLVED_RESOURCE_TYPES`)
- Modify: `…/security/ResourceEnvironmentResolverCoverageTest.java` (the `containsAll(List.of("Connection", …))` assertion)
- Test: `automation-app-service/src/test/java/com/bytechef/ee/automation/app/security/AppResolversTest.java`

**Interfaces:**
- Consumes: `AppService.fetchApp(long) : Optional<App>` (Task 4 — write the resolvers against the interface now; Task 4 supplies the implementation). To keep this task compilable, create the `AppService` **interface** here with only `Optional<App> fetchApp(long id);`; Task 4 extends it.
- Produces: resource type string `"App"`; scopes `APP_VIEW`, `APP_MANAGE`.

- [ ] **Step 1: Scopes**

`AppPermissionScope.java` (in `…configuration.security.scope`, EE header):

```java
public enum AppPermissionScope implements PermissionScopeType {

    APP_VIEW,
    APP_MANAGE
}
```

`AppPermissionScopeProvider.java`:

```java
@Component
@ConditionalOnEEVersion
public class AppPermissionScopeProvider implements PermissionScopeProvider {

    @Override
    public Set<ScopeDefinition> scopeDefinitions() {
        return Set.of(
            new ScopeDefinition(AppPermissionScope.APP_VIEW, WorkspaceRole.VIEWER),
            new ScopeDefinition(AppPermissionScope.APP_MANAGE, WorkspaceRole.ADMIN));
    }
}
```

Imports as in `VariablePermissionScopeProvider` (same package).

- [ ] **Step 2: Failing resolver test**

```java
package com.bytechef.ee.automation.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class AppResolversTest {

    private final AppService appService = mock(AppService.class);

    @Test
    void ownershipResolvesWorkspaceAndOwnerAndFailsClosed() {
        App app = new App();

        app.setWorkspaceId(7L);
        app.setOwnerUserId(3L);

        when(appService.fetchApp(1L)).thenReturn(Optional.of(app));
        when(appService.fetchApp(2L)).thenReturn(Optional.empty());

        AppOwnershipResolver resolver = new AppOwnershipResolver(appService);

        ResourceOwner owner = resolver.resolveOwner(1L);

        assertThat(owner.workspaceId()).hasValue(7L);
        assertThat(owner.ownerUserId()).hasValue(3L);
        assertThat(resolver.resolveOwner(2L)).isEqualTo(ResourceOwner.unknown());
        assertThat(resolver.resourceType()).isEqualTo("App");
    }

    @Test
    void environmentComesFromTheRow() {
        App app = new App();

        app.setEnvironment(Environment.STAGING);

        when(appService.fetchApp(1L)).thenReturn(Optional.of(app));

        assertThat(new AppEnvironmentResolver(appService).fetchEnvironment(1L)).hasValue(Environment.STAGING);
        assertThat(new AppEnvironmentResolver(appService).fetchEnvironment("x")).isEmpty();
    }

    @Test
    void visibilityProviderReturnsRecordOrEmpty() {
        App app = new App();

        app.setId(1L);
        app.setVisibility(ResourceVisibility.WORKSPACE);
        app.setCreatedBy("admin");

        when(appService.fetchApp(1L)).thenReturn(Optional.of(app));

        assertThat(new AppVisibilityProvider(appService).fetchVisibility(1L))
            .hasValueSatisfying(record -> {
                assertThat(record.visibility()).isEqualTo(ResourceVisibility.WORKSPACE);
                assertThat(record.createdBy()).isEqualTo("admin");
            });
        assertThat(new AppVisibilityProvider(appService).fetchVisibility(9L)).isEmpty();
    }

    @Test
    void policyDefaultsToWorkspace() {
        AppVisibilityPolicy policy = new AppVisibilityPolicy();

        assertThat(policy.defaultVisibility()).isEqualTo(ResourceVisibility.WORKSPACE);
        assertThat(policy.supportedVisibilities()).contains(ResourceVisibility.PRIVATE, ResourceVisibility.WORKSPACE);
    }
}
```

- [ ] **Step 3: Run, expect failure** (`…automation-app-service:test --tests '*AppResolversTest*'`, redirect, `exit=1`).

- [ ] **Step 4: Implement**

`AppService` interface (api module, `service/AppService.java`), for now:

```java
public interface AppService {

    Optional<App> fetchApp(long id);
}
```

`AppVisibilityPolicy`:

```java
@Component
public class AppVisibilityPolicy implements ResourceVisibilityPolicy {

    @Override
    public String resourceType() {
        return "App";
    }

    @Override
    public ResourceVisibility defaultVisibility() {
        return ResourceVisibility.WORKSPACE;
    }

    @Override
    public Set<ResourceVisibility> supportedVisibilities() {
        return Set.of(ResourceVisibility.PRIVATE, ResourceVisibility.WORKSPACE, ResourceVisibility.ORGANIZATION);
    }
}
```

`AppVisibilityProvider`:

```java
@Component
public class AppVisibilityProvider implements ResourceVisibilityProvider {

    private final AppService appService;

    @SuppressFBWarnings("EI")
    public AppVisibilityProvider(AppService appService) {
        this.appService = appService;
    }

    @Override
    public String resourceType() {
        return "App";
    }

    @Override
    public Optional<VisibilityRecord> fetchVisibility(long id) {
        return appService.fetchApp(id)
            .map(app -> new VisibilityRecord(app.getId(), app.getVisibility(), app.getCreatedBy()));
    }
}
```

`AppOwnershipResolver`:

```java
@Component
public class AppOwnershipResolver implements ResourceOwnershipResolver {

    private final AppService appService;

    @SuppressFBWarnings("EI")
    public AppOwnershipResolver(AppService appService) {
        this.appService = appService;
    }

    @Override
    public String resourceType() {
        return "App";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return appService.fetchApp(id)
            .map(app -> ResourceOwner.of(
                app.getWorkspaceId() == null ? OptionalLong.empty() : OptionalLong.of(app.getWorkspaceId()),
                OptionalLong.of(app.getOwnerUserId())))
            .orElseGet(ResourceOwner::unknown);
    }
}
```

`AppEnvironmentResolver`:

```java
@Component
public class AppEnvironmentResolver implements ResourceEnvironmentResolver {

    private final AppService appService;

    @SuppressFBWarnings("EI")
    public AppEnvironmentResolver(AppService appService) {
        this.appService = appService;
    }

    @Override
    public String resourceType() {
        return "App";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        return appService.fetchApp(number.longValue())
            .map(App::getEnvironment);
    }
}
```

All four carry the EE header and `@version ee` Javadoc. Imports: `com.bytechef.automation.configuration.security.*`, `com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord`, `com.bytechef.platform.security.domain.*`, `com.bytechef.platform.configuration.domain.Environment`, `edu.umd.cs.findbugs.annotations.SuppressFBWarnings`, `org.springframework.stereotype.Component`.

- [ ] **Step 5: Update the two coverage tests**

In `EnvironmentAwareGateCoverageTest`: `List.of("Connection", "ProjectDeployment", "McpServer", "ApiCollection", "App")`.
In `ResourceEnvironmentResolverCoverageTest` (the `containsAll` assertion near line 226): add `"App"` to the list. Both tests scan the classpath: add `testImplementation(project(":server:ee:libs:automation:automation-app:automation-app-service"))` to `automation-configuration-service/build.gradle.kts` only if the scan does not already see the new module (run first; add only on failure).

- [ ] **Step 6: Run**

```bash
./gradlew :server:ee:libs:automation:automation-app:automation-app-service:test :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests '*AppResolversTest*' --tests '*CoverageTest*' --tests '*PermissionScopeProviderEditionGatingTest*' --continue > /tmp/apps-t3.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED" /tmp/apps-t3.log
```

Expected: `exit=0`, no FAILED lines.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply > /tmp/apps-spotless.log 2>&1; echo "exit=$?"
git add server/ee/libs/automation/automation-app server/ee/libs/automation/automation-configuration/automation-configuration-service
git commit -m "- Add App permission scopes, visibility policy and ownership and environment resolvers"
```

---

### Task 4: `AppService` and `AppConnectionService`

**Files:**
- Modify: `automation-app-api/…/service/AppService.java` (full interface)
- Create: `automation-app-api/…/service/AppConnectionService.java`
- Create: `automation-app-service/…/service/AppServiceImpl.java`, `AppConnectionServiceImpl.java`
- Test: `automation-app-service/src/test/java/com/bytechef/ee/automation/app/service/AppServiceIntTest.java`, `AppConnectionServiceTest.java`

**Interfaces:**
- Produces:

```java
public interface AppService {
    record CreatedApp(App app, String secretKey) {}
    record RotatedKey(App app, String secretKey) {}

    CreatedApp create(App app);                       // generates uuid + key; name unique per (workspace, environment)
    void delete(long id);
    Optional<App> fetchApp(long id);
    Optional<App> fetchAppBySecretKey(String secretKey); // hashes, looks up; any status
    App getApp(long id);                              // NoSuchElementException-free: ConfigurationException APP_NOT_FOUND
    List<App> getApps(long workspaceId, Environment environment);
    RotatedKey rotateSecretKey(long id);
    void touchSecretKeyLastUsedDate(long id);         // best-effort, 60 s throttle
    App update(App app);                              // name, description, status, hostingTarget, url, lastDeployedDate, visibility
    App updateStatus(long id, AppStatus status);
}

public interface AppConnectionService {
    AppConnection bind(long appId, String bindingKey, long connectionId, List<String> actionNames,
        List<String> toolNames, boolean proxyEnabled, List<String> proxyAllowedMethods); // create or replace
    Optional<AppConnection> fetchBinding(long appId, String bindingKey);
    List<AppConnection> getBindings(long appId);
    List<AppConnection> getBindingsByConnectionId(long connectionId);
    void unbind(long appId, String bindingKey);
}
```

- [ ] **Step 1: Failing unit test for the binding rules**

`AppConnectionServiceTest`:

```java
@ExtendWith(MockitoExtension.class)
class AppConnectionServiceTest {

    @Mock
    private AppConnectionRepository appConnectionRepository;
    @Mock
    private AppService appService;
    @Mock
    private ConnectionService connectionService;
    @Mock
    private WorkspaceConnectionService workspaceConnectionService;

    private AppConnectionServiceImpl appConnectionService;

    @BeforeEach
    void beforeEach() {
        appConnectionService = new AppConnectionServiceImpl(
            appConnectionRepository, appService, connectionService, workspaceConnectionService);
    }

    @Test
    void rejectsBindingKeyThatIsNotLowerKebab() {
        assertThatThrownBy(() -> appConnectionService.bind(1L, "Sales Slack", 5L, List.of(), List.of(), false, List.of()))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(AppErrorType.APP_BINDING_KEY_INVALID.getErrorKey());
    }

    @Test
    void rejectsConnectionOutsideTheAppsWorkspaceOrEnvironmentOrOfAnotherType() {
        App app = app(7L, Environment.PRODUCTION);
        when(appService.getApp(1L)).thenReturn(app);
        when(workspaceConnectionService.getWorkspaceConnections(7L)).thenReturn(List.of(new WorkspaceConnection(5L, 7L)));

        Connection wrongEnvironment = connection(5L, Environment.STAGING, PlatformType.AUTOMATION);
        when(connectionService.getConnection(5L)).thenReturn(wrongEnvironment);
        assertNotBindable(() -> appConnectionService.bind(1L, "crm", 5L, List.of(), List.of(), false, List.of()));

        Connection embedded = connection(5L, Environment.PRODUCTION, PlatformType.EMBEDDED);
        when(connectionService.getConnection(5L)).thenReturn(embedded);
        assertNotBindable(() -> appConnectionService.bind(1L, "crm", 5L, List.of(), List.of(), false, List.of()));

        Connection otherWorkspace = connection(6L, Environment.PRODUCTION, PlatformType.AUTOMATION);
        when(connectionService.getConnection(6L)).thenReturn(otherWorkspace);
        assertNotBindable(() -> appConnectionService.bind(1L, "crm", 6L, List.of(), List.of(), false, List.of()));
    }

    @Test
    void bindReplacesAnExistingBindingInPlace() {
        App app = app(7L, Environment.PRODUCTION);
        when(appService.getApp(1L)).thenReturn(app);
        when(workspaceConnectionService.getWorkspaceConnections(7L)).thenReturn(List.of(new WorkspaceConnection(5L, 7L)));
        when(connectionService.getConnection(5L)).thenReturn(connection(5L, Environment.PRODUCTION, PlatformType.AUTOMATION));

        AppConnection existing = new AppConnection();
        existing.setId(44L);
        existing.setAppId(1L);
        existing.setBindingKey("crm");
        existing.setConnectionId(5L);

        when(appConnectionRepository.findByAppIdAndBindingKey(1L, "crm")).thenReturn(Optional.of(existing));
        when(appConnectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppConnection saved = appConnectionService.bind(
            1L, "crm", 5L, List.of("createRecord"), List.of("searchRecords"), true, List.of("GET", "POST"));

        assertThat(saved.getId()).isEqualTo(44L);
        assertThat(saved.allows(AppOperationType.ACTION, "createRecord")).isTrue();
        assertThat(saved.allows(AppOperationType.ACTION, "deleteRecord")).isFalse();
        assertThat(saved.getProxyAllowedMethodList()).containsExactly("GET", "POST");
    }

    @Test
    void rejectsUnknownProxyMethod() {
        App app = app(7L, Environment.PRODUCTION);
        when(appService.getApp(1L)).thenReturn(app);
        when(workspaceConnectionService.getWorkspaceConnections(7L)).thenReturn(List.of(new WorkspaceConnection(5L, 7L)));
        when(connectionService.getConnection(5L)).thenReturn(connection(5L, Environment.PRODUCTION, PlatformType.AUTOMATION));

        assertThatThrownBy(() -> appConnectionService.bind(1L, "crm", 5L, List.of(), List.of(), true, List.of("TRACE")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static void assertNotBindable(ThrowingCallable callable) {
        assertThatThrownBy(callable)
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(AppErrorType.APP_CONNECTION_NOT_BINDABLE.getErrorKey());
    }

    private static App app(long workspaceId, Environment environment) {
        App app = new App();
        app.setId(1L);
        app.setWorkspaceId(workspaceId);
        app.setEnvironment(environment);
        return app;
    }

    private static Connection connection(long id, Environment environment, PlatformType type) {
        Connection connection = new Connection();
        connection.setId(id);
        connection.setEnvironmentId(environment.ordinal());
        connection.setType(type);
        connection.setComponentName("salesforce");
        connection.setConnectionVersion(1);
        return connection;
    }
}
```

(If `Connection` has no public setters for these fields, build it the way `ConnectionFacadeImplTest` or `ConnectionServiceIntTest` does — look there first; otherwise `mock(Connection.class)` with `when(...)` for `getId`, `getEnvironmentId`, `getType`.)

- [ ] **Step 2: Run, expect failure.**

- [ ] **Step 3: Implement `AppServiceImpl`**

```java
@Service
@Transactional
@ConditionalOnEEVersion
public class AppServiceImpl implements AppService {

    private static final Duration LAST_USED_THROTTLE = Duration.ofMinutes(1);

    private final AppRepository appRepository;

    @SuppressFBWarnings("EI")
    public AppServiceImpl(AppRepository appRepository) {
        this.appRepository = appRepository;
    }

    @Override
    public CreatedApp create(App app) {
        Validate.notNull(app.getWorkspaceId(), "workspaceId");
        Validate.notBlank(app.getName(), "name");

        appRepository.findByWorkspaceIdAndEnvironmentAndName(app.getWorkspaceId(), app.getEnvironmentId(), app.getName())
            .ifPresent(existing -> {
                throw new ConfigurationException(
                    "App '%s' already exists".formatted(app.getName()), AppErrorType.APP_NAME_ALREADY_EXISTS);
            });

        AppSecretKeys.GeneratedKey generatedKey = AppSecretKeys.generate();

        app.setUuid(UUID.randomUUID()
            .toString());
        app.setSecretKeyHash(generatedKey.hash());
        app.setSecretKeyHint(generatedKey.hint());
        app.setStatus(AppStatus.ENABLED);

        if (app.getVisibility() == null) {
            app.setVisibility(ResourceVisibility.WORKSPACE);
        }

        try {
            return new CreatedApp(appRepository.save(app), generatedKey.key());
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            throw new ConfigurationException(
                "App '%s' already exists".formatted(app.getName()), dataIntegrityViolationException,
                AppErrorType.APP_NAME_ALREADY_EXISTS);
        }
    }

    @Override
    public void delete(long id) {
        appRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<App> fetchApp(long id) {
        return appRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<App> fetchAppBySecretKey(String secretKey) {
        return appRepository.findBySecretKeyHash(AppSecretKeys.hash(secretKey));
    }

    @Override
    @Transactional(readOnly = true)
    public App getApp(long id) {
        return appRepository.findById(id)
            .orElseThrow(() -> new ConfigurationException("App %s not found".formatted(id), AppErrorType.APP_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<App> getApps(long workspaceId, Environment environment) {
        return appRepository.findAllByWorkspaceIdAndEnvironmentOrderByName(workspaceId, environment.ordinal());
    }

    @Override
    public RotatedKey rotateSecretKey(long id) {
        App app = getApp(id);

        AppSecretKeys.GeneratedKey generatedKey = AppSecretKeys.generate();

        app.setSecretKeyHash(generatedKey.hash());
        app.setSecretKeyHint(generatedKey.hint());
        app.setSecretKeyLastUsedDate(null);

        return new RotatedKey(appRepository.save(app), generatedKey.key());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void touchSecretKeyLastUsedDate(long id) {
        Instant now = Instant.now();

        appRepository.touchSecretKeyLastUsedDate(id, now, now.minus(LAST_USED_THROTTLE));
    }

    @Override
    public App update(App app) {
        App current = getApp(Validate.notNull(app.getId(), "id"));

        current.setName(app.getName());
        current.setDescription(app.getDescription());
        current.setHostingTarget(app.getHostingTarget());
        current.setUrl(app.getUrl());
        current.setLastDeployedDate(app.getLastDeployedDate());
        current.setVisibility(app.getVisibility());

        return appRepository.save(current);
    }

    @Override
    public App updateStatus(long id, AppStatus status) {
        App app = getApp(id);

        app.setStatus(status);

        return appRepository.save(app);
    }
}
```

Note `visibility` is an `int` field, so `getVisibility()` never returns null; in `create`, set `WORKSPACE` unconditionally unless the caller set `PRIVATE` explicitly — model this by having `AppFacadeImpl` set visibility before calling `create`, and drop the null check.

- [ ] **Step 4: Implement `AppConnectionServiceImpl`**

```java
@Service
@Transactional
@ConditionalOnEEVersion
public class AppConnectionServiceImpl implements AppConnectionService {

    private static final Pattern BINDING_KEY_PATTERN = Pattern.compile("^[a-z][a-z0-9_-]{0,63}$");
    private static final Set<String> PROXY_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD");

    private final AppConnectionRepository appConnectionRepository;
    private final AppService appService;
    private final ConnectionService connectionService;
    private final WorkspaceConnectionService workspaceConnectionService;

    // constructor with @SuppressFBWarnings("EI")

    @Override
    public AppConnection bind(
        long appId, String bindingKey, long connectionId, List<String> actionNames, List<String> toolNames,
        boolean proxyEnabled, List<String> proxyAllowedMethods) {

        if (bindingKey == null || !BINDING_KEY_PATTERN.matcher(bindingKey)
            .matches()) {

            throw new ConfigurationException(
                "Binding key must match " + BINDING_KEY_PATTERN.pattern(), AppErrorType.APP_BINDING_KEY_INVALID);
        }

        App app = appService.getApp(appId);
        Connection connection = connectionService.getConnection(connectionId);

        checkBindable(app, connection);

        List<String> methods = proxyAllowedMethods.stream()
            .map(String::toUpperCase)
            .toList();

        for (String method : methods) {
            Validate.isTrue(PROXY_METHODS.contains(method), "Unsupported proxy method: %s", method);
        }

        AppConnection appConnection = appConnectionRepository.findByAppIdAndBindingKey(appId, bindingKey)
            .orElseGet(AppConnection::new);

        appConnection.setAppId(appId);
        appConnection.setBindingKey(bindingKey);
        appConnection.setConnectionId(connectionId);
        appConnection.setOperations(actionNames, toolNames);
        appConnection.setProxyEnabled(proxyEnabled);
        appConnection.setProxyAllowedMethodList(proxyEnabled ? methods : List.of());

        return appConnectionRepository.save(appConnection);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AppConnection> fetchBinding(long appId, String bindingKey) {
        return appConnectionRepository.findByAppIdAndBindingKey(appId, bindingKey);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppConnection> getBindings(long appId) {
        return appConnectionRepository.findAllByAppIdOrderByBindingKey(appId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppConnection> getBindingsByConnectionId(long connectionId) {
        return appConnectionRepository.findAllByConnectionId(connectionId);
    }

    @Override
    public void unbind(long appId, String bindingKey) {
        appConnectionRepository.findByAppIdAndBindingKey(appId, bindingKey)
            .ifPresent(appConnectionRepository::delete);
    }

    /**
     * A connection is bindable when it is an automation connection in the app's environment and a member of the
     * app's workspace. Virtual AI-provider connections have no workspace_connection row, so the membership check
     * excludes them as well.
     */
    private void checkBindable(App app, Connection connection) {
        boolean inWorkspace = workspaceConnectionService.getWorkspaceConnections(
            Validate.notNull(app.getWorkspaceId(), "workspaceId"))
            .stream()
            .anyMatch(workspaceConnection -> Objects.equals(workspaceConnection.getConnectionId(), connection.getId()));

        if (!inWorkspace || connection.getType() != PlatformType.AUTOMATION
            || connection.getEnvironmentId() != app.getEnvironmentId()) {

            throw new ConfigurationException(
                "Connection %s cannot be bound to app %s: it must be an automation connection in the app's workspace and environment"
                    .formatted(connection.getId(), app.getId()),
                AppErrorType.APP_CONNECTION_NOT_BINDABLE);
        }
    }
}
```

- [ ] **Step 5: `AppServiceIntTest`** (same `AppIntTestConfiguration` with the component scan widened to `com.bytechef.ee.automation.app` and `excludeFilters` for classes that need beans not present — at this point only `AppConnectionServiceImpl` needs `ConnectionService`/`WorkspaceConnectionService`; register `@MockitoBean(types = {ConnectionService.class, WorkspaceConnectionService.class})` on the test):

```java
@ActiveProfiles("testint")
@SpringBootTest(classes = AppIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {ConnectionService.class, WorkspaceConnectionService.class})
class AppServiceIntTest {

    @Autowired
    private AppRepository appRepository;
    @Autowired
    private AppService appService;

    @AfterEach
    void afterEach() {
        appRepository.deleteAll();
    }

    @Test
    void createReturnsTheKeyOnceAndStoresOnlyItsHash() {
        AppService.CreatedApp created = TenantContext.callWithTenantId("public", () -> appService.create(newApp("crm")));

        App stored = appRepository.findById(created.app().getId()).orElseThrow();

        assertThat(created.secretKey()).isNotBlank();
        assertThat(stored.getSecretKeyHash()).isEqualTo(AppSecretKeys.hash(created.secretKey()));
        assertThat(stored.getSecretKeyHash()).isNotEqualTo(created.secretKey());
        assertThat(appService.fetchAppBySecretKey(created.secretKey())).isPresent();
    }

    @Test
    void duplicateNameInSameWorkspaceAndEnvironmentIsRejected() {
        TenantContext.callWithTenantId("public", () -> appService.create(newApp("crm")));

        assertThatThrownBy(() -> TenantContext.callWithTenantId("public", () -> appService.create(newApp("crm"))))
            .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void rotateInvalidatesOldHashOnly() {
        AppService.CreatedApp created = TenantContext.callWithTenantId("public", () -> appService.create(newApp("crm")));

        AppService.RotatedKey rotated = TenantContext.callWithTenantId(
            "public", () -> appService.rotateSecretKey(created.app().getId()));

        assertThat(appService.fetchAppBySecretKey(created.secretKey())).isEmpty();
        assertThat(appService.fetchAppBySecretKey(rotated.secretKey())).isPresent();
        assertThat(rotated.secretKey()).isNotEqualTo(created.secretKey());
    }

    @Test
    void touchLastUsedWritesOncePerMinute() {
        AppService.CreatedApp created = TenantContext.callWithTenantId("public", () -> appService.create(newApp("crm")));
        long id = created.app().getId();

        appService.touchSecretKeyLastUsedDate(id);
        Instant first = appRepository.findById(id).orElseThrow().getSecretKeyLastUsedDate();
        appService.touchSecretKeyLastUsedDate(id);
        Instant second = appRepository.findById(id).orElseThrow().getSecretKeyLastUsedDate();

        assertThat(first).isNotNull();
        assertThat(second).isEqualTo(first);
    }

    private static App newApp(String name) {
        App app = new App();
        app.setName(name);
        app.setWorkspaceId(1L);
        app.setEnvironment(Environment.PRODUCTION);
        app.setOwnerUserId(1L);
        app.setHostingTarget(AppHostingTarget.VERCEL);
        app.setVisibility(ResourceVisibility.WORKSPACE);
        return app;
    }
}
```

- [ ] **Step 6: Run both**

```bash
./gradlew :server:ee:libs:automation:automation-app:automation-app-service:test :server:ee:libs:automation:automation-app:automation-app-service:testIntegration --continue > /tmp/apps-t4.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED" /tmp/apps-t4.log
```

Expected: `exit=0`.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply > /tmp/apps-spotless.log 2>&1; echo "exit=$?"
git add server/ee/libs/automation/automation-app
git commit -m "- Add AppService and AppConnectionService with key rotation and binding validation"
```

---

### Task 5: Audit publisher

**Files:**
- Create: `automation-app-service/…/audit/AppAuditEvent.java`, `AppAuditPublisher.java`
- Test: `automation-app-service/src/test/java/…/audit/AppAuditPublisherTest.java`

- [ ] **Step 1: Failing test**

```java
@ExtendWith(MockitoExtension.class)
class AppAuditPublisherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Test
    void publishesAuditApplicationEventWithAppIdAndSystemPrincipalWhenNoUser() {
        AppAuditPublisher publisher = new AppAuditPublisher(applicationEventPublisher);

        publisher.publish(AppAuditEvent.APP_CREATED, 5L, Map.of("name", "crm"));

        ArgumentCaptor<AuditApplicationEvent> captor = ArgumentCaptor.forClass(AuditApplicationEvent.class);

        verify(applicationEventPublisher).publishEvent(captor.capture());

        AuditEvent auditEvent = captor.getValue().getAuditEvent();

        assertThat(auditEvent.getType()).isEqualTo("APP_CREATED");
        assertThat(auditEvent.getPrincipal()).isEqualTo("SYSTEM");
        assertThat(auditEvent.getData()).containsEntry("appId", "5").containsEntry("name", "crm");
    }
}
```

- [ ] **Step 2: Run, expect failure.**

- [ ] **Step 3: Implement** — copy `ApiCollectionAuditPublisher` verbatim with `ApiCollection`→`App`, data key `appId`, and the enum:

```java
public enum AppAuditEvent {

    APP_CREATED, APP_DELETED, APP_ENABLED, APP_DISABLED, APP_SECRET_KEY_ROTATED, APP_CONNECTION_BOUND,
    APP_CONNECTION_UNBOUND, APP_OPERATIONS_CHANGED
}
```

- [ ] **Step 4: Run, expect pass. Step 5: Commit** `- Add the App audit publisher`.

---

### Task 6: `AppFacade` (gated admin surface) and DTOs

**Files:**
- Create: `automation-app-api/…/dto/AppDTO.java`, `AppBindingDTO.java`, `AppSecretKeyDTO.java`
- Create: `automation-app-api/…/facade/AppFacade.java`
- Create: `automation-app-service/…/facade/AppFacadeImpl.java`
- Test: `automation-app-service/src/test/java/…/facade/AppFacadeTest.java`

**Interfaces:**
- Produces:

```java
public record AppDTO(long id, String uuid, String name, @Nullable String description, long workspaceId,
    Environment environment, long ownerUserId, AppStatus status, ResourceVisibility visibility,
    AppHostingTarget hostingTarget, @Nullable String url, @Nullable Instant lastDeployedDate, String secretKeyHint,
    @Nullable Instant secretKeyLastUsedDate, String createdBy, Instant createdDate, String lastModifiedBy,
    Instant lastModifiedDate, int version) {
    public static AppDTO of(App app) { … }
}

public record AppBindingDTO(long id, long appId, String bindingKey, long connectionId, String connectionName,
    String componentName, int connectionVersion, List<String> actionNames, List<String> toolNames,
    boolean proxyEnabled, List<String> proxyAllowedMethods) {}

public record AppSecretKeyDTO(AppDTO app, String secretKey) {}

public interface AppFacade {
    AppSecretKeyDTO createApp(long workspaceId, Environment environment, String name, @Nullable String description,
        AppHostingTarget hostingTarget, ResourceVisibility visibility);
    void deleteApp(long id);
    AppDTO getApp(long id);
    List<AppDTO> getApps(long workspaceId, long environmentId);
    AppDTO updateApp(long id, String name, @Nullable String description, AppHostingTarget hostingTarget,
        @Nullable String url, @Nullable Instant lastDeployedDate, ResourceVisibility visibility);
    AppDTO updateAppStatus(long id, AppStatus status);
    AppSecretKeyDTO rotateSecretKey(long id);
    List<AppBindingDTO> getBindings(long appId);
    AppBindingDTO bindConnection(long appId, String bindingKey, long connectionId, List<String> actionNames,
        List<String> toolNames, boolean proxyEnabled, List<String> proxyAllowedMethods);
    void unbindConnection(long appId, String bindingKey);
}
```

Gates (verbatim strings; a test pins them):

| Method | `@PreAuthorize` |
|---|---|
| `createApp` | `hasWorkspaceScopeInEnvironment(#workspaceId, 'APP_MANAGE', #environment)` |
| `getApps` | `hasWorkspaceScopeInEnvironmentId(#workspaceId, 'APP_VIEW', #environmentId)` |
| `getApp`, `getBindings` | `hasPermission(#id, 'App', 'APP_VIEW')` / `hasPermission(#appId, 'App', 'APP_VIEW')` |
| `updateApp`, `updateAppStatus`, `rotateSecretKey`, `deleteApp`, `unbindConnection` | `hasPermission(#id, 'App', 'APP_MANAGE')` (or `#appId`) |
| `bindConnection` | `hasPermission(#appId, 'App', 'APP_MANAGE') and hasPermission(#connectionId, 'Connection', 'CONNECTION_USE')` |

- [ ] **Step 1: Failing test pinning the gates and the owner**

```java
class AppFacadeTest {

    @Test
    void preAuthorizeExpressionsArePinned() throws Exception {
        assertThat(preAuthorize("createApp", long.class, Environment.class, String.class, String.class,
            AppHostingTarget.class, ResourceVisibility.class))
            .isEqualTo("hasWorkspaceScopeInEnvironment(#workspaceId, 'APP_MANAGE', #environment)");
        assertThat(preAuthorize("getApps", long.class, long.class))
            .isEqualTo("hasWorkspaceScopeInEnvironmentId(#workspaceId, 'APP_VIEW', #environmentId)");
        assertThat(preAuthorize("rotateSecretKey", long.class)).isEqualTo("hasPermission(#id, 'App', 'APP_MANAGE')");
        assertThat(preAuthorize("bindConnection", long.class, String.class, long.class, List.class, List.class,
            boolean.class, List.class))
            .isEqualTo("hasPermission(#appId, 'App', 'APP_MANAGE') and hasPermission(#connectionId, 'Connection', 'CONNECTION_USE')");
    }

    @Test
    void createSetsOwnerToCurrentUserAndPublishesAudit() {
        AppService appService = mock(AppService.class);
        AppConnectionService appConnectionService = mock(AppConnectionService.class);
        ConnectionService connectionService = mock(ConnectionService.class);
        UserService userService = mock(UserService.class);
        AppAuditPublisher appAuditPublisher = mock(AppAuditPublisher.class);
        ResourceGrantService resourceGrantService = mock(ResourceGrantService.class);

        User user = new User();
        user.setId(9L);
        user.setLogin("admin");
        when(userService.getUser("admin")).thenReturn(user);
        when(appService.create(any())).thenAnswer(invocation -> {
            App app = invocation.getArgument(0);
            app.setId(1L);
            app.setUuid("u");
            app.setSecretKeyHint("hint");
            app.setCreatedBy("admin");
            app.setCreatedDate(Instant.EPOCH);
            app.setLastModifiedBy("admin");
            app.setLastModifiedDate(Instant.EPOCH);
            return new AppService.CreatedApp(app, "secret");
        });

        AppFacadeImpl facade = new AppFacadeImpl(
            appAuditPublisher, appConnectionService, appService, connectionService, resourceGrantService, userService);

        AppSecretKeyDTO result = SecurityUtils.runAs("admin", List.of(), () -> facade.createApp(
            7L, Environment.PRODUCTION, "crm", null, AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE));

        assertThat(result.secretKey()).isEqualTo("secret");
        assertThat(result.app().ownerUserId()).isEqualTo(9L);
        verify(appAuditPublisher).publish(eq(AppAuditEvent.APP_CREATED), eq(1L), anyMap());
    }

    private static String preAuthorize(String method, Class<?>... parameterTypes) throws NoSuchMethodException {
        return AppFacadeImpl.class.getMethod(method, parameterTypes).getAnnotation(PreAuthorize.class).value();
    }
}
```

Check `UserService` for the exact getter (`getUser(String login)` vs `fetchUser`) and `SecurityUtils.runAs` signature before writing; both exist per `SecurityUtils` (`runAs(String login, Collection<? extends GrantedAuthority>, Supplier<T>)`).

- [ ] **Step 2: Run, expect failure.**

- [ ] **Step 3: Implement `AppFacadeImpl`**

```java
@Service
@Transactional
@ConditionalOnEEVersion
public class AppFacadeImpl implements AppFacade {

    // fields + constructor for appAuditPublisher, appConnectionService, appService, connectionService,
    // resourceGrantService, userService (alphabetical), @SuppressFBWarnings("EI")

    @Override
    @PreAuthorize("hasWorkspaceScopeInEnvironment(#workspaceId, 'APP_MANAGE', #environment)")
    public AppSecretKeyDTO createApp(
        long workspaceId, Environment environment, String name, @Nullable String description,
        AppHostingTarget hostingTarget, ResourceVisibility visibility) {

        User user = userService.getUser(SecurityUtils.getCurrentUserLogin());

        App app = new App();

        app.setWorkspaceId(workspaceId);
        app.setEnvironment(environment);
        app.setName(name);
        app.setDescription(description);
        app.setHostingTarget(hostingTarget);
        app.setVisibility(visibility);
        app.setOwnerUserId(user.getId());

        AppService.CreatedApp created = appService.create(app);

        Map<String, Object> data = new HashMap<>();

        data.put("name", name);
        data.put("workspaceId", workspaceId);

        appAuditPublisher.publish(AppAuditEvent.APP_CREATED, created.app().getId(), data);

        return new AppSecretKeyDTO(AppDTO.of(created.app()), created.secretKey());
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'App', 'APP_MANAGE')")
    public void deleteApp(long id) {
        appService.delete(id);                       // bindings and operations cascade in the schema
        resourceGrantService.deleteGrants("App", id); // no FK: grants must go explicitly
        appAuditPublisher.publish(AppAuditEvent.APP_DELETED, id);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'App', 'APP_VIEW')")
    @Transactional(readOnly = true)
    public AppDTO getApp(long id) {
        return AppDTO.of(appService.getApp(id));
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEnvironmentId(#workspaceId, 'APP_VIEW', #environmentId)")
    @Transactional(readOnly = true)
    public List<AppDTO> getApps(long workspaceId, long environmentId) {
        return appService.getApps(workspaceId, Environment.values()[(int) environmentId])
            .stream()
            .map(AppDTO::of)
            .toList();
    }

    // updateApp: load, copy fields, appService.update; updateAppStatus: appService.updateStatus + APP_ENABLED/APP_DISABLED;
    // rotateSecretKey: appService.rotateSecretKey + APP_SECRET_KEY_ROTATED; returns AppSecretKeyDTO.

    @Override
    @PreAuthorize("hasPermission(#appId, 'App', 'APP_VIEW')")
    @Transactional(readOnly = true)
    public List<AppBindingDTO> getBindings(long appId) {
        return appConnectionService.getBindings(appId)
            .stream()
            .map(this::toBindingDTO)
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#appId, 'App', 'APP_MANAGE') and hasPermission(#connectionId, 'Connection', 'CONNECTION_USE')")
    public AppBindingDTO bindConnection(
        long appId, String bindingKey, long connectionId, List<String> actionNames, List<String> toolNames,
        boolean proxyEnabled, List<String> proxyAllowedMethods) {

        boolean existed = appConnectionService.fetchBinding(appId, bindingKey)
            .isPresent();

        AppConnection appConnection = appConnectionService.bind(
            appId, bindingKey, connectionId, actionNames, toolNames, proxyEnabled, proxyAllowedMethods);

        appAuditPublisher.publish(
            existed ? AppAuditEvent.APP_OPERATIONS_CHANGED : AppAuditEvent.APP_CONNECTION_BOUND, appId,
            Map.of("bindingKey", bindingKey, "connectionId", connectionId));

        return toBindingDTO(appConnection);
    }

    @Override
    @PreAuthorize("hasPermission(#appId, 'App', 'APP_MANAGE')")
    public void unbindConnection(long appId, String bindingKey) {
        appConnectionService.unbind(appId, bindingKey);
        appAuditPublisher.publish(AppAuditEvent.APP_CONNECTION_UNBOUND, appId, Map.of("bindingKey", bindingKey));
    }

    private AppBindingDTO toBindingDTO(AppConnection appConnection) {
        Connection connection = connectionService.getConnection(appConnection.getConnectionId());

        return new AppBindingDTO(
            appConnection.getId(), appConnection.getAppId(), appConnection.getBindingKey(),
            appConnection.getConnectionId(), connection.getName(), connection.getComponentName(),
            connection.getConnectionVersion(), appConnection.getOperationNames(AppOperationType.ACTION),
            appConnection.getOperationNames(AppOperationType.TOOL), appConnection.isProxyEnabled(),
            appConnection.getProxyAllowedMethodList());
    }
}
```

Add `implementation(project(":server:libs:platform:platform-user:platform-user-api"))` to the service `build.gradle.kts`. Add `createApp`'s `hasWorkspaceScopeInEnvironment` and the by-id `hasPermission(…, 'App', …)` methods to the classification table in `docs/superpowers/specs/2026-08-17-project-visibility-design.md` §17 (one row per method, same columns as the rows already there).

- [ ] **Step 4: Run, expect pass. Step 5: Commit** `- Add the gated AppFacade with app and binding DTOs`.

---

### Task 7: Refuse deleting a bound connection

**Files:**
- Create: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/usage/ConnectionUsageChecker.java` (CE, Apache header)
- Modify: `server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/facade/ConnectionFacadeImpl.java` (constructor + `isConnectionUsed` + `delete`)
- Create: `automation-app-service/…/security/AppConnectionUsageChecker.java`
- Test: `automation-app-service/src/test/java/…/security/AppConnectionUsageCheckerTest.java`; existing `ConnectionFacadeIntTest` and any `new ConnectionFacadeImpl(` call sites (grep) keep compiling.

**Interfaces:**
- Produces (CE SPI):

```java
package com.bytechef.platform.connection.usage;

/**
 * Lets a module outside platform-connection declare that a connection is in use, so the connection cannot be
 * deleted from under it. Each description names the consumer, e.g. {@code "app 'crm-dashboard'"}.
 */
public interface ConnectionUsageChecker {

    List<String> describeUsages(long connectionId);
}
```

- [ ] **Step 1: Failing test**

```java
@ExtendWith(MockitoExtension.class)
class AppConnectionUsageCheckerTest {

    @Mock
    private AppConnectionService appConnectionService;
    @Mock
    private AppService appService;

    @Test
    void disabledAppsStillCountAsUsage() {
        AppConnection binding = new AppConnection();
        binding.setAppId(1L);
        binding.setBindingKey("crm");

        App app = new App();
        app.setId(1L);
        app.setName("crm-dashboard");
        app.setStatus(AppStatus.DISABLED);

        when(appConnectionService.getBindingsByConnectionId(5L)).thenReturn(List.of(binding));
        when(appService.fetchApp(1L)).thenReturn(Optional.of(app));

        assertThat(new AppConnectionUsageChecker(appConnectionService, appService).describeUsages(5L))
            .containsExactly("app 'crm-dashboard' (binding 'crm')");
    }

    @Test
    void unboundConnectionHasNoUsages() {
        when(appConnectionService.getBindingsByConnectionId(5L)).thenReturn(List.of());

        assertThat(new AppConnectionUsageChecker(appConnectionService, appService).describeUsages(5L)).isEmpty();
    }
}
```

- [ ] **Step 2: Run, expect failure.**

- [ ] **Step 3: Implement**

`AppConnectionUsageChecker` (`@Component @ConditionalOnEEVersion`):

```java
@Override
public List<String> describeUsages(long connectionId) {
    return appConnectionService.getBindingsByConnectionId(connectionId)
        .stream()
        .map(binding -> "app '%s' (binding '%s')".formatted(
            appService.fetchApp(binding.getAppId())
                .map(App::getName)
                .orElse("#" + binding.getAppId()),
            binding.getBindingKey()))
        .toList();
}
```

In `ConnectionFacadeImpl`: add a constructor parameter `ObjectProvider<ConnectionUsageChecker> connectionUsageCheckerProvider`, store `this.connectionUsageCheckers = connectionUsageCheckerProvider.orderedStream().toList();`, and change:

```java
    public void delete(Long id) {
        Connection connection = connectionService.getConnection(id);

        List<String> usages = describeUsages(id);

        if (isConnectionUsed(id, connection.getType()) || !usages.isEmpty()) {
            throw new ConfigurationException(
                usages.isEmpty()
                    ? "Connection id=%s is used".formatted(id)
                    : "Connection id=%s is used by %s".formatted(id, String.join(", ", usages)),
                ConnectionErrorType.CONNECTION_IS_USED);
        }

        connectionService.delete(id);
    }

    private List<String> describeUsages(long connectionId) {
        return connectionUsageCheckers.stream()
            .flatMap(checker -> checker.describeUsages(connectionId)
                .stream())
            .toList();
    }
```

and in the private `isConnectionUsed(long, PlatformType)` add `|| !describeUsages(connectionId).isEmpty()` so the three DTO call sites (lines ~310, ~320, ~453) report `used = true` for app-bound connections too. Grep `new ConnectionFacadeImpl(` across `server/` and pass `new StaticObjectProvider(...)`/a mock `ObjectProvider` (or `org.springframework.beans.factory.ObjectProvider` via `mock(ObjectProvider.class)` with `orderedStream()` returning `Stream.empty()`) in every hand-built site.

Because `WorkspaceConnectionFacadeImpl.delete` removes the `workspace_connection` row **before** calling `connectionFacade.delete`, and the whole method is transactional, a refusal rolls that removal back — add one assertion to `AppConnectionServiceTest`'s module? No: cover it in Task 15's facade IntTest (`deletingBoundConnectionIsRefusedAndKeepsWorkspaceLink`), which has the full graph.

- [ ] **Step 4: Run** `automation-app-service:test`, `platform-connection-service:test`, `platform-connection-service:testIntegration` with `--continue`; expect no FAILED tasks.

- [ ] **Step 5: Commit** `- Refuse deleting a connection that an app binds through a ConnectionUsageChecker SPI`.

---

### Task 8: GraphQL admin surface

**Files:**
- Create: `automation-app-graphql/src/main/resources/graphql/app.graphqls`
- Create: `automation-app-graphql/src/main/java/com/bytechef/ee/automation/app/web/graphql/AppGraphQlController.java`
- Modify: `client/codegen.ts` (add the schema path)
- Test: `automation-app-graphql/src/test/java/…/AppGraphQlControllerTest.java`

- [ ] **Step 1: Schema**

```graphql
extend type Query {
    app(id: ID!): App!
    appBindings(appId: ID!): [AppBinding!]!
    apps(workspaceId: ID!, environmentId: ID!): [App!]!
    appBindableConnections(appId: ID!): [AppBindableConnection!]!
    appComponentOperations(componentName: String!, componentVersion: Int!): AppComponentOperations!
}

extend type Mutation {
    createApp(workspaceId: ID!, environmentId: ID!, input: AppInput!): AppSecretKey!
    updateApp(id: ID!, input: AppInput!): App!
    updateAppStatus(id: ID!, status: AppStatus!): App!
    rotateAppSecretKey(id: ID!): AppSecretKey!
    deleteApp(id: ID!): Boolean!
    bindAppConnection(appId: ID!, input: AppBindingInput!): AppBinding!
    unbindAppConnection(appId: ID!, bindingKey: String!): Boolean!
}

enum AppStatus { ENABLED DISABLED }
enum AppHostingTarget { VERCEL SELF_HOSTED }

type App {
    id: ID!
    uuid: String!
    name: String!
    description: String
    workspaceId: ID!
    environmentId: ID!
    ownerUserId: ID!
    status: AppStatus!
    visibility: String!
    hostingTarget: AppHostingTarget!
    url: String
    lastDeployedDate: String
    secretKeyHint: String!
    secretKeyLastUsedDate: String
    createdBy: String
    createdDate: String
    lastModifiedBy: String
    lastModifiedDate: String
}

type AppSecretKey {
    app: App!
    secretKey: String!
}

type AppBinding {
    id: ID!
    appId: ID!
    bindingKey: String!
    connectionId: ID!
    connectionName: String!
    componentName: String!
    connectionVersion: Int!
    actionNames: [String!]!
    toolNames: [String!]!
    proxyEnabled: Boolean!
    proxyAllowedMethods: [String!]!
}

type AppBindableConnection {
    id: ID!
    name: String!
    componentName: String!
    connectionVersion: Int!
}

type AppComponentOperations {
    actionNames: [String!]!
    toolNames: [String!]!
}

input AppInput {
    name: String!
    description: String
    hostingTarget: AppHostingTarget!
    url: String
    visibility: String
}

input AppBindingInput {
    bindingKey: String!
    connectionId: ID!
    actionNames: [String!]!
    toolNames: [String!]!
    proxyEnabled: Boolean!
    proxyAllowedMethods: [String!]!
}
```

- [ ] **Step 2: Failing controller test** (mirrors `WorkspaceVariableGraphQlControllerTest`): mock `AppFacade`, `WorkspaceConnectionFacade`-equivalent and the component services; assert `createApp` maps `environmentId` to `Environment.values()[…]`, `rotateAppSecretKey` returns the facade's key, and dates are `String`s (Instant → `toString()`).

- [ ] **Step 3: Implement** `AppGraphQlController` (`@Controller @ConditionalOnEEVersion @ConditionalOnCoordinator`), delegating every operation to `AppFacade`; no `@PreAuthorize` here — the facade owns it. `appBindableConnections` returns the automation connections in the app's workspace and environment with `type == AUTOMATION`, via the automation `WorkspaceConnectionFacade` the Connections page already uses (grep `workspaceConnections` in automation-configuration GraphQL to find it) filtered by `environmentId == app.environmentId`; `appComponentOperations` returns `ActionDefinitionService.getActionDefinitions(componentName, componentVersion)` names and `ClusterElementDefinitionService.getClusterElementDefinitions(componentName, componentVersion, BaseToolFunction.TOOLS)` names. Response records map `Instant` → `String`.

Add to `codegen.ts` schema list:
`'../server/ee/libs/automation/automation-app/automation-app-graphql/src/main/resources/graphql/*.graphqls',`

- [ ] **Step 4: Run module tests, pass. Step 5: Commit** `- Add the App GraphQL admin surface`.

---

### Task 9: Admin UI — Apps list, create dialog, detail page with Connections tab

**Files:**
- Create: `client/src/graphql/automation/app/{apps,app,appBindings,appBindableConnections,appComponentOperations,createApp,updateApp,updateAppStatus,rotateAppSecretKey,deleteApp,bindAppConnection,unbindAppConnection}.graphql`
- Modify: `client/src/shared/navigation/navigationItems.ts` (Deploy row), `client/src/App.tsx` (EE gate), `client/src/routes.tsx` (two routes)
- Create: `client/src/pages/automation/apps/Apps.tsx`, `App.tsx`, `components/AppDialog.tsx`, `components/AppSecretKeyView.tsx`, `components/app-list/{AppList,AppListItem}.tsx`, `components/AppBindingDialog.tsx`, `components/AppBindingsTab.tsx`, `components/AppOverviewTab.tsx`
- Test: `client/src/pages/automation/apps/tests/Apps.test.tsx`, `tests/AppDialog.test.tsx`

- [ ] **Step 1: Operations and codegen**

Write the twelve `.graphql` files (query names in camelCase matching the schema: `apps`, `app`, `appBindings`, …; mutation names `createApp` etc., selecting every field the UI shows), then:

```bash
cd client && npx graphql-codegen > /tmp/apps-codegen.log 2>&1; echo "exit=$?"
```

Commit the operations and the regenerated `src/shared/middleware/graphql.ts` **separately** (`- client - Add App GraphQL operations`, `- client - Regenerate GraphQL client for Apps`).

- [ ] **Step 2: Navigation + routes**

`navigationItems.ts`: add after the A2A Servers entry

```ts
    {
        group: 'Deploy',
        href: '/automation/apps',
        icon: AppWindowIcon,
        name: 'Apps',
    },
```

and `AppWindowIcon` into the sorted lucide import. `App.tsx` filter: `if (navItem.href === '/automation/apps') { return edition === EditionType.EE; }`. `routes.tsx`: lazy imports `Apps` and `AppDetail` (`@/pages/automation/apps/Apps`, `@/pages/automation/apps/App`), routes `apps` and `apps/:id` shaped exactly like `context-stores` / `context-stores/:id` (ADMIN or USER, `EEVersion`, `LazyLoadWrapper hasLeftSidebar`). Add a test to `navigationItems.test.ts`: `it('lists Apps in the Deploy group', …)`.

- [ ] **Step 3: Failing page test**

`tests/Apps.test.tsx`, following `Variables.test.tsx`'s mocking style:

```tsx
const hoisted = vi.hoisted(() => ({
    apps: [] as unknown[],
    scopes: ['APP_VIEW', 'APP_MANAGE'] as string[],
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAppsQuery: vi.fn(() => ({data: {apps: hoisted.apps}, error: null, isLoading: false})),
    useCreateAppMutation: vi.fn(() => ({isPending: false, mutate: vi.fn()})),
    useDeleteAppMutation: vi.fn(() => ({mutate: vi.fn()})),
    useMyWorkspaceScopesQuery: vi.fn(() => ({data: {myWorkspaceScopes: hoisted.scopes}})),
    useUpdateAppStatusMutation: vi.fn(() => ({mutate: vi.fn()})),
}));

it('shows the empty state with a New App button for managers', () => {
    render(<TooltipProvider><MemoryRouter><Apps /></MemoryRouter></TooltipProvider>);
    expect(screen.getByText('No Apps')).toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'New App'})).toBeInTheDocument();
});

it('hides New App without APP_MANAGE', () => {
    hoisted.scopes = ['APP_VIEW'];
    render(…);
    expect(screen.queryByRole('button', {name: 'New App'})).not.toBeInTheDocument();
});

it('lists apps with status switch and hint', () => {
    hoisted.apps = [{environmentId: '2', hostingTarget: 'VERCEL', id: '1', lastDeployedDate: null, name: 'crm-dashboard',
        secretKeyHint: 'ijklmnop', status: 'ENABLED', url: 'https://crm.vercel.app', workspaceId: '7'}];
    render(…);
    expect(screen.getByText('crm-dashboard')).toBeInTheDocument();
    expect(screen.getByRole('switch')).toBeChecked();
});
```

- [ ] **Step 4: Run** `cd client && npx vitest run src/pages/automation/apps` — expect failure.

- [ ] **Step 5: Implement**

- `Apps.tsx`: copy `ContextStores.tsx`'s skeleton (`LayoutContainer`, `Header`, `PageLoader`, `EmptyList` with `AppWindowIcon`, title "No Apps", message "Register an app to let it call connections through ByteChef."). `canManage` from `useMyWorkspaceScopesQuery` includes `'APP_MANAGE'`. Query variables `{environmentId: String(currentEnvironmentId), workspaceId: String(currentWorkspaceId)}`.
- `AppListItem.tsx`: the `ApiCollectionListItem` two-column rhythm verbatim — left: name (+ description tooltip) on `min-h-8`, hosting target badge + URL on `mt-2 min-h-7`; right: `min-w-52 flex-col items-end gap-y-2` with the status `Switch` (`min-h-8`, calls `updateAppStatus`, disabled without `canManage`) and "Last deployed …" / "Never deployed" on `min-h-7`. Row click navigates to `/automation/apps/${app.id}`; actions menu (Edit, Rotate key, Delete) stops propagation.
- `AppDialog.tsx`: react-hook-form + zod (`name` 1–256 chars, `hostingTarget` enum, optional `description`, `url` must be a URL when present) in the `@/components/Dialog` style of `ApiKeyDialog`. On create success, swap the form for `AppSecretKeyView`.
- `AppSecretKeyView.tsx`: the secret-once block from `ApiKeyDialog` (read-only `Input`, Copy button with `useCopyToClipboard` + `toast`), plus a second read-only block with the two lines to paste into the host's environment:

  ```
  BYTECHEF_URL=<window.location.origin>
  BYTECHEF_APP_KEY=<secret>
  ```

  and the sentence "Save this key now. ByteChef cannot show it again; rotate the key if you lose it."
- `App.tsx` (detail, route `apps/:id`): `useAppQuery({id})`, `KnowledgeBase.tsx`-style shell, `Tabs` with `overview` and `connections` (`activity` is added in Task 17). Header right: Rotate key (opens `AppSecretKeyView` in a dialog after `rotateAppSecretKey` succeeds), status switch, Delete.
- `AppOverviewTab.tsx`: owner, URL, hosting target, key hint (`…ijklmnop`), last used, created/modified.
- `AppBindingsTab.tsx` + `AppBindingDialog.tsx`: table of bindings (binding key, connection name, component, allowed actions count, allowed tools count, proxy methods). Dialog: binding key input (zod regex `^[a-z][a-z0-9_-]{0,63}$`), connection `ComboBox` from `useAppBindableConnectionsQuery`, then — once a connection is chosen — checkbox lists of actions and tools from `useAppComponentOperationsQuery({componentName, componentVersion})`, a Proxy switch and method checkboxes (GET/POST/PUT/PATCH/DELETE/HEAD). Submit → `bindAppConnection`, invalidate `['appBindings']`. Row "Unbind" → `unbindAppConnection`.

Keep object keys sorted, icons with the `Icon` suffix, interfaces ending in `Props`/`I`, and hooks ordered per CLAUDE.md.

- [ ] **Step 6: Run** `cd client && npm run check > /tmp/apps-client.log 2>&1; echo "exit=$?"` — expect `exit=0`.

- [ ] **Step 7: Commit** `- client - Add the Apps list, create dialog and app detail page with connection bindings`.

---

### Task 10: Management REST under `/api/automation/v1/apps`

**Files:**
- Create: `automation-app-public-rest/openapi-management.yaml`
- Modify: `automation-app-public-rest/build.gradle.kts` (two generator tasks)
- Create: `automation-app-public-rest/src/main/java/com/bytechef/ee/automation/app/public_/web/rest/AppApiController.java`
- Create: `…/AppRestExceptionHandler.java`
- Test: `automation-app-public-rest/src/test/java/…/AppApiControllerTest.java` (MockMvc standalone)

- [ ] **Step 1: Generator tasks**

Prepend to `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.org.openapi.generator)
}

val generateOpenAPISpringManagement by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    apiPackage.set("com.bytechef.ee.automation.app.public_.web.rest.management")
    configOptions.set(mapOf("interfaceOnly" to "true", "useSpringBoot3" to "true", "useTags" to "true"))
    generatorName.set("spring")
    inputSpec.set("$projectDir/openapi-management.yaml")
    modelNameSuffix.set("Model")
    modelPackage.set("com.bytechef.ee.automation.app.public_.web.rest.management.model")
    outputDir.set("$projectDir/generated")
}

val generateOpenAPISpring by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    apiPackage.set("com.bytechef.ee.automation.app.public_.web.rest")
    configOptions.set(mapOf("interfaceOnly" to "true", "useSpringBoot3" to "true", "useTags" to "true"))
    generatorName.set("spring")
    inputSpec.set("$projectDir/openapi.yaml")
    modelNameSuffix.set("Model")
    modelPackage.set("com.bytechef.ee.automation.app.public_.web.rest.model")
    outputDir.set("$projectDir/generated")
}

sourceSets.main.get().java.srcDir("$projectDir/generated/src/main/java")

tasks.register("generateOpenAPI") {
    dependsOn(generateOpenAPISpringManagement, generateOpenAPISpring)
}
```

Until Task 14 writes `openapi.yaml`, make `generateOpenAPI` depend only on the management task (add the second dependency in Task 14).

- [ ] **Step 2: `openapi-management.yaml`**

Header `servers: - url: "/api/automation/v1"`, tag `app`, `securitySchemes.bearerAuth` and the `components.responses` + `Errors` schema copied from embedded's spec (section 4 of the explorer report, adjusted `Environment` `$ref` depth: `../../../../../../libs/platform/platform-rest/openapi/components/schemas/objects/environment.yaml` — count directories from this module and verify the file exists). Paths and operationIds:

| Path | op | operationId | request | response |
|---|---|---|---|---|
| `/apps` | GET | `getApps` | query `workspaceId` (int64, required), header `X-Environment` | `[App]` |
| `/apps` | POST | `createApp` | query `workspaceId`, header `X-Environment`, body `AppInput` | `AppSecretKey` (201) |
| `/apps/{id}` | GET | `getApp` | | `App` |
| `/apps/{id}` | PATCH | `updateApp` | body `AppUpdate` (all optional: name, description, status, hostingTarget, url, lastDeployedDate) | `App` |
| `/apps/{id}` | DELETE | `deleteApp` | | 204 |
| `/apps/{id}/secret-key` | POST | `rotateAppSecretKey` | | `AppSecretKey` |
| `/apps/{id}/connections` | GET | `getAppBindings` | | `[AppBinding]` |
| `/apps/{id}/connections/{bindingKey}` | PUT | `bindAppConnection` | body `AppBindingInput` | `AppBinding` |
| `/apps/{id}/connections/{bindingKey}` | DELETE | `unbindAppConnection` | | 204 |

Schemas `App`, `AppSecretKey`, `AppBinding`, `AppInput`, `AppUpdate`, `AppBindingInput` mirror the GraphQL types (dates as `string, format: date-time`). Every operation: `security: - bearerAuth: []` and the 400/401/403/404/409/500 response refs.

Run `./gradlew :server:ee:libs:automation:automation-app:automation-app-public-rest:generateOpenAPI` and commit `generated/`.

- [ ] **Step 3: Failing controller test** — `MockMvcBuilders.standaloneSetup(new AppApiController(appFacade, environmentService)).setControllerAdvice(new AppRestExceptionHandler())`; assert `POST /v1/apps?workspaceId=7` returns 201 with `secretKey`, `DELETE /v1/apps/1` returns 204, and a facade `AccessDeniedException` becomes 403 while `ConfigurationException(APP_NOT_FOUND)` becomes 404.

- [ ] **Step 4: Implement**

`AppApiController` — `@RestController("com.bytechef.ee.automation.app.public_.web.rest.AppApiController") @RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/v1") @ConditionalOnCoordinator @ConditionalOnEEVersion`, implementing the generated `AppApi`, mapping models ↔ DTOs, `Environment environment = environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name())`.

`AppRestExceptionHandler` — `@RestControllerAdvice(assignableTypes = {AppApiController.class})` (Task 14 extends the list) with:

```java
@ExceptionHandler(AccessDeniedException.class)             → 403 {"error": message}
@ExceptionHandler(ConfigurationException.class)            → when exception.getErrorType().getErrorClass() == App.class,
    status from a switch on exception.getErrorKey():
    APP_NOT_FOUND, APP_CONNECTION_NOT_FOUND → 404; APP_DISABLED, APP_OPERATION_NOT_ALLOWED → 403;
    APP_CONNECTION_UNAVAILABLE, APP_PROXY_UNSUPPORTED → 409; APP_EXECUTION_TIMEOUT → 504;
    APP_PROXY_REQUEST_TOO_LARGE → 413; APP_PROXY_RESPONSE_TOO_LARGE → 502; APP_BINDING_KEY_INVALID,
    APP_CONNECTION_NOT_BINDABLE, APP_NAME_ALREADY_EXISTS, APP_PROXY_PATH_REJECTED → 400;
    body {"code": "<constant name>", "message": …}
    Any other ConfigurationException (a component's own error type, e.g. COMPONENT_DISABLED) → 403 for
    ActionDefinitionErrorType.COMPONENT_DISABLED / ACTION_DISABLED, otherwise 422 with
    {"code": "COMPONENT_ERROR", "message": exception.getMessage()} — the spec's "action or tool threw" row.
@ExceptionHandler(ProviderException.class)                 → 422 {"code": "PROVIDER_ERROR", "message": …}
```

Expose the constant name by adding to `AppErrorType` a `public static String nameOf(int errorKey)` built from a static `Map<Integer, String>` — the generic `ErrorType` has no name.

- [ ] **Step 5: Run module tests; pass. Step 6: Commit** `- Add the App management REST API under /api/automation/v1/apps`.

A1 is complete: an admin can register an app, bind connections, rotate and disable from the UI, CLI-shaped REST and GraphQL.

---

# Part A2 — app-facing authentication, action and tool endpoints, logging

### Task 11: A non-user constructor on `AbstractApiKeyAuthenticationToken`

**Files:**
- Modify: `server/libs/platform/platform-security-web/platform-security-web-api/src/main/java/com/bytechef/platform/security/web/authentication/AbstractApiKeyAuthenticationToken.java`
- Test: `server/libs/platform/platform-security-web/platform-security-web-api/src/test/java/com/bytechef/platform/security/web/authentication/AbstractApiKeyAuthenticationTokenTest.java`

**Interfaces:**
- Produces: `protected AbstractApiKeyAuthenticationToken(long environmentId, String tenantId, Object principal, Collection<? extends GrantedAuthority> authorities)` — authenticated, environment present, `getPrincipal()` returns `principal`.

- [ ] **Step 1: Failing test**

```java
class AbstractApiKeyAuthenticationTokenTest {

    private static final class TestToken extends AbstractApiKeyAuthenticationToken {
        TestToken(Object principal) {
            super(2L, "tenant1", principal, List.of(new SimpleGrantedAuthority("ROLE_APP")));
        }
    }

    @Test
    void principalConstructorIsAuthenticatedAndKeepsEnvironmentAndTenant() {
        Object principal = new Object();
        TestToken token = new TestToken(principal);

        assertThat(token.isAuthenticated()).isTrue();
        assertThat(token.getPrincipal()).isSameAs(principal);
        assertThat(token.getTenantId()).isEqualTo("tenant1");
        assertThat(token.fetchEnvironmentId()).hasValue(2L);
        assertThat(token.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_APP");
    }
}
```

- [ ] **Step 2: Run, expect compile failure.**

- [ ] **Step 3: Implement** — add a `private Object principal;` field, the constructor:

```java
    protected AbstractApiKeyAuthenticationToken(
        long environmentId, String tenantId, Object principal, Collection<? extends GrantedAuthority> authorities) {

        super(authorities);

        this.environmentId = environmentId;
        this.environmentIdPresent = true;
        this.principal = principal;
        this.tenantId = tenantId;

        setAuthenticated(true);
        setDetails(principal);
    }
```

and change `getPrincipal()` to `return principal != null ? principal : user;`. The two existing `User` constructors are unchanged.

- [ ] **Step 4: Run** `platform-security-web-api:test`; pass. **Step 5: Commit** `- Let an API key authentication token carry a non-user principal`.

---

### Task 12: `AppPrincipal`, token, converter, provider, configurer

**Files:**
- Create: `automation-app-api/src/main/java/com/bytechef/ee/automation/app/security/AppPrincipal.java`
- Create: `server/ee/libs/automation/automation-security-web/automation-security-web-impl/src/main/java/com/bytechef/ee/automation/security/web/app/{AppAuthenticationToken,AppApiKeyAuthenticationProvider,AppApiKeySecurityConfigurer,AppApiKeySecurityConfigurerContributor}.java`
- Modify: `server/ee/libs/automation/automation-security-web/automation-security-web-impl/build.gradle.kts` (add `implementation(project(":server:ee:libs:automation:automation-app:automation-app-api"))`)
- Modify: `server/apps/server-app/src/main/resources/config/application.yml` and `server/ee/apps/config-server-app/src/main/resources/config/apps/application.yml` — add `app: /api/app` under `openapi.openAPIDefinition.base-path`
- Test: `automation-security-web-impl/src/test/java/com/bytechef/ee/automation/security/web/app/{AppApiKeyAuthenticationConverterTest,AppApiKeyAuthenticationProviderTest,AppApiKeySecurityConfigurerTest}.java`

**Interfaces:**
- Produces: `record AppPrincipal(String tenantId, long appId, long workspaceId, Environment environment)` with `static Optional<AppPrincipal> current()` reading `SecurityContextHolder`; `AppAuthenticationToken` (pre-auth: `(environmentId=-1 placeholder, secretKey, tenantId)`; authenticated: `(AppPrincipal)` → authority `ROLE_APP`); `AppApiKeySecurityConfigurer.PATH_PATTERN = "^/api/app/v[0-9]+/.+"`.

- [ ] **Step 1: Failing tests**

`AppApiKeyAuthenticationConverterTest`:

```java
class AppApiKeyAuthenticationConverterTest {

    private final AppApiKeySecurityConfigurer.AppApiKeyAuthenticationConverter converter =
        new AppApiKeySecurityConfigurer.AppApiKeyAuthenticationConverter();

    @Test
    void readsTenantFromKeyAndIgnoresEnvironmentHeader() {
        String key = String.valueOf(TenantKey.of("tenant9"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + key);
        request.addHeader("X-ENVIRONMENT", "DEVELOPMENT");

        AppAuthenticationToken token = (AppAuthenticationToken) converter.convert(request);

        assertThat(token.getTenantId()).isEqualTo("tenant9");
        assertThat(token.getSecretKey()).isEqualTo(key);
        assertThat(token.isAuthenticated()).isFalse();
    }

    @Test
    void malformedKeyIsBadCredentials() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not-base64!!");

        assertThatThrownBy(() -> converter.convert(request)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void missingHeaderIsBadCredentials() {
        assertThatThrownBy(() -> converter.convert(new MockHttpServletRequest()))
            .isInstanceOf(BadCredentialsException.class);
    }
}
```

`AppApiKeyAuthenticationProviderTest`:

```java
@ExtendWith(MockitoExtension.class)
class AppApiKeyAuthenticationProviderTest {

    @Mock
    private AppService appService;

    @Test
    void unknownKeyIsBadCredentials() {
        when(appService.fetchAppBySecretKey("k")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new AppApiKeyAuthenticationProvider(appService)
            .authenticate(new AppAuthenticationToken("k", "public")))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void disabledAppIsDisabledException() {
        App app = app(AppStatus.DISABLED);
        when(appService.fetchAppBySecretKey("k")).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> new AppApiKeyAuthenticationProvider(appService)
            .authenticate(new AppAuthenticationToken("k", "public")))
            .isInstanceOf(DisabledException.class);
    }

    @Test
    void enabledAppYieldsPrincipalWithEnvironmentFromTheRow() {
        App app = app(AppStatus.ENABLED);
        when(appService.fetchAppBySecretKey("k")).thenReturn(Optional.of(app));

        Authentication authentication = new AppApiKeyAuthenticationProvider(appService)
            .authenticate(new AppAuthenticationToken("k", "public"));

        AppPrincipal principal = (AppPrincipal) authentication.getPrincipal();

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(principal.appId()).isEqualTo(1L);
        assertThat(principal.workspaceId()).isEqualTo(7L);
        assertThat(principal.environment()).isEqualTo(Environment.STAGING);
        assertThat(((AbstractApiKeyAuthenticationToken) authentication).fetchEnvironmentId())
            .hasValue((long) Environment.STAGING.ordinal());
        verify(appService).touchSecretKeyLastUsedDate(1L);
    }

    private static App app(AppStatus status) {
        App app = new App();
        app.setId(1L);
        app.setWorkspaceId(7L);
        app.setEnvironment(Environment.STAGING);
        app.setStatus(status);
        return app;
    }
}
```

`AppApiKeySecurityConfigurerTest`: assert `PATH_PATTERN` matches `/api/app/v1/tools` and not `/api/automation/v1/apps`, and that `registerCsrfOverride` calls `csrf.ignoringRequestMatchers(any(RequestMatcher.class))` (mock `CsrfConfigurer`).

- [ ] **Step 2: Run, expect failure.**

- [ ] **Step 3: Implement**

`AppPrincipal` (api module):

```java
public record AppPrincipal(String tenantId, long appId, long workspaceId, Environment environment) {

    public static Optional<AppPrincipal> current() {
        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof AppPrincipal appPrincipal) {
            return Optional.of(appPrincipal);
        }

        return Optional.empty();
    }

    public static AppPrincipal getCurrent() {
        return current().orElseThrow(() -> new AccessDeniedException("Not an app request"));
    }
}
```

`AppAuthenticationToken`:

```java
public class AppAuthenticationToken extends AbstractApiKeyAuthenticationToken {

    public static final String ROLE_APP = "ROLE_APP";

    private @Nullable String secretKey;

    /** Pre-authentication: environment is not known yet; it belongs to the app row. */
    public AppAuthenticationToken(String secretKey, String tenantId) {
        super(0L, tenantId);

        this.secretKey = secretKey;
    }

    public AppAuthenticationToken(AppPrincipal appPrincipal) {
        super(
            appPrincipal.environment()
                .ordinal(),
            appPrincipal.tenantId(), appPrincipal, List.of(new SimpleGrantedAuthority(ROLE_APP)));
    }

    public @Nullable String getSecretKey() {
        return secretKey;
    }
}
```

`AppApiKeyAuthenticationProvider`:

```java
public class AppApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final AppService appService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        AppAuthenticationToken token = (AppAuthenticationToken) authentication;

        App app = appService.fetchAppBySecretKey(Objects.requireNonNull(token.getSecretKey()))
            .orElseThrow(() -> new BadCredentialsException("Unknown app key"));

        if (app.getStatus() != AppStatus.ENABLED) {
            throw new DisabledException("App is disabled");
        }

        appService.touchSecretKeyLastUsedDate(app.getId());

        return new AppAuthenticationToken(new AppPrincipal(
            token.getTenantId(), app.getId(), Objects.requireNonNull(app.getWorkspaceId()), app.getEnvironment()));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(AppAuthenticationToken.class);
    }
}
```

`DisabledException` is `org.springframework.security.authentication.DisabledException`; the filter's failure handler maps every `AuthenticationException` to 401. The spec wants 403 `APP_DISABLED`: override that in the configurer by passing a failure handler — `ApiKeyAuthenticationFilter`'s handler is a private field, so instead have the **converter** throw nothing and let the provider throw `DisabledException`, then add to `AppRestExceptionHandler` nothing (it never reaches a controller). Accept 401 for disabled from the filter, **and** add an `AppStatusCheck` in `AppExecutionFacadeImpl` (Task 15) that reloads the app and throws `ConfigurationException(APP_DISABLED)` → 403, so a disable that happens between authentication and execution is also caught. Record in the error table of the spec that "disabled" is 401 at the filter and 403 `APP_DISABLED` at the facade.

`AppApiKeySecurityConfigurer`:

```java
public class AppApiKeySecurityConfigurer extends AbstractApiKeyHttpConfigurer {

    public static final String PATH_PATTERN = "^/api/app/v[0-9]+/.+";

    public AppApiKeySecurityConfigurer(AppService appService) {
        super(PATH_PATTERN, new AppApiKeyAuthenticationConverter(), new AppApiKeyAuthenticationProvider(appService));
    }

    @Override
    protected void registerCsrfOverride(CsrfConfigurer<?> csrf) {
        csrf.ignoringRequestMatchers(regexMatcher(PATH_PATTERN));
    }

    static class AppApiKeyAuthenticationConverter extends AbstractApiKeyAuthenticationConverter {

        @Override
        public Authentication convert(HttpServletRequest request) {
            String authToken = getAuthToken(request);

            TenantKey tenantKey;

            try {
                tenantKey = TenantKey.parse(authToken);
            } catch (IllegalArgumentException illegalArgumentException) {
                throw new BadCredentialsException("Malformed app key", illegalArgumentException);
            }

            return new AppAuthenticationToken(authToken, tenantKey.getTenantId());
        }
    }
}
```

`AppApiKeySecurityConfigurerContributor` — `@Configuration @ConditionalOnEEVersion`, copy of `AutomationApiKeySecurityConfigurerContributor` returning `new AppApiKeySecurityConfigurer(appService)`.

yml: add `app: /api/app` beside `automation:`/`embedded:`/`platform:` in both files.

- [ ] **Step 4: Run** `automation-security-web-impl:test`; pass. **Step 5: Commit** `- Authenticate /api/app requests with the app secret key as an AppPrincipal`.

---

### Task 13: Logging — new surfaces, `appId` on the event and the log

**Files:**
- Modify: `server/libs/platform/platform-tool-execution/platform-tool-execution-api/…/ToolExecutionSurface.java` (append `AUTOMATION_APP_ACTION, AUTOMATION_APP_TOOL, AUTOMATION_APP_PROXY`), `ToolExecutionKind.java` (append `HTTP_PROXY`), `ToolExecutionEvent.java` (`@Nullable Long appId` + builder setter)
- Modify: `…/platform-tool-execution-api/src/test/…/ToolExecutionEnumOrdinalStabilityTest.java` (extend the pinned lists)
- Modify: `server/ee/libs/platform/platform-tool-invocation-log/platform-tool-invocation-log-api/…/ToolInvocationLog.java` (`appId`), `…-service/…/ToolInvocationLogEventListener.java` (map it), `ToolInvocationLogRepository.java` + `ToolInvocationLogService(.java/Impl)` (`appId` filter), `…-graphql/…/tool-invocation-log.graphqls` + controller (`appId: Long` argument)
- Create: `…-service/src/main/resources/config/liquibase/changelog/platform/tool/invocation/log/20261006000002_tool_invocation_log_added_app_id.xml`
- Modify: `client/src/graphql/tool-invocation-log/toolInvocationLogs.graphql` (`$appId: Long`), regenerate
- Test: existing `ToolInvocationLog*` tests plus a new case in the listener test.

- [ ] **Step 1: Update the ordinal test first** (it is the failing test): append the three surfaces and `HTTP_PROXY` to the pinned lists.
- [ ] **Step 2: Run** `platform-tool-execution-api:test`; expect failure. **Step 3: Append the enum values and `appId`.** Every call site of the 19-arg record constructor is the builder's `build()` — add `appId` there only.
- [ ] **Step 4: Changeset**

```xml
<changeSet id="20261006000002-1" author="Ivica Cardic">
    <addColumn tableName="tool_invocation_log">
        <column name="app_id" type="BIGINT"/>
    </addColumn>
    <createIndex tableName="tool_invocation_log" indexName="idx_tool_invocation_log_app_created">
        <column name="app_id"/>
        <column name="created_date" descending="true"/>
    </createIndex>
</changeSet>
```

- [ ] **Step 5: Thread `appId`** through `ToolInvocationLog` (field + accessors), the listener (`toolInvocationLog.setAppId(event.appId())`), `findFiltered`/`countFiltered` (`AND (:appId IS NULL OR app_id = :appId)`), the service signature, the GraphQL argument and the client operation (`$appId: Long` → `appId: $appId`; also select `appId`). Run `cd client && npx graphql-codegen`. Add `AUTOMATION_APP_ACTION` / `AUTOMATION_APP_TOOL` / `AUTOMATION_APP_PROXY` to `surfaceOptions` in `ToolInvocations.tsx` and to `SURFACE_LABELS` in `ToolInvocationsTable.tsx` (labels "App action", "App tool", "App proxy").
- [ ] **Step 6: Run** the three modules' `test` and `testIntegration` with `--continue`, and `cd client && npm run check`. **Step 7: Commit** server and client separately: `- Record app invocations with new surfaces and an app_id on the tool invocation log`, `- client - Filter tool invocations by app and label the app surfaces`.

---

### Task 14: App-facing OpenAPI spec and controllers (actions, tools, connections)

**Files:**
- Create: `automation-app-public-rest/openapi.yaml`
- Modify: `automation-app-public-rest/build.gradle.kts` (`generateOpenAPI` depends on both tasks)
- Create: `…/public_/web/rest/AppConnectionApiController.java`, `AppToolApiController.java`
- Modify: `…/AppRestExceptionHandler.java` (`assignableTypes` list)
- Test: `…/AppToolApiControllerTest.java` (MockMvc standalone with a mocked `AppExecutionFacade`)

**Interfaces:**
- Consumes (Task 15): `AppExecutionFacade` —

```java
public interface AppExecutionFacade {
    List<AppBindingDTO> getBindings();                                 // for the current AppPrincipal
    Object executeAction(String bindingKey, String actionName, @Nullable Integer componentVersion, Map<String, Object> input);
    List<AppToolDTO> getTools();
    Object executeTool(String bindingKey, String toolName, Map<String, Object> input);
    ProxyResponse proxy(String bindingKey, ProxyRequest request);      // Task 20
}
public record AppToolDTO(String name, String bindingKey, String componentName, String toolName,
    @Nullable String description, String parameters) {}
```

Write the interface and `AppToolDTO` in this task (api module) so the controllers compile; Task 15 implements it.

- [ ] **Step 1: `openapi.yaml`**

```yaml
---
openapi: "3.0.1"
info:
  title: "The App Public V1 API"
  version: "1"
servers:
  - url: "/api/app/v1"
tags:
  - name: "app-connection"
    description: "The App Connection Public API"
  - name: "app-tool"
    description: "The App Tool Public API"
  - name: "app-proxy"
    description: "The App Proxy Public API"
paths:
  /connections:
    get:
      summary: "Lists the connections bound to the calling app"
      tags: ["app-connection"]
      operationId: "getConnections"
      responses:
        "200":
          description: "The bindings."
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: "#/components/schemas/AppBinding"
        "401": { $ref: "#/components/responses/unauthorized" }
        "403": { $ref: "#/components/responses/forbidden" }
        "500": { $ref: "#/components/responses/internalServerError" }
      security:
        - bearerAuth: [ ]
  /connections/{bindingKey}/actions/{actionName}:
    post:
      summary: "Executes an allowed action through a bound connection"
      tags: ["app-connection"]
      operationId: "executeAction"
      parameters:
        - name: "bindingKey"
          in: "path"
          required: true
          schema: { type: "string" }
        - name: "actionName"
          in: "path"
          required: true
          schema: { type: "string" }
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [input]
              properties:
                componentVersion:
                  type: integer
                  description: "The component version; the latest when omitted."
                input:
                  type: object
                  additionalProperties: true
      responses:
        "200":
          description: "The action result under `result`."
          content:
            application/json:
              schema:
                type: object
        "400": { $ref: "#/components/responses/badRequest" }
        "401": { $ref: "#/components/responses/unauthorized" }
        "403": { $ref: "#/components/responses/forbidden" }
        "404": { $ref: "#/components/responses/notFound" }
        "409": { $ref: "#/components/responses/conflict" }
        "422": { $ref: "#/components/responses/unprocessableEntity" }
        "504": { $ref: "#/components/responses/gatewayTimeout" }
      security:
        - bearerAuth: [ ]
  /tools:
    get:
      summary: "Lists the tools the calling app may execute"
      tags: ["app-tool"]
      operationId: "getTools"
      responses:
        "200":
          description: "The tools."
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: "#/components/schemas/AppTool"
        "401": { $ref: "#/components/responses/unauthorized" }
      security:
        - bearerAuth: [ ]
  /connections/{bindingKey}/tools/{toolName}:
    post:
      summary: "Executes an allowed tool through a bound connection"
      tags: ["app-tool"]
      operationId: "executeTool"
      parameters:
        - name: "bindingKey"
          in: "path"
          required: true
          schema: { type: "string" }
        - name: "toolName"
          in: "path"
          required: true
          schema: { type: "string" }
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [input]
              properties:
                input:
                  type: object
                  additionalProperties: true
      responses:
        "200":
          description: "The tool result under `result`."
          content:
            application/json:
              schema:
                type: object
        "400": { $ref: "#/components/responses/badRequest" }
        "401": { $ref: "#/components/responses/unauthorized" }
        "403": { $ref: "#/components/responses/forbidden" }
        "404": { $ref: "#/components/responses/notFound" }
        "409": { $ref: "#/components/responses/conflict" }
        "422": { $ref: "#/components/responses/unprocessableEntity" }
        "504": { $ref: "#/components/responses/gatewayTimeout" }
      security:
        - bearerAuth: [ ]
  /connections/{bindingKey}/proxy:
    post:
      summary: "Sends a raw HTTP request through a bound connection"
      tags: ["app-proxy"]
      operationId: "proxy"
      parameters:
        - name: "bindingKey"
          in: "path"
          required: true
          schema: { type: "string" }
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/ProxyRequest"
      responses:
        "200":
          description: "The upstream response; `status` is the upstream status."
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ProxyResponse"
        "400": { $ref: "#/components/responses/badRequest" }
        "401": { $ref: "#/components/responses/unauthorized" }
        "403": { $ref: "#/components/responses/forbidden" }
        "404": { $ref: "#/components/responses/notFound" }
        "409": { $ref: "#/components/responses/conflict" }
        "413": { $ref: "#/components/responses/contentTooLarge" }
        "504": { $ref: "#/components/responses/gatewayTimeout" }
      security:
        - bearerAuth: [ ]
components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
  responses:
    # badRequest, unauthorized, forbidden, notFound, conflict, unprocessableEntity, contentTooLarge
    # ("Content too large"), gatewayTimeout ("Gateway timeout"), internalServerError — each
    #   description: …
    #   content: { application/json: { schema: { type: object, properties: { errors: { $ref: "#/components/schemas/Errors" } } } } }
  schemas:
    Errors:
      # copied verbatim from embedded-execution-public-rest/openapi.yaml
    AppBinding:
      type: object
      required: [bindingKey, componentName, connectionVersion, actionNames, toolNames, proxyEnabled, proxyAllowedMethods]
      properties:
        bindingKey: { type: string }
        componentName: { type: string }
        connectionVersion: { type: integer }
        actionNames: { type: array, items: { type: string } }
        toolNames: { type: array, items: { type: string } }
        proxyEnabled: { type: boolean }
        proxyAllowedMethods: { type: array, items: { type: string } }
    AppTool:
      type: object
      required: [name, bindingKey, componentName, toolName, parameters]
      properties:
        name: { type: string, description: "<BINDING_KEY>__<TOOL_NAME>, unique per app, for LLM function calling." }
        bindingKey: { type: string }
        componentName: { type: string }
        toolName: { type: string }
        description: { type: string }
        parameters: { type: string, description: "JSON Schema of the tool input." }
    ProxyRequest:
      type: object
      required: [method, path]
      properties:
        method: { type: string, enum: [GET, POST, PUT, PATCH, DELETE, HEAD] }
        path: { type: string, description: "Relative to the connection's base URI; must start with a single '/'." }
        query:
          type: object
          additionalProperties: { type: array, items: { type: string } }
        headers:
          type: object
          additionalProperties: { type: array, items: { type: string } }
        body: { type: string, nullable: true, description: "UTF-8 text, or base64 when bodyEncoding is BASE64." }
        bodyEncoding: { type: string, enum: [UTF8, BASE64], default: UTF8 }
        contentType: { type: string }
    ProxyResponse:
      type: object
      required: [status, headers, bodyEncoding]
      properties:
        status: { type: integer }
        headers:
          type: object
          additionalProperties: { type: array, items: { type: string } }
        body: { type: string, nullable: true }
        bodyEncoding: { type: string, enum: [UTF8, BASE64] }
```

Expand the `responses` and `Errors` placeholders by copying from the embedded spec; the generator rejects comments-as-content. Run `generateOpenAPI`, commit `generated/`.

- [ ] **Step 2: Failing MockMvc test** — standalone setup of `AppToolApiController(appExecutionFacade)` + advice; `GET /v1/tools` returns the facade's list as JSON; `POST /v1/connections/crm/tools/searchRecords` with `{"input":{"q":"x"}}` returns `{"result": …}`; a facade `ConfigurationException(APP_OPERATION_NOT_ALLOWED)` → 403 with `code: "APP_OPERATION_NOT_ALLOWED"`; `APP_EXECUTION_TIMEOUT` → 504.

- [ ] **Step 3: Implement** `AppConnectionApiController` (implements generated `AppConnectionApi`: `getConnections`, `executeAction`) and `AppToolApiController` (`getTools`, `executeTool`), both `@RestController @RequestMapping("${openapi.openAPIDefinition.base-path.app:/api/app}/v1") @ConditionalOnCoordinator @ConditionalOnEEVersion`, delegating to `AppExecutionFacade`; results wrapped as `Map.of("result", value)` (a null result → `Collections.singletonMap("result", null)`). Add both controllers to `AppRestExceptionHandler`'s `assignableTypes`.

- [ ] **Step 4: Run, pass. Step 5: Commit** `- Add the app-facing connections, actions and tools REST endpoints`.

---

### Task 15: `AppExecutionFacade` — allow-list, executor lane, timeout, recording

**Files:**
- Create: `automation-app-service/…/config/AppProperties.java` (`@ConfigurationProperties("bytechef.app")`, nested `Execution { Duration timeout; }` and `Proxy { long maxRequestBytes; long maxResponseBytes; Duration timeout; }` — no initializers)
- Modify: `server/apps/server-app/src/main/resources/config/application.yml` — under `bytechef:` add

```yaml
  app:
    execution:
      timeout: 60s
    proxy:
      max-request-bytes: 10485760
      max-response-bytes: 10485760
      timeout: 30s
```

  (add the same block to every other yml that carries `bytechef.worker.task.sync-concurrency-limit`; grep for it.) If `ApplicationProperties` (`server/libs/config/app-config`) is the binding root for `bytechef.*`, add the nested `App` class there instead and extend `ApplicationPropertiesTest`; otherwise register `AppProperties` with `@EnableConfigurationProperties` on `AppJdbcRepositoryConfiguration`'s sibling `AppConfiguration`.
- Create: `automation-app-service/…/facade/AppExecutionFacadeImpl.java`
- Test: `automation-app-service/src/test/java/…/facade/AppExecutionFacadeTest.java`, `AppExecutionFacadeIntTest.java`

- [ ] **Step 1: Failing unit test**

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppExecutionFacadeTest {

    @Mock private ActionDefinitionFacade actionDefinitionFacade;
    @Mock private AppConnectionService appConnectionService;
    @Mock private AppService appService;
    @Mock private ClusterElementDefinitionFacade clusterElementDefinitionFacade;
    @Mock private ClusterElementDefinitionService clusterElementDefinitionService;
    @Mock private ComponentDefinitionService componentDefinitionService;
    @Mock private ConnectionService connectionService;
    @Mock private ToolExecutionRecorder toolExecutionRecorder;
    @Mock private WorkspaceConnectionService workspaceConnectionService;

    private final AppProperties appProperties = new AppProperties();
    private AppExecutionFacadeImpl facade;

    @BeforeEach
    void beforeEach() {
        appProperties.getExecution().setTimeout(Duration.ofSeconds(2));

        facade = new AppExecutionFacadeImpl(
            actionDefinitionFacade, appConnectionService, appProperties, appService, clusterElementDefinitionFacade,
            clusterElementDefinitionService, componentDefinitionService, connectionService,
            null /* AppProxyExecutor arrives in Task 19; the production constructor receives it through
                    ObjectProvider<AppProxyExecutor>.getIfAvailable() */,
            new SimpleAsyncTaskExecutor("test-"), toolExecutionRecorder, workspaceConnectionService);

        when(toolExecutionRecorder.record(any(), any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(1);
            return supplier.get();
        });

        App app = new App();
        app.setId(1L);
        app.setWorkspaceId(7L);
        app.setEnvironment(Environment.PRODUCTION);
        app.setStatus(AppStatus.ENABLED);
        when(appService.getApp(1L)).thenReturn(app);

        AppConnection binding = new AppConnection();
        binding.setAppId(1L);
        binding.setBindingKey("crm");
        binding.setConnectionId(5L);
        binding.setOperations(List.of("createRecord"), List.of("searchRecords"));
        when(appConnectionService.fetchBinding(1L, "crm")).thenReturn(Optional.of(binding));
        when(appConnectionService.getBindings(1L)).thenReturn(List.of(binding));

        Connection connection = mock(Connection.class);
        when(connection.getId()).thenReturn(5L);
        when(connection.getComponentName()).thenReturn("salesforce");
        when(connection.getConnectionVersion()).thenReturn(1);
        when(connection.getEnvironmentId()).thenReturn(Environment.PRODUCTION.ordinal());
        when(connection.getStatus()).thenReturn(ConnectionStatus.ACTIVE);
        when(connectionService.getConnection(5L)).thenReturn(connection);
        when(workspaceConnectionService.getWorkspaceConnections(7L)).thenReturn(List.of(new WorkspaceConnection(5L, 7L)));

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);
        when(componentDefinition.getVersion()).thenReturn(3);
        when(componentDefinitionService.getComponentDefinition("salesforce", null)).thenReturn(componentDefinition);
    }

    private <T> T asApp(Supplier<T> supplier) {
        return SecurityUtils.runAs(
            new AppAuthenticationToken(new AppPrincipal("public", 1L, 7L, Environment.PRODUCTION)), supplier);
    }

    @Test
    void allowedActionExecutesWithTheBoundConnectionAndRecords() {
        when(actionDefinitionFacade.executePerform(eq("salesforce"), eq(3), eq("createRecord"), isNull(), isNull(),
            isNull(), isNull(), isNull(), anyMap(), eq(Map.of("salesforce", 5L)), anyMap(),
            eq((long) Environment.PRODUCTION.ordinal()), eq(PlatformType.AUTOMATION), eq(false), isNull(), isNull(),
            isNull())).thenReturn(Map.of("id", "001"));

        Object result = asApp(() -> facade.executeAction("crm", "createRecord", null, Map.of("name", "ACME")));

        assertThat(result).isEqualTo(Map.of("id", "001"));

        ArgumentCaptor<ToolExecutionEvent.Builder> captor = ArgumentCaptor.forClass(ToolExecutionEvent.Builder.class);
        verify(toolExecutionRecorder).record(captor.capture(), any());
        ToolExecutionEvent event = captor.getValue().build();
        assertThat(event.surface()).isEqualTo(ToolExecutionSurface.AUTOMATION_APP_ACTION);
        assertThat(event.appId()).isEqualTo(1L);
        assertThat(event.workspaceId()).isEqualTo(7L);
        assertThat(event.connectionId()).isEqualTo(5L);
    }

    @Test
    void actionNotOnAllowListIsRefused() {
        assertConfigurationError(
            () -> asApp(() -> facade.executeAction("crm", "deleteRecord", null, Map.of())),
            AppErrorType.APP_OPERATION_NOT_ALLOWED);
        verifyNoInteractions(actionDefinitionFacade);
    }

    @Test
    void unknownBindingIsNotFound() {
        assertConfigurationError(
            () -> asApp(() -> facade.executeAction("hr", "createRecord", null, Map.of())),
            AppErrorType.APP_CONNECTION_NOT_FOUND);
    }

    @Test
    void connectionMovedOutOfWorkspaceIsUnavailable() {
        when(workspaceConnectionService.getWorkspaceConnections(7L)).thenReturn(List.of());

        assertConfigurationError(
            () -> asApp(() -> facade.executeAction("crm", "createRecord", null, Map.of())),
            AppErrorType.APP_CONNECTION_UNAVAILABLE);
    }

    @Test
    void disabledAppIsRefusedEvenWithValidPrincipal() {
        App disabled = appService.getApp(1L);
        disabled.setStatus(AppStatus.DISABLED);

        assertConfigurationError(
            () -> asApp(() -> facade.executeAction("crm", "createRecord", null, Map.of())),
            AppErrorType.APP_DISABLED);
    }

    @Test
    void slowActionIsCutOffWithTimeout() {
        when(actionDefinitionFacade.executePerform(any(), anyInt(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any(), anyBoolean(), any(), any(), any()))
            .thenAnswer(invocation -> {
                Thread.sleep(5_000);
                return null;
            });

        assertConfigurationError(
            () -> asApp(() -> facade.executeAction("crm", "createRecord", null, Map.of())),
            AppErrorType.APP_EXECUTION_TIMEOUT);
    }

    @Test
    void unknownComponentVersionIsConfigurationError() {
        when(actionDefinitionFacade.executePerform(any(), eq(99), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any(), anyBoolean(), any(), any(), any()))
            .thenThrow(new ConfigurationException("no such version", ActionDefinitionErrorType.COMPONENT_DISABLED));

        assertThatThrownBy(() -> asApp(() -> facade.executeAction("crm", "createRecord", 99, Map.of())))
            .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void toolsListUsesBindingScopedFlatNamesAndOnlyAllowedTools() {
        ClusterElementDefinition search = mock(ClusterElementDefinition.class);
        when(search.getName()).thenReturn("searchRecords");
        when(search.getDescription()).thenReturn("Search");
        when(search.getProperties()).thenReturn(List.of());
        ClusterElementDefinition delete = mock(ClusterElementDefinition.class);
        when(delete.getName()).thenReturn("deleteRecords");
        when(clusterElementDefinitionService.getClusterElementDefinitions("salesforce", 3, BaseToolFunction.TOOLS))
            .thenReturn(List.of(search, delete));

        List<AppToolDTO> tools = asApp(() -> facade.getTools());

        assertThat(tools).extracting(AppToolDTO::name).containsExactly("CRM__SEARCH_RECORDS");
        assertThat(tools.getFirst().toolName()).isEqualTo("searchRecords");
    }

    @Test
    void twoBindingsOnOneComponentYieldDistinctFlatNames() {
        AppConnection second = new AppConnection();
        second.setAppId(1L);
        second.setBindingKey("support-crm");
        second.setConnectionId(5L);
        second.setOperations(List.of(), List.of("searchRecords"));
        when(appConnectionService.getBindings(1L)).thenReturn(List.of(appConnectionService.fetchBinding(1L, "crm").orElseThrow(), second));

        ClusterElementDefinition search = mock(ClusterElementDefinition.class);
        when(search.getName()).thenReturn("searchRecords");
        when(search.getProperties()).thenReturn(List.of());
        when(clusterElementDefinitionService.getClusterElementDefinitions("salesforce", 3, BaseToolFunction.TOOLS))
            .thenReturn(List.of(search));

        assertThat(asApp(() -> facade.getTools())).extracting(AppToolDTO::name)
            .containsExactly("CRM__SEARCH_RECORDS", "SUPPORT_CRM__SEARCH_RECORDS");
    }

    private static void assertConfigurationError(ThrowingCallable callable, AppErrorType errorType) {
        assertThatThrownBy(callable)
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(errorType.getErrorKey());
    }
}
```

- [ ] **Step 2: Run, expect failure.**

- [ ] **Step 3: Implement `AppExecutionFacadeImpl`**

```java
@Service
@ConditionalOnEEVersion
public class AppExecutionFacadeImpl implements AppExecutionFacade {

    private static final Pattern NON_ALNUM = Pattern.compile("[^A-Za-z0-9]+");

    // fields (alphabetical) + constructor. The executor parameter is
    // @Qualifier(AsyncConfiguration.SYNC_WORKER_EXECUTOR) AsyncTaskExecutor syncWorkerExecutor; the proxy executor
    // parameter is @Nullable AppProxyExecutor appProxyExecutor, which the Spring constructor obtains from
    // ObjectProvider<AppProxyExecutor>.getIfAvailable() (a second, @Autowired constructor) so the facade compiles and
    // boots before Task 19 adds the class. Every call path on the request thread resolves TenantContext and the
    // SecurityContext BEFORE submitting to the lane; the lookup by key is one indexed SELECT per request, uncached.

    @Override
    public List<AppBindingDTO> getBindings() {
        ResolvedApp resolved = resolveApp();

        return appConnectionService.getBindings(resolved.app().getId())
            .stream()
            .map(binding -> toBindingDTO(binding, connectionService.getConnection(binding.getConnectionId())))
            .toList();
    }

    @Override
    public Object executeAction(
        String bindingKey, String actionName, @Nullable Integer componentVersion, Map<String, Object> input) {

        ResolvedBinding resolved = resolveBinding(bindingKey, AppOperationType.ACTION, actionName);
        Connection connection = resolved.connection();
        int version = componentVersion != null ? componentVersion : latestVersion(connection.getComponentName());

        return toolExecutionRecorder.record(
            eventBuilder(ToolExecutionSurface.AUTOMATION_APP_ACTION, ToolExecutionKind.COMPONENT, actionName, resolved)
                .componentVersion(version)
                .operationName(actionName),
            () -> runWithTimeout(() -> actionDefinitionFacade.executePerform(
                connection.getComponentName(), version, actionName, null, null, null, null, null, input,
                Map.of(connection.getComponentName(), connection.getId()), Map.of(),
                (long) resolved.app().getEnvironmentId(), PlatformType.AUTOMATION, false, null, null, null)));
    }

    @Override
    public List<AppToolDTO> getTools() {
        ResolvedApp resolved = resolveApp();
        List<AppToolDTO> tools = new ArrayList<>();

        for (AppConnection binding : appConnectionService.getBindings(resolved.app().getId())) {
            List<String> allowed = binding.getOperationNames(AppOperationType.TOOL);

            if (allowed.isEmpty()) {
                continue;
            }

            Connection connection = connectionService.getConnection(binding.getConnectionId());
            int version = latestVersion(connection.getComponentName());

            for (ClusterElementDefinition definition : clusterElementDefinitionService.getClusterElementDefinitions(
                connection.getComponentName(), version, BaseToolFunction.TOOLS)) {

                if (!allowed.contains(definition.getName())) {
                    continue;
                }

                tools.add(new AppToolDTO(
                    flatToolName(binding.getBindingKey(), definition.getName()), binding.getBindingKey(),
                    connection.getComponentName(), definition.getName(), definition.getDescription(),
                    JsonSchemaGeneratorUtils.generateInputSchema(
                        ToolPropertyUtils.withoutToolOverrideProperties(definition.getProperties()))));
            }
        }

        return tools;
    }

    @Override
    public Object executeTool(String bindingKey, String toolName, Map<String, Object> input) {
        ResolvedBinding resolved = resolveBinding(bindingKey, AppOperationType.TOOL, toolName);
        Connection connection = resolved.connection();
        int version = latestVersion(connection.getComponentName());

        if (connection.getStatus() != ConnectionStatus.ACTIVE) { /* resolveBinding already checked; keep the tool
            path explicit because ClusterElementDefinitionFacadeImpl does not validate connection status */ }

        return toolExecutionRecorder.record(
            eventBuilder(ToolExecutionSurface.AUTOMATION_APP_TOOL, ToolExecutionKind.COMPONENT,
                flatToolName(bindingKey, toolName), resolved)
                .componentVersion(version)
                .operationName(toolName),
            () -> runWithTimeout(() -> clusterElementDefinitionFacade.executeTool(
                connection.getComponentName(), version, toolName, input, connection.getId())));
    }

    static String flatToolName(String bindingKey, String toolName) {
        String key = NON_ALNUM.matcher(bindingKey).replaceAll("_").toUpperCase();
        StringBuilder tool = new StringBuilder();

        for (int i = 0; i < toolName.length(); i++) {
            char c = toolName.charAt(i);

            if (Character.isUpperCase(c) && i > 0) {
                tool.append('_');
            }

            tool.append(Character.toUpperCase(c));
        }

        return key + "__" + tool;
    }

    private ResolvedApp resolveApp() {
        AppPrincipal principal = AppPrincipal.getCurrent();
        App app = appService.getApp(principal.appId());

        if (app.getStatus() != AppStatus.ENABLED) {
            throw new ConfigurationException("App is disabled", AppErrorType.APP_DISABLED);
        }

        return new ResolvedApp(principal, app);
    }

    private ResolvedBinding resolveBinding(String bindingKey, AppOperationType type, String operationName) {
        ResolvedApp resolved = resolveApp();

        AppConnection binding = appConnectionService.fetchBinding(resolved.app().getId(), bindingKey)
            .orElseThrow(() -> new ConfigurationException(
                "Binding '%s' not found".formatted(bindingKey), AppErrorType.APP_CONNECTION_NOT_FOUND));

        if (!binding.allows(type, operationName)) {
            throw new ConfigurationException(
                "%s '%s' is not allowed on binding '%s'".formatted(type, operationName, bindingKey),
                AppErrorType.APP_OPERATION_NOT_ALLOWED);
        }

        return new ResolvedBinding(resolved.principal(), resolved.app(), binding, checkAvailable(resolved.app(), binding));
    }

    ResolvedBinding resolveProxyBinding(String bindingKey, String method) { … same, but checks binding.isProxyEnabled()
        and getProxyAllowedMethodList().contains(method) instead of allows(); used by Task 20 }

    private Connection checkAvailable(App app, AppConnection binding) {
        Connection connection = connectionService.fetchConnection(binding.getConnectionId())
            .orElseThrow(() -> unavailable(binding));

        boolean inWorkspace = workspaceConnectionService.getWorkspaceConnections(Objects.requireNonNull(app.getWorkspaceId()))
            .stream()
            .anyMatch(workspaceConnection -> Objects.equals(workspaceConnection.getConnectionId(), connection.getId()));

        if (!inWorkspace || connection.getEnvironmentId() != app.getEnvironmentId()
            || connection.getStatus() != ConnectionStatus.ACTIVE) {

            throw unavailable(binding);
        }

        return connection;
    }

    private static ConfigurationException unavailable(AppConnection binding) {
        return new ConfigurationException(
            "Connection of binding '%s' is unavailable".formatted(binding.getBindingKey()),
            AppErrorType.APP_CONNECTION_UNAVAILABLE);
    }

    private int latestVersion(String componentName) {
        return componentDefinitionService.getComponentDefinition(componentName, null)
            .getVersion();
    }

    private ToolExecutionEvent.Builder eventBuilder(
        ToolExecutionSurface surface, ToolExecutionKind kind, String toolName, ResolvedBinding resolved) {

        return ToolExecutionEvent.builder(surface, kind, toolName)
            .tenantId(resolved.principal().tenantId())
            .appId(resolved.app().getId())
            .workspaceId(resolved.app().getWorkspaceId())
            .environment(resolved.app().getEnvironmentId())
            .componentName(resolved.connection().getComponentName())
            .connectionId(resolved.connection().getId());
    }

    /**
     * Component code never runs on the request thread. The sync lane is the bulkhead the API Platform and the
     * editor's Test button already share; a burst from one app takes permits here, not from scheduled runs.
     */
    private <T> T runWithTimeout(Callable<T> callable) {
        Future<T> future = syncWorkerExecutor.submit(callable);

        try {
            return future.get(appProperties.getExecution().getTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeoutException) {
            future.cancel(true);

            throw new ConfigurationException(
                "Execution exceeded " + appProperties.getExecution().getTimeout(), timeoutException,
                AppErrorType.APP_EXECUTION_TIMEOUT);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();

            throw new ConfigurationException("Interrupted", interruptedException, AppErrorType.APP_EXECUTION_TIMEOUT);
        } catch (ExecutionException executionException) {
            Throwable cause = executionException.getCause();

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            throw new RuntimeException(cause);
        }
    }

    record ResolvedApp(AppPrincipal principal, App app) {}
    record ResolvedBinding(AppPrincipal principal, App app, AppConnection binding, Connection connection) {}
}
```

`ToolExecutionRecorderImpl` maps a `TimeoutException` anywhere in the cause chain to outcome `TIMEOUT`, so the `ConfigurationException` wrapping it is enough. `TenantContext` and the `SecurityContext` must be read on the request thread (`resolveBinding` runs there; only the `executePerform` call runs on the lane); the `TenantTaskDecorator` propagates the tenant to the lane.

Add to the service `build.gradle.kts`: `implementation(project(":server:libs:config:async-config"))` (for `AsyncConfiguration.SYNC_WORKER_EXECUTOR`) and `implementation("org.springframework.boot:spring-boot")` if `@ConfigurationProperties` needs it.

- [ ] **Step 4: IntTest** `AppExecutionFacadeIntTest` — extends `AppIntTestConfiguration`'s graph with `@MockitoBean(types = {ActionDefinitionFacade.class, ClusterElementDefinitionFacade.class, ClusterElementDefinitionService.class, ComponentDefinitionService.class, ConnectionService.class, WorkspaceConnectionService.class, ToolExecutionRecorder.class})` and a real `SimpleAsyncTaskExecutor` bean named `syncWorkerExecutor`; proves the whole path against Postgres: create app → bind → `runAs(app)` → `executeAction` hits the mocked facade with the bound connection id; then `updateStatus(DISABLED)` → `APP_DISABLED`; `rotateSecretKey` → `fetchAppBySecretKey(old)` empty. Also `deletingBoundConnectionIsRefusedAndKeepsWorkspaceLink` belongs to the connection module, not here: add it to `ConnectionFacadeIntTest` with a stub `ConnectionUsageChecker` bean returning one usage and assert `delete` throws `CONNECTION_IS_USED` and the row survives.

- [ ] **Step 5: Run** service `test` + `testIntegration` and `platform-connection-service:testIntegration`; pass. **Step 6: Commit** `- Execute allowed app actions and tools on the sync worker lane and record them`.

---

### Task 16: Activity tab and app filter on the Tool Invocations page

**Files:**
- Create: `client/src/pages/automation/apps/components/AppActivityTab.tsx`
- Modify: `client/src/pages/automation/apps/App.tsx` (third tab)
- Modify: `client/src/pages/automation/tool-invocations/hooks/useToolInvocations.ts` (`appId` parameter), `ToolInvocations.tsx` (`appOptions` prop like `mcpServerOptions`), `AutomationToolInvocations.tsx` (builds `appOptions` from `useAppsQuery`)
- Test: `client/src/pages/automation/apps/tests/AppActivityTab.test.tsx`

- [ ] **Step 1: Failing test** — mock `useToolInvocationLogsQuery` to return two rows; `render(<AppActivityTab appId="1" />)`; assert the hook was called with `{appId: 1, page: 0}` and both tool names render; with an empty result the text "No invocations recorded for this app yet." renders.
- [ ] **Step 2: Implement** `AppActivityTab` on the `ConnectedUserSheetPanelToolInvocationList` pattern (`useToolInvocationLogsQuery({appId: Number(appId), page})` + `ToolInvocationsTable`, with a Load more button driven by `totalPages`). Thread `appId` through `useToolInvocations` (state + `handleAppChange`), add an **App** `ComboBox` to the Tool Invocations sidebar when `appOptions` is non-empty, and link the Activity tab header to `/automation/executions/tool-invocations` (the page reads `?appId=` on mount into the filter state).
- [ ] **Step 3: Run** `npm run check`; pass. **Step 4: Commit** `- client - Show an app's invocations on its Activity tab and filter Tool Invocations by app`.

A2 is complete: an app with `BYTECHEF_APP_KEY` can list its bindings, execute allowed actions and tools, and every call appears under the app's Activity tab.

---

# Part A3 — connection proxy

### Task 17: Proxy request/response types and path validation

**Files:**
- Create: `automation-app-api/…/proxy/ProxyRequest.java`, `ProxyResponse.java`, `BodyEncoding.java`
- Create: `automation-app-service/…/proxy/ProxyPathValidator.java`
- Test: `automation-app-service/src/test/java/…/proxy/ProxyPathValidatorTest.java`

**Interfaces:**
- Produces:

```java
public enum BodyEncoding { UTF8, BASE64 }
public record ProxyRequest(String method, String path, Map<String, List<String>> query,
    Map<String, List<String>> headers, @Nullable String body, BodyEncoding bodyEncoding, @Nullable String contentType) {}
public record ProxyResponse(int status, Map<String, List<String>> headers, @Nullable String body, BodyEncoding bodyEncoding) {}
public final class ProxyPathValidator {
    /** Returns the normalised path or throws ConfigurationException(APP_PROXY_PATH_REJECTED). */
    public static String validate(String path);
    /** Throws APP_PROXY_PATH_REJECTED unless resolved shares scheme, host, port with base and its path starts with base's path. */
    public static URI pin(String baseUri, String path);
}
```

- [ ] **Step 1: Failing table test**

```java
class ProxyPathValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "https://evil.com/x", "//evil.com/x", "/\\evil.com", "/a/../../b", "/%2e%2e/b", "/a/%2E%2E/b",
        "@evil.com/x", "/a%0d%0ainjected: x", "/a\nb", "relative/path", "", "/a?b=c", "/a#frag", "/a\u0000b"
    })
    void rejects(String path) {
        assertThatThrownBy(() -> ProxyPathValidator.validate(path))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(AppErrorType.APP_PROXY_PATH_REJECTED.getErrorKey());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/services/data/v60.0/query", "/a/b-c_d.json", "/a/%20b", "/sobjects/Account/001"})
    void accepts(String path) {
        assertThat(ProxyPathValidator.validate(path)).isEqualTo(path);
    }

    @Test
    void pinsHostAndBasePath() {
        assertThat(ProxyPathValidator.pin("https://acme.my.salesforce.com", "/services/x"))
            .hasToString("https://acme.my.salesforce.com/services/x");
        assertThat(ProxyPathValidator.pin("https://api.example.com/v2", "/items"))
            .hasToString("https://api.example.com/v2/items");
        assertThatThrownBy(() -> ProxyPathValidator.pin("https://api.example.com/v2", "/../admin"))
            .isInstanceOf(ConfigurationException.class);
        assertThatThrownBy(() -> ProxyPathValidator.pin("https://api.example.com", "/x@evil.com/"))
            .isInstanceOf(ConfigurationException.class);
    }
}
```

- [ ] **Step 2: Run, expect failure.** **Step 3: Implement**

```java
public final class ProxyPathValidator {

    private static final Pattern ALLOWED = Pattern.compile("^/[A-Za-z0-9._~!$&'()*+,;=:%/-]*$");

    public static String validate(String path) {
        if (path == null || !ALLOWED.matcher(path).matches() || path.startsWith("//") || path.contains("@")
            || path.contains("://") || path.contains("\\")) {

            throw rejected(path);
        }

        String decoded;

        try {
            decoded = URLDecoder.decode(path.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw rejected(path);
        }

        for (char c : decoded.toCharArray()) {
            if (c < 0x20 || c == 0x7f) {
                throw rejected(path);
            }
        }

        for (String segment : decoded.split("/")) {
            if (segment.equals("..") || segment.equals(".")) {
                throw rejected(path);
            }
        }

        return path;
    }

    public static URI pin(String baseUri, String path) {
        URI base = URI.create(baseUri.endsWith("/") ? baseUri.substring(0, baseUri.length() - 1) : baseUri);
        URI resolved = URI.create(base + validate(path)).normalize();

        String basePath = base.getPath() == null ? "" : base.getPath();

        if (!Objects.equals(resolved.getScheme(), base.getScheme()) || !Objects.equals(resolved.getHost(), base.getHost())
            || resolved.getPort() != base.getPort() || resolved.getUserInfo() != null
            || resolved.getPath() == null || !resolved.getPath().startsWith(basePath)) {

            throw rejected(path);
        }

        return resolved;
    }

    private static ConfigurationException rejected(String path) {
        return new ConfigurationException("Proxy path rejected: " + path, AppErrorType.APP_PROXY_PATH_REJECTED);
    }
}
```

The regex excludes `?` and `#` (query goes in `ProxyRequest.query`), `@`, backslash and whitespace. `%2e%2e` survives the regex but fails after decoding.

- [ ] **Step 4: Run, pass. Step 5: Commit** `- Add proxy request types and the proxy path validator`.

---

### Task 18: Header hygiene and body limits

**Files:**
- Create: `automation-app-service/…/proxy/ProxyHeaders.java`
- Test: `…/proxy/ProxyHeadersTest.java`

- [ ] **Step 1: Failing test**

```java
class ProxyHeadersTest {

    @Test
    void stripsForbiddenRequestHeadersCaseInsensitively() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("authorization", List.of("Bearer evil"));
        headers.put("PROXY-Authorization", List.of("x"));
        headers.put("Cookie", List.of("a=b"));
        headers.put("host", List.of("evil"));
        headers.put("Connection", List.of("keep-alive"));
        headers.put("Transfer-Encoding", List.of("chunked"));
        headers.put("Accept", List.of("application/json"));
        headers.put("X-Custom", List.of("ok"));

        assertThat(ProxyHeaders.sanitizeRequest(headers)).containsOnlyKeys("Accept", "X-Custom");
    }

    @Test
    void dropsSetCookieFromResponse() {
        assertThat(ProxyHeaders.sanitizeResponse(Map.of("set-cookie", List.of("s=1"), "Content-Type", List.of("a"))))
            .containsOnlyKeys("Content-Type");
    }
}
```

- [ ] **Step 2: Run, fail. Step 3: Implement**

```java
public final class ProxyHeaders {

    private static final Set<String> FORBIDDEN_REQUEST = Set.of(
        "authorization", "proxy-authorization", "cookie", "host", "content-length", "connection", "keep-alive",
        "proxy-authenticate", "te", "trailer", "transfer-encoding", "upgrade");
    private static final Set<String> FORBIDDEN_RESPONSE = Set.of(
        "set-cookie", "connection", "keep-alive", "transfer-encoding", "trailer", "upgrade");

    public static Map<String, List<String>> sanitizeRequest(Map<String, List<String>> headers) { … filter by lower-case key not in FORBIDDEN_REQUEST, keep original casing, LinkedHashMap … }
    public static Map<String, List<String>> sanitizeResponse(Map<String, List<String>> headers) { … }
}
```

- [ ] **Step 4: Pass. Step 5: Commit** `- Add proxy header hygiene`.

---

### Task 19: `AppProxyExecutor`

**Files:**
- Create: `automation-app-service/…/proxy/AppProxyExecutor.java`
- Modify: `automation-app-service/build.gradle.kts` — add `implementation(project(":server:libs:platform:platform-component:platform-component-context:platform-component-context-api"))` (already listed in Task 1; verify) and `implementation(project(":server:libs:core:file-storage:file-storage-api"))` if `FileEntry` is needed for the bounded read
- Test: `…/proxy/AppProxyExecutorTest.java`

**Interfaces:**
- Consumes: `ContextFactory.createContext(String componentName, ComponentConnection componentConnection)`, `ConnectionDefinitionService.executeBaseUri(componentName, componentConnection, context)`, `ComponentVisibilityProvider.isVisible`.
- Produces: `ProxyResponse execute(App app, AppConnection binding, Connection connection, ProxyRequest request)`.

- [ ] **Step 1: Failing test** (mock `ContextFactory` → mock `Context` whose `http(fn)` applies `fn` to a mock `Http` returning a recording `Executor`; mock `ConnectionDefinitionService.executeBaseUri` → `Optional.of("https://api.example.com/v2")`):

```java
@Test
void rejectsMethodNotAllowedOnBinding()            → APP_OPERATION_NOT_ALLOWED when binding allows only GET and request is POST
@Test
void refusesConnectionWithoutAuthorizationType()  → APP_PROXY_UNSUPPORTED
@Test
void refusesConnectionWithoutBaseUri()            → APP_PROXY_UNSUPPORTED when executeBaseUri is empty
@Test
void refusesOversizedRequestBody()                → APP_PROXY_REQUEST_TOO_LARGE for a UTF8 body > maxRequestBytes
@Test
void refusesHiddenComponent()                     → COMPONENT_DISABLED (ConfigurationException) when any ComponentVisibilityProvider.isVisible is false
@Test
void sendsSanitizedHeadersAndPinnedUrlAndWrapsStatus() {
    // binding allows GET+POST, request GET /items?x=1 with headers {Authorization: evil, Accept: json}
    // executor records: url "/items" (relative — HttpClientExecutor joins the base URI), method GET,
    // headers without Authorization, query {x:[1]}, configuration responseType BINARY, requestTimeout 30s
    // response: status 404, headers {Content-Type: application/json, Set-Cookie: s}, body bytes "{}"
    // assert ProxyResponse(status=404, headers without Set-Cookie, body "{}", UTF8)
}
@Test
void base64EncodesNonTextBodies()                 → Content-Type application/octet-stream → bodyEncoding BASE64
@Test
void refusesOversizedResponse()                   → APP_PROXY_RESPONSE_TOO_LARGE when the stored body exceeds maxResponseBytes
```

- [ ] **Step 2: Run, fail. Step 3: Implement**

```java
@Component
@ConditionalOnEEVersion
public class AppProxyExecutor {

    private final AppProperties appProperties;
    private final List<ComponentVisibilityProvider> componentVisibilityProviders;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final ContextFactory contextFactory;

    public ProxyResponse execute(App app, AppConnection binding, Connection connection, ProxyRequest request) {
        String method = request.method().toUpperCase();

        if (!binding.isProxyEnabled() || !binding.getProxyAllowedMethodList().contains(method)) {
            throw new ConfigurationException(
                "Proxy %s is not allowed on binding '%s'".formatted(method, binding.getBindingKey()),
                AppErrorType.APP_OPERATION_NOT_ALLOWED);
        }

        if (connection.getAuthorizationType() == null) {
            throw new ConfigurationException(
                "Connection has no authorization to inject", AppErrorType.APP_PROXY_UNSUPPORTED);
        }

        if (!componentVisibilityProviders.stream().allMatch(provider -> provider.isVisible(connection.getComponentName()))) {
            throw new ConfigurationException(
                "Component '%s' is disabled".formatted(connection.getComponentName()),
                ActionDefinitionErrorType.COMPONENT_DISABLED);
        }

        byte[] bodyBytes = decodeBody(request);

        if (bodyBytes.length > appProperties.getProxy().getMaxRequestBytes()) {
            throw new ConfigurationException("Request body too large", AppErrorType.APP_PROXY_REQUEST_TOO_LARGE);
        }

        ComponentConnection componentConnection = new ComponentConnection(
            connection.getComponentName(), connection.getConnectionVersion(), connection.getId(),
            connection.getParameters(), connection.getAuthorizationType());

        Context context = contextFactory.createContext(connection.getComponentName(), componentConnection);

        String baseUri = connectionDefinitionService.executeBaseUri(connection.getComponentName(), componentConnection, context)
            .orElseThrow(() -> new ConfigurationException(
                "Connection has no base URI", AppErrorType.APP_PROXY_UNSUPPORTED));

        String path = ProxyPathValidator.validate(request.path());

        ProxyPathValidator.pin(baseUri, path);   // host pinning; the relative path is what HttpClientExecutor joins

        Map<String, List<String>> headers = new HashMap<>(ProxyHeaders.sanitizeRequest(request.headers()));

        Http.Response response = context.http(http -> http
            .exchange(path, Http.RequestMethod.valueOf(method))
            .headers(headers)
            .queryParameters(request.query() == null ? Map.of() : request.query())
            .body(toBody(bodyBytes, request.contentType()))
            .configuration(Http.responseType(Http.ResponseType.BINARY)
                .requestTimeout(appProperties.getProxy().getTimeout()))
            .execute());

        return toProxyResponse(response, context);
    }

    // decodeBody: UTF8 → getBytes, BASE64 → Base64 decoder; null body → empty array.
    // toBody: null for empty; else Body.of(fileEntry) is not available without storage — use
    //   Body.of(new String(bytes, UTF_8), contentType == null ? "application/octet-stream" : contentType) for text
    //   content types and a temp FileEntry via context.file(file -> file.storeContent(name, new ByteArrayInputStream(bytes)))
    //   for binary ones.
    // toProxyResponse: BINARY responses arrive as a FileEntry; read it through
    //   context.file(file -> file.getStream(fileEntry)) with a bounded copy (maxResponseBytes + 1; throw
    //   APP_PROXY_RESPONSE_TOO_LARGE when exceeded); if Content-Type starts with "text/" or is application/json|xml|
    //   x-www-form-urlencoded → UTF8 body, else BASE64. Sanitize response headers. A 204 / empty body → body null.
}
```

The response status passes through unchanged: `HttpClientExecutor.handleResponse` returns `ResponseImpl(headers, body, status)` for any status, so a 404 from the provider is a 200 proxy response with `status: 404`. Verify in `HttpClientExecutor.handleResponse` that non-2xx statuses do not throw before relying on this; if `ProviderException` is thrown for 4xx/5xx, catch it and map to a `ProxyResponse` with that status and the exception message as body.

- [ ] **Step 4: Pass. Step 5: Commit** `- Add the app connection proxy executor with host pinning and size limits`.

---

### Task 20: Proxy endpoint, facade wiring, recording

**Files:**
- Modify: `automation-app-service/…/facade/AppExecutionFacadeImpl.java` (`proxy(...)`, `resolveProxyBinding`)
- Create: `…/public_/web/rest/AppProxyApiController.java`
- Modify: `AppRestExceptionHandler` (add controller; map `APP_PROXY_REQUEST_TOO_LARGE` → 413, `APP_PROXY_RESPONSE_TOO_LARGE` → 502)
- Test: extend `AppExecutionFacadeTest` with `proxyRecordsMethodAndPathWithoutQuery`; `AppProxyApiControllerTest` (MockMvc).

- [ ] **Step 1: Failing test** — `proxy("crm", new ProxyRequest("GET", "/items", Map.of("q", List.of("secret")), Map.of(), null, BodyEncoding.UTF8, null))` on a binding with `proxyEnabled=true`, methods `GET`: the mocked `AppProxyExecutor` is invoked; the recorded event has `surface AUTOMATION_APP_PROXY`, `kind HTTP_PROXY`, `operationName "GET /items"` and `toolName "GET /items"` — never containing `secret`.

- [ ] **Step 2: Implement**

```java
@Override
public ProxyResponse proxy(String bindingKey, ProxyRequest request) {
    ResolvedBinding resolved = resolveProxyBinding(bindingKey, request.method().toUpperCase());
    String operation = request.method().toUpperCase() + " " + request.path();

    return toolExecutionRecorder.record(
        eventBuilder(ToolExecutionSurface.AUTOMATION_APP_PROXY, ToolExecutionKind.HTTP_PROXY, operation, resolved)
            .operationName(operation),
        () -> runWithTimeout(() -> appProxyExecutor.execute(
            resolved.app(), resolved.binding(), resolved.connection(), request)));
}
```

`AppProxyApiController` implements the generated `AppProxyApi`, maps `ProxyRequestModel` ↔ `ProxyRequest` and `ProxyResponse` ↔ `ProxyResponseModel`.

- [ ] **Step 3: Run** service and public-rest tests; pass. **Step 4: Commit** `- Add the app connection proxy endpoint and record proxy calls`.

---

### Task 21: Full check, documentation, spec sync

- [ ] **Step 1:** `./gradlew check --continue -p server/ee/libs/automation/automation-app > /tmp/apps-check.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED" /tmp/apps-check.log` — SpotBugs, PMD, Checkstyle on every new module; fix every finding.
- [ ] **Step 2:** `./gradlew check --continue` for the modified modules: `platform-security-web-api`, `platform-connection-service`, `platform-tool-execution-api`, `platform-tool-invocation-log-*`, `automation-security-web-impl`, `automation-configuration-service`.
- [ ] **Step 3:** `cd client && npm run check`.
- [ ] **Step 4:** Boot smoke: start infra, `./gradlew -p server/apps/server-app bootRun` with `bytechef.edition=ee`; in the UI create an app, bind a connection, copy the key; `curl -H "Authorization: Bearer $KEY" http://localhost:9555/api/app/v1/connections` returns the binding; disable the app in the UI → the same curl returns 401; a user API key on `/api/app/v1/tools` is 401; the app key on `/api/automation/v1/apps` is 401.
- [ ] **Step 5:** Write `.agents/apps.md` (module map, the key format and why it is hashed unsalted, the two-filter-chain rule, the sync lane, the proxy's pinning rules, the deletion guard SPI) and add one row to the deep-dive table in `CLAUDE.md`. Update the spec: Limits bullet (see "One deviation"), error table rows for disabled (401 filter / 403 facade) and 413/502 proxy codes.
- [ ] **Step 6: Commit** `- docs - Add the Apps deep-dive and sync the spec with the implementation`.
