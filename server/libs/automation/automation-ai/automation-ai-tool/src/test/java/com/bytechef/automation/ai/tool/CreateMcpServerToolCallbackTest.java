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

package com.bytechef.automation.ai.tool;

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
            Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 99L));

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
    void testPassesEnabledTrueThrough() throws Exception {
        WorkspaceMcpServerFacade facade = mock(WorkspaceMcpServerFacade.class);
        McpServer created = mock(McpServer.class);

        when(created.getId()).thenReturn(43L);
        when(created.getName()).thenReturn("Live tools");
        when(created.getType()).thenReturn(PlatformType.AUTOMATION);
        when(created.getEnvironment()).thenReturn(Environment.PRODUCTION);
        when(created.isEnabled()).thenReturn(true);
        when(facade.createWorkspaceMcpServer(
            eq("Live tools"), eq(PlatformType.AUTOMATION), eq(Environment.PRODUCTION), eq(true), eq(99L)))
                .thenReturn(created);

        CreateMcpServerToolCallback callback = new CreateMcpServerToolCallback(facade);

        ToolContext toolContext = new ToolContext(
            Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 99L));

        String result = callback.call(
            "{\"name\":\"Live tools\",\"environment\":\"PRODUCTION\",\"enabled\":true}", toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("enabled")
            .asBoolean()).isTrue();

        verify(facade).createWorkspaceMcpServer(
            eq("Live tools"), eq(PlatformType.AUTOMATION), eq(Environment.PRODUCTION), eq(true), eq(99L));
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
            Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 99L));

        String result = callback.call("{\"name\":\"X\",\"environment\":\"NOPE\"}", toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("Unknown environment");
    }
}
