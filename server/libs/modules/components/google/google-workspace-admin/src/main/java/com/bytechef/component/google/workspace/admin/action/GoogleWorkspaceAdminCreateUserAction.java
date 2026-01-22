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

package com.bytechef.component.google.workspace.admin.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.ADDRESS;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.CHANGE_PASSWORD;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.EMAIL;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.FIRST_NAME;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.LAST_NAME;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.PASSWORD;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.PHONE;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.USER_OUTPUT_PROPERTY;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleUtils;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class GoogleWorkspaceAdminCreateUserAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createUser")
        .title("Create User")
        .description("Creates a new user.")
        .properties(
            string(FIRST_NAME)
                .label("First Name")
                .description("The user's first name.")
                .required(true),
            string(LAST_NAME)
                .label("Last Name")
                .description("The user's last name.")
                .required(true),
            string(EMAIL)
                .label("Email")
                .description("The user's email address.")
                .required(true),
            string(PASSWORD)
                .label("Password")
                .description("The password for the user account.")
                .required(true),
            bool(CHANGE_PASSWORD)
                .label("Change Password At Next Login")
                .description("Indicates if the user is forced to change their password at next login.")
                .required(false),
            string(ADDRESS)
                .label("Address")
                .description("The user's full address.")
                .required(false),
            string(PHONE)
                .label("Phone")
                .description("The user's phone number.")
                .required(false))
        .output(outputSchema(USER_OUTPUT_PROPERTY))
        .perform(GoogleWorkspaceAdminCreateUserAction::perform)
        .processErrorResponse(GoogleUtils::processErrorResponse);

    private GoogleWorkspaceAdminCreateUserAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context
            .http(http -> http.post("https://admin.googleapis.com/admin/directory/v1/users"))
            .configuration(responseType(ResponseType.JSON))
            .body(
                Body.of(
                    EMAIL, inputParameters.getRequiredString(EMAIL),
                    PASSWORD, inputParameters.getRequiredString(PASSWORD),
                    "name", Map.of(
                        FIRST_NAME, inputParameters.getRequiredString(FIRST_NAME),
                        LAST_NAME, inputParameters.getRequiredString(LAST_NAME)),
                    CHANGE_PASSWORD, inputParameters.getBoolean(CHANGE_PASSWORD),
                    ADDRESS, inputParameters.getString(ADDRESS),
                    PHONE, inputParameters.getString(PHONE)))
            .execute()
            .getBody();
    }
}
