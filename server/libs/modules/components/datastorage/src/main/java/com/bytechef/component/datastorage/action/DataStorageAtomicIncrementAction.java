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

package com.bytechef.component.datastorage.action;

import static com.bytechef.component.datastorage.constant.DataStorageConstants.KEY;
import static com.bytechef.component.datastorage.constant.DataStorageConstants.SCOPE;
import static com.bytechef.component.datastorage.constant.DataStorageConstants.SCOPE_OPTIONS;
import static com.bytechef.component.datastorage.constant.DataStorageConstants.VALUE;
import static com.bytechef.component.datastorage.constant.DataStorageConstants.VALUE_TO_ADD;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.ParameterMap;

/**
 * @author Ivica Cardic
 */
public class DataStorageAtomicIncrementAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("atomicIncrement")
        .title("Atomic Increment")
        .description(
            "The numeric value can be incremented atomically, and the action can be used concurrently from multiple executions.")
        .properties(
            string(KEY)
                .label("Key")
                .description("The identifier of a value to increment.")
                .required(true),
            integer(SCOPE)
                .label("Scope")
                .description("The namespace to obtain a value from.")
                .options(SCOPE_OPTIONS)
                .required(true),
            integer(VALUE_TO_ADD)
                .label("Value to add")
                .description(
                    "The value that can be added to the existing numeric value, which may have a negative value.")
                .defaultValue(1))
        .outputSchema(
            object()
                .properties(integer(VALUE)))
        .perform(DataStorageAtomicIncrementAction::perform);

    protected static Object perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext context) {

        // TODO

        return null;
    }
}
