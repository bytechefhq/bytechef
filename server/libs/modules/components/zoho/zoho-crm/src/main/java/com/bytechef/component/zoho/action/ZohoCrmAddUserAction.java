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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.EMAIL;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.FIRST_NAME;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.LAST_NAME;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.PROFILE;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.ROLE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.zoho.util.ZohoCrmUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Luka Ljubić
 * @author Monika Kušter
 */
public class ZohoCrmAddUserAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("addUser")
        .title("Add User")
        .description("Add user to your organization.")
        .properties(
            string(FIRST_NAME)
                .label("First name")
                .description("First name of the user.")
                .required(true),
            string(LAST_NAME)
                .label("Last name")
                .description("Last name of the user.")
                .required(false),
            string(EMAIL)
                .label("Email")
                .description("User's email. An invitation will be sent to this email address")
                .controlType(ControlType.EMAIL)
                .required(true),
            string(ROLE)
                .label("Role")
                .description("Role you want to assign the user with.")
                .options((ActionOptionsFunction<String>) ZohoCrmUtils::getRoleOptions)
                .required(true),
            string(PROFILE)
                .label("Profile")
                .description("Profile you want to assign the user with..")
                .options((ActionOptionsFunction<String>) ZohoCrmUtils::getProfileOptions)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        array("users")
                            .items(
                                object()
                                    .properties(
                                        string("code"),
                                        object("details")
                                            .properties(
                                                string("id")),
                                        string("message"),
                                        string("status"))))))
        .perform(ZohoCrmAddUserAction::perform);

    private ZohoCrmAddUserAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters conectionParameters, ActionContext actionContext) {

        return actionContext.http(http -> http.post("/users"))
            .body(
                Body.of(
                    "users", List.of(
                        Map.of(ROLE, inputParameters.getRequiredString(ROLE),
                            FIRST_NAME, inputParameters.getRequiredString(FIRST_NAME),
                            EMAIL, inputParameters.getRequiredString(EMAIL),
                            PROFILE, inputParameters.getRequiredString(PROFILE),
                            LAST_NAME, inputParameters.getString(LAST_NAME, "")))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
