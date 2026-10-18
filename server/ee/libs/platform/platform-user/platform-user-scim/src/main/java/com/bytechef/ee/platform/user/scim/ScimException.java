/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim;

import org.springframework.http.HttpStatus;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ScimException extends RuntimeException {

    private final String scimType;
    private final HttpStatus status;

    ScimException(HttpStatus status, String scimType, String detail) {
        super(detail);

        this.scimType = scimType;
        this.status = status;
    }

    public static ScimException invalidFilter(String detail) {
        return new ScimException(HttpStatus.BAD_REQUEST, "invalidFilter", detail);
    }

    public static ScimException invalidPath(String detail) {
        return new ScimException(HttpStatus.BAD_REQUEST, "invalidPath", detail);
    }

    public static ScimException invalidSyntax(String detail) {
        return new ScimException(HttpStatus.BAD_REQUEST, "invalidSyntax", detail);
    }

    public static ScimException invalidValue(String detail) {
        return new ScimException(HttpStatus.BAD_REQUEST, "invalidValue", detail);
    }

    public static ScimException mutability(String detail) {
        return new ScimException(HttpStatus.BAD_REQUEST, "mutability", detail);
    }

    public static ScimException notFound(String detail) {
        return new ScimException(HttpStatus.NOT_FOUND, null, detail);
    }

    public static ScimException notImplemented(String detail) {
        return new ScimException(HttpStatus.NOT_IMPLEMENTED, null, detail);
    }

    public static ScimException uniqueness(String detail) {
        return new ScimException(HttpStatus.CONFLICT, "uniqueness", detail);
    }

    public String getScimType() {
        return scimType;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
