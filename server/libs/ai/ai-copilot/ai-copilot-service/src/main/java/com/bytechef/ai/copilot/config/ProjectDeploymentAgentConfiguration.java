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
import com.bytechef.ai.copilot.advisor.CopilotGuardrailsAdvisorFactory;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.agent.SliceSpringAIAgent;
import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.automation.ai.tool.DeploymentToolCallbacksFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
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
public class ProjectDeploymentAgentConfiguration {

    @Value("classpath:prompt_project_deployment_ask.txt")
    private Resource promptProjectDeploymentAskResource;

    @Value("classpath:prompt_project_deployment_build.txt")
    private Resource promptProjectDeploymentBuildResource;

    private final State state = new State();

    @Bean
    SliceSpringAIAgent projectDeploymentAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, DeploymentToolCallbacksFactory deploymentToolCallbacksFactory,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider,
        CopilotGuardrailsAdvisorFactory copilotGuardrailsAdvisorFactory)
        throws AGUIException {

        return SliceSpringAIAgent.builder()
            .advisors(copilotGuardrailsAdvisorFactory.guardrailsAdvisors())
            .agentId(CopilotAgentType.PROJECT_DEPLOYMENT_ASK.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptProjectDeploymentAskResource))
            .state(state)
            .toolCallbacks(askToolCallbacks(securityContextRehydrator, deploymentToolCallbacksFactory))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    SliceSpringAIAgent projectDeploymentBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, DeploymentToolCallbacksFactory deploymentToolCallbacksFactory,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider,
        CopilotGuardrailsAdvisorFactory copilotGuardrailsAdvisorFactory)
        throws AGUIException {

        return SliceSpringAIAgent.builder()
            .advisors(copilotGuardrailsAdvisorFactory.guardrailsAdvisors())
            .agentId(CopilotAgentType.PROJECT_DEPLOYMENT_BUILD.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptProjectDeploymentBuildResource))
            .state(state)
            .toolCallbacks(buildToolCallbacks(securityContextRehydrator, deploymentToolCallbacksFactory))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    List<ToolCallback> askToolCallbacks(
        SecurityContextRehydrator securityContextRehydrator,
        DeploymentToolCallbacksFactory deploymentToolCallbacksFactory) {

        return wrapToolCallbacks(securityContextRehydrator, deploymentToolCallbacksFactory.readToolCallbacks());
    }

    List<ToolCallback> buildToolCallbacks(
        SecurityContextRehydrator securityContextRehydrator,
        DeploymentToolCallbacksFactory deploymentToolCallbacksFactory) {

        return wrapToolCallbacks(securityContextRehydrator, deploymentToolCallbacksFactory.writeToolCallbacks());
    }

    private List<ToolCallback> wrapToolCallbacks(
        SecurityContextRehydrator securityContextRehydrator, List<ToolCallback> toolCallbacks) {

        List<ToolCallback> wrapped = new ArrayList<>(toolCallbacks.size());

        for (ToolCallback toolCallback : toolCallbacks) {
            wrapped.add(RehydrateContextToolCallback.wrap(toolCallback, securityContextRehydrator));
        }

        return wrapped;
    }

    private String readPrompt(Resource resource) {
        try {
            InputStream inputStream = resource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read project deployment prompt resource: " + resource.getDescription(), exception);
        }
    }
}
