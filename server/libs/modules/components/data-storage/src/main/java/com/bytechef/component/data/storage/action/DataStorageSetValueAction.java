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
import static com.bytechef.component.data.storage.constant.DataStorageConstants.TYPE;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.TYPE_OPTIONS;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.VALUE;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.nullable;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.time;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class DataStorageSetValueAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("setValue")
        .title("Set Value")
        .description("Set a value under a key, in the specified scope.")
        .properties(
            string(KEY)
                .label("Key")
                .description(
                    "The identifier of a value. Must be unique across all keys within the chosen scope to prevent overwriting the existing value with a new one. Also, it must be less than 1024 bytes in length.")
                .required(true),
            integer(SCOPE)
                .label("Scope")
                .description(
                    "The namespace to set a value in. The value should have been previously accessible, either in the present workflow execution, or the workflow itself for all the executions, or the user account for all the workflows the user has.")
                .options(SCOPE_OPTIONS)
                .required(true),
            integer(TYPE)
                .label("Type")
                .description("The value type.")
                .options(TYPE_OPTIONS),
            array(VALUE)
                .label("Value")
                .description("The value to set under the specified key.")
                .displayCondition("type === 1")
                .required(true),
            bool(VALUE)
                .label("Value")
                .description("The value to set under the specified key.")
                .displayCondition("type === 2")
                .required(true),
            date(VALUE)
                .label("Value")
                .description("The value to set under the specified key.")
                .displayCondition("type === 3")
                .required(true),
            dateTime(VALUE)
                .label("Value")
                .description("The value to set under the specified key.")
                .displayCondition("type === 4")
                .required(true),
            integer(VALUE)
                .label("Value")
                .description("The value to set under the specified key.")
                .displayCondition("type === 5")
                .required(true),
            nullable(VALUE)
                .label("Value")
                .description("The value to set under the specified key.")
                .displayCondition("type === 6")
                .required(true),
            number(VALUE)
                .label("Value")
                .description("The value to set under the specified key.")
                .displayCondition("type === 7")
                .required(true),
            object(VALUE)
                .label("Value")
                .description("The value to set under the specified key.")
                .displayCondition("type === 8")
                .required(true),
            string(VALUE)
                .label("Value")
                .description("The value to set under the specified key.")
                .displayCondition("type === 9")
                .required(true),
            time(VALUE)
                .label("Value")
                .description("The value to set under the specified key.")
                .displayCondition("type === 10")
                .required(true))
        .perform(DataStorageSetValueAction::perform);

    protected static Map<String, ?> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        // TODO

        return null;
    }
}
