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

package com.bytechef.component.data.storage.action;

import static com.bytechef.component.data.storage.constant.DataStorageConstants.KEY;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.SCOPE;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.SCOPE_OPTIONS;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.TYPE;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.TYPE_OPTIONS;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.VALUE;
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

import com.bytechef.component.data.storage.constant.ValueType;
import com.bytechef.component.data.storage.util.DataStorageUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Data.Scope;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;

/**
 * @author Ivica Cardic
 */
public class DataStorageSetValueAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("setValue")
        .title("Set Value")
        .description("Set a value under a key, in the specified scope.")
        .properties(
            string(KEY)
                .label("Key")
                .description(
                    "The identifier of a value. Must be unique across all keys within the chosen scope to prevent " +
                        "overwriting the existing value with a new one. Also, it must be less than 1024 bytes in " +
                        "length.")
                .required(true),
            string(SCOPE)
                .label("Scope")
                .description(
                    "The namespace to set a value in. The value should have been previously accessible, either in " +
                        "the present workflow execution, or the workflow itself for all the executions, or the user " +
                        "account for all the workflows the user has.")
                .options(SCOPE_OPTIONS)
                .required(true),
            string(TYPE)
                .label("Type")
                .description("The value type.")
                .options(TYPE_OPTIONS)
                .required(true),
            array(VALUE)
                .label("Value")
                .description("The value to set under the specified key.")
                .displayCondition("type == '%s'".formatted(ValueType.ARRAY))
                .required(true),
            bool(VALUE)
                .label("Value")
                .description("The value to set under the specified key.")
                .displayCondition("type == '%s'".formatted(ValueType.BOOLEAN))
                .required(true),
            date(VALUE)
                .label("Value")
                .description("The value to set under the specified key.")
                .displayCondition("type == '%s'".formatted(ValueType.DATE))
                .required(true),
            dateTime(VALUE)
                .label("Value")
                .description("The value to set under the specified key.")
                .displayCondition("type == '%s'".formatted(ValueType.DATE_TIME))
                .required(true),
            integer(VALUE)
                .label("Value")
                .description("The value to set under the specified key.")
                .displayCondition("type == '%s'".formatted(ValueType.INTEGER))
                .required(true),
            nullable(VALUE)
                .label("Value")
                .description("The value to set under the specified key.")
                .displayCondition("type == '%s'".formatted(ValueType.NULL))
                .required(true),
            number(VALUE)
                .label("Value")
                .description("The value to set under the specified key.")
                .displayCondition("type == '%s'".formatted(ValueType.NUMBER))
                .required(true),
            object(VALUE)
                .label("Value")
                .description("The value to set under the specified key.")
                .displayCondition("type == '%s'".formatted(ValueType.OBJECT))
                .required(true),
            string(VALUE)
                .label("Value")
                .description("The value to set under the specified key.")
                .displayCondition("type == '%s'".formatted(ValueType.STRING))
                .required(true),
            time(VALUE)
                .label("Value")
                .description("The value to set under the specified key.")
                .displayCondition("type == '%s'".formatted(ValueType.TIME))
                .required(true))
        .perform(DataStorageSetValueAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        context.data(data -> data.put(
            Scope.valueOf(inputParameters.getRequiredString(SCOPE)), inputParameters.getRequiredString(KEY),
            DataStorageUtils.getValue(inputParameters)));

        return null;
    }
}
