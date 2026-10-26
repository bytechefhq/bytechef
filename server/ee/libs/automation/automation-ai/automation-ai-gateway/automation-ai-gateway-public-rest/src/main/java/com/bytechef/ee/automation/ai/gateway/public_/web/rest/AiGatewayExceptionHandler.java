/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.public_.web.rest;

import com.bytechef.ee.platform.ai.gateway.domain.BudgetExceededException;
import com.bytechef.ee.platform.ai.gateway.domain.RateLimitExceededException;
import com.bytechef.ee.platform.ai.gateway.exception.AiGatewayGuardrailException;
import com.bytechef.ee.platform.ai.gateway.exception.BadRequestException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestControllerAdvice(basePackages = {
    "com.bytechef.ee.automation.ai.gateway",
    "com.bytechef.ee.automation.ai.eval",
    "com.bytechef.ee.embedded.ai.gateway"
})
public class AiGatewayExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException exception) {
        log.warn("Bad request", exception);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(toErrorBody("invalid_request_error", exception.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<Map<String, Object>> handleStructuredBadRequest(BadRequestException exception) {
        log.warn("Bad request: {}", exception.getMessage(), exception);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(toErrorBody("invalid_request_error", exception.getSafeClientMessage()));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<Map<String, Object>> handleRateLimitExceeded(RateLimitExceededException exception) {
        log.info("Rate limit exceeded: {}", exception.getMessage());

        Map<String, Object> errorFields = new LinkedHashMap<>();

        errorFields.put("message", exception.getMessage() != null ? exception.getMessage() : "Rate limit exceeded");
        errorFields.put("type", "rate_limit_exceeded");

        Long resetAtEpochMs = exception.getResetAtEpochMs();

        if (resetAtEpochMs != null) {
            errorFields.put("resetAtEpochMs", resetAtEpochMs);
        }

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS);

        if (resetAtEpochMs != null) {
            long retryAfterSeconds = Math.max(1L, (resetAtEpochMs - Instant.now()
                .toEpochMilli()) / 1000L);

            responseBuilder.header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        }

        return responseBuilder.body(Map.of("error", errorFields));
    }

    @ExceptionHandler(AiGatewayGuardrailException.class)
    ResponseEntity<Map<String, Object>> handleGuardrailViolation(AiGatewayGuardrailException exception) {
        log.info("AI Gateway request rejected by content guardrail");

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(toErrorBody("guardrail_violation", exception.getMessage()));
    }

    @ExceptionHandler(BudgetExceededException.class)
    ResponseEntity<Map<String, Object>> handleBudgetExceeded(BudgetExceededException exception) {
        Map<String, Object> errorFields = new LinkedHashMap<>();

        errorFields.put("message", exception.getMessage() != null ? exception.getMessage() : "Budget exceeded");
        errorFields.put("type", "budget_exceeded");

        if (exception.getBudgetUsd() != null) {
            errorFields.put("budgetUsd", exception.getBudgetUsd());
        }

        if (exception.getSpentUsd() != null) {
            errorFields.put("spentUsd", exception.getSpentUsd());
        }

        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
            .body(Map.of("error", errorFields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> handleMalformedRequest(HttpMessageNotReadableException exception) {
        log.debug("Malformed request body", exception);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(toErrorBody("invalid_request_error", "Malformed request body"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> handleValidationError(MethodArgumentNotValidException exception) {
        List<String> fieldErrors = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .toList();

        String message = fieldErrors.isEmpty() ? "Request validation failed" : String.join("; ", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(toErrorBody("invalid_request_error", message));
    }

    @ExceptionHandler(HttpClientErrorException.class)
    ResponseEntity<Map<String, Object>> handleUpstreamClientError(HttpClientErrorException exception) {
        log.warn("Upstream LLM provider returned client error: {} {}", exception.getStatusCode(),
            exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(toErrorBody("upstream_error",
                "Upstream LLM provider returned error: " + exception.getStatusCode()));
    }

    @ExceptionHandler(HttpServerErrorException.class)
    ResponseEntity<Map<String, Object>> handleUpstreamServerError(HttpServerErrorException exception) {
        log.error("Upstream LLM provider returned server error: {} {}", exception.getStatusCode(),
            exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(toErrorBody("upstream_error",
                "Upstream LLM provider is temporarily unavailable"));
    }

    @ExceptionHandler(ResourceAccessException.class)
    ResponseEntity<Map<String, Object>> handleNetworkTimeout(ResourceAccessException exception) {
        log.error("Network error communicating with upstream LLM provider", exception);

        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
            .body(toErrorBody("timeout_error",
                "Timed out communicating with upstream LLM provider"));
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Map<String, Object>> handleDatabaseError(DataAccessException exception) {
        log.error("Database error in LLM Gateway", exception);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(toErrorBody("service_unavailable", "LLM Gateway is temporarily unavailable"));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException exception) {
        log.warn("Service unavailable: {}", exception.getMessage(), exception);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(toErrorBody("service_unavailable", "Service temporarily unavailable"));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    ResponseEntity<Map<String, Object>> handleUnsupportedOperation(UnsupportedOperationException exception) {
        log.error("AI gateway operation not implemented in this deployment", exception);

        String message = exception.getMessage() != null
            ? exception.getMessage()
            : "Operation not implemented in this deployment";

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(toErrorBody("not_implemented", message));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleGenericException(Exception exception) {
        String errorId = UUID.randomUUID()
            .toString();

        MDC.put("errorId", errorId);

        try {
            log.error("Unexpected error in LLM Gateway API errorId={}", errorId, exception);
        } finally {
            MDC.remove("errorId");
        }

        Map<String, Object> errorFields = new LinkedHashMap<>();

        errorFields.put("message", "An internal error occurred");
        errorFields.put("type", "internal_error");
        errorFields.put("errorId", errorId);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", errorFields));
    }

    private static Map<String, Object> toErrorBody(String type, String message) {
        return Map.of(
            "error", Map.of(
                "message", message != null ? message : "Unknown error",
                "type", type));
    }
}
