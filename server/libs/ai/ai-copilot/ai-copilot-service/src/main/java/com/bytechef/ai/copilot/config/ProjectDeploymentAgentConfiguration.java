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
import com.bytechef.ai.copilot.agent.ManagerSliceSpringAIAgent;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.util.Mode;
import com.bytechef.ai.copilot.util.Source;
import com.bytechef.automation.ai.tool.ProjectDeploymentToolCallbacksFactory;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Registers the Project Deployment Copilot panel source agents ({@code project_deployment_ask}/
 * {@code project_deployment_build}). Lives in CE alongside {@code CopilotConfiguration} because
 * {@link ProjectDeploymentToolCallbacksFactory} and the {@link ProjectDeploymentFacade} it wraps are CE.
 *
 * <p>
 * This configuration is purely additive: it does not touch {@code DeploymentManagerConfiguration}'s existing
 * {@code deploymentManagerChatClient} bean or {@code createDeploymentManagerToolCallback} factory methods, which back
 * the {@code deployment_manager} subagent consumed by AI Hub and the management MCP server. Those keep building their
 * own tool list independently of {@link ProjectDeploymentToolCallbacksFactory}.
 * </p>
 *
 * <p>
 * Gated on {@code bytechef.ai.copilot.enabled} rather than an OR with {@code bytechef.ai.hub.enabled}, matching
 * {@code ProjectAgentConfiguration}: an OR-gate would let this configuration register with
 * {@code hub.enabled=true, copilot.enabled=false}, when nothing in the AI Hub surface consumes these panel-agent beans.
 * </p>
 *
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
    ProjectDeploymentToolCallbacksFactory projectDeploymentToolCallbacksFactory(
        ProjectDeploymentFacade projectDeploymentFacade) {

        return new ProjectDeploymentToolCallbacksFactory(projectDeploymentFacade);
    }

    @Bean
    ManagerSliceSpringAIAgent projectDeploymentAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel,
        ProjectDeploymentToolCallbacksFactory projectDeploymentToolCallbacksFactory,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.PROJECT_DEPLOYMENT.name() + "_" + Mode.ASK.name();

        return ManagerSliceSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptProjectDeploymentAskResource))
            .state(state)
            .toolCallbacks(askToolCallbacks(securityContextRehydrator, projectDeploymentToolCallbacksFactory))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    ManagerSliceSpringAIAgent projectDeploymentBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel,
        ProjectDeploymentToolCallbacksFactory projectDeploymentToolCallbacksFactory,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.PROJECT_DEPLOYMENT.name() + "_" + Mode.BUILD.name();

        return ManagerSliceSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptProjectDeploymentBuildResource))
            .state(state)
            .toolCallbacks(buildToolCallbacks(securityContextRehydrator, projectDeploymentToolCallbacksFactory))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    /**
     * Package-private so {@code ProjectDeploymentAgentConfigurationTest} can assert on the resolved tool names directly
     * — {@link ManagerSliceSpringAIAgent} does not expose its wrapped {@link ToolCallback} list.
     */
    List<ToolCallback> askToolCallbacks(
        SecurityContextRehydrator securityContextRehydrator,
        ProjectDeploymentToolCallbacksFactory projectDeploymentToolCallbacksFactory) {

        return wrapToolCallbacks(
            securityContextRehydrator, projectDeploymentToolCallbacksFactory.readToolCallbacks());
    }

    /**
     * Package-private so {@code ProjectDeploymentAgentConfigurationTest} can assert on the resolved tool names directly
     * — {@link ManagerSliceSpringAIAgent} does not expose its wrapped {@link ToolCallback} list.
     */
    List<ToolCallback> buildToolCallbacks(
        SecurityContextRehydrator securityContextRehydrator,
        ProjectDeploymentToolCallbacksFactory projectDeploymentToolCallbacksFactory) {

        return wrapToolCallbacks(
            securityContextRehydrator, projectDeploymentToolCallbacksFactory.writeToolCallbacks());
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
