/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.rest;

import com.bytechef.ee.platform.user.scim.ScimConstants;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private static final MediaType SCIM_JSON = MediaType.parseMediaType("application/scim+json");

    @ExceptionHandler(ScimException.class)
    ResponseEntity<Map<String, Object>> handleScimException(ScimException scimException) {
        return error(scimException.getStatus(), scimException.getScimType(), scimException.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException accessDeniedException) {
        return error(HttpStatus.FORBIDDEN, null, "SCIM API requires a SCIM bearer token");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException notReadableException) {
        return error(HttpStatus.BAD_REQUEST, "invalidSyntax", "Request body is not valid SCIM JSON");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException illegalArgumentException) {
        return error(HttpStatus.BAD_REQUEST, "invalidValue", illegalArgumentException.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException runtimeException) {
        log.error("SCIM request failed", runtimeException);

        return error(HttpStatus.INTERNAL_SERVER_ERROR, null, "Internal error");
    }

    static ResponseEntity<Map<String, Object>> error(HttpStatus status, String scimType, String detail) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("schemas", List.of(ScimConstants.ERROR_SCHEMA));

        if (scimType != null) {
            body.put("scimType", scimType);
        }

        body.put("detail", detail);
        body.put("status", String.valueOf(status.value()));

        return ResponseEntity.status(status)
            .contentType(SCIM_JSON)
            .body(body);
    }
}
