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

package com.bytechef.component.sendfox;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.sendfox.action.SendFoxCreateContactAction;
import com.bytechef.component.sendfox.action.SendFoxCreateListAction;
import com.bytechef.component.sendfox.action.SendFoxUnsubscribeContactAction;
import com.bytechef.component.sendfox.connection.SendFoxConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractSendFoxComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("sendfox")
            .title("SendFox")
            .description(
                "SendFox lets you automate email campaigns, complete with custom opt-in forms and landing pages, so you're not tanking your budget to get new subscribers."))
                    .actions(modifyActions(SendFoxCreateContactAction.ACTION_DEFINITION,
                        SendFoxCreateListAction.ACTION_DEFINITION, SendFoxUnsubscribeContactAction.ACTION_DEFINITION))
                    .connection(modifyConnection(SendFoxConnection.CONNECTION_DEFINITION))
                    .clusterElements(modifyClusterElements(tool(SendFoxCreateContactAction.ACTION_DEFINITION),
                        tool(SendFoxCreateListAction.ACTION_DEFINITION),
                        tool(SendFoxUnsubscribeContactAction.ACTION_DEFINITION)))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
