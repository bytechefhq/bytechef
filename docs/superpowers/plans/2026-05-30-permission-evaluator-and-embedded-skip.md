# PermissionEvaluator Migration + Embedded Authorization Skip — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `@permissionService.x(...)` bean-reference `@PreAuthorize` style with Spring Security's standard `hasPermission(...)` built-in backed by a custom `PermissionEvaluator`, and add a narrowly-scoped, fail-safe-cleared thread-local that lets `AutomationWorkflowProjectFacadeImpl` (embedded → automation delegation) bypass automation RBAC checks.

**Architecture:** A thin `PermissionEvaluator` delegates to the existing `PermissionService` (CE no-op / EE real), so all RBAC logic and edition-conditioning stay untouched. A single global `MethodSecurityExpressionHandler` bean wires the evaluator into the already-enabled `@EnableMethodSecurity`. A `ThreadLocal` flag (read at the evaluator chokepoint, set by a class-level `@SkipAutomationAuthorization` annotation + one AOP aspect on the embedded facade) implements the skip.

**Tech Stack:** Java 25, Spring Boot 4 / Spring Security 6, Spring AOP (AspectJ weaver), JUnit 5, Mockito, AssertJ, Gradle.

**Reference spec:** `docs/superpowers/specs/2026-05-30-permission-evaluator-and-embedded-skip-design.md`

---

## File Structure

**New files:**
- `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/security/AutomationAuthorizationContext.java` — thread-local skip flag + cleanup wrapper.
- `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/security/SkipAutomationAuthorization.java` — marker annotation.
- `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/security/ProjectWorkspacePermissionEvaluator.java` — the `PermissionEvaluator`.
- `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/security/AutomationMethodSecurityConfiguration.java` — global `MethodSecurityExpressionHandler` bean.
- `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/security/SkipAutomationAuthorizationAspect.java` — AOP aspect.
- Test files (see tasks): `AutomationAuthorizationContextTest`, `ProjectWorkspacePermissionEvaluatorTest`, `PermissionEvaluatorWiringIntTest`, `SkipAutomationAuthorizationAspectIntTest`.

**Modified files:**
- `automation-configuration-service` `@PreAuthorize` sites (Task 6).
- EE `automation-configuration-*` `@PreAuthorize` sites (Task 7).
- EE AI-Hub / AI-Gateway / AI-Observability / AI-Eval GraphQL controllers `@PreAuthorize` sites (Task 8).
- `AutomationWorkflowProjectFacadeImpl` — add class-level `@SkipAutomationAuthorization` (Task 5).
- `embedded-configuration-service/build.gradle.kts` — add `aspectjweaver` (Task 5).

**Unchanged:** `PermissionService` interface, both `PermissionServiceImpl` (CE/EE), the 5 direct Java callers, `SecurityConfiguration`.

---

## Annotation transformation rules (used in Tasks 6–8)

Apply these five literal rewrites to every `@PreAuthorize` SpEL string. `<ARG>` is copied verbatim (e.g. `#id`, `#projectDeployment.projectId`, `#input.workspaceId`).

| # | Old | New |
|---|---|---|
| R1 | `@permissionService.hasProjectScope(<ARG>, '<SCOPE>')` | `hasPermission(<ARG>, 'ProjectScope', '<SCOPE>')` |
| R2 | `@permissionService.hasProjectRole(<ARG>, '<ROLE>')` | `hasPermission(<ARG>, 'ProjectRole', '<ROLE>')` |
| R3 | `@permissionService.hasWorkspaceRole(<ARG>, '<ROLE>')` | `hasPermission(<ARG>, 'WorkspaceRole', '<ROLE>')` |
| R4 | `@permissionService.isCurrentUser(<ARG>)` | `hasPermission(<ARG>, 'User', 'SELF')` |
| R5 | `@permissionService.isTenantAdmin()` | `hasPermission('Tenant', 'ADMIN')` |

Combined expressions compose, e.g.:
`@permissionService.isTenantAdmin() or @permissionService.isCurrentUser(#id)`
→ `hasPermission('Tenant', 'ADMIN') or hasPermission(#id, 'User', 'SELF')`

---

## Task 1: Skip-context + marker annotation

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/security/AutomationAuthorizationContext.java`
- Create: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/security/SkipAutomationAuthorization.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/security/AutomationAuthorizationContextTest.java` (placed in the `-service` module, which already has JUnit/AssertJ test deps; `-api` has none)

- [ ] **Step 1: Write the failing test**

Create `AutomationAuthorizationContextTest.java`:

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class AutomationAuthorizationContextTest {

    @Test
    void testIsSkipChecksDefaultsFalse() {
        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }

    @Test
    void testCallSkippingChecksEnablesDuringCallAndRestoresAfter() throws Throwable {
        AtomicBoolean insideValue = new AtomicBoolean(false);

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            insideValue.set(AutomationAuthorizationContext.isSkipChecks());

            return null;
        });

        assertThat(insideValue.get()).isTrue();
        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }

    @Test
    void testCallSkippingChecksRestoresOnException() {
        assertThatThrownBy(() -> AutomationAuthorizationContext.callSkippingChecks(() -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }

    @Test
    void testNestedCallsRestoreToOuterValue() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            AutomationAuthorizationContext.callSkippingChecks(() -> null);

            assertThat(AutomationAuthorizationContext.isSkipChecks()).isTrue();

            return null;
        });

        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }

    @Test
    void testSkipDoesNotLeakToNewThread() throws Throwable {
        AtomicBoolean otherThreadValue = new AtomicBoolean(true);

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            Thread thread = new Thread(() -> otherThreadValue.set(AutomationAuthorizationContext.isSkipChecks()));

            thread.start();
            thread.join();

            return null;
        });

        assertThat(otherThreadValue.get()).isFalse();
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "com.bytechef.automation.configuration.security.AutomationAuthorizationContextTest"`
Expected: FAIL — compilation error, `AutomationAuthorizationContext` does not exist.

- [ ] **Step 3: Create the marker annotation**

Create `SkipAutomationAuthorization.java`:

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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type or method whose entire (synchronous) execution is an embedded → automation delegation,
 * during which automation RBAC checks must be bypassed. Handled by an AOP aspect that toggles
 * {@link AutomationAuthorizationContext}.
 *
 * @author Ivica Cardic
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({
    ElementType.TYPE, ElementType.METHOD
})
public @interface SkipAutomationAuthorization {
}
```

- [ ] **Step 4: Create the context class**

Create `AutomationAuthorizationContext.java`:

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

/**
 * Thread-local flag marking that the current synchronous call stack is an embedded → automation
 * delegation, during which automation RBAC checks are bypassed.
 *
 * <p>
 * Fail-open by nature: the flag is always cleared via {@code try/finally}, scoped narrowly to a single
 * embedded facade operation, and never propagated across threads (a plain {@link ThreadLocal} does not
 * cross thread boundaries, so async work fails closed — the safe direction).
 *
 * @author Ivica Cardic
 */
public final class AutomationAuthorizationContext {

    private static final ThreadLocal<Boolean> SKIP_CHECKS = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private AutomationAuthorizationContext() {
    }

    public static boolean isSkipChecks() {
        return Boolean.TRUE.equals(SKIP_CHECKS.get());
    }

    public static <V> V callSkippingChecks(SkippableCall<V> call) throws Throwable {
        boolean previous = isSkipChecks();

        SKIP_CHECKS.set(Boolean.TRUE);

        try {
            return call.call();
        } finally {
            if (previous) {
                SKIP_CHECKS.set(Boolean.TRUE);
            } else {
                SKIP_CHECKS.remove();
            }
        }
    }

    @FunctionalInterface
    public interface SkippableCall<V> {

        V call() throws Throwable;
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "com.bytechef.automation.configuration.security.AutomationAuthorizationContextTest"`
Expected: PASS (5 tests).

- [ ] **Step 6: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/security/ \
        server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/security/AutomationAuthorizationContextTest.java
git commit -m "0_732 Add automation authorization skip context and marker annotation"
```

---

## Task 2: The PermissionEvaluator

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/security/ProjectWorkspacePermissionEvaluator.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/security/ProjectWorkspacePermissionEvaluatorTest.java`

- [ ] **Step 1: Write the failing test**

Create `ProjectWorkspacePermissionEvaluatorTest.java`:

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProjectWorkspacePermissionEvaluatorTest {

    private PermissionService permissionService;
    private ProjectWorkspacePermissionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        permissionService = mock(PermissionService.class);
        evaluator = new ProjectWorkspacePermissionEvaluator(permissionService);
    }

    @Test
    void testProjectScopeDelegates() {
        when(permissionService.hasProjectScope(1L, "PROJECT_DELETE")).thenReturn(true);

        assertThat(evaluator.hasPermission(null, 1L, "ProjectScope", "PROJECT_DELETE")).isTrue();
    }

    @Test
    void testProjectRoleDelegates() {
        when(permissionService.hasProjectRole(1L, "ADMIN")).thenReturn(true);

        assertThat(evaluator.hasPermission(null, 1L, "ProjectRole", "ADMIN")).isTrue();
    }

    @Test
    void testWorkspaceRoleDelegates() {
        when(permissionService.hasWorkspaceRole(2L, "EDITOR")).thenReturn(true);

        assertThat(evaluator.hasPermission(null, 2L, "WorkspaceRole", "EDITOR")).isTrue();
    }

    @Test
    void testUserSelfDelegatesToIsCurrentUser() {
        when(permissionService.isCurrentUser(7L)).thenReturn(true);

        assertThat(evaluator.hasPermission(null, 7L, "User", "SELF")).isTrue();
    }

    @Test
    void testUserWithNonSelfPermissionReturnsFalse() {
        assertThat(evaluator.hasPermission(null, 7L, "User", "OTHER")).isFalse();

        verifyNoInteractions(permissionService);
    }

    @Test
    void testUnknownTargetTypeReturnsFalse() {
        assertThat(evaluator.hasPermission(null, 1L, "Nonsense", "X")).isFalse();
    }

    @Test
    void testTenantAdminObjectFormDelegates() {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        assertThat(evaluator.hasPermission(null, "Tenant", "ADMIN")).isTrue();
    }

    @Test
    void testUnknownObjectTargetReturnsFalse() {
        assertThat(evaluator.hasPermission(null, "Something", "ADMIN")).isFalse();

        verifyNoInteractions(permissionService);
    }

    @Test
    void testSkipChecksShortCircuitsWithoutTouchingPermissionService() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(evaluator.hasPermission(null, 1L, "ProjectScope", "PROJECT_DELETE")).isTrue();
            assertThat(evaluator.hasPermission(null, "Tenant", "ADMIN")).isTrue();

            return null;
        });

        verifyNoInteractions(permissionService);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "com.bytechef.automation.configuration.security.AutomationPermissionEvaluatorTest"`
Expected: FAIL — `ProjectWorkspacePermissionEvaluator` does not exist.

- [ ] **Step 3: Create the evaluator**

Create `ProjectWorkspacePermissionEvaluator.java`:

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

import com.bytechef.automation.configuration.service.PermissionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Adapts the automation {@link PermissionService} to Spring Security's {@link PermissionEvaluator} so
 * that authorization is expressed as the standard {@code hasPermission(...)} SpEL built-in. The
 * evaluator is deliberately thin: it routes on {@code targetType} and delegates; all RBAC logic and
 * CE/EE conditioning live in {@link PermissionService}.
 *
 * <p>
 * The three object-permission checks use the id+type overload; the two non-object checks use sentinel
 * identifiers — {@code hasPermission(#id, 'User', 'SELF')} for the current-user check and the
 * target-less {@code hasPermission('Tenant', 'ADMIN')} for the tenant-admin check.
 *
 * <p>
 * When {@link AutomationAuthorizationContext#isSkipChecks()} is active (embedded → automation
 * delegation), every check short-circuits to {@code true} without touching {@link PermissionService}.
 *
 * @author Ivica Cardic
 */
@Component
public class ProjectWorkspacePermissionEvaluator implements PermissionEvaluator {

    static final String ADMIN = "ADMIN";
    static final String PROJECT_ROLE = "ProjectRole";
    static final String PROJECT_SCOPE = "ProjectScope";
    static final String SELF = "SELF";
    static final String TENANT = "Tenant";
    static final String USER = "User";
    static final String WORKSPACE_ROLE = "WorkspaceRole";

    private final PermissionService permissionService;

    @SuppressFBWarnings("EI")
    public ProjectWorkspacePermissionEvaluator(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        return TENANT.equals(targetDomainObject) && ADMIN.equals(String.valueOf(permission)) &&
            permissionService.isTenantAdmin();
    }

    @Override
    public boolean hasPermission(
        Authentication authentication, Serializable targetId, String targetType, Object permission) {

        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        long id = ((Number) targetId).longValue();
        String value = String.valueOf(permission);

        return switch (targetType) {
            case PROJECT_SCOPE -> permissionService.hasProjectScope(id, value);
            case PROJECT_ROLE -> permissionService.hasProjectRole(id, value);
            case WORKSPACE_ROLE -> permissionService.hasWorkspaceRole(id, value);
            case USER -> SELF.equals(value) && permissionService.isCurrentUser(id);
            default -> false;
        };
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "com.bytechef.automation.configuration.security.AutomationPermissionEvaluatorTest"`
Expected: PASS (9 tests).

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/security/ProjectWorkspacePermissionEvaluator.java \
        server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/security/ProjectWorkspacePermissionEvaluatorTest.java
git commit -m "0_732 Add ProjectWorkspacePermissionEvaluator delegating to PermissionService"
```

---

## Task 3: Wire the evaluator into method security

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/security/AutomationMethodSecurityConfiguration.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/security/PermissionEvaluatorWiringIntTest.java`

This integration test proves end-to-end that a `hasPermission(...)` annotation routes through our evaluator → `PermissionService`, and that the skip context bypasses it. It mirrors the existing `PreAuthorizeProxyEnforcementIntTest` pattern (EE module) but uses the **new** annotation form.

- [ ] **Step 1: Write the failing integration test**

Create `PermissionEvaluatorWiringIntTest.java`:

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

@SpringBootTest(classes = PermissionEvaluatorWiringIntTest.Config.class)
class PermissionEvaluatorWiringIntTest {

    @Autowired
    private GuardedService guardedService;

    @Autowired
    private PermissionService permissionService;

    @BeforeEach
    void authenticateAsNonAdmin() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "viewer", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDeniesWhenScopeMissing() {
        when(permissionService.hasProjectScope(1L, "PROJECT_DELETE")).thenReturn(false);

        assertThatThrownBy(() -> guardedService.deleteProject(1L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testAllowsWhenScopePresent() {
        when(permissionService.hasProjectScope(1L, "PROJECT_DELETE")).thenReturn(true);

        assertThatCode(() -> guardedService.deleteProject(1L)).doesNotThrowAnyException();
    }

    @Test
    void testSkipContextBypassesDenial() throws Throwable {
        when(permissionService.hasProjectScope(1L, "PROJECT_DELETE")).thenReturn(false);

        Boolean result = AutomationAuthorizationContext.callSkippingChecks(() -> {
            guardedService.deleteProject(1L);

            return Boolean.TRUE;
        });

        assertThat(result).isTrue();
    }

    @SpringBootConfiguration
    @EnableMethodSecurity
    @Import({
        ProjectWorkspacePermissionEvaluator.class, AutomationMethodSecurityConfiguration.class, GuardedService.class
    })
    static class Config {

        @Bean("permissionService")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }
    }

    @Service
    static class GuardedService {

        @PreAuthorize("hasPermission(#projectId, 'ProjectScope', 'PROJECT_DELETE')")
        public void deleteProject(long projectId) {
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "com.bytechef.automation.configuration.security.PermissionEvaluatorWiringIntTest"`
Expected: FAIL — `AutomationMethodSecurityConfiguration` does not exist (compilation error).

- [ ] **Step 3: Create the configuration**

Create `AutomationMethodSecurityConfiguration.java`:

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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;

/**
 * Contributes the single global {@link MethodSecurityExpressionHandler} that backs the
 * {@code hasPermission(...)} SpEL built-in with {@link ProjectWorkspacePermissionEvaluator}. All other
 * built-ins ({@code isAuthenticated()}, {@code hasAuthority(...)}, ...) keep their default behavior.
 *
 * @author Ivica Cardic
 */
@Configuration
public class AutomationMethodSecurityConfiguration {

    /**
     * Declared {@code static} so the method-security infrastructure can initialize this handler early
     * without forcing premature initialization of the surrounding configuration.
     */
    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(PermissionEvaluator permissionEvaluator) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();

        handler.setPermissionEvaluator(permissionEvaluator);

        return handler;
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "com.bytechef.automation.configuration.security.PermissionEvaluatorWiringIntTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/security/AutomationMethodSecurityConfiguration.java \
        server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/security/PermissionEvaluatorWiringIntTest.java
git commit -m "0_732 Wire ProjectWorkspacePermissionEvaluator into method security"
```

---

## Task 4: The skip aspect (EE embedded)

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/build.gradle.kts` — add `implementation("org.aspectj:aspectjweaver")`
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/security/SkipAutomationAuthorizationAspect.java`
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/security/SkipAutomationAuthorizationAspectIntTest.java`

- [ ] **Step 1: Add the aspectjweaver dependency**

In `embedded-configuration-service/build.gradle.kts`, add inside the `dependencies { ... }` block (alongside the other `implementation(...)` entries), if not already present:

```kotlin
implementation("org.aspectj:aspectjweaver")
```

Confirm it is not already declared first:
Run: `grep -n "aspectjweaver" server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/build.gradle.kts`
If a line is returned, skip adding it.

- [ ] **Step 2: Write the failing integration test**

Create `SkipAutomationAuthorizationAspectIntTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.security.SkipAutomationAuthorization;
import com.bytechef.automation.configuration.service.PermissionService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@SpringBootTest(classes = SkipAutomationAuthorizationAspectIntTest.Config.class)
class SkipAutomationAuthorizationAspectIntTest {

    @Autowired
    private DownstreamService downstreamService;

    @Autowired
    private EmbeddedBridge embeddedBridge;

    @Autowired
    private PermissionService permissionService;

    @BeforeEach
    void authenticateAsNonAdmin() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "viewer", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        when(permissionService.hasProjectScope(1L, "WORKFLOW_EDIT")).thenReturn(false);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDirectDownstreamCallIsDenied() {
        assertThatThrownBy(() -> downstreamService.editWorkflow(1L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testCallThroughAnnotatedBridgeSkipsAuthorization() {
        assertThatCode(() -> embeddedBridge.editViaBridge(1L)).doesNotThrowAnyException();
    }

    @SpringBootConfiguration
    @EnableAspectJAutoProxy
    @EnableMethodSecurity
    @Import({
        AutomationMethodSecurityConfiguration.class, DownstreamService.class, EmbeddedBridge.class,
        ProjectWorkspacePermissionEvaluator.class, SkipAutomationAuthorizationAspect.class
    })
    static class Config {

        @Bean("permissionService")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }
    }

    @Service
    static class DownstreamService {

        @PreAuthorize("hasPermission(#projectId, 'ProjectScope', 'WORKFLOW_EDIT')")
        public void editWorkflow(long projectId) {
        }
    }

    @Service
    @SkipAutomationAuthorization
    static class EmbeddedBridge {

        private final DownstreamService downstreamService;

        EmbeddedBridge(DownstreamService downstreamService) {
            this.downstreamService = downstreamService;
        }

        public void editViaBridge(long projectId) {
            downstreamService.editWorkflow(projectId);
        }
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "com.bytechef.ee.embedded.configuration.security.SkipAutomationAuthorizationAspectIntTest"`
Expected: FAIL — `SkipAutomationAuthorizationAspect` does not exist (compilation error).

- [ ] **Step 4: Create the aspect**

Create `SkipAutomationAuthorizationAspect.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Activates {@link AutomationAuthorizationContext} skip mode for the full synchronous execution of any
 * type or method annotated with
 * {@code com.bytechef.automation.configuration.security.SkipAutomationAuthorization}. Ordered at
 * {@link Ordered#HIGHEST_PRECEDENCE} so the skip flag is set before any nested method-security
 * interceptor evaluates a downstream automation {@code @PreAuthorize}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Aspect
@Component
@ConditionalOnEEVersion
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SkipAutomationAuthorizationAspect {

    @Around(
        "@within(com.bytechef.automation.configuration.security.SkipAutomationAuthorization) || " +
            "@annotation(com.bytechef.automation.configuration.security.SkipAutomationAuthorization)")
    public Object skipAutomationAuthorization(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        return AutomationAuthorizationContext.callSkippingChecks(proceedingJoinPoint::proceed);
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "com.bytechef.ee.embedded.configuration.security.SkipAutomationAuthorizationAspectIntTest"`
Expected: PASS (2 tests).

- [ ] **Step 6: Format and commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/build.gradle.kts \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/security/SkipAutomationAuthorizationAspect.java \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/security/SkipAutomationAuthorizationAspectIntTest.java
git commit -m "0_732 Add embedded SkipAutomationAuthorization aspect"
```

---

## Task 5: Annotate the embedded facade

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectFacadeImpl.java`

- [ ] **Step 1: Add the class-level annotation**

Add the import (alphabetically among the `com.bytechef.automation.configuration.*` imports):

```java
import com.bytechef.automation.configuration.security.SkipAutomationAuthorization;
```

Add `@SkipAutomationAuthorization` to the class declaration, so the annotations read:

```java
@Service
@Transactional
@ConditionalOnEEVersion
@SkipAutomationAuthorization
public class AutomationWorkflowProjectFacadeImpl implements AutomationWorkflowProjectFacade {
```

- [ ] **Step 2: Compile to verify it builds**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Format and commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectFacadeImpl.java
git commit -m "0_732 Skip automation authorization in AutomationWorkflowProjectFacadeImpl"
```

---

## Task 6: Migrate annotations — CE automation-configuration-service

**Files (all under `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/`):**
- `facade/ProjectFacadeImpl.java`
- `service/ProjectServiceImpl.java`
- `service/ProjectWorkflowServiceImpl.java`
- `service/ProjectDeploymentServiceImpl.java`

- [ ] **Step 1: Enumerate the occurrences**

Run:
```bash
grep -rn "@permissionService\." server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/
```
Expected: a list of `@PreAuthorize("@permissionService....")` lines.

- [ ] **Step 2: Apply the transformation rules**

For every line found, rewrite the SpEL string per the rules table at the top of this plan. Representative examples (apply the same mechanically to all):

- `@PreAuthorize("@permissionService.hasProjectScope(#id, 'PROJECT_DELETE')")`
  → `@PreAuthorize("hasPermission(#id, 'ProjectScope', 'PROJECT_DELETE')")`
- `@PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'EDITOR')")`
  → `@PreAuthorize("hasPermission(#workspaceId, 'WorkspaceRole', 'EDITOR')")`
- `@PreAuthorize("@permissionService.hasProjectScope(#projectDeployment.projectId, 'DEPLOYMENT_PUSH')")`
  → `@PreAuthorize("hasPermission(#projectDeployment.projectId, 'ProjectScope', 'DEPLOYMENT_PUSH')")`
- `@PreAuthorize("@permissionService.isTenantAdmin()")`
  → `@PreAuthorize("hasPermission('Tenant', 'ADMIN')")`

Do NOT touch `@PreAuthorize("isAuthenticated()")` or any non-`@permissionService` expression.

- [ ] **Step 3: Verify none remain in this module's main sources**

Run:
```bash
grep -rn "@permissionService\." server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/
```
Expected: no output.

- [ ] **Step 4: Build and run the module tests**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:check`
Expected: BUILD SUCCESSFUL.

If a test that boots its own `@EnableMethodSecurity` context now fails on a migrated production method (because `hasPermission` resolved against the default handler with no evaluator), fix that test's `@Import` to include the evaluator + config, e.g.:

```java
@Import({
    com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration.class,
    com.bytechef.automation.configuration.security.AutomationPermissionEvaluator.class,
    /* existing imports */
})
```
and ensure a `@Bean("permissionService")` mock/stub is present in that test config. Re-run until green.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-configuration/automation-configuration-service/
git commit -m "0_732 Migrate CE automation @PreAuthorize sites to hasPermission"
```

---

## Task 7: Migrate annotations — EE automation-configuration

**Files (all under `server/ee/libs/automation/automation-configuration/`):**
- `automation-configuration-graphql/.../web/graphql/ProjectUserGraphQlController.java`
- `automation-configuration-service/.../service/ProjectUserServiceImpl.java`
- `automation-configuration-service/.../service/WorkspaceServiceImpl.java`
- `automation-configuration-service/.../service/WorkspaceUserServiceImpl.java`
- `automation-configuration-service/.../service/CustomRoleServiceImpl.java`
- `automation-configuration-service/.../facade/WorkspaceFacadeImpl.java`

- [ ] **Step 1: Enumerate the occurrences**

Run:
```bash
grep -rn "@permissionService\." server/ee/libs/automation/automation-configuration/
```

- [ ] **Step 2: Apply the transformation rules**

Rewrite every SpEL string per the rules table. Note the combined expression in `WorkspaceFacadeImpl`:

- `@PreAuthorize("@permissionService.isTenantAdmin() or @permissionService.isCurrentUser(#id)")`
  → `@PreAuthorize("hasPermission('Tenant', 'ADMIN') or hasPermission(#id, 'User', 'SELF')")`

And the recurring scope/role forms, e.g.:
- `@PreAuthorize("@permissionService.hasProjectScope(#projectId, 'PROJECT_MANAGE_USERS')")`
  → `@PreAuthorize("hasPermission(#projectId, 'ProjectScope', 'PROJECT_MANAGE_USERS')")`
- `@PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'ADMIN')")`
  → `@PreAuthorize("hasPermission(#workspaceId, 'WorkspaceRole', 'ADMIN')")`

Leave `@PreAuthorize("isAuthenticated()")` untouched.

- [ ] **Step 3: Verify none remain in these modules' main sources**

Run:
```bash
grep -rn "@permissionService\." server/ee/libs/automation/automation-configuration/*/src/main/java/
```
Expected: no output.

- [ ] **Step 4: Build and run the module tests**

Run:
```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:check \
          :server:ee:libs:automation:automation-configuration:automation-configuration-graphql:check
```
Expected: BUILD SUCCESSFUL.

Apply the same test-config fix described in Task 6 Step 4 to any failing self-booted-method-security test (notably check `PreAuthorizeProxyEnforcementIntTest` — its internal `GuardedProjectMutations` fixtures still use the `@permissionService.` bean-ref form, which remains valid because the bean is retained; leave those fixtures as-is unless the test boots a real migrated production service).

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/automation/automation-configuration/
git commit -m "0_732 Migrate EE automation @PreAuthorize sites to hasPermission"
```

---

## Task 8: Migrate annotations — EE AI controllers

**Files (GraphQL controllers, mostly `hasWorkspaceRole(..., 'VIEWER')` / `'ADMIN'`):**
- `server/ee/libs/.../AiHubWorkspaceSettingsGraphQlController.java`
- `server/ee/libs/.../AiPromptGraphQlController.java`
- `server/ee/libs/.../AiGatewayWorkspaceSettingsGraphQlController.java`
- `server/ee/libs/.../AiGatewayRequestLogGraphQlController.java`
- `server/ee/libs/.../AiObservabilitySessionGraphQlController.java`
- `server/ee/libs/.../AiObservabilityAlertRuleGraphQlController.java`
- `server/ee/libs/.../AiObservabilityExportJobGraphQlController.java`
- `server/ee/libs/.../AiObservabilityTraceGraphQlController.java`
- `server/ee/libs/.../AiObservabilityWebhookSubscriptionGraphQlController.java`
- `server/ee/libs/.../AiObservabilityNotificationChannelGraphQlController.java`
- `server/ee/libs/.../AiEvalScoreConfigGraphQlController.java`
- `server/ee/libs/.../AiEvalRuleGraphQlController.java`
- `server/ee/libs/.../AiEvalScoreGraphQlController.java`

- [ ] **Step 1: Enumerate ALL remaining occurrences across the whole server tree**

Run:
```bash
grep -rn "@permissionService\." server/ --include=*.java | grep "src/main/java"
```
This is the authoritative list — it catches any controller path not spelled out above.

- [ ] **Step 2: Apply the transformation rules**

Rewrite each per the rules table. These are predominantly:
- `@PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'VIEWER')")`
  → `@PreAuthorize("hasPermission(#workspaceId, 'WorkspaceRole', 'VIEWER')")`
- `@PreAuthorize("@permissionService.hasWorkspaceRole(#input.workspaceId, 'ADMIN')")`
  → `@PreAuthorize("hasPermission(#input.workspaceId, 'WorkspaceRole', 'ADMIN')")`

- [ ] **Step 3: Verify ZERO remain in all main sources (global)**

Run:
```bash
grep -rn "@permissionService\." server/ --include=*.java | grep "src/main/java"
```
Expected: no output.

- [ ] **Step 4: Build and run the affected modules' tests**

Run `check` on each module that owns a modified controller, e.g.:
```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-hub:<module>:check
```
(Resolve exact gradle paths from the file paths surfaced in Step 1.) Expected: BUILD SUCCESSFUL. Apply the Task 6 Step 4 test-config fix to any self-booted method-security test that fails.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/
git commit -m "0_732 Migrate EE AI controller @PreAuthorize sites to hasPermission"
```

---

## Task 9: Full verification

- [ ] **Step 1: Confirm exactly one `MethodSecurityExpressionHandler` is contributed**

Run:
```bash
grep -rn "MethodSecurityExpressionHandler" server/ --include=*.java | grep -v "src/test" | grep "@Bean\|implements\|extends\|return"
```
Expected: only `AutomationMethodSecurityConfiguration` defines a `MethodSecurityExpressionHandler` bean. If a second pre-existing handler bean is found, reconcile (there must be exactly one in any single app context).

- [ ] **Step 2: Confirm no `@permissionService.` SpEL remains in production code**

Run:
```bash
grep -rn "@permissionService\." server/ --include=*.java | grep "src/main/java"
```
Expected: no output.

- [ ] **Step 3: Run the full check on all touched modules**

Run:
```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:check \
          :server:ee:libs:automation:automation-configuration:automation-configuration-service:check \
          :server:ee:libs:automation:automation-configuration:automation-configuration-graphql:check \
          :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:check
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Spotless + global compile sanity**

Run:
```bash
./gradlew spotlessApply
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Final commit (if Spotless reformatted anything)**

```bash
git add -A
git commit -m "0_732 Apply spotless formatting for PermissionEvaluator migration" || echo "nothing to commit"
```

---

## Self-Review notes (resolved during planning)

- **Spec §3.1 (thin evaluator)** → Task 2. **§3.2 (annotation migration + sentinels)** → Tasks 6–8 + rules table. **§3.3 (global handler, placement, single-handler verification)** → Task 3 + Task 9 Step 1. **§3.4 (context, marker, aspect, set/read points)** → Tasks 1, 4, 5. **§3.5 (fail-open containment)** → Task 1 tests (restore-on-exception, no-thread-leak) + `@Order(HIGHEST_PRECEDENCE)` aspect.
- **Spec open decision §6.1 (module for context/annotation)** → resolved: `automation-configuration-api` (depended on by both the evaluator module and the embedded facade module). **§6.2 (set-point breadth)** → resolved in spec: only `AutomationWorkflowProjectFacadeImpl` (Task 5). **§6.3 (isTenantAdmin form)** → sentinel `hasPermission('Tenant', 'ADMIN')` (R5).
- **Type consistency:** `AutomationAuthorizationContext.isSkipChecks()` / `callSkippingChecks(SkippableCall)`, `ProjectWorkspacePermissionEvaluator(PermissionService)`, and the four `targetType` discriminators (`ProjectScope`, `ProjectRole`, `WorkspaceRole`, `User`) plus sentinels (`Tenant`/`ADMIN`, `SELF`) are used identically across Tasks 1–8.
- **Known risk flagged in-plan:** existing tests that build a minimal `@EnableMethodSecurity` context around *migrated production* services will deny everything unless they import the evaluator + handler — Task 6/7/8 Step 4 carries the fix recipe.
