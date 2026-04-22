/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.public_.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bytechef.ee.platform.ai.gateway.domain.BudgetExceededException;
import com.bytechef.ee.platform.ai.gateway.exception.BadRequestException;
import com.bytechef.ee.platform.ai.gateway.exception.SafeMessage;
import com.bytechef.ee.platform.ai.llm.usage.Money;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * @author Ivica Cardic
 * @version ee
 */
class AiGatewayExceptionHandlerTest {

    private AiGatewayExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new AiGatewayExceptionHandler();
    }

    @Test
    void testIllegalArgumentExceptionReturnsBadRequest() {
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleBadRequest(
            new IllegalArgumentException("Invalid model format"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertErrorBody(response.getBody(), "invalid_request_error", "Invalid model format");
    }

    @Test
    void testBudgetExceededExceptionReturnsPaymentRequired() {
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleBudgetExceeded(
            new BudgetExceededException(
                "Budget limit exceeded", Money.usd(new BigDecimal("100.00")), Money.usd(new BigDecimal("100.00"))));

        assertEquals(HttpStatus.PAYMENT_REQUIRED, response.getStatusCode());
        assertErrorBody(response.getBody(), "budget_exceeded", "Budget limit exceeded");
    }

    @Test
    void testHttpClientErrorReturnsGatewayError() {
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleUpstreamClientError(
            HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null));

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    }

    @Test
    void testHttpServerErrorReturnsGatewayError() {
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleUpstreamServerError(
            HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error", null, null, null));

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    }

    @Test
    void testResourceAccessExceptionReturnsGatewayTimeout() {
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleNetworkTimeout(
            new ResourceAccessException("Connection timed out"));

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
    }

    @Test
    void testDataAccessExceptionReturnsServiceUnavailable() {
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleDatabaseError(
            new DataAccessResourceFailureException("Database unavailable"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void testIllegalStateExceptionReturnsServiceUnavailable() {
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleIllegalState(
            new IllegalStateException("Service not ready"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        // The wire body MUST NOT echo the IllegalStateException message — the OTLP ingest facade's
        // trace-race rethrow embeds the column-pair "(workspace_id, external_trace_id)" in its message,
        // which is schema-shape information a probing caller could use. Test pins the fixed-shape body.
        assertErrorBody(response.getBody(), "service_unavailable", "Service temporarily unavailable");
    }

    @Test
    void testBadRequestExceptionReturnsBadRequestWithSafeMessageOnly() {
        // The diagnostic detail ("internal trace id 99999 missing column workspace_id") MUST stay server-side;
        // only the safeClientMessage reaches the wire. SafeMessage.of(...) screens the wire-side string at the
        // throw site so a future caller cannot accidentally surface the diagnostic detail.
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleStructuredBadRequest(
            new BadRequestException(
                SafeMessage.of("Invalid metadata payload"),
                "internal trace id 99999 missing column workspace_id"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertErrorBody(response.getBody(), "invalid_request_error", "Invalid metadata payload");
    }

    @Test
    void testGenericExceptionReturnsInternalServerError() {
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGenericException(
            new RuntimeException("Unexpected error"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertErrorBody(response.getBody(), "internal_error", "An internal error occurred");
    }

    @SuppressWarnings("unchecked")
    private void assertErrorBody(Map<String, Object> body, String expectedType, String expectedMessage) {
        assertNotNull(body);

        Map<String, Object> error = (Map<String, Object>) body.get("error");

        assertNotNull(error);
        assertEquals(expectedType, error.get("type"));
        assertEquals(expectedMessage, error.get("message"));
    }
}
