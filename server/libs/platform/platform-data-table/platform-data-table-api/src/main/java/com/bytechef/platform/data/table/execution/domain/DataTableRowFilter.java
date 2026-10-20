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

package com.bytechef.platform.data.table.execution.domain;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A condition a row must satisfy. {@link Operation#IN} expects a non-empty collection value, {@link Operation#BETWEEN}
 * a collection of exactly two values (lower and upper bound, inclusive), and {@link Operation#EMPTY} and
 * {@link Operation#NOT_EMPTY} take no value. Every other operation expects a single value. Values must not be null or
 * blank. {@link Operation#EQUALS_IGNORE_CASE}, {@link Operation#CONTAINS}, {@link Operation#NOT_CONTAINS},
 * {@link Operation#STARTS_WITH} and {@link Operation#ENDS_WITH} apply to text columns only.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record DataTableRowFilter(String column, Operation operation, @Nullable Object value) {

    public DataTableRowFilter {
        Objects.requireNonNull(column, "column must not be null");
        Objects.requireNonNull(operation, "operation must not be null");

        if (column.isBlank()) {
            throw new IllegalArgumentException("column must not be blank");
        }

        switch (operation) {
            case EMPTY, NOT_EMPTY -> value = null;
            case IN, BETWEEN -> {
                if (!(value instanceof Collection<?> collection)) {
                    throw new IllegalArgumentException(
                        "Filter on column '%s' with operation %s requires a list of values".formatted(
                            column, operation));
                }

                if (collection.isEmpty()) {
                    throw new IllegalArgumentException(
                        "Filter on column '%s' with operation %s requires at least one value".formatted(
                            column, operation));
                }

                if (collection.stream()
                    .anyMatch(DataTableRowFilter::isMissing)) {

                    throw new IllegalArgumentException(
                        "Filter on column '%s' with operation %s requires every value to be set".formatted(
                            column, operation));
                }

                if (operation == Operation.BETWEEN && collection.size() != 2) {
                    throw new IllegalArgumentException(
                        "Filter on column '%s' with operation BETWEEN requires exactly two values, got %d".formatted(
                            column, collection.size()));
                }

                value = List.copyOf(collection);
            }
            default -> {
                if (isMissing(value)) {
                    throw new IllegalArgumentException(
                        ("Filter on column '%s' with operation %s requires a value; use Empty or Not Empty to match " +
                            "records by whether the column has a value").formatted(column, operation));
                }

                if (value instanceof Collection<?> || value instanceof Map<?, ?>) {
                    throw new IllegalArgumentException(
                        "Filter on column '%s' with operation %s requires a single value, not a list or an object"
                            .formatted(column, operation));
                }
            }
        }
    }

    private static boolean isMissing(@Nullable Object value) {
        return value == null || (value instanceof String string && string.isBlank());
    }

    public enum Operation {

        EQUALS,
        EQUALS_IGNORE_CASE,
        NOT_EQUALS,
        CONTAINS,
        NOT_CONTAINS,
        STARTS_WITH,
        ENDS_WITH,
        GREATER,
        GREATER_EQUALS,
        LESS,
        LESS_EQUALS,
        IN,
        BETWEEN,
        EMPTY,
        NOT_EMPTY
    }
}
