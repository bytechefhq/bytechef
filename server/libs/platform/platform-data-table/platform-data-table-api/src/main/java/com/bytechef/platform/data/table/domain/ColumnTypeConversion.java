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

/**
 * @author Ivica Cardic
 */
public enum ColumnTypeConversion {

    LOSSLESS,
    LOSSY,
    UNSUPPORTED;

    public static ColumnTypeConversion of(ColumnType fromColumnType, ColumnType toColumnType) {
        if (fromColumnType == toColumnType) {
            return LOSSLESS;
        }

        if (fromColumnType == ColumnType.LINK) {
            return UNSUPPORTED;
        }

        return switch (toColumnType) {
            case STRING -> LOSSLESS;
            case INTEGER -> switch (fromColumnType) {
                case STRING, NUMBER -> LOSSY;
                case BOOLEAN -> LOSSLESS;
                default -> UNSUPPORTED;
            };
            case NUMBER -> switch (fromColumnType) {
                case STRING -> LOSSY;
                case INTEGER, BOOLEAN -> LOSSLESS;
                default -> UNSUPPORTED;
            };
            case BOOLEAN -> switch (fromColumnType) {
                case STRING, INTEGER, NUMBER -> LOSSY;
                default -> UNSUPPORTED;
            };
            case DATE -> switch (fromColumnType) {
                case STRING, DATE_TIME -> LOSSY;
                default -> UNSUPPORTED;
            };
            case DATE_TIME -> switch (fromColumnType) {
                case STRING -> LOSSY;
                case DATE -> LOSSLESS;
                default -> UNSUPPORTED;
            };
            case LINK -> UNSUPPORTED;
        };
    }
}
