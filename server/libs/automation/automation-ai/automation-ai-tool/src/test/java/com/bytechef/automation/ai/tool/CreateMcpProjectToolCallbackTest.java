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

import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.facade.McpProjectFacade;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 *
 * @author Ivica Cardic
 */
class CreateMcpProjectToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testCreateMcpProjectExposesWorkflowsAsTools() throws Exception {
        // Pin the contract: the LLM's workflowIds list passes through verbatim to McpProjectFacade.createMcpProject —
        // the callback does not silently drop, dedupe, or reorder. If we ever started filtering, the user would see a
        // tool count mismatch with no signal of why; surface that in tests so the contract stays explicit.
        McpProjectFacade facade = mock(McpProjectFacade.class);

        McpProject created = new McpProject(50L, 10L);

        created.setId(99L);

        when(facade.createMcpProject(eq(10L), eq(42L), eq(3), eq(List.of("wf-a", "wf-b"))))
            .thenReturn(created);

        CreateMcpProjectToolCallback callback = new CreateMcpProjectToolCallback(facade);

        String result = callback.call(
            "{\"mcpServerId\":\"10\",\"projectId\":\"42\",\"projectVersion\":3," +
                "\"workflowIds\":[\"wf-a\",\"wf-b\"]}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("mcpProjectId")
            .asLong()).isEqualTo(99L);
        assertThat(node.get("projectDeploymentId")
            .asLong()).isEqualTo(50L);
        assertThat(node.get("mcpServerId")
            .asLong()).isEqualTo(10L);
        assertThat(node.get("exposedWorkflowCount")
            .asInt()).isEqualTo(2);

        verify(facade).createMcpProject(10L, 42L, 3, List.of("wf-a", "wf-b"));
    }

    @Test
    void testRejectsEmptyWorkflowIds() throws Exception {
        CreateMcpProjectToolCallback callback = new CreateMcpProjectToolCallback(mock(McpProjectFacade.class));

        String result = callback.call(
            "{\"mcpServerId\":\"10\",\"projectId\":\"42\",\"projectVersion\":3,\"workflowIds\":[]}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("workflowIds");
    }

    @Test
    void testRejectsBadServerId() throws Exception {
        CreateMcpProjectToolCallback callback = new CreateMcpProjectToolCallback(mock(McpProjectFacade.class));

        String result = callback.call(
            "{\"mcpServerId\":\"not-a-number\",\"projectId\":\"42\",\"projectVersion\":3," +
                "\"workflowIds\":[\"wf-a\"]}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("mcpServerId");
    }
}
