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
import com.bytechef.ai.copilot.agent.AiAgentSpringAIAgent;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.util.Mode;
import com.bytechef.ai.copilot.util.Source;
import com.bytechef.automation.ai.tool.DeploymentToolCallbacksFactory;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Registers the deployments Copilot panel source agents ({@code deployment_ask}/{@code deployment_build}) backing the
 * copilot trigger on the project-deployments and agent-deployments pages.
 *
 * <p>
 * Deliberately registers NO subagent {@code ChatClient} beans, unlike the other domain slices: the AI Hub already
 * reaches this domain through the {@code project_deployment_agent} subagent, which has no ASK/BUILD split by design.
 * Adding hub delegates here would give the hub two competing paths to the same deployment tools. Both surfaces share
 * one {@link DeploymentToolCallbacksFactory} so their tool sets cannot drift.
 * </p>
 *
 * <p>
 * Gated so the panel agents exist whenever the Copilot panel is enabled, and also under the AI Hub toggle for parity
 * with the other domain configurations.
 * </p>
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnExpression("${bytechef.ai.copilot.enabled:false} or ${bytechef.ai.hub.enabled:false}")
public class DeploymentAgentConfiguration {

    @Value("classpath:prompt_deployment_ask.txt")
    private Resource promptDeploymentAskResource;

    @Value("classpath:prompt_deployment_build.txt")
    private Resource promptDeploymentBuildResource;

    private final State state = new State();

    @Bean
    DeploymentToolCallbacksFactory deploymentToolCallbacksFactory(ProjectDeploymentFacade projectDeploymentFacade) {
        return new DeploymentToolCallbacksFactory(projectDeploymentFacade);
    }

    @Bean
    AiAgentSpringAIAgent deploymentAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, DeploymentToolCallbacksFactory deploymentToolCallbacksFactory,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.DEPLOYMENT.name() + "_" + Mode.ASK.name();

        return AiAgentSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptDeploymentAskResource))
            .state(state)
            .toolCallbacks(
                wrapToolCallbacks(securityContextRehydrator, deploymentToolCallbacksFactory.readToolCallbacks()))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    AiAgentSpringAIAgent deploymentBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, DeploymentToolCallbacksFactory deploymentToolCallbacksFactory,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.DEPLOYMENT.name() + "_" + Mode.BUILD.name();

        return AiAgentSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptDeploymentBuildResource))
            .state(state)
            .toolCallbacks(
                wrapToolCallbacks(securityContextRehydrator, deploymentToolCallbacksFactory.writeToolCallbacks()))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
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
                "Failed to read deployment prompt resource: " + resource.getDescription(), exception);
        }
    }
}
