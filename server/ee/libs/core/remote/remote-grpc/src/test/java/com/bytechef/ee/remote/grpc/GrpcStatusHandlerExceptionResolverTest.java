/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.Status;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 */
class GrpcStatusHandlerExceptionResolverTest {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
        .build();

    private final GrpcStatusHandlerExceptionResolver grpcStatusHandlerExceptionResolver =
        new GrpcStatusHandlerExceptionResolver(JSON_MAPPER);

    @Test
    void testPermissionDeniedKeepsItsReasonLikeALocalDenial() throws Exception {
        MockHttpServletResponse mockHttpServletResponse = resolve(
            Status.PERMISSION_DENIED
                .withDescription("Only the connection creator or an admin can modify connection 7"));

        JsonNode problemDetail = JSON_MAPPER.readTree(mockHttpServletResponse.getContentAsString());

        assertThat(mockHttpServletResponse.getStatus()).isEqualTo(500);
        assertThat(mockHttpServletResponse.getContentType()).isEqualTo("application/problem+json");
        assertThat(problemDetail.get("detail")
            .asString()).isEqualTo("Only the connection creator or an admin can modify connection 7");
        assertThat(problemDetail.get("status")
            .asInt()).isEqualTo(500);
    }

    @Test
    void testStatusesMapToHttp() throws Exception {
        assertThat(resolve(Status.UNAUTHENTICATED).getStatus()).isEqualTo(500);
        assertThat(resolve(Status.NOT_FOUND).getStatus()).isEqualTo(404);
        assertThat(resolve(Status.INVALID_ARGUMENT).getStatus()).isEqualTo(400);
        assertThat(resolve(Status.INTERNAL.withDescription("IllegalStateException: boom")).getStatus())
            .isEqualTo(500);
    }

    @Test
    void testUnavailableAndTimedOutRemoteCallsMapToGatewayStatuses() {
        assertThat(resolve(Status.UNAVAILABLE).getStatus()).isEqualTo(503);
        assertThat(resolve(Status.DEADLINE_EXCEEDED).getStatus()).isEqualTo(504);
    }

    @Test
    void testCommittedResponseIsLeftAsItIs() throws Exception {
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        mockHttpServletResponse.setStatus(200);
        mockHttpServletResponse.getWriter()
            .write("partial");
        mockHttpServletResponse.setCommitted(true);

        ModelAndView modelAndView = grpcStatusHandlerExceptionResolver.resolveException(
            new MockHttpServletRequest("GET", "/api/connections"), mockHttpServletResponse, null,
            Status.UNAVAILABLE.asRuntimeException());

        assertThat(modelAndView).isNotNull();
        assertThat(mockHttpServletResponse.getStatus()).isEqualTo(200);
        assertThat(mockHttpServletResponse.getContentAsString()).isEqualTo("partial");
    }

    @Test
    void testOtherExceptionsAreLeftToTheNextResolver() {
        ModelAndView modelAndView = grpcStatusHandlerExceptionResolver.resolveException(
            new MockHttpServletRequest(), new MockHttpServletResponse(), null, new IllegalStateException("boom"));

        assertThat(modelAndView).isNull();
    }

    private MockHttpServletResponse resolve(Status status) {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest("GET", "/api/connections");
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        ModelAndView modelAndView = grpcStatusHandlerExceptionResolver.resolveException(
            mockHttpServletRequest, mockHttpServletResponse, null, status.asRuntimeException());

        assertThat(modelAndView).isNotNull();

        return mockHttpServletResponse;
    }
}
