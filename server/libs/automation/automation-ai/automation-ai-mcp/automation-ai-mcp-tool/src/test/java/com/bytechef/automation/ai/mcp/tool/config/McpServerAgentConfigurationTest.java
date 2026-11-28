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

package com.bytechef.automation.ai.mcp.tool.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.agui.core.exception.AGUIException;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.automation.ai.mcp.tool.agent.McpServerSpringAIAgent;
import com.bytechef.platform.ai.tool.SecurityContextRehydrator;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * @author Ivica Cardic
 */
class McpServerAgentConfigurationTest {

    private static final Resource PROMPT_RESOURCE = new ByteArrayResource("prompt".getBytes(StandardCharsets.UTF_8));

    private final McpServerAgentConfiguration mcpServerAgentConfiguration = new McpServerAgentConfiguration();

    @Test
    void testMcpServerBuildAgentIsRegisteredUnderTheMcpServerSourceId() throws AGUIException {
        McpServerSpringAIAgent mcpServerSpringAIAgent = mcpServerAgentConfiguration.mcpServerBuildSpringAIAgent(
            mock(ChatMemory.class), mock(ChatModel.class),
            McpServerAgentConfigurationTest.<IntelligentToolChatClientFactory>emptyProvider(),
            PROMPT_RESOURCE, mock(SecurityContextRehydrator.class),
            McpServerAgentConfigurationTest.<OverrideChatClientResolver>emptyProvider(),
            McpServerAgentConfigurationTest.<SubAgentChatModelResolver>emptyProvider());

        assertThat(mcpServerSpringAIAgent.getAgentId()).isEqualTo("mcp_server_build");
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        return mock(ObjectProvider.class);
    }
}
