
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

package com.bytechef.component.datastorage;

import com.bytechef.component.datastorage.action.AppendValueToListAction;
import com.bytechef.component.datastorage.action.AtomicIncrementAction;
import com.bytechef.component.datastorage.action.AwaitGetValueAction;
import com.bytechef.component.datastorage.action.DeleteValueAction;
import com.bytechef.component.datastorage.action.DeleteValueFromListAction;
import com.bytechef.component.datastorage.action.GetAllKeysAction;
import com.bytechef.component.datastorage.action.GetValueAction;
import com.bytechef.component.datastorage.action.SetValueInListAction;
import com.bytechef.component.datastorage.action.SetValueAction;
import com.bytechef.hermes.component.ComponentDefinitionFactory;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.data.storage.service.DataStorageService;
import org.springframework.stereotype.Component;

import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.definition.DefinitionDSL.display;

/**
 * @author Ivica Cardic
 */
@Component
public class DataStorageComponentHandler implements ComponentDefinitionFactory {

    private final ComponentDefinition componentDefinition;

    public DataStorageComponentHandler(DataStorageService dataStorageService) {
        componentDefinition = component("dataStorage")
            .display(display("Data Storage").description(
                "Using the Data Storage component, you can easily manage and operate on lists and objects by setting or retrieving any desired data. This process employs a key-value store mechanism, where the key represents the field's name and the value corresponds to the particular data's actual value."))
            .actions(
                new AppendValueToListAction(dataStorageService).actionDefinition,
                new AtomicIncrementAction(dataStorageService).actionDefinition,
                new AwaitGetValueAction(dataStorageService).actionDefinition,
                new DeleteValueAction(dataStorageService).actionDefinition,
                new DeleteValueFromListAction(dataStorageService).actionDefinition,
                new GetAllKeysAction(dataStorageService).actionDefinition,
                new GetValueAction(dataStorageService).actionDefinition,
                new SetValueAction(dataStorageService).actionDefinition,
                new SetValueInListAction(dataStorageService).actionDefinition);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
