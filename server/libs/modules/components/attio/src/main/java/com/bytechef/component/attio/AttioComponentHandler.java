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

package com.bytechef.component.attio;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.attio.action.AttioCreateRecordAction;
import com.bytechef.component.attio.action.AttioCreateTaskAction;
import com.bytechef.component.attio.action.AttioUpdateRecordAction;
import com.bytechef.component.attio.connection.AttioConnection;
import com.bytechef.component.attio.trigger.AttioRecordCreatedTrigger;
import com.bytechef.component.attio.trigger.AttioTaskCreatedTrigger;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class AttioComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("attio")
        .title("Attio")
        .description("Attio is the AI-native CRM that builds, scales and grows your company to the next level.")
        .icon("path:assets/attio.svg")
        .categories(ComponentCategory.CRM)
        .connection(AttioConnection.CONNECTION_DEFINITION)
        .actions(
            AttioCreateRecordAction.ACTION_DEFINITION,
            AttioCreateTaskAction.ACTION_DEFINITION,
            AttioUpdateRecordAction.ACTION_DEFINITION)
        .clusterElements(
            tool(AttioCreateRecordAction.ACTION_DEFINITION),
            tool(AttioCreateTaskAction.ACTION_DEFINITION),
            tool(AttioUpdateRecordAction.ACTION_DEFINITION))
        .triggers(
            AttioRecordCreatedTrigger.TRIGGER_DEFINITION,
            AttioTaskCreatedTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
