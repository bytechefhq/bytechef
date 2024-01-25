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

package com.bytechef.component.microsoft.outlook;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.MICROSOFT_OUTLOOK_365;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.microsoft.outlook.action.MicrosoftOutlook365GetMailAction;
import com.bytechef.component.microsoft.outlook.action.MicrosoftOutlook365SearchEmailAction;
import com.bytechef.component.microsoft.outlook.action.MicrosoftOutlook365SendEmailAction;
import com.bytechef.component.microsoft.outlook.connection.MicrosoftOutlook365Connection;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class MicrosoftOutlook365ComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(MICROSOFT_OUTLOOK_365)
        .title("Microsoft Outlook 365")
        .description(
            "Microsoft Outlook 365 is a comprehensive email and productivity platform that integrates email, " +
                "calendar, contacts, and tasks to streamline communication and organization.")
        .icon("path:assets/microsoft-outlook-365.svg")
        .connection(MicrosoftOutlook365Connection.CONNECTION_DEFINITION)
        .actions(
            MicrosoftOutlook365GetMailAction.ACTION_DEFINITION,
            MicrosoftOutlook365SearchEmailAction.ACTION_DEFINITION,
            MicrosoftOutlook365SendEmailAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
