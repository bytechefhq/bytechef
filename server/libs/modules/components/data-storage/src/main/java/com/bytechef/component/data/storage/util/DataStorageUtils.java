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
import static com.bytechef.component.definition.ComponentDSL.nullable;

import com.bytechef.component.definition.Parameters;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

public class DataStorageUtils {

    public static Class<?> getType(Parameters inputParameters) {
        return switch (inputParameters.getRequiredInteger(TYPE)) {
            case 1 -> ArrayList.class;
            case 2 -> Boolean.class;
            case 3 -> LocalDate.class;
            case 4 -> LocalDateTime.class;
            case 5 -> Integer.class;
            case 7 -> Number.class;
            case 8 -> Object.class;
            case 9 -> String.class;
            case 10 -> LocalTime.class;
            default -> nullable().getClass();
        };
    }

    public static Object getValue(Parameters inputParameters) {
        Object value = null;

        switch (inputParameters.getRequiredInteger(TYPE)) {
            case 1:
                value = inputParameters.getRequiredArray(VALUE);
                break;
            case 2:
                value = inputParameters.getRequiredBoolean(VALUE);
                break;
            case 3:
                value = inputParameters.getRequiredLocalDate(VALUE);
                break;
            case 4:
                value = inputParameters.getRequiredLocalDateTime(VALUE);
                break;
            case 5:
                value = inputParameters.getRequiredInteger(VALUE);
                break;
            case 6:
                value = nullable();
                break;
            case 7:
                value = inputParameters.getRequiredDouble(VALUE);
                break;
            case 8:
                value = inputParameters.getRequiredMap(VALUE);
                break;
            case 9:
                value = inputParameters.getRequiredString(VALUE);
                break;
            case 10:
                value = inputParameters.getRequiredLocalTime(VALUE);
                break;
            default:
                break;
        }

        return value;
    }
}
