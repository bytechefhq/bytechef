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

package com.bytechef.component.data.mapper.action;

import static com.bytechef.component.data.mapper.constant.DataMapperConstants.FROM;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INCLUDE_EMPTY_STRINGS;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INCLUDE_NULLS;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INCLUDE_UNMAPPED;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INPUT;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INPUT_TYPE;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.MAPPINGS;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.REQUIRED_FIELD;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.TO;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.data.mapper.util.mapping.RequiredStringMapping;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.graalvm.collections.Pair;

/**
 * @author Ivica Cardic
 */
public class DataMapperMapObjectsToObjectAction {

    private DataMapperMapObjectsToObjectAction() {
    }

    public static final ModifiableActionDefinition ACTION_DEFINITION = ComponentDSL.action("mapObjectsToObject")
        .title("Map objects to object")
        .description("Creates a new object with the chosen input properties. You can also rename the property keys.")
        .properties(
            integer(INPUT_TYPE)
                .label("Input type")
                .description("The input type.")
                .options(
                    option("Object", 1),
                    option("Array", 2))
                .required(true),
            object(INPUT)
                .label("Input")
                .description("An object containing one or more properties.")
                .displayCondition("inputType == 1")
                .required(true),
            array(INPUT)
                .label("Input")
                .description("An array containing one or more objects.")
                .displayCondition("inputType == 2")
                .items(object())
                .required(true),
            array(MAPPINGS)
                .label("Mapping")
                .description(
                    "An array of objects that contains properties 'from', 'to' and 'requiredField'. For nested keys, it supports dot notation, where the new mapped path can be used for nested mapping.")
                .items(
                    object()
                        .properties(
                            string(FROM)
                                .label("From")
                                .description(
                                    "Name of the input property key that you want to put in the newly created object."),
                            string(TO)
                                .label("To")
                                .description("Name of the property key in the newly created object."),
                            bool(REQUIRED_FIELD)
                                .label("Required field")
                                .description("Does the property require a value?")
                                .defaultValue(false)))
                .required(true),
            bool(INCLUDE_UNMAPPED)
                .label("Include Unmapped")
                .description(
                    "Should fields from the original object that do not have mappings be included in the new object?")
                .defaultValue(false),
            bool(INCLUDE_NULLS)
                .label("Include Nulls")
                .description("Should fields that have null values be included in the new object?")
                .defaultValue(true),
            bool(INCLUDE_EMPTY_STRINGS)
                .label("Include Empty strings")
                .description("Should fields with empty string values be included in the new object?")
                .defaultValue(true))
        .output()
        .perform(DataMapperMapObjectsToObjectAction::perform);

    protected static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        Map<String, Object> output = new HashMap<>();

        List<RequiredStringMapping> mappingList =
            inputParameters.getList(MAPPINGS, RequiredStringMapping.class, List.of());
        Map<String, Pair<String, Boolean>> mappings = mappingList.stream()
            .collect(Collectors.toMap(RequiredStringMapping::getFrom,
                value -> Pair.create(value.getTo(), value.isRequiredField())));

        if (inputParameters.getInteger(INPUT_TYPE)
            .equals(1)) {
            Map<String, Object> input = inputParameters.getMap(INPUT, Object.class, Map.of());

            fillOutput(inputParameters, input, output, mappings);
        } else {
            List<Object> input = inputParameters.getList(INPUT, Object.class, List.of());

            for (Object object : input) {
                fillOutput(inputParameters, (Map<String, Object>) object, output, mappings);
            }
        }

        return output;
    }

    private static void fillOutput(
        Parameters inputParameters, Map<String, Object> input, Map<String, Object> output,
        Map<String, Pair<String, Boolean>> mappings) {
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            if (isAllowedToMap(inputParameters, entry)) {
                if (mappings.containsKey(entry.getKey())) {
                    if (mappings.get(entry.getKey())
                        .getRight() != null
                        && mappings.get(entry.getKey())
                            .getRight()) {
                        Objects.requireNonNull(entry.getValue(),
                            "Required field " + entry.getKey() + " cannot be null.");
                        Validate.notBlank(entry.getValue()
                            .toString(),
                            "Required field " + entry.getKey() + " cannot be empty.");
                    }

                    output.put(mappings.get(entry.getKey())
                        .getLeft(),
                        entry.getValue());
                } else if (inputParameters.getBoolean(INCLUDE_UNMAPPED) != null
                    && inputParameters.getBoolean(INCLUDE_UNMAPPED))
                    output.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static boolean isAllowedToMap(Parameters inputParameters, Map.Entry<String, Object> entry) {
        return (inputParameters.getBoolean(INCLUDE_NULLS) == null
            || (inputParameters.getBoolean(INCLUDE_NULLS) != null && (inputParameters.getBoolean(INCLUDE_NULLS) ||
                ObjectUtils.anyNotNull(entry.getValue()))))
            && (inputParameters.getBoolean(INCLUDE_EMPTY_STRINGS) == null
                || (inputParameters.getBoolean(INCLUDE_EMPTY_STRINGS) != null
                    && (inputParameters.getBoolean(INCLUDE_EMPTY_STRINGS) ||
                        ObjectUtils.isNotEmpty(entry.getValue()))));
    }

}
