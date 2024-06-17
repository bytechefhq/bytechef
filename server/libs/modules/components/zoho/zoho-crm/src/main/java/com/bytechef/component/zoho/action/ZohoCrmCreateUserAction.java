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
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.BASE_URL;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.CREATE_USER;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.EMAIL;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.FIRST_NAME;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.LAST_NAME;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.REGION;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.USER_PROFILE;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.USER_ROLE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.zoho.util.ZohoCrmUtils;

/**
 * @author Luka Ljubić
 */
public class ZohoCrmCreateUserAction {

    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_USER)
        .title("Create a user")
        .description("Create a user for your CRM team")
        .properties(
            string(FIRST_NAME)
                .label("Users First Name")
                .description("Input user first name")
                .required(true),
            string(LAST_NAME)
                .label("User Last Name")
                .description("Input user last name")
                .required(true),
            string(EMAIL)
                .label("User Email")
                .description("Input user email")
                .required(true),
            string(USER_ROLE)
                .label("User Role")
                .description("Input user role")
                .options((ActionOptionsFunction<String>) ZohoCrmUtils::getRoleOptions)
                .required(true),
            string(USER_PROFILE)
                .label("User Profile")
                .description("Input user profile")
                .options((ActionOptionsFunction<String>) ZohoCrmUtils::getProfileOptions)
                .required(true))
        .outputSchema(
            object()
                .properties(
                    object("users")
                        .properties(
                            string("code"),
                            object("details")
                                .properties(
                                    string("id")),
                            string("message"),
                            string("status"))))
        .perform(ZohoCrmCreateUserAction::perform);

    private ZohoCrmCreateUserAction() {
    }

    protected static Object perform(Parameters inputParameters, Parameters conectionParameters, ActionContext context) {
        return context.http(http -> http.post(conectionParameters.getRequiredString(REGION) + BASE_URL + "/users"))
            .body(
                Body.of(
                    USER_ROLE, inputParameters.getRequiredString(USER_ROLE),
                    FIRST_NAME, inputParameters.getRequiredString(FIRST_NAME),
                    EMAIL, inputParameters.getRequiredString(EMAIL),
                    USER_PROFILE, inputParameters.getRequiredString(USER_PROFILE),
                    LAST_NAME, inputParameters.getRequiredString(LAST_NAME)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
