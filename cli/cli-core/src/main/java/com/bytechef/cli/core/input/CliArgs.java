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

package com.bytechef.cli.core.input;

import com.bytechef.cli.core.error.CliException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * Parsing helpers for optional command-line filter arguments.
 *
 * @author Ivica Cardic
 */
public final class CliArgs {

    private CliArgs() {
    }

    public static OffsetDateTime parseDateTime(String value) {
        if (value == null) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new CliException(1, "Invalid date-time (expected ISO-8601): " + value);
        }
    }

    public static List<String> splitCsv(String value) {
        if (value == null) {
            return null;
        }

        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .toList();
    }

    public static List<Long> splitCsvLong(String value) {
        if (value == null) {
            return null;
        }

        try {
            return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .map(Long::valueOf)
                .toList();
        } catch (NumberFormatException e) {
            throw new CliException(1, "Invalid id list (expected comma-separated numbers): " + value);
        }
    }
}
