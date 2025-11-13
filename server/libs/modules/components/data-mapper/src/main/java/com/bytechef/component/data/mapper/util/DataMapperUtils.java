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

package com.bytechef.component.data.mapper.util;

import static com.bytechef.component.data.mapper.constant.DataMapperConstants.TYPE;

import com.bytechef.component.data.mapper.constant.ValueType;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

/**
 * @author Marko Kriskovic
 */
public class DataMapperUtils {

    public static final String VALUE_DESCRIPTION = "The value you want to replace.";
    public static final String VALUE_LABEL = "Value";
    public static final String DEFAULT_VALUE_DESCRIPTION =
        "If there is no existing mapping, assign this value as default.";
    public static final String DEFAULT_VALUE_LABEL = "Default Value";
    public static final String MAPPINGS_DESCRIPTION = "An array of objects that contains properties 'from' and 'to'.";
    public static final String MAPPINGS_LABEL = "Mappings";
    public static final String LABEL_TO = "Value To";
    public static final String LABEL_FROM = "Value From";
    public static final String FROM_DESCRIPTION = "Defines the property value you want to change.";
    public static final String TO_DESCRIPTION = "Defines what you want to change the property value to.";

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
            default ->
                throw new IllegalStateException(
                    "Unexpected value: " + inputParameters.getRequired(TYPE, ValueType.class));
        };
    }

    public static boolean canConvert(Context context, Object mappingFrom, Class<?> type) {
        return context.convert(convert -> convert.canConvert(mappingFrom, type));
    }

    public static Object convertTo(Context context, Object mappingTo, Class<?> type) {
        return context.convert(convert -> convert.value(mappingTo, type));
    }

    public static Object convertFrom(Context context, Object mappingFrom, Class<?> type) {
        return context.convert(convert -> convert.value(mappingFrom, type));
    }

    public static String getDisplayCondition(ValueType valueType) {
        return "type == '%s'".formatted(valueType);
    }
}
