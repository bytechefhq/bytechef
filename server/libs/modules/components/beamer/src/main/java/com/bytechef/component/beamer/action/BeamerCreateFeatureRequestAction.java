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

import static com.bytechef.component.beamer.constant.BeamerConstants.CATEGORY;
import static com.bytechef.component.beamer.constant.BeamerConstants.CONTENT;
import static com.bytechef.component.beamer.constant.BeamerConstants.ID;
import static com.bytechef.component.beamer.constant.BeamerConstants.TITLE;
import static com.bytechef.component.beamer.constant.BeamerConstants.USER_EMAIL;
import static com.bytechef.component.beamer.constant.BeamerConstants.USER_FIRST_NAME;
import static com.bytechef.component.beamer.constant.BeamerConstants.USER_ID;
import static com.bytechef.component.beamer.constant.BeamerConstants.USER_LAST_NAME;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class BeamerCreateFeatureRequestAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createFeatureRequest")
        .title("Create Feature Request")
        .description("Creates a new feature request.")
        .properties(
            string(TITLE)
                .label("Feature Request Title")
                .description("The name of the new feature request.")
                .required(true),
            string(CONTENT)
                .label("Feature Request Content")
                .description("The content of the new feature request.")
                .required(false),
            string(USER_EMAIL)
                .label("User Email")
                .description("The email of the user that is creating the new feature request.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID)
                            .description("The ID of the new feature request."),
                        string("date")
                            .description("Publish date of the new feature request."),
                        string("visible")
                            .description("Whether this feature required is visible or not."),
                        string(CATEGORY)
                            .description("The category of the new feature request."),
                        string("status")
                            .description("The status of the new feature request."),
                        array("translations")
                            .items(
                                object()
                                    .properties(
                                        string(TITLE)
                                            .description("Title of the feature request."),
                                        string(CONTENT)
                                            .description("Content of the feature request (plain text)."),
                                        string("contentHtml")
                                            .description("Content of the feature request (original HTML format)."),
                                        string("language")
                                            .description(
                                                "Language of this feature request (in ISO-639 two-letter code format)."),
                                        string("permalink")
                                            .description("Permalink of this feature request."),
                                        array("images")
                                            .description("URLs of the images embedded in this post.")
                                            .items(string()))),
                        integer("votesCount")
                            .description("The number of votes for the new feature request."),
                        string("commentsCount")
                            .description("The number of comments for the new feature request."),
                        string("notes")
                            .description("The notes for the new feature request."),
                        string("filters")
                            .description("Segment filters for the new feature request."),
                        string("internalUserEmail")
                            .description(
                                "Email of the user in your account who created this feature request (if created by a team member)."),
                        string("internalUserFirstname")
                            .description(
                                "First name of the user in your account who created this feature request (if created by a team member)."),
                        string("internalUserLastname")
                            .description(
                                "Last name of the user in your account who created this feature request (if created by a team member)."),
                        string(USER_ID)
                            .description(
                                "ID of the end user who created this feature request (if created by an end user)."),
                        string(USER_EMAIL)
                            .description(
                                "Email of the end user who created this feature request (if created by an end user)."),
                        string(USER_FIRST_NAME)
                            .description(
                                "First name of the end user who created this feature request (if created by an end user)."),
                        string(USER_LAST_NAME)
                            .description(
                                "Last name of the end user who created this feature request (if created by an end user)."))))
        .perform(BeamerCreateFeatureRequestAction::perform);

    private BeamerCreateFeatureRequestAction() {
    }

    protected static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.post("/requests"))
            .body(
                Body.of(
                    TITLE, List.of(inputParameters.getRequiredString(TITLE)),
                    CONTENT, List.of(inputParameters.getString(CONTENT)),
                    USER_EMAIL, inputParameters.getString(USER_EMAIL)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
