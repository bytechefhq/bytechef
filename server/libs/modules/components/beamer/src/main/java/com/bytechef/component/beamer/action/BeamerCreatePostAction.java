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

import static com.bytechef.component.beamer.constant.BeamerConstants.CATEGORY;
import static com.bytechef.component.beamer.constant.BeamerConstants.CONTENT;
import static com.bytechef.component.beamer.constant.BeamerConstants.ID;
import static com.bytechef.component.beamer.constant.BeamerConstants.TITLE;
import static com.bytechef.component.beamer.constant.BeamerConstants.USER_EMAIL;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.beamer.util.BeamerUtils;
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
public class BeamerCreatePostAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createPost")
        .title("Create Post")
        .description("Creates a new post.")
        .properties(
            string(TITLE)
                .label("Title")
                .description("Title of the new post.")
                .required(true),
            string(CONTENT)
                .label("Content")
                .description("Content of the new post.")
                .required(true),
            string(CATEGORY)
                .label("Category")
                .description("Category of the new post.")
                .options(BeamerUtils.getPostCategoryOptions())
                .required(true),
            string(USER_EMAIL)
                .label("User Email")
                .description("Email of the user that is creating the new post.")
                .required(false))
        .output(outputSchema(
            object()
                .properties(
                    string(ID),
                    string("date"),
                    string("dueDate"),
                    string("published"),
                    string(CATEGORY),
                    string("feedbackEnabled"),
                    string("reactionsEnabled"),
                    array("translations")
                        .items(
                            object()
                                .properties(
                                    string(TITLE),
                                    string(CONTENT),
                                    string(CATEGORY),
                                    string("contentHtml"),
                                    string("language"),
                                    string("postUrl"))))))
        .perform(BeamerCreatePostAction::perform);

    private BeamerCreatePostAction() {
    }

    protected static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.post("/posts"))
            .body(
                Body.of(
                    TITLE, List.of(inputParameters.getRequiredString(TITLE)),
                    CONTENT, List.of(inputParameters.getRequiredString(CONTENT)),
                    CATEGORY, inputParameters.getRequiredString(CATEGORY),
                    USER_EMAIL, inputParameters.getString(USER_EMAIL)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
