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

import static com.bytechef.component.data.storage.constant.DataStorageConstants.KEY;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.SCOPE;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.data.storage.constant.DataStorageConstants;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;

/**
 * @author Ivica Cardic
 */
public class DataStorageDeleteValueAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteValue")
        .title("Delete Value")
        .description("Remove a value associated with a key in the specified scope.")
        .properties(
            string(DataStorageConstants.KEY)
                .label("Key")
                .description("The identifier of a value to delete, stored earlier in the selected scope.")
                .required(true),
            string(DataStorageConstants.SCOPE)
                .label("Scope")
                .description(
                    "The namespace to delete a value from. The value should have been previously accessible, either in the present workflow execution, or the workflow itself for all the executions, or the user account for all the workflows the user has.")
                .options(DataStorageConstants.SCOPE_OPTIONS)
                .required(true))
        .perform(DataStorageDeleteValueAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context
            .data(data -> data.deleteValue(ActionContext.Data.Scope.valueOf(inputParameters.getRequiredString(SCOPE)),
                inputParameters.getRequiredString(KEY)));
    }
}
