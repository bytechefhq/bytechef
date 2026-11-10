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

package com.bytechef.component.slack.action;

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.slack.constant.SlackConstants.EMAIL;
import static com.bytechef.component.slack.constant.SlackConstants.ERROR;
import static com.bytechef.component.slack.constant.SlackConstants.ID;
import static com.bytechef.component.slack.constant.SlackConstants.NAME;
import static com.bytechef.component.slack.constant.SlackConstants.OK;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class SlackFindUserByEmailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("findUserByEmail")
        .title("Find User by Email")
        .description("Finds a user in the Slack workspace by their email address.")
        .properties(
            string(EMAIL)
                .label("Email")
                .description("Email address of the user to find.")
                .exampleValue("john.doe@example.com")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID)
                            .description("ID of the user."),
                        string("team_id")
                            .description("ID of the workspace the user belongs to."),
                        string(NAME)
                            .description("Username of the user."),
                        string("real_name")
                            .description("Real name of the user."),
                        bool("deleted")
                            .description("Indicates if the user has been deactivated."),
                        bool("is_admin")
                            .description("Indicates if the user is an admin of the workspace."),
                        bool("is_owner")
                            .description("Indicates if the user is an owner of the workspace."),
                        bool("is_bot")
                            .description("Indicates if the user is a bot user."),
                        string("tz")
                            .description("Time zone of the user."),
                        object("profile")
                            .description("Profile of the user.")
                            .properties(
                                string("real_name")
                                    .description("Real name of the user."),
                                string("display_name")
                                    .description("Display name of the user."),
                                string(EMAIL)
                                    .description("Email address of the user."),
                                string("title")
                                    .description("Title of the user."),
                                string("phone")
                                    .description("Phone number of the user."),
                                string("status_text")
                                    .description("Status text of the user."),
                                string("status_emoji")
                                    .description("Status emoji of the user.")))))
        .help("", "https://docs.bytechef.io/reference/components/slack_v1#find-user-by-email")
        .perform(SlackFindUserByEmailAction::perform);

    private SlackFindUserByEmailAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, Object> body = context
            .http(http -> http.get("/users.lookupByEmail"))
            .queryParameter(EMAIL, inputParameters.getRequiredString(EMAIL))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if ((boolean) body.get(OK)) {
            return body.get("user");
        } else {
            throw new ProviderException((String) body.get(ERROR));
        }
    }
}
