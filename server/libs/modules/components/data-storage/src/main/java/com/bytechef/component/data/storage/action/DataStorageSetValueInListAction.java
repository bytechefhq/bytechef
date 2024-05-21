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

import static com.bytechef.component.data.storage.constant.DataStorageConstants.INDEX;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.KEY;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.SCOPE;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.SCOPE_OPTIONS;
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

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class DataStorageSetValueInListAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("setValueInList")
        .title("Set Value in List")
        .description("Set value under a specified index in a list.")
        .properties(
            string(KEY)
                .label("Key")
                .description(
                    "The identifier of a list. Must be unique across all keys within the chosen scope to prevent overwriting the existing value with a new one. Also, it must be less than 1024 bytes in length.")
                .required(true),
            string(SCOPE)
                .label("Scope")
                .description(
                    "The namespace to set a value in. The value should have been previously accessible, either in the present workflow execution, or the workflow itself for all the executions, or the user account for all the workflows the user has.")
                .options(SCOPE_OPTIONS)
                .required(true),
            integer(INDEX)
                .label("Index")
                .description("The index in a list to set a value under. The previous value will be overridden.")
                .required(true),
            integer(TYPE)
                .label("Type")
                .description("The value type.")
                .options(TYPE_OPTIONS),
            array(VALUE)
                .label("Value")
                .description("The value to set under the specified list's key.")
                .displayCondition("type == 1")
                .required(true),
            bool(VALUE)
                .label("Value")
                .description("The value to set under the specified list's key.")
                .displayCondition("type == 2")
                .required(true),
            date(VALUE)
                .label("Value")
                .description("The value to set under the specified list's key.")
                .displayCondition("type == 3")
                .required(true),
            dateTime(VALUE)
                .label("Value")
                .description("The value to set under the specified list's key.")
                .displayCondition("type == 4")
                .required(true),
            integer(VALUE)
                .label("Value")
                .description("The value to set under the specified key.")
                .displayCondition("type == 5")
                .required(true),
            nullable(VALUE)
                .label("Value")
                .description("The value to set under the specified key.")
                .displayCondition("type == 6")
                .required(true),
            number(VALUE)
                .label("Value")
                .description("The value to set under the specified list's key.")
                .displayCondition("type == 7")
                .required(true),
            object(VALUE)
                .label("Value")
                .description("The value to set under the specified list's key.")
                .displayCondition("type == 8")
                .required(true),
            string(VALUE)
                .label("Value")
                .description("The value to set under the specified list's key.")
                .displayCondition("type == 9")
                .required(true),
            time(VALUE)
                .label("Value")
                .description("The value to set under the specified list's key.")
                .displayCondition("type == 10")
                .required(true))
        .perform(DataStorageSetValueInListAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Object value = null;
        switch (inputParameters.getRequiredInteger(TYPE)) {
            case 1:
                value = inputParameters.getRequiredArray(VALUE);
                break;
            case 2:
                value = inputParameters.getRequiredBoolean(VALUE);
                break;
            case 3:
                value = inputParameters.getRequiredLocalDate(VALUE);
                break;
            case 4:
                value = inputParameters.getRequiredLocalDateTime(VALUE);
                break;
            case 5:
                value = inputParameters.getRequiredInteger(VALUE);
                break;
            case 6:
                value = nullable();
                break;
            case 7:
                value = inputParameters.getRequiredDouble(VALUE);
                break;
            case 8:
                value = inputParameters.getRequiredMap(VALUE);
                break;
            case 9:
                value = inputParameters.getRequiredString(VALUE);
                break;
            case 10:
                value = inputParameters.getRequiredLocalTime(VALUE);
                break;
            default:
                break;
        }

        List<Object> list = null;
        Optional<Object> optionalList = context
            .data(data -> data.fetchValue(ActionContext.Data.Scope.valueOf(inputParameters.getRequiredString(SCOPE)),
                inputParameters.getRequiredString(KEY)));
        if (optionalList.isPresent() && optionalList.get() instanceof List)
            list = (List<Object>) optionalList.get();
        else
            return null;

        list.set(inputParameters.getRequiredInteger(INDEX), value);

        List<Object> finalList = list;
        return context
            .data(data -> data.setValue(ActionContext.Data.Scope.valueOf(inputParameters.getRequiredString(SCOPE)),
                inputParameters.getRequiredString(KEY), finalList));
    }
}
