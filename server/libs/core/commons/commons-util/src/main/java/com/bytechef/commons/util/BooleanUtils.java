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

import java.util.Locale;

public class BooleanUtils {
    /**
     * Parses the given string and attempts to convert it into a Boolean value. The method recognizes several string
     * representations for true and false: - For true: "true", "t", "yes", "y", and "1" (case insensitive). - For false:
     * "false", "f", "no", "n", and "0" (case insensitive). If the input string does not match any of these
     * representations, the method defers to {@link Boolean#parseBoolean(String)}.
     *
     * @param string The input string to be parsed. If null or empty, the method may return false depending on
     *               {@link Boolean#parseBoolean(String)}.
     * @return A Boolean value representing the input string, or null if the input does not match any recognized
     *         representation.
     */
    public static Boolean parseBoolean(String string) {
        String value = string.toLowerCase(Locale.ROOT);

        if (value.equals("true") || value.equals("t") || value.equals("yes") || value.equals("y") ||
            value.equals("1")) {

            return Boolean.TRUE;
        }

        if (value.equals("false") || value.equals("f") || value.equals("no") || value.equals("n") ||
            value.equals("0")) {

            return Boolean.FALSE;
        }

        return Boolean.parseBoolean(value);
    }
}
