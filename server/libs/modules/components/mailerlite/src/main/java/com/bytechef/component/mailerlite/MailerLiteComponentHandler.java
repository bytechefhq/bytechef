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

package com.bytechef.component.mailerlite;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.mailerlite.action.MailerLiteAddSubscriberToGroupAction;
import com.bytechef.component.mailerlite.action.MailerLiteCreateOrUpdateSubscriberAction;
import com.bytechef.component.mailerlite.action.MailerLiteRemoveSubscriberFromGroupAction;
import com.bytechef.component.mailerlite.connection.MailerLiteConnection;
import com.bytechef.component.mailerlite.trigger.MailerLiteSubscriberAddedToGroupTrigger;
import com.bytechef.component.mailerlite.trigger.MailerLiteSubscriberCreatedTrigger;
import com.bytechef.component.mailerlite.trigger.MailerLiteSubscriberUnsubscribedTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class MailerLiteComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("mailerLite")
        .title("MailerLite")
        .description(
            "MailerLite is an intuitive email marketing platform that offers automation, landing pages, and " +
                "subscriber management for businesses and creators.")
        .icon("path:assets/mailerlite.svg")
        .categories(ComponentCategory.MARKETING_AUTOMATION)
        .connection(MailerLiteConnection.CONNECTION_DEFINITION)
        .actions(
            MailerLiteAddSubscriberToGroupAction.ACTION_DEFINITION,
            MailerLiteCreateOrUpdateSubscriberAction.ACTION_DEFINITION,
            MailerLiteRemoveSubscriberFromGroupAction.ACTION_DEFINITION)
        .triggers(
            MailerLiteSubscriberAddedToGroupTrigger.TRIGGER_DEFINITION,
            MailerLiteSubscriberCreatedTrigger.TRIGGER_DEFINITION,
            MailerLiteSubscriberUnsubscribedTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
