
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.component.mailchimp;

import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;

import com.bytechef.component.mailchimp.action.AddMemberToListAction;
import com.bytechef.component.mailchimp.connection.MailchimpConnection;
import com.bytechef.hermes.component.OpenApiComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractMailchimpComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = component("mailchimp")
        .display(
            modifyDisplay(
                display("Mailchimp")
                    .description("Mailchimp REST API documentation")))
        .actions(modifyActions(AddMemberToListAction.ACTION_DEFINITION))
        .connection(modifyConnection(MailchimpConnection.CONNECTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
