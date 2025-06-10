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

package com.bytechef.component.zoho.crm.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.zoho.crm.constant.ZohoCrmConstants.TYPE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Luka Ljubić
 * @author Monika Kušter
 */
public class ZohoCrmListUsersAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listUsers")
        .title("List Users")
        .description("Lists users found in Zoho account.")
        .properties(
            string(TYPE)
                .label("Type")
                .description("What type of user to return in list.")
                .options(
                    option("All users", "AllUsers", "All users (both active and inactive)."),
                    option("Active users", "ActiveUsers", "Only active users."),
                    option("Inactive users", "DeactiveUsers", "Users who have been deactivated."),
                    option("Confirmed users", "ConfirmedUsers", "Users who have confirmed their accounts."),
                    option("Non-confirmed users", "NotConfirmedUsers", "Users who have not confirmed their accounts."),
                    option("Deleted users", "DeletedUsers", "Users who have been deleted."),
                    option("Active confirmed users", "ActiveConfirmedUsers",
                        "Users who are both active and confirmed."),
                    option("Admin users", "AdminUsers", "Retrieves users with Administrator privileges."),
                    option("Active confirmed admins", "ActiveConfirmedAdmins",
                        "Retrieves active and confirmed administrators."),
                    option("Current user", "CurrentUser", "Currently logged-in CRM user."))
                .defaultValue("AllUsers")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        array("users")
                            .items(
                                object()
                                    .properties(
                                        string("country")
                                            .description("Country of the user."),
                                        string("language")
                                            .description("Language in which the user accesses the CRM."),
                                        string("id")
                                            .description("ID of the user."),
                                        object("profile")
                                            .properties(
                                                string("name")
                                                    .description("Name of the profile of the user."),
                                                string("id")
                                                    .description("ID of the profile of the user.")),
                                        object("created_by")
                                            .properties(
                                                string("name"),
                                                string("id")),
                                        string("full_name")
                                            .description("Full name of the user."),
                                        string("status")
                                            .description("Status of the user (active/inactive)."),
                                        object("role")
                                            .properties(
                                                string("name")
                                                    .description("Name of the role of the user."),
                                                string("id")
                                                    .description("ID of the role of the user.")),
                                        string("first_name")
                                            .description("first name of the user."),
                                        string("email")
                                            .description("Email address of the user."))))))
        .perform(ZohoCrmListUsersAction::perform);

    private ZohoCrmListUsersAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters conectionParameters, Context context) {
        return context.http(http -> http.get("/users"))
            .queryParameters(TYPE, inputParameters.getString(TYPE))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
