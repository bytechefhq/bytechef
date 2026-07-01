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

package com.bytechef.commons.util;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

public class NumberUtils {

    public static @Nullable Long asLong(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        return parseLong(value.toString());
    }

    /**
     * Parses the given string and attempts to convert it into a {@link BigDecimal}. If the input string cannot be
     * parsed as a valid BigDecimal, the method returns null.
     *
     * @param string The input string to be parsed. Should represent a valid BigDecimal value. If null or an invalid
     *               format is provided, the method will return null.
     * @return A {@link BigDecimal} representing the parsed value, or null if the input cannot be interpreted as a valid
     *         BigDecimal.
     */
    public static BigDecimal parseBigDecimal(String string) {
        try {
            return new BigDecimal(string);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses the given string and attempts to convert it into a {@link Long}. Initially, the method tries to parse the
     * string using {@link Long#valueOf(String)}. If that fails due to a {@link NumberFormatException}, it attempts to
     * parse the string as a {@link BigDecimal} and then converts it to a long value. If both parsing attempts fail, the
     * method returns null.
     *
     * @param string The input string to be parsed. Should represent a valid numeric value that can be interpreted as a
     *               {@link Long} or {@link BigDecimal}. If null or an invalid format is provided, the method returns
     *               null.
     * @return A {@link Long} representing the parsed value, or null if the input cannot be interpreted as a valid
     *         numeric value.
     */
    public static Long parseLong(String string) {
        try {
            return Long.valueOf(string);
        } catch (NumberFormatException e) {
            try {
                return new BigDecimal(string).longValue();
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }
}
