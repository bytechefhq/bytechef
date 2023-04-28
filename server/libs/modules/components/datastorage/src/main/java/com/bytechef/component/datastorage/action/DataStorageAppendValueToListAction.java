
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
import com.bytechef.hermes.component.ActionContext;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.definition.ActionDefinition;

import static com.bytechef.component.datastorage.constant.DataStorageConstants.SCOPE_OPTIONS;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.bool;

import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.nullable;
import static com.bytechef.hermes.definition.DefinitionDSL.number;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.oneOf;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class DataStorageAppendValueToListAction {

    public static final ActionDefinition ACTION_DEFINITION = action("appendValueToList")
        .title("Append Value to List")
        .description("Append value to the end of a list. If the list does not exist, it will be created.")
        .properties(
            string(DataStorageConstants.KEY)
                .label("Key")
                .description(
                    "The identifier of a list must be unique within the chosen scope, or a new value will overwrite the existing one.")
                .required(true),
            string(DataStorageConstants.SCOPE)
                .label("Scope")
                .description("The namespace for appending a value.")
                .options(SCOPE_OPTIONS)
                .required(true),
            oneOf(DataStorageConstants.VALUE)
                .types(array(), bool(), integer(), number(), object(), string(), nullable())
                .label("Value")
                .description("The value to set under given key.")
                .required(true),
            bool(DataStorageConstants.CREATE_VALUE_IF_MISSING)
                .label("Create value if missing")
                .description(
                    "When the specified list doesn't exist, it will be created with the provided value during the append operation."),
            bool(DataStorageConstants.APPEND_LIST_AS_SINGLE_ITEM)
                .label("Append a list as a single item")
                .description(
                    "When set to true, and the value is a list, it will be added as a single value rather than concatenating the lists."))
        .execute(DataStorageAppendValueToListAction::execute);

    protected static Object execute(ActionContext actionContext, InputParameters inputParameters) {
        System.out.println(actionContext);

        return null;
    }
}
