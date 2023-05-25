
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
import com.bytechef.component.datastorage.action.DataStorageGetAllEntriesAction;
import com.bytechef.component.datastorage.action.DataStorageGetValueAction;
import com.bytechef.component.datastorage.action.DataStorageSetValueInListAction;
import com.bytechef.component.datastorage.action.DataStorageSetValueAction;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;

import static com.bytechef.hermes.component.definition.ComponentDSL.component;

/**
 * @author Ivica Cardic
 */
public class DataStorageComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("dataStorage")
        .title("Data Storage")
        .description(
            "Using the Data Storage component, you can easily manage and operate on lists and objects by setting or retrieving any desired data. This process employs a key-value store mechanism, where the key represents the field's name and the value corresponds to the particular data's actual value.")
        .icon("path:assets/datastorage.svg")
        .actions(
            DataStorageAppendValueToListAction.ACTION_DEFINITION,
            DataStorageAtomicIncrementAction.ACTION_DEFINITION,
            DataStorageAwaitGetValueAction.ACTION_DEFINITION,
            DataStorageDeleteValueAction.ACTION_DEFINITION,
            DataStorageDeleteValueFromListAction.ACTION_DEFINITION,
            DataStorageGetAllEntriesAction.ACTION_DEFINITION,
            DataStorageGetValueAction.ACTION_DEFINITION,
            DataStorageSetValueAction.ACTION_DEFINITION,
            DataStorageSetValueInListAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
