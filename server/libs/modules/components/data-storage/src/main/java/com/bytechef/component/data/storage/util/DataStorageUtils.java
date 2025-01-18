/*
 * Copyright 2023-present ByteChef Inc.
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
import static com.bytechef.component.definition.ComponentDsl.nullable;

import com.bytechef.component.data.storage.constant.ValueType;
import com.bytechef.component.definition.Parameters;
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
}
