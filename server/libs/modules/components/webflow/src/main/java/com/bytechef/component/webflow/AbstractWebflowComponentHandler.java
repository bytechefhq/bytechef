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

package com.bytechef.component.webflow;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.webflow.action.WebflowFulfillOrderAction;
import com.bytechef.component.webflow.action.WebflowGetCollectionItemAction;
import com.bytechef.component.webflow.connection.WebflowConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractWebflowComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("webflow")
            .title("Webflow")
            .description(
                "Webflow is a web design and development platform that allows users to build responsive websites visually without writing code."))
                    .actions(modifyActions(WebflowFulfillOrderAction.ACTION_DEFINITION,
                        WebflowGetCollectionItemAction.ACTION_DEFINITION))
                    .connection(modifyConnection(WebflowConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
