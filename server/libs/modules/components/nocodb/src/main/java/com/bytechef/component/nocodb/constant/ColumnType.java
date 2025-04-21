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

package com.bytechef.component.nocodb.constant;

/**
 * @author Monika Kušter
 */
public enum ColumnType {

    CHECKBOX("Checkbox"),
    SINGLE_LINE_TEXT("SingleLineText"),
    PHONE_NUMBER("PhoneNumber"),
    EMAIL("Email"),
    URL("URL"),
    LONG_TEXT("LongText"),
    NUMBER("Number"),
    DECIMAL("Decimal"),
    PERCENT("Percent"),
    RATING("Rating"),
    CURRENCY("Currency"),
    YEAR("Year"),
    MULTISELECT("MultiSelect"),
    SINGLE_SELECT("SingleSelect"),
    DATE("Date"),
    TIME("Time"),
    DATETIME("DateTime");

    private final String name;

    ColumnType(String name) {
        this.name = name;
    }

    public static ColumnType getColumnType(String name) {
        for (ColumnType columnType : values()) {
            if (name.equals(columnType.name)) {
                return columnType;
            }
        }

        return null;
    }
}
