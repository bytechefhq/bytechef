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

import static com.bytechef.component.data.mapper.constant.DataMapperConstants.TYPE;
import static com.bytechef.component.definition.ComponentDSL.nullable;

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
    public static final String DEFAULT_VALUE_LABEL = "Default value";
    public static final String MAPPINGS_DESCRIPTION = "An array of objects that contains properties 'from' and 'to'.";
    public static final String MAPPINGS_LABEL = "Mappings";
    public static final String LABEL_TO = "Value to";
    public static final String LABEL_FROM = "Value from";
    public static final String FROM_DESCRIPTION = "Defines the property value you want to change.";
    public static final String TO_DESCRIPTION = "Defines what you want to change the property value to.";

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

    public static String getDisplayCondition(String number) {
        return "type == " + number;
    }
}
