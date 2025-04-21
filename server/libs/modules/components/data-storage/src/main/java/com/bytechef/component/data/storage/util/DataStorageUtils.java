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

package com.bytechef.component.data.storage.util;

import static com.bytechef.component.data.storage.constant.DataStorageConstants.TYPE;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.VALUE;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.nullable;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;

import com.bytechef.component.data.storage.constant.ValueType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ValueProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

public class DataStorageUtils {

    public static Class<?> getType(Parameters inputParameters) {
        return switch (inputParameters.getRequired(TYPE, ValueType.class)) {
            case ARRAY -> ArrayList.class;
            case BOOLEAN -> Boolean.class;
            case DATE -> LocalDate.class;
            case DATE_TIME -> LocalDateTime.class;
            case INTEGER -> Integer.class;
            case NUMBER -> Number.class;
            case OBJECT -> Object.class;
            case STRING -> String.class;
            case TIME -> LocalTime.class;
            default -> null;
        };
    }

    public static Object getValue(Parameters inputParameters) {
        Object value = null;

        switch (inputParameters.getRequired(TYPE, ValueType.class)) {
            case ARRAY:
                value = inputParameters.getRequiredArray(VALUE);

                break;
            case BOOLEAN:
                value = inputParameters.getRequiredBoolean(VALUE);

                break;
            case DATE:
                value = inputParameters.getRequiredLocalDate(VALUE);

                break;
            case DATE_TIME:
                value = inputParameters.getRequiredLocalDateTime(VALUE);

                break;
            case INTEGER:
                value = inputParameters.getRequiredInteger(VALUE);

                break;
            case NULL:
                value = nullable();

                break;
            case NUMBER:
                value = inputParameters.getRequiredDouble(VALUE);

                break;
            case OBJECT:
                value = inputParameters.getRequiredMap(VALUE);

                break;
            case STRING:
                value = inputParameters.getRequiredString(VALUE);

                break;
            case TIME:
                value = inputParameters.getRequiredLocalTime(VALUE);

                break;
            default:

                break;
        }

        return value;
    }

    public static ValueProperty<?> getValueProperty(ValueType valueType) {
        return switch (valueType) {
            case ARRAY -> array();
            case BOOLEAN -> bool();
            case DATE -> date();
            case DATE_TIME -> dateTime();
            case INTEGER -> integer();
            case NUMBER -> number();
            case OBJECT -> object();
            case STRING -> string();
            case TIME -> time();
            default -> nullable();
        };
    }
}
