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

package com.bytechef.component.nifty;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.nifty.action.NiftyCreateProjectAction;
import com.bytechef.component.nifty.action.NiftyCreateTaskAction;
import com.bytechef.component.nifty.connection.NiftyConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractNiftyComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("nifty")
            .title("Nifty")
            .description(
                "Nifty Project Management is a software tool that streamlines team collaboration and project tracking with features like task management, timelines, and communication tools to enhance productivity."))
                    .actions(modifyActions(NiftyCreateProjectAction.ACTION_DEFINITION,
                        NiftyCreateTaskAction.ACTION_DEFINITION))
                    .connection(modifyConnection(NiftyConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
