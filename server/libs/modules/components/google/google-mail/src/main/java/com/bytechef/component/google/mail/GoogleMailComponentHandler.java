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

package com.bytechef.component.google.mail;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;
import static com.bytechef.component.google.mail.connection.GoogleMailConnection.CONNECTION_DEFINITION;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.google.mail.action.GoogleMailAddLabelsAction;
import com.bytechef.component.google.mail.action.GoogleMailArchiveEmailAction;
import com.bytechef.component.google.mail.action.GoogleMailCreateLabelAction;
import com.bytechef.component.google.mail.action.GoogleMailDeleteMailAction;
import com.bytechef.component.google.mail.action.GoogleMailGetMailAction;
import com.bytechef.component.google.mail.action.GoogleMailGetThreadAction;
import com.bytechef.component.google.mail.action.GoogleMailListLabelsAction;
import com.bytechef.component.google.mail.action.GoogleMailRemoveLabelsAction;
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
        .customAction(true)
        .categories(ComponentCategory.COMMUNICATION)
        .connection(CONNECTION_DEFINITION)
        .actions(
            GoogleMailAddLabelsAction.ACTION_DEFINITION,
            GoogleMailArchiveEmailAction.ACTION_DEFINITION,
            GoogleMailCreateLabelAction.ACTION_DEFINITION,
            GoogleMailDeleteMailAction.ACTION_DEFINITION,
            GoogleMailGetMailAction.ACTION_DEFINITION,
            GoogleMailGetThreadAction.ACTION_DEFINITION,
            GoogleMailListLabelsAction.ACTION_DEFINITION,
            GoogleMailRemoveLabelsAction.ACTION_DEFINITION,
            GoogleMailReplyToEmailAction.ACTION_DEFINITION,
            GoogleMailSearchEmailAction.ACTION_DEFINITION,
            GoogleMailSendEmailAction.ACTION_DEFINITION)
        .triggers(
            GoogleMailNewEmailTrigger.TRIGGER_DEFINITION,
            GoogleMailNewEmailPollingTrigger.TRIGGER_DEFINITION)
        .clusterElements(
            tool(GoogleMailAddLabelsAction.ACTION_DEFINITION),
            tool(GoogleMailArchiveEmailAction.ACTION_DEFINITION),
            tool(GoogleMailCreateLabelAction.ACTION_DEFINITION),
            tool(GoogleMailDeleteMailAction.ACTION_DEFINITION),
            tool(GoogleMailGetMailAction.ACTION_DEFINITION),
            tool(GoogleMailGetThreadAction.ACTION_DEFINITION),
            tool(GoogleMailListLabelsAction.ACTION_DEFINITION),
            tool(GoogleMailRemoveLabelsAction.ACTION_DEFINITION),
            tool(GoogleMailReplyToEmailAction.ACTION_DEFINITION),
            tool(GoogleMailSearchEmailAction.ACTION_DEFINITION),
            tool(GoogleMailSendEmailAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
