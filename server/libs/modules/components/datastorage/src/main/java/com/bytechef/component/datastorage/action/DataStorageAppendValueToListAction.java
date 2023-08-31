
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.component.datastorage.action;

import com.bytechef.component.datastorage.constant.DataStorageConstants;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;

import java.util.Map;

import static com.bytechef.component.datastorage.constant.DataStorageConstants.APPEND_LIST_AS_SINGLE_ITEM;
import static com.bytechef.component.datastorage.constant.DataStorageConstants.CREATE_VALUE_IF_MISSING;
import static com.bytechef.component.datastorage.constant.DataStorageConstants.SCOPE;
import static com.bytechef.component.datastorage.constant.DataStorageConstants.SCOPE_OPTIONS;
import static com.bytechef.component.datastorage.constant.DataStorageConstants.TYPE;
import static com.bytechef.component.datastorage.constant.DataStorageConstants.TYPE_OPTIONS;
import static com.bytechef.component.datastorage.constant.DataStorageConstants.VALUE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.bool;

import static com.bytechef.hermes.definition.DefinitionDSL.date;
import static com.bytechef.hermes.definition.DefinitionDSL.dateTime;
import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.nullable;
import static com.bytechef.hermes.definition.DefinitionDSL.number;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;
import static com.bytechef.hermes.definition.DefinitionDSL.time;

/**
 * @author Ivica Cardic
 */
public class DataStorageAppendValueToListAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("appendValueToList")
        .title("Append Value to List")
        .description("Append value to the end of a list. If the list does not exist, it will be created.")
        .properties(
            integer(SCOPE)
                .label("Scope")
                .description("The namespace for appending a value.")
                .options(SCOPE_OPTIONS)
                .required(true),
            string(DataStorageConstants.KEY)
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
                .displayCondition("type === 1")
                .required(true),
            bool(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type === 2")
                .required(true),
            date(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type === 3")
                .required(true),
            dateTime(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type === 4")
                .required(true),
            integer(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type === 5")
                .required(true),
            nullable(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type === 6")
                .required(true),
            number(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type === 7")
                .required(true),
            object(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type === 8")
                .required(true),
            string(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type === 9")
                .required(true),
            time(VALUE)
                .label("Value")
                .description("The value to set under given key.")
                .displayCondition("type === 10")
                .required(true),
            bool(CREATE_VALUE_IF_MISSING)
                .label("Create value if missing")
                .description(
                    "When the specified list doesn't exist, it will be created with the provided value during the append operation."),
            bool(APPEND_LIST_AS_SINGLE_ITEM)
                .label("Append a list as a single item")
                .description(
                    "When set to true, and the value is a list, it will be added as a single value rather than concatenating the lists."))
        .perform(DataStorageAppendValueToListAction::perform);

    protected static Object perform(Map<String, ?> inputParameters, ActionDefinition.ActionContext actionContext) {
        // TODO

        return null;
    }
}
