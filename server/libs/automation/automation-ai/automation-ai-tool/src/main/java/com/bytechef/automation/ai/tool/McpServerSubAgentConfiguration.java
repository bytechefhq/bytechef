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

import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.automation.ai.mcp.facade.McpProjectWorkflowFacade;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnExpression("${bytechef.ai.copilot.enabled:false} or ${bytechef.ai.hub.enabled:false}")
public class McpServerSubAgentConfiguration {

    @Value("classpath:prompt_mcp_agent.txt")
    private Resource promptResource;

    @Bean
    @ConditionalOnBean({
        McpProjectWorkflowFacade.class
    })
    ChatClient mcpServerBuildSubAgentChatClient(
        ChatModel chatModel, McpProjectWorkflowFacade mcpProjectWorkflowFacade) {

        return ChatClient.builder(chatModel)
            .defaultSystem(readPrompt(promptResource))
            .defaultTools(
                new ListMcpProjectWorkflowsToolCallback(mcpProjectWorkflowFacade),
                new UpdateMcpProjectWorkflowParametersToolCallback(mcpProjectWorkflowFacade))
            .build();
    }

    @Bean
    @ConditionalOnBean({
        McpProjectWorkflowFacade.class
    })
    IntelligentToolChatClientFactory mcpServerBuildSubAgentChatClientFactory(
        @Qualifier("mcpServerBuildSubAgentChatClient") ChatClient mcpServerBuildSubAgentChatClient) {

        return chatModel -> mcpServerBuildSubAgentChatClient;
    }

    private String readPrompt(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read mcp server prompt resource: " + resource.getDescription(), exception);
        }
    }
}
