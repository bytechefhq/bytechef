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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmptyToolFollowUpAdvisorTest {

    private final EmptyToolFollowUpAdvisor advisor = new EmptyToolFollowUpAdvisor();

    @Test
    void testAnEmptyReplyToAToolResultIsRetriedOnceWithAContinuePrompt() {
        CallAdvisorChain callAdvisorChain = callAdvisorChain(response(""), response("Rolled back."));

        ChatClientResponse chatClientResponse = advisor.adviseCall(afterToolResult(), callAdvisorChain);

        assertThat(text(chatClientResponse)).isEqualTo("Rolled back.");

        List<ChatClientRequest> requests = capturedCallRequests(callAdvisorChain, 2);

        assertThat(requests.get(0)
            .prompt()
            .getInstructions()).hasSize(3);
        assertThat(lastMessage(requests.get(1))).isInstanceOf(UserMessage.class);
        assertThat(lastMessage(requests.get(1)).getText()).isEqualTo(EmptyToolFollowUpAdvisor.CONTINUE_PROMPT);
    }

    @Test
    void testAReplyWithTextIsNotRetried() {
        CallAdvisorChain callAdvisorChain = callAdvisorChain(response("Done."));

        ChatClientResponse chatClientResponse = advisor.adviseCall(afterToolResult(), callAdvisorChain);

        assertThat(text(chatClientResponse)).isEqualTo("Done.");

        capturedCallRequests(callAdvisorChain, 1);
    }

    @Test
    void testAReplyWithAToolCallIsNotRetried() {
        CallAdvisorChain callAdvisorChain = callAdvisorChain(toolCallResponse());

        advisor.adviseCall(afterToolResult(), callAdvisorChain);

        capturedCallRequests(callAdvisorChain, 1);
    }

    @Test
    void testAnEmptyReplyToAUserMessageIsNotRetried() {
        CallAdvisorChain callAdvisorChain = callAdvisorChain(response(""));

        ChatClientRequest chatClientRequest = ChatClientRequest.builder()
            .prompt(new Prompt(List.of(new UserMessage("Roll that deployment back."))))
            .build();

        advisor.adviseCall(chatClientRequest, callAdvisorChain);

        capturedCallRequests(callAdvisorChain, 1);
    }

    @Test
    void testASecondEmptyReplyIsReturnedWithoutAnotherRetry() {
        CallAdvisorChain callAdvisorChain = callAdvisorChain(response(""), response(""));

        ChatClientResponse chatClientResponse = advisor.adviseCall(afterToolResult(), callAdvisorChain);

        assertThat(text(chatClientResponse)).isEmpty();

        capturedCallRequests(callAdvisorChain, 2);
    }

    @Test
    void testAStreamedEmptyReplyToAToolResultStreamsTheRetry() {
        StreamAdvisorChain streamAdvisorChain = mock(StreamAdvisorChain.class);

        when(streamAdvisorChain.copy(advisor)).thenReturn(streamAdvisorChain);
        when(streamAdvisorChain.nextStream(any(ChatClientRequest.class)))
            .thenReturn(Flux.just(response("")), Flux.just(response("Rolled "), response("back.")));

        List<ChatClientResponse> chatClientResponses = advisor.adviseStream(afterToolResult(), streamAdvisorChain)
            .collectList()
            .block();

        assertThat(chatClientResponses).extracting(EmptyToolFollowUpAdvisorTest::text)
            .containsExactly("", "Rolled ", "back.");

        ArgumentCaptor<ChatClientRequest> requestCaptor = ArgumentCaptor.forClass(ChatClientRequest.class);

        verify(streamAdvisorChain, times(2)).nextStream(requestCaptor.capture());

        List<ChatClientRequest> requests = requestCaptor.getAllValues();

        assertThat(lastMessage(requests.get(1)).getText()).isEqualTo(EmptyToolFollowUpAdvisor.CONTINUE_PROMPT);
    }

    @Test
    void testAStreamedReplyWithTextIsNotRetried() {
        StreamAdvisorChain streamAdvisorChain = mock(StreamAdvisorChain.class);

        when(streamAdvisorChain.copy(advisor)).thenReturn(streamAdvisorChain);
        when(streamAdvisorChain.nextStream(any(ChatClientRequest.class)))
            .thenReturn(Flux.just(response(""), response("Done.")));

        List<ChatClientResponse> chatClientResponses = advisor.adviseStream(afterToolResult(), streamAdvisorChain)
            .collectList()
            .block();

        assertThat(chatClientResponses).extracting(EmptyToolFollowUpAdvisorTest::text)
            .containsExactly("", "Done.");

        verify(streamAdvisorChain, times(1)).nextStream(any(ChatClientRequest.class));
    }

    private CallAdvisorChain callAdvisorChain(ChatClientResponse first, ChatClientResponse... rest) {
        CallAdvisorChain callAdvisorChain = mock(CallAdvisorChain.class);

        when(callAdvisorChain.copy(advisor)).thenReturn(callAdvisorChain);
        when(callAdvisorChain.nextCall(any(ChatClientRequest.class))).thenReturn(first, rest);

        return callAdvisorChain;
    }

    private static List<ChatClientRequest> capturedCallRequests(CallAdvisorChain callAdvisorChain, int count) {
        ArgumentCaptor<ChatClientRequest> requestCaptor = ArgumentCaptor.forClass(ChatClientRequest.class);

        verify(callAdvisorChain, times(count)).nextCall(requestCaptor.capture());

        return requestCaptor.getAllValues();
    }

    private static ChatClientRequest afterToolResult() {
        AssistantMessage toolCallMessage = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(new ToolCall("toolu_1", "function", "toolSearchTool", "{\"query\":\"rollback\"}")))
            .build();

        ToolResponseMessage toolResponseMessage = ToolResponseMessage.builder()
            .responses(List.of(new ToolResponse("toolu_1", "toolSearchTool", "[\"rollbackProjectDeployment\"]")))
            .build();

        return ChatClientRequest.builder()
            .prompt(new Prompt(List.of(new UserMessage("Roll that deployment back."), toolCallMessage,
                toolResponseMessage)))
            .build();
    }

    private static ChatClientResponse response(String text) {
        return ChatClientResponse.builder()
            .chatResponse(new ChatResponse(List.of(new Generation(new AssistantMessage(text)))))
            .build();
    }

    private static ChatClientResponse toolCallResponse() {
        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(new ToolCall("toolu_2", "function", "rollbackProjectDeployment", "{}")))
            .build();

        return ChatClientResponse.builder()
            .chatResponse(new ChatResponse(List.of(new Generation(assistantMessage))))
            .build();
    }

    private static Message lastMessage(ChatClientRequest chatClientRequest) {
        Prompt prompt = chatClientRequest.prompt();

        List<Message> instructions = prompt.getInstructions();

        return instructions.getLast();
    }

    private static String text(ChatClientResponse chatClientResponse) {
        ChatResponse chatResponse = Objects.requireNonNull(chatClientResponse.chatResponse());

        Generation generation = Objects.requireNonNull(chatResponse.getResult());

        AssistantMessage assistantMessage = generation.getOutput();

        return assistantMessage.getText();
    }
}
