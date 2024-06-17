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

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.BASE_URL;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.GET_ALL_USERS;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.REGION;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.USER_TYPE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;

/**
 * @author Luka Ljubić
 */
public class ZohoCrmGetAllUsersAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(GET_ALL_USERS)
        .title("Get All Users")
        .description("Get all users in your Zoho CRM team")
        .properties(
            string(USER_TYPE)
                .label("User Type")
                .description("Chose a category for your users response")
                .options(
                    option("Get all users", "AllUsers"),
                    option("Get all active users", "ActiveUsers"),
                    option("Get all inactive users", "DeactiveUsers"),
                    option("Get all confirmed users", "ConfirmedUsers"),
                    option("Get all non-confirmed users", "NotConfirmedUsers"),
                    option("Get all deleted users", "DeletedUsers"),
                    option("Get all active confirmed users", "ActiveConfirmedUsers"),
                    option("Get all admin users", "AdminUsers"),
                    option("Get all active confirmed admins", "ActiveConfirmedAdmins"),
                    option("Get current user", "CurrentUser"))
                .required(true))
        .outputSchema(
            object()
                .properties(
                    object("users")
                        .properties(
                            string("country"),
                            string("name_format__s"),
                            string("language"),
                            string("microsoft"),
                            string("Currency"),
                            string("ID"),
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
                            string("city"))))
        .perform(ZohoCrmGetAllUsersAction::perform);

    private ZohoCrmGetAllUsersAction() {
    }

    protected static Object perform(Parameters inputParameters, Parameters conectionParameters, ActionContext context) {
        return context.http(http -> http.get(conectionParameters.getRequiredString(REGION) + BASE_URL + "/users"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new Context.TypeReference<>() {});
    }

}
