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

package com.bytechef.ai.copilot.config;

import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.agent.ProjectSpringAIAgent;
import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolScope;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowLifecycleTools;
import com.bytechef.automation.ai.tool.ReadProjectTools;
import com.bytechef.automation.ai.tool.ReadProjectWorkflowTools;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class ProjectAgentConfiguration {

    @Value("classpath:prompt_project_ask.txt")
    private Resource promptProjectAskResource;

    @Value("classpath:prompt_project_build.txt")
    private Resource promptProjectBuildResource;

    private final State state = new State();

    @Bean
    ProjectSpringAIAgent projectAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        return ProjectSpringAIAgent.builder()
            .agentId(CopilotAgentType.PROJECT_ASK.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptProjectAskResource))
            .state(state)
            .toolCallbacks(askToolCallbacks(securityContextRehydrator, readProjectTools, readProjectWorkflowTools))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    ProjectSpringAIAgent projectBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ProjectTools projectTools,
        ProjectWorkflowLifecycleTools projectWorkflowLifecycleTools,
        SecurityContextRehydrator securityContextRehydrator,
        IntelligentToolCatalog intelligentToolCatalog,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        return ProjectSpringAIAgent.builder()
            .agentId(CopilotAgentType.PROJECT_BUILD.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptProjectBuildResource))
            .state(state)
            .toolCallbacks(
                buildToolCallbacks(
                    securityContextRehydrator, projectTools, projectWorkflowLifecycleTools, intelligentToolCatalog))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    List<ToolCallback> askToolCallbacks(
        SecurityContextRehydrator securityContextRehydrator, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools) {

        return wrapTools(securityContextRehydrator, List.of(readProjectTools, readProjectWorkflowTools));
    }

    List<ToolCallback> buildToolCallbacks(
        SecurityContextRehydrator securityContextRehydrator, ProjectTools projectTools,
        ProjectWorkflowLifecycleTools projectWorkflowLifecycleTools, IntelligentToolCatalog intelligentToolCatalog) {

        List<ToolCallback> toolCallbacks =
            new ArrayList<>(
                wrapTools(securityContextRehydrator, List.of(projectTools, projectWorkflowLifecycleTools)));

        toolCallbacks.addAll(
            intelligentToolCatalog.getForPanel(
                IntelligentToolScope.PROJECT, IntelligentToolVariant.BUILD, (chatClient, definition) -> chatClient,
                (toolCallback, definition) -> RehydrateContextToolCallback.wrap(
                    toolCallback, securityContextRehydrator)));

        return toolCallbacks;
    }

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

    private String readPrompt(Resource resource) {
        try {
            InputStream inputStream = resource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read project prompt resource: " + resource.getDescription(), exception);
        }
    }
}
