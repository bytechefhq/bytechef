/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayFacade;
import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayPlaygroundFacade;
import com.bytechef.ee.automation.ai.gateway.web.rest.AiGatewayPlaygroundRestController.PlaygroundChatCompletionStreamInput;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * Pins that the playground SSE stream reaches the gateway through the facade layer, which is where this codebase puts
 * authorization. The {@code EDITOR} workspace-role guard itself is pinned by
 * {@code AiGatewayPlaygroundFacadeImplTest#testStreamDeniedWhenCallerIsNotAWorkspaceEditor}.
 *
 * <p>
 * The controller carries no gate of its own and is not meant to. Asserting the delegation is not enough on its own:
 * {@code testControllerHoldsNoAuthorizationOrGatewayCollaborators} is what makes a revert to a locally written
 * {@code permissionService} check — or to calling the shared {@code AiGatewayFacade} directly, past the guarded
 * playground facade — fail here rather than pass.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiGatewayPlaygroundRestControllerTest {

    private static final long WORKSPACE_ID = 7L;

    private final AiGatewayPlaygroundFacade aiGatewayPlaygroundFacade = mock(AiGatewayPlaygroundFacade.class);

    private final AiGatewayPlaygroundRestController aiGatewayPlaygroundRestController =
        new AiGatewayPlaygroundRestController(aiGatewayPlaygroundFacade);

    @Test
    void testChatCompletionsStreamReadsThroughTheGuardedFacade() {
        AiGatewayChatCompletionResponse response =
            new AiGatewayChatCompletionResponse("id", "chat.completion", 0L, "gpt-4o", List.of(), null, null);

        when(aiGatewayPlaygroundFacade.playgroundChatCompletionStream(eq(WORKSPACE_ID), any(), any(AtomicLong.class)))
            .thenReturn(Flux.just(response));

        List<ServerSentEvent<Object>> events = aiGatewayPlaygroundRestController.chatCompletionsStream(input())
            .collectList()
            .block();

        // One chunk per gateway response plus the trailing totals chunk the controller appends.
        assertThat(events).hasSize(2);

        verify(aiGatewayPlaygroundFacade)
            .playgroundChatCompletionStream(eq(WORKSPACE_ID), any(), any(AtomicLong.class));
    }

    @Test
    void testChatCompletionsStreamRejectsAMissingWorkspaceIdWithoutReachingTheFacade() {
        PlaygroundChatCompletionStreamInput input = new PlaygroundChatCompletionStreamInput(
            "gpt-4o", List.of(), null, null, null, null);

        assertThatThrownBy(() -> aiGatewayPlaygroundRestController.chatCompletionsStream(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("workspaceId is required for playground streaming");

        verifyNoInteractions(aiGatewayPlaygroundFacade);
    }

    @Test
    void testControllerHoldsNoAuthorizationOrGatewayCollaborators() {
        assertThat(Arrays.stream(AiGatewayPlaygroundRestController.class.getDeclaredFields())
            .map(Field::getType))
                .as("authorization belongs on the playground facade, not on the controller")
                .doesNotContain(PermissionService.class, AiGatewayFacade.class);
    }

    private static PlaygroundChatCompletionStreamInput input() {
        return new PlaygroundChatCompletionStreamInput("gpt-4o", List.of(), null, null, null, WORKSPACE_ID);
    }
}
