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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @author Ivica Cardic
 */
class JsonSchemaValidationAdvisorTest {

    private static final String JSON_SCHEMA = """
        {"type":"object","required":["result"],"additionalProperties":false,
         "properties":{"result":{"type":"array","items":{"type":"string"}}}}
        """;

    private static final String TOOL_NAME = "listItems";
    private static final String VALID_JSON = "{\"result\":[\"a\",\"b\"]}";

    @Test
    void testValidReplyIsReturnedWithoutRetry() {
        ScriptedChatModel chatModel = new ScriptedChatModel(textReply(VALID_JSON));

        String content = call(chatModel, new JsonSchemaValidationAdvisor(JSON_SCHEMA));

        assertEquals(VALID_JSON, content);
        assertEquals(1, chatModel.prompts.size());
    }

    @Test
    void testInvalidReplyIsRetriedWithValidationError() {
        ScriptedChatModel chatModel = new ScriptedChatModel(textReply("{\"name\":\"x\"}"), textReply(VALID_JSON));

        String content = call(chatModel, new JsonSchemaValidationAdvisor(JSON_SCHEMA));

        assertEquals(VALID_JSON, content);
        assertEquals(2, chatModel.prompts.size());

        UserMessage retryUserMessage = chatModel.prompts.get(1)
            .getUserMessage();

        assertTrue(retryUserMessage.getText()
            .contains("Output JSON validation failed because of:"));
    }

    @Test
    void testInvalidReplyIsReturnedAfterMaxRepeatAttempts() {
        ScriptedChatModel chatModel = new ScriptedChatModel(textReply("not json"));

        String content = call(chatModel, new JsonSchemaValidationAdvisor(JSON_SCHEMA, 2));

        assertEquals("not json", content);
        assertEquals(3, chatModel.prompts.size());
    }

    @Test
    void testToolCallReplyIsNotRetried() {
        ScriptedChatModel chatModel = new ScriptedChatModel(toolCallReply(), textReply(VALID_JSON));

        String content = callWithTool(chatModel, new JsonSchemaValidationAdvisor(JSON_SCHEMA));

        assertEquals(VALID_JSON, content);

        // One call that asks for the tool, one that answers with the tool result.
        assertEquals(2, chatModel.prompts.size());
    }

    @Test
    void testSpringValidationAdvisorRetriesToolCallReply() {
        // Pins why JsonSchemaValidationAdvisor replaces Spring AI's advisor: behind ToolCallingAdvisor, Spring AI's
        // advisor asks for the tool call 1 + maxRepeatAttempts times before the tool runs. If a Spring AI release
        // fixes this, this test fails and the replacement can be revisited.
        ScriptedChatModel chatModel = new ScriptedChatModel(
            toolCallReply(), toolCallReply(), toolCallReply(), toolCallReply(), textReply(VALID_JSON));

        String content = callWithTool(
            chatModel, StructuredOutputValidationAdvisor.builder()
                .outputJsonSchema(JSON_SCHEMA)
                .build());

        assertEquals(VALID_JSON, content);
        assertEquals(5, chatModel.prompts.size());
    }

    @Test
    void testOrderRunsBeforeCodeFenceStripping() {
        assertTrue(
            new JsonSchemaValidationAdvisor(JSON_SCHEMA).getOrder() < new CodeFenceStrippingAdvisor().getOrder());
    }

    @Test
    void testNegativeMaxRepeatAttemptsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new JsonSchemaValidationAdvisor(JSON_SCHEMA, -1));
    }

    private static String call(ChatModel chatModel, Advisor validationAdvisor) {
        return ChatClient.create(chatModel)
            .prompt()
            .user("List the items")
            .advisors(validationAdvisor)
            .call()
            .content();
    }

    private static String callWithTool(ChatModel chatModel, Advisor validationAdvisor) {
        return ChatClient.create(chatModel)
            .prompt()
            .user("List the items")
            .advisors(
                ToolCallingAdvisor.builder()
                    .build(),
                validationAdvisor)
            .toolCallbacks(new ListItemsToolCallback())
            .call()
            .content();
    }

    private static ChatResponse textReply(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    private static ChatResponse toolCallReply() {
        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(new AssistantMessage.ToolCall("call_1", "function", TOOL_NAME, "{}")))
            .build();

        return new ChatResponse(List.of(new Generation(assistantMessage)));
    }

    /**
     * Returns the scripted replies in order, repeating the last one once the script runs out.
     */
    private static final class ScriptedChatModel implements ChatModel {

        private final List<Prompt> prompts = new ArrayList<>();
        private final Deque<ChatResponse> replies;

        private ScriptedChatModel(ChatResponse... replies) {
            this.replies = new ConcurrentLinkedDeque<>(List.of(replies));
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);

            return replies.size() > 1 ? replies.poll() : replies.peek();
        }

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder()
                .build();
        }
    }

    private static final class ListItemsToolCallback implements ToolCallback {

        @Override
        public ToolDefinition getToolDefinition() {
            return DefaultToolDefinition.builder()
                .name(TOOL_NAME)
                .description("Lists the items")
                .inputSchema("{\"type\":\"object\",\"properties\":{}}")
                .build();
        }

        @Override
        public String call(String toolInput) {
            return "[\"a\",\"b\"]";
        }
    }
}
