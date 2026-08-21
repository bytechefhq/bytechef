# Connection Visibility → EE — Slice A: Read-Path SPI — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract the connection visibility-scoping decision out of CE `WorkspaceConnectionFacadeImpl.getConnections` behind a `ConnectionVisibilityResolver` SPI — CE default = PRIVATE-only, EE impl = full workspace/project/org resolution — so no EE-licensed read-path logic remains in the CE artifact.

**Architecture:** Paired edition-conditional beans (CE `@ConditionalOnCEVersion`, EE `@ConditionalOnEEVersion`), matching the existing `WorkspaceServiceImpl` pattern. The CE facade keeps `getConnections` and delegates the filter to the resolver. The EE resolver lives in `server/ee/.../automation-configuration-service` and reuses the still-CE `ProjectConnectionService` + a new EE `ProjectMembershipAccessor` (backed by `ProjectUserService`).

**Tech Stack:** Java 25, Spring Boot, Spring Data JDBC, JUnit 5 + Mockito, Gradle.

Spec: `docs/superpowers/specs/2026-06-13-connection-visibility-to-ee-design.md`

---

## File Structure

- Create `automation-configuration-api` (CE): `ConnectionVisibilityResolver.java` — the SPI interface.
- Create `automation-configuration-service` (CE): `DefaultConnectionVisibilityResolver.java` — PRIVATE-only impl (`@ConditionalOnCEVersion`).
- Modify `automation-configuration-service` (CE): `DefaultProjectMembershipAccessor.java` — add `@ConditionalOnCEVersion`.
- Create `automation-configuration-service` (EE): `ConnectionVisibilityResolverImpl.java` — full resolution (`@ConditionalOnEEVersion`); `ProjectMembershipAccessorImpl.java` — membership narrowing (`@ConditionalOnEEVersion`).
- Modify `automation-configuration-service` (CE): `WorkspaceConnectionFacadeImpl.java` — `getConnections` delegates to the resolver; drop the `projectMembershipAccessor` field/param.
- Modify EE `automation-configuration-service/build.gradle.kts` — add `platform-connection-api` + `platform-api` deps.
- Tests: CE `DefaultConnectionVisibilityResolverTest`, EE `ConnectionVisibilityResolverImplTest`, EE `ProjectMembershipAccessorImplTest`, and update CE `WorkspaceConnectionFacadeTest`.

Behavior note: introducing the EE `ProjectMembershipAccessor` (Task 5) intentionally **changes** EE behavior — PROJECT-shared connections become visible only to project members, not all workspace members (closing the gap the CE no-op left open). This is the intended EE correctness fix and is tested explicitly.

---

## Task 1: `ConnectionVisibilityResolver` SPI interface (CE)

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/ConnectionVisibilityResolver.java`

- [ ] **Step 1: Create the interface**

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

package com.bytechef.automation.configuration.service;

import com.bytechef.platform.connection.dto.ConnectionDTO;
import java.util.List;

/**
 * Filters the connections of a workspace down to those visible to the current principal. CE resolves
 * PRIVATE-only (creator or admin); EE resolves the full WORKSPACE / PROJECT / ORGANIZATION scope model.
 *
 * @author Ivica Cardic
 */
public interface ConnectionVisibilityResolver {

    /**
     * @param connections all connections belonging to the workspace (already loaded); never {@code null}
     * @param workspaceId the workspace whose connections are being listed
     * @return the subset visible to the current principal. Never {@code null}; may be empty.
     */
    List<ConnectionDTO> filterVisible(List<ConnectionDTO> connections, long workspaceId);
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-api:compileJava -q`
Expected: BUILD SUCCESSFUL (the module already depends on `platform-connection-api` transitively via `ConnectionDTO` use elsewhere; if it fails with "package com.bytechef.platform.connection.dto does not exist", add `api(project(":server:libs:platform:platform-connection:platform-connection-api"))` to that module's `build.gradle.kts` and re-run).

- [ ] **Step 3: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/ConnectionVisibilityResolver.java
git commit -m "4750 Add ConnectionVisibilityResolver SPI for connection read-path visibility"
```

---

## Task 2: CE default resolver (PRIVATE-only) + test

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/DefaultConnectionVisibilityResolver.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/service/DefaultConnectionVisibilityResolverTest.java`

- [ ] **Step 1: Write the failing test**

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

package com.bytechef.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.platform.connection.domain.ConnectionVisibility;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class DefaultConnectionVisibilityResolverTest {

    private final DefaultConnectionVisibilityResolver resolver = new DefaultConnectionVisibilityResolver();

    private static ConnectionDTO connection(long id, String createdBy, ConnectionVisibility visibility) {
        return ConnectionDTO.builder()
            .id(id)
            .createdBy(createdBy)
            .name("c" + id)
            .componentName("x")
            .visibility(visibility)
            .build();
    }

    @Test
    void testNonAdminSeesOnlyOwnPrivateConnections() {
        ConnectionDTO mine = connection(1L, "user", ConnectionVisibility.PRIVATE);
        ConnectionDTO others = connection(2L, "other", ConnectionVisibility.PRIVATE);
        ConnectionDTO workspace = connection(3L, "other", ConnectionVisibility.WORKSPACE);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin)
                .thenReturn("user");
            securityUtils.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
                .thenReturn(false);

            List<ConnectionDTO> result = resolver.filterVisible(List.of(mine, others, workspace), 10L);

            assertThat(result).containsExactly(mine);
        }
    }

    @Test
    void testAdminSeesAllPrivateConnections() {
        ConnectionDTO mine = connection(1L, "user", ConnectionVisibility.PRIVATE);
        ConnectionDTO others = connection(2L, "other", ConnectionVisibility.PRIVATE);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin)
                .thenReturn("admin");
            securityUtils.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
                .thenReturn(true);

            List<ConnectionDTO> result = resolver.filterVisible(List.of(mine, others), 10L);

            assertThat(result).containsExactly(mine, others);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "*DefaultConnectionVisibilityResolverTest" -q`
Expected: FAIL — `DefaultConnectionVisibilityResolver` does not exist (compile error).

- [ ] **Step 3: Write the implementation**

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

package com.bytechef.automation.configuration.service;

import com.bytechef.platform.annotation.ConditionalOnCEVersion;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * CE default: only PRIVATE connections are reachable in CE, so a connection is visible to its creator or to an admin
 * (admins need full PRIVATE visibility for orphan-recovery from the connections list). WORKSPACE / PROJECT /
 * ORGANIZATION connections do not exist in CE (creation is forced PRIVATE) and are filtered out if present.
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnCEVersion
public class DefaultConnectionVisibilityResolver implements ConnectionVisibilityResolver {

    @Override
    public List<ConnectionDTO> filterVisible(List<ConnectionDTO> connections, long workspaceId) {
        String currentUserLogin = SecurityUtils.getCurrentUserLogin();
        boolean isAdmin = SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN);

        return connections.stream()
            .filter(connection -> switch (connection.visibility()) {
                case PRIVATE -> isAdmin || Objects.equals(currentUserLogin, connection.createdBy());
                default -> false;
            })
            .toList();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "*DefaultConnectionVisibilityResolverTest" -q`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/DefaultConnectionVisibilityResolver.java server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/service/DefaultConnectionVisibilityResolverTest.java
git commit -m "4750 Add CE PRIVATE-only ConnectionVisibilityResolver default"
```

---

## Task 3: Gate the CE `DefaultProjectMembershipAccessor` so EE can override

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/DefaultProjectMembershipAccessor.java`

- [ ] **Step 1: Add `@ConditionalOnCEVersion` and fix the stale `@Primary` doc**

Replace the class annotations and the class Javadoc's last sentence. The class currently reads:

```java
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * CE default implementation. Returns the input list unchanged because CE has no per-project membership table: any
 * workspace member can see any PROJECT-scoped connection shared to a project in that workspace. EE supplies a narrower
 * implementation marked with {@code @Primary} so it takes precedence when present.
 *
 * @author Ivica Cardic
 */
@Service
public class DefaultProjectMembershipAccessor implements ProjectMembershipAccessor {
```

Change to:

```java
import com.bytechef.platform.annotation.ConditionalOnCEVersion;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * CE default implementation. Returns the input list unchanged because CE has no per-project membership table: any
 * workspace member can see any PROJECT-scoped connection shared to a project in that workspace. EE supplies a narrower
 * implementation gated with {@code @ConditionalOnEEVersion}; this default is gated with
 * {@code @ConditionalOnCEVersion} so exactly one is active per edition.
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnCEVersion
public class DefaultProjectMembershipAccessor implements ProjectMembershipAccessor {
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/DefaultProjectMembershipAccessor.java
git commit -m "4750 Gate DefaultProjectMembershipAccessor with @ConditionalOnCEVersion"
```

---

## Task 4: EE `ConnectionVisibilityResolver` impl (full resolution) + test

**Files:**
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-service/build.gradle.kts`
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/service/ConnectionVisibilityResolverImpl.java`
- Test: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/service/ConnectionVisibilityResolverImplTest.java`

- [ ] **Step 1: Add the build dependencies**

In `server/ee/libs/automation/automation-configuration/automation-configuration-service/build.gradle.kts`, inside `dependencies { ... }`, add (alphabetical with the other `implementation(project(...))` lines):

```kotlin
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-connection:platform-connection-api"))
```

- [ ] **Step 2: Write the failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectConnection;
import com.bytechef.automation.configuration.service.ProjectConnectionService;
import com.bytechef.automation.configuration.service.ProjectMembershipAccessor;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.platform.connection.domain.ConnectionVisibility;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConnectionVisibilityResolverImplTest {

    @Mock
    private ProjectConnectionService projectConnectionService;
    @Mock
    private ProjectMembershipAccessor projectMembershipAccessor;
    @Mock
    private ProjectService projectService;

    private static ConnectionDTO connection(long id, String createdBy, ConnectionVisibility visibility) {
        return ConnectionDTO.builder()
            .id(id)
            .createdBy(createdBy)
            .name("c" + id)
            .componentName("x")
            .visibility(visibility)
            .build();
    }

    @Test
    void testWorkspaceVisibleToAllMembers() {
        ConnectionVisibilityResolverImpl resolver =
            new ConnectionVisibilityResolverImpl(projectConnectionService, projectMembershipAccessor, projectService);

        ConnectionDTO workspace = connection(1L, "owner", ConnectionVisibility.WORKSPACE);

        when(projectService.getProjects(null, null, null, null, null, 10L)).thenReturn(List.of());
        when(projectConnectionService.getProjectConnectionsByProjectIds(anyList())).thenReturn(List.of());

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin)
                .thenReturn("member");
            securityUtils.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
                .thenReturn(false);

            List<ConnectionDTO> result = resolver.filterVisible(List.of(workspace), 10L);

            assertThat(result).containsExactly(workspace);
        }
    }

    @Test
    void testProjectVisibleOnlyToProjectMembers() {
        ConnectionVisibilityResolverImpl resolver =
            new ConnectionVisibilityResolverImpl(projectConnectionService, projectMembershipAccessor, projectService);

        ConnectionDTO shared = connection(5L, "owner", ConnectionVisibility.PROJECT);

        when(projectService.getProjects(null, null, null, null, null, 10L))
            .thenReturn(List.of(project(100L), project(200L)));
        when(projectMembershipAccessor.filterByMembership("member", List.of(100L, 200L)))
            .thenReturn(List.of(100L));
        when(projectConnectionService.getProjectConnectionsByProjectIds(List.of(100L)))
            .thenReturn(List.of(projectConnection(5L)));

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin)
                .thenReturn("member");
            securityUtils.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
                .thenReturn(false);

            List<ConnectionDTO> result = resolver.filterVisible(List.of(shared), 10L);

            assertThat(result).containsExactly(shared);
        }
    }

    private static Project project(long id) {
        Project project = new Project();

        project.setId(id);

        return project;
    }

    private static ProjectConnection projectConnection(long connectionId) {
        ProjectConnection projectConnection = new ProjectConnection();

        projectConnection.setConnectionId(connectionId);

        return projectConnection;
    }
}
```

NOTE before writing: confirm `ProjectConnection` exposes `setConnectionId(long)` and `Project` exposes `setId(long)`; if the setters differ, adjust the two helper methods to match. (Both are plain Spring Data JDBC entities with standard setters as of this writing.)

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests "*ConnectionVisibilityResolverImplTest" -q`
Expected: FAIL — `ConnectionVisibilityResolverImpl` does not exist.

- [ ] **Step 4: Write the implementation (logic moved verbatim from the CE facade)**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectConnection;
import com.bytechef.automation.configuration.service.ConnectionVisibilityResolver;
import com.bytechef.automation.configuration.service.ProjectConnectionService;
import com.bytechef.automation.configuration.service.ProjectMembershipAccessor;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * EE full visibility resolution: WORKSPACE and ORGANIZATION connections are visible to all workspace members; PROJECT
 * connections only to members of a project they are shared with; PRIVATE only to the creator (or any admin, for
 * orphan-recovery). Membership narrowing comes from {@link ProjectMembershipAccessor}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class ConnectionVisibilityResolverImpl implements ConnectionVisibilityResolver {

    private final ProjectConnectionService projectConnectionService;
    private final ProjectMembershipAccessor projectMembershipAccessor;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public ConnectionVisibilityResolverImpl(
        ProjectConnectionService projectConnectionService, ProjectMembershipAccessor projectMembershipAccessor,
        ProjectService projectService) {

        this.projectConnectionService = projectConnectionService;
        this.projectMembershipAccessor = projectMembershipAccessor;
        this.projectService = projectService;
    }

    @Override
    public List<ConnectionDTO> filterVisible(List<ConnectionDTO> connections, long workspaceId) {
        String currentUserLogin = SecurityUtils.getCurrentUserLogin();
        boolean isAdmin = SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN);

        List<Long> workspaceProjectIds = CollectionUtils.map(
            projectService.getProjects(null, null, null, null, null, workspaceId), Project::getId);

        List<Long> accessibleProjectIds = isAdmin || currentUserLogin == null
            ? workspaceProjectIds
            : projectMembershipAccessor.filterByMembership(currentUserLogin, workspaceProjectIds);

        Set<Long> projectConnectionIds = new HashSet<>(
            CollectionUtils.map(
                projectConnectionService.getProjectConnectionsByProjectIds(accessibleProjectIds),
                ProjectConnection::getConnectionId));

        return connections.stream()
            .filter(connection -> switch (connection.visibility()) {
                case ORGANIZATION, WORKSPACE -> true;
                case PROJECT -> projectConnectionIds.contains(connection.id());
                case PRIVATE -> isAdmin || Objects.equals(currentUserLogin, connection.createdBy());
            })
            .toList();
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests "*ConnectionVisibilityResolverImplTest" -q`
Expected: PASS (2 tests).

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/automation/automation-configuration/automation-configuration-service/build.gradle.kts server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/service/ConnectionVisibilityResolverImpl.java server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/service/ConnectionVisibilityResolverImplTest.java
git commit -m "4750 Add EE ConnectionVisibilityResolver with full scope resolution"
```

---

## Task 5: EE `ProjectMembershipAccessor` impl (membership narrowing) + test

**Files:**
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/service/ProjectMembershipAccessorImpl.java`
- Test: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/service/ProjectMembershipAccessorImplTest.java`

This closes the PROJECT-visibility gap: a workspace member who is not a project member must not see connections shared to that project. Backed by `ProjectUserRepository.findProjectIdsByUserIdAndWorkspaceId` (already used by `WorkspaceServiceImpl`).

- [ ] **Step 1: Confirm the lookup chain**

Run: `grep -n "findProjectIdsByUserIdAndWorkspaceId\|interface ProjectUserRepository" server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/repository/ProjectUserRepository.java`
Expected: shows `List<Long> findProjectIdsByUserIdAndWorkspaceId(long userId, long workspaceId)`. Also confirm how to resolve a `userId` from a `userLogin` and the current `workspaceId` — `UserService.getUser(login).getId()` resolves the id. The accessor receives only `userLogin` + `candidateProjectIds` (no workspaceId); resolve the user id, fetch all the user's project ids across the workspace via the candidate projects' workspace, then intersect. Since the candidate project ids are already workspace-scoped, intersect the user's project memberships against them directly.

- [ ] **Step 2: Write the failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.configuration.repository.ProjectUserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectMembershipAccessorImplTest {

    @Mock
    private ProjectUserRepository projectUserRepository;

    @Test
    void testNarrowsToMemberProjects() {
        ProjectMembershipAccessorImpl accessor = new ProjectMembershipAccessorImpl(projectUserRepository);

        when(projectUserRepository.findProjectIdsByUserLogin("member")).thenReturn(List.of(100L, 300L));

        List<Long> result = accessor.filterByMembership("member", List.of(100L, 200L, 300L));

        assertThat(result).containsExactly(100L, 300L);
    }

    @Test
    void testReturnsEmptyWhenNoMemberships() {
        ProjectMembershipAccessorImpl accessor = new ProjectMembershipAccessorImpl(projectUserRepository);

        when(projectUserRepository.findProjectIdsByUserLogin("member")).thenReturn(List.of());

        List<Long> result = accessor.filterByMembership("member", List.of(100L, 200L));

        assertThat(result).isEmpty();
    }
}
```

NOTE: this test assumes a repository method `findProjectIdsByUserLogin(String)`. The existing repository exposes `findProjectIdsByUserIdAndWorkspaceId(long, long)`. During implementation, EITHER (a) add a derived query `List<Long> findProjectIdsByUserLogin(String userLogin)` to `ProjectUserRepository` if `project_user` joins to a login, OR (b) resolve `userLogin → userId` via `UserService` and keep `findProjectIdsByUserIdAndWorkspaceId`, threading a workspaceId. Because the accessor signature has no workspaceId, prefer (a): add a query keyed on login that returns all the user's project ids; the caller already constrains candidates to the workspace. Adjust the test's mocked method to match the chosen approach before moving on. Confirm the actual `project_user` schema (does it store user id or login?) with: `grep -rn "class ProjectUser" server/ee --include=*.java` and read the entity.

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests "*ProjectMembershipAccessorImplTest" -q`
Expected: FAIL — `ProjectMembershipAccessorImpl` does not exist.

- [ ] **Step 4: Write the implementation**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.service.ProjectMembershipAccessor;
import com.bytechef.ee.automation.configuration.repository.ProjectUserRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * EE membership narrowing: returns only the candidate project ids the user is actually a member of, closing the
 * PROJECT-visibility gap the CE no-op accessor leaves open.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class ProjectMembershipAccessorImpl implements ProjectMembershipAccessor {

    private final ProjectUserRepository projectUserRepository;

    @SuppressFBWarnings("EI")
    public ProjectMembershipAccessorImpl(ProjectUserRepository projectUserRepository) {
        this.projectUserRepository = projectUserRepository;
    }

    @Override
    public List<Long> filterByMembership(String userLogin, List<Long> candidateProjectIds) {
        Set<Long> memberProjectIds = new HashSet<>(projectUserRepository.findProjectIdsByUserLogin(userLogin));

        return candidateProjectIds.stream()
            .filter(memberProjectIds::contains)
            .toList();
    }
}
```

- [ ] **Step 5: Add the repository query** (only if approach (a) was chosen)

In `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/repository/ProjectUserRepository.java`, add:

```java
    @Query("SELECT pu.project_id FROM project_user pu JOIN \"user\" u ON pu.user_id = u.id WHERE u.login = :userLogin")
    List<Long> findProjectIdsByUserLogin(String userLogin);
```

(Verify the `user` table/column names against an existing query in the repository before committing; match the existing quoting/casing style.)

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests "*ProjectMembershipAccessorImplTest" -q`
Expected: PASS (2 tests).

- [ ] **Step 7: Commit**

```bash
git add server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/service/ProjectMembershipAccessorImpl.java server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/service/ProjectMembershipAccessorImplTest.java server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/repository/ProjectUserRepository.java
git commit -m "4750 Add EE ProjectMembershipAccessor narrowing project visibility to members"
```

---

## Task 6: Delegate `WorkspaceConnectionFacadeImpl.getConnections` to the resolver

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionFacadeImpl.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionFacadeTest.java`

- [ ] **Step 1: Replace the `getConnections` body**

Replace lines 297–348 (the whole `getConnections` method) with:

```java
    @Override
    @Transactional(readOnly = true)
    public List<ConnectionDTO> getConnections(
        long workspaceId, String componentName, Integer connectionVersion, Long environmentId, Long tagId) {

        List<Long> connectionIds = CollectionUtils.map(
            workspaceConnectionService.getWorkspaceConnections(workspaceId), WorkspaceConnection::getConnectionId);

        if (connectionIds.isEmpty()) {
            return List.of();
        }

        List<ConnectionDTO> allConnections = connectionFacade.getConnections(
            componentName, connectionVersion, connectionIds, tagId, environmentId, PlatformType.AUTOMATION);

        return connectionVisibilityResolver.filterVisible(allConnections, workspaceId);
    }
```

- [ ] **Step 2: Add the `connectionVisibilityResolver` field, replace `projectMembershipAccessor`**

In the field block (around line 90), remove:

```java
    private final ProjectMembershipAccessor projectMembershipAccessor;
```

and add (keep alphabetical):

```java
    private final ConnectionVisibilityResolver connectionVisibilityResolver;
```

In the constructor parameter list, remove `ProjectMembershipAccessor projectMembershipAccessor,` and add `ConnectionVisibilityResolver connectionVisibilityResolver,` (keep alphabetical with the other params). In the constructor body, remove `this.projectMembershipAccessor = projectMembershipAccessor;` and add `this.connectionVisibilityResolver = connectionVisibilityResolver;`.

Add the import `import com.bytechef.automation.configuration.service.ConnectionVisibilityResolver;` and remove `import com.bytechef.automation.configuration.service.ProjectMembershipAccessor;`. Also remove now-unused imports if the compiler flags them: `Project`, `ProjectService`, `SecurityUtils`, `AuthorityConstants`, `HashSet`, `Objects`, `Set` are still used by other methods — **do not** remove them blindly; let Step 4's compile tell you. (`projectService` and `projectConnectionService` fields remain — they are still used by the write-path methods until Slice D.)

- [ ] **Step 3: Update the CE facade test constructor calls**

In `WorkspaceConnectionFacadeTest.java`, the `@Mock` for `ProjectMembershipAccessor` is replaced by a `@Mock ConnectionVisibilityResolver connectionVisibilityResolver;` and every `new WorkspaceConnectionFacadeImpl(...)` call swaps the `projectMembershipAccessor` argument for `connectionVisibilityResolver` (same positional slot, alphabetical). For the existing `getConnections` test(s), replace the now-removed stubs on `projectService`/`projectMembershipAccessor`/`projectConnectionService` (read path) with a single stub:

```java
        when(connectionVisibilityResolver.filterVisible(anyList(), eq(WORKSPACE_ID)))
            .thenReturn(List.of(/* the expected visible ConnectionDTOs for this test */));
```

Run `grep -n "getConnections\|projectMembershipAccessor\|new WorkspaceConnectionFacadeImpl" WorkspaceConnectionFacadeTest.java` first to enumerate the call sites; update each. The read-path filtering assertions move to `DefaultConnectionVisibilityResolverTest` / `ConnectionVisibilityResolverImplTest` (Tasks 2 & 4), so the facade test only needs to assert it returns whatever the resolver returns.

- [ ] **Step 4: Compile and run the CE facade module tests**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:compileJava :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "*WorkspaceConnectionFacadeTest" -q`
Expected: BUILD SUCCESSFUL; tests pass. Fix any unused-import / unused-field checkstyle errors surfaced.

- [ ] **Step 5: Run spotless + the module check**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:spotlessApply :server:libs:automation:automation-configuration:automation-configuration-service:check -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionFacadeImpl.java server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionFacadeTest.java
git commit -m "4750 Delegate WorkspaceConnectionFacade.getConnections to ConnectionVisibilityResolver"
```

---

## Task 7: Edition wiring verification

**Files:** none (verification only)

- [ ] **Step 1: Full compile across editions**

Run: `./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL — confirms the EE module's new deps resolve and nothing else referenced the removed `projectMembershipAccessor` facade field.

- [ ] **Step 2: Confirm exactly one resolver bean per edition**

Run: `grep -rn "@ConditionalOnCEVersion\|@ConditionalOnEEVersion" server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/DefaultConnectionVisibilityResolver.java server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/service/ConnectionVisibilityResolverImpl.java`
Expected: CE impl shows `@ConditionalOnCEVersion`, EE impl shows `@ConditionalOnEEVersion` — mutually exclusive.

- [ ] **Step 3: Run the broader automation-configuration test suite**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test :server:ee:libs:automation:automation-configuration:automation-configuration-service:test -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Final commit (if spotless/formatting produced changes)**

```bash
git add -A
git commit -m "4750 Format and finalize read-path visibility resolver slice"
```

---

## Self-Review notes

- **Behavior change (intended):** Task 5 makes EE PROJECT visibility member-scoped. CE behavior is unchanged (no-op → PRIVATE-only resolver yields the same set since CE connections are all PRIVATE).
- **Deferred to later slices:** `projectConnectionService` / `projectService` fields remain in the CE facade for the write-path methods (Slice D). Audit annotations on the CE facade are untouched (Slice E / follow-on).
- **Risk:** the repository-query approach in Task 5 (login→projectIds) must be validated against the real `project_user` schema before committing; the task spells out the fallback (resolve via `UserService`).
