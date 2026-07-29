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
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreTargetNotFoundException;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreWorkspaceBoundaryException;
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
 * @author Ivica Cardic
 * @version ee
 */
@RestControllerAdvice(basePackages = {
    "com.bytechef.ee.automation.ai.gateway",
    "com.bytechef.ee.automation.ai.eval"
})
public class AiGatewayExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayExceptionHandler.class);

    /**
     * Catch-all 400 handler for validation failures surfaced as plain {@link IllegalArgumentException}. The exception
     * message is echoed to the client and the full throwable is logged at warn server-side.
     *
     * <p>
     * Convention: do NOT throw {@link IllegalArgumentException} from facades / services with messages that contain
     * internal entity ids, workspace ids, or any cross-tenant identifying tokens. Such messages would leak via this
     * handler. Sensitive paths (e.g. cross-workspace provider ownership) catch the IAE inline at the call site and
     * convert it to a structured non-leaking response (see
     * {@code WorkspaceAiGatewayProviderFacadeImpl.testWorkspaceProviderConnection}). New client-facing validation
     * should prefer {@link BadRequestException} (with explicit {@code safeClientMessage}) over plain IAE so the
     * client-facing string is reviewer-controlled rather than convention-only.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException exception) {
        log.warn("Bad request", exception);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(toErrorBody("invalid_request_error", exception.getMessage()));
    }

    /**
     * Structured 400 handler — the only message exposed to the client is
     * {@link BadRequestException#getSafeClientMessage()}, keeping any caller-content / internal id / column-name detail
     * server-side in the diagnostic message. New client-facing validation should throw {@link BadRequestException}
     * rather than plain {@link IllegalArgumentException}: the safe-message split forces a deliberate review of what
     * crosses the wire.
     */
    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<Map<String, Object>> handleStructuredBadRequest(BadRequestException exception) {
        log.warn("Bad request: {}", exception.getMessage(), exception);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(toErrorBody("invalid_request_error", exception.getSafeClientMessage()));
    }

    /**
     * Mapped to HTTP 429 (Too Many Requests). Without this dedicated handler the exception falls through to the generic
     * {@link #handleGenericException} 500 path and clients retry on the convention "500 = transient, retry" —
     * amplifying the very burst the limiter is designed to dampen. The {@code Retry-After} header carries the limiter's
     * window reset (in seconds, per the HTTP spec) so well-behaved clients can back off precisely.
     */
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
            // HTTP Retry-After is delta-seconds (RFC 7231 §7.1.3). Floor to 1s when the window has technically
            // already elapsed by the time the response is built, so clients still observe a non-zero hint.
            long retryAfterSeconds = Math.max(1L, (resetAtEpochMs - Instant.now()
                .toEpochMilli()) / 1000L);

            responseBuilder.header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        }

        return responseBuilder.body(Map.of("error", errorFields));
    }

    /**
     * Mapped to HTTP 422 (Unprocessable Entity): the request is well-formed but was refused by a configured content
     * guardrail (a blocked term). Distinct from a 400 malformed-request and from upstream errors — the client should
     * revise the prompt, not retry. The exception message is guardrail-authored and names neither the offending content
     * nor the matched term, so it is safe to echo.
     */
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

        // HTTP 402 Payment Required: a budget hard-limit is a payment-boundary signal to clients, distinct from
        // rate limiting (429). Clients should surface a billing/top-up flow, not a retry.
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
            .body(Map.of("error", errorFields));
    }

    @ExceptionHandler(AiScoreWorkspaceBoundaryException.class)
    ResponseEntity<Map<String, Object>> handleWorkspaceBoundary(AiScoreWorkspaceBoundaryException exception) {
        // Logged at error severity WITH the exception object (not just the message) so security ops dashboards
        // see attempted tenant-isolation breaches with the full stack — the controller/path that was breached
        // appears in the trace, which a message-only log silently elides. Warn-level routinely escapes notice.
        // The principal is not currently wired here — surface that as a follow-up if SecurityContextHolder
        // access becomes available without breaking the current dependency direction.
        log.error("Cross-workspace score write rejected: {}", exception.getMessage(), exception);

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(toErrorBody("workspace_boundary", exception.getMessage()));
    }

    @ExceptionHandler(AiScoreTargetNotFoundException.class)
    ResponseEntity<Map<String, Object>> handleScoreTargetNotFound(AiScoreTargetNotFoundException exception) {
        // Pass the exception object (not just its message) so the carefully-preserved IllegalArgumentException
        // cause from the facade's loadTrace/loadSpan reaches logs. Without it, a validation bug masquerading as
        // a 404 would leave operators chasing ghost "missing target" tickets when the real failure was a
        // malformed id.
        //
        // Default level is INFO — clients passing stale ids is normal — but bumped to WARN when the cause is a
        // non-empty IllegalArgumentException, the shape that signals a real upstream validation bug rather than
        // a routine missing row. A flood of stale-id 404s without that signal would otherwise drown the
        // distinguishable validation-bug case in noise.
        Throwable cause = exception.getCause();
        boolean validationCause = cause instanceof IllegalArgumentException
            && cause.getMessage() != null
            && !cause.getMessage()
                .isBlank();

        if (validationCause) {
            log.warn("Score target not found (validation cause): {}", exception.getMessage(), exception);
        } else {
            log.info("Score target not found: {}", exception.getMessage(), exception);
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(toErrorBody("target_not_found", exception.getMessage()));
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

    /**
     * 503 handler — the wire body is fixed-shape ("Service temporarily unavailable") regardless of the exception
     * message. The original detail is logged server-side at warn and reaches operators via that path. Echoing
     * {@code exception.getMessage()} would leak internal context: the OTLP ingest facade's trace-race rethrow at
     * {@code AiObservabilityOtlpIngestFacadeImpl.resolveOrCreateTrace} carries the column-pair
     * {@code (workspace_id, external_trace_id)} in its message — schema-shape information a probing caller could use to
     * reason about table layout. The retry-handler's "All deployments failed after retries" is innocuous, but the shape
     * contract here is "no IllegalStateException can leak", not "operator-curated messages can leak".
     */
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException exception) {
        log.warn("Service unavailable: {}", exception.getMessage(), exception);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(toErrorBody("service_unavailable", "Service temporarily unavailable"));
    }

    /**
     * Mapped to HTTP 501 (not 400) and logged at error severity. Surfaces gateway capabilities that the running
     * deployment cannot serve — typically a provider feature gate (e.g. embeddings against a chat-only provider) or an
     * endpoint reached against an app variant that does not have the backing service wired. The user sees "not
     * implemented" rather than "bad request"; the operator gets the stack trace via the error log so the
     * misconfiguration is actionable instead of silent.
     */
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
        // Correlation id is the bridge between the user's "I got an internal error at 14:32" support ticket
        // and the server-side stack trace. Without it, operators have no way to find the matching log line.
        // UUID is fine — pseudo-randomness suffices for log correlation, and the server log carries the full
        // stack so the body needs only the id, not any leak-prone exception detail.
        String errorId = UUID.randomUUID()
            .toString();

        // Promote errorId into MDC so structured-log indexers expose it as a queryable field rather than
        // burying it in the message body. Without this, a Kibana / Loki query for `errorId=abc-123` from a
        // user support ticket misses the matching server-side log unless operators know to grep the
        // message text. The try/finally ensures MDC stays clean even if log.error itself throws (a
        // misconfigured appender can do that), so subsequent unrelated requests on this thread are not
        // tagged with a stale errorId.
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
