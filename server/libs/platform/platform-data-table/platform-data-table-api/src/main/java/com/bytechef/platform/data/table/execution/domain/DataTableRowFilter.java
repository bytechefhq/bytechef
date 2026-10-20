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
import java.util.Objects;

/**
 * A condition a row must satisfy. {@link Operation#IN} expects a collection value, {@link Operation#BETWEEN} a
 * collection of exactly two values (lower and upper bound, inclusive), and {@link Operation#EMPTY} and
 * {@link Operation#NOT_EMPTY} ignore the value.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record DataTableRowFilter(String column, Operation operation, Object value) {

    public DataTableRowFilter {
        Objects.requireNonNull(column, "column must not be null");
        Objects.requireNonNull(operation, "operation must not be null");
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
