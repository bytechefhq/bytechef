/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.exception.ApiCollectionErrorType;
import com.bytechef.exception.AbstractException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Error translation shared by the API platform public API controllers: a status and a {@code {"code", "message"}} body.
 * Declared on the controller class rather than as an advice because {@code GlobalResponseEntityExceptionHandler} is
 * {@code HIGHEST_PRECEDENCE} and would answer {@code AccessDeniedException} with 500 and every
 * {@code AbstractException} with 400; Spring consults a controller's own {@code @ExceptionHandler}s before any advice.
 *
 * <p>
 * API collection errors are told apart from other {@link AbstractException}s by their entity class, and named by
 * {@link ApiCollectionErrorType#nameOf(int)}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public abstract class AbstractApiCollectionPublicApiController {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorBody> handleAccessDeniedException(AccessDeniedException accessDeniedException) {
        return toResponse(HttpStatus.FORBIDDEN, "ACCESS_DENIED", accessDeniedException.getMessage());
    }

    @ExceptionHandler(AbstractException.class)
    public ResponseEntity<ErrorBody> handleAbstractException(AbstractException abstractException) {
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

    private static ResponseEntity<ErrorBody> translate(AbstractException abstractException) {
        int errorKey = abstractException.getErrorKey();
        String message = abstractException.getMessage();

        String code = ApiCollection.class.equals(abstractException.getEntityClass())
            ? ApiCollectionErrorType.nameOf(errorKey) : null;

        if (code == null) {
            return toResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
        }

        return toResponse(toStatus(errorKey), code, message);
    }

    private static HttpStatus toStatus(int errorKey) {
        if (errorKey == ApiCollectionErrorType.API_COLLECTION_NOT_FOUND.getErrorKey() ||
            errorKey == ApiCollectionErrorType.WORKFLOW_NOT_FOUND.getErrorKey()) {

            return HttpStatus.NOT_FOUND;
        }

        if (errorKey == ApiCollectionErrorType.ENDPOINT_WORKFLOW_REMOVED.getErrorKey()) {
            return HttpStatus.CONFLICT;
        }

        return HttpStatus.BAD_REQUEST;
    }

    private static ResponseEntity<ErrorBody> toResponse(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
            .body(new ErrorBody(code, message));
    }

    public record ErrorBody(String code, String message) {
    }
}
