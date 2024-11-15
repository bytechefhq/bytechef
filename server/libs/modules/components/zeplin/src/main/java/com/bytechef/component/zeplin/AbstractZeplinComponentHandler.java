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

package com.bytechef.component.zeplin;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.zeplin.action.ZeplinUpdateProjectAction;
import com.bytechef.component.zeplin.connection.ZeplinConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractZeplinComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("zeplin")
            .title("Zeplin")
            .description(
                "Zeplin is a collaboration tool that bridges the gap between designers and developers by providing a platform to share, organize, and translate design files into development."))
                    .actions(modifyActions(ZeplinUpdateProjectAction.ACTION_DEFINITION))
                    .connection(modifyConnection(ZeplinConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
