/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.user.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.user.exception.InvalidPasswordException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Ivica Cardic
 */
class PasswordValidatorTest {

    @Test
    void testValidateWithValidPassword() {
        // Password with 8+ chars, uppercase, and digit
        assertDoesNotThrow(() -> PasswordValidator.validate("Password1"));
        assertDoesNotThrow(() -> PasswordValidator.validate("MySecret123"));
        assertDoesNotThrow(() -> PasswordValidator.validate("A1bcdefgh"));
        assertDoesNotThrow(() -> PasswordValidator.validate("ABCDEFG1abcdefg"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testValidateWithNullOrEmptyPassword(String password) {
        assertThrows(InvalidPasswordException.class, () -> PasswordValidator.validate(password));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Pass1", "Ab1", "A1"
    })
    void testValidateWithPasswordTooShort(String password) {
        assertThrows(InvalidPasswordException.class, () -> PasswordValidator.validate(password));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "password123", "alllowercase1", "no_uppercase_9"
    })
    void testValidateWithPasswordMissingUppercase(String password) {
        assertThrows(InvalidPasswordException.class, () -> PasswordValidator.validate(password));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "PasswordABC", "ALLUPPERCASE", "NoDigitsHere"
    })
    void testValidateWithPasswordMissingDigit(String password) {
        assertThrows(InvalidPasswordException.class, () -> PasswordValidator.validate(password));
    }

    @Test
    void testIsInvalidWithValidPassword() {
        assertFalse(PasswordValidator.isInvalid("Password1"));
        assertFalse(PasswordValidator.isInvalid("MySecret123"));
        assertFalse(PasswordValidator.isInvalid("A1bcdefgh"));
    }

    @Test
    void testIsInvalidWithInvalidPassword() {
        // Null
        assertTrue(PasswordValidator.isInvalid(null));

        // Empty
        assertTrue(PasswordValidator.isInvalid(""));

        // Too short
        assertTrue(PasswordValidator.isInvalid("Pass1"));

        // Missing uppercase
        assertTrue(PasswordValidator.isInvalid("password123"));

        // Missing digit
        assertTrue(PasswordValidator.isInvalid("PasswordABC"));
    }

    @Test
    void testPasswordConstants() {
        assertTrue(PasswordValidator.PASSWORD_MIN_LENGTH == 8);
        assertTrue(PasswordValidator.PASSWORD_MAX_LENGTH == 100);
    }
}
