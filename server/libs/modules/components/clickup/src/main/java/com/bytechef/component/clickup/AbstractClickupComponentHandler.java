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

package com.bytechef.component.clickup;

import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.OpenAPIComponentHandler;
import com.bytechef.component.clickup.action.ClickupCreateFolderAction;
import com.bytechef.component.clickup.action.ClickupCreateListAction;
import com.bytechef.component.clickup.action.ClickupCreateTaskAction;
import com.bytechef.component.clickup.connection.ClickupConnection;
import com.bytechef.component.definition.ComponentDefinition;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractClickupComponentHandler implements OpenAPIComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("clickup")
            .title("Clickup")
            .description(
                "ClickUp is a cloud-based collaboration tool that offers task management, document sharing, goal tracking, and other productivity features for teams."))
                    .actions(modifyActions(ClickupCreateListAction.ACTION_DEFINITION,
                        ClickupCreateTaskAction.ACTION_DEFINITION, ClickupCreateFolderAction.ACTION_DEFINITION))
                    .connection(modifyConnection(ClickupConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
