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

package com.bytechef.component.mcp.client.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.ToolContextToMcpMetaConverter;

/**
 * @author Ivica Cardic
 */
class McpClientUtilsTest {

    @Test
    void testToolContextToMcpMetaConverterDropsTheConversationId() {
        ToolContextToMcpMetaConverter toolContextToMcpMetaConverter =
            McpClientUtils.createToolContextToMcpMetaConverter();

        Map<String, Object> mcpMeta = toolContextToMcpMetaConverter.convert(
            new ToolContext(
                Map.of(
                    ChatMemory.CONVERSATION_ID, "conversation-1",
                    McpToolUtils.TOOL_CONTEXT_MCP_EXCHANGE_KEY, "exchange",
                    "tenantHint", "tenant-1")));

        assertThat(mcpMeta).containsExactly(Map.entry("tenantHint", "tenant-1"));
    }

    @Test
    void testToolContextToMcpMetaConverterHandlesAnEmptyToolContext() {
        ToolContextToMcpMetaConverter toolContextToMcpMetaConverter =
            McpClientUtils.createToolContextToMcpMetaConverter();

        assertThat(toolContextToMcpMetaConverter.convert(new ToolContext(Map.of()))).isEmpty();
    }
}
