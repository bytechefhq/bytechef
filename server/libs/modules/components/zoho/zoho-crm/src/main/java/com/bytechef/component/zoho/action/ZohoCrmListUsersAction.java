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

package com.bytechef.component.zoho.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.TYPE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
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
                    option("All users", "AllUsers"),
                    option("Active users", "ActiveUsers"),
                    option("Inactive users", "DeactiveUsers"),
                    option("Confirmed users", "ConfirmedUsers"),
                    option("Non-confirmed users", "NotConfirmedUsers"),
                    option("Deleted users", "DeletedUsers"),
                    option("Active confirmed users", "ActiveConfirmedUsers"),
                    option("Admin users", "AdminUsers"),
                    option("Active confirmed admins", "ActiveConfirmedAdmins"),
                    option("Current user", "CurrentUser"))
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
                                        string("country"),
                                        string("language"),
                                        string("id"),
                                        object("profile")
                                            .properties(
                                                string("name"),
                                                string("id")),
                                        object("created_by")
                                            .properties(
                                                string("name"),
                                                string("id")),
                                        string("full_name"),
                                        string("status"),
                                        object("role")
                                            .properties(
                                                string("name"),
                                                string("id")),
                                        string("first_name"),
                                        string("email"))))))
        .perform(ZohoCrmListUsersAction::perform);

    private ZohoCrmListUsersAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters conectionParameters, ActionContext actionContext) {

        return actionContext.http(http -> http.get("/users"))
            .queryParameters(TYPE, inputParameters.getString(TYPE))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
