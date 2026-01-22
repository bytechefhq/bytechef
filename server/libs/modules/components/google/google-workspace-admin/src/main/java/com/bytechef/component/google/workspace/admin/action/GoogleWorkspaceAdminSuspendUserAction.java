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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.USER_ID;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.USER_OUTPUT_PROPERTY;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.workspace.admin.util.GoogleWorkspaceAdminUtils;
import com.bytechef.google.commons.GoogleUtils;

/**
 * @author Marija Horvat
 */
public class GoogleWorkspaceAdminSuspendUserAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("suspendUser")
        .title("Suspend User")
        .description("Suspends a user.")
        .properties(
            string(USER_ID)
                .label("User ID")
                .description("The unique ID of the user.")
                .options((OptionsFunction<String>) GoogleWorkspaceAdminUtils::getUserIdOptions)
                .required(true))
        .output(outputSchema(USER_OUTPUT_PROPERTY))
        .perform(GoogleWorkspaceAdminSuspendUserAction::perform)
        .processErrorResponse(GoogleUtils::processErrorResponse);

    private GoogleWorkspaceAdminSuspendUserAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context
            .http(http -> http.post(
                "https://admin.googleapis.com/admin/directory/v1/users/" + inputParameters.getRequiredString(USER_ID)))
            .configuration(responseType(ResponseType.JSON))
            .body(Body.of("suspended", true))
            .execute()
            .getBody();
    }
}
