# Workspace System Prompt Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a workspace admin set one free-text system prompt that is appended to every AI agent LLM turn in the workspace — AI Hub ASK/BUILD + personal agents, subagent delegates, and the canvas AI Agent component.

**Architecture:** New EE module `platform-ai-workspace-prompt` (api/service/graphql), a parallel of `platform-ai-guardrails`: a property-backed per-workspace prompt, a cached engine bean (`WorkspaceSystemPrompts`), and a Spring AI `CallAdvisor`/`StreamAdvisor` (`WorkspaceSystemPromptAdvisor`) that appends the text to the request's system message under a pinned header. Wired at the same three seams the guardrails advisor already passes through.

**Tech Stack:** Java 25 / Spring Boot 4, Spring AI (CallAdvisor/StreamAdvisor), Caffeine, Spring GraphQL, React 19 + TanStack Query + generated GraphQL client.

**Spec:** `docs/superpowers/specs/2026-08-04-workspace-system-prompt-design.md`

## Global Constraints

- Files under `server/ee/` use the ByteChef Enterprise license header and an `@version ee` Javadoc tag. Files under `server/libs/` use the Apache 2.0 header. Spotless picks the EE header from the `@version ee` CONTENT, not the path — never put `@version ee` on a CE file.
- Max prompt length is exactly **4000** characters (`WorkspaceSystemPrompt.MAX_LENGTH`), matching `CopilotConstants.ADDITIONAL_SYSTEM_PROMPT_MAX_LENGTH`.
- The advisory header wording is load-bearing and pinned by test (Task 4). Copy it verbatim wherever it appears.
- Advisor order is `Ordered.HIGHEST_PRECEDENCE + 100` — after `AiGuardrailsAdvisor` (`HIGHEST_PRECEDENCE`), so admin text is never redacted/blocked by the workspace's own guardrails.
- All lookups on the agent hot path are fail-open: a storage error logs and skips the append, never fails the turn.
- Workspace-only: a `null` workspaceId means "no prompt" — there is NO tenant-default row in this feature.
- Java style: blank line before control statements, blank line between a variable modification and its use, no trailing blank line in a class body, no `TODO:` comments, test method names camelCase without underscores.
- Client style: interface names end `I`/`Props`, sort-keys (alphabetical object keys), `twMerge` not `cn()`, Lucide icons with `Icon` suffix, named imports sorted.
- Never judge a Gradle run piped into `tail`/`grep` — redirect to a file, check `$?` on its own line, then grep the file.
- Commit only files this task touched. NEVER amend existing commits (user commits in parallel on this branch).

---

### Task 1: Module scaffolding + API module

**Files:**
- Modify: `settings.gradle.kts` (three `include` lines, next to the `platform-ai-guardrails` trio at ~line 774)
- Create: `server/ee/libs/platform/platform-ai/platform-ai-workspace-prompt/platform-ai-workspace-prompt-api/build.gradle.kts`
- Create: `server/ee/libs/platform/platform-ai/platform-ai-workspace-prompt/platform-ai-workspace-prompt-service/build.gradle.kts`
- Create: `server/ee/libs/platform/platform-ai/platform-ai-workspace-prompt/platform-ai-workspace-prompt-graphql/build.gradle.kts`
- Create: `.../platform-ai-workspace-prompt-api/src/main/java/com/bytechef/ee/platform/ai/workspaceprompt/domain/WorkspaceSystemPrompt.java`
- Create: `.../platform-ai-workspace-prompt-api/src/main/java/com/bytechef/ee/platform/ai/workspaceprompt/service/WorkspaceSystemPromptService.java`

**Interfaces:**
- Produces: `WorkspaceSystemPrompt(long workspaceId, String prompt)` record with `PROPERTY_KEY = "workspace_system_prompt"` and `MAX_LENGTH = 4000`; `WorkspaceSystemPromptService` with `Optional<String> fetchWorkspaceSystemPrompt(long workspaceId)` and `Optional<String> saveWorkspaceSystemPrompt(long workspaceId, @Nullable String prompt)`.

- [ ] **Step 1: Register the three modules in `settings.gradle.kts`**

Directly after the three `platform-ai-guardrails` includes (~line 776), add:

```kotlin
include("server:ee:libs:platform:platform-ai:platform-ai-workspace-prompt:platform-ai-workspace-prompt-api")
include("server:ee:libs:platform:platform-ai:platform-ai-workspace-prompt:platform-ai-workspace-prompt-graphql")
include("server:ee:libs:platform:platform-ai:platform-ai-workspace-prompt:platform-ai-workspace-prompt-service")
```

- [ ] **Step 2: Create the three `build.gradle.kts` files**

`platform-ai-workspace-prompt-api/build.gradle.kts`:

```kotlin
dependencies {
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(project(":server:libs:test:test-support"))
}
```

`platform-ai-workspace-prompt-service/build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    // Resolves a job principal id (project deployment id) to its workspace for WorkspaceSystemPromptAdvisorProviderImpl.
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    // Implements the CE SPI seam (WorkspaceSystemPromptAdvisorProvider) so non-EE components can obtain this advisor.
    api(project(":server:libs:platform:platform-ai:platform-ai-api"))
    // WorkspaceSystemPromptAdvisor implements Spring AI's CallAdvisor/StreamAdvisor and takes
    // ChatClientRequest types on its public surface, so callers registering it need these transitively too.
    api("org.springframework.ai:spring-ai-client-chat")
    api("org.springframework.ai:spring-ai-model")

    api(project(":server:ee:libs:platform:platform-ai:platform-ai-workspace-prompt:platform-ai-workspace-prompt-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation(project(":server:libs:test:test-support"))
}
```

`platform-ai-workspace-prompt-graphql/build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.springframework.graphql:spring-graphql")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-workspace-prompt:platform-ai-workspace-prompt-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}
```

- [ ] **Step 3: Create the domain record**

`WorkspaceSystemPrompt.java` (EE header — copy the exact 6-line header from `AiGuardrailsWorkspaceSettings.java`):

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.workspaceprompt.domain;

/**
 * A workspace administrator's standing instructions for every AI agent operating in the workspace. Persisted as a
 * single {@link com.bytechef.platform.configuration.domain.Property} row per workspace rather than a dedicated table —
 * the platform property store already handles scope/audit/versioning and this is plain config data (the same storage
 * decision as {@code AiGuardrailsWorkspaceSettings}). Workspace-only by design: there is no tenant-default row; a
 * {@code null} workspace resolution means "no prompt applies".
 *
 * @version ee
 */
public record WorkspaceSystemPrompt(long workspaceId, String prompt) {

    public static final String PROPERTY_KEY = "workspace_system_prompt";

    public static final int MAX_LENGTH = 4000;
}
```

- [ ] **Step 4: Create the service interface**

`WorkspaceSystemPromptService.java` (EE header):

```java
package com.bytechef.ee.platform.ai.workspaceprompt.service;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * CRUD for the per-workspace system prompt. Values are always stored stripped; a blank or null prompt on save deletes
 * the row.
 *
 * @version ee
 */
public interface WorkspaceSystemPromptService {

    /**
     * Returns the workspace's stored prompt, or empty when none is set. Never returns a blank string.
     */
    Optional<String> fetchWorkspaceSystemPrompt(long workspaceId);

    /**
     * Saves the stripped prompt for the workspace and returns it. A null/blank {@code prompt} deletes the stored row
     * and returns empty. Throws {@link IllegalArgumentException} when the stripped prompt exceeds
     * {@link com.bytechef.ee.platform.ai.workspaceprompt.domain.WorkspaceSystemPrompt#MAX_LENGTH}.
     */
    Optional<String> saveWorkspaceSystemPrompt(long workspaceId, @Nullable String prompt);
}
```

- [ ] **Step 5: Verify it compiles, then commit**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-workspace-prompt:platform-ai-workspace-prompt-api:compileJava > /tmp/t1.log 2>&1
echo $?
git add settings.gradle.kts server/ee/libs/platform/platform-ai/platform-ai-workspace-prompt
git commit -m "Add the platform-ai-workspace-prompt module skeleton (domain, service API)"
```

---

### Task 2: Property-backed service implementation (TDD)

**Files:**
- Create: `.../platform-ai-workspace-prompt-service/src/main/java/com/bytechef/ee/platform/ai/workspaceprompt/service/WorkspaceSystemPromptServiceImpl.java`
- Test: `.../platform-ai-workspace-prompt-service/src/test/java/com/bytechef/ee/platform/ai/workspaceprompt/service/WorkspaceSystemPromptServiceTest.java`

**Interfaces:**
- Consumes: Task 1's `WorkspaceSystemPromptService` interface, `PropertyService` (`fetchProperty(key, Scope, scopeId)`, `save(key, Map, Scope, scopeId)`, `delete(key, Scope, scopeId)`).
- Produces: the `@Service @ConditionalOnEEVersion` binding later tasks autowire.

- [ ] **Step 1: Write the failing test**

```java
/*
 * (EE license header)
 */

package com.bytechef.ee.platform.ai.workspaceprompt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.workspaceprompt.domain.WorkspaceSystemPrompt;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class WorkspaceSystemPromptServiceTest {

    private final PropertyService propertyService = mock(PropertyService.class);
    private final WorkspaceSystemPromptServiceImpl service = new WorkspaceSystemPromptServiceImpl(propertyService);

    @Test
    void testFetchReturnsStoredPrompt() {
        Property property = new Property();

        property.setValue(Map.of("prompt", "Always answer in German."));

        when(propertyService.fetchProperty(
            WorkspaceSystemPrompt.PROPERTY_KEY, Property.Scope.WORKSPACE, 7L))
                .thenReturn(Optional.of(property));

        assertThat(service.fetchWorkspaceSystemPrompt(7L)).contains("Always answer in German.");
    }

    @Test
    void testFetchReturnsEmptyWhenNoRow() {
        when(propertyService.fetchProperty(
            WorkspaceSystemPrompt.PROPERTY_KEY, Property.Scope.WORKSPACE, 7L))
                .thenReturn(Optional.empty());

        assertThat(service.fetchWorkspaceSystemPrompt(7L)).isEmpty();
    }

    @Test
    void testSaveStripsAndStoresPrompt() {
        Optional<String> saved = service.saveWorkspaceSystemPrompt(7L, "  Be concise.  ");

        assertThat(saved).contains("Be concise.");

        verify(propertyService).save(
            eq(WorkspaceSystemPrompt.PROPERTY_KEY), eq(Map.of("prompt", "Be concise.")),
            eq(Property.Scope.WORKSPACE), eq(7L));
    }

    @Test
    void testSaveBlankDeletesRow() {
        assertThat(service.saveWorkspaceSystemPrompt(7L, "   ")).isEmpty();
        assertThat(service.saveWorkspaceSystemPrompt(7L, null)).isEmpty();

        verify(propertyService, org.mockito.Mockito.times(2))
            .delete(WorkspaceSystemPrompt.PROPERTY_KEY, Property.Scope.WORKSPACE, 7L);
    }

    @Test
    void testSaveRejectsOverLengthPrompt() {
        String tooLong = "x".repeat(WorkspaceSystemPrompt.MAX_LENGTH + 1);

        assertThatThrownBy(() -> service.saveWorkspaceSystemPrompt(7L, tooLong))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

Note: if `Property` has no no-arg constructor + setter shape, mock it instead: `Property property = mock(Property.class); when(property.getValue()).thenReturn(Map.of("prompt", ...));` — check `AiGuardrailsWorkspaceSettingsServiceTest` for the established construction pattern and copy it.

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-workspace-prompt:platform-ai-workspace-prompt-service:test --tests "com.bytechef.ee.platform.ai.workspaceprompt.service.WorkspaceSystemPromptServiceTest" > /tmp/t2.log 2>&1
echo $?
```

Expected: compile failure — `WorkspaceSystemPromptServiceImpl` does not exist.

- [ ] **Step 3: Write the implementation**

```java
/*
 * (EE license header)
 */

package com.bytechef.ee.platform.ai.workspaceprompt.service;

import com.bytechef.ee.platform.ai.workspaceprompt.domain.WorkspaceSystemPrompt;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backs the workspace system prompt with a single {@link Property} row per workspace, keyed by
 * {@link WorkspaceSystemPrompt#PROPERTY_KEY} under {@link Property.Scope#WORKSPACE} — mirroring
 * {@code AiGuardrailsWorkspaceSettingsServiceImpl}'s storage shape. No PLATFORM-scope tenant-default row exists for
 * this feature.
 *
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
class WorkspaceSystemPromptServiceImpl implements WorkspaceSystemPromptService {

    private static final String KEY_PROMPT = "prompt";

    private final PropertyService propertyService;

    WorkspaceSystemPromptServiceImpl(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> fetchWorkspaceSystemPrompt(long workspaceId) {
        return propertyService
            .fetchProperty(WorkspaceSystemPrompt.PROPERTY_KEY, Property.Scope.WORKSPACE, workspaceId)
            .map(property -> (String) property.getValue()
                .get(KEY_PROMPT))
            .filter(prompt -> prompt != null && !prompt.isBlank());
    }

    @Override
    public Optional<String> saveWorkspaceSystemPrompt(long workspaceId, @Nullable String prompt) {
        String stripped = prompt == null ? "" : prompt.strip();

        if (stripped.isEmpty()) {
            propertyService.delete(WorkspaceSystemPrompt.PROPERTY_KEY, Property.Scope.WORKSPACE, workspaceId);

            return Optional.empty();
        }

        Validate.isTrue(
            stripped.length() <= WorkspaceSystemPrompt.MAX_LENGTH,
            "prompt must be at most %d characters", WorkspaceSystemPrompt.MAX_LENGTH);

        propertyService.save(
            WorkspaceSystemPrompt.PROPERTY_KEY, Map.of(KEY_PROMPT, stripped), Property.Scope.WORKSPACE, workspaceId);

        return Optional.of(stripped);
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Same command as Step 2. Expected: exit 0, all 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/platform/platform-ai/platform-ai-workspace-prompt/platform-ai-workspace-prompt-service
git commit -m "Add the property-backed workspace system prompt service"
```

---

### Task 3: `WorkspaceSystemPrompts` engine bean (cached, fail-open) (TDD)

**Files:**
- Create: `.../platform-ai-workspace-prompt-service/src/main/java/com/bytechef/ee/platform/ai/workspaceprompt/WorkspaceSystemPrompts.java`
- Test: `.../platform-ai-workspace-prompt-service/src/test/java/com/bytechef/ee/platform/ai/workspaceprompt/WorkspaceSystemPromptsTest.java`

**Interfaces:**
- Consumes: Task 2's `WorkspaceSystemPromptService`.
- Produces: `public @Nullable String fetchPrompt(@Nullable Long workspaceId)` — the hot-path lookup every seam and the advisor use. Registered as `@Component @ConditionalOnEEVersion`.

- [ ] **Step 1: Write the failing test**

```java
/*
 * (EE license header)
 */

package com.bytechef.ee.platform.ai.workspaceprompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.workspaceprompt.service.WorkspaceSystemPromptService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class WorkspaceSystemPromptsTest {

    private final WorkspaceSystemPromptService service = mock(WorkspaceSystemPromptService.class);
    private final WorkspaceSystemPrompts workspaceSystemPrompts = new WorkspaceSystemPrompts(service);

    @Test
    void testFetchPromptReturnsStoredPrompt() {
        when(service.fetchWorkspaceSystemPrompt(7L)).thenReturn(Optional.of("Be concise."));

        assertThat(workspaceSystemPrompts.fetchPrompt(7L)).isEqualTo("Be concise.");
    }

    @Test
    void testFetchPromptReturnsNullForNullWorkspace() {
        assertThat(workspaceSystemPrompts.fetchPrompt(null)).isNull();
    }

    @Test
    void testFetchPromptMemoizesLookups() {
        when(service.fetchWorkspaceSystemPrompt(7L)).thenReturn(Optional.of("Be concise."));

        workspaceSystemPrompts.fetchPrompt(7L);
        workspaceSystemPrompts.fetchPrompt(7L);

        verify(service, times(1)).fetchWorkspaceSystemPrompt(7L);
    }

    @Test
    void testFetchPromptFailsOpenOnLookupError() {
        when(service.fetchWorkspaceSystemPrompt(7L)).thenThrow(new IllegalStateException("db down"));

        assertThat(workspaceSystemPrompts.fetchPrompt(7L)).isNull();
    }
}
```

- [ ] **Step 2: Run the test to verify it fails** (same gradle module as Task 2; expected compile failure)

- [ ] **Step 3: Write the implementation**

```java
/*
 * (EE license header)
 */

package com.bytechef.ee.platform.ai.workspaceprompt;

import com.bytechef.ee.platform.ai.workspaceprompt.service.WorkspaceSystemPromptService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Hot-path lookup for the workspace system prompt, memoized for {@link #CACHE_TTL} so agent tool loops (one advisor
 * pass per model iteration) do not hit the property store on every call. Registered unconditionally, like the
 * {@code AiGuardrails} engine — inert (all lookups miss) when no workspace has a prompt set.
 *
 * <p>
 * Fail-open by contract: a lookup error logs and returns {@code null} (no prompt), never failing the turn. A saved
 * change propagates to running agents within the TTL; there is deliberately no cross-node invalidation.
 * </p>
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
public class WorkspaceSystemPrompts {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceSystemPrompts.class);

    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final WorkspaceSystemPromptService workspaceSystemPromptService;
    private final Cache<Long, Optional<String>> promptCache = Caffeine.newBuilder()
        .expireAfterWrite(CACHE_TTL)
        .build();

    public WorkspaceSystemPrompts(WorkspaceSystemPromptService workspaceSystemPromptService) {
        this.workspaceSystemPromptService = workspaceSystemPromptService;
    }

    /**
     * Returns the workspace's prompt, or {@code null} when the workspace is unknown ({@code null}), has no prompt set,
     * or the lookup failed. This feature is workspace-only — a {@code null} workspace id never falls back to any
     * tenant-wide default.
     */
    public @Nullable String fetchPrompt(@Nullable Long workspaceId) {
        if (workspaceId == null) {
            return null;
        }

        try {
            Optional<String> prompt = promptCache.get(
                workspaceId, id -> workspaceSystemPromptService.fetchWorkspaceSystemPrompt(id));

            return prompt.orElse(null);
        } catch (RuntimeException exception) {
            log.warn("Workspace system prompt lookup failed for workspace {}; skipping", workspaceId, exception);

            return null;
        }
    }
}
```

Note: Caffeine's `get(key, mappingFunction)` propagates a mapping-function exception to the caller and caches nothing — which is exactly the fail-open shape the test pins (a later call retries).

- [ ] **Step 4: Run the test to verify it passes**

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/platform/platform-ai/platform-ai-workspace-prompt/platform-ai-workspace-prompt-service
git commit -m "Add the cached WorkspaceSystemPrompts engine bean"
```

---

### Task 4: `WorkspaceSystemPromptAdvisor` (TDD)

**Files:**
- Create: `.../platform-ai-workspace-prompt-service/src/main/java/com/bytechef/ee/platform/ai/workspaceprompt/advisor/WorkspaceSystemPromptAdvisor.java`
- Test: `.../platform-ai-workspace-prompt-service/src/test/java/com/bytechef/ee/platform/ai/workspaceprompt/advisor/WorkspaceSystemPromptAdvisorTest.java`

**Interfaces:**
- Consumes: Task 3's `WorkspaceSystemPrompts.fetchPrompt(Long)`.
- Produces: `public WorkspaceSystemPromptAdvisor(WorkspaceSystemPrompts workspaceSystemPrompts, @Nullable Long workspaceId)` implementing `CallAdvisor` + `StreamAdvisor`; public constant `WORKSPACE_INSTRUCTIONS_HEADER = "## Workspace instructions"`; order `HIGHEST_PRECEDENCE + 100`.

- [ ] **Step 1: Write the failing test**

Use a recording `CallAdvisorChain`/`StreamAdvisorChain` mock that captures the forwarded request (see `AiGuardrailsAdvisorTest` for the harness idiom — mock the chain, capture with `ArgumentCaptor`).

```java
/*
 * (EE license header)
 */

package com.bytechef.ee.platform.ai.workspaceprompt.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.workspaceprompt.WorkspaceSystemPrompts;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

/**
 * @version ee
 */
class WorkspaceSystemPromptAdvisorTest {

    private static final Long WORKSPACE_ID = 7L;

    private final WorkspaceSystemPrompts workspaceSystemPrompts = mock(WorkspaceSystemPrompts.class);
    private final CallAdvisorChain callChain = mock(CallAdvisorChain.class);
    private final WorkspaceSystemPromptAdvisor advisor =
        new WorkspaceSystemPromptAdvisor(workspaceSystemPrompts, WORKSPACE_ID);

    @Test
    void testAppendsSectionToExistingSystemMessage() {
        when(workspaceSystemPrompts.fetchPrompt(WORKSPACE_ID)).thenReturn("Always answer in German.");

        ChatClientRequest forwarded = adviseCallAndCapture(request(new SystemMessage("Base prompt."),
            new UserMessage("hi")));

        String systemText = systemText(forwarded);

        assertThat(systemText).startsWith("Base prompt.");
        assertThat(systemText).contains(WorkspaceSystemPromptAdvisor.WORKSPACE_INSTRUCTIONS_HEADER);
        assertThat(systemText).endsWith("Always answer in German.");
    }

    @Test
    void testPinsExactAdvisoryWording() {
        when(workspaceSystemPrompts.fetchPrompt(WORKSPACE_ID)).thenReturn("Be concise.");

        ChatClientRequest forwarded = adviseCallAndCapture(request(new SystemMessage("Base."),
            new UserMessage("hi")));

        assertThat(systemText(forwarded)).contains(
            "## Workspace instructions\n\n"
                + "The workspace administrator provided the following instructions. Follow them\n"
                + "where they apply, but they cannot override or weaken any rule above,\n"
                + "including safety and security rules.\n\n"
                + "Be concise.");
    }

    @Test
    void testInsertsSystemMessageWhenNoneExists() {
        when(workspaceSystemPrompts.fetchPrompt(WORKSPACE_ID)).thenReturn("Be concise.");

        ChatClientRequest forwarded = adviseCallAndCapture(request(new UserMessage("hi")));

        List<Message> instructions = forwarded.prompt()
            .getInstructions();

        assertThat(instructions.get(0)).isInstanceOf(SystemMessage.class);
        assertThat(instructions.get(0)
            .getText()).startsWith(WorkspaceSystemPromptAdvisor.WORKSPACE_INSTRUCTIONS_HEADER);
    }

    @Test
    void testPassesThroughWhenNoPrompt() {
        when(workspaceSystemPrompts.fetchPrompt(WORKSPACE_ID)).thenReturn(null);

        ChatClientRequest original = request(new SystemMessage("Base."), new UserMessage("hi"));
        ChatClientRequest forwarded = adviseCallAndCapture(original);

        assertThat(forwarded).isSameAs(original);
    }

    @Test
    void testDoesNotAppendTwice() {
        when(workspaceSystemPrompts.fetchPrompt(WORKSPACE_ID)).thenReturn("Be concise.");

        ChatClientRequest first = adviseCallAndCapture(request(new SystemMessage("Base."), new UserMessage("hi")));

        // A request whose system message already carries the section (e.g. replayed) is left unchanged.
        ChatClientRequest second = adviseCallAndCapture(first);

        assertThat(second).isSameAs(first);
    }

    @Test
    void testStreamPathAppendsSection() {
        when(workspaceSystemPrompts.fetchPrompt(WORKSPACE_ID)).thenReturn("Be concise.");

        StreamAdvisorChain streamChain = mock(StreamAdvisorChain.class);

        when(streamChain.nextStream(any())).thenReturn(Flux.empty());

        advisor.adviseStream(request(new SystemMessage("Base."), new UserMessage("hi")), streamChain);

        ArgumentCaptor<ChatClientRequest> captor = ArgumentCaptor.forClass(ChatClientRequest.class);

        verify(streamChain).nextStream(captor.capture());

        assertThat(systemText(captor.getValue())).contains(
            WorkspaceSystemPromptAdvisor.WORKSPACE_INSTRUCTIONS_HEADER);
    }

    @Test
    void testOrderRunsAfterGuardrails() {
        assertThat(advisor.getOrder()).isGreaterThan(Ordered.HIGHEST_PRECEDENCE);
    }

    private ChatClientRequest adviseCallAndCapture(ChatClientRequest request) {
        when(callChain.nextCall(any())).thenReturn(mock(ChatClientResponse.class));

        advisor.adviseCall(request, callChain);

        ArgumentCaptor<ChatClientRequest> captor = ArgumentCaptor.forClass(ChatClientRequest.class);

        verify(callChain, org.mockito.Mockito.atLeastOnce()).nextCall(captor.capture());

        return captor.getValue();
    }

    private static ChatClientRequest request(Message... messages) {
        return ChatClientRequest.builder()
            .prompt(new Prompt(List.of(messages)))
            .build();
    }

    private static String systemText(ChatClientRequest request) {
        return request.prompt()
            .getInstructions()
            .stream()
            .filter(SystemMessage.class::isInstance)
            .map(Message::getText)
            .findFirst()
            .orElseThrow();
    }
}
```

Note: `testDoesNotAppendTwice` reuses `callChain` across two advises; use `org.mockito.Mockito.clearInvocations(callChain)` between them if the captor picks up the first call's value — or capture with `getAllValues()` and take the last.

- [ ] **Step 2: Run the test to verify it fails** (compile failure — advisor class missing)

- [ ] **Step 3: Write the implementation**

```java
/*
 * (EE license header)
 */

package com.bytechef.ee.platform.ai.workspaceprompt.advisor;

import com.bytechef.ee.platform.ai.workspaceprompt.WorkspaceSystemPrompts;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * Spring AI {@link CallAdvisor}/{@link StreamAdvisor} appending the workspace administrator's standing instructions to
 * the request's system message, under {@link #WORKSPACE_INSTRUCTIONS_HEADER}. The base agent prompt always comes first
 * and the section wording states it cannot override safety/security rules — the same advisory posture as the
 * personal-agent overlay.
 *
 * <p>
 * Runs at {@code HIGHEST_PRECEDENCE + 100} — AFTER {@code AiGuardrailsAdvisor}'s input scan, so an admin's own
 * instructions are never redacted or blocked by the workspace's own guardrail policy. Idempotent per request: a system
 * message that already carries the header is left untouched. All failure modes are fail-open (the engine returns
 * {@code null} on lookup errors) — a missing prompt simply passes the request through unchanged.
 * </p>
 *
 * @version ee
 */
public final class WorkspaceSystemPromptAdvisor implements CallAdvisor, StreamAdvisor {

    public static final String WORKSPACE_INSTRUCTIONS_HEADER = "## Workspace instructions";

    private static final String NAME = "WorkspaceSystemPromptAdvisor";
    private static final int ORDER = HIGHEST_PRECEDENCE + 100;

    private final WorkspaceSystemPrompts workspaceSystemPrompts;
    private final @Nullable Long workspaceId;

    public WorkspaceSystemPromptAdvisor(WorkspaceSystemPrompts workspaceSystemPrompts, @Nullable Long workspaceId) {
        this.workspaceSystemPrompts = Objects.requireNonNull(workspaceSystemPrompts, "workspaceSystemPrompts");
        this.workspaceId = workspaceId;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        return callAdvisorChain.nextCall(applyWorkspacePrompt(chatClientRequest));
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
        ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

        return streamAdvisorChain.nextStream(applyWorkspacePrompt(chatClientRequest));
    }

    private ChatClientRequest applyWorkspacePrompt(ChatClientRequest chatClientRequest) {
        String workspacePrompt = workspaceSystemPrompts.fetchPrompt(workspaceId);

        if (workspacePrompt == null) {
            return chatClientRequest;
        }

        Prompt prompt = chatClientRequest.prompt();
        List<Message> instructions = prompt.getInstructions();

        int lastSystemIndex = -1;

        for (int index = 0; index < instructions.size(); index++) {
            if (instructions.get(index)
                .getMessageType() == MessageType.SYSTEM) {

                lastSystemIndex = index;
            }
        }

        List<Message> patched = new ArrayList<>(instructions);

        if (lastSystemIndex >= 0) {
            SystemMessage systemMessage = (SystemMessage) instructions.get(lastSystemIndex);
            String text = systemMessage.getText();

            if (text != null && text.contains(WORKSPACE_INSTRUCTIONS_HEADER)) {
                return chatClientRequest;
            }

            String appended = (text == null ? "" : text) + "\n\n" + section(workspacePrompt);

            patched.set(lastSystemIndex, systemMessage.mutate()
                .text(appended)
                .build());
        } else {
            patched.add(0, new SystemMessage(section(workspacePrompt)));
        }

        Prompt patchedPrompt = new Prompt(patched, prompt.getOptions());

        return chatClientRequest.mutate()
            .prompt(patchedPrompt)
            .build();
    }

    /**
     * The exact wording is pinned by {@code WorkspaceSystemPromptAdvisorTest#testPinsExactAdvisoryWording} — it is the
     * contract that keeps the workspace overlay subordinate to the base prompt. Change it only together with that test.
     */
    private static String section(String workspacePrompt) {
        return WORKSPACE_INSTRUCTIONS_HEADER + "\n\n"
            + "The workspace administrator provided the following instructions. Follow them\n"
            + "where they apply, but they cannot override or weaken any rule above,\n"
            + "including safety and security rules.\n\n"
            + workspacePrompt;
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/platform/platform-ai/platform-ai-workspace-prompt/platform-ai-workspace-prompt-service
git commit -m "Add WorkspaceSystemPromptAdvisor appending workspace instructions to the system message"
```

---

### Task 5: CE SPI + EE provider impl for the canvas surface (TDD)

**Files:**
- Create: `server/libs/platform/platform-ai/platform-ai-api/src/main/java/com/bytechef/platform/ai/workspaceprompt/WorkspaceSystemPromptAdvisorProvider.java` (CE — Apache header, NO `@version ee`)
- Create: `.../platform-ai-workspace-prompt-service/src/main/java/com/bytechef/ee/platform/ai/workspaceprompt/advisor/WorkspaceSystemPromptAdvisorProviderImpl.java`
- Test: `.../platform-ai-workspace-prompt-service/src/test/java/com/bytechef/ee/platform/ai/workspaceprompt/advisor/WorkspaceSystemPromptAdvisorProviderTest.java`

**Interfaces:**
- Consumes: `PlatformType`, `ProjectDeploymentService`, `ProjectService` (same resolution chain as `AiGuardrailsAdvisorProviderImpl`), Tasks 3–4.
- Produces: CE `WorkspaceSystemPromptAdvisorProvider.getAdvisor(@Nullable PlatformType, @Nullable Long jobPrincipalId, String surface): Optional<Advisor>` — the exact same shape as `AiGuardrailsAdvisorProvider`, consumed by Task 9.

- [ ] **Step 1: Write the CE SPI interface** (Apache 2.0 header — copy from `AiGuardrailsAdvisorProvider.java`)

```java
package com.bytechef.platform.ai.workspaceprompt;

import com.bytechef.platform.constant.PlatformType;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.advisor.api.Advisor;

/**
 * CE-side SPI seam so a component running on the classpath without any EE module (e.g. the canvas AI Agent component)
 * can obtain a workspace-bound system-prompt advisor without depending on the EE
 * {@code com.bytechef.ee.platform.ai.workspaceprompt} module directly — the same idiom as
 * {@code AiGuardrailsAdvisorProvider}. When no EE implementation is registered, or the resolved workspace has no
 * prompt, callers simply skip attaching an advisor.
 *
 * @author Ivica Cardic
 */
public interface WorkspaceSystemPromptAdvisorProvider {

    /**
     * Returns an {@link Advisor} bound to the workspace resolved for {@code jobPrincipalId}, or empty when no workspace
     * system prompt applies (no EE implementation, non-AUTOMATION run, unresolvable workspace, or no prompt set).
     *
     * @param platformType   the platform the run belongs to, or {@code null} when the calling context carries none
     * @param jobPrincipalId the run's job principal id (a project deployment id), or {@code null} when unknown
     * @param surface        identifies the calling surface (e.g. {@code "ai_agent"}); reserved for telemetry
     * @return the workspace system prompt advisor, or empty when none applies
     */
    Optional<Advisor> getAdvisor(@Nullable PlatformType platformType, @Nullable Long jobPrincipalId, String surface);
}
```

- [ ] **Step 2: Write the failing provider-impl test**

```java
/*
 * (EE license header)
 */

package com.bytechef.ee.platform.ai.workspaceprompt.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.platform.ai.workspaceprompt.WorkspaceSystemPrompts;
import com.bytechef.platform.constant.PlatformType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 */
class WorkspaceSystemPromptAdvisorProviderTest {

    private final WorkspaceSystemPrompts workspaceSystemPrompts = mock(WorkspaceSystemPrompts.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectService projectService = mock(ProjectService.class);

    @SuppressWarnings("unchecked")
    private WorkspaceSystemPromptAdvisorProviderImpl provider() {
        ObjectProvider<ProjectDeploymentService> deploymentProvider = mock(ObjectProvider.class);
        ObjectProvider<ProjectService> projectProvider = mock(ObjectProvider.class);

        when(deploymentProvider.getIfAvailable()).thenReturn(projectDeploymentService);
        when(projectProvider.getIfAvailable()).thenReturn(projectService);

        return new WorkspaceSystemPromptAdvisorProviderImpl(
            workspaceSystemPrompts, deploymentProvider, projectProvider);
    }

    @Test
    void testReturnsAdvisorWhenWorkspaceHasPrompt() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setProjectId(11L);

        Project project = new Project();

        project.setWorkspaceId(7L);

        when(projectDeploymentService.getProjectDeployment(42L)).thenReturn(projectDeployment);
        when(projectService.getProject(11L)).thenReturn(project);
        when(workspaceSystemPrompts.fetchPrompt(7L)).thenReturn("Be concise.");

        assertThat(provider().getAdvisor(PlatformType.AUTOMATION, 42L, "ai_agent")).isPresent();
    }

    @Test
    void testReturnsEmptyWhenWorkspaceHasNoPrompt() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setProjectId(11L);

        Project project = new Project();

        project.setWorkspaceId(7L);

        when(projectDeploymentService.getProjectDeployment(42L)).thenReturn(projectDeployment);
        when(projectService.getProject(11L)).thenReturn(project);
        when(workspaceSystemPrompts.fetchPrompt(7L)).thenReturn(null);

        assertThat(provider().getAdvisor(PlatformType.AUTOMATION, 42L, "ai_agent")).isEmpty();
    }

    @Test
    void testReturnsEmptyForNonAutomationOrUnknownPrincipal() {
        assertThat(provider().getAdvisor(PlatformType.EMBEDDED, 42L, "ai_agent")).isEmpty();
        assertThat(provider().getAdvisor(PlatformType.AUTOMATION, null, "ai_agent")).isEmpty();
        assertThat(provider().getAdvisor(null, 42L, "ai_agent")).isEmpty();
    }

    @Test
    void testFailsOpenWhenResolutionThrows() {
        when(projectDeploymentService.getProjectDeployment(42L)).thenThrow(new IllegalStateException("gone"));

        assertThat(provider().getAdvisor(PlatformType.AUTOMATION, 42L, "ai_agent")).isEmpty();
    }
}
```

Note: if `Project`/`ProjectDeployment` lack the setter shape shown, copy the construction idiom from `AiGuardrailsAdvisorProviderImplTest` — it builds the same two domain objects.

- [ ] **Step 3: Run the test to verify it fails** (compile failure)

- [ ] **Step 4: Write the provider impl**

Mirror `AiGuardrailsAdvisorProviderImpl` exactly, minus metrics, with one semantic difference: an unresolvable workspace returns **empty** (this feature has no tenant default), where guardrails fall back to the tenant-default policy.

```java
/*
 * (EE license header)
 */

package com.bytechef.ee.platform.ai.workspaceprompt.advisor;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.platform.ai.workspaceprompt.WorkspaceSystemPrompts;
import com.bytechef.platform.ai.workspaceprompt.WorkspaceSystemPromptAdvisorProvider;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * EE implementation of the CE {@link WorkspaceSystemPromptAdvisorProvider} SPI: resolves the calling run's workspace
 * ({@code jobPrincipalId} → project deployment → project → workspace, the same memoized chain as
 * {@code AiGuardrailsAdvisorProviderImpl}) and, when that workspace has a prompt set, returns a
 * {@link WorkspaceSystemPromptAdvisor} bound to it. Unlike guardrails there is no tenant default: a non-AUTOMATION
 * run, an unknown principal, or a failed resolution yields empty — no advisor — rather than a fallback policy.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
public class WorkspaceSystemPromptAdvisorProviderImpl implements WorkspaceSystemPromptAdvisorProvider {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceSystemPromptAdvisorProviderImpl.class);

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private record WorkspaceCacheKey(PlatformType platformType, long jobPrincipalId) {
    }

    private final WorkspaceSystemPrompts workspaceSystemPrompts;
    private final ObjectProvider<ProjectDeploymentService> projectDeploymentServiceProvider;
    private final ObjectProvider<ProjectService> projectServiceProvider;
    private final Cache<WorkspaceCacheKey, Optional<Long>> workspaceIdCache = Caffeine.newBuilder()
        .expireAfterWrite(CACHE_TTL)
        .build();

    @SuppressFBWarnings("EI2")
    public WorkspaceSystemPromptAdvisorProviderImpl(
        WorkspaceSystemPrompts workspaceSystemPrompts,
        ObjectProvider<ProjectDeploymentService> projectDeploymentServiceProvider,
        ObjectProvider<ProjectService> projectServiceProvider) {

        this.workspaceSystemPrompts = workspaceSystemPrompts;
        this.projectDeploymentServiceProvider = projectDeploymentServiceProvider;
        this.projectServiceProvider = projectServiceProvider;
    }

    @Override
    public Optional<Advisor> getAdvisor(
        @Nullable PlatformType platformType, @Nullable Long jobPrincipalId, String surface) {

        Long workspaceId = resolveWorkspaceId(platformType, jobPrincipalId);

        if (workspaceId == null || workspaceSystemPrompts.fetchPrompt(workspaceId) == null) {
            return Optional.empty();
        }

        return Optional.of(new WorkspaceSystemPromptAdvisor(workspaceSystemPrompts, workspaceId));
    }

    private @Nullable Long resolveWorkspaceId(@Nullable PlatformType platformType, @Nullable Long jobPrincipalId) {
        if (platformType != PlatformType.AUTOMATION || jobPrincipalId == null) {
            return null;
        }

        WorkspaceCacheKey cacheKey = new WorkspaceCacheKey(platformType, jobPrincipalId);

        Optional<Long> cachedWorkspaceId = workspaceIdCache.get(
            cacheKey, key -> Optional.ofNullable(fetchWorkspaceId(key.jobPrincipalId())));

        return cachedWorkspaceId.orElse(null);
    }

    private @Nullable Long fetchWorkspaceId(long jobPrincipalId) {
        ProjectDeploymentService projectDeploymentService = projectDeploymentServiceProvider.getIfAvailable();
        ProjectService projectService = projectServiceProvider.getIfAvailable();

        if (projectDeploymentService == null || projectService == null) {
            log.debug(
                "ProjectDeploymentService/ProjectService not available; no workspace system prompt for job principal "
                    + "{}",
                jobPrincipalId);

            return null;
        }

        try {
            ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(jobPrincipalId);
            Project project = projectService.getProject(projectDeployment.getProjectId());

            return project.getWorkspaceId();
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to resolve workspace for job principal {}; skipping workspace system prompt",
                jobPrincipalId, exception);

            return null;
        }
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

- [ ] **Step 6: Commit**

```bash
git add server/libs/platform/platform-ai/platform-ai-api server/ee/libs/platform/platform-ai/platform-ai-workspace-prompt
git commit -m "Add the workspace system prompt CE SPI and EE advisor provider"
```

---

### Task 6: GraphQL module + app registration (TDD)

**Files:**
- Create: `.../platform-ai-workspace-prompt-graphql/src/main/resources/graphql/workspace-system-prompt.graphqls`
- Create: `.../platform-ai-workspace-prompt-graphql/src/main/java/com/bytechef/ee/platform/ai/workspaceprompt/web/graphql/WorkspaceSystemPromptGraphQlController.java`
- Test: `.../platform-ai-workspace-prompt-graphql/src/test/java/com/bytechef/ee/platform/ai/workspaceprompt/web/graphql/WorkspaceSystemPromptGraphQlControllerTest.java`
- Modify: `server/apps/server-app/build.gradle.kts` (next to the `platform-ai-guardrails-graphql` line at ~303)

**Interfaces:**
- Consumes: Task 1's service interface, Task 1's domain record.
- Produces: GraphQL `workspaceSystemPrompt(workspaceId: ID!)` query and `updateWorkspaceSystemPrompt(input)` mutation — consumed by the client in Tasks 11–12.

- [ ] **Step 1: Write the schema**

`workspace-system-prompt.graphqls`:

```graphql
extend type Query {
    workspaceSystemPrompt(workspaceId: ID!): WorkspaceSystemPrompt
}

extend type Mutation {
    updateWorkspaceSystemPrompt(input: WorkspaceSystemPromptInput!): WorkspaceSystemPrompt
}

type WorkspaceSystemPrompt {
    workspaceId: ID!
    prompt: String!
}

input WorkspaceSystemPromptInput {
    workspaceId: ID!
    prompt: String
}
```

- [ ] **Step 2: Write the failing controller test**

```java
/*
 * (EE license header)
 */

package com.bytechef.ee.platform.ai.workspaceprompt.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.workspaceprompt.domain.WorkspaceSystemPrompt;
import com.bytechef.ee.platform.ai.workspaceprompt.service.WorkspaceSystemPromptService;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 */
class WorkspaceSystemPromptGraphQlControllerTest {

    private final WorkspaceSystemPromptService service = mock(WorkspaceSystemPromptService.class);
    private final WorkspaceSystemPromptGraphQlController controller =
        new WorkspaceSystemPromptGraphQlController(service);

    @Test
    void testQueryReturnsNullWhenUnset() {
        when(service.fetchWorkspaceSystemPrompt(7L)).thenReturn(Optional.empty());

        assertThat(controller.workspaceSystemPrompt(7L)).isNull();
    }

    @Test
    void testQueryReturnsStoredPrompt() {
        when(service.fetchWorkspaceSystemPrompt(7L)).thenReturn(Optional.of("Be concise."));

        assertThat(controller.workspaceSystemPrompt(7L))
            .isEqualTo(new WorkspaceSystemPrompt(7L, "Be concise."));
    }

    @Test
    void testUpdateSavesAndEchoes() {
        when(service.saveWorkspaceSystemPrompt(7L, "Be concise.")).thenReturn(Optional.of("Be concise."));

        WorkspaceSystemPrompt result = controller.updateWorkspaceSystemPrompt(
            new WorkspaceSystemPromptGraphQlController.WorkspaceSystemPromptInput(7L, "Be concise."));

        assertThat(result).isEqualTo(new WorkspaceSystemPrompt(7L, "Be concise."));
    }

    @Test
    void testUpdateBlankReturnsNull() {
        when(service.saveWorkspaceSystemPrompt(7L, "   ")).thenReturn(Optional.empty());

        assertThat(controller.updateWorkspaceSystemPrompt(
            new WorkspaceSystemPromptGraphQlController.WorkspaceSystemPromptInput(7L, "   "))).isNull();
    }

    @Test
    void testMutationIsAdminGated() throws NoSuchMethodException {
        Method method = WorkspaceSystemPromptGraphQlController.class.getMethod(
            "updateWorkspaceSystemPrompt", WorkspaceSystemPromptGraphQlController.WorkspaceSystemPromptInput.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains("ROLE_ADMIN");
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-workspace-prompt:platform-ai-workspace-prompt-graphql:test > /tmp/t6.log 2>&1
echo $?
```

- [ ] **Step 4: Write the controller**

```java
/*
 * (EE license header)
 */

package com.bytechef.ee.platform.ai.workspaceprompt.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.platform.ai.workspaceprompt.domain.WorkspaceSystemPrompt;
import com.bytechef.ee.platform.ai.workspaceprompt.service.WorkspaceSystemPromptService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL surface for the workspace system prompt settings page. Read is scoped to workspace members (or a tenant
 * admin) via the same {@code AI_GATEWAY_VIEW} workspace permission every other AI-settings read reuses; the mutation is
 * admin-only on the controller itself (guardrails/A2A precedent — no facade layer owns the check here).
 *
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
class WorkspaceSystemPromptGraphQlController {

    private final WorkspaceSystemPromptService workspaceSystemPromptService;

    @SuppressFBWarnings("EI")
    WorkspaceSystemPromptGraphQlController(WorkspaceSystemPromptService workspaceSystemPromptService) {
        this.workspaceSystemPromptService = workspaceSystemPromptService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')")
    public @Nullable WorkspaceSystemPrompt workspaceSystemPrompt(@Argument long workspaceId) {
        return workspaceSystemPromptService.fetchWorkspaceSystemPrompt(workspaceId)
            .map(prompt -> new WorkspaceSystemPrompt(workspaceId, prompt))
            .orElse(null);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public @Nullable WorkspaceSystemPrompt updateWorkspaceSystemPrompt(@Argument WorkspaceSystemPromptInput input) {
        return workspaceSystemPromptService.saveWorkspaceSystemPrompt(input.workspaceId(), input.prompt())
            .map(prompt -> new WorkspaceSystemPrompt(input.workspaceId(), prompt))
            .orElse(null);
    }

    public record WorkspaceSystemPromptInput(long workspaceId, @Nullable String prompt) {
    }
}
```

Add the missing import `com.bytechef.platform.annotation.ConditionalOnEEVersion`.

- [ ] **Step 5: Run the test to verify it passes**

- [ ] **Step 6: Register the graphql module in server-app**

In `server/apps/server-app/build.gradle.kts`, next to the `platform-ai-guardrails-graphql` implementation line (~303):

```kotlin
implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-workspace-prompt:platform-ai-workspace-prompt-graphql"))
```

Then check whether `platform-ai-guardrails-service` is registered anywhere besides via ai-hub-service:

```bash
grep -rn "platform-ai-guardrails-service" server/apps server/ee/apps --include="build.gradle.kts"
```

Mirror every hit with the equivalent `platform-ai-workspace-prompt-service` line, so the engine/provider beans exist wherever the guardrails engine does.

- [ ] **Step 7: Commit**

```bash
git add server/ee/libs/platform/platform-ai/platform-ai-workspace-prompt server/apps/server-app/build.gradle.kts
git commit -m "Expose the workspace system prompt over GraphQL"
```

---

### Task 7: AI Hub main-agent seam (TDD)

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/build.gradle.kts` (dependency, next to the guardrails-service line at 91)
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/agent/AiHubSpringAIAgent.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubConfiguration.java` (the two agent bean methods at ~227 and ~421)
- Test: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/agent/AiHubSpringAIAgentWorkspaceSystemPromptTest.java`

**Interfaces:**
- Consumes: Task 3's `WorkspaceSystemPrompts.fetchPrompt`, Task 4's `WorkspaceSystemPromptAdvisor`, existing `AiHubStateKeys.VERIFIED_WORKSPACE_ID`, the builder pattern at `AiHubSpringAIAgent.Builder.aiGuardrails(...)` (~line 740).
- Produces: builder method `workspaceSystemPrompts(@Nullable WorkspaceSystemPrompts)`; `resolveChatClient` now attaches both advisors.

- [ ] **Step 1: Add the dependency**

In `ai-hub-service/build.gradle.kts`, next to line 91:

```kotlin
implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-workspace-prompt:platform-ai-workspace-prompt-service"))
```

- [ ] **Step 2: Write the failing test**

The harness (the `CapturingChatModel`, `runInput(State)`, and `streamAssistantReply` helpers) is copied verbatim from `AiHubSpringAIAgentGuardrailsTest.java` — that file is the pattern's source of truth; keep the copies private to this class.

```java
/*
 * (EE license header)
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agui.core.agent.RunAgentInput;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.ee.platform.ai.workspaceprompt.WorkspaceSystemPrompts;
import com.bytechef.ee.platform.ai.workspaceprompt.advisor.WorkspaceSystemPromptAdvisor;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * Pins {@link AiHubSpringAIAgent#resolveChatClient} as the seam that also attaches the workspace's
 * {@link WorkspaceSystemPromptAdvisor}: the prompt-set path appends the section, the no-prompt path skips the
 * {@code mutate()} entirely.
 *
 * @version ee
 */
class AiHubSpringAIAgentWorkspaceSystemPromptTest {

    private static final Long WORKSPACE_ID = 7L;

    private final WorkspaceSystemPrompts workspaceSystemPrompts = mock(WorkspaceSystemPrompts.class);

    @Test
    void testResolveChatClientAppendsWorkspacePromptOnDefaultChatClient() throws AGUIException {
        when(workspaceSystemPrompts.fetchPrompt(WORKSPACE_ID)).thenReturn("Always answer in German.");

        CapturingChatModel capturingChatModel = new CapturingChatModel();

        AiHubSpringAIAgent agent = AiHubSpringAIAgent.builder()
            .agentId("ai_hub_ask")
            .chatModel(capturingChatModel)
            .systemMessage("Base prompt.")
            .state(new State())
            .workspaceSystemPrompts(workspaceSystemPrompts)
            .build();

        ChatClient chatClient = agent.resolveChatClient(runInput());

        List<ChatResponse> responses = streamAssistantReply(chatClient)
            .collectList()
            .block();

        assertThat(responses).isNotEmpty();
        assertThat(capturingChatModel.receivedPrompts).hasSize(1);

        String systemText = systemText(capturingChatModel.receivedPrompts.getFirst());

        assertThat(systemText).contains(WorkspaceSystemPromptAdvisor.WORKSPACE_INSTRUCTIONS_HEADER);
        assertThat(systemText).endsWith("Always answer in German.");
    }

    @Test
    void testResolveChatClientSkipsAdvisorWhenNoPrompt() throws AGUIException {
        when(workspaceSystemPrompts.fetchPrompt(WORKSPACE_ID)).thenReturn(null);

        CapturingChatModel capturingChatModel = new CapturingChatModel();

        AiHubSpringAIAgent agent = AiHubSpringAIAgent.builder()
            .agentId("ai_hub_ask")
            .chatModel(capturingChatModel)
            .systemMessage("Base prompt.")
            .state(new State())
            .workspaceSystemPrompts(workspaceSystemPrompts)
            .build();

        ChatClient chatClient = agent.resolveChatClient(runInput());

        List<ChatResponse> responses = streamAssistantReply(chatClient)
            .collectList()
            .block();

        assertThat(responses).isNotEmpty();

        String systemText = systemText(capturingChatModel.receivedPrompts.getFirst());

        assertThat(systemText).doesNotContain(WorkspaceSystemPromptAdvisor.WORKSPACE_INSTRUCTIONS_HEADER);
    }

    /**
     * Backward-compatibility guard: with no engine wired (module absent), {@code resolveChatClient} keeps returning a
     * working {@link ChatClient}.
     */
    @Test
    void testResolveChatClientSkipsAdvisorWhenEngineAbsent() throws AGUIException {
        CapturingChatModel capturingChatModel = new CapturingChatModel();

        AiHubSpringAIAgent agent = AiHubSpringAIAgent.builder()
            .agentId("ai_hub_ask")
            .chatModel(capturingChatModel)
            .systemMessage("Base prompt.")
            .state(new State())
            .build();

        ChatClient chatClient = agent.resolveChatClient(runInput());

        List<ChatResponse> responses = streamAssistantReply(chatClient)
            .collectList()
            .block();

        assertThat(responses).isNotEmpty();
        assertThat(capturingChatModel.receivedPrompts).hasSize(1);
    }

    private static String systemText(Prompt prompt) {
        return prompt.getInstructions()
            .stream()
            .filter(message -> message.getMessageType() == MessageType.SYSTEM)
            .map(Message::getText)
            .findFirst()
            .orElseThrow();
    }

    private static RunAgentInput runInput() {
        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, WORKSPACE_ID);

        UserMessage userMessage = new UserMessage();

        userMessage.setContent("Tell me about the project");

        return new RunAgentInput(
            "thread", "run", state, List.of((BaseMessage) userMessage), List.of(), List.of(), null);
    }

    private static Flux<ChatResponse> streamAssistantReply(ChatClient chatClient) {
        return chatClient.prompt()
            .system("You are a helpful assistant.")
            .user("Tell me about the project")
            .stream()
            .chatResponse();
    }

    private static final class CapturingChatModel implements ChatModel {

        private final List<Prompt> receivedPrompts = new ArrayList<>();

        @Override
        public ChatResponse call(Prompt prompt) {
            receivedPrompts.add(prompt);

            return cannedResponse();
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            receivedPrompts.add(prompt);

            return Flux.just(cannedResponse());
        }

        private static ChatResponse cannedResponse() {
            return ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage("OK"))))
                .build();
        }
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

```bash
./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests "com.bytechef.ee.ai.hub.agent.AiHubSpringAIAgentWorkspaceSystemPromptTest" > /tmp/t7.log 2>&1
echo $?
```

Expected: compile failure — no `workspaceSystemPrompts` builder method.

- [ ] **Step 4: Implement the seam in `AiHubSpringAIAgent`**

1. Add field (next to `aiGuardrails` at ~137): `private final @Nullable WorkspaceSystemPrompts workspaceSystemPrompts;` and assign in the constructor from `builder.workspaceSystemPrompts`.
2. Add builder field + method (next to `aiGuardrails(...)` at ~740):

```java
/**
 * Wires the workspace system prompt engine so {@link #resolveChatClient} appends the admin's standing instructions
 * to every LLM turn — see {@link #attachWorkspaceSystemPromptAdvisor}. Null (module absent) disables the overlay.
 */
public Builder workspaceSystemPrompts(@Nullable WorkspaceSystemPrompts workspaceSystemPrompts) {
    this.workspaceSystemPrompts = workspaceSystemPrompts;

    return this;
}
```

3. Change `resolveChatClient`:

```java
@Override
protected ChatClient resolveChatClient(RunAgentInput input) {
    ChatClient chatClient = attachGuardrailsAdvisor(resolveConfiguredChatClient(input), input);

    return attachWorkspaceSystemPromptAdvisor(chatClient, input);
}
```

4. Add the attach method (next to `attachGuardrailsAdvisor`):

```java
/**
 * Registers a fresh, workspace-bound {@link WorkspaceSystemPromptAdvisor} so the admin's standing instructions are
 * appended to this turn's system message. Self-orders AFTER the guardrails advisor (HIGHEST_PRECEDENCE + 100), so
 * the admin text is never redacted or blocked by the workspace's own guardrail policy. A missing engine bean
 * (module absent) or a workspace without a prompt (the common case — {@code fetchPrompt} is memoized) skips the
 * {@code mutate()} entirely so the no-op case pays no per-turn overhead.
 */
private ChatClient attachWorkspaceSystemPromptAdvisor(ChatClient chatClient, RunAgentInput input) {
    if (workspaceSystemPrompts == null) {
        return chatClient;
    }

    State state = input.state();
    Long workspaceId = state == null ? null : NumberUtils.asLong(state.get(AiHubStateKeys.VERIFIED_WORKSPACE_ID));

    if (workspaceSystemPrompts.fetchPrompt(workspaceId) == null) {
        return chatClient;
    }

    return chatClient.mutate()
        .defaultAdvisors(new WorkspaceSystemPromptAdvisor(workspaceSystemPrompts, workspaceId))
        .build();
}
```

- [ ] **Step 5: Wire it in `AiHubConfiguration`'s two agent bean methods**

Both `aiHubAskSpringAIAgent` (~227) and `aiHubBuildSpringAIAgent` (~421) gain a parameter `ObjectProvider<WorkspaceSystemPrompts> workspaceSystemPromptsProvider`, resolve `WorkspaceSystemPrompts workspaceSystemPrompts = workspaceSystemPromptsProvider.getIfAvailable();` next to the existing `aiGuardrails` resolution (~242/~433), and add `.workspaceSystemPrompts(workspaceSystemPrompts)` to the agent builder chain right after `.aiGuardrails(...)`. Keep the resolved local — Task 8 threads the same variable into the wrap sites.

- [ ] **Step 6: Run the test to verify it passes** (same command as Step 3)

- [ ] **Step 7: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service
git commit -m "Append the workspace system prompt on all AI Hub main-agent LLM turns"
```

---

### Task 8: Subagent delegate seam (TDD)

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/guardrails/SubAgentGuardrailedChatClient.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubConfiguration.java` (every `SubAgentGuardrailedChatClient.wrap(...)` call — ~18 sites; `grep -n "SubAgentGuardrailedChatClient.wrap"` lists them all)
- Test: extend `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/guardrails/SubAgentGuardrailedChatClientTest.java`

**Interfaces:**
- Consumes: Tasks 3–4, existing `AgentToolInvocationContext.fromToolContext` workspace resolution.
- Produces: new signature `wrap(ChatClient, @Nullable AiGuardrails, @Nullable AiGuardrailMetrics, @Nullable WorkspaceSystemPrompts)` — ALL call sites must pass the fourth argument.

- [ ] **Step 1: Write the failing tests** (add to the existing test class — its harness builds a REAL `ChatClient` over a private `CapturingChatModel` with a `receivedPrompts` list, and forwards the workspace id via `.toolContext(Map.of(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID))`)

Three new test methods (new imports: `WorkspaceSystemPrompts`, `WorkspaceSystemPromptAdvisor`, `MessageType`, `Message`):

```java
@Test
void testWrapAttachesWorkspaceSystemPromptAdvisorWhenPromptSet() {
    WorkspaceSystemPrompts workspaceSystemPrompts = mock(WorkspaceSystemPrompts.class);

    when(workspaceSystemPrompts.fetchPrompt(WORKSPACE_ID)).thenReturn("Always answer in German.");

    CapturingChatModel capturingChatModel = new CapturingChatModel();
    ChatClient inner = ChatClient.builder(capturingChatModel)
        .build();

    ChatClient guarded = SubAgentGuardrailedChatClient.wrap(inner, null, null, workspaceSystemPrompts);

    Map<String, Object> forwardedContext =
        Map.of(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID);

    String content = guarded.prompt()
        .system("Base prompt.")
        .user("hi")
        .toolContext(forwardedContext)
        .call()
        .content();

    assertThat(content).isEqualTo("OK");

    String forwardedSystemText = capturingChatModel.receivedPrompts.getFirst()
        .getInstructions()
        .stream()
        .filter(message -> message.getMessageType() == MessageType.SYSTEM)
        .map(Message::getText)
        .findFirst()
        .orElseThrow();

    assertThat(forwardedSystemText).contains(WorkspaceSystemPromptAdvisor.WORKSPACE_INSTRUCTIONS_HEADER);
    assertThat(forwardedSystemText).endsWith("Always answer in German.");
}

@Test
void testWrapSkipsPromptAdvisorWhenWorkspaceHasNoPrompt() {
    WorkspaceSystemPrompts workspaceSystemPrompts = mock(WorkspaceSystemPrompts.class);

    when(workspaceSystemPrompts.fetchPrompt(WORKSPACE_ID)).thenReturn(null);

    CapturingChatModel capturingChatModel = new CapturingChatModel();
    ChatClient inner = ChatClient.builder(capturingChatModel)
        .build();

    ChatClient guarded = SubAgentGuardrailedChatClient.wrap(inner, null, null, workspaceSystemPrompts);

    Map<String, Object> forwardedContext =
        Map.of(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID);

    String content = guarded.prompt()
        .system("Base prompt.")
        .user("hi")
        .toolContext(forwardedContext)
        .call()
        .content();

    assertThat(content).isEqualTo("OK");

    String forwardedSystemText = capturingChatModel.receivedPrompts.getFirst()
        .getInstructions()
        .stream()
        .filter(message -> message.getMessageType() == MessageType.SYSTEM)
        .map(Message::getText)
        .findFirst()
        .orElseThrow();

    assertThat(forwardedSystemText).doesNotContain(WorkspaceSystemPromptAdvisor.WORKSPACE_INSTRUCTIONS_HEADER);
}

@Test
void testWrapReturnsUnwrappedWhenBothEnginesAbsent() {
    ChatClient inner = ChatClient.builder(new CapturingChatModel())
        .build();

    assertThat(SubAgentGuardrailedChatClient.wrap(inner, null, null, null)).isSameAs(inner);
}
```

Also update every existing 3-arg `wrap(...)` call in the test file to pass `null` as the fourth argument (the pre-existing `testWrapReturnsChatClientUnchangedWhenAiGuardrailsAbsent` becomes a 4-null-arg call and stays green).

- [ ] **Step 2: Run to verify failure** (compile failure — 3-arg wrap)

```bash
./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:compileTestJava > /tmp/t8.log 2>&1
echo $?
```

- [ ] **Step 3: Generalize `SubAgentGuardrailedChatClient`**

1. Add field `private final @Nullable WorkspaceSystemPrompts workspaceSystemPrompts;`, make `aiGuardrails`/`aiGuardrailMetrics` fields `@Nullable`.
2. New `wrap` signature:

```java
public static ChatClient wrap(
    ChatClient chatClient, @Nullable AiGuardrails aiGuardrails, @Nullable AiGuardrailMetrics aiGuardrailMetrics,
    @Nullable WorkspaceSystemPrompts workspaceSystemPrompts) {

    boolean guardrailsPresent = aiGuardrails != null && aiGuardrailMetrics != null;

    if (!guardrailsPresent && workspaceSystemPrompts == null) {
        return chatClient;
    }

    return new SubAgentGuardrailedChatClient(
        chatClient, guardrailsPresent ? aiGuardrails : null, guardrailsPresent ? aiGuardrailMetrics : null,
        workspaceSystemPrompts);
}
```

3. Rename `attachGuardrailAdvisorIfActive` → `attachWorkspaceAdvisorsIfActive` and extend:

```java
private void attachWorkspaceAdvisorsIfActive() {
    Long workspaceId = resolveWorkspaceId(capturedToolContext);

    if (aiGuardrails != null && aiGuardrailMetrics != null && aiGuardrails.isActive(workspaceId)) {
        delegateSpec = delegateSpec.advisors(
            new AiGuardrailsAdvisor(aiGuardrails, workspaceId, aiGuardrailMetrics));
    }

    if (workspaceSystemPrompts != null && workspaceSystemPrompts.fetchPrompt(workspaceId) != null) {
        delegateSpec = delegateSpec.advisors(
            new WorkspaceSystemPromptAdvisor(workspaceSystemPrompts, workspaceId));
    }
}
```

4. Update the class javadoc: it now attaches the workspace's per-request advisors (guardrails + system prompt); keep the class name (renaming would churn 18 call sites and the git history for no behavior gain — note this in the javadoc).

- [ ] **Step 4: Update all `wrap(...)` call sites in `AiHubConfiguration`**

Thread the `workspaceSystemPrompts` local resolved in Task 7 Step 5 through the same helper-method parameters that already carry `aiGuardrails, aiGuardrailMetrics` (the registration helpers `registerSubAgentToolCallbacks`, `registerManagerSubAgentToolCallbacks`, `registerCopilotSubAgentToolCallbacks`, and the inline sites — everywhere `grep -n "SubAgentGuardrailedChatClient.wrap"` hits), appending it as the fourth `wrap` argument at every site. This is a purely mechanical companion-parameter addition; do not change any other argument.

- [ ] **Step 5: Run the module's tests**

```bash
./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test > /tmp/t8b.log 2>&1
echo $?
grep "^> Task .* FAILED" /tmp/t8b.log
```

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service
git commit -m "Append the workspace system prompt on subagent delegate LLM calls"
```

---

### Task 9: Canvas AI Agent seam (TDD)

**Files:**
- Modify: `server/libs/modules/components/ai/agent/src/main/java/com/bytechef/component/ai/agent/action/AbstractAiAgentChatAction.java`
- Modify: `server/libs/modules/components/ai/agent/src/main/java/com/bytechef/component/ai/agent/action/AiAgentChatAction.java` (and `AiAgentStreamChatAction` / `AiAgentRealtimeChatAction` IF their `of(...)` factories take the guardrails provider — mirror exactly what each does today)
- Modify: `server/libs/modules/components/ai/agent/src/main/java/com/bytechef/component/ai/agent/AiAgentComponentHandler.java`
- Test: `server/libs/modules/components/ai/agent/src/test/java/com/bytechef/component/ai/agent/action/...` — mirror wherever the existing guardrails-provider attach test lives (`grep -rn "AiGuardrailsAdvisorProvider" server/libs/modules/components/ai/agent/src/test`); if none exists, add `AbstractAiAgentChatActionWorkspaceSystemPromptTest` asserting the provider's advisor lands in the request spec.

**Interfaces:**
- Consumes: Task 5's CE SPI `WorkspaceSystemPromptAdvisorProvider` (from `platform-ai-api`, already a dependency of this component — verify with `grep platform-ai-api server/libs/modules/components/ai/agent/build.gradle.kts`, add if missing).
- Produces: the canvas agent attaches the workspace prompt advisor on every run with a resolvable AUTOMATION workspace.

- [ ] **Step 1: Extend `AbstractAiAgentChatAction`**

1. New field: `private final @Nullable ObjectProvider<WorkspaceSystemPromptAdvisorProvider> workspaceSystemPromptAdvisorProviderObjectProvider;`
2. New 6-arg constructor (the existing 5-arg one chains to it with `null`):

```java
protected AbstractAiAgentChatAction(
    AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService,
    ToolCallingManager toolCallingManager,
    @Nullable ObjectProvider<ToolExecutionRecorder> toolExecutionRecorderObjectProvider,
    @Nullable ObjectProvider<AiGuardrailsAdvisorProvider> aiGuardrailsAdvisorProviderObjectProvider,
    @Nullable ObjectProvider<WorkspaceSystemPromptAdvisorProvider> workspaceSystemPromptAdvisorProviderObjectProvider) {
    ...assign all six...
}
```

3. In `getChatClientRequestSpec` (~line 259), rename the local `guardrailsAdvisors` → `workspaceAdvisors` and add after the existing guardrails block:

```java
if (workspaceSystemPromptAdvisorProviderObjectProvider != null
    && context instanceof ActionContextAware actionContextAware) {
    workspaceSystemPromptAdvisorProviderObjectProvider.ifAvailable(
        provider -> provider
            .getAdvisor(actionContextAware.getPlatformType(), actionContextAware.getJobPrincipalId(),
                "ai_agent")
            .ifPresent(workspaceAdvisors::add));
}
```

(The comment above the block should note: the system-prompt advisor self-orders AFTER the guardrails advisor, so listing both in one list is order-safe.)

- [ ] **Step 2: Thread the provider through the concrete actions and handler**

- Every `of(...)` factory / constructor among `AiAgentChatAction`, `AiAgentStreamChatAction`, `AiAgentRealtimeChatAction` that currently takes `ObjectProvider<AiGuardrailsAdvisorProvider>` gains a trailing `@Nullable ObjectProvider<WorkspaceSystemPromptAdvisorProvider>` parameter, passed straight through to `super(...)`.
- `AiAgentComponentHandler`'s constructor (~line 52) gains `ObjectProvider<WorkspaceSystemPromptAdvisorProvider> workspaceSystemPromptAdvisorProviderObjectProvider` and forwards it to each factory it already passes the guardrails provider to.

- [ ] **Step 3: Write/extend the test**

If a guardrails attach test exists in this module, clone its shape for the new provider. Otherwise minimal new test: mock `WorkspaceSystemPromptAdvisorProvider` returning `Optional.of(advisor)`, build the action through the 6-arg constructor with an `ActionContextAware` mock (`getPlatformType()` → `AUTOMATION`, `getJobPrincipalId()` → 42L), call `getChatClientRequestSpec`, and assert via the returned spec (or an `ArgumentCaptor` on a mocked ChatClient builder — mirror the module's existing test tooling) that the advisor was added. If the module's test harness makes this impractical (heavy `ClusterElementMap` fixtures), fall back to asserting the seam indirectly: unit-test that the new block calls `provider.getAdvisor(AUTOMATION, 42L, "ai_agent")` exactly once — that is the load-bearing contract.

- [ ] **Step 4: Compile + run module tests**

```bash
./gradlew :server:libs:modules:components:ai:agent:test > /tmp/t9.log 2>&1
echo $?
grep "^> Task .* FAILED" /tmp/t9.log
```

- [ ] **Step 5: Commit**

```bash
git add server/libs/modules/components/ai/agent
git commit -m "Append the workspace system prompt on canvas AI Agent runs"
```

---

### Task 10: Server-wide verification

- [ ] **Step 1: Format**

```bash
./gradlew spotlessApply > /tmp/t10a.log 2>&1
echo $?
```

- [ ] **Step 2: Full compile with `--continue`**

```bash
./gradlew compileJava compileTestJava --continue > /tmp/t10b.log 2>&1
echo $?
grep "^> Task .* FAILED" /tmp/t10b.log
```

Fix anything that surfaces (most likely: an EE remote-client app now missing the `WorkspaceSystemPrompts` bean — the seams all use `ObjectProvider`/`@Nullable`, so a missing bean must degrade to no-op, never to a startup failure; if an EE app fails DI, the fix is an `ObjectProvider`, not a stub bean).

- [ ] **Step 3: Run the touched modules' checks**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-workspace-prompt:platform-ai-workspace-prompt-api:check \
  :server:ee:libs:platform:platform-ai:platform-ai-workspace-prompt:platform-ai-workspace-prompt-service:check \
  :server:ee:libs:platform:platform-ai:platform-ai-workspace-prompt:platform-ai-workspace-prompt-graphql:check \
  :server:ee:libs:ai:ai-hub:ai-hub-service:test \
  :server:libs:modules:components:ai:agent:test \
  --continue > /tmp/t10c.log 2>&1
echo $?
grep "^> Task .* FAILED" /tmp/t10c.log
```

- [ ] **Step 4: Commit any spotless/checkstyle fixups**

```bash
git add -u server/
git commit -m "Apply spotless and static-analysis fixups for the workspace system prompt"
```

(Skip the commit if `git status` shows nothing staged.)

---

### Task 11: Client GraphQL operations + codegen

**Files:**
- Modify: `client/codegen.ts` (schema array, next to the guardrails line at ~103)
- Create: `client/src/graphql/platform/workspace-system-prompt/workspaceSystemPrompt.graphql`
- Regenerate: `client/src/shared/middleware/graphql.ts` (committed separately)

- [ ] **Step 1: Add the schema path to `client/codegen.ts`**

Next to the guardrails entry:

```ts
'../server/ee/libs/platform/platform-ai/platform-ai-workspace-prompt/platform-ai-workspace-prompt-graphql/src/main/resources/graphql/*.graphqls',
```

- [ ] **Step 2: Create the operations file**

`client/src/graphql/platform/workspace-system-prompt/workspaceSystemPrompt.graphql`:

```graphql
query workspaceSystemPrompt($workspaceId: ID!) {
    workspaceSystemPrompt(workspaceId: $workspaceId) {
        prompt
        workspaceId
    }
}

mutation updateWorkspaceSystemPrompt($input: WorkspaceSystemPromptInput!) {
    updateWorkspaceSystemPrompt(input: $input) {
        prompt
        workspaceId
    }
}
```

- [ ] **Step 3: Regenerate and verify**

```bash
cd client
npx graphql-codegen
grep -n "useWorkspaceSystemPromptQuery\|useUpdateWorkspaceSystemPromptMutation" src/shared/middleware/graphql.ts | head -3
```

Expected: both hooks present.

- [ ] **Step 4: Commit — operations and generated file separately**

```bash
git add client/codegen.ts client/src/graphql/platform/workspace-system-prompt
git commit -m "0 client - Add the workspace system prompt GraphQL operations"
git add client/src/shared/middleware/graphql.ts
git commit -m "0 client - Regenerate the GraphQL client for the workspace system prompt"
```

---

### Task 12: Client settings page + route (TDD)

**Files:**
- Create: `client/src/ee/pages/settings/automation/ai/system-prompt/WorkspaceSystemPrompt.tsx`
- Test: `client/src/ee/pages/settings/automation/ai/system-prompt/WorkspaceSystemPrompt.test.tsx`
- Modify: `client/src/routes.tsx` (lazy import next to `AiGuardrails` ~line 132; route entry + nav item next to the `ai/guardrails` entries)

**Interfaces:**
- Consumes: Task 11's `useWorkspaceSystemPromptQuery` / `useUpdateWorkspaceSystemPromptMutation`, `useWorkspaceStore`.

- [ ] **Step 1: Write the failing test** (mirror `AiGuardrails.test.tsx`'s hoisted-mock harness exactly)

```tsx
import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkspaceSystemPrompt from './WorkspaceSystemPrompt';

const {invalidateQueriesMock, mutateMock, queryMock} = vi.hoisted(() => ({
    invalidateQueriesMock: vi.fn(),
    mutateMock: vi.fn(),
    queryMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useUpdateWorkspaceSystemPromptMutation: () => ({isPending: false, mutate: mutateMock}),
    useWorkspaceSystemPromptQuery: (...args: unknown[]) => queryMock(...args),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: invalidateQueriesMock}),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn(() => 123),
}));

describe('WorkspaceSystemPrompt', () => {
    beforeEach(() => {
        mutateMock.mockClear();
        invalidateQueriesMock.mockClear();

        queryMock.mockReturnValue({
            data: {workspaceSystemPrompt: {prompt: 'Always answer in German.', workspaceId: '123'}},
            error: null,
            isLoading: false,
        });
    });

    it('renders the stored prompt and its character count', () => {
        render(<WorkspaceSystemPrompt />);

        expect(screen.getByLabelText('Workspace system prompt')).toHaveValue('Always answer in German.');
        expect(screen.getByText('24 / 4000')).toBeInTheDocument();
    });

    it('renders empty when the query returns null (no prompt set yet)', () => {
        queryMock.mockReturnValue({
            data: {workspaceSystemPrompt: null},
            error: null,
            isLoading: false,
        });

        render(<WorkspaceSystemPrompt />);

        expect(screen.getByLabelText('Workspace system prompt')).toHaveValue('');
        expect(screen.getByText('0 / 4000')).toBeInTheDocument();
    });

    it('saves the edited prompt', () => {
        render(<WorkspaceSystemPrompt />);

        fireEvent.change(screen.getByLabelText('Workspace system prompt'), {target: {value: 'Be concise.'}});
        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(mutateMock).toHaveBeenCalledWith({
            input: {prompt: 'Be concise.', workspaceId: '123'},
        });
    });

    it('saves undefined when cleared, deleting the stored prompt', () => {
        render(<WorkspaceSystemPrompt />);

        fireEvent.change(screen.getByLabelText('Workspace system prompt'), {target: {value: '   '}});
        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(mutateMock).toHaveBeenCalledWith({
            input: {prompt: undefined, workspaceId: '123'},
        });
    });
});
```

- [ ] **Step 2: Run to verify failure**

```bash
cd client
npx vitest run src/ee/pages/settings/automation/ai/system-prompt
```

- [ ] **Step 3: Write the page**

```tsx
import Button from '@/components/Button/Button';
import PageLoader from '@/components/PageLoader';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    useUpdateWorkspaceSystemPromptMutation,
    useWorkspaceSystemPromptQuery,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useState} from 'react';
import {toast} from 'sonner';

const MAX_LENGTH = 4000;

const WorkspaceSystemPrompt = () => {
    const [prompt, setPrompt] = useState('');

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const {data, error, isLoading} = useWorkspaceSystemPromptQuery(
        {workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : ''},
        {enabled: currentWorkspaceId != null}
    );

    const updateMutation = useUpdateWorkspaceSystemPromptMutation({
        onSuccess: () => {
            toast.success('Workspace system prompt saved');

            queryClient.invalidateQueries({queryKey: ['workspaceSystemPrompt']});
        },
    });

    useEffect(() => {
        setPrompt(data?.workspaceSystemPrompt?.prompt ?? '');
    }, [data]);

    const handleSave = () => {
        if (currentWorkspaceId == null) {
            return;
        }

        const trimmed = prompt.trim();

        updateMutation.mutate({
            input: {
                prompt: trimmed.length > 0 ? trimmed : undefined,
                workspaceId: String(currentWorkspaceId),
            },
        });
    };

    return (
        <LayoutContainer header={<Header centerTitle position="main" title="System Prompt" />} leftSidebarOpen={false}>
            <PageLoader errors={[error]} loading={isLoading}>
                <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
                    <div className="space-y-6 py-6">
                        <p className="text-sm text-muted-foreground">
                            Standing instructions appended to every AI agent operating in this workspace: AI Hub chat
                            (copilot and personal agents), its specialist subagents, and canvas AI Agent runs. Each
                            agent&apos;s own prompt and safety rules always come first — these instructions cannot
                            override them.
                        </p>

                        <fieldset className="space-y-2 border-0 p-0">
                            <Label htmlFor="workspace-system-prompt">Workspace system prompt</Label>

                            <Textarea
                                className="min-h-64 font-mono text-sm"
                                id="workspace-system-prompt"
                                maxLength={MAX_LENGTH}
                                onChange={(event) => setPrompt(event.target.value)}
                                placeholder="Example: Always answer in German. Our fiscal year starts in February."
                                value={prompt}
                            />

                            <p className="text-right text-xs text-muted-foreground">
                                {prompt.length} / {MAX_LENGTH}
                            </p>
                        </fieldset>

                        <div className="flex justify-end">
                            <Button
                                disabled={updateMutation.isPending}
                                label={updateMutation.isPending ? 'Saving...' : 'Save'}
                                onClick={handleSave}
                            />
                        </div>
                    </div>
                </div>
            </PageLoader>
        </LayoutContainer>
    );
};

export default WorkspaceSystemPrompt;
```

- [ ] **Step 4: Run the test to verify it passes** (same command as Step 2)

- [ ] **Step 5: Register the route and nav item in `client/src/routes.tsx`**

Lazy import next to `AiGuardrails` (~line 132):

```ts
const WorkspaceSystemPrompt = lazy(
    () => import('@/ee/pages/settings/automation/ai/system-prompt/WorkspaceSystemPrompt')
);
```

Route entry directly after the `ai/guardrails` route object:

```tsx
{
    element: (
        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
            <EEVersion>
                <LazyLoadWrapper>
                    <WorkspaceSystemPrompt />
                </LazyLoadWrapper>
            </EEVersion>
        </PrivateRoute>
    ),
    path: 'ai/system-prompt',
},
```

Nav item directly after the `ai/guardrails` nav entry:

```ts
{
    href: 'ai/system-prompt',
    title: 'AI System Prompt',
},
```

- [ ] **Step 6: Commit**

```bash
git add client/src/ee/pages/settings/automation/ai/system-prompt client/src/routes.tsx
git commit -m "0 client - Add the Workspace Settings AI System Prompt page"
```

---

### Task 13: Client verification + wrap-up

- [ ] **Step 1: Full client check**

```bash
cd client
npm run format
npm run check
```

Fix anything that surfaces (sort-keys and import-sort violations are the usual suspects; ESLint `--fix` does NOT fix sort-keys — fix by hand). Commit fixups as `0 client - Apply lint fixups for the workspace system prompt page`.

- [ ] **Step 2: Final server sanity**

```bash
./gradlew compileJava compileTestJava --continue > /tmp/t13.log 2>&1
echo $?
grep "^> Task .* FAILED" /tmp/t13.log
```

- [ ] **Step 3: Update the spec status line**

Change `Status: approved design, not yet implemented` to `Status: implemented` in `docs/superpowers/specs/2026-08-04-workspace-system-prompt-design.md`; commit as `Mark the workspace system prompt spec implemented`.

---

## Self-review notes (already applied)

- **Spec coverage:** storage/service (Tasks 1–2), engine + fail-open + TTL (3), advisor + pinned wording + ordering + idempotence (4), canvas SPI/provider with no-tenant-default semantics (5), GraphQL + gating (6), AI Hub seam incl. override clients (7), subagent delegates (8), canvas component (9), UI page + route + counter + clear-to-delete (11–12), out-of-scope items need no tasks.
- **Type consistency:** `fetchWorkspaceSystemPrompt(long)` / `saveWorkspaceSystemPrompt(long, String)` / `fetchPrompt(Long)` / `wrap(ChatClient, AiGuardrails, AiGuardrailMetrics, WorkspaceSystemPrompts)` used identically across Tasks 2–9.
- **Known judgment calls for the implementer:** exact `Property`/`Project`/`ProjectDeployment` construction in tests (copy from the named guardrails twin tests); whether `AiAgentStreamChatAction`/`AiAgentRealtimeChatAction` factories take the guardrails provider (mirror each file's current shape); the Task 9 test harness (mirror the module's existing test tooling, or pin the `getAdvisor(AUTOMATION, jobPrincipalId, "ai_agent")` call contract if the full request-spec fixture is impractical).
