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
import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
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
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

/**
 * @author Ivica Cardic
 */
@AutoConfiguration(after = ToolCallbacksFactoryConfiguration.class)
@ConditionalOnBean(AiAgentToolCallbacksFactory.class)
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class AiAgentAgentConfiguration {

    private final State state = new State();

    @Bean
    AiAgentSpringAIAgent aiAgentAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, AiAgentToolCallbacksFactory aiAgentToolCallbacksFactory,
        InteractivePickerToolCallbacks interactivePickerToolCallbacks,
        @Value("classpath:prompt_ai_agent_ask.txt") Resource promptAiAgentAskResource,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        return AiAgentSpringAIAgent.builder()
            .agentId(CopilotAgentType.AI_AGENT_ASK.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptAiAgentAskResource))
            .state(state)
            .toolCallbacks(
                wrapToolCallbacks(
                    securityContextRehydrator, aiAgentToolCallbacksFactory.readToolCallbacks(),
                    interactivePickerToolCallbacks.read()))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    AiAgentSpringAIAgent aiAgentBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, AiAgentToolCallbacksFactory aiAgentToolCallbacksFactory,
        InteractivePickerToolCallbacks interactivePickerToolCallbacks,
        @Value("classpath:prompt_ai_agent_build.txt") Resource promptAiAgentBuildResource,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        return AiAgentSpringAIAgent.builder()
            .agentId(CopilotAgentType.AI_AGENT_BUILD.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptAiAgentBuildResource))
            .state(state)
            .toolCallbacks(
                wrapToolCallbacks(
                    securityContextRehydrator, aiAgentToolCallbacksFactory.writeToolCallbacks(),
                    interactivePickerToolCallbacks.write()))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    private static List<ToolCallback> wrapToolCallbacks(
        SecurityContextRehydrator securityContextRehydrator, List<ToolCallback> aiAgentToolCallbacks,
        List<ToolCallback> pickerToolCallbacks) {

        List<ToolCallback> wrappedToolCallbacks = new ArrayList<>(
            aiAgentToolCallbacks.size() + pickerToolCallbacks.size());

        for (ToolCallback toolCallback : aiAgentToolCallbacks) {
            wrappedToolCallbacks.add(RehydrateContextToolCallback.wrap(toolCallback, securityContextRehydrator));
        }

        for (ToolCallback toolCallback : pickerToolCallbacks) {
            wrappedToolCallbacks.add(RehydrateContextToolCallback.wrap(toolCallback, securityContextRehydrator));
        }

        return wrappedToolCallbacks;
    }

    private static String readPrompt(Resource resource) {
        try {
            InputStream inputStream = resource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read AI Agent prompt resource: " + resource.getDescription(), exception);
        }
    }
}
