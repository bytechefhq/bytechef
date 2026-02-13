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

package com.bytechef.component.hacker.news;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.hacker.news.action.HackerNewsFetchTopStoriesAction;
import com.google.auto.service.AutoService;

/**
 * @author Marija Horvat
 */
@AutoService(ComponentHandler.class)
public class HackerNewsComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("hackerNews")
        .title("Hacker News")
        .description(
            "Hacker News is a social news website focused on computer science, startups and technology-related topics.")
        .customAction(true)
        .customActionHelp("", "https://github.com/HackerNews/API")
        .icon("path:assets/hacker-news.svg")
        .categories(ComponentCategory.SOCIAL_MEDIA)
        .actions(HackerNewsFetchTopStoriesAction.ACTION_DEFINITION)
        .clusterElements(tool(HackerNewsFetchTopStoriesAction.ACTION_DEFINITION))
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
