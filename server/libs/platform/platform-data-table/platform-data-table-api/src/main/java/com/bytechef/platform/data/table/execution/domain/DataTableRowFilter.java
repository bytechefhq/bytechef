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
 * A condition a row must satisfy. The {@link Operation.Arity} of the operation decides the shape of the value:
 * {@link Operation.Arity#LIST} expects a non-empty collection, {@link Operation.Arity#PAIR} a list of exactly two
 * values (lower and upper bound, inclusive), {@link Operation.Arity#NONE} ignores any value and
 * {@link Operation.Arity#ONE} a single value. A single value, and each value of a list, must not be null, blank, an
 * array, a collection or a map. The row service rejects text-only operations on non-text columns.
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

        switch (operation.arity()) {
            case NONE -> value = null;
            case LIST, PAIR -> {
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

                if (collection.stream()
                    .anyMatch(DataTableRowFilter::isCompound)) {

                    throw new IllegalArgumentException(
                        "Filter on column '%s' with operation %s requires single values, not lists or objects"
                            .formatted(column, operation));
                }

                if (operation.arity() == Operation.Arity.PAIR && !(collection instanceof List<?>)) {
                    throw new IllegalArgumentException(
                        "Filter on column '%s' with operation %s requires an ordered list of a lower and an upper bound"
                            .formatted(column, operation));
                }

                if (operation.arity() == Operation.Arity.PAIR && collection.size() != 2) {
                    throw new IllegalArgumentException(
                        "Filter on column '%s' with operation %s requires exactly two values, got %d".formatted(
                            column, operation, collection.size()));
                }

                value = List.copyOf(collection);
            }
            default -> {
                if (isMissing(value)) {
                    throw new IllegalArgumentException(
                        ("Filter on column '%s' with operation %s requires a value; use Empty or Not Empty to match " +
                            "records by whether the column has a value").formatted(column, operation));
                }

                if (isCompound(value)) {
                    throw new IllegalArgumentException(
                        "Filter on column '%s' with operation %s requires a single value, not a list or an object"
                            .formatted(column, operation));
                }
            }
        }
    }

    /**
     * Returns the value to compare with for an {@link Operation.Arity#ONE} operation.
     *
     * @throws IllegalStateException if the operation does not take a single value
     */
    public Object singleValue() {
        if (operation.arity() != Operation.Arity.ONE) {
            throw new IllegalStateException("Operation %s does not take a single value".formatted(operation));
        }

        return Objects.requireNonNull(value);
    }

    /**
     * Returns the values to compare with: none for {@link Operation.Arity#NONE}, the single value for
     * {@link Operation.Arity#ONE}, and the list for {@link Operation.Arity#LIST} and {@link Operation.Arity#PAIR}.
     */
    @SuppressWarnings("unchecked")
    public List<Object> values() {
        return switch (operation.arity()) {
            case NONE -> List.of();
            case ONE -> List.of(Objects.requireNonNull(value));
            case LIST, PAIR -> (List<Object>) Objects.requireNonNull(value);
        };
    }

    private static boolean isCompound(@Nullable Object value) {
        if (value == null) {
            return false;
        }

        Class<?> valueClass = value.getClass();

        return value instanceof Collection<?> || value instanceof Map<?, ?> || valueClass.isArray();
    }

    private static boolean isMissing(@Nullable Object value) {
        return value == null || (value instanceof String string && string.isBlank());
    }

    public enum Operation {

        EQUALS(Arity.ONE, false),
        EQUALS_IGNORE_CASE(Arity.ONE, true),
        NOT_EQUALS(Arity.ONE, false),
        CONTAINS(Arity.ONE, true),
        NOT_CONTAINS(Arity.ONE, true),
        STARTS_WITH(Arity.ONE, true),
        ENDS_WITH(Arity.ONE, true),
        GREATER(Arity.ONE, false),
        GREATER_EQUALS(Arity.ONE, false),
        LESS(Arity.ONE, false),
        LESS_EQUALS(Arity.ONE, false),
        IN(Arity.LIST, false),
        BETWEEN(Arity.PAIR, false),
        EMPTY(Arity.NONE, false),
        NOT_EMPTY(Arity.NONE, false);

        private final Arity arity;
        private final boolean textOnly;

        Operation(Arity arity, boolean textOnly) {
            this.arity = arity;
            this.textOnly = textOnly;
        }

        public Arity arity() {
            return arity;
        }

        public boolean textOnly() {
            return textOnly;
        }

        public enum Arity {

            NONE,
            ONE,
            LIST,
            PAIR
        }
    }
}
