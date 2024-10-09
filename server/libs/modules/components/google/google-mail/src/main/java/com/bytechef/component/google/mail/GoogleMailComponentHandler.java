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

package com.bytechef.component.google.mail;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.google.mail.connection.GoogleMailConnection.CONNECTION_DEFINITION;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.google.mail.action.GoogleMailAddLabelAction;
import com.bytechef.component.google.mail.action.GoogleMailDeleteMailAction;
import com.bytechef.component.google.mail.action.GoogleMailGetMailAction;
import com.bytechef.component.google.mail.action.GoogleMailGetThreadAction;
import com.bytechef.component.google.mail.action.GoogleMailReplyToEmailAction;
import com.bytechef.component.google.mail.action.GoogleMailSearchEmailAction;
import com.bytechef.component.google.mail.action.GoogleMailSendEmailAction;
import com.bytechef.component.google.mail.trigger.GoogleMailNewEmailPollingTrigger;
import com.bytechef.component.google.mail.trigger.GoogleMailNewEmailTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class GoogleMailComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("googleMail")
        .title("Gmail")
        .description(
            "Gmail is a widely used email service by Google, offering free and feature-rich communication, " +
                "organization, and storage capabilities accessible through web browsers and mobile apps.")
        .icon("path:assets/google-mail.svg")
        .categories(ComponentCategory.COMMUNICATION)
        .connection(CONNECTION_DEFINITION)
        .actions(GoogleMailGetMailAction.ACTION_DEFINITION,
            GoogleMailGetThreadAction.ACTION_DEFINITION,
            GoogleMailReplyToEmailAction.ACTION_DEFINITION,
            GoogleMailSearchEmailAction.ACTION_DEFINITION,
            GoogleMailSendEmailAction.ACTION_DEFINITION,
            GoogleMailDeleteMailAction.ACTION_DEFINITION,
            GoogleMailAddLabelAction.ACTION_DEFINITION)
        .triggers(
            GoogleMailNewEmailTrigger.TRIGGER_DEFINITION,
            GoogleMailNewEmailPollingTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
