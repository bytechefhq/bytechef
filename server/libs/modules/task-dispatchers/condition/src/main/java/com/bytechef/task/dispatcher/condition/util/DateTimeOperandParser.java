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

package com.bytechef.task.dispatcher.condition.util;

import com.bytechef.commons.util.TemporalValueUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import org.jspecify.annotations.Nullable;

/**
 * Parses a {@code dateTime} condition operand into a {@link ZonedDateTime} at UTC. Operands reach this class either as
 * a reconstructed temporal value or as text from an API response or the editor's date picker.
 *
 * @author Ivica Cardic
 */
final class DateTimeOperandParser {

    private DateTimeOperandParser() {
    }

    static ZonedDateTime parse(@Nullable Object operand, String operandName) {
        Object normalized = TemporalValueUtils.normalize(operand);

        if (normalized instanceof ZonedDateTime zonedDateTime) {
            return zonedDateTime;
        }

        if (normalized instanceof LocalDateTime localDateTime) {
            return localDateTime.atZone(ZoneOffset.UTC);
        }

        if (normalized instanceof LocalDate localDate) {
            return localDate.atStartOfDay(ZoneOffset.UTC);
        }

        if (normalized instanceof String text && !text.isBlank()) {
            return parseText(text, operandName);
        }

        throw new IllegalArgumentException(
            "Condition operand " + operandName + " is not a date: " + normalized);
    }

    private static ZonedDateTime parseText(String text, String operandName) {
        try {
            return ZonedDateTime.parse(text)
                .withZoneSameInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException zonedException) {
            try {
                return LocalDateTime.parse(text)
                    .atZone(ZoneOffset.UTC);
            } catch (DateTimeParseException localDateTimeException) {
                try {
                    return LocalDate.parse(text)
                        .atStartOfDay(ZoneOffset.UTC);
                } catch (DateTimeParseException localDateException) {
                    throw new IllegalArgumentException(
                        "Condition operand " + operandName + " is not a date: " + text, localDateException);
                }
            }
        }
    }
}
