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
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.data.mapper.model.Pair;
import com.bytechef.component.data.mapper.model.RequiredStringMapping;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 * @author Marko Kriskovic
 */
public class DataMapperMapObjectsToObjectAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("mapObjectsToObject")
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
                                .label("Path From")
                                .description(
                                    "Path to the input property key that you want to put in the newly created object, written in dot notation."),
                            string(TO)
                                .label("To")
                                .description(
                                    "Name of the key you want to assign to the input property value in the newly created object."),
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

    private DataMapperMapObjectsToObjectAction() {
    }

    @SuppressWarnings("unchecked")
    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        List<RequiredStringMapping> mappings = inputParameters.getList(
            MAPPINGS, RequiredStringMapping.class, List.of());

        Map<String, Pair<String, Boolean>> mappingMap = mappings.stream()
            .collect(
                Collectors.toMap(
                    RequiredStringMapping::getFrom, value -> Pair.create(value.getTo(), value.isRequiredField())));

        Integer inputType = inputParameters.getInteger(INPUT_TYPE);

        if (inputType != null && inputType.equals(1)) {
            DocumentContext input = JsonPath.parse(inputParameters.getMap(INPUT, Object.class, Map.of()));

            return fillOutput(inputParameters, input, mappingMap, context);
        } else {
            List<Object> input = inputParameters.getList(INPUT, Object.class, List.of());

            List<Object> output = new LinkedList<>();
            for (Object object : input) {
                DocumentContext inputObj = JsonPath.parse(object);

                Map<String, Object> map = fillOutput(inputParameters, inputObj, mappingMap, context);
                if (!map.isEmpty())
                    output.add(map);
            }
            return output;
        }
    }

    private static Map<String, Object> fillOutput(
        Parameters inputParameters, DocumentContext input, Map<String, Pair<String, Boolean>> mappingMap,
        ActionContext context) {
        Map<String, Object> output = new LinkedHashMap<>();
        if (inputParameters.getBoolean(INCLUDE_UNMAPPED) != null && inputParameters.getBoolean(INCLUDE_UNMAPPED)) {
            output = input.read("$");
        }

        for (Map.Entry<String, Pair<String, Boolean>> pair : mappingMap.entrySet()) {
            Object value = null;
            try {
                value = input.read(pair.getKey());

                if (isAllowedToMap(inputParameters, value)) {
                    if (pair.getValue()
                        .getRight() != null && pair.getValue()
                            .getRight()) {
                        Objects.requireNonNull(value, "Required field " + pair.getKey() + " cannot be null.");
                        Validate.notBlank(value.toString(), "Required field " + pair.getKey() + " cannot be empty.");
                    }

                    output.put(pair.getValue()
                        .getLeft(), value);
                }
            } catch (PathNotFoundException exception) {
                context.logger(logger -> logger.info(exception.getMessage()));
            }
        }

        return output;
    }

    private static boolean isAllowedToMap(Parameters inputParameters, Object value) {
        return (inputParameters.getBoolean(INCLUDE_NULLS) == null ||
            (inputParameters.getBoolean(INCLUDE_NULLS) != null && (inputParameters.getBoolean(INCLUDE_NULLS) ||
                ObjectUtils.anyNotNull(value))))
            &&
            (inputParameters.getBoolean(INCLUDE_EMPTY_STRINGS) == null ||
                (inputParameters.getBoolean(INCLUDE_EMPTY_STRINGS) != null &&
                    (inputParameters.getBoolean(INCLUDE_EMPTY_STRINGS) || ObjectUtils.isNotEmpty(value))));
    }
}
