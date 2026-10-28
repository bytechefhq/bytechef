/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testPermissionDeniedAnswersAccessDeniedLikeALocalDenial() throws Exception {
        setAuthentication(UsernamePasswordAuthenticationToken.authenticated("user", null, List.of()));

        MockHttpServletResponse mockHttpServletResponse = resolve(
            Status.PERMISSION_DENIED
                .withDescription("Only the connection creator or an admin can modify connection 7"));

        JsonNode problemDetail = JSON_MAPPER.readTree(mockHttpServletResponse.getContentAsString());

        assertThat(mockHttpServletResponse.getStatus()).isEqualTo(403);
        assertThat(mockHttpServletResponse.getContentType()).isEqualTo("application/problem+json");
        assertThat(problemDetail.get("detail")
            .asString()).isEqualTo("Access denied");
        assertThat(problemDetail.get("errorCode")
            .asString()).isEqualTo("ACCESS_DENIED");
        assertThat(problemDetail.get("status")
            .asInt()).isEqualTo(403);
        assertThat(problemDetail.get("title")
            .asString()).isEqualTo("Forbidden");
    }

    @Test
    void testPermissionDeniedWithoutAuthenticationAnswersAccessDenied() {
        assertThat(resolve(Status.PERMISSION_DENIED).getStatus()).isEqualTo(403);
    }

    @Test
    void testPermissionDeniedForAnAnonymousCallerIsRethrownAsAccessDenied() {
        setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThatThrownBy(() -> resolve(Status.PERMISSION_DENIED.withDescription("denied")))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("denied")
            .hasCauseInstanceOf(StatusRuntimeException.class);
    }

    @Test
    void testPermissionDeniedForARememberedCallerIsRethrownAsAccessDenied() {
        setAuthentication(
            new RememberMeAuthenticationToken("key", "user", AuthorityUtils.createAuthorityList("ROLE_USER")));

        assertThatThrownBy(() -> resolve(Status.PERMISSION_DENIED))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testProblemDetailHasTheShapeOfTheGlobalErrorHandler() throws Exception {
        JsonNode problemDetail = JSON_MAPPER.readTree(resolve(Status.NOT_FOUND.withDescription("Connection 7"))
            .getContentAsString());

        assertThat(problemDetail.has("properties")).isFalse();
        assertThat(problemDetail.get("instance")
            .asString()).isEqualTo("/api/connections");
        assertThat(problemDetail.get("title")
            .asString()).isEqualTo("Not Found");
    }

    @Test
    void testProblemDetailPropertiesAreWrittenAtTopLevel() throws Exception {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Connection 7");

        problemDetail.setProperty("errorKey", 101);

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        grpcStatusHandlerExceptionResolver.writeProblemDetail(mockHttpServletResponse, problemDetail);

        JsonNode body = JSON_MAPPER.readTree(mockHttpServletResponse.getContentAsString());

        assertThat(body.has("properties")).isFalse();
        assertThat(body.get("errorKey")
            .asInt()).isEqualTo(101);
        assertThat(body.get("detail")
            .asString()).isEqualTo("Connection 7");
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

    private static void setAuthentication(Authentication authentication) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);

        SecurityContextHolder.setContext(securityContext);
    }
}
