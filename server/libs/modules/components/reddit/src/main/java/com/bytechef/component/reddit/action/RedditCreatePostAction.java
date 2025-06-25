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
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.reddit.constant.RedditConstants.KIND;
import static com.bytechef.component.reddit.constant.RedditConstants.SUBREDDIT_NAME;
import static com.bytechef.component.reddit.constant.RedditConstants.TEXT;
import static com.bytechef.component.reddit.constant.RedditConstants.TITLE;
import static com.bytechef.component.reddit.constant.RedditConstants.URL;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Marija Horvat
 */
public class RedditCreatePostAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createPost")
        .title("Create Post")
        .description("Creates new reddit post.")
        .properties(
            string(SUBREDDIT_NAME)
                .label("Subreddit Name")
                .description("Subreddit name.")
                .required(true),
            string(TITLE)
                .label("Title")
                .description("Post title.")
                .required(true),
            string(KIND)
                .label("Kind")
                .description("Type of post.")
                .options(option("Link", "link"), option("Self", "self"))
                .required(true),
            string(URL)
                .label("URL")
                .description("Link URL.")
                .required(true)
                .displayCondition("%s == '%s'".formatted(KIND, "link")),
            string(TEXT)
                .label("Text")
                .description("Post text.")
                .required(true)
                .displayCondition("%s == '%s'".formatted(KIND, "self")))
        .output()
        .perform(RedditCreatePostAction::perform);

    private RedditCreatePostAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.post("/submit"))
            .body(Body.of(
                SUBREDDIT_NAME, inputParameters.getRequiredString(SUBREDDIT_NAME),
                TITLE, inputParameters.getRequiredString(TITLE),
                KIND, inputParameters.getRequiredString(KIND),
                URL, inputParameters.getString(URL),
                TEXT, inputParameters.getString(TEXT)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
