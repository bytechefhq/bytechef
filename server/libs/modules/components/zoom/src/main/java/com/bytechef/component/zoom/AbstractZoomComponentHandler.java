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

package com.bytechef.component.zoom;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.zoom.action.ZoomAddMeetingRegistrantAction;
import com.bytechef.component.zoom.action.ZoomCreateMeetingAction;
import com.bytechef.component.zoom.connection.ZoomConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractZoomComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("zoom")
            .title("Zoom")
            .description(
                "Zoom is a cloud-based video conferencing platform that enables virtual meetings, webinars, and collaboration through video, audio, and chat."))
                    .actions(modifyActions(ZoomCreateMeetingAction.ACTION_DEFINITION,
                        ZoomAddMeetingRegistrantAction.ACTION_DEFINITION))
                    .connection(modifyConnection(ZoomConnection.CONNECTION_DEFINITION))
                    .clusterElements(modifyClusterElements(tool(ZoomCreateMeetingAction.ACTION_DEFINITION),
                        tool(ZoomAddMeetingRegistrantAction.ACTION_DEFINITION)))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
