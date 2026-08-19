/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.chat.AiHubChatService.AiHubChatMessage;
import com.bytechef.ee.ai.hub.exception.TitleGenerationFailedException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Unit tests for {@link TitleGenerationService}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class TitleGenerationServiceTest {

    @Mock
    private ChatModel chatModel;

    @InjectMocks
    private TitleGenerationService titleGenerationService;

    @Test
    void testGenerateTitleReturnsTrimmedResponse() {
        stubChatModel("  Create Workflow Automation  ");

        List<AiHubChatMessage> messages = List.of(
            new AiHubChatMessage("USER", "How do I create a workflow?", Instant.now()),
            new AiHubChatMessage("ASSISTANT", "You can create a workflow by...", Instant.now()));

        String title = titleGenerationService.generateTitle(messages);

        assertThat(title).isEqualTo("Create Workflow Automation");
    }

    @Test
    void testGenerateTitleReturnsEmptyWhenResponseIsBlank() {
        stubChatModel("   ");

        List<AiHubChatMessage> messages = List.of(
            new AiHubChatMessage("USER", "Hello", Instant.now()));

        String title = titleGenerationService.generateTitle(messages);

        assertThat(title).isEmpty();
    }

    @Test
    void testGenerateTitleTruncatesOverlyLongResponses() {
        String longResponse = "This is a very long title that exceeds the sixty character limit set by the service";

        assertThat(longResponse.length()).isGreaterThan(60);

        stubChatModel(longResponse);

        List<AiHubChatMessage> messages = List.of(
            new AiHubChatMessage("USER", "Tell me something", Instant.now()),
            new AiHubChatMessage("ASSISTANT", "Sure, here is something", Instant.now()));

        String title = titleGenerationService.generateTitle(messages);

        assertThat(title).isEmpty();
    }

    @Test
    void testGenerateTitleReturnsEmptyForEmptyMessageList() {
        String title = titleGenerationService.generateTitle(List.of());

        assertThat(title).isEmpty();
    }

    @Test
    void testGenerateTitleThrowsWhenChatModelFails() {
        RuntimeException upstream = new RuntimeException("upstream model 503");

        when(chatModel.call(any(Prompt.class))).thenThrow(upstream);

        List<AiHubChatMessage> messages = List.of(
            new AiHubChatMessage("USER", "Hello", Instant.now()));

        // Pin: ChatModel failure must propagate as TitleGenerationFailedException so the controller can return
        // 503 instead of silently leaving the chat labelled "Untitled".
        assertThatThrownBy(() -> titleGenerationService.generateTitle(messages))
            .isInstanceOf(TitleGenerationFailedException.class)
            .hasCauseReference(upstream);
    }

    private void stubChatModel(String responseText) {
        AssistantMessage assistantMessage = new AssistantMessage(responseText);
        Generation generation = new Generation(assistantMessage);
        ChatResponse chatResponse = new ChatResponse(List.of(generation));

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
    }
}
