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

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * @author Ivica Cardic
 */
public class DateUtils {

    private static final DateTimeFormatter[] DATE_TIME_FORMATTERS = new DateTimeFormatter[] {
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    };

    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[] {
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyy-M-d")
    };

    public static Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault())
            .toInstant());
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
}
