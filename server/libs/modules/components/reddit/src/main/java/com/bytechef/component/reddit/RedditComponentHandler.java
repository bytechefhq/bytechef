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

package com.bytechef.component.reddit;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.reddit.action.RedditCreateCommentAction;
import com.bytechef.component.reddit.action.RedditCreatePostAction;
import com.bytechef.component.reddit.connection.RedditConnection;
import com.google.auto.service.AutoService;

/**
 * @author Marija Horvat
 */
@AutoService(ComponentHandler.class)
public class RedditComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("reddit")
        .title("Reddit")
        .description(
            "Reddit is a social news aggregation, discussion, and content-sharing platform where users post and " +
                "vote on content organized into communities called subreddits.")
        .icon("path:assets/reddit.svg")
        .customAction(true)
        .categories(ComponentCategory.SOCIAL_MEDIA)
        .connection(RedditConnection.CONNECTION_DEFINITION)
        .actions(
            RedditCreateCommentAction.ACTION_DEFINITION,
            RedditCreatePostAction.ACTION_DEFINITION)
        .clusterElements(
            tool(RedditCreateCommentAction.ACTION_DEFINITION),
            tool(RedditCreatePostAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
