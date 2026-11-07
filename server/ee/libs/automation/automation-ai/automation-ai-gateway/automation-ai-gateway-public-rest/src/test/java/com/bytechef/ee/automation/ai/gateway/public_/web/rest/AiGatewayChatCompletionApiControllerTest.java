/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.public_.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayFacade;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatRole;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.HandlerMethod;
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiGatewayChatCompletionApiControllerTest {

    private static final String REQUEST_BODY = """
        {"model": "gpt-4o", "messages": [{"role": "user", "content": "hi"}]}""";

    private AiGatewayFacade aiGatewayFacade;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        aiGatewayFacade = mock(AiGatewayFacade.class);

        when(aiGatewayFacade.chatCompletion(any(), any(), any())).thenReturn(chatCompletionResponse());
        when(aiGatewayFacade.chatCompletionStream(any(), any(), any()))
            .thenReturn(Flux.just(chatCompletionResponse()));

        mockMvc = MockMvcBuilders.standaloneSetup(new AiGatewayChatCompletionApiController(aiGatewayFacade))
            .build();
    }

    @Test
    void testAcceptEventStreamDispatchesToStreamingHandler() throws Exception {
        assertEquals("chatCompletionsStream", dispatchedHandlerMethodName(streamingRequest()));
    }

    @Test
    void testAcceptEventStreamStreamsServerSentEvents() throws Exception {
        MvcResult mvcResult = mockMvc.perform(streamingRequest())
            .andExpect(request().asyncStarted())
            .andReturn();

        String responseBody = mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertTrue(responseBody.contains("data:"), "Expected an SSE data frame but got: " + responseBody);
        assertTrue(responseBody.contains("chatcmpl-1"), "Expected the streamed chunk payload but got: " + responseBody);

        verify(aiGatewayFacade).chatCompletionStream(any(), any(), any());
        verify(aiGatewayFacade, never()).chatCompletion(any(), any(), any());
    }

    @Test
    void testAcceptJsonDispatchesToBlockingHandler() throws Exception {
        assertEquals("chatCompletions", dispatchedHandlerMethodName(jsonRequest(MediaType.APPLICATION_JSON)));
    }

    @Test
    void testAcceptJsonReturnsJsonResponse() throws Exception {
        mockMvc.perform(jsonRequest(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value("chatcmpl-1"))
            .andExpect(header().string("x-gateway-provider", "openai"));

        verify(aiGatewayFacade).chatCompletion(any(), any(), any());
        verify(aiGatewayFacade, never()).chatCompletionStream(any(), any(), any());
    }

    @Test
    void testMissingAcceptDispatchesToBlockingHandler() throws Exception {
        assertEquals("chatCompletions", dispatchedHandlerMethodName(jsonRequest(null)));
    }

    @Test
    void testMissingAcceptReturnsJsonResponse() throws Exception {
        mockMvc.perform(jsonRequest(null))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value("chatcmpl-1"));
    }

    @Test
    void testWildcardAcceptDispatchesToBlockingHandler() throws Exception {
        assertEquals("chatCompletions", dispatchedHandlerMethodName(jsonRequest(MediaType.ALL)));
    }

    private MockHttpServletRequestBuilder streamingRequest() {
        return jsonRequest(MediaType.TEXT_EVENT_STREAM);
    }

    private MockHttpServletRequestBuilder jsonRequest(MediaType acceptMediaType) {
        MockHttpServletRequestBuilder requestBuilder = post("/api/ai-gateway/v1/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(REQUEST_BODY);

        if (acceptMediaType != null) {
            requestBuilder = requestBuilder.accept(acceptMediaType);
        }

        return requestBuilder;
    }

    private String dispatchedHandlerMethodName(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andReturn();

        HandlerMethod handlerMethod = assertInstanceOf(HandlerMethod.class, mvcResult.getHandler());

        return handlerMethod.getMethod()
            .getName();
    }

    private static AiGatewayChatCompletionResponse chatCompletionResponse() {
        return new AiGatewayChatCompletionResponse(
            "chatcmpl-1", "chat.completion", 0L, "gpt-4o",
            List.of(
                new AiGatewayChatCompletionResponse.Choice(
                    0, new AiGatewayChatMessage(AiGatewayChatRole.ASSISTANT, "hello", null, null, null), "stop")),
            new AiGatewayChatCompletionResponse.Usage(1, 1, 2),
            new AiGatewayChatCompletionResponse.GatewayMetadata(
                "openai", "gpt-4o", 12L, false, null, "req-1"));
    }
}
