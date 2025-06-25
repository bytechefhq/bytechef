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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.reddit.constant.RedditConstants.TEXT;
import static com.bytechef.component.reddit.constant.RedditConstants.THING_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;

/**
 * @author Marija Horvat
 */
public class RedditCreateCommentAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createComment")
        .title("Create Comment")
        .description("Creates comment on a Reddit post or replies to a comment.")
        .properties(
            string(THING_ID)
                .label("Parent ID")
                .description("Post ID (t3_*) or comment ID (t1_*) to reply to.")
                .required(true),
            string(TEXT)
                .label("Comment Text")
                .description("Comment text.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        array("jquery")
                            .description("An array with response data."),
                        bool("success")
                            .description("Boolean value that indicates the success or failure of the request."))))
        .perform(RedditCreateCommentAction::perform);

    private RedditCreateCommentAction() {
    }

    protected static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.post("/api/comment"))
            .queryParameters(
                THING_ID, inputParameters.getRequiredString(THING_ID),
                TEXT, inputParameters.getRequiredString(TEXT))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
