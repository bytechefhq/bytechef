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

package com.bytechef.component.data.mapper;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.data.mapper.action.DataMapperMapObjectsToArrayAction;
import com.bytechef.component.data.mapper.action.DataMapperMapObjectsToObjectAction;
import com.bytechef.component.data.mapper.action.DataMapperMergeAndPivotByKeyAction;
import com.bytechef.component.data.mapper.action.DataMapperRenameKeysAction;
import com.bytechef.component.data.mapper.action.DataMapperReplaceAllSpecifiedValuesAction;
import com.bytechef.component.data.mapper.action.DataMapperReplaceMultipleValuesByKeyAction;
import com.bytechef.component.data.mapper.action.DataMapperReplaceValueAction;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class DataMapperComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("dataMapper")
        .title("Data Mapper")
        .description("The Data Mapper enables you to configure data mappings.")
        .icon("path:assets/data-mapper.svg")
        .categories(ComponentCategory.HELPERS)
        .actions(
            DataMapperMapObjectsToArrayAction.ACTION_DEFINITION,
            DataMapperMapObjectsToObjectAction.ACTION_DEFINITION,
            DataMapperMergeAndPivotByKeyAction.ACTION_DEFINITION,
            DataMapperRenameKeysAction.ACTION_DEFINITION,
            DataMapperReplaceAllSpecifiedValuesAction.ACTION_DEFINITION,
            DataMapperReplaceMultipleValuesByKeyAction.ACTION_DEFINITION,
            DataMapperReplaceValueAction.ACTION_DEFINITION)
        .clusterElements(
            tool(DataMapperMapObjectsToArrayAction.ACTION_DEFINITION),
            tool(DataMapperMapObjectsToObjectAction.ACTION_DEFINITION),
            tool(DataMapperMergeAndPivotByKeyAction.ACTION_DEFINITION),
            tool(DataMapperRenameKeysAction.ACTION_DEFINITION),
            tool(DataMapperReplaceAllSpecifiedValuesAction.ACTION_DEFINITION),
            tool(DataMapperReplaceMultipleValuesByKeyAction.ACTION_DEFINITION),
            tool(DataMapperReplaceValueAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
