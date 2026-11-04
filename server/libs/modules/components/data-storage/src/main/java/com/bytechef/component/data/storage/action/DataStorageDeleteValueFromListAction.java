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

import static com.bytechef.component.data.storage.constant.DataStorageConstants.INDEX;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.KEY;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.SCOPE;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.SCOPE_OPTIONS;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Data.Scope;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
            string(SCOPE)
                .label("Scope")
                .description(
                    "The namespace to delete a value from. The value should have been previously accessible, " +
                        "either in the present workflow execution, or the workflow itself for all the executions, or " +
                        "the user account for all the workflows the user has.")
                .options(SCOPE_OPTIONS)
                .required(true),
            integer(INDEX)
                .label("Index")
                .description(
                    "The specified index in the list will be removed, and if it doesn't exist, the list will " +
                        "remain unaltered.")
                .required(true))
        .help("", "https://docs.bytechef.io/reference/components/data-storage_v1#delete-value-from-list")
        .perform(DataStorageDeleteValueFromListAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Scope scope = Scope.valueOf(inputParameters.getRequiredString(SCOPE));
        String key = inputParameters.getRequiredString(KEY);

        Optional<Object> optionalList = context.data(data -> data.fetch(scope, key));

        if (optionalList.isEmpty() || !(optionalList.get() instanceof List<?>)) {
            return null;
        }

        int index = inputParameters.getRequiredInteger(INDEX);

        context.data(data -> data.update(
            scope, key, (Optional<Object> currentValue) -> removeValue(currentValue, key, index)));

        return null;
    }

    private static List<Object> removeValue(Optional<Object> currentValue, String key, int index) {
        if (currentValue.isEmpty() || !(currentValue.get() instanceof List<?> currentList)) {
            throw new IllegalStateException(
                "The list stored under key '%s' was removed while it was being updated".formatted(key));
        }

        List<Object> list = new ArrayList<>(currentList);

        list.remove(index);

        return list;
    }
}
