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

package com.bytechef.component.airtable;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.airtable.action.AirtableCreateRecordAction;
import com.bytechef.component.airtable.action.AirtableDeleteRecordAction;
import com.bytechef.component.airtable.action.AirtableGetRecordAction;
import com.bytechef.component.airtable.action.AirtableUpdateRecordAction;
import com.bytechef.component.airtable.connection.AirtableConnection;
import com.bytechef.component.definition.ComponentDefinition;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractAirtableComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("airtable")
            .title("Airtable")
            .description("Airtable is a user-friendly and flexible cloud-based database management tool."))
                .actions(modifyActions(AirtableCreateRecordAction.ACTION_DEFINITION,
                    AirtableUpdateRecordAction.ACTION_DEFINITION, AirtableDeleteRecordAction.ACTION_DEFINITION,
                    AirtableGetRecordAction.ACTION_DEFINITION))
                .connection(modifyConnection(AirtableConnection.CONNECTION_DEFINITION))
                .clusterElements(modifyClusterElements(tool(AirtableCreateRecordAction.ACTION_DEFINITION),
                    tool(AirtableUpdateRecordAction.ACTION_DEFINITION),
                    tool(AirtableDeleteRecordAction.ACTION_DEFINITION),
                    tool(AirtableGetRecordAction.ACTION_DEFINITION)))
                .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
