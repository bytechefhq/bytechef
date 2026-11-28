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

import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.automation.ai.mcp.tool.ConfigureMcpServerToolCallback;
import com.bytechef.automation.ai.mcp.tool.McpServerAgentType;
import com.bytechef.automation.ai.mcp.tool.agent.McpServerSpringAIAgent;
import com.bytechef.platform.ai.tool.RehydrateContextToolCallback;
import com.bytechef.platform.ai.tool.SecurityContextRehydrator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * @author Marko Kriskovic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class McpServerAgentConfiguration {

    private final State state = new State();

    private List<ToolCallback> wrapTools(SecurityContextRehydrator securityContextRehydrator, List<Object> tools) {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (Object tool : tools) {
            if (tool instanceof ToolCallback toolCallback) {
                toolCallbacks.add(RehydrateContextToolCallback.wrap(toolCallback, securityContextRehydrator));
            } else {
                for (ToolCallback toolCallback : ToolCallbacks.from(tool)) {
                    toolCallbacks.add(RehydrateContextToolCallback.wrap(toolCallback, securityContextRehydrator));
                }
            }
        }

        return toolCallbacks;
    }

    @Bean
    McpServerSpringAIAgent mcpServerBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel,
        @Qualifier("mcpServerBuildSubAgentChatClientFactory") ObjectProvider<IntelligentToolChatClientFactory> mcpServerBuildSubAgentChatClientFactoryProvider,
        @Value("classpath:prompt_mcp_server_build.txt") Resource promptMcpServerBuildResource,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider,
        ObjectProvider<SubAgentChatModelResolver> chatModelResolverProvider)
        throws AGUIException {

        IntelligentToolChatClientFactory mcpServerChatClientFactory =
            candidateChatModel -> mcpServerBuildSubAgentChatClientFactoryProvider.getObject()
                .get(candidateChatModel);

        return McpServerSpringAIAgent.builder()
            .agentId(McpServerAgentType.MCP_SERVER_BUILD.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptMcpServerBuildResource))
            .state(state)
            .toolCallbacks(
                wrapTools(
                    securityContextRehydrator,
                    List.of(
                        new ConfigureMcpServerToolCallback(
                            mcpServerChatClientFactory, chatModelResolverProvider.getIfAvailable()))))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    private String getSystemPrompt(Resource systemPromptResource) {
        try {
            InputStream inputStream = systemPromptResource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read system prompt resource: " + systemPromptResource.getDescription(), exception);
        }
    }
}
