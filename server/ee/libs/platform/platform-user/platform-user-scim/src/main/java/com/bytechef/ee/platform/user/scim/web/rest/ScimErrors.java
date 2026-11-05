/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.rest;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class ScimErrors {

    static final String ERROR_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:Error";
    static final MediaType SCIM_JSON = MediaType.parseMediaType("application/scim+json");
    static final String INVALID_FILTER = "invalidFilter";
    static final String UNIQUENESS = "uniqueness";

    private ScimErrors() {
    }

    static ResponseEntity<Object> invalidFilter(String filter) {
        return error(HttpStatus.BAD_REQUEST, INVALID_FILTER, "Unsupported filter expression: " + filter);
    }

    static ResponseEntity<Object> uniqueness(String detail) {
        return error(HttpStatus.CONFLICT, UNIQUENESS, detail);
    }

    private static ResponseEntity<Object> error(HttpStatus httpStatus, String scimType, String detail) {
        return ResponseEntity.status(httpStatus)
            .contentType(SCIM_JSON)
            .body(
                Map.of(
                    "schemas", List.of(ERROR_SCHEMA),
                    "scimType", scimType,
                    "detail", detail,
                    "status", String.valueOf(httpStatus.value())));
    }
}
