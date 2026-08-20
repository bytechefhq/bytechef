/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * Pins the {@code EDITOR} workspace-role guard that moved off
 * {@code AiGatewayPlaygroundRestController.chatCompletionsStream}'s method body onto this facade.
 *
 * <p>
 * {@code testStreamDeniedWhenCallerIsNotAWorkspaceEditor} goes red if the guard is deleted. It asserts the throw is
 * synchronous rather than a Flux error signal, because that distinction is the whole wire shape: a synchronous throw
 * becomes a 403 on the request, while an error signal would be framed as an {@code event: error} on an accepted SSE
 * stream. It also asserts the gateway is never invoked, so a guard demoted to somewhere inside the returned pipeline
 * cannot pass here either.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiGatewayPlaygroundFacadeImplTest {

    private static final long WORKSPACE_ID = 7L;

    private final AiGatewayFacade aiGatewayFacade = mock(AiGatewayFacade.class);
    private final PermissionService permissionService = mock(PermissionService.class);

    private final AiGatewayPlaygroundFacade aiGatewayPlaygroundFacade =
        new AiGatewayPlaygroundFacadeImpl(aiGatewayFacade, permissionService);

    @Test
    void testStreamDeniedWhenCallerIsNotAWorkspaceEditor() {
        when(permissionService.hasWorkspaceRole(WORKSPACE_ID, "EDITOR")).thenReturn(false);

        assertThatThrownBy(
            () -> aiGatewayPlaygroundFacade.playgroundChatCompletionStream(
                WORKSPACE_ID, AiGatewayPlaygroundFacadeImplTest::request, new AtomicLong()))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Not authorized for the requested workspace");

        verify(aiGatewayFacade, never()).chatCompletionStream(any(), any(), any(), any());
    }

    @Test
    void testStreamDeniedWhenPermissionServiceIsNotWired() {
        AiGatewayPlaygroundFacade facade = new AiGatewayPlaygroundFacadeImpl(aiGatewayFacade, null);

        assertThatThrownBy(
            () -> facade.playgroundChatCompletionStream(
                WORKSPACE_ID, AiGatewayPlaygroundFacadeImplTest::request, new AtomicLong()))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Not authorized for the requested workspace");

        verify(aiGatewayFacade, never()).chatCompletionStream(any(), any(), any(), any());
    }

    @Test
    void testStreamAllowedForAWorkspaceEditor() {
        AtomicLong traceIdHolder = new AtomicLong();
        AiGatewayChatCompletionResponse response =
            new AiGatewayChatCompletionResponse("id", "chat.completion", 0L, "gpt-4o", List.of(), null, null);

        when(permissionService.hasWorkspaceRole(WORKSPACE_ID, "EDITOR")).thenReturn(true);
        when(aiGatewayFacade.chatCompletionStream(any(), eq(null), eq(null), eq(traceIdHolder)))
            .thenReturn(Flux.just(response));

        StepVerifier
            .create(
                aiGatewayPlaygroundFacade.playgroundChatCompletionStream(
                    WORKSPACE_ID, AiGatewayPlaygroundFacadeImplTest::request, traceIdHolder))
            .expectNext(response)
            .verifyComplete();
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

    private static AiGatewayChatCompletionRequest request() {
        return new AiGatewayChatCompletionRequest(
            "gpt-4o", List.of(), null, null, null, true, null, null, null, null, null);
    }
}
