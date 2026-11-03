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

package com.bytechef.component.viber;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.viber.action.ViberBroadcastTextMessageAction;
import com.bytechef.component.viber.action.ViberSendTextMessageAction;
import com.bytechef.component.viber.connection.ViberConnection;
import com.bytechef.component.viber.trigger.ViberNewEventTrigger;
import com.bytechef.component.viber.trigger.ViberNewMessageTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class ViberComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("viber")
        .title("Viber")
        .description(
            "Viber is a messaging platform whose bot API lets businesses send messages to subscribers and react to " +
                "their events.")
        .icon("path:assets/viber.svg")
        .categories(ComponentCategory.COMMUNICATION)
        .connection(ViberConnection.CONNECTION_DEFINITION)
        .customAction(true)
        .customActionHelp("", "https://developers.viber.com/docs/api/rest-bot-api/")
        .actions(ViberSendTextMessageAction.ACTION_DEFINITION, ViberBroadcastTextMessageAction.ACTION_DEFINITION)
        .triggers(ViberNewMessageTrigger.TRIGGER_DEFINITION, ViberNewEventTrigger.TRIGGER_DEFINITION)
        .clusterElements(
            tool(ViberSendTextMessageAction.ACTION_DEFINITION),
            tool(ViberBroadcastTextMessageAction.ACTION_DEFINITION))
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
