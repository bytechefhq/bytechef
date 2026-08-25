/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.variable.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import com.bytechef.ee.platform.variable.exception.VariableErrorType;
import com.bytechef.exception.ConfigurationException;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class VariableNameValidatorTest {

    @Test
    void testValidNamesPass() {
        assertThatCode(() -> VariableNameValidator.validate("API_URL", "x")).doesNotThrowAnyException();
        assertThatCode(() -> VariableNameValidator.validate("_private", "x")).doesNotThrowAnyException();
        assertThatCode(() -> VariableNameValidator.validate("a".repeat(50), "x")).doesNotThrowAnyException();
        assertThatCode(() -> VariableNameValidator.validate("A", "")).doesNotThrowAnyException();
    }

    @Test
    void testInvalidNamesAreRejected() {
        for (String name : new String[] {
            "", " ", "1abc", "api-url", "api url", "naïve", "a".repeat(51)
        }) {
            assertThatThrownBy(() -> VariableNameValidator.validate(name, "x"))
                .asInstanceOf(type(ConfigurationException.class))
                .extracting(ConfigurationException::getErrorKey)
                .isEqualTo(VariableErrorType.VARIABLE_NAME_INVALID.getErrorKey());
        }
    }

    @Test
    void testTooLongValueIsRejected() {
        assertThatThrownBy(() -> VariableNameValidator.validate("OK", "v".repeat(4097)))
            .asInstanceOf(type(ConfigurationException.class))
            .extracting(ConfigurationException::getErrorKey)
            .isEqualTo(VariableErrorType.VARIABLE_VALUE_TOO_LONG.getErrorKey());
        assertThatCode(() -> VariableNameValidator.validate("OK", "v".repeat(4096))).doesNotThrowAnyException();
    }

    @Test
    void testNullValueIsRejected() {
        assertThatThrownBy(() -> VariableNameValidator.validate("OK", null))
            .asInstanceOf(type(ConfigurationException.class))
            .extracting(ConfigurationException::getErrorKey)
            .isEqualTo(VariableErrorType.VARIABLE_VALUE_REQUIRED.getErrorKey());
    }
}
