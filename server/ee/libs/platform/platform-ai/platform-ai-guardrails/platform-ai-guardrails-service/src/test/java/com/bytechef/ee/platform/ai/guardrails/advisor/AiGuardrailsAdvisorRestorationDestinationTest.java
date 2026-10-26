/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsScope;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsTarget;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings;
import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService;
import com.bytechef.platform.ai.guardrails.GuardrailSurface;
import com.bytechef.platform.ai.guardrails.RestorationDestination;
import com.bytechef.platform.ai.sensitivedata.tokenization.SensitiveDataPolicy;
import com.bytechef.platform.ai.sensitivedata.tokenization.SensitiveDataPolicyToolContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class AiGuardrailsAdvisorRestorationDestinationTest {

    private static final Long WORKSPACE_ID = 42L;
    private static final String EMAIL = "ada@example.com";
    private static final String TOKEN_PREFIX = "[PII_EMAIL_ADDRESS_1_";

    private final AiGuardrailsWorkspaceSettingsService settingsService =
        mock(AiGuardrailsWorkspaceSettingsService.class);
    private final AiGuardrailMetrics metrics = mock(AiGuardrailMetrics.class);

    @Test
    void testAWorkflowOutputRestoresWhenTheSettingIsOn() {
        String responseText = echoedResponseText(RestorationDestination.WORKFLOW_OUTPUT, true);

        assertThat(responseText).contains(EMAIL);
    }

    @Test
    void testAWorkflowOutputKeepsTokensWhenTheSettingIsOff() {
        String responseText = echoedResponseText(RestorationDestination.WORKFLOW_OUTPUT, false);

        assertThat(responseText).doesNotContain(EMAIL);
        assertThat(responseText).contains(TOKEN_PREFIX);
    }

    @Test
    void testAConversationRestoresEvenWhenTheSettingIsOff() {
        String responseText = echoedResponseText(RestorationDestination.CONVERSATION, false);

        assertThat(responseText).contains(EMAIL);
    }

    @Test
    void testWithholdingRestorationIsRecorded() {
        echoedResponseText(RestorationDestination.WORKFLOW_OUTPUT, false);

        verify(metrics).recordRestoreSuppressed();
    }

    @Test
    void testASuppressedRestoreIsNotRecordedWhenThereWasNothingToRestore() {
        adviseCall(RestorationDestination.WORKFLOW_OUTPUT, false, staticChain("nothing sensitive here"));

        verify(metrics, never()).recordRestoreSuppressed();
    }

    @Test
    void testAStreamingAgentStillGatesItsToolArguments() {
        assertThat(toolContextPolicy().restoreOutboundArguments()).isFalse();
    }

    @Test
    void testAStreamingTailRestoresEvenWhenTheSettingIsOff() {
        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.of(new AiGuardrailsWorkspaceSettings(
            AiGuardrailsSettingsScope.WORKSPACE, WORKSPACE_ID, null, null, null, null, null, null, null, null, null,
            false)));

        AiGuardrails aiGuardrails = new AiGuardrails(
            settingsService, null, null, null, true, false, "", false, false, false, false);

        AiGuardrailsAdvisor advisor = new AiGuardrailsAdvisor(
            aiGuardrails, AiGuardrailsSettingsTarget.workspace(WORKSPACE_ID), metrics, GuardrailSurface.AI_AGENT,
            RestorationDestination.WORKFLOW_OUTPUT);

        ChatClientRequest request = requestWithUserMessage("mail " + EMAIL);
        StreamAdvisorChain chain = mock(StreamAdvisorChain.class);

        when(chain.nextStream(any())).thenAnswer(invocation -> {
            ChatClientRequest forwardedRequest = invocation.getArgument(0);

            return Flux.just(responseChunk(userTextOf(forwardedRequest)));
        });

        List<ChatClientResponse> emitted = Objects.requireNonNull(
            advisor.adviseStream(request, chain)
                .collectList()
                .block(),
            "emitted");

        String combinedText = emitted.stream()
            .map(response -> {
                ChatResponse chatResponse = Objects.requireNonNull(response.chatResponse(), "chatResponse");
                Generation generation = Objects.requireNonNull(chatResponse.getResult(), "generation");

                return generation.getOutput()
                    .getText();
            })
            .collect(Collectors.joining());

        assertThat(combinedText).contains(EMAIL);
    }

    @Test
    void testRestoreSuppressedIsRecordedOnceEvenAcrossMultipleGenerations() {
        adviseCall(RestorationDestination.WORKFLOW_OUTPUT, false, twoGenerationEchoingChain());

        verify(metrics, times(1)).recordRestoreSuppressed();
    }

    @Test
    void testTheRestorationSettingIsReadOncePerCall() {
        AiGuardrails spiedAiGuardrails = spiedAiGuardrails(true);

        AiGuardrailsAdvisor advisor = new AiGuardrailsAdvisor(
            spiedAiGuardrails, AiGuardrailsSettingsTarget.workspace(WORKSPACE_ID), metrics, GuardrailSurface.AI_AGENT,
            RestorationDestination.WORKFLOW_OUTPUT);

        advisor.adviseCall(requestWithToolCallingOptions("mail " + EMAIL), echoingChain());

        verify(spiedAiGuardrails, times(1)).isRestoreIntoWorkflowOutput(any());
    }

    @Test
    void testTheRequestAndResponseHalvesCannotDisagree() {
        AiGuardrails spiedAiGuardrails = spiedAiGuardrails(false);

        doReturn(false, true).when(spiedAiGuardrails)
            .isRestoreIntoWorkflowOutput(any());

        AiGuardrailsAdvisor advisor = new AiGuardrailsAdvisor(
            spiedAiGuardrails, AiGuardrailsSettingsTarget.workspace(WORKSPACE_ID), metrics, GuardrailSurface.AI_AGENT,
            RestorationDestination.WORKFLOW_OUTPUT);

        ChatClientResponse response =
            advisor.adviseCall(requestWithToolCallingOptions("mail " + EMAIL), echoingChain());

        assertThat(textOf(response)).doesNotContain(EMAIL);
    }

    private AiGuardrails spiedAiGuardrails(boolean restoreIntoWorkflowOutput) {
        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.of(new AiGuardrailsWorkspaceSettings(
            AiGuardrailsSettingsScope.WORKSPACE, WORKSPACE_ID, null, null, null, null, null, null, null, null, null,
            restoreIntoWorkflowOutput)));

        AiGuardrails aiGuardrails = new AiGuardrails(
            settingsService, null, null, null, true, false, "", false, false, false, false);

        return spy(aiGuardrails);
    }

    private SensitiveDataPolicy toolContextPolicy() {
        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.of(new AiGuardrailsWorkspaceSettings(
            AiGuardrailsSettingsScope.WORKSPACE, WORKSPACE_ID, null, null, null, null, null, null, null, null, null,
            false)));

        AiGuardrails aiGuardrails = new AiGuardrails(
            settingsService, null, null, null, true, false, "", false, false, false, false);

        AiGuardrailsAdvisor advisor = new AiGuardrailsAdvisor(
            aiGuardrails, AiGuardrailsSettingsTarget.workspace(WORKSPACE_ID), metrics, GuardrailSurface.AI_AGENT,
            RestorationDestination.CONVERSATION);

        ChatClientRequest request = requestWithToolCallingOptions("mail " + EMAIL);
        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ArgumentCaptor<ChatClientRequest> forwardedRequestCaptor = ArgumentCaptor.forClass(ChatClientRequest.class);

        when(chain.nextCall(forwardedRequestCaptor.capture())).thenReturn(ChatClientResponse.builder()
            .context(Map.of())
            .build());

        advisor.adviseCall(request, chain);

        return Objects.requireNonNull(
            SensitiveDataPolicyToolContext.from(capturedToolContext(forwardedRequestCaptor)));
    }

    private static ChatClientRequest requestWithToolCallingOptions(String text) {
        ChatOptions chatOptions = ToolCallingChatOptions.builder()
            .toolContext(Map.of())
            .build();

        return new ChatClientRequest(new Prompt(List.of(new UserMessage(text)), chatOptions), Map.of());
    }

    private static @Nullable ToolContext capturedToolContext(ArgumentCaptor<ChatClientRequest> forwardedRequestCaptor) {
        ChatOptions chatOptions = forwardedRequestCaptor.getValue()
            .prompt()
            .getOptions();

        if (!(chatOptions instanceof ToolCallingChatOptions toolCallingChatOptions)) {
            return null;
        }

        Map<String, Object> toolContext = toolCallingChatOptions.getToolContext();

        return toolContext == null ? null : new ToolContext(toolContext);
    }

    private static CallAdvisorChain twoGenerationEchoingChain() {
        CallAdvisorChain callAdvisorChain = mock(CallAdvisorChain.class);

        when(callAdvisorChain.nextCall(any())).thenAnswer(invocation -> {
            ChatClientRequest chatClientRequest = invocation.getArgument(0);
            String text = userTextOf(chatClientRequest);

            return ChatClientResponse.builder()
                .chatResponse(new ChatResponse(List.of(
                    new Generation(new AssistantMessage(text)),
                    new Generation(new AssistantMessage(text)))))
                .context(chatClientRequest.context())
                .build();
        });

        return callAdvisorChain;
    }

    private String echoedResponseText(RestorationDestination destination, boolean restoreIntoWorkflowOutput) {
        ChatClientResponse response = adviseCall(destination, restoreIntoWorkflowOutput, echoingChain());

        return textOf(response);
    }

    private ChatClientResponse adviseCall(
        RestorationDestination destination, boolean restoreIntoWorkflowOutput, CallAdvisorChain chain) {

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.of(new AiGuardrailsWorkspaceSettings(
            AiGuardrailsSettingsScope.WORKSPACE, WORKSPACE_ID, null, null, null, null, null, null, null, null, null,
            restoreIntoWorkflowOutput)));

        AiGuardrails aiGuardrails = new AiGuardrails(
            settingsService, null, null, null, true, false, "", false, false, false, false);

        AiGuardrailsAdvisor advisor = new AiGuardrailsAdvisor(
            aiGuardrails, AiGuardrailsSettingsTarget.workspace(WORKSPACE_ID), metrics, GuardrailSurface.AI_AGENT,
            destination);

        return advisor.adviseCall(requestWithUserMessage("mail " + EMAIL), chain);
    }

    private static CallAdvisorChain echoingChain() {
        CallAdvisorChain callAdvisorChain = mock(CallAdvisorChain.class);

        when(callAdvisorChain.nextCall(any())).thenAnswer(invocation -> {
            ChatClientRequest chatClientRequest = invocation.getArgument(0);

            return ChatClientResponse.builder()
                .chatResponse(new ChatResponse(List.of(new Generation(new AssistantMessage(
                    userTextOf(chatClientRequest))))))
                .context(chatClientRequest.context())
                .build();
        });

        return callAdvisorChain;
    }

    private static CallAdvisorChain staticChain(String responseText) {
        CallAdvisorChain callAdvisorChain = mock(CallAdvisorChain.class);

        when(callAdvisorChain.nextCall(any())).thenAnswer(invocation -> {
            ChatClientRequest chatClientRequest = invocation.getArgument(0);

            return ChatClientResponse.builder()
                .chatResponse(new ChatResponse(List.of(new Generation(new AssistantMessage(responseText)))))
                .context(chatClientRequest.context())
                .build();
        });

        return callAdvisorChain;
    }

    private static ChatClientRequest requestWithUserMessage(String text) {
        return new ChatClientRequest(new Prompt(List.of(new UserMessage(text))), Map.of());
    }

    private static String textOf(ChatClientResponse chatClientResponse) {
        ChatResponse chatResponse = Objects.requireNonNull(chatClientResponse.chatResponse());
        Generation generation = Objects.requireNonNull(chatResponse.getResult());
        AssistantMessage assistantMessage = Objects.requireNonNull(generation.getOutput());

        return Objects.requireNonNull(assistantMessage.getText());
    }

    private static ChatClientResponse responseChunk(String text) {
        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(text))))
            .build();

        return ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .context(Map.of())
            .build();
    }

    private static String userTextOf(ChatClientRequest chatClientRequest) {
        Prompt prompt = chatClientRequest.prompt();

        return prompt.getInstructions()
            .stream()
            .filter(message -> message.getMessageType() == MessageType.USER)
            .map(Message::getText)
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow();
    }
}
