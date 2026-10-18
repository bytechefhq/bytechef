/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.rest;

import org.springframework.http.HttpStatus;

/**
 * A SCIM protocol error (RFC 7644 section 3.12), rendered by {@link ScimExceptionHandler} as a SCIM Error response.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ScimException extends RuntimeException {

    private final String scimType;
    private final HttpStatus status;

    ScimException(HttpStatus status, String scimType, String detail) {
        super(detail);

        this.scimType = scimType;
        this.status = status;
    }

    static ScimException invalidFilter(String detail) {
        return new ScimException(HttpStatus.BAD_REQUEST, "invalidFilter", detail);
    }

    static ScimException invalidPath(String detail) {
        return new ScimException(HttpStatus.BAD_REQUEST, "invalidPath", detail);
    }

    static ScimException invalidSyntax(String detail) {
        return new ScimException(HttpStatus.BAD_REQUEST, "invalidSyntax", detail);
    }

    static ScimException invalidValue(String detail) {
        return new ScimException(HttpStatus.BAD_REQUEST, "invalidValue", detail);
    }

    static ScimException mutability(String detail) {
        return new ScimException(HttpStatus.BAD_REQUEST, "mutability", detail);
    }

    static ScimException notFound(String detail) {
        return new ScimException(HttpStatus.NOT_FOUND, null, detail);
    }

    static ScimException notImplemented(String detail) {
        return new ScimException(HttpStatus.NOT_IMPLEMENTED, null, detail);
    }

    static ScimException uniqueness(String detail) {
        return new ScimException(HttpStatus.CONFLICT, "uniqueness", detail);
    }

    String getScimType() {
        return scimType;
    }

    HttpStatus getStatus() {
        return status;
    }
}
