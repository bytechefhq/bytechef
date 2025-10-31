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

import static com.bytechef.component.data.storage.constant.DataStorageConstants.DEFAULT_VALUE;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.DEFAULT_VALUE_LABEL;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.KEY;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.SCOPE;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.SCOPE_OPTIONS;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.TIMEOUT;
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

import com.bytechef.component.data.storage.constant.ValueType;
import com.bytechef.component.data.storage.util.DataStorageUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Data.Scope;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class DataStorageAwaitGetValueAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("awaitGetValue")
        .title("Await Get Value")
        .description("Wait for a value under a specified key, until it's available.")
        .properties(
            string(KEY)
                .label("Key")
                .description("The identifier of a value to wait for.")
                .required(true),
            string(SCOPE)
                .label("Scope")
                .description("The namespace to obtain a value from.")
                .options(SCOPE_OPTIONS)
                .required(true),
            string(TYPE)
                .label("Type")
                .description("The value type.")
                .options(TYPE_OPTIONS)
                .required(true),
            array(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == '%s'".formatted(ValueType.ARRAY))
                .required(false),
            bool(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == '%s'".formatted(ValueType.BOOLEAN))
                .required(false),
            date(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == '%s'".formatted(ValueType.DATE))
                .required(false),
            dateTime(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == '%s'".formatted(ValueType.DATE_TIME))
                .required(false),
            integer(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == '%s'".formatted(ValueType.INTEGER))
                .required(false),
            nullable(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == '%s'".formatted(ValueType.NULL))
                .required(false),
            number(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == '%s'".formatted(ValueType.NUMBER))
                .required(false),
            object(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == '%s'".formatted(ValueType.OBJECT))
                .required(false),
            string(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == '%s'".formatted(ValueType.STRING))
                .required(false),
            time(DEFAULT_VALUE)
                .label(DEFAULT_VALUE_LABEL)
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == '%s'".formatted(ValueType.TIME))
                .required(false),
            integer(TIMEOUT)
                .label("Timeout")
                .description(
                    "If a value is not found within the specified time, the action returns a null value. Therefore, " +
                        "the maximum wait time should be set accordingly.")
                .minValue(1)
                .maxValue(300)
                .required(true))
        .output(DataStorageAwaitGetValueAction::output)
        .perform(DataStorageAwaitGetValueAction::perform);

    protected static OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        ValueProperty<?> property = DataStorageUtils.getValueProperty(
            inputParameters.getRequired(TYPE, ValueType.class));

        return OutputResponse.of(property, null);
    }

    protected static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context)
        throws InterruptedException {

        Class<?> type = DataStorageUtils.getType(inputParameters);

        if (type == null) {
            return null;
        }

        Optional<Object> optional = Optional.empty();

        for (int i = 0; i < inputParameters.getRequiredInteger(TIMEOUT); i = i + 5) {
            optional = context.data(data -> data.fetch(
                Scope.valueOf(inputParameters.getRequiredString(SCOPE)), inputParameters.getRequiredString(KEY)));

            if (optional.isPresent()) {
                break;
            }

            Thread.sleep(5000);
        }

        if (optional.isEmpty()) {
            if (inputParameters.containsKey(DEFAULT_VALUE) &&
                context.convert(convert -> convert.canConvert(inputParameters.get(DEFAULT_VALUE), type))) {

                return context.convert(convert -> convert.value(inputParameters.get(DEFAULT_VALUE), type));
            }

            return inputParameters.getString(DEFAULT_VALUE);
        }

        Optional<Object> finalOptional = optional;

        if (context.convert(convert -> convert.canConvert(finalOptional.get(), type))) {
            return context.convert(convert -> convert.value(finalOptional.get(), type));
        }

        return optional.get();
    }
}
