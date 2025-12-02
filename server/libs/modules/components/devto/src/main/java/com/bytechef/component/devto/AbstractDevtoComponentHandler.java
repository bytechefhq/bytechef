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

package com.bytechef.component.devto;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.devto.action.DevtoCreateArticleAction;
import com.bytechef.component.devto.action.DevtoGetArticleAction;
import com.bytechef.component.devto.action.DevtoUpdateArticleAction;
import com.bytechef.component.devto.connection.DevtoConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractDevtoComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("devto")
            .title("Dev.to")
            .description(
                "Dev.to is an online community and platform where software developers share articles, tutorials, and discussions about programming and technology."))
                    .actions(modifyActions(DevtoCreateArticleAction.ACTION_DEFINITION,
                        DevtoGetArticleAction.ACTION_DEFINITION, DevtoUpdateArticleAction.ACTION_DEFINITION))
                    .connection(modifyConnection(DevtoConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
