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
import static com.bytechef.component.data.storage.constant.DataStorageConstants.SCOPE_OPTIONS;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.VALUE_TO_ADD;
import static com.bytechef.component.data.storage.util.DataStorageUtils.locker;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
            string(SCOPE)
                .label("Scope")
                .description("The namespace to obtain a value from.")
                .options(SCOPE_OPTIONS)
                .required(true),
            integer(VALUE_TO_ADD)
                .label("Value to add")
                .description(
                    "The value that can be added to the existing numeric value, which may have a negative value.")
                .defaultValue(1))
        .outputSchema(integer())
        .perform(DataStorageAtomicIncrementAction::perform);

    protected static Integer perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Integer number = null;
        try {
            if (locker.tryLock(10, TimeUnit.SECONDS)) {
                Optional<Object> optionalList = context.data(
                    data -> data.fetchValue(ActionContext.Data.Scope.valueOf(inputParameters.getRequiredString(SCOPE)),
                        inputParameters.getRequiredString(KEY)));
                if (optionalList.isPresent() && optionalList.get() instanceof Number)
                    number = (Integer) optionalList.get();
                else
                    return null;

                number += inputParameters.getRequiredInteger(VALUE_TO_ADD);

                Integer finalNumber = number;
                context.data(
                    data -> data.setValue(ActionContext.Data.Scope.valueOf(inputParameters.getRequiredString(SCOPE)),
                        inputParameters.getRequiredString(KEY), finalNumber));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            locker.unlock();
        }

        return number;
    }
}
