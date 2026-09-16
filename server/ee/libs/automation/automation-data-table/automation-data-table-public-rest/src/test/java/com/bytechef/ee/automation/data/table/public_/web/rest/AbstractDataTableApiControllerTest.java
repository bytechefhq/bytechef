/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.public_.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.exception.DataTableStorageLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AbstractDataTableApiControllerTest {

    private final AbstractDataTableApiController controller = new AbstractDataTableApiController() {};

    @Test
    void testDataTableExceptionsMapByKey() {
        assertEquals(HttpStatus.NOT_FOUND, controller.handleDataTableException(
            new DataTableException("x", DataTableErrorType.ROW_NOT_FOUND), request("/api/automation/v1/data-tables/t"))
            .getStatusCode());
        assertEquals(HttpStatus.CONFLICT, controller.handleDataTableException(
            new DataTableException("x", DataTableErrorType.ROW_EXTERNAL_ID_CONFLICT), request("/x"))
            .getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, controller.handleDataTableException(
            new DataTableException("x", DataTableErrorType.FILTER_INVALID), request("/x"))
            .getStatusCode());

        ProblemDetail body = Objects.requireNonNull(controller.handleDataTableException(
            new DataTableException("x", DataTableErrorType.FILTER_INVALID), request("/x"))
            .getBody());
        Map<String, Object> properties = Objects.requireNonNull(body.getProperties());

        assertEquals(112, properties.get("errorKey"));
        assertEquals("DataTableErrorType", properties.get("entityClass"));
    }

    /**
     * {@code NOT_FOUND_KEYS} and {@code CONFLICT_KEYS} name six constants between them, but the other tests in this
     * class only ever drive {@code ROW_NOT_FOUND} and {@code ROW_EXTERNAL_ID_CONFLICT}. A mistyped constant in either
     * set would map to 400 instead of 404/409 with nothing else here failing, so every remaining member of both sets
     * gets its own assertion, on status and on the {@code errorKey} property.
     */
    @Test
    void testEveryRemainingNotFoundAndConflictKeyMapsToItsStatus() {
        assertMapsTo(DataTableErrorType.COLUMN_NOT_FOUND, HttpStatus.NOT_FOUND);
        assertMapsTo(DataTableErrorType.DATA_TABLE_ALREADY_EXISTS, HttpStatus.CONFLICT);
        assertMapsTo(DataTableErrorType.COLUMN_ALREADY_EXISTS, HttpStatus.CONFLICT);
    }

    @Test
    void testExecutionExceptionWithADataTableKeyIsHandledTheSameWay() {
        assertEquals(HttpStatus.NOT_FOUND, controller.handleDataTableException(
            new ExecutionException("x", DataTableErrorType.DATA_TABLE_NOT_FOUND), request("/x"))
            .getStatusCode());
    }

    @Test
    void testStorageLimitIs507() {
        assertEquals(HttpStatus.INSUFFICIENT_STORAGE, controller.handleStorageLimit(
            new DataTableStorageLimitExceededException(10, 5), request("/x"))
            .getStatusCode());
    }

    @Test
    void testAccessDeniedIs404OnItemUrlsAndRethrownOnWorkspaceUrls() {
        assertEquals(HttpStatus.NOT_FOUND, controller.handleAccessDenied(
            new AccessDeniedException("x"), request("/api/automation/v1/data-tables/orders"))
            .getStatusCode());

        // On a workspace URL the handler rethrows: nothing in a plain unit-test call resolves it further, but at
        // runtime the rethrow escapes ExceptionHandlerExceptionResolver and is translated to 403 by Spring
        // Security's ExceptionTranslationFilter -- so the meaningful thing this test can prove is that the
        // handler does not swallow or otherwise map the exception on a non-item URL.
        assertThrows(AccessDeniedException.class, () -> controller.handleAccessDenied(
            new AccessDeniedException("x"), request("/api/automation/v1/workspaces/1/data-tables")));
    }

    private void assertMapsTo(DataTableErrorType errorType, HttpStatus expectedStatus) {
        ResponseEntity<ProblemDetail> responseEntity = controller.handleDataTableException(
            new DataTableException("x", errorType), request("/x"));

        assertEquals(expectedStatus, responseEntity.getStatusCode());

        ProblemDetail body = Objects.requireNonNull(responseEntity.getBody());
        Map<String, Object> properties = Objects.requireNonNull(body.getProperties());

        assertEquals(errorType.getErrorKey(), properties.get("errorKey"));
    }

    private static HttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);

        return request;
    }
}
