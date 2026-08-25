/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.variable.validator;

import com.bytechef.ee.platform.variable.exception.VariableErrorType;
import com.bytechef.exception.ConfigurationException;
import java.util.regex.Pattern;

/**
 * Static, unconditional validation of a variable's name and value -- deliberately not a Spring bean so it cannot be
 * silently disabled by a conditional; invoked by {@code VariableServiceImpl} on every create/update.
 *
 * <p>
 * Names are identifiers ({@code ^[A-Za-z_][A-Za-z0-9_]{0,49}$}): a leading digit is rejected because
 * {@code ${vars.1abc}} is not a valid SpEL property path. Values must be non-null and are capped at
 * {@link #MAX_VALUE_LENGTH} characters.
 *
 * @version ee
 */
public final class VariableNameValidator {

    public static final int MAX_NAME_LENGTH = 50;
    public static final int MAX_VALUE_LENGTH = 4096;

    private static final Pattern NAME_PATTERN =
        Pattern.compile("^[A-Za-z_][A-Za-z0-9_]{0," + (MAX_NAME_LENGTH - 1) + "}$");

    private VariableNameValidator() {
    }

    public static void validate(String name, String value) {
        if (name == null || !NAME_PATTERN.matcher(name)
            .matches()) {

            throw new ConfigurationException(
                "Variable name must match [A-Za-z_][A-Za-z0-9_]* and be at most " + MAX_NAME_LENGTH +
                    " characters: '" + name + "'",
                VariableErrorType.VARIABLE_NAME_INVALID);
        }

        if (value == null) {
            throw new ConfigurationException(
                "Variable value must not be null", VariableErrorType.VARIABLE_VALUE_REQUIRED);
        }

        if (value.length() > MAX_VALUE_LENGTH) {
            throw new ConfigurationException(
                "Variable value must be at most " + MAX_VALUE_LENGTH + " characters",
                VariableErrorType.VARIABLE_VALUE_TOO_LONG);
        }
    }
}
