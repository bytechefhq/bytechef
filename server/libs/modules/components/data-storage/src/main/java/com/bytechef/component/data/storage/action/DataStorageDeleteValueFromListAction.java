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
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.Parameters;

/**
 * @author Ivica Cardic
 */
public class DataStorageDeleteValueFromListAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteValueFromlist")
        .title("Delete Value from List")
        .description("Delete a value from the given index in a list.")
        .properties(
            string(KEY)
                .label("Key")
                .description("The identifier of a list to delete value from, stored earlier in the selected scope.")
                .required(true),
            integer(SCOPE)
                .label("Scope")
                .description(
                    "The namespace to delete a value from. The value should have been previously accessible, either in the present workflow execution, or the workflow itself for all the executions, or the user account for all the workflows the user has.")
                .options(SCOPE_OPTIONS)
                .required(true),
            integer(INDEX)
                .label("Index")
                .description(
                    "The specified index in the list will be removed, and if it doesn't exist, the list will remain unaltered.")
                .required(true))
        .perform(DataStorageDeleteValueFromListAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        // TODO

        return null;
    }
}
