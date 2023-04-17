
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

package com.bytechef.component.airtable;

import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;

import com.bytechef.component.airtable.action.CreateRecordAction;
import com.bytechef.component.airtable.connection.AirtableConnection;
import com.bytechef.hermes.component.OpenApiComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractAirtableComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = component("airtable")
        .display(
            modifyDisplay(
                display("Airtable")
                    .description(
                        "Airtable is a user-friendly and flexible cloud-based database management tool that enables teams and individuals to organize, share, and collaborate on their work in a visually pleasing and customizable way.")))
        .actions(modifyActions(CreateRecordAction.ACTION_DEFINITION))
        .connection(modifyConnection(AirtableConnection.CONNECTION_DEFINITION))
        .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
