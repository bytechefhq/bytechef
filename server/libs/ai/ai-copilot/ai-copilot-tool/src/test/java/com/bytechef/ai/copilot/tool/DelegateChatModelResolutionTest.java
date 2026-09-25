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

package com.bytechef.ai.copilot.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.ai.copilot.agent.SelectedChatModelUnavailableException;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class DelegateChatModelResolutionTest {

    private static final String REQUEST_INPUT = "{\"request\": \"build it\"}";

    @ParameterizedTest
    @MethodSource("delegateConstructors")
    void testUsesTheResolvedChatModelForTheDelegation(
        String toolName, CallbackConstructor callbackConstructor, String toolInput) {
        ChatModel chatModel = mock(ChatModel.class);
        AtomicReference<ChatModel> received = new AtomicReference<>();

        SubAgentChatModelResolver chatModelResolver = toolContext -> chatModel;

        ToolCallback toolCallback = callbackConstructor.create(
            candidate -> {
                received.set(candidate);

                return stubChatClient("done");
            },
            chatModelResolver);

        toolCallback.call(toolInput, new ToolContext(Map.of()));

        assertThat(received.get())
            .as("%s must pass the resolved ChatModel into its chatClientFactory", toolName)
            .isSameAs(chatModel);
    }

    @ParameterizedTest
    @MethodSource("delegateConstructors")
    void testFallsBackToTheDefaultClientWhenTheResolverReturnsNull(
        String toolName, CallbackConstructor callbackConstructor, String toolInput) {

        AtomicReference<ChatModel> received = new AtomicReference<>();
        AtomicReference<Boolean> factoryInvoked = new AtomicReference<>(false);

        SubAgentChatModelResolver chatModelResolver = toolContext -> null;

        ToolCallback toolCallback = callbackConstructor.create(
            candidate -> {
                received.set(candidate);
                factoryInvoked.set(true);

                return stubChatClient("done");
            },
            chatModelResolver);

        String result = toolCallback.call(toolInput, new ToolContext(Map.of()));

        assertThat(factoryInvoked.get())
            .as("%s must still resolve a client from its default-yielding factory", toolName)
            .isTrue();
        assertThat(received.get())
            .as("%s must fall back to the default client when the resolver returns null", toolName)
            .isNull();
        assertThat(result).isEqualTo("done");
    }

    @ParameterizedTest
    @MethodSource("delegateConstructors")
    void testFailsTheTurnWhenTheSelectedModelIsUnavailable(
        String toolName, CallbackConstructor callbackConstructor, String toolInput) {

        AtomicReference<Boolean> factoryInvoked = new AtomicReference<>(false);

        SubAgentChatModelResolver chatModelResolver = toolContext -> {
            throw new SelectedChatModelUnavailableException("anthropic", "claude-opus-4", 1);
        };

        ToolCallback toolCallback = callbackConstructor.create(
            candidate -> {
                factoryInvoked.set(true);

                return stubChatClient("done");
            },
            chatModelResolver);

        assertThatThrownBy(() -> toolCallback.call(toolInput, new ToolContext(Map.of())))
            .as("%s must fail the turn rather than return a tool error", toolName)
            .isInstanceOf(SelectedChatModelUnavailableException.class)
            .hasMessageContaining("The selected model claude-opus-4 (anthropic) is not available in environment 1");

        assertThat(factoryInvoked.get())
            .as("%s must not run the delegate on another model", toolName)
            .isFalse();
    }

    @ParameterizedTest
    @MethodSource("delegateConstructors")
    void testFailsTheTurnWhenTheResolverThrows(
        String toolName, CallbackConstructor callbackConstructor, String toolInput) {

        AtomicReference<Boolean> factoryInvoked = new AtomicReference<>(false);

        SubAgentChatModelResolver chatModelResolver = toolContext -> {
            throw new IllegalStateException("model lookup failed");
        };

        ToolCallback toolCallback = callbackConstructor.create(
            candidate -> {
                factoryInvoked.set(true);

                return stubChatClient("done");
            },
            chatModelResolver);

        assertThatThrownBy(() -> toolCallback.call(toolInput, new ToolContext(Map.of())))
            .as("%s must fail the turn rather than fall back to the startup model", toolName)
            .isInstanceOf(ChatModelUnavailableException.class)
            .hasMessageContaining("model lookup failed");

        assertThat(factoryInvoked.get())
            .as("%s must not run the delegate on another model", toolName)
            .isFalse();
    }

    @ParameterizedTest
    @MethodSource("delegateConstructors")
    void testFallsBackToTheDefaultClientWhenThereIsNoResolver(
        String toolName, CallbackConstructor callbackConstructor, String toolInput) {

        AtomicReference<ChatModel> received = new AtomicReference<>();

        ToolCallback toolCallback = callbackConstructor.create(
            candidate -> {
                received.set(candidate);

                return stubChatClient("done");
            },
            null);

        String result = toolCallback.call(toolInput, new ToolContext(Map.of()));

        assertThat(received.get())
            .as("%s must fall back to the default client when no resolver was wired", toolName)
            .isNull();
        assertThat(result).isEqualTo("done");
    }

    private static Stream<Arguments> delegateConstructors() {
        return Stream.of(
            Arguments.of(
                "buildWorkflow", (CallbackConstructor) ProjectWorkflowAgentToolCallback::new, REQUEST_INPUT),
            Arguments.of("importWorkflow", (CallbackConstructor) ConverterAgentToolCallback::new, REQUEST_INPUT),
            Arguments.of(
                "configureClusterElement", (CallbackConstructor) ClusterElementAgentToolCallback::new, REQUEST_INPUT),
            Arguments.of("writeScript", (CallbackConstructor) CodeEditorAgentToolCallback::new, REQUEST_INPUT),
            Arguments.of("authorSkill", (CallbackConstructor) SkillsAgentToolCallback::new, REQUEST_INPUT),
            Arguments.of(
                "debugWorkflowExecution", (CallbackConstructor) WorkflowExecutionAgentToolCallback::new,
                REQUEST_INPUT),
            Arguments.of(
                "configureMcpServer", (CallbackConstructor) ConfigureMcpServerToolCallback::new,
                "{\"mcpServerId\": 1}"));
    }

    private static ChatClient stubChatClient(String content) {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);

        when(chatClient.prompt(anyString())
            .toolContext(anyMap())
            .call()
            .content()).thenReturn(content);

        return chatClient;
    }

    private interface CallbackConstructor {

        ToolCallback create(
            IntelligentToolChatClientFactory chatClientFactory, @Nullable SubAgentChatModelResolver chatModelResolver);
    }
}
