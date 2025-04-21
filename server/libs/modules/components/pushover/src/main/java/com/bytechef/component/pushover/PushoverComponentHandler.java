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

package com.bytechef.component.pushover;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.pushover.action.PushoverSendNotificationAction;
import com.bytechef.component.pushover.connection.PushoverConnection;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class PushoverComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("pushover")
        .title("Pushover")
        .description(
            "Pushover is a notification service that sends real-time alerts to mobile and desktop devices, " +
                "integrating with apps, scripts, and services.")
        .icon("path:assets/pushover.svg")
        .categories(ComponentCategory.COMMUNICATION)
        .connection(PushoverConnection.CONNECTION_DEFINITION)
        .actions(PushoverSendNotificationAction.ACTION_DEFINITION)
        .clusterElements(tool(PushoverSendNotificationAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
