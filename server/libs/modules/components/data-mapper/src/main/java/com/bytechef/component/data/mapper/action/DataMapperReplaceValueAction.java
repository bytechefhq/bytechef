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

import static com.bytechef.component.data.mapper.constant.DataMapperConstants.DEFAULT_VALUE;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.FROM;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.MAPPINGS;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.TO;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.TYPE;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.VALUE;
import static com.bytechef.component.data.mapper.util.DataMapperUtils.DEFAULT_VALUE_DESCRIPTION;
import static com.bytechef.component.data.mapper.util.DataMapperUtils.DEFAULT_VALUE_LABEL;
import static com.bytechef.component.data.mapper.util.DataMapperUtils.FROM_DESCRIPTION;
import static com.bytechef.component.data.mapper.util.DataMapperUtils.LABEL_FROM;
import static com.bytechef.component.data.mapper.util.DataMapperUtils.LABEL_TO;
import static com.bytechef.component.data.mapper.util.DataMapperUtils.MAPPINGS_DESCRIPTION;
import static com.bytechef.component.data.mapper.util.DataMapperUtils.MAPPINGS_LABEL;
import static com.bytechef.component.data.mapper.util.DataMapperUtils.TO_DESCRIPTION;
import static com.bytechef.component.data.mapper.util.DataMapperUtils.VALUE_DESCRIPTION;
import static com.bytechef.component.data.mapper.util.DataMapperUtils.VALUE_LABEL;
import static com.bytechef.component.data.mapper.util.DataMapperUtils.getDisplayCondition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.nullable;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;

import com.bytechef.component.data.mapper.constant.ValueType;
import com.bytechef.component.data.mapper.model.Mapping;
import com.bytechef.component.data.mapper.model.ObjectMapping;
import com.bytechef.component.data.mapper.util.DataMapperUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ivica Cardic
 * @author Marko Kriskovic
 */
public class DataMapperReplaceValueAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("replaceValue")
        .title("Replace Value")
        .description(
            "Replaces a given value with the specified value defined in mappings. In case there is no mapping " +
                "specified for the value, it returns the default value, and if there is no default defined, it " +
                "returns null. You can also change a string value with regex.")
        .properties(
            string(TYPE)
                .label("Value Type")
                .description("The value type.")
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
            array(VALUE)
                .label(VALUE_LABEL)
                .description(VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.ARRAY))
                .required(true),
            bool(VALUE)
                .label(VALUE_LABEL)
                .description(VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.BOOLEAN))
                .required(true),
            date(VALUE)
                .label(VALUE_LABEL)
                .description(VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.DATE))
                .required(true),
            dateTime(VALUE)
                .label(VALUE_LABEL)
                .description(VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.DATE_TIME))
                .required(true),
            integer(VALUE)
                .label(VALUE_LABEL)
                .description(VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.INTEGER))
                .required(true),
            number(VALUE)
                .label(VALUE_LABEL)
                .description(VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.NUMBER))
                .required(true),
            object(VALUE)
                .label(VALUE_LABEL)
                .description(VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.OBJECT))
                .additionalProperties(
                    array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time())
                .required(true),
            string(VALUE)
                .label(VALUE_LABEL)
                .description(VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.STRING))
                .required(true),
            time(VALUE)
                .label(VALUE_LABEL)
                .description(VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.TIME))
                .required(true),
            array(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description(DEFAULT_VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.ARRAY))
                .required(true),
            bool(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description(DEFAULT_VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.BOOLEAN))
                .required(true),
            date(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description(DEFAULT_VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.DATE))
                .required(true),
            dateTime(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description(DEFAULT_VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.DATE_TIME))
                .required(true),
            integer(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description(DEFAULT_VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.INTEGER))
                .required(true),
            nullable(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description(DEFAULT_VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.NULL))
                .required(true),
            number(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description(DEFAULT_VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.NUMBER))
                .required(true),
            object(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description(DEFAULT_VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.OBJECT))
                .required(true),
            string(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description(DEFAULT_VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.STRING))
                .required(true),
            time(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description(DEFAULT_VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition(ValueType.TIME))
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
                    object().properties(
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
                    object().properties(
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
        .perform(DataMapperReplaceValueAction::perform);

    private DataMapperReplaceValueAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Class<?> type = DataMapperUtils.getType(inputParameters);
        List<ObjectMapping> mappings = inputParameters.getList(MAPPINGS, ObjectMapping.class, List.of());

        for (Mapping<Object, Object> mapping : mappings) {
            Object mappingFrom = mapping.getFrom();

            if (DataMapperUtils.canConvert(context, mappingFrom, type)) {
                Object mappingTo = mapping.getTo();

                if (type.equals(String.class)) {
                    String value = inputParameters.getString(VALUE);

                    Pattern pattern = Pattern.compile(mappingFrom.toString());
                    Matcher matcher = pattern.matcher(value);

                    return matcher.replaceAll(mappingTo.toString());
                } else {
                    Object from = DataMapperUtils.convertFrom(context, mappingFrom, type);

                    if (from.equals(inputParameters.get(VALUE, type))) {
                        return DataMapperUtils.convertTo(context, mappingTo, type);
                    }
                }
            }
        }

        return inputParameters.get(DEFAULT_VALUE, type);
    }
}
