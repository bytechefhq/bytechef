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

package com.bytechef.component.brevo;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.brevo.action.BrevoCreateContactAction;
import com.bytechef.component.brevo.action.BrevoSendTransactionalEmailAction;
import com.bytechef.component.brevo.action.BrevoUpdateContactAction;
import com.bytechef.component.brevo.connection.BrevoConnection;
import com.bytechef.component.brevo.trigger.BrevoTransactionalEmailOpenedTrigger;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Marija Horvat
 */
@AutoService(ComponentHandler.class)
public class BrevoComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("brevo")
        .title("Brevo")
        .description(
            "Brevo is an email marketing platform that offers a cloud-based marketing communication software suite " +
                "with transactional email, marketing automation, customer-relationship management and more.")
        .icon("path:assets/brevo.svg")
        .categories(ComponentCategory.MARKETING_AUTOMATION)
        .connection(BrevoConnection.CONNECTION_DEFINITION)
        .actions(
            BrevoCreateContactAction.ACTION_DEFINITION,
            BrevoUpdateContactAction.ACTION_DEFINITION,
            BrevoSendTransactionalEmailAction.ACTION_DEFINITION)
        .triggers(
            BrevoTransactionalEmailOpenedTrigger.TRIGGER_DEFINITION)
        .clusterElements(
            tool(BrevoCreateContactAction.ACTION_DEFINITION),
            tool(BrevoUpdateContactAction.ACTION_DEFINITION),
            tool(BrevoSendTransactionalEmailAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
