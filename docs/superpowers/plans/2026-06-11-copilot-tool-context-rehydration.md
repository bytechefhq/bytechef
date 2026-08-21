# Copilot Tool Context Rehydration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the `workflow_editor_build` agent's tools rehydrate `{tenant, user}` from the `ToolContext` so they persist correctly on worker threads, then delete the `WorkflowGenerationSubmissionStore` hand-off — and migrate AI Hub onto the same shared wrapper.

**Architecture:** Identity travels as data on Spring AI's `ToolContext` (a method parameter, thread-safe across async hops) and is rehydrated as context inside a shared `RehydrateContextToolCallback` decorator (tenant via `TenantContext`, security via an extracted `SecurityContextRehydrator`). The wrapper is applied at tool registration. With it in place the agent calls `updateWorkflow` directly in both interactive and autonomous modes, so the submission store, the `submitWorkflow` tool, and the autonomous prompt's persistence prohibitions are removed.

**Tech Stack:** Java 25, Spring Boot 4, Spring AI (ToolCallback / ToolContext / ToolCallbacks), JUnit 5 + Mockito + AssertJ, `TenantContext` (`server/libs/core/tenant/tenant-api`), `SecurityUtils` (`platform-api`).

**Spec:** `docs/superpowers/specs/2026-06-11-copilot-tool-context-rehydration-design.md`

**Conventions:** Every new/changed file under `server/ee/` uses the ByteChef Enterprise license header and a `@version ee` Javadoc tag. Run `./gradlew spotlessApply` before each commit. Commit messages: `5184 <description>` (server-side).

---

## File Structure

**Phase 1 — Copilot (`server/ee/libs/ai/ai-copilot`)**
- New `ai-copilot-tool/.../tool/SecurityContextRehydrator.java` — `@Component`; `userId → User → authorities → SecurityUtils.runAs` (extracted from `PropertyOptionsResolver`).
- New `ai-copilot-tool/.../tool/RehydrateContextToolCallback.java` — `ToolCallback` decorator: tenant + security rehydration from `AgentToolInvocationContext`.
- New `ai-copilot-tool/.../tool/CopilotToolCallbackWrappers.java` — registration-time wrapping helper.
- Modify `ai-copilot-tool/.../tool/AgentToolInvocationContext.java` — add `tenantId`.
- Modify `ai-copilot-tool/.../tool/PropertyOptionsResolver.java` — delegate to `SecurityContextRehydrator`.
- Modify `ai-copilot-tool/build.gradle.kts` — add `tenant-api`.
- Modify `ai-copilot-api/.../util/CopilotStateKeys.java` — add `STATE_TENANT_ID`.
- Modify `ai-copilot-service/.../util/CopilotToolContextUtils.java` — read `tenantId`.
- Modify `ai-copilot-rest/.../web/rest/CopilotApiController.java` + `ai-copilot-rest/build.gradle.kts` — capture tenant into state.
- Modify `ai-copilot-service/.../config/CopilotConfiguration.java` — wrap build-agent tools, inject `SecurityContextRehydrator`, drop the store from the tool list.
- Modify `ai-copilot-service/.../resources/prompt_workflow_editor_build.txt` — remove persistence prohibitions.
- Modify `ai-copilot-service/.../service/CopilotWorkflowGeneratorImpl.java` — seed `{tenantId,userId}`, delete poll/persist tail.
- Delete `ai-copilot-service/.../service/WorkflowGenerationSubmissionStore.java`.

**Phase 2 — AI Hub migration (`server/ee/libs/ai/ai-hub`)**
- Modify `ai-hub-service/.../agent/AiHubToolCallbackWrappers.java` — swap to shared `RehydrateContextToolCallback`.
- Modify `ai-hub-service/.../agent/AiHubSpringAIAgent.java` — pass `tenantId` into `AgentToolInvocationContext`; inject `SecurityContextRehydrator`.
- Modify `ai-hub-rest/.../web/rest/AiHubApiController.java` — capture tenant into a verified state key.
- Modify `ai-hub-service/.../toolsearch/ToolSearchAdvisorConfiguration.java` — pass the rehydrator to the wrapper.
- Delete `ai-hub-service/.../agent/RehydrateSecurityContextToolCallback.java` + its test.

---

## PHASE 1 — Copilot

### Task 1: Add `tenantId` to `AgentToolInvocationContext`

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ee/ai/copilot/tool/AgentToolInvocationContext.java`
- Test: `server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/test/java/com/bytechef/ee/ai/copilot/tool/AgentToolInvocationContextTest.java`

- [ ] **Step 1: Write the failing test** (create the test file)

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @version ee
 */
class AgentToolInvocationContextTest {

    @Test
    void testTenantIdRoundTrip() {
        AgentToolInvocationContext context = new AgentToolInvocationContext(1L, 2L, 3L, "conv", "acme");

        Map<String, Object> map = context.toToolContext();

        assertThat(map).containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_TENANT_ID_KEY, "acme");

        AgentToolInvocationContext restored = AgentToolInvocationContext.fromToolContext(new ToolContext(map));

        assertThat(restored).isNotNull();
        assertThat(restored.tenantId()).isEqualTo("acme");
        assertThat(restored.userId()).isEqualTo(2L);
    }

    @Test
    void testNullTenantIdOmitted() {
        AgentToolInvocationContext context = new AgentToolInvocationContext(1L, 2L, 3L, null, null);

        assertThat(context.toToolContext()).doesNotContainKey(AgentToolInvocationContext.TOOL_CONTEXT_TENANT_ID_KEY);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-tool:test --tests '*AgentToolInvocationContextTest'`
Expected: COMPILE FAILURE — constructor takes 4 args, `tenantId()` / `TOOL_CONTEXT_TENANT_ID_KEY` undefined.

- [ ] **Step 3: Add `tenantId` to the record** (append as the last component to minimise positional churn)

In `AgentToolInvocationContext.java`, change the record header and key block:

```java
public record AgentToolInvocationContext(
    @Nullable Long workspaceId, @Nullable Long userId, @Nullable Long environmentId,
    @Nullable String conversationId, @Nullable String tenantId) {

    public static final String TOOL_CONTEXT_WORKSPACE_ID_KEY = "bytechef.agentTool.workspaceId";
    public static final String TOOL_CONTEXT_USER_ID_KEY = "bytechef.agentTool.userId";
    public static final String TOOL_CONTEXT_ENVIRONMENT_ID_KEY = "bytechef.agentTool.environmentId";
    public static final String TOOL_CONTEXT_CONVERSATION_ID_KEY = "bytechef.agentTool.conversationId";
    public static final String TOOL_CONTEXT_TENANT_ID_KEY = "bytechef.agentTool.tenantId";
```

In `fromToolContext`, read it and include in the all-null guard + constructor:

```java
        String conversationId = asString(map.get(TOOL_CONTEXT_CONVERSATION_ID_KEY));
        String tenantId = asString(map.get(TOOL_CONTEXT_TENANT_ID_KEY));

        if (workspaceId == null && userId == null && environmentId == null && conversationId == null
            && tenantId == null) {
            return null;
        }

        return new AgentToolInvocationContext(workspaceId, userId, environmentId, conversationId, tenantId);
```

In `toToolContext`, add:

```java
        if (tenantId != null) {
            map.put(TOOL_CONTEXT_TENANT_ID_KEY, tenantId);
        }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-tool:test --tests '*AgentToolInvocationContextTest'`
Expected: PASS. (Compilation of the two existing call sites — `CopilotToolContextUtils` and `AiHubSpringAIAgent` — will break; they are fixed in Task 6 and Task 12. For now this module compiles because both callers live in other modules.)

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ee/ai/copilot/tool/AgentToolInvocationContext.java server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/test/java/com/bytechef/ee/ai/copilot/tool/AgentToolInvocationContextTest.java
git commit -m "5184 Add tenantId to AgentToolInvocationContext"
```

---

### Task 2: Extract `SecurityContextRehydrator`

**Files:**
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ee/ai/copilot/tool/SecurityContextRehydrator.java`
- Test: `server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/test/java/com/bytechef/ee/ai/copilot/tool/SecurityContextRehydratorTest.java`

- [ ] **Step 1: Write the failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class SecurityContextRehydratorTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthorityService authorityService;

    @Test
    void testRunsAsUserWhenUserIdPresent() {
        User user = new User();

        user.setId(7L);
        user.setLogin("user@localhost.com");

        when(userService.fetchUser(7L)).thenReturn(Optional.of(user));

        SecurityContextRehydrator rehydrator = new SecurityContextRehydrator(userService, authorityService);

        String principal = rehydrator.withUserSecurityContext(
            7L, () -> (String) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal());

        assertThat(principal).isEqualTo("user@localhost.com");
    }

    @Test
    void testNoRehydrationWhenUserIdNull() {
        SecurityContextRehydrator rehydrator = new SecurityContextRehydrator(userService, authorityService);

        boolean noAuth = rehydrator.withUserSecurityContext(
            null, () -> SecurityContextHolder.getContext()
                .getAuthentication() == null);

        assertThat(noAuth).isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-tool:test --tests '*SecurityContextRehydratorTest'`
Expected: COMPILE FAILURE — `SecurityContextRehydrator` does not exist.

- [ ] **Step 3: Create `SecurityContextRehydrator`** (lift the logic verbatim from `PropertyOptionsResolver.withUserSecurityContext` + its authority loop)

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Rehydrates Spring Security's SecurityContext from a user id on Reactor scheduler / worker threads that did not inherit
 * the request thread's thread-local SecurityContext. Single source of truth for the {@code userId → login + authorities
 * → SecurityUtils.runAs} sequence shared by the property-options pickers and the {@link RehydrateContextToolCallback}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class SecurityContextRehydrator {

    private static final Logger log = LoggerFactory.getLogger(SecurityContextRehydrator.class);

    private final UserService userService;
    private final AuthorityService authorityService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SecurityContextRehydrator(UserService userService, AuthorityService authorityService) {
        this.userService = userService;
        this.authorityService = authorityService;
    }

    /**
     * Resolves the user's login + authorities and runs {@code action} inside a temporary SecurityContext. When
     * {@code userId} is null or the user no longer exists, runs the action without rehydration — the callee then sees no
     * current user, the same as a scheduler-initiated call. Authority-resolution failures are logged at debug and the
     * user is treated as having no authorities rather than aborting.
     */
    public <T> T withUserSecurityContext(@Nullable Long userId, Supplier<T> action) {
        if (userId == null) {
            return action.get();
        }

        Optional<User> userOptional = userService.fetchUser(userId);

        if (userOptional.isEmpty()) {
            log.debug("Skipping SecurityContext rehydration: user id {} not found", userId);

            return action.get();
        }

        User user = userOptional.get();

        List<GrantedAuthority> authorities = new ArrayList<>();

        for (Long authorityId : user.getAuthorityIds()) {
            Optional<Authority> authorityOptional = authorityService.fetchAuthority(authorityId);

            authorityOptional.map(Authority::getName)
                .map(SimpleGrantedAuthority::new)
                .ifPresent(authorities::add);
        }

        return SecurityUtils.runAs(user.getLogin(), authorities, action);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-tool:test --tests '*SecurityContextRehydratorTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ee/ai/copilot/tool/SecurityContextRehydrator.java server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/test/java/com/bytechef/ee/ai/copilot/tool/SecurityContextRehydratorTest.java
git commit -m "5184 Extract SecurityContextRehydrator from PropertyOptionsResolver"
```

---

### Task 3: Delegate `PropertyOptionsResolver` to `SecurityContextRehydrator`

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ee/ai/copilot/tool/PropertyOptionsResolver.java`

- [ ] **Step 1: Replace the inlined security logic with the injected rehydrator**

Change the constructor and `withUserSecurityContext`; drop the now-unused imports (`Authority`, `SecurityUtils`, `ArrayList`, `SimpleGrantedAuthority`, `GrantedAuthority`, `Optional` if unused, `User` if unused):

```java
    private final SecurityContextRehydrator securityContextRehydrator;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public PropertyOptionsResolver(SecurityContextRehydrator securityContextRehydrator) {
        this.securityContextRehydrator = securityContextRehydrator;
    }

    public <T> T withUserSecurityContext(@Nullable Long userId, Supplier<T> action) {
        return securityContextRehydrator.withUserSecurityContext(userId, action);
    }
```

- [ ] **Step 2: Build the module and run its tests**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-tool:test`
Expected: PASS. `PropertyOptionsResolver` is now a thin delegate; existing picker behaviour is unchanged because `SecurityContextRehydrator` holds the identical logic.

- [ ] **Step 3: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ee/ai/copilot/tool/PropertyOptionsResolver.java
git commit -m "5184 Delegate PropertyOptionsResolver security rehydration to SecurityContextRehydrator"
```

---

### Task 4: Add `tenant-api` dependency to `ai-copilot-tool`

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-tool/build.gradle.kts`

- [ ] **Step 1: Add the dependency** (alongside the other `implementation(project(...))` lines)

```kotlin
    implementation(project(":server:libs:core:tenant:tenant-api"))
```

- [ ] **Step 2: Verify it resolves**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-tool:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-tool/build.gradle.kts
git commit -m "5184 Add tenant-api dependency to ai-copilot-tool"
```

---

### Task 5: Create `RehydrateContextToolCallback` + `CopilotToolCallbackWrappers`

**Files:**
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ee/ai/copilot/tool/RehydrateContextToolCallback.java`
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ee/ai/copilot/tool/CopilotToolCallbackWrappers.java`
- Test: `server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/test/java/com/bytechef/ee/ai/copilot/tool/RehydrateContextToolCallbackTest.java`

- [ ] **Step 1: Write the failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.tenant.TenantContext;
import java.util.Map;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @version ee
 */
class RehydrateContextToolCallbackTest {

    /** A SecurityContextRehydrator whose withUserSecurityContext just records the userId and runs the action. */
    private static final class RecordingRehydrator extends SecurityContextRehydrator {

        private Long observedUserId;

        private RecordingRehydrator() {
            super(null, null);
        }

        @Override
        public <T> T withUserSecurityContext(@Nullable Long userId, Supplier<T> action) {
            observedUserId = userId;

            return action.get();
        }
    }

    private static final class ProbeToolCallback implements ToolCallback {

        private String tenantSeenInside;

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name("probe")
                .description("probe")
                .inputSchema("{\"type\":\"object\"}")
                .build();
        }

        @Override
        public String call(String toolInput) {
            return call(toolInput, null);
        }

        @Override
        public String call(String toolInput, @Nullable ToolContext toolContext) {
            tenantSeenInside = TenantContext.getCurrentTenantId();

            return "ok";
        }
    }

    @Test
    void testSetsTenantAndUserInsideCallThenRestores() {
        TenantContext.setCurrentTenantId(TenantContext.DEFAULT_TENANT_ID);

        ProbeToolCallback probe = new ProbeToolCallback();
        RecordingRehydrator rehydrator = new RecordingRehydrator();

        ToolCallback wrapped = RehydrateContextToolCallback.wrap(probe, rehydrator);

        Map<String, Object> map = new AgentToolInvocationContext(null, 42L, null, null, "acme").toToolContext();

        String result = wrapped.call("{}", new ToolContext(map));

        assertThat(result).isEqualTo("ok");
        assertThat(probe.tenantSeenInside).isEqualTo("acme");
        assertThat(rehydrator.observedUserId).isEqualTo(42L);
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testIdempotentWrap() {
        RecordingRehydrator rehydrator = new RecordingRehydrator();
        ToolCallback wrapped = RehydrateContextToolCallback.wrap(new ProbeToolCallback(), rehydrator);

        assertThat(RehydrateContextToolCallback.wrap(wrapped, rehydrator)).isSameAs(wrapped);
    }

    @Test
    void testNoToolContextPassesThrough() {
        ToolCallback wrapped = RehydrateContextToolCallback.wrap(new ProbeToolCallback(), new RecordingRehydrator());

        assertThat(wrapped.call("{}")).isEqualTo("ok");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-tool:test --tests '*RehydrateContextToolCallbackTest'`
Expected: COMPILE FAILURE — `RehydrateContextToolCallback` does not exist.

- [ ] **Step 3: Create `RehydrateContextToolCallback`** (inline tenant save/restore — deliberately not `TenantContext.callWithTenantId`, which logs an error and double-wraps on any tool exception; tool errors are normal control flow and must propagate cleanly)

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Decorator that rehydrates the request's tenant and Spring Security principal from the {@link
 * AgentToolInvocationContext} carried on the Spring AI {@link ToolContext}, before delegating. Spring AI dispatches tool
 * calls on Reactor scheduler / worker threads that do not inherit the request thread's {@code TenantContext} or
 * SecurityContext thread-locals, so any tool that reads tenant-scoped data or hits a {@code @PreAuthorize} method would
 * otherwise see the {@code public} tenant and no principal. Each half is independent: a missing tenant id leaves the
 * tenant unchanged, a missing user id skips the security rehydration.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class RehydrateContextToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final SecurityContextRehydrator securityContextRehydrator;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    private RehydrateContextToolCallback(ToolCallback delegate, SecurityContextRehydrator securityContextRehydrator) {
        this.delegate = delegate;
        this.securityContextRehydrator = securityContextRehydrator;
    }

    /**
     * Wraps {@code delegate} so each {@link #call(String, ToolContext)} runs under the invocation's tenant + principal.
     * Idempotent — re-wrapping a callback already wrapped by this class returns it unchanged.
     */
    public static ToolCallback wrap(ToolCallback delegate, SecurityContextRehydrator securityContextRehydrator) {
        if (delegate instanceof RehydrateContextToolCallback) {
            return delegate;
        }

        return new RehydrateContextToolCallback(delegate, securityContextRehydrator);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        // No ToolContext means no AG-UI invocation identity to rehydrate from. Pass straight through so the wrapper is
        // transparent for non-AG-UI call sites (e.g. tests that exercise call(String) directly).
        return delegate.call(toolInput);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        AgentToolInvocationContext invocationContext = AgentToolInvocationContext.fromToolContext(toolContext);

        if (invocationContext == null) {
            return delegate.call(toolInput, toolContext);
        }

        String tenantId = invocationContext.tenantId();
        Long userId = invocationContext.userId();

        if (tenantId == null) {
            return securityContextRehydrator.withUserSecurityContext(
                userId, () -> delegate.call(toolInput, toolContext));
        }

        String previousTenantId = TenantContext.getCurrentTenantId();

        try {
            TenantContext.setCurrentTenantId(tenantId);

            return securityContextRehydrator.withUserSecurityContext(
                userId, () -> delegate.call(toolInput, toolContext));
        } finally {
            TenantContext.setCurrentTenantId(previousTenantId);
        }
    }
}
```

- [ ] **Step 4: Create `CopilotToolCallbackWrappers`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

import org.springframework.ai.tool.ToolCallback;

/**
 * Centralised registration-time wrapping for Copilot agent tool callbacks. Applies {@link
 * RehydrateContextToolCallback} so every tenant- or {@code @PreAuthorize}-scoped facade call runs under the invoking
 * tenant + principal on Reactor scheduler threads. Wrapping at registration covers every current and future tool the
 * agent gains, rather than patching call sites one at a time.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class CopilotToolCallbackWrappers {

    private CopilotToolCallbackWrappers() {
    }

    public static ToolCallback wrap(ToolCallback callback, SecurityContextRehydrator securityContextRehydrator) {
        return RehydrateContextToolCallback.wrap(callback, securityContextRehydrator);
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-tool:test --tests '*RehydrateContextToolCallbackTest'`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ee/ai/copilot/tool/RehydrateContextToolCallback.java server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ee/ai/copilot/tool/CopilotToolCallbackWrappers.java server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/test/java/com/bytechef/ee/ai/copilot/tool/RehydrateContextToolCallbackTest.java
git commit -m "5184 Add shared RehydrateContextToolCallback (tenant + security rehydration)"
```

---

### Task 6: Carry `tenantId` from state into the tool context

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/util/CopilotStateKeys.java`
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/util/CopilotToolContextUtils.java`

- [ ] **Step 1: Add the state key** in `CopilotStateKeys.java` (inside the class, after `STATE_AUTHENTICATED_USER_ID`)

```java
    /**
     * Run-state key under which the controller / generator injects the request's tenant id (captured on the request
     * thread). Drives tenant rehydration in the shared tool wrapper so tools persist to the correct schema on worker
     * threads. Server-populated, never client-supplied.
     */
    public static final String STATE_TENANT_ID = "bytechef.copilot.tenantId";
```

- [ ] **Step 2: Read it in `CopilotToolContextUtils.toToolContext`** — change the `AgentToolInvocationContext` construction to pass `tenantId` (note the new 5th argument; `conversationId` stays `null`)

```java
        Long workspaceId = asLong(state.get("workspaceId"));
        Long userId = asLong(state.get(CopilotStateKeys.STATE_AUTHENTICATED_USER_ID));
        Long environmentId = asLong(state.get("environmentId"));
        String tenantId = asString(state.get(CopilotStateKeys.STATE_TENANT_ID));

        toolContext.putAll(
            new AgentToolInvocationContext(workspaceId, userId, environmentId, null, tenantId).toToolContext());
```

Add the helper (next to `asLong`):

```java
    private static @Nullable String asString(@Nullable Object value) {
        return value instanceof String string && !string.isBlank() ? string : null;
    }
```

- [ ] **Step 3: Build both modules**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-api:compileJava :server:ee:libs:ai:ai-copilot:ai-copilot-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/util/CopilotStateKeys.java server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/util/CopilotToolContextUtils.java
git commit -m "5184 Carry tenantId from run state into copilot tool context"
```

---

### Task 7: Capture tenant in `CopilotApiController` (interactive path)

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-rest/build.gradle.kts`
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-rest/src/main/java/com/bytechef/ee/ai/copilot/web/rest/CopilotApiController.java`

- [ ] **Step 1: Add the `tenant-api` dependency** in `ai-copilot-rest/build.gradle.kts`

```kotlin
    implementation(project(":server:libs:core:tenant:tenant-api"))
```

- [ ] **Step 2: Inject the tenant into state.** In `CopilotApiController`, add the import and a call after `injectAuthenticatedUserId(stateMap);`

Import:
```java
import com.bytechef.tenant.TenantContext;
```

In `chat(...)`, after the existing `injectAuthenticatedUserId(stateMap);`:
```java
        stateMap.put(CopilotStateKeys.STATE_TENANT_ID, TenantContext.getCurrentTenantId());
```

- [ ] **Step 3: Build the module**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-rest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/ai/ai-copilot/ai-copilot-rest/build.gradle.kts server/ee/libs/ai/ai-copilot/ai-copilot-rest/src/main/java/com/bytechef/ee/ai/copilot/web/rest/CopilotApiController.java
git commit -m "5184 Capture tenant id into copilot run state on the request thread"
```

---

### Task 8: Wrap the `workflow_editor_build` agent tools; drop the store from the tool list

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/config/CopilotConfiguration.java`

**Context:** the `workflowEditorBuildSpringAIAgent` bean currently does `.tools(List.of(projectTools, projectWorkflowTools, componentTools, taskTools, scriptTools, workflowValidatorTools, workflowInstructionTools, workflowGenerationSubmissionStore))`. We convert those `@Tool` beans to `ToolCallback`s, wrap each with `CopilotToolCallbackWrappers.wrap(..., securityContextRehydrator)`, pass them via `.toolCallbacks(...)`, and remove `workflowGenerationSubmissionStore`.

- [ ] **Step 1: Add imports** to `CopilotConfiguration.java`

```java
import com.bytechef.ee.ai.copilot.tool.CopilotToolCallbackWrappers;
import com.bytechef.ee.ai.copilot.tool.SecurityContextRehydrator;
import java.util.ArrayList;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
```

- [ ] **Step 2: Add `SecurityContextRehydrator` to the `workflowEditorBuildSpringAIAgent(...)` bean method parameters** and remove the `WorkflowGenerationSubmissionStore workflowGenerationSubmissionStore` parameter.

- [ ] **Step 3: Replace the `.tools(List.of(...))` call** in that bean with a wrapped-callback build:

```java
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (Object tool : List.of(
            projectTools, projectWorkflowTools, componentTools, taskTools, scriptTools, workflowValidatorTools,
            workflowInstructionTools)) {

            for (ToolCallback toolCallback : ToolCallbacks.from(tool)) {
                toolCallbacks.add(CopilotToolCallbackWrappers.wrap(toolCallback, securityContextRehydrator));
            }
        }

        return WorkflowEditorSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowEditorBuildResource))
            .state(state)
            .toolCallbacks(toolCallbacks)
            .workflowService(workflowService)
            .workflowNodeOutputFacade(workflowNodeOutputFacade)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
```

(Keep any builder lines already present that are not shown here; only `.tools(...)` becomes `.toolCallbacks(toolCallbacks)` and the loop is inserted above the `return`.)

- [ ] **Step 4: Build the module**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:compileJava`
Expected: BUILD SUCCESSFUL. (`SecurityContextRehydrator` is a `@Component` in `ai-copilot-tool`, on which `ai-copilot-service` already depends, so Spring injects it.)

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/config/CopilotConfiguration.java
git commit -m "5184 Wrap workflow_editor_build tools with tenant+security rehydration; drop submission store"
```

---

### Task 9: Remove the persistence prohibitions from the build prompt

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_workflow_editor_build.txt`

- [ ] **Step 1: Edit the "Autonomous Generation Mode" section.** Keep the behavioural bullets (never ask follow-ups / never wait for confirmation / run straight through) and the "keep the existing label" bullet. Remove the bullet that forbids `updateWorkflow`/`getWorkflow`/`createProject`/`createProjectWorkflow`/`listProjects`/`searchProjects` and the bullet instructing `submitWorkflow(...)`. Replace the persistence instruction so both modes end the same way:

Old (delete these two bullets):
```
- Do NOT call `updateWorkflow`, `getWorkflow`, `createProject`, `createProjectWorkflow`, `listProjects`,
  or `searchProjects`. These DO NOT WORK in this context (no tenant/security context on the worker thread)
  and will fail. The workflow already exists and will be persisted for you once you submit it.
...
- When the COMPLETE workflow is built and validated, call `submitWorkflow(workflowId, workflow)` EXACTLY ONCE
  with the `workflowId` from state and the complete definition, then end your turn.
```

New (single closing bullet):
```
- When the COMPLETE workflow is built and validated, call `updateWorkflow(workflowId, workflow)` with the
  `workflowId` from state and the complete definition, then end your turn.
```

- [ ] **Step 2: Verify no other reference to `submitWorkflow` remains in the prompt**

Run: `grep -n "submitWorkflow" server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_workflow_editor_build.txt`
Expected: no output.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_workflow_editor_build.txt
git commit -m "5184 Build prompt persists via updateWorkflow in autonomous mode (no submitWorkflow)"
```

---

### Task 10: Seed `{tenantId, userId}` and drop the poll/persist tail in `CopilotWorkflowGeneratorImpl`

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/service/CopilotWorkflowGeneratorImpl.java`
- Test: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/service/CopilotWorkflowGeneratorImplTest.java`

- [ ] **Step 1: Write the failing test** (verifies the state carries tenant + user, and that no store/persist collaborators are required). Capture the `State` passed to `runAgent` via a stub `LocalAgent`.

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentParameters;
import com.agui.server.LocalAgent;
import com.bytechef.ee.ai.copilot.util.CopilotStateKeys;
import com.bytechef.tenant.TenantContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class CopilotWorkflowGeneratorImplTest {

    @Test
    void testSeedsTenantIntoState() {
        AtomicReference<Map<String, Object>> capturedState = new AtomicReference<>();

        LocalAgent stubAgent = new LocalAgent("workflow_editor_build") {

            @Override
            public java.util.concurrent.CompletableFuture<Void> runAgent(
                RunAgentParameters parameters, AgentSubscriber subscriber) {

                capturedState.set(parameters.getState()
                    .getState());

                subscriber.onRunFinalized(new AgentSubscriberParams(null, null, null, null));

                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }
        };

        CopilotWorkflowGeneratorImpl generator = new CopilotWorkflowGeneratorImpl(List.of(stubAgent));

        TenantContext.runWithTenantId("acme", () -> generator.generateWorkflow("wf-1", "build a thing", Set.of()));

        assertThat(capturedState.get()).containsEntry(CopilotStateKeys.STATE_TENANT_ID, "acme");
    }
}
```

(If `LocalAgent` is `final` or its constructor differs, substitute a Mockito `mock(LocalAgent.class)` with `when(getAgentId()).thenReturn("workflow_editor_build")` and an `Answer` on `runAgent` that captures the state and invokes `onRunFinalized`.)

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests '*CopilotWorkflowGeneratorImplTest'`
Expected: COMPILE FAILURE — the constructor still requires `WorkflowGenerationSubmissionStore` + `WorkflowService`, and `STATE_TENANT_ID` is not seeded.

- [ ] **Step 3: Simplify the implementation.** Replace the constructor and `generateWorkflow` so it (a) seeds tenant + user into state, (b) drops the store + workflowService fields and the poll/persist tail. New constructor:

```java
    private final Map<String, LocalAgent> localAgentMap;

    @SuppressFBWarnings("EI")
    public CopilotWorkflowGeneratorImpl(List<LocalAgent> localAgents) {
        this.localAgentMap = localAgents.stream()
            .collect(Collectors.toMap(LocalAgent::getAgentId, localAgent -> localAgent));
    }
```

In `generateWorkflow`, after `stateMap.put("autonomous", true);` add the identity capture:

```java
        stateMap.put(CopilotStateKeys.STATE_TENANT_ID, TenantContext.getCurrentTenantId());

        SecurityUtils.fetchCurrentUserId()
            .ifPresent(userId -> stateMap.put(CopilotStateKeys.STATE_AUTHENTICATED_USER_ID, userId));
```

Delete everything from the `// The agent builds the workflow on reactor threads...` comment through the final `workflowService.update(...)` (the entire poll/persist tail). The method now ends right after the `error` re-throw block.

Remove the now-unused imports: `com.bytechef.atlas.configuration.domain.Workflow`, `com.bytechef.atlas.configuration.service.WorkflowService`, `com.bytechef.commons.util.JsonUtils`, `java.util.LinkedHashMap`. Add imports: `com.bytechef.ee.ai.copilot.util.CopilotStateKeys`, `com.bytechef.platform.security.util.SecurityUtils`, `com.bytechef.tenant.TenantContext`.

> **Verify during implementation:** confirm `SecurityUtils.fetchCurrentUserId()` exists and returns `Optional<Long>`. If the available accessor is `fetchCurrentUserLogin()` only, resolve the id the same way `CopilotApiController.injectAuthenticatedUserId` does (inject `UserService`, `fetchCurrentUserLogin().flatMap(userService::fetchUserByLogin).map(User::getId)`). For the embedded autonomous path the current user is typically absent, so this is expected to be empty — that is fine, tenant alone is sufficient there.

- [ ] **Step 4: Add the `tenant-api` dependency** to `server/ee/libs/ai/ai-copilot/ai-copilot-service/build.gradle.kts`

```kotlin
    implementation(project(":server:libs:core:tenant:tenant-api"))
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests '*CopilotWorkflowGeneratorImplTest'`
Expected: PASS.

- [ ] **Step 6: Update the CopilotConfiguration call site if needed.** `CopilotWorkflowGeneratorImpl` is `@Service` (constructor-injected), so removing constructor params requires no config change. Build the module:

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/service/CopilotWorkflowGeneratorImpl.java server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/service/CopilotWorkflowGeneratorImplTest.java server/ee/libs/ai/ai-copilot/ai-copilot-service/build.gradle.kts
git commit -m "5184 Seed tenant+user into generation state; remove poll/persist tail"
```

---

### Task 11: Delete `WorkflowGenerationSubmissionStore`

**Files:**
- Delete: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/service/WorkflowGenerationSubmissionStore.java`

- [ ] **Step 1: Confirm no remaining references**

Run: `grep -rn "WorkflowGenerationSubmissionStore\|submitWorkflow" server/ee/libs/ai/ai-copilot --include='*.java'`
Expected: no output (Task 8 removed it from the tool list, Task 10 removed the poll). If anything remains, remove it before deleting.

- [ ] **Step 2: Delete the file**

```bash
git rm server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/service/WorkflowGenerationSubmissionStore.java
```

- [ ] **Step 3: Build the module**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git commit -m "5184 Delete WorkflowGenerationSubmissionStore (agent persists inline now)"
```

---

### Task 12: Phase 1 integration check

- [ ] **Step 1: Build + test the whole copilot tree**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-tool:test :server:ee:libs:ai:ai-copilot:ai-copilot-service:test`
Expected: PASS.

- [ ] **Step 2: Add a tool-path integration test** that proves persistence honours the carried tenant. In `ai-copilot-tool` (which already has `automation-configuration-service` + `platform-user-service` test deps), wrap a real persistence-style delegate and assert the tenant seen inside `call` equals the carried tenant. Reuse the `RehydrateContextToolCallbackTest.ProbeToolCallback` pattern but assert it against a non-default tenant value, confirming the wrapper sets and restores `TenantContext` around the delegate. (This is the correctness win without a live LLM.)

- [ ] **Step 3: Commit any test added**

```bash
./gradlew spotlessApply
git add -A
git commit -m "5184 Add tenant-rehydration tool-path integration test"
```

---

## PHASE 2 — AI Hub migration onto the shared wrapper

### Task 13: Capture tenant in AI Hub and carry it on `AgentToolInvocationContext`

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-rest/src/main/java/com/bytechef/ee/ai/hub/web/rest/AiHubApiController.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/agent/AiHubSpringAIAgent.java`
- (Verify) `server/ee/libs/ai/ai-hub/ai-hub-api/.../AiHubStateKeys.java` for a tenant key constant.

- [ ] **Step 1: Add a verified tenant state key.** In `AiHubStateKeys` add (mirroring `AUTHENTICATED_USER_ID`):

```java
    public static final String VERIFIED_TENANT_ID = "bytechef.aiHub.verifiedTenantId";
```

- [ ] **Step 2: Capture the tenant in `AiHubApiController`** where it already writes the verified workspace/user/thread keys into state. Add the import `com.bytechef.tenant.TenantContext;` and, alongside those writes:

```java
        stateMap.put(AiHubStateKeys.VERIFIED_TENANT_ID, TenantContext.getCurrentTenantId());
```

(Verify `ai-hub-rest/build.gradle.kts` depends on `:server:libs:core:tenant:tenant-api`; add it if missing.)

- [ ] **Step 3: Pass the tenant into `AgentToolInvocationContext`** in `AiHubSpringAIAgent.toolContext` (currently lines 161-164). Read the tenant from state and pass it as the new 5th arg:

```java
    @Override
    protected Map<String, Object> toolContext(RunAgentInput input) {
        AiHubToolInvocationContext aiHubContext = buildInvocationContext(input);

        String tenantId = input.state() == null
            ? null : asString(input.state()
                .get(AiHubStateKeys.VERIFIED_TENANT_ID));

        Map<String, Object> toolContext = new HashMap<>(aiHubContext.toToolContext());

        toolContext.putAll(
            new AgentToolInvocationContext(
                aiHubContext.workspaceId(), aiHubContext.userId(), aiHubContext.environmentId(),
                aiHubContext.threadId(), tenantId).toToolContext());

        return toolContext;
    }
```

- [ ] **Step 4: Build the two modules**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-rest:compileJava :server:ee:libs:ai:ai-hub:ai-hub-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/ai/ai-hub/ai-hub-api server/ee/libs/ai/ai-hub/ai-hub-rest server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/agent/AiHubSpringAIAgent.java
git commit -m "5184 AI Hub carries tenant id on AgentToolInvocationContext"
```

---

### Task 14: Point `AiHubToolCallbackWrappers` at the shared `RehydrateContextToolCallback`

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/agent/AiHubToolCallbackWrappers.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/agent/AiHubSpringAIAgent.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/toolsearch/ToolSearchAdvisorConfiguration.java`

**Context:** the wrapper is called from three sites, all routing through `AiHubToolCallbackWrappers.wrap(callback, userService, authorityService)` (AiHubSpringAIAgent:234, AiHubSpringAIAgent's builder path, ToolSearchAdvisorConfiguration:196). Change the wrapper to take a `SecurityContextRehydrator` and apply the shared callback; update the call sites to pass an injected `SecurityContextRehydrator` instead of `userService`/`authorityService`.

- [ ] **Step 1: Rewrite `AiHubToolCallbackWrappers`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.bytechef.ee.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ee.ai.copilot.tool.SecurityContextRehydrator;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;

/**
 * Shared wrapping for AI Hub tool callbacks: (1) {@link NonEmptyToolCallback} so an empty tool result does not trigger
 * an Anthropic "non-empty content" HTTP 400, then (2) the shared {@link RehydrateContextToolCallback} so
 * tenant-scoped and {@code @PreAuthorize}-protected service calls run under the invoking tenant + principal on Reactor
 * scheduler threads. When the rehydrator is absent the context layer is skipped.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiHubToolCallbackWrappers {

    private AiHubToolCallbackWrappers() {
    }

    public static ToolCallback wrap(
        ToolCallback callback, @Nullable SecurityContextRehydrator securityContextRehydrator) {

        ToolCallback nonEmpty = NonEmptyToolCallback.wrap(callback);

        if (securityContextRehydrator == null) {
            return nonEmpty;
        }

        return RehydrateContextToolCallback.wrap(nonEmpty, securityContextRehydrator);
    }
}
```

- [ ] **Step 2: Update `AiHubSpringAIAgent`** — inject `SecurityContextRehydrator` (replace the `userService`/`authorityService` fields *only where they were used solely for wrapping*; keep them if used elsewhere) and change `wrapToolCallback`:

```java
    ToolCallback wrapToolCallback(ToolCallback callback) {
        return AiHubToolCallbackWrappers.wrap(callback, securityContextRehydrator);
    }
```

Thread `securityContextRehydrator` through the agent's builder/constructor the same way `userService` was. (Verify whether `userService`/`authorityService` remain referenced elsewhere in the class via `grep -n "userService\|authorityService" AiHubSpringAIAgent.java`; keep the fields if still used, drop them if now dead.)

- [ ] **Step 3: Update `ToolSearchAdvisorConfiguration:196`** to pass the injected `securityContextRehydrator`:

```java
                callbackList.add(AiHubToolCallbackWrappers.wrap(toolCallback, securityContextRehydrator));
```

Inject `SecurityContextRehydrator` into that configuration where `userService`/`authorityService` were obtained.

- [ ] **Step 4: Build the module**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/ai/ai-hub/ai-hub-service
git commit -m "5184 AI Hub uses shared RehydrateContextToolCallback (tenant + security)"
```

---

### Task 15: Delete `RehydrateSecurityContextToolCallback`

**Files:**
- Delete: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/agent/RehydrateSecurityContextToolCallback.java`
- Delete/repoint: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/agent/RehydrateSecurityContextToolCallbackTest.java`

- [ ] **Step 1: Confirm no remaining references**

Run: `grep -rn "RehydrateSecurityContextToolCallback" server/ee/libs/ai/ai-hub --include='*.java'`
Expected: only the class file + its test. If the test asserts behaviour now covered by `RehydrateContextToolCallbackTest`, delete it; if it asserts AI-Hub-specific wrapping order, move those assertions to an `AiHubToolCallbackWrappersTest`.

- [ ] **Step 2: Delete**

```bash
git rm server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/agent/RehydrateSecurityContextToolCallback.java
git rm server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/agent/RehydrateSecurityContextToolCallbackTest.java
```

- [ ] **Step 3: Build + test**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git commit -m "5184 Delete ai-hub RehydrateSecurityContextToolCallback (replaced by shared wrapper)"
```

---

### Task 16: Full verification

- [ ] **Step 1: Compile everything**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:compileJava :server:ee:libs:ai:ai-hub:ai-hub-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Run the AI subtree tests**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-tool:test :server:ee:libs:ai:ai-copilot:ai-copilot-service:test :server:ee:libs:ai:ai-hub:ai-hub-service:test`
Expected: PASS.

- [ ] **Step 3: Run static checks on the touched modules**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-tool:check :server:ee:libs:ai:ai-copilot:ai-copilot-service:check :server:ee:libs:ai:ai-hub:ai-hub-service:check`
Expected: BUILD SUCCESSFUL (Spotless/Checkstyle/PMD/SpotBugs clean).

---

## Self-Review notes

- **Spec coverage:** shared wrapper (Tasks 2,5), `tenantId` on context (Task 1), tenant capture interactive (Task 7) + autonomous (Task 10) + ai-hub (Task 13), wrap at registration (Task 8), prompt decoupling of `autonomous` (Task 9), store deletion (Tasks 10–11), ai-hub migration + delete (Tasks 13–15), non-EE `ProjectWorkflowTools` untouched (no task modifies it). All spec sections map to a task.
- **Type consistency:** `AgentToolInvocationContext` canonical constructor becomes 5-arg `(workspaceId, userId, environmentId, conversationId, tenantId)` — used identically in Task 6 (copilot) and Task 13 (ai-hub). `SecurityContextRehydrator.withUserSecurityContext(Long, Supplier<T>)` is the single signature used by `PropertyOptionsResolver` (Task 3) and `RehydrateContextToolCallback` (Task 5). `CopilotToolCallbackWrappers.wrap(ToolCallback, SecurityContextRehydrator)` and `AiHubToolCallbackWrappers.wrap(ToolCallback, SecurityContextRehydrator)` share the same shape.
- **Open verifications flagged inline:** `SecurityUtils.fetchCurrentUserId()` availability (Task 10 Step 3), `LocalAgent` mockability (Task 10 Step 1), residual `userService`/`authorityService` usage in `AiHubSpringAIAgent` (Task 14 Step 2), `AiHubStateKeys` + ai-hub-rest tenant dep (Task 13).
