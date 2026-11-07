/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.gateway.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayFacade;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatRole;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.CredentialType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.HandlerMethod;
import reactor.core.publisher.Flux;

/**
 * @version ee
 * @author Ivica Cardic
 */
class EmbeddedAiGatewayChatCompletionApiControllerTest {

    private static final long ENVIRONMENT_ID = 1L;
    private static final String REQUEST_BODY = """
        {"model": "gpt-4o", "messages": [{"role": "user", "content": "hi"}]}""";

    private AiGatewayFacade aiGatewayFacade;
    private ConnectedUser connectedUser;
    private ConnectedUserService connectedUserService;
    private MockMvc mockMvc;

    @BeforeEach
    void beforeEach() {
        aiGatewayFacade = mock(AiGatewayFacade.class);
        connectedUser = mock(ConnectedUser.class);
        connectedUserService = mock(ConnectedUserService.class);

        when(connectedUser.getId()).thenReturn(5L);
        when(connectedUser.isEnabled()).thenReturn(true);

        when(aiGatewayFacade.chatCompletion(any(), any(), any(), anyLong())).thenReturn(chatCompletionResponse());
        when(aiGatewayFacade.chatCompletionStream(any(), any(), any(), any(), any()))
            .thenReturn(Flux.just(chatCompletionResponse()));

        mockMvc = MockMvcBuilders.standaloneSetup(
            new EmbeddedAiGatewayChatCompletionApiController(aiGatewayFacade, connectedUserService))
            .build();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testResolvesTheConnectedUserAndPassesItsIdToTheFacade() throws Exception {
        authenticateAs("customer-1");

        when(connectedUserService.fetchConnectedUser("customer-1", ENVIRONMENT_ID))
            .thenReturn(Optional.of(connectedUser));

        mockMvc.perform(
            post("/v1/customer-1/ai-gateway/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY))
            .andExpect(status().isOk());

        verify(aiGatewayFacade).chatCompletion(any(), any(), any(), eq(5L));
    }

    @Test
    void testUnknownExternalUserIdIsRejected() throws Exception {
        authenticateAs("ghost");

        when(connectedUserService.fetchConnectedUser("ghost", ENVIRONMENT_ID)).thenReturn(Optional.empty());

        mockMvc.perform(
            post("/v1/ghost/ai-gateway/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY))
            .andExpect(status().isForbidden());

        verifyNoInteractions(aiGatewayFacade);
    }

    @Test
    void testDisabledConnectedUserIsRejected() throws Exception {
        authenticateAs("customer-1");

        when(connectedUser.isEnabled()).thenReturn(false);
        when(connectedUserService.fetchConnectedUser("customer-1", ENVIRONMENT_ID))
            .thenReturn(Optional.of(connectedUser));

        mockMvc.perform(
            post("/v1/customer-1/ai-gateway/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY))
            .andExpect(status().isForbidden());
    }

    @Test
    void testNoConnectedUserIsEverCreated() throws Exception {
        authenticateAs("ghost");

        when(connectedUserService.fetchConnectedUser("ghost", ENVIRONMENT_ID)).thenReturn(Optional.empty());

        mockMvc.perform(
            post("/v1/ghost/ai-gateway/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY));

        verify(connectedUserService, never()).createConnectedUser(anyString(), anyLong());
        verify(connectedUserService, never()).createConnectedUser(anyString(), any(Environment.class));
    }

    @Test
    void testMismatchedIdentityIsRejected() {
        authenticateAs("someone-else");

        assertThatThrownBy(
            () -> mockMvc.perform(
                post("/v1/customer-1/ai-gateway/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(REQUEST_BODY)))
                        .hasCauseInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(aiGatewayFacade);
        verifyNoInteractions(connectedUserService);
    }

    @Test
    void testMismatchedIdentityIsRejectedOnStreamingMapping() {
        authenticateAs("someone-else");

        assertThatThrownBy(
            () -> mockMvc.perform(
                post("/v1/customer-1/ai-gateway/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .content(REQUEST_BODY)))
                        .hasCauseInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(aiGatewayFacade);
        verifyNoInteractions(connectedUserService);
    }

    @Test
    void testUnknownConnectedUserIsRejectedOnStreamingMapping() throws Exception {
        authenticateAs("ghost");

        when(connectedUserService.fetchConnectedUser("ghost", ENVIRONMENT_ID)).thenReturn(Optional.empty());

        mockMvc.perform(
            post("/v1/ghost/ai-gateway/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .content(REQUEST_BODY))
            .andExpect(status().isForbidden());

        verifyNoInteractions(aiGatewayFacade);
    }

    @Test
    void testRequestBindsSnakeCaseFieldsWithStringToolChoice() throws Exception {
        authenticateAs("customer-1");

        when(connectedUserService.fetchConnectedUser("customer-1", ENVIRONMENT_ID))
            .thenReturn(Optional.of(connectedUser));

        String requestBody = """
            {
                "model": "gpt-4o",
                "messages": [{"role": "user", "content": "hi"}],
                "max_tokens": 256,
                "top_p": 0.5,
                "routing_policy": "cost-optimized",
                "tool_choice": "auto"
            }""";

        mockMvc.perform(
            post("/v1/customer-1/ai-gateway/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());

        AiGatewayChatCompletionRequest request = captureFacadeRequest();

        assertThat(request.maxTokens()).isEqualTo(256);
        assertThat(request.topP()).isEqualTo(0.5);
        assertThat(request.routingPolicy()).isEqualTo("cost-optimized");
        assertThat(request.toolChoice()
            .isStringValue()).isTrue();
        assertThat(request.toolChoice()
            .stringValue()).isEqualTo("auto");
    }

    @Test
    void testOmittedCollectionFieldsReachTheFacadeAsNullNotEmpty() throws Exception {
        authenticateAs("customer-1");

        when(connectedUserService.fetchConnectedUser("customer-1", ENVIRONMENT_ID))
            .thenReturn(Optional.of(connectedUser));

        String requestBody = """
            {
                "model": "gpt-4o",
                "messages": [{"role": "user", "content": "hi"}]
            }""";

        mockMvc.perform(
            post("/v1/customer-1/ai-gateway/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());

        AiGatewayChatCompletionRequest request = captureFacadeRequest();

        assertThat(request.tools()).isNull();
        assertThat(request.tags()).isNull();
        assertThat(request.messages()).hasSize(1);

        AiGatewayChatMessage message = request.messages()
            .getFirst();

        assertThat(message.toolCalls()).isNull();
        assertThat(message.contentBlocks()).isNull();
    }

    @Test
    void testRequestBindsSnakeCaseFieldsWithObjectToolChoice() throws Exception {
        authenticateAs("customer-1");

        when(connectedUserService.fetchConnectedUser("customer-1", ENVIRONMENT_ID))
            .thenReturn(Optional.of(connectedUser));

        String requestBody = """
            {
                "model": "gpt-4o",
                "messages": [{"role": "user", "content": "hi"}],
                "max_tokens": 256,
                "top_p": 0.5,
                "routing_policy": "cost-optimized",
                "tool_choice": {"type": "function", "function": {"name": "lookup"}}
            }""";

        mockMvc.perform(
            post("/v1/customer-1/ai-gateway/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());

        AiGatewayChatCompletionRequest request = captureFacadeRequest();

        assertThat(request.maxTokens()).isEqualTo(256);
        assertThat(request.topP()).isEqualTo(0.5);
        assertThat(request.routingPolicy()).isEqualTo("cost-optimized");
        assertThat(request.toolChoice()
            .isStringValue()).isFalse();
        assertThat(request.toolChoice()
            .toolRef()
            .name()).isEqualTo("lookup");
    }

    @Test
    void testResponseUsesWireFieldNamesAndOmitsGatewayMetadataFromTheBody() throws Exception {
        authenticateAs("customer-1");

        when(connectedUserService.fetchConnectedUser("customer-1", ENVIRONMENT_ID))
            .thenReturn(Optional.of(connectedUser));
        when(aiGatewayFacade.chatCompletion(any(), any(), any(), anyLong()))
            .thenReturn(chatCompletionResponseWithMetadata());

        MvcResult mvcResult = mockMvc.perform(
            post("/v1/customer-1/ai-gateway/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.choices[0].finish_reason").value("stop"))
            .andExpect(jsonPath("$.usage.prompt_tokens").value(3))
            .andExpect(jsonPath("$.usage.completion_tokens").value(7))
            .andExpect(jsonPath("$.usage.total_tokens").value(10))
            .andExpect(header().string("x-gateway-provider", "openai"))
            .andExpect(header().string("x-gateway-budget-warning", "2.50"))
            .andReturn();

        String responseBody = mvcResult.getResponse()
            .getContentAsString();

        assertThat(responseBody).doesNotContain("gatewayMetadata");
    }

    private AiGatewayChatCompletionRequest captureFacadeRequest() {
        ArgumentCaptor<AiGatewayChatCompletionRequest> requestCaptor = ArgumentCaptor.forClass(
            AiGatewayChatCompletionRequest.class);

        verify(aiGatewayFacade).chatCompletion(requestCaptor.capture(), any(), any(), eq(5L));

        return requestCaptor.getValue();
    }

    @Test
    void testAcceptEventStreamDispatchesToStreamingHandler() throws Exception {
        authenticateAs("customer-1");

        when(connectedUserService.fetchConnectedUser("customer-1", ENVIRONMENT_ID))
            .thenReturn(Optional.of(connectedUser));

        MvcResult mvcResult = mockMvc.perform(
            post("/v1/customer-1/ai-gateway/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .content(REQUEST_BODY))
            .andReturn();

        HandlerMethod handlerMethod = assertInstanceOf(HandlerMethod.class, mvcResult.getHandler());

        assertEquals(
            "chatCompletionsStream", handlerMethod.getMethod()
                .getName());

        verify(aiGatewayFacade).chatCompletionStream(any(), any(), any(), eq(5L), any());
    }

    @Test
    void testEmptyRequestBodyThrowsIllegalArgumentException() throws Exception {
        authenticateAs("customer-1");

        when(connectedUserService.fetchConnectedUser("customer-1", ENVIRONMENT_ID))
            .thenReturn(Optional.of(connectedUser));

        assertThatThrownBy(
            () -> mockMvc.perform(
                post("/v1/customer-1/ai-gateway/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")))
                        .hasCauseInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(aiGatewayFacade);
    }

    private static void authenticateAs(String externalUserId) {
        User user = new User(externalUserId, "", List.of());

        SecurityContextHolder.getContext()
            .setAuthentication(new TestApiKeyAuthenticationToken(ENVIRONMENT_ID, user));
    }

    private static AiGatewayChatCompletionResponse chatCompletionResponse() {
        return new AiGatewayChatCompletionResponse(
            "chatcmpl-1", "chat.completion", 0L, "gpt-4o",
            List.of(new AiGatewayChatCompletionResponse.Choice(
                0, new AiGatewayChatMessage(AiGatewayChatRole.ASSISTANT, "hello"), "stop")),
            new AiGatewayChatCompletionResponse.Usage(1, 1, 2));
    }

    private static AiGatewayChatCompletionResponse chatCompletionResponseWithMetadata() {
        return new AiGatewayChatCompletionResponse(
            "chatcmpl-1", "chat.completion", 0L, "gpt-4o",
            List.of(new AiGatewayChatCompletionResponse.Choice(
                0, new AiGatewayChatMessage(AiGatewayChatRole.ASSISTANT, "hello"), "stop")),
            new AiGatewayChatCompletionResponse.Usage(3, 7, 10),
            new AiGatewayChatCompletionResponse.GatewayMetadata(
                "openai", "gpt-4o", 12L, false, "cost-optimized", "req-1", new BigDecimal("2.50")));
    }

    private static final class TestApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

        TestApiKeyAuthenticationToken(long environmentId, User user) {
            super(environmentId, user);
        }

        @Override
        public CredentialType getCredentialType() {
            return CredentialType.EMBEDDED_API_KEY;
        }

        @Override
        public PlatformType getPlatformType() {
            return PlatformType.EMBEDDED;
        }
    }
}
