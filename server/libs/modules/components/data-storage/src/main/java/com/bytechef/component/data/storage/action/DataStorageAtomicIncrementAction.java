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

import static com.bytechef.component.data.storage.constant.DataStorageConstants.KEY;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.SCOPE;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.SCOPE_OPTIONS;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.VALUE_TO_ADD;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Data.Scope;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class DataStorageAtomicIncrementAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("atomicIncrement")
        .title("Atomic Increment")
        .description(
            "The numeric value can be incremented atomically, and the action can be used concurrently from " +
                "multiple executions.")
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
                .label("Value to Add")
                .description(
                    "The value that can be added to the existing numeric value, which may have a negative value.")
                .defaultValue(1)
                .required(true))
        .output(outputSchema(integer()))
        .help("", "https://docs.bytechef.io/reference/components/data-storage_v1#atomic-increment")
        .perform(DataStorageAtomicIncrementAction::perform);

    protected static Integer perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Scope scope = Scope.valueOf(inputParameters.getRequiredString(SCOPE));
        String key = inputParameters.getRequiredString(KEY);
        int valueToAdd = inputParameters.getRequiredInteger(VALUE_TO_ADD);

        Object value = context.data(
            data -> data.update(scope, key,
                (Optional<Object> currentValue) -> increment(currentValue, key, valueToAdd)));

        return (Integer) value;
    }

    private static Integer increment(Optional<Object> currentValue, String key, int valueToAdd) {
        try {
            return Math.addExact(getValue(currentValue, key), valueToAdd);
        } catch (ArithmeticException arithmeticException) {
            throw new IllegalArgumentException(
                "Incrementing the value stored under key '%s' overflows an integer".formatted(key),
                arithmeticException);
        }
    }

    private static int getValue(Optional<Object> currentValue, String key) {
        if (currentValue.isEmpty()) {
            return 0;
        }

        if (currentValue.get() instanceof Number number) {
            try {
                return Math.toIntExact(number.longValue());
            } catch (ArithmeticException arithmeticException) {
                throw new IllegalArgumentException(
                    "The value stored under key '%s' is outside the integer range".formatted(key),
                    arithmeticException);
            }
        }

        throw new IllegalArgumentException("The value stored under key '%s' is not a number".formatted(key));
    }
}
