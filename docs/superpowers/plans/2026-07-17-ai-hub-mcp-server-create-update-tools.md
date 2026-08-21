# AI Hub MCP-Server Create/Update Tools Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the AI Hub agent create and update (rename / enable-disable) platform MCP servers from chat, closing the one capability gap where MCP servers were UI/GraphQL-only while deployments were already fully AI-drivable.

**Architecture:** Two new Spring AI `ToolCallback` classes in the EE AI Hub tool package mirror the existing `CreateProjectDeploymentToolCallback` / `UpdateProjectDeploymentToolCallback` pattern. Both depend only on the workspace-scoped `WorkspaceMcpServerFacade` (never the raw `McpServerService`), so tenant isolation stays enforced by the facade's `@PreAuthorize` permission checks. Create reuses the existing `createWorkspaceMcpServer`; update needs one new facade method delegating to `McpServerService.update`. Both tools register on the same `workspaceMcpServerFacadeProvider.ifAvailable` branch in `AiHubConfiguration` that already holds `ListMcpServersToolCallback`.

**Tech Stack:** Java 25, Spring Boot 4, Spring AI `ToolCallback`, Jackson 3 (`tools.jackson`), JUnit 5 + Mockito + AssertJ.

## Global Constraints

- EE files (`server/ee/**`): use the **ByteChef Enterprise license** header (not Apache 2.0) and add a `@version ee` Javadoc tag to every class.
- Non-EE files (`server/libs/**`): use the **Apache 2.0** license header.
- MCP server type on this surface is always `PlatformType.AUTOMATION` — hardcoded, never an LLM input.
- Newly created servers default to `enabled = false` (safety: an addressable endpoint must not go live from a chat turn).
- Tenant isolation is enforced by `@PreAuthorize("hasPermission(...)")` on the facade — tools must depend on `WorkspaceMcpServerFacade`, never `McpServerService` directly.
- One blank line before control statements; one blank line after a variable modification that precedes its use (repo Java style, enforced by Spotless — run `./gradlew spotlessApply` before every commit).
- Tool-callback unit tests are plain Mockito (no Spring context), mirroring `ListProjectDeploymentsToolCallbackTest`.

---

### Task 1: Add `updateWorkspaceMcpServer` to the workspace facade

**Files:**
- Modify: `server/libs/automation/automation-ai/automation-ai-mcp/automation-ai-mcp-api/src/main/java/com/bytechef/automation/ai/mcp/facade/WorkspaceMcpServerFacade.java`
- Modify: `server/libs/automation/automation-ai/automation-ai-mcp/automation-ai-mcp-service/src/main/java/com/bytechef/automation/ai/mcp/facade/WorkspaceMcpServerFacadeImpl.java:122-128`
- Test: `server/libs/automation/automation-ai/automation-ai-mcp/automation-ai-mcp-service/src/test/java/com/bytechef/automation/ai/mcp/facade/WorkspaceMcpServerFacadeImplTest.java`

**Interfaces:**
- Consumes: `McpServerService.update(long id, String name, Boolean enabled)` (already exists, returns `McpServer`).
- Produces: `McpServer WorkspaceMcpServerFacade.updateWorkspaceMcpServer(Long mcpServerId, String name, Boolean enabled)` — `name`/`enabled` are null-to-keep-unchanged; consumed by Task 3.

- [ ] **Step 1: Write the failing test**

Create `WorkspaceMcpServerFacadeImplTest.java` (Apache header):

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

package com.bytechef.automation.ai.mcp.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.tag.service.TagService;
import org.junit.jupiter.api.Test;

class WorkspaceMcpServerFacadeImplTest {

    @Test
    void testUpdateWorkspaceMcpServerDelegatesToService() {
        McpServerService mcpServerService = mock(McpServerService.class);
        McpServer updated = mock(McpServer.class);

        when(mcpServerService.update(3L, "New name", true)).thenReturn(updated);

        WorkspaceMcpServerFacadeImpl facade = new WorkspaceMcpServerFacadeImpl(
            mock(McpProjectService.class), mock(McpServerFacade.class), mcpServerService,
            mock(TagService.class), mock(WorkspaceMcpServerService.class));

        McpServer result = facade.updateWorkspaceMcpServer(3L, "New name", true);

        assertThat(result).isSameAs(updated);

        verify(mcpServerService).update(3L, "New name", true);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-mcp:automation-ai-mcp-service:test --tests "com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacadeImplTest"`
Expected: FAIL — compile error, `updateWorkspaceMcpServer` is undefined.

- [ ] **Step 3: Add the interface method**

In `WorkspaceMcpServerFacade.java`, after the `createWorkspaceMcpServer` Javadoc/method block (before `deleteWorkspaceMcpServer`), add:

```java
    /**
     * Updates the name and/or enabled state of an MCP server belonging to a workspace.
     *
     * @param mcpServerId the ID of the MCP server to update
     * @param name        the new name (null to leave unchanged)
     * @param enabled     the new enabled state (null to leave unchanged)
     * @return the updated MCP server
     */
    McpServer updateWorkspaceMcpServer(Long mcpServerId, String name, Boolean enabled);
```

- [ ] **Step 4: Add the impl method**

In `WorkspaceMcpServerFacadeImpl.java`, after `createWorkspaceMcpServer` (around line 120, before `deleteWorkspaceMcpServer`), add:

```java
    @Override
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_EDIT')")
    public McpServer updateWorkspaceMcpServer(Long mcpServerId, String name, Boolean enabled) {
        return mcpServerService.update(mcpServerId, name, enabled);
    }
```

(`PreAuthorize` and `mcpServerService` are already imported / injected in this class — no new imports needed.)

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-mcp:automation-ai-mcp-service:test --tests "com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacadeImplTest"`
Expected: PASS.

- [ ] **Step 6: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-ai/automation-ai-mcp/automation-ai-mcp-api/src/main/java/com/bytechef/automation/ai/mcp/facade/WorkspaceMcpServerFacade.java \
        server/libs/automation/automation-ai/automation-ai-mcp/automation-ai-mcp-service/src/main/java/com/bytechef/automation/ai/mcp/facade/WorkspaceMcpServerFacadeImpl.java \
        server/libs/automation/automation-ai/automation-ai-mcp/automation-ai-mcp-service/src/test/java/com/bytechef/automation/ai/mcp/facade/WorkspaceMcpServerFacadeImplTest.java
git commit -m "Add updateWorkspaceMcpServer to WorkspaceMcpServerFacade"
```

---

### Task 2: `CreateMcpServerToolCallback`

**Files:**
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/CreateMcpServerToolCallback.java`
- Test: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/CreateMcpServerToolCallbackTest.java`

**Interfaces:**
- Consumes: `WorkspaceMcpServerFacade.createWorkspaceMcpServer(String name, PlatformType type, Environment environment, Boolean enabled, Long workspaceId)` (returns `McpServer`); `AiHubToolInvocationContext.fromToolContext(ToolContext)` → context with `workspaceId()`.
- Produces: `CreateMcpServerToolCallback(WorkspaceMcpServerFacade)` with `static final String TOOL_NAME = "createMcpServer"` — registered in Task 4.

- [ ] **Step 1: Write the failing test**

Create `CreateMcpServerToolCallbackTest.java` (EE header):

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CreateMcpServerToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testCreatesServerScopedToWorkspaceDisabledByDefault() throws Exception {
        WorkspaceMcpServerFacade facade = mock(WorkspaceMcpServerFacade.class);
        McpServer created = mock(McpServer.class);

        when(created.getId()).thenReturn(42L);
        when(created.getName()).thenReturn("Support tools");
        when(created.getType()).thenReturn(PlatformType.AUTOMATION);
        when(created.getEnvironment()).thenReturn(Environment.STAGING);
        when(created.isEnabled()).thenReturn(false);
        when(facade.createWorkspaceMcpServer(
            eq("Support tools"), eq(PlatformType.AUTOMATION), eq(Environment.STAGING), eq(false), eq(99L)))
                .thenReturn(created);

        CreateMcpServerToolCallback callback = new CreateMcpServerToolCallback(facade);

        ToolContext toolContext = new ToolContext(
            Map.of(AiHubToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 99L));

        String result = callback.call("{\"name\":\"Support tools\",\"environment\":\"STAGING\"}", toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("mcpServerId")
            .asLong()).isEqualTo(42L);
        assertThat(node.get("type")
            .asText()).isEqualTo("AUTOMATION");
        assertThat(node.get("environment")
            .asText()).isEqualTo("STAGING");
        assertThat(node.get("enabled")
            .asBoolean()).isFalse();

        verify(facade).createWorkspaceMcpServer(
            eq("Support tools"), eq(PlatformType.AUTOMATION), eq(Environment.STAGING), eq(false), eq(99L));
    }

    @Test
    void testRejectsMissingWorkspaceContext() throws Exception {
        CreateMcpServerToolCallback callback = new CreateMcpServerToolCallback(mock(WorkspaceMcpServerFacade.class));

        String result = callback.call("{\"name\":\"X\",\"environment\":\"STAGING\"}", null);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("Workspace context");
    }

    @Test
    void testRejectsUnknownEnvironment() throws Exception {
        CreateMcpServerToolCallback callback = new CreateMcpServerToolCallback(mock(WorkspaceMcpServerFacade.class));

        ToolContext toolContext = new ToolContext(
            Map.of(AiHubToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 99L));

        String result = callback.call("{\"name\":\"X\",\"environment\":\"NOPE\"}", toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("Unknown environment");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests "com.bytechef.ee.ai.hub.tool.CreateMcpServerToolCallbackTest"`
Expected: FAIL — `CreateMcpServerToolCallback` is undefined.

- [ ] **Step 3: Write the implementation**

Create `CreateMcpServerToolCallback.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that creates a platform {@link McpServer} in the caller's workspace. The server type is
 * always {@link PlatformType#AUTOMATION} on this surface (an MCP server exposing workflows-as-tools), so the LLM never
 * supplies it. The server is created disabled by default — {@code updateMcpServer} brings it online after the user has
 * attached workflows via {@code createMcpProject}.
 *
 * <p>
 * Delegates to {@link WorkspaceMcpServerFacade#createWorkspaceMcpServer}, whose {@code @PreAuthorize} check enforces
 * that the resolved workspace is one the caller may create servers in — the tenant boundary lives in the facade, not
 * here.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
public class CreateMcpServerToolCallback implements ToolCallback {

    static final String TOOL_NAME = "createMcpServer";

    private static final String SUPPORTED_ENVIRONMENTS = Arrays.stream(Environment.values())
        .map(Environment::name)
        .collect(Collectors.joining(", "));

    private static final String DESCRIPTION = """
        Create a new MCP server in the current workspace. An MCP server exposes selected workflows as MCP
        tools to external MCP clients. Supply name and environment (one of: %s). Optionally supply enabled
        (defaults to false). The server is created disabled — call updateMcpServer with enabled=true once
        workflows are attached via createMcpProject. Returns {mcpServerId, name, type, environment, enabled}
        on success or {error: <message>} on failure.""".formatted(SUPPORTED_ENVIRONMENTS);

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "name": {"type": "string", "description": "Human-readable MCP server name"},
                    "environment": {"type": "string", "description": "Target environment: DEVELOPMENT, STAGING, or PRODUCTION"},
                    "enabled": {"type": "boolean", "description": "Whether the server is enabled (optional, default false)"}
                },
                "required": ["name", "environment"]
            }""";

    private final WorkspaceMcpServerFacade workspaceMcpServerFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateMcpServerToolCallback(WorkspaceMcpServerFacade workspaceMcpServerFacade) {
        this.workspaceMcpServerFacade = workspaceMcpServerFacade;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(TOOL_NAME)
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            AiHubToolInvocationContext context = AiHubToolInvocationContext.fromToolContext(toolContext);

            if (context == null || context.workspaceId() == null) {
                return toolError("Workspace context unavailable — open this chat from the AI Hub of a workspace.");
            }

            CreateMcpServerInput input = jsonMapper.readValue(toolInput, CreateMcpServerInput.class);

            if (input.name() == null || input.name()
                .isBlank()) {
                return toolError("name is required");
            }

            if (input.environment() == null || input.environment()
                .isBlank()) {
                return toolError("environment is required (one of: " + SUPPORTED_ENVIRONMENTS + ")");
            }

            Environment environment;

            try {
                environment = Environment.valueOf(input.environment()
                    .toUpperCase());
            } catch (IllegalArgumentException exception) {
                return toolError(
                    "Unknown environment '" + input.environment() + "'. Supported: " + SUPPORTED_ENVIRONMENTS);
            }

            boolean enabled = input.enabled() != null && input.enabled();

            McpServer mcpServer = workspaceMcpServerFacade.createWorkspaceMcpServer(
                input.name(), PlatformType.AUTOMATION, environment, enabled, context.workspaceId());

            return jsonMapper.writeValueAsString(
                new CreateMcpServerOutput(
                    mcpServer.getId(), mcpServer.getName(),
                    mcpServer.getType()
                        .name(),
                    mcpServer.getEnvironment()
                        .name(),
                    mcpServer.isEnabled()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, CreateMcpServerToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record CreateMcpServerInput(String name, String environment, @Nullable Boolean enabled) {
    }

    public record CreateMcpServerOutput(
        long mcpServerId, String name, String type, String environment, boolean enabled) {
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests "com.bytechef.ee.ai.hub.tool.CreateMcpServerToolCallbackTest"`
Expected: PASS (all three tests).

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/CreateMcpServerToolCallback.java \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/CreateMcpServerToolCallbackTest.java
git commit -m "Add CreateMcpServerToolCallback for AI Hub agent"
```

---

### Task 3: `UpdateMcpServerToolCallback`

**Files:**
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/UpdateMcpServerToolCallback.java`
- Test: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/UpdateMcpServerToolCallbackTest.java`

**Interfaces:**
- Consumes: `WorkspaceMcpServerFacade.updateWorkspaceMcpServer(Long, String, Boolean)` from Task 1 (returns `McpServer`).
- Produces: `UpdateMcpServerToolCallback(WorkspaceMcpServerFacade)` with `static final String TOOL_NAME = "updateMcpServer"` — registered in Task 4.

- [ ] **Step 1: Write the failing test**

Create `UpdateMcpServerToolCallbackTest.java` (EE header):

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.platform.mcp.domain.McpServer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class UpdateMcpServerToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testEnablesServerLeavingNameUnchanged() throws Exception {
        WorkspaceMcpServerFacade facade = mock(WorkspaceMcpServerFacade.class);
        McpServer updated = mock(McpServer.class);

        when(updated.getId()).thenReturn(7L);
        when(updated.getName()).thenReturn("Existing name");
        when(updated.isEnabled()).thenReturn(true);
        when(facade.updateWorkspaceMcpServer(eq(7L), isNull(), eq(true))).thenReturn(updated);

        UpdateMcpServerToolCallback callback = new UpdateMcpServerToolCallback(facade);

        String result = callback.call("{\"mcpServerId\":\"7\",\"enabled\":true}", null);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("mcpServerId")
            .asLong()).isEqualTo(7L);
        assertThat(node.get("enabled")
            .asBoolean()).isTrue();

        verify(facade).updateWorkspaceMcpServer(eq(7L), isNull(), eq(true));
    }

    @Test
    void testRejectsWhenNoFieldSupplied() throws Exception {
        UpdateMcpServerToolCallback callback = new UpdateMcpServerToolCallback(mock(WorkspaceMcpServerFacade.class));

        String result = callback.call("{\"mcpServerId\":\"7\"}", null);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("at least one of name or enabled");
    }

    @Test
    void testRejectsNonNumericId() throws Exception {
        UpdateMcpServerToolCallback callback = new UpdateMcpServerToolCallback(mock(WorkspaceMcpServerFacade.class));

        String result = callback.call("{\"mcpServerId\":\"abc\",\"enabled\":true}", null);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("numeric id");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests "com.bytechef.ee.ai.hub.tool.UpdateMcpServerToolCallbackTest"`
Expected: FAIL — `UpdateMcpServerToolCallback` is undefined.

- [ ] **Step 3: Write the implementation**

Create `UpdateMcpServerToolCallback.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.platform.mcp.domain.McpServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that updates an existing platform {@link McpServer}'s name and/or enabled state. This
 * single tool doubles as the enable/disable toggle — the underlying {@code McpServerService.update(id, name, enabled)}
 * covers both intents, so no separate toggle tool is needed (unlike deployments, which split rename and toggle).
 *
 * <p>
 * Delegates to {@link WorkspaceMcpServerFacade#updateWorkspaceMcpServer}, whose {@code @PreAuthorize} check enforces
 * the tenant boundary — a caller cannot rename or disable a server outside a workspace they may edit.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class UpdateMcpServerToolCallback implements ToolCallback {

    static final String TOOL_NAME = "updateMcpServer";

    private static final String DESCRIPTION = """
        Update an existing MCP server's name and/or enabled state. Supply mcpServerId (numeric, from
        listMcpServers). At least one of name or enabled must be supplied — fields you omit are left
        unchanged. Use enabled=true to bring a server online or enabled=false to take it offline. Returns
        {mcpServerId, name, enabled} reflecting the post-update values.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "mcpServerId": {"type": "string", "description": "Numeric MCP server id from listMcpServers"},
                    "name": {"type": "string", "description": "New server name (optional; leave omitted to keep)"},
                    "enabled": {"type": "boolean", "description": "New enabled state (optional; leave omitted to keep)"}
                },
                "required": ["mcpServerId"]
            }""";

    private final WorkspaceMcpServerFacade workspaceMcpServerFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public UpdateMcpServerToolCallback(WorkspaceMcpServerFacade workspaceMcpServerFacade) {
        this.workspaceMcpServerFacade = workspaceMcpServerFacade;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(TOOL_NAME)
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            UpdateMcpServerInput input = jsonMapper.readValue(toolInput, UpdateMcpServerInput.class);

            if (input.mcpServerId() == null || input.mcpServerId()
                .isBlank()) {
                return toolError("mcpServerId is required");
            }

            boolean nameSupplied = input.name() != null && !input.name()
                .isBlank();
            boolean enabledSupplied = input.enabled() != null;

            if (!nameSupplied && !enabledSupplied) {
                return toolError("Supply at least one of name or enabled to update");
            }

            long mcpServerId;

            try {
                mcpServerId = Long.parseLong(input.mcpServerId());
            } catch (NumberFormatException exception) {
                return toolError("Invalid mcpServerId — must be a numeric id");
            }

            McpServer updated = workspaceMcpServerFacade.updateWorkspaceMcpServer(
                mcpServerId, nameSupplied ? input.name() : null, input.enabled());

            return jsonMapper.writeValueAsString(
                new UpdateMcpServerOutput(updated.getId(), updated.getName(), updated.isEnabled()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, UpdateMcpServerToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record UpdateMcpServerInput(String mcpServerId, @Nullable String name, @Nullable Boolean enabled) {
    }

    public record UpdateMcpServerOutput(long mcpServerId, String name, boolean enabled) {
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests "com.bytechef.ee.ai.hub.tool.UpdateMcpServerToolCallbackTest"`
Expected: PASS (all three tests).

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/UpdateMcpServerToolCallback.java \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/UpdateMcpServerToolCallbackTest.java
git commit -m "Add UpdateMcpServerToolCallback for AI Hub agent"
```

---

### Task 4: Register both tools on the AI Hub agent

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubConfiguration.java` (imports near line 101; registration block at lines 557-558)

**Interfaces:**
- Consumes: `CreateMcpServerToolCallback` (Task 2), `UpdateMcpServerToolCallback` (Task 3), the already-injected `workspaceMcpServerFacadeProvider`.
- Produces: both tool callbacks are now on the agent's `toolCallbacks` list whenever `WorkspaceMcpServerFacade` is present.

- [ ] **Step 1: Add the imports**

In `AiHubConfiguration.java`, next to the existing `import com.bytechef.ee.ai.hub.tool.CreateMcpProjectToolCallback;` (line 81) and `import com.bytechef.ee.ai.hub.tool.ListMcpServersToolCallback;` (line 101), add (keeping imports alphabetically ordered):

```java
import com.bytechef.ee.ai.hub.tool.CreateMcpServerToolCallback;
import com.bytechef.ee.ai.hub.tool.UpdateMcpServerToolCallback;
```

- [ ] **Step 2: Extend the registration block**

Replace the existing block at lines 557-558:

```java
        workspaceMcpServerFacadeProvider.ifAvailable(
            workspaceMcpServerFacade -> toolCallbacks.add(new ListMcpServersToolCallback(workspaceMcpServerFacade)));
```

with:

```java
        workspaceMcpServerFacadeProvider.ifAvailable(workspaceMcpServerFacade -> {
            toolCallbacks.add(new ListMcpServersToolCallback(workspaceMcpServerFacade));
            toolCallbacks.add(new CreateMcpServerToolCallback(workspaceMcpServerFacade));
            toolCallbacks.add(new UpdateMcpServerToolCallback(workspaceMcpServerFacade));
        });
```

- [ ] **Step 3: Compile the module**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run the ai-hub-service test suite**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test`
Expected: BUILD SUCCESSFUL — including the two new tool-callback test classes. If a `NonEmptyToolCallbackTest`-style guard asserts every tool has a non-blank name/description/schema, it now covers `createMcpServer` and `updateMcpServer` automatically; both have all three.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubConfiguration.java
git commit -m "Register createMcpServer/updateMcpServer tools on AI Hub agent"
```

---

### Task 5: Full verification

**Files:** none (verification only)

- [ ] **Step 1: Run checks across both changed modules**

Run:
```bash
./gradlew spotlessApply
./gradlew :server:libs:automation:automation-ai:automation-ai-mcp:automation-ai-mcp-service:check \
          :server:ee:libs:ai:ai-hub:ai-hub-service:check
```
Expected: BUILD SUCCESSFUL (Spotless, Checkstyle, PMD, SpotBugs, tests all green).

- [ ] **Step 2: Manual smoke check (optional, requires running stack)**

With the dev stack up (`docker compose -f server/docker-compose.dev.infra.yml up -d` then `./gradlew -p server/apps/server-app bootRun`) and `bytechef.ai.hub` enabled, open the AI Hub chat in a workspace and prompt: *"Create an MCP server called Support Tools in staging, then enable it."* Expect the agent to call `createMcpServer` (returns disabled) then `updateMcpServer` with `enabled=true`, and the new server to appear on the MCP Servers list page.

- [ ] **Step 3: Final commit if any formatting changed**

```bash
git status
# if spotlessApply changed anything already committed, amend or add a follow-up commit
```
