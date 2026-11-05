/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.facade;

import com.bytechef.ee.platform.user.scim.exception.ScimBadRequestException;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class ScimValues {

    static final int EXTERNAL_ID_MAX_LENGTH = 255;

    static final int GROUP_NAME_MAX_LENGTH = 50;

    static final int NAME_MAX_LENGTH = 50;

    private ScimValues() {
    }

    static @Nullable String checkMaxLength(String attributeName, @Nullable String value, int maxLength) {
        if (value != null && value.codePointCount(0, value.length()) > maxLength) {
            throw ScimBadRequestException.invalidValue(
                attributeName + " must not be longer than " + maxLength + " characters");
        }

        return value;
    }

    static String checkRequired(String attributeName, @Nullable String value) {
        if (value == null || value.isBlank()) {
            throw ScimBadRequestException.invalidValue(attributeName + " is required");
        }

        return value;
    }
}
