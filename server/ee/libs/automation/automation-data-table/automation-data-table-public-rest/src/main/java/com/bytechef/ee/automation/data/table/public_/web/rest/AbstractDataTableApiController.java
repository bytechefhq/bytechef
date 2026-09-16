/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.public_.web.rest;

import com.bytechef.exception.AbstractException;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.exception.DataTableStorageLimitExceededException;
import com.bytechef.web.rest.error.constant.ErrorConstants;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Error translation shared by the public data table controllers. Declared on the controller class rather than as an
 * advice because {@code GlobalResponseEntityExceptionHandler} is {@code HIGHEST_PRECEDENCE} and maps every
 * {@code AbstractException} to 400; Spring consults a controller's own {@code @ExceptionHandler}s before any advice.
 *
 * <p>
 * The body is the same {@code ProblemDetail} shape the global handler emits -- {@code type}, {@code title},
 * {@code errorKey}, {@code entityClass} -- so a consumer never sees two error dialects.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public abstract class AbstractDataTableApiController {

    private static final Set<Integer> NOT_FOUND_KEYS = Set.of(
        DataTableErrorType.DATA_TABLE_NOT_FOUND.getErrorKey(), DataTableErrorType.COLUMN_NOT_FOUND.getErrorKey(),
        DataTableErrorType.ROW_NOT_FOUND.getErrorKey());
    private static final Set<Integer> CONFLICT_KEYS = Set.of(
        DataTableErrorType.DATA_TABLE_ALREADY_EXISTS.getErrorKey(),
        DataTableErrorType.COLUMN_ALREADY_EXISTS.getErrorKey(),
        DataTableErrorType.ROW_EXTERNAL_ID_CONFLICT.getErrorKey());

    /**
     * Declared for the two concrete types rather than for {@code AbstractException}, and never rethrows. A handler that
     * throws is logged by {@code ExceptionHandlerExceptionResolver} and yields null; because that resolver has already
     * been selected, the global advice in it is NOT retried, so a rethrow here would surface as a raw 500. An exception
     * carrying some other entity class therefore gets the same 400 body the global advice would have produced, built
     * from its own entity class and error key.
     */
    @ExceptionHandler({
        DataTableException.class, ExecutionException.class
    })
    public ResponseEntity<ProblemDetail> handleDataTableException(
        AbstractException exception, HttpServletRequest request) {

        if (exception.getEntityClass() != DataTableErrorType.class) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                    problemDetail(
                        HttpStatus.BAD_REQUEST, exception.getMessage(), exception.getErrorKey(),
                        exception.getErrorMessageCode(), exception.getEntityClass()));
        }

        HttpStatus status = HttpStatus.BAD_REQUEST;

        if (NOT_FOUND_KEYS.contains(exception.getErrorKey())) {
            status = HttpStatus.NOT_FOUND;
        } else if (CONFLICT_KEYS.contains(exception.getErrorKey())) {
            status = HttpStatus.CONFLICT;
        }

        return ResponseEntity.status(status)
            .body(
                problemDetail(
                    status, exception.getMessage(), exception.getErrorKey(), exception.getErrorMessageCode(),
                    DataTableErrorType.class));
    }

    @ExceptionHandler(DataTableStorageLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleStorageLimit(
        DataTableStorageLimitExceededException exception, HttpServletRequest request) {

        DataTableErrorType errorType = DataTableErrorType.STORAGE_LIMIT_EXCEEDED;

        return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE)
            .body(
                problemDetail(
                    HttpStatus.INSUFFICIENT_STORAGE, exception.getMessage(), errorType.getErrorKey(),
                    "error.dataTableErrorType." + errorType.getErrorKey(), DataTableErrorType.class));
    }

    /**
     * Names are guessable, so on item URLs a denial answers exactly like a missing table: a caller learns nothing about
     * tables in workspaces they cannot see. Under /workspaces the caller already named the workspace, so a plain 403
     * leaks nothing and is the more useful answer.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(
        AccessDeniedException exception, HttpServletRequest request) {

        String requestUri = request.getRequestURI();

        if (requestUri.contains("/data-tables/")) {
            DataTableErrorType errorType = DataTableErrorType.DATA_TABLE_NOT_FOUND;

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                    problemDetail(
                        HttpStatus.NOT_FOUND, "Data table not found", errorType.getErrorKey(),
                        "error.dataTableErrorType." + errorType.getErrorKey(), DataTableErrorType.class));
        }

        throw exception;
    }

    private static ProblemDetail problemDetail(
        HttpStatus status, String detail, int errorKey, String errorMessageCode, Class<?> entityClass) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);

        problemDetail.setTitle("Error");
        problemDetail.setType(URI.create(ErrorConstants.PROBLEM_BASE_URL + "/" + errorMessageCode));
        problemDetail.setProperty("entityClass", entityClass.getSimpleName());
        problemDetail.setProperty("errorKey", errorKey);

        return problemDetail;
    }
}
