# Centralize connection/property tools in `ai-copilot-tool` (server) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.
>
> **Git note:** the user commits to `0_732` in parallel. NEVER `git commit --amend` — HEAD may be theirs. Always fresh commits. Stage only each task's files.

**Goal:** Relocate the connection/property tools + `PropertyOptionsResolver` from `ai-hub-service` into the shared `ai-copilot-tool` module behind neutral abstractions, with AI Hub consuming them and **no behavior change**, so a later spec can give the Copilot panel the same tools.

**Architecture:** Introduce a neutral `AgentToolInvocationContext` + `ToolStateVisibilityMetrics` in `ai-copilot-tool` (which `ai-hub-service` already depends on). Move the 6 tool callbacks + resolver there, swapping `AiHubToolInvocationContext`→neutral and `AiHubToolAttachMetrics`→the neutral interface. AI Hub imports them from the new package, merges the neutral context keys into its agent tool-context, and deletes the old copies. The existing AI Hub tests are the behavior guard.

**Tech Stack:** Java 25, Spring Boot 4, Spring AI, Jackson 3 (`tools.jackson`), JUnit 5 + Mockito + AssertJ. EE module → Enterprise license header + `@version ee` on every file.

---

## Background facts (verified)

- Dependency direction: `ai-hub-service` → depends on → `ai-copilot-tool` + `ai-copilot-api`. `ai-copilot-service` → depends on → `ai-copilot-tool`. `ai-copilot-tool` → `ai-api`, `ai-copilot-api`, spring-ai, jackson. **Neither copilot module depends on ai-hub.**
- `ai-copilot-tool` has spring-ai (`ToolContext` available) and `tools.jackson`. `ai-copilot-api` does **not** have spring-ai — so the neutral context lives in **`ai-copilot-tool`** (refinement of the spec, which said api).
- `ai-copilot-tool` build currently lacks platform/automation deps; the moved tools need `platform-component-api`, `platform-connection-api`, `platform-user-api`, `platform-security-api`, `automation-configuration-api`.
- `AiHubToolAttachMetrics` (`com.bytechef.ee.ai.hub.metric`) already has `public void recordStateVisibility(String tool, String outcome)` — matches the neutral interface; just add `implements`.
- `AiHubToolInvocationContext` (`ai-hub-api`, record `workspaceId, userId, sourceOrdinal, lastUserPrompt, environmentId, threadId`) is built in `AiHubSpringAIAgent.buildInvocationContext` (~line 241) and serialized at `AiHubSpringAIAgent.toolContext` (line 154-156: `return buildInvocationContext(input).toToolContext();`).
- The tools' context usage: `ListConnectionsForComponentToolCallback` uses `workspaceId`, `userId`, and `AiHubToolInvocationContext.resolveEnvironmentOrDefault(ctx)` (static). The lookup/select callbacks use `workspaceId` (null-check), `userId` (via `resolver.withUserSecurityContext(ctx.userId(), ...)`). `SelectConnectionToolCallback` uses no context (just `componentName` + `ComponentDefinitionService`). `PropertyOptionsResolver.withUserSecurityContext(@Nullable Long userId, Supplier)` takes the raw userId.
- Tools to move (all in `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/`): `PropertyOptionsResolver`, `ListConnectionsForComponentToolCallback`, `SelectConnectionToolCallback`, `LookupActionPropertyOptionsToolCallback`, `LookupTriggerPropertyOptionsToolCallback`, `SelectPropertyOptionToolCallback`, `SelectTriggerPropertyOptionToolCallback`. Their tests are in `.../src/test/java/.../tool/`.
- These tools currently also reference `AiHubToolInvocationContext` from `com.bytechef.ee.ai.hub.tool` (same package, no import). After the move they import the neutral context.

Run module checks:
```bash
./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-tool:test
./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test
```

## File Structure

- Create: `ai-copilot-tool/.../copilot/tool/AgentToolInvocationContext.java` + test.
- Create: `ai-copilot-tool/.../copilot/tool/ToolStateVisibilityMetrics.java`.
- Modify: `ai-copilot-tool/build.gradle.kts` (add platform/automation deps).
- Modify: `ai-hub-service/.../metric/AiHubToolAttachMetrics.java` (implements interface).
- Move (→ `ai-copilot-tool/.../copilot/tool/`, package `com.bytechef.ee.ai.copilot.tool`): the 7 classes + their tests.
- Modify: `ai-hub-service/.../config/AiHubConfiguration.java` (import swap), `.../agent/AiHubSpringAIAgent.java` (merge neutral keys).
- Delete: the 7 old classes + tests from `ai-hub-service`.

`ai-copilot-tool` package base: `com.bytechef.ee.ai.copilot.tool` (existing — e.g. `WorkflowEditorAgentToolCallback` lives there).

---

## Task 1: Neutral `AgentToolInvocationContext` + `ToolStateVisibilityMetrics`

**Files:**
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ee/ai/copilot/tool/AgentToolInvocationContext.java`
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ee/ai/copilot/tool/ToolStateVisibilityMetrics.java`
- Test: `server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/test/java/com/bytechef/ee/ai/copilot/tool/AgentToolInvocationContextTest.java`

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

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AgentToolInvocationContextTest {

    @Test
    void testRoundTripThroughToolContext() {
        AgentToolInvocationContext context = new AgentToolInvocationContext(1L, 10L, 0L, "thread-1");

        AgentToolInvocationContext restored =
            AgentToolInvocationContext.fromToolContext(new ToolContext(context.toToolContext()));

        assertThat(restored).isNotNull();
        assertThat(restored.workspaceId()).isEqualTo(1L);
        assertThat(restored.userId()).isEqualTo(10L);
        assertThat(restored.environmentId()).isEqualTo(0L);
        assertThat(restored.conversationId()).isEqualTo("thread-1");
    }

    @Test
    void testFromToolContextReturnsNullWhenNoKeys() {
        assertThat(AgentToolInvocationContext.fromToolContext(new ToolContext(Map.of("unrelated", "x")))).isNull();
        assertThat(AgentToolInvocationContext.fromToolContext(null)).isNull();
    }

    @Test
    void testResolveEnvironmentOrDefault() {
        assertThat(new AgentToolInvocationContext(1L, 10L, 3L, null).resolveEnvironmentOrDefault()).isEqualTo(3);
        assertThat(new AgentToolInvocationContext(1L, 10L, null, null).resolveEnvironmentOrDefault()).isEqualTo(0);
    }

    @Test
    void testToToolContextOmitsNullFields() {
        Map<String, Object> map = new AgentToolInvocationContext(1L, null, null, null).toToolContext();

        assertThat(map).containsOnlyKeys(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY);
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-tool:test --tests 'com.bytechef.ee.ai.copilot.tool.AgentToolInvocationContextTest'`
Expected: COMPILE FAILURE — `AgentToolInvocationContext` doesn't exist.

- [ ] **Step 3: Create the two types**

`AgentToolInvocationContext.java`:
```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;

/**
 * Surface-neutral invocation context for shared component-interaction tool callbacks (connection / property options).
 * Carries the workspace, user, and environment a tool needs, plus an optional conversation id (used by a later
 * server-side artifact-attach revision). Both AI Hub and the Copilot panel populate it into their agent
 * {@code ToolContext}; the shared tools read only this type, so they don't depend on any one surface.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AgentToolInvocationContext(
    @Nullable Long workspaceId, @Nullable Long userId, @Nullable Long environmentId,
    @Nullable String conversationId) {

    public static final String TOOL_CONTEXT_WORKSPACE_ID_KEY = "bytechef.agentTool.workspaceId";
    public static final String TOOL_CONTEXT_USER_ID_KEY = "bytechef.agentTool.userId";
    public static final String TOOL_CONTEXT_ENVIRONMENT_ID_KEY = "bytechef.agentTool.environmentId";
    public static final String TOOL_CONTEXT_CONVERSATION_ID_KEY = "bytechef.agentTool.conversationId";

    public static @Nullable AgentToolInvocationContext fromToolContext(@Nullable ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }

        Map<String, Object> map = toolContext.getContext();

        if (map == null || map.isEmpty()) {
            return null;
        }

        Long workspaceId = asLong(map.get(TOOL_CONTEXT_WORKSPACE_ID_KEY));
        Long userId = asLong(map.get(TOOL_CONTEXT_USER_ID_KEY));
        Long environmentId = asLong(map.get(TOOL_CONTEXT_ENVIRONMENT_ID_KEY));
        String conversationId = asString(map.get(TOOL_CONTEXT_CONVERSATION_ID_KEY));

        if (workspaceId == null && userId == null && environmentId == null && conversationId == null) {
            return null;
        }

        return new AgentToolInvocationContext(workspaceId, userId, environmentId, conversationId);
    }

    public int resolveEnvironmentOrDefault() {
        return environmentId == null ? 0 : environmentId.intValue();
    }

    public Map<String, Object> toToolContext() {
        Map<String, Object> map = new HashMap<>();

        if (workspaceId != null) {
            map.put(TOOL_CONTEXT_WORKSPACE_ID_KEY, workspaceId);
        }

        if (userId != null) {
            map.put(TOOL_CONTEXT_USER_ID_KEY, userId);
        }

        if (environmentId != null) {
            map.put(TOOL_CONTEXT_ENVIRONMENT_ID_KEY, environmentId);
        }

        if (conversationId != null) {
            map.put(TOOL_CONTEXT_CONVERSATION_ID_KEY, conversationId);
        }

        return map;
    }

    private static @Nullable Long asLong(@Nullable Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String string && !string.isBlank()) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        return null;
    }

    private static @Nullable String asString(@Nullable Object value) {
        return value instanceof String string ? string : null;
    }
}
```

`ToolStateVisibilityMetrics.java`:
```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

/**
 * Surface-neutral metric sink for shared tool callbacks: records a tool's state-visibility outcome (e.g. "success",
 * "empty", "connection_required"). AI Hub's {@code AiHubToolAttachMetrics} implements it; surfaces without metrics use
 * {@link #NOOP}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ToolStateVisibilityMetrics {

    ToolStateVisibilityMetrics NOOP = (toolName, state) -> {
    };

    void recordStateVisibility(String toolName, String state);
}
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-tool:test --tests 'com.bytechef.ee.ai.copilot.tool.AgentToolInvocationContextTest'`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ee/ai/copilot/tool/AgentToolInvocationContext.java \
        server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ee/ai/copilot/tool/ToolStateVisibilityMetrics.java \
        server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/test/java/com/bytechef/ee/ai/copilot/tool/AgentToolInvocationContextTest.java
git commit -m "0_732 Add neutral AgentToolInvocationContext + ToolStateVisibilityMetrics in ai-copilot-tool"
```

---

## Task 2: Add platform/automation deps to `ai-copilot-tool`

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-tool/build.gradle.kts`

- [ ] **Step 1: Add the implementation + test deps**

In the `dependencies { }` block, add (alongside the existing `implementation(project(...))` lines):
```kotlin
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-connection:platform-connection-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))
    implementation(project(":server:libs:platform:platform-security:platform-security-api"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
```
And add the test deps (mirroring what `ai-hub-service` uses for these tools' tests):
```kotlin
    testImplementation(project(":server:libs:automation:automation-configuration:automation-configuration-service"))
    testImplementation(project(":server:libs:platform:platform-connection:platform-connection-service"))
    testImplementation(project(":server:libs:platform:platform-security:platform-security-service"))
    testImplementation(project(":server:libs:platform:platform-user:platform-user-service"))
```
(If `platform-security-api` does not exist as a module — verify the exact path `SecurityUtils` lives in by checking `ai-hub-service`'s `build.gradle.kts`; `ai-hub-service` line ~95 uses `platform-security-service` as a testImpl, and `SecurityUtils` is in `com.bytechef.platform.security.util`. Use whichever module exports `SecurityUtils` on the `implementation` classpath — if it's `platform-security-api`, use that; otherwise match `ai-hub-service`'s production dep for security.)

- [ ] **Step 2: Verify the module still compiles**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-tool:compileJava`
Expected: BUILD SUCCESSFUL (no consumers of the new deps yet — just confirms the deps resolve).

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-tool/build.gradle.kts
git commit -m "0_732 Add platform/automation deps to ai-copilot-tool for shared component tools"
```

---

## Task 3: `AiHubToolAttachMetrics implements ToolStateVisibilityMetrics`

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/metric/AiHubToolAttachMetrics.java`

- [ ] **Step 1: Add the interface to the class declaration**

Add import:
```java
import com.bytechef.ee.ai.copilot.tool.ToolStateVisibilityMetrics;
```
Change the class declaration from `public class AiHubToolAttachMetrics {` to:
```java
public class AiHubToolAttachMetrics implements ToolStateVisibilityMetrics {
```
The existing `public void recordStateVisibility(String tool, String outcome)` already satisfies the interface — add `@Override` to it. No other change.

- [ ] **Step 2: Compile**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:compileJava`
Expected: BUILD SUCCESSFUL (`ai-hub-service` already depends on `ai-copilot-tool`).

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/metric/AiHubToolAttachMetrics.java
git commit -m "0_732 AiHubToolAttachMetrics implements ToolStateVisibilityMetrics"
```

---

## Task 4: Relocate `PropertyOptionsResolver` + the 6 tool callbacks (+ tests) into `ai-copilot-tool`

This is a mechanical relocation: `git mv` each file, change its `package` to `com.bytechef.ee.ai.copilot.tool`, and swap two symbol families. The moved tests are the behavior guard.

**Files (move main + test for each):** `PropertyOptionsResolver`, `ListConnectionsForComponentToolCallback`, `SelectConnectionToolCallback`, `LookupActionPropertyOptionsToolCallback`, `LookupTriggerPropertyOptionsToolCallback`, `SelectPropertyOptionToolCallback`, `SelectTriggerPropertyOptionToolCallback`.

- [ ] **Step 1: `git mv` the 7 main classes + their test classes**

For each `<Name>` in the list (main has a test except `SelectConnectionToolCallback` — confirm which tests exist with `ls`):
```bash
git mv server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/<Name>.java \
       server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ee/ai/copilot/tool/<Name>.java
git mv server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/<Name>Test.java \
       server/ee/libs/ai/ai-copilot/ai-copilot-tool/src/test/java/com/bytechef/ee/ai/copilot/tool/<Name>Test.java
```

- [ ] **Step 2: In every moved file, change the package + swap symbols**

(a) Change `package com.bytechef.ee.ai.hub.tool;` → `package com.bytechef.ee.ai.copilot.tool;`.

(b) **Invocation context swap** (in `ListConnectionsForComponentToolCallback`, `LookupAction/TriggerPropertyOptionsToolCallback`, `SelectPropertyOption/SelectTriggerPropertyOptionToolCallback`, and their tests):
- Replace `AiHubToolInvocationContext` → `AgentToolInvocationContext` (same package now — no import needed; remove any `import ...AiHubToolInvocationContext;`).
- `AiHubToolInvocationContext.resolveEnvironmentOrDefault(invocationContext)` (static, in `ListConnectionsForComponentToolCallback`) → `invocationContext.resolveEnvironmentOrDefault()` (instance).
- In tests, fixtures that built `new AiHubToolInvocationContext(1L, 10L, (short) 0, "x", 0L, "thread-1")` → `new AgentToolInvocationContext(1L, 10L, 0L, "thread-1")` (4-arg: workspaceId, userId, environmentId, conversationId). `.toToolContext()` stays.

(c) **Metrics swap** (in the lookup/select callbacks + `ListConnectionsForComponentToolCallback`): change the constructor param type `AiHubToolAttachMetrics metrics` → `ToolStateVisibilityMetrics metrics` and the import `com.bytechef.ee.ai.hub.metric.AiHubToolAttachMetrics` → (none; `ToolStateVisibilityMetrics` is same package). In tests, `mock(AiHubToolAttachMetrics.class)` → `ToolStateVisibilityMetrics.NOOP` (or `mock(ToolStateVisibilityMetrics.class)`), dropping the `AiHubToolAttachMetrics` import.

(d) `PropertyOptionsResolver` itself: only the `package` line changes (it already depends only on platform-user `UserService`/`AuthorityService`, `SecurityUtils`, `Option` — all reachable via Task 2 deps). Its test: `package` line + the `AgentToolInvocationContext`/metrics swaps if it references them (it references neither context nor metrics directly — confirm; if it uses `Option` mocks only, just the package line).

(e) `SelectConnectionToolCallback`: uses `ComponentDefinitionService` + `jsonMapper` only — just the `package` line + (if present) any `AiHubToolInvocationContext` usage (it has none). Confirm via compile.

- [ ] **Step 3: Run the moved tests in their new module**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-tool:test`
Expected: PASS — all moved tests green (they exercise the same behavior; only context/metrics types changed). Fix any compile error from a missed symbol swap. If a test needs a service-classpath dep not added in Task 2, add it there.

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-tool server/ee/libs/ai/ai-hub/ai-hub-service
git commit -m "0_732 Relocate connection/property tools + PropertyOptionsResolver to ai-copilot-tool"
```
(The `ai-hub-service` paths here are just the `git mv` deletions; the AI Hub rewire is Task 5. At this point `ai-hub-service` will NOT compile yet — that's expected and fixed in Task 5. Do not run `ai-hub-service` tests until Task 5.)

---

## Task 5: Rewire AI Hub to consume the relocated tools

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubConfiguration.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/agent/AiHubSpringAIAgent.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/config/PropertyOptionsToolWiringTest.java`

- [ ] **Step 1: Swap imports in `AiHubConfiguration.java`**

Change the imports for the 6 tool callbacks + `PropertyOptionsResolver` from `com.bytechef.ee.ai.hub.tool.*` to `com.bytechef.ee.ai.copilot.tool.*`. The `new XxxToolCallback(...)` constructions are otherwise unchanged — they still pass `aiHubToolAttachMetrics` (which now implements `ToolStateVisibilityMetrics`) and `propertyOptionsResolver` (now the relocated bean). NOTE: `PropertyOptionsResolver` is a `@Component`; after moving to `com.bytechef.ee.ai.copilot.tool`, confirm AI Hub's component scan still picks it up — `ai-hub-service`'s `@SpringBootApplication`/config scans `com.bytechef`, so it will, but verify the bean resolves at Task 6's `check`. If the relocated `PropertyOptionsResolver` is no longer component-scanned by the AI Hub app, add it as an explicit `@Bean` in `AiHubConfiguration` (constructed with `UserService` + `AuthorityService`).

- [ ] **Step 2: Merge the neutral context keys in `AiHubSpringAIAgent.toolContext`**

Add import `import com.bytechef.ee.ai.copilot.tool.AgentToolInvocationContext;` and `import java.util.HashMap;` (if not present). Replace the method body at lines 153-156:
```java
    @Override
    protected Map<String, Object> toolContext(RunAgentInput input) {
        AiHubToolInvocationContext aiHubContext = buildInvocationContext(input);

        Map<String, Object> toolContext = new HashMap<>(aiHubContext.toToolContext());

        toolContext.putAll(
            new AgentToolInvocationContext(
                aiHubContext.workspaceId(), aiHubContext.userId(), aiHubContext.environmentId(),
                aiHubContext.threadId()).toToolContext());

        return toolContext;
    }
```
This keeps the existing AiHub keys (other AI Hub tools still read `AiHubToolInvocationContext`) AND adds the neutral keys the relocated tools now read.

- [ ] **Step 3: Fix the wiring test import**

In `PropertyOptionsToolWiringTest.java`, update imports of the moved tool/resolver types from `com.bytechef.ee.ai.hub.tool.*` to `com.bytechef.ee.ai.copilot.tool.*` (e.g. `PropertyOptionsResolver`). The reflective `getDeclaredMethod(...)` on `registerToolAttachStateVisibilityToolCallbacks` and the asserted tool-name strings are unchanged.

- [ ] **Step 4: Verify AI Hub compiles + the wiring test passes**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests 'com.bytechef.ee.ai.hub.config.PropertyOptionsToolWiringTest'`
Expected: BUILD SUCCESSFUL — tools resolve from `ai-copilot-tool`, names still present in both catalogs.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubConfiguration.java \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/agent/AiHubSpringAIAgent.java \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/config/PropertyOptionsToolWiringTest.java
git commit -m "0_732 Wire AI Hub to the relocated shared component tools + merge neutral tool context"
```

---

## Task 6: Full verification of both modules

- [ ] **Step 1: spotless + check both modules**

Run:
```bash
./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-tool:spotlessApply :server:ee:libs:ai:ai-hub:ai-hub-service:spotlessApply \
          :server:ee:libs:ai:ai-copilot:ai-copilot-tool:check :server:ee:libs:ai:ai-hub:ai-hub-service:check
```
Expected: BUILD SUCCESSFUL for both. AI Hub's full suite (runtime-provider, sidebar, wiring) is the behavior guard — it must stay green, proving the relocation changed nothing for AI Hub. If SpotBugs flags the relocated records' `Map`/`List` exposure, the existing `@SuppressFBWarnings` annotations moved with them — confirm they're intact.

- [ ] **Step 2: Confirm no stale references to the old package**

Run:
```bash
grep -rn "com.bytechef.ee.ai.hub.tool.PropertyOptionsResolver\|com.bytechef.ee.ai.hub.tool.ListConnectionsForComponentToolCallback\|com.bytechef.ee.ai.hub.tool.SelectConnectionToolCallback\|com.bytechef.ee.ai.hub.tool.LookupActionPropertyOptionsToolCallback\|com.bytechef.ee.ai.hub.tool.LookupTriggerPropertyOptionsToolCallback\|com.bytechef.ee.ai.hub.tool.SelectPropertyOptionToolCallback\|com.bytechef.ee.ai.hub.tool.SelectTriggerPropertyOptionToolCallback" server --include=*.java | grep -v /.claude/
```
Expected: no matches (all references now point at `com.bytechef.ee.ai.copilot.tool`).

- [ ] **Step 3: Final formatting commit (only if spotless changed files — fresh commit, never amend)**

```bash
git add -u server/ee/libs/ai/ai-copilot/ai-copilot-tool server/ee/libs/ai/ai-hub/ai-hub-service
git commit -m "0_732 Apply spotless formatting for shared-tools relocation"
```

---

## Self-Review

**Spec coverage:**
- Home `ai-copilot-tool` + deps → Task 2. ✓ (Neutral context placed in `ai-copilot-tool` not `ai-copilot-api` — documented refinement, because `ai-copilot-api` lacks Spring AI `ToolContext`.)
- Neutral `AgentToolInvocationContext` (workspaceId/userId/environmentId/conversationId) → Task 1. ✓
- Neutral `ToolStateVisibilityMetrics` + AiHub impl + NOOP → Tasks 1, 3. ✓
- Move resolver + 6 callbacks (+ tests) → Task 4. ✓
- `kind` markers unchanged → no task needed (relocation preserves strings). ✓
- AI Hub imports from new package + merges neutral context + deletes old → Tasks 4 (mv) + 5. ✓
- Behavior identical for AI Hub (existing tests as guard) → Tasks 5–6. ✓
- `askUserQuestion` stays in AI Hub → not moved (not in the move list). ✓

**Placeholder scan:** No TBD/TODO. Task 4 uses precise transform steps (git mv + package + 2 symbol swaps) rather than re-pasting 7 existing files — appropriate for a relocation; every symbol swap is named explicitly. Two "verify/confirm" notes (security module path in Task 2; `PropertyOptionsResolver` component-scan in Task 5) are explicit conditional instructions with a concrete fallback, not vague placeholders.

**Type consistency:** `AgentToolInvocationContext(Long workspaceId, Long userId, Long environmentId, String conversationId)` defined in Task 1, constructed identically in Task 5's merge and in Task 4's test-fixture swap. `ToolStateVisibilityMetrics.recordStateVisibility(String,String)` matches `AiHubToolAttachMetrics`'s existing signature (Task 3) and the tools' calls. Package `com.bytechef.ee.ai.copilot.tool` consistent across Tasks 1, 4, 5.
