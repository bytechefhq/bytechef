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

import static com.bytechef.component.data.storage.constant.DataStorageConstants.APPEND_LIST_AS_SINGLE_ITEM;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.KEY;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.SCOPE;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.TYPE;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.TYPE_OPTIONS;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.VALUE;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.nullable;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.time;

import com.bytechef.component.data.storage.constant.DataStorageConstants;
import com.bytechef.component.data.storage.util.DataStorageUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Data.Scope;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
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
                .options(DataStorageConstants.SCOPE_OPTIONS)
                .required(true),
            string(KEY)
                .label("Key")
                .description(
                    "The identifier of a list must be unique within the chosen scope, or a new value will overwrite the existing one.")
                .required(true),
            integer(TYPE)
                .label("Type")
                .description("The value type.")
                .options(TYPE_OPTIONS),
            array(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type == 1")
                .required(true),
            bool(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type == 2")
                .required(true),
            date(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type == 3")
                .required(true),
            dateTime(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type == 4")
                .required(true),
            integer(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type == 5")
                .required(true),
            nullable(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type == 6")
                .required(true),
            number(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type == 7")
                .required(true),
            object(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type == 8")
                .required(true),
            string(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type == 9")
                .required(true),
            time(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type == 10")
                .required(true),
            bool(APPEND_LIST_AS_SINGLE_ITEM)
                .label("Append a list as a single item")
                .description(
                    "When set to true, and the value is a list, it will be added as a single value rather than concatenating the lists."))
        .perform(DataStorageAppendValueToListAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context.data(data -> data.setValue(
            Scope.valueOf(inputParameters.getRequiredString(SCOPE)), inputParameters.getRequiredString(KEY),
            getValues(inputParameters, context, DataStorageUtils.getValue(inputParameters))));
    }

    @SuppressWarnings("unchecked")
    private static List<Object> getValues(Parameters inputParameters, ActionContext context, Object value) {
        List<Object> list;
        Optional<Object> optionalList = context.data(data -> data.fetchValue(
            Scope.valueOf(inputParameters.getRequiredString(SCOPE)), inputParameters.getRequiredString(KEY)));

        if (optionalList.isPresent() && optionalList.get() instanceof List<?> curList) {
            list = (List<Object>) curList;
        } else {
            list = new ArrayList<>();
        }

        if (value instanceof Object[] && !inputParameters.getRequiredBoolean(APPEND_LIST_AS_SINGLE_ITEM)) {
            list.addAll(Arrays.asList((Object[]) value));
        } else {
            list.add(value);
        }

        return list;
    }
}
