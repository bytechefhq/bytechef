/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.ai.prompt.service.WorkspaceAiPromptService;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatRole;
import com.bytechef.ee.platform.ai.prompt.AiPromptVersion;
import com.bytechef.ee.platform.ai.prompt.AiPromptVersionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiGatewayPlaygroundFacadeImplTest {

    private static final long OTHER_WORKSPACE_ID = 9L;
    private static final long PROMPT_ID = 42L;
    private static final long WORKSPACE_ID = 7L;

    private final AiGatewayFacade aiGatewayFacade = mock(AiGatewayFacade.class);
    private final AiPromptVersionService aiPromptVersionService = mock(AiPromptVersionService.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final WorkspaceAiPromptService workspaceAiPromptService = mock(WorkspaceAiPromptService.class);

    private final AiGatewayPlaygroundFacade aiGatewayPlaygroundFacade = new AiGatewayPlaygroundFacadeImpl(
        aiGatewayFacade, aiPromptVersionService, permissionService, workspaceAiPromptService);

    @Test
    void testChatCompletionRunsUnderTheCallersWorkspace() {
        AiGatewayChatCompletionResponse response = response();

        when(permissionService.hasWorkspaceRole(WORKSPACE_ID, "EDITOR")).thenReturn(true);
        when(aiGatewayFacade.chatCompletion(any(), eq(null))).thenReturn(response);

        AiGatewayChatCompletionResponse result = aiGatewayPlaygroundFacade.playgroundChatCompletion(
            WORKSPACE_ID, requestWithTags(Map.of("workspace_id", "999", "project_id", "5")), null, null);

        assertThat(result).isSameAs(response);

        AiGatewayChatCompletionRequest sentRequest = captureSyncRequest();

        assertThat(sentRequest.tags()).isEqualTo(Map.of("workspace_id", String.valueOf(WORKSPACE_ID)));
        assertThat(sentRequest.messages()).hasSize(1);
    }

    @Test
    void testChatCompletionDeniedWhenCallerIsNotAWorkspaceEditor() {
        when(permissionService.hasWorkspaceRole(WORKSPACE_ID, "EDITOR")).thenReturn(false);

        assertThatThrownBy(
            () -> aiGatewayPlaygroundFacade.playgroundChatCompletion(WORKSPACE_ID, request(), null, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Not authorized for the requested workspace");

        verifyNoInteractions(aiGatewayFacade, workspaceAiPromptService, aiPromptVersionService);
    }

    @Test
    void testChatCompletionDeniedWhenPermissionServiceIsNotWired() {
        AiGatewayPlaygroundFacade facade = new AiGatewayPlaygroundFacadeImpl(
            aiGatewayFacade, aiPromptVersionService, null, workspaceAiPromptService);

        assertThatThrownBy(() -> facade.playgroundChatCompletion(WORKSPACE_ID, request(), null, null))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Not authorized for the requested workspace");

        verifyNoInteractions(aiGatewayFacade);
    }

    @Test
    void testManagedPromptIsRenderedAndPrependedAsSystemMessage() {
        AiPromptVersion activeVersion = mock(AiPromptVersion.class);

        when(activeVersion.getContent()).thenReturn("You help {{ customer }} with {{topic}}.");
        when(permissionService.hasWorkspaceRole(WORKSPACE_ID, "EDITOR")).thenReturn(true);
        when(workspaceAiPromptService.getWorkspaceId(PROMPT_ID)).thenReturn(WORKSPACE_ID);
        when(aiPromptVersionService.getActiveVersion(PROMPT_ID, "production")).thenReturn(Optional.of(activeVersion));
        when(aiGatewayFacade.chatCompletion(any(), eq(null))).thenReturn(response());

        aiGatewayPlaygroundFacade.playgroundChatCompletion(
            WORKSPACE_ID, request(), PROMPT_ID, "{\"customer\":\"Acme\",\"topic\":\"billing\"}");

        AiGatewayChatCompletionRequest sentRequest = captureSyncRequest();

        assertThat(sentRequest.messages()).hasSize(2);

        AiGatewayChatMessage systemMessage = sentRequest.messages()
            .getFirst();

        assertThat(systemMessage.role()).isEqualTo(AiGatewayChatRole.SYSTEM);
        assertThat(systemMessage.content()).isEqualTo("You help Acme with billing.");
        assertThat(sentRequest.tags()).isEqualTo(Map.of("workspace_id", String.valueOf(WORKSPACE_ID)));
    }

    @Test
    void testManagedPromptFromAnotherWorkspaceIsRejected() {
        when(permissionService.hasWorkspaceRole(WORKSPACE_ID, "EDITOR")).thenReturn(true);
        when(workspaceAiPromptService.getWorkspaceId(PROMPT_ID)).thenReturn(OTHER_WORKSPACE_ID);

        assertThatThrownBy(
            () -> aiGatewayPlaygroundFacade.playgroundChatCompletion(WORKSPACE_ID, request(), PROMPT_ID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prompt not found: " + PROMPT_ID);

        verifyNoInteractions(aiGatewayFacade, aiPromptVersionService);
    }

    @Test
    void testManagedPromptWithMalformedVariablesIsRejected() {
        AiPromptVersion activeVersion = mock(AiPromptVersion.class);

        when(activeVersion.getContent()).thenReturn("Hello {{name}}");
        when(permissionService.hasWorkspaceRole(WORKSPACE_ID, "EDITOR")).thenReturn(true);
        when(workspaceAiPromptService.getWorkspaceId(PROMPT_ID)).thenReturn(WORKSPACE_ID);
        when(aiPromptVersionService.getActiveVersion(PROMPT_ID, "production")).thenReturn(Optional.of(activeVersion));

        assertThatThrownBy(
            () -> aiGatewayPlaygroundFacade.playgroundChatCompletion(WORKSPACE_ID, request(), PROMPT_ID, "{not json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Malformed prompt variables JSON");

        verify(aiGatewayFacade, never()).chatCompletion(any(), any());
    }

    @Test
    void testStreamDeniedWhenCallerIsNotAWorkspaceEditor() {
        when(permissionService.hasWorkspaceRole(WORKSPACE_ID, "EDITOR")).thenReturn(false);

        assertThatThrownBy(
            () -> aiGatewayPlaygroundFacade.playgroundChatCompletionStream(
                WORKSPACE_ID, AiGatewayPlaygroundFacadeImplTest::request, new AtomicLong()))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Not authorized for the requested workspace");

        verify(aiGatewayFacade, never()).chatCompletionStream(any(), any(), any(), any(AtomicLong.class));
    }

    @Test
    void testStreamDeniedWhenPermissionServiceIsNotWired() {
        AiGatewayPlaygroundFacade facade = new AiGatewayPlaygroundFacadeImpl(
            aiGatewayFacade, aiPromptVersionService, null, workspaceAiPromptService);

        assertThatThrownBy(
            () -> facade.playgroundChatCompletionStream(
                WORKSPACE_ID, AiGatewayPlaygroundFacadeImplTest::request, new AtomicLong()))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Not authorized for the requested workspace");

        verify(aiGatewayFacade, never()).chatCompletionStream(any(), any(), any(), any(AtomicLong.class));
    }

    @Test
    void testStreamRunsUnderTheCallersWorkspace() {
        AtomicLong traceIdHolder = new AtomicLong();
        AiGatewayChatCompletionResponse response = response();

        when(permissionService.hasWorkspaceRole(WORKSPACE_ID, "EDITOR")).thenReturn(true);
        when(aiGatewayFacade.chatCompletionStream(any(), eq(null), eq(null), eq(traceIdHolder)))
            .thenReturn(Flux.just(response));

        StepVerifier
            .create(
                aiGatewayPlaygroundFacade.playgroundChatCompletionStream(
                    WORKSPACE_ID, () -> requestWithTags(Map.of("workspace_id", "999")), traceIdHolder))
            .expectNext(response)
            .verifyComplete();

        ArgumentCaptor<AiGatewayChatCompletionRequest> requestCaptor =
            ArgumentCaptor.forClass(AiGatewayChatCompletionRequest.class);

        verify(aiGatewayFacade).chatCompletionStream(requestCaptor.capture(), eq(null), eq(null), eq(traceIdHolder));

        AiGatewayChatCompletionRequest sentRequest = requestCaptor.getValue();

        assertThat(sentRequest.tags()).isEqualTo(Map.of("workspace_id", String.valueOf(WORKSPACE_ID)));
    }

    @Test
    void testRequestIsBuiltOnSubscriptionSoAMalformedRequestBecomesAnErrorSignal() {
        when(permissionService.hasWorkspaceRole(WORKSPACE_ID, "EDITOR")).thenReturn(true);

        Flux<AiGatewayChatCompletionResponse> flux = aiGatewayPlaygroundFacade.playgroundChatCompletionStream(
            WORKSPACE_ID, () -> {
                throw new IllegalArgumentException("No enum constant AiGatewayChatRole.bogus");
            }, new AtomicLong());

        StepVerifier.create(flux)
            .verifyError(IllegalArgumentException.class);
    }

    private AiGatewayChatCompletionRequest captureSyncRequest() {
        ArgumentCaptor<AiGatewayChatCompletionRequest> requestCaptor =
            ArgumentCaptor.forClass(AiGatewayChatCompletionRequest.class);

        verify(aiGatewayFacade).chatCompletion(requestCaptor.capture(), eq(null));

        return requestCaptor.getValue();
    }

    private static AiGatewayChatCompletionRequest request() {
        return requestWithTags(null);
    }

    private static AiGatewayChatCompletionRequest requestWithTags(Map<String, String> tags) {
        return new AiGatewayChatCompletionRequest(
            "openai/gpt-4o", List.of(new AiGatewayChatMessage(AiGatewayChatRole.USER, "Hi", null, null, null)), null,
            null, null, false, null, null, null, null, tags);
    }

    private static AiGatewayChatCompletionResponse response() {
        return new AiGatewayChatCompletionResponse("id", "chat.completion", 0L, "gpt-4o", List.of(), null, null);
    }
}
