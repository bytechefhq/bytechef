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

package com.bytechef.component.insightly;

import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.insightly.action.InsightlyCreateContactAction;
import com.bytechef.component.insightly.action.InsightlyCreateOrganizationAction;
import com.bytechef.component.insightly.action.InsightlyCreateTaskAction;
import com.bytechef.component.insightly.connection.InsightlyConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractInsightlyComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("insightly")
            .title("Insightly")
            .description(
                "Insightly is a customer relationship management (CRM) software that helps businesses manage contacts, sales, projects, and tasks in one platform."))
                    .actions(modifyActions(InsightlyCreateContactAction.ACTION_DEFINITION,
                        InsightlyCreateOrganizationAction.ACTION_DEFINITION,
                        InsightlyCreateTaskAction.ACTION_DEFINITION))
                    .connection(modifyConnection(InsightlyConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
