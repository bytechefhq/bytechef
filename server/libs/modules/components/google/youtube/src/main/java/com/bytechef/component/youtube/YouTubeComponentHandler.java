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

package com.bytechef.component.youtube;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.youtube.action.YouTubeUploadVideoAction;
import com.bytechef.component.youtube.connection.YouTubeConnection;
import com.bytechef.component.youtube.trigger.YouTubeNewVideoTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class YouTubeComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("youTube")
        .title("YouTube")
        .description(
            "Enjoy the videos and music you love, upload original content, and share it all with friends, family, " +
                "and the world on YouTube.")
        .icon("path:assets/youtube.svg")
        .categories(ComponentCategory.HELPERS)
        .connection(YouTubeConnection.CONNECTION_DEFINITION)
        .actions(YouTubeUploadVideoAction.ACTION_DEFINITION)
        .clusterElements(tool(YouTubeUploadVideoAction.ACTION_DEFINITION))
        .triggers(YouTubeNewVideoTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
