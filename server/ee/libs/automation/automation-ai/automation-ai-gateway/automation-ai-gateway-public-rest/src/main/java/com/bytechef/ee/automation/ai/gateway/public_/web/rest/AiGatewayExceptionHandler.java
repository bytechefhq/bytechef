/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.public_.web.rest;

import com.bytechef.ee.automation.ai.gateway.domain.BudgetExceededException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
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
 */
@RestControllerAdvice(basePackageClasses = AiGatewayChatCompletionApiController.class)
class AiGatewayExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(AiGatewayExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException exception) {
        logger.warn("Bad request: {}", exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(toErrorBody("invalid_request_error", exception.getMessage()));
    }

    @ExceptionHandler(BudgetExceededException.class)
    ResponseEntity<Map<String, Object>> handleBudgetExceeded(BudgetExceededException exception) {
        Map<String, Object> errorFields = new java.util.LinkedHashMap<>();

        errorFields.put("message", exception.getMessage() != null ? exception.getMessage() : "Budget exceeded");
        errorFields.put("type", "budget_exceeded");

        if (exception.getBudgetUsd() != null) {
            errorFields.put("budgetUsd", exception.getBudgetUsd());
        }

        if (exception.getSpentUsd() != null) {
            errorFields.put("spentUsd", exception.getSpentUsd());
        }

        // HTTP 402 Payment Required per spec §F8: a budget hard-limit is a payment-boundary signal to clients,
        // distinct from rate limiting (429). Clients should surface a billing/top-up flow, not a retry.
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
            .body(Map.of("error", errorFields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> handleMalformedRequest(HttpMessageNotReadableException exception) {
        logger.debug("Malformed request body", exception);

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
        logger.warn("Upstream LLM provider returned client error: {} {}", exception.getStatusCode(),
            exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(toErrorBody("upstream_error",
                "Upstream LLM provider returned error: " + exception.getStatusCode()));
    }

    @ExceptionHandler(HttpServerErrorException.class)
    ResponseEntity<Map<String, Object>> handleUpstreamServerError(HttpServerErrorException exception) {
        logger.error("Upstream LLM provider returned server error: {} {}", exception.getStatusCode(),
            exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(toErrorBody("upstream_error",
                "Upstream LLM provider is temporarily unavailable"));
    }

    @ExceptionHandler(ResourceAccessException.class)
    ResponseEntity<Map<String, Object>> handleNetworkTimeout(ResourceAccessException exception) {
        logger.error("Network error communicating with upstream LLM provider", exception);

        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
            .body(toErrorBody("timeout_error",
                "Timed out communicating with upstream LLM provider"));
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Map<String, Object>> handleDatabaseError(DataAccessException exception) {
        logger.error("Database error in LLM Gateway", exception);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(toErrorBody("service_unavailable", "LLM Gateway is temporarily unavailable"));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException exception) {
        logger.warn("Service unavailable: {}", exception.getMessage());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(toErrorBody("service_unavailable", exception.getMessage()));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    ResponseEntity<Map<String, Object>> handleUnsupportedOperation(UnsupportedOperationException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(toErrorBody("unsupported_operation", exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleGenericException(Exception exception) {
        logger.error("Unexpected error in LLM Gateway API", exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(toErrorBody("internal_error", "An internal error occurred"));
    }

    private static Map<String, Object> toErrorBody(String type, String message) {
        return Map.of(
            "error", Map.of(
                "message", message != null ? message : "Unknown error",
                "type", type));
    }
}
