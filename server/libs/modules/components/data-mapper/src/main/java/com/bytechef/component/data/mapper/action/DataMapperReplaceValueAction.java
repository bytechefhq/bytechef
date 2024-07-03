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
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.nullable;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.time;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.component.data.mapper.model.Mapping;
import com.bytechef.component.data.mapper.model.ObjectMapping;
import com.bytechef.component.data.mapper.util.DataMapperUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.List;

/**
 * @author Ivica Cardic
 * @author Marko Kriskovic
 */
public class DataMapperReplaceValueAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = ComponentDSL.action("replaceValue")
        .title("Replace value")
        .description(
            "Replaces a given value with the specified value defined in mappings. In case there is no mapping specified for the value, it returns the default value, and if there is no default defined, it returns null.")
        .properties(
            integer(TYPE)
                .label("Value type")
                .description("The value type.")
                .required(true)
                .options(
                    option("Array", 1),
                    option("Boolean", 2),
                    option("Date", 3),
                    option("Date Time", 4),
                    option("Integer", 5),
                    option("Number", 7),
                    option("Object", 8),
                    option("String", 9),
                    option("Time", 10))
                .required(true),
            array(VALUE)
                .label(VALUE_LABEL)
                .description(VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition("1"))
                .required(true),
            bool(VALUE)
                .label(VALUE_LABEL)
                .description(VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition("2"))
                .required(true),
            date(VALUE)
                .label(VALUE_LABEL)
                .description(VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition("3"))
                .required(true),
            dateTime(VALUE)
                .label(VALUE_LABEL)
                .description(VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition("4"))
                .required(true),
            integer(VALUE)
                .label(VALUE_LABEL)
                .description(VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition("5"))
                .required(true),
            number(VALUE)
                .label(VALUE_LABEL)
                .description(VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition("7"))
                .required(true),
            object(VALUE)
                .label(VALUE_LABEL)
                .description(VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition("8"))
                .additionalProperties(
                    array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time())
                .required(true),
            string(VALUE)
                .label(VALUE_LABEL)
                .description(VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition("9"))
                .required(true),
            time(VALUE)
                .label(VALUE_LABEL)
                .description(VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition("10"))
                .required(true),
            array(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description(DEFAULT_VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition("1"))
                .required(true),
            bool(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description(DEFAULT_VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition("2"))
                .required(true),
            date(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description(DEFAULT_VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition("3"))
                .required(true),
            dateTime(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description(DEFAULT_VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition("4"))
                .required(true),
            integer(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description(DEFAULT_VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition("5"))
                .required(true),
            nullable(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description(DEFAULT_VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition("6"))
                .required(true),
            number(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description(DEFAULT_VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition("7"))
                .required(true),
            object(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description(DEFAULT_VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition("8"))
                .required(true),
            string(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description(DEFAULT_VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition("9"))
                .required(true),
            time(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description(DEFAULT_VALUE_DESCRIPTION)
                .displayCondition(getDisplayCondition("10"))
                .required(true),
            array(MAPPINGS)
                .label(MAPPINGS_LABEL)
                .description(MAPPINGS_DESCRIPTION)
                .displayCondition(getDisplayCondition("1"))
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
                .displayCondition(getDisplayCondition("2"))
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
                .displayCondition(getDisplayCondition("3"))
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
                .displayCondition(getDisplayCondition("4"))
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
                .displayCondition(getDisplayCondition("5"))
                .items(
                    object().properties(
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
                .displayCondition(getDisplayCondition("7"))
                .items(
                    object().properties(
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
                .displayCondition(getDisplayCondition("8"))
                .items(
                    object().properties(
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
                .displayCondition(getDisplayCondition("9"))
                .items(
                    object().properties(
                        string(FROM)
                            .label(LABEL_FROM)
                            .description(FROM_DESCRIPTION)
                            .required(true),
                        string(TO)
                            .label(LABEL_TO)
                            .description(TO_DESCRIPTION)
                            .required(true))),
            array(MAPPINGS)
                .label(MAPPINGS_LABEL)
                .description(MAPPINGS_DESCRIPTION)
                .displayCondition(getDisplayCondition("10"))
                .items(
                    object().properties(
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
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Class<?> type = DataMapperUtils.getType(inputParameters);

        List<ObjectMapping> mappings = inputParameters.getList(MAPPINGS, ObjectMapping.class, List.of());

        for (Mapping<Object, Object> mapping : mappings) {
            if (ConvertUtils.canConvert(mapping.getFrom(), type)) {
                Object from = ConvertUtils.convertValue(mapping.getFrom(), type);

                if (from.equals(inputParameters.get(VALUE, type))) {
                    return ConvertUtils.convertValue(mapping.getTo(), type);
                }
            }
        }

        return inputParameters.get(DEFAULT_VALUE, type);
    }
}
