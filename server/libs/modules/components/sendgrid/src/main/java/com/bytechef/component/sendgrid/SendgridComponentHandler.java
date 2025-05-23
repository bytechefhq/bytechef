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

package com.bytechef.component.sendgrid;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.sendgrid.action.SendgridSendEmailAction;
import com.bytechef.component.sendgrid.connection.SendgridConnection;
import com.google.auto.service.AutoService;

/**
 * @author Marko Krišković
 */
@AutoService(ComponentHandler.class)
public class SendgridComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("sendgrid")
        .title("Sendgrid")
        .description("Trusted for reliable email delivery at scale.")
        .customAction(true)
        .connection(SendgridConnection.CONNECTION_DEFINITION)
        .icon("path:assets/sendgrid.svg")
        .categories(ComponentCategory.COMMUNICATION, ComponentCategory.MARKETING_AUTOMATION)
        .actions(SendgridSendEmailAction.ACTION_DEFINITION)
        .clusterElements(tool(SendgridSendEmailAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
