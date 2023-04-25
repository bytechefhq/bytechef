
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

package com.bytechef.component.datamapper;

import com.bytechef.component.datamapper.action.DataMapperMapKeysAction;
import com.bytechef.component.datamapper.action.DataMapperMapListToObjectAction;
import com.bytechef.component.datamapper.action.DataMapperMapMultipleValuesBetweenObjectsAction;
import com.bytechef.component.datamapper.action.DataMapperMapObjectsAction;
import com.bytechef.component.datamapper.action.DataMapperMapObjectsToListAction;
import com.bytechef.component.datamapper.action.DataMapperMapOneValueAction;
import com.bytechef.component.datamapper.action.DataMapperMapValuesAction;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

import static com.bytechef.hermes.component.definition.ComponentDSL.component;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class DataMapperComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("dataMapper")
        .title("Data Mapper")
        .description("The Data Mapper enables you to configure data mappings.")
        .actions(
            DataMapperMapKeysAction.ACTION_DEFINITION,
            DataMapperMapListToObjectAction.ACTION_DEFINITION,
            DataMapperMapMultipleValuesBetweenObjectsAction.ACTION_DEFINITION,
            DataMapperMapObjectsAction.ACTION_DEFINITION,
            DataMapperMapObjectsToListAction.ACTION_DEFINITION,
            DataMapperMapOneValueAction.ACTION_DEFINITION,
            DataMapperMapValuesAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
