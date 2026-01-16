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

package com.bytechef.component.form.util;

/**
 * @author Ivica Cardic
 */
public enum FieldType {

    CHECKBOX(1),
    CUSTOM_HTML(12),
    DATE_PICKER(2),
    DATETIME_PICKER(3),
    EMAIL_INPUT(8),
    FILE_INPUT(4),
    HIDDEN_FIELD(13),
    INPUT(6),
    NUMBER_INPUT(9),
    PASSWORD_INPUT(10),
    RADIO(11),
    SELECT(7),
    TEXTAREA(5);

    private final int value;

    FieldType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static FieldType fromValue(int value) {
        for (FieldType fieldType : values()) {
            if (fieldType.value == value) {
                return fieldType;
            }
        }

        throw new IllegalArgumentException("Invalid FieldType value: " + value);
    }
}
