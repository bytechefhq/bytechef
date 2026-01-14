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

import com.bytechef.platform.user.exception.InvalidPasswordException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for password validation.
 *
 * Password requirements: - Minimum 8 characters - Maximum 100 characters - At least 1 uppercase letter - At least 1
 * digit
 *
 * @author Ivica Cardic
 */
public final class PasswordValidator {

    public static final int PASSWORD_MIN_LENGTH = 8;

    public static final int PASSWORD_MAX_LENGTH = 100;

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");

    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");

    private PasswordValidator() {
    }

    /**
     * Validates the password against the defined requirements.
     *
     * @param password the password to validate
     * @throws InvalidPasswordException if the password does not meet requirements
     */
    public static void validate(String password) {
        if (isInvalid(password)) {
            throw new InvalidPasswordException();
        }
    }

    /**
     * Checks if the password is invalid.
     *
     * @param password the password to check
     * @return true if the password is invalid, false otherwise
     */
    public static boolean isInvalid(String password) {
        if (password == null || password.isEmpty()) {
            return true;
        }

        if (password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH) {
            return true;
        }

        Matcher matcher = UPPERCASE_PATTERN.matcher(password);

        if (!matcher.find()) {
            return true;
        }

        return !DIGIT_PATTERN.matcher(password)
            .find();
    }
}
