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
import static com.bytechef.component.definition.ComponentDsl.bool;
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
public class SendgridListSendersAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listSenders")
        .title("List Senders")
        .description(
            "Lists all Marketing Campaigns senders. Only verified senders can be used to send a Single Send.")
        .output(
            outputSchema(
                array()
                    .items(
                        object()
                            .properties(
                                integer("id")
                                    .description("The ID of the sender."),
                                string("nickname")
                                    .description("The nickname of the sender."),
                                object("from")
                                    .properties(
                                        string("email")
                                            .description("The email address recipients receive emails from."),
                                        string("name")
                                            .description("The name shown with the from email address.")),
                                object("reply_to")
                                    .properties(
                                        string("email")
                                            .description("The email address recipients reply to."),
                                        string("name")
                                            .description("The name shown with the reply to email address.")),
                                bool("verified")
                                    .description("Whether the sender is verified."),
                                bool("locked")
                                    .description("Whether the sender is used by a draft or scheduled send.")))))
        .help("", "https://docs.bytechef.io/reference/components/sendgrid_v1#list-senders")
        .perform(SendgridListSendersAction::perform);

    private SendgridListSendersAction() {
    }

    public static List<Map<?, ?>> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return SendgridUtils.getSenders(context);
    }
}
