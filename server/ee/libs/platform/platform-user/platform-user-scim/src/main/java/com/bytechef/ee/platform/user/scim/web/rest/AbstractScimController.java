/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.rest;

import com.bytechef.ee.platform.user.scim.exception.ScimBadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Base of the SCIM controllers: renders every failure as a SCIM Error response, which identity providers parse to
 * decide whether to retry. The handlers live on the controllers rather than in a controller advice because the
 * application's global advice is ordered first and would otherwise answer with its own error body.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
abstract class AbstractScimController {

    private static final Logger log = LoggerFactory.getLogger(AbstractScimController.class);

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Object> handleAccessDeniedException() {
        return ScimErrors.error(HttpStatus.FORBIDDEN, null, "SCIM API requires a SCIM bearer token");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Object> handleHttpMessageNotReadableException() {
        return ScimErrors.error(
            HttpStatus.BAD_REQUEST, ScimBadRequestException.INVALID_SYNTAX, "Request body is not valid SCIM JSON");
    }

    @ExceptionHandler(ScimBadRequestException.class)
    ResponseEntity<Object> handleScimBadRequestException(ScimBadRequestException scimBadRequestException) {
        return ScimErrors.badRequest(scimBadRequestException);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException illegalArgumentException) {
        return ScimErrors.error(
            HttpStatus.BAD_REQUEST, ScimBadRequestException.INVALID_VALUE, illegalArgumentException.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<Object> handleRuntimeException(RuntimeException runtimeException) {
        log.error("SCIM request failed", runtimeException);

        return ScimErrors.error(HttpStatus.INTERNAL_SERVER_ERROR, null, "Internal error");
    }
}
