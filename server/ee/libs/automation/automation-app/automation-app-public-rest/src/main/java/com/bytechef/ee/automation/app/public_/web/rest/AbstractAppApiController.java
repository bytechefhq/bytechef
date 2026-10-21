/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import com.bytechef.component.exception.ProviderException;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.exception.AbstractException;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.exception.ExecutionException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Error translation shared by the App REST controllers: a status and a {@code {"code", "message"}} body. Declared on
 * the controller class rather than as an advice because {@code GlobalResponseEntityExceptionHandler} is
 * {@code HIGHEST_PRECEDENCE} and would answer {@code AccessDeniedException} with 500 and every
 * {@code AbstractException} with 400; Spring consults a controller's own {@code @ExceptionHandler}s before any advice.
 * No handler here rethrows, since a handler that throws is not retried against the global advice.
 *
 * <p>
 * A component's own failure (an {@link ExecutionException} from a perform or a tool, or any other
 * {@link AbstractException} that is not an App error) answers 422 {@code COMPONENT_ERROR} with the exception message
 * only: the input parameters an {@link AbstractException} carries are never part of the response. When that exception's
 * cause chain holds a {@link ProviderException}, it answers 422 {@code PROVIDER_ERROR} with the provider exception's
 * message instead. App errors are decided first.
 *
 * <p>
 * App errors are told apart from other {@link ConfigurationException}s by their entity class, and named by
 * {@link AppErrorType#nameOf(int)}. An App error answered with a 5xx is logged at ERROR with its cause.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public abstract class AbstractAppApiController {

    /**
     * Marks a response as ByteChef's own app-level error, so a client can tell it from a status a workflow chose.
     */
    public static final String APP_ERROR_HEADER = AppErrorType.APP_ERROR_HEADER;

    private static final Logger log = LoggerFactory.getLogger(AbstractAppApiController.class);

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorBody> handleAccessDeniedException(AccessDeniedException accessDeniedException) {
        return toResponse(HttpStatus.FORBIDDEN, "ACCESS_DENIED", accessDeniedException.getMessage());
    }

    @ExceptionHandler(ConfigurationException.class)
    public ResponseEntity<ErrorBody> handleConfigurationException(ConfigurationException configurationException) {
        return translate(configurationException);
    }

    @ExceptionHandler({
        ExecutionException.class, AbstractException.class
    })
    public ResponseEntity<ErrorBody> handleExecutionException(AbstractException abstractException) {
        return translate(abstractException);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorBody> handleIllegalArgumentException(IllegalArgumentException illegalArgumentException) {
        return toResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", illegalArgumentException.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException methodArgumentNotValidException) {

        FieldError fieldError = methodArgumentNotValidException.getFieldError();

        String message = fieldError == null
            ? "The request body is invalid" : fieldError.getField() + " " + fieldError.getDefaultMessage();

        return toResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
    }

    @ExceptionHandler(ProviderException.class)
    public ResponseEntity<ErrorBody> handleProviderException(ProviderException providerException) {
        return toResponse(HttpStatus.UNPROCESSABLE_ENTITY, "PROVIDER_ERROR", providerException.getMessage());
    }

    static ResponseEntity<ErrorBody> translate(AbstractException abstractException) {
        Class<?> entityClass = abstractException.getEntityClass();
        int errorKey = abstractException.getErrorKey();
        String message = abstractException.getMessage();

        if (App.class.equals(entityClass)) {
            String code = AppErrorType.nameOf(errorKey);
            HttpStatus status = toAppStatus(errorKey);

            if (code == null) {
                code = "APP_ERROR";
            }

            if (status.is5xxServerError()) {
                log.error("App request answered {} {}", status.value(), code, abstractException);
            }

            return toResponse(status, code, message);
        }

        ProviderException providerException = findProviderException(abstractException);

        if (providerException != null) {
            return toResponse(HttpStatus.UNPROCESSABLE_ENTITY, "PROVIDER_ERROR", providerException.getMessage());
        }

        return toResponse(HttpStatus.UNPROCESSABLE_ENTITY, "COMPONENT_ERROR", message);
    }

    private static @Nullable ProviderException findProviderException(Throwable throwable) {
        Set<Throwable> visitedThrowables = Collections.newSetFromMap(new IdentityHashMap<>());

        for (Throwable cause = throwable.getCause(); cause != null && visitedThrowables.add(cause); cause =
            cause.getCause()) {

            if (cause instanceof ProviderException providerException) {
                return providerException;
            }
        }

        return null;
    }

    private static HttpStatus toAppStatus(int errorKey) {
        if (errorKey == AppErrorType.APP_NOT_FOUND.getErrorKey() ||
            errorKey == AppErrorType.APP_CONNECTION_NOT_FOUND.getErrorKey() ||
            errorKey == AppErrorType.APP_WORKFLOW_NOT_FOUND.getErrorKey() ||
            errorKey == AppErrorType.APP_WORKFLOW_RUN_NOT_FOUND.getErrorKey() ||
            errorKey == AppErrorType.APP_API_COLLECTION_NOT_FOUND.getErrorKey() ||
            errorKey == AppErrorType.APP_API_ENDPOINT_NOT_FOUND.getErrorKey()) {

            return HttpStatus.NOT_FOUND;
        }

        if (errorKey == AppErrorType.APP_DISABLED.getErrorKey() ||
            errorKey == AppErrorType.APP_OPERATION_NOT_ALLOWED.getErrorKey()) {

            return HttpStatus.FORBIDDEN;
        }

        if (errorKey == AppErrorType.APP_CONNECTION_UNAVAILABLE.getErrorKey() ||
            errorKey == AppErrorType.APP_PROXY_UNSUPPORTED.getErrorKey() ||
            errorKey == AppErrorType.APP_BINDING_KEY_ALREADY_EXISTS.getErrorKey() ||
            errorKey == AppErrorType.APP_WORKFLOW_UNAVAILABLE.getErrorKey() ||
            errorKey == AppErrorType.APP_API_COLLECTION_UNAVAILABLE.getErrorKey()) {

            return HttpStatus.CONFLICT;
        }

        if (errorKey == AppErrorType.APP_EXECUTION_TIMEOUT.getErrorKey()) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }

        if (errorKey == AppErrorType.APP_PROXY_REQUEST_TOO_LARGE.getErrorKey()) {
            return HttpStatus.CONTENT_TOO_LARGE;
        }

        if (errorKey == AppErrorType.APP_PROXY_RESPONSE_TOO_LARGE.getErrorKey() ||
            errorKey == AppErrorType.APP_PROXY_UPSTREAM_UNAVAILABLE.getErrorKey()) {
            return HttpStatus.BAD_GATEWAY;
        }

        if (errorKey == AppErrorType.APP_PROXY_STORAGE_FAILED.getErrorKey() ||
            errorKey == AppErrorType.APP_EXECUTION_FAILED.getErrorKey()) {

            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return HttpStatus.BAD_REQUEST;
    }

    static ResponseEntity<ErrorBody> toResponse(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
            .header(APP_ERROR_HEADER, "true")
            .body(new ErrorBody(code, message));
    }

    public record ErrorBody(String code, String message) {
    }
}
