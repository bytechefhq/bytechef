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

package com.bytechef.component.ai.llm.advisor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.resolution.DelegatingToolCallbackResolver;

/**
 * @author Ivica Cardic
 */
class ToolCallAwareStructuredOutputValidationAdvisorTest {

    private static final String JSON_SCHEMA = """
        {"type":"object","required":["result"],"additionalProperties":false,
         "properties":{"result":{"type":"string"}}}
        """;

    private static final String TOOL_NAME = "lookUp";
    private static final String VALID_JSON = "{\"result\":\"done\"}";

    @Test
    void testToolCallReplyIsNotRetriedAndTheToolRunsOnce() {
        List<Prompt> prompts = new CopyOnWriteArrayList<>();
        AtomicInteger toolCallCount = new AtomicInteger();

        ChatModel chatModel = createChatModel(
            prompts, prompt -> getLastMessage(prompt) instanceof ToolResponseMessage
                ? textResponse(VALID_JSON) : toolCallResponse());

        String content = createChatClientRequestSpec(chatModel, toolCallCount, 3)
            .call()
            .content();

        assertThat(content).isEqualTo(VALID_JSON);
        assertThat(prompts).hasSize(2);
        assertThat(toolCallCount).hasValue(1);
    }

    @Test
    void testInvalidAnswerIsRetriedWithTheValidationError() {
        List<Prompt> prompts = new CopyOnWriteArrayList<>();

        ChatModel chatModel = createChatModel(
            prompts, prompt -> prompts.size() == 1 ? textResponse("{\"other\":1}") : textResponse(VALID_JSON));

        String content = createChatClientRequestSpec(chatModel, new AtomicInteger(), 3)
            .call()
            .content();

        assertThat(content).isEqualTo(VALID_JSON);
        assertThat(prompts).hasSize(2);

        UserMessage retryUserMessage = prompts.get(1)
            .getUserMessage();

        assertThat(retryUserMessage.getText()).contains("Output JSON validation failed because of:");
    }

    @Test
    void testInvalidAnswerAfterAToolCallIsRetriedWithoutRunningTheToolAgain() {
        List<Prompt> prompts = new CopyOnWriteArrayList<>();
        AtomicInteger toolCallCount = new AtomicInteger();

        ChatModel chatModel = createChatModel(
            prompts, prompt -> switch (prompts.size()) {
                case 1 -> toolCallResponse();
                case 2 -> textResponse("{\"other\":1}");
                default -> textResponse(VALID_JSON);
            });

        String content = createChatClientRequestSpec(chatModel, toolCallCount, 3)
            .call()
            .content();

        assertThat(content).isEqualTo(VALID_JSON);
        assertThat(prompts).hasSize(3);
        assertThat(toolCallCount).hasValue(1);

        List<Message> retryInstructions = prompts.get(2)
            .getInstructions();

        assertThat(retryInstructions).anyMatch(message -> message instanceof ToolResponseMessage);
        assertThat(prompts.get(2)
            .getUserMessage()
            .getText()).contains("Output JSON validation failed because of:");
    }

    @Test
    void testRetriesStopAfterTheMaxRepeatAttempts() {
        List<Prompt> prompts = new CopyOnWriteArrayList<>();

        ChatModel chatModel = createChatModel(prompts, prompt -> textResponse("not json"));

        String content = createChatClientRequestSpec(chatModel, new AtomicInteger(), 2)
            .call()
            .content();

        assertThat(content).isEqualTo("not json");
        assertThat(prompts).hasSize(3);
    }

    private static ChatClient.ChatClientRequestSpec createChatClientRequestSpec(
        ChatModel chatModel, AtomicInteger toolCallCount, int maxRepeatAttempts) {

        ToolCallback toolCallback = FunctionToolCallback
            .<String, String>builder(TOOL_NAME, input -> {
                toolCallCount.incrementAndGet();

                return "looked up";
            })
            .inputType(String.class)
            .build();

        ChatClient chatClient = ChatClient.builder(chatModel)
            .build();

        return chatClient.prompt()
            .user("answer in JSON")
            .advisors(
                ToolCallingAdvisor.builder()
                    .toolCallingManager(
                        DefaultToolCallingManager.builder()
                            .toolCallbackResolver(new DelegatingToolCallbackResolver(List.of()))
                            .build())
                    .build(),
                new ToolCallAwareStructuredOutputValidationAdvisor(JSON_SCHEMA, maxRepeatAttempts))
            .tools(toolCallback);
    }

    private static ChatModel createChatModel(List<Prompt> prompts, ResponseFunction responseFunction) {
        return new ChatModel() {

            @Override
            public ChatResponse call(Prompt prompt) {
                prompts.add(prompt);

                return responseFunction.apply(prompt);
            }

            @Override
            public ToolCallingChatOptions getOptions() {
                return ToolCallingChatOptions.builder()
                    .build();
            }
        };
    }

    private static Message getLastMessage(Prompt prompt) {
        List<Message> instructions = prompt.getInstructions();

        return instructions.getLast();
    }

    private static ChatResponse textResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    private static ChatResponse toolCallResponse() {
        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(new AssistantMessage.ToolCall("call_1", "function", TOOL_NAME, "\"x\"")))
            .build();

        return new ChatResponse(List.of(new Generation(assistantMessage)));
    }

    @FunctionalInterface
    private interface ResponseFunction {

        ChatResponse apply(Prompt prompt);
    }
}
