# @AuditConnection Annotation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace manual `connectionAuditPublisher.publish()` calls with a declarative `@AuditConnection` annotation and aspect using SpEL expressions.

**Architecture:** A `@AuditConnection` annotation with nested `@AuditData` is placed on facade methods. A `ConnectionAuditAspect` using `@AfterReturning` evaluates SpEL expressions against method parameters and the return value, then delegates to the existing `ConnectionAuditPublisher`. The `reassignAllConnections` loop case keeps its manual call.

**Tech Stack:** Spring AOP, Spring Expression Language (SpEL), `MethodBasedEvaluationContext`.

---

### Task 1: Create @AuditConnection annotation

**Files:**
- Create: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/audit/AuditConnection.java`

- [ ] **Step 1: Create the annotation file**

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

package com.bytechef.platform.connection.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative connection audit annotation. Methods annotated with this will automatically publish a
 * {@link ConnectionAuditEvent} after successful completion. SpEL expressions in {@link #connectionId()} and
 * {@link AuditData#value()} are evaluated against method parameters ({@code #paramName}) and the return value
 * ({@code #result}).
 *
 * @author Ivica Cardic
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AuditConnection {

    /**
     * The audit event type to publish.
     */
    ConnectionAuditEvent event();

    /**
     * SpEL expression resolving to the connection ID. Examples: {@code "#connectionId"}, {@code "#result"}.
     */
    String connectionId();

    /**
     * Additional key-value pairs to include in the audit event data. Each value is a SpEL expression.
     */
    AuditData[] data() default {};

    /**
     * A key-value pair for audit event data. The value is a SpEL expression evaluated against method parameters and the
     * return value.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface AuditData {

        String key();

        /**
         * SpEL expression. Examples: {@code "#projectId"}, {@code "'PRIVATE'"}, {@code "#result.name()"}.
         */
        String value();
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/audit/AuditConnection.java
git commit -m "Add @AuditConnection annotation with nested @AuditData for declarative connection auditing"
```

---

### Task 2: Create ConnectionAuditAspect

**Files:**
- Create: `server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/audit/ConnectionAuditAspect.java`
- Modify: `server/libs/platform/platform-connection/platform-connection-service/build.gradle.kts`

- [ ] **Step 1: Add spring-aop dependency to build.gradle.kts**

Add to the `dependencies` block in `server/libs/platform/platform-connection/platform-connection-service/build.gradle.kts`:

```kotlin
implementation("org.springframework:spring-aop")
implementation("org.aspectj:aspectjweaver")
```

- [ ] **Step 2: Create the aspect**

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

package com.bytechef.platform.connection.audit;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * Aspect that intercepts methods annotated with {@link AuditConnection} and publishes a connection audit event after
 * successful method completion. SpEL expressions in the annotation are evaluated against the method parameters and
 * return value.
 *
 * @author Ivica Cardic
 */
@Aspect
@Component
public class ConnectionAuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionAuditAspect.class);

    private final ConnectionAuditPublisher connectionAuditPublisher;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @SuppressFBWarnings("EI")
    public ConnectionAuditAspect(ConnectionAuditPublisher connectionAuditPublisher) {
        this.connectionAuditPublisher = connectionAuditPublisher;
    }

    @AfterReturning(pointcut = "@annotation(auditConnection)", returning = "result")
    public void audit(JoinPoint joinPoint, AuditConnection auditConnection, Object result) {
        try {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            Method method = methodSignature.getMethod();

            EvaluationContext evaluationContext = buildEvaluationContext(method, joinPoint.getArgs(), result);

            long connectionId = evaluateConnectionId(auditConnection.connectionId(), evaluationContext);

            Map<String, Object> data = evaluateAuditData(auditConnection.data(), evaluationContext);

            connectionAuditPublisher.publish(auditConnection.event(), connectionId, data);
        } catch (Exception exception) {
            logger.error(
                "Failed to publish audit event {} for method {}",
                auditConnection.event(),
                joinPoint.getSignature().toShortString(),
                exception);
        }
    }

    private EvaluationContext buildEvaluationContext(Method method, Object[] args, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);

        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        context.setVariable("result", result);

        return context;
    }

    private long evaluateConnectionId(String expression, EvaluationContext evaluationContext) {
        Object value = expressionParser.parseExpression(expression).getValue(evaluationContext);

        if (value instanceof Number number) {
            return number.longValue();
        }

        throw new IllegalArgumentException(
            "connectionId expression '%s' did not evaluate to a number, got: %s".formatted(expression, value));
    }

    private Map<String, Object> evaluateAuditData(
        AuditConnection.AuditData[] auditDataEntries, EvaluationContext evaluationContext) {

        if (auditDataEntries.length == 0) {
            return Map.of();
        }

        Map<String, Object> data = new HashMap<>();

        for (AuditConnection.AuditData entry : auditDataEntries) {
            Object value = expressionParser.parseExpression(entry.value()).getValue(evaluationContext);

            data.put(entry.key(), value != null ? value.toString() : "null");
        }

        return data;
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/libs/platform/platform-connection/platform-connection-service/build.gradle.kts \
  server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/audit/ConnectionAuditAspect.java
git commit -m "Add ConnectionAuditAspect that processes @AuditConnection via SpEL"
```

---

### Task 3: Annotate WorkspaceConnectionFacadeImpl and remove manual publish calls

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionFacade.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionFacadeImpl.java`

- [ ] **Step 1: Change promoteToWorkspace return type in the interface**

In `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionFacade.java`, change:

```java
void promoteToWorkspace(long workspaceId, long connectionId);
```

to:

```java
ConnectionVisibility promoteToWorkspace(long workspaceId, long connectionId);
```

Add import `com.bytechef.platform.connection.domain.ConnectionVisibility` if not already present.

- [ ] **Step 2: Rewrite WorkspaceConnectionFacadeImpl**

Read the current `WorkspaceConnectionFacadeImpl.java` first.

Changes:
1. Remove the `ConnectionAuditPublisher` field and constructor parameter
2. Remove `import com.bytechef.platform.connection.audit.ConnectionAuditPublisher`
3. Remove `import com.bytechef.platform.connection.audit.ConnectionAuditEvent`
4. Add `import com.bytechef.platform.connection.audit.AuditConnection`
5. Add `import com.bytechef.platform.connection.audit.AuditConnection.AuditData`
6. Add static imports: `import static com.bytechef.platform.connection.audit.ConnectionAuditEvent.*`
7. Add `@AuditConnection` annotations to all 5 methods that had manual publish calls
8. Remove all `connectionAuditPublisher.publish(...)` calls
9. Change `promoteToWorkspace` return type to `ConnectionVisibility` and return `currentVisibility`
10. Change `create` to return `connectionId` directly (remove the intermediate `publish` call before return)

The 5 annotated methods:

```java
@Override
@AuditConnection(event = CONNECTION_CREATED, connectionId = "#result",
    data = @AuditData(key = "visibility", value = "'PRIVATE'"))
public long create(long workspaceId, ConnectionDTO connectionDTO) {
    long connectionId = connectionFacade.create(connectionDTO, PlatformType.AUTOMATION);

    workspaceConnectionService.create(connectionId, workspaceId);

    return connectionId;
}

@Override
@AuditConnection(event = CONNECTION_DEMOTED, connectionId = "#connectionId",
    data = @AuditData(key = "toVisibility", value = "'PRIVATE'"))
public void demoteToPrivate(long workspaceId, long connectionId) {
    validateConnectionBelongsToWorkspace(workspaceId, connectionId);
    validateConnectionNotUsedByDeployments(connectionId);

    projectConnectionService.deleteByConnectionId(connectionId);

    connectionService.updateVisibility(connectionId, ConnectionVisibility.PRIVATE);
}

@Override
@AuditConnection(event = CONNECTION_PROMOTED, connectionId = "#connectionId",
    data = {@AuditData(key = "fromVisibility", value = "#result.name()"),
            @AuditData(key = "toVisibility", value = "'WORKSPACE'")})
public ConnectionVisibility promoteToWorkspace(long workspaceId, long connectionId) {
    validateConnectionBelongsToWorkspace(workspaceId, connectionId);

    ConnectionVisibility currentVisibility = connectionService.getConnection(connectionId)
        .getVisibility();

    if (currentVisibility.isAtLeast(ConnectionVisibility.WORKSPACE)) {
        throw new ConfigurationException(
            "Connection id=%s already has %s visibility and cannot be promoted to WORKSPACE".formatted(
                connectionId, currentVisibility),
            ConnectionErrorType.INVALID_CONNECTION);
    }

    connectionService.updateVisibility(connectionId, ConnectionVisibility.WORKSPACE);

    return currentVisibility;
}

@Override
@AuditConnection(event = CONNECTION_REVOKED, connectionId = "#connectionId",
    data = @AuditData(key = "projectId", value = "T(String).valueOf(#projectId)"))
public void revokeConnectionFromProject(long workspaceId, long connectionId, long projectId) {
    validateConnectionBelongsToWorkspace(workspaceId, connectionId);
    validateConnectionNotUsedByDeployments(connectionId);

    projectConnectionService.delete(connectionId, projectId);

    List<ProjectConnection> remaining = projectConnectionService.getConnectionProjects(connectionId);

    if (remaining.isEmpty()) {
        connectionService.updateVisibility(connectionId, ConnectionVisibility.PRIVATE);
    }
}

@Override
@AuditConnection(event = CONNECTION_SHARED, connectionId = "#connectionId",
    data = @AuditData(key = "projectId", value = "T(String).valueOf(#projectId)"))
public void shareConnectionToProject(long workspaceId, long connectionId, long projectId) {
    validateConnectionBelongsToWorkspace(workspaceId, connectionId);

    ConnectionVisibility currentVisibility = connectionService.getConnection(connectionId)
        .getVisibility();

    if (currentVisibility.isAtLeast(ConnectionVisibility.WORKSPACE)) {
        throw new ConfigurationException(
            "Connection id=%s has %s visibility and cannot be shared at PROJECT level".formatted(
                connectionId, currentVisibility),
            ConnectionErrorType.INVALID_CONNECTION);
    }

    projectConnectionService.create(connectionId, projectId);

    connectionService.updateVisibility(connectionId, ConnectionVisibility.PROJECT);
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-api/ \
  server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionFacadeImpl.java
git commit -m "Replace manual audit publish calls with @AuditConnection in WorkspaceConnectionFacadeImpl"
```

---

### Task 4: Annotate OrganizationConnectionFacadeImpl

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/OrganizationConnectionFacadeImpl.java`

- [ ] **Step 1: Update OrganizationConnectionFacadeImpl**

Read the current file first.

Changes:
1. Remove `ConnectionAuditPublisher` field and constructor parameter
2. Remove `import com.bytechef.platform.connection.audit.ConnectionAuditPublisher`
3. Remove `import com.bytechef.platform.connection.audit.ConnectionAuditEvent`
4. Remove `import java.util.Map`
5. Add `import com.bytechef.platform.connection.audit.AuditConnection`
6. Add `import com.bytechef.platform.connection.audit.AuditConnection.AuditData`
7. Add `import static com.bytechef.platform.connection.audit.ConnectionAuditEvent.CONNECTION_CREATED`
8. Add `@AuditConnection` to the `create` method, remove manual publish call

The annotated `create` method:

```java
@Override
@Transactional
@AuditConnection(event = CONNECTION_CREATED, connectionId = "#result",
    data = @AuditData(key = "visibility", value = "'ORGANIZATION'"))
public long create(ConnectionDTO connectionDTO) {
    long connectionId = connectionFacade.create(connectionDTO, PlatformType.AUTOMATION);

    connectionService.updateVisibility(connectionId, ConnectionVisibility.ORGANIZATION);

    return connectionId;
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/OrganizationConnectionFacadeImpl.java
git commit -m "Replace manual audit publish with @AuditConnection in OrganizationConnectionFacadeImpl"
```

---

### Task 5: Annotate ConnectionReassignmentFacadeImpl (reassignConnection only)

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/ConnectionReassignmentFacadeImpl.java`

- [ ] **Step 1: Update ConnectionReassignmentFacadeImpl**

Read the current file first.

Changes:
1. Add `import com.bytechef.platform.connection.audit.AuditConnection`
2. Add `import com.bytechef.platform.connection.audit.AuditConnection.AuditData`
3. Add `import static com.bytechef.platform.connection.audit.ConnectionAuditEvent.CONNECTION_REASSIGNED`
4. Add `@AuditConnection` to `reassignConnection` and remove its manual publish call
5. Keep `ConnectionAuditPublisher` field — still needed for the `reassignAllConnections` loop
6. Keep the manual publish call in `reassignAllConnections`

The annotated `reassignConnection` method:

```java
@Override
@AuditConnection(event = CONNECTION_REASSIGNED, connectionId = "#connectionId",
    data = @AuditData(key = "newOwnerLogin", value = "#newOwnerLogin"))
public void reassignConnection(long workspaceId, long connectionId, String newOwnerLogin) {
    validateConnectionBelongsToWorkspace(workspaceId, connectionId);
    validateUserExists(newOwnerLogin);

    reassignConnectionWithoutAudit(connectionId, newOwnerLogin);
}
```

Note: the method signature has `workspaceId` as the first param but the SpEL expression `#connectionId` references the second param — this is correct because SpEL resolves by parameter name.

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/ConnectionReassignmentFacadeImpl.java
git commit -m "Replace manual audit publish with @AuditConnection in ConnectionReassignmentFacadeImpl.reassignConnection"
```

---

### Task 6: Adapt GraphQL controller for promoteToWorkspace return type change

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/com/bytechef/automation/configuration/web/graphql/ConnectionGraphQlController.java`

- [ ] **Step 1: Update the controller method**

Read the current file first.

The `promoteConnectionToWorkspace` method currently calls `workspaceConnectionFacade.promoteToWorkspace(...)` and ignores the return. Since `promoteToWorkspace` now returns `ConnectionVisibility`, the controller just ignores it (the GraphQL mutation still returns `Boolean!`):

```java
@MutationMapping(name = "promoteConnectionToWorkspace")
@PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
public boolean promoteConnectionToWorkspace(@Argument long workspaceId, @Argument long connectionId) {
    workspaceConnectionFacade.promoteToWorkspace(workspaceId, connectionId);

    return true;
}
```

No actual code change needed — calling a method that returns a value and ignoring the return is valid Java. Just verify the file compiles.

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-graphql:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit (if any formatting changes)**

Only commit if spotless or other formatting changes were applied.

---

### Task 7: Update facade tests to remove ConnectionAuditPublisher mocks

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionFacadeTest.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/OrganizationConnectionFacadeTest.java`

- [ ] **Step 1: Update WorkspaceConnectionFacadeTest**

Read the current file first.

Changes:
1. Remove `@Mock private ConnectionAuditPublisher connectionAuditPublisher;`
2. Remove `connectionAuditPublisher` from the `WorkspaceConnectionFacadeImpl` constructor call in `setUp()`
3. Remove all `verify(connectionAuditPublisher).publish(...)` assertions
4. Update `testPromoteToWorkspace` to assert the returned `ConnectionVisibility` instead of void
5. Remove `import com.bytechef.platform.connection.audit.ConnectionAuditEvent`
6. Remove `import com.bytechef.platform.connection.audit.ConnectionAuditPublisher`

- [ ] **Step 2: Update OrganizationConnectionFacadeTest**

Read the current file first.

Changes:
1. Remove `@Mock private ConnectionAuditPublisher connectionAuditPublisher;`
2. Remove `connectionAuditPublisher` from the `OrganizationConnectionFacadeImpl` constructor call in `setUp()`
3. Remove all `verify(connectionAuditPublisher).publish(...)` assertions
4. Remove `import com.bytechef.platform.connection.audit.ConnectionAuditPublisher`
5. Remove `import com.bytechef.platform.connection.audit.ConnectionAuditEvent`

- [ ] **Step 3: Run tests**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 4: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-service/src/test/
git commit -m "Remove ConnectionAuditPublisher mocks from facade tests after @AuditConnection migration"
```

---

### Task 8: Final verification

- [ ] **Step 1: Run full compile**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run unit tests for affected modules**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test :server:libs:platform:platform-connection:platform-connection-service:test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Run spotless**

Run: `./gradlew spotlessApply`

- [ ] **Step 4: Commit formatting if any**

```bash
git add -A && git commit -m "Apply code formatting"
```
