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

package com.bytechef.component.coda;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.coda.action.CodaCopyDocAction;
import com.bytechef.component.coda.action.CodaListDocsAction;
import com.bytechef.component.coda.action.CodaUpdateRowAction;
import com.bytechef.component.coda.connection.CodaConnection;
import com.bytechef.component.definition.ComponentDefinition;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractCodaComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("coda")
            .title("Coda")
            .description(
                "Coda is a collaborative all-in-one productivity tool that combines documents, spreadsheets, apps, and databases into a single platform."))
                    .actions(modifyActions(CodaListDocsAction.ACTION_DEFINITION, CodaCopyDocAction.ACTION_DEFINITION,
                        CodaUpdateRowAction.ACTION_DEFINITION))
                    .connection(modifyConnection(CodaConnection.CONNECTION_DEFINITION))
                    .clusterElements(modifyClusterElements(tool(CodaListDocsAction.ACTION_DEFINITION),
                        tool(CodaCopyDocAction.ACTION_DEFINITION), tool(CodaUpdateRowAction.ACTION_DEFINITION)))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
