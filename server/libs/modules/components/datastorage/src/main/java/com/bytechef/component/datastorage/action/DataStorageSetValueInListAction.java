
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
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.data.storage.service.DataStorageService;

import static com.bytechef.component.datastorage.constant.DataStorageConstants.INDEX;
import static com.bytechef.component.datastorage.constant.DataStorageConstants.KEY;
import static com.bytechef.component.datastorage.constant.DataStorageConstants.SCOPE;
import static com.bytechef.component.datastorage.constant.DataStorageConstants.SCOPE_OPTIONS;
import static com.bytechef.component.datastorage.constant.DataStorageConstants.VALUE;
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
public class DataStorageSetValueInListAction {

    public final ActionDefinition actionDefinition = action("setValueInList")
        .title("Set Value in List")
        .description("Set value under a specified index in a list.")
        .properties(
            string(KEY)
                .label("Key")
                .description(
                    "The identifier of a list. Must be unique across all keys within the chosen scope to prevent overwriting the existing value with a new one. Also, it must be less than 1024 bytes in length.")
                .required(true),
            string(SCOPE)
                .label("Scope")
                .description(
                    "The namespace to set a value in. The value should have been previously accessible, either in the present workflow execution, or the workflow itself for all the executions, or the user account for all the workflows the user has.")
                .options(SCOPE_OPTIONS)
                .required(true),
            integer(INDEX)
                .label("Index")
                .description("The index in a list to set a value under. The previous value will be overridden.")
                .required(true),
            oneOf(VALUE).types(array(), bool(), integer(), number(), object(), string(), nullable())
                .label("Value")
                .description("The value to set under the specified list's key.")
                .required(true))
        .execute(this::execute);

    private final DataStorageService dataStorageService;

    public DataStorageSetValueInListAction(DataStorageService dataStorageService) {
        this.dataStorageService = dataStorageService;
    }

    protected Object execute(ActionContext actionContext, InputParameters inputParameters) {
        System.out.println(dataStorageService.toString());

        return null;
    }
}
