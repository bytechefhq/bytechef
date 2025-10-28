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
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class BeamerNewVoteAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("newVote")
        .title("New Vote")
        .description("Creates a new vote on selected feature request.")
        .properties(
            string(FEATURE_REQUEST_ID)
                .label("Feature Request ID")
                .description("ID of the feature request that will have the new vote.")
                .options((OptionsFunction<String>) BeamerUtils::getFeatureRequestsOptions)
                .required(true),
            string(USER_ID)
                .label("User ID")
                .description("ID of the user that is creating the new vote.")
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
                        string(ID)
                            .description("The ID of the new vote."),
                        string("date")
                            .description("Creation date of the new vote."),
                        string("featureRequestTitle")
                            .description("Title of the feature request this vote is created on."),
                        string(USER_ID)
                            .description("ID of the user that created the new vote."),
                        string(USER_EMAIL)
                            .description("Email of the user that created the new vote."),
                        string(USER_FIRST_NAME)
                            .description("First name of the user that created the new vote."),
                        string(USER_LAST_NAME)
                            .description("Last name of the user that created the new vote."),
                        string("url")
                            .description("URL of the new vote in your dashboard."))))
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
