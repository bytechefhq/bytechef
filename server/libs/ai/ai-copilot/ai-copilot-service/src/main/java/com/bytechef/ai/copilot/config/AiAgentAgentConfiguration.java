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
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import com.bytechef.automation.ai.tool.aiagent.AiAgentToolCallbacksFactory;
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
 * Registers the AI Agent builder Copilot panel source agents ({@code ai_agent_ask}/{@code ai_agent_build}) and the
 * {@link AiAgentToolCallbacksFactory} bean the AI Hub ASK/BUILD agents and the management MCP surface flatten the AI
 * Agent (agent-builder) CRUD tool set from. Lives in CE alongside {@code CopilotConfiguration} because
 * {@link AiAgentToolCallbacksFactory} wraps the CE {@link AiAgentFacade}.
 *
 * <p>
 * There is no longer an {@code ai_agent_agent} subagent {@code ChatClient} pair here (ticket 732, CRUD-delegate-unwind
 * Task 8 — the LAST delegate in the plan): the {@code aiAgentAskSubAgentChatClient}/
 * {@code aiAgentBuildSubAgentChatClient} beans that used to back the dissolved delegate are gone along with it — see
 * {@code AiHubConfiguration#aiAgentFlatCrudToolCallbacks}/{@code #aiAgentCatalogToolCallbacks} and
 * {@code ToolCallbackContributorConfiguration#aiAgentFlatCrudMcpContributor}.
 * </p>
 *
 * <p>
 * Gated so the factory bean exists when either the Copilot panel or the AI Hub surface is enabled, since both consume
 * it (the panel agents below always need it; AI Hub/MCP need it only when the corresponding flattening runs).
 * </p>
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnExpression("${bytechef.ai.copilot.enabled:false} or ${bytechef.ai.hub.enabled:false}")
public class AiAgentAgentConfiguration {

    @Value("classpath:prompt_ai_agent_ask.txt")
    private Resource promptAiAgentAskResource;

    @Value("classpath:prompt_ai_agent_build.txt")
    private Resource promptAiAgentBuildResource;

    private final State state = new State();

    @Bean
    AiAgentToolCallbacksFactory aiAgentToolCallbacksFactory(AiAgentFacade aiAgentFacade) {
        return new AiAgentToolCallbacksFactory(aiAgentFacade);
    }

    @Bean
    AiAgentSpringAIAgent aiAgentAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, AiAgentToolCallbacksFactory aiAgentToolCallbacksFactory,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.AI_AGENT.name() + "_" + Mode.ASK.name();

        return AiAgentSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptAiAgentAskResource))
            .state(state)
            .toolCallbacks(
                wrapToolCallbacks(securityContextRehydrator, aiAgentToolCallbacksFactory.readToolCallbacks()))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    AiAgentSpringAIAgent aiAgentBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, AiAgentToolCallbacksFactory aiAgentToolCallbacksFactory,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.AI_AGENT.name() + "_" + Mode.BUILD.name();

        return AiAgentSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptAiAgentBuildResource))
            .state(state)
            .toolCallbacks(
                wrapToolCallbacks(securityContextRehydrator, aiAgentToolCallbacksFactory.writeToolCallbacks()))
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
                "Failed to read AI agent prompt resource: " + resource.getDescription(), exception);
        }
    }
}
