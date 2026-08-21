# Session Chat Memory Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Promote `chat-memory-session` into a configurable `CHAT_MEMORY` cluster-element family that exposes the full Spring AI session-management surface (compaction, recall storage, branch isolation) and pluggably selects its storage backend (built-in / external JDBC / in-memory) and an optional summarizer model.

**Architecture:** `sessionChatMemory` is a `CHAT_MEMORY` cluster element **and** a cluster root. It resolves a required `SESSION_REPOSITORY` child (the storage backend) and an optional `MODEL` child (summarizer), then builds a `SessionMemoryAdvisor` plus optional `conversation_search` tools, returning them through an extended `ChatMemoryFunction.Result`. The AI Agent runtime injects the advisor unchanged (it is `CHAT_MEMORY`) and gains a small change to also merge memory-contributed tools onto the `ChatClient`.

**Tech Stack:** Java 25, Spring Boot, ByteChef ComponentDsl, `org.springaicommunity:spring-ai-session-management:0.4.2`, `spring-ai-session-jdbc:0.4.2`, JUnit 5 + `JsonFileAssert` snapshot tests.

---

## File Structure

**New SPI (platform-component-api):**
- `.../platform/component/definition/ai/agent/SessionRepositoryFunction.java` — new `SESSION_REPOSITORY` type + SPI.
- `.../platform/component/definition/ai/agent/ChatMemoryFunction.java` — extend `Result` with `toolCallbacks`.

**Agent runtime (modify):**
- `server/libs/modules/components/ai/agent/.../action/AbstractAiAgentChatAction.java` — build memory `Result` once, merge `toolCallbacks` into `.tools(...)`.

**Backend modules (new), each: `build.gradle.kts`, handler, cluster element, constants, asset svg, test + snapshot:**
- `chat-memory-in-memory-session` → `inMemorySessionChatMemory` (SESSION_REPOSITORY)
- `chat-memory-jdbc-session` → `jdbcSessionChatMemory` (SESSION_REPOSITORY + DATA_SOURCE child); also exports `SessionChatMemoryUtils` (schema init + repo build).
- `chat-memory-builtin-session` → `builtInSessionChatMemory` (SESSION_REPOSITORY, internal app DataSource), reuses jdbc-session utils.

**Parent module (rework existing `chat-memory-session`):**
- `SessionChatMemory.java` (cluster element) — full property set, child resolution, advisor+tools assembly.
- `SessionChatMemoryConstants.java` — property keys + compaction enum values.
- `SessionChatMemoryComponentHandler.java` — inject `ClusterElementDefinitionService` (drop direct repo building).
- `SessionChatMemoryUtils.java` — DELETE (moves to jdbc-session module).

**Registration / docs:**
- `settings.gradle.kts` — add 3 new modules.

---

## Phase 0 — Spike: verify editor nesting + tool context (do FIRST)

### Task 0: Verify the two unproven mechanisms

**Why:** Two assumptions underpin the design. Confirm them before building 4 modules on top.

- [ ] **Step 1: Confirm a `CHAT_MEMORY` element can host a non-`DATA_SOURCE` child in the editor.**

`jdbcChatMemory` already hosts a `DATA_SOURCE` child. We need `sessionChatMemory` to host a `SESSION_REPOSITORY` child AND a `MODEL` child. Search how the client/editor decides permitted nested cluster-element types:

Run:
```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/session-chat-memory
grep -rn "DATA_SOURCE\|clusterElementType\|nestedClusterElement\|ClusterElementType" client/src --include=*.ts --include=*.tsx -l | head
grep -rn "MODEL\|CHAT_MEMORY\|DATA_SOURCE" server/libs/platform --include=*.java -l | grep -i cluster | head
```
Expected: locate where allowed child types per cluster element are derived. Document whether nesting is unrestricted (any registered type) or declared per element.

- [ ] **Step 2: Decide nesting approach from findings.**

If nesting is unrestricted by type → no extra declaration needed (mirror `jdbcChatMemory`).
If nesting is declared per element → record the exact DSL/registration call required and add it to Tasks 5 & 7. Write the finding into `docs/superpowers/specs/2026-06-07-session-chat-memory-design.md` under a new "Nesting mechanism" note.

- [ ] **Step 3: Confirm conversation id reaches `@Tool` context for recall search.**

`SessionEventTools.SESSION_ID_CONTEXT_KEY = "chat_memory_conversation_id"` and `SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY = ChatMemory.CONVERSATION_ID`. Verify they are the same string and that the agent propagates advisor context → tool `ToolContext`:

Run:
```bash
javap -classpath "$(find ~/.gradle -name 'spring-ai-chat-*.jar' | head -1)" -constants org.springframework.ai.chat.memory.ChatMemory 2>/dev/null | grep CONVERSATION_ID
grep -rn "ToolContext\|toolContext\|getConversationAdvisor" server/libs/modules/components/ai/agent/src/main/java/com/bytechef/component/ai/agent/action/AbstractAiAgentChatAction.java
```
Expected: confirm `ChatMemory.CONVERSATION_ID == "chat_memory_conversation_id"`. If the agent does NOT pass the conversation id into tool context, add a task to set it (`.toolContext(Map.of(SessionEventTools.SESSION_ID_CONTEXT_KEY, conversationId))` on the request spec). Record the outcome in the spec.

- [ ] **Step 4: Commit findings.**
```bash
git add docs/superpowers/specs/2026-06-07-session-chat-memory-design.md
git commit -m "732 Record session memory nesting + tool-context spike findings"
```

> If Step 1 or 3 reveals a blocker, STOP and report before proceeding.

---

## Phase 1 — SPI foundations (`platform-component-api`)

### Task 1: New `SessionRepositoryFunction` SPI + `SESSION_REPOSITORY` type

**Files:**
- Create: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/ai/agent/SessionRepositoryFunction.java`

- [ ] **Step 1: Create the SPI file** (clone of `DataSourceFunction`, returning `SessionRepository`):

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

package com.bytechef.platform.component.definition.ai.agent;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import java.util.Map;
import org.springframework.ai.session.SessionRepository;

/**
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface SessionRepositoryFunction {

    ClusterElementType SESSION_REPOSITORY =
        new ClusterElementType("SESSION_REPOSITORY", "sessionRepository", "Session Repository", true);

    SessionRepository apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) throws Exception;
}
```

- [ ] **Step 2: Add the session-management dependency to `platform-component-api`** if `org.springframework.ai.session.SessionRepository` does not resolve.

Run:
```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/session-chat-memory
grep -n "session.management\|spring.ai.session" server/libs/platform/platform-component/platform-component-api/build.gradle.kts || echo "NOT PRESENT"
```
If NOT PRESENT, add to that `build.gradle.kts` `dependencies {}`:
```kotlin
    implementation(libs.org.springaicommunity.spring.ai.session.management)
```

- [ ] **Step 3: Compile.**
```bash
./gradlew :server:libs:platform:platform-component:platform-component-api:compileJava -q
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Create the two cluster-root component-definition interfaces** (per spike finding: child nesting must be DECLARED via `getClusterElementTypes()`; precedent `JdbcChatMemoryComponentDefinition` / `VectorStoreChatMemoryComponentDefinition`).

Create `.../platform/component/definition/SessionChatMemoryComponentDefinition.java`:
```java
package com.bytechef.platform.component.definition;

import static com.bytechef.platform.component.definition.ai.agent.ModelFunction.MODEL;
import static com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction.SESSION_REPOSITORY;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import java.util.List;

public interface SessionChatMemoryComponentDefinition extends ClusterRootComponentDefinition {

    @Override
    default List<ClusterElementType> getClusterElementTypes() {
        return List.of(SESSION_REPOSITORY, MODEL);
    }
}
```
Create `.../platform/component/definition/JdbcSessionChatMemoryComponentDefinition.java`:
```java
package com.bytechef.platform.component.definition;

import static com.bytechef.platform.component.definition.ai.agent.DataSourceFunction.DATA_SOURCE;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import java.util.List;

public interface JdbcSessionChatMemoryComponentDefinition extends ClusterRootComponentDefinition {

    @Override
    default List<ClusterElementType> getClusterElementTypes() {
        return List.of(DATA_SOURCE);
    }
}
```
(Apache header on both; copy from a sibling. The builtin/in-memory backends are leaves — no definition interface needed.)

- [ ] **Step 5: Compile + commit.**
```bash
./gradlew :server:libs:platform:platform-component:platform-component-api:compileJava -q
git add server/libs/platform/platform-component/platform-component-api
git commit -m "732 Add SessionRepositoryFunction SPI, SESSION_REPOSITORY type, and session cluster-root definitions"
```

### Task 2: Extend `ChatMemoryFunction.Result` with `toolCallbacks`

**Files:**
- Modify: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/ai/agent/ChatMemoryFunction.java`

- [ ] **Step 1: Replace the `Result` record** (lines 51-60) with a 3-arg record + back-compat 2-arg ctor. Add imports for `ToolCallback`.

Add import:
```java
import org.springframework.ai.tool.ToolCallback;
```
Replace the record:
```java
    @SuppressFBWarnings({
        "EI", "EI2"
    })
    record Result(BaseAdvisor advisor, @Nullable ChatMemory chatMemory, @Nullable ToolCallback[] toolCallbacks) {

        public Result(BaseAdvisor advisor, @Nullable ChatMemory chatMemory) {
            this(advisor, chatMemory, null);
        }
    }
```

- [ ] **Step 2: Compile the whole chat-memory family** to confirm the 2-arg ctor keeps the 7 existing implementations compiling.
```bash
./gradlew :server:libs:platform:platform-component:platform-component-api:compileJava \
  :server:libs:modules:components:ai:agent:chat-memory:chat-memory-builtin:compileJava \
  :server:libs:modules:components:ai:agent:chat-memory:chat-memory-jdbc:compileJava \
  :server:libs:modules:components:ai:agent:chat-memory:chat-memory-in-memory:compileJava -q
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit.**
```bash
git add server/libs/platform/platform-component/platform-component-api
git commit -m "732 Extend ChatMemoryFunction.Result with optional toolCallbacks"
```

---

## Phase 2 — Agent runtime merges memory-contributed tools

### Task 3: Merge `Result.toolCallbacks()` into the agent's `.tools(...)`

**Files:**
- Modify: `server/libs/modules/components/ai/agent/src/main/java/com/bytechef/component/ai/agent/action/AbstractAiAgentChatAction.java` (`getChatClientRequestSpec` ~124-178, `getAdvisors` ~364-440)

**Approach:** Build the memory `Result` ONCE in `getChatClientRequestSpec`, hand it to `getAdvisors` (advisor) and to tool assembly (toolCallbacks). Avoids constructing the stateful `SessionService` twice.

- [ ] **Step 1: Add a helper that resolves the memory Result once.** After `conversationId` is computed (~line 160), add:
```java
        Optional<ChatMemoryFunction.Result> chatMemoryResult =
            clusterElementMap.fetchClusterElement(CHAT_MEMORY)
                .map(clusterElement -> buildChatMemoryResult(connectionParameters, clusterElement, context));
```

- [ ] **Step 2: Change `getAdvisors` to accept the prebuilt Result** instead of building it internally. Update its signature and remove the internal `buildChatMemoryResult` call (lines 393-395), using the passed value:
```java
    List<Advisor> getAdvisors(
        ClusterElementMap clusterElementMap, Map<String, ComponentConnection> connectionParameters,
        ActionContext context, Optional<ChatMemoryFunction.Result> chatMemoryResult) {
```
Delete the old local `chatMemoryResult` assignment at 393-395; keep the rest (`chatMemoryResult.map(...advisor...).ifPresent(advisors::add)` etc.) intact.

- [ ] **Step 3: Update the `getAdvisors(...)` call site** (line 170) to pass `chatMemoryResult`:
```java
            .advisors(getAdvisors(clusterElementMap, connectionParameters, context, chatMemoryResult))
```

- [ ] **Step 4: Merge memory tools into `.tools(...)`.** Replace the `.tools(...)` block (173-177) so memory-contributed callbacks are appended:
```java
            .tools(
                concatToolCallbacks(
                    getToolCallbacks(
                        clusterElementMap.getClusterElements(BaseToolFunction.TOOLS), connectionParameters,
                        context.isEditorEnvironment(), toolExecutionListener, toolSimulations, chatModel, context),
                    chatMemoryResult)
                        .toArray());
```
Add the private helper (place near `getToolCallbacks`):
```java
    private static List<? extends org.springframework.ai.tool.ToolCallback> concatToolCallbacks(
        List<? extends org.springframework.ai.tool.ToolCallback> toolCallbacks,
        Optional<ChatMemoryFunction.Result> chatMemoryResult) {

        org.springframework.ai.tool.ToolCallback[] memoryToolCallbacks = chatMemoryResult
            .map(ChatMemoryFunction.Result::toolCallbacks)
            .orElse(null);

        if (memoryToolCallbacks == null || memoryToolCallbacks.length == 0) {
            return toolCallbacks;
        }

        List<org.springframework.ai.tool.ToolCallback> combined = new ArrayList<>(toolCallbacks);

        combined.addAll(java.util.Arrays.asList(memoryToolCallbacks));

        return combined;
    }
```

- [ ] **Step 4b: Propagate the conversation id into the @Tool ToolContext** (spike finding: recall search reads `chat_memory_conversation_id` from tool context, which the agent does NOT currently set — advisor context and tool context are separate maps in RC1).

In `getChatClientRequestSpec`, after the `.tools(...)` block, add a tool-context entry when `conversationId != null`:
```java
        if (conversationId != null) {
            requestSpec = requestSpec.toolContext(
                java.util.Map.of(org.springframework.ai.session.tool.SessionEventTools.SESSION_ID_CONTEXT_KEY,
                    conversationId));
        }
```
> FIRST verify whether `ChatClientRequestSpec.toolContext(Map)` MERGES or REPLACES. Check the other `.toolContext(...)` call sites (`AiAgentChatAction`, `AiAgentStreamChatAction`, `AbstractAiAgentChatAction:309`). If it MERGES, the snippet above is safe. If it REPLACES, instead fold the key into each existing caller's `.toolContext(...)` map (those call sites pass `ACTION_CONTEXT`/SSE keys). Add a dependency on `spring-ai-session-management` to the agent module's `build.gradle.kts` if `SessionEventTools` does not resolve (or hardcode the literal `"chat_memory_conversation_id"` with an explanatory comment to avoid the dep).

- [ ] **Step 5: Compile the agent module.**
```bash
./gradlew :server:libs:modules:components:ai:agent:compileJava -q
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Run existing agent tests** to confirm no regression in advisor wiring.
```bash
./gradlew :server:libs:modules:components:ai:agent:test -q
```
Expected: PASS (pre-existing tests).

- [ ] **Step 7: Commit.**
```bash
git add server/libs/modules/components/ai/agent/src/main/java/com/bytechef/component/ai/agent/action/AbstractAiAgentChatAction.java
git commit -m "732 Merge chat-memory-contributed tool callbacks onto agent ChatClient"
```

---

## Phase 3 — Backend repository modules (SESSION_REPOSITORY)

> Each module follows the `chat-memory-in-memory` / `chat-memory-jdbc` / `chat-memory-builtin` layout. Copy the asset SVG from `chat-memory-session/src/main/resources/assets/session-chat-memory.svg` into each module's `assets/` with the appropriate filename.

### Task 4: `chat-memory-in-memory-session` module

**Files:**
- Create: `.../chat-memory-in-memory-session/build.gradle.kts`
- Create: `.../in/memory/session/InMemorySessionChatMemoryComponentHandler.java`
- Create: `.../in/memory/session/cluster/InMemorySessionChatMemory.java`
- Create: `.../in/memory/session/constant/InMemorySessionChatMemoryConstants.java`
- Create: `.../in/memory/session/util/InMemorySessionRepositoryHolder.java`
- Create asset: `.../src/main/resources/assets/in-memory-session-chat-memory.svg`
- Test: `.../InMemorySessionChatMemoryComponentHandlerTest.java`

Package root: `com.bytechef.component.ai.agent.chat.memory.in.memory.session`

- [ ] **Step 1: `build.gradle.kts`:**
```kotlin
dependencies {
    implementation(libs.org.springaicommunity.spring.ai.session.management)
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
}
```

- [ ] **Step 2: Constants:**
```java
package com.bytechef.component.ai.agent.chat.memory.in.memory.session.constant;

public class InMemorySessionChatMemoryConstants {

    public static final String IN_MEMORY_SESSION_CHAT_MEMORY = "inMemorySessionChatMemory";

    private InMemorySessionChatMemoryConstants() {
    }
}
```
(Include the Apache license header — copy from any sibling file.)

- [ ] **Step 3: Holder — one `InMemorySessionRepository` per tenant** (mirror `InMemoryChatMemoryRepositoryHolder` using Caffeine + `TenantContext`):
```java
package com.bytechef.component.ai.agent.chat.memory.in.memory.session.util;

import com.bytechef.tenant.util.TenantContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionRepository;

public class InMemorySessionRepositoryHolder {

    private static final Cache<String, SessionRepository> REPOSITORIES = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofHours(1))
        .build();

    private InMemorySessionRepositoryHolder() {
    }

    public static SessionRepository getInstance() {
        return REPOSITORIES.get(
            TenantContext.getCurrentTenantId(),
            tenantId -> InMemorySessionRepository.builder()
                .build());
    }
}
```
> Verify the `TenantContext` import path matches `InMemoryChatMemoryRepositoryHolder`'s; copy it exactly. Add `implementation` deps for caffeine + tenant module by matching `chat-memory-in-memory/build.gradle.kts`.

- [ ] **Step 4: Cluster element** (`SESSION_REPOSITORY` type, no properties, returns the holder repository):
```java
package com.bytechef.component.ai.agent.chat.memory.in.memory.session.cluster;

import static com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction.SESSION_REPOSITORY;

import com.bytechef.component.ai.agent.chat.memory.in.memory.session.util.InMemorySessionRepositoryHolder;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction;

public class InMemorySessionChatMemory {

    public static ClusterElementDefinition<SessionRepositoryFunction> of() {
        return ComponentDsl.<SessionRepositoryFunction>clusterElement("sessionRepository")
            .title("In-memory Session Repository")
            .description("Stores session events in memory; cleared on restart.")
            .type(SESSION_REPOSITORY)
            .object(() -> (inputParameters, connectionParameters, extensions, componentConnections) ->
                InMemorySessionRepositoryHolder.getInstance());
    }

    private InMemorySessionChatMemory() {
    }
}
```

- [ ] **Step 5: Handler** (mirror `InMemoryChatMemoryComponentHandler`):
```java
package com.bytechef.component.ai.agent.chat.memory.in.memory.session;

import static com.bytechef.component.ai.agent.chat.memory.in.memory.session.constant.InMemorySessionChatMemoryConstants.IN_MEMORY_SESSION_CHAT_MEMORY;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.chat.memory.in.memory.session.cluster.InMemorySessionChatMemory;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import org.springframework.stereotype.Component;

@Component(IN_MEMORY_SESSION_CHAT_MEMORY + "_v1_ComponentHandler")
public class InMemorySessionChatMemoryComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition = component(IN_MEMORY_SESSION_CHAT_MEMORY)
        .title("In-memory Session Repository")
        .description("In-memory storage backend for Session Chat Memory.")
        .icon("path:assets/in-memory-session-chat-memory.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .clusterElements(InMemorySessionChatMemory.of());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
```

- [ ] **Step 6: Test** (snapshot):
```java
package com.bytechef.component.ai.agent.chat.memory.in.memory.session;

import com.bytechef.test.jackson.JsonFileAssert;
import org.junit.jupiter.api.Test;

public class InMemorySessionChatMemoryComponentHandlerTest {

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals(
            "definition/inMemorySessionChatMemory_v1.json",
            new InMemorySessionChatMemoryComponentHandler().getDefinition());
    }
}
```
> Match the exact `JsonFileAssert` import + test build deps used by `chat-memory-in-memory`'s test `build.gradle.kts` (`testImplementation` of the component-test-int-support / test support module).

- [ ] **Step 7: Generate the snapshot + run test.**
```bash
rm -f server/libs/modules/components/ai/agent/chat-memory/chat-memory-in-memory-session/src/test/resources/definition/*.json
rm -rf server/libs/modules/components/ai/agent/chat-memory/chat-memory-in-memory-session/build/resources/test/definition
./gradlew :server:libs:modules:components:ai:agent:chat-memory:chat-memory-in-memory-session:test -q
```
Expected: first run generates `inMemorySessionChatMemory_v1.json`; PASS.

- [ ] **Step 8: Register in `settings.gradle.kts`** — add (alphabetical position near other chat-memory entries):
```kotlin
include("server:libs:modules:components:ai:agent:chat-memory:chat-memory-in-memory-session")
```

- [ ] **Step 9: Commit.**
```bash
git add server/libs/modules/components/ai/agent/chat-memory/chat-memory-in-memory-session settings.gradle.kts
git commit -m "732 Add in-memory session repository backend component"
```

### Task 5: `chat-memory-jdbc-session` module (+ shared utils)

**Files:**
- Create: `.../chat-memory-jdbc-session/build.gradle.kts`
- Create: `.../jdbc/session/JdbcSessionChatMemoryComponentHandler.java`
- Create: `.../jdbc/session/cluster/JdbcSessionChatMemory.java`
- Create: `.../jdbc/session/constant/JdbcSessionChatMemoryConstants.java`
- Create: `.../jdbc/session/util/SessionChatMemoryUtils.java` (schema init + `JdbcSessionRepository` from a `DataSource`)
- Create asset + test + snapshot.

Package root: `com.bytechef.component.ai.agent.chat.memory.jdbc.session`

- [ ] **Step 1: `build.gradle.kts`:**
```kotlin
dependencies {
    implementation(libs.org.springaicommunity.spring.ai.session.management)
    implementation(libs.org.springaicommunity.spring.ai.session.jdbc)
    implementation("org.springframework:spring-jdbc")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
}
```

- [ ] **Step 2: `SessionChatMemoryUtils`** — move the schema-init logic from the old `chat-memory-session/.../util/SessionChatMemoryUtils.java` and add a `getSessionRepository(DataSource)` and a `getDataSource(extensions, componentConnections, service)` (the latter mirrors `JdbcChatMemoryUtils.getDataSource`, resolving the `DATA_SOURCE` child):
```java
package com.bytechef.component.ai.agent.chat.memory.jdbc.session.util;

import static com.bytechef.platform.component.definition.ai.agent.DataSourceFunction.DATA_SOURCE;

import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.DataSourceFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import java.sql.DatabaseMetaData;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.jdbc.JdbcSessionRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcUtils;
import tools.jackson.databind.json.JsonMapper;

public class SessionChatMemoryUtils {

    private SessionChatMemoryUtils() {
    }

    public static SessionRepository getSessionRepository(DataSource dataSource) {
        initializeSchema(dataSource);

        return JdbcSessionRepository.builder()
            .dataSource(dataSource)
            .jsonMapper(JsonMapper.builder()
                .build())
            .build();
    }

    public static DataSource getDataSource(
        Parameters extensions, Map<String, ComponentConnection> componentConnections,
        ClusterElementDefinitionService clusterElementDefinitionService) throws Exception {

        ClusterElement clusterElement = ClusterElementMap.of(extensions)
            .getClusterElement(DATA_SOURCE);

        DataSourceFunction dataSourceFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

        return dataSourceFunction.apply(
            ParametersFactory.create(clusterElement.getParameters()),
            ParametersFactory.create(componentConnection.getParameters()),
            ParametersFactory.create(clusterElement.getExtensions()), componentConnections);
    }

    public static void initializeSchema(DataSource dataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
            new ClassPathResource(resolveSchemaScript(dataSource)));

        populator.setContinueOnError(true);

        DatabasePopulatorUtils.execute(populator, dataSource);
    }

    private static String resolveSchemaScript(DataSource dataSource) {
        String productName = null;

        try {
            productName = JdbcUtils.extractDatabaseMetaData(dataSource, DatabaseMetaData::getDatabaseProductName);
        } catch (Exception ignored) {
        }

        String schemaName = switch (productName != null ? productName : "") {
            case "MySQL", "MariaDB" -> "schema-mysql.sql";
            case "H2" -> "schema-h2.sql";
            default -> "schema-postgresql.sql";
        };

        return "org/springframework/ai/session/jdbc/" + schemaName;
    }
}
```

- [ ] **Step 3: Cluster element** (`SESSION_REPOSITORY` + DATA_SOURCE child; needs `ClusterElementDefinitionService`):
```java
package com.bytechef.component.ai.agent.chat.memory.jdbc.session.cluster;

import static com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction.SESSION_REPOSITORY;

import com.bytechef.component.ai.agent.chat.memory.jdbc.session.util.SessionChatMemoryUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.ai.session.SessionRepository;

public class JdbcSessionChatMemory {

    private final ClusterElementDefinitionService clusterElementDefinitionService;

    public static ClusterElementDefinition<SessionRepositoryFunction> of(
        ClusterElementDefinitionService clusterElementDefinitionService) {

        return new JdbcSessionChatMemory(clusterElementDefinitionService).build();
    }

    private JdbcSessionChatMemory(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    private ClusterElementDefinition<SessionRepositoryFunction> build() {
        return ComponentDsl.<SessionRepositoryFunction>clusterElement("sessionRepository")
            .title("JDBC Session Repository")
            .description("Stores session events in a relational database.")
            .type(SESSION_REPOSITORY)
            .object(() -> this::apply);
    }

    protected SessionRepository apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) throws Exception {

        DataSource dataSource = SessionChatMemoryUtils.getDataSource(
            extensions, componentConnections, clusterElementDefinitionService);

        return SessionChatMemoryUtils.getSessionRepository(dataSource);
    }
}
```

- [ ] **Step 4: Constants + handler** (mirror `JdbcChatMemoryComponentHandler`, injecting `ClusterElementDefinitionService`; component name `jdbcSessionChatMemory`). If Task 0 Step 2 found that a DATA_SOURCE child must be DECLARED, add that declaration here.

```java
package com.bytechef.component.ai.agent.chat.memory.jdbc.session.constant;

public class JdbcSessionChatMemoryConstants {

    public static final String JDBC_SESSION_CHAT_MEMORY = "jdbcSessionChatMemory";

    private JdbcSessionChatMemoryConstants() {
    }
}
```
```java
package com.bytechef.component.ai.agent.chat.memory.jdbc.session;

import static com.bytechef.component.ai.agent.chat.memory.jdbc.session.constant.JdbcSessionChatMemoryConstants.JDBC_SESSION_CHAT_MEMORY;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.chat.memory.jdbc.session.cluster.JdbcSessionChatMemory;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

@Component(JDBC_SESSION_CHAT_MEMORY + "_v1_ComponentHandler")
public class JdbcSessionChatMemoryComponentHandler implements ComponentHandler {

    private final JdbcSessionChatMemoryComponentDefinition componentDefinition;

    public JdbcSessionChatMemoryComponentHandler(
        ClusterElementDefinitionService clusterElementDefinitionService) {

        this.componentDefinition = new JdbcSessionChatMemoryComponentDefinitionImpl(
            component(JDBC_SESSION_CHAT_MEMORY)
                .title("JDBC Session Repository")
                .description("JDBC storage backend for Session Chat Memory.")
                .icon("path:assets/jdbc-session-chat-memory.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .clusterElements(JdbcSessionChatMemory.of(clusterElementDefinitionService)));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class JdbcSessionChatMemoryComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements JdbcSessionChatMemoryComponentDefinition {

        public JdbcSessionChatMemoryComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
```
> Imports: `com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper` and `com.bytechef.platform.component.definition.JdbcSessionChatMemoryComponentDefinition` (mirror `JdbcChatMemoryComponentHandler`). This declares the `DATA_SOURCE` child slot so the editor permits attaching one.

- [ ] **Step 5: Test + snapshot** (handler constructed with `null` service, like `JdbcChatMemory`'s test can't build the def with a real service — verify the JDBC chat memory test to copy the exact pattern; if it needs the service for `getDefinition()`, pass a mock). Generate snapshot:
```bash
rm -f server/libs/modules/components/ai/agent/chat-memory/chat-memory-jdbc-session/src/test/resources/definition/*.json
rm -rf server/libs/modules/components/ai/agent/chat-memory/chat-memory-jdbc-session/build/resources/test/definition
./gradlew :server:libs:modules:components:ai:agent:chat-memory:chat-memory-jdbc-session:test -q
```
Expected: PASS.

- [ ] **Step 6: Register module + commit.**
```bash
# add include(...) line to settings.gradle.kts
git add server/libs/modules/components/ai/agent/chat-memory/chat-memory-jdbc-session settings.gradle.kts
git commit -m "732 Add JDBC session repository backend component"
```

### Task 6: `chat-memory-builtin-session` module (internal app DataSource)

**Files:**
- Create: `.../chat-memory-builtin-session/build.gradle.kts`
- Create: `.../builtin/session/BuiltInSessionChatMemoryComponentHandler.java`
- Create: `.../builtin/session/cluster/BuiltInSessionChatMemory.java`
- Create: `.../builtin/session/constant/BuiltInSessionChatMemoryConstants.java`
- Create asset + test + snapshot.

Package root: `com.bytechef.component.ai.agent.chat.memory.builtin.session`

- [ ] **Step 1: `build.gradle.kts`** (reuse jdbc-session utils, like `chat-memory-builtin` reuses `chat-memory-jdbc`):
```kotlin
dependencies {
    implementation(libs.org.springaicommunity.spring.ai.session.management)
    implementation(libs.org.springaicommunity.spring.ai.session.jdbc)
    implementation("org.springframework:spring-jdbc")
    implementation(project(":server:libs:modules:components:ai:agent:chat-memory:chat-memory-jdbc-session"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
}
```

- [ ] **Step 2: Cluster element** — internal `DataSource` from injected `JdbcTemplate`, `InMemorySessionRepository` fallback:
```java
package com.bytechef.component.ai.agent.chat.memory.builtin.session.cluster;

import static com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction.SESSION_REPOSITORY;

import com.bytechef.component.ai.agent.chat.memory.jdbc.session.util.SessionChatMemoryUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionRepository;
import org.springframework.jdbc.core.JdbcTemplate;

public class BuiltInSessionChatMemory {

    public static ClusterElementDefinition<SessionRepositoryFunction> of(@Nullable JdbcTemplate jdbcTemplate) {
        DataSource dataSource = jdbcTemplate == null ? null : jdbcTemplate.getDataSource();

        SessionRepository sessionRepository = dataSource == null
            ? InMemorySessionRepository.builder()
                .build()
            : SessionChatMemoryUtils.getSessionRepository(dataSource);

        return ComponentDsl.<SessionRepositoryFunction>clusterElement("sessionRepository")
            .title("Built-in Session Repository")
            .description("Stores session events in the application database.")
            .type(SESSION_REPOSITORY)
            .object(() -> (inputParameters, connectionParameters, extensions, componentConnections) ->
                sessionRepository);
    }

    private BuiltInSessionChatMemory() {
    }
}
```

- [ ] **Step 3: Constants + handler** (inject `@Autowired(required=false) @Nullable JdbcTemplate`, `@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")` as the current session handler does; component name `builtInSessionChatMemory`).

- [ ] **Step 4: Test + snapshot + register + commit** (same pattern as Task 4 Steps 6-9; handler test constructs with `null` JdbcTemplate).
```bash
git add server/libs/modules/components/ai/agent/chat-memory/chat-memory-builtin-session settings.gradle.kts
git commit -m "732 Add built-in session repository backend component"
```

---

## Phase 4 — `sessionChatMemory` parent (rework `chat-memory-session`)

### Task 7: Constants + compaction enum + full property set

**Files:**
- Modify: `.../chat-memory-session/.../constant/SessionChatMemoryConstants.java`

- [ ] **Step 1: Add property keys + compaction strategy values:**
```java
package com.bytechef.component.ai.agent.chat.memory.session.constant;

public class SessionChatMemoryConstants {

    public static final String SESSION_CHAT_MEMORY = "sessionChatMemory";

    public static final String CONVERSATION_ID = "conversationId";
    public static final String DEFAULT_USER_ID = "defaultUserId";
    public static final String COMPACTION_STRATEGY = "compactionStrategy";
    public static final String MAX_EVENTS = "maxEvents";
    public static final String MAX_TURNS = "maxTurns";
    public static final String MAX_TOKENS = "maxTokens";
    public static final String MAX_EVENTS_TO_KEEP = "maxEventsToKeep";
    public static final String OVERLAP_SIZE = "overlapSize";
    public static final String ENABLE_CONVERSATION_SEARCH = "enableConversationSearch";
    public static final String SEARCH_PAGE_SIZE = "searchPageSize";
    public static final String AGENT_BRANCH = "agentBranch";

    // compactionStrategy option values
    public static final String NONE = "NONE";
    public static final String SLIDING_WINDOW = "SLIDING_WINDOW";
    public static final String TURN_WINDOW = "TURN_WINDOW";
    public static final String TOKEN_COUNT = "TOKEN_COUNT";
    public static final String RECURSIVE_SUMMARIZATION = "RECURSIVE_SUMMARIZATION";

    public static final String DEFAULT_USER_ID_VALUE = "bytechef";

    private SessionChatMemoryConstants() {
    }
}
```

- [ ] **Step 2: Compile + commit.**
```bash
./gradlew :server:libs:modules:components:ai:agent:chat-memory:chat-memory-session:compileJava -q
git add server/libs/modules/components/ai/agent/chat-memory/chat-memory-session/src/main/java/com/bytechef/component/ai/agent/chat/memory/session/constant/SessionChatMemoryConstants.java
git commit -m "732 Add session chat memory property + compaction strategy constants"
```

### Task 8: Rework `SessionChatMemory` cluster element — properties + child resolution + advisor/tools assembly

**Files:**
- Modify: `.../chat-memory-session/.../cluster/SessionChatMemory.java`
- Modify: `.../chat-memory-session/.../SessionChatMemoryComponentHandler.java`
- Delete: `.../chat-memory-session/.../util/SessionChatMemoryUtils.java`
- Modify: `.../chat-memory-session/build.gradle.kts`

- [ ] **Step 1: Update `build.gradle.kts`** — drop session-jdbc (moved out), add platform-component-service (for `ClusterElementDefinitionService`) and the session-management dep:
```kotlin
dependencies {
    implementation(libs.org.springaicommunity.spring.ai.session.management)
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-service"))
}
```

- [ ] **Step 2: Delete the old util** (schema-init moved to jdbc-session):
```bash
git rm server/libs/modules/components/ai/agent/chat-memory/chat-memory-session/src/main/java/com/bytechef/component/ai/agent/chat/memory/session/util/SessionChatMemoryUtils.java
```

- [ ] **Step 3: Rewrite the cluster element.** Full property set, resolve `SESSION_REPOSITORY` child → `SessionService`, build advisor with compaction (auto-paired trigger derived from strategy size), branch filter, recall tools; optional `MODEL` child for summarization.

```java
package com.bytechef.component.ai.agent.chat.memory.session.cluster;

import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.AGENT_BRANCH;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.COMPACTION_STRATEGY;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.CONVERSATION_ID;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.DEFAULT_USER_ID;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.DEFAULT_USER_ID_VALUE;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.ENABLE_CONVERSATION_SEARCH;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.MAX_EVENTS;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.MAX_EVENTS_TO_KEEP;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.MAX_TOKENS;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.MAX_TURNS;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.NONE;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.OVERLAP_SIZE;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.RECURSIVE_SUMMARIZATION;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.SEARCH_PAGE_SIZE;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.SLIDING_WINDOW;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.TOKEN_COUNT;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.TURN_WINDOW;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction.CHAT_MEMORY;
import static com.bytechef.platform.component.definition.ai.agent.ModelFunction.MODEL;
import static com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction.SESSION_REPOSITORY;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.compaction.CompactionStrategy;
import org.springframework.ai.session.compaction.CompactionTrigger;
import org.springframework.ai.session.compaction.RecursiveSummarizationCompactionStrategy;
import org.springframework.ai.session.compaction.SlidingWindowCompactionStrategy;
import org.springframework.ai.session.compaction.TokenCountCompactionStrategy;
import org.springframework.ai.session.compaction.TokenCountTrigger;
import org.springframework.ai.session.compaction.TurnCountTrigger;
import org.springframework.ai.session.compaction.TurnWindowCompactionStrategy;
import org.springframework.ai.session.tool.SessionEventTools;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

public class SessionChatMemory {

    private final ClusterElementDefinitionService clusterElementDefinitionService;

    public static ClusterElementDefinition<ChatMemoryFunction> of(
        ClusterElementDefinitionService clusterElementDefinitionService) {

        return new SessionChatMemory(clusterElementDefinitionService).build();
    }

    private SessionChatMemory(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    private ClusterElementDefinition<ChatMemoryFunction> build() {
        return ComponentDsl.<ChatMemoryFunction>clusterElement("chatMemory")
            .title("Session Chat Memory")
            .description("Event-sourced session memory; prior messages are recalled per conversation session.")
            .properties(
                string(CONVERSATION_ID)
                    .label("Conversation ID")
                    .description("The unique identifier for the conversation session.")
                    .required(true),
                string(DEFAULT_USER_ID)
                    .label("Default User ID")
                    .description("User id assigned to new sessions when none is supplied per request.")
                    .defaultValue(DEFAULT_USER_ID_VALUE)
                    .required(false),
                string(COMPACTION_STRATEGY)
                    .label("Compaction Strategy")
                    .description("How to shrink history when it grows. The strategy's size also drives compaction.")
                    .options(
                        option("None", NONE),
                        option("Sliding window (by events)", SLIDING_WINDOW),
                        option("Turn window (by turns)", TURN_WINDOW),
                        option("Token count", TOKEN_COUNT),
                        option("Recursive summarization (LLM)", RECURSIVE_SUMMARIZATION))
                    .defaultValue(NONE)
                    .required(false),
                integer(MAX_EVENTS)
                    .label("Max events")
                    .defaultValue(20)
                    .displayCondition("%s == '%s'".formatted(COMPACTION_STRATEGY, SLIDING_WINDOW))
                    .required(true),
                integer(MAX_TURNS)
                    .label("Max turns")
                    .defaultValue(10)
                    .displayCondition("%s == '%s'".formatted(COMPACTION_STRATEGY, TURN_WINDOW))
                    .required(true),
                integer(MAX_TOKENS)
                    .label("Max tokens")
                    .defaultValue(4000)
                    .displayCondition("%s == '%s'".formatted(COMPACTION_STRATEGY, TOKEN_COUNT))
                    .required(true),
                integer(MAX_EVENTS_TO_KEEP)
                    .label("Max events to keep")
                    .defaultValue(10)
                    .displayCondition("%s == '%s'".formatted(COMPACTION_STRATEGY, RECURSIVE_SUMMARIZATION))
                    .required(true),
                integer(OVERLAP_SIZE)
                    .label("Overlap size")
                    .defaultValue(2)
                    .displayCondition("%s == '%s'".formatted(COMPACTION_STRATEGY, RECURSIVE_SUMMARIZATION))
                    .required(true),
                bool(ENABLE_CONVERSATION_SEARCH)
                    .label("Enable conversation search tool")
                    .description("Exposes a keyword search tool over the full archived event log (recall storage).")
                    .defaultValue(false)
                    .required(false),
                integer(SEARCH_PAGE_SIZE)
                    .label("Search page size")
                    .defaultValue(10)
                    .displayCondition("%s == true".formatted(ENABLE_CONVERSATION_SEARCH))
                    .required(false),
                string(AGENT_BRANCH)
                    .label("Agent branch")
                    .description("Restrict recalled events to this dot-path branch (multi-agent isolation).")
                    .required(false))
            .type(CHAT_MEMORY)
            .object(() -> this::apply);
    }

    protected ChatMemoryFunction.Result apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) throws Exception {

        SessionService sessionService = DefaultSessionService.builder()
            .sessionRepository(resolveSessionRepository(extensions, componentConnections))
            .build();

        SessionMemoryAdvisor.Builder builder = SessionMemoryAdvisor.builder(sessionService)
            .defaultUserId(inputParameters.getString(DEFAULT_USER_ID, DEFAULT_USER_ID_VALUE));

        String agentBranch = inputParameters.getString(AGENT_BRANCH);

        if (agentBranch != null && !agentBranch.isBlank()) {
            builder.eventFilter(EventFilter.forBranch(agentBranch));
        }

        CompactionStrategy strategy = resolveCompactionStrategy(inputParameters, extensions, componentConnections);

        if (strategy != null) {
            builder.compactionStrategy(strategy)
                .compactionTrigger(resolveCompactionTrigger(inputParameters));
        }

        BaseAdvisor advisor = builder.build();

        ToolCallback[] toolCallbacks = resolveRecallToolCallbacks(inputParameters, sessionService);

        return new ChatMemoryFunction.Result(advisor, null, toolCallbacks);
    }

    private SessionRepository resolveSessionRepository(
        Parameters extensions, Map<String, ComponentConnection> componentConnections) throws Exception {

        ClusterElement clusterElement = ClusterElementMap.of(extensions)
            .getClusterElement(SESSION_REPOSITORY);

        SessionRepositoryFunction function = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

        return function.apply(
            ParametersFactory.create(clusterElement.getParameters()),
            componentConnection == null
                ? ParametersFactory.create(Map.of())
                : ParametersFactory.create(componentConnection.getParameters()),
            ParametersFactory.create(clusterElement.getExtensions()), componentConnections);
    }

    private CompactionStrategy resolveCompactionStrategy(
        Parameters inputParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) throws Exception {

        String selection = inputParameters.getString(COMPACTION_STRATEGY, NONE);

        return switch (selection) {
            case SLIDING_WINDOW -> SlidingWindowCompactionStrategy.builder()
                .maxEvents(inputParameters.getInteger(MAX_EVENTS, 20))
                .build();
            case TURN_WINDOW -> TurnWindowCompactionStrategy.builder()
                .maxTurns(inputParameters.getInteger(MAX_TURNS, 10))
                .build();
            case TOKEN_COUNT -> TokenCountCompactionStrategy.builder()
                .maxTokens(inputParameters.getInteger(MAX_TOKENS, 4000))
                .build();
            case RECURSIVE_SUMMARIZATION -> RecursiveSummarizationCompactionStrategy
                .builder(resolveSummarizerChatClient(extensions, componentConnections))
                .maxEventsToKeep(inputParameters.getInteger(MAX_EVENTS_TO_KEEP, 10))
                .overlapSize(inputParameters.getInteger(OVERLAP_SIZE, 2))
                .build();
            default -> null;
        };
    }

    private CompactionTrigger resolveCompactionTrigger(Parameters inputParameters) {
        String selection = inputParameters.getString(COMPACTION_STRATEGY, NONE);

        return switch (selection) {
            case SLIDING_WINDOW -> new TurnCountTrigger(inputParameters.getInteger(MAX_EVENTS, 20));
            case TURN_WINDOW -> new TurnCountTrigger(inputParameters.getInteger(MAX_TURNS, 10));
            case TOKEN_COUNT -> TokenCountTrigger.builder()
                .threshold(inputParameters.getInteger(MAX_TOKENS, 4000))
                .build();
            case RECURSIVE_SUMMARIZATION -> new TurnCountTrigger(inputParameters.getInteger(MAX_EVENTS_TO_KEEP, 10));
            default -> throw new IllegalStateException("No trigger for strategy: " + selection);
        };
    }

    private ChatClient resolveSummarizerChatClient(
        Parameters extensions, Map<String, ComponentConnection> componentConnections) throws Exception {

        ClusterElement clusterElement = ClusterElementMap.of(extensions)
            .getClusterElement(MODEL);

        if (clusterElement == null) {
            throw new IllegalStateException(
                "Recursive summarization requires a Model child to be configured on Session Chat Memory.");
        }

        ModelFunction modelFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

        ChatModel chatModel = (ChatModel) modelFunction.apply(
            ParametersFactory.create(clusterElement.getParameters()),
            componentConnection == null
                ? ParametersFactory.create(Map.of())
                : ParametersFactory.create(componentConnection.getParameters()),
            false);

        return ChatClient.builder(chatModel)
            .build();
    }

    private ToolCallback[] resolveRecallToolCallbacks(Parameters inputParameters, SessionService sessionService) {
        if (!Boolean.TRUE.equals(inputParameters.getBoolean(ENABLE_CONVERSATION_SEARCH, false))) {
            return null;
        }

        SessionEventTools sessionEventTools = SessionEventTools.builder(sessionService)
            .pageSize(inputParameters.getInteger(SEARCH_PAGE_SIZE, 10))
            .build();

        return ToolCallbacks.from(sessionEventTools);
    }
}
```

> Verify `Parameters` has `getInteger(key, default)` / `getBoolean(key, default)` / `getString(key, default)` — if the exact overloads differ, adjust to the available signatures (check `chat-memory-jdbc` / other components for usage). `ClusterElementMap.getClusterElement(MODEL)` returns the element or throws — confirm whether it returns null or throws when absent; if it throws, wrap the MODEL lookup in `fetchClusterElement(MODEL)` and handle empty.

- [ ] **Step 4: Update the handler** to inject `ClusterElementDefinitionService` and drop the `JdbcTemplate`/`createSessionService` logic:
```java
package com.bytechef.component.ai.agent.chat.memory.session;

import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.SESSION_CHAT_MEMORY;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.chat.memory.session.cluster.SessionChatMemory;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

@Component(SESSION_CHAT_MEMORY + "_v1_ComponentHandler")
public class SessionChatMemoryComponentHandler implements ComponentHandler {

    private final SessionChatMemoryComponentDefinition componentDefinition;

    public SessionChatMemoryComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new SessionChatMemoryComponentDefinitionImpl(
            component(SESSION_CHAT_MEMORY)
                .title("Session Chat Memory")
                .description("Event-sourced session-based chat memory.")
                .icon("path:assets/session-chat-memory.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .clusterElements(SessionChatMemory.of(clusterElementDefinitionService)));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class SessionChatMemoryComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements SessionChatMemoryComponentDefinition {

        public SessionChatMemoryComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
```
> Imports: `com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper` and `com.bytechef.platform.component.definition.SessionChatMemoryComponentDefinition`. This declares the `SESSION_REPOSITORY` + `MODEL` child slots so the editor permits attaching them.

- [ ] **Step 5: Compile.**
```bash
./gradlew :server:libs:modules:components:ai:agent:chat-memory:chat-memory-session:compileJava -q
```
Expected: BUILD SUCCESSFUL (fix any `Parameters` getter signature mismatches surfaced here).

- [ ] **Step 6: Commit.**
```bash
git add server/libs/modules/components/ai/agent/chat-memory/chat-memory-session
git commit -m "732 Rework sessionChatMemory: full config knobs + child-resolved backend and summarizer"
```

### Task 9: Update the parent snapshot test

**Files:**
- Modify: `.../chat-memory-session/.../SessionChatMemoryComponentHandlerTest.java`
- Regenerate: `.../src/test/resources/definition/sessionChatMemory_v1.json`

- [ ] **Step 1: Update the test** to construct the handler with a mock/null `ClusterElementDefinitionService`:
```java
    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals(
            "definition/sessionChatMemory_v1.json",
            new SessionChatMemoryComponentHandler(null).getDefinition());
    }
```
> If `SessionChatMemory.of(null)` triggers an NPE at definition-build time, pass `org.mockito.Mockito.mock(ClusterElementDefinitionService.class)` instead. (Definition build only needs the service captured, not invoked, so `null` should be fine.)

- [ ] **Step 2: Regenerate snapshot + run.**
```bash
rm -f server/libs/modules/components/ai/agent/chat-memory/chat-memory-session/src/test/resources/definition/sessionChatMemory_v1.json
rm -rf server/libs/modules/components/ai/agent/chat-memory/chat-memory-session/build/resources/test/definition
./gradlew :server:libs:modules:components:ai:agent:chat-memory:chat-memory-session:test -q
```
Expected: regenerates the JSON with all new properties; PASS.

- [ ] **Step 3: Commit.**
```bash
git add server/libs/modules/components/ai/agent/chat-memory/chat-memory-session/src/test
git commit -m "732 Regenerate sessionChatMemory definition snapshot"
```

---

## Phase 5 — Build, format, docs, integration verification

### Task 10: Full build + spotless + checks

- [ ] **Step 1: Spotless + compile the whole server.**
```bash
./gradlew spotlessApply
./gradlew clean compileJava -q
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Run the chat-memory family + agent tests.**
```bash
./gradlew :server:libs:modules:components:ai:agent:test \
  :server:libs:modules:components:ai:agent:chat-memory:chat-memory-session:test \
  :server:libs:modules:components:ai:agent:chat-memory:chat-memory-builtin-session:test \
  :server:libs:modules:components:ai:agent:chat-memory:chat-memory-jdbc-session:test \
  :server:libs:modules:components:ai:agent:chat-memory:chat-memory-in-memory-session:test -q
```
Expected: PASS.

- [ ] **Step 3: Commit any spotless reformatting.**
```bash
git add -A
git commit -m "732 spotlessApply for session chat memory family"
```

### Task 11: README docs (each new component) + generateDocumentation

- [ ] **Step 1:** Add `src/main/resources/README.md` to each new module (match the format of `chat-memory-jdbc/src/main/resources/README.md`).
- [ ] **Step 2:**
```bash
./gradlew generateDocumentation -q
```
- [ ] **Step 3: Commit.**
```bash
git add -A
git commit -m "732 Add session chat memory component documentation"
```

### Task 12: End-to-end verification in a running agent

- [ ] **Step 1:** Start infra + server per CLAUDE.md, open the workflow editor, build an AI Agent with `sessionChatMemory` → attach a `builtInSessionChatMemory` SESSION_REPOSITORY child. Run a 2-turn conversation; confirm the second turn recalls the first (advisor injection works end to end).
- [ ] **Step 2:** Set `compactionStrategy = RECURSIVE_SUMMARIZATION`, attach a `MODEL` child; run enough turns to exceed `maxEventsToKeep`; confirm a summary event is produced and no error is thrown.
- [ ] **Step 3:** Toggle `enableConversationSearch = true`; confirm the agent can call `conversation_search` and that it returns the right session's events (validates the Task 0 Step 3 tool-context finding).
- [ ] **Step 4:** Record results in the PR description. If Step 3 fails on session id, apply the tool-context fix identified in Task 0 Step 3.

---

## Self-Review notes

- **Spec coverage:** SessionRepositoryFunction (T1), Result.toolCallbacks (T2), runtime tool merge (T3), in-memory/jdbc/builtin backends (T4-6), all knobs incl. summarization via MODEL child (T7-8), recall storage (T8), branch isolation (T8), snapshot tests (T4-6,9), docs (T11) — all covered.
- **Open risks gated by Task 0:** editor child-nesting for non-DATA_SOURCE/MODEL children; conversation-id propagation to `@Tool` context. Both are verified before module work and have fallback tasks.
- **Type consistency:** component names — `sessionChatMemory` (parent), `builtInSessionChatMemory`, `jdbcSessionChatMemory`, `inMemorySessionChatMemory` (backends); cluster element key `sessionRepository` for all backends, `chatMemory` for the parent; `SessionRepositoryFunction.apply` 4-arg signature matches `DataSourceFunction`.
- **To confirm during build (noted inline):** exact `Parameters` getter overloads; whether `ClusterElementMap.getClusterElement(MODEL)` returns null vs throws; `JsonFileAssert`/test-support dependency coordinates per module `build.gradle.kts`.
