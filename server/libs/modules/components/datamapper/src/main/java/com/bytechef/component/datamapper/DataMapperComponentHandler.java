
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

import com.bytechef.component.datamapper.action.MapKeysAction;
import com.bytechef.component.datamapper.action.MapListToObjectAction;
import com.bytechef.component.datamapper.action.MapMultipleValuesBetweenObjectsAction;
import com.bytechef.component.datamapper.action.MapObjectsAction;
import com.bytechef.component.datamapper.action.MapObjectsToListAction;
import com.bytechef.component.datamapper.action.MapOneValueAction;
import com.bytechef.component.datamapper.action.MapValuesAction;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.definition.DefinitionDSL.display;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class DataMapperComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("dataMapper")
        .display(display("Data Mapper").description("The Data Mapper enables you to configure data mappings."))
        .actions(
            MapKeysAction.ACTION_DEFINITION,
            MapListToObjectAction.ACTION_DEFINITION,
            MapMultipleValuesBetweenObjectsAction.ACTION_DEFINITION,
            MapObjectsAction.ACTION_DEFINITION,
            MapObjectsToListAction.ACTION_DEFINITION,
            MapOneValueAction.ACTION_DEFINITION,
            MapValuesAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
