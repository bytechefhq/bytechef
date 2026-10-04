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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.ai.tool.ToolSuspension;
import com.bytechef.platform.component.definition.ActionContextAware;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * @author Ivica Cardic
 */
class AgentToolSuspensionTest {

    private static final String HUMAN_ANSWER = "{\"approved\":true}";
    private static final String SUSPENDED_TOOL_RESULT = createSuspendedToolResult();

    @Test
    void testResumeConversationReplacesOnlyTheSuspendedResponse() {
        AgentToolSuspension agentToolSuspension = new AgentToolSuspension(
            ConversationState.from(
                List.of(
                    new SystemMessage("you are a helper"),
                    new UserMessage("please get approval"),
                    toolResponseMessage(
                        new ToolResponseMessage.ToolResponse("call_a", "otherTool", "kept"),
                        new ToolResponseMessage.ToolResponse("call_b", "requestApproval", SUSPENDED_TOOL_RESULT)))),
            "call_b");

        List<Message> messages = agentToolSuspension.resumeConversation(HUMAN_ANSWER);

        assertThat(messages).hasSize(3);
        assertThat(messages.get(0)).isInstanceOf(SystemMessage.class);
        assertThat(messages.get(1)).isInstanceOf(UserMessage.class);

        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) messages.get(2);

        assertThat(toolResponseMessage.getResponses()).containsExactly(
            new ToolResponseMessage.ToolResponse("call_a", "otherTool", "kept"),
            new ToolResponseMessage.ToolResponse("call_b", "requestApproval", HUMAN_ANSWER));
    }

    @Test
    void testResumeConversationKeepsAnEarlierResponseWithTheSameIdThatIsNotSuspended() {
        AgentToolSuspension agentToolSuspension = new AgentToolSuspension(
            ConversationState.from(
                List.of(
                    toolResponseMessage(new ToolResponseMessage.ToolResponse("call_a", "requestApproval", "earlier")),
                    toolResponseMessage(
                        new ToolResponseMessage.ToolResponse("call_a", "requestApproval", SUSPENDED_TOOL_RESULT)))),
            "call_a");

        List<Message> messages = agentToolSuspension.resumeConversation(HUMAN_ANSWER);

        assertThat(((ToolResponseMessage) messages.get(0)).getResponses()).containsExactly(
            new ToolResponseMessage.ToolResponse("call_a", "requestApproval", "earlier"));
        assertThat(((ToolResponseMessage) messages.get(1)).getResponses()).containsExactly(
            new ToolResponseMessage.ToolResponse("call_a", "requestApproval", HUMAN_ANSWER));
    }

    @Test
    void testResumeConversationPatchesTheSuspendedResponseWhenEveryToolCallHasAnEmptyId() {
        AgentToolSuspension agentToolSuspension = new AgentToolSuspension(
            ConversationState.from(
                List.of(
                    AssistantMessage.builder()
                        .content("")
                        .toolCalls(
                            List.of(
                                new AssistantMessage.ToolCall("", "function", "lookUpCustomer", "{}"),
                                new AssistantMessage.ToolCall("", "function", "requestApproval", "{}"),
                                new AssistantMessage.ToolCall("", "function", "sendEmail", "{}")))
                        .build(),
                    toolResponseMessage(
                        new ToolResponseMessage.ToolResponse("", "lookUpCustomer", "{\"name\":\"Ann\"}"),
                        new ToolResponseMessage.ToolResponse("", "requestApproval", SUSPENDED_TOOL_RESULT),
                        new ToolResponseMessage.ToolResponse("", "sendEmail", "sent")))),
            "");

        List<Message> messages = agentToolSuspension.resumeConversation(HUMAN_ANSWER);

        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) messages.get(1);

        assertThat(toolResponseMessage.getResponses()).containsExactly(
            new ToolResponseMessage.ToolResponse("", "lookUpCustomer", "{\"name\":\"Ann\"}"),
            new ToolResponseMessage.ToolResponse("", "requestApproval", HUMAN_ANSWER),
            new ToolResponseMessage.ToolResponse("", "sendEmail", "sent"));
    }

    @Test
    void testResumeConversationThrowsWhenNoResponseMatches() {
        AgentToolSuspension agentToolSuspension = new AgentToolSuspension(
            ConversationState.from(
                List.of(
                    new SystemMessage("you are a helper"),
                    toolResponseMessage(
                        new ToolResponseMessage.ToolResponse("call_a", "otherTool", "kept"),
                        new ToolResponseMessage.ToolResponse("call_b", "requestApproval", SUSPENDED_TOOL_RESULT)))),
            "missing_id");

        assertThatThrownBy(() -> agentToolSuspension.resumeConversation(HUMAN_ANSWER))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No suspended tool response with id 'missing_id'");
    }

    @Test
    void testResumeConversationThrowsWhenTheMatchingResponseIsNotSuspended() {
        AgentToolSuspension agentToolSuspension = new AgentToolSuspension(
            ConversationState.from(
                List.of(toolResponseMessage(new ToolResponseMessage.ToolResponse("call_a", "otherTool", "kept")))),
            "call_a");

        assertThatThrownBy(() -> agentToolSuspension.resumeConversation(HUMAN_ANSWER))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No suspended tool response with id 'call_a'");
    }

    @Test
    void testResumeConversationThrowsWhenMoreThanOneResponseMatches() {
        AgentToolSuspension agentToolSuspension = new AgentToolSuspension(
            ConversationState.from(
                List.of(
                    toolResponseMessage(
                        new ToolResponseMessage.ToolResponse("", "requestApproval", SUSPENDED_TOOL_RESULT),
                        new ToolResponseMessage.ToolResponse("", "askUserQuestion", SUSPENDED_TOOL_RESULT)))),
            "");

        assertThatThrownBy(() -> agentToolSuspension.resumeConversation(HUMAN_ANSWER))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Found 2 suspended tool responses with id ''");
    }

    private static ToolResponseMessage toolResponseMessage(ToolResponseMessage.ToolResponse... toolResponses) {
        return ToolResponseMessage.builder()
            .responses(List.of(toolResponses))
            .build();
    }

    private static String createSuspendedToolResult() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getSuspend()).thenReturn(new ActionContext.Suspend(Map.of(), Instant.now()));

        return ToolSuspension.suspendedToolResult(actionContext);
    }
}
