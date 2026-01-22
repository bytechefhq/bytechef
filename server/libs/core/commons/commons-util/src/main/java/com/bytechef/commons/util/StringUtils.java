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
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * @author Ivica Cardic
 */
public class StringUtils {

    private static final DateTimeFormatter[] DATE_TIME_FORMATTERS = new DateTimeFormatter[] {
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    };

    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[] {
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyy-M-d")
    };

    /**
     * Appends the given string to the provided StringBuilder, ensuring that it is preceded by a newline character if
     * the StringBuilder is not empty.
     *
     * @param string The string to append to the StringBuilder. Cannot be null.
     * @param sb     The StringBuilder to which the string will be appended. Cannot be null.
     */
    public static void appendWithNewline(String string, StringBuilder sb) {
        if (!sb.isEmpty()) {
            sb.append("\n");
        }

        sb.append(string);
    }

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

    /**
     * Parses the given string and attempts to convert it into a {@link java.sql.Date}. The method uses the following
     * strategies in order: 1. Tries to parse the string using predefined local date patterns. 2. Attempts to interpret
     * the string as epoch milliseconds. 3. Delegates to {@link #parseSqlTimestamp(String)} to handle date-time inputs
     * and truncates the result to produce a {@link java.sql.Date}. If all parsing attempts fail, the method returns
     * null.
     *
     * @param string The input string to be parsed. Cannot be null. Should represent a valid local date, epoch
     *               milliseconds, or a parsable date-time string.
     * @return A {@link java.sql.Date} object representing the parsed value, or null if the input cannot be interpreted
     *         as a valid date.
     */
    public static java.sql.Date parseSqlDate(String string) {
        // Try ISO first
        for (DateTimeFormatter dateTimeFormatter : DATE_FORMATTERS) {
            try {
                LocalDate localDate = LocalDate.parse(string, dateTimeFormatter);
                return java.sql.Date.valueOf(localDate);
            } catch (DateTimeParseException ignored) {
            }
        }

        // Try epoch millis
        try {
            long epoch = Long.parseLong(string);
            return new java.sql.Date(epoch);
        } catch (NumberFormatException ignored) {
        }

        // Try date-time inputs by truncating to date
        Timestamp timestamp = parseSqlTimestamp(string);

        if (timestamp != null) {
            return new java.sql.Date(timestamp.getTime());
        }
        return null;
    }

    /**
     * Parses the given string and attempts to convert it into a {@link Timestamp}. The method uses multiple strategies
     * in the following order: 1. Tries to parse the string as an ISO 8601 instant. 2. Attempts to parse the string
     * using predefined local date-time patterns, assuming the system's default time zone. 3. Tries to interpret the
     * string as epoch milliseconds. If all parsing attempts fail, the method returns null.
     *
     * @param string The input string to be parsed. Should represent a valid ISO 8601 instant, a date-time string
     *               matching predefined patterns, or epoch milliseconds. If null or an invalid format is provided, the
     *               method will return null.
     * @return A {@link Timestamp} representing the parsed value, or null if the input cannot be interpreted as a valid
     *         timestamp.
     */
    public static Timestamp parseSqlTimestamp(String string) {
        // Try instant (ISO8601)
        try {
            Instant instant = Instant.parse(string);

            return Timestamp.from(instant);
        } catch (DateTimeParseException ignored) {
        }

        // Try a few local date-time patterns (assume system default zone)
        for (DateTimeFormatter dateTimeFormatter : DATE_TIME_FORMATTERS) {
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(string, dateTimeFormatter);

                return Timestamp.valueOf(localDateTime);
            } catch (DateTimeParseException ignored) {
            }
        }

        // Try epoch millis
        try {
            long epoch = Long.parseLong(string);

            return new Timestamp(epoch);
        } catch (NumberFormatException ignored) {
        }

        return null;
    }

    /**
     * Sanitizes the given string by applying a set of transformations defined by the overloaded method
     * {@link #sanitize(String, int)} with a default length limit of -1. This method is useful for ensuring the input
     * string is free of unsafe or undesirable characters and formatted appropriately.
     *
     * @param value The input string to be sanitized. If null, the method returns null. If blank, it returns an empty
     *              string.
     * @return A sanitized version of the input string with no length restriction.
     */
    public static String sanitize(String value) {
        return sanitize(value, -1);
    }

    /**
     * Sanitizes the given string by removing or replacing unsafe characters and trimming whitespace. This method is
     * useful for preparing file names, identifiers, or other strings that require a specific format or should avoid
     * problematic characters.
     * <p>
     * The method applies the following transformations: 1. Trims leading and trailing whitespace from the input string.
     * 2. Replaces Windows line break characters ("\r") with "\\r". 3. Replaces Unix line break characters ("\n") with
     * "\\n". 4. Replaces Windows forbidden characters (e.g., \ / : * ? " < > |) with an underscore. 5. Replaces
     * sequences of whitespace with a single underscore. 6. Collapses consecutive underscores into a single underscore.
     * 7. Truncates the string to the specified length limit, if provided.
     *
     * @param value       The input string to be sanitized. If null, the method returns null. If blank, it returns an
     *                    empty string.
     * @param lengthLimit An integer specifying the maximum allowed length for the sanitized string. Use -1 for no
     *                    length restriction.
     * @return A sanitized version of
     */
    public static String sanitize(String value, int lengthLimit) {
        if (value == null) {
            return null;
        }

        if (org.apache.commons.lang3.StringUtils.isBlank(value)) {
            return "";
        }

        String sanitized = value.trim()
            .replace("\r", "\\r") // Windows line break
            .replace("\n", "\\n") // Unix line break
            .replaceAll("[\\\\/:*?\"<>|]", "_") // Windows forbidden characters
            .replaceAll("\\s+", "_") // Replace whitespace with underscores
            .replaceAll("_{2,}", "_"); // Replace multiple underscores with single;

        if (lengthLimit != -1 && sanitized.length() > lengthLimit) {
            sanitized = sanitized.substring(0, lengthLimit);
        }

        return sanitized;
    }
}
