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
import com.bytechef.component.definition.Property.ControlType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class DataStorageAppendValueToListAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("appendValueToList")
        .title("Append Value to List")
        .description("Append value to the end of a list. If the list does not exist, it will be created.")
        .properties(
            string(SCOPE)
                .label("Scope")
                .description("The namespace for appending a value.")
                .options(SCOPE_OPTIONS)
                .required(true),
            string(KEY)
                .label("Key")
                .description(
                    "The identifier of a list must be unique within the chosen scope, or a new value will " +
                        "overwrite the existing one.")
                .required(true),
            string(TYPE)
                .label("Type")
                .description("The value type.")
                .options(TYPE_OPTIONS)
                .required(true),
            array(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type == '%s'".formatted(ValueType.ARRAY))
                .required(true),
            bool(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type == '%s'".formatted(ValueType.BOOLEAN))
                .required(true),
            date(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type == '%s'".formatted(ValueType.DATE))
                .required(true),
            dateTime(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type == '%S'".formatted(ValueType.DATE_TIME))
                .required(true),
            integer(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type == '%s'".formatted(ValueType.INTEGER))
                .required(true),
            nullable(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type == '%s'".formatted(ValueType.NULL))
                .required(true),
            number(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type == '%s'".formatted(ValueType.NUMBER))
                .required(true),
            object(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type == '%s'".formatted(ValueType.OBJECT))
                .required(true),
            string(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .controlType(ControlType.TEXT_AREA)
                .displayCondition("type == '%s'".formatted(ValueType.STRING))
                .required(true),
            time(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type == '%s'".formatted(ValueType.TIME))
                .required(true))
        .help("", "https://docs.bytechef.io/reference/components/data-storage_v1#append-value-to-list")
        .perform(DataStorageAppendValueToListAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Scope scope = Scope.valueOf(inputParameters.getRequiredString(SCOPE));
        String key = inputParameters.getRequiredString(KEY);
        Object value = DataStorageUtils.getValue(inputParameters);

        context
            .data(data -> data.update(scope, key, (Optional<Object> currentValue) -> appendValue(currentValue, value)));

        return null;
    }

    private static List<Object> appendValue(Optional<Object> currentValue, Object value) {
        List<Object> list = new ArrayList<>();

        if (currentValue.isPresent() && currentValue.get() instanceof List<?> currentList) {
            list.addAll(currentList);
        }

        if (value instanceof Object[] values) {
            list.addAll(Arrays.asList(values));
        } else {
            list.add(value);
        }

        return list;
    }
}
