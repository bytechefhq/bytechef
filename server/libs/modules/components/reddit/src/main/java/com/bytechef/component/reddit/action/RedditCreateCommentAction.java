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

package com.bytechef.component.reddit.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.reddit.constant.RedditConstants.TEXT;
import static com.bytechef.component.reddit.constant.RedditConstants.THING_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Marija Horvat
 */
public class RedditCreateCommentAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createComment")
        .title("Create Comment")
        .description("Creates a new comment on a Reddit post or comment.")
        .properties(
            string(THING_ID)
                .label("Parent ID")
                .description("Fullname of parent (post ID or comment ID to reply to).")
                .required(true),
            string(TEXT)
                .label("Comment Text")
                .description("Comment text.")
                .required(true))
        .output()
        .perform(RedditCreateCommentAction::perform);

    private RedditCreateCommentAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.post("/comment"))
            .body(Body.of(
                THING_ID, inputParameters.getRequiredString(THING_ID),
                TEXT, inputParameters.getRequiredString(TEXT)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
