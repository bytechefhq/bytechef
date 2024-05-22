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

package com.bytechef.component.email;

import static com.bytechef.component.email.constant.EmailConstants.EMAIL;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.email.action.SendEmailAction;
import com.bytechef.component.email.connection.EmailConnection;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class EmailComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = ComponentDSL.component(EMAIL)
        .title("Email")
        .description("The Email connector sends emails using an SMTP email server.")
        .connection(EmailConnection.CONNECTION_DEFINITION)
        .icon("path:assets/email.svg")
        .categories(ComponentCategory.COMMUNICATION, ComponentCategory.HELPERS)
        .actions(SendEmailAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
