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

package com.bytechef.platform.data.table.domain;

import org.jspecify.annotations.Nullable;

/**
 * One condition on a data table query. Deliberately the same operator set as {@code ContextStoreQueryFilter}, so the
 * two query surfaces read alike.
 *
 * <p>
 * {@code value} is whatever the caller supplied -- a scalar for most operators, a {@code List} for {@code IN} and
 * {@code BETWEEN}, and {@code null} for {@code EQ}/{@code NEQ} to mean {@code IS NULL}/{@code IS NOT NULL}. It is
 * coerced to the column's type when the SQL is built, never before.
 *
 * @author Ivica Cardic
 */
public record RowFilter(String field, Operator operator, @Nullable Object value) {

    public enum Operator {
        EQ,
        NEQ,
        IN,
        CONTAINS,
        STARTS_WITH,
        GT,
        GTE,
        LT,
        LTE,
        BETWEEN
    }
}
