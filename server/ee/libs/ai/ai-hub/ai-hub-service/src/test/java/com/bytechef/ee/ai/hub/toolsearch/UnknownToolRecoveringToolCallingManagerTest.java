/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;

/**
 * Pins the unknown-tool recovery contract: a model call to a tool name the delegate cannot resolve must produce a
 * recoverable tool-error round (so the model self-corrects on the next iteration) instead of propagating the
 * {@code IllegalStateException} that kills the whole streaming turn.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class UnknownToolRecoveringToolCallingManagerTest {

    private final ToolCallingManager delegate = mock(ToolCallingManager.class);

    private final UnknownToolRecoveringToolCallingManager toolCallingManager =
        new UnknownToolRecoveringToolCallingManager(delegate);

    @Test
    void testUnknownToolNameProducesToolErrorRoundInsteadOfThrowing() {
        when(delegate.executeToolCalls(any(), any())).thenThrow(
            new IllegalStateException("No ToolCallback found for tool name: research"));

        UserMessage userMessage = new UserMessage("research my competitors");

        Prompt prompt = new Prompt(List.of(userMessage));

        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(new AssistantMessage.ToolCall("call_1", "function", "research", "{\"topic\":\"x\"}")))
            .build();

        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(assistantMessage)))
            .build();

        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, chatResponse);

        List<Message> conversationHistory = toolExecutionResult.conversationHistory();

        assertThat(conversationHistory).containsSubsequence(userMessage, assistantMessage);

        Message lastMessage = conversationHistory.getLast();

        assertThat(lastMessage).isInstanceOf(ToolResponseMessage.class);

        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) lastMessage;

        List<ToolResponseMessage.ToolResponse> toolResponses = toolResponseMessage.getResponses();

        assertThat(toolResponses).hasSize(1);
        assertThat(toolResponses.getFirst()
            .id()).isEqualTo("call_1");
        assertThat(toolResponses.getFirst()
            .name()).isEqualTo("research");
        assertThat(toolResponses.getFirst()
            .responseData()).contains("No ToolCallback found for tool name: research");
    }

    @Test
    void testEveryToolCallOfTheRoundGetsAnErrorResponse() {
        when(delegate.executeToolCalls(any(), any())).thenThrow(
            new IllegalStateException("No ToolCallback found for tool name: research"));

        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content("")
            .toolCalls(
                List.of(
                    new AssistantMessage.ToolCall("call_1", "function", "research", "{}"),
                    new AssistantMessage.ToolCall("call_2", "function", "listDataTables", "{}")))
            .build();

        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(assistantMessage)))
            .build();

        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(
            new Prompt(List.of(new UserMessage("hi"))), chatResponse);

        Message lastMessage = toolExecutionResult.conversationHistory()
            .getLast();

        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) lastMessage;

        assertThat(toolResponseMessage.getResponses()).extracting(ToolResponseMessage.ToolResponse::id)
            .containsExactly("call_1", "call_2");
    }

    @Test
    void testOtherIllegalStateExceptionsPropagate() {
        when(delegate.executeToolCalls(any(), any())).thenThrow(new IllegalStateException("something else broke"));

        Prompt prompt = new Prompt(List.of(new UserMessage("hi")));

        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(AssistantMessage.builder()
                .content("")
                .build())))
            .build();

        assertThatThrownBy(() -> toolCallingManager.executeToolCalls(prompt, chatResponse))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("something else broke");
    }

    @Test
    void testSuccessfulExecutionPassesThrough() {
        ToolExecutionResult delegateResult = ToolExecutionResult.builder()
            .conversationHistory(List.of(new UserMessage("hi")))
            .build();

        when(delegate.executeToolCalls(any(), any())).thenReturn(delegateResult);

        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(AssistantMessage.builder()
                .content("")
                .build())))
            .build();

        assertThat(toolCallingManager.executeToolCalls(new Prompt(List.of(new UserMessage("hi"))), chatResponse))
            .isSameAs(delegateResult);
    }

    @Test
    void testToolResponsesAnswerTheGenerationThatCarriesTheToolCalls() {
        when(delegate.executeToolCalls(any(), any())).thenThrow(
            new IllegalStateException("No ToolCallback found for tool name: research"));

        AssistantMessage plainMessage = AssistantMessage.builder()
            .content("no tools here")
            .build();

        AssistantMessage toolCallMessage = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(new AssistantMessage.ToolCall("call_9", "function", "research", "{}")))
            .build();

        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(plainMessage), new Generation(toolCallMessage)))
            .build();

        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(
            new Prompt(List.of(new UserMessage("hi"))), chatResponse);

        List<Message> conversationHistory = toolExecutionResult.conversationHistory();

        assertThat(conversationHistory).contains(toolCallMessage);
        assertThat(conversationHistory).doesNotContain(plainMessage);
    }
}
