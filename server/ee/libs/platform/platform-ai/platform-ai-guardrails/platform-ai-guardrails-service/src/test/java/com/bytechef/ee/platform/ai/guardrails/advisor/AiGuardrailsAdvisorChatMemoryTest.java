/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsScope;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsTarget;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings;
import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService;
import com.bytechef.platform.ai.guardrails.ChatMemoryRedactingAdvisor;
import com.bytechef.platform.ai.guardrails.GuardrailSurface;
import com.bytechef.platform.ai.guardrails.RestorationDestination;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiGuardrailsAdvisorChatMemoryTest {

    private static final String CONVERSATION_ID = "conversation-1";
    private static final String EMAIL = "grace@example.com";
    private static final String MODEL_REPLY = "Write to " + EMAIL + " for details.";
    private static final Long WORKSPACE_ID = 42L;

    private final AiGuardrailsWorkspaceSettingsService settingsService =
        mock(AiGuardrailsWorkspaceSettingsService.class);
    private final AiGuardrailMetrics metrics = mock(AiGuardrailMetrics.class);

    @Test
    void testMemoryStoresTheRedactedReplyWhenPiiRedactionIsOn() {
        ChatMemory chatMemory = newChatMemory();

        String content = callAgent(true, chatMemory);

        assertThat(content).doesNotContain(EMAIL);
        assertThat(storedAssistantText(chatMemory))
            .doesNotContain(EMAIL)
            .contains("[REDACTED_");
    }

    @Test
    void testMemoryStoresTheRawReplyWhenPiiRedactionIsOff() {
        ChatMemory chatMemory = newChatMemory();

        callAgent(false, chatMemory);

        assertThat(storedAssistantText(chatMemory)).isEqualTo(MODEL_REPLY);
    }

    @Test
    void testStreamedMemoryStoresTheRedactedReplyWhenPiiRedactionIsOn() {
        ChatMemory chatMemory = newChatMemory();

        streamAgent(true, chatMemory);

        assertThat(storedAssistantText(chatMemory))
            .doesNotContain(EMAIL)
            .contains("[REDACTED_");
    }

    @Test
    void testStreamedMemoryStoresTheRawReplyWhenPiiRedactionIsOff() {
        ChatMemory chatMemory = newChatMemory();

        streamAgent(false, chatMemory);

        assertThat(storedAssistantText(chatMemory)).isEqualTo(MODEL_REPLY);
    }

    private @Nullable String callAgent(boolean redactPii, ChatMemory chatMemory) {
        ChatModel chatModel = mock(ChatModel.class);

        when(chatModel.getOptions()).thenReturn(ChatOptions.builder()
            .build());
        when(chatModel.call(any(Prompt.class)))
            .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(MODEL_REPLY)))));

        return newRequestSpec(redactPii, chatMemory, chatModel)
            .call()
            .content();
    }

    private void streamAgent(boolean redactPii, ChatMemory chatMemory) {
        ChatModel chatModel = mock(ChatModel.class);

        when(chatModel.getOptions()).thenReturn(ChatOptions.builder()
            .build());
        when(chatModel.stream(any(Prompt.class)))
            .thenReturn(Flux.just(
                new ChatResponse(List.of(new Generation(new AssistantMessage("Write to grace@exa")))),
                new ChatResponse(List.of(new Generation(new AssistantMessage("mple.com for details."))))));

        newRequestSpec(redactPii, chatMemory, chatModel)
            .stream()
            .content()
            .collectList()
            .block();
    }

    private ChatClient.ChatClientRequestSpec newRequestSpec(
        boolean redactPii, ChatMemory chatMemory, ChatModel chatModel) {

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.of(new AiGuardrailsWorkspaceSettings(
            AiGuardrailsSettingsScope.WORKSPACE, WORKSPACE_ID, redactPii, null, null, null, null, true, null, null,
            null, false)));

        AiGuardrails aiGuardrails = new AiGuardrails(
            settingsService, null, null, null, false, false, "", false, false, false, false);

        AiGuardrailsSettingsTarget target = AiGuardrailsSettingsTarget.workspace(WORKSPACE_ID);

        AiGuardrailsAdvisor aiGuardrailsAdvisor = new AiGuardrailsAdvisor(
            aiGuardrails, target, metrics, GuardrailSurface.AI_AGENT, RestorationDestination.WORKFLOW_OUTPUT);

        BaseAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
            .build();

        UnaryOperator<String> chatMemoryRedactor = aiGuardrails.newChatMemoryRedactor(target);

        if (chatMemoryRedactor != null) {
            memoryAdvisor = new ChatMemoryRedactingAdvisor(memoryAdvisor, chatMemoryRedactor);
        }

        ChatClient chatClient = ChatClient.builder(chatModel)
            .build();

        return chatClient.prompt()
            .user("Who should I contact?")
            .advisors(aiGuardrailsAdvisor, memoryAdvisor)
            .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, CONVERSATION_ID));
    }

    private static ChatMemory newChatMemory() {
        return MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .build();
    }

    private static String storedAssistantText(ChatMemory chatMemory) {
        List<Message> assistantMessages = chatMemory.get(CONVERSATION_ID)
            .stream()
            .filter(message -> message.getMessageType() == MessageType.ASSISTANT)
            .toList();

        assertThat(assistantMessages).hasSize(1);

        Message assistantMessage = assistantMessages.getFirst();

        return assistantMessage.getText();
    }
}
