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

package com.bytechef.component.data.mapper.util;

import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INCLUDE_EMPTY_STRINGS;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INCLUDE_NULLS;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INCLUDE_UNMAPPED;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.TYPE;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.VALUE;
import static com.bytechef.component.definition.ComponentDSL.nullable;

import com.bytechef.component.definition.Parameters;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.graalvm.collections.Pair;

public class DataMapperUtils {
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

    public static void mapEntry(
        Parameters inputParameters, Map<String, Object> output, Map<String, Pair<String, Boolean>> mappings,
        Map.Entry<String, Object> entry) {
        if ((inputParameters.getBoolean(INCLUDE_NULLS) == null || (inputParameters.getBoolean(INCLUDE_NULLS) != null
            && (inputParameters.getBoolean(INCLUDE_NULLS) || ObjectUtils.anyNotNull(entry.getValue()))))
            && (inputParameters.getBoolean(INCLUDE_EMPTY_STRINGS) == null || (inputParameters
                .getBoolean(INCLUDE_EMPTY_STRINGS) != null
                && (inputParameters.getBoolean(INCLUDE_EMPTY_STRINGS) || ObjectUtils.isNotEmpty(entry.getValue()))))) {

            if (mappings.containsKey(entry.getKey())) {
                if (mappings.get(entry.getKey())
                    .getRight() != null && mappings.get(entry.getKey())
                        .getRight()) {
                    Objects.requireNonNull(entry.getValue(), "Required field " + entry.getKey() + " cannot be null.");
                    Validate.notBlank(entry.getValue()
                        .toString(), "Required field " + entry.getKey() + " cannot be empty.");
                }

                output.put(mappings.get(entry.getKey())
                    .getLeft(), entry.getValue());
            } else if (inputParameters.getBoolean(INCLUDE_UNMAPPED) != null
                && inputParameters.getBoolean(INCLUDE_UNMAPPED))
                output.put(entry.getKey(), entry.getValue());
        }
    }
}
