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

package com.bytechef.component.beamer.action;

import static com.bytechef.component.beamer.constant.BeamerConstants.FEATURE_REQUEST_ID;
import static com.bytechef.component.beamer.constant.BeamerConstants.ID;
import static com.bytechef.component.beamer.constant.BeamerConstants.USER_EMAIL;
import static com.bytechef.component.beamer.constant.BeamerConstants.USER_FIRST_NAME;
import static com.bytechef.component.beamer.constant.BeamerConstants.USER_ID;
import static com.bytechef.component.beamer.constant.BeamerConstants.USER_LAST_NAME;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.beamer.util.BeamerUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class BeamerNewVoteAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("newVote")
        .title("New Vote")
        .description("Create a new vote on selected feature request.")
        .properties(
            string(FEATURE_REQUEST_ID)
                .label("Feature Request")
                .description("The id of the feature request that will have the new vote.")
                .options((ActionOptionsFunction<String>) BeamerUtils::getFeatureRequestsOptions)
                .required(true),
            string(USER_ID)
                .label("User ID")
                .description("The id of the user that is creating the new vote.")
                .required(false),
            string(USER_EMAIL)
                .label("User Email")
                .description("Email of the user that is creating the new vote.")
                .required(false),
            string(USER_FIRST_NAME)
                .label("User First Name")
                .description("First name of the user that is creating the new vote.")
                .required(false),
            string(USER_LAST_NAME)
                .label("User Last Name")
                .description("Last name of the user that is creating the new vote.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID),
                        string("date"),
                        string("featureRequestTitle"),
                        string(USER_ID),
                        string(USER_EMAIL),
                        string(USER_FIRST_NAME),
                        string(USER_LAST_NAME),
                        string("userCustomAttributes"),
                        string("url"))))
        .perform(BeamerNewVoteAction::perform);

    private BeamerNewVoteAction() {
    }

    protected static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.post("/requests/" + inputParameters.getRequiredString(FEATURE_REQUEST_ID) + "/votes"))
            .body(
                Body.of(
                    USER_ID, inputParameters.getRequiredString(USER_ID),
                    USER_EMAIL, inputParameters.getString(USER_EMAIL),
                    USER_FIRST_NAME, inputParameters.getString(USER_FIRST_NAME),
                    USER_LAST_NAME, inputParameters.getString(USER_LAST_NAME)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
