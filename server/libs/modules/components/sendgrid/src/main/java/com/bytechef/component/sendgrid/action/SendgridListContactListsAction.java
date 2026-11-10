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

package com.bytechef.component.sendgrid.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.sendgrid.util.SendgridUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class SendgridListContactListsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listContactLists")
        .title("List Contact Lists")
        .description("Lists all Marketing Campaigns contact lists in the SendGrid account.")
        .output(
            outputSchema(
                array()
                    .items(
                        object()
                            .properties(
                                string("id")
                                    .description("The ID of the contact list."),
                                string("name")
                                    .description("The name of the contact list."),
                                integer("contact_count")
                                    .description("The number of contacts on the list.")))))
        .help("", "https://docs.bytechef.io/reference/components/sendgrid_v1#list-contact-lists")
        .perform(SendgridListContactListsAction::perform);

    private SendgridListContactListsAction() {
    }

    public static List<Map<?, ?>> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return SendgridUtils.getContactLists(context);
    }
}
