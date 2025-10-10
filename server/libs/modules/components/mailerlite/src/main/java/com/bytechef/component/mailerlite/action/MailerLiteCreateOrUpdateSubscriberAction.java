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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.DATA;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.EMAIL;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.GROUPS;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.GROUP_ID;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.ID;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.mailerlite.util.MailerLiteUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class MailerLiteCreateOrUpdateSubscriberAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createOrUpdateSubscriber")
        .title("Create or Update Subscriber")
        .description("Create new user or update an existing user.")
        .properties(
            string(EMAIL)
                .description("The email address of the subscriber.")
                .required(true),
            string(GROUP_ID)
                .label("Group ID")
                .description("ID of the group to which you want to add the subscriber to.")
                .options((OptionsFunction<String>) MailerLiteUtils::getGroupIdOptions)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        object(DATA)
                            .properties(
                                string(ID)
                                    .description("ID of the user that was created or updated."),
                                string(EMAIL)
                                    .description("The email address of the subscriber."),
                                string("status"),
                                string("source"),
                                integer("sent")
                                    .description("The number of messages that were sent to the subscriber."),
                                integer("opens_count")
                                    .description("Number of email messages the user has opened."),
                                integer("clicks_count"),
                                integer("open_rate"),
                                integer("click_rate"),
                                string("subscribed_at"),
                                string("created_at"),
                                string("updated_at"),
                                array("fields"),
                                array("groups")))))
        .perform(MailerLiteCreateOrUpdateSubscriberAction::perform);

    private MailerLiteCreateOrUpdateSubscriberAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.post("/subscribers"))
            .configuration(responseType(ResponseType.JSON))
            .body(
                Body.of(
                    Map.of(
                        EMAIL, inputParameters.getRequiredString(EMAIL),
                        GROUPS, List.of(inputParameters.getRequiredString(GROUP_ID)))))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
