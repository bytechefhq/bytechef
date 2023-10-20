
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

import com.bytechef.hermes.component.ActionContext;
import com.bytechef.hermes.component.definition.ActionDefinition;

import java.util.Map;

import static com.bytechef.component.datastorage.constant.DataStorageConstants.DEFAULT_VALUE;
import static com.bytechef.component.datastorage.constant.DataStorageConstants.KEY;
import static com.bytechef.component.datastorage.constant.DataStorageConstants.SCOPE;
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
public class DataStorageGetValueAction {

    public static final ActionDefinition ACTION_DEFINITION = action("getValue")
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
                    "The namespace to get a value from. The value should have been previously accessible, either in the present workflow execution, or the workflow itself for all the executions, or the user account for all the workflows the user has.")
                .options(SCOPE_OPTIONS)
                .required(true),
            oneOf(DEFAULT_VALUE).types(array(), bool(), integer(), number(), object(), string(), nullable())
                .label("Default value")
                .description("The default value to return if no value exists under the given key.")
                .required(true))
        .execute(DataStorageGetValueAction::execute);

    protected static Object execute(ActionContext actionContext, Map<String, ?> inputParameters) {
        System.out.println(actionContext.toString());

        return null;
    }
}
