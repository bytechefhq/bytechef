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

package com.bytechef.component.data.mapper.action;

import static com.bytechef.component.data.mapper.constant.DataMapperConstants.FROM;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INPUT;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INPUT_TYPE;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.MAPPINGS;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.TO;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.TYPE;
import static com.bytechef.component.data.mapper.constant.InputType.ARRAY;
import static com.bytechef.component.data.mapper.constant.InputType.OBJECT;
import static com.bytechef.component.data.mapper.util.DataMapperUtils.FROM_DESCRIPTION;
import static com.bytechef.component.data.mapper.util.DataMapperUtils.LABEL_FROM;
import static com.bytechef.component.data.mapper.util.DataMapperUtils.LABEL_TO;
import static com.bytechef.component.data.mapper.util.DataMapperUtils.MAPPINGS_DESCRIPTION;
import static com.bytechef.component.data.mapper.util.DataMapperUtils.MAPPINGS_LABEL;
import static com.bytechef.component.data.mapper.util.DataMapperUtils.TO_DESCRIPTION;
import static com.bytechef.component.data.mapper.util.DataMapperUtils.getDisplayCondition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;

import com.bytechef.component.data.mapper.constant.InputType;
import com.bytechef.component.data.mapper.constant.ValueType;
import com.bytechef.component.data.mapper.model.ObjectMapping;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ivica Cardic
 * @author Marko Kriskovic
 */
public class DataMapperReplaceAllSpecifiedValuesAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("replaceAllSpecifiedValues")
        .title("Replace All Specified Values")
        .description("Goes through all object parameters and replaces all specified input parameter values.")
        .properties(
            string(INPUT_TYPE)
                .label("Input Type")
                .description("The input type.")
                .options(
                    option("Object", OBJECT.name()),
                    option("Array", ARRAY.name()))
                .required(true),
            object(INPUT)
                .label("Input")
                .description("An object containing one or more properties.")
                .displayCondition("inputType == '%s'".formatted(OBJECT.name()))
                .required(true),
            array(INPUT)
                .label("Input")
                .description("An array containing one or more objects.")
                .displayCondition("inputType == '%s'".formatted(ARRAY.name()))
                .items(object())
                .required(true),
            string(TYPE)
                .label("Value Type")
                .description("The value type of 'from' and 'to' property values.")
                .required(true)
                .options(
                    option("Array", ValueType.ARRAY.name()),
                    option("Boolean", ValueType.BOOLEAN.name()),
                    option("Date", ValueType.DATE.name()),
                    option("Date Time", ValueType.DATE_TIME.name()),
                    option("Integer", ValueType.INTEGER.name()),
                    option("Number", ValueType.NUMBER.name()),
                    option("Object", ValueType.OBJECT.name()),
                    option("String", ValueType.STRING.name()),
                    option("Time", ValueType.TIME.name()))
                .required(true),
            array(MAPPINGS)
                .label(MAPPINGS_LABEL)
                .description(MAPPINGS_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.ARRAY))
                .items(
                    object().properties(
                        array(FROM)
                            .label(LABEL_FROM)
                            .description(FROM_DESCRIPTION)
                            .required(true),
                        array(TO)
                            .label(LABEL_TO)
                            .description(TO_DESCRIPTION)
                            .required(true))),
            array(MAPPINGS)
                .label(MAPPINGS_LABEL)
                .description(MAPPINGS_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.BOOLEAN))
                .items(
                    object().properties(
                        bool(FROM)
                            .label(LABEL_FROM)
                            .description(FROM_DESCRIPTION)
                            .required(true),
                        bool(TO)
                            .label(LABEL_TO)
                            .description(TO_DESCRIPTION)
                            .required(true))),
            array(MAPPINGS)
                .label(MAPPINGS_LABEL)
                .description(MAPPINGS_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.DATE))
                .items(
                    object()
                        .properties(
                            date(FROM)
                                .label(LABEL_FROM)
                                .description(FROM_DESCRIPTION)
                                .required(true),
                            date(TO)
                                .label(LABEL_TO)
                                .description(TO_DESCRIPTION)
                                .required(true))),
            array(MAPPINGS)
                .label(MAPPINGS_LABEL)
                .description(MAPPINGS_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.DATE_TIME))
                .items(
                    object()
                        .properties(
                            dateTime(FROM)
                                .label(LABEL_FROM)
                                .description(FROM_DESCRIPTION)
                                .required(true),
                            dateTime(TO)
                                .label(LABEL_TO)
                                .description(TO_DESCRIPTION)
                                .required(true))),
            array(MAPPINGS)
                .label(MAPPINGS_LABEL)
                .description(MAPPINGS_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.INTEGER))
                .items(
                    object()
                        .properties(
                            integer(FROM)
                                .label(LABEL_FROM)
                                .description(FROM_DESCRIPTION)
                                .required(true),
                            integer(TO)
                                .label(LABEL_TO)
                                .description(TO_DESCRIPTION)
                                .required(true))),
            array(MAPPINGS)
                .label(MAPPINGS_LABEL)
                .description(MAPPINGS_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.NUMBER))
                .items(
                    object()
                        .properties(
                            number(FROM)
                                .label(LABEL_FROM)
                                .description(FROM_DESCRIPTION)
                                .required(true),
                            number(TO)
                                .label(LABEL_TO)
                                .description(TO_DESCRIPTION)
                                .required(true))),
            array(MAPPINGS)
                .label(MAPPINGS_LABEL)
                .description(MAPPINGS_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.OBJECT))
                .items(
                    object()
                        .properties(
                            object(FROM)
                                .label(LABEL_FROM)
                                .description(FROM_DESCRIPTION)
                                .required(true),
                            object(TO)
                                .label(LABEL_TO)
                                .description(TO_DESCRIPTION)
                                .required(true))),
            array(MAPPINGS)
                .label(MAPPINGS_LABEL)
                .description(MAPPINGS_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.STRING))
                .items(
                    object()
                        .properties(
                            string(FROM)
                                .label(LABEL_FROM)
                                .description("Part of the string value you want to change, defined by regex.")
                                .required(true),
                            string(TO)
                                .label(LABEL_TO)
                                .description("The value you want to change the defined part to, defined by regex.")
                                .required(true))),
            array(MAPPINGS)
                .label(MAPPINGS_LABEL)
                .description(MAPPINGS_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.TIME))
                .items(
                    object()
                        .properties(
                            time(FROM)
                                .label(LABEL_FROM)
                                .description(FROM_DESCRIPTION)
                                .required(true),
                            time(TO)
                                .label(LABEL_TO)
                                .description(TO_DESCRIPTION)
                                .required(true))))
        .output()
        .perform(DataMapperReplaceAllSpecifiedValuesAction::perform);

    private DataMapperReplaceAllSpecifiedValuesAction() {
    }

    @SuppressWarnings("unchecked")
    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        List<ObjectMapping> mappings = inputParameters.getList(MAPPINGS, ObjectMapping.class, List.of());

        Map<Object, Object> mappingMap = mappings.stream()
            .collect(HashMap::new, (map, value) -> map.put(value.getFrom(), value.getTo()), HashMap::putAll);

        InputType inputType = inputParameters.get(INPUT_TYPE, InputType.class);
        ValueType mappingType = inputParameters.getRequired(TYPE, ValueType.class);

        if (inputType == OBJECT) {
            Map<String, Object> input = inputParameters.getMap(INPUT, Object.class, Map.of());

            return fillOutput(mappingType, input, mappingMap);
        } else {
            List<Object> input = inputParameters.getList(INPUT, Object.class, List.of());
            List<Map<String, Object>> output = new LinkedList<>();

            for (Object object : input) {
                output.add(fillOutput(mappingType, (Map<String, Object>) object, mappingMap));
            }

            return output;
        }
    }

    private static Map<String, Object> fillOutput(
        ValueType mappingType, Map<String, Object> inputMap, Map<Object, Object> mappingMap) {

        Map<String, Object> outputMap = new HashMap<>(inputMap);

        if (mappingType == ValueType.STRING) {
            for (Map.Entry<Object, Object> entry : mappingMap.entrySet()) {
                Set<Map.Entry<String, Object>> entries = outputMap.entrySet();

                for (Map.Entry<String, Object> outputEntry : entries) {
                    Object inputValue = inputMap.get(outputEntry.getKey());
                    Object key = entry.getKey();
                    Object value = entry.getValue();

                    Pattern pattern = Pattern.compile(key.toString());
                    Matcher matcher = pattern.matcher(inputValue.toString());

                    outputEntry.setValue(matcher.replaceAll(value.toString()));
                }
            }
        } else {
            for (Map.Entry<String, Object> entry : inputMap.entrySet()) {
                if (mappingMap.containsKey(entry.getValue())) {
                    outputMap.put(entry.getKey(), mappingMap.get(entry.getValue()));
                }
            }
        }

        return outputMap;
    }
}
