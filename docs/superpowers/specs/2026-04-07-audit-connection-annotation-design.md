# @AuditConnection Annotation Design

**Date:** 2026-04-07
**Status:** Approved

## Overview

Replace manual `connectionAuditPublisher.publish()` calls with a declarative `@AuditConnection` annotation processed by an `@AfterReturning` aspect. The annotation uses SpEL expressions to extract `connectionId` and additional audit data from method parameters and return values.

## Annotation

**Location:** `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/audit/AuditConnection.java`

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AuditConnection {
    ConnectionAuditEvent event();
    String connectionId();  // SpEL: "#connectionId", "#result", etc.
    AuditData[] data() default {};

    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface AuditData {
        String key();
        String value();  // SpEL: "#projectId", "'PRIVATE'", "#result.name()", etc.
    }
}
```

## Aspect

**Location:** `server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/audit/ConnectionAuditAspect.java`

- Uses `@AfterReturning` (fires only on successful completion, not on exceptions)
- Evaluates SpEL expressions against method parameters (via `MethodBasedEvaluationContext`) and the return value (`#result`)
- Builds `Map<String, Object>` from `@AuditData` entries
- Delegates to existing `ConnectionAuditPublisher.publish(event, connectionId, data)`

```java
@Aspect
@Component
public class ConnectionAuditAspect {

    private final ConnectionAuditPublisher connectionAuditPublisher;

    @AfterReturning(pointcut = "@annotation(auditConnection)", returning = "result")
    public void audit(JoinPoint joinPoint, AuditConnection auditConnection, Object result) {
        // 1. Build SpEL evaluation context with method params + result
        // 2. Evaluate connectionId expression → long
        // 3. Evaluate each AuditData value expression → build Map
        // 4. connectionAuditPublisher.publish(event, connectionId, dataMap)
    }
}
```

## Call Site Transformations

### WorkspaceConnectionFacadeImpl (5 calls → 5 annotations)

**create:**
```java
@AuditConnection(event = CONNECTION_CREATED, connectionId = "#result",
    data = @AuditData(key = "visibility", value = "'PRIVATE'"))
public long create(long workspaceId, ConnectionDTO connectionDTO) {
    long connectionId = connectionFacade.create(connectionDTO, PlatformType.AUTOMATION);
    workspaceConnectionService.create(connectionId, workspaceId);
    return connectionId;
}
```

**demoteToPrivate:**
```java
@AuditConnection(event = CONNECTION_DEMOTED, connectionId = "#connectionId",
    data = @AuditData(key = "toVisibility", value = "'PRIVATE'"))
public void demoteToPrivate(long workspaceId, long connectionId) { ... }
```

**promoteToWorkspace** (return type changes from `void` to `ConnectionVisibility`):
```java
@AuditConnection(event = CONNECTION_PROMOTED, connectionId = "#connectionId",
    data = {@AuditData(key = "fromVisibility", value = "#result.name()"),
            @AuditData(key = "toVisibility", value = "'WORKSPACE'")})
public ConnectionVisibility promoteToWorkspace(long workspaceId, long connectionId) {
    validateConnectionBelongsToWorkspace(workspaceId, connectionId);
    ConnectionVisibility currentVisibility = connectionService.getConnection(connectionId).getVisibility();
    // ... validation ...
    connectionService.updateVisibility(connectionId, ConnectionVisibility.WORKSPACE);
    return currentVisibility;
}
```

**revokeConnectionFromProject:**
```java
@AuditConnection(event = CONNECTION_REVOKED, connectionId = "#connectionId",
    data = @AuditData(key = "projectId", value = "T(String).valueOf(#projectId)"))
public void revokeConnectionFromProject(long workspaceId, long connectionId, long projectId) { ... }
```

**shareConnectionToProject:**
```java
@AuditConnection(event = CONNECTION_SHARED, connectionId = "#connectionId",
    data = @AuditData(key = "projectId", value = "T(String).valueOf(#projectId)"))
public void shareConnectionToProject(long workspaceId, long connectionId, long projectId) { ... }
```

### OrganizationConnectionFacadeImpl (1 call → 1 annotation)

**create:**
```java
@AuditConnection(event = CONNECTION_CREATED, connectionId = "#result",
    data = @AuditData(key = "visibility", value = "'ORGANIZATION'"))
public long create(ConnectionDTO connectionDTO) {
    long connectionId = connectionFacade.create(connectionDTO, PlatformType.AUTOMATION);
    connectionService.updateVisibility(connectionId, ConnectionVisibility.ORGANIZATION);
    return connectionId;
}
```

### ConnectionReassignmentFacadeImpl (2 calls → 1 annotation + 1 extracted method)

**reassignConnection** (direct annotation):
```java
@AuditConnection(event = CONNECTION_REASSIGNED, connectionId = "#connectionId",
    data = @AuditData(key = "newOwnerLogin", value = "#newOwnerLogin"))
public void reassignConnection(long connectionId, String newOwnerLogin) {
    reassignConnectionWithoutAudit(connectionId, newOwnerLogin);
}
```

**reassignAllConnections** (calls extracted method in loop):
```java
public void reassignAllConnections(String newOwnerLogin) {
    List<ConnectionReassignmentItem> unresolvedConnections = getUnresolvedConnections();
    for (ConnectionReassignmentItem item : unresolvedConnections) {
        reassignSingleConnection(item.connectionId(), newOwnerLogin);
    }
}

@AuditConnection(event = CONNECTION_REASSIGNED, connectionId = "#connectionId",
    data = @AuditData(key = "newOwnerLogin", value = "#newOwnerLogin"))
public void reassignSingleConnection(long connectionId, String newOwnerLogin) {
    reassignConnectionWithoutAudit(connectionId, newOwnerLogin);
}
```

Note: `reassignSingleConnection` must be a `public` method on a Spring proxy (not `private`) for the aspect to intercept it. Since `reassignAllConnections` calls it via `this.`, it won't go through the proxy. Fix: inject `self` reference or use `ApplicationContext.getBean()`, OR keep the manual `publish()` call inside `reassignAllConnections` and only annotate `reassignConnection`. The latter is simpler.

**Revised approach for reassignAllConnections:** Keep the manual `connectionAuditPublisher.publish()` call inside the loop. Only `reassignConnection` gets the annotation. This avoids the self-invocation proxy problem entirely.

## Interface Changes

**WorkspaceConnectionFacade interface:**
- `promoteToWorkspace(long workspaceId, long connectionId)` return type changes from `void` to `ConnectionVisibility`

**Callers of promoteToWorkspace:**
- `ConnectionGraphQlController.promoteConnectionToWorkspace()` — ignore the return value (was already `void` handler returning `boolean true`)

## Facades After Refactoring

After the annotation covers all their audit calls, these facades can remove the `ConnectionAuditPublisher` field:
- `WorkspaceConnectionFacadeImpl` — remove field, constructor param
- `OrganizationConnectionFacadeImpl` — remove field, constructor param

`ConnectionReassignmentFacadeImpl` keeps the field because `reassignAllConnections` still uses the manual call.

## File Summary

**New files:**
- `server/libs/platform/platform-connection/platform-connection-api/.../audit/AuditConnection.java`
- `server/libs/platform/platform-connection/platform-connection-service/.../audit/ConnectionAuditAspect.java`

**Modified files:**
- `server/libs/platform/platform-connection/platform-connection-api/.../facade/WorkspaceConnectionFacade.java` — change `promoteToWorkspace` return type
- `server/libs/automation/automation-configuration/automation-configuration-service/.../facade/WorkspaceConnectionFacadeImpl.java` — replace 5 publish calls with annotations, remove `ConnectionAuditPublisher` field
- `server/libs/automation/automation-configuration/automation-configuration-service/.../facade/OrganizationConnectionFacadeImpl.java` — replace 1 publish call with annotation, remove `ConnectionAuditPublisher` field
- `server/libs/automation/automation-configuration/automation-configuration-service/.../facade/ConnectionReassignmentFacadeImpl.java` — replace 1 of 2 publish calls with annotation on `reassignConnection`
- `server/libs/automation/automation-configuration/automation-configuration-graphql/.../ConnectionGraphQlController.java` or callers — adapt to `promoteToWorkspace` return type change
- Tests for affected facades — update mocks

**Not changed:**
- `ConnectionAuditPublisher` — stays as-is, used by aspect and the one remaining manual call
- `ConnectionAuditEvent` enum — no changes
