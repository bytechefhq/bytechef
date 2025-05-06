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

package com.bytechef.component.youtube;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.youtube.action.YoutubeTesting;
import com.bytechef.component.youtube.action.YoutubeUploadVideoAction;
import com.bytechef.component.youtube.connection.YoutubeConnection;
import com.bytechef.component.youtube.trigger.YoutubeNewVideoTrigger;
import com.google.auto.service.AutoService;

@AutoService(ComponentHandler.class)
public class YoutubeComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("youtube")
        .title("Youtube")
        .description("YouTube is an American social media and online video sharing platform owned by Google.")
        .icon("path:assets/youtube.svg")
        .categories(ComponentCategory.HELPERS)
        .connection(YoutubeConnection.CONNECTION_DEFINITION)
        .actions(
            YoutubeUploadVideoAction.ACTION_DEFINITION,
            YoutubeTesting.ACTION_DEFINITION)
        .triggers(YoutubeNewVideoTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
