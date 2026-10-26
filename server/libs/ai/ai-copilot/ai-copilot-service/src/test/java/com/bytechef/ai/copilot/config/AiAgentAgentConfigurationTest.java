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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.core.exception.AGUIException;
import com.bytechef.ai.copilot.agent.AiAgentSpringAIAgent;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.util.Mode;
import com.bytechef.ai.copilot.util.Source;
import com.bytechef.automation.ai.tool.aiagent.AiAgentToolCallbacksFactory;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * @author Ivica Cardic
 */
class AiAgentAgentConfigurationTest {

    private static final Resource PROMPT_RESOURCE = new ByteArrayResource("prompt".getBytes(StandardCharsets.UTF_8));

    private final AiAgentAgentConfiguration aiAgentAgentConfiguration = new AiAgentAgentConfiguration();
    private final AiAgentToolCallbacksFactory aiAgentToolCallbacksFactory = mock(AiAgentToolCallbacksFactory.class);
    private final InteractivePickerToolCallbacks interactivePickerToolCallbacks =
        mock(InteractivePickerToolCallbacks.class);

    @Test
    void testAskAgentIsRegisteredUnderTheAiAgentSourceIdWithTheReadTools() throws AGUIException {
        when(aiAgentToolCallbacksFactory.readToolCallbacks()).thenReturn(List.of());
        when(interactivePickerToolCallbacks.read()).thenReturn(List.of());

        AiAgentSpringAIAgent aiAgentSpringAIAgent = aiAgentAgentConfiguration.aiAgentAskSpringAIAgent(
            mock(ChatMemory.class), mock(ChatModel.class), aiAgentToolCallbacksFactory, interactivePickerToolCallbacks,
            PROMPT_RESOURCE,
            mock(SecurityContextRehydrator.class), emptyOverrideChatClientResolverProvider());

        assertThat(aiAgentSpringAIAgent.getAgentId()).isEqualTo(Source.AI_AGENT.agentId(Mode.ASK));

        verify(aiAgentToolCallbacksFactory).readToolCallbacks();
        verify(aiAgentToolCallbacksFactory, never()).writeToolCallbacks();
        verify(interactivePickerToolCallbacks).read();
        verify(interactivePickerToolCallbacks, never()).write();
    }

    @Test
    void testBuildAgentIsRegisteredUnderTheAiAgentSourceIdWithTheWriteTools() throws AGUIException {
        when(aiAgentToolCallbacksFactory.writeToolCallbacks()).thenReturn(List.of());
        when(interactivePickerToolCallbacks.write()).thenReturn(List.of());

        AiAgentSpringAIAgent aiAgentSpringAIAgent = aiAgentAgentConfiguration.aiAgentBuildSpringAIAgent(
            mock(ChatMemory.class), mock(ChatModel.class), aiAgentToolCallbacksFactory, interactivePickerToolCallbacks,
            PROMPT_RESOURCE,
            mock(SecurityContextRehydrator.class), emptyOverrideChatClientResolverProvider());

        assertThat(aiAgentSpringAIAgent.getAgentId()).isEqualTo(Source.AI_AGENT.agentId(Mode.BUILD));

        verify(aiAgentToolCallbacksFactory).writeToolCallbacks();
        verify(interactivePickerToolCallbacks).write();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<OverrideChatClientResolver> emptyOverrideChatClientResolverProvider() {
        return mock(ObjectProvider.class);
    }
}
