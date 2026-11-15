/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.rest;

import com.bytechef.ee.platform.user.scim.ScimConstants;
import com.bytechef.ee.platform.user.scim.exception.ScimBadRequestException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class ScimErrors {

    static final MediaType SCIM_JSON = MediaType.parseMediaType("application/scim+json");
    static final String INVALID_FILTER = "invalidFilter";
    static final String UNIQUENESS = "uniqueness";

    private ScimErrors() {
    }

    static ResponseEntity<Object> badRequest(ScimBadRequestException scimBadRequestException) {
        return error(
            HttpStatus.BAD_REQUEST, scimBadRequestException.getScimType(), scimBadRequestException.getMessage());
    }

    static ResponseEntity<Object> invalidFilter(String filter) {
        return error(HttpStatus.BAD_REQUEST, INVALID_FILTER, "Unsupported filter expression: " + filter);
    }

    static ResponseEntity<Object> uniqueness(String detail) {
        return error(HttpStatus.CONFLICT, UNIQUENESS, detail);
    }

    static ResponseEntity<Object> error(HttpStatus httpStatus, @Nullable String scimType, String detail) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("schemas", List.of(ScimConstants.ERROR_SCHEMA));

        if (scimType != null) {
            body.put("scimType", scimType);
        }

        body.put("detail", detail);
        body.put("status", String.valueOf(httpStatus.value()));

        return ResponseEntity.status(httpStatus)
            .contentType(SCIM_JSON)
            .body(body);
    }
}
