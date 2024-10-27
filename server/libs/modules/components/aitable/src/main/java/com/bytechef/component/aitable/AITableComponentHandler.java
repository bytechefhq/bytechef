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

package com.bytechef.component.aitable;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.aitable.action.AITableCreateRecordAction;
import com.bytechef.component.aitable.action.AITableFindRecordsAction;
import com.bytechef.component.aitable.action.AITableUpdateRecordAction;
import com.bytechef.component.aitable.connection.AITableConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class AITableComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("aitable")
        .title("AITable")
        .description(
            "AITable is an AI-powered platform that enables users to create interactive and dynamic tables for data " +
                "visualization and analysis without requiring coding skills.")
        .icon("path:assets/aitable.svg")
        .customAction(true)
        .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION)
        .connection(AITableConnection.CONNECTION_DEFINITION)
        .actions(
            AITableCreateRecordAction.ACTION_DEFINITION,
            AITableFindRecordsAction.ACTION_DEFINITION,
            AITableUpdateRecordAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }

}
