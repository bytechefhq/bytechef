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

package com.bytechef.component.posthog;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.posthog.action.PostHogCreateEventAction;
import com.bytechef.component.posthog.action.PostHogCreateProjectAction;
import com.bytechef.component.posthog.connection.PostHogConnection;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class PostHogComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("postHog")
        .title("PostHog")
        .description(
            "PostHog is the only all-in-one platform for product analytics, feature flags, session replays, " +
                "experiments, and surveys that's built for developers.")
        .icon("path:assets/posthog.svg")
        .categories(ComponentCategory.ANALYTICS)
        .connection(PostHogConnection.CONNECTION_DEFINITION)
        .actions(
            PostHogCreateEventAction.ACTION_DEFINITION,
            PostHogCreateProjectAction.ACTION_DEFINITION)
        .clusterElements(
            tool(PostHogCreateEventAction.ACTION_DEFINITION),
            tool(PostHogCreateProjectAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
