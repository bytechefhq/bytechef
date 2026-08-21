# Centralized IDOR / Resource-Authorization Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generalize the `ProjectWorkspacePermissionEvaluator` pattern into a per-domain `ResourceOwnershipResolver` SPI, then close IDOR on the secret-bearing domains (connections T18, API/signing keys T19).

**Architecture:** A new SPI (`ResourceOwnershipResolver`) maps a resource id to its owning coordinates (workspaceId and/or ownerUserId). `PermissionService` gains `hasResourceScope` / `isResourceOwner`, backed by a registry of resolvers; `ProjectWorkspacePermissionEvaluator` routes two new prefix tokens (`X:ResourceScope`, `X:ResourceOwner`) to them. Facade/controller entry points carry `@PreAuthorize` using those tokens.

**Tech Stack:** Java 25, Spring Boot 4, Spring Security method security, Spring Data JDBC, JUnit 5, Mockito, AssertJ.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-06-19-centralized-idor-authorization-design.md`.
- `hasResourceScope` CE: tenant-admin → `true`; else resolver yields `ownerUserId` → owner check; else fail-closed `false`. EE: tenant-admin → `true`; else resolve `workspaceId` → `hasWorkspaceScope`; else `false`.
- `isResourceOwner` CE: `true` (EE-only enforcement for now). EE: tenant-admin → `true`; else `ownerUserId` → `isCurrentUser`; else `false`.
- Annotate **automation entry points** (controllers / automation facades), NEVER the shared platform `ConnectionFacade` / `ApiKeyFacade` (would break embedded + workspace-admin paths).
- EE files require the ByteChef Enterprise license header + `@version ee` Javadoc tag (Spotless picks the header by `@version ee` content, not path).
- Java style: one blank line before control statements and after a variable modification used by the next statement; no `_`-prefixed private methods; descriptive variable names; no trailing blank line before class `}`.
- Persistence of `PermissionScope` is by **name** (VARCHAR); ordinals are pinned by `EnumOrdinalPinTest` purely as a wrong-position guard — append new constants at the end and extend the test.
- Before committing server changes: `./gradlew spotlessApply` then `./gradlew check`.
- Commit message convention (server): `gecko <description>` (no ticket number for this work), ending with the `Co-Authored-By: Claude Opus 4.8 (1M context)` trailer. Make fresh commits on `0_732` — never `--amend`.

---

## File Structure

**T17 — central layer**
- Create `automation-configuration-api/.../security/ResourceOwnershipResolver.java` — SPI + `ResourceOwner` record.
- Modify `automation-configuration-api/.../service/PermissionService.java` — add `hasResourceScope`, `isResourceOwner`.
- Modify CE `automation-configuration-service/.../service/PermissionServiceImpl.java` — implement both + resolver registry.
- Modify EE `automation-configuration-service/.../service/PermissionServiceImpl.java` — implement both + resolver registry.
- Modify `automation-configuration-service/.../security/ProjectWorkspacePermissionEvaluator.java` — prefix-token routing.

**T19 enum prep**
- Modify EE `automation-configuration-api/.../security/constant/PermissionScope.java` and `BuiltInRoleScopes.java`; extend `EnumOrdinalPinTest.java`.

**T18 connections**
- Create `automation-configuration-service/.../security/ConnectionOwnershipResolver.java`.
- Modify `automation-configuration-rest-impl/.../web/rest/ConnectionApiController.java`, `ConnectionTagApiController.java`.

**T19 keys**
- Create `automation-configuration-service/.../security/ApiKeyOwnershipResolver.java`.
- Modify `automation-configuration-api/.../facade/WorkspaceApiKeyFacade.java`.
- Modify platform `platform-security-graphql/.../web/graphql/ApiKeyGraphQlController.java`.
- Create EE `embedded-security-service/.../security/SigningKeyOwnershipResolver.java`; modify `SigningKeyServiceImpl.java`.
- Create EE `automation-api-platform-configuration-service/.../security/ApiClientOwnershipResolver.java`; modify `ApiClientServiceImpl.java`.

**Wrap-up**
- Modify `gecko-remediation-tasks.md`.

---

### Task 1: ResourceOwnershipResolver SPI

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/security/ResourceOwnershipResolver.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-api/src/test/java/com/bytechef/automation/configuration/security/ResourceOwnerTest.java`

**Interfaces:**
- Produces: `interface ResourceOwnershipResolver { String resourceType(); ResourceOwner resolveOwner(long id); }` and `record ResourceOwner(OptionalLong workspaceId, OptionalLong ownerUserId)` with `static ResourceOwner unknown()` and `static ResourceOwner ofWorkspace(long)` / `ofUser(long)` / `of(OptionalLong, OptionalLong)` factories.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class ResourceOwnerTest {

    @Test
    void testUnknownIsEmpty() {
        ResourceOwner owner = ResourceOwner.unknown();

        assertThat(owner.workspaceId()).isEmpty();
        assertThat(owner.ownerUserId()).isEmpty();
    }

    @Test
    void testOfWorkspaceSetsOnlyWorkspace() {
        ResourceOwner owner = ResourceOwner.ofWorkspace(42L);

        assertThat(owner.workspaceId()).hasValue(42L);
        assertThat(owner.ownerUserId()).isEmpty();
    }

    @Test
    void testOfUserSetsOnlyOwner() {
        ResourceOwner owner = ResourceOwner.ofUser(7L);

        assertThat(owner.ownerUserId()).hasValue(7L);
        assertThat(owner.workspaceId()).isEmpty();
    }

    @Test
    void testOfBothSetsBoth() {
        ResourceOwner owner = ResourceOwner.of(OptionalLong.of(42L), OptionalLong.of(7L));

        assertThat(owner.workspaceId()).hasValue(42L);
        assertThat(owner.ownerUserId()).hasValue(7L);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-api:test --tests ResourceOwnerTest`
Expected: FAIL — `ResourceOwnershipResolver` does not exist (compilation error).

- [ ] **Step 3: Write the SPI**

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

package com.bytechef.automation.configuration.security;

import java.util.OptionalLong;

/**
 * SPI contributed once per resource family to map a resource id to its owning coordinates. Consumed by
 * {@code PermissionService.hasResourceScope} / {@code isResourceOwner} via a registry keyed on {@link #resourceType()}.
 * Implementations MUST fail closed: an unknown / missing resource returns {@link ResourceOwner#unknown()} rather than
 * throwing.
 *
 * @author Ivica Cardic
 */
public interface ResourceOwnershipResolver {

    /**
     * Resource-type discriminator matching the {@code @PreAuthorize} token prefix, e.g. {@code "Connection"} for the
     * token {@code 'Connection:ResourceScope'}. Must be unique across all registered resolvers.
     */
    String resourceType();

    /**
     * Owning coordinates for the given resource id. Returns {@link ResourceOwner#unknown()} when the resource does not
     * exist or its owner cannot be determined.
     */
    ResourceOwner resolveOwner(long id);

    /**
     * Owning coordinates of a resource. A resolver may populate {@code workspaceId} (workspace-mapped resources),
     * {@code ownerUserId} (user-owned resources), or both.
     */
    record ResourceOwner(OptionalLong workspaceId, OptionalLong ownerUserId) {

        public static ResourceOwner unknown() {
            return new ResourceOwner(OptionalLong.empty(), OptionalLong.empty());
        }

        public static ResourceOwner ofWorkspace(long workspaceId) {
            return new ResourceOwner(OptionalLong.of(workspaceId), OptionalLong.empty());
        }

        public static ResourceOwner ofUser(long ownerUserId) {
            return new ResourceOwner(OptionalLong.empty(), OptionalLong.of(ownerUserId));
        }

        public static ResourceOwner of(OptionalLong workspaceId, OptionalLong ownerUserId) {
            return new ResourceOwner(workspaceId, ownerUserId);
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-api:test --tests ResourceOwnerTest`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/security/ResourceOwnershipResolver.java \
        server/libs/automation/automation-configuration/automation-configuration-api/src/test/java/com/bytechef/automation/configuration/security/ResourceOwnerTest.java
git commit -m "gecko Add ResourceOwnershipResolver SPI (T17)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 2: PermissionService interface + CE implementation

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/PermissionService.java` (add two methods after `hasWorkspaceScopeForProject`, line 89)
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/PermissionServiceImpl.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/service/PermissionServiceImplResourceTest.java`

**Interfaces:**
- Consumes: `ResourceOwnershipResolver` (Task 1).
- Produces: `boolean hasResourceScope(String resourceType, long id, String scope)` and `boolean isResourceOwner(String resourceType, long id)` on `PermissionService`. CE impl constructor gains `List<ResourceOwnershipResolver> resourceOwnershipResolvers`.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PermissionServiceImplResourceTest {

    private final UserService userService = Mockito.mock(UserService.class);

    private PermissionService permissionService(ResourceOwnershipResolver... resolvers) {
        return new PermissionServiceImpl(userService, List.of(resolvers));
    }

    private static ResourceOwnershipResolver resolver(String type, ResourceOwner owner) {
        return new ResourceOwnershipResolver() {
            @Override
            public String resourceType() {
                return type;
            }

            @Override
            public ResourceOwner resolveOwner(long id) {
                return owner;
            }
        };
    }

    @Test
    void testHasResourceScopeOwnerMatchAllowsInCe() {
        User user = new User();

        user.setId(7L);

        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user));

        PermissionService service = permissionService(resolver("Connection", ResourceOwner.ofUser(7L)));

        assertThat(service.hasResourceScope("Connection", 1L, "CONNECTION_DELETE")).isTrue();
    }

    @Test
    void testHasResourceScopeOwnerMismatchDeniesInCe() {
        User user = new User();

        user.setId(99L);

        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user));

        PermissionService service = permissionService(resolver("Connection", ResourceOwner.ofUser(7L)));

        assertThat(service.hasResourceScope("Connection", 1L, "CONNECTION_DELETE")).isFalse();
    }

    @Test
    void testHasResourceScopeNoOwnerFailsClosedInCe() {
        PermissionService service = permissionService(resolver("Connection", ResourceOwner.unknown()));

        assertThat(service.hasResourceScope("Connection", 1L, "CONNECTION_DELETE")).isFalse();
    }

    @Test
    void testHasResourceScopeUnregisteredTypeFailsClosed() {
        PermissionService service = permissionService();

        assertThat(service.hasResourceScope("Nope", 1L, "X")).isFalse();
    }

    @Test
    void testIsResourceOwnerPermissiveInCe() {
        PermissionService service = permissionService(resolver("ApiKey", ResourceOwner.ofUser(7L)));

        assertThat(service.isResourceOwner("ApiKey", 1L)).isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests PermissionServiceImplResourceTest`
Expected: FAIL — `hasResourceScope` / new constructor not defined (compilation error).

- [ ] **Step 3: Add the interface methods**

In `PermissionService.java`, insert after the `hasWorkspaceScopeForProject` declaration (after line 89):

```java
    /**
     * Resource-scoped authorization keyed on a {@link com.bytechef.automation.configuration.security.ResourceOwnershipResolver}
     * registered for {@code resourceType}. CE: tenant-admin passes; else owner-isolation when the resolver yields an
     * {@code ownerUserId}; else fail-closed {@code false}. EE: tenant-admin passes; else the scope is evaluated against
     * the resource's owning workspace; else {@code false}. Used by the {@code 'Type:ResourceScope'} SpEL token.
     */
    boolean hasResourceScope(String resourceType, long id, String scope);

    /**
     * Owner-isolation check keyed on a registered {@link com.bytechef.automation.configuration.security.ResourceOwnershipResolver}.
     * CE: permissive {@code true} (EE-only enforcement for now). EE: tenant-admin passes; else the resolver's
     * {@code ownerUserId} must match the current user. Used by the {@code 'Type:ResourceOwner'} SpEL token.
     */
    boolean isResourceOwner(String resourceType, long id);
```

- [ ] **Step 4: Implement in the CE `PermissionServiceImpl`**

Add the import and field, widen the constructor, build the registry, and implement both methods. Replace the existing constructor (lines 53-56) and add the methods before the closing brace:

```java
// add imports
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.stream.Collectors;

// fields
    private final UserService userService;
    private final Map<String, ResourceOwnershipResolver> resourceOwnershipResolvers;

    @SuppressFBWarnings("EI")
    public PermissionServiceImpl(
        UserService userService, List<ResourceOwnershipResolver> resourceOwnershipResolvers) {

        this.userService = userService;
        this.resourceOwnershipResolvers = resourceOwnershipResolvers.stream()
            .collect(Collectors.toMap(ResourceOwnershipResolver::resourceType, Function.identity()));
    }
```

```java
    @Override
    public boolean hasResourceScope(String resourceType, long id, String scope) {
        if (isTenantAdmin()) {
            return true;
        }

        ResourceOwnershipResolver resolver = resourceOwnershipResolvers.get(resourceType);

        if (resolver == null) {
            return false;
        }

        // CE has no workspace RBAC, so a workspace-mapped resource is authorized by owner-isolation: the current user
        // must be the resource's creator (honoring CE's PRIVATE/creator-only model). No owner resolvable -> deny.
        OptionalLong ownerUserId = resolver.resolveOwner(id)
            .ownerUserId();

        return ownerUserId.isPresent() && isCurrentUser(ownerUserId.getAsLong());
    }

    @Override
    public boolean isResourceOwner(String resourceType, long id) {
        // EE-only enforcement for now; CE stays permissive.
        return true;
    }
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests PermissionServiceImplResourceTest`
Expected: PASS (5 tests).

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/PermissionService.java \
        server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/PermissionServiceImpl.java \
        server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/service/PermissionServiceImplResourceTest.java
git commit -m "gecko Add hasResourceScope/isResourceOwner to PermissionService + CE impl (T17)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 3: EE PermissionServiceImpl implementation

**Files:**
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/service/PermissionServiceImpl.java`
- Test: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/service/PermissionServiceImplResourceTest.java`

**Interfaces:**
- Consumes: `ResourceOwnershipResolver` (Task 1), the interface methods (Task 2).
- Produces: EE `hasResourceScope` (workspace-scope path) + `isResourceOwner` (isCurrentUser path); constructor gains `List<ResourceOwnershipResolver>`.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

class PermissionServiceImplResourceTest {

    private final CurrentUserResolver currentUserResolver = Mockito.mock(CurrentUserResolver.class);
    private final ProjectRepository projectRepository = Mockito.mock(ProjectRepository.class);
    private final WorkspaceScopeCacheService workspaceScopeCacheService =
        Mockito.mock(WorkspaceScopeCacheService.class);
    private final WorkspaceUserRepository workspaceUserRepository = Mockito.mock(WorkspaceUserRepository.class);

    @SuppressWarnings("unchecked")
    private PermissionServiceImpl service(ResourceOwnershipResolver... resolvers) {
        ObjectProvider<io.micrometer.core.instrument.MeterRegistry> provider = Mockito.mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(null);

        return new PermissionServiceImpl(
            currentUserResolver, projectRepository, workspaceScopeCacheService, workspaceUserRepository, provider,
            List.of(resolvers));
    }

    private static ResourceOwnershipResolver resolver(String type, ResourceOwner owner) {
        return new ResourceOwnershipResolver() {
            @Override
            public String resourceType() {
                return type;
            }

            @Override
            public ResourceOwner resolveOwner(long id) {
                return owner;
            }
        };
    }

    @Test
    void testHasResourceScopeUsesWorkspaceScopeInEe() {
        lenient().when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));
        when(workspaceScopeCacheService.getWorkspaceScopes(7L, 42L))
            .thenReturn(java.util.Set.of(
                com.bytechef.ee.automation.configuration.security.constant.PermissionScope.CONNECTION_DELETE));

        PermissionServiceImpl service = service(resolver("Connection", ResourceOwner.ofWorkspace(42L)));

        assertThat(service.hasResourceScope("Connection", 1L, "CONNECTION_DELETE")).isTrue();
    }

    @Test
    void testHasResourceScopeNoWorkspaceFailsClosed() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));

        PermissionServiceImpl service = service(resolver("Connection", ResourceOwner.unknown()));

        assertThat(service.hasResourceScope("Connection", 1L, "CONNECTION_DELETE")).isFalse();
    }

    @Test
    void testIsResourceOwnerMatchAllowsInEe() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));

        PermissionServiceImpl service = service(resolver("ApiKey", ResourceOwner.ofUser(7L)));

        assertThat(service.isResourceOwner("ApiKey", 1L)).isTrue();
    }

    @Test
    void testIsResourceOwnerMismatchDeniesInEe() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(99L));

        PermissionServiceImpl service = service(resolver("ApiKey", ResourceOwner.ofUser(7L)));

        assertThat(service.isResourceOwner("ApiKey", 1L)).isFalse();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests PermissionServiceImplResourceTest`
Expected: FAIL — new constructor / methods not defined.

- [ ] **Step 3: Widen the EE constructor + add the registry field**

Add import `import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;`, `import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;`, `import java.util.function.Function;`, `import java.util.stream.Collectors;` (others already present). Add field after `checkErrorCounters`:

```java
    private final Map<String, ResourceOwnershipResolver> resourceOwnershipResolvers;
```

Append the registry build to the constructor (after the `checkErrorCounters` assignment), and add the `List<ResourceOwnershipResolver>` parameter:

```java
    public PermissionServiceImpl(
        CurrentUserResolver currentUserResolver, ProjectRepository projectRepository,
        WorkspaceScopeCacheService workspaceScopeCacheService, WorkspaceUserRepository workspaceUserRepository,
        ObjectProvider<MeterRegistry> meterRegistryProvider,
        List<ResourceOwnershipResolver> resourceOwnershipResolvers) {

        // ... existing assignments unchanged ...

        this.resourceOwnershipResolvers = resourceOwnershipResolvers.stream()
            .collect(Collectors.toMap(ResourceOwnershipResolver::resourceType, Function.identity()));
    }
```

- [ ] **Step 4: Implement both methods (EE)**

Add before the private helpers:

```java
    @Override
    public boolean hasResourceScope(String resourceType, long id, String scope) {
        if (isTenantAdmin()) {
            return true;
        }

        ResourceOwnershipResolver resolver = resourceOwnershipResolvers.get(resourceType);

        if (resolver == null) {
            return false;
        }

        OptionalLong workspaceId = withCheckErrorCounter("hasWorkspaceScope",
            () -> resolver.resolveOwner(id)
                .workspaceId());

        if (workspaceId.isEmpty()) {
            return false;
        }

        return hasWorkspaceScope(workspaceId.getAsLong(), scope);
    }

    @Override
    public boolean isResourceOwner(String resourceType, long id) {
        if (isTenantAdmin()) {
            return true;
        }

        ResourceOwnershipResolver resolver = resourceOwnershipResolvers.get(resourceType);

        if (resolver == null) {
            return false;
        }

        OptionalLong ownerUserId = withCheckErrorCounter("hasWorkspaceScope",
            () -> resolver.resolveOwner(id)
                .ownerUserId());

        return ownerUserId.isPresent() && isCurrentUser(ownerUserId.getAsLong());
    }
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests PermissionServiceImplResourceTest`
Expected: PASS (4 tests).

- [ ] **Step 6: Update any other EE PermissionServiceImpl constructor call sites**

Run: `grep -rn "new PermissionServiceImpl(" server/ee`
Expected: only the EE bean factory / tests. Fix any direct `new` call to pass the resolver list (tests use `List.of(...)`). Spring constructor injection needs no change — it auto-wires `List<ResourceOwnershipResolver>`.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/service/PermissionServiceImpl.java \
        server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/service/PermissionServiceImplResourceTest.java
git commit -m "gecko Implement hasResourceScope/isResourceOwner in EE PermissionService (T17)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 4: Evaluator prefix-token routing

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/security/ProjectWorkspacePermissionEvaluator.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/security/ProjectWorkspacePermissionEvaluatorTest.java` (add tests)

**Interfaces:**
- Consumes: `PermissionService.hasResourceScope` / `isResourceOwner` (Tasks 2-3).
- Produces: tokens `"<Type>:ResourceScope"` → `hasResourceScope(type, id, permission)`, `"<Type>:ResourceOwner"` → `isResourceOwner(type, id)`.

- [ ] **Step 1: Write the failing tests** (append to `ProjectWorkspacePermissionEvaluatorTest`)

```java
    @Test
    void testResourceScopeTokenDelegatesToHasResourceScope() {
        when(permissionService.hasResourceScope("Connection", 5L, "CONNECTION_DELETE")).thenReturn(true);

        assertThat(evaluator.hasPermission(null, 5L, "Connection:ResourceScope", "CONNECTION_DELETE")).isTrue();
    }

    @Test
    void testResourceOwnerTokenDelegatesToIsResourceOwner() {
        when(permissionService.isResourceOwner("ApiKey", 9L)).thenReturn(true);

        assertThat(evaluator.hasPermission(null, 9L, "ApiKey:ResourceOwner", "SELF")).isTrue();
    }

    @Test
    void testUnknownPrefixKindReturnsFalse() {
        assertThat(evaluator.hasPermission(null, 9L, "ApiKey:Bogus", "SELF")).isFalse();

        verifyNoInteractions(permissionService);
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests ProjectWorkspacePermissionEvaluatorTest`
Expected: FAIL — `Connection:ResourceScope` hits `default -> false`.

- [ ] **Step 3: Add constants + prefix parsing**

Add constants near the existing ones (line 48-54):

```java
    static final String RESOURCE_OWNER = "ResourceOwner";
    static final String RESOURCE_SCOPE = "ResourceScope";
```

In the 4-arg `hasPermission` (after computing `id` and `value`, before the existing `switch`), insert:

```java
        int separatorIndex = targetType.indexOf(':');

        if (separatorIndex > 0) {
            String resourceType = targetType.substring(0, separatorIndex);
            String kind = targetType.substring(separatorIndex + 1);

            return switch (kind) {
                case RESOURCE_SCOPE -> permissionService.hasResourceScope(resourceType, id, value);
                case RESOURCE_OWNER -> permissionService.isResourceOwner(resourceType, id);
                default -> false;
            };
        }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests ProjectWorkspacePermissionEvaluatorTest`
Expected: PASS (existing + 3 new).

- [ ] **Step 5: Add a wiring smoke test** (append to `PermissionEvaluatorWiringIntTest.GuardedService` + a test)

Add to the inner `GuardedService`:

```java
        @PreAuthorize("hasPermission(#id, 'Connection:ResourceScope', 'CONNECTION_DELETE')")
        public void deleteConnection(long id) {
        }
```

Add test:

```java
    @Test
    void testResourceScopeTokenIsWired() {
        when(permissionService.hasResourceScope("Connection", 3L, "CONNECTION_DELETE")).thenReturn(false);

        assertThatThrownBy(() -> guardedService.deleteConnection(3L)).isInstanceOf(AccessDeniedException.class);
    }
```

- [ ] **Step 6: Run the wiring test**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests PermissionEvaluatorWiringIntTest`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/security/ProjectWorkspacePermissionEvaluator.java \
        server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/security/ProjectWorkspacePermissionEvaluatorTest.java \
        server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/security/PermissionEvaluatorWiringIntTest.java
git commit -m "gecko Route ResourceScope/ResourceOwner tokens in evaluator (T17)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 5: Add API_KEY_* permission scopes

**Files:**
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration/security/constant/PermissionScope.java`
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration/security/constant/BuiltInRoleScopes.java`
- Test: `server/ee/libs/automation/automation-configuration/automation-configuration-api/src/test/java/com/bytechef/ee/automation/configuration/security/constant/EnumOrdinalPinTest.java`

**Interfaces:**
- Produces: `PermissionScope.API_KEY_VIEW/API_KEY_CREATE/API_KEY_EDIT/API_KEY_DELETE` (ordinals 21-24).

- [ ] **Step 1: Update the ordinal-pin test first (it will fail)**

In `EnumOrdinalPinTest.testPermissionScopeOrdinalsPinned()`, after the `PROJECT_DELETE` assertion (ordinal 20) and before the size assertion, add:

```java
        assertThat(PermissionScope.API_KEY_VIEW.ordinal()).isEqualTo(21);
        assertThat(PermissionScope.API_KEY_CREATE.ordinal()).isEqualTo(22);
        assertThat(PermissionScope.API_KEY_EDIT.ordinal()).isEqualTo(23);
        assertThat(PermissionScope.API_KEY_DELETE.ordinal()).isEqualTo(24);
```

Change the size assertion from `hasSize(21)` to `hasSize(25)`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-api:test --tests EnumOrdinalPinTest`
Expected: FAIL — `API_KEY_VIEW` does not exist.

- [ ] **Step 3: Append constants to `PermissionScope`**

After `PROJECT_DELETE` (line ~60), add:

```java
    PROJECT_DELETE,

    // API keys
    API_KEY_VIEW,
    API_KEY_CREATE,
    API_KEY_EDIT,
    API_KEY_DELETE
```

(Remove the trailing comma issue: `PROJECT_DELETE` previously had no comma — add one before the new block.)

- [ ] **Step 4: Map the new scopes to roles in `BuiltInRoleScopes`**

In the static initializer add `API_KEY_VIEW` to the `viewer` set, and `API_KEY_CREATE, API_KEY_EDIT, API_KEY_DELETE` to the `editor.addAll(...)` block:

```java
        EnumSet<PermissionScope> viewer = EnumSet.of(
            WORKFLOW_VIEW,
            EXECUTION_VIEW,
            CONNECTION_VIEW,
            AGENT_VIEW,
            API_KEY_VIEW);
```

```java
        editor.addAll(EnumSet.of(
            WORKFLOW_TOGGLE,
            EXECUTION_DATA, EXECUTION_RETRY,
            CONNECTION_USE,
            AGENT_EXECUTE,
            WORKFLOW_CREATE, WORKFLOW_EDIT, WORKFLOW_DELETE,
            CONNECTION_CREATE, CONNECTION_EDIT, CONNECTION_DELETE,
            AGENT_CREATE, AGENT_EDIT,
            DEPLOYMENT_PUSH, DEPLOYMENT_PULL,
            API_KEY_CREATE, API_KEY_EDIT, API_KEY_DELETE));
```

(ADMIN already gets all scopes via `EnumSet.allOf`.)

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-api:test --tests EnumOrdinalPinTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration/security/constant/PermissionScope.java \
        server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration/security/constant/BuiltInRoleScopes.java \
        server/ee/libs/automation/automation-configuration/automation-configuration-api/src/test/java/com/bytechef/ee/automation/configuration/security/constant/EnumOrdinalPinTest.java
git commit -m "gecko Add API_KEY_* permission scopes (T19)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 6: ConnectionOwnershipResolver

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/security/ConnectionOwnershipResolver.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/security/ConnectionOwnershipResolverTest.java`

**Interfaces:**
- Consumes: `ResourceOwnershipResolver` (Task 1); `WorkspaceConnectionRepository.findByConnectionId(long) : Optional<WorkspaceConnection>`; `WorkspaceConnection.getWorkspaceId() : Long`; `ConnectionService.fetchConnection`/`getConnection`; `Connection.getCreatedBy() : String`; `UserService.fetchUserByLogin(String) : Optional<User>`; `User.getId() : Long`.
- Produces: bean `ConnectionOwnershipResolver`, `resourceType() == "Connection"`.

- [ ] **Step 1: Confirm the ConnectionService read method**

Run: `grep -n "fetchConnection\|getConnection" server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/ConnectionService.java`
Expected: a `getConnection(long)` and/or `fetchConnection(long)`. Use `fetchConnection(long) : Optional<Connection>` if present; otherwise wrap `getConnection(long)` in a try/catch-free `Optional.ofNullable`. (The test below assumes `fetchConnection(long id) : Optional<Connection>`; adjust if the real name differs.)

- [ ] **Step 2: Write the failing test**

```java
package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ConnectionOwnershipResolverTest {

    private final WorkspaceConnectionRepository workspaceConnectionRepository =
        Mockito.mock(WorkspaceConnectionRepository.class);
    private final ConnectionService connectionService = Mockito.mock(ConnectionService.class);
    private final UserService userService = Mockito.mock(UserService.class);

    private final ConnectionOwnershipResolver resolver =
        new ConnectionOwnershipResolver(workspaceConnectionRepository, connectionService, userService);

    @Test
    void testResourceType() {
        assertThat(resolver.resourceType()).isEqualTo("Connection");
    }

    @Test
    void testResolvesWorkspaceAndOwner() {
        WorkspaceConnection workspaceConnection = new WorkspaceConnection();

        workspaceConnection.setWorkspaceId(42L);

        when(workspaceConnectionRepository.findByConnectionId(1L)).thenReturn(Optional.of(workspaceConnection));

        Connection connection = new Connection();

        connection.setCreatedBy("alice");

        when(connectionService.fetchConnection(1L)).thenReturn(Optional.of(connection));

        User user = new User();

        user.setId(7L);

        when(userService.fetchUserByLogin("alice")).thenReturn(Optional.of(user));

        assertThat(resolver.resolveOwner(1L).workspaceId()).hasValue(42L);
        assertThat(resolver.resolveOwner(1L).ownerUserId()).hasValue(7L);
    }

    @Test
    void testUnknownConnectionIsUnknown() {
        when(workspaceConnectionRepository.findByConnectionId(99L)).thenReturn(Optional.empty());
        when(connectionService.fetchConnection(99L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(99L).workspaceId()).isEmpty();
        assertThat(resolver.resolveOwner(99L).ownerUserId()).isEmpty();
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests ConnectionOwnershipResolverTest`
Expected: FAIL — class does not exist.

- [ ] **Step 4: Implement the resolver**

```java
/*
 * Copyright 2025 ByteChef
 * Licensed under the Apache License, Version 2.0 ... (full Apache header)
 */

package com.bytechef.automation.configuration.security;

import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.OptionalLong;
import org.springframework.stereotype.Component;

/**
 * Maps a connection id to its owning workspace (via the {@code workspace_connection} relation) and creator (via
 * {@code created_by} resolved to a user id). EE authorizes by workspace scope; CE authorizes by owner-isolation
 * (creator), honoring CE's PRIVATE/creator-only connection model.
 *
 * @author Ivica Cardic
 */
@Component
public class ConnectionOwnershipResolver implements ResourceOwnershipResolver {

    private final WorkspaceConnectionRepository workspaceConnectionRepository;
    private final ConnectionService connectionService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public ConnectionOwnershipResolver(
        WorkspaceConnectionRepository workspaceConnectionRepository, ConnectionService connectionService,
        UserService userService) {

        this.workspaceConnectionRepository = workspaceConnectionRepository;
        this.connectionService = connectionService;
        this.userService = userService;
    }

    @Override
    public String resourceType() {
        return "Connection";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        OptionalLong workspaceId = workspaceConnectionRepository.findByConnectionId(id)
            .map(workspaceConnection -> OptionalLong.of(workspaceConnection.getWorkspaceId()))
            .orElse(OptionalLong.empty());

        OptionalLong ownerUserId = connectionService.fetchConnection(id)
            .map(connection -> connection.getCreatedBy())
            .flatMap(userService::fetchUserByLogin)
            .map(user -> OptionalLong.of(user.getId()))
            .orElse(OptionalLong.empty());

        return ResourceOwner.of(workspaceId, ownerUserId);
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests ConnectionOwnershipResolverTest`
Expected: PASS (3 tests).

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/security/ConnectionOwnershipResolver.java \
        server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/security/ConnectionOwnershipResolverTest.java
git commit -m "gecko Add ConnectionOwnershipResolver (T18)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 7: Gate connection endpoints

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-rest/automation-configuration-rest-impl/src/main/java/com/bytechef/automation/configuration/web/rest/ConnectionApiController.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-rest/automation-configuration-rest-impl/src/main/java/com/bytechef/automation/configuration/web/rest/ConnectionTagApiController.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-rest/automation-configuration-rest-impl/src/test/java/com/bytechef/automation/configuration/web/rest/ConnectionApiControllerAuthorizationTest.java`

**Interfaces:**
- Consumes: token `'Connection:ResourceScope'` (Tasks 4, 6); existing `'WorkspaceScope'` token.
- Produces: `@PreAuthorize` on `getConnection`, `deleteConnection`, `updateConnection`, `getWorkspaceConnections`, `updateConnectionTags`.

- [ ] **Step 1: Write the failing reflection test**

```java
package com.bytechef.automation.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class ConnectionApiControllerAuthorizationTest {

    @Test
    void testGetConnectionRequiresConnectionViewScope() {
        assertExpression(
            ConnectionApiController.class, "getConnection",
            "hasPermission(#id, 'Connection:ResourceScope', 'CONNECTION_VIEW')");
    }

    @Test
    void testDeleteConnectionRequiresConnectionDeleteScope() {
        assertExpression(
            ConnectionApiController.class, "deleteConnection",
            "hasPermission(#id, 'Connection:ResourceScope', 'CONNECTION_DELETE')");
    }

    @Test
    void testUpdateConnectionRequiresConnectionEditScope() {
        assertExpression(
            ConnectionApiController.class, "updateConnection",
            "hasPermission(#id, 'Connection:ResourceScope', 'CONNECTION_EDIT')");
    }

    @Test
    void testGetWorkspaceConnectionsRequiresConnectionViewScope() {
        assertExpression(
            ConnectionApiController.class, "getWorkspaceConnections",
            "hasPermission(#id, 'WorkspaceScope', 'CONNECTION_VIEW')");
    }

    private static void assertExpression(Class<?> clazz, String methodName, String expression) {
        Method method = null;

        for (Method candidate : clazz.getDeclaredMethods()) {
            if (candidate.getName()
                .equals(methodName)) {
                method = candidate;

                break;
            }
        }

        assertThat(method)
            .as("method %s", methodName)
            .isNotNull();
        assertThat(method.getAnnotation(PreAuthorize.class))
            .as("@PreAuthorize on %s", methodName)
            .isNotNull();
        assertThat(method.getAnnotation(PreAuthorize.class)
            .value()).isEqualTo(expression);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-rest:automation-configuration-rest-impl:test --tests ConnectionApiControllerAuthorizationTest`
Expected: FAIL — no `@PreAuthorize`.

- [ ] **Step 3: Annotate the controller methods**

Add `import org.springframework.security.access.prepost.PreAuthorize;` and annotate (the `getWorkspaceConnections` path param is named `id` and is the **workspaceId**, so it uses the WorkspaceScope token):

```java
    @PreAuthorize("hasPermission(#id, 'Connection:ResourceScope', 'CONNECTION_VIEW')")
    public ResponseEntity<ConnectionModel> getConnection(Long id) { ... }

    @PreAuthorize("hasPermission(#id, 'Connection:ResourceScope', 'CONNECTION_DELETE')")
    public ResponseEntity<Void> deleteConnection(Long id) { ... }

    @PreAuthorize("hasPermission(#id, 'Connection:ResourceScope', 'CONNECTION_EDIT')")
    public ResponseEntity<Void> updateConnection(Long id, UpdateConnectionRequestModel updateConnectionRequestModel) { ... }

    @PreAuthorize("hasPermission(#id, 'WorkspaceScope', 'CONNECTION_VIEW')")
    public ResponseEntity<List<ConnectionModel>> getWorkspaceConnections(Long id, String componentName, Integer connectionVersion, Long environmentId, Long tagId) { ... }
```

In `ConnectionTagApiController`, annotate `updateConnectionTags` (path id is a connection id):

```java
    @PreAuthorize("hasPermission(#id, 'Connection:ResourceScope', 'CONNECTION_EDIT')")
    public ResponseEntity<Void> updateConnectionTags(Long id, UpdateTagsRequestModel updateTagsRequestModel) { ... }
```

Add a matching test method `testUpdateConnectionTagsRequiresConnectionEditScope` in a `ConnectionTagApiControllerAuthorizationTest` (same helper) asserting `"hasPermission(#id, 'Connection:ResourceScope', 'CONNECTION_EDIT')"`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-rest:automation-configuration-rest-impl:test --tests "Connection*AuthorizationTest"`
Expected: PASS.

- [ ] **Step 5: Note the deferred list-filtering items**

`ConnectionSearchAssetProvider.search` and any `findAll`-style listing return cross-owner rows; these need **per-row filtering**, not a single gate, and are tracked as a follow-up bullet under T18 in `gecko-remediation-tasks.md` (added in Task 13), not closed here. Likewise `ClusterElementDefinitionFacadeImpl` connection loads run inside already-gated editor flows; leave unannotated and note in T13.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-configuration/automation-configuration-rest/automation-configuration-rest-impl/src/main/java/com/bytechef/automation/configuration/web/rest/ConnectionApiController.java \
        server/libs/automation/automation-configuration/automation-configuration-rest/automation-configuration-rest-impl/src/main/java/com/bytechef/automation/configuration/web/rest/ConnectionTagApiController.java \
        server/libs/automation/automation-configuration/automation-configuration-rest/automation-configuration-rest-impl/src/test/java/com/bytechef/automation/configuration/web/rest/ConnectionApiControllerAuthorizationTest.java \
        server/libs/automation/automation-configuration/automation-configuration-rest/automation-configuration-rest-impl/src/test/java/com/bytechef/automation/configuration/web/rest/ConnectionTagApiControllerAuthorizationTest.java
git commit -m "gecko client - n/a; gecko Gate connection endpoints with Connection:ResourceScope (T18)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 8: ApiKeyOwnershipResolver

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/security/ApiKeyOwnershipResolver.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/security/ApiKeyOwnershipResolverTest.java`

**Interfaces:**
- Consumes: `WorkspaceApiKeyRepository.findByApiKeyId(long) : Optional<WorkspaceApiKey>`; `WorkspaceApiKey.getWorkspaceId() : Long`; `ApiKeyService` read method; `ApiKey.getUserId() : Long`.
- Produces: bean `ApiKeyOwnershipResolver`, `resourceType() == "ApiKey"`.

- [ ] **Step 1: Confirm the ApiKey read method (platform-security service)**

Run: `grep -n "fetchApiKey\|getApiKey" server/libs/platform/platform-security/platform-security-api/src/main/java/com/bytechef/platform/security/service/ApiKeyService.java`
Expected: a `fetchApiKey(long) : Optional<ApiKey>` or `getApiKey(long) : ApiKey`. The test assumes `fetchApiKey(long) : Optional<ApiKey>`; adjust to the real name.

- [ ] **Step 2: Write the failing test**

```java
package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.WorkspaceApiKey;
import com.bytechef.automation.configuration.repository.WorkspaceApiKeyRepository;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ApiKeyOwnershipResolverTest {

    private final WorkspaceApiKeyRepository workspaceApiKeyRepository = Mockito.mock(WorkspaceApiKeyRepository.class);
    private final ApiKeyService apiKeyService = Mockito.mock(ApiKeyService.class);

    private final ApiKeyOwnershipResolver resolver =
        new ApiKeyOwnershipResolver(workspaceApiKeyRepository, apiKeyService);

    @Test
    void testResourceType() {
        assertThat(resolver.resourceType()).isEqualTo("ApiKey");
    }

    @Test
    void testResolvesWorkspaceAndOwner() {
        WorkspaceApiKey workspaceApiKey = new WorkspaceApiKey();

        workspaceApiKey.setWorkspaceId(42L);

        when(workspaceApiKeyRepository.findByApiKeyId(1L)).thenReturn(Optional.of(workspaceApiKey));

        ApiKey apiKey = new ApiKey();

        apiKey.setUserId(7L);

        when(apiKeyService.fetchApiKey(1L)).thenReturn(Optional.of(apiKey));

        assertThat(resolver.resolveOwner(1L).workspaceId()).hasValue(42L);
        assertThat(resolver.resolveOwner(1L).ownerUserId()).hasValue(7L);
    }

    @Test
    void testUnknownApiKeyIsUnknown() {
        when(workspaceApiKeyRepository.findByApiKeyId(99L)).thenReturn(Optional.empty());
        when(apiKeyService.fetchApiKey(99L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(99L).workspaceId()).isEmpty();
        assertThat(resolver.resolveOwner(99L).ownerUserId()).isEmpty();
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests ApiKeyOwnershipResolverTest`
Expected: FAIL — class does not exist.

- [ ] **Step 4: Implement the resolver**

```java
package com.bytechef.automation.configuration.security;

// Apache header + imports for WorkspaceApiKeyRepository, ApiKeyService, OptionalLong, @Component, SuppressFBWarnings

@Component
public class ApiKeyOwnershipResolver implements ResourceOwnershipResolver {

    private final WorkspaceApiKeyRepository workspaceApiKeyRepository;
    private final ApiKeyService apiKeyService;

    @SuppressFBWarnings("EI")
    public ApiKeyOwnershipResolver(
        WorkspaceApiKeyRepository workspaceApiKeyRepository, ApiKeyService apiKeyService) {

        this.workspaceApiKeyRepository = workspaceApiKeyRepository;
        this.apiKeyService = apiKeyService;
    }

    @Override
    public String resourceType() {
        return "ApiKey";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        OptionalLong workspaceId = workspaceApiKeyRepository.findByApiKeyId(id)
            .map(workspaceApiKey -> OptionalLong.of(workspaceApiKey.getWorkspaceId()))
            .orElse(OptionalLong.empty());

        OptionalLong ownerUserId = apiKeyService.fetchApiKey(id)
            .map(apiKey -> apiKey.getUserId())
            .map(OptionalLong::of)
            .orElse(OptionalLong.empty());

        return ResourceOwner.of(workspaceId, ownerUserId);
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests ApiKeyOwnershipResolverTest`
Expected: PASS (3 tests).

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/security/ApiKeyOwnershipResolver.java \
        server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/security/ApiKeyOwnershipResolverTest.java
git commit -m "gecko Add ApiKeyOwnershipResolver (T19)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 9: Gate WorkspaceApiKeyFacade

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/facade/WorkspaceApiKeyFacade.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/WorkspaceApiKeyFacadeAuthorizationTest.java`

**Interfaces:**
- Consumes: token `'ApiKey:ResourceScope'`, `'WorkspaceScope'`.
- Produces: `@PreAuthorize` on `create`, `delete`, `getApiKeys`.

> Annotate the **interface** method (the impl inherits the annotation; consistent with the project's facade-auth pattern). `create(workspaceId, ...)` and `getApiKeys(workspaceId, ...)` use the WorkspaceScope token (the arg is a workspaceId); `delete(apiKeyId)` uses the ApiKey:ResourceScope token (the arg is an apiKeyId).

- [ ] **Step 1: Write the failing reflection test** (use the same `assertExpression` helper as Task 7, on the interface)

```java
    @Test
    void testCreateRequiresApiKeyCreateScope() {
        assertExpression(WorkspaceApiKeyFacade.class, "create",
            "hasPermission(#workspaceId, 'WorkspaceScope', 'API_KEY_CREATE')");
    }

    @Test
    void testDeleteRequiresApiKeyDeleteScope() {
        assertExpression(WorkspaceApiKeyFacade.class, "delete",
            "hasPermission(#apiKeyId, 'ApiKey:ResourceScope', 'API_KEY_DELETE')");
    }

    @Test
    void testGetApiKeysRequiresApiKeyViewScope() {
        assertExpression(WorkspaceApiKeyFacade.class, "getApiKeys",
            "hasPermission(#workspaceId, 'WorkspaceScope', 'API_KEY_VIEW')");
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests WorkspaceApiKeyFacadeAuthorizationTest`
Expected: FAIL.

- [ ] **Step 3: Annotate the interface methods**

```java
    @PreAuthorize("hasPermission(#workspaceId, 'WorkspaceScope', 'API_KEY_CREATE')")
    String create(long workspaceId, ApiKey apiKey);

    @PreAuthorize("hasPermission(#apiKeyId, 'ApiKey:ResourceScope', 'API_KEY_DELETE')")
    void delete(long apiKeyId);

    @PreAuthorize("hasPermission(#workspaceId, 'WorkspaceScope', 'API_KEY_VIEW')")
    List<ApiKey> getApiKeys(long workspaceId, long environmentId);
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests WorkspaceApiKeyFacadeAuthorizationTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/facade/WorkspaceApiKeyFacade.java \
        server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/WorkspaceApiKeyFacadeAuthorizationTest.java
git commit -m "gecko Gate WorkspaceApiKeyFacade with API_KEY scopes (T19)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 10: Gate personal ApiKeyGraphQlController

**Files:**
- Modify: `server/libs/platform/platform-security/platform-security-graphql/src/main/java/com/bytechef/platform/security/web/graphql/ApiKeyGraphQlController.java`
- Test: `server/libs/platform/platform-security/platform-security-graphql/src/test/java/com/bytechef/platform/security/web/graphql/ApiKeyGraphQlControllerAuthorizationTest.java`

**Interfaces:**
- Consumes: token `'ApiKey:ResourceOwner'`; `'Tenant'`/`'ADMIN'`.
- Produces: `@PreAuthorize` on `apiKey`, `updateApiKey`, `deleteApiKey`, `adminApiKeys`.

> These are the **personal** key operations. Owner-isolation (`ResourceOwner`, EE-only) for per-id ops; `adminApiKeys` is tenant-admin only. `apiKeys`/`createApiKey` operate on the current user implicitly (the facade sets `userId` from the SecurityContext) — leave them on `isAuthenticated()` (add it explicitly for clarity).

- [ ] **Step 1: Write the failing reflection test** (same `assertExpression` helper)

```java
    @Test
    void testApiKeyRequiresOwner() {
        assertExpression(ApiKeyGraphQlController.class, "apiKey",
            "hasPermission(#id, 'ApiKey:ResourceOwner', 'SELF')");
    }

    @Test
    void testUpdateApiKeyRequiresOwner() {
        assertExpression(ApiKeyGraphQlController.class, "updateApiKey",
            "hasPermission(#id, 'ApiKey:ResourceOwner', 'SELF')");
    }

    @Test
    void testDeleteApiKeyRequiresOwner() {
        assertExpression(ApiKeyGraphQlController.class, "deleteApiKey",
            "hasPermission(#id, 'ApiKey:ResourceOwner', 'SELF')");
    }

    @Test
    void testAdminApiKeysRequiresTenantAdmin() {
        assertExpression(ApiKeyGraphQlController.class, "adminApiKeys",
            "hasPermission('Tenant', 'ADMIN')");
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-security:platform-security-graphql:test --tests ApiKeyGraphQlControllerAuthorizationTest`
Expected: FAIL.

- [ ] **Step 3: Annotate the controller methods**

```java
    @PreAuthorize("hasPermission(#id, 'ApiKey:ResourceOwner', 'SELF')")
    public ApiKey apiKey(@Argument long id) { ... }

    @PreAuthorize("hasPermission(#id, 'ApiKey:ResourceOwner', 'SELF')")
    public Boolean updateApiKey(@Argument long id, @Argument String name) { ... }

    @PreAuthorize("hasPermission(#id, 'ApiKey:ResourceOwner', 'SELF')")
    public Boolean deleteApiKey(@Argument long id) { ... }

    @PreAuthorize("hasPermission('Tenant', 'ADMIN')")
    public List<ApiKey> adminApiKeys(@Argument Long environmentId) { ... }
```

Add `@PreAuthorize("isAuthenticated()")` to `apiKeys` and `createApiKey`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:platform:platform-security:platform-security-graphql:test --tests ApiKeyGraphQlControllerAuthorizationTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-security/platform-security-graphql/src/main/java/com/bytechef/platform/security/web/graphql/ApiKeyGraphQlController.java \
        server/libs/platform/platform-security/platform-security-graphql/src/test/java/com/bytechef/platform/security/web/graphql/ApiKeyGraphQlControllerAuthorizationTest.java
git commit -m "gecko Gate personal ApiKey GraphQL ops with owner-isolation (T19)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 11: SigningKey owner-isolation (EE)

**Files:**
- Create: `server/ee/libs/embedded/embedded-security/embedded-security-service/src/main/java/com/bytechef/ee/embedded/security/security/SigningKeyOwnershipResolver.java`
- Modify: `server/ee/libs/embedded/embedded-security/embedded-security-api/src/main/java/com/bytechef/ee/embedded/security/service/SigningKeyService.java`
- Test (resolver): `.../embedded-security-service/src/test/java/com/bytechef/ee/embedded/security/security/SigningKeyOwnershipResolverTest.java`
- Test (annotations): `.../embedded-security-service/src/test/java/com/bytechef/ee/embedded/security/service/SigningKeyServiceAuthorizationTest.java`

**Interfaces:**
- Consumes: `SigningKeyService.fetchSigningKey`/`getSigningKey`; `SigningKey.getUserId() : Long`.
- Produces: bean `SigningKeyOwnershipResolver`, `resourceType() == "SigningKey"`; `@PreAuthorize("hasPermission(#id, 'SigningKey:ResourceOwner', 'SELF')")` on `getSigningKey`, `update`, `delete`.

> All files here are EE — use the Enterprise license header and `@version ee` Javadoc tag.

- [ ] **Step 1: Confirm the read method**

Run: `grep -n "fetchSigningKey\|getSigningKey" server/ee/libs/embedded/embedded-security/embedded-security-api/src/main/java/com/bytechef/ee/embedded/security/service/SigningKeyService.java`
Expected: `getSigningKey(long) : SigningKey` (and possibly a fetch variant). If only `getSigningKey` exists, the resolver wraps it: `try { return OptionalLong.of(signingKeyService.getSigningKey(id).getUserId()); } catch (RuntimeException e) { return OptionalLong.empty(); }`. Prefer adding `Optional<SigningKey> fetchSigningKey(long)` to the service if a not-found returns null/throws — decide based on what `getSigningKey` does on miss.

- [ ] **Step 2: Write the resolver failing test**

```java
package com.bytechef.ee.embedded.security.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.security.domain.SigningKey;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SigningKeyOwnershipResolverTest {

    private final SigningKeyService signingKeyService = Mockito.mock(SigningKeyService.class);
    private final SigningKeyOwnershipResolver resolver = new SigningKeyOwnershipResolver(signingKeyService);

    @Test
    void testResourceType() {
        assertThat(resolver.resourceType()).isEqualTo("SigningKey");
    }

    @Test
    void testResolvesOwner() {
        SigningKey signingKey = new SigningKey();

        signingKey.setUserId(7L);

        when(signingKeyService.fetchSigningKey(1L)).thenReturn(java.util.Optional.of(signingKey));

        assertThat(resolver.resolveOwner(1L).ownerUserId()).hasValue(7L);
        assertThat(resolver.resolveOwner(1L).workspaceId()).isEmpty();
    }

    @Test
    void testUnknownIsUnknown() {
        when(signingKeyService.fetchSigningKey(99L)).thenReturn(java.util.Optional.empty());

        assertThat(resolver.resolveOwner(99L).ownerUserId()).isEmpty();
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:embedded:embedded-security:embedded-security-service:test --tests SigningKeyOwnershipResolverTest`
Expected: FAIL.

- [ ] **Step 4: Implement the resolver** (EE header + `@version ee`)

```java
package com.bytechef.ee.embedded.security.security;

// EE license header; imports: SigningKeyService, ResourceOwnershipResolver, OptionalLong, @Component, SuppressFBWarnings

/**
 * ...
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component
public class SigningKeyOwnershipResolver implements ResourceOwnershipResolver {

    private final SigningKeyService signingKeyService;

    @SuppressFBWarnings("EI")
    public SigningKeyOwnershipResolver(SigningKeyService signingKeyService) {
        this.signingKeyService = signingKeyService;
    }

    @Override
    public String resourceType() {
        return "SigningKey";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return signingKeyService.fetchSigningKey(id)
            .map(signingKey -> ResourceOwner.ofUser(signingKey.getUserId()))
            .orElseGet(ResourceOwner::unknown);
    }
}
```

If `fetchSigningKey` does not exist, add it to `SigningKeyService` + impl (`Optional<SigningKey> fetchSigningKey(long id)` delegating to `signingKeyRepository.findById(id)`).

- [ ] **Step 5: Write the annotation failing test**

```java
package com.bytechef.ee.embedded.security.service;

// assertExpression helper (copy the Task 7 helper)

    @Test
    void testGetSigningKeyRequiresOwner() {
        assertExpression(SigningKeyService.class, "getSigningKey",
            "hasPermission(#id, 'SigningKey:ResourceOwner', 'SELF')");
    }

    @Test
    void testUpdateRequiresOwner() {
        // update takes a SigningKey; gate on its id
        assertExpression(SigningKeyService.class, "update",
            "hasPermission(#signingKey.id, 'SigningKey:ResourceOwner', 'SELF')");
    }

    @Test
    void testDeleteRequiresOwner() {
        assertExpression(SigningKeyService.class, "delete",
            "hasPermission(#id, 'SigningKey:ResourceOwner', 'SELF')");
    }
```

- [ ] **Step 6: Annotate `SigningKeyService` interface methods**

```java
    @PreAuthorize("hasPermission(#id, 'SigningKey:ResourceOwner', 'SELF')")
    SigningKey getSigningKey(long id);

    @PreAuthorize("hasPermission(#signingKey.id, 'SigningKey:ResourceOwner', 'SELF')")
    SigningKey update(SigningKey signingKey);

    @PreAuthorize("hasPermission(#id, 'SigningKey:ResourceOwner', 'SELF')")
    void delete(long id);
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `./gradlew :server:ee:libs:embedded:embedded-security:embedded-security-service:test --tests "SigningKey*Test"`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-security/embedded-security-service/src/main/java/com/bytechef/ee/embedded/security/security/SigningKeyOwnershipResolver.java \
        server/ee/libs/embedded/embedded-security/embedded-security-api/src/main/java/com/bytechef/ee/embedded/security/service/SigningKeyService.java \
        server/ee/libs/embedded/embedded-security/embedded-security-service/src/test/java/com/bytechef/ee/embedded/security/security/SigningKeyOwnershipResolverTest.java \
        server/ee/libs/embedded/embedded-security/embedded-security-service/src/test/java/com/bytechef/ee/embedded/security/service/SigningKeyServiceAuthorizationTest.java
git commit -m "gecko Owner-isolate SigningKey service ops (T19)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 12: ApiClient owner-isolation (EE)

**Files:**
- Create: `server/ee/libs/automation/automation-api-platform/automation-api-platform-configuration/automation-api-platform-configuration-service/src/main/java/com/bytechef/ee/automation/apiplatform/configuration/security/ApiClientOwnershipResolver.java`
- Modify: `server/ee/libs/automation/automation-api-platform/automation-api-platform-configuration/automation-api-platform-configuration-api/src/main/java/com/bytechef/ee/automation/apiplatform/configuration/service/ApiClientService.java`
- Test (resolver + annotations): in the corresponding `-service` test tree.

**Interfaces:**
- Consumes: `ApiClientService.fetchApiClient(long)`/`getApiClient`; `ApiClient.getCreatedBy() : String`; `UserService.fetchUserByLogin`.
- Produces: bean `ApiClientOwnershipResolver`, `resourceType() == "ApiClient"`; `@PreAuthorize("hasPermission(#id, 'ApiClient:ResourceOwner', 'SELF')")` on `getApiClient`, `update`, `delete`.

> EE files — Enterprise header + `@version ee`. `ApiClient` has only `createdBy` (login String), so the resolver resolves it to a user id via `UserService.fetchUserByLogin`.

- [ ] **Step 1: Confirm a by-id read returning Optional**

Run: `grep -n "fetchApiClient\|getApiClient" server/ee/libs/automation/automation-api-platform/automation-api-platform-configuration/automation-api-platform-configuration-api/src/main/java/com/bytechef/ee/automation/apiplatform/configuration/service/ApiClientService.java`
Expected: `fetchApiClient(String secretKey, long environmentId)` exists but is not a by-id Optional. Add `Optional<ApiClient> fetchApiClient(long id)` to the service + impl delegating to `apiClientRepository.findById(id)`.

- [ ] **Step 2: Write the resolver failing test**

```java
package com.bytechef.ee.automation.apiplatform.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiClient;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiClientService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ApiClientOwnershipResolverTest {

    private final ApiClientService apiClientService = Mockito.mock(ApiClientService.class);
    private final UserService userService = Mockito.mock(UserService.class);
    private final ApiClientOwnershipResolver resolver = new ApiClientOwnershipResolver(apiClientService, userService);

    @Test
    void testResourceType() {
        assertThat(resolver.resourceType()).isEqualTo("ApiClient");
    }

    @Test
    void testResolvesOwnerViaCreatedBy() {
        ApiClient apiClient = new ApiClient();

        apiClient.setCreatedBy("alice");

        when(apiClientService.fetchApiClient(1L)).thenReturn(Optional.of(apiClient));

        User user = new User();

        user.setId(7L);

        when(userService.fetchUserByLogin("alice")).thenReturn(Optional.of(user));

        assertThat(resolver.resolveOwner(1L).ownerUserId()).hasValue(7L);
    }

    @Test
    void testUnknownIsUnknown() {
        when(apiClientService.fetchApiClient(99L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(99L).ownerUserId()).isEmpty();
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:automation:automation-api-platform:automation-api-platform-configuration:automation-api-platform-configuration-service:test --tests ApiClientOwnershipResolverTest`
Expected: FAIL.

- [ ] **Step 4: Implement the resolver** (EE header + `@version ee`)

```java
@Component
public class ApiClientOwnershipResolver implements ResourceOwnershipResolver {

    private final ApiClientService apiClientService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public ApiClientOwnershipResolver(ApiClientService apiClientService, UserService userService) {
        this.apiClientService = apiClientService;
        this.userService = userService;
    }

    @Override
    public String resourceType() {
        return "ApiClient";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return apiClientService.fetchApiClient(id)
            .map(apiClient -> apiClient.getCreatedBy())
            .flatMap(userService::fetchUserByLogin)
            .map(user -> ResourceOwner.ofUser(user.getId()))
            .orElseGet(ResourceOwner::unknown);
    }
}
```

- [ ] **Step 5: Write + run the annotation test, then annotate `ApiClientService`**

Annotation test (same `assertExpression` helper) asserts `getApiClient`/`update`/`delete` carry `"hasPermission(#id, 'ApiClient:ResourceOwner', 'SELF')"` (for `update`, `"hasPermission(#apiClient.id, 'ApiClient:ResourceOwner', 'SELF')"`). Then annotate:

```java
    @PreAuthorize("hasPermission(#id, 'ApiClient:ResourceOwner', 'SELF')")
    ApiClient getApiClient(long id);

    @PreAuthorize("hasPermission(#apiClient.id, 'ApiClient:ResourceOwner', 'SELF')")
    ApiClient update(ApiClient apiClient);

    @PreAuthorize("hasPermission(#id, 'ApiClient:ResourceOwner', 'SELF')")
    void delete(long id);
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew :server:ee:libs:automation:automation-api-platform:automation-api-platform-configuration:automation-api-platform-configuration-service:test --tests "ApiClient*Test"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/automation/automation-api-platform/automation-api-platform-configuration/automation-api-platform-configuration-service/src/main/java/com/bytechef/ee/automation/apiplatform/configuration/security/ApiClientOwnershipResolver.java \
        server/ee/libs/automation/automation-api-platform/automation-api-platform-configuration/automation-api-platform-configuration-api/src/main/java/com/bytechef/ee/automation/apiplatform/configuration/service/ApiClientService.java \
        server/ee/libs/automation/automation-api-platform/automation-api-platform-configuration/automation-api-platform-configuration-service/src/test/java/com/bytechef/ee/automation/apiplatform/configuration/security/ApiClientOwnershipResolverTest.java \
        server/ee/libs/automation/automation-api-platform/automation-api-platform-configuration/automation-api-platform-configuration-service/src/test/java/com/bytechef/ee/automation/apiplatform/configuration/service/ApiClientServiceAuthorizationTest.java
git commit -m "gecko Owner-isolate ApiClient service ops (T19)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 13: Full check + mark tasks done

**Files:**
- Modify: `gecko-remediation-tasks.md`

- [ ] **Step 1: Run the full server check**

Run: `./gradlew spotlessApply && ./gradlew check`
Expected: BUILD SUCCESSFUL. If `EnumOrdinalStabilityTest` or any `*DefinitionFactoryTest` snapshot fails, follow CLAUDE.md (delete stale snapshot JSON from `src/test/resources/definition/` and `build/resources/test/definition/`, rerun).

- [ ] **Step 2: Check off the tasks**

In `gecko-remediation-tasks.md`, change the boxes and append notes:

```markdown
- [x] **T17. Build a central resource-authorization layer** _(enabler for T18–T25)_
  Done: `ResourceOwnershipResolver` SPI + `PermissionService.hasResourceScope`/`isResourceOwner` + `ProjectWorkspacePermissionEvaluator` `X:ResourceScope`/`X:ResourceOwner` tokens. Spec: docs/superpowers/specs/2026-06-19-centralized-idor-authorization-design.md.
```

```markdown
- [x] **T18. Connections & credentials** _(~20 findings)_ — get/update/delete/tag gated via `Connection:ResourceScope` (EE workspace-scope, CE owner-isolation). NOTE: per-row result filtering for `ConnectionSearchAssetProvider` and `findAll`-style listings is a remaining follow-up; `ClusterElementDefinitionFacadeImpl` connection loads run inside already-gated editor flows and are intentionally not gated here.
```

```markdown
- [x] **T19. API keys & signing keys** _(~14 findings)_ — WorkspaceApiKey gated by `API_KEY_*` workspace scopes; personal ApiKey, SigningKey, ApiClient owner-isolated (`X:ResourceOwner`, EE-only). NOTE: `adminApiKeys` is tenant-admin only.
```

- [ ] **Step 3: Commit**

```bash
git add gecko-remediation-tasks.md
git commit -m "gecko Mark T17-T19 done in remediation task list

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage:**
- §3.1 SPI → Task 1. §3.2 PermissionService methods (CE/EE table) → Tasks 2-3. §3.3 evaluator tokens → Task 4. §5.1 connections → Tasks 6-7. §5.2 API keys (incl. new scopes §5.2 + §6) → Tasks 5, 8-10. §5.3 signing keys / api clients → Tasks 11-12. §8 testing → resolver unit tests + reflection annotation tests + wiring test, all present. §9 marking done → Task 13. Fail-closed + admin bypass (§3.2 corrected) → encoded in Tasks 2-3 logic and tested.
- Open items §10: (1) `createdBy`-login→userId resolved via `UserService.fetchUserByLogin` in Tasks 6/12; (2) list-endpoint filtering + (3) `ClusterElementDefinitionFacadeImpl` explicitly deferred with a NOTE in Task 13 (honest, not over-claimed).

**Placeholder scan:** Each code step contains real code; the only deliberate "confirm the exact method name" steps (1.x in Tasks 6, 8, 11, 12) are because the read-method name (`fetchX` vs `getX`) must be verified against source — each gives the exact fallback to implement, so there is no unresolved placeholder.

**Type consistency:** `ResourceOwner` factories (`unknown`/`ofWorkspace`/`ofUser`/`of`) used identically across Tasks 1, 6, 8, 11, 12. `hasResourceScope(String,long,String)` / `isResourceOwner(String,long)` signatures consistent across Tasks 2, 3, 4. Token strings (`'<Type>:ResourceScope'`, `'<Type>:ResourceOwner'`) consistent between evaluator (Task 4) and every annotation task. `getUserId() : Long`, `getCreatedBy() : String`, `getWorkspaceId() : Long` match the verified accessors.

## Execution Handoff

(Provided after save.)
