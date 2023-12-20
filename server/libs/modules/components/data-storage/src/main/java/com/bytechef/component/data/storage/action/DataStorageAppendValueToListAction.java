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

import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.date;
import static com.bytechef.hermes.component.definition.ComponentDSL.dateTime;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.nullable;
import static com.bytechef.hermes.component.definition.ComponentDSL.number;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.definition.ComponentDSL.time;

import com.bytechef.component.data.storage.constant.DataStorageConstants;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.Parameters;

/**
 * @author Ivica Cardic
 */
public class DataStorageAppendValueToListAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("appendValueToList")
        .title("Append Value to List")
        .description("Append value to the end of a list. If the list does not exist, it will be created.")
        .properties(
            integer(DataStorageConstants.SCOPE)
                .label("Scope")
                .description("The namespace for appending a value.")
                .options(DataStorageConstants.SCOPE_OPTIONS)
                .required(true),
            string(DataStorageConstants.KEY)
                .label("Key")
                .description(
                    "The identifier of a list must be unique within the chosen scope, or a new value will overwrite the existing one.")
                .required(true),
            integer(DataStorageConstants.TYPE)
                .label("Type")
                .description("The value type.")
                .options(DataStorageConstants.TYPE_OPTIONS),
            array(DataStorageConstants.VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type === 1")
                .required(true),
            bool(DataStorageConstants.VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type === 2")
                .required(true),
            date(DataStorageConstants.VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type === 3")
                .required(true),
            dateTime(DataStorageConstants.VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type === 4")
                .required(true),
            integer(DataStorageConstants.VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type === 5")
                .required(true),
            nullable(DataStorageConstants.VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type === 6")
                .required(true),
            number(DataStorageConstants.VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type === 7")
                .required(true),
            object(DataStorageConstants.VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type === 8")
                .required(true),
            string(DataStorageConstants.VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type === 9")
                .required(true),
            time(DataStorageConstants.VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type === 10")
                .required(true),
            bool(DataStorageConstants.CREATE_VALUE_IF_MISSING)
                .label("Create value if missing")
                .description(
                    "When the specified list doesn't exist, it will be created with the provided value during the append operation."),
            bool(DataStorageConstants.APPEND_LIST_AS_SINGLE_ITEM)
                .label("Append a list as a single item")
                .description(
                    "When set to true, and the value is a list, it will be added as a single value rather than concatenating the lists."))
        .perform(DataStorageAppendValueToListAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        // TODO

        return null;
    }
}
