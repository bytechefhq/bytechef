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

package com.bytechef.component.data.storage.action;

import static com.bytechef.component.data.storage.constant.DataStorageConstants.DEFAULT_VALUE;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.DEFAULT_VALUE_LABEL;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.KEY;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.SCOPE;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.SCOPE_OPTIONS;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.TYPE;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.TYPE_OPTIONS;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.nullable;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.component.data.storage.constant.ValueType;
import com.bytechef.component.data.storage.util.DataStorageUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Data.Scope;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class DataStorageGetValueAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getValue")
        .title("Get Value")
        .description("Retrieve a previously assigned value within the specified scope using its corresponding key.")
        .properties(
            string(KEY)
                .label("Key")
                .description("The identifier of a value to get, stored earlier in the selected scope.")
                .required(true),
            string(SCOPE)
                .label("Scope")
                .description(
                    "The namespace to get a value from. The value should have been previously accessible, " +
                        "either in the present workflow execution, or the workflow itself for all the executions, " +
                        "or the user account for all the workflows the user has.")
                .options(SCOPE_OPTIONS)
                .required(true),
            string(TYPE)
                .label("Type")
                .description("The value type.")
                .options(TYPE_OPTIONS),
            array(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == '%s'".formatted(ValueType.ARRAY))
                .required(true),
            bool(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == '%s'".formatted(ValueType.BOOLEAN))
                .required(true),
            date(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == '%s'".formatted(ValueType.DATE))
                .required(true),
            dateTime(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == '%s'".formatted(ValueType.DATE_TIME))
                .required(true),
            integer(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == '%s'".formatted(ValueType.INTEGER))
                .required(true),
            nullable(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == '%s'".formatted(ValueType.NULL))
                .required(true),
            number(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == '%s'".formatted(ValueType.NUMBER))
                .required(true),
            object(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == '%s'".formatted(ValueType.OBJECT))
                .required(true),
            string(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == '%s'".formatted(ValueType.STRING))
                .required(true),
            time(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == '%s'".formatted(ValueType.TIME))
                .required(true))
        .output()
        .perform(DataStorageGetValueAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context)
        throws ClassNotFoundException {

        Class<?> type = DataStorageUtils.getType(inputParameters);

        if (type == null) {
            return null;
        }

        Optional<Object> optional = context.data(data -> data.fetch(
            Scope.valueOf(inputParameters.getRequiredString(SCOPE)), inputParameters.getRequiredString(KEY)));

        if (optional.isEmpty()) {
            if (ConvertUtils.canConvert(inputParameters.getRequired(DEFAULT_VALUE), type)) {
                return ConvertUtils.convertValue(inputParameters.getRequired(DEFAULT_VALUE), type);
            }

            return inputParameters.getRequired(DEFAULT_VALUE);
        }

        if (ConvertUtils.canConvert(optional.get(), type)) {
            return ConvertUtils.convertValue(optional.get(), type);
        }

        return optional.get();
    }
}
