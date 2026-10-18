/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Pin the failure-mode coverage for {@link NonEmptyMessagesAdvisor}. Every test names the wire-level Anthropic 400 it
 * prevents and which Spring AI bug surfaces the problem.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class NonEmptyMessagesAdvisorTest {

    @Test
    void testPassesThroughWhenAllMessagesAreNonEmpty() {
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        Prompt prompt = new Prompt(List.of(
            new UserMessage("Hello"),
            new AssistantMessage("Hi there"),
            new UserMessage("How are you?")));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        ChatClientResponse response = dispatch(advisor, request);

        assertThat(response).isNotNull();
    }

    @Test
    void testSubstitutesPlaceholderForEmptyUserMessage() {
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        Prompt prompt = new Prompt(List.of(new UserMessage("Hello"), new AssistantMessage("Hi"), new UserMessage("")));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        List<Message> patched = capturePatchedMessages(advisor, request);

        assertThat(patched).hasSize(3);
        assertThat(patched.get(2)
            .getText()).isEqualTo("no content");
    }

    @Test
    void testStripsToolResponseMessageWithEmptyResponsesList() {
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        ToolResponseMessage staleTool = ToolResponseMessage.builder()
            .responses(List.of())
            .build();

        Prompt prompt = new Prompt(List.of(
            new UserMessage("Previous prompt"),
            new AssistantMessage("Previous reply"),
            staleTool,
            new UserMessage("Current prompt")));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        List<Message> patched = capturePatchedMessages(advisor, request);

        assertThat(patched).hasSize(3);
        assertThat(patched.get(0)
            .getText()).isEqualTo("Previous prompt");
        assertThat(patched.get(2)
            .getText()).isEqualTo("Current prompt");
    }

    @Test
    void testStripsAssistantMessageWithEmptyTextAndNoToolCalls() {
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        Prompt prompt = new Prompt(List.of(
            new UserMessage("Prev"),
            new AssistantMessage(""),
            new UserMessage("Current")));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        List<Message> patched = capturePatchedMessages(advisor, request);

        assertThat(patched).hasSize(2);
        assertThat(patched.get(0)
            .getText()).isEqualTo("Prev");
        assertThat(patched.get(1)
            .getText()).isEqualTo("Current");
    }

    @Test
    void testKeepsAssistantWithEmptyTextButPresentToolCalls() {
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        AssistantMessage currentTurn = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(new ToolCall("call-1", "function", "searchComponents", "{}")))
            .build();

        Prompt prompt = new Prompt(List.of(new UserMessage("Find Slack"), currentTurn));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        List<Message> patched = capturePatchedMessages(advisor, request);

        assertThat(patched).hasSize(2);
        assertThat(patched.get(1)).isSameAs(currentTurn);
    }

    @Test
    void testStripsToolResponseMessageWithAllBlankResponseData() {
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        ToolResponseMessage staleTool = ToolResponseMessage.builder()
            .responses(List.of(
                new ToolResponseMessage.ToolResponse("call-1", "searchComponents", ""),
                new ToolResponseMessage.ToolResponse("call-2", "listConnections", "   ")))
            .build();

        Prompt prompt = new Prompt(List.of(new UserMessage("Q"), new AssistantMessage("A"), staleTool));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        List<Message> patched = capturePatchedMessages(advisor, request);

        assertThat(patched).hasSize(2);
    }

    @Test
    void testPatchesOnlyEmptyResponseDataWhenMixed() {
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        ToolResponseMessage toolMessage = ToolResponseMessage.builder()
            .responses(List.of(
                new ToolResponseMessage.ToolResponse("call-1", "good", "{\"ok\":true}"),
                new ToolResponseMessage.ToolResponse("call-2", "bad", ""),
                new ToolResponseMessage.ToolResponse("call-3", "alsoGood", "{\"ok\":true}")))
            .build();

        Prompt prompt = new Prompt(List.of(new UserMessage("Q"), new AssistantMessage("A"), toolMessage));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        List<Message> patched = capturePatchedMessages(advisor, request);

        assertThat(patched).hasSize(3);

        ToolResponseMessage finalToolMessage = (ToolResponseMessage) patched.get(2);

        assertThat(finalToolMessage.getResponses()).hasSize(3);
        assertThat(finalToolMessage.getResponses()
            .get(0)
            .responseData()).isEqualTo("{\"ok\":true}");
        assertThat(finalToolMessage.getResponses()
            .get(1)
            .responseData()).isEqualTo("{\"result\":\"no content\"}");
        assertThat(finalToolMessage.getResponses()
            .get(2)
            .responseData()).isEqualTo("{\"ok\":true}");
    }

    @Test
    void testStripsAdjacentStaleAssistantAndToolPairsCompactingHistory() {
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        Prompt prompt = new Prompt(List.of(
            new UserMessage("Schedule a daily message"),
            new AssistantMessage(""),
            ToolResponseMessage.builder()
                .responses(List.of())
                .build(),
            new AssistantMessage("I need a couple more details before building this workflow."),
            ToolResponseMessage.builder()
                .responses(List.of())
                .build(),
            new UserMessage("Use #engineering")));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        List<Message> patched = capturePatchedMessages(advisor, request);

        assertThat(patched).hasSize(3);
        assertThat(patched.get(0)
            .getText()).isEqualTo("Schedule a daily message");
        assertThat(patched.get(1)
            .getText()).contains("I need a couple more details");
        assertThat(patched.get(2)
            .getText()).isEqualTo("Use #engineering");
    }

    @Test
    void testWhitespaceOnlyUserMessageIsTreatedAsEmpty() {
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        Prompt prompt = new Prompt(List.of(new UserMessage("  \n\t  ")));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        List<Message> patched = capturePatchedMessages(advisor, request);

        assertThat(patched).hasSize(1);
        assertThat(patched.get(0)
            .getText()).isEqualTo("no content");
    }

    @Test
    void testStripsDanglingAssistantToolCallFollowedByUserMessage() {
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        AssistantMessage danglingToolUse = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(new ToolCall("call-1", "function", "listConnectionsForComponent", "{}")))
            .build();

        Prompt prompt = new Prompt(List.of(
            new UserMessage("Use OpenAI"),
            danglingToolUse,
            new UserMessage("try again")));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        List<Message> patched = capturePatchedMessages(advisor, request);

        assertThat(patched).hasSize(2);
        assertThat(patched.get(0)
            .getText()).isEqualTo("Use OpenAI");
        assertThat(patched.get(1)
            .getText()).isEqualTo("try again");
    }

    @Test
    void testKeepsAnsweredToolCallButStripsUnansweredSibling() {
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(
                new ToolCall("call-1", "function", "answered", "{}"),
                new ToolCall("call-2", "function", "unanswered", "{}")))
            .build();

        ToolResponseMessage toolResponseMessage = ToolResponseMessage.builder()
            .responses(List.of(new ToolResponseMessage.ToolResponse("call-1", "answered", "{\"ok\":true}")))
            .build();

        Prompt prompt = new Prompt(List.of(
            new UserMessage("Q"), assistantMessage, toolResponseMessage, new UserMessage("Next")));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        List<Message> patched = capturePatchedMessages(advisor, request);

        assertThat(patched).hasSize(4);

        AssistantMessage patchedAssistant = (AssistantMessage) patched.get(1);

        assertThat(patchedAssistant.getToolCalls()).hasSize(1);
        assertThat(patchedAssistant.getToolCalls()
            .get(0)
            .id()).isEqualTo("call-1");
    }

    @Test
    void testStripsAssistantToolUseOrphanedByEmptyToolResultRemoval() {
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(new ToolCall("call-1", "function", "searchComponents", "{}")))
            .build();

        Prompt prompt = new Prompt(List.of(
            new UserMessage("Find Slack"),
            assistantMessage,
            ToolResponseMessage.builder()
                .responses(List.of())
                .build(),
            new UserMessage("Anything?")));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        List<Message> patched = capturePatchedMessages(advisor, request);

        assertThat(patched).hasSize(2);
        assertThat(patched.get(0)
            .getText()).isEqualTo("Find Slack");
        assertThat(patched.get(1)
            .getText()).isEqualTo("Anything?");
    }

    private static ChatClientResponse dispatch(NonEmptyMessagesAdvisor advisor, ChatClientRequest request) {
        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientResponse response = mock(ChatClientResponse.class);

        when(chain.nextCall(any())).thenReturn(response);

        return advisor.adviseCall(request, chain);
    }

    private static List<Message> capturePatchedMessages(NonEmptyMessagesAdvisor advisor, ChatClientRequest request) {
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(mock(ChatClientResponse.class));

        advisor.adviseCall(request, chain);

        ArgumentCaptor<ChatClientRequest> captor = ArgumentCaptor.forClass(ChatClientRequest.class);

        verify(chain).nextCall(captor.capture());

        return captor.getValue()
            .prompt()
            .getInstructions();
    }
}
