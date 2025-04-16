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
import static com.bytechef.component.hacker.news.constant.HackerNewsConstants.NUMBER_OF_STORIES;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.hacker.news.constant.HackerNewsConstants;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Marija Horvat
 */
public class HackerNewsFetchTopStoriesAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("fetchTopStories")
        .title("Fetch Top Stories")
        .description("Fetch top stories from Hacker News.")
        .properties(
            integer(NUMBER_OF_STORIES)
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
            .http(http -> http.get(HackerNewsConstants.BASE_URL + "/topstories.json"))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getStories(inputParameters.getRequiredInteger(NUMBER_OF_STORIES), context, storyIds);
    }

    private static Object getStories(int numberOfStories, Context context, List<String> storyIds) {
        return storyIds
            .stream()
            .limit(Math.min(numberOfStories, storyIds.size()))
            .map(storyId -> fetchStory(context, storyId))
            .collect(Collectors.toList());
    }

    private static Object fetchStory(Context context, String storyId) {
        return context
            .http(http -> http.get(HackerNewsConstants.BASE_URL + "/item/" + storyId + ".json"))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
