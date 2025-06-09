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

package com.bytechef.component.contiguity;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.contiguity.action.ContiguitySendEmailAction;
import com.bytechef.component.contiguity.action.ContiguitySendSmsAction;
import com.bytechef.component.contiguity.connection.ContiguityConnection;
import com.bytechef.component.definition.ComponentDefinition;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractContiguityComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("contiguity")
            .title("Contiguity")
            .description("Contiguity is an SMS service for your needs - quick and simple."))
                .actions(modifyActions(ContiguitySendSmsAction.ACTION_DEFINITION,
                    ContiguitySendEmailAction.ACTION_DEFINITION))
                .connection(modifyConnection(ContiguityConnection.CONNECTION_DEFINITION))
                .clusterElements(modifyClusterElements(tool(ContiguitySendSmsAction.ACTION_DEFINITION),
                    tool(ContiguitySendEmailAction.ACTION_DEFINITION)))
                .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
