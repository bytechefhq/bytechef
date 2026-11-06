/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.exception;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ScimBadRequestException extends RuntimeException {

    public static final String INVALID_SYNTAX = "invalidSyntax";
    public static final String INVALID_VALUE = "invalidValue";
    public static final String MUTABILITY = "mutability";
    public static final String NO_TARGET = "noTarget";

    private final String scimType;

    private ScimBadRequestException(String scimType, String detail) {
        super(detail);

        this.scimType = scimType;
    }

    public static ScimBadRequestException invalidSyntax(String detail) {
        return new ScimBadRequestException(INVALID_SYNTAX, detail);
    }

    public static ScimBadRequestException invalidValue(String detail) {
        return new ScimBadRequestException(INVALID_VALUE, detail);
    }

    public static ScimBadRequestException mutability(String detail) {
        return new ScimBadRequestException(MUTABILITY, detail);
    }

    public static ScimBadRequestException noTarget(String detail) {
        return new ScimBadRequestException(NO_TARGET, detail);
    }

    public String getScimType() {
        return scimType;
    }
}
