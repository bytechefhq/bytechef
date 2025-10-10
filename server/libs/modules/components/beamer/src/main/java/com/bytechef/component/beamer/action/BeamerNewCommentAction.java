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

import static com.bytechef.component.beamer.constant.BeamerConstants.ID;
import static com.bytechef.component.beamer.constant.BeamerConstants.POST_ID;
import static com.bytechef.component.beamer.constant.BeamerConstants.TEXT;
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
public class BeamerNewCommentAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("newComment")
        .title("New Comment")
        .description("Creates a new comment on selected post.")
        .properties(
            string(POST_ID)
                .label("Post")
                .description("ID of the post that will have the new comment.")
                .options((OptionsFunction<String>) BeamerUtils::getPostsOptions)
                .required(true),
            string(TEXT)
                .label("Text")
                .description("Text of the comment.")
                .required(false),
            string(USER_ID)
                .label("User ID")
                .description("ID of the user that is creating the new comment.")
                .required(false),
            string(USER_EMAIL)
                .label("User Email")
                .description("Email of the user that is creating the new comment.")
                .required(false),
            string(USER_FIRST_NAME)
                .label("User First Name")
                .description("First name of the user that is creating the new comment.")
                .required(false),
            string(USER_LAST_NAME)
                .label("User Last Name")
                .description("Last name of the user that is creating the new comment.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID)
                            .description("ID of the new comment."),
                        string("date")
                            .description("Publication date of the new comment."),
                        string(TEXT)
                            .description("Content of the new comment."),
                        string("postTitle")
                            .description("Title of the post this comment was created on."),
                        string(USER_ID)
                            .description("ID of the user that created the new comment."),
                        string(USER_EMAIL)
                            .description("Email of the user that created the new comment."),
                        string(USER_FIRST_NAME)
                            .description("First name of the user that created the new comment."),
                        string(USER_LAST_NAME)
                            .description("Last name of the user that created the new comment."),
                        string("url")
                            .description("URL of the new comment in your dashboard."))))
        .perform(BeamerNewCommentAction::perform);

    private BeamerNewCommentAction() {
    }

    protected static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.post("/posts/" + inputParameters.getRequiredString(POST_ID) + "/comments"))
            .body(
                Body.of(
                    TEXT, inputParameters.getRequiredString(TEXT),
                    USER_ID, inputParameters.getRequiredString(USER_ID),
                    USER_EMAIL, inputParameters.getRequiredString(USER_EMAIL),
                    USER_FIRST_NAME, inputParameters.getRequiredString(USER_FIRST_NAME),
                    USER_LAST_NAME, inputParameters.getRequiredString(USER_LAST_NAME)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

}
