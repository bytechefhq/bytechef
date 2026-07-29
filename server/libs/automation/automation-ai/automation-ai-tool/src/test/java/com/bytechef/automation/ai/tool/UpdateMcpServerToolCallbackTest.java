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
