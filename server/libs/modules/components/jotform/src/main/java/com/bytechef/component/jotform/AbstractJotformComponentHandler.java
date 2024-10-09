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

package com.bytechef.component.jotform;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.jotform.action.JotformGetFormSubmissionsAction;
import com.bytechef.component.jotform.connection.JotformConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractJotformComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("jotform")
            .title("JotForm")
            .description(
                "JotForm is an online form builder that enables users to create customized forms for various purposes without needing coding skills."))
                    .actions(modifyActions(JotformGetFormSubmissionsAction.ACTION_DEFINITION))
                    .connection(modifyConnection(JotformConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
