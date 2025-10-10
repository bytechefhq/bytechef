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

package com.bytechef.component.mailerlite.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.DATA;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.GROUP_ID;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.ID;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.NAME;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.SUBSCRIBER_ID;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.mailerlite.util.MailerLiteUtils;

/**
 * @author Nikolina Spehar
 */
public class MailerLiteAddSubscriberToGroupAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("addSubscriberToGroup")
        .title("Add Subscriber to Group")
        .description("Adding a subscriber to a selected group.")
        .properties(
            string(SUBSCRIBER_ID)
                .label("Subscriber Email")
                .description("ID of the user that will be added to the selected group.")
                .options((OptionsFunction<String>) MailerLiteUtils::getSubscriberIdOptions)
                .required(true),
            string(GROUP_ID)
                .label("Group ID")
                .description("ID of the group to which the user will be added.")
                .options((OptionsFunction<String>) MailerLiteUtils::getGroupIdOptions)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        object(DATA)
                            .properties(
                                string(ID)
                                    .description("ID of the group to which the user was added."),
                                string(NAME)
                                    .description("The name of the group to which the user was added."),
                                integer("active_count")
                                    .description("Number of active subscribers in the group."),
                                integer("sent_count")
                                    .description("Number of sent email messages in the group."),
                                integer("opens_count")
                                    .description("Number of opened email messages in the group."),
                                object("open_rate")
                                    .properties(
                                        number("float"),
                                        string("string")),
                                integer("clicks_count"),
                                object("click_rate")
                                    .properties(
                                        number("float"),
                                        string("string")),
                                integer("unsubscribed_count")
                                    .description("Number of subscribers that unsubscribed from the group."),
                                integer("unconfirmed_count")
                                    .description("Number of unconfirmed subscribers in the group."),
                                integer("bounced_count")
                                    .description("Number of subscribers that were bounced from the group."),
                                integer("junk_count")
                                    .description("Number of email messages that were sent to junk."),
                                string("created_at")
                                    .description("The date and time the group was created.")))))
        .perform(MailerLiteAddSubscriberToGroupAction::perform);

    private MailerLiteAddSubscriberToGroupAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.post(
            "/subscribers/" + inputParameters.getRequiredString(SUBSCRIBER_ID) + "/groups/" +
                inputParameters.getRequiredString(GROUP_ID)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
