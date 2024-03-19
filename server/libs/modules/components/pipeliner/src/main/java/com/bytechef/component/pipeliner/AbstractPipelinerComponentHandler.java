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

package com.bytechef.component.pipeliner;

import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.pipeliner.action.PipelinerCreateAccountAction;
import com.bytechef.component.pipeliner.action.PipelinerCreateContactAction;
import com.bytechef.component.pipeliner.action.PipelinerCreateTaskAction;
import com.bytechef.component.pipeliner.connection.PipelinerConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractPipelinerComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("pipeliner")
            .title("Pipeliner")
            .description(
                "Pipeliner CRM is a comprehensive sales management tool that helps streamline sales processes through visual pipline management, contact organization, sales forecasting, and reporting."))
                    .actions(modifyActions(PipelinerCreateAccountAction.ACTION_DEFINITION,
                        PipelinerCreateContactAction.ACTION_DEFINITION, PipelinerCreateTaskAction.ACTION_DEFINITION))
                    .connection(modifyConnection(PipelinerConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
