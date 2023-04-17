
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

import static com.bytechef.component.datastorage.constant.DataStorageConstants.SCOPE;
import static com.bytechef.component.datastorage.constant.DataStorageConstants.SCOPE_OPTIONS;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.definition.DefinitionDSL.display;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class GetAllKeysAction {

    public final ActionDefinition actionDefinition = action("getAllKeys")
        .display(display("Get All Keys").description(
            "Retrieve all the currently existing keys from storage, along with their values within the provided scope."))
        .properties(
            string(SCOPE)
                .label("Scope")
                .description("The namespace to get keys from.")
                .options(SCOPE_OPTIONS)
                .required(true))
        .execute(this::execute);

    private final DataStorageService dataStorageService;

    public GetAllKeysAction(DataStorageService dataStorageService) {
        this.dataStorageService = dataStorageService;
    }

    protected Object execute(ActionContext actionContext, InputParameters inputParameters) {
        System.out.println(dataStorageService.toString());

        return null;
    }
}
