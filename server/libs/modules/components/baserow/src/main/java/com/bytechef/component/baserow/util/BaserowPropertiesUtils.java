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

package com.bytechef.component.baserow.util;

import static com.bytechef.component.baserow.constant.BaserowConstants.NAME;
import static com.bytechef.component.baserow.constant.BaserowConstants.READ_ONLY;
import static com.bytechef.component.baserow.constant.BaserowConstants.TABLE_ID;
import static com.bytechef.component.baserow.constant.BaserowConstants.TYPE;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.baserow.constant.BaserowFieldType;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.Property.ValueProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Monika Kušter
 */
public class BaserowPropertiesUtils {

    private BaserowPropertiesUtils() {
    }

    public static List<ValueProperty<?>> createPropertiesForRow(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        ActionContext context) {

        List<Map<String, ?>> tableFields = BaserowUtils.getTableFields(
            context, inputParameters.getRequiredString(TABLE_ID));

        return new ArrayList<>(tableFields.stream()
            .map(BaserowPropertiesUtils::createProperty)
            .filter(Objects::nonNull)
            .toList());
    }

    private static ModifiableValueProperty<?, ?> createProperty(Map<String, ?> map) {
        String name = (String) map.get(NAME);
        String type = (String) map.get(TYPE);
        boolean readOnly = (boolean) map.get(READ_ONLY);

        if (!readOnly) {
            BaserowFieldType baserowFieldType = BaserowFieldType.getBaserowFieldType(type);

            return switch (baserowFieldType) {
                case BOOLEAN -> bool(name)
                    .label(name)
                    .required(false);
                case RATING -> {
                    int maxValue = (int) map.get("max_value");

                    yield integer(name)
                        .label(name)
                        .description("Enter valid value between 1 and " + maxValue)
                        .maxValue(maxValue)
                        .required(false);
                }
                case LONG_TEXT -> string(name)
                    .label(name)
                    .controlType(ControlType.TEXT_AREA)
                    .required(false);
                case SINGLE_SELECT -> string(name)
                    .label(name)
                    .options(getOptions(map))
                    .required(false);
                case NUMBER -> number(name)
                    .label(name)
                    .required(false);
                case TEXT -> string(name)
                    .label(name)
                    .defaultValue((String) map.get("text_default"))
                    .required(false);
                case URL -> string(name)
                    .label(name)
                    .controlType(ControlType.URL)
                    .required(false);
                case PHONE_NUMBER -> string(name)
                    .label(name)
                    .controlType(ControlType.PHONE)
                    .required(false);
                case DATE -> {
                    boolean dateIncludeTime = (boolean) map.get("date_include_time");

                    if (dateIncludeTime) {
                        yield dateTime(name)
                            .label(name)
                            .required(false);
                    } else {
                        yield date(name)
                            .label(name)
                            .required(false);
                    }
                }
                case MULTI_SELECT -> array(name)
                    .label(name)
                    .items(
                        string()
                            .options(getOptions(map)))
                    .required(false);

                default -> null;
            };
        }

        return null;
    }

    private static List<Option<String>> getOptions(Map<String, ?> map) {
        List<Option<String>> options = new ArrayList<>();

        if (map.get("select_options") instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> opt) {
                    String value = (String) opt.get("value");
                    options.add(option(value, value));
                }
            }
        }

        return options;
    }
}
