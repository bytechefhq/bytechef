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

package com.bytechef.component.ai.agent.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

class ConversationStateTest {

    @Test
    void testRoundTripPreservesAllMessageKinds() {
        List<Message> original = List.of(
            new SystemMessage("you are an agent"),
            new UserMessage("please get approval"),
            AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall("call_1", "function", "requestApproval", "{}")))
                .build(),
            ToolResponseMessage.builder()
                .responses(List.of(
                    new ToolResponseMessage.ToolResponse("call_1", "requestApproval", "__bytechef_tool_suspended__")))
                .build());

        List<Message> restored = ConversationState.from(original)
            .toMessages();

        assertEquals(4, restored.size());
        assertInstanceOf(SystemMessage.class, restored.get(0));
        assertInstanceOf(UserMessage.class, restored.get(1));

        AssistantMessage assistantMessage = assertInstanceOf(AssistantMessage.class, restored.get(2));

        assertEquals("call_1", assistantMessage.getToolCalls()
            .get(0)
            .id());

        ToolResponseMessage toolResponseMessage = assertInstanceOf(ToolResponseMessage.class, restored.get(3));

        assertEquals(
            "__bytechef_tool_suspended__",
            toolResponseMessage.getResponses()
                .get(0)
                .responseData());
    }

    @Test
    void testSurvivesJacksonRoundTrip() throws Exception {
        ConversationState state = ConversationState.from(
            List.of(
                new UserMessage("hello"),
                ToolResponseMessage.builder()
                    .responses(List.of(new ToolResponseMessage.ToolResponse("call_1", "tool", "result")))
                    .build()));

        tools.jackson.databind.ObjectMapper objectMapper = new tools.jackson.databind.ObjectMapper();

        String json = objectMapper.writeValueAsString(state);
        ConversationState restored = objectMapper.readValue(json, ConversationState.class);

        assertEquals(state, restored);
    }
}
