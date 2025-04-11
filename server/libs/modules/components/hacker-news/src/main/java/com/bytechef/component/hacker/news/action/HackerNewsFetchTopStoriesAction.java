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

package com.bytechef.component.hacker.news.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;

public class HackerNewsFetchTopStoriesAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("fetchTopStories")
        .title("Fetch Top Stories")
        .description("Fetch top stories from Hacker News.")
        .properties(
            integer("numberOfStories")
                .label("Number Of Stories")
                .description("Number of stories to fetch.")
                .defaultValue(10)
                .required(true))
        .output(outputSchema(array().items(object())))
        .perform(HackerNewsFetchTopStoriesAction::perform);

    private HackerNewsFetchTopStoriesAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<String> storyIds = context
            .http(http -> http.get("https://hacker-news.firebaseio.com/v0/topstories.json"))
            .configuration(responseType(Context.Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getItems(inputParameters, context, storyIds);
    }

    private static Object getItems(Parameters inputParameters, Context context, List<String> storyIds) {
        List<Object> bodies = new ArrayList<>();

        for (int i = 0; i < Math.min(inputParameters.getInteger("numberOfStories"), storyIds.size()); i++) {
            final String storyId = storyIds.get(i);

            Object body = context
                .http(http -> http.get("https://hacker-news.firebaseio.com/v0/item/" + storyId + ".json"))
                .configuration(responseType(Context.Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            bodies.add(body);
        }
        return bodies;
    }
}
