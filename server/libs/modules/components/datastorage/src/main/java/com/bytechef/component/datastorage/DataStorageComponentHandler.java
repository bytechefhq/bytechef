
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

import com.bytechef.component.datastorage.action.DataStorageAppendValueToListAction;
import com.bytechef.component.datastorage.action.DataStorageAtomicIncrementAction;
import com.bytechef.component.datastorage.action.DataStorageAwaitGetValueAction;
import com.bytechef.component.datastorage.action.DataStorageDeleteValueAction;
import com.bytechef.component.datastorage.action.DataStorageDeleteValueFromListAction;
import com.bytechef.component.datastorage.action.DataStorageGetAllKeysAction;
import com.bytechef.component.datastorage.action.DataStorageGetValueAction;
import com.bytechef.component.datastorage.action.DataStorageSetValueInListAction;
import com.bytechef.component.datastorage.action.DataStorageSetValueAction;
import com.bytechef.hermes.component.ComponentDefinitionFactory;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.data.storage.service.DataStorageService;
import org.springframework.stereotype.Component;

import static com.bytechef.hermes.component.definition.ComponentDSL.component;

/**
 * @author Ivica Cardic
 */
@Component
public class DataStorageComponentHandler implements ComponentDefinitionFactory {

    private final ComponentDefinition componentDefinition;

    public DataStorageComponentHandler(DataStorageService dataStorageService) {
        componentDefinition = component("dataStorage")
            .title("Data Storage")
            .description(
                "Using the Data Storage component, you can easily manage and operate on lists and objects by setting or retrieving any desired data. This process employs a key-value store mechanism, where the key represents the field's name and the value corresponds to the particular data's actual value.")
            .actions(
                new DataStorageAppendValueToListAction(dataStorageService).actionDefinition,
                new DataStorageAtomicIncrementAction(dataStorageService).actionDefinition,
                new DataStorageAwaitGetValueAction(dataStorageService).actionDefinition,
                new DataStorageDeleteValueAction(dataStorageService).actionDefinition,
                new DataStorageDeleteValueFromListAction(dataStorageService).actionDefinition,
                new DataStorageGetAllKeysAction(dataStorageService).actionDefinition,
                new DataStorageGetValueAction(dataStorageService).actionDefinition,
                new DataStorageSetValueAction(dataStorageService).actionDefinition,
                new DataStorageSetValueInListAction(dataStorageService).actionDefinition);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
