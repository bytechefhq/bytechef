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

import com.bytechef.commons.util.DateUtils;
import java.math.BigDecimal;

/**
 * @author Ivica Cardic
 */
public final class ColumnValues {

    private ColumnValues() {
    }

    public static boolean isCoercible(ColumnType columnType, Object value) {
        String text = String.valueOf(value)
            .trim();

        try {
            return switch (columnType) {
                case STRING -> true;
                case BOOLEAN -> value instanceof Boolean || "true".equalsIgnoreCase(text) ||
                    "false".equalsIgnoreCase(text);
                case INTEGER -> value instanceof Integer || value instanceof Long || parsesAsLong(text);
                case NUMBER -> value instanceof Number || parsesAsDecimal(text);
                case DATE -> DateUtils.parseSqlDate(text) != null;
                case DATE_TIME -> DateUtils.parseSqlTimestamp(text) != null;
            };
        } catch (RuntimeException runtimeException) {
            return false;
        }
    }

    private static boolean parsesAsLong(String text) {
        Long.parseLong(text);

        return true;
    }

    private static boolean parsesAsDecimal(String text) {
        new BigDecimal(text);

        return true;
    }
}
